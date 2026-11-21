package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.MilAmmo;
import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.network.MilControlPayload;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Côté client des véhicules militaires :
 * - touches (dans un véhicule militaire) : X fumigènes, C changer de place, R type de munition, H déployer (lance-
 *   missiles), N vision thermique (tireur / commandant / pilote), Z zoom x1 / x4 / x8 ; hélicoptère : Espace monter,
 *   Ctrl (sprint) descendre ;
 * - HUD : blindage, carburant, vitesse, angle de tourelle, armes et munitions de la place, rechargement, surchauffe,
 *   fumigènes, protection active, modules endommagés, verrouillage des missiles, télémètre ;
 * - caméra du tireur dans l'axe du canon, zoom, vision thermique, recul de la caméra à la 3e personne ;
 * - passagers protégés (dans la caisse) cachés ; bruit de moteur diesel / turbine / chenilles / rotor.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class MilClient {
    private static int zoom = 1;
    private static boolean thermal;
    private static final Map<Integer, Loop> SOUNDS = new HashMap<>();

    static {
        MilVehicleEntity.heliLift = () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) {
                return 0;
            }
            return mc.options.keyJump.isDown() ? 1 : mc.options.keySprint.isDown() ? -1 : 0;
        };
    }

    public static MilVehicleEntity vehicle() {
        Player p = Minecraft.getInstance().player;
        return p != null && p.getVehicle() instanceof MilVehicleEntity v ? v : null;
    }

    public static MilType.Seat seat() {
        MilVehicleEntity v = vehicle();
        return v == null ? null : v.seat(Minecraft.getInstance().player);
    }

    /** Place armée (clic gauche / droit = armes du véhicule) : la détente est envoyée au serveur comme pour une arme. */
    public static boolean armedSeat(Player p) {
        if (!(p.getVehicle() instanceof MilVehicleEntity v)) {
            return false;
        }
        MilType.Seat s = v.seat(p);
        return s != null && (s.primary() >= 0 || s.secondary() >= 0);
    }

    public static boolean thermalActive() {
        return thermal && vehicle() != null;
    }

    private static boolean optics(MilType.Seat s) {
        return s != null && (s.role() == MilType.Role.GUNNER || s.role() == MilType.Role.COMMANDER || s.role() == MilType.Role.PILOT
                || s.role() == MilType.Role.OPERATOR);
    }

    // ------------------------------------------------------------------ touches
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        MilVehicleEntity v = vehicle();
        if (v == null) {
            zoom = 1;
            thermal = false;
            return;
        }
        MilType.Seat s = seat();
        while (ModKeyMappings.PRONE.consumeClick()) {
            send(MilControlPayload.SMOKE);
        }
        while (ModKeyMappings.MELEE.consumeClick()) {
            send(MilControlPayload.SEAT);
            zoom = 1;
        }
        while (ModKeyMappings.RELOAD.consumeClick()) {
            send(MilControlPayload.AMMO);
        }
        while (ModKeyMappings.SIREN.consumeClick()) {
            send(MilControlPayload.DEPLOY);
        }
        while (ModKeyMappings.HELMET_MODULE.consumeClick()) {
            if (optics(s)) {
                thermal = !thermal;
                mc.player.playSound(ModSounds.NVG_ON.get(), 0.6f, thermal ? 1.2f : 0.8f);
            }
        }
        while (ModKeyMappings.VEHICLE_ZOOM.consumeClick()) {
            if (optics(s)) {
                zoom = zoom == 1 ? 4 : zoom == 4 ? 8 : 1;
            }
        }
        if (!optics(s)) {
            zoom = 1;
            thermal = false;
        }
    }

    private static void send(int action) {
        PacketDistributor.sendToServer(new MilControlPayload(action));
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (vehicle() != null && zoom > 1) {
            event.setNewFovModifier(event.getNewFovModifier() / zoom);
        }
    }

    @SubscribeEvent
    public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        MilVehicleEntity v = vehicle();
        if (v != null) {
            float[] d = MilVehicleEntity.dims(v.type());
            event.setDistance(Math.max(event.getDistance(), d[0] * 0.8f + 2));
        }
    }

    /** Vue à la 1re personne du tireur : dans l'axe du canon (viseur au-dessus du tourillon). */
    public static Vec3 cameraPosition(float partial) {
        MilVehicleEntity v = vehicle();
        Minecraft mc = Minecraft.getInstance();
        if (v == null || !mc.options.getCameraType().isFirstPerson()) {
            return null;
        }
        MilType.Seat s = v.seat(mc.player);
        if (s == null || s.role() != MilType.Role.GUNNER || s.primary() < 0 || v.type().weapons.get(s.primary()).free()) {
            return null;
        }
        var geo = v.type().geo();
        if (geo.gun() == null) {
            return null;
        }
        Vec3[] md = v.muzzle(v.type().weapons.get(s.primary()), mc.player, 0);
        Vec3 pivot = v.toWorld(geo.gun().x, geo.gun().y, geo.gun().z);
        Vec3 dir = md[1];
        // viseur : au-dessus du tourillon, un peu en avant (on ne voit pas l'intérieur de la tourelle)
        return pivot.add(0, 0.45, 0).add(dir.scale(v.type().heli() ? 0.6 : 1.2));
    }

    /** Les passagers à l'abri dans la caisse ne sont pas dessinés. */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        Entity e = event.getEntity();
        if (e.getVehicle() instanceof MilVehicleEntity v) {
            MilType.Seat s = v.seat(e);
            if (s != null && s.enclosed()) {
                event.setCanceled(true);
            }
        }
    }

    // ------------------------------------------------------------------ sons moteur
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            SOUNDS.clear();
            return;
        }
        SOUNDS.values().removeIf(Loop::isStopped);
        for (MilVehicleEntity v : mc.level.getEntitiesOfClass(MilVehicleEntity.class, mc.player.getBoundingBox().inflate(80))) {
            boolean running = v.type().heli() ? v.rotor() > 0.05f : v.getControllingPassenger() != null;
            if (running && !SOUNDS.containsKey(v.getId())) {
                Loop l = new Loop(v);
                SOUNDS.put(v.getId(), l);
                mc.getSoundManager().play(l);
                if (v.type().tracked()) {
                    Loop t = new Loop(v, ModSounds.TRACKS_LOOP.get(), true);
                    SOUNDS.put(-v.getId(), t);
                    mc.getSoundManager().play(t);
                }
            }
        }
    }

    static final class Loop extends AbstractTickableSoundInstance {
        private final MilVehicleEntity v;
        private final boolean tracks;

        Loop(MilVehicleEntity v) {
            this(v, v.type().heli() ? ModSounds.ROTOR_LOOP.get()
                    : v.type() == MilType.T_80BVM || v.type() == MilType.M1A2_ABRAMS ? ModSounds.TURBINE_LOOP.get() : ModSounds.DIESEL_LOOP.get(), false);
        }

        Loop(MilVehicleEntity v, SoundEvent sound, boolean tracks) {
            super(sound, SoundSource.NEUTRAL, RandomSource.create());
            this.v = v;
            this.tracks = tracks;
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.volume = 0.6f;
            this.x = v.getX();
            this.y = v.getY();
            this.z = v.getZ();
        }

        @Override
        public void tick() {
            boolean running = v.type().heli() ? v.rotor() > 0.05f : v.getControllingPassenger() != null;
            if (v.isRemoved() || !running) {
                stop();
                return;
            }
            x = v.getX();
            y = v.getY();
            z = v.getZ();
            float sp = Math.min(1, Math.abs(v.speed()) / Math.max(0.01f, v.type().maxSpeed()));
            if (v.type().heli()) {
                volume = 0.4f + v.rotor() * 1.6f;
                pitch = 0.6f + v.rotor() * 0.45f;
            } else if (tracks) {
                volume = sp * 1.2f;
                pitch = 0.7f + sp * 0.5f;
            } else {
                volume = 0.7f + sp * 0.8f;
                pitch = (v.type().health >= 400 ? 0.6f : 0.8f) + sp * 0.5f;
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }

    // ------------------------------------------------------------------ HUD
    public static void renderHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        MilVehicleEntity v = vehicle();
        if (v == null || mc.options.hideGui) {
            return;
        }
        MilType t = v.type();
        MilType.Seat s = v.seat(mc.player);
        var font = mc.font;
        int x = 6, y = 6, w = 190;
        List<Component> lines = new java.util.ArrayList<>();
        int hp = Math.round(v.health() / t.health * 100);
        int lineCount = 6 + (s != null && s.primary() >= 0 ? 1 : 0) + (s != null && s.secondary() >= 0 ? 1 : 0);
        g.fill(x - 3, y - 3, x + w, y + lineCount * 11 + 2, 0x90000000);
        g.drawString(font, Component.translatable("entity.modeminecraft." + t.id).withStyle(net.minecraft.ChatFormatting.BOLD), x, y, 0xFFE8D890);
        y += 11;
        bar(g, font, x, y, "hud.modeminecraft.mil.armor", hp / 100f, hp > 50 ? 0xFF5BC85B : hp > 25 ? 0xFFE0B030 : 0xFFE04040, hp + " %");
        y += 11;
        bar(g, font, x, y, "hud.modeminecraft.mil.fuel", v.fuel(), v.fuel() > 0.2f ? 0xFF4AA0E0 : 0xFFE04040, Math.round(v.fuel() * 100) + " %");
        y += 11;
        float kmh = Math.abs(v.speed()) / (t.heli() ? 0.0095f : 0.0111f);
        String turret = t.geo().turret() != null ? "   " + Component.translatable("hud.modeminecraft.mil.turret").getString() + " "
                + Math.round(Mth.wrapDegrees(-v.turret(1)) + 360) % 360 + "°" : "";
        String alt = t.heli() ? "   " + Component.translatable("hud.modeminecraft.mil.altitude").getString() + " "
                + (int) (v.getY() - mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, v.getBlockX(), v.getBlockZ())) + " m" : "";
        g.drawString(font, Component.translatable("hud.modeminecraft.mil.speed", Math.round(kmh)).getString() + turret + alt, x, y, 0xFFD0D0D0);
        y += 11;
        if (s != null) {
            g.drawString(font, Component.translatable("hud.modeminecraft.mil.seat",
                    Component.translatable("seat.modeminecraft." + s.role().name().toLowerCase())), x, y, 0xFFB0C8FF);
        }
        y += 11;
        var info = v.info();
        if (s != null) {
            for (int k = 0; k < 2; k++) {
                int wi = k == 0 ? s.primary() : s.secondary();
                if (wi < 0) {
                    continue;
                }
                MilType.Weapon wpn = t.weapons.get(wi);
                List<MilAmmo> list = wpn.ammo();
                MilAmmo a = list.get(Math.floorMod(info.getInt("sel" + wi), list.size()));
                int n = info.getInt(a.name());
                String ammoName = Component.translatable("item.modeminecraft." + BuiltInRegistries.ITEM.getKey(a.item()).getPath()).getString();
                String txt = (k == 0 ? "[G] " : "[D] ") + Component.translatable("weapon.modeminecraft.mil." + wpn.key()).getString() + " : " + n;
                if (list.size() > 1 || wpn.kind() == MilType.Kind.CANNON) {
                    txt += " " + ammoName;
                }
                g.drawString(font, txt, x, y, n > 0 ? 0xFFFFFFFF : 0xFFE05050);
                int rl = info.getInt("rl" + wi), heat = info.getInt("heat" + wi);
                if (wpn.kind() == MilType.Kind.CANNON && rl > 0) {
                    float f = 1 - rl / (float) wpn.cooldown();
                    g.fill(x + w - 50, y + 2, x + w - 50 + (int) (40 * f), y + 6, 0xFFE0C040);
                } else if (heat > 0) {
                    g.fill(x + w - 50, y + 2, x + w - 50 + (int) (40 * heat / 100f), y + 6, heat > 80 ? 0xFFE03030 : 0xFFE09030);
                }
                y += 11;
            }
        }
        // état : modules, fumigènes, protection active, optiques
        StringBuilder st = new StringBuilder();
        if (t.has(MilType.Flag.SMOKE)) {
            st.append(Component.translatable("hud.modeminecraft.mil.smoke", info.getInt("smoke")).getString()).append("  ");
        }
        if (t.has(MilType.Flag.APS)) {
            st.append("APS ").append(info.getInt("aps")).append("  ");
        }
        if (zoom > 1) {
            st.append("x").append(zoom).append("  ");
        }
        if (thermal) {
            st.append(Component.translatable("hud.modeminecraft.mil.thermal").getString());
        }
        g.drawString(font, st.toString(), x, y, 0xFFA0E0A0);
        y += 11;
        List<String> warn = new java.util.ArrayList<>();
        if (v.is(MilVehicleEntity.ENGINE_DOWN)) {
            warn.add(Component.translatable("hud.modeminecraft.mil.engine_down").getString());
        }
        if (v.is(MilVehicleEntity.MOBILITY_DOWN)) {
            warn.add(Component.translatable(t.tracked() ? "hud.modeminecraft.mil.tracks_down" : "hud.modeminecraft.mil.wheels_down").getString());
        }
        if (v.is(MilVehicleEntity.BURNING)) {
            warn.add(Component.translatable("hud.modeminecraft.mil.fire").getString());
        }
        if (v.fuel() <= 0) {
            warn.add(Component.translatable("hud.modeminecraft.mil.empty").getString());
        }
        if (!warn.isEmpty() && (v.tickCount / 8) % 2 == 0) {
            g.drawString(font, String.join("  ", warn), x, y + 3, 0xFFFF4040);
        }
        // réticule du tireur, télémètre, verrouillage
        if (s != null && s.primary() >= 0 && mc.options.getCameraType().isFirstPerson()) {
            int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
            int col = v.is(MilVehicleEntity.LOCKED) ? 0xFFFF3030 : 0xFF40FF60;
            g.fill(cx - 30, cy, cx - 8, cy + 1, col);
            g.fill(cx + 8, cy, cx + 30, cy + 1, col);
            g.fill(cx, cy + 8, cx + 1, cy + 22, col);
            for (int i = 1; i <= 3; i++) {
                g.fill(cx - 3, cy + 8 + i * 4, cx + 4, cy + 9 + i * 4, col);
            }
            Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
            var hit = mc.level.clip(new ClipContext(eye, eye.add(mc.player.getLookAngle().scale(800)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            String range = hit.getType() == HitResult.Type.MISS ? "---" : Math.round(eye.distanceTo(hit.getLocation())) + " m";
            g.drawString(font, range, cx + 34, cy - 4, col);
            if (v.is(MilVehicleEntity.LOCKED)) {
                g.drawCenteredString(font, Component.translatable("hud.modeminecraft.mil.locked"), cx, cy - 26, 0xFFFF3030);
            } else if (v.is(MilVehicleEntity.LOCKING)) {
                g.drawCenteredString(font, Component.translatable("hud.modeminecraft.mil.locking"), cx, cy - 26, 0xFFFFC030);
            }
        }
        if (thermal) {
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0x18FFFFFF);
        }
    }

    private static void bar(GuiGraphics g, net.minecraft.client.gui.Font font, int x, int y, String key, float f, int col, String label) {
        g.drawString(font, Component.translatable(key), x, y, 0xFFD0D0D0);
        int bx = x + 62, bw = 80;
        g.fill(bx, y + 1, bx + bw, y + 7, 0xFF303030);
        g.fill(bx, y + 1, bx + (int) (bw * Mth.clamp(f, 0, 1)), y + 7, col);
        g.drawString(font, label, bx + bw + 4, y, 0xFFD0D0D0);
    }

    private MilClient() {}
}

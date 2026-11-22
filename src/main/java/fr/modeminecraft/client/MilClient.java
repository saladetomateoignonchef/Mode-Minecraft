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
    private static int hudLines;
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

    /** Place servie (celle du tireur pour un conducteur seul en mode solo). */
    public static MilType.Seat seat() {
        MilVehicleEntity v = vehicle();
        return v == null ? null : v.weaponSeat(Minecraft.getInstance().player);
    }

    /** Point du monde sous le réticule : lancer de rayon depuis la caméra (blocs puis entités), 300 blocs au plus. */
    private static Vec3 crosshairPoint(MilVehicleEntity v) {
        Minecraft mc = Minecraft.getInstance();
        var cam = mc.gameRenderer.getMainCamera();
        Vec3 eye = cam.getPosition();
        Vec3 look = new Vec3(cam.getLookVector());
        Vec3 end = eye.add(look.scale(300));
        var bh = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        if (bh.getType() != HitResult.Type.MISS) {
            end = bh.getLocation();
        }
        var eh = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(mc.level, mc.player, eye, end,
                new net.minecraft.world.phys.AABB(eye, end).inflate(1), e -> !e.isSpectator() && e.isPickable() && e != v && !v.hasPassenger(e)
                        && !(e instanceof fr.modeminecraft.entity.military.MilPart mp && mp.getParent() == v), 0f);
        return eh != null ? eh.getLocation() : end;
    }

    /** Place armée (clic gauche / droit = armes du véhicule) : la détente est envoyée au serveur comme pour une arme. */
    public static boolean armedSeat(Player p) {
        if (!(p.getVehicle() instanceof MilVehicleEntity v)) {
            return false;
        }
        MilType.Seat s = v.weaponSeat(p);
        return s != null && (s.primary() >= 0 || s.secondary() >= 0) || v.canBomb(p);
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
            MilVehicleEntity.bombSightActive = false;
            return;
        }
        if (v.type().plane()) {
            // B-29 : G train, O trappes, V viseur Norden, Espace largage (H : clé d'armement, voir DEPLOY)
            while (ModKeyMappings.AMMO_TYPE.consumeClick()) {
                send(MilControlPayload.GEAR);
            }
            while (ModKeyMappings.BOMB_BAY.consumeClick()) {
                send(MilControlPayload.BAY);
            }
            while (ModKeyMappings.UNDERBARREL.consumeClick()) {
                if (v.canBomb(mc.player)) {
                    MilVehicleEntity.bombSightActive = !MilVehicleEntity.bombSightActive;
                }
            }
            if (v.canBomb(mc.player)) {
                while (mc.options.keyJump.consumeClick()) {
                    send(MilControlPayload.DROP);
                }
            } else {
                MilVehicleEntity.bombSightActive = false;
            }
            if (!mc.options.getCameraType().isFirstPerson()) {
                MilVehicleEntity.bombSightActive = false;
            }
        }
        MilType.Seat s = seat();
        if ((s != null && (s.primary() >= 0 || s.secondary() >= 0) || v.canBomb(mc.player)) && mc.level != null) {
            Vec3 aim = crosshairPoint(v);
            v.setAim(mc.player, aim);                                   // tourelle locale orientée sans attendre le serveur
            if (mc.player.tickCount % 2 == 0) {
                PacketDistributor.sendToServer(new fr.modeminecraft.network.MilAimPayload(aim.x, aim.y, aim.z));
            }
        }
        while (ModKeyMappings.PRONE.consumeClick()) {
            send(MilControlPayload.SMOKE);
        }
        while (!v.type().plane() && ModKeyMappings.MELEE.consumeClick()) {
            send(MilControlPayload.SEAT);                                // avion : C géré par AirClient (siège éjectable)
            zoom = 1;
        }
        while (ModKeyMappings.RELOAD.consumeClick()) {
            send(MilControlPayload.AMMO);
        }
        while (ModKeyMappings.SIREN.consumeClick()) {
            send(MilControlPayload.DEPLOY);
        }
        if (s != null && s.role() == MilType.Role.OPERATOR) {
            while (mc.options.keyUse.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new FireControlScreen(v));
                }
            }
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

    /** Point d'impact prévu d'une bombe larguée maintenant (vitesse et altitude de l'avion, traînée de la bombe). */
    public static Vec3 bombImpact(MilVehicleEntity v, float partial) {
        Vec3 p = v.getPosition(partial).add(v.toWorld(0.8, 0.9, 0).subtract(v.position()));
        Vec3 vel = v.position().subtract(v.xo, v.yo, v.zo).add(0, -0.1, 0);
        var level = Minecraft.getInstance().level;
        for (int i = 0; i < 600; i++) {
            vel = new Vec3(vel.x * 0.992, Math.max(-3.2, vel.y - 0.045) * 0.998, vel.z * 0.992);
            p = p.add(vel);
            int g = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(p.x), (int) Math.floor(p.z));
            if (p.y <= g) {
                return new Vec3(p.x, g, p.z);
            }
        }
        return p;
    }

    /** Viseur de bombardement Norden : caméra sous le nez vitré, braquée sur le point d'impact prévu. */
    @SubscribeEvent
    public static void onCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        MilVehicleEntity v = vehicle();
        if (v == null || !MilVehicleEntity.bombSightActive) {
            return;
        }
        float partial = (float) event.getPartialTick();
        Vec3 eye = event.getCamera().getPosition();
        Vec3 d = bombImpact(v, partial).subtract(eye);
        event.setYaw((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        event.setPitch((float) Math.toDegrees(Math.atan2(-d.y, Math.hypot(d.x, d.z))));
    }

    /** Vue à la 1re personne du tireur : dans l'axe du canon (viseur au-dessus du tourillon). */
    public static Vec3 cameraPosition(float partial) {
        MilVehicleEntity v = vehicle();
        Minecraft mc = Minecraft.getInstance();
        if (v == null || !mc.options.getCameraType().isFirstPerson()) {
            return null;
        }
        if (MilVehicleEntity.bombSightActive) {
            return v.getPosition(partial).add(v.toWorld(9.3, 0.9, 0).subtract(v.position()));       // sous le nez vitré
        }
        MilType.Seat s = v.weaponSeat(mc.player);
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

    /**
     * Verrouillage sol-air (Avenger, Strela…) : carré jaune pendant l'acquisition (1,5 s), rouge une fois verrouillé,
     * dessiné autour de la cible, visible à travers le relief.
     */
    @SubscribeEvent
    public static void onRenderLevel(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        MilVehicleEntity v = vehicle();
        Minecraft mc = Minecraft.getInstance();
        if (v == null || v.lockId() < 0 || mc.level == null) {
            return;
        }
        Entity t = mc.level.getEntity(v.lockId());
        if (t == null) {
            return;
        }
        boolean locked = v.is(MilVehicleEntity.LOCKED);
        if (!locked && !v.is(MilVehicleEntity.LOCKING)) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 at = t.getPosition(partial).add(0, t.getBbHeight() / 2, 0);
        Vec3 cam = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(event.getCamera().rotation());
        float size = (float) Math.max(1.2, Math.max(t.getBbWidth(), t.getBbHeight()) * 0.8) * (locked ? 1f : 1.3f);
        float w = size * 0.08f;
        org.joml.Matrix4f m = pose.last().pose();
        var buffers = mc.renderBuffers().bufferSource();
        var vc = buffers.getBuffer(net.minecraft.client.renderer.RenderType.textBackgroundSeeThrough());
        int col = locked ? 0xFFFF2020 : 0xFFFFC020;
        int light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        quad(vc, m, -size, -size, size, -size + w, col, light);
        quad(vc, m, -size, size - w, size, size, col, light);
        quad(vc, m, -size, -size, -size + w, size, col, light);
        quad(vc, m, size - w, -size, size, size, col, light);
        pose.popPose();
        buffers.endBatch(net.minecraft.client.renderer.RenderType.textBackgroundSeeThrough());
    }

    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer vc, org.joml.Matrix4f m, float x0, float y0, float x1, float y1, int argb, int light) {
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        vc.addVertex(m, x0, y1, 0).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x1, y1, 0).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x1, y0, 0).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x0, y0, 0).setColor(r, g, b, a).setLight(light);
        // face arrière (le carré reste visible quel que soit le sens du quadrilatère)
        vc.addVertex(m, x0, y0, 0).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x1, y0, 0).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x1, y1, 0).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x0, y1, 0).setColor(r, g, b, a).setLight(light);
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
            this(v, v.type().jet() ? ModSounds.JET_ENGINE_LOOP.get() : v.type().plane() ? ModSounds.PROP_ENGINE_LOOP.get() : v.type().heli() ? ModSounds.ROTOR_LOOP.get()
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
            if (v.type().jet()) {
                volume = 0.5f + v.rotor() * 1.8f + (v.is(MilVehicleEntity.AFTERBURNER) ? 0.8f : 0);
                pitch = 0.6f + v.rotor() * 0.25f + sp * 0.35f;
            } else if (v.type().heli()) {
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
        MilType.Seat own = v.seat(mc.player);
        MilType.Seat s = v.weaponSeat(mc.player);
        var font = mc.font;
        int x = 6, y = 6, w = 190;
        List<Component> lines = new java.util.ArrayList<>();
        int hp = Math.round(v.health() / t.health * 100);
        int lineCount = 6 + (s != null && s.primary() >= 0 ? 1 : 0) + (s != null && s.secondary() >= 0 ? 1 : 0);
        float hs = panelScale();
        g.pose().pushPose();
        g.pose().scale(hs, hs, 1);                                                  // panneau réduit (lisible sans masquer la vue)
        g.fill(x - 3, y - 3, x + w, y + lineCount * 11 + 2, 0x90000000);
        hudLines = lineCount;
        g.drawString(font, Component.translatable("entity.modeminecraft." + t.id).withStyle(net.minecraft.ChatFormatting.BOLD), x, y, 0xFFE8D890);
        y += 11;
        bar(g, font, x, y, "hud.modeminecraft.mil.armor", hp / 100f, hp > 50 ? 0xFF5BC85B : hp > 25 ? 0xFFE0B030 : 0xFFE04040, hp + " %");
        y += 11;
        bar(g, font, x, y, "hud.modeminecraft.mil.fuel", v.fuel(), v.fuel() > 0.2f ? 0xFF4AA0E0 : 0xFFE04040, Math.round(v.fuel() * 100) + " %");
        y += 11;
        float kmh = Math.abs(v.speed()) / (t.heli() ? 0.0095f : 0.0111f);
        String turret = t.geo().turret() != null ? "   " + Component.translatable("hud.modeminecraft.mil.turret").getString() + " "
                + Math.round(Mth.wrapDegrees(v.turret(1)) + 360) % 360 + "°" : "";
        String alt = t.heli() ? "   " + Component.translatable("hud.modeminecraft.mil.altitude").getString() + " "
                + (int) (v.getY() - mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, v.getBlockX(), v.getBlockZ())) + " m" : "";
        g.drawString(font, Component.translatable("hud.modeminecraft.mil.speed", Math.round(kmh)).getString() + turret + alt, x, y, 0xFFD0D0D0);
        y += 11;
        if (own != null) {
            Component role = Component.translatable("seat.modeminecraft." + own.role().name().toLowerCase());
            if (v.solo(mc.player)) {
                role = Component.translatable("hud.modeminecraft.mil.solo", role);
            }
            g.drawString(font, Component.translatable("hud.modeminecraft.mil.seat", role), x, y, 0xFFB0C8FF);
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
        if (s != null && s.role() == MilType.Role.OPERATOR && !t.weapons.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (MilAmmo a : t.weapons.get(0).ammo()) {
                sb.append(Component.translatable("ammo_short.modeminecraft." + a.name().toLowerCase()).getString())
                        .append(" : ").append(info.getInt(a.name())).append("   ");
            }
            g.drawString(font, sb.toString(), x, y, 0xFFFFFFFF);
            y += 11;
            g.drawString(font, Component.translatable(v.is(MilVehicleEntity.DEPLOYED) ? "hud.modeminecraft.mil.console" : "hud.modeminecraft.mil.deploy_hint"),
                    x, y, 0xFFE0C060);
            y += 11;
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
        g.pose().popPose();
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
        g.pose().pushPose();
        g.pose().scale(hs, hs, 1);
        if (v.raisable() && s != null && s.primary() >= 0) {
            boolean up = v.raised();
            // sous le panneau du véhicule (la barre d'action en bas d'écran sert aux messages)
            g.drawString(font, Component.translatable(up ? "hud.modeminecraft.mil.launcher_up" : v.raise(0) > 0 && v.raise(0) < 1
                    ? "hud.modeminecraft.mil.launcher_moving" : "hud.modeminecraft.mil.launcher_down"), 6, 6 + hudLines * 11 + 6, up ? 0xFF60E060 : 0xFFFFB040);
        }
        if (t.plane()) {
            var binfo = v.info();
            int py = 6 + hudLines * 11 + 6;
            boolean nuke = binfo.getBoolean("nuke");
            if (t.bayCapacity() <= 0) {
                py -= 11;                                                          // pas de soute à bombes
            } else {
                g.drawString(font, Component.translatable("hud.modeminecraft.b29.bombs", binfo.getInt("bombs"),
                        Component.translatable(nuke ? "hud.modeminecraft.b29.nuke" : "hud.modeminecraft.b29.conventional")), 6, py, nuke ? 0xFFFF6060 : 0xFFE0D090);
            }
            g.drawString(font, Component.translatable("hud.modeminecraft.b29.status",
                    Component.translatable(v.is(MilVehicleEntity.GEAR_UP) ? "hud.modeminecraft.b29.gear_up" : "hud.modeminecraft.b29.gear_down"),
                    Component.translatable(v.is(MilVehicleEntity.BAY_OPEN) ? "hud.modeminecraft.b29.bay_open" : "hud.modeminecraft.b29.bay_closed")),
                    6, py + 11, v.is(MilVehicleEntity.BAY_OPEN) ? 0xFFFFB040 : 0xFFB0C8FF);
            if (nuke) {
                g.drawString(font, Component.translatable(v.is(MilVehicleEntity.NUKE_ARMED) ? "hud.modeminecraft.b29.armed" : "hud.modeminecraft.b29.safe"),
                        6, py + 22, v.is(MilVehicleEntity.NUKE_ARMED) && (v.tickCount / 6) % 2 == 0 ? 0xFFFF2020 : 0xFF90A090);
            }
            if (v.canBomb(mc.player) && t == MilType.B29_SUPERFORTRESS) {
                g.drawString(font, Component.translatable("hud.modeminecraft.b29.keys"), 6, py + 33, 0xFF909090);
            }
            g.pose().popPose();
            AirClient.hud(g, v, delta, py + (t == MilType.B29_SUPERFORTRESS ? 44 : nuke ? 33 : 22), hs);
            float stallKmh = t.maxSpeed() * 0.45f / 0.0095f;
            if (kmh < stallKmh && v.airborneClient() && v.rotor() > 0.05f) {
                g.drawCenteredString(font, Component.translatable("hud.modeminecraft.b29.stall"), g.guiWidth() / 2, g.guiHeight() / 2 - 40, 0xFFFF3030);
            }
            if (MilVehicleEntity.bombSightActive) {
                // réticule Norden : le centre de l'écran EST le point d'impact prévu
                int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
                for (int i = 0; i < 360; i += 6) {
                    double a = Math.toRadians(i);
                    g.fill(cx + (int) (Math.cos(a) * 18), cy + (int) (Math.sin(a) * 18), cx + (int) (Math.cos(a) * 18) + 1, cy + (int) (Math.sin(a) * 18) + 1, 0xFFFF4040);
                }
                g.fill(cx - 40, cy, cx - 22, cy + 1, 0xFFFF4040);
                g.fill(cx + 22, cy, cx + 40, cy + 1, 0xFFFF4040);
                g.fill(cx, cy - 40, cx + 1, cy - 22, 0xFFFF4040);
                g.fill(cx, cy + 22, cx + 1, cy + 40, 0xFFFF4040);
                Vec3 imp = bombImpact(v, delta.getGameTimeDeltaPartialTick(false));
                g.drawCenteredString(font, Component.translatable("hud.modeminecraft.b29.sight", (int) (v.getY() - imp.y),
                        (int) Math.hypot(imp.x - v.getX(), imp.z - v.getZ())), cx, cy + 48, 0xFFFF6060);
            }
        }
        if (!t.plane()) {
            g.pose().popPose();
        }
        if (thermal) {
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0x18FFFFFF);
        }
    }

    /** Échelle du panneau du véhicule : plus petit quand l'interface de Minecraft est grande. */
    public static float panelScale() {
        double gui = Minecraft.getInstance().getWindow().getGuiScale();
        return (float) Mth.clamp(2.0 / gui, 0.5, 0.8);
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

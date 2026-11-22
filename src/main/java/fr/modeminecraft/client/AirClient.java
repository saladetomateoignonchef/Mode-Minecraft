package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.network.MilControlPayload;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/**
 * Côté client de l'aviation (cahier des charges « Aviation militaire » + idées du QCM) :
 * - commandes : Ctrl postcombustion, C maintenue (ou double appui) siège éjectable, X leurres, R station d'armes ;
 * - HUD tête haute : échelle d'assiette, cap, vitesse et Mach, altitude, station, leurres, alarme « MISSILE » ;
 * - effets : traînées de condensation en haute altitude, flamme de postcombustion, bang supersonique et cône de vapeur
 *   au passage du mur du son, vapeur de bout d'aile en virage serré, fumée d'avarie ; parachute après éjection.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class AirClient {
    /** Vitesse du son du jeu (blocs / tick) : les réacteurs la passent en postcombustion. */
    public static final float MACH1 = 4.0f;
    private static int cHold, lastRelease = -100, warnBeep;
    private static boolean abSent, cWasDown, ejected;
    private static final Map<Integer, Float> LAST_SPEED = new HashMap<>();

    private static MilVehicleEntity plane() {
        MilVehicleEntity v = MilClient.vehicle();
        return v != null && v.type().plane() ? v : null;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        MilVehicleEntity v = plane();
        if (v == null || mc.player == null) {
            cHold = 0;
            cWasDown = false;
            ejected = false;
            if (abSent) {
                abSent = false;
            }
            return;
        }
        boolean pilot = v.seatOf(mc.player) == 0;
        // postcombustion : Ctrl (touche de sprint) maintenue
        boolean ab = pilot && v.type().jet() && mc.screen == null && mc.options.keySprint.isDown();
        if (ab != abSent || pilot && v.type().jet() && v.is(MilVehicleEntity.AFTERBURNER) != ab && mc.player.tickCount % 10 == 0) {
            abSent = ab;
            PacketDistributor.sendToServer(new MilControlPayload(ab ? MilControlPayload.AB_ON : MilControlPayload.AB_OFF));
            if (ab) {
                mc.player.playSound(ModSounds.AFTERBURNER.get(), 1.2f, 1f);
            }
        }
        // C : appui bref = changer de place ; maintenue 0,7 s ou double appui = éjection
        boolean down = ModKeyMappings.MELEE.isDown() && mc.screen == null;
        while (ModKeyMappings.MELEE.consumeClick()) {
            // traité ci-dessous (appui / relâchement)
        }
        if (down) {
            cHold++;
            if (cHold == 14 && !ejected && !v.onGround()) {
                eject();
            }
        } else if (cWasDown) {
            if (cHold < 14) {
                if (mc.player.tickCount - lastRelease < 8 && !v.onGround()) {
                    eject();
                } else {
                    PacketDistributor.sendToServer(new MilControlPayload(MilControlPayload.SEAT));
                }
                lastRelease = mc.player.tickCount;
            }
            cHold = 0;
        }
        cWasDown = down;
        if (pilot && v.is(MilVehicleEntity.MISSILE_WARN) && warnBeep-- <= 0) {
            warnBeep = 20;
            mc.player.playSound(ModSounds.MISSILE_WARNING.get(), 0.9f, 1f);
        }
    }

    private static void eject() {
        ejected = true;
        PacketDistributor.sendToServer(new MilControlPayload(MilControlPayload.EJECT));
    }

    // ------------------------------------------------------------------ effets visuels et sonores
    @SubscribeEvent
    public static void onTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            LAST_SPEED.clear();
            return;
        }
        for (MilVehicleEntity v : mc.level.getEntitiesOfClass(MilVehicleEntity.class, mc.player.getBoundingBox().inflate(256),
                x -> x.type().plane() && !x.is(MilVehicleEntity.WRECK))) {
            fx(mc, v);
        }
        LAST_SPEED.keySet().removeIf(id -> mc.level.getEntity(id) == null);
    }

    private static void fx(Minecraft mc, MilVehicleEntity v) {
        MilType t = v.type();
        float[] d = MilVehicleEntity.dims(t);
        var geo = t.geo();
        float sp = Math.abs(v.speed());
        double h = d[2] * 0.45;
        var level = mc.level;
        // traînées de condensation : bouts d'ailes (réacteurs : tuyères) au-dessus de 150 blocs
        if (v.getY() > 150 && sp > 1.2f && !v.onGround()) {
            double span = geo.halfSpan() * 0.95;
            for (int s = -1; s <= 1; s += 2) {
                Vec3 p = v.toWorld(-d[0] * 0.15, h, s * span);
                level.addParticle(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, 0, 0);
            }
            if (t.jet()) {
                Vec3 p = v.toWorld(-d[0] / 2, h, 0);
                level.addParticle(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, 0, 0);
            }
        }
        // vapeur de bout d'aile en virage serré
        if (Math.abs(v.roll) > 45 && sp > 2f && v.tickCount % 2 == 0) {
            double span = geo.halfSpan() * 0.9;
            for (int s = -1; s <= 1; s += 2) {
                Vec3 p = v.toWorld(-d[0] * 0.1, h, s * span);
                level.addParticle(ParticleTypes.WHITE_ASH, p.x, p.y, p.z, 0, 0, 0);
                level.addParticle(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, 0, 0);
            }
        }
        // postcombustion : flamme à la tuyère
        if (v.is(MilVehicleEntity.AFTERBURNER)) {
            Vec3 back = v.position().subtract(v.xo, v.yo, v.zo).normalize().scale(-0.4);
            for (int s = -1; s <= 1; s += 2) {
                double z = d[1] > 2.4f ? s * 0.9 : 0;
                Vec3 p = v.toWorld(-d[0] / 2 - 0.3, h, z);
                level.addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, back.x, back.y, back.z);
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, back.x * 0.5, back.y * 0.5, back.z * 0.5);
                if (z == 0) {
                    break;
                }
            }
        }
        // avarie : fumée noire derrière l'avion
        if (v.health() < t.health * 0.5f && v.tickCount % 2 == 0) {
            Vec3 p = v.toWorld(-d[0] * 0.3, h, 0);
            level.addParticle(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 0, 0.02, 0);
        }
        // bang supersonique : passage de Mach 1 (cône de vapeur + double détonation)
        Float last = LAST_SPEED.put(v.getId(), sp);
        if (t.jet() && last != null && last < MACH1 && sp >= MACH1 && !v.onGround()) {
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI * 2 / 24;
                Vec3 p = v.toWorld(-d[0] * 0.1, h + Math.cos(a) * 1.6, Math.sin(a) * 1.6);
                level.addParticle(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, 0, 0);
                level.addParticle(ParticleTypes.POOF, p.x, p.y, p.z, 0, 0, 0);
            }
            level.playLocalSound(v.getX(), v.getY(), v.getZ(), ModSounds.SONIC_BOOM.get(), SoundSource.NEUTRAL, 8f, 1f, false);
        }
    }

    // ------------------------------------------------------------------ parachute (éjection, saut du C-130)
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity e = event.getEntity();
        if (e.isPassenger() || e.onGround() || e.isInWater() || !e.hasEffect(MobEffects.SLOW_FALLING) || e.getDeltaMovement().y > -0.02) {
            return;
        }
        var br = Minecraft.getInstance().getBlockRenderer();
        PoseStack pose = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        pose.pushPose();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                double up = (4 - Math.abs(i) - Math.abs(j)) * 0.12;
                BlockState s = ((i + j) & 1) == 0 ? Blocks.WHITE_WOOL.defaultBlockState() : Blocks.GREEN_WOOL.defaultBlockState();
                block(br, pose, buffers, light, s, i * 0.8 - 0.4, 3.6 + up, j * 0.8 - 0.4, 0.8, 0.1, 0.8);
            }
        }
        for (int[] c : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            block(br, pose, buffers, light, Blocks.CHAIN.defaultBlockState(), c[0] * 0.8, 1.6, c[1] * 0.8, 0.05, 2.0, 0.05);
        }
        pose.popPose();
    }

    private static void block(net.minecraft.client.renderer.block.BlockRenderDispatcher br, PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                              int light, BlockState s, double x, double y, double z, double sx, double sy, double sz) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale((float) sx, (float) sy, (float) sz);
        br.renderSingleBlock(s, pose, buffers, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    // ------------------------------------------------------------------ HUD tête haute
    /** Collimateur du pilote : échelle d'assiette, cap, vitesse / Mach, altitude, station d'armes, leurres, alarmes. */
    public static void hud(GuiGraphics g, MilVehicleEntity v, DeltaTracker delta, int panelBottom, float hs) {
        Minecraft mc = Minecraft.getInstance();
        var font = mc.font;
        MilType t = v.type();
        var info = v.info();
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        int col = 0xFF40FF70;
        boolean pilot = v.seatOf(mc.player) == 0;
        float sp = Math.abs(v.speed());
        int kmh = Math.round(sp / 0.0095f);
        int ground = mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, v.getBlockX(), v.getBlockZ());
        int agl = (int) (v.getY() - ground);
        // panneau d'aviation (sous le panneau du véhicule, même échelle réduite)
        int y = panelBottom;
        g.pose().pushPose();
        g.pose().scale(hs, hs, 1);
        if (pilot) {
            g.drawString(font, Component.translatable("hud.modeminecraft.air.station", stationName(v)), 6, y, 0xFFFFE080);
            y += 11;
        }
        if (t.jet() || t.heavy() && t != MilType.B29_SUPERFORTRESS) {
            g.drawString(font, Component.translatable("hud.modeminecraft.air.flares", info.getInt("flares")), 6, y, info.getInt("flares") > 0 ? 0xFFA0E0A0 : 0xFFE06060);
            y += 11;
        }
        if (t == MilType.C_130) {
            g.drawString(font, Component.translatable(info.contains("cargo") ? "hud.modeminecraft.air.cargo" : "hud.modeminecraft.air.no_cargo",
                    info.contains("cargo") ? Component.translatable("entity.modeminecraft." + info.getString("cargo")) : Component.empty(), info.getInt("crates")),
                    6, y, 0xFFB0C8FF);
            y += 11;
            g.drawString(font, Component.translatable(v.is(MilVehicleEntity.BAY_OPEN) ? "hud.modeminecraft.air.ramp_open" : "hud.modeminecraft.air.ramp_closed"),
                    6, y, v.is(MilVehicleEntity.BAY_OPEN) ? 0xFFFFB040 : 0xFFB0C8FF);
            y += 11;
        }
        if (info.getInt("rearm") > 0) {
            float f = info.getInt("rearm") / 200f;
            g.drawString(font, Component.translatable("hud.modeminecraft.air.rearm"), 6, y, 0xFF60E0FF);
            g.fill(80, y + 2, 80 + (int) (80 * f), y + 6, 0xFF60E0FF);
            y += 11;
        }
        if (pilot) {
            g.drawString(font, Component.translatable(t.jet() ? "hud.modeminecraft.air.keys_jet" : "hud.modeminecraft.air.keys"), 6, y, 0xFF909090);
        }
        g.pose().popPose();
        if (!pilot || !mc.options.getCameraType().isFirstPerson() && !v.is(MilVehicleEntity.MISSILE_WARN)) {
            warnings(g, font, v, cx, cy);
            return;
        }
        // collimateur un peu réduit autour du centre de l'écran
        g.pose().pushPose();
        float cs = Math.max(0.6f, hs + 0.1f);
        g.pose().translate(cx, cy, 0);
        g.pose().scale(cs, cs, 1);
        g.pose().translate(-cx, -cy, 0);
        // échelle d'assiette (barres tous les 10°)
        float pitch = v.tilt, roll = v.roll;
        float ppd = 3.0f;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-roll));          // l'horizon tourne à l'opposé de l'inclinaison
        for (int deg = -60; deg <= 60; deg += 10) {
            int yy = Math.round((pitch - deg) * ppd);
            if (Math.abs(yy) > 110) {
                continue;
            }
            int w = deg == 0 ? 70 : 28;
            int gap = deg == 0 ? 18 : 14;
            g.fill(-w - gap, yy, -gap, yy + 1, col);
            g.fill(gap, yy, w + gap, yy + 1, col);
            if (deg != 0) {
                g.fill(-gap - 1, yy + (deg > 0 ? 0 : -4), -gap, yy + (deg > 0 ? 5 : 1), col);
                g.fill(gap, yy + (deg > 0 ? 0 : -4), gap + 1, yy + (deg > 0 ? 5 : 1), col);
                g.drawString(font, Integer.toString(deg), w + gap + 3, yy - 4, col, false);
            }
        }
        pose.popPose();
        // repère de l'avion (« W ») et vecteur vitesse
        g.fill(cx - 12, cy, cx - 4, cy + 1, col);
        g.fill(cx + 4, cy, cx + 12, cy + 1, col);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
        // cap (bande en haut)
        float heading = Mth.positiveModulo(v.getYRot() + 180, 360);
        int top = 18;
        for (int k = -40; k <= 40; k += 5) {
            int hdg = Math.floorMod(Math.round(heading / 5) * 5 + k, 360);
            int xx = cx + Math.round((Math.round(heading / 5) * 5 + k - heading) * 3);
            g.fill(xx, top, xx + 1, top + (hdg % 10 == 0 ? 6 : 3), col);
            if (hdg % 30 == 0) {
                String lbl = hdg == 0 ? "N" : hdg == 90 ? "E" : hdg == 180 ? "S" : hdg == 270 ? "W" : Integer.toString(hdg / 10);
                g.drawCenteredString(font, lbl, xx, top + 8, col);
            }
        }
        g.drawCenteredString(font, String.format("%03d", Math.round(heading) % 360), cx, top - 10, col);
        // vitesse (gauche) et altitude (droite)
        String mach = String.format("M %.2f", sp / MACH1);
        g.drawString(font, kmh + " km/h", cx - 130, cy - 4, col, false);
        g.drawString(font, mach, cx - 130, cy + 8, sp >= MACH1 ? 0xFFFFE040 : col, false);
        g.drawString(font, (int) v.getY() + " m", cx + 100, cy - 4, col, false);
        g.drawString(font, "R " + agl, cx + 100, cy + 8, agl < 30 ? 0xFFFF4040 : col, false);
        if (v.is(MilVehicleEntity.AFTERBURNER)) {
            g.drawString(font, "A/B", cx - 130, cy + 20, 0xFFFF9030, false);
        }
        if (!v.is(MilVehicleEntity.GEAR_UP)) {
            g.drawString(font, "GEAR", cx + 100, cy + 20, 0xFFFFE040, false);
        }
        if (agl < 25 && v.airborneClient() && v.getDeltaMovement().y < -0.3 && v.tickCount / 5 % 2 == 0) {
            g.drawCenteredString(font, Component.translatable("hud.modeminecraft.air.pull_up"), cx, cy + 40, 0xFFFF3030);
        }
        warnings(g, font, v, cx, cy);
        g.pose().popPose();
    }

    /** Nom de la station d'armes du pilote (d'après les infos synchronisées du véhicule). */
    public static Component stationName(MilVehicleEntity v) {
        var info = v.info();
        if (!info.contains("station")) {
            return Component.literal("-");
        }
        int st = info.getInt("station");
        if (st == -1) {
            return Component.translatable("hud.modeminecraft.air.bombs");
        }
        if (st < 0 || st >= v.type().weapons.size()) {
            return Component.literal("-");
        }
        MilType.Weapon w = v.type().weapons.get(st);
        Component n = Component.translatable("weapon.modeminecraft.mil." + w.key());
        if (w.ammo().size() > 1) {
            var a = w.ammo().get(Math.floorMod(info.getInt("sel" + st), w.ammo().size()));
            n = n.copy().append(" (").append(Component.translatable("ammo_short.modeminecraft." + a.name().toLowerCase())).append(")");
        }
        return n;
    }

    private static void warnings(GuiGraphics g, net.minecraft.client.gui.Font font, MilVehicleEntity v, int cx, int cy) {
        if (v.is(MilVehicleEntity.MISSILE_WARN) && v.tickCount / 4 % 2 == 0) {
            g.fill(cx - 40, cy - 64, cx + 40, cy - 50, 0xA0400000);
            g.drawCenteredString(font, Component.translatable("hud.modeminecraft.air.missile"), cx, cy - 61, 0xFFFF2020);
        }
        if (v.is(MilVehicleEntity.STEALTH_BREAK) || v.type().rcs() <= 0 && v.is(MilVehicleEntity.BAY_OPEN)) {
            g.drawCenteredString(font, Component.translatable("hud.modeminecraft.air.stealth_break"), cx, cy - 76, 0xFFFFB040);
        }
    }

    private AirClient() {}
}

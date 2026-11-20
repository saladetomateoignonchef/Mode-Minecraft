package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.drone.DroneEntity;
import fr.modeminecraft.entity.drone.DroneType;
import fr.modeminecraft.network.DroneControlPayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Pilotage d'un drone, côté client : caméra FPV embarquée, commandes (Z/Q/S/D, Espace / Ctrl, souris,
 * clic gauche = action, clic droit = zoom x2,5 ou vision thermique), retour automatique (Maj ou R),
 * HUD (batterie, signal, altitude, armement) et neige analogique quand le drone est brouillé.
 * Le corps du pilote reste immobile (et vulnérable) pendant ce temps.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class DroneClient {
    private static int droneId = -1;
    private static CameraType previousCamera;
    private static boolean zoom, thermal, useWasDown;
    private static boolean fire, exit, detonate;
    private static final RandomSource NOISE = RandomSource.create();
    /** Ticks depuis le début du pilotage : le temps que le drone arrive côté client, on attend au lieu de couper. */
    private static int linkTicks;

    public static void onPilot(int id, boolean start) {
        Minecraft mc = Minecraft.getInstance();
        if (start) {
            droneId = id;
            linkTicks = 0;
            while (ModKeyMappings.RELOAD.consumeClick()) { /* un appui sur R en attente ne doit pas faire rentrer le drone */ }
            previousCamera = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            zoom = thermal = false;
            if (mc.level != null && mc.level.getEntity(id) instanceof DroneEntity d) {
                mc.setCameraEntity(d);
            }
        } else if (id == droneId || id < 0) {
            stop();
        }
    }

    private static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (droneId >= 0) {
            if (mc.player != null) {
                mc.setCameraEntity(mc.player);
            }
            if (previousCamera != null) {
                mc.options.setCameraType(previousCamera);
            }
        }
        droneId = -1;
        zoom = thermal = false;
    }

    public static boolean piloting() {
        return droneId >= 0;
    }

    public static boolean thermalActive() {
        return piloting() && thermal;
    }

    public static DroneEntity drone() {
        Minecraft mc = Minecraft.getInstance();
        return droneId >= 0 && mc.level != null && mc.level.getEntity(droneId) instanceof DroneEntity d ? d : null;
    }

    /** Avant le traitement des touches : on lit les commandes du drone et on « avale » les clics du joueur. */
    @SubscribeEvent
    public static void onTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!piloting()) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            droneId = -1;
            return;
        }
        var o = mc.options;
        boolean screen = mc.screen != null;
        fire = !screen && o.keyAttack.isDown();
        boolean use = !screen && o.keyUse.isDown();
        if (use && !useWasDown) {
            DroneEntity d = drone();
            if (d != null && d.droneType() == DroneType.REAPER) {
                thermal = !thermal;                                // optique thermique
            } else {
                zoom = !zoom;                                      // zoom optique x2,5
            }
        }
        useWasDown = use;
        // pendant la seconde de décollage, Maj (encore enfoncée après l'association) ne coupe pas la liaison
        exit = !screen && linkTicks > fr.modeminecraft.entity.drone.DroneEntity.LINK_GRACE
                && (o.keyShift.isDown() || ModKeyMappings.RELOAD.consumeClick());
        // touche N (module de casque) : pendant le pilotage, elle déclenche la charge du drone kamikaze
        while (ModKeyMappings.HELMET_MODULE.consumeClick()) {
            detonate = !screen;
        }
        while (o.keyAttack.consumeClick()) { /* pas de coup de poing pendant le pilotage */ }
        while (o.keyUse.consumeClick()) { /* ni d'utilisation d'objet */ }
        while (o.keyPickItem.consumeClick()) { /* ni de choix de bloc */ }
        while (o.keyDrop.consumeClick()) { /* ni de jeter la télécommande */ }
    }

    @SubscribeEvent
    public static void onTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!piloting() || mc.player == null) {
            return;
        }
        linkTicks++;
        DroneEntity d = drone();
        if (d == null && linkTicks < 60) {
            return;                                                // drone pas encore reçu par le client : on patiente
        }
        if (d == null || !d.isAlive()) {
            stop();
            return;
        }
        if (mc.getCameraEntity() != d) {
            mc.setCameraEntity(d);
        }
        var o = mc.options;
        boolean screen = mc.screen != null;
        float forward = screen ? 0 : (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        float strafe = screen ? 0 : (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
        float vertical = screen ? 0 : (o.keyJump.isDown() ? 1 : 0) - (o.keySprint.isDown() ? 1 : 0);
        byte buttons = (byte) ((fire ? 1 : 0) | (exit ? 2 : 0) | (detonate ? 4 : 0));
        detonate = false;
        PacketDistributor.sendToServer(new DroneControlPayload(droneId, forward, strafe, vertical, mc.player.getYRot(), mc.player.getXRot(), buttons));
        if (exit) {
            stop();                                                // le drone rentre seul (RTH)
        }
    }

    /** Le corps du pilote ne bouge pas. */
    @SubscribeEvent
    public static void onMovement(MovementInputUpdateEvent event) {
        if (piloting()) {
            var in = event.getInput();
            in.forwardImpulse = 0;
            in.leftImpulse = 0;
            in.up = in.down = in.left = in.right = false;
            in.jumping = false;
            in.shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (piloting()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** La caméra du drone suit la souris du pilote. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        DroneEntity d = drone();
        if (d != null && mc.getCameraEntity() == d && mc.player != null) {
            float partial = (float) event.getPartialTick();
            event.setYaw(mc.player.getViewYRot(partial));
            event.setPitch(mc.player.getViewXRot(partial));
            var v = d.getDeltaMovement();
            float yaw = mc.player.getViewYRot(partial) * Mth.DEG_TO_RAD;
            double side = v.x * Mth.cos(yaw) + v.z * Mth.sin(yaw);
            event.setRoll((float) Mth.clamp(side * 30, -12, 12));  // léger roulis en virage
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (piloting() && zoom) {
            event.setFOV(event.getFOV() / 2.5);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (piloting()) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ HUD caméra FPV
    public static void renderHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        DroneEntity d = drone();
        if (d == null || mc.player == null || mc.options.hideGui) {
            return;
        }
        int w = g.guiWidth(), h = g.guiHeight();
        int green = 0xFF7CFF8A, amber = 0xFFFFC040, red = 0xFFFF4040;
        // coins du cadre
        int m = 18, l = 22;
        for (int[] c : new int[][]{{m, m, 1, 1}, {w - m, m, -1, 1}, {m, h - m, 1, -1}, {w - m, h - m, -1, -1}}) {
            g.fill(Math.min(c[0], c[0] + c[2] * l), c[1], Math.max(c[0], c[0] + c[2] * l), c[1] + c[3], green);
            g.fill(c[0], Math.min(c[1], c[1] + c[3] * l), c[0] + c[2], Math.max(c[1], c[1] + c[3] * l), green);
        }
        // réticule
        int cx = w / 2, cy = h / 2;
        g.fill(cx - 9, cy, cx - 3, cy + 1, green);
        g.fill(cx + 4, cy, cx + 10, cy + 1, green);
        g.fill(cx, cy - 9, cx + 1, cy - 3, green);
        g.fill(cx, cy + 4, cx + 1, cy + 10, green);
        DroneType t = d.droneType();
        if (t == DroneType.MAVIC) {
            g.drawCenteredString(mc.font, "▼", cx, cy + 14, amber);   // point de largage à la verticale
        }
        // enregistrement
        if ((mc.level.getGameTime() / 10) % 2 == 0) {
            g.fill(m + 6, m + 6, m + 11, m + 11, red);
        }
        g.drawString(mc.font, Component.translatable("item.modeminecraft." + t.id), m + 15, m + 5, green, false);
        // batterie
        float bat = d.battery() / (float) d.maxBattery();
        int bx = w - m - 64, by = m + 6;
        g.fill(bx, by, bx + 50, by + 8, 0x80000000);
        g.fill(bx + 1, by + 1, bx + 1 + (int) (48 * bat), by + 7, bat < 0.2f ? red : bat < 0.5f ? amber : green);
        g.drawString(mc.font, (int) (bat * 100) + "%", bx + 52, by, green, false);
        // signal
        double dist = d.distanceTo(mc.player);
        double range = Math.min(t.range, mc.options.getEffectiveRenderDistance() * 16 - 24);
        float sig = (float) Mth.clamp(1 - dist / range, 0, 1) * (1 - d.interference());
        int bars = sig > 0.75 ? 4 : sig > 0.5 ? 3 : sig > 0.25 ? 2 : sig > 0.05 ? 1 : 0;
        for (int i = 0; i < 4; i++) {
            g.fill(bx + i * 5, by + 22 - i * 3 - 3, bx + i * 5 + 3, by + 22, i < bars ? (bars <= 1 ? red : green) : 0x60FFFFFF);
        }
        g.drawString(mc.font, String.format("%.0f m", dist), bx + 24, by + 14, sig < 0.25f ? red : green, false);
        // altitude
        int ground = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, d.getBlockX(), d.getBlockZ());
        g.drawString(mc.font, Component.translatable("gui.modeminecraft.drone.alt", String.format("%.1f", d.getY() - ground)), m + 6, h - m - 14, green, false);
        // armement
        Component arms = switch (t) {
            case HORNET -> Component.translatable("gui.modeminecraft.drone.mark");
            case FPV -> Component.translatable(d.payload().isEmpty() ? "gui.modeminecraft.drone.no_charge" : "gui.modeminecraft.drone.charge_n",
                    d.payload().getHoverName());
            case MAVIC -> Component.translatable("gui.modeminecraft.drone.grenades", d.arms());
            case WASP -> Component.translatable("gui.modeminecraft.drone.rounds", d.arms());
            case REAPER -> Component.translatable("gui.modeminecraft.drone.rockets", d.arms());
        };
        if (d.hasCargo() && t != DroneType.HORNET) {
            arms = Component.translatable("gui.modeminecraft.drone.cargo");
        }
        g.drawString(mc.font, arms, w - m - 6 - mc.font.width(arms), h - m - 14, amber, false);
        if (zoom) {
            g.drawCenteredString(mc.font, "x2.5", cx, h - m - 14, green);
        }
        if (thermal) {
            g.drawCenteredString(mc.font, Component.translatable("gui.modeminecraft.drone.thermal"), cx, h - m - 14, 0xFFFF60FF);
        }
        if (t == DroneType.WASP && !d.hasCargo()) {
            g.drawCenteredString(mc.font, "·", cx, cy - 4, 0xFF40FF60);         // point du laser vert
        }
        g.drawCenteredString(mc.font, Component.translatable("gui.modeminecraft.drone.keys"), cx, h - 12, 0xA0FFFFFF);
        // neige analogique (tour de brouillage, fusil EMP)
        float noise = d.interference();
        if (sig < 0.15f) {
            noise = Math.max(noise, 0.35f);
        }
        if (noise > 0) {
            int n = (int) (noise * noise * 2400);
            for (int i = 0; i < n; i++) {
                int x = NOISE.nextInt(w), y = NOISE.nextInt(h);
                int v = NOISE.nextInt(256);
                g.fill(x, y, x + 2, y + 1, 0xFF000000 | v << 16 | v << 8 | v);
            }
            if ((mc.level.getGameTime() / 3) % 7 == 0) {
                int y = NOISE.nextInt(h);
                g.fill(0, y, w, y + 2 + NOISE.nextInt(6), 0x60FFFFFF);
            }
        }
        if (d.jammed()) {
            g.fill(0, 0, w, h, 0x50000000);
            g.drawCenteredString(mc.font, Component.translatable("gui.modeminecraft.drone.jammed"), cx, cy - 30, red);
        } else if (noise >= 0.99f) {
            g.drawCenteredString(mc.font, Component.translatable("gui.modeminecraft.drone.interference"), cx, cy - 30, red);
        }
    }

    private DroneClient() {}
}

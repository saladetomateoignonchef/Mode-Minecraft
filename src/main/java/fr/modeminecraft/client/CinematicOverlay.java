package fr.modeminecraft.client;

import fr.modeminecraft.registry.ModSounds;
import fr.modeminecraft.scenario.ScenarioClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Cinématiques du scénario : bandes noires qui se referment, carte titre, sous-titres radio tapés à la machine et,
 * pour l'alerte au bombardier, un écran radar (balayage, votre base au centre, l'écho du bombardier).
 */
public final class CinematicOverlay {
    private CinematicOverlay() {}

    private static String kind;
    private static List<Integer> data = List.of();
    private static long start;
    private static int duration;

    public static void start(String k, List<Integer> d) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        kind = k;
        data = d;
        start = System.currentTimeMillis();
        duration = switch (k) {
            case "intro" -> 220;
            case "bomber" -> 260;
            default -> 160;
        };
        var sound = k.equals("bomber") ? ModSounds.AIR_RAID_ALARM.get() : ModSounds.RADIO_CALL_CUT.get();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1f, k.equals("bomber") ? 0.6f : 1f));
    }

    public static boolean active() {
        return kind != null;
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (kind == null || mc.level == null) {
            return;
        }
        float t = (System.currentTimeMillis() - start) / 50f;                  // en ticks, indépendant de l'heure du monde
        if (t > duration || t < 0) {
            kind = null;
            return;
        }
        int w = g.guiWidth(), h = g.guiHeight();
        float in = Mth.clamp(t / 15f, 0, 1), out = Mth.clamp((duration - t) / 15f, 0, 1);
        int bar = (int) (h * 0.12f * Math.min(in, out));
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        int alpha = (int) (255 * Math.min(Mth.clamp((t - 8) / 12f, 0, 1), out));
        if (alpha < 8) {
            return;
        }
        String side = ScenarioClientState.side == null ? "us" : ScenarioClientState.side.id;
        Component title = Component.translatable("cinematic.modeminecraft." + kind + ".title");
        g.pose().pushPose();
        g.pose().translate(w / 2f, h * 0.24f, 0);
        g.pose().scale(2f, 2f, 1);
        g.drawCenteredString(mc.font, title, 0, 0, (alpha << 24) | (kind.equals("bomber") || kind.equals("struck") ? 0xFF4040 : 0xFFE8B0));
        g.pose().popPose();
        // sous-titres radio, tapés à la machine
        String text = Component.translatable("cinematic.modeminecraft." + kind + "." + side).getString();
        int chars = (int) Math.max(0, (t - 20) * 1.6f);
        String shown = text.substring(0, Math.min(text.length(), chars));
        int y = (int) (h * 0.24f) + 26;                                      // sous le titre (le chat reste lisible)
        for (var line : mc.font.split(Component.literal(shown), Math.min(360, w - 40))) {
            g.drawCenteredString(mc.font, line, w / 2, y, (alpha << 24) | 0xD8D8D8);
            y += 10;
        }
        if (kind.equals("bomber") && data.size() >= 4) {
            radar(g, mc, t, w - 80, h / 2, 50, alpha);
        }
    }

    private static void radar(GuiGraphics g, Minecraft mc, float t, int cx, int cy, int r, int alpha) {
        int green = (alpha << 24) | 0x30E060;
        g.fill(cx - r - 2, cy - r - 2, cx + r + 3, cy + r + 3, (Math.min(alpha, 200) << 24));
        for (int k = 0; k < 360; k += 3) {
            double a = Math.toRadians(k);
            for (int rr : new int[]{r, r * 2 / 3, r / 3}) {
                int x = cx + (int) (Math.cos(a) * rr), y = cy + (int) (Math.sin(a) * rr);
                g.fill(x, y, x + 1, y + 1, (alpha / 2 << 24) | 0x30E060);
            }
        }
        double sweep = t * 0.12;
        for (int i = 0; i < r; i++) {
            int x = cx + (int) (Math.cos(sweep) * i), y = cy + (int) (Math.sin(sweep) * i);
            g.fill(x, y, x + 1, y + 1, green);
        }
        // votre base au centre, le bombardier à sa position relative (au moins 6 000 blocs d'échelle)
        double dx = data.get(0) - data.get(2), dz = data.get(1) - data.get(3);
        double scale = r / Math.max(1500.0, Math.hypot(dx, dz) * 1.15);
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, (alpha << 24) | 0x40A0FF);
        int bx = cx + (int) (dx * scale), by = cy + (int) (dz * scale);
        if ((int) (t / 6) % 2 == 0) {
            g.fill(bx - 2, by - 2, bx + 3, by + 3, (alpha << 24) | 0xFF3030);
        }
        g.drawCenteredString(mc.font, (int) Math.hypot(dx, dz) + " m", cx, cy + r + 6, green);
    }
}

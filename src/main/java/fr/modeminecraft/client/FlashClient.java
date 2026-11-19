package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/** Flashbang : écran blanc 3 s, puis persistance rétinienne 3 s, avec un acouphène. */
public final class FlashClient {
    private static long start = -1000;
    private static float strength;
    public static float strength() { return strength; }

    public static void flash(float s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || s <= 0.02f) {
            return;
        }
        start = mc.level.getGameTime();
        strength = s;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.FLASH_RING.get(), 1.0f, 0.4f + 0.6f * s));
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float t = mc.level.getGameTime() - start + delta.getGameTimeDeltaPartialTick(false);
        if (t > 120) {
            return;
        }
        int alpha;
        int rgb;
        if (t < 60) { // écran blanc 3 s
            alpha = (int) (255 * strength * (t < 40 ? 1 : 1 - (t - 40) / 40f));
            rgb = 0xFFFFFF;
        } else { // persistance rétinienne : voile jaunâtre qui s'efface
            alpha = (int) (150 * strength * (1 - (t - 60) / 60f));
            rgb = 0xFFF3C0;
        }
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), Math.max(0, Math.min(255, alpha)) << 24 | rgb);
    }

    private FlashClient() {}
}

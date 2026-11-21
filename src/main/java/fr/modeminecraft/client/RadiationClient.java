package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.nuclear.FlashPayload;
import fr.modeminecraft.nuclear.RadiationPayload;
import fr.modeminecraft.nuclear.RadiationSuitItem;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Côté client des zones radioactives : ciel verdâtre et brumeux, cendres radioactives, compteur Geiger (crépitement
 * dans la barre d'action, écran quand on le tient en main) et flash des explosions nucléaires.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class RadiationClient {
    private static List<RadiationPayload.Entry> zones = new ArrayList<>();
    private static float flash;
    private static double intensity, nearest = Double.MAX_VALUE;
    private static RadiationPayload.Entry current;
    private static int idleClick;

    public static void zones(RadiationPayload p) {
        zones = new ArrayList<>(p.zones());
    }

    public static void flash(FlashPayload p) {
        flash = Math.max(flash, p.strength());
    }

    public static int zoneCount() {
        return zones.size();
    }

    /** Intensité (0..1) au point de la caméra. */
    public static double intensity() {
        return intensity;
    }

    private static void update(Player p) {
        intensity = 0;
        nearest = Double.MAX_VALUE;
        current = null;
        long now = p.level().getGameTime();
        for (RadiationPayload.Entry z : zones) {
            if (now >= z.expire()) {
                continue;
            }
            double dx = p.getX() - z.x() - 0.5, dz = p.getZ() - z.z() - 0.5, dy = Math.max(0, Math.abs(p.getY() - z.y()) - 40);
            double d = Math.sqrt(dx * dx + dz * dz + dy * dy);
            double k = d >= z.radius() ? 0 : 1 - d / z.radius();
            if (k > intensity || current == null && d - z.radius() < nearest) {
                current = z;
            }
            intensity = Math.max(intensity, k);
            nearest = Math.min(nearest, d - z.radius());
        }
    }

    private static boolean hasGeiger(Player p) {
        if (p.getOffhandItem().is(ModItems.GEIGER_COUNTER.get())) {
            return true;
        }
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).is(ModItems.GEIGER_COUNTER.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean holdingGeiger(Player p) {
        return p.getMainHandItem().is(ModItems.GEIGER_COUNTER.get()) || p.getOffhandItem().is(ModItems.GEIGER_COUNTER.get());
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (flash > 0) {
            flash = Math.max(0, flash - 0.02f);
        }
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        Player p = mc.player;
        update(p);
        // cendres radioactives qui flottent dans l'air
        if (intensity > 0) {
            int n = 1 + (int) (intensity * 10);
            for (int i = 0; i < n; i++) {
                double x = p.getX() + (p.getRandom().nextDouble() - 0.5) * 24, y = p.getY() + p.getRandom().nextDouble() * 12 - 2;
                double z = p.getZ() + (p.getRandom().nextDouble() - 0.5) * 24;
                mc.level.addParticle(p.getRandom().nextBoolean() ? ParticleTypes.WHITE_ASH : ParticleTypes.ASH, x, y, z, 0, -0.02, 0);
            }
        }
        // compteur Geiger : crépitement proportionnel au débit de dose
        if (hasGeiger(p)) {
            double rate;                                                          // clics par seconde
            if (intensity > 0) {
                rate = 2 + Math.pow(intensity, 1.4) * 70;
            } else if (nearest < 20) {
                rate = 0.4 + (20 - nearest) / 20 * 1.6;                          // approche de la zone : clics espacés
            } else {
                rate = 1 / 30.0;
            }
            double expected = rate / 20;
            int clicks = (int) expected + (p.getRandom().nextDouble() < expected - (int) expected ? 1 : 0);
            for (int i = 0; i < Math.min(clicks, 4); i++) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.GEIGER_CLICK.get(), 0.85f + p.getRandom().nextFloat() * 0.3f,
                        0.35f + (float) Math.min(0.4, intensity)));
            }
        }
    }

    /** Débit de dose affiché (µSv/h) : 0,12 µSv/h hors zone, plus de 500 Sv/h au point zéro. */
    static double doseRate(double k) {
        return k <= 0 ? 0.12 : 0.12 * Math.pow(10, k * 9.62);
    }

    static String formatDose(double uSv) {
        if (uSv >= 5e8) {
            return "> 500 Sv/h";
        }
        if (uSv >= 1e6) {
            return String.format("%.1f Sv/h", uSv / 1e6);
        }
        if (uSv >= 1e3) {
            return String.format("%.1f mSv/h", uSv / 1e3);
        }
        return String.format("%.2f µSv/h", uSv);
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        if (flash > 0) {
            int a = (int) (Mth.clamp(flash, 0, 1) * 255);
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), a << 24 | 0xFFFFF8);
        }
        if (mc.player == null || mc.options.hideGui || !holdingGeiger(mc.player)) {
            return;
        }
        var font = mc.font;
        int w = 150, x = g.guiWidth() - w - 8, y = g.guiHeight() / 2 - 40;
        g.fill(x - 4, y - 4, x + w, y + 56, 0xC0201C08);
        g.fill(x - 4, y - 4, x + w, y - 2, 0xFFE0C020);
        g.drawString(font, Component.translatable("item.modeminecraft.geiger_counter").withStyle(ChatFormatting.BOLD), x, y, 0xFFE8D020);
        double dose = doseRate(intensity);
        int col = intensity <= 0 ? 0xFF60E060 : intensity < 0.3 ? 0xFFE0E040 : intensity < 0.7 ? 0xFFFF9030 : 0xFFFF3030;
        g.drawString(font, formatDose(dose), x, y + 12, col);
        // aiguille analogique
        float needle = (float) Math.min(1, Math.log10(dose / 0.12) / 9.62);
        g.fill(x, y + 24, x + w - 8, y + 27, 0xFF403C30);
        g.fill(x, y + 24, x + (int) ((w - 8) * needle), y + 27, col);
        if (current != null && intensity > 0) {
            long left = Math.max(0, current.expire() - mc.level.getGameTime());
            g.drawString(font, Component.translatable("hud.modeminecraft.geiger.left", left / 24000, (left % 24000) / 1000), x, y + 31, 0xFFD0D0D0);
        } else {
            g.drawString(font, Component.translatable(nearest < 20 ? "hud.modeminecraft.geiger.near" : "hud.modeminecraft.geiger.safe"), x, y + 31,
                    nearest < 20 ? 0xFFE0E040 : 0xFF90B090);
        }
        float sat = RadiationSuitItem.saturation(mc.player);
        g.drawString(font, sat < 0 ? Component.translatable("hud.modeminecraft.geiger.no_suit")
                : Component.translatable("hud.modeminecraft.geiger.suit", Math.round(sat * 100)), x, y + 43, sat < 0 ? 0xFFFF7060 : 0xFF80C0FF);
    }

    /** Ciel et brouillard verdâtres et jaunâtres dans la zone contaminée. */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (intensity <= 0) {
            return;
        }
        float k = (float) Math.min(0.75, 0.25 + intensity);
        event.setRed(Mth.lerp(k, event.getRed(), 0.55f));
        event.setGreen(Mth.lerp(k, event.getGreen(), 0.62f));
        event.setBlue(Mth.lerp(k, event.getBlue(), 0.22f));
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (intensity <= 0) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float want = Mth.lerp((float) Math.min(1, intensity * 1.4), far, 40f);
        event.setFarPlaneDistance(Math.min(far, want));
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), want * 0.15f));
        event.setCanceled(true);
    }

    private RadiationClient() {}
}

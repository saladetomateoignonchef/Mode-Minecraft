package fr.modeminecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.ResolvedGun;
import fr.modeminecraft.item.gun.SightOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Affichage à l'écran :
 * - munitions (chargeur / réserve) à droite de la barre d'inventaire ;
 * - réticule ou lunette quand on vise.
 */
public final class GunHudLayer {
    public static final LayeredDraw.Layer AMMO = GunHudLayer::renderAmmo;
    public static final LayeredDraw.Layer SIGHT_OVERLAY = GunHudLayer::renderSight;

    private static void renderAmmo(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        int ammo = GunItem.getAmmo(stack);
        int max = gun.resolve(stack).magazineSize();
        String reserve = player.getAbilities().instabuild ? "∞" : Integer.toString(gun.countReserve(player));
        String text = ammo + " / " + max + "  [" + reserve + "]";

        int x = graphics.guiWidth() / 2 + 91 + 8;
        int y = graphics.guiHeight() - 19;
        graphics.renderItem(new ItemStack(gun.stats().ammo().get()), x, y - 1);
        int color = ammo == 0 ? 0xFF5555 : (ammo <= max / 4 ? 0xFFAA00 : 0xFFFFFF);
        graphics.drawString(mc.font, text, x + 18, y + 4, color, true);
        // Mode de tir, juste au-dessus des munitions
        Component mode = Component.translatable(gun.getFireMode(stack).shortKey());
        graphics.drawString(mc.font, mode, x + 18, y - 6, 0xFFD84A, true);
    }

    private static void renderSight(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ResolvedGun aim = GunClientEvents.aiming(Minecraft.getInstance().player);
        if (aim == null || aim.overlay() == SightOverlay.NONE) {
            return;
        }
        ResourceLocation texture = ModeMinecraft.id(aim.overlay().texture);
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        RenderSystem.enableBlend(); // les réticules sont transparents
        if (aim.overlay().isFullScope()) {
            int size = Math.min(w, h);
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            graphics.blit(texture, x, y, -90, 0.0f, 0.0f, size, size, size, size);
            // Bandes noires autour de la lunette
            graphics.fill(0, 0, w, y, -90, 0xFF000000);
            graphics.fill(0, y + size, w, h, -90, 0xFF000000);
            graphics.fill(0, y, x, y + size, -90, 0xFF000000);
            graphics.fill(x + size, y, w, y + size, -90, 0xFF000000);
        } else {
            // Viseur point rouge / holographique : petit réticule au centre
            int size = aim.overlay() == SightOverlay.HOLO ? 48 : 16;
            graphics.blit(texture, (w - size) / 2, (h - size) / 2, 0, 0.0f, 0.0f, size, size, size, size);
        }
        RenderSystem.disableBlend();
    }

    private GunHudLayer() {}
}

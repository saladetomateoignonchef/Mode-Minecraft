package fr.modeminecraft.client;

import fr.modeminecraft.item.gun.GunItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Affichage à l'écran :
 * - munitions (chargeur / réserve) à droite de la barre d'inventaire ;
 * - lunette du sniper quand on vise.
 */
public final class GunHudLayer {
    private static final ResourceLocation SCOPE = ResourceLocation.withDefaultNamespace("textures/misc/spyglass_scope.png");

    public static final LayeredDraw.Layer AMMO = GunHudLayer::renderAmmo;
    public static final LayeredDraw.Layer SCOPE_OVERLAY = GunHudLayer::renderScope;

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
        int max = gun.stats().magazineSize();
        String reserve = player.getAbilities().instabuild ? "∞" : Integer.toString(gun.countReserve(player));
        String text = ammo + " / " + max + "  [" + reserve + "]";

        int x = graphics.guiWidth() / 2 + 91 + 8;
        int y = graphics.guiHeight() - 19;
        graphics.renderItem(new ItemStack(gun.stats().ammo().get()), x, y - 1);
        int color = ammo == 0 ? 0xFF5555 : (ammo <= max / 4 ? 0xFFAA00 : 0xFFFFFF);
        graphics.drawString(mc.font, text, x + 18, y + 4, color, true);
    }

    private static void renderScope(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!GunClientEvents.isScoping(mc.player)) {
            return;
        }
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int size = Math.min(w, h);
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        RenderSystem.enableBlend(); // le centre de la lunette est transparent
        graphics.blit(SCOPE, x, y, -90, 0.0f, 0.0f, size, size, size, size);
        RenderSystem.disableBlend();
        // Bandes noires autour de la lunette
        graphics.fill(0, 0, w, y, -90, 0xFF000000);
        graphics.fill(0, y + size, w, h, -90, 0xFF000000);
        graphics.fill(0, y, x, y + size, -90, 0xFF000000);
        graphics.fill(x + size, y, w, y + size, -90, 0xFF000000);
    }

    private GunHudLayer() {}
}

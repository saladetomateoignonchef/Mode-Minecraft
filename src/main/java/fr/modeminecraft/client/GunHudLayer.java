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
        String reserve = player.getAbilities().instabuild ? "∞" : Integer.toString(gun.countReserve(player, stack));
        boolean chambered = gun.stats().chamber() && (GunItem.isChambered(stack) || player.getAbilities().instabuild);
        String text = ammo + (chambered ? "+1" : "") + " / " + max + "  [" + reserve + "]";

        int x = graphics.guiWidth() / 2 + 91 + 8;
        int y = graphics.guiHeight() - 19;
        graphics.renderItem(new ItemStack(gun.stats().ammo().get()), x, y - 1);
        int color = ammo == 0 ? 0xFF5555 : (ammo <= max / 4 ? 0xFFAA00 : 0xFFFFFF);
        graphics.drawString(mc.font, text, x + 18, y + 4, color, true);
        // Mode de tir, juste au-dessus des munitions
        Component mode = Component.translatable(gun.getFireMode(stack).shortKey());
        graphics.drawString(mc.font, mode, x + 18, y - 6, 0xFFD84A, true);
        var type = GunItem.getLoadedType(stack);
        if (type != fr.modeminecraft.item.gun.AmmoType.STANDARD) {
            graphics.drawString(mc.font, Component.translatable(type.translationKey()),
                    x + 22 + mc.font.width(mode), y - 6, type.color, true);
        }
    }

    private static void renderSight(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ResolvedGun aim = GunClientEvents.aiming(Minecraft.getInstance().player);
        if (aim == null) {
            return;
        }
        SightOverlay overlay = SightEffects.currentMode(aim).overlay();
        renderSightInfo(graphics, aim);
        if (overlay.texture == null) {
            return;
        }
        ResourceLocation texture = ModeMinecraft.id(overlay.texture);
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        RenderSystem.enableBlend(); // les réticules sont transparents
        if (overlay.isFullScope()) {
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
            int size = switch (overlay) {
                case HOLO, KOBRA, GHOST_RING -> 48;
                default -> 16;
            };
            graphics.blit(texture, (w - size) / 2, (h - size) / 2, 0, 0.0f, 0.0f, size, size, size, size);
        }
        RenderSystem.disableBlend();
    }

    /** Lunette numérique (cible + points de vie), télémètre (distance), zoom variable. */
    private static void renderSightInfo(GuiGraphics graphics, ResolvedGun aim) {
        Minecraft mc = Minecraft.getInstance();
        int cx = graphics.guiWidth() / 2, cy = graphics.guiHeight() / 2;
        var hit = SightEffects.aimedAt();
        int line = cy + 40;
        if (aim.sightModes().size() > 1) {
            String zoom = String.format("x%.1f", SightEffects.currentMode(aim).magnification()).replace(".0", "");
            graphics.drawCenteredString(mc.font, net.minecraft.network.chat.Component.translatable("hud.modeminecraft.zoom", zoom),
                    cx, cy - 52, 0xFFD84A);
        }
        if (hit == null) {
            return;
        }
        if (aim.has(fr.modeminecraft.item.gun.AttachmentItem.Special.RANGEFINDER) && hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            int meters = (int) Math.round(Math.sqrt(hit.getLocation().distanceToSqr(mc.player.getEyePosition())));
            graphics.drawCenteredString(mc.font, net.minecraft.network.chat.Component.translatable("hud.modeminecraft.distance", meters),
                    cx, line, 0xFF5050);
            line += 10;
        }
        if (aim.has(fr.modeminecraft.item.gun.AttachmentItem.Special.DIGITAL)
                && hit instanceof net.minecraft.world.phys.EntityHitResult e && e.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
            graphics.drawCenteredString(mc.font, living.getDisplayName(), cx, line, 0x60FF80);
            graphics.drawCenteredString(mc.font, net.minecraft.network.chat.Component.translatable("hud.modeminecraft.health",
                    String.format("%.1f", living.getHealth()), String.format("%.0f", living.getMaxHealth())), cx, line + 10, 0x60FF80);
        }
    }

    private GunHudLayer() {}
}

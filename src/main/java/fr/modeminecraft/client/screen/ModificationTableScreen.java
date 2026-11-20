package fr.modeminecraft.client.screen;

import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.ResolvedGun;
import fr.modeminecraft.menu.ModificationTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Écran de la table de modification : l'arme au centre, les 4 accessoires autour,
 * les statistiques finales à droite et le choix du skin.
 */
public class ModificationTableScreen extends AbstractContainerScreen<ModificationTableMenu> {
    private static final String[] SLOT_HINTS = {"gui.modeminecraft.slot.sight", "gui.modeminecraft.slot.muzzle",
            "gui.modeminecraft.slot.grip", "gui.modeminecraft.slot.magazine", "gui.modeminecraft.slot.pistol_grip",
            "gui.modeminecraft.slot.stock", "gui.modeminecraft.slot.side_rail"};
    private static final int STATS_X = 98;

    private Button previousSkin, nextSkin;

    public ModificationTableScreen(ModificationTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        previousSkin = addRenderableWidget(Button.builder(Component.literal("<"), b -> click(ModificationTableMenu.BUTTON_PREVIOUS_SKIN))
                .bounds(leftPos + STATS_X, topPos + 74, 12, 14).build());
        nextSkin = addRenderableWidget(Button.builder(Component.literal(">"), b -> click(ModificationTableMenu.BUTTON_NEXT_SKIN))
                .bounds(leftPos + imageWidth - 20, topPos + 74, 12, 14).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hasGun = menu.gunStack().getItem() instanceof GunItem;
        previousSkin.visible = nextSkin.visible = hasGun;
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        // Infobulle "Viseur", "Bouche"... sur les cases vides
        for (int i = 1; i <= SLOT_HINTS.length; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                g.renderTooltip(font, Component.translatable(hint(i)), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            GuiDraw.slot(g, leftPos + slot.x, topPos + slot.y);
        }
        // Lignes qui relient l'arme à ses accessoires
        int gx = leftPos + ModificationTableMenu.GUN_X + 8, gy = topPos + ModificationTableMenu.GUN_Y + 8;
        for (int[] p : ModificationTableMenu.PART_POS) {
            int px = leftPos + p[0] + 8, py = topPos + p[1] + 8;
            g.fill(Math.min(gx, px), py, Math.max(gx, px) + 1, py + 1, 0xFF8B8B8B);
            g.fill(gx, Math.min(gy, py), gx + 1, Math.max(gy, py), 0xFF8B8B8B);
        }
        for (Slot slot : menu.slots) { // on redessine les cases par-dessus les lignes
            GuiDraw.slot(g, leftPos + slot.x, topPos + slot.y);
        }
        // Cases indisponibles pour cette arme : grisées avec une croix
        for (int i = 1; i <= SLOT_HINTS.length; i++) {
            if (menu.slots.get(i) instanceof ModificationTableMenu.PartSlot part && !part.isAvailable()) {
                int x = leftPos + part.x, y = topPos + part.y;
                g.fill(x, y, x + 16, y + 16, 0xA0303030);
                if (menu.gunStack().getItem() instanceof GunItem) {
                    g.drawString(font, "x", x + 5, y + 4, 0xFF5555, false);
                }
            } else if (!menu.slots.get(i).hasItem()) {
                // Lettre d'indice dans la case vide
                String hint = Component.translatable(hint(i)).getString().substring(0, 1);
                Slot s = menu.slots.get(i);
                g.drawString(font, hint, leftPos + s.x + 5, topPos + s.y + 4, 0xFF6F6F6F, false);
            }
        }
        GuiDraw.inset(g, leftPos + STATS_X - 2, topPos + 12, imageWidth - STATS_X - 6, 60);
    }

    private static final String[] DRONE_HINTS = {"gui.modeminecraft.slot.drone_ventral", "gui.modeminecraft.slot.drone_rotors",
            "gui.modeminecraft.slot.drone_battery"};

    private String hint(int i) {
        if (menu.droneMode()) {
            return i <= 3 ? DRONE_HINTS[i - 1] : SLOT_HINTS[i - 1];
        }
        return SLOT_HINTS[i - 1];
    }

    /** Écrit une ligne en la réduisant si elle dépasse la largeur disponible. */
    private void drawFitted(GuiGraphics g, Component text, int x, int y, int maxWidth, int color) {
        float scale = Math.min(1.0f, maxWidth / (float) font.width(text));
        g.pose().pushPose();
        g.pose().translate(x, y + (1 - scale) * 4, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 4, GuiDraw.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiDraw.TEXT, false);

        ItemStack stack = menu.gunStack();
        int x = STATS_X + 1, y = 15;
        if (stack.getItem() instanceof fr.modeminecraft.entity.drone.DroneItem drone) {
            var t = drone.type();
            var d = fr.modeminecraft.entity.drone.DroneData.read(stack);
            int bat = (d.getBoolean("HighCap") ? 10 : 5);
            Component[] lines = {
                    Component.translatable("item.modeminecraft." + t.id),
                    Component.translatable("gui.modeminecraft.drone.stat_range", t.range),
                    Component.translatable("gui.modeminecraft.drone.stat_battery", bat),
                    Component.translatable("gui.modeminecraft.drone.stat_noise", t == fr.modeminecraft.entity.drone.DroneType.HORNET ? t.noise
                            : d.getBoolean("Silent") ? 7 : t.noise),
                    Component.translatable(d.getBoolean("Cargo") ? "gui.modeminecraft.drone.cargo" : "gui.modeminecraft.drone.stat_armed")};
            for (int i = 0; i < lines.length; i++) {
                drawFitted(g, lines[i], x, y + i * 9, imageWidth - STATS_X - 12, i == 0 ? 0xFFD84A : 0xE0E0E0);
            }
            return;
        }
        if (!(stack.getItem() instanceof GunItem gun)) {
            g.drawWordWrap(font, Component.translatable("gui.modeminecraft.place_gun"), x, y + 4, imageWidth - STATS_X - 10, 0xFFFFFF);
            return;
        }
        ResolvedGun r = gun.resolve(stack);
        int c = 0xE0E0E0;
        String dmg = gun.stats().explosionPower() > 0
                ? Component.translatable("tooltip.modeminecraft.gun.explosive").getString()
                : String.format("%.1f", r.damage());
        String zoom = r.zoom() < 1 ? String.format("x%.1f", 1 / r.zoom()) : "-";
        Component[] lines = {
                Component.translatable("gui.modeminecraft.stat.damage_short", dmg),
                Component.translatable("gui.modeminecraft.stat.magazine", r.magazineSize()),
                Component.translatable("gui.modeminecraft.stat.recoil", String.format("%.1f", r.recoil())),
                Component.translatable("gui.modeminecraft.stat.spread", String.format("%.1f", r.aimSpread())),
                Component.translatable("gui.modeminecraft.stat.reload", String.format("%.1f", r.reloadTicks() / 20f)),
                Component.translatable("gui.modeminecraft.stat.zoom", zoom),
        };
        int maxWidth = imageWidth - STATS_X - 12;
        for (int i = 0; i < lines.length; i++) {
            drawFitted(g, lines[i], x, y + i * 9, maxWidth, c);
        }

        // Nom du skin entre les flèches (réduit s'il est trop long)
        Component skin = Component.translatable(GunItem.getSkin(stack).translationKey());
        int available = imageWidth - STATS_X - 34;
        float scale = Math.min(1.0f, available / (float) font.width(skin));
        int center = STATS_X + 14 + available / 2;
        g.pose().pushPose();
        g.pose().translate(center, 78 + (1 - scale) * 4, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, skin, -font.width(skin) / 2, 0, 0xFFD84A, true);
        g.pose().popPose();
    }
}

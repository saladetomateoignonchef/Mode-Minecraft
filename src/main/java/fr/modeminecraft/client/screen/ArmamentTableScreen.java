package fr.modeminecraft.client.screen;

import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunStats;
import fr.modeminecraft.menu.ArmamentTableMenu;
import fr.modeminecraft.recipe.ArmamentRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * Écran de la table d'armement :
 * onglets (armes / munitions / accessoires) | liste des objets | détails + matériaux + bouton "Fabriquer".
 */
public class ArmamentTableScreen extends AbstractContainerScreen<ArmamentTableMenu> {
    private static final int LIST_X = 8, LIST_Y = 34, ROW_H = 18, ROWS = 9, LIST_W = 112;
    private static final int DETAIL_X = 126;

    private ArmamentRecipe.Category category = ArmamentRecipe.Category.WEAPONS;
    private int scroll;
    private int selected = -1; // index dans la liste COMPLÈTE des recettes (= id envoyé au serveur)
    private Button craftButton;
    private final List<Button> tabs = new ArrayList<>();

    public ArmamentTableScreen(ArmamentTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 214;
    }

    @Override
    protected void init() {
        super.init();
        tabs.clear();
        ArmamentRecipe.Category[] categories = ArmamentRecipe.Category.values();
        for (int i = 0; i < categories.length; i++) {
            ArmamentRecipe.Category c = categories[i];
            Button tab = Button.builder(Component.translatable("gui.modeminecraft.category." + c.getSerializedName()), b -> {
                category = c;
                scroll = 0;
                selected = -1;
            }).bounds(leftPos + 8 + i * 80, topPos + 14, 78, 16).build();
            tabs.add(addRenderableWidget(tab));
        }
        craftButton = addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.craft"), b -> craft())
                .bounds(leftPos + DETAIL_X, topPos + 188, 122, 18)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.modeminecraft.craft_hint")))
                .build());
    }

    /** Recettes de l'onglet courant, avec leur index dans la liste complète. */
    private List<Integer> visible() {
        List<RecipeHolder<ArmamentRecipe>> all = menu.recipes();
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).value().category() == category) {
                out.add(i);
            }
        }
        return out;
    }

    private ArmamentRecipe selectedRecipe() {
        List<RecipeHolder<ArmamentRecipe>> all = menu.recipes();
        return selected >= 0 && selected < all.size() ? all.get(selected).value() : null;
    }

    private void craft() {
        if (selected >= 0 && minecraft != null && minecraft.gameMode != null) {
            int id = Screen.hasShiftDown() ? selected + ArmamentTableMenu.CRAFT_MANY : selected;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        ArmamentRecipe recipe = selectedRecipe();
        craftButton.active = recipe != null && recipe.canCraft(minecraft.player);
        craftButton.setMessage(Component.translatable(Screen.hasShiftDown() ? "gui.modeminecraft.craft_many" : "gui.modeminecraft.craft"));
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).active = ArmamentRecipe.Category.values()[i] != category;
        }
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        // Infobulle des objets de la liste
        List<Integer> rows = visible();
        for (int r = 0; r < ROWS && r + scroll < rows.size(); r++) {
            int x = leftPos + LIST_X, y = topPos + LIST_Y + r * ROW_H;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + ROW_H) {
                g.renderTooltip(font, menu.recipes().get(rows.get(r + scroll)).value().result(), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        GuiDraw.inset(g, leftPos + LIST_X - 1, topPos + LIST_Y - 1, LIST_W + 2, ROWS * ROW_H + 2);
        GuiDraw.inset(g, leftPos + DETAIL_X - 1, topPos + LIST_Y - 1, 124, 150);

        // Liste
        List<Integer> rows = visible();
        List<RecipeHolder<ArmamentRecipe>> all = menu.recipes();
        for (int r = 0; r < ROWS && r + scroll < rows.size(); r++) {
            int index = rows.get(r + scroll);
            ArmamentRecipe recipe = all.get(index).value();
            int x = leftPos + LIST_X, y = topPos + LIST_Y + r * ROW_H;
            boolean hover = mouseX >= x && mouseX < x + LIST_W && mouseY >= y && mouseY < y + ROW_H;
            if (index == selected) {
                g.fill(x, y, x + LIST_W, y + ROW_H, 0xFF5B6EE1);
            } else if (hover) {
                g.fill(x, y, x + LIST_W, y + ROW_H, 0xFF9AA0B5);
            }
            g.renderItem(recipe.result(), x + 1, y + 1);
            g.renderItemDecorations(font, recipe.result(), x + 1, y + 1);
            int color = recipe.canCraft(minecraft.player) ? 0xFFFFFF : 0xFF7777;
            g.drawString(font, font.plainSubstrByWidth(recipe.result().getHoverName().getString(), LIST_W - 22),
                    x + 20, y + 5, color, true);
        }
        // Barre de défilement
        if (rows.size() > ROWS) {
            int barH = ROWS * ROW_H * ROWS / rows.size();
            int barY = topPos + LIST_Y + (ROWS * ROW_H - barH) * scroll / (rows.size() - ROWS);
            g.fill(leftPos + LIST_X + LIST_W - 3, barY, leftPos + LIST_X + LIST_W, barY + barH, 0xFF404040);
        }

        renderDetails(g, mouseX, mouseY);
    }

    private void renderDetails(GuiGraphics g, int mouseX, int mouseY) {
        ArmamentRecipe recipe = selectedRecipe();
        int x = leftPos + DETAIL_X + 3, y = topPos + LIST_Y + 3;
        if (recipe == null) {
            g.drawWordWrap(font, Component.translatable("gui.modeminecraft.select_item"), x, y + 40, 116, 0xFFFFFF);
            return;
        }
        ItemStack result = recipe.result();
        // Grande icône (x2)
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(2, 2, 1);
        g.renderItem(result, 0, 0);
        g.pose().popPose();
        g.drawWordWrap(font, result.getHoverName(), x + 36, y + 2, 82, 0xFFFFFF);

        int line = y + 38;
        if (result.getItem() instanceof GunItem gun) {
            GunStats s = gun.stats();
            String damage = s.explosionPower() > 0 ? Component.translatable("tooltip.modeminecraft.gun.explosive").getString()
                    : (s.pellets() > 1 ? s.pellets() + "x" : "") + s.damage();
            g.drawString(font, Component.translatable("tooltip.modeminecraft.gun.damage", damage), x, line, 0xE0E0E0, false);
            g.drawString(font, Component.translatable("gui.modeminecraft.stat.magazine", s.magazineSize()), x, line + 9, 0xE0E0E0, false);
            g.drawString(font, Component.translatable("gui.modeminecraft.stat.rpm", 1200 / s.fireDelay()), x, line + 18, 0xE0E0E0, false);
            line += 29;
        }

        g.drawString(font, Component.translatable("gui.modeminecraft.materials"), x, line, 0xFFD84A, false);
        line += 11;
        for (SizedIngredient ingredient : recipe.ingredients()) {
            ItemStack[] options = ingredient.ingredient().getItems();
            if (options.length == 0) {
                continue;
            }
            // Pour un tag (ex. #planches), on fait défiler les possibilités
            ItemStack shown = options[(int) (System.currentTimeMillis() / 1000 % options.length)];
            int have = ArmamentRecipe.count(minecraft.player.getInventory(), ingredient);
            boolean ok = have >= ingredient.count() || minecraft.player.getAbilities().instabuild;
            g.renderItem(shown, x, line - 4);
            String txt = Math.min(have, 999) + " / " + ingredient.count() + "  " + shown.getHoverName().getString();
            g.drawString(font, font.plainSubstrByWidth(txt, 98), x + 18, line, ok ? 0x80FF80 : 0xFF6060, false);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= line - 4 && mouseY < line + 12) {
                g.renderTooltip(font, shown, mouseX, mouseY);
            }
            line += 13;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 4, GuiDraw.TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Integer> rows = visible();
        for (int r = 0; r < ROWS && r + scroll < rows.size(); r++) {
            int x = leftPos + LIST_X, y = topPos + LIST_Y + r * ROW_H;
            if (mouseX >= x && mouseX < x + LIST_W && mouseY >= y && mouseY < y + ROW_H) {
                selected = rows.get(r + scroll);
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, visible().size() - ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        return true;
    }
}

package fr.modeminecraft.compat.jei;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.recipe.ArmamentRecipe;
import fr.modeminecraft.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/** Catégorie JEI "Table d'armement" : jusqu'à 10 ingrédients (avec quantités) -> l'objet fabriqué. */
public class ArmamentCategory implements IRecipeCategory<ArmamentRecipe> {
    public static final RecipeType<ArmamentRecipe> TYPE = RecipeType.create(ModeMinecraft.MODID, "armament", ArmamentRecipe.class);
    private final IDrawable icon;

    public ArmamentCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(ModItems.ARMAMENT_TABLE.get()));
    }

    @Override
    public RecipeType<ArmamentRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.modeminecraft.armament_table");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 48;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ArmamentRecipe recipe, IFocusGroup focuses) {
        var ings = recipe.ingredients();
        for (int i = 0; i < ings.size(); i++) {
            int count = ings.get(i).count();
            var stacks = Arrays.stream(ings.get(i).ingredient().getItems()).map(s -> s.copyWithCount(count)).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 5) * 18, 1 + (i / 5) * 18).addItemStacks(stacks);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 15).addItemStack(recipe.result());
    }

    @Override
    public void draw(ArmamentRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        g.drawString(Minecraft.getInstance().font, "→", 108, 19, 0xFF404040, false);
        g.drawString(Minecraft.getInstance().font, Component.translatable("gui.modeminecraft.category." + recipe.category().getSerializedName()),
                1, 39, 0xFF606060, false);
    }
}

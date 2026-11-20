package fr.modeminecraft.compat.jei;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.recipe.ArmamentRecipe;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Compatibilité JEI (Just Enough Items) : facultative. Si JEI est installé, il trouve ce plugin tout seul et affiche
 * nos recettes de la table d'armement (établi d'armurier) et les informations de génération du titane.
 * Sans JEI, cette classe n'est jamais chargée.
 */
@JeiPlugin
public class ModJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ModeMinecraft.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ArmamentCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(ArmamentCategory.TYPE, ModRecipes.armamentRecipes(level).stream().map(h -> (ArmamentRecipe) h.value()).toList());
        }
        Component titanium = Component.translatable("jei.modeminecraft.titanium_info");
        for (var item : new net.minecraft.world.level.ItemLike[]{ModItems.TITANIUM_ORE.get(), ModItems.DEEPSLATE_TITANIUM_ORE.get(), ModItems.RAW_TITANIUM.get()}) {
            registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK, titanium);
        }
        registration.addIngredientInfo(new ItemStack(ModItems.MILITARY_FIELD_MANUAL.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("tooltip.modeminecraft.manual"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.ARMAMENT_TABLE.get()), ArmamentCategory.TYPE);
    }
}

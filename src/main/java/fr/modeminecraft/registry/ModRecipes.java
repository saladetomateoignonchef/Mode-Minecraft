package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.recipe.ArmamentRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Comparator;
import java.util.List;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ModeMinecraft.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ModeMinecraft.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ArmamentRecipe>> ARMAMENT =
            TYPES.register("armament", () -> RecipeType.simple(ModeMinecraft.id("armament")));
    public static final DeferredHolder<RecipeSerializer<?>, ArmamentRecipe.Serializer> ARMAMENT_SERIALIZER =
            SERIALIZERS.register("armament", ArmamentRecipe.Serializer::new);

    /** Toutes les recettes de la table d'armement, triées (même ordre côté client et serveur). */
    public static List<RecipeHolder<ArmamentRecipe>> armamentRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ARMAMENT.get()).stream()
                .sorted(Comparator.comparing((RecipeHolder<ArmamentRecipe> r) -> r.value().category())
                        .thenComparing(r -> r.id().toString()))
                .toList();
    }

    private ModRecipes() {}
}

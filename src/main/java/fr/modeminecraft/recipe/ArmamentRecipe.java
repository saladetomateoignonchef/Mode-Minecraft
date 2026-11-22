package fr.modeminecraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/**
 * Recette de la table d'armement : une liste de matériaux (avec quantités) -> un objet.
 * Pas de forme à respecter : la table prend directement les matériaux dans l'inventaire.
 *
 * Exemple de fichier JSON (data/modeminecraft/recipe/armament/ak47.json) :
 * <pre>
 * {
 *   "type": "modeminecraft:armament",
 *   "category": "weapons",
 *   "ingredients": [ {"item": "minecraft:iron_ingot", "count": 8}, {"tag": "minecraft:planks", "count": 4} ],
 *   "result": {"id": "modeminecraft:ak47"}
 * }
 * </pre>
 */
public record ArmamentRecipe(Category category, List<SizedIngredient> ingredients, ItemStack result)
        implements Recipe<SingleRecipeInput> {

    /** Onglets de la table d'armement. */
    public enum Category implements net.minecraft.util.StringRepresentable {
        WEAPONS("weapons"), AMMO("ammo"), SPECIAL_AMMO("special_ammo"), ATTACHMENTS("attachments"), ARMOR("armor"),
        /** Établi d'artillerie : munitions lourdes, roquettes, missiles, carburant. */
        ARTILLERY("artillery"),
        /** Établi d'assemblage : pièces détachées et véhicules complets. */
        VEHICLE_PARTS("vehicle_parts"), VEHICLES("vehicles");

        public static final Codec<Category> CODEC = net.minecraft.util.StringRepresentable.fromEnum(Category::values);
        public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, Category> STREAM_CODEC =
                net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs.enumCodec(Category.class);
        private final String name;

        Category(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<ArmamentRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Category.CODEC.fieldOf("category").forGetter(ArmamentRecipe::category),
            SizedIngredient.FLAT_CODEC.listOf().fieldOf("ingredients").forGetter(ArmamentRecipe::ingredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ArmamentRecipe::result)
    ).apply(i, ArmamentRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmamentRecipe> STREAM_CODEC = StreamCodec.composite(
            Category.STREAM_CODEC.cast(), ArmamentRecipe::category,
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), ArmamentRecipe::ingredients,
            ItemStack.STREAM_CODEC, ArmamentRecipe::result,
            ArmamentRecipe::new);

    /** Les munitions spéciales (traçantes...) ne peuvent pas servir d'ingrédient : seules les normales comptent. */
    private static boolean usable(SizedIngredient ingredient, ItemStack stack) {
        return ingredient.ingredient().test(stack) && !stack.has(ModDataComponents.AMMO_TYPE.get());
    }

    /** Nombre d'objets correspondant à un ingrédient dans l'inventaire du joueur. */
    public static int count(Inventory inventory, SizedIngredient ingredient) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (usable(ingredient, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Quantité exigée pour ce joueur (scénario : bombe moins chère grâce aux plans volés et au scientifique). */
    public int need(Player player, SizedIngredient ingredient) {
        return fr.modeminecraft.scenario.ScenarioCost.need(player, result, ingredient);
    }

    public boolean canCraft(Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        for (SizedIngredient ingredient : ingredients) {
            if (count(player.getInventory(), ingredient) < need(player, ingredient)) {
                return false;
            }
        }
        return true;
    }

    /** Retire les matériaux de l'inventaire (à n'appeler qu'après canCraft). */
    public void consume(Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (SizedIngredient ingredient : ingredients) {
            int remaining = need(player, ingredient);
            for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (usable(ingredient, stack)) {
                    int n = Math.min(remaining, stack.getCount());
                    stack.shrink(n);
                    remaining -= n;
                }
            }
        }
        inventory.setChanged();
    }

    // --- Méthodes imposées par l'interface Recipe (non utilisées : on gère le craft nous-mêmes) ---
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    /** "Spéciale" = ignorée par le livre de recettes vanilla. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ARMAMENT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ARMAMENT.get();
    }

    public static class Serializer implements RecipeSerializer<ArmamentRecipe> {
        @Override
        public MapCodec<ArmamentRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ArmamentRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

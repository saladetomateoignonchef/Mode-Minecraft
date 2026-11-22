package fr.modeminecraft.menu;

import fr.modeminecraft.recipe.ArmamentRecipe;
import fr.modeminecraft.registry.ModBlocks;
import fr.modeminecraft.registry.ModMenus;
import fr.modeminecraft.registry.ModRecipes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * Menu de la table d'armement. Il n'a pas d'emplacements : l'écran affiche la liste des recettes
 * et envoie l'index de la recette choisie au serveur (clickMenuButton).
 */
public class ArmamentTableMenu extends AbstractContainerMenu {
    /** id >= CRAFT_MANY : fabriquer jusqu'à 10 fois d'un coup (Maj + clic). */
    public static final int CRAFT_MANY = 100_000;

    private final ContainerLevelAccess access;
    private final Player player;

    /** Constructeur côté client. */
    public ArmamentTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ArmamentTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        this(ModMenus.ARMAMENT_TABLE.get(), containerId, inventory, access);
    }

    /** Établi d'artillerie : même menu, autres recettes (onglet « Artillerie » seul). */
    public static ArmamentTableMenu artillery(int containerId, Inventory inventory) {
        return new ArmamentTableMenu(ModMenus.ARTILLERY_TABLE.get(), containerId, inventory, ContainerLevelAccess.NULL);
    }

    public static ArmamentTableMenu artillery(int containerId, Inventory inventory, ContainerLevelAccess access) {
        return new ArmamentTableMenu(ModMenus.ARTILLERY_TABLE.get(), containerId, inventory, access);
    }

    public static ArmamentTableMenu assembly(int containerId, Inventory inventory) {
        return new ArmamentTableMenu(ModMenus.ASSEMBLY_TABLE.get(), containerId, inventory, ContainerLevelAccess.NULL);
    }

    public static ArmamentTableMenu assembly(int containerId, Inventory inventory, ContainerLevelAccess access) {
        return new ArmamentTableMenu(ModMenus.ASSEMBLY_TABLE.get(), containerId, inventory, access);
    }

    public boolean isArtillery() {
        return getType() == ModMenus.ARTILLERY_TABLE.get();
    }

    public boolean isAssembly() {
        return getType() == ModMenus.ASSEMBLY_TABLE.get();
    }

    /** Onglets de cette table. */
    public List<ArmamentRecipe.Category> categories() {
        if (isArtillery()) {
            return List.of(ArmamentRecipe.Category.ARTILLERY);
        }
        if (isAssembly()) {
            return List.of(ArmamentRecipe.Category.VEHICLE_PARTS, ArmamentRecipe.Category.VEHICLES);
        }
        return java.util.Arrays.stream(ArmamentRecipe.Category.values()).filter(c -> c.ordinal() < ArmamentRecipe.Category.ARTILLERY.ordinal()).toList();
    }

    private ArmamentTableMenu(net.minecraft.world.inventory.MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(type, containerId);
        this.access = access;
        this.player = inventory.player;
        // Emplacements invisibles de l'inventaire du joueur : ils ne servent qu'à synchroniser
        // l'inventaire avec le client pendant que la table est ouverte (sinon les quantités
        // affichées ne se mettraient pas à jour après une fabrication).
        for (int i = 0; i < 36; i++) {
            addSlot(new Slot(inventory, i, -1000, -1000) {
                @Override
                public boolean isActive() {
                    return false;
                }
            });
        }
    }

    public List<RecipeHolder<ArmamentRecipe>> recipes() {
        List<ArmamentRecipe.Category> cats = categories();
        return ModRecipes.armamentRecipes(player.level()).stream().filter(r -> cats.contains(r.value().category())
                && fr.modeminecraft.scenario.ScenarioCost.visible(r.id(), player)).toList();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int index = id % CRAFT_MANY;
        int times = id >= CRAFT_MANY ? 10 : 1;
        List<RecipeHolder<ArmamentRecipe>> recipes = recipes();
        if (index < 0 || index >= recipes.size()) {
            return false;
        }
        ArmamentRecipe recipe = recipes.get(index).value();
        int crafted = 0;
        while (crafted < times && recipe.canCraft(player)) {
            recipe.consume(player);
            ItemStack result = recipe.result().copy();
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
            crafted++;
        }
        if (crafted > 0) {
            access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE,
                    SoundSource.BLOCKS, 1.0f, 1.0f));
        }
        return crafted > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, isArtillery() ? ModBlocks.ARTILLERY_TABLE.get() : isAssembly() ? ModBlocks.ASSEMBLY_TABLE.get()
                : ModBlocks.ARMAMENT_TABLE.get());
    }
}

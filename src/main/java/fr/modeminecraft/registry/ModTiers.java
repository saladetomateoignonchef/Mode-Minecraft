package fr.modeminecraft.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * Matériaux d'outils / d'armes. Le rubis se place entre le diamant et la netherite.
 */
public final class ModTiers {
    // Pour comparaison : diamant = 1561 utilisations, vitesse 8, bonus dégâts 3 ; netherite = 2031, 9, 4
    public static final Tier RUBIS = new SimpleTier(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, // peut miner tout ce que le diamant mine
            1800,   // durabilité
            8.5f,   // vitesse de minage
            3.5f,   // bonus de dégâts
            15,     // enchantabilité
            () -> Ingredient.of(ModItems.RUBIS.get())); // réparation à l'enclume

    private ModTiers() {}
}

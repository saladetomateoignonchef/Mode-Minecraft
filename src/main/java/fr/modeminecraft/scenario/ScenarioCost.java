package fr.modeminecraft.scenario;

import fr.modeminecraft.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Bombe nucléaire du scénario : la recette classique (cœurs d'uranium enrichi) est remplacée par celle de la Course à
 * l'arme nucléaire (les 4 composants des chambres fortes des boss + l'uranium des milices du désert + titane).
 * Plans volés (Espionnage) : moitié moins de titane ; scientifique exfiltré : moitié moins d'uranium.
 */
public final class ScenarioCost {
    private ScenarioCost() {}

    /** Recettes « scenario/... » : seulement en scénario ; la bombe classique : seulement hors scénario. */
    public static boolean visible(ResourceLocation id, Player p) {
        boolean scenario = Scenario.side(p) != null;
        if (id.getPath().startsWith("scenario/")) {
            return scenario;
        }
        return !(scenario && id.getPath().equals("artillery/strategic_nuclear_bomb"));
    }

    static boolean flag(Player p, String f) {
        if (p instanceof ServerPlayer sp) {
            return Missions.flag(sp, f);
        }
        return ScenarioClientState.missions.getCompound("Flags").getBoolean(f);
    }

    public static int need(Player p, ItemStack result, SizedIngredient ing) {
        int n = ing.count();
        if (!result.is(ModItems.NUCLEAR_BOMB.get()) || Scenario.side(p) == null) {
            return n;
        }
        ItemStack[] items = ing.ingredient().getItems();
        if (items.length == 0) {
            return n;
        }
        if (items[0].is(ModItems.URANIUM_CANISTER.get()) && flag(p, "uranium_half")) {
            return Math.max(1, n / 2);
        }
        if (items[0].is(ModItems.TITANIUM_PLATE.get()) && flag(p, "plans")) {
            return Math.max(1, n / 2);
        }
        return n;
    }
}

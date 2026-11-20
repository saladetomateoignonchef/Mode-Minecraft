package fr.modeminecraft.entity.bot;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Marché noir : les marchands clandestins (égouts, souks, saloons, pontons) rachètent le butin en trop
 * (armes, insignes, or) contre des émeraudes et vendent munitions, soins, piles et cartes d'accès.
 */
final class BlackMarket {
    static MerchantOffers offers(RandomSource r) {
        List<MerchantOffer> sell = new ArrayList<>(List.of(
                sell("ammo_9mm", 32, 3), sell("ammo_556", 24, 4), sell("ammo_762", 20, 5), sell("shotgun_shell", 16, 3),
                sell("bandage", 4, 3), sell("first_aid_kit", 1, 6), sell("adrenaline_syringe", 1, 9), sell("tactical_battery", 4, 5),
                sell("frag_grenade", 2, 6), sell("blue_access_card", 1, 14), sell("red_access_card", 1, 28)));
        List<MerchantOffer> buy = new ArrayList<>(List.of(
                buy("glock17", 1, 4), buy("beretta_m9", 1, 4), buy("mp5", 1, 6), buy("uzi", 1, 5), buy("ak47", 1, 8), buy("m4", 1, 8),
                buy("ak74u", 1, 7), buy("dragunov", 1, 10), buy("remington_870", 1, 5), buy("kar98k", 1, 6),
                buy("falcon_insignia", 1, 12), new MerchantOffer(new ItemCost(Items.GOLD_INGOT, 3), new ItemStack(Items.EMERALD, 2), 16, 1, 0.05f)));
        MerchantOffers out = new MerchantOffers();
        java.util.Collections.shuffle(sell, new java.util.Random(r.nextLong()));
        java.util.Collections.shuffle(buy, new java.util.Random(r.nextLong()));
        sell.stream().limit(6).forEach(out::add);
        buy.stream().limit(5).forEach(out::add);
        // la carte noire : seulement contre un insigne du Faucon Blanc
        out.add(new MerchantOffer(new ItemCost(Items.EMERALD, 40), Optional.of(new ItemCost(HumanBotEntity.item("falcon_insignia"), 1)),
                new ItemStack(HumanBotEntity.item("black_access_card")), 2, 5, 0.05f));
        return out;
    }

    private static MerchantOffer sell(String id, int count, int emeralds) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(HumanBotEntity.item(id), count), 12, 2, 0.05f);
    }

    private static MerchantOffer buy(String id, int count, int emeralds) {
        return new MerchantOffer(new ItemCost(HumanBotEntity.item(id), count), new ItemStack(Items.EMERALD, emeralds), 8, 2, 0.05f);
    }

    private BlackMarket() {}
}

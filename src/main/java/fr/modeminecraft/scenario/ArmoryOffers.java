package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Catalogue de l'armurier : l'arsenal de la faction (pistolets, PM, fusils d'assaut, fusils de précision, mitrailleuses,
 * lance-roquettes, munitions, accessoires) payable en lingots d'or, diamants, émeraudes ou barils de pétrole, et la carte
 * tactique de toutes les bases (4 lingots d'or ou 8 émeraudes). Remise selon le grade ; un Général se sert gratuitement
 * (réquisition, voir {@link Armory}).
 */
public final class ArmoryOffers {
    private ArmoryOffers() {}

    /** Article : objet (et quantité), prix principal et éventuel second prix ; side = null : les deux camps. */
    public record Entry(String item, int count, Item cur, int price, Item cur2, int price2, Side side) {
        public ItemStack stack() {
            Item i = BuiltInRegistries.ITEM.get(ModeMinecraft.id(item));
            return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i, count);
        }
    }

    private static final Item GOLD = Items.GOLD_INGOT, DIAMOND = Items.DIAMOND, EMERALD = Items.EMERALD;

    private static Item oil() {
        return ModItems.OIL_BARREL.get();
    }

    private static List<Entry> catalogue;

    /** Catalogue complet (même ordre côté client et serveur : les index servent aux réquisitions). */
    public static List<Entry> catalogue() {
        if (catalogue == null) {
            List<Entry> l = new ArrayList<>();
            Side us = Side.US, su = Side.SOVIET;
            // pistolets
            l.add(new Entry("beretta_m9", 1, GOLD, 3, null, 0, us));
            l.add(new Entry("colt_m1911", 1, GOLD, 3, null, 0, us));
            l.add(new Entry("sig_p226", 1, GOLD, 4, null, 0, us));
            l.add(new Entry("makarov", 1, GOLD, 2, null, 0, su));
            l.add(new Entry("desert_eagle", 1, GOLD, 6, null, 0, null));
            // pistolets-mitrailleurs
            l.add(new Entry("mp5", 1, GOLD, 6, EMERALD, 4, us));
            l.add(new Entry("mp7", 1, GOLD, 7, EMERALD, 4, us));
            l.add(new Entry("pp19_bizon", 1, GOLD, 5, EMERALD, 4, su));
            l.add(new Entry("ak74u", 1, GOLD, 6, EMERALD, 4, su));
            // fusils d'assaut
            l.add(new Entry("m4", 1, DIAMOND, 3, null, 0, us));
            l.add(new Entry("m16a4", 1, DIAMOND, 3, null, 0, us));
            l.add(new Entry("hk416", 1, DIAMOND, 4, null, 0, us));
            l.add(new Entry("scar_h", 1, DIAMOND, 5, null, 0, us));
            l.add(new Entry("ak47", 1, DIAMOND, 3, null, 0, su));
            l.add(new Entry("ak74", 1, DIAMOND, 3, null, 0, su));
            l.add(new Entry("ak12", 1, DIAMOND, 4, null, 0, su));
            l.add(new Entry("an94", 1, DIAMOND, 5, null, 0, su));
            // fusils à pompe
            l.add(new Entry("remington_870", 1, GOLD, 5, null, 0, us));
            l.add(new Entry("benelli_m4", 1, DIAMOND, 3, null, 0, us));
            l.add(new Entry("spas12", 1, DIAMOND, 3, null, 0, su));
            // fusils de précision
            l.add(new Entry("m14_ebr", 1, DIAMOND, 5, null, 0, us));
            l.add(new Entry("m24", 1, DIAMOND, 6, null, 0, us));
            l.add(new Entry("barrett_m82", 1, DIAMOND, 10, EMERALD, 8, us));
            l.add(new Entry("dragunov", 1, DIAMOND, 5, null, 0, su));
            l.add(new Entry("vss", 1, DIAMOND, 6, null, 0, su));
            // mitrailleuses
            l.add(new Entry("m249", 1, DIAMOND, 8, null, 0, us));
            l.add(new Entry("m60", 1, DIAMOND, 9, null, 0, us));
            l.add(new Entry("pkm", 1, DIAMOND, 8, null, 0, su));
            // lance-roquettes et lance-grenades (barils de pétrole)
            l.add(new Entry("m72_law", 1, oil(), 1, DIAMOND, 2, us));
            l.add(new Entry("at4", 1, oil(), 2, DIAMOND, 2, us));
            l.add(new Entry("m32_mgl", 1, oil(), 2, DIAMOND, 4, us));
            l.add(new Entry("javelin", 1, oil(), 4, DIAMOND, 6, us));
            l.add(new Entry("stinger", 1, oil(), 4, DIAMOND, 6, null));
            l.add(new Entry("rpg7", 1, oil(), 2, DIAMOND, 3, su));
            // munitions
            l.add(new Entry("ammo_9mm", 32, EMERALD, 1, null, 0, us));
            l.add(new Entry("ammo_45acp", 24, EMERALD, 1, null, 0, us));
            l.add(new Entry("ammo_46", 32, EMERALD, 2, null, 0, us));
            l.add(new Entry("ammo_556", 30, EMERALD, 2, null, 0, us));
            l.add(new Entry("ammo_762", 30, EMERALD, 2, null, 0, null));
            l.add(new Entry("ammo_545", 30, EMERALD, 2, null, 0, su));
            l.add(new Entry("ammo_762x54r", 20, EMERALD, 2, null, 0, su));
            l.add(new Entry("ammo_50bmg", 10, EMERALD, 4, null, 0, us));
            l.add(new Entry("ammo_50ae", 14, EMERALD, 2, null, 0, null));
            l.add(new Entry("shotgun_shell", 16, EMERALD, 1, null, 0, null));
            l.add(new Entry("grenade_40mm", 6, EMERALD, 3, null, 0, us));
            l.add(new Entry("javelin_missile", 1, oil(), 1, EMERALD, 2, us));
            l.add(new Entry("stinger_missile", 1, oil(), 1, EMERALD, 2, null));
            l.add(new Entry("rpg_rocket", 2, oil(), 1, EMERALD, 1, su));
            l.add(new Entry("ammo_box_50", 2, EMERALD, 3, null, 0, null));
            // gadgets
            l.add(new Entry("portable_jammer", 1, EMERALD, 8, null, 0, null));
            l.add(new Entry("tactical_battery", 4, EMERALD, 2, null, 0, null));
            l.add(new Entry("zip_ties", 4, EMERALD, 2, null, 0, null));
            // accessoires
            l.add(new Entry("thermal_scope", 1, DIAMOND, 8, null, 0, null));
            // carte tactique de toutes les bases (prix volontairement modique)
            l.add(new Entry("strategic_bases_map", 1, GOLD, 4, null, 0, null));
            l.add(new Entry("strategic_bases_map", 1, EMERALD, 8, null, 0, null));
            catalogue = l;
        }
        return catalogue;
    }

    /** Articles proposés à un camp (index dans le catalogue). */
    public static List<Integer> forSide(Side side) {
        List<Integer> out = new ArrayList<>();
        List<Entry> c = catalogue();
        for (int i = 0; i < c.size(); i++) {
            if ((c.get(i).side() == null || c.get(i).side() == side) && !c.get(i).stack().isEmpty()) {
                out.add(i);
            }
        }
        return out;
    }

    public static int price(int base, Grade g) {
        return Math.max(1, Math.round(base * (1 - Math.min(0.9f, g.discount))));
    }

    /** Boutique (interface de marchand) : prix réduits selon le grade. */
    public static MerchantOffers offers(Side side, Grade g) {
        MerchantOffers out = new MerchantOffers();
        if (side == null) {
            return out;
        }
        for (int i : forSide(side)) {
            Entry e = catalogue().get(i);
            Optional<ItemCost> b = e.cur2() == null ? Optional.empty() : Optional.of(new ItemCost(e.cur2(), price(e.price2(), g)));
            out.add(new MerchantOffer(new ItemCost(e.cur(), price(e.price(), g)), b, e.stack(), 999, 0, 0f));
        }
        return out;
    }
}

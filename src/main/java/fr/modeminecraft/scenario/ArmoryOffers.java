package fr.modeminecraft.scenario;

import net.minecraft.world.item.trading.MerchantOffers;

/** Catalogue de l'armurier (complété avec la phase « armurier »). */
public final class ArmoryOffers {
    private ArmoryOffers() {}

    public static MerchantOffers offers(Side side, Grade g) {
        return new MerchantOffers();
    }
}

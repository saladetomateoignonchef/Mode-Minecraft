package fr.modeminecraft.scenario;

import fr.modeminecraft.entity.bot.HumanBotEntity;
import net.minecraft.server.level.ServerPlayer;

/** Armurier de la base (boutique, contrats, carte des bases) : voir la phase suivante du scénario. */
public final class Armory {
    private Armory() {}

    public static void open(HumanBotEntity bot, ServerPlayer p) {
        bot.openShop(p, ArmoryOffers.offers(Scenario.side(p), Reputation.grade(p)));
    }
}

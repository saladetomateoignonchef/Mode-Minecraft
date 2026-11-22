package fr.modeminecraft.scenario;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Personnel des bases : officier recruteur (Tableau des opérations, comptes rendus de mission), cuisinier de la cantine
 * et infirmier (tarifs réduits selon le grade, gratuits pour un Général), scientifique transfuge (libération),
 * soldats de la base (petites tâches).
 */
public final class ScenarioNpc {
    private ScenarioNpc() {}

    /** true : l'interaction est prise en charge. */
    public static boolean interact(HumanBotEntity bot, ServerPlayer p) {
        BotRole role = bot.role();
        Side side = Scenario.side(p);
        if (role == BotRole.SCIENTIST) {
            return Missions.freeScientist(p, bot);
        }
        if (Prisoners.prisoner(bot)) {
            return side != null && Prisoners.interact(p, bot);
        }
        if (bot.getPersistentData().getBoolean(BlackMarketVan.TAG)) {
            BlackMarketVan.open(bot, p);
            return true;
        }
        if (side != null && role == BotRole.RECRUITER && bot.faction() == side.enemy) {
            return Defection.request(p, bot);
        }
        if (side == null || bot.faction() != side.ally) {
            if (role.staff()) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.staff.enemy").withStyle(ChatFormatting.RED), true);
                return true;
            }
            return false;
        }
        bot.getLookControl().setLookAt(p);
        switch (role) {
            case RECRUITER -> {
                Missions.report(p);
                MissionNet.openBoard(p);
                return true;
            }
            case COOK, MEDIC -> {
                Grade g = Reputation.grade(p);
                if (g.discount >= 1f) {
                    free(p, role);
                    return true;
                }
                bot.openShop(p, offers(role, g));
                return true;
            }
            case ARMORER -> {
                Armory.open(bot, p);
                return true;
            }
            default -> {
                if (!p.isSecondaryUseActive() && Tasks.ofMyBase(p, bot)) {
                    Tasks.interact(p, bot);
                    return true;
                }
                return false;
            }
        }
    }

    /** Général : rations et soins offerts (une dotation toutes les 5 minutes). */
    private static void free(ServerPlayer p, BotRole role) {
        var x = Scenario.record(p).extra;
        String key = "Free" + role.name();
        long now = p.level().getGameTime();
        if (Scenario.record(p).wait(key, 6000, now) > 0) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.staff.free_wait"), true);
            return;
        }
        x.putLong(key, now);
        if (role == BotRole.COOK) {
            Missions.give(p, new ItemStack(fr.modeminecraft.registry.ModItems.MILITARY_RATION.get(), 4), new ItemStack(Items.COOKED_BEEF, 8));
        } else {
            Missions.give(p, new ItemStack(fr.modeminecraft.registry.ModItems.BANDAGE.get(), 4), item("first_aid_kit", 2));
            p.heal(p.getMaxHealth());
        }
        p.displayClientMessage(Component.translatable("message.modeminecraft.staff.free").withStyle(ChatFormatting.GOLD), true);
    }

    private static ItemStack item(String id, int n) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("modeminecraft", id)), n);
    }

    /** Tarifs de la cantine / de l'infirmerie, réduits selon le grade. */
    static MerchantOffers offers(BotRole role, Grade g) {
        MerchantOffers out = new MerchantOffers();
        if (role == BotRole.COOK) {
            out.add(offer(Items.EMERALD, 2, new ItemStack(fr.modeminecraft.registry.ModItems.MILITARY_RATION.get(), 2), g));
            out.add(offer(Items.EMERALD, 2, new ItemStack(Items.COOKED_BEEF, 6), g));
            out.add(offer(Items.EMERALD, 1, new ItemStack(Items.BREAD, 6), g));
            out.add(offer(Items.EMERALD, 2, new ItemStack(Items.BAKED_POTATO, 8), g));
            out.add(offer(Items.GOLD_INGOT, 2, new ItemStack(Items.GOLDEN_CARROT, 4), g));
        } else {
            out.add(offer(Items.EMERALD, 3, new ItemStack(fr.modeminecraft.registry.ModItems.BANDAGE.get(), 3), g));
            out.add(offer(Items.EMERALD, 6, item("first_aid_kit", 1), g));
            out.add(offer(Items.EMERALD, 9, item("adrenaline_syringe", 1), g));
            out.add(offer(Items.GOLD_INGOT, 4, new ItemStack(Items.GOLDEN_APPLE, 1), g));
        }
        return out;
    }

    private static MerchantOffer offer(net.minecraft.world.item.Item cur, int price, ItemStack result, Grade g) {
        int cost = Math.max(1, Math.round(price * (1 - g.discount)));
        return new MerchantOffer(new ItemCost(cur, cost), result, 999, 0, 0f);
    }
}

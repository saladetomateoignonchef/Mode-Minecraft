package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.VehicleEntity;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Fourgon du marché noir : chaque nuit, un contrebandier gare son fourgon blindé à l'extérieur de la base du joueur
 * (position annoncée par la rumeur du camp). Il vend cher ce qu'on ne trouve pas ailleurs (carburant, uranium,
 * brouilleur, serre-flex, armes « tombées du camion ») et repart à l'aube.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class BlackMarketVan {
    private BlackMarketVan() {}

    public static final String TAG = "modeminecraft_van";
    /** Base (centre) -> {fourgon, contrebandier}. */
    private static final Map<Long, UUID[]> VANS = new HashMap<>();
    private static final Map<Long, Long> NIGHT = new HashMap<>();

    static boolean night(ServerLevel level) {
        long t = level.getDayTime() % 24000;
        return t >= 13000 && t < 23000;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % 200 != 0 || !level.getGameRules().getBoolean(Scenario.RULE)) {
            return;
        }
        long day = level.getDayTime() / 24000;
        if (!night(level)) {
            for (UUID[] ids : VANS.values()) {
                for (UUID id : ids) {
                    Entity e = level.getEntity(id);
                    if (e != null) {
                        e.discard();
                    }
                }
            }
            VANS.clear();
            // fourgons d'une session précédente (rechargés avec leur zone)
            for (Entity e : level.getAllEntities()) {
                if (e instanceof HumanBotEntity b && b.getPersistentData().getBoolean(TAG)) {
                    if (b.getVehicle() != null) {
                        b.getVehicle().discard();
                    }
                    b.discard();
                }
            }
            return;
        }
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            CityPlanner.City c = Scenario.side(p) == null ? null : Quarters.base(p);
            if (c == null) {
                continue;
            }
            long key = BlockPos.asLong(c.middleX(), 0, c.middleZ());
            if (VANS.containsKey(key) || NIGHT.getOrDefault(key, -1L) == day) {
                continue;
            }
            NIGHT.put(key, day);
            spawn(level, p, c, key);
        }
    }

    private static void spawn(ServerLevel level, ServerPlayer p, CityPlanner.City c, long key) {
        int d = c.rad() * 16 + 24;
        Vec3 center = new Vec3(c.middleX(), c.baseY(), c.middleZ());
        Vec3 at = Missions.landSpot(level, center, d, d + 24);
        if (at == null) {
            return;
        }
        float yaw = level.random.nextFloat() * 360;
        VehicleEntity van = new VehicleEntity(level, VehicleEntity.Type.ARMORED_TRUCK, at.x, at.y, at.z, yaw);
        level.addFreshEntity(van);
        HumanBotEntity t = Missions.spawnBot(level, at.add(2.5, 0, 0), Faction.TRADER, BotRole.TRADER);
        t.getPersistentData().putBoolean(TAG, true);
        t.setCustomName(Component.translatable("entity.modeminecraft.bot.smuggler").withStyle(ChatFormatting.DARK_PURPLE));
        t.home = BlockPos.containing(at);
        t.homeRadius = 3;
        VANS.put(key, new UUID[]{van.getUUID(), t.getUUID()});
        for (ServerPlayer o : level.getServer().getPlayerList().getPlayers()) {
            if (c.equals(Quarters.base(o))) {
                o.sendSystemMessage(Component.translatable("message.modeminecraft.van.rumor", (int) at.x, (int) at.z).withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }

    public static void open(HumanBotEntity bot, ServerPlayer p) {
        bot.openShop(p, offers());
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ModeMinecraft.id(id));
    }

    private static MerchantOffer offer(Item cur, int n, Item cur2, int n2, ItemStack out) {
        return new MerchantOffer(new ItemCost(cur, n), cur2 == null ? Optional.empty() : Optional.of(new ItemCost(cur2, n2)), out, 4, 0, 0f);
    }

    static MerchantOffers offers() {
        MerchantOffers o = new MerchantOffers();
        for (MerchantOffer m : List.of(
                offer(Items.EMERALD, 6, null, 0, new ItemStack(item("oil_barrel"))),
                offer(Items.EMERALD, 3, null, 0, new ItemStack(item("jerrycan"))),
                offer(Items.EMERALD, 20, Items.DIAMOND, 3, new ItemStack(item("uranium_canister"))),
                offer(Items.EMERALD, 10, null, 0, new ItemStack(item("portable_jammer"))),
                offer(Items.EMERALD, 3, null, 0, new ItemStack(item("tactical_battery"), 4)),
                offer(Items.EMERALD, 2, null, 0, new ItemStack(item("zip_ties"), 4)),
                offer(Items.DIAMOND, 6, Items.EMERALD, 6, new ItemStack(item("ak47"))),
                offer(Items.DIAMOND, 6, Items.EMERALD, 6, new ItemStack(item("m4"))),
                offer(Items.DIAMOND, 4, Items.EMERALD, 4, new ItemStack(item("dragunov"))),
                offer(Items.EMERALD, 24, null, 0, new ItemStack(item("red_access_card"))),
                new MerchantOffer(new ItemCost(Items.GOLD_INGOT, 3), new ItemStack(Items.EMERALD, 2), 16, 0, 0f))) {
            if (!m.getResult().isEmpty() && m.getResult().getItem() != Items.AIR) {
                o.add(m);
            }
        }
        return o;
    }
}

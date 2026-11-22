package fr.modeminecraft.scenario;

import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Petites tâches logistiques des PNJ militaires de sa base (intendants, mécaniciens, cuisiniers, gardes…) : livrer du
 * bois, de la pierre, du fer, des vivres, du charbon, un bidon de carburant, ou patrouiller autour de la base ; de +15
 * à +50 points de réputation. Clic droit sur un soldat de sa base : il présente sa demande ; second clic droit dans les
 * 10 secondes : acceptée ; clic droit avec les objets : livrée.
 */
public final class Tasks {
    private Tasks() {}

    public record Def(String kind, String item, int count, int rep) {}

    private static final Def[] POOL = {
            new Def("deliver", "minecraft:oak_log", 32, 30), new Def("deliver", "minecraft:oak_log", 64, 50),
            new Def("deliver", "minecraft:cobblestone", 64, 20), new Def("deliver", "minecraft:stone_bricks", 32, 25),
            new Def("deliver", "minecraft:iron_ingot", 16, 35), new Def("deliver", "minecraft:wheat", 32, 20),
            new Def("deliver", "minecraft:cooked_beef", 16, 25), new Def("deliver", "minecraft:baked_potato", 16, 20),
            new Def("deliver", "minecraft:apple", 16, 15), new Def("deliver", "minecraft:coal", 32, 20),
            new Def("deliver", "minecraft:redstone", 32, 30), new Def("deliver", "modeminecraft:jerrycan", 1, 40),
            new Def("patrol", "", 5, 40), new Def("patrol", "", 10, 50)};
    private static final Map<UUID, UUID> OFFERED = new HashMap<>();
    private static final Map<UUID, Long> OFFER_TIME = new HashMap<>();

    public static Def forBot(HumanBotEntity bot) {
        return POOL[Math.floorMod(bot.getUUID().hashCode(), POOL.length)];
    }

    static Item item(Def d) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(d.item()));
    }

    /** Ce soldat appartient-il à la base du joueur (même armée, dans l'enceinte) ? */
    public static boolean ofMyBase(ServerPlayer p, HumanBotEntity bot) {
        Side s = Scenario.side(p);
        if (s == null || bot.faction() != s.ally || bot.role().staff() || bot.convoy) {
            return false;
        }
        CityPlanner.City c = Quarters.base(p);
        return c != null && bot.getX() >= c.minX() - 16 && bot.getX() <= c.maxX() + 16 && bot.getZ() >= c.minZ() - 16 && bot.getZ() <= c.maxZ() + 16;
    }

    public static void interact(ServerPlayer p, HumanBotEntity bot) {
        Def d = forBot(bot);
        CompoundTag mine = null;
        for (var t : Missions.active(p)) {
            CompoundTag e = (CompoundTag) t;
            if (e.getString("Id").equals("task") && e.hasUUID("Giver") && e.getUUID("Giver").equals(bot.getUUID())) {
                mine = e;
            }
        }
        if (mine != null) {
            if (d.kind().equals("deliver")) {
                if (Missions.count(p, item(d)) >= d.count()) {
                    Missions.take(p, item(d), d.count());
                    finish(p, mine, bot);
                } else {
                    p.displayClientMessage(Component.translatable("message.modeminecraft.task.missing", Missions.count(p, item(d)), d.count(),
                            item(d).getDescription()).withStyle(ChatFormatting.YELLOW), true);
                }
            } else if (mine.getInt("Progress") >= d.count()) {
                finish(p, mine, bot);
            } else {
                p.displayClientMessage(Component.translatable("message.modeminecraft.task.patrol_progress", mine.getInt("Progress"), d.count()), true);
            }
            return;
        }
        long now = p.level().getGameTime();
        if (bot.getUUID().equals(OFFERED.get(p.getUUID())) && now - OFFER_TIME.getOrDefault(p.getUUID(), 0L) < 200) {
            if (Missions.active(p).size() >= Missions.MAX_ACTIVE) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.mission.unavailable.full").withStyle(ChatFormatting.RED), true);
                return;
            }
            long cd = Scenario.record(p).extra.getCompound("TaskCooldown").getLong(bot.getUUID().toString());
            if (cd > now) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.task.cooldown"), true);
                return;
            }
            CompoundTag e = new CompoundTag();
            e.putString("Id", "task");
            e.putUUID("Giver", bot.getUUID());
            e.putString("Kind", d.kind());
            e.putString("Item", d.item());
            e.putInt("Goal", d.count());
            e.putInt("Rep", d.rep());
            e.putString("GiverName", bot.getName().getString());
            e.putBoolean("Pinned", true);
            e.putInt("TX", bot.getBlockX());
            e.putInt("TY", bot.getBlockY());
            e.putInt("TZ", bot.getBlockZ());
            e.putBoolean("HasTarget", true);
            Missions.active(p).add(e);
            OFFERED.remove(p.getUUID());
            p.displayClientMessage(Component.translatable("message.modeminecraft.task.accepted").withStyle(ChatFormatting.GREEN), true);
            ScenarioData.get(p.serverLevel()).setDirty();
            MissionNet.sync(p);
            return;
        }
        OFFERED.put(p.getUUID(), bot.getUUID());
        OFFER_TIME.put(p.getUUID(), now);
        Component ask = d.kind().equals("deliver")
                ? Component.translatable("message.modeminecraft.task.offer_deliver", bot.getName(), d.count(), item(d).getDescription(), d.rep())
                : Component.translatable("message.modeminecraft.task.offer_patrol", bot.getName(), d.count(), d.rep());
        p.sendSystemMessage(ask.copy().withStyle(ChatFormatting.AQUA));
        bot.getLookControl().setLookAt(p);
    }

    private static void finish(ServerPlayer p, CompoundTag e, HumanBotEntity bot) {
        Missions.active(p).remove(e);
        CompoundTag cd = Scenario.record(p).extra.getCompound("TaskCooldown");
        cd.putLong(bot.getUUID().toString(), p.level().getGameTime() + 12000);   // nouvelle demande dans une demi-journée
        Scenario.record(p).extra.put("TaskCooldown", cd);
        p.sendSystemMessage(Component.translatable("message.modeminecraft.task.done", bot.getName()).withStyle(ChatFormatting.GREEN));
        Reputation.add(p, e.getInt("Rep"), "rep.modeminecraft.task");
        MissionNet.sync(p);
    }

    /** Progression des livraisons (objets dans l'inventaire). */
    static void tick(ServerPlayer p, CompoundTag e) {
        if (e.getString("Kind").equals("deliver")) {
            Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.getString("Item")));
            e.putInt("Progress", Math.min(e.getInt("Goal"), Missions.count(p, it)));
        }
    }

    static void onKill(ServerPlayer p, LivingEntity dead) {
        CityPlanner.City c = Quarters.base(p);
        if (c == null || Math.hypot(dead.getX() - c.middleX(), dead.getZ() - c.middleZ()) > c.rad() * 16 + 150) {
            return;
        }
        for (var t : Missions.active(p)) {
            CompoundTag e = (CompoundTag) t;
            if (e.getString("Id").equals("task") && e.getString("Kind").equals("patrol") && e.getInt("Progress") < e.getInt("Goal")) {
                e.putInt("Progress", e.getInt("Progress") + 1);
                if (e.getInt("Progress") >= e.getInt("Goal")) {
                    p.displayClientMessage(Component.translatable("message.modeminecraft.task.patrol_done").withStyle(ChatFormatting.GREEN), true);
                }
                MissionNet.sync(p);
                return;
            }
        }
    }
}

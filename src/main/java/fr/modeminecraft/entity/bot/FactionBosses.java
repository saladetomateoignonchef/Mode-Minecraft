package fr.modeminecraft.entity.bot;

import fr.modeminecraft.item.gun.Legendary;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * La Chasse aux 6 Chefs de Faction : chaque grande faction est dirigée par un Chef unique (100 PV) qui détient une arme
 * légendaire nommée. Le 6e est le Chef des Troupes de Choc des Faucons Blancs (Pourfendeuse de Dragons).
 * - chaque Chef n'existe qu'une fois par monde : vaincu, il n'apparaît plus dans les villes générées ensuite ;
 * - chaque joueur suit sa propre progression (« Chefs vaincus : 3/6 »).
 */
public final class FactionBosses {
    public static final List<String> ALL = List.of("boss_swat", "boss_militia", "boss_garrison", "boss_soviet", "boss_cartel", "boss_refinery", "boss_falcon", "boss_offshore");

    public static Legendary legendaryFor(Faction f) {
        return switch (f) {
            case SWAT, POLICE -> Legendary.PRESTIGE;
            case MILITIA -> Legendary.SOUVERAIN;
            case GARRISON -> Legendary.FANTOME;
            case SOVIET -> Legendary.HIVER;
            case CARTEL -> Legendary.CARTEL;
            default -> null;
        };
    }

    /** Identifiant du Chef (null si ce bot n'en est pas un). */
    public static String bossId(HumanBotEntity bot) {
        if (bot.role() == BotRole.CHIEF) {
            return "boss_falcon";
        }
        return bot.role() == BotRole.BOSS ? "boss_" + bot.faction().id : null;
    }

    /** Équipement du Chef : arme légendaire et tenue. */
    static void equip(HumanBotEntity bot, Faction f) {
        Legendary l = legendaryFor(f);
        if (l != null) {
            bot.setItemSlot(EquipmentSlot.MAINHAND, l.create());
        }
        switch (f) {
            case SWAT -> {
                bot.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.BALLISTIC_SHIELD.get()));
                bot.setItemSlot(EquipmentSlot.HEAD, new ItemStack(HumanBotEntity.item("altyn")));
                bot.setItemSlot(EquipmentSlot.CHEST, new ItemStack(HumanBotEntity.item("swat_vest")));
            }
            case GARRISON -> bot.setItemSlot(EquipmentSlot.CHEST, new ItemStack(HumanBotEntity.item("jpc_plate_carrier")));   // Général 4 étoiles
            case OFFSHORE -> {
                bot.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HumanBotEntity.item("scar_h")));
                bot.setItemSlot(EquipmentSlot.CHEST, new ItemStack(HumanBotEntity.item("avs_plate_carrier")));
                bot.setItemSlot(EquipmentSlot.HEAD, new ItemStack(HumanBotEntity.item("ops_core_fast")));
            }
            case SOVIET -> bot.setItemSlot(EquipmentSlot.CHEST, new ItemStack(HumanBotEntity.item("fort_defender")));
            case CARTEL -> {
                bot.setItemSlot(EquipmentSlot.CHEST, new ItemStack(HumanBotEntity.item("jpc_plate_carrier")));
                bot.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.GOLDEN_MACHETE.get()));
            }
            default -> {
            }
        }
        for (EquipmentSlot s : EquipmentSlot.values()) {
            bot.setDropChance(s, 0);                         // le butin est donné à part (garanti)
        }
        bot.getAttribute(Attributes.SCALE).setBaseValue(1.1);
        bot.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5);
        bot.setPersistenceRequired();
    }

    /** Butin garanti : l'arme unique (et le bouclier du Commissaire, la machette en or d'El Patrón). */
    static void dropLoot(HumanBotEntity bot) {
        if (bot.role() != BotRole.BOSS) {
            return;
        }
        Legendary l = legendaryFor(bot.faction());
        if (l != null) {
            bot.spawnAtLocation(l.create());
        }
        if (bot.faction() == Faction.SWAT) {
            bot.spawnAtLocation(new ItemStack(ModItems.BALLISTIC_SHIELD.get()));
        } else if (bot.faction() == Faction.CARTEL) {
            bot.spawnAtLocation(new ItemStack(ModItems.GOLDEN_MACHETE.get()));
        }
        bot.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.EMERALD, 24));
        // carte d'accès nominative : ouvre la chambre forte du bâtiment principal de sa ville
        var vault = switch (bot.faction()) {
            case SWAT, POLICE -> fr.modeminecraft.item.BossCardItem.VaultType.SWAT_HQ;
            case MILITIA -> fr.modeminecraft.item.BossCardItem.VaultType.DESERT_PALACE;
            case GARRISON -> fr.modeminecraft.item.BossCardItem.VaultType.FOREST_BUNKER;
            case SOVIET -> fr.modeminecraft.item.BossCardItem.VaultType.SOVIET_HQ;
            case CARTEL -> fr.modeminecraft.item.BossCardItem.VaultType.CARTEL_VILLA;
            case REFINERY, SHERIFF -> fr.modeminecraft.item.BossCardItem.VaultType.BADLANDS_REFINERY;
            case OFFSHORE -> fr.modeminecraft.item.BossCardItem.VaultType.OFFSHORE_VAULT;
            default -> null;
        };
        if (vault != null) {
            bot.spawnAtLocation(new ItemStack(ModItems.BOSS_CARDS.get(vault).get()));
        }
    }

    public static BossEvent.BossBarColor color(Faction f) {
        return switch (f) {
            case SWAT, POLICE -> BossEvent.BossBarColor.BLUE;
            case MILITIA -> BossEvent.BossBarColor.YELLOW;
            case GARRISON -> BossEvent.BossBarColor.GREEN;
            case SOVIET -> BossEvent.BossBarColor.RED;
            case CARTEL -> BossEvent.BossBarColor.PINK;
            case REFINERY -> BossEvent.BossBarColor.PURPLE;
            case OFFSHORE -> BossEvent.BossBarColor.YELLOW;
            default -> BossEvent.BossBarColor.WHITE;
        };
    }

    /** Un Chef tombe : il est rayé du monde, et le joueur qui l'a abattu progresse dans sa chasse. */
    static void onDeath(HumanBotEntity bot, ServerPlayer killer) {
        String id = bossId(bot);
        if (id == null || !(bot.level() instanceof ServerLevel level)) {
            return;
        }
        Data data = Data.get(level);
        data.defeated.add(id);
        data.setDirty();
        Component name = Component.translatable("entity.modeminecraft.bot." + id);
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.modeminecraft.boss.defeated_world", name).withStyle(ChatFormatting.GOLD), false);
        if (killer != null) {
            Set<String> mine = data.byPlayer.computeIfAbsent(killer.getUUID(), k -> new HashSet<>());
            mine.add(id);
            killer.displayClientMessage(Component.translatable("message.modeminecraft.boss.progress", mine.size(), ALL.size())
                    .withStyle(ChatFormatting.YELLOW), false);
            if (mine.containsAll(ALL)) {
                killer.displayClientMessage(Component.translatable("message.modeminecraft.boss.all").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
                level.playSound(null, killer.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
            }
        }
    }

    /** Ce Chef a-t-il déjà été vaincu dans ce monde ? (il n'existe qu'une fois) */
    static boolean alreadyDefeated(ServerLevel level, String id) {
        return Data.get(level).defeated.contains(id);
    }

    public static Set<String> defeatedBy(ServerLevel level, UUID player) {
        return Data.get(level).byPlayer.getOrDefault(player, Set.of());
    }

    /** Sauvegarde (monde principal) : Chefs vaincus, et progression de chaque joueur. */
    static final class Data extends SavedData {
        final Set<String> defeated = new HashSet<>();
        final Map<UUID, Set<String>> byPlayer = new HashMap<>();

        static Data get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage()
                    .computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), "modeminecraft_bosses");
        }

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            for (var t : tag.getList("defeated", 8)) {
                d.defeated.add(t.getAsString());
            }
            CompoundTag players = tag.getCompound("players");
            for (String key : players.getAllKeys()) {
                Set<String> set = new HashSet<>();
                for (var t : players.getList(key, 8)) {
                    set.add(t.getAsString());
                }
                d.byPlayer.put(UUID.fromString(key), set);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            defeated.forEach(s -> list.add(StringTag.valueOf(s)));
            tag.put("defeated", list);
            CompoundTag players = new CompoundTag();
            byPlayer.forEach((uuid, set) -> {
                ListTag l = new ListTag();
                set.forEach(s -> l.add(StringTag.valueOf(s)));
                players.put(uuid.toString(), l);
            });
            tag.put("players", players);
            return tag;
        }
    }

    private FactionBosses() {}
}

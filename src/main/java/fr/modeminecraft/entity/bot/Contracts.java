package fr.modeminecraft.entity.bot;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrats de chasse : un donneur de contrats (PNJ des villes) confie une cible nommée (chef d'une faction hostile,
 * entouré de gardes) cachée à 200-400 blocs. La boussole de traque pointe vers elle ; la cible n'apparaît que quand un
 * joueur approche. Une fois abattue, on revient voir un donneur de contrats pour toucher la prime.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class Contracts {
    private static final String[] FIRST = {"Viktor", "Ramón", "Hakim", "Dmitri", "Esteban", "Jack", "Omar", "Sergei", "Luis", "Boris", "Karim", "Nikolai", "Carlos", "Wade"};
    private static final String[] LAST = {"Morozov", "Escobar", "Al-Rashid", "Volkov", "Herrera", "McCoy", "Haddad", "Petrov", "Vargas", "Ivanov", "Kane", "Mendoza", "Sokolov", "Black"};
    private static final Faction[] TARGET_FACTIONS = {Faction.CARTEL, Faction.MILITIA, Faction.GANG, Faction.REFINERY, Faction.SOVIET};

    /** Contrat (sur l'objet "contrat de chasse" et dans les données du monde). */
    public record Contract(UUID id, String name, Faction faction, BlockPos pos, int reward, boolean done, Optional<UUID> target) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id);
            t.putString("Name", name);
            t.putString("Faction", faction.id);
            t.putLong("Pos", pos.asLong());
            t.putInt("Reward", reward);
            t.putBoolean("Done", done);
            target.ifPresent(u -> t.putUUID("Target", u));
            return t;
        }

        static Contract load(CompoundTag t) {
            return new Contract(t.getUUID("Id"), t.getString("Name"), Faction.byId(t.getString("Faction")), BlockPos.of(t.getLong("Pos")),
                    t.getInt("Reward"), t.getBoolean("Done"), t.hasUUID("Target") ? Optional.of(t.getUUID("Target")) : Optional.empty());
        }

        Contract withTarget(UUID u) {
            return new Contract(id, name, faction, pos, reward, done, Optional.of(u));
        }

        Contract finished() {
            return new Contract(id, name, faction, pos, reward, true, target);
        }
    }

    // ------------------------------------------------------------------ objet
    public static Contract of(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains("Contract")) {
            return null;
        }
        return Contract.load(data.copyTag().getCompound("Contract"));
    }

    static ItemStack stack(Contract c) {
        ItemStack s = new ItemStack(ModItems.HUNTING_CONTRACT.get());
        CompoundTag t = new CompoundTag();
        t.put("Contract", c.save());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    static ItemStack findContract(Player p) {
        for (ItemStack s : p.getInventory().items) {
            if (s.is(ModItems.HUNTING_CONTRACT.get()) && of(s) != null) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ donneur de contrats
    /** Clic droit sur un donneur de contrats : prime d'un contrat rempli, ou nouveau contrat + boussole de traque. */
    public static void interact(HumanBotEntity giver, Player player) {
        if (!(giver.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack held = findContract(player);
        Contract c = held.isEmpty() ? null : of(held);
        if (c != null && c.done()) {
            held.shrink(1);
            give(player, new ItemStack(Items.EMERALD, c.reward()));
            RandomSource r = level.getRandom();
            give(player, new ItemStack(r.nextFloat() < 0.3f ? ModItems.RED_ACCESS_CARD.get() : r.nextFloat() < 0.5f ? Items.DIAMOND : Items.GOLD_INGOT, 1 + r.nextInt(2)));
            data(level).contracts.remove(c.id());
            data(level).setDirty();
            player.displayClientMessage(Component.translatable("message.modeminecraft.contract.paid", c.reward()).withStyle(ChatFormatting.GREEN), false);
            level.playSound(null, giver.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.2f);
            return;
        }
        if (c != null) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.contract.ongoing", c.name()).withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        RandomSource r = level.getRandom();
        double a = r.nextDouble() * Math.PI * 2, d = 200 + r.nextInt(200);
        int x = (int) (giver.getX() + Math.cos(a) * d), z = (int) (giver.getZ() + Math.sin(a) * d);
        Faction f = TARGET_FACTIONS[r.nextInt(TARGET_FACTIONS.length)];
        String name = FIRST[r.nextInt(FIRST.length)] + " " + LAST[r.nextInt(LAST.length)];
        Contract nc = new Contract(UUID.randomUUID(), name, f, new BlockPos(x, 0, z), 18 + (int) (d / 25), false, Optional.empty());
        data(level).contracts.put(nc.id(), nc);
        data(level).setDirty();
        give(player, stack(nc));
        boolean hasCompass = player.getInventory().items.stream().anyMatch(s -> s.is(ModItems.TRACKING_COMPASS.get()));
        if (!hasCompass) {
            give(player, new ItemStack(ModItems.TRACKING_COMPASS.get()));
        }
        player.displayClientMessage(Component.translatable("message.modeminecraft.contract.new", name,
                Component.translatable("entity.modeminecraft.bot." + f.id), nc.reward()).withStyle(ChatFormatting.GOLD), false);
        level.playSound(null, giver.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1f, 1f);
    }

    private static void give(Player p, ItemStack s) {
        if (!p.getInventory().add(s)) {
            p.drop(s, false);
        }
    }

    // ------------------------------------------------------------------ boussole de traque
    /** Met à jour la cible de la boussole (appelé chaque seconde depuis l'inventaire du joueur). */
    public static void updateCompass(ItemStack compass, ServerPlayer player) {
        ItemStack held = findContract(player);
        Contract c = held.isEmpty() ? null : of(held);
        if (c == null || c.done()) {
            compass.remove(DataComponents.LODESTONE_TRACKER);
            return;
        }
        BlockPos pos = c.pos();
        Contract live = data(player.serverLevel()).contracts.get(c.id());
        if (live != null && live.target().isPresent() && player.serverLevel().getEntity(live.target().get()) instanceof HumanBotEntity t) {
            pos = t.blockPosition();
        }
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(player.level().dimension(), pos)), false));
    }

    // ------------------------------------------------------------------ apparition et mort de la cible
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % 40 != 0) {
            return;
        }
        Data data = data(level);
        for (Contract c : List.copyOf(data.contracts.values())) {
            if (c.done() || c.target().isPresent()) {
                continue;
            }
            Player near = level.getNearestPlayer(c.pos().getX(), 100, c.pos().getZ(), 96, false);
            if (near == null || !level.isLoaded(c.pos())) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.pos().getX(), c.pos().getZ());
            BlockPos base = new BlockPos(c.pos().getX(), y, c.pos().getZ());
            HumanBotEntity boss = spawn(level, base, c.faction(), BotRole.OFFICER);
            if (boss == null) {
                continue;
            }
            boss.setCustomName(Component.literal("★ " + c.name()).withStyle(ChatFormatting.RED));
            boss.setCustomNameVisible(true);
            boss.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(50);
            boss.setHealth(50);
            boss.getPersistentData().putUUID("modeminecraft_contract", c.id());
            for (int i = 0; i < 3; i++) {
                spawn(level, base.offset(level.getRandom().nextInt(7) - 3, 0, level.getRandom().nextInt(7) - 3), c.faction(), BotRole.SOLDIER);
            }
            data.contracts.put(c.id(), c.withTarget(boss.getUUID()));
            data.setDirty();
        }
    }

    private static HumanBotEntity spawn(ServerLevel level, BlockPos p, Faction f, BotRole role) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ());
        HumanBotEntity bot = ModEntities.HUMAN_BOT.get().create(level);
        if (bot == null) {
            return null;
        }
        bot.moveTo(p.getX() + 0.5, y, p.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        bot.setup(f, role, level.getRandom());
        bot.home = new BlockPos(p.getX(), y, p.getZ());
        bot.homeRadius = 10;
        bot.setPersistenceRequired();
        bot.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null);
        level.addFreshEntity(bot);
        return bot;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof HumanBotEntity bot) || !bot.getPersistentData().hasUUID("modeminecraft_contract")
                || !(bot.level() instanceof ServerLevel level)) {
            return;
        }
        UUID id = bot.getPersistentData().getUUID("modeminecraft_contract");
        Data data = data(level.getServer().overworld());
        Contract c = data.contracts.get(id);
        if (c == null) {
            return;
        }
        data.contracts.put(id, c.finished());
        data.setDirty();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            for (ItemStack s : p.getInventory().items) {
                Contract pc = s.is(ModItems.HUNTING_CONTRACT.get()) ? of(s) : null;
                if (pc != null && pc.id().equals(id)) {
                    CompoundTag t = new CompoundTag();
                    t.put("Contract", pc.finished().save());
                    s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
                    p.displayClientMessage(Component.translatable("message.modeminecraft.contract.done", c.name()).withStyle(ChatFormatting.GREEN), false);
                    level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1f);
                }
            }
        }
    }

    // ------------------------------------------------------------------ sauvegarde
    static Data data(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), "modeminecraft_contracts");
    }

    static final class Data extends SavedData {
        final Map<UUID, Contract> contracts = new HashMap<>();

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            for (Tag t : tag.getList("Contracts", Tag.TAG_COMPOUND)) {
                Contract c = Contract.load((CompoundTag) t);
                d.contracts.put(c.id(), c);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            contracts.values().forEach(c -> list.add(c.save()));
            tag.put("Contracts", list);
            return tag;
        }
    }

    private Contracts() {}
}

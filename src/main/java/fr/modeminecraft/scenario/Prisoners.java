package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.block.police.JailDoorBlockEntity;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.world.city.CityPlanner;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prisonniers et interrogatoires.
 * - Capture : serre-flex sur un combattant ennemi blessé (moins de la moitié de sa vie) ou surpris par derrière ; il
 *   lâche son arme, lève les mains et suit son ravisseur (y compris en véhicule).
 * - Incarcération : amené dans l'enceinte de la prison de sa base, il est enfermé dans une cellule libre (porte
 *   coulissante verrouillée) : +40 réputation.
 * - Interrogatoire : clic droit sur un détenu, il est conduit en salle d'interrogatoire ; on choisit l'approche
 *   (persuader, intimider, marchander 2 émeraudes). Réussite : programme nucléaire ennemi freiné, réputation, et souvent
 *   un renseignement (position d'une base ennemie ou d'une chambre forte) épinglé au journal. 3 séances par détenu.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class Prisoners {
    private Prisoners() {}

    /** Cellules de la prison (repère local du site 32 x 32) : centre x, centre z, porte x, porte z. */
    public static final int[][] CELLS = {{6, 6, 6, 8}, {10, 6, 10, 8}, {14, 6, 14, 8}, {18, 6, 18, 8}, {22, 6, 22, 8},
            {10, 13, 10, 11}, {14, 13, 14, 11}, {18, 13, 18, 11}, {22, 13, 22, 11}};
    /** Chaise du détenu en salle d'interrogatoire. */
    public static final int[] CHAIR = {14, 0, 16};
    public static final int SESSIONS = 3;
    private static final String TAG = "modeminecraft_prisoner", CAPTOR = "modeminecraft_captor", CELL = "modeminecraft_cell";
    private static final Map<UUID, UUID> ESCORTED = new ConcurrentHashMap<>();

    public static boolean prisoner(HumanBotEntity b) {
        return b.getPersistentData().getBoolean(TAG);
    }

    /** Désarme et neutralise un bot (capture, ou détenu placé à la génération de la prison). */
    public static void makePrisoner(HumanBotEntity b) {
        b.getPersistentData().putBoolean(TAG, true);
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            b.setItemSlot(s, ItemStack.EMPTY);
        }
        b.setDropChance(EquipmentSlot.MAINHAND, 0);
        b.setTarget(null);
        b.startRadio(null, 0);
        b.convoy = true;                                                    // plus de patrouille ni d'engagement
    }

    // ------------------------------------------------------------------ capture
    public static void capture(ServerPlayer p, HumanBotEntity bot, ItemStack ties) {
        Side side = Scenario.side(p);
        if (side == null || !bot.isAlive() || prisoner(bot)) {
            return;
        }
        Faction f = bot.faction();
        if (!f.combatant() || f == side.ally || f == Faction.FALCON || bot.role() == BotRole.BOSS || bot.role().staff()) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.prisoner.cannot").withStyle(ChatFormatting.RED), true);
            return;
        }
        Vec3 look = bot.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 toPlayer = p.position().subtract(bot.position()).multiply(1, 0, 1).normalize();
        boolean behind = bot.getTarget() == null && look.dot(toPlayer) < -0.4;
        boolean weak = bot.getHealth() <= bot.getMaxHealth() * 0.5f;
        if (!behind && !weak) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.prisoner.resists").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!p.getAbilities().instabuild) {
            ties.shrink(1);
        }
        makePrisoner(bot);
        bot.home = null;
        bot.getPersistentData().putUUID(CAPTOR, p.getUUID());
        ESCORTED.put(bot.getUUID(), p.getUUID());
        bot.level().playSound(null, bot.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1f, 1.3f);
        p.displayClientMessage(Component.translatable("message.modeminecraft.prisoner.captured").withStyle(ChatFormatting.GOLD), false);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof HumanBotEntity b && b.getPersistentData().hasUUID(CAPTOR)) {
            ESCORTED.put(b.getUUID(), b.getPersistentData().getUUID(CAPTOR));
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % 10 != 0 || ESCORTED.isEmpty()) {
            return;
        }
        for (var it = ESCORTED.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Entity ent = level.getEntity(e.getKey());
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getValue());
            if (!(ent instanceof HumanBotEntity bot) || !bot.isAlive()) {
                if (ent == null && p != null) {
                    continue;                                                // hors zone chargée : on garde le lien
                }
                it.remove();
                continue;
            }
            if (p == null) {
                continue;
            }
            // suit son ravisseur, à pied ou dans son véhicule
            Entity veh = p.getVehicle();
            if (veh != null && bot.getVehicle() != veh && bot.distanceTo(p) < 8 && veh.getPassengers().size() < 6) {
                bot.startRiding(veh, true);
            } else if (veh == null && bot.getVehicle() != null) {
                bot.stopRiding();
            } else if (bot.getVehicle() == null) {
                if (bot.distanceTo(p) > 40) {
                    bot.teleportTo(p.getX() + 1, p.getY(), p.getZ() + 1);
                } else if (bot.distanceTo(p) > 3) {
                    bot.getNavigation().moveTo(p, 1.15);
                }
            }
            // arrivé dans l'enceinte de la prison de la base : incarcération
            CityPlanner.City c = Quarters.base(p);
            Side side = Scenario.side(p);
            if (c != null && side != null && bot.getVehicle() == null) {
                BlockPos a = ScenarioSites.at(c, side, ScenarioSites.Site.PRISON, new int[]{0, 0, 0});
                if (bot.getX() >= a.getX() && bot.getX() < a.getX() + 32 && bot.getZ() >= a.getZ() && bot.getZ() < a.getZ() + 32) {
                    if (incarcerate(p, bot, c, side)) {
                        it.remove();
                    }
                }
            }
        }
    }

    private static BlockPos cellPos(CityPlanner.City c, Side side, int[] cell, boolean door) {
        return ScenarioSites.at(c, side, ScenarioSites.Site.PRISON, door ? new int[]{cell[2], 0, cell[3]} : new int[]{cell[0], 0, cell[1]});
    }

    /** Cellule libre la plus proche, porte verrouillée sur le détenu. */
    static boolean incarcerate(ServerPlayer p, HumanBotEntity bot, CityPlanner.City c, Side side) {
        ServerLevel level = p.serverLevel();
        for (int i = 0; i < CELLS.length; i++) {
            BlockPos center = cellPos(c, side, CELLS[i], false);
            boolean busy = !level.getEntitiesOfClass(HumanBotEntity.class, new net.minecraft.world.phys.AABB(center).inflate(1.4, 1, 1.4),
                    o -> o != bot && prisoner(o)).isEmpty();
            if (busy) {
                continue;
            }
            bot.stopRiding();
            bot.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            bot.getNavigation().stop();
            bot.home = center;
            bot.homeRadius = 1;
            bot.getPersistentData().remove(CAPTOR);
            bot.getPersistentData().putInt(CELL, i);
            JailDoorBlockEntity.lock(level, cellPos(c, side, CELLS[i], true), 24000L * 3650, bot.getUUID());
            level.playSound(null, center, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1f, 0.7f);
            p.displayClientMessage(Component.translatable("message.modeminecraft.prisoner.jailed", i + 1).withStyle(ChatFormatting.GREEN), false);
            Reputation.add(p, 40, "rep.modeminecraft.prisoner");
            return true;
        }
        p.displayClientMessage(Component.translatable("message.modeminecraft.prisoner.full").withStyle(ChatFormatting.RED), true);
        return false;
    }

    // ------------------------------------------------------------------ interrogatoire
    /** Clic droit sur un détenu de sa base : conduite en salle d'interrogatoire, puis ouverture de l'interrogatoire. */
    public static boolean interact(ServerPlayer p, HumanBotEntity bot) {
        if (!prisoner(bot)) {
            return false;
        }
        if (bot.getPersistentData().hasUUID(CAPTOR)) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.prisoner.escort"), true);
            return true;
        }
        Side side = Scenario.side(p);
        CityPlanner.City c = Quarters.base(p);
        if (side == null || c == null || bot.faction() == side.ally) {
            return true;
        }
        int done = bot.getPersistentData().getInt("modeminecraft_sessions");
        if (done >= SESSIONS) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.prisoner.exhausted"), true);
            return true;
        }
        BlockPos chair = ScenarioSites.at(c, side, ScenarioSites.Site.PRISON, CHAIR);
        if (bot.blockPosition().distSqr(chair) > 2) {
            bot.teleportTo(chair.getX() + 0.5, chair.getY(), chair.getZ() + 0.5);
            bot.home = chair;
            bot.homeRadius = 0;
            bot.setYRot(180);
            p.displayClientMessage(Component.translatable("message.modeminecraft.prisoner.to_room").withStyle(ChatFormatting.YELLOW), false);
            return true;
        }
        PacketDistributor.sendToPlayer(p, new Open(bot.getId(), SESSIONS - done, bot.getName().getString()));
        return true;
    }

    static void interrogate(ServerPlayer p, HumanBotEntity bot, String approach) {
        Side side = Scenario.side(p);
        CityPlanner.City c = Quarters.base(p);
        if (side == null || c == null || !prisoner(bot) || bot.distanceTo(p) > 8) {
            return;
        }
        int done = bot.getPersistentData().getInt("modeminecraft_sessions");
        if (done >= SESSIONS) {
            return;
        }
        float chance = switch (approach) {
            case "intimidate" -> 0.6f;
            case "bribe" -> 0.85f;
            default -> 0.4f;
        };
        if (approach.equals("bribe")) {
            if (Missions.count(p, Items.EMERALD) < 2) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.prisoner.no_emerald").withStyle(ChatFormatting.RED), true);
                return;
            }
            Missions.take(p, Items.EMERALD, 2);
        }
        bot.getPersistentData().putInt("modeminecraft_sessions", done + 1);
        ServerLevel level = p.serverLevel();
        if (level.random.nextFloat() < chance) {
            EnemyNuke.setback(p, 5);
            Reputation.add(p, 30, "rep.modeminecraft.interrogation");
            if (level.random.nextFloat() < 0.6f) {
                intel(p, side, level);
            } else {
                p.sendSystemMessage(Component.translatable("message.modeminecraft.prisoner.talks").withStyle(ChatFormatting.GREEN));
            }
        } else {
            if (approach.equals("intimidate")) {
                Reputation.add(p, -5, "rep.modeminecraft.interrogation_abuse");
            }
            p.sendSystemMessage(Component.translatable("message.modeminecraft.prisoner.silent").withStyle(ChatFormatting.GRAY));
        }
        // retour en cellule
        Integer cell = bot.getPersistentData().contains(CELL) ? bot.getPersistentData().getInt(CELL) : null;
        if (cell != null && cell < CELLS.length) {
            BlockPos center = cellPos(c, side, CELLS[cell], false);
            bot.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            bot.home = center;
            bot.homeRadius = 1;
            JailDoorBlockEntity.lock(level, cellPos(c, side, CELLS[cell], true), 24000L * 3650, bot.getUUID());
        }
    }

    /** Renseignement : position de la base ennemie ou d'une chambre forte, épinglée au journal. */
    private static void intel(ServerPlayer p, Side side, ServerLevel level) {
        String[] variants = {side.opposite().baseVariant, "metropolis", "desert", "refinery"};
        String v = variants[level.random.nextInt(variants.length)];
        CityPlanner.City c = Scenario.findCity(level, x -> x.variant().equals(v), p.blockPosition(), 8);
        if (c == null) {
            p.sendSystemMessage(Component.translatable("message.modeminecraft.prisoner.talks").withStyle(ChatFormatting.GREEN));
            return;
        }
        CompoundTag e = new CompoundTag();
        e.putString("Id", "intel");
        e.putString("Kind", v);
        e.putBoolean("Pinned", Missions.active(p).size() < Missions.MAX_ACTIVE);
        Missions.putTarget(e, new BlockPos(c.middleX(), c.baseY(), c.middleZ()));
        if (Missions.active(p).size() < Missions.MAX_ACTIVE) {
            Missions.active(p).add(e);
        }
        p.sendSystemMessage(Component.translatable("message.modeminecraft.prisoner.intel", Component.translatable("gui.modeminecraft.map." +
                (v.equals(side.opposite().baseVariant) ? v : v)), c.middleX(), c.middleZ()).withStyle(ChatFormatting.AQUA));
        Missions.dirty(p);
    }

    // ------------------------------------------------------------------ réseau
    public record Open(int entity, int left, String name) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ModeMinecraft.id("interrogation_open"));
        public static final StreamCodec<ByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Open::entity,
                ByteBufCodecs.VAR_INT, Open::left, ByteBufCodecs.STRING_UTF8, Open::name, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open o, IPayloadContext ctx) {
            ctx.enqueueWork(() -> fr.modeminecraft.client.GadgetScreens.interrogation(o.entity(), o.left(), o.name()));
        }
    }

    public record Act(int entity, String approach) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(ModeMinecraft.id("interrogation_act"));
        public static final StreamCodec<ByteBuf, Act> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Act::entity,
                ByteBufCodecs.STRING_UTF8, Act::approach, Act::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Act a, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp && sp.serverLevel().getEntity(a.entity()) instanceof HumanBotEntity bot) {
                    interrogate(sp, bot, a.approach());
                }
            });
        }
    }

    static List<int[]> cells() {
        return List.of(CELLS);
    }
}

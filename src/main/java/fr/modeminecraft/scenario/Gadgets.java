package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.AirdropEntity;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.entity.military.AirAI;
import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.entity.military.Strikes;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import fr.modeminecraft.world.city.CityPlanner;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gadgets du scénario :
 * - mallette de frappe stratégique (Général) : frappe de missiles balistiques sur des coordonnées X / Z, une par jour ;
 * - talkie-walkie de commandement (Lieutenant et plus) : 3 soldats d'élite en renfort, ou un largage par C-130 ;
 *   recharge de 15 minutes ;
 * - brouilleur portatif : 45 s de brouillage dans un rayon de 25 blocs autour du porteur (appels radio des gardes
 *   coupés, drones perturbés), consomme une pile CR123A.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class Gadgets {
    private Gadgets() {}

    public static final int RADIO_COOLDOWN = 18000, BRIEFCASE_COOLDOWN = 24000, JAM_TICKS = 900, JAM_RADIUS = 25;
    /** Brouilleurs actifs : porteur -> fin du brouillage. */
    private static final Map<UUID, Long> JAMMERS = new ConcurrentHashMap<>();
    /** Renforts d'élite : bot -> joueur suivi. */
    private static final Map<UUID, UUID> ESCORTS = new ConcurrentHashMap<>();
    /** C-130 du largage : avion -> heure de retrait. */
    private static final Map<UUID, Long> FLYOVERS = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ brouilleur portatif
    public static void jam(ServerPlayer p) {
        long now = p.level().getGameTime();
        if (JAMMERS.getOrDefault(p.getUUID(), 0L) > now) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.jammer.active"), true);
            return;
        }
        if (!p.getAbilities().instabuild) {
            int slot = -1;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).is(ModItems.TACTICAL_BATTERY.get())) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.jammer.no_battery").withStyle(ChatFormatting.RED), true);
                return;
            }
            p.getInventory().getItem(slot).shrink(1);
        }
        JAMMERS.put(p.getUUID(), now + JAM_TICKS);
        p.serverLevel().playSound(null, p.blockPosition(), ModSounds.RADIO_CRACKLE.get(), SoundSource.PLAYERS, 1.2f, 0.6f);
        p.displayClientMessage(Component.translatable("message.modeminecraft.jammer.on", JAM_RADIUS, JAM_TICKS / 20).withStyle(ChatFormatting.AQUA), true);
        MissionNet.sync(p);
    }

    /** Un brouilleur portatif actif couvre-t-il ce point ? */
    public static boolean jammed(Level level, Vec3 at) {
        if (JAMMERS.isEmpty() || !(level instanceof ServerLevel sl)) {
            return false;
        }
        long now = sl.getGameTime();
        for (var e : JAMMERS.entrySet()) {
            if (e.getValue() <= now) {
                continue;
            }
            Player p = sl.getPlayerByUUID(e.getKey());
            if (p != null && p.position().distanceToSqr(at) < JAM_RADIUS * JAM_RADIUS) {
                return true;
            }
        }
        return false;
    }

    public static long jamEnd(UUID player) {
        return JAMMERS.getOrDefault(player, 0L);
    }

    // ------------------------------------------------------------------ talkie-walkie de commandement
    public static void radio(ServerPlayer p, String what) {
        Side side = Scenario.side(p);
        if (side == null) {
            return;
        }
        if (!Reputation.grade(p).atLeast(Grade.LIEUTENANT)) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.gadget.rank", Component.translatable(Grade.LIEUTENANT.key(side)))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        var x = Scenario.record(p).extra;
        long now = p.level().getGameTime(), left = Scenario.record(p).wait("RadioCall", RADIO_COOLDOWN, now);
        if (left > 0) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.gadget.cooldown", left / 1200 + 1), true);
            return;
        }
        ServerLevel level = p.serverLevel();
        if (what.equals("elite")) {
            for (int i = 0; i < 3; i++) {
                double a = level.random.nextDouble() * Math.PI * 2;
                Vec3 at = p.position().add(Math.cos(a) * 4, 0, Math.sin(a) * 4);
                HumanBotEntity b = Missions.spawnBot(level, at, side.ally, BotRole.SOLDIER);
                b.setCustomName(Component.translatable("entity.modeminecraft.bot.elite." + side.id).withStyle(ChatFormatting.GOLD));
                b.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(60);
                b.setHealth(60);
                b.home = null;
                b.getPersistentData().putUUID("modeminecraft_escort", p.getUUID());
                b.getPersistentData().putLong("modeminecraft_escort_end", now + 12000);
                ESCORTS.put(b.getUUID(), p.getUUID());
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, at.x, at.y + 1, at.z, 12, 0.4, 0.6, 0.4, 0.02);
            }
            Briefings.radio(p, "elite");
        } else {
            Vec3 dir = new Vec3(1, 0, 0).yRot(level.random.nextFloat() * 6.28f);
            Vec3 start = p.position().subtract(dir.scale(350));
            MilVehicleEntity c130 = AirAI.spawnPlane(level, MilType.C_130, start, side.ally, null, p.position());
            AirAI.assign(c130, p.position().add(dir.scale(400)), null);
            FLYOVERS.put(c130.getUUID(), now + 900);
            double a = level.random.nextDouble() * Math.PI * 2;
            int dx = (int) (p.getX() + Math.cos(a) * 12), dz = (int) (p.getZ() + Math.sin(a) * 12);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, dx, dz);
            level.addFreshEntity(new AirdropEntity(level, dx + 0.5, Math.min(level.getMaxBuildHeight() - 5, ground + 70), dz + 0.5));
            Briefings.radio(p, "drop");
        }
        x.putLong("RadioCall", now);
        ScenarioData.get(level).setDirty();
    }

    // ------------------------------------------------------------------ mallette de frappe stratégique
    public static void strike(ServerPlayer p, int tx, int tz) {
        Side side = Scenario.side(p);
        if (side == null) {
            return;
        }
        if (Reputation.grade(p) != Grade.GENERAL) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.gadget.rank", Component.translatable(Grade.GENERAL.key(side)))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        var x = Scenario.record(p).extra;
        long now = p.level().getGameTime(), left = Scenario.record(p).wait("Briefcase", BRIEFCASE_COOLDOWN, now);
        if (left > 0) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.gadget.cooldown", left / 1200 + 1), true);
            return;
        }
        ServerLevel level = p.serverLevel();
        CityPlanner.City base = Quarters.base(p);
        Vec3 from = base != null ? new Vec3(base.middleX(), base.baseY() + 2, base.middleZ()) : p.position();
        for (int i = 0; i < 3; i++) {
            double ox = (i - 1) * 9, oz = (i == 1 ? 8 : -4);
            Strikes.launch(level, Strikes.Warhead.HE, from.add(i * 3, 0, 0), new Vec3(tx + ox, 64, tz + oz), p);
        }
        x.putLong("Briefcase", now);
        ScenarioData.get(level).setDirty();
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1f, 0.5f);
        p.displayClientMessage(Component.translatable("message.modeminecraft.briefcase.launched", tx, tz).withStyle(ChatFormatting.GOLD), false);
        Briefings.radio(p, "strike");
    }

    // ------------------------------------------------------------------ tic
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        if (now % 10 != 0) {
            return;
        }
        JAMMERS.values().removeIf(end -> end <= now);
        // particules du brouilleur autour du porteur
        for (var e : JAMMERS.keySet()) {
            Player p = level.getPlayerByUUID(e);
            if (p != null && now % 20 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.6, p.getZ(), 3, 0.3, 0.2, 0.3, 0.02);
            }
        }
        // retrait des C-130 de largage
        for (var it = FLYOVERS.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (e.getValue() <= now) {
                Entity v = level.getEntity(e.getKey());
                if (v != null) {
                    v.getPassengers().forEach(Entity::discard);
                    v.discard();
                }
                it.remove();
            }
        }
        if (now % 20 != 0) {
            return;
        }
        // renforts d'élite : suivent le joueur, repartent au bout de 10 minutes
        List<UUID> gone = new ArrayList<>();
        for (var e : ESCORTS.entrySet()) {
            Entity b = level.getEntity(e.getKey());
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getValue());
            if (!(b instanceof HumanBotEntity bot) || !bot.isAlive() || p == null
                    || bot.getPersistentData().getLong("modeminecraft_escort_end") < now) {
                if (b != null && b.isAlive()) {
                    b.discard();
                }
                gone.add(e.getKey());
                continue;
            }
            double d = bot.distanceTo(p);
            if (d > 48 || bot.level() != p.level()) {
                bot.teleportTo(p.getX() + 2, p.getY(), p.getZ() + 2);
            } else if (d > 6 && (bot.getTarget() == null || !bot.getTarget().isAlive())) {
                bot.getNavigation().moveTo(p, 1.2);
            }
        }
        gone.forEach(ESCORTS::remove);
    }

    // ------------------------------------------------------------------ réseau (écrans du talkie et de la mallette)
    public record Use(String kind, String arg) implements CustomPacketPayload {
        public static final Type<Use> TYPE = new Type<>(ModeMinecraft.id("gadget_use"));
        public static final StreamCodec<ByteBuf, Use> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Use::kind,
                ByteBufCodecs.STRING_UTF8, Use::arg, Use::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Use u, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer sp)) {
                    return;
                }
                switch (u.kind()) {
                    case "radio" -> {
                        if (holding(sp, ModItems.COMMAND_RADIO.get())) {
                            radio(sp, u.arg());
                        }
                    }
                    case "strike" -> {
                        String[] xz = u.arg().split(",");
                        if (xz.length == 2 && holding(sp, ModItems.STRATEGIC_BRIEFCASE.get())) {
                            try {
                                strike(sp, Integer.parseInt(xz[0].trim()), Integer.parseInt(xz[1].trim()));
                            } catch (NumberFormatException ignored) {
                                sp.displayClientMessage(Component.translatable("message.modeminecraft.briefcase.bad"), true);
                            }
                        }
                    }
                    default -> {
                    }
                }
            });
        }
    }

    private static boolean holding(Player p, net.minecraft.world.item.Item i) {
        return p.getMainHandItem().is(i) || p.getOffhandItem().is(i);
    }

    /** Ouvre l'écran du gadget côté client. */
    public record Open(String kind) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ModeMinecraft.id("gadget_open"));
        public static final StreamCodec<ByteBuf, Open> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Open::new, Open::kind);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open o, IPayloadContext ctx) {
            ctx.enqueueWork(() -> fr.modeminecraft.client.GadgetScreens.open(o.kind()));
        }
    }

    static BlockPos unused() {
        return BlockPos.ZERO;
    }

    static ItemStack none() {
        return ItemStack.EMPTY;
    }
}

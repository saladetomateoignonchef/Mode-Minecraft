package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModBlocks;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Vie de la base, jour et nuit : le soir, les soldats qui ne sont pas de garde (trois sur quatre) rentrent au dortoir
 * et dorment dans les couchettes ; à l'aube, rassemblement au centre de la base, puis chacun reprend son poste.
 * Un soldat endormi se réveille dès qu'il est touché.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class BaseRoutine {
    private BaseRoutine() {}

    private static final String HOME = "modeminecraft_routine_home", RADIUS = "modeminecraft_routine_r", SLEEP = "modeminecraft_routine_sleep";

    enum Phase { DAY, NIGHT, ROLL_CALL }

    static Phase phase(ServerLevel level) {
        long t = level.getDayTime() % 24000;
        if (t >= 13500 && t < 23000) {
            return Phase.NIGHT;
        }
        return t >= 23000 || t < 1200 ? Phase.ROLL_CALL : Phase.DAY;
    }

    static boolean sentry(HumanBotEntity b) {
        return (b.getUUID().hashCode() & 3) == 0;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % 100 != 37 || !level.getGameRules().getBoolean(Scenario.RULE)) {
            return;
        }
        Phase ph = phase(level);
        Set<CityPlanner.City> done = new HashSet<>();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            Side side = Scenario.side(p);
            CityPlanner.City c = side == null ? null : Quarters.base(p);
            if (c == null || !done.add(c)) {
                continue;
            }
            AABB box = new AABB(c.minX(), c.baseY() - 30, c.minZ(), c.maxX() + 1, c.baseY() + 40, c.maxZ() + 1);
            BlockPos dorm = ScenarioSites.at(c, side, ScenarioSites.Site.DORMITORY, new int[]{16, 0, 8});
            BlockPos parade = new BlockPos(c.middleX(), c.baseY(), c.middleZ());
            for (HumanBotEntity b : level.getEntitiesOfClass(HumanBotEntity.class, box, b -> b.isAlive() && b.faction() == side.ally
                    && b.role() == BotRole.SOLDIER && !b.convoy && !Prisoners.prisoner(b) && b.getVehicle() == null
                    && !b.getPersistentData().hasUUID("modeminecraft_escort") && b.home != null)) {
                step(level, b, ph, dorm, parade);
            }
        }
    }

    private static void step(ServerLevel level, HumanBotEntity b, Phase ph, BlockPos dorm, BlockPos parade) {
        CompoundTag d = b.getPersistentData();
        if (d.getBoolean(SLEEP) && !b.isSleeping()) {
            wake(b);                                                        // réveillé par un coup
        }
        if (ph == Phase.DAY || sentry(b) || b.getTarget() != null) {
            if (d.contains(HOME)) {
                wake(b);
                b.home = BlockPos.of(d.getLong(HOME));
                b.homeRadius = d.getInt(RADIUS);
                d.remove(HOME);
            }
            return;
        }
        if (!d.contains(HOME)) {
            d.putLong(HOME, b.home.asLong());
            d.putInt(RADIUS, b.homeRadius);
        }
        if (ph == Phase.ROLL_CALL) {
            wake(b);
            b.home = parade;
            b.homeRadius = 6;
            return;
        }
        // nuit : au dortoir, puis dans une couchette libre
        b.home = dorm;
        b.homeRadius = 6;
        if (!b.isSleeping() && b.blockPosition().distSqr(dorm) < 12 * 12) {
            for (BlockPos q : BlockPos.betweenClosed(b.blockPosition().offset(-4, -1, -4), b.blockPosition().offset(4, 2, 4))) {
                BlockState s = level.getBlockState(q);
                if ((s.is(ModBlocks.MILITARY_BUNK_BED_US.get()) || s.is(ModBlocks.MILITARY_BUNK_BED_SOVIET.get()))
                        && level.getEntitiesOfClass(HumanBotEntity.class, new AABB(q).inflate(0.3), HumanBotEntity::isSleeping).isEmpty()) {
                    b.getNavigation().stop();
                    b.startSleeping(q.immutable());
                    b.setNoAi(true);
                    d.putBoolean(SLEEP, true);
                    break;
                }
            }
        }
    }

    static void wake(HumanBotEntity b) {
        if (b.getPersistentData().getBoolean(SLEEP)) {
            b.getPersistentData().remove(SLEEP);
            if (b.isSleeping()) {
                b.stopSleeping();
            }
            b.setNoAi(false);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof HumanBotEntity b && !b.level().isClientSide && b.getPersistentData().getBoolean(SLEEP)) {
            wake(b);
        }
    }
}

package fr.modeminecraft.entity.military;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * IA aérienne (idée du QCM « Patrouilles + interceptions ») :
 * - patrouilles de chasse : au-dessus des bases militaires (OTAN) et des villes soviétiques, une paire de chasseurs
 *   tourne en orbite à 70-90 blocs d'altitude quand un joueur est dans les parages ;
 * - interceptions : un joueur qui pénètre en aéronef dans l'espace aérien d'une base ou d'une ville soviétique fait
 *   décoller deux intercepteurs (alerte « QRA ») qui le prennent en chasse au canon et aux missiles — sauf s'il est
 *   invisible aux radars (F-117, B-2 soute fermée, F-22 / Su-57 hors de courte portée) ;
 * - combat aérien : poursuite avec anticipation, rafales de canon quand la cible est dans l'axe, missiles air-air une
 *   fois la cible verrouillée dans le cône de l'autodirecteur, leurres quand un missile arrive, évitement du sol.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class AirAI {
    private static final MilType[] SOVIET = {MilType.MIG_29, MilType.SU_57, MilType.MIG_21, MilType.SU_34, MilType.MIG_25, MilType.SU_25};
    private static final MilType[] NATO = {MilType.F_16C, MilType.F_15E, MilType.F_22, MilType.F_4E, MilType.A_10};
    private static final MilType[] INTERCEPTORS_SOV = {MilType.MIG_29, MilType.SU_57, MilType.MIG_25};
    private static final MilType[] INTERCEPTORS_NATO = {MilType.F_22, MilType.F_15E, MilType.F_16C};
    /** Probabilité d'une patrouille de chasse au-dessus d'une zone militaire (toutes les 30 s). */
    public static float PATROL_CHANCE = 0.25f;
    private static final Map<UUID, Long> SCRAMBLED = new HashMap<>();

    /** État de l'IA par avion : cible, centre de l'orbite, décisions de tir. */
    private static final class Brain {
        int target = -1;
        Vec3 home;
        boolean gun, missile;
        float orbit;
    }

    private static final Map<Integer, Brain> BRAINS = new HashMap<>();

    private static Brain brain(MilVehicleEntity v) {
        return BRAINS.computeIfAbsent(v.getId(), k -> {
            Brain b = new Brain();
            b.home = v.position();
            b.orbit = v.getRandom().nextFloat() * Mth.TWO_PI;
            return b;
        });
    }

    public static boolean wantsGun(MilVehicleEntity v) {
        Brain b = BRAINS.get(v.getId());
        return b != null && b.gun;
    }

    public static boolean wantsMissile(MilVehicleEntity v) {
        Brain b = BRAINS.get(v.getId());
        return b != null && b.missile;
    }

    /** Fixe le centre de patrouille et la cible d'un avion de l'IA. */
    public static void assign(MilVehicleEntity v, Vec3 home, Entity target) {
        Brain b = brain(v);
        b.home = home;
        b.target = target == null ? -1 : target.getId();
    }

    /** Radar ennemi le plus proche devant l'avion (missile anti-radar AGM-88 HARM). */
    public static Entity nearestRadar(MilVehicleEntity v, Vec3 dir, double range) {
        Entity best = null;
        double bestD = range * range;
        for (MilVehicleEntity o : v.level().getEntitiesOfClass(MilVehicleEntity.class, v.getBoundingBox().inflate(range, 300, range),
                o -> (o.type().has(MilType.Flag.STRATEGIC) || o.type().has(MilType.Flag.RADAR)) && !o.is(MilVehicleEntity.WRECK))) {
            Vec3 to = o.position().subtract(v.position());
            double d = to.lengthSqr();
            if (d < bestD && to.normalize().dot(dir) > Math.cos(Math.toRadians(45))) {
                bestD = d;
                best = o;
            }
        }
        return best;
    }

    /** Une cible aérienne est-elle visible pour ce pilote (radar, ou à l'œil nu de jour sous 35 blocs) ? */
    private static boolean visible(MilVehicleEntity v, Entity e) {
        if (MilVehicleEntity.radarSees(v, e)) {
            return true;
        }
        boolean day = v.level().isDay();
        return day && v.distanceToSqr(e) < 35 * 35;
    }

    /** Cible légitime : aéronef piloté (joueur, ou bot d'une faction ennemie). */
    private static boolean hostile(MilVehicleEntity v, HumanBotEntity pilot, Entity e) {
        if (!(e instanceof MilVehicleEntity o) || o == v || !o.type().heli() || o.is(MilVehicleEntity.WRECK) || o.onGround()) {
            return false;
        }
        Entity c = o.getControllingPassenger();
        if (c instanceof Player p) {
            return !p.isCreative() && !p.isSpectator();
        }
        return c instanceof HumanBotEntity hb && pilot.faction().enemyOf(hb.faction());
    }

    /** Pilotage de l'IA (chaque tick, côté serveur). */
    static void fly(MilVehicleEntity v, HumanBotEntity pilot) {
        if (!(v.level() instanceof ServerLevel sl)) {
            return;
        }
        Brain b = brain(v);
        b.gun = false;
        b.missile = false;
        Entity t = b.target >= 0 ? sl.getEntity(b.target) : null;
        if (t != null && (!t.isAlive() || !hostile(v, pilot, t) || v.distanceToSqr(t) > 700 * 700 || v.tickCount % 20 == 0 && !visible(v, t))) {
            t = null;
            b.target = -1;
        }
        if (t == null && (v.tickCount + v.getId()) % 20 == 0) {
            double best = 420 * 420;
            for (MilVehicleEntity o : sl.getEntitiesOfClass(MilVehicleEntity.class, v.getBoundingBox().inflate(420, 250, 420), o -> hostile(v, pilot, o))) {
                double d = o.distanceToSqr(v);
                if (d < best && visible(v, o)) {
                    best = d;
                    t = o;
                }
            }
            b.target = t == null ? -1 : t.getId();
        }
        int ground = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, v.getBlockX(), v.getBlockZ());
        Vec3 motion = v.position().subtract(v.xo, v.yo, v.zo);
        Vec3 aheadPos = v.position().add(motion.scale(25));
        int groundAhead = sl.isLoaded(BlockPos.containing(aheadPos))
                ? sl.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(aheadPos.x), (int) Math.floor(aheadPos.z)) : ground;
        double alt = v.getY() - Math.max(ground, groundAhead);
        Vec3 goal;
        v.aiThrottle = 1;
        if (t != null) {
            // poursuite : point d'anticipation (vitesse de la cible x temps de vol des obus)
            Vec3 tp = t.position().add(0, t.getBbHeight() / 2, 0);
            Vec3 tv = t.position().subtract(t.xo, t.yo, t.zo);
            double d = v.position().distanceTo(tp);
            double flight = d / (6 + Math.max(0.5, motion.length()));
            goal = tp.add(tv.scale(flight));
            Vec3 nose = Vec3.directionFromRotation(-pitchOf(motion), v.getYRot());
            double cos = goal.subtract(v.position()).normalize().dot(nose);
            int gun = v.type().planeGun();
            b.gun = gun >= 0 && d < 140 && cos > Math.cos(Math.toRadians(4));
            // missile air-air : cible dans le cône de l'autodirecteur
            int aam = -1;
            for (int i = 0; i < v.type().weapons.size(); i++) {
                MilType.Weapon w = v.type().weapons.get(i);
                if (w.kind() == MilType.Kind.AAM && v.ammo(v.selectedAmmo(i)) > 0) {
                    aam = i;
                    break;
                }
            }
            if (aam >= 0 && d > 60 && d < 420 && cos > Math.cos(Math.toRadians(12)) && MilVehicleEntity.radarSees(v, t)) {
                v.aiLock(t, aam);
                b.missile = v.aiLockReady();
            } else {
                v.aiLock(null, -1);
            }
            // trop près : on dégage pour reprendre de la distance
            if (d < 25) {
                goal = v.position().add(nose.scale(60)).add(0, 20, 0);
            }
        } else {
            v.aiLock(null, -1);
            // patrouille : orbite de 130 blocs autour de la zone, à 75 blocs au-dessus du sol
            b.orbit += 0.012f;
            double r = 130;
            goal = new Vec3(b.home.x + Math.cos(b.orbit) * r, Math.max(b.home.y, ground) + 75, b.home.z + Math.sin(b.orbit) * r);
            v.aiThrottle = 0.6f;
        }
        Vec3 to = goal.subtract(v.position());
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90;
        float pitch = (float) -(Mth.atan2(to.y, Math.hypot(to.x, to.z)) * Mth.RAD_TO_DEG);       // convention du joueur
        if (alt < 35) {
            pitch = Math.min(pitch, -25);                                                      // évitement du sol
        }
        if (v.getY() > sl.getMaxBuildHeight() - 45) {
            pitch = Math.max(pitch, 5);
        }
        v.aiYaw = yaw;
        v.aiPitch = Mth.clamp(pitch, -45, 45);
        // décollage d'un avion sorti du hangar : plein gaz sur la piste, train rentré une fois en l'air
        float sp = Math.abs(v.speed());
        if (v.groundedNow()) {
            // roulage : plein gaz, on ne cabre qu'à 50 % de la vitesse max (sinon décrochage juste après la rotation)
            v.aiThrottle = 1;
            v.aiPitch = sp >= v.type().maxSpeed() * 0.5f ? -12 : 0;
        } else if (alt < 35 && sp < v.type().maxSpeed() * 0.75f) {
            v.aiThrottle = 1;
            v.aiPitch = -8;                                                       // montée douce tant que la vitesse est faible
        } else if (alt > 15 && !v.is(MilVehicleEntity.GEAR_UP)) {
            v.setGearUp();
        }
        // leurres quand un missile arrive
        if (v.is(MilVehicleEntity.MISSILE_WARN) && v.getRandom().nextInt(6) == 0) {
            v.flares();
        }
    }

    private static float pitchOf(Vec3 m) {
        return m.lengthSqr() < 1e-4 ? 0 : (float) Math.toDegrees(Math.atan2(m.y, Math.hypot(m.x, m.z)));
    }

    public static void forget(MilVehicleEntity v) {
        BRAINS.remove(v.getId());
    }

    // ------------------------------------------------------------------ apparition des patrouilles et intercepteurs
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        if (now % 100 != 0 || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        BRAINS.keySet().removeIf(id -> !(level.getEntity(id) instanceof MilVehicleEntity mv) || mv.isRemoved());
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) {
                continue;
            }
            var city = CityPlanner.cityAtCached(CityPlanner.Env.of(level), p.getBlockX(), p.getBlockZ(), 160);
            if (city == null || !city.kind().equals("base") && !city.kind().equals("soviet")) {
                continue;
            }
            Faction f = city.kind().equals("base") ? Faction.GARRISON : Faction.SOVIET;
            Vec3 center = new Vec3(city.middleX(), city.baseY(), city.middleZ());
            long airborneAi = level.getEntitiesOfClass(MilVehicleEntity.class, p.getBoundingBox().inflate(600, 300, 600),
                    v -> v.type().plane() && v.patrol).size();
            // interception : le joueur survole l'espace aérien en aéronef, visible au radar
            if (p.getVehicle() instanceof MilVehicleEntity mine && mine.type().heli() && !mine.onGround()
                    && MilVehicleEntity.radarSees(p, mine) && airborneAi < 4
                    && now - SCRAMBLED.getOrDefault(p.getUUID(), -100000L) > 2400) {
                SCRAMBLED.put(p.getUUID(), now);
                MilType[] pool = f == Faction.SOVIET ? INTERCEPTORS_SOV : INTERCEPTORS_NATO;
                int fromHangar = launchFromHangars(level, center, city.rad() * 16 + 64, f, 2, mine);
                for (int i = fromHangar; i < 2; i++) {
                    MilVehicleEntity v = spawnPlane(level, pool[level.random.nextInt(pool.length)], center.add(i * 25, 0, 0), f, center, mine.position());
                    if (v != null) {
                        assign(v, center, mine);
                    }
                }
                p.displayClientMessage(Component.translatable(f == Faction.SOVIET ? "message.modeminecraft.air.scramble_sov" : "message.modeminecraft.air.scramble_nato")
                        .withStyle(ChatFormatting.RED), false);
                continue;
            }
            if (now % 600 == 0 && airborneAi == 0 && level.random.nextFloat() < PATROL_CHANCE) {
                if (launchFromHangars(level, center, city.rad() * 16 + 64, f, 2, null) > 0) {
                    continue;                                                     // patrouille partie des hangars
                }
                MilType[] pool = f == Faction.SOVIET ? SOVIET : NATO;
                double a = level.random.nextDouble() * Math.PI * 2;
                Vec3 start = center.add(Math.cos(a) * 260, 0, Math.sin(a) * 260);
                MilType type = pool[level.random.nextInt(pool.length)];
                for (int i = 0; i < 2; i++) {
                    MilVehicleEntity v = spawnPlane(level, type, start.add(i * 20, 0, i * 20), f, null, center);
                    if (v != null) {
                        assign(v, center, null);
                    }
                }
            }
        }
    }

    /**
     * Décollage d'alerte depuis les méga-hangars : les chasseurs garés du bon camp (sans équipage) reçoivent un pilote,
     * le plein et les munitions, puis sortent du hangar et décollent sur la piste. Renvoie le nombre d'avions lancés.
     */
    public static int launchFromHangars(ServerLevel level, Vec3 center, double radius, Faction f, int max, Entity target) {
        MilType.Side side = f == Faction.SOVIET ? MilType.Side.SOVIET : MilType.Side.US;
        int n = 0;
        for (MilVehicleEntity v : level.getEntitiesOfClass(MilVehicleEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(radius, 80, radius),
                v -> v.type().plane() && !v.type().heavy() && v.type().side == side && v.getPassengers().isEmpty() && !v.patrol
                        && !v.is(MilVehicleEntity.WRECK) && v.groundedNow())) {
            if (n >= max) {
                break;
            }
            v.fillAmmo(1f);
            v.setFuel(1f);
            v.patrol = true;
            v.setAirspeed(0);                                                     // moteurs lancés : décollage d'alerte
            v.holdChunks();
            HumanBotEntity b = ModEntities.HUMAN_BOT.get().create(level);
            b.moveTo(v.getX(), v.getY() + 1, v.getZ(), v.getYRot(), 0);
            b.setup(f, BotRole.OFFICER, level.getRandom());
            b.convoy = true;
            b.finalizeSpawn(level, level.getCurrentDifficultyAt(v.blockPosition()), MobSpawnType.EVENT, null);
            level.addFreshEntity(b);
            b.startRiding(v, true);
            v.aiYaw = v.getYRot();
            assign(v, center, target);
            ModeMinecraft.LOGGER.info("Décollage d'alerte ({}, {}) depuis le hangar en {}", f.id, v.type().id, v.blockPosition());
            n++;
        }
        return n;
    }

    /** Un avion de l'IA en vol, plein de munitions, avec son pilote bot. `from` : piste (décollage d'alerte) ou null. */
    public static MilVehicleEntity spawnPlane(ServerLevel level, MilType t, Vec3 at, Faction f, Vec3 from, Vec3 toward) {
        BlockPos pos = BlockPos.containing(at);
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);                         // zone d'apparition chargée
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        double y = from != null ? ground + 30 : ground + 80;
        float yaw = (float) (Mth.atan2(toward.z - at.z, toward.x - at.x) * Mth.RAD_TO_DEG) - 90;
        MilVehicleEntity v = MilVehicleEntity.create(level, t, at.x, y, at.z, yaw);
        v.fillAmmo(1f);
        v.setFuel(1f);
        v.patrol = true;
        v.setGearUp();
        v.setAirspeed(t.maxSpeed() * 0.85f);
        v.aiYaw = yaw;
        level.addFreshEntity(v);
        v.holdChunks();
        HumanBotEntity b = ModEntities.HUMAN_BOT.get().create(level);
        b.moveTo(at.x, y, at.z, yaw, 0);
        b.setup(f, BotRole.OFFICER, level.getRandom());
        b.convoy = true;
        b.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        level.addFreshEntity(b);
        b.startRiding(v, true);
        ModeMinecraft.LOGGER.info("Avion de l'IA ({}, {}) en vol à {}", f.id, t.id, BlockPos.containing(at.x, y, at.z));
        return v;
    }

    private AirAI() {}
}

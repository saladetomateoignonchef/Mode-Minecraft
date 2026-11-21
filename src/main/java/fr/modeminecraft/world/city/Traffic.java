package fr.modeminecraft.world.city;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.VehicleEntity;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.FactionRelations;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Circulation des métropoles : berlines et taxis jaunes conduits par des civils, voitures de patrouille, bus urbain.
 * Les véhicules suivent les voies (conduite à droite), choisissent leur direction aux carrefours, s'arrêtent aux feux
 * rouges et devant un obstacle (klaxon si un joueur bloque la route), se rangent devant une sirène. Les voitures de
 * police prennent en chasse un joueur recherché puis déploient leurs agents. Le bus fait le tour de la ville sur le
 * boulevard circulaire et marque 10 s d'arrêt à chaque abribus (on peut y monter gratuitement).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class Traffic {
    public static final int MAX_CARS = 8, BUS_RING = 6;
    /** Arrêts de bus du boulevard circulaire (chunks relatifs au centre), dans le sens horaire. */
    public static final int[][] BUS_STOPS = {{4, -BUS_RING}, {BUS_RING, 4}, {-4, BUS_RING}, {-BUS_RING, -4}};

    /** Itinéraire d'un véhicule de la circulation. */
    public static final class Route {
        final CityPlanner.City city;
        final CityLayout layout;
        int dx, dz;
        Direction heading;
        final boolean bus;
        int stopTicks, blockedTicks, yieldTicks, hornCooldown;
        Player chase;
        Vec3 target;

        Route(CityPlanner.City city, int dx, int dz, Direction heading, boolean bus) {
            this.city = city;
            this.layout = city.layout();
            this.dx = dx;
            this.dz = dz;
            this.heading = heading;
            this.bus = bus;
        }

        boolean road(int x, int z) {
            if (Math.abs(x) > city.rad() || Math.abs(z) > city.rad()) {
                return false;
            }
            CityLayout.Plot p = layout.plot(x, z);
            return p != null && p.type().equals("road");
        }

        boolean crossing(int x, int z) {
            return Math.floorMod(x + city.rad(), 3) == 0 && Math.floorMod(z + city.rad(), 3) == 0;
        }

        /** Fin de poursuite (suspect mort ou disparu). */
        public void stopChase(Player p) {
            if (chase == p) {
                chase = null;
            }
        }

        /** Point de la voie de droite, au bord de sortie du chunk courant. */
        Vec3 exitPoint() {
            double x0 = (city.center().x + dx) * 16, z0 = (city.center().z + dz) * 16;
            double y = city.baseY();
            return switch (heading) {
                case SOUTH -> new Vec3(x0 + 5.5, y, z0 + 16.5);
                case NORTH -> new Vec3(x0 + 10.5, y, z0 - 0.5);
                case EAST -> new Vec3(x0 + 16.5, y, z0 + 10.5);
                default -> new Vec3(x0 - 0.5, y, z0 + 5.5);
            };
        }

        /** On sort du chunk : chunk suivant, et nouvelle direction si c'est un carrefour. */
        void advance(net.minecraft.util.RandomSource r) {
            dx += heading.getStepX();
            dz += heading.getStepZ();
            if (!crossing(dx, dz)) {
                if (!road(dx + heading.getStepX(), dz + heading.getStepZ()) && !crossing(dx + heading.getStepX(), dz + heading.getStepZ())) {
                    heading = heading.getOpposite();                       // impasse : demi-tour
                }
                return;
            }
            Direction[] opts = bus ? busTurn() : new Direction[]{heading, heading.getClockWise(), heading.getCounterClockWise()};
            float[] weight = {0.6f, 0.2f, 0.2f};
            Direction pick = null;
            for (int tries = 0; tries < 6 && pick == null; tries++) {
                float x = r.nextFloat();
                int i = bus ? 0 : x < weight[0] ? 0 : x < weight[0] + weight[1] ? 1 : 2;
                Direction d = opts[Math.min(i, opts.length - 1)];
                if (road(dx + d.getStepX(), dz + d.getStepZ())) {
                    pick = d;
                }
            }
            heading = pick != null ? pick : heading.getOpposite();
        }

        /** Le bus tourne à droite aux coins du boulevard circulaire (sens horaire). */
        Direction[] busTurn() {
            boolean corner = Math.abs(dx) == BUS_RING && Math.abs(dz) == BUS_RING;
            return new Direction[]{corner ? heading.getClockWise() : heading};
        }
    }

    // ------------------------------------------------------------------ conduite
    /** Commandes (accélérateur, volant) du chauffeur d'un véhicule de la circulation. */
    public static float[] input(VehicleEntity v, HumanBotEntity driver, Route route) {
        ServerLevel level = (ServerLevel) v.level();
        Vec3 pos = v.position();
        // poursuite : la voiture de police fonce vers le suspect puis déploie ses agents
        if (route.chase != null) {
            if (!route.chase.isAlive() || route.chase.distanceToSqr(v) > 140 * 140) {
                route.chase = null;
            } else {
                Vec3 to = route.chase.position().subtract(pos);
                if (to.horizontalDistance() < 9) {
                    deploy(v, route.chase);
                    return new float[]{-1, 0};
                }
                return steer(v, route.chase.position(), 1f);
            }
        }
        if (route.yieldTicks > 0) {                                   // sirène qui approche : on se range
            route.yieldTicks--;
            Vec3 right = Vec3.directionFromRotation(0, v.getYRot() + 90);
            return steer(v, pos.add(Vec3.directionFromRotation(0, v.getYRot()).scale(3)).add(right.scale(2)), 0.2f);
        }
        if (route.stopTicks > 0) {                                    // arrêt de bus
            route.stopTicks--;
            if (route.stopTicks == 0) {
                level.playSound(null, v.getX(), v.getY(), v.getZ(), ModSounds.BUS_BRAKES.get(), SoundSource.NEUTRAL, 1f, 1.2f);
            }
            return new float[]{-1, 0};
        }
        Vec3 exit = route.exitPoint();
        Vec3 along = new Vec3(route.heading.getStepX(), 0, route.heading.getStepZ());
        double ahead = exit.subtract(pos).dot(along);
        if (ahead < 1.5) {
            route.advance(v.getRandom());
            exit = route.exitPoint();
            along = new Vec3(route.heading.getStepX(), 0, route.heading.getStepZ());
            ahead = exit.subtract(pos).dot(along);
            if (route.bus) {
                for (int[] s : BUS_STOPS) {
                    if (s[0] == route.dx && s[1] == route.dz) {
                        route.stopTicks = 200;                        // 10 s à l'abribus
                        level.playSound(null, v.getX(), v.getY(), v.getZ(), ModSounds.BUS_BRAKES.get(), SoundSource.NEUTRAL, 1.2f, 0.9f);
                    }
                }
            }
        }
        // feu rouge au prochain carrefour
        int nx = route.dx + route.heading.getStepX(), nz = route.dz + route.heading.getStepZ();
        if (route.crossing(nx, nz) && ahead < 7 && ahead > 1.5) {
            BlockPos light = new BlockPos((route.city.center().x + nx) * 16 + 2, route.city.baseY() + 4, (route.city.center().z + nz) * 16 + 2);
            Direction axis = route.heading.getAxis() == Direction.Axis.X ? Direction.EAST : Direction.NORTH;
            if (fr.modeminecraft.block.city.TrafficLightBlock.phase(level.getGameTime(), light, axis) != 0) {
                return new float[]{-1, 0};
            }
        }
        // obstacle devant (piéton, joueur, autre véhicule) : on freine, on klaxonne un joueur qui gêne
        Vec3 fwd = Vec3.directionFromRotation(0, v.getYRot());
        AABB box = v.getBoundingBox().move(fwd.scale(3.2)).inflate(0.4, 0, 0.4);
        List<Entity> in = level.getEntities(v, box, e -> (e instanceof LivingEntity && !v.hasPassenger(e)) || e instanceof VehicleEntity);
        if (!in.isEmpty()) {
            route.blockedTicks++;
            if (route.hornCooldown-- <= 0 && route.blockedTicks > 25 && in.stream().anyMatch(e -> e instanceof Player)) {
                route.hornCooldown = 40;
                level.playSound(null, v.getX(), v.getY(), v.getZ(), ModSounds.CAR_HORN.get(), SoundSource.NEUTRAL, 1.4f, route.bus ? 0.7f : 1f);
            }
            return new float[]{-1, 0};
        }
        route.blockedTicks = 0;
        Vec3 aim = exit.add(along.scale(4));
        return steer(v, aim, route.bus ? 0.55f : 0.6f);
    }

    private static float[] steer(VehicleEntity v, Vec3 goal, float throttle) {
        Vec3 to = goal.subtract(v.position());
        float want = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90;
        float diff = Mth.wrapDegrees(want - v.getYRot());
        float side = Mth.clamp(-diff / 20f, -1, 1);
        float cruise = v.type().maxSpeed * (throttle > 0.9f ? 0.85f : 0.45f);
        float fwd = Math.abs(v.speed()) > cruise ? 0 : Math.abs(diff) > 70 ? 0.35f : throttle;
        return new float[]{fwd, side};
    }

    /** Arrivée sur le suspect : sirène, l'équipage descend et l'arrête. */
    private static void deploy(VehicleEntity v, Player target) {
        for (Entity p : List.copyOf(v.getPassengers())) {
            if (p instanceof HumanBotEntity cop && cop.faction().group.equals("police")) {
                cop.stopRiding();
                cop.setNoAi(false);
                cop.setTarget(target);
            }
        }
        v.traffic = null;
        v.convoy = true;                                                // repart avec le reste du convoi une fois loin
    }

    // ------------------------------------------------------------------ apparition / disparition
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 40 != 13) {
            return;
        }
        ServerLevel level = server.overworld();
        for (VehicleEntity v : level.getEntities(ModEntities.VEHICLE.get(), e -> e.trafficCar)) {
            if (v.traffic == null && !v.convoy || level.getNearestPlayer(v, 128) == null) {
                for (Entity p : List.copyOf(v.getPassengers())) {
                    if (p instanceof HumanBotEntity) p.discard();
                }
                if (v.getPassengers().isEmpty() || level.getNearestPlayer(v, 128) == null) {
                    v.ejectPassengers();
                    v.discard();
                }
            }
        }
        for (ServerPlayer player : level.players()) {
            CityPlanner.City city = CityPresence.of(player);
            if (city == null || !city.variant().equals("metropolis") || player.isSpectator()) {
                continue;
            }
            List<VehicleEntity> near = level.getEntitiesOfClass(VehicleEntity.class, player.getBoundingBox().inflate(96), e -> e.trafficCar);
            long cars = near.stream().filter(e -> e.type() != VehicleEntity.Type.BUS).count();
            if (cars < MAX_CARS) {
                spawnCar(level, player, city);
            }
            if (near.stream().noneMatch(e -> e.type() == VehicleEntity.Type.BUS)) {
                spawnBus(level, player, city);
            }
            // un suspect recherché : les voitures de patrouille proches le prennent en chasse
            if (FactionRelations.isHostile(player.getUUID(), Faction.POLICE, level.getGameTime())) {
                chase(level, player);
            }
        }
    }

    private static void spawnCar(ServerLevel level, ServerPlayer player, CityPlanner.City city) {
        var r = level.getRandom();
        int pdx = (player.getBlockX() >> 4) - city.center().x, pdz = (player.getBlockZ() >> 4) - city.center().z;
        for (int tries = 0; tries < 12; tries++) {
            int dx = pdx + r.nextInt(9) - 4, dz = pdz + r.nextInt(9) - 4;
            if (Math.abs(dx - pdx) + Math.abs(dz - pdz) < 2) continue;
            Route probe = new Route(city, dx, dz, Direction.NORTH, false);
            if (!probe.road(dx, dz) || probe.crossing(dx, dz)) continue;
            boolean ns = Math.floorMod(dx + city.rad(), 3) == 0;
            Direction h = ns ? (r.nextBoolean() ? Direction.SOUTH : Direction.NORTH) : (r.nextBoolean() ? Direction.EAST : Direction.WEST);
            float t = r.nextFloat();
            VehicleEntity.Type type = t < 0.55f ? VehicleEntity.Type.SEDAN : t < 0.82f ? VehicleEntity.Type.TAXI : VehicleEntity.Type.POLICE_CAR;
            spawn(level, city, new Route(city, dx, dz, h, false), type);
            return;
        }
    }

    private static void spawnBus(ServerLevel level, ServerPlayer player, CityPlanner.City city) {
        var r = level.getRandom();
        int side = r.nextInt(4);
        int along = r.nextInt(2 * BUS_RING - 1) - BUS_RING + 1;
        if (Math.floorMod(along + city.rad(), 3) == 0) along += 1;
        int dx = side == 0 ? along : side == 1 ? BUS_RING : side == 2 ? -along : -BUS_RING;
        int dz = side == 0 ? -BUS_RING : side == 1 ? along : side == 2 ? BUS_RING : -along;
        Direction h = side == 0 ? Direction.EAST : side == 1 ? Direction.SOUTH : side == 2 ? Direction.WEST : Direction.NORTH;
        Vec3 at = new Vec3((city.center().x + dx) * 16 + 8, city.baseY(), (city.center().z + dz) * 16 + 8);
        if (player.position().distanceTo(at) > 90 || player.position().distanceTo(at) < 24) {
            return;
        }
        spawn(level, city, new Route(city, dx, dz, h, true), VehicleEntity.Type.BUS);
    }

    private static void spawn(ServerLevel level, CityPlanner.City city, Route route, VehicleEntity.Type type) {
        Vec3 exit = route.exitPoint();
        Vec3 back = new Vec3(-route.heading.getStepX(), 0, -route.heading.getStepZ()).scale(8);
        Vec3 at = exit.add(back);
        if (!level.isLoaded(BlockPos.containing(at)) || !level.getEntitiesOfClass(VehicleEntity.class, new AABB(at, at).inflate(6)).isEmpty()) {
            return;
        }
        VehicleEntity v = new VehicleEntity(level, type, at.x, at.y + 0.1, at.z, route.heading.toYRot());
        v.trafficCar = true;
        v.traffic = route;
        level.addFreshEntity(v);
        boolean police = type == VehicleEntity.Type.POLICE_CAR;
        int crew = police ? 2 : 1;
        for (int i = 0; i < crew; i++) {
            HumanBotEntity d = ModEntities.HUMAN_BOT.get().create(level);
            if (d == null) continue;
            d.moveTo(at.x, at.y, at.z, route.heading.toYRot(), 0);
            d.setup(police ? Faction.POLICE : Faction.CIVILIAN, police ? BotRole.SOLDIER : BotRole.CIVILIAN, level.getRandom());
            d.setNoAi(true);
            d.convoy = true;
            level.addFreshEntity(d);
            d.startRiding(v, true);
        }
    }

    /** Poursuite : les voitures de patrouille à 120 blocs allument sirène et gyrophares et foncent sur le suspect. */
    public static void chase(ServerLevel level, Player player) {
        for (VehicleEntity v : level.getEntitiesOfClass(VehicleEntity.class, player.getBoundingBox().inflate(120),
                e -> e.trafficCar && e.traffic != null && e.type() == VehicleEntity.Type.POLICE_CAR && e.traffic.chase == null)) {
            v.traffic.chase = player;
            if (!v.sirenOn()) {
                v.toggleSiren();
            }
        }
    }

    /** Sirène : les véhicules civils proches se rangent sur le côté. */
    public static void yieldTo(VehicleEntity siren) {
        for (VehicleEntity v : siren.level().getEntitiesOfClass(VehicleEntity.class, siren.getBoundingBox().inflate(20),
                e -> e != siren && e.traffic != null && e.traffic.chase == null)) {
            v.traffic.yieldTicks = 60;
        }
    }

    static {
        fr.modeminecraft.world.police.PolicePatrols.chase = Traffic::chase;
        fr.modeminecraft.world.police.PolicePatrols.yield = Traffic::yieldTo;
    }

    private Traffic() {}
}

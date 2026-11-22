package fr.modeminecraft.entity.train;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.entity.military.MilGeometry;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModSounds;
import fr.modeminecraft.world.city.CityPlanner;
import fr.modeminecraft.world.rail.RailNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Un élément du train des villes du Mesa : la locomotive (index 0, qui calcule tout) ou un wagon de voyageurs.
 * <p>
 * Le train suit la ligne du {@link RailNetwork} (abscisse curviligne {@code s} du nez de la locomotive) ; les wagons sont
 * placés derrière elle sur la même voie. Tout est calculé sur le serveur (les clients interpolent les positions).
 * Le conducteur est un PNJ (conduite automatique : accélération, freinage en gare, arrêt de 25 s portes ouvertes, sifflet
 * au départ) ; un joueur peut le déloger (Shift + clic droit) et conduire : Z accélère, S freine puis recule, H sifflet,
 * O portes. À chaque arrêt, les fraudeurs étourdis par les contrôleurs sont expulsés sur le quai.
 */
public class MesaTrainCarEntity extends Entity {
    public static final int LOCOMOTIVE = 0, WAGON = 1;
    public static final String LOCO_MODEL = "mesa_locomotive", WAGON_MODEL = "mesa_wagon";
    public static final float LOCO_HALF = 4.4f, WAGON_LEN = 11.4f;
    public static final double V_MAX = 0.7, ACCEL = 0.008, DECEL = 0.012;
    public static final int STOP_TICKS = 500;                  // 25 secondes en gare

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MesaTrainCarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DOORS = SynchedEntityData.defineId(MesaTrainCarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> DOORS_AT = SynchedEntityData.defineId(MesaTrainCarEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(MesaTrainCarEntity.class, EntityDataSerializers.FLOAT);

    // ------------------------------------------------------------------ état persistant
    private UUID trainId = UUID.randomUUID();
    private int index;
    /** Ville de départ (le train y est ou vient d'en partir) et ville d'arrivée, en coordonnées de région. */
    private int curRx, curRz, dstRx, dstRz, prevRx = Integer.MIN_VALUE, prevRz;
    private int sigma = 1;                                       // +1 : sens canonique de la ligne (de a vers b)
    private double s;                                            // abscisse du nez de la locomotive sur la ligne
    private float speed;
    private boolean stopped;
    private int stopTicks;
    private boolean planned;
    private final Set<UUID> validated = new HashSet<>();         // passagers dont le billet a été composté pour ce trajet
    private final Set<UUID> fraudsters = new HashSet<>();
    private final Map<UUID, Long> stunCooldown = new HashMap<>();

    // ------------------------------------------------------------------ état transitoire
    private RailNetwork.Line line;
    private List<MesaTrainCarEntity> cars = List.of();
    private long carsAt = -100;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYaw;
    private boolean whistled;
    private boolean pendingOpen;

    public MesaTrainCarEntity(EntityType<? extends MesaTrainCarEntity> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    public static MesaTrainCarEntity create(ServerLevel level, int kind, UUID train, int index) {
        MesaTrainCarEntity e = new MesaTrainCarEntity(ModEntities.MESA_TRAIN_CAR.get(), level);
        e.entityData.set(KIND, kind);
        e.trainId = train;
        e.index = index;
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, WAGON).define(DOORS, false).define(DOORS_AT, 0L).define(SPEED, 0f);
    }

    public int kind() {
        return entityData.get(KIND);
    }

    public boolean isLoco() {
        return kind() == LOCOMOTIVE;
    }

    public String modelId() {
        return isLoco() ? LOCO_MODEL : WAGON_MODEL;
    }

    public boolean doorsOpen() {
        return entityData.get(DOORS);
    }

    /** Ouverture des portes (0 fermées, 1 ouvertes), lissée sur 1 seconde. */
    public float doors(float partial) {
        float t = Mth.clamp((level().getGameTime() + partial - entityData.get(DOORS_AT)) / 20f, 0, 1);
        return doorsOpen() ? t : 1 - t;
    }

    public float trainSpeed() {
        return entityData.get(SPEED);
    }

    public UUID trainId() {
        return trainId;
    }

    public int index() {
        return index;
    }

    private void setDoors(boolean open) {
        if (doorsOpen() != open) {
            entityData.set(DOORS, open);
            entityData.set(DOORS_AT, level().getGameTime());
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL, 1.2f, open ? 0.8f : 0.6f);
        }
    }

    // ------------------------------------------------------------------ sièges
    public int capacity() {
        return isLoco() ? 1 : 12;
    }

    private float[] seat(int i) {
        MilGeometry.Geo g = MilGeometry.get(modelId());
        float[][] seats = g.seats();
        return seats[Math.min(i, seats.length - 1)];
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < capacity();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return null;                                            // serveur seul aux commandes : les clients ne simulent rien
    }

    /** Le conducteur (siège 0 de la locomotive). */
    public Entity driver() {
        return isLoco() && !getPassengers().isEmpty() ? getPassengers().get(0) : null;
    }

    public Vec3 toWorld(double fwd, double up, double right) {
        double rad = Math.toRadians(getYRot());
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        return new Vec3(getX() + fx * fwd - fz * right, getY() + up, getZ() + fz * fwd + fx * right);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (!hasPassenger(passenger)) {
            return;
        }
        if (passenger instanceof HumanBotEntity bot && bot.role() == BotRole.TRAIN_INSPECTOR) {
            // le contrôleur arpente le couloir central, d'un bout à l'autre du wagon
            double t = (level().getGameTime() * 0.025) + (bot.getId() % 7) * 1.3;
            double x = Math.sin(t) * 3.6;
            Vec3 w = toWorld(x, 0.97, 0);
            move.accept(passenger, w.x, w.y, w.z);
            bot.setYRot(getYRot() + (Math.cos(t) >= 0 ? 0 : 180));
            bot.setYBodyRot(bot.getYRot());
            bot.setYHeadRot(bot.getYRot());
            return;
        }
        float[] o = seat(getPassengers().indexOf(passenger));
        Vec3 w = toWorld(o[0], o[1] - 0.6, o[2]);
        move.accept(passenger, w.x, w.y, w.z);
        if (passenger instanceof LivingEntity le && !(passenger instanceof Player)) {
            le.setYRot(getYRot());
            le.setYBodyRot(getYRot());
            le.setYHeadRot(getYRot());
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // quai : sur le côté (le sud de la voie dans les gares), sinon à côté du wagon
        return toWorld(0, 0.1, 2.6);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;                                           // indestructible (rail, pas de dégâts)
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 256 * 256;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        MesaTrainCarEntity loco = leader();
        if (loco == null || Math.abs(loco.speed) > 0.05f) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.train.moving"), true);
            return InteractionResult.CONSUME;
        }
        if (isLoco()) {
            Entity d = driver();
            if (d != null && d != player) {
                if (!player.isSecondaryUseActive()) {
                    player.displayClientMessage(Component.translatable("message.modeminecraft.train.driver_busy"), true);
                    return InteractionResult.CONSUME;
                }
                // Shift + clic droit : on déloge le conducteur
                TrainStaff.force(() -> d.stopRiding());
                Vec3 off = toWorld(0, 0.1, 2.4);
                d.teleportTo(off.x, off.y, off.z);
            }
            player.startRiding(this);
            player.displayClientMessage(Component.translatable("message.modeminecraft.train.drive"), true);
            return InteractionResult.CONSUME;
        }
        if (!doorsOpen() && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.train.doors_closed"), true);
            return InteractionResult.CONSUME;
        }
        if (getPassengers().size() >= capacity()) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.train.full"), true);
            return InteractionResult.CONSUME;
        }
        player.startRiding(this);
        return InteractionResult.CONSUME;
    }

    /** La locomotive du train (elle-même si c'en est une). */
    public MesaTrainCarEntity leader() {
        if (isLoco()) {
            return this;
        }
        for (MesaTrainCarEntity c : trainCars()) {
            if (c.isLoco()) {
                return c;
            }
        }
        return null;
    }

    /** Éléments chargés de ce train, locomotive d'abord puis wagons dans l'ordre. */
    public List<MesaTrainCarEntity> trainCars() {
        long now = level().getGameTime();
        if (now - carsAt >= 10) {
            carsAt = now;
            List<MesaTrainCarEntity> list = new ArrayList<>(level().getEntitiesOfClass(MesaTrainCarEntity.class,
                    getBoundingBox().inflate(70, 20, 70), c -> c.trainId.equals(trainId)));
            list.sort((a, b) -> Integer.compare(a.index, b.index));
            cars = list;
        }
        return cars;
    }

    // ------------------------------------------------------------------ boucle principale
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        if (!isLoco()) {
            return;                                             // la locomotive place les wagons
        }
        CityPlanner.Env env = CityPlanner.Env.of(sl);
        if (!planned) {
            planNext(env, true);
        }
        if (!planned || !ensureLine(env)) {
            return;
        }
        List<MesaTrainCarEntity> cars = trainCars();
        if (pendingOpen && cars.size() >= expectedCars()) {
            setDoorsAll(cars, true);                              // départ initial : portes ouvertes en gare
            pendingOpen = false;
        }
        Entity driver = driver();
        boolean manual = driver instanceof Player;
        boolean auto = driver instanceof HumanBotEntity;
        double len = line.length();
        double dir = sigma;
        boolean allHere = cars.size() >= expectedCars();
        // arrêt en gare
        if (stopped) {
            speed = 0;
            if (manual) {
                if (((Player) driver).zza != 0) {
                    leaveStation(sl, env);
                }
            } else if (auto && allHere) {
                stopTicks--;
                if (stopTicks == 60) {
                    whistle(sl);
                }
                if (stopTicks == 30) {
                    setDoorsAll(cars, false);
                }
                if (stopTicks <= 0) {
                    leaveStation(sl, env);
                }
            }
        } else if (!allHere || !lookAheadLoaded(sl, dir)) {
            speed = Math.max(0, speed - (float) DECEL);          // chunks ou wagons pas là : on ne fonce pas dans le vide
        } else if (manual) {
            drive((Player) driver, len);
        } else if (auto) {
            autoDrive(sl, env);
        } else {
            speed = Math.max(0, speed - 0.004f);                 // pas de conducteur : le train ralentit et s'arrête
        }
        if (!stopped) {
            s += dir * speed;
        }
        entityData.set(SPEED, speed);
        place(cars);
        if (!stopped && Math.abs(speed) > 0.12f) {
            hitAhead(sl);
        }
        if (tickCount % 4 == 0) {
            TrainStaff.tick(this, cars);
        }
        if (Math.abs(speed) > 0.02f && tickCount % 24 == 0) {
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 0.9f, 0.5f);
        }
    }

    private int expectedCars() {
        return Math.max(1, wagonCount + 1);
    }

    private int wagonCount = 2;

    public void setWagonCount(int n) {
        wagonCount = n;
    }

    private boolean lookAheadLoaded(ServerLevel sl, double dir) {
        double[] p = line.at(s + dir * 30);
        return sl.hasChunkAt(BlockPos.containing(p[0], p[2], p[1])) && sl.hasChunkAt(BlockPos.containing(line.at(s)[0], line.at(s)[2], line.at(s)[1]));
    }

    private boolean ensureLine(CityPlanner.Env env) {
        if (line != null) {
            return true;
        }
        CityPlanner.City a = CityPlanner.city(env, curRx, curRz), b = CityPlanner.city(env, dstRx, dstRz);
        if (a == null || b == null) {
            return false;
        }
        line = RailNetwork.line(env, a, b);
        return true;
    }

    /** Choisit la prochaine ville (même sens de marche si possible, sans faire demi-tour sur ses pas) et se place en gare. */
    private void planNext(CityPlanner.Env env, boolean first) {
        CityPlanner.City here = CityPlanner.city(env, curRx, curRz);
        if (here == null) {
            return;
        }
        int facing = first ? 1 : (line != null ? (int) line.dirX() * sigma : 1);
        List<CityPlanner.City> links = RailNetwork.links(env, here);
        if (links.isEmpty()) {
            return;
        }
        CityPlanner.City best = null;
        for (int pass = 0; pass < 4 && best == null; pass++) {
            for (CityPlanner.City o : links) {
                int heading = o.middleX() >= here.middleX() ? 1 : -1;
                boolean back = o.rx() == prevRx && o.rz() == prevRz;
                boolean ok = switch (pass) {
                    case 0 -> heading == facing && !back;
                    case 1 -> heading == facing;
                    case 2 -> !back;
                    default -> true;
                };
                if (ok) {
                    best = o;
                    break;
                }
            }
        }
        dstRx = best.rx();
        dstRz = best.rz();
        line = RailNetwork.line(env, here, best);
        boolean hereIsA = line.a().rx() == here.rx() && line.a().rz() == here.rz();
        sigma = hereIsA ? 1 : -1;
        double center = hereIsA ? 0 : line.length();
        s = center + sigma * RailNetwork.STOP_AHEAD;
        planned = true;
        if (first) {
            stopped = true;
            stopTicks = 160;
            pendingOpen = true;
        }
    }

    private void leaveStation(ServerLevel sl, CityPlanner.Env env) {
        planNext(env, false);                                   // ligne suivante : même sens de marche si possible, sinon demi-tour
        stopped = false;
        whistled = false;
        validated.clear();
        setDoorsAll(trainCars(), false);
        speed = 0;
        sl.playSound(null, getX(), getY(), getZ(), ModSounds.TRAIN_WHISTLE.get(), SoundSource.NEUTRAL, 6f, 1f);
    }

    private void whistle(ServerLevel sl) {
        sl.playSound(null, getX(), getY(), getZ(), ModSounds.TRAIN_WHISTLE.get(), SoundSource.NEUTRAL, 6f, 1f);
    }

    /** Touche H : sifflet du conducteur. */
    public void honk(Entity by) {
        if (isLoco() && by == driver()) {
            whistle((ServerLevel) level());
        }
    }

    /** Touche O : portes (à l'arrêt seulement). */
    public void toggleDoors(Entity by) {
        MesaTrainCarEntity loco = leader();
        if (loco == null || by != loco.driver() || Math.abs(loco.speed) > 0.05f) {
            return;
        }
        boolean open = !doorsOpen();
        for (MesaTrainCarEntity c : loco.trainCars()) {
            c.setDoors(open);
        }
    }

    private void setDoorsAll(List<MesaTrainCarEntity> cars, boolean open) {
        for (MesaTrainCarEntity c : cars) {
            c.setDoors(open);
        }
    }

    /** Conduite automatique : vitesse de croisière, freinage progressif devant le quai, arrêt exact. */
    private void autoDrive(ServerLevel sl, CityPlanner.Env env) {
        double len = line.length();
        double destCenter = sigma > 0 ? len : 0;
        double stop = destCenter + sigma * RailNetwork.STOP_AHEAD;
        double remaining = sigma * (stop - s);
        double target = remaining <= 0 ? 0 : Math.min(V_MAX, Math.sqrt(2 * DECEL * remaining) + 0.02);
        if (speed < target) {
            speed = (float) Math.min(target, speed + ACCEL);
        } else if (speed > target) {
            speed = (float) Math.max(target, speed - DECEL);
        }
        if (remaining <= 0.4 && speed < 0.06) {
            s = stop;
            arrive(sl, env);
        }
    }

    private void arrive(ServerLevel sl, CityPlanner.Env env) {
        speed = 0;
        prevRx = curRx;
        prevRz = curRz;
        curRx = dstRx;
        curRz = dstRz;
        stopped = true;
        stopTicks = STOP_TICKS;
        List<MesaTrainCarEntity> cars = trainCars();
        setDoorsAll(cars, true);
        TrainStaff.arrived(this, cars, CityPlanner.city(env, curRx, curRz));
    }

    /** Conduite par le joueur. */
    private void drive(Player p, double len) {
        float zza = p.zza;
        if (zza > 0) {
            speed = (float) Math.min(V_MAX, speed + ACCEL * 1.2);
        } else if (zza < 0) {
            speed = speed > 0.02f ? (float) (speed - DECEL * 2.5) : (float) Math.max(-0.2, speed - ACCEL);
        } else {
            speed = Math.abs(speed) < 0.004f ? 0 : speed - Math.signum(speed) * 0.002f;
        }
        double next = s + sigma * speed;
        if (next < -34 || next > len + 34) {
            speed = 0;
        }
        // en gare, à l'arrêt, au quai : portes automatiques comme le conducteur PNJ
        if (Math.abs(speed) < 0.02f && Math.abs(sigma * (s - (sigma > 0 ? len : 0)) - RailNetwork.STOP_AHEAD) < 1.5) {
            // arrivé : on enregistre la gare
            speed = 0;
            CityPlanner.Env env = CityPlanner.Env.of((ServerLevel) level());
            arrive((ServerLevel) level(), env);
        }
    }

    /** Positionne la locomotive et les wagons le long de la ligne. */
    private void place(List<MesaTrainCarEntity> cars) {
        for (MesaTrainCarEntity c : cars) {
            double off = c.index == 0 ? LOCO_HALF : 9.8 + 5.2 + (c.index - 1) * WAGON_LEN;
            double sc = s - sigma * off;
            double[] p = line.at(sc);
            float yaw = line.yaw(sc) + (sigma < 0 ? 180 : 0);
            double y = Math.round(p[2]) + 1 + 0.0625;
            if (c.tickCount > 2 && Math.abs(c.getY() - y) < 1.6) {
                y = c.getY() + (y - c.getY()) * 0.3;
            }
            c.setPos(p[0], y, p[1]);
            c.setYRot(yaw);
            if (c != this) {
                c.entityData.set(SPEED, speed);
            }
            if (c.tickCount == 0) {
                c.xo = p[0];
                c.yo = y;
                c.zo = p[1];
                c.yRotO = yaw;
            }
        }
    }

    /** Le nez de la locomotive blesse (sans tuer d'un coup) ce qu'il percute. */
    private void hitAhead(ServerLevel sl) {
        Vec3 nose = toWorld(LOCO_HALF + 1.2, 1.2, 0);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(nose, nose).inflate(1.4, 1.2, 1.4))) {
            if (e.getVehicle() instanceof MesaTrainCarEntity || e instanceof HumanBotEntity hb && hb.role().rail()) {
                continue;
            }
            e.hurt(sl.damageSources().generic(), 6 + Math.abs(speed) * 12);
            Vec3 d = nose.subtract(position()).normalize();
            e.setDeltaMovement(d.x * 1.1, 0.5, d.z * 1.1);
            e.hurtMarked = true;
        }
    }

    // ------------------------------------------------------------------ client
    private void clientTick() {
        if (lerpSteps > 0) {
            double k = 1.0 / lerpSteps;
            setPos(getX() + (lerpX - getX()) * k, getY() + (lerpY - getY()) * k, getZ() + (lerpZ - getZ()) * k);
            setYRot((float) (getYRot() + Mth.wrapDegrees(lerpYaw - getYRot()) * k));
            lerpSteps--;
        }
        if (isLoco() && trainSpeed() > 0.01f || isLoco() && stoppedSmoke()) {
            Vec3 chimney = toWorld(3.2, 4.2, 0);
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, chimney.x, chimney.y, chimney.z, 0, 0.07, 0);
        }
    }

    private boolean stoppedSmoke() {
        return tickCount % 8 == 0;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYaw = yaw;
        lerpSteps = 3;
    }

    // ------------------------------------------------------------------ accès pour le personnel
    public Set<UUID> validated() {
        return validated;
    }

    public Set<UUID> fraudsters() {
        return fraudsters;
    }

    public Map<UUID, Long> stunCooldown() {
        return stunCooldown;
    }

    public boolean isStopped() {
        return stopped;
    }

    /** Étage de la ville d'arrivée : hauteur et position du quai pour y déposer un voyageur. */
    public Vec3 platformSpot(CityPlanner.City city) {
        return new Vec3(getX(), city.baseY() + 0.1, RailNetwork.railZ(city) + 2.2);
    }

    // ------------------------------------------------------------------ sauvegarde
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(KIND, tag.getInt("Kind"));
        if (tag.hasUUID("Train")) {
            trainId = tag.getUUID("Train");
        }
        index = tag.getInt("Index");
        wagonCount = tag.contains("Wagons") ? tag.getInt("Wagons") : 2;
        curRx = tag.getInt("CurRx");
        curRz = tag.getInt("CurRz");
        dstRx = tag.getInt("DstRx");
        dstRz = tag.getInt("DstRz");
        prevRx = tag.contains("PrevRx") ? tag.getInt("PrevRx") : Integer.MIN_VALUE;
        prevRz = tag.getInt("PrevRz");
        sigma = tag.getInt("Sigma") == 0 ? 1 : tag.getInt("Sigma");
        s = tag.getDouble("S");
        stopped = tag.getBoolean("Stopped");
        stopTicks = tag.getInt("StopTicks");
        planned = tag.getBoolean("Planned");
        speed = 0;
        ListTag v = tag.getList("Validated", 11);
        validated.clear();
        for (int i = 0; i < v.size(); i++) {
            validated.add(NbtUtils.loadUUID(v.get(i)));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", kind());
        tag.putUUID("Train", trainId);
        tag.putInt("Index", index);
        tag.putInt("Wagons", wagonCount);
        tag.putInt("CurRx", curRx);
        tag.putInt("CurRz", curRz);
        tag.putInt("DstRx", dstRx);
        tag.putInt("DstRz", dstRz);
        if (prevRx != Integer.MIN_VALUE) {
            tag.putInt("PrevRx", prevRx);
            tag.putInt("PrevRz", prevRz);
        }
        tag.putInt("Sigma", sigma);
        tag.putDouble("S", s);
        tag.putBoolean("Stopped", stopped);
        tag.putInt("StopTicks", stopTicks);
        tag.putBoolean("Planned", planned);
        ListTag v = new ListTag();
        for (UUID u : validated) {
            v.add(NbtUtils.createUUID(u));
        }
        tag.put("Validated", v);
    }

    /** Position de départ : ville de la gare, la locomotive se place en tête du quai. */
    public void startAt(CityPlanner.City station) {
        curRx = station.rx();
        curRz = station.rz();
        dstRx = curRx;
        dstRz = curRz;
        planned = false;
    }

    @Override
    public EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        return EntityDimensions.scalable(2.8f, 3.6f);
    }

    public static net.minecraft.resources.ResourceLocation id() {
        return ModeMinecraft.id("mesa_train_car");
    }
}

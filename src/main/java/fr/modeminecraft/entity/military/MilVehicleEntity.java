package fr.modeminecraft.entity.military;

import fr.modeminecraft.entity.VehicleEntity;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.item.gun.TriggerHandler;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Véhicule militaire (cahier des charges 0.4) : 4x4 et camions, blindés à roues ou à chenilles, chars, hélicoptères
 * et complexes lance-missiles.
 * <ul>
 * <li>places avec rôles (conducteur / pilote, tireur, commandant, passagers, servants de sabord) ; touche C pour changer
 * de place ; habitacle blindé : les passagers protégés ne prennent ni balles ni éclats ;</li>
 * <li>tourelle et canon qui suivent le regard du tireur (vitesse de pointage, élévation limitée) ; clic gauche = arme
 * principale, clic droit = arme secondaire ; R = type de munition ;</li>
 * <li>soutes à munitions (clic droit avec la munition), carburant (bidon), réparation (clé à molette + plaques) ;</li>
 * <li>blindage directionnel (avant > flancs > arrière), moteur arrière (immobilisation + incendie), chenilles / roues
 * détruites (-70 % de vitesse), fumigènes (X), protection active (T-14), brouilleur Shtora (T-90M) ;</li>
 * <li>les chars et gros blindés écrasent arbres, clôtures, vitres et voitures ; amphibies ; hélicoptères.</li>
 * </ul>
 */
public class MilVehicleEntity extends Entity {
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FUEL = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STEER = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TURRET = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTOR = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> SEATS = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> INFO = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.COMPOUND_TAG);

    /** Bits de STATE. */
    public static final int ENGINE_DOWN = 1, MOBILITY_DOWN = 2, DEPLOYED = 4, BURNING = 8, LOCKING = 16, LOCKED = 32;

    /** Entrées d'un pilote d'hélicoptère côté client (monter / descendre) : fournies par le code client. */
    public static java.util.function.IntSupplier heliLift = () -> 0;

    // ------------------------------------------------------------------ état
    private float speed, steer;
    public float wheelRot, wheelRotO, rotorAngle, rotorAngleO, radarAngle, radarAngleO;
    /** Inclinaison visuelle (hélicoptère : piqué / roulis), en degrés. */
    public float tilt, tiltO, roll, rollO;
    private float turretLocal, pitchLocal;
    public float turretO, pitchO;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYaw;
    private int stuckTicks;
    private final MilPart[] parts;
    /** Soutes : coups restants par type de munition. */
    private final Map<MilAmmo, Integer> stock = new EnumMap<>(MilAmmo.class);
    private final int[] selected, lastShot, heat;
    private final boolean[] alternate;
    private boolean infoDirty = true;
    private int smoke = 3, aps = 4, smokeTimer, apsTimer, burnTicks;
    private final SimpleContainer trunk;
    /** Cible verrouillée (missiles sol-air) et temps de verrouillage. */
    private int lockTarget = -1, lockCandidate = -1, lockTicks;
    /** Pilote automatique (bot) : destination, véhicule de tête. */
    public BlockPos destination;
    public java.util.UUID leader;
    /** Véhicule de patrouille / de convoi (disparaît avec son équipage quand aucun joueur n'est proche). */
    public boolean patrol;

    public MilVehicleEntity(EntityType<? extends MilVehicleEntity> et, Level level, MilType t) {
        super(et, level);
        entityData.set(TYPE, t.ordinal());
        entityData.set(HEALTH, t.health);
        blocksBuilding = true;
        float[] d = dims(t);
        int n = Math.max(1, (int) Math.ceil(d[0] / d[1]));
        List<MilPart> list = new ArrayList<>();
        if (n >= 2) {
            for (int i = 0; i < n; i++) {
                list.add(new MilPart(this, i, d[1], d[2]));
            }
        }
        parts = list.toArray(new MilPart[0]);
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        int nw = t.weapons.size();
        selected = new int[nw];
        lastShot = new int[nw];
        heat = new int[nw];
        alternate = new boolean[nw];
        trunk = new SimpleContainer(Math.max(9, t.trunk));
    }

    public static MilVehicleEntity create(Level level, MilType t, double x, double y, double z, float yaw) {
        MilVehicleEntity v = new MilVehicleEntity(ModEntities.MIL_VEHICLE_TYPES.get(t).get(), level, t);
        v.setPos(x, y, z);
        v.setYRot(yaw);
        v.yRotO = yaw;
        v.entityData.set(HEALTH, t.health);
        v.entityData.set(FUEL, 0.6f);
        return v;
    }

    /** Remplit les soutes (fraction de la contenance) : véhicules des hangars, des patrouilles, du créatif. */
    public void fillAmmo(float fraction) {
        for (MilType.Weapon w : type().weapons) {
            for (int i = 0; i < w.ammo().size(); i++) {
                MilAmmo a = w.ammo().get(i);
                int cap = capacity(a);
                stock.put(a, Math.max(stock.getOrDefault(a, 0), Math.round(cap * fraction / (i == 0 ? 1f : 2f))));
            }
        }
        infoDirty = true;
    }

    public void setFuel(float f) {
        entityData.set(FUEL, Mth.clamp(f, 0, 1));
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                parts[i].setId(id + i + 1);
            }
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return parts.length > 0;
    }

    @Override
    public net.neoforged.neoforge.entity.PartEntity<?>[] getParts() {
        return parts;
    }

    /** Longueur, largeur, hauteur (blocs). */
    public static float[] dims(MilType t) {
        return switch (t) {
            case UAZ_469 -> new float[]{4.2f, 1.9f, 2.0f};
            case TIGR -> new float[]{5.6f, 2.3f, 2.4f};
            case KAMAZ -> new float[]{7.8f, 2.5f, 3.2f};
            case BRDM_2 -> new float[]{5.8f, 2.3f, 2.6f};
            case BTR_80 -> new float[]{7.4f, 2.6f, 2.6f};
            case BMP_2 -> new float[]{6.8f, 3.1f, 2.6f};
            case BM_21_GRAD -> new float[]{7.4f, 2.5f, 3.0f};
            case ZSU_23_4 -> new float[]{6.5f, 3.0f, 2.9f};
            case MI_24 -> new float[]{13.5f, 2.4f, 3.4f};
            case T_90M, T_80BVM -> new float[]{6.9f, 3.6f, 2.4f};
            case T_14_ARMATA -> new float[]{8.4f, 3.6f, 2.6f};
            case BMPT_TERMINATOR -> new float[]{6.8f, 3.5f, 2.5f};
            case SPRUT_SD -> new float[]{6.9f, 3.1f, 2.4f};
            case HUMVEE -> new float[]{4.8f, 2.2f, 2.1f};
            case M_ATV -> new float[]{6.2f, 2.5f, 2.7f};
            case M939 -> new float[]{7.8f, 2.5f, 3.1f};
            case STRYKER -> new float[]{7.0f, 2.7f, 2.6f};
            case M113 -> new float[]{5.0f, 2.7f, 2.2f};
            case M2A3_BRADLEY -> new float[]{6.6f, 3.6f, 3.0f};
            case M1A2_ABRAMS -> new float[]{7.8f, 3.7f, 2.5f};
            case M142_HIMARS -> new float[]{7.2f, 2.5f, 3.0f};
            case AVENGER -> new float[]{4.8f, 2.2f, 2.4f};
            case UH_60_BLACK_HAWK -> new float[]{14.5f, 2.4f, 3.1f};
            case AH_64E_APACHE -> new float[]{13.5f, 2.2f, 2.8f};
            case S_400, ISKANDER -> new float[]{12.6f, 2.8f, 3.0f};
            case S_500 -> new float[]{13.6f, 2.9f, 3.1f};
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(TYPE, 0).define(HEALTH, 50f).define(FUEL, 0.6f).define(SPEED, 0f).define(STEER, 0f).define(TURRET, 0f).define(PITCH, 0f)
                .define(ROTOR, 0f).define(STATE, 0).define(SEATS, new CompoundTag()).define(INFO, new CompoundTag());
    }

    public MilType type() {
        return MilType.values()[Math.floorMod(entityData.get(TYPE), MilType.values().length)];
    }

    public float health() {
        return entityData.get(HEALTH);
    }

    public float fuel() {
        return entityData.get(FUEL);
    }

    public float speed() {
        return entityData.get(SPEED);
    }

    public float steering() {
        return entityData.get(STEER);
    }

    public float rotor() {
        return entityData.get(ROTOR);
    }

    public int state() {
        return entityData.get(STATE);
    }

    public boolean is(int bit) {
        return (state() & bit) != 0;
    }

    private void setState(int bit, boolean on) {
        int s = state();
        entityData.set(STATE, on ? s | bit : s & ~bit);
    }

    public CompoundTag info() {
        return entityData.get(INFO);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float[] d = dims(type());
        return EntityDimensions.scalable(d[1], d[2]);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (TYPE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public float maxUpStep() {
        return type().heli() ? 0.6f : type().tracked() ? 1.25f : 1.05f;
    }

    // ------------------------------------------------------------------ places
    /** Place occupée par ce passager (-1 : aucune). */
    public int seatOf(Entity e) {
        CompoundTag t = entityData.get(SEATS);
        String k = Integer.toString(e.getId());
        return t.contains(k) ? t.getInt(k) : -1;
    }

    public Entity occupant(int seat) {
        for (Entity p : getPassengers()) {
            if (seatOf(p) == seat) {
                return p;
            }
        }
        return null;
    }

    private void assignSeat(Entity e, int seat) {
        CompoundTag t = entityData.get(SEATS).copy();
        // on nettoie les passagers partis
        for (String k : List.copyOf(t.getAllKeys())) {
            boolean present = false;
            for (Entity p : getPassengers()) {
                present |= Integer.toString(p.getId()).equals(k);
            }
            if (!present) {
                t.remove(k);
            }
        }
        if (seat < 0) {
            t.remove(Integer.toString(e.getId()));
        } else {
            t.putInt(Integer.toString(e.getId()), seat);
        }
        entityData.set(SEATS, t);
    }

    public MilType.Seat seat(Entity e) {
        int s = seatOf(e);
        return s < 0 || s >= type().seats.size() ? null : type().seats.get(s);
    }

    /** Première place libre (en partant de `from`). */
    private int freeSeat(int from, boolean driverFirst) {
        int n = type().seats.size();
        for (int k = 0; k < n; k++) {
            int s = (from + k) % n;
            if (!driverFirst && s == 0 && from != 0) {
                continue;
            }
            if (occupant(s) == null) {
                return s;
            }
        }
        return -1;
    }

    /** Touche C : place suivante libre. */
    public void nextSeat(Entity e) {
        int cur = seatOf(e);
        int n = type().seats.size();
        for (int k = 1; k < n; k++) {
            int s = (cur + k) % n;
            if (occupant(s) == null) {
                assignSeat(e, s);
                if (e instanceof Player p) {
                    p.displayClientMessage(Component.translatable("message.modeminecraft.mil.seat",
                            Component.translatable("seat.modeminecraft." + type().seats.get(s).role().name().toLowerCase())), true);
                }
                return;
            }
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < type().seats.size();
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!level().isClientSide) {
            int s = freeSeat(0, true);
            assignSeat(passenger, Math.max(0, s));
            if (passenger instanceof Player p) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.mil.seat",
                        Component.translatable("seat.modeminecraft." + type().seats.get(Math.max(0, s)).role().name().toLowerCase())), true);
            }
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide) {
            assignSeat(passenger, -1);
        }
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity d = occupant(0);
        return d instanceof Player || d instanceof HumanBotEntity ? (LivingEntity) d : null;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (!hasPassenger(passenger)) {
            return;
        }
        int s = Math.max(0, seatOf(passenger));
        float[][] seats = type().geo().seats();
        float[] o = seats[Math.min(s, seats.length - 1)];
        Vec3 w = toWorld(o[0], o[1] - 0.6, o[2]);
        move.accept(passenger, w.x, w.y, w.z);
        if (passenger instanceof LivingEntity le && !(passenger instanceof Player)) {
            le.setYBodyRot(getYRot());
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        float[] d = dims(type());
        int s = Math.max(0, seatOf(passenger));
        float[] o = type().geo().seats()[Math.min(s, type().geo().seats().length - 1)];
        return toWorld(o[0], 0.1, o[2] >= 0 ? d[1] / 2 + 0.7 : -d[1] / 2 - 0.7);
    }

    /** Repère du véhicule (avant, haut, droite) vers le monde (sans inclinaison). */
    public Vec3 toWorld(double fwd, double up, double right) {
        double rad = Math.toRadians(getYRot());
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        double rx = -fz, rz = fx;
        return new Vec3(getX() + fx * fwd + rx * right, getY() + up, getZ() + fz * fwd + rz * right);
    }

    // ------------------------------------------------------------------ boucle principale
    @Override
    public void tick() {
        super.tick();
        wheelRotO = wheelRot;
        rotorAngleO = rotorAngle;
        radarAngleO = radarAngle;
        tiltO = tilt;
        rollO = roll;
        turretO = turret(1);
        pitchO = gunPitch(1);
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            if (type().heli()) {
                fly();
            } else {
                drive();
            }
        } else if (!level().isClientSide) {
            double rad = Math.toRadians(getYRot());
            Vec3 moved = position().subtract(xo, yo, zo);
            entityData.set(SPEED, (float) (moved.x * -Math.sin(rad) + moved.z * Math.cos(rad)));
        } else if (lerpSteps > 0) {
            double k = 1.0 / lerpSteps;
            setPos(getX() + (lerpX - getX()) * k, getY() + (lerpY - getY()) * k, getZ() + (lerpZ - getZ()) * k);
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYaw - getYRot()) * (float) k);
            lerpSteps--;
        }
        wheelRot += speed() * 1.8f;
        rotorAngle += rotor() * 47f;
        radarAngle += type().has(MilType.Flag.STRATEGIC) && !is(DEPLOYED) ? 0 : 6f;
        aimTurret();
        positionParts();
        if (!level().isClientSide) {
            serverTick();
        } else {
            clientFx();
        }
    }

    private void positionParts() {
        if (parts.length == 0) {
            return;
        }
        float[] d = dims(type());
        double half = (d[0] - d[1]) / 2;
        for (int i = 0; i < parts.length; i++) {
            double f = parts.length == 1 ? 0 : -half + 2 * half * i / (parts.length - 1);
            Vec3 p = toWorld(f, 0, 0);
            MilPart part = parts[i];
            part.xo = part.getX();
            part.yo = part.getY();
            part.zo = part.getZ();
            part.setPos(p.x, p.y, p.z);
        }
    }

    // ------------------------------------------------------------------ conduite (roues, chenilles, eau)
    private float[] input() {
        Entity c = getControllingPassenger();
        if (c instanceof Player p) {
            return new float[]{p.zza, p.xxa};
        }
        if (c instanceof HumanBotEntity bot && !level().isClientSide) {
            return MilAI.drive(this, bot);
        }
        return new float[]{0, 0};
    }

    private boolean canRun() {
        return fuel() > 0 && !is(ENGINE_DOWN) && getControllingPassenger() != null;
    }

    private void drive() {
        MilType t = type();
        float[] in = canRun() ? input() : new float[]{0, 0};
        float forward = in[0], side = in[1];
        boolean water = isInWater();
        boolean deep = water && getFluidHeight(net.minecraft.tags.FluidTags.WATER) > dims(t)[2] * 0.45;
        float max = t.maxSpeed() * (is(MOBILITY_DOWN) ? 0.3f : 1f);
        if (deep) {
            max = t.has(MilType.Flag.AMPHIBIOUS) ? 0.28f : 0;          // hydrojet : ~10 km/h ; sinon noyé
        }
        if (t.has(MilType.Flag.STRATEGIC) && is(DEPLOYED)) {
            max = 0;                                                     // béquilles sorties
        }
        if (!t.has(MilType.Flag.SNOW) && t.tracked() && level().getBlockState(blockPosition()).is(Blocks.SNOW)) {
            max *= 0.85f;
        }
        float accel = t.maxSpeed() / (t.tracked() ? 55f : 38f);
        float reverse = t.has(MilType.Flag.FAST_REVERSE) ? 0.75f : 0.35f;
        if (forward > 0) {
            speed = Math.min(max, speed + accel);
        } else if (forward < 0) {
            speed = speed > 0 ? Math.max(0, speed - accel * 3) : Math.max(-max * reverse, speed - accel * 0.7f);
        } else {
            speed *= onGround() || deep ? 0.95f : 0.99f;
            if (Math.abs(speed) < 0.003f) {
                speed = 0;
            }
        }
        if (Math.abs(speed) > max && max >= 0) {
            speed *= 0.9f;
        }
        steer = Mth.lerp(0.35f, steer, -side);
        if (t.tracked()) {
            // chenilles : pivot sur place, virage plus large à pleine vitesse
            float rate = 2.6f - 1.2f * Math.min(1, Math.abs(speed) / Math.max(0.01f, t.maxSpeed()));
            setYRot(getYRot() + steer * rate * (speed < -0.01f ? -1 : 1));
        } else {
            float grip = Math.min(1, Math.abs(speed) / 0.12f);
            float turn = dims(t)[0] > 6.5f ? 2.3f : 3.2f;
            setYRot(getYRot() + steer * turn * grip * Math.signum(speed == 0 ? 1 : speed));
        }
        double rad = Math.toRadians(getYRot());
        Vec3 dir = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 dm = getDeltaMovement();
        double vy = onGround() ? -0.04 : Math.max(-1.5, dm.y - 0.08);
        if (water && t.has(MilType.Flag.AMPHIBIOUS)) {
            vy = deep ? Math.min(vy + 0.12, 0.06) : vy;                 // flotte
        } else if (water) {
            vy = Math.max(vy, -0.15);
        }
        setDeltaMovement(dir.x * speed, vy, dir.z * speed);
        Vec3 before = position();
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision && Math.abs(speed) > 0.25f) {
            speed *= t.tracked() ? 0.5f : -0.3f;
        }
        stuckTicks = before.distanceToSqr(position()) < 0.0004 && forward != 0 ? stuckTicks + 1 : 0;
        entityData.set(SPEED, speed);
        entityData.set(STEER, steer);
    }

    // ------------------------------------------------------------------ vol (hélicoptères)
    private void fly() {
        MilType t = type();
        boolean run = canRun();
        float r = rotor();
        r = run ? Math.min(1, r + 0.008f) : Math.max(0, r - 0.004f);
        entityData.set(ROTOR, r);
        float[] in = run ? input() : new float[]{0, 0};
        Entity c = getControllingPassenger();
        int lift = c instanceof Player && level().isClientSide ? heliLift.getAsInt() : c instanceof HumanBotEntity ? MilAI.lift(this) : 0;
        boolean flying = r > 0.8f;
        Vec3 dm = getDeltaMovement();
        double vy = dm.y;
        if (flying) {
            vy = Mth.lerp(0.08, vy, lift * 0.32 + (onGround() ? 0 : 0.0));
        } else {
            vy = Math.max(-0.7, vy - 0.04 * (1 - r));                    // autorotation : chute freinée
            if (r > 0.3f) {
                vy = Math.max(vy, -0.35);
            }
        }
        float max = t.maxSpeed();
        boolean grounded = onGround() && lift <= 0;
        float target = grounded || !flying ? 0 : in[0] > 0 ? max : in[0] < 0 ? -max * 0.35f : 0;
        speed = Mth.lerp(grounded ? 0.3f : 0.06f, speed, target);
        if (!grounded && flying) {
            setYRot(getYRot() - in[1] * 2.8f);
        }
        tilt = Mth.lerp(0.1f, tilt, -speed / Math.max(0.01f, max) * 12f);
        roll = Mth.lerp(0.1f, roll, grounded ? 0 : in[1] * 10f);
        double rad = Math.toRadians(getYRot());
        setDeltaMovement(-Math.sin(rad) * speed, vy, Math.cos(rad) * speed);
        double fall = getDeltaMovement().y;
        move(MoverType.SELF, getDeltaMovement());
        if (onGround() && fall < -0.55 && !level().isClientSide) {
            hurtInternal((float) (-fall * 120), null, position().add(0, -1, 0), false);   // atterrissage brutal
        }
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0, 1));
        }
        entityData.set(SPEED, speed);
    }

    @Override
    public boolean isControlledByLocalInstance() {
        Entity c = getControllingPassenger();
        if (c instanceof Player p) {
            return p.isLocalPlayer();
        }
        return !level().isClientSide;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYaw = yaw;
        lerpSteps = 10;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------------ tourelle
    /** Lacet de la tourelle par rapport à la caisse (degrés). */
    public float turret(float partial) {
        return isLocalGunner() ? turretLocal : entityData.get(TURRET);
    }

    public float gunPitch(float partial) {
        return isLocalGunner() ? pitchLocal : entityData.get(PITCH);
    }

    private boolean isLocalGunner() {
        if (!level().isClientSide) {
            return false;
        }
        Entity g = gunner();
        return g instanceof Player p && p.isLocalPlayer();
    }

    /** Servant de la tourelle (place de tireur, ou pilote de l'Apache pour personne). */
    public Entity gunner() {
        List<MilType.Seat> seats = type().seats;
        for (int i = 0; i < seats.size(); i++) {
            MilType.Seat s = seats.get(i);
            if ((s.role() == MilType.Role.GUNNER || s.role() == MilType.Role.OPERATOR) && s.primary() >= 0
                    && !type().weapons.get(s.primary()).free()) {
                return occupant(i);
            }
        }
        return null;
    }

    private MilType.Weapon turretWeapon() {
        for (MilType.Seat s : type().seats) {
            if (s.role() == MilType.Role.GUNNER && s.primary() >= 0 && !type().weapons.get(s.primary()).free()) {
                return type().weapons.get(s.primary());
            }
        }
        return null;
    }

    private void aimTurret() {
        MilType.Weapon w = turretWeapon();
        if (w == null) {
            return;
        }
        float yaw = level().isClientSide && isLocalGunner() ? turretLocal : entityData.get(TURRET);
        float pitch = level().isClientSide && isLocalGunner() ? pitchLocal : entityData.get(PITCH);
        Entity g = gunner();
        float[] want = null;
        if (g instanceof HumanBotEntity bot && !level().isClientSide) {
            want = MilAI.aim(this, bot, w);
        } else if (g instanceof Player p && (!level().isClientSide || p.isLocalPlayer())) {
            want = new float[]{Mth.wrapDegrees(p.getYRot() - getYRot()), -p.getXRot()};
            Entity air = radarTrack(p);
            if (air != null) {
                want = aimAt(air.position().add(0, air.getBbHeight() / 2, 0).add(air.getDeltaMovement().scale(distanceTo(air) / 5.0)));
            }
        }
        if (want == null) {
            return;
        }
        float tr = w.traverse() <= 0 ? 360 : w.traverse();
        yaw += Mth.clamp(Mth.wrapDegrees(want[0] - yaw), -tr, tr);
        yaw = Mth.wrapDegrees(yaw);
        float elev = Mth.clamp(want[1], w.minPitch(), w.maxPitch());
        pitch += Mth.clamp(elev - pitch, -tr * 0.6f, tr * 0.6f);
        if (level().isClientSide) {
            turretLocal = yaw;
            pitchLocal = pitch;
        } else {
            entityData.set(TURRET, yaw);
            entityData.set(PITCH, pitch);
        }
    }

    /** Lacet / élévation (relatifs à la caisse) pour viser un point du monde depuis le pivot du canon. */
    public float[] aimAt(Vec3 target) {
        var geo = type().geo();
        Vec3 pivot = geo.gun() != null ? toWorld(geo.gun().x, geo.gun().y, geo.gun().z) : position().add(0, 1.5, 0);
        Vec3 d = target.subtract(pivot);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90 - getYRot();
        float pitch = (float) (Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        return new float[]{Mth.wrapDegrees(yaw), pitch};
    }

    /** Radar de poursuite (Shilka) : clic droit maintenu, la tourelle suit le drone / l'hélicoptère le plus proche. */
    private Entity radarTrack(Player p) {
        if (!type().has(MilType.Flag.RADAR) || !TriggerHandler.isRightHeld(p)) {
            return null;
        }
        Entity best = null;
        double bestD = 350 * 350;
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(350), MilVehicleEntity::isAircraft)) {
            double d = e.distanceToSqr(this);
            if (d < bestD && !hasPassenger(e)) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** Cible aérienne (radars, missiles sol-air) : hélicoptères, drones, missiles et roquettes en vol. */
    public static boolean isAircraft(Entity e) {
        return e instanceof MilVehicleEntity v && v.type().heli() && !v.onGround()
                || e instanceof fr.modeminecraft.entity.drone.DroneEntity
                || e instanceof ShellEntity s && s.shell().rocket()
                || e instanceof fr.modeminecraft.entity.AirdropEntity;
    }

    /**
     * Position d'une bouche de tir dans le monde, et direction du tir : canon (tourelle + élévation), tourelle (lacet),
     * caisse (dans l'axe du véhicule, élévation du regard du tireur).
     */
    public Vec3[] muzzle(MilType.Weapon w, Entity shooter, int alt) {
        var geo = type().geo();
        float[] m = geo.muzzles()[Math.min(w.muzzle() + alt, geo.muzzles().length - 1)];
        int mount = (int) m[3];
        double x = m[0], y = m[1], z = m[2];
        float yaw = turret(1), pitch = gunPitch(1);
        if (mount == 2 && geo.gun() != null) {
            // élévation autour du pivot du canon (axe latéral)
            double dx = x - geo.gun().x, dy = y - geo.gun().y;
            double c = Math.cos(Math.toRadians(pitch)), s = Math.sin(Math.toRadians(pitch));
            x = geo.gun().x + dx * c - dy * s;
            y = geo.gun().y + dx * s + dy * c;
        }
        if (mount >= 1 && geo.turret() != null) {
            double dx = x - geo.turret().x, dz = z - geo.turret().z;
            double c = Math.cos(Math.toRadians(yaw)), s = Math.sin(Math.toRadians(yaw));
            // lacet positif = vers la gauche (sens des angles de Minecraft)
            x = geo.turret().x + dx * c + dz * s;
            z = geo.turret().z - dx * s + dz * c;
        }
        Vec3 pos = toWorld(x, y, z);
        Vec3 dir;
        if (w.free()) {
            float py = shooter.getYRot(), pp = shooter.getXRot();
            pp = Mth.clamp(pp, -w.maxPitch(), -w.minPitch());
            dir = Vec3.directionFromRotation(pp, py);
        } else if (mount == 0) {
            float pp = shooter instanceof LivingEntity ? Mth.clamp(shooter.getXRot(), -w.maxPitch(), -w.minPitch()) : 0;
            dir = Vec3.directionFromRotation(pp, getYRot());
        } else {
            dir = Vec3.directionFromRotation(-pitch, getYRot() + yaw);
        }
        return new Vec3[]{pos, dir};
    }

    // ------------------------------------------------------------------ serveur
    private void serverTick() {
        MilType t = type();
        ServerLevel sl = (ServerLevel) level();
        long now = sl.getGameTime();
        // patrouille sans joueur à proximité : disparaît avec son équipage
        if (patrol && tickCount % 200 == 0 && sl.getNearestPlayer(this, 192) == null) {
            for (Entity p : List.copyOf(getPassengers())) {
                p.discard();
            }
            discard();
            return;
        }
        // carburant (moteur qui tourne)
        if (canRun()) {
            float load = 0.15f + Math.abs(speed()) / Math.max(0.01f, t.maxSpeed());
            float minutes = t.heli() ? 14 : 22;
            setFuel(fuel() - load / (minutes * 1200f));
            if (fuel() <= 0 && getControllingPassenger() instanceof Player p) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.mil.no_fuel").withStyle(ChatFormatting.RED), true);
            }
        }
        if (t.heli() && !isControlledByLocalInstance()) {
            // régime du rotor calculé aussi par le serveur (le pilote joueur le calcule de son côté)
            entityData.set(ROTOR, canRun() ? Math.min(1, rotor() + 0.008f) : Math.max(0, rotor() - 0.004f));
        }
        if (t.heli() && getControllingPassenger() == null && !onGround()) {
            // plus de pilote : chute
            entityData.set(ROTOR, Math.max(0, rotor() - 0.004f));
            setDeltaMovement(getDeltaMovement().multiply(0.98, 1, 0.98).add(0, -0.04, 0));
            move(MoverType.SELF, getDeltaMovement());
        }
        // incendie moteur
        if (is(BURNING)) {
            if (--burnTicks <= 0) {
                setState(BURNING, false);
            } else if (tickCount % 20 == 0) {
                hurtInternal(2, null, null, false);
                for (Entity p : getPassengers()) {
                    MilType.Seat s = seat(p);
                    if (s != null && !s.enclosed()) {
                        p.igniteForSeconds(3);
                    }
                }
            }
        }
        // fumigènes / protection active : rechargement
        if (smoke < 3 && ++smokeTimer > 600) {
            smoke++;
            smokeTimer = 0;
            infoDirty = true;
        }
        if (t.has(MilType.Flag.APS)) {
            if (aps < 4 && ++apsTimer > 1200) {
                aps++;
                apsTimer = 0;
                infoDirty = true;
            }
            if (tickCount % 2 == 0) {
                activeProtection(sl);
            }
        }
        // armes
        for (Entity p : getPassengers()) {
            MilType.Seat s = seat(p);
            if (s == null) {
                continue;
            }
            boolean left, right;
            if (p instanceof Player pl) {
                left = TriggerHandler.isTriggerDown(pl);
                right = TriggerHandler.isRightHeld(pl);
            } else if (p instanceof HumanBotEntity bot) {
                if (s.role() == MilType.Role.PASSENGER) {
                    MilAI.passengerFire(this, bot);                             // tir par la fenêtre, à l'arrêt
                    continue;
                }
                left = MilAI.wantsFire(this, bot, s);
                right = false;
            } else {
                continue;
            }
            if (s.primary() >= 0) {
                MilType.Weapon w = t.weapons.get(s.primary());
                if (w.kind() == MilType.Kind.SAM) {
                    updateLock(p, w);
                }
                if (left) {
                    fire(s.primary(), p, now);
                }
            }
            if (s.secondary() >= 0 && right && !(t.has(MilType.Flag.RADAR))) {
                fire(s.secondary(), p, now);
            }
        }
        for (int i = 0; i < heat.length; i++) {
            if (heat[i] > 0 && now - lastShot[i] > 4) {
                heat[i] = Math.max(0, heat[i] - 2);
                if (heat[i] % 10 == 0) {
                    infoDirty = true;
                }
            }
        }
        // écrasement
        if (crusher() && (Math.abs(speed()) > 0.04f || getControllingPassenger() instanceof Player pl && pl.zza != 0)) {
            crush(sl);
        }
        // l'eau noie les moteurs non amphibies
        if (!t.heli() && !t.has(MilType.Flag.AMPHIBIOUS) && getFluidHeight(net.minecraft.tags.FluidTags.WATER) > dims(t)[2] * 0.5) {
            if (!is(ENGINE_DOWN)) {
                setState(ENGINE_DOWN, true);
            }
        }
        if (health() < t.health * 0.3f && tickCount % 4 == 0) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + dims(t)[2], getZ(), 1, 0.3, 0.1, 0.3, 0.01);
        }
        if (infoDirty && tickCount % 2 == 0) {
            syncInfo();
        }
    }

    private boolean crusher() {
        return type().tracked() || type().has(MilType.Flag.HEAVY) || type().health >= 200 && !type().heli();
    }

    /** Écrase ce qui est devant : arbres (tronc entier), feuillages, clôtures, vitres, plantes, voitures, piétons. */
    private void crush(ServerLevel sl) {
        float[] d = dims(type());
        double reach = d[0] / 2 + 0.8 + Math.abs(speed()) * 4;
        int sign = speed() < -0.01f ? -1 : 1;
        Vec3 front = toWorld(sign * (reach - 0.6), 0, 0);
        AABB box = new AABB(front.x - d[1] / 2, getY() + 0.5, front.z - d[1] / 2, front.x + d[1] / 2, getY() + 2.6, front.z + d[1] / 2);
        int broken = 0;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            BlockState s = sl.getBlockState(p);
            if (s.isAir() || !crushable(s)) {
                continue;
            }
            if (s.is(BlockTags.LOGS)) {
                // le tronc entier tombe (le feuillage se décompose ensuite)
                BlockPos.MutableBlockPos up = p.mutable();
                for (int k = 0; k < 14 && sl.getBlockState(up).is(BlockTags.LOGS); k++, up.move(0, 1, 0)) {
                    sl.destroyBlock(up, false, this);
                }
            } else {
                sl.destroyBlock(p, false, this);
            }
            if (++broken > 12) {
                break;
            }
        }
        if (broken > 0 && tickCount % 4 == 0) {
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1f, 0.7f);
        }
        AABB hit = box.inflate(0.3).expandTowards(0, -0.5, 0);
        for (Entity e : sl.getEntities(this, hit, e -> !hasPassenger(e) && !(e instanceof MilPart))) {
            if (e instanceof VehicleEntity car) {
                car.hurt(damageSources().generic(), 25);                          // voiture écrasée
                e.push(-Math.sin(Math.toRadians(getYRot())) * 0.4 * sign, 0.2, Math.cos(Math.toRadians(getYRot())) * 0.4 * sign);
            } else if (e instanceof LivingEntity le && Math.abs(speed()) > 0.08f) {
                LivingEntity driver = getControllingPassenger();
                le.hurt(driver != null ? damageSources().mobAttack(driver) : damageSources().generic(), 8 + Math.abs(speed()) * 30);
                le.push(-Math.sin(Math.toRadians(getYRot())) * 0.6 * sign, 0.3, Math.cos(Math.toRadians(getYRot())) * 0.6 * sign);
            }
        }
    }

    private static boolean crushable(BlockState s) {
        return s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.is(BlockTags.FENCES) || s.is(BlockTags.FENCE_GATES)
                || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.CROPS) || s.is(BlockTags.SNOW)
                || s.is(Blocks.BAMBOO) || s.is(Blocks.CACTUS) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.VINE)
                || s.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock && s.getSoundType() == net.minecraft.world.level.block.SoundType.GLASS
                || s.getBlock() instanceof net.minecraft.world.level.block.TransparentBlock && s.getSoundType() == net.minecraft.world.level.block.SoundType.GLASS
                || s.getBlock() instanceof net.minecraft.world.level.block.StainedGlassBlock
                || s.canBeReplaced() && s.getFluidState().isEmpty();
    }

    /** Système de protection active Afganit : détruit en vol les roquettes et missiles qui arrivent. */
    private void activeProtection(ServerLevel sl) {
        if (aps <= 0) {
            return;
        }
        for (Entity e : sl.getEntities(this, getBoundingBox().inflate(16), e -> e instanceof fr.modeminecraft.entity.RocketEntity
                || e instanceof ShellEntity s && s.shell().rocket())) {
            if (e instanceof net.minecraft.world.entity.projectile.Projectile pr && pr.getOwner() != null && hasPassenger(pr.getOwner())) {
                continue;
            }
            Vec3 v = e.getDeltaMovement();
            Vec3 to = position().add(0, 1.2, 0).subtract(e.position());
            if (v.lengthSqr() < 0.01 || to.length() < 3 || v.normalize().dot(to.normalize()) < 0.8) {
                continue;
            }
            sl.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0);
            sl.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.2f, 1.6f);
            e.discard();
            aps--;
            infoDirty = true;
            break;
        }
    }

    // ------------------------------------------------------------------ tir
    public int ammo(MilAmmo a) {
        return stock.getOrDefault(a, 0);
    }

    public int capacity(MilAmmo a) {
        int cap = 0;
        for (MilType.Weapon w : type().weapons) {
            if (w.ammo().contains(a)) {
                cap += w.capacity();
            }
        }
        return cap;
    }

    public MilAmmo selectedAmmo(int weapon) {
        List<MilAmmo> list = type().weapons.get(weapon).ammo();
        return list.get(Math.floorMod(selected[weapon], list.size()));
    }

    /** Touche R : munition suivante de l'arme principale (ou secondaire si la principale n'en a qu'une). */
    public void cycleAmmo(Entity e) {
        MilType.Seat s = seat(e);
        if (s == null) {
            return;
        }
        int w = s.primary() >= 0 && type().weapons.get(s.primary()).ammo().size() > 1 ? s.primary() : s.secondary();
        if (w < 0 || type().weapons.get(w).ammo().size() < 2) {
            return;
        }
        selected[w] = (selected[w] + 1) % type().weapons.get(w).ammo().size();
        // le canon doit être rechargé avec la nouvelle munition
        if (type().weapons.get(w).kind() == MilType.Kind.CANNON) {
            lastShot[w] = (int) level().getGameTime();
        }
        infoDirty = true;
        if (e instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.mil.ammo",
                    Component.translatable("item.modeminecraft." + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(selectedAmmo(w).item()).getPath())), true);
        }
    }

    private void fire(int wi, Entity shooter, long now) {
        MilType.Weapon w = type().weapons.get(wi);
        if (now - lastShot[wi] < w.cooldown() || heat[wi] >= 100 && now - lastShot[wi] < 100) {
            return;
        }
        MilAmmo a = selectedAmmo(wi);
        if (ammo(a) <= 0) {
            // soute vide : on passe à une munition disponible
            for (int k = 1; k < w.ammo().size() && ammo(a) <= 0; k++) {
                selected[wi] = (selected[wi] + 1) % w.ammo().size();
                a = selectedAmmo(wi);
            }
            if (ammo(a) <= 0) {
                if (now - lastShot[wi] > 20) {
                    lastShot[wi] = (int) now;
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.NEUTRAL, 0.8f, 1.2f);
                }
                return;
            }
        }
        if (w.kind() == MilType.Kind.SAM && lockTarget < 0) {
            return;                                                           // pas de tir sans verrouillage
        }
        lastShot[wi] = (int) now;
        stock.put(a, ammo(a) - 1);
        if (w.kind() == MilType.Kind.MG || w.kind() == MilType.Kind.AUTOCANNON) {
            heat[wi] = Math.min(100, heat[wi] + (w.kind() == MilType.Kind.MG ? 2 : 3));
        }
        infoDirty = true;
        int alt = 0;
        if (w.kind() != MilType.Kind.MG && w.kind() != MilType.Kind.GRENADE && mirrored(w)) {
            alternate[wi] = !alternate[wi];
            alt = alternate[wi] ? 1 : 0;
        }
        Vec3[] md = muzzle(w, shooter, alt);
        Vec3 pos = md[0], dir = md[1];
        LivingEntity owner = shooter instanceof LivingEntity le ? le : null;
        ServerLevel sl = (ServerLevel) level();
        MilAmmo.Shell sh = a.shell;
        float spread = w.kind() == MilType.Kind.MG ? 0.012f : w.kind() == MilType.Kind.ROCKETS && w.cooldown() <= 10 ? 0.03f : 0.004f;
        dir = dir.add(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread).normalize();
        if (sh.bullet()) {
            var b = new fr.modeminecraft.entity.BulletEntity(sl, owner != null ? owner : getControllingPassenger(), sh.damage,
                    sh == MilAmmo.Shell.MG_145 ? 2 : 1, sh != MilAmmo.Shell.MG_762, fr.modeminecraft.item.gun.AmmoType.STANDARD);
            b.setPos(pos.x, pos.y, pos.z);
            b.setDeltaMovement(dir.scale(sh.speed));
            sl.addFreshEntity(b);
        } else {
            ShellEntity s = new ShellEntity(sl, owner, sh, this);
            s.setPos(pos.x, pos.y, pos.z);
            s.setDeltaMovement(dir.scale(sh.speed * (sh.guided && sh != MilAmmo.Shell.SAM ? 0.7 : 1)));
            if (sh == MilAmmo.Shell.ATGM || sh == MilAmmo.Shell.REFLEKS) {
                s.beamRider(shooter);                                       // suit le réticule du tireur (SACLOS)
            } else if (sh == MilAmmo.Shell.SAM) {
                s.home(sl.getEntity(lockTarget));
            } else if (sh == MilAmmo.Shell.ROCKET_227 || sh == MilAmmo.Shell.ATACMS) {
                s.gps(designated(shooter));
            }
            sl.addFreshEntity(s);
        }
        // effets : flamme, fumée, son, recul
        sl.sendParticles(sh.bullet() ? ParticleTypes.SMOKE : ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, sh.bullet() ? 2 : 8,
                0.1, 0.1, 0.1, 0.02);
        sl.sendParticles(ParticleTypes.FLAME, pos.x + dir.x * 0.3, pos.y + dir.y * 0.3, pos.z + dir.z * 0.3, sh.bullet() ? 1 : 5, 0.05, 0.05, 0.05, 0.01);
        var sound = switch (w.kind()) {
            case CANNON -> ModSounds.TANK_CANNON.get();
            case AUTOCANNON -> ModSounds.AUTOCANNON.get();
            case ROCKETS -> ModSounds.ROCKET_LAUNCH.get();
            case ATGM, SAM -> ModSounds.MISSILE_LAUNCH.get();
            case GRENADE -> ModSounds.AUTOCANNON.get();
            case MG -> sh == MilAmmo.Shell.MG_762 ? ModSounds.VEHICLE_MG_LIGHT.get() : ModSounds.VEHICLE_MG_HEAVY.get();
        };
        sl.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.NEUTRAL, w.kind() == MilType.Kind.CANNON ? 6f : 3f,
                0.95f + random.nextFloat() * 0.1f);
        if (w.kind() == MilType.Kind.CANNON) {
            fr.modeminecraft.entity.bot.CivilianLife.panic(sl, pos, 48, 200);
            if (type().heli()) {
                return;
            }
            setDeltaMovement(getDeltaMovement().add(dir.x * -0.05, 0, dir.z * -0.05));
        }
    }

    /** Bouches de tir jumelles (pod gauche / droit) : on tire alternativement de l'une et de l'autre. */
    private boolean mirrored(MilType.Weapon w) {
        float[][] m = type().geo().muzzles();
        if (w.muzzle() + 1 >= m.length) {
            return false;
        }
        float[] a = m[w.muzzle()], b = m[w.muzzle() + 1];
        return Math.abs(a[0] - b[0]) < 0.3f && Math.abs(a[2] + b[2]) < 0.05f && a[3] == b[3] && Math.abs(a[2]) > 0.05f;
    }

    /** Point visé par le tireur (roquettes guidées GPS) : marque du désignateur laser, sinon le réticule (600 blocs). */
    private Vec3 designated(Entity shooter) {
        Vec3 mark = fr.modeminecraft.item.military.LaserDesignatorItem.mark(shooter);
        if (mark != null) {
            return mark;
        }
        Vec3 eye = shooter.getEyePosition();
        Vec3 end = eye.add(shooter.getLookAngle().scale(600));
        var hit = level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    /** Missiles sol-air : verrouillage infrarouge (1,5 s dans un cône de 6°), bip continu une fois verrouillé. */
    private void updateLock(Entity shooter, MilType.Weapon w) {
        Vec3 eye = shooter.getEyePosition(), look = shooter.getLookAngle();
        Entity best = null;
        double bestDot = Math.cos(Math.toRadians(6));
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(350), MilVehicleEntity::isAircraft)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double dot = to.normalize().dot(look);
            if (dot > bestDot && !hasPassenger(e)) {
                bestDot = dot;
                best = e;
            }
        }
        if (best == null) {
            lockTicks = 0;
            lockCandidate = -1;
        } else if (best.getId() != lockCandidate) {
            lockCandidate = best.getId();
            lockTicks = 1;
        } else {
            lockTicks++;
        }
        lockTarget = lockTicks >= 30 ? lockCandidate : -1;
        boolean locked = lockTarget >= 0;
        setState(LOCKED, locked);
        setState(LOCKING, !locked && lockTicks > 0);
        if (tickCount % (locked ? 4 : 10) == 0 && lockTicks > 0 && shooter instanceof Player p) {
            level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.6f, locked ? 2f : 1.4f);
        }
    }

    /** Touche X : écran de fumée devant le véhicule (brise le verrouillage des missiles ennemis). */
    public void smoke() {
        if (!type().has(MilType.Flag.SMOKE) || smoke <= 0 || level().isClientSide) {
            return;
        }
        smoke--;
        infoDirty = true;
        float[] d = dims(type());
        for (int i = -1; i <= 1; i++) {
            double a = Math.toRadians(getYRot() + i * 30);
            Vec3 at = position().add(-Math.sin(a) * (d[0] / 2 + 5), 1.2, Math.cos(a) * (d[0] / 2 + 5));
            level().addFreshEntity(new fr.modeminecraft.entity.AreaCloudEntity(level(), fr.modeminecraft.entity.AreaCloudEntity.Type.SMOKE, at, 4f, 500,
                    getControllingPassenger()));
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 2f, 0.6f);
    }

    /** Déploiement des complexes lance-missiles (béquilles + relèvement des tubes). */
    public void toggleDeploy(Entity by) {
        if (!type().has(MilType.Flag.STRATEGIC) || Math.abs(speed()) > 0.02f) {
            return;
        }
        setState(DEPLOYED, !is(DEPLOYED));
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PISTON_EXTEND, SoundSource.NEUTRAL, 2f, 0.5f);
    }

    private void syncInfo() {
        infoDirty = false;
        CompoundTag t = new CompoundTag();
        for (var e : stock.entrySet()) {
            t.putInt(e.getKey().name(), e.getValue());
        }
        long now = level().getGameTime();
        for (int i = 0; i < selected.length; i++) {
            t.putInt("sel" + i, selected[i]);
            t.putInt("heat" + i, heat[i]);
            t.putInt("rl" + i, (int) Math.max(0, type().weapons.get(i).cooldown() - (now - lastShot[i])));
        }
        t.putInt("smoke", smoke);
        t.putInt("aps", aps);
        entityData.set(INFO, t);
    }

    /** Le tir d'un missile guidé de ce véhicule est brouillé (Shtora) ou détourné par la fumée. */
    public boolean shtora() {
        return type().has(MilType.Flag.SHTORA) && random.nextFloat() < 0.7f;
    }

    // ------------------------------------------------------------------ dégâts
    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || level().isClientSide || isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof Player p && p.getAbilities().instabuild && source.getDirectEntity() == p && p.isShiftKeyDown()) {
            discard();
            return true;
        }
        if (source.getEntity() != null && hasPassenger(source.getEntity()) && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            return false;                                                       // ses propres armes
        }
        Vec3 from = source.getSourcePosition() != null ? source.getSourcePosition()
                : source.getDirectEntity() != null ? source.getDirectEntity().position() : null;
        boolean explosion = source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
        boolean bullet = source.getDirectEntity() instanceof fr.modeminecraft.entity.BulletEntity;
        float k = 1;
        if (bullet) {
            // armes légères contre blindage : presque rien (les mitrailleuses lourdes entament les blindés légers)
            k = type().soft() ? 0.6f : amount >= 15 ? 0.3f : 0.05f;
        } else if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            k = 0.2f;
        }
        return hurtInternal(amount * k, source.getEntity(), from, explosion);
    }

    /** Impact d'un obus / missile du mod : perforation selon la munition. */
    public void shellHit(ShellEntity shell, Vec3 at) {
        MilAmmo.Shell s = shell.shell();
        float armor = armorAt(at);
        float k = (float) Math.pow(armor, 1 - s.pierce * 0.7f);
        float dmg = s.damage * k;
        hurtInternal(dmg / Math.max(0.05f, armor), shell.getOwner(), at, true, armor);
        modules(at, s.pierce >= 0.3f || s.blast >= 3);
    }

    private boolean hurtInternal(float amount, Entity by, Vec3 from, boolean heavy) {
        return hurtInternal(amount, by, from, heavy, from == null ? (type().armorFront + type().armorSide + type().armorRear) / 3 : armorAt(from));
    }

    private boolean hurtInternal(float amount, Entity by, Vec3 from, boolean heavy, float armor) {
        float hp = health() - amount * armor;
        entityData.set(HEALTH, hp);
        level().playSound(null, getX(), getY(), getZ(), heavy ? SoundEvents.ANVIL_LAND : SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL,
                heavy ? 1f : 0.5f, 0.6f + random.nextFloat() * 0.4f);
        if (heavy && from != null && amount * armor > 12) {
            modules(from, true);
        }
        if (hp <= 0) {
            destroy(by);
        }
        return true;
    }

    /** Facteur de blindage selon le côté touché (avant / flancs / arrière). */
    public float armorAt(Vec3 at) {
        MilType t = type();
        Vec3 to = at.subtract(position());
        double rad = Math.toRadians(getYRot());
        double fwd = to.x * -Math.sin(rad) + to.z * Math.cos(rad);
        double side = to.x * -Math.cos(rad) + to.z * -Math.sin(rad);
        double angle = Math.toDegrees(Math.atan2(Math.abs(side), fwd));
        return angle < 45 ? t.armorFront : angle > 135 ? t.armorRear : t.armorSide;
    }

    /** Modules : moteur (arrière, ou avant des camions) et chenilles / roues (bas des flancs). */
    private void modules(Vec3 at, boolean big) {
        if (!big || type().heli()) {
            return;
        }
        Vec3 to = at.subtract(position());
        double rad = Math.toRadians(getYRot());
        double fwd = to.x * -Math.sin(rad) + to.z * Math.cos(rad);
        double side = to.x * -Math.cos(rad) + to.z * -Math.sin(rad);
        double angle = Math.toDegrees(Math.atan2(Math.abs(side), fwd));
        double low = at.y - getY();
        boolean engineRear = !type().soft();
        if ((engineRear ? angle > 135 : angle < 45) && random.nextFloat() < 0.8f && !is(ENGINE_DOWN)) {
            setState(ENGINE_DOWN, true);
            setState(BURNING, true);
            burnTicks = 300;
            announce("message.modeminecraft.mil.engine_hit");
        } else if (angle >= 45 && angle <= 135 && low < 1.1 && !is(MOBILITY_DOWN)) {
            setState(MOBILITY_DOWN, true);
            announce(type().tracked() ? "message.modeminecraft.mil.track_hit" : "message.modeminecraft.mil.wheel_hit");
        }
    }

    private void announce(String key) {
        for (Entity p : getPassengers()) {
            if (p instanceof Player pl) {
                pl.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
            }
        }
    }

    /** Explosion de la mine sous le véhicule (coque en V du M-ATV : -80 %). */
    public float mineFactor() {
        return type().has(MilType.Flag.MINE_PROOF) ? 0.2f : 1f;
    }

    private void destroy(Entity by) {
        ServerLevel sl = (ServerLevel) level();
        float[] d = dims(type());
        for (Entity p : List.copyOf(getPassengers())) {
            p.stopRiding();
            p.hurt(by != null ? damageSources().explosion(this, by) : damageSources().generic(), 30);
        }
        sl.explode(this, getX(), getY() + d[2] / 2, getZ(), type().tracked() ? 4.5f : 3.5f, Level.ExplosionInteraction.NONE);
        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1, getZ(), 30, d[1] / 2, 0.5, d[1] / 2, 0.1);
        spawnAtLocation(new ItemStack(ModItems.STEEL_PLATE.get(), 2 + random.nextInt(4)));
        for (int i = 0; i < trunk.getContainerSize(); i++) {
            ItemStack s = trunk.getItem(i);
            if (!s.isEmpty()) {
                spawnAtLocation(s);
            }
        }
        discard();
    }

    // ------------------------------------------------------------------ interaction
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hasPassenger(player)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        MilType t = type();
        if (player.isSecondaryUseActive()) {
            if (t.trunk > 0) {
                int rows = Math.max(1, Math.min(6, (t.trunk + 8) / 9));
                MenuType<ChestMenu> menu = switch (rows) {
                    case 1 -> MenuType.GENERIC_9x1;
                    case 2 -> MenuType.GENERIC_9x2;
                    case 3 -> MenuType.GENERIC_9x3;
                    case 4 -> MenuType.GENERIC_9x4;
                    case 5 -> MenuType.GENERIC_9x5;
                    default -> MenuType.GENERIC_9x6;
                };
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ChestMenu(menu, id, inv, trunk, rows),
                        Component.translatable("entity.modeminecraft." + t.id)));
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(ModItems.JERRYCAN.get())) {
            if (fuel() >= 0.99f) {
                return InteractionResult.PASS;
            }
            setFuel(fuel() + 0.25f);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 1f, 0.8f);
            player.displayClientMessage(Component.translatable("message.modeminecraft.mil.fuel", Math.round(fuel() * 100)), true);
            return InteractionResult.SUCCESS;
        }
        if (held.is(ModItems.WRENCH.get())) {
            return repair(player);
        }
        for (MilType.Weapon w : t.weapons) {
            for (MilAmmo a : w.ammo()) {
                if (held.is(a.item())) {
                    int room = capacity(a) - ammo(a);
                    int n = Math.min(held.getCount(), Math.max(0, (room + a.perItem - 1) / a.perItem));
                    if (n <= 0) {
                        player.displayClientMessage(Component.translatable("message.modeminecraft.mil.ammo_full"), true);
                        return InteractionResult.SUCCESS;
                    }
                    stock.put(a, Math.min(capacity(a), ammo(a) + n * a.perItem));
                    if (!player.getAbilities().instabuild) {
                        held.shrink(n);
                    }
                    infoDirty = true;
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1f, 0.8f);
                    player.displayClientMessage(Component.translatable("message.modeminecraft.mil.loaded", ammo(a), capacity(a)), true);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        if (getPassengers().size() < t.seats.size()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Clé à molette + plaques d'acier (ou de titane pour les chars lourds) : +15 % de blindage, modules réparés. */
    private InteractionResult repair(Player player) {
        MilType t = type();
        boolean heavy = t.has(MilType.Flag.HEAVY);
        var plate = heavy ? ModItems.TITANIUM_PLATE.get() : ModItems.STEEL_PLATE.get();
        int slot = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(plate) || s.is(ModItems.TITANIUM_PLATE.get())) {
                slot = i;
                break;
            }
        }
        if (health() >= t.health && !is(ENGINE_DOWN) && !is(MOBILITY_DOWN)) {
            return InteractionResult.PASS;
        }
        if (slot < 0 && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.mil.need_plate",
                    Component.translatable(plate.getDescriptionId())).withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS;
        }
        if (slot >= 0 && !player.getAbilities().instabuild) {
            player.getInventory().getItem(slot).shrink(1);
        }
        entityData.set(HEALTH, Math.min(t.health, health() + t.health * 0.15f));
        setState(ENGINE_DOWN, false);
        setState(MOBILITY_DOWN, false);
        setState(BURNING, false);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.8f, 1.1f);
        player.displayClientMessage(Component.translatable("message.modeminecraft.mil.repaired", Math.round(health() / t.health * 100)), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.MIL_VEHICLES.get(type()).get());
    }

    // ------------------------------------------------------------------ client
    private void clientFx() {
        MilType t = type();
        if (Math.abs(speed()) > 0.05 && tickCount % 6 == 0 && !t.heli()) {
            Vec3 ex = toWorld(-dims(t)[0] / 2 + 0.3, 1.2, dims(t)[1] / 2 - 0.4);
            level().addParticle(ParticleTypes.SMOKE, ex.x, ex.y, ex.z, 0, 0.03, 0);
        }
        if (is(BURNING) && tickCount % 2 == 0) {
            Vec3 ex = toWorld(t.soft() ? dims(t)[0] / 2 - 0.8 : -dims(t)[0] / 2 + 1, dims(t)[2] - 0.3, 0);
            level().addParticle(ParticleTypes.FLAME, ex.x + random.nextGaussian() * 0.4, ex.y, ex.z + random.nextGaussian() * 0.4, 0, 0.06, 0);
            level().addParticle(ParticleTypes.LARGE_SMOKE, ex.x, ex.y + 0.5, ex.z, 0, 0.08, 0);
        }
        if (t.heli() && rotor() > 0.5f && tickCount % 3 == 0) {
            int ground = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
            if (getY() - ground < 12) {
                for (int i = 0; i < 4; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(a) * 5, ground + 0.2, getZ() + Math.sin(a) * 5,
                            Math.cos(a) * 0.3, 0.02, Math.sin(a) * 0.3);
                }
            }
        }
    }

    // ------------------------------------------------------------------ sauvegarde
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        MilType t = MilType.byId(tag.getString("MilType"));
        entityData.set(TYPE, (t == null ? MilType.UAZ_469 : t).ordinal());
        entityData.set(HEALTH, tag.contains("Health") ? tag.getFloat("Health") : type().health);
        entityData.set(FUEL, tag.contains("Fuel") ? tag.getFloat("Fuel") : 0.6f);
        entityData.set(STATE, tag.getInt("State") & (ENGINE_DOWN | MOBILITY_DOWN | DEPLOYED));
        entityData.set(TURRET, tag.getFloat("Turret"));
        entityData.set(PITCH, tag.getFloat("Pitch"));
        patrol = tag.getBoolean("Patrol");
        CompoundTag ammo = tag.getCompound("Ammo");
        for (MilAmmo a : MilAmmo.values()) {
            if (ammo.contains(a.name())) {
                stock.put(a, ammo.getInt(a.name()));
            }
        }
        smoke = tag.contains("Smoke") ? tag.getInt("Smoke") : 3;
        aps = tag.contains("Aps") ? tag.getInt("Aps") : 4;
        if (tag.contains("Trunk")) {
            trunk.fromTag(tag.getList("Trunk", 10), registryAccess());
        }
        if (tag.contains("Dest")) {
            destination = BlockPos.of(tag.getLong("Dest"));
        }
        infoDirty = true;
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("MilType", type().id);
        tag.putFloat("Health", health());
        tag.putFloat("Fuel", fuel());
        tag.putInt("State", state());
        tag.putFloat("Turret", entityData.get(TURRET));
        tag.putFloat("Pitch", entityData.get(PITCH));
        tag.putBoolean("Patrol", patrol);
        CompoundTag ammo = new CompoundTag();
        stock.forEach((a, n) -> ammo.putInt(a.name(), n));
        tag.put("Ammo", ammo);
        tag.putInt("Smoke", smoke);
        tag.putInt("Aps", aps);
        ListTag items = trunk.createTag(registryAccess());
        tag.put("Trunk", items);
        if (destination != null) {
            tag.putLong("Dest", destination.asLong());
        }
    }

    /** Le type est fixé à la création (chaque modèle a son propre type d'entité, voir ModEntities). */
    public void initType(MilType t) {
        entityData.set(TYPE, t.ordinal());
        entityData.set(HEALTH, t.health);
        refreshDimensions();
    }
}

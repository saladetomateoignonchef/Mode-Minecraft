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
import net.minecraft.world.level.ChunkPos;
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
    /** Fin du relevage hydraulique (HIMARS, heure de jeu) et cible verrouillée (Stinger), synchronisées pour le client. */
    private static final EntityDataAccessor<Integer> RAISE_END = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LOCK_ID = SynchedEntityData.defineId(MilVehicleEntity.class, EntityDataSerializers.INT);
    public static final int RAISE_TICKS = 40;
    /** Dernières coordonnées de frappe saisies à la console (Iskander), conservées dans le véhicule. */
    public int lastTargetX, lastTargetZ;
    public boolean hasSavedTarget;

    /** Bits de STATE. */
    public static final int ENGINE_DOWN = 1, MOBILITY_DOWN = 2, DEPLOYED = 4, BURNING = 8, LOCKING = 16, LOCKED = 32;
    /** Aéronef en perdition (chute incontrôlable), rotor de queue détruit (vrille), épave en feu. */
    public static final int CRASHING = 64, TAIL_HIT = 128, WRECK = 256;
    /** B-29 : train rentré, trappes de soute ouvertes, bombe nucléaire armée. */
    public static final int GEAR_UP = 512, BAY_OPEN = 1024, NUKE_ARMED = 2048;
    /** Aviation : missile en approche (alarme), postcombustion, signature radar trahie (soute ouverte il y a < 3 s). */
    public static final int MISSILE_WARN = 4096, AFTERBURNER = 8192, STEALTH_BREAK = 16384;
    /** Soute à bombes (avions). */
    public final fr.modeminecraft.nuclear.BombBay bombBay = new fr.modeminecraft.nuclear.BombBay();
    /** Assiette de l'avion (degrés, positif = nez en l'air) et viseur de bombardement actif (client du pilote). */
    public float planePitch, spool;
    private boolean planeInit;
    private static final net.minecraft.server.level.TicketType<ChunkPos> FLIGHT_TICKET = net.minecraft.server.level.TicketType.create(
            "modeminecraft_flight", java.util.Comparator.comparingLong(ChunkPos::toLong), 100);
    private ChunkPos flightTicket;
    public static boolean bombSightActive;
    private int bombCooldown;
    private double lastVy;
    /** Station d'armes du pilote (indice dans MilType.stations()) ; leurres ; fin de la dernière salve de leurres. */
    private int station;
    private int flares = 60;
    private long flareUntil, warnUntil, stealthBreakUntil;
    /** Réarmement au sol (ticks passés près d'une citerne de kérosène et d'une caisse de munitions d'aviation). */
    private int rearmTicks;
    /** Assiette déduite du mouvement (serveur) : direction des armes fixes de l'avion. */
    private float flightPitch;
    /** Pilote automatique de l'IA aérienne : cap, assiette (convention du joueur : négatif = cabrer) et gaz. */
    public float aiYaw, aiPitch, aiThrottle;
    /** Éjection / saut en parachute en cours : la descente du passager est autorisée. */
    private boolean letGo;
    /** C-130 : véhicule léger embarqué (NBT) et caisses de ravitaillement parachutables. */
    private CompoundTag cargo;
    private int supplyCrates = 4;
    /** Véhicule largué sous parachute (C-130) : chute freinée jusqu'au sol. */
    public boolean parachute;

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
    /** S-500 : sentinelle automatique ; S-400 des bases : défense aérienne automatique. */
    public boolean sentinel, autoDefense;
    /** Joueur qui commande la batterie (ses propres frappes ne sont pas interceptées). */
    public java.util.UUID ownerId;

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
        bombBay.configure(t.bayCapacity(), t.bayNukes());
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
            int n = w.ammo().size();
            for (int i = 0; i < n; i++) {
                MilAmmo a = w.ammo().get(i);
                // la première munition prend 60 % de la soute, les autres se partagent le reste
                float share = n == 1 ? 1f : i == 0 ? 0.6f : 0.4f / (n - 1);
                stock.put(a, Math.max(stock.getOrDefault(a, 0), Math.round(w.capacity() * fraction * share)));
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
            case B29_SUPERFORTRESS -> new float[]{20.5f, 3.2f, 4.4f};
            case IL_2 -> new float[]{11.4f, 2.0f, 3.2f};
            case LA_7 -> new float[]{8.6f, 1.8f, 2.8f};
            case MIG_15 -> new float[]{10.0f, 1.8f, 3.0f};
            case TU_95 -> new float[]{30.5f, 3.4f, 5.5f};
            case MIG_21 -> new float[]{13.4f, 1.8f, 3.2f};
            case MIG_25 -> new float[]{19.4f, 3.8f, 4.4f};
            case SU_25 -> new float[]{14.6f, 2.6f, 3.8f};
            case MIG_29 -> new float[]{16.6f, 3.2f, 3.6f};
            case SU_34 -> new float[]{20.6f, 3.8f, 4.4f};
            case SU_57 -> new float[]{19.0f, 4.0f, 3.8f};
            case P_51 -> new float[]{9.4f, 1.6f, 2.8f};
            case F_86 -> new float[]{10.8f, 1.8f, 3.2f};
            case F_4E -> new float[]{17.6f, 3.2f, 4.0f};
            case C_130 -> new float[]{28.5f, 4.2f, 6.2f};
            case A_10 -> new float[]{16.0f, 3.2f, 4.2f};
            case F_117 -> new float[]{14.6f, 4.0f, 2.8f};
            case F_15E -> new float[]{18.6f, 3.6f, 4.2f};
            case F_16C -> new float[]{14.6f, 2.0f, 3.8f};
            case B_2 -> new float[]{15.0f, 7.5f, 3.6f};
            case F_22 -> new float[]{17.8f, 4.0f, 3.8f};
            case S_400, ISKANDER -> new float[]{12.6f, 2.8f, 3.0f};
            case S_500 -> new float[]{13.6f, 2.9f, 3.1f};
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(TYPE, 0).define(HEALTH, 50f).define(FUEL, 0.6f).define(SPEED, 0f).define(STEER, 0f).define(TURRET, 0f).define(PITCH, 0f)
                .define(ROTOR, 0f).define(STATE, 0).define(SEATS, new CompoundTag()).define(INFO, new CompoundTag())
                .define(RAISE_END, 0).define(LOCK_ID, -1);
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
            return type().plane() ? new float[]{aiThrottle, 0} : MilAI.drive(this, bot);
        }
        return new float[]{0, 0};
    }

    /** Afterburner demandé par le pilote local (Ctrl), fourni par le code client. */
    public static java.util.function.BooleanSupplier afterburnerKey = () -> false;

    /** Cap et assiette visés par le pilote (regard du joueur : vol arcade guidé à la souris), ou consigne de l'IA. */
    private float[] pilotLook(Entity c) {
        if (c instanceof Player p) {
            return new float[]{p.getYRot(), p.getXRot()};
        }
        if (c instanceof HumanBotEntity) {
            return new float[]{aiYaw, aiPitch};
        }
        return null;
    }

    private boolean canRun() {
        return fuel() > 0 && !is(ENGINE_DOWN) && getControllingPassenger() != null;
    }

    private void drive() {
        if (parachute) {
            // véhicule largué par un C-130 : descente sous parachute
            Vec3 dm = getDeltaMovement();
            setDeltaMovement(dm.x * 0.97, Math.max(-0.3, dm.y - 0.04), dm.z * 0.97);
            move(MoverType.SELF, getDeltaMovement());
            if (onGround() || isInWater()) {
                parachute = false;
            }
            return;
        }
        MilType t = type();
        float[] in = canRun() ? input() : new float[]{0, 0};
        float forward = in[0], side = in[1];
        boolean water = isInWater();
        boolean deep = water && getFluidHeight(net.minecraft.tags.FluidTags.WATER) > dims(t)[2] * 0.45;
        float max = t.maxSpeed() * (is(MOBILITY_DOWN) ? 0.3f : 1f);
        if (deep) {
            max = t.has(MilType.Flag.AMPHIBIOUS) ? 0.28f : 0;          // hydrojet : ~10 km/h ; sinon noyé
        }
        if ((t.has(MilType.Flag.STRATEGIC) || raisable()) && is(DEPLOYED)) {
            max = 0;                                                     // béquilles sorties / lanceur relevé
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
        if (is(WRECK)) {
            Vec3 v = getDeltaMovement();
            setDeltaMovement(0, onGround() ? 0 : Math.max(-1.5, v.y - 0.08), 0);
            move(MoverType.SELF, getDeltaMovement());
            return;
        }
        if (is(CRASHING)) {
            crashFly();
            return;
        }
        if (type().plane()) {
            flyPlane();
            return;
        }
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

    // ------------------------------------------------------------------ avion à voilure fixe (B-29)
    /**
     * Vol d'avion : Z / S = gaz, Q / D = virage (avec inclinaison), la souris (regard vers le haut / le bas) commande
     * la profondeur. Il faut atteindre la vitesse de rotation sur une piste dégagée pour décoller ; sous la vitesse de
     * décrochage, l'avion s'enfonce nez en bas. Train rentré au sol = atterrissage sur le ventre.
     */
    private void flyPlane() {
        MilType t = type();
        boolean run = canRun();
        if (!planeInit) {
            // entité (re)créée en plein vol : on repart de l'état synchronisé (moteurs, vitesse)
            planeInit = true;
            boolean parked = onGround() || !level().noCollision(this, getBoundingBox().expandTowards(0, -2, 0));
            spool = Math.max(rotor(), parked ? 0 : 0.9f);
            speed = Math.max(speed, Math.abs(speed()));
            if (!parked) {
                speed = Math.max(speed, type().maxSpeed() * 0.7f);              // en vol : on garde la vitesse de croisière
            }
        }
        // régime des moteurs : calculé par celui qui fait voler l'avion (le serveur ne fait que l'afficher)
        spool = run ? Math.min(1, spool + 0.01f) : Math.max(0, spool - 0.005f);
        float r = spool;
        if (!level().isClientSide) {
            entityData.set(ROTOR, r);
        }
        Entity c = getControllingPassenger();
        if (c instanceof HumanBotEntity bot && !level().isClientSide) {
            AirAI.fly(this, bot);
        }
        float[] in = run ? input() : new float[]{0, 0};
        boolean ground = onGround();
        boolean ab = t.jet() && run && is(AFTERBURNER) && fuel() > 0;
        float cruise = t.maxSpeed(), max = cruise * (ab ? 1.25f : 1f), stall = cruise * 0.45f;
        float accel = cruise / (t.jet() ? (ab ? 80f : 140f) : 170f);
        if (in[0] > 0) {
            speed = Math.min(max * r, speed + accel);
        } else if (in[0] < 0) {
            speed = Math.max(0, speed - cruise / (ground ? 45f : 260f));
        } else if (!run) {
            speed = Math.max(0, speed - cruise / (ground ? 60f : 400f));
        } else if (ground) {
            speed = Math.max(0, speed - cruise / 600f);
        }
        if (speed > max && !ground) {
            speed = Math.max(max, speed - cruise / 300f);                         // postcombustion coupée : on revient à la croisière
        }
        boolean flying = speed >= stall;
        float[] look = pilotLook(c);
        boolean sight = level().isClientSide && bombSightActive;
        float rate = t.turnRate() * Mth.clamp(speed / Math.max(0.01f, stall * 1.6f), 0.35f, 1f) * (ab ? 0.85f : 1f);
        float pitchMax = t.heavy() ? 22 : 60, pitchRate = t.heavy() ? 1.2f : 2.6f;
        float want = 0, dyaw;
        if (ground) {
            // roulage : Q / D seulement ; rotation (cabrer) au-dessus de la vitesse de décollage
            dyaw = -in[1] * (speed < 0.05f ? 0 : 1.4f);
            if (look != null && flying && !sight) {
                want = Math.max(0, Mth.clamp(-look[1] * 0.8f, -pitchMax, pitchMax));
            }
        } else if (look == null || sight || in[1] != 0) {
            // Q / D (ou viseur de bombardement) : virage manuel ; la vue du pilote tourne avec l'avion
            dyaw = -in[1] * rate;
            if (look != null && !sight) {
                want = Mth.clamp(-look[1] * 0.8f, -pitchMax, pitchMax);
            }
            if (c instanceof Player p && dyaw != 0) {
                p.setYRot(p.getYRot() + dyaw);
                p.yRotO += dyaw;
            }
        } else {
            // vol arcade : l'avion vire vers l'endroit où regarde le pilote
            dyaw = Mth.clamp(Mth.wrapDegrees(look[0] - getYRot()), -rate, rate);
            want = Mth.clamp(-look[1], -pitchMax, pitchMax);
        }
        if (!flying && !ground) {
            want = -25;                                                            // décrochage : le nez tombe
        }
        planePitch = Mth.approach(planePitch, want, pitchRate);
        if (flying && !ground) {
            // énergie : la montée coûte de la vitesse, le piqué en donne
            speed = Mth.clamp(speed - (float) Math.sin(Math.toRadians(planePitch)) * cruise * 0.006f, 0, cruise * 1.45f);
        }
        setYRot(getYRot() + dyaw);
        roll = Mth.lerp(0.12f, roll, ground ? 0 : -dyaw / Math.max(0.1f, rate) * (t.heavy() ? 28f : 65f));
        tilt = planePitch;
        double rad = Math.toRadians(getYRot()), pr = Math.toRadians(planePitch);
        double horiz = speed * Math.cos(pr);
        double vy;
        if (flying) {
            vy = speed * Math.sin(pr);
            if (ground && vy < 0) {
                vy = 0;
            }
        } else {
            vy = ground ? 0 : Math.max(-1.4, getDeltaMovement().y - 0.06);
        }
        if (level().isClientSide && horiz > 1.2 && !level().isLoaded(BlockPos.containing(position().add(-Math.sin(rad) * horiz * 12, 0, Math.cos(rad) * horiz * 12)))) {
            // terrain devant pas encore reçu du serveur : on ralentit le temps qu'il arrive
            speed = Math.max(Math.max(1.2f, stall * 1.15f), speed - 0.15f);
            horiz = speed * Math.cos(pr);
        }
        if (getY() > level().getMaxBuildHeight() - 30) {
            vy = Math.min(vy, -0.05);                                              // plafond pratique : l'air est trop rare
            planePitch = Math.min(planePitch, 0);
        }
        setDeltaMovement(-Math.sin(rad) * horiz, vy, Math.cos(rad) * horiz);
        move(MoverType.SELF, getDeltaMovement());
        if (onGround() && is(GEAR_UP) && speed > 0.2f) {
            speed *= 0.92f;                                                         // le ventre racle la piste
        }
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0, 1));
        }
        entityData.set(SPEED, speed);
    }

    /** Vitesse de vol imposée (mise en vol directe, essais). */
    public void setAirspeed(float v) {
        speed = v;
        spool = 1;
    }

    /** Places qui commandent la soute : bombardier, ou pilote s'il est seul à bord. */
    public boolean canBomb(Entity e) {
        MilType t = type();
        if (!t.plane() || t.bayCapacity() <= 0 && t != MilType.C_130) {
            return false;
        }
        int s = seatOf(e);
        if (s >= 0 && s < t.seats.size() && t.seats.get(s).role() == MilType.Role.BOMBARDIER) {
            return true;
        }
        if (s != 0) {
            return false;
        }
        // pilote : seul à bord (mode solo), ou sans bombardier dans l'équipage
        for (int i = 1; i < t.seats.size(); i++) {
            if (t.seats.get(i).role() == MilType.Role.BOMBARDIER && occupant(i) != null) {
                return false;
            }
        }
        return true;
    }

    /** Soute interne (B-29, Tu-95) : les trappes doivent être ouvertes à la main (touche O) avant le largage. */
    private boolean manualBay() {
        return type() == MilType.B29_SUPERFORTRESS || type() == MilType.TU_95;
    }

    /** Touche G : sortir / rentrer le train (pilote). */
    public void toggleGear(Entity by) {
        if (!type().plane() || seatOf(by) != 0) {
            return;
        }
        if (!is(GEAR_UP) && onGround()) {
            return;                                                                // pas au sol !
        }
        setState(GEAR_UP, !is(GEAR_UP));
        level().playSound(null, getX(), getY(), getZ(), ModSounds.HIMARS_HYDRAULICS.get(), SoundSource.NEUTRAL, 1.5f, 1.3f);
        if (by instanceof Player p) {
            p.displayClientMessage(Component.translatable(is(GEAR_UP) ? "message.modeminecraft.b29.gear_up" : "message.modeminecraft.b29.gear_down")
                    .withStyle(ChatFormatting.AQUA), true);
        }
    }

    /** Touche O : trappes de soute (une bombe nucléaire doit d'abord être armée). */
    public void toggleBay(Entity by) {
        if (!canBomb(by) && seatOf(by) != 0) {
            return;
        }
        if (type() == MilType.C_130) {
            setState(BAY_OPEN, !is(BAY_OPEN));
            level().playSound(null, getX(), getY(), getZ(), ModSounds.HIMARS_HYDRAULICS.get(), SoundSource.NEUTRAL, 2f, 0.8f);
            if (is(BAY_OPEN) && cargo != null && !level().noCollision(this, getBoundingBox().move(0, -0.25, 0))) {
                unloadCargo(false);
            }
            return;
        }
        if (!is(BAY_OPEN) && bombBay.hasNuke() && !is(NUKE_ARMED)) {
            if (by instanceof Player p) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.b29.arm_first").withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        setState(BAY_OPEN, !is(BAY_OPEN));
        level().playSound(null, getX(), getY(), getZ(), ModSounds.BOMB_BAY_DOORS.get(), SoundSource.NEUTRAL, 2f, 1f);
    }

    /** Touche H : clé d'armement de la bombe nucléaire (pilote). */
    public void toggleArm(Entity by) {
        if (seatOf(by) != 0 || !(by instanceof Player p)) {
            return;
        }
        if (!bombBay.hasNuke()) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.b29.no_nuke").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        setState(NUKE_ARMED, !is(NUKE_ARMED));
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.NEUTRAL, 1f, is(NUKE_ARMED) ? 0.6f : 1.2f);
        p.displayClientMessage(Component.translatable(is(NUKE_ARMED) ? "message.modeminecraft.b29.armed" : "message.modeminecraft.b29.disarmed")
                .withStyle(is(NUKE_ARMED) ? ChatFormatting.RED : ChatFormatting.GREEN), true);
    }

    /** Clic gauche / Espace : largage de la bombe suivante (trappes ouvertes). */
    public void dropBomb(Entity by) {
        if (level().isClientSide || !canBomb(by) || bombCooldown > 0) {
            return;
        }
        if (type() == MilType.C_130) {
            c130Drop(by);
            return;
        }
        if (!is(BAY_OPEN) && manualBay()) {
            if (by instanceof Player p) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.b29.open_bay").withStyle(ChatFormatting.YELLOW), true);
            }
            bombCooldown = 10;
            return;
        }
        if (bombBay.hasNuke() && !is(NUKE_ARMED)) {
            return;
        }
        var kind = bombBay.take();
        if (kind == null) {
            if (by instanceof Player p) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.b29.empty").withStyle(ChatFormatting.GRAY), true);
            }
            bombCooldown = 10;
            return;
        }
        bombCooldown = kind == fr.modeminecraft.nuclear.BombItem.Kind.NUKE ? 40 : 6;
        if (kind == fr.modeminecraft.nuclear.BombItem.Kind.NUKE) {
            setState(NUKE_ARMED, false);
        }
        Vec3 bay = toWorld(0.8, 0.9, 0);
        Vec3 motion = position().subtract(xo, yo, zo);
        if (motion.lengthSqr() < 1e-4) {
            double rad = Math.toRadians(getYRot());
            motion = new Vec3(-Math.sin(rad) * speed(), 0, Math.cos(rad) * speed());
        }
        LivingEntity owner = by instanceof LivingEntity le ? le : null;
        var bomb = new fr.modeminecraft.nuclear.BombEntity(level(), kind, owner, this, bay, motion.add(0, -0.1, 0));
        if (kind == fr.modeminecraft.nuclear.BombItem.Kind.GUIDED) {
            Vec3 mark = fr.modeminecraft.item.military.LaserDesignatorItem.mark(by);
            Vec3 ap = aimPoint(by);
            bomb.guideTo(mark != null ? mark : ap != null && ap.y < getY() - 5 ? ap : null);
        }
        level().addFreshEntity(bomb);
        breakStealth();
        level().playSound(null, bay.x, bay.y, bay.z, ModSounds.BOMB_BAY_RELEASE.get(), SoundSource.NEUTRAL, 3f, 1f);
        infoDirty = true;
    }

    // ------------------------------------------------------------------ aviation : furtivité, leurres, éjection
    /** Surface équivalente radar actuelle (F-117 / B-2 : 0 soute fermée, 0,35 pendant 3 s après l'ouverture). */
    public float rcs() {
        MilType t = type();
        if (t.rcs() <= 0) {
            return is(BAY_OPEN) || is(STEALTH_BREAK) ? 0.35f : 0f;
        }
        return t.rcs();
    }

    private void breakStealth() {
        if (type().rcs() <= 0.25f) {
            stealthBreakUntil = level().getGameTime() + 60;
            setState(STEALTH_BREAK, true);
        }
    }

    /**
     * Un radar (S-400, S-500, Shilka, Avenger, défense de base, missile à guidage radar) voit-il cette cible aérienne ?
     * Les furtifs ne sont détectés qu'à courte portée (Su-57 150 blocs, F-22 90), le F-117 et le B-2 jamais soute fermée.
     */
    public static boolean radarSees(Entity radar, Entity e) {
        if (!isAircraft(e)) {
            return false;
        }
        MilVehicleEntity v = e instanceof MilVehicleEntity mv ? mv : null;
        if (v == null || !v.type().plane()) {
            return true;
        }
        float r = v.rcs();
        if (r <= 0) {
            return false;
        }
        if (r < 1) {
            double range = r >= 0.3f ? 260 : v.type().stealthRange();
            return radar.distanceToSqr(e) < range * range;
        }
        return true;
    }

    /** Leurres thermiques / paillettes (touche X) : 80 % des missiles guidés qui visent l'avion décrochent. */
    public void flares() {
        if (level().isClientSide || flares <= 0 || !type().plane() || level().getGameTime() < flareUntil - 20) {
            return;
        }
        flares--;
        flareUntil = level().getGameTime() + 30;
        infoDirty = true;
        ServerLevel sl = (ServerLevel) level();
        Vec3 back = toWorld(-dims(type())[0] / 2, dims(type())[2] * 0.4, 0);
        Vec3 m = position().subtract(xo, yo, zo);
        for (int i = 0; i < 6; i++) {
            double side = (i % 2 == 0 ? 1 : -1) * (1 + i * 0.4);
            Vec3 p = back.add(toWorld(0, 0, side).subtract(position()).scale(0.6));
            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 4, 0.2, 0.2, 0.2, 0.05);
            sl.sendParticles(ParticleTypes.FIREWORK, p.x, p.y - 0.5, p.z, 6, 0.4, 0.3, 0.4, 0.08);
            sl.sendParticles(ParticleTypes.SMOKE, p.x - m.x, p.y - 1, p.z - m.z, 4, 0.5, 0.5, 0.5, 0.02);
        }
        level().playSound(null, getX(), getY(), getZ(), ModSounds.FLARE_LAUNCH.get(), SoundSource.NEUTRAL, 3f, 1f);
    }

    /** Une salve de leurres est en l'air (les missiles qui visent l'avion peuvent être déviés). */
    public boolean flaring() {
        return level().getGameTime() < flareUntil;
    }

    public long flareStamp() {
        return flareUntil;
    }

    /** Un missile guidé verrouille cet avion : alarme dans le cockpit. */
    public void missileWarning() {
        warnUntil = level().getGameTime() + 12;
        if (!is(MISSILE_WARN)) {
            setState(MISSILE_WARN, true);
        }
    }

    /** Siège éjectable (C maintenue ou double appui) : verrière éjectée, propulsion verticale, parachute. */
    public void eject(Entity e) {
        if (level().isClientSide || !type().plane() || !hasPassenger(e) || onGround()) {
            return;
        }
        Vec3 m = position().subtract(xo, yo, zo);
        Vec3 at = toWorld(0, dims(type())[2] + 1.5, 0);
        letGo = true;
        e.stopRiding();
        letGo = false;
        e.teleportTo(at.x, at.y, at.z);
        e.setDeltaMovement(m.scale(0.4).add(0, 1.6, 0));
        e.hurtMarked = true;
        if (e instanceof LivingEntity le) {
            le.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 900, 0, false, false, true));
            le.getPersistentData().putLong("ModeMcParachute", level().getGameTime());
        }
        ServerLevel sl = (ServerLevel) level();
        sl.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y - 1, at.z, 2, 0.3, 0.3, 0.3, 0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y - 1, at.z, 20, 0.5, 0.5, 0.5, 0.05);
        level().playSound(null, at.x, at.y, at.z, ModSounds.EJECTION_SEAT.get(), SoundSource.NEUTRAL, 3f, 1f);
        if (e instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.air.ejected").withStyle(ChatFormatting.GOLD), true);
        }
        if (getControllingPassenger() == null) {
            startCrash(false);                                                     // l'avion vide part en vrille
        }
    }

    /** Descente autorisée : au sol, éjection, saut en parachute par la rampe du C-130, épave. */
    public boolean mayDismount(Entity e) {
        if (level().isClientSide || letGo || isRemoved() || !type().plane() || is(WRECK) || !e.isAlive() || !airborne()) {
            return true;
        }
        if (type() == MilType.C_130 && is(BAY_OPEN) && seatOf(e) >= 2) {
            // parachutistes : saut par la rampe arrière
            if (e instanceof LivingEntity le) {
                le.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 900, 0, false, false, true));
                le.getPersistentData().putLong("ModeMcParachute", level().getGameTime());
            }
            return true;
        }
        return false;
    }

    /** Station d'armes actuelle du pilote : indice d'arme, ou -1 = bombes. */
    public int currentStation() {
        List<Integer> st = type().stations();
        return st.isEmpty() ? -2 : st.get(Math.floorMod(station, st.size()));
    }

    /** Touche R (pilote) : station suivante (et munition suivante d'une station qui en a plusieurs). */
    private void cycleStation(Entity e) {
        List<Integer> st = type().stations();
        if (st.isEmpty()) {
            return;
        }
        int cur = currentStation();
        if (cur >= 0 && type().weapons.get(cur).ammo().size() > 1 && selected[cur] < type().weapons.get(cur).ammo().size() - 1) {
            selected[cur]++;
        } else {
            if (cur >= 0) {
                selected[cur] = 0;
            }
            station = Math.floorMod(station + 1, st.size());
        }
        lockTicks = 0;
        lockTarget = -1;
        infoDirty = true;
        if (e instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.air.station", stationName(currentStation())), true);
        }
    }

    public Component stationName(int st) {
        if (st == -1) {
            return Component.translatable("hud.modeminecraft.air.bombs");
        }
        if (st < 0) {
            return Component.literal("-");
        }
        MilType.Weapon w = type().weapons.get(st);
        Component n = Component.translatable("weapon.modeminecraft.mil." + w.key());
        if (w.ammo().size() > 1) {
            n = n.copy().append(" (").append(Component.translatable("ammo_short.modeminecraft." + selectedAmmo(st).name().toLowerCase())).append(")");
        }
        return n;
    }

    // ------------------------------------------------------------------ C-130 : rampe, parachutages, fret
    private void c130Drop(Entity by) {
        if (!is(BAY_OPEN)) {
            if (by instanceof Player p) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.air.ramp_closed").withStyle(ChatFormatting.YELLOW), true);
            }
            bombCooldown = 10;
            return;
        }
        bombCooldown = 30;
        boolean grounded = !level().noCollision(this, getBoundingBox().move(0, -0.25, 0));
        if (cargo != null) {
            unloadCargo(!grounded);
            return;
        }
        if (grounded) {
            return;
        }
        if (supplyCrates <= 0) {
            if (by instanceof Player p) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.air.no_crates").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        supplyCrates--;
        infoDirty = true;
        Vec3 at = toWorld(-dims(type())[0] / 2 - 2, 1, 0);
        level().addFreshEntity(new fr.modeminecraft.entity.AirdropEntity(level(), at.x, at.y, at.z));
        level().playSound(null, at.x, at.y, at.z, ModSounds.BOMB_BAY_RELEASE.get(), SoundSource.NEUTRAL, 2f, 0.8f);
    }

    /** Véhicule léger embarquable par la rampe du C-130 (4x4, blindés à roues légers). */
    private static boolean lightVehicle(MilVehicleEntity o) {
        MilType t = o.type();
        return t.chassis == MilType.Chassis.WHEELS && !t.has(MilType.Flag.STRATEGIC) && dims(t)[0] <= 6.5f;
    }

    private void loadCargo(ServerLevel sl) {
        Vec3 ramp = toWorld(-dims(type())[0] / 2 - 1.5, 0, 0);
        for (MilVehicleEntity o : sl.getEntitiesOfClass(MilVehicleEntity.class, new net.minecraft.world.phys.AABB(ramp, ramp).inflate(3.5, 2, 3.5))) {
            if (o == this || !lightVehicle(o) || Math.abs(o.speed()) > 0.25f || o.is(WRECK)) {
                continue;
            }
            List<Entity> crew = List.copyOf(o.getPassengers());
            for (Entity p : crew) {
                p.stopRiding();
            }
            CompoundTag tag = new CompoundTag();
            o.saveWithoutId(tag);
            cargo = tag;
            o.discard();
            for (Entity p : crew) {
                if (getPassengers().size() < type().seats.size()) {
                    p.startRiding(this, true);
                }
            }
            infoDirty = true;
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 2f, 0.6f);
            if (getControllingPassenger() instanceof Player pl) {
                pl.displayClientMessage(Component.translatable("message.modeminecraft.air.cargo_loaded",
                        Component.translatable("entity.modeminecraft." + o.type().id)), true);
            }
            return;
        }
    }

    /** Débarque le véhicule embarqué par la rampe (au sol), ou le largue sous parachute (en vol). */
    private void unloadCargo(boolean airdrop) {
        MilType t = MilType.byId(cargo.getString("MilType"));
        if (t == null) {
            cargo = null;
            return;
        }
        Vec3 at = toWorld(-dims(type())[0] / 2 - (airdrop ? 6 : 4), airdrop ? -2 : 0.2, 0);
        MilVehicleEntity o = create(level(), t, at.x, at.y, at.z, getYRot());
        o.load(cargo);
        o.setPos(at.x, at.y, at.z);
        o.setDeltaMovement(airdrop ? position().subtract(xo, yo, zo).scale(0.5) : Vec3.ZERO);
        o.setUUID(java.util.UUID.randomUUID());
        o.parachute = airdrop;
        level().addFreshEntity(o);
        cargo = null;
        infoDirty = true;
        level().playSound(null, at.x, at.y, at.z, SoundEvents.CHAIN_BREAK, SoundSource.NEUTRAL, 2f, 0.6f);
    }

    public boolean hasCargo() {
        return cargo != null;
    }

    // ------------------------------------------------------------------ crash des aéronefs
    private int wreckTicks, crashTicks;

    /** En vol (plus de 3 blocs au-dessus du sol) ? */
    private boolean airborne() {
        return !onGround() && !isInWater()
                && getY() - level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ()) > 3
                && level().noCollision(this, getBoundingBox().expandTowards(0, -3, 0));     // rien juste dessous (hangar, piste surélevée)
    }

    /**
     * Perte de contrôle (cahier des charges « Crashs aériens ») : pilote parti ou mort sans copilote, structure sous 20 %,
     * ou rotor de queue détruit (vrille). Les commandes ne répondent plus, le nez plonge, fumée noire et flammes,
     * alarme de cockpit, puis explosion au contact du sol, d'un mur ou de l'eau.
     */
    public void startCrash(boolean tail) {
        if (level().isClientSide || !type().heli() || is(WRECK)) {
            return;
        }
        if (tail && !is(TAIL_HIT)) {
            setState(TAIL_HIT, true);
            announce("message.modeminecraft.mil.tail_rotor_hit");
        }
        if (!is(CRASHING)) {
            setState(CRASHING, true);
            crashTicks = 0;
            announce("message.modeminecraft.mil.crashing");
            level().playSound(null, getX(), getY(), getZ(), ModSounds.AIRCRAFT_CRASH_ALARM.get(), SoundSource.NEUTRAL, 3f, 1f);
        }
    }

    /** Un passager « copilote » (place de tireur / pilote) reprend les commandes si le pilote saute ou meurt. */
    private boolean copilotTakesOver() {
        for (Entity p : getPassengers()) {
            MilType.Seat s = seat(p);
            if (s != null && (s.role() == MilType.Role.GUNNER && !type().plane() || s.role() == MilType.Role.PILOT
                    || s.role() == MilType.Role.BOMBARDIER) && seatOf(p) != 0
                    && (p instanceof Player || p instanceof HumanBotEntity)) {
                assignSeat(p, 0);
                if (p instanceof Player pl) {
                    pl.displayClientMessage(Component.translatable("message.modeminecraft.mil.copilot").withStyle(ChatFormatting.GOLD), true);
                }
                return true;
            }
        }
        return false;
    }

    private void crashFly() {
        crashTicks++;
        float sp = speed() * 0.985f + 0.004f;                                 // l'inertie l'emporte en avant
        if (is(TAIL_HIT)) {
            setYRot(getYRot() + 8);                                            // vrille : 8° par tick
        }
        double rad = Math.toRadians(getYRot());
        double vy = Math.max(-1.6, getDeltaMovement().y - 0.045);
        setDeltaMovement(-Math.sin(rad) * sp, vy, Math.cos(rad) * sp);
        move(MoverType.SELF, getDeltaMovement());
        entityData.set(SPEED, sp);
        entityData.set(ROTOR, Math.max(0.35f, rotor() - 0.003f));
        if (crashTicks > 3 && (onGround() || horizontalCollision || verticalCollision || isInWater())) {
            crashImpact();
        }
    }

    /** Écrasement : explosion (puissance 7, incendies), occupants tués, épave en feu pendant 30 s. */
    private void crashImpact() {
        if (!(level() instanceof ServerLevel sl) || is(WRECK)) {
            return;
        }
        destroyed = true;                                                     // l'épave ne subit plus de dégâts
        setState(CRASHING, false);
        setState(TAIL_HIT, false);
        setState(WRECK, true);
        setState(BURNING, true);
        entityData.set(HEALTH, 0f);
        entityData.set(ROTOR, 0f);
        entityData.set(SPEED, 0f);
        wreckTicks = 0;
        for (Entity p : List.copyOf(getPassengers())) {
            p.stopRiding();
            p.hurt(damageSources().explosion(this, null), 1000);
        }
        sl.explode(this, getX(), getY() + 1, getZ(), 7f, true, Level.ExplosionInteraction.TNT);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 2, 1, 1, 1, 0);
        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1, getZ(), 40, 2, 0.5, 2, 0.1);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.HUGE_EXPLOSION.get(), SoundSource.NEUTRAL, 6f, 1f);
        setDeltaMovement(Vec3.ZERO);
    }

    /** Épave en feu : flammes, fumée noire, brûle ce qui la touche, disparaît après 30 s en laissant de la ferraille. */
    private void wreckTick(ServerLevel sl) {
        float[] d = dims(type());
        if (tickCount % 3 == 0) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + d[2] * 0.5, getZ(), 6, d[0] * 0.3, d[2] * 0.3, d[0] * 0.3, 0.02);
            sl.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, getX(), getY() + d[2], getZ(), 2, d[0] * 0.2, 0.2, d[0] * 0.2, 0.01);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + d[2], getZ(), 3, d[0] * 0.3, 0.3, d[0] * 0.3, 0.03);
        }
        if (tickCount % 20 == 0) {
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.5))) {
                e.igniteForSeconds(4);
            }
        }
        if (++wreckTicks > 600) {
            spawnAtLocation(new ItemStack(ModItems.STEEL_PLATE.get(), 2 + random.nextInt(4)));
            discard();
        }
    }

    @Override
    public boolean isControlledByLocalInstance() {
        if (is(CRASHING) || is(WRECK)) {
            return !level().isClientSide;                                     // plus aucune commande : le serveur simule la chute
        }
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

    /** Servant de la tourelle (place de tireur ; conducteur seul à bord : le conducteur lui-même). */
    public Entity gunner() {
        List<MilType.Seat> seats = type().seats;
        for (int i = 0; i < seats.size(); i++) {
            MilType.Seat s = seats.get(i);
            if ((s.role() == MilType.Role.GUNNER || s.role() == MilType.Role.OPERATOR) && s.primary() >= 0
                    && !type().weapons.get(s.primary()).free()) {
                Entity o = occupant(i);
                if (o == null) {
                    Entity driver = occupant(0);
                    if (driver != null && soloGunnerSeat(driver) == i) {
                        return driver;
                    }
                }
                return o;
            }
        }
        return null;
    }

    /**
     * Mode solo (cahier des charges « Corrections chars » §1.3) : un joueur seul au poste de conduite d'un blindé dont
     * la place de tireur est libre conduit (ZQSD) ET sert la tourelle (souris, clic gauche canon, clic droit coaxiale,
     * R munitions). Dès qu'un tireur monte, les commandes de la tourelle lui reviennent.
     */
    private int soloGunnerSeat(Entity e) {
        if (!(e instanceof Player) || seatOf(e) != 0 || type().heli() || type().has(MilType.Flag.STRATEGIC)) {
            return -1;
        }
        List<MilType.Seat> seats = type().seats;
        for (int i = 1; i < seats.size(); i++) {
            MilType.Seat s = seats.get(i);
            if (s.role() == MilType.Role.GUNNER && s.primary() >= 0 && !type().weapons.get(s.primary()).free()) {
                return occupant(i) == null ? i : -1;
            }
        }
        return -1;
    }

    /** Conducteur seul qui sert aussi la tourelle ? */
    public boolean solo(Entity e) {
        return soloGunnerSeat(e) >= 0;
    }

    /** Place dont `e` sert les armes : la sienne, ou celle du tireur en mode solo. */
    public MilType.Seat weaponSeat(Entity e) {
        int g = soloGunnerSeat(e);
        return g >= 0 ? type().seats.get(g) : seat(e);
    }

    // ------------------------------------------------------------------ point visé (réticule)
    private Vec3 aimPoint;
    private long aimTime;
    private int aimBy = -1;

    /** Point visé par le réticule du joueur (lancer de rayon caméra jusqu'à 300 blocs, envoyé par le client). */
    public void setAim(Entity by, Vec3 p) {
        aimPoint = p;
        aimBy = by.getId();
        aimTime = level().getGameTime();
    }

    public Vec3 aimPoint(Entity by) {
        return aimPoint != null && by != null && aimBy == by.getId() && Math.abs(level().getGameTime() - aimTime) < 20 ? aimPoint : null;
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
            Vec3 ap = aimPoint(p);
            // le canon s'oriente exactement vers le point visé (pas de parallaxe entre le réticule et le tube)
            want = ap != null && ap.distanceToSqr(position()) > 36 ? aimAt(ap) : new float[]{Mth.wrapDegrees(p.getYRot() - getYRot()), -p.getXRot()};
            Entity air = radarTrack(p);
            if (air != null) {
                want = aimAt(air.position().add(0, air.getBbHeight() / 2, 0).add(air.getDeltaMovement().scale(distanceTo(air) / 5.0)));
            }
        }
        if (want == null) {
            return;
        }
        if (raisable() && !raised()) {
            // vérins : le module se relève (ou se recouche) dans l'axe, puis seulement le tireur le pointe
            want = new float[]{0, is(DEPLOYED) ? 30 * raise(0) : 0};
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
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(350), e2 -> radarSees(this, e2))) {
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
            // lacet positif = vers la droite (sens des angles de Minecraft, comme la direction du tir) ; +z = droite
            x = geo.turret().x + dx * c - dz * s;
            z = geo.turret().z + dx * s + dz * c;
        }
        Vec3 pos = toWorld(x, y, z);
        Vec3 dir;
        if (w.free()) {
            float py = shooter.getYRot(), pp = shooter.getXRot();
            pp = Mth.clamp(pp, -w.maxPitch(), -w.minPitch());
            dir = Vec3.directionFromRotation(pp, py);
        } else if (type().plane()) {
            // arme fixe d'avion : dans l'axe du fuselage (assiette de l'avion)
            float fp = level().isClientSide && isControlledByLocalInstance() ? planePitch : flightPitch;
            dir = Vec3.directionFromRotation(-fp, getYRot());
            double cp = Math.cos(Math.toRadians(fp)), sp = Math.sin(Math.toRadians(fp));
            double hy = dims(type())[2] * 0.45;
            pos = toWorld(x * cp - (y - hy) * sp, hy + x * sp + (y - hy) * cp, z);
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
        if (patrol && tickCount % 200 == 0 && sl.getNearestPlayer(this, t.plane() ? 640 : 192) == null) {
            for (Entity p : List.copyOf(getPassengers())) {
                p.discard();
            }
            discard();
            return;
        }
        // carburant (moteur qui tourne)
        if (canRun()) {
            float load = (0.15f + Math.abs(speed()) / Math.max(0.01f, t.maxSpeed())) * (is(AFTERBURNER) ? 3 : 1);
            float minutes = t.plane() ? 20 : t.heli() ? 14 : 22;
            setFuel(fuel() - load / (minutes * 1200f));
            if (fuel() <= 0 && getControllingPassenger() instanceof Player p) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.mil.no_fuel").withStyle(ChatFormatting.RED), true);
            }
        }
        if (t.heli() && !isControlledByLocalInstance()) {
            // régime du rotor calculé aussi par le serveur (le pilote joueur le calcule de son côté)
            entityData.set(ROTOR, canRun() ? Math.min(1, rotor() + 0.008f) : Math.max(0, rotor() - 0.004f));
        }
        if (t.heli() && is(WRECK)) {
            wreckTick(sl);
            return;
        }
        if (t.heli() && !is(CRASHING) && airborne()) {
            if (getControllingPassenger() == null && !copilotTakesOver()) {
                startCrash(false);                                               // pilote parti ou mort, sans copilote
            } else if (health() < t.health * 0.2f) {
                startCrash(false);                                               // structure trop endommagée
            }
        }
        if (t.heli() && is(CRASHING) && tickCount % 2 == 0) {
            float[] d = dims(t);
            Vec3 eng = toWorld(-d[0] * 0.15, d[2] * 0.8, 0);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, eng.x, eng.y, eng.z, 4, 0.4, 0.3, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, eng.x, eng.y, eng.z, 2, 0.3, 0.2, 0.3, 0.01);
            sl.sendParticles(ParticleTypes.FLAME, eng.x, eng.y, eng.z, 3, 0.3, 0.2, 0.3, 0.02);
            if (crashTicks % 40 == 0) {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.AIRCRAFT_CRASH_ALARM.get(), SoundSource.NEUTRAL, 3f, 1f);
            }
        }
        if (t.heli() && getControllingPassenger() == null && !onGround() && !is(CRASHING)) {
            // posé de travers / très bas sans pilote : il retombe simplement
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
        if (bombCooldown > 0) {
            bombCooldown--;
        }
        // aéronef posé près d'une citerne de kérosène (hangar à avions) : ravitaillement automatique
        if (t.heli() && tickCount % 40 == 0 && onGround() && Math.abs(speed()) < 0.05f && fuel() < 0.999f) {
            BlockPos c = blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-8, -1, -8), c.offset(8, 3, 8))) {
                if (sl.getBlockState(q).is(fr.modeminecraft.registry.ModBlocks.KEROSENE_TANK.get())) {
                    setFuel(Math.min(1f, fuel() + 0.05f));
                    if (getControllingPassenger() instanceof Player pl) {
                        pl.displayClientMessage(Component.translatable("message.modeminecraft.mil.refuel", Math.round(fuel() * 100)), true);
                    }
                    break;
                }
            }
        }
        if (t.plane() && tickCount % 10 == 0) {
            infoDirty = true;                                                       // contenu de la soute (rempli à la main)
        }
        if (t.plane()) {
            airTick(sl, now);
        }
        if (t.heli() && !onGround()) {
            // aéronef rapide : couloir de chunks chargés devant lui (sinon il entre dans du terrain pas encore généré,
            // le serveur cesse de le suivre et il « saute » ou disparaît sous son pilote)
            Vec3 motion = position().subtract(xo, yo, zo);
            ChunkPos here = chunkPosition();
            if (!here.equals(flightTicket) || tickCount % 40 == 0) {
                sl.getChunkSource().addRegionTicket(FLIGHT_TICKET, here, 3, here);
                flightTicket = here;
            }
            if (motion.lengthSqr() > 0.04 && tickCount % 8 == 0) {
                Vec3 dir = new Vec3(motion.x, 0, motion.z).normalize();
                int reach = (int) Math.min(10, Math.ceil(motion.length() * 40 / 16));        // ~2 s de vol
                ChunkPos last = here;
                for (int k = 1; k <= reach; k++) {
                    ChunkPos cp = new ChunkPos(BlockPos.containing(position().add(dir.scale(k * 16))));
                    if (!cp.equals(last)) {
                        sl.getChunkSource().addRegionTicket(FLIGHT_TICKET, cp, 1, cp);
                        last = cp;
                    }
                }
            }
        }
        if (t.plane() && !is(WRECK) && !is(CRASHING)) {
            // contrôles faits par le serveur (le pilote joueur fait voler l'avion de son côté)
            double vy = getY() - yo, hs = Math.hypot(getX() - xo, getZ() - zo);
            if (onGround() && lastVy < -0.5) {
                hurtInternal((float) (-lastVy * 160), null, position().add(0, -1, 0), false);       // atterrissage brutal
            }
            if (onGround() && is(GEAR_UP) && hs > 0.2) {
                hurtInternal(t.health * 0.02f, null, position().add(0, -1, 0), false);              // atterrissage sur le ventre
                sl.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 3, 2, 0.1, 2, 0);
            }
            if (horizontalCollision && hs > 0.6 && !onGround()) {
                startCrash(false);
                crashImpact();                                                     // contre une falaise / un immeuble
                return;
            }
            lastVy = vy;
        }
        // armes
        for (Entity p : getPassengers()) {
            if (t.plane() && (seatOf(p) == 0 || canBomb(p))) {
                planeWeapons(p, now);
                continue;
            }
            MilType.Seat s = weaponSeat(p);
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
        if (t.has(MilType.Flag.STRATEGIC)) {
            StrategicSystems.tick(this);
        }
        if (infoDirty && tickCount % 2 == 0) {
            syncInfo();
        }
    }

    /** Pilote (et bombardier) d'un avion : clic droit = canon de bord, clic gauche = station d'armes (R pour changer). */
    private void planeWeapons(Entity p, long now) {
        MilType t = type();
        boolean left, right;
        if (p instanceof Player pl) {
            left = TriggerHandler.isTriggerDown(pl);
            right = TriggerHandler.isRightHeld(pl);
        } else if (p instanceof HumanBotEntity) {
            left = AirAI.wantsMissile(this);
            right = AirAI.wantsGun(this);
        } else {
            return;
        }
        if (seatOf(p) != 0) {
            if (left && canBomb(p)) {
                dropBomb(p);                                                       // bombardier : largage
            }
            return;
        }
        int g = t.planeGun();
        int st = currentStation();
        if (right && g >= 0) {
            fire(g, p, now);
        }
        if (st >= 0) {
            MilType.Weapon w = t.weapons.get(st);
            if (p instanceof HumanBotEntity) {
                // verrouillage géré par AirAI
            } else if (w.kind() == MilType.Kind.AAM || w.kind() == MilType.Kind.AGM) {
                updateLock(p, w);
            } else if (is(LOCKED) || is(LOCKING)) {
                setState(LOCKED, false);
                setState(LOCKING, false);
                entityData.set(LOCK_ID, -1);
            }
            if (left) {
                fire(st, p, now);
            }
        } else if (st == -1 && left && canBomb(p)) {
            dropBomb(p);                                                           // tapis de bombes en maintenant le clic
        }
    }

    /** Aviation, côté serveur : assiette, alarmes, furtivité, réarmement au sol, fret du C-130. */
    private void airTick(ServerLevel sl, long now) {
        double dy = getY() - yo, hs = Math.hypot(getX() - xo, getZ() - zo);
        if (hs > 0.05) {
            flightPitch = (float) Math.toDegrees(Math.atan2(dy, hs));
        } else if (onGround()) {
            flightPitch = 0;
        }
        if (is(MISSILE_WARN) && now > warnUntil) {
            setState(MISSILE_WARN, false);
        }
        if (is(STEALTH_BREAK) && now > stealthBreakUntil) {
            setState(STEALTH_BREAK, false);
        }
        if (is(AFTERBURNER) && (fuel() <= 0 || !(getControllingPassenger() instanceof Player) && !(getControllingPassenger() instanceof HumanBotEntity))) {
            setState(AFTERBURNER, false);
        }
        boolean grounded = onGround() || !level().noCollision(this, getBoundingBox().move(0, -0.25, 0));
        if (type() == MilType.C_130 && grounded && is(BAY_OPEN) && cargo == null && tickCount % 10 == 0) {
            loadCargo(sl);
        }
        // réarmement : citerne de kérosène + caisse de munitions d'aviation à moins de 10 blocs, avion immobile au sol
        if (tickCount % 20 == 0) {
            boolean still = grounded && hs < 0.05;
            boolean fuelOk = false, ammoOk = false;
            if (still) {
                BlockPos c = blockPosition();
                for (BlockPos q : BlockPos.betweenClosed(c.offset(-14, -2, -14), c.offset(14, 4, 14))) {
                    var st = sl.getBlockState(q);
                    fuelOk |= st.is(fr.modeminecraft.registry.ModBlocks.KEROSENE_TANK.get());
                    ammoOk |= st.is(fr.modeminecraft.registry.ModBlocks.AVIATION_AMMO_CRATE.get());
                    if (fuelOk && ammoOk) {
                        break;
                    }
                }
            }
            if (fuelOk && ammoOk) {
                rearmTicks += 20;
                infoDirty = true;
                if (rearmTicks >= 200) {
                    rearmTicks = 0;
                    fillAmmo(1f);
                    flares = 60;
                    supplyCrates = 4;
                    setFuel(1f);
                    bombBay.refill(type().defaultBomb());
                    entityData.set(HEALTH, Math.min(type().health, health() + type().health * 0.25f));
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.NEUTRAL, 2f, 0.7f);
                    if (getControllingPassenger() instanceof Player pl) {
                        pl.displayClientMessage(Component.translatable("message.modeminecraft.air.rearmed").withStyle(ChatFormatting.GREEN), true);
                    }
                } else if (getControllingPassenger() instanceof Player pl) {
                    pl.displayClientMessage(Component.translatable("message.modeminecraft.air.rearming", rearmTicks / 20, 10), true);
                }
            } else if (rearmTicks > 0) {
                rearmTicks = 0;
                infoDirty = true;
            }
        }
    }

    /** IA aérienne : verrouillage d'une cible par l'autodirecteur de l'arme `wi` (1,5 s dans le cône du nez). */
    void aiLock(Entity t, int wi) {
        if (t == null) {
            lockTicks = 0;
            lockCandidate = -1;
            lockTarget = -1;
            setState(LOCKED, false);
            return;
        }
        List<Integer> st = type().stations();
        int k = st.indexOf(wi);
        if (k >= 0) {
            station = k;
        }
        if (t.getId() != lockCandidate) {
            lockCandidate = t.getId();
            lockTicks = 0;
        }
        lockTicks++;
        lockTarget = lockTicks >= 30 ? lockCandidate : -1;
        setState(LOCKED, lockTarget >= 0);
    }

    boolean aiLockReady() {
        return lockTarget >= 0;
    }

    /**
     * Véhicule qui porte un joueur : toujours simulé et suivi, comme le joueur lui-même. Sinon, un avion qui entre dans
     * une section de chunk dont les entités ne sont pas encore chargées cesse d'être suivi : il disparaît un instant sous
     * son pilote côté client (ou pour de bon si le serveur rame).
     */
    @Override
    public boolean isAlwaysTicking() {
        for (Entity p : getPassengers()) {
            if (p instanceof Player) {
                return true;
            }
        }
        return false;
    }

    /** Au sol (le serveur ne simule pas le mouvement d'un avion piloté par un joueur : on teste le contact). */
    public boolean groundedNow() {
        return onGround() || !level().noCollision(this, getBoundingBox().move(0, -0.25, 0));
    }

    /** Garde la zone de l'avion chargée (apparition d'un avion de l'IA hors de la zone simulée). */
    public void holdChunks() {
        if (level() instanceof ServerLevel sl) {
            ChunkPos cp = chunkPosition();
            sl.getChunkSource().addRegionTicket(FLIGHT_TICKET, cp, 3, cp);
        }
    }

    /** Train rentré (avions de l'IA mis en vol directement). */
    public void setGearUp() {
        setState(GEAR_UP, true);
    }

    /** Postcombustion (Ctrl), demandée par le pilote. */
    public void setAfterburner(Entity by, boolean on) {
        if (seatOf(by) == 0 && type().jet() && is(AFTERBURNER) != on) {
            setState(AFTERBURNER, on && fuel() > 0);
        }
    }

    public int flaresLeft() {
        return flares;
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

    /** Retire un coup de la soute (consoles de tir). */
    /** Missile rendu à la soute (tir annulé). */
    public void refund(MilAmmo a) {
        stock.put(a, ammo(a) + 1);
        infoDirty = true;
    }

    public boolean consume(MilAmmo a) {
        if (ammo(a) <= 0) {
            return false;
        }
        stock.put(a, ammo(a) - 1);
        infoDirty = true;
        return true;
    }

    /** Contenance des soutes pour ce type de munition (les munitions d'une même arme se partagent sa soute). */
    public int capacity(MilAmmo a) {
        int cap = 0;
        for (MilType.Weapon w : type().weapons) {
            if (w.ammo().contains(a)) {
                cap = Math.max(cap, w.capacity());
            }
        }
        return cap;
    }

    /** Place libre pour ce type de munition. */
    public int room(MilAmmo a) {
        int room = 0;
        for (MilType.Weapon w : type().weapons) {
            if (w.ammo().contains(a)) {
                int used = 0;
                for (MilAmmo b : w.ammo()) {
                    used += ammo(b);
                }
                room = Math.max(room, w.capacity() - used);
            }
        }
        return room;
    }

    public MilAmmo selectedAmmo(int weapon) {
        List<MilAmmo> list = type().weapons.get(weapon).ammo();
        return list.get(Math.floorMod(selected[weapon], list.size()));
    }

    /** Touche R : munition suivante de l'arme principale (ou secondaire si la principale n'en a qu'une). */
    public void cycleAmmo(Entity e) {
        if (type().plane() && seatOf(e) == 0) {
            cycleStation(e);
            return;
        }
        MilType.Seat s = weaponSeat(e);
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
        if (!raised()) {
            if (now - lastShot[wi] > 20 && shooter instanceof Player p) {
                lastShot[wi] = (int) now;
                p.displayClientMessage(Component.translatable("message.modeminecraft.mil.raise_first").withStyle(ChatFormatting.RED), true);
            }
            return;
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
        // convergence : le projectile part de la bouche, vers le point exact sous le réticule
        Vec3 ap = shooter instanceof Player ? aimPoint(shooter) : null;
        if (ap != null && ap.distanceToSqr(pos) > 9) {
            Vec3 target = ap;
            if (!sh.bullet() && !sh.rocket() && !sh.guided && sh.gravity > 0) {
                double t = ap.distanceTo(pos) / Math.max(0.1, sh.speed);
                target = target.add(0, sh.gravity * t * (t + 1) / 2, 0);              // compensation de la chute
            }
            Vec3 nd = target.subtract(pos).normalize();
            if (w.free() || nd.dot(dir) > Math.cos(Math.toRadians(12))) {
                dir = nd;
            }
        }
        float spread = w.kind() == MilType.Kind.MG ? 0.012f : w.kind() == MilType.Kind.ROCKETS && w.cooldown() <= 10 ? 0.03f : 0.004f;
        dir = dir.add(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread).normalize();
        Vec3 carrier = type().plane() ? position().subtract(xo, yo, zo) : Vec3.ZERO;
        if (sh.bullet()) {
            var b = new fr.modeminecraft.entity.BulletEntity(sl, owner != null ? owner : getControllingPassenger(), sh.damage,
                    sh == MilAmmo.Shell.MG_145 ? 2 : 1, sh != MilAmmo.Shell.MG_762, fr.modeminecraft.item.gun.AmmoType.STANDARD);
            b.setPos(pos.x, pos.y, pos.z);
            b.setDeltaMovement(dir.scale(sh.speed).add(carrier));
            sl.addFreshEntity(b);
        } else {
            ShellEntity s = new ShellEntity(sl, owner, sh, this);
            // missiles et roquettes : départ devant les tubes, hors de la boîte de collision du lanceur
            Vec3 out = sh.rocket() || sh.guided ? pos.add(dir.scale(1.2)) : pos;
            s.setPos(out.x, out.y, out.z);
            s.setDeltaMovement(dir.scale(sh.speed * (sh.guided && sh != MilAmmo.Shell.SAM ? 0.7 : 1)).add(carrier));
            if (sh == MilAmmo.Shell.ATGM || sh == MilAmmo.Shell.REFLEKS) {
                s.beamRider(shooter);                                       // suit le réticule du tireur (SACLOS)
            } else if (sh == MilAmmo.Shell.SAM) {
                s.home(lockTarget >= 0 ? sl.getEntity(lockTarget) : null);      // sans verrouillage : tir libre, vol droit
            } else if (sh == MilAmmo.Shell.ROCKET_227 || sh == MilAmmo.Shell.ATACMS) {
                s.gps(designated(shooter));
            } else if (sh == MilAmmo.Shell.AAM_IR || sh == MilAmmo.Shell.AAM_RADAR) {
                s.home(lockTarget >= 0 ? sl.getEntity(lockTarget) : null);      // sans verrouillage : vol droit
            } else if (sh == MilAmmo.Shell.AGM) {
                Entity locked = lockTarget >= 0 ? sl.getEntity(lockTarget) : null;
                if (locked != null) {
                    s.home(locked);
                } else {
                    Vec3 mark = fr.modeminecraft.item.military.LaserDesignatorItem.mark(shooter);
                    s.gps(mark != null ? mark : ap != null ? ap : designated(shooter));
                }
            } else if (sh == MilAmmo.Shell.ARM) {
                Entity radar = AirAI.nearestRadar(this, dir, 520);
                if (radar != null) {
                    s.home(radar);
                } else {
                    s.gps(ap != null ? ap : designated(shooter));
                }
            } else if (sh == MilAmmo.Shell.CRUISE) {
                Vec3 mark = fr.modeminecraft.item.military.LaserDesignatorItem.mark(shooter);
                s.gps(mark != null ? mark : ap != null ? ap : designated(shooter));
                ShellEntity.preloadPath(sl, pos, mark != null ? mark : ap != null ? ap : designated(shooter));
            }
            if (sh.guided && type().plane()) {
                breakStealth();                                                 // soute interne ouverte pour le tir
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
            case ATGM, SAM, AAM, AGM, ARM, CRUISE -> ModSounds.MISSILE_LAUNCH.get();
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
        boolean air = type().plane();
        double bestDot = Math.cos(Math.toRadians(air ? type().lockCone() : 6));
        double reach = air ? (selectedAmmo(type().weapons.indexOf(w)) == MilAmmo.AAM_RADAR ? 450 : 260) : 350;
        java.util.function.Predicate<Entity> filter = w.kind() == MilType.Kind.AGM
                ? e2 -> (e2 instanceof MilVehicleEntity mv && !mv.type().heli() && !mv.is(WRECK) || e2 instanceof fr.modeminecraft.entity.VehicleEntity)
                : e2 -> radarSees(this, e2) && !(e2 instanceof ShellEntity && air);
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(reach), filter)) {
            if (e.distanceToSqr(this) > reach * reach) {
                continue;
            }
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
        if (entityData.get(LOCK_ID) != lockCandidate) {
            entityData.set(LOCK_ID, lockCandidate);
        }
        setState(LOCKED, locked);
        setState(LOCKING, !locked && lockTicks > 0);
        if (tickCount % (locked ? 4 : 10) == 0 && lockTicks > 0 && shooter instanceof Player p) {
            level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.6f, locked ? 2f : 1.4f);
        }
    }

    /** Touche X : écran de fumée devant le véhicule (brise le verrouillage des missiles ennemis). */
    public void smoke() {
        if (type().plane()) {
            flares();
            return;
        }
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

    /** Déployé dès la génération (bases militaires). */
    public void deployNow() {
        setState(DEPLOYED, true);
    }

    /** Déploiement des complexes lance-missiles (béquilles + relèvement des tubes). */
    public void toggleDeploy(Entity by) {
        if (type().plane()) {
            toggleArm(by);
            return;
        }
        if (!type().has(MilType.Flag.STRATEGIC) && !raisable() || Math.abs(speed()) > 0.02f) {
            return;
        }
        if (raisable()) {
            // HIMARS : relevage / abaissement hydraulique du module lance-roquettes (2 s)
            if (level().getGameTime() < entityData.get(RAISE_END)) {
                return;                                                   // vérins déjà en mouvement
            }
            setState(DEPLOYED, !is(DEPLOYED));
            entityData.set(RAISE_END, (int) level().getGameTime() + RAISE_TICKS);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.HIMARS_HYDRAULICS.get(), SoundSource.NEUTRAL, 2f, is(DEPLOYED) ? 1f : 0.85f);
            return;
        }
        setState(DEPLOYED, !is(DEPLOYED));
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PISTON_EXTEND, SoundSource.NEUTRAL, 2f, 0.5f);
    }

    /** Lanceur à relever avant le tir (HIMARS). */
    public boolean raisable() {
        return type() == MilType.M142_HIMARS;
    }

    /** Progression du relevage (0 = couché, 1 = relevé et prêt), interpolée côté client. */
    public float raise(float partial) {
        if (!raisable()) {
            return 1;
        }
        float left = Mth.clamp((entityData.get(RAISE_END) - (level().getGameTime() + partial)) / RAISE_TICKS, 0, 1);
        return is(DEPLOYED) ? 1 - left : left;
    }

    /** Lanceur relevé, vérins à l'arrêt : le tir est autorisé. */
    public boolean raised() {
        return !raisable() || is(DEPLOYED) && level().getGameTime() >= entityData.get(RAISE_END);
    }

    /** Mémorise les coordonnées saisies à la console (Iskander). */
    public void saveTarget(int x, int z) {
        lastTargetX = x;
        lastTargetZ = z;
        hasSavedTarget = true;
        infoDirty = true;
    }

    /** Cible verrouillée par le système sol-air (client : carré rouge). */
    public int lockId() {
        return entityData.get(LOCK_ID);
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
        if (type().plane()) {
            t.putInt("bombs", bombBay.bombs());
            t.putBoolean("nuke", bombBay.hasNuke());
            t.putInt("station", currentStation());
            t.putInt("flares", flares);
            t.putInt("rearm", rearmTicks);
            t.putInt("crates", supplyCrates);
            if (cargo != null) {
                t.putString("cargo", cargo.getString("MilType"));
            }
        }
        if (hasSavedTarget) {
            t.putInt("LastTargetX", lastTargetX);                         // console Iskander : champs pré-remplis
            t.putInt("LastTargetZ", lastTargetZ);
        }
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
        // missile sol-air / antichar / obus antiaérien sur un hélicoptère en vol : rotor de queue arraché (vrille)
        if (type().heli() && !is(WRECK) && airborne() && (s.guided || s.blast >= 2)) {
            boolean rear = armorAt(at) == type().armorRear;
            if (rear || random.nextFloat() < 0.35f) {
                startCrash(!type().plane());                                     // avion : moteurs en feu, piqué (pas de vrille)
            }
        }
    }

    private boolean hurtInternal(float amount, Entity by, Vec3 from, boolean heavy) {
        return hurtInternal(amount, by, from, heavy, from == null ? (type().armorFront + type().armorSide + type().armorRear) / 3 : armorAt(from));
    }

    private boolean hurtInternal(float amount, Entity by, Vec3 from, boolean heavy, float armor) {
        if (destroyed || isRemoved()) {
            return false;                                                  // déjà en train d'exploser
        }
        float hp = health() - amount * armor;
        entityData.set(HEALTH, hp);
        level().playSound(null, getX(), getY(), getZ(), heavy ? SoundEvents.ANVIL_LAND : SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL,
                heavy ? 1f : 0.5f, 0.6f + random.nextFloat() * 0.4f);
        if (heavy && from != null && amount * armor > 12) {
            modules(from, true);
        }
        if (hp <= 0) {
            if (type().heli() && airborne() && hp > -type().health * 0.5f) {
                entityData.set(HEALTH, 1f);                                      // abattu en vol : il s'écrase
                startCrash(random.nextFloat() < 0.5f);
            } else {
                destroy(by);
            }
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

    /** Impulsion électromagnétique (Iskander EMP) : électronique grillée, moteur coupé, radar replié. */
    public void empHit() {
        if (level().isClientSide) {
            return;
        }
        setState(ENGINE_DOWN, true);
        setState(DEPLOYED, false);
        announce("message.modeminecraft.mil.emp");
    }

    /** Souffle d'une énorme explosion (frappe balistique, mine). */
    public void blastHit(Vec3 at, float damage) {
        if (!level().isClientSide && damage > 0) {
            hurtInternal(damage, null, at, true);
        }
    }

    /** Mine antichar sous le véhicule : gros dégâts et train de roulement détruit (sauf coque en V). */
    public void mineHit(Vec3 at, float damage) {
        if (level().isClientSide) {
            return;
        }
        hurtInternal(damage, null, at, true, 1f);
        if (!type().has(MilType.Flag.MINE_PROOF) && !is(MOBILITY_DOWN)) {
            setState(MOBILITY_DOWN, true);
            announce(type().tracked() ? "message.modeminecraft.mil.track_hit" : "message.modeminecraft.mil.wheel_hit");
        }
        for (Entity p : getPassengers()) {
            MilType.Seat s = seat(p);
            if (s != null && !s.enclosed() && !type().has(MilType.Flag.MINE_PROOF)) {
                p.hurt(damageSources().explosion(null, null), 8);
            }
        }
    }

    /** Explosion de la mine sous le véhicule (coque en V du M-ATV : -80 %). */
    public float mineFactor() {
        return type().has(MilType.Flag.MINE_PROOF) ? 0.2f : 1f;
    }

    private boolean destroyed;

    private void destroy(Entity by) {
        if (destroyed) {
            return;
        }
        destroyed = true;
        ServerLevel sl = (ServerLevel) level();
        float[] d = dims(type());
        for (Entity p : List.copyOf(getPassengers())) {
            p.stopRiding();
            p.hurt(by != null ? damageSources().explosion(this, by) : damageSources().generic(), 30);
        }
        sl.explode(this, getX(), getY() + d[2] / 2, getZ(), type().tracked() ? 4.5f : 3.5f, Level.ExplosionInteraction.NONE);
        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1, getZ(), 30, d[1] / 2, 0.5, d[1] / 2, 0.1);
        spawnAtLocation(new ItemStack(ModItems.STEEL_PLATE.get(), 2 + random.nextInt(4)));
        for (int i = 0; i < bombBay.getContainerSize(); i++) {
            ItemStack s = bombBay.getItem(i);
            if (!s.isEmpty() && !(s.getItem() instanceof fr.modeminecraft.nuclear.BombItem b && b.kind == fr.modeminecraft.nuclear.BombItem.Kind.NUKE)) {
                spawnAtLocation(s);
            }
        }
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
        if (hasPassenger(player) || is(WRECK)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        MilType t = type();
        if (player.isSecondaryUseActive() && t.plane()) {
            // soute à bombes (Maj + clic droit sous le fuselage)
            bombBay.user(player);
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x2, id, inv, bombBay, 2),
                    Component.translatable("container.modeminecraft.bomb_bay")));
            return InteractionResult.SUCCESS;
        }
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
                    int room = room(a);
                    int n = Math.min(held.getCount(), Math.max(0, (room + a.perItem - 1) / a.perItem));
                    if (n <= 0) {
                        player.displayClientMessage(Component.translatable("message.modeminecraft.mil.ammo_full"), true);
                        return InteractionResult.SUCCESS;
                    }
                    stock.put(a, ammo(a) + Math.min(room, n * a.perItem));
                    if (!player.getAbilities().instabuild) {
                        held.shrink(n);
                    }
                    infoDirty = true;
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1f, 0.8f);
                    player.displayClientMessage(Component.translatable("message.modeminecraft.mil.loaded", ammo(a), ammo(a) + room(a)), true);
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
        if (t.plane() && !isControlledByLocalInstance() && !is(CRASHING)) {
            // assiette vue par les autres joueurs : déduite de la trajectoire
            double dy = getY() - yo, hz = Math.hypot(getX() - xo, getZ() - zo);
            tilt = Mth.lerp(0.2f, tilt, hz > 0.05 ? (float) Math.toDegrees(Math.atan2(dy, hz)) : 0);
            roll = Mth.lerp(0.1f, roll, 0);
        }
        if (is(CRASHING)) {
            // nez qui plonge et roulis désordonné
            tilt = Mth.lerp(0.05f, tilt, -35f);
            roll = Mth.lerp(0.1f, roll, (float) Math.sin(tickCount * 0.3) * 12f);
        }
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
        entityData.set(STATE, tag.getInt("State") & (ENGINE_DOWN | MOBILITY_DOWN | DEPLOYED | WRECK | GEAR_UP | NUKE_ARMED));
        entityData.set(TURRET, tag.getFloat("Turret"));
        hasSavedTarget = tag.getBoolean("HasSavedTarget");
        destroyed = is(WRECK);
        lastTargetX = tag.getInt("LastTargetX");
        lastTargetZ = tag.getInt("LastTargetZ");
        infoDirty = true;
        entityData.set(PITCH, tag.getFloat("Pitch"));
        patrol = tag.getBoolean("Patrol");
        sentinel = tag.getBoolean("Sentinel");
        autoDefense = tag.getBoolean("AutoDefense");
        if (tag.hasUUID("Owner")) {
            ownerId = tag.getUUID("Owner");
        }
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
        if (tag.contains("BombBay")) {
            bombBay.fromTag(tag.getList("BombBay", 10), registryAccess());
        }
        if (tag.contains("Dest")) {
            destination = BlockPos.of(tag.getLong("Dest"));
        }
        if (tag.contains("Flares")) {
            flares = tag.getInt("Flares");
            station = tag.getInt("Station");
            supplyCrates = tag.getInt("Crates");
        }
        cargo = tag.contains("Cargo") ? tag.getCompound("Cargo") : null;
        parachute = tag.getBoolean("Parachute");
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
        if (hasSavedTarget) {
            tag.putBoolean("HasSavedTarget", true);
            tag.putInt("LastTargetX", lastTargetX);
            tag.putInt("LastTargetZ", lastTargetZ);
        }
        tag.putFloat("Pitch", entityData.get(PITCH));
        tag.putBoolean("Patrol", patrol);
        tag.putBoolean("Sentinel", sentinel);
        tag.putBoolean("AutoDefense", autoDefense);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        CompoundTag ammo = new CompoundTag();
        stock.forEach((a, n) -> ammo.putInt(a.name(), n));
        tag.put("Ammo", ammo);
        tag.putInt("Smoke", smoke);
        tag.putInt("Aps", aps);
        ListTag items = trunk.createTag(registryAccess());
        tag.put("Trunk", items);
        if (type().plane()) {
            tag.put("BombBay", bombBay.createTag(registryAccess()));
        }
        if (destination != null) {
            tag.putLong("Dest", destination.asLong());
        }
        if (type().plane()) {
            tag.putInt("Flares", flares);
            tag.putInt("Station", station);
            tag.putInt("Crates", supplyCrates);
            if (cargo != null) {
                tag.put("Cargo", cargo);
            }
        }
        if (parachute) {
            tag.putBoolean("Parachute", true);
        }
    }

    /** Le type est fixé à la création (chaque modèle a son propre type d'entité, voir ModEntities). */
    public void initType(MilType t) {
        entityData.set(TYPE, t.ordinal());
        entityData.set(HEALTH, t.health);
        refreshDimensions();
    }
}

package fr.modeminecraft.entity;

import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Véhicule conduisible : quad, voiture de police, jeep militaire, camion blindé.
 * - conduit par un joueur (Z/S accélérer / freiner / reculer, Q/D tourner — la direction dépend de la vitesse) ;
 * - ou par un bot (convois de patrouille) qui suit une destination en évitant les obstacles ;
 * - monte les marches d'un bloc, ralentit dans l'eau, prend des dégâts (blindage) et explose quand il est détruit.
 */
public class VehicleEntity extends Entity {
    public enum Type {
        //          id               vmax  accél. virage PV   blindage places  sirène largeur hauteur
        QUAD("quad", 0.55f, 0.030f, 4.5f, 30, 1.0f, 2, false, 1.4f, 1.1f),
        POLICE_CAR("police_car", 0.80f, 0.028f, 3.6f, 60, 0.8f, 4, true, 2.0f, 1.5f),
        JEEP("jeep", 0.70f, 0.026f, 3.4f, 70, 0.7f, 4, false, 2.0f, 1.6f),
        ARMORED_TRUCK("armored_truck", 0.50f, 0.016f, 2.2f, 200, 0.3f, 4, false, 2.6f, 2.6f);

        public final String id;
        public final float maxSpeed, accel, turn, health, armor;
        public final int seats;
        public final boolean siren;
        public final float width, height;

        Type(String id, float maxSpeed, float accel, float turn, float health, float armor, int seats, boolean siren, float width, float height) {
            this.id = id;
            this.maxSpeed = maxSpeed;
            this.accel = accel;
            this.turn = turn;
            this.health = health;
            this.armor = armor;
            this.seats = seats;
            this.siren = siren;
            this.width = width;
            this.height = height;
        }

        public Item item() {
            return switch (this) {
                case QUAD -> ModItems.QUAD.get();
                case POLICE_CAR -> ModItems.POLICE_CAR.get();
                case JEEP -> ModItems.JEEP.get();
                case ARMORED_TRUCK -> ModItems.ARMORED_TRUCK.get();
            };
        }

        /** Places (avant / côté / hauteur, en blocs dans le repère du véhicule). Place 0 = conducteur. */
        public Vec3 seat(int i) {
            return switch (this) {
                case QUAD -> i == 0 ? new Vec3(-0.05, 0.55, 0) : new Vec3(-0.6, 0.62, 0);
                case POLICE_CAR, JEEP -> new Vec3(i < 2 ? 0.25 : -0.65, 0.35, i % 2 == 0 ? -0.42 : 0.42);
                case ARMORED_TRUCK -> new Vec3(i < 2 ? 1.6 : -0.8, 0.95, i % 2 == 0 ? -0.5 : 0.5);
            };
        }
    }

    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STEER = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SIREN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);

    /** Vitesse (blocs / tick, négative en marche arrière) et braquage (-1 à 1). */
    private float speed, steer;
    /** Distance parcourue (rotation des roues). */
    public float wheelRot, wheelRotO;
    /** Destination d'un convoi (bot au volant) ; véhicule de tête suivi. */
    public BlockPos destination;
    public java.util.UUID leader;
    /** Véhicule de convoi (disparaît avec son équipage quand aucun joueur n'est proche). */
    public boolean convoy;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYaw;
    private int stuckTicks;

    public VehicleEntity(EntityType<? extends VehicleEntity> type, Level level) {
        super(type, level);
        blocksBuilding = true;
    }

    public VehicleEntity(Level level, Type type, double x, double y, double z, float yaw) {
        this(ModEntities.VEHICLE.get(), level);
        entityData.set(TYPE, type.ordinal());
        entityData.set(HEALTH, type.health);
        setPos(x, y, z);
        setYRot(yaw);
        yRotO = yaw;
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(TYPE, 0).define(HEALTH, 30f).define(SPEED, 0f).define(STEER, 0f).define(SIREN, false);
    }

    public Type type() {
        return Type.values()[Math.floorMod(entityData.get(TYPE), Type.values().length)];
    }

    public float speed() {
        return entityData.get(SPEED);
    }

    public float steering() {
        return entityData.get(STEER);
    }

    public boolean sirenOn() {
        return entityData.get(SIREN);
    }

    public float health() {
        return entityData.get(HEALTH);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        Type t = type();
        return EntityDimensions.scalable(t.width, t.height);
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
        return 1.05f;
    }

    // ------------------------------------------------------------------ conduite
    @Override
    public void tick() {
        super.tick();
        wheelRotO = wheelRot;
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            drive();
        } else if (!level().isClientSide) {
            // conduit par un joueur : le serveur déduit la vitesse du déplacement (roues qui tournent pour tous)
            double rad = Math.toRadians(getYRot());
            Vec3 moved = position().subtract(xo, yo, zo);
            float v = (float) (moved.x * -Math.sin(rad) + moved.z * Math.cos(rad));
            entityData.set(SPEED, v);
        } else if (lerpSteps > 0) {
            double k = 1.0 / lerpSteps;
            setPos(getX() + (lerpX - getX()) * k, getY() + (lerpY - getY()) * k, getZ() + (lerpZ - getZ()) * k);
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYaw - getYRot()) * (float) k);
            lerpSteps--;
        }
        wheelRot += speed() * 1.8f;
        if (!level().isClientSide) {
            if (convoy && tickCount % 200 == 0 && level().getNearestPlayer(this, 192) == null) {
                for (Entity p : java.util.List.copyOf(getPassengers())) {
                    p.getPassengers().forEach(Entity::discard);
                    p.discard();
                }
                discard();
                return;
            }
            if (health() < type().health * 0.3f && tickCount % 4 == 0) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + type().height, getZ(), 1, 0.2, 0.1, 0.2, 0.01);
            }
            if (sirenOn() && tickCount % 40 == 0) {
                level().playSound(null, getX(), getY(), getZ(), fr.modeminecraft.registry.ModSounds.POLICE_FREEZE_WARNING.get(), SoundSource.NEUTRAL, 0.7f, 1.4f);
            }
        }
        if (level().isClientSide && sirenOn() && tickCount % 2 == 0) {
            boolean red = (tickCount / 4) % 2 == 0;
            double rad = Math.toRadians(getYRot());
            double side = red ? -0.35 : 0.35;
            level().addParticle(new net.minecraft.core.particles.DustParticleOptions(red ? new org.joml.Vector3f(1, 0.1f, 0.1f) : new org.joml.Vector3f(0.15f, 0.3f, 1), 1.2f),
                    getX() - Math.sin(rad) * 0.1 + Math.cos(rad) * side, getY() + 1.55, getZ() + Math.cos(rad) * 0.1 + Math.sin(rad) * side, 0, 0, 0);
        }
        if (Math.abs(speed()) > 0.05 && tickCount % 10 == 0 && level().isClientSide) {
            level().addParticle(ParticleTypes.SMOKE, getX() - Math.cos(Math.toRadians(getYRot() + 90)) * type().width, getY() + 0.3,
                    getZ() - Math.sin(Math.toRadians(getYRot() + 90)) * type().width, 0, 0.02, 0);
        }
    }

    /** Lit les commandes (joueur ou bot), puis déplace le véhicule. */
    private void drive() {
        Type t = type();
        float forward = 0, side = 0;
        Entity c = getControllingPassenger();
        if (c instanceof Player p) {
            forward = p.zza;
            side = p.xxa;
        } else if (c instanceof HumanBotEntity bot && !level().isClientSide) {
            float[] in = autopilot(bot);
            forward = in[0];
            side = in[1];
        }
        boolean water = isInWater();
        if (forward > 0) {
            speed = Math.min(t.maxSpeed * (water ? 0.3f : 1f), speed + t.accel);
        } else if (forward < 0) {
            speed = speed > 0 ? Math.max(0, speed - t.accel * 3) : Math.max(-t.maxSpeed * 0.35f, speed - t.accel * 0.6f);
        } else {
            speed *= onGround() ? 0.94f : 0.99f;
            if (Math.abs(speed) < 0.003f) {
                speed = 0;
            }
        }
        steer = Mth.lerp(0.35f, steer, -side);
        float grip = Math.min(1, Math.abs(speed) / 0.12f);
        setYRot(getYRot() + steer * t.turn * grip * Math.signum(speed == 0 ? 1 : speed));
        double rad = Math.toRadians(getYRot());
        Vec3 dir = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 dm = getDeltaMovement();
        double vy = onGround() ? -0.04 : Math.max(-1.5, dm.y - 0.08);
        if (water) {
            vy = Math.min(vy + 0.06, 0.02);
        }
        setDeltaMovement(dir.x * speed, vy, dir.z * speed);
        Vec3 before = position();
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision && Math.abs(speed) > 0.2f) {
            speed *= -0.3f;                                   // choc contre un mur
            if (!level().isClientSide) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_HURT, SoundSource.NEUTRAL, 0.8f, 0.6f);
            }
        }
        // écrase / bouscule ce qu'il percute à pleine vitesse
        if (!level().isClientSide && Math.abs(speed) > 0.35f) {
            for (Entity e : level().getEntities(this, getBoundingBox().inflate(0.2), e -> e instanceof LivingEntity && !hasPassenger(e))) {
                e.hurt(c instanceof LivingEntity le ? damageSources().mobAttack(le) : damageSources().generic(), Math.abs(speed) * 14);
                e.push(dir.x * speed * 2, 0.3, dir.z * speed * 2);
            }
        }
        stuckTicks = before.distanceToSqr(position()) < 0.0004 && forward != 0 ? stuckTicks + 1 : 0;
        entityData.set(SPEED, speed);
        entityData.set(STEER, steer);
    }

    /** Pilote automatique d'un bot : vers la destination (ou derrière le véhicule de tête), en évitant les obstacles. */
    private float[] autopilot(HumanBotEntity bot) {
        Vec3 goal;
        if (leader != null && level() instanceof ServerLevel sl && sl.getEntity(leader) instanceof VehicleEntity lead && lead.isAlive()) {
            double r = Math.toRadians(lead.getYRot());
            goal = lead.position().subtract(-Math.sin(r) * 9, 0, Math.cos(r) * 9);
        } else if (destination != null) {
            goal = Vec3.atBottomCenterOf(destination);
        } else {
            return new float[]{0, 0};
        }
        // un ennemi en vue : on s'arrête, l'équipage descend combattre
        if (bot.getTarget() != null && bot.distanceTo(bot.getTarget()) < 30) {
            if (Math.abs(speed) < 0.05f) {
                for (Entity p : java.util.List.copyOf(getPassengers())) {
                    if (p instanceof HumanBotEntity crew && p != getControllingPassenger() && !(p.getVehicle() instanceof MountedGunEntity)) {
                        crew.stopRiding();
                    }
                }
            }
            return new float[]{-1, 0};
        }
        Vec3 to = goal.subtract(position());
        double dist = Math.sqrt(to.x * to.x + to.z * to.z);
        if (dist < 6) {
            if (leader == null) {
                destination = null;
            }
            return new float[]{-1, 0};
        }
        float want = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90;
        float diff = Mth.wrapDegrees(want - getYRot());
        // obstacle droit devant : on contourne
        double rad = Math.toRadians(getYRot());
        Vec3 eye = position().add(0, 1.2, 0);
        Vec3 ahead = eye.add(-Math.sin(rad) * 5, 0, Math.cos(rad) * 5);
        boolean blocked = level().clip(new ClipContext(eye, ahead, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this)).getType() == HitResult.Type.BLOCK;
        // à-pic ou eau devant : on contourne aussi (pas de chute dans les ravins)
        for (double d = 3; d <= 6 && !blocked; d += 1.5) {
            int ax = Mth.floor(getX() - Math.sin(rad) * d), az = Mth.floor(getZ() + Math.cos(rad) * d);
            int ground = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ax, az);
            BlockPos top = new BlockPos(ax, ground - 1, az);
            if (ground < getY() - 3 || !level().getFluidState(top).isEmpty()) {
                blocked = true;
            }
        }
        if (blocked || stuckTicks > 20) {
            diff = diff >= 0 ? 60 : -60;
        }
        float side = Mth.clamp(-diff / 25f, -1, 1);
        float fwd = stuckTicks > 40 ? -1 : (Math.abs(diff) > 100 ? 0.3f : 1f);
        if (stuckTicks > 60) {
            stuckTicks = 0;
        }
        return new float[]{fwd * (leader != null && dist < 12 ? 0.4f : 0.7f), side};
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
    public LivingEntity getControllingPassenger() {
        Entity first = getFirstPassenger();
        return first instanceof Player || first instanceof HumanBotEntity ? (LivingEntity) first : null;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYaw = yaw;
        lerpSteps = 10;
    }

    // ------------------------------------------------------------------ passagers
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || hasPassenger(player)) {
            return InteractionResult.PASS;
        }
        if (getPassengers().size() >= type().seats) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < type().seats + (type() == Type.ARMORED_TRUCK ? 1 : 0);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (!hasPassenger(passenger)) {
            return;
        }
        int i = getPassengers().indexOf(passenger);
        Vec3 off;
        if (passenger instanceof MountedGunEntity) {
            off = new Vec3(-0.6, type().height - 0.05, 0);                 // tourelle sur le toit du camion
            passenger.setYRot(getYRot());
        } else {
            off = type().seat(Math.min(i, type().seats - 1));
        }
        double rad = Math.toRadians(getYRot());
        double fx = -Math.sin(rad), fz = Math.cos(rad);       // avant
        double rx = -fz, rz = fx;                              // droite
        move.accept(passenger, getX() + fx * off.x + rx * off.z, getY() + off.y, getZ() + fz * off.x + rz * off.z);
        if (passenger instanceof LivingEntity le && !(passenger instanceof Player)) {
            le.setYBodyRot(getYRot());
        }
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (passenger instanceof Player p && type().siren && getPassengers().size() == 1 && !level().isClientSide) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.modeminecraft.vehicle.drive"), true);
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double rad = Math.toRadians(getYRot() + 90);
        return position().add(Math.cos(rad) * (type().width / 2 + 0.6), 0.1, Math.sin(rad) * (type().width / 2 + 0.6));
    }

    public void toggleSiren() {
        if (type().siren) {
            entityData.set(SIREN, !sirenOn());
        }
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
        if (source.getEntity() instanceof Player p && p.getAbilities().instabuild && source.getDirectEntity() == p) {
            discard();
            return true;
        }
        float hp = health() - amount * type().armor;
        entityData.set(HEALTH, hp);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.3f, 1.6f);
        if (hp <= 0) {
            level().explode(this, getX(), getY() + 1, getZ(), type() == Type.ARMORED_TRUCK ? 3.5f : 2.5f, Level.ExplosionInteraction.NONE);
            spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 2 + random.nextInt(4)));
            discard();
        }
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(type().item());
    }

    // ------------------------------------------------------------------ sauvegarde
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TYPE, tag.getInt("VehicleType"));
        convoy = tag.getBoolean("Convoy");
        entityData.set(HEALTH, tag.contains("Health") ? tag.getFloat("Health") : type().health);
        if (tag.contains("Dest")) {
            destination = BlockPos.of(tag.getLong("Dest"));
        }
        if (tag.hasUUID("Leader")) {
            leader = tag.getUUID("Leader");
        }
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("VehicleType", type().ordinal());
        tag.putBoolean("Convoy", convoy);
        tag.putFloat("Health", health());
        if (destination != null) {
            tag.putLong("Dest", destination.asLong());
        }
        if (leader != null) {
            tag.putUUID("Leader", leader);
        }
    }
}

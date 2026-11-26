package fr.modeminecraft.entity.wild;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Faune sauvage du réalisme naturel (lot 3) :
 * - [12] aigle : plane en grands cercles dans le ciel des montagnes et de la Mesa ;
 * - [13] mouette : cercle près des côtes, suit les navires, se pose de temps en temps sur l'eau ou la roche ;
 * - [16] scorpion et serpent : discrets au sol du désert, ne piquent (poison léger) que si on leur marche dessus ;
 * - [17] cerf : harde paisible en lisière de forêt qui s'enfuit au moindre véhicule ou joueur qui court.
 * Même classe pour toutes les espèces ; l'espèce est fixée à la création (un type d'entité par espèce).
 */
public class WildlifeEntity extends PathfinderMob {
    public enum Kind {
        EAGLE(8, 0.3, 0.9f, 0.7f, 1.7f), GULL(4, 0.3, 0.5f, 0.4f, 1.0f), SCORPION(6, 0.17, 0.6f, 0.3f, 1.0f), SNAKE(6, 0.2, 0.7f, 0.25f, 1.0f), DEER(14, 0.3, 0.9f, 1.4f, 1.1f);

        public final int health;
        public final double speed;
        public final float width, height, scale;

        Kind(int health, double speed, float width, float height, float scale) {
            this.health = health;
            this.speed = speed;
            this.width = width;
            this.height = height;
            this.scale = scale;
        }

        public String id() {
            return name().toLowerCase();
        }

        public boolean flier() {
            return this == EAGLE || this == GULL;
        }
    }

    public final Kind kind;
    /** Centre et rayon du cercle de vol, sens de rotation, état de pose (mouette). */
    private double anchorX, anchorZ, radius = 30, angle;
    private int dir = 1, perch;
    private double perchX, perchZ;
    private long lastBite;
    /** Animation des ailes (client). */
    public float flap, flapO;

    public WildlifeEntity(EntityType<? extends PathfinderMob> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        this.dir = random.nextBoolean() ? 1 : -1;
        this.angle = random.nextDouble() * Math.PI * 2;
        if (kind.flier()) {
            setNoGravity(true);
        }
        initGoals();
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, kind.health).add(Attributes.MOVEMENT_SPEED, kind.speed).add(Attributes.FOLLOW_RANGE, 24);
    }

    /** Point autour duquel un oiseau tourne (à la création). */
    public void setAnchor(double x, double z, double radius) {
        this.anchorX = x;
        this.anchorZ = z;
        this.radius = radius;
    }

    /** Appelé depuis le constructeur, une fois l'espèce connue (registerGoals() du parent s'exécute avant, quand elle est encore nulle). */
    private void initGoals() {
        switch (kind) {
            case SCORPION, SNAKE -> {
                goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6, 240));          // reste longtemps immobile, à l'ombre des rochers
                goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6));
            }
            case DEER -> {
                goalSelector.addGoal(1, new FleeGoal(this));
                goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8, 0.008f));
                goalSelector.addGoal(6, new RandomLookAroundGoal(this));
            }
            default -> {
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        flapO = flap;
        if (!kind.flier()) {
            return;
        }
        if (level().isClientSide) {
            flap += perch > 0 ? 0f : kind == Kind.EAGLE ? 0.18f : 1.1f;
            return;
        }
        fly();
    }

    private void fly() {
        // la mouette suit un navire proche : le centre de son cercle devient le navire
        if (kind == Kind.GULL && tickCount % 20 == 0) {
            Entity ship = nearestShip();
            if (ship != null) {
                anchorX = ship.getX();
                anchorZ = ship.getZ();
                radius = 9;
                perch = 0;
            } else if (perch == 0 && random.nextInt(18) == 0) {
                perch = 140 + random.nextInt(200);                            // se pose
                perchX = getX();
                perchZ = getZ();
            }
        }
        double tx, tz, ty;
        if (perch > 0) {
            perch--;
            tx = perchX;
            tz = perchZ;
            ty = level().getHeight(Heightmap.Types.WORLD_SURFACE, (int) Math.floor(tx), (int) Math.floor(tz)) + 0.1;
        } else {
            angle += (kind == Kind.EAGLE ? 0.011 : 0.045) * dir;
            tx = anchorX + Math.cos(angle) * radius;
            tz = anchorZ + Math.sin(angle) * radius;
            int surface = level().getHeight(Heightmap.Types.WORLD_SURFACE, (int) Math.floor(tx), (int) Math.floor(tz));
            ty = Math.max(surface, level().getSeaLevel()) + (kind == Kind.EAGLE ? 34 : 7) + Math.sin(tickCount * 0.03) * 2.5;
        }
        Vec3 to = new Vec3(tx - getX(), ty - getY(), tz - getZ());
        double dist = to.length();
        double speed = perch > 0 ? 0.12 : kind == Kind.EAGLE ? 0.42 : 0.3;
        Vec3 v = dist < 0.4 ? Vec3.ZERO : to.scale(1.0 / dist).scale(Math.min(speed, dist * 0.25));
        setDeltaMovement(getDeltaMovement().scale(0.5).add(v.scale(0.5)));
        Vec3 dm = getDeltaMovement();
        if (dm.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) Math.toDegrees(Math.atan2(-dm.x, dm.z));
            setYRot(Mth.rotLerp(0.3f, getYRot(), yaw));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    private Entity nearestShip() {
        List<Entity> found = level().getEntities(this, getBoundingBox().inflate(48, 20, 48), e -> e instanceof Boat
                || e instanceof fr.modeminecraft.entity.military.MilVehicleEntity m && m.type().naval());
        Entity best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : found) {
            double d = e.distanceToSqr(this);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    /** Scorpions et serpents : léger poison seulement si on leur marche directement dessus. */
    @Override
    public void playerTouch(Player player) {
        if ((kind == Kind.SCORPION || kind == Kind.SNAKE) && !level().isClientSide && !player.isCreative() && !player.isSpectator() && tickCount - lastBite > 60) {
            lastBite = tickCount;
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            player.hurt(damageSources().mobAttack(this), 1.0f);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return !kind.flier() && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("AnchorX", anchorX);
        tag.putDouble("AnchorZ", anchorZ);
        tag.putDouble("Radius", radius);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        anchorX = tag.getDouble("AnchorX");
        anchorZ = tag.getDouble("AnchorZ");
        radius = tag.contains("Radius") ? tag.getDouble("Radius") : 30;
    }

    /** Cerf : s'enfuit vite devant un joueur qui court (ou trop proche) et devant tout véhicule en mouvement, et prévient toute la harde. */
    static class FleeGoal extends Goal {
        private final WildlifeEntity deer;
        private Vec3 threat;
        private int ticks;

        FleeGoal(WildlifeEntity deer) {
            this.deer = deer;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (deer.tickCount % 4 != 0) {
                return false;
            }
            threat = findThreat();
            return threat != null;
        }

        private Vec3 findThreat() {
            Player p = deer.level().getNearestPlayer(deer, 22);
            if (p != null && !p.isSpectator() && !p.isCreative() && !p.isCrouching()
                    && (p.isSprinting() || p.getVehicle() != null || p.getAbilities().flying || p.distanceToSqr(deer) < 49)) {
                return p.position();
            }
            for (Entity e : deer.level().getEntities(deer, deer.getBoundingBox().inflate(40), x -> x instanceof fr.modeminecraft.entity.VehicleEntity
                    || x instanceof fr.modeminecraft.entity.military.MilVehicleEntity || x instanceof fr.modeminecraft.entity.drone.DroneEntity)) {
                if (e.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                    return e.position();
                }
            }
            return null;
        }

        @Override
        public void start() {
            ticks = 0;
            run(deer, threat);
            for (WildlifeEntity other : deer.level().getEntitiesOfClass(WildlifeEntity.class, deer.getBoundingBox().inflate(18), o -> o.kind == Kind.DEER && o != deer)) {
                run(other, threat);                                           // toute la harde part ensemble
            }
        }

        private static void run(WildlifeEntity d, Vec3 from) {
            Vec3 pos = DefaultRandomPos.getPosAway(d, 20, 8, from);
            if (pos != null) {
                d.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.7);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return ticks++ < 90 && !deer.getNavigation().isDone();
        }
    }
}

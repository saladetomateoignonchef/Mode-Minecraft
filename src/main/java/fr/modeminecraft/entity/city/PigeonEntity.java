package fr.modeminecraft.entity.city;

import fr.modeminecraft.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Pigeon (ou mouette près de l'eau) posé sur les places et les parcs : il picore et roucoule ; si un joueur approche
 * (sans se faufiler) ou si un coup de feu éclate, toute la nuée s'envole d'un coup et disparaît au loin.
 */
public class PigeonEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(PigeonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GULL = SynchedEntityData.defineId(PigeonEntity.class, EntityDataSerializers.BOOLEAN);
    private Vec3 flight;
    private int flown;
    /** Animation des ailes / du picorage (client). */
    public float flap, flapO, peck;

    public PigeonEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 3).add(Attributes.MOVEMENT_SPEED, 0.18);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(FLYING, false);
        b.define(GULL, false);
    }

    public boolean flying() { return entityData.get(FLYING); }
    public boolean gull() { return entityData.get(GULL); }
    public void setGull(boolean g) { entityData.set(GULL, g); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    /** La nuée s'envole (tous les oiseaux à 8 blocs). */
    public void scare(Vec3 from) {
        if (flying()) {
            return;
        }
        for (PigeonEntity p : level().getEntitiesOfClass(PigeonEntity.class, getBoundingBox().inflate(8))) {
            p.takeOff(from);
        }
    }

    private void takeOff(Vec3 from) {
        if (flying()) {
            return;
        }
        entityData.set(FLYING, true);
        setNoGravity(true);
        Vec3 away = position().subtract(from);
        away = new Vec3(away.x, 0, away.z).normalize();
        if (away.lengthSqr() < 0.01) {
            away = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).normalize();
        }
        double a = (random.nextDouble() - 0.5) * 0.9;
        flight = new Vec3(away.x * Math.cos(a) - away.z * Math.sin(a), 0, away.x * Math.sin(a) + away.z * Math.cos(a));
        flown = 0;
        playSound(ModSounds.PIGEON_FLAP.get(), 0.8f, 0.9f + random.nextFloat() * 0.3f);
    }

    @Override
    public void tick() {
        super.tick();
        flapO = flap;
        if (level().isClientSide) {
            flap += flying() ? 1.6f : 0;
            peck = !flying() && getDeltaMovement().horizontalDistanceSqr() < 1.0E-4 ? peck + 0.12f : 0;
            return;
        }
        if (flying()) {
            flown++;
            double climb = flown < 30 ? 0.35 : 0.12;
            setDeltaMovement(flight.x * 0.45, climb, flight.z * 0.45);
            setYRot((float) Math.toDegrees(Math.atan2(-flight.x, flight.z)));
            yBodyRot = getYRot();
            if (flown > 140) {
                discard();                                           // la nuée est partie au loin
            }
            return;
        }
        if (tickCount % 5 == 0) {
            Player p = level().getNearestPlayer(this, 5);
            if (p != null && !p.isShiftKeyDown() && !p.isSpectator()) {
                scare(p.position());
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            scare(source.getSourcePosition() != null ? source.getSourcePosition() : position());
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return gull() ? null : ModSounds.PIGEON_COO.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Gull", gull());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setGull(tag.getBoolean("Gull"));
    }
}

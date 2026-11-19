package fr.modeminecraft.entity;

import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Roquette : laisse une traînée de fumée et explose au premier contact.
 * Les dégâts aux blocs suivent la règle mobGriefing (comme un creeper).
 */
public class RocketEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE_TICKS = 200;

    private float explosionPower = 3.0f; // TNT = 4

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
    }

    public RocketEntity(Level level, LivingEntity shooter, float explosionPower) {
        super(ModEntities.ROCKET.get(), shooter, level);
        this.explosionPower = explosionPower;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.RPG_ROCKET.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.005;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 back = position().subtract(getDeltaMovement().scale(0.5));
            level().addParticle(ParticleTypes.LARGE_SMOKE, back.x, back.y, back.z, 0, 0.02, 0);
            level().addParticle(ParticleTypes.FLAME, back.x, back.y, back.z, 0, 0, 0);
        } else if (tickCount > MAX_LIFE_TICKS) {
            explode();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            explode();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ExplosionPower", explosionPower);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        explosionPower = tag.getFloat("ExplosionPower");
    }

    private void explode() {
        level().explode(this, getX(), getY(), getZ(), explosionPower, Level.ExplosionInteraction.MOB);
        discard();
    }
}

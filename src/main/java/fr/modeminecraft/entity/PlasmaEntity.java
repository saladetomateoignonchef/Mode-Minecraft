package fr.modeminecraft.entity;

import fr.modeminecraft.registry.ModDamageTypes;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Boule d'énergie du fusil à plasma : file droit, brûle la cible. */
public class PlasmaEntity extends ThrowableItemProjectile {
    private float damage = 7;

    public PlasmaEntity(EntityType<? extends PlasmaEntity> type, Level level) {
        super(type, level);
    }

    public PlasmaEntity(Level level, LivingEntity shooter, float damage) {
        super(ModEntities.PLASMA.get(), shooter, level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PLASMA_BALL.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 0, 0, 0);
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > 40) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            result.getEntity().hurt(level().damageSources().source(ModDamageTypes.BULLET, this, getOwner()), damage);
            result.getEntity().igniteForSeconds(4);
            result.getEntity().invulnerableTime = 0;
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            ((net.minecraft.server.level.ServerLevel) level()).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.05);
            discard();
        }
    }
}

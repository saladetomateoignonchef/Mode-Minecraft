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
 * Roquette (ou grenade 40 mm) : laisse une traînée de fumée et explose au premier contact.
 * Les dégâts aux blocs suivent la règle mobGriefing (comme un creeper).
 */
public class RocketEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE_TICKS = 200;

    private float explosionPower = 3.0f; // TNT = 4
    private int targetId = -1;           // missile guidé : entité visée
    private boolean topAttack = false;   // Javelin : monte d'abord, puis plonge sur la cible
    private float directDamage = 0;      // dégâts directs à l'impact (Panzerfaust : ignore 90 % de l'armure)
    private static final float MAX_TURN = (float) Math.toRadians(6); // virage maximal par tick

    public void guide(net.minecraft.world.entity.Entity target, boolean topAttack) {
        this.targetId = target == null ? -1 : target.getId();
        this.topAttack = topAttack;
    }

    public void setDirectDamage(float damage) {
        this.directDamage = damage;
    }

    private boolean isMissile() {
        return getType() == ModEntities.MISSILE.get();
    }

    /** Guidage : on tourne la vitesse vers la cible de 6° maximum par tick. */
    private void steer() {
        var target = targetId < 0 ? null : level().getEntity(targetId);
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3 v = getDeltaMovement();
        double speed = Math.max(v.length(), 1.6);
        Vec3 aim = target.position().add(0, target.getBbHeight() / 2, 0).subtract(position());
        if (topAttack && tickCount <= 18) {
            // Javelin : montée à 55° vers le ciel pendant les 18 premiers ticks
            Vec3 flat = new Vec3(aim.x, 0, aim.z).normalize();
            aim = flat.scale(Math.cos(Math.toRadians(55))).add(0, Math.sin(Math.toRadians(55)), 0);
        }
        Vec3 cur = v.normalize(), want = aim.normalize();
        double angle = Math.acos(Math.max(-1, Math.min(1, cur.dot(want))));
        Vec3 next = angle <= MAX_TURN ? want : cur.add(want.subtract(cur).scale(MAX_TURN / angle)).normalize();
        setDeltaMovement(next.scale(speed));
        hasImpulse = true;
    }

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
    }

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level, LivingEntity shooter, float explosionPower) {
        super(type, shooter, level);
        this.explosionPower = explosionPower;
    }

    /** Grenade 40 mm : tir en cloche, petite traînée. Roquette : presque droite, grosse traînée. */
    private boolean isGrenade() {
        return getType() == ModEntities.GRENADE.get();
    }

    @Override
    protected Item getDefaultItem() {
        if (isMissile()) {
            return ModItems.STINGER_MISSILE.get();
        }
        return isGrenade() ? ModItems.GRENADE_40MM.get() : ModItems.RPG_ROCKET.get();
    }

    @Override
    protected double getDefaultGravity() {
        if (isMissile()) {
            return targetId >= 0 ? 0.0 : 0.004;
        }
        return isGrenade() ? 0.04 : 0.005;
    }

    @Override
    public void tick() {
        if (!level().isClientSide && isMissile()) {
            steer();
        }
        super.tick();
        if (level().isClientSide) {
            Vec3 back = position().subtract(getDeltaMovement().scale(0.5));
            if (isGrenade()) {
                level().addParticle(ParticleTypes.SMOKE, back.x, back.y, back.z, 0, 0, 0);
            } else {
                level().addParticle(ParticleTypes.LARGE_SMOKE, back.x, back.y, back.z, 0, 0.02, 0);
                level().addParticle(ParticleTypes.FLAME, back.x, back.y, back.z, 0, 0, 0);
            }
        } else if (tickCount > MAX_LIFE_TICKS) {
            explode();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide && directDamage > 0 && result instanceof net.minecraft.world.phys.EntityHitResult e
                && e.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
            // Charge anti-blindage : dégâts directs qui ignorent 90 % de l'armure
            var source = level().damageSources().source(fr.modeminecraft.registry.ModDamageTypes.BULLET_AP, this, getOwner());
            float dmg = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(living, directDamage, source,
                    living.getArmorValue() * 0.1f, 0);
            living.hurt(source, dmg);
        }
        if (!level().isClientSide) {
            explode();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ExplosionPower", explosionPower);
        tag.putFloat("DirectDamage", directDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        explosionPower = tag.getFloat("ExplosionPower");
        directDamage = tag.getFloat("DirectDamage");
    }

    private void explode() {
        level().explode(this, getX(), getY(), getZ(), explosionPower, Level.ExplosionInteraction.MOB);
        discard();
    }
}

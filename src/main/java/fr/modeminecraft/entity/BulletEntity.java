package fr.modeminecraft.entity;

import fr.modeminecraft.registry.ModDamageTypes;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * Balle tirée par une arme à feu.
 * - Va très vite, sans gravité, et disparaît au bout de 3 secondes.
 * - Tir à la tête = dégâts x1.5.
 * - Casse le verre (si la règle mobGriefing est activée).
 */
public class BulletEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE_TICKS = 60;
    private static final float HEADSHOT_MULTIPLIER = 1.5f;

    private float damage = 5.0f;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public BulletEntity(Level level, LivingEntity shooter, float damage) {
        super(ModEntities.BULLET.get(), shooter, level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BULLET.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0; // trajectoire droite
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > MAX_LIFE_TICKS) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Entity target = result.getEntity();
        float finalDamage = damage;

        // Tir à la tête : on regarde à quelle hauteur la balle traverse la hitbox de la cible
        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement());
        boolean headshot = target instanceof LivingEntity living && target.getBoundingBox().clip(start, end)
                .map(hit -> hit.y >= living.getEyeY() - 0.25)
                .orElse(false);
        if (headshot) {
            finalDamage *= HEADSHOT_MULTIPLIER;
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(), 8, 0.2, 0.2, 0.2, 0.2);
            if (getOwner() instanceof Player shooter) {
                level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                        SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.6f, 1.6f);
            }
        }

        DamageSource source = level.damageSources().source(ModDamageTypes.BULLET, this, getOwner());
        if (target.hurt(source, finalDamage)) {
            // Supprime l'invincibilité temporaire : sinon les plombs du fusil à pompe
            // et les rafales du fusil d'assaut ne toucheraient qu'une fois sur deux.
            target.invulnerableTime = 0;
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel level) {
            BlockPos pos = result.getBlockPos();
            BlockState state = level.getBlockState(pos);
            Vec3 hit = result.getLocation();

            boolean glass = state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES);
            if (glass && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                level.destroyBlock(pos, false, getOwner());
            } else {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        hit.x, hit.y, hit.z, 6, 0.05, 0.05, 0.05, 0.1);
                level.sendParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, 2, 0.02, 0.02, 0.02, 0.01);
                level.playSound(null, pos, state.getSoundType(level, pos, this).getHitSound(),
                        SoundSource.BLOCKS, 0.5f, 1.4f);
            }
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
    }
}

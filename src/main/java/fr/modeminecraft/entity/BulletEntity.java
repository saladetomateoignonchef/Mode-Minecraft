package fr.modeminecraft.entity;

import fr.modeminecraft.item.gun.AmmoType;
import fr.modeminecraft.registry.ModDamageTypes;
import fr.modeminecraft.registry.ModParticles;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * - Son type (traçante, explosive, incendiaire...) ajoute un effet à l'impact, voir {@link #applyTypeEffect}.
 */
public class BulletEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE_TICKS = 60;
    private static final float HEADSHOT_MULTIPLIER = 1.5f;

    /** Type de munition, synchronisé avec le client (pour la traînée des traçantes). */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> TYPE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(BulletEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);

    private float damage = 5.0f;
    private int pierceLeft = 0;          // ennemis encore traversables
    private boolean armorPiercing = false;
    private final java.util.Set<Integer> alreadyHit = new java.util.HashSet<>();

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public BulletEntity(Level level, LivingEntity shooter, float damage, int pierce, boolean armorPiercing, AmmoType type) {
        super(ModEntities.BULLET.get(), shooter, level);
        this.damage = damage;
        this.pierceLeft = pierce;
        this.armorPiercing = armorPiercing;
        this.entityData.set(TYPE, type.ordinal());
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 0);
    }

    public AmmoType ammoType() {
        int i = entityData.get(TYPE);
        return i >= 0 && i < AmmoType.values().length ? AmmoType.values()[i] : AmmoType.STANDARD;
    }

    /** Une balle perforante ne touche pas deux fois le même ennemi. */
    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !alreadyHit.contains(target.getId());
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.AMMO_9MM.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0; // trajectoire droite
    }

    @Override
    public void tick() {
        Vec3 before = position();
        super.tick();
        AmmoType type = ammoType();
        if (level().isClientSide) {
            // Traçante : une traînée lumineuse le long de la trajectoire, visible par tout le monde
            if (type == AmmoType.TRACER || type == AmmoType.DRAGON_BREATH) {
                Vec3 after = position();
                double length = after.distanceTo(before);
                int steps = Math.max(1, (int) (length / 0.2));
                Entity owner = getOwner();
                for (int i = 0; i < steps; i++) {
                    Vec3 p = before.lerp(after, i / (double) steps);
                    // Rien juste devant le tireur : la traînée ne doit pas masquer sa vue
                    if (owner != null && owner.getEyePosition().distanceToSqr(p) < 2.5 * 2.5) {
                        continue;
                    }
                    level().addParticle(type == AmmoType.TRACER ? ModParticles.TRACER.get() : ParticleTypes.FLAME,
                            p.x, p.y, p.z, 0, 0, 0);
                }
            }
        } else if (tickCount > (type == AmmoType.DRAGON_BREATH ? 8 : MAX_LIFE_TICKS)) {
            discard(); // le souffle du dragon porte très peu loin
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

        AmmoType type = ammoType();
        if (type == AmmoType.HOLLOW_POINT && target instanceof LivingEntity living) {
            finalDamage *= living.getArmorValue() > 0 ? 0.6f : 1.35f; // très efficace sans armure
        }
        DamageSource source = level.damageSources().source(
                armorPiercing ? ModDamageTypes.BULLET_AP : ModDamageTypes.BULLET, this, getOwner());
        if (target.hurt(source, finalDamage)) {
            applyTypeEffect(level, target, finalDamage);
            // Supprime l'invincibilité temporaire : sinon les plombs du fusil à pompe
            // et les rafales ne toucheraient qu'une fois sur deux.
            target.invulnerableTime = 0;
        }
        if (pierceLeft > 0) {
            // Balle perforante (Barrett) : elle continue sa route
            pierceLeft--;
            alreadyHit.add(target.getId());
            damage *= 0.8f;
        } else {
            discard();
        }
    }

    /** Effet du type de munition sur l'ennemi touché. */
    private void applyTypeEffect(ServerLevel level, Entity target, float dealt) {
        LivingEntity living = target instanceof LivingEntity l ? l : null;
        switch (ammoType()) {
            case EXPLOSIVE -> level.explode(this, getX(), getY(), getZ(), 1.2f, Level.ExplosionInteraction.NONE);
            case INCENDIARY, DRAGON_BREATH -> target.igniteForSeconds(4);
            case RUBBER -> {
                if (living != null) {
                    Vec3 push = getDeltaMovement().normalize();
                    living.knockback(1.4, -push.x, -push.z);
                }
            }
            case MARKER -> { if (living != null) living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200), getOwner()); }
            case VENOM -> { if (living != null) living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), getOwner()); }
            case CRYO -> {
                if (living != null) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), getOwner());
                    living.setTicksFrozen(Math.max(living.getTicksFrozen(), living.getTicksRequiredToFreeze() + 30));
                }
            }
            case ELECTRIC -> {
                if (living != null) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4), getOwner());
                }
                // L'éclair saute sur l'ennemi le plus proche
                level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4),
                                e -> e != target && e != getOwner() && e.isAlive())
                        .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(target)))
                        .ifPresent(next -> {
                            next.hurt(level.damageSources().source(ModDamageTypes.BULLET, this, getOwner()), dealt * 0.5f);
                            Vec3 a = target.position().add(0, target.getBbHeight() / 2, 0);
                            Vec3 b = next.position().add(0, next.getBbHeight() / 2, 0);
                            for (int i = 0; i <= 8; i++) {
                                Vec3 p = a.lerp(b, i / 8.0);
                                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
                            }
                        });
                level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.3f, 2.0f);
            }
            default -> { }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel level) {
            BlockPos pos = result.getBlockPos();
            AmmoType type = ammoType();
            if (type == AmmoType.EXPLOSIVE) {
                Vec3 h = result.getLocation();
                level.explode(this, h.x, h.y, h.z, 1.2f, Level.ExplosionInteraction.NONE);
            } else if ((type == AmmoType.INCENDIARY || type == AmmoType.DRAGON_BREATH)
                    && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                BlockPos fire = pos.relative(result.getDirection());
                if (net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(level, fire, result.getDirection())) {
                    level.setBlockAndUpdate(fire, net.minecraft.world.level.block.BaseFireBlock.getState(level, fire));
                }
            }
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
        tag.putInt("Pierce", pierceLeft);
        tag.putBoolean("ArmorPiercing", armorPiercing);
        tag.putInt("AmmoType", entityData.get(TYPE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        pierceLeft = tag.getInt("Pierce");
        armorPiercing = tag.getBoolean("ArmorPiercing");
        entityData.set(TYPE, tag.getInt("AmmoType"));
    }
}


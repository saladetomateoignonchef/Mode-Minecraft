package fr.modeminecraft.nuclear;

import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Bombe larguée par le B-29 : chute avec traînée aérodynamique (le nez suit la trajectoire), détonation à l'impact. */
public class BombEntity extends Projectile {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(BombEntity.class, EntityDataSerializers.INT);
    private int life;
    private int launcher = -1;
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> BOMB_TICKET = net.minecraft.server.level.TicketType.create(
            "modeminecraft_bomb", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 60);

    public BombEntity(EntityType<? extends BombEntity> type, Level level) {
        super(type, level);
    }

    public BombEntity(Level level, BombItem.Kind kind, LivingEntity owner, Entity launcher, Vec3 pos, Vec3 velocity) {
        this(ModEntities.BOMB.get(), level);
        entityData.set(KIND, kind.ordinal());
        setOwner(owner);
        this.launcher = launcher == null ? -1 : launcher.getId();
        setPos(pos.x, pos.y, pos.z);
        setDeltaMovement(velocity);
        updateRotation();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, 0);
    }

    public BombItem.Kind kind() {
        return BombItem.Kind.values()[Math.floorMod(entityData.get(KIND), BombItem.Kind.values().length)];
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 512 * 512;
    }

    @Override
    public void tick() {
        super.tick();
        life++;
        Vec3 v = getDeltaMovement();
        // gravité et traînée (la bombe garde l'élan horizontal de l'avion puis s'incline)
        v = new Vec3(v.x * 0.992, Math.max(-3.2, v.y - 0.045) * 0.998, v.z * 0.992);
        setDeltaMovement(v);
        if (level() instanceof ServerLevel sl && life % 10 == 1) {
            var cp = chunkPosition();                                              // la bombe garde sa zone de chute chargée
            sl.getChunkSource().addRegionTicket(BOMB_TICKET, cp, 3, cp);
        }
        if (!level().isClientSide) {
            if (life == 8) {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.BOMB_WHISTLE.get(), SoundSource.NEUTRAL, 6f, kind() == BombItem.Kind.NUKE ? 0.7f : 1f);
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS || getY() < level().getMinBuildHeight()) {
                Vec3 at = hit.getType() == HitResult.Type.MISS ? position() : hit.getLocation();
                discard();
                Bombs.detonate((ServerLevel) level(), kind(), at, getOwner());
                return;
            }
            if (life > 1200) {
                discard();
                return;
            }
        } else if (life % 2 == 0) {
            level().addParticle(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 0, 0, 0);
        }
        Vec3 p = position().add(getDeltaMovement());
        setPos(p.x, p.y, p.z);
        updateRotation();
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (life < 6) {
            return false;                                                         // sort de la soute
        }
        Entity l = launcher < 0 ? null : level().getEntity(launcher);
        if (l != null && (e == l || l.hasPassenger(e) || e instanceof fr.modeminecraft.entity.military.MilPart mp && mp.getParent() == l)) {
            return false;
        }
        return super.canHitEntity(e) || e instanceof fr.modeminecraft.entity.military.MilPart;
    }

    /** Orientation du nez (degrés) pour le rendu. */
    public float pitchDeg() {
        Vec3 v = getDeltaMovement();
        return (float) (Mth.atan2(-v.y, Math.hypot(v.x, v.z)) * Mth.RAD_TO_DEG);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(KIND, tag.getInt("Kind"));
        life = tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", entityData.get(KIND));
        tag.putInt("Life", life);
    }
}

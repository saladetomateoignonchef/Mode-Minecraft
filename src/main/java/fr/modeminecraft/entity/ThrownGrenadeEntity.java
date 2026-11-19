package fr.modeminecraft.entity;

import fr.modeminecraft.item.explosive.Detonations;
import fr.modeminecraft.item.explosive.GrenadeItem;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Grenade lancée. Rebonds (cahier des charges) : V' = V - 2(V·N)N, puis X/Z x0,55 et Y x0,45 ;
 * sous 0,08 bloc/tick elle s'immobilise. Grenades collantes (Semtex, détection) : elles se collent au premier contact.
 * Leurre et détection restent ensuite actifs un moment (voir Detonations).
 */
public class ThrownGrenadeEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(ThrownGrenadeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STUCK = SynchedEntityData.defineId(ThrownGrenadeEntity.class, EntityDataSerializers.BOOLEAN);

    private int fuse;
    private boolean resting;
    private int stuckTo = -1;
    private Vec3 stuckOffset = Vec3.ZERO;
    /** Leurre / détection : temps restant une fois activé (-1 = pas encore activé). */
    private int activeTicks = -1;

    public ThrownGrenadeEntity(EntityType<? extends ThrownGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public ThrownGrenadeEntity(Level level, LivingEntity thrower, GrenadeItem.Kind kind, int fuse) {
        super(ModEntities.THROWN_GRENADE.get(), thrower, level);
        entityData.set(KIND, kind.ordinal());
        this.fuse = fuse;
        setItem(new net.minecraft.world.item.ItemStack(itemFor(kind)));
    }

    public static Item itemFor(GrenadeItem.Kind kind) {
        return fr.modeminecraft.registry.ModItems.ITEMS.getEntries().stream().<Item>map(h -> h.get())
                .filter(i -> i instanceof GrenadeItem g && g.kind() == kind).findFirst()
                .orElse(fr.modeminecraft.registry.ModItems.FRAG_GRENADE.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0).define(STUCK, false);
    }

    public GrenadeItem.Kind kind() {
        return GrenadeItem.Kind.values()[Math.max(0, Math.min(GrenadeItem.Kind.values().length - 1, entityData.get(KIND)))];
    }

    public boolean isStuckOrResting() {
        return entityData.get(STUCK) || resting;
    }

    @Override
    protected Item getDefaultItem() {
        return fr.modeminecraft.registry.ModItems.FRAG_GRENADE.get();
    }

    @Override
    protected double getDefaultGravity() {
        return isStuckOrResting() ? 0 : 0.045;
    }

    @Override
    public void tick() {
        if (entityData.get(STUCK)) {
            // Collée : elle suit l'entité (ou reste sur le bloc)
            Entity host = stuckTo >= 0 ? level().getEntity(stuckTo) : null;
            if (host != null) {
                setPos(host.position().add(stuckOffset));
            }
            setDeltaMovement(Vec3.ZERO);
            baseTick();
        } else if (resting) {
            setDeltaMovement(Vec3.ZERO);
            baseTick();
        } else {
            super.tick();
        }
        GrenadeItem.Kind kind = kind();
        if (level().isClientSide) {
            if (kind == GrenadeItem.Kind.SEMTEX && entityData.get(STUCK) && tickCount % 6 < 3) {
                level().addParticle(new DustParticleOptions(new Vector3f(1, 0.05f, 0.05f), 0.6f), getX(), getY() + 0.15, getZ(), 0, 0, 0);
            }
            if (kind == GrenadeItem.Kind.MOLOTOV) {
                level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.2, getZ(), 0, 0.01, 0);
            }
            return;
        }
        if (activeTicks >= 0) {
            if (--activeTicks <= 0 || !Detonations.tickActive(this, kind, activeTicks)) {
                discard();
            }
            return;
        }
        boolean fuseRunning = !kind.sticky || entityData.get(STUCK);
        if (kind.sticky && entityData.get(STUCK) && kind == GrenadeItem.Kind.SEMTEX && fuse % 10 == 0) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.GRENADE_BEEP.get(), SoundSource.PLAYERS, 0.8f, 1.0f + (50 - fuse) / 60f);
        }
        if (kind.fuse > 0 && fuseRunning && --fuse <= 0) {
            explode();
        }
        if (tickCount > 1200) {
            discard();
        }
    }

    private void explode() {
        int active = Detonations.detonate(this, kind());
        if (active > 0) {
            activeTicks = active; // leurre / détection : ils restent actifs
        } else {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) {
            return;
        }
        GrenadeItem.Kind kind = kind();
        if (kind.impact) {
            explode();
        } else if (kind.sticky) {
            stick(result.getEntity());
        } else {
            setDeltaMovement(getDeltaMovement().scale(-0.25));
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide) {
            return;
        }
        GrenadeItem.Kind kind = kind();
        if (kind.impact) {
            explode();
            return;
        }
        if (kind.sticky) {
            setPos(result.getLocation());
            stick(null);
            return;
        }
        // Rebond : réflexion sur la face touchée, puis perte d'énergie
        Vec3 n = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        Vec3 v = getDeltaMovement();
        Vec3 r = v.subtract(n.scale(2 * v.dot(n)));
        r = new Vec3(r.x * 0.55, r.y * 0.45, r.z * 0.55);
        if (r.length() < 0.08) {
            resting = true;
            setDeltaMovement(Vec3.ZERO);
            setPos(result.getLocation().add(n.scale(0.1)));
        } else {
            setDeltaMovement(r);
            hasImpulse = true;
            setPos(result.getLocation().add(n.scale(0.05)).subtract(r));
            if (v.length() > 0.2) {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.GRENADE_BOUNCE.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
            }
        }
    }

    private void stick(Entity host) {
        entityData.set(STUCK, true);
        setDeltaMovement(Vec3.ZERO);
        if (host != null) {
            stuckTo = host.getId();
            stuckOffset = position().subtract(host.position());
        }
        level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuse", fuse);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fuse = tag.getInt("Fuse");
    }
}

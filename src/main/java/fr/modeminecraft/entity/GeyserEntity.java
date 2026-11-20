package fr.modeminecraft.entity;

import fr.modeminecraft.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Geyser d'une bouche d'incendie détruite : colonne d'eau de 5 blocs pendant 8 s, qui soulève et éteint. */
public class GeyserEntity extends Entity {
    private static final int LIFE = 160;

    public GeyserEntity(EntityType<? extends GeyserEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public GeyserEntity(Level level, double x, double y, double z) {
        this(ModEntities.GEYSER.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        float k = 1 - tickCount / (float) LIFE;
        sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.5, getZ(), 30, 0.15, 2.0 * k, 0.15, 0.4);
        sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 5 * k, getZ(), 12, 0.6, 0.2, 0.6, 0);
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 4.5 * k, getZ(), 2, 0.3, 0.1, 0.3, 0.02);
        if (tickCount % 20 == 0) {
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 1.2f, 1.4f);
        }
        for (Entity e : sl.getEntities(this, new AABB(getX() - 0.8, getY(), getZ() - 0.8, getX() + 0.8, getY() + 5 * k, getZ() + 0.8))) {
            e.setDeltaMovement(e.getDeltaMovement().x, Math.max(e.getDeltaMovement().y, 0.55 * k + 0.1), e.getDeltaMovement().z);
            e.hurtMarked = true;
            e.clearFire();
            e.fallDistance = 0;
        }
        if (tickCount > LIFE) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}

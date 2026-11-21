package fr.modeminecraft.nuclear;

import fr.modeminecraft.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Champignon atomique : boule de feu, colonne et chapeau qui montent jusqu'à 180 blocs (rendu par volumes dans
 * NukeCloudRenderer, visible de très loin) ; il se dissipe au bout de 90 s.
 */
public class NukeCloudEntity extends Entity {
    public static final int LIFE = 1800;

    public NukeCloudEntity(EntityType<? extends NukeCloudEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public NukeCloudEntity(Level level, Vec3 at) {
        this(ModEntities.NUKE_CLOUD.get(), level);
        setPos(at.x, at.y, at.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (tickCount > LIFE) {
                discard();
            }
            return;
        }
        // fumées et braises autour de la colonne et du chapeau
        float h = capHeight(tickCount);
        for (int i = 0; i < 6; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double rr = random.nextDouble();
            boolean cap = random.nextBoolean();
            double radius = cap ? capRadius(tickCount) * (0.6 + rr * 0.5) : 10 + rr * 6;
            double y = cap ? getY() + h + (random.nextDouble() - 0.4) * capThickness(tickCount) : getY() + random.nextDouble() * h;
            level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, getX() + Math.cos(a) * radius, y,
                    getZ() + Math.sin(a) * radius, 0, 0.05, 0);
        }
        if (tickCount < 400) {
            for (int i = 0; i < 4; i++) {
                level().addAlwaysVisibleParticle(ParticleTypes.LAVA, true, getX() + (random.nextDouble() - 0.5) * 30, getY() + random.nextDouble() * 10,
                        getZ() + (random.nextDouble() - 0.5) * 30, 0, 0.1, 0);
            }
        }
    }

    /** Hauteur du chapeau (blocs) : monte vite puis plafonne à 150 (sommet ~180). */
    public static float capHeight(float t) {
        return 150f * (1 - (float) Math.exp(-t / 140f)) + 6;
    }

    public static float capRadius(float t) {
        return 12 + 38 * (1 - (float) Math.exp(-t / 160f));
    }

    public static float capThickness(float t) {
        return 10 + 22 * (1 - (float) Math.exp(-t / 160f));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(60, 200, 60).move(0, 100, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", tickCount);
    }
}

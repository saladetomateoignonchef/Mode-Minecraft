package fr.modeminecraft.entity;

import fr.modeminecraft.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Zone d'effet posée au sol : fumée (bloque la vue des monstres), gaz toxique (sans masque à gaz) ou flaque de feu (molotov).
 */
public class AreaCloudEntity extends Entity {
    public enum Type { SMOKE, GAS, FIRE }

    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(AreaCloudEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(AreaCloudEntity.class, EntityDataSerializers.FLOAT);
    private int life;
    private Entity owner;

    public AreaCloudEntity(EntityType<? extends AreaCloudEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public AreaCloudEntity(Level level, Type type, Vec3 pos, float radius, int life, Entity owner) {
        this(ModEntities.AREA_CLOUD.get(), level);
        setPos(pos);
        entityData.set(TYPE, type.ordinal());
        entityData.set(RADIUS, radius);
        this.life = life;
        this.owner = owner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0).define(RADIUS, 5f);
    }

    public Type cloudType() {
        return Type.values()[entityData.get(TYPE)];
    }

    public float radius() {
        return entityData.get(RADIUS);
    }

    /** Ce point est-il dans le nuage ? */
    public boolean contains(Vec3 p) {
        double dy = p.y - getY();
        return dy > -1 && dy < (cloudType() == Type.SMOKE ? radius() : 2.5) && horizontal(p) < radius();
    }

    private double horizontal(Vec3 p) {
        return Math.sqrt((p.x - getX()) * (p.x - getX()) + (p.z - getZ()) * (p.z - getZ()));
    }

    /** Le segment a -> b traverse-t-il le nuage de fumée ? */
    public boolean blocksSight(Vec3 a, Vec3 b) {
        for (int i = 0; i <= 10; i++) {
            if (contains(a.lerp(b, i / 10.0))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        float r = radius();
        Type type = cloudType();
        if (level().isClientSide) {
            int n = type == Type.SMOKE ? 14 : 6;
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = Math.sqrt(random.nextDouble()) * r;
                double x = getX() + Math.cos(a) * d, z = getZ() + Math.sin(a) * d;
                switch (type) {
                    case SMOKE -> level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x,
                            getY() + random.nextDouble() * r * 0.8, z, 0, 0.005, 0);
                    case GAS -> level().addParticle(new DustParticleOptions(new Vector3f(0.7f, 0.85f, 0.2f), 2.5f), x,
                            getY() + random.nextDouble() * 1.2, z, 0, 0, 0);
                    case FIRE -> level().addParticle(random.nextFloat() < 0.3f ? ParticleTypes.LAVA : ParticleTypes.FLAME, x,
                            getY() + 0.1, z, 0, 0.03, 0);
                }
            }
            return;
        }
        if (--life <= 0) {
            discard();
            return;
        }
        AABB box = new AABB(position(), position()).inflate(r, r, r);
        switch (type) {
            case SMOKE -> {
                // Les monstres perdent leur cible s'ils ne la voient plus à travers la fumée
                for (Mob m : level().getEntitiesOfClass(Mob.class, box.inflate(24))) {
                    LivingEntity t = m.getTarget();
                    if (t != null && (contains(t.getEyePosition()) || blocksSight(m.getEyePosition(), t.getEyePosition()))) {
                        m.setTarget(null);
                    }
                }
            }
            case GAS -> {
                if (tickCount % 20 == 0) {
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box)) {
                        boolean mask = e.getItemBySlot(EquipmentSlot.HEAD).is(fr.modeminecraft.registry.ModItems.GAS_MASK.get());
                        if (!mask && contains(e.position())) {
                            e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100));
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                        }
                    }
                }
            }
            case FIRE -> {
                if (tickCount % 10 == 0) {
                    var src = level().damageSources().source(fr.modeminecraft.registry.ModDamageTypes.BULLET_AP, this, owner);
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box)) {
                        if (contains(e.position())) {
                            e.igniteForSeconds(3);
                            e.hurt(src, 2.0f);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        life = tag.getInt("Life");
        entityData.set(TYPE, tag.getInt("Type"));
        entityData.set(RADIUS, tag.getFloat("Radius"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", life);
        tag.putInt("Type", entityData.get(TYPE));
        tag.putFloat("Radius", radius());
    }
}

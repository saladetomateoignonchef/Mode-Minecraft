package fr.modeminecraft.nuclear;

import fr.modeminecraft.entity.AreaCloudEntity;
import fr.modeminecraft.entity.military.MilHelpers;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Effets des bombes d'aviation à l'impact. */
public final class Bombs {
    public static void detonate(ServerLevel level, BombItem.Kind kind, Vec3 at, Entity owner) {
        switch (kind) {
            case HE -> he(level, at, owner);
            case NAPALM -> napalm(level, at, owner);
            case NUKE -> NuclearExplosion.start(level, BlockPos.containing(at), owner);
        }
    }

    /** AN-M65 : explosion de 14 blocs de rayon, cratère profond, véhicules blindés pulvérisés, pas de radiation. */
    static void he(ServerLevel level, Vec3 at, Entity owner) {
        crater(level, BlockPos.containing(at), 8, 1.6);
        level.explode(owner, null, null, at.x, at.y, at.z, 7f, false, Level.ExplosionInteraction.TNT);
        hurtAround(level, at, 14, 70, owner);
        MilHelpers.damageVehicles(level, at, 14, 700);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1, at.z, 4, 3, 2, 3, 0);
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, at.x, at.y + 2, at.z, 60, 4, 4, 4, 0.06);
        level.playSound(null, at.x, at.y, at.z, ModSounds.HUGE_EXPLOSION.get(), SoundSource.NEUTRAL, 16f, 0.9f);
        fr.modeminecraft.entity.bot.CivilianLife.panic(level, at, 90, 400);
    }

    /** AN-M76 : explosion moyenne (8 blocs) et nappe de feu liquide sur 22 blocs pendant 45 secondes. */
    static void napalm(ServerLevel level, Vec3 at, Entity owner) {
        level.explode(owner, null, null, at.x, at.y, at.z, 3.5f, true, Level.ExplosionInteraction.TNT);
        hurtAround(level, at, 8, 30, owner);
        int r = 22;
        BlockPos c = BlockPos.containing(at);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r || level.random.nextFloat() > 0.6f) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, c.getX() + dx, c.getZ() + dz);
                m.set(c.getX() + dx, y, c.getZ() + dz);
                if (Math.abs(y - c.getY()) < 14 && level.getBlockState(m).isAir()) {
                    level.setBlock(m, Blocks.FIRE.defaultBlockState(), 3);
                }
            }
        }
        level.addFreshEntity(new AreaCloudEntity(level, AreaCloudEntity.Type.FIRE, at, r, 900, owner));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r))) {
            if (e.position().distanceTo(at) < r) {
                e.igniteForSeconds(12);
            }
        }
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 2, at.z, 500, 10, 2, 10, 0.12);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 4, at.z, 120, 8, 4, 8, 0.05);
        level.playSound(null, at.x, at.y, at.z, ModSounds.HUGE_EXPLOSION.get(), SoundSource.NEUTRAL, 10f, 1.3f);
        fr.modeminecraft.entity.bot.CivilianLife.panic(level, at, 90, 400);
    }

    /** Cratère en cuvette (rayon r, profondeur relative k) : les blocs indestructibles restent. */
    static void crater(ServerLevel level, BlockPos c, int r, double k) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -r; dy <= r; dy++) {
                    double d = Math.sqrt(dx * dx + dz * dz + dy * dy * k * k);
                    if (d > r + level.random.nextFloat() * 0.8) {
                        continue;
                    }
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    clear(level, m);
                }
            }
        }
    }

    static boolean clear(ServerLevel level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        if (!st.isAir() && st.getDestroySpeed(level, p) >= 0 && !st.is(Blocks.BEDROCK)) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            return true;
        }
        return false;
    }

    static void hurtAround(ServerLevel level, Vec3 at, double r, float dmg, Entity src) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r))) {
            double d = e.position().distanceTo(at);
            if (d < r) {
                e.hurt(src != null ? level.damageSources().explosion(src, src) : level.damageSources().explosion(null, null), (float) (dmg * (1 - d / r)));
            }
        }
    }

    private Bombs() {}
}

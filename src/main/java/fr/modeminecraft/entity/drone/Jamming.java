package fr.modeminecraft.entity.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guerre électronique :
 * - fusil brouilleur (EMP) : faisceau invisible de 80 blocs, tout drone touché atterrit en douceur, désactivé 15 s ;
 * - tours de brouillage : dôme de 50 blocs (interférences sur l'écran du pilote), perte de contrôle à moins de 15 blocs.
 */
public final class Jamming {
    public static final double TOWER_FIELD = 50, TOWER_CONTROL = 15, EMP_RANGE = 80;
    public static final int EMP_TICKS = 300;
    private static final Map<ResourceKey<Level>, Set<BlockPos>> TOWERS = new ConcurrentHashMap<>();

    public static void registerTower(Level level, BlockPos pos) {
        TOWERS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    /** Nouveau monde / arrêt du serveur : on oublie les tours de la partie précédente. */
    public static void clear() {
        TOWERS.clear();
    }

    public static void removeTower(Level level, BlockPos pos) {
        Set<BlockPos> set = TOWERS.get(level.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    /** Distance à la tour de brouillage la plus proche (ou +infini). */
    public static double towerDistance(Level level, Vec3 pos) {
        Set<BlockPos> set = TOWERS.get(level.dimension());
        double best = Double.MAX_VALUE;
        if (set != null) {
            for (BlockPos p : set) {
                // tour disparue (détruite, autre monde chargé avant) : on l'oublie, elle ne brouille plus rien
                if (level.isLoaded(p) && !level.getBlockState(p).is(fr.modeminecraft.registry.ModBlocks.JAMMER_TOWER.get())) {
                    set.remove(p);
                    continue;
                }
                best = Math.min(best, Math.sqrt(p.getCenter().distanceToSqr(pos)));
            }
        }
        return best;
    }

    public static boolean lostControl(Level level, Vec3 pos) {
        return towerDistance(level, pos) < TOWER_CONTROL || fr.modeminecraft.scenario.Gadgets.jammed(level, pos);   // brouilleur portatif
    }

    /** Interférences (0 = aucune, 1 = perte de contrôle) pour l'écran du pilote. */
    public static void tickDrone(DroneEntity drone, ServerLevel level) {
        if (drone.tickCount % 10 != 0) {
            return;
        }
        double d = towerDistance(level, drone.position());
        float noise = d >= TOWER_FIELD ? 0 : (float) Math.min(1, (TOWER_FIELD - d) / (TOWER_FIELD - TOWER_CONTROL));
        drone.setInterference(drone.jammed() ? 1 : noise);
    }

    /** Tir du fusil brouilleur : le premier drone sur la ligne de visée (80 blocs) est brouillé. */
    public static void empShot(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(EMP_RANGE));
        HitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double max = wall.getType() == HitResult.Type.MISS ? EMP_RANGE : wall.getLocation().distanceTo(eye);
        DroneEntity best = null;
        double bestT = Double.MAX_VALUE;
        for (DroneEntity d : level.getEntitiesOfClass(DroneEntity.class, new AABB(eye, end).inflate(2))) {
            Vec3 to = d.getBoundingBox().getCenter().subtract(eye);
            double t = to.dot(look);
            if (t < 0 || t > max) {
                continue;
            }
            double off = to.subtract(look.scale(t)).length();
            if (off < 1.6 + d.getBbWidth() && t < bestT) {
                best = d;
                bestT = t;
            }
        }
        // l'onde est invisible : seulement quelques étincelles là où elle frappe
        Vec3 hit = best != null ? best.position() : eye.add(look.scale(max));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y + 0.3, hit.z, 25, 0.5, 0.5, 0.5, 0.3);
        if (best != null) {
            best.jam(EMP_TICKS);
        }
    }

    private Jamming() {}
}

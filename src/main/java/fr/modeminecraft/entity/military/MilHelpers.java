package fr.modeminecraft.entity.military;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Petits utilitaires partagés (frappes, mines). */
public final class MilHelpers {
    /** Souffle sur les véhicules militaires du rayon (décroît avec la distance). */
    public static void damageVehicles(ServerLevel level, Vec3 at, double r, float dmg) {
        for (MilVehicleEntity v : level.getEntitiesOfClass(MilVehicleEntity.class, new AABB(at, at).inflate(r))) {
            double d = v.position().distanceTo(at);
            if (d < r) {
                v.blastHit(at, (float) (dmg * (1 - d / r)));
            }
        }
    }

    private MilHelpers() {}
}

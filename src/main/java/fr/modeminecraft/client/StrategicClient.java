package fr.modeminecraft.client;

import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.WeakHashMap;

/** Complexes lance-missiles : animation de relèvement des tubes / missiles au déploiement. */
public final class StrategicClient {
    private static final Map<MilVehicleEntity, float[]> ERECT = new WeakHashMap<>();

    /** Élévation des conteneurs-lanceurs (degrés) : 0 en transport, 85-90° une fois déployé. */
    public static float erect(MilVehicleEntity v, float partial) {
        float[] a = ERECT.computeIfAbsent(v, k -> new float[]{0, 0, -1});
        if (a[2] != v.tickCount) {
            a[2] = v.tickCount;
            a[1] = a[0];
            float max = v.type() == MilType.ISKANDER ? 85 : 90;
            a[0] = Mth.approach(a[0], v.is(MilVehicleEntity.DEPLOYED) ? max : 0, 1.2f);
        }
        return Mth.lerp(partial, a[1], a[0]);
    }

    private StrategicClient() {}
}

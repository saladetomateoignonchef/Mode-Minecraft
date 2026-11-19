package fr.modeminecraft.item.gun;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Vent du monde : il dévie les balles (surtout à longue distance).
 * Il change lentement de direction et de force, et souffle plus fort sous la pluie et l'orage.
 * Le calcul ne dépend que du temps du monde et de la météo : le client et le serveur trouvent le même vent.
 */
public final class Wind {
    /** Accélération latérale d'une balle (blocs / tick²) pour 1 m/s de vent. */
    public static final double ACCEL_PER_MS = 0.0004;

    /** Vent horizontal en m/s (direction + force). */
    public static Vec3 get(Level level) {
        double t = level.getGameTime() / 2400.0; // le vent évolue sur quelques minutes
        double angle = smooth(t * 0.5, 1) * Math.PI * 2;
        double strength = 0.5 + 3.5 * (0.5 + 0.5 * smooth(t, 2));          // 0,5 à 4 m/s par beau temps
        strength *= 1 + level.getRainLevel(1) * 1.2 + level.getThunderLevel(1) * 1.5;
        return new Vec3(Math.cos(angle) * strength, 0, Math.sin(angle) * strength);
    }

    /** Bruit lisse entre -1 et 1. */
    private static double smooth(double x, int seed) {
        long i = (long) Math.floor(x);
        double f = x - i;
        f = f * f * (3 - 2 * f);
        return Mth.lerp(f, hash(i, seed), hash(i + 1, seed));
    }

    private static double hash(long i, int seed) {
        long h = i * 0x9E3779B97F4A7C15L + seed * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h & 0xFFFFFF) / (double) 0xFFFFFF * 2 - 1;
    }

    private Wind() {}
}

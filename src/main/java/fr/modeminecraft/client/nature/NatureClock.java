package fr.modeminecraft.client.nature;

import net.minecraft.util.Mth;

/**
 * Calendrier naturel déterministe : tempêtes de sable, vent et saisons de brouillard se déduisent de l'heure du monde (identique pour tous les
 * joueurs, sans paquet réseau). Les jours à tempête de sable sont tirés par une fonction de hachage du numéro du jour.
 */
public final class NatureClock {
    private NatureClock() {}

    /** Nombre pseudo-aléatoire stable dans [0, 1[ pour un entier donné (mélange de bits de type splitmix). */
    public static double hash(long n) {
        long z = n * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 11) * (1.0 / (1L << 53));
    }

    /** Intensité (0 à 1) d'une tempête de sable à cette heure : environ un jour sur trois, 3 500 à 6 000 ticks, avec 500 ticks de montée et de retombée. */
    public static float sandstorm(long dayTime) {
        long day = Math.floorDiv(dayTime, 24000L), tod = Math.floorMod(dayTime, 24000L);
        if (hash(day * 7 + 1) > 0.34) {
            return 0;
        }
        long start = 1000 + (long) (hash(day * 7 + 2) * 12000), len = 3500 + (long) (hash(day * 7 + 3) * 2500);
        long t = tod - start;
        if (t < 0 || t > len) {
            return 0;
        }
        return Mth.clamp(Math.min(t, len - t) / 500f, 0f, 1f);
    }

    /** Brouillard matinal : de 23 000 à 2 500 (aube), plein pendant la première heure, puis qui se dissipe. */
    public static float morningFog(long dayTime) {
        long tod = Math.floorMod(dayTime, 24000L);
        long t = tod >= 22500 ? tod - 22500 : tod + 1500;                  // 0 à 4 000
        if (t > 4000) {
            return 0;
        }
        return Mth.clamp(Math.min(t / 500f, (4000 - t) / 2000f), 0f, 1f);
    }

    /** Brume de chaleur : ticks 5 000 à 10 000, maximale à 7 500. */
    public static float heatHaze(long dayTime) {
        long tod = Math.floorMod(dayTime, 24000L);
        if (tod < 5000 || tod > 10000) {
            return 0;
        }
        return (float) Math.sin((tod - 5000) / 5000.0 * Math.PI);
    }

    /** Direction du vent (radians), lentement variable d'un jour à l'autre et au fil de la journée. */
    public static double windAngle(long dayTime) {
        long day = Math.floorDiv(dayTime, 24000L);
        return hash(day * 13 + 5) * Math.PI * 2 + Math.sin(dayTime / 6000.0) * 0.5;
    }
}

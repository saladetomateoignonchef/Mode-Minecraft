package fr.modeminecraft.item.gun;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Statistiques et progression d'une arme (stockées dans l'arme elle-même : elles la suivent si on la donne).
 * Expérience : +10 par élimination, +5 de plus si tir à la tête, +10 de plus pour un gros monstre (50 PV ou plus).
 * Niveaux 1 à 5 : chaque niveau au-dessus du 1er donne -4 % de recul, -3 % de dispersion et -3 % de temps de
 * rechargement ; les niveaux 3 et 5 débloquent des skins (Carbone, Diamant).
 */
public record GunProgress(int kills, int headshots, int shots, int xp) {
    public static final GunProgress EMPTY = new GunProgress(0, 0, 0, 0);
    public static final int[] LEVEL_XP = {0, 100, 300, 600, 1000};
    public static final int MAX_LEVEL = LEVEL_XP.length;

    public static final Codec<GunProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("kills", 0).forGetter(GunProgress::kills),
            Codec.INT.optionalFieldOf("headshots", 0).forGetter(GunProgress::headshots),
            Codec.INT.optionalFieldOf("shots", 0).forGetter(GunProgress::shots),
            Codec.INT.optionalFieldOf("xp", 0).forGetter(GunProgress::xp)
    ).apply(i, GunProgress::new));

    public static final StreamCodec<ByteBuf, GunProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GunProgress::kills, ByteBufCodecs.VAR_INT, GunProgress::headshots,
            ByteBufCodecs.VAR_INT, GunProgress::shots, ByteBufCodecs.VAR_INT, GunProgress::xp, GunProgress::new);

    public int level() {
        int level = 1;
        for (int i = 1; i < LEVEL_XP.length; i++) {
            if (xp >= LEVEL_XP[i]) {
                level = i + 1;
            }
        }
        return level;
    }

    /** Expérience nécessaire pour le niveau suivant (ou -1 au niveau max). */
    public int nextLevelXp() {
        int level = level();
        return level >= MAX_LEVEL ? -1 : LEVEL_XP[level];
    }

    public GunProgress withShot() {
        return new GunProgress(kills, headshots, shots + 1, xp);
    }

    public GunProgress withKill(boolean headshot, boolean big) {
        return new GunProgress(kills + 1, headshots + (headshot ? 1 : 0), shots, xp + 10 + (headshot ? 5 : 0) + (big ? 10 : 0));
    }

    /** Bonus du niveau (multiplicateurs). */
    public static float recoilMul(int level) { return 1 - 0.04f * (level - 1); }
    public static float spreadMul(int level) { return 1 - 0.03f * (level - 1); }
    public static float reloadMul(int level) { return 1 - 0.03f * (level - 1); }
}

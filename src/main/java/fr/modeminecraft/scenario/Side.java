package fr.modeminecraft.scenario;

import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.military.MilType;

/** Les deux superpuissances de la Course à l'arme nucléaire. */
public enum Side {
    US("us", Faction.GARRISON, Faction.SOVIET, "base", MilType.Side.US, 0xFF3C5A9A),
    SOVIET("soviet", Faction.SOVIET, Faction.GARRISON, "soviet", MilType.Side.SOVIET, 0xFFB02020);

    public final String id;
    /** Armée alliée, armée ennemie à vue. */
    public final Faction ally, enemy;
    /** Variante de ville de la base principale. */
    public final String baseVariant;
    public final MilType.Side vehicles;
    public final int color;

    Side(String id, Faction ally, Faction enemy, String baseVariant, MilType.Side vehicles, int color) {
        this.id = id;
        this.ally = ally;
        this.enemy = enemy;
        this.baseVariant = baseVariant;
        this.vehicles = vehicles;
        this.color = color;
    }

    public Side opposite() {
        return this == US ? SOVIET : US;
    }

    public static Side byId(String id) {
        for (Side s : values()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** Camp d'une armée (null : ni l'une ni l'autre). */
    public static Side of(Faction f) {
        return f == Faction.GARRISON ? US : f == Faction.SOVIET ? SOVIET : null;
    }
}

package fr.modeminecraft.scenario;

/**
 * Échelle des grades (réputation requise) : logement attribué, réduction à l'infirmerie et à la cantine de la base,
 * accès aux quartiers réservés et aux véhicules.
 */
public enum Grade {
    SOLDIER(0, 0f, "Pvt", "Ряд"),
    CORPORAL(150, 0.15f, "Cpl", "Ефр"),
    SERGEANT(400, 0.30f, "Sgt", "Срж"),
    LIEUTENANT(900, 0.50f, "Lt", "Лт"),
    MAJOR(1800, 0.75f, "Maj", "Мaj"),
    GENERAL(3000, 1.0f, "Gen", "Ген");

    public final int reputation;
    /** Réduction sur les soins et la nourriture (1 = gratuit). */
    public final float discount;
    public final String shortUs, shortSoviet;

    Grade(int reputation, float discount, String shortUs, String shortSoviet) {
        this.reputation = reputation;
        this.discount = discount;
        this.shortUs = shortUs;
        this.shortSoviet = shortSoviet;
    }

    public static Grade of(int rep) {
        Grade g = SOLDIER;
        for (Grade x : values()) {
            if (rep >= x.reputation) {
                g = x;
            }
        }
        return g;
    }

    public Grade next() {
        return this == GENERAL ? null : values()[ordinal() + 1];
    }

    public boolean atLeast(Grade g) {
        return ordinal() >= g.ordinal();
    }

    /** Clé de traduction du nom du grade dans l'armée du camp. */
    public String key(Side side) {
        return "grade.modeminecraft." + (side == Side.SOVIET ? "soviet." : "us.") + name().toLowerCase();
    }
}

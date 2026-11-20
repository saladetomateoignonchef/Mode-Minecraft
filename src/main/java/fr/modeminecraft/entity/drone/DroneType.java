package fr.modeminecraft.entity.drone;

/** Les 5 drones de combat (progression par coût). */
public enum DroneType {
    /** Micro-drone de reconnaissance : quasi inaudible, marque les ennemis à travers les murs. */
    HORNET("black_hornet", 200, 0.42, 3, 0.3f, 0.25f, 5),
    /** Drone kamikaze FPV : très rapide et agile, emporte n'importe quel explosif du mod. */
    FPV("fpv_frelon", 250, 0.95, 6, 0.55f, 0.3f, 22),
    /** Drone bombardier : largue jusqu'à 2 grenades à la verticale. */
    MAVIC("mavic_tactique", 250, 0.6, 10, 0.75f, 0.35f, 22),
    /** Drone d'assaut armé : tourelle ventrale, 60 balles, 600 coups/min, 6 PV par balle. */
    WASP("wasp_mg", 300, 0.55, 20, 0.95f, 0.45f, 22),
    /** Drone lourd : 4 mini-roquettes guidées (24 PV), optique thermique. */
    REAPER("reaper_x", 400, 0.62, 40, 1.4f, 0.5f, 22);

    public final String id;
    /** Portée radio (blocs). */
    public final int range;
    /** Vitesse maximale (blocs / tick). */
    public final double speed;
    public final float health, width, height;
    /** Distance à laquelle les bots l'entendent (hélices silencieuses : 7 blocs, sauf le Black Hornet). */
    public final int noise;

    DroneType(String id, int range, double speed, float health, float width, float height, int noise) {
        this.id = id;
        this.range = range;
        this.speed = speed;
        this.health = health;
        this.width = width;
        this.height = height;
        this.noise = noise;
    }

    public static DroneType byId(String id) {
        for (DroneType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return HORNET;
    }

    /** Peut-il recevoir le module cargo (drones de niveau 2 à 4) ? */
    public boolean cargoCapable() {
        return this != HORNET;
    }
}

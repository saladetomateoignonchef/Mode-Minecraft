package fr.modeminecraft.world.city;

/**
 * Plan d'une ville : le rôle de chaque chunk (route, bâtiment, place...), calculé de façon déterministe à partir de
 * la graine de la ville. Les grands bâtiments (commissariat, palais, souk, usine, hacienda) couvrent plusieurs chunks :
 * chaque chunk connaît l'origine (ancre) et la taille du bâtiment, et n'en construit que sa part.
 */
public final class CityLayout {
    /** type = nature de la parcelle ; ancre (ax, az) et taille (w, h) en chunks pour les bâtiments sur plusieurs chunks. */
    public record Plot(String type, int above, int below, int ax, int az, int w, int h) {
        static Plot of(String type, int above, int dx, int dz) {
            return new Plot(type, above, 2, dx, dz, 1, 1);
        }
    }

    private final String variant;
    private final int rad;
    private final long seed;

    public CityLayout(String variant, int rad, long seed) {
        this.variant = variant;
        this.rad = rad;
        this.seed = seed;
    }

    /** Hasard déterministe propre à un chunk de la ville (0 à 1). */
    public double hash(int dx, int dz, int salt) {
        long h = seed ^ (dx * 341873128712L) ^ (dz * 132897987541L) ^ (salt * 0x9E3779B97F4A7C15L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 53);
    }

    public boolean gridRoad(int dx, int dz) {
        return Math.floorMod(dx + rad, 3) == 0 || Math.floorMod(dz + rad, 3) == 0;
    }

    private static boolean in(int dx, int dz, int x0, int z0, int x1, int z1) {
        return dx >= x0 && dx <= x1 && dz >= z0 && dz <= z1;
    }

    private static Plot multi(String type, int above, int ax, int az, int w, int h) {
        return new Plot(type, above, 2, ax, az, w, h);
    }

    public Plot plot(int dx, int dz) {
        boolean edge = Math.abs(dx) == rad || Math.abs(dz) == rad;
        switch (variant) {
            case "metropolis" -> {
                // îlots de 2 x 2 chunks entre les avenues (une avenue tous les 3 chunks)
                if (in(dx, dz, 1, 1, 2, 2)) {
                    return multi("park", 12, 1, 1, 2, 2);                          // Central Park
                }
                if (in(dx, dz, 4, 1, 5, 2)) {
                    return multi("police", 22, 4, 1, 2, 2);                        // commissariat central
                }
                if (in(dx, dz, -5, -5, -4, -4) || in(dx, dz, -11, 7, -10, 8)) {
                    int ax = dx < -6 ? -11 : -5, az = dz > 0 ? 7 : -5;
                    return multi("park", 12, ax, az, 2, 2);                        // parcs de quartier
                }
                if (gridRoad(dx, dz)) {
                    return Plot.of("road", 10, dx, dz);
                }
                int ring = Math.max(Math.abs(dx), Math.abs(dz));
                double h = hash(dx, dz, 1);
                if (ring <= 5) {
                    return Plot.of("tower", 36 + (int) (h * 40) + 6, dx, dz);       // centre d'affaires : 35 à 75 blocs
                }
                if (dx >= 7 && dz <= -7) {
                    return Plot.of(h < 0.55 ? "warehouse" : "factory_m", 18, dx, dz); // zone industrielle
                }
                if (ring <= 10) {                                                   // quartiers commerçants
                    if (h < 0.3) return Plot.of("office", 18 + (int) (h * 60) + 4, dx, dz);
                    if (h < 0.45) return Plot.of("supermarket", 10, dx, dz);
                    if (h < 0.6) return Plot.of("cafe", 10, dx, dz);
                    if (h < 0.78) return Plot.of("shop", 10, dx, dz);
                    if (h < 0.88) return Plot.of("plaza", 10, dx, dz);
                    return Plot.of("apartment", 22, dx, dz);
                }
                // quartiers résidentiels et périphérie
                if (h < 0.45) return Plot.of("house", 12, dx, dz);
                if (h < 0.7) return Plot.of("apartment", 22, dx, dz);
                if (h < 0.78) return Plot.of("gas_station", 10, dx, dz);
                if (h < 0.86) return Plot.of("plaza", 10, dx, dz);
                return Plot.of(h < 0.93 ? "shop" : "cafe", 12, dx, dz);
            }
            case "desert" -> {
                if (in(dx, dz, -1, -5, 1, -3)) {
                    return multi("palace", 26, -1, -5, 3, 3);
                }
                if (in(dx, dz, 2, 1, 4, 2)) {
                    return multi("souk", 10, 2, 1, 3, 2);
                }
                if (in(dx, dz, -7, 5, -5, 6)) {
                    return multi("souk", 10, -7, 5, 3, 2);                          // second marché
                }
                if (dz == 0 || dx == 0 || Math.abs(dx) == 8 || Math.abs(dz) == 8) {
                    return Plot.of("avenue", 8, dx, dz);                           // avenues et boulevard circulaire
                }
                return Plot.of("riad", 14, dx, dz);
            }
            case "rebels" -> {
                if (dx == 0 && dz == 0) {
                    return Plot.of("hq_tree", 32, dx, dz);
                }
                return Plot.of(hash(dx, dz, 2) < 0.6 ? "treehouse" : "camp", 30, dx, dz);
            }
            case "base" -> {
                if (dx == 0 && dz == 0) {
                    return Plot.of("bunker", 10, dx, dz);
                }
                if (Math.abs(dx) == rad && Math.abs(dz) == rad) {
                    return Plot.of("mirador", 16, dx, dz);
                }
                if (in(dx, dz, -2, 1, -1, 2)) {
                    return multi("hangar", 14, -2, 1, 2, 2);
                }
                if (dx == 1 && dz == -1) {
                    return Plot.of("radar", 26, dx, dz);
                }
                if (dx == 1 && dz == 1) {
                    return Plot.of("flak", 8, dx, dz);
                }
                if (edge) {
                    return Plot.of("perimeter", 6, dx, dz);
                }
                return Plot.of(hash(dx, dz, 3) < 0.5 ? "barracks" : "tanks", 14, dx, dz);
            }
            case "soviet" -> {
                if (dz == rad) {
                    return Plot.of("station", 10, dx, dz);
                }
                if (in(dx, dz, 4, -5, 5, -4)) {
                    return multi("factory", 28, 4, -5, 2, 2);
                }
                if (in(dx, dz, -8, 4, -7, 5)) {
                    return multi("factory", 28, -8, 4, 2, 2);                       // seconde usine
                }
                if (in(dx, dz, 0, 0, 1, 1)) {
                    return multi("plaza", 12, 0, 0, 2, 2);
                }
                if (gridRoad(dx, dz)) {
                    return Plot.of("road", 8, dx, dz);
                }
                return Plot.of("khrushchyovka", 24, dx, dz);
            }
            case "polar" -> {
                boolean evenX = Math.floorMod(dx, 2) == 0, evenZ = Math.floorMod(dz, 2) == 0;
                if (evenX && evenZ) {
                    return Plot.of("dome", 14, dx, dz);
                }
                if (evenX || evenZ) {
                    return Plot.of("tunnel", 6, dx, dz);
                }
                return Plot.of("snowfield", 14, dx, dz);
            }
            case "mine" -> {
                if (dx == 0 && dz == 0) {
                    return Plot.of("mine_entrance", 16, dx, dz);
                }
                return Plot.of(edge ? "mine_yard" : "mine_gallery", 8, dx, dz);
            }
            case "cartel" -> {
                if (in(dx, dz, -1, -1, 0, 0)) {
                    return multi("hacienda", 16, -1, -1, 2, 2);
                }
                if (dx == 1 && (dz == -1 || dz == 0)) {
                    return multi("pool", 6, 1, -1, 1, 2);
                }
                if (dz == rad) {
                    return Plot.of("airstrip", 6, dx, dz);
                }
                if (dx == rad && dz == -rad) {
                    return Plot.of("hangar_small", 12, dx, dz);
                }
                return Plot.of("garden", 8, dx, dz);
            }
            case "stilts" -> {
                return Plot.of(dx == 0 && dz == 0 ? "dock" : "shacks", 12, dx, dz);
            }
            case "frontier" -> {
                if (dz == 0) {
                    return Plot.of("main_street", 6, dx, dz);
                }
                if (dz == -rad) {
                    return Plot.of(dx == 0 ? "station_west" : "railway", 10, dx, dz);
                }
                if (Math.abs(dz) == 1) {
                    String[] b = dz == 1 ? new String[]{"hotel", "store", "saloon", "house_west", "house_west", "store", "house_west"}
                            : new String[]{"house_west", "sheriff", "house_west", "bank", "store", "house_west", "house_west"};
                    return Plot.of(b[Math.floorMod(dx + 3, b.length)], 16, dx, dz);
                }
                if (dx == rad - 1 && dz == 2) {
                    return Plot.of("water_tower", 20, dx, dz);
                }
                return Plot.of("ranch", 8, dx, dz);
            }
            case "refinery" -> {
                if (Math.abs(dx) == rad && Math.abs(dz) == rad) {
                    return Plot.of("guard_tower", 16, dx, dz);
                }
                if (edge) {
                    return Plot.of("fence", 6, dx, dz);
                }
                if (dx == -2 && dz == -2) {
                    return Plot.of("refinery_office", 12, dx, dz);
                }
                if (dx == 0 || dz == 0) {
                    return Plot.of(dx == 0 && dz == 0 ? "flare" : "pipes", dx == 0 && dz == 0 ? 34 : 8, dx, dz);
                }
                return Plot.of(hash(dx, dz, 4) < 0.55 ? "oil_tank" : "pumpjack", 20, dx, dz);
            }
            default -> {
                return null;
            }
        }
    }
}

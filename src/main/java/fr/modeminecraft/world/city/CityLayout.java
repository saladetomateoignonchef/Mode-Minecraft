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

    /** Parcelle d'un bâtiment du scénario dans la base de ce camp, ou null. */
    private static Plot scenario(fr.modeminecraft.scenario.Side side, int dx, int dz) {
        var site = fr.modeminecraft.scenario.ScenarioSites.siteAt(side, dx, dz);
        if (site == null) {
            return null;
        }
        int[] a = site.anchor(side);
        int below = site == fr.modeminecraft.scenario.ScenarioSites.Site.GENERAL_BUNKER ? 10 : 2;
        return new Plot(site.plot(), 14, below, a[0], a[1], site.w, site.h);
    }

    private static Plot multi(String type, int above, int ax, int az, int w, int h) {
        return new Plot(type, above, 2, ax, az, w, h);
    }

    // ------------------------------------------------------------------ contour organique et variété des quartiers
    /** Les villes de grande taille n'ont pas un contour carré et varient leurs quartiers. */
    private boolean organic() {
        return rad >= 10 && (variant.equals("desert") || variant.equals("metropolis") || variant.equals("soviet"));
    }

    /** Parcelles de remplissage dont le contour irrégulier peut retirer le bâtiment (les sites uniques restent). */
    private boolean shapeable(Plot p) {
        if (p.type().equals("park") && variant.equals("metropolis")) {
            return true;                                           // parcs de quartier (2 x 2) : retirés en bloc selon leur ancre
        }
        if (p.w() != 1 || p.h() != 1) {
            return false;
        }
        return switch (variant) {
            case "desert" -> p.type().equals("riad") || p.type().equals("avenue");
            case "metropolis" -> switch (p.type()) {
                case "road", "house", "apartment", "gas_station", "shop", "cafe", "supermarket", "office", "plaza", "warehouse", "factory_m", "park" -> true;
                default -> false;
            };
            case "soviet" -> p.type().equals("khrushchyovka") || p.type().equals("road");
            default -> false;
        };
    }

    /**
     * Contour irrégulier : le noyau de la ville (moins de la moitié du rayon) est toujours là ; au-delà, la limite
     * ondule selon l'angle (lobes de 2, 3 et 5 harmoniques propres à chaque ville) et des quartiers s'étirent le long
     * des deux avenues centrales (les routes entre villes y arrivent toujours). Plus de grand bloc carré.
     */
    private boolean outside(int dx, int dz) {
        double d = Math.hypot(dx, dz) / rad;
        double core = variant.equals("soviet") ? 0.6 : 0.5;
        if (d <= core) {
            return false;
        }
        int along = Math.min(Math.abs(dx), Math.abs(dz));
        if (along <= 1) {
            return false;                                          // avenues centrales : jamais coupées
        }
        double th = Math.atan2(dz, dx);
        double n = 0.5 + 0.5 * (0.5 * Math.sin(2 * th + hash(0, 0, 41) * 6.28) + 0.3 * Math.sin(3 * th + hash(0, 0, 42) * 6.28)
                + 0.2 * Math.sin(5 * th + hash(0, 0, 43) * 6.28));
        double bound = 0.52 + 0.46 * n + (hash(dx, dz, 44) - 0.5) * 0.1;
        if (along <= 3) {
            bound += 0.32;                                         // quartier qui s'étire le long de la route
        }
        return d > bound;
    }

    private record Weighted(String type, int above, double weight) {}

    private static final Weighted[] DESERT_POOL = {
            new Weighted("riad", 14, 0.30), new Weighted("riad_tall", 24, 0.16), new Weighted("oasis", 10, 0.14),
            new Weighted("market_hall", 10, 0.14), new Weighted("worksite", 14, 0.12), new Weighted("square", 10, 0.14)};
    private static final Weighted[] COMMERCE_POOL = {
            new Weighted("office", 28, 0.24), new Weighted("supermarket", 10, 0.12), new Weighted("cafe", 10, 0.14),
            new Weighted("shop", 10, 0.18), new Weighted("plaza", 10, 0.10), new Weighted("apartment", 22, 0.22)};
    private static final Weighted[] RESIDENTIAL_POOL = {
            new Weighted("house", 12, 0.36), new Weighted("apartment", 22, 0.24), new Weighted("gas_station", 10, 0.08),
            new Weighted("plaza", 10, 0.10), new Weighted("shop", 12, 0.12), new Weighted("cafe", 12, 0.10)};
    private static final Weighted[] INDUSTRIAL_POOL = {
            new Weighted("warehouse", 18, 0.5), new Weighted("factory_m", 18, 0.3), new Weighted("gas_station", 10, 0.2)};

    private String[] kinds;                                        // type final de chaque parcelle de remplissage (voir diversify)

    /** Parcelles de remplissage concernées par la diversification. */
    private boolean filler(Plot p) {
        if (p == null || p.w() != 1 || p.h() != 1) {
            return false;
        }
        return switch (variant) {
            case "desert" -> p.type().equals("riad");
            case "metropolis" -> switch (p.type()) {
                case "house", "apartment", "gas_station", "shop", "cafe", "supermarket", "office", "plaza", "warehouse", "factory_m" -> true;
                default -> false;
            };
            default -> false;
        };
    }

    private Weighted[] pool(int dx, int dz) {
        if (variant.equals("desert")) {
            return DESERT_POOL;
        }
        if (dx >= 7 && dz <= -7) {
            return INDUSTRIAL_POOL;
        }
        return Math.max(Math.abs(dx), Math.abs(dz)) <= 10 ? COMMERCE_POOL : RESIDENTIAL_POOL;
    }

    /**
     * Deux parcelles voisines n'ont jamais le même type : on parcourt la ville ligne par ligne et chaque parcelle
     * tire son type (selon des poids) en écartant ceux de ses voisines déjà décidées (ouest et nord).
     */
    private synchronized String[] kinds() {
        if (kinds == null) {
            int n = 2 * rad + 1;
            String[] k = new String[n * n];
            for (int z = -rad; z <= rad; z++) {
                for (int x = -rad; x <= rad; x++) {
                    Plot raw = rawPlot(x, z);
                    if (!filler(raw) || outside(x, z)) {
                        continue;
                    }
                    String west = x > -rad ? k[(z + rad) * n + (x - 1 + rad)] : null;
                    String north = z > -rad ? k[(z - 1 + rad) * n + (x + rad)] : null;
                    Weighted[] pool = pool(x, z);
                    String pick = null;
                    for (int attempt = 0; attempt < 8 && pick == null; attempt++) {
                        double r = hash(x, z, 60 + attempt), acc = 0;
                        for (Weighted w : pool) {
                            acc += w.weight();
                            if (r * total(pool) < acc) {
                                pick = w.type();
                                break;
                            }
                        }
                        if (pick != null && (pick.equals(west) || pick.equals(north))) {
                            pick = null;
                        }
                    }
                    if (pick == null) {
                        for (Weighted w : pool) {
                            if (!w.type().equals(west) && !w.type().equals(north)) {
                                pick = w.type();
                                break;
                            }
                        }
                    }
                    k[(z + rad) * n + (x + rad)] = pick;
                }
            }
            kinds = k;
        }
        return kinds;
    }

    private static double total(Weighted[] pool) {
        double t = 0;
        for (Weighted w : pool) {
            t += w.weight();
        }
        return t;
    }

    /** Parcelle d'une ville : plan de base, contour irrégulier et quartiers variés. */
    public Plot plot(int dx, int dz) {
        Plot p = rawPlot(dx, dz);
        if (p == null || !organic() || Math.abs(dx) > rad || Math.abs(dz) > rad) {
            return p;                                              // hors du plan (voisines interrogées par les bâtiments)
        }
        if (shapeable(p) && outside(p.ax(), p.az())) {
            return null;
        }
        if (filler(p)) {
            String type = kinds()[(dz + rad) * (2 * rad + 1) + (dx + rad)];
            if (type != null && !type.equals(p.type())) {
                for (Weighted[] pool : new Weighted[][]{DESERT_POOL, COMMERCE_POOL, RESIDENTIAL_POOL, INDUSTRIAL_POOL}) {
                    for (Weighted w : pool) {
                        if (w.type().equals(type)) {
                            return Plot.of(type, w.above(), dx, dz);
                        }
                    }
                }
            }
        }
        return p;
    }

    private Plot rawPlot(int dx, int dz) {
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
                // scénario « Course à l'arme nucléaire » : bâtiments de la base (parcelles réservées)
                Plot sc = scenario(fr.modeminecraft.scenario.Side.US, dx, dz);
                if (sc != null) {
                    return sc;
                }
                if (in(dx, dz, -4, 3, 1, 4)) {
                    return multi("airstrip", 16, -4, 3, 6, 2);                     // piste d'atterrissage (96 m)
                }
                if (in(dx, dz, 2, 2, 4, 4)) {
                    return multi("aircraft_hangar", 18, 2, 2, 3, 3);              // méga-hangar à avions
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
                Plot sc = scenario(fr.modeminecraft.scenario.Side.SOVIET, dx, dz);  // scénario : bâtiments de la base
                if (sc != null) {
                    return sc;
                }
                // aérodrome militaire en bordure nord de la ville : piste de 112 m, méga-hangar dans son axe (sa porte
                // donne sur le seuil de piste : les avions sortent du hangar et décollent tout droit)
                if (in(dx, dz, -12, -11, -6, -10)) {
                    return multi("airstrip", 16, -12, -11, 7, 2);                  // jusqu'au bord ouest : dégagé au bout
                }
                if (in(dx, dz, -5, -12, -3, -10)) {
                    return multi("aircraft_hangar", 18, -5, -12, 3, 3);
                }
                if (in(dx, dz, 4, -5, 5, -4)) {
                    return multi("factory", 28, 4, -5, 2, 2);
                }
                if (in(dx, dz, -8, 4, -7, 5)) {
                    return multi("factory", 28, -8, 4, 2, 2);                       // seconde usine
                }
                if (in(dx, dz, -9, -9, -8, -7)) {
                    return multi("mil_hangar", 16, -9, -9, 2, 3);                   // hangar militaire blindé
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

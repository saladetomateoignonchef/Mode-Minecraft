package fr.modeminecraft.world.rail;

import fr.modeminecraft.world.city.CityPlacement;
import fr.modeminecraft.world.city.CityPlanner;
import fr.modeminecraft.world.road.RoadNetwork;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Réseau ferroviaire du biome Mesa : chaque ville frontière (gare {@code mesa_train_station}) est reliée aux deux villes
 * frontières les plus proches parmi les 8 régions voisines. Une ligne part de la voie qui traverse la ville (rangée de
 * chunks nord, axe est-ouest), sort par le bord est ou ouest, suit le même profil en long que les routes (au-dessus de
 * l'eau, raccordé au niveau des rues) et entre dans l'autre ville par sa voie. Tout est déterministe : la génération
 * des rails, les trains et les gares retrouvent exactement le même tracé sans rien sauvegarder.
 */
public final class RailNetwork {
    public static final double STEP = RoadNetwork.STEP;
    /** Distance entre le centre de la gare et l'arrêt du nez de la locomotive (le train s'arrête devant le quai). */
    public static final double STOP_AHEAD = 8;

    /**
     * Une ligne (sens canonique de a vers b) : points tous les 0,5 bloc depuis le centre de la gare a jusqu'au centre de
     * la gare b. Au-delà de ces bouts, la voie se prolonge en ligne droite le long de la rangée est-ouest des villes.
     */
    public record Line(String key, CityPlanner.City a, CityPlanner.City b, double[] xs, double[] zs, double[] ys, double dirX, int[][][] cellBox) {
        public double length() {
            return (xs.length - 1) * STEP;
        }

        /** Position (x, z, y du bloc de ballast) à l'abscisse curviligne s ; au-delà des bouts : prolongement droit. */
        public double[] at(double s) {
            int n = xs.length;
            if (s <= 0) {
                return new double[]{xs[0] + dirX * s, zs[0], ys[0]};
            }
            double len = length();
            if (s >= len) {
                return new double[]{xs[n - 1] + dirX * (s - len), zs[n - 1], ys[n - 1]};
            }
            double f = s / STEP;
            int i = (int) f;
            double t = f - i;
            return new double[]{xs[i] + (xs[i + 1] - xs[i]) * t, zs[i] + (zs[i + 1] - zs[i]) * t, ys[i] + (ys[i + 1] - ys[i]) * t};
        }

        /** Cap (degrés, convention Minecraft : 0 = +Z, 90 = -X) de la voie à l'abscisse s, dans le sens des s croissants. */
        public float yaw(double s) {
            double[] p = at(s - 1), q = at(s + 1);
            return (float) Math.toDegrees(Math.atan2(-(q[0] - p[0]), q[1] - p[1]));
        }

        public int minX() { return (int) Math.floor(min(xs)) - 6; }
        public int maxX() { return (int) Math.ceil(max(xs)) + 6; }
        public int minZ() { return (int) Math.floor(min(zs)) - 6; }
        public int maxZ() { return (int) Math.ceil(max(zs)) + 6; }

        private static double min(double[] v) {
            double m = Double.MAX_VALUE;
            for (double d : v) m = Math.min(m, d);
            return m;
        }

        private static double max(double[] v) {
            double m = -Double.MAX_VALUE;
            for (double d : v) m = Math.max(m, d);
            return m;
        }

        /** Cellules de la voie (blocs), liées orthogonalement : x, z, y du rail. */
        public int[][] cells() {
            synchronized (cellBox) {
                if (cellBox[0] == null) {
                    cellBox[0] = computeCells(this);
                }
                return cellBox[0];
            }
        }
    }

    private static final Map<ChunkGenerator, Map<String, Line>> LINES = new WeakHashMap<>();

    /** Ville dotée d'une gare : la ville frontière du Mesa. */
    public static boolean station(CityPlanner.City c) {
        return c != null && c.variant().equals("frontier");
    }

    /** Z (monde) de la voie qui traverse une ville frontière : rangée de chunks la plus au nord, bloc 7. */
    public static double railZ(CityPlanner.City c) {
        return (c.center().z - c.rad()) * 16 + 7.5;
    }

    /** Villes reliées à celle-ci : les deux villes frontières les plus proches des 8 régions voisines. */
    private static List<CityPlanner.City> nearest(CityPlanner.Env e, CityPlanner.City c) {
        List<CityPlanner.City> out = new ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                CityPlanner.City o = CityPlanner.city(e, c.rx() + i, c.rz() + j);
                if (station(o)) {
                    out.add(o);
                }
            }
        }
        out.sort(Comparator.comparingDouble(o -> Math.hypot(o.middleX() - c.middleX(), o.middleZ() - c.middleZ())));
        return out.size() > 2 ? new ArrayList<>(out.subList(0, 2)) : out;
    }

    private static boolean same(CityPlanner.City a, CityPlanner.City b) {
        return a.rx() == b.rx() && a.rz() == b.rz();
    }

    /** Toutes les villes reliées par rail à c (la liaison existe si l'une des deux la choisit). */
    public static List<CityPlanner.City> links(CityPlanner.Env e, CityPlanner.City c) {
        List<CityPlanner.City> out = new ArrayList<>(nearest(e, c));
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                CityPlanner.City o = CityPlanner.city(e, c.rx() + i, c.rz() + j);
                if (station(o) && out.stream().noneMatch(x -> same(x, o)) && nearest(e, o).stream().anyMatch(x -> same(x, c))) {
                    out.add(o);
                }
            }
        }
        return out;
    }

    public static String key(CityPlanner.City a, CityPlanner.City b) {
        long ka = ChunkPos.asLong(a.rx(), a.rz()), kb = ChunkPos.asLong(b.rx(), b.rz());
        return ka <= kb ? ka + ":" + kb : kb + ":" + ka;
    }

    /** La ligne entre deux villes reliées (sens canonique : la ville de plus petite clé en premier). */
    public static Line line(CityPlanner.Env e, CityPlanner.City x, CityPlanner.City y) {
        long kx = ChunkPos.asLong(x.rx(), x.rz()), ky = ChunkPos.asLong(y.rx(), y.rz());
        CityPlanner.City a = kx <= ky ? x : y, b = kx <= ky ? y : x;
        String key = key(a, b);
        Map<String, Line> cache;
        synchronized (LINES) {
            cache = LINES.computeIfAbsent(e.gen(), g -> new ConcurrentHashMap<>());
        }
        String ck = e.seed() + "/" + key;
        Line l = cache.get(ck);
        if (l == null) {
            l = build(e, key, a, b);
            if (cache.size() > 512) {
                cache.clear();
            }
            cache.put(ck, l);
        }
        return l;
    }

    private static Line build(CityPlanner.Env e, String key, CityPlanner.City a, CityPlanner.City b) {
        double dir = b.middleX() >= a.middleX() ? 1 : -1;
        double za = railZ(a), zb = railZ(b);
        double exitA = dir > 0 ? a.maxX() + 1 : a.minX() - 1, entryB = dir > 0 ? b.minX() - 1 : b.maxX() + 1;
        RoadNetwork.Path path = RoadNetwork.build(e, a, b, exitA, za, entryB, zb, dir, 0);
        double startX = a.middleX() + 0.5, endX = b.middleX() + 0.5;
        List<double[]> pts = new ArrayList<>();
        double ya = a.baseY() - 1, yb = b.baseY() - 1;
        for (double x = startX; (x - exitA) * dir < 0; x += dir * STEP) {
            pts.add(new double[]{x, za, ya});
        }
        for (int k = 0; k < path.size(); k++) {
            pts.add(new double[]{path.xs()[k], path.zs()[k], path.ys()[k]});
        }
        for (double x = entryB + dir * STEP; (x - endX) * dir <= 0; x += dir * STEP) {
            pts.add(new double[]{x, zb, yb});
        }
        int n = pts.size();
        double[] xs = new double[n], zs = new double[n], ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = pts.get(i)[0];
            zs[i] = pts.get(i)[1];
            ys[i] = pts.get(i)[2];
        }
        return new Line(key, a, b, xs, zs, ys, dir, new int[1][][]);
    }

    /** Lignes qui peuvent passer dans ce chunk. */
    public static List<Line> linesNear(CityPlanner.Env e, ChunkPos chunk) {
        List<Line> out = new ArrayList<>();
        CityPlacement p = CityPlanner.placement(e.reg());
        if (p == null) {
            return out;
        }
        int rx = Math.floorDiv(chunk.x, p.spacing()), rz = Math.floorDiv(chunk.z, p.spacing());
        int x0 = chunk.getMinBlockX() - 8, z0 = chunk.getMinBlockZ() - 8, x1 = chunk.getMaxBlockX() + 8, z1 = chunk.getMaxBlockZ() + 8;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                CityPlanner.City c = CityPlanner.city(e, rx + i, rz + j);
                if (!station(c)) {
                    continue;
                }
                for (CityPlanner.City o : links(e, c)) {
                    String key = key(c, o);
                    if (!seen.add(key)) {
                        continue;
                    }
                    // tracé encore inconnu : calculé seulement si ce chunk est dans la zone entre les deux gares
                    int m = 100;
                    if (Math.max(c.middleX(), o.middleX()) + m < x0 || Math.min(c.middleX(), o.middleX()) - m > x1
                            || Math.max(railZ(c), railZ(o)) + m < z0 || Math.min(railZ(c), railZ(o)) - m > z1) {
                        continue;
                    }
                    Line l = line(e, c, o);
                    if (l.maxX() >= x0 && l.minX() <= x1 && l.maxZ() >= z0 && l.minZ() <= z1) {
                        out.add(l);
                    }
                }
            }
        }
        return out;
    }

    private static int[][] computeCells(Line l) {
        List<int[]> cells = new ArrayList<>();
        int lastX = Integer.MIN_VALUE, lastZ = 0;
        for (int k = 0; k < l.xs().length; k++) {
            int x = (int) Math.floor(l.xs()[k]), z = (int) Math.floor(l.zs()[k]);
            if (x == lastX && z == lastZ) {
                continue;
            }
            int y = (int) Math.round(l.ys()[k]) + 1;
            if (lastX != Integer.MIN_VALUE && x != lastX && z != lastZ) {
                cells.add(new int[]{x, lastZ, y});                   // pas en diagonale : on passe par un coin
            }
            cells.add(new int[]{x, z, y});
            lastX = x;
            lastZ = z;
        }
        // pente d'au plus 1 bloc entre deux cellules voisines
        for (int i = 1; i < cells.size(); i++) {
            int[] c = cells.get(i), p = cells.get(i - 1);
            c[2] = Math.max(p[2] - 1, Math.min(p[2] + 1, c[2]));
        }
        return cells.toArray(new int[0][]);
    }

    private RailNetwork() {}
}

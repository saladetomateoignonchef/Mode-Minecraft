package fr.modeminecraft.world.road;

import fr.modeminecraft.world.city.CityPlacement;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Réseau routier entre les villes d'un même biome : chaque ville est reliée aux villes de même famille des 8 régions
 * voisines (et, si elle n'en a aucune, à la plus proche à 2 régions). Le tracé part du milieu du bord de la ville qui
 * fait face à l'autre (dans l'axe de l'avenue centrale), s'en écarte tout droit sur 48 blocs, puis ondule doucement.
 * Le profil en long suit le sol (moyenne glissante sur 80 blocs, au-dessus de l'eau pour les ponts) et rejoint
 * exactement le niveau des rues à chaque bout. Tout est déterministe : chaque chunk retrouve le même tracé.
 */
public final class RoadNetwork {
    /** Un tracé : points tous les 0,5 bloc (x, z), altitude du revêtement (bloc), famille de biome. */
    public record Path(String kind, double[] xs, double[] zs, double[] ys, int minX, int minZ, int maxX, int maxZ) {
        public int size() { return xs.length; }
    }

    public static final double STEP = 0.5;
    private static final Map<ChunkGenerator, Map<Long, Path>> PATHS = new WeakHashMap<>();

    /** Tracés qui peuvent passer dans ce chunk. */
    public static List<Path> pathsNear(CityPlanner.Env e, ChunkPos chunk) {
        CityPlacement p = CityPlanner.placement(e.reg());
        List<Path> out = new ArrayList<>();
        if (p == null) {
            return out;
        }
        int rx = Math.floorDiv(chunk.x, p.spacing()), rz = Math.floorDiv(chunk.z, p.spacing());
        Map<Long, long[]> edges = new HashMap<>();
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                CityPlanner.City c = CityPlanner.city(e, rx + i, rz + j);
                if (c == null) {
                    continue;
                }
                for (CityPlanner.City o : neighbours(e, c)) {
                    long a = ChunkPos.asLong(c.rx(), c.rz()), b = ChunkPos.asLong(o.rx(), o.rz());
                    long key = Math.min(a, b) * 31 + Math.max(a, b);
                    edges.putIfAbsent(key, new long[]{Math.min(a, b), Math.max(a, b)});
                }
            }
        }
        int x0 = chunk.getMinBlockX() - 8, z0 = chunk.getMinBlockZ() - 8, x1 = chunk.getMaxBlockX() + 8, z1 = chunk.getMaxBlockZ() + 8;
        Map<Long, Path> cache;
        synchronized (PATHS) {
            cache = PATHS.computeIfAbsent(e.gen(), g -> new ConcurrentHashMap<>());
        }
        for (var entry : edges.entrySet()) {
            Path path = cache.get(entry.getKey() ^ e.seed());
            if (path == null) {
                long[] ends = entry.getValue();
                CityPlanner.City a = CityPlanner.city(e, ChunkPos.getX(ends[0]), ChunkPos.getZ(ends[0]));
                CityPlanner.City b = CityPlanner.city(e, ChunkPos.getX(ends[1]), ChunkPos.getZ(ends[1]));
                if (a == null || b == null) {
                    continue;
                }
                path = build(e, a, b);
                if (cache.size() > 2048) {
                    cache.clear();
                }
                cache.put(entry.getKey() ^ e.seed(), path);
            }
            if (path.maxX() >= x0 && path.minX() <= x1 && path.maxZ() >= z0 && path.minZ() <= z1) {
                out.add(path);
            }
        }
        return out;
    }

    /** Villes de même famille reliées à celle-ci. */
    public static List<CityPlanner.City> neighbours(CityPlanner.Env e, CityPlanner.City c) {
        List<CityPlanner.City> out = new ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                CityPlanner.City o = CityPlanner.city(e, c.rx() + i, c.rz() + j);
                if (o != null && o.kind().equals(c.kind())) {
                    out.add(o);
                }
            }
        }
        if (out.isEmpty()) {
            CityPlanner.City best = null;
            double bestD = Double.MAX_VALUE;
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    if (Math.max(Math.abs(i), Math.abs(j)) < 2) continue;
                    CityPlanner.City o = CityPlanner.city(e, c.rx() + i, c.rz() + j);
                    if (o != null && o.kind().equals(c.kind())) {
                        double d = Math.hypot(o.middleX() - c.middleX(), o.middleZ() - c.middleZ());
                        if (d < bestD) {
                            bestD = d;
                            best = o;
                        }
                    }
                }
            }
            if (best != null) {
                out.add(best);
            }
        }
        // l'autre ville peut, elle, ne pas avoir besoin de nous : on garde aussi les liens qu'elle crée vers nous
        return out;
    }

    private static double hash(long seed, long a, long b) {
        long h = seed ^ a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 53);
    }

    public static Path build(CityPlanner.Env e, CityPlanner.City a, CityPlanner.City b) {
        double dx = b.middleX() - a.middleX(), dz = b.middleZ() - a.middleZ();
        boolean alongX = Math.abs(dx) >= Math.abs(dz);
        double sx = Math.signum(alongX ? dx : 0), sz = Math.signum(alongX ? 0 : dz);
        // sorties dans l'axe de l'avenue centrale de chaque ville
        double ax = alongX ? (dx > 0 ? a.maxX() + 1 : a.minX() - 1) : a.middleX() + 0.5;
        double az = alongX ? a.middleZ() + 0.5 : (dz > 0 ? a.maxZ() + 1 : a.minZ() - 1);
        double bx = alongX ? (dx > 0 ? b.minX() - 1 : b.maxX() + 1) : b.middleX() + 0.5;
        double bz = alongX ? b.middleZ() + 0.5 : (dz > 0 ? b.minZ() - 1 : b.maxZ() + 1);
        double[][] ctrl = new double[5][];
        ctrl[0] = new double[]{ax, az};
        ctrl[1] = new double[]{ax + sx * 48, az + sz * 48};
        ctrl[4] = new double[]{bx, bz};
        ctrl[3] = new double[]{bx - sx * 48, bz - sz * 48};
        double mx = (ctrl[1][0] + ctrl[3][0]) / 2, mz = (ctrl[1][1] + ctrl[3][1]) / 2;
        double len = Math.hypot(ctrl[3][0] - ctrl[1][0], ctrl[3][1] - ctrl[1][1]);
        double px = -(ctrl[3][1] - ctrl[1][1]) / Math.max(1, len), pz = (ctrl[3][0] - ctrl[1][0]) / Math.max(1, len);
        double wiggle = (hash(e.seed(), ChunkPos.asLong(a.rx(), a.rz()), ChunkPos.asLong(b.rx(), b.rz())) * 2 - 1) * Math.min(70, len * 0.12);
        ctrl[2] = new double[]{mx + px * wiggle, mz + pz * wiggle};
        // Catmull-Rom échantillonnée finement, puis rééchantillonnée tous les 0,5 bloc
        List<double[]> fine = new ArrayList<>();
        fine.add(ctrl[0]);
        for (int s = 0; s < 4; s++) {
            double[] p0 = ctrl[Math.max(0, s - 1)], p1 = ctrl[s], p2 = ctrl[s + 1], p3 = ctrl[Math.min(4, s + 2)];
            double segLen = Math.hypot(p2[0] - p1[0], p2[1] - p1[1]);
            int n = Math.max(2, (int) (segLen * 2));
            for (int k = 1; k <= n; k++) {
                double t = k / (double) n, t2 = t * t, t3 = t2 * t;
                double x = 0.5 * (2 * p1[0] + (-p0[0] + p2[0]) * t + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2 + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3);
                double z = 0.5 * (2 * p1[1] + (-p0[1] + p2[1]) * t + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2 + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3);
                fine.add(new double[]{x, z});
            }
        }
        List<double[]> pts = new ArrayList<>();
        pts.add(fine.get(0));
        double carry = 0;
        for (int k = 1; k < fine.size(); k++) {
            double[] p = fine.get(k - 1), q = fine.get(k);
            double l = Math.hypot(q[0] - p[0], q[1] - p[1]);
            double pos = STEP - carry;
            while (pos <= l) {
                double f = pos / Math.max(1e-9, l);
                pts.add(new double[]{p[0] + (q[0] - p[0]) * f, p[1] + (q[1] - p[1]) * f});
                pos += STEP;
            }
            carry = l - (pos - STEP);
        }
        int n = pts.size();
        double[] xs = new double[n], zs = new double[n], ys = new double[n];
        for (int k = 0; k < n; k++) {
            xs[k] = pts.get(k)[0];
            zs[k] = pts.get(k)[1];
        }
        // profil en long : sol tous les 8 blocs, au-dessus de l'eau, moyenne glissante, raccord aux rues
        int every = (int) (8 / STEP);
        int m = (n - 1) / every + 1;
        double[] raw = new double[m];
        boolean[] wet = new boolean[m];
        int sea = e.gen().getSeaLevel();
        for (int k = 0; k < m; k++) {
            int idx = Math.min(n - 1, k * every);
            int x = (int) Math.floor(xs[idx]), z = (int) Math.floor(zs[idx]);
            int floor = e.gen().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, e.height(), e.rs()) - 1;
            int surf = e.gen().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, e.height(), e.rs()) - 1;
            wet[k] = surf > floor;
            raw[k] = wet[k] ? Math.max(floor, sea + 1) : floor;                // eau : tablier au-dessus
        }
        double[] smooth = new double[m];
        for (int k = 0; k < m; k++) {
            double sum = 0;
            int cnt = 0;
            for (int w = -5; w <= 5; w++) {
                int q = Math.min(m - 1, Math.max(0, k + w));
                sum += raw[q];
                cnt++;
            }
            smooth[k] = wet[k] ? Math.max(sum / cnt, sea + 1) : sum / cnt;
        }
        double startY = a.baseY() - 1, endY = b.baseY() - 1;
        double total = (n - 1) * STEP;
        for (int k = 0; k < n; k++) {
            double f = k / (double) every;
            int q = Math.min(m - 2, (int) f);
            double y = m < 2 ? smooth[0] : smooth[q] + (smooth[q + 1] - smooth[q]) * Math.min(1, f - q);
            double s = k * STEP, toEnd = total - s;
            double wa = Math.max(0, 1 - s / 64.0), wb = Math.max(0, 1 - toEnd / 64.0);
            wa = wa * wa * (3 - 2 * wa);
            wb = wb * wb * (3 - 2 * wb);
            y = y * (1 - wa) + startY * wa;
            y = y * (1 - wb) + endY * wb;
            ys[k] = y;
        }
        double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            minX = Math.min(minX, xs[k]);
            maxX = Math.max(maxX, xs[k]);
            minZ = Math.min(minZ, zs[k]);
            maxZ = Math.max(maxZ, zs[k]);
        }
        return new Path(a.kind(), xs, zs, ys, (int) Math.floor(minX) - 6, (int) Math.floor(minZ) - 6, (int) Math.ceil(maxX) + 6, (int) Math.ceil(maxZ) + 6);
    }

    private RoadNetwork() {}
}

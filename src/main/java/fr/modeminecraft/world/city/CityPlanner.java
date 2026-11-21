package fr.modeminecraft.world.city;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plan des villes du monde, calculé de façon déterministe (graine du monde + région) : famille de biome, variante,
 * taille, centre, altitude des rues. Le même calcul sert à la génération des districts, aux routes entre villes et
 * aux cellules du commissariat (arrestations) : tous voient exactement les mêmes villes, sans rien sauvegarder.
 */
public final class CityPlanner {
    public static final String[] KINDS = {"plains", "desert", "forest", "snow", "jungle", "mesa"};

    /** Une ville : centre (chunk), demi-taille `rad` en chunks, altitude des rues `baseY`, graine du plan. */
    public record City(String kind, String variant, int rad, ChunkPos center, int baseY, long seed, int rx, int rz) {
        public int minX() { return (center.x - rad) * 16; }
        public int minZ() { return (center.z - rad) * 16; }
        public int maxX() { return (center.x + rad) * 16 + 15; }
        public int maxZ() { return (center.z + rad) * 16 + 15; }
        public int middleX() { return center.getMiddleBlockX(); }
        public int middleZ() { return center.getMiddleBlockZ(); }

        public boolean contains(int x, int z, int margin) {
            return x >= minX() - margin && x <= maxX() + margin && z >= minZ() - margin && z <= maxZ() + margin;
        }

        public CityLayout layout() {
            return new CityLayout(variant, rad, seed);
        }
    }

    /** Ce qu'il faut pour calculer une ville. */
    public record Env(long seed, ChunkGenerator gen, RandomState rs, LevelHeightAccessor height, RegistryAccess reg) {
        public static Env of(ServerLevel level) {
            return new Env(level.getSeed(), level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level, level.registryAccess());
        }
    }

    private static final Map<ChunkGenerator, Map<Long, Optional<City>>> CACHE = new WeakHashMap<>();

    public static CityPlacement placement(RegistryAccess reg) {
        var set = reg.registryOrThrow(Registries.STRUCTURE_SET).get(ModeMinecraft.id("cities"));
        return set != null && set.placement() instanceof CityPlacement p ? p : null;
    }

    /** La ville de la région (rx, rz), ou null (pas de ville : biome, relief, eau). */
    public static City city(Env e, int rx, int rz) {
        Map<Long, Optional<City>> cache;
        synchronized (CACHE) {
            cache = CACHE.computeIfAbsent(e.gen(), g -> new ConcurrentHashMap<>());
        }
        long key = ChunkPos.asLong(rx, rz) ^ e.seed() * 31;
        Optional<City> c = cache.get(key);
        if (c == null) {
            c = Optional.ofNullable(compute(e, rx, rz));
            if (cache.size() > 4096) {
                cache.clear();
            }
            cache.put(key, c);
        }
        return c.orElse(null);
    }

    /** Famille de biome au centre d'une ville (celle dont la structure accepte ce biome). */
    static String kindAt(Env e, int x, int y, int z) {
        Holder<Biome> biome = e.gen().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), e.rs().sampler());
        var structures = e.reg().registryOrThrow(Registries.STRUCTURE);
        for (String kind : KINDS) {
            Structure s = structures.get(ModeMinecraft.id("city_" + kind));
            if (s != null && s.biomes().contains(biome)) {
                return kind;
            }
        }
        return null;
    }

    private static City compute(Env e, int rx, int rz) {
        CityPlacement p = placement(e.reg());
        if (p == null) {
            return null;
        }
        ChunkPos cp = p.center(e.seed(), rx, rz);
        int cx = cp.getMiddleBlockX(), cz = cp.getMiddleBlockZ();
        var gen = e.gen();
        int center = gen.getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, e.height(), e.rs());
        String kind = kindAt(e, cx, center, cz);
        if (kind == null) {
            return null;
        }
        RandomSource r = RandomSource.create(e.seed() ^ cp.toLong() * 0x9E3779B97F4A7C15L ^ 0x5DEECE66DL);
        String variant = CityStructure.variant(kind, r);
        int rad = CityStructure.radius(variant);
        Integer base = baseY(e, variant, rad, cx, cz, center, false);
        if (base == null) {
            return null;
        }
        return new City(kind, variant, rad, cp, base, r.nextLong(), rx, rz);
    }

    /** Altitude des rues, ou null si le terrain ne convient pas (trop accidenté, dans l'eau). */
    static Integer baseY(Env e, String variant, int rad, int cx, int cz, int center, boolean forced) {
        var gen = e.gen();
        int sea = gen.getSeaLevel();
        int n = 1;                                                  // 3 x 3 relevés (le calcul du relief coûte cher)
        int span = rad * 16 * 3 / 4;
        int min = center, max = center, water = 0, samples = 0;
        for (int i = -n; i <= n; i++) {
            for (int j = -n; j <= n; j++) {
                int x = cx + i * span, z = cz + j * span;
                int h = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, e.height(), e.rs());
                int s = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, e.height(), e.rs());
                if (s > h + 1) {
                    water++;
                }
                min = Math.min(min, h);
                max = Math.max(max, h);
                samples++;
            }
        }
        if (forced) {
            return variant.equals("stilts") ? Math.max(center, sea + 2) : center;
        }
        if (variant.equals("stilts")) {
            return center > sea + 6 ? null : sea + 2;              // planchers au-dessus de l'eau
        }
        int rough = switch (variant) {
            case "mine" -> 55;
            case "frontier", "refinery" -> 45;                     // badlands : relief tourmenté
            case "metropolis", "desert", "soviet" -> 38;
            default -> 40;
        };
        if (water * 3 > samples || center <= sea || max - min > rough) {
            return null;                                           // trop accidenté ou dans l'eau
        }
        return variant.equals("mine") ? min + 1 : center;
    }

    private static final java.util.Set<Long> PENDING = ConcurrentHashMap.newKeySet();

    /**
     * Comme {@link #cityAt}, mais sans jamais calculer sur le fil appelant (fil principal du serveur) : les régions pas
     * encore connues sont calculées en arrière-plan et seront disponibles au prochain appel.
     */
    public static City cityAtCached(Env e, int x, int z, int margin) {
        CityPlacement p = placement(e.reg());
        if (p == null) {
            return null;
        }
        Map<Long, Optional<City>> cache;
        synchronized (CACHE) {
            cache = CACHE.computeIfAbsent(e.gen(), g -> new ConcurrentHashMap<>());
        }
        int rx = Math.floorDiv(x >> 4, p.spacing()), rz = Math.floorDiv(z >> 4, p.spacing());
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                int ax = rx + di, az = rz + dj;
                long key = ChunkPos.asLong(ax, az) ^ e.seed() * 31;
                Optional<City> c = cache.get(key);
                if (c == null) {
                    if (PENDING.add(key)) {
                        net.minecraft.Util.backgroundExecutor().execute(() -> {
                            try {
                                city(e, ax, az);
                            } finally {
                                PENDING.remove(key);
                            }
                        });
                    }
                } else if (c.isPresent() && c.get().contains(x, z, margin)) {
                    return c.get();
                }
            }
        }
        return null;
    }

    /** La ville (s'il y en a une) qui couvre ce point, avec une marge en blocs. */
    public static City cityAt(Env e, int x, int z, int margin) {
        CityPlacement p = placement(e.reg());
        if (p == null) {
            return null;
        }
        int rx = Math.floorDiv(x >> 4, p.spacing()), rz = Math.floorDiv(z >> 4, p.spacing());
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                City c = city(e, rx + di, rz + dj);
                if (c != null && c.contains(x, z, margin)) {
                    return c;
                }
            }
        }
        return null;
    }

    private CityPlanner() {}
}

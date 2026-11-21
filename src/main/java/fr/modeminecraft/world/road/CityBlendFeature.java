package fr.modeminecraft.world.road;

import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptation douce des abords des villes (« terrain blending ») : sur un anneau de 24 blocs autour de chaque ville,
 * le relief naturel est raccordé progressivement au niveau des rues (les collines sont arasées en pente douce, les
 * creux comblés avec la terre du biome) : plus de falaises artificielles ni de murs de terre au bord des villes.
 * Le revêtement d'origine (herbe, sable, neige, terre cuite...) est conservé sur la nouvelle pente.
 */
public class CityBlendFeature extends Feature<NoneFeatureConfiguration> {
    public static final int RING = 24;

    public CityBlendFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel server = level.getLevel();
        var placement = CityPlanner.placement(level.registryAccess());
        if (server == null || placement == null) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(ctx.origin());
        CityPlanner.Env env = new CityPlanner.Env(level.getSeed(), ctx.chunkGenerator(), server.getChunkSource().randomState(), level, level.registryAccess());
        int x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ();
        List<CityPlanner.City> near = new ArrayList<>();
        int rx = Math.floorDiv(chunk.x, placement.spacing()), rz = Math.floorDiv(chunk.z, placement.spacing());
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                CityPlanner.City c = CityPlanner.city(env, rx + i, rz + j);
                if (c != null && !c.variant().equals("stilts")
                        && c.maxX() + RING >= x0 && c.minX() - RING <= x0 + 15 && c.maxZ() + RING >= z0 && c.minZ() - RING <= z0 + 15) {
                    near.add(c);
                }
            }
        }
        if (near.isEmpty()) {
            return false;
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                CityPlanner.City best = null;
                double bestD = Double.MAX_VALUE;
                boolean inside = false;
                for (CityPlanner.City c : near) {
                    double dx = Math.max(0, Math.max(c.minX() - x, x - c.maxX())), dz = Math.max(0, Math.max(c.minZ() - z, z - c.maxZ()));
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d == 0) {
                        inside = true;
                        break;
                    }
                    if (d < bestD) {
                        bestD = d;
                        best = c;
                    }
                }
                if (inside || best == null || bestD > RING) {
                    continue;
                }
                blend(level, m, x, z, best.baseY() - 1, bestD);
            }
        }
        return true;
    }

    private static boolean soft(BlockState s) {
        return s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced() && s.getFluidState().isEmpty();
    }

    private static void blend(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int z, int base, double d) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        int h = top;
        while (h > level.getMinBuildHeight() + 1 && soft(level.getBlockState(m.set(x, h, z)))) {
            h--;
        }
        BlockState ground = level.getBlockState(m.set(x, h, z));
        if (!ground.getFluidState().isEmpty()) {
            return;                                                    // lacs, rivières : on n'y touche pas
        }
        double f = d / RING;
        double w = f * f * (3 - 2 * f);                                // 0 au bord de la ville, 1 au bout de l'anneau
        int t = (int) Math.round(base + (h - base) * w);
        if (t == h || Math.abs(h - base) > 48) {
            return;                                                    // déjà raccordé, ou vraie montagne (on la laisse)
        }
        boolean snowy = level.getBlockState(m.set(x, h + 1, z)).is(Blocks.SNOW);
        BlockState surface = surfaceOf(ground);
        BlockState filler = fillerOf(ground, level.getBlockState(m.set(x, h - 1, z)));
        if (t < h) {
            // colline arasée : tout ce qui dépasse la pente disparaît (arbres compris)
            for (int y = t + 1; y <= Math.max(top, h) + 1; y++) {
                if (!level.getBlockState(m.set(x, y, z)).isAir()) {
                    level.setBlock(m, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            level.setBlock(m.set(x, t, z), surface, 2);
        } else {
            // creux comblé (les plantes au-dessus de l'ancien sol sont recouvertes)
            for (int y = h; y < t; y++) {
                level.setBlock(m.set(x, y, z), filler, 2);
            }
            level.setBlock(m.set(x, t, z), surface, 2);
            for (int y = t + 1; y <= t + 2; y++) {
                BlockState s = level.getBlockState(m.set(x, y, z));
                if (!s.isAir() && s.canBeReplaced() && s.getFluidState().isEmpty()) {
                    level.setBlock(m, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        if (snowy && level.getBlockState(m.set(x, t + 1, z)).isAir()) {
            level.setBlock(m, Blocks.SNOW.defaultBlockState(), 2);
        }
    }

    /** Revêtement de la nouvelle pente : celui du sol d'origine (la roche à nu reçoit de l'herbe si c'était de l'herbe). */
    private static BlockState surfaceOf(BlockState ground) {
        return ground.is(Blocks.DIRT) || ground.is(Blocks.ROOTED_DIRT) ? Blocks.GRASS_BLOCK.defaultBlockState() : ground;
    }

    private static BlockState fillerOf(BlockState ground, BlockState below) {
        if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.PODZOL) || ground.is(Blocks.MYCELIUM) || ground.is(Blocks.SNOW_BLOCK)
                || ground.is(Blocks.MOSS_BLOCK) || ground.is(Blocks.COARSE_DIRT)) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (ground.is(Blocks.MUD)) {
            return Blocks.MUD.defaultBlockState();
        }
        if (ground.is(BlockTags.SAND)) {
            return below.is(BlockTags.SAND) || below.isAir() ? ground : below;          // sable sur grès
        }
        if (ground.is(BlockTags.TERRACOTTA)) {
            return ground;
        }
        return below.isAir() || !below.getFluidState().isEmpty() ? ground : below;
    }
}

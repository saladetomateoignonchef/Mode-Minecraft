package fr.modeminecraft.world.rail;

import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit, chunk par chunk, les lignes du {@link RailNetwork} entre les villes frontières : ballast de gravier sur
 * remblai, rails orientés (courbes, rampes), tranchée de dégagement au-dessus, et pont à chevalets en bois sombre au-dessus
 * des ravins et de l'eau (piles tous les 8 blocs, garde-corps). Les voies à l'intérieur des villes existent déjà.
 */
public class RailFeature extends Feature<NoneFeatureConfiguration> {
    public RailFeature() {
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
        List<RailNetwork.Line> lines = RailNetwork.linesNear(env, chunk);
        if (lines.isEmpty()) {
            return false;
        }
        int cx0 = chunk.getMinBlockX(), cz0 = chunk.getMinBlockZ();
        List<CityPlanner.City> cities = new ArrayList<>();
        int rx = Math.floorDiv(chunk.x, placement.spacing()), rz = Math.floorDiv(chunk.z, placement.spacing());
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                CityPlanner.City c = CityPlanner.city(env, rx + i, rz + j);
                if (c != null) {
                    cities.add(c);
                }
            }
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (RailNetwork.Line line : lines) {
            int[][] cells = line.cells();
            for (int i = 0; i < cells.length; i++) {
                int x = cells[i][0], z = cells[i][1], y = cells[i][2];
                if (x < cx0 || x >= cx0 + 16 || z < cz0 || z >= cz0 + 16) {
                    continue;
                }
                boolean inCity = false;
                for (CityPlanner.City c : cities) {
                    inCity |= c.contains(x, z, 0);
                }
                if (inCity) {
                    continue;
                }
                int[] prev = i > 0 ? cells[i - 1] : null, next = i + 1 < cells.length ? cells[i + 1] : null;
                Direction pd = prev == null ? null : dir(prev[0] - x, prev[1] - z), nd = next == null ? null : dir(next[0] - x, next[1] - z);
                if (pd == null && nd != null) pd = nd.getOpposite();
                if (nd == null && pd != null) nd = pd.getOpposite();
                if (pd == null) continue;
                RailShape shape = shape(pd, nd, prev == null ? y : prev[2], next == null ? y : next[2], y);
                any = true;
                int ground = groundY(level, m, x, y + 24, z);
                clear(level, m, x, y, z);
                boolean bridge = ground < y - 3;
                boolean straight = pd.getAxis() == nd.getAxis();
                // ballast : la cellule et, sur les alignements droits, ses deux voisines latérales
                setBed(level, m, x, y - 1, z, ground, bridge, i);
                if (straight) {
                    Direction side = pd.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
                    for (int s = -1; s <= 1; s += 2) {
                        int sx = x + side.getStepX() * s, sz = z + side.getStepZ() * s;
                        if (sx < cx0 || sx >= cx0 + 16 || sz < cz0 || sz >= cz0 + 16) {
                            continue;
                        }
                        clear(level, m, sx, y, sz);
                        setBed(level, m, sx, y - 1, sz, groundY(level, m, sx, y + 24, sz), bridge, i);
                        if (bridge) {
                            level.setBlock(m.set(sx, y, sz), Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);       // garde-corps
                        }
                    }
                }
                level.setBlock(m.set(x, y, z), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape), 2);
                // chevalets : piles de bois sombre tous les 8 blocs
                if (bridge && i % 8 == 0) {
                    for (int yy = y - 2; yy > ground && yy > level.getMinBuildHeight(); yy--) {
                        level.setBlock(m.set(x, yy, z), Blocks.DARK_OAK_LOG.defaultBlockState(), 2);
                    }
                }
            }
        }
        return any;
    }

    private static Direction dir(int dx, int dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Forme du rail selon les directions vers la cellule précédente et suivante, et la pente. */
    static RailShape shape(Direction pd, Direction nd, int prevY, int nextY, int y) {
        if (pd.getAxis() == nd.getAxis()) {
            boolean x = pd.getAxis() == Direction.Axis.X;
            if (nextY > y) return ascending(nd);
            if (prevY > y) return ascending(pd);
            return x ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        }
        boolean south = pd == Direction.SOUTH || nd == Direction.SOUTH, north = pd == Direction.NORTH || nd == Direction.NORTH;
        boolean east = pd == Direction.EAST || nd == Direction.EAST;
        if (south) return east ? RailShape.SOUTH_EAST : RailShape.SOUTH_WEST;
        if (north) return east ? RailShape.NORTH_EAST : RailShape.NORTH_WEST;
        return RailShape.NORTH_SOUTH;
    }

    private static RailShape ascending(Direction d) {
        return switch (d) {
            case EAST -> RailShape.ASCENDING_EAST;
            case WEST -> RailShape.ASCENDING_WEST;
            case NORTH -> RailShape.ASCENDING_NORTH;
            default -> RailShape.ASCENDING_SOUTH;
        };
    }

    private static void setBed(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int y, int z, int ground, boolean bridge, int i) {
        if (bridge) {
            level.setBlock(m.set(x, y, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
            return;
        }
        level.setBlock(m.set(x, y, z), Blocks.GRAVEL.defaultBlockState(), 2);
        for (int yy = y - 1; yy > ground; yy--) {                                      // remblai
            level.setBlock(m.set(x, yy, z), Blocks.RED_SANDSTONE.defaultBlockState(), 2);
        }
    }

    /** Dégage 5 blocs au-dessus du rail (tranchée) : terrain, arbres, plantes. */
    private static void clear(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int y, int z) {
        for (int yy = y; yy <= y + 5; yy++) {
            BlockState s = level.getBlockState(m.set(x, yy, z));
            if (!s.isAir() && !s.is(Blocks.RAIL)) {
                level.setBlock(m, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        for (int yy = y + 6; yy <= y + 14; yy++) {
            BlockState s = level.getBlockState(m.set(x, yy, z));
            if (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS)) {
                level.setBlock(m, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static int groundY(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int from, int z) {
        for (int y = from; y > level.getMinBuildHeight(); y--) {
            BlockState s = level.getBlockState(m.set(x, y, z));
            if (s.isAir() || !s.getFluidState().isEmpty() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced()) {
                continue;
            }
            return y;
        }
        return level.getMinBuildHeight();
    }
}

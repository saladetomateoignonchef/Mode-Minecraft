package fr.modeminecraft.world.road;

import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit, chunk par chunk, les routes du {@link RoadNetwork} : revêtement selon le biome (asphalte à ligne blanche
 * en plaine, grès pavé au désert, route gravillonnée enneigée...), dalles dans les pentes, remblai sous la chaussée
 * dans les petits creux, et pont à piliers jusqu'au sol (même dans l'eau) au-dessus des ravins et des rivières :
 * aucune route ne flotte. Au-dessus de la chaussée, le terrain est dégagé (tranchée) et les arbres coupés.
 */
public class RoadFeature extends Feature<NoneFeatureConfiguration> {
    public RoadFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /**
     * Matériaux d'une famille de route, et style de ses ponts (cahier des charges) :
     * plaines = pont routier en béton, rambardes métalliques et lampadaires ; désert = grès massif à arches ;
     * forêt / taïga = rondins et poutres d'acier ; neige = viaduc soviétique en béton brut et métal rouillé ;
     * jungle = pont suspendu en bois exotique et câbles ; mesa = pont à chevalets en bois massif (Far West).
     */
    record Style(String kind, int halfWidth, BlockState surface, BlockState slab, BlockState edge, BlockState line, BlockState base,
                 BlockState pillar, BlockState rail, BlockState arch) {}

    static Style style(String kind) {
        return switch (kind) {
            case "desert" -> new Style(kind, 2, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SMOOTH_SANDSTONE_SLAB.defaultBlockState(),
                    Blocks.CUT_SANDSTONE.defaultBlockState(), null, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(),
                    Blocks.SANDSTONE_WALL.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState());
            case "snow" -> new Style(kind, 2, Blocks.POLISHED_ANDESITE.defaultBlockState(), Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState(),
                    Blocks.GRAVEL.defaultBlockState(), null, Blocks.STONE.defaultBlockState(), Blocks.GRAY_CONCRETE.defaultBlockState(),
                    Blocks.IRON_BARS.defaultBlockState(), Blocks.EXPOSED_CUT_COPPER.defaultBlockState());
            case "forest" -> new Style(kind, 2, Blocks.DIRT_PATH.defaultBlockState(), Blocks.SPRUCE_SLAB.defaultBlockState(),
                    Blocks.COARSE_DIRT.defaultBlockState(), null, Blocks.DIRT.defaultBlockState(), Blocks.SPRUCE_LOG.defaultBlockState(),
                    Blocks.SPRUCE_FENCE.defaultBlockState(), Blocks.IRON_BARS.defaultBlockState());
            case "jungle" -> new Style(kind, 2, Blocks.PACKED_MUD.defaultBlockState(), Blocks.MUD_BRICK_SLAB.defaultBlockState(),
                    Blocks.MUD_BRICKS.defaultBlockState(), null, Blocks.DIRT.defaultBlockState(), Blocks.JUNGLE_LOG.defaultBlockState(),
                    Blocks.JUNGLE_FENCE.defaultBlockState(), null);
            case "mesa" -> new Style(kind, 2, Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState(), Blocks.SMOOTH_RED_SANDSTONE_SLAB.defaultBlockState(),
                    Blocks.CUT_RED_SANDSTONE.defaultBlockState(), null, Blocks.RED_SANDSTONE.defaultBlockState(), Blocks.DARK_OAK_LOG.defaultBlockState(),
                    Blocks.DARK_OAK_FENCE.defaultBlockState(), Blocks.DARK_OAK_FENCE.defaultBlockState());
            default -> new Style(kind, 3, Blocks.BLACK_CONCRETE.defaultBlockState(), Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState(),
                    Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(),
                    Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), Blocks.IRON_BARS.defaultBlockState(), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
        };
    }

    /** Une colonne de chaussée : distance à l'axe, abscisse curviligne, altitude du revêtement. */
    private record Cell(double off, double s, double y, Style style) {}

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel server = level.getLevel();
        if (server == null) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(ctx.origin());
        CityPlanner.Env env = new CityPlanner.Env(level.getSeed(), ctx.chunkGenerator(), server.getChunkSource().randomState(), level, level.registryAccess());
        List<RoadNetwork.Path> paths = RoadNetwork.pathsNear(env, chunk);
        if (paths.isEmpty()) {
            return false;
        }
        int cx0 = chunk.getMinBlockX(), cz0 = chunk.getMinBlockZ();
        // villes voisines : la route s'arrête à leur bord
        List<CityPlanner.City> cities = new ArrayList<>();
        var placement = CityPlanner.placement(level.registryAccess());
        int rx = Math.floorDiv(chunk.x, placement.spacing()), rz = Math.floorDiv(chunk.z, placement.spacing());
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                CityPlanner.City c = CityPlanner.city(env, rx + i, rz + j);
                if (c != null) cities.add(c);
            }
        }
        Map<Long, Cell> cells = new HashMap<>();
        List<double[]> pillars = new ArrayList<>();
        for (RoadNetwork.Path p : paths) {
            Style st = style(p.kind());
            double reach = st.halfWidth() + 1.5;
            for (int k = 0; k < p.size(); k++) {
                double x = p.xs()[k], z = p.zs()[k];
                if (x < cx0 - reach || x > cx0 + 16 + reach || z < cz0 - reach || z > cz0 + 16 + reach) {
                    continue;
                }
                int k2 = Math.min(p.size() - 1, k + 1), k1 = Math.max(0, k - 1);
                double tx = p.xs()[k2] - p.xs()[k1], tz = p.zs()[k2] - p.zs()[k1];
                double tl = Math.max(1e-6, Math.hypot(tx, tz));
                double nx = -tz / tl, nz = tx / tl;
                for (double o = -st.halfWidth() - 0.5; o <= st.halfWidth() + 0.5; o += 0.5) {
                    int bx = (int) Math.floor(x + nx * o), bz = (int) Math.floor(z + nz * o);
                    if (bx < cx0 || bx >= cx0 + 16 || bz < cz0 || bz >= cz0 + 16) {
                        continue;
                    }
                    long key = BlockPos.asLong(bx, 0, bz);
                    Cell old = cells.get(key);
                    if (old == null || Math.abs(o) < Math.abs(old.off())) {
                        cells.put(key, new Cell(o, k * RoadNetwork.STEP, p.ys()[k], st));
                    }
                }
                // piliers de pont tous les 10 blocs, sous les deux bords
                if (k % 20 == 0) {
                    for (int side = -1; side <= 1; side += 2) {
                        pillars.add(new double[]{x + nx * side * (st.halfWidth() - 0.5), z + nz * side * (st.halfWidth() - 0.5), p.ys()[k]});
                    }
                }
            }
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (var e : cells.entrySet()) {
            int x = BlockPos.getX(e.getKey()), z = BlockPos.getZ(e.getKey());
            boolean inCity = false;
            for (CityPlanner.City c : cities) {
                if (c.contains(x, z, 0)) {
                    inCity = true;
                    break;
                }
            }
            if (inCity) {
                continue;
            }
            Cell c = e.getValue();
            Style st = c.style();
            int y = (int) Math.floor(c.y());
            boolean half = c.y() - y >= 0.5;
            boolean edge = Math.abs(c.off()) > st.halfWidth() - 0.25;
            int ground = groundY(level, m, x, y + 24, z);
            // dégagement au-dessus de la chaussée : tranchée (sauf très profonde : on garde un tunnel de 5 de haut)
            int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z);
            int clearTo = top - y <= 14 ? Math.max(top, y + 5) : y + 5;
            for (int yy = y + 1; yy <= clearTo + 1; yy++) {
                BlockState s = level.getBlockState(m.set(x, yy, z));
                if (!s.isAir() && (yy <= y + 5 || yy <= clearTo || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS))) {
                    level.setBlock(m, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            for (int yy = clearTo + 2; yy <= y + 16; yy++) {                       // houppiers d'arbres coupés
                BlockState s = level.getBlockState(m.set(x, yy, z));
                if (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS)) {
                    level.setBlock(m, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            boolean bridge = ground < y - 3;
            BlockState surface = st.surface();
            if (edge) {
                surface = bridge ? st.pillar() : st.edge();
            } else if (st.line() != null && Math.abs(c.off()) < 0.5 && (c.s() % 6) < 3) {
                surface = st.line();                                                     // ligne médiane pointillée
            }
            if (surface.is(Blocks.GRAVEL) || surface.is(Blocks.DIRT_PATH) && bridge) {
                surface = bridge ? st.pillar() : surface;
            }
            level.setBlock(m.set(x, y, z), surface, 2);
            if (half) {
                level.setBlock(m.set(x, y + 1, z), st.slab().setValue(SlabBlock.TYPE, SlabType.BOTTOM), 2);
            }
            if (bridge) {
                int ry = y + (half ? 2 : 1);
                if (edge) {
                    level.setBlock(m.set(x, ry, z), st.rail(), 2);                       // garde-corps
                }
                level.setBlock(m.set(x, y - 1, z), st.pillar(), 2);                    // tablier
                bridgeDeco(level, m, x, y, z, ry, ground, c, edge);
            } else {
                for (int yy = y - 1; yy > ground; yy--) {                                // remblai
                    level.setBlock(m.set(x, yy, z), st.base(), 2);
                }
            }
        }
        // piles de pont jusqu'au sol (à travers l'eau)
        for (double[] p : pillars) {
            int x = (int) Math.floor(p[0]), z = (int) Math.floor(p[1]);
            if (x < cx0 || x >= cx0 + 16 || z < cz0 || z >= cz0 + 16) {
                continue;
            }
            Cell c = cells.get(BlockPos.asLong(x, 0, z));
            if (c == null) {
                continue;
            }
            int y = (int) Math.floor(c.y());
            int ground = groundY(level, m, x, y - 2, z);
            if (ground >= y - 3) {
                continue;
            }
            boolean inCity = false;
            for (CityPlanner.City city : cities) {
                inCity |= city.contains(x, z, 0);
            }
            if (inCity) continue;
            for (int yy = y - 2; yy > ground && yy > level.getMinBuildHeight(); yy--) {
                level.setBlock(m.set(x, yy, z), c.style().pillar(), 2);
            }
        }
        return true;
    }

    /** Décor des ponts selon le biome : arches sous le tablier, lampadaires, pylônes et câbles, chevalets. */
    private static void bridgeDeco(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int y, int z, int ry, int ground, Cell c, boolean edge) {
        Style st = c.style();
        double q = c.s() % 10;
        // arches (ou contreventements) entre les piles, tous les 10 blocs
        if (st.arch() != null) {
            int depth = q < 1.5 || q >= 8.5 ? 2 : q < 2.5 || q >= 7.5 ? 1 : 0;
            if (st.kind().equals("mesa") || st.kind().equals("forest")) {
                // chevalets / poutres d'acier : croisillons en X entre les piles
                double u = Math.abs(q - 5) / 5;                           // 1 aux piles, 0 au milieu
                depth = 0;
                int yy = y - 2 - (int) Math.round((1 - u) * 3);
                if (yy > ground && level.getBlockState(m.set(x, yy, z)).isAir()) {
                    level.setBlock(m, st.arch(), 2);
                }
                yy = y - 2 - (int) Math.round(u * 3);
                if (yy > ground && level.getBlockState(m.set(x, yy, z)).isAir()) {
                    level.setBlock(m, st.arch(), 2);
                }
            }
            for (int k = 1; k <= depth; k++) {
                if (y - 1 - k > ground && level.getBlockState(m.set(x, y - 1 - k, z)).isAir()) {
                    level.setBlock(m, st.arch(), 2);
                }
            }
        }
        if (!edge) {
            return;
        }
        switch (st.kind()) {
            case "plains" -> {
                if (c.s() % 16 < 0.6) {                                   // lampadaire tous les 16 blocs
                    level.setBlock(m.set(x, ry + 1, z), Blocks.IRON_BARS.defaultBlockState(), 2);
                    level.setBlock(m.set(x, ry + 2, z), Blocks.IRON_BARS.defaultBlockState(), 2);
                    level.setBlock(m.set(x, ry + 3, z), fr.modeminecraft.registry.ModBlocks.STREET_LANTERN.get().defaultBlockState(), 2);
                }
            }
            case "desert" -> {
                if (q < 0.6) {
                    level.setBlock(m.set(x, ry, z), Blocks.CHISELED_SANDSTONE.defaultBlockState(), 2);   // colonnettes
                    level.setBlock(m.set(x, ry + 1, z), Blocks.SANDSTONE_SLAB.defaultBlockState(), 2);
                }
            }
            case "snow" -> {
                if (c.s() % 20 < 0.6) {                                   // poutres de rive en métal rouillé
                    level.setBlock(m.set(x, ry, z), Blocks.EXPOSED_CUT_COPPER.defaultBlockState(), 2);
                    level.setBlock(m.set(x, ry + 1, z), Blocks.LIGHTNING_ROD.defaultBlockState(), 2);
                }
            }
            case "jungle" -> {
                // pont suspendu : pylônes tous les 20 blocs, câbles en chaînette entre eux
                double p = c.s() % 20;
                if (p < 0.6) {
                    for (int k = 0; k <= 4; k++) {
                        level.setBlock(m.set(x, ry + k, z), Blocks.JUNGLE_LOG.defaultBlockState(), 2);
                    }
                } else {
                    double u = (p - 10) / 10;
                    int h = (int) Math.round(1 + 3.4 * u * u);
                    // câble porteur à la hauteur h, suspentes verticales tous les 4 blocs
                    for (int k = p % 4 < 0.6 ? 1 : h; k <= h; k++) {
                        level.setBlock(m.set(x, ry + k, z), Blocks.CHAIN.defaultBlockState(), 2);
                    }
                }
            }
            default -> {}
        }
    }

    /** Sol solide sous la route (on ignore l'eau, les feuilles, les troncs et les plantes). */
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

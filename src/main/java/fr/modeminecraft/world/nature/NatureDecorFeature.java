package fr.modeminecraft.world.nature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.modeminecraft.registry.ModBlocks;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;

import java.util.List;
import java.util.Map;

/**
 * Décors de nature hors des villes et des factions : un petit groupe d'objets de la série « nature » (troncs creux, cairns, squelettes,
 * pontons, coquillages, igloos, nids, affûts, meules, tentes, ruines, menhirs, filons, épaves...) posé au sol selon le thème du biome.
 * Les villes (et une marge de 24 blocs) sont évitées. Le thème « mine » cherche le sol d'une grotte ; les thèmes « river » et « beach »
 * demandent de l'eau à proximité.
 */
public class NatureDecorFeature extends Feature<NatureDecorFeature.Config> {
    public record Config(String theme, int tries) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("theme").forGetter(Config::theme),
                Codec.intRange(1, 8).fieldOf("tries").forGetter(Config::tries)).apply(i, Config::new));
    }

    /** Objets de chaque thème (identifiants de blocs de la série de nature). */
    static final Map<String, List<String>> THEMES = Map.ofEntries(
            Map.entry("forest", List.of("hollow_log", "giant_mushrooms", "mossy_stump", "fallen_branches", "forest_fern_patch", "log_pile_stack")),
            Map.entry("mountain", List.of("stone_cairn", "erratic_boulder", "crystal_cluster", "scree_pile", "rock_arch_small", "mountain_spire")),
            Map.entry("desert", List.of("dune_skeleton", "saguaro_cactus", "tumbleweed", "bleached_skull", "wind_rippled_dune", "wild_caravan_well")),
            Map.entry("swamp", List.of("giant_lily_pads", "mangrove_roots", "swamp_fungus_stump", "giant_jungle_flower", "mud_pit", "stilt_hut_small")),
            Map.entry("river", List.of("wooden_jetty", "moored_rowboat", "drying_fishing_net", "reed_clump", "small_water_mill", "stepping_stones")),
            Map.entry("beach", List.of("shell_pile", "driftwood_pile", "beach_crab_rocks", "seaweed_heap", "sandcastle", "beached_buoy")),
            Map.entry("snow", List.of("roadside_snowman", "ice_stalagmites", "snow_igloo_small", "frozen_geyser", "snow_footprints", "icicle_cluster")),
            Map.entry("sky", List.of("meteorite_crater", "fulgurite_glass", "eagle_aerie", "plains_tipi", "wind_banner_pole", "rain_gauge_post")),
            Map.entry("fauna", List.of("burrow_mound", "ground_bird_nest", "animal_tracks", "carcass_bones", "bee_skep_wild", "spider_web_cocoons")),
            Map.entry("forage", List.of("hunter_blind", "pelt_drying_rack", "berry_basket", "snare_trap", "fish_smoker", "mushroom_baskets")),
            Map.entry("farm", List.of("hay_bale_roll", "scarecrow_field", "feed_trough", "rustic_fence_section", "wind_pump", "vegetable_patch")),
            Map.entry("camp", List.of("hiker_tent", "camp_fire_ring", "trail_marker_post", "tree_hammock", "rope_bridge_anchor", "refuge_bench")),
            Map.entry("ruins", List.of("broken_column", "overgrown_temple_arch", "old_tomb_slab", "mossy_statue_head", "dry_stone_wall", "ruined_well")),
            Map.entry("magic", List.of("menhir_ring", "runic_altar", "sacred_tree_shrine", "ancient_portal_arch", "firefly_swarm", "witch_cauldron_wild")),
            Map.entry("mine", List.of("ore_vein_rock", "mine_cart_wreck", "timber_support", "cave_glow_crystals", "stalagmite_cluster", "miner_lantern_hook")),
            Map.entry("arid_flora", List.of("agave_plant", "flowering_cactus_red", "flowering_cactus_yellow", "barrel_cactus", "dry_shrub", "succulent_rosette")),
            Map.entry("shore_flora", List.of("cattail_tall", "reed_bank", "marsh_marigold", "water_iris")),
            Map.entry("pond_flora", List.of("pink_lily_pad", "white_lily_pad")),
            Map.entry("wreck", List.of("crashed_plane_wing", "rusted_car_wreck", "airdrop_crate", "tangled_parachute", "distress_beacon", "shipwreck_ribs")));

    public NatureDecorFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel server = level.getLevel();
        List<String> ids = THEMES.get(ctx.config().theme());
        if (server == null || ids == null) {
            return false;
        }
        RandomSource rnd = ctx.random();
        BlockPos origin = ctx.origin();
        String theme = ctx.config().theme();
        CityPlanner.Env env = new CityPlanner.Env(level.getSeed(), ctx.chunkGenerator(), server.getChunkSource().randomState(), level, level.registryAccess());
        if (CityPlanner.cityAt(env, origin.getX(), origin.getZ(), 24) != null) {
            return false;                                                   // pas dans une ville ni à ses abords
        }
        boolean any = false;
        for (int i = 0; i < ctx.config().tries(); i++) {
            BlockPos p = origin.offset(rnd.nextInt(11) - 5, 0, rnd.nextInt(11) - 5);
            if (!level.hasChunkAt(p) || !inChunkNeighbourhood(origin, p)) {
                continue;
            }
            BlockPos spot = theme.equals("mine") ? caveFloor(level, p, rnd) : surface(level, p);
            if (spot == null || !(theme.equals("pond_flora") ? onCalmWater(level, spot) : valid(level, spot, theme))) {
                continue;
            }
            Block block = ModBlocks.realism(ids.get(rnd.nextInt(ids.size())));
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(rnd);
            BlockState state = block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
            level.setBlock(spot, state, 2);
            any = true;
        }
        return any;
    }

    /** Les blocs voisins doivent être dans la zone de 3 x 3 chunks accessible pendant la génération. */
    private static boolean inChunkNeighbourhood(BlockPos origin, BlockPos p) {
        return Math.abs((p.getX() >> 4) - (origin.getX() >> 4)) <= 1 && Math.abs((p.getZ() >> 4) - (origin.getZ() >> 4)) <= 1;
    }

    /** Première case d'air au-dessus du sol (heightmap de surface). */
    private static BlockPos surface(WorldGenLevel level, BlockPos p) {
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, p.getX(), p.getZ());
        return new BlockPos(p.getX(), y, p.getZ());
    }

    /** Sol d'une grotte : case d'air avec un bloc plein dessous et de l'air au-dessus, entre y = -48 et la surface - 10. */
    private static BlockPos caveFloor(WorldGenLevel level, BlockPos p, RandomSource rnd) {
        int top = Math.min(level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, p.getX(), p.getZ()) - 10, 40);
        int start = -48 + rnd.nextInt(Math.max(1, top + 48));
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(p.getX(), start, p.getZ());
        for (int y = start; y > -48; y--) {
            m.setY(y);
            if (level.getBlockState(m).isAir() && level.getBlockState(m.above()).isAir() && level.getBlockState(m.above(2)).isAir()
                    && level.getBlockState(m.below()).isFaceSturdy(level, m.below(), Direction.UP) && level.getFluidState(m).isEmpty()) {
                return m.immutable();
            }
        }
        return null;
    }

    /** Nénuphars : la case est de l'air posé sur une source d'eau, sans courant, avec de l'air au-dessus. */
    private static boolean onCalmWater(WorldGenLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        var fluid = level.getFluidState(below);
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() && fluid.is(FluidTags.WATER) && fluid.isSource()
                && level.getFluidState(pos).isEmpty();
    }

    private static boolean valid(WorldGenLevel level, BlockPos pos, String theme) {
        BlockState here = level.getBlockState(pos);
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (!(here.isAir() || here.canBeReplaced()) || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        if (!ground.isFaceSturdy(level, below, Direction.UP) || ground.is(BlockTags.LEAVES) || !level.getFluidState(below).isEmpty()) {
            return false;
        }
        if (!level.getBlockState(pos.above()).isAir() && !level.getBlockState(pos.above()).canBeReplaced()) {
            return false;
        }
        if (theme.equals("river") || theme.equals("beach") || theme.equals("shore_flora")) {
            return waterNear(level, below, 6);
        }
        return true;
    }

    private static boolean waterNear(WorldGenLevel level, BlockPos p, int r) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx += 2) {
            for (int dz = -r; dz <= r; dz += 2) {
                m.set(p.getX() + dx, p.getY(), p.getZ() + dz);
                if (level.hasChunkAt(m) && level.getFluidState(m).is(FluidTags.WATER)) {
                    return true;
                }
            }
        }
        return false;
    }
}

package fr.modeminecraft.world.night;

import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Régime nocturne des villes (de 13 000 à 23 000 ticks) :
 * - lampadaires, néons, enseignes et projecteurs s'allument la nuit (l'éclairage intérieur reste allumé le jour) ;
 * - pannes de secteur : un transformateur saboté éteint tout dans un rayon de 60 blocs pendant 5 minutes.
 */
public final class NightOps {
    public static final int BLACKOUT_RADIUS = 60, BLACKOUT_TICKS = 6000;

    private record Blackout(BlockPos center, int radius, long end) {}

    private static final Map<ResourceKey<Level>, List<Blackout>> BLACKOUTS = new ConcurrentHashMap<>();

    public static boolean isNight(Level level) {
        long t = Math.floorMod(level.getDayTime(), 24000L);
        return t >= 13000 && t < 23000;
    }

    /** Panne de secteur autour de ce point. */
    public static void blackout(ServerLevel level, BlockPos center, int radius, int ticks) {
        List<Blackout> list = BLACKOUTS.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
        synchronized (list) {
            list.add(new Blackout(center.immutable(), radius, level.getGameTime() + ticks));
        }
    }

    /** Ce point est-il privé de courant ? */
    public static boolean blackedOut(Level level, BlockPos pos) {
        List<Blackout> list = BLACKOUTS.get(level.dimension());
        if (list == null) {
            return false;
        }
        long now = level.getGameTime();
        synchronized (list) {
            list.removeIf(b -> b.end <= now);
            for (Blackout b : list) {
                if (b.center.distSqr(pos) <= (double) b.radius * b.radius) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Ce point est-il en plein air (lampadaire, néon de toit) ou à l'intérieur d'un bâtiment ? */
    public static boolean outdoor(Level level, BlockPos pos) {
        return pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) - 3;
    }

    /** Un éclairage de ville doit-il être allumé ici ? */
    public static boolean lightsOn(Level level, BlockPos pos) {
        return !blackedOut(level, pos) && (isNight(level) || !outdoor(level, pos));
    }

    /** Lampes de ville : vérifie l'état toutes les 1 à 2 secondes (la panne se propage en cascade). */
    public static void tickLight(BlockState state, ServerLevel level, BlockPos pos, Block block, RandomSource random) {
        boolean want = lightsOn(level, pos);
        if (state.getValue(BlockStateProperties.LIT) != want) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, want), Block.UPDATE_CLIENTS);
        }
        level.scheduleTick(pos, block, 20 + random.nextInt(16));
    }

    /** Génération des villes : remplace l'éclairage vanilla par l'éclairage "intelligent" de la ville. */
    public static BlockState cityLight(BlockState s) {
        if (s.is(Blocks.SEA_LANTERN)) {
            return ModBlocks.CITY_NEON.get().defaultBlockState();
        }
        if (s.is(Blocks.GLOWSTONE) || s.is(Blocks.REDSTONE_LAMP)) {
            return ModBlocks.CITY_LAMP.get().defaultBlockState();
        }
        if (s.is(Blocks.LANTERN)) {
            return ModBlocks.STREET_LANTERN.get().defaultBlockState().setValue(BlockStateProperties.HANGING, s.getValue(BlockStateProperties.HANGING));
        }
        return s;
    }

    public static boolean isCityLight(BlockState s) {
        return s.getBlock() instanceof CityLightBlock || s.getBlock() instanceof StreetLanternBlock;
    }

    private NightOps() {}
}

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.tags.FluidTags;

/**
 * Sources chaudes et geysers des biomes froids et montagneux : bassin d'eau à fond turquoise bordé de travertin (calcite, tuf, diorite) avec un
 * évent à vapeur, ou petit cône de travertin couronné d'un évent (geyser). Les joueurs qui se baignent récupèrent doucement de la vie
 * (voir {@link fr.modeminecraft.block.nature.SteamVentBlock}).
 */
public class HotSpringFeature extends Feature<HotSpringFeature.Config> {
    public record Config(boolean geyser) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.fieldOf("geyser").forGetter(Config::geyser)).apply(i, Config::new));
    }

    public HotSpringFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel server = level.getLevel();
        if (server == null) {
            return false;
        }
        BlockPos origin = ctx.origin();
        CityPlanner.Env env = new CityPlanner.Env(level.getSeed(), ctx.chunkGenerator(), server.getChunkSource().randomState(), level, level.registryAccess());
        if (CityPlanner.cityAt(env, origin.getX(), origin.getZ(), 24) != null) {
            return false;
        }
        int gy = origin.getY() - 1;                                          // sol sous le point d'origine
        BlockState ground = level.getBlockState(origin.below());
        if (!ground.isFaceSturdy(level, origin.below(), Direction.UP) || !level.getFluidState(origin.below()).isEmpty() || !level.getFluidState(origin).isEmpty()) {
            return false;
        }
        RandomSource rnd = ctx.random();
        return ctx.config().geyser() ? geyser(level, origin, gy, rnd) : spring(level, origin, gy, rnd);
    }

    private static boolean near(WorldGenLevel level, BlockPos origin, BlockPos p) {
        return level.hasChunkAt(p) && Math.abs((p.getX() >> 4) - (origin.getX() >> 4)) <= 1 && Math.abs((p.getZ() >> 4) - (origin.getZ() >> 4)) <= 1;
    }

    private static BlockState travertine(RandomSource rnd) {
        int r = rnd.nextInt(10);
        return (r < 5 ? Blocks.CALCITE : r < 8 ? Blocks.TUFF : Blocks.DIORITE).defaultBlockState();
    }

    private static void clearAbove(WorldGenLevel level, BlockPos origin, int x, int y, int z, int h) {
        for (int i = 1; i <= h; i++) {
            BlockPos p = new BlockPos(x, y + i, z);
            if (near(level, origin, p) && !level.getBlockState(p).isAir()) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static void fillDown(WorldGenLevel level, BlockPos origin, int x, int y, int z, RandomSource rnd) {
        for (int i = 0; i < 6; i++) {
            BlockPos p = new BlockPos(x, y - i, z);
            if (!near(level, origin, p) || level.getBlockState(p).isFaceSturdy(level, p, Direction.UP) && level.getFluidState(p).isEmpty()) {
                return;
            }
            level.setBlock(p, travertine(rnd), 2);
        }
    }

    private static void vent(WorldGenLevel level, BlockPos pos, RandomSource rnd) {
        Block block = ModBlocks.realism("hot_spring_vent");
        level.setBlock(pos, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(rnd)), 2);
        level.scheduleTick(pos, block, 40);
    }

    private boolean spring(WorldGenLevel level, BlockPos origin, int gy, RandomSource rnd) {
        int a = 3 + rnd.nextInt(3), b = 2 + rnd.nextInt(3);
        BlockPos ventPos = null;
        for (int dx = -a - 2; dx <= a + 2; dx++) {
            for (int dz = -b - 2; dz <= b + 2; dz++) {
                double d = (double) dx * dx / (a * a) + (double) dz * dz / (b * b) + (rnd.nextDouble() - 0.5) * 0.18;
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                if (!near(level, origin, new BlockPos(x, gy, z))) {
                    continue;
                }
                if (d <= 1.0) {                                               // bassin : eau sur fond turquoise
                    level.setBlock(new BlockPos(x, gy, z), Blocks.WATER.defaultBlockState(), 2);
                    level.setBlock(new BlockPos(x, gy - 1, z), Blocks.WATER.defaultBlockState(), 2);
                    level.setBlock(new BlockPos(x, gy - 2, z), Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(), 2);
                    clearAbove(level, origin, x, gy, z, 4);
                } else if (d <= 1.9) {                                        // margelle de travertin
                    level.setBlock(new BlockPos(x, gy, z), travertine(rnd), 2);
                    fillDown(level, origin, x, gy - 1, z, rnd);
                    clearAbove(level, origin, x, gy, z, 3);
                    if (ventPos == null && dx >= a && Math.abs(dz) <= 1) {
                        ventPos = new BlockPos(x, gy + 1, z);
                    }
                }
            }
        }
        if (ventPos == null) {
            ventPos = origin.offset(a + 1, 0, 0);
        }
        vent(level, ventPos, rnd);
        return true;
    }

    private boolean geyser(WorldGenLevel level, BlockPos origin, int gy, RandomSource rnd) {
        int r = 2 + rnd.nextInt(2);
        int top = gy;
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                if (!near(level, origin, new BlockPos(x, gy, z))) {
                    continue;
                }
                if (d <= r + 0.5) {
                    int h = (int) Math.round(r + 0.5 - d) + 1;               // cône : plus haut au centre
                    for (int i = 0; i < h; i++) {
                        level.setBlock(new BlockPos(x, gy + i, z), travertine(rnd), 2);
                    }
                    clearAbove(level, origin, x, gy + h - 1, z, 3);
                    fillDown(level, origin, x, gy - 1, z, rnd);
                    top = Math.max(top, gy + h - 1);
                }
            }
        }
        vent(level, new BlockPos(origin.getX(), top + 1, origin.getZ()), rnd);
        return true;
    }
}

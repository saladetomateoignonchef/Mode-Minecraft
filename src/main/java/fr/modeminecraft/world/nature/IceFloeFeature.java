package fr.modeminecraft.world.nature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Banquise fracturée et icebergs des océans froids : plaques de glace compacte flottantes, aux bords irréguliers et traversées de fissures, et
 * parfois un iceberg de glace bleue dont la partie immergée est plus grande que la partie visible. Ce sont des obstacles pour la navigation.
 */
public class IceFloeFeature extends Feature<IceFloeFeature.Config> {
    public record Config(int maxRadius) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(3, 12).fieldOf("max_radius").forGetter(Config::maxRadius)).apply(i, Config::new));
    }

    public IceFloeFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();                                      // première case d'air au-dessus de l'eau
        RandomSource rnd = ctx.random();
        if (!level.getFluidState(origin.below()).is(FluidTags.WATER) || !level.getFluidState(origin.below(6)).is(FluidTags.WATER)
                || !level.getBlockState(origin).isAir()) {
            return false;                                                    // eau assez profonde seulement
        }
        int maxR = ctx.config().maxRadius();
        int a = 3 + rnd.nextInt(Math.max(1, maxR - 2)), b = 3 + rnd.nextInt(Math.max(1, maxR - 2));
        double crack = rnd.nextDouble() * Math.PI;
        boolean berg = rnd.nextFloat() < 0.4f;
        int y = origin.getY();
        boolean any = false;
        for (int dx = -a - 1; dx <= a + 1; dx++) {
            for (int dz = -b - 1; dz <= b + 1; dz++) {
                double d = (double) dx * dx / (a * a) + (double) dz * dz / (b * b) + (rnd.nextDouble() - 0.5) * 0.35;
                if (d > 1.0) {
                    continue;
                }
                double line = Math.abs(Math.sin(crack) * dx - Math.cos(crack) * dz);        // fissure : bande vide traversant la plaque
                if (line < 0.7 && !berg) {
                    continue;
                }
                BlockPos top = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                if (!inReach(level, origin, top) || !level.getFluidState(top.below()).is(FluidTags.WATER)) {
                    continue;
                }
                put(level, top, rnd.nextInt(6) == 0 ? Blocks.SNOW_BLOCK.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState());
                put(level, top.below(), Blocks.PACKED_ICE.defaultBlockState());
                if (rnd.nextInt(3) > 0) {
                    put(level, top.below(2), Blocks.ICE.defaultBlockState());
                }
                any = true;
            }
        }
        if (berg && any) {
            int h = 3 + rnd.nextInt(6);                                      // partie émergée ; la partie immergée est environ deux fois plus profonde
            int r = Math.max(2, Math.min(a, b) - 1);
            for (int i = 1; i <= h; i++) {
                int ri = Math.max(1, (int) Math.round(r * (1.0 - (double) i / (h + 2))));
                for (int dx = -ri; dx <= ri; dx++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        if (dx * dx + dz * dz <= ri * ri + rnd.nextInt(2)) {
                            BlockPos p = new BlockPos(origin.getX() + dx, y + i, origin.getZ() + dz);
                            put(level, p, rnd.nextInt(4) == 0 ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState());
                        }
                    }
                }
            }
            for (int i = 2; i <= h * 2; i++) {
                int ri = Math.max(1, (int) Math.round(r * (1.0 - (double) i / (h * 2 + 3))));
                for (int dx = -ri; dx <= ri; dx++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        if (dx * dx + dz * dz <= ri * ri) {
                            BlockPos p = new BlockPos(origin.getX() + dx, y - i, origin.getZ() + dz);
                            if (level.getFluidState(p).is(FluidTags.WATER)) {
                                put(level, p, rnd.nextInt(3) == 0 ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState());
                            }
                        }
                    }
                }
            }
        }
        return any;
    }

    private static boolean inReach(WorldGenLevel level, BlockPos origin, BlockPos p) {
        return level.hasChunkAt(p) && Math.abs((p.getX() >> 4) - (origin.getX() >> 4)) <= 1 && Math.abs((p.getZ() >> 4) - (origin.getZ() >> 4)) <= 1;
    }

    private static void put(WorldGenLevel level, BlockPos p, BlockState state) {
        if (inReach(level, p, p)) {
            BlockState cur = level.getBlockState(p);
            if (cur.isAir() || level.getFluidState(p).is(FluidTags.WATER)) {
                level.setBlock(p, state, 2);
            }
        }
    }
}

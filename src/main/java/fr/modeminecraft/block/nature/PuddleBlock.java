package fr.modeminecraft.block.nature;

import fr.modeminecraft.block.police.FacingDecoBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Flaque d'eau temporaire : posée par la pluie (voir NatureWeather) sur les chemins et les roches plates, elle s'évapore dès que la pluie cesse
 * (plus vite en plein soleil) ; un tick programmé la surveille toutes les 5 secondes. Fine pellicule bleue, sans collision.
 */
public class PuddleBlock extends FacingDecoBlock {
    public PuddleBlock(Properties props, VoxelShape shape) {
        super(props, shape);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 100 + level.random.nextInt(100));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd) {
        boolean raining = level.isRainingAt(pos.above());
        boolean sunny = !level.isRaining() && level.isDay() && level.canSeeSky(pos);
        if (!raining && (sunny ? rnd.nextInt(3) > 0 : rnd.nextInt(8) == 0) || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }
        level.scheduleTick(pos, this, 100 + rnd.nextInt(100));
    }
}

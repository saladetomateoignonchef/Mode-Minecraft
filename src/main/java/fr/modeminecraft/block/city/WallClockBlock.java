package fr.modeminecraft.block.city;

import fr.modeminecraft.block.police.FacingDecoBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Horloge murale fonctionnelle (heure du monde). */
public class WallClockBlock extends FacingDecoBlock implements EntityBlock {
    public WallClockBlock(Properties properties) {
        super(properties, Block.box(2, 2, 14, 14, 14, 16));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WallClockBlockEntity(pos, state);
    }
}

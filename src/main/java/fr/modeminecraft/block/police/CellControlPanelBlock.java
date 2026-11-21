package fr.modeminecraft.block.police;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Pupitre d'ouverture centralisée du poste de garde : ouvre (et déverrouille) toutes les cellules à 24 blocs. */
public class CellControlPanelBlock extends FacingDecoBlock {
    public CellControlPanelBlock(Properties properties) {
        super(properties, net.minecraft.world.level.block.Block.box(1, 0, 2, 15, 10, 14));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-24, -6, -24), pos.offset(24, 6, 24))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof SlidingJailDoorBlock && s.getValue(SlidingJailDoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                JailDoorBlockEntity.release(level, p.immutable());
                n++;
            }
        }
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1f, 0.7f);
        player.displayClientMessage(Component.translatable("message.modeminecraft.cell_panel", n).withStyle(ChatFormatting.YELLOW), true);
        return InteractionResult.CONSUME;
    }
}

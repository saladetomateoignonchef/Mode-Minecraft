package fr.modeminecraft.block.police;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Casier métallique du vestiaire de police : verrouillé par un cadenas, il ne s'ouvre pas à main nue ; on le force
 * au pied-de-biche. Il contient la plus grosse partie du matériel du commissariat (table de butin police_locker).
 */
public class PoliceLockerBlock extends BaseEntityBlock {
    public static final MapCodec<PoliceLockerBlock> CODEC = simpleCodec(PoliceLockerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public PoliceLockerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LockedDoorBlock.LOCKED, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LockedDoorBlock.LOCKED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(LockedDoorBlock.LOCKED, false);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PoliceLockerBlockEntity(pos, state);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return fr.modeminecraft.item.melee.Crowbar.onBlock(stack, level, pos, player);      // pied-de-biche : effraction
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(LockedDoorBlock.LOCKED)) {
            level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.8f, 1.3f);
            player.displayClientMessage(Component.translatable("message.modeminecraft.police_locker.locked").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof PoliceLockerBlockEntity be) {
            player.openMenu(be);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.7f, 1.2f);
        }
        return InteractionResult.CONSUME;
    }

    /** Forcé au pied-de-biche : cadenas arraché, porte tordue, le casier s'ouvre. */
    public static void force(Level level, BlockPos pos, Player player) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof PoliceLockerBlock) {
            level.setBlock(pos, s.setValue(LockedDoorBlock.LOCKED, false), 3);
            if (level.getBlockEntity(pos) instanceof PoliceLockerBlockEntity be && player != null) {
                player.openMenu(be);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PoliceLockerBlockEntity be) {
            Containers.dropContents(level, pos, be);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}

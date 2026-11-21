package fr.modeminecraft.block.police;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Porte verrouillée (arrière-boutiques, bâtiments sécurisés, bureaux du commissariat) : impossible à ouvrir à la
 * main ni à la redstone tant qu'elle est verrouillée ; il faut la forcer au pied-de-biche (2,5 s). Une fois forcée,
 * elle s'ouvre et se ferme normalement.
 */
public class LockedDoorBlock extends DoorBlock {
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");

    public LockedDoorBlock(BlockSetType type, Properties properties) {
        super(type, properties);
        registerDefaultState(defaultBlockState().setValue(LOCKED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(LOCKED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = super.getStateForPlacement(ctx);
        return s == null ? null : s.setValue(LOCKED, false);                   // posée par un joueur : déverrouillée
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return fr.modeminecraft.item.melee.Crowbar.onBlock(stack, level, pos, player);      // pied-de-biche : effraction
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(LOCKED)) {
            if (!level.isClientSide) {
                level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.8f, 0.6f);
                player.displayClientMessage(Component.translatable("message.modeminecraft.locked_door").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        setOpen(player, level, state, pos, !isOpen(state));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!state.getValue(LOCKED)) {
            super.neighborChanged(state, level, pos, block, from, moving);
        }
    }

    /** Forcée au pied-de-biche : déverrouillée et ouverte (les deux moitiés). */
    public static void force(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof LockedDoorBlock door)) {
            return;
        }
        BlockPos lower = s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockPos upper = lower.above();
        for (BlockPos p : new BlockPos[]{lower, upper}) {
            BlockState st = level.getBlockState(p);
            if (st.is(door)) {
                level.setBlock(p, st.setValue(LOCKED, false).setValue(OPEN, true), 10);
            }
        }
    }
}

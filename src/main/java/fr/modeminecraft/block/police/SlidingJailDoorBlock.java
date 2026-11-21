package fr.modeminecraft.block.police;

import com.mojang.serialization.MapCodec;
import fr.modeminecraft.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Porte de cellule coulissante à barreaux (2 blocs de haut) : cadre d'acier, barreaux épais, serrure électronique à
 * voyant rouge (verrouillée) / vert. Indestructible à main nue. Verrouillée pendant une détention : elle s'ouvre seule
 * à la fin de la peine (minuterie affichée au-dessus), par le pupitre d'ouverture centralisée du poste de garde, ou
 * forcée au pied-de-biche depuis le couloir. FACING = côté couloir.
 */
public class SlidingJailDoorBlock extends BaseEntityBlock {
    public static final MapCodec<SlidingJailDoorBlock> CODEC = simpleCodec(SlidingJailDoorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty LOCKED = LockedDoorBlock.LOCKED;
    private static final VoxelShape NS = Block.box(0, 0, 6, 16, 16, 10), EW = Block.box(6, 0, 0, 10, 16, 16);
    private static final VoxelShape NS_OPEN = Shapes.or(Block.box(0, 0, 6, 1.5, 16, 10), Block.box(14.5, 0, 6, 16, 16, 10));
    private static final VoxelShape EW_OPEN = Shapes.or(Block.box(6, 0, 0, 10, 16, 1.5), Block.box(6, 0, 14.5, 10, 16, 16));

    public SlidingJailDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(OPEN, false).setValue(LOCKED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, HALF, OPEN, LOCKED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        boolean ns = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        if (state.getValue(OPEN)) {
            return ns ? NS_OPEN : EW_OPEN;
        }
        return ns ? NS : EW;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        if (pos.getY() >= ctx.getLevel().getMaxBuildHeight() - 1 || !ctx.getLevel().getBlockState(pos.above()).canBeReplaced(ctx)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (dir.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (dir == Direction.UP)) {
            return neighbor.is(this) && neighbor.getValue(HALF) != half
                    ? state.setValue(OPEN, neighbor.getValue(OPEN)).setValue(LOCKED, neighbor.getValue(LOCKED)) : Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER || level.getBlockState(pos.below()).is(this);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new JailDoorBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.JAIL_DOOR.get(), JailDoorBlockEntity::serverTick);
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
        if (state.getValue(LOCKED)) {
            level.playSound(null, pos, fr.modeminecraft.registry.ModSounds.JAIL_BUZZER.get(), SoundSource.BLOCKS, 0.6f, 0.6f);
            player.displayClientMessage(Component.translatable("message.modeminecraft.jail_door.locked").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        setOpen(level, lower(state, pos), !state.getValue(OPEN), false);
        return InteractionResult.CONSUME;
    }

    public static BlockPos lower(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    /** Ouvre / ferme (et verrouille / déverrouille) les deux moitiés, avec le bruit des barreaux qui coulissent. */
    public static void setOpen(Level level, BlockPos lower, boolean open, boolean locked) {
        BlockState s = level.getBlockState(lower);
        if (!(s.getBlock() instanceof SlidingJailDoorBlock)) {
            return;
        }
        boolean changed = s.getValue(OPEN) != open;
        for (BlockPos p : new BlockPos[]{lower, lower.above()}) {
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof SlidingJailDoorBlock) {
                level.setBlock(p, st.setValue(OPEN, open).setValue(LOCKED, locked), 10);
            }
        }
        if (changed) {
            level.playSound(null, lower, fr.modeminecraft.registry.ModSounds.JAIL_DOOR_SLIDE.get(), SoundSource.BLOCKS, 1f, 1f);
        }
    }

    /** Le joueur est-il du côté du couloir (à l'extérieur de la cellule) ? */
    public static boolean outside(BlockState state, BlockPos pos, Player player) {
        Direction f = state.getValue(FACING);
        double d = (player.getX() - (pos.getX() + 0.5)) * f.getStepX() + (player.getZ() - (pos.getZ() + 0.5)) * f.getStepZ();
        return d > 0;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos other = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        if (level.getBlockState(other).is(this)) {
            level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}

package fr.modeminecraft.block.city;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Feu tricolore fonctionnel : vert, orange, rouge en cycle (animé par son rendu, décalé selon la position). */
public class TrafficLightBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TrafficLightBlock> CODEC = simpleCodec(TrafficLightBlock::new);
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

    public TrafficLightBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrafficLightBlockEntity(pos, state);
    }

    /** Phase du feu (0 vert, 1 orange, 2 rouge) à cet instant : 10 s vert, 3 s orange, 10 s rouge. */
    public static int phase(long time, BlockPos pos, net.minecraft.core.Direction facing) {
        long offset = facing.getAxis() == net.minecraft.core.Direction.Axis.X ? 230 : 0;    // les deux axes d'un carrefour sont décalés
        long t = Math.floorMod(time + offset + (pos.getX() / 16 + pos.getZ() / 16) * 37L, 460);
        return t < 200 ? 0 : t < 260 ? 1 : 2;
    }
}

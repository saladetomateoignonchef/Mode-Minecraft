package fr.modeminecraft.block.police;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Mobilier orienté à forme personnalisée (bloc sanitaire, banc, moniteur...). La forme est donnée face au nord. */
public class FacingDecoBlock extends HorizontalDirectionalBlock {
    private final VoxelShape[] shapes = new VoxelShape[4];

    public FacingDecoBlock(Properties properties, VoxelShape north) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        for (Direction d : Direction.Plane.HORIZONTAL) {
            shapes[d.get2DDataValue()] = rotate(north, d);
        }
    }

    public FacingDecoBlock(Properties properties) {
        this(properties, Block.box(0, 0, 0, 16, 16, 16));
    }

    static VoxelShape rotate(VoxelShape s, Direction to) {
        VoxelShape[] out = {net.minecraft.world.phys.shapes.Shapes.empty()};
        s.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            double ax0, az0, ax1, az1;
            switch (to) {
                case SOUTH -> { ax0 = 1 - x1; ax1 = 1 - x0; az0 = 1 - z1; az1 = 1 - z0; }
                case EAST -> { ax0 = 1 - z1; ax1 = 1 - z0; az0 = x0; az1 = x1; }
                case WEST -> { ax0 = z0; ax1 = z1; az0 = 1 - x1; az1 = 1 - x0; }
                default -> { ax0 = x0; ax1 = x1; az0 = z0; az1 = z1; }
            }
            out[0] = net.minecraft.world.phys.shapes.Shapes.or(out[0], net.minecraft.world.phys.shapes.Shapes.box(ax0, y0, az0, ax1, y1, az1));
        });
        return out[0];
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(p -> new FacingDecoBlock(p));
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
        return shapes[state.getValue(FACING).get2DDataValue()];
    }
}

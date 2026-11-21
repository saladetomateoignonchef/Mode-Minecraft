package fr.modeminecraft.block.city;

import fr.modeminecraft.block.police.FacingDecoBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Écran géant publicitaire (4 x 2 blocs) : chaque bloc affiche sa part d'une image animée qui défile. */
public class GiantScreenBlock extends FacingDecoBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 7);

    public GiantScreenBlock(Properties properties) {
        super(properties, Block.box(0, 0, 13, 16, 16, 16));
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(PART);
    }
}

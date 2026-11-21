package fr.modeminecraft.item;

import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Tente de camping pliée : clic droit sur le sol pour la monter (toile kaki tendue sur des arceaux, 5 x 3 x 4,
 * ouverture à l'avant, sac de couchage à l'intérieur). Elle se démonte bloc par bloc.
 */
public class CampingTentItem extends Item {
    public CampingTentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (ctx.getClickedFace() != Direction.UP || ctx.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        Direction front = ctx.getPlayer().getDirection().getOpposite();           // ouverture face au joueur
        BlockPos origin = ctx.getClickedPos().above();
        if (!fits(level, origin, front)) {
            if (!level.isClientSide) {
                ctx.getPlayer().displayClientMessage(Component.translatable("message.modeminecraft.tent.no_room").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            build(level, origin, front, true, false);
            level.playSound(null, origin, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
            if (!ctx.getPlayer().getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Position d'un bloc de la tente : `side` -2..2 (gauche/droite), `depth` 0..3 (de l'ouverture au fond). */
    static BlockPos at(BlockPos origin, Direction front, int side, int y, int depth) {
        Direction right = front.getCounterClockWise();
        return origin.relative(right, side).relative(front.getOpposite(), depth).above(y);
    }

    static boolean fits(Level level, BlockPos origin, Direction front) {
        for (int side = -2; side <= 2; side++) {
            for (int depth = 0; depth < 4; depth++) {
                for (int y = 0; y < 3; y++) {
                    if (!level.getBlockState(at(origin, front, side, y, depth)).canBeReplaced()) {
                        return false;
                    }
                }
                if (!level.getBlockState(at(origin, front, side, -1, depth)).isFaceSturdy(level, at(origin, front, side, -1, depth), Direction.UP)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Construit la tente (aussi utilisé par les campements générés) : versants en escaliers de toile, faîtage,
     * fond fermé, arceaux en bois à l'avant, sac de couchage au centre. Renvoie la position libre pour un coffre.
     */
    public static BlockPos build(net.minecraft.world.level.LevelAccessor level, BlockPos origin, Direction front, boolean sleepingBag, boolean chest) {
        Direction right = front.getCounterClockWise();
        BlockState canvas = ModBlocks.TENT_CANVAS.get().defaultBlockState();
        BlockState stairs = ModBlocks.TENT_CANVAS_STAIRS.get().defaultBlockState();
        int flags = 3;
        for (int depth = 0; depth < 4; depth++) {
            // versants : la partie haute des escaliers vers le faîtage
            level.setBlock(at(origin, front, -2, 0, depth), stairs.setValue(StairBlock.FACING, right), flags);
            level.setBlock(at(origin, front, -1, 1, depth), stairs.setValue(StairBlock.FACING, right), flags);
            level.setBlock(at(origin, front, 0, 2, depth), canvas, flags);
            level.setBlock(at(origin, front, 1, 1, depth), stairs.setValue(StairBlock.FACING, right.getOpposite()), flags);
            level.setBlock(at(origin, front, 2, 0, depth), stairs.setValue(StairBlock.FACING, right.getOpposite()), flags);
        }
        // fond fermé
        for (int side = -1; side <= 1; side++) {
            level.setBlock(at(origin, front, side, 0, 3), canvas, flags);
        }
        level.setBlock(at(origin, front, 0, 1, 3), canvas, flags);
        // arceaux de l'ouverture
        level.setBlock(at(origin, front, -2, 1, 0), Blocks.SPRUCE_FENCE.defaultBlockState(), flags);
        level.setBlock(at(origin, front, 2, 1, 0), Blocks.SPRUCE_FENCE.defaultBlockState(), flags);
        level.setBlock(at(origin, front, 0, 3, 0), Blocks.SPRUCE_FENCE.defaultBlockState(), flags);
        if (sleepingBag) {
            level.setBlock(at(origin, front, 0, 0, 2), ModBlocks.SLEEPING_BAG.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, front.getOpposite()), flags);
        }
        return at(origin, front, -1, 0, 2);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.camping_tent").withStyle(ChatFormatting.GRAY));
    }
}

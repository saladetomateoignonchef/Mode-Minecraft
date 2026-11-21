package fr.modeminecraft.nuclear;

import com.mojang.serialization.MapCodec;
import fr.modeminecraft.registry.ModBlockEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Centrifugeuse à uranium : clic droit avec des lingots d'uranium pour la charger (64 au plus), un signal de redstone la
 * fait tourner ; 16 lingots donnent 1 cœur d'uranium enrichi en 60 s. Clic droit à main nue : récupérer les cœurs (ou,
 * s'il n'y en a pas, afficher l'état).
 */
public class CentrifugeBlock extends BaseEntityBlock {
    public static final MapCodec<CentrifugeBlock> CODEC = simpleCodec(CentrifugeBlock::new);

    public CentrifugeBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CentrifugeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CENTRIFUGE.get(), CentrifugeBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(ModItems.URANIUM_INGOT.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CentrifugeBlockEntity be) {
            int n = Math.min(stack.getCount(), CentrifugeBlockEntity.CAPACITY - be.ingots);
            if (n > 0) {
                be.ingots += n;
                stack.consume(n, player);
                be.setChanged();
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1f, 1.2f);
            }
            be.status(player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CentrifugeBlockEntity be) {
            if (be.cores > 0) {
                ItemStack out = new ItemStack(ModItems.ENRICHED_URANIUM_CORE.get(), be.cores);
                be.cores = 0;
                be.setChanged();
                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                }
                player.displayClientMessage(Component.translatable("message.modeminecraft.centrifuge.collected").withStyle(ChatFormatting.GREEN), true);
            } else {
                be.status(player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

package fr.modeminecraft.block;

import fr.modeminecraft.item.AccessCardItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Porte blindée de salle forte : ne s'ouvre qu'avec une carte d'accès de niveau suffisant
 * (1 = bleue, 2 = rouge, 3 = noire), puis se referme seule au bout de 5 secondes.
 */
public class VaultDoorBlock extends DoorBlock {
    private final int level;

    public VaultDoorBlock(int level, Properties properties) {
        super(BlockSetType.IRON, properties);
        this.level = level;
    }

    public int level() {
        return level;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof AccessCardItem card) {
            if (world.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            if (card.level() < level) {
                player.displayClientMessage(Component.translatable("message.modeminecraft.vault.denied", level).withStyle(ChatFormatting.RED), true);
                world.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1f, 0.5f);
                return ItemInteractionResult.CONSUME;
            }
            if (!isOpen(state)) {
                setOpen(player, world, state, pos, true);
                world.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1f, 1.6f);
                BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
                world.scheduleTick(lower, this, 100);
                player.displayClientMessage(Component.translatable("message.modeminecraft.vault.granted").withStyle(ChatFormatting.GREEN), true);
            }
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide && !isOpen(state) && fr.modeminecraft.world.night.NightOps.blackedOut(world, pos)) {
            // panne de secteur : la serrure électronique est hors service, la porte se pousse à la main
            setOpen(player, world, state, pos, true);
            BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            world.scheduleTick(lower, this, 100);
            player.displayClientMessage(Component.translatable("message.modeminecraft.vault.blackout").withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }
        if (!world.isClientSide) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.vault.locked", level).withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    /** Fermeture automatique. */
    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.is(this) && isOpen(state)) {
            setOpen(null, world, state, pos, false);
        }
    }
}

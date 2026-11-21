package fr.modeminecraft.block;

import fr.modeminecraft.item.BossCardItem;
import fr.modeminecraft.registry.ModSounds;
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
 * Porte blindée de la chambre forte d'un boss de ville : indestructible (explosifs, C4, pioches et pied-de-biche
 * sans effet) ; seule la carte d'accès nominative de CE boss déverrouille le lecteur magnétique (ouverture
 * hydraulique, la porte se referme seule au bout de 20 s ; la carte n'est pas consommée).
 */
public class BossVaultDoorBlock extends DoorBlock {
    public final BossCardItem.VaultType vault;

    public BossVaultDoorBlock(BossCardItem.VaultType vault, Properties properties) {
        super(BlockSetType.IRON, properties);
        this.vault = vault;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(stack.getItem() instanceof BossCardItem card)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (world.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (card.vault != vault) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.boss_vault.wrong_card").withStyle(ChatFormatting.RED), true);
            world.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1f, 0.5f);
            return ItemInteractionResult.CONSUME;
        }
        if (!isOpen(state)) {
            setOpen(player, world, state, pos, true);
            world.playSound(null, pos, ModSounds.VAULT_DOOR_OPEN.get(), SoundSource.BLOCKS, 1.6f, 1f);
            BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            world.scheduleTick(lower, this, 400);
            player.displayClientMessage(Component.translatable("message.modeminecraft.boss_vault.open").withStyle(ChatFormatting.GOLD), true);
        }
        return ItemInteractionResult.CONSUME;
    }

    /** Sans la carte, la main nue ne fait rien (pas d'ouverture comme une porte normale). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.boss_vault.locked").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isOpen(state)) {
            setOpen(null, level, state, pos, false);
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.2f, 0.7f);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block, BlockPos from, boolean moving) {
        // pas de redstone
    }
}

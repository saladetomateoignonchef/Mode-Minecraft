package fr.modeminecraft.scenario;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Casier personnel des dortoirs : le premier soldat qui l'ouvre en devient le titulaire (ou il est attribué à l'arrivée
 * du joueur) ; les autres ne peuvent pas l'ouvrir. Ce qu'on y range est conservé à la mort (le reste de l'inventaire
 * tombe comme d'habitude). Le forcer au pied-de-biche est un vol dans sa propre base.
 */
public class BarracksLockerBlock extends fr.modeminecraft.block.police.PoliceLockerBlock {
    public static final MapCodec<BarracksLockerBlock> CODEC = simpleCodec(BarracksLockerBlock::new);

    public BarracksLockerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(fr.modeminecraft.block.police.LockedDoorBlock.LOCKED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarracksLockerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof BarracksLockerBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (be.owner == null) {
            be.owner = player.getUUID();
            be.ownerName = player.getGameProfile().getName();
            be.setChanged();
            player.displayClientMessage(Component.translatable("message.modeminecraft.locker.claimed").withStyle(ChatFormatting.GREEN), true);
        } else if (!be.owner.equals(player.getUUID()) && !player.isCreative() && !state.getValue(fr.modeminecraft.block.police.LockedDoorBlock.LOCKED)) {
            level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.8f, 1.3f);
            player.displayClientMessage(Component.translatable("message.modeminecraft.locker.not_yours", be.ownerName).withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        player.openMenu(be);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.7f, 1.2f);
        return InteractionResult.CONSUME;
    }

    /** Attribue le casier à un joueur (arrivée au dortoir). */
    public static void assign(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof BarracksLockerBlockEntity be) {
            be.owner = player.getUUID();
            be.ownerName = player.getGameProfile().getName();
            be.setChanged();
        }
    }
}

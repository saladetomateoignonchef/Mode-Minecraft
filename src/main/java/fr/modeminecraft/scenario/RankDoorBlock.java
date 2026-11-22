package fr.modeminecraft.scenario;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Porte à contrôle d'accréditation (RankSecurityDoor) des quartiers réservés : lecteur de badge, elle ne s'ouvre que
 * pour un soldat du camp de la base ayant au moins le grade requis (1 Caporal … 5 Général) ; sinon la sentinelle
 * repousse le visiteur (« Accès refusé — Réservé aux gradés »).
 */
public class RankDoorBlock extends DoorBlock {
    public static final IntegerProperty RANK = IntegerProperty.create("rank", 1, 5);

    public RankDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
        registerDefaultState(defaultBlockState().setValue(RANK, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(RANK);
    }

    public static Grade required(BlockState s) {
        return Grade.values()[s.getValue(RANK)];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Grade need = required(state);
        boolean ok = player.isCreative();
        if (!ok && player instanceof ServerPlayer sp && Scenario.side(sp) != null) {
            ok = Reputation.grade(sp).atLeast(need);
        }
        if (!ok) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1f, 0.5f);
            Side s = Scenario.side(player);
            player.displayClientMessage(Component.translatable("message.modeminecraft.rank_door.denied",
                    Component.translatable(need.key(s == null ? Side.US : s))).withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        boolean open = !state.getValue(OPEN);
        setOpen(player, level, state, pos, open);
        level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1f, 1.1f);
        if (open) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6f, 1.8f);   // badge accepté
        }
        return InteractionResult.CONSUME;
    }
}

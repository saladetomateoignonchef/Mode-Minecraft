package fr.modeminecraft.block.camp;

import com.mojang.serialization.MapCodec;
import fr.modeminecraft.ModeMinecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sac de couchage (et lit de cellule) : clic droit = dormir pour passer la nuit SANS changer de point de
 * réapparition ; Maj + clic droit = dormir ET en faire son point de réapparition.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public class SleepingBagBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SleepingBagBlock> CODEC = simpleCodec(SleepingBagBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 0, 15, 4, 16);
    /** Joueurs qui ont demandé (Maj) à faire du sac leur point de réapparition. */
    private static final Set<UUID> KEEP_SPAWN = ConcurrentHashMap.newKeySet();

    public SleepingBagBlock(Properties properties) {
        super(properties);
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
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, @Nullable LivingEntity sleeper) {
        return true;
    }

    /** Pas d'état « occupé » comme les lits : rien à mémoriser. */
    @Override
    public void setBedOccupied(BlockState state, Level level, BlockPos pos, LivingEntity sleeper, boolean occupied) {
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        boolean setSpawn = player.isSecondaryUseActive();
        if (setSpawn) {
            KEEP_SPAWN.add(sp.getUUID());
            sp.setRespawnPosition(level.dimension(), pos, sp.getYRot(), false, false);
            sp.displayClientMessage(Component.translatable("message.modeminecraft.sleeping_bag.spawn").withStyle(ChatFormatting.GREEN), true);
        }
        try {
            sp.startSleepInBed(pos).ifLeft(problem -> {
                if (problem.getMessage() != null) {
                    sp.displayClientMessage(problem.getMessage(), true);
                }
            });
        } finally {
            KEEP_SPAWN.remove(sp.getUUID());
        }
        return InteractionResult.SUCCESS;
    }

    /** Dormir dans un sac ne remplace pas le point de réapparition (sauf avec Maj). */
    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent event) {
        BlockPos pos = event.getNewSpawn();
        if (pos != null && event.getEntity().level().getBlockState(pos).getBlock() instanceof SleepingBagBlock
                && !KEEP_SPAWN.contains(event.getEntity().getUUID()) && !event.isForced()) {
            event.setCanceled(true);
        }
    }
}

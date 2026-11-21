package fr.modeminecraft.block.city;

import fr.modeminecraft.block.police.FacingDecoBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Caisse enregistreuse (comptoirs des supermarchés, cafés, stations-service, banques) : écran, clavier, tiroir-caisse.
 * Braquage : un commerçant tenu en joue l'ouvre, ou on la force au pied-de-biche ; le tiroir saute (sonnette) et
 * crache liasses de billets, or et émeraudes. Toute ouverture forcée déclenche l'alarme : la police est prévenue.
 */
public class CashRegisterBlock extends FacingDecoBlock {
    public static final BooleanProperty OPEN = net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN;

    public CashRegisterBlock(Properties properties) {
        super(properties, Block.box(2, 0, 3, 14, 9, 14));
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(OPEN);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return fr.modeminecraft.item.melee.Crowbar.onBlock(stack, level, pos, player);      // pied-de-biche : effraction
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && state.getValue(OPEN) && player.isSecondaryUseActive()) {
            level.setBlock(pos, state.setValue(OPEN, false), 3);            // Maj + clic : on referme le tiroir (vide)
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 1.5f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Le tiroir saute : butin, sonnette, alarme. */
    public static void burst(Level level, BlockPos pos, Player robber) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof CashRegisterBlock) || s.getValue(OPEN) || !(level instanceof ServerLevel sl)) {
            return;
        }
        level.setBlock(pos, s.setValue(OPEN, true), 3);
        level.playSound(null, pos, fr.modeminecraft.registry.ModSounds.CASH_REGISTER_BELL.get(), SoundSource.BLOCKS, 1f, 1f);
        var r = level.random;
        ItemStack[] loot = {
                new ItemStack(fr.modeminecraft.registry.ModItems.CASH_BUNDLE.get(), 2 + r.nextInt(5)),
                new ItemStack(Items.GOLD_NUGGET, 3 + r.nextInt(8)),
                new ItemStack(Items.EMERALD, 1 + r.nextInt(3)),
                r.nextFloat() < 0.3f ? new ItemStack(Items.GOLD_INGOT, 1 + r.nextInt(2)) : ItemStack.EMPTY};
        var front = s.getValue(FACING);
        for (ItemStack st : loot) {
            if (st.isEmpty()) continue;
            ItemEntity e = new ItemEntity(level, pos.getX() + 0.5 + front.getStepX() * 0.6, pos.getY() + 0.7, pos.getZ() + 0.5 + front.getStepZ() * 0.6, st);
            e.setDeltaMovement(front.getStepX() * 0.18 + (r.nextDouble() - 0.5) * 0.15, 0.25, front.getStepZ() * 0.18 + (r.nextDouble() - 0.5) * 0.15);
            level.addFreshEntity(e);
        }
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(fr.modeminecraft.registry.ModItems.CASH_BUNDLE.get())),
                pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 25, 0.3, 0.2, 0.3, 0.12);
        if (robber != null) {
            fr.modeminecraft.world.police.Wanted.alarm(sl, pos, robber);
        }
    }
}

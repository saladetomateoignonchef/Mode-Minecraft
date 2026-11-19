package fr.modeminecraft.block;

import com.mojang.serialization.MapCodec;
import fr.modeminecraft.item.gun.GunItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Râtelier d'armes (à poser contre un mur) : il expose jusqu'à 3 armes à feu, l'une au-dessus de l'autre.
 * Clic droit avec une arme : on la pose à la hauteur visée ; clic droit main vide : on reprend l'arme visée.
 */
public class WeaponRackBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<WeaponRackBlock> CODEC = simpleCodec(WeaponRackBlock::new);
    private static final VoxelShape[] SHAPES = {
            Block.box(0, 0, 10, 16, 16, 16), // face au nord : planche contre le mur sud
            Block.box(0, 0, 0, 16, 16, 6),   // sud
            Block.box(10, 0, 0, 16, 16, 16), // ouest
            Block.box(0, 0, 0, 6, 16, 16)};  // est

    public WeaponRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPES[1];
            case WEST -> SHAPES[2];
            case EAST -> SHAPES[3];
            default -> SHAPES[0];
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeaponRackBlockEntity(pos, state);
    }

    /** Emplacement visé : 0 en bas, 2 en haut. */
    private static int slotAt(BlockHitResult hit, BlockPos pos) {
        double y = hit.getLocation().y - pos.getY();
        return Math.max(0, Math.min(WeaponRackBlockEntity.SIZE - 1, (int) (y * WeaponRackBlockEntity.SIZE)));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof GunItem) || !(level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int slot = slotAt(hit, pos);
        if (!rack.getItem(slot).isEmpty()) {
            slot = rack.firstEmpty();
            if (slot < 0) {
                return ItemInteractionResult.FAIL;
            }
        }
        if (!level.isClientSide) {
            rack.setItem(slot, stack.split(1));
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.8f, 0.9f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        int slot = slotAt(hit, pos);
        if (rack.getItem(slot).isEmpty()) {
            slot = rack.lastFilled();
            if (slot < 0) {
                return InteractionResult.PASS;
            }
        }
        if (!level.isClientSide) {
            ItemStack gun = rack.removeItemNoUpdate(slot);
            rack.setChanged();
            if (!player.getInventory().add(gun)) {
                player.drop(gun, false);
            }
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 0.8f, 1.1f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack) {
            Containers.dropContents(level, pos, rack);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

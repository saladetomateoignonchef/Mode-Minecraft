package fr.modeminecraft.block;

import com.mojang.serialization.MapCodec;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.Legendary;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Râtelier à trophées mural : expose en relief une arme du mod. Pour une arme de Chef de Faction, une plaque nominative
 * dorée affiche le nom du Chef vaincu et un léger éclairage met le trophée en valeur.
 */
public class TrophyRackBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TrophyRackBlock> CODEC = simpleCodec(TrophyRackBlock::new);
    private static final VoxelShape[] SHAPES = {
            Block.box(0, 0, 13, 16, 16, 16), Block.box(0, 0, 0, 16, 16, 3), Block.box(13, 0, 0, 16, 16, 16), Block.box(0, 0, 0, 3, 16, 16)};

    public TrophyRackBlock(Properties properties) {
        super(properties.lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 7 : 0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction d = context.getHorizontalDirection().getOpposite();
        if (context.getClickedFace().getAxis().isHorizontal()) {
            d = context.getClickedFace();
        }
        return defaultBlockState().setValue(FACING, d);
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
        return new TrophyRackBlockEntity(pos, state);
    }

    /** N'importe quelle arme du mod (armes à feu, armes blanches, épées, Pourfendeuse...). */
    public static boolean isWeapon(ItemStack s) {
        return s.getItem() instanceof GunItem || s.getItem() instanceof SwordItem
                || s.getItem() instanceof fr.modeminecraft.item.melee.MeleeWeaponItem
                || s.getItem() instanceof net.minecraft.world.item.BowItem || s.getItem() instanceof net.minecraft.world.item.TridentItem;
    }

    /** Trophée d'un Chef de Faction : identifiant du Chef (pour la plaque), sinon null. */
    public static String bossOf(ItemStack s) {
        Legendary l = Legendary.of(s);
        if (l != null) {
            return l.boss;
        }
        if (s.is(ModItems.DRAGONSLAYER.get())) {
            return "boss_falcon";
        }
        if (s.is(ModItems.GOLDEN_MACHETE.get())) {
            return "boss_cartel";
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!isWeapon(stack) || !(level.getBlockEntity(pos) instanceof TrophyRackBlockEntity rack) || !rack.item().isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            rack.setItem(stack.split(1));
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, bossOf(rack.item()) != null), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.8f, 0.8f);
            String boss = bossOf(rack.item());
            if (boss != null) {
                player.displayClientMessage(Component.translatable("message.modeminecraft.trophy.placed",
                        Component.translatable("entity.modeminecraft.bot." + boss)).withStyle(net.minecraft.ChatFormatting.GOLD), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TrophyRackBlockEntity rack) || rack.item().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack item = rack.item();
            rack.setItem(ItemStack.EMPTY);
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 0.8f, 1.1f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TrophyRackBlockEntity rack && !rack.item().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, rack.item());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

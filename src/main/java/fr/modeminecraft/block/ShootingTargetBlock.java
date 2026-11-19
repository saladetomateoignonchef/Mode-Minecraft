package fr.modeminecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cible de tir (stand de tir) : une balle qui la touche donne un score de 0 à 10 selon la distance au centre,
 * affiché au tireur avec la distance de tir et le total de la série (remise à zéro après 15 s sans tir).
 * La cible émet un signal de redstone égal au score pendant 1 seconde.
 */
public class ShootingTargetBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ShootingTargetBlock> CODEC = simpleCodec(ShootingTargetBlock::new);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    private static final VoxelShape[] SHAPES = {
            Block.box(0, 0, 14, 16, 16, 16), Block.box(0, 0, 0, 16, 16, 2),
            Block.box(14, 0, 0, 16, 16, 16), Block.box(0, 0, 0, 2, 16, 16)};

    private record Series(int total, int shots, long last) {}
    private static final Map<UUID, Series> SERIES = new ConcurrentHashMap<>();

    public ShootingTargetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWER, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER);
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

    /** Score (0 à 10) d'un impact en "hit", d'après sa distance au centre de la face. */
    public static int score(BlockState state, BlockPos pos, Vec3 hit) {
        Vec3 local = hit.subtract(Vec3.atCenterOf(pos));
        double u = state.getValue(FACING).getAxis() == Direction.Axis.Z ? local.x : local.z;
        double r = Math.sqrt(u * u + local.y * local.y) / 0.5; // 0 au centre, 1 au bord
        return r >= 1 ? 0 : 10 - (int) (r * 10);
    }

    /** Appelé par la balle (BulletEntity) quand elle touche la cible. */
    public static void onBulletHit(ServerLevel level, BlockPos pos, BlockState state, Vec3 hit, Entity shooter) {
        int score = score(state, pos, hit);
        level.setBlock(pos, state.setValue(POWER, Math.min(15, score)), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), 20);
        level.sendParticles(score >= 9 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.CRIT, hit.x, hit.y, hit.z, 4, 0.03, 0.03, 0.03, 0);
        level.playSound(null, pos, score == 10 ? SoundEvents.NOTE_BLOCK_BELL.value() : SoundEvents.WOOD_HIT, SoundSource.BLOCKS,
                0.9f, 0.8f + score * 0.06f);
        if (shooter instanceof Player player) {
            long now = level.getGameTime();
            Series s = SERIES.get(player.getUUID());
            if (s == null || now - s.last > 300) {
                s = new Series(0, 0, now);
            }
            s = new Series(s.total + score, s.shots + 1, now);
            SERIES.put(player.getUUID(), s);
            double distance = player.position().distanceTo(hit);
            ChatFormatting color = score >= 9 ? ChatFormatting.GOLD : score >= 6 ? ChatFormatting.GREEN : score > 0 ? ChatFormatting.WHITE : ChatFormatting.RED;
            player.displayClientMessage(Component.translatable("message.modeminecraft.target", score,
                    String.format("%.1f", distance), s.total, s.shots).withStyle(color), true);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWER) != 0) {
            level.setBlock(pos, state.setValue(POWER, 0), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }
}

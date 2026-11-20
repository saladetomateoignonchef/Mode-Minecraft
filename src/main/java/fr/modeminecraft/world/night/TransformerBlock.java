package fr.modeminecraft.world.night;

import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Transformateur haute tension : boîtier métallique, isolateurs, câbles, diode verte.
 * Détruit par une charge (C4, grenade, roquette) ou par des tirs soutenus (6 impacts), il explose en gerbe d'étincelles
 * et coupe le courant dans un rayon de 60 blocs pendant 5 minutes ; l'équipe de maintenance le répare ensuite.
 */
public class TransformerBlock extends HorizontalDirectionalBlock {
    public static final com.mojang.serialization.MapCodec<TransformerBlock> CODEC = simpleCodec(TransformerBlock::new);
    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");
    public static final IntegerProperty HITS = IntegerProperty.create("hits", 0, 5);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);

    public TransformerBlock(Properties properties) {
        super(properties.lightLevel(s -> s.getValue(BROKEN) ? 0 : 3));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(BROKEN, false).setValue(HITS, 0));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BROKEN, HITS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** Une balle touche le transformateur : étincelles, et au 6e impact c'est le court-circuit. */
    public static boolean shot(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof TransformerBlock) || state.getValue(BROKEN)) {
            return state.getBlock() instanceof TransformerBlock;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.3);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 1f, 1.6f);
        int hits = state.getValue(HITS) + 1;
        if (hits > 5) {
            shortCircuit(level, pos, state);
        } else {
            level.setBlock(pos, state.setValue(HITS, hits), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    /** Court-circuit : explosion d'étincelles, électrocution alentour, panne de secteur. */
    public static void shortCircuit(ServerLevel level, BlockPos pos, BlockState state) {
        level.playSound(null, pos, ModSounds.TRANSFORMER_SHORT.get(), SoundSource.BLOCKS, 3f, 1f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 120, 0.6, 0.6, 0.6, 0.6);
        level.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 25, 0.4, 0.5, 0.4, 0.03);
        level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2.5))) {
            e.hurt(level.damageSources().lightningBolt(), 6f);
        }
        if (state.getBlock() instanceof TransformerBlock) {
            level.setBlock(pos, state.setValue(BROKEN, true).setValue(HITS, 0), Block.UPDATE_ALL);
            level.scheduleTick(pos, state.getBlock(), NightOps.BLACKOUT_TICKS);   // réparé après la panne
        }
        NightOps.blackout(level, pos, NightOps.BLACKOUT_RADIUS, NightOps.BLACKOUT_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(BROKEN)) {
            level.setBlock(pos, state.setValue(BROKEN, false).setValue(HITS, 0), Block.UPDATE_ALL);
        }
    }

    /** Charge explosive : le transformateur saute (il reste en place, calciné, jusqu'à la réparation). */
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (level instanceof ServerLevel sl && !state.getValue(BROKEN)) {
            shortCircuit(sl, pos, state);
            return;
        }
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl && !state.getValue(BROKEN) && !player.isCreative()) {
            shortCircuit(sl, pos, state);
            state = level.getBlockState(pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Les étincelles d'un transformateur encore sous tension. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(BROKEN)) {
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0.04, 0);
            }
        } else if (state.getValue(HITS) > 0 && random.nextInt(6 - state.getValue(HITS)) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + random.nextDouble(), pos.getY() + 0.9, pos.getZ() + random.nextDouble(), 0, 0.1, 0);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
    }
}

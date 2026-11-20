package fr.modeminecraft.entity.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tour de brouillage haute sécurité (toits du palais du dictateur, des bases militaires et des stations polaires) :
 * dôme de brouillage de 50 blocs, perte de contrôle des drones à moins de 15 blocs.
 * Il faut la détruire (8 balles de sniper, ou une charge de C4) avant d'envoyer ses drones.
 */
public class JammerTowerBlock extends Block {
    public static final IntegerProperty HITS = IntegerProperty.create("hits", 0, 7);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public JammerTowerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HITS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HITS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide && !old.is(this)) {
            Jamming.registerTower(level, pos);
            level.scheduleTick(pos, this, 20);
        }
    }

    /** Se réenregistre régulièrement (après un redémarrage du serveur, ou une tour générée avec la ville). */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Jamming.registerTower(level, pos);
        level.scheduleTick(pos, this, 100);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!newState.is(this)) {
            Jamming.removeTower(level, pos);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 40, 0.3, 0.6, 0.3, 0.3);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** Impact de balle : au 8e, l'antenne est détruite. */
    public static boolean shot(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof JammerTowerBlock)) {
            return false;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 10, 0.2, 0.3, 0.2, 0.2);
        level.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 1f, 1.5f);
        int hits = state.getValue(HITS) + 1;
        if (hits > 7) {
            level.playSound(null, pos, fr.modeminecraft.registry.ModSounds.TRANSFORMER_SHORT.get(), SoundSource.BLOCKS, 2f, 1.3f);
            level.destroyBlock(pos, false);
        } else {
            level.setBlock(pos, state.setValue(HITS, hits), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        level.playSound(null, pos, fr.modeminecraft.registry.ModSounds.TRANSFORMER_SHORT.get(), SoundSource.BLOCKS, 2f, 1.3f);
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1, 0.1f, 0.1f), 0.6f),
                    pos.getX() + 0.5, pos.getY() + 1.95, pos.getZ() + 0.5, 0, 0, 0);
        }
    }
}

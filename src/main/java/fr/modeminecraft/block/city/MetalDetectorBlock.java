package fr.modeminecraft.block.city;

import fr.modeminecraft.entity.bot.BotGoals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Portique détecteur de métaux (barre haute, sans collision) : bipe et clignote en rouge si on le franchit armé. */
public class MetalDetectorBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 13, 6, 16, 16, 10);
    private static final Map<UUID, Long> LAST = new WeakHashMap<>();

    public MetalDetectorBlock(Properties p) {
        super(p);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel sl) || !(entity instanceof Player p) || p.isSpectator()) {
            return;
        }
        long now = level.getGameTime();
        if (LAST.getOrDefault(p.getUUID(), -100L) > now - 20) {
            return;
        }
        LAST.put(p.getUUID(), now);
        if (BotGoals.armedVisible(p)) {
            for (int i = 0; i < 3; i++) {
                sl.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.4f, 1.8f);
            }
            sl.sendParticles(new DustParticleOptions(new Vector3f(1, 0.1f, 0.1f), 1.5f), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 12, 0.4, 0.05, 0.1, 0);
        } else {
            sl.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.4f, 2.0f);
        }
    }
}

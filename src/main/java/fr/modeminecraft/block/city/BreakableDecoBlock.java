package fr.modeminecraft.block.city;

import fr.modeminecraft.entity.AreaCloudEntity;
import fr.modeminecraft.entity.GeyserEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Décor urbain qui réagit à sa destruction (à la main, par une explosion ou par une balle) :
 * - bouche d'incendie : geyser d'eau pendant 8 s ;
 * - extincteur : il explose en nuage de fumée blanche.
 */
public class BreakableDecoBlock extends Block {
    public enum Kind { HYDRANT, EXTINGUISHER }

    private final Kind kind;
    private final VoxelShape shape;

    public BreakableDecoBlock(Kind kind, Properties p) {
        super(p);
        this.kind = kind;
        this.shape = kind == Kind.HYDRANT ? Block.box(4, 0, 4, 12, 13, 12) : Block.box(6, 0, 6, 10, 11, 10);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide && !newState.is(this) && !moving && level instanceof ServerLevel sl) {
            if (kind == Kind.HYDRANT) {
                sl.addFreshEntity(new GeyserEntity(sl, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
                sl.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.5f, 0.6f);
            } else {
                sl.addFreshEntity(new AreaCloudEntity(sl, AreaCloudEntity.Type.SMOKE, Vec3.atBottomCenterOf(pos), 3.5f, 160, null));
                sl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.4f, 0.5f);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** Une balle détruit le décor. */
    public static boolean shot(ServerLevel level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity bullet) {
        if (state.getBlock() instanceof BreakableDecoBlock) {
            level.destroyBlock(pos, false, bullet);
            return true;
        }
        return false;
    }
}

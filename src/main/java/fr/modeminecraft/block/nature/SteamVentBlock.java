package fr.modeminecraft.block.nature;

import fr.modeminecraft.block.police.FacingDecoBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Évent d'une source chaude ou d'un geyser : colonne de vapeur ascendante (côté client) et, pour les joueurs qui se baignent dans le
 * bassin voisin, une légère régénération (vérifiée toutes les 2 secondes par un tick programmé, posé à la génération).
 */
public class SteamVentBlock extends FacingDecoBlock {
    public SteamVentBlock(Properties props, VoxelShape shape) {
        super(props, shape);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (rnd.nextInt(2) == 0) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x + (rnd.nextDouble() - 0.5) * 0.3, y, z + (rnd.nextDouble() - 0.5) * 0.3,
                    (rnd.nextDouble() - 0.5) * 0.01, 0.06 + rnd.nextDouble() * 0.03, (rnd.nextDouble() - 0.5) * 0.01);
        }
        if (rnd.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.CLOUD, x, y + 0.6, z, (rnd.nextDouble() - 0.5) * 0.04, 0.12, (rnd.nextDouble() - 0.5) * 0.04);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 40);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd) {
        List<Player> near = level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(pos).inflate(5, 3, 5), p -> p.isInWater() && !p.isSpectator());
        for (Player p : near) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
        }
        level.scheduleTick(pos, this, 40);
    }
}

package fr.modeminecraft.block.city;

import fr.modeminecraft.block.police.FacingDecoBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Décors urbains animés (côté client) :
 * - bouche d'égout / d'aération fumante : volutes de vapeur blanche ;
 * - jet de fontaine publique : gerbes d'eau qui retombent en éclaboussures ;
 * les néons clignotants, écrans géants et ventilateurs de climatiseurs sont animés par leurs textures.
 */
public class AnimatedDecoBlock extends FacingDecoBlock {
    public enum Fx { NONE, STEAM, FOUNTAIN }

    private final Fx fx;

    public AnimatedDecoBlock(Properties properties, VoxelShape north, Fx fx) {
        super(properties, north);
        this.fx = fx;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        switch (fx) {
            case STEAM -> {
                for (int i = 0; i < 2; i++) {
                    level.addParticle(ParticleTypes.CLOUD, x + (r.nextDouble() - 0.5) * 0.6, y + 0.15, z + (r.nextDouble() - 0.5) * 0.6,
                            (r.nextDouble() - 0.5) * 0.01, 0.04 + r.nextDouble() * 0.03, (r.nextDouble() - 0.5) * 0.01);
                }
                if (r.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.WHITE_SMOKE, x, y + 0.2, z, 0, 0.05, 0);
                }
            }
            case FOUNTAIN -> {
                for (int i = 0; i < 6; i++) {
                    double a = r.nextDouble() * Math.PI * 2, s = 0.06 + r.nextDouble() * 0.05;
                    level.addParticle(ParticleTypes.FALLING_WATER, x, y + 0.9, z, 0, 0, 0);
                    level.addParticle(ParticleTypes.SPLASH, x + Math.cos(a) * 0.3, y + 1.0, z + Math.sin(a) * 0.3, Math.cos(a) * s, 0.25, Math.sin(a) * s);
                }
                level.addParticle(ParticleTypes.BUBBLE_POP, x + (r.nextDouble() - 0.5), y + 0.1, z + (r.nextDouble() - 0.5), 0, 0.02, 0);
            }
            default -> { }
        }
    }
}

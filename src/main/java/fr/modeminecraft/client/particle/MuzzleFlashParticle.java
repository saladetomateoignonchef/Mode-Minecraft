package fr.modeminecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Flash de bouche : une étoile lumineuse au bout du canon, pendant 2 ticks. */
public class MuzzleFlashParticle extends TextureSheetParticle {
    protected MuzzleFlashParticle(ClientLevel level, double x, double y, double z, double size, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = this.yd = this.zd = 0;
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 2;
        this.quadSize = (float) (size > 0 ? size : 0.22) * (0.85f + random.nextFloat() * 0.3f);
        this.roll = this.oRoll = random.nextFloat() * 6.283f;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        alpha = 1 - age / (float) lifetime;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new MuzzleFlashParticle(level, x, y, z, vx, sprites); // vx = taille
        }
    }
}

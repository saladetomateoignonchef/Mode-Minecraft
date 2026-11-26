package fr.modeminecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Traînée de condensation : nuage blanc qui s'élargit lentement et s'estompe sur environ 25 secondes. */
public class ContrailParticle extends TextureSheetParticle {
    protected ContrailParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = this.yd = this.zd = 0;
        this.gravity = 0;
        this.lifetime = 500;
        this.quadSize = 0.7f;
        this.hasPhysics = false;
        this.alpha = 0.0f;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        quadSize = Math.min(2.4f, quadSize + 0.004f);
        float t = age / (float) lifetime;
        alpha = Math.min(0.55f, t * 20f) * (1 - t);                   // apparaît vite, s'efface doucement
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;                                              // blanc franc, de jour comme de nuit
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
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new ContrailParticle(level, x, y, z, sprites);
        }
    }
}

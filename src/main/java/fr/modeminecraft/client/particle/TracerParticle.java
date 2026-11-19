package fr.modeminecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Point lumineux (toujours à pleine luminosité) qui forme la traînée des balles traçantes. */
public class TracerParticle extends TextureSheetParticle {
    protected TracerParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = this.yd = this.zd = 0;
        this.gravity = 0;
        this.lifetime = 6;
        this.quadSize = 0.08f;
        this.hasPhysics = false;
        setColor(1.0f, 0.55f, 0.15f);
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        alpha = 1 - age / (float) lifetime;
        quadSize *= 0.9f;
    }

    /** Plus le point est loin, plus il est gros : il reste visible comme un point lumineux qui s'éloigne. */
    @Override
    public float getQuadSize(float partialTick) {
        var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distance = Math.sqrt(camera.distanceToSqr(x, y, z));
        return quadSize * (float) Math.max(1.0, Math.min(6.0, distance / 5.0));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0; // brille même dans le noir
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
            return new TracerParticle(level, x, y, z, sprites);
        }
    }
}

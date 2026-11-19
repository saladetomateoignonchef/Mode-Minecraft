package fr.modeminecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ColorParticleOption;

/**
 * Laser : le point lumineux sur la cible (opaque) ou un morceau du faisceau (transparent).
 * Il ne vit que 2 ticks : il est recréé à chaque tick tant que le laser est allumé.
 */
public class LaserParticle extends TextureSheetParticle {
    private final boolean dot;

    protected LaserParticle(ClientLevel level, double x, double y, double z, ColorParticleOption color, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = this.yd = this.zd = 0;
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 2;
        this.dot = color.getAlpha() > 0.99f;
        this.quadSize = dot ? 0.07f : 0.016f;
        setColor(color.getRed(), color.getGreen(), color.getBlue());
        setAlpha(dot ? 1.0f : color.getAlpha());
        pickSprite(sprites);
    }

    /** Le point grossit avec la distance pour rester visible de loin. */
    @Override
    public float getQuadSize(float partialTick) {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distance = Math.sqrt(camera.distanceToSqr(x, y, z));
        return quadSize * (float) Math.max(1.0, Math.min(dot ? 8.0 : 4.0, distance / 4.0));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0; // brille même dans le noir
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption color, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new LaserParticle(level, x, y, z, color, sprites);
        }
    }
}

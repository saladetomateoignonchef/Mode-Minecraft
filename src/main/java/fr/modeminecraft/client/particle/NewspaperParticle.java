package fr.modeminecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Feuille de journal emportée par le vent : elle vole à l'horizontale, virevolte, retombe lentement. */
public class NewspaperParticle extends TextureSheetParticle {
    private final float spin;

    protected NewspaperParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 0.012f;
        this.friction = 0.985f;
        this.lifetime = 160 + random.nextInt(120);
        this.quadSize = 0.22f + random.nextFloat() * 0.1f;
        this.spin = (random.nextFloat() - 0.5f) * 0.35f;
        this.hasPhysics = true;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        oRoll = roll;
        roll += spin;
        // rafales : la feuille remonte un peu puis replonge
        yd += Mth.sin(age * 0.18f) * 0.006;
        if (onGround) {
            xd *= 0.6;
            zd *= 0.6;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new NewspaperParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}

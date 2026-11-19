package fr.modeminecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Douille éjectée : elle part sur le côté, retombe, rebondit un peu sur le sol puis disparaît.
 */
public class CasingParticle extends TextureSheetParticle {
    private final float spin;
    private final boolean shell;
    private boolean landed;

    protected CasingParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites,
                             boolean shell) {
        super(level, x, y, z);
        this.shell = shell;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 0.9f;
        this.friction = 0.98f;
        this.lifetime = 50 + random.nextInt(20);
        this.quadSize = 0.035f;
        this.hasPhysics = true;
        this.spin = (random.nextFloat() - 0.5f) * 0.9f;
        this.roll = random.nextFloat() * 6.28f;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        double previousYd = yd;
        super.tick();
        oRoll = roll;
        if (onGround && !landed) {
            // Tintement de la douille qui touche le sol (cartouche en plastique : bruit sourd)
            landed = true;
            level.playLocalSound(x, y, z, (shell ? fr.modeminecraft.registry.ModSounds.SHELL_CASING
                            : fr.modeminecraft.registry.ModSounds.CASING).get(), net.minecraft.sounds.SoundSource.PLAYERS,
                    0.35f, 0.9f + random.nextFloat() * 0.2f, false);
        }
        if (onGround) {
            // Petit rebond, puis la douille glisse et s'arrête
            if (previousYd < -0.08) {
                yd = -previousYd * 0.3;
            }
            xd *= 0.6;
            zd *= 0.6;
        } else {
            roll += spin; // elle tourne sur elle-même en l'air
        }
        if (age > lifetime - 10) {
            alpha = (lifetime - age) / 10f;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean shell;

        public Provider(SpriteSet sprites) {
            this(sprites, false);
        }

        public Provider(SpriteSet sprites, boolean shell) {
            this.sprites = sprites;
            this.shell = shell;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new CasingParticle(level, x, y, z, vx, vy, vz, sprites, shell);
        }
    }
}

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

    /** Douille en 3D : un petit pavé allongé (laiton / acier / cartouche) qui tourne sur lui-même. */
    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera camera, float partialTicks) {
        var cam = camera.getPosition();
        float px = (float) (net.minecraft.util.Mth.lerp(partialTicks, xo, x) - cam.x());
        float py = (float) (net.minecraft.util.Mth.lerp(partialTicks, yo, y) - cam.y()) + (shell ? 0.02f : 0.008f);
        float pz = (float) (net.minecraft.util.Mth.lerp(partialTicks, zo, z) - cam.z());
        float angle = net.minecraft.util.Mth.lerp(partialTicks, oRoll, roll);
        // Couchée au sol (axe horizontal), elle tourne autour de l'axe vertical en roulant
        org.joml.Quaternionf q = new org.joml.Quaternionf().rotateY(angle).rotateZ(onGround ? 0 : angle * 1.7f);
        float len = shell ? 0.045f : 0.028f, rad = shell ? 0.013f : 0.0065f;
        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        int light = getLightColor(partialTicks);
        float[][] faces = {
                {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        float[] size = {len, rad, rad};
        for (float[] n : faces) {
            // Les 4 coins de la face, dans le repère de la douille
            int axis = n[0] != 0 ? 0 : n[1] != 0 ? 1 : 2;
            int a1 = (axis + 1) % 3, a2 = (axis + 2) % 3;
            float shade = axis == 1 ? (n[1] > 0 ? 1.0f : 0.55f) : (axis == 0 ? 0.8f : 0.7f);
            float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
            float[][] uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
            for (int k = 0; k < 4; k++) {
                float[] c = new float[3];
                c[axis] = n[axis] * size[axis];
                c[a1] = corners[k][0] * size[a1];
                c[a2] = corners[k][1] * size[a2] * Math.signum(n[axis]);
                org.joml.Vector3f v = q.transform(new org.joml.Vector3f(c[0], c[1], c[2]));
                buffer.addVertex(px + v.x, py + v.y, pz + v.z).setUv(uv[k][0], uv[k][1])
                        .setColor(rCol * shade, gCol * shade, bCol * shade, alpha).setLight(light);
            }
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

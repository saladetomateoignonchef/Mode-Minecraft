package fr.modeminecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Particules du réalisme naturel : brume d'altitude, lucioles, papillons, empreintes de pas et traces de pneus. */
public final class NatureParticles {
    private NatureParticles() {}

    /** Nappe de brume : grande, très translucide, qui dérive lentement avec le vent. */
    public static class Mist extends TextureSheetParticle {
        Mist(ClientLevel level, double x, double y, double z, double vx, double vz, SpriteSet sprites) {
            super(level, x, y, z);
            this.xd = vx;
            this.zd = vz;
            this.yd = 0;
            this.gravity = 0;
            this.lifetime = 140 + random.nextInt(60);
            this.quadSize = 5f + random.nextFloat() * 4f;
            this.hasPhysics = false;
            this.alpha = 0;
            pickSprite(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            float t = age / (float) lifetime;
            alpha = 0.22f * Mth.sin(t * Mth.PI);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** Luciole : petit point jaune-vert qui flotte, s'allume et s'éteint doucement. Aucun son. */
    public static class Firefly extends TextureSheetParticle {
        private final float phase;

        Firefly(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            this.xd = this.yd = this.zd = 0;
            this.gravity = 0;
            this.lifetime = 160 + random.nextInt(140);
            this.quadSize = 0.07f;
            this.hasPhysics = false;
            this.phase = random.nextFloat() * 6.28f;
            pickSprite(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            xd += (random.nextDouble() - 0.5) * 0.004;
            yd += (random.nextDouble() - 0.5) * 0.003;
            zd += (random.nextDouble() - 0.5) * 0.004;
            xd *= 0.96;
            yd *= 0.96;
            zd *= 0.96;
            float blink = 0.5f + 0.5f * Mth.sin(age * 0.12f + phase);
            alpha = Mth.clamp(blink, 0f, 1f) * Math.min(1f, Math.min(age / 20f, (lifetime - age) / 20f));
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** Papillon : trajectoire erratique au-dessus des fleurs, ailes qui battent (deux images). */
    public static class Butterfly extends TextureSheetParticle {
        private final SpriteSet sprites;
        private double tx, ty, tz;

        Butterfly(ClientLevel level, double x, double y, double z, float r, float g, float b, SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            this.gravity = 0;
            this.lifetime = 500 + random.nextInt(300);
            this.quadSize = 0.12f;
            this.hasPhysics = false;
            setColor(r, g, b);
            pickTarget();
            setSprite(sprites.get(0, 1));
        }

        private void pickTarget() {
            double a = random.nextDouble() * 6.283;
            tx = Math.cos(a) * 0.035;
            tz = Math.sin(a) * 0.035;
            ty = (random.nextDouble() - 0.45) * 0.03;
        }

        @Override
        public void tick() {
            super.tick();
            if (age % 14 == 0) {
                pickTarget();
            }
            xd += (tx - xd) * 0.12;
            yd += (ty - yd) * 0.12 + Math.sin(age * 0.6) * 0.004;
            zd += (tz - zd) * 0.12;
            setSprite(sprites.get(age / 3 % 2, 1));                      // les ailes battent
            alpha = Math.min(1f, Math.min(age / 15f, (lifetime - age) / 30f));
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** Empreinte ou trace de pneu : carré posé à plat sur le sol, orienté selon la direction du déplacement, qui s'estompe en trois minutes. */
    public static class Print extends TextureSheetParticle {
        private final float yaw;
        private final boolean flip;

        Print(ClientLevel level, double x, double y, double z, float yaw, boolean flip, float size, float r, float g, float b, SpriteSet sprites) {
            super(level, x, y, z);
            this.xd = this.yd = this.zd = 0;
            this.gravity = 0;
            this.lifetime = 3600;
            this.quadSize = size;
            this.hasPhysics = false;
            this.yaw = yaw;
            this.flip = flip;
            setColor(r, g, b);
            pickSprite(sprites);
        }

        @Override
        public void tick() {
            age++;
            if (age >= lifetime) {
                remove();
            }
            alpha = 0.55f * Math.min(1f, (lifetime - age) / 600f);
        }

        @Override
        public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
            Vec3 cam = camera.getPosition();
            float cx = (float) (x - cam.x), cy = (float) (y - cam.y), cz = (float) (z - cam.z);
            float s = quadSize;
            Quaternionf rot = new Quaternionf().rotationY(-yaw).rotateX(-Mth.HALF_PI);        // à plat, tournée selon la direction
            Vector3f[] v = {new Vector3f(-s, -s, 0), new Vector3f(-s, s, 0), new Vector3f(s, s, 0), new Vector3f(s, -s, 0)};
            for (Vector3f p : v) {
                if (flip) {
                    p.x = -p.x;
                }
                p.rotate(rot).add(cx, cy, cz);
            }
            float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
            int light = getLightColor(partialTicks);
            if (flip) {
                float t = u0;
                u0 = u1;
                u1 = t;
            }
            buffer.addVertex(v[0].x, v[0].y, v[0].z).setUv(u1, v1).setColor(rCol, gCol, bCol, alpha).setLight(light);
            buffer.addVertex(v[1].x, v[1].y, v[1].z).setUv(u1, v0).setColor(rCol, gCol, bCol, alpha).setLight(light);
            buffer.addVertex(v[2].x, v[2].y, v[2].z).setUv(u0, v0).setColor(rCol, gCol, bCol, alpha).setLight(light);
            buffer.addVertex(v[3].x, v[3].y, v[3].z).setUv(u0, v1).setColor(rCol, gCol, bCol, alpha).setLight(light);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class MistProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public MistProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double vx, double vy, double vz) {
            return new Mist(l, x, y, z, vx, vz, sprites);
        }
    }

    public static class FireflyProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FireflyProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double vx, double vy, double vz) {
            return new Firefly(l, x, y, z, sprites);
        }
    }

    /** Papillon : la couleur est passée dans (vx, vy, vz) = (rouge, vert, bleu). */
    public static class ButterflyProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public ButterflyProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double vx, double vy, double vz) {
            return new Butterfly(l, x, y, z, (float) vx, (float) vy, (float) vz, sprites);
        }
    }

    /** Empreinte : (vx, vy, vz) = (direction en radians, 1 pour le pied droit, teinte de gris 0..1). */
    public static class PrintProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float size;

        public PrintProvider(SpriteSet sprites, float size) {
            this.sprites = sprites;
            this.size = size;
        }

        @Override
        public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double vx, double vy, double vz) {
            float g = (float) vz;
            return new Print(l, x, y, z, (float) vx, vy > 0.5, size, g, g, Math.min(1f, g * 1.05f), sprites);
        }
    }
}

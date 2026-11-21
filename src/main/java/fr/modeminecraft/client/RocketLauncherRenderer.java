package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.RocketLauncherEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Lance-roquettes statiques, dessinés en volumes simples : trépied et berceau de 4 tubes (Type 63), ou socle, pivot et
 * bloc de 12 tubes (batterie de 122 mm). Les tubes chargés montrent l'ogive rouge de leur roquette.
 */
public class RocketLauncherRenderer extends EntityRenderer<RocketLauncherEntity> {
    private static final ResourceLocation TEXTURE = ModeMinecraft.id("textures/entity/rocket_launcher.png");

    public RocketLauncherRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.6f;
    }

    @Override
    public ResourceLocation getTextureLocation(RocketLauncherEntity e) {
        return TEXTURE;
    }

    @Override
    public void render(RocketLauncherEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        RocketLauncherEntity.Kind k = e.kind();
        float az = e.azimuth(), el = e.elevation();
        boolean light63 = k == RocketLauncherEntity.Kind.TYPE63;
        float ph = (float) e.pivotHeight();
        int steel = 0xFF8A8E80, olive = 0xFFFFFFFF, dark = 0xFF5A5E50, red = 0xFFFF5040;
        pose.pushPose();
        if (light63) {
            // trépied : 3 pieds écartés à 120°, qui ne tournent pas avec les tubes
            for (int i = 0; i < 3; i++) {
                pose.pushPose();
                pose.translate(0, ph, 0);
                pose.mulPose(Axis.YP.rotationDegrees(-az + 180 + (i - 1) * 120));
                pose.mulPose(Axis.XP.rotationDegrees(-52));
                box(pose, vc, light, steel, -0.035f, -0.035f, -0.95f, 0.035f, 0.035f, 0f);
                pose.popPose();
            }
            pose.pushPose();
            pose.translate(0, ph - 0.08, 0);
            box(pose, vc, light, dark, -0.08f, -0.05f, -0.08f, 0.08f, 0.08f, 0.08f);     // tête du trépied
            pose.popPose();
        } else {
            box(pose, vc, light, dark, -1.2f, 0, -1.2f, 1.2f, 0.35f, 1.2f);                 // socle bétonné
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-az));
            box(pose, vc, light, steel, -0.5f, 0.35f, -0.5f, 0.5f, 0.6f, 0.5f);             // tourelleau
            box(pose, vc, light, dark, -0.18f, 0.6f, -0.18f, 0.18f, ph, 0.18f);              // pivot
            box(pose, vc, light, steel, -0.25f, 0.35f, -1.05f, 0.25f, 0.9f, -0.5f);          // poste de pointage
            pose.popPose();
        }
        // berceau et tubes
        pose.translate(0, ph, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-az));
        pose.mulPose(Axis.XP.rotationDegrees(-el));
        int cols = light63 ? 2 : 4;
        int rows = k.tubes / cols;
        float sp = light63 ? 0.22f : 0.3f, half = light63 ? 0.095f : 0.13f;
        float len = (float) e.tubeLength();
        float back = -len * 0.45f, front = len * 0.55f;
        box(pose, vc, light, dark, -cols * sp / 2 - 0.02f, -rows * sp / 2 - 0.06f, back + len * 0.2f,
                cols * sp / 2 + 0.02f, -rows * sp / 2 + 0.02f, front - len * 0.25f);       // berceau
        int mask = e.loadedMask();
        for (int i = 0; i < k.tubes; i++) {
            float cx = -((i % cols) - (cols - 1) / 2f) * sp;
            float cy = ((i / cols) - (rows - 1) / 2f) * sp;
            box(pose, vc, light, olive, cx - half, cy - half, back, cx + half, cy + half, front);
            box(pose, vc, light, 0xFF202220, cx - half * 0.7f, cy - half * 0.7f, front, cx + half * 0.7f, cy + half * 0.7f, front + 0.005f);
            if ((mask & 1 << i) != 0) {
                box(pose, vc, light, red, cx - half * 0.6f, cy - half * 0.6f, front - 0.06f, cx + half * 0.6f, cy + half * 0.6f, front + 0.06f);
            }
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    /** Pavé texturé (texture entière sur chaque face), teinté. */
    public static void box(PoseStack pose, VertexConsumer vc, int light, int argb, float x0, float y0, float z0, float x1, float y1, float z1) {
        PoseStack.Pose p = pose.last();
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        // (x, y, z) des 4 coins de chaque face + normale
        float[][] faces = {
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0},
                {x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0},
                {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1},
                {x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1},
                {x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0},
                {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0},
        };
        float[][] uv = {{0, 1}, {0, 0}, {1, 0}, {1, 1}};
        for (float[] f : faces) {
            for (int v = 0; v < 4; v++) {
                vc.addVertex(p, f[v * 3], f[v * 3 + 1], f[v * 3 + 2]).setColor(r, g, b, a).setUv(uv[v][0], uv[v][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, f[12], f[13], f[14]);
            }
        }
    }
}

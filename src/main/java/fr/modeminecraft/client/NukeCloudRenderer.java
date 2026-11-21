package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.modeminecraft.nuclear.NukeCloudEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Champignon atomique en volumes : boule de feu incandescente, colonne, anneau de condensation, chapeau en tore qui
 * passe de l'orange au gris, nuage de base ; il s'estompe en fin de vie.
 */
public class NukeCloudRenderer extends EntityRenderer<NukeCloudEntity> {
    public NukeCloudRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(NukeCloudEntity e) {
        return BombRenderer.PLAIN;
    }

    private static int col(float heat, float alpha) {
        // chaleur 1 = orange incandescent, 0 = gris brun
        int r = (int) Mth.lerp(heat, 120, 255), g = (int) Mth.lerp(heat, 108, 150), b = (int) Mth.lerp(heat, 96, 40);
        return (int) (Mth.clamp(alpha, 0, 1) * 255) << 24 | r << 16 | g << 8 | b;
    }

    @Override
    public void render(NukeCloudEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = e.tickCount + partial;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(BombRenderer.PLAIN));
        int full = LightTexture.FULL_BRIGHT;
        float fade = t > NukeCloudEntity.LIFE - 400 ? Math.max(0, (NukeCloudEntity.LIFE - t) / 400f) : 1;
        float heat = Mth.clamp(1 - t / 260f, 0, 1);
        float h = NukeCloudEntity.capHeight(t), capR = NukeCloudEntity.capRadius(t), capT = NukeCloudEntity.capThickness(t);
        pose.pushPose();
        // boule de feu initiale
        if (t < 80) {
            float r = Math.min(28, t * 2.2f), a = t < 40 ? 0.95f : (80 - t) / 40f;
            for (int i = 0; i < 3; i++) {
                pose.pushPose();
                pose.translate(0, r * 0.6f + t * 0.3f, 0);
                pose.mulPose(Axis.YP.rotationDegrees(i * 30));
                pose.mulPose(Axis.XP.rotationDegrees(i * 35));
                RocketLauncherRenderer.box(pose, vc, full, col(1, a), -r, -r, -r, r, r, r);
                pose.popPose();
            }
        }
        // colonne
        int seg = 10;
        for (int i = 0; i < seg; i++) {
            float y0 = h * i / seg, y1 = h * (i + 1) / seg + 1;
            float rr = 9 + 3 * (float) Math.sin(i * 1.7 + t * 0.02) - i * 0.25f;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 22.5f + t * 0.1f));
            RocketLauncherRenderer.box(pose, vc, full, col(heat * (1 - i / (float) seg) * 0.9f, 0.85f * fade), -rr, y0, -rr, rr, y1, rr);
            pose.popPose();
        }
        // anneau de condensation autour de la colonne
        if (t > 60 && t < 900) {
            float ringA = (t < 200 ? (t - 60) / 140f : (900 - t) / 700f) * 0.55f * fade;
            float ry = h * 0.55f, rr = 22 + t * 0.02f;
            for (int i = 0; i < 12; i++) {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(i * 30));
                RocketLauncherRenderer.box(pose, vc, full, 0x00F0F0F0 | (int) (ringA * 255) << 24, rr - 3, ry - 1.2f, -6, rr + 3, ry + 1.2f, 6);
                pose.popPose();
            }
        }
        // chapeau : tore + dôme
        for (int i = 0; i < 14; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * (360 / 14f) + t * 0.15f));
            float cx = capR * 0.62f, s = capR * 0.42f;
            float wob = (float) Math.sin(i * 2.3 + t * 0.03) * 2;
            RocketLauncherRenderer.box(pose, vc, full, col(heat * 0.85f, 0.9f * fade), cx - s, h - capT * 0.5f + wob, -s, cx + s, h + capT * 0.5f + wob, s);
            pose.popPose();
        }
        for (int i = 0; i < 2; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 45 + t * 0.1f));
            float s = capR * 0.55f;
            RocketLauncherRenderer.box(pose, vc, full, col(heat, 0.92f * fade), -s, h - capT * 0.2f, -s, s, h + capT * 0.75f, s);
            pose.popPose();
        }
        // nuage de base qui s'étale au sol
        float baseR = Math.min(80, 10 + t * 0.35f);
        for (int i = 0; i < 16; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 22.5f));
            RocketLauncherRenderer.box(pose, vc, light, col(heat * 0.3f, 0.7f * fade), baseR * 0.55f, 0, -baseR * 0.2f, baseR, 9, baseR * 0.2f);
            pose.popPose();
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }
}

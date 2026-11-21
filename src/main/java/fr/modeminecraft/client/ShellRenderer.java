package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.MilAmmo;
import fr.modeminecraft.entity.military.ShellEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Obus, roquettes et missiles en vol : corps allongé (couleur selon la munition : obus laiton, flèche noire, roquette
 * vert armée, missile blanc), ailerons, et traçant lumineux pour les obus de canon automatique.
 */
public class ShellRenderer extends EntityRenderer<ShellEntity> {
    private static final ResourceLocation TEX = ModeMinecraft.id("textures/entity/shell_colors.png");
    /** Lignes de la texture : 0 laiton, 1 noir, 2 vert armée, 3 blanc, 4 rouge, 5 gris, 6 traçant. */
    private static final int BRASS = 0, BLACK = 1, OLIVE = 2, WHITE = 3, RED = 4, GREY = 5, TRACER = 6;

    public ShellRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(ShellEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        MilAmmo.Shell s = e.shell();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, e.yRotO, e.getYRot()) - 90));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.getXRot())));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        Matrix4f m = pose.last().pose();
        switch (s) {
            case AP_I, HE_I -> {
                VertexConsumer glow = buffers.getBuffer(RenderType.eyes(TEX));
                box(glow, m, -1.2f, 0.0f, 0.05f, TRACER, LightTexture.FULL_BRIGHT);
            }
            case GRENADE_40 -> box(vc, m, -0.08f, 0.08f, 0.07f, OLIVE, light);
            case APFSDS -> {
                box(vc, m, -0.5f, 0.4f, 0.03f, BLACK, light);
                fins(vc, m, -0.5f, 0.12f, BLACK, light);
            }
            case HE, HEAT -> {
                box(vc, m, -0.35f, 0.25f, 0.07f, s == MilAmmo.Shell.HE ? OLIVE : GREY, light);
                box(vc, m, 0.25f, 0.4f, 0.04f, s == MilAmmo.Shell.HE ? RED : BLACK, light);
            }
            case REFLEKS, ATGM -> {
                box(vc, m, -0.6f, 0.5f, 0.08f, OLIVE, light);
                fins(vc, m, -0.55f, 0.2f, BLACK, light);
            }
            case SAM -> {
                box(vc, m, -0.75f, 0.6f, 0.06f, WHITE, light);
                fins(vc, m, -0.7f, 0.16f, GREY, light);
            }
            case ROCKET_122, ROCKET_107, S8, HYDRA -> {
                box(vc, m, -0.9f, 0.6f, 0.07f, OLIVE, light);
                box(vc, m, 0.6f, 0.8f, 0.045f, RED, light);
                fins(vc, m, -0.85f, 0.16f, OLIVE, light);
            }
            case ROCKET_227, ATACMS -> {
                float r = s == MilAmmo.Shell.ATACMS ? 0.3f : 0.12f;
                box(vc, m, -2.0f, 1.4f, r, WHITE, light);
                box(vc, m, 1.4f, 1.9f, r * 0.6f, GREY, light);
                fins(vc, m, -1.9f, r * 2.2f, GREY, light);
            }
            case SAM_LONG, SAM_AGILE, ABM, SAM_EXTREME -> {
                box(vc, m, -2.2f, 1.6f, 0.22f, WHITE, light);
                box(vc, m, 1.6f, 2.2f, 0.14f, GREY, light);
                fins(vc, m, -2.1f, 0.5f, GREY, light);
            }
            case BALLISTIC -> {
                box(vc, m, -3.5f, 2.5f, 0.45f, WHITE, light);
                box(vc, m, 2.5f, 3.6f, 0.3f, GREY, light);
                fins(vc, m, -3.4f, 0.9f, GREY, light);
            }
            case SUBMUNITION -> box(vc, m, -0.15f, 0.15f, 0.1f, OLIVE, light);
            default -> box(vc, m, -0.2f, 0.2f, 0.05f, BRASS, light);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    /** Pavé allongé le long de X, section carrée de demi-côté r. */
    private static void box(VertexConsumer vc, Matrix4f m, float x0, float x1, float r, int row, int light) {
        float v0 = row / 8f + 0.01f, v1 = (row + 1) / 8f - 0.01f;
        float[][] faces = {
                {x0, -r, -r, x1, -r, -r, x1, r, -r, x0, r, -r},
                {x0, r, r, x1, r, r, x1, -r, r, x0, -r, r},
                {x0, r, -r, x1, r, -r, x1, r, r, x0, r, r},
                {x0, -r, r, x1, -r, r, x1, -r, -r, x0, -r, -r},
                {x1, -r, -r, x1, -r, r, x1, r, r, x1, r, -r},
                {x0, r, -r, x0, r, r, x0, -r, r, x0, -r, -r}};
        for (float[] f : faces) {
            quad(vc, m, f, v0, v1, light);
        }
    }

    private static void fins(VertexConsumer vc, Matrix4f m, float x, float span, int row, int light) {
        float v0 = row / 8f + 0.01f, v1 = (row + 1) / 8f - 0.01f;
        quad(vc, m, new float[]{x, -span, 0, x + 0.25f, -span, 0, x + 0.25f, span, 0, x, span, 0}, v0, v1, light);
        quad(vc, m, new float[]{x, 0, -span, x + 0.25f, 0, -span, x + 0.25f, 0, span, x, 0, span}, v0, v1, light);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float[] f, float v0, float v1, int light) {
        float[][] uv = {{0, v0}, {1, v0}, {1, v1}, {0, v1}};
        for (int i = 0; i < 4; i++) {
            vc.addVertex(m, f[i * 3], f[i * 3 + 1], f[i * 3 + 2]).setColor(255, 255, 255, 255).setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ShellEntity e) {
        return TEX;
    }
}

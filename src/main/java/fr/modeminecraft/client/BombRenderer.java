package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.nuclear.BombEntity;
import fr.modeminecraft.nuclear.BombItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Bombes en chute : corps profilé et ailettes ; la « Fat Man » nucléaire est ventrue, jaune et noire. */
public class BombRenderer extends EntityRenderer<BombEntity> {
    static final ResourceLocation PLAIN = ModeMinecraft.id("textures/entity/plain.png");

    public BombRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(BombEntity e) {
        return PLAIN;
    }

    @Override
    public void render(BombEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(PLAIN));
        BombItem.Kind k = e.kind();
        pose.pushPose();
        pose.translate(0, 0.35, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(e.pitchDeg()));
        int body = k == BombItem.Kind.HE ? 0xFF5A6438 : k == BombItem.Kind.NAPALM ? 0xFF8A8E92 : 0xFFD8B020;
        int band = k == BombItem.Kind.NAPALM ? 0xFFB02818 : 0xFF202020;
        float r = k == BombItem.Kind.NUKE ? 0.75f : 0.28f, len = k == BombItem.Kind.NUKE ? 1.6f : 1.3f;
        RocketLauncherRenderer.box(pose, vc, light, body, -r, -r, -len * 0.4f, r, r, len * 0.45f);
        RocketLauncherRenderer.box(pose, vc, light, body, -r * 0.7f, -r * 0.7f, len * 0.45f, r * 0.7f, r * 0.7f, len * 0.6f);   // ogive
        RocketLauncherRenderer.box(pose, vc, light, band, -r * 1.02f, -r * 1.02f, len * 0.1f, r * 1.02f, r * 1.02f, len * 0.18f);
        RocketLauncherRenderer.box(pose, vc, light, body, -r * 0.45f, -r * 0.45f, -len * 0.75f, r * 0.45f, r * 0.45f, -len * 0.4f);
        float f = k == BombItem.Kind.NUKE ? 0.95f : 0.45f;                                                                  // empennage
        RocketLauncherRenderer.box(pose, vc, light, band, -f, -0.03f, -len * 0.8f, f, 0.03f, -len * 0.55f);
        RocketLauncherRenderer.box(pose, vc, light, band, -0.03f, -f, -len * 0.8f, 0.03f, f, -len * 0.55f);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }
}

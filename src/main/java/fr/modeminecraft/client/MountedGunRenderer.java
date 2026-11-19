package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.MountedGunEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Mitrailleuse posée : trépied fixe + arme qui suit le regard du tireur. */
public class MountedGunRenderer extends EntityRenderer<MountedGunEntity> {
    public static final ModelResourceLocation TRIPOD = ModelResourceLocation.standalone(ModeMinecraft.id("item/mg_tripod_3d"));

    public MountedGunRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MountedGunEntity gun, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        var mc = Minecraft.getInstance();
        var renderer = mc.getItemRenderer();
        pose.pushPose();
        // Trépied (orienté comme lors de la pose)
        pose.pushPose();
        pose.translate(0, MountedGunEntity.PIVOT, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-(gun.getYRot() + 90)));
        renderer.render(new ItemStack(gun.kind().item()), ItemDisplayContext.NONE, false, pose, buffers, light,
                OverlayTexture.NO_OVERLAY, mc.getModelManager().getModel(TRIPOD));
        pose.popPose();
        // L'arme : le tireur la fait pivoter (on suit son regard en douceur)
        float gunYaw = gun.gunYaw(), gunPitch = gun.gunPitch();
        if (gun.getFirstPassenger() instanceof Player p) {
            float base = gun.getYRot();
            gunYaw = base + Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(partialTick, p.yRotO, p.getYRot()) - base), -70, 70);
            gunPitch = Mth.clamp(Mth.lerp(partialTick, p.xRotO, p.getXRot()), -25, 30);
        }
        pose.translate(0, MountedGunEntity.PIVOT, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-(gunYaw + 90)));
        pose.mulPose(Axis.ZP.rotationDegrees(-gunPitch));
        renderer.renderStatic(new ItemStack(gun.kind().item()), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, gun.level(), gun.getId());
        pose.popPose();
        super.render(gun, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MountedGunEntity entity) {
        return ModeMinecraft.id("textures/item/gun_palette_default.png");
    }
}

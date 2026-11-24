package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Rendu des véhicules : carrosserie (modèle à l'échelle 1/2, dessiné x2) et roues qui tournent / braquent. */
public class VehicleRenderer extends EntityRenderer<VehicleEntity> {
    private static final float SCALE = 2f;

    /** Essieux (x en pixels du modèle), voie (z gauche / droite) et rayon des roues. Le premier essieu braque. */
    private record Wheels(float[] axles, float zl, float zr, float r) {}

    public VehicleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 1.0f;
    }

    public static ModelResourceLocation part(String vehicle, String part) {
        return ModelResourceLocation.standalone(ModeMinecraft.id("vehicle/" + vehicle + "_" + part));
    }

    private static Wheels wheels(VehicleEntity.Type t) {
        return switch (t) {
            case QUAD -> new Wheels(new float[]{13, 3}, 2.0f, 14.0f, 2.6f);
            case POLICE_CAR -> new Wheels(new float[]{21, -4}, 1.2f, 14.8f, 3.0f);
            case JEEP -> new Wheels(new float[]{19, -3}, 1.3f, 14.7f, 3.4f);
            case ARMORED_TRUCK -> new Wheels(new float[]{24.6f, 2.5f, -8.6f}, 0.4f, 15.6f, 4.0f);
            case SEDAN, TAXI -> new Wheels(new float[]{20, -4}, 1.0f, 15.0f, 2.8f);
            case BUS -> new Wheels(new float[]{25f, -8f}, 0.6f, 15.4f, 3.6f);
            case AMBULANCE -> new Wheels(new float[]{22f, -6f}, 0.8f, 15.2f, 3.2f);
        };
    }

    @Override
    public void render(VehicleEntity v, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        var mc = Minecraft.getInstance();
        var t = v.type();
        ItemStack stack = new ItemStack(t.item());
        float bodyYaw = Mth.rotLerp(partial, v.yRotO, v.getYRot());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90 - bodyYaw));
        pose.scale(SCALE, SCALE, SCALE);
        // carrosserie (légère suspension avec la vitesse)
        pose.pushPose();
        pose.translate(0, 0.5 + Math.abs(v.speed()) * 0.01 * Mth.sin((v.tickCount + partial) * 0.9f), 0);
        mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY,
                mc.getModelManager().getModel(part(t.id, "body")));
        pose.popPose();
        // roues
        Wheels w = wheels(t);
        float rot = Mth.lerp(partial, v.wheelRotO, v.wheelRot) * 57.3f / (w.r() / 16f * SCALE);
        var wheelModel = mc.getModelManager().getModel(part(t.id, "wheel"));
        for (int i = 0; i < w.axles().length; i++) {
            for (float z : new float[]{w.zl(), w.zr()}) {
                pose.pushPose();
                pose.translate((w.axles()[i] - 8) / 16f, w.r() / 16f, (z - 8) / 16f);
                if (i == 0) {
                    pose.mulPose(Axis.YP.rotationDegrees(v.steering() * 28));
                }
                pose.mulPose(Axis.ZP.rotationDegrees(-rot));
                mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, wheelModel);
                pose.popPose();
            }
        }
        pose.popPose();
        super.render(v, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(VehicleEntity v) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}

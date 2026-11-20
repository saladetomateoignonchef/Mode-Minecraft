package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.drone.DroneEntity;
import fr.modeminecraft.entity.drone.DroneType;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Rendu des drones : corps 3D, hélices qui tournent, inclinaison selon la vitesse. */
public class DroneRenderer extends EntityRenderer<DroneEntity> {
    private static final ItemStack DUMMY = new ItemStack(Items.STICK);

    public DroneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.3f;
    }

    public static ModelResourceLocation part(String drone, String part) {
        return ModelResourceLocation.standalone(ModeMinecraft.id("drone/" + drone + "_" + part));
    }

    /** Position des hélices (pixels du modèle : x, y, z). */
    private static float[][] rotors(DroneType t) {
        return switch (t) {
            case HORNET -> new float[][]{{8, 2.4f, 8}};
            case FPV -> new float[][]{{12.5f, 2.6f, 8}, {3.5f, 2.6f, 8}, {8, 2.6f, 12.5f}, {8, 2.6f, 3.5f}};
            case MAVIC -> new float[][]{{13, 3.9f, 13}, {13, 3.9f, 3}, {3, 3.9f, 13}, {3, 3.9f, 3}};
            case WASP -> new float[][]{{15, 5.0f, 8}, {11.5f, 5.0f, 14}, {4.5f, 5.0f, 14}, {1, 5.0f, 8}, {4.5f, 5.0f, 2}, {11.5f, 5.0f, 2}};
            case REAPER -> new float[][]{{19, 5.8f, 18}, {19, 5.8f, -2}, {-3, 5.8f, 18}, {-3, 5.8f, -2}};
        };
    }

    @Override
    public void render(DroneEntity d, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        var mc = Minecraft.getInstance();
        DroneType t = d.droneType();
        float bodyYaw = Mth.rotLerp(partial, d.yRotO, d.getYRot());
        Vec3 v = d.getDeltaMovement();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90 - bodyYaw));
        // inclinaison : le drone penche dans le sens de la marche
        float yawRad = (bodyYaw + 90) * Mth.DEG_TO_RAD;
        double fwd = v.x * -Mth.sin(bodyYaw * Mth.DEG_TO_RAD) + v.z * Mth.cos(bodyYaw * Mth.DEG_TO_RAD);
        double side = v.x * Mth.cos(bodyYaw * Mth.DEG_TO_RAD) + v.z * Mth.sin(bodyYaw * Mth.DEG_TO_RAD);
        pose.mulPose(Axis.ZP.rotationDegrees((float) Mth.clamp(-fwd * 40, -25, 25)));
        pose.mulPose(Axis.XP.rotationDegrees((float) Mth.clamp(side * 40, -25, 25)));
        if (t == DroneType.FPV) {
            pose.mulPose(Axis.YP.rotationDegrees(45));           // cadre en X
        }
        pose.translate(-0.5, 0, -0.5);
        mc.getItemRenderer().render(DUMMY, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY,
                mc.getModelManager().getModel(part(t.id, "body")));
        var rotor = mc.getModelManager().getModel(part(t.id, "rotor"));
        float spin = Mth.lerp(partial, d.rotorSpinO, d.rotorSpin) * 57.3f;
        float[][] rs = rotors(t);
        for (int i = 0; i < rs.length; i++) {
            pose.pushPose();
            pose.translate(rs[i][0] / 16f, rs[i][1] / 16f, rs[i][2] / 16f);
            pose.mulPose(Axis.YP.rotationDegrees((i % 2 == 0 ? spin : -spin) + i * 37));
            pose.translate(-0.5, 0, -0.5);
            mc.getItemRenderer().render(DUMMY, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, rotor);
            pose.popPose();
        }
        pose.popPose();
        super.render(d, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DroneEntity d) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}

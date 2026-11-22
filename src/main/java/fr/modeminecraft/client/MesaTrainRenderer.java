package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.MilGeometry;
import fr.modeminecraft.entity.train.MesaTrainCarEntity;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Rendu de la locomotive et des wagons (modèles voxel générés par tools/train_models.py) ; portes coulissantes animées. */
public class MesaTrainRenderer extends EntityRenderer<MesaTrainCarEntity> {
    public MesaTrainRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0;
    }

    public static ModelResourceLocation part(String model, String part) {
        return ModelResourceLocation.standalone(ModeMinecraft.id("vehicle/" + model + "_" + part));
    }

    @Override
    public boolean shouldRender(MesaTrainCarEntity car, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(car.getBoundingBox().inflate(7, 2, 7));
    }

    @Override
    public void render(MesaTrainCarEntity car, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        MilGeometry.Geo geo = MilGeometry.get(car.modelId());
        if (geo == null) {
            return;
        }
        var mc = Minecraft.getInstance();
        float s = geo.scale();
        float bodyYaw = Mth.rotLerp(partial, car.yRotO, car.getYRot());
        ItemStack stack = new ItemStack(ModItems.MESA_TRAIN_TICKET.get());          // le rendu d'objet exige un objet : n'importe lequel
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90 - bodyYaw));
        pose.scale(s, s, s);
        pose.translate(-(geo.ox() - 8) / 16f, 0, 0);
        draw(mc, pose, buffers, light, stack, car.modelId(), "hull");
        if (!car.isLoco()) {
            // portes coulissantes : chaque battant glisse d'un mètre, vers l'arrière et vers l'avant du wagon
            float open = Mth.clamp(car.doors(partial), 0, 1) / s;
            pose.pushPose();
            pose.translate(-open, 0, 0);
            draw(mc, pose, buffers, light, stack, car.modelId(), "doors");
            pose.popPose();
            pose.pushPose();
            pose.translate(open, 0, 0);
            draw(mc, pose, buffers, light, stack, car.modelId(), "doors_f");
            pose.popPose();
        }
        pose.popPose();
    }

    private static void draw(Minecraft mc, PoseStack pose, MultiBufferSource buffers, int light, ItemStack stack, String model, String part) {
        BakedModel baked = mc.getModelManager().getModel(part(model, part));
        if (baked == mc.getModelManager().getMissingModel()) {
            return;
        }
        pose.pushPose();
        pose.translate(0, 0.5, 0);
        mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, baked);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MesaTrainCarEntity car) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}

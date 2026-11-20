package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.GearItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Pantalons et rangers 3D : chaque pièce est dessinée sur la partie du corps qu'elle couvre (ceinture sur le torse,
 * jambes du pantalon et bottes sur chaque jambe), elle suit donc la marche, la course, l'accroupissement...
 * Repère d'une jambe (pixels) : [6 ; 10] x [2 ; 14] x [6 ; 10], hanche en haut (y = 14), devant vers -Z.
 */
public class GearLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public GearLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    public static ModelResourceLocation part(String item, String part) {
        return ModelResourceLocation.standalone(ModeMinecraft.id("gear/" + item + "_" + part));
    }

    /** Pièces 3D séparées de chaque objet (enregistrées au chargement des modèles). */
    public static String[] parts(GearItem item) {
        return item.isBoots() ? new String[]{"boot_l", "boot_r"} : new String[]{"belt", "leg_l", "leg_r"};
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof GearItem gear)) {
                continue;
            }
            String id = BuiltInRegistries.ITEM.getKey(gear).getPath();
            M model = getParentModel();
            if (!gear.isBoots()) {
                draw(pose, buffers, light, stack, model.body, part(id, "belt"));
                draw(pose, buffers, light, stack, model.leftLeg, part(id, "leg_l"));
                draw(pose, buffers, light, stack, model.rightLeg, part(id, "leg_r"));
            } else {
                draw(pose, buffers, light, stack, model.leftLeg, part(id, "boot_l"));
                draw(pose, buffers, light, stack, model.rightLeg, part(id, "boot_r"));
            }
        }
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, int light, ItemStack stack, ModelPart bodyPart, ModelResourceLocation model) {
        var mc = Minecraft.getInstance();
        pose.pushPose();
        bodyPart.translateAndRotate(pose);
        pose.translate(0, 6 / 16f, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY,
                mc.getModelManager().getModel(model));
        pose.popPose();
    }
}

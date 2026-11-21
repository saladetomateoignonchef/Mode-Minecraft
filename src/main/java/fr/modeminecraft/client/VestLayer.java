package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.item.VestItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Dessine le vrai modèle 3D du gilet sur le torse (joueurs, zombies, squelettes, supports d'armure...).
 * Repère du modèle (pixels) : torse dans [4 ; 12] x [2 ; 14] x [6 ; 10], cou en haut (y = 14), devant vers -Z.
 */
public class VestLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public VestLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (RenderLod.far(entity)) {
            return;                                   // trop loin : détail invisible
        }
        ItemStack vest = fr.modeminecraft.item.tactical.TacticalSlots.vest(entity);
        if (!(vest.getItem() instanceof VestItem)) {
            return;
        }
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // Le modèle est en "y vers le haut" ; le corps de l'entité est en "y vers le bas", cou en 0
        pose.translate(0, 6 / 16f, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        if (fr.modeminecraft.item.tactical.TacticalSlots.vestInTacticalSlot(entity)
                && !entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            pose.scale(1.14f, 1.04f, 1.32f);                         // porté par-dessus un plastron : un peu plus large
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(entity, vest, ItemDisplayContext.NONE, false, pose, buffers,
                entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId());
        pose.popPose();
    }
}

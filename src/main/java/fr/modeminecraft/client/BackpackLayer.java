package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.item.tactical.TacticalSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Sac à dos tactique porté : dans le dos du joueur (modèle 3D). */
public class BackpackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public BackpackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack bag = TacticalSlots.backpack(player);
        if (bag.isEmpty() || player.isInvisible()) {
            return;
        }
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // repère du corps : y vers le bas depuis le cou, +z vers l'arrière ; le modèle est en y vers le haut
        pose.translate(0, 6 / 16f, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        Minecraft.getInstance().getItemRenderer().renderStatic(player, bag, ItemDisplayContext.NONE, false, pose, buffers,
                player.level(), light, OverlayTexture.NO_OVERLAY, player.getId() + 7);
        pose.popPose();
    }
}

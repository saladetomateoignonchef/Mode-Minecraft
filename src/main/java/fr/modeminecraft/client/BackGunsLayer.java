package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Affiche les armes rangées : fusil en travers du dos, pistolet sur la cuisse droite. */
public class BackGunsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Map<Integer, ItemStack[]> GUNS = new ConcurrentHashMap<>();

    public BackGunsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void update(int entityId, ItemStack back, ItemStack hip) {
        GUNS.put(entityId, new ItemStack[]{back, hip});
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack[] guns = GUNS.get(player.getId());
        if (guns == null || player.isInvisible()) {
            return;
        }
        var renderer = Minecraft.getInstance().getItemRenderer();
        double bag = fr.modeminecraft.item.tactical.TacticalSlots.backpack(player).isEmpty() ? 0 : 0.2;   // par-dessus le sac à dos
        if (guns[0].is(fr.modeminecraft.registry.ModItems.DRAGONSLAYER.get())) {
            // épée géante en diagonale : de l'épaule droite (poignée) à la hanche gauche, plat de la lame vers l'arrière
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            pose.translate(0, 0.23, 0.3 + bag);
            pose.mulPose(Axis.ZP.rotationDegrees(-30));
            renderer.renderStatic(player, guns[0], ItemDisplayContext.FIXED, false, pose, buffers, player.level(), light,
                    OverlayTexture.NO_OVERLAY, player.getId());
            pose.popPose();
        } else if (!guns[0].isEmpty()) {
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            pose.translate(0.0, 0.38, 0.19 + bag);     // milieu du dos, juste derrière le corps
            pose.mulPose(Axis.ZP.rotationDegrees(180 + 40)); // en travers, canon vers le haut à droite
            pose.scale(0.9f, 0.9f, 0.9f);
            renderer.renderStatic(player, guns[0], ItemDisplayContext.NONE, false, pose, buffers, player.level(), light,
                    OverlayTexture.NO_OVERLAY, player.getId());
            pose.popPose();
        }
        if (!guns[1].isEmpty()) {
            pose.pushPose();
            getParentModel().rightLeg.translateAndRotate(pose);
            pose.translate(-0.17, 0.2, 0.0);            // côté extérieur de la cuisse droite
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(-100)); // canon vers le bas
            pose.scale(0.6f, 0.6f, 0.6f);
            renderer.renderStatic(player, guns[1], ItemDisplayContext.NONE, false, pose, buffers, player.level(), light,
                    OverlayTexture.NO_OVERLAY, player.getId() + 1);
            pose.popPose();
        }
    }
}

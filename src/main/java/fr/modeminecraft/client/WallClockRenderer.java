package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.block.city.WallClockBlockEntity;
import fr.modeminecraft.block.police.FacingDecoBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Aiguilles de l'horloge murale (heures et minutes du monde Minecraft : 6:00 au lever du jour). */
public class WallClockRenderer implements BlockEntityRenderer<WallClockBlockEntity> {
    public WallClockRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(WallClockBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        long t = Math.floorMod(be.getLevel().getDayTime() + 6000, 24000);              // 0 = minuit
        float hours = t / 1000f, minutes = (t % 1000) / 1000f * 60;
        float yaw = -be.getBlockState().getValue(FacingDecoBlock.FACING).toYRot();
        Font font = Minecraft.getInstance().font;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw + 180));
        pose.translate(0, 0, 0.37);                                                   // devant le cadran
        hand(pose, buffers, font, (hours % 12) / 12f * 360, 0.17f, 0.035f, 0xFF101010, light);
        hand(pose, buffers, font, minutes / 60f * 360, 0.27f, 0.022f, 0xFF202020, light);
        pose.popPose();
    }

    /** Une aiguille : rectangle plein (fond de texte) tourné autour du centre du cadran. */
    private static void hand(PoseStack pose, MultiBufferSource buffers, Font font, float deg, float len, float width, int color, int light) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(deg));
        pose.scale(width / 8f, len / 8f, 1);
        pose.translate(-4, -8, 0);
        font.drawInBatch(" ", 0, 0, 0, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, color, light);
        pose.popPose();
    }
}

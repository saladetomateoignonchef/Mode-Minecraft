package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.block.police.JailDoorBlockEntity;
import fr.modeminecraft.block.police.SlidingJailDoorBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.LightTexture;

/** Minuterie de détention affichée au-dessus de la porte de cellule (des deux côtés), en rouge. */
public class JailDoorRenderer implements BlockEntityRenderer<JailDoorBlockEntity> {
    public JailDoorRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(JailDoorBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = be.getLevel();
        if (level == null || !be.getBlockState().getValue(SlidingJailDoorBlock.LOCKED) || be.lockedUntil <= 0) {
            return;
        }
        long left = Math.max(0, be.lockedUntil - level.getGameTime());
        String text = String.format("%d:%02d", left / 1200, (left / 20) % 60);
        Font font = Minecraft.getInstance().font;
        float yaw = -be.getBlockState().getValue(SlidingJailDoorBlock.FACING).toYRot();
        for (int side = 0; side < 2; side++) {
            pose.pushPose();
            pose.translate(0.5, 2.25, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(yaw + side * 180));
            pose.translate(0, 0, 0.2);
            pose.scale(-0.025f, -0.025f, 0.025f);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            float w = font.width(text);
            font.drawInBatch(text, -w / 2, 0, 0xFFFF3030, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0x80000000,
                    LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
    }
}

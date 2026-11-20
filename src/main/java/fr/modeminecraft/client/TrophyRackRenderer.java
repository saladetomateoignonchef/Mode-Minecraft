package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.modeminecraft.block.TrophyRackBlock;
import fr.modeminecraft.block.TrophyRackBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.joml.Matrix4f;

/** Râtelier à trophées : l'arme en relief, et pour un trophée de Chef, la plaque nominative dorée. */
public class TrophyRackRenderer implements BlockEntityRenderer<TrophyRackBlockEntity> {
    private final Font font;

    public TrophyRackRenderer(BlockEntityRendererProvider.Context context) {
        font = context.getFont();
    }

    @Override
    public void render(TrophyRackBlockEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = rack.item();
        if (stack.isEmpty()) {
            return;
        }
        float rot = 180 - rack.getBlockState().getValue(HorizontalDirectionalBlock.FACING).toYRot();
        String boss = TrophyRackBlock.bossOf(stack);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(rot));
        // l'arme, en relief devant la planche
        pose.pushPose();
        pose.translate(0, boss != null ? 0.1 : 0, 0.28);
        boolean big = stack.is(fr.modeminecraft.registry.ModItems.DRAGONSLAYER.get());
        float scale = big ? 0.55f : 0.9f;
        pose.scale(scale, scale, scale);
        if (big) {
            pose.mulPose(Axis.ZP.rotationDegrees(-45));
        }
        int shown = boss != null ? LightTexture.FULL_BRIGHT : light;           // trophée mis en valeur
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, shown, overlay, pose, buffers,
                rack.getLevel(), (int) rack.getBlockPos().asLong());
        pose.popPose();
        if (boss != null) {
            // plaque dorée
            pose.pushPose();
            pose.translate(0, -0.36, 0.33);
            Matrix4f m = pose.last().pose();
            VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
            float w = 0.36f, h = 0.075f;
            quad(vc, m, -w, -h, w, h, 0xFF8A6A10);
            quad(vc, m, -w + 0.015f, -h + 0.015f, w - 0.015f, h - 0.015f, 0xFFE0B840);
            // texte côté pièce (le +Z local pointe vers le mur) : demi-tour pour qu'il soit lisible
            pose.translate(0, 0, -0.003);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            Component name = Component.translatable("entity.modeminecraft.bot." + boss);
            float k = Math.min(0.0075f, (2 * w - 0.05f) / Math.max(1, font.width(name)));
            pose.scale(k, -k, k);
            font.drawInBatch(name, -font.width(name) / 2f, -font.lineHeight / 2f + 1, 0xFF3A2A06, false, pose.last().pose(), buffers,
                    Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, int argb) {
        vc.addVertex(m, x0, y0, 0).setColor(argb);
        vc.addVertex(m, x1, y0, 0).setColor(argb);
        vc.addVertex(m, x1, y1, 0).setColor(argb);
        vc.addVertex(m, x0, y1, 0).setColor(argb);
    }
}

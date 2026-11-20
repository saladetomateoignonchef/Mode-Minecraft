package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.modeminecraft.block.city.TrafficLightBlock;
import fr.modeminecraft.block.city.TrafficLightBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

/** Feux tricolores : la lampe active brille (vert / orange / rouge), les autres sont éteintes. */
public class TrafficLightRenderer implements BlockEntityRenderer<TrafficLightBlockEntity> {
    private static final int[][] COLORS = {{40, 230, 70}, {255, 170, 20}, {235, 30, 30}};

    public TrafficLightRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TrafficLightBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        Direction facing = be.getBlockState().getValue(TrafficLightBlock.FACING);
        int phase = TrafficLightBlock.phase(be.getLevel().getGameTime(), be.getBlockPos(), facing);
        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
        for (int side = 0; side < 2; side++) {
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + side * 180));
            Matrix4f m = pose.last().pose();
            // lampes : rouge en haut, orange au milieu, vert en bas (de 3 x 3 pixels)
            for (int lamp = 0; lamp < 3; lamp++) {
                int idx = 2 - lamp;                  // 0 = vert (bas)
                float y0 = (2 + lamp * 4.5f) / 16f, y1 = y0 + 3.5f / 16f;
                boolean on = idx == phase;
                int[] c = COLORS[idx];
                float f = on ? 1f : 0.18f;
                quad(vc, m, -1.75f / 16f, y0, 1.75f / 16f, y1, 3.05f / 16f, (int) (c[0] * f), (int) (c[1] * f), (int) (c[2] * f));
            }
            pose.popPose();
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int r, int g, int b) {
        vc.addVertex(m, x0, y0, z).setColor(r, g, b, 255);
        vc.addVertex(m, x1, y0, z).setColor(r, g, b, 255);
        vc.addVertex(m, x1, y1, z).setColor(r, g, b, 255);
        vc.addVertex(m, x0, y1, z).setColor(r, g, b, 255);
    }
}

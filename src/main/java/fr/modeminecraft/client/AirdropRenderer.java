package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.modeminecraft.entity.AirdropEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Caisse de largage sous son parachute rouge et blanc (blocs dessinés à l'échelle). */
public class AirdropRenderer extends EntityRenderer<AirdropEntity> {
    public AirdropRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(AirdropEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        var br = Minecraft.getInstance().getBlockRenderer();
        pose.pushPose();
        pose.translate(-0.5, 0, -0.5);
        br.renderSingleBlock(Blocks.BARREL.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        // suspentes
        for (int[] c : new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}}) {
            block(br, pose, buffers, light, Blocks.CHAIN.defaultBlockState(), c[0] * 0.9 + 0.05 - 0.45 * (c[0] * 2 - 1), 1, c[1] * 0.9 + 0.05 - 0.45 * (c[1] * 2 - 1), 0.1, 3, 0.1);
        }
        // voilure : panneaux alternés rouges et blancs, bombée au centre
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                double up = (4 - Math.abs(i) - Math.abs(j)) * 0.12;
                BlockState s = ((i + j) & 1) == 0 ? Blocks.WHITE_WOOL.defaultBlockState() : Blocks.RED_WOOL.defaultBlockState();
                block(br, pose, buffers, light, s, 0.5 + i * 0.9 - 0.45, 4 + up, 0.5 + j * 0.9 - 0.45, 0.9, 0.12, 0.9);
            }
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private static void block(net.minecraft.client.renderer.block.BlockRenderDispatcher br, PoseStack pose, MultiBufferSource buffers, int light,
                              BlockState s, double x, double y, double z, double sx, double sy, double sz) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale((float) sx, (float) sy, (float) sz);
        br.renderSingleBlock(s, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(AirdropEntity e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}

package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.block.WeaponRackBlock;
import fr.modeminecraft.block.WeaponRackBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Affiche les armes posées sur le râtelier : couchées, canon vers la droite, l'une au-dessus de l'autre. */
public class WeaponRackRenderer implements BlockEntityRenderer<WeaponRackBlockEntity> {
    public WeaponRackRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(WeaponRackBlockEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float rot = 180 - rack.getBlockState().getValue(WeaponRackBlock.FACING).toYRot();
        for (int i = 0; i < WeaponRackBlockEntity.SIZE; i++) {
            ItemStack stack = rack.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(rot));
            pose.translate(0, (i + 0.5) / WeaponRackBlockEntity.SIZE - 0.5, 0.06);
            pose.scale(0.85f, 0.85f, 0.85f);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers,
                    rack.getLevel(), (int) rack.getBlockPos().asLong() + i);
            pose.popPose();
        }
    }
}

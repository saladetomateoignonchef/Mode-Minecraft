package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.city.PigeonEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Pigeon / mouette : corps, tête à bec, ailes qui battent en vol, queue, pattes. */
public class PigeonRenderer extends MobRenderer<PigeonEntity, PigeonRenderer.Model> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ModeMinecraft.id("pigeon"), "main");
    private static final ResourceLocation PIGEON = ModeMinecraft.id("textures/entity/pigeon.png");
    private static final ResourceLocation GULL = ModeMinecraft.id("textures/entity/gull.png");

    public PigeonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.2f);
    }

    @Override
    public ResourceLocation getTextureLocation(PigeonEntity e) {
        return e.gull() ? GULL : PIGEON;
    }

    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, -2.5f, -3f, 3, 3, 6), PartPose.offset(0, 21, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 9).addBox(-1f, -2.5f, -1.5f, 2, 2, 2)
                .texOffs(8, 9).addBox(-0.5f, -1.5f, -2.5f, 1, 1, 1), PartPose.offset(0, 19, -3f));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 13).addBox(0, 0, -2.5f, 1, 2, 5), PartPose.offset(1.5f, 18.6f, 0));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(12, 13).addBox(-1, 0, -2.5f, 1, 2, 5), PartPose.offset(-1.5f, 18.6f, 0));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(12, 9).addBox(-1f, 0, 0, 2, 1, 3), PartPose.offsetAndRotation(0, 19, 3, -0.3f, 0, 0));
        root.addOrReplaceChild("legs", CubeListBuilder.create().texOffs(18, 0).addBox(-1f, 0, 0, 0.5f, 3, 0.5f).addBox(0.5f, 0, 0, 0.5f, 3, 0.5f),
                PartPose.offset(0, 21, 0));
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static class Model extends EntityModel<PigeonEntity> {
        private final ModelPart body, head, leftWing, rightWing, tail, legs;

        public Model(ModelPart root) {
            body = root.getChild("body");
            head = root.getChild("head");
            leftWing = root.getChild("left_wing");
            rightWing = root.getChild("right_wing");
            tail = root.getChild("tail");
            legs = root.getChild("legs");
        }

        @Override
        public void setupAnim(PigeonEntity e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
            head.yRot = headYaw * Mth.DEG_TO_RAD;
            head.xRot = e.flying() ? 0 : Math.max(0, Mth.sin(e.peck) * 0.9f);      // picore
            float f = e.flying() ? Mth.sin(age * 1.6f) * 1.1f : 0;
            leftWing.zRot = -f - (e.flying() ? 0.3f : 0);
            rightWing.zRot = f + (e.flying() ? 0.3f : 0);
            legs.visible = !e.flying();
            body.xRot = e.flying() ? 0.25f : 0;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
            body.render(pose, buffer, light, overlay, color);
            head.render(pose, buffer, light, overlay, color);
            leftWing.render(pose, buffer, light, overlay, color);
            rightWing.render(pose, buffer, light, overlay, color);
            tail.render(pose, buffer, light, overlay, color);
            legs.render(pose, buffer, light, overlay, color);
        }
    }
}

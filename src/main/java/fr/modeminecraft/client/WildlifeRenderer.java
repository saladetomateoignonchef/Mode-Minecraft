package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.wild.WildlifeEntity;
import fr.modeminecraft.entity.wild.WildlifeEntity.Kind;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rendu de la faune sauvage : chaque espèce est un modèle en boîtes décrit par une liste de pièces (nom, position, boîte, couleur 0 = principale,
 * 1 et 2 = teintes d'accent de la texture). Les pièces « leg* » balancent, « wing_l / wing_r » battent, « seg* » ondulent (serpent), « head » suit le regard.
 */
public class WildlifeRenderer extends MobRenderer<WildlifeEntity, WildlifeRenderer.Model> {
    /** Pièce : nom, décalage du pivot (px), boîte (origine puis taille), couleur de texture. */
    record Part(String name, float ox, float oy, float oz, float x, float y, float z, float w, float h, float d, int color) {}

    private static Part p(String n, float ox, float oy, float oz, float x, float y, float z, float w, float h, float d, int c) {
        return new Part(n, ox, oy, oz, x, y, z, w, h, d, c);
    }

    private static final Map<Kind, List<Part>> SPECS = new HashMap<>();

    static {
        SPECS.put(Kind.EAGLE, List.of(
                p("body", 0, 17, 0, -2, -2, -5, 4, 4, 10, 0), p("head", 0, 16, -5, -1.5f, -1.5f, -3, 3, 3, 3, 1), p("beak", 0, 17.4f, -8, -0.5f, 0, -1.2f, 1, 1.2f, 1.8f, 2),
                p("wing_l", 2, 16, 0, 0, -0.5f, -3, 13, 1, 6, 0), p("wing_r", -2, 16, 0, -13, -0.5f, -3, 13, 1, 6, 0),
                p("tail", 0, 17, 5, -2, 0, 0, 4, 0.6f, 5, 1), p("leg0", 1, 19, 0, -0.5f, 0, -0.5f, 1, 2.5f, 1, 2), p("leg1", -1, 19, 0, -0.5f, 0, -0.5f, 1, 2.5f, 1, 2)));
        SPECS.put(Kind.GULL, List.of(
                p("body", 0, 20, 0, -1.5f, -1.5f, -3, 3, 3, 6, 0), p("head", 0, 19, -3, -1, -1.5f, -1.5f, 2, 2.5f, 2.5f, 0), p("beak", 0, 19.8f, -4.6f, -0.5f, 0, -1, 1, 0.8f, 1.6f, 2),
                p("wing_l", 1.5f, 19, 0, 0, -0.5f, -2, 8, 1, 4, 1), p("wing_r", -1.5f, 19, 0, -8, -0.5f, -2, 8, 1, 4, 1),
                p("tail", 0, 20, 3, -1.2f, 0, 0, 2.4f, 0.5f, 3, 1), p("leg0", 0.8f, 21.5f, 0, -0.3f, 0, -0.3f, 0.6f, 2.5f, 0.6f, 2), p("leg1", -0.8f, 21.5f, 0, -0.3f, 0, -0.3f, 0.6f, 2.5f, 0.6f, 2)));
        SPECS.put(Kind.SCORPION, List.of(
                p("body", 0, 22.5f, 0, -2, -1, -3, 4, 1.6f, 5, 0), p("head", 0, 22.5f, -3, -1.2f, -0.8f, -1.2f, 2.4f, 1.2f, 1.4f, 1),
                p("claw_l", 1.5f, 22.4f, -4, 0, -0.5f, -3, 1.6f, 1, 3, 1), p("claw_r", -1.5f, 22.4f, -4, -1.6f, -0.5f, -3, 1.6f, 1, 3, 1),
                p("tail1", 0, 21.5f, 2, -0.7f, -1.5f, 0, 1.4f, 1.4f, 3, 1), p("tail2", 0, 19.8f, 4.2f, -0.7f, -1.5f, 0, 1.4f, 3, 1.4f, 1),
                p("tail3", 0, 18, 3.2f, -0.6f, -1.2f, -1.4f, 1.2f, 1.2f, 2.6f, 1), p("sting", 0, 18.4f, 1.6f, -0.3f, 0, -0.5f, 0.6f, 1.6f, 0.6f, 2),
                p("leg0", 2, 23.5f, -2, 0, 0, -0.3f, 2.5f, 0.6f, 0.6f, 1), p("leg1", -2, 23.5f, -2, -2.5f, 0, -0.3f, 2.5f, 0.6f, 0.6f, 1),
                p("leg2", 2, 23.5f, -0.5f, 0, 0, -0.3f, 2.5f, 0.6f, 0.6f, 1), p("leg3", -2, 23.5f, -0.5f, -2.5f, 0, -0.3f, 2.5f, 0.6f, 0.6f, 1),
                p("leg4", 2, 23.5f, 1, 0, 0, -0.3f, 2.5f, 0.6f, 0.6f, 1), p("leg5", -2, 23.5f, 1, -2.5f, 0, -0.3f, 2.5f, 0.6f, 0.6f, 1),
                p("leg6", 2, 23.5f, 2.2f, 0, 0, -0.3f, 2.5f, 0.6f, 0.6f, 1), p("leg7", -2, 23.5f, 2.2f, -2.5f, 0, -0.3f, 2.5f, 0.6f, 0.6f, 1)));
        SPECS.put(Kind.SNAKE, List.of(
                p("head", 0, 22.8f, -9, -1.3f, -0.9f, -2, 2.6f, 1.8f, 3, 1), p("tongue", 0, 23.4f, -11.2f, -0.2f, 0, 0, 0.4f, 0.2f, 1.6f, 2),
                p("seg0", 0, 23, -6, -1, -1.4f, -1.5f, 2, 1.8f, 3, 0), p("seg1", 0, 23, -3, -1, -1.4f, -1.5f, 2, 1.8f, 3, 1), p("seg2", 0, 23, 0, -1, -1.4f, -1.5f, 2, 1.8f, 3, 0),
                p("seg3", 0, 23, 3, -0.9f, -1.3f, -1.5f, 1.8f, 1.6f, 3, 1), p("seg4", 0, 23.2f, 6, -0.8f, -1.2f, -1.5f, 1.6f, 1.4f, 3, 0), p("seg5", 0, 23.3f, 9, -0.6f, -1, -1.5f, 1.2f, 1.2f, 3, 1),
                p("seg6", 0, 23.4f, 12, -0.4f, -0.8f, -1.5f, 0.8f, 0.8f, 3, 0)));
        SPECS.put(Kind.DEER, List.of(
                p("body", 0, 13, 0, -3, -3, -6, 6, 6, 12, 0), p("belly", 0, 16.2f, 0, -2.6f, 0, -5.5f, 5.2f, 0.6f, 11, 1), p("neck", 0, 11, -5.5f, -1.5f, -6, -1.5f, 3, 7, 3, 0),
                p("head", 0, 6, -7.5f, -2, -1.5f, -3.5f, 4, 3, 5, 0), p("nose", 0, 7.5f, -11, -1, -0.3f, 0, 2, 1.2f, 1, 2),
                p("ear_l", 1.8f, 4.6f, -7.5f, 0, -1.8f, -0.5f, 1.2f, 2.4f, 0.6f, 0), p("ear_r", -1.8f, 4.6f, -7.5f, -1.2f, -1.8f, -0.5f, 1.2f, 2.4f, 0.6f, 0),
                p("antler_l", 1.2f, 4.5f, -7.5f, 0, -4.5f, 0, 0.7f, 4.5f, 0.7f, 2), p("antler_r", -1.2f, 4.5f, -7.5f, -0.7f, -4.5f, 0, 0.7f, 4.5f, 0.7f, 2),
                p("tail", 0, 11, 6, -1, -0.5f, 0, 2, 2.6f, 1.2f, 1),
                p("leg0", 2.2f, 14, -4.5f, -1, 0, -1, 2, 10, 2, 0), p("leg1", -2.2f, 14, -4.5f, -1, 0, -1, 2, 10, 2, 0),
                p("leg2", 2.2f, 14, 4.5f, -1, 0, -1, 2, 10, 2, 0), p("leg3", -2.2f, 14, 4.5f, -1, 0, -1, 2, 10, 2, 0)));
    }

    public static ModelLayerLocation layer(Kind kind) {
        return new ModelLayerLocation(ModeMinecraft.id("wild_" + kind.id()), "main");
    }

    public static LayerDefinition definition(Kind kind) {
        MeshDefinition mesh = new MeshDefinition();
        for (Part pt : SPECS.get(kind)) {
            int v = pt.color == 0 ? 0 : pt.color == 1 ? 48 : 56;
            mesh.getRoot().addOrReplaceChild(pt.name, CubeListBuilder.create().texOffs(0, v).addBox(pt.x, pt.y, pt.z, pt.w, pt.h, pt.d), PartPose.offset(pt.ox, pt.oy, pt.oz));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private final Kind kind;

    public WildlifeRenderer(EntityRendererProvider.Context ctx, Kind kind) {
        super(ctx, new Model(ctx.bakeLayer(layer(kind)), kind), 0.2f * kind.scale);
        this.kind = kind;
    }

    @Override
    public ResourceLocation getTextureLocation(WildlifeEntity e) {
        return ModeMinecraft.id("textures/entity/wild_" + kind.id() + ".png");
    }

    @Override
    protected void scale(WildlifeEntity e, PoseStack pose, float partial) {
        pose.scale(kind.scale, kind.scale, kind.scale);
    }

    public static class Model extends EntityModel<WildlifeEntity> {
        private final ModelPart root;
        private final Kind kind;
        private final Map<String, ModelPart> parts = new HashMap<>();
        private final Map<String, Float> baseX = new HashMap<>();

        public Model(ModelPart root, Kind kind) {
            this.root = root;
            this.kind = kind;
            for (Part pt : SPECS.get(kind)) {
                parts.put(pt.name, root.getChild(pt.name));
                baseX.put(pt.name, pt.ox);
            }
        }

        @Override
        public void setupAnim(WildlifeEntity e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
            ModelPart head = parts.get("head");
            if (head != null && !kind.flier() && kind != Kind.SNAKE) {
                head.yRot = headYaw * Mth.DEG_TO_RAD * 0.6f;
                head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5f;
            }
            float flap = Mth.lerp(0f, e.flapO, e.flap);
            for (Map.Entry<String, ModelPart> en : parts.entrySet()) {
                String n = en.getKey();
                ModelPart part = en.getValue();
                if (n.startsWith("leg") && kind != Kind.EAGLE && kind != Kind.GULL) {
                    int i = n.charAt(3) - '0';
                    float k = kind == Kind.SCORPION ? 0.9f : 1.2f;
                    part.xRot = Mth.cos(limbSwing * 0.6662f + (i % 2) * Mth.PI + (i / 2) * 1.2f) * k * limbAmount;
                } else if (n.equals("wing_l") || n.equals("wing_r")) {
                    float f = e.flap == 0 ? 0 : Mth.sin(age * (kind == Kind.EAGLE ? 0.25f : 1.5f)) * (kind == Kind.EAGLE ? 0.18f : 0.9f);
                    part.zRot = (n.equals("wing_l") ? -f : f) - (kind == Kind.EAGLE ? 0.05f : 0.1f) * (n.equals("wing_l") ? 1 : -1);
                } else if (n.startsWith("seg")) {
                    int i = n.charAt(3) - '0';
                    part.x = baseX.get(n) + Mth.sin(age * 0.35f + i * 0.9f) * 1.4f * (0.4f + Math.min(1f, limbAmount * 3));
                } else if (n.startsWith("tail") && kind == Kind.SCORPION) {
                    part.xRot = Mth.sin(age * 0.08f) * 0.05f;
                }
            }
            if (kind.flier()) {
                ModelPart body = parts.get("body");
                if (body != null) {
                    body.xRot = (float) (e.getDeltaMovement().y * -0.5);
                }
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
            root.render(pose, buffer, light, overlay, color);
        }
    }
}

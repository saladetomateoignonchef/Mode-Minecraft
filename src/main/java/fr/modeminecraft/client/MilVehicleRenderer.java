package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.MilGeometry;
import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Rendu des véhicules militaires : caisse, tourelle (lacet), canon / lanceur (élévation), roues (qui tournent, celles
 * de l'essieu avant braquent), rotors et radar qui tournent, inclinaison des hélicoptères. Les pivots viennent de
 * {@link MilGeometry} (générée avec les modèles).
 */
public class MilVehicleRenderer extends EntityRenderer<MilVehicleEntity> {
    public MilVehicleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 1.6f;
    }

    public static ModelResourceLocation part(String vehicle, String part) {
        return ModelResourceLocation.standalone(ModeMinecraft.id("vehicle/" + vehicle + "_" + part));
    }

    public static final String[] PARTS = {"hull", "turret", "gun", "wheel", "rotor", "tail_rotor", "radar"};

    @Override
    public boolean shouldRender(MilVehicleEntity v, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(v.getBoundingBox().inflate(MilVehicleEntity.dims(v.type())[0] / 2 + 2));
    }

    @Override
    public void render(MilVehicleEntity v, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        var mc = Minecraft.getInstance();
        MilType t = v.type();
        MilGeometry.Geo geo = t.geo();
        if (geo == null) {
            return;
        }
        float s = geo.scale();
        ox = geo.ox();
        // (le rendu d'objet ignore une pile vide : on passe l'objet du véhicule)
        ItemStack stack = new ItemStack(fr.modeminecraft.registry.ModItems.MIL_VEHICLES.get(t).get());
        STACK = stack;
        float bodyYaw = Mth.rotLerp(partial, v.yRotO, v.getYRot());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90 - bodyYaw));
        if (t.heli()) {
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, v.tiltO, v.tilt)));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, v.rollO, v.roll)));
        } else if (Math.abs(v.speed()) > 0.02f && t.tracked()) {
            pose.translate(0, 0.015 * Mth.sin((v.tickCount + partial) * 1.7f), 0);           // vibration des chenilles
        }
        pose.scale(s, s, s);
        pose.translate(-(geo.ox() - 8) / 16f, 0, 0);
        draw(mc, pose, buffers, light, t, "hull", null, 0, (char) 0);
        // tourelle et canon
        float ty = Mth.rotLerp(partial, v.turretO, v.turret(partial));
        float gp = Mth.lerp(partial, v.pitchO, v.gunPitch(partial));
        if (t.has(MilType.Flag.STRATEGIC)) {
            gp = StrategicClient.erect(v, partial);
            ty = 0;
        }
        if (geo.turret() != null) {
            pose.pushPose();
            pivot(pose, geo.turret(), s);
            pose.mulPose(Axis.YP.rotationDegrees(ty));
            unpivot(pose, geo.turret(), s);
            draw(mc, pose, buffers, light, t, "turret", null, 0, (char) 0);
            if (geo.gun() != null) {
                pivot(pose, geo.gun(), s);
                pose.mulPose(Axis.ZP.rotationDegrees(gp));
                unpivot(pose, geo.gun(), s);
                draw(mc, pose, buffers, light, t, "gun", null, 0, (char) 0);
            }
            pose.popPose();
        }
        // roues
        if (geo.wheels().length > 0) {
            BakedModel wheel = mc.getModelManager().getModel(part(t.id, "wheel"));
            float maxX = -1e9f;
            for (float[] w : geo.wheels()) {
                maxX = Math.max(maxX, w[0]);
            }
            float spin = Mth.lerp(partial, v.wheelRotO, v.wheelRot);
            for (float[] w : geo.wheels()) {
                pose.pushPose();
                pose.translate(w[0] / s + (geo.ox() - 8) / 16f, w[1] / s, w[2] / s);
                if (w[0] >= maxX - 0.05f) {
                    pose.mulPose(Axis.YP.rotationDegrees(v.steering() * 26));
                }
                pose.mulPose(Axis.ZP.rotationDegrees(-spin * 57.3f / Math.max(0.2f, w[3])));
                mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, wheel);
                pose.popPose();
            }
        }
        // rotors, radar
        float rot = Mth.lerp(partial, v.rotorAngleO, v.rotorAngle);
        if (geo.rotor() != null) {
            draw(mc, pose, buffers, light, t, "rotor", geo.rotor(), rot, 'y');
        }
        if (geo.tail() != null) {
            draw(mc, pose, buffers, light, t, "tail_rotor", geo.tail(), rot * 1.6f, 'z');
        }
        if (geo.radar() != null) {
            draw(mc, pose, buffers, light, t, "radar", geo.radar(), Mth.lerp(partial, v.radarAngleO, v.radarAngle), 'y');
        }
        pose.popPose();
        super.render(v, yaw, partial, pose, buffers, light);
    }

    /** Pivots en mètres -> repère courant (après mise à l'échelle et décalage de l'origine du modèle). */
    private static float ox;
    private static ItemStack STACK = ItemStack.EMPTY;

    private static void pivot(PoseStack pose, Vec3 p, float s) {
        pose.translate(p.x / s + (ox - 8) / 16f, p.y / s, p.z / s);
    }

    private static void unpivot(PoseStack pose, Vec3 p, float s) {
        pose.translate(-(p.x / s + (ox - 8) / 16f), -p.y / s, -p.z / s);
    }

    /** Pièce aux coordonnées absolues (repère du modèle), éventuellement tournée autour d'un pivot. */
    private static void draw(Minecraft mc, PoseStack pose, MultiBufferSource buffers, int light, MilType t, String part, Vec3 pivot, float angle, char axis) {
        BakedModel model = mc.getModelManager().getModel(part(t.id, part));
        if (model == mc.getModelManager().getMissingModel()) {
            return;
        }
        float s = t.geo().scale();
        pose.pushPose();
        if (pivot != null) {
            pivot(pose, pivot, s);
            pose.mulPose(axis == 'y' ? Axis.YP.rotationDegrees(angle) : Axis.ZP.rotationDegrees(angle));
            unpivot(pose, pivot, s);
        }
        pose.translate(0, 0.5, 0);
        mc.getItemRenderer().render(STACK, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, model);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MilVehicleEntity v) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}

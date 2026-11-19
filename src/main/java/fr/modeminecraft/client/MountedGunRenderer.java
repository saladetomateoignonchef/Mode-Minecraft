package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.MountedGunEntity;
import fr.modeminecraft.entity.TurretGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Tourelle posée, en pièces : trépied (les pieds s'écartent au déploiement), support qui tourne, arme qui suit le
 * regard du tireur (recul + vibrations au tir), capot qui s'ouvre pendant le rechargement, caisse et bande de
 * cartouches qui défile vers l'arme (elle disparaît quand la caisse est vide), canon rougi en surchauffe.
 * Les deux bras cubiques du tireur tiennent les poignées arrière (1re et 3e personne).
 */
public class MountedGunRenderer extends EntityRenderer<MountedGunEntity> {
    public MountedGunRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static ModelResourceLocation part(String turret, String part) {
        return ModelResourceLocation.standalone(ModeMinecraft.id("turret/" + turret + "_" + part));
    }

    private static void draw(MountedGunEntity gun, String part, PoseStack pose, MultiBufferSource buffers, int light) {
        var mc = Minecraft.getInstance();
        mc.getItemRenderer().render(new ItemStack(gun.kind().item()), ItemDisplayContext.NONE, false, pose, buffers, light,
                OverlayTexture.NO_OVERLAY, mc.getModelManager().getModel(part(gun.kind().id, part)));
    }

    /** Recul de l'arme (pixels) juste après un coup. */
    private static float recoil(MountedGunEntity gun, float partialTick) {
        float since = gun.level().getGameTime() + partialTick - gun.lastShot();
        return since < 0 || since > 3 ? 0 : (1 - since / 3) * (gun.kind() == MountedGunEntity.Kind.MK19 ? 1.0f : 0.6f);
    }

    @Override
    public void render(MountedGunEntity gun, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        TurretGeometry.Geo geo = gun.kind().geo();
        if (geo == null) {
            return;
        }
        String id = gun.kind().id;
        float yaw = gun.gunYaw(partialTick), pitch = gun.gunPitch(partialTick);
        float time = gun.level().getGameTime() + partialTick;
        boolean firing = time - gun.lastShot() < 1200f / gun.kind().rpm + 2;

        // Trépied / plate-forme (fixe) : déploiement, les pieds s'écartent et se verrouillent
        pose.pushPose();
        pose.translate(0, geo.pivot(), 0);
        pose.mulPose(Axis.YP.rotationDegrees(-(gun.getYRot() + 90)));
        float d = Mth.clamp((gun.deployTicks() + (gun.deployTicks() < MountedGunEntity.DEPLOY_TICKS ? partialTick : 0))
                / MountedGunEntity.DEPLOY_TICKS, 0, 1);
        float spread = 0.2f + 0.8f * (1 - (1 - d) * (1 - d));
        Vector3f head = geo.baseHead() == null ? new Vector3f(8, 8, 8) : geo.baseHead();
        pose.translate((head.x - 8) / 16, (head.y - 8) / 16, (head.z - 8) / 16);
        pose.scale(spread, 1, spread);
        pose.translate(-(head.x - 8) / 16, -(head.y - 8) / 16, -(head.z - 8) / 16);
        draw(gun, "base", pose, buffers, light);
        pose.popPose();

        // Support (direction seulement) puis arme (direction + hauteur)
        pose.pushPose();
        pose.translate(0, geo.pivot(), 0);
        pose.mulPose(Axis.YP.rotationDegrees(-(yaw + 90)));
        draw(gun, "mount", pose, buffers, light);
        pose.mulPose(Axis.ZP.rotationDegrees(-pitch));
        float kick = recoil(gun, partialTick);
        float shake = firing ? (gun.getRandom().nextFloat() - 0.5f) * 0.25f : 0;
        pose.translate(-kick / 16, shake / 16, 0);
        draw(gun, "body", pose, buffers, light);
        draw(gun, gun.heat() > 0.75f || gun.isOverheated() ? "barrel_hot" : "barrel", pose, buffers, light);
        if (geo.parts().contains("box")) {
            draw(gun, "box", pose, buffers, light);
        }
        // Capot d'alimentation : il s'ouvre puis se referme pendant le rechargement
        if (geo.coverHinge() != null) {
            float r = gun.reloadProgress(partialTick);
            float open = r < 0 ? 0 : Mth.sin(r * (float) Math.PI);
            Vector3f h = geo.coverHinge();
            pose.pushPose();
            pose.translate((h.x - 8) / 16, (h.y - 8) / 16, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(-75 * Math.min(1, open * 1.6f)));
            pose.translate(-(h.x - 8) / 16, -(h.y - 8) / 16, 0);
            draw(gun, "cover", pose, buffers, light);
            pose.popPose();
        }
        // Flakvierling : un chargeur latéral par tranche de 20 obus
        for (int i = 0; i < 4; i++) {
            if (geo.parts().contains("mag" + i) && gun.ammo() > i * 20) {
                draw(gun, "mag" + i, pose, buffers, light);
            }
        }
        // Bande de cartouches : de la caisse jusqu'à l'arme ; elle défile pendant le tir
        TurretGeometry.Belt belt = geo.belt();
        if (belt != null && gun.ammo() > 0) {
            int count = Math.min(belt.count(), gun.ammo());
            float phase = firing ? (time * gun.kind().rpm / 1200f) % 1f : 0;
            for (int i = 0; i < count; i++) {
                float t = 1 - (i + phase) / belt.count();
                if (t < 0) {
                    continue;
                }
                Vector3f p = bezier(belt, t);
                pose.pushPose();
                float jiggle = firing ? (gun.getRandom().nextFloat() - 0.5f) * 0.12f : 0;
                pose.translate(p.x / 16, (p.y + jiggle) / 16, p.z / 16);
                draw(gun, "round", pose, buffers, light);
                pose.popPose();
            }
        }
        pose.popPose();

        if (gun.getFirstPassenger() instanceof AbstractClientPlayer rider) {
            renderArms(gun, rider, geo, yaw, pitch, kick + Math.abs(shake), partialTick, pose, buffers, light);
        }
        super.render(gun, entityYaw, partialTick, pose, buffers, light);
    }

    private static Vector3f bezier(TurretGeometry.Belt b, float t) {
        float u = 1 - t;
        return new Vector3f(b.from()).mul(u * u).add(new Vector3f(b.ctrl()).mul(2 * u * t)).add(new Vector3f(b.to()).mul(t * t));
    }

    /** Les bras du tireur, des épaules jusqu'aux poignées arrière (ils suivent la rotation et le recul de l'arme). */
    private static void renderArms(MountedGunEntity gun, AbstractClientPlayer rider, TurretGeometry.Geo geo, float yaw, float pitch,
                                   float kick, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (geo.grips().size() < 2) {
            return;
        }
        var mc = Minecraft.getInstance();
        if (!(mc.getEntityRenderDispatcher().getRenderer(rider) instanceof PlayerRenderer renderer)) {
            return;
        }
        Vec3 base = gun.getPosition(partialTick);
        boolean firstPerson = rider == mc.player && mc.options.getCameraType().isFirstPerson();
        Vec3 eye = (firstPerson ? mc.gameRenderer.getMainCamera().getPosition() : rider.getEyePosition(partialTick)).subtract(base);
        Vec3 right = Vec3.directionFromRotation(0, yaw + 90);
        Vec3 fwd = Vec3.directionFromRotation(0, yaw);
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        var skin = rider.getSkin().texture();
        for (int i = 0; i < 2; i++) {
            boolean isRight = i == 1;                           // poignée 0 = gauche (-Z), 1 = droite
            Vector3f g = new Vector3f(geo.grips().get(i)).add(-kick, 0, 0);
            Vec3 hand = TurretGeometry.offset(geo.pivot(), yaw, pitch, g);
            Vec3 shoulder = eye.add(right.scale(isRight ? 0.34 : -0.34)).add(0, -0.32, 0).add(fwd.scale(-0.05));
            GunPoseHelper.drawArm(pose, buffers, light, skin, isRight ? model.rightArm : model.leftArm,
                    rider.isModelPartShown(isRight ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE)
                            ? (isRight ? model.rightSleeve : model.leftSleeve) : null,
                    shoulder.toVector3f(), hand.toVector3f(), isRight);
        }
    }

    @Override
    public boolean shouldRender(MountedGunEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(2.0));
    }

    @Override
    public ResourceLocation getTextureLocation(MountedGunEntity entity) {
        return ModeMinecraft.id("textures/item/gun_palette_default.png");
    }
}

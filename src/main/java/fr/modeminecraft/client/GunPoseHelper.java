package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.client.model.GunMounts;
import fr.modeminecraft.item.gun.AttachmentSlot;
import fr.modeminecraft.item.gun.GunAnimations;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Position de l'arme en vue à la 1re personne :
 * - hanche : comme Minecraft ; en visant, l'arme vient au centre et on regarde à travers le viseur 3D
 *   (« vraie visée ») ;
 * - recul de l'arme à chaque tir, inclinaison pendant le rechargement, pivot pour vérifier la chambre ;
 * - les deux bras du joueur tiennent l'arme : main droite sur la poignée, main gauche sous le garde-main
 *   (ou sur le chargeur pendant le rechargement).
 */
public final class GunPoseHelper {
    /** Transformation "firstperson_righthand" des modèles d'armes (tools/generate_gun_models.py). */
    private static final float DISPLAY_ROT_Y = 100f, DISPLAY_SCALE = 0.9f;
    private static final Vector3f DISPLAY_T = new Vector3f(1 / 16f, 4 / 16f, -2 / 16f);
    /** Distance du viseur à l'œil en vraie visée (blocs) : bras tendus pour un pistolet. */
    private static float adsDistance(ItemStack stack, boolean optic) {
        if (stack.getItem() instanceof GunItem g && g.stats().pistol()) {
            return optic ? 0.34f : 0.42f;
        }
        return optic ? 0.2f : 0.18f; // l'œil tout près du viseur : grande fenêtre, comme dans un FPS
    }
    static final Vector3f RIGHT_SHOULDER = new Vector3f(0.36f, -0.62f, 0.12f);
    static final Vector3f LEFT_SHOULDER = new Vector3f(-0.34f, -0.62f, 0.05f);

    private static float aim, aimPrev;

    /** Progression de la mise en joue (0 = hanche, 1 = vraie visée). */
    public static float aimProgress(float partialTick) {
        return Mth.lerp(partialTick, aimPrev, aim);
    }

    /** Appelé à chaque tick : la mise en joue avance selon la vitesse de visée de l'arme. */
    public static void tick(net.minecraft.world.entity.player.Player player) {
        aimPrev = aim;
        var gun = GunClientEvents.aiming(player);
        float target = gun != null ? 1 : 0;
        float aimSpeed = gun != null ? gun.aimSpeed() : 1.5f;
        if (gun != null && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(fr.modeminecraft.registry.ModItems.MICH_2000.get())) {
            aimSpeed = Math.max(1.0f, aimSpeed); // casque MICH sans visière : pas de malus de visée avec les lunettes
        }
        if (gun != null) {
            aimSpeed *= fr.modeminecraft.item.VestItem.adsMul(player); // porte-plaques JPC : +15 %
        }
        float speed = 0.22f * aimSpeed;
        aim = target > aim ? Math.min(target, aim + speed) : Math.max(target, aim - speed);
    }

    public static void apply(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                             float partialTick, float equip, float swing) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        boolean mainRight = arm == HumanoidArm.RIGHT && player.getMainHandItem() == stack;
        Matrix4f base = new Matrix4f(pose.last().pose());
        String gun = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

        // --- Hanche (comme Minecraft) -> vraie visée, en douceur ---
        float a = mainRight ? smooth(aimProgress(partialTick)) : 0;
        Vector3f hip = new Vector3f(side * 0.56f, -0.52f + equip * -0.6f, -0.72f);
        Vector3f ads = mainRight ? adsPosition(gun, stack) : hip;
        Vector3f p = hip.lerp(ads, a, new Vector3f());
        pose.translate(p.x, p.y, p.z);
        // Canon dans l'axe : en visant exactement ; à la hanche, il pointe vers le réticule (à ~10 blocs)
        pose.mulPose(Axis.YP.rotationDegrees(mainRight ? -10f * a - 6.4f * (1 - a) : 0));
        pose.mulPose(Axis.XP.rotationDegrees(mainRight ? 3.2f * (1 - a) : 0));

        float now = player.level().getGameTime() + partialTick;
        float calm = 1 - 0.7f * a; // en visant, les mouvements sont plus petits

        // --- Recul visuel au tir ---
        float sinceShot = GunAnimations.ticksSinceShot(player, partialTick);
        if (sinceShot < 3) {
            float kick = 1 - sinceShot / 3;
            pose.translate(0, 0.01f * kick, 0.07f * kick * (1 - 0.5f * a));
            pose.mulPose(Axis.XP.rotationDegrees(4 * kick * calm));
        }

        // --- Rechargement : l'arme s'incline et descend un peu ---
        Long start = stack.get(ModDataComponents.RELOAD_START.get());
        Long end = stack.get(ModDataComponents.RELOAD_END.get());
        if (start != null && end != null && now >= start && now <= end) {
            float tilt = Math.min(1, Math.min((now - start) / 5f, (end - now) / 5f));
            tilt = tilt * tilt * (3 - 2 * tilt);
            pose.translate(-side * 0.04f * tilt, -0.03f * tilt, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 18 * tilt));
            pose.mulPose(Axis.XP.rotationDegrees(-6 * tilt));
        }

        // --- Inspection (touche H) : on tourne l'arme pour l'admirer, d'un côté puis de l'autre ---
        Long check = stack.get(ModDataComponents.CHECK_AT.get());
        int len = GunItem.INSPECT_TICKS;
        if (check != null && now >= check && now <= check + len) {
            float t = (now - check) / len;
            float in = smooth(Math.min(1, t / 0.18f)), out = smooth(Math.min(1, (1 - t) / 0.18f));
            float w = Math.min(in, out);
            float swap = smooth(Mth.clamp((t - 0.45f) / 0.2f, 0, 1)); // passe d'un côté à l'autre
            float yaw = Mth.lerp(swap, 55, -35), roll = Mth.lerp(swap, 30, -20);
            pose.translate(-side * 0.12f * w, 0.06f * w, 0.12f * w);
            pose.mulPose(Axis.YP.rotationDegrees(side * yaw * w));
            pose.mulPose(Axis.ZP.rotationDegrees(side * roll * w));
            pose.mulPose(Axis.XP.rotationDegrees(-8 * w));
        }

        if (mainRight && !player.isInvisible()) {
            renderArms(pose, base, player, stack, gun, now, partialTick);
        }
        if (mainRight) {
            Vector3f lens = LaserBeams.lensPoint(gun, stack);
            if (lens != null) {
                LaserBeams.drawFirstPerson(player, stack, new Matrix4f(pose.last().pose()).mul(displayMatrix()).transformPosition(lens));
            }
        }
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    /** Matrice du modèle de l'arme (pixels -> écran), comme l'applique Minecraft après notre transformation. */
    private static Matrix4f displayMatrix() {
        return new Matrix4f().translate(DISPLAY_T).rotate(Axis.YP.rotationDegrees(DISPLAY_ROT_Y))
                .scale(DISPLAY_SCALE).translate(-0.5f, -0.5f, -0.5f);
    }

    /**
     * Où placer l'arme pour que le viseur soit exactement au centre de l'écran, à ADS_DISTANCE de l'œil.
     */
    private static Vector3f adsPosition(String gun, ItemStack stack) {
        Vector3f sight = GunMounts.get(gun, AttachmentSlot.SIGHT);
        if (sight == null) {
            sight = new Vector3f(8 / 16f, 10 / 16f, 0.5f);
        }
        var mounted = GunItem.getAttachments(stack).get(AttachmentSlot.SIGHT);
        boolean optic = mounted.isPresent();
        Float h = optic ? GunMounts.opticHeight(BuiltInRegistries.ITEM.getKey(mounted.get()).getPath()) : null;
        // L'œil pile au centre de la vitre du viseur (ou juste au-dessus du rail pour les organes de visée)
        boolean launcher = stack.getItem() instanceof GunItem g && g.stats().category() == fr.modeminecraft.item.gun.GunStats.Category.LAUNCHER;
        // Lance-roquettes : l'œil au-dessus du tube, contre le viseur (le tube ne cache pas la vue)
        float above = h != null ? h : optic ? 1.3f : launcher ? 3.8f : 0.55f;
        Vector3f eyePoint = new Vector3f(sight).add(0, above / 16f, 0);
        // Position du point de visée après la transformation d'affichage, puis la rotation de -10°
        Vector3f q = displayMatrix().transformPosition(eyePoint, new Vector3f());
        new Matrix4f().rotate(Axis.YP.rotationDegrees(-10f)).transformPosition(q);
        return new Vector3f(0, 0, -adsDistance(stack, optic)).sub(q);
    }

    // ------------------------------------------------------------------ bras
    private static void renderArms(PoseStack pose, Matrix4f base, LocalPlayer player, ItemStack stack, String gun,
                                   float now, float partialTick) {
        // Transformation de l'arme relative à la caméra (sans le balancement de la vue, déjà dans "base")
        Matrix4f rel = new Matrix4f(base).invert().mul(pose.last().pose()).mul(displayMatrix());
        Vector3f rightHand = handPoint(gun, true, stack, now);
        Vector3f leftHand = handPoint(gun, false, stack, now);
        if (rightHand == null) {
            return;
        }
        rel.transformPosition(rightHand);
        if (leftHand != null) {
            rel.transformPosition(leftHand);
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        MultiBufferSource buffers = mc.renderBuffers().bufferSource();
        int light = mc.getEntityRenderDispatcher().getPackedLightCoords(player, partialTick);
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        var skin = player.getSkin().texture();
        pose.pushPose();
        pose.last().pose().set(base);
        drawArm(pose, buffers, light, skin, model.rightArm, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE) ? model.rightSleeve : null,
                RIGHT_SHOULDER, rightHand, true);
        if (leftHand != null) {
            drawArm(pose, buffers, light, skin, model.leftArm, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE) ? model.leftSleeve : null,
                    LEFT_SHOULDER, leftHand, false);
        }
        pose.popPose();
    }

    /** Point tenu par la main (pixels du modèle), ou null. */
    private static Vector3f handPoint(String gun, boolean right, ItemStack stack, float now) {
        Vector3f grip = GunMounts.get(gun, AttachmentSlot.PISTOL_GRIP);
        if (right) {
            return grip == null ? null : new Vector3f(grip).add(-0.7f / 16f, -1.6f / 16f, 0);
        }
        // Colt SAA (fanning) : la main gauche frappe le chien à chaque coup
        var player = Minecraft.getInstance().player;
        if (stack.getItem() instanceof GunItem g && g.stats().special() == fr.modeminecraft.item.gun.GunStats.Special.FANNING
                && player != null && fr.modeminecraft.item.gun.TriggerHandler.isRightHeld(player) && grip != null) {
            float t = Math.min(1, GunAnimations.ticksSinceShot(player, 0) / 3f);
            return new Vector3f(grip).add((-1.2f - 1.5f * (1 - t)) / 16f, (3.2f + 0.6f * t) / 16f, 0);
        }
        // Rechargement : la main gauche suit le chargeur (et va en chercher un autre en bas)
        float off = GunAnimations.reloadMagOffset(stack, now);
        Vector3f mag = GunMounts.get(gun, AttachmentSlot.MAGAZINE);
        if (mag != null && (Float.isNaN(off) || off > 0)) {
            float down = Float.isNaN(off) ? 16 : off + 2.5f;
            return new Vector3f(mag).add(0, -down / 16f, 0);
        }
        Vector3f fore = GunMounts.get(gun, AttachmentSlot.GRIP);
        if (fore != null) {
            boolean vertical = GunItem.getAttachments(stack).get(AttachmentSlot.GRIP).isPresent();
            return new Vector3f(fore).add(0, (vertical ? -2.6f : -0.5f) / 16f, 0);
        }
        // Pistolet : la main gauche enveloppe la droite
        return grip == null ? null : new Vector3f(grip).add(0.2f / 16f, -2.2f / 16f, 0);
    }

    /** Un bras (et sa manche) allant de l'épaule à la main, la main sur le point donné. */
    static void drawArm(PoseStack pose, MultiBufferSource buffers, int light, net.minecraft.resources.ResourceLocation skin,
                                ModelPart arm, ModelPart sleeve, Vector3f shoulder, Vector3f hand, boolean right) {
        Vector3f dir = new Vector3f(hand).sub(shoulder).normalize();
        pose.pushPose();
        pose.translate(hand.x, hand.y, hand.z);
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), dir));
        // Dans le modèle, le bras va de y = -2 (épaule) à y = 10 (main) : on met la main au point voulu
        pose.translate((right ? 1 : -1) / 16f, -9.5f / 16f, 0);
        for (ModelPart part : new ModelPart[]{arm, sleeve}) {
            if (part == null) {
                continue;
            }
            var saved = part.storePose();
            part.x = part.y = part.z = 0;
            part.xRot = part.yRot = part.zRot = 0;
            part.render(pose, buffers.getBuffer(part == arm ? RenderType.entitySolid(skin) : RenderType.entityTranslucent(skin)),
                    light, OverlayTexture.NO_OVERLAY);
            part.loadPose(saved);
        }
        pose.popPose();
    }

    private GunPoseHelper() {}
}

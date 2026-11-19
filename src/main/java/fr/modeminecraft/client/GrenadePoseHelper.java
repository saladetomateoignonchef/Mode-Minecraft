package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Grenades à la 1re personne (cahier des charges, 3.1) :
 * - dégoupillage (clic droit maintenu) : la grenade monte devant le torse, le bras gauche vient sur l'anneau puis le tire
 *   vers la gauche ;
 * - tant qu'on la garde, elle tremble un peu de plus en plus (le minuteur tourne) ;
 * - lancer : le bras droit arme vers l'arrière puis part en arc de cercle (animation du coup).
 */
public final class GrenadePoseHelper {
    public static void apply(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick,
                             float equip, float swing) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        Matrix4f base = new Matrix4f(pose.last().pose());
        pose.translate(side * 0.56f, -0.52f + equip * -0.6f, -0.72f);
        boolean cooking = player.isUsingItem() && player.getUseItem() == stack;
        if (cooking) {
            float held = player.getTicksUsingItem() + partialTick;
            float up = Math.min(1, held / 4f);
            pose.translate(-side * 0.4f * up, 0.2f * up, 0.14f * up);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 12 * up));
            if (held > 8) { // le minuteur tourne : la main tremble un peu
                float shake = Math.min(1, (held - 8) / 50f) * 0.006f;
                pose.translate(Mth.sin(held * 2.3f) * shake, Mth.cos(held * 3.1f) * shake, 0);
            }
            // Bras gauche : il attrape l'anneau (0 à 4 ticks) puis le tire vers la gauche (4 à 8 ticks)
            float pull = Mth.clamp((held - 4) / 4f, 0, 1);
            float reach = Math.min(1, held / 4f);
            Matrix4f rel = new Matrix4f(base).invert().mul(pose.last().pose());
            Vector3f ring = rel.transformPosition(new Vector3f(0.02f * side, 0.12f, 0.02f));
            Vector3f rest = new Vector3f(-side * 0.55f, -0.85f, -0.4f);
            Vector3f hand = new Vector3f(rest).lerp(ring, reach).add(-side * 0.22f * pull, -0.04f * pull, 0.05f * pull);
            drawLeftArm(pose, base, player, partialTick, hand, side);
            return;
        }
        if (swing > 0) {
            // Lancer : le bras arme vers l'arrière puis part vers l'avant en arc de cercle
            float back = Mth.sin(Math.min(1, swing * 3) * (float) Math.PI / 2) * (1 - swing);
            float fwd = Mth.sin(swing * (float) Math.PI);
            pose.translate(0, 0.25f * back + 0.1f * fwd, 0.25f * back - 0.45f * fwd);
            pose.mulPose(Axis.XP.rotationDegrees(70 * back - 60 * fwd));
        }
    }

    private static void drawLeftArm(PoseStack pose, Matrix4f base, LocalPlayer player, float partialTick, Vector3f hand, int side) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        int light = mc.getEntityRenderDispatcher().getPackedLightCoords(player, partialTick);
        boolean right = side < 0; // grenade tenue à gauche : c'est le bras droit qui dégoupille
        Vector3f shoulder = new Vector3f(GunPoseHelper.LEFT_SHOULDER);
        shoulder.x *= side;
        pose.pushPose();
        pose.last().pose().set(base);
        GunPoseHelper.drawArm(pose, mc.renderBuffers().bufferSource(), light, player.getSkin().texture(),
                right ? model.rightArm : model.leftArm,
                player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE) ? (right ? model.rightSleeve : model.leftSleeve) : null,
                shoulder, hand, right);
        pose.popPose();
    }

    private GrenadePoseHelper() {}
}

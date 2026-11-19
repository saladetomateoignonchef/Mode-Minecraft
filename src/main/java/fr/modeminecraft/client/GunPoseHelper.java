package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.item.gun.GunAnimations;
import fr.modeminecraft.registry.ModDataComponents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * Position de l'arme en vue à la 1re personne : comme Minecraft, plus nos mouvements :
 * - léger recul de l'arme à chaque tir ;
 * - l'arme s'incline pendant le rechargement ;
 * - l'arme pivote pendant la vérification de la chambre.
 */
public final class GunPoseHelper {

    public static void apply(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                             float partialTick, float equip, float swing) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        // --- Comportement vanilla (balancement du coup + position de la main) ---
        float f5 = -0.4F * Mth.sin(Mth.sqrt(swing) * (float) Math.PI);
        float f6 = 0.2F * Mth.sin(Mth.sqrt(swing) * (float) (Math.PI * 2));
        float f10 = -0.2F * Mth.sin(swing * (float) Math.PI);
        pose.translate(side * f5, f6, f10);
        pose.translate(side * 0.56F, -0.52F + equip * -0.6F, -0.72F);
        float a = Mth.sin(swing * swing * (float) Math.PI);
        pose.mulPose(Axis.YP.rotationDegrees(side * (45.0F + a * -20.0F)));
        float b = Mth.sin(Mth.sqrt(swing) * (float) Math.PI);
        pose.mulPose(Axis.ZP.rotationDegrees(side * b * -20.0F));
        pose.mulPose(Axis.XP.rotationDegrees(b * -80.0F));
        pose.mulPose(Axis.YP.rotationDegrees(side * -45.0F));

        float now = player.level().getGameTime() + partialTick;

        // --- Recul visuel au tir ---
        float sinceShot = GunAnimations.ticksSinceShot(player, partialTick);
        if (sinceShot < 3) {
            float kick = 1 - sinceShot / 3;
            pose.translate(0, 0.01f * kick, 0.07f * kick);
            pose.mulPose(Axis.XP.rotationDegrees(4 * kick));
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

        // --- Vérification de la chambre : l'arme pivote vers soi ---
        Long check = stack.get(ModDataComponents.CHECK_AT.get());
        if (check != null && now >= check && now <= check + 12) {
            float t = Math.min(1, Math.min((now - check) / 3f, (check + 12 - now) / 3f));
            pose.mulPose(Axis.ZP.rotationDegrees(side * 35 * t));
            pose.mulPose(Axis.YP.rotationDegrees(side * -15 * t));
        }
    }

    private GunPoseHelper() {}
}

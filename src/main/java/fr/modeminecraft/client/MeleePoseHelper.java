package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.item.melee.MeleeEvents;
import fr.modeminecraft.item.melee.MeleeWeaponItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * Armes blanches en vue à la 1re personne :
 * - 3 coups différents selon le combo : balayage droite → gauche, revers gauche → droite, coup vertical (ou estoc
 *   pour les armes d'estoc) ;
 * - parade : l'arme est tenue en travers devant le torse ;
 * - karambit : il tourne autour du poignet quand on le sort.
 */
public final class MeleePoseHelper {
    public static void apply(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick,
                             float equip, float swing) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        MeleeWeaponItem w = (MeleeWeaponItem) stack.getItem();
        pose.translate(side * 0.56f, -0.52f + equip * -0.6f, -0.72f);
        if (w == fr.modeminecraft.registry.ModItems.KARAMBIT.get() && equip > 0.01f) {
            pose.mulPose(Axis.ZP.rotationDegrees(side * 360 * equip)); // rotation autour du poignet
        }
        if (player.isUsingItem() && player.getUseItem() == stack) {
            // Parade : arme en travers devant le torse
            float t = Math.min(1, player.getTicksUsingItem() / 3f);
            pose.translate(-side * 0.38f * t, 0.14f * t, 0.05f * t);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 75 * t));
            pose.mulPose(Axis.YP.rotationDegrees(side * -25 * t));
            return;
        }
        if (swing <= 0) {
            return;
        }
        float s = Mth.sin(swing * (float) Math.PI);         // 0 -> 1 -> 0
        float t = swing;                                   // 0 -> 1
        boolean thrust = w.stats().ability() == MeleeWeaponItem.Ability.THRUST_PIERCE
                || w.stats().ability() == MeleeWeaponItem.Ability.CHARGE || w.stats().ability() == MeleeWeaponItem.Ability.DISMOUNT;
        switch (MeleeEvents.comboIndex(player)) {
            case 0 -> { // balayage horizontal, de droite à gauche
                pose.translate(-side * 0.55f * t, 0.12f * s, -0.2f * s);
                pose.mulPose(Axis.YP.rotationDegrees(side * (55 - 130 * t)));
                pose.mulPose(Axis.ZP.rotationDegrees(side * -55 * s));
            }
            case 1 -> { // revers, de gauche à droite
                pose.translate(-side * 0.55f * (1 - t), 0.12f * s, -0.2f * s);
                pose.mulPose(Axis.YP.rotationDegrees(side * (-75 + 130 * t)));
                pose.mulPose(Axis.ZP.rotationDegrees(side * 55 * s));
            }
            default -> {
                if (thrust) { // estoc : l'arme part droit devant
                    pose.translate(-side * 0.25f * s, 0.06f * s, -0.7f * s);
                    pose.mulPose(Axis.XP.rotationDegrees(-15 * s));
                } else { // coup vertical, de haut en bas
                    pose.translate(-side * 0.15f * s, 0.35f * s - 0.35f * t * t, -0.25f * s);
                    pose.mulPose(Axis.XP.rotationDegrees(-110 * s + 40 * t));
                }
            }
        }
    }

    private MeleePoseHelper() {}
}

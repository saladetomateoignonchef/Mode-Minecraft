package fr.modeminecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pose à la 3e personne d'un joueur qui tient la Pourfendeuse de Dragons :
 * - dégainage de 0,6 s : le bras droit passe par-dessus l'épaule droite pour saisir la poignée,
 *   puis ramène la lame à deux mains devant le torse ;
 * - ensuite : lame tenue à deux mains, pointe vers l'avant.
 * (pose ajoutée à l'énumération vanilla via META-INF/enumextensions.json)
 */
public final class GreatswordPose {
    public static final int DRAW_TICKS = 12;
    @SuppressWarnings("unused")
    public static final EnumProxy<HumanoidModel.ArmPose> ENUM_PARAMS = new EnumProxy<>(HumanoidModel.ArmPose.class,
            true, (IArmPoseTransformer) GreatswordPose::apply);

    /** entité -> {début du dégainage, dernier tick où l'épée était en main} */
    private static final Map<Integer, int[]> DRAW = new ConcurrentHashMap<>();

    public static HumanoidModel.ArmPose pose() {
        return ENUM_PARAMS.getValue();
    }

    /** Progression du dégainage (0 -> 1) ; recommence si l'épée vient d'être sélectionnée. */
    public static float progress(LivingEntity e) {
        int now = e.tickCount;
        int[] d = DRAW.computeIfAbsent(e.getId(), k -> new int[]{now, now});
        if (now - d[1] > 2 || now < d[1]) {
            d[0] = now;                                       // l'épée vient d'être sortie
        }
        d[1] = now;
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        return Mth.clamp((now - d[0] + partial) / DRAW_TICKS, 0, 1);
    }

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float t = progress(entity);
        float pitch = model.head.xRot;
        if (t < 0.5f) {
            float k = smooth(t / 0.5f);                       // la main droite monte par-dessus l'épaule
            model.rightArm.xRot = Mth.lerp(k, 0, -2.9f);
            model.rightArm.zRot = Mth.lerp(k, 0, -0.35f);
            model.rightArm.yRot = 0;
            model.leftArm.xRot = 0;
            model.leftArm.yRot = 0;
        } else {
            float k = smooth((t - 0.5f) / 0.5f);              // la lame sort et vient devant, la main gauche la saisit
            model.rightArm.xRot = Mth.lerp(k, -2.9f, -0.95f + pitch * 0.4f);
            model.rightArm.zRot = Mth.lerp(k, -0.35f, 0);
            model.rightArm.yRot = Mth.lerp(k, 0, -0.15f);
            model.leftArm.xRot = Mth.lerp(k, 0, -0.95f + pitch * 0.4f);
            model.leftArm.yRot = Mth.lerp(k, 0, 0.6f);
        }
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private GreatswordPose() {}
}

package fr.modeminecraft.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/** Pilotage d'un drone : les deux bras tiennent la radiocommande devant le torse (3e personne). */
public final class ControllerPose {
    @SuppressWarnings("unused")
    public static final EnumProxy<HumanoidModel.ArmPose> ENUM_PARAMS = new EnumProxy<>(HumanoidModel.ArmPose.class,
            true, (IArmPoseTransformer) ControllerPose::apply);

    public static HumanoidModel.ArmPose pose() {
        return ENUM_PARAMS.getValue();
    }

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        model.rightArm.xRot = -0.85f;
        model.rightArm.yRot = -0.35f;
        model.rightArm.zRot = 0;
        model.leftArm.xRot = -0.85f;
        model.leftArm.yRot = 0.35f;
        model.leftArm.zRot = 0;
    }

    private ControllerPose() {}
}

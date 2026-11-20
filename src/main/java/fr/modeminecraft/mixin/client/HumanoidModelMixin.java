package fr.modeminecraft.mixin.client;

import fr.modeminecraft.client.HelmetClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Casques : les bras montent pour poser le casque / baisser la visière (vus de l'extérieur). */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void modeminecraft$helmetArms(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                          float headPitch, CallbackInfo ci) {
        HelmetClient.poseArms(entity, (HumanoidModel<?>) (Object) this);
        fr.modeminecraft.client.VestClient.poseArms(entity, (HumanoidModel<?>) (Object) this);
        fr.modeminecraft.client.GearClient.poseArms(entity, (HumanoidModel<?>) (Object) this);
        fr.modeminecraft.client.FlashlightClient.poseArms(entity, (HumanoidModel<?>) (Object) this);
    }
}

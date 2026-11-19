package fr.modeminecraft.mixin.client;

import fr.modeminecraft.client.AimClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Se pencher : la caméra (vue à la 1re personne) glisse sur le côté. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void move(float zoom, float dy, float dx);

    @Inject(method = "setup", at = @At("TAIL"))
    private void modeminecraft$lean(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick,
                                    CallbackInfo ci) {
        if (!detached && entity == Minecraft.getInstance().player) {
            float lean = AimClient.lean(partialTick);
            if (lean != 0) {
                // dx positif = vers la gauche de la caméra
                move(0, -Math.abs(lean) * 0.08f, -lean * (float) fr.modeminecraft.item.gun.LeanHandler.MAX_OFFSET);
            }
        }
    }
}

package fr.modeminecraft.mixin.client;

import fr.modeminecraft.client.SightEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vision thermique / lunette en rubis : certaines créatures doivent apparaître en surbrillance. */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void modeminecraft$glow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (SightEffects.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }
}

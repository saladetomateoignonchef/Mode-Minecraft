package fr.modeminecraft.mixin.client;

import fr.modeminecraft.client.SightEffects;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Couleur de la surbrillance : blanc en thermique, rouge pour la cible de la lunette en rubis. */
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void modeminecraft$color(CallbackInfoReturnable<Integer> cir) {
        Integer color = SightEffects.glowColor((Entity) (Object) this);
        if (color != null) {
            cir.setReturnValue(color);
        }
    }
}

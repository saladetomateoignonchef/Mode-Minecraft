package fr.modeminecraft.mixin;

import fr.modeminecraft.item.GearEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Pantalons et rangers : moins de faim (la dépense d'énergie est multipliée avant d'être comptée). */
@Mixin(Player.class)
public abstract class PlayerExhaustionMixin {
    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float modeminecraft$gearFood(float exhaustion) {
        return exhaustion * GearEffects.foodMul((Player) (Object) this);
    }
}

package fr.modeminecraft.mixin;

import fr.modeminecraft.item.GearEffects;
import fr.modeminecraft.item.GearItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bruits de pas : toujours ceux de Minecraft selon le bloc (aucun son de semelle ajouté) ; seules les rangers
 * furtives atténuent ces pas de 80 %.
 */
@Mixin(Entity.class)
public abstract class EntityStepMixin {
    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void modeminecraft$rangerSteps(BlockPos pos, BlockState state, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LivingEntity e) || state.liquid() || !(GearItem.worn(e, EquipmentSlot.FEET) instanceof GearItem boots)) {
            return;
        }
        if (!GearEffects.stealthy(e)) {
            return;                                   // pas normaux de Minecraft, selon le bloc
        }
        var type = state.getSoundType(e.level(), pos, e);
        e.playSound(type.getStepSound(), type.getVolume() * 0.15f * 0.2f, type.getPitch());
        ci.cancel();
    }
}

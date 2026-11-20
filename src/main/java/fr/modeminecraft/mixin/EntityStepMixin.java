package fr.modeminecraft.mixin;

import fr.modeminecraft.item.GearEffects;
import fr.modeminecraft.item.GearItem;
import fr.modeminecraft.registry.ModSounds;
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
 * Bruits de pas des rangers : semelles lourdes (ranger_step) ; bottes furtives : pas atténués de 80 %.
 * Les liquides et les blocs spéciaux gardent leur bruit normal.
 */
@Mixin(Entity.class)
public abstract class EntityStepMixin {
    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void modeminecraft$rangerSteps(BlockPos pos, BlockState state, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LivingEntity e) || state.liquid() || !(GearItem.worn(e, EquipmentSlot.FEET) instanceof GearItem boots)) {
            return;
        }
        var type = state.getSoundType(e.level(), pos, e);
        if (GearEffects.stealthy(e)) {
            e.playSound(type.getStepSound(), type.getVolume() * 0.15f * 0.2f, type.getPitch());
        } else {
            e.playSound(type.getStepSound(), type.getVolume() * 0.08f, type.getPitch());
            e.playSound(ModSounds.RANGER_STEP.get(), 0.18f, 0.9f + e.getRandom().nextFloat() * 0.2f);
        }
        ci.cancel();
    }
}

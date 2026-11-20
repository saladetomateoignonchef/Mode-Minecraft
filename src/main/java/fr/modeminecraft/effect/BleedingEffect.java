package fr.modeminecraft.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Saignement : 1 PV par seconde. */
public class BleedingEffect extends MobEffect {
    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0A0A);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Porte-plaques AVS : garrot et trousse de soin à portée de main, le saignement est réduit de moitié
        boolean avs = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(fr.modeminecraft.registry.ModItems.AVS_PLATE_CARRIER.get());
        entity.hurt(entity.damageSources().generic(), avs ? 0.5f : 1.0f);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}

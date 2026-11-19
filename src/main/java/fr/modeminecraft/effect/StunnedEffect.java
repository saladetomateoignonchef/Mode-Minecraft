package fr.modeminecraft.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;

/** Paralysie (taser) : la cible ne peut plus bouger ni attaquer pendant quelques secondes. */
public class StunnedEffect extends MobEffect {
    public StunnedEffect() {
        super(MobEffectCategory.HARMFUL, 0xF0E040);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            if (mob instanceof Creeper creeper) {
                creeper.setSwellDir(-1); // empêche le Creeper d'exploser
            }
        }
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0, 1, 0));
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}

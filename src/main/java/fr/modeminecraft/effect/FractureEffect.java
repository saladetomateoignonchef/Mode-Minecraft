package fr.modeminecraft.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Fracture : la jambe est cassée, on boite. Niveau I (amplificateur 0) = jambe immobilisée par une attelle
 * (vitesse et saut un peu réduits) ; niveau II (amplificateur 1) = fracture complète (vitesse et saut très réduits,
 * pas de sprint). Elle ne disparaît pas toute seule : il faut un plâtre (ou un lit d'hôpital). Les modificateurs de vitesse et de saut sont ajoutés dans {@link fr.modeminecraft.registry.ModEffects}.
 */
public class FractureEffect extends MobEffect {
    public FractureEffect() {
        super(MobEffectCategory.HARMFUL, 0xC8C0A8);
    }

    @Override
    public boolean applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        if (amplifier >= 1 && entity.isSprinting()) {
            entity.setSprinting(false);                    // fracture complète : impossible de courir
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}

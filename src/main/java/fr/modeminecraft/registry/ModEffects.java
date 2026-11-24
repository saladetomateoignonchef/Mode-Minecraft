package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.effect.StunnedEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Effets du mod. */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ModeMinecraft.MODID);

    /** Paralysie : vitesse nulle, pas d'attaque (taser, matraque électrique). */
    public static final DeferredHolder<MobEffect, MobEffect> STUNNED = EFFECTS.register("stunned",
            () -> new StunnedEffect()
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, ModeMinecraft.id("stunned_speed"), -1.0,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, ModeMinecraft.id("stunned_attack"), -1.0,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** IEM : viseurs électroniques, lasers et lunettes spéciales coupés. */
    public static final DeferredHolder<MobEffect, MobEffect> EMP = EFFECTS.register("emp",
            () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x3C8CFF) {});

    /** Saignement : 1 PV par seconde (kukri). */
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = EFFECTS.register("bleeding",
            () -> new fr.modeminecraft.effect.BleedingEffect());

    /** Fracture : jambe immobilisée par une attelle (niveau I) ou cassée (niveau II). Guérie par un plâtre. */
    public static final DeferredHolder<MobEffect, MobEffect> FRACTURE = EFFECTS.register("fracture",
            () -> new fr.modeminecraft.effect.FractureEffect()
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, ModeMinecraft.id("fracture_speed"), -0.25,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, ModeMinecraft.id("fracture_jump"), -0.25,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private ModEffects() {}
}

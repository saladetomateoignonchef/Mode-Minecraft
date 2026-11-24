package fr.modeminecraft.event;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.MedicalItem;
import fr.modeminecraft.registry.ModDamageTypes;
import fr.modeminecraft.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Blessures réalistes (joueurs) :
 * - une chute importante ou une explosion proche peut casser une jambe (fracture : vitesse et saut très réduits, pas de sprint) ;
 * - une balle un peu forte peut provoquer un saignement.
 * Soins : plâtre (fracture), attelle (jambe immobilisée), bandage ou garrot (saignement) — voir {@link MedicalItem}.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class InjuryEvents {
    /** Durée d'une fracture non soignée : 20 minutes. */
    private static final int FRACTURE_TICKS = 24000;

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        DamageSource source = event.getSource();
        float damage = event.getNewDamage();
        if (source.is(DamageTypeTags.IS_FALL) && damage >= 4f) {
            // chute : de 4 PV (probabilité 20 %) jusqu'à 8 PV et plus (certain)
            if (player.getRandom().nextFloat() < Math.min(1f, (damage - 3f) / 5f)) {
                fracture(player, "fall");
            }
        } else if (source.is(DamageTypeTags.IS_EXPLOSION) && damage >= 6f) {
            if (player.getRandom().nextFloat() < 0.5f) {
                fracture(player, "blast");
            }
        } else if ((source.is(ModDamageTypes.BULLET) || source.is(ModDamageTypes.BULLET_AP)) && damage >= 4f) {
            if (player.getRandom().nextFloat() < 0.3f) {
                bleed(player);
            }
        }
    }

    private static void fracture(ServerPlayer player, String cause) {
        var current = player.getEffect(ModEffects.FRACTURE);
        if (current != null && current.getAmplifier() >= 1) {
            return;                                                // déjà cassée
        }
        player.addEffect(new MobEffectInstance(ModEffects.FRACTURE, FRACTURE_TICKS, 1, false, true, true));
        player.displayClientMessage(Component.translatable("message.modeminecraft.injury.fracture." + cause).withStyle(ChatFormatting.RED), true);
    }

    private static void bleed(ServerPlayer player) {
        if (player.hasEffect(ModEffects.BLEEDING)
                || player.getPersistentData().getLong(MedicalItem.TOURNIQUET_TAG) > player.level().getGameTime()) {
            return;
        }
        player.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 200));
        player.displayClientMessage(Component.translatable("message.modeminecraft.injury.bleeding").withStyle(ChatFormatting.DARK_RED), true);
    }

    private InjuryEvents() {}
}

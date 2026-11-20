package fr.modeminecraft.item;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.registry.ModDamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Protections des casques en dehors des tirs à la tête (ceux-là sont dans BulletEntity) :
 * - mêlée (Galea -10 %, heaume -20 %), éclats de grenade et de Claymore (Adrian -30 %, Brodie -35 %),
 *   explosions (M1 -15 %) ; le casque s'use un peu à chaque coup encaissé ;
 * - jumelles de vision nocturne du FAST : vision nocturne sans potion tant qu'elles sont abaissées.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class HelmetEvents {
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        ItemStack helm = target.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helm.getItem() instanceof HelmetItem h)) {
            return;
        }
        HelmetItem.Stats s = h.stats();
        var source = event.getSource();
        float reduction = 0;
        boolean melee = source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity()
                && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.IS_EXPLOSION);
        // Éclats : dégâts "balle" qui ne viennent pas d'une vraie balle (grenades, Claymore)
        boolean shrapnel = source.is(ModDamageTypes.BULLET) && !(source.getDirectEntity() instanceof BulletEntity);
        if (melee) {
            reduction = s.melee();
        } else if (shrapnel) {
            reduction = s.shrapnel();
        } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            reduction = s.explosion();
        }
        if (reduction > 0) {
            float before = event.getAmount();
            event.setAmount(before * (1 - reduction));
            helm.hurtAndBreak(Math.max(1, Math.round(before * reduction / 2)), target, EquipmentSlot.HEAD);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 20 != 0) {
            return;
        }
        ItemStack helm = player.getItemBySlot(EquipmentSlot.HEAD);
        boolean nvg = helm.getItem() instanceof HelmetItem h && h.stats().module() == HelmetItem.Module.NVG && HelmetItem.isModuleActive(helm);
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (nvg) {
            if (current == null || current.getDuration() < 260) {
                // ambiante, sans particules ni icône : c'est l'électronique du casque, pas une potion
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, false));
            }
        } else if (current != null && current.isAmbient() && !current.isVisible() && current.getDuration() <= 300) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private HelmetEvents() {}
}

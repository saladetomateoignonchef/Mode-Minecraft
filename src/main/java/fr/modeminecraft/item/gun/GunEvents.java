package fr.modeminecraft.item.gun;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Événements côté serveur liés aux accessoires :
 * - délai pour sortir l'arme (réduit par la poignée compacte) ;
 * - vision nocturne quand on vise (lampe tactique, lunette de vision nocturne) ;
 * - baïonnette : +6 dégâts au corps à corps.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class GunEvents {
    private static final Map<UUID, ItemStack> LAST_HELD = new ConcurrentHashMap<>();
    private static final Set<UUID> NIGHT_VISION = ConcurrentHashMap.newKeySet();
    private static final int NV_DURATION = 300;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        GunItem gun = held.getItem() instanceof GunItem g ? g : null;

        // Délai pour sortir l'arme quand on la prend en main
        ItemStack previous = LAST_HELD.put(player.getUUID(), held);
        if (gun != null && previous != held && previous != null && !player.getAbilities().instabuild) {
            int draw = Math.round(gun.stats().drawTicks() * gun.resolve(held).drawMul());
            if (draw > 0 && !player.getCooldowns().isOnCooldown(gun)) {
                player.getCooldowns().addCooldown(gun, draw);
            }
        }

        // Vision nocturne pendant la visée
        boolean nv = gun != null && GunItem.isAiming(player) && gun.resolve(held).has(AttachmentItem.Special.NIGHT_VISION);
        if (nv) {
            if (player.tickCount % 20 == 0 || !NIGHT_VISION.contains(player.getUUID())) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NV_DURATION, 0, true, false, false));
            }
            NIGHT_VISION.add(player.getUUID());
        } else if (NIGHT_VISION.remove(player.getUUID())) {
            // On retire seulement la vision nocturne que l'on a donnée (pas celle d'une potion)
            MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
            if (effect != null && effect.getDuration() <= NV_DURATION) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    /** Baïonnette : coup de corps à corps renforcé. */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof Player player
                && event.getSource().getEntity() == player
                && player.getMainHandItem().getItem() instanceof GunItem gun
                && gun.resolve(player.getMainHandItem()).has(AttachmentItem.Special.BAYONET)) {
            event.setAmount(event.getAmount() + 6);
        }
    }

    private GunEvents() {}
}

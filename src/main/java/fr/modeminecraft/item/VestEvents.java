package fr.modeminecraft.item;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.registry.ModDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gilets pare-balles, en dehors des balles (traitées dans BulletEntity) :
 * - éclats de grenade / Claymore : la plaque avant les arrête, puis réduction du gilet ;
 * - explosions et mêlée : réduction du gilet ;
 * - plaques insérées : +1 d'armure chacune (+2 avec les deux), plaque en acier -3 % de vitesse ;
 * - EOD : aucun recul, et sauter fatigue davantage ; AVS : +25 % de régénération hors combat.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class VestEvents {
    private static final Map<UUID, Long> LAST_HURT = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        ItemStack vest = fr.modeminecraft.item.tactical.TacticalSlots.vest(target);
        if (!(vest.getItem() instanceof VestItem v) || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        var source = event.getSource();
        VestItem.Stats s = v.stats();
        boolean shrapnel = source.is(ModDamageTypes.BULLET) && !(source.getDirectEntity() instanceof BulletEntity);
        if (shrapnel) {
            // Éclats : la plaque avant les arrête (calibre "léger"), puis le gilet les réduit
            float amount = VestItem.protect(level, target, event.getAmount(), 0, true);
            event.setAmount(amount * (1 - s.shrapnel()));
            return;
        }
        float r = 0;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            r = s.explosion();
        } else if (source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity()
                && !source.is(DamageTypeTags.IS_PROJECTILE)) {
            r = s.melee();
        }
        if (r > 0) {
            float before = event.getAmount();
            event.setAmount(before * (1 - r));
            vest.hurtAndBreak(Math.max(1, Math.round(before * r / 3)), target, EquipmentSlot.CHEST);
        }
    }

    /** Armure des plaques insérées et poids des plaques en acier. */
    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof VestItem v) || !v.stats().plates()) {
            return;
        }
        int plates = 0, steel = 0;
        for (boolean front : new boolean[]{true, false}) {
            VestItem.Plate p = VestItem.plate(stack, front);
            if (p != null && p.durability() > 0) {
                plates++;
                if (p.kind() == VestItem.PlateType.STEEL) {
                    steel++;
                }
            }
        }
        if (plates > 0) {
            event.addModifier(Attributes.ARMOR, new AttributeModifier(ModeMinecraft.id("vest_plates"), plates,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST);
        }
        if (steel > 0) {
            event.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(ModeMinecraft.id("vest_steel_plates"), -0.03 * steel,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.CHEST);
        }
    }

    /** Combinaison EOD : elle annule tout recul (balles, explosions). */
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        VestItem v = VestItem.worn(event.getEntity());
        if (v != null && v.stats().special() == VestItem.Special.EOD) {
            event.setCanceled(true);
        }
    }

    /** Combinaison EOD : sauter avec 30 kg sur le dos donne faim. */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        VestItem v = VestItem.worn(event.getEntity());
        if (v != null && v.stats().special() == VestItem.Special.EOD && event.getEntity() instanceof Player p) {
            p.causeFoodExhaustion(0.6f);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LAST_HURT.put(event.getEntity().getUUID(), event.getEntity().level().getGameTime());
    }

    /** Porte-plaques AVS : +25 % de régénération naturelle hors combat (10 s sans être blessé). */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity e = event.getEntity();
        VestItem v = VestItem.worn(e);
        if (v != null && v.stats().special() == VestItem.Special.AVS
                && e.level().getGameTime() - LAST_HURT.getOrDefault(e.getUUID(), -10000L) > 200) {
            event.setAmount(event.getAmount() * 1.25f);
        }
    }

    private VestEvents() {}
}

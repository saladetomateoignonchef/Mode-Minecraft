package fr.modeminecraft.entity.military;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Passagers à l'abri dans un habitacle blindé (places « protégées ») : ni balles, ni éclats, ni souffle tant que le
 * véhicule tient (c'est lui qui encaisse). Les tireurs exposés (tourelle ouverte, sabord) restent vulnérables.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class MilEvents {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        var e = event.getEntity();
        if (!(e.getVehicle() instanceof MilVehicleEntity v)) {
            return;
        }
        MilType.Seat s = v.seat(e);
        var src = event.getSource();
        if (s != null && s.enclosed() && (src.is(DamageTypeTags.IS_PROJECTILE) || src.is(DamageTypeTags.IS_EXPLOSION)
                || src.getDirectEntity() instanceof fr.modeminecraft.entity.BulletEntity || src.is(DamageTypeTags.IS_FALL)
                || src.getEntity() != null && v.hasPassenger(src.getEntity()))) {
            event.setCanceled(true);
        }
    }

    /** Pas de descente en plein vol (Maj) : il faut s'éjecter (C maintenue) ou sauter par la rampe du C-130. */
    @SubscribeEvent
    public static void onMount(net.neoforged.neoforge.event.entity.EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityBeingMounted() instanceof MilVehicleEntity v && !v.mayDismount(event.getEntityMounting())) {
            event.setCanceled(true);
            if (event.getEntityMounting() instanceof net.minecraft.world.entity.player.Player p && !p.level().isClientSide) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.modeminecraft.air.no_dismount")
                        .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
            }
        }
    }

    private MilEvents() {}
}

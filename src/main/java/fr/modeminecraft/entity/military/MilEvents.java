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

    private MilEvents() {}
}

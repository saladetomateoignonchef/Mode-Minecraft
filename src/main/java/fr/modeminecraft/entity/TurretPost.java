package fr.modeminecraft.entity;

import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.entity.military.RocketLauncherEntity;
import fr.modeminecraft.ModeMinecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Poste de tourelle : un joueur ou un PNJ assis sur une mitrailleuse fixe, un lanceur statique ou au poste de tireur
 * d'un véhicule ne tient plus son arme personnelle (rendu masqué) et ne peut plus s'en servir.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class TurretPost {
    private TurretPost() {}

    public static boolean operating(Entity rider) {
        if (rider == null) {
            return false;
        }
        Entity v = rider.getVehicle();
        if (v instanceof MountedGunEntity || v instanceof RocketLauncherEntity) {
            return true;
        }
        if (v instanceof MilVehicleEntity mil) {
            MilType.Seat seat = mil.seat(rider);
            return seat != null && (seat.role() == MilType.Role.GUNNER || seat.role() == MilType.Role.DOOR_GUNNER
                    || seat.role() == MilType.Role.GRENADIER || seat.role() == MilType.Role.OPERATOR);
        }
        return false;
    }

    private static boolean personalWeapon(Item item) {
        return item instanceof fr.modeminecraft.item.gun.GunItem || item instanceof fr.modeminecraft.item.melee.MeleeWeaponItem
                || item instanceof fr.modeminecraft.item.explosive.GrenadeItem
                || item instanceof fr.modeminecraft.item.explosive.PlaceableExplosiveItem;
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (operating(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (operating(event.getEntity()) && personalWeapon(event.getItemStack().getItem())) {
            event.setCanceled(true);
        }
    }
}

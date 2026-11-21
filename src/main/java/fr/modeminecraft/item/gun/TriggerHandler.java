package fr.modeminecraft.item.gun;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Commandes façon FPS :
 * - clic gauche maintenu = détente (le tir suit la cadence de l'arme et son mode de tir) ;
 * - clic droit maintenu = visée.
 * Le client envoie l'état des deux boutons au serveur ; les deux côtés font tourner la même logique
 * (le client pour le recul et les effets, le serveur pour les balles).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class TriggerHandler {
    /** État de la détente d'un joueur. */
    public static final class Trigger {
        boolean down;
        /** Ticks depuis l'appui. */
        int held;
        /** Balles tirées depuis l'appui. */
        int shots;
        /** Il faut relâcher avant de pouvoir tirer à nouveau (coup par coup, fin de rafale, arme vide...). */
        boolean locked;

        public int held() {
            return held;
        }

        public int shots() {
            return shots;
        }

        public void lock() {
            locked = true;
        }
    }

    private static final Map<UUID, Trigger> SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, Trigger> CLIENT = new ConcurrentHashMap<>();
    private static final Set<UUID> AIM_SERVER = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> AIM_CLIENT = ConcurrentHashMap.newKeySet();

    private static Map<UUID, Trigger> triggers(Player p) {
        return p.level().isClientSide ? CLIENT : SERVER;
    }

    public static Trigger trigger(Player player) {
        return triggers(player).computeIfAbsent(player.getUUID(), u -> new Trigger());
    }

    public static void setTrigger(Player player, boolean down) {
        Trigger t = trigger(player);
        if (t.down != down) {
            t.down = down;
            t.held = 0;
            t.shots = 0;
            t.locked = false;
        }
    }

    public static boolean isTriggerDown(Player player) {
        Trigger t = triggers(player).get(player.getUUID());
        return t != null && t.down;
    }

    public static void setAiming(Player player, boolean aiming) {
        Set<UUID> set = player.level().isClientSide ? AIM_CLIENT : AIM_SERVER;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof GunItem gun && gun.stats().doubleBarrel()) {
            // Pas de visée avec un fusil à canon scié : le clic droit tire les deux canons d'un coup
            if (aiming && !set.contains(player.getUUID())) {
                gun.fireBothBarrels(player.level(), player, held);
            }
            if (aiming) set.add(player.getUUID()); else set.remove(player.getUUID());
            return;
        }
        if (aiming) {
            set.add(player.getUUID());
        } else {
            set.remove(player.getUUID());
        }
    }

    /** Clic droit maintenu (même avec une arme qui ne vise pas : fanning du Colt SAA). */
    public static boolean isRightHeld(Player player) {
        return (player.level().isClientSide ? AIM_CLIENT : AIM_SERVER).contains(player.getUUID());
    }

    public static boolean isAiming(Player player) {
        if (player.getMainHandItem().getItem() instanceof GunItem gun
                && (gun.stats().doubleBarrel() || gun.stats().special() == GunStats.Special.FANNING)) {
            return false;
        }
        if (fr.modeminecraft.item.ShieldCombat.blocked(player)) {
            return false;
        }
        return (player.level().isClientSide ? AIM_CLIENT : AIM_SERVER).contains(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Trigger t = triggers(player).get(player.getUUID());
        if (t == null || !t.down) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun) || player.isSpectator()) {
            return;
        }
        if (fr.modeminecraft.item.ShieldCombat.blocked(player)) {
            fr.modeminecraft.item.ShieldCombat.warn(player);          // fusil + bouclier : ni tir ni visée
            return;
        }
        gun.triggerTick(player.level(), player, stack, t);
        t.held++;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        SERVER.remove(id);
        CLIENT.remove(id);
        AIM_SERVER.remove(id);
        AIM_CLIENT.remove(id);
    }

    private TriggerHandler() {}
}

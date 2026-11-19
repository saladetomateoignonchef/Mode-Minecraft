package fr.modeminecraft.item.gun;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Position allongée (double appui sur "s'accroupir"), avec ou sans arme.
 * On force la pose "nage" de Minecraft, qui sur la terre ferme correspond à ramper.
 * Allongé : l'arme est beaucoup plus stable (recul et dispersion divisés par 2).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class ProneHandler {
    // Un ensemble par côté (client / serveur), car en solo les deux tournent dans le même programme
    private static final Set<UUID> SERVER = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> CLIENT = ConcurrentHashMap.newKeySet();

    private static Set<UUID> side(Player player) {
        return player.level().isClientSide ? CLIENT : SERVER;
    }

    public static boolean isProne(Player player) {
        return side(player).contains(player.getUUID());
    }

    /** Peut-on se relever ici ? (pas de plafond trop bas) */
    public static boolean canStandUp(Player player) {
        return player.level().noCollision(player, player.getDimensions(Pose.STANDING).makeBoundingBox(player.position()));
    }

    /** Se coucher ou se relever. Renvoie false si c'est impossible. */
    public static boolean setProne(Player player, boolean prone) {
        if (prone == isProne(player)) {
            return true;
        }
        if (prone) {
            if (player.isInWater() || player.isPassenger() || player.getAbilities().flying || player.isFallFlying()) {
                return false;
            }
            side(player).add(player.getUUID());
            player.setForcedPose(Pose.SWIMMING);
        } else {
            if (!canStandUp(player)) {
                if (!player.level().isClientSide) {
                    player.displayClientMessage(Component.translatable("message.modeminecraft.prone.no_room"), true);
                }
                return false;
            }
            side(player).remove(player.getUUID());
            player.setForcedPose(null);
        }
        return true;
    }

    /** Dans l'eau, en vol ou sur une monture, on ne peut pas rester allongé. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (isProne(player) && (player.isInWater() || player.isPassenger() || player.getAbilities().flying)) {
            side(player).remove(player.getUUID());
            player.setForcedPose(null);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            side(player).remove(player.getUUID());
            player.setForcedPose(null);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SERVER.remove(event.getEntity().getUUID());
        CLIENT.remove(event.getEntity().getUUID());
    }

    private ProneHandler() {}
}

package fr.modeminecraft.item.gun;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.network.LeanSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Se pencher à gauche (-1) ou à droite (+1) en visant, pour tirer depuis un coin de mur.
 * Le serveur décale le départ des balles ; les autres joueurs voient le tireur pencher.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class LeanHandler {
    /** Décalage maximal de la tête (en blocs). */
    public static final double MAX_OFFSET = 0.5;

    private static final Map<UUID, Integer> SERVER = new ConcurrentHashMap<>();
    /** Côté client : entityId -> direction. */
    public static final Map<Integer, Integer> CLIENT = new ConcurrentHashMap<>();

    public static int get(Player player) {
        return player.level().isClientSide ? CLIENT.getOrDefault(player.getId(), 0) : SERVER.getOrDefault(player.getUUID(), 0);
    }

    public static void set(ServerPlayer player, int dir) {
        dir = Integer.signum(dir);
        if (dir == get(player)) {
            return;
        }
        if (dir == 0) {
            SERVER.remove(player.getUUID());
        } else {
            SERVER.put(player.getUUID(), dir);
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new LeanSyncPayload(player.getId(), dir));
    }

    /** Vecteur "droite" horizontal du joueur. */
    public static Vec3 right(Player player) {
        float yaw = (float) Math.toRadians(player.getYRot());
        return new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
    }

    /** Décalage de la tête quand le joueur se penche (limité par les murs). */
    public static Vec3 offset(Player player) {
        return offsetFor(player, get(player));
    }

    /** Décalage de la tête pour une direction donnée (limité par les murs). */
    public static Vec3 offsetFor(Player player, int dir) {
        if (dir == 0) {
            return Vec3.ZERO;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 wanted = right(player).scale(dir * MAX_OFFSET);
        HitResult hit = player.level().clip(new ClipContext(eye, eye.add(wanted.scale(1.4)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double room = hit.getType() == HitResult.Type.MISS ? 1.0 : Math.max(0, hit.getLocation().distanceTo(eye) - 0.2) / MAX_OFFSET;
        return wanted.scale(Math.min(1.0, room));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SERVER.remove(event.getEntity().getUUID());
        CLIENT.remove(event.getEntity().getId());
    }

    private LeanHandler() {}
}

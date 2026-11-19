package fr.modeminecraft.item.gun;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stinger / Javelin : cible verrouillée par chaque joueur (envoyée par le client, vérifiée au tir). */
public final class LockOnHandler {
    private static final Map<UUID, Integer> LOCKED = new ConcurrentHashMap<>();

    public static void set(Player player, int entityId) {
        if (entityId < 0) {
            LOCKED.remove(player.getUUID());
        } else {
            LOCKED.put(player.getUUID(), entityId);
        }
    }

    /** Cible verrouillée encore valide (vivante, à moins de 200 blocs), ou null. */
    public static Entity target(Player player) {
        Integer id = LOCKED.get(player.getUUID());
        Entity e = id == null ? null : player.level().getEntity(id);
        return e != null && e.isAlive() && e.distanceToSqr(player) < 200 * 200 ? e : null;
    }

    private LockOnHandler() {}
}

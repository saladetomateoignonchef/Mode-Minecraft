package fr.modeminecraft.item.gun;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.network.BackGunsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Armes visibles sur le joueur quand il ne les tient pas : la 1re arme longue de la barre d'action
 * dans le dos, le 1er pistolet à la hanche. Les autres clients ne connaissent pas l'inventaire
 * des joueurs : le serveur leur envoie ces deux objets quand ils changent.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class BackGunsHandler {
    private static final Map<UUID, String> LAST = new ConcurrentHashMap<>();

    private static ItemStack[] pick(Player player) {
        ItemStack back = ItemStack.EMPTY, hip = ItemStack.EMPTY;
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().items.get(i);
            if (i == player.getInventory().selected || !(s.getItem() instanceof GunItem gun)) {
                continue;
            }
            if (gun.stats().pistol()) {
                if (hip.isEmpty()) hip = s;
            } else if (back.isEmpty()) {
                back = s;
            }
        }
        return new ItemStack[]{back.copyWithCount(1), hip.copyWithCount(1)};
    }

    /** Clé qui ne change que si l'apparence change (pas à chaque balle tirée). */
    private static String key(ItemStack s) {
        return s.isEmpty() ? "-" : s.getItem() + "|" + GunItem.getSkin(s) + "|" + GunItem.getAttachments(s);
    }

    private static BackGunsPayload payload(Player player) {
        ItemStack[] p = pick(player);
        return new BackGunsPayload(player.getId(), p[0], p[1]);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        ItemStack[] p = pick(player);
        String key = key(p[0]) + "#" + key(p[1]);
        if (!key.equals(LAST.get(player.getUUID()))) {
            LAST.put(player.getUUID(), key);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new BackGunsPayload(player.getId(), p[0], p[1]));
        }
    }

    /** Un joueur en voit un autre apparaître : on lui envoie ses armes rangées. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player target && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, payload(target));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    private BackGunsHandler() {}
}

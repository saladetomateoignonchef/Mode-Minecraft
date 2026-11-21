package fr.modeminecraft.world.city;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.network.CityAmbiencePayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Suit la ville où se trouve chaque joueur (ambiance sonore côté client, circulation, événements de quartier). */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class CityPresence {
    private static final Map<UUID, CityPlanner.City> CURRENT = new ConcurrentHashMap<>();

    public static CityPlanner.City of(ServerPlayer player) {
        return CURRENT.get(player.getUUID());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 40 != 7
                || !(player.level() instanceof ServerLevel level) || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return;
        }
        CityPlanner.City city = CityPlanner.cityAt(CityPlanner.Env.of(level), player.getBlockX(), player.getBlockZ(), 8);
        CityPlanner.City before = CURRENT.get(player.getUUID());
        if (city == null) {
            CURRENT.remove(player.getUUID());
        } else {
            CURRENT.put(player.getUUID(), city);
        }
        if (before != city && (before == null || city == null || !before.center().equals(city.center()))) {
            PacketDistributor.sendToPlayer(player, new CityAmbiencePayload(city == null ? "" : city.variant(), city == null ? 0 : city.baseY()));
        }
    }

    private CityPresence() {}
}

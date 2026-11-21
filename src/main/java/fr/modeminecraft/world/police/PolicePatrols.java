package fr.modeminecraft.world.police;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** Voitures de patrouille lancées à la poursuite d'un suspect (voir la circulation des villes). */
public final class PolicePatrols {
    /** Point d'entrée : implémenté par la circulation (TrafficManager) quand elle existe. */
    public static java.util.function.BiConsumer<ServerLevel, Player> chase = (l, p) -> { };

    /** Une sirène approche : la circulation civile proche se range (branché par la circulation). */
    public static java.util.function.Consumer<fr.modeminecraft.entity.VehicleEntity> yield = v -> { };

    public static void dispatch(ServerLevel level, BlockPos pos, Player player) {
        chase.accept(level, player);
    }

    private PolicePatrols() {}
}

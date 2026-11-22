package fr.modeminecraft.mixin;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Avions rapides (jusqu'à 5 blocs / tick) : un véhicule qui entre dans un chunk pas encore envoyé à son pilote cessait
 * d'être suivi par ce joueur — le véhicule disparaissait sous lui côté client et il se retrouvait éjecté. Le véhicule
 * qui porte le joueur (directement ou non) reste donc toujours suivi par ce joueur.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {
    @Shadow @Final Entity entity;
    @Shadow @Final ServerEntity serverEntity;
    @Shadow @Final Set<ServerPlayerConnection> seenBy;

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void modeminecraft$keepOwnVehicle(ServerPlayer player, CallbackInfo ci) {
        if (player != entity && entity instanceof fr.modeminecraft.entity.military.MilVehicleEntity && player.getRootVehicle() == entity) {
            if (seenBy.add(player.connection)) {
                serverEntity.addPairing(player);
            }
            ci.cancel();
        }
    }

}

package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Commandes dans un véhicule militaire : fumigènes, changer de place, type de munition, déploiement. */
public record MilControlPayload(int action) implements CustomPacketPayload {
    public static final int SMOKE = 0, SEAT = 1, AMMO = 2, DEPLOY = 3, BAY = 4, GEAR = 5, DROP = 7, AB_ON = 8, AB_OFF = 9, EJECT = 10;
    public static final Type<MilControlPayload> TYPE = new Type<>(ModeMinecraft.id("mil_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MilControlPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(MilControlPayload::new, MilControlPayload::action).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MilControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p && p.getVehicle() instanceof MilVehicleEntity v) {
                switch (payload.action()) {
                    case SMOKE -> v.smoke();
                    case SEAT -> v.nextSeat(p);
                    case AMMO -> v.cycleAmmo(p);
                    case DEPLOY -> v.toggleDeploy(p);
                    case BAY -> v.toggleBay(p);
                    case GEAR -> v.toggleGear(p);
                    case DROP -> v.dropBomb(p);
                    case AB_ON -> v.setAfterburner(p, true);
                    case AB_OFF -> v.setAfterburner(p, false);
                    case EJECT -> v.eject(p);
                    default -> {}
                }
            }
        });
    }
}

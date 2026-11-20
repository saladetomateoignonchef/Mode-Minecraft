package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> client : début (caméra du drone) ou fin du pilotage. */
public record DronePilotPayload(int droneId, boolean start) implements CustomPacketPayload {
    public static final Type<DronePilotPayload> TYPE = new Type<>(ModeMinecraft.id("drone_pilot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DronePilotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DronePilotPayload::droneId, ByteBufCodecs.BOOL, DronePilotPayload::start, DronePilotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DronePilotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> fr.modeminecraft.client.DroneClient.onPilot(payload.droneId(), payload.start()));
    }
}

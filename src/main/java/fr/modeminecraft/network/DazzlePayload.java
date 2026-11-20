package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> joueur : il regarde une lampe en mode stroboscope (halo blanc pulsant, force 0 à 1). */
public record DazzlePayload(float strength) implements CustomPacketPayload {
    public static final Type<DazzlePayload> TYPE = new Type<>(ModeMinecraft.id("dazzle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DazzlePayload> STREAM_CODEC =
            ByteBufCodecs.FLOAT.map(DazzlePayload::new, DazzlePayload::strength).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DazzlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> fr.modeminecraft.client.FlashlightClient.dazzle(payload.strength()));
    }
}

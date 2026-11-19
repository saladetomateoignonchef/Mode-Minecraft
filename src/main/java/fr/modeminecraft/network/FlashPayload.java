package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> joueur : il est ébloui par une flashbang (0 à 1). */
public record FlashPayload(float strength) implements CustomPacketPayload {
    public static final Type<FlashPayload> TYPE = new Type<>(ModeMinecraft.id("flash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlashPayload> STREAM_CODEC =
            ByteBufCodecs.FLOAT.map(FlashPayload::new, FlashPayload::strength).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FlashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> fr.modeminecraft.client.FlashClient.flash(payload.strength()));
    }
}

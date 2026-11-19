package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> tireur : sa balle a touché (0), touché à la tête (1) ou tué (2). */
public record HitMarkerPayload(int kind) implements CustomPacketPayload {
    public static final int HIT = 0, HEADSHOT = 1, KILL = 2;
    public static final Type<HitMarkerPayload> TYPE = new Type<>(ModeMinecraft.id("hit_marker"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HitMarkerPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(HitMarkerPayload::new, HitMarkerPayload::kind).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HitMarkerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> fr.modeminecraft.client.GunFx.onHitMarker(payload.kind()));
    }
}

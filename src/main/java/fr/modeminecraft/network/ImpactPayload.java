package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> clients proches : une balle a laissé un trou sur la face d'un bloc. */
public record ImpactPayload(Vec3 hit, Direction face) implements CustomPacketPayload {
    public static final Type<ImpactPayload> TYPE = new Type<>(ModeMinecraft.id("impact"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImpactPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VECTOR3F.map(Vec3::new, Vec3::toVector3f), ImpactPayload::hit,
            Direction.STREAM_CODEC, ImpactPayload::face, ImpactPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ImpactPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> fr.modeminecraft.client.ImpactDecals.add(payload.hit(), payload.face()));
    }
}

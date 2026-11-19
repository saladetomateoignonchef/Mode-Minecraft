package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.ProneHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Le joueur veut se coucher (true) ou se relever (false). */
public record PronePayload(boolean prone) implements CustomPacketPayload {
    public static final Type<PronePayload> TYPE = new Type<>(ModeMinecraft.id("prone"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PronePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(PronePayload::new, PronePayload::prone).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PronePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ProneHandler.setProne(context.player(), payload.prone()));
    }
}

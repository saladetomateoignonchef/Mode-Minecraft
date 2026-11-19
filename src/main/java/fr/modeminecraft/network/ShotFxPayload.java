package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> clients proches : un joueur vient de tirer (flash lumineux, culasse qui recule). */
public record ShotFxPayload(int entityId, boolean silenced) implements CustomPacketPayload {
    public static final Type<ShotFxPayload> TYPE = new Type<>(ModeMinecraft.id("shot_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShotFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShotFxPayload::entityId, ByteBufCodecs.BOOL, ShotFxPayload::silenced, ShotFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShotFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> fr.modeminecraft.client.GunFx.onRemoteShot(payload.entityId(), payload.silenced()));
    }
}

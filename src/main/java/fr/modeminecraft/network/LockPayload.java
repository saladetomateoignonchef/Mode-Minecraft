package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.LockOnHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> serveur : cible verrouillée par le Stinger / Javelin (-1 = aucune). */
public record LockPayload(int entityId) implements CustomPacketPayload {
    public static final Type<LockPayload> TYPE = new Type<>(ModeMinecraft.id("lock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LockPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(LockPayload::new, LockPayload::entityId).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> LockOnHandler.set(context.player(), payload.entityId()));
    }
}

package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.LeanHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> clients : tel joueur se penche (pour l'afficher penché). */
public record LeanSyncPayload(int entityId, int dir) implements CustomPacketPayload {
    public static final Type<LeanSyncPayload> TYPE = new Type<>(ModeMinecraft.id("lean_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeanSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LeanSyncPayload::entityId, ByteBufCodecs.VAR_INT, LeanSyncPayload::dir, LeanSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LeanSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.dir() == 0) {
                LeanHandler.CLIENT.remove(payload.entityId());
            } else {
                LeanHandler.CLIENT.put(payload.entityId(), payload.dir());
            }
        });
    }
}

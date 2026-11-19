package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.LeanHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Le joueur se penche : -1 gauche, 0 droit, +1 droite. */
public record LeanPayload(int dir) implements CustomPacketPayload {
    public static final Type<LeanPayload> TYPE = new Type<>(ModeMinecraft.id("lean"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeanPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(LeanPayload::new, LeanPayload::dir).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LeanPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                LeanHandler.set(player, payload.dir());
            }
        });
    }
}

package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.TriggerHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> serveur : état des boutons (détente = clic gauche, visée = clic droit). */
public record TriggerPayload(boolean trigger, boolean aim) implements CustomPacketPayload {
    public static final Type<TriggerPayload> TYPE = new Type<>(ModeMinecraft.id("trigger"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TriggerPayload::trigger, ByteBufCodecs.BOOL, TriggerPayload::aim, TriggerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TriggerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            TriggerHandler.setTrigger(context.player(), payload.trigger());
            TriggerHandler.setAiming(context.player(), payload.aim());
        });
    }
}

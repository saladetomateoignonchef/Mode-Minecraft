package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Touche L : allumer / éteindre la lampe tactique ; Maj + L (cycle = true) : mode suivant. */
public record FlashlightPayload(boolean cycle) implements CustomPacketPayload {
    public static final Type<FlashlightPayload> TYPE = new Type<>(ModeMinecraft.id("flashlight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlashlightPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(FlashlightPayload::new, FlashlightPayload::cycle).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FlashlightPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem gun) {
                gun.switchLight(player, stack, payload.cycle());
            }
        });
    }
}

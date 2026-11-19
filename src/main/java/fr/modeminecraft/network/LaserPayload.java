package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Touche T : allumer / éteindre le laser. */
public record LaserPayload() implements CustomPacketPayload {
    public static final LaserPayload INSTANCE = new LaserPayload();
    public static final Type<LaserPayload> TYPE = new Type<>(ModeMinecraft.id("laser"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaserPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LaserPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem gun) {
                gun.toggleLaser(player, stack);
            }
        });
    }
}

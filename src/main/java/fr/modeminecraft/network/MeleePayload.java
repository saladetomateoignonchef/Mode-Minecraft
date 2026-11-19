package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Touche C : coup de crosse (ou de baïonnette). */
public record MeleePayload() implements CustomPacketPayload {
    public static final MeleePayload INSTANCE = new MeleePayload();
    public static final Type<MeleePayload> TYPE = new Type<>(ModeMinecraft.id("melee"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MeleePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MeleePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem gun) {
                gun.melee(player, stack);
            }
        });
    }
}

package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Touche V : tir secondaire (lance-grenades M203 / fusil à pompe M26 sous le canon). */
public record UnderbarrelPayload() implements CustomPacketPayload {
    public static final UnderbarrelPayload INSTANCE = new UnderbarrelPayload();
    public static final Type<UnderbarrelPayload> TYPE = new Type<>(ModeMinecraft.id("underbarrel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UnderbarrelPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UnderbarrelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem gun) {
                gun.secondaryFire(player, stack);
            }
        });
    }
}

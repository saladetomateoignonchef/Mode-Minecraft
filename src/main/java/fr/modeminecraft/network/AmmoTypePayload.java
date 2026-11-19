package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Touche T : passer au type de munition suivant. */
public record AmmoTypePayload() implements CustomPacketPayload {
    public static final AmmoTypePayload INSTANCE = new AmmoTypePayload();
    public static final Type<AmmoTypePayload> TYPE = new Type<>(ModeMinecraft.id("ammo_type"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AmmoTypePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AmmoTypePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem gun) {
                gun.cycleAmmoType(player, stack);
            }
        });
    }
}

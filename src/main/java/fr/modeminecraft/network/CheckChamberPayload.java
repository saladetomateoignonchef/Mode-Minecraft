package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Touche H : vérifier s'il y a une balle dans la chambre. */
public record CheckChamberPayload() implements CustomPacketPayload {
    public static final CheckChamberPayload INSTANCE = new CheckChamberPayload();
    public static final Type<CheckChamberPayload> TYPE = new Type<>(ModeMinecraft.id("check_chamber"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CheckChamberPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CheckChamberPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem gun) {
                gun.checkChamber(player, stack);
            }
        });
    }
}

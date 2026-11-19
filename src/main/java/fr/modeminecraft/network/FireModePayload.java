package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Envoyé au serveur quand le joueur appuie sur la touche "Mode de tir" (B). */
public record FireModePayload() implements CustomPacketPayload {
    public static final FireModePayload INSTANCE = new FireModePayload();
    public static final Type<FireModePayload> TYPE = new Type<>(ModeMinecraft.id("fire_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireModePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FireModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem gun) {
                gun.cycleFireMode(player, stack);
            }
        });
    }
}

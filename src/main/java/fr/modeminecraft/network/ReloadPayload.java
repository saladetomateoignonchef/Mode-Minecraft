package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Paquet envoyé par le client au serveur quand le joueur appuie sur la touche "Recharger".
 * Il ne contient aucune donnée : le serveur regarde simplement l'arme en main.
 */
public record ReloadPayload() implements CustomPacketPayload {
    public static final ReloadPayload INSTANCE = new ReloadPayload();
    public static final Type<ReloadPayload> TYPE = new Type<>(ModeMinecraft.id("reload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReloadPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReloadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem gun) {
                gun.reload(player, stack);
            }
        });
    }
}

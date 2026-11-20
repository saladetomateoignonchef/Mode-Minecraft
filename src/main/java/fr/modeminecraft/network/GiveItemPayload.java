package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Livre des recettes, mode triche : le serveur donne l'objet (seulement en créatif ou aux opérateurs). */
public record GiveItemPayload(ItemStack stack, int count) implements CustomPacketPayload {
    public static final Type<GiveItemPayload> TYPE = new Type<>(ModeMinecraft.id("give_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GiveItemPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, GiveItemPayload::stack, ByteBufCodecs.VAR_INT, GiveItemPayload::count, GiveItemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GiveItemPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!player.isCreative() && !player.hasPermissions(2)) {
                return;
            }
            if (!payload.stack().getItem().builtInRegistryHolder().key().location().getNamespace().equals(ModeMinecraft.MODID)) {
                return; // seulement les objets du mod
            }
            ItemStack give = payload.stack().copyWithCount(Math.max(1, Math.min(payload.count(), payload.stack().getMaxStackSize())));
            if (!player.getInventory().add(give)) {
                player.drop(give, false);
            }
        });
    }
}

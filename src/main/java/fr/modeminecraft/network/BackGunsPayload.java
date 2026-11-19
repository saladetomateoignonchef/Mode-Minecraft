package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> clients : armes rangées par un joueur (fusil dans le dos, pistolet à la hanche). */
public record BackGunsPayload(int entityId, ItemStack back, ItemStack hip) implements CustomPacketPayload {
    public static final Type<BackGunsPayload> TYPE = new Type<>(ModeMinecraft.id("back_guns"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackGunsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BackGunsPayload::entityId, ItemStack.OPTIONAL_STREAM_CODEC, BackGunsPayload::back,
            ItemStack.OPTIONAL_STREAM_CODEC, BackGunsPayload::hip, BackGunsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BackGunsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> fr.modeminecraft.client.BackGunsLayer.update(payload.entityId(), payload.back(), payload.hip()));
    }
}

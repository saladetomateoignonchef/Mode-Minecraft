package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> clients : gilet et sac à dos portés par un joueur (pour les afficher sur lui). */
public record TacticalSyncPayload(int entityId, ItemStack vest, ItemStack backpack) implements CustomPacketPayload {
    public static final Type<TacticalSyncPayload> TYPE = new Type<>(ModeMinecraft.id("tactical_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TacticalSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TacticalSyncPayload::entityId, ItemStack.OPTIONAL_STREAM_CODEC, TacticalSyncPayload::vest,
            ItemStack.OPTIONAL_STREAM_CODEC, TacticalSyncPayload::backpack, TacticalSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TacticalSyncPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null && level.getEntity(p.entityId()) instanceof net.minecraft.world.entity.player.Player player) {
                var slots = fr.modeminecraft.item.tactical.TacticalSlots.of(player);
                slots.setStackInSlot(0, p.vest());
                slots.setStackInSlot(1, p.backpack());
            }
        });
    }
}

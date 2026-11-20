package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.tactical.BackpackItem;
import fr.modeminecraft.item.tactical.TacticalSlots;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Touche K : ouvrir le sac à dos porté. */
public record OpenBackpackPayload() implements CustomPacketPayload {
    public static final Type<OpenBackpackPayload> TYPE = new Type<>(ModeMinecraft.id("open_backpack"));
    public static final StreamCodec<ByteBuf, OpenBackpackPayload> STREAM_CODEC = StreamCodec.unit(new OpenBackpackPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenBackpackPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp && TacticalSlots.backpack(sp).getItem() instanceof BackpackItem) {
                BackpackItem.open(sp, () -> TacticalSlots.backpack(sp), -1);
            }
        });
    }
}

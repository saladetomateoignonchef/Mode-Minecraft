package fr.modeminecraft.nuclear;

import fr.modeminecraft.ModeMinecraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Flash d'une explosion nucléaire (serveur -> client) : écran blanc, intensité selon la distance. */
public record FlashPayload(double x, double y, double z, float strength) implements CustomPacketPayload {
    public static final Type<FlashPayload> TYPE = new Type<>(ModeMinecraft.id("nuclear_flash"));
    public static final StreamCodec<ByteBuf, FlashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, FlashPayload::x, ByteBufCodecs.DOUBLE, FlashPayload::y, ByteBufCodecs.DOUBLE, FlashPayload::z,
            ByteBufCodecs.FLOAT, FlashPayload::strength, FlashPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FlashPayload p, IPayloadContext context) {
        context.enqueueWork(() -> fr.modeminecraft.client.RadiationClient.flash(p));
    }
}

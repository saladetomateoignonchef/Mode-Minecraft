package fr.modeminecraft.nuclear;

import fr.modeminecraft.ModeMinecraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Zones radioactives actives (serveur -> client) : ciel, cendres, compteur Geiger. */
public record RadiationPayload(List<Entry> zones, long serverTime) implements CustomPacketPayload {
    public record Entry(int x, int y, int z, float radius, long expire) {
        public static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, Entry::x, ByteBufCodecs.INT, Entry::y, ByteBufCodecs.INT, Entry::z,
                ByteBufCodecs.FLOAT, Entry::radius, ByteBufCodecs.VAR_LONG, Entry::expire, Entry::new);
    }

    public static final Type<RadiationPayload> TYPE = new Type<>(ModeMinecraft.id("radiation_zones"));
    public static final StreamCodec<ByteBuf, RadiationPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.CODEC.apply(ByteBufCodecs.list(64)), RadiationPayload::zones, ByteBufCodecs.VAR_LONG, RadiationPayload::serverTime,
            RadiationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RadiationPayload p, IPayloadContext context) {
        context.enqueueWork(() -> fr.modeminecraft.client.RadiationClient.zones(p));
    }
}

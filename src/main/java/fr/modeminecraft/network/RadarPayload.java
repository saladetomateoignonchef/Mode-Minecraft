package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Contacts radar d'un S-400 / S-500 (envoyés à son opérateur toutes les 10 ticks). */
public record RadarPayload(int vehicle, boolean deployed, boolean sentinel, List<Blip> blips) implements CustomPacketPayload {
    /** Contact : identifiant (négatif = frappe balistique), position relative, altitude relative, nature. */
    public record Blip(int id, float dx, float dz, float alt, byte kind) {
        static final StreamCodec<ByteBuf, Blip> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Blip::id, ByteBufCodecs.FLOAT, Blip::dx, ByteBufCodecs.FLOAT, Blip::dz,
                ByteBufCodecs.FLOAT, Blip::alt, ByteBufCodecs.BYTE, Blip::kind, Blip::new);
    }

    public static final Type<RadarPayload> TYPE = new Type<>(ModeMinecraft.id("radar"));
    public static final StreamCodec<ByteBuf, RadarPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RadarPayload::vehicle, ByteBufCodecs.BOOL, RadarPayload::deployed, ByteBufCodecs.BOOL, RadarPayload::sentinel,
            Blip.CODEC.apply(ByteBufCodecs.list(64)), RadarPayload::blips, RadarPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RadarPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> fr.modeminecraft.client.FireControlScreen.radar(payload));
    }
}

package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> client : le joueur est dans une ville (variante, altitude des rues) ou n'y est plus (variante vide). */
public record CityAmbiencePayload(String variant, int baseY) implements CustomPacketPayload {
    public static final Type<CityAmbiencePayload> TYPE = new Type<>(ModeMinecraft.id("city_ambience"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CityAmbiencePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CityAmbiencePayload::variant, ByteBufCodecs.VAR_INT, CityAmbiencePayload::baseY, CityAmbiencePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CityAmbiencePayload p, IPayloadContext context) {
        context.enqueueWork(() -> fr.modeminecraft.client.CityAmbience.setCity(p.variant(), p.baseY()));
    }
}

package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Cinématiques du scénario (bandes noires, carte titre, sous-titres radio tapés à la machine, écran radar) :
 * - « intro » : briefing d'arrivée après le choix du camp ;
 * - « bomber » : décollage du bombardier nucléaire ennemi (radar : le bombardier et votre base) ;
 * - « intercepted » / « struck » : bombardier abattu, ou base frappée ;
 * - « final » : départ de la Frappe finale ; « defect » : passage à l'ennemi.
 */
public final class Cinematics {
    private Cinematics() {}

    public static void play(ServerPlayer p, String kind, int a, int b, int c, int d) {
        PacketDistributor.sendToPlayer(p, new Play(kind, List.of(a, b, c, d)));
    }

    public record Play(String kind, List<Integer> data) implements CustomPacketPayload {
        public static final Type<Play> TYPE = new Type<>(ModeMinecraft.id("cinematic"));
        public static final StreamCodec<ByteBuf, Play> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Play::kind,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), Play::data, Play::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Play p, IPayloadContext ctx) {
            ctx.enqueueWork(() -> fr.modeminecraft.client.CinematicOverlay.start(p.kind(), p.data()));
        }
    }
}

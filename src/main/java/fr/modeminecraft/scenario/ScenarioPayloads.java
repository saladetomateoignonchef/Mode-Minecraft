package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Paquets du scénario. */
public final class ScenarioPayloads {
    private ScenarioPayloads() {}

    /** Serveur -> client : ouvrir l'écran de choix du camp. */
    public record OpenChoice() implements CustomPacketPayload {
        public static final OpenChoice INSTANCE = new OpenChoice();
        public static final Type<OpenChoice> TYPE = new Type<>(ModeMinecraft.id("scenario_open_choice"));
        public static final StreamCodec<ByteBuf, OpenChoice> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(OpenChoice p, IPayloadContext ctx) {
            ctx.enqueueWork(fr.modeminecraft.client.FactionSelectionScreen::open);
        }
    }

    /** Client -> serveur : camp choisi. */
    public record Choose(String side) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(ModeMinecraft.id("scenario_choose"));
        public static final StreamCodec<ByteBuf, Choose> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Choose::side, Choose::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Choose p, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    Scenario.choose(sp, Side.byId(p.side()));
                }
            });
        }
    }

    /** Serveur -> client : camp et réputation du joueur. */
    public record Sync(String side, int reputation) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ModeMinecraft.id("scenario_sync"));
        public static final StreamCodec<ByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Sync::side,
                ByteBufCodecs.VAR_INT, Sync::reputation, Sync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Sync p, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                ScenarioClientState.side = Side.byId(p.side());
                ScenarioClientState.reputation = p.reputation();
            });
        }
    }

    /** Serveur -> client : écran de victoire (camp, réputation, jours de guerre). */
    public record VictoryScreen(String side, int reputation, int days) implements CustomPacketPayload {
        public static final Type<VictoryScreen> TYPE = new Type<>(ModeMinecraft.id("scenario_victory"));
        public static final StreamCodec<ByteBuf, VictoryScreen> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, VictoryScreen::side,
                ByteBufCodecs.VAR_INT, VictoryScreen::reputation, ByteBufCodecs.VAR_INT, VictoryScreen::days, VictoryScreen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(VictoryScreen p, IPayloadContext ctx) {
            ctx.enqueueWork(() -> fr.modeminecraft.client.VictoryScreenUi.open(p.side(), p.reputation(), p.days()));
        }
    }
}

package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.entity.bot.Robbery;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Le joueur a tenu un civil en joue (clic droit maintenu 1,5 s, à moins de 3,5 blocs) : braquage. */
public record RobPayload(int entity) implements CustomPacketPayload {
    public static final Type<RobPayload> TYPE = new Type<>(ModeMinecraft.id("rob"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RobPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(RobPayload::new, RobPayload::entity).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RobPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(payload.entity()) instanceof HumanBotEntity bot) {
                Robbery.rob(player.serverLevel(), player, bot);
            }
        });
    }
}

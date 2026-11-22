package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.train.MesaTrainCarEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Commandes de la cabine de la locomotive : sifflet (H) et portes (O). */
public record TrainControlPayload(int action) implements CustomPacketPayload {
    public static final int WHISTLE = 0, DOORS = 1;
    public static final Type<TrainControlPayload> TYPE = new Type<>(ModeMinecraft.id("train_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrainControlPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(TrainControlPayload::new, TrainControlPayload::action).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TrainControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p && p.getVehicle() instanceof MesaTrainCarEntity car) {
                MesaTrainCarEntity loco = car.leader();
                if (loco == null) {
                    return;
                }
                switch (payload.action()) {
                    case WHISTLE -> loco.honk(p);
                    case DOORS -> loco.toggleDoors(p);
                    default -> {}
                }
            }
        });
    }
}

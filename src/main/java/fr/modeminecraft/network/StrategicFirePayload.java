package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.entity.military.StrategicSystems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Console de tir : 0 tir sol-air sur des contacts, 1 sentinelle on/off (S-500), 2 frappe Iskander sur (x, z). */
public record StrategicFirePayload(int action, List<Integer> targets, int ammo, int x, int z) implements CustomPacketPayload {
    public static final int FIRE_SAM = 0, TOGGLE_SENTINEL = 1, FIRE_STRIKE = 2;
    public static final Type<StrategicFirePayload> TYPE = new Type<>(ModeMinecraft.id("strategic_fire"));
    public static final StreamCodec<ByteBuf, StrategicFirePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StrategicFirePayload::action, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)), StrategicFirePayload::targets,
            ByteBufCodecs.VAR_INT, StrategicFirePayload::ammo, ByteBufCodecs.INT, StrategicFirePayload::x, ByteBufCodecs.INT, StrategicFirePayload::z,
            StrategicFirePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StrategicFirePayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp) || !(sp.getVehicle() instanceof MilVehicleEntity v)) {
                return;
            }
            switch (p.action()) {
                case FIRE_SAM -> StrategicSystems.fireSam(v, sp, p.targets().stream().mapToInt(Integer::intValue).toArray(), p.ammo());
                case TOGGLE_SENTINEL -> {
                    if (v.seatOf(sp) == 1) {
                        v.sentinel = !v.sentinel;
                        v.ownerId = sp.getUUID();
                    }
                }
                case FIRE_STRIKE -> StrategicSystems.fireIskander(v, sp, p.x(), p.z(), p.ammo());
                default -> {}
            }
        });
    }
}

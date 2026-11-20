package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.drone.DroneEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> serveur : commandes du pilote (20 fois par seconde). buttons : 1 = tir, 2 = quitter (retour auto). */
public record DroneControlPayload(int droneId, float forward, float strafe, float vertical, float yaw, float pitch, byte buttons)
        implements CustomPacketPayload {
    public static final Type<DroneControlPayload> TYPE = new Type<>(ModeMinecraft.id("drone_control"));
    public static final StreamCodec<ByteBuf, DroneControlPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeInt(p.droneId);
                buf.writeFloat(p.forward);
                buf.writeFloat(p.strafe);
                buf.writeFloat(p.vertical);
                buf.writeFloat(p.yaw);
                buf.writeFloat(p.pitch);
                buf.writeByte(p.buttons);
            },
            buf -> new DroneControlPayload(buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DroneControlPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp && sp.level().getEntity(p.droneId) instanceof DroneEntity drone) {
                drone.applyInput(sp, p.forward, p.strafe, p.vertical, p.yaw, p.pitch, (p.buttons & 1) != 0, (p.buttons & 2) != 0);
            }
        });
    }
}

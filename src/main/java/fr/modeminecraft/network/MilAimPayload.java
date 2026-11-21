package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Point du monde sous le réticule du tireur d'un véhicule (lancer de rayon depuis la caméra, 300 blocs). */
public record MilAimPayload(double x, double y, double z) implements CustomPacketPayload {
    public static final Type<MilAimPayload> TYPE = new Type<>(ModeMinecraft.id("mil_aim"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MilAimPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, MilAimPayload::x, ByteBufCodecs.DOUBLE, MilAimPayload::y, ByteBufCodecs.DOUBLE, MilAimPayload::z, MilAimPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MilAimPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Vec3 p = new Vec3(payload.x(), payload.y(), payload.z());
            if (context.player() instanceof ServerPlayer sp && sp.getVehicle() instanceof MilVehicleEntity v && Double.isFinite(p.x + p.y + p.z)
                    && p.distanceToSqr(sp.position()) < 400 * 400) {
                v.setAim(sp, p);
            }
        });
    }
}

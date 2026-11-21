package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.RocketLauncherEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Lance-roquettes statique : pointage (écran), tir d'une roquette ou salve (Espace, bouton de l'écran). */
public record LauncherPayload(int entity, int action, float azimuth, float elevation) implements CustomPacketPayload {
    public static final int AIM = 0, FIRE_ONE = 1, SALVO = 2;
    public static final Type<LauncherPayload> TYPE = new Type<>(ModeMinecraft.id("rocket_launcher"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LauncherPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LauncherPayload::entity, ByteBufCodecs.VAR_INT, LauncherPayload::action,
            ByteBufCodecs.FLOAT, LauncherPayload::azimuth, ByteBufCodecs.FLOAT, LauncherPayload::elevation, LauncherPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LauncherPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp) || !(sp.level().getEntity(p.entity()) instanceof RocketLauncherEntity l)
                    || l.distanceToSqr(sp) > 8 * 8) {
                return;
            }
            switch (p.action()) {
                case AIM -> {
                    if (!l.isVehicle() && Float.isFinite(p.azimuth()) && Float.isFinite(p.elevation())) {
                        l.setAim(p.azimuth(), p.elevation());
                    }
                }
                case FIRE_ONE -> l.command(sp, false);
                case SALVO -> l.command(sp, true);
                default -> {}
            }
        });
    }
}

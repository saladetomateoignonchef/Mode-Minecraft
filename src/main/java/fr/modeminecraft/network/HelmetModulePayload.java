package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.HelmetItem;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Touche N : abaisser / relever la visière blindée ou les jumelles de vision nocturne du casque porté. */
public record HelmetModulePayload() implements CustomPacketPayload {
    public static final HelmetModulePayload INSTANCE = new HelmetModulePayload();
    public static final Type<HelmetModulePayload> TYPE = new Type<>(ModeMinecraft.id("helmet_module"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HelmetModulePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HelmetModulePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var helm = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!(helm.getItem() instanceof HelmetItem h) || h.stats().module() == HelmetItem.Module.NONE) {
                return;
            }
            long now = player.level().getGameTime();
            if (now - helm.getOrDefault(ModDataComponents.HELMET_MODULE_TIME.get(), -100L) < 10) {
                return; // le bras n'a pas fini son geste
            }
            boolean on = !HelmetItem.isModuleActive(helm);
            helm.set(ModDataComponents.HELMET_MODULE.get(), on);
            helm.set(ModDataComponents.HELMET_MODULE_TIME.get(), now);
            var sound = h.stats().module() == HelmetItem.Module.NVG && on ? ModSounds.NVG_ON.get() : ModSounds.HELMET_VISOR.get();
            player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS, 0.9f,
                    on ? 1.0f : 1.15f);
        });
    }
}

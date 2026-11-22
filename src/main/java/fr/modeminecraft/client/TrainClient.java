package fr.modeminecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.train.MesaTrainCarEntity;
import fr.modeminecraft.network.TrainControlPayload;
import fr.modeminecraft.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Côté client du train : H = sifflet et O = portes quand on est au poste de conduite, et caméra qui tremble pendant
 * l'étourdissement électrique d'un contrôleur.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class TrainClient {
    private TrainClient() {}

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getAction() != InputConstants.PRESS || mc.player == null || mc.screen != null
                || !(mc.player.getVehicle() instanceof MesaTrainCarEntity car) || !car.isLoco()) {
            return;
        }
        if (ModKeyMappings.SIREN.matches(event.getKey(), event.getScanCode())) {
            PacketDistributor.sendToServer(new TrainControlPayload(TrainControlPayload.WHISTLE));
        } else if (ModKeyMappings.BOMB_BAY.matches(event.getKey(), event.getScanCode())) {
            PacketDistributor.sendToServer(new TrainControlPayload(TrainControlPayload.DOORS));
        }
    }

    /** Étourdi : secousses de la caméra (roulis, tangage, lacet). */
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.hasEffect(ModEffects.STUNNED)) {
            return;
        }
        var r = mc.player.getRandom();
        event.setRoll(event.getRoll() + (r.nextFloat() - 0.5f) * 4f);
        event.setPitch(event.getPitch() + (r.nextFloat() - 0.5f) * 2f);
        event.setYaw(event.getYaw() + (r.nextFloat() - 0.5f) * 2f);
    }
}

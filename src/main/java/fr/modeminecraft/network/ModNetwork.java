package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Enregistre les paquets réseau du mod. */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class ModNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(ReloadPayload.TYPE, ReloadPayload.STREAM_CODEC, ReloadPayload::handle)
                .playToServer(GiveItemPayload.TYPE, GiveItemPayload.STREAM_CODEC, GiveItemPayload::handle)
                .playToServer(PlaceRecipePayload.TYPE, PlaceRecipePayload.STREAM_CODEC, PlaceRecipePayload::handle)
                .playToServer(HelmetModulePayload.TYPE, HelmetModulePayload.STREAM_CODEC, HelmetModulePayload::handle)
                .playToServer(QuickAttachPayload.TYPE, QuickAttachPayload.STREAM_CODEC, QuickAttachPayload::handle)
                .playToServer(FireModePayload.TYPE, FireModePayload.STREAM_CODEC, FireModePayload::handle)
                .playToServer(CheckChamberPayload.TYPE, CheckChamberPayload.STREAM_CODEC, CheckChamberPayload::handle)
                .playToServer(PronePayload.TYPE, PronePayload.STREAM_CODEC, PronePayload::handle)
                .playToServer(AmmoTypePayload.TYPE, AmmoTypePayload.STREAM_CODEC, AmmoTypePayload::handle)
                .playToServer(UnderbarrelPayload.TYPE, UnderbarrelPayload.STREAM_CODEC, UnderbarrelPayload::handle)
                .playToServer(LaserPayload.TYPE, LaserPayload.STREAM_CODEC, LaserPayload::handle)
                .playToServer(LeanPayload.TYPE, LeanPayload.STREAM_CODEC, LeanPayload::handle)
                .playToServer(TriggerPayload.TYPE, TriggerPayload.STREAM_CODEC, TriggerPayload::handle)
                .playToServer(MeleePayload.TYPE, MeleePayload.STREAM_CODEC, MeleePayload::handle)
                .playToServer(LockPayload.TYPE, LockPayload.STREAM_CODEC, LockPayload::handle)
                .playToClient(LeanSyncPayload.TYPE, LeanSyncPayload.STREAM_CODEC, LeanSyncPayload::handle)
                .playToClient(ShotFxPayload.TYPE, ShotFxPayload.STREAM_CODEC, ShotFxPayload::handle)
                .playToClient(HitMarkerPayload.TYPE, HitMarkerPayload.STREAM_CODEC, HitMarkerPayload::handle)
                .playToClient(ImpactPayload.TYPE, ImpactPayload.STREAM_CODEC, ImpactPayload::handle)
                .playToClient(BackGunsPayload.TYPE, BackGunsPayload.STREAM_CODEC, BackGunsPayload::handle)
                .playToClient(FlashPayload.TYPE, FlashPayload.STREAM_CODEC, FlashPayload::handle);
    }

    private ModNetwork() {}
}

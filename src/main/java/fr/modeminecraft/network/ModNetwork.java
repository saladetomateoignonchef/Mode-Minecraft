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
                .playToServer(DroneControlPayload.TYPE, DroneControlPayload.STREAM_CODEC, DroneControlPayload::handle)
                .playToClient(DronePilotPayload.TYPE, DronePilotPayload.STREAM_CODEC, DronePilotPayload::handle)
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
                .playToServer(FlashlightPayload.TYPE, FlashlightPayload.STREAM_CODEC, FlashlightPayload::handle)
                .playToServer(StrategicFirePayload.TYPE, StrategicFirePayload.STREAM_CODEC, StrategicFirePayload::handle)
                .playToClient(RadarPayload.TYPE, RadarPayload.STREAM_CODEC, RadarPayload::handle)
                .playToServer(MilControlPayload.TYPE, MilControlPayload.STREAM_CODEC, MilControlPayload::handle)
                .playToServer(MilAimPayload.TYPE, MilAimPayload.STREAM_CODEC, MilAimPayload::handle)
                .playToServer(LauncherPayload.TYPE, LauncherPayload.STREAM_CODEC, LauncherPayload::handle)
                .playToClient(fr.modeminecraft.nuclear.RadiationPayload.TYPE, fr.modeminecraft.nuclear.RadiationPayload.STREAM_CODEC, fr.modeminecraft.nuclear.RadiationPayload::handle)
                .playToClient(fr.modeminecraft.nuclear.FlashPayload.TYPE, fr.modeminecraft.nuclear.FlashPayload.STREAM_CODEC, fr.modeminecraft.nuclear.FlashPayload::handle)
                .playToServer(RobPayload.TYPE, RobPayload.STREAM_CODEC, RobPayload::handle)
                .playToServer(LeanPayload.TYPE, LeanPayload.STREAM_CODEC, LeanPayload::handle)
                .playToServer(TriggerPayload.TYPE, TriggerPayload.STREAM_CODEC, TriggerPayload::handle)
                .playToServer(MeleePayload.TYPE, MeleePayload.STREAM_CODEC, MeleePayload::handle)
                .playToServer(LockPayload.TYPE, LockPayload.STREAM_CODEC, LockPayload::handle)
                .playToClient(LeanSyncPayload.TYPE, LeanSyncPayload.STREAM_CODEC, LeanSyncPayload::handle)
                .playToClient(fr.modeminecraft.scenario.ScenarioPayloads.OpenChoice.TYPE, fr.modeminecraft.scenario.ScenarioPayloads.OpenChoice.STREAM_CODEC,
                        fr.modeminecraft.scenario.ScenarioPayloads.OpenChoice::handle)
                .playToServer(fr.modeminecraft.scenario.ScenarioPayloads.Choose.TYPE, fr.modeminecraft.scenario.ScenarioPayloads.Choose.STREAM_CODEC,
                        fr.modeminecraft.scenario.ScenarioPayloads.Choose::handle)
                .playToClient(fr.modeminecraft.scenario.ScenarioPayloads.Sync.TYPE, fr.modeminecraft.scenario.ScenarioPayloads.Sync.STREAM_CODEC,
                        fr.modeminecraft.scenario.ScenarioPayloads.Sync::handle)
                .playToClient(fr.modeminecraft.scenario.ScenarioPayloads.VictoryScreen.TYPE, fr.modeminecraft.scenario.ScenarioPayloads.VictoryScreen.STREAM_CODEC,
                        fr.modeminecraft.scenario.ScenarioPayloads.VictoryScreen::handle)
                .playToClient(fr.modeminecraft.scenario.MissionNet.State.TYPE, fr.modeminecraft.scenario.MissionNet.State.STREAM_CODEC,
                        fr.modeminecraft.scenario.MissionNet.State::handle)
                .playToServer(fr.modeminecraft.scenario.MissionNet.Action.TYPE, fr.modeminecraft.scenario.MissionNet.Action.STREAM_CODEC,
                        fr.modeminecraft.scenario.MissionNet.Action::handle)
                .playToClient(fr.modeminecraft.scenario.Armory.Open.TYPE, fr.modeminecraft.scenario.Armory.Open.STREAM_CODEC,
                        fr.modeminecraft.scenario.Armory.Open::handle)
                .playToServer(fr.modeminecraft.scenario.Armory.Act.TYPE, fr.modeminecraft.scenario.Armory.Act.STREAM_CODEC,
                        fr.modeminecraft.scenario.Armory.Act::handle)
                .playToServer(fr.modeminecraft.scenario.Gadgets.Use.TYPE, fr.modeminecraft.scenario.Gadgets.Use.STREAM_CODEC,
                        fr.modeminecraft.scenario.Gadgets.Use::handle)
                .playToClient(fr.modeminecraft.scenario.Gadgets.Open.TYPE, fr.modeminecraft.scenario.Gadgets.Open.STREAM_CODEC,
                        fr.modeminecraft.scenario.Gadgets.Open::handle)
                .playToClient(fr.modeminecraft.scenario.Cinematics.Play.TYPE, fr.modeminecraft.scenario.Cinematics.Play.STREAM_CODEC,
                        fr.modeminecraft.scenario.Cinematics.Play::handle)
                .playToClient(fr.modeminecraft.scenario.Prisoners.Open.TYPE, fr.modeminecraft.scenario.Prisoners.Open.STREAM_CODEC,
                        fr.modeminecraft.scenario.Prisoners.Open::handle)
                .playToServer(fr.modeminecraft.scenario.Prisoners.Act.TYPE, fr.modeminecraft.scenario.Prisoners.Act.STREAM_CODEC,
                        fr.modeminecraft.scenario.Prisoners.Act::handle)
                .playToClient(fr.modeminecraft.scenario.StrategicMapItem.MapData.TYPE, fr.modeminecraft.scenario.StrategicMapItem.MapData.STREAM_CODEC,
                        fr.modeminecraft.scenario.StrategicMapItem.MapData::handle)
                .playToClient(ShotFxPayload.TYPE, ShotFxPayload.STREAM_CODEC, ShotFxPayload::handle)
                .playToClient(CityAmbiencePayload.TYPE, CityAmbiencePayload.STREAM_CODEC, CityAmbiencePayload::handle)
                .playToClient(HitMarkerPayload.TYPE, HitMarkerPayload.STREAM_CODEC, HitMarkerPayload::handle)
                .playToClient(ImpactPayload.TYPE, ImpactPayload.STREAM_CODEC, ImpactPayload::handle)
                .playToClient(BackGunsPayload.TYPE, BackGunsPayload.STREAM_CODEC, BackGunsPayload::handle)
                .playToClient(TacticalSyncPayload.TYPE, TacticalSyncPayload.STREAM_CODEC, TacticalSyncPayload::handle)
                .playToServer(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC, OpenBackpackPayload::handle)
                .playToClient(FlashPayload.TYPE, FlashPayload.STREAM_CODEC, FlashPayload::handle)
                .playToClient(DazzlePayload.TYPE, DazzlePayload.STREAM_CODEC, DazzlePayload::handle);
    }

    private ModNetwork() {}
}

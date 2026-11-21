package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sons du mod (fichiers générés par tools/generate_sounds.py, voir assets/modeminecraft/sounds.json). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ModeMinecraft.MODID);

    // Tirs, par famille d'arme
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_PISTOL = register("shot.pistol");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_MAGNUM = register("shot.magnum");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_SMG = register("shot.smg");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_RIFLE = register("shot.rifle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_AK = register("shot.ak");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_BATTLE = register("shot.battle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_SNIPER = register("shot.sniper");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_HEAVY = register("shot.heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_SHOTGUN = register("shot.shotgun");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_MINIGUN = register("shot.minigun");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_SUPPRESSED = register("shot.suppressed");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_SUPPRESSED_HEAVY = register("shot.suppressed_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_DISTANT = register("shot.distant");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_ROCKET = register("shot.rocket");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT_LAUNCHER = register("shot.launcher");
    // Mécanique
    public static final DeferredHolder<SoundEvent, SoundEvent> MAG_OUT = register("gun.mag_out");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAG_IN = register("gun.mag_in");
    public static final DeferredHolder<SoundEvent, SoundEvent> RACK = register("gun.rack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT = register("gun.bolt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PUMP = register("gun.pump");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_INSERT = register("gun.shell_insert");
    public static final DeferredHolder<SoundEvent, SoundEvent> EMPTY_CLICK = register("gun.empty_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEAR_EQUIP_CLOTH = register("gun.gear_equip_cloth");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOOTS_LACE = register("gun.boots_lace");
    public static final DeferredHolder<SoundEvent, SoundEvent> RANGER_STEP = register("gun.ranger_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> VEST_EQUIP = register("gun.vest_equip_velcro");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLATE_INSERT = register("gun.plate_insert");
    public static final DeferredHolder<SoundEvent, SoundEvent> VEST_BULLET_HIT = register("gun.vest_bullet_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLATE_SHATTER = register("gun.plate_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELMET_STRAP = register("gun.helmet_strap_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELMET_RICOCHET = register("gun.helmet_ricochet");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELMET_VISOR = register("gun.helmet_visor");
    public static final DeferredHolder<SoundEvent, SoundEvent> NVG_ON = register("gun.nvg_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELMET_BREATH = register("gun.helmet_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_DEPLOY = register("gun.turret_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_RELOAD = register("gun.turret_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_DRY_FIRE = register("gun.dry_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> CASING = register("gun.casing");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_CASING = register("gun.shell_casing");
    public static final DeferredHolder<SoundEvent, SoundEvent> HITMARKER = register("gun.hitmarker");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEADSHOT = register("gun.headshot");
    public static final DeferredHolder<SoundEvent, SoundEvent> GARAND_PING = register("gun.garand_ping");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCK_SEARCH = register("gun.lock_search");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCK_TONE = register("gun.lock_tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_PIN = register("gun.grenade_pin");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_BOUNCE = register("gun.grenade_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_BEEP = register("gun.grenade_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONAR = register("gun.sonar");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASH_RING = register("gun.flash_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASHLIGHT_CLICK = register("gun.flashlight_switch_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> POLICE_FREEZE_WARNING = register("bot.police_freeze_warning");
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_HORN_CHARGE = register("bot.falcon_horn_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRAGONSLAYER_UNSHEATHE = register("bot.dragonslayer_unsheathe");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRAGONSLAYER_SHEATHE = register("bot.dragonslayer_sheathe");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRAGONSLAYER_CLANG = register("bot.dragonslayer_clang");
    public static final DeferredHolder<SoundEvent, SoundEvent> BERSERK_ROAR = register("bot.berserk_roar_clang");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRANSFORMER_SHORT = register("bot.transformer_short_circuit");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_PAIRED = register("bot.drone_paired");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_MOTOR = register("bot.drone_motor");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_ROTOR_LOOP = register("bot.drone_rotor_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> FPV_BUZZ = register("bot.fpv_incoming_buzz");
    public static final DeferredHolder<SoundEvent, SoundEvent> EMP_JAMMER = register("bot.emp_jammer");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_JAMMED = register("bot.drone_jammed");
    public static final DeferredHolder<SoundEvent, SoundEvent> VEHICLE_ENGINE = register("bot.vehicle_engine");

    // ---- mise à jour 0.3 : police, prison, ville vivante
    /** Sirène de police : entendue à 80 blocs. */
    public static final DeferredHolder<SoundEvent, SoundEvent> POLICE_SIREN = SOUNDS.register("bot.police_siren",
            () -> SoundEvent.createFixedRangeEvent(ModeMinecraft.id("bot.police_siren"), 80));
    public static final DeferredHolder<SoundEvent, SoundEvent> CROWBAR_PRY = register("city.crowbar_pry");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAIL_DOOR_SLIDE = register("city.jail_door_slide");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAIL_BUZZER = register("city.jail_buzzer");
    public static final DeferredHolder<SoundEvent, SoundEvent> CASH_REGISTER_BELL = register("city.cash_register_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> CIVILIAN_SCREAM = register("city.civilian_scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAR_HORN = register("city.car_horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUS_BRAKES = register("city.bus_brakes");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRAFFIC_LOOP = register("city.traffic_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_LOOP = register("city.wind_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MARKET_LOOP = register("city.market_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAFE_MUSIC = register("city.cafe_music");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALOON_PIANO = register("city.saloon_piano");
    public static final DeferredHolder<SoundEvent, SoundEvent> STREET_GUITAR = register("city.street_guitar");
    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_CRACKLE = register("city.radio_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOUDSPEAKER = register("city.loudspeaker");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOG_BARK = register("city.dog_bark");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIGEON_COO = register("city.pigeon_coo");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIGEON_FLAP = register("city.pigeon_flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRICKETS_LOOP = register("city.crickets_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAIN_TIN_LOOP = register("city.rain_tin_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRANSFORMER_HUM = register("city.transformer_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPRAY_PAINT = register("city.spray_paint");

    // ----- mise à jour 0.4 : véhicules militaires et missiles
    public static final DeferredHolder<SoundEvent, SoundEvent> TANK_CANNON = register("mil.tank_cannon");
    public static final DeferredHolder<SoundEvent, SoundEvent> AUTOCANNON = register("mil.autocannon");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_LAUNCH = register("mil.rocket_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_LAUNCH = register("mil.missile_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> VEHICLE_MG_LIGHT = register("mil.mg_light");
    public static final DeferredHolder<SoundEvent, SoundEvent> VEHICLE_MG_HEAVY = register("mil.mg_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> DIESEL_LOOP = register("mil.diesel_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRACKS_LOOP = register("mil.tracks_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURBINE_LOOP = register("mil.turbine_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROTOR_LOOP = register("mil.rotor_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUGE_EXPLOSION = register("mil.huge_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIR_RAID_ALARM = register("mil.air_raid_alarm");
    // ----- mise à jour 0.5 : appel radio, chambres fortes des boss
    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_DISPATCH_CALL = register("mil.radio_dispatch_call");
    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_CALL_CUT = register("mil.radio_call_cut");
    public static final DeferredHolder<SoundEvent, SoundEvent> VAULT_DOOR_OPEN = register("mil.vault_door_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIMARS_HYDRAULICS = register("mil.himars_hydraulics");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIRCRAFT_CRASH_ALARM = register("mil.aircraft_crash_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOMB_BAY_RELEASE = register("mil.bomb_bay_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOMB_BAY_DOORS = register("mil.bomb_bay_doors");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOMB_WHISTLE = register("mil.bomb_whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEAR_DETONATION = register("mil.nuclear_detonation");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEAR_RUMBLE = register("mil.nuclear_detonation_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_CLICK = register("mil.geiger_clicks");
    public static final DeferredHolder<SoundEvent, SoundEvent> PROP_ENGINE_LOOP = register("mil.prop_engine_loop");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ModeMinecraft.id(name)));
    }

    private ModSounds() {}
}

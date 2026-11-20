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

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ModeMinecraft.id(name)));
    }

    private ModSounds() {}
}

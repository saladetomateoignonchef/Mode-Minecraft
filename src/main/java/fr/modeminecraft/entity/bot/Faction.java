package fr.modeminecraft.entity.bot;

import java.util.List;

/**
 * Factions humaines. Chaque faction a son attitude envers les joueurs, sa banque de skins sur mesure
 * (textures/entity/bot/<id>_<n>.png) et son équipement par rôle (identifiants d'objets du mod).
 */
public enum Faction {
    //          id               attitude                 skins  groupe      couleur
    POLICE("police", Attitude.WARN, 6, "police", 0x3A6AD0,
            new Loadout(List.of("glock17", "beretta_m9", "glock17", "stun_baton"), List.of("glock17"), List.of(), List.of("pasgt_vest"), List.of(), List.of(), null)),
    SWAT("swat", Attitude.WARN, 4, "police", 0x2A2E36,
            new Loadout(List.of("mp5", "m4", "mp5"), List.of("m4"), List.of("mich_2000"), List.of("swat_vest"), List.of("swat_pants"), List.of("salomon_forces_boots"), "flashbang")),
    MILITIA("militia", Attitude.HOSTILE, 6, "militia", 0xC2A060,
            new Loadout(List.of("ak47", "ak74u", "ak47", "rpg7"), List.of("dragunov"), List.of("ssh40", "pasgt"), List.of("vest_6b3"), List.of(), List.of("m65_combat_boots"), "frag_grenade")),
    REBEL("rebel", Attitude.PROVOKED, 5, "rebel", 0x6A7A3A,
            new Loadout(List.of("kar98k", "fn_fal", "remington_870"), List.of("kar98k"), List.of(), List.of(), List.of("bdu_woodland_pants"), List.of("m65_combat_boots"), "molotov")),
    GARRISON("garrison", Attitude.HOSTILE, 5, "garrison", 0x4B5A2A,
            new Loadout(List.of("hk416", "m4", "m249"), List.of("m24"), List.of("ops_core_fast"), List.of("iotv_vest"), List.of("crye_g3_pants"), List.of("lowa_zephyr_boots"), "frag_grenade")),
    SOVIET("soviet", Attitude.HOSTILE_INDOORS, 5, "soviet", 0x8A2A2A,
            new Loadout(List.of("ak74u", "pp19_bizon", "ak74u"), List.of("dragunov"), List.of("altyn"), List.of("vest_6b3"), List.of("ratnik_pants"), List.of(), "frag_grenade")),
    ARCTIC("arctic", Attitude.HOSTILE, 4, "arctic", 0xE0E8F0,
            new Loadout(List.of("p90", "vector"), List.of("awm"), List.of("ops_core_fast"), List.of("avs_plate_carrier"), List.of(), List.of("salomon_forces_boots"), "emp_grenade")),
    MINE_SECURITY("mine_security", Attitude.WARN, 4, "mine_security", 0xE07A20,
            new Loadout(List.of("glock17", "remington_870"), List.of("remington_870"), List.of(), List.of("pasgt_vest"), List.of(), List.of(), null)),
    CARTEL("cartel", Attitude.HOSTILE, 6, "cartel", 0x2A9A6A,
            new Loadout(List.of("scar_h", "galil_ace", "spas12", "desert_eagle"), List.of("scar_h"), List.of(), List.of("jpc_plate_carrier"), List.of(), List.of(), "frag_grenade")),
    GANG("gang", Attitude.ARMED_HOSTILE, 5, "gang", 0xC03030,
            new Loadout(List.of("uzi", "mac10", "machete"), List.of("uzi"), List.of(), List.of(), List.of(), List.of(), "molotov")),
    SHERIFF("sheriff", Attitude.WARN, 5, "sheriff", 0xC0A040,
            new Loadout(List.of("colt_python", "colt_saa", "kar98k"), List.of("kar98k"), List.of(), List.of(), List.of(), List.of(), null)),
    REFINERY("refinery", Attitude.HOSTILE, 4, "refinery", 0x6A4A2A,
            new Loadout(List.of("m16a4"), List.of("barrett_m82"), List.of("mich_2000"), List.of("avs_plate_carrier"), List.of(), List.of(), "frag_grenade")),
    CIVILIAN("civilian", Attitude.PASSIVE, 8, "civilian", 0xA0A0A0, Loadout.NONE),
    TRADER("trader", Attitude.PASSIVE, 4, "trader", 0x5A3A6A, Loadout.NONE),
    FALCON("falcon", Attitude.HOSTILE_ALL, 6, "falcon", 0xF0F0F0, Loadout.NONE);

    /** Comportement envers un joueur. */
    public enum Attitude {
        PASSIVE,          // ne combat jamais (fuit)
        PROVOKED,         // neutre tant qu'on ne l'attaque pas (ni ne touche à ses coffres)
        WARN,             // sommation de 3,5 s si on a une arme en main, puis hostile
        ARMED_HOSTILE,    // neutre, mais hostile dès qu'on sort une arme
        HOSTILE,          // ouvre le feu à vue
        HOSTILE_INDOORS,  // hostile dans ses bâtiments (sous un toit), sommation dehors
        HOSTILE_ALL       // attaque à vue joueurs ET autres factions
    }

    /** Équipement : armes (tirées au hasard), arme du sniper, casques, gilets, pantalons, rangers, grenade (ou null). */
    public record Loadout(List<String> weapons, List<String> sniper, List<String> helmets, List<String> vests,
                          List<String> pants, List<String> boots, String grenade) {
        static final Loadout NONE = new Loadout(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null);
    }

    public final String id;
    public final Attitude attitude;
    public final int skins;
    /** Les factions d'un même groupe partagent leur hostilité (police + SWAT). */
    public final String group;
    public final int color;
    public final Loadout loadout;

    Faction(String id, Attitude attitude, int skins, String group, int color, Loadout loadout) {
        this.id = id;
        this.attitude = attitude;
        this.skins = skins;
        this.group = group;
        this.color = color;
        this.loadout = loadout;
    }

    /**
     * Factions qui ne se supportent pas (elles s'ouvrent le feu dessus à vue) : forces de l'ordre (police, SWAT,
     * shérifs) contre milices, cartels et gangs ; garnison américaine / OTAN contre forces soviétiques.
     * Les Faucons Blancs, eux, attaquent tout le monde (géré à part).
     */
    public boolean enemyOf(Faction o) {
        return law() && o.outlaw() || o.law() && outlaw()
                || this == GARRISON && o == SOVIET || this == SOVIET && o == GARRISON;
    }

    private boolean law() {
        return this == POLICE || this == SWAT || this == SHERIFF;
    }

    private boolean outlaw() {
        return this == MILITIA || this == CARTEL || this == GANG;
    }

    public boolean combatant() {
        return attitude != Attitude.PASSIVE;
    }

    /** Portée de vue (blocs) : 45, 90 pour les snipers. */
    public double sightRange(BotRole role) {
        return role == BotRole.SNIPER ? 90 : this == FALCON ? 40 : 45;
    }

    public static Faction byId(String id) {
        for (Faction f : values()) {
            if (f.id.equals(id)) {
                return f;
            }
        }
        return CIVILIAN;
    }
}

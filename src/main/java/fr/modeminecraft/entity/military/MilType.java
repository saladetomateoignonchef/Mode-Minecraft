package fr.modeminecraft.entity.military;

import java.util.List;

/**
 * Catalogue des véhicules militaires (cahier des charges 0.4) : caractéristiques, places et armes.
 * La géométrie (pivots, places, bouches de tir) vient de {@link MilGeometry} (générée avec les modèles 3D).
 */
public enum MilType {
    // ------------------------------------------------------------ soviétiques / russes
    UAZ_469("uaz_469", Side.SOVIET, Chassis.WHEELS, 85, 45, 1f, 1f, 1f, 78, 18, Flag.NONE,
            List.of(seat(Role.DRIVER, false), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false)),
            List.of()),
    TIGR("tigr", Side.SOVIET, Chassis.WHEELS, 80, 130, 0.55f, 0.65f, 0.75f, 138, 27, Flag.BULLETPROOF,
            List.of(seat(Role.DRIVER, true), seat(Role.PASSENGER, true), seat(Role.GUNNER, false, 0), seat(Role.PASSENGER, true)),
            List.of(w("kord", Kind.MG, 0, 3, -12, 50, 900, 7, MilAmmo.MG_127, MilAmmo.MG_762))),
    KAMAZ("kamaz", Side.SOVIET, Chassis.WHEELS, 75, 95, 0.9f, 0.9f, 0.9f, 210, 54, Flag.NONE,
            List.of(seat(Role.DRIVER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false),
                    seat(Role.PASSENGER, false), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false),
                    seat(Role.PASSENGER, false), seat(Role.PASSENGER, false)),
            List.of()),
    BRDM_2("brdm_2", Side.SOVIET, Chassis.WHEELS, 80, 150, 0.45f, 0.55f, 0.7f, 290, 0, Flag.AMPHIBIOUS | Flag.BULLETPROOF | Flag.SMOKE,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0, 1), seat(Role.COMMANDER, true)),
            List.of(w("kpvt", Kind.MG, 0, 3, -5, 30, 500, 4, MilAmmo.MG_145), w("pkt", Kind.MG, 1, 2, -5, 30, 2000, 5, MilAmmo.MG_762))),
    BTR_80("btr_80", Side.SOVIET, Chassis.WHEELS, 75, 210, 0.4f, 0.5f, 0.65f, 300, 0, Flag.AMPHIBIOUS | Flag.BULLETPROOF | Flag.SMOKE,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0, 1), seat(Role.COMMANDER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true)),
            List.of(w("kpvt", Kind.MG, 0, 3, -4, 60, 500, 4, MilAmmo.MG_145), w("pkt", Kind.MG, 1, 2, -4, 60, 2000, 5, MilAmmo.MG_762))),
    BMP_2("bmp_2", Side.SOVIET, Chassis.TRACKS, 65, 270, 0.32f, 0.5f, 0.6f, 460, 0, Flag.AMPHIBIOUS | Flag.BULLETPROOF | Flag.SMOKE,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0, 1), seat(Role.COMMANDER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true)),
            List.of(w("2a42", Kind.AUTOCANNON, 0, 4, -5, 74, 500, 3.5f, MilAmmo.AP_I, MilAmmo.HE_I),
                    w("konkurs", Kind.ATGM, 1, 120, -5, 15, 4, 3.5f, MilAmmo.ATGM))),
    BM_21_GRAD("bm_21_grad", Side.SOVIET, Chassis.WHEELS, 75, 100, 0.9f, 0.9f, 0.9f, 360, 0, Flag.NONE,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0)),
            List.of(w("grad", Kind.ROCKETS, 0, 10, 0, 55, 40, 1.5f, MilAmmo.ROCKET_122))),
    ZSU_23_4("zsu_23_4", Side.SOVIET, Chassis.TRACKS, 50, 200, 0.45f, 0.55f, 0.65f, 515, 0, Flag.BULLETPROOF | Flag.RADAR,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0), seat(Role.COMMANDER, true)),
            List.of(w("azp_23", Kind.AUTOCANNON, 0, 1, -4, 85, 2000, 6, MilAmmo.HE_I, MilAmmo.AP_I))),
    MI_24("mi_24", Side.SOVIET, Chassis.HELI, 145, 320, 0.5f, 0.55f, 0.6f, 600, 0, Flag.BULLETPROOF,
            List.of(seat(Role.PILOT, true, 1, 2), seat(Role.GUNNER, true, 0), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true)),
            List.of(w("yakb", Kind.MG, 0, 2, -60, 15, 1500, 8, MilAmmo.MG_127),
                    w("s8", Kind.ROCKETS, 1, 4, -20, 5, 40, 0, MilAmmo.S8),
                    w("shturm", Kind.ATGM, 3, 80, -25, 10, 4, 0, MilAmmo.ATGM))),
    T_90M("t_90m", Side.SOVIET, Chassis.TRACKS, 60, 540, 0.15f, 0.35f, 0.6f, 1600, 0, Flag.BULLETPROOF | Flag.SMOKE | Flag.SHTORA | Flag.HEAVY,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0, 1), seat(Role.COMMANDER, true, 2)),
            List.of(w("2a46m5", Kind.CANNON, 0, 140, -6, 14, 40, 2, MilAmmo.APFSDS, MilAmmo.HE, MilAmmo.HEAT, MilAmmo.REFLEKS),
                    w("pkt", Kind.MG, 0, 2, -6, 14, 2000, 2, MilAmmo.MG_762),
                    free("kord", Kind.MG, 1, 3, -10, 50, 500, MilAmmo.MG_127))),
    T_80BVM("t_80bvm", Side.SOVIET, Chassis.TRACKS, 75, 500, 0.17f, 0.38f, 0.62f, 1100, 0,
            Flag.BULLETPROOF | Flag.SMOKE | Flag.HEAVY | Flag.FAST_REVERSE | Flag.SNOW,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0, 1), seat(Role.COMMANDER, true, 2)),
            List.of(w("2a46m4", Kind.CANNON, 0, 140, -5, 14, 38, 2.2f, MilAmmo.APFSDS, MilAmmo.HE, MilAmmo.HEAT, MilAmmo.REFLEKS),
                    w("pkt", Kind.MG, 0, 2, -5, 14, 2000, 2.2f, MilAmmo.MG_762),
                    free("nsvt", Kind.MG, 1, 3, -10, 50, 500, MilAmmo.MG_127))),
    T_14_ARMATA("t_14_armata", Side.SOVIET, Chassis.TRACKS, 75, 650, 0.12f, 0.3f, 0.55f, 1600, 0,
            Flag.BULLETPROOF | Flag.SMOKE | Flag.HEAVY | Flag.APS,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0, 1), seat(Role.COMMANDER, true)),
            List.of(w("2a82", Kind.CANNON, 0, 110, -5, 20, 45, 2.5f, MilAmmo.APFSDS, MilAmmo.HE, MilAmmo.HEAT, MilAmmo.REFLEKS),
                    w("2a42_coax", Kind.AUTOCANNON, 1, 4, -5, 20, 300, 2.5f, MilAmmo.AP_I, MilAmmo.HE_I))),
    BMPT_TERMINATOR("bmpt_terminator", Side.SOVIET, Chassis.TRACKS, 60, 500, 0.18f, 0.35f, 0.55f, 1600, 0,
            Flag.BULLETPROOF | Flag.SMOKE | Flag.HEAVY,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0, 1), seat(Role.COMMANDER, true), seat(Role.GRENADIER, true, 2),
                    seat(Role.GRENADIER, true, 3)),
            List.of(w("2a42_twin", Kind.AUTOCANNON, 0, 3, -5, 45, 850, 3, MilAmmo.AP_I, MilAmmo.HE_I),
                    w("ataka", Kind.ATGM, 2, 100, -5, 20, 4, 3, MilAmmo.ATGM),
                    fixed("ag17d_l", Kind.GRENADE, 4, 5, -5, 25, 300, MilAmmo.GRENADE_40),
                    fixed("ag17d_r", Kind.GRENADE, 4, 5, -5, 25, 300, MilAmmo.GRENADE_40))),
    SPRUT_SD("sprut_sd", Side.SOVIET, Chassis.TRACKS, 70, 320, 0.35f, 0.5f, 0.65f, 400, 0,
            Flag.BULLETPROOF | Flag.SMOKE | Flag.AMPHIBIOUS,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0, 1), seat(Role.COMMANDER, true)),
            List.of(w("2a75", Kind.CANNON, 0, 140, -5, 15, 40, 2.5f, MilAmmo.APFSDS, MilAmmo.HE, MilAmmo.HEAT, MilAmmo.REFLEKS),
                    w("pkt", Kind.MG, 0, 2, -5, 15, 2000, 2.5f, MilAmmo.MG_762))),
    // ------------------------------------------------------------ américains
    HUMVEE("humvee", Side.US, Chassis.WHEELS, 80, 120, 0.65f, 0.75f, 0.85f, 95, 18, Flag.BULLETPROOF,
            List.of(seat(Role.DRIVER, true), seat(Role.PASSENGER, true), seat(Role.GUNNER, false, 0, 1), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true)),
            List.of(w("m2", Kind.MG, 0, 3, -12, 50, 800, 7, MilAmmo.MG_127), w("mk19", Kind.GRENADE, 0, 6, -12, 40, 128, 7, MilAmmo.GRENADE_40))),
    M_ATV("m_atv", Side.US, Chassis.WHEELS, 80, 170, 0.5f, 0.55f, 0.65f, 300, 18, Flag.BULLETPROOF | Flag.MINE_PROOF,
            List.of(seat(Role.DRIVER, true), seat(Role.PASSENGER, true), seat(Role.GUNNER, false, 0), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true)),
            List.of(w("m2", Kind.MG, 0, 3, -12, 50, 800, 7, MilAmmo.MG_127))),
    M939("m939", Side.US, Chassis.WHEELS, 75, 95, 0.9f, 0.9f, 0.9f, 300, 54, Flag.NONE,
            List.of(seat(Role.DRIVER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false),
                    seat(Role.PASSENGER, false), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false),
                    seat(Role.PASSENGER, false), seat(Role.PASSENGER, false)),
            List.of()),
    STRYKER("stryker", Side.US, Chassis.WHEELS, 80, 240, 0.4f, 0.5f, 0.6f, 200, 0, Flag.BULLETPROOF | Flag.SMOKE,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true)),
            List.of(w("m2_rws", Kind.MG, 0, 3, -20, 60, 800, 6, MilAmmo.MG_127))),
    M113("m113", Side.US, Chassis.TRACKS, 60, 180, 0.5f, 0.55f, 0.65f, 360, 0, Flag.BULLETPROOF | Flag.AMPHIBIOUS,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, false, 0), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true)),
            List.of(w("m2", Kind.MG, 0, 3, -10, 50, 800, 7, MilAmmo.MG_127))),
    M2A3_BRADLEY("m2a3_bradley", Side.US, Chassis.TRACKS, 60, 310, 0.3f, 0.45f, 0.6f, 660, 0, Flag.BULLETPROOF | Flag.SMOKE,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0, 1), seat(Role.COMMANDER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true)),
            List.of(w("m242", Kind.AUTOCANNON, 0, 5, -9, 59, 900, 3.5f, MilAmmo.AP_I, MilAmmo.HE_I),
                    w("tow", Kind.ATGM, 1, 120, -9, 20, 7, 3.5f, MilAmmo.ATGM))),
    M1A2_ABRAMS("m1a2_abrams", Side.US, Chassis.TRACKS, 65, 640, 0.1f, 0.3f, 0.55f, 1900, 0, Flag.BULLETPROOF | Flag.SMOKE | Flag.HEAVY,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0, 1), seat(Role.COMMANDER, true, 2), seat(Role.PASSENGER, true)),
            List.of(w("m256", Kind.CANNON, 0, 120, -10, 20, 42, 2.2f, MilAmmo.APFSDS, MilAmmo.HE, MilAmmo.HEAT),
                    w("m240_coax", Kind.MG, 0, 2, -10, 20, 2000, 2.2f, MilAmmo.MG_762),
                    free("m2_cupola", Kind.MG, 1, 3, -10, 60, 900, MilAmmo.MG_127))),
    M142_HIMARS("m142_himars", Side.US, Chassis.WHEELS, 75, 110, 0.8f, 0.8f, 0.8f, 300, 0, Flag.NONE,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0), seat(Role.PASSENGER, true)),
            List.of(w("m270_pod", Kind.ROCKETS, 0, 40, 0, 60, 6, 1.2f, MilAmmo.ROCKET_227, MilAmmo.ATACMS))),
    AVENGER("avenger", Side.US, Chassis.WHEELS, 80, 100, 0.75f, 0.8f, 0.85f, 95, 0, Flag.NONE,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, true, 0, 1)),
            List.of(w("stinger_pods", Kind.SAM, 0, 30, -10, 70, 8, 6, MilAmmo.SAM), w("m3p", Kind.MG, 2, 2, -10, 70, 600, 6, MilAmmo.MG_127))),
    UH_60_BLACK_HAWK("uh_60_black_hawk", Side.US, Chassis.HELI, 150, 270, 0.6f, 0.65f, 0.7f, 1360, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true), seat(Role.DOOR_GUNNER, false, 0), seat(Role.DOOR_GUNNER, false, 1), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, false), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false),
                    seat(Role.PASSENGER, false), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false)),
            List.of(free("m134_l", Kind.MG, 0, 1, -60, 30, 3000, MilAmmo.MINIGUN), free("m134_r", Kind.MG, 1, 1, -60, 30, 3000, MilAmmo.MINIGUN))),
    AH_64E_APACHE("ah_64e_apache", Side.US, Chassis.HELI, 160, 290, 0.45f, 0.5f, 0.55f, 1400, 0, Flag.BULLETPROOF,
            List.of(seat(Role.PILOT, true, 1), seat(Role.GUNNER, true, 0, 2)),
            List.of(w("m230", Kind.AUTOCANNON, 0, 3, -60, 11, 1200, 10, MilAmmo.AP_I, MilAmmo.HE_I),
                    w("hydra", Kind.ROCKETS, 1, 4, -20, 5, 38, 0, MilAmmo.HYDRA),
                    w("hellfire", Kind.ATGM, 3, 60, -40, 10, 8, 0, MilAmmo.ATGM))),
    // ------------------------------------------------------------ complexes lance-missiles (armes : voir StrategicSystems)
    S_400("s_400", Side.SOVIET, Chassis.WHEELS, 60, 160, 0.8f, 0.8f, 0.8f, 500, 0, Flag.STRATEGIC,
            List.of(seat(Role.DRIVER, true), seat(Role.OPERATOR, true)),
            List.of(w("s400_tubes", Kind.SAM, 0, 20, 0, 90, 4, 0, MilAmmo.S400_48N6, MilAmmo.S400_9M96))),
    S_500("s_500", Side.SOVIET, Chassis.WHEELS, 55, 180, 0.8f, 0.8f, 0.8f, 550, 0, Flag.STRATEGIC,
            List.of(seat(Role.DRIVER, true), seat(Role.OPERATOR, true)),
            List.of(w("s500_tubes", Kind.SAM, 0, 30, 0, 90, 2, 0, MilAmmo.S500_77N6, MilAmmo.S500_40N6))),
    ISKANDER("iskander", Side.SOVIET, Chassis.WHEELS, 60, 170, 0.8f, 0.8f, 0.8f, 500, 0, Flag.STRATEGIC,
            List.of(seat(Role.DRIVER, true), seat(Role.OPERATOR, true)),
            List.of(w("9m723", Kind.ROCKETS, 0, 200, 0, 85, 2, 0, MilAmmo.ISK_HE, MilAmmo.ISK_CLUSTER, MilAmmo.ISK_EMP))),
    // ------------------------------------------------------------ bombardier stratégique (soute : voir MilVehicleEntity.bombBay)
    B29_SUPERFORTRESS("b29_superfortress", Side.US, Chassis.PLANE, 210, 950, 0.7f, 0.7f, 0.75f, 1800, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true), seat(Role.BOMBARDIER, true), seat(Role.GUNNER, true, 0), seat(Role.GUNNER, true, 1),
                    seat(Role.GUNNER, true, 2), seat(Role.GUNNER, true, 3)),
            List.of(free("m2_dorsal_front", Kind.MG, 0, 2, -8, 85, 1000, MilAmmo.MG_127),
                    free("m2_dorsal_rear", Kind.MG, 1, 2, -8, 85, 1000, MilAmmo.MG_127),
                    free("m2_ventral", Kind.MG, 2, 2, -85, 8, 1000, MilAmmo.MG_127),
                    free("m2_tail", Kind.MG, 3, 2, -40, 40, 1000, MilAmmo.MG_127)));

    public enum Side { SOVIET, US }

    /** HELI : hélicoptère ; PLANE : avion à voilure fixe (vitesse de sustentation, piste, train d'atterrissage). */
    public enum Chassis { WHEELS, TRACKS, HELI, PLANE }

    /** Rôle d'une place. */
    public enum Role { DRIVER, PILOT, GUNNER, COMMANDER, PASSENGER, DOOR_GUNNER, GRENADIER, OPERATOR, BOMBARDIER }

    public enum Kind { MG, AUTOCANNON, CANNON, GRENADE, ROCKETS, ATGM, SAM }

    /** Options (masque de bits). */
    public static final class Flag {
        public static final int NONE = 0, AMPHIBIOUS = 1, BULLETPROOF = 2, SMOKE = 4, SHTORA = 8, APS = 16, HEAVY = 32,
                FAST_REVERSE = 64, SNOW = 128, MINE_PROOF = 256, RADAR = 512, STRATEGIC = 1024;
    }

    /** Une place : rôle, protégée (habitacle blindé) ou exposée, arme du clic gauche et du clic droit (-1 = aucune). */
    public record Seat(Role role, boolean enclosed, int primary, int secondary) {}

    /**
     * Une arme : bouche de tir (indice dans la géométrie), cadence (ticks entre deux coups), élévation min / max,
     * contenance des soutes, vitesse de pointage de la tourelle (°/tick ; 0 = arme fixe dans l'axe du véhicule),
     * visée libre (suit le regard du tireur, ex. mitrailleuse de tourelleau), munitions acceptées.
     */
    public record Weapon(String key, Kind kind, int muzzle, int cooldown, float minPitch, float maxPitch, int capacity, float traverse,
                         boolean free, List<MilAmmo> ammo) {}

    public final String id;
    public final Side side;
    public final Chassis chassis;
    public final int kmh, fuel, trunk, flags;
    public final float health, armorFront, armorSide, armorRear;
    public final List<Seat> seats;
    public final List<Weapon> weapons;

    MilType(String id, Side side, Chassis chassis, int kmh, float health, float front, float sideArmor, float rear, int fuel, int trunk, int flags,
            List<Seat> seats, List<Weapon> weapons) {
        this.id = id;
        this.side = side;
        this.chassis = chassis;
        this.kmh = kmh;
        this.health = health;
        this.armorFront = front;
        this.armorSide = sideArmor;
        this.armorRear = rear;
        this.fuel = fuel;
        this.trunk = trunk;
        this.flags = flags;
        this.seats = seats;
        this.weapons = weapons;
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    /** Vitesse maximale en blocs / tick (85 km/h ≈ 0,94). */
    public float maxSpeed() {
        return kmh * (chassis == Chassis.HELI || chassis == Chassis.PLANE ? 0.0095f : 0.0111f);
    }

    public MilGeometry.Geo geo() {
        return MilGeometry.get(id);
    }

    public boolean tracked() {
        return chassis == Chassis.TRACKS;
    }

    /** Aéronef (hélicoptère ou avion) : vol, crash en vol, cible des radars. */
    public boolean heli() {
        return chassis == Chassis.HELI || chassis == Chassis.PLANE;
    }

    /** Avion à voilure fixe (B-29). */
    public boolean plane() {
        return chassis == Chassis.PLANE;
    }

    /** Pas de blindage digne de ce nom (camions, 4x4 non blindés) : le moteur avant est fragile, les roues aussi. */
    public boolean soft() {
        return armorFront >= 0.8f;
    }

    public static MilType byId(String id) {
        for (MilType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    private static Seat seat(Role role, boolean enclosed) {
        return new Seat(role, enclosed, -1, -1);
    }

    private static Seat seat(Role role, boolean enclosed, int primary) {
        return new Seat(role, enclosed, primary, -1);
    }

    private static Seat seat(Role role, boolean enclosed, int primary, int secondary) {
        return new Seat(role, enclosed, primary, secondary);
    }

    private static Weapon w(String key, Kind kind, int muzzle, int cooldown, float minPitch, float maxPitch, int capacity, float traverse,
                            MilAmmo... ammo) {
        return new Weapon(key, kind, muzzle, cooldown, minPitch, maxPitch, capacity, traverse, false, List.of(ammo));
    }

    /** Arme à visée libre (tourelleau, sabord) : elle tire là où regarde son servant. */
    private static Weapon free(String key, Kind kind, int muzzle, int cooldown, float minPitch, float maxPitch, int capacity, MilAmmo... ammo) {
        return new Weapon(key, kind, muzzle, cooldown, minPitch, maxPitch, capacity, 0, true, List.of(ammo));
    }

    /** Arme fixe dans la caisse (lance-grenades de caisse du BMPT) : visée libre dans un cône vers l'avant. */
    private static Weapon fixed(String key, Kind kind, int muzzle, int cooldown, float minPitch, float maxPitch, int capacity, MilAmmo... ammo) {
        return new Weapon(key, kind, muzzle, cooldown, minPitch, maxPitch, capacity, 0, true, List.of(ammo));
    }
}

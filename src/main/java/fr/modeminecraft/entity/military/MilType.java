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
                    free("m2_tail", Kind.MG, 3, 2, -40, 40, 1000, MilAmmo.MG_127))),
    // ------------------------------------------------------------ aviation (cahier des charges « Aviation militaire »)
    // pilote : clic droit = canon de bord, clic gauche = station d'armes choisie avec R (roquettes, missiles, bombes)
    IL_2("il2_sturmovik", Side.SOVIET, Chassis.PLANE, 230, 650, 0.5f, 0.6f, 0.7f, 900, 0, Flag.BULLETPROOF,
            List.of(seat(Role.PILOT, true, 1, 0)),
            List.of(gun("vya23", Kind.AUTOCANNON, 0, 3, 1500, MilAmmo.HE_I, MilAmmo.AP_I), gun("rs82", Kind.ROCKETS, 1, 6, 16, MilAmmo.S8))),
    LA_7("la7", Side.SOVIET, Chassis.PLANE, 270, 380, 0.85f, 0.85f, 0.85f, 600, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 0, 0)),
            List.of(gun("b20", Kind.AUTOCANNON, 0, 2, 1800, MilAmmo.HE_I, MilAmmo.AP_I))),
    MIG_15("mig15", Side.SOVIET, Chassis.PLANE, 330, 420, 0.8f, 0.8f, 0.8f, 700, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 1, 0)),
            List.of(gun("nr23", Kind.AUTOCANNON, 0, 3, 1200, MilAmmo.HE_I, MilAmmo.AP_I), gun("n37", Kind.AUTOCANNON, 0, 9, 400, MilAmmo.HE_I))),
    TU_95("tu95_bear", Side.SOVIET, Chassis.PLANE, 260, 1300, 0.7f, 0.7f, 0.75f, 3000, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 0), seat(Role.BOMBARDIER, true), seat(Role.GUNNER, true, 1), seat(Role.GUNNER, true, 2)),
            List.of(gun("kh55", Kind.CRUISE, 3, 60, 6, MilAmmo.CRUISE),
                    free("am23_tail", Kind.AUTOCANNON, 7, 3, -40, 40, 1200, MilAmmo.HE_I),
                    free("am23_dorsal", Kind.AUTOCANNON, 8, 3, -8, 85, 1200, MilAmmo.HE_I))),
    MIG_21("mig21", Side.SOVIET, Chassis.PLANE, 400, 340, 0.85f, 0.85f, 0.85f, 600, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 1, 0)),
            List.of(gun("gsh23", Kind.AUTOCANNON, 0, 2, 1200, MilAmmo.HE_I, MilAmmo.AP_I), gun("r60", Kind.AAM, 3, 15, 6, MilAmmo.AAM_IR),
                    gun("s5", Kind.ROCKETS, 1, 4, 32, MilAmmo.S8))),
    MIG_25("mig25", Side.SOVIET, Chassis.PLANE, 430, 450, 0.8f, 0.8f, 0.8f, 1400, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 0)),
            List.of(gun("r40", Kind.AAM, 3, 25, 6, MilAmmo.AAM_RADAR))),
    SU_25("su25", Side.SOVIET, Chassis.PLANE, 320, 800, 0.45f, 0.55f, 0.65f, 800, 0, Flag.BULLETPROOF,
            List.of(seat(Role.PILOT, true, 1, 0)),
            List.of(gun("gsh302", Kind.AUTOCANNON, 0, 2, 2000, MilAmmo.AP_I, MilAmmo.HE_I), gun("s8", Kind.ROCKETS, 1, 3, 80, MilAmmo.S8),
                    gun("kh25", Kind.AGM, 3, 30, 4, MilAmmo.AGM))),
    MIG_29("mig29", Side.SOVIET, Chassis.PLANE, 420, 420, 0.8f, 0.8f, 0.8f, 900, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 1, 0)),
            List.of(gun("gsh301", Kind.AUTOCANNON, 0, 2, 1500, MilAmmo.HE_I, MilAmmo.AP_I), gun("r73", Kind.AAM, 3, 15, 4, MilAmmo.AAM_IR),
                    gun("r27", Kind.AAM, 5, 20, 4, MilAmmo.AAM_RADAR))),
    SU_34("su34", Side.SOVIET, Chassis.PLANE, 380, 700, 0.6f, 0.65f, 0.7f, 1600, 0, Flag.BULLETPROOF,
            List.of(seat(Role.PILOT, true, 1, 0), seat(Role.BOMBARDIER, true)),
            List.of(gun("gsh301", Kind.AUTOCANNON, 0, 2, 1800, MilAmmo.HE_I, MilAmmo.AP_I), gun("kh29", Kind.AGM, 3, 25, 3, MilAmmo.AGM),
                    gun("r77", Kind.AAM, 5, 20, 3, MilAmmo.AAM_RADAR))),
    SU_57("su57", Side.SOVIET, Chassis.PLANE, 440, 520, 0.75f, 0.75f, 0.75f, 1200, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 1, 0)),
            List.of(gun("gsh301", Kind.AUTOCANNON, 0, 2, 1500, MilAmmo.HE_I, MilAmmo.AP_I), gun("r77m", Kind.AAM, 3, 15, 4, MilAmmo.AAM_RADAR),
                    gun("kh59", Kind.AGM, 5, 25, 4, MilAmmo.AGM))),
    P_51("p51_mustang", Side.US, Chassis.PLANE, 280, 380, 0.85f, 0.85f, 0.85f, 900, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 1, 0)),
            List.of(gun("m2_wing", Kind.MG, 0, 1, 2400, MilAmmo.MG_127), gun("hvar", Kind.ROCKETS, 1, 6, 10, MilAmmo.HYDRA))),
    F_86("f86_sabre", Side.US, Chassis.PLANE, 330, 400, 0.8f, 0.8f, 0.8f, 700, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 1, 0)),
            List.of(gun("m3", Kind.MG, 0, 1, 2400, MilAmmo.MG_127), gun("hvar", Kind.ROCKETS, 1, 5, 8, MilAmmo.HYDRA))),
    F_4E("f4_phantom", Side.US, Chassis.PLANE, 400, 600, 0.75f, 0.75f, 0.8f, 1400, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 1, 0), seat(Role.BOMBARDIER, true)),
            List.of(gun("m61", Kind.AUTOCANNON, 0, 1, 1800, MilAmmo.HE_I, MilAmmo.AP_I), gun("aim9", Kind.AAM, 3, 15, 4, MilAmmo.AAM_IR),
                    gun("aim7", Kind.AAM, 5, 20, 4, MilAmmo.AAM_RADAR))),
    C_130("c130_hercules", Side.US, Chassis.PLANE, 240, 1100, 0.75f, 0.75f, 0.75f, 2500, 27, Flag.NONE,
            List.of(seat(Role.PILOT, true), seat(Role.PILOT, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true), seat(Role.PASSENGER, true),
                    seat(Role.PASSENGER, true), seat(Role.PASSENGER, true)),
            List.of()),
    A_10("a10_thunderbolt", Side.US, Chassis.PLANE, 300, 1100, 0.4f, 0.5f, 0.6f, 1200, 0, Flag.BULLETPROOF,
            List.of(seat(Role.PILOT, true, 1, 0)),
            List.of(gun("gau8", Kind.AUTOCANNON, 0, 1, 3000, MilAmmo.AP_I, MilAmmo.HE_I), gun("hydra", Kind.ROCKETS, 1, 3, 76, MilAmmo.HYDRA),
                    gun("agm65", Kind.AGM, 3, 25, 6, MilAmmo.AGM))),
    F_117("f117_nighthawk", Side.US, Chassis.PLANE, 340, 420, 0.8f, 0.8f, 0.8f, 1000, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 0)),
            List.of(gun("agm88", Kind.ARM, 3, 30, 2, MilAmmo.ARM), gun("agm65", Kind.AGM, 5, 30, 2, MilAmmo.AGM))),
    F_15E("f15e_strike_eagle", Side.US, Chassis.PLANE, 420, 620, 0.75f, 0.75f, 0.8f, 1600, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 1, 0), seat(Role.BOMBARDIER, true)),
            List.of(gun("m61", Kind.AUTOCANNON, 0, 1, 1800, MilAmmo.HE_I, MilAmmo.AP_I), gun("aim120", Kind.AAM, 3, 18, 4, MilAmmo.AAM_RADAR),
                    gun("aim9x", Kind.AAM, 5, 15, 4, MilAmmo.AAM_IR))),
    F_16C("f16_falcon", Side.US, Chassis.PLANE, 420, 420, 0.8f, 0.8f, 0.8f, 1000, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 1, 0)),
            List.of(gun("m61", Kind.AUTOCANNON, 0, 1, 1500, MilAmmo.HE_I, MilAmmo.AP_I),
                    gun("aim_mix", Kind.AAM, 3, 15, 6, MilAmmo.AAM_IR, MilAmmo.AAM_RADAR), gun("hydra", Kind.ROCKETS, 1, 3, 38, MilAmmo.HYDRA))),
    B_2("b2_spirit", Side.US, Chassis.PLANE, 300, 1300, 0.75f, 0.75f, 0.75f, 3000, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true), seat(Role.BOMBARDIER, true)),
            List.of()),
    F_22("f22_raptor", Side.US, Chassis.PLANE, 440, 520, 0.75f, 0.75f, 0.75f, 1200, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true, 1, 0)),
            List.of(gun("m61a2", Kind.AUTOCANNON, 0, 1, 1500, MilAmmo.HE_I, MilAmmo.AP_I), gun("aim120d", Kind.AAM, 3, 15, 6, MilAmmo.AAM_RADAR),
                    gun("aim9x", Kind.AAM, 5, 15, 2, MilAmmo.AAM_IR))),
    // ------------------------------------------------------------ marine (cahier des charges « Guerre navale »)
    // places : 0 commandant (barre), 1 canonnier (tourelle principale), 2 officier d'armes (stations : R, clic G)
    KIROV("kirov_cruiser", Side.SOVIET, Chassis.SHIP, 50, 4000, 0.4f, 0.5f, 0.55f, 6000, 54, Flag.RADAR,
            ship(Role.GUNNER, 6), List.of(naval("ak130", 0, 25, 200, MilAmmo.NAVAL_SHELL), auto("kashtan", Kind.CIWS, 1, 3000, MilAmmo.CIWS_ROUNDS),
                    gun("granit", Kind.NAVAL_MISSILE, 2, 40, 20, MilAmmo.ASHM), auto("s300f", Kind.SAM, 7, 96, MilAmmo.NAVAL_SAM),
                    gun("usetk80", Kind.TORPEDO, 5, 60, 10, MilAmmo.TORPEDO), gun("rbu6000", Kind.ASW, 8, 20, 24, MilAmmo.ASW_ROCKET))),
    GORSHKOV("admiral_gorshkov", Side.SOVIET, Chassis.SHIP, 55, 2200, 0.5f, 0.6f, 0.65f, 4000, 27, Flag.RADAR,
            ship(Role.GUNNER, 4), List.of(naval("a192", 0, 25, 200, MilAmmo.NAVAL_SHELL), auto("duet", Kind.CIWS, 1, 2000, MilAmmo.CIWS_ROUNDS),
                    gun("ukks", Kind.NAVAL_MISSILE, 4, 40, 16, MilAmmo.HYPERSONIC, MilAmmo.ASHM, MilAmmo.CRUISE),
                    auto("redut", Kind.SAM, 7, 32, MilAmmo.NAVAL_SAM))),
    SLAVA("slava_cruiser", Side.SOVIET, Chassis.SHIP, 50, 3200, 0.45f, 0.55f, 0.6f, 5000, 36, Flag.RADAR,
            ship(Role.GUNNER, 5), List.of(naval("ak130", 0, 25, 200, MilAmmo.NAVAL_SHELL), auto("ak630", Kind.CIWS, 1, 3000, MilAmmo.CIWS_ROUNDS),
                    gun("vulkan", Kind.NAVAL_MISSILE, 2, 40, 16, MilAmmo.ASHM), auto("s300f", Kind.SAM, 7, 64, MilAmmo.NAVAL_SAM),
                    gun("rbu6000", Kind.ASW, 8, 20, 24, MilAmmo.ASW_ROCKET))),
    UDALOY("udaloy_destroyer", Side.SOVIET, Chassis.SHIP, 55, 2600, 0.5f, 0.6f, 0.65f, 4500, 27, Flag.RADAR,
            ship(Role.GUNNER, 4), List.of(naval("ak130", 0, 25, 200, MilAmmo.NAVAL_SHELL), auto("ak630", Kind.CIWS, 1, 2000, MilAmmo.CIWS_ROUNDS),
                    gun("moskit", Kind.NAVAL_MISSILE, 2, 40, 8, MilAmmo.ASHM, MilAmmo.CRUISE), gun("set65", Kind.TORPEDO, 5, 60, 8, MilAmmo.TORPEDO),
                    gun("rbu6000", Kind.ASW, 8, 20, 24, MilAmmo.ASW_ROCKET), auto("kinzhal", Kind.SAM, 7, 24, MilAmmo.NAVAL_SAM))),
    BUYAN("buyan_corvette", Side.SOVIET, Chassis.SHIP, 60, 1200, 0.6f, 0.7f, 0.75f, 2000, 9, Flag.NONE,
            ship(Role.GUNNER, 2), List.of(naval("a190", 0, 20, 150, MilAmmo.NAVAL_SHELL), auto("duet", Kind.CIWS, 1, 2000, MilAmmo.CIWS_ROUNDS),
                    gun("kalibr", Kind.NAVAL_MISSILE, 4, 50, 8, MilAmmo.CRUISE))),
    ARLEIGH_BURKE("arleigh_burke", Side.US, Chassis.SHIP, 58, 2600, 0.5f, 0.6f, 0.65f, 4500, 27, Flag.RADAR,
            ship(Role.GUNNER, 4), List.of(naval("mk45", 0, 22, 200, MilAmmo.NAVAL_SHELL), auto("phalanx", Kind.CIWS, 1, 3000, MilAmmo.CIWS_ROUNDS),
                    gun("mk41", Kind.NAVAL_MISSILE, 4, 40, 56, MilAmmo.CRUISE, MilAmmo.ASHM), auto("sm6", Kind.SAM, 7, 40, MilAmmo.NAVAL_SAM),
                    gun("mk32", Kind.TORPEDO, 5, 60, 6, MilAmmo.TORPEDO), gun("asroc", Kind.ASW, 8, 30, 8, MilAmmo.ASW_ROCKET))),
    TICONDEROGA("ticonderoga_cruiser", Side.US, Chassis.SHIP, 55, 3000, 0.45f, 0.55f, 0.6f, 5000, 36, Flag.RADAR,
            ship(Role.GUNNER, 5), List.of(naval("mk45", 0, 22, 200, MilAmmo.NAVAL_SHELL), auto("phalanx", Kind.CIWS, 1, 3000, MilAmmo.CIWS_ROUNDS),
                    gun("mk41", Kind.NAVAL_MISSILE, 4, 40, 61, MilAmmo.CRUISE), gun("harpoon", Kind.NAVAL_MISSILE, 2, 40, 8, MilAmmo.ASHM),
                    auto("sm2", Kind.SAM, 7, 61, MilAmmo.NAVAL_SAM))),
    ZUMWALT("zumwalt_destroyer", Side.US, Chassis.SHIP, 55, 2800, 0.45f, 0.55f, 0.6f, 4800, 27, Flag.NONE,
            ship(Role.GUNNER, 4), List.of(naval("ags", 0, 30, 150, MilAmmo.NAVAL_SHELL), auto("mk46", Kind.CIWS, 1, 2000, MilAmmo.CIWS_ROUNDS),
                    gun("mk57", Kind.NAVAL_MISSILE, 4, 40, 80, MilAmmo.HYPERSONIC, MilAmmo.CRUISE))),
    OHIO("ohio_submarine", Side.US, Chassis.SUB, 40, 2400, 0.5f, 0.55f, 0.6f, 9000, 18, Flag.NONE,
            sub(5), List.of(gun("tomahawk", Kind.NAVAL_MISSILE, 4, 40, 154, MilAmmo.CRUISE), gun("mk48", Kind.TORPEDO, 5, 50, 24, MilAmmo.TORPEDO))),
    VIRGINIA("virginia_submarine", Side.US, Chassis.SUB, 50, 1800, 0.55f, 0.6f, 0.65f, 9000, 9, Flag.NONE,
            sub(3), List.of(gun("tomahawk", Kind.NAVAL_MISSILE, 4, 40, 12, MilAmmo.CRUISE), gun("mk48", Kind.TORPEDO, 5, 40, 26, MilAmmo.TORPEDO))),
    RHIB("mercenary_rhib", Side.MERC, Chassis.SHIP, 95, 120, 0.9f, 0.9f, 0.9f, 300, 0, Flag.NONE,
            List.of(seat(Role.DRIVER, false), seat(Role.GUNNER, false, 0), seat(Role.PASSENGER, false), seat(Role.PASSENGER, false),
                    seat(Role.PASSENGER, false)),
            List.of(free("m240b", Kind.MG, 0, 2, -20, 30, 1000, MilAmmo.MG_762))),
    INTERCEPTOR_BOAT("mercenary_interceptor_boat", Side.MERC, Chassis.SHIP, 85, 250, 0.7f, 0.75f, 0.8f, 400, 0, Flag.BULLETPROOF,
            List.of(seat(Role.DRIVER, true), seat(Role.GUNNER, false, 0), seat(Role.GUNNER, false, 1)),
            List.of(free("m621", Kind.AUTOCANNON, 0, 3, -15, 40, 500, MilAmmo.HE_I, MilAmmo.AP_I),
                    free("mrl8", Kind.ROCKETS, 1, 6, -5, 40, 8, MilAmmo.S8))),
    EXPLOSIVE_USV("mercenary_explosive_usv", Side.MERC, Chassis.SHIP, 90, 60, 0.9f, 0.9f, 0.9f, 200, 0, Flag.NONE,
            List.of(seat(Role.DRIVER, false)), List.of()),
    // ------------------------------------------------------------ civil (à la fin : l'ordre des constantes est enregistré dans les mondes)
    /** Avion de tourisme biplace, sans aucun armement : décollage court, vol souple, très peu de carburant consommé. */
    LIGHT_CIVIL_PLANE("light_civil_plane", Side.CIVIL, Chassis.PLANE, 115, 160, 0.15f, 0.15f, 0.15f, 220, 0, Flag.NONE,
            List.of(seat(Role.PILOT, true), seat(Role.PASSENGER, true)), List.of());

    public enum Side { SOVIET, US, MERC, CIVIL }

    /** HELI : hélicoptère ; PLANE : avion à voilure fixe (vitesse de sustentation, piste, train d'atterrissage). */
    public enum Chassis { WHEELS, TRACKS, HELI, PLANE, SHIP, SUB }

    /** Rôle d'une place. */
    public enum Role { DRIVER, PILOT, GUNNER, COMMANDER, PASSENGER, DOOR_GUNNER, GRENADIER, OPERATOR, BOMBARDIER, WEAPONS_OFFICER }

    /** AAM : missile air-air (verrouillage d'un aéronef) ; AGM : air-sol (véhicule verrouillé ou point visé) ;
     *  ARM : anti-radar (radar ennemi le plus proche devant l'avion) ; CRUISE : missile de croisière (point visé, GPS). */
    public enum Kind { MG, AUTOCANNON, CANNON, GRENADE, ROCKETS, ATGM, SAM, AAM, AGM, ARM, CRUISE,
        /** Marine : missile en silo (antinavire rasant, hypersonique ou croisière selon la munition), torpille,
         *  roquette anti-sous-marine, défense rapprochée automatique (CIWS). */
        NAVAL_MISSILE, TORPEDO, ASW, CIWS;

        /** Missile tiré par un avion (station d'armes). */
        public boolean airMissile() {
            return this == AAM || this == AGM || this == ARM || this == CRUISE || this == NAVAL_MISSILE;
        }
    }

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

    /** Aéronef civil : aucun armement, ignoré des défenses antiaériennes (sauf zone militaire interdite). */
    public boolean civil() {
        return side == Side.CIVIL;
    }

    /** Avion à voilure fixe (B-29). */
    public boolean plane() {
        return chassis == Chassis.PLANE;
    }

    // ------------------------------------------------------------------ aviation
    /** Avion à réaction (postcombustion, bang supersonique, leurres, siège éjectable) ; les autres ont des hélices. */
    public boolean jet() {
        return plane() && geo() != null && geo().props().length == 0;
    }

    /** Gros porteur (bombardiers, transport) : virages lents, pas de voltige. */
    public boolean heavy() {
        return this == B29_SUPERFORTRESS || this == TU_95 || this == C_130 || this == B_2;
    }

    /** Taux de virage maximal (degrés / tick) à pleine vitesse. */
    public float turnRate() {
        return switch (this) {
            case B29_SUPERFORTRESS, TU_95, C_130 -> 1.6f;
            case B_2 -> 1.9f;
            case IL_2, SU_25, A_10, F_117, MIG_25 -> 3.2f;
            case SU_34, F_4E, F_15E -> 3.6f;
            case MIG_29, SU_57, F_22, F_16C -> 4.8f;
            default -> 4.2f;
        };
    }

    /**
     * Surface équivalente radar (cahier des charges §5.A) : 1,5 gros porteurs, 1 chasseurs, 0,2 furtifs de 5e génération
     * (Su-57, F-22), 0 F-117 et B-2 (soute fermée). Tout ce qui n'est pas un avion : 1.
     */
    public float rcs() {
        return switch (this) {
            case B29_SUPERFORTRESS, TU_95, C_130 -> 1.5f;
            case SU_57, F_22 -> 0.2f;
            case F_117, B_2 -> 0f;
            default -> 1f;
        };
    }

    /** Portée maximale de détection radar des furtifs de 5e génération (blocs) : Su-57 -75 %, F-22 -85 %. */
    public float stealthRange() {
        return this == SU_57 ? 150 : this == F_22 ? 90 : 600;
    }

    /** Soute à bombes : nombre maximal de bombes conventionnelles (0 = pas de soute). */
    public int bayCapacity() {
        return switch (this) {
            case B29_SUPERFORTRESS, TU_95, B_2 -> 16;
            case SU_34, F_4E, F_15E -> 8;
            case IL_2, SU_25, F_117, F_16C -> 4;
            case P_51 -> 2;
            default -> 0;
        };
    }

    /** Bombes nucléaires acceptées (seules dans la soute). */
    public int bayNukes() {
        return switch (this) {
            case B29_SUPERFORTRESS, TU_95, F_117 -> 1;
            case B_2 -> 2;
            default -> 0;
        };
    }

    /** Bombes chargées au réarmement (caisse de munitions d'aviation) : guidées pour les avions modernes. */
    public fr.modeminecraft.nuclear.BombItem.Kind defaultBomb() {
        return switch (this) {
            case SU_34, F_15E, F_16C, F_117, B_2 -> fr.modeminecraft.nuclear.BombItem.Kind.GUIDED;
            default -> fr.modeminecraft.nuclear.BombItem.Kind.HE;
        };
    }

    /** Cône de verrouillage des missiles air-air (degrés) : le viseur de casque du MiG-29 permet de verrouiller de côté. */
    public float lockCone() {
        return this == MIG_29 ? 35 : this == SU_57 || this == F_22 ? 14 : 8;
    }

    /** Arme de bord du clic droit (canon / mitrailleuses fixes) : -1 si l'avion n'en a pas. */
    public int planeGun() {
        for (int i = 0; i < weapons.size(); i++) {
            Weapon w = weapons.get(i);
            if (!w.free() && (w.kind() == Kind.MG || w.kind() == Kind.AUTOCANNON)) {
                return i;
            }
        }
        return -1;
    }

    /** Stations d'armes du pilote (clic gauche, R pour changer) : armes fixes hors canon principal, puis -1 = bombes. */
    public List<Integer> stations() {
        List<Integer> l = new java.util.ArrayList<>();
        if (naval()) {
            for (int i = 0; i < weapons.size(); i++) {
                Kind k = weapons.get(i).kind();
                if (k == Kind.NAVAL_MISSILE || k == Kind.TORPEDO || k == Kind.ASW) {
                    l.add(i);
                }
            }
            return l;
        }
        int g = planeGun();
        for (int i = 0; i < weapons.size(); i++) {
            if (i != g && !weapons.get(i).free()) {
                l.add(i);
            }
        }
        if (bayCapacity() > 0) {
            l.add(-1);
        }
        if (l.isEmpty() && g >= 0) {
            l.add(g);
        }
        return l;
    }

    // ------------------------------------------------------------------ marine
    /** Bâtiment de surface ou sous-marin. */
    public boolean naval() {
        return chassis == Chassis.SHIP || chassis == Chassis.SUB;
    }

    public boolean submarine() {
        return chassis == Chassis.SUB;
    }

    /** Tirant d'eau (blocs sous la ligne de flottaison, en surface). */
    public float draft() {
        return switch (this) {
            case KIROV, ZUMWALT -> 3.0f;
            case SLAVA, TICONDEROGA -> 2.7f;
            case UDALOY, ARLEIGH_BURKE -> 2.5f;
            case GORSHKOV -> 2.2f;
            case BUYAN -> 1.4f;
            case OHIO -> 6.4f;
            case VIRGINIA -> 4.9f;
            case INTERCEPTOR_BOAT -> 0.5f;
            default -> 0.35f;
        };
    }

    /** Taux de giration maximal (degrés / tick) : les petites embarcations virent sec, les croiseurs lentement. */
    public float rudder() {
        return switch (this) {
            case RHIB, INTERCEPTOR_BOAT, EXPLOSIVE_USV -> 3.5f;
            case BUYAN -> 1.4f;
            case KIROV, OHIO -> 0.5f;
            default -> 0.8f;
        };
    }

    /** Arme qui tire toute seule (défense rapprochée, missiles antiaériens navals). */
    public static boolean automatic(Weapon w) {
        return w.kind() == Kind.CIWS || w.kind() == Kind.SAM && w.ammo().contains(MilAmmo.NAVAL_SAM);
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

    /** Équipage d'un bâtiment de surface : commandant, canonnier, officier d'armes, puis `extra` matelots. */
    private static List<Seat> ship(Role gunner, int extra) {
        List<Seat> l = new java.util.ArrayList<>(List.of(seat(Role.DRIVER, true), seat(gunner, true, 0), seat(Role.WEAPONS_OFFICER, true)));
        for (int i = 0; i < extra; i++) {
            l.add(seat(Role.PASSENGER, true));
        }
        return l;
    }

    /** Équipage d'un sous-marin : commandant, officier d'armes, matelots. */
    private static List<Seat> sub(int extra) {
        List<Seat> l = new java.util.ArrayList<>(List.of(seat(Role.DRIVER, true), seat(Role.WEAPONS_OFFICER, true)));
        for (int i = 0; i < extra; i++) {
            l.add(seat(Role.PASSENGER, true));
        }
        return l;
    }

    /** Canon naval en tourelle (pointé par le canonnier). */
    private static Weapon naval(String key, int muzzle, int cooldown, int capacity, MilAmmo... ammo) {
        return new Weapon(key, Kind.CANNON, muzzle, cooldown, -5, 60, capacity, 1.6f, false, List.of(ammo));
    }

    /** Système automatique (CIWS, missiles antiaériens navals) : aucun servant, il défend le navire tout seul. */
    private static Weapon auto(String key, Kind kind, int muzzle, int capacity, MilAmmo... ammo) {
        return new Weapon(key, kind, muzzle, 1, -10, 90, capacity, 0, true, List.of(ammo));
    }

    /** Arme fixe d'avion, dans l'axe du fuselage (canon de bord, paniers de roquettes, rails de missiles). */
    private static Weapon gun(String key, Kind kind, int muzzle, int cooldown, int capacity, MilAmmo... ammo) {
        return new Weapon(key, kind, muzzle, cooldown, -90, 90, capacity, 0, false, List.of(ammo));
    }

    /** Arme fixe dans la caisse (lance-grenades de caisse du BMPT) : visée libre dans un cône vers l'avant. */
    private static Weapon fixed(String key, Kind kind, int muzzle, int cooldown, float minPitch, float maxPitch, int capacity, MilAmmo... ammo) {
        return new Weapon(key, kind, muzzle, cooldown, minPitch, maxPitch, capacity, 0, true, List.of(ammo));
    }
}

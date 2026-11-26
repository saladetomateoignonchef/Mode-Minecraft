package fr.modeminecraft.world.city;

import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;

/**
 * Décors propres aux complexes de cartel de la jungle : hacienda (bureau du patron, salon, chapelle, prison, blanchiment, musique), piscine,
 * piste clandestine, hangar (laboratoire et armurerie) et jardins (plantation, camp, jungle piégée, poste de garde). Objets de la série
 * « cartel » posés dans les espaces libres ; la direction est celle de la face visible.
 */
final class JungleDecor {
    private static void put(B b, String id, int x, int y, int z, Direction facing) {
        b.set(x, y, z, B.facing(ModBlocks.realism(id), facing));
    }

    /** Hacienda (32 x 32) : sol du rez-de-chaussée en y = 0, étage en y = 5, terrasse en y = 10. */
    static void hacienda(B b) {
        // bureau du narco-patron (la trappe du coffre secret est en (15, 0, 17) : on laisse x 14..16, z 16..18 libres)
        put(b, "patron_desk", 15, 1, 13, Direction.SOUTH);
        put(b, "boss_leather_chair", 15, 1, 11, Direction.SOUTH);
        put(b, "gold_pistol_case", 12, 1, 12, Direction.SOUTH);
        put(b, "cigar_humidor", 18, 1, 13, Direction.SOUTH);
        put(b, "tequila_cabinet", 12, 1, 5, Direction.SOUTH);
        put(b, "patron_portrait", 15, 3, 5, Direction.SOUTH);
        put(b, "patron_portrait", 22, 3, 5, Direction.SOUTH);
        // salon luxueux et kitsch
        put(b, "giant_tv_wall", 12, 2, 26, Direction.NORTH);
        put(b, "leopard_sofa", 11, 1, 22, Direction.SOUTH);
        put(b, "leopard_sofa", 13, 1, 22, Direction.SOUTH);
        put(b, "stuffed_tiger", 26, 1, 12, Direction.WEST);
        put(b, "golden_bust", 20, 1, 12, Direction.SOUTH);
        put(b, "mini_bar_trolley", 16, 1, 9, Direction.SOUTH);
        put(b, "crystal_chandelier_big", 15, 4, 24, Direction.SOUTH);
        put(b, "pinata_hanging", 12, 4, 17, Direction.SOUTH);
        put(b, "fiesta_banner", 20, 4, 17, Direction.SOUTH);
        // chapelle (côté ouest)
        put(b, "santa_muerte_altar", 5, 1, 22, Direction.EAST);
        put(b, "ex_voto_wall", 5, 2, 18, Direction.EAST);
        put(b, "saint_statue", 5, 1, 19, Direction.EAST);
        put(b, "tall_candle_stand", 6, 1, 20, Direction.EAST);
        put(b, "tall_candle_stand", 6, 1, 24, Direction.EAST);
        put(b, "offering_table", 7, 1, 22, Direction.EAST);
        put(b, "prayer_bench", 9, 1, 20, Direction.WEST);
        put(b, "prayer_bench", 9, 1, 24, Direction.WEST);
        // prison et salle d'interrogatoire (coin sud-est)
        put(b, "bamboo_cage", 22, 1, 20, Direction.SOUTH);
        put(b, "interrogation_chair", 20, 1, 23, Direction.EAST);
        put(b, "torture_table", 23, 1, 23, Direction.NORTH);
        put(b, "chain_wall", 26, 2, 21, Direction.WEST);
        put(b, "pit_grate", 20, 1, 19, Direction.SOUTH);
        put(b, "prison_slop_bucket", 25, 1, 19, Direction.SOUTH);
        // musique et détente
        put(b, "norteno_accordion", 21, 1, 8, Direction.SOUTH);
        put(b, "guitar_stand_c", 23, 1, 8, Direction.SOUTH);
        put(b, "neon_jukebox", 26, 1, 9, Direction.WEST);
        put(b, "cartel_domino_table", 23, 1, 11, Direction.SOUTH);
        put(b, "hammock_lounge", 25, 1, 15, Direction.SOUTH);
        put(b, "tequila_bar_c", 24, 1, 26, Direction.NORTH);
        // étage : blanchiment et comptabilité
        put(b, "bill_counter_machine", 12, 6, 10, Direction.SOUTH);
        put(b, "ledger_desk", 15, 6, 10, Direction.SOUTH);
        put(b, "dollar_pallet", 18, 6, 10, Direction.SOUTH);
        put(b, "cash_duffel_bags", 12, 6, 13, Direction.SOUTH);
        put(b, "paper_shredder", 18, 6, 13, Direction.SOUTH);
        put(b, "safe_deposit_wall", 5, 7, 10, Direction.EAST);
        // terrasse : sécurité
        put(b, "jungle_floodlight", 12, 11, 12, Direction.SOUTH);
        put(b, "cctv_pole", 20, 11, 8, Direction.SOUTH);
        put(b, "mg_nest_j", 24, 11, 12, Direction.SOUTH);
        // façade : enseignes, graffitis, avertissements
        put(b, "cartel_tag_wall", 8, 2, 28, Direction.SOUTH);
        put(b, "cartel_tag_wall", 25, 2, 28, Direction.SOUTH);
        put(b, "cartel_banner_j", 12, 2, 28, Direction.SOUTH);
        put(b, "reward_posters", 21, 2, 28, Direction.SOUTH);
        put(b, "private_property_sign", 9, 0, 30, Direction.SOUTH);
        put(b, "private_property_sign", 22, 0, 30, Direction.SOUTH);
        put(b, "skull_warning_sign", 12, 0, 30, Direction.SOUTH);
    }

    /** Piscine du cartel (16 x 32) : transats, enceintes, bar et fontaine de champagne. */
    static void pool(B b) {
        for (int z = 10; z <= 22; z += 4) {
            put(b, "pool_lounger", 3, 0, z, Direction.EAST);
        }
        put(b, "pool_speaker_stack", 12, 0, 5, Direction.SOUTH);
        put(b, "pool_bar_stools", 7, 0, 25, Direction.SOUTH);
        put(b, "champagne_fountain", 7, 0, 28, Direction.NORTH);
        put(b, "festival_fireworks", 13, 0, 29, Direction.SOUTH);
    }

    /** Piste clandestine : balises, carburant et contrebande au sud de la piste. */
    static void airstrip(B b, int dx) {
        put(b, "airstrip_beacon", 2, 0, 1, Direction.SOUTH);
        put(b, "airstrip_beacon", 13, 0, 1, Direction.SOUTH);
        put(b, "jungle_fuel_drums", 4, 0, 14, Direction.NORTH);
        put(b, "contraband_crate_stack", 8, 0, 14, Direction.NORTH);
        put(b, "cargo_net_bundle", 11, 0, 14, Direction.NORTH);
        put(b, "boat_motor_rack", 13, 0, 13, Direction.WEST);
        if (dx != -2) {
            put(b, "wind_sock", 14, 0, 14, Direction.NORTH);
        }
    }

    /** Petit hangar (laboratoire de drogue et armurerie des sicarios) : sol en y = 0, objets en y = 1. */
    static void hangar(B b) {
        put(b, "chemical_vat", 5, 1, 11, Direction.NORTH);
        put(b, "flask_rack", 2, 1, 6, Direction.EAST);
        put(b, "packing_table", 11, 1, 6, Direction.SOUTH);
        put(b, "fume_hood", 13, 1, 10, Direction.WEST);
        put(b, "chemical_drums", 3, 1, 4, Direction.SOUTH);
        put(b, "pill_press", 10, 1, 10, Direction.SOUTH);
        put(b, "ak_wall_rack", 13, 1, 6, Direction.WEST);
        put(b, "ammo_belt_crates", 13, 1, 8, Direction.WEST);
        put(b, "rpg_cartel_rack", 13, 1, 4, Direction.WEST);
        put(b, "sicario_vest_rack", 2, 1, 8, Direction.EAST);
        put(b, "gun_cleaning_bench", 2, 1, 10, Direction.EAST);
        put(b, "sicario_target_dummy", 10, 1, 12, Direction.NORTH);
    }

    /** Jardin tropical (16 x 16) : plantation, camp, jungle piégée ou poste de garde selon la parcelle. */
    static void garden(B b, double h) {
        put(b, "hanging_vines", 5, 4, 4, Direction.SOUTH);
        put(b, "hanging_vines", 12, 4, 9, Direction.SOUTH);
        if (h < 0.25) {
            put(b, "coca_plants", 2, 0, 6, Direction.EAST);
            put(b, "coca_plants", 2, 0, 8, Direction.EAST);
            put(b, "coca_plants", 13, 0, 3, Direction.WEST);
            put(b, "coca_plants", 13, 0, 6, Direction.WEST);
            put(b, "leaf_drying_rack", 13, 0, 11, Direction.WEST);
            put(b, "camouflaged_greenhouse", 2, 0, 10, Direction.EAST);
            put(b, "irrigation_pipes_j", 5, 0, 3, Direction.SOUTH);
            put(b, "leaf_sacks", 12, 0, 13, Direction.NORTH);
            put(b, "press_tub", 3, 0, 13, Direction.NORTH);
        } else if (h < 0.5) {
            put(b, "hammock_bunk_row", 2, 0, 3, Direction.EAST);
            put(b, "jungle_field_kitchen", 13, 0, 4, Direction.WEST);
            put(b, "cartel_radio_set", 13, 0, 7, Direction.WEST);
            put(b, "jungle_tent", 2, 0, 8, Direction.EAST);
            put(b, "cartel_campfire", 5, 0, 10, Direction.SOUTH);
            put(b, "supply_cache", 13, 0, 10, Direction.WEST);
        } else if (h < 0.75) {
            put(b, "giant_fern", 2, 0, 3, Direction.SOUTH);
            put(b, "giant_fern", 13, 0, 3, Direction.SOUTH);
            put(b, "giant_fern", 13, 0, 13, Direction.SOUTH);
            put(b, "wild_beehive_log", 3, 0, 13, Direction.NORTH);
            put(b, "parrot_perch", 13, 0, 6, Direction.WEST);
            put(b, "orchid_cluster", 5, 0, 2, Direction.SOUTH);
            put(b, "overgrown_statue", 8, 0, 2, Direction.SOUTH);
            put(b, "spike_pit", 11, 0, 3, Direction.SOUTH);
            put(b, "tripwire_trap", 4, 0, 8, Direction.SOUTH);
            put(b, "camo_hide_hut", 13, 0, 10, Direction.WEST);
            put(b, "hunting_net_trap", 2, 0, 10, Direction.EAST);
            put(b, "foot_trap", 6, 0, 13, Direction.NORTH);
        } else {
            put(b, "bamboo_watchtower", 2, 0, 2, Direction.SOUTH);
            put(b, "jungle_floodlight", 13, 0, 3, Direction.WEST);
            put(b, "guard_dog_kennel", 13, 0, 7, Direction.WEST);
            put(b, "barbed_fence_j", 7, 0, 2, Direction.SOUTH);
            put(b, "cctv_pole", 2, 0, 7, Direction.EAST);
            put(b, "mg_nest_j", 13, 0, 11, Direction.WEST);
            put(b, "trench_section", 5, 0, 10, Direction.SOUTH);
            put(b, "skull_warning_sign", 11, 0, 13, Direction.SOUTH);
            put(b, "jungle_trail_sign", 2, 0, 13, Direction.SOUTH);
        }
    }

    private JungleDecor() {}
}

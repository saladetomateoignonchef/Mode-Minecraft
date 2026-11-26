package fr.modeminecraft.world.city;

import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;

/**
 * Décors propres à la cité du désert : riad (salon, cuisine, hammam), palais fortifié (trône, fontaine, caserne), grand souk (épices, artisans),
 * oasis, marché couvert, chantier et place publique. Objets de la série « désert » posés dans les espaces libres, face au centre de la pièce.
 */
final class DesertDecor {
    private static void put(B b, String id, int x, int y, int z, Direction facing) {
        b.set(x, y, z, B.facing(ModBlocks.realism(id), facing));
    }

    /** Riad : sol du rez-de-chaussée à y = 0 (objets en y = 1), patio central x 6..9, z 6..9, étage dont le sol est en y = 4. */
    static void riad(B b, double h) {
        put(b, "low_brass_table", 7, 1, 11, Direction.NORTH);
        put(b, "pouf_stack", 6, 1, 4, Direction.SOUTH);
        put(b, "wedding_chest", 12, 1, 6, Direction.WEST);
        put(b, "zellige_panel", 6, 2, 12, Direction.NORTH);
        put(b, "arched_niche", 12, 2, 8, Direction.WEST);
        put(b, "water_jug_stand", 10, 1, 12, Direction.NORTH);
        if (h < 0.5) {
            put(b, "tagine_stove", 12, 1, 3, Direction.WEST);
            put(b, "olive_oil_jars", 8, 1, 12, Direction.NORTH);
        } else {
            put(b, "clay_bread_oven", 12, 1, 3, Direction.WEST);
            put(b, "steam_bench", 8, 1, 12, Direction.NORTH);
        }
        put(b, "tea_tray", 9, 1, 11, Direction.NORTH);
        put(b, "hanging_oil_lamp", 10, 3, 10, Direction.SOUTH);
        put(b, "incense_burner", 4, 5, 8, Direction.EAST);
        put(b, "dallah_coffee_pot", 4, 5, 11, Direction.EAST);
        put(b, "game_cushion_set", 8, 5, 11, Direction.NORTH);
    }

    /** Maison-tour de trois étages : pièces de 8 x 8, sols en y = 0, 4 et 8 (objets un bloc plus haut). */
    static void riadTall(B b, double h) {
        put(b, "pouf_stack", 5, 1, 5, Direction.SOUTH);
        put(b, "low_brass_table", 8, 1, 8, Direction.SOUTH);
        put(b, "herbalist_shelf", 11, 1, 5, Direction.WEST);
        put(b, "dispensary_cot", 5, 5, 10, Direction.NORTH);
        put(b, "remedy_cabinet", 11, 5, 5, Direction.WEST);
        put(b, "talisman_wall", 6, 6, 4, Direction.SOUTH);
        put(b, "dama_board", 8, 9, 8, Direction.SOUTH);
        put(b, "backgammon_table", 5, 9, 5, Direction.SOUTH);
        put(b, "kettle_stove", 10, 9, 10, Direction.WEST);
    }

    /** Palais du dictateur (48 x 48) : cour, salle du trône, salle forte, caserne (étage y = 10), entrée gardée. */
    static void palace(B b) {
        put(b, "palace_fountain", 23, 0, 33, Direction.SOUTH);
        put(b, "sheikh_throne", 23, 2, 10, Direction.SOUTH);
        put(b, "silk_gold_divan", 19, 1, 14, Direction.EAST);
        put(b, "silk_gold_divan", 28, 1, 14, Direction.WEST);
        put(b, "gilded_arch", 21, 1, 22, Direction.SOUTH);
        put(b, "gilded_arch", 26, 1, 22, Direction.SOUTH);
        put(b, "ornate_chandelier", 23, 4, 18, Direction.SOUTH);
        put(b, "sultan_coffer", 14, 6, 11, Direction.EAST);
        put(b, "sultan_coffer", 14, 6, 13, Direction.EAST);
        put(b, "desert_rifle_rack", 27, 11, 25, Direction.NORTH);
        put(b, "militia_crates", 30, 11, 25, Direction.NORTH);
        put(b, "rpg_rack", 33, 11, 25, Direction.NORTH);
        put(b, "desert_target_dummy", 33, 11, 22, Direction.WEST);
        put(b, "checkpoint_booth_d", 12, 0, 44, Direction.NORTH);
        put(b, "checkpoint_booth_d", 35, 0, 44, Direction.NORTH);
        put(b, "desert_sandbag_wall", 14, 0, 43, Direction.NORTH);
        put(b, "desert_sandbag_wall", 33, 0, 43, Direction.NORTH);
        put(b, "desert_floodlight", 14, 0, 41, Direction.NORTH);
        put(b, "desert_floodlight", 33, 0, 41, Direction.NORTH);
        put(b, "militia_flag", 19, 0, 42, Direction.NORTH);
        put(b, "militia_flag", 28, 0, 42, Direction.NORTH);
        put(b, "timber_barricade", 17, 0, 40, Direction.NORTH);
        put(b, "timber_barricade", 30, 0, 40, Direction.NORTH);
        put(b, "adobe_watchtower", 8, 0, 8, Direction.SOUTH);
        put(b, "adobe_watchtower", 39, 0, 8, Direction.SOUTH);
        put(b, "hung_rugs_stall", 6, 0, 22, Direction.EAST);
        put(b, "camel_standing", 8, 0, 34, Direction.EAST);
        put(b, "festival_lantern_string", 23, 3, 30, Direction.SOUTH);
        put(b, "calligraphy_plate", 18, 2, 9, Direction.SOUTH);
        put(b, "calligraphy_plate", 29, 2, 9, Direction.SOUTH);
        put(b, "stone_stele", 36, 0, 34, Direction.WEST);
        put(b, "sand_drift", 3, 0, 24, Direction.SOUTH);
        put(b, "portcullis_gate", 41, 0, 20, Direction.WEST);
    }

    /** Grand souk couvert (48 x 32) : un métier ou un étal près de chaque emplacement de marchand, coin musique et coin des trafiquants. */
    static void souk(B b) {
        String[] crafts = {"spice_cones", "merchant_scales", "hung_rugs_stall", "pottery_stack", "tea_seller_cart", "fabric_bolts",
                "blacksmith_forge", "potter_wheel", "weaving_loom", "dye_vats", "jeweler_bench", "tannery_vat"};
        int i = 0;
        for (int sx = 2; sx < 43; sx += 8) {
            for (int sz = 2; sz < 29; sz += 8) {
                put(b, crafts[(i++ * 5) % crafts.length], sx + 6, 0, sz + 1, Direction.WEST);
                if (i % 4 == 1) {
                    put(b, "festival_lantern_string", sx + 5, 4, sz + 4, Direction.SOUTH);
                }
            }
        }
        put(b, "darbouka_drums", 22, 0, 14, Direction.SOUTH);
        put(b, "oud_stand", 24, 0, 14, Direction.SOUTH);
        put(b, "dance_carpet", 22, 0, 15, Direction.SOUTH);
        put(b, "dance_carpet", 24, 0, 15, Direction.SOUTH);
        put(b, "festival_drum", 26, 0, 14, Direction.SOUTH);
        put(b, "hotel_sign_d", 44, 0, 4, Direction.SOUTH);
        put(b, "false_floor_hatch", 42, 0, 30, Direction.NORTH);
        put(b, "smuggler_crates", 44, 0, 30, Direction.NORTH);
        put(b, "hidden_jerrycans", 46, 0, 30, Direction.NORTH);
        put(b, "precision_scale", 40, 0, 30, Direction.NORTH);
        put(b, "smuggler_desk", 38, 0, 30, Direction.NORTH);
        put(b, "cash_suitcase_d", 36, 0, 30, Direction.NORTH);
    }

    /** Oasis publique : jardin irrigué, noria, dromadaire, séchoir à dattes. */
    static void oasis(B b, double h) {
        put(b, "shaduf_well", 7, 0, 12, Direction.NORTH);
        put(b, "irrigation_channel", 3, 0, 5, Direction.SOUTH);
        put(b, "irrigation_channel", 3, 0, 6, Direction.SOUTH);
        put(b, "date_drying_rack", 13, 0, 3, Direction.SOUTH);
        put(b, "grain_sack_pile", 2, 0, 12, Direction.EAST);
        put(b, "camel_standing", 13, 0, 9, Direction.WEST);
        if (h < 0.5) {
            put(b, "noria_wheel", 12, 0, 6, Direction.WEST);
        }
        put(b, "ablution_fountain", 6, 0, 3, Direction.SOUTH);
        put(b, "sundial_stele", 2, 0, 2, Direction.SOUTH);
    }

    /** Marché couvert (16 x 16) : étals et métiers. */
    static void marketHall(B b, double h) {
        put(b, "spice_cones", 5, 0, 13, Direction.NORTH);
        put(b, "tea_seller_cart", 11, 0, 13, Direction.NORTH);
        put(b, "pottery_stack", 2, 0, 7, Direction.EAST);
        put(b, "fabric_bolts", 13, 0, 9, Direction.WEST);
        put(b, "merchant_scales", 8, 0, 3, Direction.SOUTH);
        put(b, "festival_lantern_string", 7, 5, 10, Direction.SOUTH);
        if (h > 0.5) {
            put(b, "hung_rugs_stall", 13, 0, 4, Direction.WEST);
        }
    }

    /** Chantier : forge, tour de potier et gravats. */
    static void worksite(B b, double h) {
        put(b, "blacksmith_forge", 11, 0, 4, Direction.WEST);
        put(b, "potter_wheel", 11, 0, 10, Direction.WEST);
        put(b, "rubble_pile_d", 6, 0, 11, Direction.NORTH);
        put(b, "sand_drift", 14, 0, 10, Direction.WEST);
        put(b, "wrecked_pickup", 14, 0, 5, Direction.WEST);
        put(b, "grain_sack_pile", 1, 0, 12, Direction.EAST);
        put(b, "cracked_wall_section", 1, 0, 6, Direction.EAST);
    }

    /** Place publique : tapis de danse, musique, jeux et fêtes. */
    static void square(B b, double h) {
        put(b, "dance_carpet", 4, 0, 10, Direction.SOUTH);
        put(b, "dance_carpet", 5, 0, 10, Direction.SOUTH);
        put(b, "darbouka_drums", 4, 0, 11, Direction.NORTH);
        put(b, "oud_stand", 6, 0, 12, Direction.NORTH);
        put(b, "festival_drum", 10, 0, 12, Direction.NORTH);
        put(b, "festival_fireworks", 12, 0, 3, Direction.SOUTH);
        put(b, "tea_tray", 12, 0, 6, Direction.WEST);
        put(b, "backgammon_table", 3, 0, 5, Direction.EAST);
        put(b, "stone_stele", 8, 0, 13, Direction.NORTH);
        put(b, "caravan_signpost", 13, 0, 11, Direction.WEST);
    }

    /** Avenue (axe nord-sud, chaussée x 3..12) : seulement les bords, pour laisser passer véhicules et bots. */
    static void avenue(B b, int variant) {
        put(b, "souk_banner", 14, 2, 2, Direction.WEST);
        put(b, "arabic_shop_sign", 1, 2, 9, Direction.EAST);
        put(b, "waterskin_rack", 1, 0, 4, Direction.EAST);
        put(b, "camel_saddle_stand", 1, 0, 13, Direction.EAST);
        put(b, "cargo_bales", 14, 0, 13, Direction.WEST);
        put(b, "nomad_fire", 14, 0, 4, Direction.WEST);
        if (variant == 0) {
            put(b, "checkpoint_booth_d", 14, 0, 6, Direction.WEST);
        } else if (variant == 1) {
            put(b, "bedouin_tent", 1, 0, 10, Direction.EAST);
            put(b, "torn_militia_flag", 14, 0, 7, Direction.WEST);
        } else if (variant == 2) {
            put(b, "sand_drift", 14, 0, 11, Direction.WEST);
            put(b, "desert_floodlight", 1, 0, 11, Direction.EAST);
        } else {
            put(b, "stone_stele", 14, 0, 11, Direction.WEST);
            put(b, "hidden_jerrycans", 1, 0, 10, Direction.EAST);
        }
    }

    private DesertDecor() {}
}

package fr.modeminecraft.world.city;

import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;

/**
 * Décors propres aux bâtiments de la base américaine du scénario (dortoir, armurerie, cafétéria / PX, infirmerie, caporaux avec salle de sport,
 * club des officiers, état-major, résidence du colonel, bunker du général, parking, stand de tir) : objets de la série « américaine » posés dans
 * les espaces libres. La direction est celle vers laquelle regarde la face visible de l'objet (vers le centre de la pièce).
 */
final class UsDecor {
    private static void put(B b, String id, int x, int y, int z, Direction facing) {
        b.set(x, y, z, B.facing(ModBlocks.realism(id), facing));
    }

    static void dormitory(B b) {
        put(b, "footlocker_us", 8, 0, 3, Direction.SOUTH);
        put(b, "sea_bag", 9, 0, 3, Direction.SOUTH);
        put(b, "combat_boots_pair", 14, 0, 3, Direction.SOUTH);
        put(b, "desk_fan", 15, 0, 3, Direction.SOUTH);
        put(b, "bedside_radio_us", 12, 0, 3, Direction.SOUTH);
        put(b, "mini_fridge", 11, 0, 12, Direction.NORTH);
        put(b, "footlocker_us", 17, 0, 12, Direction.NORTH);
        put(b, "sea_bag", 20, 0, 12, Direction.NORTH);
        put(b, "photo_wall", 6, 2, 4, Direction.EAST);
        put(b, "wall_calendar_board", 6, 2, 11, Direction.EAST);
        put(b, "folding_chair_row", 19, 0, 7, Direction.SOUTH);
    }

    static void armory(B b) {
        put(b, "m4_rack", 5, 0, 4, Direction.SOUTH);
        put(b, "m4_rack", 8, 0, 4, Direction.SOUTH);
        put(b, "ammo_crates_us", 11, 0, 6, Direction.SOUTH);
        put(b, "armorer_bench", 10, 0, 7, Direction.SOUTH);
        put(b, "target_dummy_us", 3, 0, 12, Direction.NORTH);
        put(b, "gear_lockers", 12, 0, 10, Direction.WEST);
        put(b, "body_armor_rack", 12, 0, 12, Direction.WEST);
        put(b, "oil_can_stack", 9, 0, 12, Direction.NORTH);
        put(b, "restricted_area_sign", 5, 0, 12, Direction.NORTH);
        put(b, "no_smoking_sign", 12, 2, 11, Direction.WEST);
        put(b, "tactical_mannequin", 5, 0, 10, Direction.NORTH);
    }

    static void canteen(B b) {
        put(b, "burger_grill", 11, 0, 2, Direction.SOUTH);
        put(b, "cafeteria_tray_line", 12, 0, 3, Direction.SOUTH);
        put(b, "coffee_urn", 13, 0, 3, Direction.WEST);
        put(b, "snack_vending_us", 13, 0, 5, Direction.WEST);
        put(b, "drink_fridge", 13, 0, 6, Direction.WEST);
        put(b, "px_shelf", 13, 0, 8, Direction.WEST);
        put(b, "hotdog_stand", 13, 0, 9, Direction.WEST);
        put(b, "wall_calendar_board", 2, 2, 9, Direction.EAST);
        put(b, "fourth_july_banner", 2, 2, 11, Direction.EAST);
        put(b, "usa_bunting", 7, 3, 12, Direction.NORTH);
        put(b, "christmas_wreath", 2, 2, 6, Direction.EAST);
        put(b, "pumpkin_stack", 2, 0, 12, Direction.NORTH);
        put(b, "thanksgiving_corn", 13, 0, 12, Direction.NORTH);
    }

    static void infirmary(B b) {
        put(b, "folding_cot", 6, 0, 8, Direction.NORTH);
        put(b, "folding_cot", 9, 0, 8, Direction.NORTH);
        put(b, "coffee_urn", 12, 0, 5, Direction.WEST);
        put(b, "folded_flag_case", 12, 0, 7, Direction.WEST);
        put(b, "purple_heart_case", 12, 0, 9, Direction.WEST);
        put(b, "mini_fridge", 12, 0, 11, Direction.WEST);
        put(b, "memorial_plaque", 7, 2, 12, Direction.NORTH);
        put(b, "cooler_box", 10, 0, 11, Direction.NORTH);
    }

    /** Caporaux : coin salle de sport entre les lits. */
    static void corporals(B b) {
        put(b, "weight_bench", 4, 0, 5, Direction.SOUTH);
        put(b, "treadmill", 8, 0, 5, Direction.SOUTH);
        put(b, "dumbbell_rack", 12, 0, 5, Direction.SOUTH);
        put(b, "punching_bag", 13, 0, 4, Direction.SOUTH);
        put(b, "pullup_bar", 2, 0, 6, Direction.EAST);
        put(b, "exercise_mat", 11, 0, 6, Direction.SOUTH);
        put(b, "exercise_mat", 3, 0, 8, Direction.SOUTH);
        put(b, "dartboard", 13, 2, 9, Direction.WEST);
    }

    static void club(B b) {
        put(b, "poker_table", 12, 0, 10, Direction.NORTH);
        put(b, "ping_pong_table", 6, 0, 11, Direction.NORTH);
        put(b, "dartboard", 13, 2, 4, Direction.WEST);
        put(b, "coffee_urn", 9, 0, 3, Direction.SOUTH);
        put(b, "photo_wall", 13, 2, 11, Direction.WEST);
        put(b, "golden_eagle", 13, 0, 5, Direction.WEST);
        put(b, "usa_bunting", 7, 3, 12, Direction.NORTH);
    }

    static void warRoom(B b) {
        put(b, "server_rack", 13, 0, 4, Direction.WEST);
        put(b, "tactical_screen", 2, 0, 5, Direction.EAST);
        put(b, "satellite_phone", 2, 0, 7, Direction.EAST);
        put(b, "headset_mic_desk", 2, 0, 9, Direction.EAST);
        put(b, "radio_console_us", 13, 0, 7, Direction.WEST);
        put(b, "nuclear_football", 13, 0, 10, Direction.WEST);
        put(b, "red_hotline_phone", 11, 1, 5, Direction.SOUTH);
        put(b, "briefing_podium", 9, 0, 11, Direction.NORTH);
        put(b, "folding_chair_row", 4, 0, 11, Direction.NORTH);
        put(b, "projector_screen", 7, 2, 12, Direction.NORTH);
        put(b, "president_portrait", 4, 2, 12, Direction.NORTH);
        put(b, "wall_calendar_board", 10, 2, 12, Direction.NORTH);
        put(b, "op_plans_table", 13, 0, 8, Direction.WEST);
        put(b, "admin_inbox_tray", 13, 0, 2, Direction.SOUTH);
    }

    static void residence(B b) {
        put(b, "colonel_desk", 9, 0, 7, Direction.SOUTH);
        put(b, "standing_globe", 12, 0, 6, Direction.WEST);
        put(b, "desk_flag_set", 11, 0, 8, Direction.SOUTH);
        put(b, "president_bust", 9, 0, 9, Direction.NORTH);
        put(b, "golden_eagle", 12, 0, 9, Direction.WEST);
        put(b, "president_portrait", 7, 2, 10, Direction.NORTH);
        put(b, "red_hotline_phone", 12, 0, 4, Direction.WEST);
        put(b, "folded_flag_case", 3, 0, 7, Direction.EAST);
    }

    static void generalBunker(B b) {
        put(b, "dual_key_station", 9, -5, 4, Direction.SOUTH);
        put(b, "red_hotline_phone", 11, -5, 4, Direction.SOUTH);
        put(b, "us_launch_console", 13, -6, 4, Direction.WEST);
        put(b, "nuclear_football", 3, -6, 3, Direction.SOUTH);
        put(b, "missile_silo_hatch", 11, -6, 5, Direction.SOUTH);
        put(b, "alarm_light_us", 6, -4, 2, Direction.SOUTH);
        put(b, "blast_door_us", 2, -6, 4, Direction.EAST);
        put(b, "server_rack", 13, -6, 2, Direction.WEST);
    }

    /** Parking réservé : hangar d'entretien, rue principale américaine, fêtes, signalétique et défense de la grille. */
    static void parking(B b) {
        put(b, "aircraft_tug", 3, 0, 11, Direction.SOUTH);
        put(b, "aircraft_boarding_ladder", 6, 0, 11, Direction.SOUTH);
        put(b, "jp8_fuel_drums", 9, 0, 11, Direction.SOUTH);
        put(b, "folded_parachute", 12, 0, 11, Direction.SOUTH);
        put(b, "pilot_helmet_stand", 13, 0, 9, Direction.SOUTH);
        put(b, "hotdog_stand", 22, 0, 12, Direction.NORTH);
        put(b, "ice_cream_cart", 24, 0, 12, Direction.NORTH);
        put(b, "barber_pole", 26, 0, 12, Direction.SOUTH);
        put(b, "post_box_us", 28, 0, 12, Direction.NORTH);
        put(b, "school_bus_stop", 29, 0, 6, Direction.WEST);
        put(b, "firework_display", 25, 0, 4, Direction.SOUTH);
        put(b, "pumpkin_stack", 21, 0, 3, Direction.SOUTH);
        put(b, "little_church", 27, 0, 3, Direction.SOUTH);
        put(b, "base_floodlight", 2, 0, 13, Direction.SOUTH);
        put(b, "base_floodlight", 29, 0, 13, Direction.SOUTH);
        put(b, "guard_post_us", 19, 0, 13, Direction.SOUTH);
        put(b, "spike_barrier", 15, 0, 12, Direction.SOUTH);
        put(b, "spike_barrier", 16, 0, 12, Direction.SOUTH);
        put(b, "restricted_area_sign", 12, 0, 14, Direction.SOUTH);
        put(b, "speed_limit_sign", 18, 0, 14, Direction.SOUTH);
        put(b, "direction_arrows_sign", 13, 0, 13, Direction.SOUTH);
        put(b, "building_number_sign", 1, 0, 14, Direction.SOUTH);
    }

    /** Stand de tir : camp de campagne à l'ouest des cibles. */
    static void range(B b) {
        put(b, "field_table_us", 6, 0, 6, Direction.EAST);
        put(b, "cooler_box", 6, 0, 7, Direction.EAST);
        put(b, "campfire_us", 6, 0, 10, Direction.EAST);
        put(b, "folding_cot", 7, 0, 12, Direction.NORTH);
        put(b, "camo_net", 6, 0, 3, Direction.EAST);
        put(b, "field_tent_us", 5, 0, 13, Direction.NORTH);
        put(b, "site_generator", 5, 0, 2, Direction.SOUTH);
        put(b, "watchtower_post", 28, 0, 13, Direction.WEST);
        put(b, "field_antenna_mast", 28, 0, 2, Direction.WEST);
        put(b, "target_dummy_us", 8, 0, 4, Direction.EAST);
    }

    private UsDecor() {}
}

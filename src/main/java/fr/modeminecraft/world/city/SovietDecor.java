package fr.modeminecraft.world.city;

import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;

/**
 * Décors propres aux bâtiments de la base soviétique du scénario (cantine, armurerie, infirmerie, état-major, club, résidence,
 * bunker du général) : objets de la série « soviétique » posés dans les espaces libres de chaque bâtiment, face au centre de la pièce.
 * Les objets « muraux » sont posés contre un mur avec la face visible vers l'intérieur.
 */
final class SovietDecor {
    private static void put(B b, String id, int x, int y, int z, Direction facing) {
        b.set(x, y, z, B.facing(ModBlocks.realism(id), facing));
    }

    static void canteen(B b) {
        put(b, "big_cauldron", 11, 0, 2, Direction.SOUTH);
        put(b, "ladle_rack", 12, 0, 2, Direction.SOUTH);
        put(b, "canned_goods_shelf", 12, 0, 5, Direction.WEST);
        put(b, "cabbage_barrel", 12, 0, 6, Direction.WEST);
        put(b, "ration_poster", 2, 2, 6, Direction.EAST);
        put(b, "cyrillic_exit_sign", 7, 3, 12, Direction.NORTH);
        put(b, "caged_bunker_lamp", 2, 3, 9, Direction.EAST);
        put(b, "slogan_banner", 2, 3, 11, Direction.EAST);
    }

    static void armory(B b) {
        put(b, "grenade_crates", 11, 0, 10, Direction.SOUTH);
        put(b, "repair_bench", 4, 0, 10, Direction.SOUTH);
        put(b, "ammo_sacks", 9, 0, 11, Direction.SOUTH);
        put(b, "oil_drum", 12, 0, 12, Direction.SOUTH);
        put(b, "shooting_dummy", 3, 0, 12, Direction.SOUTH);
        put(b, "rifle_rack", 9, 0, 5, Direction.SOUTH);
        put(b, "cyrillic_danger_sign", 12, 0, 9, Direction.WEST);
        put(b, "caged_bunker_lamp", 3, 3, 9, Direction.EAST);
    }

    static void infirmary(B b) {
        put(b, "treatment_table", 11, 0, 8, Direction.WEST);
        put(b, "soviet_med_cabinet", 12, 0, 10, Direction.WEST);
        put(b, "medical_jars", 12, 0, 7, Direction.WEST);
        put(b, "uv_lamp", 10, 0, 11, Direction.NORTH);
        put(b, "bucket_ladle", 3, 0, 11, Direction.NORTH);
        put(b, "cyrillic_exit_sign", 7, 3, 12, Direction.NORTH);
    }

    static void warRoom(B b) {
        put(b, "teleprinter", 12, 0, 4, Direction.WEST);
        put(b, "code_safe", 12, 0, 11, Direction.WEST);
        put(b, "signal_jammer", 2, 0, 5, Direction.EAST);
        put(b, "tube_radio", 2, 0, 7, Direction.EAST);
        put(b, "leader_portrait", 7, 2, 12, Direction.NORTH);
        put(b, "staffing_board", 4, 2, 12, Direction.NORTH);
        put(b, "frequency_map", 10, 2, 12, Direction.NORTH);
        put(b, "soviet_flag_fringed", 12, 0, 9, Direction.WEST);
        put(b, "russian_wall_clock", 2, 3, 3, Direction.EAST);
    }

    static void club(B b) {
        put(b, "red_corner", 11, 0, 12, Direction.NORTH);
        put(b, "medal_case", 7, 0, 12, Direction.NORTH);
        put(b, "chess_table", 7, 0, 10, Direction.NORTH);
        put(b, "card_game_table", 5, 0, 11, Direction.NORTH);
        put(b, "balalaika_stand", 12, 0, 4, Direction.WEST);
        put(b, "accordion", 12, 0, 5, Direction.WEST);
        put(b, "tea_vodka_corner", 9, 0, 3, Direction.SOUTH);
        put(b, "leader_bust", 2, 0, 3, Direction.EAST);
    }

    static void residence(B b) {
        put(b, "leader_bust", 9, 0, 9, Direction.NORTH);
        put(b, "soviet_flag_fringed", 12, 0, 9, Direction.WEST);
        put(b, "leader_portrait", 7, 2, 10, Direction.NORTH);
        put(b, "archive_cabinet", 12, 0, 6, Direction.WEST);
        put(b, "general_desk", 9, 0, 6, Direction.SOUTH);
    }

    static void generalBunker(B b) {
        put(b, "dual_launch_keys", 9, -5, 4, Direction.SOUTH);
        put(b, "geiger_counter", 11, -5, 4, Direction.SOUTH);
        put(b, "launch_console", 13, -6, 4, Direction.WEST);
        put(b, "caged_bunker_lamp", 2, -4, 4, Direction.EAST);
        put(b, "radiation_sign", 6, -6, 2, Direction.SOUTH);
    }

    private SovietDecor() {}
}

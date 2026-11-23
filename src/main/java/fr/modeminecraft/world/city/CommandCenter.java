package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.registry.ModBlocks;
import fr.modeminecraft.scenario.Grade;
import fr.modeminecraft.scenario.Side;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Centre de Commandement et des Missions (bâtiments de recrutement des bases américaine et soviétique), parcelle de 2 x 2
 * chunks (32 x 32), façade et parvis au sud (z croissants).
 * <ul>
 * <li>emprise 28 x 22 sur 2 niveaux (13 blocs avec le parapet), environ deux fois les dortoirs ; grand parvis dégagé ;</li>
 * <li>États-Unis : béton clair et verre teinté, colonnade, deux grands drapeaux, enseigne lumineuse ;
 *     URSS : brique et béton brut, étoile rouge en relief au-dessus du porche, deux grands drapeaux rouges, enseigne ;</li>
 * <li>toit : mât radio de 16 blocs à balise rouge et parabole ; projecteurs et lampadaires pour la nuit ;</li>
 * <li>rez-de-chaussée : hall des missions (3 comptoirs d'officiers recruteurs, tableaux de missions, salle d'attente) ;
 *     étage : salle de stratégie (table tactique, moniteurs, terminal des plans) derrière une porte de rang, galerie.</li>
 * </ul>
 */
final class CommandCenter {
    private CommandCenter() {}

    /** Hauteur des murs (niveau du toit) et coordonnées du corps de bâtiment. */
    private static final int ROOF = 12, X0 = 2, X1 = 29, Z0 = 3, Z1 = 24, FLOOR2 = 6;

    static void build(B b, Side side) {
        boolean us = side == Side.US;
        Faction f = side.ally;
        Block wall = us ? Blocks.WHITE_CONCRETE : Blocks.BRICKS;
        Block trim = us ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE;
        Block glass = us ? Blocks.TINTED_GLASS : Blocks.GLASS;
        Block floor = us ? Blocks.POLISHED_ANDESITE : Blocks.SPRUCE_PLANKS;
        Block column = us ? Blocks.QUARTZ_PILLAR : Blocks.LIGHT_GRAY_CONCRETE;

        // ---- parvis dégagé (z 25..31) : dallage, allée d'accès, lampadaires et projecteurs
        b.fill(0, -1, 25, 31, -1, 31, us ? Blocks.SMOOTH_STONE : Blocks.GRAY_CONCRETE);
        b.fill(12, -1, 25, 19, -1, 31, us ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
        for (int z = 26; z <= 31; z += 5) {
            Deco.lampPost(b, 11, 0, z, Direction.EAST);
            Deco.lampPost(b, 20, 0, z, Direction.WEST);
        }
        for (int x : new int[]{5, 9, 22, 26}) {
            Deco.searchlight(b, x, 0, 29);                                    // projecteurs braqués sur la façade
            b.set(x, 1, 28, Blocks.SEA_LANTERN);
        }

        // ---- corps du bâtiment : 28 x 22, deux niveaux, toit à 12
        ScenarioBuildings.box(b, X0, Z0, X1, Z1, ROOF, wall, floor, Blocks.GRAY_CONCRETE);
        b.fill(X0, 0, Z0, X1, 0, Z0, trim);                                    // soubassement
        b.fill(X0, 0, Z1, X1, 0, Z1, trim);
        b.fill(X0, 0, Z0, X0, 0, Z1, trim);
        b.fill(X1, 0, Z0, X1, 0, Z1, trim);
        for (int x = X0; x <= X1; x++) {                                       // corniche
            b.set(x, ROOF - 1, Z1, trim);
            b.set(x, ROOF - 1, Z0, trim);
        }
        // bandes vitrées sur les deux niveaux (montants tous les 4 blocs)
        for (int lvl : new int[]{2, 8}) {
            for (int y = lvl; y <= lvl + 2; y++) {
                for (int x = X0 + 1; x <= X1 - 1; x++) {
                    if ((x - X0) % 4 != 0) {
                        b.set(x, y, Z1, glass);
                        b.set(x, y, Z0, glass);
                    }
                }
                for (int z = Z0 + 1; z <= Z1 - 1; z++) {
                    if ((z - Z0) % 4 != 0) {
                        b.set(X0, y, z, glass);
                        b.set(X1, y, z, glass);
                    }
                }
            }
        }
        // pilastres de façade, du sol à la corniche
        for (int x = X0; x <= X1; x += 4) {
            b.fill(x, 0, Z1, x, ROOF - 1, Z1, column);
        }
        // sol du 1er étage : toute la surface sauf l'atrium central (x 11..20, z 14..22) et la cage d'escalier (x 26..28, z 16..22)
        for (int x = X0 + 1; x <= X1 - 1; x++) {
            for (int z = Z0 + 1; z <= Z1 - 1; z++) {
                boolean atrium = x >= 11 && x <= 20 && z >= 14 && z <= 22;
                boolean stairs = x >= 26 && x <= 28 && z >= 16 && z <= 22;
                if (!atrium && !stairs) {
                    b.set(x, FLOOR2, z, floor);
                }
            }
        }
        // garde-corps de l'atrium
        for (int x = 11; x <= 20; x++) {
            b.set(x, FLOOR2 + 1, 14, Blocks.IRON_BARS);
            b.set(x, FLOOR2 + 1, 22, Blocks.IRON_BARS);
        }
        for (int z = 14; z <= 22; z++) {
            b.set(11, FLOOR2 + 1, z, Blocks.IRON_BARS);
            b.set(20, FLOOR2 + 1, z, Blocks.IRON_BARS);
        }
        // escalier monumental le long du mur est (7 marches, monte vers le nord)
        for (int i = 0; i <= 6; i++) {
            b.set(27, i, 22 - i, B.stairs(us ? Blocks.QUARTZ_STAIRS : Blocks.STONE_BRICK_STAIRS, Direction.SOUTH));
            b.set(26, i, 22 - i, i == 6 ? Blocks.AIR.defaultBlockState() : Blocks.STONE_BRICK_WALL.defaultBlockState());
        }

        facade(b, side, wall, trim, column);

        // ---- toit : parapet, mât radio à balise rouge, parabole
        for (int x = X0; x <= X1; x++) {
            b.set(x, ROOF + 1, Z0, trim);
            b.set(x, ROOF + 1, Z1, trim);
        }
        for (int z = Z0; z <= Z1; z++) {
            b.set(X0, ROOF + 1, z, trim);
            b.set(X1, ROOF + 1, z, trim);
        }
        b.fill(15, ROOF + 1, 8, 15, ROOF + 16, 8, Blocks.IRON_BARS);            // mât radio de 16 blocs
        for (int y = ROOF + 3; y <= ROOF + 15; y += 4) {
            b.fill(14, y, 8, 16, y, 8, Blocks.IRON_BARS);                       // traverses
        }
        b.set(15, ROOF + 17, 8, Blocks.REDSTONE_BLOCK);                         // balise rouge (s'allume la nuit : lampe de ville)
        b.set(15, ROOF + 18, 8, Blocks.REDSTONE_LAMP);
        b.set(14, ROOF + 17, 8, Blocks.RED_CONCRETE);
        b.set(16, ROOF + 17, 8, Blocks.RED_CONCRETE);
        // parabole (cuvette tournée vers le sud-ouest) sur un pylône
        b.fill(24, ROOF + 1, 8, 24, ROOF + 4, 8, Blocks.IRON_BARS);
        b.fill(23, ROOF + 5, 7, 25, ROOF + 5, 9, Blocks.WHITE_CONCRETE);
        for (int x = 22; x <= 26; x++) {
            for (int z = 6; z <= 10; z++) {
                if (x == 22 || x == 26 || z == 6 || z == 10) {
                    b.set(x, ROOF + 6, z, B.slab(Blocks.QUARTZ_SLAB, false));
                }
            }
        }
        b.set(24, ROOF + 7, 8, Blocks.LIGHTNING_ROD);
        b.set(6, ROOF + 1, 20, Blocks.REDSTONE_LAMP);                           // balises d'angle
        b.set(25, ROOF + 1, 20, Blocks.REDSTONE_LAMP);

        hall(b, side, f, us);
        upstairs(b, side, f, us);

        // ---- gardes : deux sentinelles à l'entrée, une patrouille sur le parvis
        b.bot(13, 0, 27, f, BotRole.SOLDIER, 3);
        b.bot(18, 0, 27, f, BotRole.SOLDIER, 3);
        b.bot(8, 0, 29, f, BotRole.SOLDIER, 6);
        b.bot(23, 0, 29, f, BotRole.OFFICER, 6);
    }

    // ------------------------------------------------------------------ façade monumentale
    private static void facade(B b, Side side, Block wall, Block trim, Block column) {
        boolean us = side == Side.US;
        // porte principale : double porte de fer, sas vitré
        b.fill(15, 0, Z1, 16, 2, Z1, Blocks.AIR);
        Deco.door(b, 15, 0, Z1, Direction.SOUTH, Blocks.IRON_DOOR);
        Deco.door(b, 16, 0, Z1, Direction.SOUTH, Blocks.IRON_DOOR);
        b.set(14, 1, Z1 + 1, B.facing(Blocks.STONE_BUTTON, Direction.SOUTH));
        b.set(17, 1, Z1 + 1, B.facing(Blocks.STONE_BUTTON, Direction.SOUTH));
        b.fill(14, 3, Z1, 17, 3, Z1, trim);                                    // linteau
        // enseigne lumineuse : fond de lanternes marines et panneaux lumineux
        b.fill(12, 2, Z1, 19, 4, Z1, Blocks.SEA_LANTERN);
        b.fill(15, 0, Z1, 16, 1, Z1, Blocks.AIR);
        Deco.door(b, 15, 0, Z1, Direction.SOUTH, Blocks.IRON_DOOR);
        Deco.door(b, 16, 0, Z1, Direction.SOUTH, Blocks.IRON_DOOR);
        if (us) {
            ScenarioBuildings.sign(b, 13, 4, Z1 + 1, Direction.SOUTH, "", "US", "", "");
            ScenarioBuildings.sign(b, 14, 4, Z1 + 1, Direction.SOUTH, "", "COMMAND", "", "");
            ScenarioBuildings.sign(b, 15, 4, Z1 + 1, Direction.SOUTH, "", "& RECRUIT-", "", "");
            ScenarioBuildings.sign(b, 16, 4, Z1 + 1, Direction.SOUTH, "", "MENT", "", "");
            ScenarioBuildings.sign(b, 17, 4, Z1 + 1, Direction.SOUTH, "", "CENTER", "", "");
            ScenarioBuildings.sign(b, 15, 3, Z1 + 1, Direction.SOUTH, "", "CENTRE DE", "", "");
            ScenarioBuildings.sign(b, 16, 3, Z1 + 1, Direction.SOUTH, "", "COMMANDEMENT", "", "");
        } else {
            ScenarioBuildings.sign(b, 13, 4, Z1 + 1, Direction.SOUTH, "", "SOVIET", "", "");
            ScenarioBuildings.sign(b, 14, 4, Z1 + 1, Direction.SOUTH, "", "HIGH", "", "");
            ScenarioBuildings.sign(b, 15, 4, Z1 + 1, Direction.SOUTH, "", "COMMAND", "", "");
            ScenarioBuildings.sign(b, 16, 4, Z1 + 1, Direction.SOUTH, "", "& MISSIONS", "", "");
            ScenarioBuildings.sign(b, 15, 3, Z1 + 1, Direction.SOUTH, "", "ÉTAT-MAJOR", "", "");
            ScenarioBuildings.sign(b, 16, 3, Z1 + 1, Direction.SOUTH, "", "& MISSIONS URSS", "", "");
        }
        // deux grands drapeaux verticaux (2 x 7) de part et d'autre de la porte
        for (int x : new int[]{9, 10, 21, 22}) {
            for (int y = 1; y <= 7; y++) {
                ScenarioBuildings.flag(b, x, y, Z1 + 1, Direction.SOUTH, side, y >= 6 && (x == 9 || x == 21));
            }
        }
        if (us) {
            // colonnade d'entrée et portique
            for (int x = 8; x <= 23; x += 3) {
                b.fill(x, 0, Z1 + 3, x, 8, Z1 + 3, column);
            }
            b.fill(7, 9, Z1 + 1, 24, 9, Z1 + 4, Blocks.QUARTZ_SLAB);
            b.fill(7, 8, Z1 + 3, 24, 8, Z1 + 3, Blocks.QUARTZ_BLOCK);
            for (int x = 8; x <= 23; x += 3) {
                b.set(x, 7, Z1 + 2, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
            }
        } else {
            // étoile rouge en relief (8 x 8) au-dessus du porche, sur un cartouche de béton
            String[] star = {"...XX...", "...XX...", "XXXXXXXX", ".XXXXXX.", "..XXXX..", "..XXXX..", ".XXXXXX.", ".XX..XX."};
            b.fill(11, 5, Z1 + 1, 20, 12, Z1 + 1, Blocks.LIGHT_GRAY_CONCRETE);
            for (int j = 0; j < 8; j++) {
                for (int i = 0; i < 8; i++) {
                    if (star[j].charAt(i) == 'X') {
                        b.set(12 + i, 11 - j, Z1 + 2, Blocks.RED_CONCRETE);
                    }
                }
            }
            // porche massif : deux piliers et un auvent de béton brut
            for (int x : new int[]{11, 19}) {
                b.fill(x, 0, Z1 + 2, x + 1, 3, Z1 + 3, Blocks.LIGHT_GRAY_CONCRETE);
            }
            b.fill(11, 4, Z1 + 2, 20, 4, Z1 + 3, Blocks.GRAY_CONCRETE);                  // auvent (l'enseigne reste dégagée)
        }
    }

    // ------------------------------------------------------------------ rez-de-chaussée : hall des missions
    private static void hall(B b, Side side, Faction f, boolean us) {
        Block counter = us ? Blocks.SMOOTH_QUARTZ_SLAB : Blocks.DARK_OAK_SLAB;
        // trois comptoirs d'accueil devant des tableaux de missions (au fond du hall)
        for (int cx : new int[]{7, 15, 23}) {
            b.fill(cx - 2, 1, Z0 + 1, cx + 2, 4, Z0 + 1, Blocks.DARK_OAK_PLANKS);            // tableau d'affichage
            for (int x = cx - 2; x <= cx + 2; x++) {
                for (int y = 1; y <= 4; y++) {
                    if ((x * 5 + y * 3) % 4 == 0) {
                        b.set(x, y, Z0 + 1, (x + y) % 2 == 0 ? Blocks.WHITE_WOOL : Blocks.YELLOW_WOOL);   // fiches de mission épinglées
                    }
                }
            }
            b.set(cx - 2, 5, Z0 + 1, Blocks.SEA_LANTERN);
            b.set(cx + 2, 5, Z0 + 1, Blocks.SEA_LANTERN);
            for (int x = cx - 2; x <= cx + 2; x++) {
                b.set(x, 0, Z0 + 4, B.slab(counter.defaultBlockState().getBlock(), true));    // comptoir
            }
            b.set(cx, 1, Z0 + 4, B.facing(Blocks.LECTERN, Direction.NORTH));
            b.set(cx - 2, 1, Z0 + 4, Blocks.CARTOGRAPHY_TABLE);
            b.set(cx + 2, 1, Z0 + 4, Blocks.NOTE_BLOCK);
            b.bot(cx, 0, Z0 + 2, f, BotRole.RECRUITER, 2);                                    // officier recruteur
        }
        // carte murale (mur ouest) : océans, terres et bases
        for (int z = 8; z <= 18; z++) {
            for (int y = 1; y <= 4; y++) {
                boolean land = (z * 7 + y * 3) % 5 > 1;
                b.set(X0 + 1, y, z, land ? Blocks.GREEN_CONCRETE : Blocks.BLUE_CONCRETE);
            }
        }
        b.set(X0 + 1, 3, 10, Blocks.RED_CONCRETE);
        b.set(X0 + 1, 2, 16, us ? Blocks.LIGHT_BLUE_CONCRETE : Blocks.YELLOW_CONCRETE);
        ScenarioBuildings.sign(b, X0 + 1, 5, 13, Direction.EAST, "MISSIONS", "5 max.", "", "");
        // salle d'attente (bancs le long du mur est et sous l'escalier), plantes, horloge
        for (int z = 9; z <= 14; z++) {
            b.set(X1 - 1, 0, z, B.stairs(Blocks.SPRUCE_STAIRS, Direction.WEST));
        }
        for (int x = 4; x <= 9; x++) {
            b.set(x, 0, 21, B.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
            b.set(x + 12, 0, 6 + 8, B.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
        }
        b.set(4, 0, 22, Blocks.POTTED_FERN);
        b.set(27, 0, 7, Blocks.POTTED_FERN);
        b.set(X1 - 1, 3, 8, B.facing(ModBlocks.WALL_CLOCK.get(), Direction.WEST));
        // plafonniers du hall (double hauteur au centre)
        for (int x = 6; x <= 26; x += 5) {
            for (int z = 8; z <= 20; z += 6) {
                boolean atrium = x >= 11 && x <= 20 && z >= 14 && z <= 22;
                if (!atrium) {
                    b.set(x, FLOOR2 - 1, z, Blocks.SEA_LANTERN);
                }
            }
        }
        for (int x = 13; x <= 18; x += 5) {                                                   // lustres de l'atrium
            b.fill(x, ROOF - 3, 18, x, ROOF - 1, 18, Blocks.CHAIN);
            b.set(x, ROOF - 4, 18, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        }
        b.bot(5, 0, 16, f, BotRole.SOLDIER, 4);
        b.bot(24, 0, 12, f, BotRole.SOLDIER, 4);
    }

    // ------------------------------------------------------------------ 1er étage : salle de stratégie (War Room)
    private static void upstairs(B b, Side side, Faction f, boolean us) {
        int y = FLOOR2 + 1;                                                                   // niveau des pieds
        Block wall = Blocks.POLISHED_DEEPSLATE;
        // salle de stratégie : x 3..20, z 4..13, cloisons blindées, porte à accréditation de Lieutenant
        b.fill(3, y, 14, 20, y + 4, 14, wall);
        b.fill(21, y, 4, 21, y + 4, 14, wall);
        b.fill(21, y, 9, 21, y + 1, 9, Blocks.AIR);
        BlockState door = ModBlocks.RANK_SECURITY_DOOR.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(fr.modeminecraft.scenario.RankDoorBlock.RANK, Math.max(1, Grade.LIEUTENANT.ordinal()));
        b.set(21, y, 9, door);
        b.set(21, y + 1, 9, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        ScenarioBuildings.sign(b, 22, y + 2, 9, Direction.EAST, "WAR ROOM", "Grade", "LIEUTENANT", "");
        // grande table tactique : carte du monde en relief, bases ennemies en rouge, bases alliées en bleu
        for (int x = 7; x <= 16; x++) {
            for (int z = 6; z <= 11; z++) {
                boolean land = (x * 5 + z * 7) % 6 > 1;
                b.set(x, y, z, land ? ((x + z) % 4 == 0 ? Blocks.BROWN_CONCRETE : Blocks.LIME_CONCRETE) : Blocks.LIGHT_BLUE_CONCRETE);
            }
        }
        b.set(9, y + 1, 7, Blocks.RED_CANDLE);
        b.set(14, y + 1, 10, Blocks.BLUE_CANDLE);
        b.set(12, y + 1, 8, Blocks.RED_CANDLE);
        b.set(11, y + 1, 9, Blocks.END_ROD);
        b.set(12, y + 1, 9, Blocks.END_ROD);                                                  // hologramme stylisé
        b.fill(6, y, 5, 17, y, 5, B.slab(Blocks.SMOOTH_STONE_SLAB, false));
        for (int x = 7; x <= 16; x += 3) {
            b.set(x, y + 1, 5, B.facing(ModBlocks.SURVEILLANCE_MONITOR.get(), Direction.SOUTH));
        }
        b.set(4, y, 6, B.facing(ModBlocks.PLANS_TERMINAL.get(), Direction.EAST));
        for (int x = 6; x <= 16; x += 5) {
            b.set(x, ROOF - 1, 8, Blocks.SEA_LANTERN);
        }
        b.bot(10, y, 12, f, BotRole.OFFICER, 3);
        // galerie sur l'atrium : bancs et vue sur le hall
        for (int x = 12; x <= 19; x += 2) {
            b.set(x, y, 23, B.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
        }
        for (int x = 6; x <= 24; x += 6) {
            b.set(x, ROOF - 1, 19, Blocks.SEA_LANTERN);
        }
        b.set(23, ROOF - 1, 10, Blocks.SEA_LANTERN);
        b.bot(24, y, 18, f, BotRole.SOLDIER, 3);
    }
}

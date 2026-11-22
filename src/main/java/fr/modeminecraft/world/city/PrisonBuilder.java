package fr.modeminecraft.world.city;

import fr.modeminecraft.block.police.SlidingJailDoorBlock;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModBlocks;
import fr.modeminecraft.scenario.Grade;
import fr.modeminecraft.scenario.Prisoners;
import fr.modeminecraft.scenario.Side;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Prison militaire de la base (site de 32 x 32, entrée au sud) :
 * - double enceinte grillagée surmontée de barbelés, chemin de ronde gravillonné, deux miradors avec projecteurs et
 *   tireurs, projecteurs d'angle, portail à double sas gardé, guérite et barrière levante ;
 * - bâtiment principal : bloc cellulaire de 9 cellules (couchette, WC-lavabo, banc, lucarne à barreaux, éclairage
 *   encastré, porte coulissante verrouillable) de part et d'autre d'une coursive centrale, cellule d'isolement
 *   aveugle, poste de garde (pupitre d'ouverture des cellules, mur d'écrans de vidéosurveillance, casiers, râtelier,
 *   machine à café), salle d'interrogatoire (table, deux chaises, lampe suspendue, magnétophone) avec miroir sans tain
 *   donnant sur la salle d'observation, bureau du directeur (accès Lieutenant), hall d'entrée avec portique détecteur
 *   et comptoir d'accueil ;
 * - annexe : réfectoire (tables, bancs, comptoir de cuisine) et douches communes, buanderie ;
 * - cour de promenade grillagée : panier de basket, barres de traction, bancs de musculation, gradins ;
 * - quelques détenus ennemis déjà incarcérés, gardiens, directeur.
 */
final class PrisonBuilder {
    private PrisonBuilder() {}

    static void prison(B b, Side side) {
        boolean us = side == Side.US;
        Faction guard = side.ally;
        Block wall = us ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.GRAY_CONCRETE;
        Block trim = us ? Blocks.CYAN_TERRACOTTA : Blocks.RED_TERRACOTTA;
        Block floor = us ? Blocks.POLISHED_ANDESITE : Blocks.POLISHED_DEEPSLATE;
        Block cellFloor = Blocks.SMOOTH_STONE;
        Block roof = us ? Blocks.SMOOTH_STONE : Blocks.STONE_BRICKS;

        // ---------------------------------------------------------- enceinte
        b.fill(1, -1, 1, 30, -1, 30, Blocks.GRAVEL);                                           // chemin de ronde
        fenceLine(b, 0, 0, 31, 0);
        fenceLine(b, 0, 0, 0, 31);
        fenceLine(b, 31, 0, 31, 31);
        fenceLine(b, 0, 31, 13, 31);
        fenceLine(b, 18, 31, 31, 31);
        fenceLine(b, 2, 2, 29, 2);
        fenceLine(b, 2, 2, 2, 29);
        fenceLine(b, 29, 2, 29, 29);
        fenceLine(b, 2, 29, 13, 29);
        fenceLine(b, 18, 29, 29, 29);
        b.fill(3, -1, 3, 28, -1, 28, us ? Blocks.STONE : Blocks.ANDESITE);                     // sol de l'enceinte
        // portail : barrière levante, sas entre les deux grilles, guérite
        for (int x = 14; x <= 17; x++) {
            b.set(x, -1, 30, Blocks.GRAY_CONCRETE);
            b.set(x, -1, 31, Blocks.GRAY_CONCRETE);
        }
        b.set(13, 0, 31, Blocks.ANDESITE_WALL);
        b.set(13, 1, 31, Blocks.LANTERN);
        b.set(18, 0, 31, Blocks.ANDESITE_WALL);
        b.set(18, 1, 31, Blocks.LANTERN);
        for (int x = 14; x <= 17; x++) {
            b.set(x, 1, 31, Blocks.STRIPPED_BIRCH_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));   // barrière levante
        }
        b.set(14, 1, 31, Blocks.RED_CONCRETE);
        b.set(16, 1, 31, Blocks.RED_CONCRETE);
        // guérite (entre portail et bâtiment)
        b.fill(19, 0, 25, 22, 3, 28, wall);
        b.fill(20, 0, 26, 21, 2, 27, Blocks.AIR);
        b.set(19, 1, 26, Blocks.GLASS_PANE);
        b.set(19, 1, 27, Blocks.GLASS_PANE);
        b.set(20, 0, 25, Blocks.AIR);
        b.set(20, 1, 25, Blocks.AIR);
        b.set(21, 0, 27, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
        b.set(21, 1, 27, B.facing(ModBlocks.SURVEILLANCE_MONITOR.get(), Direction.WEST));
        b.fill(19, 4, 25, 22, 4, 28, Blocks.SMOOTH_STONE_SLAB);
        b.set(20, 2, 26, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        b.bot(20, 0, 26, guard, BotRole.SOLDIER, 2);
        // allée du portail au bâtiment
        b.fill(14, -1, 24, 17, -1, 30, Blocks.GRAY_CONCRETE);

        // ---------------------------------------------------------- miradors, projecteurs
        Deco.watchtower(b, 0, 0, 7, Blocks.SPRUCE_LOG, Blocks.SPRUCE_PLANKS);
        Deco.watchtower(b, 28, 0, 7, Blocks.SPRUCE_LOG, Blocks.SPRUCE_PLANKS);
        Deco.searchlight(b, 2, 8, 2);
        Deco.searchlight(b, 29, 8, 2);
        Deco.searchlight(b, 0, 4, 31);
        Deco.searchlight(b, 31, 4, 31);
        b.bot(1, 8, 1, guard, BotRole.SNIPER, 1);
        b.bot(30, 8, 1, guard, BotRole.SNIPER, 1);

        // ---------------------------------------------------------- bâtiment principal (x 4..27, z 4..23)
        b.fill(4, -1, 4, 27, -1, 23, floor);
        b.fill(4, 0, 4, 27, 5, 23, wall);
        b.fill(5, 0, 5, 26, 5, 22, Blocks.AIR);
        b.fill(4, 6, 4, 27, 6, 23, roof);
        b.fill(4, 3, 4, 27, 3, 4, trim);                                                       // bandeau de façade
        b.fill(4, 3, 23, 27, 3, 23, trim);
        // bloc cellulaire nord (z 5..7) et sud (z 12..14), coursive z 9..10
        b.fill(5, -1, 5, 26, -1, 15, cellFloor);
        b.fill(5, -1, 9, 26, -1, 10, Blocks.POLISHED_ANDESITE);
        b.fill(5, 0, 8, 26, 4, 8, Blocks.IRON_BARS);                                           // façades à barreaux nord
        b.fill(8, 0, 11, 24, 4, 11, Blocks.IRON_BARS);                                         // façades à barreaux sud
        b.fill(5, 0, 15, 26, 5, 15, wall);                                                     // mur arrière des cellules sud
        b.fill(5, 5, 5, 26, 5, 14, wall);                                                      // plafond bas du bloc
        for (int x : new int[]{8, 12, 16, 20, 24}) {
            b.fill(x, 0, 5, x, 4, 7, wall);                                                    // séparations nord
            if (x <= 24) {
                b.fill(x, 0, 12, x, 4, 14, wall);                                              // séparations sud
            }
        }
        for (int[] c : Prisoners.CELLS) {
            cell(b, c[0], c[1], c[2], c[3]);
        }
        // cellule d'isolement (x 25..26, z 5..7) : murs pleins, porte pleine, aucune lumière
        b.fill(25, 0, 8, 26, 4, 8, wall);
        Deco.door(b, 25, 0, 8, Direction.NORTH, ModBlocks.LOCKED_METAL_DOOR.get());
        b.set(26, 0, 5, B.facing(ModBlocks.PRISON_BUNK_BED.get(), Direction.SOUTH));
        b.set(25, 0, 5, Blocks.CAULDRON);
        // passages ouest (x 5..7) et est (x 25..26) entre coursive et zone administrative
        b.fill(5, 0, 11, 7, 4, 14, Blocks.AIR);
        b.fill(25, 0, 11, 26, 4, 14, Blocks.AIR);
        b.fill(5, -1, 11, 7, -1, 14, Blocks.POLISHED_ANDESITE);
        b.fill(25, -1, 11, 26, -1, 14, Blocks.POLISHED_ANDESITE);
        b.set(6, 0, 15, Blocks.AIR);
        b.set(6, 1, 15, Blocks.AIR);
        b.set(25, 0, 15, Blocks.AIR);
        b.set(25, 1, 15, Blocks.AIR);
        b.fill(5, 0, 15, 5, 1, 15, wall);
        // éclairage de la coursive, caméras
        for (int x = 6; x <= 25; x += 4) {
            b.set(x, 5, 9, Blocks.SEA_LANTERN);
            b.set(x + 2, 5, 10, Blocks.SEA_LANTERN);
        }
        Deco.camera(b, 5, 4, 9, Direction.EAST);
        Deco.camera(b, 26, 4, 10, Direction.WEST);
        ScenarioBuildings.sign(b, 15, 3, 9, Direction.SOUTH, us ? new String[]{"", "CELL BLOCK A", "", ""} : new String[]{"", "БЛОК А", "Bloc cellulaire", ""});

        // ---------------------------------------------------------- zone administrative (z 16..22)
        b.fill(11, 0, 16, 11, 5, 22, wall);                                                    // cloison poste de garde
        b.fill(17, 0, 16, 17, 5, 19, wall);                                                    // cloison interrogatoire / observation
        b.fill(21, 0, 16, 21, 5, 22, wall);                                                    // cloison bureau du directeur
        b.fill(12, 0, 20, 20, 5, 20, wall);                                                    // mur du hall
        // poste de garde (x 5..10)
        b.set(10, 0, 17, B.facing(ModBlocks.CELL_CONTROL_PANEL.get(), Direction.WEST));
        for (int z = 16; z <= 19; z++) {
            b.set(5, 1, z, B.facing(ModBlocks.SURVEILLANCE_MONITOR.get(), Direction.EAST));    // mur d'écrans
            b.set(5, 2, z, B.facing(ModBlocks.SURVEILLANCE_MONITOR.get(), Direction.EAST));
        }
        b.fill(6, 0, 16, 6, 0, 19, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));                    // pupitre
        b.set(7, 0, 17, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST));
        b.set(7, 0, 18, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST));
        for (int x = 6; x <= 9; x++) {
            b.set(x, 0, 22, B.facing(ModBlocks.POLICE_LOCKER.get(), Direction.NORTH));        // casiers des gardiens
        }
        Deco.armoryRack(b, 10, 0, 21, Direction.WEST);
        b.set(10, 0, 20, Blocks.BREWING_STAND);                                                 // machine à café
        b.set(8, 5, 19, Blocks.SEA_LANTERN);
        b.set(11, 0, 21, Blocks.AIR);                                                            // porte vers le hall
        b.set(11, 1, 21, Blocks.AIR);
        Deco.door(b, 11, 0, 21, Direction.EAST, Blocks.IRON_DOOR);
        b.bot(8, 0, 20, guard, BotRole.OFFICER, 3);
        b.bot(7, 0, 9, guard, BotRole.SOLDIER, 8);                                              // ronde de la coursive
        b.bot(22, 0, 10, guard, BotRole.SOLDIER, 8);
        // salle d'interrogatoire (x 12..16, z 16..19) : table, chaises, lampe suspendue, magnétophone
        b.fill(12, -1, 16, 16, -1, 19, Blocks.GRAY_CONCRETE);
        b.set(14, 0, 17, B.slab(Blocks.POLISHED_DEEPSLATE_SLAB, true));                         // table
        b.set(13, 0, 17, B.slab(Blocks.POLISHED_DEEPSLATE_SLAB, true));
        b.set(14, 0, 16, B.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));                     // chaise du détenu (cf. Prisoners.CHAIR)
        b.set(14, 0, 18, B.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));                     // chaise de l'interrogateur
        b.set(14, 4, 17, Blocks.CHAIN);
        b.set(14, 3, 17, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        b.set(13, 1, 17, Blocks.NOTE_BLOCK);                                                    // magnétophone
        b.set(16, 0, 16, Blocks.CAULDRON);                                                        // seau d'eau
        Deco.camera(b, 12, 4, 16, Direction.SOUTH);
        // miroir sans tain vers l'observation
        b.fill(17, 1, 17, 17, 2, 18, Blocks.TINTED_GLASS);
        b.set(14, 0, 20, Blocks.AIR);
        b.set(14, 1, 20, Blocks.AIR);
        Deco.door(b, 14, 0, 20, Direction.NORTH, ModBlocks.LOCKED_METAL_DOOR.get());
        ScenarioBuildings.sign(b, 13, 2, 20, Direction.SOUTH, us ? new String[]{"", "INTERROGATION", "", ""} : new String[]{"", "ДОПРОСНАЯ", "Interrogatoire", ""});
        // salle d'observation (x 18..20, z 16..19)
        b.set(18, 0, 17, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST));
        b.set(18, 0, 18, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST));
        b.set(20, 0, 16, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
        b.set(20, 1, 16, B.facing(ModBlocks.SURVEILLANCE_MONITOR.get(), Direction.SOUTH));
        b.set(19, 4, 18, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        b.set(19, 0, 20, Blocks.AIR);
        b.set(19, 1, 20, Blocks.AIR);
        Deco.door(b, 19, 0, 20, Direction.NORTH, Blocks.IRON_DOOR);
        // bureau du directeur (x 22..26, z 16..22)
        b.fill(22, -1, 16, 26, -1, 22, Blocks.DARK_OAK_PLANKS);
        b.fill(23, -1, 18, 25, -1, 20, us ? Blocks.BLUE_WOOL : Blocks.RED_WOOL);               // tapis
        Deco.desk(b, 24, 0, 18, Direction.NORTH);
        b.set(23, 0, 18, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
        b.fill(26, 0, 16, 26, 1, 17, Blocks.BOOKSHELF);
        b.set(26, 0, 19, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));   // classeurs
        b.set(26, 0, 20, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        b.set(22, 0, 16, Blocks.POTTED_FERN);
        ScenarioBuildings.flag(b, 24, 2, 16, Direction.SOUTH, side);
        b.set(24, 5, 19, Blocks.SEA_LANTERN);
        b.set(27, 1, 19, Blocks.GLASS_PANE);
        b.set(27, 1, 20, Blocks.GLASS_PANE);
        b.bot(24, 0, 20, guard, BotRole.OFFICER, 2);                                            // directeur
        // hall d'entrée (x 12..20, z 21..22) : portique détecteur, comptoir, porte d'accès au bureau
        b.fill(12, -1, 21, 20, -1, 22, Blocks.POLISHED_DIORITE);
        b.fill(15, 0, 21, 15, 2, 21, Blocks.IRON_BARS);                                          // portique
        b.fill(17, 0, 21, 17, 2, 21, Blocks.IRON_BARS);
        b.set(16, 2, 21, Blocks.IRON_BARS);
        b.set(12, 0, 21, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));                              // comptoir d'accueil
        b.set(12, 0, 22, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
        b.set(13, 0, 21, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST));
        b.set(21, 0, 21, Blocks.AIR);
        b.set(21, 1, 21, Blocks.AIR);
        ScenarioBuildings.rankDoor(b, 21, 21, Direction.EAST, Grade.LIEUTENANT);
        b.set(16, 5, 21, Blocks.SEA_LANTERN);
        // entrée principale (sud)
        b.fill(15, 0, 23, 16, 1, 23, Blocks.AIR);
        Deco.door(b, 15, 0, 23, Direction.SOUTH, Blocks.IRON_DOOR);
        Deco.door(b, 16, 0, 23, Direction.SOUTH, Blocks.IRON_DOOR);
        ScenarioBuildings.sign(b, 15, 2, 24, Direction.SOUTH, us ? new String[]{"", "MILITARY", "DETENTION FACILITY", ""}
                : new String[]{"", "ГАУПТВАХТА", "Prison militaire", ""});
        ScenarioBuildings.flag(b, 17, 3, 24, Direction.SOUTH, side);
        // lucarnes à barreaux sur les façades
        for (int x = 6; x <= 22; x += 4) {
            b.set(x, 3, 4, Blocks.IRON_BARS);
        }
        Deco.camera(b, 14, 4, 24, Direction.SOUTH);

        // ---------------------------------------------------------- annexe : réfectoire, douches, buanderie (x 3..12, z 24..28)
        b.fill(3, -1, 24, 12, -1, 28, Blocks.WHITE_CONCRETE);
        b.fill(3, 0, 24, 12, 3, 28, wall);
        b.fill(4, 0, 25, 11, 2, 27, Blocks.AIR);
        b.fill(3, 4, 24, 12, 4, 28, roof);
        b.fill(8, 0, 25, 8, 2, 27, wall);                                                        // cloison réfectoire / douches
        b.set(8, 0, 26, Blocks.AIR);
        b.set(8, 1, 26, Blocks.AIR);
        // réfectoire (x 4..7)
        for (int x = 4; x <= 7; x++) {
            b.set(x, 0, 27, B.facing(ModBlocks.PRISON_BENCH.get(), Direction.NORTH));             // bancs
        }
        b.fill(4, 0, 26, 6, 0, 26, B.slab(Blocks.SMOOTH_STONE_SLAB, true));                    // longue table
        Deco.kitchen(b, 5, 0, 25);                                                              // comptoir de cuisine
        b.set(6, 3, 26, Blocks.SEA_LANTERN);
        // douches (x 9..11) : pommeaux, carrelage, siphons
        b.fill(9, -1, 25, 11, -1, 27, Blocks.LIGHT_BLUE_TERRACOTTA);
        for (int z = 25; z <= 27; z++) {
            b.set(11, 2, z, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
            b.set(11, 0, z, Blocks.IRON_TRAPDOOR.defaultBlockState());
        }
        b.set(9, 0, 27, Blocks.CAULDRON);                                                        // buanderie
        b.set(9, 1, 27, Blocks.WHITE_CARPET);
        b.set(5, 1, 24, Blocks.AIR);
        b.set(5, 0, 24, Blocks.AIR);
        Deco.door(b, 5, 0, 24, Direction.NORTH, Blocks.IRON_DOOR);
        ScenarioBuildings.sign(b, 7, 2, 23 + 1, Direction.NORTH, us ? new String[]{"", "MESS / SHOWERS", "", ""} : new String[]{"", "СТОЛОВАЯ", "Réfectoire", ""});

        // ---------------------------------------------------------- cour de promenade (x 23..28, z 24..28 ; grillagée)
        b.fill(23, -1, 24, 28, -1, 28, Blocks.GRAY_CONCRETE);
        for (int x = 23; x <= 28; x++) {
            b.fill(x, 0, 24, x, 2, 24, Blocks.IRON_BARS);
            b.fill(x, 0, 28, x, 2, 28, Blocks.IRON_BARS);
        }
        b.fill(23, 0, 24, 23, 2, 28, Blocks.IRON_BARS);
        b.set(23, 0, 26, Blocks.AIR);
        b.set(23, 1, 26, Blocks.AIR);
        b.fill(28, 0, 26, 28, 3, 26, Blocks.IRON_BARS);                                          // panier de basket
        b.set(27, 3, 26, Blocks.IRON_BARS);
        b.set(27, 2, 26, B.facing(Blocks.IRON_TRAPDOOR, Direction.EAST));
        b.set(25, -1, 26, Blocks.WHITE_CONCRETE);
        b.fill(24, 0, 25, 24, 2, 25, Blocks.IRON_BARS);                                          // barre de traction
        b.fill(26, 0, 25, 26, 2, 25, Blocks.IRON_BARS);
        b.set(25, 2, 25, Blocks.CHAIN.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        b.set(25, 0, 27, B.facing(ModBlocks.PRISON_BENCH.get(), Direction.NORTH));               // banc de musculation
        b.set(26, 0, 27, Blocks.ANVIL);                                                           // haltères
        b.set(24, 0, 27, B.stairs(Blocks.STONE_BRICK_STAIRS, Direction.EAST));                    // gradins

        // ---------------------------------------------------------- détenus ennemis déjà incarcérés
        Faction enemy = side.enemy;
        for (int k : new int[]{0, 2, 6}) {
            int[] c = Prisoners.CELLS[k];
            b.bot(c[0], 0, c[1], enemy, BotRole.SOLDIER, 1, p -> {
                Prisoners.makePrisoner(p);
                p.getPersistentData().putInt("modeminecraft_cell", k);
            });
        }
    }

    /** Clôture grillagée de 3 blocs surmontée de barbelés. */
    private static void fenceLine(B b, int x0, int z0, int x1, int z1) {
        Deco.fence(b, Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1), 3);
    }

    /** Cellule : couchette, WC-lavabo, banc, éclairage encastré, porte coulissante (verrouillée) au centre de la façade. */
    private static void cell(B b, int cx, int cz, int dx, int dz) {
        boolean north = dz < cz;                                                                 // porte au nord : cellule sud
        int back = north ? cz + 1 : cz - 1;
        b.set(cx - 1, 0, back, B.facing(ModBlocks.PRISON_BUNK_BED.get(), north ? Direction.NORTH : Direction.SOUTH));
        b.set(cx + 1, 0, back, B.facing(ModBlocks.PRISON_TOILET_SINK.get(), north ? Direction.SOUTH : Direction.NORTH));
        b.set(cx + 1, 0, cz, B.facing(ModBlocks.PRISON_BENCH.get(), Direction.WEST));
        b.set(cx, 4, cz, Blocks.SEA_LANTERN);
        BlockState door = ModBlocks.SLIDING_JAIL_DOOR.get().defaultBlockState().setValue(SlidingJailDoorBlock.FACING, north ? Direction.NORTH : Direction.SOUTH)
                .setValue(SlidingJailDoorBlock.LOCKED, true);
        b.set(dx, 0, dz, door);
        b.set(dx, 1, dz, door.setValue(SlidingJailDoorBlock.HALF, DoubleBlockHalf.UPPER));
    }
}

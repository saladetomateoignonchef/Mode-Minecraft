package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hôpital de la métropole (2 x 2 chunks, 2 étages + héliport) :
 * - rez-de-chaussée : accueil avec pharmacienne (vend bandages, garrots, attelles, plâtres, défibrillateurs), salle d'attente,
 *   salle de soins en lits, bloc opératoire, scanner, distributeur ;
 * - 1er étage : chambres de patients, bureaux, réserve de médicaments (coffres de butin) ;
 * - toit : héliport ; devant : baie d'ambulances.
 */
final class Hospital {
    static void build(B b) {
        b.ground(0, 0, 31, 31, Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 22);
        BlockState wall = Blocks.WHITE_CONCRETE.defaultBlockState(), trim = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState red = Blocks.RED_CONCRETE.defaultBlockState();
        for (int f = 0; f < 2; f++) {
            int y = f * 5;
            b.fill(3, y, 4, 28, y, 26, Blocks.SMOOTH_QUARTZ);                                    // sol
            b.walls(3, y, 4, 28, y + 4, 26, wall);
            b.walls(3, y + 4, 4, 28, y + 4, 26, trim);
            for (int i = 5; i <= 26; i += 4) {                                                   // fenêtres
                b.fill(i, y + 1, 4, i + 1, y + 3, 4, Blocks.LIGHT_BLUE_STAINED_GLASS);
                b.fill(i, y + 1, 26, i + 1, y + 3, 26, Blocks.LIGHT_BLUE_STAINED_GLASS);
            }
            for (int i = 8; i <= 22; i += 7) {
                b.fill(3, y + 1, i, 3, y + 3, i + 1, Blocks.LIGHT_BLUE_STAINED_GLASS);
                b.fill(28, y + 1, i, 28, y + 3, i + 1, Blocks.LIGHT_BLUE_STAINED_GLASS);
            }
        }
        b.fill(3, 9, 4, 28, 9, 26, trim);                                                         // toit plat
        // croix rouge en façade (au-dessus de l'entrée)
        b.fill(15, 5, 4, 16, 9, 4, red);
        b.fill(13, 6, 4, 18, 7, 4, red);
        // entrée : portes ouvertes, auvent rouge, bornes d'ambulances
        b.fill(14, 1, 4, 17, 3, 4, Blocks.AIR.defaultBlockState());
        b.fill(12, 4, 2, 19, 4, 3, red);
        b.fill(12, 0, 0, 19, 0, 3, Blocks.WHITE_CONCRETE);
        b.set(11, 1, 3, Blocks.RED_BANNER);
        b.set(20, 1, 3, Blocks.RED_BANNER);
        // baie d'ambulances (à gauche de l'entrée) : marquage au sol
        b.fill(4, 0, 0, 10, 0, 3, Blocks.YELLOW_CONCRETE);
        b.fill(5, 0, 1, 9, 0, 2, Blocks.GRAY_CONCRETE);
        b.vehicle(7, 1, 1, fr.modeminecraft.entity.VehicleEntity.Type.AMBULANCE, 90f);                  // ambulance garée dans la baie
        // escalier : échelle (rez-de-chaussée -> étage -> toit)
        for (int y = 1; y <= 9; y++) {
            b.set(27, y, 25, B.facing(Blocks.LADDER, Direction.NORTH));
        }
        b.fill(26, 5, 25, 26, 8, 25, wall);
        b.set(27, 5, 25, Blocks.AIR);

        // ---------------- rez-de-chaussée
        // accueil : comptoir, horloge, enseigne, pharmacienne
        b.fill(10, 1, 8, 21, 1, 8, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
        b.set(12, 3, 7, B.facing(ModBlocks.WALL_CLOCK.get(), Direction.SOUTH));
        b.set(19, 3, 7, B.facing(ModBlocks.NEON_SIGN_CYAN.get(), Direction.SOUTH));
        b.fill(8, 1, 9, 23, 3, 9, Blocks.WHITE_CONCRETE);                                        // cloison derrière le comptoir
        b.fill(14, 1, 9, 17, 3, 9, Blocks.AIR.defaultBlockState());
        b.bot(15, 1, 9, Faction.CIVILIAN, BotRole.CIVILIAN, 1, p -> {                           // drapeaux posés avant l'ajout (génération)
            p.shopkeeper = true;
            p.pharmacy = true;
        });
        // salle d'attente : bancs, distributeur, extincteur
        for (int x = 5; x <= 11; x += 3) {
            Deco.bench(b, x, 1, 6, Direction.NORTH);
        }
        for (int x = 20; x <= 26; x += 3) {
            Deco.bench(b, x, 1, 6, Direction.NORTH);
        }
        Deco.vendingMachine(b, 4, 1, 7, Direction.EAST);
        b.set(12, 1, 6, B.facing(ModBlocks.realism("wheelchair"), Direction.SOUTH));              // fauteuils roulants dans la salle d'attente
        b.set(19, 1, 6, B.facing(ModBlocks.realism("wheelchair"), Direction.SOUTH));
        b.set(10, 1, 3, B.facing(ModBlocks.realism("hospital_stretcher"), Direction.EAST));       // brancard à l'entrée des urgences
        Deco.extinguisher(b, 4, 2, 11);
        b.bot(8, 1, 5, Faction.CIVILIAN, BotRole.CIVILIAN, 4);
        b.bot(23, 1, 5, Faction.CIVILIAN, BotRole.CIVILIAN, 4);
        // couloir central z 10..11 ; salles de soins en dessous
        b.fill(4, 1, 12, 27, 4, 12, wall);
        for (int x : new int[]{9, 15, 21}) {
            b.fill(x, 1, 12, x + 1, 3, 12, Blocks.AIR.defaultBlockState());                       // portes des salles
        }
        // salle de soins : lits avec perfusions (partie droite)
        for (int x = 18; x <= 26; x += 4) {
            Deco.bed(b, x, 1, 14, Direction.SOUTH, Blocks.WHITE_BED);
            Deco.bed(b, x, 1, 21, Direction.NORTH, Blocks.WHITE_BED);
            drip(b, x + 1, 1, 14);
            drip(b, x + 1, 1, 22);
            monitor(b, x - 1, 1, 15, Direction.EAST);
            monitor(b, x - 1, 1, 21, Direction.EAST);
            b.set(x, 3, 17, Blocks.SEA_LANTERN);
        }
        b.fill(17, 1, 13, 17, 3, 24, Blocks.WHITE_CONCRETE);                                      // cloison
        b.fill(17, 1, 18, 17, 3, 19, Blocks.AIR.defaultBlockState());                             // passage
        b.bot(24, 1, 17, Faction.CIVILIAN, BotRole.CIVILIAN, 4);
        // bloc opératoire (milieu) : table, scialytique, plateau
        b.fill(10, 1, 13, 16, 3, 13, Blocks.WHITE_CONCRETE);
        b.fill(15, 1, 13, 16, 3, 13, Blocks.AIR.defaultBlockState());                              // porte du bloc opératoire
        b.fill(10, 1, 24, 16, 3, 24, Blocks.WHITE_CONCRETE);
        b.fill(10, 0, 14, 16, 0, 23, Blocks.LIGHT_BLUE_CONCRETE);
        b.fill(12, 1, 18, 14, 1, 19, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));                    // table d'opération
        b.fill(12, 2, 18, 14, 2, 19, Blocks.WHITE_CARPET.defaultBlockState());
        b.set(13, 3, 18, B.facing(ModBlocks.realism("surgical_light"), Direction.NORTH));        // scialytiques au-dessus de la table
        b.set(13, 3, 19, B.facing(ModBlocks.realism("surgical_light"), Direction.NORTH));
        b.set(13, 4, 18, Blocks.SEA_LANTERN);
        b.set(13, 4, 19, Blocks.SEA_LANTERN);
        b.set(11, 1, 22, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));                                // plateau d'instruments
        b.set(11, 2, 22, Blocks.BREWING_STAND.defaultBlockState());
        b.chest(15, 1, 22, Direction.WEST, "hospital_pharmacy", false);
        // scanner (gauche) : anneau de béton, lit coulissant
        b.fill(5, 1, 17, 8, 4, 17, Blocks.WHITE_CONCRETE);
        b.fill(6, 1, 17, 7, 3, 17, Blocks.AIR.defaultBlockState());
        b.fill(6, 1, 14, 7, 1, 20, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
        b.set(5, 2, 14, B.facing(ModBlocks.SURVEILLANCE_MONITOR.get(), Direction.EAST));
        b.set(5, 1, 13, Blocks.IRON_BLOCK);
        b.fill(4, 1, 13, 9, 3, 13, Blocks.WHITE_CONCRETE);
        b.fill(9, 1, 13, 9, 3, 13, Blocks.AIR.defaultBlockState());                                // porte du scanner
        b.fill(9, 1, 14, 9, 3, 24, Blocks.WHITE_CONCRETE);
        b.set(5, 3, 20, Blocks.SEA_LANTERN);
        b.bot(7, 1, 22, Faction.CIVILIAN, BotRole.CIVILIAN, 3);

        // ---------------- 1er étage : chambres, bureaux, réserve
        int y1 = 5;
        b.fill(4, y1 + 1, 12, 27, y1 + 4, 12, wall);
        for (int x : new int[]{9, 15, 21}) {
            b.fill(x, y1 + 1, 12, x + 1, y1 + 3, 12, Blocks.AIR.defaultBlockState());
        }
        for (int x = 5; x <= 25; x += 5) {
            Deco.bed(b, x, y1 + 1, 14, Direction.SOUTH, Blocks.WHITE_BED);                       // chambres au nord
            drip(b, x + 1, y1 + 1, 14);
            monitor(b, x - 1, y1 + 1, 15, Direction.EAST);
            Deco.bed(b, x, y1 + 1, 22, Direction.NORTH, Blocks.WHITE_BED);                       // chambres au sud
            drip(b, x + 1, y1 + 1, 22);
            b.fill(x + 3, y1 + 1, 13, x + 3, y1 + 3, 24, Blocks.WHITE_CONCRETE);                  // cloisons de chambres
            b.fill(x + 3, y1 + 1, 18, x + 3, y1 + 2, 19, Blocks.AIR.defaultBlockState());
        }
        for (int x = 6; x <= 24; x += 6) {                                                        // bureaux de médecins le long de l'accueil
            Deco.desk(b, x, y1 + 1, 7, Direction.SOUTH);
        }
        b.set(8, y1 + 3, 5, B.facing(ModBlocks.WALL_CLOCK.get(), Direction.SOUTH));
        // réserve de médicaments (coin) : coffres de butin
        b.fill(21, y1 + 1, 8, 21, y1 + 4, 11, wall);
        b.chest(26, y1 + 1, 9, Direction.WEST, "hospital_pharmacy", false);
        b.chest(26, y1 + 1, 10, Direction.WEST, "hospital_pharmacy", false);
        b.chest(24, y1 + 1, 8, Direction.SOUTH, "hospital_pharmacy", false);
        Deco.extinguisher(b, 22, y1 + 2, 11);
        b.set(22, y1 + 1, 8, B.facing(ModBlocks.realism("medical_cabinet"), Direction.SOUTH));    // armoires à pharmacie
        b.set(26, y1 + 1, 8, B.facing(ModBlocks.realism("medical_cabinet"), Direction.SOUTH));
        for (int x = 7; x <= 23; x += 8) {
            b.set(x, y1 + 4, 10, Blocks.SEA_LANTERN);
        }
        for (int x = 6; x <= 26; x += 5) {                                                        // éclairage des chambres
            b.set(x, y1 + 4, 16, Blocks.SEA_LANTERN);
            b.set(x, y1 + 4, 20, Blocks.SEA_LANTERN);
        }

        // ---------------- toit : héliport
        b.fill(11, 9, 11, 20, 9, 20, Blocks.GRAY_CONCRETE);
        b.fill(14, 9, 12, 14, 9, 19, Blocks.YELLOW_CONCRETE);                                     // « H »
        b.fill(17, 9, 12, 17, 9, 19, Blocks.YELLOW_CONCRETE);
        b.fill(14, 9, 15, 17, 9, 16, Blocks.YELLOW_CONCRETE);
        b.set(27, 9, 25, Blocks.AIR);
        Deco.antenna(b, 5, 10, 6, 4);

        // lumières du couloir
        for (int x = 6; x <= 26; x += 5) {
            b.set(x, 4, 10, Blocks.SEA_LANTERN);
        }
    }

    /** Pied à perfusion à côté du lit. */
    private static void drip(B b, int x, int y, int z) {
        b.set(x, y, z, B.facing(ModBlocks.realism("iv_stand"), Direction.WEST));
    }

    /** Moniteur cardiaque à la tête du lit, écran tourné vers le lit. */
    private static void monitor(B b, int x, int y, int z, Direction toBed) {
        b.set(x, y, z, B.facing(ModBlocks.realism("heart_monitor"), toBed));
    }

    private Hospital() {}
}

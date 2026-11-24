package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Métro de la métropole : une ligne en double voie sous l'avenue est-ouest centrale (rangée de chunks dz = 0), 12 blocs sous les égouts.
 * <ul>
 * <li>tunnel : voûte de béton, deux voies (rails sur ballast), néons de tunnel qui vacillent, ventilateurs, armoires électriques ;</li>
 * <li>stations (tous les 6 chunks) : quai central, bancs, rame garée sur la voie nord, portillons et panneaux de sortie ;
 *     accès par un puits à échelle depuis le trottoir (bouche de métro) ;</li>
 * <li>bout de ligne ouest : bunker blindé (porte à carte bleue, groupe électrogène, lits, caisses de ravitaillement).</li>
 * </ul>
 * Tout est à y -17 à -9 par rapport à la rue (sous les égouts, qui vont de -8 à -4).
 */
final class Metro {
    static final int FIRST = -9, LAST = 9;

    static boolean onLine(int dx, int dz) {
        return dz == 0 && dx >= FIRST && dx <= LAST;
    }

    static void build(B b, int dx) {
        boolean station = Math.floorMod(dx, 6) == 0;
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState dark = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        int yf = -16, yc = -9;                                                           // sol et plafond de la caverne
        // coque et intérieur
        b.fill(0, yf, 2, 15, yc, 13, wall);
        b.fill(0, yf + 1, 3, 15, yc - 1, 12, Blocks.AIR.defaultBlockState());
        b.fill(0, yf, 3, 15, yf, 12, dark);
        // voies (rails est-ouest sur ballast de gravier) en z 4 et z 11
        for (int z : new int[]{4, 11}) {
            b.fill(0, yf, z, 15, yf, z, Blocks.GRAVEL);
            for (int x = 0; x < 16; x++) {
                b.set(x, yf + 1, z, Blocks.RAIL.defaultBlockState().setValue(BlockStateProperties.RAIL_SHAPE, RailShape.EAST_WEST));
            }
        }
        // éclairage : néons de tunnel contre les parois, alternés
        for (int x = 2; x < 16; x += 6) {
            b.set(x, yc - 2, 3, B.facing(ModBlocks.realism("tunnel_light"), Direction.SOUTH));
            b.set(x + 3, yc - 2, 12, B.facing(ModBlocks.realism("tunnel_light"), Direction.NORTH));
        }
        b.set(8, yc - 1, 7, Blocks.SEA_LANTERN);
        // ventilateur et armoire électrique dans la paroi sud
        b.set(11, yf + 3, 12, B.facing(ModBlocks.realism("vent_fan"), Direction.NORTH));
        b.set(14, yf + 1, 12, B.facing(ModBlocks.realism("electrical_cabinet"), Direction.NORTH));
        if (station) {
            station(b);
        } else {
            // refuge de service : niche avec armoire et groupe électrogène (une fois sur deux)
            if (Math.floorMod(dx, 2) == 1) {
                b.set(5, yf + 1, 12, B.facing(ModBlocks.realism("emergency_generator"), Direction.NORTH));
            }
            b.set(1, yf + 4, 12, B.facing(ModBlocks.realism("exit_sign"), Direction.NORTH));
        }
        // fermeture aux deux bouts de la ligne
        if (dx == FIRST) {
            bunkerDoor(b);
        }
        if (dx == LAST) {
            b.fill(15, yf + 1, 3, 15, yc - 1, 12, Blocks.STONE_BRICKS.defaultBlockState());
        }
    }

    // ------------------------------------------------------------------ station
    private static void station(B b) {
        int yf = -16;
        BlockState quartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        // quai central (z 6..9) surélevé d'un bloc avec bande jaune de sécurité
        b.fill(0, yf + 1, 6, 15, yf + 1, 9, quartz);
        b.fill(0, yf + 1, 6, 15, yf + 1, 6, Blocks.YELLOW_CONCRETE);
        b.fill(0, yf + 1, 9, 15, yf + 1, 9, Blocks.YELLOW_CONCRETE);
        // traversée des voies au niveau de l'accès (dalles sur les rails, x 2..3 côté sud)
        b.fill(2, yf + 1, 11, 3, yf + 1, 11, quartz);
        b.fill(2, yf + 1, 10, 3, yf + 1, 10, quartz);
        // piliers
        for (int x = 2; x <= 14; x += 4) {
            b.fill(x, yf + 1, 7, x, yf + 6, 7, Blocks.QUARTZ_PILLAR);
            b.fill(x, yf + 1, 8, x, yf + 6, 8, Blocks.QUARTZ_PILLAR);
        }
        // mobilier : bancs, distributeur, panneaux, horloge, extincteur
        Deco.bench(b, 5, yf + 2, 7, Direction.NORTH);
        Deco.bench(b, 9, yf + 2, 8, Direction.SOUTH);
        Deco.vendingMachine(b, 13, yf + 2, 7, Direction.SOUTH);
        b.set(6, yf + 5, 6, B.facing(ModBlocks.realism("flight_board"), Direction.SOUTH));        // panneau des prochains passages
        b.set(10, yf + 5, 9, B.facing(ModBlocks.realism("flight_board"), Direction.NORTH));
        b.set(7, yf + 4, 3, B.facing(ModBlocks.NEON_SIGN_CYAN.get(), Direction.SOUTH));
        b.set(12, yf + 4, 12, B.facing(ModBlocks.WALL_CLOCK.get(), Direction.NORTH));
        Deco.extinguisher(b, 1, yf + 2, 7);
        // rame garée sur la voie nord (z 4) : caisse de béton, bande bleue, vitres, portes côté quai
        b.fill(1, yf + 2, 3, 14, yf + 5, 5, Blocks.LIGHT_GRAY_CONCRETE);
        b.fill(2, yf + 3, 4, 13, yf + 4, 4, Blocks.AIR.defaultBlockState());
        b.fill(1, yf + 3, 3, 14, yf + 3, 3, Blocks.BLUE_CONCRETE);
        b.fill(1, yf + 3, 5, 14, yf + 3, 5, Blocks.BLUE_CONCRETE);
        for (int x = 2; x <= 13; x++) {
            if (x != 7 && x != 8) {
                b.set(x, yf + 4, 5, Blocks.LIGHT_BLUE_STAINED_GLASS);
                b.set(x, yf + 4, 3, Blocks.LIGHT_BLUE_STAINED_GLASS);
            }
        }
        b.fill(7, yf + 2, 5, 8, yf + 4, 5, Blocks.AIR.defaultBlockState());                           // portes ouvertes côté quai
        b.fill(1, yf + 2, 4, 1, yf + 4, 4, Blocks.BLACK_CONCRETE);
        b.fill(14, yf + 2, 4, 14, yf + 4, 4, Blocks.BLACK_CONCRETE);
        for (int x : new int[]{3, 5, 10, 12}) {                                                       // banquettes
            b.set(x, yf + 3, 4, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
        }
        b.set(7, yf + 6, 4, Blocks.SEA_LANTERN);
        // accès : puits à échelle jusqu'au trottoir (angle sud-ouest de l'îlot), couloir, portillons et panneau de sortie
        b.fill(0, yf + 1, 14, 2, -2, 15, Blocks.STONE_BRICKS.defaultBlockState());                    // gaine maçonnée du puits
        b.fill(1, yf + 1, 14, 1, -2, 14, B.facing(Blocks.LADDER, Direction.NORTH));                    // échelle (appuyée sur le mur z 15)
        b.fill(0, yf + 1, 13, 3, yf + 4, 13, Blocks.AIR.defaultBlockState());                          // couloir vers la caverne
        b.set(1, yf + 4, 13, B.facing(ModBlocks.realism("exit_sign"), Direction.SOUTH));
        b.set(2, yf + 1, 12, B.facing(ModBlocks.realism("metro_turnstile"), Direction.NORTH));
        b.set(3, yf + 1, 12, B.facing(ModBlocks.realism("metro_turnstile"), Direction.NORTH));
        b.bot(5, yf + 2, 6, Faction.CIVILIAN, BotRole.CIVILIAN, 3);                                    // voyageurs
        b.bot(10, yf + 2, 9, Faction.CIVILIAN, BotRole.CIVILIAN, 3);
        // bouche de métro en surface : tampon et panneau
        b.set(1, -1, 14, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
        b.set(1, 0, 15, B.facing(ModBlocks.NEON_SIGN_CYAN.get(), Direction.SOUTH));
    }

    // ------------------------------------------------------------------ bunker de bout de ligne
    private static void bunkerDoor(B b) {
        int yf = -16;
        b.fill(0, yf + 1, 3, 0, -10, 12, Blocks.STONE_BRICKS.defaultBlockState());                    // mur de fond du tunnel
        CityGen.vaultDoor(b, 0, yf + 2, 7, Direction.EAST, 1);                                         // porte blindée à carte bleue
        CityGen.vaultDoor(b, 0, yf + 2, 8, Direction.EAST, 1);
    }

    /** Bunker (à l'ouest du dernier chunk du tunnel) : abri de 14 x 10 blocs. Construit par le chunk voisin dx = FIRST - 1. */
    static void bunker(B b) {
        int yf = -16, yc = -9;
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        b.fill(0, yf, 2, 15, yc, 13, wall);
        b.fill(1, yf + 1, 3, 15, yc - 1, 12, Blocks.AIR.defaultBlockState());
        b.fill(1, yf, 3, 15, yf, 12, Blocks.GRAY_CONCRETE);
        for (int x = 3; x <= 13; x += 5) {
            b.set(x, yc - 1, 5, Blocks.SEA_LANTERN);
            b.set(x, yc - 1, 10, Blocks.SEA_LANTERN);
        }
        b.set(14, yf + 1, 4, B.facing(ModBlocks.realism("emergency_generator"), Direction.WEST));
        b.set(14, yf + 1, 6, B.facing(ModBlocks.realism("electrical_cabinet"), Direction.WEST));
        b.set(14, yf + 1, 11, B.facing(ModBlocks.realism("medical_cabinet"), Direction.WEST));
        for (int z = 4; z <= 10; z += 3) {
            b.set(2, yf + 1, z, B.facing(ModBlocks.MILITARY_BUNK_BED_US.get(), Direction.EAST));
        }
        b.chest(8, yf + 1, 4, Direction.SOUTH, "military_crate", false);
        b.chest(9, yf + 1, 4, Direction.SOUTH, "military_crate", false);
        b.chest(8, yf + 1, 11, Direction.NORTH, "hospital_pharmacy", false);
        b.set(13, yf + 3, 3, B.facing(ModBlocks.realism("exit_sign"), Direction.SOUTH));
        b.set(15, yf + 2, 7, Blocks.AIR.defaultBlockState());
        b.set(15, yf + 2, 8, Blocks.AIR.defaultBlockState());
    }

    private Metro() {}
}

package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Aéroport civil de la métropole, en deux parcelles :
 * <ul>
 * <li>{@link #terminal} (2 x 2 chunks) : hall d'enregistrement (comptoirs, panneaux des vols), contrôle de sûreté (portiques,
 * scanners à rayons X), salle d'embarquement (comptoirs de porte, carrousels à bagages), boutique et salon à l'étage ;</li>
 * <li>{@link #airside} (8 x 3 chunks) : aire de stationnement (avions de ligne régionaux et avion léger, chariots à bagages,
 * citernes de kérosène), taxiway, piste de 128 blocs avec feux d'approche, manche à air et tour de contrôle.</li>
 * </ul>
 */
final class Airport {
    // =====================================================================================================
    // TERMINAL (32 x 32)
    // =====================================================================================================
    static void terminal(B b) {
        b.ground(0, 0, 31, 31, Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 18);
        BlockState wall = Blocks.WHITE_CONCRETE.defaultBlockState(), trim = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        for (int f = 0; f < 2; f++) {
            int y = f * 5;
            b.fill(2, y, 4, 29, y, 27, Blocks.SMOOTH_QUARTZ);
            b.walls(2, y, 4, 29, y + 4, 27, wall);
            b.walls(2, y + 4, 4, 29, y + 4, 27, trim);
            b.fill(4, y + 1, 4, 27, y + 3, 4, glass);                                        // façade vitrée côté ville
            b.fill(4, y + 1, 27, 27, y + 3, 27, glass);                                      // façade vitrée côté piste
            b.fill(2, y + 1, 7, 2, y + 3, 24, glass);
            b.fill(29, y + 1, 7, 29, y + 3, 24, glass);
        }
        b.fill(2, 9, 4, 29, 9, 27, trim);                                                     // toit plat
        // entrée : portes ouvertes, auvent, enseigne
        b.fill(13, 1, 4, 18, 3, 4, Blocks.AIR.defaultBlockState());
        b.fill(11, 4, 1, 20, 4, 3, trim);
        for (int x = 12; x <= 19; x++) {
            b.set(x, 5, 4, B.facing(ModBlocks.NEON_SIGN_CYAN.get(), Direction.NORTH));
        }
        b.fill(8, 1, 27, 10, 3, 27, Blocks.AIR.defaultBlockState());                          // sorties vers l'aire de stationnement
        b.fill(21, 1, 27, 23, 3, 27, Blocks.AIR.defaultBlockState());
        // échelle d'accès à l'étage
        for (int y = 1; y <= 8; y++) {
            b.set(28, y, 26, B.facing(Blocks.LADDER, Direction.NORTH));
        }
        b.set(28, 5, 26, B.facing(Blocks.LADDER, Direction.NORTH));
        b.set(28, 4, 26, Blocks.AIR);

        // ---------------- rez-de-chaussée : hall d'enregistrement (z 5..14)
        for (int x = 4; x <= 9; x++) {
            b.set(x, 1, 9, B.facing(ModBlocks.realism("check_in_desk"), Direction.NORTH));
        }
        for (int x = 22; x <= 27; x++) {
            b.set(x, 1, 9, B.facing(ModBlocks.realism("check_in_desk"), Direction.NORTH));
        }
        for (int x : new int[]{6, 13, 18, 25}) {
            b.set(x, 3, 5, B.facing(ModBlocks.realism("flight_board"), Direction.SOUTH));    // panneaux des vols au-dessus de l'accueil
        }
        b.set(10, 3, 5, B.facing(ModBlocks.WALL_CLOCK.get(), Direction.SOUTH));
        b.set(21, 3, 5, B.facing(ModBlocks.WALL_CLOCK.get(), Direction.SOUTH));
        Deco.vendingMachine(b, 3, 1, 12, Direction.EAST);
        Deco.extinguisher(b, 3, 2, 6);
        b.bot(7, 1, 11, Faction.CIVILIAN, BotRole.CIVILIAN, 4);                              // agent d'enregistrement et voyageurs
        b.bot(24, 1, 11, Faction.CIVILIAN, BotRole.CIVILIAN, 4);
        b.bot(16, 1, 8, Faction.CIVILIAN, BotRole.CIVILIAN, 6);
        // cloison de sûreté (z 15) : deux files avec portique et scanner à rayons X
        b.fill(3, 1, 15, 28, 4, 15, wall);
        for (int x : new int[]{12, 17}) {
            b.fill(x, 1, 15, x + 2, 3, 15, Blocks.AIR.defaultBlockState());
            b.set(x, 2, 15, ModBlocks.METAL_DETECTOR.get());                                   // portique détecteur de métaux
            b.set(x + 1, 2, 15, ModBlocks.METAL_DETECTOR.get());
            b.set(x - 1, 1, 14, B.facing(ModBlocks.realism("xray_scanner"), Direction.NORTH)); // scanner à bagages
            b.set(x + 2, 1, 14, B.facing(ModBlocks.realism("suitcase_pile"), Direction.SOUTH));
        }
        b.bot(15, 1, 16, Faction.POLICE, BotRole.SOLDIER, 3);                                  // agent de sûreté
        for (int x = 4; x <= 26; x += 11) {
            Deco.camera(b, x, 4, 14, Direction.SOUTH);
        }

        // ---------------- salle d'embarquement (z 16..26)
        for (int x : new int[]{5, 15, 25}) {
            b.set(x, 1, 18, B.facing(ModBlocks.realism("boarding_gate_desk"), Direction.SOUTH));
            b.set(x, 3, 16, B.facing(ModBlocks.realism("flight_board"), Direction.SOUTH));
        }
        for (int x = 5; x <= 25; x += 10) {
            Deco.bench(b, x, 1, 21, Direction.SOUTH);
            Deco.bench(b, x + 3, 1, 21, Direction.SOUTH);
        }
        for (int x : new int[]{5, 24}) {                                                       // carrousels à bagages avec valises
            b.set(x, 1, 25, B.facing(ModBlocks.realism("baggage_carousel"), Direction.NORTH));
            b.set(x + 1, 1, 25, B.facing(ModBlocks.realism("baggage_carousel"), Direction.NORTH));
            b.set(x, 2, 25, B.facing(ModBlocks.realism("suitcase_pile"), Direction.NORTH));
        }
        b.set(13, 1, 25, B.facing(ModBlocks.realism("baggage_cart"), Direction.EAST));
        b.set(18, 1, 25, B.facing(ModBlocks.realism("baggage_cart"), Direction.WEST));
        for (int x = 7; x <= 24; x += 6) {
            b.set(x, 4, 21, Blocks.SEA_LANTERN);
        }
        for (int x = 7; x <= 24; x += 6) {
            b.set(x, 4, 10, Blocks.SEA_LANTERN);
        }
        b.bot(10, 1, 22, Faction.CIVILIAN, BotRole.CIVILIAN, 5);
        b.bot(20, 1, 22, Faction.CIVILIAN, BotRole.CIVILIAN, 5);
        b.set(14, 3, 26, B.facing(ModBlocks.WALL_CLOCK.get(), Direction.NORTH));

        // ---------------- 1er étage : boutique hors taxes et salon
        int y1 = 5;
        b.fill(3, y1 + 1, 12, 28, y1 + 4, 12, wall);
        b.fill(14, y1 + 1, 12, 17, y1 + 3, 12, Blocks.AIR.defaultBlockState());
        for (int x = 5; x <= 25; x += 5) {
            b.chest(x, y1 + 1, 6, Direction.SOUTH, "city_shop", true);
            b.set(x, y1 + 2, 6, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.SOUTH));
        }
        b.set(15, y1 + 1, 9, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
        b.set(16, y1 + 1, 9, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
        CityGen.cashRegister(b, 15, y1 + 2, 9, Direction.SOUTH);
        CityGen.shopkeeper(b, 15, y1 + 1, 8);
        for (int x = 5; x <= 25; x += 5) {                                                     // salon : bancs et fenêtre sur la piste
            Deco.bench(b, x, y1 + 1, 24, Direction.SOUTH);
            b.set(x, y1 + 4, 18, Blocks.SEA_LANTERN);
        }
        b.set(14, y1 + 3, 14, B.facing(ModBlocks.realism("flight_board"), Direction.SOUTH));
        Deco.vendingMachine(b, 27, y1 + 1, 14, Direction.WEST);
        // toit : antenne radio
        Deco.antenna(b, 15, 10, 15, 6);
    }

    // =====================================================================================================
    // AIRE DE STATIONNEMENT, TAXIWAY ET PISTE (128 x 48)
    // =====================================================================================================
    static void airside(B b) {
        final int len = 128;
        b.ground(0, 0, len - 1, 47, Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 22);
        // aire de stationnement (z 0..25) : béton clair, postes de stationnement marqués en jaune
        b.fill(0, -1, 0, len - 1, -1, 25, Blocks.LIGHT_GRAY_CONCRETE);
        for (int cx : new int[]{30, 62, 95}) {
            b.fill(cx - 14, -1, 0, cx + 14, -1, 0, Blocks.YELLOW_CONCRETE);
            b.fill(cx - 14, -1, 25, cx + 14, -1, 25, Blocks.YELLOW_CONCRETE);
            b.fill(cx - 14, -1, 0, cx - 14, -1, 25, Blocks.YELLOW_CONCRETE);
            b.fill(cx + 14, -1, 0, cx + 14, -1, 25, Blocks.YELLOW_CONCRETE);
            b.fill(cx, -1, 13, cx, -1, 25, Blocks.YELLOW_CONCRETE);                              // ligne d'entrée du poste
        }
        // taxiway (z 26..30) avec ligne médiane jaune
        b.fill(0, -1, 26, len - 1, -1, 30, Blocks.GRAY_CONCRETE);
        for (int x = 0; x < len; x++) {
            if (x % 6 < 4) {
                b.set(x, -1, 28, Blocks.YELLOW_CONCRETE);
            }
        }
        // piste (z 31..46) : bitume, seuils, ligne médiane, feux de bord
        b.fill(0, -1, 31, len - 1, -1, 46, Blocks.BLACK_CONCRETE);
        for (int x = 0; x < len; x++) {
            for (int z = 31; z <= 46; z++) {
                boolean threshold = (x < 8 || x >= len - 8) && (z - 31) % 3 != 2 && z > 32 && z < 45;
                boolean centre = (z == 38 || z == 39) && x % 10 < 6 && x >= 12 && x < len - 12;
                boolean edge = (z == 31 || z == 46) && x % 2 == 0;
                if (threshold || centre || edge) {
                    b.set(x, -1, z, Blocks.WHITE_CONCRETE);
                }
            }
            if (x % 8 == 4) {
                b.set(x, 0, 31, B.facing(ModBlocks.realism("runway_light"), Direction.NORTH));      // feux de bord de piste
                b.set(x, 0, 46, B.facing(ModBlocks.realism("runway_light"), Direction.SOUTH));
            }
        }
        for (int z = 33; z <= 44; z += 3) {                                                      // feux d'approche aux extrémités
            b.set(0, 0, z, B.facing(ModBlocks.realism("runway_light"), Direction.EAST));
            b.set(len - 1, 0, z, B.facing(ModBlocks.realism("runway_light"), Direction.WEST));
        }
        // avions garés : deux avions de ligne régionaux et un avion léger, ouverts aux passagers
        b.milVehicle(30, 0, 12, MilType.REGIONAL_AIRLINER, 90);
        b.milVehicle(95, 0, 12, MilType.REGIONAL_AIRLINER, 90);
        b.milVehicle(62, 0, 14, MilType.LIGHT_CIVIL_PLANE, 90);
        // chariots à bagages, cônes, valises au pied des avions
        for (int cx : new int[]{30, 95}) {
            b.set(cx + 6, 0, 21, B.facing(ModBlocks.realism("baggage_cart"), Direction.EAST));
            b.set(cx + 8, 0, 21, B.facing(ModBlocks.realism("baggage_cart"), Direction.EAST));
            b.set(cx - 7, 0, 22, B.facing(ModBlocks.realism("suitcase_pile"), Direction.NORTH));
            for (int dz : new int[]{4, 21}) {
                b.set(cx - 12, 0, dz, ModBlocks.TRAFFIC_CONE.get());
                b.set(cx + 12, 0, dz, ModBlocks.TRAFFIC_CONE.get());
            }
            b.bot(cx + 4, 0, 18, Faction.CIVILIAN, BotRole.CIVILIAN, 6);                         // personnel au sol
        }
        b.bot(60, 0, 20, Faction.CIVILIAN, BotRole.CIVILIAN, 6);
        // dépôt de kérosène (ravitaillement) : réservoirs en bout d'aire
        for (int x = 116; x <= 124; x += 4) {
            b.set(x, 0, 3, ModBlocks.KEROSENE_TANK.get());
            b.set(x, 1, 3, ModBlocks.KEROSENE_TANK.get());
            b.fill(x - 1, -1, 2, x + 1, -1, 4, Blocks.YELLOW_CONCRETE);
        }
        // manche à air
        b.fill(64, 0, 42, 64, 5, 42, Blocks.IRON_BARS);
        b.set(64, 6, 42, Blocks.ORANGE_BANNER.defaultBlockState().setValue(net.minecraft.world.level.block.BannerBlock.ROTATION, 4));
        // tour de contrôle : fût, cabine vitrée, radar
        int tx = 6, tz = 4;
        b.walls(tx, 0, tz, tx + 4, 14, tz + 4, Blocks.WHITE_CONCRETE.defaultBlockState());
        b.fill(tx + 1, 0, tz + 1, tx + 3, 14, tz + 3, Blocks.AIR.defaultBlockState());
        b.fill(tx + 2, 0, tz + 1, tx + 2, 14, tz + 1, B.facing(Blocks.LADDER, Direction.SOUTH));
        b.set(tx + 2, 0, tz, Blocks.AIR);
        b.fill(tx - 1, 15, tz - 1, tx + 5, 15, tz + 5, Blocks.LIGHT_GRAY_CONCRETE);
        b.walls(tx - 1, 16, tz - 1, tx + 5, 18, tz + 5, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        b.fill(tx - 1, 19, tz - 1, tx + 5, 19, tz + 5, Blocks.LIGHT_GRAY_CONCRETE);
        b.set(tx + 2, 16, tz + 4, B.facing(ModBlocks.SURVEILLANCE_MONITOR.get(), Direction.NORTH));
        b.fill(tx + 2, 20, tz + 2, tx + 2, 23, tz + 2, Blocks.IRON_BARS);
        b.fill(tx, 24, tz + 2, tx + 4, 24, tz + 2, Blocks.IRON_BARS);                               // antenne radar
        b.bot(tx + 2, 16, tz + 2, Faction.CIVILIAN, BotRole.CIVILIAN, 2);                          // contrôleur aérien
        b.fill(tx + 1, 0, tz - 1, tx + 3, 0, tz - 1, Blocks.AIR.defaultBlockState());
    }

    private Airport() {}
}

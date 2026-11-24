package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.MountedGunEntity;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Construction des parcelles des villes (répartition par variante) + la métropole moderne (biome plaine).
 * Les autres variantes sont dans {@link CityGenRegions}.
 */
final class CityGen {
    static void build(B b, CityLayout layout, CityLayout.Plot plot, String variant, int dx, int dz, int rad) {
        switch (variant) {
            case "metropolis" -> metropolis(b, layout, plot, dx, dz, rad);
            default -> CityGenRegions.build(b, layout, plot, variant, dx, dz, rad);
        }
    }

    // =====================================================================================================
    // MÉTROPOLE MODERNE (plaine) : gratte-ciel, Central Park, commissariat, égouts, police municipale / SWAT
    // =====================================================================================================
    private static void metropolis(B b, CityLayout layout, CityLayout.Plot plot, int dx, int dz, int rad) {
        switch (plot.type()) {
            case "road" -> road(b, layout, dx, dz, rad);
            case "tower" -> tower(b, plot.above() - 6, layout.hash(dx, dz, 7));
            case "office" -> office(b, plot.above() - 4, layout.hash(dx, dz, 8));
            case "house" -> townhouse(b, layout.hash(dx, dz, 9));
            case "shop" -> shop(b, layout.hash(dx, dz, 10));
            case "cafe" -> cafe(b);
            case "supermarket" -> supermarket(b);
            case "gas_station" -> gasStation(b);
            case "park" -> park(b);
            case "police" -> policeStation(b);
            case "hospital" -> Hospital.build(b);
            case "apartment" -> apartment(b, plot.above() - 4, layout.hash(dx, dz, 11));
            case "warehouse" -> warehouse(b, layout.hash(dx, dz, 12));
            case "factory_m" -> factory(b, layout.hash(dx, dz, 13));
            case "plaza" -> plaza(b, layout.hash(dx, dz, 14));
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ routes, trottoirs, égouts
    private static void road(B b, CityLayout layout, int dx, int dz, int rad) {
        boolean ns = Math.floorMod(dx + rad, 3) == 0, ew = Math.floorMod(dz + rad, 3) == 0;
        b.ground(0, 0, 15, 15, Blocks.BLACK_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 10);
        // trottoirs du côté des parcelles construites
        boolean west = !layout.gridRoad(dx - 1, dz) || isBuilding(layout, dx - 1, dz), east = !layout.gridRoad(dx + 1, dz) || isBuilding(layout, dx + 1, dz);
        boolean north = !layout.gridRoad(dx, dz - 1) || isBuilding(layout, dx, dz - 1), south = !layout.gridRoad(dx, dz + 1) || isBuilding(layout, dx, dz + 1);
        BlockState walk = Blocks.SMOOTH_STONE.defaultBlockState();
        if (ns && !ew) {
            b.fill(0, -1, 0, 2, -1, 15, walk);
            b.fill(13, -1, 0, 15, -1, 15, walk);
            for (int z = 0; z < 16; z += 4) {
                b.fill(7, -1, z, 8, -1, z + 1, Blocks.YELLOW_CONCRETE);        // ligne médiane pointillée
            }
        } else if (ew && !ns) {
            b.fill(0, -1, 0, 15, -1, 2, walk);
            b.fill(0, -1, 13, 15, -1, 15, walk);
            for (int x = 0; x < 16; x += 4) {
                b.fill(x, -1, 7, x + 1, -1, 8, Blocks.YELLOW_CONCRETE);
            }
        } else {
            // carrefour : passages piétons et feux
            b.fill(0, -1, 0, 2, -1, 2, walk);
            b.fill(13, -1, 0, 15, -1, 2, walk);
            b.fill(0, -1, 13, 2, -1, 15, walk);
            b.fill(13, -1, 13, 15, -1, 15, walk);
            for (int i = 4; i <= 11; i += 2) {
                b.fill(i, -1, 0, i, -1, 2, Blocks.WHITE_CONCRETE);
                b.fill(i, -1, 13, i, -1, 15, Blocks.WHITE_CONCRETE);
                b.fill(0, -1, i, 2, -1, i, Blocks.WHITE_CONCRETE);
                b.fill(13, -1, i, 15, -1, i, Blocks.WHITE_CONCRETE);
            }
            Deco.trafficLight(b, 2, 0, 2);
            Deco.trafficLight(b, 13, 0, 13);
            Deco.trafficLight(b, 13, 0, 2);
            Deco.trafficLight(b, 2, 0, 13);
        }
        // bordures
        if (ns && !ew) {
            b.fill(3, -1, 0, 3, -1, 15, Blocks.LIGHT_GRAY_CONCRETE);
            b.fill(12, -1, 0, 12, -1, 15, Blocks.LIGHT_GRAY_CONCRETE);
        } else if (ew && !ns) {
            b.fill(0, -1, 3, 15, -1, 3, Blocks.LIGHT_GRAY_CONCRETE);
            b.fill(0, -1, 12, 15, -1, 12, Blocks.LIGHT_GRAY_CONCRETE);
        }
        // abribus de la ligne de bus du boulevard circulaire
        Direction stop = null;
        for (int i = 0; i < Traffic.BUS_STOPS.length; i++) {
            if (Traffic.BUS_STOPS[i][0] == dx && Traffic.BUS_STOPS[i][1] == dz) {
                stop = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}[i];
            }
        }
        if (stop != null) {
            Deco.busShelter(b, stop);
        }
        // décors des trottoirs
        if (ns != ew && stop == null) {
            int[][] spots = ns ? new int[][]{{1, 3}, {14, 11}, {1, 12}, {14, 4}} : new int[][]{{3, 1}, {11, 14}, {12, 1}, {4, 14}};
            Deco.lampPost(b, spots[0][0], 0, spots[0][1], ns ? Direction.EAST : Direction.SOUTH);
            Deco.lampPost(b, spots[1][0], 0, spots[1][1], ns ? Direction.WEST : Direction.NORTH);
            int kind = b.nextInt(9);
            int sx = spots[2][0], sz = spots[2][1];
            switch (kind) {
                case 0 -> Deco.hydrant(b, sx, 0, sz);
                case 1 -> Deco.bench(b, sx, 0, sz, ns ? (sx < 8 ? Direction.EAST : Direction.WEST) : (sz < 8 ? Direction.SOUTH : Direction.NORTH));
                case 2 -> Deco.busStop(b, ns ? 0 : sx - 1, 0, ns ? sz - 1 : 0);
                case 3 -> Deco.kiosk(b, sx, 0, sz);
                case 4 -> Deco.dumpster(b, sx, 0, sz);
                case 5 -> Deco.vendingMachine(b, sx, 0, sz, ns ? Direction.EAST : Direction.SOUTH);
                case 6 -> Deco.chargingStation(b, sx, 0, sz);
                case 7 -> Deco.billboard(b, sx, 0, sz, ns ? Direction.SOUTH : Direction.EAST);
                default -> Deco.trashBin(b, sx, 0, sz);
            }
            Deco.parkingMeter(b, spots[3][0], 0, spots[3][1]);
            Deco.hydrant(b, spots[3][0] + (ns ? 0 : 2), 0, spots[3][1] + (ns ? 2 : 0));
        }
        // voitures de police garées le long du trottoir
        if (ns != ew && b.chance(0.15f)) {
            b.vehicle(ns ? 5 : 8, 0, ns ? 8 : 5, fr.modeminecraft.entity.VehicleEntity.Type.POLICE_CAR, ns ? 0 : 90);
        }
        // bouche d'égout qui fume sur la chaussée
        if (ns != ew && b.chance(0.25f)) {
            b.set(ns ? 6 : 3, 0, ns ? 3 : 6, fr.modeminecraft.registry.ModBlocks.STEAM_VENT.get());
        }
        // égouts sous la chaussée
        sewer(b, ns || !ew, ew || !ns, ns && ew);
        // habitants et police
        // (densités réduites : une métropole de 31 x 31 chunks comptait sinon ~400 habitants, trop lourd)
        if (ns && ew) {
            if (b.chance(0.08f)) {
                b.pair(3, 0, 3, Faction.POLICE, 40);
            } else if (b.chance(0.12f)) {
                b.bot(3, 0, 3, Faction.POLICE, BotRole.SOLDIER, 40);
            }
            if (b.chance(0.15f)) {
                b.bot(7, -6, 7, Faction.TRADER, BotRole.TRADER, 2);                // marchand clandestin dans les égouts
            }
        } else if (b.chance(0.1f)) {
            b.bot(ns ? 1 : 7, 0, ns ? 7 : 1, Faction.CIVILIAN, BotRole.CIVILIAN, 24);
        }
    }

    private static boolean isBuilding(CityLayout layout, int dx, int dz) {
        CityLayout.Plot p = layout.plot(dx, dz);
        return p != null && !p.type().equals("road");
    }

    /** Tunnel d'égout (3 de large, 3 de haut) avec caniveau, éclairage et bouches d'égout. */
    private static void sewer(B b, boolean ns, boolean ew, boolean cross) {
        BlockState wall = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        int y0 = -8, y1 = -4;
        if (ns) {
            b.fill(5, y0, 0, 10, y1, 15, wall);
            b.fill(6, y0 + 1, 0, 9, y1 - 1, 15, Blocks.AIR.defaultBlockState());
            b.fill(7, y0, 0, 8, y0, 15, Blocks.WATER.defaultBlockState());
        }
        if (ew) {
            b.fill(0, y0, 5, 15, y1, 10, wall);
            b.fill(0, y0 + 1, 6, 15, y1 - 1, 9, Blocks.AIR.defaultBlockState());
            b.fill(0, y0, 7, 15, y0, 8, Blocks.WATER.defaultBlockState());
        }
        if (ns) {
            b.fill(6, y0 + 1, 0, 6, y0 + 1, 15, Blocks.AIR.defaultBlockState());
        }
        b.set(6, y1 - 1, 4, Blocks.GLOWSTONE);
        b.set(9, y1 - 1, 12, Blocks.GLOWSTONE);
        b.set(4, y1 - 1, 6, Blocks.GLOWSTONE);
        b.set(12, y1 - 1, 9, Blocks.GLOWSTONE);
        if (cross || b.chance(0.3f)) {
            // bouche d'égout : échelle jusqu'à la rue
            int lx = ns ? 6 : 3, lz = ns ? 3 : 6;
            b.fill(lx, y0 + 1, lz, lx, -1, lz, Blocks.AIR.defaultBlockState());
            b.fill(lx, y0 + 1, lz, lx, -2, lz, B.facing(Blocks.LADDER, ns ? Direction.EAST : Direction.SOUTH));
            b.set(lx + (ns ? -1 : 0), y0 + 1, lz + (ns ? 0 : -1), wall);
            b.set(lx, -1, lz, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
            for (int y = y0 + 1; y <= -2; y++) {
                if (!(ns && y <= y1 - 1) && !(ew && y <= y1 - 1)) {
                    b.set(lx + (ns ? -1 : 0), y, lz + (ns ? 0 : -1), Blocks.STONE_BRICKS);
                }
            }
        }
    }

    // ------------------------------------------------------------------ gratte-ciel
    private static final Block[][] TOWER_STYLES = {
            {Blocks.QUARTZ_PILLAR, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.WHITE_CONCRETE},
            {Blocks.POLISHED_BLACKSTONE, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.POLISHED_BLACKSTONE_BRICKS},
            {Blocks.POLISHED_DEEPSLATE, Blocks.CYAN_STAINED_GLASS_PANE, Blocks.DEEPSLATE_TILES},
            {Blocks.SMOOTH_STONE, Blocks.BLUE_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_CONCRETE},
            {Blocks.WHITE_CONCRETE, Blocks.BLACK_STAINED_GLASS_PANE, Blocks.GRAY_CONCRETE}};

    /** Gratte-ciel : hall en marbre, étages de bureaux, salle de réunion, penthouse, héliport. */
    private static void tower(B b, int height, double h) {
        Block[] st = TOWER_STYLES[(int) (h * TOWER_STYLES.length) % TOWER_STYLES.length];
        int floors = Math.max(8, height / 4);
        int top = floors * 4;
        b.ground(0, 0, 15, 15, Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), top + 6);
        // structure : piliers d'angle, murs-rideaux, bandeaux d'étage
        for (int f = 0; f < floors; f++) {
            int y = f * 4;
            b.fill(1, y, 1, 14, y, 14, f == 0 ? Blocks.POLISHED_DIORITE.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
            b.walls(1, y, 1, 14, y, 14, st[2].defaultBlockState());
            b.walls(1, y + 1, 1, 14, y + 3, 14, st[1].defaultBlockState());
            for (int[] c : new int[][]{{1, 1}, {14, 1}, {1, 14}, {14, 14}, {7, 1}, {8, 1}, {7, 14}, {8, 14}, {1, 7}, {1, 8}, {14, 7}, {14, 8}}) {
                b.fill(c[0], y + 1, c[1], c[0], y + 3, c[1], st[0].defaultBlockState());
            }
            // plafonniers
            for (int x = 3; x <= 12; x += 4) {
                for (int z = 3; z <= 12; z += 4) {
                    b.set(x, y + 3, z, Blocks.SEA_LANTERN);
                }
            }
        }
        b.fill(1, top, 1, 14, top, 14, st[2].defaultBlockState());
        // noyau : cage d'escalier (échelles) au centre
        for (int f = 0; f < floors; f++) {
            int y = f * 4;
            b.walls(6, y + 1, 6, 9, y + 3, 9, Blocks.STONE_BRICKS.defaultBlockState());
            b.fill(7, y + 1, 6, 7, y + 2, 6, Blocks.AIR.defaultBlockState());                 // porte du noyau
            b.fill(8, y, 8, 8, y + 3, 8, B.facing(Blocks.LADDER, Direction.NORTH));
            b.set(8, y, 8, B.facing(Blocks.LADDER, Direction.NORTH));
        }
        b.fill(8, top, 8, 8, top, 8, B.facing(Blocks.LADDER, Direction.NORTH));
        b.set(8, top + 1, 8, Blocks.AIR);
        // écran géant publicitaire sur la façade nord (4 x 2 blocs)
        if (h < 0.4) {
            for (int part = 0; part < 8; part++) {
                b.set(10 - part % 4, 10 - part / 4, 0, fr.modeminecraft.registry.ModBlocks.GIANT_SCREEN.get().defaultBlockState()
                        .setValue(fr.modeminecraft.block.city.GiantScreenBlock.FACING, Direction.NORTH)
                        .setValue(fr.modeminecraft.block.city.GiantScreenBlock.PART, part));
            }
        }
        // hall du rez-de-chaussée : portes vitrées, accueil, portique de sécurité, plantes
        b.fill(6, 1, 1, 9, 3, 1, Blocks.AIR.defaultBlockState());
        b.fill(6, 1, 14, 9, 3, 14, Blocks.AIR.defaultBlockState());
        b.fill(3, 1, 4, 5, 1, 4, B.slab(Blocks.POLISHED_DIORITE_SLAB, true));               // banque d'accueil
        b.set(4, 1, 5, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
        b.fill(6, 1, 3, 6, 2, 3, Blocks.IRON_BARS);                                          // portique détecteur de métaux
        b.fill(9, 1, 3, 9, 2, 3, Blocks.IRON_BARS);
        b.fill(7, 2, 3, 8, 2, 3, fr.modeminecraft.registry.ModBlocks.METAL_DETECTOR.get().defaultBlockState());
        b.set(2, 1, 2, Blocks.POTTED_FERN);
        b.set(13, 1, 2, Blocks.POTTED_FERN);
        Deco.camera(b, 2, 3, 13, Direction.SOUTH);
        Deco.extinguisher(b, 13, 1, 13);
        if (b.chance(0.4f)) {
            b.bot(4, 1, 6, Faction.CIVILIAN, BotRole.CIVILIAN, 6);
        }
        // étages : open-spaces, salle de réunion, penthouse
        for (int f = 1; f < floors; f++) {
            int y = f * 4 + 1;
            if (f == floors - 1) {
                // penthouse
                b.fill(2, y - 1, 2, 13, y - 1, 13, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                b.fill(3, y, 3, 5, y, 3, Blocks.WHITE_CARPET.defaultBlockState());
                Deco.bed(b, 11, y, 3, Direction.SOUTH, Blocks.WHITE_BED);
                b.fill(2, y, 12, 4, y + 1, 12, Blocks.BOOKSHELF.defaultBlockState());
                b.set(12, y, 12, Blocks.JUKEBOX);
                b.chest(13, y, 12, Direction.WEST, "city_safe", false);
                b.fill(3, y, 7, 4, y, 9, B.stairs(Blocks.QUARTZ_STAIRS, Direction.EAST));
            } else if (f % 3 == 2) {
                // salle de réunion
                b.fill(3, y, 10, 12, y, 11, B.slab(Blocks.DARK_OAK_SLAB, true));
                for (int x = 3; x <= 12; x += 2) {
                    b.set(x, y, 9, B.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
                    b.set(x, y, 12, B.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
                }
                b.set(13, y, 10, Blocks.WHITE_BANNER);
            } else {
                // open-space : rangées de bureaux avec ordinateurs
                for (int x = 3; x <= 12; x += 3) {
                    for (int z : new int[]{3, 12}) {
                        Deco.desk(b, x, y, z, z < 8 ? Direction.SOUTH : Direction.NORTH);
                    }
                }
                b.set(13, y, 7, Blocks.POTTED_BAMBOO);
                b.chest(2, y, 8, Direction.EAST, "city_office", true);
            }
            Deco.extinguisher(b, 13, y, 13);
        }
        // toit : héliport, climatiseurs, antenne, nacelle de laveur de vitres
        int ry = top + 1;
        if (floors >= 12) {
            b.fill(3, top, 3, 12, top, 12, Blocks.GRAY_CONCRETE);
            b.fill(5, top, 5, 5, top, 10, Blocks.YELLOW_CONCRETE);
            b.fill(10, top, 5, 10, top, 10, Blocks.YELLOW_CONCRETE);
            b.fill(5, top, 7, 10, top, 8, Blocks.YELLOW_CONCRETE);
        }
        b.walls(1, ry, 1, 14, ry, 14, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        Deco.airConditioner(b, 2, ry, 2);
        Deco.airConditioner(b, 12, ry, 2);
        Deco.antenna(b, 13, ry, 13, 6 + (int) (h * 8));
        if (h > 0.5) {
            int gy = top - 3 - (int) (h * 10) % Math.max(1, top - 8);
            b.fill(4, gy, 0, 9, gy, 0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
            b.fill(4, gy + 1, 0, 9, gy + 1, 0, Blocks.IRON_BARS.defaultBlockState());
            b.fill(4, gy + 2, 0, 4, top + 1, 0, Blocks.CHAIN.defaultBlockState());
            b.fill(9, gy + 2, 0, 9, top + 1, 0, Blocks.CHAIN.defaultBlockState());
        }
    }

    /** Immeuble de bureaux en brique (4 à 9 étages). */
    private static void office(B b, int height, double h) {
        Block wall = h < 0.33 ? Blocks.BRICKS : h < 0.66 ? Blocks.STONE_BRICKS : Blocks.TERRACOTTA;
        int floors = Math.max(3, height / 4);
        int top = floors * 4;
        b.ground(0, 0, 15, 15, Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), top + 4);
        for (int f = 0; f < floors; f++) {
            int y = f * 4;
            b.fill(2, y, 2, 13, y, 13, Blocks.SPRUCE_PLANKS);
            b.walls(2, y, 2, 13, y + 3, 13, wall.defaultBlockState());
            for (int i = 4; i <= 11; i += 3) {
                b.fill(i, y + 1, 2, i + 1, y + 2, 2, Blocks.GLASS_PANE.defaultBlockState());
                b.fill(i, y + 1, 13, i + 1, y + 2, 13, Blocks.GLASS_PANE.defaultBlockState());
                b.fill(2, y + 1, i, 2, y + 2, i + 1, Blocks.GLASS_PANE.defaultBlockState());
                b.fill(13, y + 1, i, 13, y + 2, i + 1, Blocks.GLASS_PANE.defaultBlockState());
            }
            b.set(7, y + 3, 7, Blocks.GLOWSTONE);
            b.fill(12, y, 12, 12, y + 3, 12, B.facing(Blocks.LADDER, Direction.WEST));
            if (f > 0) {
                Deco.desk(b, 4, y + 1, 5, Direction.SOUTH);
                Deco.desk(b, 8, y + 1, 5, Direction.SOUTH);
                b.chest(4, y + 1, 11, Direction.NORTH, "city_office", true);
            }
        }
        b.fill(2, top, 2, 13, top, 13, wall.defaultBlockState());
        b.fill(12, top, 12, 12, top, 12, B.facing(Blocks.LADDER, Direction.WEST));
        b.set(12, top + 1, 12, Blocks.AIR);
        b.fill(7, 1, 2, 8, 2, 2, Blocks.AIR.defaultBlockState());
        b.set(7, 1, 2, Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        b.set(7, 2, 2, Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        Deco.airConditioner(b, 4, top + 1, 4);
        Deco.antenna(b, 10, top + 1, 10, 3);
        if (h > 0.8) {
            Deco.scaffolding(b, 1, 1, 14, 1, Math.min(top, 10));                     // chantier en façade
        }
    }

    /** Immeuble d'habitation : appartements (lits, cuisine), cage d'escalier, balcons. */
    private static void apartment(B b, int height, double h) {
        Block wall = h < 0.3 ? Blocks.WHITE_CONCRETE : h < 0.6 ? Blocks.BRICKS : Blocks.LIGHT_GRAY_CONCRETE;
        Block balcony = h < 0.5 ? Blocks.IRON_BARS : Blocks.GLASS_PANE;
        int floors = Math.max(3, height / 4);
        int top = floors * 4;
        b.ground(0, 0, 15, 15, Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), top + 4);
        for (int f = 0; f < floors; f++) {
            int y = f * 4;
            b.fill(2, y, 2, 13, y, 13, f == 0 ? Blocks.POLISHED_ANDESITE : Blocks.OAK_PLANKS);
            b.walls(2, y, 2, 13, y + 3, 13, wall.defaultBlockState());
            for (int i = 4; i <= 11; i += 4) {
                b.fill(i, y + 1, 2, i + 1, y + 2, 2, Blocks.GLASS);
                b.fill(i, y + 1, 13, i + 1, y + 2, 13, Blocks.GLASS);
                b.fill(2, y + 1, i, 2, y + 2, i + 1, Blocks.GLASS);
                b.fill(13, y + 1, i, 13, y + 2, i + 1, Blocks.GLASS);
            }
            b.set(7, y + 3, 7, Blocks.GLOWSTONE);
            b.fill(12, y, 12, 12, y + 3, 12, B.facing(Blocks.LADDER, Direction.WEST));
            if (f > 0) {
                b.fill(3, y + 1, 7, 11, y + 3, 7, wall.defaultBlockState());         // deux appartements par étage
                b.fill(6, y + 1, 7, 6, y + 2, 7, Blocks.AIR.defaultBlockState());
                Deco.bed(b, 3, y + 1, 3, Direction.EAST, b.chance(0.5f) ? Blocks.BLUE_BED : Blocks.RED_BED);
                Deco.kitchen(b, 8, y + 1, 3);
                Deco.bed(b, 3, y + 1, 11, Direction.EAST, b.chance(0.5f) ? Blocks.LIGHT_GRAY_BED : Blocks.GREEN_BED);
                b.chest(9, y + 1, 11, Direction.NORTH, "city_house", false);
                b.fill(5, y, 1, 10, y, 1, B.slab(Blocks.SMOOTH_STONE_SLAB, true));     // balcon
                b.fill(5, y + 1, 1, 10, y + 1, 1, balcony);
            }
        }
        b.fill(2, top, 2, 13, top, 13, wall.defaultBlockState());
        b.fill(12, top, 12, 12, top, 12, B.facing(Blocks.LADDER, Direction.WEST));
        b.set(12, top + 1, 12, Blocks.AIR);
        b.fill(7, 1, 2, 8, 2, 2, Blocks.AIR.defaultBlockState());
        Deco.door(b, 7, 1, 2, Direction.NORTH, Blocks.SPRUCE_DOOR);
        Deco.airConditioner(b, 4, top + 1, 4);
        Deco.antenna(b, 10, top + 1, 10, 2);
        if (b.chance(0.2f)) {
            b.bot(7, 1, 5, Faction.CIVILIAN, BotRole.CIVILIAN, 10);
        }
    }

    /** Entrepôt de la zone industrielle : grand hall, rideau métallique, rayonnages, quai de chargement. */
    private static void warehouse(B b, double h) {
        b.ground(0, 0, 15, 15, Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 18);
        Block wall = h < 0.3 ? Blocks.LIGHT_GRAY_CONCRETE : h < 0.6 ? Blocks.CYAN_TERRACOTTA : Blocks.IRON_BLOCK;
        b.fill(1, 0, 1, 14, 0, 14, Blocks.SMOOTH_STONE);
        b.walls(1, 0, 1, 14, 9, 14, wall.defaultBlockState());
        b.fill(1, 9, 1, 14, 9, 14, Blocks.LIGHT_GRAY_CONCRETE);
        for (int x = 2; x <= 13; x++) {                                               // toit en dents de scie (verrières)
            if (x % 3 == 0) b.fill(x, 9, 2, x, 9, 13, Blocks.GLASS);
        }
        b.fill(5, 1, 1, 10, 5, 1, Blocks.IRON_BARS);                                   // rideau métallique
        b.fill(6, 1, 1, 9, 3, 1, Blocks.AIR.defaultBlockState());
        for (int z = 4; z <= 11; z += 4) {                                             // rayonnages
            b.fill(3, 1, z, 12, 1, z, Blocks.BARREL.defaultBlockState());
            b.fill(3, 2, z, 12, 2, z, B.slab(Blocks.SPRUCE_SLAB, false));
            b.fill(3, 3, z, 12, 3, z, Blocks.BARREL.defaultBlockState());
        }
        b.chest(12, 1, 13, Direction.WEST, "city_shop", false);
        b.set(7, 8, 7, Blocks.SEA_LANTERN);
        CityGenRegions.forklift(b, 3, 1, 12);
        if (b.chance(0.25f)) {
            b.bot(7, 1, 2, Faction.CIVILIAN, BotRole.CIVILIAN, 12);
        }
    }

    /** Usine : hall de production, cheminées fumantes, cuves, convoyeur. */
    private static void factory(B b, double h) {
        b.ground(0, 0, 15, 15, Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 22);
        b.fill(1, 0, 3, 14, 0, 14, Blocks.SMOOTH_STONE);
        b.walls(1, 0, 3, 14, 7, 14, Blocks.BRICKS.defaultBlockState());
        b.fill(1, 7, 3, 14, 7, 14, Blocks.GRAY_CONCRETE);
        for (int i = 3; i <= 12; i += 3) {
            b.fill(i, 3, 3, i + 1, 5, 3, Blocks.GLASS);
            b.fill(i, 3, 14, i + 1, 5, 14, Blocks.GLASS);
        }
        b.fill(7, 1, 3, 8, 3, 3, Blocks.AIR.defaultBlockState());
        for (int[] c : new int[][]{{3, 12}, {12, 12}}) {                               // cheminées
            b.fill(c[0], 8, c[1], c[0] + 1, 18, c[1] + 1, Blocks.BRICKS);
            b.set(c[0], 19, c[1], Blocks.CAMPFIRE.defaultBlockState());
        }
        b.fill(3, 1, 7, 12, 1, 7, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());      // convoyeur
        b.fill(3, 1, 9, 4, 3, 10, Blocks.IRON_BLOCK);                                  // cuves
        b.fill(11, 1, 9, 12, 3, 10, Blocks.IRON_BLOCK);
        b.set(7, 6, 8, Blocks.SEA_LANTERN);
        b.chest(13, 1, 4, Direction.WEST, "city_shop", true);
        b.fill(0, 0, 0, 15, 0, 1, Blocks.GRAVEL);
    }

    /** Petite place : pavés, fontaine, arbres, bancs, lampadaires, kiosque. */
    private static void plaza(B b, double h) {
        b.ground(0, 0, 15, 15, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE.defaultBlockState(), 10);
        b.fill(2, -1, 2, 13, -1, 13, Blocks.POLISHED_ANDESITE);
        b.fill(6, -1, 6, 9, -1, 9, Blocks.STONE_BRICKS);
        b.fill(7, -1, 7, 8, -1, 8, Blocks.WATER);
        b.fill(6, 0, 6, 9, 0, 9, Blocks.STONE_BRICK_WALL);
        b.fill(7, 0, 7, 8, 0, 8, Blocks.AIR.defaultBlockState());
        b.set(7, 0, 7, Blocks.WATER);
        b.set(8, 0, 8, fr.modeminecraft.registry.ModBlocks.FOUNTAIN_SPOUT.get());
        for (int[] t : new int[][]{{2, 2}, {13, 2}, {2, 13}, {13, 13}}) {
            tree(b, t[0], t[1], 4 + (t[0] + t[1]) % 2);
        }
        Deco.bench(b, 4, 0, 7, Direction.EAST);
        Deco.bench(b, 11, 0, 8, Direction.WEST);
        Deco.lampPost(b, 7, 0, 3, Direction.SOUTH);
        Deco.lampPost(b, 8, 0, 12, Direction.NORTH);
        if (h < 0.5) {
            Deco.kiosk(b, 12, 0, 5);
        }
        if (b.chance(0.3f)) {
            b.bot(5, 0, 5, Faction.CIVILIAN, BotRole.CIVILIAN, 10);
        }
    }

    /** Maison de ville sur deux étages, avec jardin. */
    private static void townhouse(B b, double h) {
        Block wall = h < 0.5 ? Blocks.BRICKS : Blocks.WHITE_TERRACOTTA;
        b.ground(0, 0, 15, 15, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 14);
        b.fill(0, -1, 0, 15, -1, 1, Blocks.SMOOTH_STONE);
        for (int f = 0; f < 2; f++) {
            int y = f * 4;
            b.fill(3, y, 3, 12, y, 10, Blocks.OAK_PLANKS);
            b.walls(3, y, 3, 12, y + 3, 10, wall.defaultBlockState());
            b.fill(5, y + 1, 3, 6, y + 2, 3, Blocks.GLASS_PANE.defaultBlockState());
            b.fill(9, y + 1, 3, 10, y + 2, 3, Blocks.GLASS_PANE.defaultBlockState());
            b.fill(12, y + 1, 5, 12, y + 2, 8, Blocks.GLASS_PANE.defaultBlockState());
            b.set(7, y + 3, 6, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        }
        for (int i = 0; i <= 4; i++) {
            b.fill(2 + i, 8 + i, 2, 13 - i, 8 + i, 11, B.slab(Blocks.DARK_OAK_SLAB, false));
            b.fill(3 + i, 8 + i, 3, 12 - i, 8 + i, 10, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }
        b.set(7, 1, 3, Blocks.SPRUCE_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        b.set(7, 2, 3, Blocks.SPRUCE_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        Deco.kitchen(b, 4, 1, 9);
        b.fill(9, 1, 8, 10, 1, 8, Blocks.OAK_SLAB);
        b.set(11, 0, 5, Blocks.RED_CARPET);
        b.fill(11, 1, 9, 11, 4, 9, B.facing(Blocks.LADDER, Direction.WEST));
        b.set(11, 4, 9, B.facing(Blocks.LADDER, Direction.WEST));
        Deco.bed(b, 4, 5, 5, Direction.SOUTH, Blocks.RED_BED);
        b.chest(5, 5, 9, Direction.NORTH, "city_house", false);
        b.fill(0, 0, 12, 15, 0, 12, Blocks.OAK_FENCE);
        b.set(2, 0, 14, Blocks.ROSE_BUSH);
        b.set(13, 0, 14, Blocks.LILAC);
        if (h > 0.7) {
            b.bot(7, 1, 7, Faction.CIVILIAN, BotRole.CIVILIAN, 10);
        }
    }

    /** Commerce : vitrines, comptoir, rayonnages. */
    private static void shop(B b, double h) {
        Block wall = h < 0.5 ? Blocks.SMOOTH_SANDSTONE : Blocks.POLISHED_GRANITE;
        b.ground(0, 0, 15, 15, Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 10);
        b.fill(2, 0, 2, 13, 0, 13, Blocks.BIRCH_PLANKS);
        b.walls(2, 0, 2, 13, 5, 13, wall.defaultBlockState());
        b.fill(3, 1, 2, 12, 3, 2, Blocks.GLASS_PANE.defaultBlockState());
        b.fill(7, 1, 2, 8, 2, 2, Blocks.AIR.defaultBlockState());
        b.fill(2, 5, 2, 13, 5, 13, wall.defaultBlockState());
        b.fill(2, 4, 1, 13, 4, 1, (h < 0.5 ? Blocks.RED_WOOL : Blocks.BLUE_WOOL).defaultBlockState());   // auvent
        b.fill(4, 1, 6, 11, 1, 6, B.slab(Blocks.SPRUCE_SLAB, true));
        cashRegister(b, 6, 2, 6, Direction.NORTH);
        b.set(4, 3, 1, B.facing(fr.modeminecraft.registry.ModBlocks.NEON_SIGN.get(), Direction.NORTH));             // néon clignotant
        b.fill(3, 1, 10, 12, 4, 10, wall.defaultBlockState());                                       // arrière-boutique
        Deco.door(b, 7, 1, 10, Direction.SOUTH, fr.modeminecraft.registry.ModBlocks.LOCKED_WOODEN_DOOR.get());
        for (int x = 3; x <= 12; x++) {
            b.chest(x, 1, 12, Direction.NORTH, "city_shop", true);
            b.set(x, 2, 12, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        }
        b.set(7, 4, 7, Blocks.GLOWSTONE);
        shopkeeper(b, 7, 1, 8);
    }

    private static void cafe(B b) {
        b.ground(0, 0, 15, 15, Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 9);
        b.fill(4, 0, 6, 13, 0, 13, Blocks.SPRUCE_PLANKS);
        b.walls(4, 0, 6, 13, 4, 13, Blocks.SPRUCE_LOG.defaultBlockState());
        b.fill(5, 1, 6, 12, 2, 6, Blocks.GLASS_PANE.defaultBlockState());
        b.fill(8, 1, 6, 9, 2, 6, Blocks.AIR.defaultBlockState());
        b.fill(4, 4, 6, 13, 4, 13, Blocks.SPRUCE_SLAB.defaultBlockState());
        for (int x = 3; x <= 14; x++) {
            b.set(x, 4, 5, ((x % 2) == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL).defaultBlockState());   // store rayé
        }
        b.fill(5, 1, 10, 12, 1, 10, B.slab(Blocks.SPRUCE_SLAB, true));                                // comptoir
        b.set(6, 2, 10, Blocks.BREWING_STAND);                                                          // machine à café
        cashRegister(b, 10, 2, 10, Direction.NORTH);
        b.set(12, 3, 5, B.facing(fr.modeminecraft.registry.ModBlocks.NEON_SIGN_CYAN.get(), Direction.NORTH));
        b.set(8, 3, 12, B.facing(fr.modeminecraft.registry.ModBlocks.WALL_CLOCK.get(), Direction.NORTH));
        b.set(12, 1, 12, B.facing(fr.modeminecraft.registry.ModBlocks.AMBIENT_SPEAKER.get(), Direction.NORTH));   // musique d'ambiance
        b.set(10, 3, 9, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        for (int x = 3; x <= 12; x += 4) {                                                             // terrasse
            b.set(x, 0, 2, Blocks.OAK_FENCE);
            b.set(x, 1, 2, Blocks.OAK_PRESSURE_PLATE);
            b.set(x - 1, 0, 2, B.stairs(Blocks.OAK_STAIRS, Direction.WEST));
            b.set(x + 1, 0, 2, B.stairs(Blocks.OAK_STAIRS, Direction.EAST));
        }
        shopkeeper(b, 8, 1, 11);
    }

    /** Supermarché : rayons, caisses, parking. */
    private static void supermarket(B b) {
        b.ground(0, 0, 15, 15, Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 10);
        b.fill(1, 0, 6, 14, 0, 14, Blocks.WHITE_CONCRETE);
        b.walls(1, 0, 6, 14, 5, 14, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
        b.fill(2, 1, 6, 13, 3, 6, Blocks.GLASS_PANE.defaultBlockState());
        b.fill(6, 1, 6, 9, 3, 6, Blocks.AIR.defaultBlockState());
        b.fill(1, 5, 6, 14, 5, 14, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
        b.fill(3, 4, 5, 12, 4, 5, Blocks.ORANGE_CONCRETE.defaultBlockState());                          // enseigne
        for (int z = 9; z <= 13; z += 2) {
            for (int x = 3; x <= 12; x++) {
                if (x == 7 || x == 8) {
                    continue;
                }
                b.chest(x, 1, z, Direction.NORTH, "city_shop", true);
                b.set(x, 2, z, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
            }
        }
        b.fill(3, 1, 7, 4, 1, 7, B.slab(Blocks.SMOOTH_STONE_SLAB, true));                                  // caisses
        b.fill(11, 1, 7, 12, 1, 7, B.slab(Blocks.SMOOTH_STONE_SLAB, true));
        cashRegister(b, 3, 2, 7, Direction.NORTH);
        cashRegister(b, 12, 2, 7, Direction.NORTH);
        shopkeeper(b, 3, 1, 8);
        b.set(10, 3, 5, B.facing(fr.modeminecraft.registry.ModBlocks.NEON_SIGN.get(), Direction.NORTH));
        b.set(13, 4, 13, B.facing(fr.modeminecraft.registry.ModBlocks.AMBIENT_SPEAKER.get(), Direction.NORTH));
        Deco.door(b, 7, 1, 14, Direction.NORTH, fr.modeminecraft.registry.ModBlocks.LOCKED_METAL_DOOR.get());   // réserve
        for (int x = 3; x <= 12; x += 3) {
            b.set(x, 4, 11, Blocks.SEA_LANTERN);
            b.fill(x, -1, 0, x, -1, 4, Blocks.WHITE_CONCRETE);                                           // places de parking
        }
        Deco.dumpster(b, 13, 0, 1);
        b.bot(5, 1, 8, Faction.CIVILIAN, BotRole.CIVILIAN, 6);
    }

    /** Station-service : auvent, pompes (sur réservoirs explosifs), boutique. */
    private static void gasStation(B b) {
        b.ground(0, 0, 15, 15, Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 9);
        for (int[] p : new int[][]{{2, 2}, {13, 2}, {2, 8}, {13, 8}}) {
            b.fill(p[0], 0, p[1], p[0], 4, p[1], Blocks.WHITE_CONCRETE);
        }
        b.fill(1, 5, 1, 14, 5, 9, Blocks.RED_CONCRETE);
        b.fill(2, 5, 2, 13, 5, 8, Blocks.WHITE_CONCRETE);
        for (int x : new int[]{5, 10}) {
            b.set(x, -2, 5, Blocks.TNT);                                                                 // cuve de carburant
            b.set(x, 0, 5, Blocks.IRON_BLOCK);
            b.set(x, 1, 5, Blocks.RED_CONCRETE);
            b.set(x, 2, 5, Blocks.LIGHT_GRAY_STAINED_GLASS);
            b.set(x, 4, 5, Blocks.SEA_LANTERN);
        }
        b.fill(3, 0, 11, 12, 0, 14, Blocks.WHITE_CONCRETE);
        b.walls(3, 0, 11, 12, 4, 14, Blocks.WHITE_CONCRETE.defaultBlockState());
        b.fill(4, 1, 11, 11, 2, 11, Blocks.GLASS_PANE.defaultBlockState());
        b.fill(7, 1, 11, 7, 2, 11, Blocks.AIR.defaultBlockState());
        b.fill(3, 4, 11, 12, 4, 14, Blocks.WHITE_CONCRETE.defaultBlockState());
        b.chest(4, 1, 13, Direction.SOUTH, "city_shop", true);
        b.chest(5, 1, 13, Direction.SOUTH, "city_shop", true);
        b.fill(8, 1, 12, 10, 1, 12, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));                             // comptoir
        cashRegister(b, 9, 2, 12, Direction.NORTH);
        shopkeeper(b, 9, 1, 13);
        Deco.vendingMachine(b, 11, 1, 13, Direction.NORTH);
        Deco.explosiveBarrel(b, 13, 0, 13);
    }

    /** Commerçant derrière son comptoir (vend des rations, des soins, des piles ; se fait braquer). */
    static void shopkeeper(B b, int x, int y, int z) {
        var k = b.bot(x, y, z, Faction.CIVILIAN, BotRole.CIVILIAN, 2);
        if (k != null) {
            k.shopkeeper = true;
        }
    }

    /** Caisse enregistreuse posée sur un comptoir, écran tourné vers les clients. */
    static void cashRegister(B b, int x, int y, int z, Direction facing) {
        b.set(x, y, z, B.facing(fr.modeminecraft.registry.ModBlocks.CASH_REGISTER.get(), facing));
    }

    // ------------------------------------------------------------------ Central Park
    private static void park(B b) {
        b.ground(0, 0, 31, 31, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 14);
        b.fill(14, -1, 0, 17, -1, 31, Blocks.GRAVEL);                                              // allées en croix
        b.fill(0, -1, 14, 31, -1, 17, Blocks.GRAVEL);
        // bassin et fontaine
        b.fill(11, -1, 11, 20, -1, 20, Blocks.STONE_BRICKS);
        b.fill(12, -1, 12, 19, -1, 19, Blocks.WATER);
        b.fill(12, -2, 12, 19, -2, 19, Blocks.STONE_BRICKS);
        b.fill(15, -1, 15, 16, 2, 16, Blocks.CHISELED_STONE_BRICKS);
        b.set(15, 3, 15, fr.modeminecraft.registry.ModBlocks.FOUNTAIN_SPOUT.get());                          // jet d'eau animé
        b.set(16, 3, 16, Blocks.WATER);
        // statue
        b.fill(4, 0, 4, 5, 0, 5, Blocks.POLISHED_ANDESITE);
        b.fill(4, 1, 4, 5, 3, 5, Blocks.ANDESITE_WALL);
        b.fill(4, 4, 4, 5, 5, 5, Blocks.POLISHED_ANDESITE);
        // arbres, bancs, lampadaires, fleurs
        for (int[] t : new int[][]{{4, 24}, {9, 7}, {24, 5}, {26, 22}, {8, 27}, {22, 27}, {27, 11}, {5, 12}}) {
            tree(b, t[0], t[1], 5 + (t[0] + t[1]) % 3);
        }
        for (int i = 3; i <= 28; i += 8) {
            Deco.bench(b, 13, 0, i, Direction.EAST);
            Deco.lampPost(b, 18, 0, i + 2, Direction.WEST);
        }
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                if ((x * 31 + z * 17) % 23 == 0 && b.get(x, 0, z).isAir() && b.get(x, -1, z).is(Blocks.GRASS_BLOCK)) {
                    b.set(x, 0, z, ((x + z) % 3 == 0 ? Blocks.POPPY : (x + z) % 3 == 1 ? Blocks.DANDELION : Blocks.CORNFLOWER));
                }
            }
        }
        b.bot(15, 0, 5, Faction.CIVILIAN, BotRole.CIVILIAN, 14);
        b.bot(25, 0, 15, Faction.CIVILIAN, BotRole.CIVILIAN, 14);
        b.pair(6, 0, 15, Faction.POLICE, 20);
    }

    static void tree(B b, int x, int z, int h) {
        b.fill(x, 0, z, x, h, z, Blocks.OAK_LOG);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = h - 2; dy <= h + 1; dy++) {
                    if (dx * dx + dz * dz + (dy - h + 0.5) * (dy - h + 0.5) <= 6 && !(dx == 0 && dz == 0 && dy <= h)) {
                        b.set(x + dx, dy, z + dz, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ commissariat central
    /**
     * Commissariat central (2 x 2 chunks) :
     * - rez-de-chaussée : accueil, poste de garde vitré (moniteurs, pupitre d'ouverture centralisée), couloir de
     *   surveillance et 3 cellules de détention de 5 x 5 x 4 (béton, porte coulissante à barreaux, lit, sanitaire inox,
     *   banc, fenêtre haute à barreaux, plafond lumineux) ;
     * - 1er étage : vestiaires, des dizaines de casiers verrouillés (le gros du matériel, à forcer au pied-de-biche) ;
     * - 2e étage : bureaux fermés à clé ; toit : héliport ; sous-sol : armurerie blindée (carte bleue).
     */
    private static void policeStation(B b) {
        b.ground(0, 0, 31, 31, Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 22);
        BlockState wall = Blocks.WHITE_CONCRETE.defaultBlockState(), trim = Blocks.BLUE_CONCRETE.defaultBlockState();
        for (int f = 0; f < 3; f++) {
            int y = f * 5;
            b.fill(3, y, 3, 28, y, 24, Blocks.POLISHED_ANDESITE);
            b.walls(3, y, 3, 28, y + 4, 24, wall);
            b.walls(3, y + 4, 3, 28, y + 4, 24, trim);
            for (int i = 5; i <= 26; i += 4) {
                b.fill(i, y + 1, 3, i + 1, y + 3, 3, Blocks.LIGHT_BLUE_STAINED_GLASS);
                if (f > 0) b.fill(i, y + 1, 24, i + 1, y + 3, 24, Blocks.LIGHT_BLUE_STAINED_GLASS);
            }
            b.fill(26, y + 1, 23, 26, y + 4, 23, wall);                                       // cage d'échelle
            b.fill(26, y, 22, 26, y + 4, 22, B.facing(Blocks.LADDER, Direction.NORTH));
        }
        b.fill(3, 15, 3, 28, 15, 24, trim);
        b.fill(26, 15, 22, 26, 15, 22, B.facing(Blocks.LADDER, Direction.NORTH));
        b.set(26, 16, 22, Blocks.AIR);
        // entrée : portes vitrées, auvent bleu, drapeaux
        b.fill(14, 1, 3, 17, 3, 3, Blocks.AIR.defaultBlockState());
        b.fill(12, 4, 1, 19, 4, 2, trim);
        b.set(12, 1, 2, Blocks.BLUE_BANNER);
        b.set(19, 1, 2, Blocks.BLUE_BANNER);
        // ---------------- rez-de-chaussée : accueil
        b.fill(10, 1, 7, 21, 1, 7, B.slab(Blocks.POLISHED_ANDESITE_SLAB, true));
        b.chest(20, 1, 6, Direction.NORTH, "police_office", false);
        b.set(12, 3, 7, B.facing(fr.modeminecraft.registry.ModBlocks.WALL_CLOCK.get(), Direction.NORTH));
        b.fill(4, 1, 8, 27, 4, 8, wall);                                                       // cloison accueil / poste
        Deco.door(b, 15, 1, 8, Direction.SOUTH, Blocks.SPRUCE_DOOR);
        Deco.door(b, 25, 1, 8, Direction.SOUTH, fr.modeminecraft.registry.ModBlocks.LOCKED_WOODEN_DOOR.get());
        // poste de garde vitré face aux cellules : bureaux, moniteurs de surveillance, pupitre d'ouverture centralisée
        b.fill(4, 1, 13, 25, 4, 13, Blocks.GLASS);                                              // vitre (blocs pleins)
        Deco.door(b, 26, 1, 13, Direction.SOUTH, Blocks.SPRUCE_DOOR);
        for (int x = 6; x <= 22; x += 4) {
            b.set(x, 1, 11, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
            b.set(x, 2, 11, B.facing(fr.modeminecraft.registry.ModBlocks.SURVEILLANCE_MONITOR.get(), Direction.SOUTH));
            b.set(x + 1, 1, 11, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
            b.set(x + 1, 2, 11, B.facing(fr.modeminecraft.registry.ModBlocks.SURVEILLANCE_MONITOR.get(), Direction.SOUTH));
            b.set(x, 1, 10, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
        }
        b.set(14, 1, 12, B.facing(fr.modeminecraft.registry.ModBlocks.CELL_CONTROL_PANEL.get(), Direction.SOUTH));
        b.chest(4, 1, 9, Direction.EAST, "police_office", false);
        Deco.camera(b, 4, 4, 17, Direction.EAST);
        Deco.camera(b, 22, 4, 14, Direction.WEST);
        // couloir de surveillance (z 14..17), éclairage
        for (int x = 6; x <= 24; x += 6) {
            b.set(x, 4, 15, Blocks.SEA_LANTERN);
        }
        // cellules de détention
        for (int x0 : fr.modeminecraft.world.police.Arrest.CELL_X) {
            cell(b, x0);
        }
        b.fill(4, 1, 18, 4, 4, 23, Blocks.LIGHT_GRAY_CONCRETE);                                  // murs extérieurs des cellules
        b.fill(22, 1, 18, 22, 4, 23, Blocks.LIGHT_GRAY_CONCRETE);
        // ---------------- 1er étage : vestiaires et casiers de police (verrouillés)
        int y1 = 5;
        for (int z : new int[]{5, 9, 13, 17}) {
            for (int x = 5; x <= 24; x++) {
                Direction face = z % 8 == 5 ? Direction.SOUTH : Direction.NORTH;
                BlockState locker = fr.modeminecraft.registry.ModBlocks.POLICE_LOCKER.get().defaultBlockState()
                        .setValue(fr.modeminecraft.block.police.PoliceLockerBlock.FACING, face);
                b.set(x, y1 + 1, z, locker);
                b.set(x, y1 + 2, z, locker);
                b.lootAt(x, y1 + 1, z, "police_locker");
                b.lootAt(x, y1 + 2, z, "police_locker");
            }
        }
        for (int x = 7; x <= 22; x += 5) {
            b.set(x, y1 + 1, 7, B.facing(fr.modeminecraft.registry.ModBlocks.PRISON_BENCH.get(), Direction.SOUTH));
            b.set(x, y1 + 1, 15, B.facing(fr.modeminecraft.registry.ModBlocks.PRISON_BENCH.get(), Direction.NORTH));
            b.set(x, y1 + 4, 11, Blocks.SEA_LANTERN);
        }
        // ---------------- 2e étage : bureaux fermés à clé
        int y2 = 10;
        b.fill(4, y2 + 1, 13, 27, y2 + 3, 13, wall);
        Deco.door(b, 10, y2 + 1, 13, Direction.NORTH, fr.modeminecraft.registry.ModBlocks.LOCKED_WOODEN_DOOR.get());
        Deco.door(b, 20, y2 + 1, 13, Direction.NORTH, fr.modeminecraft.registry.ModBlocks.LOCKED_WOODEN_DOOR.get());
        for (int x = 6; x <= 25; x += 4) {
            Deco.desk(b, x, y2 + 1, 8, Direction.SOUTH);
            Deco.desk(b, x, y2 + 1, 18, Direction.NORTH);
        }
        b.chest(5, y2 + 1, 21, Direction.EAST, "police_office", false);
        b.chest(24, y2 + 1, 5, Direction.WEST, "police_office", false);
        for (int x = 7; x <= 25; x += 6) {
            b.set(x, y2 + 4, 10, Blocks.SEA_LANTERN);
            b.set(x, y2 + 4, 18, Blocks.SEA_LANTERN);
        }
        // armurerie blindée au sous-sol (porte à carte bleue)
        b.fill(6, -6, 6, 25, -1, 13, Blocks.POLISHED_DEEPSLATE);
        b.fill(7, -5, 7, 24, -2, 12, Blocks.AIR.defaultBlockState());
        b.fill(25, -6, 9, 28, -1, 21, Blocks.POLISHED_DEEPSLATE);                             // couloir d'accès
        b.fill(26, -5, 10, 27, -2, 20, Blocks.AIR.defaultBlockState());
        b.fill(27, -5, 20, 27, -1, 20, B.facing(Blocks.LADDER, Direction.NORTH));
        b.set(27, 0, 20, Blocks.AIR);
        b.set(26, -2, 15, Blocks.SEA_LANTERN);
        vaultDoor(b, 25, -5, 10, Direction.EAST, 1);
        for (int x = 8; x <= 22; x += 2) {
            Deco.armoryRack(b, x, -4, 7, Direction.SOUTH);
            b.chest(x, -5, 12, Direction.NORTH, "city_armory", false);
        }
        b.set(15, -2, 9, Blocks.SEA_LANTERN);
        b.set(15, -5, 9, fr.modeminecraft.registry.ModBlocks.ARMAMENT_TABLE.get());
        // garage et voitures de police
        b.vehicle(8, 0, 28, fr.modeminecraft.entity.VehicleEntity.Type.POLICE_CAR, 90);
        b.vehicle(20, 0, 28, fr.modeminecraft.entity.VehicleEntity.Type.POLICE_CAR, 90);
        b.chest(29, 0, 26, Direction.WEST, "city_armory", false);
        // toit : héliport, antenne, projecteur
        b.fill(10, 15, 8, 21, 15, 19, Blocks.GRAY_CONCRETE);
        b.fill(13, 15, 11, 18, 15, 16, Blocks.YELLOW_CONCRETE);
        b.fill(14, 15, 12, 17, 15, 15, Blocks.GRAY_CONCRETE);
        Deco.antenna(b, 5, 16, 5, 8);
        Deco.searchlight(b, 26, 16, 5);
        // effectifs
        b.pair(14, 1, 5, Faction.POLICE, 16);
        b.bot(10, 1, 10, Faction.POLICE, BotRole.OFFICER, 6);                 // gardien du poste
        b.pair(18, 6, 11, Faction.POLICE, 12);
        b.bot(16, 1, 1, Faction.POLICE, BotRole.OFFICER, 10);
        b.bot(16, 1, 1, Faction.SWAT, BotRole.BOSS, 8);                       // Commissaire Divisionnaire SWAT
        BossVaults.build(b, 16, 1, 3, 15, false, fr.modeminecraft.item.BossCardItem.VaultType.SWAT_HQ);               // sous-sol haute sécurité
        b.bot(15, 1, 5, Faction.CIVILIAN, BotRole.CONTRACTOR, 2);
        b.bot(12, -5, 9, Faction.SWAT, BotRole.SOLDIER, 6);
        b.bot(20, -5, 9, Faction.SWAT, BotRole.SOLDIER, 6);
        b.bot(15, 16, 13, Faction.POLICE, BotRole.SNIPER, 4);
    }

    /** Une cellule de 5 x 5 x 4 (bord gauche x0) : murs en béton, barreaux et porte coulissante sur le couloir. */
    private static void cell(B b, int x0) {
        int z0 = fr.modeminecraft.world.police.Arrest.CELL_Z, dz = fr.modeminecraft.world.police.Arrest.DOOR_Z;
        b.fill(x0, 0, z0, x0 + 4, 0, z0 + 4, Blocks.GRAY_CONCRETE);                              // sol
        b.fill(x0 - 1, 1, z0, x0 - 1, 4, z0 + 4, Blocks.LIGHT_GRAY_CONCRETE);                    // murs latéraux
        b.fill(x0 + 5, 1, z0, x0 + 5, 4, z0 + 4, Blocks.LIGHT_GRAY_CONCRETE);
        b.fill(x0 - 1, 1, z0 + 5, x0 + 5, 4, z0 + 5, Blocks.LIGHT_GRAY_CONCRETE);                // mur du fond
        b.fill(x0, 1, z0, x0 + 4, 4, z0 + 4, Blocks.AIR.defaultBlockState());
        b.fill(x0, 5, z0, x0 + 4, 5, z0 + 4, Blocks.SMOOTH_STONE);                                // plafond
        b.set(x0 + 2, 5, z0 + 2, Blocks.SEA_LANTERN);                                            // barreau lumineux encastré
        b.fill(x0 - 1, 1, dz, x0 + 5, 4, dz, Blocks.IRON_BARS);                                  // façade à barreaux
        BlockState door = fr.modeminecraft.registry.ModBlocks.SLIDING_JAIL_DOOR.get().defaultBlockState()
                .setValue(fr.modeminecraft.block.police.SlidingJailDoorBlock.FACING, Direction.NORTH);
        b.set(x0 + 2, 1, dz, door);
        b.set(x0 + 2, 2, dz, door.setValue(fr.modeminecraft.block.police.SlidingJailDoorBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        b.set(x0 + 2, 3, z0 + 5, Blocks.IRON_BARS);                                              // fenêtre haute à barreaux
        b.set(x0 + 2, 3, z0 + 6, Blocks.AIR);
        b.set(x0, 1, z0 + 3, B.facing(fr.modeminecraft.registry.ModBlocks.PRISON_BUNK_BED.get(), Direction.SOUTH));
        b.set(x0 + 4, 1, z0 + 4, B.facing(fr.modeminecraft.registry.ModBlocks.PRISON_TOILET_SINK.get(), Direction.NORTH));
        b.set(x0 + 4, 1, z0 + 1, B.facing(fr.modeminecraft.registry.ModBlocks.PRISON_BENCH.get(), Direction.WEST));
    }

    /** Voiture de police (blocs) : carrosserie noire et blanche, gyrophare. */
    static void policeCar(B b, int x, int y, int z) {
        b.fill(x, y, z, x + 4, y, z + 1, Blocks.BLACK_CONCRETE);
        b.fill(x + 1, y + 1, z, x + 3, y + 1, z + 1, Blocks.WHITE_CONCRETE);
        b.fill(x + 1, y + 1, z, x + 1, y + 1, z + 1, Blocks.GRAY_STAINED_GLASS);
        b.set(x + 2, y + 2, z, Blocks.RED_STAINED_GLASS);
        b.set(x + 2, y + 2, z + 1, Blocks.BLUE_STAINED_GLASS);
        b.set(x, y, z - 1, Blocks.BLACK_WOOL);
        b.set(x + 4, y, z - 1, Blocks.BLACK_WOOL);
    }

    /** Porte blindée à carte d'accès (niveau 1 bleu, 2 rouge, 3 noir). */
    static void vaultDoor(B b, int x, int y, int z, Direction facing, int level) {
        Block door = level == 1 ? ModBlocks.VAULT_DOOR_BLUE.get() : level == 2 ? ModBlocks.VAULT_DOOR_RED.get() : ModBlocks.VAULT_DOOR_BLACK.get();
        BlockState s = door.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        b.set(x, y, z, s);
        b.set(x, y + 1, z, s.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
    }

    @SuppressWarnings("unused")
    private static void unused(MountedGunEntity.Kind k) {
    }
}

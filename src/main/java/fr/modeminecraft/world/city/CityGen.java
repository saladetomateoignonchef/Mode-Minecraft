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
        // décors des trottoirs
        if (ns != ew) {
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
        // égouts sous la chaussée
        sewer(b, ns || !ew, ew || !ns, ns && ew);
        // habitants et police
        if (ns && ew) {
            if (b.chance(0.4f)) {
                b.pair(3, 0, 3, Faction.POLICE, 40);
            }
            if (b.chance(0.25f)) {
                b.bot(7, -6, 7, Faction.TRADER, BotRole.TRADER, 2);                // marchand clandestin dans les égouts
            }
        } else if (b.chance(0.3f)) {
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
        // hall du rez-de-chaussée : portes vitrées, accueil, portique de sécurité, plantes
        b.fill(6, 1, 1, 9, 3, 1, Blocks.AIR.defaultBlockState());
        b.fill(6, 1, 14, 9, 3, 14, Blocks.AIR.defaultBlockState());
        b.fill(3, 1, 4, 5, 1, 4, B.slab(Blocks.POLISHED_DIORITE_SLAB, true));               // banque d'accueil
        b.set(4, 1, 5, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
        b.fill(6, 1, 3, 6, 2, 3, Blocks.IRON_BARS);                                          // portique détecteur de métaux
        b.fill(9, 1, 3, 9, 2, 3, Blocks.IRON_BARS);
        b.fill(6, 3, 3, 9, 3, 3, Blocks.POLISHED_ANDESITE_SLAB);
        b.set(2, 1, 2, Blocks.POTTED_FERN);
        b.set(13, 1, 2, Blocks.POTTED_FERN);
        Deco.camera(b, 2, 3, 13, Direction.SOUTH);
        Deco.extinguisher(b, 13, 1, 13);
        b.bot(4, 1, 6, Faction.CIVILIAN, BotRole.CIVILIAN, 6);
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
        if (h > 0.4) {
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
        for (int x = 3; x <= 12; x++) {
            b.chest(x, 1, 12, Direction.NORTH, "city_shop", true);
            b.set(x, 2, 12, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        }
        b.set(7, 4, 7, Blocks.GLOWSTONE);
        b.bot(7, 1, 8, Faction.CIVILIAN, BotRole.CIVILIAN, 4);
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
        b.fill(5, 1, 12, 12, 1, 12, B.slab(Blocks.SPRUCE_SLAB, true));
        b.set(6, 2, 12, Blocks.BREWING_STAND);                                                          // machine à café
        b.set(10, 3, 9, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        for (int x = 3; x <= 12; x += 4) {                                                             // terrasse
            b.set(x, 0, 2, Blocks.OAK_FENCE);
            b.set(x, 1, 2, Blocks.OAK_PRESSURE_PLATE);
            b.set(x - 1, 0, 2, B.stairs(Blocks.OAK_STAIRS, Direction.WEST));
            b.set(x + 1, 0, 2, B.stairs(Blocks.OAK_STAIRS, Direction.EAST));
        }
        b.bot(8, 1, 10, Faction.CIVILIAN, BotRole.CIVILIAN, 4);
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
        Deco.vendingMachine(b, 11, 1, 13, Direction.NORTH);
        Deco.explosiveBarrel(b, 13, 0, 13);
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
        b.set(15, 3, 15, Blocks.WATER);
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
    private static void policeStation(B b) {
        b.ground(0, 0, 31, 31, Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 22);
        BlockState wall = Blocks.WHITE_CONCRETE.defaultBlockState(), trim = Blocks.BLUE_CONCRETE.defaultBlockState();
        for (int f = 0; f < 3; f++) {
            int y = f * 5;
            b.fill(3, y, 3, 28, y, 24, Blocks.POLISHED_ANDESITE);
            b.walls(3, y, 3, 28, y + 4, 24, wall);
            b.walls(3, y + 4, 3, 28, y + 4, 24, trim);
            for (int i = 5; i <= 26; i += 4) {
                b.fill(i, y + 1, 3, i + 1, y + 3, 3, Blocks.GLASS_PANE.defaultBlockState());
                b.fill(i, y + 1, 24, i + 1, y + 3, 24, Blocks.GLASS_PANE.defaultBlockState());
            }
            for (int x = 7; x <= 25; x += 6) {
                b.set(x, y + 4, 13, Blocks.SEA_LANTERN);
            }
            b.fill(26, y, 22, 26, y + 4, 22, B.facing(Blocks.LADDER, Direction.NORTH));
        }
        b.fill(3, 15, 3, 28, 15, 24, trim);
        b.fill(26, 15, 22, 26, 15, 22, B.facing(Blocks.LADDER, Direction.NORTH));
        b.set(26, 16, 22, Blocks.AIR);
        // entrée : portes, auvent bleu, drapeaux
        b.fill(14, 1, 3, 17, 3, 3, Blocks.AIR.defaultBlockState());
        b.fill(12, 4, 1, 19, 4, 2, trim);
        b.set(12, 1, 2, Blocks.BLUE_BANNER);
        b.set(19, 1, 2, Blocks.BLUE_BANNER);
        // accueil, bureaux, cellules
        b.fill(10, 1, 7, 21, 1, 7, B.slab(Blocks.POLISHED_ANDESITE_SLAB, true));
        for (int x = 6; x <= 25; x += 4) {
            Deco.desk(b, x, 6, 8, Direction.SOUTH);
            Deco.desk(b, x, 6, 18, Direction.NORTH);
        }
        for (int c = 0; c < 4; c++) {
            int x0 = 5 + c * 5;
            b.walls(x0, 1, 17, x0 + 4, 3, 23, Blocks.IRON_BARS.defaultBlockState());
            b.fill(x0 + 1, 1, 18, x0 + 3, 3, 22, Blocks.AIR.defaultBlockState());
            Deco.bed(b, x0 + 1, 1, 21, Direction.EAST, Blocks.GRAY_BED);
        }
        Deco.camera(b, 4, 3, 16, Direction.EAST);
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
        // garage et voiture de police
        policeCar(b, 6, 0, 27);
        // toit : héliport, antenne, sirène
        b.fill(10, 15, 8, 21, 15, 19, Blocks.GRAY_CONCRETE);
        b.fill(13, 15, 11, 18, 15, 16, Blocks.YELLOW_CONCRETE);
        b.fill(14, 15, 12, 17, 15, 15, Blocks.GRAY_CONCRETE);
        Deco.antenna(b, 5, 16, 5, 8);
        Deco.searchlight(b, 26, 16, 5);
        // effectifs
        b.pair(14, 1, 10, Faction.POLICE, 16);
        b.pair(18, 6, 12, Faction.POLICE, 16);
        b.bot(16, 1, 1, Faction.POLICE, BotRole.OFFICER, 10);
        b.bot(12, -5, 9, Faction.SWAT, BotRole.SOLDIER, 6);
        b.bot(20, -5, 9, Faction.SWAT, BotRole.SOLDIER, 6);
        b.bot(15, 16, 13, Faction.POLICE, BotRole.SNIPER, 4);
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

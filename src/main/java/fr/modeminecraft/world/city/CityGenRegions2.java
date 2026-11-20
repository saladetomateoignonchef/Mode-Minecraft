package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.MountedGunEntity;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Neige (avant-poste minier), jungle (cartel / pilotis), badlands (Far West / raffinerie). */
final class CityGenRegions2 {
    static void build(B b, CityLayout layout, CityLayout.Plot plot, String variant, int dx, int dz, int rad) {
        double h = layout.hash(dx, dz, 13);
        switch (variant) {
            case "mine" -> mine(b, plot.type(), dx, dz, h);
            case "cartel" -> cartel(b, plot.type(), dx, dz, rad, h);
            case "stilts" -> stilts(b, layout, plot.type(), dx, dz, h);
            case "frontier" -> frontier(b, plot.type(), dx, dz, h);
            case "refinery" -> refinery(b, plot.type(), dx, dz, rad, h);
            default -> {
            }
        }
    }

    // =====================================================================================================
    // NEIGE C : avant-poste minier fortifié (galeries riches en titane)
    // =====================================================================================================
    private static void mine(B b, String type, int dx, int dz, double h) {
        int gy = -10;                                               // niveau des galeries
        switch (type) {
            case "mine_entrance" -> {
                b.ground(0, 0, 15, 15, Blocks.GRAVEL.defaultBlockState(), Blocks.STONE.defaultBlockState(), 22);
                b.walls(0, 0, 0, 15, 3, 15, Blocks.STONE_BRICKS.defaultBlockState());
                b.fill(6, 0, 15, 9, 2, 15, Blocks.AIR.defaultBlockState());
                // chevalement au-dessus du puits
                for (int[] c : new int[][]{{5, 5}, {10, 5}, {5, 10}, {10, 10}}) {
                    b.fill(c[0], 0, c[1], c[0], 14, c[1], Blocks.DARK_OAK_LOG);
                }
                b.fill(5, 15, 5, 10, 15, 10, Blocks.DARK_OAK_PLANKS);
                b.fill(7, 16, 6, 8, 18, 9, Blocks.IRON_BARS);
                b.set(7, 19, 7, Blocks.IRON_BLOCK);
                b.fill(7, gy, 7, 8, -1, 8, Blocks.AIR.defaultBlockState());
                b.fill(7, gy, 7, 7, -1, 7, B.facing(Blocks.LADDER, Direction.SOUTH));
                b.fill(7, gy, 6, 7, -1, 6, Blocks.STONE_BRICKS);
                b.fill(8, 0, 8, 8, 13, 8, Blocks.CHAIN);
                // bureau de la sécurité
                b.fill(1, 0, 1, 4, 3, 4, Blocks.SPRUCE_PLANKS);
                b.fill(2, 0, 2, 3, 2, 3, Blocks.AIR.defaultBlockState());
                b.set(2, 0, 4, Blocks.AIR);
                b.set(2, 1, 4, Blocks.AIR);
                b.chest(3, 0, 2, Direction.WEST, "mine_crate", false);
                Deco.lampPost(b, 12, 0, 12, Direction.WEST);
                Deco.searchlight(b, 14, 4, 1);
                galleries(b, gy, h);
                b.bot(8, 0, 13, Faction.MINE_SECURITY, BotRole.OFFICER, 10);
                b.pair(3, 0, 8, Faction.MINE_SECURITY, 16);
            }
            case "mine_gallery" -> {
                galleries(b, gy, h);
                if (h < 0.5) {
                    b.bot(7, gy + 1, 3, Faction.MINE_SECURITY, BotRole.SOLDIER, 12);
                }
            }
            default -> {   // carreau de la mine : terrils, convoyeur, wagonnets
                b.ground(0, 0, 15, 15, Blocks.GRAVEL.defaultBlockState(), Blocks.STONE.defaultBlockState(), 10);
                for (int i = 0; i < 4; i++) {
                    b.fill(2 + i, i, 2 + i, 7 - i, i, 7 - i, (i % 2 == 0 ? Blocks.COBBLESTONE : Blocks.GRAVEL).defaultBlockState());
                }
                b.fill(9, 0, 3, 9, 2, 12, Blocks.STONE_BRICKS);
                b.fill(9, 3, 3, 9, 3, 12, Blocks.RAIL);                                    // tapis roulant
                b.fill(11, 0, 3, 11, 0, 12, Blocks.RAIL);
                b.set(11, 0, 8, Blocks.CHEST.defaultBlockState());
                Deco.fence(b, 0, 15, 15, 15, 3);
                Deco.explosiveBarrel(b, 13, 0, 13);
            }
        }
    }

    /** Galeries : boisages, rails, minerai de titane apparent, lanternes. */
    private static void galleries(B b, int gy, double h) {
        b.fill(0, gy - 1, 6, 15, gy + 3, 9, Blocks.STONE);
        b.fill(6, gy - 1, 0, 9, gy + 3, 15, Blocks.STONE);
        b.fill(0, gy, 7, 15, gy + 2, 8, Blocks.AIR.defaultBlockState());
        b.fill(7, gy, 0, 8, gy + 2, 15, Blocks.AIR.defaultBlockState());
        b.fill(0, gy, 7, 15, gy, 7, Blocks.RAIL);
        for (int i = 1; i < 16; i += 4) {
            b.set(i, gy, 9, Blocks.OAK_FENCE);
            b.set(i, gy + 1, 9, Blocks.OAK_FENCE);
            b.set(i, gy, 6, Blocks.OAK_FENCE);
            b.set(i, gy + 1, 6, Blocks.OAK_FENCE);
            b.fill(i, gy + 2, 6, i, gy + 2, 9, Blocks.OAK_PLANKS);
            b.set(i, gy + 1, 8, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        }
        Block ti = ModBlocks.TITANIUM_ORE.get();
        for (int i = 0; i < 16; i++) {
            if ((i * 7 + (int) (h * 10)) % 5 == 0) {
                b.set(i, gy + 1, 6, ti);
                b.set(i, gy + 2, 9, ti);
            }
            if ((i * 3 + (int) (h * 10)) % 6 == 0) {
                b.set(6, gy + 1, i, ti);
                b.set(9, gy, i, ti);
            }
        }
        if (h > 0.5) {
            b.set(3, gy, 8, Blocks.CHEST);                                      // wagonnet de minerai
        }
    }

    // =====================================================================================================
    // JUNGLE A : villa / complexe de cartel
    // =====================================================================================================
    private static void cartel(B b, String type, int dx, int dz, int rad, double h) {
        boolean big = type.equals("hacienda");
        int n = big ? 31 : 15;
        int nz = type.equals("pool") ? 31 : n;
        b.ground(0, 0, n, nz, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 18);
        // mur d'enceinte surmonté de tessons de verre
        if (!type.equals("airstrip")) {
            if (dx == -rad) {
                wallShards(b, 0, 0, 0, 15);
            }
            if (dx == rad) {
                wallShards(b, 15, 0, 15, 15);
            }
            if (dz == -rad) {
                wallShards(b, 0, 0, 15, 0);
            }
        }
        switch (type) {
            case "hacienda" -> hacienda(b);
            case "pool" -> {
                b.fill(2, -1, 4, 13, -1, 27, Blocks.SMOOTH_QUARTZ);
                b.fill(4, -3, 7, 11, -1, 24, Blocks.LIGHT_BLUE_CONCRETE);
                b.fill(5, -2, 8, 10, -1, 23, Blocks.WATER);
                for (int z = 8; z <= 22; z += 4) {                                   // transats et parasols
                    b.set(2, 0, z, B.stairs(Blocks.BIRCH_STAIRS, Direction.EAST));
                    b.set(3, 0, z, B.slab(Blocks.BIRCH_SLAB, false));
                    b.fill(13, 0, z, 13, 2, z, Blocks.BIRCH_FENCE);
                    b.fill(12, 3, z - 1, 14, 3, z + 1, (z % 8 == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL).defaultBlockState());
                }
                b.fill(5, 0, 26, 10, 0, 26, B.slab(Blocks.DARK_OAK_SLAB, true));   // bar de la piscine
                b.set(7, 1, 26, Blocks.BREWING_STAND);
                b.bot(7, 0, 4, Faction.CARTEL, BotRole.SOLDIER, 10);
            }
            case "airstrip" -> {
                b.ground(0, 0, 15, 15, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 10);
                b.fill(0, -1, 3, 15, -1, 12, Blocks.BLACK_CONCRETE);
                for (int x = 0; x < 16; x += 4) {
                    b.fill(x, -1, 7, x + 1, -1, 8, Blocks.WHITE_CONCRETE);
                    b.set(x, -1, 2, Blocks.SEA_LANTERN);                         // piste balisée
                    b.set(x, -1, 13, Blocks.SEA_LANTERN);
                }
                if (dx == 0) {                                                    // petit avion de la contrebande
                    b.fill(3, 0, 7, 12, 1, 8, Blocks.WHITE_CONCRETE);
                    b.fill(6, 1, 2, 8, 1, 13, Blocks.WHITE_CONCRETE);
                    b.fill(12, 1, 7, 12, 3, 8, Blocks.RED_CONCRETE);
                    b.set(3, 1, 7, Blocks.LIGHT_BLUE_STAINED_GLASS);
                    b.set(2, 0, 8, Blocks.IRON_BARS);
                }
                if (dx == -2) {
                    b.fill(1, 0, 14, 1, 4, 14, Blocks.OAK_FENCE);                   // manche à air
                    b.set(2, 4, 14, Blocks.ORANGE_WOOL);
                }
            }
            case "hangar_small" -> {
                b.fill(1, 0, 2, 14, 0, 14, Blocks.GRAY_CONCRETE);
                b.walls(1, 0, 2, 14, 7, 14, Blocks.IRON_BLOCK.defaultBlockState());
                b.fill(1, 7, 2, 14, 7, 14, Blocks.IRON_BLOCK);
                b.fill(4, 1, 14, 11, 5, 14, Blocks.AIR.defaultBlockState());
                b.vehicle(6, 1, 6, fr.modeminecraft.entity.VehicleEntity.Type.JEEP, 0);   // 4x4 blindé
                for (int x = 3; x <= 12; x += 3) {
                    b.set(x, 1, 12, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));   // cuves chimiques
                    b.set(x, 2, 12, Blocks.LIME_STAINED_GLASS);
                }
                b.chest(12, 1, 4, Direction.WEST, "cartel_safe", false);
                b.set(7, 6, 8, Blocks.SEA_LANTERN);
                b.pair(8, 1, 8, Faction.CARTEL, 10);
            }
            default -> {   // jardins tropicaux
                for (int[] p : new int[][]{{4, 4}, {11, 9}}) {
                    b.fill(p[0], 0, p[1], p[0], 6, p[1], Blocks.JUNGLE_LOG);
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        b.fill(p[0] + d.getStepX(), 6, p[1] + d.getStepZ(), p[0] + d.getStepX() * 2, 6, p[1] + d.getStepZ() * 2,
                                Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
                        b.set(p[0] + d.getStepX() * 3, 5, p[1] + d.getStepZ() * 3, Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
                    }
                }
                b.fill(2, 0, 12, 13, 0, 12, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
                if (h < 0.35) {                                                    // fontaine
                    b.fill(7, 0, 5, 9, 0, 7, Blocks.QUARTZ_BLOCK);
                    b.set(8, 0, 6, Blocks.WATER);
                    b.fill(8, 1, 6, 8, 2, 6, Blocks.QUARTZ_PILLAR);
                }
                if (h > 0.5) {
                    b.pair(8, 0, 9, Faction.CARTEL, 18);
                }
            }
        }
    }

    private static void wallShards(B b, int x0, int z0, int x1, int z1) {
        b.fill(x0, 0, z0, x1, 3, z1, Blocks.WHITE_CONCRETE);
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                b.set(x, 4, z, ((x + z) % 2 == 0 ? Blocks.GLASS_PANE : Blocks.IRON_BARS).defaultBlockState());
            }
        }
    }

    /** Hacienda (2 x 2 chunks) : arcades, lustres en cristal, billard, coffre derrière un tableau, toit-terrasse armé. */
    private static void hacienda(B b) {
        int x0 = 4, z0 = 4, x1 = 27, z1 = 27;
        for (int f = 0; f < 2; f++) {
            int y = f * 5;
            b.fill(x0, y, z0, x1, y, z1, Blocks.POLISHED_DIORITE);
            b.walls(x0, y, z0, x1, y + 4, z1, Blocks.WHITE_CONCRETE.defaultBlockState());
            for (int i = x0 + 2; i < x1; i += 4) {
                b.fill(i, y + 1, z0, i + 1, y + 3, z0, Blocks.GLASS_PANE.defaultBlockState());
                b.fill(i, y + 1, z1, i + 1, y + 3, z1, Blocks.GLASS_PANE.defaultBlockState());
            }
            b.fill(x1 - 1, y, z0 + 1, x1 - 1, y + 4, z0 + 1, B.facing(Blocks.LADDER, Direction.WEST));
        }
        b.fill(x0, 10, z0, x1, 10, z1, Blocks.POLISHED_DIORITE);
        b.set(x1 - 1, 10, z0 + 1, Blocks.AIR);
        // toit de tuiles sur les côtés, terrasse au centre
        b.walls(x0 - 1, 10, z0 - 1, x1 + 1, 10, z1 + 1, B.stairs(Blocks.GRANITE_STAIRS, Direction.NORTH));
        b.walls(x0, 11, z0, x1, 11, z1, Blocks.WHITE_CONCRETE.defaultBlockState());
        // arcades à l'entrée
        for (int x = 10; x <= 21; x += 3) {
            b.fill(x, 0, z1 + 1, x, 3, z1 + 1, Blocks.QUARTZ_PILLAR);
        }
        b.fill(9, 4, z1 + 1, 22, 4, z1 + 2, B.slab(Blocks.GRANITE_SLAB, false));
        b.fill(14, 1, z1, 17, 3, z1, Blocks.AIR.defaultBlockState());
        // lustres en cristal
        for (int[] c : new int[][]{{10, 10}, {21, 10}, {15, 20}}) {
            b.set(c[0], 4, c[1], Blocks.CHAIN);
            b.set(c[0], 3, c[1], Blocks.END_ROD);
            b.set(c[0], 3, c[1] + 1, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
            b.set(c[0] + 1, 3, c[1], Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        }
        // salle de billard
        b.fill(7, 1, 7, 9, 1, 12, Blocks.GREEN_WOOL);
        b.walls(6, 1, 6, 10, 1, 13, B.slab(Blocks.DARK_OAK_SLAB, true));
        b.fill(7, 1, 7, 9, 1, 12, Blocks.GREEN_WOOL);
        // salon, bar, chambres
        b.fill(18, 1, 6, 25, 1, 6, B.slab(Blocks.DARK_OAK_SLAB, true));
        b.set(19, 2, 6, Blocks.BREWING_STAND);
        for (int x = 18; x <= 24; x += 3) {
            b.set(x, 1, 10, B.stairs(Blocks.CRIMSON_STAIRS, Direction.SOUTH));
        }
        Deco.bed(b, 6, 6, 20, Direction.SOUTH, Blocks.RED_BED);
        Deco.bed(b, 22, 6, 20, Direction.SOUTH, Blocks.WHITE_BED);
        // coffre derrière un tableau (dans le mur), salle forte rouge
        b.chest(x0, 6, 14, Direction.EAST, "cartel_safe", false);
        if (b.inside(x0 + 1, 7, 14)) {
            var level = b.level.getLevel();
            net.minecraft.world.entity.decoration.Painting.create(level, b.pos(x0 + 1, 7, 14), Direction.EAST)
                    .ifPresent(b.level::addFreshEntity);
        }
        b.walls(22, 5, 22, 26, 8, 26, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        CityGen.vaultDoor(b, 22, 6, 24, Direction.WEST, 2);
        b.chest(25, 6, 23, Direction.WEST, "cartel_safe", false);
        b.chest(25, 6, 25, Direction.WEST, "city_vault", false);
        // terrasse : mitrailleuse et gardes
        b.turret(8, 11, 8, MountedGunEntity.Kind.M2, 180, Faction.CARTEL);
        b.bot(15, 1, 15, Faction.CARTEL, BotRole.OFFICER, 10);
        b.bot(15, 1, 15, Faction.CARTEL, BotRole.BOSS, 8);                    // El Patrón
        b.bot(15, 1, 15, Faction.CARTEL, BotRole.DRONE_OPERATOR, 8);
        b.pair(12, 1, 20, Faction.CARTEL, 12);
        b.pair(16, 0, 30, Faction.CARTEL, 16);
        b.bot(24, 11, 24, Faction.CARTEL, BotRole.SNIPER, 3);
    }

    // =====================================================================================================
    // JUNGLE B : ville sur pilotis / bidonville aquatique
    // =====================================================================================================
    private static void stilts(B b, CityLayout layout, String type, int dx, int dz, double h) {
        // pontons sur l'axe du chunk, vers les voisins
        for (int i = 0; i < 16; i++) {
            b.set(i, -1, 7, Blocks.SPRUCE_PLANKS);
            b.set(i, -1, 8, Blocks.SPRUCE_PLANKS);
            b.set(7, -1, i, Blocks.SPRUCE_PLANKS);
            b.set(8, -1, i, Blocks.SPRUCE_PLANKS);
            if (i % 5 == 0) {
                b.fill(i, -6, 6, i, 0, 6, Blocks.OAK_LOG);
                b.set(i, 1, 6, Blocks.LANTERN);                                   // pontons illuminés
            }
        }
        if (type.equals("dock")) {
            b.fill(3, -1, 3, 12, -1, 12, Blocks.SPRUCE_PLANKS);
            for (int[] c : new int[][]{{3, 3}, {12, 3}, {3, 12}, {12, 12}}) {
                b.fill(c[0], -8, c[1], c[0], 3, c[1], Blocks.OAK_LOG);
            }
            b.fill(3, 4, 3, 12, 4, 12, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
            for (int x = 5; x <= 10; x += 5) {
                b.chest(x, 0, 4, Direction.SOUTH, "city_shop", true);
            }
            b.bot(7, 0, 10, Faction.TRADER, BotRole.TRADER, 3);
            b.bot(5, 0, 8, Faction.CIVILIAN, BotRole.CONTRACTOR, 2);
            b.bot(9, 0, 5, Faction.CIVILIAN, BotRole.CIVILIAN, 10);
            return;
        }
        int count = 2 + (int) (h * 3);
        int[][] spots = {{1, 1}, {10, 1}, {1, 10}, {10, 10}};
        for (int k = 0; k < count && k < 4; k++) {
            int x0 = spots[k][0], z0 = spots[k][1];
            // pilotis couverts d'algues, cabane en bois et toit de tôle
            for (int[] c : new int[][]{{0, 0}, {4, 0}, {0, 4}, {4, 4}}) {
                b.fill(x0 + c[0], -8, z0 + c[1], x0 + c[0], -1, z0 + c[1], Blocks.OAK_LOG);
                b.set(x0 + c[0], -2, z0 + c[1] + (c[1] == 0 ? -1 : 1), Blocks.VINE.defaultBlockState().setValue(BlockStateProperties.NORTH, c[1] != 0)
                        .setValue(BlockStateProperties.SOUTH, c[1] == 0));
            }
            b.fill(x0, -1, z0, x0 + 4, -1, z0 + 4, Blocks.SPRUCE_PLANKS);
            b.walls(x0, 0, z0, x0 + 4, 2, z0 + 4, ((k + (int) (h * 9)) % 2 == 0 ? Blocks.SPRUCE_PLANKS : Blocks.STRIPPED_OAK_LOG).defaultBlockState());
            b.fill(x0 - 1, 3, z0 - 1, x0 + 5, 3, z0 + 5, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM));
            int doorX = x0 + 2, doorZ = z0 < 7 ? z0 + 4 : z0;
            b.fill(doorX, 0, doorZ, doorX, 1, doorZ, Blocks.AIR.defaultBlockState());
            b.set(x0 + 1, 1, z0 + 1, Blocks.LANTERN);
            b.chest(x0 + 3, 0, z0 + 1, Direction.WEST, "city_house", true);
            b.fill(x0 + 1, 0, z0 + 3, x0 + 3, 0, z0 + 3, Blocks.WHITE_CARPET);   // hamac
        }
        // filets de pêche, lanternes en papier, pirogue amarrée, moteur hors-bord
        b.fill(13, 0, 13, 15, 2, 13, Blocks.COBWEB);
        b.set(12, 2, 7, Blocks.SHROOMLIGHT);
        b.set(3, 2, 8, Blocks.SHROOMLIGHT);
        b.set(9, -1, 14, Blocks.IRON_BLOCK);
        b.set(9, 0, 14, Blocks.LEVER);
        if (b.inside(5, -1, 13) && b.level.getBlockState(b.pos(5, -2, 13)).getFluidState().isSource()) {
            Boat boat = new Boat(b.level.getLevel(), b.pos(5, -1, 13).getX() + 0.5, b.pos(5, -1, 13).getY(), b.pos(5, -1, 13).getZ() + 0.5);
            boat.setVariant(Boat.Type.JUNGLE);
            b.level.addFreshEntity(boat);
        }
        if (h > 0.4) {
            b.pair(7, 0, 7, Faction.GANG, 20);
        } else {
            b.bot(8, 0, 8, Faction.CIVILIAN, BotRole.CIVILIAN, 14);
        }
    }

    // =====================================================================================================
    // BADLANDS A : ville frontière (Far West moderne)
    // =====================================================================================================
    private static void frontier(B b, String type, int dx, int dz, double h) {
        b.ground(0, 0, 15, 15, Blocks.RED_SAND.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), 20);
        switch (type) {
            case "main_street" -> {
                b.fill(0, -1, 3, 15, -1, 12, Blocks.COARSE_DIRT);
                for (int x = 0; x < 16; x++) {
                    if ((x * 5 + (int) (h * 7)) % 4 == 0) {
                        b.set(x, -1, 7, Blocks.DIRT_PATH);
                    }
                }
                b.fill(0, 0, 0, 15, 0, 2, B.slab(Blocks.SPRUCE_SLAB, false));     // trottoirs en planches
                b.fill(0, 0, 13, 15, 0, 15, B.slab(Blocks.SPRUCE_SLAB, false));
                for (int x = 2; x < 16; x += 6) {
                    b.fill(x, 1, 2, x, 2, 2, Blocks.SPRUCE_FENCE);                  // barres d'attache
                    b.set(x, 3, 2, Blocks.LANTERN);
                }
                b.set(5, 0, 12, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));   // abreuvoir
                b.set(9, 0, 9, Blocks.DEAD_BUSH);                                  // virevoltant
                if (h < 0.4) {
                    b.pair(7, 0, 7, Faction.SHERIFF, 28);
                } else if (h < 0.7) {
                    b.bot(10, 0, 5, Faction.CIVILIAN, BotRole.CIVILIAN, 20);
                }
            }
            case "saloon" -> westernBuilding(b, dz, h, "saloon");
            case "bank" -> westernBuilding(b, dz, h, "bank");
            case "sheriff" -> westernBuilding(b, dz, h, "sheriff");
            case "store", "hotel", "house_west" -> westernBuilding(b, dz, h, type);
            case "railway", "station_west" -> {
                b.fill(0, -1, 6, 15, -1, 9, Blocks.GRAVEL);
                b.fill(0, 0, 7, 15, 0, 7, Blocks.RAIL);
                if (type.equals("station_west")) {
                    b.fill(1, 0, 1, 14, 0, 5, Blocks.SPRUCE_PLANKS);
                    b.fill(3, 1, 1, 12, 4, 3, Blocks.SPRUCE_PLANKS);
                    b.fill(4, 1, 1, 11, 3, 2, Blocks.AIR.defaultBlockState());
                    b.fill(1, 5, 1, 14, 5, 5, B.slab(Blocks.SPRUCE_SLAB, false));
                    b.set(7, 4, 2, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
                    b.bot(7, 1, 4, Faction.CIVILIAN, BotRole.CIVILIAN, 8);
                } else if (h < 0.5) {
                    b.fill(2, 1, 7, 12, 3, 7, Blocks.STRIPPED_SPRUCE_LOG);           // wagon de marchandises
                    b.chest(7, 1, 8, Direction.NORTH, "city_shop", false);
                }
            }
            case "water_tower" -> {
                for (int[] c : new int[][]{{4, 4}, {11, 4}, {4, 11}, {11, 11}}) {
                    b.fill(c[0], 0, c[1], c[0], 10, c[1], Blocks.SPRUCE_LOG);
                }
                for (int y = 11; y <= 17; y++) {
                    for (int x = 3; x <= 12; x++) {
                        for (int z = 3; z <= 12; z++) {
                            double d = Math.hypot(x - 7.5, z - 7.5);
                            if (d <= 4.8) {
                                b.set(x, y, z, (d > 3.8 || y == 11 ? Blocks.SPRUCE_PLANKS : Blocks.WATER).defaultBlockState());
                            }
                        }
                    }
                }
                b.fill(4, 18, 4, 11, 18, 11, B.slab(Blocks.SPRUCE_SLAB, false));
                b.fill(4, 0, 3, 4, 17, 3, B.facing(Blocks.LADDER, Direction.NORTH));
            }
            default -> {   // ranch / désert : cactus, corral, bottes de foin
                if (h < 0.5) {
                    b.walls(3, 0, 3, 12, 0, 12, Blocks.SPRUCE_FENCE.defaultBlockState());
                    b.set(7, 0, 3, Blocks.SPRUCE_FENCE_GATE);
                    b.set(5, 0, 6, Blocks.HAY_BLOCK);
                    b.set(5, 1, 6, Blocks.HAY_BLOCK);
                    if (h < 0.25) {
                        b.vehicle(9, 0, 9, fr.modeminecraft.entity.VehicleEntity.Type.QUAD, 0);
                    }
                } else {
                    b.fill(4, 0, 9, 4, 2, 9, Blocks.CACTUS);
                    b.fill(11, 0, 4, 11, 1, 4, Blocks.CACTUS);
                    b.set(8, 0, 12, Blocks.DEAD_BUSH);
                }
            }
        }
    }

    /** Bâtiment western : fausse façade haute, porche, intérieur selon le type. */
    private static void westernBuilding(B b, int dz, double h, String kind) {
        boolean southFacing = dz < 0;               // la façade donne sur la rue principale (dz = 0)
        int front = southFacing ? 15 : 0, back = southFacing ? 2 : 13;
        int zf = southFacing ? 13 : 2;              // mur de façade
        Block wall = kind.equals("bank") ? Blocks.BRICKS : (h < 0.5 ? Blocks.SPRUCE_PLANKS : Blocks.STRIPPED_SPRUCE_LOG);
        int x0 = 2, x1 = 13, z0 = Math.min(zf, back), z1 = Math.max(zf, back);
        int floors = kind.equals("saloon") || kind.equals("hotel") ? 2 : 1;
        for (int f = 0; f < floors; f++) {
            int y = f * 5;
            b.fill(x0, y, z0, x1, y, z1, Blocks.SPRUCE_PLANKS);
            b.walls(x0, y, z0, x1, y + 4, z1, wall.defaultBlockState());
            b.set(7, y + 4, (z0 + z1) / 2, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        }
        int roof = floors * 5;
        b.fill(x0, roof, z0, x1, roof, z1, B.slab(Blocks.SPRUCE_SLAB, false));
        b.fill(x0, roof, zf, x1, roof + 3, zf, wall.defaultBlockState());                    // fausse façade
        b.fill(x0 + 1, roof + 1, zf, x1 - 1, roof + 2, zf, (kind.equals("saloon") ? Blocks.RED_TERRACOTTA : kind.equals("bank") ? Blocks.YELLOW_TERRACOTTA
                : kind.equals("sheriff") ? Blocks.BLACK_TERRACOTTA : Blocks.WHITE_TERRACOTTA).defaultBlockState());
        // porche
        int pz = southFacing ? 14 : 1;
        b.fill(x0, 0, pz, x1, 0, pz, B.slab(Blocks.SPRUCE_SLAB, true));
        b.fill(x0, 1, pz, x0, 3, pz, Blocks.SPRUCE_FENCE);
        b.fill(x1, 1, pz, x1, 3, pz, Blocks.SPRUCE_FENCE);
        b.fill(x0, 4, pz, x1, 4, pz, B.slab(Blocks.SPRUCE_SLAB, false));
        b.fill(4, 2, zf, 5, 3, zf, Blocks.GLASS_PANE.defaultBlockState());
        b.fill(10, 2, zf, 11, 3, zf, Blocks.GLASS_PANE.defaultBlockState());
        b.fill(7, 1, zf, 8, 2, zf, Blocks.AIR.defaultBlockState());
        int in = southFacing ? -1 : 1;                                                     // vers l'intérieur
        switch (kind) {
            case "saloon" -> {
                // portes battantes, bar, piano, tables de poker, marchand clandestin
                b.set(7, 1, zf, B.facing(Blocks.SPRUCE_TRAPDOOR, southFacing ? Direction.SOUTH : Direction.NORTH).setValue(BlockStateProperties.OPEN, true));
                b.set(8, 1, zf, B.facing(Blocks.SPRUCE_TRAPDOOR, southFacing ? Direction.SOUTH : Direction.NORTH).setValue(BlockStateProperties.OPEN, true));
                b.fill(3, 1, back + in * -1, 12, 1, back + in * -1, B.slab(Blocks.DARK_OAK_SLAB, true));
                b.set(3, 1, zf + in * 2, Blocks.NOTE_BLOCK);
                b.set(3, 2, zf + in * 2, Blocks.DARK_OAK_SLAB);
                b.set(4, 1, zf + in * 2, Blocks.NOTE_BLOCK);
                for (int x = 6; x <= 10; x += 4) {
                    b.set(x, 1, (z0 + z1) / 2, Blocks.SPRUCE_FENCE);
                    b.set(x, 2, (z0 + z1) / 2, Blocks.GREEN_CARPET);
                    b.set(x - 1, 1, (z0 + z1) / 2, B.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST));
                    b.set(x + 1, 1, (z0 + z1) / 2, B.stairs(Blocks.SPRUCE_STAIRS, Direction.WEST));
                }
                b.fill(12, 1, zf + in, 12, 5, zf + in, B.facing(Blocks.LADDER, Direction.WEST));
                Deco.bed(b, 4, 6, (z0 + z1) / 2, Direction.SOUTH, Blocks.RED_BED);
                b.bot(8, 1, back - in * -1 + in, Faction.TRADER, BotRole.TRADER, 3);
                b.bot(9, 1, (z0 + z1) / 2, Faction.CIVILIAN, BotRole.CIVILIAN, 6);
            }
            case "bank" -> {
                b.fill(3, 1, (z0 + z1) / 2, 12, 1, (z0 + z1) / 2, B.slab(Blocks.SMOOTH_STONE_SLAB, true));
                b.fill(3, 2, (z0 + z1) / 2, 12, 3, (z0 + z1) / 2, Blocks.IRON_BARS);
                b.fill(7, 1, (z0 + z1) / 2, 7, 3, (z0 + z1) / 2, Blocks.AIR.defaultBlockState());
                int vz = back + (southFacing ? 1 : -4);
                b.walls(3, 1, Math.min(vz, vz + 3), 7, 4, Math.max(vz, vz + 3), Blocks.IRON_BLOCK.defaultBlockState());
                CityGen.vaultDoor(b, 7, 1, vz + 1, Direction.EAST, 1);
                b.chest(4, 1, vz + 1, Direction.EAST, "bank_vault", false);
                b.chest(4, 1, vz + 2, Direction.EAST, "bank_vault", false);
                b.set(5, 1, vz + 1, Blocks.GOLD_BLOCK);
                b.bot(10, 1, back + in * -2, Faction.SHERIFF, BotRole.SOLDIER, 5);
            }
            case "sheriff" -> {
                Deco.desk(b, 9, 1, (z0 + z1) / 2, southFacing ? Direction.NORTH : Direction.SOUTH);
                b.walls(3, 1, back + (southFacing ? 1 : -4), 6, 3, back + (southFacing ? 4 : -1), Blocks.IRON_BARS.defaultBlockState());
                b.fill(4, 1, back + (southFacing ? 2 : -3), 5, 3, back + (southFacing ? 3 : -2), Blocks.AIR.defaultBlockState());
                Deco.armoryRack(b, 12, 2, (z0 + z1) / 2, Direction.WEST);
                b.set(2, 2, (z0 + z1) / 2 + 1, Blocks.WHITE_BANNER);                         // affiche WANTED
                b.chest(12, 1, back + in * -1, Direction.WEST, "city_armory", false);
                b.bot(9, 1, (z0 + z1) / 2 + in, Faction.SHERIFF, BotRole.OFFICER, 10);
                b.bot(5, 1, (z0 + z1) / 2, Faction.SHERIFF, BotRole.CONTRACTOR, 2);
            }
            case "store" -> {
                for (int x = 3; x <= 12; x++) {
                    b.chest(x, 1, back + in * -1, Direction.NORTH, "city_shop", true);
                }
                b.fill(4, 1, (z0 + z1) / 2, 11, 1, (z0 + z1) / 2, B.slab(Blocks.SPRUCE_SLAB, true));
                b.bot(7, 1, back + in * -2, Faction.CIVILIAN, BotRole.CIVILIAN, 4);
            }
            default -> {
                Deco.bed(b, 4, 1, back + in * -2, Direction.EAST, Blocks.BROWN_BED);
                b.chest(11, 1, back + in * -1, Direction.NORTH, "city_house", false);
                b.set(10, 1, (z0 + z1) / 2, B.lit(Blocks.FURNACE));
                if (kind.equals("hotel")) {
                    b.fill(12, 1, zf + in, 12, 5, zf + in, B.facing(Blocks.LADDER, Direction.WEST));
                    Deco.bed(b, 4, 6, (z0 + z1) / 2, Direction.EAST, Blocks.WHITE_BED);
                    Deco.bed(b, 9, 6, (z0 + z1) / 2, Direction.EAST, Blocks.WHITE_BED);
                }
            }
        }
        if (front == 15 || front == 0) {
            b.set(x0 - 1, 0, pz, Blocks.SPRUCE_FENCE);
        }
    }

    // =====================================================================================================
    // BADLANDS B : raffinerie pétrolière fortifiée
    // =====================================================================================================
    private static void refinery(B b, String type, int dx, int dz, int rad, double h) {
        b.ground(0, 0, 15, 15, Blocks.TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), 34);
        if (Math.abs(dx) == rad || Math.abs(dz) == rad) {
            if (dx == -rad) {
                Deco.fence(b, 0, 0, 0, 15, 4);
            }
            if (dx == rad) {
                Deco.fence(b, 15, 0, 15, 15, 4);
            }
            if (dz == -rad) {
                Deco.fence(b, 0, 0, 15, 0, 4);
            }
            if (dz == rad && dx != 0) {
                Deco.fence(b, 0, 15, 15, 15, 4);
            }
        }
        switch (type) {
            case "guard_tower" -> {
                Deco.watchtower(b, 6, 6, 10, Blocks.IRON_BARS, Blocks.IRON_BLOCK);
                b.turret(8, 11, 8, MountedGunEntity.Kind.MK19, dz < 0 ? 180 : 0, Faction.REFINERY);
                Deco.searchlight(b, 6, 11, 9);
            }
            case "oil_tank" -> {
                for (int y = 0; y < 14; y++) {
                    for (int x = 1; x <= 14; x++) {
                        for (int z = 1; z <= 14; z++) {
                            double d = Math.hypot(x - 7.5, z - 7.5);
                            if (d <= 6.4 && (d > 5.4 || y == 0 || y == 13)) {
                                b.set(x, y, z, (y % 4 == 3 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.WHITE_CONCRETE).defaultBlockState());
                            }
                        }
                    }
                }
                b.fill(1, 0, 7, 1, 13, 7, B.facing(Blocks.LADDER, Direction.WEST));
                b.walls(2, 14, 2, 13, 14, 13, Blocks.IRON_BARS.defaultBlockState());
                if (h > 0.5) {
                    b.pair(0, 0, 14, Faction.REFINERY, 20);
                }
            }
            case "pumpjack" -> {
                // chevalet de pompage
                b.fill(6, 0, 7, 6, 4, 7, Blocks.IRON_BARS);
                b.fill(6, 0, 9, 6, 4, 9, Blocks.IRON_BARS);
                b.fill(2, 5, 8, 11, 5, 8, Blocks.BLACK_CONCRETE);
                b.fill(1, 3, 8, 2, 5, 8, Blocks.BLACK_CONCRETE);                                   // tête de cheval
                b.fill(1, 0, 8, 1, 2, 8, Blocks.CHAIN);
                b.fill(10, 0, 7, 12, 2, 9, Blocks.YELLOW_CONCRETE);
                // flaque de pétrole inflammable
                b.fill(3, -1, 11, 7, -1, 14, Blocks.NETHERRACK);
                b.fill(3, 0, 11, 7, 0, 14, Blocks.BLACK_CARPET);
                Deco.explosiveBarrel(b, 13, 0, 13);
            }
            case "pipes" -> {
                boolean alongX = dz == 0;
                for (int i = 0; i < 16; i++) {
                    int x = alongX ? i : 6, z = alongX ? 6 : i;
                    for (int k = 0; k <= 2; k += 2) {
                        b.set(alongX ? i : 6 + k, 2, alongX ? 6 + k : i, Blocks.WAXED_CUT_COPPER);
                    }
                    if (i % 5 == 0) {
                        b.fill(x, 0, z, x, 1, z, Blocks.IRON_BARS);
                        b.fill(alongX ? i : 8, 0, alongX ? 8 : i, alongX ? i : 8, 1, alongX ? 8 : i, Blocks.IRON_BARS);
                        b.set(alongX ? i : 7, 3, alongX ? 7 : i, B.facing(Blocks.IRON_TRAPDOOR, Direction.NORTH));   // volant de vanne
                    }
                }
                if (h < 0.4) {                                                                        // camion-citerne
                    b.fill(2, 0, 11, 9, 2, 13, Blocks.LIGHT_GRAY_CONCRETE);
                    b.fill(10, 0, 11, 12, 2, 13, Blocks.RED_CONCRETE);
                    b.set(12, 1, 12, Blocks.GRAY_STAINED_GLASS);
                    b.fill(2, 0, 10, 12, 0, 10, Blocks.BLACK_WOOL);
                }
            }
            case "flare" -> {
                b.fill(6, 0, 6, 9, 0, 9, Blocks.IRON_BLOCK);
                b.fill(7, 1, 7, 8, 30, 8, Blocks.IRON_BARS);
                b.fill(7, 31, 7, 8, 31, 8, Blocks.NETHERRACK);
                b.fill(7, 32, 7, 8, 32, 8, Blocks.FIRE);                                              // torchère enflammée
                b.set(6, 30, 6, B.lit(Blocks.CAMPFIRE));
            }
            case "refinery_office" -> {
                b.fill(2, 0, 2, 13, 0, 13, Blocks.GRAY_CONCRETE);
                b.walls(2, 0, 2, 13, 4, 13, Blocks.GRAY_CONCRETE.defaultBlockState());
                b.fill(2, 4, 2, 13, 4, 13, Blocks.GRAY_CONCRETE);
                b.fill(7, 1, 13, 8, 2, 13, Blocks.AIR.defaultBlockState());
                for (int x = 4; x <= 11; x += 3) {
                    b.fill(x, 2, 2, x + 1, 3, 2, Blocks.GLASS_PANE.defaultBlockState());
                }
                Deco.desk(b, 5, 1, 5, Direction.SOUTH);
                b.chest(11, 1, 4, Direction.WEST, "military_crate", false);
                b.set(7, 3, 7, Blocks.GLOWSTONE);
                b.bot(7, 1, 8, Faction.REFINERY, BotRole.OFFICER, 10);
                b.bot(7, 1, 8, Faction.REFINERY, BotRole.DRONE_OPERATOR, 8);
                b.vehicle(7, 0, 15, fr.modeminecraft.entity.VehicleEntity.Type.JEEP, 90);
            }
            default -> {
                if (h > 0.6) {
                    b.pair(7, 0, 7, Faction.REFINERY, 24);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private static Block unused() {
        return Blocks.AIR;
    }
}

package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Quartiers variés de la cité du désert (en plus du riad classique) : maison-tour, oasis publique, marché couvert,
 * chantier et place à fontaine. Une parcelle n'a jamais le même type que sa voisine (voir {@link CityLayout}).
 */
final class DesertDistricts {
    private DesertDistricts() {}

    private static BlockState leaves() {
        return Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
    }

    private static void sandGround(B b) {
        b.ground(0, 0, 15, 15, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), 20);
    }

    /** Maison-tour de 3 étages : façade étroite, moucharabiehs, toit-terrasse avec réservoir. */
    static void riadTall(B b, double h) {
        Block wall = h < 0.33 ? Blocks.CUT_SANDSTONE : h < 0.66 ? Blocks.ORANGE_TERRACOTTA : Blocks.WHITE_TERRACOTTA;
        sandGround(b);
        int x0 = 3, z0 = 3, x1 = 12, z1 = 12;
        for (int f = 0; f < 3; f++) {
            int y = f * 4;
            b.fill(x0, y, z0, x1, y, z1, Blocks.SMOOTH_SANDSTONE);
            b.walls(x0, y, z0, x1, y + 3, z1, wall.defaultBlockState());
            b.fill(x0 + 1, y + 1, z0 + 1, x1 - 1, y + 3, z1 - 1, Blocks.AIR.defaultBlockState());
            for (int i = 5; i <= 10; i += 5) {
                b.set(i, y + 2, z0, B.facing(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH).setValue(BlockStateProperties.OPEN, true));
                b.set(i, y + 2, z1, B.facing(Blocks.SPRUCE_TRAPDOOR, Direction.SOUTH).setValue(BlockStateProperties.OPEN, true));
                b.set(x0, y + 2, i, B.facing(Blocks.SPRUCE_TRAPDOOR, Direction.WEST).setValue(BlockStateProperties.OPEN, true));
                b.set(x1, y + 2, i, B.facing(Blocks.SPRUCE_TRAPDOOR, Direction.EAST).setValue(BlockStateProperties.OPEN, true));
            }
            // escalier intérieur (échelle) et lanterne
            if (f < 2) {
                b.fill(x1 - 1, y + 1, z1 - 1, x1 - 1, y + 4, z1 - 1, B.facing(Blocks.LADDER, Direction.WEST));
                b.set(x1 - 1, y + 4, z1 - 1, Blocks.AIR);
            }
            b.set(7, y + 3, 7, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        }
        b.fill(x1 - 1, 9, z1 - 1, x1 - 1, 9, z1 - 1, Blocks.AIR.defaultBlockState());
        b.fill(7, 1, z0, 8, 2, z0, Blocks.AIR.defaultBlockState());                      // porte sur la ruelle
        b.fill(x0, 12, z0, x1, 12, z1, Blocks.SMOOTH_SANDSTONE);
        b.fill(x1 - 1, 12, z1 - 1, x1 - 1, 12, z1 - 1, Blocks.AIR.defaultBlockState());
        b.walls(x0, 13, z0, x1, 13, z1, Blocks.SANDSTONE_WALL.defaultBlockState());
        b.set(5, 13, 5, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        b.set(10, 13, 6, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
        b.chest(5, 5, 5, Direction.EAST, "city_house", false);
        if (h > 0.5) {
            b.bot(7, 1, 7, Faction.CIVILIAN, BotRole.CIVILIAN, 4);
        } else if (h < 0.15) {
            b.bot(8, 13, 8, Faction.MILITIA, BotRole.SNIPER, 2);
        }
    }

    /** Oasis publique : bassin, palmiers, herbe, bancs. */
    static void oasis(B b, double h) {
        sandGround(b);
        b.fill(1, -1, 1, 14, -1, 14, Blocks.GRASS_BLOCK);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if ((x * 7 + z * 3) % 11 == 0) {
                    b.set(x, -1, z, Blocks.COARSE_DIRT);
                }
            }
        }
        // bassin irrégulier
        b.fill(5, -1, 5, 10, -1, 10, Blocks.WATER);
        b.fill(6, -2, 6, 9, -2, 9, Blocks.SANDSTONE);
        b.set(5, -1, 5, Blocks.GRASS_BLOCK);
        b.set(10, -1, 10, Blocks.GRASS_BLOCK);
        b.set(7, 0, 7, Blocks.LILY_PAD);
        // palmiers
        int[][] palms = h < 0.5 ? new int[][]{{3, 3}, {12, 4}, {4, 12}, {12, 12}} : new int[][]{{2, 8}, {13, 7}, {8, 2}, {8, 13}};
        for (int[] p : palms) {
            int height = 5 + b.nextInt(3);
            b.fill(p[0], 0, p[1], p[0], height, p[1], Blocks.JUNGLE_LOG);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= 3 && (dx != 0 || dz != 0)) {
                        b.set(p[0] + dx, height, p[1] + dz, leaves());
                        if (Math.abs(dx) + Math.abs(dz) <= 1) {
                            b.set(p[0] + dx, height + 1, p[1] + dz, leaves());
                        }
                    }
                }
            }
        }
        Deco.bench(b, 4, 0, 7, Direction.EAST);
        Deco.bench(b, 11, 0, 8, Direction.WEST);
        Deco.lampPost(b, 1, 0, 1, Direction.EAST);
        Deco.lampPost(b, 14, 0, 14, Direction.WEST);
        if (b.chance(0.5f)) {
            b.bot(7, 0, 3, Faction.CIVILIAN, BotRole.CIVILIAN, 10);
        }
    }

    /** Marché couvert : piliers, toit de terre cuite, étals alignés, lanternes. */
    static void marketHall(B b, double h) {
        sandGround(b);
        b.fill(1, -1, 1, 14, -1, 14, Blocks.CUT_SANDSTONE);
        for (int x : new int[]{1, 7, 8, 14}) {
            for (int z : new int[]{1, 7, 8, 14}) {
                b.fill(x, 0, z, x, 5, z, Blocks.SPRUCE_LOG);
            }
        }
        b.fill(0, 6, 0, 15, 6, 15, Blocks.TERRACOTTA);
        b.fill(1, 7, 1, 14, 7, 14, B.slab(Blocks.ORANGE_TERRACOTTA, false));
        Block[] cloth = {Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL, Blocks.BLUE_WOOL};
        for (int x = 2; x <= 13; x++) {
            for (int z = 2; z <= 13; z += 5) {
                b.set(x, 5, z, cloth[(x / 3 + z) % cloth.length]);
            }
        }
        // étals
        Block[] goods = {Blocks.ORANGE_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER};
        for (int sx = 3; sx <= 10; sx += 7) {
            for (int sz = 3; sz <= 11; sz += 4) {
                b.fill(sx, 0, sz, sx + 2, 0, sz, B.slab(Blocks.SPRUCE_SLAB, true));
                for (int i = 0; i < 3; i++) {
                    b.set(sx + i, 1, sz, goods[(sx + sz + i) % goods.length].defaultBlockState());
                }
                b.set(sx + 1, 0, sz + 1, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
            }
        }
        b.chest(13, 0, 7, Direction.WEST, "city_shop", true);
        b.set(7, 5, 7, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        b.bot(7, 0, 5, Faction.TRADER, BotRole.TRADER, 4);
        if (h > 0.4) {
            b.bot(5, 0, 9, Faction.CIVILIAN, BotRole.CIVILIAN, 8);
        }
    }

    /** Chantier : échafaudages, murs inachevés, tas de sable, caisses et grue. */
    static void worksite(B b, double h) {
        b.ground(0, 0, 15, 15, Blocks.SAND.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), 20);
        b.fill(0, -1, 0, 15, -1, 15, Blocks.SMOOTH_SANDSTONE);
        // fondations et murs à moitié montés
        b.walls(3, 0, 3, 12, 0, 12, Blocks.SANDSTONE.defaultBlockState());
        for (int x = 3; x <= 12; x++) {
            int up = 1 + (x * 5 + (int) (h * 10)) % 4;
            b.fill(x, 1, 3, x, up, 3, Blocks.CUT_SANDSTONE);
            b.fill(x, 1, 12, x, Math.max(1, up - 1), 12, Blocks.CUT_SANDSTONE);
        }
        for (int z = 4; z <= 11; z += 3) {
            b.fill(3, 1, z, 3, 3, z, Blocks.CUT_SANDSTONE);
            b.fill(12, 1, z, 12, 2, z, Blocks.CUT_SANDSTONE);
        }
        Deco.scaffolding(b, 2, 2, 6, 6, 6);
        // tas de matériaux
        b.fill(8, 0, 6, 10, 1, 8, Blocks.SAND);
        b.fill(9, 2, 7, 9, 2, 7, Blocks.SAND);
        b.set(5, 0, 9, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        b.set(6, 0, 9, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        b.set(6, 1, 9, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        // grue : mât, flèche, câble
        b.fill(13, 0, 13, 13, 11, 13, Blocks.IRON_BARS);
        b.fill(8, 11, 13, 13, 11, 13, Blocks.SPRUCE_FENCE);
        b.fill(9, 5, 13, 9, 10, 13, Blocks.CHAIN);
        b.set(9, 4, 13, Blocks.SPRUCE_PLANKS);
        b.set(7, 12, 13, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false));
        if (b.chance(0.6f)) {
            b.bot(9, 0, 10, Faction.CIVILIAN, BotRole.CONTRACTOR, 6);
        }
        b.chest(4, 0, 10, Direction.EAST, "city_house", true);
    }

    /** Place publique : dallage en damier, fontaine, obélisque, bancs et lampadaires. */
    static void square(B b, double h) {
        sandGround(b);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                b.set(x, -1, z, (x / 2 + z / 2) % 2 == 0 ? Blocks.SMOOTH_SANDSTONE : Blocks.CUT_SANDSTONE);
            }
        }
        if (h < 0.5) {
            // fontaine à bassin
            b.fill(5, -1, 5, 10, -1, 10, Blocks.SANDSTONE);
            b.fill(6, 0, 6, 9, 0, 9, Blocks.WATER);
            b.walls(5, 0, 5, 10, 0, 10, Blocks.SANDSTONE_WALL.defaultBlockState());
            b.fill(7, 0, 7, 8, 2, 8, Blocks.CHISELED_SANDSTONE);
            b.set(7, 3, 7, Blocks.WATER);
        } else {
            // obélisque
            b.fill(6, -1, 6, 9, -1, 9, Blocks.POLISHED_GRANITE);
            b.fill(7, 0, 7, 8, 1, 8, Blocks.CUT_SANDSTONE);
            b.fill(7, 2, 7, 8, 5, 8, Blocks.SMOOTH_SANDSTONE);
            b.set(7, 6, 7, Blocks.SANDSTONE_SLAB);
            b.set(8, 6, 8, Blocks.SANDSTONE_SLAB);
        }
        Deco.bench(b, 3, 0, 8, Direction.EAST);
        Deco.bench(b, 12, 0, 8, Direction.WEST);
        Deco.lampPost(b, 1, 0, 1, Direction.EAST);
        Deco.lampPost(b, 14, 0, 1, Direction.WEST);
        Deco.lampPost(b, 1, 0, 14, Direction.EAST);
        Deco.lampPost(b, 14, 0, 14, Direction.WEST);
        b.bot(4, 0, 4, Faction.CIVILIAN, BotRole.CIVILIAN, 12);
        if (b.chance(0.5f)) {
            b.pair(12, 0, 12, Faction.MILITIA, 12);
        }
    }
}

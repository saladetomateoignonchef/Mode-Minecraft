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

/** Désert (dictature), forêt (rebelles / base secrète), neige (cité soviétique / station polaire). */
final class CityGenRegions {
    static void build(B b, CityLayout layout, CityLayout.Plot plot, String variant, int dx, int dz, int rad) {
        double h = layout.hash(dx, dz, 11);
        switch (variant) {
            case "desert" -> {
                switch (plot.type()) {
                    case "avenue" -> desertAvenue(b, dx, dz);
                    case "riad" -> riad(b, layout, dx, dz, h);
                    case "palace" -> palace(b);
                    case "souk" -> souk(b);
                    default -> {
                    }
                }
            }
            case "rebels" -> {
                switch (plot.type()) {
                    case "treehouse", "hq_tree" -> treehouse(b, layout, dx, dz, plot.type().equals("hq_tree"));
                    default -> rebelCamp(b, h);
                }
            }
            case "base" -> militaryBase(b, plot.type(), dx, dz, rad, h);
            case "soviet" -> soviet(b, layout, plot.type(), dx, dz, rad, h);
            case "polar" -> polar(b, layout, plot.type(), dx, dz, h);
            default -> CityGenRegions2.build(b, layout, plot, variant, dx, dz, rad);
        }
    }

    // =====================================================================================================
    // DÉSERT : cité basse sous dictature
    // =====================================================================================================
    private static void desertAvenue(B b, int dx, int dz) {
        b.ground(0, 0, 15, 15, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), 10);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if ((x + z) % 5 == 0) {
                    b.set(x, -1, z, Blocks.CUT_SANDSTONE);
                }
            }
        }
        // toiles d'ombrage multicolores tendues au-dessus de l'avenue
        Block[] cloth = {Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL, Blocks.BLUE_WOOL, Blocks.WHITE_WOOL};
        for (int i = 2; i < 16; i += 7) {
            b.set(0, 0, i, Blocks.SPRUCE_FENCE);
            b.fill(0, 0, i, 0, 4, i, Blocks.SPRUCE_FENCE);
            b.fill(15, 0, i, 15, 4, i, Blocks.SPRUCE_FENCE);
            for (int x = 0; x < 16; x++) {
                b.set(x, 5, i, cloth[(x / 3 + i) % cloth.length]);
                b.set(x, 5, i + 1, cloth[(x / 3 + i + 1) % cloth.length]);
            }
        }
        Deco.lampPost(b, 1, 0, 7, Direction.EAST);
        // haut-parleur de propagande
        b.fill(14, 0, 9, 14, 5, 9, Blocks.SPRUCE_FENCE);
        b.set(14, 6, 9, Blocks.BELL);
        // points de contrôle : sacs de sable, hérissons, fûts
        int k = b.nextInt(4);
        if (k == 0) {
            Deco.sandbags(b, 4, 0, 4, 9, 4);
            Deco.czechHedgehog(b, 12, 0, 11);
        } else if (k == 1) {
            Deco.explosiveBarrel(b, 3, 0, 12);
            Deco.explosiveBarrel(b, 4, 0, 12);
            Deco.camoNet(b, 9, 10, 13, 13, 3, false);
        } else if (k == 2) {
            b.set(6, 0, 6, Blocks.DECORATED_POT);                          // amphores
            b.set(7, 0, 6, Blocks.DECORATED_POT);
            Deco.searchlight(b, 12, 0, 3);
        }
        if (b.chance(0.35f)) {
            b.pair(7, 0, 8, Faction.MILITIA, 32);
        }
        if (b.chance(0.2f)) {
            b.bot(10, 0, 3, Faction.CIVILIAN, BotRole.CIVILIAN, 16);
        }
    }

    /** Riad : maison à patio, toit-terrasse (passerelles vers les voisins pour le parkour). */
    private static void riad(B b, CityLayout layout, int dx, int dz, double h) {
        Block wall = h < 0.33 ? Blocks.SMOOTH_SANDSTONE : h < 0.66 ? Blocks.WHITE_TERRACOTTA : Blocks.ORANGE_TERRACOTTA;
        b.ground(0, 0, 15, 15, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), 16);
        int x0 = 2, z0 = 2, x1 = 13, z1 = 13;
        for (int f = 0; f < 2; f++) {
            int y = f * 4;
            b.fill(x0, y, z0, x1, y, z1, Blocks.SMOOTH_SANDSTONE);
            b.walls(x0, y, z0, x1, y + 3, z1, wall.defaultBlockState());
            // moucharabiehs (fenêtres ajourées)
            for (int i = 4; i <= 11; i += 4) {
                b.set(i, y + 2, z0, B.facing(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH).setValue(BlockStateProperties.OPEN, true));
                b.set(i, y + 2, z1, B.facing(Blocks.SPRUCE_TRAPDOOR, Direction.SOUTH).setValue(BlockStateProperties.OPEN, true));
                b.set(x0, y + 2, i, B.facing(Blocks.SPRUCE_TRAPDOOR, Direction.WEST).setValue(BlockStateProperties.OPEN, true));
                b.set(x1, y + 2, i, B.facing(Blocks.SPRUCE_TRAPDOOR, Direction.EAST).setValue(BlockStateProperties.OPEN, true));
            }
            b.set(7, y + 3, 4, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        }
        // patio central à ciel ouvert, fontaine ou citronnier
        b.fill(6, 0, 6, 9, 8, 9, Blocks.AIR.defaultBlockState());
        b.fill(6, -1, 6, 9, -1, 9, Blocks.CUT_SANDSTONE);
        if (h < 0.5) {
            b.set(7, 0, 7, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));   // puits
            b.set(7, 1, 7, Blocks.SPRUCE_FENCE);
            b.set(7, 2, 7, Blocks.CHAIN);
            b.set(7, 3, 7, Blocks.SPRUCE_SLAB);
        } else {
            b.fill(8, 0, 8, 8, 3, 8, Blocks.JUNGLE_LOG);
            b.fill(7, 4, 7, 9, 4, 9, Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
        }
        // toit-terrasse : parapet, linge, réservoir, accès
        b.fill(x0, 8, z0, x1, 8, z1, Blocks.SMOOTH_SANDSTONE);
        b.fill(6, 8, 6, 9, 8, 9, Blocks.AIR.defaultBlockState());
        b.walls(x0, 9, z0, x1, 9, z1, Blocks.SANDSTONE_WALL.defaultBlockState());
        b.fill(12, 1, 12, 12, 8, 12, B.facing(Blocks.LADDER, Direction.WEST));
        b.set(12, 9, 12, Blocks.AIR);
        b.set(4, 9, 4, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        b.fill(4, 10, 11, 4, 10, 11, Blocks.SPRUCE_FENCE);
        // passerelles de toit vers les riads voisins (même règle des deux côtés)
        for (Direction dir : new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH}) {
            int nx = dx + dir.getStepX(), nz = dz + dir.getStepZ();
            CityLayout.Plot n = layout.plot(nx, nz);
            int lo = Math.min(dir.getStepX() != 0 ? dx : dz, dir.getStepX() != 0 ? nx : nz);
            boolean bridge = n != null && n.type().equals("riad") && layout.hash(dir.getStepX() != 0 ? lo : dx, dir.getStepZ() != 0 ? lo : dz, dir.getAxis().ordinal() + 20) < 0.6;
            if (!bridge) {
                continue;
            }
            int cx = dir.getStepX() > 0 ? 13 : dir.getStepX() < 0 ? 0 : 7;
            int cz = dir.getStepZ() > 0 ? 13 : dir.getStepZ() < 0 ? 0 : 7;
            int ex = dir.getStepX() > 0 ? 15 : dir.getStepX() < 0 ? 2 : 7;
            int ez = dir.getStepZ() > 0 ? 15 : dir.getStepZ() < 0 ? 2 : 7;
            b.fill(Math.min(cx, ex), 8, Math.min(cz, ez), Math.max(cx, ex), 8, Math.max(cz, ez), Blocks.SPRUCE_PLANKS);
            b.fill(Math.min(cx, ex), 9, Math.min(cz, ez), Math.max(cx, ex), 9, Math.max(cz, ez), Blocks.AIR.defaultBlockState());
        }
        // intérieur : tapis persans, coussins, four tandoor, narguilé, amphores
        Block[] rug = {Blocks.RED_CARPET, Blocks.ORANGE_CARPET, Blocks.YELLOW_CARPET, Blocks.BLUE_CARPET};
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 12; z++) {
                b.set(x, 1, z, rug[(x + z * 2) % rug.length]);
            }
        }
        b.set(11, 1, 3, B.lit(Blocks.FURNACE));
        b.set(11, 2, 3, Blocks.TERRACOTTA);
        b.set(10, 1, 3, Blocks.BREWING_STAND);
        b.set(3, 1, 12, Blocks.DECORATED_POT);
        b.chest(11, 5, 11, Direction.WEST, "city_house", false);
        Deco.bed(b, 3, 5, 3, Direction.SOUTH, Blocks.ORANGE_BED);
        // porte sur la ruelle
        b.fill(7, 1, z0, 8, 2, z0, Blocks.AIR.defaultBlockState());
        // portrait géant du Dictateur sur les façades donnant sur l'avenue
        CityLayout.Plot north = layout.plot(dx, dz - 1);
        if (north != null && north.type().equals("avenue") && h > 0.4) {
            portrait(b, 4, 2, 1);
        }
        if (h > 0.88) {
            b.bot(8, 9, 4, Faction.MILITIA, BotRole.SNIPER, 2);
        } else if (h < 0.15) {
            b.bot(4, 1, 8, Faction.CIVILIAN, BotRole.CIVILIAN, 8);
        }
    }

    /** Portrait géant du Dictateur (laine) sur une façade orientée nord, en z. */
    private static void portrait(B b, int x0, int y0, int z) {
        String[] img = {
                "..KKKK..", ".KKKKKK.", ".KSSSSK.", "KSWKKWSK", "KSSSSSSK", ".SSKKSS.", ".SKKKKS.", "..SSSS..",
                "GGGRRGGG", "GGGRRGGG"};
        for (int j = 0; j < img.length; j++) {
            for (int i = 0; i < 8; i++) {
                char c = img[j].charAt(i);
                Block blk = switch (c) {
                    case 'K' -> Blocks.BLACK_WOOL;
                    case 'S' -> Blocks.BROWN_WOOL;
                    case 'W' -> Blocks.WHITE_WOOL;
                    case 'G' -> Blocks.GREEN_WOOL;
                    case 'R' -> Blocks.RED_WOOL;
                    default -> Blocks.WHITE_WOOL;
                };
                b.set(x0 + i, y0 + 9 - j, z, blk);
            }
        }
    }

    /** Palais fortifié du Dictateur (3 x 3 chunks). */
    private static void palace(B b) {
        int n = 47;
        b.ground(0, 0, n, n, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), 26);
        // enceinte crénelée et tours d'angle avec mitrailleuses
        b.walls(1, 0, 1, n - 1, 7, n - 1, Blocks.CUT_RED_SANDSTONE.defaultBlockState());
        for (int i = 1; i < n; i += 2) {
            b.set(i, 8, 1, Blocks.RED_SANDSTONE_WALL);
            b.set(i, 8, n - 1, Blocks.RED_SANDSTONE_WALL);
            b.set(1, 8, i, Blocks.RED_SANDSTONE_WALL);
            b.set(n - 1, 8, i, Blocks.RED_SANDSTONE_WALL);
        }
        int[][] corners = {{1, 1}, {n - 6, 1}, {1, n - 6}, {n - 6, n - 6}};
        for (int[] c : corners) {
            b.fill(c[0], 0, c[1], c[0] + 5, 13, c[1] + 5, Blocks.CUT_RED_SANDSTONE);
            b.fill(c[0] + 1, 1, c[1] + 1, c[0] + 4, 12, c[1] + 4, Blocks.AIR.defaultBlockState());
            b.fill(c[0] + 1, 13, c[1] + 1, c[0] + 4, 13, c[1] + 4, Blocks.SMOOTH_RED_SANDSTONE);
            b.fill(c[0] + 1, 1, c[1] + 1, c[0] + 1, 13, c[1] + 1, B.facing(Blocks.LADDER, Direction.SOUTH));
            b.walls(c[0], 14, c[1], c[0] + 5, 14, c[1] + 5, Blocks.RED_SANDSTONE_WALL.defaultBlockState());
            Deco.searchlight(b, c[0] + 4, 14, c[1] + 4);
            b.turret(c[0] + 3, 14, c[1] + 3, c[0] < 10 && c[1] < 10 ? MountedGunEntity.Kind.M2 : MountedGunEntity.Kind.MAXIM,
                    c[1] < 10 ? 180 : 0, Faction.MILITIA);
        }
        // porte d'entrée gardée (sud)
        b.fill(21, 1, n - 1, 26, 5, n - 1, Blocks.AIR.defaultBlockState());
        Deco.sandbags(b, 18, 0, n + 1 > n ? n - 3 : n - 3, 20, n - 3);
        Deco.sandbags(b, 27, 0, n - 3, 29, n - 3);
        Deco.czechHedgehog(b, 16, 0, n - 2);
        Deco.czechHedgehog(b, 31, 0, n - 2);
        // cour : fontaine, palmiers
        b.fill(20, -1, 30, 27, -1, 37, Blocks.CUT_SANDSTONE);
        b.fill(21, 0, 31, 26, 0, 36, Blocks.SANDSTONE_WALL);
        b.fill(22, 0, 32, 25, 0, 35, Blocks.WATER);
        for (int[] p : new int[][]{{8, 30}, {39, 30}, {8, 40}, {39, 40}}) {
            b.fill(p[0], 0, p[1], p[0], 6, p[1], Blocks.JUNGLE_LOG);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                b.fill(p[0] + d.getStepX(), 6, p[1] + d.getStepZ(), p[0] + d.getStepX() * 2, 6, p[1] + d.getStepZ() * 2,
                        Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
            }
        }
        // palais : 3 étages, coupole, salle du trône, salle forte (carte rouge)
        int px0 = 10, pz0 = 8, px1 = 37, pz1 = 26;
        for (int f = 0; f < 3; f++) {
            int y = f * 5;
            b.fill(px0, y, pz0, px1, y, pz1, Blocks.SMOOTH_SANDSTONE);
            b.walls(px0, y, pz0, px1, y + 4, pz1, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
            for (int x = px0 + 2; x < px1; x += 3) {
                b.fill(x, y + 1, pz1, x, y + 3, pz1, Blocks.ORANGE_STAINED_GLASS_PANE.defaultBlockState());
            }
            for (int x = px0 + 4; x < px1; x += 6) {
                b.set(x, y + 4, (pz0 + pz1) / 2, Blocks.GLOWSTONE);
            }
            b.fill(px1 - 1, y, pz0 + 1, px1 - 1, y + 4, pz0 + 1, B.facing(Blocks.LADDER, Direction.WEST));
        }
        b.fill(px0, 15, pz0, px1, 15, pz1, Blocks.SMOOTH_SANDSTONE);
        b.set(px1 - 1, 15, pz0 + 1, Blocks.AIR);
        for (int i = 0; i < 7; i++) {
            int r = 7 - i;
            b.fill(23 - r, 16 + i, 17 - r, 24 + r, 16 + i, 17 + r, (i == 6 ? Blocks.GOLD_BLOCK : Blocks.SMOOTH_SANDSTONE).defaultBlockState());
        }
        b.fill(21, 1, pz1, 26, 3, pz1, Blocks.AIR.defaultBlockState());
        // salle du trône
        b.fill(22, 1, pz1 - 1, 25, 1, pz0 + 3, Blocks.RED_CARPET);
        b.fill(22, 1, pz0 + 1, 25, 2, pz0 + 1, Blocks.GOLD_BLOCK);
        b.set(23, 1, pz0 + 2, B.stairs(Blocks.RED_NETHER_BRICK_STAIRS, Direction.NORTH));
        b.set(24, 1, pz0 + 2, B.stairs(Blocks.RED_NETHER_BRICK_STAIRS, Direction.NORTH));
        portrait(b, 20, 2, pz0 + 1);
        portrait(b, 20, 6, pz1 + 1);
        // salle forte
        b.walls(px0 + 1, 6, pz0 + 1, px0 + 7, 9, pz0 + 7, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        CityGen.vaultDoor(b, px0 + 7, 6, pz0 + 4, Direction.EAST, 2);
        for (int z = pz0 + 2; z <= pz0 + 6; z += 2) {
            b.chest(px0 + 2, 6, z, Direction.EAST, "city_vault", false);
        }
        b.set(px0 + 4, 9, pz0 + 4, Blocks.GLOWSTONE);
        // casernement
        for (int x = px0 + 10; x <= px0 + 20; x += 2) {
            Deco.bed(b, x, 11, pz0 + 2, Direction.SOUTH, Blocks.GREEN_BED);
        }
        Deco.armoryRack(b, px0 + 12, 11, pz1 - 1, Direction.NORTH);
        b.chest(px0 + 14, 11, pz1 - 1, Direction.NORTH, "military_crate", false);
        Deco.explosiveBarrel(b, 4, 0, 20);
        Deco.explosiveBarrel(b, 4, 0, 21);
        // garnison
        b.bot(23, 1, pz0 + 4, Faction.MILITIA, BotRole.OFFICER, 8);
        b.bot(23, 1, pz0 + 4, Faction.MILITIA, BotRole.BOSS, 8);              // Général Suprême de la Milice
        BossVaults.build(b, 23, 1, pz0 + 5, 12, false, fr.modeminecraft.item.BossCardItem.VaultType.DESERT_PALACE);     // salle secrète derrière le trône
        b.pair(23, 0, 40, Faction.MILITIA, 18);
        b.pair(14, 0, 30, Faction.MILITIA, 18);
        b.pair(30, 6, 18, Faction.MILITIA, 10);
        b.bot(23, 16, 10, Faction.MILITIA, BotRole.SNIPER, 3);
    }

    /** Grand Souk couvert (3 x 2 chunks) : étals, épices, tapis, narguilés, marchand. */
    private static void souk(B b) {
        int w = 47, d = 31;
        b.ground(0, 0, w, d, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), 10);
        Block[] cloth = {Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL, Blocks.PURPLE_WOOL, Blocks.BLUE_WOOL};
        for (int x = 0; x <= w; x++) {
            for (int z = 0; z <= d; z++) {
                if ((x % 8 == 0) && (z % 8 == 0)) {
                    b.fill(x, 0, z, x, 5, z, Blocks.SPRUCE_LOG);
                }
                if ((x / 4 + z / 4) % 3 != 0) {
                    b.set(x, 6, z, cloth[(x / 4 + z / 5) % cloth.length]);
                }
            }
        }
        Block[] spice = {Blocks.ORANGE_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE_POWDER, Blocks.BROWN_CONCRETE_POWDER};
        for (int sx = 2; sx < w - 4; sx += 8) {
            for (int sz = 2; sz < d - 3; sz += 8) {
                // étal : comptoir, épices, tonneaux
                b.fill(sx, 0, sz, sx + 4, 0, sz, B.slab(Blocks.SPRUCE_SLAB, true));
                for (int i = 0; i < 4; i++) {
                    b.set(sx + i, 1, sz, spice[(sx + sz + i) % spice.length].defaultBlockState());
                }
                b.chest(sx + 4, 0, sz + 2, Direction.WEST, "city_shop", true);
                b.set(sx + 1, 0, sz + 2, B.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
                b.set(sx + 2, 0, sz + 3, Blocks.DECORATED_POT);
                b.set(sx + 2, 5, sz + 2, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
                if ((sx + sz) % 3 == 0) {
                    b.set(sx + 3, 0, sz + 3, Blocks.BREWING_STAND);                         // narguilé
                }
            }
        }
        b.bot(24, 0, 16, Faction.TRADER, BotRole.TRADER, 4);
        b.bot(12, 0, 10, Faction.CIVILIAN, BotRole.CONTRACTOR, 2);
        b.bot(10, 0, 20, Faction.CIVILIAN, BotRole.CIVILIAN, 16);
        b.bot(35, 0, 10, Faction.CIVILIAN, BotRole.CIVILIAN, 16);
        b.pair(20, 0, 28, Faction.MILITIA, 24);
    }

    // =====================================================================================================
    // FORÊT A : bastion rebelle arboricole (cabanes à 12 blocs, ponts suspendus, caches)
    // =====================================================================================================
    private static final int DECK = 12;

    private static boolean isTree(CityLayout layout, int dx, int dz) {
        CityLayout.Plot p = layout.plot(dx, dz);
        return p != null && (p.type().equals("treehouse") || p.type().equals("hq_tree"));
    }

    private static void treehouse(B b, CityLayout layout, int dx, int dz, boolean hq) {
        int top = DECK + 12;
        // dégagement léger autour du tronc
        b.fill(4, 0, 4, 11, top + 2, 11, Blocks.AIR.defaultBlockState());
        b.ground(5, 5, 10, 10, Blocks.PODZOL.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 0);
        b.fill(7, -1, 7, 8, top, 8, Blocks.DARK_OAK_LOG);                               // tronc 2 x 2
        for (int y = top - 5; y <= top + 2; y++) {                                       // houppier
            int r = y > top ? 3 : 5;
            for (int x = 7 - r; x <= 8 + r; x++) {
                for (int z = 7 - r; z <= 8 + r; z++) {
                    double ddx = x - 7.5, ddz = z - 7.5;
                    if (ddx * ddx + ddz * ddz <= r * r && b.get(x, y, z).isAir()) {
                        b.set(x, y, z, Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
                    }
                }
            }
        }
        // plate-forme et cabane fortifiée
        int r = hq ? 6 : 4;
        b.fill(7 - r, DECK, 7 - r, 8 + r, DECK, 8 + r, Blocks.SPRUCE_PLANKS);
        b.fill(7, DECK, 7, 8, DECK, 8, Blocks.DARK_OAK_LOG);
        b.walls(7 - r, DECK + 1, 7 - r, 8 + r, DECK + 1, 8 + r, Blocks.SPRUCE_FENCE.defaultBlockState());
        b.walls(5, DECK + 1, 5, 10, DECK + 4, 10, Blocks.SPRUCE_LOG.defaultBlockState());
        b.fill(6, DECK + 1, 5, 6, DECK + 2, 5, Blocks.AIR.defaultBlockState());
        b.fill(5, DECK + 5, 5, 10, DECK + 5, 10, Blocks.SPRUCE_SLAB);
        b.set(9, DECK + 4, 9, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        b.fill(6, 0, 6, 6, DECK, 6, B.facing(Blocks.LADDER, Direction.NORTH));             // échelle le long du tronc
        b.set(6, DECK, 6, B.facing(Blocks.LADDER, Direction.NORTH));
        // ponts suspendus vers les arbres voisins (bois et cordes)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!isTree(layout, dx + dir.getStepX(), dz + dir.getStepZ())) {
                continue;
            }
            // tablier de 2 de large jusqu'au bord du chunk, cordes (chaînes) de chaque côté
            int from = 8 + r + 1, to = 15;
            for (int i = from; i <= to; i++) {
                int a = dir.getStepX() + dir.getStepZ() > 0 ? i : 15 - i;
                for (int lane = 7; lane <= 8; lane++) {
                    if (dir.getAxis() == Direction.Axis.X) {
                        b.set(a, DECK, lane, B.slab(Blocks.SPRUCE_SLAB, true));
                    } else {
                        b.set(lane, DECK, a, B.slab(Blocks.SPRUCE_SLAB, true));
                    }
                }
                if (dir.getAxis() == Direction.Axis.X) {
                    b.set(a, DECK + 1, 6, Blocks.CHAIN);
                    b.set(a, DECK + 1, 9, Blocks.CHAIN);
                } else {
                    b.set(6, DECK + 1, a, Blocks.CHAIN);
                    b.set(9, DECK + 1, a, Blocks.CHAIN);
                }
            }
            // ouverture dans la rambarde
            if (dir.getAxis() == Direction.Axis.X) {
                int e = dir.getStepX() > 0 ? 8 + r : 7 - r;
                b.fill(e, DECK + 1, 7, e, DECK + 1, 8, Blocks.AIR.defaultBlockState());
            } else {
                int e = dir.getStepZ() > 0 ? 8 + r : 7 - r;
                b.fill(7, DECK + 1, e, 8, DECK + 1, e, Blocks.AIR.defaultBlockState());
            }
        }
        // atelier d'armurerie artisanale et cache
        b.set(6, DECK + 1, 9, Blocks.CRAFTING_TABLE);
        b.set(7, DECK + 1, 9, Blocks.SMITHING_TABLE);
        b.chest(9, DECK + 1, 6, Direction.WEST, "rebel_cache", false);
        if (hq) {
            b.set(8, DECK + 1, 9, Blocks.ANVIL);
            b.set(9, DECK + 1, 9, Blocks.GRINDSTONE);
            Deco.armoryRack(b, 9, DECK + 2, 7, Direction.WEST);
            b.chest(9, DECK + 1, 7, Direction.WEST, "rebel_cache", false);
            b.bot(6, DECK + 1, 7, Faction.REBEL, BotRole.OFFICER, 6);
            b.bot(8, DECK + 1, 4, Faction.REBEL, BotRole.CONTRACTOR, 2);
        }
        b.bot(4, DECK + 1, 4, Faction.REBEL, BotRole.SOLDIER, 8);
        if (b.chance(0.2f)) {
            b.bot(7, DECK + 13, 7, Faction.REBEL, BotRole.SNIPER, 2);
        }
    }

    /** Campement au sol : feu de camp et marmite, billots, peaux tendues, séchoir, cibles, trappe de cache. */
    private static void rebelCamp(B b, double h) {
        b.fill(3, 0, 3, 12, 6, 12, Blocks.AIR.defaultBlockState());
        b.ground(3, 3, 12, 12, Blocks.COARSE_DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 0);
        b.set(7, 0, 7, B.lit(Blocks.CAMPFIRE));
        b.set(8, 0, 7, Blocks.CAULDRON);
        for (int[] l : new int[][]{{5, 5}, {10, 5}, {5, 10}}) {
            b.set(l[0], 0, l[1], Blocks.STRIPPED_OAK_LOG);                                     // billots
        }
        b.fill(10, 0, 9, 10, 2, 9, Blocks.OAK_FENCE);                                          // peau tendue
        b.fill(10, 0, 11, 10, 2, 11, Blocks.OAK_FENCE);
        b.fill(10, 1, 10, 10, 2, 10, Blocks.BROWN_WOOL);
        b.fill(3, 0, 8, 3, 2, 8, Blocks.OAK_FENCE);                                            // séchoir à viande
        b.fill(3, 2, 9, 3, 2, 11, Blocks.OAK_FENCE);
        b.fill(3, 0, 12, 3, 2, 12, Blocks.OAK_FENCE);
        b.set(3, 1, 10, Blocks.RED_CARPET);
        if (h < 0.5) {
            b.set(12, 0, 3, fr.modeminecraft.registry.ModBlocks.SHOOTING_TARGET.get());           // cibles de tir
            b.set(12, 0, 6, fr.modeminecraft.registry.ModBlocks.SHOOTING_TARGET.get());
        }
        // trappe camouflée vers une cache souterraine
        b.fill(4, -5, 3, 7, -2, 6, Blocks.DIRT);
        b.fill(5, -4, 4, 6, -2, 5, Blocks.AIR.defaultBlockState());
        b.set(5, -1, 4, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
        b.fill(5, -4, 4, 5, -2, 4, B.facing(Blocks.LADDER, Direction.SOUTH));
        b.set(5, -4, 3, Blocks.DIRT);
        b.chest(6, -4, 5, Direction.WEST, "rebel_cache", false);
        b.set(6, -2, 4, Blocks.LANTERN);
        // tente
        b.fill(11, 0, 11, 13, 0, 13, Blocks.GREEN_CARPET);
        b.fill(11, 1, 11, 13, 1, 11, Blocks.GREEN_WOOL);
        b.fill(12, 2, 11, 12, 2, 13, Blocks.GREEN_WOOL);
        b.fill(11, 1, 13, 13, 1, 13, Blocks.GREEN_WOOL);
        b.bot(8, 0, 9, Faction.REBEL, BotRole.SOLDIER, 12);
        if (h < 0.4) {
            b.vehicle(4, 0, 12, fr.modeminecraft.entity.VehicleEntity.Type.QUAD, 45);
        }
        if (h > 0.6) {
            b.bot(5, 0, 8, Faction.REBEL, BotRole.SOLDIER, 12);
        }
    }

    // =====================================================================================================
    // FORÊT B : base militaire secrète (double clôture, miradors, hangars, radar, bunker sur 3 niveaux)
    // =====================================================================================================
    private static void militaryBase(B b, String type, int dx, int dz, int rad, double h) {
        var scenarioSite = fr.modeminecraft.scenario.ScenarioSites.byPlot(type);
        if (scenarioSite != null) {
            ScenarioBuildings.build(b, scenarioSite, fr.modeminecraft.scenario.Side.US);   // bâtiments du scénario
            return;
        }
        boolean wide = type.equals("hangar");
        int w = wide ? 31 : 15;
        b.ground(0, 0, w, w, Blocks.GRAVEL.defaultBlockState(), Blocks.STONE.defaultBlockState(), 26);
        // double grillage barbelé sur le bord extérieur de la base
        if (dx == -rad) {
            Deco.fence(b, 0, 0, 0, 15, 4);
            Deco.fence(b, 3, 0, 3, 15, 4);
        }
        if (dx == rad) {
            Deco.fence(b, 15, 0, 15, 15, 4);
            Deco.fence(b, 12, 0, 12, 15, 4);
        }
        if (dz == -rad) {
            Deco.fence(b, 0, 0, 15, 0, 4);
            Deco.fence(b, 0, 3, 15, 3, 4);
        }
        if (dz == rad) {
            if (dx == 0) {
                Deco.sandbags(b, 4, 0, 13, 6, 13);                          // entrée gardée
                Deco.sandbags(b, 9, 0, 13, 11, 13);
            } else {
                Deco.fence(b, 0, 15, 15, 15, 4);
                Deco.fence(b, 0, 12, 15, 12, 4);
            }
        }
        // batteries fixes de roquettes de 122 mm sur le périmètre, tournées vers l'extérieur
        if (type.equals("perimeter") && h < 0.3) {
            b.rocketBattery(8, 0, 8, Integer.signum(dx) * (Math.abs(dx) == rad ? 1 : 0), Integer.signum(dz) * (Math.abs(dz) == rad ? 1 : 0));
        }
        // champ de mines antichars entre les deux grillages (mise à jour 0.4)
        for (int k = 2; k <= 13; k += 4) {
            if (dx == -rad) b.mine(1 + (k % 2), 0, k, false);
            if (dx == rad) b.mine(13 + (k % 2), 0, k, false);
            if (dz == -rad) b.mine(k, 0, 1 + (k % 2), false);
            if (dz == rad && dx != 0) b.mine(k, 0, 13 + (k % 2), false);
        }
        switch (type) {
            case "airstrip" -> Airfields.airstrip(b, 96, Faction.GARRISON);
            case "aircraft_hangar" -> Airfields.megaHangar(b, Faction.GARRISON, 0.45f);
            case "mirador" -> {
                Deco.watchtower(b, 6, 6, 9, Blocks.SPRUCE_LOG, Blocks.SPRUCE_PLANKS);
                b.turret(8, 10, 8, MountedGunEntity.Kind.MG42, dz < 0 ? 180 : 0, Faction.GARRISON);
                Deco.searchlight(b, 6, 10, 9);
            }
            case "hangar" -> {
                for (int x = 2; x <= 29; x++) {
                    for (int i = 0; i <= 90; i++) {                                          // voûte pleine (sans jour)
                        double a = Math.PI * i / 90;
                        int z = (int) Math.round(16 - Math.cos(a) * 13), y = (int) Math.round(Math.sin(a) * 11);
                        b.set(x, y, z, (x % 5 == 0 ? Blocks.IRON_BLOCK : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState());
                    }
                }
                b.fill(2, 0, 3, 2, 11, 29, Blocks.LIGHT_GRAY_CONCRETE);
                b.fill(2, 1, 9, 2, 7, 23, Blocks.AIR.defaultBlockState());                      // grande porte
                b.fill(29, 0, 3, 29, 11, 29, Blocks.LIGHT_GRAY_CONCRETE);                         // mur du fond (fermé)
                for (int z = 3; z <= 29; z++) {
                    for (int y = 0; y <= 11; y++) {
                        double r = Math.hypot(z - 16, y * 13.0 / 11);
                        if (r > 13.4) {                                                       // hors de la voûte : air
                            b.set(2, y, z, Blocks.AIR);
                            b.set(29, y, z, Blocks.AIR);
                        }
                    }
                }
                Deco.door(b, 29, 0, 16, Direction.WEST, Blocks.IRON_DOOR);                        // porte de service
                b.set(28, 1, 15, B.facing(Blocks.STONE_BUTTON, Direction.WEST));
                b.set(30, 1, 17, B.facing(Blocks.STONE_BUTTON, Direction.EAST));
                for (int x = 6; x <= 26; x += 5) {                                              // rampes d'éclairage
                    b.set(x, 10, 16, Blocks.SEA_LANTERN);
                    b.set(x, 6, 8, Blocks.SEA_LANTERN);
                    b.set(x, 6, 24, Blocks.SEA_LANTERN);
                }
                b.fill(3, -1, 3, 29, -1, 29, Blocks.GRAY_CONCRETE);
                // 2 ou 3 blindés lourds prêts au combat, face à la grande porte
                b.milVehicle(10, 0, 11, b.chance(0.5f) ? fr.modeminecraft.entity.military.MilType.BMP_2 : fr.modeminecraft.entity.military.MilType.BTR_80, 90);
                b.milVehicle(12, 0, 21, b.chance(0.5f) ? fr.modeminecraft.entity.military.MilType.T_80BVM : fr.modeminecraft.entity.military.MilType.BRDM_2, 90);
                if (b.chance(0.5f)) {
                    b.milVehicle(21, 0, 9, b.chance(0.25f) ? fr.modeminecraft.entity.military.MilType.T_14_ARMATA : fr.modeminecraft.entity.military.MilType.BM_21_GRAD, 90);
                }
                Hangars.baseFittings(b);
                forklift(b, 20, 0, 14);
                for (int z = 6; z <= 26; z += 4) {
                    if (z != 14 && z != 18) {
                        b.chest(27, 0, z, Direction.WEST, "military_base", false);
                    }
                }
                // armurerie du hangar : râteliers contre le mur nord
                for (int x = 14; x <= 24; x += 2) {
                    Deco.armoryRack(b, x, 1, 5, Direction.SOUTH);
                }
                b.set(19, 0, 5, fr.modeminecraft.registry.ModBlocks.ARMAMENT_TABLE.get());
                b.set(16, 10, 16, Blocks.SEA_LANTERN);
                b.set(10, 9, 16, Blocks.SEA_LANTERN);
                b.set(22, 9, 16, Blocks.SEA_LANTERN);
                b.pair(14, 0, 16, Faction.GARRISON, 14);
                b.bot(16, 12, 16, Faction.GARRISON, BotRole.SNIPER, 2);
            }
            case "radar" -> {
                b.fill(4, -1, 4, 11, 4, 11, Blocks.GRAY_CONCRETE);                              // poste de contrôle
                b.fill(5, 0, 5, 10, 3, 10, Blocks.AIR.defaultBlockState());
                Deco.door(b, 7, 0, 4, Direction.NORTH, Blocks.IRON_DOOR);
                b.set(6, 1, 3, B.facing(Blocks.STONE_BUTTON, Direction.NORTH));
                b.set(8, 1, 5, B.facing(Blocks.STONE_BUTTON, Direction.SOUTH));
                b.set(4, 2, 8, Blocks.GRAY_STAINED_GLASS);
                b.set(11, 2, 8, Blocks.GRAY_STAINED_GLASS);
                for (int x = 5; x <= 10; x++) {                                                  // consoles
                    b.set(x, 0, 10, (x % 2 == 0 ? Blocks.OBSERVER : Blocks.IRON_BLOCK).defaultBlockState());
                    b.set(x, 1, 10, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, x % 2 == 1));
                }
                Deco.desk(b, 6, 0, 8, Direction.NORTH);
                Deco.desk(b, 9, 0, 8, Direction.NORTH);
                b.set(7, 3, 7, Blocks.SEA_LANTERN);
                b.chest(10, 0, 6, Direction.WEST, "military_base", false);
                b.bot(7, 0, 7, Faction.GARRISON, BotRole.OFFICER, 4);
                for (int y = 5; y < 20; y++) {
                    b.set(6, y, 6, Blocks.IRON_BARS);
                    b.set(9, y, 6, Blocks.IRON_BARS);
                    b.set(6, y, 9, Blocks.IRON_BARS);
                    b.set(9, y, 9, Blocks.IRON_BARS);
                }
                b.fill(6, 20, 6, 9, 20, 9, Blocks.IRON_BLOCK);
                b.fill(4, 22, 7, 11, 24, 8, Blocks.WHITE_CONCRETE);                               // antenne radar
                b.fill(7, 21, 7, 8, 21, 8, Blocks.LIGHTNING_ROD);
                // objectifs prioritaires : batterie S-400 déployée (défense aérienne automatique), parfois un Iskander
                b.strategic(8, 0, 14, fr.modeminecraft.entity.military.MilType.S_400, 90);
                if (b.chance(0.45f)) {
                    b.strategic(8, 0, 1, fr.modeminecraft.entity.military.MilType.ISKANDER, 90);
                }

            }
            case "flak" -> {
                Deco.sandbags(b, 4, 0, 4, 11, 4);
                Deco.sandbags(b, 4, 0, 11, 11, 11);
                Deco.sandbags(b, 4, 0, 4, 4, 11);
                Deco.sandbags(b, 11, 0, 4, 11, 11);
                b.turret(8, 0, 8, MountedGunEntity.Kind.FLAK, 0, Faction.GARRISON);
                b.chest(5, 0, 5, Direction.SOUTH, "military_base", false);
            }
            case "barracks" -> {
                b.fill(2, 0, 3, 13, 0, 12, Blocks.SPRUCE_PLANKS);
                b.walls(2, 0, 3, 13, 4, 12, Blocks.GREEN_TERRACOTTA.defaultBlockState());
                b.fill(2, 4, 3, 13, 4, 12, Blocks.GREEN_TERRACOTTA);
                b.fill(7, 1, 3, 8, 2, 3, Blocks.AIR.defaultBlockState());
                for (int x = 3; x <= 12; x += 2) {
                    Deco.bed(b, x, 1, 10, Direction.NORTH, Blocks.GREEN_BED);
                    b.chest(x, 1, 11, Direction.NORTH, "military_base", true);
                }
                b.set(5, 3, 7, Blocks.GLOWSTONE);
                b.set(10, 3, 7, Blocks.GLOWSTONE);
                b.set(2, 2, 7, Blocks.GRAY_STAINED_GLASS);
                b.set(13, 2, 7, Blocks.GRAY_STAINED_GLASS);
                Deco.armoryRack(b, 3, 1, 4, Direction.SOUTH);
                Deco.armoryRack(b, 12, 1, 4, Direction.SOUTH);
                Deco.camoNet(b, 1, 2, 14, 13, 5, false);
                b.set(4, 0, 1, fr.modeminecraft.registry.ModBlocks.SHOOTING_TARGET.get());
                b.pair(6, 0, 1, Faction.GARRISON, 24);
            }
            case "tanks" -> {
                for (int[] c : new int[][]{{4, 5}, {11, 10}}) {
                    for (int y = 0; y < 3; y++) {
                        b.set(c[0] - 2, y, c[1] - 2, Blocks.IRON_BARS);
                        b.set(c[0] + 2, y, c[1] + 2, Blocks.IRON_BARS);
                    }
                    for (int y = 3; y < 9; y++) {
                        for (int x = -3; x <= 3; x++) {
                            for (int z = -3; z <= 3; z++) {
                                if (x * x + z * z <= 9) {
                                    b.set(c[0] + x, y, c[1] + z, (y == 3 || y == 8 || x * x + z * z >= 7 ? (y == 8 ? Blocks.SMOOTH_STONE : Blocks.LIGHT_GRAY_CONCRETE) : Blocks.AIR).defaultBlockState());
                                }
                            }
                        }
                    }
                }
                b.pair(8, 0, 2, Faction.GARRISON, 24);
            }
            case "bunker" -> bunker(b);
            default -> {
                if (h < 0.5) {
                    Deco.czechHedgehog(b, 8, 0, 8);
                }
                if (h > 0.7) {
                    b.pair(7, 0, 7, Faction.GARRISON, 24);
                }
            }
        }
    }

    /**
     * Bunker souterrain sur 3 niveaux reliés par un puits d'échelles : poste de commandement (serveurs, table des
     * cartes) et salle radio ; dortoir et armurerie séparés par une cloison ; salle forte (carte noire) et salle
     * des générateurs. Chaque salle a son plafond, ses portes et son éclairage.
     */
    private static void bunker(B b) {
        b.fill(4, 0, 4, 11, 4, 11, Blocks.GRAY_CONCRETE);                             // blockhaus d'entrée
        b.fill(5, 0, 5, 10, 3, 10, Blocks.AIR.defaultBlockState());
        CityGen.vaultDoor(b, 7, 0, 4, Direction.NORTH, 1);
        b.set(6, 3, 7, Blocks.SEA_LANTERN);
        Deco.camoNet(b, 2, 2, 13, 13, 5, false);
        for (int lvl = 0; lvl < 3; lvl++) {
            int y = -6 - lvl * 6;
            b.fill(1, y - 1, 1, 14, y + 4, 14, Blocks.POLISHED_DEEPSLATE);
            b.fill(2, y, 2, 13, y + 3, 13, Blocks.AIR.defaultBlockState());
            b.fill(2, y - 1, 2, 13, y - 1, 13, Blocks.SMOOTH_STONE);                       // sol
            b.fill(2, y + 4, 2, 13, y + 4, 13, Blocks.POLISHED_DEEPSLATE);                 // plafond
            // cloison est-ouest : deux salles par niveau, reliées par une porte
            b.fill(2, y, 8, 13, y + 3, 8, Blocks.DEEPSLATE_TILES);
            Deco.door(b, 5, y, 8, Direction.SOUTH, Blocks.IRON_DOOR);
            b.set(4, y + 1, 7, B.facing(Blocks.STONE_BUTTON, Direction.NORTH));
            b.set(4, y + 1, 9, B.facing(Blocks.STONE_BUTTON, Direction.SOUTH));
            for (int x = 3; x <= 12; x += 3) {
                b.set(x, y + 3, 5, Blocks.SEA_LANTERN);
                b.set(x, y + 3, 11, Blocks.SEA_LANTERN);
            }
        }
        b.fill(9, -18, 9, 9, 0, 9, Blocks.AIR.defaultBlockState());                          // puits d'échelles
        b.fill(9, -18, 9, 9, -1, 9, B.facing(Blocks.LADDER, Direction.NORTH));
        b.fill(9, -18, 10, 9, -1, 10, Blocks.POLISHED_DEEPSLATE);
        for (int lvl = 0; lvl < 3; lvl++) {
            b.set(9, -6 - lvl * 6, 8, Blocks.AIR);                                          // le puits débouche des deux côtés
            b.set(9, -5 - lvl * 6, 8, Blocks.AIR);
        }
        // niveau 1 (sud) : poste de commandement, serveurs militaires, table des cartes
        for (int x = 3; x <= 12; x++) {
            if (x == 9) continue;
            b.set(x, -6, 13, (x % 2 == 0 ? Blocks.IRON_BLOCK : Blocks.OBSERVER).defaultBlockState());
            b.set(x, -5, 13, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, x % 3 == 0));
        }
        b.set(6, -6, 11, Blocks.CARTOGRAPHY_TABLE);
        b.set(7, -6, 11, Blocks.CARTOGRAPHY_TABLE);
        b.bot(6, -6, 10, Faction.GARRISON, BotRole.OFFICER, 5);
        b.bot(6, -6, 10, Faction.GARRISON, BotRole.BOSS, 5);                   // Commandant des Forces Spéciales
        BossVaults.build(b, 6, -6, 9, 22, true, fr.modeminecraft.item.BossCardItem.VaultType.FOREST_BUNKER);           // niveau -3 du bunker
        // niveau 1 (nord) : salle radio
        Deco.desk(b, 4, -6, 3, Direction.SOUTH);
        Deco.desk(b, 7, -6, 3, Direction.SOUTH);
        b.set(11, -6, 2, Blocks.JUKEBOX);
        b.chest(12, -6, 2, Direction.SOUTH, "military_base", false);
        b.bot(6, -6, 5, Faction.GARRISON, BotRole.DRONE_OPERATOR, 4);
        // niveau 2 (nord) : dortoir ; (sud) : armurerie
        for (int x = 3; x <= 11; x += 2) {
            if (x != 9) Deco.bed(b, x, -12, 2, Direction.SOUTH, Blocks.GREEN_BED);
        }
        b.pair(6, -12, 5, Faction.GARRISON, 4);
        for (int x = 3; x <= 12; x++) {
            if (x == 9) continue;
            Deco.armoryRack(b, x, -11, 13, Direction.NORTH);
            if (x % 2 == 1) b.chest(x, -12, 13, Direction.NORTH, "military_base", false);
        }
        b.set(11, -12, 10, fr.modeminecraft.registry.ModBlocks.ARMAMENT_TABLE.get());
        b.set(12, -12, 10, fr.modeminecraft.registry.ModBlocks.MODIFICATION_TABLE.get());
        // niveau 3 (nord) : salle des générateurs ; (sud) : salle forte (carte noire)
        for (int x = 3; x <= 7; x += 2) {
            b.fill(x, -18, 3, x, -16, 4, Blocks.IRON_BLOCK);
            b.set(x, -15, 3, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        }
        b.walls(2, -18, 9, 7, -15, 13, Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
        CityGen.vaultDoor(b, 7, -18, 11, Direction.EAST, 3);
        b.chest(3, -18, 10, Direction.EAST, "city_vault", false);
        b.chest(3, -18, 12, Direction.EAST, "military_base", false);
        b.set(4, -15, 11, Blocks.SEA_LANTERN);
        b.chest(12, -18, 12, Direction.WEST, "military_base", false);
    }

    static void jeep(B b, int x, int y, int z) {
        b.fill(x, y, z, x + 4, y, z + 2, Blocks.GREEN_TERRACOTTA);
        b.fill(x + 1, y + 1, z, x + 2, y + 1, z + 2, Blocks.GREEN_TERRACOTTA);
        b.set(x + 3, y + 1, z + 1, Blocks.GRAY_STAINED_GLASS);
        b.set(x, y, z - 1, Blocks.BLACK_WOOL);
        b.set(x + 4, y, z - 1, Blocks.BLACK_WOOL);
        b.set(x, y, z + 3, Blocks.BLACK_WOOL);
        b.set(x + 4, y, z + 3, Blocks.BLACK_WOOL);
        b.set(x + 1, y + 2, z + 1, Blocks.IRON_BARS);
    }

    static void forklift(B b, int x, int y, int z) {
        b.fill(x, y, z, x + 1, y + 1, z + 1, Blocks.YELLOW_CONCRETE);
        b.fill(x + 2, y, z, x + 2, y + 3, z, Blocks.IRON_BARS);
        b.fill(x + 2, y, z + 1, x + 2, y + 3, z + 1, Blocks.IRON_BARS);
        b.fill(x + 3, y, z, x + 4, y, z + 1, B.slab(Blocks.SMOOTH_STONE_SLAB, false));
    }

    // =====================================================================================================
    // NEIGE A : cité soviétique (khrouchtchevkas, gare enneigée, usine sidérurgique, place à la statue)
    // =====================================================================================================
    private static void soviet(B b, CityLayout layout, String type, int dx, int dz, int rad, double h) {
        switch (type) {
            case "dormitory", "recruitment", "armory", "canteen", "infirmary", "corporal_barracks", "nco_quarters", "officer_pavilion",
                 "officers_club", "war_room", "command_residence", "general_bunker", "officers_parking", "shooting_range", "prison" ->
                    ScenarioBuildings.build(b, fr.modeminecraft.scenario.ScenarioSites.byPlot(type), fr.modeminecraft.scenario.Side.SOVIET);
            case "road" -> {
                b.ground(0, 0, 15, 15, Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 10);
                if ((Math.abs(dx) == rad || Math.abs(dz) == rad) && h > 0.6) {
                    // batterie de 122 mm en lisière de ville
                    b.rocketBattery(9, 0, 9, Integer.signum(dx) * (Math.abs(dx) == rad ? 1 : 0), Integer.signum(dz) * (Math.abs(dz) == rad ? 1 : 0));
                }
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if ((x * 7 + z * 13) % 9 == 0) {
                            b.set(x, 0, z, Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 1 + (x + z) % 3));
                        }
                    }
                }
                Deco.lampPost(b, 1, 0, 1, Direction.EAST);
                // tuyaux de chauffage aériens
                if (h < 0.6) {
                    b.fill(0, 4, 14, 15, 4, 14, Blocks.LIGHT_GRAY_CONCRETE);
                    for (int x = 0; x < 16; x += 5) {
                        b.fill(x, 0, 14, x, 3, 14, Blocks.IRON_BARS);
                    }
                }
                b.fill(13, 0, 2, 14, 1, 3, Blocks.SNOW_BLOCK);                                   // congère
                if (b.chance(0.45f)) {
                    b.pair(7, 0, 7, Faction.SOVIET, 30);
                }
            }
            case "khrushchyovka" -> {
                b.ground(0, 0, 15, 15, Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState(), 24);
                Block panel = h < 0.5 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.GRAY_CONCRETE;
                for (int f = 0; f < 5; f++) {
                    int y = f * 4;
                    b.fill(1, y, 3, 14, y, 12, Blocks.SMOOTH_STONE);
                    b.walls(1, y, 3, 14, y + 3, 12, panel.defaultBlockState());
                    for (int x = 3; x <= 12; x += 3) {
                        b.fill(x, y + 1, 3, x + 1, y + 2, 3, Blocks.GLASS_PANE.defaultBlockState());
                        b.fill(x, y + 1, 12, x + 1, y + 2, 12, Blocks.GLASS_PANE.defaultBlockState());
                        if (f > 0 && (x + f) % 2 == 0) {
                            b.fill(x - 1, y, 13, x + 2, y, 13, B.slab(Blocks.SMOOTH_STONE_SLAB, true));     // balcons
                            b.fill(x - 1, y + 1, 13, x + 2, y + 1, 13, Blocks.IRON_BARS);
                        }
                    }
                    b.set(4, y + 1, 11, Blocks.POLISHED_BLACKSTONE_WALL);                                   // radiateurs en fonte
                    b.set(11, y + 1, 4, B.lit(Blocks.FURNACE));                                            // poêle à bois
                    b.set(7, y + 3, 7, Blocks.GLOWSTONE);
                    b.fill(13, y, 11, 13, y + 3, 11, B.facing(Blocks.LADDER, Direction.WEST));
                    if (f > 0) {
                        Deco.bed(b, 2, y + 1, 5, Direction.SOUTH, Blocks.BROWN_BED);
                        b.chest(3, y + 1, 4, Direction.SOUTH, "city_house", false);
                    }
                }
                b.fill(1, 20, 3, 14, 20, 12, panel.defaultBlockState());
                b.fill(1, 21, 3, 14, 21, 12, Blocks.SNOW.defaultBlockState());
                b.fill(7, 1, 3, 8, 2, 3, Blocks.AIR.defaultBlockState());
                for (int x = 2; x <= 13; x += 2) {
                    b.set(x, 19, 2, Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN));
                }
                if (h < 0.5) {
                    b.bot(7, 1, 7, Faction.SOVIET, BotRole.SOLDIER, 6);
                }
                if (h > 0.85) {
                    b.bot(7, 21, 7, Faction.SOVIET, BotRole.SNIPER, 2);
                }
            }
            case "station" -> {
                b.ground(0, 0, 15, 15, Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState(), 10);
                b.fill(0, -1, 9, 15, -1, 13, Blocks.GRAVEL);
                b.fill(0, 0, 10, 15, 0, 10, Blocks.RAIL);
                b.fill(0, 0, 12, 15, 0, 12, Blocks.RAIL);
                b.fill(0, 0, 4, 15, 0, 8, Blocks.STONE_BRICKS);                                     // quai
                for (int x = 1; x < 16; x += 5) {
                    b.fill(x, 1, 5, x, 4, 5, Blocks.IRON_BARS);
                }
                b.fill(0, 5, 3, 15, 5, 8, Blocks.SMOOTH_STONE_SLAB);
                b.fill(0, 6, 3, 15, 6, 8, Blocks.SNOW);
                if (Math.floorMod(dx, 2) == 0) {                                                       // wagon enneigé
                    b.fill(2, 1, 12, 12, 3, 12, Blocks.BROWN_TERRACOTTA);
                    b.fill(2, 4, 12, 12, 4, 12, Blocks.SNOW_BLOCK);
                } else {
                    b.fill(3, 1, 0, 12, 4, 2, Blocks.BRICKS);                                       // bâtiment de la gare
                    b.set(7, 4, 3, Blocks.WHITE_CONCRETE);
                }
                if (b.chance(0.4f)) {
                    b.pair(5, 1, 6, Faction.SOVIET, 20);
                }
            }
            case "mil_hangar" -> Hangars.soviet(b);
            case "airstrip" -> Airfields.airstrip(b, 112, Faction.SOVIET);
            case "aircraft_hangar" -> Airfields.megaHangar(b, Faction.SOVIET, 0.3f);
            case "factory" -> {
                int n = 31;
                b.ground(0, 0, n, n, Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 28);
                b.walls(2, 0, 2, n - 2, 12, n - 2, Blocks.BRICKS.defaultBlockState());
                b.fill(2, 12, 2, n - 2, 12, n - 2, Blocks.DEEPSLATE_TILES);
                for (int x = 5; x < n - 4; x += 4) {
                    b.fill(x, 6, 2, x + 1, 9, 2, Blocks.GLASS_PANE.defaultBlockState());
                }
                b.fill(14, 1, 2, 17, 6, 2, Blocks.AIR.defaultBlockState());
                for (int x = 5; x < n - 4; x += 3) {                                                  // hauts fourneaux
                    b.set(x, 1, 8, B.lit(Blocks.BLAST_FURNACE));
                    b.set(x, 2, 8, Blocks.IRON_BLOCK);
                }
                b.fill(4, 0, 15, n - 4, 0, 15, Blocks.RAIL);                                         // convoyeur
                b.set(10, 1, 22, Blocks.LAVA_CAULDRON);
                b.set(20, 1, 22, Blocks.LAVA_CAULDRON);
                b.fill(4, 10, 15, n - 4, 10, 15, Blocks.IRON_BARS);                                   // pont roulant
                for (int[] c : new int[][]{{6, 26}, {24, 26}}) {                                      // cheminées
                    b.fill(c[0], 0, c[1], c[0] + 1, 27, c[1] + 1, Blocks.BRICKS);
                    b.set(c[0], 28, c[1], B.lit(Blocks.CAMPFIRE));
                }
                for (int x = 6; x < n - 6; x += 6) {
                    b.set(x, 11, 16, Blocks.SEA_LANTERN);
                }
                b.chest(27, 1, 27, Direction.WEST, "military_crate", false);
                b.pair(15, 1, 12, Faction.SOVIET, 12);
                b.bot(20, 1, 20, Faction.SOVIET, BotRole.OFFICER, 10);
                b.bot(20, 1, 20, Faction.SOVIET, BotRole.BOSS, 8);            // Maréchal de l'Est
                BossVaults.build(b, 20, 1, 22, 12, false, fr.modeminecraft.item.BossCardItem.VaultType.SOVIET_HQ);    // quartier général
            }
            case "plaza" -> {
                b.ground(0, 0, 31, 31, Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState(), 20);
                b.fill(10, -1, 10, 21, -1, 21, Blocks.POLISHED_ANDESITE);
                // statue soviétique
                b.fill(13, 0, 13, 18, 1, 18, Blocks.POLISHED_GRANITE);
                b.fill(15, 2, 15, 16, 6, 16, Blocks.STONE);
                b.fill(14, 7, 15, 17, 11, 16, Blocks.STONE);
                b.fill(15, 12, 15, 16, 13, 16, Blocks.STONE);
                b.fill(18, 10, 15, 19, 14, 15, Blocks.STONE);                                          // bras levé
                b.set(15, 14, 15, Blocks.RED_WOOL);
                for (int i = 10; i <= 21; i += 11) {
                    b.fill(i, 0, 10, i, 6, 10, Blocks.IRON_BARS);
                    b.set(i, 6, 11, Blocks.RED_BANNER);
                }
                Deco.lampPost(b, 9, 0, 22, Direction.EAST);
                Deco.lampPost(b, 22, 0, 9, Direction.WEST);
                Deco.bench(b, 12, 0, 23, Direction.NORTH);
                b.pair(20, 0, 20, Faction.SOVIET, 16);
                b.bot(12, 0, 21, Faction.CIVILIAN, BotRole.CONTRACTOR, 2);
            }
            default -> {
            }
        }
    }

    // =====================================================================================================
    // NEIGE B : station scientifique polaire (dômes géodésiques reliés par des tunnels pressurisés)
    // =====================================================================================================
    private static void polar(B b, CityLayout layout, String type, int dx, int dz, double h) {
        b.ground(0, 0, 15, 15, Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState(), 14);
        switch (type) {
            case "dome" -> {
                double r = 6.5;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y <= 7; y++) {
                            double d = Math.sqrt((x - 7.5) * (x - 7.5) + (z - 7.5) * (z - 7.5) + y * y * 1.2);
                            if (d <= r && d > r - 1.2) {
                                boolean pane = (x + y * 2 + z) % 4 == 0 && y > 1;
                                b.set(x, y, z, (pane ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.WHITE_CONCRETE).defaultBlockState());
                            }
                        }
                    }
                }
                b.fill(2, -1, 2, 13, -1, 13, Blocks.SMOOTH_QUARTZ);
                b.set(7, 6, 7, Blocks.SEA_LANTERN);
                // sas de dépressurisation (vers chaque tunnel)
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    int ex = 7 + dir.getStepX() * 6, ez = 7 + dir.getStepZ() * 6;
                    b.fill(Math.min(7, ex), 0, Math.min(7, ez), Math.max(8, ex), 2, Math.max(8, ez), Blocks.AIR.defaultBlockState());
                }
                int kind = (int) (h * 4);
                switch (kind) {
                    case 0 -> {   // laboratoire : cuves biologiques lumineuses
                        for (int[] p : new int[][]{{4, 4}, {11, 4}, {4, 11}, {11, 11}}) {
                            b.set(p[0], 0, p[1], Blocks.SEA_LANTERN);
                            b.fill(p[0], 1, p[1], p[0], 2, p[1], Blocks.LIME_STAINED_GLASS);
                            b.set(p[0], 3, p[1], Blocks.GLASS);
                        }
                        b.set(6, 0, 4, Blocks.BREWING_STAND);
                        b.chest(9, 0, 4, Direction.SOUTH, "lab", false);
                    }
                    case 1 -> {   // cultures hydroponiques sous lampes UV
                        for (int x = 4; x <= 11; x++) {
                            b.set(x, 0, 4, Blocks.FARMLAND);
                            b.set(x, 0, 11, Blocks.FARMLAND);
                            b.set(x, 1, 4, Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 5));
                            b.set(x, 1, 11, Blocks.CARROTS.defaultBlockState().setValue(BlockStateProperties.AGE_7, 6));
                            b.set(x, 3, 4, Blocks.MAGENTA_STAINED_GLASS);
                            b.set(x, 3, 11, Blocks.MAGENTA_STAINED_GLASS);
                        }
                        b.fill(4, 4, 5, 11, 4, 10, Blocks.GLOWSTONE);
                    }
                    case 2 -> {   // quartiers
                        Deco.bed(b, 3, 0, 4, Direction.SOUTH, Blocks.WHITE_BED);
                        Deco.bed(b, 11, 0, 4, Direction.SOUTH, Blocks.WHITE_BED);
                        b.chest(4, 0, 11, Direction.NORTH, "military_crate", true);
                    }
                    default -> {  // générateur
                        b.fill(5, 0, 5, 6, 1, 6, Blocks.IRON_BLOCK);
                        b.set(9, 0, 9, B.lit(Blocks.BLAST_FURNACE));
                        b.set(9, 1, 9, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
                    }
                }
                b.bot(7, 0, 9, Faction.ARCTIC, h > 0.6 ? BotRole.OFFICER : BotRole.SOLDIER, 6);
            }
            case "tunnel" -> {
                boolean alongX = Math.floorMod(dz, 2) == 0;
                for (int i = 0; i < 16; i++) {
                    int x = alongX ? i : 6, z = alongX ? 6 : i;
                    b.fill(alongX ? i : 6, 0, alongX ? 6 : i, alongX ? i : 9, 0, alongX ? 9 : i, Blocks.WHITE_CONCRETE);
                    b.fill(x, 3, z, alongX ? i : 9, 3, alongX ? 9 : i, Blocks.WHITE_CONCRETE);
                    b.fill(alongX ? i : 6, 1, alongX ? 6 : i, alongX ? i : 6, 2, alongX ? 6 : i, Blocks.LIGHT_BLUE_STAINED_GLASS);
                    b.fill(alongX ? i : 9, 1, alongX ? 9 : i, alongX ? i : 9, 2, alongX ? 9 : i, Blocks.LIGHT_BLUE_STAINED_GLASS);
                }
                b.set(alongX ? 7 : 7, 3, 7, Blocks.SEA_LANTERN);
            }
            default -> {
                Deco.antenna(b, 7, 0, 7, 10);
                // motoneige
                b.fill(3, 0, 11, 6, 0, 11, Blocks.WHITE_CONCRETE);
                b.fill(4, 1, 11, 5, 1, 11, Blocks.RED_CONCRETE);
                b.set(3, 1, 11, Blocks.GRAY_STAINED_GLASS_PANE);
                b.fill(3, 0, 12, 6, 0, 12, Blocks.BLACK_CARPET);
                Deco.explosiveBarrel(b, 11, 0, 3);
                b.fill(10, 0, 9, 12, 1, 10, Blocks.SNOW_BLOCK);
                Deco.camoNet(b, 9, 2, 13, 5, 3, true);
            }
        }
    }

    @SuppressWarnings("unused")
    private static BlockState none() {
        return ModBlocks.RUBIS_BLOCK.get().defaultBlockState();
    }
}

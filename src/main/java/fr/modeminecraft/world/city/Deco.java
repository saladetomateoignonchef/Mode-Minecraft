package fr.modeminecraft.world.city;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Décorations urbaines construites en blocs (rue, intérieurs, défenses). Coordonnées locales au bâtiment ;
 * (x, z) = pied de la décoration, y = 0 au niveau du sol.
 */
final class Deco {
    // ------------------------------------------------------------------ rue (métropole)
    /** Lampadaire : mât, crosse et lanterne. */
    static void lampPost(B b, int x, int y, int z, Direction arm) {
        b.fill(x, y, z, x, y + 4, z, Blocks.ANDESITE_WALL);
        b.set(x + arm.getStepX(), y + 5, z + arm.getStepZ(), Blocks.POLISHED_ANDESITE_SLAB);
        b.set(x, y + 5, z, Blocks.POLISHED_ANDESITE);
        b.set(x + arm.getStepX(), y + 4, z + arm.getStepZ(), Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
    }

    /** Feu tricolore (rouge, orange, vert) sur son poteau. */
    static void trafficLight(B b, int x, int y, int z) {
        b.fill(x, y, z, x, y + 3, z, Blocks.IRON_BARS);
        // deux feux dos à dos sur le poteau, un par axe du carrefour (décalés : l'un est vert quand l'autre est rouge)
        b.set(x, y + 4, z, B.facing(fr.modeminecraft.registry.ModBlocks.TRAFFIC_LIGHT.get(), Direction.NORTH));
        b.set(x, y + 5, z, B.facing(fr.modeminecraft.registry.ModBlocks.TRAFFIC_LIGHT.get(), Direction.EAST));
        b.set(x, y + 6, z, Blocks.IRON_BARS);
    }

    /** Bouche d'incendie. */
    static void hydrant(B b, int x, int y, int z) {
        b.set(x, y, z, fr.modeminecraft.registry.ModBlocks.FIRE_HYDRANT.get());
    }

    static void bench(B b, int x, int y, int z, Direction facing) {
        b.set(x, y, z, B.stairs(Blocks.DARK_OAK_STAIRS, facing.getOpposite()));
        Direction side = facing.getClockWise();
        b.set(x + side.getStepX(), y, z + side.getStepZ(), B.stairs(Blocks.DARK_OAK_STAIRS, facing.getOpposite()));
    }

    static void trashBin(B b, int x, int y, int z) {
        b.set(x, y, z, Blocks.CAULDRON);
    }

    /** Benne à ordures fouillable (tonneau à butin + couvercle). */
    static void dumpster(B b, int x, int y, int z) {
        b.set(x, y, z, Blocks.GREEN_TERRACOTTA);
        b.chest(x + 1, y, z, Direction.NORTH, "city_dumpster", true);
        b.set(x, y + 1, z, B.facing(Blocks.IRON_TRAPDOOR, Direction.NORTH));
        b.set(x + 1, y + 1, z, B.facing(Blocks.IRON_TRAPDOOR, Direction.NORTH));
    }

    /** Abribus vitré avec banc. */
    static void busStop(B b, int x, int y, int z) {
        b.fill(x, y, z, x + 3, y + 2, z, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE);
        b.fill(x, y + 3, z, x + 3, y + 3, z + 1, Blocks.SMOOTH_STONE_SLAB);
        b.set(x, y, z + 1, Blocks.IRON_BARS);
        b.set(x + 3, y, z + 1, Blocks.IRON_BARS);
        b.set(x + 1, y, z + 1, B.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
        b.set(x + 2, y, z + 1, B.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
        b.set(x + 1, y + 2, z + 1, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
    }

    /** Kiosque à journaux. */
    static void kiosk(B b, int x, int y, int z) {
        b.fill(x, y, z, x + 1, y + 1, z + 1, Blocks.GREEN_TERRACOTTA);
        b.set(x, y + 1, z, Blocks.LECTERN);
        b.fill(x - 1, y + 2, z - 1, x + 2, y + 2, z + 2, Blocks.GREEN_CARPET);
        b.fill(x, y + 2, z, x + 1, y + 2, z + 1, Blocks.DARK_OAK_SLAB);
    }

    /** Distributeur automatique (avec un peu de butin). */
    static void vendingMachine(B b, int x, int y, int z, Direction facing) {
        b.set(x, y, z, Blocks.RED_CONCRETE);
        b.chest(x, y + 1, z, facing, "city_vending", true);
        b.set(x, y + 2, z, Blocks.RED_CONCRETE);
    }

    static void parkingMeter(B b, int x, int y, int z) {
        b.set(x, y, z, Blocks.IRON_BARS);
        b.set(x, y + 1, z, Blocks.POLISHED_DEEPSLATE_WALL);
    }

    /** Borne de recharge électrique. */
    static void chargingStation(B b, int x, int y, int z) {
        b.set(x, y, z, Blocks.WHITE_CONCRETE);
        b.set(x, y + 1, z, Blocks.LIME_STAINED_GLASS);
        b.set(x, y + 2, z, Blocks.WHITE_CONCRETE);
    }

    /** Panneau publicitaire. */
    static void billboard(B b, int x, int y, int z, Direction along) {
        b.fill(x, y, z, x, y + 3, z, Blocks.IRON_BARS);
        int dx = along.getStepX(), dz = along.getStepZ();
        Block[] ad = {Blocks.YELLOW_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.MAGENTA_CONCRETE};
        Block c = ad[b.nextInt(ad.length)];
        for (int i = -2; i <= 2; i++) {
            for (int j = 4; j <= 6; j++) {
                b.set(x + dx * i, y + j, z + dz * i, (i + j) % 3 == 0 ? Blocks.WHITE_CONCRETE.defaultBlockState() : c.defaultBlockState());
            }
        }
    }

    /** Climatiseur de toit. */
    static void airConditioner(B b, int x, int y, int z) {
        b.fill(x, y, z, x + 1, y, z + 1, Blocks.IRON_BLOCK);
        b.set(x, y + 1, z, B.facing(Blocks.IRON_TRAPDOOR, Direction.NORTH));
        b.set(x + 1, y + 1, z, Blocks.LIGHTNING_ROD);
    }

    /** Antenne / paratonnerre. */
    static void antenna(B b, int x, int y, int z, int h) {
        b.fill(x, y, z, x, y + h, z, Blocks.IRON_BARS);
        b.set(x, y + h + 1, z, Blocks.LIGHTNING_ROD);
        b.set(x, y + h, z, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
    }

    /** Caméra de surveillance (observateur) accrochée au mur. */
    static void camera(B b, int x, int y, int z, Direction facing) {
        b.set(x, y, z, B.facing(Blocks.OBSERVER, facing));
    }

    /** Échafaudage de chantier. */
    static void scaffolding(B b, int x0, int z0, int x1, int z1, int h) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (x == x0 || x == x1 || z == z0 || z == z1) {
                    for (int y = 0; y < h; y++) {
                        if ((x + z + y) % 3 != 0) {
                            b.set(x, y, z, Blocks.SCAFFOLDING);
                        }
                    }
                }
            }
        }
    }

    /** Extincteur (explose en nuage de fumée quand on le casse : voir CityEvents). */
    static void extinguisher(B b, int x, int y, int z) {
        b.set(x, y, z, fr.modeminecraft.registry.ModBlocks.FIRE_EXTINGUISHER.get());
    }

    // ------------------------------------------------------------------ défenses militaires
    /** Mur de sacs de sable (1 de haut, 2 au milieu). */
    static void sandbags(B b, int x0, int y, int z0, int x1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                b.set(x, y, z, Blocks.MUD_BRICKS);
                if ((x + z) % 2 == 0) {
                    b.set(x, y + 1, z, Blocks.MUD_BRICK_SLAB);
                }
            }
        }
    }

    /** Hérisson antichar (croix d'acier). */
    static void czechHedgehog(B b, int x, int y, int z) {
        b.set(x, y, z, Blocks.IRON_BARS);
        b.set(x, y + 1, z, Blocks.CHAIN);
        b.set(x - 1, y, z, Blocks.IRON_BARS);
        b.set(x + 1, y + 1, z, Blocks.IRON_BARS);
        b.set(x, y, z - 1, Blocks.IRON_BARS);
        b.set(x, y + 1, z + 1, Blocks.IRON_BARS);
    }

    /** Barbelés au sommet d'une clôture. */
    static void barbedWire(B b, int x0, int y, int z0, int x1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                b.set(x, y, z, (x + z) % 3 == 0 ? Blocks.COBWEB : Blocks.IRON_BARS);
            }
        }
    }

    /** Fût explosif (TNT dans un tonneau rouge). */
    static void explosiveBarrel(B b, int x, int y, int z) {
        b.set(x, y, z, Blocks.TNT);
        b.set(x, y + 1, z, Blocks.RED_CARPET);
    }

    /** Filet de camouflage tendu sur des piquets. */
    static void camoNet(B b, int x0, int z0, int x1, int z1, int h, boolean snow) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if ((x == x0 || x == x1) && (z == z0 || z == z1)) {
                    b.fill(x, 0, z, x, h - 1, z, Blocks.OAK_FENCE);
                }
                Block c = snow ? ((x + z) % 3 == 0 ? Blocks.LIGHT_GRAY_CARPET : Blocks.WHITE_CARPET)
                        : ((x * 7 + z * 3) % 4 == 0 ? Blocks.BROWN_CARPET : (x + z) % 2 == 0 ? Blocks.GREEN_CARPET : Blocks.MOSS_CARPET);
                b.set(x, h, z, c);
            }
        }
    }

    /** Projecteur militaire. */
    static void searchlight(B b, int x, int y, int z) {
        b.set(x, y, z, Blocks.IRON_BARS);
        b.set(x, y + 1, z, Blocks.SEA_LANTERN);
        b.set(x, y + 2, z, B.facing(Blocks.IRON_TRAPDOOR, Direction.NORTH));
    }

    /** Clôture grillagée (barreaux) de hauteur h, avec barbelés. */
    static void fence(B b, int x0, int z0, int x1, int z1, int h) {
        b.fill(x0, 0, z0, x1, h - 1, z1, Blocks.IRON_BARS);
        barbedWire(b, x0, h, z0, x1, z1);
    }

    /** Mirador : 4 poteaux, plate-forme à h, toit, échelle. */
    static void watchtower(B b, int x, int z, int h, Block post, Block floor) {
        for (int i = 0; i <= 3; i += 3) {
            for (int j = 0; j <= 3; j += 3) {
                b.fill(x + i, 0, z + j, x + i, h + 3, z + j, post);
            }
        }
        b.fill(x, h, z, x + 3, h, z + 3, floor);
        b.walls(x, h + 1, z, x + 3, h + 1, z + 3, Blocks.SPRUCE_FENCE);
        b.fill(x - 1, h + 4, z - 1, x + 4, h + 4, z + 4, Blocks.SPRUCE_SLAB);
        b.fill(x + 1, 0, z + 4, x + 1, h, z + 4, B.facing(Blocks.LADDER, Direction.SOUTH));
        b.set(x + 1, h + 1, z + 4, Blocks.AIR);
        b.set(x + 1, h + 3, z + 1, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
    }

    // ------------------------------------------------------------------ intérieurs
    static void desk(B b, int x, int y, int z, Direction chair) {
        b.set(x, y, z, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
        b.set(x, y + 1, z, b.chance(0.5f) ? Blocks.BLACK_STAINED_GLASS_PANE : Blocks.FLOWER_POT);  // écran / plante
        b.set(x + chair.getStepX(), y, z + chair.getStepZ(), B.stairs(Blocks.DARK_OAK_STAIRS, chair.getOpposite()));
    }

    static void bed(B b, int x, int y, int z, Direction dir, Block bed) {
        BlockState foot = bed.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, dir)
                .setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
        b.set(x, y, z, foot);
        b.set(x + dir.getStepX(), y, z + dir.getStepZ(), foot.setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
    }

    /** Cuisine : plan de travail, évier, four. */
    static void kitchen(B b, int x, int y, int z) {
        b.set(x, y, z, Blocks.SMOKER.defaultBlockState());
        b.set(x + 1, y, z, Blocks.CAULDRON);
        b.set(x + 2, y, z, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        b.set(x + 2, y + 1, z, Blocks.FLOWER_POT);
    }

    /** Porte (deux moitiés) ; `door` = n'importe quel bloc de porte. */
    static void door(B b, int x, int y, int z, Direction facing, Block door) {
        BlockState s = door.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        b.set(x, y, z, s);
        b.set(x, y + 1, z, s.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
    }

    /** Râtelier d'armes et établi d'armurier (armurerie). */
    static void armoryRack(B b, int x, int y, int z, Direction facing) {
        b.set(x, y, z, B.facing(fr.modeminecraft.registry.ModBlocks.WEAPON_RACK.get(), facing));
    }
}

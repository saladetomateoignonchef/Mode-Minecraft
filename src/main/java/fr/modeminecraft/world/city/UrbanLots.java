package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.VehicleEntity;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Parcelles ouvertes et zones techniques de la métropole (cahier des charges « villes organiques » §2.2 et §4.1 : 15 à 20 %
 * de parcelles ouvertes pour aérer la ville) : parking, marché sous tentes colorées, petit parc de quartier avec aire de
 * jeux, château d'eau avec cuves et transformateurs.
 */
final class UrbanLots {
    /** Parking : places marquées, voitures civiles garées, bornes de recharge, bennes, lampadaires et haie. */
    static void parking(B b, double h) {
        b.ground(0, 0, 15, 15, Blocks.BLACK_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 8);
        for (int x = 1; x <= 14; x += 3) {                                                    // places (lignes blanches)
            b.fill(x, -1, 1, x, -1, 5, Blocks.WHITE_CONCRETE);
            b.fill(x, -1, 10, x, -1, 14, Blocks.WHITE_CONCRETE);
        }
        b.fill(1, -1, 1, 14, -1, 1, Blocks.WHITE_CONCRETE);
        b.fill(1, -1, 14, 14, -1, 14, Blocks.WHITE_CONCRETE);
        VehicleEntity.Type[] cars = {VehicleEntity.Type.SEDAN, VehicleEntity.Type.TAXI, VehicleEntity.Type.SEDAN, VehicleEntity.Type.JEEP};
        int n = 0;
        for (int x = 2; x <= 12; x += 3) {                                                    // voitures dans quelques places
            if (b.chance(0.6f)) {
                b.vehicle(x, 0, 3, cars[(n++ + (int) (h * 10)) % cars.length], 0);
            }
            if (b.chance(0.5f)) {
                b.vehicle(x, 0, 12, cars[(n++ + (int) (h * 7)) % cars.length], 180);
            }
        }
        Deco.lampPost(b, 0, 0, 7, Direction.EAST);
        Deco.lampPost(b, 15, 0, 8, Direction.WEST);
        Deco.chargingStation(b, 7, 0, 7);
        Deco.chargingStation(b, 8, 0, 8);
        Deco.dumpster(b, 14, 0, 7);
        b.set(0, 0, 4, ModBlocks.realism("mailbox").defaultBlockState());
        for (int x = 0; x <= 15; x += 3) {                                                     // haie de bordure au nord
            b.set(x, 0, 0, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
        }
        Deco.parkingMeter(b, 5, 0, 8);
        Deco.parkingMeter(b, 10, 0, 7);
    }

    /** Marché : étals sous tentes rayées, caisses, fûts, ardoise de menu, bancs, marchands. */
    static void market(B b, double h) {
        b.ground(0, 0, 15, 15, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE.defaultBlockState(), 10);
        b.fill(1, -1, 1, 14, -1, 14, Blocks.POLISHED_ANDESITE);
        Block[] tents = {Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.BLUE_WOOL, Blocks.GREEN_WOOL, Blocks.ORANGE_WOOL};
        int off = (int) (h * 5);
        for (int row = 0; row < 2; row++) {
            int z = row == 0 ? 3 : 11;
            Direction aisle = row == 0 ? Direction.SOUTH : Direction.NORTH;
            for (int i = 0; i < 4; i++) {
                int x = 2 + i * 3;
                b.set(x, 0, z, B.facing(ModBlocks.realism("market_stall"), aisle));
                b.set(x + 1, 0, z, B.facing(ModBlocks.realism("market_stall"), aisle));
                BlockState tent = ((i + row) % 2 == 0 ? tents[(i + off) % tents.length] : Blocks.WHITE_WOOL).defaultBlockState();
                b.fill(x, 3, z - 1, x + 1, 3, z + 1, tent);                                      // toit de toile
                b.set(x - 1 < 1 ? x : x - 1, 1, z - 1, Blocks.OAK_FENCE);
                b.set(x + 1, 2, z - 1, Blocks.OAK_FENCE);
            }
        }
        b.set(2, 0, 7, B.facing(ModBlocks.realism("crate_stack"), Direction.EAST));
        b.set(13, 0, 8, B.facing(ModBlocks.realism("oil_barrels"), Direction.WEST));
        b.set(7, 0, 7, B.facing(ModBlocks.realism("menu_board"), Direction.SOUTH));
        Deco.bench(b, 5, 0, 7, Direction.SOUTH);
        Deco.bench(b, 10, 0, 8, Direction.NORTH);
        Deco.lampPost(b, 8, 0, 7, Direction.EAST);
        CityGen.tree(b, 14, 7, 4);
        CityGen.shopkeeper(b, 3, 0, 5);
        CityGen.shopkeeper(b, 9, 0, 9);
        b.bot(6, 0, 8, Faction.CIVILIAN, BotRole.CIVILIAN, 8);
    }

    /** Petit parc de quartier : allées de gravier, aire de jeux (balançoire, toboggan), pique-nique, parterres, statue. */
    static void pocketPark(B b, double h) {
        b.ground(0, 0, 15, 15, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 12);
        b.fill(7, -1, 0, 8, -1, 15, Blocks.GRAVEL);                                            // allées en croix
        b.fill(0, -1, 7, 15, -1, 8, Blocks.GRAVEL);
        b.fill(6, -1, 6, 9, -1, 9, Blocks.STONE_BRICKS);
        b.set(7, 0, 7, ModBlocks.realism(h < 0.5 ? "statue" : "bird_bath").defaultBlockState());
        b.set(3, 0, 3, B.facing(ModBlocks.realism("swing"), Direction.SOUTH));
        b.set(5, 0, 3, B.facing(ModBlocks.realism("swing"), Direction.SOUTH));
        b.set(12, 0, 3, B.facing(ModBlocks.realism("slide"), Direction.WEST));
        b.set(4, 0, 12, B.facing(ModBlocks.realism("picnic_table"), Direction.NORTH));
        b.set(11, 0, 12, B.facing(ModBlocks.realism("flower_bed"), Direction.NORTH));
        b.set(13, 0, 10, B.facing(ModBlocks.realism("flower_bed"), Direction.NORTH));
        b.set(1, 0, 10, B.facing(ModBlocks.realism("flower_bed"), Direction.EAST));
        b.set(10, 0, 5, B.facing(ModBlocks.realism("garden_gnome"), Direction.SOUTH));
        CityGen.tree(b, 2, 14, 5);
        CityGen.tree(b, 14, 14, 4);
        CityGen.tree(b, 14, 1, 5);
        CityGen.tree(b, 1, 6, 4);
        Deco.bench(b, 6, 0, 10, Direction.NORTH);
        Deco.bench(b, 9, 0, 5, Direction.SOUTH);
        Deco.lampPost(b, 8, 0, 8, Direction.EAST);
        Deco.trashBin(b, 8, 0, 11);
        b.bot(5, 0, 6, Faction.CIVILIAN, BotRole.CIVILIAN, 10);
    }

    /** Château d'eau : tour à quatre pieds portant une cuve, tuyauterie, cuve basse, armoires électriques, grillage. */
    static void waterTower(B b, double h) {
        b.ground(0, 0, 15, 15, Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 22);
        BlockState steel = Blocks.IRON_BLOCK.defaultBlockState(), conc = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        for (int[] c : new int[][]{{5, 5}, {10, 5}, {5, 10}, {10, 10}}) {                       // pieds
            b.fill(c[0], 0, c[1], c[0], 13, c[1], conc);
        }
        for (int y = 4; y <= 12; y += 4) {                                                       // entretoises
            b.fill(5, y, 5, 10, y, 5, Blocks.IRON_BARS);
            b.fill(5, y, 10, 10, y, 10, Blocks.IRON_BARS);
            b.fill(5, y, 5, 5, y, 10, Blocks.IRON_BARS);
            b.fill(10, y, 5, 10, y, 10, Blocks.IRON_BARS);
        }
        b.fill(4, 14, 4, 11, 14, 11, conc);                                                      // plateau
        b.fill(4, 15, 4, 11, 20, 11, Blocks.LIGHT_BLUE_CONCRETE);                                // cuve
        b.fill(5, 16, 5, 10, 19, 10, Blocks.WATER);
        b.fill(5, 21, 5, 10, 21, 10, Blocks.LIGHT_GRAY_CONCRETE);
        b.fill(6, 22, 6, 9, 22, 9, Blocks.IRON_BLOCK);
        b.set(7, 23, 7, Blocks.LIGHTNING_ROD);
        b.fill(8, 0, 4, 8, 14, 4, B.facing(Blocks.LADDER, Direction.SOUTH));                     // échelle d'accès
        b.set(2, 0, 2, B.facing(ModBlocks.realism("storage_tank"), Direction.SOUTH));
        b.set(13, 0, 2, B.facing(ModBlocks.realism("storage_tank"), Direction.SOUTH));
        b.set(3, 0, 8, B.facing(ModBlocks.realism("pipe_run"), Direction.NORTH));
        b.set(12, 0, 8, B.facing(ModBlocks.realism("electrical_cabinet"), Direction.WEST));
        b.set(12, 0, 11, B.facing(ModBlocks.realism("electrical_cabinet"), Direction.WEST));
        b.set(2, 0, 13, B.facing(ModBlocks.realism("toolbox"), Direction.NORTH));
        b.walls(0, 0, 0, 15, 1, 15, Blocks.IRON_BARS.defaultBlockState());                       // grillage
        b.fill(7, 0, 0, 8, 1, 0, Blocks.AIR.defaultBlockState());                                 // portail
        Deco.lampPost(b, 3, 0, 4, Direction.EAST);
    }

    private UrbanLots() {}
}

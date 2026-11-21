package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Infrastructures aéronautiques (cahier des charges B-29 §1.2), en bordure des bases secrètes et des cités soviétiques :
 * <ul>
 * <li><b>piste</b> (parcelle de N x 2 chunks) : béton armé de 18 blocs de large, seuils de piste et ligne médiane
 * discontinue blancs, balises lumineuses sur les bords, manche à air (bannière qui flotte), tour de contrôle radar ;</li>
 * <li><b>méga-hangar</b> (parcelle de 3 x 3 chunks) : voûte d'acier de 36 x 32 x 14 dont la grande porte coulissante
 * (ouverte) donne sur la piste par un taxiway ; projecteurs, citernes de kérosène (ravitaillement automatique des
 * aéronefs garés à côté), caisses de pièces détachées et, selon la rareté, un B-29 Superfortress prêt au décollage.</li>
 * </ul>
 */
final class Airfields {
    /** Piste le long de +X (longueur `len`), centrée sur z = 16 de la parcelle (2 chunks de large). */
    static void airstrip(B b, int len, Faction f) {
        BlockState concrete = Blocks.GRAY_CONCRETE.defaultBlockState();
        b.ground(0, 0, len - 1, 31, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 24);
        int z0 = 7, z1 = 24;                                                     // 18 blocs de large
        b.fill(0, -1, z0, len - 1, -1, z1, Blocks.BLACK_CONCRETE);
        for (int x = 0; x < len; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean threshold = (x < 8 || x >= len - 8) && (z - z0) % 3 != 2 && z > z0 + 1 && z < z1 - 1;
                boolean centre = (z == 15 || z == 16) && x % 10 < 6 && x >= 12 && x < len - 12;
                boolean edge = z == z0 || z == z1;
                if (threshold || centre || edge && x % 2 == 0) {
                    b.set(x, -1, z, Blocks.WHITE_CONCRETE);
                }
            }
            if (x % 8 == 3) {                                                     // balises de bord de piste
                b.set(x, -1, z0 - 1, Blocks.SEA_LANTERN);
                b.set(x, -1, z1 + 1, Blocks.SEA_LANTERN);
            }
        }
        b.fill(0, -1, z0 - 2, len - 1, -1, z0 - 2, concrete);
        b.fill(0, -1, z1 + 2, len - 1, -1, z1 + 2, concrete);
        // feux d'approche rouges et verts aux extrémités
        for (int z = z0; z <= z1; z += 3) {
            b.set(0, -1, z, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
            b.set(0, -2, z, Blocks.REDSTONE_BLOCK);
            b.set(len - 1, -1, z, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
            b.set(len - 1, -2, z, Blocks.REDSTONE_BLOCK);
        }
        // manche à air : mât et bannière orange qui flotte au vent
        b.fill(len / 2, 0, 2, len / 2, 5, 2, Blocks.IRON_BARS);
        b.set(len / 2, 6, 2, Blocks.ORANGE_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, 4));
        // tour de contrôle radar (côté nord de la piste)
        int tx = len / 3, tz = 0;
        b.walls(tx, 0, tz, tx + 4, 9, tz + 4, Blocks.SMOOTH_STONE.defaultBlockState());
        b.fill(tx + 1, 0, tz + 1, tx + 3, 9, tz + 3, Blocks.AIR.defaultBlockState());
        b.fill(tx + 2, 0, tz + 1, tx + 2, 9, tz + 1, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH));
        b.set(tx + 2, 0, tz, Blocks.AIR);
        b.set(tx + 2, 1, tz, Blocks.AIR);
        b.fill(tx - 1, 10, tz - 1, tx + 5, 10, tz + 5, Blocks.SMOOTH_STONE);
        b.walls(tx - 1, 11, tz - 1, tx + 5, 13, tz + 5, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        b.fill(tx - 1, 14, tz - 1, tx + 5, 14, tz + 5, Blocks.SMOOTH_STONE);
        Deco.antenna(b, tx + 2, 15, tz + 2, 4);
        Deco.searchlight(b, tx + 4, 15, tz + 4);
        b.chest(tx + 1, 11, tz + 1, Direction.SOUTH, "military_crate", false);
        b.bot(tx + 2, 11, tz + 2, f, BotRole.SOLDIER, 6);
        b.pair(len - 10, 0, 28, f, 30);
    }

    /** Méga-hangar 36 x 32 x 14 dans une parcelle de 48 x 48, porte vers -X (côté piste), taxiway jusqu'au bord. */
    static void megaHangar(B b, Faction f, float b29Chance) {
        int x0 = 6, x1 = 41, z0 = 8, z1 = 39, top = 14;
        b.ground(0, 0, 47, 47, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 22);
        b.fill(x0, -1, z0, x1, -1, z1, Blocks.POLISHED_ANDESITE);
        // voûte : profil en arc (hauteur 14 au faîte, 6 sur les côtés)
        int span = z1 - z0;
        for (int z = z0; z <= z1; z++) {
            double u = (z - z0) / (double) span * 2 - 1;
            int h = (int) Math.round(6 + (top - 6) * Math.sqrt(Math.max(0, 1 - u * u)));
            for (int x = x0; x <= x1; x++) {
                boolean wallZ = z == z0 || z == z1, back = x == x1, front = x == x0;
                for (int y = 0; y <= h; y++) {
                    boolean shell = y == h || wallZ || back || front;
                    b.set(x, y, z, shell ? (y < 3 ? Blocks.GRAY_CONCRETE : (x + z) % 2 == 0 ? Blocks.IRON_BLOCK : Blocks.LIGHT_GRAY_CONCRETE)
                            : Blocks.AIR);
                }
                if (y0Roof(x, z) && h > 8) {
                    b.set(x, h, z, Blocks.GLASS);                                 // lanterneaux
                }
            }
        }
        // grande porte coulissante ouverte (28 de large, 10 de haut) + vantaux repoussés le long de la façade
        b.fill(x0, 0, z0 + 2, x0, 9, z1 - 2, Blocks.AIR.defaultBlockState());
        b.fill(x0 - 1, 10, z0 + 1, x0 - 1, 10, z1 - 1, Blocks.IRON_BARS);
        b.fill(x0 - 1, 0, z0 - 3, x0 - 1, 9, z0, Blocks.IRON_BLOCK);
        b.fill(x0 - 1, 0, z1, x0 - 1, 9, z1 + 3, Blocks.IRON_BLOCK);
        // taxiway vers la piste (bord ouest de la parcelle)
        b.fill(0, -1, z0 + 4, x0 - 1, -1, z1 - 4, Blocks.BLACK_CONCRETE);
        for (int x = 0; x < x0; x += 2) {
            b.set(x, -1, (z0 + z1) / 2, Blocks.YELLOW_CONCRETE);
        }
        // projecteurs plafonniers
        for (int x = x0 + 4; x < x1; x += 8) {
            for (int z = z0 + 6; z < z1; z += 8) {
                b.set(x, 11, z, Blocks.SEA_LANTERN);
                b.fill(x, 12, z, x, 13, z, Blocks.CHAIN);
            }
        }
        // citernes de kérosène (ravitaillement automatique), pièces détachées, établi
        for (int i = 0; i < 3; i++) {
            int z = z0 + 3 + i * 4;
            b.fill(x1 - 3, 0, z, x1 - 2, 2, z + 1, ModBlocks.KEROSENE_TANK.get());
        }
        for (int i = 0; i < 4; i++) {
            b.chest(x1 - 2, 0, z1 - 3 - i * 2, Direction.WEST, i == 0 ? "military_base" : "military_crate", i % 2 == 1);
        }
        b.set(x1 - 2, 0, z1 - 12, ModBlocks.ASSEMBLY_TABLE.get());
        // B-29 garé face à la porte (fuselage le long de X, ailes le long de Z), selon la rareté de la base
        if (b.chance(b29Chance)) {
            b.milVehicle((x0 + x1) / 2 + 2, 0, (z0 + z1) / 2, MilType.B29_SUPERFORTRESS, 90);
        }
        b.pair(x0 + 3, 0, z0 + 3, f, 20);
        b.bot(x1 - 4, 0, (z0 + z1) / 2, f, BotRole.SOLDIER, 10);
    }

    private static boolean y0Roof(int x, int z) {
        return x % 7 == 3 && z % 9 >= 3 && z % 9 <= 5;
    }

    private Airfields() {}
}

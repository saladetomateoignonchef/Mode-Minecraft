package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.military.MilType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;

/**
 * Hangars militaires blindés (cahier des charges 0.4) : tôle ondulée sur soubassement en béton armé (24 x 36 x 14),
 * grande porte coulissante métallique, pont roulant au plafond, fosse de maintenance, fûts de carburant, établis
 * de réparation. À l'intérieur, 2 ou 3 véhicules lourds prêts au combat (réservoir partiellement rempli, réparables à
 * la clé à molette) : BRDM-2, BTR-80, BMP-2, BM-21 Grad, T-80BVM, et plus rarement un T-14 Armata.
 */
final class Hangars {
    private static final MilType[] POOL = {MilType.BRDM_2, MilType.BTR_80, MilType.BMP_2, MilType.BM_21_GRAD, MilType.T_80BVM};

    /** Véhicules garés côte à côte, face à la porte (vers -Z dans le repère du hangar). */
    static void park(B b, int x0, int z, int spacing, int count, boolean elite, float doorYaw) {
        for (int i = 0; i < count; i++) {
            MilType t = POOL[b.nextInt(POOL.length)];
            if (elite && i == 0 && b.chance(0.35f)) {
                t = MilType.T_14_ARMATA;                                  // hangar principal haute sécurité
            }
            b.milVehicle(x0 + i * spacing, 0, z, t, doorYaw);
        }
    }

    /**
     * Grand hangar d'une cité soviétique, sur une parcelle de 2 x 3 chunks (32 x 48) : 24 x 36 x 14 centré, porte au
     * nord (z petits).
     */
    static void soviet(B b) {
        int x0 = 4, x1 = 27, z0 = 6, z1 = 41, top = 13;
        b.ground(0, 0, 31, 47, Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 20);
        b.fill(x0, -1, z0, x1, -1, z1, Blocks.POLISHED_ANDESITE);
        // soubassement en béton (4 de haut), puis tôle ondulée (nervures en fer)
        for (int y = 0; y <= top; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
                    if (!wall) {
                        b.set(x, y, z, Blocks.AIR);
                        continue;
                    }
                    int along = x == x0 || x == x1 ? z : x;
                    b.set(x, y, z, y < 4 ? Blocks.GRAY_CONCRETE : along % 2 == 0 ? Blocks.IRON_BLOCK : Blocks.LIGHT_GRAY_CONCRETE);
                }
            }
        }
        // toit en tôle avec lanterneaux
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean sky = (x - x0) % 6 == 3 && (z - z0) % 8 >= 2 && (z - z0) % 8 <= 5 && x > x0 + 1 && x < x1 - 1;
                b.set(x, top + 1, z, sky ? Blocks.GLASS : x % 2 == 0 ? Blocks.IRON_BLOCK : Blocks.LIGHT_GRAY_CONCRETE);
            }
        }
        // grande porte coulissante (face nord) : ouverte, vantaux repoussés contre la façade, rail au-dessus
        b.fill(x0 + 3, 0, z0, x1 - 3, 9, z0, Blocks.AIR.defaultBlockState());
        b.fill(x0 + 3, 10, z0 - 1, x1 - 3, 10, z0 - 1, Blocks.IRON_BARS);
        b.fill(x0 - 1, 0, z0 - 1, x0 + 2, 9, z0 - 1, Blocks.IRON_BLOCK);
        b.fill(x1 - 2, 0, z0 - 1, x1 + 1, 9, z0 - 1, Blocks.IRON_BLOCK);
        b.fill(x0 + 3, -1, z0 - 4, x1 - 3, -1, z0 - 1, Blocks.GRAY_CONCRETE);                   // aire de sortie
        // pont roulant : deux chemins de roulement, une poutre, un palan
        b.fill(x0 + 1, 12, z0 + 1, x0 + 1, 12, z1 - 1, Blocks.IRON_BLOCK);
        b.fill(x1 - 1, 12, z0 + 1, x1 - 1, 12, z1 - 1, Blocks.IRON_BLOCK);
        b.fill(x0 + 2, 12, z0 + 22, x1 - 2, 12, z0 + 22, Blocks.POLISHED_DEEPSLATE);
        b.fill(x0 + 12, 8, z0 + 22, x0 + 12, 11, z0 + 22, Blocks.CHAIN);
        b.set(x0 + 12, 7, z0 + 22, Blocks.IRON_BLOCK);
        // fosse de maintenance (2 de profondeur, échelle)
        b.fill(x0 + 10, -3, z0 + 25, x0 + 12, -2, z0 + 33, Blocks.STONE);
        b.fill(x0 + 11, -2, z0 + 26, x0 + 11, -1, z0 + 32, Blocks.AIR.defaultBlockState());
        b.set(x0 + 11, -2, z0 + 26, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        b.set(x0 + 11, -1, z0 + 26, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        // fûts de carburant, établis de réparation, outillage
        for (int z = z0 + 26; z <= z0 + 33; z += 2) {
            b.set(x1 - 1, 0, z, Blocks.CAULDRON);
            b.set(x1 - 2, 0, z, Blocks.CAULDRON);
        }
        b.set(x0 + 1, 0, z0 + 28, Blocks.ANVIL);
        b.set(x0 + 1, 0, z0 + 29, Blocks.SMITHING_TABLE);
        b.set(x0 + 1, 0, z0 + 30, Blocks.GRINDSTONE);
        b.set(x0 + 1, 0, z0 + 31, Blocks.CRAFTING_TABLE);
        b.set(x0 + 1, 0, z0 + 32, fr.modeminecraft.registry.ModBlocks.ARMAMENT_TABLE.get());
        b.chest(x0 + 1, 0, z0 + 33, Direction.EAST, "military_base", false);
        b.chest(x1 - 1, 0, z0 + 35 - 1, Direction.WEST, "military_base", false);
        // éclairage
        for (int x = x0 + 4; x <= x1 - 4; x += 5) {
            for (int z = z0 + 4; z <= z1 - 4; z += 8) {
                b.set(x, top, z, Blocks.SEA_LANTERN);
            }
        }
        park(b, x0 + 6, z0 + 13, 6, 2 + b.nextInt(2), true, 180);
        b.pair(x0 + 12, 0, z0 + 3, Faction.SOVIET, 12);
        b.bot(x0 + 4, 0, z0 + 20, Faction.SOVIET, BotRole.SOLDIER, 10);
    }

    /** Équipement du hangar en voûte de la base secrète (forêt) : pont roulant, fosse, fûts, établis. */
    static void baseFittings(B b) {
        b.fill(6, 10, 9, 26, 10, 9, Blocks.IRON_BLOCK);                                         // chemins de roulement
        b.fill(6, 10, 23, 26, 10, 23, Blocks.IRON_BLOCK);
        b.fill(18, 10, 10, 18, 10, 22, Blocks.POLISHED_DEEPSLATE);                              // poutre
        b.fill(18, 7, 16, 18, 9, 16, Blocks.CHAIN);
        b.set(18, 6, 16, Blocks.IRON_BLOCK);
        b.fill(21, -3, 20, 23, -2, 26, Blocks.STONE);                                            // fosse
        b.fill(22, -2, 21, 22, -1, 25, Blocks.AIR.defaultBlockState());
        b.set(22, -2, 21, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        b.set(22, -1, 21, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        for (int z = 25; z <= 27; z++) {
            b.set(26, 0, z, Blocks.CAULDRON);
        }
        b.set(24, 0, 27, Blocks.ANVIL);
        b.set(23, 0, 27, Blocks.SMITHING_TABLE);
    }

    private Hangars() {}
}

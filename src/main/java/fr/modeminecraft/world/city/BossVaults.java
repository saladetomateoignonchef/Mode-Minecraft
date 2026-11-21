package fr.modeminecraft.world.city;

import fr.modeminecraft.item.BossCardItem;
import fr.modeminecraft.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Chambre forte d'un boss de ville (cahier des charges « Cartes d'accès des boss ») : sous le bâtiment principal, une
 * trappe dissimulée près du boss descend par une échelle jusqu'à une porte blindée indestructible (carte nominative du
 * boss) ; derrière, une salle aux murs renforcés avec 4 à 6 coffres blindés et des râteliers d'armes, au butin
 * personnalisé selon la ville. L'un des coffres contient la tenue de la faction (au désert : une seule pièce au hasard
 * de la tenue du Boss du Désert).
 */
final class BossVaults {
    /**
     * @param sx, sy, sz trappe d'accès (sy = niveau des pieds, la trappe remplace le sol en sy - 1)
     * @param depth      profondeur du sol de la salle sous la trappe
     * @param north      la salle s'étend vers le nord (z décroissants) au lieu du sud
     */
    static void build(B b, int sx, int sy, int sz, int depth, boolean north, BossCardItem.VaultType t) {
        int f = sy - depth;                                       // sol de la salle (niveau des pieds)
        int dz = north ? -1 : 1;
        Block wall = ModBlocks.VAULT_WALL.get();
        // puits d'accès (échelle), entouré de murs renforcés
        for (int y = f; y < sy; y++) {
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    if (ox == 0 && oz == 0) {
                        continue;
                    }
                    if (oz == dz && y < f + 2) {
                        continue;                                 // passage vers la porte
                    }
                    if (y >= sy - 1 && (ox != 0 || oz != -dz)) {
                        continue;                                 // le sol du bâtiment reste intact autour de la trappe
                    }
                    b.set(sx + ox, y, sz + oz, wall);
                }
            }
            b.set(sx, y, sz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, north ? Direction.NORTH : Direction.SOUTH));
        }
        b.set(sx, f - 1, sz, wall);
        b.set(sx, sy - 1, sz, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP)
                .setValue(TrapDoorBlock.FACING, north ? Direction.SOUTH : Direction.NORTH));
        // salle : 9 x 7, 4 de haut, murs / sol / plafond renforcés
        int z0 = sz + dz * 2, z1 = sz + dz * 8;
        int lz = Math.min(z0, z1), hz = Math.max(z0, z1);
        for (int x = sx - 5; x <= sx + 5; x++) {
            for (int z = lz - 1; z <= hz + 1; z++) {
                for (int y = f - 1; y <= f + 4; y++) {
                    boolean shell = x == sx - 5 || x == sx + 5 || z == lz - 1 || z == hz + 1 || y == f - 1 || y == f + 4;
                    b.set(x, y, z, shell ? wall.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
        // porte blindée (2 blocs) entre le puits et la salle
        Direction facing = north ? Direction.NORTH : Direction.SOUTH;
        BlockState door = ModBlocks.BOSS_VAULT_DOORS.get(t).get().defaultBlockState().setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        b.set(sx, f, sz + dz, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        b.set(sx, f + 1, sz + dz, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        for (int ox = -1; ox <= 1; ox += 2) {
            b.set(sx + ox, f, sz + dz, wall);
            b.set(sx + ox, f + 1, sz + dz, wall);
        }
        b.set(sx, f + 2, sz + dz, wall);
        // décor et coffres
        Block floor = switch (t) {
            case SWAT_HQ -> Blocks.POLISHED_DEEPSLATE;
            case DESERT_PALACE -> Blocks.SMOOTH_SANDSTONE;
            case FOREST_BUNKER -> Blocks.GRAY_CONCRETE;
            case SOVIET_HQ -> Blocks.RED_TERRACOTTA;
            case CARTEL_VILLA -> Blocks.POLISHED_DIORITE;
            case BADLANDS_REFINERY -> Blocks.COPPER_BLOCK;
        };
        b.fill(sx - 4, f - 1, lz, sx + 4, f - 1, hz, floor);
        for (int x = sx - 3; x <= sx + 3; x += 3) {
            b.set(x, f + 3, (lz + hz) / 2, Blocks.SEA_LANTERN);
        }
        String table = "boss_vault_" + t.id;
        int far = north ? lz : hz;                               // mur du fond
        Direction toDoor = north ? Direction.SOUTH : Direction.NORTH;
        int chests = 4 + b.nextInt(3);
        int[] xs = {sx - 3, sx - 1, sx + 1, sx + 3, sx - 4, sx + 4};
        for (int i = 0; i < chests; i++) {
            int x = xs[i], z = i < 4 ? far : (lz + hz) / 2;
            b.chest(x, f, z, i < 4 ? toDoor : (x < sx ? Direction.EAST : Direction.WEST), table, false);
        }
        // coffre spécial au fond, au centre, sur un socle doré éclairé : uniquement la tenue de la faction
        // (au palais du désert : une seule pièce au hasard de la tenue du Boss du Désert)
        String outfit = switch (t) {
            case SWAT_HQ -> "police_uniform";
            case DESERT_PALACE -> "desert_boss";
            case FOREST_BUNKER -> "special_forces";
            case SOVIET_HQ -> "soviet_uniform";
            case CARTEL_VILLA -> "cartel_outfit";
            case BADLANDS_REFINERY -> "sheriff_outfit";
        };
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
        java.util.List<net.minecraft.world.item.ItemStack> suit = new java.util.ArrayList<>();
        if (t == BossCardItem.VaultType.DESERT_PALACE) {
            suit.add(new net.minecraft.world.item.ItemStack(fr.modeminecraft.registry.ModItems.OUTFITS.get(outfit + "_" + pieces[b.nextInt(4)]).get()));
        } else {
            for (String p : pieces) {
                suit.add(new net.minecraft.world.item.ItemStack(fr.modeminecraft.registry.ModItems.OUTFITS.get(outfit + "_" + p).get()));
            }
        }
        b.set(sx, f - 1, far, Blocks.GOLD_BLOCK);
        b.itemChest(sx, f, far, toDoor, suit, net.minecraft.network.chat.Component.translatable("container.modeminecraft.outfit_chest",
                net.minecraft.network.chat.Component.translatable("outfit.modeminecraft." + outfit)));
        b.set(sx, f + 2, far, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        b.set(sx, f + 3, far, Blocks.CHAIN);
        // râteliers d'armes sur les côtés, et le trésor du thème
        for (int z = lz + 1; z <= hz - 1; z += 2) {
            Deco.armoryRack(b, sx - 4, f + 1, z, Direction.EAST);
            Deco.armoryRack(b, sx + 4, f + 1, z, Direction.WEST);
        }
        Block treasure = switch (t) {
            case DESERT_PALACE, CARTEL_VILLA -> Blocks.GOLD_BLOCK;
            case BADLANDS_REFINERY -> Blocks.RAW_GOLD_BLOCK;
            case SOVIET_HQ -> Blocks.IRON_BLOCK;
            default -> null;
        };
        if (treasure != null) {
            b.set(sx - 1, f, (lz + hz) / 2, treasure);
            b.set(sx + 1, f, (lz + hz) / 2, treasure);
            b.set(sx, f, (lz + hz) / 2, t == BossCardItem.VaultType.CARTEL_VILLA ? Blocks.EMERALD_BLOCK : treasure);
        }
    }

    private BossVaults() {}
}

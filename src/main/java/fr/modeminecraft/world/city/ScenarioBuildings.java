package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.registry.ModBlocks;
import fr.modeminecraft.scenario.ScenarioSites;
import fr.modeminecraft.scenario.Side;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Bâtiments du scénario « Course à l'arme nucléaire » dans les bases américaines et soviétiques. Dortoir des recrues
 * (22 x 14 x 6) : US en béton gris-bleu propre éclairé aux néons ; URSS en béton brut, tuyauterie apparente, affiches
 * de propagande. Lits superposés le long des murs, casier au pied de chaque lit, table centrale, râtelier d'armes vide,
 * radiateurs / poêle, horloge murale, drapeau au mur du fond, recrues au repos et sentinelles devant la porte.
 */
public final class ScenarioBuildings {
    private ScenarioBuildings() {}

    /** Bâtiment : x 5 à 26, z 1 à 14 (parcelle 2 x 1 chunks), porte à l'ouest, sol en y = -1, toit en y = 5. */
    static void dormitory(B b, Side side) {
        boolean us = side == Side.US;
        Faction f = side.ally;
        Block wall = us ? Blocks.CYAN_TERRACOTTA : Blocks.GRAY_CONCRETE, inner = us ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE;
        Block floor = us ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.SPRUCE_PLANKS, roof = us ? Blocks.GRAY_CONCRETE : Blocks.GRAY_CONCRETE;
        int x0 = 5, x1 = 26, z0 = 1, z1 = 14;
        b.fill(x0, -1, z0, x1, -1, z1, floor);
        b.fill(x0, 0, z0, x1, 4, z1, wall);
        b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 4, z1 - 1, Blocks.AIR);
        b.fill(x0, 5, z0, x1, 5, z1, roof);
        b.fill(x0 + 1, -1, z0 + 1, x1 - 1, -1, z1 - 1, floor);
        if (!us) {
            b.fill(x0, 0, z0, x1, 0, z0, Blocks.ANDESITE);                                   // soubassement en béton brut
            b.fill(x0, 0, z1, x1, 0, z1, Blocks.ANDESITE);
        }
        // fenêtres hautes entre les lits
        for (int x = 8; x <= 24; x += 3) {
            b.fill(x + 0, 2, z0, x + 1, 3, z0, Blocks.GLASS);
            b.fill(x + 0, 2, z1, x + 1, 3, z1, Blocks.GLASS);
        }
        // porte double (ouest), auvent, éclairage
        Deco.door(b, x0, 0, 7, Direction.WEST, Blocks.IRON_DOOR);
        Deco.door(b, x0, 0, 8, Direction.WEST, Blocks.IRON_DOOR);
        b.set(x0 - 1, 1, 6, B.facing(Blocks.STONE_BUTTON, Direction.WEST));
        b.set(x0 + 1, 1, 6, B.facing(Blocks.STONE_BUTTON, Direction.EAST));
        b.fill(x0 - 2, 3, 6, x0 - 1, 3, 9, B.slab(Blocks.SMOOTH_STONE_SLAB, true));
        b.set(x0 - 1, 2, 6, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        b.fill(x0 - 3, -1, 6, x0 - 1, -1, 9, Blocks.GRAY_CONCRETE);
        // lits superposés + casier au pied de chaque lit (6 de chaque côté)
        Block bunk = us ? ModBlocks.MILITARY_BUNK_BED_US.get() : ModBlocks.MILITARY_BUNK_BED_SOVIET.get();
        for (int x = 7; x <= 22; x += 3) {
            b.set(x, 0, 2, B.facing(bunk, Direction.NORTH));
            b.set(x, 0, 13, B.facing(bunk, Direction.SOUTH));
            b.set(x, 0, 3, B.facing(ModBlocks.BARRACKS_LOCKER.get(), Direction.SOUTH));
            b.set(x, 0, 12, B.facing(ModBlocks.BARRACKS_LOCKER.get(), Direction.NORTH));
        }
        // table centrale et bancs
        for (int x = 13; x <= 18; x++) {
            b.set(x, 0, 7, B.slab(Blocks.SPRUCE_SLAB, true));
            b.set(x, 0, 8, B.slab(Blocks.SPRUCE_SLAB, true));
            b.set(x, 0, 6, B.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
            b.set(x, 0, 9, B.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
        }
        b.set(14, 1, 7, Blocks.LANTERN);
        b.set(17, 1, 8, us ? Blocks.FLOWER_POT : Blocks.CANDLE);
        // mur du fond : râtelier d'armes vide, drapeau, horloge
        for (int z = 4; z <= 5; z++) {
            b.set(x1 - 1, 0, z, B.facing(ModBlocks.WEAPON_RACK.get(), Direction.WEST));
        }
        for (int z = 10; z <= 11; z++) {
            b.set(x1 - 1, 0, z, B.facing(ModBlocks.WEAPON_RACK.get(), Direction.WEST));
        }
        flag(b, x1 - 1, 3, 7, Direction.WEST, side);
        flag(b, x1 - 1, 3, 8, Direction.WEST, side);
        b.set(x1 - 1, 3, 5, B.facing(ModBlocks.WALL_CLOCK.get(), Direction.WEST));
        // chauffage et éclairage : US radiateurs + néons ; URSS poêle, tuyaux apparents, ampoules nues, affiches
        if (us) {
            b.fill(x0 + 1, 4, z0 + 1, x1 - 1, 4, z1 - 1, inner);                                   // faux plafond
            for (int x = 8; x <= 24; x += 6) {
                b.fill(x, 4, 6, x + 2, 4, 6, Blocks.SEA_LANTERN);
                b.fill(x, 4, 9, x + 2, 4, 9, Blocks.SEA_LANTERN);
            }
            b.set(x0 + 1, 1, 4, B.facing(Blocks.IRON_TRAPDOOR, Direction.EAST));              // radiateurs
            b.set(x0 + 1, 1, 11, B.facing(Blocks.IRON_TRAPDOOR, Direction.EAST));
            b.set(x1 - 1, 0, 13, Blocks.CAULDRON);                                              // lavabo
            b.fill(x1 - 6, 6, 3, x1 - 4, 6, 4, Blocks.LIGHT_GRAY_CONCRETE);                     // climatisation sur le toit
            b.set(x1 - 5, 7, 3, Blocks.IRON_BARS);
        } else {
            b.set(x1 - 1, 0, 9, Blocks.BLAST_FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                    .setValue(BlockStateProperties.LIT, true));                                  // poêle
            b.fill(x1 - 1, 1, 9, x1 - 1, 4, 9, Blocks.IRON_BARS);                                 // conduit
            b.fill(x1 - 1, 5, 9, x1 - 1, 7, 9, Blocks.POLISHED_ANDESITE);
            b.fill(x0 + 1, 4, 4, x1 - 2, 4, 4, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                    .setValue(BlockStateProperties.AXIS, Direction.Axis.X));                        // tuyauterie apparente
            b.fill(x0 + 1, 4, 11, x1 - 2, 4, 11, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                    .setValue(BlockStateProperties.AXIS, Direction.Axis.X));
            for (int x = 9; x <= 24; x += 5) {
                b.set(x, 4, 7, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
            }
            // affiches de propagande (rouges, étoile jaune) entre les fenêtres
            for (int x = 10; x <= 24; x += 6) {
                for (int z : new int[]{z0 + 1, z1 - 1}) {
                    if (z == z0 + 1 && (x - 7) % 3 == 0 || z == z1 - 1 && (x - 7) % 3 == 0) {
                        continue;                                                              // pas derrière un lit
                    }
                    b.fill(x, 2, z, x + 1, 3, z, Blocks.RED_WOOL);
                    b.set(x, 3, z, Blocks.YELLOW_WOOL);                                        // étoile
                }
            }
        }
        // recrues au repos, sentinelles devant la porte
        b.bot(15, 0, 5, f, BotRole.SOLDIER, 5);
        b.bot(19, 0, 10, f, BotRole.SOLDIER, 5);
        b.bot(11, 0, 9, f, BotRole.SOLDIER, 5);
        b.bot(x0 - 2, 0, 5, f, BotRole.SOLDIER, 3);
        b.bot(x0 - 2, 0, 10, f, BotRole.OFFICER, 3);
    }

    /** Drapeau national (bannière murale) : bannière étoilée / drapeau rouge frappé de l'étoile. */
    static void flag(B b, int x, int y, int z, Direction facing, Side side) {
        if (!b.inside(x, y, z)) {
            return;
        }
        boolean us = side == Side.US;
        BlockState s = (us ? Blocks.WHITE_WALL_BANNER : Blocks.RED_WALL_BANNER).defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        b.set(x, y, z, s);
        var lookup = b.level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        BannerPatternLayers.Builder layers = new BannerPatternLayers.Builder();
        if (us) {
            layers.add(lookup.getOrThrow(BannerPatterns.STRIPE_SMALL), DyeColor.RED);
            layers.add(lookup.getOrThrow(BannerPatterns.SQUARE_TOP_LEFT), DyeColor.BLUE);
        } else {
            layers.add(lookup.getOrThrow(BannerPatterns.SQUARE_TOP_LEFT), DyeColor.YELLOW);
            layers.add(lookup.getOrThrow(BannerPatterns.HALF_HORIZONTAL_MIRROR), DyeColor.RED);
            layers.add(lookup.getOrThrow(BannerPatterns.STRIPE_BOTTOM), DyeColor.RED);
        }
        if (b.level.getBlockEntity(b.pos(x, y, z)) instanceof net.minecraft.world.level.block.entity.BannerBlockEntity be) {
            ItemStack stack = new ItemStack(us ? net.minecraft.world.item.Items.WHITE_BANNER : net.minecraft.world.item.Items.RED_BANNER);
            stack.set(net.minecraft.core.component.DataComponents.BANNER_PATTERNS, layers.build());
            be.applyComponentsFromItemStack(stack);
            be.setChanged();
        }
    }

    /** Les coordonnées du lit n° 1 et de son casier sont fixées par {@link ScenarioSites}. */
    static int[] dormBed() {
        return ScenarioSites.DORM_BED;
    }
}

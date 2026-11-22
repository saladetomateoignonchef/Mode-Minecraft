package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.registry.ModBlocks;
import fr.modeminecraft.scenario.ScenarioSites;
import fr.modeminecraft.scenario.Grade;
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
        flag(b, x, y, z, facing, side, true);
    }

    /** Élément de drapeau : avec canton (carré étoilé / étoile jaune) ou seulement les bandes (bas des grands drapeaux). */
    static void flag(B b, int x, int y, int z, Direction facing, Side side, boolean canton) {
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
            if (canton) {
                layers.add(lookup.getOrThrow(BannerPatterns.SQUARE_TOP_LEFT), DyeColor.BLUE);
            }
        } else if (canton) {
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


    // =================================================================================================
    // Bâtiments de la base (une parcelle de 16 x 16, façade et entrée au sud, z = 15)
    // =================================================================================================
    /** Construit le bâtiment du scénario de cette parcelle (sol préparé : gravier US, béton URSS). */
    static void build(B b, fr.modeminecraft.scenario.ScenarioSites.Site site, Side side) {
        boolean us = side == Side.US;
        BlockState ground = (us ? Blocks.GRAVEL : Blocks.GRAY_CONCRETE).defaultBlockState();
        b.ground(0, 0, b.w - 1, b.d - 1, ground, Blocks.STONE.defaultBlockState(), 20);
        switch (site) {
            case DORMITORY -> dormitory(b, side);
            case RECRUITMENT -> recruitment(b, side);
            case ARMORY -> armory(b, side);
            case CANTEEN -> canteen(b, side);
            case INFIRMARY -> infirmary(b, side);
            case CORPORAL_BARRACKS -> corporals(b, side);
            case NCO_QUARTERS -> rooms(b, side, Grade.SERGEANT, 3, false);
            case OFFICER_PAVILION -> rooms(b, side, Grade.LIEUTENANT, 2, true);
            case OFFICERS_CLUB -> club(b, side);
            case WAR_ROOM -> warRoom(b, side);
            case COMMAND_RESIDENCE -> residence(b, side);
            case GENERAL_BUNKER -> generalBunker(b, side);
            case OFFICERS_PARKING -> parking(b, side);
            case SHOOTING_RANGE -> range(b, side);
            case PRISON -> PrisonBuilder.prison(b, side);
        }
    }

    // ------------------------------------------------------------------ aides
    private static Block wall(Side s) {
        return s == Side.US ? Blocks.CYAN_TERRACOTTA : Blocks.GRAY_CONCRETE;
    }

    private static Block floor(Side s) {
        return s == Side.US ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.SPRUCE_PLANKS;
    }

    /** Bâtiment : murs de y = 0 à h - 1, toit en y = h, sol en y = -1, intérieur vide. */
    static void box(B b, int x0, int z0, int x1, int z1, int h, Block wall, Block floor, Block roof) {
        b.fill(x0, -1, z0, x1, -1, z1, floor);
        b.fill(x0, 0, z0, x1, h - 1, z1, wall);
        b.fill(x0 + 1, 0, z0 + 1, x1 - 1, h - 1, z1 - 1, Blocks.AIR);
        b.fill(x0, h, z0, x1, h, z1, roof);
    }

    static void windows(B b, int x0, int x1, int z, int y, int step) {
        for (int x = x0; x <= x1; x += step) {
            b.set(x, y, z, Blocks.GLASS);
        }
    }

    /** Porte à contrôle d'accréditation (deux moitiés). */
    static void rankDoor(B b, int x, int z, Direction facing, Grade g) {
        BlockState s = ModBlocks.RANK_SECURITY_DOOR.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(fr.modeminecraft.scenario.RankDoorBlock.RANK, Math.max(1, g.ordinal()));
        b.set(x, 0, z, s);
        b.set(x, 1, z, s.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
    }

    /** Panneau mural (texte sur 4 lignes). */
    static void sign(B b, int x, int y, int z, Direction facing, String... lines) {
        if (!b.inside(x, y, z)) {
            return;
        }
        b.set(x, y, z, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        if (b.level.getBlockEntity(b.pos(x, y, z)) instanceof net.minecraft.world.level.block.entity.SignBlockEntity be) {
            // en génération, le bloc n'est pas encore dans un monde chargé : on passe par ses données (pas de mise à jour réseau)
            var reg = b.level.registryAccess();
            net.minecraft.nbt.ListTag msgs = new net.minecraft.nbt.ListTag();
            for (int i = 0; i < 4; i++) {
                String line = i < lines.length ? lines[i] : "";
                msgs.add(net.minecraft.nbt.StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(net.minecraft.network.chat.Component.literal(line), reg)));
            }
            net.minecraft.nbt.CompoundTag front = new net.minecraft.nbt.CompoundTag();
            front.put("messages", msgs);
            front.putString("color", "white");
            front.putBoolean("has_glowing_text", true);
            net.minecraft.nbt.CompoundTag tag = be.saveWithoutMetadata(reg);
            tag.put("front_text", front);
            tag.putBoolean("is_waxed", true);
            be.loadWithComponents(tag, reg);
        }
    }

    /** Cour clôturée (grilles de 2 blocs) avec un passage gardé au sud. */
    static void yard(B b, Side side) {
        Faction f = side.ally;
        for (int i = 0; i <= 15; i++) {
            for (int y = 0; y <= 1; y++) {
                b.set(i, y, 0, Blocks.IRON_BARS);
                b.set(0, y, i, Blocks.IRON_BARS);
                b.set(15, y, i, Blocks.IRON_BARS);
                if (i < 6 || i > 9) {
                    b.set(i, y, 15, Blocks.IRON_BARS);
                }
            }
        }
        b.set(6, 0, 15, Blocks.STONE_BRICK_WALL);
        b.set(9, 0, 15, Blocks.STONE_BRICK_WALL);
        b.set(6, 1, 15, Blocks.LANTERN);
        b.set(9, 1, 15, Blocks.LANTERN);
        b.bot(5, 0, 14, f, BotRole.SOLDIER, 1);                                               // sentinelle
    }

    private static String[] label(Side s, String us, String ru, String fr) {
        return s == Side.US ? new String[]{"", us, "", ""} : new String[]{"", ru, fr, ""};
    }

    // ------------------------------------------------------------------ centre de commandement et des missions (32 x 32)
    static void recruitment(B b, Side side) {
        CommandCenter.build(b, side);
    }

    // ------------------------------------------------------------------ armurerie fortifiée
    static void armory(B b, Side side) {
        Faction f = side.ally;
        box(b, 2, 2, 13, 13, 5, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);
        b.fill(2, 0, 2, 13, 0, 13, Blocks.CHISELED_STONE_BRICKS);
        b.fill(3, 0, 3, 12, 0, 12, Blocks.AIR);
        Deco.door(b, 7, 0, 13, Direction.SOUTH, Blocks.IRON_DOOR);
        b.set(6, 1, 14, B.facing(Blocks.STONE_BUTTON, Direction.SOUTH));
        b.set(8, 1, 12, B.facing(Blocks.STONE_BUTTON, Direction.NORTH));
        sign(b, 9, 3, 14, Direction.SOUTH, label(side, "ARMORY", "ОРУЖЕЙНАЯ", "Armurerie"));
        // comptoir grillagé
        b.fill(3, 0, 8, 12, 0, 8, Blocks.POLISHED_ANDESITE);
        b.fill(3, 1, 8, 12, 2, 8, Blocks.IRON_BARS);
        b.set(7, 1, 8, Blocks.AIR);
        b.set(7, 0, 8, B.slab(Blocks.SMOOTH_STONE_SLAB, true));
        b.bot(7, 0, 6, f, BotRole.ARMORER, 2);
        for (int x = 3; x <= 12; x += 2) {
            Deco.armoryRack(b, x, 1, 3, Direction.SOUTH);
        }
        b.set(3, 0, 5, ModBlocks.ARMAMENT_TABLE.get());
        b.set(12, 0, 5, ModBlocks.AVIATION_AMMO_CRATE.get());
        b.set(12, 1, 5, ModBlocks.AVIATION_AMMO_CRATE.get());
        b.set(11, 0, 4, Blocks.BARREL.defaultBlockState());
        b.fill(3, 4, 3, 12, 4, 12, Blocks.AIR);
        b.set(5, 4, 5, Blocks.SEA_LANTERN);
        b.set(10, 4, 10, Blocks.SEA_LANTERN);
        b.bot(4, 0, 11, f, BotRole.SOLDIER, 3);
    }

    // ------------------------------------------------------------------ cantine
    static void canteen(B b, Side side) {
        boolean us = side == Side.US;
        Faction f = side.ally;
        box(b, 1, 1, 14, 13, 5, wall(side), us ? Blocks.WHITE_TERRACOTTA : Blocks.SPRUCE_PLANKS, Blocks.GRAY_CONCRETE);
        windows(b, 3, 12, 13, 2, 3);
        windows(b, 3, 12, 1, 2, 3);
        Deco.door(b, 7, 0, 13, Direction.SOUTH, Blocks.SPRUCE_DOOR);
        sign(b, 8, 3, 14, Direction.SOUTH, label(side, "MESS HALL", "СТОЛОВАЯ", "Cantine"));
        // cuisine et comptoir
        Deco.kitchen(b, 2, 0, 2);
        b.set(6, 0, 2, Blocks.SMOKER.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        b.set(7, 0, 2, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        b.set(9, 0, 2, Blocks.CAULDRON);
        b.fill(2, 0, 4, 12, 0, 4, B.slab(Blocks.SMOOTH_STONE_SLAB, true));
        b.set(7, 0, 4, Blocks.AIR);
        b.bot(6, 0, 3, f, BotRole.COOK, 2);
        // tables et bancs
        for (int z = 7; z <= 11; z += 4) {
            for (int x = 3; x <= 11; x++) {
                b.set(x, 0, z, B.slab(Blocks.SPRUCE_SLAB, true));
                b.set(x, 0, z - 1, B.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
                b.set(x, 0, z + 1, B.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
            }
            b.set(5, 1, z, Blocks.CANDLE);
            b.set(9, 1, z, Blocks.CANDLE);
        }
        b.set(4, 4, 8, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        b.set(10, 4, 8, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        b.bot(5, 0, 8, f, BotRole.SOLDIER, 3);
        b.bot(10, 0, 10, f, BotRole.SOLDIER, 3);
    }

    // ------------------------------------------------------------------ infirmerie
    static void infirmary(B b, Side side) {
        Faction f = side.ally;
        box(b, 1, 1, 14, 13, 5, Blocks.WHITE_CONCRETE, Blocks.WHITE_TERRACOTTA, Blocks.LIGHT_GRAY_CONCRETE);
        windows(b, 3, 12, 13, 2, 3);
        Deco.door(b, 7, 0, 13, Direction.SOUTH, Blocks.IRON_DOOR);
        b.set(6, 1, 14, B.facing(Blocks.STONE_BUTTON, Direction.SOUTH));
        b.set(8, 1, 12, B.facing(Blocks.STONE_BUTTON, Direction.NORTH));
        // croix rouge sur la façade
        b.fill(10, 2, 14, 12, 2, 14, Blocks.RED_WOOL);
        b.fill(11, 1, 14, 11, 3, 14, Blocks.RED_WOOL);
        sign(b, 4, 3, 14, Direction.SOUTH, label(side, "INFIRMARY", "САНЧАСТЬ", "Infirmerie"));
        for (int x = 3; x <= 12; x += 3) {
            Deco.bed(b, x, 0, 3, Direction.NORTH, Blocks.WHITE_BED);
            b.set(x + 1, 0, 2, Blocks.WHITE_STAINED_GLASS_PANE);
        }
        b.set(2, 0, 9, Blocks.BREWING_STAND);
        b.set(2, 0, 10, Blocks.CAULDRON);
        b.fill(2, 0, 7, 2, 0, 8, B.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
        b.bot(4, 0, 9, f, BotRole.MEDIC, 2);
        b.set(7, 4, 6, Blocks.SEA_LANTERN);
        b.set(7, 4, 10, Blocks.SEA_LANTERN);
    }

    // ------------------------------------------------------------------ baraquement des caporaux (porte : Caporal)
    static void corporals(B b, Side side) {
        boolean us = side == Side.US;
        Block bunk = us ? ModBlocks.MILITARY_BUNK_BED_US.get() : ModBlocks.MILITARY_BUNK_BED_SOVIET.get();
        box(b, 1, 1, 14, 13, 5, wall(side), floor(side), Blocks.GRAY_CONCRETE);
        windows(b, 2, 13, 1, 2, 3);
        rankDoor(b, 7, 13, Direction.SOUTH, Grade.CORPORAL);
        sign(b, 9, 3, 14, Direction.SOUTH, label(side, "CORPORALS", "ЕФРЕЙТОРЫ", "Caporaux"));
        for (int x = 3; x <= 12; x += 3) {
            b.set(x, 0, 2, B.facing(bunk, Direction.NORTH));
            b.set(x, 0, 3, B.facing(ModBlocks.BARRACKS_LOCKER.get(), Direction.SOUTH));
            b.set(x, 0, 12, B.facing(bunk, Direction.SOUTH));
            b.set(x, 0, 11, B.facing(ModBlocks.BARRACKS_LOCKER.get(), Direction.NORTH));
        }
        b.fill(5, 0, 7, 10, 0, 7, B.slab(Blocks.SPRUCE_SLAB, true));
        b.set(7, 1, 7, Blocks.LANTERN);
        flag(b, 13, 2, 7, Direction.WEST, side);
        b.set(7, 4, 4, Blocks.SEA_LANTERN);
        b.set(7, 4, 10, Blocks.SEA_LANTERN);
        b.bot(9, 0, 8, side.ally, BotRole.SOLDIER, 3);
    }

    // ------------------------------------------------------------------ sous-officiers (3 chambres) / officiers (2 chambres, lit double)
    static void rooms(B b, Side side, Grade g, int n, boolean officers) {
        yard(b, side);
        Block w = officers ? Blocks.WHITE_CONCRETE : wall(side);
        box(b, 1, 1, 14, 13, 5, w, Blocks.OAK_PLANKS, Blocks.GRAY_CONCRETE);
        rankDoor(b, 7, 13, Direction.SOUTH, g);
        sign(b, 9, 3, 14, Direction.SOUTH, officers ? label(side, "OFFICERS", "ОФИЦЕРЫ", "Officiers") : label(side, "NCO QUARTERS", "СЕРЖАНТЫ", "Sous-officiers"));
        int width = officers ? 6 : 4;                                                         // chambres : x 2.. (largeur intérieure)
        for (int i = 0; i < n; i++) {
            int x0 = 2 + i * (width + 1), x1 = x0 + width - 1;
            if (x1 >= 14) {
                break;
            }
            if (x1 + 1 < 14) {
                b.fill(x1 + 1, 0, 2, x1 + 1, 4, 8, w);                                       // cloison
            }
            b.fill(x0, 0, 8, x1, 3, 8, w);
            Deco.door(b, x0 + 1, 0, 8, Direction.SOUTH, Blocks.OAK_DOOR);
            Deco.bed(b, x0 + 1, 0, 4, Direction.NORTH, officers ? Blocks.BLUE_BED : Blocks.GREEN_BED);   // tête en z = 3
            if (officers) {
                Deco.bed(b, x0 + 2, 0, 4, Direction.NORTH, Blocks.BLUE_BED);                   // lit double
            }
            b.set(x1, 0, 2, B.facing(ModBlocks.BARRACKS_LOCKER.get(), Direction.SOUTH));     // coffre-fort privé
            b.set(x0, 0, 6, B.slab(Blocks.SPRUCE_SLAB, true));                                // bureau personnel
            b.set(x0, 1, 6, Blocks.LANTERN);                                                   // lampe de chevet
            b.set(x1, 0, 6, Blocks.BARREL.defaultBlockState());                                // armoire / vestiaire
            b.set(x0 + 1, 3, 5, Blocks.SEA_LANTERN);
            windows(b, x0 + 1, x0 + 1, 1, 2, 1);
        }
        // couloir : salle d'eau, salle de briefing
        b.set(2, 0, 11, Blocks.CAULDRON);
        b.set(2, 1, 11, B.facing(Blocks.IRON_TRAPDOOR, Direction.EAST));
        b.fill(9, 0, 10, 12, 0, 11, B.slab(Blocks.DARK_OAK_SLAB, true));
        b.set(10, 1, 10, B.facing(Blocks.LECTERN, Direction.SOUTH));
        b.set(13, 2, 11, B.facing(ModBlocks.WALL_CLOCK.get(), Direction.WEST));
        b.set(7, 4, 11, Blocks.SEA_LANTERN);
    }

    // ------------------------------------------------------------------ mess / club des officiers (Sergent)
    static void club(B b, Side side) {
        yard(b, side);
        box(b, 1, 1, 14, 13, 5, Blocks.DARK_OAK_PLANKS, Blocks.RED_TERRACOTTA, Blocks.GRAY_CONCRETE);
        windows(b, 3, 12, 1, 2, 3);
        rankDoor(b, 7, 13, Direction.SOUTH, Grade.SERGEANT);
        sign(b, 9, 3, 14, Direction.SOUTH, label(side, "OFFICERS CLUB", "ОФИЦЕРСКИЙ", "Mess"));
        // comptoir de cantine (tarifs réduits), table de banquet, salon, radio d'époque, tableaux tactiques
        b.fill(2, 0, 3, 7, 0, 3, B.slab(Blocks.DARK_OAK_SLAB, true));
        b.bot(4, 0, 2, side.ally, BotRole.COOK, 1);
        for (int x = 4; x <= 11; x++) {
            b.set(x, 0, 7, B.slab(Blocks.DARK_OAK_SLAB, true));
            b.set(x, 0, 6, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
            b.set(x, 0, 8, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
        }
        b.set(5, 1, 7, Blocks.CANDLE);
        b.set(10, 1, 7, Blocks.CANDLE);
        b.fill(2, -1, 9, 4, -1, 12, Blocks.RED_WOOL);                                        // salon : tapis, fauteuils
        b.set(2, 0, 11, B.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST));
        b.set(2, 0, 10, B.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST));
        b.set(3, 0, 12, Blocks.JUKEBOX);
        b.fill(9, 1, 1, 12, 3, 1, Blocks.BLACK_CONCRETE);                                    // tableaux tactiques
        b.set(10, 2, 1, Blocks.WHITE_CONCRETE);
        b.set(11, 2, 1, Blocks.RED_CONCRETE);
        flag(b, 13, 2, 7, Direction.WEST, side);
        b.set(7, 4, 7, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        b.bot(10, 0, 10, side.ally, BotRole.OFFICER, 3);
    }

    // ------------------------------------------------------------------ salle de stratégie / état-major (Lieutenant)
    static void warRoom(B b, Side side) {
        yard(b, side);
        box(b, 1, 1, 14, 13, 6, Blocks.POLISHED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES);
        rankDoor(b, 7, 13, Direction.SOUTH, Grade.LIEUTENANT);
        sign(b, 9, 3, 14, Direction.SOUTH, label(side, "WAR ROOM", "ГЕНШТАБ", "État-major"));
        // grande table topographique (carte du monde)
        for (int x = 4; x <= 11; x++) {
            for (int z = 5; z <= 9; z++) {
                boolean land = (x * 5 + z * 7) % 6 > 1;
                b.set(x, 0, z, land ? ((x + z) % 4 == 0 ? Blocks.BROWN_CONCRETE : Blocks.LIME_CONCRETE) : Blocks.LIGHT_BLUE_CONCRETE);
            }
        }
        b.set(5, 1, 6, Blocks.RED_CANDLE);
        b.set(10, 1, 8, Blocks.BLUE_CANDLE);
        // terminaux de liaison satellite, terminal des plans
        for (int x = 3; x <= 12; x += 3) {
            b.set(x, 0, 2, B.slab(Blocks.SMOOTH_STONE_SLAB, true));
            b.set(x, 1, 2, B.facing(ModBlocks.SURVEILLANCE_MONITOR.get(), Direction.SOUTH));
        }
        b.set(7, 0, 2, B.facing(ModBlocks.PLANS_TERMINAL.get(), Direction.SOUTH));
        // infirmerie privée des officiers et poste de commandement nucléaire
        Deco.bed(b, 13, 0, 12, Direction.NORTH, Blocks.WHITE_BED);
        b.set(2, 0, 11, Blocks.RED_CONCRETE);
        b.set(2, 1, 11, B.facing(Blocks.STONE_BUTTON, Direction.EAST));
        b.set(2, 0, 12, B.facing(Blocks.LECTERN, Direction.EAST));
        // antenne et parabole sur le toit
        b.fill(12, 7, 3, 12, 10, 3, Blocks.IRON_BARS);
        b.set(12, 11, 3, Blocks.LIGHTNING_ROD);
        b.set(11, 7, 4, Blocks.DAYLIGHT_DETECTOR);
        for (int x = 4; x <= 11; x += 7) {
            b.set(x, 5, 7, Blocks.SEA_LANTERN);
        }
        b.bot(7, 0, 11, side.ally, BotRole.OFFICER, 3);
        b.bot(3, 0, 8, side.ally, BotRole.SOLDIER, 2);
    }

    // ------------------------------------------------------------------ résidence privée de commandement (Commandant)
    static void residence(B b, Side side) {
        yard(b, side);
        b.fill(1, -1, 1, 14, -1, 14, Blocks.GRASS_BLOCK);
        box(b, 2, 2, 13, 11, 5, Blocks.QUARTZ_BLOCK, Blocks.DARK_OAK_PLANKS, Blocks.DEEPSLATE_TILES);
        windows(b, 3, 12, 11, 2, 2);
        rankDoor(b, 7, 11, Direction.SOUTH, Grade.MAJOR);
        sign(b, 9, 3, 12, Direction.SOUTH, label(side, "COMMAND", "КОМАНДИР", "Commandement"));
        Deco.bed(b, 4, 0, 5, Direction.NORTH, Blocks.RED_BED);                                // lit double (tête z = 4)
        Deco.bed(b, 5, 0, 5, Direction.NORTH, Blocks.RED_BED);
        b.set(7, 0, 3, B.facing(ModBlocks.BARRACKS_LOCKER.get(), Direction.SOUTH));
        Deco.desk(b, 10, 0, 4, Direction.SOUTH);
        b.fill(12, 0, 3, 12, 2, 3, Blocks.BOOKSHELF);
        b.set(3, 0, 9, Blocks.POTTED_BAMBOO);
        b.fill(4, 0, 9, 6, 0, 9, B.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
        b.set(7, 4, 6, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        // terrasse et jardin
        b.fill(2, -1, 12, 13, -1, 13, Blocks.SMOOTH_STONE);
        for (int x = 2; x <= 13; x += 3) {
            b.set(x, 0, 14, Blocks.POPPY);
        }
        flag(b, 13, 3, 6, Direction.WEST, side);
    }

    // ------------------------------------------------------------------ bunker privé du haut-commandement (Général)
    static void generalBunker(B b, Side side) {
        yard(b, side);
        // casemate d'entrée
        box(b, 5, 9, 10, 13, 4, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS);
        rankDoor(b, 7, 13, Direction.SOUTH, Grade.GENERAL);
        sign(b, 9, 2, 14, Direction.SOUTH, label(side, "HIGH COMMAND", "ВЕРХОВНОЕ", "Haut-commandement"));
        // escalier vers la salle souterraine (y = -6)
        for (int i = 0; i < 6; i++) {
            b.set(7, -1 - i, 11 - i, B.stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.SOUTH));
            b.fill(7, -i, 11 - i, 8, 2 - i, 11 - i, Blocks.AIR);
            b.set(8, -1 - i, 11 - i, B.stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.SOUTH));
        }
        b.fill(1, -8, 1, 14, -2, 6, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.fill(2, -7, 2, 13, -3, 5, Blocks.AIR);
        b.fill(2, -7, 2, 13, -7, 5, Blocks.POLISHED_DEEPSLATE);
        b.fill(7, -6, 5, 8, -4, 6, Blocks.AIR);
        Deco.bed(b, 4, -6, 5, Direction.NORTH, Blocks.BLACK_BED);                             // tête en (4, 4)
        b.set(7, -6, 3, B.facing(ModBlocks.BARRACKS_LOCKER.get(), Direction.SOUTH));
        b.fill(9, -6, 3, 12, -6, 4, B.slab(Blocks.DARK_OAK_SLAB, true));                    // table des cartes
        b.set(10, -5, 3, Blocks.CARTOGRAPHY_TABLE);
        b.set(12, -5, 4, Blocks.NOTE_BLOCK);
        b.set(13, -6, 2, Blocks.BARREL.defaultBlockState());
        b.set(2, -6, 2, Blocks.BARREL.defaultBlockState());
        for (int x = 4; x <= 11; x += 7) {
            b.set(x, -3, 3, Blocks.SEA_LANTERN);
        }
        flag(b, 13, -4, 3, Direction.WEST, side);
        b.bot(5, 0, 14, side.ally, BotRole.OFFICER, 1);
    }

    // ------------------------------------------------------------------ parking réservé des officiers (32 x 16)
    static void parking(B b, Side side) {
        boolean us = side == Side.US;
        b.fill(1, -1, 1, 30, -1, 14, Blocks.BLACK_CONCRETE);
        for (int x = 2; x <= 29; x += 6) {
            b.fill(x, -1, 2, x, -1, 8, Blocks.WHITE_CONCRETE);
        }
        for (int i = 0; i <= 31; i++) {
            if (i < 14 || i > 17) {
                b.set(i, 0, 15, Blocks.IRON_BARS);
            }
            b.set(i, 0, 0, Blocks.IRON_BARS);
        }
        sign(b, 13, 1, 15, Direction.SOUTH, label(side, "OFFICERS", "ОФИЦЕРЫ", "Parking"));
        b.milVehicle(5, 0, 5, us ? fr.modeminecraft.entity.military.MilType.HUMVEE : fr.modeminecraft.entity.military.MilType.TIGR, 180);
        b.milVehicle(11, 0, 5, us ? fr.modeminecraft.entity.military.MilType.M_ATV : fr.modeminecraft.entity.military.MilType.UAZ_469, 180);
        if (us) {
            b.milVehicle(17, 0, 5, fr.modeminecraft.entity.military.MilType.AVENGER, 180);
        } else {
            b.strategic(22, 0, 7, fr.modeminecraft.entity.military.MilType.S_400, 0);       // défense antiaérienne du QG
        }
        b.bot(15, 0, 14, side.ally, BotRole.SOLDIER, 2);
    }

    // ------------------------------------------------------------------ stand de tir (32 x 16)
    static void range(B b, Side side) {
        b.fill(1, -1, 1, 30, -1, 14, Blocks.SAND);
        b.fill(1, -1, 2, 4, -1, 13, Blocks.SMOOTH_STONE);
        for (int z = 3; z <= 12; z += 3) {
            b.set(3, 0, z, B.slab(Blocks.SPRUCE_SLAB, true));                                // pupitres de tir
            b.set(3, 0, z + 1, Blocks.SANDSTONE_WALL);
            for (int x : new int[]{10, 18, 26}) {
                b.set(x, 0, z, Blocks.HAY_BLOCK);
                b.set(x, 1, z, B.facing(ModBlocks.SHOOTING_TARGET.get(), Direction.WEST));
            }
        }
        b.fill(29, 0, 1, 30, 3, 14, Blocks.SANDSTONE);                                         // butte de tir
        for (int i = 1; i <= 30; i++) {
            b.set(i, 0, 0, Blocks.SANDSTONE_WALL);
            b.set(i, 0, 15, Blocks.SANDSTONE_WALL);
        }
        b.set(1, 0, 15, Blocks.AIR);
        b.set(2, 0, 15, Blocks.AIR);
        sign(b, 3, 1, 15, Direction.SOUTH, label(side, "FIRING RANGE", "СТРЕЛЬБИЩЕ", "Stand de tir"));
        b.bot(2, 0, 8, side.ally, BotRole.OFFICER, 2);
        RangeChallenge.place(b, side);
    }
}

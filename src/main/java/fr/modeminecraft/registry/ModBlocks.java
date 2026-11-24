package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.block.WorkbenchBlock;
import fr.modeminecraft.menu.ArmamentTableMenu;
import fr.modeminecraft.menu.ModificationTableMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ModeMinecraft.MODID);

    public static final DeferredBlock<Block> RUBIS_ORE = BLOCKS.register("rubis_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)));

    public static final DeferredBlock<Block> DEEPSLATE_RUBIS_ORE = BLOCKS.register("deepslate_rubis_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE)));

    public static final DeferredBlock<Block> RUBIS_BLOCK = BLOCKS.register("rubis_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK)));

    /** Table d'armement : fabrique armes, munitions et accessoires. */
    public static final DeferredBlock<Block> ARMAMENT_TABLE = BLOCKS.register("armament_table",
            () -> new WorkbenchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE),
                    Component.translatable("container.modeminecraft.armament_table"), ArmamentTableMenu::new));

    /** Établi d'artillerie : obus de char, bandes de canon automatique, roquettes, missiles, carburant. */
    public static final DeferredBlock<Block> ARTILLERY_TABLE = BLOCKS.register("artillery_table",
            () -> new WorkbenchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ANVIL).sound(net.minecraft.world.level.block.SoundType.METAL),
                    Component.translatable("container.modeminecraft.artillery_table"), ArmamentTableMenu::artillery));

    /** Établi d'assemblage : on fabrique les véhicules à partir de pièces (moteur, chenilles, canon, blindage...). */
    public static final DeferredBlock<Block> ASSEMBLY_TABLE = BLOCKS.register("assembly_table",
            () -> new WorkbenchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ANVIL).sound(net.minecraft.world.level.block.SoundType.METAL),
                    Component.translatable("container.modeminecraft.assembly_table"), ArmamentTableMenu::assembly));

    /** Chambres fortes des boss (mise à jour 0.5) : portes indestructibles et murs renforcés. */
    public static final java.util.Map<fr.modeminecraft.item.BossCardItem.VaultType, DeferredBlock<Block>> BOSS_VAULT_DOORS =
            new java.util.EnumMap<>(fr.modeminecraft.item.BossCardItem.VaultType.class);

    static {
        for (var t : fr.modeminecraft.item.BossCardItem.VaultType.values()) {
            BOSS_VAULT_DOORS.put(t, BLOCKS.register("boss_vault_door_" + t.id, () -> new fr.modeminecraft.block.BossVaultDoorBlock(t,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(-1f, 3600000f).noLootTable()
                            .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK))));
        }
    }

    /** Minerais de plomb (fréquent, Y -32 à 64) et d'uranium (rare, profond, faiblement lumineux). */
    public static final DeferredBlock<Block> LEAD_ORE = BLOCKS.register("lead_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 1), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)));
    public static final DeferredBlock<Block> DEEPSLATE_LEAD_ORE = BLOCKS.register("deepslate_lead_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 1), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE)));
    public static final DeferredBlock<Block> URANIUM_ORE = BLOCKS.register("uranium_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).lightLevel(s -> 4)));
    public static final DeferredBlock<Block> DEEPSLATE_URANIUM_ORE = BLOCKS.register("deepslate_uranium_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).lightLevel(s -> 4)));
    /** Centrifugeuse à uranium : 16 lingots d'uranium -> 1 cœur d'uranium enrichi (alimentée par la redstone). */
    public static final DeferredBlock<Block> CENTRIFUGE = BLOCKS.register("uranium_centrifuge",
            () -> new fr.modeminecraft.nuclear.CentrifugeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    /** Citerne de kérosène des hangars à avions : ravitaille seule les aéronefs posés à moins de 8 blocs. */
    public static final DeferredBlock<Block> KEROSENE_TANK = BLOCKS.register("kerosene_tank",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    /** Caisse de munitions d'aviation : avec une citerne de kérosène, réarme en 10 s l'avion posé à moins de 10 blocs. */
    public static final DeferredBlock<Block> AVIATION_AMMO_CRATE = BLOCKS.register("aviation_ammo_crate",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3f, 6f)));

    public static final DeferredBlock<Block> VAULT_WALL = BLOCKS.register("vault_wall",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.REINFORCED_DEEPSLATE).strength(-1f, 3600000f).noLootTable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    /** Table de modification : accessoires et skins des armes. */
    public static final DeferredBlock<Block> MODIFICATION_TABLE = BLOCKS.register("modification_table",
            () -> new WorkbenchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE),
                    Component.translatable("container.modeminecraft.modification_table"), ModificationTableMenu::new));

    /** Minerai de titane : entre Y = -64 et Y = 16, aussi rare que l'or ; pioche en fer au minimum. */
    public static final DeferredBlock<Block> TITANIUM_ORE = BLOCKS.register("titanium_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2), BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)));
    public static final DeferredBlock<Block> DEEPSLATE_TITANIUM_ORE = BLOCKS.register("deepslate_titanium_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_GOLD_ORE)));

    /** Râtelier d'armes : expose 3 armes à feu contre un mur. */
    // Décors urbains fonctionnels
    public static final DeferredBlock<Block> TRAFFIC_LIGHT = BLOCKS.register("traffic_light",
            () -> new fr.modeminecraft.block.city.TrafficLightBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK).strength(3f).noOcclusion().lightLevel(s -> 7)));
    public static final DeferredBlock<Block> FIRE_HYDRANT = BLOCKS.register("fire_hydrant",
            () -> new fr.modeminecraft.block.city.BreakableDecoBlock(fr.modeminecraft.block.city.BreakableDecoBlock.Kind.HYDRANT,
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED)
                            .strength(1.5f, 2f).noOcclusion().sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final DeferredBlock<Block> FIRE_EXTINGUISHER = BLOCKS.register("fire_extinguisher",
            () -> new fr.modeminecraft.block.city.BreakableDecoBlock(fr.modeminecraft.block.city.BreakableDecoBlock.Kind.EXTINGUISHER,
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED)
                            .strength(0.5f).noOcclusion().sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final DeferredBlock<Block> METAL_DETECTOR = BLOCKS.register("metal_detector",
            () -> new fr.modeminecraft.block.city.MetalDetectorBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(3f).noOcclusion().noCollission()));

    /** Éclairage de ville (allumé la nuit, éteint par les pannes de secteur) et transformateur haute tension. */
    public static final DeferredBlock<Block> CITY_LAMP = BLOCKS.register("city_lamp",
            () -> new fr.modeminecraft.world.night.CityLightBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_LAMP)));
    public static final DeferredBlock<Block> CITY_NEON = BLOCKS.register("city_neon",
            () -> new fr.modeminecraft.world.night.CityLightBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN)));
    public static final DeferredBlock<Block> STREET_LANTERN = BLOCKS.register("street_lantern",
            () -> new fr.modeminecraft.world.night.StreetLanternBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)));
    public static final DeferredBlock<Block> HIGH_VOLTAGE_TRANSFORMER = BLOCKS.register("high_voltage_transformer",
            () -> new fr.modeminecraft.world.night.TransformerBlock(BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY).strength(4f, 1f).noOcclusion()
                    .sound(net.minecraft.world.level.block.SoundType.METAL)));

    /** Campements : toile de tente (bloc et escaliers) et sac de couchage. */
    public static final DeferredBlock<Block> TENT_CANVAS = BLOCKS.register("tent_canvas",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).mapColor(net.minecraft.world.level.material.MapColor.SAND)));
    public static final DeferredBlock<Block> TENT_CANVAS_STAIRS = BLOCKS.register("tent_canvas_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(TENT_CANVAS.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).mapColor(net.minecraft.world.level.material.MapColor.SAND)));
    public static final DeferredBlock<Block> SLEEPING_BAG = BLOCKS.register("sleeping_bag",
            () -> new fr.modeminecraft.block.camp.SleepingBagBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GREEN_WOOL).strength(0.4f).noOcclusion()));

    /** Commissariat : casiers, portes verrouillées, cellules de détention, poste de garde. */
    private static BlockBehaviour.Properties steel() {
        return BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(5f, 6f)
                .sound(net.minecraft.world.level.block.SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
    }
    public static final DeferredBlock<Block> POLICE_LOCKER = BLOCKS.register("police_locker",
            () -> new fr.modeminecraft.block.police.PoliceLockerBlock(steel()));
    public static final DeferredBlock<Block> LOCKED_WOODEN_DOOR = BLOCKS.register("locked_wooden_door",
            () -> new fr.modeminecraft.block.police.LockedDoorBlock(net.minecraft.world.level.block.state.properties.BlockSetType.DARK_OAK,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_DOOR).strength(6f, 4f)));
    public static final DeferredBlock<Block> LOCKED_METAL_DOOR = BLOCKS.register("locked_metal_door",
            () -> new fr.modeminecraft.block.police.LockedDoorBlock(net.minecraft.world.level.block.state.properties.BlockSetType.IRON,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(10f, 8f)));
    public static final DeferredBlock<Block> SLIDING_JAIL_DOOR = BLOCKS.register("sliding_jail_door",
            () -> new fr.modeminecraft.block.police.SlidingJailDoorBlock(BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(-1f, 1200f)
                    .sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    /** Scénario : lits superposés des dortoirs (US / URSS) et casier personnel. */
    public static final DeferredBlock<Block> MILITARY_BUNK_BED_US = BLOCKS.register("military_bunk_bed_us",
            () -> new fr.modeminecraft.scenario.MilitaryBunkBedBlock(steel()));
    public static final DeferredBlock<Block> MILITARY_BUNK_BED_SOVIET = BLOCKS.register("military_bunk_bed_soviet",
            () -> new fr.modeminecraft.scenario.MilitaryBunkBedBlock(steel()));
    public static final DeferredBlock<Block> RANK_SECURITY_DOOR = BLOCKS.register("rank_security_door",
            () -> new fr.modeminecraft.scenario.RankDoorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(-1f, 1200f)));
    public static final DeferredBlock<Block> PLANS_TERMINAL = BLOCKS.register("plans_terminal",
            () -> new fr.modeminecraft.scenario.PlansTerminalBlock(steel()));
    public static final DeferredBlock<Block> BARRACKS_LOCKER = BLOCKS.register("barracks_locker",
            () -> new fr.modeminecraft.scenario.BarracksLockerBlock(steel()));
    public static final DeferredBlock<Block> PRISON_BUNK_BED = BLOCKS.register("prison_bunk_bed",
            () -> new fr.modeminecraft.block.police.PrisonBunkBlock(steel()));
    public static final DeferredBlock<Block> PRISON_TOILET_SINK = BLOCKS.register("prison_toilet_sink",
            () -> new fr.modeminecraft.block.police.FacingDecoBlock(steel(), Block.box(2, 0, 4, 14, 14, 16)));
    public static final DeferredBlock<Block> PRISON_BENCH = BLOCKS.register("prison_bench",
            () -> new fr.modeminecraft.block.police.FacingDecoBlock(steel(), Block.box(0, 0, 3, 16, 8.5, 13)));
    public static final DeferredBlock<Block> CELL_CONTROL_PANEL = BLOCKS.register("cell_control_panel",
            () -> new fr.modeminecraft.block.police.CellControlPanelBlock(steel()));
    public static final DeferredBlock<Block> SURVEILLANCE_MONITOR = BLOCKS.register("surveillance_monitor",
            () -> new fr.modeminecraft.block.police.FacingDecoBlock(steel().lightLevel(s -> 6), Block.box(1, 0, 6, 15, 14, 11)));

    /** Haut-parleur d'ambiance (musique des cafés, piano du saloon, annonces). */
    public static final DeferredBlock<Block> AMBIENT_SPEAKER = BLOCKS.register("ambient_speaker",
            () -> new fr.modeminecraft.block.city.AmbientSpeakerBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK)
                    .strength(1.5f).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    /** Décors animés des villes. */
    private static BlockBehaviour.Properties deco(int light) {
        return BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(1.5f, 3f)
                .sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion().lightLevel(s -> light);
    }
    public static final DeferredBlock<Block> NEON_SIGN = BLOCKS.register("neon_sign",
            () -> new fr.modeminecraft.block.police.FacingDecoBlock(deco(10).noCollission(), Block.box(0, 2, 14, 16, 14, 16)));
    public static final DeferredBlock<Block> NEON_SIGN_CYAN = BLOCKS.register("neon_sign_cyan",
            () -> new fr.modeminecraft.block.police.FacingDecoBlock(deco(10).noCollission(), Block.box(0, 2, 14, 16, 14, 16)));
    public static final DeferredBlock<Block> GIANT_SCREEN = BLOCKS.register("giant_screen",
            () -> new fr.modeminecraft.block.city.GiantScreenBlock(deco(12)));
    public static final DeferredBlock<Block> STEAM_VENT = BLOCKS.register("steam_vent",
            () -> new fr.modeminecraft.block.city.AnimatedDecoBlock(deco(0), Block.box(0, 0, 0, 16, 1, 16), fr.modeminecraft.block.city.AnimatedDecoBlock.Fx.STEAM));
    public static final DeferredBlock<Block> FOUNTAIN_SPOUT = BLOCKS.register("fountain_spout",
            () -> new fr.modeminecraft.block.city.AnimatedDecoBlock(deco(0), Block.box(5, 0, 5, 11, 10, 11), fr.modeminecraft.block.city.AnimatedDecoBlock.Fx.FOUNTAIN));
    public static final DeferredBlock<Block> AC_UNIT = BLOCKS.register("ac_unit",
            () -> new fr.modeminecraft.block.police.FacingDecoBlock(deco(0), Block.box(1, 0, 1, 15, 12, 15)));
    public static final DeferredBlock<Block> WALL_CLOCK = BLOCKS.register("wall_clock",
            () -> new fr.modeminecraft.block.city.WallClockBlock(deco(0).noCollission()));
    public static final DeferredBlock<Block> TRAFFIC_CONE = BLOCKS.register("traffic_cone",
            () -> new fr.modeminecraft.block.police.FacingDecoBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_ORANGE)
                    .strength(0.5f).noOcclusion(), Block.box(4, 0, 4, 12, 12, 12)));
    /** Caisse enregistreuse (braquages). */
    public static final DeferredBlock<Block> CASH_REGISTER = BLOCKS.register("cash_register",
            () -> new fr.modeminecraft.block.city.CashRegisterBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY)
                    .strength(3f, 4f).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<Block> JAMMER_TOWER = BLOCKS.register("jammer_tower_block",
            () -> new fr.modeminecraft.entity.drone.JammerTowerBlock(BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY).strength(5f, 2f).noOcclusion()
                    .sound(net.minecraft.world.level.block.SoundType.METAL).lightLevel(s -> 4)));

    /** Portes blindées de salles fortes (cartes d'accès bleue / rouge / noire). */
    public static final DeferredBlock<Block> VAULT_DOOR_BLUE = BLOCKS.register("vault_door_blue",
            () -> new fr.modeminecraft.block.VaultDoorBlock(1, vaultProps()));
    public static final DeferredBlock<Block> VAULT_DOOR_RED = BLOCKS.register("vault_door_red",
            () -> new fr.modeminecraft.block.VaultDoorBlock(2, vaultProps()));
    public static final DeferredBlock<Block> VAULT_DOOR_BLACK = BLOCKS.register("vault_door_black",
            () -> new fr.modeminecraft.block.VaultDoorBlock(3, vaultProps()));

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties vaultProps() {
        return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL)
                .requiresCorrectToolForDrops().strength(50f, 1200f).noOcclusion()
                .sound(net.minecraft.world.level.block.SoundType.NETHERITE_BLOCK).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK);
    }

    public static final DeferredBlock<Block> WEAPON_RACK = BLOCKS.register("weapon_rack",
            () -> new fr.modeminecraft.block.WeaponRackBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).noOcclusion()));

    /** Râtelier à trophées mural (armes des Chefs de Faction). */
    public static final DeferredBlock<Block> TROPHY_WEAPON_RACK = BLOCKS.register("trophy_weapon_rack",
            () -> new fr.modeminecraft.block.TrophyRackBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).noOcclusion()));

    /** Cible de tir : score de 0 à 10, distance et total de la série. */
    public static final DeferredBlock<Block> SHOOTING_TARGET = BLOCKS.register("shooting_target",
            () -> new fr.modeminecraft.block.ShootingTargetBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TARGET).noOcclusion()));

    /**
     * Décors de réalisme (aéroport, hôpital, gare, métro) : tout vient de la table générée RealismDecoData (formes et lumière) ;
     * modèles, textures et états de bloc sont produits par tools/generate_realism_decor.py.
     */
    public static final java.util.Map<String, DeferredBlock<Block>> REALISM = new java.util.LinkedHashMap<>();
    static {
        for (fr.modeminecraft.block.city.RealismDecoData.Def d : fr.modeminecraft.block.city.RealismDecoData.ALL) {
            REALISM.put(d.id(), BLOCKS.register(d.id(), () -> {
                BlockBehaviour.Properties props = deco(d.light());
                if (d.passable()) {
                    props = props.noCollission();
                }
                return new fr.modeminecraft.block.police.FacingDecoBlock(props, Block.box(d.x0(), d.y0(), d.z0(), d.x1(), d.y1(), d.z1()));
            }));
        }
    }

    /** Un décor de réalisme par son identifiant (pour la génération des bâtiments). */
    public static Block realism(String id) {
        return REALISM.get(id).get();
    }

    private ModBlocks() {}
}

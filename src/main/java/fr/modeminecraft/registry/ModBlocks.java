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

    private ModBlocks() {}
}

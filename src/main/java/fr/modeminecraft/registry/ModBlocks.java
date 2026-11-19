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

    /** Râtelier d'armes : expose 3 armes à feu contre un mur. */
    public static final DeferredBlock<Block> WEAPON_RACK = BLOCKS.register("weapon_rack",
            () -> new fr.modeminecraft.block.WeaponRackBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).noOcclusion()));

    /** Cible de tir : score de 0 à 10, distance et total de la série. */
    public static final DeferredBlock<Block> SHOOTING_TARGET = BLOCKS.register("shooting_target",
            () -> new fr.modeminecraft.block.ShootingTargetBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TARGET).noOcclusion()));

    private ModBlocks() {}
}

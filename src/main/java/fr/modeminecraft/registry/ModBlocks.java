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

    private ModBlocks() {}
}

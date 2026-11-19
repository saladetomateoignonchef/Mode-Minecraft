package fr.modeminecraft;

import com.mojang.logging.LogUtils;
import fr.modeminecraft.registry.ModArmorMaterials;
import fr.modeminecraft.registry.ModBlocks;
import fr.modeminecraft.registry.ModCreativeTabs;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Point d'entrée du mod. NeoForge instancie cette classe au démarrage du jeu.
 */
@Mod(ModeMinecraft.MODID)
public class ModeMinecraft {
    public static final String MODID = "modeminecraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ModeMinecraft(IEventBus modEventBus, ModContainer modContainer) {
        // L'ordre n'a pas d'importance : NeoForge enregistre chaque registre au bon moment.
        ModBlocks.BLOCKS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        LOGGER.info("Mode Minecraft chargé !");
    }

    /** Raccourci pour créer un identifiant "modeminecraft:nom". */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

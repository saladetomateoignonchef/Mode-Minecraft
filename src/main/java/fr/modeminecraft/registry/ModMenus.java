package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.menu.ArmamentTableMenu;
import fr.modeminecraft.menu.ModificationTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ModeMinecraft.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ArmamentTableMenu>> ARMAMENT_TABLE =
            MENUS.register("armament_table", () -> new MenuType<>(ArmamentTableMenu::new, FeatureFlags.VANILLA_SET));
    /** Établi d'artillerie (même menu que la table d'armement, recettes lourdes). */
    public static final DeferredHolder<MenuType<?>, MenuType<ArmamentTableMenu>> ARTILLERY_TABLE =
            MENUS.register("artillery_table", () -> new MenuType<>(ArmamentTableMenu::artillery, FeatureFlags.VANILLA_SET));
    /** Établi d'assemblage des véhicules (pièces détachées, véhicules complets). */
    public static final DeferredHolder<MenuType<?>, MenuType<ArmamentTableMenu>> ASSEMBLY_TABLE =
            MENUS.register("assembly_table", () -> new MenuType<>(ArmamentTableMenu::assembly, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<ModificationTableMenu>> MODIFICATION_TABLE =
            MENUS.register("modification_table", () -> new MenuType<>(ModificationTableMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {}
}

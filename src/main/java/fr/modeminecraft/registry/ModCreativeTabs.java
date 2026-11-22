package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.GunItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/**
 * Les 8 onglets du mode créatif du mod (cahier des charges) : chaque objet est rangé automatiquement selon sa nature.
 * 1 armes à feu, 2 armes blanches et boucliers, 3 munitions / grenades / explosifs, 4 accessoires d'armes,
 * 5 équipements et armures 3D, 6 drones et véhicules, 7 soins / outils / objets spéciaux, 8 blocs / décors / générateurs.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModeMinecraft.MODID);

    public enum Tab { FIREARMS, MELEE, AMMO, ATTACHMENTS, EQUIPMENT, DRONES, TOOLS, BLOCKS }

    /** Munitions et charges sans classe dédiée. */
    private static final Set<String> AMMO_IDS = Set.of("pzf3_warhead", "stinger_missile", "javelin_missile", "plasma_ball", "grenade_40mm",
            "rpg_rocket", "mortar_shell", "ap_i_belt", "he_i_belt", "apfsds_shell", "he_shell", "heat_shell", "cannon_atgm_missile",
            "grad_rocket", "rocket_107", "mlrs_rocket", "atacms_missile", "s8_rocket", "hydra_rocket", "atgm_missile", "missile_48n6e3", "missile_9m96e2",
            "missile_77n6n", "missile_40n6m", "iskander_he", "iskander_cluster", "iskander_emp",
            "aam_ir_missile", "aam_radar_missile", "agm_missile", "arm_missile", "cruise_missile",
            "naval_shell", "ciws_belt", "ashm_missile", "hypersonic_missile", "naval_sam_missile", "torpedo", "asw_rocket");
    /** Drones, véhicules et ce qui va avec (brouilleur EMP, clé à molette, bidons). */
    private static final Set<String> DRONE_IDS = Set.of("emp_jammer_rifle", "wrench", "jerrycan", "vehicle_engine", "gas_turbine", "track_set",
            "wheel_set", "cannon_barrel", "autocannon_mount", "fire_control_module", "rotor_assembly", "launcher_module", "type63_launcher", "iron_pipe", "aircraft_engine", "pressurized_cockpit",
            "jet_engine", "avionics_suite", "radar_absorbent_panel", "ship_hull_section", "sonar_array", "naval_reactor");
    /** Objets présents dans deux onglets (le pied-de-biche est une arme ET un outil d'effraction). */
    private static final Set<String> ALSO_TOOLS = Set.of("crowbar");

    public static Tab tabOf(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        String cls = item.getClass().getSimpleName();
        if (item instanceof BlockItem || item instanceof SpawnEggItem || item instanceof fr.modeminecraft.item.MountedGunItem
                || item instanceof fr.modeminecraft.item.BotSpawnEggItem) {
            return Tab.BLOCKS;
        }
        if (DRONE_IDS.contains(path) || item instanceof fr.modeminecraft.entity.drone.DroneItem
                || item instanceof fr.modeminecraft.entity.drone.DroneControllerItem || item instanceof fr.modeminecraft.entity.drone.DroneModuleItem
                || item instanceof fr.modeminecraft.item.VehicleItem || item instanceof fr.modeminecraft.item.military.MilVehicleItem
                || item instanceof fr.modeminecraft.item.military.LaserDesignatorItem) {
            return Tab.DRONES;
        }
        if (item instanceof GunItem) {
            return Tab.FIREARMS;
        }
        if (item instanceof AttachmentItem || path.equals("spare_barrel")) {
            return Tab.ATTACHMENTS;
        }
        if (AMMO_IDS.contains(path) || item instanceof fr.modeminecraft.item.AmmoItem || item instanceof fr.modeminecraft.item.TurretAmmoItem
                || item instanceof fr.modeminecraft.item.explosive.GrenadeItem || item instanceof fr.modeminecraft.item.explosive.PlaceableExplosiveItem
                || item instanceof fr.modeminecraft.item.explosive.DetonatorItem || item instanceof fr.modeminecraft.nuclear.BombItem) {
            return Tab.AMMO;
        }
        if (item instanceof fr.modeminecraft.item.HelmetItem || item instanceof fr.modeminecraft.item.VestItem
                || item instanceof fr.modeminecraft.item.GearItem || item instanceof ArmorItem || item instanceof fr.modeminecraft.item.BallisticPlateItem
                || item instanceof fr.modeminecraft.item.tactical.BackpackItem || item instanceof fr.modeminecraft.item.explosive.GasMaskItem
                || item instanceof fr.modeminecraft.nuclear.HazmatKitItem) {
            return Tab.EQUIPMENT;
        }
        if (item instanceof SwordItem || item instanceof ShieldItem || item instanceof BowItem || item instanceof fr.modeminecraft.item.melee.MeleeWeaponItem
                || cls.endsWith("SwordItem") || cls.endsWith("HammerItem") || cls.endsWith("BladeItem") || cls.endsWith("StaffItem")
                || item instanceof fr.modeminecraft.item.DragonslayerItem) {
            return Tab.MELEE;
        }
        if (item instanceof TieredItem) {
            return Tab.TOOLS;                                         // outils en rubis
        }
        return Tab.TOOLS;                                             // soins, cartes, matériaux, manuel...
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(Tab tab, String name, String icon, String before) {
        return CREATIVE_TABS.register(name, () -> {
            CreativeModeTab.Builder b = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ModeMinecraft.MODID + "." + name))
                    .icon(() -> stack(icon))
                    .displayItems((params, output) -> ModItems.ITEMS.getEntries().forEach(h -> {
                        Item item = h.get();
                        if (tabOf(item) == tab || tab == Tab.TOOLS && ALSO_TOOLS.contains(h.getId().getPath())) {
                            output.accept(item);
                        }
                    }));
            if (before != null) {
                b.withTabsBefore(ModeMinecraft.id(before));
            }
            return b.build();
        });
    }

    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.get(ModeMinecraft.id(id));
        return new ItemStack(item);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FIREARMS = tab(Tab.FIREARMS, "firearms", "m4", null);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MELEE = tab(Tab.MELEE, "melee", "katana", "firearms");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AMMO = tab(Tab.AMMO, "ammo_explosives", "frag_grenade", "melee");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ATTACHMENTS = tab(Tab.ATTACHMENTS, "attachments", "acog_scope", "ammo_explosives");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EQUIPMENT = tab(Tab.EQUIPMENT, "equipment", "ops_core_fast", "attachments");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DRONES = tab(Tab.DRONES, "drones_vehicles", "drone_controller", "equipment");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS = tab(Tab.TOOLS, "medical_tools", "first_aid_kit", "drones_vehicles");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOCKS = tab(Tab.BLOCKS, "blocks", "traffic_light", "medical_tools");

    private ModCreativeTabs() {}
}

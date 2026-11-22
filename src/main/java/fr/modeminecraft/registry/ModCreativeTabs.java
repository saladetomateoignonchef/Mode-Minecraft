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
 * Les 7 onglets du mode créatif du mod : chaque objet est rangé automatiquement selon sa nature.
 * 1 armes et accessoires, 2 munitions et explosifs, 3 tenues et équipements, 4 véhicules terrestres,
 * 5 aviation et drones, 6 marine et sous-marins, 7 blocs, décoration et clés.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModeMinecraft.MODID);

    public enum Tab { WEAPONS, AMMO, GEAR, GROUND, AIR, SEA, BLOCKS }

    /** Munitions et charges sans classe dédiée. */
    private static final Set<String> AMMO_IDS = Set.of("pzf3_warhead", "stinger_missile", "javelin_missile", "plasma_ball", "grenade_40mm",
            "rpg_rocket", "mortar_shell", "ap_i_belt", "he_i_belt", "apfsds_shell", "he_shell", "heat_shell", "cannon_atgm_missile",
            "grad_rocket", "rocket_107", "mlrs_rocket", "atacms_missile", "s8_rocket", "hydra_rocket", "atgm_missile", "missile_48n6e3", "missile_9m96e2",
            "missile_77n6n", "missile_40n6m", "iskander_he", "iskander_cluster", "iskander_emp",
            "aam_ir_missile", "aam_radar_missile", "agm_missile", "arm_missile", "cruise_missile",
            "naval_shell", "ciws_belt", "ashm_missile", "hypersonic_missile", "naval_sam_missile", "torpedo", "asw_rocket");
    /** Pièces et matériaux de véhicules terrestres, brouilleur EMP, clé à molette, bidons. */
    private static final Set<String> GROUND_IDS = Set.of("emp_jammer_rifle", "wrench", "jerrycan", "vehicle_engine", "gas_turbine", "track_set",
            "wheel_set", "cannon_barrel", "autocannon_mount", "fire_control_module", "launcher_module", "type63_launcher", "iron_pipe");
    private static final Set<String> AIR_IDS = Set.of("rotor_assembly", "aircraft_engine", "pressurized_cockpit", "jet_engine", "avionics_suite",
            "radar_absorbent_panel");
    private static final Set<String> SEA_IDS = Set.of("ship_hull_section", "sonar_array", "naval_reactor", "torpedo", "asw_rocket", "ashm_missile", "naval_sam_missile",
            "naval_shell", "ciws_belt");

    public static Tab tabOf(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        String cls = item.getClass().getSimpleName();
        // ---- véhicules : par milieu d'évolution
        if (item instanceof fr.modeminecraft.item.military.MilVehicleItem mv) {
            return mv.type.naval() ? Tab.SEA : mv.type.heli() ? Tab.AIR : Tab.GROUND;
        }
        if (path.equals("mesa_train_ticket")) {
            return Tab.BLOCKS;                                         // billets et clés
        }
        if (path.equals("light_civil_plane")) {
            return Tab.AIR;
        }
        if (SEA_IDS.contains(path)) {
            return Tab.SEA;
        }
        if (AIR_IDS.contains(path) || item instanceof fr.modeminecraft.entity.drone.DroneItem
                || item instanceof fr.modeminecraft.entity.drone.DroneControllerItem || item instanceof fr.modeminecraft.entity.drone.DroneModuleItem) {
            return Tab.AIR;
        }
        if (GROUND_IDS.contains(path) || item instanceof fr.modeminecraft.item.VehicleItem
                || item instanceof fr.modeminecraft.item.military.RocketLauncherItem || item instanceof fr.modeminecraft.item.MountedGunItem) {
            return Tab.GROUND;
        }
        if (item instanceof fr.modeminecraft.item.AccessCardItem || item instanceof fr.modeminecraft.item.BossCardItem) {
            return Tab.BLOCKS;
        }
        if (item instanceof BlockItem || item instanceof SpawnEggItem || item instanceof fr.modeminecraft.item.BotSpawnEggItem) {
            return Tab.BLOCKS;
        }
        // ---- armes et accessoires
        if (item instanceof GunItem || item instanceof AttachmentItem || path.equals("spare_barrel")
                || item instanceof SwordItem || item instanceof ShieldItem || item instanceof BowItem || item instanceof fr.modeminecraft.item.melee.MeleeWeaponItem
                || cls.endsWith("SwordItem") || cls.endsWith("HammerItem") || cls.endsWith("BladeItem") || cls.endsWith("StaffItem")
                || item instanceof fr.modeminecraft.item.DragonslayerItem) {
            return Tab.WEAPONS;
        }
        // ---- munitions et explosifs
        if (AMMO_IDS.contains(path) || item instanceof fr.modeminecraft.item.AmmoItem || item instanceof fr.modeminecraft.item.TurretAmmoItem
                || item instanceof fr.modeminecraft.item.explosive.GrenadeItem || item instanceof fr.modeminecraft.item.explosive.PlaceableExplosiveItem
                || item instanceof fr.modeminecraft.item.explosive.DetonatorItem || item instanceof fr.modeminecraft.nuclear.BombItem) {
            return Tab.AMMO;
        }
        // ---- tenues, équipements, gadgets, soins, outils (tout le reste)
        return Tab.GEAR;
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(Tab tab, String name, String icon, String before) {
        return CREATIVE_TABS.register(name, () -> {
            CreativeModeTab.Builder b = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ModeMinecraft.MODID + "." + name))
                    .icon(() -> stack(icon))
                    .displayItems((params, output) -> ModItems.ITEMS.getEntries().forEach(h -> {
                        Item item = h.get();
                        if (tabOf(item) == tab) {
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

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WEAPONS = tab(Tab.WEAPONS, "weapons", "m4", null);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AMMO = tab(Tab.AMMO, "ammo_explosives", "frag_grenade", "weapons");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GEAR = tab(Tab.GEAR, "gear_outfits", "ops_core_fast", "ammo_explosives");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GROUND = tab(Tab.GROUND, "ground_vehicles", "jeep", "gear_outfits");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AIR = tab(Tab.AIR, "aviation_drones", "drone_controller", "ground_vehicles");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SEA = tab(Tab.SEA, "marine_submarines", "torpedo", "aviation_drones");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOCKS = tab(Tab.BLOCKS, "blocks_keys", "traffic_light", "marine_submarines");

    private ModCreativeTabs() {}
}

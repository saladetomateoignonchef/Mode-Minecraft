package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.EnderBladeItem;
import fr.modeminecraft.item.FireStaffItem;
import fr.modeminecraft.item.MagicSwordItem;
import fr.modeminecraft.item.RubisBowItem;
import fr.modeminecraft.item.ThunderHammerItem;
import fr.modeminecraft.item.WarHammerItem;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunItem.GunStats;
import fr.modeminecraft.item.gun.RocketLauncherItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tous les items du mod.
 *
 * Dégâts affichés en jeu = 1 + dégâts de l'arme + bonus du matériau (3.5 pour le rubis).
 * Vitesse affichée en jeu = 4 + vitesse de l'arme.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ModeMinecraft.MODID);

    // ---------- Matériaux ----------
    public static final DeferredItem<Item> RUBIS = ITEMS.registerSimpleItem("rubis");

    public static final DeferredItem<BlockItem> RUBIS_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.RUBIS_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_RUBIS_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_RUBIS_ORE);
    public static final DeferredItem<BlockItem> RUBIS_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.RUBIS_BLOCK);

    // ---------- Outils ----------
    public static final DeferredItem<PickaxeItem> RUBIS_PICKAXE = ITEMS.register("rubis_pickaxe",
            () -> new PickaxeItem(ModTiers.RUBIS, new Item.Properties()
                    .attributes(DiggerItem.createAttributes(ModTiers.RUBIS, 1.0f, -2.8f))));
    public static final DeferredItem<AxeItem> RUBIS_AXE = ITEMS.register("rubis_axe",
            () -> new AxeItem(ModTiers.RUBIS, new Item.Properties()
                    .attributes(DiggerItem.createAttributes(ModTiers.RUBIS, 5.5f, -3.0f))));
    public static final DeferredItem<ShovelItem> RUBIS_SHOVEL = ITEMS.register("rubis_shovel",
            () -> new ShovelItem(ModTiers.RUBIS, new Item.Properties()
                    .attributes(DiggerItem.createAttributes(ModTiers.RUBIS, 1.5f, -3.0f))));
    public static final DeferredItem<HoeItem> RUBIS_HOE = ITEMS.register("rubis_hoe",
            () -> new HoeItem(ModTiers.RUBIS, new Item.Properties()
                    .attributes(DiggerItem.createAttributes(ModTiers.RUBIS, -3.5f, 0.0f))));

    // ---------- Armes de mêlée ----------
    /** Épée classique : 7.5 dégâts, vitesse 1.6 */
    public static final DeferredItem<SwordItem> RUBIS_SWORD = ITEMS.register("rubis_sword",
            () -> new SwordItem(ModTiers.RUBIS, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModTiers.RUBIS, 3, -2.4f))));

    /** Dague : 5.5 dégâts mais très rapide (vitesse 2.5) */
    public static final DeferredItem<SwordItem> RUBIS_DAGGER = ITEMS.register("rubis_dagger",
            () -> new SwordItem(ModTiers.RUBIS, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModTiers.RUBIS, 1, -1.5f))));

    /** Marteau de guerre : 11.5 dégâts, très lent (vitesse 0.6), onde de choc */
    public static final DeferredItem<WarHammerItem> RUBIS_WAR_HAMMER = ITEMS.register("rubis_war_hammer",
            () -> new WarHammerItem(ModTiers.RUBIS, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModTiers.RUBIS, 7, -3.4f)), "war_hammer"));

    // ---------- Armes magiques ----------
    /** Épée de flammes : enflamme la cible pendant 5 secondes */
    public static final DeferredItem<MagicSwordItem> FLAME_SWORD = ITEMS.register("flame_sword",
            () -> new MagicSwordItem(ModTiers.RUBIS, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(SwordItem.createAttributes(ModTiers.RUBIS, 3, -2.4f)),
                    "flame_sword",
                    (target, attacker) -> target.igniteForSeconds(5)));

    /** Lame de givre : ralentit fortement et gèle la cible */
    public static final DeferredItem<MagicSwordItem> FROST_SWORD = ITEMS.register("frost_sword",
            () -> new MagicSwordItem(ModTiers.RUBIS, new Item.Properties().rarity(Rarity.RARE)
                    .attributes(SwordItem.createAttributes(ModTiers.RUBIS, 3, -2.4f)),
                    "frost_sword",
                    (target, attacker) -> {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), attacker);
                        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 40));
                    }));

    /** Dague venimeuse : empoisonne la cible (Poison II, 5 secondes) */
    public static final DeferredItem<MagicSwordItem> VENOM_DAGGER = ITEMS.register("venom_dagger",
            () -> new MagicSwordItem(ModTiers.RUBIS, new Item.Properties().rarity(Rarity.RARE)
                    .attributes(SwordItem.createAttributes(ModTiers.RUBIS, 1, -1.5f)),
                    "venom_dagger",
                    (target, attacker) -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), attacker)));

    /** Lame de l'End : clic droit = téléportation de 8 blocs */
    public static final DeferredItem<EnderBladeItem> ENDER_BLADE = ITEMS.register("ender_blade",
            () -> new EnderBladeItem(ModTiers.RUBIS, new Item.Properties().rarity(Rarity.EPIC)
                    .attributes(SwordItem.createAttributes(ModTiers.RUBIS, 3, -2.2f)), "ender_blade"));

    /** Marteau du tonnerre : onde de choc + clic droit = foudre */
    public static final DeferredItem<ThunderHammerItem> THUNDER_HAMMER = ITEMS.register("thunder_hammer",
            () -> new ThunderHammerItem(ModTiers.RUBIS, new Item.Properties().rarity(Rarity.EPIC)
                    .attributes(SwordItem.createAttributes(ModTiers.RUBIS, 8, -3.4f)), "thunder_hammer"));

    // ---------- Armes à distance ----------
    public static final DeferredItem<RubisBowItem> RUBIS_BOW = ITEMS.register("rubis_bow",
            () -> new RubisBowItem(new Item.Properties().durability(768)));

    public static final DeferredItem<FireStaffItem> FIRE_STAFF = ITEMS.register("fire_staff",
            () -> new FireStaffItem(new Item.Properties().durability(250).rarity(Rarity.RARE)));

    // ---------- Armure ----------
    public static final DeferredItem<ArmorItem> RUBIS_HELMET = ITEMS.register("rubis_helmet",
            () -> new ArmorItem(ModArmorMaterials.RUBIS, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(35))));
    public static final DeferredItem<ArmorItem> RUBIS_CHESTPLATE = ITEMS.register("rubis_chestplate",
            () -> new ArmorItem(ModArmorMaterials.RUBIS, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(35))));
    public static final DeferredItem<ArmorItem> RUBIS_LEGGINGS = ITEMS.register("rubis_leggings",
            () -> new ArmorItem(ModArmorMaterials.RUBIS, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(35))));
    public static final DeferredItem<ArmorItem> RUBIS_BOOTS = ITEMS.register("rubis_boots",
            () -> new ArmorItem(ModArmorMaterials.RUBIS, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(35))));

    // ---------- Armes à feu ----------
    // Munitions
    public static final DeferredItem<Item> BULLET = ITEMS.registerSimpleItem("bullet");
    public static final DeferredItem<Item> SHOTGUN_SHELL = ITEMS.registerSimpleItem("shotgun_shell");
    public static final DeferredItem<Item> ROCKET = ITEMS.registerSimpleItem("rocket", new Item.Properties().stacksTo(16));

    //                                           dégâts plombs chargeur cadence recharge vitesse dispersion visée recul  auto
    /** Pistolet : précis, semi-automatique. */
    public static final DeferredItem<GunItem> PISTOL = ITEMS.register("pistol",
            () -> new GunItem(new GunStats(6.0f, 1, 12, 5, 30, 4.0f, 1.5f, 0.5f, 2.5f, false,
                    ModItems.BULLET, SoundEvents.GENERIC_EXPLODE.value(), 0.4f, 1.9f), new Item.Properties()));

    /** Fusil d'assaut : tir automatique en maintenant le clic droit. */
    public static final DeferredItem<GunItem> ASSAULT_RIFLE = ITEMS.register("assault_rifle",
            () -> new GunItem(new GunStats(5.0f, 1, 30, 3, 45, 4.5f, 2.5f, 1.0f, 1.2f, true,
                    ModItems.BULLET, SoundEvents.GENERIC_EXPLODE.value(), 0.35f, 1.7f), new Item.Properties()));

    /** Fusil à pompe : 8 plombs, dévastateur de près. */
    public static final DeferredItem<GunItem> SHOTGUN = ITEMS.register("shotgun",
            () -> new GunItem(new GunStats(3.5f, 8, 6, 18, 50, 3.0f, 9.0f, 7.0f, 6.0f, false,
                    ModItems.SHOTGUN_SHELL, SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 1.2f), new Item.Properties()));

    /** Fusil de sniper : énormes dégâts, zoom en s'accroupissant, parfaitement précis en visée. */
    public static final DeferredItem<GunItem> SNIPER_RIFLE = ITEMS.register("sniper_rifle",
            () -> new GunItem(new GunStats(22.0f, 1, 5, 30, 60, 7.0f, 4.0f, 0.0f, 8.0f, false,
                    ModItems.BULLET, SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 1.4f),
                    new Item.Properties().rarity(Rarity.RARE)));

    /** Lance-roquettes : 1 roquette explosive par chargeur. */
    public static final DeferredItem<RocketLauncherItem> ROCKET_LAUNCHER = ITEMS.register("rocket_launcher",
            () -> new RocketLauncherItem(new GunStats(0.0f, 1, 1, 20, 60, 1.8f, 0.5f, 0.2f, 10.0f, false,
                    ModItems.ROCKET, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.6f),
                    new Item.Properties().rarity(Rarity.EPIC)));

    private ModItems() {}
}

package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.EnderBladeItem;
import fr.modeminecraft.item.FireStaffItem;
import fr.modeminecraft.item.MagicSwordItem;
import fr.modeminecraft.item.RubisBowItem;
import fr.modeminecraft.item.ThunderHammerItem;
import fr.modeminecraft.item.WarHammerItem;
import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.AttachmentSlot;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunStats;
import fr.modeminecraft.item.gun.SightOverlay;
import net.minecraft.sounds.SoundEvent;
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

import java.util.function.Supplier;

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

    // ================================================================
    // ARMES À FEU
    // ================================================================

    // ---------- Munitions (calibres) ----------
    public static final DeferredItem<Item> AMMO_9MM = ITEMS.registerSimpleItem("ammo_9mm");
    public static final DeferredItem<Item> AMMO_556 = ITEMS.registerSimpleItem("ammo_556");
    public static final DeferredItem<Item> AMMO_762 = ITEMS.registerSimpleItem("ammo_762");
    public static final DeferredItem<Item> SHOTGUN_SHELL = ITEMS.registerSimpleItem("shotgun_shell");
    public static final DeferredItem<Item> RPG_ROCKET = ITEMS.registerSimpleItem("rpg_rocket", new Item.Properties().stacksTo(16));

    private static final Supplier<SoundEvent> SHOT = SoundEvents.GENERIC_EXPLODE::value;
    private static final Supplier<SoundEvent> ROCKET_SHOT = () -> SoundEvents.FIREWORK_ROCKET_LAUNCH;
    private static final AttachmentSlot[] ALL_SLOTS = AttachmentSlot.values();

    // ---------- Armes ----------
    /** Pistolet 9 mm : précis, semi-automatique. */
    public static final DeferredItem<GunItem> PISTOL = gun("pistol", Rarity.COMMON, GunStats.builder()
            .damage(6).magazine(12).fireDelay(5).reload(30).velocity(4.0f).spread(1.5f, 0.5f).recoil(2.5f)
            .ammo(ModItems.AMMO_9MM).sound(SHOT, 0.4f, 1.9f)
            .slots(AttachmentSlot.SIGHT, AttachmentSlot.MUZZLE, AttachmentSlot.MAGAZINE));

    /** Fusil d'assaut générique 5.56. */
    public static final DeferredItem<GunItem> ASSAULT_RIFLE = gun("assault_rifle", Rarity.COMMON, GunStats.builder()
            .damage(5).magazine(30).fireDelay(3).reload(45).velocity(4.5f).spread(2.5f, 1.0f).recoil(1.2f).automatic()
            .ammo(ModItems.AMMO_556).sound(SHOT, 0.35f, 1.7f).slots(ALL_SLOTS));

    /** AK-47 : 7.62, gros dégâts mais beaucoup de recul. */
    public static final DeferredItem<GunItem> AK47 = gun("ak47", Rarity.UNCOMMON, GunStats.builder()
            .damage(7).magazine(30).fireDelay(3).reload(50).velocity(4.5f).spread(3.0f, 1.2f).recoil(2.0f).automatic()
            .ammo(ModItems.AMMO_762).sound(SHOT, 0.45f, 1.45f).slots(ALL_SLOTS));

    /** M4 : 5.56, précis et rapide. */
    public static final DeferredItem<GunItem> M4 = gun("m4", Rarity.UNCOMMON, GunStats.builder()
            .damage(5.5f).magazine(30).fireDelay(2).reload(40).velocity(5.0f).spread(2.0f, 0.7f).recoil(1.1f).automatic()
            .ammo(ModItems.AMMO_556).sound(SHOT, 0.38f, 1.75f).slots(ALL_SLOTS));

    /** FAMAS : 5.56, cadence de tir énorme, chargeur de 25. */
    public static final DeferredItem<GunItem> FAMAS = gun("famas", Rarity.UNCOMMON, GunStats.builder()
            .damage(4.5f).magazine(25).fireDelay(1).reload(42).velocity(5.0f).spread(2.2f, 0.9f).recoil(0.9f).automatic()
            .ammo(ModItems.AMMO_556).sound(SHOT, 0.35f, 1.85f).slots(ALL_SLOTS));

    /** Fusil à pompe : 8 plombs, dévastateur de près. */
    public static final DeferredItem<GunItem> SHOTGUN = gun("shotgun", Rarity.COMMON, GunStats.builder()
            .damage(3.5f).pellets(8).magazine(6).fireDelay(18).reload(50).velocity(3.0f).spread(9.0f, 7.0f).recoil(6.0f)
            .ammo(ModItems.SHOTGUN_SHELL).sound(SHOT, 0.7f, 1.2f)
            .slots(AttachmentSlot.SIGHT, AttachmentSlot.MUZZLE, AttachmentSlot.GRIP));

    /** Fusil de sniper 7.62 : lunette x5 intégrée. */
    public static final DeferredItem<GunItem> SNIPER_RIFLE = gun("sniper_rifle", Rarity.RARE, GunStats.builder()
            .damage(22).magazine(5).fireDelay(30).reload(60).velocity(7.0f).spread(4.0f, 0.0f).recoil(8.0f).scope(0.2f)
            .ammo(ModItems.AMMO_762).sound(SHOT, 0.9f, 1.4f)
            .slots(AttachmentSlot.MUZZLE, AttachmentSlot.GRIP));

    /** Lance-roquettes. */
    public static final DeferredItem<GunItem> ROCKET_LAUNCHER = gun("rocket_launcher", Rarity.EPIC, GunStats.builder()
            .magazine(1).fireDelay(20).reload(60).velocity(1.8f).spread(0.5f, 0.2f).recoil(10).explosion(3.0f)
            .ammo(ModItems.RPG_ROCKET).sound(ROCKET_SHOT, 1.5f, 0.6f));

    /** RPG-7 : roquette plus puissante, viseur optique possible. */
    public static final DeferredItem<GunItem> RPG7 = gun("rpg7", Rarity.EPIC, GunStats.builder()
            .magazine(1).fireDelay(20).reload(70).velocity(2.2f).spread(0.6f, 0.15f).recoil(10).explosion(4.0f)
            .ammo(ModItems.RPG_ROCKET).sound(ROCKET_SHOT, 1.6f, 0.5f).slots(AttachmentSlot.SIGHT));

    // ---------- Accessoires ----------
    public static final DeferredItem<AttachmentItem> RED_DOT_SIGHT = attachment("red_dot_sight",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).sight(0.85f, SightOverlay.RED_DOT).aimSpread(0.7f));
    public static final DeferredItem<AttachmentItem> HOLO_SIGHT = attachment("holo_sight",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).sight(0.75f, SightOverlay.HOLO).aimSpread(0.6f));
    public static final DeferredItem<AttachmentItem> ACOG_SCOPE = attachment("acog_scope",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).sight(0.25f, SightOverlay.SCOPE_X4).aimSpread(0.4f));
    public static final DeferredItem<AttachmentItem> SILENCER = attachment("silencer",
            AttachmentItem.Stats.of(AttachmentSlot.MUZZLE).silencer().damage(0.9f));
    public static final DeferredItem<AttachmentItem> COMPENSATOR = attachment("compensator",
            AttachmentItem.Stats.of(AttachmentSlot.MUZZLE).recoil(0.6f));
    public static final DeferredItem<AttachmentItem> VERTICAL_GRIP = attachment("vertical_grip",
            AttachmentItem.Stats.of(AttachmentSlot.GRIP).recoil(0.65f));
    public static final DeferredItem<AttachmentItem> ANGLED_GRIP = attachment("angled_grip",
            AttachmentItem.Stats.of(AttachmentSlot.GRIP).spread(0.7f).aimSpread(0.8f));
    public static final DeferredItem<AttachmentItem> MAGAZINE_24 = attachment("magazine_24",
            AttachmentItem.Stats.of(AttachmentSlot.MAGAZINE).magazine(24));
    public static final DeferredItem<AttachmentItem> MAGAZINE_36 = attachment("magazine_36",
            AttachmentItem.Stats.of(AttachmentSlot.MAGAZINE).magazine(36).reload(1.1f));
    public static final DeferredItem<AttachmentItem> DRUM_MAGAZINE = attachment("drum_magazine",
            AttachmentItem.Stats.of(AttachmentSlot.MAGAZINE).magazine(75).reload(1.6f).spread(1.1f));

    // ---------- Tables ----------
    public static final DeferredItem<BlockItem> ARMAMENT_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.ARMAMENT_TABLE);
    public static final DeferredItem<BlockItem> MODIFICATION_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.MODIFICATION_TABLE);

    private static DeferredItem<GunItem> gun(String name, Rarity rarity, GunStats.Builder stats) {
        return ITEMS.register(name, () -> new GunItem(stats.build(), new Item.Properties().rarity(rarity)));
    }

    private static DeferredItem<AttachmentItem> attachment(String name, AttachmentItem.Stats stats) {
        return ITEMS.register(name, () -> new AttachmentItem(stats, new Item.Properties()));
    }

    private ModItems() {}
}

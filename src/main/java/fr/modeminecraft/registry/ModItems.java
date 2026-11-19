package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.AmmoItem;
import fr.modeminecraft.item.EnderBladeItem;
import fr.modeminecraft.item.FireStaffItem;
import fr.modeminecraft.item.MagicSwordItem;
import fr.modeminecraft.item.RubisBowItem;
import fr.modeminecraft.item.ThunderHammerItem;
import fr.modeminecraft.item.WarHammerItem;
import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.AttachmentSlot;
import fr.modeminecraft.item.gun.FireMode;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunStats;
import fr.modeminecraft.item.gun.SightMode;
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

    // ---------- Munitions (calibres réalistes) ----------
    public static final DeferredItem<Item> AMMO_9MM = ammo("ammo_9mm");          // Glock, Beretta, Makarov, MP5, UZI
    public static final DeferredItem<Item> AMMO_45ACP = ammo("ammo_45acp");      // M1911, Thompson, Vector
    public static final DeferredItem<Item> AMMO_50AE = ammo("ammo_50ae");        // Desert Eagle
    public static final DeferredItem<Item> AMMO_357 = ammo("ammo_357");          // Colt Python
    public static final DeferredItem<Item> AMMO_57 = ammo("ammo_57");            // FN P90
    public static final DeferredItem<Item> AMMO_46 = ammo("ammo_46");            // MP7
    public static final DeferredItem<Item> AMMO_762X25 = ammo("ammo_762x25");    // PPSh-41
    public static final DeferredItem<Item> AMMO_545 = ammo("ammo_545");          // AK-74, AN-94
    public static final DeferredItem<Item> AMMO_556 = ammo("ammo_556");          // M4, FAMAS, HK416, G36, AUG, Tavor, M16A4, M249
    public static final DeferredItem<Item> AMMO_762 = ammo("ammo_762");          // AK-47, SCAR-H, G3, M14, M60, Minigun
    public static final DeferredItem<Item> AMMO_762X54R = ammo("ammo_762x54r");  // Dragunov, Mosin-Nagant, PKM
    public static final DeferredItem<Item> AMMO_792 = ammo("ammo_792");          // Kar98k
    public static final DeferredItem<Item> AMMO_338 = ammo("ammo_338");          // AWM
    public static final DeferredItem<Item> AMMO_50BMG = ammo("ammo_50bmg");      // Barrett M82
    public static final DeferredItem<Item> SHOTGUN_SHELL = ITEMS.register("shotgun_shell", // fusils à pompe
            () -> new AmmoItem(true, new Item.Properties()));
    public static final DeferredItem<Item> GRENADE_40MM = ITEMS.registerSimpleItem("grenade_40mm", new Item.Properties().stacksTo(16));
    /** Canon de rechange : pour changer le canon d'une mitrailleuse en surchauffe (touche R). */
    public static final DeferredItem<Item> SPARE_BARREL = ITEMS.registerSimpleItem("spare_barrel", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> RPG_ROCKET = ITEMS.registerSimpleItem("rpg_rocket", new Item.Properties().stacksTo(16));

    private static final Supplier<SoundEvent> SHOT = SoundEvents.GENERIC_EXPLODE::value;
    private static final Supplier<SoundEvent> ROCKET_SHOT = () -> SoundEvents.FIREWORK_ROCKET_LAUNCH;
    private static final Supplier<SoundEvent> GRENADE_SHOT = () -> SoundEvents.FIREWORK_ROCKET_BLAST;

    // Emplacements d'accessoires selon le type d'arme
    private static final AttachmentSlot SIGHT = AttachmentSlot.SIGHT, MUZZLE = AttachmentSlot.MUZZLE,
            GRIP = AttachmentSlot.GRIP, MAG = AttachmentSlot.MAGAZINE,
            PGRIP = AttachmentSlot.PISTOL_GRIP, STOCK = AttachmentSlot.STOCK,
            RAIL = AttachmentSlot.SIDE_RAIL;

    // Pistolet / pistolet-mitrailleur / fusil : réglages communs
    private static GunStats.Builder pistol() {
        return GunStats.builder().pistolCategory().velocity(4.0f).spread(1.5f, 0.5f).sound(SHOT, 0.4f, 1.9f)
                .slots(SIGHT, MUZZLE, MAG, PGRIP, STOCK);
    }

    private static GunStats.Builder smg() {
        return GunStats.builder().category(GunStats.Category.SMG).automatic().velocity(4.0f).spread(2.2f, 0.9f).recoil(0.9f).sound(SHOT, 0.35f, 1.95f)
                .draw(7).slots(SIGHT, MUZZLE, GRIP, MAG, PGRIP, STOCK);
    }

    private static GunStats.Builder rifle() {
        return GunStats.builder().category(GunStats.Category.ASSAULT).automatic().velocity(4.8f).spread(2.2f, 0.8f).recoil(1.2f)
                .sound(SHOT, 0.4f, 1.7f).slots(SIGHT, MUZZLE, GRIP, MAG, PGRIP, STOCK, RAIL);
    }

    private static GunStats.Builder sniper() {
        return GunStats.builder().category(GunStats.Category.SNIPER).velocity(7.5f).spread(4.0f, 0.0f).sound(SHOT, 0.9f, 1.35f).draw(14)
                .slots(SIGHT, MUZZLE, GRIP, PGRIP, STOCK);
    }

    private static GunStats.Builder shotgun() {
        return GunStats.builder().category(GunStats.Category.SHOTGUN).velocity(3.0f).spread(8.0f, 6.0f).ammo(ModItems.SHOTGUN_SHELL).sound(SHOT, 0.7f, 1.2f)
                .slots(SIGHT, MUZZLE, GRIP, PGRIP, STOCK);
    }

    private static GunStats.Builder machineGun() {
        return GunStats.builder().category(GunStats.Category.MACHINE_GUN).automatic().velocity(4.8f).spread(3.2f, 1.4f).sound(SHOT, 0.5f, 1.5f).draw(18).heat(1.3f)
                .slots(SIGHT, MUZZLE, GRIP, PGRIP, STOCK);
    }

    // ================= PISTOLETS =================
    public static final DeferredItem<GunItem> GLOCK17 = gun("glock17", Rarity.COMMON,
            pistol().slideLock().damage(6).magazine(17).fireDelay(4).reload(28).recoil(2.2f).ammo(ModItems.AMMO_9MM));
    public static final DeferredItem<GunItem> DESERT_EAGLE = gun("desert_eagle", Rarity.UNCOMMON,
            pistol().slideLock().damage(13).magazine(7).fireDelay(8).reload(34).recoil(7).spread(2.0f, 0.6f).ammo(ModItems.AMMO_50AE)
                    .sound(SHOT, 0.7f, 1.4f));
    public static final DeferredItem<GunItem> BERETTA_M9 = gun("beretta_m9", Rarity.COMMON,
            pistol().slideLock().damage(6).magazine(15).fireDelay(4).reload(30).recoil(2.4f).ammo(ModItems.AMMO_9MM));
    public static final DeferredItem<GunItem> COLT_M1911 = gun("colt_m1911", Rarity.COMMON,
            pistol().slideLock().damage(8.5f).magazine(7).fireDelay(5).reload(30).recoil(3.5f).ammo(ModItems.AMMO_45ACP)
                    .sound(SHOT, 0.5f, 1.7f));
    /** Revolver : rechargement balle par balle. */
    public static final DeferredItem<GunItem> COLT_PYTHON = gun("colt_python", Rarity.UNCOMMON,
            GunStats.builder().noChamber().casing(GunStats.Casing.NONE).damage(11).magazine(6).fireDelay(7).reloadPerRound(10).velocity(4.5f).spread(1.4f, 0.4f)
                    .recoil(5.5f).ammo(ModItems.AMMO_357).sound(SHOT, 0.6f, 1.55f).pistolCategory().slots(SIGHT, PGRIP));
    public static final DeferredItem<GunItem> MAKAROV = gun("makarov", Rarity.COMMON,
            pistol().slideLock().damage(5.5f).magazine(8).fireDelay(4).reload(26).recoil(2).ammo(ModItems.AMMO_9MM));

    // ================= PISTOLETS-MITRAILLEURS =================
    public static final DeferredItem<GunItem> MP5 = gun("mp5", Rarity.UNCOMMON,
            smg().damage(5).magazine(30).fireDelay(2).reload(38).spread(1.8f, 0.6f).ammo(ModItems.AMMO_9MM));
    public static final DeferredItem<GunItem> UZI = gun("uzi", Rarity.COMMON,
            smg().damage(4.5f).magazine(32).fireDelay(2).reload(36).spread(3.0f, 1.4f).recoil(1.0f).ammo(ModItems.AMMO_9MM));
    public static final DeferredItem<GunItem> P90 = gun("p90", Rarity.UNCOMMON,
            smg().damage(4.5f).magazine(50).fireDelay(2).reload(50).recoil(0.7f).ammo(ModItems.AMMO_57)
                    .noSlots().slots(SIGHT, MUZZLE, PGRIP)); // chargeur sur le dessus, bullpup : ni chargeur ni crosse
    /** MP7 : balles perforantes (ignorent l'armure). */
    public static final DeferredItem<GunItem> MP7 = gun("mp7", Rarity.UNCOMMON,
            smg().damage(4).magazine(40).fireDelay(2).reload(40).spread(2.0f, 0.8f).armorPiercing().ammo(ModItems.AMMO_46));
    public static final DeferredItem<GunItem> THOMPSON = gun("thompson", Rarity.UNCOMMON,
            smg().damage(6).magazine(50).fireDelay(2).reload(60).spread(3.0f, 1.5f).recoil(1.4f).ammo(ModItems.AMMO_45ACP)
                    .sound(SHOT, 0.45f, 1.6f));
    public static final DeferredItem<GunItem> VECTOR = gun("vector", Rarity.RARE,
            smg().damage(5.5f).magazine(25).fireDelay(1).reload(40).spread(2.0f, 0.8f).recoil(0.5f).ammo(ModItems.AMMO_45ACP));
    public static final DeferredItem<GunItem> PPSH41 = gun("ppsh41", Rarity.UNCOMMON,
            smg().casing(GunStats.Casing.STEEL).damage(4.5f).magazine(71).fireDelay(1).reload(70).spread(3.5f, 1.8f).recoil(1.0f).ammo(ModItems.AMMO_762X25));

    // ================= FUSILS D'ASSAUT =================
    public static final DeferredItem<GunItem> AK47 = gun("ak47", Rarity.UNCOMMON,
            rifle().casing(GunStats.Casing.STEEL).damage(7).magazine(30).fireDelay(3).reload(50).spread(3.0f, 1.2f).recoil(2.0f).ammo(ModItems.AMMO_762)
                    .sound(SHOT, 0.45f, 1.45f));
    public static final DeferredItem<GunItem> AK74 = gun("ak74", Rarity.UNCOMMON,
            rifle().casing(GunStats.Casing.STEEL).damage(6).magazine(30).fireDelay(3).reload(48).spread(2.4f, 0.9f).recoil(1.4f).ammo(ModItems.AMMO_545));
    public static final DeferredItem<GunItem> M4 = gun("m4", Rarity.UNCOMMON,
            rifle().damage(5.5f).magazine(30).fireDelay(2).reload(40).velocity(5.0f).spread(2.0f, 0.7f).recoil(1.1f)
                    .ammo(ModItems.AMMO_556).sound(SHOT, 0.38f, 1.75f));
    public static final DeferredItem<GunItem> HK416 = gun("hk416", Rarity.RARE,
            rifle().damage(5.8f).magazine(30).fireDelay(2).reload(40).velocity(5.0f).spread(1.8f, 0.6f).recoil(1.0f)
                    .ammo(ModItems.AMMO_556));
    public static final DeferredItem<GunItem> FAMAS = gun("famas", Rarity.UNCOMMON,
            rifle().damage(4.5f).magazine(25).fireDelay(1).reload(42).velocity(5.0f).spread(2.2f, 0.9f).recoil(0.9f)
                    .ammo(ModItems.AMMO_556).sound(SHOT, 0.35f, 1.85f).without(STOCK));
    public static final DeferredItem<GunItem> SCAR_H = gun("scar_h", Rarity.RARE,
            rifle().damage(8).magazine(20).fireDelay(3).reload(45).spread(2.2f, 0.8f).recoil(2.2f).ammo(ModItems.AMMO_762)
                    .sound(SHOT, 0.5f, 1.45f));
    /** G36 : lunette intégrée (pas d'emplacement viseur). */
    public static final DeferredItem<GunItem> G36 = gun("g36", Rarity.UNCOMMON,
            rifle().damage(5.5f).magazine(30).fireDelay(2).reload(42).spread(2.0f, 0.6f).recoil(1.0f).ammo(ModItems.AMMO_556)
                    .scope(0.4f, SightOverlay.SCOPE_X4).noSlots().slots(MUZZLE, GRIP, MAG, PGRIP, STOCK, RAIL));
    /** Steyr AUG : bullpup avec lunette intégrée. */
    public static final DeferredItem<GunItem> AUG = gun("aug", Rarity.UNCOMMON,
            rifle().damage(5.5f).magazine(30).fireDelay(2).reload(44).spread(2.0f, 0.6f).recoil(1.0f).ammo(ModItems.AMMO_556)
                    .scope(0.6f, SightOverlay.SCOPE_X4).noSlots().slots(MUZZLE, GRIP, MAG, PGRIP, RAIL)); // bullpup : pas de crosse
    public static final DeferredItem<GunItem> TAVOR = gun("tavor", Rarity.UNCOMMON,
            rifle().damage(5.5f).magazine(30).fireDelay(2).reload(40).spread(2.0f, 0.8f).recoil(1.0f).ammo(ModItems.AMMO_556).without(STOCK));
    /** M16A4 : coup par coup ou rafale de 3 (pas d'automatique). */
    public static final DeferredItem<GunItem> M16A4 = gun("m16a4", Rarity.UNCOMMON,
            rifle().modes(FireMode.SEMI, FireMode.BURST).damage(6.5f).magazine(30).fireDelay(2).burst(3, 8).reload(42).velocity(5.2f).spread(1.6f, 0.5f)
                    .recoil(1.0f).ammo(ModItems.AMMO_556));
    public static final DeferredItem<GunItem> G3 = gun("g3", Rarity.UNCOMMON,
            rifle().damage(8.5f).magazine(20).fireDelay(3).reload(48).spread(2.5f, 0.9f).recoil(2.5f).ammo(ModItems.AMMO_762)
                    .sound(SHOT, 0.5f, 1.4f));
    /** AN-94 : rafale de 2 coups ultra-rapide (ou coup par coup, ou automatique). */
    public static final DeferredItem<GunItem> AN94 = gun("an94", Rarity.RARE,
            rifle().casing(GunStats.Casing.STEEL).damage(6).magazine(30).fireDelay(1).burst(2, 6).reload(46).modes(FireMode.SEMI, FireMode.BURST, FireMode.AUTO).spread(1.6f, 0.4f).recoil(0.8f)
                    .ammo(ModItems.AMMO_545));

    // ================= FUSILS DE PRÉCISION =================
    /** Barrett M82 : traverse jusqu'à 3 ennemis. */
    public static final DeferredItem<GunItem> BARRETT_M82 = gun("barrett_m82", Rarity.EPIC,
            sniper().damage(35).magazine(10).fireDelay(20).reload(80).velocity(8.5f).recoil(12).pierce(3)
                    .scope(0.15f, SightOverlay.SNIPER).ammo(ModItems.AMMO_50BMG).sound(SHOT, 1.2f, 1.0f));
    public static final DeferredItem<GunItem> AWM = gun("awm", Rarity.EPIC,
            sniper().damage(30).magazine(5).fireDelay(30).reload(70).velocity(8.5f).recoil(9)
                    .scope(0.15f, SightOverlay.SNIPER).ammo(ModItems.AMMO_338).sound(SHOT, 1.0f, 1.2f));
    public static final DeferredItem<GunItem> DRAGUNOV = gun("dragunov", Rarity.RARE,
            sniper().casing(GunStats.Casing.STEEL).damage(16).magazine(10).fireDelay(8).reload(55).recoil(5)
                    .scope(0.25f, SightOverlay.SNIPER).ammo(ModItems.AMMO_762X54R));
    /** Mosin-Nagant : viseur métallique, rechargement balle par balle. */
    public static final DeferredItem<GunItem> MOSIN_NAGANT = gun("mosin_nagant", Rarity.COMMON,
            sniper().casing(GunStats.Casing.STEEL).damage(18).magazine(5).fireDelay(25).reloadPerRound(12).spread(3.0f, 0.3f).recoil(6)
                    .ammo(ModItems.AMMO_762X54R).noSlots().slots(SIGHT, MUZZLE));
    public static final DeferredItem<GunItem> KAR98K = gun("kar98k", Rarity.COMMON,
            sniper().damage(18).magazine(5).fireDelay(24).reloadPerRound(12).spread(3.0f, 0.3f).recoil(6)
                    .ammo(ModItems.AMMO_792).noSlots().slots(SIGHT, MUZZLE));
    /** M14 EBR : fusil de tireur d'élite semi-automatique. */
    public static final DeferredItem<GunItem> M14_EBR = gun("m14_ebr", Rarity.RARE,
            sniper().damage(12).magazine(20).fireDelay(5).reload(50).velocity(6.0f).spread(2.5f, 0.3f).recoil(3.5f)
                    .ammo(ModItems.AMMO_762).noSlots().slots(SIGHT, MUZZLE, GRIP, MAG, PGRIP, STOCK));

    // ================= FUSILS À POMPE =================
    public static final DeferredItem<GunItem> REMINGTON_870 = gun("remington_870", Rarity.COMMON,
            shotgun().casing(GunStats.Casing.SHELL).damage(3.5f).pellets(8).magazine(6).fireDelay(18).reloadPerRound(10).recoil(6));
    public static final DeferredItem<GunItem> BENELLI_M4 = gun("benelli_m4", Rarity.UNCOMMON,
            shotgun().casing(GunStats.Casing.SHELL).damage(3.2f).pellets(8).magazine(7).fireDelay(7).reloadPerRound(9).recoil(5));
    public static final DeferredItem<GunItem> SPAS12 = gun("spas12", Rarity.UNCOMMON,
            shotgun().casing(GunStats.Casing.SHELL).damage(3.8f).pellets(9).magazine(8).fireDelay(16).reloadPerRound(10).recoil(6.5f));
    /** AA-12 : fusil à pompe automatique avec camembert. */
    public static final DeferredItem<GunItem> AA12 = gun("aa12", Rarity.RARE,
            shotgun().casing(GunStats.Casing.SHELL).damage(3.0f).pellets(7).magazine(20).fireDelay(4).automatic().reload(60).spread(9.0f, 7.0f).recoil(3)
                    .noSlots().slots(SIGHT, GRIP, PGRIP, STOCK));
    public static final DeferredItem<GunItem> SAWED_OFF = gun("sawed_off", Rarity.COMMON,
            shotgun().noChamber().casing(GunStats.Casing.NONE).damage(3.5f).pellets(10).magazine(2).fireDelay(4).reload(40).spread(14.0f, 12.0f).recoil(9)
                    .sound(SHOT, 0.9f, 1.0f).noSlots());

    // ================= MITRAILLEUSES =================
    public static final DeferredItem<GunItem> M249 = gun("m249", Rarity.RARE,
            machineGun().damage(5.5f).magazine(100).fireDelay(2).reload(100).recoil(1.0f).ammo(ModItems.AMMO_556));
    public static final DeferredItem<GunItem> PKM = gun("pkm", Rarity.RARE,
            machineGun().casing(GunStats.Casing.STEEL).damage(7.5f).magazine(100).fireDelay(3).reload(110).recoil(1.6f).ammo(ModItems.AMMO_762X54R));
    public static final DeferredItem<GunItem> M60 = gun("m60", Rarity.RARE,
            machineGun().damage(8).magazine(100).fireDelay(3).reload(120).spread(3.5f, 1.6f).recoil(2.2f).ammo(ModItems.AMMO_762));
    /** Minigun : temps de lancement d'une seconde, puis 20 balles par seconde. */
    public static final DeferredItem<GunItem> MINIGUN = gun("minigun", Rarity.EPIC,
            machineGun().noChamber().damage(5).magazine(200).fireDelay(1).spinUp(20).reload(160).spread(4.0f, 3.0f).recoil(0.4f)
                    .ammo(ModItems.AMMO_762).draw(25).heat(0.6f).noSlots());

    // ================= LANCEURS =================
    public static final DeferredItem<GunItem> RPG7 = gun("rpg7", Rarity.EPIC, GunStats.builder().noChamber()
            .magazine(1).fireDelay(20).reload(70).velocity(2.2f).spread(0.6f, 0.15f).recoil(10).rocket(4.0f)
            .ammo(ModItems.RPG_ROCKET).sound(ROCKET_SHOT, 1.6f, 0.5f).draw(16).slots(SIGHT));
    /** M32 MGL : lance-grenades à barillet de 6, rechargement grenade par grenade. */
    public static final DeferredItem<GunItem> M32_MGL = gun("m32_mgl", Rarity.EPIC, GunStats.builder().noChamber()
            .magazine(6).fireDelay(10).reloadPerRound(14).velocity(1.6f).spread(1.0f, 0.4f).recoil(5).grenade(2.2f)
            .ammo(ModItems.GRENADE_40MM).sound(GRENADE_SHOT, 1.0f, 0.6f).slots(SIGHT));
    public static final DeferredItem<GunItem> M79 = gun("m79", Rarity.RARE, GunStats.builder().noChamber()
            .magazine(1).fireDelay(10).reload(35).velocity(1.6f).spread(1.0f, 0.4f).recoil(6).grenade(2.5f)
            .ammo(ModItems.GRENADE_40MM).sound(GRENADE_SHOT, 1.0f, 0.5f));
    /** AT4 : lance-roquettes jetable, vendu chargé. */
    public static final DeferredItem<GunItem> AT4 = gun("at4", Rarity.EPIC, GunStats.builder().noChamber()
            .magazine(1).fireDelay(20).velocity(2.6f).spread(0.5f, 0.1f).recoil(12).rocket(5.0f).disposable()
            .ammo(ModItems.RPG_ROCKET).sound(ROCKET_SHOT, 1.8f, 0.45f));

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


    // ---------- Poignées AVANT (emplacement "poignée avant", sous le canon) ----------
    public static final DeferredItem<AttachmentItem> STUBBY_GRIP = attachment("stubby_grip", fg().recoil(0.8f));
    public static final DeferredItem<AttachmentItem> MAGPUL_AFG = attachment("magpul_afg", fg().spread(0.7f).recoil(0.9f));
    public static final DeferredItem<AttachmentItem> MAGPUL_RVG = attachment("magpul_rvg", fg().recoil(0.75f).spread(0.9f));
    public static final DeferredItem<AttachmentItem> MAGPUL_MVG = attachment("magpul_mvg", fg().recoil(0.8f).reload(0.95f));
    public static final DeferredItem<AttachmentItem> BCM_KAG = attachment("bcm_kag", fg().recoil(0.65f).aimSpeed(0.85f));
    public static final DeferredItem<AttachmentItem> HAND_STOP = attachment("hand_stop", fg().aimSpread(0.8f));
    public static final DeferredItem<AttachmentItem> GRIP_POD = attachment("grip_pod", fg().steady(0.3f, 1.0f));
    public static final DeferredItem<AttachmentItem> HARRIS_BIPOD = attachment("harris_bipod", fg().steady(0.2f, 0.5f));
    public static final DeferredItem<AttachmentItem> FLASHLIGHT_GRIP = attachment("flashlight_grip",
            fg().special(AttachmentItem.Special.NIGHT_VISION));
    public static final DeferredItem<AttachmentItem> LASER_GRIP = attachment("laser_grip",
            fg().hipSpread(0.5f).special(AttachmentItem.Special.LASER));
    public static final DeferredItem<AttachmentItem> LIGHT_LASER_GRIP = attachment("light_laser_grip",
            fg().hipSpread(0.5f).reload(1.1f).special(AttachmentItem.Special.NIGHT_VISION, AttachmentItem.Special.LASER));
    public static final DeferredItem<AttachmentItem> WOODEN_FOREGRIP = attachment("wooden_foregrip", fg().recoil(0.7f));
    public static final DeferredItem<AttachmentItem> FOLDING_GRIP = attachment("folding_grip", fg().recoil(0.85f));
    public static final DeferredItem<AttachmentItem> CARBON_GRIP = attachment("carbon_grip", fg().recoil(0.9f).mobility(1.15f));
    public static final DeferredItem<AttachmentItem> RUBBER_FOREGRIP = attachment("rubber_foregrip", fg().recoil(0.8f).movingSpread(0.0f));
    public static final DeferredItem<AttachmentItem> M203 = attachment("m203",
            launcher().guns("m4", "m16a4", "hk416", "tavor"));
    public static final DeferredItem<AttachmentItem> M26_MASS = attachment("m26_mass",
            fg().mobility(0.85f).aimSpeed(0.85f).special(AttachmentItem.Special.UNDERBARREL_SHOTGUN).guns("m4", "m16a4", "hk416"));
    public static final DeferredItem<AttachmentItem> BAYONET = attachment("bayonet", fg().special(AttachmentItem.Special.BAYONET));
    public static final DeferredItem<AttachmentItem> RUBY_FOREGRIP = attachment("ruby_foregrip", fg().recoil(0.6f));
    public static final DeferredItem<AttachmentItem> TANGO_DOWN_GRIP = attachment("tango_down_grip", fg().recoil(0.75f).spread(0.95f));

    // ---------- Poignées PISTOLET ----------
    public static final DeferredItem<AttachmentItem> MAGPUL_MOE = attachment("magpul_moe", pg().recoil(0.9f));
    public static final DeferredItem<AttachmentItem> MAGPUL_K2 = attachment("magpul_k2", pg().aimSpeed(1.25f));
    public static final DeferredItem<AttachmentItem> HOGUE_GRIP = attachment("hogue_grip", pg().recoil(0.8f));
    public static final DeferredItem<AttachmentItem> BCM_GUNFIGHTER = attachment("bcm_gunfighter", pg().spread(0.85f));
    public static final DeferredItem<AttachmentItem> TANGO_BATTLE_GRIP = attachment("tango_battle_grip", pg().recoil(0.85f).reload(0.9f));
    public static final DeferredItem<AttachmentItem> ERGO_GRIP = attachment("ergo_grip", pg().movingSpread(0.5f));
    public static final DeferredItem<AttachmentItem> WALNUT_GRIP = attachment("walnut_grip", pg().recoil(0.9f));
    public static final DeferredItem<AttachmentItem> PEARL_GRIP = attachment("pearl_grip", pg().cosmetic());
    public static final DeferredItem<AttachmentItem> GOLD_GRIP = attachment("gold_grip", pg().cosmetic());
    public static final DeferredItem<AttachmentItem> RUBY_PISTOL_GRIP = attachment("ruby_pistol_grip", pg().recoil(0.75f).spread(0.9f));
    public static final DeferredItem<AttachmentItem> SKELETON_GRIP = attachment("skeleton_grip", pg().reload(0.85f));
    public static final DeferredItem<AttachmentItem> COMPACT_GRIP = attachment("compact_grip", pg().draw(0.0f));
    public static final DeferredItem<AttachmentItem> COMPETITION_GRIP = attachment("competition_grip", pg().spread(0.75f).reload(1.1f));
    public static final DeferredItem<AttachmentItem> STIPPLED_GRIP = attachment("stippled_grip", pg().recoil(0.85f));
    public static final DeferredItem<AttachmentItem> THUMB_REST_GRIP = attachment("thumb_rest_grip", pg().aimSpread(0.8f));
    public static final DeferredItem<AttachmentItem> LASERGRIP = attachment("lasergrip",
            pg().hipSpread(0.6f).special(AttachmentItem.Special.LASER));
    public static final DeferredItem<AttachmentItem> STORAGE_GRIP = attachment("storage_grip", pg().reload(0.8f));
    public static final DeferredItem<AttachmentItem> FINGER_GROOVE_GRIP = attachment("finger_groove_grip", pg().recoil(0.8f).spread(0.95f));
    public static final DeferredItem<AttachmentItem> MAGPUL_MIAD = attachment("magpul_miad", pg().recoil(0.9f).spread(0.9f));
    public static final DeferredItem<AttachmentItem> GEL_GRIP = attachment("gel_grip", pg().recoil(0.7f).reload(1.1f));

    // ---------- Réticules / viseurs / lunettes ----------
    public static final DeferredItem<AttachmentItem> THERMAL_SCOPE = attachment("thermal_scope",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).sight(0.4f, SightOverlay.THERMAL).aimSpread(0.5f)
                    .special(AttachmentItem.Special.THERMAL));
    public static final DeferredItem<AttachmentItem> NIGHT_VISION_SCOPE = attachment("night_vision_scope",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).sight(0.4f, SightOverlay.NIGHT_VISION).aimSpread(0.5f)
                    .special(AttachmentItem.Special.NIGHT_VISION));
    public static final DeferredItem<AttachmentItem> KOBRA_SIGHT = attachment("kobra_sight", sight(0.85f, SightOverlay.KOBRA).aimSpread(0.7f));
    public static final DeferredItem<AttachmentItem> RMR_SIGHT = attachment("rmr_sight",
            sight(0.9f, SightOverlay.MINI_DOT).aimSpread(0.75f).aimSpeed(1.1f));
    public static final DeferredItem<AttachmentItem> MINI_REFLEX = attachment("mini_reflex",
            sight(0.9f, SightOverlay.MINI_DOT).aimSpread(0.8f).aimSpeed(1.2f));
    public static final DeferredItem<AttachmentItem> HOLO_MAGNIFIER = attachment("holo_magnifier",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).aimSpread(0.6f)
                    .sights(new SightMode(0.75f, SightOverlay.HOLO), new SightMode(0.33f, SightOverlay.HOLO_MAGNIFIED)));
    public static final DeferredItem<AttachmentItem> LPVO = attachment("lpvo",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).aimSpread(0.5f).sights(
                    new SightMode(0.9f, SightOverlay.LPVO), new SightMode(0.5f, SightOverlay.LPVO),
                    new SightMode(0.33f, SightOverlay.LPVO), new SightMode(0.25f, SightOverlay.LPVO),
                    new SightMode(0.2f, SightOverlay.LPVO), new SightMode(0.166f, SightOverlay.LPVO)));
    public static final DeferredItem<AttachmentItem> SCOPE_X6 = attachment("scope_x6",
            sight(0.166f, SightOverlay.SCOPE_X6).aimSpread(0.35f).aimSpeed(0.85f));
    public static final DeferredItem<AttachmentItem> SCOPE_X8 = attachment("scope_x8",
            sight(0.125f, SightOverlay.SCOPE_X8).aimSpread(0.3f).aimSpeed(0.8f));
    public static final DeferredItem<AttachmentItem> SCOPE_X12 = attachment("scope_x12",
            sight(0.083f, SightOverlay.SCOPE_X12).aimSpread(0.25f).aimSpeed(0.7f));
    public static final DeferredItem<AttachmentItem> PSO1 = attachment("pso1", sight(0.25f, SightOverlay.PSO1).aimSpread(0.4f));
    public static final DeferredItem<AttachmentItem> RANGEFINDER_SCOPE = attachment("rangefinder_scope",
            sight(0.2f, SightOverlay.RANGEFINDER).aimSpread(0.35f).special(AttachmentItem.Special.RANGEFINDER));
    public static final DeferredItem<AttachmentItem> ELCAN = attachment("elcan",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).aimSpread(0.45f)
                    .sights(new SightMode(0.9f, SightOverlay.MINI_DOT), new SightMode(0.25f, SightOverlay.ELCAN)));
    public static final DeferredItem<AttachmentItem> PRISM_X2 = attachment("prism_x2", sight(0.5f, SightOverlay.PRISM).aimSpread(0.6f));
    public static final DeferredItem<AttachmentItem> GHOST_RING = attachment("ghost_ring",
            sight(0.95f, SightOverlay.GHOST_RING).aimSpread(0.85f).aimSpeed(1.4f));
    public static final DeferredItem<AttachmentItem> CANTED_SCOPE = attachment("canted_scope",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).aimSpread(0.45f)
                    .sights(new SightMode(0.25f, SightOverlay.SCOPE_X4), new SightMode(0.95f, SightOverlay.IRON)));
    public static final DeferredItem<AttachmentItem> PEQ15 = attachment("peq15",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).hipSpread(0.5f).laser(0x30FF40));
    public static final DeferredItem<AttachmentItem> HUNTING_SCOPE = attachment("hunting_scope",
            sight(0.25f, SightOverlay.HUNTING).aimSpread(0.4f));
    public static final DeferredItem<AttachmentItem> DIGITAL_SCOPE = attachment("digital_scope",
            sight(0.25f, SightOverlay.DIGITAL).aimSpread(0.4f).special(AttachmentItem.Special.DIGITAL));
    public static final DeferredItem<AttachmentItem> RUBY_SCOPE = attachment("ruby_scope",
            sight(0.125f, SightOverlay.RUBY).aimSpread(0.3f).special(AttachmentItem.Special.RUBY_MARK));

    // ---------- Crosses ----------
    public static final DeferredItem<AttachmentItem> A2_STOCK = attachment("a2_stock", st().recoil(0.8f));
    public static final DeferredItem<AttachmentItem> CTR_STOCK = attachment("ctr_stock", st().recoil(0.85f).aimSpeed(1.15f));
    public static final DeferredItem<AttachmentItem> PRS_STOCK = attachment("prs_stock", st().aimSpread(0.6f).mobility(0.85f).aimSpeed(0.9f));
    public static final DeferredItem<AttachmentItem> SKELETON_STOCK = attachment("skeleton_stock", st().recoil(0.9f).mobility(1.1f));
    public static final DeferredItem<AttachmentItem> FOLDING_STOCK = attachment("folding_stock", st().recoil(0.9f).mobility(1.15f));
    public static final DeferredItem<AttachmentItem> WIRE_STOCK = attachment("wire_stock", st().recoil(0.9f).mobility(1.15f));
    public static final DeferredItem<AttachmentItem> WOODEN_STOCK = attachment("wooden_stock", st().recoil(0.75f).aimSpeed(0.85f));
    public static final DeferredItem<AttachmentItem> CHEEK_REST_STOCK = attachment("cheek_rest_stock", st().aimSpread(0.7f));
    public static final DeferredItem<AttachmentItem> THUMBHOLE_STOCK = attachment("thumbhole_stock", st().recoil(0.8f).spread(0.9f));
    public static final DeferredItem<AttachmentItem> NO_STOCK = attachment("no_stock",
            st().recoil(1.5f).mobility(1.4f).aimSpeed(1.2f).draw(0.7f));
    public static final DeferredItem<AttachmentItem> PISTOL_BRACE = attachment("pistol_brace",
            AttachmentItem.Stats.of(AttachmentSlot.STOCK).recoil(0.75f).only(AttachmentItem.Compat.PISTOL));
    public static final DeferredItem<AttachmentItem> BUFFER_TUBE = attachment("buffer_tube", st().recoil(0.95f).mobility(1.1f));
    public static final DeferredItem<AttachmentItem> LIMBSAVER_PAD = attachment("limbsaver_pad", st().recoil(0.75f));
    public static final DeferredItem<AttachmentItem> KNOXX_STOCK = attachment("knoxx_stock", st().recoil(0.55f).mobility(0.85f));
    public static final DeferredItem<AttachmentItem> CARBON_STOCK = attachment("carbon_stock", st().recoil(0.85f));
    public static final DeferredItem<AttachmentItem> MONOPOD_STOCK = attachment("monopod_stock", st().steady(1.0f, 0.5f));
    public static final DeferredItem<AttachmentItem> AI_STOCK = attachment("ai_stock", st().aimSpread(0.5f).mobility(0.85f));
    public static final DeferredItem<AttachmentItem> ZHUKOV_STOCK = attachment("zhukov_stock", st().recoil(0.8f));
    public static final DeferredItem<AttachmentItem> SCAR_FOLDING_STOCK = attachment("scar_folding_stock", st().recoil(0.85f).reload(0.95f));
    public static final DeferredItem<AttachmentItem> RUBY_STOCK = attachment("ruby_stock", st().recoil(0.65f).spread(0.85f));

    // ---------- Lance-grenades sous le canon (seulement sur les armes qui l'acceptent en vrai) ----------
    public static final DeferredItem<AttachmentItem> M320 = attachment("m320", launcher().guns("m4", "m16a4", "hk416"));
    public static final DeferredItem<AttachmentItem> GP25 = attachment("gp25", launcher().guns("ak47", "ak74", "an94"));
    public static final DeferredItem<AttachmentItem> AG36 = attachment("ag36", launcher().guns("g36"));
    public static final DeferredItem<AttachmentItem> FN_EGLM = attachment("fn_eglm", launcher().guns("scar_h"));
    public static final DeferredItem<AttachmentItem> HK79 = attachment("hk79", launcher().guns("g3"));

    // ---------- Lasers (rail latéral des fusils d'assaut, touche T) ----------
    public static final DeferredItem<AttachmentItem> LASER_RED = attachment("laser_red", laser(0xFF1010));
    public static final DeferredItem<AttachmentItem> LASER_GREEN = attachment("laser_green", laser(0x20FF30));
    public static final DeferredItem<AttachmentItem> LASER_BLUE = attachment("laser_blue", laser(0x2050FF));
    public static final DeferredItem<AttachmentItem> LASER_PURPLE = attachment("laser_purple", laser(0xB030FF));
    public static final DeferredItem<AttachmentItem> LASER_YELLOW = attachment("laser_yellow", laser(0xFFE020));
    public static final DeferredItem<AttachmentItem> LASER_ORANGE = attachment("laser_orange", laser(0xFF8010));
    public static final DeferredItem<AttachmentItem> LASER_CYAN = attachment("laser_cyan", laser(0x20F0FF));
    public static final DeferredItem<AttachmentItem> LASER_WHITE = attachment("laser_white", laser(0xFFFFFF));
    public static final DeferredItem<AttachmentItem> LASER_RAINBOW = attachment("laser_rainbow", laser(-1));

    // ---------- Silencieux (selon le calibre, comme en vrai) ----------
    public static final DeferredItem<AttachmentItem> OMEGA_9K = attachment("omega_9k", sil().damage(0.95f).calibers("ammo_9mm"));
    public static final DeferredItem<AttachmentItem> OSPREY_45 = attachment("osprey_45",
            sil().damage(0.92f).aimSpread(0.9f).calibers("ammo_45acp", "ammo_9mm"));
    public static final DeferredItem<AttachmentItem> GEMTECH_GM9 = attachment("gemtech_gm9", sil().damage(0.95f).calibers("ammo_9mm"));
    public static final DeferredItem<AttachmentItem> BT_IMPULS = attachment("bt_impuls",
            sil().damage(0.92f).recoil(0.9f).only(AttachmentItem.Compat.PISTOL).calibers("ammo_9mm", "ammo_45acp"));
    public static final DeferredItem<AttachmentItem> DEAD_AIR_WOLFMAN = attachment("dead_air_wolfman",
            sil().volume(0.1f).damage(0.92f).calibers("ammo_9mm"));
    public static final DeferredItem<AttachmentItem> HYBRID_46 = attachment("hybrid_46",
            sil().damage(0.9f).calibers("ammo_9mm", "ammo_45acp", "ammo_357", "ammo_57", "ammo_46", "ammo_762x25", "ammo_556"));
    public static final DeferredItem<AttachmentItem> OBSIDIAN_45 = attachment("obsidian_45",
            sil().damage(0.93f).recoil(0.95f).calibers("ammo_9mm", "ammo_45acp", "ammo_357"));
    public static final DeferredItem<AttachmentItem> TI_RANT_45 = attachment("ti_rant_45",
            sil().damage(0.92f).draw(0.9f).calibers("ammo_45acp", "ammo_9mm"));
    public static final DeferredItem<AttachmentItem> SOCOM556 = attachment("socom556", sil().damage(0.92f).recoil(0.9f).calibers("ammo_556"));
    public static final DeferredItem<AttachmentItem> HUXWRX_FLOW = attachment("huxwrx_flow", sil().damage(0.95f).calibers("ammo_556"));
    public static final DeferredItem<AttachmentItem> BT_ROTEX = attachment("bt_rotex", sil().damage(0.92f).spread(0.95f).calibers("ammo_556"));
    public static final DeferredItem<AttachmentItem> SANDMAN_S = attachment("sandman_s",
            sil().volume(0.1f).damage(0.9f).calibers("ammo_556", "ammo_762", "ammo_545"));
    public static final DeferredItem<AttachmentItem> PBS1 = attachment("pbs1", sil().damage(0.9f).guns("ak47"));
    public static final DeferredItem<AttachmentItem> PBS4 = attachment("pbs4", sil().damage(0.9f).recoil(0.9f).guns("ak74", "an94"));
    public static final DeferredItem<AttachmentItem> TGP_A = attachment("tgp_a", sil().damage(0.93f).guns("ak47", "ak74", "an94", "pkm"));
    public static final DeferredItem<AttachmentItem> SURGE_762 = attachment("surge_762",
            sil().volume(0.12f).damage(0.92f).calibers("ammo_762", "ammo_762x54r", "ammo_556"));
    public static final DeferredItem<AttachmentItem> SOCOM762 = attachment("socom762", sil().damage(0.95f).calibers("ammo_762", "ammo_556"));
    public static final DeferredItem<AttachmentItem> AAC_SDN6 = attachment("aac_sdn6",
            sil().damage(0.92f).aimSpread(0.9f).calibers("ammo_762", "ammo_556"));
    public static final DeferredItem<AttachmentItem> OMEGA_300 = attachment("omega_300",
            sil().damage(0.92f).calibers("ammo_556", "ammo_545", "ammo_762", "ammo_762x54r", "ammo_792", "ammo_338"));
    public static final DeferredItem<AttachmentItem> THUNDER_BEAST = attachment("thunder_beast",
            sil().damage(0.95f).recoil(0.85f).calibers("ammo_762", "ammo_762x54r", "ammo_792", "ammo_338"));
    public static final DeferredItem<AttachmentItem> INTEGRAL_SD = attachment("integral_sd",
            sil().volume(0.05f).damage(0.9f).mobility(0.9f).only(AttachmentItem.Compat.SMG));
    public static final DeferredItem<AttachmentItem> BOTTLE_SILENCER = attachment("bottle_silencer", sil().spread(1.15f).wears(40));
    public static final DeferredItem<AttachmentItem> RUBY_SILENCER = attachment("ruby_silencer", sil().damage(1.05f));
    public static final DeferredItem<AttachmentItem> ECHO_SILENCER = attachment("echo_silencer",
            sil().volume(0.0f).damage(0.92f).special(AttachmentItem.Special.ECHO));
    public static final DeferredItem<AttachmentItem> OBSIDIAN_SILENCER = attachment("obsidian_silencer",
            sil().damage(0.92f).recoil(0.75f).mobility(0.9f));
    public static final DeferredItem<AttachmentItem> OSS_HELIX = attachment("oss_helix",
            sil().damage(0.93f).aimSpeed(1.15f).calibers("ammo_556", "ammo_762", "ammo_545", "ammo_762x54r"));

    // ---------- Grosses lunettes (fusils de précision uniquement) ----------
    public static final DeferredItem<AttachmentItem> LEUPOLD_X6 = attachment("leupold_x6",
            sight(0.166f, SightOverlay.SCOPE_X6).aimSpread(0.3f).aimSpeed(0.85f).only(AttachmentItem.Compat.SNIPER));
    public static final DeferredItem<AttachmentItem> SB_X12 = attachment("sb_x12",
            sight(0.083f, SightOverlay.SCOPE_X12).aimSpread(0.2f).aimSpeed(0.75f).only(AttachmentItem.Compat.SNIPER));
    public static final DeferredItem<AttachmentItem> NIGHTFORCE_X20 = attachment("nightforce_x20",
            sight(0.05f, SightOverlay.SNIPER).aimSpread(0.15f).aimSpeed(0.65f).only(AttachmentItem.Compat.SNIPER));
    public static final DeferredItem<AttachmentItem> SB_PM2_5_25 = attachment("sb_pm2_5_25",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).aimSpread(0.15f).aimSpeed(0.7f).only(AttachmentItem.Compat.SNIPER).sights(
                    new SightMode(0.2f, SightOverlay.SCOPE_X12), new SightMode(0.1f, SightOverlay.SCOPE_X12),
                    new SightMode(0.0667f, SightOverlay.SCOPE_X12), new SightMode(0.05f, SightOverlay.SCOPE_X12),
                    new SightMode(0.04f, SightOverlay.SCOPE_X12)));

    private static AttachmentItem.Stats launcher() {
        return fg().mobility(0.85f).aimSpeed(0.85f).special(AttachmentItem.Special.UNDERBARREL_GRENADE);
    }
    private static AttachmentItem.Stats laser(int color) {
        return AttachmentItem.Stats.of(AttachmentSlot.SIDE_RAIL).hipSpread(0.6f).laser(color).only(AttachmentItem.Compat.ASSAULT);
    }
    private static AttachmentItem.Stats sil() { return AttachmentItem.Stats.of(AttachmentSlot.MUZZLE).silencer(); }

    private static AttachmentItem.Stats fg() { return AttachmentItem.Stats.of(AttachmentSlot.GRIP); }
    private static AttachmentItem.Stats pg() { return AttachmentItem.Stats.of(AttachmentSlot.PISTOL_GRIP); }
    private static AttachmentItem.Stats st() { return AttachmentItem.Stats.of(AttachmentSlot.STOCK).only(AttachmentItem.Compat.LONG); }
    private static AttachmentItem.Stats sight(float zoom, SightOverlay overlay) {
        return AttachmentItem.Stats.of(AttachmentSlot.SIGHT).sight(zoom, overlay);
    }

    // ---------- Tables ----------
    public static final DeferredItem<BlockItem> ARMAMENT_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.ARMAMENT_TABLE);
    public static final DeferredItem<BlockItem> MODIFICATION_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.MODIFICATION_TABLE);

    private static DeferredItem<GunItem> gun(String name, Rarity rarity, GunStats.Builder stats) {
        return ITEMS.register(name, () -> new GunItem(stats.build(), new Item.Properties().rarity(rarity)));
    }

    private static DeferredItem<Item> ammo(String name) {
        return ITEMS.register(name, () -> new AmmoItem(false, new Item.Properties()));
    }

    private static DeferredItem<AttachmentItem> attachment(String name, AttachmentItem.Stats stats) {
        return ITEMS.register(name, () -> new AttachmentItem(stats, new Item.Properties()));
    }

    private ModItems() {}
}

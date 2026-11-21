package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.AmmoItem;
import fr.modeminecraft.item.EnderBladeItem;
import fr.modeminecraft.item.FireStaffItem;
import fr.modeminecraft.item.HelmetItem;
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
    public static final DeferredItem<BlockItem> TRAFFIC_LIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.TRAFFIC_LIGHT);
    public static final DeferredItem<BlockItem> FIRE_HYDRANT = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_HYDRANT);
    public static final DeferredItem<BlockItem> FIRE_EXTINGUISHER = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_EXTINGUISHER);
    public static final DeferredItem<BlockItem> METAL_DETECTOR = ITEMS.registerSimpleBlockItem(ModBlocks.METAL_DETECTOR);
    public static final DeferredItem<BlockItem> CITY_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.CITY_LAMP);
    public static final DeferredItem<BlockItem> CITY_NEON = ITEMS.registerSimpleBlockItem(ModBlocks.CITY_NEON);
    public static final DeferredItem<BlockItem> STREET_LANTERN = ITEMS.registerSimpleBlockItem(ModBlocks.STREET_LANTERN);
    public static final DeferredItem<BlockItem> HIGH_VOLTAGE_TRANSFORMER = ITEMS.registerSimpleBlockItem(ModBlocks.HIGH_VOLTAGE_TRANSFORMER);
    public static final DeferredItem<BlockItem> TENT_CANVAS = ITEMS.registerSimpleBlockItem(ModBlocks.TENT_CANVAS);
    public static final DeferredItem<BlockItem> TENT_CANVAS_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.TENT_CANVAS_STAIRS);
    public static final DeferredItem<BlockItem> SLEEPING_BAG = ITEMS.registerSimpleBlockItem(ModBlocks.SLEEPING_BAG);
    public static final DeferredItem<BlockItem> POLICE_LOCKER = ITEMS.registerSimpleBlockItem(ModBlocks.POLICE_LOCKER);
    public static final DeferredItem<Item> LOCKED_WOODEN_DOOR = ITEMS.register("locked_wooden_door",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.LOCKED_WOODEN_DOOR.get(), new Item.Properties()));
    public static final DeferredItem<Item> LOCKED_METAL_DOOR = ITEMS.register("locked_metal_door",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.LOCKED_METAL_DOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SLIDING_JAIL_DOOR = ITEMS.registerSimpleBlockItem(ModBlocks.SLIDING_JAIL_DOOR);
    public static final DeferredItem<BlockItem> PRISON_BUNK_BED = ITEMS.registerSimpleBlockItem(ModBlocks.PRISON_BUNK_BED);
    public static final DeferredItem<BlockItem> PRISON_TOILET_SINK = ITEMS.registerSimpleBlockItem(ModBlocks.PRISON_TOILET_SINK);
    public static final DeferredItem<BlockItem> PRISON_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.PRISON_BENCH);
    public static final DeferredItem<BlockItem> CELL_CONTROL_PANEL = ITEMS.registerSimpleBlockItem(ModBlocks.CELL_CONTROL_PANEL);
    public static final DeferredItem<BlockItem> SURVEILLANCE_MONITOR = ITEMS.registerSimpleBlockItem(ModBlocks.SURVEILLANCE_MONITOR);
    public static final DeferredItem<BlockItem> CASH_REGISTER = ITEMS.registerSimpleBlockItem(ModBlocks.CASH_REGISTER);
    public static final DeferredItem<BlockItem> AMBIENT_SPEAKER = ITEMS.registerSimpleBlockItem(ModBlocks.AMBIENT_SPEAKER);
    public static final DeferredItem<BlockItem> NEON_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.NEON_SIGN);
    public static final DeferredItem<BlockItem> NEON_SIGN_CYAN = ITEMS.registerSimpleBlockItem(ModBlocks.NEON_SIGN_CYAN);
    public static final DeferredItem<BlockItem> GIANT_SCREEN = ITEMS.registerSimpleBlockItem(ModBlocks.GIANT_SCREEN);
    public static final DeferredItem<BlockItem> STEAM_VENT = ITEMS.registerSimpleBlockItem(ModBlocks.STEAM_VENT);
    public static final DeferredItem<BlockItem> FOUNTAIN_SPOUT = ITEMS.registerSimpleBlockItem(ModBlocks.FOUNTAIN_SPOUT);
    public static final DeferredItem<BlockItem> AC_UNIT = ITEMS.registerSimpleBlockItem(ModBlocks.AC_UNIT);
    public static final DeferredItem<BlockItem> WALL_CLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.WALL_CLOCK);
    public static final DeferredItem<BlockItem> TRAFFIC_CONE = ITEMS.registerSimpleBlockItem(ModBlocks.TRAFFIC_CONE);
    /** Accessoires des scènes de rue : guitare du musicien, bombe de peinture de l'artiste. */
    public static final DeferredItem<Item> ACOUSTIC_GUITAR = ITEMS.registerSimpleItem("acoustic_guitar", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SPRAY_CAN = ITEMS.registerSimpleItem("spray_can", new Item.Properties().stacksTo(16));
    /** Liasse de billets (butin des braquages, monnaie des marchands). */
    public static final DeferredItem<Item> CASH_BUNDLE = ITEMS.registerSimpleItem("cash_bundle", new Item.Properties().stacksTo(64));
    /** Butin des braquages de civils : téléphone, montre (revendables aux commerçants), ration militaire. */
    public static final DeferredItem<Item> SMARTPHONE = ITEMS.registerSimpleItem("smartphone", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> WRISTWATCH = ITEMS.registerSimpleItem("wristwatch", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> MILITARY_RATION = ITEMS.registerSimpleItem("military_ration", new Item.Properties().stacksTo(16)
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(10).saturationModifier(0.9f).build()));
    /** Tente de camping pliée (se monte d'un clic droit au sol). */
    public static final DeferredItem<Item> CAMPING_TENT = ITEMS.register("camping_tent",
            () -> new fr.modeminecraft.item.CampingTentItem(new Item.Properties().stacksTo(4)));
    public static final DeferredItem<BlockItem> JAMMER_TOWER = ITEMS.registerSimpleBlockItem(ModBlocks.JAMMER_TOWER);

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
    public static final DeferredItem<Item> AMMO_45COLT = ammo("ammo_45colt");    // Colt Single Action Army
    public static final DeferredItem<Item> AMMO_500SW = ammo("ammo_500sw");      // Smith & Wesson Model 500
    public static final DeferredItem<Item> AMMO_44MAG = ammo("ammo_44mag");      // Taurus Raging Bull
    public static final DeferredItem<Item> AMMO_408 = ammo("ammo_408");          // CheyTac M200
    public static final DeferredItem<Item> LEAD_BALL = ammo("lead_ball");        // pistolet à silex
    public static final DeferredItem<Item> CROSSBOW_BOLT = ammo("crossbow_bolt"); // arbalète (normal / explosif / incendiaire)
    public static final DeferredItem<Item> TASER_CARTRIDGE = ammo("taser_cartridge");
    public static final DeferredItem<Item> RUBY_CELL = ammo("ruby_cell");        // railgun
    public static final DeferredItem<Item> PZF3_WARHEAD = ITEMS.registerSimpleItem("pzf3_warhead", new Item.Properties().stacksTo(8));
    public static final DeferredItem<Item> STINGER_MISSILE = ITEMS.registerSimpleItem("stinger_missile", new Item.Properties().stacksTo(4));
    public static final DeferredItem<Item> JAVELIN_MISSILE = ITEMS.registerSimpleItem("javelin_missile", new Item.Properties().stacksTo(4));
    public static final DeferredItem<Item> PLASMA_BALL = ITEMS.registerSimpleItem("plasma_ball", new Item.Properties());
    public static final DeferredItem<Item> SHOTGUN_SHELL = ITEMS.register("shotgun_shell", // fusils à pompe
            () -> new AmmoItem(true, new Item.Properties()));
    public static final DeferredItem<Item> GRENADE_40MM = ITEMS.registerSimpleItem("grenade_40mm", new Item.Properties().stacksTo(16));
    /** Canon de rechange : pour changer le canon d'une mitrailleuse en surchauffe (touche R). */
    public static final DeferredItem<Item> SPARE_BARREL = ITEMS.registerSimpleItem("spare_barrel", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> RPG_ROCKET = ITEMS.registerSimpleItem("rpg_rocket", new Item.Properties().stacksTo(16));


    // Emplacements d'accessoires selon le type d'arme
    private static final AttachmentSlot SIGHT = AttachmentSlot.SIGHT, MUZZLE = AttachmentSlot.MUZZLE,
            GRIP = AttachmentSlot.GRIP, MAG = AttachmentSlot.MAGAZINE,
            PGRIP = AttachmentSlot.PISTOL_GRIP, STOCK = AttachmentSlot.STOCK,
            RAIL = AttachmentSlot.SIDE_RAIL;

    // Pistolet / pistolet-mitrailleur / fusil : réglages communs
    private static GunStats.Builder pistol() {
        return GunStats.builder().pistolCategory().velocity(4.0f).spread(1.5f, 0.5f).sound(ModSounds.SHOT_PISTOL, 0.4f, 1.0f)
                .slots(SIGHT, MUZZLE, MAG, PGRIP, STOCK);
    }

    private static GunStats.Builder smg() {
        return GunStats.builder().category(GunStats.Category.SMG).automatic().velocity(4.0f).spread(2.2f, 0.9f).recoil(0.9f).sound(ModSounds.SHOT_SMG, 0.35f, 1.0f)
                .draw(7).slots(SIGHT, MUZZLE, GRIP, MAG, PGRIP, STOCK);
    }

    private static GunStats.Builder rifle() {
        return GunStats.builder().category(GunStats.Category.ASSAULT).slideLock().automatic().velocity(4.8f).spread(2.2f, 0.8f).recoil(1.2f)
                .sound(ModSounds.SHOT_RIFLE, 0.4f, 1.0f).slots(SIGHT, MUZZLE, GRIP, MAG, PGRIP, STOCK, RAIL);
    }

    private static GunStats.Builder sniper() {
        return GunStats.builder().category(GunStats.Category.SNIPER).velocity(7.5f).spread(4.0f, 0.0f).sound(ModSounds.SHOT_SNIPER, 0.9f, 1.0f).draw(14)
                .slots(SIGHT, MUZZLE, GRIP, PGRIP, STOCK);
    }

    private static GunStats.Builder shotgun() {
        return GunStats.builder().category(GunStats.Category.SHOTGUN).velocity(3.0f).spread(8.0f, 6.0f).ammo(ModItems.SHOTGUN_SHELL).sound(ModSounds.SHOT_SHOTGUN, 0.7f, 1.0f)
                .slots(SIGHT, MUZZLE, GRIP, PGRIP, STOCK);
    }

    private static GunStats.Builder machineGun() {
        return GunStats.builder().category(GunStats.Category.MACHINE_GUN).automatic().velocity(4.8f).spread(3.2f, 1.4f).sound(ModSounds.SHOT_BATTLE, 0.5f, 1.0f).draw(18).heat(1.3f)
                .slots(SIGHT, MUZZLE, GRIP, PGRIP, STOCK);
    }

    // ================= PISTOLETS =================
    public static final DeferredItem<GunItem> GLOCK17 = gun("glock17", Rarity.COMMON,
            pistol().slideLock().damage(5).magazine(17).fireDelay(3).reload(28).recoil(1.4f).ammo(ModItems.AMMO_9MM)); // semi rapide, recul très faible
    public static final DeferredItem<GunItem> DESERT_EAGLE = gun("desert_eagle", Rarity.UNCOMMON,
            pistol().slideLock().damage(12).magazine(7).fireDelay(9).reload(34).recoil(8.5f).spread(2.0f, 0.6f).ammo(ModItems.AMMO_50AE)
                    .sound(ModSounds.SHOT_MAGNUM, 0.7f, 1.0f));
    public static final DeferredItem<GunItem> BERETTA_M9 = gun("beretta_m9", Rarity.COMMON,
            pistol().slideLock().damage(5.5f).magazine(15).fireDelay(4).reload(30).recoil(2.0f).spread(1.1f, 0.4f).ammo(ModItems.AMMO_9MM)); // précis au jugé
    public static final DeferredItem<GunItem> COLT_M1911 = gun("colt_m1911", Rarity.COMMON,
            pistol().slideLock().damage(8.5f).magazine(7).fireDelay(5).reload(30).recoil(3.5f).ammo(ModItems.AMMO_45ACP)
                    .sound(ModSounds.SHOT_PISTOL, 0.5f, 0.9f));
    /** Revolver : rechargement balle par balle. */
    public static final DeferredItem<GunItem> COLT_PYTHON = gun("colt_python", Rarity.UNCOMMON,
            GunStats.builder().noChamber().casing(GunStats.Casing.NONE).damage(10).magazine(6).fireDelay(7).reloadPerRound(10).velocity(4.5f).spread(1.4f, 0.4f)
                    .recoil(5.5f).ammo(ModItems.AMMO_357).sound(ModSounds.SHOT_MAGNUM, 0.6f, 1.05f).pistolCategory().slots(SIGHT, PGRIP));
    public static final DeferredItem<GunItem> MAKAROV = gun("makarov", Rarity.COMMON,
            pistol().slideLock().damage(4.5f).magazine(8).fireDelay(4).reload(26).recoil(2).ammo(ModItems.AMMO_9MM)); // arme de début

    // ================= PISTOLETS-MITRAILLEURS =================
    public static final DeferredItem<GunItem> MP5 = gun("mp5", Rarity.UNCOMMON,
            smg().damage(5).magazine(30).rpm(800).reload(38).spread(1.6f, 0.6f).ammo(ModItems.AMMO_9MM)); // très précis en mouvement
    public static final DeferredItem<GunItem> UZI = gun("uzi", Rarity.COMMON,
            smg().damage(4.5f).magazine(32).rpm(950).reload(36).spread(3.0f, 1.4f).recoil(1.0f).walkSpeed(1.1f).ammo(ModItems.AMMO_9MM)); // léger : +10 % vitesse
    public static final DeferredItem<GunItem> P90 = gun("p90", Rarity.UNCOMMON,
            smg().damage(5).magazine(50).rpm(900).reload(50).recoil(0.7f).ammo(ModItems.AMMO_57)
                    .noSlots().slots(SIGHT, MUZZLE, PGRIP)); // chargeur sur le dessus, bullpup : ni chargeur ni crosse
    /** MP7 : balles perforantes (ignorent l'armure). */
    public static final DeferredItem<GunItem> MP7 = gun("mp7", Rarity.UNCOMMON,
            smg().slideLock().damage(5).magazine(40).rpm(950).reload(40).velocity(4.6f).spread(2.0f, 0.8f).armorPiercing().ammo(ModItems.AMMO_46));
    public static final DeferredItem<GunItem> THOMPSON = gun("thompson", Rarity.UNCOMMON,
            smg().damage(6).magazine(50).rpm(800).reload(60).spread(3.0f, 1.5f).recoil(1.4f).ammo(ModItems.AMMO_45ACP)
                    .sound(ModSounds.SHOT_SMG, 0.45f, 0.85f));
    public static final DeferredItem<GunItem> VECTOR = gun("vector", Rarity.RARE,
            smg().slideLock().damage(4.5f).magazine(25).rpm(1100).reload(40).spread(2.0f, 0.8f).recoil(0.25f).ammo(ModItems.AMMO_45ACP)); // recul vertical quasi nul
    public static final DeferredItem<GunItem> PPSH41 = gun("ppsh41", Rarity.UNCOMMON,
            smg().casing(GunStats.Casing.STEEL).damage(4.8f).magazine(71).rpm(1000).reload(70).spread(3.5f, 1.8f).recoil(1.0f).sound(ModSounds.SHOT_SMG, 0.4f, 1.1f).ammo(ModItems.AMMO_762X25));

    // ================= FUSILS D'ASSAUT =================
    public static final DeferredItem<GunItem> AK47 = gun("ak47", Rarity.UNCOMMON,
            rifle().slideLock(false).casing(GunStats.Casing.STEEL).damage(10).magazine(30).rpm(600).reload(50).spread(3.0f, 1.2f).recoil(2.8f).ammo(ModItems.AMMO_762)
                    .sound(ModSounds.SHOT_AK, 0.45f, 1.0f));
    public static final DeferredItem<GunItem> AK74 = gun("ak74", Rarity.UNCOMMON,
            rifle().slideLock(false).casing(GunStats.Casing.STEEL).damage(7.8f).magazine(30).rpm(650).reload(48).spread(2.4f, 0.9f).recoil(1.4f).sound(ModSounds.SHOT_AK, 0.42f, 1.1f).ammo(ModItems.AMMO_545));
    public static final DeferredItem<GunItem> M4 = gun("m4", Rarity.UNCOMMON,
            rifle().damage(7.5f).magazine(30).rpm(800).reload(40).velocity(5.0f).spread(2.0f, 0.7f).recoil(0.9f)
                    .ammo(ModItems.AMMO_556).sound(ModSounds.SHOT_RIFLE, 0.38f, 1.0f));
    public static final DeferredItem<GunItem> HK416 = gun("hk416", Rarity.RARE,
            rifle().damage(7.8f).magazine(30).rpm(850).reload(40).velocity(5.5f).spread(1.6f, 0.5f).recoil(1.0f)
                    .ammo(ModItems.AMMO_556));
    public static final DeferredItem<GunItem> FAMAS = gun("famas", Rarity.UNCOMMON,
            rifle().modes(FireMode.SEMI, FireMode.BURST).damage(7).magazine(25).rpm(850).burst(3, 7).reload(42).velocity(5.0f).spread(1.8f, 0.5f).recoil(0.9f)
                    .ammo(ModItems.AMMO_556).sound(ModSounds.SHOT_RIFLE, 0.35f, 1.08f).without(STOCK));
    public static final DeferredItem<GunItem> SCAR_H = gun("scar_h", Rarity.RARE,
            rifle().damage(11).magazine(20).rpm(600).reload(45).velocity(5.4f).spread(2.0f, 0.6f).recoil(2.2f).ammo(ModItems.AMMO_762)
                    .sound(ModSounds.SHOT_BATTLE, 0.5f, 1.0f));
    /** G36 : lunette intégrée (pas d'emplacement viseur). */
    public static final DeferredItem<GunItem> G36 = gun("g36", Rarity.UNCOMMON,
            rifle().damage(7.2f).magazine(30).rpm(750).reload(42).spread(2.0f, 0.6f).recoil(1.0f).ammo(ModItems.AMMO_556)
                    .scope(0.4f, SightOverlay.SCOPE_X4).noSlots().slots(MUZZLE, GRIP, MAG, PGRIP, STOCK, RAIL));
    /** Steyr AUG : bullpup avec lunette intégrée. */
    public static final DeferredItem<GunItem> AUG = gun("aug", Rarity.UNCOMMON,
            rifle().damage(7.4f).magazine(30).rpm(750).reload(44).spread(1.8f, 0.5f).recoil(0.75f).ammo(ModItems.AMMO_556)
                    .scope(0.667f, SightOverlay.SCOPE_X4).noSlots().slots(MUZZLE, GRIP, MAG, PGRIP, RAIL)); // bullpup : pas de crosse
    public static final DeferredItem<GunItem> TAVOR = gun("tavor", Rarity.UNCOMMON,
            rifle().damage(7.2f).magazine(30).rpm(850).reload(40).spread(2.0f, 0.8f).recoil(1.0f).ammo(ModItems.AMMO_556).without(STOCK));
    /** M16A4 : coup par coup ou rafale de 3 (pas d'automatique). */
    public static final DeferredItem<GunItem> M16A4 = gun("m16a4", Rarity.UNCOMMON,
            rifle().modes(FireMode.SEMI, FireMode.BURST).damage(7.6f).magazine(30).rpm(800).burst(3, 8).reload(42).velocity(5.2f).spread(1.6f, 0.5f)
                    .recoil(1.0f).ammo(ModItems.AMMO_556));
    public static final DeferredItem<GunItem> G3 = gun("g3", Rarity.UNCOMMON,
            rifle().damage(11).magazine(20).rpm(550).reload(48).spread(2.5f, 0.9f).recoil(2.5f).ammo(ModItems.AMMO_762)
                    .sound(ModSounds.SHOT_BATTLE, 0.5f, 0.97f));
    /** AN-94 : rafale de 2 coups ultra-rapide (ou coup par coup, ou automatique). */
    public static final DeferredItem<GunItem> AN94 = gun("an94", Rarity.RARE,
            rifle().slideLock(false).casing(GunStats.Casing.STEEL).damage(8).magazine(30).rpm(600).burst(2, 6).reload(46).modes(FireMode.SEMI, FireMode.BURST, FireMode.AUTO).spread(1.6f, 0.4f).recoil(0.8f)
                    .sound(ModSounds.SHOT_AK, 0.42f, 1.12f).ammo(ModItems.AMMO_545));

    // ================= FUSILS DE PRÉCISION =================
    /** Barrett M82 : traverse jusqu'à 3 ennemis. */
    public static final DeferredItem<GunItem> BARRETT_M82 = gun("barrett_m82", Rarity.EPIC,
            sniper().slideLock().damage(38).magazine(10).fireDelay(10).reload(80).velocity(8.5f).recoil(12).pierce(3)
                    .scope(0.15f, SightOverlay.SNIPER).ammo(ModItems.AMMO_50BMG).sound(ModSounds.SHOT_HEAVY, 1.2f, 1.0f));
    public static final DeferredItem<GunItem> AWM = gun("awm", Rarity.EPIC,
            sniper().damage(32).magazine(5).fireDelay(30).reload(70).velocity(8.5f).recoil(9)
                    .scope(0.15f, SightOverlay.SNIPER).ammo(ModItems.AMMO_338).sound(ModSounds.SHOT_SNIPER, 1.0f, 0.95f));
    public static final DeferredItem<GunItem> DRAGUNOV = gun("dragunov", Rarity.RARE,
            sniper().casing(GunStats.Casing.STEEL).damage(20).magazine(10).fireDelay(8).reload(55).recoil(5)
                    .scope(0.25f, SightOverlay.SNIPER).sound(ModSounds.SHOT_AK, 0.8f, 0.85f).ammo(ModItems.AMMO_762X54R));
    /** Mosin-Nagant : viseur métallique, rechargement balle par balle. */
    public static final DeferredItem<GunItem> MOSIN_NAGANT = gun("mosin_nagant", Rarity.COMMON,
            sniper().casing(GunStats.Casing.STEEL).damage(22).magazine(5).fireDelay(30).reloadPerRound(12).spread(3.0f, 0.3f).recoil(6)
                    .sound(ModSounds.SHOT_SNIPER, 0.85f, 1.05f).ammo(ModItems.AMMO_762X54R).noSlots().slots(SIGHT, MUZZLE));
    public static final DeferredItem<GunItem> KAR98K = gun("kar98k", Rarity.COMMON,
            sniper().damage(21).magazine(5).fireDelay(28).reloadPerRound(12).spread(3.0f, 0.3f).recoil(6).draw(10)
                    .ammo(ModItems.AMMO_792).noSlots().slots(SIGHT, MUZZLE));
    /** M14 EBR : fusil de tireur d'élite semi-automatique. */
    public static final DeferredItem<GunItem> M14_EBR = gun("m14_ebr", Rarity.RARE,
            sniper().slideLock().damage(18).magazine(20).fireDelay(7).reload(50).velocity(6.0f).spread(2.5f, 0.3f).recoil(3.5f)
                    .ammo(ModItems.AMMO_762).noSlots().slots(SIGHT, MUZZLE, GRIP, MAG, PGRIP, STOCK));

    // ================= FUSILS À POMPE =================
    public static final DeferredItem<GunItem> REMINGTON_870 = gun("remington_870", Rarity.COMMON,
            shotgun().casing(GunStats.Casing.SHELL).damage(2.5f).pellets(8).magazine(6).fireDelay(18).reloadPerRound(10).spread(5.5f, 4.0f).recoil(6)); // dispersion serrée
    public static final DeferredItem<GunItem> BENELLI_M4 = gun("benelli_m4", Rarity.UNCOMMON,
            shotgun().casing(GunStats.Casing.SHELL).damage(2.4f).pellets(8).magazine(7).fireDelay(6).reloadPerRound(9).recoil(5)); // semi-auto tactique
    public static final DeferredItem<GunItem> SPAS12 = gun("spas12", Rarity.UNCOMMON,
            shotgun().casing(GunStats.Casing.SHELL).damage(2.2f).pellets(8).magazine(8).fireDelay(7).reloadPerRound(10).recoil(6.5f)); // semi-automatique
    /** AA-12 : fusil à pompe automatique avec camembert. */
    public static final DeferredItem<GunItem> AA12 = gun("aa12", Rarity.RARE,
            shotgun().casing(GunStats.Casing.SHELL).damage(1.8f).pellets(8).magazine(20).rpm(300).automatic().reload(60).spread(9.0f, 7.0f).recoil(2)
                    .noSlots().slots(SIGHT, GRIP, PGRIP, STOCK));
    public static final DeferredItem<GunItem> SAWED_OFF = gun("sawed_off", Rarity.COMMON,
            shotgun().noChamber().casing(GunStats.Casing.NONE).damage(2.5f).pellets(8).magazine(2).fireDelay(4).reload(40).spread(14.0f, 12.0f).recoil(9).doubleBarrel()
                    .sound(ModSounds.SHOT_SHOTGUN, 0.9f, 0.9f).noSlots());

    // ================= MITRAILLEUSES =================
    public static final DeferredItem<GunItem> M249 = gun("m249", Rarity.RARE,
            machineGun().damage(8).magazine(200).rpm(850).reload(100).recoil(1.0f).walkSpeed(0.85f).sound(ModSounds.SHOT_RIFLE, 0.5f, 0.95f).ammo(ModItems.AMMO_556));
    public static final DeferredItem<GunItem> PKM = gun("pkm", Rarity.RARE,
            machineGun().casing(GunStats.Casing.STEEL).damage(10.5f).magazine(100).rpm(650).reload(110).walkSpeed(0.85f).recoil(1.6f).sound(ModSounds.SHOT_AK, 0.55f, 0.9f).ammo(ModItems.AMMO_762X54R));
    public static final DeferredItem<GunItem> M60 = gun("m60", Rarity.RARE,
            machineGun().damage(11).magazine(100).rpm(650).reload(120).walkSpeed(0.85f).spread(3.5f, 1.6f).recoil(2.2f).ammo(ModItems.AMMO_762));
    /** Minigun : temps de lancement d'une seconde, puis 20 balles par seconde. */
    public static final DeferredItem<GunItem> MINIGUN = gun("minigun", Rarity.EPIC,
            machineGun().noChamber().damage(6).magazine(250).rpm(2000).spinUp(12).reload(160).walkSpeed(0.75f).noSprint().spread(4.0f, 3.0f).recoil(0.4f)
                    .sound(ModSounds.SHOT_MINIGUN, 0.6f, 1.0f).ammo(ModItems.AMMO_762).draw(25).heat(0.6f).noSlots());

    // ================= LANCEURS =================
    public static final DeferredItem<GunItem> RPG7 = gun("rpg7", Rarity.EPIC, GunStats.builder().noChamber()
            .magazine(1).fireDelay(20).reload(70).velocity(2.2f).spread(0.6f, 0.15f).recoil(10).rocket(4.5f)
            .ammo(ModItems.RPG_ROCKET).sound(ModSounds.SHOT_ROCKET, 1.6f, 1.0f).draw(16).slots(SIGHT));
    /** M32 MGL : lance-grenades à barillet de 6, rechargement grenade par grenade. */
    public static final DeferredItem<GunItem> M32_MGL = gun("m32_mgl", Rarity.EPIC, GunStats.builder().noChamber()
            .magazine(6).fireDelay(10).reloadPerRound(14).velocity(1.6f).spread(1.0f, 0.4f).recoil(5).grenade(2.2f)
            .ammo(ModItems.GRENADE_40MM).sound(ModSounds.SHOT_LAUNCHER, 1.0f, 1.0f).slots(SIGHT));
    public static final DeferredItem<GunItem> M79 = gun("m79", Rarity.RARE, GunStats.builder().noChamber()
            .magazine(1).fireDelay(10).reload(35).velocity(1.6f).spread(1.0f, 0.4f).recoil(6).grenade(2.0f)
            .ammo(ModItems.GRENADE_40MM).sound(ModSounds.SHOT_LAUNCHER, 1.0f, 0.95f));
    /** AT4 : lance-roquettes jetable, vendu chargé. */
    public static final DeferredItem<GunItem> AT4 = gun("at4", Rarity.EPIC, GunStats.builder().noChamber()
            .magazine(1).fireDelay(20).velocity(2.6f).spread(0.5f, 0.1f).recoil(12).rocket(5.0f).disposable()
            .ammo(ModItems.RPG_ROCKET).sound(ModSounds.SHOT_ROCKET, 1.8f, 0.9f));

    // ================= CAHIER DES CHARGES : NOUVELLES ARMES =================
    private static final GunStats.Special S_NONE = GunStats.Special.NONE;
    private static GunStats.Builder revolverB() {
        return GunStats.builder().pistolCategory().noChamber().casing(GunStats.Casing.NONE).velocity(4.5f).spread(1.4f, 0.4f)
                .sound(ModSounds.SHOT_MAGNUM, 0.6f, 1.0f).slots(SIGHT, PGRIP);
    }
    public static final DeferredItem<GunItem> SIG_P226 = gun("sig_p226", Rarity.COMMON,
            pistol().slideLock().damage(6).magazine(15).fireDelay(4).reload(24).recoil(2.0f).ammo(ModItems.AMMO_9MM)); // rechargement éclair
    public static final DeferredItem<GunItem> USP_S = gun("usp_s", Rarity.UNCOMMON,
            pistol().slideLock().damage(6.5f).magazine(12).fireDelay(4).reload(30).recoil(2.2f).ammo(ModItems.AMMO_45ACP)
                    .special(GunStats.Special.INTEGRATED_SILENCER).without(MUZZLE));
    public static final DeferredItem<GunItem> FIVE_SEVEN = gun("five_seven", Rarity.UNCOMMON,
            pistol().slideLock().damage(4.5f).magazine(20).fireDelay(3).reload(30).recoil(1.5f).velocity(4.6f).armorPiercing()
                    .ammo(ModItems.AMMO_57));
    public static final DeferredItem<GunItem> CZ75_AUTO = gun("cz75_auto", Rarity.UNCOMMON,
            pistol().slideLock().automatic().rpm(1100).damage(4.5f).magazine(12).reload(28).recoil(2.6f).spread(2.2f, 0.9f)
                    .ammo(ModItems.AMMO_9MM).without(MUZZLE));
    public static final DeferredItem<GunItem> LUGER_P08 = gun("luger_p08", Rarity.UNCOMMON,
            pistol().slideLock().damage(5.5f).magazine(8).fireDelay(5).reload(32).spread(1.0f, 0.3f).recoil(2.0f)
                    .ammo(ModItems.AMMO_9MM).noSlots().slots(SIGHT, PGRIP));
    public static final DeferredItem<GunItem> FLINTLOCK = gun("flintlock", Rarity.COMMON,
            GunStats.builder().pistolCategory().noChamber().casing(GunStats.Casing.NONE).damage(14).magazine(1).fireDelay(10)
                    .reload(70).velocity(3.0f).spread(3.0f, 1.5f).recoil(6).falloff(0.06f).special(GunStats.Special.BLACK_POWDER)
                    .ammo(ModItems.LEAD_BALL).sound(ModSounds.SHOT_MAGNUM, 0.8f, 0.7f).draw(8));
    public static final DeferredItem<GunItem> SW_MODEL_500 = gun("sw_model_500", Rarity.RARE,
            revolverB().damage(15).magazine(5).fireDelay(10).reloadPerRound(11).velocity(4.6f).recoil(11).ammo(ModItems.AMMO_500SW)
                    .sound(ModSounds.SHOT_MAGNUM, 0.9f, 0.85f));
    public static final DeferredItem<GunItem> COLT_SAA = gun("colt_saa", Rarity.UNCOMMON,
            revolverB().damage(8).magazine(6).fireDelay(8).reloadPerRound(12).recoil(4).ammo(ModItems.AMMO_45COLT)
                    .special(GunStats.Special.FANNING).sound(ModSounds.SHOT_MAGNUM, 0.6f, 1.1f).noSlots().slots(PGRIP));
    public static final DeferredItem<GunItem> MATEBA_UNICA = gun("mateba_unica", Rarity.RARE,
            revolverB().damage(9.5f).magazine(6).fireDelay(7).reloadPerRound(10).recoil(3.3f).ammo(ModItems.AMMO_357)); // recul vertical -40 %
    public static final DeferredItem<GunItem> RAGING_BULL = gun("raging_bull", Rarity.UNCOMMON,
            revolverB().damage(11).magazine(6).fireDelay(8).reloadPerRound(10).velocity(4.9f).recoil(6.5f).ammo(ModItems.AMMO_44MAG));
    public static final DeferredItem<GunItem> TAURUS_JUDGE = gun("taurus_judge", Rarity.UNCOMMON,
            revolverB().damage(2.6f).pellets(5).magazine(5).fireDelay(8).reloadPerRound(10).spread(6.0f, 5.0f).recoil(6)
                    .ammo(ModItems.SHOTGUN_SHELL).sound(ModSounds.SHOT_SHOTGUN, 0.7f, 1.1f));
    public static final DeferredItem<GunItem> GLOCK18 = gun("glock18", Rarity.RARE,
            pistol().slideLock().automatic().rpm(1200).damage(5).magazine(33).reload(32).recoil(1.8f).spread(2.4f, 1.0f)
                    .ammo(ModItems.AMMO_9MM).without(MAG));
    public static final DeferredItem<GunItem> UMP45 = gun("ump45", Rarity.UNCOMMON,
            smg().damage(6).magazine(25).rpm(800).reload(40).recoil(1.3f).ammo(ModItems.AMMO_45ACP));
    public static final DeferredItem<GunItem> PP19_BIZON = gun("pp19_bizon", Rarity.UNCOMMON,
            smg().damage(4.5f).magazine(64).rpm(800).reload(55).ammo(ModItems.AMMO_9MM).noSlots().slots(SIGHT, MUZZLE, PGRIP, STOCK));
    public static final DeferredItem<GunItem> MAC10 = gun("mac10", Rarity.COMMON,
            smg().damage(4.5f).magazine(30).rpm(1100).reload(36).spread(3.6f, 1.8f).recoil(1.3f).ammo(ModItems.AMMO_45ACP)
                    .noSlots().slots(SIGHT, MUZZLE, STOCK));
    public static final DeferredItem<GunItem> SCORPION_EVO = gun("scorpion_evo", Rarity.UNCOMMON,
            smg().damage(4.6f).magazine(30).rpm(1050).reload(28).ammo(ModItems.AMMO_9MM));
    public static final DeferredItem<GunItem> AK74U = gun("ak74u", Rarity.UNCOMMON,
            rifle().slideLock(false).casing(GunStats.Casing.STEEL).damage(7.2f).magazine(30).rpm(700).reload(42).velocity(4.0f)
                    .spread(2.6f, 1.1f).recoil(1.6f).falloff(0.012f).draw(6).sound(ModSounds.SHOT_AK, 0.42f, 1.15f).ammo(ModItems.AMMO_545));
    public static final DeferredItem<GunItem> GALIL_ACE = gun("galil_ace", Rarity.UNCOMMON,
            rifle().slideLock(false).casing(GunStats.Casing.STEEL).damage(9.5f).magazine(35).rpm(650).reload(48).spread(2.4f, 0.9f)
                    .recoil(1.7f).sound(ModSounds.SHOT_AK, 0.45f, 1.02f).ammo(ModItems.AMMO_762));
    public static final DeferredItem<GunItem> FN_FAL = gun("fn_fal", Rarity.RARE,
            rifle().modes(FireMode.SEMI).damage(11.5f).magazine(20).fireDelay(4).reload(46).velocity(5.4f).recoil(3.0f)
                    .sound(ModSounds.SHOT_BATTLE, 0.5f, 1.0f).ammo(ModItems.AMMO_762));
    public static final DeferredItem<GunItem> AK12 = gun("ak12", Rarity.RARE,
            rifle().slideLock(false).casing(GunStats.Casing.STEEL).damage(8).magazine(30).rpm(700).reload(44).spread(2.0f, 0.7f)
                    .recoil(1.3f).sound(ModSounds.SHOT_AK, 0.42f, 1.08f).ammo(ModItems.AMMO_545));
    public static final DeferredItem<GunItem> M1_GARAND = gun("m1_garand", Rarity.UNCOMMON,
            rifle().modes(FireMode.SEMI).damage(12).magazine(8).fireDelay(4).reload(40).velocity(5.6f).recoil(3.2f)
                    .special(GunStats.Special.EN_BLOC).sound(ModSounds.SHOT_BATTLE, 0.5f, 1.04f).ammo(ModItems.AMMO_762)
                    .noSlots().slots(MUZZLE, GRIP));
    public static final DeferredItem<GunItem> MOSSBERG_590 = gun("mossberg_590", Rarity.COMMON,
            shotgun().casing(GunStats.Casing.SHELL).damage(2.5f).pellets(8).magazine(8).fireDelay(18).reloadPerRound(10).recoil(6));
    public static final DeferredItem<GunItem> M24 = gun("m24", Rarity.RARE,
            sniper().damage(24).magazine(5).fireDelay(28).reloadPerRound(14).velocity(8.0f).recoil(7).noDrop(100)
                    .scope(0.15f, SightOverlay.SNIPER).sound(ModSounds.SHOT_SNIPER, 0.95f, 1.05f).ammo(ModItems.AMMO_762));
    public static final DeferredItem<GunItem> CHEYTAC_M200 = gun("cheytac_m200", Rarity.EPIC,
            sniper().damage(34).magazine(7).fireDelay(34).reload(75).velocity(10.0f).recoil(10).bulletLife(120).pierce(2)
                    .scope(0.1f, SightOverlay.SNIPER).sound(ModSounds.SHOT_HEAVY, 1.1f, 1.1f).ammo(ModItems.AMMO_408));
    public static final DeferredItem<GunItem> M72_LAW = gun("m72_law", Rarity.RARE, GunStats.builder().noChamber()
            .magazine(1).fireDelay(20).velocity(3.2f).spread(0.5f, 0.1f).recoil(9).rocket(3.5f).disposable()
            .ammo(ModItems.RPG_ROCKET).sound(ModSounds.SHOT_ROCKET, 1.6f, 1.1f).draw(12));
    public static final DeferredItem<GunItem> PANZERFAUST3 = gun("panzerfaust3", Rarity.EPIC, GunStats.builder().noChamber()
            .magazine(1).fireDelay(20).reload(80).velocity(2.4f).spread(0.6f, 0.15f).recoil(11).rocket(1.5f).directDamage(50)
            .ammo(ModItems.PZF3_WARHEAD).sound(ModSounds.SHOT_ROCKET, 1.7f, 0.9f).draw(16).slots(SIGHT));
    public static final DeferredItem<GunItem> STINGER = gun("stinger", Rarity.EPIC, GunStats.builder().noChamber()
            .magazine(1).fireDelay(20).reload(80).velocity(2.6f).spread(0.4f, 0.1f).recoil(8).missile(3.5f)
            .special(GunStats.Special.LOCK_ON).ammo(ModItems.STINGER_MISSILE).sound(ModSounds.SHOT_ROCKET, 1.6f, 1.2f).draw(16));
    public static final DeferredItem<GunItem> JAVELIN = gun("javelin", Rarity.EPIC, GunStats.builder().noChamber()
            .magazine(1).fireDelay(20).reload(100).velocity(2.0f).spread(0.4f, 0.1f).recoil(8).missile(5.0f)
            .special(GunStats.Special.LOCK_ON_TOP_ATTACK).ammo(ModItems.JAVELIN_MISSILE).sound(ModSounds.SHOT_ROCKET, 1.8f, 0.85f).draw(20));
    // ---- Armes spéciales ----
    public static final DeferredItem<GunItem> TACTICAL_CROSSBOW = gun("tactical_crossbow", Rarity.RARE,
            GunStats.builder().category(GunStats.Category.SNIPER).noChamber().casing(GunStats.Casing.NONE).projectile(GunStats.ProjectileKind.BOLT)
                    .damage(12).magazine(1).fireDelay(10).reload(30).velocity(4.5f).spread(1.0f, 0.2f).recoil(1.5f)
                    .ammo(ModItems.CROSSBOW_BOLT).sound(ModSounds.SHOT_SUPPRESSED, 0.15f, 0.7f).draw(10).slots(SIGHT, GRIP));
    public static final DeferredItem<GunItem> TASER = gun("taser", Rarity.UNCOMMON,
            GunStats.builder().pistolCategory().noChamber().casing(GunStats.Casing.NONE).projectile(GunStats.ProjectileKind.TASER)
                    .damage(1).magazine(1).fireDelay(10).reload(30).recoil(1).ammo(ModItems.TASER_CARTRIDGE)
                    .sound(ModSounds.SHOT_SUPPRESSED, 0.3f, 1.8f));
    public static final DeferredItem<GunItem> RUBY_RAILGUN = gun("ruby_railgun", Rarity.EPIC,
            GunStats.builder().category(GunStats.Category.SNIPER).noChamber().casing(GunStats.Casing.NONE).projectile(GunStats.ProjectileKind.RAIL)
                    .damage(30).magazine(5).fireDelay(20).reload(60).charge(40).recoil(8).ammo(ModItems.RUBY_CELL)
                    .sound(ModSounds.SHOT_HEAVY, 1.0f, 1.6f).draw(16).slots(SIGHT));
    public static final DeferredItem<GunItem> PLASMA_RIFLE = gun("plasma_rifle", Rarity.EPIC,
            GunStats.builder().category(GunStats.Category.ASSAULT).noChamber().casing(GunStats.Casing.NONE).projectile(GunStats.ProjectileKind.PLASMA)
                    .automatic().rpm(400).damage(7).magazine(1).velocity(3.0f).spread(1.5f, 0.6f).recoil(0.8f).heat(2.2f)
                    .special(GunStats.Special.NO_AMMO).ammo(ModItems.PLASMA_BALL).sound(ModSounds.SHOT_SUPPRESSED_HEAVY, 0.5f, 1.8f).slots(SIGHT, RAIL));
    /** VSS Vintorez : fusil de précision silencieux intégré (9x39 subsonique), semi-automatique ou automatique. */
    public static final DeferredItem<GunItem> VSS = gun("vss", Rarity.RARE,
            sniper().slideLock().casing(GunStats.Casing.STEEL).automatic().rpm(700).modes(FireMode.SEMI, FireMode.AUTO).damage(18)
                    .magazine(20).reload(48).velocity(4.2f).spread(2.2f, 0.4f).recoil(2.2f).falloff(0.03f)
                    .scope(0.25f, SightOverlay.SNIPER).special(GunStats.Special.INTEGRATED_SILENCER)
                    .sound(ModSounds.SHOT_SUPPRESSED_HEAVY, 0.25f, 0.95f).ammo(ModItems.AMMO_762).noSlots().slots(MAG, GRIP, RAIL));
    /**
     * Fusil brouilleur anti-drone : faisceau électromagnétique invisible (80 blocs), force tout drone touché
     * à atterrir, désactivé 15 s. Pas de munitions : il chauffe.
     */
    public static final DeferredItem<GunItem> EMP_JAMMER_RIFLE = gun("emp_jammer_rifle", Rarity.EPIC,
            GunStats.builder().category(GunStats.Category.LAUNCHER).noChamber().casing(GunStats.Casing.NONE)
                    .projectile(GunStats.ProjectileKind.EMP).magazine(1).fireDelay(30).recoil(1.5f).heat(34)
                    .special(GunStats.Special.NO_AMMO).ammo(ModItems.PLASMA_BALL).sound(ModSounds.EMP_JAMMER, 1.0f, 1.0f).draw(16).noSlots());
    // ---- Mitrailleuses lourdes à poser ----
    // Tourelles et mitrailleuses lourdes montables (cahier des charges "tourelles")
    public static final DeferredItem<Item> M2_BROWNING = turret("m2_browning", fr.modeminecraft.entity.MountedGunEntity.Kind.M2);
    public static final DeferredItem<Item> DSHK = turret("dshk", fr.modeminecraft.entity.MountedGunEntity.Kind.DSHK);
    public static final DeferredItem<Item> MG42 = turret("mg42", fr.modeminecraft.entity.MountedGunEntity.Kind.MG42);
    public static final DeferredItem<Item> VICKERS = turret("vickers", fr.modeminecraft.entity.MountedGunEntity.Kind.VICKERS);
    public static final DeferredItem<Item> MAXIM_M1910 = turret("maxim_m1910", fr.modeminecraft.entity.MountedGunEntity.Kind.MAXIM);
    public static final DeferredItem<Item> FLAKVIERLING = turret("flakvierling", fr.modeminecraft.entity.MountedGunEntity.Kind.FLAK);
    public static final DeferredItem<Item> MK19 = turret("mk19", fr.modeminecraft.entity.MountedGunEntity.Kind.MK19);
    public static final DeferredItem<Item> AMMO_BOX_50 = ITEMS.register("ammo_box_50", () -> new fr.modeminecraft.item.TurretAmmoItem(new Item.Properties()));
    public static final DeferredItem<Item> AMMO_BOX_792 = ITEMS.register("ammo_box_792", () -> new fr.modeminecraft.item.TurretAmmoItem(new Item.Properties()));
    public static final DeferredItem<Item> AMMO_BELT_303 = ITEMS.register("ammo_belt_303", () -> new fr.modeminecraft.item.TurretAmmoItem(new Item.Properties()));
    public static final DeferredItem<Item> AMMO_BELT_762R = ITEMS.register("ammo_belt_762r", () -> new fr.modeminecraft.item.TurretAmmoItem(new Item.Properties()));
    public static final DeferredItem<Item> FLAK_MAGAZINE_20MM = ITEMS.register("flak_magazine_20mm", () -> new fr.modeminecraft.item.TurretAmmoItem(new Item.Properties()));
    public static final DeferredItem<Item> AMMO_BOX_40MM = ITEMS.register("ammo_box_40mm", () -> new fr.modeminecraft.item.TurretAmmoItem(new Item.Properties()));
    /** Clé à molette : Shift + clic droit sur une tourelle pour la démonter. */
    public static final DeferredItem<Item> WRENCH = ITEMS.register("wrench", () -> new Item(new Item.Properties().stacksTo(1)) {
        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
                                    net.minecraft.world.item.TooltipFlag flag) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.modeminecraft.wrench").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    });

    private static DeferredItem<Item> turret(String name, fr.modeminecraft.entity.MountedGunEntity.Kind kind) {
        return ITEMS.register(name, () -> new fr.modeminecraft.item.MountedGunItem(kind, new Item.Properties().rarity(Rarity.EPIC)));
    }

    // ================= ARMES BLANCHES (cahier des charges) =================
    private static DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> melee(String name, Rarity rarity, float damage, float speed,
            float reach, float walk, float backstab, float armorPen, fr.modeminecraft.item.melee.MeleeWeaponItem.Ability ability) {
        var stats = new fr.modeminecraft.item.melee.MeleeWeaponItem.Stats(damage, speed, reach, walk, backstab, armorPen, ability);
        return ITEMS.register(name, () -> new fr.modeminecraft.item.melee.MeleeWeaponItem(stats, new Item.Properties().rarity(rarity)));
    }
    private static final fr.modeminecraft.item.melee.MeleeWeaponItem.Ability
            A_NONE = fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.NONE;
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> COMBAT_KNIFE = melee("combat_knife", Rarity.COMMON, 6, 2.4f, 3, 1.1f, 2.5f, 0, A_NONE);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> KARAMBIT = melee("karambit", Rarity.RARE, 5.5f, 3.0f, 3, 1.15f, 3, 0, A_NONE);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> KATANA = melee("katana", Rarity.RARE, 9, 1.6f, 4.2f, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.DASH);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> MACE = melee("mace", Rarity.UNCOMMON, 11, 0.9f, 3, 1, 1, 0.5f,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.ARMOR_BREAK);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> WAR_HAMMER = melee("war_hammer", Rarity.EPIC, 14, 0.7f, 3, 0.95f, 1, 0.2f,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.SHOCKWAVE);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> TOMAHAWK = melee("tomahawk", Rarity.UNCOMMON, 8.5f, 1.2f, 3, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.THROW);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> STUN_BATON = melee("stun_baton", Rarity.UNCOMMON, 5, 1.6f, 3, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.STUN);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> MACHETE = melee("machete", Rarity.COMMON, 8, 1.4f, 3, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.ANTI_UNDEAD);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> KUKRI = melee("kukri", Rarity.UNCOMMON, 8.5f, 1.3f, 3, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.BLEED);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> WAKIZASHI = melee("wakizashi", Rarity.UNCOMMON, 7.5f, 2.0f, 3.5f, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.FAST_PARRY);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> DAGGER = melee("dagger", Rarity.COMMON, 5.5f, 3.2f, 2.2f, 1, 3, 0, A_NONE);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> CLAYMORE = melee("claymore", Rarity.RARE, 13, 0.8f, 4.5f, 0.95f, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.WIDE_SWEEP);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> RAPIER = melee("rapier", Rarity.UNCOMMON, 7.5f, 1.8f, 3.8f, 1, 1, 0.4f,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.THRUST_PIERCE);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> SCIMITAR = melee("scimitar", Rarity.UNCOMMON, 8, 1.8f, 3, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.JUMP_CRITS);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> LANCE = melee("lance", Rarity.UNCOMMON, 8, 1.1f, 5.0f, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.CHARGE);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> HALBERD = melee("halberd", Rarity.RARE, 11.5f, 0.9f, 4.8f, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.DISMOUNT);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> WAR_SCYTHE = melee("war_scythe", Rarity.RARE, 10, 1.0f, 4.5f, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.PULL);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> BRASS_KNUCKLES = melee("brass_knuckles", Rarity.COMMON, 6, 3.5f, 2.0f, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.STUN_CHANCE);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> CROWBAR = melee("crowbar", Rarity.COMMON, 7.5f, 1.3f, 3, 1, 1, 0.3f,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.CROWBAR);
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> CUTLASS = melee("cutlass", Rarity.UNCOMMON, 8.5f, 1.6f, 3, 1, 1, 0,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.WATER);

    /** Machette en or d'El Patrón (arme unique du Chef de Cartel) : lourde, tranchante, fait saigner. */
    public static final DeferredItem<fr.modeminecraft.item.melee.MeleeWeaponItem> GOLDEN_MACHETE = melee("golden_machete", Rarity.EPIC, 11, 1.5f, 3.2f, 1, 1.5f, 0.3f,
            fr.modeminecraft.item.melee.MeleeWeaponItem.Ability.BLEED);
    /** Bouclier balistique lourd du Commissaire Divisionnaire SWAT : arrête les balles, mais ralentit. */
    public static final DeferredItem<Item> BALLISTIC_SHIELD = ITEMS.register("ballistic_shield",
            () -> new fr.modeminecraft.item.BallisticShieldItem(new Item.Properties().durability(1200).rarity(Rarity.EPIC)));

    // ================= GRENADES & EXPLOSIFS (cahier des charges) =================
    private static DeferredItem<Item> grenade(String name, fr.modeminecraft.item.explosive.GrenadeItem.Kind kind, Rarity rarity) {
        return ITEMS.register(name, () -> new fr.modeminecraft.item.explosive.GrenadeItem(kind, new Item.Properties().rarity(rarity)));
    }
    public static final DeferredItem<Item> FRAG_GRENADE = grenade("frag_grenade", fr.modeminecraft.item.explosive.GrenadeItem.Kind.FRAG, Rarity.COMMON);
    public static final DeferredItem<Item> OFFENSIVE_GRENADE = grenade("offensive_grenade", fr.modeminecraft.item.explosive.GrenadeItem.Kind.OFFENSIVE, Rarity.UNCOMMON);
    public static final DeferredItem<Item> SEMTEX = grenade("semtex", fr.modeminecraft.item.explosive.GrenadeItem.Kind.SEMTEX, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MOLOTOV = grenade("molotov", fr.modeminecraft.item.explosive.GrenadeItem.Kind.MOLOTOV, Rarity.COMMON);
    public static final DeferredItem<Item> CLUSTER_GRENADE = grenade("cluster_grenade", fr.modeminecraft.item.explosive.GrenadeItem.Kind.CLUSTER, Rarity.RARE);
    public static final DeferredItem<Item> FLASHBANG = grenade("flashbang", fr.modeminecraft.item.explosive.GrenadeItem.Kind.FLASHBANG, Rarity.COMMON);
    public static final DeferredItem<Item> SMOKE_GRENADE = grenade("smoke_grenade", fr.modeminecraft.item.explosive.GrenadeItem.Kind.SMOKE, Rarity.COMMON);
    public static final DeferredItem<Item> GAS_GRENADE = grenade("gas_grenade", fr.modeminecraft.item.explosive.GrenadeItem.Kind.GAS, Rarity.UNCOMMON);
    public static final DeferredItem<Item> EMP_GRENADE = grenade("emp_grenade", fr.modeminecraft.item.explosive.GrenadeItem.Kind.EMP, Rarity.RARE);
    public static final DeferredItem<Item> DECOY_GRENADE = grenade("decoy_grenade", fr.modeminecraft.item.explosive.GrenadeItem.Kind.DECOY, Rarity.UNCOMMON);
    public static final DeferredItem<Item> IMPULSE_GRENADE = grenade("impulse_grenade", fr.modeminecraft.item.explosive.GrenadeItem.Kind.IMPULSE, Rarity.RARE);
    public static final DeferredItem<Item> SENSOR_GRENADE = grenade("sensor_grenade", fr.modeminecraft.item.explosive.GrenadeItem.Kind.SENSOR, Rarity.UNCOMMON);
    public static final DeferredItem<Item> BOMBLET = grenade("bomblet", fr.modeminecraft.item.explosive.GrenadeItem.Kind.BOMBLET, Rarity.COMMON);
    public static final DeferredItem<Item> C4 = ITEMS.register("c4", () -> new fr.modeminecraft.item.explosive.PlaceableExplosiveItem(
            fr.modeminecraft.entity.PlacedExplosiveEntity.Kind.C4, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> DETONATOR = ITEMS.register("detonator", () -> new fr.modeminecraft.item.explosive.DetonatorItem(new Item.Properties()));
    public static final DeferredItem<Item> CLAYMORE_MINE = ITEMS.register("claymore_mine", () -> new fr.modeminecraft.item.explosive.PlaceableExplosiveItem(
            fr.modeminecraft.entity.PlacedExplosiveEntity.Kind.CLAYMORE, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MORTAR = ITEMS.register("mortar", () -> new fr.modeminecraft.item.explosive.PlaceableExplosiveItem(
            fr.modeminecraft.entity.PlacedExplosiveEntity.Kind.MORTAR, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> MORTAR_SHELL = ITEMS.registerSimpleItem("mortar_shell", new Item.Properties().stacksTo(16));
    /** Mines antichars (mise à jour 0.4). */
    public static final DeferredItem<Item> TM62_MINE = ITEMS.register("tm62_mine", () -> new fr.modeminecraft.item.explosive.PlaceableExplosiveItem(
            fr.modeminecraft.entity.PlacedExplosiveEntity.Kind.TM62, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> M15_MINE = ITEMS.register("m15_mine", () -> new fr.modeminecraft.item.explosive.PlaceableExplosiveItem(
            fr.modeminecraft.entity.PlacedExplosiveEntity.Kind.M15, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> GAS_MASK = ITEMS.register("gas_mask", () -> new fr.modeminecraft.item.explosive.GasMaskItem(new Item.Properties()));

    // ---------- Accessoires ----------
    public static final DeferredItem<AttachmentItem> RED_DOT_SIGHT = attachment("red_dot_sight",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).sight(0.85f, SightOverlay.RED_DOT).aimSpread(0.7f));
    public static final DeferredItem<AttachmentItem> HOLO_SIGHT = attachment("holo_sight",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).sight(0.75f, SightOverlay.HOLO).aimSpread(0.6f));
    public static final DeferredItem<AttachmentItem> ACOG_SCOPE = attachment("acog_scope",
            AttachmentItem.Stats.of(AttachmentSlot.SIGHT).sight(0.333f, SightOverlay.SCOPE_X4).aimSpread(0.45f)); // ACOG x3
    public static final DeferredItem<AttachmentItem> SILENCER = attachment("silencer",
            AttachmentItem.Stats.of(AttachmentSlot.MUZZLE).silencer().damage(0.9f));
    public static final DeferredItem<AttachmentItem> COMPENSATOR = attachment("compensator",
            AttachmentItem.Stats.of(AttachmentSlot.MUZZLE).recoil(0.6f));
    public static final DeferredItem<AttachmentItem> VERTICAL_GRIP = attachment("vertical_grip",
            AttachmentItem.Stats.of(AttachmentSlot.GRIP).recoil(0.85f)); // -15 % de recul
    public static final DeferredItem<AttachmentItem> ANGLED_GRIP = attachment("angled_grip",
            AttachmentItem.Stats.of(AttachmentSlot.GRIP).aimSpeed(1.25f)); // +25 % de vitesse de visée
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

    // ---------- Accessoires du cahier des charges ----------
    public static final DeferredItem<AttachmentItem> MUZZLE_BRAKE = attachment("muzzle_brake",
            AttachmentItem.Stats.of(AttachmentSlot.MUZZLE).recoil(0.8f).volume(1.15f)); // -20 % de recul (plus bruyant)
    public static final DeferredItem<AttachmentItem> FLASH_HIDER = attachment("flash_hider",
            AttachmentItem.Stats.of(AttachmentSlot.MUZZLE).special(AttachmentItem.Special.NO_FLASH));
    public static final DeferredItem<AttachmentItem> EXTENDED_MAG = attachment("extended_mag",
            AttachmentItem.Stats.of(AttachmentSlot.MAGAZINE).magazineMul(1.35f).reload(1.1f)); // +35 % de balles
    public static final DeferredItem<AttachmentItem> FAST_MAG = attachment("fast_mag",
            AttachmentItem.Stats.of(AttachmentSlot.MAGAZINE).reload(0.8f)); // rechargement +25 % plus rapide

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

    // ---------- Lampes tactiques (rail latéral, touche L / Maj + L) ----------
    /** Pile CR123A : 10 minutes d'éclairage. Clic droit (ou posée sur l'arme dans l'inventaire) pour changer la pile. */
    public static final DeferredItem<Item> TACTICAL_BATTERY = ITEMS.register("tactical_battery",
            () -> new fr.modeminecraft.item.TacticalBatteryItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<AttachmentItem> DUCT_TAPE_FLASHLIGHT = attachment("duct_tape_flashlight",
            AttachmentItem.Stats.of(AttachmentSlot.SIDE_RAIL).aimSpeed(0.95f)
                    .light(new AttachmentItem.Light(15, 12, 0xFFD48A, 6, 1, false, false, true, 1, 1)));
    public static final DeferredItem<AttachmentItem> SUREFIRE_M600 = attachment("surefire_m600",
            AttachmentItem.Stats.of(AttachmentSlot.SIDE_RAIL)
                    .light(new AttachmentItem.Light(28, 15, 0xF4F8FF, 7, 2, false, false, false, 1, 1)));
    public static final DeferredItem<AttachmentItem> STREAMLIGHT_TLR1 = attachment("streamlight_tlr1",
            AttachmentItem.Stats.of(AttachmentSlot.SIDE_RAIL)
                    .light(new AttachmentItem.Light(38, 15, 0xFFFFFF, 11, 3, true, false, false, 1, 1)));
    public static final DeferredItem<AttachmentItem> AN_PEQ15 = attachment("an_peq15",
            AttachmentItem.Stats.of(AttachmentSlot.SIDE_RAIL).laser(0x30FF40)
                    .light(new AttachmentItem.Light(30, 15, 0xF0F6FF, 7, 3, false, false, false, 0.75f, 1.1f)));
    public static final DeferredItem<AttachmentItem> MAWL_C1 = attachment("mawl_c1",
            AttachmentItem.Stats.of(AttachmentSlot.SIDE_RAIL)
                    .light(new AttachmentItem.Light(45, 15, 0xEEF4FF, 8, 4, true, true, false, 1, 1)));

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
    public static final DeferredItem<BlockItem> ARTILLERY_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.ARTILLERY_TABLE);
    public static final DeferredItem<BlockItem> ASSEMBLY_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.ASSEMBLY_TABLE);
    /** Pièces détachées des véhicules (établi d'assemblage, bases militaires, hangars). */
    public static final DeferredItem<Item> VEHICLE_ENGINE = ammoItem("vehicle_engine", 4);
    public static final DeferredItem<Item> GAS_TURBINE = ammoItem("gas_turbine", 4);
    public static final DeferredItem<Item> TRACK_SET = ammoItem("track_set", 4);
    public static final DeferredItem<Item> WHEEL_SET = ammoItem("wheel_set", 4);
    public static final DeferredItem<Item> CANNON_BARREL = ammoItem("cannon_barrel", 2);
    public static final DeferredItem<Item> AUTOCANNON_MOUNT = ammoItem("autocannon_mount", 4);
    public static final DeferredItem<Item> FIRE_CONTROL_MODULE = ammoItem("fire_control_module", 8);
    public static final DeferredItem<Item> ROTOR_ASSEMBLY = ammoItem("rotor_assembly", 2);
    public static final DeferredItem<Item> LAUNCHER_MODULE = ammoItem("launcher_module", 4);
    public static final DeferredItem<BlockItem> MODIFICATION_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.MODIFICATION_TABLE);
    public static final DeferredItem<BlockItem> WEAPON_RACK = ITEMS.registerSimpleBlockItem(ModBlocks.WEAPON_RACK);
    public static final DeferredItem<BlockItem> TROPHY_WEAPON_RACK = ITEMS.registerSimpleBlockItem(ModBlocks.TROPHY_WEAPON_RACK);
    public static final DeferredItem<BlockItem> SHOOTING_TARGET = ITEMS.registerSimpleBlockItem(ModBlocks.SHOOTING_TARGET);

    // ---------- Nouveaux matériaux : titane, acier, kevlar ----------
    public static final DeferredItem<BlockItem> TITANIUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.TITANIUM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_TITANIUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_TITANIUM_ORE);
    public static final DeferredItem<Item> RAW_TITANIUM = ITEMS.registerSimpleItem("raw_titanium");
    public static final DeferredItem<Item> TITANIUM_INGOT = ITEMS.registerSimpleItem("titanium_ingot");
    public static final DeferredItem<Item> TITANIUM_PLATE = ITEMS.registerSimpleItem("titanium_plate");
    public static final DeferredItem<Item> COMPOSITE_PREFORM = ITEMS.registerSimpleItem("composite_preform");
    public static final DeferredItem<Item> CERAMIC_TITANIUM_COMPOSITE = ITEMS.registerSimpleItem("ceramic_titanium_composite");
    public static final DeferredItem<Item> CARBURIZED_IRON = ITEMS.registerSimpleItem("carburized_iron");
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<Item> STEEL_PLATE = ITEMS.registerSimpleItem("steel_plate");
    public static final DeferredItem<Item> KEVLAR_FIBER = ITEMS.registerSimpleItem("kevlar_fiber");
    public static final DeferredItem<Item> KEVLAR_PLATE = ITEMS.registerSimpleItem("kevlar_plate");
    public static final DeferredItem<Item> MULTILAYER_KEVLAR = ITEMS.registerSimpleItem("multilayer_kevlar");

    // ---------- Casques militaires 3D (par époque) ----------
    //                                                           époque armure tête  visière mêlée éclats explos. durab. module        vitesse réparation
    public static final DeferredItem<Item> GALEA = helmet("galea", Rarity.COMMON,
            new HelmetItem.Stats(1, 2.0, 0.15f, 0.15f, 0.10f, 0, 0, 180, HelmetItem.Module.NONE, 0, () -> net.minecraft.world.item.Items.IRON_INGOT));
    public static final DeferredItem<Item> GREAT_HELM = helmet("great_helm", Rarity.COMMON,
            new HelmetItem.Stats(1, 3.0, 0.25f, 0.25f, 0.20f, 0, 0, 240, HelmetItem.Module.NONE, 0, () -> net.minecraft.world.item.Items.IRON_INGOT));
    public static final DeferredItem<Item> ADRIAN_M15 = helmet("adrian_m15", Rarity.COMMON,
            new HelmetItem.Stats(1, 2.5, 0.20f, 0.20f, 0, 0.30f, 0, 200, HelmetItem.Module.NONE, 0, () -> net.minecraft.world.item.Items.IRON_INGOT));
    public static final DeferredItem<Item> BRODIE_MK1 = helmet("brodie_mk1", Rarity.COMMON,
            new HelmetItem.Stats(1, 2.5, 0.20f, 0.20f, 0, 0.35f, 0, 200, HelmetItem.Module.NONE, 0, () -> net.minecraft.world.item.Items.IRON_INGOT));
    public static final DeferredItem<Item> M1_HELMET = helmet("m1_helmet", Rarity.UNCOMMON,
            new HelmetItem.Stats(2, 3.0, 0.35f, 0.35f, 0, 0, 0.15f, 330, HelmetItem.Module.NONE, 0, () -> ModItems.STEEL_INGOT.get()));
    public static final DeferredItem<Item> STAHLHELM_M40 = helmet("stahlhelm_m40", Rarity.UNCOMMON,
            new HelmetItem.Stats(2, 3.5, 0.40f, 0.40f, 0, 0, 0, 330, HelmetItem.Module.NONE, 0, () -> ModItems.STEEL_INGOT.get()));
    public static final DeferredItem<Item> SSH40 = helmet("ssh40", Rarity.UNCOMMON,
            new HelmetItem.Stats(2, 3.5, 0.38f, 0.38f, 0, 0, 0, 600, HelmetItem.Module.NONE, 0, () -> ModItems.STEEL_INGOT.get()));
    public static final DeferredItem<Item> PASGT = helmet("pasgt", Rarity.RARE,
            new HelmetItem.Stats(3, 4.0, 0.50f, 0.50f, 0, 0, 0, 420, HelmetItem.Module.NONE, 0, () -> ModItems.KEVLAR_PLATE.get()));
    public static final DeferredItem<Item> ALTYN = helmet("altyn", Rarity.RARE,
            new HelmetItem.Stats(3, 5.0, 0.50f, 0.75f, 0, 0, 0, 520, HelmetItem.Module.VISOR, 0, () -> ModItems.TITANIUM_PLATE.get()));
    public static final DeferredItem<Item> MICH_2000 = helmet("mich_2000", Rarity.EPIC,
            new HelmetItem.Stats(4, 4.5, 0.60f, 0.60f, 0, 0, 0, 520, HelmetItem.Module.NONE, 0, () -> ModItems.MULTILAYER_KEVLAR.get()));
    public static final DeferredItem<Item> OPS_CORE_FAST = helmet("ops_core_fast", Rarity.EPIC,
            new HelmetItem.Stats(4, 5.0, 0.65f, 0.65f, 0, 0, 0, 560, HelmetItem.Module.NVG, 0, () -> ModItems.MULTILAYER_KEVLAR.get()));
    public static final DeferredItem<Item> JUGGERNAUT_HELMET = helmet("juggernaut_helmet", Rarity.EPIC,
            new HelmetItem.Stats(4, 6.0, 0.85f, 0.85f, 0, 0, 0, 900, HelmetItem.Module.NONE, -0.08, () -> ModItems.CERAMIC_TITANIUM_COMPOSITE.get()));

    /** Manuel d'armement & d'ingénierie militaire : ouvre le livre des recettes. */
    public static final DeferredItem<Item> MILITARY_FIELD_MANUAL = ITEMS.register("military_field_manual",
            () -> new fr.modeminecraft.item.FieldManualItem(new Item.Properties().stacksTo(1)));

    // ---------- Gilets pare-balles 3D : matériaux, plaques balistiques, gilets ----------
    public static final DeferredItem<Item> BALLISTIC_NYLON = ITEMS.registerSimpleItem("ballistic_nylon");
    public static final DeferredItem<Item> TACTICAL_STRAP = ITEMS.registerSimpleItem("tactical_strap");
    public static final DeferredItem<Item> STEEL_BALLISTIC_PLATE = ITEMS.register("steel_ballistic_plate",
            () -> new fr.modeminecraft.item.BallisticPlateItem(fr.modeminecraft.item.VestItem.PlateType.STEEL, new Item.Properties()));
    public static final DeferredItem<Item> CERAMIC_BALLISTIC_PLATE = ITEMS.register("ceramic_ballistic_plate",
            () -> new fr.modeminecraft.item.BallisticPlateItem(fr.modeminecraft.item.VestItem.PlateType.CERAMIC, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> NETHERITE_BALLISTIC_PLATE = ITEMS.register("netherite_ballistic_plate",
            () -> new fr.modeminecraft.item.BallisticPlateItem(fr.modeminecraft.item.VestItem.PlateType.NETHERITE, new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    //                                                        époque armure éclats explo. léger fusil gros  mêlée plaques vitesse recharg. visée durab. spécial
    public static final DeferredItem<Item> WWI_CUIRASS = vest("wwi_cuirass", Rarity.COMMON, new fr.modeminecraft.item.VestItem.Stats(
            1, 5.0, 0.40f, 0.40f, 0.20f, 0, 0, 0.15f, false, -0.10, 0, 0, 220, fr.modeminecraft.item.VestItem.Special.NONE, () -> net.minecraft.world.item.Items.IRON_INGOT));
    public static final DeferredItem<Item> M12_FLAK_VEST = vest("m12_flak_vest", Rarity.COMMON, new fr.modeminecraft.item.VestItem.Stats(
            1, 4.0, 0.55f, 0.55f, 0.15f, 0.15f, 0.15f, 0.10f, false, 0, 0, 0, 240, fr.modeminecraft.item.VestItem.Special.NONE, () -> ModItems.STEEL_INGOT.get()));
    public static final DeferredItem<Item> PASGT_VEST = vest("pasgt_vest", Rarity.UNCOMMON, new fr.modeminecraft.item.VestItem.Stats(
            2, 6.0, 0.30f, 0.20f, 0.50f, 0.20f, 0.05f, 0, false, 0, 0, 0, 380, fr.modeminecraft.item.VestItem.Special.NONE, () -> ModItems.KEVLAR_PLATE.get()));
    public static final DeferredItem<Item> VEST_6B3 = vest("vest_6b3", Rarity.UNCOMMON, new fr.modeminecraft.item.VestItem.Stats(
            2, 7.0, 0.30f, 0.20f, 0.40f, 0.40f, 0.40f, 0, false, -0.05, 0.10f, 0, 450, fr.modeminecraft.item.VestItem.Special.NONE, () -> ModItems.TITANIUM_PLATE.get()));
    public static final DeferredItem<Item> SWAT_VEST = vest("swat_vest", Rarity.UNCOMMON, new fr.modeminecraft.item.VestItem.Stats(
            2, 6.5, 0.30f, 0.30f, 0.60f, 0.15f, 0.05f, 0.10f, false, 0, 0, 0, 400, fr.modeminecraft.item.VestItem.Special.NONE, () -> ModItems.KEVLAR_PLATE.get()));
    public static final DeferredItem<Item> JPC_PLATE_CARRIER = vest("jpc_plate_carrier", Rarity.RARE, new fr.modeminecraft.item.VestItem.Stats(
            3, 6.0, 0.20f, 0.10f, 0.20f, 0.10f, 0, 0, true, 0, 0.15f, 0.15f, 420, fr.modeminecraft.item.VestItem.Special.NONE, () -> ModItems.MULTILAYER_KEVLAR.get()));
    public static final DeferredItem<Item> IOTV_VEST = vest("iotv_vest", Rarity.RARE, new fr.modeminecraft.item.VestItem.Stats(
            3, 7.5, 0.45f, 0.50f, 0.45f, 0.45f, 0.45f, 0, true, -0.04, 0, 0, 520, fr.modeminecraft.item.VestItem.Special.NONE, () -> ModItems.MULTILAYER_KEVLAR.get()));
    public static final DeferredItem<Item> AVS_PLATE_CARRIER = vest("avs_plate_carrier", Rarity.RARE, new fr.modeminecraft.item.VestItem.Stats(
            3, 8.0, 0.30f, 0.20f, 0.25f, 0.15f, 0.05f, 0, true, 0, 0, 0, 520, fr.modeminecraft.item.VestItem.Special.AVS, () -> ModItems.MULTILAYER_KEVLAR.get()));
    public static final DeferredItem<Item> FORT_DEFENDER = vest("fort_defender", Rarity.EPIC, new fr.modeminecraft.item.VestItem.Stats(
            4, 9.0, 0.50f, 0.50f, 0.65f, 0.65f, 0.65f, 0.20f, true, -0.12, 0, 0, 800, fr.modeminecraft.item.VestItem.Special.FORT_DEFENDER, () -> ModItems.CERAMIC_TITANIUM_COMPOSITE.get()));
    public static final DeferredItem<Item> EOD_SUIT = vest("eod_suit", Rarity.EPIC, new fr.modeminecraft.item.VestItem.Stats(
            4, 10.0, 0.85f, 0.85f, 0.70f, 0.70f, 0.70f, 0.20f, false, -0.18, 0, 0, 1000, fr.modeminecraft.item.VestItem.Special.EOD, () -> ModItems.CERAMIC_TITANIUM_COMPOSITE.get()));

    // ---------- Pantalons militaires 3D (jambières) ----------
    private static final net.minecraft.world.entity.EquipmentSlot LEGS = net.minecraft.world.entity.EquipmentSlot.LEGS;
    private static final net.minecraft.world.entity.EquipmentSlot FEET = net.minecraft.world.entity.EquipmentSlot.FEET;
    private static fr.modeminecraft.item.GearItem.Stats g(int tier, double armor, int durability) {
        return fr.modeminecraft.item.GearItem.Stats.of(tier, armor, durability);
    }
    public static final DeferredItem<Item> WWI_TROUSERS = gear("wwi_trousers", Rarity.COMMON, LEGS,
            g(1, 1.0, 150).coldImmune());
    public static final DeferredItem<Item> M42_PARATROOPER_PANTS = gear("m42_paratrooper_pants", Rarity.COMMON, LEGS,
            g(1, 1.5, 180).food(0.10f).reload(0.10f));
    public static final DeferredItem<Item> BDU_WOODLAND_PANTS = gear("bdu_woodland_pants", Rarity.UNCOMMON, LEGS,
            g(2, 2.0, 260).speed(0.08).sneak(0.25).prone(0.25).repair(() -> ModItems.BALLISTIC_NYLON.get()));
    public static final DeferredItem<Item> SWAT_PANTS = gear("swat_pants", Rarity.UNCOMMON, LEGS,
            g(2, 2.5, 300).sneak(0.35).prone(0.35).draw(0.30f).legs(0.25f, 0.25f, true).repair(() -> ModItems.BALLISTIC_NYLON.get()));
    public static final DeferredItem<Item> CRYE_G3_PANTS = gear("crye_g3_pants", Rarity.RARE, LEGS,
            g(3, 2.5, 360).speed(0.15).prone(0.50).sneak(0.40).food(0.25f).repair(() -> ModItems.BALLISTIC_NYLON.get()));
    public static final DeferredItem<Item> INFILTRATOR_PANTS = gear("infiltrator_pants", Rarity.RARE, LEGS,
            g(3, 3.0, 380).speed(0.18).prone(0.65).sneak(0.65).detection(0.50f).food(0.30f).repair(() -> ModItems.KEVLAR_PLATE.get()));
    public static final DeferredItem<Item> RATNIK_PANTS = gear("ratnik_pants", Rarity.EPIC, LEGS,
            g(4, 3.5, 520).knockback().prone(0.30).food(0.20f).legs(0.45f, 0.45f, false).repair(() -> ModItems.MULTILAYER_KEVLAR.get()));
    public static final DeferredItem<Item> TALOS_EXO_PANTS = gear("talos_exo_pants", Rarity.EPIC, LEGS,
            g(4, 3.0, 520).sprint(0.25).prone(0.80).food(0.50f).step(0.4).repair(() -> ModItems.TITANIUM_PLATE.get()));

    // ---------- Rangers et bottes militaires 3D ----------
    public static final DeferredItem<Item> WWI_HOBNAIL_BOOTS = gear("wwi_hobnail_boots", Rarity.COMMON, FEET,
            g(1, 1.0, 150).noSlowdown());
    public static final DeferredItem<Item> CORCORAN_JUMP_BOOTS = gear("corcoran_jump_boots", Rarity.COMMON, FEET,
            g(1, 1.5, 200).fall(0.25).food(0.10f).repair(() -> ModItems.STEEL_INGOT.get()));
    public static final DeferredItem<Item> M65_COMBAT_BOOTS = gear("m65_combat_boots", Rarity.UNCOMMON, FEET,
            g(2, 2.0, 280).speed(0.10).fall(0.30).food(0.15f).repair(() -> ModItems.STEEL_INGOT.get()));
    public static final DeferredItem<Item> PANAMA_JUNGLE_BOOTS = gear("panama_jungle_boots", Rarity.UNCOMMON, FEET,
            g(2, 1.5, 240).biomeSpeed(0.15).swim(0.25).food(0.20f).repair(() -> ModItems.BALLISTIC_NYLON.get()));
    public static final DeferredItem<Item> LOWA_ZEPHYR_BOOTS = gear("lowa_zephyr_boots", Rarity.RARE, FEET,
            g(3, 2.0, 340).sprint(0.18).food(0.40f).fall(0.40).repair(() -> ModItems.BALLISTIC_NYLON.get()));
    public static final DeferredItem<Item> SALOMON_FORCES_BOOTS = gear("salomon_forces_boots", Rarity.RARE, FEET,
            g(3, 2.5, 360).stealth().speed(0.20).prone(0.30).fall(0.50).detection(0.30f).repair(() -> ModItems.KEVLAR_PLATE.get()));
    public static final DeferredItem<Item> EOD_BOOTS = gear("eod_boots", Rarity.EPIC, FEET,
            g(4, 3.0, 520).groundBlast(0.75f).knockback().repair(() -> ModItems.MULTILAYER_KEVLAR.get()));
    public static final DeferredItem<Item> APEX_JUMP_BOOTS = gear("apex_jump_boots", Rarity.EPIC, FEET,
            g(4, 2.5, 520).sprint(0.25).jump(0.22).fall(1.0).safeFall(22).food(0.50f).repair(() -> ModItems.TITANIUM_PLATE.get()));

    private static DeferredItem<Item> gear(String name, Rarity rarity, net.minecraft.world.entity.EquipmentSlot slot,
                                           fr.modeminecraft.item.GearItem.Stats stats) {
        return ITEMS.register(name, () -> new fr.modeminecraft.item.GearItem(slot, stats, new Item.Properties().rarity(rarity)));
    }

    private static DeferredItem<Item> vest(String name, Rarity rarity, fr.modeminecraft.item.VestItem.Stats stats) {
        return ITEMS.register(name, () -> new fr.modeminecraft.item.VestItem(stats, new Item.Properties().rarity(rarity)));
    }

    private static DeferredItem<Item> helmet(String name, Rarity rarity, HelmetItem.Stats stats) {
        return ITEMS.register(name, () -> new HelmetItem(stats, new Item.Properties().rarity(rarity)));
    }

    private static DeferredItem<GunItem> gun(String name, Rarity rarity, GunStats.Builder stats) {
        return ITEMS.register(name, () -> new GunItem(stats.build(), new Item.Properties().rarity(rarity)));
    }

    private static DeferredItem<Item> ammo(String name) {
        return ITEMS.register(name, () -> new AmmoItem(false, new Item.Properties()));
    }

    // ---------- Villes et factions ----------
    /** La Pourfendeuse de Dragons (Chef des Troupes de Choc des Faucons Blancs) : impossible à enchanter. */
    public static final DeferredItem<Item> DRAGONSLAYER = ITEMS.register("dragonslayer",
            () -> new fr.modeminecraft.item.DragonslayerItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> BLUE_ACCESS_CARD = ITEMS.register("blue_access_card",
            () -> new fr.modeminecraft.item.AccessCardItem(1, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RED_ACCESS_CARD = ITEMS.register("red_access_card",
            () -> new fr.modeminecraft.item.AccessCardItem(2, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> BLACK_ACCESS_CARD = ITEMS.register("black_access_card",
            () -> new fr.modeminecraft.item.AccessCardItem(3, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> VAULT_DOOR_BLUE = ITEMS.register("vault_door_blue",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.VAULT_DOOR_BLUE.get(), new Item.Properties()));
    public static final DeferredItem<Item> VAULT_DOOR_RED = ITEMS.register("vault_door_red",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.VAULT_DOOR_RED.get(), new Item.Properties()));
    public static final DeferredItem<Item> VAULT_DOOR_BLACK = ITEMS.register("vault_door_black",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.VAULT_DOOR_BLACK.get(), new Item.Properties()));
    // Soins tactiques
    public static final DeferredItem<Item> BANDAGE = ITEMS.register("bandage",
            () -> new fr.modeminecraft.item.MedicalItem(fr.modeminecraft.item.MedicalItem.Kind.BANDAGE, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> FIRST_AID_KIT = ITEMS.register("first_aid_kit",
            () -> new fr.modeminecraft.item.MedicalItem(fr.modeminecraft.item.MedicalItem.Kind.FIRST_AID_KIT, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<Item> ADRENALINE_SYRINGE = ITEMS.register("adrenaline_syringe",
            () -> new fr.modeminecraft.item.MedicalItem(fr.modeminecraft.item.MedicalItem.Kind.ADRENALINE, new Item.Properties().stacksTo(4).rarity(Rarity.UNCOMMON)));
    // Véhicules
    // ================= VÉHICULES MILITAIRES & MUNITIONS LOURDES (mise à jour 0.4) =================
    public static final java.util.Map<fr.modeminecraft.entity.military.MilType, DeferredItem<Item>> MIL_VEHICLES =
            new java.util.EnumMap<>(fr.modeminecraft.entity.military.MilType.class);

    static {
        for (var t : fr.modeminecraft.entity.military.MilType.values()) {
            Rarity r = t.has(fr.modeminecraft.entity.military.MilType.Flag.STRATEGIC) || t.health >= 500 ? Rarity.EPIC
                    : t.health >= 200 || t.heli() ? Rarity.RARE : Rarity.UNCOMMON;
            MIL_VEHICLES.put(t, ITEMS.register(t.id, () -> new fr.modeminecraft.item.military.MilVehicleItem(t, new Item.Properties().rarity(r))));
        }
    }

    /** Caisse de 14,5 mm (KPVT). */
    public static final DeferredItem<Item> AMMO_BOX_145 = ITEMS.register("ammo_box_145", () -> new fr.modeminecraft.item.TurretAmmoItem(new Item.Properties()));
    public static final DeferredItem<Item> AP_I_BELT = ammoItem("ap_i_belt", 16);
    public static final DeferredItem<Item> HE_I_BELT = ammoItem("he_i_belt", 16);
    public static final DeferredItem<Item> APFSDS_SHELL = ammoItem("apfsds_shell", 8);
    public static final DeferredItem<Item> HE_SHELL = ammoItem("he_shell", 8);
    public static final DeferredItem<Item> HEAT_SHELL = ammoItem("heat_shell", 8);
    public static final DeferredItem<Item> CANNON_ATGM_MISSILE = ammoItem("cannon_atgm_missile", 4);
    public static final DeferredItem<Item> GRAD_ROCKET = ammoItem("grad_rocket", 16);
    public static final DeferredItem<Item> MLRS_ROCKET = ammoItem("mlrs_rocket", 6);
    public static final DeferredItem<Item> ATACMS_MISSILE = ammoItem("atacms_missile", 1);
    public static final DeferredItem<Item> S8_ROCKET = ammoItem("s8_rocket", 20);
    public static final DeferredItem<Item> HYDRA_ROCKET = ammoItem("hydra_rocket", 19);
    public static final DeferredItem<Item> ATGM_MISSILE = ammoItem("atgm_missile", 4);
    /** Missiles des complexes S-400 / S-500 / Iskander. */
    public static final DeferredItem<Item> MISSILE_48N6E3 = ammoItem("missile_48n6e3", 1);
    public static final DeferredItem<Item> MISSILE_9M96E2 = ammoItem("missile_9m96e2", 1);
    public static final DeferredItem<Item> MISSILE_77N6N = ammoItem("missile_77n6n", 1);
    public static final DeferredItem<Item> MISSILE_40N6M = ammoItem("missile_40n6m", 1);
    public static final DeferredItem<Item> ISKANDER_HE = ammoItem("iskander_he", 1);
    public static final DeferredItem<Item> ISKANDER_CLUSTER = ammoItem("iskander_cluster", 1);
    public static final DeferredItem<Item> ISKANDER_EMP = ammoItem("iskander_emp", 1);
    /** Tenues d'infiltration (chambres fortes des boss) : 6 tenues de 4 pièces. */
    public static final java.util.Map<String, DeferredItem<Item>> OUTFITS = new java.util.LinkedHashMap<>();

    static {
        outfit("police_uniform", fr.modeminecraft.entity.bot.Disguise.Outfit.POLICE, ModArmorMaterials.POLICE_UNIFORM, Rarity.RARE);
        outfit("desert_boss", fr.modeminecraft.entity.bot.Disguise.Outfit.DESERT, ModArmorMaterials.DESERT_BOSS, Rarity.EPIC);
        outfit("soviet_uniform", fr.modeminecraft.entity.bot.Disguise.Outfit.SOVIET, ModArmorMaterials.SOVIET_UNIFORM, Rarity.UNCOMMON);
        outfit("special_forces", fr.modeminecraft.entity.bot.Disguise.Outfit.SPECIAL_FORCES, ModArmorMaterials.SPECIAL_FORCES, Rarity.UNCOMMON);
        outfit("cartel_outfit", fr.modeminecraft.entity.bot.Disguise.Outfit.CARTEL, ModArmorMaterials.CARTEL_OUTFIT, Rarity.UNCOMMON);
        outfit("sheriff_outfit", fr.modeminecraft.entity.bot.Disguise.Outfit.SHERIFF, ModArmorMaterials.SHERIFF_OUTFIT, Rarity.UNCOMMON);
    }

    private static void outfit(String name, fr.modeminecraft.entity.bot.Disguise.Outfit o, net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> m, Rarity r) {
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
        ArmorItem.Type[] types = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};
        for (int i = 0; i < 4; i++) {
            ArmorItem.Type t = types[i];
            String id = name + "_" + pieces[i];
            OUTFITS.put(id, ITEMS.register(id, () -> new fr.modeminecraft.item.DisguiseArmorItem(o, m, t,
                    new Item.Properties().rarity(r).durability(t.getDurability(14)))));
        }
    }

    /** Cartes d'accès nominatives des boss de ville. */
    public static final java.util.Map<fr.modeminecraft.item.BossCardItem.VaultType, DeferredItem<Item>> BOSS_CARDS =
            new java.util.EnumMap<>(fr.modeminecraft.item.BossCardItem.VaultType.class);

    static {
        for (var t : fr.modeminecraft.item.BossCardItem.VaultType.values()) {
            BOSS_CARDS.put(t, ITEMS.register("boss_card_" + t.id, () -> new fr.modeminecraft.item.BossCardItem(t, new Item.Properties().rarity(Rarity.EPIC))));
        }
    }

    /** Bidon de carburant militaire (diesel) : +25 % de réservoir. */
    public static final DeferredItem<Item> JERRYCAN = ammoItem("jerrycan", 4);
    public static final DeferredItem<Item> LASER_DESIGNATOR = ITEMS.register("laser_designator",
            () -> new fr.modeminecraft.item.military.LaserDesignatorItem(new Item.Properties().rarity(Rarity.RARE)));

    private static DeferredItem<Item> ammoItem(String name, int stack) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(stack)) {
            @Override
            public void appendHoverText(net.minecraft.world.item.ItemStack st, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
                                        net.minecraft.world.item.TooltipFlag flag) {
                tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.modeminecraft." + name).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        });
    }

    public static final DeferredItem<Item> QUAD = ITEMS.register("quad",
            () -> new fr.modeminecraft.item.VehicleItem(fr.modeminecraft.entity.VehicleEntity.Type.QUAD, new Item.Properties()));
    public static final DeferredItem<Item> POLICE_CAR = ITEMS.register("police_car",
            () -> new fr.modeminecraft.item.VehicleItem(fr.modeminecraft.entity.VehicleEntity.Type.POLICE_CAR, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> JEEP = ITEMS.register("jeep",
            () -> new fr.modeminecraft.item.VehicleItem(fr.modeminecraft.entity.VehicleEntity.Type.JEEP, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> ARMORED_TRUCK = ITEMS.register("armored_truck",
            () -> new fr.modeminecraft.item.VehicleItem(fr.modeminecraft.entity.VehicleEntity.Type.ARMORED_TRUCK, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> SEDAN = ITEMS.register("sedan",
            () -> new fr.modeminecraft.item.VehicleItem(fr.modeminecraft.entity.VehicleEntity.Type.SEDAN, new Item.Properties()));
    public static final DeferredItem<Item> TAXI = ITEMS.register("taxi",
            () -> new fr.modeminecraft.item.VehicleItem(fr.modeminecraft.entity.VehicleEntity.Type.TAXI, new Item.Properties()));
    public static final DeferredItem<Item> BUS = ITEMS.register("bus",
            () -> new fr.modeminecraft.item.VehicleItem(fr.modeminecraft.entity.VehicleEntity.Type.BUS, new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** Sac à dos tactique : 18 cases, porté dans la case « sac à dos » de l'inventaire. */
    public static final DeferredItem<Item> TACTICAL_BACKPACK = ITEMS.register("tactical_backpack",
            () -> new fr.modeminecraft.item.tactical.BackpackItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    // Drones de combat et guerre électronique
    public static final DeferredItem<Item> DRONE_CONTROLLER = ITEMS.register("drone_controller",
            () -> new fr.modeminecraft.entity.drone.DroneControllerItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BLACK_HORNET = drone("black_hornet", fr.modeminecraft.entity.drone.DroneType.HORNET, Rarity.COMMON);
    public static final DeferredItem<Item> FPV_FRELON = drone("fpv_frelon", fr.modeminecraft.entity.drone.DroneType.FPV, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MAVIC_TACTIQUE = drone("mavic_tactique", fr.modeminecraft.entity.drone.DroneType.MAVIC, Rarity.UNCOMMON);
    public static final DeferredItem<Item> WASP_MG = drone("wasp_mg", fr.modeminecraft.entity.drone.DroneType.WASP, Rarity.RARE);
    public static final DeferredItem<Item> REAPER_X = drone("reaper_x", fr.modeminecraft.entity.drone.DroneType.REAPER, Rarity.EPIC);
    public static final DeferredItem<Item> SILENT_ROTORS = droneModule("silent_rotors", fr.modeminecraft.entity.drone.DroneModuleItem.Kind.ROTORS);
    public static final DeferredItem<Item> HIGH_CAP_BATTERY = droneModule("high_cap_battery", fr.modeminecraft.entity.drone.DroneModuleItem.Kind.BATTERY);
    public static final DeferredItem<Item> CARGO_MODULE = droneModule("cargo_module", fr.modeminecraft.entity.drone.DroneModuleItem.Kind.CARGO);

    private static DeferredItem<Item> drone(String name, fr.modeminecraft.entity.drone.DroneType type, Rarity rarity) {
        return ITEMS.register(name, () -> new fr.modeminecraft.entity.drone.DroneItem(type, new Item.Properties().rarity(rarity)));
    }

    private static DeferredItem<Item> droneModule(String name, fr.modeminecraft.entity.drone.DroneModuleItem.Kind kind) {
        return ITEMS.register(name, () -> new fr.modeminecraft.entity.drone.DroneModuleItem(kind, new Item.Properties()));
    }

    public static final DeferredItem<Item> HUNTING_CONTRACT = ITEMS.register("hunting_contract",
            () -> new fr.modeminecraft.item.HuntingContractItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> TRACKING_COMPASS = ITEMS.register("tracking_compass",
            () -> new fr.modeminecraft.item.TrackingCompassItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> FALCON_INSIGNIA = ITEMS.registerSimpleItem("falcon_insignia", new Item.Properties().rarity(Rarity.RARE));
    // Œufs d'apparition des bots (une faction chacun, rôle tiré au hasard)
    public static final java.util.List<DeferredItem<Item>> BOT_EGGS = new java.util.ArrayList<>();
    static {
        for (var f : fr.modeminecraft.entity.bot.Faction.values()) {
            BOT_EGGS.add(ITEMS.register("bot_egg_" + f.id, () -> new fr.modeminecraft.item.BotSpawnEggItem(f, null, new Item.Properties())));
        }
        BOT_EGGS.add(ITEMS.register("bot_egg_falcon_chief", () -> new fr.modeminecraft.item.BotSpawnEggItem(
                fr.modeminecraft.entity.bot.Faction.FALCON, fr.modeminecraft.entity.bot.BotRole.CHIEF, new Item.Properties())));
        BOT_EGGS.add(ITEMS.register("bot_egg_falcon_squad", () -> new fr.modeminecraft.item.BotSpawnEggItem(
                fr.modeminecraft.entity.bot.Faction.FALCON, fr.modeminecraft.entity.bot.BotRole.COMMANDER, new Item.Properties())));
    }

    private static DeferredItem<AttachmentItem> attachment(String name, AttachmentItem.Stats stats) {
        return ITEMS.register(name, () -> new AttachmentItem(stats, new Item.Properties()));
    }

    private ModItems() {}
}

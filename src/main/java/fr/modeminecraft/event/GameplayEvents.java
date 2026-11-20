package fr.modeminecraft.event;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.entity.ai.GunAttackGoal;
import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunStats;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gameplay autour des armes :
 * - monstres armés : certains zombies (pistolets), squelettes (fusils à verrou) et pillards (fusils d'assaut,
 *   fusils à pompe, pistolets-mitrailleurs) apparaissent avec une arme à feu et s'en servent ;
 * - ils lâchent parfois leur arme (8 %) et souvent des munitions (50 %) ;
 * - armes, munitions, accessoires et grenades dans les coffres des structures ;
 * - les éliminations par roquette, plasma, carreau... comptent aussi pour l'arme tenue.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class GameplayEvents {
    private static final List<String> ZOMBIE_GUNS = List.of("glock17", "colt_m1911", "beretta_m9", "makarov", "sawed_off");
    private static final List<String> SKELETON_GUNS = List.of("mosin_nagant", "kar98k", "m1_garand", "m24");
    private static final List<String> PILLAGER_GUNS = List.of("ak47", "ak74", "remington_870", "uzi", "mp5", "thompson", "ppsh41");

    /** Manuel d'armement offert à la première connexion (désactivable : config/modeminecraft-common.toml). */
    @SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !fr.modeminecraft.ModConfigs.GIVE_MANUAL.get()) {
            return;
        }
        var data = player.getPersistentData();
        var persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.getBoolean("modeminecraft_manual")) {
            persisted.putBoolean("modeminecraft_manual", true);
            data.put(Player.PERSISTED_NBT_TAG, persisted);
            ItemStack manual = new ItemStack(ModItems.MILITARY_FIELD_MANUAL.get());
            if (!player.getInventory().add(manual)) {
                player.drop(manual, false);
            }
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!(mob instanceof Zombie || mob instanceof AbstractSkeleton || mob instanceof Pillager)) {
            return;
        }
        mob.goalSelector.addGoal(1, new GunAttackGoal(mob));
        if (event.loadedFromDisk()) {
            return;
        }
        boolean hard = event.getLevel().getDifficulty() == Difficulty.HARD;
        float chance;
        List<String> pool;
        if (mob instanceof Pillager) {
            chance = 0.25f;
            pool = PILLAGER_GUNS;
        } else if (mob instanceof AbstractSkeleton) {
            chance = 0.04f;
            pool = SKELETON_GUNS;
        } else {
            if (!mob.getMainHandItem().isEmpty() || mob.isBaby()) {
                return;
            }
            chance = 0.04f;
            pool = ZOMBIE_GUNS;
        }
        if (mob.getRandom().nextFloat() >= chance * (hard ? 2 : 1)) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(ModeMinecraft.id(pool.get(mob.getRandom().nextInt(pool.size()))));
        if (!(item instanceof GunItem gun)) {
            return;
        }
        ItemStack stack = new ItemStack(gun);
        GunItem.setAmmo(stack, gun.stats().magazineSize());
        GunItem.setChambered(stack, true);
        mob.setItemSlot(EquipmentSlot.MAINHAND, stack);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.08f);
    }

    /** Les monstres armés lâchent souvent des munitions de leur arme. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e instanceof Mob) || !(e.getMainHandItem().getItem() instanceof GunItem gun) || !event.isRecentlyHit()) {
            return;
        }
        if (e.getRandom().nextFloat() < 0.5f) {
            ItemStack ammo = new ItemStack(gun.stats().ammo().get(), 4 + e.getRandom().nextInt(9));
            event.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY() + 0.5, e.getZ(), ammo));
        }
    }

    /** Éliminations sans balle (roquette, plasma, carreau, railgun) : elles comptent pour l'arme tenue. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof Player player) || player.level().isClientSide
                || source.getDirectEntity() instanceof BulletEntity || source.getDirectEntity() == player) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof GunItem) {
            GunItem.recordKill(held, player, false, event.getEntity().getMaxHealth() >= 50);
        }
    }

    // ------------------------------------------------------------------ butin des coffres
    private enum Tier { COMMON, MILITARY, RARE }

    private static final Map<String, Tier> CHESTS = Map.ofEntries(
            Map.entry("chests/simple_dungeon", Tier.COMMON), Map.entry("chests/abandoned_mineshaft", Tier.COMMON),
            Map.entry("chests/shipwreck_supply", Tier.COMMON), Map.entry("chests/village/village_weaponsmith", Tier.MILITARY),
            Map.entry("chests/pillager_outpost", Tier.MILITARY), Map.entry("chests/woodland_mansion", Tier.MILITARY),
            Map.entry("chests/desert_pyramid", Tier.COMMON), Map.entry("chests/jungle_temple", Tier.COMMON),
            Map.entry("chests/stronghold_corridor", Tier.MILITARY), Map.entry("chests/buried_treasure", Tier.RARE),
            Map.entry("chests/bastion_treasure", Tier.RARE), Map.entry("chests/ancient_city", Tier.RARE),
            Map.entry("chests/end_city_treasure", Tier.RARE), Map.entry("chests/trial_chambers/reward", Tier.MILITARY));

    private static final Set<GunStats.Category> COMMON_GUNS = Set.of(GunStats.Category.PISTOL, GunStats.Category.SHOTGUN);
    private static final Set<GunStats.Category> MILITARY_GUNS = Set.of(GunStats.Category.SMG, GunStats.Category.ASSAULT, GunStats.Category.SHOTGUN);

    @SubscribeEvent
    public static void onLootTable(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (!name.getNamespace().equals("minecraft")) {
            return;
        }
        if (name.getPath().equals("chests/spawn_bonus_chest")) {
            // option « Coffre bonus » : équipement de départ en plus des objets de Minecraft
            for (var e : java.util.List.of(Map.entry("combat_knife", 1), Map.entry("glock17", 1), Map.entry("ammo_9mm", 32))) {
                Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(fr.modeminecraft.ModeMinecraft.id(e.getKey()));
                event.getTable().addPool(LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(
                                net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(e.getValue())))).build());
            }
            return;
        }
        Tier tier = CHESTS.get(name.getPath());
        if (tier == null) {
            return;
        }
        LootPool.Builder pool = LootPool.lootPool().setRolls(UniformGenerator.between(1, tier == Tier.RARE ? 3 : 2))
                .when(LootItemRandomChanceCondition.randomChance(tier == Tier.COMMON ? 0.45f : 0.7f));
        for (var holder : ModItems.ITEMS.getEntries()) {
            Item item = holder.get();
            if (item instanceof GunItem gun) {
                GunStats.Category c = gun.stats().category();
                boolean ok = switch (tier) {
                    case COMMON -> COMMON_GUNS.contains(c);
                    case MILITARY -> MILITARY_GUNS.contains(c);
                    case RARE -> c == GunStats.Category.SNIPER || c == GunStats.Category.ASSAULT || c == GunStats.Category.MACHINE_GUN
                            || c == GunStats.Category.LAUNCHER;
                };
                if (ok && gun.stats().projectile() != GunStats.ProjectileKind.RAIL && gun.stats().projectile() != GunStats.ProjectileKind.PLASMA) {
                    pool.add(LootItem.lootTableItem(item).setWeight(2));
                }
            } else if (item instanceof fr.modeminecraft.item.AmmoItem) {
                pool.add(LootItem.lootTableItem(item).setWeight(6)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(6, tier == Tier.RARE ? 32 : 20))));
            } else if (item instanceof AttachmentItem && tier != Tier.COMMON) {
                pool.add(LootItem.lootTableItem(item).setWeight(1));
            } else if (item instanceof fr.modeminecraft.item.explosive.GrenadeItem && item != ModItems.BOMBLET.get()) {
                pool.add(LootItem.lootTableItem(item).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))));
            }
        }
        event.getTable().addPool(pool.build());
    }

    private GameplayEvents() {}
}

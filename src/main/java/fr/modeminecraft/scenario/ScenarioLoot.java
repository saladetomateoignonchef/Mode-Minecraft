package fr.modeminecraft.scenario;

import com.mojang.serialization.MapCodec;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.MountedGunItem;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.registry.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Économie du scénario « Course à l'arme nucléaire » (règle {@code modeminecraftScenario}) appliquée à tous les coffres
 * du monde, vanilla compris :
 * - aucune arme à feu (ni mitrailleuse démontée) : on ne s'en procure qu'à l'établi ou chez l'armurier de sa base ;
 * - le carburant (bidons, fûts de pétrole) ne se trouve que sur les plateformes offshore, les dépôts navals et dans les
 *   raffineries du Mesa ; ces coffres en reçoivent un supplément ;
 * - l'uranium ne se trouve que chez les milices du désert (dépôts plombés du palais, chambre forte du Dictateur).
 */
public class ScenarioLoot extends LootModifier {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ModeMinecraft.MODID);
    public static final MapCodec<ScenarioLoot> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> codecStart(i).apply(i, ScenarioLoot::new));
    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> SCENARIO_LOOT = SERIALIZERS.register("scenario_loot", () -> CODEC);

    /** Coffres où l'on peut voler du carburant. */
    private static final Set<String> FUEL = Set.of("offshore_supplies", "naval_depot", "boss_vault_offshore_vault",
            "boss_vault_badlands_refinery", "refinery_fuel");
    /** Coffres des milices du désert : seule source d'uranium. */
    private static final Set<String> URANIUM = Set.of("desert_uranium", "boss_vault_desert_palace");

    public ScenarioLoot(LootItemCondition[] conditions) {
        super(conditions);
    }

    public static boolean firearm(ItemStack s) {
        return s.getItem() instanceof GunItem || s.getItem() instanceof MountedGunItem;
    }

    static boolean fuel(ItemStack s) {
        return s.is(ModItems.JERRYCAN.get()) || s.is(ModItems.OIL_BARREL.get()) || s.is(ModItems.KEROSENE_TANK.get());
    }

    static boolean uranium(ItemStack s) {
        Item i = s.getItem();
        return i == ModItems.URANIUM_CANISTER.get() || i == ModItems.ENRICHED_URANIUM_CORE.get() || i == ModItems.RAW_URANIUM.get()
                || i == ModItems.URANIUM_INGOT.get();
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        if (!ctx.getLevel().getGameRules().getBoolean(Scenario.RULE)) {
            return loot;
        }
        ResourceLocation id = ctx.getQueriedLootTableId();
        if (!id.getPath().startsWith("chests/")) {
            return loot;
        }
        String name = id.getNamespace().equals(ModeMinecraft.MODID) ? id.getPath().substring("chests/".length()) : "";
        boolean fuelSite = FUEL.contains(name), uraniumSite = URANIUM.contains(name);
        loot.removeIf(s -> firearm(s) || !fuelSite && fuel(s) || !uraniumSite && uranium(s));
        RandomSource r = ctx.getRandom();
        if (fuelSite && !name.equals("refinery_fuel")) {
            loot.add(new ItemStack(ModItems.JERRYCAN.get(), 1 + r.nextInt(3)));
            if (r.nextFloat() < 0.6f) {
                loot.add(new ItemStack(ModItems.OIL_BARREL.get(), 1 + r.nextInt(2)));
            }
        }
        if (name.equals("boss_vault_desert_palace")) {
            loot.add(new ItemStack(ModItems.URANIUM_CANISTER.get(), 1 + r.nextInt(3)));
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    /** Identifiant d'objet (pour les tables générées). */
    static String id(Item i) {
        return BuiltInRegistries.ITEM.getKey(i).toString();
    }
}

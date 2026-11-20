package fr.modeminecraft.client.book;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.AmmoItem;
import fr.modeminecraft.item.BallisticPlateItem;
import fr.modeminecraft.item.HelmetItem;
import fr.modeminecraft.item.MountedGunItem;
import fr.modeminecraft.item.TurretAmmoItem;
import fr.modeminecraft.item.VestItem;
import fr.modeminecraft.item.explosive.GrenadeItem;
import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunStats;
import fr.modeminecraft.item.melee.MeleeWeaponItem;
import fr.modeminecraft.recipe.ArmamentRecipe;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Index du livre des recettes (lu dynamiquement : aucune recette n'est écrite en dur dans l'interface).
 * - tous les objets et blocs du mod, avec des mots-clés pour la recherche ("@sniper", "@wwii", "@kevlar", "9mm"...) ;
 * - les recettes du RecipeManager du client (établi, fours, table d'armement) dont l'identifiant est celui du mod :
 *   un datapack qui modifie ou ajoute une recette est donc pris en compte automatiquement.
 */
public final class RecipeIndex {
    /** Onglets de stations du livre. */
    public enum Station { CRAFTING, ARMAMENT, COOKING, WORLDGEN, STATS }

    public record Entry(ItemStack stack, String name, Set<String> keywords) {
        public boolean matches(String token) {
            if (token.startsWith("@")) {
                String k = token.substring(1);
                return !k.isEmpty() && keywords.stream().anyMatch(w -> w.startsWith(k));
            }
            return name.contains(token) || keywords.stream().anyMatch(w -> w.startsWith(token));
        }
    }

    private static List<Entry> entries;
    private static Object builtFor;

    /** Les objets d'époque Seconde Guerre mondiale (pour "@wwii"). */
    private static final Set<String> WWII = Set.of("thompson", "ppsh41", "kar98k", "mosin_nagant", "m1_garand", "luger_p08", "colt_m1911",
            "mg42", "flakvierling", "vickers", "m2_browning", "dshk", "m1_helmet", "stahlhelm_m40", "ssh40", "m12_flak_vest");

    public static String normalize(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public static synchronized List<Entry> all() {
        Object key = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getRecipeManager();
        if (entries == null || builtFor != key) {
            entries = build();
            builtFor = key;
        }
        return entries;
    }

    public static List<Entry> search(String query) {
        String q = normalize(query.trim());
        if (q.isEmpty()) {
            return all();
        }
        String[] tokens = q.split("\\s+");
        List<Entry> out = new ArrayList<>();
        for (Entry e : all()) {
            boolean ok = true;
            for (String t : tokens) {
                ok &= e.matches(t);
            }
            if (ok) {
                out.add(e);
            }
        }
        return out;
    }

    private static List<Entry> build() {
        List<Entry> list = new ArrayList<>();
        Set<Item> kevlarBased = kevlarBased();
        for (var holder : ModItems.ITEMS.getEntries()) {
            Item item = holder.get();
            ItemStack stack = new ItemStack(item);
            String id = BuiltInRegistries.ITEM.getKey(item).getPath();
            Set<String> k = new LinkedHashSet<>();
            for (String part : id.split("_")) {
                k.add(part);
            }
            if (WWII.contains(id)) {
                k.add("wwii");
                k.add("ww2");
            }
            if (kevlarBased.contains(item) || id.contains("kevlar")) {
                k.add("kevlar");
            }
            if (item instanceof GunItem gun) {
                GunStats s = gun.stats();
                k.addAll(List.of("gun", "arme", "feu"));
                k.add(s.category().name().toLowerCase(Locale.ROOT));
                k.addAll(switch (s.category()) {
                    case PISTOL -> List.of("pistolet", "revolver");
                    case SMG -> List.of("pm", "smg", "mitraillette");
                    case ASSAULT -> List.of("fusil", "assaut", "rifle");
                    case SNIPER -> List.of("sniper", "precision");
                    case SHOTGUN -> List.of("pompe", "shotgun");
                    case MACHINE_GUN -> List.of("mitrailleuse", "lmg");
                    case LAUNCHER -> List.of("lanceur", "roquette", "launcher");
                });
                caliber(k, BuiltInRegistries.ITEM.getKey(s.ammo().get()).getPath());
            } else if (item instanceof MeleeWeaponItem) {
                k.addAll(List.of("melee", "blanche", "lame"));
            } else if (item instanceof GrenadeItem || id.contains("c4") || id.contains("claymore") || id.contains("mortar") || id.contains("detonator")) {
                k.addAll(List.of("grenade", "explosif", "explosive"));
            } else if (item instanceof AmmoItem || item instanceof TurretAmmoItem) {
                k.addAll(List.of("ammo", "munition", "balle"));
                caliber(k, id);
            } else if (item instanceof AttachmentItem a) {
                k.addAll(List.of("attachment", "accessoire", a.slot().getSerializedName()));
            } else if (item instanceof HelmetItem h) {
                k.addAll(List.of("helmet", "casque", "armor", "protection"));
                k.addAll(era(h.stats().tier(), true));
            } else if (item instanceof VestItem v) {
                k.addAll(List.of("vest", "gilet", "armor", "protection"));
                k.addAll(era(v.stats().tier(), false));
            } else if (item instanceof fr.modeminecraft.item.GearItem gear) {
                k.addAll(gear.isBoots() ? List.of("boots", "rangers", "bottes", "chaussures") : List.of("pants", "pantalon", "jambieres", "treillis"));
                k.addAll(List.of("armor", "protection", "equipement"));
                k.addAll(era(gear.stats().tier, false));
            } else if (item instanceof BallisticPlateItem) {
                k.addAll(List.of("plate", "plaque", "balistique", "protection"));
            } else if (item instanceof MountedGunItem) {
                k.addAll(List.of("turret", "tourelle", "mitrailleuse"));
            } else if (item instanceof BlockItem) {
                k.addAll(List.of("block", "bloc"));
            } else {
                k.addAll(List.of("material", "materiau", "ingredient"));
            }
            for (String w : k.toArray(String[]::new)) {
                k.add(normalize(w));
            }
            list.add(new Entry(stack, normalize(stack.getHoverName().getString()), k));
        }
        return list;
    }

    private static void caliber(Set<String> k, String ammoId) {
        String c = ammoId.replace("ammo_box_", "").replace("ammo_belt_", "").replace("ammo_", "");
        k.add(c);
        k.add("calibre");
    }

    private static List<String> era(int tier, boolean helmet) {
        if (helmet) {
            return switch (tier) {
                case 1 -> List.of("ancient", "antiquite", "wwi", "ww1");
                case 2 -> List.of("wwii", "ww2");
                case 3 -> List.of("coldwar", "guerrefroide");
                default -> List.of("modern", "moderne");
            };
        }
        return switch (tier) {
            case 1 -> List.of("wwi", "ww1", "wwii", "ww2");
            case 2 -> List.of("coldwar", "guerrefroide");
            case 3 -> List.of("modern", "moderne");
            default -> List.of("prototype", "lourd");
        };
    }

    /** Objets dont une recette utilise du kevlar (pour "@kevlar"). */
    private static Set<Item> kevlarBased() {
        Set<Item> out = new HashSet<>();
        for (var h : recipes()) {
            for (Ingredient ing : ingredients(h.value())) {
                for (ItemStack s : ing.getItems()) {
                    if (BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains("kevlar")) {
                        out.add(result(h.value()).getItem());
                    }
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ recettes
    /** Recettes du mod connues du client (établi, fours, table d'armement). */
    public static List<RecipeHolder<?>> recipes() {
        var level = Minecraft.getInstance().level;
        List<RecipeHolder<?>> out = new ArrayList<>();
        if (level == null) {
            return out;
        }
        for (RecipeHolder<?> h : level.getRecipeManager().getRecipes()) {
            if (h.id().getNamespace().equals(ModeMinecraft.MODID) && station(h.value()) != null) {
                out.add(h);
            }
        }
        out.sort((a, b) -> a.id().compareTo(b.id()));
        return out;
    }

    public static Station station(Recipe<?> r) {
        if (r instanceof ArmamentRecipe) {
            return Station.ARMAMENT;
        }
        if (r instanceof AbstractCookingRecipe) {
            return Station.COOKING;
        }
        if (r instanceof CraftingRecipe && !(r instanceof CustomRecipe)) {
            return Station.CRAFTING;
        }
        return null;
    }

    public static ItemStack result(Recipe<?> r) {
        if (r instanceof ArmamentRecipe a) {
            return a.result();
        }
        var level = Minecraft.getInstance().level;
        return level == null ? ItemStack.EMPTY : r.getResultItem(level.registryAccess());
    }

    public static List<Ingredient> ingredients(Recipe<?> r) {
        if (r instanceof ArmamentRecipe a) {
            return a.ingredients().stream().map(si -> si.ingredient()).toList();
        }
        return r.getIngredients();
    }

    public static List<RecipeHolder<?>> producing(ItemStack stack, Station station) {
        List<RecipeHolder<?>> out = new ArrayList<>();
        for (var h : recipes()) {
            if (station(h.value()) == station && ItemStack.isSameItem(result(h.value()), stack)) {
                out.add(h);
            }
        }
        return out;
    }

    public static List<RecipeHolder<?>> using(ItemStack stack, Station station) {
        List<RecipeHolder<?>> out = new ArrayList<>();
        for (var h : recipes()) {
            if (station(h.value()) != station) {
                continue;
            }
            for (Ingredient ing : ingredients(h.value())) {
                if (!ing.isEmpty() && ing.test(stack)) {
                    out.add(h);
                    break;
                }
            }
        }
        return out;
    }

    /** Minerais : informations de génération naturelle. */
    public static boolean hasWorldGen(ItemStack stack) {
        return stack.is(ModItems.TITANIUM_ORE.get()) || stack.is(ModItems.DEEPSLATE_TITANIUM_ORE.get()) || stack.is(ModItems.RAW_TITANIUM.get())
                || stack.is(ModItems.RUBIS_ORE.get()) || stack.is(ModItems.DEEPSLATE_RUBIS_ORE.get()) || stack.is(ModItems.RUBIS.get());
    }

    private RecipeIndex() {}
}

package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ModeMinecraft.MODID);

    /** Armure en rubis : un peu plus solide que le diamant. Texture : textures/models/armor/rubis_layer_1/2.png */
    public static final Holder<ArmorMaterial> RUBIS = ARMOR_MATERIALS.register("rubis", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 8);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 11);
            }),
            15,                                  // enchantabilité
            SoundEvents.ARMOR_EQUIP_DIAMOND,     // son quand on l'équipe
            () -> Ingredient.of(ModItems.RUBIS.get()),
            List.of(new ArmorMaterial.Layer(ModeMinecraft.id("rubis"))),
            2.5f,                                // robustesse (diamant = 2)
            0.05f));                             // résistance au recul

    /** Tenues d'infiltration (déguisements de faction, mise à jour 0.5) : uniformes en tissu, sauf la tenue du Boss du Désert. */
    public static final Holder<ArmorMaterial> POLICE_UNIFORM = outfit("police_uniform", 1, 3, 2, 1, 0f);
    public static final Holder<ArmorMaterial> DESERT_BOSS = outfit("desert_boss", 3, 7, 6, 3, 1.5f);
    public static final Holder<ArmorMaterial> SOVIET_UNIFORM = outfit("soviet_uniform", 2, 4, 3, 1, 0f);
    public static final Holder<ArmorMaterial> SPECIAL_FORCES = outfit("special_forces", 2, 4, 3, 1, 0f);
    public static final Holder<ArmorMaterial> CARTEL_OUTFIT = outfit("cartel_outfit", 1, 3, 2, 1, 0f);
    public static final Holder<ArmorMaterial> SHERIFF_OUTFIT = outfit("sheriff_outfit", 2, 3, 2, 1, 0f);

    private static Holder<ArmorMaterial> outfit(String name, int helmet, int chest, int legs, int boots, float toughness) {
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                    map.put(ArmorItem.Type.BOOTS, boots);
                    map.put(ArmorItem.Type.LEGGINGS, legs);
                    map.put(ArmorItem.Type.CHESTPLATE, chest);
                    map.put(ArmorItem.Type.HELMET, helmet);
                    map.put(ArmorItem.Type.BODY, chest);
                }),
                12, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(net.minecraft.world.item.Items.LEATHER),
                List.of(new ArmorMaterial.Layer(ModeMinecraft.id(name))), toughness, 0f));
    }

    private ModArmorMaterials() {}
}

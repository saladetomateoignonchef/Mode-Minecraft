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

    private ModArmorMaterials() {}
}

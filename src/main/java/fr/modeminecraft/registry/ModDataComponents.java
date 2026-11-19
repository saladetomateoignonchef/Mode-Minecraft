package fr.modeminecraft.registry;

import com.mojang.serialization.Codec;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.FireMode;
import fr.modeminecraft.item.gun.GunAttachments;
import fr.modeminecraft.item.gun.GunSkin;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Données stockées directement dans un ItemStack (sauvegardées + synchronisées avec le client).
 */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ModeMinecraft.MODID);

    /** Nombre de balles dans le chargeur d'une arme à feu. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AMMO =
            DATA_COMPONENTS.registerComponentType("ammo", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Accessoires montés sur une arme. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunAttachments>> ATTACHMENTS =
            DATA_COMPONENTS.registerComponentType("attachments", builder -> builder
                    .persistent(GunAttachments.CODEC)
                    .networkSynchronized(GunAttachments.STREAM_CODEC));

    /** Skin (apparence) d'une arme. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunSkin>> SKIN =
            DATA_COMPONENTS.registerComponentType("skin", builder -> builder
                    .persistent(GunSkin.CODEC)
                    .networkSynchronized(GunSkin.STREAM_CODEC));

    /** Mode de tir choisi (coup par coup, rafale, automatique). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FireMode>> FIRE_MODE =
            DATA_COMPONENTS.registerComponentType("fire_mode", builder -> builder
                    .persistent(FireMode.CODEC)
                    .networkSynchronized(FireMode.STREAM_CODEC));

    private ModDataComponents() {}
}

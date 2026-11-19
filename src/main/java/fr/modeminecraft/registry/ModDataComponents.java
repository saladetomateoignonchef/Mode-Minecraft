package fr.modeminecraft.registry;

import com.mojang.serialization.Codec;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.AmmoType;
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

    /** Type d'une munition (sur les piles de munitions) ou des balles chargées (sur les armes). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AmmoType>> AMMO_TYPE =
            DATA_COMPONENTS.registerComponentType("ammo_type", b -> b.persistent(AmmoType.CODEC).networkSynchronized(AmmoType.STREAM_CODEC));

    /** Une balle est-elle dans la chambre ? */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CHAMBERED =
            DATA_COMPONENTS.registerComponentType("chambered", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Instants (temps du monde, en ticks) des animations : armement, vérification de chambre, rechargement. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RACK_AT = timestamp("rack_at");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> CHECK_AT = timestamp("check_at");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RELOAD_START = timestamp("reload_start");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RELOAD_END = timestamp("reload_end");

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Long>> timestamp(String name) {
        // Non sauvegardé sur le disque : ne sert qu'aux animations
        return DATA_COMPONENTS.registerComponentType(name, b -> b.networkSynchronized(ByteBufCodecs.VAR_LONG));
    }

    private ModDataComponents() {}
}

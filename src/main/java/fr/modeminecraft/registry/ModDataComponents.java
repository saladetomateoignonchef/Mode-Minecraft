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

    /** Lance-grenades / fusil à pompe sous le canon chargé ? */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UNDERBARREL_LOADED =
            DATA_COMPONENTS.registerComponentType("underbarrel_loaded", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Laser allumé ? (touche T ; allumé par défaut) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LASER_ON =
            DATA_COMPONENTS.registerComponentType("laser_on", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Nombre de tirs effectués avec un silencieux qui s'use (silencieux artisanal). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MUZZLE_WEAR =
            DATA_COMPONENTS.registerComponentType("muzzle_wear", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Chaleur du canon (0 à 100) au moment HEAT_TIME ; elle redescend ensuite toute seule. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> HEAT =
            DATA_COMPONENTS.registerComponentType("heat", b -> b.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> HEAT_TIME = timestamp("heat_time");
    /** Le canon a surchauffé : on ne peut plus tirer tant qu'il n'a pas refroidi. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> OVERHEATED =
            DATA_COMPONENTS.registerComponentType("overheated", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> BARREL_SWAP_AT = timestamp("barrel_swap_at");

    /** Instants (temps du monde, en ticks) des animations : armement, vérification de chambre, rechargement. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RACK_AT = timestamp("rack_at");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> CHECK_AT = timestamp("check_at");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RELOAD_START = timestamp("reload_start");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RELOAD_END = timestamp("reload_end");
    /** Rechargement cartouche par cartouche : munitions avant / après (pour pouvoir l'interrompre). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RELOAD_FROM =
            DATA_COMPONENTS.registerComponentType("reload_from", b -> b.networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RELOAD_TARGET =
            DATA_COMPONENTS.registerComponentType("reload_target", b -> b.networkSynchronized(ByteBufCodecs.VAR_INT));

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Long>> timestamp(String name) {
        // Non sauvegardé sur le disque : ne sert qu'aux animations
        return DATA_COMPONENTS.registerComponentType(name, b -> b.networkSynchronized(ByteBufCodecs.VAR_LONG));
    }

    private ModDataComponents() {}
}

package fr.modeminecraft.item.gun;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Accessoires montés sur une arme (stockés dans l'ItemStack de l'arme).
 * Objet immuable : chaque modification crée une nouvelle instance.
 */
public record GunAttachments(Map<AttachmentSlot, Item> parts) {
    public static final GunAttachments EMPTY = new GunAttachments(Map.of());

    public static final Codec<GunAttachments> CODEC =
            Codec.unboundedMap(AttachmentSlot.CODEC, BuiltInRegistries.ITEM.byNameCodec())
                    .xmap(GunAttachments::new, GunAttachments::parts);

    public static final StreamCodec<RegistryFriendlyByteBuf, GunAttachments> STREAM_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, AttachmentSlot, Item, Map<AttachmentSlot, Item>>map(
                            HashMap::new, AttachmentSlot.STREAM_CODEC.cast(), ByteBufCodecs.registry(Registries.ITEM))
                    .map(GunAttachments::new, GunAttachments::parts);

    public GunAttachments {
        parts = Map.copyOf(parts);
    }

    public Optional<Item> get(AttachmentSlot slot) {
        return Optional.ofNullable(parts.get(slot));
    }

    public GunAttachments with(AttachmentSlot slot, Item item) {
        Map<AttachmentSlot, Item> copy = new EnumMap<>(AttachmentSlot.class);
        copy.putAll(parts);
        if (item == null) {
            copy.remove(slot);
        } else {
            copy.put(slot, item);
        }
        return new GunAttachments(copy);
    }

    public boolean isEmpty() {
        return parts.isEmpty();
    }
}

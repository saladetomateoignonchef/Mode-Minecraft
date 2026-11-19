package fr.modeminecraft.item.gun;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/** Les emplacements d'accessoires d'une arme. */
public enum AttachmentSlot implements StringRepresentable {
    SIGHT("sight"),       // viseur
    MUZZLE("muzzle"),     // bouche du canon (silencieux, compensateur)
    GRIP("grip"),         // poignée
    MAGAZINE("magazine"); // chargeur

    public static final Codec<AttachmentSlot> CODEC = StringRepresentable.fromEnum(AttachmentSlot::values);
    public static final StreamCodec<FriendlyByteBuf, AttachmentSlot> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(AttachmentSlot.class);

    private final String name;

    AttachmentSlot(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String translationKey() {
        return "attachment_slot.modeminecraft." + name;
    }
}

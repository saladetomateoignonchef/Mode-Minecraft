package fr.modeminecraft.item.gun;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * Skins des armes à feu. Chaque skin a sa palette de couleurs (générée par tools/generate_gun_models.py).
 * Pour ajouter un skin : l'ajouter ici + dans SKINS du script Python, puis relancer le script.
 */
public enum GunSkin implements StringRepresentable {
    DEFAULT("default"),
    DESERT("desert"),
    WOODLAND("woodland"),
    ARCTIC("arctic"),
    GOLD("gold"),
    RUBY("ruby");

    public static final Codec<GunSkin> CODEC = StringRepresentable.fromEnum(GunSkin::values);
    public static final StreamCodec<FriendlyByteBuf, GunSkin> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(GunSkin.class);

    private final String name;

    GunSkin(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String translationKey() {
        return "skin.modeminecraft." + name;
    }

    public GunSkin next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public GunSkin previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }
}

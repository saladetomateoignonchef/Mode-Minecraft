package fr.modeminecraft.item.gun;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/** Mode de tir d'une arme (changé avec la touche B). */
public enum FireMode implements StringRepresentable {
    SEMI("semi"),   // coup par coup : une balle par clic
    BURST("burst"), // rafale : quelques balles par clic
    AUTO("auto");   // automatique : tire tant que le clic est maintenu

    public static final Codec<FireMode> CODEC = StringRepresentable.fromEnum(FireMode::values);
    public static final StreamCodec<FriendlyByteBuf, FireMode> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(FireMode.class);

    private final String name;

    FireMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Nom complet ("Automatique"). */
    public String translationKey() {
        return "fire_mode.modeminecraft." + name;
    }

    /** Nom court pour le HUD ("AUTO"). */
    public String shortKey() {
        return "fire_mode.modeminecraft." + name + ".short";
    }
}

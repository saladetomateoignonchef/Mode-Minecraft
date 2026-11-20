package fr.modeminecraft.item.gun;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/** Mode d'une lampe tactique montée sur une arme. */
public enum LightMode implements StringRepresentable {
    OFF("off"),
    ON("on"),           // éclairage continu
    STROBE("strobe"),   // stroboscope : 10 flashs par seconde, éblouit
    IR("ir");           // infrarouge : invisible à l'œil nu, visible avec des jumelles de vision nocturne

    public static final Codec<LightMode> CODEC = StringRepresentable.fromEnum(LightMode::values);
    public static final StreamCodec<FriendlyByteBuf, LightMode> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(LightMode.class);

    private final String name;

    LightMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Lumière visible (blocs éclairés, faisceau vu par tout le monde) ? */
    public boolean visible() {
        return this == ON || this == STROBE;
    }
}

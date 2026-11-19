package fr.modeminecraft.item.gun;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * Types de munitions. Une munition spéciale = une munition normale avec ce type (composant AMMO_TYPE).
 * Les effets au moment de l'impact sont dans BulletEntity ; ceux au moment du tir dans GunItem.
 */
public enum AmmoType implements StringRepresentable {
    //            nom              fusil à pompe ?  dégâts  vitesse  dispersion  recul  volume  couleur
    STANDARD("standard",        Use.ALL,      1.0f,  1.0f,  1.0f,  1.0f,  1.0f,  0xFFFFFF),
    ARMOR_PIERCING("ap",        Use.BULLETS,  0.9f,  1.1f,  1.0f,  1.0f,  1.0f,  0x3C8CFF),
    EXPLOSIVE("he",             Use.BULLETS,  0.7f,  1.0f,  1.0f,  1.1f,  1.0f,  0xFF3C1E),
    TRACER("tracer",            Use.BULLETS,  1.0f,  1.0f,  1.0f,  1.0f,  1.0f,  0xFFA020),
    INCENDIARY("incendiary",    Use.BULLETS,  0.9f,  1.0f,  1.0f,  1.0f,  1.0f,  0xFF6A00),
    HOLLOW_POINT("hollow_point", Use.BULLETS, 1.0f,  1.0f,  1.0f,  1.0f,  1.0f,  0xC0C0C0),
    SUBSONIC("subsonic",        Use.BULLETS,  0.95f, 0.7f,  1.0f,  0.8f,  0.25f, 0x5A5A70),
    RUBBER("rubber",            Use.BULLETS,  0.25f, 0.8f,  1.0f,  0.8f,  0.9f,  0x303030),
    MARKER("marker",            Use.BULLETS,  0.9f,  1.0f,  1.0f,  1.0f,  1.0f,  0xFFFF40),
    VENOM("venom",              Use.BULLETS,  0.9f,  1.0f,  1.0f,  1.0f,  1.0f,  0x55B335),
    CRYO("cryo",                Use.BULLETS,  0.9f,  1.0f,  1.0f,  1.0f,  1.0f,  0x7CC4EA),
    ELECTRIC("electric",        Use.BULLETS,  0.9f,  1.0f,  1.0f,  1.0f,  1.0f,  0xB070FF),
    RUBY("ruby",                Use.BULLETS,  1.25f, 1.1f,  0.9f,  1.0f,  1.0f,  0xC8223B),
    SLUG("slug",                Use.SHELLS,   1.0f,  1.2f,  0.15f, 1.1f,  1.0f,  0x9AA3AD),
    FLECHETTE("flechette",      Use.SHELLS,   0.4f,  1.3f,  0.6f,  0.9f,  0.9f,  0xD0D0D0),
    DRAGON_BREATH("dragon_breath", Use.SHELLS, 0.5f, 0.6f,  1.3f,  1.0f,  1.2f,  0xFF4000),
    MAGNUM("magnum",            Use.SHELLS,   1.3f,  1.05f, 1.0f,  1.3f,  1.2f,  0x8A1426);

    /** Sur quelles munitions ce type existe. */
    public enum Use { ALL, BULLETS, SHELLS }

    public static final Codec<AmmoType> CODEC = StringRepresentable.fromEnum(AmmoType::values);
    public static final StreamCodec<FriendlyByteBuf, AmmoType> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(AmmoType.class);

    private final String name;
    public final Use use;
    public final float damageMul, velocityMul, spreadMul, recoilMul, volumeMul;
    public final int color;

    AmmoType(String name, Use use, float damageMul, float velocityMul, float spreadMul, float recoilMul,
             float volumeMul, int color) {
        this.name = name;
        this.use = use;
        this.damageMul = damageMul;
        this.velocityMul = velocityMul;
        this.spreadMul = spreadMul;
        this.recoilMul = recoilMul;
        this.volumeMul = volumeMul;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String translationKey() {
        return "ammo_type.modeminecraft." + name;
    }

    public String descriptionKey() {
        return "ammo_type.modeminecraft." + name + ".desc";
    }

    /** Ce type existe-t-il pour ce genre de munition ? */
    public boolean appliesTo(boolean shotgunShell) {
        return use == Use.ALL || (use == Use.SHELLS) == shotgunShell;
    }

    /** Nombre de projectiles d'une cartouche de fusil à pompe selon le type. */
    public int pellets(int base) {
        return switch (this) {
            case SLUG -> 1;
            case FLECHETTE -> base * 5 / 2;
            default -> base;
        };
    }

    /** Dégâts par projectile (le slug regroupe la puissance de tous les plombs). */
    public float pelletDamage(float base, int basePellets) {
        return this == SLUG ? base * basePellets * 0.85f : base * damageMul;
    }
}

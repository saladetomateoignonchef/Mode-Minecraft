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
    RUBY("ruby"),
    URBAN("urban"),
    TIGER("tiger"),
    CARBON("carbon", 3),   // débloqué au niveau 3 de l'arme
    DIAMOND("diamond", 5), // débloqué au niveau 5
    // Skins des armes légendaires des Chefs de Faction (uniquement sur leur arme, jamais au choix)
    PRESTIGE("prestige", "glock18"),
    SOUVERAIN("souverain", "ak47"),
    FANTOME("fantome", "vss"),
    HIVER("hiver", "pkm"),
    CARTEL("cartel", "desert_eagle");

    /** Objets dessinés comme des armes mais sans skins. */
    private static final java.util.Set<String> NO_SKINS = java.util.Set.of("emp_jammer_rifle");

    public static final Codec<GunSkin> CODEC = StringRepresentable.fromEnum(GunSkin::values);
    public static final StreamCodec<FriendlyByteBuf, GunSkin> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(GunSkin.class);

    private final String name;
    private final int requiredLevel;
    private final String onlyFor;

    GunSkin(String name) {
        this(name, 1);
    }

    GunSkin(String name, int requiredLevel) {
        this.name = name;
        this.requiredLevel = requiredLevel;
        this.onlyFor = null;
    }

    GunSkin(String name, String onlyFor) {
        this.name = name;
        this.requiredLevel = 1;
        this.onlyFor = onlyFor;
    }

    /** Skin d'arme légendaire (réservé à une arme, non sélectionnable). */
    public boolean legendary() {
        return onlyFor != null;
    }

    /** Ce skin existe-t-il (modèles générés) pour cette arme ? */
    public boolean availableFor(String gun) {
        if (onlyFor != null) {
            return onlyFor.equals(gun);
        }
        return this == DEFAULT || !NO_SKINS.contains(gun);
    }

    /** Niveau d'arme nécessaire pour utiliser ce skin. */
    public int requiredLevel() {
        return requiredLevel;
    }

    /** Skin suivant (ou précédent si step = -1) accessible pour une arme de ce niveau. */
    public GunSkin cycle(int step, int level) {
        GunSkin s = this;
        do {
            s = values()[(s.ordinal() + values().length + step) % values().length];
        } while ((s.requiredLevel > level || s.legendary()) && s != this);
        return s;
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

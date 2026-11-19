package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * Types de dégâts du mod. Le type lui-même est défini en JSON :
 * data/modeminecraft/damage_type/bullet.json (+ message de mort dans les fichiers de langue).
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> BULLET =
            ResourceKey.create(Registries.DAMAGE_TYPE, ModeMinecraft.id("bullet"));

    /** Balle perforante (MP7) : ignore l'armure. */
    public static final ResourceKey<DamageType> BULLET_AP =
            ResourceKey.create(Registries.DAMAGE_TYPE, ModeMinecraft.id("bullet_ap"));

    private ModDamageTypes() {}
}

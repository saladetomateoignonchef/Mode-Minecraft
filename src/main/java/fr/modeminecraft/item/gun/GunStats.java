package fr.modeminecraft.item.gun;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Statistiques de base d'une arme (sans accessoires).
 * Se construit avec un builder lisible, par exemple :
 * <pre>
 * GunStats.builder().damage(7).magazine(30).fireDelay(3).automatic()
 *         .ammo(ModItems.AMMO_762).slots(AttachmentSlot.values()).build()
 * </pre>
 */
public record GunStats(
        float damage,          // dégâts par balle (1 coeur = 2)
        int pellets,           // projectiles par tir (fusil à pompe > 1)
        int magazineSize,      // capacité du chargeur d'origine
        int fireDelay,         // ticks entre deux tirs (20 ticks = 1 seconde)
        int reloadTicks,       // durée du rechargement
        float velocity,        // vitesse du projectile (flèche d'arc tendu = 3)
        float spread,          // dispersion en tir normal
        float aimSpread,       // dispersion accroupi (visée)
        float recoil,          // recul de la caméra en degrés
        boolean automatic,     // tir continu en maintenant le clic droit
        Supplier<? extends Item> ammo,
        Supplier<SoundEvent> sound, float volume, float pitch,
        float explosionPower,  // > 0 pour les lance-roquettes
        float scopeZoom,       // zoom intégré quand on vise (1 = aucun ; 0.2 = x5 pour le sniper)
        Set<AttachmentSlot> slots) { // emplacements d'accessoires disponibles

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float damage = 5, spread = 2, aimSpread = 1, recoil = 2, velocity = 4, volume = 0.5f, pitch = 1.5f;
        private float explosionPower = 0, scopeZoom = 1;
        private int pellets = 1, magazine = 10, fireDelay = 5, reload = 40;
        private boolean automatic;
        private Supplier<? extends Item> ammo;
        private Supplier<SoundEvent> sound;
        private final Set<AttachmentSlot> slots = EnumSet.noneOf(AttachmentSlot.class);

        public Builder damage(float v) { damage = v; return this; }
        public Builder pellets(int v) { pellets = v; return this; }
        public Builder magazine(int v) { magazine = v; return this; }
        public Builder fireDelay(int ticks) { fireDelay = ticks; return this; }
        public Builder reload(int ticks) { reload = ticks; return this; }
        public Builder velocity(float v) { velocity = v; return this; }
        public Builder spread(float normal, float aiming) { spread = normal; aimSpread = aiming; return this; }
        public Builder recoil(float degrees) { recoil = degrees; return this; }
        public Builder automatic() { automatic = true; return this; }
        public Builder ammo(Supplier<? extends Item> item) { ammo = item; return this; }
        public Builder sound(Supplier<SoundEvent> s, float vol, float pit) { sound = s; volume = vol; pitch = pit; return this; }
        public Builder explosion(float power) { explosionPower = power; return this; }
        public Builder scope(float zoom) { scopeZoom = zoom; return this; }
        public Builder slots(AttachmentSlot... s) { slots.addAll(Set.of(s)); return this; }

        public GunStats build() {
            return new GunStats(damage, pellets, magazine, fireDelay, reload, velocity, spread, aimSpread, recoil,
                    automatic, ammo, sound, volume, pitch, explosionPower, scopeZoom, Set.copyOf(slots));
        }
    }
}

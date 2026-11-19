package fr.modeminecraft.item.gun;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

import java.util.EnumSet;
import java.util.List;
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
        float explosionPower,  // puissance d'explosion (roquettes, grenades)
        ProjectileKind projectile, // balle, roquette ou grenade
        float scopeZoom,       // zoom intégré quand on vise (1 = aucun ; 0.2 = x5)
        SightOverlay scopeOverlay, // réticule de la lunette intégrée
        int burst,             // > 0 : nombre de balles par rafale (M16A4 = 3)
        int burstCooldown,     // pause après une rafale
        boolean reloadPerRound,// rechargement balle par balle (reloadTicks = durée PAR balle)
        int pierce,            // nombre d'ennemis traversés par une balle
        boolean armorPiercing, // ignore l'armure
        int spinUp,            // ticks de lancement avant de tirer (minigun)
        boolean disposable,    // arme jetable (AT4) : vendue chargée, détruite après le tir
        List<FireMode> fireModes, // modes de tir disponibles (touche B), dans l'ordre
        Casing casing,         // douille éjectée au tir
        boolean chamber,       // l'arme a une chambre : +1 balle, et il faut armer la culasse si elle est vide
        boolean slideLock,     // la culasse reste bloquée en arrière quand l'arme est vide (pistolets)
        Set<AttachmentSlot> slots) { // emplacements d'accessoires disponibles

    /** Douille éjectée au tir. */
    public enum Casing { NONE, BRASS, STEEL, SHELL }

    /** Type de projectile tiré. */
    public enum ProjectileKind { BULLET, ROCKET, GRENADE }

    /** Mode de tir d'une arme neuve : le plus "rapide" disponible. */
    public FireMode defaultMode() {
        return fireModes.get(fireModes.size() - 1);
    }

    /** Nombre de balles d'une rafale (3 si l'arme n'a pas de rafale spéciale). */
    public int burstSize() {
        return burst > 0 ? burst : 3;
    }

    public int burstPause() {
        return burstCooldown > 0 ? burstCooldown : 8;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float damage = 5, spread = 2, aimSpread = 1, recoil = 2, velocity = 4, volume = 0.5f, pitch = 1.5f;
        private float explosionPower = 0, scopeZoom = 1;
        private int pellets = 1, magazine = 10, fireDelay = 5, reload = 40;
        private int burst = 0, burstCooldown = 0, pierce = 0, spinUp = 0;
        private boolean automatic, reloadPerRound, armorPiercing, disposable;
        private ProjectileKind projectile = ProjectileKind.BULLET;
        private SightOverlay scopeOverlay = SightOverlay.NONE;
        private List<FireMode> modes;
        private Casing casing = Casing.BRASS;
        private boolean chamber = true, slideLock = false;
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
        public Builder rocket(float power) { projectile = ProjectileKind.ROCKET; explosionPower = power; return this; }
        public Builder grenade(float power) { projectile = ProjectileKind.GRENADE; explosionPower = power; return this; }
        /** Lunette ou viseur intégré : zoom (0.2 = x5) et réticule affiché quand on vise. */
        public Builder scope(float zoom, SightOverlay overlay) { scopeZoom = zoom; scopeOverlay = overlay; return this; }
        /** Rafale de n balles par appui, puis pause de cooldown ticks. */
        public Builder burst(int n, int cooldown) { burst = n; burstCooldown = cooldown; return this; }
        /** Rechargement balle par balle : ticksPerRound par balle. */
        public Builder reloadPerRound(int ticksPerRound) { reloadPerRound = true; reload = ticksPerRound; return this; }
        public Builder pierce(int entities) { pierce = entities; return this; }
        public Builder armorPiercing() { armorPiercing = true; return this; }
        /** Canons rotatifs : délai avant le premier tir. */
        public Builder spinUp(int ticks) { spinUp = ticks; automatic = true; return this; }
        public Builder disposable() { disposable = true; return this; }
        /** Force les modes de tir disponibles (sinon : déduits de automatic/burst). */
        public Builder modes(FireMode... m) { modes = List.of(m); return this; }
        public Builder casing(Casing c) { casing = c; return this; }
        /** Pas de chambre (revolver, lanceurs...) : on tire directement depuis le chargeur. */
        public Builder noChamber() { chamber = false; return this; }
        public Builder slideLock() { slideLock = true; return this; }
        public Builder slots(AttachmentSlot... s) { slots.addAll(Set.of(s)); return this; }
        /** Retire tous les emplacements (puis on peut en rajouter avec slots(...)). */
        public Builder noSlots() { slots.clear(); return this; }

        public GunStats build() {
            List<FireMode> fireModes = modes;
            if (fireModes == null) {
                if (spinUp > 0) {
                    fireModes = List.of(FireMode.AUTO);                                   // minigun
                } else if (automatic) {
                    fireModes = List.of(FireMode.SEMI, FireMode.BURST, FireMode.AUTO);    // armes automatiques
                } else if (burst > 0) {
                    fireModes = List.of(FireMode.SEMI, FireMode.BURST);                   // M16A4
                } else {
                    fireModes = List.of(FireMode.SEMI);                                   // pistolets, snipers...
                }
            }
            return new GunStats(damage, pellets, magazine, fireDelay, reload, velocity, spread, aimSpread, recoil,
                    automatic, ammo, sound, volume, pitch, explosionPower, projectile, scopeZoom, scopeOverlay,
                    burst, burstCooldown, reloadPerRound, pierce, armorPiercing, spinUp, disposable, fireModes,
                    projectile == ProjectileKind.BULLET ? casing : Casing.NONE, chamber && !disposable, slideLock, Set.copyOf(slots));
        }
    }
}

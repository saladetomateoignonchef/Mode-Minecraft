package fr.modeminecraft.item.gun;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Statistiques finales d'une arme, accessoires compris.
 * Calculées à la volée par {@link GunItem#resolve}.
 */
public record ResolvedGun(
        float damage, int magazineSize, int reloadTicks,
        float spread,          // dispersion sans viser
        float aimSpread,       // dispersion en visant
        float recoil,
        float volume, boolean silenced,
        List<SightMode> sightModes, // réglages du viseur (au moins un)
        float movingPenalty,   // multiplicateur de dispersion en marchant
        float steadyRecoil, float steadySpread, // stable = allongé, ou en visant sans bouger
        float mobility, float aimSpeed, float drawMul,
        Set<AttachmentItem.Special> specials) {

    /** Malus de dispersion en marchant, sans accessoire. */
    private static final float BASE_MOVING_PENALTY = 0.5f;

    static ResolvedGun of(GunStats base, GunAttachments attachments) {
        float damage = base.damage(), spread = base.spread(), aimSpread = base.aimSpread(), recoil = base.recoil();
        float reload = base.reloadTicks(), volume = base.volume();
        float moving = BASE_MOVING_PENALTY, steadyRecoil = 1, steadySpread = 1, mobility = 1, aimSpeed = 1, draw = 1;
        int magazine = base.magazineSize();
        boolean silenced = false;
        List<SightMode> modes = List.of(new SightMode(base.scopeZoom(), base.scopeOverlay()));
        Set<AttachmentItem.Special> specials = EnumSet.noneOf(AttachmentItem.Special.class);

        for (var item : attachments.parts().values()) {
            if (!(item instanceof AttachmentItem attachment)) {
                continue;
            }
            AttachmentItem.Stats s = attachment.stats();
            damage *= s.damageMul;
            spread *= s.spreadMul * s.hipSpreadMul;
            aimSpread *= s.spreadMul * s.aimSpreadMul;
            recoil *= s.recoilMul;
            reload *= s.reloadMul;
            volume *= s.volumeMul;
            moving *= s.movingSpreadMul;
            steadyRecoil *= s.steadyRecoilMul;
            steadySpread *= s.steadySpreadMul;
            mobility *= s.mobilityMul;
            aimSpeed *= s.aimSpeedMul;
            draw *= s.drawMul;
            silenced |= s.silenced;
            specials.addAll(s.specials);
            if (s.magazineSize > 0) {
                magazine = s.magazineSize;
            }
            if (s.magazineMul != 1) {
                magazine = Math.round(base.magazineSize() * s.magazineMul);
            }
            if (!s.modes.isEmpty()) {
                modes = s.modes;
            }
        }
        return new ResolvedGun(damage, magazine, Math.round(reload), spread, aimSpread, recoil, volume, silenced,
                modes, 1 + moving, steadyRecoil, steadySpread, mobility, aimSpeed, draw, specials);
    }

    /** Bonus du niveau de l'arme (voir GunProgress). */
    public ResolvedGun withLevel(int level) {
        if (level <= 1) {
            return this;
        }
        float s = GunProgress.spreadMul(level);
        return new ResolvedGun(damage, magazineSize, Math.round(reloadTicks * GunProgress.reloadMul(level)), spread * s, aimSpread * s,
                recoil * GunProgress.recoilMul(level), volume, silenced, sightModes, movingPenalty, steadyRecoil, steadySpread,
                mobility, aimSpeed, drawMul, specials);
    }

    public boolean has(AttachmentItem.Special special) {
        return specials.contains(special);
    }

    /** Réglage du viseur n° index (bouclé). */
    public SightMode sightMode(int index) {
        return sightModes.get(Math.floorMod(index, sightModes.size()));
    }

    /** Premier réglage (pour l'affichage des statistiques). */
    public float zoom() {
        return sightModes.get(0).zoom();
    }

    public SightOverlay overlay() {
        return sightModes.get(0).overlay();
    }
}

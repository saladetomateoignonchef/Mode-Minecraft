package fr.modeminecraft.item.gun;

/**
 * Statistiques finales d'une arme, accessoires compris.
 * Calculées à la volée par {@link GunItem#resolve}.
 */
public record ResolvedGun(
        float damage, int magazineSize, int reloadTicks,
        float spread, float aimSpread, float recoil,
        float volume, boolean silenced,
        float zoom, SightOverlay overlay) {

    static ResolvedGun of(GunStats base, GunAttachments attachments) {
        float damage = base.damage(), spread = base.spread(), aimSpread = base.aimSpread(), recoil = base.recoil();
        float reload = base.reloadTicks(), volume = base.volume();
        int magazine = base.magazineSize();
        boolean silenced = false;
        float zoom = base.scopeZoom();
        SightOverlay overlay = base.scopeOverlay();

        for (var item : attachments.parts().values()) {
            if (!(item instanceof AttachmentItem attachment)) {
                continue;
            }
            AttachmentItem.Stats s = attachment.stats();
            damage *= s.damageMul;
            spread *= s.spreadMul;
            aimSpread *= s.aimSpreadMul;
            recoil *= s.recoilMul;
            reload *= s.reloadMul;
            volume *= s.volumeMul;
            silenced |= s.silenced;
            if (s.magazineSize > 0) {
                magazine = s.magazineSize;
            }
            if (s.overlay != SightOverlay.NONE) {
                zoom = s.zoom;
                overlay = s.overlay;
            }
        }
        return new ResolvedGun(damage, magazine, Math.round(reload), spread, aimSpread, recoil, volume, silenced, zoom, overlay);
    }
}

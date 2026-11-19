package fr.modeminecraft.item.gun;

/** Un réglage de viseur : zoom (FOV : 1 = aucun, 0.25 = x4) et réticule affiché. */
public record SightMode(float zoom, SightOverlay overlay) {
    public static final SightMode NONE = new SightMode(1.0f, SightOverlay.NONE);

    /** Grossissement lisible ("x4"). */
    public float magnification() {
        return 1.0f / zoom;
    }
}

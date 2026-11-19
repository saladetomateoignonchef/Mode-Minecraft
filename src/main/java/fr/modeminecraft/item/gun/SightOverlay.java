package fr.modeminecraft.item.gun;

/** Ce qui s'affiche à l'écran quand on vise (accroupi) avec une arme. */
public enum SightOverlay {
    NONE(null),
    RED_DOT("textures/misc/red_dot.png"),
    HOLO("textures/misc/holo_reticle.png"),
    SCOPE_X4("textures/misc/acog_scope.png"),
    SNIPER("textures/misc/sniper_scope.png");

    /** Chemin de la texture (dans assets/modeminecraft), ou null. */
    public final String texture;

    SightOverlay(String texture) {
        this.texture = texture;
    }

    /** Les lunettes couvrent tout l'écran ; les viseurs ne dessinent qu'un réticule au centre. */
    public boolean isFullScope() {
        return this == SCOPE_X4 || this == SNIPER;
    }
}

package fr.modeminecraft.item.gun;

/** Ce qui s'affiche à l'écran quand on vise (accroupi) avec une arme. */
public enum SightOverlay {
    //            texture (textures/misc/)  plein écran ?  effet d'image (shader)
    NONE(null, false, null),
    RED_DOT("red_dot", false, null),
    HOLO("holo_reticle", false, null),
    KOBRA("kobra", false, null),
    MINI_DOT("mini_dot", false, null),
    GHOST_RING("ghost_ring", false, null),
    IRON(null, false, null),                       // viseur métallique : pas de réticule
    SCOPE_X4("acog_scope", true, null),
    SNIPER("sniper_scope", true, null),
    PRISM("prism", true, null),
    LPVO("lpvo", true, null),
    SCOPE_X6("scope_x6", true, null),
    SCOPE_X8("scope_x8", true, null),
    SCOPE_X12("scope_x12", true, null),
    PSO1("pso1", true, null),
    RANGEFINDER("rangefinder", true, null),
    ELCAN("elcan", true, null),
    HUNTING("hunting", true, null),
    DIGITAL("digital", true, null),
    RUBY("ruby_scope", true, null),
    HOLO_MAGNIFIED("holo_magnified", true, null),
    THERMAL("thermal", true, "thermal"),
    NIGHT_VISION("night_vision", true, "night_vision");

    /** Chemin de la texture (dans assets/modeminecraft), ou null. */
    public final String texture;
    private final boolean fullScope;
    /** Effet d'image (shaders/post/<nom>.json) appliqué quand on vise, ou null. */
    public final String postEffect;

    SightOverlay(String texture, boolean fullScope, String postEffect) {
        this.texture = texture == null ? null : "textures/misc/" + texture + ".png";
        this.fullScope = fullScope;
        this.postEffect = postEffect;
    }

    /** Les lunettes couvrent tout l'écran ; les viseurs ne dessinent qu'un réticule au centre. */
    public boolean isFullScope() {
        return fullScope;
    }
}

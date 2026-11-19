"""
Formes 3D des accessoires d'armes (utilisé par generate_gun_models.py).

Chaque accessoire est dessiné autour de son point de fixation (0, 0, 0), centré sur Z = 0 :
- viseurs (SIGHT) : posés sur le rail, le bas du viseur en y = 0 ;
- bouche du canon (MUZZLE) : part de l'extrémité du canon vers +X ;
- poignées avant (GRIP) : sous le garde-main, vers le bas (-Y) ;
- chargeurs (MAGAZINE) : sous le puits de chargeur, vers le bas ;
- poignées pistolet (PISTOL_GRIP) : le haut de la poignée en (0, 0), inclinée vers l'arrière ;
- crosses (STOCK) : partent de l'arrière du boîtier vers -X.

Couleurs : k/m/l/G métal, d noir, w/W bois, t/T/u kaki, g or, r rubis, s verre (voir gun_designs.py).
"""
from gun_designs import box


def zc(x0, y0, x1, y1, hw, c, rot=None):
    """Pavé centré sur Z = 0, de demi-largeur hw."""
    return box([x0, y0, -hw], [x1, y1, hw], c, rot)


# =====================================================================
# Constructeurs
# =====================================================================
def scope(length, r=0.8, c="k", obj=1.0, rings="m", lens="s", base_y=0.0):
    """Lunette : tube + oculaire + objectif + montages. length = longueur totale."""
    h = length / 2
    cy = base_y + 1.2 + r
    els = [
        zc(-h + 1.2, cy - r, h - 1.6, cy + r, r, c),
        zc(-h, cy - r - 0.2, -h + 1.2, cy + r + 0.2, r + 0.2, c),
        zc(h - 1.6, cy - r - 0.3 * obj, h, cy + r + 0.3 * obj, r + 0.3 * obj, c),
        zc(h - 0.05, cy - r, h + 0.05, cy + r, r, lens),
        zc(-h - 0.05, cy - r + 0.1, -h + 0.05, cy + r - 0.1, r - 0.1, lens),
        zc(-h + 2.0, base_y, -h + 2.8, cy - r, 0.45, rings),
        zc(h - 3.0, base_y, h - 2.2, cy - r, 0.45, rings),
        zc(-h + 1.8, base_y, h - 2.0, base_y + 0.4, 0.55, "k"),
    ]
    return els


def reflex(w=1.5, h=1.8, c="k", glass="s", base=True):
    """Viseur réflexe (point rouge) : socle + cadre + vitre."""
    els = [zc(-w, 0, w, 0.4, 0.75, c)] if base else []
    els += [
        box([-w * 0.6, 0.4, -0.75], [w * 0.6, 0.4 + h, -0.55], c),
        box([-w * 0.6, 0.4, 0.55], [w * 0.6, 0.4 + h, 0.75], c),
        zc(-w * 0.6, 0.4 + h - 0.25, w * 0.6, 0.4 + h, 0.75, c),
        zc(w * 0.5, 0.4, w * 0.55, 0.4 + h - 0.25, 0.55, glass),
    ]
    return els


def vgrip(length=3.6, c="k", hw=0.7, base="m"):
    """Poignée avant verticale."""
    return [zc(-hw, -length, hw, 0, hw, c), zc(-1.1, -0.4, 1.1, 0, 0.75, base),
            zc(-hw - 0.1, -length - 0.3, hw + 0.1, -length, hw + 0.1, c)]


def pgrip(c="k", length=3.6, w=1.6, extra=()):
    """Poignée pistolet inclinée vers l'arrière (comme celles des armes)."""
    rot = ("z", -22.5, [0, 0, 0])
    els = [zc(-w / 2, -length, w / 2, 0, 0.72, c, rot)]
    for frm, to, col in extra:
        els.append(box(frm, to, col, rot))
    return els


def solid_stock(c="k", length=6.0, y0=-1.6, y1=1.2, hw=0.8, pad="d", pad_len=0.3):
    """Crosse pleine (de 0 vers -length)."""
    return [zc(-length, y0, 0, y1, hw, c), zc(-length - pad_len, y0 - 0.2, -length, y1 + 0.1, hw + 0.05, pad)]


def tube_stock(c="k", body=(-6, -2.5)):
    """Crosse sur tube (type M4) : tube tampon + corps de crosse."""
    x0, x1 = body
    return [zc(-3.5, -0.45, 0, 0.45, 0.45, "k"), zc(x0, -1.5, x1, 1.0, 0.75, c),
            zc(x0 - 0.25, -1.7, x0, 1.1, 0.8, "d")]


# =====================================================================
# Accessoires
# =====================================================================
ACCESSORIES = {
    # ---------------- Viseurs ----------------
    "red_dot_sight": [
        # Tube creux : on voit à travers en visant (vitre à l'avant, traitement rouge du verre)
        zc(-1.5, 0, 1.5, 0.4, 0.7, "k"), zc(-0.8, 0.4, 1, 0.6, 0.7, "k"), zc(-0.8, 1.6, 1, 1.8, 0.7, "k"),
        box([-0.8, 0.6, -0.7], [1, 1.6, -0.5], "k"), box([-0.8, 0.6, 0.5], [1, 1.6, 0.7], "k"),
        zc(0.95, 0.6, 1.0, 1.6, 0.5, "s"), zc(1.0, 1.6, 1.3, 1.9, 0.72, "k"),
    ],
    "holo_sight": reflex(2.0, 2.0),
    "acog_scope": [
        zc(-1.5, 0, 1.5, 0.6, 0.6, "k"), zc(-3, 0.6, 3, 2.4, 0.9, "k"), zc(2, 0.4, 3.5, 2.6, 1.1, "k"),
        zc(3.5, 0.7, 3.55, 2.3, 0.8, "s"), zc(-3.5, 0.5, -2.5, 2.5, 1, "k"), zc(-3.55, 0.8, -3.5, 2.2, 0.7, "s"),
    ],
    "thermal_scope": [
        zc(-1.5, 0, 1.5, 0.6, 0.6, "m"), zc(-3, 0.6, 3, 2.9, 1.1, "k"), zc(-3.9, 1.0, -3, 2.5, 0.8, "d"),
        zc(3, 0.9, 3.4, 2.6, 1.0, "k"), zc(3.35, 1.1, 3.45, 2.4, 0.8, "s"),
        zc(-2, 2.9, 2, 3.2, 0.8, "m"), zc(-1.5, 3.2, -1.0, 3.4, 0.3, "r"),
    ],
    "night_vision_scope": [
        zc(-1.5, 0, 1.5, 0.6, 0.6, "k"), zc(-3, 0.7, 2.5, 2.5, 0.9, "t"), zc(2.5, 0.5, 3.8, 2.7, 1.1, "u"),
        zc(3.8, 0.8, 3.85, 2.4, 0.85, "s"), zc(-3.8, 0.9, -3, 2.3, 0.75, "d"), zc(-1, 2.5, 1, 2.9, 0.5, "u"),
    ],
    "kobra_sight": [
        # Le corps est sous la fenêtre : on voit à travers la vitre en visant
        zc(-1.6, 0, 1.6, 0.4, 0.9, "k"), zc(-1.6, 0.4, -0.5, 0.8, 0.9, "k"),
        box([-0.5, 0.4, -0.9], [0.6, 2.2, -0.7], "k"), box([-0.5, 0.4, 0.7], [0.6, 2.2, 0.9], "k"),
        zc(-0.5, 2.0, 0.6, 2.2, 0.9, "k"), zc(-0.5, 0.4, 0.6, 0.8, 0.9, "k"), zc(0.5, 0.8, 0.55, 2.0, 0.7, "s"),
    ],
    "rmr_sight": [zc(-1, 0, 1, 0.35, 0.6, "k")] + reflex(1.0, 0.9, base=False),
    "mini_reflex": [zc(-0.9, 0, 0.9, 0.3, 0.55, "m")] + reflex(0.9, 0.8, c="m", base=False),
    "holo_magnifier": [e for e in reflex(1.6, 1.9)] + [
        # Loupe x3 creuse (parois autour de l'axe + lentilles) : on vise à travers en x1
        zc(-4.3, 0.4, -1.8, 0.6, 0.8, "k"), zc(-4.3, 2.1, -1.8, 2.3, 0.8, "k"),
        box([-4.3, 0.6, -0.8], [-1.8, 2.1, -0.6], "k"), box([-4.3, 0.6, 0.6], [-1.8, 2.1, 0.8], "k"),
        zc(-4.35, 0.6, -4.3, 2.1, 0.6, "s"), zc(-1.85, 0.6, -1.8, 2.1, 0.6, "s"), zc(-3.4, 0, -2.6, 0.4, 0.5, "m"),
    ],
    "lpvo": scope(8.0, r=0.75, obj=1.2),
    "scope_x6": scope(9.0, r=0.8, obj=1.6),
    "scope_x8": scope(10.0, r=0.85, obj=2.0),
    "scope_x12": scope(11.0, r=0.9, obj=2.6),
    "pso1": scope(7.0, r=0.8, obj=1.4, c="k") + [zc(-0.6, 2.8, 0.6, 3.6, 0.6, "m"), zc(-3.9, 0.9, -3.4, 2.9, 1.1, "d")],
    "rangefinder_scope": scope(9.0, r=0.8, obj=1.6) + [zc(-1.5, 2.8, 1.5, 3.6, 0.6, "k"), zc(1.5, 3.0, 1.55, 3.4, 0.3, "r")],
    "elcan": [zc(-1.5, 0, 1.5, 0.5, 0.6, "k"), zc(-2.6, 0.5, 2.6, 2.4, 0.9, "k"), zc(-2.2, 2.4, 2.2, 2.7, 0.6, "m"),
              zc(2.6, 0.8, 2.65, 2.1, 0.7, "s"), zc(-2.65, 0.9, -2.6, 2.0, 0.6, "s")],
    "prism_x2": [zc(-1.6, 0, 1.6, 0.5, 0.7, "k"), zc(-1.6, 0.5, 1.6, 2.1, 0.8, "k"),
                 zc(1.6, 0.7, 1.65, 1.9, 0.6, "s"), zc(-1.65, 0.8, -1.6, 1.8, 0.5, "s")],
    "ghost_ring": [zc(-0.5, 0, 0.5, 0.4, 0.6, "k"), box([-0.25, 0.4, -0.6], [0.25, 1.6, -0.35], "k"),
                   box([-0.25, 0.4, 0.35], [0.25, 1.6, 0.6], "k"), zc(-0.25, 1.4, 0.25, 1.6, 0.6, "k")],
    "canted_scope": [
        zc(-1.5, 0, 1.5, 0.6, 0.6, "k"), zc(-3, 0.6, 3, 2.4, 0.9, "k"), zc(2, 0.4, 3.5, 2.6, 1.1, "k"),
        zc(3.5, 0.7, 3.55, 2.3, 0.8, "s"),
        box([-0.3, 0.2, 0.9], [0.3, 1.4, 1.2], "k", ("x", 45, [0, 0.4, 0.9])),
        box([2.5, 0.2, 0.9], [2.9, 1.2, 1.2], "k", ("x", 45, [2.7, 0.4, 0.9])),
    ],
    "peq15": [zc(-1.8, 0, 1.8, 1.2, 0.8, "T"), zc(1.8, 0.3, 1.85, 0.6, 0.3, "r"),
              zc(1.8, 0.7, 1.85, 1.0, 0.3, "s"), zc(-1.2, 1.2, 0.2, 1.4, 0.5, "u")],
    "hunting_scope": scope(10.0, r=0.6, obj=1.3, c="G", rings="g"),
    "digital_scope": [zc(-1.5, 0, 1.5, 0.6, 0.6, "k"), zc(-2.8, 0.6, 2.8, 2.8, 1.0, "k"),
                      zc(-2.85, 0.9, -2.8, 2.5, 0.8, "s"), zc(2.8, 1.2, 3.2, 2.2, 0.6, "s"),
                      zc(-1, 2.8, 1, 3.1, 0.4, "m")],
    "ruby_scope": scope(10.0, r=0.85, obj=1.8, c="r", rings="g"),

    # ---------------- Bouche du canon ----------------
    "silencer": [zc(0, -0.9, 6, 0.9, 0.9, "k"), zc(6, -0.6, 6.1, 0.6, 0.6, "d"), zc(0.5, -0.95, 1, 0.95, 0.95, "m")],
    "compensator": [zc(0, -0.7, 2, 0.7, 0.7, "l"), zc(0.5, 0.7, 1.5, 0.75, 0.3, "d"), zc(2, -0.4, 2.05, 0.4, 0.4, "d")],

    # ---------------- Poignées avant ----------------
    "vertical_grip": vgrip(4.0),
    "angled_grip": [zc(-1.5, -2, 1.5, 0, 0.7, "k", ("z", -22.5, [0, 0, 0])), zc(-1.8, -0.4, 1.8, 0, 0.75, "m")],
    "stubby_grip": vgrip(2.2),
    "magpul_afg": [zc(-2, -0.4, 2, 0, 0.75, "m"), zc(-1.8, -1.4, 0.8, -0.4, 0.65, "k", ("z", 22.5, [0, -0.4, 0]))],
    "magpul_rvg": vgrip(3.4, c="T"),
    "magpul_mvg": vgrip(3.8) + [zc(0.7, -3.0 + i * 0.8, 0.8, -2.7 + i * 0.8, 0.5, "d") for i in range(3)],
    "bcm_kag": [zc(-1.1, -0.4, 1.1, 0, 0.75, "m"), zc(-1, -2.2, 1, -0.3, 0.7, "k", ("z", -22.5, [0, -0.3, 0]))],
    "hand_stop": [zc(-1, -0.3, 1, 0, 0.7, "m"), zc(0.2, -1.1, 1, -0.3, 0.6, "k")],
    "grip_pod": vgrip(4.2) + [zc(-0.5, -4.5, 0.5, -4.2, 0.8, "m")],
    "harris_bipod": [zc(-0.8, -0.8, 0.8, 0, 0.9, "k"), box([0, -1.2, -1.0], [6, -0.9, -0.7], "k"),
                     box([0, -1.2, 0.7], [6, -0.9, 1.0], "k"), zc(-0.3, -1.3, 0.3, -0.8, 0.95, "m")],
    "flashlight_grip": vgrip(3.6) + [zc(0.6, -1.6, 3.2, -0.3, 0.6, "k"), zc(3.2, -1.5, 3.25, -0.4, 0.5, "s")],
    "laser_grip": vgrip(3.6) + [zc(0.6, -1.4, 2.4, -0.4, 0.5, "T"), zc(2.4, -1.0, 2.45, -0.7, 0.2, "r")],
    "light_laser_grip": vgrip(3.6) + [zc(0.6, -1.6, 3.2, -0.3, 0.6, "k"), zc(3.2, -1.5, 3.25, -0.4, 0.5, "s"),
                                      zc(0.6, -2.4, 2.4, -1.6, 0.45, "T"), zc(2.4, -2.1, 2.45, -1.9, 0.2, "r")],
    "wooden_foregrip": [zc(-0.9, -3.6, 0.9, 0, 0.8, "W"), zc(-1.2, -0.5, 1.2, 0, 0.85, "m")]
                       + [zc(0.9, -3.0 + i * 0.9, 1.0, -2.6 + i * 0.9, 0.6, "w") for i in range(3)],
    "folding_grip": [zc(-1.2, -0.4, 1.2, 0, 0.75, "m"), zc(-0.5, -1.0, 2.8, -0.4, 0.6, "k")],
    "carbon_grip": vgrip(3.2, c="d", hw=0.6),
    "rubber_foregrip": vgrip(3.5, c="u", hw=0.75),
    "m203": [zc(-1, -2.6, 8, -0.4, 1.0, "t"), zc(-1, -0.4, 8, 0, 0.7, "u"), zc(7.95, -2.2, 8.05, -0.8, 0.7, "d"),
             zc(-1.5, -3.8, -0.4, -2.6, 0.5, "k")],
    "m26_mass": [zc(-3, -2.2, 7, -0.6, 0.8, "k"), zc(-3, -0.6, 7, 0, 0.6, "m"), zc(6.95, -1.9, 7.05, -0.9, 0.5, "d"),
                 zc(0, -4.2, 1.5, -2.2, 0.5, "k")],
    "bayonet": [zc(3, -0.9, 5, 0, 0.45, "k"), zc(5, -0.75, 11, -0.15, 0.15, "G"), zc(11, -0.6, 11.8, -0.3, 0.12, "G"),
                zc(4.6, -1.3, 5.0, 0.2, 0.5, "m")],
    "ruby_foregrip": vgrip(3.5, c="r", base="g"),
    "tango_down_grip": vgrip(3.6) + [zc(-1.0, -3.9, 1.0, -3.6, 0.95, "k")],

    # ---------------- Chargeurs ----------------
    "magazine_24": [zc(-1, -5, 1, 0, 0.6, "l"), zc(-1.05, -5.3, 1.05, -5, 0.65, "k")],
    "magazine_36": [zc(-1, -7, 1, 0, 0.6, "l"), zc(-1.05, -7.3, 1.05, -7, 0.65, "k")],
    "drum_magazine": [
        zc(-0.8, -1.5, 0.8, 0, 0.6, "k"), zc(-2.6, -6, 2.6, -1, 1.2, "k"),
        zc(-2.6, -6, 2.6, -1, 1.2, "k", ("z", 45, [0, -3.5, 0])), zc(-1, -4.5, 1, -2.5, 1.3, "l"),
    ],

    # ---------------- Poignées pistolet ----------------
    "magpul_moe": pgrip("k"),
    "magpul_k2": pgrip("k", length=3.4, extra=[([-0.8, -0.6, -0.75], [1.0, 0, 0.75], "m")]),
    "hogue_grip": pgrip("u", extra=[([0.8, -3.2 + i, -0.6], [0.95, -2.9 + i, 0.6], "d") for i in range(3)]),
    "bcm_gunfighter": pgrip("k", extra=[([-0.95, -1.2, -0.6], [-0.8, 0, 0.6], "m")]),
    "tango_battle_grip": pgrip("k", length=3.8, extra=[([-0.9, -3.9, -0.8], [0.9, -3.6, 0.8], "m")]),
    "ergo_grip": pgrip("d", length=3.7),
    "walnut_grip": pgrip("W", extra=[([-0.82, -3.0, -0.74], [0.82, -2.8, 0.74], "w")]),
    "pearl_grip": pgrip("G", extra=[([-0.82, -2.5, -0.74], [0.82, -1.0, 0.74], "l")]),
    "gold_grip": pgrip("g", extra=[([-0.82, -2.2, -0.74], [0.82, -1.6, 0.74], "G")]),
    "ruby_pistol_grip": pgrip("r", extra=[([-0.85, -3.7, -0.76], [0.85, -3.5, 0.76], "g")]),
    "skeleton_grip": [box([-0.8, -3.5, -0.72], [0.8, 0, -0.42], "k", ("z", -22.5, [0, 0, 0])),
                      box([-0.8, -3.5, 0.42], [0.8, 0, 0.72], "k", ("z", -22.5, [0, 0, 0])),
                      zc(-0.8, -3.6, 0.8, -3.3, 0.72, "k", ("z", -22.5, [0, 0, 0]))],
    "compact_grip": pgrip("k", length=2.6),
    "competition_grip": pgrip("m", w=2.0, extra=[([-1.2, -3.9, -0.85], [1.2, -3.5, 0.85], "k")]),
    "stippled_grip": pgrip("k", extra=[([-0.82, -3.0 + i * 0.9, -0.74], [0.82, -2.8 + i * 0.9, 0.74], "m") for i in range(3)]),
    "thumb_rest_grip": pgrip("k", extra=[([-0.6, -1.4, 0.72], [0.8, -0.6, 1.1], "k")]),
    "lasergrip": pgrip("k", extra=[([0.6, -1.0, -0.5], [1.2, -0.2, 0.5], "T"), ([1.2, -0.7, -0.15], [1.25, -0.4, 0.15], "r")]),
    "storage_grip": pgrip("k", extra=[([-0.9, -3.9, -0.8], [0.9, -3.6, 0.8], "u")]),
    "finger_groove_grip": pgrip("k", extra=[([0.8, -3.3 + i * 0.9, -0.7], [1.0, -2.8 + i * 0.9, 0.7], "k") for i in range(3)]),
    "magpul_miad": pgrip("T", extra=[([-0.9, -3.8, -0.8], [0.9, -3.5, 0.8], "k")]),
    "gel_grip": pgrip("u", w=1.8),

    # ---------------- Crosses ----------------
    "a2_stock": solid_stock("k"),
    "ctr_stock": tube_stock("k"),
    "prs_stock": solid_stock("k", length=6.5, y0=-1.8, y1=1.3, hw=0.85)
                 + [zc(-5, 1.3, -1.5, 2.0, 0.7, "k"), zc(-5.6, -2.6, -5.0, -1.8, 0.4, "m")],
    "skeleton_stock": [zc(-6, 0.6, 0, 1.1, 0.55, "k"), zc(-6, -1.4, 0, -0.9, 0.55, "k"), zc(-6.3, -1.6, -5.6, 1.2, 0.7, "k")],
    "folding_stock": [box([0, -1.0, 0.95], [6, 0.8, 1.3], "k"), box([5.6, -1.4, 0.95], [6.2, 1.1, 1.3], "d")],
    "wire_stock": [zc(-6, 0.5, 0, 0.75, 0.2, "m"), zc(-6, -1.3, 0, -1.05, 0.2, "m"), zc(-6.3, -1.5, -6.0, 1.0, 0.7, "m")],
    "wooden_stock": solid_stock("W", pad="k"),
    "cheek_rest_stock": solid_stock("k") + [zc(-5, 1.2, -1.5, 1.8, 0.75, "u")],
    "thumbhole_stock": [zc(-6, 0.3, 0, 1.2, 0.8, "k"), zc(-6, -1.8, -2.5, -0.4, 0.8, "k"), zc(-6.3, -2.0, -6.0, 1.3, 0.85, "d")],
    "no_stock": [],
    "pistol_brace": [zc(-4, -1.2, 0, 0.4, 0.6, "k"), zc(-4.2, -1.6, -3.4, 0.8, 0.8, "u")],
    "buffer_tube": [zc(-4, -0.45, 0, 0.45, 0.45, "k"), zc(-4.2, -0.5, -4.0, 0.5, 0.5, "m")],
    "limbsaver_pad": solid_stock("k", pad="u", pad_len=0.9),
    "knoxx_stock": tube_stock("k") + [zc(-2.5, -0.7, -0.5, 0.2, 0.55, "m")],
    "carbon_stock": solid_stock("d", hw=0.7, pad="k"),
    "monopod_stock": solid_stock("k") + [zc(-5.3, -3.0, -4.8, -1.6, 0.35, "m"), zc(-5.5, -3.2, -4.6, -3.0, 0.5, "k")],
    "ai_stock": [zc(-6.5, -1.8, 0, 0.2, 0.8, "T"), zc(-6.5, 0.2, -3, 1.6, 0.7, "T"), zc(-3, 0.6, 0, 1.0, 0.6, "T"),
                 zc(-6.8, -2.0, -6.5, 1.7, 0.85, "d")],
    "zhukov_stock": [zc(-5.5, -1.4, 0, 1.0, 0.75, "k"), zc(-5.5, 1.0, -2, 1.5, 0.6, "k"), zc(-5.8, -1.6, -5.5, 1.5, 0.8, "d")],
    "scar_folding_stock": [zc(-5.8, -1.5, 0, 1.1, 0.75, "T"), zc(-4, 1.1, -1, 1.6, 0.6, "T"), zc(-6.1, -1.7, -5.8, 1.2, 0.8, "k")],
    "ruby_stock": solid_stock("r", pad="g"),
}


# =====================================================================
# Phase 4 : lasers, silencieux, lance-grenades, grosses lunettes
# =====================================================================
def side_laser(body="k"):
    """Laser sur rail latéral : boîtier collé au flanc gauche (vers -Z), lentille colorée à l'avant."""
    return [box([-1.8, -0.55, -1.1], [1.6, 0.55, -0.15], body),
            box([-1.1, -0.7, -0.3], [0.9, 0.7, 0.0], "m"),                 # collier de fixation
            box([1.6, -0.42, -1.0], [1.95, 0.42, -0.25], "s"),             # lentille (couleur du laser)
            box([-1.4, 0.55, -0.85], [-0.6, 0.75, -0.4], "s"),             # voyant sur le dessus
            box([-2.1, -0.2, -0.9], [-1.8, 0.2, -0.45], "d")]              # interrupteur


def can(length, r, c="k", end="d", rings=(), collar="m", holes=False):
    """Silencieux cylindrique : collier de fixation, corps, embout percé, bagues décoratives."""
    els = [zc(0, -r - 0.05, 0.7, r + 0.05, r + 0.05, collar), zc(0.7, -r, length, r, r, c),
           zc(length, -r * 0.75, length + 0.08, r * 0.75, r * 0.75, end),
           zc(length + 0.08, -0.22, length + 0.1, 0.22, 0.22, "d")]
    for x in rings:
        els.append(zc(x, -r - 0.04, x + 0.25, r + 0.04, r + 0.04, collar))
    if holes:
        for i in range(3):
            x = length - 1.2 - i * 0.8
            els.append(zc(x, r - 0.05, x + 0.35, r + 0.02, 0.2, "d"))
    return els


def gl_tube(x0, x1, y0, y1, hw, c="k"):
    """Tube de lance-grenades + bouche."""
    return [zc(x0, y0, x1, y1, hw, c), zc(x1 - 0.05, y0 + 0.25, x1 + 0.05, y1 - 0.25, hw - 0.2, "d")]


def big_scope(length, r, obj, c="k", ring=None, turrets=True):
    """Grosse lunette de sniper : tube, objectif large, tourelles de réglage, bague colorée."""
    els = scope(length, r=r, obj=obj, c=c)
    cy = 1.2 + r
    h = length / 2
    if turrets:
        els += [zc(-0.6, cy + r, 0.6, cy + r + 0.9, 0.5, "m"), zc(-0.5, cy + r + 0.9, 0.5, cy + r + 1.0, 0.45, "d"),
                box([-0.6, cy - 0.5, r], [0.6, cy + 0.5, r + 0.9], "m")]
    if ring:
        els.append(zc(h - 1.7, cy - r - 0.3 * obj - 0.02, h - 1.5, cy + r + 0.3 * obj + 0.02, r + 0.3 * obj + 0.02, ring))
    return els


LENS_COLORS = {
    "laser_red": "#ff2020", "laser_green": "#30ff40", "laser_blue": "#3060ff", "laser_purple": "#b040ff",
    "laser_yellow": "#ffe030", "laser_orange": "#ff8a20", "laser_cyan": "#30f0ff", "laser_white": "#ffffff",
    "laser_rainbow": ["#ff2020", "#ff8a20", "#ffe030", "#30ff40", "#3060ff", "#b040ff"],
}

ACCESSORIES.update({name: side_laser() for name in LENS_COLORS})
ACCESSORIES.update({
    # ---------- Silencieux pour pistolets / PM ----------
    "omega_9k": can(3.6, 0.62),
    "osprey_45": [zc(0, -0.65, 0.7, 0.65, 0.68, "m"), zc(0.7, -1.5, 5.0, 0.62, 0.72, "k"),
                  zc(5.0, -1.2, 5.08, 0.4, 0.55, "d")],
    "gemtech_gm9": can(4.2, 0.6, c="m", collar="k"),
    "bt_impuls": can(4.5, 0.66, rings=(1.6, 3.0)),
    "dead_air_wolfman": can(5.4, 0.7, c="G", collar="k"),
    "hybrid_46": can(6.4, 0.76, holes=True),
    "obsidian_45": can(5.4, 0.74, c="m", rings=(1.4, 2.4, 3.4, 4.4), collar="k"),
    "ti_rant_45": can(5.2, 0.7, c="l", collar="m"),
    # ---------- Fusils d'assaut 5,56 ----------
    "socom556": can(6.0, 0.8),
    "huxwrx_flow": can(5.6, 0.86, c="t", collar="k", holes=True),
    "bt_rotex": can(6.0, 0.8, c="G", rings=(2.0, 4.0)),
    "sandman_s": can(6.8, 0.84, rings=(1.6,)),
    # ---------- Armes russes ----------
    "pbs1": can(7.0, 0.8, c="d", collar="k", rings=(2.5, 4.8)),
    "pbs4": can(6.0, 0.74, c="d", collar="k", rings=(3.0,)),
    "tgp_a": can(6.4, 0.8, c="k", rings=(1.4, 2.2, 3.0, 3.8, 4.6, 5.4), collar="d"),
    "surge_762": can(7.4, 0.9, c="m", collar="k"),
    # ---------- Précision ----------
    "socom762": can(7.0, 0.9),
    "aac_sdn6": can(7.2, 0.86, c="d", rings=(5.6,)),
    "omega_300": can(7.0, 0.85, c="m", collar="k", holes=True),
    "thunder_beast": can(8.0, 0.95, c="l", rings=(2.0, 6.0)),
    # ---------- Spéciaux ----------
    "integral_sd": [zc(-6.0, -1.0, 3.5, 1.0, 1.0, "k"), zc(3.5, -0.7, 3.6, 0.7, 0.7, "d")]
                   + [zc(-5.5 + i * 1.6, 0.98, -4.9 + i * 1.6, 1.05, 0.5, "d") for i in range(5)],
    "bottle_silencer": [zc(0, -0.45, 0.8, 0.45, 0.45, "k"), zc(0.8, -0.5, 1.6, 0.5, 0.5, "q"),
                        zc(1.6, -1.1, 2.2, 1.1, 1.1, "q"), zc(2.2, -1.4, 6.4, 1.4, 1.4, "q"),
                        zc(3.4, -1.45, 4.4, 1.45, 1.45, "r"), zc(6.4, -1.0, 6.5, 1.0, 1.0, "q")],
    "ruby_silencer": can(6.0, 0.8, c="r", collar="g", rings=(2.0, 4.0)),
    "echo_silencer": can(6.2, 0.86, c="e", collar="d", rings=(1.6, 3.2, 4.8))
                     + [zc(1.0 + i * 1.6, 0.8, 1.4 + i * 1.6, 0.9, 0.3, "s") for i in range(3)],
    "obsidian_silencer": can(6.6, 1.0, c="o", collar="o", rings=(2.0, 4.4)),
    "oss_helix": can(6.4, 0.9, rings=(1.2, 2.0, 2.8, 3.6, 4.4, 5.2), collar="m"),

    # ---------- Lance-grenades sous le canon ----------
    "m320": gl_tube(-1, 6, -2.3, -0.4, 0.9) + [zc(-1, -0.4, 6, 0, 0.6, "m"), zc(-3.2, -2.7, -1, -0.3, 1.0, "k"),
                                               zc(-3.6, -4.5, -2.4, -2.7, 0.55, "k")],
    "gp25": gl_tube(-0.5, 5.2, -2.2, -0.4, 0.85) + [zc(-0.5, -0.4, 5.2, 0, 0.55, "m"),
                                                   zc(-2.6, -2.4, -0.5, -0.3, 0.9, "m"), zc(-2.7, -4.4, -1.5, -2.4, 0.62, "w")],
    "ag36": [zc(-2.6, -2.9, 5.5, -0.2, 1.0, "d")] + gl_tube(5.5, 7.0, -2.3, -0.7, 0.75)
            + [zc(-2.7, -4.7, -1.5, -2.9, 0.55, "d"), zc(-1, -0.2, 5.5, 0, 0.7, "k")],
    "fn_eglm": gl_tube(-1.5, 5.6, -2.5, -0.3, 0.95) + [zc(-2.8, -2.6, -1.5, -0.3, 0.95, "k"),
                                                     zc(-2.9, -4.5, -1.8, -2.6, 0.55, "T"), zc(0, -0.3, 5.6, 0, 0.7, "m")],
    "hk79": gl_tube(-0.6, 4.6, -2.0, -0.4, 0.8, "m") + [zc(-0.6, -0.4, 4.6, 0, 0.6, "k"),
                                                       zc(-1.4, -1.8, -0.6, -0.6, 0.6, "k")],

    # ---------- Grosses lunettes de sniper ----------
    "leupold_x6": big_scope(11.0, 0.9, 2.0, ring="g", turrets=False) + [zc(-0.5, 2.1 + 0.9, 0.5, 2.1 + 1.6, 0.42, "m")],
    "sb_x12": big_scope(13.0, 1.0, 2.8),
    "nightforce_x20": big_scope(15.0, 1.05, 3.4, ring="m"),
    "sb_pm2_5_25": big_scope(14.0, 1.0, 3.0, ring="l") + [zc(-4.6, 0.6, -4.0, 3.2, 1.6, "m")],
})

# ---------- Accessoires du cahier des charges ----------
ACCESSORIES.update({
    # Frein de bouche : bloc percé de larges évents sur les côtés
    "muzzle_brake": [zc(0, -0.7, 2.4, 0.7, 0.75, "k"), zc(2.4, -0.45, 2.5, 0.45, 0.5, "d")]
                    + [box([0.5 + i * 0.6, -0.4, -0.8], [0.8 + i * 0.6, 0.4, 0.8], "d") for i in range(3)],
    # Cache-flamme : tube fendu (dents à l'avant)
    "flash_hider": [zc(0, -0.5, 1.4, 0.5, 0.5, "k")]
                   + [zc(1.4, y0, 2.6, y0 + 0.25, 0.5, "k") for y0 in (-0.5, 0.25)]
                   + [box([1.4, -0.25, z0], [2.6, 0.25, z0 + 0.25], "k") for z0 in (-0.5, 0.25)],
    # Chargeur étendu (+35 %) : plus long, bande de couleur
    "extended_mag": [zc(-1, -8, 1, 0, 0.6, "m"), zc(-1.05, -8.3, 1.05, -8, 0.65, "k"), zc(-1.02, -6.4, 1.02, -6.1, 0.62, "r")],
    # Chargeur rapide : chargeur standard + languette de traction (type Magpul)
    "fast_mag": [zc(-1, -5, 1, 0, 0.6, "k"), zc(-1.05, -5.3, 1.05, -5, 0.65, "d"), zc(-0.5, -6.3, 0.5, -5.3, 0.25, "T")],
})

# Pièces internes (pas des accessoires) : cartouches du chargeur transparent du P90, maillons de la bande de la M249
ACCESSORIES.update({
    "p90_round": [zc(-0.45, -0.18, 0.45, 0.18, 0.18, "g"), zc(0.45, -0.1, 0.6, 0.1, 0.1, "m")],
    "belt_link": [box([-0.35, -0.6, -0.15], [0.35, 0.6, 0.15], "g"), box([-0.4, -0.15, -0.2], [0.4, 0.15, 0.2], "k")],
})

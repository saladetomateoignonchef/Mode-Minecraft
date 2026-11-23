"""
Flotte navale (cahier des charges « Guerre navale ») : 5 navires russes, 5 américains (dont 2 sous-marins) et 3
embarcations des Mercenaires Offshore, en voxels. Repère : avant +X, haut +Y (y = 0 à la quille), droite +Z.
Pièces : hull, turret / gun (tourelle principale), et les bouches de tir (mount 0 caisse, 2 canon) :
0 = canon principal ; 1 = CIWS ; 2 / 3 = missiles antinavires G / D ; 4 = lanceurs verticaux (croisière) ;
5 / 6 = tubes lance-torpilles G / D ; 7 = missiles antiaériens ; 8 = roquettes anti-sous-marines.
"""
import math

SHIP_COLORS = {
    "ru_navy": ["#6f7a80", "#68737a", "#76818a"], "ru_navy_d": "#4a545a",
    "us_navy": ["#8a9298", "#838b91", "#91999f"], "us_navy_d": "#5e666c",
    "hull_red": ["#7a2a22", "#72261f", "#822e26"], "deck": ["#555b60", "#5b6166", "#4f555a"],
    "deck_us": ["#4a5056", "#50565c", "#444a50"], "sub_black": ["#1c1e20", "#202224", "#181a1c"],
    "merc_black": ["#1a1b1e", "#202125", "#16171a"], "merc_orange": "#c05a14", "rubber_gray": ["#3a3c3e", "#36383a", "#3e4042"],
    "zum_gray": ["#5a6066", "#555b61", "#5f656b"], "vls": "#2a2e32", "helipad": "#3a4046", "window": "#20262c",
}


def hull(v, L, half, depth, draft, p, red="hull_red", bow=0.22, stern=0.06, deck="deck", flat_stern=True, steps=10):
    """Coque en tranches : effilée à l'étrave, bulbe sous l'eau, tableau arrière ; pont à y = depth."""
    x0, x1 = -L / 2, L / 2
    bow_len = L * bow
    n = steps
    for i in range(n):
        a = x1 - bow_len + bow_len * i / n
        b = x1 - bow_len + bow_len * (i + 1) / n
        f = (i + 0.5) / n
        w = half * max(0.12, 1 - f ** 1.6)
        v.box("hull", a, b, draft, depth, -w, w, p)
        v.box("hull", a, b, draft * 0.25 + f * draft * 0.5, draft, -w * 0.85, w * 0.85, red)
    body0 = x0 + L * stern
    v.box("hull", body0, x1 - bow_len, draft, depth, -half, half, p)
    v.box("hull", body0, x1 - bow_len, 0, draft, -half * 0.85, half * 0.85, red)
    v.box("hull", x0, body0, draft * 0.6, depth, -half * 0.92, half * 0.92, p)            # poupe
    v.box("hull", x0 + 0.5, body0, draft * 0.3, draft * 0.6, -half * 0.7, half * 0.7, red)
    v.box("hull", x0, x1 - bow_len * 0.6, depth, depth + 0.06, -half * 0.97, half * 0.97, deck)


def island(v, x0, x1, y0, h, half, p, tiers=3, windows=True):
    for t in range(tiers):
        k = 1 - t * 0.18
        a, b = x0 + (x1 - x0) * t * 0.1, x1 - (x1 - x0) * t * 0.15
        ya, yb = y0 + h * t / tiers, y0 + h * (t + 1) / tiers
        v.box("hull", a, b, ya, yb, -half * k, half * k, p)
        if windows and t == tiers - 1:
            v.box("hull", b - 0.05, b + 0.02, ya + (yb - ya) * 0.4, ya + (yb - ya) * 0.75, -half * k * 0.8, half * k * 0.8, "window")


def mast(v, x, y0, h, w=0.35, p="ru_navy_d"):
    v.box("hull", x - w, x + w, y0, y0 + h, -w, w, p)
    v.box("hull", x - w * 0.5, x + w * 0.5, y0 + h, y0 + h * 1.35, -w * 0.5, w * 0.5, p)
    v.box("hull", x - 0.08, x + 0.08, y0 + h * 0.75, y0 + h * 0.8, -w * 5, w * 5, p)       # vergue


def radar_panels(v, x, y, size, half, key="ru_navy_d"):
    for s in (-1, 1):
        v.box("hull", x - size / 2, x + size / 2, y, y + size, s * half - 0.08, s * half + 0.08, key)


def vls(v, x0, x1, y, half, rows=2, key="vls"):
    v.box("hull", x0, x1, y, y + 0.12, -half, half, key)
    n = max(2, int((x1 - x0) / 0.7))
    for i in range(n):
        xa = x0 + (x1 - x0) * i / n
        v.box("hull", xa + 0.05, xa + (x1 - x0) / n - 0.05, y + 0.12, y + 0.15, -half + 0.1, half - 0.1, "black")


def naval_gun(v, x, y, s=1.0, p="ru_navy", twin=False, decor=False, aft=False):
    tp, gp = ("hull", "hull") if decor else ("turret", "gun")
    k = -1 if aft else 1
    if not decor:
        v.turret = (x, y, 0.0)
        v.gunp = (x + 1.2 * s, y + 0.55 * s, 0.0)
    a, b = sorted((x - 1.4 * s * k, x + 1.2 * s * k))
    v.box(tp, a, b, y, y + 1.0 * s, -1.1 * s, 1.1 * s, p)
    a, b = sorted((x + 1.2 * s * k, x + 2.0 * s * k))
    v.slope(tp, a, b, y, y + 1.0 * s, -1.0 * s, 1.0 * s, p, 3, up_front=aft)
    for dz in ((-0.35 * s, 0.35 * s) if twin else (0.0,)):
        a, b = sorted((x + 1.2 * s * k, x + 5.0 * s * k))
        v.cyl(gp, "x", (y + 0.55 * s, dz), 0.16 * s, a, b, "gun")
    return (x + 5.0 * s, y + 0.55 * s, 0.0)


def ciws(v, x, y, z, p):
    v.cyl("hull", "y", (x, z), 0.55, y, y + 0.7, p)
    v.cyl("hull", "y", (x, z), 0.45, y + 0.7, y + 1.6, "white")                              # radôme
    v.cyl("hull", "x", (y + 0.6, z), 0.12, x, x + 1.4, "gun")
    return (x + 1.4, y + 0.6, z)


def inclined_tubes(v, x0, x1, y, z, n, p):
    for i in range(n):
        xa = x0 + (x1 - x0) * i / n
        for s in (-1, 1):
            v.cyl("hull", "x", (y + 0.8 + i * 0.05, s * z), 0.45, xa, xa + (x1 - x0) / n * 0.9, p)


def funnel(v, x, y, h, w, p):
    v.box("hull", x - w, x + w, y, y + h, -w * 0.8, w * 0.8, p)
    v.box("hull", x - w * 0.85, x + w * 0.85, y + h, y + h + 0.1, -w * 0.65, w * 0.65, "black")


def helideck(v, x0, x1, y, half):
    v.box("hull", x0, x1, y, y + 0.08, -half, half, "helipad")
    v.box("hull", (x0 + x1) / 2 - 0.15, (x0 + x1) / 2 + 0.15, y + 0.08, y + 0.1, -half * 0.6, half * 0.6, "white")


def muzzles(v, gun, ciws_p, ashm, vls_p, torp, sam, asw):
    v.muzzle(*gun, 2)
    v.muzzle(*ciws_p, 0)
    for z in (-ashm[2], ashm[2]):
        v.muzzle(ashm[0], ashm[1], z, 0)
    v.muzzle(*vls_p, 0)
    for z in (-torp[2], torp[2]):
        v.muzzle(torp[0], torp[1], z, 0)
    v.muzzle(*sam, 0)
    v.muzzle(*asw, 0)


def make(V0):
    ships = []
    SCALE = {}

    def V(name, sc):
        return V0(name, max(sc, SCALE.get(name, 0)))

    def reg(fn):
        def build():
            v = fn()
            need = max(v.xmax - v.xmin, 2 * v.zmax) / 2.9
            if v.S < need:
                SCALE[v.name] = math.ceil(need)
                v = fn()
            return v
        ships.append(build)
        return fn

    # ------------------------------------------------------------------ porte-avions
    def carrier(name, L, W, soviet):
        """Porte-avions : coque, pont d'envol débordant (pont droit + pont oblique), îlot à tribord, ascenseurs, catapultes
        (EMALS) ou tremplin, défense rapprochée. Pont à 12 blocs au-dessus de la flottaison (draft 6,5)."""
        v = V(name, 46)
        p = "ru_navy" if soviet else "us_navy"
        dk = "deck" if soviet else "deck_us"
        draft, D = 6.5, 18.5 if not soviet else 17.5
        hull(v, L, W / 2 - 5, D - 0.5, draft, p, deck=dk, bow=0.2)
        x0, x1 = -L / 2, L / 2
        # pont d'envol : dalle principale, étrave rétrécie par paliers, débord de l'arrière et du pont oblique
        v.box("hull", x0, x1 - 30, D - 0.5, D, -W / 2, W / 2, dk)
        for i in range(5):
            w = W / 2 * (1 - 0.14 * (i + 1))
            v.box("hull", x1 - 30 + i * 6, x1 - 24 + i * 6, D - 0.5, D, -w, w, dk)
        v.box("hull", x0 - 3, x0 + 14, D - 0.5, D, -W / 2 + 2, W / 2 - 2, dk)                 # plage arrière
        v.box("hull", x0 + 4, x0 + 56, D - 0.2, D + 0.05, -W / 2 + 1.5, -W / 2 + 7, "ru_navy_d" if soviet else "us_navy_d")   # pont oblique
        # marquages : axe de piste, lignes de catapultes, ascenseurs
        for k in range(-int(L / 2) + 12, int(L / 2) - 8, 8):
            v.box("hull", k, k + 4, D, D + 0.08, -0.25, 0.25, "white")
        if not soviet:
            for z in (-5, 5):
                v.box("hull", x1 - 62, x1 - 16, D, D + 0.08, z - 0.2, z + 0.2, "black")        # rails des catapultes EMALS
        for xe, ze in ((-14, W / 2 - 4), (4, W / 2 - 4), (30, -W / 2 + 5)):
            v.box("hull", xe - 5, xe + 5, D, D + 0.07, ze - 3.5, ze + 3.5, "ru_navy_d" if soviet else "us_navy_d")
        if soviet:
            # tremplin : rampe en marches sur les 24 derniers blocs
            for i in range(12):
                v.box("hull", x1 - 24 + i * 2, x1 - 22 + i * 2, D, D + 0.25 + i * 0.35, -W / 2 * (0.6 - 0.02 * i), W / 2 * (0.6 - 0.02 * i), dk)
            for k in range(6):
                v.box("hull", x1 - 62 + k * 7, x1 - 59 + k * 7, D, D + 0.4, W / 2 - 4, W / 2 - 2.6, "red")   # silos Granit (suggérés)
        # îlot à tribord
        zi = W / 2 - 2.8
        island(v, -12, 10, D, 12, 3.0, p, 3)
        v.box("hull", -12, 10, D, D + 12, zi - 2.6, zi + 2.6, p)
        v.box("hull", -4, 6, D + 12, D + 14, zi - 2.2, zi + 2.2, p)
        v.box("hull", 5.8, 6.1, D + 9, D + 11.5, zi - 2.0, zi + 2.0, "window")                # passerelle vitrée
        mast(v, -2, D + 14, 8, 0.4, "ru_navy_d" if soviet else "us_navy_d")
        radar_panels(v, 2, D + 10, 2.6, zi + 2.65, "ru_navy_d" if soviet else "us_navy_d")
        # défense rapprochée sur les sponsons
        c = None
        for x, z in ((x1 - 20, -W / 2 + 1.2), (x0 + 10, -W / 2 + 1.2), (x0 + 12, W / 2 - 1.2), (x1 - 26, W / 2 - 1.2)):
            c = ciws(v, x, D, z, p)
        v.muzzle(x1 - 20, D + 1.2, -W / 2 + 1.2, 0)                                           # 0 : (inutilisé)
        v.muzzle(*(x1 - 20, D + 1.2, -W / 2 + 1.2), 0)                                        # 1 : défense rapprochée
        for z in (-3, 3):
            v.muzzle(x1 - 40, D + 0.4, z, 0)                                                  # 2 / 3 : silos antinavires
        v.muzzle(x1 - 30, D + 0.3, 0, 0)                                                      # 4
        for z in (-3, 3):
            v.muzzle(0, 2.0, z * 2, 0)                                                        # 5 / 6
        v.muzzle(x0 + 12, D + 1.5, W / 2 - 1.2, 0)                                            # 7 : missiles surface-air
        v.muzzle(x0 + 8, D + 0.4, 0, 0)                                                       # 8
        v.seat(2, D + 11, zi)                                                                  # barre (passerelle)
        v.seat(x1 - 20, D + 1.2, -W / 2 + 2)                                                   # tireur de défense rapprochée
        v.seat(-1, D + 11, zi + 1.0)                                                           # officier d'armes
        for k in range(3):
            v.seat(-6 + k * 2, D + 5, zi)
        return v

    @reg
    def gerald_ford():
        return carrier("uss_gerald_r_ford", 130, 34, False)

    @reg
    def kuznetsov():
        return carrier("admiral_kuznetsov", 120, 30, True)

    # ------------------------------------------------------------------ Russie
    @reg
    def kirov():
        v = V("kirov_cruiser", 33)
        p, L, h, d, D = "ru_navy", 95, 8.0, 3.0, 7.0
        hull(v, L, h, D, d, p, steps=12)
        island(v, -12, 14, D, 9, 5.5, p, 3)
        mast(v, 4, D + 9, 9)
        radar_panels(v, 8, D + 7, 3.0, 5.0)
        funnel(v, -16, D, 6, 2.2, p)
        vls(v, -42, -38, D + 0.06, 3.0)                                                        # RBU (arrière)
        vls(v, -36, -22, D + 0.06, 4.0)                                                        # S-300F
        g = naval_gun(v, 38, D + 0.06, 1.6, p, twin=True)                                      # AK-130 (avant)
        c = ciws(v, 14, D + 9, 5.5, p)
        ciws(v, 14, D + 9, -5.5, p)
        helideck(v, -47, -44, D + 0.06, 6)
        vls(v, 20, 32, D + 0.06, 4.5)
        muzzles(v, g, c, (26, D + 1, 3.0), (26, D + 1, 0), (10, 2, 7.6), (-29, D + 1, 0), (-40, D + 1, 0))
        v.seat(10, D + 8, 0)
        v.seat(-38, D + 1, 0)
        v.seat(6, D + 8, 1.5)
        for i in range(3):
            v.seat(0 - i * 3, D + 1, 3.5)
        return v

    @reg
    def gorshkov():
        v = V("admiral_gorshkov", 19)
        p, L, h, d, D = "ru_navy", 55, 4.6, 2.2, 5.0
        hull(v, L, h, D, d, p)
        island(v, -8, 8, D, 7, 3.6, p, 3)
        v.box("hull", -1.5, 1.5, D + 7, D + 10, -1.5, 1.5, p)                                  # mât intégré (Poliment)
        radar_panels(v, 0, D + 8, 2.4, 1.55, "ru_navy_d")
        vls(v, 10, 16, D + 0.06, 2.6)
        g = naval_gun(v, 20, D + 0.06, 1.2, p)
        c = ciws(v, -9, D + 4, 2.5, p)
        helideck(v, -26, -14, D + 0.06, 4)
        muzzles(v, g, c, (13, D + 1, 1.5), (13, D + 1, 0), (-4, 1.6, 4.4), (12, D + 1, 0), (-10, D + 1, 0))
        v.seat(6, D + 6, 0)
        v.seat(16, D + 1, 0)
        v.seat(4, D + 6, 1.2)
        v.seat(-3, D + 1, 2.5)
        return v

    @reg
    def slava():
        v = V("slava_cruiser", 26)
        p, L, h, d, D = "ru_navy", 75, 6.6, 2.8, 6.0
        hull(v, L, h, D, d, p)
        island(v, -4, 12, D, 8, 4.2, p, 3)
        mast(v, 2, D + 8, 8)
        funnel(v, -10, D, 5, 2.0, p)
        inclined_tubes(v, 0, 26, D, 4.6, 4, "ru_navy_d")                                       # 16 P-1000 Vulkan
        vls(v, -26, -16, D + 0.06, 3.5)
        g = naval_gun(v, 30, D + 0.06, 1.4, p, twin=True)
        c = ciws(v, -14, D + 4, 3.5, p)
        helideck(v, -36, -28, D + 0.06, 5)
        muzzles(v, g, c, (24, D + 1.5, 4.6), (10, D + 1, 0), (-6, 2, 6.2), (-21, D + 1, 0), (20, D + 1, 1.5))
        v.seat(10, D + 7, 0)
        v.seat(26, D + 1, 0)
        v.seat(6, D + 7, 1.2)
        v.seat(-2, D + 1, 3)
        return v

    @reg
    def udaloy():
        v = V("udaloy_destroyer", 23)
        p, L, h, d, D = "ru_navy", 65, 5.6, 2.5, 5.6
        hull(v, L, h, D, d, p)
        island(v, 2, 16, D, 8, 3.8, p, 3)
        mast(v, 8, D + 8, 7)
        funnel(v, -4, D, 4.5, 1.8, p)
        funnel(v, -10, D, 4.5, 1.8, p)
        vls(v, 18, 24, D + 0.06, 3.0)                                                          # Moskit / Kalibr
        g = naval_gun(v, 26, D + 0.06, 1.3, p, twin=True)
        v.box("hull", 18, 22, D + 0.06, D + 1.2, 2.2, 3.4, "ru_navy_d")                         # RBU-6000
        c = ciws(v, -14, D + 4, 3, p)
        helideck(v, -31, -18, D + 0.06, 4.6)
        muzzles(v, g, c, (21, D + 1, 1.5), (21, D + 1, 0), (-2, 1.8, 5.2), (-15, D + 1, 0), (20, D + 1.3, 2.8))
        v.seat(14, D + 7, 0)
        v.seat(22, D + 1, 0)
        v.seat(10, D + 7, 1.2)
        v.seat(-8, D + 1, 3)
        return v

    @reg
    def buyan():
        v = V("buyan_corvette", 12)
        p, L, h, d, D = "ru_navy", 32, 3.4, 1.4, 3.6
        hull(v, L, h, D, d, p, steps=8)
        island(v, -6, 6, D, 5, 2.6, p, 2)
        mast(v, 0, D + 5, 4, 0.25)
        vls(v, -10, -7, D + 0.06, 1.6)                                                         # 8 Kalibr
        g = naval_gun(v, 9, D + 0.06, 0.9, p)
        c = ciws(v, -12, D + 0.06, 0, p)
        muzzles(v, g, c, (-8, D + 1, 1), (-8.5, D + 1, 0), (0, 1.2, 3.3), (-8, D + 1, 0), (-8, D + 1, 0))
        v.seat(3, D + 4, 0)
        v.seat(8, D + 1, 0)
        v.seat(1, D + 4, 1)
        return v

    # ------------------------------------------------------------------ États-Unis
    @reg
    def arleigh_burke():
        v = V("arleigh_burke", 22)
        p, L, h, d, D = "us_navy", 62, 5.2, 2.5, 5.4
        hull(v, L, h, D, d, p, deck="deck_us")
        island(v, -2, 14, D, 9, 3.8, p, 3)
        v.box("hull", 10, 13, D + 3, D + 7, -3.9, 3.9, p)
        radar_panels(v, 12, D + 4, 3.0, 3.95, "us_navy_d")                                      # SPY-1 Aegis
        mast(v, 6, D + 9, 7, 0.3, "us_navy_d")
        funnel(v, -2, D, 5, 1.6, p)
        funnel(v, -9, D, 5, 1.6, p)
        vls(v, 17, 23, D + 0.06, 3.0)                                                          # Mk 41 avant
        vls(v, -22, -14, D + 0.06, 3.2)                                                        # Mk 41 arrière
        g = naval_gun(v, 25, D + 0.06, 1.1, p)                                                 # Mk 45
        c = ciws(v, 15, D + 7, 0, p)
        helideck(v, -30, -24, D + 0.06, 4.6)
        muzzles(v, g, c, (-12, D + 1, 2.5), (20, D + 1, 0), (-5, 1.8, 4.8), (-18, D + 1, 0), (20, D + 1, 1.0))
        v.seat(12, D + 8, 0)
        v.seat(22, D + 1, 0)
        v.seat(8, D + 8, 1.2)
        v.seat(-6, D + 1, 3)
        return v

    @reg
    def ticonderoga():
        v = V("ticonderoga_cruiser", 25)
        p, L, h, d, D = "us_navy", 72, 5.8, 2.7, 5.8
        hull(v, L, h, D, d, p, deck="deck_us")
        island(v, -6, 16, D, 9, 4.4, p, 3)
        radar_panels(v, 12, D + 4, 3.0, 4.45, "us_navy_d")
        radar_panels(v, -4, D + 4, 3.0, 4.45, "us_navy_d")
        mast(v, 4, D + 9, 7, 0.3, "us_navy_d")
        vls(v, 20, 27, D + 0.06, 3.4)
        vls(v, -28, -20, D + 0.06, 3.6)
        naval_gun(v, -32, D + 0.06, 1.0, p, decor=True, aft=True)                             # 2e Mk 45 (arrière)
        g = naval_gun(v, 30, D + 0.06, 1.1, p)
        c = ciws(v, 16, D + 7, 0, p)
        ciws(v, -8, D + 7, 0, p)
        muzzles(v, g, c, (-16, D + 1, 3), (23, D + 1, 0), (-4, 1.9, 5.4), (-24, D + 1, 0), (24, D + 1, 1.0))
        v.seat(14, D + 8, 0)
        v.seat(26, D + 1, 0)
        v.seat(10, D + 8, 1.2)
        v.seat(-10, D + 1, 3)
        return v

    @reg
    def zumwalt():
        v = V("zumwalt_destroyer", 26)
        p, L, h, d, D = "zum_gray", 74, 6.0, 3.0, 6.0
        # coque inversée (tumblehome) : flancs rentrants, étrave perce-vagues
        for i in range(10):
            a, b = -L / 2 + L * i / 10, -L / 2 + L * (i + 1) / 10
            f = i / 9
            w = h * (1 - max(0, f - 0.75) * 3.6)
            v.box("hull", a, b, d, D * 0.6, -w, w, p)
            v.box("hull", a, b, D * 0.6, D, -w * 0.85, w * 0.85, p)
            v.box("hull", a, b, 0, d, -w * 0.8, w * 0.8, "hull_red")
        v.box("hull", -14, 12, D, D + 9, -4.0, 4.0, p)                                          # superstructure monobloc
        v.slope("hull", 12, 16, D, D + 9, -3.6, 3.6, p, 4)
        radar_panels(v, 6, D + 6, 3, 4.05, "us_navy_d")
        vls(v, 18, 26, D + 0.06, 0.6)
        for s in (-1, 1):
            v.box("hull", -30, 30, D - 0.6, D, s * 5.0 - 0.6, s * 5.0 + 0.6, "vls")               # Mk 57 PVLS périphériques
        g = naval_gun(v, 22, D + 0.06, 1.0, p)
        c = ciws(v, -14, D + 9, 0, p)
        muzzles(v, g, c, (-20, D + 1, 5), (0, D + 1, 5), (-5, 2, 5.6), (-24, D + 1, 5), (-24, D + 1, 0))
        v.seat(10, D + 8, 0)
        v.seat(20, D + 1, 0)
        v.seat(6, D + 8, 1.2)
        v.seat(-8, D + 1, 3)
        return v

    @reg
    def ohio():
        v = V("ohio_submarine", 25)
        p, L, r = "sub_black", 70, 3.6
        for i in range(12):
            a, b = -L / 2 + L * i / 12, -L / 2 + L * (i + 1) / 12
            f = i / 11
            rr = r * (0.45 + 0.55 * min(1, f * 4)) * (1 - max(0, f - 0.85) * 3.5)
            v.cyl("hull", "x", (r, 0), max(0.6, rr), a, b, p)
        v.box("hull", 8, 16, r * 2 - 0.4, r * 2 + 4.0, -0.9, 0.9, p)                             # kiosque
        for s in (-1, 1):
            v.box("hull", 10, 13, r * 2 + 2.6, r * 2 + 2.8, s * 0.9, s * 3.2, p)                  # barres de plongée
        vls(v, -12, 8, r * 2 - 0.2, 1.2)                                                       # 22 tubes Tomahawk
        v.box("hull", -36, -34, r - 0.2, r + 4.0, -0.15, 0.15, p)                                # gouvernail
        v.box("hull", -36, -34, r - 0.15, r + 0.15, -3.2, 3.2, p)
        v.muzzle(30, r, 0, 0)
        v.muzzle(12, r * 2 + 4, 0, 0)
        v.muzzle(-2, r * 2, 1, 0)
        v.muzzle(-2, r * 2, -1, 0)
        v.muzzle(-2, r * 2, 0, 0)
        v.muzzle(33, r - 1, -1.2, 0)
        v.muzzle(33, r - 1, 1.2, 0)
        v.muzzle(12, r * 2 + 4, 0, 0)
        v.muzzle(30, r, 0, 0)
        v.seat(12, r * 2 + 1, 0)
        v.seat(10, r + 0.5, 0)
        v.seat(8, r + 0.5, 0.8)
        v.seat(4, r + 0.5, -0.8)
        return v

    @reg
    def virginia():
        v = V("virginia_submarine", 18)
        p, L, r = "sub_black", 50, 2.8
        for i in range(10):
            a, b = -L / 2 + L * i / 10, -L / 2 + L * (i + 1) / 10
            f = i / 9
            rr = r * (0.4 + 0.6 * min(1, f * 4)) * (1 - max(0, f - 0.85) * 3.8)
            v.cyl("hull", "x", (r, 0), max(0.5, rr), a, b, p)
        v.box("hull", 8, 14, r * 2 - 0.3, r * 2 + 3.2, -0.7, 0.7, p)
        vls(v, 16, 20, r * 2 - 0.15, 0.8)                                                      # 12 VLS
        v.box("hull", -26, -24, r - 0.2, r + 3.2, -0.12, 0.12, p)
        v.box("hull", -26, -24, r - 0.12, r + 0.12, -2.6, 2.6, p)
        for z in (-1, 1):
            v.box("hull", 18, 20, r - 0.3, r + 0.3, z * 2.0 - 0.3, z * 2.0 + 0.3, p)
        v.muzzle(22, r, 0, 0)
        v.muzzle(11, r * 2 + 3, 0, 0)
        v.muzzle(18, r * 2, 0.5, 0)
        v.muzzle(18, r * 2, -0.5, 0)
        v.muzzle(18, r * 2, 0, 0)
        v.muzzle(24, r - 0.8, -1.0, 0)
        v.muzzle(24, r - 0.8, 1.0, 0)
        v.muzzle(11, r * 2 + 3, 0, 0)
        v.muzzle(22, r, 0, 0)
        v.seat(11, r * 2 + 0.6, 0)
        v.seat(9, r + 0.4, 0)
        v.seat(6, r + 0.4, 0.6)
        return v

    # ------------------------------------------------------------------ Mercenaires offshore
    @reg
    def rhib():
        v = V("mercenary_rhib", 4)
        p = "merc_black"
        hull(v, 8.5, 1.5, 1.2, 0.35, p, red="rubber_gray", bow=0.3, deck="merc_black", steps=6)
        for s in (-1, 1):
            v.cyl("hull", "x", (0.95, s * 1.45), 0.42, -4.0, 2.6, "rubber_gray")                  # boudins
        v.box("hull", -0.5, 0.8, 1.2, 2.2, -0.5, 0.5, "merc_orange")                            # console
        v.box("hull", 0.7, 0.8, 2.0, 2.4, -0.45, 0.45, "glass")
        v.box("hull", -4.2, -3.6, 0.5, 1.6, -0.4, 0.4, "black")                                 # hors-bord
        v.cyl("hull", "x", (1.9, 0), 0.06, 2.6, 3.9, "gun")                                    # M240B de proue
        v.muzzle(3.9, 1.9, 0, 0)
        v.seat(0.0, 1.2, 0)
        v.seat(2.6, 1.2, 0)
        v.seat(-1.4, 1.2, -0.7)
        v.seat(-1.4, 1.2, 0.7)
        v.seat(-2.6, 1.2, 0)
        return v

    @reg
    def interceptor():
        v = V("mercenary_interceptor_boat", 5)
        p = "merc_black"
        hull(v, 12, 1.8, 1.6, 0.5, p, red="merc_orange", bow=0.3, deck="merc_black", steps=6)
        v.box("hull", -1, 2.4, 1.6, 2.9, -1.3, 1.3, p)                                         # cabine blindée
        v.box("hull", 2.3, 2.5, 2.0, 2.7, -1.2, 1.2, "glass")
        v.cyl("hull", "y", (4.2, 0), 0.45, 1.6, 2.1, p)                                         # canon de 20 mm
        v.cyl("hull", "x", (2.2, 0), 0.08, 4.2, 5.8, "gun")
        v.box("hull", -5.2, -3.2, 1.6, 2.6, -0.8, 0.8, "ru_navy_d")                              # lance-roquettes 8 tubes
        for i in range(8):
            v.cyl("hull", "x", (1.8 + (i // 4) * 0.4, -0.6 + (i % 4) * 0.4), 0.14, -5.2, -3.0, "black")
        v.muzzle(5.8, 2.2, 0, 0)
        v.muzzle(-3.0, 2.0, -0.3, 0)
        v.muzzle(-3.0, 2.0, 0.3, 0)
        v.seat(0.6, 1.6, 0)
        v.seat(3.6, 1.6, 0)
        v.seat(-2.4, 1.6, 0)
        return v

    @reg
    def usv():
        v = V("mercenary_explosive_usv", 3)
        p = "merc_black"
        hull(v, 5.5, 0.9, 0.9, 0.3, p, red="merc_orange", bow=0.35, deck="merc_black", steps=5)
        v.box("hull", -1.2, 1.4, 0.9, 1.3, -0.6, 0.6, "merc_orange")                            # charge de C4 naval
        v.box("hull", 1.6, 1.8, 0.9, 1.6, -0.05, 0.05, "black")                                 # antenne
        v.box("hull", -2.9, -2.4, 0.3, 1.1, -0.3, 0.3, "black")
        v.seat(-0.4, 0.9, 0)
        return v

    return ships

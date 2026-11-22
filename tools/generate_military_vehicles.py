#!/usr/bin/env python3
"""
Véhicules militaires (mise à jour 0.4) : 14 soviétiques / russes, 11 américains et 3 complexes lance-missiles.
Modèles voxel détaillés, dessinés en mètres (1 bloc = 1 m), convertis en pixels de modèle à l'échelle de rendu `S`
(1 pixel de modèle = S / 16 bloc ; les modèles de Minecraft sont limités à 48 pixels par axe).

Pièces : caisse (hull), tourelle (turret, tourne autour de son pivot), canon / lanceur (gun, s'élève autour de son
pivot), roue (wheel, une seule, posée à chaque emplacement), rotor principal et rotor de queue, radar tournant.
Repère : avant vers +X, sol en y = 0, côté droit vers +Z, centre du véhicule en x = z = 0.

Le script écrit aussi src/main/java/fr/modeminecraft/entity/military/MilGeometry.java : échelle, pivots, roues,
places et bouches de tir de chaque véhicule (la même source sert au rendu et à la balistique).
Utilisation : python3 tools/generate_military_vehicles.py [nom...]
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import voxel_bake as vb  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
JAVA = os.path.join(ROOT, "src/main/java/fr/modeminecraft/entity/military/MilGeometry.java")
MOD = "modeminecraft"

SOV = ["#4f5d34", "#45522d", "#55633a"]
RU = ["#5b6a3a", "#3c4828", "#7d7a5a", "#2f3622"]            # camouflage russe moderne (3 tons + noir)
US = ["#b39b6e", "#a88f62", "#bba57a"]                        # sable OTAN
USG = ["#4a5038", "#424832", "#50563c"]                       # vert OTAN (hélicoptères)
COLORS = {
    "sov": SOV, "ru": RU, "us": US, "usg": USG,
    "sov_d": "#36401f", "ru_d": "#2c3520", "us_d": "#8a7550", "usg_d": "#33382a",
    "track": "#2a2a2a", "rim": "#5a5e52", "tire": "#161616", "metal": "#585c60", "dark": "#24262a", "gun": "#3a3e36",
    "glass": "#7fa8c0", "light": "#fff2c0", "ir": "#d02018", "canvas": "#5d6640", "canvas_us": "#9c8a62", "ammo": "#c8a040",
    "rubber": "#1e1e1e", "rotor": "#2a2c2e", "radar": "#c8ccc8", "white": "#e4e4e0", "red": "#b02018", "missile": "#d8dcd0",
    "missile_g": "#6a7a4a", "seat": "#3a3428", "black": "#141414", "steel": "#7a8088", "orange": "#d07020",
    "alu": ["#c4c8cc", "#bcc0c4", "#cdd1d4"], "alu_d": "#9ea3a8", "glass_frame": "#8a9096", "navy": "#1c2a5c",
}
MATERIAL = {"glass": "glass", "light": "flat", "ir": "flat", "track": "metal", "tire": "polymer", "rubber": "polymer", "canvas": "fabric",
            "canvas_us": "fabric", "seat": "leather", "red": "flat"}
PAINTS = {"sov", "ru", "us", "usg", "sov_d", "ru_d", "us_d", "usg_d", "white", "missile", "missile_g", "radar", "orange", "navy"}


def material(k):
    return MATERIAL.get(k, "paint" if k in PAINTS else "metal")


class V:
    """Un véhicule en construction : pièces (listes de pavés en pixels de modèle) + données de géométrie."""

    def __init__(self, name, scale):
        self.name = name
        self.S = scale
        self.parts = {"hull": []}
        self.turret = None          # pivot (m)
        self.gunp = None
        self.rotor = None
        self.tail = None
        self.radar = None
        self.wheels = []            # (x, y, z, r) en mètres (centre de roue)
        self.wheel_r = 0.5
        self.wheel_w = 0.35
        self.seats = []             # (x, y, z) m : position des fesses du passager
        self.props = []             # (x, y, z) m : moyeux des hélices (avions), tournant autour de l'axe X
        self.zmax = 0.0             # demi-envergure (m)
        self.muzzles = []           # (x, y, z, mount) mount : 0 caisse, 1 tourelle, 2 canon (tourelle + élévation)
        # origine du modèle (pixels) : recentrée sur la longueur réelle (canon compris), voir main()
        self.ox = OX.get(name, 8.0)
        self.xmin, self.xmax = 1e9, -1e9

    def px(self, x, y, z):
        k = 16 / self.S
        self.xmin, self.xmax = min(self.xmin, x), max(self.xmax, x)
        self.zmax = max(self.zmax, abs(z))
        return [round(self.ox + x * k, 3), round(y * k, 3), round(8 + z * k, 3)]

    def box(self, part, x0, x1, y0, y1, z0, z1, key, rot=None):
        a, b = self.px(min(x0, x1), min(y0, y1), min(z0, z1)), self.px(max(x0, x1), max(y0, y1), max(z0, z1))
        e = {"from": a, "to": b, "faces": {f: {"uv": [0, 0, 1, 1], "texture": "#p"} for f in vb.FACES}, "__key": key}
        if rot:
            axis, angle, origin = rot
            e["rotation"] = {"axis": axis, "angle": angle, "origin": self.px(*origin)}
        self.parts.setdefault(part, []).append(e)
        return e

    def sym(self, part, x0, x1, y0, y1, z0, z1, key):
        """Pavé et son symétrique (côtés gauche et droit)."""
        self.box(part, x0, x1, y0, y1, z0, z1, key)
        self.box(part, x0, x1, y0, y1, -z1, -z0, key)

    def cyl(self, part, axis, c, r, l0, l1, key, sides=True):
        """Cylindre en marches (section octogonale) le long d'un axe ('x', 'y' ou 'z'). c = centre (2 autres coords)."""
        for (a, b) in ((r, r * 0.42), (r * 0.42, r), (r * 0.72, r * 0.72)):
            if axis == "x":
                self.box(part, l0, l1, c[0] - b, c[0] + b, c[1] - a, c[1] + a, key)
            elif axis == "y":
                self.box(part, c[0] - a, c[0] + a, l0, l1, c[1] - b, c[1] + b, key)
            else:
                self.box(part, c[0] - a, c[0] + a, c[1] - b, c[1] + b, l0, l1, key)

    def slope(self, part, x0, x1, y0, y1, z0, z1, key, steps=4, up_front=False):
        """Plan incliné en marches (glacis, capot) : de x0 (bas, y0..y1) vers x1 (hauteur qui décroît)."""
        for i in range(steps):
            fa, fb = i / steps, (i + 1) / steps
            xa, xb = x0 + (x1 - x0) * fa, x0 + (x1 - x0) * fb
            top = y1 - (y1 - y0) * (fb if not up_front else 1 - fb)
            self.box(part, xa, xb, y0, max(y0 + 0.05, top), z0, z1, key)

    def wheel_at(self, x, z, r=None):
        r = r or self.wheel_r
        self.wheels.append((x, r, z, r))

    def axles(self, xs, half, r=None):
        for x in xs:
            self.wheel_at(x, -half, r)
            self.wheel_at(x, half, r)

    def seat(self, x, y, z):
        self.seats.append((x, y, z))

    def muzzle(self, x, y, z, mount):
        self.muzzles.append((x, y, z, mount))


# =========================================================================== composants
def wheel_model(v, r, w):
    """Roue centrée (en pixels) sur (8, 8, 8) : pneu à crampons, jante, moyeu."""
    k = 16 / v.S
    R, W = r * k, w * k
    e = []

    def b(x0, y0, z0, x1, y1, z1, key):
        e.append({"from": [round(x0, 3), round(y0, 3), round(z0, 3)], "to": [round(x1, 3), round(y1, 3), round(z1, 3)],
                  "faces": {f: {"uv": [0, 0, 1, 1], "texture": "#p"} for f in vb.FACES}, "__key": key})
    b(8 - R, 8 - R * 0.42, 8 - W / 2, 8 + R, 8 + R * 0.42, 8 + W / 2, "tire")
    b(8 - R * 0.42, 8 - R, 8 - W / 2, 8 + R * 0.42, 8 + R, 8 + W / 2, "tire")
    b(8 - R * 0.72, 8 - R * 0.72, 8 - W / 2, 8 + R * 0.72, 8 + R * 0.72, 8 + W / 2, "tire")
    b(8 - R * 0.5, 8 - R * 0.5, 8 - W / 2 - 0.15, 8 + R * 0.5, 8 + R * 0.5, 8 + W / 2 + 0.15, "rim")
    b(8 - R * 0.18, 8 - R * 0.18, 8 - W / 2 - 0.35, 8 + R * 0.18, 8 + R * 0.18, 8 + W / 2 + 0.35, "metal")
    return e


def tracks(v, x0, x1, half, w, h, n, r_road, paint):
    """Chenilles (des deux côtés) : bande à patins, galets de roulement, barbotin, roue folle, garde-boue."""
    for s in (-1, 1):
        zi, zo = (half - w, half) if s > 0 else (-half, -half + w)
        v.box("hull", x0 + 0.25, x1 - 0.25, 0, 0.12, zi, zo, "track")                  # brin inférieur
        v.box("hull", x0, x1, h - 0.12, h, zi, zo, "track")                            # brin supérieur
        v.box("hull", x0, x0 + 0.3, 0.15, h - 0.15, zi, zo, "track")                   # enroulements
        v.box("hull", x1 - 0.3, x1, 0.15, h - 0.15, zi, zo, "track")
        for i in range(0, int((x1 - x0) / 0.25)):                                       # patins
            x = x0 + 0.3 + i * 0.25
            if x < x1 - 0.3:
                v.box("hull", x, x + 0.1, -0.02, 0.04, zi - 0.02, zo + 0.02, "dark")
        zo2 = zo + 0.04 if s > 0 else zi - 0.04
        zin = zo - 0.08 if s > 0 else zi + 0.08
        for i in range(n):
            x = x0 + 0.55 + (x1 - x0 - 1.1) * i / max(1, n - 1)
            a, b = (zin, zo2) if s > 0 else (zo2, zin)
            v.box("hull", x - r_road, x + r_road, 0.12, 0.12 + 2 * r_road * 0.9, a, b, "rim")
            v.box("hull", x - r_road * 0.35, x + r_road * 0.35, 0.12 + r_road * 0.55, 0.12 + r_road * 1.25, a - 0.02 * s, b + 0.02 * s, "metal")
        for x in (x0 + 0.3, x1 - 0.3):                                                  # barbotin / roue folle
            a, b = (zin, zo2) if s > 0 else (zo2, zin)
            v.box("hull", x - 0.28, x + 0.28, h * 0.45, h * 0.45 + 0.56, a, b, "metal")
        v.box("hull", x0 - 0.05, x1 + 0.05, h, h + 0.08, zi - 0.03, zo + 0.03, paint)     # garde-boue


def mg(v, part, x, y, z, length, r=0.06, shield=False, paint="metal"):
    """Mitrailleuse (canon le long de +X) : boîtier, canon, cache-flamme, caisse de munitions."""
    v.box(part, x - 0.35, x + 0.15, y - 0.09, y + 0.09, z - 0.08, z + 0.08, "dark")
    v.cyl(part, "x", (y, z), r, x + 0.15, x + length, "gun")
    v.box(part, x + length - 0.08, x + length + 0.04, y - r * 1.6, y + r * 1.6, z - r * 1.6, z + r * 1.6, "dark")
    v.box(part, x - 0.25, x, y - 0.25, y - 0.08, z + 0.08, z + 0.25, "ammo" if paint == "metal" else paint)
    v.box(part, x - 0.5, x - 0.35, y - 0.14, y + 0.02, z - 0.03, z + 0.03, "dark")          # poignées
    if shield:
        v.box(part, x + 0.18, x + 0.24, y - 0.35, y + 0.4, z - 0.45, z + 0.45, paint)


def cannon(v, part, x, y, z, length, r, sleeve=True, extractor=True, muzzle_brake=False):
    """Canon de char (vers +X depuis x) : manchon thermique, évacuateur de fumées, frein de bouche."""
    v.cyl(part, "x", (y, z), r * 1.6, x - 0.2, x + 0.35, "gun")                             # masque
    v.cyl(part, "x", (y, z), r, x, x + length, "gun")
    if sleeve:
        for i in range(3):
            a = x + 0.5 + i * length * 0.28
            v.cyl(part, "x", (y, z), r * 1.25, a, a + length * 0.18, "ru_d" if part != "us" else "us_d")
    if extractor:
        a = x + length * 0.45
        v.cyl(part, "x", (y, z), r * 1.55, a, a + 0.45, "gun")
    if muzzle_brake:
        v.box(part, x + length - 0.25, x + length, y - r * 1.4, y + r * 1.4, z - r * 2, z + r * 2, "dark")


def rotor_blades(v, n, span, chord, y, key="rotor", part="rotor"):
    """Pales du rotor principal (autour du pivot v.rotor) : n pales en étoile (pavés le long de X et de Z)."""
    px, py, pz = v.rotor
    v.cyl(part, "y", (px, pz), 0.35, py - 0.25, py + 0.2, "metal")                      # moyeu
    for i in range(n):
        a = 2 * math.pi * i / n
        # pale approximée par des segments alignés (les modèles ne tournent que par pas de 22,5°)
        for k in range(10):
            d0, d1 = 0.4 + (span - 0.4) * k / 10, 0.4 + (span - 0.4) * (k + 1) / 10
            cx, cz = px + math.cos(a) * (d0 + d1) / 2, pz + math.sin(a) * (d0 + d1) / 2
            hw = chord / 2
            ext = (d1 - d0) / 2 + 0.02
            dx, dz = abs(math.cos(a)) * ext + abs(math.sin(a)) * hw, abs(math.sin(a)) * ext + abs(math.cos(a)) * hw
            v.box(part, cx - dx, cx + dx, py, py + 0.06, cz - dz, cz + dz, key)


def tail_rotor_blades(v, span, chord):
    px, py, pz = v.tail
    v.box("tail_rotor", px - 0.12, px + 0.12, py - 0.12, py + 0.12, pz - 0.15, pz + 0.15, "metal")
    for (dx, dy) in ((1, 0), (0, 1)):
        hx = span * dx + chord / 2 * dy
        hy = span * dy + chord / 2 * dx
        v.box("tail_rotor", px - hx, px + hx, py - hy, py + hy, pz - 0.03, pz + 0.03, "rotor")


def cab_truck(v, x0, x1, half, h, paint, glass=True):
    """Cabine de camion (avant en x1) : caisse, pare-brise, vitres latérales, phares, calandre."""
    v.box("hull", x0, x1, 1.0, h, -half, half, paint)
    if glass:
        v.box("hull", x1 - 0.05, x1 + 0.02, h - 0.75, h - 0.15, -half + 0.15, half - 0.15, "glass")
        v.sym("hull", x0 + 0.3, x1 - 0.5, h - 0.7, h - 0.2, half - 0.02, half + 0.02, "glass")
    v.box("hull", x1, x1 + 0.08, 1.05, 1.55, -half + 0.2, half - 0.2, "dark")                # calandre
    v.sym("hull", x1, x1 + 0.1, 1.3, 1.45, half - 0.45, half - 0.15, "light")


def chassis(v, x0, x1, half, y0=0.55, y1=0.85):
    v.box("hull", x0, x1, y0, y1, -half, half, "dark")


# =========================================================================== véhicules soviétiques / russes
def uaz():
    v = V("uaz_469", 2.0)
    p = "sov"
    v.wheel_r, v.wheel_w = 0.42, 0.28
    chassis(v, -1.9, 1.9, 0.75)
    v.box("hull", -1.95, 1.95, 0.75, 1.25, -0.85, 0.85, p)                                   # caisse
    v.box("hull", 0.8, 1.95, 1.25, 1.42, -0.8, 0.8, p)                                       # capot
    v.slope("hull", 0.5, 0.8, 1.25, 1.95, -0.82, 0.82, "glass", 2)                          # pare-brise
    v.box("hull", 0.45, 0.55, 1.25, 1.95, -0.85, 0.85, "dark")                              # cadre de pare-brise
    v.box("hull", -1.95, 0.5, 1.25, 1.98, -0.86, 0.86, "canvas")                            # bâche
    v.sym("hull", -1.2, 0.3, 1.4, 1.8, 0.86, 0.88, "glass")
    v.box("hull", 1.95, 2.02, 0.8, 1.3, -0.6, 0.6, "dark")                                   # calandre
    v.sym("hull", 1.95, 2.05, 1.05, 1.25, 0.55, 0.75, "light")
    v.sym("hull", 0.9, 1.8, 0.95, 1.15, 0.85, 0.95, p)                                       # garde-boue
    v.sym("hull", -1.75, -0.9, 0.95, 1.15, 0.85, 0.95, p)
    v.box("hull", -2.12, -1.95, 0.85, 1.55, -0.3, 0.3, "tire")                              # roue de secours
    v.box("hull", 1.95, 2.15, 0.6, 0.72, -0.85, 0.85, "dark")                                # pare-chocs
    v.axles([1.25, -1.25], 0.8)
    v.seat(0.0, 0.85, -0.4)
    v.seat(0.0, 0.85, 0.4)
    v.seat(-1.1, 0.85, -0.4)
    v.seat(-1.1, 0.85, 0.4)
    return v


def tigr():
    v = V("tigr", 2.5)
    p = "sov"
    v.wheel_r, v.wheel_w = 0.55, 0.38
    chassis(v, -2.6, 2.6, 0.95, 0.6, 1.0)
    v.box("hull", -2.7, 1.2, 1.0, 2.25, -1.1, 1.1, p)                                         # caisse blindée
    v.slope("hull", 1.2, 2.75, 1.0, 1.75, -1.08, 1.08, p, 4)                                 # capot en pente
    v.slope("hull", 0.7, 1.25, 1.75, 2.25, -1.05, 1.05, "glass", 3)                         # pare-brise blindé
    v.sym("hull", -1.5, 0.6, 1.65, 2.05, 1.1, 1.12, "glass")                                # vitres pare-balles
    v.sym("hull", -2.5, -1.7, 1.65, 2.0, 1.1, 1.12, "glass")
    v.box("hull", 2.7, 2.85, 0.75, 1.45, -0.95, 0.95, "dark")                                # pare-buffle
    v.sym("hull", 2.72, 2.82, 1.3, 1.45, 0.65, 0.9, "light")
    v.sym("hull", 1.3, 2.4, 1.0, 1.25, 1.08, 1.2, p)                                         # passages de roue
    v.sym("hull", -2.4, -1.3, 1.0, 1.25, 1.08, 1.2, p)
    v.box("hull", -2.85, -2.7, 1.1, 2.0, -0.6, 0.6, p)                                        # porte arrière
    v.box("hull", -2.95, -2.85, 1.2, 1.9, -0.4, 0.4, "tire")                                 # roue de secours
    v.box("hull", -0.9, 0.3, 2.25, 2.35, -0.6, 0.6, "sov_d")                                 # anneau de tourelle
    v.axles([1.75, -1.75], 1.0)
    v.turret = (-0.3, 2.35, 0.0)
    v.box("turret", -0.75, 0.15, 2.35, 2.5, -0.45, 0.45, "sov_d")
    v.box("turret", 0.15, 0.22, 2.4, 2.95, -0.45, 0.45, p)                                    # bouclier
    v.gunp = (0.0, 2.7, 0.0)
    mg(v, "gun", 0.0, 2.7, 0.0, 1.25, 0.06)
    v.muzzle(1.3, 2.7, 0.0, 2)
    v.seat(0.4, 1.05, -0.5)                                                                    # conducteur
    v.seat(0.4, 1.05, 0.5)                                                                     # passager avant
    v.seat(-0.3, 1.75, 0.0)                                                                    # tireur (debout dans l'écoutille)
    v.seat(-1.6, 1.05, 0.0)                                                                    # passager arrière
    return v


def kamaz():
    v = V("kamaz", 3.0)
    p = "sov"
    v.wheel_r, v.wheel_w = 0.6, 0.4
    chassis(v, -3.8, 3.6, 0.9, 0.65, 1.05)
    cab_truck(v, 1.6, 3.6, 1.2, 2.9, p)
    v.box("hull", 1.55, 1.6, 1.0, 2.9, -1.2, 1.2, "sov_d")
    v.sym("hull", 2.3, 3.5, 1.0, 1.3, 1.2, 1.3, p)                                           # garde-boue avant
    v.box("hull", -3.9, 1.4, 1.05, 1.5, -1.25, 1.25, "sov_d")                                # plateau
    v.sym("hull", -3.9, 1.4, 1.5, 2.1, 1.2, 1.25, p)                                         # ridelles
    v.box("hull", -3.95, -3.9, 1.5, 2.1, -1.25, 1.25, p)
    for x in (-3.6, -2.4, -1.2, 0.0, 1.15):                                                    # arceaux + bâche
        v.box("hull", x, x + 0.08, 2.1, 3.0, -1.26, 1.26, "dark")
    v.box("hull", -3.9, 1.4, 2.9, 3.15, -1.26, 1.26, "canvas")
    v.sym("hull", -3.9, 1.4, 2.1, 2.9, 1.24, 1.27, "canvas")
    v.box("hull", 1.3, 1.4, 2.1, 3.0, -1.26, 1.26, "canvas")
    v.sym("hull", -2.6, 0.8, 1.55, 1.75, 0.75, 1.15, "seat")                                  # bancs
    v.box("hull", 0.2, 1.4, 0.6, 1.0, 1.0, 1.3, "metal")                                      # réservoir
    v.axles([2.6, -1.4, -2.75], 1.05)
    v.seat(2.3, 1.15, -0.5)
    v.seat(2.3, 1.15, 0.5)
    for i in range(4):
        v.seat(-2.5 + i * 0.9, 1.55, -0.8)
        v.seat(-2.5 + i * 0.9, 1.55, 0.8)
    return v


def brdm():
    v = V("brdm_2", 2.5)
    p = "sov"
    v.wheel_r, v.wheel_w = 0.55, 0.38
    v.box("hull", -2.6, 2.0, 0.6, 1.75, -1.15, 1.15, p)                                       # coque
    v.slope("hull", 2.0, 2.9, 0.6, 1.75, -1.1, 1.1, p, 4)                                     # proue de bateau
    v.box("hull", -2.85, -2.6, 0.7, 1.6, -1.0, 1.0, p)
    v.box("hull", -0.6, 1.6, 1.75, 2.0, -1.0, 1.0, p)                                         # toit
    v.slope("hull", 1.6, 2.1, 1.75, 2.0, -0.95, 0.95, p, 2)
    v.sym("hull", 1.5, 1.9, 1.55, 1.85, 0.5, 0.85, "glass")                                   # trappes de vision
    v.box("hull", -2.5, -0.6, 1.75, 1.85, -1.0, 1.0, "sov_d")                                 # grilles moteur
    v.sym("hull", -0.5, 0.5, 0.5, 0.75, 1.1, 1.2, "tire")                                     # roues centrales rétractables
    v.box("hull", -2.92, -2.85, 0.8, 1.1, -0.35, 0.35, "metal")                               # hydrojet
    v.sym("hull", 2.6, 2.7, 1.3, 1.45, 0.5, 0.7, "light")
    v.axles([1.45, -1.55], 1.1)
    v.turret = (0.35, 2.0, 0.0)
    v.cyl("turret", "y", (0.35, 0.0), 0.62, 2.0, 2.3, p)                                      # tourelle conique
    v.cyl("turret", "y", (0.35, 0.0), 0.48, 2.3, 2.6, p)
    v.cyl("turret", "y", (0.35, 0.0), 0.3, 2.6, 2.7, p)
    v.gunp = (0.8, 2.3, 0.0)
    v.cyl("gun", "x", (2.3, 0.0), 0.14, 0.7, 1.1, "gun")
    v.cyl("gun", "x", (2.3, 0.0), 0.07, 1.1, 2.6, "gun")                                       # KPVT 14,5 mm
    v.cyl("gun", "x", (2.3, 0.18), 0.04, 0.8, 1.5, "gun")                                      # PKT coaxiale
    v.muzzle(2.65, 2.3, 0.0, 2)
    v.muzzle(1.5, 2.3, 0.18, 2)
    v.seat(1.2, 0.9, -0.45)
    v.seat(0.35, 1.25, 0.0)
    v.seat(1.2, 0.9, 0.45)
    return v


def btr80():
    v = V("btr_80", 3.0)
    p = "sov"
    v.wheel_r, v.wheel_w = 0.6, 0.4
    v.box("hull", -3.6, 2.6, 0.7, 1.95, -1.25, 1.25, p)
    v.slope("hull", 2.6, 3.8, 0.7, 1.95, -1.2, 1.2, p, 5)                                      # nez en pointe
    v.sym("hull", -3.6, 2.6, 1.95, 2.3, 1.0, 1.25, p)                                         # flancs inclinés
    v.box("hull", -3.6, 2.6, 1.95, 2.3, -1.0, 1.0, p)
    v.slope("hull", 2.6, 3.2, 1.95, 2.3, -1.15, 1.15, p, 2)
    v.sym("hull", 2.3, 2.7, 2.0, 2.25, 0.4, 0.85, "glass")                                    # vision pilote
    for x in (-1.8, -0.6, 0.6):                                                                 # trappes de tir
        v.sym("hull", x, x + 0.2, 1.6, 1.8, 1.25, 1.28, "dark")
    v.sym("hull", -0.3, 0.8, 0.85, 1.75, 1.25, 1.3, "sov_d")                                  # porte latérale
    v.box("hull", -3.6, -1.5, 2.3, 2.38, -1.0, 1.0, "sov_d")                                  # grilles moteur
    v.box("hull", -3.85, -3.6, 0.9, 1.4, -0.4, 0.4, "metal")                                  # hydrojet
    v.sym("hull", 3.55, 3.7, 1.45, 1.6, 0.6, 0.85, "light")
    for x in (-0.2, 0.2):                                                                       # lance-fumigènes
        v.sym("hull", 0.6 + x, 0.75 + x, 2.3, 2.6, 0.5, 0.65, "dark")
    v.axles([2.2, 0.9, -1.0, -2.3], 1.2)
    v.turret = (0.9, 2.3, 0.0)
    v.cyl("turret", "y", (0.9, 0.0), 0.7, 2.3, 2.6, p)
    v.cyl("turret", "y", (0.9, 0.0), 0.5, 2.6, 2.85, p)
    v.box("turret", 0.3, 0.6, 2.6, 2.9, -0.2, 0.2, "glass")                                   # viseur
    v.gunp = (1.4, 2.55, 0.0)
    v.cyl("gun", "x", (2.55, 0.0), 0.15, 1.3, 1.8, "gun")
    v.cyl("gun", "x", (2.55, 0.0), 0.08, 1.8, 3.6, "gun")
    v.cyl("gun", "x", (2.55, 0.25), 0.04, 1.4, 2.2, "gun")
    v.muzzle(3.65, 2.55, 0.0, 2)
    v.muzzle(2.2, 2.55, 0.25, 2)
    v.seat(2.3, 1.05, -0.5)                                                                     # conducteur
    v.seat(0.9, 1.3, 0.0)                                                                       # tireur
    v.seat(2.3, 1.05, 0.5)                                                                      # commandant
    for i in range(3):
        v.seat(-0.4 - i * 1.0, 1.05, -0.6)
        v.seat(-0.4 - i * 1.0, 1.05, 0.6)
    return v


def bmp2():
    v = V("bmp_2", 3.0)
    p = "sov"
    tracks(v, -3.3, 3.1, 1.55, 0.42, 0.9, 6, 0.36, p)
    v.box("hull", -3.4, 2.0, 0.45, 1.6, -1.15, 1.15, p)
    v.slope("hull", 2.0, 3.45, 0.45, 1.6, -1.15, 1.15, p, 6)                                  # nez à côtes
    for i in range(3):
        v.box("hull", 2.3 + i * 0.35, 2.4 + i * 0.35, 1.05 - i * 0.22, 1.35 - i * 0.22, -1.1, 1.1, "sov_d")
    v.box("hull", -3.4, 2.0, 1.6, 1.95, -1.5, 1.5, p)                                          # toit / garde-boue
    v.box("hull", -3.4, -1.8, 1.95, 2.05, -1.0, 1.0, "sov_d")                                 # trappes arrière
    v.box("hull", -3.5, -3.4, 0.6, 1.6, -0.9, 0.9, p)                                          # portes arrière (réservoirs)
    v.sym("hull", 1.2, 1.6, 1.95, 2.1, 0.6, 0.9, "glass")
    for x in (-2.2, -1.3, -0.4):
        v.sym("hull", x, x + 0.15, 1.65, 1.82, 1.5, 1.52, "dark")                             # meurtrières
    v.turret = (0.2, 1.95, 0.0)
    v.cyl("turret", "y", (0.2, 0.0), 0.95, 1.95, 2.3, p)
    v.cyl("turret", "y", (0.2, 0.0), 0.75, 2.3, 2.55, p)
    v.box("turret", -0.2, 0.25, 2.55, 2.7, -0.55, -0.15, "sov_d")                             # tourelleau
    v.box("turret", -0.35, 0.35, 2.55, 2.85, 0.35, 0.6, "missile_g")                          # tube Konkurs
    v.box("turret", -0.45, 0.45, 2.6, 2.78, 0.4, 0.55, "missile_g")
    v.sym("turret", -0.6, -0.4, 2.3, 2.55, 0.7, 0.85, "dark")                                  # fumigènes
    v.gunp = (0.95, 2.25, 0.0)
    v.cyl("gun", "x", (2.25, 0.0), 0.13, 0.85, 1.3, "gun")
    v.cyl("gun", "x", (2.25, 0.0), 0.065, 1.3, 3.8, "gun")                                     # 2A42 30 mm
    v.box("gun", 3.55, 3.85, 2.18, 2.32, -0.08, 0.08, "dark")
    v.muzzle(3.85, 2.25, 0.0, 2)
    v.muzzle(0.4, 2.7, 0.48, 1)                                                                 # Konkurs
    v.seat(2.1, 0.9, -0.55)
    v.seat(0.2, 1.3, 0.25)
    v.seat(0.2, 1.3, -0.3)
    for i in range(5):
        v.seat(-0.9 - i * 0.5, 0.9, 0.5 if i % 2 else -0.5)
    return v


def grad():
    v = V("bm_21_grad", 3.0)
    p = "sov"
    v.wheel_r, v.wheel_w = 0.6, 0.4
    chassis(v, -3.7, 3.5, 0.9, 0.65, 1.05)
    v.box("hull", 1.8, 3.55, 1.0, 2.0, -1.2, 1.2, p)                                          # capot long (Ural)
    v.slope("hull", 2.9, 3.6, 1.6, 2.0, -1.15, 1.15, p, 2)
    v.box("hull", 0.7, 1.9, 1.0, 2.8, -1.2, 1.2, p)                                            # cabine
    v.box("hull", 1.88, 1.92, 2.0, 2.65, -1.05, 1.05, "glass")
    v.sym("hull", 0.9, 1.7, 2.1, 2.6, 1.2, 1.22, "glass")
    v.box("hull", 3.55, 3.65, 1.05, 1.7, -0.7, 0.7, "dark")
    v.sym("hull", 3.5, 3.62, 1.6, 1.8, 0.75, 1.0, "light")
    v.sym("hull", 2.2, 3.4, 1.0, 1.3, 1.2, 1.3, p)
    v.box("hull", -3.8, 0.6, 1.05, 1.35, -1.2, 1.2, "sov_d")                                  # plateau
    v.box("hull", -2.5, -0.8, 1.35, 1.75, -0.6, 0.6, p)                                        # socle du lanceur
    v.sym("hull", -3.8, -3.3, 0.3, 1.05, 0.9, 1.1, "metal")                                    # vérins
    v.axles([2.7, -1.5, -2.8], 1.05)
    v.turret = (-1.6, 1.75, 0.0)
    v.cyl("turret", "y", (-1.6, 0.0), 0.65, 1.75, 1.95, p)
    v.sym("turret", -2.2, -1.0, 1.95, 2.35, 0.6, 0.75, p)                                      # flasques
    v.gunp = (-1.6, 2.25, 0.0)
    # panier de 40 tubes (4 rangées de 10), vers l'avant
    for row in range(4):
        for col in range(10):
            y = 2.05 + row * 0.16
            z = -0.82 + col * 0.18
            v.box("gun", -3.6, 0.6, y, y + 0.14, z, z + 0.15, "sov_d" if (row + col) % 2 else "sov")
    v.box("gun", -3.62, 0.62, 2.0, 2.05, -0.86, 0.96, "dark")
    v.box("gun", -0.4, -0.3, 2.0, 2.72, -0.86, 0.96, "dark")                                   # cerclages
    v.box("gun", -2.6, -2.5, 2.0, 2.72, -0.86, 0.96, "dark")
    v.muzzle(0.65, 2.36, 0.07, 2)
    v.seat(1.3, 1.15, -0.5)
    v.seat(1.3, 1.15, 0.5)
    return v


def shilka():
    v = V("zsu_23_4", 3.0)
    p = "sov"
    tracks(v, -3.0, 3.1, 1.5, 0.4, 0.9, 6, 0.35, p)
    v.box("hull", -3.2, 2.6, 0.45, 1.65, -1.1, 1.1, p)
    v.slope("hull", 2.6, 3.25, 0.45, 1.65, -1.1, 1.1, p, 3)
    v.box("hull", -3.2, 2.6, 1.65, 1.85, -1.5, 1.5, p)
    v.sym("hull", 2.0, 2.5, 1.85, 2.0, 0.5, 0.9, "glass")
    v.turret = (-0.2, 1.85, 0.0)
    v.box("turret", -1.6, 1.4, 1.85, 2.75, -1.25, 1.25, p)                                     # grosse tourelle plate
    v.slope("turret", 1.4, 1.75, 1.85, 2.75, -1.2, 1.2, p, 3)
    v.box("turret", -1.5, -0.3, 2.75, 2.85, -1.0, 1.0, "sov_d")
    # radar RPK-2 Tobol sur mât arrière
    v.box("turret", -1.4, -1.2, 2.75, 3.3, -0.1, 0.1, "metal")
    v.radar = (-1.3, 3.35, 0.0)
    v.box("radar", -1.42, -1.18, 3.3, 3.45, -0.12, 0.12, "metal")
    v.box("radar", -1.25, -1.15, 3.35, 4.1, -0.7, 0.7, "radar")                                # parabole
    v.box("radar", -1.15, -1.1, 3.45, 4.0, -0.55, 0.55, "radar")
    v.box("radar", -1.0, -0.92, 3.6, 3.85, -0.05, 0.05, "dark")
    v.gunp = (1.3, 2.35, 0.0)
    for zz in (-0.95, 0.95):                                                                     # 4 canons 23 mm
        v.box("gun", 1.1, 1.6, 2.1, 2.6, zz - 0.18, zz + 0.18, "sov_d")
        for yy in (2.22, 2.48):
            v.cyl("gun", "x", (yy, zz), 0.045, 1.6, 3.4, "gun")
            v.cyl("gun", "x", (yy, zz), 0.07, 1.6, 2.1, "gun")                                  # manchons refroidis
    v.muzzle(3.45, 2.35, 0.0, 2)
    v.seat(2.1, 0.85, -0.5)
    v.seat(-0.2, 1.4, 0.4)
    v.seat(-0.2, 1.4, -0.4)
    return v


def mi24():
    v = V("mi_24", 5.0)
    p = "sov"
    L = 7.0
    # fuselage (x de -L à L avec queue)
    v.box("hull", -2.5, 3.5, 0.9, 2.9, -1.0, 1.0, p)                                            # soute
    v.slope("hull", 3.5, 5.0, 0.9, 2.6, -0.85, 0.85, p, 4)
    v.box("hull", 4.2, 6.6, 1.0, 1.9, -0.55, 0.55, p)                                           # nez
    v.box("hull", 5.2, 6.4, 1.9, 2.55, -0.5, 0.5, "glass")                                      # verrière avant (artilleur)
    v.box("hull", 3.6, 4.9, 2.3, 3.15, -0.55, 0.55, "glass")                                    # verrière arrière (pilote)
    v.box("hull", -2.2, 3.0, 2.9, 3.4, -0.75, 0.75, p)                                          # capots moteurs
    v.sym("hull", 1.6, 2.6, 2.95, 3.3, 0.75, 0.85, "dark")                                      # prises d'air
    v.sym("hull", -1.6, 1.8, 1.6, 2.2, 1.0, 1.02, "glass")                                      # hublots soute
    v.box("hull", -L, -2.5, 2.1, 2.7, -0.35, 0.35, p)                                           # poutre de queue
    v.box("hull", -L - 0.3, -6.2, 2.4, 4.2, -0.12, 0.12, p)                                    # dérive
    v.box("hull", -6.3, -5.6, 2.2, 2.35, -1.2, 1.2, p)                                          # stabilisateur
    # ailerons d'emport + pylônes (roquettes S-8, missiles Shturm)
    v.box("hull", -0.6, 0.9, 2.0, 2.15, -3.3, 3.3, p)
    for zz in (-2.9, -1.8, 1.8, 2.9):
        v.box("hull", -0.3, 0.6, 1.75, 2.0, zz - 0.08, zz + 0.08, "dark")
    for zz in (-1.8, 1.8):
        v.cyl("hull", "x", (1.5, zz), 0.3, -1.0, 1.0, "sov_d")                                  # pod B-8 (S-8)
        v.box("hull", 1.0, 1.05, 1.25, 1.75, zz - 0.25, zz + 0.25, "dark")
    for zz in (-3.25, 3.25):
        for dy in (0.0, 0.3):
            v.cyl("hull", "x", (1.75 + dy, zz), 0.1, -0.8, 1.2, "missile_g")                   # Shturm
    v.box("hull", 2.5, 2.9, 0.3, 0.9, -0.08, 0.08, "dark")                                      # train
    v.sym("hull", -1.5, -1.1, 0.3, 0.95, 0.9, 1.05, "dark")
    v.box("hull", 2.5, 2.9, 0.0, 0.3, -0.12, 0.12, "tire")
    v.sym("hull", -1.5, -1.1, 0.0, 0.3, 0.9, 1.1, "tire")
    v.turret = (6.0, 1.0, 0.0)                                                                  # tourelle de menton YakB
    v.cyl("turret", "y", (6.0, 0.0), 0.32, 0.65, 1.0, "sov_d")
    v.gunp = (6.0, 0.75, 0.0)
    v.cyl("gun", "x", (0.75, 0.0), 0.06, 6.0, 7.0, "gun")
    v.cyl("gun", "x", (0.75, 0.0), 0.12, 6.0, 6.4, "dark")
    v.rotor = (0.6, 3.75, 0.0)
    v.box("hull", 0.4, 0.8, 3.4, 3.75, -0.2, 0.2, "metal")
    rotor_blades(v, 5, 6.8, 0.45, 3.75)
    v.tail = (-6.3, 3.7, 0.3)
    tail_rotor_blades(v, 1.15, 0.25)
    v.muzzle(7.05, 0.75, 0.0, 2)                                                                # YakB
    v.muzzle(1.05, 1.5, -1.8, 0)                                                                 # S-8 gauche
    v.muzzle(1.05, 1.5, 1.8, 0)
    v.muzzle(1.25, 1.9, -3.25, 0)                                                                # Shturm
    v.muzzle(1.25, 1.9, 3.25, 0)
    v.seat(4.1, 2.0, 0.0)                                                                        # pilote
    v.seat(5.6, 1.4, 0.0)                                                                        # artilleur
    for i in range(3):
        v.seat(1.8 - i * 1.3, 1.0, -0.5)
        v.seat(1.8 - i * 1.3, 1.0, 0.5)
    return v


def tank_hull(v, L0, L1, W, H, p, nwheels, r_road, era=None, nose_steps=4, rear_grill="dark"):
    """Caisse de char : chenilles, caisse, glacis en pente, garde-boue, plage arrière."""
    tracks(v, L0, L1, W / 2, 0.55, 0.95, nwheels, r_road, p)
    v.box("hull", L0 + 0.2, L1 - 0.9, 0.4, H, -W / 2 + 0.5, W / 2 - 0.5, p)
    v.slope("hull", L1 - 1.0, L1 + 0.15, 0.4, H, -W / 2 + 0.5, W / 2 - 0.5, p, nose_steps)
    v.box("hull", L0, L1 - 0.6, H - 0.12, H, -W / 2, W / 2, p)                                  # garde-boue / plage
    v.box("hull", L0 - 0.1, L0 + 1.4, H, H + 0.08, -W / 2 + 0.7, W / 2 - 0.7, rear_grill)      # grilles moteur
    v.box("hull", L0 - 0.15, L0, 0.55, H - 0.1, -W / 2 + 0.5, W / 2 - 0.5, p)
    for z in (-W / 2 + 0.85, W / 2 - 0.85):                                                      # phares
        v.box("hull", L1 - 0.45, L1 - 0.35, H, H + 0.12, z - 0.1, z + 0.1, "light")
    if era:
        # briques de blindage réactif sur le glacis et les jupes
        for i in range(5):
            z = -W / 2 + 0.6 + i * (W - 1.2) / 5
            v.box("hull", L1 - 0.7, L1 - 0.2, H - 0.1, H + 0.12, z + 0.03, z + (W - 1.2) / 5 - 0.03, era)
        for s in (-1, 1):
            for i in range(6):
                x = L1 - 1.2 - i * 0.6
                zo = W / 2 + 0.02
                v.box("hull", x - 0.55, x, 0.45, 0.85, (zo - 0.08) * s if s > 0 else -zo, zo if s > 0 else -(zo - 0.08), era)


def t90m():
    v = V("t_90m", 3.25)
    p = "ru"
    tank_hull(v, -3.5, 3.4, 3.6, 1.55, p, 6, 0.38, era="ru_d")
    v.turret = (0.0, 1.55, 0.0)
    v.box("turret", -1.6, 1.1, 1.55, 2.2, -1.25, 1.25, p)                                       # tourelle soudée
    v.slope("turret", 1.1, 1.9, 1.55, 2.2, -1.2, 1.2, p, 3)
    v.box("turret", -2.2, -1.6, 1.6, 2.15, -1.05, 1.05, p)                                      # nid arrière
    for i in range(4):                                                                            # Relikt (avant de tourelle)
        for s in (-1, 1):
            z = 0.35 + i * 0.22
            v.box("turret", 1.0 + 0.15 * (3 - i), 1.95 - 0.12 * i, 2.18, 2.3, s * z - 0.1, s * z + 0.1, "ru_d")
    v.sym("turret", 1.2, 1.45, 1.75, 2.0, 0.85, 1.1, "ir")                                      # projecteurs Shtora-1
    v.sym("turret", 1.15, 1.2, 1.7, 2.05, 0.8, 1.15, "dark")
    v.box("turret", -0.6, 0.1, 2.2, 2.45, 0.3, 0.9, "ru_d")                                      # tourelleau commandant
    mg(v, "turret", -0.3, 2.65, 0.6, 1.0, 0.055)                                                  # 12,7 mm téléopérée
    v.box("turret", -0.2, 0.3, 2.2, 2.55, -0.85, -0.45, "glass")                                 # viseur Sosna-U
    v.sym("turret", -1.4, -1.0, 2.2, 2.35, 1.0, 1.2, "dark")                                     # lance-fumigènes
    v.gunp = (1.6, 1.95, 0.0)
    cannon(v, "gun", 1.6, 1.95, 0.0, 4.3, 0.1)
    v.muzzle(5.95, 1.95, 0.0, 2)
    v.muzzle(0.75, 2.65, 0.6, 1)
    v.seat(2.2, 0.75, 0.0)
    v.seat(0.0, 1.05, 0.5)
    v.seat(0.0, 1.05, -0.5)
    return v


def t80():
    v = V("t_80bvm", 3.25)
    p = "ru"
    tank_hull(v, -3.6, 3.3, 3.55, 1.5, p, 6, 0.38, era="ru_d")
    for i in range(4):                                                                             # grille de turbine
        v.box("hull", -3.65, -3.55, 0.8 + i * 0.15, 0.88 + i * 0.15, -0.9, 0.9, "dark")
    v.turret = (0.0, 1.5, 0.0)
    v.cyl("turret", "y", (0.1, 0.0), 1.35, 1.5, 1.9, p)                                          # tourelle coulée arrondie
    v.cyl("turret", "y", (0.0, 0.0), 1.1, 1.9, 2.2, p)
    for i in range(5):                                                                             # Kontakt-5 en « fer à cheval »
        for s in (-1, 1):
            v.box("turret", 0.8 - 0.05 * i, 1.6 - 0.05 * i, 1.6 + i * 0.12, 1.72 + i * 0.12, s * 0.25, s * 1.3, "ru_d")
    v.box("turret", -1.6, -1.2, 1.6, 2.1, -1.1, 1.1, "sov_d")                                    # coffres arrière
    v.box("turret", -0.5, 0.1, 2.2, 2.45, 0.3, 0.85, "ru_d")
    mg(v, "turret", -0.3, 2.62, 0.55, 1.0, 0.055)
    v.gunp = (1.5, 1.9, 0.0)
    cannon(v, "gun", 1.5, 1.9, 0.0, 4.4, 0.1)
    v.muzzle(5.95, 1.9, 0.0, 2)
    v.muzzle(0.75, 2.62, 0.55, 1)
    v.seat(2.2, 0.75, 0.0)
    v.seat(0.0, 1.05, 0.5)
    v.seat(0.0, 1.05, -0.5)
    return v


def t14():
    v = V("t_14_armata", 3.75)
    p = "ru"
    tank_hull(v, -4.4, 4.0, 3.6, 1.65, p, 7, 0.36, era=None, nose_steps=5)
    v.sym("hull", -4.0, 3.2, 0.45, 1.1, 1.78, 1.82, "ru_d")                                      # jupes épaisses
    v.box("hull", 2.0, 3.1, 1.65, 1.9, -0.7, 0.7, "ru_d")                                         # capsule d'équipage
    v.sym("hull", 2.6, 3.0, 1.9, 2.0, 0.2, 0.6, "glass")
    v.turret = (-0.6, 1.65, 0.0)
    # tourelle inhabitée, à facettes
    v.box("turret", -2.6, 0.8, 1.65, 2.2, -1.15, 1.15, p)
    v.slope("turret", 0.8, 1.8, 1.65, 2.2, -1.05, 1.05, p, 4)
    v.box("turret", -2.4, 0.6, 2.2, 2.45, -0.95, 0.95, p)
    v.slope("turret", 0.6, 1.3, 2.2, 2.45, -0.9, 0.9, p, 2)
    v.box("turret", -0.6, 0.2, 2.45, 2.8, -0.25, 0.25, "ru_d")                                    # viseur panoramique
    v.box("turret", -0.5, 0.1, 2.55, 2.75, -0.26, 0.26, "glass")
    for i in range(3):                                                                              # APS Afganit (tubes latéraux)
        v.sym("turret", -1.8 + i * 0.45, -1.5 + i * 0.45, 1.85, 2.05, 1.15, 1.35, "dark")
    v.sym("turret", -0.5, 0.3, 1.8, 2.0, 1.15, 1.2, "radar")                                       # radar AESA
    v.gunp = (1.0, 2.0, 0.0)
    cannon(v, "gun", 1.0, 2.0, 0.0, 5.3, 0.1, extractor=False)
    v.cyl("gun", "x", (2.0, 0.42), 0.05, 0.8, 2.2, "gun")                                         # coaxial 30 mm
    v.muzzle(6.35, 2.0, 0.0, 2)
    v.muzzle(2.25, 2.0, 0.42, 2)
    v.seat(2.6, 1.05, -0.45)
    v.seat(2.6, 1.05, 0.0)
    v.seat(2.6, 1.05, 0.45)
    return v


def bmpt():
    v = V("bmpt_terminator", 3.0)
    p = "ru"
    tank_hull(v, -3.5, 3.3, 3.55, 1.5, p, 6, 0.38, era="ru_d")
    v.turret = (-0.3, 1.5, 0.0)
    v.box("turret", -1.5, 0.9, 1.5, 2.05, -0.9, 0.9, p)
    v.slope("turret", 0.9, 1.5, 1.5, 2.05, -0.85, 0.85, p, 3)
    v.box("turret", -1.0, 0.4, 2.05, 2.35, -0.4, 0.4, "ru_d")                                     # viseurs
    v.box("turret", -0.2, 0.3, 2.1, 2.3, -0.42, -0.1, "glass")
    v.gunp = (0.6, 2.0, 0.0)
    for s in (-1, 1):
        # caissons latéraux : 2 x 30 mm (ici un par côté) + 2 Ataka
        v.box("gun", -0.8, 0.9, 1.75, 2.3, s * 0.95, s * 1.45, "ru")
        v.cyl("gun", "x", (2.2, s * 1.2), 0.055, 0.9, 3.4, "gun")
        v.cyl("gun", "x", (1.98, s * 1.2), 0.08, 0.5, 1.2, "gun")
        for dy in (0.0, 0.28):
            v.cyl("gun", "x", (2.55 + dy, s * 1.2), 0.12, -0.9, 0.9, "missile_g")                 # Ataka
    v.box("hull", 1.6, 2.0, 1.5, 1.7, 0.9, 1.2, "dark")                                            # AG-17D (caisse)
    v.box("hull", 1.6, 2.0, 1.5, 1.7, -1.2, -0.9, "dark")
    v.muzzle(3.45, 2.2, -1.2, 2)
    v.muzzle(3.45, 2.2, 1.2, 2)
    v.muzzle(0.95, 2.69, -1.2, 2)
    v.muzzle(0.95, 2.69, 1.2, 2)
    v.muzzle(2.05, 1.6, 1.05, 0)                                                                    # lance-grenades
    v.seat(2.2, 0.75, 0.0)
    v.seat(-0.3, 1.05, 0.45)
    v.seat(-0.3, 1.05, -0.45)
    v.seat(1.8, 0.75, 1.0)
    v.seat(1.8, 0.75, -1.0)
    return v


def sprut():
    v = V("sprut_sd", 3.25)
    p = "ru"
    tracks(v, -3.4, 3.3, 1.55, 0.42, 0.8, 7, 0.3, p)
    v.box("hull", -3.4, 2.3, 0.4, 1.4, -1.15, 1.15, p)
    v.slope("hull", 2.3, 3.45, 0.4, 1.4, -1.15, 1.15, p, 4)
    v.box("hull", -3.4, 2.3, 1.4, 1.55, -1.55, 1.55, p)
    v.box("hull", -3.5, -3.4, 0.5, 1.3, -0.6, 0.6, "metal")                                       # hydrojets
    v.turret = (-0.2, 1.55, 0.0)
    v.box("turret", -1.6, 0.9, 1.55, 2.1, -1.05, 1.05, p)
    v.slope("turret", 0.9, 1.6, 1.55, 2.1, -1.0, 1.0, p, 3)
    v.box("turret", -0.7, -0.1, 2.1, 2.3, 0.3, 0.8, "ru_d")
    v.sym("turret", -1.3, -0.9, 2.1, 2.25, 0.9, 1.05, "dark")
    v.gunp = (1.3, 1.85, 0.0)
    cannon(v, "gun", 1.3, 1.85, 0.0, 4.6, 0.095)
    v.muzzle(5.95, 1.85, 0.0, 2)
    v.seat(2.1, 0.7, 0.0)
    v.seat(-0.2, 1.0, 0.45)
    v.seat(-0.2, 1.0, -0.45)
    return v


# =========================================================================== véhicules américains
def humvee(avenger=False):
    v = V("avenger" if avenger else "humvee", 2.0)
    p = "us"
    v.wheel_r, v.wheel_w = 0.45, 0.35
    chassis(v, -2.2, 2.2, 0.85, 0.5, 0.85)
    v.box("hull", -2.3, 2.35, 0.85, 1.3, -1.08, 1.08, p)                                         # caisse large et basse
    v.slope("hull", 0.9, 2.35, 1.3, 1.55, -1.0, 1.0, p, 3, up_front=False)                        # capot plat
    v.box("hull", 2.35, 2.45, 0.7, 1.25, -0.8, 0.8, "dark")                                       # calandre
    v.sym("hull", 2.35, 2.47, 1.0, 1.2, 0.75, 0.95, "light")
    if not avenger:
        v.box("hull", -1.2, 0.9, 1.3, 1.95, -1.05, 1.05, p)                                       # habitacle blindé
        v.box("hull", 0.85, 0.95, 1.4, 1.9, -0.95, 0.95, "glass")
        v.sym("hull", -1.0, 0.7, 1.45, 1.85, 1.05, 1.07, "glass")
        v.box("hull", -2.3, -1.2, 1.3, 1.6, -1.05, 1.05, p)                                       # coffre arrière
        v.box("hull", -0.55, 0.25, 1.95, 2.05, -0.55, 0.55, "us_d")                               # anneau de tourelle
        v.turret = (-0.15, 2.05, 0.0)
        v.box("turret", -0.55, 0.25, 2.05, 2.2, -0.5, 0.5, "us_d")
        v.box("turret", 0.25, 0.32, 2.1, 2.75, -0.55, 0.55, p)                                    # bouclier O-GPK
        v.sym("turret", -0.4, 0.3, 2.1, 2.65, 0.55, 0.6, p)
        v.gunp = (0.15, 2.5, 0.0)
        mg(v, "gun", 0.15, 2.5, 0.0, 1.2, 0.065)                                                  # M2 .50
        v.box("gun", -0.15, 0.15, 2.3, 2.42, -0.25, -0.08, "dark")                                # Mk 19 (variante)
        v.muzzle(1.4, 2.5, 0.0, 2)
        v.seat(0.3, 0.9, -0.45)
        v.seat(0.3, 0.9, 0.45)
        v.seat(-0.15, 1.55, 0.0)                                                                  # tireur de tourelle
        v.seat(-0.7, 0.9, -0.45)
        v.seat(-0.7, 0.9, 0.45)
    else:
        v.box("hull", 0.2, 1.0, 1.3, 1.95, -1.05, 1.05, p)                                        # cabine 2 places
        v.box("hull", 0.95, 1.02, 1.4, 1.9, -0.95, 0.95, "glass")
        v.box("hull", -2.3, 0.2, 1.3, 1.45, -1.05, 1.05, "us_d")                                  # plateau
        v.turret = (-1.1, 1.45, 0.0)
        v.cyl("turret", "y", (-1.1, 0.0), 0.5, 1.45, 1.65, "us_d")
        v.box("turret", -1.55, -0.65, 1.65, 2.5, -0.45, 0.45, p)                                  # tourelle gyrostabilisée
        v.box("turret", -1.0, -0.7, 2.0, 2.3, -0.46, 0.46, "glass")
        v.gunp = (-1.1, 2.1, 0.0)
        for s in (-1, 1):                                                                           # 2 pods de 4 Stinger
            v.box("gun", -1.75, -0.3, 1.8, 2.45, s * 0.5, s * 1.1, "us_d")
            for dy in (0.0, 0.3):
                for dz in (0.0, 0.28):
                    v.box("gun", -0.32, -0.28, 1.88 + dy, 2.12 + dy, s * (0.6 + dz), s * (0.6 + dz) + 0.22 * s, "dark")
        mg(v, "gun", -0.7, 1.75, 0.0, 1.0, 0.055)                                                 # M3P
        v.muzzle(-0.25, 2.12, -0.8, 2)
        v.muzzle(-0.25, 2.12, 0.8, 2)
        v.muzzle(0.35, 1.75, 0.0, 2)
        v.seat(0.6, 0.9, -0.45)
        v.seat(-1.1, 1.5, 0.0)
    v.axles([1.55, -1.5], 0.95)
    return v


def matv():
    v = V("m_atv", 2.5)
    p = "us"
    v.wheel_r, v.wheel_w = 0.6, 0.42
    chassis(v, -2.8, 2.6, 0.95, 0.65, 1.05)
    v.slope("hull", -2.9, 1.5, 0.7, 1.15, -0.9, 0.9, "us_d", 2)                                   # coque en V
    v.box("hull", -2.9, 1.3, 1.15, 2.55, -1.2, 1.2, p)
    v.slope("hull", 1.3, 3.05, 1.15, 1.9, -1.1, 1.1, p, 4)                                        # capot
    v.slope("hull", 0.9, 1.35, 1.9, 2.55, -1.15, 1.15, "glass", 3)
    v.sym("hull", -1.0, 0.6, 1.9, 2.35, 1.2, 1.22, "glass")
    v.box("hull", 3.0, 3.15, 0.8, 1.5, -0.9, 0.9, "dark")
    v.sym("hull", 3.0, 3.12, 1.3, 1.5, 0.7, 0.95, "light")
    v.box("hull", -3.05, -2.9, 1.3, 2.3, -0.7, 0.7, "tire")                                      # roue de secours
    v.box("hull", -1.2, -0.2, 2.55, 2.65, -0.55, 0.55, "us_d")
    v.turret = (-0.7, 2.65, 0.0)
    v.box("turret", -1.15, -0.25, 2.65, 2.8, -0.5, 0.5, "us_d")
    v.box("turret", -0.25, -0.18, 2.7, 3.35, -0.6, 0.6, p)
    v.sym("turret", -1.0, -0.2, 2.7, 3.2, 0.6, 0.65, p)
    v.gunp = (-0.3, 3.05, 0.0)
    mg(v, "gun", -0.3, 3.05, 0.0, 1.25, 0.065)
    v.muzzle(1.0, 3.05, 0.0, 2)
    v.axles([1.9, -1.9], 1.05)
    v.seat(0.5, 1.2, -0.5)
    v.seat(0.5, 1.2, 0.5)
    v.seat(-0.7, 2.15, 0.0)
    v.seat(-1.3, 1.2, -0.5)
    v.seat(-1.3, 1.2, 0.5)
    return v


def m939():
    v = V("m939", 3.0)
    p = "us"
    v.wheel_r, v.wheel_w = 0.58, 0.4
    chassis(v, -3.9, 3.7, 0.9, 0.65, 1.05)
    v.box("hull", 2.0, 3.7, 1.0, 1.85, -0.95, 0.95, p)                                            # capot long
    v.sym("hull", 2.0, 3.6, 1.0, 1.35, 0.95, 1.25, p)                                             # ailes
    v.box("hull", 3.7, 3.8, 1.05, 1.75, -0.8, 0.8, "dark")
    v.sym("hull", 3.65, 3.78, 1.55, 1.75, 0.95, 1.15, "light")
    v.box("hull", 0.8, 2.0, 1.0, 2.8, -1.2, 1.2, p)                                               # cabine
    v.box("hull", 1.98, 2.02, 2.0, 2.65, -1.05, 1.05, "glass")
    v.sym("hull", 1.0, 1.8, 2.05, 2.6, 1.2, 1.22, "glass")
    v.box("hull", -4.0, 0.6, 1.05, 1.45, -1.25, 1.25, "us_d")
    v.sym("hull", -4.0, 0.6, 1.45, 2.0, 1.2, 1.25, p)
    for x in (-3.7, -2.5, -1.3, -0.1):
        v.box("hull", x, x + 0.08, 2.0, 3.0, -1.26, 1.26, "dark")
    v.box("hull", -4.0, 0.6, 2.9, 3.1, -1.26, 1.26, "canvas_us")
    v.sym("hull", -4.0, 0.6, 2.0, 2.9, 1.24, 1.27, "canvas_us")
    v.box("hull", 0.5, 0.6, 2.0, 3.0, -1.26, 1.26, "canvas_us")
    v.axles([2.9, -1.6, -2.9], 1.05)
    v.seat(1.4, 1.15, -0.5)
    v.seat(1.4, 1.15, 0.5)
    for i in range(4):
        v.seat(-3.3 + i * 0.9, 1.5, -0.8)
        v.seat(-3.3 + i * 0.9, 1.5, 0.8)
    return v


def stryker():
    v = V("stryker", 3.0)
    p = "us"
    v.wheel_r, v.wheel_w = 0.58, 0.4
    v.box("hull", -3.4, 2.6, 0.65, 2.1, -1.3, 1.3, p)
    v.slope("hull", 2.6, 3.55, 0.65, 2.1, -1.25, 1.25, p, 5)
    v.sym("hull", -3.4, 2.6, 1.4, 2.1, 1.3, 1.38, "us_d")                                       # blindage additionnel
    v.box("hull", -3.5, -3.4, 0.75, 2.0, -0.7, 0.7, "us_d")                                      # rampe arrière
    v.sym("hull", 2.4, 2.8, 2.1, 2.25, 0.5, 0.9, "glass")
    v.sym("hull", 3.4, 3.52, 1.3, 1.45, 0.7, 0.95, "light")
    v.box("hull", -2.8, -0.8, 2.1, 2.2, -1.0, 1.0, "us_d")
    v.axles([2.2, 0.9, -1.0, -2.3], 1.25)
    v.turret = (0.4, 2.2, 0.0)
    v.cyl("turret", "y", (0.4, 0.0), 0.4, 2.2, 2.35, "us_d")                                    # station téléopérée
    v.box("turret", 0.0, 0.8, 2.35, 2.85, -0.35, 0.35, p)
    v.box("turret", 0.3, 0.75, 2.6, 2.8, 0.36, 0.5, "glass")                                     # caméra thermique
    v.gunp = (0.6, 2.75, -0.15)
    mg(v, "gun", 0.6, 2.75, -0.15, 1.2, 0.065)
    v.muzzle(1.85, 2.75, -0.15, 2)
    v.seat(2.3, 1.1, -0.5)
    v.seat(0.6, 1.1, 0.5)
    for i in range(9):
        v.seat(-0.2 - (i // 2) * 0.75, 1.05, -0.6 if i % 2 == 0 else 0.6)
    return v


def m113():
    v = V("m113", 2.5)
    p = "us"
    tracks(v, -2.4, 2.4, 1.35, 0.4, 0.75, 5, 0.32, p)
    v.box("hull", -2.5, 1.9, 0.4, 2.0, -0.95, 0.95, p)                                           # caisse en boîte
    v.slope("hull", 1.9, 2.55, 0.4, 2.0, -0.95, 0.95, p, 4)
    v.box("hull", -2.5, 1.9, 1.35, 1.5, -1.35, 1.35, p)
    v.box("hull", -2.6, -2.5, 0.5, 1.9, -0.8, 0.8, "us_d")                                       # rampe
    v.box("hull", 0.2, 0.9, 2.0, 2.12, -0.55, 0.15, "us_d")                                      # tourelleau
    v.sym("hull", 2.0, 2.3, 1.4, 1.55, 0.6, 0.8, "light")
    v.turret = (0.55, 2.12, -0.2)
    v.box("turret", 0.2, 0.9, 2.12, 2.2, -0.55, 0.15, "us_d")
    v.box("turret", 0.85, 0.9, 2.15, 2.65, -0.7, 0.3, p)                                         # bouclier
    v.gunp = (0.7, 2.45, -0.2)
    mg(v, "gun", 0.7, 2.45, -0.2, 1.2, 0.065)
    v.muzzle(1.95, 2.45, -0.2, 2)
    v.seat(1.4, 0.85, -0.45)
    v.seat(0.55, 1.55, -0.2)
    for i in range(9):
        v.seat(0.3 - (i // 2) * 0.6, 0.85, -0.5 if i % 2 == 0 else 0.5)
    return v


def bradley():
    v = V("m2a3_bradley", 3.0)
    p = "us"
    tracks(v, -3.2, 3.1, 1.8, 0.5, 0.95, 6, 0.38, p)
    v.box("hull", -3.3, 2.0, 0.45, 1.9, -1.3, 1.3, p)
    v.slope("hull", 2.0, 3.35, 0.45, 1.9, -1.3, 1.3, p, 4)
    v.box("hull", -3.3, 2.0, 1.2, 1.9, -1.8, 1.8, p)                                              # blindage espacé
    for i in range(6):
        v.sym("hull", -3.0 + i * 0.85, -2.3 + i * 0.85, 1.25, 1.85, 1.8, 1.84, "us_d")
    v.box("hull", -3.4, -3.3, 0.6, 1.8, -0.8, 0.8, "us_d")
    v.sym("hull", 2.9, 3.1, 1.6, 1.75, 0.7, 0.95, "light")
    v.turret = (0.0, 1.9, 0.0)
    v.box("turret", -1.3, 1.1, 1.9, 2.7, -1.0, 1.0, p)
    v.slope("turret", 1.1, 1.6, 1.9, 2.7, -0.95, 0.95, p, 3)
    v.box("turret", -1.7, -1.3, 2.0, 2.6, -0.9, 0.9, "us_d")
    v.box("turret", -0.3, 0.5, 2.7, 3.0, 0.3, 0.75, "us_d")                                       # viseur commandant
    v.box("turret", 0.1, 0.45, 2.75, 2.95, 0.31, 0.74, "glass")
    v.box("turret", -1.2, 0.6, 2.0, 2.6, -1.55, -1.0, "us_d")                                     # lanceur TOW double
    v.box("turret", 0.6, 0.65, 2.05, 2.55, -1.5, -1.05, "dark")
    v.gunp = (1.4, 2.35, 0.3)
    v.cyl("gun", "x", (2.35, 0.3), 0.15, 1.2, 1.8, "gun")
    v.cyl("gun", "x", (2.35, 0.3), 0.06, 1.8, 3.9, "gun")                                         # M242 25 mm
    v.muzzle(3.95, 2.35, 0.3, 2)
    v.muzzle(0.7, 2.3, -1.28, 1)
    v.seat(2.1, 0.8, -0.55)
    v.seat(0.0, 1.3, 0.45)
    v.seat(0.0, 1.3, -0.45)
    for i in range(6):
        v.seat(-1.0 - (i // 2) * 0.7, 0.85, -0.55 if i % 2 == 0 else 0.55)
    return v


def abrams():
    v = V("m1a2_abrams", 3.5)
    p = "us"
    tank_hull(v, -3.9, 3.7, 3.66, 1.6, p, 7, 0.38, nose_steps=3)
    v.sym("hull", -3.6, 3.3, 0.45, 1.1, 1.83, 1.88, "us_d")                                       # jupes
    for i in range(3):                                                                               # échappement de turbine
        v.box("hull", -4.0, -3.9, 0.9 + i * 0.18, 1.0 + i * 0.18, -1.0, 1.0, "dark")
    v.turret = (-0.3, 1.6, 0.0)
    v.box("turret", -2.0, 1.2, 1.6, 2.35, -1.35, 1.35, p)                                          # tourelle plate et anguleuse
    v.slope("turret", 1.2, 2.0, 1.6, 2.35, -1.3, 1.3, p, 4)
    v.box("turret", -3.2, -2.0, 1.65, 2.3, -1.25, 1.25, p)                                          # gros bustle
    v.box("turret", -3.3, -2.0, 2.3, 2.5, -1.3, 1.3, "us_d")                                        # paniers
    v.box("turret", -0.9, -0.2, 2.35, 2.6, 0.35, 0.95, "us_d")                                       # tourelleau
    mg(v, "turret", -0.55, 2.8, 0.65, 1.1, 0.065)                                                    # M2 commandant
    mg(v, "turret", -0.4, 2.7, -0.6, 0.8, 0.045)                                                     # M240 chargeur
    v.box("turret", 0.2, 0.7, 2.35, 2.6, -0.4, -0.1, "glass")
    v.sym("turret", -0.8, -0.4, 2.0, 2.3, 1.35, 1.45, "dark")
    v.gunp = (1.7, 2.0, 0.0)
    cannon(v, "gun", 1.7, 2.0, 0.0, 4.2, 0.1, sleeve=True, extractor=True)
    v.muzzle(5.95, 2.0, 0.0, 2)
    v.muzzle(0.6, 2.8, 0.65, 1)
    v.seat(2.4, 0.75, 0.0)
    v.seat(-0.3, 1.1, 0.5)
    v.seat(-0.3, 1.1, -0.5)
    v.seat(-0.8, 1.1, 0.0)
    return v


def fmtv_truck(v, p, L0, L1, cab_x):
    chassis(v, L0, L1, 0.9, 0.65, 1.05)
    v.box("hull", cab_x, L1, 1.0, 2.9, -1.2, 1.2, p)                                                # cabine avancée
    v.box("hull", L1 - 0.02, L1 + 0.03, 2.0, 2.7, -1.05, 1.05, "glass")
    v.sym("hull", cab_x + 0.3, L1 - 0.3, 2.1, 2.6, 1.2, 1.22, "glass")
    v.box("hull", L1, L1 + 0.1, 1.05, 1.7, -0.9, 0.9, "dark")
    v.sym("hull", L1, L1 + 0.12, 1.4, 1.6, 0.8, 1.05, "light")


def himars():
    v = V("m142_himars", 3.0)
    p = "us"
    v.wheel_r, v.wheel_w = 0.58, 0.4
    fmtv_truck(v, p, -3.6, 3.4, 1.6)
    v.box("hull", -3.7, 1.4, 1.05, 1.45, -1.2, 1.2, "us_d")
    v.turret = (-1.0, 1.45, 0.0)
    v.cyl("turret", "y", (-1.0, 0.0), 0.75, 1.45, 1.65, "us_d")
    v.sym("turret", -1.6, -0.4, 1.65, 2.0, 0.7, 0.85, p)
    v.gunp = (-2.6, 1.9, 0.0)
    v.box("gun", -3.6, 1.2, 1.75, 2.75, -1.0, 1.0, p)                                                # pod de 6 roquettes
    for r in range(2):
        for c in range(3):
            v.box("gun", 1.2, 1.25, 1.85 + r * 0.45, 2.25 + r * 0.45, -0.9 + c * 0.62, -0.42 + c * 0.62, "dark")
    v.box("gun", -3.62, 1.22, 2.4, 2.48, -1.02, 1.02, "us_d")
    v.muzzle(1.3, 2.25, 0.0, 2)
    v.axles([2.6, -1.4, -2.7], 1.05)
    v.seat(2.4, 1.15, -0.55)
    v.seat(2.4, 1.15, 0.0)
    v.seat(2.4, 1.15, 0.55)
    return v


def blackhawk():
    v = V("uh_60_black_hawk", 5.5)
    p = "usg"
    v.box("hull", -2.0, 3.0, 0.8, 2.6, -1.1, 1.1, p)                                                # cabine
    v.slope("hull", 3.0, 4.9, 0.8, 2.5, -1.0, 1.0, p, 5)                                           # nez
    v.box("hull", 3.0, 4.2, 1.7, 2.5, -0.95, 0.95, "glass")                                         # cockpit vitré
    v.sym("hull", 0.0, 1.8, 1.3, 2.3, 1.1, 1.12, "dark")                                            # portes ouvertes
    v.box("hull", -1.8, 2.6, 2.6, 3.1, -0.8, 0.8, p)                                                # carénage moteurs
    v.sym("hull", 0.9, 1.6, 2.65, 3.05, 0.8, 0.9, "dark")
    v.box("hull", -7.6, -2.0, 1.9, 2.5, -0.35, 0.35, p)                                             # poutre de queue
    v.box("hull", -7.95, -7.2, 2.2, 4.0, -0.12, 0.12, p)                                           # dérive
    v.box("hull", -7.6, -6.8, 2.0, 2.1, -1.3, 1.3, p)                                               # stabilisateur
    v.sym("hull", 2.3, 2.7, 0.3, 0.85, 1.0, 1.15, "dark")                                           # train
    v.sym("hull", 2.3, 2.7, 0.0, 0.3, 1.0, 1.2, "tire")
    v.box("hull", -7.0, -6.7, 1.3, 1.95, -0.06, 0.06, "dark")
    v.box("hull", -7.0, -6.7, 1.15, 1.3, -0.12, 0.12, "tire")
    for s in (-1, 1):                                                                                 # 2 miniguns M134 aux sabords
        v.box("hull", 1.95, 2.35, 1.7, 1.95, s * 1.12, s * 1.35, "dark")
        v.cyl("hull", "x", (1.82, s * 1.25), 0.07, 2.35, 3.0, "gun")
    v.rotor = (0.6, 3.5, 0.0)
    v.box("hull", 0.4, 0.8, 3.1, 3.5, -0.2, 0.2, "metal")
    rotor_blades(v, 4, 7.0, 0.5, 3.5)
    v.tail = (-6.8, 3.4, 0.25)
    tail_rotor_blades(v, 1.3, 0.25)
    v.muzzle(3.05, 1.82, -1.25, 0)
    v.muzzle(3.05, 1.82, 1.25, 0)
    v.seat(3.4, 1.4, -0.45)                                                                          # pilote
    v.seat(2.2, 1.3, -0.9)                                                                           # sabord gauche
    v.seat(2.2, 1.3, 0.9)                                                                            # sabord droit
    v.seat(3.4, 1.4, 0.45)                                                                           # copilote
    for i in range(8):
        v.seat(1.2 - (i // 2) * 0.8, 1.0, -0.5 if i % 2 == 0 else 0.5)
    return v


def apache():
    v = V("ah_64e_apache", 5.0)
    p = "usg"
    v.box("hull", -2.0, 2.5, 0.9, 2.3, -0.7, 0.7, p)                                                # fuselage étroit
    v.slope("hull", 2.5, 5.0, 0.9, 2.1, -0.6, 0.6, p, 5)
    v.box("hull", 2.4, 3.9, 2.0, 2.75, -0.45, 0.45, "glass")                                        # cockpits en tandem
    v.box("hull", 3.9, 4.8, 1.9, 2.4, -0.4, 0.4, "glass")
    v.sym("hull", -0.6, 1.4, 1.4, 2.4, 0.7, 1.2, p)                                                 # nacelles moteurs
    v.box("hull", -1.6, 2.0, 2.3, 2.7, -0.55, 0.55, p)
    v.box("hull", -7.2, -2.0, 1.9, 2.4, -0.28, 0.28, p)                                             # poutre de queue
    v.box("hull", -7.45, -6.8, 2.0, 3.7, -0.1, 0.1, p)
    v.box("hull", -7.4, -6.6, 2.0, 2.1, -1.1, 1.1, p)
    v.box("hull", -0.3, 0.9, 1.65, 1.8, -2.5, 2.5, p)                                               # ailerons
    for s in (-1, 1):
        for zz in (1.4, 2.3):
            v.box("hull", 0.1, 0.4, 1.4, 1.65, s * zz - 0.06, s * zz + 0.06, "dark")
        v.cyl("hull", "x", (1.15, s * 1.4), 0.3, -0.6, 0.9, "usg_d")                               # Hydra (19)
        for dy in (0.0, 0.25):
            for dz in (-0.15, 0.15):
                v.cyl("hull", "x", (1.15 + dy, s * 2.3 + dz), 0.08, -0.5, 0.9, "missile")          # Hellfire
    v.sym("hull", 1.0, 1.4, 0.3, 0.9, 0.9, 1.05, "dark")                                            # train
    v.sym("hull", 1.0, 1.4, 0.0, 0.3, 0.9, 1.1, "tire")
    v.box("hull", -6.8, -6.5, 1.3, 1.9, -0.06, 0.06, "dark")
    v.box("hull", -6.8, -6.5, 1.15, 1.3, -0.12, 0.12, "tire")
    v.turret = (3.2, 0.9, 0.0)                                                                       # canon M230 sous le nez
    v.cyl("turret", "y", (3.2, 0.0), 0.28, 0.6, 0.9, "usg_d")
    v.gunp = (3.2, 0.65, 0.0)
    v.cyl("gun", "x", (0.65, 0.0), 0.06, 3.2, 4.4, "gun")
    v.box("gun", 3.0, 3.5, 0.5, 0.8, -0.15, 0.15, "dark")
    v.box("hull", 4.85, 5.1, 1.4, 1.75, -0.25, 0.25, "glass")                                       # TADS
    v.rotor = (0.3, 3.15, 0.0)
    v.box("hull", 0.1, 0.5, 2.7, 3.15, -0.15, 0.15, "metal")
    v.cyl("hull", "y", (0.3, 0.0), 0.35, 3.35, 3.7, "usg_d")                                        # radar Longbow
    rotor_blades(v, 4, 6.1, 0.45, 3.15)
    v.tail = (-6.3, 3.2, 0.25)
    tail_rotor_blades(v, 1.1, 0.2)
    v.muzzle(4.45, 0.65, 0.0, 2)
    v.muzzle(0.95, 1.15, -1.4, 0)
    v.muzzle(0.95, 1.15, 1.4, 0)
    v.muzzle(0.95, 1.28, -2.3, 0)
    v.muzzle(0.95, 1.28, 2.3, 0)
    v.seat(3.1, 1.55, 0.0)                                                                            # pilote (arrière)
    v.seat(4.1, 1.35, 0.0)                                                                            # artilleur (avant)
    return v


# =========================================================================== bombardier stratégique
def prop_model(v, r):
    """Hélice tripale centrée (en pixels) sur (8, 8, 8), dans le plan YZ (elle tourne autour de l'axe X)."""
    k = 16 / v.S
    R = r * k
    e = []

    def b(x0, y0, z0, x1, y1, z1, key, rot=None):
        el = {"from": [round(x0, 3), round(y0, 3), round(z0, 3)], "to": [round(x1, 3), round(y1, 3), round(z1, 3)],
              "faces": {f: {"uv": [0, 0, 1, 1], "texture": "#p"} for f in vb.FACES}, "__key": key}
        if rot is not None:
            el["rotation"] = {"axis": "x", "angle": rot, "origin": [8, 8, 8]}
        e.append(el)
    b(7.2, 7.4, 7.4, 8.9, 8.6, 8.6, "metal")                                        # moyeu (cône)
    b(8.9, 7.7, 7.7, 9.4, 8.3, 8.3, "metal")
    w = max(0.5, 0.35 * k)
    for ang in (-45, 0, 45):                                                         # 3 pales (pas de 22,5° imposé)
        b(7.8, 8 - w / 2, 8 - R, 8.3, 8 + w / 2, 8 - 0.4, "rotor", ang)
    b(7.8, 8 - 0.4, 8 + 0.4, 8.3, 8 + 0.4, 8 + R * 0.9, "rotor", 22.5)
    return e


PROP_R = {"il2_sturmovik": 1.6, "la7": 1.5, "p51_mustang": 1.65, "tu95_bear": 2.8, "c130_hercules": 2.0}


def b29():
    """Boeing B-29 Superfortress : 28 m d'envergure, 20 m de long, quadrimoteur, soute ventrale, 4 tourelles."""
    v = V("b29_superfortress", 10.0)
    p = "alu"
    cy = 2.7                                                                         # axe du fuselage (train sorti)
    v.cyl("hull", "x", (cy, 0.0), 1.45, -7.5, 7.6, p)                               # fuselage
    v.cyl("hull", "x", (cy + 0.1, 0.0), 1.1, -9.6, -7.5, p)                         # cône arrière
    v.cyl("hull", "x", (cy - 0.05, 0.0), 1.25, 7.6, 8.8, p)
    v.cyl("hull", "x", (cy - 0.1, 0.0), 1.0, 8.8, 9.6, "glass")                     # nez vitré en dôme
    v.cyl("hull", "x", (cy - 0.15, 0.0), 0.6, 9.6, 10.1, "glass")
    v.box("hull", 8.8, 9.7, cy - 0.3, cy + 0.9, -0.5, 0.5, "glass_frame")          # montants du vitrage
    # ailes (envergure 28 m), trapézoïdales, avec saumons
    for i in range(14):
        z0, z1 = 1.2 + i * 0.9, 1.2 + (i + 1) * 0.9
        f = i / 13
        chord = 4.2 - 2.6 * f
        xl = 3.6 - 0.9 * f                                                           # bord d'attaque en flèche légère
        for s in (-1, 1):
            a, b_ = sorted((s * z0, s * z1))
            v.box("hull", xl - chord, xl, cy - 0.25 + f * 0.25, cy + 0.12 + f * 0.25, a, b_, p)
            v.box("hull", xl - chord, xl - chord + 0.4, cy - 0.22 + f * 0.25, cy + 0.08 + f * 0.25, a, b_, "alu_d")  # volets
    v.box("hull", -0.6, 3.6, cy - 0.3, cy + 0.15, -1.4, 1.4, p)                      # emplanture
    v.sym("hull", 0.0, 0.6, cy - 0.2, cy + 0.08, 13.8, 13.95, "red")                # feux de position
    # 4 moteurs en étoile dans leurs nacelles
    for zz in (4.4, 8.4):
        for s in (-1, 1):
            z = s * zz
            v.cyl("hull", "x", (cy - 0.15, z), 0.78, 0.0, 4.6, p)
            v.cyl("hull", "x", (cy - 0.15, z), 0.82, 4.4, 4.9, "dark")               # capot moteur
            v.box("hull", -0.6, 0.2, cy - 0.7, cy - 0.3, z - 0.4, z + 0.4, "dark")   # échappements / turbo
            v.props.append((5.25, cy - 0.15, z))
    # empennage : dérive haute et plans horizontaux
    v.slope("hull", -9.4, -6.5, cy + 0.6, 6.0, -0.15, 0.15, p, 6, up_front=False)
    v.box("hull", -9.6, -8.6, cy + 0.6, 6.0, -0.15, 0.15, p)
    v.box("hull", -9.3, -7.0, cy + 0.05, cy + 0.3, -5.2, 5.2, p)
    v.box("hull", -9.3, -8.7, cy + 0.05, cy + 0.3, -5.2, 5.2, "alu_d")
    v.box("hull", -9.65, -9.4, 4.8, 5.6, -0.17, 0.17, "red")
    # marques : étoiles de l'US Air Force (bleu / blanc)
    for s in (-1, 1):
        v.box("hull", -5.2, -4.0, cy - 0.5, cy + 0.7, s * 1.46 - 0.02, s * 1.46 + 0.02, "navy")
        v.box("hull", -4.9, -4.3, cy - 0.2, cy + 0.4, s * 1.48 - 0.02, s * 1.48 + 0.02, "white")
        v.box("hull", 1.0, 2.6, cy + 0.2, cy + 0.21, s * 10.0 - 0.7, s * 10.0 + 0.7, "navy")
    # tourelles défensives (M2 jumelées)
    def turret(x, y, z, top):
        v.cyl("hull", "y", (x, z), 0.55, y - 0.2, y + 0.25, "alu_d")
        v.box("hull", x - 0.35, x + 0.35, y + (0.25 if top else -0.5), y + (0.55 if top else -0.2), z - 0.35, z + 0.35, "glass")
        for dz in (-0.15, 0.15):
            v.cyl("hull", "x", (y + 0.05, z + dz), 0.05, x, x + 1.3, "gun")
    turret(5.2, cy + 1.45, 0.0, True)                                                 # dorsale avant
    turret(-2.6, cy + 1.45, 0.0, True)                                                # dorsale arrière
    turret(-1.5, cy - 1.45, 0.0, False)                                               # ventrale
    v.box("hull", -10.2, -9.5, cy - 0.3, cy + 0.6, -0.4, 0.4, "glass")               # poste de queue
    for dz in (-0.15, 0.15):
        v.cyl("hull", "x", (cy + 0.1, dz), 0.05, -11.0, -10.1, "gun")
    v.muzzle(6.5, cy + 1.5, 0.0, 0)
    v.muzzle(-1.3, cy + 1.5, 0.0, 0)
    v.muzzle(-0.2, cy - 1.45, 0.0, 0)
    v.muzzle(-11.0, cy + 0.1, 0.0, 0)
    # soute à bombes : trappes fermées (bay) ou ouvertes, pendant sous le fuselage (bay_open)
    v.box("bay", -1.8, 3.4, cy - 1.47, cy - 1.32, -0.95, 0.95, "alu_d")
    v.box("bay", -1.7, 3.3, cy - 1.49, cy - 1.47, -0.03, 0.03, "dark")
    for s in (-1, 1):
        a, b_ = sorted((s * 0.95, s * 1.05))
        v.box("bay_open", -1.8, 3.4, cy - 2.4, cy - 1.32, a, b_, "alu_d")
    v.box("bay_open", -1.7, 3.3, cy - 1.42, cy - 1.32, -0.9, 0.9, "dark")            # intérieur de la soute
    # train d'atterrissage (sorti) : 2 jambes principales à roues jumelées, train avant
    for s in (-1, 1):
        z = s * 4.4
        v.box("gear", 1.9, 2.2, 0.6, cy - 0.8, z - 0.1, z + 0.1, "metal")
        for dz in (-0.3, 0.3):
            v.cyl("gear", "z", (2.05, 0.6), 0.6, z + dz - 0.18, z + dz + 0.18, "tire")
            v.cyl("gear", "z", (2.05, 0.6), 0.3, z + dz - 0.2, z + dz + 0.2, "rim")
    v.box("gear", 7.2, 7.45, 0.45, cy - 1.0, -0.08, 0.08, "metal")
    for dz in (-0.22, 0.22):
        v.cyl("gear", "z", (7.3, 0.42), 0.42, dz - 0.12, dz + 0.12, "tire")
    # équipage : pilote, bombardier (nez), puis 4 tireurs
    v.seat(8.0, cy - 0.6, -0.35)
    v.seat(9.0, cy - 0.8, 0.0)
    v.seat(4.8, cy + 0.4, 0.0)
    v.seat(-2.4, cy + 0.4, 0.0)
    v.seat(-1.5, cy - 0.8, 0.5)
    v.seat(-9.5, cy - 0.4, 0.0)
    return v


# =========================================================================== lance-missiles stratégiques
def heavy_truck(v, p, L0, L1, axles, cab_len=1.8, h=2.9, wheels_half=1.25):
    chassis(v, L0, L1, 1.1, 0.7, 1.15)
    v.box("hull", L1 - cab_len, L1, 1.05, h, -1.4, 1.4, p)                                         # cabine
    v.box("hull", L1 - 0.02, L1 + 0.03, h - 0.9, h - 0.2, -1.25, 1.25, "glass")
    v.sym("hull", L1 - cab_len + 0.3, L1 - 0.3, h - 0.8, h - 0.25, 1.4, 1.42, "glass")
    v.box("hull", L1, L1 + 0.1, 1.1, 1.8, -1.1, 1.1, "dark")
    v.sym("hull", L1, L1 + 0.12, 1.5, 1.7, 1.0, 1.25, "light")
    v.axles(axles, wheels_half + 0.05)


def s400():
    v = V("s_400", 4.25)
    p = "sov"
    v.wheel_r, v.wheel_w = 0.68, 0.45
    heavy_truck(v, p, -6.2, 6.2, [5.0, 3.4, -2.6, -4.2])
    v.box("hull", -6.3, 4.1, 1.15, 1.6, -1.4, 1.4, "sov_d")
    v.sym("hull", -6.1, -5.7, 0.2, 1.15, 1.2, 1.4, "metal")                                         # béquilles
    v.sym("hull", 2.0, 2.4, 0.2, 1.15, 1.2, 1.4, "metal")
    v.box("hull", 2.4, 4.0, 1.6, 2.6, -1.3, 1.3, "sov_d")                                            # groupe électrogène
    # radar panoramique sur mât
    v.box("hull", 3.0, 3.3, 2.6, 4.0, -0.15, 0.15, "metal")
    v.radar = (3.15, 4.0, 0.0)
    v.box("radar", 2.6, 3.7, 4.0, 4.15, -0.2, 0.2, "metal")
    v.box("radar", 3.6, 3.75, 4.0, 5.2, -1.2, 1.2, "radar")
    v.box("radar", 3.55, 3.6, 4.1, 5.1, -1.1, 1.1, "dark")
    v.turret = (-5.8, 1.6, 0.0)
    v.gunp = (-5.8, 2.0, 0.0)
    # 4 conteneurs-lanceurs couchés vers l'avant (relevés à la verticale au déploiement)
    for zz in (-0.95, -0.32, 0.32, 0.95):
        v.cyl("gun", "x", (2.25, zz), 0.3, -6.3, 1.8, "sov")
        v.box("gun", 1.75, 1.85, 1.95, 2.55, zz - 0.3, zz + 0.3, "dark")
    v.box("gun", -6.0, -5.6, 1.85, 2.0, -1.3, 1.3, "metal")
    v.box("gun", -2.0, -1.8, 1.9, 2.6, -1.3, 1.3, "dark")
    v.muzzle(1.9, 2.25, -0.95, 2)
    v.seat(5.3, 1.2, -0.6)
    v.seat(5.3, 1.2, 0.6)
    return v


def s500():
    v = V("s_500", 4.75)
    p = "sov"
    v.wheel_r, v.wheel_w = 0.7, 0.48
    heavy_truck(v, p, -6.8, 6.6, [5.4, 4.0, -1.6, -3.0, -4.4], cab_len=2.0, h=3.0)
    v.box("hull", -6.9, 4.4, 1.15, 1.6, -1.45, 1.45, "sov_d")
    v.sym("hull", -6.6, -6.2, 0.2, 1.15, 1.25, 1.45, "metal")
    v.sym("hull", 2.2, 2.6, 0.2, 1.15, 1.25, 1.45, "metal")
    v.box("hull", 2.8, 4.3, 1.6, 2.7, -1.35, 1.35, "sov_d")
    v.box("hull", 3.4, 3.6, 2.7, 3.4, -0.2, 0.2, "metal")
    v.radar = (3.5, 3.4, 0.0)                                                                         # antenne AESA
    v.box("radar", 3.2, 3.8, 3.4, 3.55, -0.25, 0.25, "metal")
    v.box("radar", 3.75, 3.95, 3.4, 5.0, -1.3, 1.3, "radar")
    for i in range(6):
        v.box("radar", 3.95, 3.97, 3.5 + i * 0.25, 3.6 + i * 0.25, -1.2, 1.2, "dark")
    v.turret = (-6.3, 1.6, 0.0)
    v.gunp = (-6.3, 2.1, 0.0)
    for zz in (-0.65, 0.65):                                                                          # 2 conteneurs géants
        v.box("gun", -6.8, 2.2, 1.65, 2.95, zz - 0.6, zz + 0.6, "sov")
        v.box("gun", 2.2, 2.3, 1.75, 2.85, zz - 0.5, zz + 0.5, "dark")
        for x in (-5.5, -2.5, 0.5):
            v.box("gun", x, x + 0.15, 1.6, 3.0, zz - 0.62, zz + 0.62, "sov_d")
    v.muzzle(2.35, 2.3, -0.65, 2)
    v.seat(5.6, 1.25, -0.6)
    v.seat(5.6, 1.25, 0.6)
    return v


def iskander():
    v = V("iskander", 4.5)
    p = "sov"
    v.wheel_r, v.wheel_w = 0.7, 0.48
    heavy_truck(v, p, -6.2, 6.2, [5.0, 3.4, -2.8, -4.4], cab_len=2.0, h=2.9)
    v.box("hull", -6.3, 4.1, 1.15, 2.35, -1.4, 1.4, p)                                              # caisse fermée
    v.sym("hull", -6.2, 4.0, 2.35, 2.55, 0.9, 1.4, p)                                               # trappes (ouvertes)
    v.sym("hull", -6.2, 4.0, 2.55, 3.2, 1.35, 1.42, "sov_d")
    v.sym("hull", -6.1, -5.7, 0.2, 1.15, 1.2, 1.4, "metal")
    v.sym("hull", 1.6, 2.0, 0.2, 1.15, 1.2, 1.4, "metal")
    v.turret = (-5.8, 2.0, 0.0)
    v.gunp = (-5.8, 2.3, 0.0)
    for zz in (-0.5, 0.5):                                                                            # 2 missiles 9M723
        v.cyl("gun", "x", (2.3, zz), 0.4, -6.2, 0.9, "missile")
        v.cyl("gun", "x", (2.3, zz), 0.3, 0.9, 1.7, "missile")
        v.cyl("gun", "x", (2.3, zz), 0.15, 1.7, 2.1, "missile")
        v.box("gun", -6.0, -5.2, 2.0, 2.6, zz - 0.42, zz + 0.42, "dark")                            # tuyère
        for s in (-1, 1):
            v.box("gun", -5.6, -4.6, 2.25, 2.35, zz + s * 0.4, zz + s * 0.6, "missile")            # ailerons
    v.box("gun", -6.1, 1.0, 1.95, 2.05, -1.1, 1.1, "metal")                                          # bras érecteur
    v.muzzle(2.15, 2.3, -0.5, 2)
    v.seat(5.2, 1.2, -0.6)
    v.seat(5.2, 1.2, 0.6)
    return v


VEHICLES = [uaz, tigr, kamaz, brdm, btr80, bmp2, grad, shilka, mi24, t90m, t80, t14, bmpt, sprut,
            humvee, matv, m939, stryker, m113, bradley, abrams, himars, lambda: humvee(True), blackhawk, apache,
            s400, s500, iskander, b29]
import aircraft_models as am  # noqa: E402
COLORS.update(am.PLANE_COLORS)
PAINTS.update({k for k, val in am.PLANE_COLORS.items() if not k.endswith("glass")})
MATERIAL.update({"gold_glass": "glass"})
_planes = am.make(V)
VEHICLES += _planes
PLANES = {"b29_superfortress", "il2_sturmovik", "la7", "mig15", "tu95_bear", "mig21", "mig25", "su25", "mig29", "su34", "su57",
          "p51_mustang", "f86_sabre", "f4_phantom", "c130_hercules", "a10_thunderbolt", "f117_nighthawk", "f15e_strike_eagle",
          "f16_falcon", "b2_spirit", "f22_raptor"}


OX = {}


def check(v):
    for part, els in v.parts.items():
        for e in els:
            for c in e["from"] + e["to"]:
                if c < -16 or c > 32:
                    raise SystemExit(f"{v.name}/{part}: coordonnée {c} hors limites ({e['from']} {e['to']}) — augmenter l'échelle")


def java_vec(t):
    return "null" if t is None else f"new Vec3({t[0]}, {t[1]}, {t[2]})"


def main():
    only = set(sys.argv[1:])
    painter = vb.Painter(COLORS, material, {"glass": 150}, seed="mil")
    geo = []
    os.makedirs(os.path.join(ASSETS, "textures/item/vehicle"), exist_ok=True)
    for fn in VEHICLES:
        v = fn()
        # 2e passage : origine choisie pour centrer le véhicule dans les 48 pixels autorisés
        OX[v.name] = round((8 - (v.xmin + v.xmax) / 2 * 16 / v.S) * 2) / 2
        v = fn()
        geo.append(v)
        if only and v.name not in only:
            continue
        check(v)
        groups = dict(v.parts)
        groups["wheel"] = wheel_model(v, v.wheel_r, v.wheel_w)
        if v.props:
            groups["prop"] = prop_model(v, PROP_R.get(v.name, 1.85))
        if v.name in PLANES:
            for part in ("prop", "bay", "bay_open", "gear"):
                groups.setdefault(part, [])                                          # pièce vide (avion à réaction, sans soute)
        d = 2 if v.S >= 4 else 3
        baked = vb.bake(groups, d=d, skip_chamfer=("glass", "light", "ir", "track", "rotor", "radar", "canvas", "canvas_us", "dark"))
        vb.paint(baked, painter).save(os.path.join(ASSETS, f"textures/item/vehicle/{v.name}.png"))
        tex = {"p": f"{MOD}:item/vehicle/{v.name}", "particle": f"{MOD}:item/vehicle/{v.name}"}
        for part in groups:
            path = os.path.join(ASSETS, f"models/vehicle/{v.name}_{part}.json")
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8") as f:
                json.dump({"textures": tex, "elements": vb.elements(baked, part)}, f, separators=(",", ":"))
        # objet (icône 3D) : caisse + tourelle + canon
        els = []
        for part in ("hull", "turret", "gun"):
            if part in groups:
                els += vb.elements(baked, part)
        big = max(v.S, 2)
        s = round(0.8 / big, 3)
        with open(os.path.join(ASSETS, f"models/item/{v.name}.json"), "w", encoding="utf-8") as f:
            json.dump({"textures": tex, "elements": els, "display": {
                "gui": {"rotation": [25, -135, 0], "translation": [0, -1, 0], "scale": [s, s, s]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [s * 0.6] * 3},
                "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [s, s, s]},
                "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 2, 0], "scale": [s * 0.5] * 3},
                "firstperson_righthand": {"rotation": [0, 100, 0], "translation": [0, 2, 0], "scale": [s * 0.5] * 3}}}, f, separators=(",", ":"))
        print(f"{v.name} : échelle {v.S}, atlas {baked.w}x{baked.h}, {sum(len(e) for e in v.parts.values())} pavés")
    # données de géométrie (Java)
    lines = []
    for v in geo:
        wheels = ", ".join(f"{{{w[0]}f, {w[1]}f, {w[2]}f, {w[3]}f}}" for w in v.wheels)
        seats = ", ".join(f"{{{s[0]}f, {s[1]}f, {s[2]}f}}" for s in v.seats)
        muz = ", ".join(f"{{{m[0]}f, {m[1]}f, {m[2]}f, {m[3]}f}}" for m in v.muzzles)
        props = ", ".join(f"{{{q[0]}f, {q[1]}f, {q[2]}f}}" for q in v.props)
        lines.append(f'        put("{v.name}", new Geo({v.S}f, {v.ox}f, {java_vec(v.turret)}, {java_vec(v.gunp)}, {java_vec(v.rotor)}, '
                     f'{java_vec(v.tail)}, {java_vec(v.radar)},\n                new float[][]{{{wheels}}},\n'
                     f'                new float[][]{{{seats}}},\n                new float[][]{{{muz}}},\n                new float[][]{{{props}}}, {round(v.zmax, 2)}f));')
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, "w", encoding="utf-8") as f:
        f.write("""package fr.modeminecraft.entity.military;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * FICHIER GÉNÉRÉ par tools/generate_military_vehicles.py — ne pas modifier à la main.
 * Géométrie des véhicules militaires, en mètres (= blocs) dans le repère du véhicule : x vers l'avant, y vers le haut,
 * z vers la droite, origine au centre du véhicule au ras du sol. Sert au rendu (pivots des pièces mobiles, roues) et
 * au gameplay (places des passagers, bouches de tir : mount 0 = caisse, 1 = tourelle, 2 = canon).
 */
public final class MilGeometry {
    /**
     * scale : 1 pixel de modèle = scale / 16 bloc ; ox : abscisse (pixels) de l'origine du véhicule dans le modèle.
     * wheels : {x, y, z, rayon}. seats : {x, y, z}. muzzles : {x, y, z, mount}. props : moyeux d'hélices {x, y, z}.
     */
    public record Geo(float scale, float ox, Vec3 turret, Vec3 gun, Vec3 rotor, Vec3 tail, Vec3 radar, float[][] wheels, float[][] seats, float[][] muzzles,
                      float[][] props, float halfSpan) {}

    private static final Map<String, Geo> ALL = new HashMap<>();

    public static Geo get(String id) {
        return ALL.get(id);
    }

    private static void put(String id, Geo g) {
        ALL.put(id, g);
    }

    static {
""" + "\n".join(lines) + """
    }

    private MilGeometry() {}
}
""")
    print("géométrie :", JAVA)


if __name__ == "__main__":
    main()

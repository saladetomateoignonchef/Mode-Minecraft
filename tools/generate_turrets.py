#!/usr/bin/env python3
"""
Tourelles et mitrailleuses lourdes montables (cahier des charges "tourelles") : modèles 3D en pièces séparées.

Repère (pixels de modèle) : le pivot de l'arme (tourillons) est au point (8, 8, 8), le canon vers +X,
le haut vers +Y, le flanc GAUCHE du tireur vers -Z. Le sol est à y = 8 - hauteur du pivot.

Pièces (models/turret/<nom>_<pièce>.json) :
- base   : trépied / plate-forme, fixe (animation de déploiement : les pieds s'écartent)
- mount  : support qui tourne seulement à l'horizontale (pivot, siège de la Flak)
- body   : l'arme (boîtier, viseur WWII en voxels, poignées, bouclier), qui suit aussi la hauteur
- barrel / barrel_hot : canon normal / rougi par la surchauffe
- cover  : capot d'alimentation (s'ouvre pendant le rechargement)
- box    : caisse de munitions (flanc gauche)
- round  : une cartouche de la bande (dessinée N fois le long de la bande)
- mag0..mag3 : chargeurs latéraux de la Flakvierling (un par tranche de 20 obus)

Points utiles (assets/modeminecraft/turrets.json, lu par le jeu côté serveur et client) : œil du tireur
(hanche / visée), poignées, bouches des canons, bande, charnière du capot...
Utilisation : python3 tools/generate_turrets.py
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gun_designs import box  # noqa: E402
import voxel_bake as vb  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
MOD = "modeminecraft"


class T:
    def __init__(self, pivot_px):
        self.parts = {}
        self.points = {"pivot": pivot_px / 16.0}
        self.ground = 8 - pivot_px
        self._cur = "body"

    def part(self, name):
        self._cur = name
        self.parts.setdefault(name, [])
        return self

    def add(self, frm, to, c):
        self.parts.setdefault(self._cur, []).append(box(frm, to, c))

    def zb(self, x0, y0, x1, y1, hw, c, z=8.0):
        self.add([x0, y0, z - hw], [x1, y1, z + hw], c)

    def rod(self, p0, p1, s, c):
        """Barre en biais : petits cubes alignés (aspect voxel)."""
        d = [p1[i] - p0[i] for i in range(3)]
        n = max(1, int(math.dist(p0, p1) / (s * 0.7)))
        for k in range(n + 1):
            p = [p0[i] + d[i] * k / n for i in range(3)]
            self.add([p[0] - s / 2, p[1] - s / 2, p[2] - s / 2], [p[0] + s / 2, p[1] + s / 2, p[2] + s / 2], c)

    def ring_sight(self, x, cy, radii, t=0.42, cross=True, double=False, cz=8.0, c="k", depth=0.35):
        """Viseur circulaire WWII : anneaux concentriques pixelisés + croisillons (dans le plan YZ)."""
        R = max(radii)
        n = int(math.ceil((R + t) / t))
        rows = {}
        for iy in range(-n, n + 1):
            for iz in range(-n, n + 1):
                y, z = iy * t, iz * t
                r = math.hypot(y, z)
                on = any(abs(r - rr) <= t * 0.55 for rr in radii)
                if cross and r <= R:
                    if double:
                        on |= abs(abs(y) - t * 1.5) < t / 2 or abs(abs(z) - t * 1.5) < t / 2
                    else:
                        on |= abs(y) < t / 2 or abs(z) < t / 2
                if on:
                    rows.setdefault(iy, []).append(iz)
        for iy, zs in rows.items():
            zs.sort()
            start = prev = zs[0]
            for z in zs[1:] + [None]:
                if z is not None and z == prev + 1:
                    prev = z
                    continue
                self.add([x - depth / 2, cy + iy * t - t / 2, cz + start * t - t / 2],
                         [x + depth / 2, cy + iy * t + t / 2, cz + prev * t + t / 2], c)
                if z is not None:
                    start = prev = z

    def spade_grips(self, x, y0, y1, sep, rear_x, c="w"):
        """Poignées arrière doubles (spade grips) + pontets reliés au boîtier."""
        for s in (-1, 1):
            self.add([x - 0.55, y0, 8 + s * sep - 0.55], [x + 0.55, y1, 8 + s * sep + 0.55], c)
        self.add([x - 0.3, y1 - 0.2, 8 - sep], [rear_x, y1 + 0.3, 8 + sep], "k")
        self.add([x - 0.3, y0 - 0.3, 8 - sep], [rear_x, y0 + 0.2, 8 + sep], "k")
        self.add([x + 0.4, (y0 + y1) / 2 - 0.4, 7.4], [x + 1.1, (y0 + y1) / 2 + 0.4, 8.6], "G")  # détente à poussoir
        ym = (y0 + y1) / 2
        self.points["grips"] = [[x, ym, 8 - sep], [x, ym, 8 + sep]]

    def tripod(self, head, feet, c="m"):
        self.part("base")
        hx, hy, hz = head
        self.zb(hx - 1.5, hy - 1.2, hx + 1.5, hy + 0.3, 1.5, "k")
        for f in feet:
            self.rod([hx, hy - 0.6, hz], f, 0.85, c)
            self.add([f[0] - 0.9, f[1] - 0.45, f[2] - 0.9], [f[0] + 0.9, f[1], f[2] + 0.9], "k")  # patin
        self.points["base_head"] = [hx, hy, hz]

    def ammo_box(self, x0, x1, y0, y1, z0, z1, c="t"):
        self.part("box")
        self.add([x0, y0, z0], [x1, y1, z1], c)
        self.add([x0 - 0.05, y1 - 0.5, z0 - 0.05], [x1 + 0.05, y1, z1 + 0.05], "u")      # rebord
        self.add([x0 + 0.6, y1, z0 - 0.4], [x1 - 0.6, y1 + 2.4, z0 - 0.2], c)            # couvercle ouvert
        self.add([(x0 + x1) / 2 - 0.8, y0 + 1, z0 - 0.1], [(x0 + x1) / 2 + 0.8, y0 + 1.4, z0], "d")  # poignée
        self.part("body")

    def belt(self, a, b, sag, spacing=0.75, count=7):
        c = [(a[0] + b[0]) / 2, max(a[1], b[1]) + sag, (a[2] + b[2]) / 2]
        self.points["belt"] = {"from": a, "ctrl": c, "to": b, "spacing": spacing, "count": count}


def round_model(t, kind):
    t.part("round")
    if kind == "grenade":
        t.add([-1.1, -0.6, -0.6], [0.6, 0.6, 0.6], "g")
        t.add([0.6, -0.65, -0.65], [1.6, 0.65, 0.65], "u")
        t.add([-0.3, -0.75, -0.75], [0.2, 0.75, 0.75], "k")
    else:
        L = 1.3 if kind == "big" else 1.0
        t.add([-L, -0.26, -0.26], [L * 0.45, 0.26, 0.26], "g")   # douille laiton
        t.add([L * 0.45, -0.2, -0.2], [L, 0.2, 0.2], "G")        # balle
        t.add([-0.25, -0.34, -0.34], [0.15, 0.34, 0.34], "k")    # maillon
    t.part("body")


def finish(t):
    """Canon rougi = même géométrie, couleur rouge."""
    hot = []
    for el in t.parts.get("barrel", []):
        e = json.loads(json.dumps(el))
        e["__key"] = "r"
        hot.append(e)
    t.parts["barrel_hot"] = hot
    return t


# ----------------------------------------------------------------------------------- les modèles
def m2_browning(dshk=False):
    t = T(12)
    t.tripod((4, 0.5, 8), [(19, t.ground, 8), (-6, t.ground, -1), (-6, t.ground, 17)])
    t.part("mount")
    t.zb(3.2, 0.5, 4.8, 5.4, 0.8, "k")                       # pivot
    t.zb(2.4, 5.0, 5.6, 6.0, 1.7, "k")                       # berceau
    t.part("body")
    rc = "m" if dshk else "k"
    t.zb(-2, 6.2, 9, 10.2, 1.5, rc)                          # boîtier
    t.zb(-2.4, 6.6, -2, 9.8, 1.2, "k")                       # plaque arrière
    t.spade_grips(-3.6, 7.2, 10.0, 1.4, -2.0)
    if dshk:
        t.ring_sight(13.5, 13.0, [4.0, 2.4, 0.9], t=0.36)    # viseur antiaérien à anneaux
        t.zb(13.3, 8.85, 13.7, 9.2, 0.2, "k")
        t.zb(-1.0, 10.2, -0.6, 12.6, 0.2, "k")                # œilleton arrière
        t.zb(24.0, 8.9, 24.4, 13.0, 0.18, "k")                # guidon
        sight_y = 13.0
    else:
        # hausse rabattable haute avec anneau de visée
        t.zb(-0.6, 10.2, 0.2, 10.8, 0.9, "k")
        t.zb(-0.3, 10.8, -0.1, 12.0, 0.1, "k", z=7.05)
        t.zb(-0.3, 10.8, -0.1, 12.0, 0.1, "k", z=8.95)
        t.ring_sight(-0.2, 12.8, [0.85], t=0.18, cross=False, depth=0.2)
        sight_y = 12.8
        t.zb(8.5, 9.6, 9.0, sight_y, 0.2, "k")
    t.part("barrel")
    t.zb(9, 7.2, 13.5, 8.8, 0.95, "k")                       # manchon perforé
    for i in range(4):
        t.zb(9.4 + i * 1.0, 7.15, 9.9 + i * 1.0, 8.85, 1.0, "d")
    t.zb(13.5, 7.5, 26.5, 8.5, 0.5, "k")                     # canon
    if dshk:
        for i in range(5):
            t.zb(14 + i * 1.5, 7.1, 14.6 + i * 1.5, 8.9, 0.9, "k")   # ailettes
        t.zb(24.5, 7.1, 26.8, 8.9, 1.0, "m")                  # frein de bouche
    else:
        t.zb(25.0, 7.25, 26.8, 8.75, 0.75, "k")               # cache-flamme
    t.part("cover")
    t.zb(-1.6, 10.2, 8.4, 10.8, 1.4, "m")
    t.points["cover_hinge"] = [8.4, 10.5, 8]
    t.part("body")
    t.ammo_box(1.0, 6.2, 2.4, 7.6, 2.4, 6.1)
    t.belt([3.6, 7.6, 4.2], [3.6, 9.6, 6.5], 1.2, count=6)
    round_model(t, "big")
    t.points.update(hip_eye=[-14, 13.8, 8], ads_eye=[-3.0 if dshk else -9.0, sight_y, 8], muzzles=[[26.8, 8, 8]])
    return finish(t)


def mg42():
    t = T(13)
    g = t.ground
    t.tripod((4, 1, 8), [(17, g, 3), (17, g, 13), (-7, g, 8)])
    t.part("mount")
    t.zb(3.3, 1.0, 4.7, 5.6, 0.7, "k")
    t.zb(2.0, 5.2, 6.0, 6.6, 1.5, "k")
    t.part("body")
    t.zb(-3, 6.6, 8, 9.8, 1.2, "m")                           # boîtier
    t.zb(-6.5, 7.0, -3, 9.0, 0.9, "w")                        # crosse
    t.spade_grips(-4.4, 8.0, 10.8, 1.6, -3.0)
    # bouclier frontal léger (sous la ligne de visée)
    t.add([8.4, 2.5, 3.0], [8.8, 9.0, 13.0], "u")
    t.add([8.4, 9.0, 3.6], [8.8, 9.5, 12.4], "u")
    # Kreiskorn : grand viseur antiaérien en toile d'araignée, aligné avec le guidon
    t.ring_sight(13.0, 15.2, [6.0, 4.5, 3.0, 1.5], t=0.34)
    t.zb(12.8, 9.1, 13.2, 9.4, 0.25, "k")
    # Mire arrière en anneau, dans l'axe du Kreiskorn : vue depuis l'œil en visée, elle se superpose
    # exactement à l'anneau extérieur (même taille apparente) et les deux ne forment plus qu'un seul viseur.
    eye_x, ring_x, rear_x, outer = -6.0, 13.0, 0.8, 6.0
    rear_r = outer * (rear_x - eye_x) / (ring_x - eye_x)
    rear_t = 0.55 * (rear_x - eye_x) / (ring_x - eye_x)   # un peu plus épais : il recouvre l'anneau avant
    t.zb(0.6, 9.8, 1.0, 15.2 - rear_r - rear_t / 2, 0.15, "k")
    t.ring_sight(rear_x, 15.2, [rear_r], t=rear_t, cross=False, depth=0.25)
    t.zb(20.0, 8.6, 20.4, 15.2, 0.16, "k")                    # guidon au bout du canon
    t.part("barrel")
    t.zb(8, 6.9, 18.5, 9.1, 1.0, "k")                          # manchon perforé
    for i in range(6):
        t.zb(8.6 + i * 1.6, 7.4, 9.4 + i * 1.6, 8.6, 1.05, "d")
    t.zb(18.5, 7.55, 21.0, 8.45, 0.45, "k")
    t.zb(20.6, 7.3, 21.8, 8.7, 0.7, "m")                       # amplificateur de recul
    t.part("cover")
    t.zb(-2.6, 9.8, 7.6, 10.3, 1.1, "G")
    t.points["cover_hinge"] = [7.6, 10.0, 8]
    t.part("body")
    t.ammo_box(0.2, 6.0, 2.6, 7.6, 2.0, 5.9)
    t.belt([3.1, 7.6, 3.8], [3.1, 9.4, 6.8], 1.3, spacing=0.6, count=8)
    round_model(t, "small")
    t.points.update(hip_eye=[-14, 15.8, 8], ads_eye=[-6.0, 15.2, 8], muzzles=[[21.8, 8, 8]])
    return finish(t)


def vickers():
    t = T(12)
    g = t.ground
    t.tripod((4, 0.5, 8), [(16, g, 8), (-7, g, 0), (-7, g, 16)])
    t.part("mount")
    t.zb(3.2, 0.5, 4.8, 5.4, 0.8, "k")
    t.zb(2.2, 5.0, 5.8, 6.0, 1.6, "k")
    t.part("body")
    t.zb(-3, 6.0, 6, 10.4, 1.6, "T")                           # boîtier (bronze/olive)
    t.spade_grips(-4.4, 8.0, 10.6, 1.5, -3.0)
    t.zb(-2.3, 10.4, -1.9, 11.7, 0.6, "k")                     # hausse à cran de mire
    t.zb(-2.3, 11.7, -1.9, 12.3, 0.15, "k", z=7.5)
    t.zb(-2.3, 11.7, -1.9, 12.3, 0.15, "k", z=8.5)
    t.zb(18.6, 10.2, 19.0, 12.0, 0.15, "k")                    # guidon à lame
    # grand bouclier d'acier, avec une fente de visée
    t.add([7.2, 1.0, 1.0], [7.6, 10.8, 15.0], "u")
    t.add([7.2, 10.8, 1.0], [7.6, 13.6, 6.4], "u")
    t.add([7.2, 10.8, 9.6], [7.6, 13.6, 15.0], "u")
    t.add([7.2, 13.6, 1.0], [7.6, 14.4, 15.0], "u")
    t.part("barrel")
    t.zb(6, 6.2, 19, 10.2, 2.0, "m")                           # manchon de refroidissement à eau
    for i in range(6):
        t.zb(6.8 + i * 2.0, 6.1, 7.4 + i * 2.0, 10.3, 2.05, "k")   # nervures
    t.zb(18.0, 10.2, 18.6, 11.2, 0.4, "k")                     # tuyau de vapeur
    t.zb(19, 7.7, 21.5, 8.3, 0.3, "k")
    t.zb(20.8, 7.3, 22.0, 8.7, 0.7, "k")                       # cône de bouche
    t.part("cover")
    t.zb(-2.6, 10.4, 5.4, 11.0, 1.4, "T")
    t.points["cover_hinge"] = [5.4, 10.7, 8]
    t.part("body")
    t.ammo_box(-1.0, 5.0, 2.2, 7.4, 1.8, 5.8, c="t")
    t.belt([2.0, 7.4, 3.8], [2.0, 9.6, 6.4], 1.2, spacing=0.65, count=7)
    round_model(t, "small")
    t.points.update(hip_eye=[-14, 13.8, 8], ads_eye=[-10.0, 12.0, 8], muzzles=[[22.0, 8, 8]], steam=[18.3, 11.3, 8])
    return finish(t)


def maxim():
    """Maxim M1910 sur affût Sokolov : petites roues en acier, flèche en U, bouclier, manchon à eau à bouchon de remplissage."""
    t = T(12)
    g = t.ground
    t.part("base")
    for z0 in (1.6, 13.6):                                       # roues (disques en marches)
        t.add([2.2, g, z0], [5.8, g + 7.0, z0 + 0.8], "k")
        t.add([1.2, g + 1.0, z0], [6.8, g + 6.0, z0 + 0.8], "k")
        t.add([3.4, g + 3.0, z0 - 0.2], [4.6, g + 4.0, z0 + 1.0], "m")   # moyeu
    t.add([3.5, g + 3.1, 2.4], [4.5, g + 3.9, 13.6], "m")        # essieu
    t.add([-10.0, g, 6.4], [3.6, g + 1.0, 7.2], "u")             # flèche en U
    t.add([-10.0, g, 8.8], [3.6, g + 1.0, 9.6], "u")
    t.add([-11.0, g, 6.0], [-10.0, g + 1.4, 10.0], "u")
    t.add([-11.6, g + 1.4, 7.4], [-10.6, g + 3.2, 8.6], "k")     # anneau de traction
    t.part("mount")
    t.zb(3.0, g + 3.9, 5.0, 5.2, 0.9, "u")
    t.zb(2.4, 5.0, 5.6, 6.0, 1.5, "u")
    t.part("body")
    t.zb(-3, 6.0, 6, 10.2, 1.6, "t")                             # boîtier vert
    t.spade_grips(-4.4, 7.8, 10.4, 1.5, -3.0)
    t.zb(-2.3, 10.2, -1.9, 11.6, 0.6, "k")                       # hausse
    t.zb(-2.3, 11.6, -1.9, 12.2, 0.15, "k", z=7.5)
    t.zb(-2.3, 11.6, -1.9, 12.2, 0.15, "k", z=8.5)
    t.zb(18.2, 10.0, 18.6, 11.8, 0.15, "k")                      # guidon
    # bouclier plus petit, légèrement replié sur les côtés
    t.add([7.2, 2.0, 2.4], [7.6, 10.6, 13.6], "u")
    t.add([7.2, 10.6, 2.4], [7.6, 12.8, 6.6], "u")
    t.add([7.2, 10.6, 9.4], [7.6, 12.8, 13.6], "u")
    t.add([7.2, 12.8, 2.4], [7.6, 13.4, 13.6], "u")
    t.add([6.4, 2.0, 1.6], [7.2, 12.4, 2.4], "u")
    t.add([6.4, 2.0, 13.6], [7.2, 12.4, 14.4], "u")
    t.part("barrel")
    t.zb(6, 6.0, 18.4, 10.0, 2.1, "t")                           # manchon à eau (lisse sur la Maxim)
    t.zb(9.0, 10.0, 10.4, 11.0, 0.7, "k")                        # bouchon de remplissage
    t.zb(17.8, 10.0, 18.4, 11.0, 0.4, "k")                       # tuyau de vapeur
    t.zb(18.4, 7.7, 21.0, 8.3, 0.3, "k")
    t.zb(20.2, 7.3, 21.4, 8.7, 0.7, "k")
    t.part("cover")
    t.zb(-2.6, 10.2, 5.4, 10.8, 1.4, "t")
    t.points["cover_hinge"] = [5.4, 10.5, 8]
    t.part("body")
    t.ammo_box(-1.0, 5.0, 2.2, 7.2, 1.8, 5.8, c="u")
    t.belt([2.0, 7.2, 3.8], [2.0, 9.4, 6.4], 1.2, spacing=0.65, count=7)
    round_model(t, "small")
    t.points.update(hip_eye=[-14, 13.6, 8], ads_eye=[-10.0, 11.8, 8], muzzles=[[21.4, 8, 8]], steam=[18.1, 11.1, 8])
    return finish(t)


def flakvierling():
    t = T(20)
    g = t.ground
    t.part("base")
    t.zb(-6, g, 22, g + 1.4, 1.2, "k")                         # plate-forme en croix
    t.add([6.8, g, -6], [9.2, g + 1.4, 22], "k")
    for p in ((-6, 8), (22, 8), (8, -6), (8, 22)):
        t.add([p[0] - 1.4, g - 0.01, p[1] - 1.4], [p[0] + 1.4, g + 0.6, p[1] + 1.4], "m")   # vérins
    t.points["base_head"] = [8, g + 1.4, 8]
    t.part("mount")
    for i in range(8):                                           # plateau tournant (approché)
        a = i * math.pi / 8
        dx, dz = math.cos(a) * 8.5, math.sin(a) * 8.5
        t.add([8 - abs(dx) - 0.6, g + 1.4, 8 - abs(dz) - 0.6], [8 + abs(dx) + 0.6, g + 2.8, 8 + abs(dz) + 0.6], "m")
    t.add([1.0, g + 2.8, 0.6], [12.0, 8.0, 1.6], "m")           # flasques
    t.add([1.0, g + 2.8, 14.4], [12.0, 8.0, 15.4], "m")
    t.add([-15.5, 1.6, 5.6], [-11.0, 2.6, 10.4], "w")           # siège
    t.add([-16.0, 2.6, 5.8], [-15.2, 7.4, 10.2], "w")           # dossier
    t.rod([-13, 1.6, 8], [-5, g + 2.8, 8], 1.0, "k")            # bras du siège
    t.add([-9, -3.4, 4.6], [-6.5, -2.8, 11.4], "k")             # repose-pieds
    t.part("body")
    t.zb(-4, 2.6, 7, 13.4, 5.6, "m")                            # berceau
    for z in (4.4, 11.6):                                       # poignées de tir, vers l'arrière
        t.rod([-4, 13.2, z], [-6, 15.2, z], 0.7, "k")
        t.add([-6.6, 14.8, z - 0.6], [-5.4, 17.2, z + 0.6], "w")
    t.points["grips"] = [[-6.0, 16.0, 4.4], [-6.0, 16.0, 11.6]]
    t.ring_sight(4.0, 19.0, [7.0, 5.0, 3.0, 1.2], t=0.42, double=True)   # Flakvisier gradué, croisillons doubles
    t.zb(3.8, 13.4, 4.2, 12.0 + 0.1, 0.3, "k")
    t.zb(23.8, 10.4, 24.2, 19.0, 0.18, "k")                     # guidon (mât entre les canons hauts)
    t.part("barrel")
    muzzles = []
    for (dy, dz) in ((2.6, -2.6), (-2.6, 2.6), (2.6, 2.6), (-2.6, -2.6)):   # ordre de tir
        y, z = 8 + dy, 8 + dz
        t.zb(7, y - 0.75, 12, y + 0.75, 0.75, "k", z=z)
        t.zb(12, y - 0.45, 29, y + 0.45, 0.45, "k", z=z)
        t.zb(28, y - 0.7, 30, y + 0.7, 0.7, "m", z=z)
        muzzles.append([30, y, z])
    for i in range(4):                                           # chargeurs latéraux de 20 obus
        t.part(f"mag{i}")
        z0 = 0.4 if i in (0, 3) else 14.6
        y0 = 8.6 if i in (0, 2) else 3.4
        side = -1 if i in (0, 3) else 1
        t.add([-1.2, y0, z0 - (1.0 if side < 0 else 0)], [3.4, y0 + 4.2, z0 + (0 if side < 0 else 1.0)], "T")
    t.part("body")
    round_model(t, "big")
    t.points.update(hip_eye=[-14, 19.6, 8], ads_eye=[-11.0, 19.0, 8], muzzles=muzzles)
    return finish(t)


def mk19():
    t = T(13)
    g = t.ground
    t.tripod((4, 1, 8), [(17, g, 8), (-6, g, -1), (-6, g, 17)])
    t.part("mount")
    t.zb(3.2, 1.0, 4.8, 5.0, 0.8, "k")
    t.zb(1.6, 4.6, 6.4, 5.6, 2.0, "k")
    t.part("body")
    t.zb(-3, 5.5, 8, 10.5, 2.0, "u")                            # boîtier
    t.spade_grips(-4.4, 7.6, 10.4, 1.7, -3.0)
    # hausse graduée en échelle (tir en cloche)
    t.zb(-0.15, 10.5, 0.15, 15.0, 0.08, "k", z=6.9)
    t.zb(-0.15, 10.5, 0.15, 15.0, 0.08, "k", z=9.1)
    for i in range(9):
        y = 11.0 + i * 0.5
        if abs(y - 12.5) > 0.6:
            t.zb(-0.1, y, 0.1, y + 0.08, 1.1, "l" if i % 2 else "k")   # graduations
    t.ring_sight(0.0, 12.5, [0.6], t=0.14, cross=False, depth=0.2)
    t.zb(7.4, 10.5, 7.8, 12.5, 0.15, "k")                       # guidon
    t.part("barrel")
    t.zb(8, 6.8, 14, 9.2, 1.2, "k")
    for i in range(3):
        t.zb(8.6 + i * 1.8, 6.75, 9.6 + i * 1.8, 9.25, 1.25, "d")
    t.zb(14, 7.2, 20, 8.8, 0.8, "k")
    t.part("cover")
    t.zb(-2.6, 10.5, 7.2, 11.1, 1.8, "u")
    t.points["cover_hinge"] = [7.2, 10.8, 8]
    t.part("body")
    t.ammo_box(-0.5, 7.5, 1.0, 7.0, 0.4, 5.2, c="t")
    t.belt([3.5, 7.0, 2.8], [3.5, 9.6, 6.0], 1.4, spacing=1.4, count=4)
    round_model(t, "grenade")
    t.points.update(hip_eye=[-14, 13.6, 8], ads_eye=[-10.0, 12.5, 8], muzzles=[[20.0, 8, 8]])
    return finish(t)


TURRETS = {
    "maxim_m1910": maxim,
    "m2_browning": m2_browning,
    "dshk": lambda: m2_browning(dshk=True),
    "mg42": mg42,
    "vickers": vickers,
    "flakvierling": flakvierling,
    "mk19": mk19,
}

ITEM_DISPLAY = {
    "gui": {"rotation": [25, -135, 0], "translation": [0, 0, 0], "scale": [0.42, 0.42, 0.42]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]},
    "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.45, 0.45, 0.45]},
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 2, 2], "scale": [0.3, 0.3, 0.3]},
    "firstperson_righthand": {"rotation": [0, 100, 0], "translation": [2, 2, 0], "scale": [0.3, 0.3, 0.3]},
}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, separators=(",", ":"))


def main():
    from generate_gun_models import painter
    paint = painter("default")
    points = {}
    for name, fn in TURRETS.items():
        t = fn()
        for part, els in t.parts.items():
            for e in els:
                for v in e["from"] + e["to"]:
                    assert -16 <= v <= 32, f"{name}/{part} hors limites : {e['from']} {e['to']}"
        # textures détaillées : une texture (atlas) par tourelle pour toutes ses pièces ;
        # densité réduite si la tourelle est très grande (atlas plafonné à 512 pixels)
        for d in (3, 2):
            baked = vb.bake(t.parts, d=d, skip_chamfer=("s", "q", "d"))
            if baked.size <= 512:
                break
        atlas = vb.paint(baked, paint)
        atlas.save(os.path.join(ASSETS, f"textures/item/turret/{name}.png"))
        tex = {"p": f"{MOD}:item/turret/{name}", "particle": f"{MOD}:item/turret/{name}"}
        write(os.path.join(ASSETS, f"models/turret/{name}_base_tex.json"), {"textures": tex})
        for part in t.parts:
            write(os.path.join(ASSETS, f"models/turret/{name}_{part}.json"),
                  {"parent": f"{MOD}:turret/{name}_base_tex", "elements": vb.elements(baked, part)})
        # Objet : la tourelle complète, en 3D
        whole = [e for p in ("base", "mount", "body", "barrel", "cover", "box", "mag0", "mag1") if p in t.parts
                 for e in vb.elements(baked, p)]
        write(os.path.join(ASSETS, f"models/item/{name}.json"), {"textures": tex, "display": ITEM_DISPLAY, "elements": whole})
        points[name] = t.points
        points[name]["parts"] = sorted(t.parts.keys())
        print(f"{name} : atlas {baked.size}px (densité {d})")
    with open(os.path.join(ASSETS, "turrets.json"), "w", encoding="utf-8") as f:
        json.dump(points, f, indent=1)
    print(f"{len(TURRETS)} tourelles : " + ", ".join(f"{n} ({sum(len(v) for v in fn().parts.values())} pavés)" for n, fn in TURRETS.items()))


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Casques militaires 3D (cahier des charges "casques") + nouveaux matériaux (titane, acier, kevlar).

Génère :
- textures/item/helmet_palette.png : palette 64x64 (cases 8x8) aux teintes militaires mates, légèrement usées,
  avec les camouflages (Woodland, MultiCam) dessinés dans leur case ;
- models/item/<casque>.json : vrai modèle 3D voxel porté sur la tête (rendu "head" de Minecraft),
  + <casque>_module.json pour la visière abaissée (Altyn) et les jumelles de vision nocturne abaissées (FAST) ;
- textures/misc/helmet_*.png : contours d'écran (heaume, fente de visière, jumelles, visière numérique) ;
- les textures des minerais et matériaux (titane, acier, kevlar...).

Repère des casques (pixels de modèle) : la tête occupe le cube [1,6 ; 14,4] (sur la tête, le modèle est réduit
à 0,625), le visage est du côté -Z, le haut vers +Y. Les yeux sont vers y = 6,4 à 8.
Utilisation : python3 tools/generate_helmets.py
"""
import json
import math
import os
import sys
import random
import zlib

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import voxel_bake as vb  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
MOD = "modeminecraft"

# ------------------------------------------------------------------ palette
COLORS = {
    "iron_ox": "#6E6A62", "bronze": "#8C6A3A", "bronze_d": "#6E5230", "steel_d": "#3A3C40", "steel": "#5B5F66",
    "brass": "#B08D3C", "leather": "#6B4A2B", "leather_d": "#4A321E", "canvas": "#6B6447", "horizon": "#5C6B73",
    "horizon_d": "#4A5860", "khaki": "#5A5E4B", "khaki_d": "#484B3C", "moss": "#3B4433", "olive": "#4B5335",
    "olive_d": "#3C432A", "feldgrau": "#4D564D", "feldgrau_d": "#3E463E", "soviet": "#3E4E38", "soviet_d": "#323F2D",
    "rivet": "#8A8E92", "net": "#2F3324", "nato": "#4A5A3A", "titan_g": "#3B4737", "titan_g_d": "#2F392C",
    "glass": "#7FA8B8", "visor_d": "#1E2226", "coyote": "#9E8B6C", "coyote_d": "#85745A", "ranger": "#485143",
    "ranger_d": "#3A4236", "black": "#222426", "rail": "#2A2C2E", "velcro": "#5C6352", "jugg": "#2A3028",
    "lens": "#2E6B3A", "nvg": "#1A1C1E", "red": "#C8223B", "cyan": "#3FC8D8", "padding": "#3A3A3A",
    "comtac": "#1F2124", "white": "#D8D8D0", "dark_glass": "#2A3A44",
    "navy": "#1E2A44", "navy_d": "#141C30", "scale_ti": "#7E8A96", "brass_case": "#C8A040",
    "leather_red": "#7A3E26", "lace": "#B8A880",
}
CAMO = {
    "woodland": ["#4B5A2A", "#5A4630", "#1E2416", "#6B7D3A"],
    "multicam": ["#9E8B6C", "#7A6B4E", "#5A6B45", "#C2B08A", "#4A3C2A"],
}
KEYS = list(COLORS) + list(CAMO)
assert len(KEYS) <= 64
TRANSLUCENT = {"glass": 150, "dark_glass": 200}


def hexc(h, a=255):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def palette_image():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rng = random.Random(7)
    for k, key in enumerate(KEYS):
        cx, cy = (k % 8) * 8, (k // 8) * 8
        for y in range(8):
            for x in range(8):
                if key in CAMO:
                    r2 = random.Random(zlib.crc32(repr((key, x // 2, y // 2)).encode()))
                    c = hexc(r2.choice(CAMO[key]))
                else:
                    c = hexc(COLORS[key], TRANSLUCENT.get(key, 255))
                # usure mate : légères variations, sans reflets
                f = 1 + rng.uniform(-0.07, 0.05)
                img.putpixel((cx + x, cy + y), tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],))
    return img


def uv(key):
    k = KEYS.index(key)
    cx, cy = (k % 8) * 2, (k // 8) * 2
    return [cx + 0.15, cy + 0.15, cx + 1.85, cy + 1.85]


def box(frm, to, key, rot=None):
    u = uv(key)
    el = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to],
          "faces": {f: {"uv": u, "texture": "#p"} for f in ("north", "east", "south", "west", "up", "down")}, "__key": key}
    if rot:
        axis, angle, origin = rot
        el["rotation"] = {"angle": angle, "axis": axis, "origin": origin}
    return el


# ------------------------------------------------------------------ textures détaillées (voir voxel_bake.py)
MATERIAL = {}
for _k in ("glass", "dark_glass", "lens", "visor_d"):
    MATERIAL[_k] = "glass"
for _k in ("iron_ox", "bronze", "bronze_d", "steel", "steel_d", "brass", "rivet", "scale_ti", "rail", "brass_case"):
    MATERIAL[_k] = "metal"
for _k in ("canvas", "horizon", "horizon_d", "khaki", "khaki_d", "moss", "net", "nato", "coyote", "coyote_d", "ranger",
           "ranger_d", "velcro", "navy", "navy_d", "woodland", "multicam", "lace", "padding"):
    MATERIAL[_k] = "fabric"
for _k in ("leather", "leather_d", "leather_red"):
    MATERIAL[_k] = "leather"
for _k in ("black", "comtac", "nvg", "jugg"):
    MATERIAL[_k] = "polymer"
for _k in ("red", "cyan"):
    MATERIAL[_k] = "flat"
PAINTER = vb.Painter({**COLORS, **CAMO}, lambda k: MATERIAL.get(k, "paint"), dict(TRANSLUCENT), seed="armor")
SKIP = ("glass", "dark_glass", "lens", "visor_d", "red", "cyan")


def baked_models(name, groups):
    """Cuît un ensemble de pavés (groupes) dans une texture détaillée propre à l'objet ; renvoie (Baked, textures)."""
    baked = vb.bake(groups, skip_chamfer=SKIP)
    vb.paint(baked, PAINTER).save(os.path.join(ASSETS, "textures/item/armor3d", f"{name}.png"))
    t = f"{MOD}:item/armor3d/{name}"
    return baked, {"p": t, "particle": t}


class H:
    def __init__(self):
        self.els = []

    def add(self, frm, to, key, rot=None):
        self.els.append(box(frm, to, key, rot))
        return self

    def dome(self, y0, y1, rx, rz, n, key, cz=8.0, cx=8.0):
        """Calotte en couches : presque droite sur les côtés (elle couvre la tête cubique), arrondie en haut."""
        for i in range(n):
            f = (i + 0.5) / n
            s = 1 - 0.62 * f ** 4
            a = y0 + (y1 - y0) * i / n
            b = y0 + (y1 - y0) * (i + 1) / n
            self.add([cx - rx * s, a, cz - rz * s], [cx + rx * s, b, cz + rz * s], key)
        return self

    def mirror(self, frm, to, key):
        """Pièce + son symétrique (gauche / droite)."""
        self.add(frm, to, key)
        self.add([16 - to[0], frm[1], frm[2]], [16 - frm[0], to[1], to[2]], key)
        return self

    def strap(self, key="leather"):
        """Jugulaire : le long des joues puis sous le menton."""
        self.mirror([0.9, 2.0, 6.8], [1.3, 9.6, 7.8], key)
        self.add([1.3, 1.1, 6.8], [14.7, 1.5, 7.8], key)
        return self


# ------------------------------------------------------------------ les 12 casques
def galea():
    h = H().dome(10.6, 17.2, 7.4, 7.4, 7, "bronze")
    h.add([0.8, 10.0, 0.8], [15.2, 10.8, 15.2], "iron_ox")                 # bandeau
    h.add([2.0, 10.0, -0.2], [14.0, 10.6, 0.8], "iron_ox")                 # visière frontale
    h.mirror([0.5, 3.6, 1.2], [1.3, 10.2, 8.2], "bronze")                 # protège-joues
    h.mirror([0.3, 9.2, 3.6], [0.6, 9.8, 4.4], "brass")                   # charnières
    h.add([1.3, 6.4, 15.0], [14.7, 10.2, 15.8], "bronze")                 # couvre-nuque
    h.add([0.9, 5.8, 15.6], [15.1, 6.5, 17.0], "bronze_d")
    h.add([7.4, 17.1, 7.4], [8.6, 18.0, 8.6], "brass")                    # bouton sommital
    return h


def great_helm():
    h = H()
    k = "steel_d"
    h.add([1.0, 15.2, 1.0], [15.0, 16.0, 15.0], k).add([1.0, 1.0, 14.4], [15.0, 15.2, 15.0], k)
    h.add([1.0, 1.0, 1.0], [1.6, 15.2, 14.4], k).add([14.4, 1.0, 1.0], [15.0, 15.2, 14.4], k)
    # face avant percée d'une fente oculaire en croix
    z0, z1 = 1.0, 1.6
    h.add([1.0, 1.0, z0], [15.0, 3.0, z1], k)
    h.add([1.0, 3.0, z0], [7.4, 6.6, z1], k).add([8.6, 3.0, z0], [15.0, 6.6, z1], k)
    h.add([1.0, 6.6, z0], [2.6, 7.6, z1], k).add([13.4, 6.6, z0], [15.0, 7.6, z1], k)
    h.add([1.0, 7.6, z0], [7.4, 10.4, z1], k).add([8.6, 7.6, z0], [15.0, 10.4, z1], k)
    h.add([1.0, 10.4, z0], [15.0, 15.2, z1], k)
    h.add([1.6, 3.0, 1.6], [14.4, 10.4, 1.7], "visor_d")                  # intérieur sombre derrière les fentes
    # renforts en croix (laiton) et trous de respiration
    h.add([1.0, 10.4, 0.7], [15.0, 11.2, 1.0], "brass")
    h.add([6.8, 1.0, 0.7], [7.4, 6.6, 1.0], "brass").add([8.6, 1.0, 0.7], [9.2, 6.6, 1.0], "brass")
    for x in (10.6, 11.8, 13.0):
        for y in (3.6, 4.8):
            h.add([x, y, 0.95], [x + 0.6, y + 0.6, 1.0], "visor_d")
    return h


def adrian():
    h = H().dome(10.4, 16.6, 7.2, 7.4, 7, "horizon")
    h.add([7.4, 15.6, 2.6], [8.6, 17.6, 13.4], "horizon_d")                # cimier
    h.add([2.0, 9.9, -0.8], [14.0, 10.5, 1.6], "horizon")                  # visière avant
    h.add([2.0, 9.6, 14.4], [14.0, 10.2, 16.6], "horizon")                 # visière arrière
    h.add([0.6, 9.9, 1.4], [15.4, 10.5, 14.6], "horizon_d")                # bord
    h.add([7.2, 11.4, 0.4], [8.8, 13.2, 0.8], "brass")                     # insigne
    return h.strap()


def brodie():
    h = H()
    h.add([-1.6, 10.8, -1.6], [17.6, 11.4, 17.6], "khaki")                # large bord en assiette
    h.add([-1.8, 10.5, -1.8], [17.8, 10.8, 17.8], "khaki_d")
    h.dome(11.4, 16.4, 7.0, 7.0, 5, "khaki")
    return h.strap()


def m1():
    h = H().dome(9.4, 17.0, 7.6, 7.6, 7, "olive")
    h.add([0.3, 9.0, 0.3], [15.7, 9.6, 15.7], "olive_d")                  # rebord
    # filet de camouflage en relief
    for y in (11.0, 13.0, 15.0):
        s = 1 - 0.62 * ((y - 9.4) / 7.6) ** 4
        r = 7.6 * s + 0.15
        h.add([8 - r, y, 8 - r], [8 + r, y + 0.3, 8 + r], "net")
    for z in (3.0, 8.0, 13.0):
        h.mirror([0.25, 9.6, z - 0.15], [0.45, 14.8, z + 0.15], "net")
    for x in (4.0, 8.0, 12.0):
        h.add([x - 0.15, 9.6, 0.25], [x + 0.15, 14.8, 0.45], "net").add([x - 0.15, 9.6, 15.55], [x + 0.15, 14.8, 15.75], "net")
    return h.strap("canvas")


def stahlhelm():
    h = H().dome(10.0, 17.0, 7.4, 7.6, 7, "feldgrau")
    h.mirror([0.2, 5.2, 3.0], [1.0, 10.0, 15.0], "feldgrau")              # jupe évasée
    h.mirror([-0.4, 4.6, 4.0], [0.4, 5.6, 15.6], "feldgrau_d")
    h.add([1.0, 5.2, 15.0], [15.0, 10.0, 15.8], "feldgrau")
    h.add([0.4, 4.6, 15.6], [15.6, 5.6, 16.4], "feldgrau_d")
    h.add([1.2, 9.6, -0.2], [14.8, 10.4, 0.8], "feldgrau")                # avancée frontale
    h.mirror([0.0, 11.8, 7.2], [0.7, 12.6, 8.0], "steel")                 # aérateurs
    return h.strap()


def ssh40():
    h = H().dome(9.8, 17.0, 7.6, 7.6, 7, "soviet")
    h.add([0.2, 9.4, 0.2], [15.8, 10.0, 15.8], "soviet_d")
    for z in (4.0, 8.0, 12.0):                                              # rivets en relief
        h.mirror([-0.1, 11.0, z - 0.3], [0.5, 11.6, z + 0.3], "rivet")
    h.add([7.7, 11.0, -0.1], [8.3, 11.6, 0.5], "rivet")
    return h.strap()


def pasgt():
    h = H().dome(10.0, 17.0, 7.4, 7.6, 7, "woodland")
    h.mirror([0.2, 5.4, 3.0], [1.0, 10.0, 15.0], "woodland")
    h.add([1.0, 5.4, 15.0], [15.0, 10.0, 15.8], "woodland")
    h.add([1.8, 9.6, -0.8], [14.2, 10.2, 1.2], "woodland")                # légère visière frontale
    s = 1 - 0.62 * ((11.6 - 10.0) / 7.0) ** 4
    h.add([8 - 7.4 * s - 0.2, 11.6, 8 - 7.6 * s - 0.2], [8 + 7.4 * s + 0.2, 12.3, 8 + 7.6 * s + 0.2], "olive_d")  # bande élastique
    return h.strap("canvas")


def altyn(down):
    h = H().dome(8.6, 17.6, 7.8, 7.8, 8, "titan_g")
    h.mirror([0.2, 4.0, 3.0], [1.0, 9.0, 15.0], "titan_g")
    h.add([1.0, 4.0, 15.0], [15.0, 9.0, 15.8], "titan_g")
    h.mirror([-0.2, 10.6, 3.6], [0.4, 11.6, 4.6], "brass")                # charnières de la visière
    h.mirror([0.0, 10.7, -1.0], [0.6, 11.5, 4.0], "titan_g_d")             # bras de la visière
    vk = "titan_g_d"
    if down:
        z0, z1 = -1.4, -0.4
        h.add([1.0, 3.0, z0], [15.0, 7.2, z1], vk).add([1.0, 9.6, z0], [15.0, 12.6, z1], vk)
        h.add([1.0, 7.2, z0], [4.4, 9.6, z1], vk).add([11.6, 7.2, z0], [15.0, 9.6, z1], vk)
        h.add([4.4, 7.2, -1.2], [11.6, 9.6, -0.6], "glass")               # fenêtre en verre blindé
    else:
        # visière relevée sur le front
        r = ("x", -22.5, [8, 11.2, -0.9])
        z0, z1 = -1.4, -0.4
        dy = 8.6
        h.add([1.0, 3.0 + dy, z0], [15.0, 7.2 + dy, z1], vk, r).add([1.0, 9.6 + dy, z0], [15.0, 12.6 + dy, z1], vk, r)
        h.add([1.0, 7.2 + dy, z0], [4.4, 9.6 + dy, z1], vk, r).add([11.6, 7.2 + dy, z0], [15.0, 9.6 + dy, z1], vk, r)
        h.add([4.4, 7.2 + dy, -1.2], [11.6, 9.6 + dy, -0.6], "glass", r)
    return h


def mich():
    h = H().dome(9.8, 17.2, 7.4, 7.4, 7, "coyote")
    h.mirror([0.6, 7.6, 3.0], [1.4, 10.0, 14.4], "coyote")
    h.add([1.4, 6.4, 14.6], [14.6, 10.0, 15.4], "coyote")
    h.add([1.4, 9.4, 0.6], [14.6, 10.0, 1.4], "coyote_d")                  # bord avant (pas de visière)
    for s in (0, 1):                                                        # rails Picatinny latéraux
        x0, x1 = (0.0, 0.6) if s == 0 else (15.4, 16.0)
        h.add([x0, 10.4, 4.0], [x1, 11.4, 12.0], "rail")
        for i in range(6):
            z = 4.4 + i * 1.3
            xa, xb = (x0 - 0.2, x0) if s == 0 else (x1, x1 + 0.2)
            h.add([xa, 10.5, z], [xb, 11.3, z + 0.6], "rail")
    h.add([6.6, 11.6, 0.0], [9.4, 13.8, 1.2], "rail")                      # support frontal
    return h.strap("canvas")


def fast(down):
    h = H().dome(10.6, 17.2, 7.4, 7.6, 7, "ranger")
    h.mirror([0.6, 9.6, 3.0], [1.4, 11.0, 14.0], "ranger")                 # coupe haute : oreilles dégagées
    h.add([1.4, 7.6, 14.6], [14.6, 10.8, 15.4], "ranger")
    h.mirror([-0.1, 11.2, 4.0], [0.5, 12.0, 12.0], "rail")                 # rails ARC
    h.add([4.6, 17.15, 5.0], [11.4, 17.45, 11.0], "velcro")                # velcro
    # casque antibruit ComTac
    h.mirror([-1.2, 4.6, 5.4], [0.6, 9.4, 10.6], "comtac")
    h.mirror([0.6, 5.0, 5.8], [1.4, 9.0, 10.2], "padding")
    h.add([-0.9, 4.8, 2.0], [-0.5, 5.2, 5.4], "comtac").add([-1.0, 4.6, 1.4], [-0.2, 5.4, 2.2], "padding")  # micro
    h.add([5.6, 11.6, 15.4], [10.4, 14.8, 16.6], "nvg")                    # batterie (contrepoids)
    h.add([6.6, 12.0, -0.1], [9.4, 14.4, 1.2], "rail")                      # support NVG
    # Jumelles de vision nocturne quadritubes GPNVG-18
    if down:
        h.add([7.2, 9.6, -1.6], [8.8, 12.4, 0.2], "nvg")
        h.add([3.0, 7.6, -2.8], [13.0, 9.8, -1.0], "nvg")
        for x0 in (5.4, 8.6):
            h.add([x0, 7.2, -6.6], [x0 + 2.0, 9.2, -2.8], "nvg").add([x0 + 0.3, 7.5, -6.8], [x0 + 1.7, 8.9, -6.6], "lens")
        for x0, ang in ((2.0, 22.5), (12.0, -22.5)):
            r = ("y", ang, [x0 + 1.0, 8.4, -2.8])
            h.add([x0, 7.6, -5.6], [x0 + 2.0, 9.4, -2.8], "nvg", r).add([x0 + 0.3, 7.9, -5.8], [x0 + 1.7, 9.1, -5.6], "lens", r)
    else:
        h.add([7.2, 13.6, -1.6], [8.8, 15.6, 0.2], "nvg")
        h.add([3.0, 15.2, -1.8], [13.0, 17.2, 0.0], "nvg")
        for x0 in (5.4, 8.6):
            h.add([x0, 17.2, -1.6], [x0 + 2.0, 21.0, 0.4], "nvg").add([x0 + 0.3, 21.0, -1.3], [x0 + 1.7, 21.2, 0.1], "lens")
        for x0, ang in ((2.0, -22.5), (12.0, 22.5)):
            r = ("z", ang, [x0 + 1.0, 17.2, -0.6])
            h.add([x0, 17.2, -1.6], [x0 + 2.0, 20.2, 0.4], "nvg", r).add([x0 + 0.3, 20.2, -1.3], [x0 + 1.7, 20.4, 0.1], "lens", r)
    return h


def juggernaut():
    h = H().dome(8.8, 17.8, 7.9, 7.9, 8, "jugg")
    h.add([2.0, 17.0, 3.0], [14.0, 18.2, 13.0], "black")                   # plaque sommitale
    h.mirror([0.1, 1.6, 1.0], [0.9, 9.0, 15.0], "jugg")
    h.add([0.9, 1.6, 15.0], [15.1, 9.0, 15.9], "jugg")
    h.mirror([0.6, 0.6, 0.0], [2.0, 8.0, 6.0], "black")                    # plaques de joues
    h.add([1.2, 0.6, -0.6], [14.8, 5.6, 0.8], "black")                     # mandibule balistique
    for x in (4.0, 6.0, 8.0, 10.0, 12.0):
        h.add([x - 0.4, 1.4, -0.7], [x + 0.4, 4.4, -0.6], "visor_d")
    h.add([1.4, 5.6, -0.4], [14.6, 9.4, 0.6], "dark_glass")                # visière numérique blindée
    h.add([2.0, 8.6, -0.5], [14.0, 8.8, -0.4], "cyan")
    h.add([2.4, 6.0, -0.5], [3.0, 6.6, -0.4], "red").add([13.0, 6.0, -0.5], [13.6, 6.6, -0.4], "red")
    h.add([1.0, 9.4, -0.8], [15.0, 11.2, 0.8], "black")                     # front blindé
    return h


HELMETS = {
    "galea": galea, "great_helm": great_helm, "adrian_m15": adrian, "brodie_mk1": brodie,
    "m1_helmet": m1, "stahlhelm_m40": stahlhelm, "ssh40": ssh40,
    "pasgt": pasgt, "altyn": lambda: altyn(False), "mich_2000": mich,
    "ops_core_fast": lambda: fast(False), "juggernaut_helmet": juggernaut,
}
MODULES = {"altyn": lambda: altyn(True), "ops_core_fast": lambda: fast(True)}

DISPLAY = {
    "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [18, 200, 0], "translation": [0, -1.5, 0], "scale": [0.62, 0.62, 0.62]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 1], "scale": [0.35, 0.35, 0.35]},
    "firstperson_righthand": {"rotation": [0, 225, 0], "translation": [2, 3, 0], "scale": [0.4, 0.4, 0.4]},
}


# ------------------------------------------------------------------ les 10 gilets (repère du torse)
# Torse : x [4 ; 12], y [2 ; 14] (cou en haut), z [6 ; 10] ; devant vers -Z ; +X = côté DROIT du joueur.
def shell(h, key, y0=2.4, y1=13.8, t=0.9, front=True, back=True, sides=True):
    if front:
        h.add([3.4, y0, 6 - t], [12.6, y1, 6.0], key)
    if back:
        h.add([3.4, y0, 10.0], [12.6, y1, 10 + t], key)
    if sides:
        h.add([3.4, y0, 6.0], [4.0, y1, 10.0], key).add([12.0, y0, 6.0], [12.6, y1, 10.0], key)
    h.add([4.0, y1 - 0.2, 6 - t], [6.4, y1 + 0.5, 10 + t], key)                  # bretelles
    h.add([9.6, y1 - 0.2, 6 - t], [12.0, y1 + 0.5, 10 + t], key)
    return h


def collar(h, key, top=15.4, front_open=True):
    h.add([3.8, 13.6, 9.6], [12.2, top, 10.9], key)                                  # col montant (nuque)
    h.mirror([3.6, 13.6, 5.4], [4.6, top - 0.3, 10.4], key)
    if not front_open:
        h.add([4.6, 13.6, 4.9], [11.4, top - 0.8, 5.8], key)
    return h


def molle(h, key, z, x0=4.6, x1=11.4, rows=(6.4, 7.6, 8.8)):
    for y in rows:
        h.add([x0, y, z - 0.15], [x1, y + 0.35, z], key)
    return h


def cuirass():
    h = H()
    for i in range(4):                                                               # plaques d'acier qui se chevauchent
        y0 = 3.0 + i * 2.6
        w = 4.2 - abs(i - 2.2) * 0.25
        z0 = 4.7 if i % 2 else 4.4
        h.add([8 - w, y0, z0], [8 + w, y0 + 2.9, z0 + 1.0], "iron_ox")
        for x in (8 - w + 0.6, 8 + w - 1.2):
            h.add([x, y0 + 2.2, z0 - 0.2], [x + 0.6, y0 + 2.8, z0], "rivet")
    h.add([4.4, 13.6, 4.6], [6.0, 14.4, 10.6], "leather_d").add([10.0, 13.6, 4.6], [11.6, 14.4, 10.6], "leather_d")
    h.add([4.4, 4.0, 10.0], [5.6, 13.6, 10.5], "leather_d").add([10.4, 4.0, 10.0], [11.6, 13.6, 10.5], "leather_d")
    h.add([3.6, 7.6, 6.0], [12.4, 8.4, 10.5], "leather_d")                           # sangle de dos
    return h


def m12():
    h = shell(H(), "olive")
    for y in (4.0, 5.6, 7.2, 8.8, 10.4, 12.0):                                       # coutures matelassées
        h.add([3.5, y, 4.95], [12.5, y + 0.25, 5.1], "olive_d").add([3.5, y, 10.9], [12.5, y + 0.25, 11.05], "olive_d")
    for x in (5.6, 8.0, 10.4):
        h.add([x, 2.6, 4.95], [x + 0.25, 13.6, 5.1], "olive_d")
    return h


def pasgt_vest():
    h = collar(shell(H(), "woodland"), "woodland")
    h.add([7.8, 2.4, 4.9], [8.2, 13.6, 5.05], "olive_d")                              # fermeture
    h.add([4.2, 2.8, 4.3], [7.2, 5.6, 5.1], "woodland").add([8.8, 2.8, 4.3], [11.8, 5.6, 5.1], "woodland")
    return h


def vest_6b3():
    h = shell(H(), "soviet")
    for row in range(3):                                                               # écailles de titane sous le tissu
        for col in range(5):
            x = 4.0 + col * 1.6 + (0.8 if row % 2 else 0)
            if x < 11.6:
                h.add([x, 9.6 + row * 1.3, 4.95], [x + 1.3, 10.6 + row * 1.3, 5.05], "scale_ti")
    for i in range(4):                                                                 # poches à chargeurs AK
        x = 4.0 + i * 2.0
        h.add([x, 3.0, 3.6], [x + 1.8, 7.6, 5.1], "soviet_d").add([x + 0.1, 7.6, 3.5], [x + 1.7, 8.2, 5.1], "soviet")
    return h


def swat():
    h = shell(H(), "black")
    h.add([5.6, -1.6, 4.6], [10.4, 2.4, 5.6], "black")                                 # protège-aine
    h.mirror([1.8, 12.6, 5.4], [4.2, 14.6, 10.6], "black")                            # épaulières légères
    # inscription blanche dans le dos ("SWAT", pixels de 0,4)
    letters = {"S": ["111", "100", "111", "001", "111"], "W": ["101", "101", "101", "111", "101"],
               "A": ["010", "101", "111", "101", "101"], "T": ["111", "010", "010", "010", "010"]}
    x = 4.9
    for ch in "SWAT":
        for r, row in enumerate(letters[ch]):
            for c, on in enumerate(row):
                if on == "1":
                    px, py = x + c * 0.4, 11.6 - r * 0.4
                    h.add([px, py, 10.9], [px + 0.4, py + 0.4, 11.0], "white")
        x += 1.6
    return h


def jpc():
    h = H()
    h.add([4.4, 5.6, 5.0], [11.6, 13.2, 6.0], "coyote").add([4.4, 5.6, 10.0], [11.6, 13.4, 11.0], "coyote")
    for y in (5.6, 7.4):                                                               # ceinture ajourée
        h.add([3.4, y, 6.0], [4.0, y + 1.0, 10.0], "coyote_d").add([12.0, y, 6.0], [12.6, y + 1.0, 10.0], "coyote_d")
    h.add([4.6, 13.0, 5.4], [6.2, 14.3, 10.6], "coyote").add([9.8, 13.0, 5.4], [11.4, 14.3, 10.6], "coyote")
    molle(h, "coyote_d", 5.0)
    for x in (4.8, 7.2, 9.6):                                                          # 3 chargeurs M4 sur la poitrine
        h.add([x, 9.2, 4.0], [x + 1.6, 11.6, 5.0], "coyote_d").add([x + 0.3, 11.6, 4.2], [x + 1.3, 13.0, 4.8], "black")
    return h


def iotv():
    h = collar(shell(H(), "multicam", t=1.0), "multicam")
    h.mirror([1.8, 11.4, 5.0], [4.2, 14.4, 11.0], "multicam")                         # épaulières blindées
    h.add([3.6, 12.2, 4.4], [5.4, 14.8, 5.4], "ranger_d")                             # radio, épaule gauche
    h.add([4.2, 14.8, 4.7], [4.5, 18.0, 5.0], "black")                                 # antenne
    h.add([5.0, 3.6, 11.0], [11.0, 13.0, 12.6], "ranger")                              # poche d'hydratation
    molle(h, "ranger_d", 5.0, rows=(4.0, 5.2, 6.4))
    return h


def avs():
    h = H()
    h.add([4.4, 5.4, 4.9], [11.6, 13.2, 6.0], "ranger").add([4.4, 5.4, 10.0], [11.6, 13.4, 11.1], "ranger")
    h.mirror([3.0, 4.6, 6.2], [4.0, 8.6, 9.8], "ranger_d")                            # poches latérales blindées
    h.add([4.6, 13.0, 5.2], [6.4, 14.4, 10.8], "ranger").add([9.6, 13.0, 5.2], [11.4, 14.4, 10.8], "ranger")
    h.add([4.7, 12.4, 4.6], [6.3, 13.6, 5.2], "red").add([4.9, 12.7, 4.5], [6.1, 13.0, 4.6], "black")  # garrot
    h.add([5.6, 3.2, 4.0], [10.4, 5.4, 5.0], "ranger_d")                               # poche utilitaire
    molle(h, "ranger_d", 4.9)
    return h


def fort_defender():
    h = shell(H(), "moss", y0=1.6, y1=14.0, t=1.2)
    h.add([3.6, 2.0, 3.6], [12.4, 13.0, 4.8], "moss").add([4.2, 3.0, 3.0], [11.8, 11.0, 3.6], "olive_d")   # plaques superposées
    h.add([3.6, 2.0, 11.2], [12.4, 13.0, 12.4], "moss")
    collar(h, "moss", top=16.0, front_open=False)
    h.mirror([1.2, 10.8, 4.4], [4.2, 14.6, 11.4], "moss")                             # grosses épaulières
    h.add([5.4, -2.2, 3.8], [10.6, 2.0, 5.0], "moss")                                  # bas-ventre
    return h


def eod():
    h = H()
    h.add([2.8, 0.4, 2.6], [13.2, 14.0, 6.0], "jugg")                                  # plastron intégral
    h.add([4.0, 8.0, 1.8], [12.0, 13.0, 2.6], "black")                                 # renfort pectoral
    h.add([2.8, 0.4, 10.0], [13.2, 14.0, 11.4], "jugg")
    h.add([2.6, 0.4, 6.0], [4.0, 14.0, 10.0], "jugg").add([12.0, 0.4, 6.0], [13.4, 14.0, 10.0], "jugg")
    h.add([3.2, 13.6, 8.6], [12.8, 17.2, 11.4], "black")                               # haute collerette
    h.mirror([2.8, 13.6, 4.6], [4.6, 16.6, 10.6], "black")
    h.add([4.4, 3.0, 11.4], [11.6, 13.0, 13.8], "black")                               # pack dorsal stabilisateur
    h.mirror([1.0, 10.4, 3.8], [3.6, 14.6, 11.8], "jugg")
    return h


VESTS = {"wwi_cuirass": cuirass, "m12_flak_vest": m12, "pasgt_vest": pasgt_vest, "vest_6b3": vest_6b3, "swat_vest": swat,
         "jpc_plate_carrier": jpc, "iotv_vest": iotv, "avs_plate_carrier": avs, "fort_defender": fort_defender, "eod_suit": eod}

VEST_DISPLAY = {
    "gui": {"rotation": [12, 200, 0], "translation": [0, 0, 0], "scale": [0.7, 0.7, 0.7]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.7, 0.7, 0.7]},
    "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 1], "scale": [0.4, 0.4, 0.4]},
    "firstperson_righthand": {"rotation": [0, 225, 0], "translation": [2, 3, 0], "scale": [0.45, 0.45, 0.45]},
}


def vest_models():
    for name, fn in VESTS.items():
        els = fn().els
        check(name, els)
        baked, tex = baked_models(name, {"m": els})
        write(os.path.join(ASSETS, f"models/item/{name}.json"),
              {"textures": tex, "render_type": "minecraft:translucent", "display": VEST_DISPLAY, "elements": vb.elements(baked, "m")})


# ------------------------------------------------------------------ pantalons (repère d'une jambe)
# Jambe : x [6 ; 10], y [2 ; 14] (hanche en haut), z [6 ; 10] ; devant vers -Z ; +X = côté droit du joueur.
# Ceinture : repère du torse (comme les gilets), taille vers y = 2.
def leg_shell(h, key, y0=3.0, y1=14.2, t=0.45):
    h.add([6 - t, y0, 6 - t], [10 + t, y1, 6.0], key).add([6 - t, y0, 10.0], [10 + t, y1, 10 + t], key)
    h.add([6 - t, y0, 6.0], [6.0, y1, 10.0], key).add([10.0, y0, 6.0], [10 + t, y1, 10.0], key)
    return h


def outer(h, right, y0, y1, z0, z1, d, key, gap=0.45):
    """Pièce sur la face EXTÉRIEURE de la jambe (gauche : -X, droite : +X)."""
    if right:
        h.add([10 + gap, y0, z0], [10 + gap + d, y1, z1], key)
    else:
        h.add([6 - gap - d, y0, z0], [6 - gap, y1, z1], key)
    return h


def belt(key, buckle="brass"):
    h = H()
    h.add([3.5, 1.4, 5.5], [12.5, 2.8, 6.0], key).add([3.5, 1.4, 10.0], [12.5, 2.8, 10.5], key)
    h.add([3.5, 1.4, 6.0], [4.0, 2.8, 10.0], key).add([12.0, 1.4, 6.0], [12.5, 2.8, 10.0], key)
    h.add([7.2, 1.5, 5.3], [8.8, 2.7, 5.5], buckle)
    return h


def pants(shell, belt_key, extra=None, buckle="brass", belt_extra=None):
    parts = {}
    for side, right in (("leg_l", False), ("leg_r", True)):
        h = leg_shell(H(), shell)
        if extra:
            extra(h, right)
        parts[side] = h
    b = belt(belt_key, buckle)
    if belt_extra:
        belt_extra(b)
    parts["belt"] = b
    return parts


def cargo(h, right, key, flap):
    outer(h, right, 8.0, 11.4, 6.5, 9.5, 0.6, key)
    outer(h, right, 11.0, 11.6, 6.4, 9.6, 0.7, flap)


def p_wwi():
    def puttees(h, right):
        for i in range(7):
            y = 2.6 + i * 0.8
            h.add([5.4, y, 5.4], [10.6, y + 0.55, 10.6], "khaki_d")
    return pants("khaki", "leather_d", puttees)


def p_m42():
    def e(h, right):
        cargo(h, right, "olive", "olive_d")
        h.add([5.45, 7.3, 5.45], [10.55, 7.8, 10.55], "canvas")                    # sangle de maintien
    return pants("olive", "canvas", e)


def p_bdu():
    def e(h, right):
        h.add([6.2, 6.0, 5.3], [9.8, 8.4, 5.55], "woodland")                       # genoux doublés
        h.add([6.2, 10.5, 10.45], [9.8, 13.5, 10.7], "woodland")                   # assise renforcée
        cargo(h, right, "woodland", "woodland")
    return pants("woodland", "olive_d", e)


def p_swat():
    def e(h, right):
        h.add([5.9, 5.6, 4.8], [10.1, 8.6, 5.55], "comtac")                         # grosses genouillères
        if right:                                                                   # étui de cuisse (holster)
            outer(h, True, 7.0, 11.2, 6.4, 8.8, 1.1, "black")
            outer(h, True, 11.2, 12.6, 6.8, 7.8, 0.8, "rail", gap=0.6)
            h.add([5.45, 9.2, 5.45], [11.6, 9.7, 10.55], "black")
    return pants("black", "black", e, buckle="rail")


def p_crye():
    def e(h, right):
        h.add([6.2, 5.8, 5.2], [9.8, 8.2, 5.55], "coyote_d")                       # genouillères AirFlex
        cargo(h, right, "multicam", "coyote_d")
        outer(h, right, 4.0, 6.0, 7.0, 9.0, 0.5, "multicam")
    return pants("multicam", "coyote_d", e, buckle="rail")


def p_infiltrator():
    def e(h, right):
        for y in (4.6, 9.4, 12.6):                                                  # sangles élastiques silencieuses
            h.add([5.45, y, 5.45], [10.55, y + 0.4, 10.55], "steel_d")
    return pants("black", "steel_d", e, buckle="steel_d")


def p_ratnik():
    def e(h, right):
        h.add([5.6, 8.5, 5.0], [10.4, 13.6, 5.55], "olive_d")                      # kevlar multicouche sur les cuisses
        h.add([6.0, 2.8, 5.0], [10.0, 7.6, 5.55], "scale_ti")                      # protège-tibias en titane
        cargo(h, right, "moss", "olive_d")
    return pants("moss", "moss", e, buckle="scale_ti")


def p_talos():
    def e(h, right):
        outer(h, right, 3.0, 13.6, 7.6, 8.4, 0.45, "black")                         # vérins hydrauliques
        outer(h, right, 7.4, 8.6, 7.2, 8.8, 0.7, "scale_ti", gap=0.45)              # articulation du genou
        h.add([6.4, 2.6, 4.9], [9.6, 3.2, 5.5], "black")
    def battery(b):
        b.add([6.0, 0.8, 10.5], [10.0, 3.2, 11.8], "black").add([7.6, 2.4, 11.8], [8.4, 2.8, 11.9], "cyan")
    return pants("ranger", "black", e, buckle="scale_ti", belt_extra=battery)


# ------------------------------------------------------------------ rangers
def boot(key, top=6.4, sole="black", toe=None, laces="lace", extra=None):
    parts = {}
    for side, right in (("boot_l", False), ("boot_r", True)):
        h = H()
        h.add([5.5, 2.0, 5.5], [10.5, top, 10.5], key)
        h.add([5.5, 2.0, 4.5], [10.5, 3.4, 5.5], toe or key)                      # bout du pied
        h.add([5.3, 1.3, 4.3], [10.7, 2.1, 10.7], sole)                             # semelle crantée
        for x in (5.6, 7.4, 9.2):
            h.add([x, 1.1, 4.6], [x + 1.2, 1.3, 10.4], sole)
        if laces:
            h.add([7.5, 3.4, 5.42], [8.5, top - 0.3, 5.5], laces)
            for y in [3.8 + 0.8 * i for i in range(int((top - 4.2) / 0.8))]:
                h.add([7.0, y, 5.4], [9.0, y + 0.25, 5.48], laces)
        if extra:
            extra(h, right)
        parts[side] = h
    return parts


def b_wwi():
    def nails(h, right):
        for x in (5.8, 7.6, 9.4):
            h.add([x, 1.0, 5.0], [x + 0.5, 1.15, 5.5], "rivet").add([x, 1.0, 9.4], [x + 0.5, 1.15, 9.9], "rivet")
    return boot("leather_d", top=4.2, sole="steel_d", laces="leather", extra=nails)


def b_corcoran():
    def ankle(h, right):
        h.add([5.4, 3.6, 5.4], [10.6, 4.6, 10.6], "leather_d")                     # double renfort de cheville
    return boot("leather_red", top=7.0, extra=ankle)


def b_m65():
    def strap(h, right):
        h.add([5.4, 6.4, 5.4], [10.6, 7.0, 10.6], "black").add([9.6, 6.3, 5.25], [10.4, 7.1, 5.4], "rivet")
    return boot("black", top=7.2, sole="comtac", extra=strap)


def b_panama():
    def foot(h, right):
        h.add([5.45, 2.0, 5.45], [10.55, 3.6, 10.55], "black")                      # pied en cuir noir
    return boot("olive", top=7.0, extra=foot)


def b_lowa():
    def frame(h, right):
        outer(h, right, 2.4, 5.4, 6.4, 9.6, 0.15, "coyote_d", gap=0.5)              # armature latérale
    return boot("coyote", top=6.8, sole="coyote_d", extra=frame)


def b_salomon():
    return boot("black", top=6.6, sole="comtac", laces="steel_d")


def b_eod():
    def armor(h, right):
        h.add([5.2, 2.0, 5.2], [10.8, 8.0, 10.8], "moss")
        h.add([5.6, 2.4, 4.4], [10.4, 7.6, 5.2], "scale_ti")                       # plaques de titane
        h.add([5.0, 0.8, 4.2], [11.0, 2.0, 11.0], "black")                         # semelle déflectrice de souffle
    return boot("moss", top=8.0, laces=None, extra=armor)


def b_apex():
    def springs(h, right):
        h.add([6.0, 0.2, 8.4], [10.0, 1.4, 10.6], "black")                         # lame amortissante sous le talon
        for x0, x1 in ((5.15, 5.5), (10.5, 10.85)):
            h.add([x0, 1.6, 8.6], [x1, 4.6, 9.6], "scale_ti")                      # ressorts latéraux
    return boot("ranger", top=6.6, sole="black", toe="scale_ti", extra=springs)


GEAR = {
    "wwi_trousers": p_wwi, "m42_paratrooper_pants": p_m42, "bdu_woodland_pants": p_bdu, "swat_pants": p_swat,
    "crye_g3_pants": p_crye, "infiltrator_pants": p_infiltrator, "ratnik_pants": p_ratnik, "talos_exo_pants": p_talos,
    "wwi_hobnail_boots": b_wwi, "corcoran_jump_boots": b_corcoran, "m65_combat_boots": b_m65, "panama_jungle_boots": b_panama,
    "lowa_zephyr_boots": b_lowa, "salomon_forces_boots": b_salomon, "eod_boots": b_eod, "apex_jump_boots": b_apex,
}


def shifted(els, dx, dy):
    out = json.loads(json.dumps(els))
    for e in out:
        for k in ("from", "to"):
            e[k] = [round(e[k][0] + dx, 3), round(e[k][1] + dy, 3), e[k][2]]
        if "rotation" in e:
            o = e["rotation"]["origin"]
            e["rotation"]["origin"] = [o[0] + dx, o[1] + dy, o[2]]
    return out


def gear_models():
    for name, fn in GEAR.items():
        parts = fn()
        for part, h in parts.items():
            check(name, h.els)
        baked, tex = baked_models(name, {part: h.els for part, h in parts.items()})
        parts = {part: vb.elements(baked, part) for part in parts}
        for part, els in parts.items():
            write(os.path.join(ASSETS, f"models/gear/{name}_{part}.json"),
                  {"textures": tex, "render_type": "minecraft:translucent", "elements": els})
        if "belt" in parts:   # pantalon complet (inventaire) : ceinture + jambes sous la taille
            whole = parts["belt"] + shifted(parts["leg_l"], -2, -12) + shifted(parts["leg_r"], 2, -12)
            disp = {"gui": {"rotation": [15, 200, 0], "translation": [0, 6, 0], "scale": [0.55, 0.55, 0.55]}}
        else:
            whole = shifted(parts["boot_l"], -2.6, 0) + shifted(parts["boot_r"], 2.6, 0)
            disp = {"gui": {"rotation": [20, 200, 0], "translation": [0, 3, 0], "scale": [0.85, 0.85, 0.85]}}
        disp.update({k: v for k, v in VEST_DISPLAY.items() if k != "gui"})
        check(name, whole)
        write(os.path.join(ASSETS, f"models/item/{name}.json"),
              {"textures": tex, "render_type": "minecraft:translucent", "display": disp, "elements": whole})


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, separators=(",", ":"))


def check(name, els):
    for e in els:
        for v in e["from"] + e["to"]:
            assert -16 <= v <= 32, f"{name} hors limites : {e['from']} {e['to']}"


def helmet_models():
    for name, fn in HELMETS.items():
        groups = {"m": fn().els}
        if name in MODULES:
            groups["mod"] = MODULES[name]().els
        for els in groups.values():
            check(name, els)
        baked, tex = baked_models(name, groups)
        model = {"textures": tex, "render_type": "minecraft:translucent", "display": DISPLAY, "elements": vb.elements(baked, "m")}
        if name in MODULES:
            model["overrides"] = [{"predicate": {f"{MOD}:module": 1}, "model": f"{MOD}:item/{name}_module"}]
            write(os.path.join(ASSETS, f"models/item/{name}_module.json"),
                  {"textures": tex, "render_type": "minecraft:translucent", "display": DISPLAY, "elements": vb.elements(baked, "mod")})
        write(os.path.join(ASSETS, f"models/item/{name}.json"), model)


# ------------------------------------------------------------------ contours d'écran (vue à la 1re personne)
def overlay(name, fn, w=320, h=180):
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            px[x, y] = fn(x / w, y / h)
    img.save(os.path.join(ASSETS, "textures/misc", f"helmet_{name}.png"))


def vignette(strength, inner=0.62):
    def f(u, v):
        d = math.hypot((u - 0.5) * 1.6, v - 0.5) / 0.75
        a = max(0.0, min(1.0, (d - inner) / (1.0 - inner)))
        return (8, 8, 10, int(255 * strength * a ** 1.6))
    return f


def visor_slit(u, v):
    # fenêtre blindée : rectangle arrondi au centre, le reste est la visière (sombre)
    dx, dy = abs(u - 0.5) / 0.34, abs(v - 0.47) / 0.21
    d = (dx ** 6 + dy ** 6) ** (1 / 6)
    if d < 0.94:
        return (60, 90, 100, 28)                                          # léger reflet du verre
    if d < 1.0:
        return (20, 24, 26, 235)                                          # cadre
    return (34, 42, 32, 250)


def nvg(u, v):
    # jumelles quadritubes : deux grands champs ronds qui se chevauchent (vue panoramique)
    ax = (u - 0.5) * 16 / 9
    d = min(math.hypot(ax + 0.36, v - 0.5), math.hypot(ax - 0.36, v - 0.5)) / 0.47
    a = max(0.0, min(1.0, (d - 0.92) / 0.08))
    return (0, 0, 0, int(255 * a))


def jugg_hud(u, v):
    # visière numérique : bords sombres + coins et lignes cyan discrets
    base = vignette(0.55, 0.7)(u, v)
    edge = min(u, 1 - u, v, 1 - v)
    cyan = (63, 200, 216)
    corner = (min(u, 1 - u) < 0.12 and min(v, 1 - v) < 0.012 + 0.0) or (min(v, 1 - v) < 0.2 and min(u, 1 - u) < 0.007)
    if 0.04 < edge and corner and min(u, 1 - u) > 0.03 and min(v, 1 - v) > 0.05:
        return cyan + (170,)
    if abs(v - 0.9) < 0.003 and 0.35 < u < 0.65:
        return cyan + (110,)
    return base


def overlays():
    os.makedirs(os.path.join(ASSETS, "textures/misc"), exist_ok=True)
    overlay("great_helm", vignette(0.92, 0.45))
    overlay("heavy", vignette(0.45, 0.72))
    overlay("visor", visor_slit)
    overlay("nvg", nvg)
    overlay("juggernaut", jugg_hud)


# ------------------------------------------------------------------ minerais et matériaux (16x16)
TI = [hexc(c) for c in ("#5E6874", "#8C98A6", "#B4C0CC", "#DCE4EE")]       # titane
ST = [hexc(c) for c in ("#2E3238", "#4A5058", "#646B74", "#8A929C")]       # acier
FE = [hexc(c) for c in ("#5A5A5A", "#8C8C8C", "#BDBDBD", "#E2E2E2")]       # fer
KV = [hexc(c) for c in ("#7A5E1C", "#B48C2C", "#D9B44A", "#F0D27A")]       # kevlar
CE = [hexc(c) for c in ("#8A8070", "#C2B8A4", "#E2DACA", "#F4F0E6")]       # céramique
STONE = [hexc(c) for c in ("#6e6e6e", "#7a7a7a", "#858585", "#8f8f8f", "#7f7f7f")]
DEEP = [hexc(c) for c in ("#3a3a40", "#46464d", "#4f4f57", "#5a5a62", "#424249")]


def ore(seed, base):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), base[rng.randrange(len(base))])
    for cx, cy in [(2, 3), (10, 2), (12, 10), (4, 11), (8, 7), (13, 5)]:
        cells = [(cx, cy), (cx + 1, cy), (cx, cy + 1), (cx + 1, cy + 1), (cx + 2, cy + 1)]
        for i, (x, y) in enumerate(cells):
            if 0 <= x < 16 and 0 <= y < 16 and rng.random() < 0.85:
                img.putpixel((x, y), TI[1 + (i % 3)])
        img.putpixel((cx, cy), TI[3])
        if cy + 1 < 16:
            img.putpixel((cx + 1, cy + 1), TI[0])
    return img


def ingot(pal, specks=None):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(1, 10), (5, 6), (15, 6), (11, 10)], fill=pal[2])          # dessus
    d.polygon([(1, 10), (11, 10), (11, 13), (1, 13)], fill=pal[1])        # face
    d.polygon([(11, 10), (15, 6), (15, 9), (11, 13)], fill=pal[0])        # côté
    d.line([(5, 7), (13, 7)], fill=pal[3])
    d.line([(1, 10), (11, 10)], fill=pal[3])
    if specks:
        rng = random.Random(3)
        for _ in range(7):
            img.putpixel((rng.randrange(2, 11), rng.randrange(7, 13)), specks)
    return img


def plate(pal, pattern=None):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(2, 14):
        for x in range(2, 14):
            c = pal[2]
            if pattern:
                c = pattern(x, y) or c
            if x == 2 or y == 2:
                c = pal[3]
            if x == 13 or y == 13:
                c = pal[0]
            img.putpixel((x, y), c)
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        img.putpixel((x, y), pal[1])
    return img


def raw_lump(pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(11)
    for y in range(16):
        for x in range(16):
            if math.hypot((x - 7.5) / 6.2, (y - 8.5) / 5.4) < 1 and rng.random() > 0.05:
                img.putpixel((x, y), pal[rng.choice([0, 1, 1, 2, 2, 3])])
    return img


def fiber():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for s in range(4):
        for x in range(1, 15):
            y = int(4 + s * 2.6 + math.sin(x * 0.9 + s) * 1.2)
            img.putpixel((x, y), KV[2 if (x + s) % 3 else 3])
            if y + 1 < 16:
                img.putpixel((x, y + 1), KV[1])
    return img


def woven(x, y):
    return KV[3] if (x // 2 + y // 2) % 2 == 0 else KV[1]


def multilayer():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i, (pal, pat) in enumerate(((KV, woven), (TI, None), (KV, woven))):
        y0 = 3 + i * 3
        for y in range(y0, y0 + 3):
            for x in range(2 + i, 15 - (2 - i)):
                c = pat(x, y) if pat else pal[2]
                if y == y0:
                    c = pal[3]
                img.putpixel((x, y), c)
    for x in range(4, 15):
        img.putpixel((x, 12), hexc("#6B4A2B"))                            # cuir
        img.putpixel((x, 13), hexc("#4A321E"))
    return img


def composite():
    return plate(TI, lambda x, y: CE[3] if (x % 4 != 1 and y % 4 != 1) else CE[1])


def preform():
    img = raw_lump([hexc("#8A4A2A"), hexc("#B0623A"), TI[1], hexc("#E0A040")])
    return img


def nylon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    g = [hexc(c) for c in ("#3A4230", "#4B5335", "#5E6844", "#6E7A50")]
    for y in range(3, 14):
        for x in range(2, 14):
            img.putpixel((x, y), g[2] if (x + y) % 2 else g[1])
    for x in range(2, 14):
        img.putpixel((x, 3), g[3])
        img.putpixel((x, 13), g[0])
    for y in range(5, 14, 3):
        for x in range(2, 14):
            img.putpixel((x, y), g[0])
    return img


def strap():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    s = [hexc(c) for c in ("#2A2C24", "#3E4230", "#555A42")]
    for i in range(1, 15):
        for w in range(3):
            x, y = i, 14 - i + w - 1
            if 0 <= y < 16:
                img.putpixel((x, y), s[w])
    for x in range(5, 9):                                                            # boucle
        for y in range(5, 9):
            if x in (5, 8) or y in (5, 8):
                img.putpixel((x, y), hexc("#8A8E92"))
    return img


def ballistic_plate(pal, edge):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(1, 15):
        for x in range(3, 13):
            if (y < 3 and (x < 4 + (3 - y) or x > 11 - (3 - y))):
                continue                                                               # coins coupés (plaque SAPI)
            c = pal[2]
            if x in (3, 12) or y in (1, 14):
                c = edge
            img.putpixel((x, y), c)
    for x in range(5, 11):
        img.putpixel((x, 4), pal[3])
    return img


def field_manual():
    """Manuel d'armement : couverture vert olive, tranche des pages, étoile au pochoir."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cover = [hexc(c) for c in ("#2A3020", "#3E4630", "#4B5335", "#5E6844")]
    for y in range(2, 15):
        for x in range(3, 14):
            img.putpixel((x, y), cover[2] if (x + y) % 5 else cover[3])
    for y in range(2, 15):
        img.putpixel((3, y), cover[0])
        img.putpixel((13, y), hexc("#E8E2CC"))                                       # pages
        img.putpixel((12, y), hexc("#C8C0A4"))
    for x in range(3, 14):
        img.putpixel((x, 2), cover[3])
        img.putpixel((x, 14), cover[0])
    for (x, y) in [(8, 5), (7, 6), (8, 6), (9, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7), (6, 8), (7, 8), (8, 8),
                   (9, 8), (10, 8), (7, 9), (9, 9), (6, 10), (10, 10)]:
        img.putpixel((x, y), hexc("#D8C878"))                                         # étoile
    for x in range(5, 11):
        img.putpixel((x, 12), hexc("#A09060"))
    return img


def materials():
    tex = os.path.join(ASSETS, "textures")
    ore(21, STONE).save(os.path.join(tex, "block/titanium_ore.png"))
    ore(22, DEEP).save(os.path.join(tex, "block/deepslate_titanium_ore.png"))
    items = {
        "raw_titanium": raw_lump(TI), "titanium_ingot": ingot(TI), "titanium_plate": plate(TI),
        "ceramic_titanium_composite": composite(), "composite_preform": preform(),
        "steel_ingot": ingot(ST), "carburized_iron": ingot(FE, specks=hexc("#1A1A1A")), "steel_plate": plate(ST),
        "kevlar_fiber": fiber(), "kevlar_plate": plate(KV, woven), "multilayer_kevlar": multilayer(),
        "ballistic_nylon": nylon(), "tactical_strap": strap(), "military_field_manual": field_manual(),
        "steel_ballistic_plate": ballistic_plate(ST, ST[0]),
        "ceramic_ballistic_plate": ballistic_plate(CE, KV[1]),
        "netherite_ballistic_plate": ballistic_plate([hexc(c) for c in ("#2A2426", "#443A3C", "#5A4E50", "#7A6C6E")], TI[2]),
    }
    for name, img in items.items():
        img.save(os.path.join(tex, f"item/{name}.png"))
        write(os.path.join(ASSETS, f"models/item/{name}.json"), {"parent": "minecraft:item/generated",
                                                                "textures": {"layer0": f"{MOD}:item/{name}"}})
    for b in ("titanium_ore", "deepslate_titanium_ore"):
        write(os.path.join(ASSETS, f"blockstates/{b}.json"), {"variants": {"": {"model": f"{MOD}:block/{b}"}}})
        write(os.path.join(ASSETS, f"models/block/{b}.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{b}"}})
        write(os.path.join(ASSETS, f"models/item/{b}.json"), {"parent": f"{MOD}:block/{b}"})


def main():
    os.makedirs(os.path.join(ASSETS, "textures/item/armor3d"), exist_ok=True)
    palette_image().save(os.path.join(ASSETS, "textures/item/helmet_palette.png"))
    helmet_models()
    vest_models()
    gear_models()
    overlays()
    materials()
    print(f"{len(HELMETS)} casques ({len(KEYS)} couleurs), {len(MODULES)} variantes, contours et matériaux générés")


if __name__ == "__main__":
    main()

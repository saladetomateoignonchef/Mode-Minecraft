#!/usr/bin/env python3
"""
Génère les modèles 3D des armes à feu, de leurs accessoires et de leurs skins.

Utilisation (depuis la racine du projet, APRÈS tools/generate_textures.py) :
    python3 tools/generate_textures.py
    python3 tools/generate_gun_models.py

Principe :
- Chaque arme est un assemblage de pavés (comme dans Blockbench), décrits dans GUNS.
  Repère : le canon pointe vers +X, le haut est +Y, l'arme est centrée sur Z = 8.
- Le chargeur est séparé du corps de l'arme : quand on monte un chargeur (24, 36, camembert),
  le chargeur d'origine est masqué (voir client/model/GunModels.java).
- Les accessoires (ACCESSORIES) sont dessinés autour de l'origine (0, 0, 0) ; le jeu les place
  ensuite sur les points de fixation de chaque arme (MOUNTS dans GunModels.java).
- Chaque skin = une palette de couleurs (SKINS). Les camouflages utilisent plusieurs couleurs par case.
- Dans l'inventaire, l'arme reste une icône 2D (recolorée selon le skin).
"""
import json
import os
import random
import zlib

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
MOD = "modeminecraft"

# ---------- Palette par défaut : une couleur par case de 4x4 pixels ----------
KEYS = ["k", "m", "l", "w", "W", "r", "d", "s", "t", "T", "u", "g", "G"]
DEFAULT = {
    "k": "#24272d",  # métal sombre
    "m": "#4a505a",  # métal
    "l": "#7d8691",  # métal clair
    "w": "#5a3e1b",  # bois sombre
    "W": "#8a6233",  # bois clair
    "r": "#c8223b",  # rubis
    "d": "#0b0c0e",  # noir (bouche du canon)
    "s": "#5fd4ff",  # verre de lunette
    "t": "#4b5a2a",  # kaki
    "T": "#6b7d3a",  # kaki clair
    "u": "#353f1d",  # kaki sombre
    "g": "#dba521",  # or
    "G": "#9aa3ad",  # acier
}

# ---------- Skins : remplacements de couleurs (liste = motif camouflage) ----------
SKINS = {
    "default": {},
    "desert": {
        "k": "#6e5a3a", "m": "#a68b5b", "l": "#cbb48a", "w": "#7a5a32", "W": "#a07c4a",
        "t": "#8a7a50", "T": "#b0a070", "u": "#6a5a38",
    },
    "woodland": {
        "k": ["#2f3a22", "#4b5a2a", "#5a4630", "#1e2416"],
        "m": ["#4b5a2a", "#6b7d3a", "#5a4630", "#2f3a22"],
        "l": ["#6b7d3a", "#8a9a50", "#5a4630"],
        "w": ["#5a4630", "#3a2e1e", "#4b5a2a"], "W": ["#6b5a3a", "#4b5a2a", "#8a7a50"],
        "t": ["#4b5a2a", "#2f3a22", "#5a4630"], "T": ["#6b7d3a", "#4b5a2a", "#8a7a50"],
        "u": ["#2f3a22", "#1e2416"],
    },
    "arctic": {
        "k": ["#8a9096", "#c8ccd0", "#5a6066", "#e8ecef"],
        "m": ["#dfe3e6", "#a9b0b6", "#ffffff"],
        "l": ["#f2f4f6", "#c8ccd0"],
        "w": ["#b0b6bc", "#d6dadd"], "W": ["#d6dadd", "#f2f4f6"],
        "t": ["#c8ccd0", "#e8ecef"], "T": ["#e8ecef", "#ffffff"], "u": ["#8a9096", "#a9b0b6"],
    },
    "gold": {
        "k": "#7a5a08", "m": "#c9971a", "l": "#f2cf4a", "w": "#3a2410", "W": "#5a3a1c",
        "t": "#a8780e", "T": "#dba521", "u": "#7a5a08",
    },
    "ruby": {
        "k": "#5a0c18", "m": "#a01a30", "l": "#e04a62", "w": "#2a1a10", "W": "#4a2a18", "r": "#dba521",
        "t": "#8a1426", "T": "#c8223b", "u": "#5a0c18",
    },
}


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def skin_color(skin, key, x, y, seed=0):
    """Couleur d'un pixel pour une clé de palette, avec motif si le skin est un camouflage."""
    value = SKINS[skin].get(key, DEFAULT[key])
    if isinstance(value, str):
        return hexc(value)
    # Motif "taches" : on choisit la couleur selon un bruit grossier (blocs de 2x2)
    rng = random.Random(zlib.crc32(repr((skin, key, x // 2, y // 2, seed)).encode()))
    return hexc(rng.choice(value))


def palette_image(skin):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i, key in enumerate(KEYS):
        cx, cy = (i % 4) * 4, (i // 4) * 4
        for y in range(4):
            for x in range(4):
                img.putpixel((cx + x, cy + y), skin_color(skin, key, x, y))
    return img


def cell_uv(color):
    i = KEYS.index(color)
    cx, cy = (i % 4) * 4, (i // 4) * 4
    return [cx, cy, cx + 4, cy + 4]


def box(frm, to, color, rot=None):
    """Un pavé. rot = (axe, angle, origine) avec angle dans -45, -22.5, 0, 22.5, 45."""
    uv = cell_uv(color)
    el = {"from": frm, "to": to,
          "faces": {f: {"uv": uv, "texture": "#p"} for f in ("north", "east", "south", "west", "up", "down")}}
    if rot:
        axis, angle, origin = rot
        el["rotation"] = {"angle": angle, "axis": axis, "origin": origin}
    return el


# =====================================================================
# ARMES : chaque fonction renvoie (corps, chargeur)
# =====================================================================
def pistol():
    body = [
        box([3, 8, 7], [14, 10.5, 9], "m"),               # culasse
        box([3, 10.5, 7.25], [14, 11, 8.75], "l"),
        box([3, 7, 7.1], [13, 8, 8.9], "k"),              # carcasse
        box([13.9, 8.75, 7.6], [14.1, 9.75, 8.4], "d"),   # bouche du canon
        box([3, 2, 7.2], [6, 7, 8.8], "w", ("z", -22.5, [4.5, 7, 8])),  # poignée
        box([6, 6, 7.5], [8.5, 6.5, 8.5], "k"),           # pontet
        box([8.5, 6, 7.5], [9, 7, 8.5], "k"),
        box([7, 6.5, 7.75], [7.5, 7, 8.25], "d"),         # détente
        box([10, 7.2, 6.95], [11, 7.8, 9.05], "r"),       # rubis
        box([13, 11, 7.75], [13.5, 11.5, 8.25], "k"),     # guidon
        box([3.5, 11, 7.5], [4.5, 11.5, 8.5], "k"),       # hausse
    ]
    return body, []


def assault_rifle():
    body = [
        box([-2, 5, 7.2], [3, 8.5, 8.8], "w"),            # crosse
        box([-2.3, 4.5, 7.1], [-2, 9, 8.9], "k"),
        box([3, 6.5, 7], [10, 9.5, 9], "m"),              # boîtier
        box([3.5, 9.5, 7.4], [9.5, 10, 8.6], "k"),        # rail
        box([4, 10, 7.5], [5, 11, 8.5], "k"),             # hausse
        box([10, 6.8, 7.1], [14.5, 9.2, 8.9], "k"),       # garde-main
        box([14.5, 7.6, 7.6], [18, 8.4, 8.4], "k"),       # canon
        box([17.5, 7.4, 7.4], [18.5, 8.6, 8.6], "l"),
        box([18.45, 7.75, 7.75], [18.55, 8.25, 8.25], "d"),
        box([13.5, 9.2, 7.75], [14, 10.5, 8.25], "k"),    # guidon
        box([4, 3, 7.3], [6, 6.5, 8.7], "k", ("z", -22.5, [5, 6.5, 8])),  # poignée
        box([6, 5.6, 7.6], [7.4, 6, 8.4], "k"),
        box([6.4, 6, 7.75], [6.8, 6.5, 8.25], "d"),
        box([8, 8.2, 6.95], [9.5, 8.8, 9.05], "r"),
    ]
    mag = [box([7.5, 2.5, 7.4], [9.5, 6.5, 8.6], "l", ("z", 22.5, [8.5, 6.5, 8]))]
    return body, mag


def ak47():
    body = [
        box([-3, 4.8, 7.25], [3, 8, 8.75], "W"),          # crosse en bois
        box([-3.3, 4.4, 7.2], [-3, 8.3, 8.8], "k"),
        box([3, 6.5, 7.1], [10.5, 9, 8.9], "m"),          # boîtier
        box([3, 9, 7.3], [10, 9.6, 8.7], "l"),            # couvercle
        box([4, 3, 7.3], [5.6, 6.5, 8.7], "w", ("z", -22.5, [5, 6.5, 8])),  # poignée
        box([5.8, 5.6, 7.6], [7.8, 6, 8.4], "k"),         # pontet
        box([6.3, 6, 7.8], [6.7, 6.5, 8.2], "d"),
        box([10.5, 6.6, 7.15], [14.5, 8.4, 8.85], "W"),   # garde-main bas
        box([10.5, 8.6, 7.4], [14, 9.4, 8.6], "W"),       # garde-main haut (tube de gaz)
        box([14.5, 7.4, 7.65], [19, 8.2, 8.35], "k"),     # canon
        box([18.6, 7.3, 7.55], [19.3, 8.3, 8.45], "k"),
        box([19.25, 7.6, 7.85], [19.35, 8, 8.15], "d"),
        box([16.5, 8.2, 7.7], [17.2, 9.8, 8.3], "k"),     # guidon
        box([10.2, 9.4, 7.7], [10.8, 9.9, 8.3], "k"),     # hausse
        box([4, 7, 6.95], [5, 7.5, 9.05], "r"),
    ]
    mag = [  # chargeur "banane"
        box([8, 3, 7.4], [10, 6.5, 8.6], "m", ("z", 22.5, [9, 6.5, 8])),
        box([9.3, 1, 7.4], [11.1, 3.6, 8.6], "m", ("z", 45, [10.2, 3.6, 8])),
    ]
    return body, mag


def m4():
    body = [
        box([-3.5, 5.5, 7.2], [0, 8.8, 8.8], "k"),        # crosse
        box([-3.5, 8.8, 7.4], [0, 9.2, 8.6], "m"),
        box([-1, 7.6, 7.6], [3, 8.6, 8.4], "k"),          # tube de crosse
        box([3, 6.3, 7.15], [9.5, 8, 8.85], "m"),         # carcasse inférieure
        box([3, 8, 7.1], [10, 9.6, 8.9], "m"),            # carcasse supérieure
        box([3.5, 9.6, 7.45], [14, 10, 8.55], "k"),       # rail
        box([2.6, 9, 7.6], [3.1, 9.4, 8.4], "k"),         # levier d'armement
        box([4, 3, 7.3], [5.6, 6.3, 8.7], "k", ("z", -22.5, [5, 6.3, 8])),  # poignée
        box([5.8, 5.8, 7.6], [7.8, 6.2, 8.4], "k"),
        box([6.3, 6.2, 7.8], [6.7, 6.6, 8.2], "d"),
        box([10, 7, 7.05], [15, 9.6, 8.95], "k"),         # garde-main
        box([15, 7.8, 7.65], [19.5, 8.6, 8.35], "k"),     # canon
        box([15.8, 8.6, 7.75], [16.4, 11, 8.25], "k"),    # guidon en A
        box([19, 7.6, 7.5], [20, 8.8, 8.5], "k"),         # cache-flamme
        box([19.95, 7.95, 7.75], [20.05, 8.45, 8.25], "d"),
        box([7.5, 8.4, 6.95], [9, 9, 9.05], "r"),
    ]
    mag = [box([7.6, 2.4, 7.35], [9.4, 6.3, 8.65], "m")]
    return body, mag


def famas():
    body = [
        box([-3, 5.5, 7.1], [10, 9.5, 8.9], "k"),         # corps (bullpup)
        box([-3.3, 5, 7.15], [-3, 9.7, 8.85], "m"),       # plaque de couche
        box([-2, 9.5, 7.4], [0, 10, 8.6], "m"),           # appui-joue
        box([0, 9.5, 7.6], [1, 11.5, 8.4], "k"),          # poignée de transport
        box([11, 9.2, 7.6], [12, 11.5, 8.4], "k"),
        box([0, 11.5, 7.5], [12, 12.3, 8.5], "k"),
        box([10, 6.5, 7.15], [14, 9.2, 8.85], "m"),       # avant
        box([14, 7.4, 7.65], [18, 8.2, 8.35], "k"),       # canon
        box([17.6, 7.2, 7.5], [18.4, 8.4, 8.5], "k"),
        box([18.35, 7.55, 7.75], [18.45, 8.05, 8.25], "d"),
        box([7.5, 3, 7.3], [9.1, 6.5, 8.7], "k", ("z", -22.5, [8.5, 5.5, 8])),  # poignée
        box([9, 3.5, 7.6], [11, 4, 8.4], "k"),            # grand pontet
        box([10.6, 4, 7.6], [11, 5.5, 8.4], "k"),
        box([9.4, 5, 7.8], [9.8, 5.5, 8.2], "d"),
        box([5, 7, 6.95], [6, 7.5, 9.05], "r"),
    ]
    mag = [box([3, 2.5, 7.4], [4.8, 5.5, 8.6], "m")]  # chargeur derrière la poignée
    return body, mag


def shotgun():
    body = [
        box([-2, 5, 7.2], [3, 8.5, 8.8], "w"),
        box([-2.3, 4.5, 7.1], [-2, 9, 8.9], "k"),
        box([3, 7, 7], [8, 10, 9], "m"),
        box([8, 8.8, 7.5], [19, 10, 8.5], "k"),
        box([18.95, 9, 7.7], [19.05, 9.8, 8.3], "d"),
        box([8, 7.4, 7.6], [17, 8.6, 8.4], "l"),
        box([10, 6.8, 7.1], [14, 8.9, 8.9], "W"),         # pompe
        box([18.4, 10, 7.85], [18.8, 10.4, 8.15], "g"),
        box([3.5, 3.5, 7.3], [5.5, 7, 8.7], "w", ("z", -22.5, [4.5, 7, 8])),
        box([5.5, 6.4, 7.6], [7.5, 6.8, 8.4], "k"),
        box([6, 6.8, 7.75], [6.5, 7.2, 8.25], "d"),
        box([4.5, 8.2, 6.95], [6.5, 8.8, 9.05], "r"),
    ]
    return body, []


def sniper_rifle():
    body = [
        box([-3, 5, 7.2], [3, 8.5, 8.8], "w"),
        box([-1, 8.5, 7.4], [2, 9.3, 8.6], "w"),
        box([-3.3, 4.5, 7.1], [-3, 9, 8.9], "k"),
        box([3, 6.5, 7.1], [9, 9, 8.9], "m"),
        box([6, 8, 8.9], [6.5, 8.5, 10], "l"),
        box([6, 7.6, 10], [6.8, 8.4, 10.6], "k"),
        box([9, 7.3, 7.6], [22, 8.3, 8.4], "k"),
        box([21.5, 7.1, 7.4], [22.5, 8.5, 8.6], "m"),
        box([22.45, 7.55, 7.75], [22.55, 8.05, 8.25], "d"),
        box([3, 10.5, 7.2], [11, 12, 8.8], "k"),          # lunette
        box([2, 10.2, 6.9], [3.5, 12.3, 9.1], "k"),
        box([10, 10.2, 6.9], [12, 12.3, 9.1], "k"),
        box([11.95, 10.5, 7.2], [12.05, 12, 8.8], "s"),
        box([1.95, 10.5, 7.2], [2.05, 12, 8.8], "s"),
        box([4.5, 9, 7.6], [5.5, 10.5, 8.4], "m"),
        box([8.5, 9, 7.6], [9.5, 10.5, 8.4], "m"),
        box([3.5, 3.5, 7.3], [5.5, 7, 8.7], "w", ("z", -22.5, [4.5, 7, 8])),
        box([6.5, 7, 6.95], [8, 7.6, 9.05], "r"),
    ]
    mag = [box([7, 5, 7.4], [8.5, 6.5, 8.6], "k")]
    return body, mag


def rocket_launcher():
    body = [
        box([-2, 7, 6.5], [18, 10, 9.5], "T"),
        box([-2, 6.8, 6.6], [18, 7, 9.4], "u"),
        box([-2.2, 6.8, 6.3], [-0.5, 10.2, 9.7], "r"),
        box([16.5, 6.8, 6.3], [18.2, 10.2, 9.7], "r"),
        box([18.15, 7.4, 6.9], [18.25, 9.6, 9.1], "d"),
        box([-2.25, 7.4, 6.9], [-2.15, 9.6, 9.1], "d"),
        box([0, 5.5, 7.4], [2, 7, 8.6], "u"),
        box([4, 3.5, 7.3], [6, 7, 8.7], "k", ("z", -22.5, [5, 7, 8])),
        box([9, 4, 7.4], [10.5, 7, 8.6], "k"),
        box([7, 10, 7.4], [9, 11.5, 8.6], "k"),
        box([8.95, 10.3, 7.6], [9.05, 11.2, 8.4], "s"),
    ]
    return body, []


def rpg7():
    body = [
        box([-4, 7, 7.2], [14, 8.6, 8.8], "m"),           # tube
        box([-6, 6.6, 6.8], [-4, 9, 9.2], "k"),           # tuyère arrière
        box([-6.1, 7.2, 7.4], [-6, 8.4, 8.6], "d"),
        box([2, 6.7, 6.9], [8, 8.9, 9.1], "W"),           # protections en bois
        box([3.5, 3, 7.3], [5, 6.7, 8.7], "k", ("z", -22.5, [4.5, 6.7, 8])),  # poignée
        box([5.2, 5.6, 7.6], [6.8, 6, 8.4], "k"),
        box([8.5, 3.8, 7.35], [9.8, 6.8, 8.65], "k"),     # poignée avant
        box([12, 8.6, 7.8], [12.5, 9.6, 8.2], "k"),       # guidon
        box([3, 8.6, 7.8], [3.5, 9.4, 8.2], "k"),         # hausse
        box([13.9, 7.4, 7.4], [14.1, 8.2, 8.6], "d"),
    ]
    rocket = [  # roquette PG-7 : visible seulement quand l'arme est chargée
        box([14, 7.2, 7.4], [16, 8.4, 8.6], "t"),
        box([16, 6.4, 6.6], [19, 9.2, 9.4], "T"),
        box([19, 6.9, 7.1], [20.5, 8.7, 8.9], "T"),
        box([20.5, 7.5, 7.7], [22, 8.1, 8.3], "u"),
    ]
    return body, rocket


GUNS = {
    "pistol": pistol, "assault_rifle": assault_rifle, "ak47": ak47, "m4": m4, "famas": famas,
    "shotgun": shotgun, "sniper_rifle": sniper_rifle, "rocket_launcher": rocket_launcher, "rpg7": rpg7,
}

# =====================================================================
# ACCESSOIRES (dessinés autour de l'origine)
# =====================================================================
ACCESSORIES = {
    # Viseurs : posés sur le rail (bas du viseur en y = 0)
    "red_dot_sight": [
        box([-1.5, 0, -0.7], [1.5, 0.4, 0.7], "k"),
        box([-0.8, 0.4, -0.7], [1, 1.8, 0.7], "k"),
        box([1, 0.6, -0.5], [1.05, 1.6, 0.5], "s"),
        box([-0.85, 0.6, -0.5], [-0.8, 1.6, 0.5], "r"),
    ],
    "holo_sight": [
        box([-2, 0, -0.9], [2, 0.5, 0.9], "k"),
        box([-1.5, 0.5, -0.9], [1.5, 2.5, -0.6], "k"),
        box([-1.5, 0.5, 0.6], [1.5, 2.5, 0.9], "k"),
        box([-1.5, 2.5, -0.9], [1.5, 2.8, 0.9], "k"),
        box([0.9, 0.5, -0.6], [1, 2.5, 0.6], "s"),
    ],
    "acog_scope": [
        box([-1.5, 0, -0.6], [1.5, 0.6, 0.6], "k"),
        box([-3, 0.6, -0.9], [3, 2.4, 0.9], "k"),
        box([2, 0.4, -1.1], [3.5, 2.6, 1.1], "k"),
        box([3.5, 0.7, -0.8], [3.55, 2.3, 0.8], "s"),
        box([-3.5, 0.5, -1], [-2.5, 2.5, 1], "k"),
        box([-3.55, 0.8, -0.7], [-3.5, 2.2, 0.7], "s"),
    ],
    # Bouche : part de l'extrémité du canon vers +X
    "silencer": [
        box([0, -0.9, -0.9], [6, 0.9, 0.9], "k"),
        box([6, -0.6, -0.6], [6.1, 0.6, 0.6], "d"),
        box([0.5, -0.95, -0.95], [1, 0.95, 0.95], "m"),
    ],
    "compensator": [
        box([0, -0.7, -0.7], [2, 0.7, 0.7], "l"),
        box([0.5, 0.7, -0.3], [1.5, 0.75, 0.3], "d"),
        box([2, -0.4, -0.4], [2.05, 0.4, 0.4], "d"),
    ],
    # Poignées : sous le garde-main, vers le bas
    "vertical_grip": [
        box([-0.7, -4, -0.7], [0.7, 0, 0.7], "k"),
        box([-0.8, -4.3, -0.8], [0.8, -4, 0.8], "m"),
        box([-1.2, -0.4, -0.75], [1.2, 0, 0.75], "m"),
    ],
    "angled_grip": [
        box([-1.5, -2, -0.7], [1.5, 0, 0.7], "k", ("z", -22.5, [0, 0, 0])),
        box([-1.8, -0.4, -0.75], [1.8, 0, 0.75], "m"),
    ],
    # Chargeurs : sous le puits de chargeur, vers le bas
    "magazine_24": [box([-1, -5, -0.6], [1, 0, 0.6], "l"), box([-1.05, -5.3, -0.65], [1.05, -5, 0.65], "k")],
    "magazine_36": [box([-1, -7, -0.6], [1, 0, 0.6], "l"), box([-1.05, -7.3, -0.65], [1.05, -7, 0.65], "k")],
    "drum_magazine": [
        box([-0.8, -1.5, -0.6], [0.8, 0, 0.6], "k"),
        box([-2.6, -6, -1.2], [2.6, -1, 1.2], "k"),
        box([-2.6, -6, -1.2], [2.6, -1, 1.2], "k", ("z", 45, [0, -3.5, 0])),
        box([-1, -4.5, -1.3], [1, -2.5, 1.3], "l"),
    ],
}

# Position de l'arme 3D selon l'affichage (valeurs réglées à partir de captures en jeu)
DISPLAY_3D = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 1.5, -4], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, 100, 0], "translation": [1, 4, -2], "scale": [0.9, 0.9, 0.9]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}

# Couleurs des icônes 2D (doivent correspondre à GUN_COLORS de generate_textures.py) -> clé de palette
ICON_KEYS = {
    "#2b2f36": "k", "#4a505a": "m", "#6f7782": "l", "#9aa3ad": "l",
    "#5a3e1b": "w", "#8a6233": "W", "#4b5a2a": "t", "#6b7d3a": "T", "#353f1d": "u",
}


def recolor_icon(src, skin):
    img = Image.open(src).convert("RGBA")
    out = img.copy()
    lookup = {hexc(k)[:3]: v for k, v in ICON_KEYS.items()}
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            key = lookup.get((r, g, b))
            if a and key:
                c = skin_color(skin, key, x, y, seed=1)
                if (r, g, b) == hexc("#9aa3ad")[:3]:  # reflet : version plus claire
                    c = tuple(min(255, v + 40) for v in c[:3]) + (255,)
                out.putpixel((x, y), c)
    return out


def separate(base):
    return {
        "loader": "neoforge:separate_transforms",
        "gui_light": "front",
        "base": {"parent": f"{base}_3d"},
        "perspectives": {"gui": {"parent": f"{base}_icon"}, "fixed": {"parent": f"{base}_icon"}},
    }


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def main():
    models = os.path.join(ASSETS, "models/item")
    textures = os.path.join(ASSETS, "textures/item")
    os.makedirs(os.path.join(textures, "skins"), exist_ok=True)

    for skin in SKINS:
        palette_image(skin).save(os.path.join(textures, f"gun_palette_{skin}.png"))

    write(os.path.join(models, "gun_3d.json"), {
        "textures": {"p": f"{MOD}:item/gun_palette_default", "particle": f"{MOD}:item/gun_palette_default"},
        "display": DISPLAY_3D,
    })
    write(os.path.join(models, "parts/part_base.json"), {
        "textures": {"p": f"{MOD}:item/gun_palette_default", "particle": f"{MOD}:item/gun_palette_default"}})

    for name, build in GUNS.items():
        body, mag = build()
        write(os.path.join(models, f"{name}_3d.json"), {"parent": f"{MOD}:item/gun_3d", "elements": body})
        write(os.path.join(models, f"{name}_mag.json"), {"parent": f"{MOD}:item/parts/part_base", "elements": mag})
        icon_src = os.path.join(textures, f"{name}.png")
        for skin in SKINS:
            palette = f"{MOD}:item/gun_palette_{skin}"
            base = f"{MOD}:item/skins/{name}_{skin}"
            if skin == "default":
                icon = f"{MOD}:item/{name}"
            else:
                recolor_icon(icon_src, skin).save(os.path.join(textures, "skins", f"{name}_{skin}.png"))
                icon = f"{MOD}:item/skins/{name}_{skin}"
            write(os.path.join(models, "skins", f"{name}_{skin}_3d.json"),
                  {"parent": f"{MOD}:item/{name}_3d", "textures": {"p": palette, "particle": palette}})
            write(os.path.join(models, "skins", f"{name}_{skin}_mag.json"),
                  {"parent": f"{MOD}:item/{name}_mag", "textures": {"p": palette, "particle": palette}})
            write(os.path.join(models, "skins", f"{name}_{skin}_icon.json"),
                  {"parent": "minecraft:item/generated", "textures": {"layer0": icon}})
            # 3D par défaut, icône 2D dans l'inventaire et les cadres
            write(os.path.join(models, "skins", f"{name}_{skin}.json"), separate(base))
        # Modèle de l'item = skin par défaut (le jeu choisit ensuite le bon skin et ajoute les accessoires)
        write(os.path.join(models, f"{name}.json"), separate(f"{MOD}:item/skins/{name}_default"))

    for name, elements in ACCESSORIES.items():
        write(os.path.join(models, "parts", f"{name}.json"), {"parent": f"{MOD}:item/parts/part_base", "elements": elements})
        write(os.path.join(models, f"{name}.json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})

    print(f"{len(GUNS)} armes x {len(SKINS)} skins + {len(ACCESSORIES)} accessoires générés")


if __name__ == "__main__":
    main()

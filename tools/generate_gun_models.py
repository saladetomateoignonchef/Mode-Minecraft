#!/usr/bin/env python3
"""
Génère les modèles 3D des armes à feu, leurs icônes, leurs skins et les accessoires.

Utilisation (depuis la racine du projet, APRÈS tools/generate_textures.py) :
    python3 tools/generate_textures.py
    python3 tools/generate_gun_models.py

Principe :
- La forme 3D de chaque arme est décrite dans tools/gun_designs.py (canon vers +X, haut +Y, centrée sur Z = 8).
- Le chargeur est séparé du corps : quand on monte un chargeur (24, 36, camembert), celui d'origine est masqué.
- Les points de fixation des accessoires sont écrits dans assets/modeminecraft/gun_mounts.json (lu par le jeu).
- Les icônes 2D sont calculées automatiquement à partir du modèle 3D vu de profil, pour chaque skin.
- Chaque skin = une palette de couleurs (SKINS). Les camouflages utilisent plusieurs couleurs par case.
- Les accessoires (ACCESSORIES) sont dessinés autour de l'origine ; le jeu les place sur les points de fixation.
"""
import json
import math
import os
import random
import zlib

from PIL import Image, ImageDraw

from gun_designs import GUNS, KEYS, box

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
MOD = "modeminecraft"

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
OUTLINE = (18, 20, 23, 255)


def element_key(el):
    """Retrouve la couleur (clé de palette) d'un pavé à partir de ses UV."""
    u, v = el["faces"]["south"]["uv"][:2]
    return KEYS[(v // 4) * 4 + u // 4]


def element_polygon(el):
    """Contour du pavé vu de profil (plan X/Y), en tenant compte d'une rotation autour de Z."""
    (x0, y0, _), (x1, y1, _) = el["from"], el["to"]
    pts = [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
    rot = el.get("rotation")
    if rot and rot["axis"] == "z" and rot["angle"]:
        ox, oy, _ = rot["origin"]
        a = math.radians(rot["angle"])
        pts = [(ox + (x - ox) * math.cos(a) - (y - oy) * math.sin(a),
                oy + (x - ox) * math.sin(a) + (y - oy) * math.cos(a)) for x, y in pts]
    return pts


def render_icon(elements, skin, size=16):
    """Icône 2D : le modèle 3D vu de profil, dessiné en pixel art avec contour."""
    polys = [(el["to"][2], element_polygon(el), element_key(el)) for el in elements]
    xs = [x for _, p, _ in polys for x, _ in p]
    ys = [y for _, p, _ in polys for _, y in p]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    scale = min((size - 2) / (max(xs) - min(xs)), (size - 2) / (max(ys) - min(ys)))

    keys = [[None] * size for _ in range(size)]
    shade = [[0] * size for _ in range(size)]
    for _, pts, key in sorted(polys, key=lambda p: p[0]):  # du fond vers l'avant
        mask = Image.new("1", (size, size), 0)
        ImageDraw.Draw(mask).polygon([((x - cx) * scale + size / 2 - 0.5, size / 2 - 0.5 - (y - cy) * scale)
                                      for x, y in pts], fill=1)
        cells = [(x, y) for y in range(size) for x in range(size) if mask.getpixel((x, y))]
        if not cells:
            continue
        top = {}
        bottom = {}
        for x, y in cells:
            top[x] = min(top.get(x, y), y)
            bottom[x] = max(bottom.get(x, y), y)
        for x, y in cells:
            keys[y][x] = key
            shade[y][x] = 1 if y == top[x] else (-1 if y == bottom[x] and bottom[x] - top[x] >= 2 else 0)

    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            key = keys[y][x]
            if key is None:
                if any(0 <= x + dx < size and 0 <= y + dy < size and keys[y + dy][x + dx]
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    img.putpixel((x, y), OUTLINE)
                continue
            c = skin_color(skin, key, x, y, seed=1)
            delta = 35 * shade[y][x]
            img.putpixel((x, y), tuple(max(0, min(255, v + delta)) for v in c[:3]) + (255,))
    return img


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

    mounts = {}
    for name, design in sorted(GUNS.items()):
        gun = design()
        mounts[name] = {"mounts": gun.mounts, "mag_needs_ammo": gun.flags.get("mag_needs_ammo", False)}
        write(os.path.join(models, f"{name}_3d.json"), {"parent": f"{MOD}:item/gun_3d", "elements": gun.body})
        write(os.path.join(models, f"{name}_mag.json"), {"parent": f"{MOD}:item/parts/part_base", "elements": gun.mag})
        for skin in SKINS:
            palette = f"{MOD}:item/gun_palette_{skin}"
            base = f"{MOD}:item/skins/{name}_{skin}"
            icon_img = render_icon(gun.body + gun.mag, skin)
            if skin == "default":
                icon_img.save(os.path.join(textures, f"{name}.png"))
                icon = f"{MOD}:item/{name}"
            else:
                icon_img.save(os.path.join(textures, "skins", f"{name}_{skin}.png"))
                icon = f"{MOD}:item/skins/{name}_{skin}"
            write(os.path.join(models, "skins", f"{name}_{skin}_3d.json"),
                  {"parent": f"{MOD}:item/{name}_3d", "textures": {"p": palette, "particle": palette}})
            write(os.path.join(models, "skins", f"{name}_{skin}_mag.json"),
                  {"parent": f"{MOD}:item/{name}_mag", "textures": {"p": palette, "particle": palette}})
            write(os.path.join(models, "skins", f"{name}_{skin}_icon.json"),
                  {"parent": "minecraft:item/generated", "textures": {"layer0": icon}})
            write(os.path.join(models, "skins", f"{name}_{skin}.json"), separate(base))
        # Modèle de l'item = skin par défaut (le jeu choisit ensuite le bon skin et ajoute les accessoires)
        write(os.path.join(models, f"{name}.json"), separate(f"{MOD}:item/skins/{name}_default"))

    write(os.path.join(ASSETS, "gun_mounts.json"), mounts)

    for name, elements in ACCESSORIES.items():
        write(os.path.join(models, "parts", f"{name}.json"), {"parent": f"{MOD}:item/parts/part_base", "elements": elements})
        write(os.path.join(models, f"{name}.json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})

    print(f"{len(GUNS)} armes x {len(SKINS)} skins + {len(ACCESSORIES)} accessoires générés")


if __name__ == "__main__":
    main()

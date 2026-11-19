#!/usr/bin/env python3
"""
Génère les modèles 3D des armes à feu, leurs icônes, leurs skins et les accessoires.

Utilisation (depuis la racine du projet, APRÈS tools/generate_textures.py) :
    python3 tools/generate_textures.py
    python3 tools/generate_gun_models.py

Principe :
- La forme 3D de chaque arme est décrite dans tools/gun_designs.py (canon vers +X, haut +Y, centrée sur Z = 8).
- Le chargeur est séparé du corps : quand on monte un chargeur (24, 36, camembert), celui d'origine est masqué.
- La pièce mobile (culasse, verrou, pompe, canons du minigun) est aussi séparée : le jeu l'anime au tir.
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

from attachment_designs import ACCESSORIES, LENS_COLORS
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
    "e": "#0e4a57",  # sculk (silencieux à écho)
    "o": "#1d1230",  # obsidienne
    "q": "#9fd8c4",  # plastique de bouteille
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


# Matière de chaque couleur : détermine la texture dessinée (métal brossé, bois veiné...)
MATERIAL = {"k": "metal", "m": "metal", "l": "metal", "G": "metal", "g": "metal",
            "w": "wood", "W": "wood", "t": "polymer", "T": "polymer", "u": "polymer",
            "d": "flat", "s": "glass", "r": "gem", "e": "gem", "o": "gem", "q": "glass"}
CELL = 16  # taille d'une case de la palette, en pixels (texture 64x64)


def _noise(seed, x, y, cell=4):
    """Bruit doux (interpolé) entre -1 et 1 : sert aux camouflages et aux variations de matière."""
    def rnd(ix, iy):
        return random.Random(zlib.crc32(repr((seed, ix, iy)).encode())).uniform(-1, 1)
    fx, fy = x / cell, y / cell
    ix, iy = int(fx), int(fy)
    tx, ty = fx - ix, fy - iy
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    top = rnd(ix, iy) * (1 - tx) + rnd(ix + 1, iy) * tx
    bottom = rnd(ix, iy + 1) * (1 - tx) + rnd(ix + 1, iy + 1) * tx
    return top * (1 - ty) + bottom * ty


def _scale(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def material_pixel(skin, key, x, y, size=CELL, edges=True):
    """Couleur d'un pixel de la case (x, y dans [0, size[) pour une clé de palette et un skin."""
    value = SKINS[skin].get(key, DEFAULT[key])
    if isinstance(value, list):
        # Camouflage : chaque couleur occupe les zones où "son" bruit est le plus fort
        scores = [_noise((skin, key, i), x, y, cell=size / 3) for i in range(len(value))]
        base = hexc(value[scores.index(max(scores))])
    else:
        base = hexc(value)
    mat = MATERIAL[key]
    rng = random.Random(zlib.crc32(repr((skin, key, x, y)).encode()))
    f = 1.0
    if mat == "metal":
        streak = random.Random(zlib.crc32(repr((key, "row", y)).encode())).uniform(-0.05, 0.05)
        f = 1 + streak + rng.uniform(-0.03, 0.03)
        if y == 1:
            f *= 1.12  # reflet sous l'arête du haut
    elif mat == "wood":
        f = 0.92 + 0.12 * math.sin(y * 1.4 + 0.8 * math.sin(x * 0.35 + y * 0.2)) + rng.uniform(-0.03, 0.03)
    elif mat == "polymer":
        f = 1 + 0.06 * _noise((key, "grain"), x, y, cell=2) + rng.uniform(-0.03, 0.03)
    elif mat == "glass":
        f = 1.25 - 0.5 * (x + y) / (2 * size)
        if abs(x - y) <= 1 and x < size * 0.6:
            f = 1.6
    elif mat == "gem":
        f = 1.25 if (x + y) % 8 < 3 else (0.8 if (x - y) % 8 < 2 else 1.0)
    if edges and (x == 0 or y == 0 or x == size - 1 or y == size - 1):
        f *= 0.72  # arêtes plus sombres : chaque pavé ressort mieux
    c = _scale(base, f)
    if key == "s" and not skin.startswith("lens_"):
        c = c[:3] + (95,)  # vitre transparente : on voit le réticule à travers en visant
    return c


def palette_image(skin):
    """Palette 64x64 : 13 cases de 16x16 pixels, une par couleur, avec la texture de la matière."""
    img = Image.new("RGBA", (4 * CELL, 4 * CELL), (0, 0, 0, 0))
    for i, key in enumerate(KEYS):
        cx, cy = (i % 4) * CELL, (i // 4) * CELL
        for y in range(CELL):
            for x in range(CELL):
                img.putpixel((cx + x, cy + y), material_pixel(skin, key, x, y))
    return img


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


def render_icon(elements, skin, size=32):
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
            c = material_pixel(skin, key, x % CELL, y % CELL, edges=False)
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
        "render_type": "minecraft:translucent",
        "display": DISPLAY_3D,
    })
    write(os.path.join(models, "parts/part_base.json"), {
        "textures": {"p": f"{MOD}:item/gun_palette_default", "particle": f"{MOD}:item/gun_palette_default"}})

    mounts = {}
    for name, design in sorted(GUNS.items()):
        gun = design()
        # Rail latéral (laser) : sur le flanc gauche du garde-main (côté visible en vue à la 1re personne)
        if "grip" in gun.mounts and "side_rail" not in gun.mounts:
            gx, gy, _ = gun.mounts["grip"]
            gun.mounts["side_rail"] = [round(gx - 0.5, 3), round(gy + 1.0, 3), 7.15]
        mounts[name] = {"mounts": gun.mounts, "mag_needs_ammo": gun.flags.get("mag_needs_ammo", False)}
        if gun.slide:
            mounts[name]["slide"] = gun.slide_info
        write(os.path.join(models, f"{name}_3d.json"), {"parent": f"{MOD}:item/gun_3d", "elements": gun.body})
        write(os.path.join(models, f"{name}_mag.json"), {"parent": f"{MOD}:item/parts/part_base", "elements": gun.mag})
        # Pièce mobile (culasse, pompe, canons...) : animée par le jeu au moment du tir
        write(os.path.join(models, f"{name}_slide.json"), {"parent": f"{MOD}:item/parts/part_base", "elements": gun.slide})
        # Poignée et crosse d'origine : masquées quand un accessoire les remplace
        for part, elements in gun.parts.items():
            write(os.path.join(models, f"{name}_{part}.json"), {"parent": f"{MOD}:item/parts/part_base", "elements": elements})
        for skin in SKINS:
            palette = f"{MOD}:item/gun_palette_{skin}"
            base = f"{MOD}:item/skins/{name}_{skin}"
            icon_img = render_icon(gun.body + gun.slide + gun.mag + gun.parts["grip"] + gun.parts["stock"], skin)
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
            for part in ["slide"] + list(gun.parts):
                write(os.path.join(models, "skins", f"{name}_{skin}_{part}.json"),
                      {"parent": f"{MOD}:item/{name}_{part}", "textures": {"p": palette, "particle": palette}})
            write(os.path.join(models, "skins", f"{name}_{skin}_icon.json"),
                  {"parent": "minecraft:item/generated", "textures": {"layer0": icon}})
            write(os.path.join(models, "skins", f"{name}_{skin}.json"), separate(base))
        # Modèle de l'item = skin par défaut (le jeu choisit ensuite le bon skin et ajoute les accessoires)
        write(os.path.join(models, f"{name}.json"), separate(f"{MOD}:item/skins/{name}_default"))

    write(os.path.join(ASSETS, "gun_mounts.json"), mounts)

    # Lasers : une palette par couleur (la vitre "s" prend la couleur du laser)
    for name, color in LENS_COLORS.items():
        SKINS[f"lens_{name}"] = {"s": color}
        palette_image(f"lens_{name}").save(os.path.join(textures, f"gun_palette_lens_{name}.png"))

    for name, elements in ACCESSORIES.items():
        part = {"parent": f"{MOD}:item/parts/part_base", "elements": elements}
        if name in LENS_COLORS:
            pal = f"{MOD}:item/gun_palette_lens_{name}"
            part["textures"] = {"p": pal, "particle": pal}
        write(os.path.join(models, "parts", f"{name}.json"), part)
        write(os.path.join(models, f"{name}.json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        # Icône calculée à partir du modèle 3D (vue de profil)
        if elements:
            render_icon(elements, f"lens_{name}" if name in LENS_COLORS else "default").save(os.path.join(textures, f"{name}.png"))
        else:  # "sans crosse" : on dessine une crosse barrée
            render_icon(ACCESSORIES["buffer_tube"], "default").save(os.path.join(textures, f"{name}.png"))

    print(f"{len(GUNS)} armes x {len(SKINS) - len(LENS_COLORS)} skins + {len(ACCESSORIES)} accessoires générés")


if __name__ == "__main__":
    main()

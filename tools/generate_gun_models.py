#!/usr/bin/env python3
"""
Génère les modèles 3D des armes à feu (+ la texture "palette" qu'ils utilisent).

Utilisation (depuis la racine du projet) :
    python3 tools/generate_gun_models.py

Principe :
- Chaque arme est un assemblage de pavés (comme dans Blockbench), décrits plus bas dans GUNS.
- Repère du modèle : le canon pointe vers +X, le haut est +Y, l'arme est centrée sur Z = 8.
- Dans l'inventaire (gui) et dans les cadres (fixed), on garde l'icône 2D ;
  en main et au sol, c'est le modèle 3D qui est affiché (chargeur "neoforge:separate_transforms").

Astuce : tu peux ouvrir les fichiers *_3d.json générés dans Blockbench pour les retoucher à la main
(mais relancer ce script écraserait tes retouches).
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
MOD = "modeminecraft"

# ---------- Palette : une couleur par case de 4x4 pixels ----------
PALETTE = {
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
KEYS = list(PALETTE.keys())


def cell_uv(color):
    i = KEYS.index(color)
    cx, cy = (i % 4) * 4, (i // 4) * 4
    return [cx + 1, cy + 1, cx + 3, cy + 3]


def palette_image():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i, key in enumerate(KEYS):
        h = PALETTE[key].lstrip("#")
        rgb = tuple(int(h[j:j + 2], 16) for j in (0, 2, 4)) + (255,)
        cx, cy = (i % 4) * 4, (i // 4) * 4
        for y in range(cy, cy + 4):
            for x in range(cx, cx + 4):
                img.putpixel((x, y), rgb)
    return img


def box(frm, to, color, rot=None):
    """Un pavé. rot = (axe, angle, origine) avec angle dans -45, -22.5, 0, 22.5, 45."""
    uv = cell_uv(color)
    el = {"from": frm, "to": to,
          "faces": {f: {"uv": uv, "texture": "#p"} for f in ("north", "east", "south", "west", "up", "down")}}
    if rot:
        axis, angle, origin = rot
        el["rotation"] = {"angle": angle, "axis": axis, "origin": origin}
    return el


# ---------- Les armes ----------
def pistol():
    return [
        box([3, 8, 7], [14, 10.5, 9], "m"),               # culasse
        box([3, 10.5, 7.25], [14, 11, 8.75], "l"),        # dessus de la culasse
        box([3, 7, 7.1], [13, 8, 8.9], "k"),              # carcasse
        box([13.9, 8.75, 7.6], [14.1, 9.75, 8.4], "d"),   # bouche du canon
        box([3, 2, 7.2], [6, 7, 8.8], "w", ("z", -22.5, [4.5, 7, 8])),  # poignée
        box([6, 6, 7.5], [8.5, 6.5, 8.5], "k"),           # pontet
        box([8.5, 6, 7.5], [9, 7, 8.5], "k"),
        box([7, 6.5, 7.75], [7.5, 7, 8.25], "d"),         # détente
        box([10, 7.2, 6.95], [11, 7.8, 9.05], "r"),       # incrustation rubis
        box([13, 11, 7.75], [13.5, 11.5, 8.25], "k"),     # guidon
        box([3.5, 11, 7.5], [4.5, 11.5, 8.5], "k"),       # hausse
    ]


def assault_rifle():
    return [
        box([-2, 5, 7.2], [3, 8.5, 8.8], "w"),            # crosse
        box([-2.3, 4.5, 7.1], [-2, 9, 8.9], "k"),         # plaque de couche
        box([3, 6.5, 7], [10, 9.5, 9], "m"),              # boîtier
        box([3.5, 9.5, 7.4], [9.5, 10, 8.6], "k"),        # rail
        box([4, 10, 7.5], [5, 11, 8.5], "k"),             # hausse
        box([10, 6.8, 7.1], [14.5, 9.2, 8.9], "k"),       # garde-main
        box([14.5, 7.6, 7.6], [18, 8.4, 8.4], "k"),       # canon
        box([17.5, 7.4, 7.4], [18.5, 8.6, 8.6], "l"),     # frein de bouche
        box([18.45, 7.75, 7.75], [18.55, 8.25, 8.25], "d"),
        box([13.5, 9.2, 7.75], [14, 10.5, 8.25], "k"),    # guidon
        box([4, 3, 7.3], [6, 6.5, 8.7], "k", ("z", -22.5, [5, 6.5, 8])),     # poignée
        box([7.5, 2.5, 7.4], [9.5, 6.5, 8.6], "l", ("z", 22.5, [8.5, 6.5, 8])),  # chargeur
        box([6, 5.6, 7.6], [7.4, 6, 8.4], "k"),           # pontet
        box([6.4, 6, 7.75], [6.8, 6.5, 8.25], "d"),       # détente
        box([8, 8.2, 6.95], [9.5, 8.8, 9.05], "r"),       # rubis
    ]


def shotgun():
    return [
        box([-2, 5, 7.2], [3, 8.5, 8.8], "w"),            # crosse
        box([-2.3, 4.5, 7.1], [-2, 9, 8.9], "k"),
        box([3, 7, 7], [8, 10, 9], "m"),                  # boîtier
        box([8, 8.8, 7.5], [19, 10, 8.5], "k"),           # canon
        box([18.95, 9, 7.7], [19.05, 9.8, 8.3], "d"),
        box([8, 7.4, 7.6], [17, 8.6, 8.4], "l"),          # tube magasin
        box([10, 6.8, 7.1], [14, 8.9, 8.9], "W"),         # pompe
        box([18.4, 10, 7.85], [18.8, 10.4, 8.15], "g"),   # guidon
        box([3.5, 3.5, 7.3], [5.5, 7, 8.7], "w", ("z", -22.5, [4.5, 7, 8])),  # poignée
        box([5.5, 6.4, 7.6], [7.5, 6.8, 8.4], "k"),       # pontet
        box([6, 6.8, 7.75], [6.5, 7.2, 8.25], "d"),
        box([4.5, 8.2, 6.95], [6.5, 8.8, 9.05], "r"),     # rubis
    ]


def sniper_rifle():
    return [
        box([-3, 5, 7.2], [3, 8.5, 8.8], "w"),            # crosse
        box([-1, 8.5, 7.4], [2, 9.3, 8.6], "w"),          # appui-joue
        box([-3.3, 4.5, 7.1], [-3, 9, 8.9], "k"),
        box([3, 6.5, 7.1], [9, 9, 8.9], "m"),             # boîtier
        box([6, 8, 8.9], [6.5, 8.5, 10], "l"),            # levier de culasse
        box([6, 7.6, 10], [6.8, 8.4, 10.6], "k"),
        box([9, 7.3, 7.6], [22, 8.3, 8.4], "k"),          # canon long
        box([21.5, 7.1, 7.4], [22.5, 8.5, 8.6], "m"),     # bouche
        box([22.45, 7.55, 7.75], [22.55, 8.05, 8.25], "d"),
        box([3, 10.5, 7.2], [11, 12, 8.8], "k"),          # lunette
        box([2, 10.2, 6.9], [3.5, 12.3, 9.1], "k"),       # oculaire
        box([10, 10.2, 6.9], [12, 12.3, 9.1], "k"),       # objectif
        box([11.95, 10.5, 7.2], [12.05, 12, 8.8], "s"),   # verre avant
        box([1.95, 10.5, 7.2], [2.05, 12, 8.8], "s"),     # verre arrière
        box([4.5, 9, 7.6], [5.5, 10.5, 8.4], "m"),        # montages
        box([8.5, 9, 7.6], [9.5, 10.5, 8.4], "m"),
        box([3.5, 3.5, 7.3], [5.5, 7, 8.7], "w", ("z", -22.5, [4.5, 7, 8])),  # poignée
        box([7, 5, 7.4], [8.5, 6.5, 8.6], "k"),           # chargeur
        box([6.5, 7, 6.95], [8, 7.6, 9.05], "r"),         # rubis
    ]


def rocket_launcher():
    return [
        box([-2, 7, 6.5], [18, 10, 9.5], "T"),            # tube
        box([-2, 6.8, 6.6], [18, 7, 9.4], "u"),           # dessous du tube
        box([-2.2, 6.8, 6.3], [-0.5, 10.2, 9.7], "r"),    # bagues rubis
        box([16.5, 6.8, 6.3], [18.2, 10.2, 9.7], "r"),
        box([18.15, 7.4, 6.9], [18.25, 9.6, 9.1], "d"),   # ouverture avant
        box([-2.25, 7.4, 6.9], [-2.15, 9.6, 9.1], "d"),   # ouverture arrière
        box([0, 5.5, 7.4], [2, 7, 8.6], "u"),             # appui d'épaule
        box([4, 3.5, 7.3], [6, 7, 8.7], "k", ("z", -22.5, [5, 7, 8])),  # poignée
        box([9, 4, 7.4], [10.5, 7, 8.6], "k"),            # poignée avant
        box([7, 10, 7.4], [9, 11.5, 8.6], "k"),           # viseur
        box([8.95, 10.3, 7.6], [9.05, 11.2, 8.4], "s"),
    ]


GUNS = {
    "pistol": pistol,
    "assault_rifle": assault_rifle,
    "shotgun": shotgun,
    "sniper_rifle": sniper_rifle,
    "rocket_launcher": rocket_launcher,
}

# Position de l'arme 3D selon l'affichage (valeurs réglées à partir de captures en jeu)
DISPLAY_3D = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 1.5, -4], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, 100, 0], "translation": [1, 4, -2], "scale": [0.9, 0.9, 0.9]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def main():
    models = os.path.join(ASSETS, "models/item")
    palette_image().save(os.path.join(ASSETS, "textures/item/gun_palette.png"))

    write(os.path.join(models, "gun_3d.json"), {
        "textures": {"p": f"{MOD}:item/gun_palette", "particle": f"{MOD}:item/gun_palette"},
        "display": DISPLAY_3D,
    })
    for name, build in GUNS.items():
        write(os.path.join(models, f"{name}_3d.json"), {"parent": f"{MOD}:item/gun_3d", "elements": build()})
        write(os.path.join(models, f"{name}_icon.json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        # Modèle final : 3D par défaut, icône 2D dans l'inventaire et les cadres
        write(os.path.join(models, f"{name}.json"), {
            "loader": "neoforge:separate_transforms",
            "gui_light": "front",
            "base": {"parent": f"{MOD}:item/{name}_3d"},
            "perspectives": {
                "gui": {"parent": f"{MOD}:item/{name}_icon"},
                "fixed": {"parent": f"{MOD}:item/{name}_icon"},
            },
        })
    print(f"{len(GUNS)} modèles 3D générés")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Modèles 3D (petits pavés, palette des armes) des grenades, explosifs posés et masque à gaz.
Utilisation : python3 tools/generate_explosives.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gun_designs import box  # noqa: E402
import voxel_bake as vb  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
MODELS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft/models/item")
MOD = "modeminecraft"


def cyl(x0, y0, x1, y1, c, cx=8, cz=8):
    """Cylindre approché (croix de deux pavés) entre y0 et y1, de rayon r = (x1 - x0) / 2."""
    r = (x1 - x0) / 2
    return [box([cx - r, y0, cz - r * 0.7], [cx + r, y1, cz + r * 0.7], c),
            box([cx - r * 0.7, y0, cz - r], [cx + r * 0.7, y1, cz + r], c)]


def spoon(c="l"):
    return [box([10.2, 6, 7.4], [10.8, 11.2, 8.6], c), box([8.6, 11, 7.4], [10.8, 11.6, 8.6], c),
            box([9.2, 11.4, 6.2], [10.6, 12.6, 6.6], "G")]   # levier + anneau de la goupille


MODELS_DEF = {
    "frag_grenade": cyl(5, 3, 11, 10, "T") + [box([5.4, 4, 5.4], [10.6, 9, 10.6], "u")] + cyl(7, 10, 9, 11.2, "m") + spoon(),
    "offensive_grenade": cyl(5.5, 2, 10.5, 10.5, "k") + cyl(7, 10.5, 9, 11.5, "m") + spoon(),
    "semtex": [box([4, 3, 6], [12, 7.5, 10], "t"), box([4.5, 7.5, 6.5], [11.5, 8, 9.5], "u"), box([7.4, 8, 7.4], [8.6, 8.6, 8.6], "r")],
    "molotov": cyl(6, 1, 10, 9, "q") + cyl(7, 9, 9, 12, "q") + [box([7.2, 12, 7.2], [8.8, 14.5, 8.8], "W")],
    "cluster_grenade": cyl(5, 3, 11, 11, "k") + [box([4.9, 5, 4.9], [11.1, 5.6, 11.1], "g"), box([4.9, 8, 4.9], [11.1, 8.6, 11.1], "g")]
                       + cyl(7, 11, 9, 12, "m") + spoon(),
    "flashbang": cyl(6, 2, 10, 12, "l") + [box([5.9, 4, 7.6], [10.1, 5, 8.4], "d"), box([5.9, 7, 7.6], [10.1, 8, 8.4], "d"),
                                          box([5.9, 10, 7.6], [10.1, 11, 8.4], "d")] + spoon("m"),
    "smoke_grenade": cyl(5.5, 2, 10.5, 11, "t") + [box([5.4, 9, 5.4], [10.6, 9.8, 10.6], "l")] + spoon(),
    "gas_grenade": cyl(5.5, 2, 10.5, 11, "T") + [box([5.4, 9, 5.4], [10.6, 9.8, 10.6], "g")] + spoon(),
    "emp_grenade": [box([5, 3, 5], [11, 9, 11], "k")] + cyl(6.5, 9, 9.5, 10, "e") + [box([4.8, 5, 7.5], [11.2, 7, 8.5], "e")],
    "decoy_grenade": [box([5, 3, 6], [11, 9, 10], "m"), box([5.5, 5, 9.95], [10.5, 8, 10.1], "d")] + spoon(),
    "impulse_grenade": [box([5, 4, 5], [11, 10, 11], "o"), box([4.8, 6, 4.8], [11.2, 8, 11.2], "e")],
    "sensor_grenade": [box([5, 2, 5], [11, 4, 11], "k"), box([7, 4, 7], [9, 5, 9], "e")],
    "bomblet": [box([6.5, 5, 6.5], [9.5, 8, 9.5], "g"), box([7.5, 8, 7.5], [8.5, 9, 8.5], "k")],
    "c4": [box([3, 0, 5], [13, 3, 11], "T"), box([3.2, 3, 5.2], [12.8, 3.2, 10.8], "W"), box([6, 3, 7], [10, 4.6, 9], "k"),
           box([6.5, 4.6, 7.6], [7.2, 5.4, 8.4], "r"), box([8.8, 4.6, 7.6], [9.5, 5.4, 8.4], "t")],
    "claymore_mine": [box([2, 2, 7.2], [14, 9, 8.8], "t"), box([2.5, 3, 8.8], [13.5, 8, 9.0], "u"),
                      box([3, 0, 7.6], [3.6, 2, 8.4], "k"), box([12.4, 0, 7.6], [13, 2, 8.4], "k"), box([7.5, 9, 7.5], [8.5, 10.5, 8.5], "k")],
    "mortar": [box([3, 0, 3], [13, 1, 13], "k"), box([6.5, 1, 6.5], [9.5, 15, 9.5], "m", ("x", -22.5, [8, 1, 8])),
               box([6, 0.5, 6], [10, 2, 10], "k"), box([7.6, 1, 11], [8.4, 9, 11.8], "k", ("x", 22.5, [8, 9, 11]))],
    "mortar_shell": cyl(6.5, 3, 9.5, 11, "t") + cyl(7.2, 11, 8.8, 13, "u") + [box([6, 1, 7.6], [10, 3, 8.4], "k"), box([7.6, 1, 6], [8.4, 3, 10], "k")],
    "detonator": [box([5, 1, 6.5], [11, 11, 9.5], "k"), box([7, 11, 7.2], [9, 12, 8.8], "r"), box([9.5, 11, 8], [10, 15, 8.5], "m"),
                  box([6, 7, 9.5], [10, 9, 9.6], "g")],
    "ammo_box_50": [box([2, 0, 4], [14, 8, 12], "t"), box([1.8, 7, 3.8], [14.2, 8, 12.2], "u"), box([6, 8, 7.4], [10, 9, 8.6], "k"),
                    box([2.5, 3, 3.9], [7, 6, 4], "g")],
    "ammo_box_792": [box([3, 0, 5], [13, 6, 11], "m"), box([3, 6, 5], [13, 8, 11], "k"), box([4, 8, 7.5], [12, 8.6, 8.5], "k"),
                     box([2.8, 2, 4.9], [13.2, 3, 11.1], "g")],
    "ammo_belt_303": [box([2, 0, 6], [14, 1, 10], "W")] + [box([3 + i * 1.6, 1, 6.5], [4 + i * 1.6, 2.2, 9.5], "g") for i in range(7)]
                     + [box([3 + i * 1.6, 2.2, 7.6], [4 + i * 1.6, 2.8, 8.4], "G") for i in range(7)],
    "flak_magazine_20mm": [box([5, 0, 6], [11, 13, 10], "T"), box([5.5, 13, 6.5], [10.5, 14.5, 9.5], "g"), box([7, 14.5, 7.4], [9, 15.5, 8.6], "G")],
    "ammo_box_40mm": [box([1, 0, 3], [15, 9, 13], "t"), box([0.8, 8, 2.8], [15.2, 9, 13.2], "u"), box([6, 9, 7.4], [10, 10, 8.6], "k"),
                      box([1.5, 3, 2.9], [6, 6, 3], "g")],
    "wrench": [box([7, 0, 7.4], [9, 11, 8.6], "l"), box([5, 11, 7.4], [11, 13, 8.6], "l"), box([5, 13, 7.4], [6.6, 16, 8.6], "l"),
               box([9.4, 13, 7.4], [11, 15, 8.6], "l"), box([7.2, 1, 7.3], [8.8, 6, 8.7], "W")],
    "gas_mask": [box([4, 5, 6], [12, 12, 9], "k"), box([5, 9, 5.8], [7.5, 11, 6.0], "s"), box([8.5, 9, 5.8], [11, 11, 6.0], "s"),
                 box([6.5, 4, 3], [9.5, 7, 6], "m"), box([6.2, 4.3, 2.6], [9.8, 6.7, 3], "d"), box([3.6, 6, 6.5], [12.4, 7, 12], "d")],
}

BASE = {
    "textures": {"p": f"{MOD}:item/gun_palette_default", "particle": f"{MOD}:item/gun_palette_default"},
    "display": {
        "gui": {"rotation": [25, 135, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 1], "scale": [0.4, 0.4, 0.4]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4.5, 0], "scale": [0.42, 0.42, 0.42]},
        "head": {"rotation": [0, 180, 0], "translation": [0, -6.5, -5.2], "scale": [1.05, 1.05, 1.05]},
    },
}


def main():
    with open(os.path.join(MODELS, "gadget_base.json"), "w", encoding="utf-8") as f:
        json.dump(BASE, f, indent=2)
    from generate_gun_models import painter
    paint = painter("default")
    textures = os.path.join(ROOT, "src/main/resources/assets/modeminecraft/textures/item/gadget")
    os.makedirs(textures, exist_ok=True)
    for name, elements in MODELS_DEF.items():
        # texture détaillée propre à chaque objet (voir voxel_bake.py)
        baked = vb.bake({"m": elements}, skip_chamfer=("s", "q", "d"))
        vb.paint(baked, paint).save(os.path.join(textures, f"{name}.png"))
        tex = f"{MOD}:item/gadget/{name}"
        with open(os.path.join(MODELS, f"{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"parent": f"{MOD}:item/gadget_base", "textures": {"p": tex, "particle": tex},
                       "elements": vb.elements(baked, "m")}, f, separators=(",", ":"))
    print(f"{len(MODELS_DEF)} modèles d'explosifs")


if __name__ == "__main__":
    main()

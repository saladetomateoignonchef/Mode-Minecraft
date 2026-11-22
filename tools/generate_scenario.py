#!/usr/bin/env python3
"""
Scénario « Course à l'arme nucléaire » : lits superposés militaires (US : matelas bleu, cadre gris ; URSS : matelas
kaki, cadre vert olive), casier personnel des dortoirs. Textures, modèles, états de blocs, objets, butin.
Utilisation : python3 tools/generate_scenario.py
"""
import os
import random

from PIL import Image

import generate_police_assets as gp
from generate_police_assets import write, save, block_loot, metal, shade, face_all, ASSETS, MOD


def fabric(seed, base, stripe=None):
    r = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = r.randint(-7, 7) + (-16 if y in (0, 15) or x in (0, 15) else 0)
            c = base
            if stripe and (x + y) % 6 == 0:
                c = stripe
            img.putpixel((x, y), tuple(max(0, min(255, k + v)) for k in c) + (255,))
    return img


def bunk(name, frame_rgb, mat_rgb, pillow_rgb):
    save(metal(sum(map(ord, name)), frame_rgb), f"block/{name}_frame.png")
    save(fabric(sum(map(ord, name)) + 3, mat_rgb), f"block/{name}_mattress.png")
    save(fabric(7, pillow_rgb), f"block/{name}_pillow.png")
    f, m, p = "#frame", "#mat", "#pillow"
    el = []
    # 4 montants tubulaires sur deux niveaux (jusqu'à 1,75 bloc)
    for x, z in ((0, 0), (14, 0), (0, 14), (14, 14)):
        el.append({"from": [x, 0, z], "to": [x + 2, 28, z + 2], "faces": face_all(f)})
    for y in (5, 20):
        el.append({"from": [0, y, 0], "to": [16, y + 2, 2], "faces": face_all(f)})          # longerons
        el.append({"from": [0, y, 14], "to": [16, y + 2, 16], "faces": face_all(f)})
        el.append({"from": [0, y, 2], "to": [2, y + 2, 14], "faces": face_all(f)})
        el.append({"from": [14, y, 2], "to": [16, y + 2, 14], "faces": face_all(f)})
        el.append({"from": [1, y + 2, 1], "to": [15, y + 4, 15], "faces": face_all(m)})      # matelas
        el.append({"from": [3, y + 4, 1], "to": [13, y + 6, 5], "faces": face_all(p)})       # oreiller (côté tête = nord)
    el.append({"from": [2, 26, 0], "to": [14, 27, 1], "faces": face_all(f)})                 # garde-corps du haut
    el.append({"from": [2, 23, 15], "to": [14, 24, 16], "faces": face_all(f)})
    for y in (9, 13, 17):                                                                       # échelle au pied
        el.append({"from": [6, y, 15], "to": [10, y + 1, 16], "faces": face_all(f)})
    model = {"parent": "minecraft:block/block", "textures": {"particle": f"{MOD}:block/{name}_frame",
                                                             "frame": f"{MOD}:block/{name}_frame",
                                                             "mat": f"{MOD}:block/{name}_mattress",
                                                             "pillow": f"{MOD}:block/{name}_pillow"}, "elements": el}
    write(os.path.join(ASSETS, f"models/block/{name}.json"), model)
    variants = {}
    for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        v = {"model": f"{MOD}:block/{name}"}
        if y:
            v["y"] = y
        variants[f"facing={d}"] = v
    write(os.path.join(ASSETS, f"blockstates/{name}.json"), {"variants": variants})
    write(os.path.join(ASSETS, f"models/item/{name}.json"), {"parent": f"{MOD}:block/{name}"})
    block_loot(name)


def locker():
    base = (88, 98, 64)                                                           # vert olive
    img = metal(51, base)
    shade(img, 0, 0, 16, 1, -30)
    shade(img, 0, 15, 16, 16, -30)
    shade(img, 7, 1, 9, 15, -22)                                                  # séparation des deux portes
    for x0 in (2, 10):
        for y in (3, 5, 7):
            shade(img, x0, y, x0 + 4, y + 1, -26)                                 # ouïes d'aération
        shade(img, x0, 10, x0 + 4, 12, 40)                                        # porte-étiquette
    px = img.load()
    for x in (6, 9):
        px[x, 9] = (40, 40, 40, 255)                                              # poignées / cadenas
        px[x, 10] = (60, 60, 60, 255)
    save(img, "block/barracks_locker_front.png")
    side = metal(52, base)
    shade(side, 0, 0, 1, 16, -20)
    save(side, "block/barracks_locker_side.png")
    save(metal(53, (78, 88, 56)), "block/barracks_locker_top.png")
    write(os.path.join(ASSETS, "models/block/barracks_locker.json"), {"parent": "minecraft:block/orientable", "textures": {
        "front": f"{MOD}:block/barracks_locker_front", "side": f"{MOD}:block/barracks_locker_side", "top": f"{MOD}:block/barracks_locker_top"}})
    variants = {}
    for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for locked in ("true", "false"):
            v = {"model": f"{MOD}:block/barracks_locker"}
            if y:
                v["y"] = y
            variants[f"facing={d},locked={locked}"] = v
    write(os.path.join(ASSETS, "blockstates/barracks_locker.json"), {"variants": variants})
    write(os.path.join(ASSETS, "models/item/barracks_locker.json"), {"parent": f"{MOD}:block/barracks_locker"})
    block_loot("barracks_locker")


if __name__ == "__main__":
    bunk("military_bunk_bed_us", (150, 156, 164), (64, 92, 150), (226, 230, 236))
    bunk("military_bunk_bed_soviet", (84, 96, 66), (116, 112, 70), (204, 196, 170))
    locker()
    print("ok")

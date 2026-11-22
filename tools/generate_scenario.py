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


# ------------------------------------------------------------------ porte à contrôle d'accréditation (grades)
def rank_door():
    import json as _json
    import zipfile
    bottom = metal(61, (58, 62, 70))
    shade(bottom, 0, 0, 16, 1, -25)
    shade(bottom, 0, 0, 1, 16, -25)
    shade(bottom, 15, 0, 16, 16, -25)
    shade(bottom, 3, 2, 13, 3, 30)                                                # bande de grade (dorée)
    px = bottom.load()
    for y in range(4, 16, 4):
        shade(bottom, 2, y, 14, y + 1, -18)
    save(bottom, "block/rank_security_door_bottom.png")
    top = metal(62, (58, 62, 70))
    shade(top, 0, 15, 16, 16, -25)
    shade(top, 0, 0, 1, 16, -25)
    shade(top, 15, 0, 16, 16, -25)
    shade(top, 3, 3, 13, 9, 70)                                                   # hublot
    px = top.load()
    for x in range(4, 12):
        for y in range(4, 8):
            px[x, y] = (120, 160, 190, 255)
    shade(top, 11, 11, 14, 15, -40)                                               # lecteur de badge
    px[12, 12] = (40, 220, 60, 255)
    px[12, 13] = (220, 40, 40, 255)
    save(top, "block/rank_security_door_top.png")
    item = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    item.paste(bottom.resize((8, 8)), (4, 8))
    item.paste(top.resize((8, 8)), (4, 0))
    save(item, "item/rank_security_door.png")
    with zipfile.ZipFile(gp.VANILLA) as z:
        bs = _json.loads(z.read("assets/minecraft/blockstates/iron_door.json"))
    for part in ("bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open", "top_left", "top_left_open", "top_right", "top_right_open"):
        write(os.path.join(ASSETS, f"models/block/rank_security_door_{part}.json"), {"parent": f"minecraft:block/door_{part}", "textures": {
            "bottom": f"{MOD}:block/rank_security_door_bottom", "top": f"{MOD}:block/rank_security_door_top"}})
    variants = {}
    for key, v in bs["variants"].items():
        v2 = dict(v)
        v2["model"] = v["model"].replace("minecraft:block/iron_door_", f"{MOD}:block/rank_security_door_")
        for rank in range(1, 6):
            variants[f"{key},rank={rank}"] = v2
    write(os.path.join(ASSETS, "blockstates/rank_security_door.json"), {"variants": variants})
    write(os.path.join(ASSETS, "models/item/rank_security_door.json"), {"parent": "minecraft:item/generated",
                                                                         "layers": None} if False else
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/rank_security_door"}})
    gp.write(os.path.join(gp.DATA, "loot_table/blocks/rank_security_door.json"), {"type": "minecraft:block", "pools": []})


# ------------------------------------------------------------------ objets du scénario
def items():
    import generate_military as gm
    from generate_military import Icon, hexc, shade as sh
    i = Icon("uranium_canister")
    i.rect(4, 2, 11, 14, hexc("#5a6068"), noise=0.05)
    i.rect(4, 2, 11, 3, hexc("#3a3e44"), bevel=False)
    i.rect(4, 13, 11, 14, hexc("#3a3e44"), bevel=False)
    i.rect(5, 6, 10, 9, hexc("#e0c020"), bevel=False)                               # trèfle radioactif
    i.px(7, 7, hexc("#202020")), i.px(8, 7, hexc("#202020")), i.px(7, 8, hexc("#202020"))
    i.px(6, 11, hexc("#80ff60")), i.px(9, 11, hexc("#80ff60"))                       # lueur verte
    i.save("uranium_canister")
    i = Icon("nuclear_guidance_unit")
    i.rect(2, 4, 13, 12, hexc("#30343a"), noise=0.04)
    i.rect(4, 6, 8, 9, hexc("#40d070"), bevel=False)                                 # écran d'altimètre
    i.px(5, 7, hexc("#c0ffd0"))
    for x in (10, 12):
        i.px(x, 6, hexc("#e0c040")), i.px(x, 9, hexc("#e04040"))
    i.rect(2, 12, 13, 13, hexc("#b0b4b8"), bevel=False)
    i.save("nuclear_guidance_unit")
    i = Icon("nuclear_neutron_reflector")
    for y in range(3, 14):
        for x in range(3, 14):
            if (x - 8) ** 2 + (y - 8) ** 2 <= 26:
                i.px(x, y, hexc("#9aa4ae") if (x + y) % 3 else hexc("#c8d0d8"))
    i.rect(7, 7, 9, 9, hexc("#40484f"), bevel=False)
    i.save("nuclear_neutron_reflector")
    i = Icon("nuclear_implosion_lenses")
    for k, (x, y) in enumerate(((3, 3), (8, 3), (3, 8), (8, 8))):
        i.rect(x, y, x + 4, y + 4, hexc("#c06830") if k % 2 else hexc("#d8a040"), bevel=True)
    i.save("nuclear_implosion_lenses")
    i = Icon("nuclear_krytron_trigger")
    i.rect(5, 2, 10, 12, hexc("#d8e0e8"), noise=0.03)
    i.rect(6, 4, 9, 8, hexc("#80c8ff"), bevel=False)                                  # tube krytron
    i.px(7, 5, hexc("#ffffff"))
    i.rect(4, 12, 11, 14, hexc("#202428"), bevel=False)
    i.px(5, 13, hexc("#e03030")), i.px(9, 13, hexc("#30e030"))
    i.save("nuclear_krytron_trigger")
    i = Icon("secret_plans")
    i.rect(2, 3, 13, 12, hexc("#d8d0b8"), noise=0.03)
    for y in (5, 7, 9):
        i.rect(4, y, 11, y, hexc("#3050a0"), bevel=False)
    i.rect(2, 3, 13, 4, hexc("#b02020"), bevel=False)                                 # tampon « secret »
    i.save("secret_plans")
    i = Icon("oil_barrel")
    i.rect(4, 2, 11, 14, hexc("#1c2a4a"), noise=0.05)                                 # fût de 200 L bleu
    for y in (2, 7, 13):
        i.rect(4, y, 11, y, hexc("#101828"), bevel=False)                           # cerclages
    i.rect(6, 4, 9, 5, hexc("#e8e8e8"), bevel=False)
    i.px(7, 9, hexc("#202020")), i.px(8, 10, hexc("#202020")), i.px(7, 11, hexc("#202020"))
    i.px(10, 3, hexc("#d0d4d8"))                                                      # bonde
    i.save("oil_barrel")
    i = Icon("strategic_bases_map")
    i.rect(1, 2, 14, 13, hexc("#cfc49a"), noise=0.04)
    i.rect(1, 2, 14, 2, hexc("#8a7a50"), bevel=False)
    i.rect(1, 13, 14, 13, hexc("#8a7a50"), bevel=False)
    for x, y, c in ((4, 5, "#2a5ad0"), (11, 4, "#c02020"), (7, 9, "#e0a020"), (12, 10, "#202020"), (3, 11, "#20a0c0")):
        i.rect(x, y, x + 1, y + 1, hexc(c), bevel=False)                               # icônes des bases
    for x in range(5, 11):
        i.px(x, 7, hexc("#6a5a30"))
    i.save("strategic_bases_map")


if __name__ == "__main__" and True:
    rank_door()
    items()
    print("ok2")

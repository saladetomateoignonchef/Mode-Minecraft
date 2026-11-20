"""
Blocs du régime nocturne des villes (phase 21) :
- city_lamp / city_neon : éclairage de ville (allumé / éteint) ;
- street_lantern : lanterne de lampadaire (posée / suspendue, allumée / éteinte) ;
- high_voltage_transformer : boîtier haute tension 3D (isolateurs, câbles, diode verte), version calcinée.
Génère blockstates, modèles de blocs et d'objets, textures "éteintes" et tables de butin.
"""
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "modeminecraft")
DATA = os.path.join(ROOT, "data", "modeminecraft")
MOD = "modeminecraft"


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def loot(name):
    write(os.path.join(DATA, "loot_table/blocks", f"{name}.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


# ------------------------------------------------------------------ textures "éteintes"
def neon_off():
    rnd = random.Random(7)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            seam = x in (5, 10) or y in (5, 10)
            v = 70 if edge else 96 if seam else 128 + rnd.randint(-6, 6)
            px[x, y] = (v - 8, v, v + 6, 255)
    img.save(os.path.join(ASSETS, "textures/block/city_neon_off.png"))


def lantern_off():
    # lanterne éteinte : même gabarit que la lanterne vanilla (cadre noir, vitres sombres, mèche éteinte)
    rnd = random.Random(3)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    metal, metal_d, glass = (52, 54, 60, 255), (34, 35, 40, 255), (58, 52, 44, 255)
    # côté (0..6 x 2..9), dessus (0..6 x 9..15), poignée (11..13 x 1..6) : on dessine des zones génériques
    for y in range(16):
        for x in range(16):
            if x < 6 and 2 <= y < 9:              # côté du corps
                frame = x in (0, 5) or y in (2, 8)
                px[x, y] = metal_d if frame else (glass[0] + rnd.randint(-5, 5), glass[1] + rnd.randint(-5, 5), glass[2], 255)
            elif x < 6 and 9 <= y < 15:           # dessus / dessous
                px[x, y] = metal if (x + y) % 3 else metal_d
            elif x < 4 and y < 2:                 # couvercle
                px[x, y] = metal
            elif 11 <= x < 14 and 1 <= y < 6:     # anse
                px[x, y] = metal_d
    img.save(os.path.join(ASSETS, "textures/block/street_lantern_off.png"))


def lamps():
    b = os.path.join(ASSETS, "models/block")
    write(os.path.join(b, "city_lamp_on.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "minecraft:block/redstone_lamp_on"}})
    write(os.path.join(b, "city_lamp.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "minecraft:block/redstone_lamp"}})
    write(os.path.join(b, "city_neon_on.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "minecraft:block/sea_lantern"}})
    write(os.path.join(b, "city_neon.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/city_neon_off"}})
    for name in ("city_lamp", "city_neon"):
        write(os.path.join(ASSETS, "blockstates", f"{name}.json"), {"variants": {
            "lit=true": {"model": f"{MOD}:block/{name}_on"}, "lit=false": {"model": f"{MOD}:block/{name}"}}})
        write(os.path.join(ASSETS, "models/item", f"{name}.json"), {"parent": f"{MOD}:block/{name}_on"})
        loot(name)
    write(os.path.join(b, "street_lantern.json"), {"parent": "minecraft:block/template_lantern",
                                                   "textures": {"lantern": f"{MOD}:block/street_lantern_off"}})
    write(os.path.join(b, "street_lantern_hanging.json"), {"parent": "minecraft:block/template_hanging_lantern",
                                                           "textures": {"lantern": f"{MOD}:block/street_lantern_off"}})
    v = {}
    for hanging in ("false", "true"):
        for lit in ("false", "true"):
            suffix = "_hanging" if hanging == "true" else ""
            model = f"minecraft:block/lantern{suffix}" if lit == "true" else f"{MOD}:block/street_lantern{suffix}"
            v[f"hanging={hanging},lit={lit}"] = {"model": model}
    write(os.path.join(ASSETS, "blockstates/street_lantern.json"), {"variants": v})
    write(os.path.join(ASSETS, "models/item/street_lantern.json"), {"parent": "minecraft:item/lantern"})
    loot("street_lantern")


# ------------------------------------------------------------------ transformateur
def box(frm, to, tex, glow=False, shade=True):
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        if f in ("north", "south"):
            uv = [frm[0], 16 - to[1], to[0], 16 - frm[1]]
        elif f in ("east", "west"):
            uv = [frm[2], 16 - to[1], to[2], 16 - frm[1]]
        else:
            uv = [frm[0], frm[2], to[0], to[2]]
        uv = [max(0, min(16, round(u, 2))) for u in uv]
        if uv[0] == uv[2]:
            uv[2] = min(16, uv[0] + 1)
        if uv[1] == uv[3]:
            uv[3] = min(16, uv[1] + 1)
        faces[f] = {"uv": uv, "texture": tex}
    e = {"from": frm, "to": to, "faces": faces}
    if glow:
        e["shade"] = False
        e["neoforge_data"] = {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}
    return e


def transformer(broken):
    body = "#burnt" if broken else "#body"
    els = [
        box([2, 0, 3], [14, 1, 13], "#base"),                       # socle
        box([2.5, 1, 3.5], [13.5, 10, 12.5], body),                 # cuve
        box([2, 10, 3], [14, 11, 13], "#base"),                     # couvercle
    ]
    for x in (3.0, 5.0, 7.0, 9.0, 11.0):                            # ailettes de refroidissement (côtés)
        els.append(box([x, 2, 2.6], [x + 0.8, 9, 3.5], body))
        els.append(box([x, 2, 12.5], [x + 0.8, 9, 13.4], body))
    for x in (4.0, 7.5, 11.0):                                      # isolateurs en céramique + bornes
        els.append(box([x, 11, 7], [x + 1.4, 13.5, 8.4], "#ceramic"))
        els.append(box([x - 0.3, 11.8, 6.7], [x + 1.7, 12.2, 8.7], "#ceramic"))
        els.append(box([x + 0.4, 13.5, 7.4], [x + 1.0, 15, 8.0], "#copper"))
    els.append(box([4.4, 14.4, 7.5], [12.0, 14.9, 7.9], "#cable"))  # câbles entre les bornes
    els.append(box([12.0, 14.4, 7.5], [12.4, 14.9, 15.9], "#cable"))
    els.append(box([12.0, 0, 15.5], [12.4, 14.9, 15.9], "#cable"))
    # plaque "danger" et diode
    els.append(box([5, 5, 3.3], [11, 8.5, 3.5], "#warning"))
    els.append(box([11.6, 8.6, 3.2], [12.6, 9.6, 3.5], "#led_off" if broken else "#led", glow=not broken))
    tex = {"body": "minecraft:block/light_gray_concrete", "burnt": "minecraft:block/blackstone",
           "base": "minecraft:block/iron_block", "ceramic": "minecraft:block/brown_glazed_terracotta",
           "copper": "minecraft:block/copper_block", "cable": "minecraft:block/black_concrete",
           "warning": "minecraft:block/yellow_concrete", "led": "minecraft:block/lime_concrete",
           "led_off": "minecraft:block/black_concrete", "particle": "minecraft:block/iron_block"}
    if broken:
        tex["ceramic"] = "minecraft:block/black_terracotta"
    return {"parent": "minecraft:block/block", "textures": tex, "elements": els}


def transformers():
    name = "high_voltage_transformer"
    write(os.path.join(ASSETS, "models/block", f"{name}.json"), transformer(False))
    write(os.path.join(ASSETS, "models/block", f"{name}_broken.json"), transformer(True))
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    v = {}
    for facing, y in rot.items():
        for broken in ("false", "true"):
            for hits in range(6):
                m = {"model": f"{MOD}:block/{name}" + ("_broken" if broken == "true" else "")}
                if y:
                    m["y"] = y
                v[f"broken={broken},facing={facing},hits={hits}"] = m
    write(os.path.join(ASSETS, "blockstates", f"{name}.json"), {"variants": v})
    write(os.path.join(ASSETS, "models/item", f"{name}.json"), {"parent": f"{MOD}:block/{name}"})
    loot(name)


if __name__ == "__main__":
    neon_off()
    lantern_off()
    lamps()
    transformers()
    print("blocs de nuit générés")

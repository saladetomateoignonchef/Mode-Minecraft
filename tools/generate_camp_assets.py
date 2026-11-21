#!/usr/bin/env python3
"""
Campements abandonnés (mise à jour 0.3) : toile de tente (bloc + escaliers), sac de couchage 3D, tente déployable,
structure « camp » (biomes, placement), butin des coffres de survie, recettes.
Utilisation : python3 tools/generate_camp_assets.py
"""
import json
import os
import random
import zipfile

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/modeminecraft")
DATA = os.path.join(RES, "data/modeminecraft")
MOD = "modeminecraft"
VANILLA = os.path.join(ROOT, "build/moddev/artifacts/neoforge-21.1.267-client-extra-aka-minecraft-resources.jar")


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def block_loot(name):
    write(os.path.join(DATA, "loot_table/blocks", f"{name}.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


# ------------------------------------------------------------------ textures
def canvas(seed=1, base=(176, 160, 112)):
    """Toile kaki / sable : trame tissée, coutures, légères taches."""
    r = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            weave = 6 if (x + y) % 2 == 0 else -6
            seam = -22 if y in (0, 15) else (-12 if x in (7, 8) and y % 3 == 0 else 0)
            n = r.randint(-7, 7) + weave + seam
            px[x, y] = tuple(max(0, min(255, c + n)) for c in base) + (255,)
    for _ in range(3):                                  # taches de boue
        cx, cy = r.randint(1, 14), r.randint(1, 14)
        for dx in range(-1, 2):
            for dy in range(-1, 1):
                x, y = cx + dx, cy + dy
                if 0 <= x < 16 and 0 <= y < 16:
                    c = px[x, y]
                    px[x, y] = (c[0] - 30, c[1] - 28, c[2] - 22, 255)
    return img


def quilted(seed, base, stitch):
    """Tissu matelassé du sac de couchage : losanges piqués et reflets."""
    r = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            u, v = (x + y) % 6, (x - y) % 6
            light = 14 if u in (2, 3) and v in (2, 3) else 0
            line = (u == 0 or v == 0)
            c = stitch if line else tuple(min(255, b + light + r.randint(-5, 5)) for b in base)
            px[x, y] = tuple(c) + (255,)
    for x in range(16):                                 # fermeture éclair sur un côté
        px[x, 14] = (60, 60, 64, 255) if x % 2 else (150, 150, 156, 255)
    return img


def save(img, rel):
    path = os.path.join(ASSETS, "textures", rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def tent_icon():
    """Icône de la tente pliée : sac de transport kaki avec sangles."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cv = canvas(5)
    for y in range(4, 13):
        for x in range(2, 14):
            if (x in (2, 13) and y in (4, 12)):
                continue
            c = cv.getpixel((x, y))
            shade = -25 if y > 10 else (12 if y < 6 else 0)
            px[x, y] = (c[0] + shade, c[1] + shade, c[2] + shade, 255)
    for y in range(3, 14):                              # sangles
        for x in (5, 10):
            px[x, y] = (62, 52, 38, 255)
    for x in range(1, 15):                              # arceaux
        px[x, 3] = (90, 90, 96, 255)
    return img


# ------------------------------------------------------------------ modèles
def models():
    save(canvas(1), "block/tent_canvas.png")
    save(quilted(2, (58, 88, 52), (36, 56, 32)), "block/sleeping_bag_top.png")
    save(quilted(3, (48, 74, 44), (30, 46, 28)), "block/sleeping_bag_side.png")
    save(quilted(4, (196, 190, 170), (150, 144, 128)), "block/sleeping_bag_pillow.png")
    save(tent_icon(), "item/camping_tent.png")
    write(os.path.join(ASSETS, "models/block/tent_canvas.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/tent_canvas"}})
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        write(os.path.join(ASSETS, f"models/block/tent_canvas_stairs{suffix}.json"), {
            "parent": f"minecraft:block/{parent}",
            "textures": {"bottom": f"{MOD}:block/tent_canvas", "top": f"{MOD}:block/tent_canvas", "side": f"{MOD}:block/tent_canvas"}})
    write(os.path.join(ASSETS, "blockstates/tent_canvas.json"), {"variants": {"": {"model": f"{MOD}:block/tent_canvas"}}})
    with zipfile.ZipFile(VANILLA) as z:
        st = z.read("assets/minecraft/blockstates/oak_stairs.json").decode()
    st = st.replace("minecraft:block/oak_stairs", f"{MOD}:block/tent_canvas_stairs")
    with open(os.path.join(ASSETS, "blockstates/tent_canvas_stairs.json"), "w") as f:
        f.write(st)
    # sac de couchage : matelas matelassé de 3 px, oreiller, rabat ouvert
    sb = {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{MOD}:block/sleeping_bag_top", "top": f"{MOD}:block/sleeping_bag_top",
                     "side": f"{MOD}:block/sleeping_bag_side", "pillow": f"{MOD}:block/sleeping_bag_pillow"},
        "elements": [
            {"from": [1, 0, 0], "to": [15, 2.5, 16], "faces": {
                "up": {"uv": [1, 0, 15, 16], "texture": "#top"}, "down": {"uv": [1, 0, 15, 16], "texture": "#side"},
                "north": {"uv": [1, 13, 15, 15.5], "texture": "#side"}, "south": {"uv": [1, 13, 15, 15.5], "texture": "#side"},
                "west": {"uv": [0, 13, 16, 15.5], "texture": "#side"}, "east": {"uv": [0, 13, 16, 15.5], "texture": "#side"}}},
            {"from": [1.5, 2.5, 5], "to": [14.5, 3.5, 16], "faces": {
                "up": {"uv": [1, 4, 15, 15], "texture": "#side"}, "north": {"uv": [1, 0, 15, 1], "texture": "#pillow"},
                "west": {"uv": [5, 0, 16, 1], "texture": "#side"}, "east": {"uv": [5, 0, 16, 1], "texture": "#side"},
                "south": {"uv": [1, 0, 15, 1], "texture": "#side"}}},
            {"from": [3, 2.5, 0.5], "to": [13, 4.5, 4.5], "faces": {
                "up": {"uv": [3, 0, 13, 4], "texture": "#pillow"}, "north": {"uv": [3, 0, 13, 2], "texture": "#pillow"},
                "south": {"uv": [3, 0, 13, 2], "texture": "#pillow"}, "west": {"uv": [0, 0, 4, 2], "texture": "#pillow"},
                "east": {"uv": [0, 0, 4, 2], "texture": "#pillow"}}},
        ]}
    write(os.path.join(ASSETS, "models/block/sleeping_bag.json"), sb)
    write(os.path.join(ASSETS, "blockstates/sleeping_bag.json"), {"variants": {
        f"facing={f}": {"model": f"{MOD}:block/sleeping_bag", **({"y": r} if r else {})}
        for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    for name in ("tent_canvas", "tent_canvas_stairs", "sleeping_bag"):
        write(os.path.join(ASSETS, f"models/item/{name}.json"), {"parent": f"{MOD}:block/{name}"})
        block_loot(name)
    write(os.path.join(ASSETS, "models/item/camping_tent.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/camping_tent"}})


# ------------------------------------------------------------------ structure, butin, recettes
def data():
    write(os.path.join(DATA, "tags/worldgen/biome/has_camp.json"), {"values": [
        "#minecraft:is_forest", "#minecraft:is_taiga", "#minecraft:is_hill", "#minecraft:is_mountain", "#minecraft:is_savanna",
        "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:desert", "minecraft:snowy_plains",
        "minecraft:windswept_hills", "minecraft:stony_peaks", "minecraft:cherry_grove", "minecraft:badlands"]})
    write(os.path.join(DATA, "worldgen/structure/camp.json"), {
        "type": f"{MOD}:camp", "biomes": f"#{MOD}:has_camp", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "none"})
    write(os.path.join(DATA, "worldgen/structure_set/camps.json"), {
        "structures": [{"structure": f"{MOD}:camp", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 18, "separation": 7, "salt": 52914730}})

    def item(name, w, lo=1, hi=1, ns=MOD):
        e = {"type": "minecraft:item", "name": f"{ns}:{name}", "weight": w}
        if hi > 1:
            e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        return e
    write(os.path.join(DATA, "loot_table/chests/camp_supplies.json"), {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 5}, "entries": [
            item("ammo_9mm", 5, 6, 18), item("ammo_556", 3, 6, 16), item("shotgun_shell", 3, 3, 8), item("ammo_45acp", 2, 4, 12),
            item("bandage", 5, 1, 3), item("tactical_battery", 3, 1, 2),
            item("bread", 5, 1, 4, "minecraft"), item("cooked_beef", 3, 1, 3, "minecraft"), item("baked_potato", 3, 1, 4, "minecraft"),
            item("dried_kelp", 2, 2, 6, "minecraft"), item("torch", 3, 2, 6, "minecraft"), item("string", 2, 1, 4, "minecraft")]},
        {"rolls": 1, "entries": [{"type": "minecraft:empty", "weight": 10}, item("crowbar", 2), item("glock17", 1), item("beretta_m9", 1),
                                 item("makarov", 1), item("combat_knife", 2), item("first_aid_kit", 1), item("sleeping_bag", 1)]}]})
    rec = os.path.join(DATA, "recipe")
    write(os.path.join(rec, "tent_canvas.json"), {"type": "minecraft:crafting_shaped", "category": "building",
        "pattern": ["SWS", "WSW", "SWS"], "key": {"S": {"item": "minecraft:string"}, "W": {"tag": "minecraft:wool"}},
        "result": {"id": f"{MOD}:tent_canvas", "count": 4}})
    write(os.path.join(rec, "tent_canvas_stairs.json"), {"type": "minecraft:crafting_shaped", "category": "building",
        "pattern": ["C  ", "CC ", "CCC"], "key": {"C": {"item": f"{MOD}:tent_canvas"}}, "result": {"id": f"{MOD}:tent_canvas_stairs", "count": 4}})
    write(os.path.join(rec, "camping_tent.json"), {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": [" C ", "CSC", "CSC"], "key": {"C": {"item": f"{MOD}:tent_canvas"}, "S": {"item": "minecraft:stick"}},
        "result": {"id": f"{MOD}:camping_tent", "count": 1}})
    write(os.path.join(rec, "sleeping_bag.json"), {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["  W", "CCC"], "key": {"W": {"tag": "minecraft:wool"}, "C": {"tag": "minecraft:wool_carpets"}},
        "result": {"id": f"{MOD}:sleeping_bag", "count": 1}})


if __name__ == "__main__":
    models()
    data()
    print("campements : textures, modèles, structure, butin et recettes générés")

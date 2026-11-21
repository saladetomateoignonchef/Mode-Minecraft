#!/usr/bin/env python3
"""
Système policier et carcéral (mise à jour 0.3) : casier de police (fermé / forcé), porte de cellule coulissante à
barreaux (serrure à voyant rouge / vert), lit de cellule, bloc sanitaire en inox, banc métallique, pupitre
d'ouverture centralisée, moniteur de surveillance (image animée), portes verrouillées (bois sombre / métal).
Génère textures, modèles, états de blocs, modèles d'objets et tables de butin.
Utilisation : python3 tools/generate_police_assets.py
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


def save(img, rel):
    p = os.path.join(ASSETS, "textures", rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def block_loot(name, drop=None):
    write(os.path.join(DATA, "loot_table/blocks", f"{name}.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{drop or name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def metal(seed, base, w=16, h=16, brushed=True):
    r = random.Random(seed)
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        line = r.randint(-4, 4)
        for x in range(w):
            n = r.randint(-5, 5) + (line if brushed else 0)
            px[x, y] = tuple(max(0, min(255, c + n)) for c in base) + (255,)
    return img


def shade(img, x0, y0, x1, y1, d):
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = px[x, y]
            px[x, y] = (max(0, min(255, c[0] + d)), max(0, min(255, c[1] + d)), max(0, min(255, c[2] + d)), 255)


# ------------------------------------------------------------------ casier
def locker():
    base = (86, 104, 122)
    for locked in (True, False):
        img = metal(11, base)
        px = img.load()
        shade(img, 0, 0, 16, 1, -30)
        shade(img, 0, 15, 16, 16, -30)
        shade(img, 0, 0, 1, 16, -30)
        shade(img, 15, 0, 16, 16, -30)
        for y in (2, 4, 6):                                 # fentes d'aération
            for x in range(4, 12):
                px[x, y] = (30, 34, 40, 255)
            for x in range(4, 12):
                px[x, y + 1] = (128, 144, 160, 255)
        shade(img, 2, 9, 14, 13, 8)                          # porte-étiquette
        for x in range(3, 13):
            px[x, 10] = (230, 226, 210, 255)
            px[x, 11] = (210, 206, 190, 255)
        px[13, 8] = px[13, 9] = (40, 40, 44, 255)            # poignée
        px[12, 8] = (170, 170, 176, 255)
        if locked:                                           # cadenas
            for (x, y) in [(12, 11), (13, 11), (12, 12), (13, 12)]:
                px[x, y] = (196, 160, 52, 255)
            px[12, 10] = px[13, 10] = (120, 120, 126, 255)
        else:                                                # pêne forcé, tôle tordue
            px[13, 8] = (200, 200, 205, 255)
            shade(img, 11, 7, 15, 10, 25)
        save(img, f"block/police_locker_front{'' if locked else '_forced'}.png")
    side = metal(12, base)
    shade(side, 0, 0, 16, 1, -30)
    shade(side, 0, 15, 16, 16, -30)
    save(side, "block/police_locker_side.png")
    top = metal(13, (96, 112, 128), brushed=False)
    save(top, "block/police_locker_top.png")
    for locked in (True, False):
        suffix = "" if locked else "_forced"
        write(os.path.join(ASSETS, f"models/block/police_locker{suffix}.json"), {
            "parent": "minecraft:block/orientable",
            "textures": {"front": f"{MOD}:block/police_locker_front{suffix}", "side": f"{MOD}:block/police_locker_side",
                         "top": f"{MOD}:block/police_locker_top"}})
    variants = {}
    for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for locked in ("true", "false"):
            m = {"model": f"{MOD}:block/police_locker{'' if locked == 'true' else '_forced'}"}
            if r:
                m["y"] = r
            variants[f"facing={f},locked={locked}"] = m
    write(os.path.join(ASSETS, "blockstates/police_locker.json"), {"variants": variants})
    write(os.path.join(ASSETS, "models/item/police_locker.json"), {"parent": f"{MOD}:block/police_locker"})
    block_loot("police_locker")


# ------------------------------------------------------------------ porte de cellule coulissante
def jail_door():
    steel = metal(21, (92, 94, 100))
    save(steel, "block/jail_steel.png")
    for color, name in (((220, 40, 36), "red"), ((60, 220, 70), "green")):
        img = metal(22, (54, 56, 62), brushed=False)
        px = img.load()
        for y in range(3, 7):
            for x in range(3, 13):
                px[x, y] = (20, 22, 26, 255)                  # écran du digicode
        for x in range(5, 11):
            px[x, 5] = color + (255,)
        for y in (9, 11, 13):
            for x in (4, 7, 10):
                px[x, y] = (150, 150, 156, 255)
        save(img, f"block/jail_lock_{name}.png")

    def bars(half, open_):
        el = []
        ys = (0, 16)
        if half == "lower":
            el.append({"from": [0, 0, 6.5], "to": [16, 2, 9.5], "faces": face_all("#steel")})            # rail bas
        else:
            el.append({"from": [0, 14, 6.5], "to": [16, 16, 9.5], "faces": face_all("#steel")})          # rail haut
        el.append({"from": [0, 0, 6], "to": [1.5, 16, 10], "faces": face_all("#steel")})                 # montants
        el.append({"from": [14.5, 0, 6], "to": [16, 16, 10], "faces": face_all("#steel")})
        if not open_:
            for x in (3, 5.75, 8.5, 11.25):
                el.append({"from": [x, 0 if half == "lower" else 0, 7.25], "to": [x + 1.5, 16, 8.75], "faces": face_all("#steel")})
            if half == "upper":
                el.append({"from": [1.5, 7, 7], "to": [14.5, 8.5, 9], "faces": face_all("#steel")})     # traverse
            else:
                el.append({"from": [1.5, 9, 7], "to": [14.5, 10.5, 9], "faces": face_all("#steel")})
        if half == "lower":
            el.append({"from": [12.5, 9, 4.5], "to": [16, 13, 11.5], "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#lock"}, "south": {"uv": [0, 0, 16, 16], "texture": "#lock"},
                "east": {"uv": [0, 0, 8, 8], "texture": "#steel"}, "west": {"uv": [0, 0, 8, 8], "texture": "#steel"},
                "up": {"uv": [0, 0, 8, 8], "texture": "#steel"}, "down": {"uv": [0, 0, 8, 8], "texture": "#steel"}}})
        return el

    for half in ("lower", "upper"):
        for open_ in (False, True):
            for led in ("red", "green"):
                if half == "upper" and led == "green":
                    continue
                name = f"jail_door_{half}{'_open' if open_ else ''}{'_' + led if half == 'lower' else ''}"
                write(os.path.join(ASSETS, f"models/block/{name}.json"), {
                    "parent": "minecraft:block/block", "ambientocclusion": False,
                    "textures": {"particle": f"{MOD}:block/jail_steel", "steel": f"{MOD}:block/jail_steel",
                                 "lock": f"{MOD}:block/jail_lock_{led}"},
                    "elements": bars(half, open_)})
    variants = {}
    for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for half in ("lower", "upper"):
            for open_ in ("true", "false"):
                for locked in ("true", "false"):
                    led = "red" if locked == "true" else "green"
                    name = f"jail_door_{half}{'_open' if open_ == 'true' else ''}{'_' + led if half == 'lower' else ''}"
                    m = {"model": f"{MOD}:block/{name}"}
                    if r:
                        m["y"] = r
                    variants[f"facing={f},half={half},open={open_},locked={locked}"] = m
    write(os.path.join(ASSETS, "blockstates/sliding_jail_door.json"), {"variants": variants})
    write(os.path.join(ASSETS, "models/item/sliding_jail_door.json"), {"parent": f"{MOD}:block/jail_door_lower_red"})
    block_loot("sliding_jail_door")


def face_all(t, uv=(0, 0, 16, 16)):
    return {d: {"uv": list(uv), "texture": t} for d in ("north", "south", "east", "west", "up", "down")}


# ------------------------------------------------------------------ mobilier de cellule et poste de garde
def furniture():
    save(metal(31, (150, 154, 160)), "block/stainless_steel.png")
    mat = Image.new("RGBA", (16, 16))
    r = random.Random(32)
    for y in range(16):
        for x in range(16):
            v = r.randint(-6, 6) + (-14 if y in (0, 15) else 0)
            mat.putpixel((x, y), (82 + v, 98 + v, 124 + v, 255))
    save(mat, "block/cell_mattress.png")
    s = f"{MOD}:block/stainless_steel"
    bunk = {"parent": "minecraft:block/block", "textures": {"particle": s, "steel": s, "mat": f"{MOD}:block/cell_mattress"},
            "elements": [
                {"from": [0, 5, 0], "to": [16, 7, 16], "faces": face_all("#steel")},                       # cadre
                {"from": [1, 7, 1], "to": [15, 9, 15], "faces": face_all("#mat")},                         # matelas
                {"from": [0, 0, 0], "to": [2, 5, 2], "faces": face_all("#steel")},
                {"from": [14, 0, 0], "to": [16, 5, 2], "faces": face_all("#steel")},
                {"from": [0, 0, 14], "to": [2, 5, 16], "faces": face_all("#steel")},
                {"from": [14, 0, 14], "to": [16, 5, 16], "faces": face_all("#steel")}]}
    write(os.path.join(ASSETS, "models/block/prison_bunk_bed.json"), bunk)
    toilet = {"parent": "minecraft:block/block", "textures": {"particle": s, "steel": s},
              "elements": [
                  {"from": [3, 0, 4], "to": [13, 6, 14], "faces": face_all("#steel")},                    # cuvette
                  {"from": [4, 6, 5], "to": [12, 7, 13], "faces": face_all("#steel")},
                  {"from": [2, 0, 13], "to": [14, 14, 16], "faces": face_all("#steel")},                  # dosseret
                  {"from": [3, 10, 9], "to": [13, 12, 13], "faces": face_all("#steel")},                  # lavabo
                  {"from": [7, 12, 13], "to": [9, 14, 14], "faces": face_all("#steel")}]}                 # robinet
    write(os.path.join(ASSETS, "models/block/prison_toilet_sink.json"), toilet)
    bench = {"parent": "minecraft:block/block", "textures": {"particle": s, "steel": s},
             "elements": [
                 {"from": [0, 7, 3], "to": [16, 8.5, 13], "faces": face_all("#steel")},
                 {"from": [1, 0, 6], "to": [3, 7, 10], "faces": face_all("#steel")},
                 {"from": [13, 0, 6], "to": [15, 7, 10], "faces": face_all("#steel")}]}
    write(os.path.join(ASSETS, "models/block/prison_bench.json"), bench)
    # pupitre d'ouverture centralisée : boîtier incliné, gros bouton rouge, voyants
    panel = metal(41, (60, 66, 74), brushed=False)
    px = panel.load()
    for y in range(4, 10):
        for x in range(5, 11):
            if (x - 7.5) ** 2 + (y - 6.5) ** 2 < 7:
                px[x, y] = (200, 30, 30, 255) if (x, y) != (6, 5) else (255, 120, 120, 255)
    for i, x in enumerate(range(3, 14, 2)):
        px[x, 12] = (60, 220, 70, 255) if i % 2 else (230, 200, 40, 255)
    save(panel, "block/cell_control_panel.png")
    write(os.path.join(ASSETS, "models/block/cell_control_panel.json"), {
        "parent": "minecraft:block/block", "textures": {"particle": f"{MOD}:block/cell_control_panel", "top": f"{MOD}:block/cell_control_panel", "s": s},
        "elements": [{"from": [1, 0, 2], "to": [15, 10, 14], "faces": {
            "up": {"uv": [0, 0, 16, 16], "texture": "#top"}, "north": {"uv": [0, 0, 16, 10], "texture": "#s"},
            "south": {"uv": [0, 0, 16, 10], "texture": "#s"}, "east": {"uv": [0, 0, 12, 10], "texture": "#s"},
            "west": {"uv": [0, 0, 12, 10], "texture": "#s"}, "down": {"uv": [0, 0, 16, 16], "texture": "#s"}}}]})
    # moniteur de surveillance : image animée (vues de caméras qui défilent, parasites)
    frames = 8
    mon = Image.new("RGBA", (16, 16 * frames))
    r = random.Random(51)
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                frame_edge = x in (0, 15) or y in (0, 15)
                if frame_edge:
                    c = (30, 30, 34)
                else:
                    split = x == 7 or y == 7
                    g = 40 + r.randint(0, 40) + (30 if (y + f * 2) % 6 == 0 else 0)
                    c = (20, 24, 20) if split else (g // 2, g, g // 2)
                mon.putpixel((x, y + 16 * f), c + (255,))
    save(mon, "block/surveillance_monitor_screen.png")
    with open(os.path.join(ASSETS, "textures/block/surveillance_monitor_screen.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 6}}, fh)
    write(os.path.join(ASSETS, "models/block/surveillance_monitor.json"), {
        "parent": "minecraft:block/block", "textures": {"particle": s, "screen": f"{MOD}:block/surveillance_monitor_screen", "s": f"{MOD}:block/cell_control_panel"},
        "elements": [{"from": [1, 2, 6], "to": [15, 14, 11], "faces": {
            "north": {"uv": [0, 0, 16, 16], "texture": "#screen"}, "south": {"uv": [0, 0, 16, 16], "texture": "#s"},
            "east": {"uv": [0, 0, 5, 12], "texture": "#s"}, "west": {"uv": [0, 0, 5, 12], "texture": "#s"},
            "up": {"uv": [0, 0, 16, 5], "texture": "#s"}, "down": {"uv": [0, 0, 16, 5], "texture": "#s"}}},
            {"from": [6, 0, 7], "to": [10, 2, 10], "faces": face_all("#s")}]})
    for name in ("prison_bunk_bed", "prison_toilet_sink", "prison_bench", "cell_control_panel", "surveillance_monitor"):
        write(os.path.join(ASSETS, f"blockstates/{name}.json"), {"variants": {
            f"facing={f}": ({"model": f"{MOD}:block/{name}", "y": r} if r else {"model": f"{MOD}:block/{name}"})
            for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        write(os.path.join(ASSETS, f"models/item/{name}.json"), {"parent": f"{MOD}:block/{name}"})
        block_loot(name)


# ------------------------------------------------------------------ portes verrouillées (modèles de Minecraft)
def locked_doors():
    with zipfile.ZipFile(VANILLA) as z:
        for name, vanilla in (("locked_wooden_door", "dark_oak_door"), ("locked_metal_door", "iron_door")):
            st = json.loads(z.read(f"assets/minecraft/blockstates/{vanilla}.json"))
            write(os.path.join(ASSETS, f"blockstates/{name}.json"), st)       # la propriété « locked » est ignorée
            write(os.path.join(ASSETS, f"models/item/{name}.json"), {"parent": "minecraft:item/generated",
                                                                       "textures": {"layer0": f"minecraft:item/{vanilla}"}})
            write(os.path.join(DATA, "loot_table/blocks", f"{name}.json"), {
                "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}",
                    "conditions": [{"condition": "minecraft:block_state_property", "block": f"{MOD}:{name}", "properties": {"half": "lower"}}]}],
                    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


if __name__ == "__main__":
    locker()
    jail_door()
    furniture()
    locked_doors()
    print("police : casiers, porte de cellule, mobilier, moniteur et portes verrouillées générés")

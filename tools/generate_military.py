#!/usr/bin/env python3
"""
Mise à jour 0.4 « Véhicules militaires & missiles » : icônes des objets (butin des braquages, munitions lourdes,
désignateur, mines...), modèles d'objets et données. Utilisation : python3 tools/generate_military.py
"""
import json
import os
import random
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/modeminecraft")
DATA = os.path.join(RES, "data/modeminecraft")
MOD = "modeminecraft"


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3],)


class Icon:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        self.r = random.Random(seed)

    def rect(self, x0, y0, x1, y1, col, noise=0, bevel=True):
        """Rectangle [x0, x1] x [y0, y1] inclus, avec léger relief (haut-gauche clair, bas-droite sombre)."""
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                c = col
                if bevel and (x == x0 or y == y0):
                    c = shade(col, 1.18)
                elif bevel and (x == x1 or y == y1):
                    c = shade(col, 0.72)
                if noise:
                    c = shade(c, 1 + self.r.uniform(-noise, noise))
                self.img.putpixel((x, y), c)

    def px(self, x, y, col):
        self.img.putpixel((x, y), col)

    def outline(self, col=(18, 18, 20, 255)):
        src = self.img.copy()
        for y in range(16):
            for x in range(16):
                if src.getpixel((x, y))[3]:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3] > 0 and src.getpixel((nx, ny)) != col:
                        self.img.putpixel((x, y), col)
                        break

    def save(self, name, outline=True):
        if outline:
            self.outline()
        p = os.path.join(ASSETS, "textures/item", name + ".png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        self.img.save(p)
        write(os.path.join(ASSETS, "models/item", name + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})


# ------------------------------------------------------------------------------------------- butin des braquages
def loot_icons():
    i = Icon(1)                                            # smartphone
    i.rect(5, 2, 10, 14, hexc("#202226"))
    i.rect(6, 3, 9, 12, hexc("#2f6fb0"), noise=0.08, bevel=False)
    for y in range(3, 13):                                 # reflet de l'écran
        i.px(6, y, hexc("#5a9ad8"))
    i.px(7, 4, hexc("#9ac8f0"))
    i.px(7, 13, hexc("#6a6e76")), i.px(8, 13, hexc("#6a6e76"))
    i.px(7, 2, hexc("#46484e")), i.px(8, 2, hexc("#46484e"))
    i.save("smartphone")

    i = Icon(2)                                            # montre
    i.rect(6, 0, 9, 4, hexc("#3a2a20"), noise=0.08)        # bracelet cuir
    i.rect(6, 11, 9, 15, hexc("#3a2a20"), noise=0.08)
    i.rect(4, 4, 11, 11, hexc("#c8ccd2"))                  # boîtier acier
    i.rect(5, 5, 10, 10, hexc("#f0ece0"), bevel=False)     # cadran
    for (x, y) in ((7, 5), (5, 7), (10, 7), (7, 10)):
        i.px(x, y, hexc("#404040"))
    for y in range(6, 8):
        i.px(7, y, hexc("#181818"))                        # aiguilles
    i.px(8, 8, hexc("#181818")), i.px(9, 8, hexc("#181818"))
    i.px(12, 7, hexc("#a0a4aa"))                           # couronne
    i.save("wristwatch")

    i = Icon(3)                                            # ration militaire (sachet MRE)
    i.rect(3, 3, 12, 13, hexc("#6b5a3a"), noise=0.07)
    i.rect(3, 2, 12, 3, hexc("#56482e"), bevel=False)      # soudure
    i.rect(5, 6, 10, 9, hexc("#d8ccaa"), bevel=False)      # étiquette
    for x in range(6, 10):
        i.px(x, 7, hexc("#3a3428"))
    i.px(6, 8, hexc("#3a3428")), i.px(7, 8, hexc("#3a3428"))
    i.px(12, 2, (0, 0, 0, 0)), i.px(3, 2, (0, 0, 0, 0))
    i.save("military_ration")


# ------------------------------------------------------------------------------------------- munitions lourdes
def diag(icon, length, width, colors, start=(2, 13)):
    """Objet allongé en diagonale (bas-gauche vers haut-droite) : colors = liste (fraction de longueur, couleur)."""
    x0, y0 = start
    for i in range(length):
        f = i / max(1, length - 1)
        col = colors[-1][1]
        for frac, c in colors:
            if f <= frac:
                col = c
                break
        for w in range(-(width // 2), width - width // 2):
            x, y = x0 + i, y0 - i + w
            if 0 <= x < 16 and 0 <= y < 16:
                icon.px(x, y, shade(col, 1.15 if w < 0 else 0.85 if w > 0 else 1))


def heavy_ammo_icons():
    brass, steel, olive, white, black, red = hexc("#c8a040"), hexc("#8a9098"), hexc("#5a6a38"), hexc("#e0e2da"), hexc("#26282a"), hexc("#b02a20")
    # bandes de 25 / 30 mm (AP-I : pointes noires et rouges ; HE-I : pointes jaunes et rouges)
    for name, tip in (("ap_i_belt", black), ("he_i_belt", hexc("#d8c030"))):
        i = Icon(name)
        for k in range(4):
            x = 2 + k * 3
            i.rect(x, 5, x + 1, 11, brass, bevel=False)
            i.rect(x, 2, x + 1, 4, tip, bevel=False)
            i.px(x, 2, red), i.px(x + 1, 2, red)
            i.rect(x, 12, x + 2, 13, hexc("#505458"), bevel=False)        # maillons
        i.save(name)
    # obus de char
    i = Icon("apfsds")
    diag(i, 13, 2, [(0.15, brass), (0.3, hexc("#7a7a6a")), (0.85, black), (1, steel)])
    i.px(3, 12, hexc("#7a7a6a")), i.px(4, 13, hexc("#7a7a6a"))           # sabot
    i.save("apfsds_shell")
    i = Icon("he_shell")
    diag(i, 12, 3, [(0.4, brass), (0.85, olive), (1, red)])
    i.save("he_shell")
    i = Icon("heat_shell")
    diag(i, 12, 3, [(0.4, brass), (0.8, hexc("#5a5e62")), (1, black)])
    i.save("heat_shell")
    i = Icon("cannon_atgm")
    diag(i, 12, 3, [(0.4, brass), (0.9, olive), (1, hexc("#303828"))])
    i.px(9, 6, red)
    i.save("cannon_atgm_missile")
    # roquettes et missiles
    for name, body, tip, n, w in (("grad_rocket", olive, red, 14, 2), ("s8_rocket", olive, hexc("#c0c0b0"), 12, 2),
                                  ("hydra_rocket", white, olive, 12, 2), ("mlrs_rocket", white, hexc("#909498"), 14, 3),
                                  ("atacms_missile", white, black, 14, 4), ("atgm_missile", olive, black, 12, 3)):
        i = Icon(name)
        diag(i, n, w, [(0.12, hexc("#3a3a3a")), (0.85, body), (1, tip)], start=(1, 14))
        # ailerons
        i.px(1, 12, body), i.px(3, 14, body), i.px(0, 13, shade(body, 0.8))
        i.save(name)
    # caisse de 14,5 mm
    i = Icon("ammo_box_145")
    i.rect(2, 5, 13, 13, hexc("#4e5a30"), noise=0.06)
    i.rect(2, 4, 13, 5, hexc("#3c4626"), bevel=False)
    i.rect(6, 7, 9, 9, hexc("#d8d0a0"), bevel=False)
    i.rect(5, 2, 10, 3, hexc("#505458"), bevel=False)                      # poignée
    i.save("ammo_box_145")
    # bidon de carburant (jerrican)
    i = Icon("jerrycan")
    i.rect(3, 3, 12, 14, hexc("#4f5d34"), noise=0.05)
    for y in (6, 10):
        for x in range(4, 12):
            i.px(x, y, hexc("#3a4626"))                                     # nervures en X
    i.rect(4, 1, 6, 2, hexc("#383c40"), bevel=False)                       # poignées
    i.rect(8, 1, 10, 2, hexc("#383c40"), bevel=False)
    i.rect(11, 1, 12, 2, hexc("#b0a040"), bevel=False)                     # bouchon
    i.save("jerrycan")
    # désignateur laser
    i = Icon("designator")
    i.rect(2, 5, 13, 11, hexc("#3a3e32"), noise=0.05)
    i.rect(11, 6, 13, 10, hexc("#202224"), bevel=False)
    i.px(12, 7, hexc("#ff2020")), i.px(12, 8, hexc("#c01010"))
    i.rect(3, 3, 6, 4, hexc("#2a2c2e"), bevel=False)                       # oculaire
    i.rect(5, 12, 7, 14, hexc("#2a2c2e"), bevel=False)                     # poignée
    i.save("laser_designator")


def strategic_icons():
    white, grey, black, red = hexc("#e4e6e0"), hexc("#8a8e92"), hexc("#26282a"), hexc("#b02a20")
    for name, band, tip, w in (("missile_48n6e3", hexc("#5a6a38"), grey, 3), ("missile_9m96e2", hexc("#5a6a38"), grey, 2),
                               ("missile_77n6n", black, black, 3), ("missile_40n6m", red, grey, 3),
                               ("iskander_he", red, red, 4), ("iskander_cluster", hexc("#d8c030"), grey, 4), ("iskander_emp", hexc("#3080e0"), grey, 4)):
        i = Icon(name)
        diag(i, 14, w, [(0.12, hexc("#3a3a3a")), (0.35, white), (0.45, band), (0.85, white), (1, tip)], start=(1, 14))
        i.px(1, 12, grey), i.px(3, 14, grey), i.px(0, 13, shade(grey, 0.8))
        i.save(name)


def mine_icons():
    for name, body, top in (("tm62_mine", hexc("#4f5d34"), hexc("#3a4626")), ("m15_mine", hexc("#5a6a3a"), hexc("#c8b060"))):
        i = Icon(name)
        for y in range(16):
            for x in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if d < 7:
                    c = top if d < 2.2 else body if d < 6 else shade(body, 0.75)
                    i.px(x, y, shade(c, 1.12 if x + y < 14 else 0.92))
        i.px(7, 7, hexc("#202020")), i.px(8, 8, hexc("#202020"))
        i.save(name)


def shell_texture():
    """Texture des projectiles en vol : 8 lignes de couleur (voir ShellRenderer)."""
    img = Image.new("RGBA", (16, 64))
    rows = ["#c8a040", "#26282a", "#5a6a38", "#e0e2da", "#b02a20", "#7a7e84", "#ffd070", "#ffffff"]
    r = random.Random(7)
    for k, h in enumerate(rows):
        c = hexc(h)
        for y in range(k * 8, k * 8 + 8):
            for x in range(16):
                img.putpixel((x, y), shade(c, 1 + r.uniform(-0.06, 0.06)) if k != 6 else c)
    p = os.path.join(ASSETS, "textures/entity/shell_colors.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


# ------------------------------------------------------------------------------------------- établi d'artillerie
def block_tex(name, painter):
    img = Image.new("RGBA", (16, 16))
    r = random.Random(name)
    painter(img, r)
    p = os.path.join(ASSETS, "textures/block", name + ".png")
    img.save(p)


def artillery_table():
    olive = hexc("#4f5d34")

    def top(img, r):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y), shade(hexc("#5a5e62"), 1 + r.uniform(-0.07, 0.07)))       # plateau d'acier
        for x in range(16):
            img.putpixel((x, 0), hexc("#3a3e42")), img.putpixel((x, 15), hexc("#3a3e42"))
        for k in range(3):                                                                       # obus posés
            y = 3 + k * 4
            for x in range(3, 12):
                img.putpixel((x, y), shade(hexc("#c8a040"), 1.1 if x < 8 else 1.0))
                img.putpixel((x, y + 1), shade(hexc("#c8a040"), 0.8))
            img.putpixel((12, y), hexc("#5a6a38")), img.putpixel((13, y), hexc("#b02a20"))
        img.putpixel((13, 13), hexc("#b0b4b8")), img.putpixel((12, 13), hexc("#808488"))          # clé

    def side(img, r):
        for y in range(16):
            for x in range(16):
                c = olive if y > 2 else hexc("#5a5e62")
                img.putpixel((x, y), shade(c, 1 + r.uniform(-0.06, 0.06)))
        for y in (3, 15):
            for x in range(16):
                img.putpixel((x, y), hexc("#2c3420"))
        for x in (0, 15):
            for y in range(16):
                img.putpixel((x, y), hexc("#2c3420"))
        for x in (4, 7, 10):                                                                     # roquettes au râtelier
            for y in range(5, 14):
                img.putpixel((x, y), shade(hexc("#5a6a38"), 1.15))
                img.putpixel((x + 1, y), shade(hexc("#5a6a38"), 0.85))
            img.putpixel((x, 4), hexc("#b02a20")), img.putpixel((x + 1, 4), hexc("#b02a20"))
        for x in range(2, 14):
            img.putpixel((x, 12), hexc("#3a3e42"))

    def front(img, r):
        side(img, r)
        for y in range(6, 12):                                                                  # étau
            for x in range(5, 11):
                img.putpixel((x, y), shade(hexc("#6a6e72"), 1 + r.uniform(-0.05, 0.05)))
        for x in range(4, 12):
            img.putpixel((x, 5), hexc("#3a3e42"))
        img.putpixel((7, 8), hexc("#e0a020")), img.putpixel((8, 8), hexc("#e0a020"))           # étiquette danger
    block_tex("artillery_table_top", top)
    block_tex("artillery_table_side", side)
    block_tex("artillery_table_front", front)
    write(os.path.join(ASSETS, "blockstates/artillery_table.json"), {"variants": {"": {"model": f"{MOD}:block/artillery_table"}}})
    write(os.path.join(ASSETS, "models/block/artillery_table.json"), {"parent": "minecraft:block/cube", "textures": {
        "particle": f"{MOD}:block/artillery_table_side", "north": f"{MOD}:block/artillery_table_front", "south": f"{MOD}:block/artillery_table_front",
        "east": f"{MOD}:block/artillery_table_side", "west": f"{MOD}:block/artillery_table_side", "up": f"{MOD}:block/artillery_table_top",
        "down": "minecraft:block/iron_block"}})
    write(os.path.join(ASSETS, "models/item/artillery_table.json"), {"parent": f"{MOD}:block/artillery_table"})
    write(os.path.join(DATA, "loot_table/blocks/artillery_table.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"{MOD}:artillery_table"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(os.path.join(DATA, "recipe/artillery_table.json"), {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["SSS", "TAT", "I I"], "key": {"S": {"item": f"{MOD}:steel_plate"}, "T": {"item": "minecraft:tnt"},
                                                  "A": {"item": f"{MOD}:armament_table"}, "I": {"item": "minecraft:iron_block"}},
        "result": {"id": f"{MOD}:artillery_table", "count": 1}})


# recettes de l'établi d'artillerie : objet -> (quantité, [(ingrédient, nombre)])
ARTILLERY_RECIPES = {
    "ap_i_belt": (1, [("minecraft:copper_ingot", 4), ("modeminecraft:steel_ingot", 2), ("minecraft:gunpowder", 4), ("minecraft:blaze_powder", 1)]),
    "he_i_belt": (1, [("minecraft:copper_ingot", 4), ("minecraft:gunpowder", 6), ("minecraft:blaze_powder", 1)]),
    "ammo_box_145": (1, [("minecraft:copper_ingot", 4), ("minecraft:iron_ingot", 3), ("minecraft:gunpowder", 4)]),
    "apfsds_shell": (1, [("modeminecraft:titanium_ingot", 2), ("minecraft:copper_ingot", 2), ("minecraft:gunpowder", 4)]),
    "he_shell": (1, [("minecraft:iron_ingot", 3), ("minecraft:copper_ingot", 2), ("minecraft:tnt", 1), ("minecraft:gunpowder", 2)]),
    "heat_shell": (1, [("minecraft:iron_ingot", 2), ("minecraft:copper_ingot", 3), ("minecraft:tnt", 1), ("minecraft:gunpowder", 2)]),
    "cannon_atgm_missile": (1, [("modeminecraft:steel_ingot", 2), ("minecraft:redstone", 4), ("minecraft:tnt", 1), ("minecraft:gunpowder", 3)]),
    "grad_rocket": (2, [("minecraft:iron_ingot", 3), ("minecraft:tnt", 1), ("minecraft:gunpowder", 4)]),
    "s8_rocket": (4, [("minecraft:iron_ingot", 3), ("minecraft:tnt", 1), ("minecraft:gunpowder", 4)]),
    "hydra_rocket": (4, [("minecraft:iron_ingot", 3), ("minecraft:tnt", 1), ("minecraft:gunpowder", 4)]),
    "mlrs_rocket": (1, [("modeminecraft:steel_ingot", 3), ("minecraft:redstone", 4), ("minecraft:compass", 1), ("minecraft:tnt", 2)]),
    "atacms_missile": (1, [("modeminecraft:titanium_ingot", 4), ("minecraft:redstone_block", 1), ("minecraft:compass", 1), ("minecraft:tnt", 4)]),
    "atgm_missile": (1, [("modeminecraft:steel_ingot", 2), ("minecraft:redstone", 4), ("minecraft:tnt", 1), ("minecraft:string", 2)]),
    "missile_48n6e3": (1, [("modeminecraft:titanium_ingot", 4), ("minecraft:redstone_block", 1), ("minecraft:tnt", 3), ("minecraft:blaze_rod", 2)]),
    "missile_9m96e2": (1, [("modeminecraft:titanium_ingot", 3), ("minecraft:redstone_block", 1), ("minecraft:tnt", 2), ("minecraft:blaze_rod", 2)]),
    "missile_77n6n": (1, [("modeminecraft:titanium_plate", 4), ("minecraft:redstone_block", 2), ("minecraft:blaze_rod", 4), ("minecraft:diamond", 1)]),
    "missile_40n6m": (1, [("modeminecraft:titanium_plate", 4), ("minecraft:redstone_block", 2), ("minecraft:tnt", 3), ("minecraft:blaze_rod", 4)]),
    "iskander_he": (1, [("modeminecraft:titanium_plate", 6), ("minecraft:redstone_block", 2), ("minecraft:tnt", 16), ("minecraft:blaze_rod", 6)]),
    "iskander_cluster": (1, [("modeminecraft:titanium_plate", 6), ("minecraft:redstone_block", 2), ("minecraft:tnt", 12), ("minecraft:blaze_rod", 6), ("minecraft:iron_ingot", 12)]),
    "iskander_emp": (1, [("modeminecraft:titanium_plate", 6), ("minecraft:redstone_block", 4), ("minecraft:tnt", 8), ("minecraft:blaze_rod", 6), ("minecraft:copper_block", 4)]),
    "tm62_mine": (2, [("minecraft:iron_ingot", 4), ("minecraft:tnt", 2), ("minecraft:stone_pressure_plate", 1)]),
    "m15_mine": (2, [("minecraft:iron_ingot", 4), ("minecraft:tnt", 2), ("minecraft:heavy_weighted_pressure_plate", 1)]),
    "jerrycan": (1, [("minecraft:iron_ingot", 3), ("minecraft:coal", 4), ("minecraft:dried_kelp_block", 1)]),
    "laser_designator": (1, [("modeminecraft:steel_ingot", 2), ("minecraft:redstone", 6), ("minecraft:glass_pane", 2), ("minecraft:diamond", 1)]),
}


def artillery_recipes():
    for name, (n, ing) in ARTILLERY_RECIPES.items():
        write(os.path.join(DATA, f"recipe/artillery/{name}.json"), {"type": "modeminecraft:armament", "category": "artillery",
            "ingredients": [{"item": i, "count": c} for i, c in ing], "result": {"id": f"{MOD}:{name}", "count": n}})


# ------------------------------------------------------------------------------------------- fabrication des véhicules
def part_icons():
    steel, dark, olive, rubber, gold, red = hexc("#8a9098"), hexc("#3a3e42"), hexc("#5a6a38"), hexc("#1e1e1e"), hexc("#c8a040"), hexc("#b02a20")
    i = Icon("engine")                                                   # moteur diesel V12
    i.rect(2, 6, 13, 13, dark, noise=0.06)
    for x in range(3, 13, 2):
        i.rect(x, 3, x, 6, steel, bevel=False)                           # cylindres
    i.rect(1, 9, 2, 11, steel, bevel=False)
    i.rect(13, 8, 14, 10, red, bevel=False)
    i.save("vehicle_engine")
    i = Icon("turbine")
    i.rect(2, 4, 13, 12, steel, noise=0.05)
    for y in range(5, 12, 2):
        for x in range(3, 13):
            i.px(x, y, shade(steel, 0.7))
    i.rect(13, 6, 15, 10, dark, bevel=False)
    i.save("gas_turbine")
    i = Icon("tracks")
    i.rect(1, 4, 14, 12, rubber, bevel=False)
    for x in range(1, 15, 2):
        i.px(x, 4, steel), i.px(x, 12, steel)
    for cx in (4, 8, 12):
        i.rect(cx - 1, 7, cx + 1, 9, steel, bevel=False)
    i.save("track_set")
    i = Icon("wheels")
    for cx in (4, 11):
        i.rect(cx - 3, 4, cx + 3, 12, rubber, bevel=False)
        i.rect(cx - 1, 7, cx + 1, 9, steel, bevel=False)
    i.rect(4, 8, 11, 8, dark, bevel=False)
    i.save("wheel_set")
    i = Icon("barrel")
    diag(i, 15, 2, [(0.2, olive), (0.5, dark), (0.6, olive), (1, dark)], start=(0, 15))
    i.save("cannon_barrel")
    i = Icon("autocannon")
    i.rect(2, 8, 8, 12, dark, noise=0.05)
    i.rect(8, 9, 15, 10, steel, bevel=False)
    i.rect(3, 6, 6, 8, gold, bevel=False)
    i.save("autocannon_mount")
    i = Icon("fcm")
    i.rect(2, 3, 13, 12, hexc("#2a5a2a"), noise=0.05)
    for (x, y) in ((4, 5), (8, 5), (11, 8), (5, 9), (9, 10)):
        i.rect(x, y, x + 1, y + 1, dark, bevel=False)
    i.px(6, 7, gold), i.px(10, 6, gold), i.px(12, 11, red)
    i.save("fire_control_module")
    i = Icon("rotor")
    i.rect(7, 6, 8, 9, steel, bevel=False)
    diag(i, 15, 1, [(1, dark)], start=(0, 15))
    for k in range(15):
        i.px(k, k, dark)
    i.save("rotor_assembly")
    i = Icon("launcher")
    i.rect(2, 4, 13, 12, olive, noise=0.05)
    for y in (5, 8, 11):
        for x in (4, 7, 10):
            i.px(x, y, dark), i.px(x + 1, y, dark)
    i.save("launcher_module")


def assembly_table():
    def top(img, r):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y), shade(hexc("#4a4e52"), 1 + r.uniform(-0.07, 0.07)))
        for x in range(16):
            img.putpixel((x, 0), hexc("#e0b020") if x % 4 < 2 else hexc("#202020"))      # bande de sécurité
            img.putpixel((x, 15), hexc("#e0b020") if x % 4 < 2 else hexc("#202020"))
        for y in range(5, 11):                                                          # chenille posée
            for x in range(3, 13):
                img.putpixel((x, y), hexc("#1e1e1e") if (x + y) % 2 else hexc("#6a6e72"))

    def side(img, r):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y), shade(hexc("#3a4428"), 1 + r.uniform(-0.06, 0.06)))
        for x in range(16):
            img.putpixel((x, 2), hexc("#202020")), img.putpixel((x, 13), hexc("#202020"))
        for y in range(4, 12):                                                          # palan / crochet
            img.putpixel((8, y), hexc("#8a9098"))
        for x in range(6, 11):
            img.putpixel((x, 11), hexc("#8a9098"))
        img.putpixel((6, 10), hexc("#8a9098")), img.putpixel((10, 10), hexc("#8a9098"))
    block_tex("assembly_table_top", top)
    block_tex("assembly_table_side", side)
    write(os.path.join(ASSETS, "blockstates/assembly_table.json"), {"variants": {"": {"model": f"{MOD}:block/assembly_table"}}})
    write(os.path.join(ASSETS, "models/block/assembly_table.json"), {"parent": "minecraft:block/cube", "textures": {
        "particle": f"{MOD}:block/assembly_table_side", "north": f"{MOD}:block/assembly_table_side", "south": f"{MOD}:block/assembly_table_side",
        "east": f"{MOD}:block/assembly_table_side", "west": f"{MOD}:block/assembly_table_side", "up": f"{MOD}:block/assembly_table_top",
        "down": "minecraft:block/iron_block"}})
    write(os.path.join(ASSETS, "models/item/assembly_table.json"), {"parent": f"{MOD}:block/assembly_table"})
    write(os.path.join(DATA, "loot_table/blocks/assembly_table.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"{MOD}:assembly_table"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(os.path.join(DATA, "recipe/assembly_table.json"), {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["PCP", "IAI", "IBI"], "key": {"P": {"item": "minecraft:piston"}, "C": {"item": "minecraft:chain"},
                                                  "A": {"item": f"{MOD}:artillery_table"}, "I": {"item": "minecraft:iron_block"},
                                                  "B": {"item": "minecraft:blast_furnace"}},
        "result": {"id": f"{MOD}:assembly_table", "count": 1}})


M = "modeminecraft:"
PART_RECIPES = {
    "vehicle_engine": [(M + "steel_ingot", 6), ("minecraft:iron_block", 2), ("minecraft:piston", 2), ("minecraft:redstone", 4)],
    "gas_turbine": [(M + "titanium_ingot", 6), (M + "steel_ingot", 4), ("minecraft:blaze_rod", 4), ("minecraft:redstone", 4)],
    "track_set": [(M + "steel_plate", 6), ("minecraft:iron_ingot", 12), ("minecraft:chain", 6)],
    "wheel_set": [("minecraft:iron_ingot", 6), ("minecraft:leather", 4), ("minecraft:dried_kelp", 2)],
    "cannon_barrel": [(M + "titanium_ingot", 4), (M + "steel_ingot", 8), ("minecraft:iron_block", 2)],
    "autocannon_mount": [(M + "steel_ingot", 6), ("minecraft:iron_ingot", 6), ("minecraft:redstone", 2)],
    "fire_control_module": [("minecraft:redstone", 8), ("minecraft:gold_ingot", 2), ("minecraft:quartz", 4), ("minecraft:glass_pane", 2), ("minecraft:diamond", 1)],
    "rotor_assembly": [(M + "titanium_plate", 4), (M + "steel_ingot", 6), ("minecraft:iron_ingot", 4)],
    "launcher_module": [(M + "steel_plate", 4), (M + "steel_ingot", 6), ("minecraft:redstone", 4)],
}
E, T, TR, W, CB, AC, FC, RO, LA, SP, TP = ("vehicle_engine", "gas_turbine", "track_set", "wheel_set", "cannon_barrel", "autocannon_mount",
                                          "fire_control_module", "rotor_assembly", "launcher_module", "steel_plate", "titanium_plate")
VEHICLE_RECIPES = {
    "uaz_469": {E: 1, W: 1, SP: 4}, "tigr": {E: 1, W: 1, SP: 10, AC: 1}, "kamaz": {E: 1, W: 2, SP: 6},
    "brdm_2": {E: 1, W: 1, SP: 14, AC: 1}, "btr_80": {E: 1, W: 2, SP: 18, AC: 1}, "bmp_2": {E: 1, TR: 1, SP: 20, AC: 1, FC: 1, LA: 1},
    "bm_21_grad": {E: 1, W: 2, SP: 6, LA: 2}, "zsu_23_4": {E: 1, TR: 1, SP: 16, AC: 2, FC: 2}, "mi_24": {T: 2, RO: 1, SP: 14, FC: 1, AC: 1, LA: 1},
    "t_90m": {E: 1, TR: 2, TP: 16, SP: 16, CB: 1, FC: 2}, "t_80bvm": {T: 1, TR: 2, TP: 14, SP: 16, CB: 1, FC: 1},
    "t_14_armata": {E: 1, TR: 2, TP: 24, SP: 16, CB: 1, FC: 3}, "bmpt_terminator": {E: 1, TR: 2, TP: 14, SP: 16, AC: 2, LA: 1, FC: 2},
    "sprut_sd": {E: 1, TR: 2, SP: 18, CB: 1, FC: 1}, "humvee": {E: 1, W: 1, SP: 8, AC: 1}, "m_atv": {E: 1, W: 1, SP: 14, AC: 1},
    "m939": {E: 1, W: 2, SP: 6}, "stryker": {E: 1, W: 2, SP: 18, AC: 1, FC: 1}, "m113": {E: 1, TR: 1, SP: 14, AC: 1},
    "m2a3_bradley": {E: 1, TR: 2, SP: 20, TP: 4, AC: 1, LA: 1, FC: 1}, "m1a2_abrams": {T: 1, TR: 2, TP: 20, SP: 18, CB: 1, FC: 2},
    "m142_himars": {E: 1, W: 2, SP: 8, LA: 2, FC: 2}, "avenger": {E: 1, W: 1, SP: 8, LA: 1, AC: 1, FC: 2},
    "uh_60_black_hawk": {T: 2, RO: 1, SP: 12, AC: 2}, "ah_64e_apache": {T: 2, RO: 1, TP: 8, SP: 10, AC: 1, LA: 2, FC: 2},
    "s_400": {E: 1, W: 3, SP: 12, LA: 4, FC: 4}, "s_500": {E: 1, W: 3, SP: 14, LA: 2, FC: 6}, "iskander": {E: 1, W: 3, SP: 14, LA: 2, FC: 4},
}


def assembly_recipes():
    for name, ing in PART_RECIPES.items():
        write(os.path.join(DATA, f"recipe/assembly/{name}.json"), {"type": "modeminecraft:armament", "category": "vehicle_parts",
            "ingredients": [{"item": i, "count": c} for i, c in ing], "result": {"id": f"{MOD}:{name}", "count": 1}})
    for name, ing in VEHICLE_RECIPES.items():
        write(os.path.join(DATA, f"recipe/assembly/{name}.json"), {"type": "modeminecraft:armament", "category": "vehicles",
            "ingredients": [{"item": f"{MOD}:{i}", "count": c} for i, c in ing.items()], "result": {"id": f"{MOD}:{name}", "count": 1}})


# ------------------------------------------------------------------------------------------- tenues d'infiltration
OUTFITS = {
    # nom : (couvre-chef, haut, détail du haut, pantalon, bottes, accent, motif)
    "police_uniform": ("#1c2a48", "#6f95c8", "#1c2a48", "#1c2a48", "#141414", "#e0b030", "plain"),
    "desert_boss": ("#b89a64", "#c4a46c", "#8a2018", "#b89a64", "#4a3020", "#e8c040", "plain"),
    "soviet_uniform": ("#4e5a32", "#56633a", "#a02020", "#4a552f", "#1a1a1a", "#c02020", "plain"),
    "special_forces": ("#5a6240", "#6a6a48", "#2a2e22", "#6a6a48", "#5a4630", "#3a3e2a", "camo"),
    "cartel_outfit": ("#e8e2d0", "#c83a3a", "#e8c040", "#e8e2d0", "#6a4428", "#2a8a5a", "floral"),
    "sheriff_outfit": ("#6a4a2a", "#c8a878", "#6a4a2a", "#3a5a8a", "#5a3a1a", "#e8d060", "plain"),
}


def outfit_px(r, base, motif, x, y):
    c = base
    if motif == "camo":
        v = (x * 7 + y * 13 + r.randint(0, 3)) % 9
        c = hexc("#4a5236") if v < 3 else hexc("#7a6a48") if v < 5 else base
    elif motif == "floral" and (x * 3 + y * 5) % 7 == 0:
        c = hexc("#e8d040") if (x + y) % 2 else hexc("#ffffff")
    return shade(c, 1 + r.uniform(-0.06, 0.06))


def armor_layers(name, spec):
    hat, top, detail, legs, boots, accent, motif = spec
    hat, top, detail, legs, boots, accent = (hexc(c) for c in (hat, top, detail, legs, boots, accent))
    r = random.Random(name)
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # tête (couvre-chef) : dessus + 4 côtés, partie haute ; visière à l'avant
    for x in range(0, 32):
        for y in range(0, 16):
            if y < 8 and not (8 <= x < 24):
                continue
            if y >= 8 and y > 12:
                continue                                             # bas du visage dégagé
            l1.putpixel((x, y), outfit_px(r, hat, motif if name == "special_forces" else "plain", x, y))
    for x in range(8, 16):
        l1.putpixel((x, 12), shade(hat, 0.6))                        # visière / bord
    l1.putpixel((11, 10), accent), l1.putpixel((12, 10), accent)     # insigne
    # corps (16..40, 16..32) et bras (40..56, 16..32)
    for x in range(16, 56):
        for y in range(16, 32):
            if y < 20 and not (20 <= x < 36 or 44 <= x < 52):
                continue
            l1.putpixel((x, y), outfit_px(r, top, motif, x, y))
    for y in range(20, 32):                                          # gilet / plastron / bretelles
        for x in (20, 21, 26, 27):
            l1.putpixel((x, y), shade(detail, 1.0))
    for x in range(20, 28):
        l1.putpixel((x, 29), shade(detail, 0.8))                     # ceinturon
    l1.putpixel((22, 22), accent), l1.putpixel((23, 22), accent)     # badge / étoile
    for x in (44, 45, 46, 47):
        l1.putpixel((x, 20), accent if name in ("desert_boss", "soviet_uniform") else shade(top, 0.85))   # épaulettes
    # bottes (bas des jambes, calque 1)
    for x in range(0, 16):
        for y in range(16, 32):
            if y >= 26 or y < 20 and 4 <= x < 12:
                if y < 20:
                    continue
                l1.putpixel((x, y), shade(boots, 1 + r.uniform(-0.06, 0.06)))
    # pantalon (calque 2) : taille du corps + jambes
    for x in range(0, 16):
        for y in range(16, 32):
            if y < 20 and not (4 <= x < 12):
                continue
            l2.putpixel((x, y), outfit_px(r, legs, motif if name == "special_forces" else "plain", x, y))
    for y in range(20, 32):
        if name in ("desert_boss", "soviet_uniform", "police_uniform"):
            l2.putpixel((0, y), accent if name != "police_uniform" else shade(legs, 0.7))   # bande latérale
    for x in range(16, 40):
        for y in range(26, 32):
            if 20 <= x < 28 or 32 <= x < 40 or 16 <= x < 20 or 28 <= x < 32:
                l2.putpixel((x, y), outfit_px(r, legs, "plain", x, y))
    d = os.path.join(ASSETS, "textures/models/armor")
    os.makedirs(d, exist_ok=True)
    l1.save(os.path.join(d, f"{name}_layer_1.png"))
    l2.save(os.path.join(d, f"{name}_layer_2.png"))


def outfit_icons(name, spec):
    hat, top, detail, legs, boots, accent, motif = (hexc(c) if c.startswith("#") else c for c in spec)
    r = random.Random(name + "i")
    i = Icon(name + "h")                                             # couvre-chef
    i.rect(3, 5, 12, 9, hat, noise=0.05)
    i.rect(2, 9, 13, 10, shade(hat, 0.7), bevel=False)
    i.rect(7, 6, 8, 7, accent, bevel=False)
    i.save(name + "_helmet")
    i = Icon(name + "c")                                             # haut
    i.rect(2, 2, 13, 14, top, noise=0.05)
    for y in range(2, 6):
        for x in range(6, 10):
            i.px(x, y, (0, 0, 0, 0))                                 # encolure
    for y in range(6, 14):
        i.px(5, y, detail), i.px(10, y, detail)
    i.px(4, 7, accent), i.px(11, 7, accent)
    i.rect(5, 12, 10, 12, shade(detail, 0.8), bevel=False)
    i.save(name + "_chestplate")
    i = Icon(name + "l")                                             # pantalon
    i.rect(3, 2, 12, 5, legs, noise=0.05)
    i.rect(3, 5, 6, 14, legs, noise=0.05)
    i.rect(9, 5, 12, 14, legs, noise=0.05)
    i.rect(3, 2, 12, 2, shade(legs, 0.7), bevel=False)
    i.save(name + "_leggings")
    i = Icon(name + "b")                                             # bottes
    i.rect(2, 4, 5, 12, boots, noise=0.05)
    i.rect(2, 11, 7, 13, boots, noise=0.05)
    i.rect(9, 4, 12, 12, boots, noise=0.05)
    i.rect(9, 11, 14, 13, boots, noise=0.05)
    i.save(name + "_boots")


def outfits():
    for name, spec in OUTFITS.items():
        armor_layers(name, spec)
        outfit_icons(name, spec)


# ------------------------------------------------------------------------------------------- chambres fortes des boss
def it(name, w, lo, hi=None):
    full = name if ":" in name else f"{MOD}:{name}"
    e = {"type": "minecraft:item", "name": full, "weight": w}
    if hi is not None and (lo, hi) != (1, 1):
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


VAULT_LOOT = {
    "swat_hq": [it("m4", 3, 1, 1), it("hk416", 3, 1, 1), it("mp5", 3, 1, 1), it("benelli_m4", 2, 1, 1), it("ammo_556", 6, 16, 48),
                it("ammo_9mm", 6, 16, 48), it("shotgun_shell", 3, 8, 24), it("swat_vest", 3, 1, 1), it("mich_2000", 3, 1, 1),
                it("flashbang", 4, 2, 5), it("crowbar", 2, 1, 1), it("first_aid_kit", 4, 1, 3), it("tactical_battery", 4, 4, 10)],
    "desert_palace": [it("ak47", 3, 1, 1), it("dragunov", 2, 1, 1), it("rpg7", 2, 1, 1), it("rpg_rocket", 3, 2, 5),
                      it("minecraft:gold_ingot", 6, 6, 20), it("minecraft:emerald", 6, 8, 24), it("steel_ballistic_plate", 3, 1, 3),
                      it("titanium_plate", 2, 1, 4), it("ammo_762", 6, 16, 48), it("military_ration", 3, 2, 6)],
    "forest_bunker": [it("wasp_mg", 2, 1, 1), it("reaper_x", 2, 1, 1), it("drone_controller", 2, 1, 1), it("emp_jammer_rifle", 2, 1, 1),
                      it("thermal_scope", 2, 1, 1), it("night_vision_scope", 2, 1, 1), it("c4", 4, 1, 4), it("mortar_shell", 4, 2, 8),
                      it("netherite_ballistic_plate", 2, 1, 2), it("titanium_plate", 3, 2, 6), it("hk416", 2, 1, 1), it("ammo_556", 5, 16, 48),
                      it("stryker", 1, 1, 1), it("humvee", 1, 1, 1)],
    "soviet_hq": [it("pkm", 3, 1, 1), it("pp19_bizon", 3, 1, 1), it("ak74u", 3, 1, 1), it("altyn", 3, 1, 1), it("ratnik_pants", 3, 1, 1),
                  it("fort_defender", 2, 1, 1), it("apfsds_shell", 4, 2, 6), it("he_shell", 4, 2, 6), it("atgm_missile", 3, 1, 3),
                  it("jerrycan", 4, 1, 3), it("titanium_ingot", 4, 3, 10), it("ammo_545", 5, 16, 48), it("ammo_762x54r", 4, 16, 40)],
    "cartel_villa": [it("cash_bundle", 8, 16, 48), it("minecraft:gold_block", 5, 2, 6), it("minecraft:emerald", 6, 10, 32),
                     it("scar_h", 3, 1, 1), it("desert_eagle", 3, 1, 1), it("spas12", 3, 1, 1), it("adrenaline_syringe", 3, 2, 5),
                     it("fpv_frelon", 2, 1, 2), it("c4", 3, 1, 3), it("ammo_50ae", 4, 12, 30)],
    "badlands_refinery": [it("barrett_m82", 2, 1, 1), it("colt_python", 3, 1, 1), it("c4", 3, 2, 5), it("minecraft:tnt", 4, 4, 12),
                          it("jerrycan", 6, 2, 6), it("vehicle_engine", 2, 1, 1), it("wheel_set", 2, 1, 1), it("track_set", 1, 1, 1),
                          it("steel_ingot", 5, 6, 16), it("minecraft:gold_ingot", 5, 6, 16), it("ammo_50bmg", 4, 8, 24), it("ammo_357", 4, 12, 30)],
}
VAULT_OUTFIT = {"swat_hq": "police_uniform", "desert_palace": "desert_boss", "forest_bunker": "special_forces",
                "soviet_hq": "soviet_uniform", "cartel_villa": "cartel_outfit", "badlands_refinery": "sheriff_outfit"}


def vault_loot():
    for vault, entries in VAULT_LOOT.items():
        write(os.path.join(DATA, f"loot_table/chests/boss_vault_{vault}.json"), {"type": "minecraft:chest", "pools": [
            {"rolls": {"type": "minecraft:uniform", "min": 6, "max": 9}, "entries": entries}]})
        o = VAULT_OUTFIT[vault]
        pieces = [f"{MOD}:{o}_{p}" for p in ("helmet", "chestplate", "leggings", "boots")]
        if vault == "desert_palace":
            # une seule pièce au hasard de la tenue du Boss du Désert par chambre forte
            outfit_pool = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": p, "weight": 1} for p in pieces]}
            pools = [outfit_pool]
        else:
            pools = [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": p}]} for p in pieces]
        pools.append({"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": entries})
        write(os.path.join(DATA, f"loot_table/chests/boss_vault_{vault}_outfit.json"), {"type": "minecraft:chest", "pools": pools})


CARD_COLORS = {"swat_hq": ("#2a5ab8", "#e8c040"), "desert_palace": ("#b02a20", "#e8c040"), "forest_bunker": ("#1a1a1a", "#3aa050"),
               "soviet_hq": ("#b01818", "#f0d020"), "cartel_villa": ("#d8a830", "#20a060"), "badlands_refinery": ("#b06a3a", "#1a1a1a")}


def boss_cards():
    for vault, (body, stripe) in CARD_COLORS.items():
        i = Icon("card" + vault)
        i.rect(1, 3, 14, 12, hexc(body), noise=0.04)
        i.rect(1, 5, 14, 6, hexc(stripe), bevel=False)                       # bande magnétique
        i.rect(3, 8, 5, 10, hexc("#d8c060"), bevel=False)                    # puce
        i.rect(8, 8, 12, 8, hexc("#f0f0f0"), bevel=False)
        i.rect(8, 10, 11, 10, hexc("#c0c0c0"), bevel=False)
        if vault == "soviet_hq":
            i.px(12, 10, hexc("#f0d020")), i.px(11, 9, hexc("#f0d020"))      # étoile
        i.save("boss_card_" + vault)
    for vault in CARD_COLORS:
        door = f"boss_vault_door_{vault}"
        src = os.path.join(ASSETS, "blockstates/vault_door_black.json")
        with open(src) as f:
            st = json.load(f)
        write(os.path.join(ASSETS, f"blockstates/{door}.json"), st)
    write(os.path.join(ASSETS, "blockstates/vault_wall.json"), {"variants": {"": {"model": f"{MOD}:block/vault_wall"}}})
    write(os.path.join(ASSETS, "models/block/vault_wall.json"), {"parent": "minecraft:block/cube_column", "textures": {
        "side": "minecraft:block/reinforced_deepslate_side", "end": "minecraft:block/reinforced_deepslate_top"}})


# ------------------------------------------------------------------------------------------- lance-roquettes statiques
def rocket_launchers():
    """Type 63 (trépied, 4 x 107 mm) : icônes, texture d'entité, recettes ; roquette de 107 mm ; tuyau en fer."""
    olive, red, steel, dark = hexc("#5a6a38"), hexc("#b02a20"), hexc("#8a8e92"), hexc("#3a3e34")
    i = Icon("rocket_107")
    diag(i, 11, 2, [(0.15, hexc("#3a3a3a")), (0.85, olive), (1, red)], start=(2, 13))
    i.px(2, 11, olive), i.px(4, 13, olive)
    i.save("rocket_107")
    i = Icon("iron_pipe")
    diag(i, 13, 2, [(0.1, shade(steel, 0.7)), (0.9, steel), (1, shade(steel, 0.7))], start=(1, 14))
    i.save("iron_pipe")
    i = Icon("type63_launcher")
    for x0, y0 in ((3, 13), (8, 14), (12, 13)):                          # trépied
        for t in range(5):
            i.px(x0 + (8 - x0) * t // 5, y0 - t, shade(steel, 0.8))
    for k in range(4):                                                    # 4 tubes inclinés
        for t in range(9):
            x, y = 2 + t + (k % 2), 8 - t // 2 - (k // 2) * 2 + 1
            if 0 <= x < 16 and 0 <= y < 16:
                i.px(x, y, shade(olive, 1.1 if k % 2 else 0.9))
    i.px(11, 3, red), i.px(12, 3, red)
    i.save("type63_launcher")
    # texture des volumes de l'entité : vert olive mat, légèrement bruité, liserés plus sombres
    img = Image.new("RGBA", (16, 16))
    r = random.Random("rocket_launcher")
    for y in range(16):
        for x in range(16):
            c = shade(olive, 1 + r.uniform(-0.07, 0.07))
            if x in (0, 15) or y in (0, 15):
                c = shade(c, 0.75)
            img.putpixel((x, y), c)
    p = os.path.join(ASSETS, "textures/entity/rocket_launcher.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    # recettes : roquettes à l'établi d'artillerie, lanceur et tuyaux à l'établi
    write(os.path.join(DATA, "recipe/artillery/rocket_107.json"), {"type": f"{MOD}:armament", "category": "artillery",
        "ingredients": [{"item": "minecraft:iron_ingot", "count": 2}, {"item": "minecraft:tnt", "count": 1}, {"item": "minecraft:gunpowder", "count": 3}],
        "result": {"id": f"{MOD}:rocket_107", "count": 3}})
    write(os.path.join(DATA, "recipe/iron_pipe.json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["I", "I", "I"],
        "key": {"I": {"item": "minecraft:iron_ingot"}}, "result": {"id": f"{MOD}:iron_pipe", "count": 2}})
    write(os.path.join(DATA, "recipe/type63_launcher.json"), {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["S S", "PRP", "SLS"], "key": {"S": {"item": f"{MOD}:steel_ingot"}, "P": {"item": f"{MOD}:iron_pipe"},
                                              "R": {"item": "minecraft:redstone"}, "L": {"item": "minecraft:lever"}},
        "result": {"id": f"{MOD}:type63_launcher", "count": 1}})


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "launchers":
        rocket_launchers()
        sys.exit(0)
    loot_icons()
    vault_loot()
    boss_cards()
    outfits()
    part_icons()
    assembly_table()
    assembly_recipes()
    artillery_table()
    artillery_recipes()
    heavy_ammo_icons()
    strategic_icons()
    mine_icons()
    shell_texture()
    print("ok")

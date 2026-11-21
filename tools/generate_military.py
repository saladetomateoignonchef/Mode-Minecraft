#!/usr/bin/env python3
"""
Mise à jour 0.4 « Véhicules militaires & missiles » : icônes des objets (butin des braquages, munitions lourdes,
désignateur, mines...), modèles d'objets et données. Utilisation : python3 tools/generate_military.py
"""
import json
import os
import random

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
    "jerrycan": (1, [("minecraft:iron_ingot", 3), ("minecraft:coal", 4), ("minecraft:dried_kelp_block", 1)]),
    "laser_designator": (1, [("modeminecraft:steel_ingot", 2), ("minecraft:redstone", 6), ("minecraft:glass_pane", 2), ("minecraft:diamond", 1)]),
}


def artillery_recipes():
    for name, (n, ing) in ARTILLERY_RECIPES.items():
        write(os.path.join(DATA, f"recipe/artillery/{name}.json"), {"type": "modeminecraft:armament", "category": "artillery",
            "ingredients": [{"item": i, "count": c} for i, c in ing], "result": {"id": f"{MOD}:{name}", "count": n}})


if __name__ == "__main__":
    loot_icons()
    artillery_table()
    artillery_recipes()
    heavy_ammo_icons()
    strategic_icons()
    shell_texture()
    print("ok")

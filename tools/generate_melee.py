#!/usr/bin/env python3
"""
Icônes 16x16 (pixel art style Minecraft) des 20 armes blanches du cahier des charges.
Chaque arme est dessinée en diagonale : manche en bas à gauche, pointe en haut à droite.
Utilisation : python3 tools/generate_melee.py
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
OUT = os.path.join(ASSETS, "textures/item")

STEEL = [(220, 225, 232), (170, 176, 186), (110, 116, 126)]   # clair, moyen, sombre
DARK = [(90, 92, 98), (60, 62, 68), (35, 36, 40)]
WOOD = [(150, 105, 60), (115, 78, 42), (80, 52, 28)]
LEATHER = [(110, 70, 45), (80, 50, 32), (55, 34, 22)]
BLACK = [(70, 72, 78), (45, 46, 52), (25, 26, 30)]
GOLD = [(250, 215, 90), (219, 165, 33), (150, 105, 15)]
RED = [(230, 80, 90), (200, 34, 59), (120, 15, 30)]
YELLOW = [(250, 230, 90), (220, 190, 40), (150, 120, 20)]
BLUE = [(150, 220, 255), (90, 160, 240), (40, 90, 180)]
OUTLINE = (20, 20, 24, 255)


class Canvas:
    def __init__(self):
        self.img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def px(self, x, y, c):
        if isinstance(c, list):
            c = c[1]  # palette entière : teinte moyenne
        if 0 <= x < 16 and 0 <= y < 16:
            self.img.putpixel((x, y), c + (255,) if len(c) == 3 else c)

    def diag(self, a, b, pal, width=1, offset=0):
        """Segment le long de la diagonale (x = i, y = 15 - i) de a à b, épaisseur width (vers le bas-droite)."""
        for i in range(a, b + 1):
            x, y = i, 15 - i
            for w in range(width):
                c = pal[0] if w == 0 else (pal[1] if w < width - 1 else pal[2])
                self.px(x + w + offset, y + offset, c)

    def perp(self, i, half, pal):
        """Garde : petit segment perpendiculaire à la diagonale, centré sur la position i."""
        for k in range(-half, half + 1):
            self.px(i + k, 15 - i + k, pal[1] if k else pal[0])

    def outline(self):
        src = self.img.copy()
        for y in range(16):
            for x in range(16):
                if src.getpixel((x, y))[3] == 0 and any(
                        0 <= x + dx < 16 and 0 <= y + dy < 16 and src.getpixel((x + dx, y + dy))[3] > 0
                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    self.img.putpixel((x, y), OUTLINE)
        return self.img


def blade(c, a, b, pal=STEEL, width=2):
    c.diag(a, b, pal, width)


def make(name):
    c = Canvas()
    if name == "combat_knife":
        c.diag(1, 5, BLACK, 2); c.perp(5, 2, DARK); blade(c, 6, 11)
    elif name == "karambit":
        c.diag(2, 6, BLACK, 2)
        c.px(1, 14, DARK); c.px(1, 13, DARK); c.px(2, 14, DARK)  # anneau
        for (x, y) in [(7, 8), (8, 7), (9, 7), (10, 7), (11, 8), (12, 9), (12, 10)]:
            c.px(x, y, STEEL[0]); c.px(x, y + 1, STEEL[1])
    elif name == "katana":
        c.diag(1, 4, BLACK, 2); c.perp(5, 2, GOLD); c.diag(6, 14, STEEL, 2)
        for i in range(1, 5, 2):
            c.px(i, 15 - i, RED[1])
    elif name == "mace":
        c.diag(1, 8, WOOD, 2)
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                if abs(dx) + abs(dy) <= 3:
                    c.px(11 + dx, 4 + dy, STEEL[1] if (dx + dy) % 2 else STEEL[0])
        for (x, y) in [(11, 0), (14, 4), (11, 8), (8, 4)]:
            c.px(x, y, STEEL[2])
    elif name == "war_hammer":
        c.diag(1, 9, WOOD, 2)
        for dx in range(-3, 4):
            for dy in range(-1, 2):
                c.px(11 + dx - dy, 4 + dy - dx, STEEL[1] if dy else STEEL[0])
    elif name == "tomahawk":
        c.diag(1, 10, WOOD, 2)
        for (x, y) in [(9, 3), (10, 2), (8, 4), (9, 4), (10, 3), (11, 3), (8, 5), (12, 5), (13, 6)]:
            c.px(x, y, STEEL[0])
        c.px(13, 5, STEEL[1]); c.px(12, 4, STEEL[1])
    elif name == "stun_baton":
        c.diag(1, 4, BLACK, 2); c.diag(5, 13, DARK, 2)
        for i in (7, 10, 13):
            c.px(i, 15 - i, YELLOW[0])
        c.px(14, 1, BLUE[0]); c.px(13, 0, BLUE[1])
    elif name == "machete":
        c.diag(1, 4, BLACK, 2); c.diag(5, 13, STEEL, 3); c.px(14, 1, STEEL[1])
    elif name == "golden_machete":
        c.diag(1, 4, [(40, 30, 20), (28, 20, 14), (18, 12, 8)], 2); c.perp(5, 1, GOLD); c.diag(6, 13, GOLD, 3); c.px(14, 1, GOLD[0])
        for i in (8, 10, 12):
            c.px(i + 1, 15 - i, (255, 245, 190))                        # gravures
    elif name == "kukri":
        c.diag(1, 4, WOOD, 2)
        for (x, y) in [(5, 9), (6, 8), (7, 7), (8, 7), (9, 7), (10, 6), (11, 5), (12, 4), (6, 9), (7, 8), (8, 8), (9, 8), (10, 7), (11, 6)]:
            c.px(x, y, STEEL[0] if y < 8 else STEEL[1])
    elif name == "wakizashi":
        c.diag(2, 5, BLACK, 2); c.perp(6, 1, GOLD); c.diag(7, 13, STEEL, 2)
    elif name == "dagger":
        c.diag(3, 6, LEATHER, 2); c.perp(7, 2, GOLD); c.diag(8, 12, STEEL, 2)
    elif name == "claymore":
        c.diag(0, 3, LEATHER, 2); c.perp(4, 3, STEEL); c.diag(5, 15, STEEL, 3)
    elif name == "rapier":
        c.diag(1, 4, LEATHER, 2)
        for (x, y) in [(3, 13), (2, 12), (4, 13), (5, 12), (3, 11)]:
            c.px(x, y, GOLD[0])
        c.perp(5, 2, GOLD); c.diag(6, 15, STEEL, 1)
    elif name == "scimitar":
        c.diag(1, 4, GOLD, 2); c.perp(5, 2, GOLD)
        for (x, y) in [(6, 9), (7, 8), (8, 7), (9, 6), (10, 6), (11, 5), (12, 5), (13, 5), (7, 9), (8, 8), (9, 7), (10, 7), (11, 6), (12, 6)]:
            c.px(x, y, STEEL[0] if (x + y) % 2 else STEEL[1])
    elif name == "lance":
        c.diag(0, 11, WOOD, 1); c.diag(12, 15, STEEL, 2); c.perp(11, 1, DARK)
    elif name == "halberd":
        c.diag(0, 11, WOOD, 1); c.diag(12, 15, STEEL, 1)
        for (x, y) in [(10, 3), (9, 3), (9, 2), (12, 6), (13, 6), (13, 7), (11, 3), (10, 2)]:
            c.px(x, y, STEEL[0])
    elif name == "war_scythe":
        c.diag(0, 12, WOOD, 1)
        for (x, y) in [(13, 2), (12, 1), (11, 1), (10, 1), (9, 2), (8, 2), (7, 3), (11, 2), (10, 2)]:
            c.px(x, y, STEEL[0] if y == 1 else STEEL[1])
    elif name == "brass_knuckles":
        for x in range(3, 13):
            c.px(x, 6, GOLD[1]); c.px(x, 9, GOLD[2])
        for x in (4, 7, 10):
            for y in range(4, 7):
                c.px(x, y, GOLD[0]); c.px(x + 1, y, GOLD[0])
            c.px(x, 5, (0, 0, 0, 0)) if False else None
        for y in range(6, 10):
            c.px(3, y, GOLD[1]); c.px(12, y, GOLD[1])
    elif name == "crowbar":
        c.diag(1, 13, RED, 2)
        c.px(14, 1, RED[1]); c.px(14, 2, RED[2]); c.px(13, 3, RED[2])
        c.px(1, 13, RED[2]); c.px(0, 13, RED[2])
    elif name == "cutlass":
        c.diag(1, 3, WOOD, 2)
        for (x, y) in [(2, 11), (3, 10), (4, 10), (5, 10), (2, 12)]:
            c.px(x, y, GOLD[1])
        c.perp(4, 2, GOLD); c.diag(5, 13, STEEL, 2)
    return c.outline()


NAMES = ["combat_knife", "karambit", "katana", "mace", "war_hammer", "tomahawk", "stun_baton", "machete", "kukri",
         "wakizashi", "dagger", "claymore", "rapier", "scimitar", "lance", "halberd", "war_scythe", "brass_knuckles",
         "crowbar", "cutlass", "golden_machete"]


def main():
    for n in NAMES:
        make(n).save(os.path.join(OUT, f"{n}.png"))
        with open(os.path.join(ASSETS, "models/item", f"{n}.json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": f"modeminecraft:item/{n}"}}, f, indent=2)
            f.write("\n")
    print(f"{len(NAMES)} armes blanches")


if __name__ == "__main__":
    main()

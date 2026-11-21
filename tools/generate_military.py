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


if __name__ == "__main__":
    loot_icons()
    print("ok")

#!/usr/bin/env python3
"""
Icônes 2D plus détaillées : passe les textures 16x16 (objets, blocs) et 64x32 (couche d'armure) en résolution double,
façon Minecraft (pixel art, sans flou ni lissage) :

1. agrandissement Scale2x (EPX) : les escaliers en diagonale deviennent des courbes en pixels (rond mais pas lisse),
   sans inventer de nouvelles couleurs ;
2. relief : chaque pixel est éclairé selon sa position dans la forme (lumière en haut à gauche) : bord haut/gauche
   plus clair, bord bas/droit plus sombre, cœur légèrement bombé ;
3. contour sélectif : un liseré foncé de la couleur du bord (pas un noir uniforme) autour des objets ;
4. grain de matière : légère variation pixel par pixel (métal, bois, tissu ne sont jamais d'une couleur plate).

Ne traite que les fichiers encore en basse résolution : on peut le relancer sans risque après les autres générateurs.
Utilisation : python3 tools/enhance_icons.py
"""
import glob
import os
import zlib

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX = os.path.join(ROOT, "src/main/resources/assets/modeminecraft/textures")


def rnd(*a):
    return (zlib.crc32(repr(a).encode()) & 0xFFFF) / 0xFFFF


def clamp8(v):
    return max(0, min(255, int(round(v))))


def scale2x(img):
    """EPX / Scale2x : agrandissement x2 qui arrondit les diagonales (pixel art)."""
    w, h = img.size
    src = img.load()
    out = Image.new("RGBA", (w * 2, h * 2))
    dst = out.load()

    def p(x, y):
        x = min(max(x, 0), w - 1)
        y = min(max(y, 0), h - 1)
        c = src[x, y]
        return c if c[3] else (0, 0, 0, 0)

    for y in range(h):
        for x in range(w):
            e = p(x, y)
            b, d, f, hh = p(x, y - 1), p(x - 1, y), p(x + 1, y), p(x, y + 1)
            e0 = d if d == b and b != f and d != hh else e
            e1 = f if b == f and b != d and f != hh else e
            e2 = d if d == hh and d != b and hh != f else e
            e3 = f if hh == f and d != hh and b != f else e
            dst[2 * x, 2 * y] = e0
            dst[2 * x + 1, 2 * y] = e1
            dst[2 * x, 2 * y + 1] = e2
            dst[2 * x + 1, 2 * y + 1] = e3
    return out


def depth_map(img):
    """Distance (en pixels, plafonnée) de chaque pixel opaque au vide le plus proche."""
    w, h = img.size
    px = img.load()
    big = 99
    dist = [[0 if px[x, y][3] < 40 else big for x in range(w)] for y in range(h)]
    for _ in range(4):
        for y in range(h):
            for x in range(w):
                if dist[y][x] == 0:
                    continue
                m = min(dist[y + dy][x + dx] if 0 <= x + dx < w and 0 <= y + dy < h else 0
                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                dist[y][x] = min(dist[y][x], m + 1)
    return dist


def shade_item(img, name):
    """Relief + contour sélectif + grain, pour une icône d'objet (fond transparent)."""
    w, h = img.size
    px = img.load()
    dist = depth_map(img)

    def solid(x, y):
        return 0 <= x < w and 0 <= y < h and px[x, y][3] >= 40

    out = Image.new("RGBA", (w, h))
    op = out.load()
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            if c[3] < 40:
                # contour sélectif : couleur du voisin, très assombrie
                nb = [px[x + dx, y + dy] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if solid(x + dx, y + dy)]
                if nb:
                    r = sum(n[0] for n in nb) / len(nb)
                    g = sum(n[1] for n in nb) / len(nb)
                    b = sum(n[2] for n in nb) / len(nb)
                    op[x, y] = (clamp8(r * 0.32), clamp8(g * 0.32), clamp8(b * 0.34), 255)
                continue
            # pente de la "hauteur" (distance au bord) -> éclairage venant d'en haut à gauche
            def d(xx, yy):
                return dist[yy][xx] if 0 <= xx < w and 0 <= yy < h else 0
            gx = d(x + 1, y) - d(x - 1, y)
            gy = d(x, y + 1) - d(x, y - 1)
            light = (gx + gy) * 0.06                      # pente vers le bas/droite : face éclairée
            f = 1 + max(-0.22, min(0.22, light))
            if not solid(x, y - 1) or not solid(x - 1, y):
                f *= 1.18                                 # arête haute / gauche : reflet
            elif not solid(x, y + 1) or not solid(x + 1, y):
                f *= 0.76                                 # arête basse / droite : ombre
            if dist[y][x] >= 3:
                f *= 1.03                                 # cœur légèrement bombé
            f *= 1 + (rnd(name, x, y) - 0.5) * 0.08       # grain de matière
            op[x, y] = (clamp8(c[0] * f), clamp8(c[1] * f), clamp8(c[2] * f), 255)
    return out


def shade_flat(img, name, bevel=True):
    """Pour les blocs et les couches d'armure : pas de contour, grain + petit relief entre zones de couleur."""
    w, h = img.size
    px = img.load()
    out = img.copy()
    op = out.load()
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            if c[3] == 0:
                continue
            f = 1 + (rnd(name, x, y) - 0.5) * 0.1
            if bevel:
                lum = sum(c[:3])
                up = px[x, y - 1] if y > 0 else c
                down = px[x, y + 1] if y < h - 1 else c
                if up[3] and sum(up[:3]) < lum - 60:
                    f *= 1.12                              # bord haut d'une zone claire : éclairé
                elif down[3] and sum(down[:3]) < lum - 60:
                    f *= 0.9
            op[x, y] = (clamp8(c[0] * f), clamp8(c[1] * f), clamp8(c[2] * f), c[3])
    return out


def main():
    n = 0
    for path in sorted(glob.glob(os.path.join(TEX, "item", "*.png")) + glob.glob(os.path.join(TEX, "item", "ammo_badge", "*.png"))):
        img = Image.open(path).convert("RGBA")
        if img.size != (16, 16):
            continue
        name = os.path.basename(path)
        shade_item(scale2x(img), name).save(path)
        n += 1
    for path in sorted(glob.glob(os.path.join(TEX, "block", "*.png"))):
        img = Image.open(path).convert("RGBA")
        if img.size != (16, 16):
            continue
        shade_flat(scale2x(img), os.path.basename(path)).save(path)
        n += 1
    for path in sorted(glob.glob(os.path.join(TEX, "models", "armor", "*.png"))):
        img = Image.open(path).convert("RGBA")
        if img.size != (64, 32):
            continue
        shade_flat(scale2x(img), os.path.basename(path), bevel=False).save(path)
        n += 1
    print(f"{n} textures passées en haute définition")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Génère toutes les textures (pixel art 16x16) du mod Mode Minecraft.

Utilisation (depuis la racine du projet) :
    python3 tools/generate_textures.py

Les textures sont des "placeholders" corrects : tu peux les remplacer à tout moment
par tes propres dessins (Aseprite, Paint.NET, Blockbench...) en gardant le même nom de fichier.
Attention : relancer ce script écrase les PNG existants.
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX = os.path.join(ROOT, "src/main/resources/assets/modeminecraft/textures")


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def palette(outline, dark, mid, light, highlight):
    return {"o": hexc(outline), "d": hexc(dark), "m": hexc(mid), "l": hexc(light), "h": hexc(highlight)}


# ---------- Palettes ----------
RUBIS = palette("#3d0710", "#8a1426", "#c8223b", "#ef4f69", "#ffc2cc")
WOOD = palette("#2a1d0c", "#5a3e1b", "#7d5a2c", "#9c7740", "#b89458")
GOLD = palette("#4a3205", "#a8780e", "#dba521", "#f5d54a", "#fff3a8")
FIRE = palette("#4a1200", "#c23a00", "#f07a10", "#ffb530", "#fff1a0")
FROST = palette("#0e2f4a", "#3f86b8", "#7cc4ea", "#bfe9ff", "#ffffff")
VENOM = palette("#0d3010", "#2f7a22", "#55b335", "#93e05a", "#e0ffb0")
ENDER = palette("#120a1f", "#3b1d5c", "#6a35a0", "#a76ae0", "#e3c8ff")
TEAL = palette("#05302a", "#0f6b5c", "#1f9c86", "#4fd1b6", "#c4fff2")
STEEL = palette("#1b2430", "#4a5d75", "#7189a8", "#a7bfdb", "#e6f1ff")
SILVER = palette("#26282c", "#6b7078", "#9ca2ab", "#c9ced6", "#f4f6f8")
STRING = hexc("#e8e8e8")

SQ2 = math.sqrt(2)
D1 = (1 / SQ2, -1 / SQ2)  # axe de l'arme : du coin bas-gauche vers le coin haut-droit
D2 = (1 / SQ2, 1 / SQ2)   # axe perpendiculaire
ORIGIN = (1.0, 15.0)      # point de départ (bas-gauche)


class Canvas:
    """Grille 16x16 : on y "peint" des formes, puis on ajoute un contour automatique."""

    def __init__(self, size=16):
        self.size = size
        self.px = {}       # (x, y) -> couleur
        self.outline = {}  # (x, y) -> couleur du contour de la forme

    def put(self, x, y, color, outline_color):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[(x, y)] = color
            self.outline[(x, y)] = outline_color

    def erase(self, x, y):
        self.px.pop((x, y), None)
        self.outline.pop((x, y), None)

    def image(self, add_outline=True):
        img = Image.new("RGBA", (self.size, self.size), (0, 0, 0, 0))
        if add_outline:
            for (x, y), oc in list(self.outline.items()):
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    n = (x + dx, y + dy)
                    if 0 <= n[0] < self.size and 0 <= n[1] < self.size and n not in self.px:
                        img.putpixel(n, oc)
        for (x, y), c in self.px.items():
            img.putpixel((x, y), c)
        return img


def shade(pal, t):
    """t de -1 (côté éclairé) à +1 (côté ombré)."""
    if t < -0.55:
        return pal["l"]
    if t < 0.25:
        return pal["m"]
    return pal["d"]


def rot_rect(cv, center, half_len, half_wid, pal, taper_start=0.0, taper_end=0.0, axis=D1, shade_axis=D2):
    """Rectangle tourné à 45°. On teste le centre de chaque pixel -> pas de trous en damier."""
    cx, cy = center
    for y in range(cv.size):
        for x in range(cv.size):
            px, py = x + 0.5 - cx, y + 0.5 - cy
            s = px * axis[0] + py * axis[1]
            t = px * shade_axis[0] + py * shade_axis[1]
            if abs(s) > half_len:
                continue
            w = half_wid
            if taper_end and s > half_len - taper_end:
                w *= max(0.0, (half_len - s) / taper_end) + 0.15
            if taper_start and s < -half_len + taper_start:
                w *= max(0.0, (s + half_len) / taper_start) + 0.15
            if abs(t) <= w:
                cv.put(x, y, shade(pal, t / max(w, 0.01)), pal["o"])


def along(s):
    """Point situé à la distance s le long de la diagonale principale."""
    return ORIGIN[0] + D1[0] * s, ORIGIN[1] + D1[1] * s


def seg(cv, s0, s1, half_wid, pal, taper_end=0.0, taper_start=0.0):
    rot_rect(cv, along((s0 + s1) / 2), (s1 - s0) / 2, half_wid, pal, taper_start, taper_end)


def cross(cv, s, half_len, half_wid, pal, offset=0.0, taper=0.0):
    """Barre perpendiculaire à l'arme (garde, tête de marteau...). offset la décale sur le côté."""
    cx, cy = along(s)
    cx, cy = cx + D2[0] * offset, cy + D2[1] * offset
    rot_rect(cv, (cx, cy), half_len, half_wid, pal, taper, taper, axis=D2, shade_axis=D1)


def circle(cv, center, r, pal):
    cx, cy = center
    for y in range(cv.size):
        for x in range(cv.size):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d <= r:
                t = (dx + dy) / (r * 1.2)
                c = pal["h"] if (dx < -r * 0.25 and dy < -r * 0.25 and d < r * 0.6) else shade(pal, t)
                cv.put(x, y, c, pal["o"])


def sparkle(cv, x, y, pal):
    if (x, y) in cv.px:
        cv.px[(x, y)] = pal["h"]


def save(img, *path):
    full = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


# ---------- Armes ----------
def sword(blade, guard, handle=WOOD, blade_len=18.6, blade_wid=1.05, guard_len=2.4):
    cv = Canvas()
    seg(cv, 0.2, 1.4, 0.75, guard)                      # pommeau
    seg(cv, 1.4, 5.0, 0.6, handle)                      # poignée
    seg(cv, 5.6, blade_len, blade_wid, blade, taper_end=3.0)  # lame
    cross(cv, 5.4, guard_len, 0.75, guard)              # garde
    x, y = along(blade_len - 4)
    sparkle(cv, int(x), int(y), blade)
    return cv.image()


def dagger(blade, guard, handle=WOOD):
    return sword(blade, guard, handle, blade_len=13.0, blade_wid=1.0, guard_len=1.8)


def hammer(head, band, handle=WOOD):
    cv = Canvas()
    seg(cv, 0.0, 13.5, 0.6, handle)
    cross(cv, 14.2, 4.6, 2.3, head)
    cross(cv, 14.2, 4.6, 0.6, band)
    seg(cv, 17.2, 18.0, 0.6, head)  # petite pointe sur le dessus
    return cv.image()


def pickaxe():
    cv = Canvas()
    seg(cv, 0.0, 13.0, 0.6, WOOD)
    cross(cv, 13.4, 6.6, 1.0, RUBIS, taper=3.0)
    return cv.image()


def axe():
    cv = Canvas()
    seg(cv, 0.0, 15.0, 0.6, WOOD)
    cross(cv, 12.2, 2.8, 2.6, RUBIS, offset=-2.6, taper=0.6)
    return cv.image()


def shovel():
    cv = Canvas()
    seg(cv, 0.0, 11.0, 0.6, WOOD)
    seg(cv, 11.0, 17.8, 1.8, RUBIS, taper_end=2.2)
    return cv.image()


def hoe():
    cv = Canvas()
    seg(cv, 0.0, 14.0, 0.6, WOOD)
    cross(cv, 14.0, 2.4, 1.0, RUBIS, offset=-1.6)
    return cv.image()


def fire_staff():
    cv = Canvas()
    seg(cv, 0.0, 14.0, 0.65, WOOD)
    cross(cv, 12.6, 1.6, 0.6, GOLD)
    cx, cy = along(16.2)
    circle(cv, (cx, cy), 2.6, FIRE)
    return cv.image()


# ---------- Arc ----------
def bow(pull=None):
    """pull = None (au repos) ou 0/1/2 (étapes de tension)."""
    cv = Canvas()
    center, r = (1.5, 14.5), 11.6
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - center[0], center[1] - (y + 0.5)
            d = math.hypot(dx, dy)
            ang = math.degrees(math.atan2(dy, dx))
            if abs(d - r) <= 0.75 and 4 <= ang <= 86:
                mid = 34 <= ang <= 56
                pal = GOLD if mid else RUBIS
                cv.put(x, y, shade(pal, (d - r) / 0.75), pal["o"])
    img = cv.image()

    # Corde : de l'extrémité haute à l'extrémité basse, tirée vers le bas-gauche selon la tension
    top, bottom = (3, 3), (12, 12)
    back = [0, 2, 3, 4][0 if pull is None else pull + 1]
    nock = (7.5 - back * 0.7, 7.5 + back * 0.7)
    for a, b in ((top, nock), (nock, bottom)):
        steps = 24
        for i in range(steps + 1):
            x = a[0] + (b[0] - a[0]) * i / steps
            y = a[1] + (b[1] - a[1]) * i / steps
            img.putpixel((int(round(x)), int(round(y))), STRING)

    if pull is not None:
        # Flèche : du point d'encoche vers le haut-droit
        nx, ny = int(round(nock[0])), int(round(nock[1]))
        length = 8 + back
        for i in range(length):
            x, y = nx + i, ny - i
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), hexc("#8a6a3a"))
        tx, ty = nx + length - 1, ny - length + 1
        for (x, y) in ((tx, ty), (tx - 1, ty), (tx, ty + 1)):
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), hexc("#b8bcc4"))
        for (x, y) in ((nx, ny - 1), (nx + 1, ny)):
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), hexc("#f2f2f2"))
    return img


# ---------- Rubis (gemme) ----------
def gem():
    cv = Canvas()
    spans = {3: (5, 10), 4: (4, 11), 5: (3, 12), 6: (2, 13), 7: (2, 13),
             8: (3, 12), 9: (4, 11), 10: (5, 10), 11: (6, 9), 12: (7, 8)}
    for y, (a, b) in spans.items():
        for x in range(a, b + 1):
            k = (x - 2) + (y - 3)  # diagonale : en haut à gauche = clair
            if y <= 4:
                c = RUBIS["l"] if x < 9 else RUBIS["m"]
            elif k < 6:
                c = RUBIS["l"]
            elif k < 12:
                c = RUBIS["m"]
            else:
                c = RUBIS["d"]
            cv.put(x, y, c, RUBIS["o"])
    for p in ((5, 4), (4, 5), (5, 5)):
        cv.px[p] = RUBIS["h"]
    return cv.image()


# ---------- Armure (icônes d'inventaire) ----------
ARMOR_ICONS = {
    "rubis_helmet": [
        "................",
        "................",
        "................",
        ".....hllllm.....",
        "....hlllmmmm....",
        "...llmmmmmmmd...",
        "...lmmmmmmmmd...",
        "...lmmddddmmd...",
        "...lmd....dmd...",
        "...lmd....dmd...",
        "...md......dd...",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "rubis_chestplate": [
        "................",
        "................",
        "..llm......mmd..",
        ".hllmm....mmmdd.",
        ".llllmmmmmmmmdd.",
        ".lllmmmmmmmmmdd.",
        "..llmmmmmmmmdd..",
        "...lmmmmmmmmd...",
        "...lmmmddmmmd...",
        "...lmmmmmmmmd...",
        "...lmmmddmmmd...",
        "...lmmmmmmmmd...",
        "...llmmmmmmdd...",
        "................",
        "................",
        "................",
    ],
    "rubis_leggings": [
        "................",
        "................",
        "...hllllllmmd...",
        "...lmmmmmmmmd...",
        "...lmmmmmmmmd...",
        "...lmmm..mmmd...",
        "...lmmd..lmmd...",
        "...lmmd..lmmd...",
        "...lmmd..lmmd...",
        "...lmmd..lmmd...",
        "...lmmd..lmmd...",
        "...lmmd..lmmd...",
        "...ddd....ddd...",
        "................",
        "................",
        "................",
    ],
    "rubis_boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "..hld.....lmd...",
        "..lmd.....lmd...",
        "..lmd.....lmd...",
        "..lmmd....lmmd..",
        ".llmmd...llmmd..",
        ".lmmdd...lmmdd..",
        "................",
        "................",
        "................",
        "................",
    ],
}


def ascii_icon(rows, pal):
    cv = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                cv.put(x, y, pal[ch], pal["o"])
    return cv.image()


# ---------- Blocs ----------
def stone_base(rng, colors, streaks=False):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            r = rng.random()
            if streaks:
                r = (r + (0.35 if (y + (x // 5)) % 4 == 0 else 0)) % 1.0
            img.putpixel((x, y), colors[min(int(r * len(colors)), len(colors) - 1)])
    return img


def ore(seed, base_colors, streaks=False):
    rng = random.Random(seed)
    img = stone_base(rng, base_colors, streaks)
    clusters = [(3, 3), (10, 2), (12, 9), (4, 11), (8, 7)]
    for cx, cy in clusters:
        cells = [(cx, cy), (cx + 1, cy), (cx, cy + 1), (cx + 1, cy + 1)]
        if rng.random() < 0.6:
            cells.append((cx + rng.choice([-1, 2]), cy + rng.choice([0, 1])))
        for (x, y) in cells:
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), RUBIS["m"])
        img.putpixel((cx, cy), RUBIS["l"])
        if 0 <= cx + 1 < 16 and 0 <= cy + 1 < 16:
            img.putpixel((cx + 1, cy + 1), RUBIS["d"])
    return img


def rubis_block():
    img = Image.new("RGBA", (16, 16), RUBIS["m"])
    for i in range(16):
        img.putpixel((i, 0), RUBIS["l"])
        img.putpixel((0, i), RUBIS["l"])
        img.putpixel((i, 15), RUBIS["o"])
        img.putpixel((15, i), RUBIS["o"])
    for i in range(1, 15):
        img.putpixel((i, 14), RUBIS["d"])
        img.putpixel((14, i), RUBIS["d"])
        img.putpixel((i, 1), RUBIS["h"] if i < 5 else RUBIS["l"])
        img.putpixel((1, i), RUBIS["h"] if i < 5 else RUBIS["l"])
    # Facettes en losange au centre
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            if 3.0 <= d <= 4.0:
                img.putpixel((x, y), RUBIS["d"] if x + y > 15 else RUBIS["l"])
    img.putpixel((6, 6), RUBIS["h"])
    return img


# ---------- Armure portée (64x32) ----------
def armor_layer(layer):
    rng = random.Random(42 + layer)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def fill(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                r = rng.random()
                c = RUBIS["m"] if r < 0.7 else (RUBIS["l"] if r < 0.85 else RUBIS["d"])
                img.putpixel((x, y), c)

    def border(x0, y0, x1, y1):
        for x in range(x0, x1):
            img.putpixel((x, y0), RUBIS["l"])
            img.putpixel((x, y1 - 1), RUBIS["o"])
        for y in range(y0, y1):
            img.putpixel((x0, y), RUBIS["l"])
            img.putpixel((x1 - 1, y), RUBIS["o"])

    def clear(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                img.putpixel((x, y), (0, 0, 0, 0))

    if layer == 1:
        # Casque (tête : 32x16 en haut à gauche)
        fill(0, 0, 32, 16)
        for (x0, y0) in ((0, 8), (8, 8), (16, 8), (24, 8)):
            border(x0, y0, x0 + 8, y0 + 8)
        clear(9, 11, 15, 16)          # ouverture du visage
        for x in range(9, 15):
            img.putpixel((x, 10), RUBIS["d"])
        # Plastron : corps + bras
        fill(16, 16, 40, 32)
        fill(40, 16, 56, 32)
        border(20, 20, 28, 32)
        border(32, 20, 40, 32)
        for x in range(22, 26):
            img.putpixel((x, 23), RUBIS["h"])
        # Bottes : bas des jambes
        fill(0, 26, 16, 32)
        fill(4, 16, 12, 20)
        for x in range(0, 16):
            img.putpixel((x, 26), RUBIS["l"])
    else:
        # Jambières : ceinture + jambes
        fill(16, 16, 40, 32)
        clear(16, 16, 40, 27)
        fill(20, 16, 28, 20)
        for x in range(16, 40):
            img.putpixel((x, 27), RUBIS["l"])
        fill(0, 16, 16, 30)
        for (x0, y0) in ((0, 20), (4, 20), (8, 20), (12, 20)):
            border(x0, y0, x0 + 4, 30)
    return img


# ---------- Armes à feu (vue de profil, canon vers la droite) ----------
GUN_COLORS = {
    "k": hexc("#2b2f36"), "m": hexc("#4a505a"), "l": hexc("#6f7782"), "h": hexc("#9aa3ad"),
    "w": hexc("#5a3e1b"), "W": hexc("#8a6233"), "r": RUBIS["m"], "R": RUBIS["l"],
    "d": hexc("#15171b"), "o": hexc("#15171b"), "s": hexc("#5fd4ff"),
    "t": hexc("#4b5a2a"), "T": hexc("#6b7d3a"), "u": hexc("#353f1d"),
    "c": hexc("#b5603a"), "C": hexc("#e08a5a"), "b": hexc("#b8901c"), "B": hexc("#e8cf5a"),
    "p": hexc("#9e1c26"), "P": hexc("#d8404e"), "g": hexc("#7d838b"), "G": hexc("#b9bec4"),
    "f": hexc("#3a3f46"), "n": hexc("#3f8a3a"), "e": hexc("#6a6e58"), "E": hexc("#989c82"),
}
GUN_OUTLINE = hexc("#121417")

GUNS = {
    "pistol": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "..hhhhhhhhhhhh..",
        "..lllllllllllll.",
        "..mmmmmmmmmmmmm.",
        "..kkkkkkkkrkkkk.",
        "..wWWk.do.......",
        "..wWWk..o.......",
        "..wWWkooo.......",
        ".wWWk...........",
        ".wWWk...........",
        ".wwkk...........",
        "................",
    ],
    "assault_rifle": [
        "................",
        "................",
        "................",
        "................",
        "......kk....k...",
        ".wWWkhhhhhhhhhh.",
        ".wWWkllllllllllk",
        ".wWWkmmmmrmmm...",
        ".ww.kkd.mmm.....",
        "....kk...mmm....",
        "....kk....mm....",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "shotgun": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "w...hhhhhhhhhhhh",
        "wWWWmmmmmmmmmmmm",
        "wWWWkkkkwWWWWWk.",
        "wWW.kd..........",
        "ww..............",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "sniper_rifle": [
        "................",
        "................",
        "................",
        "....shhhhhs.....",
        ".....kkkkk......",
        "......k..k......",
        "wWWWkhhhhhhhhhhh",
        "wWWWkmmmrmmkkkkk",
        "wWW.kkdkk.......",
        "ww...kk.........",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "rocket_launcher": [
        "................",
        "................",
        "................",
        "................",
        "...kk.....kk....",
        ".rTTTTTTTTTTTTrk",
        ".rttttttttttttrk",
        ".ruuuuuuuuuuuurk",
        "....kd...kk.....",
        "....kk...kk.....",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "bullet": [
        "................",
        "................",
        "................",
        "....c...c...c...",
        "...cCc.cCc.cCc..",
        "...bBb.bBb.bBb..",
        "...bBb.bBb.bBb..",
        "...bBb.bBb.bBb..",
        "...bBb.bBb.bBb..",
        "...bBb.bBb.bBb..",
        "...bBb.bBb.bBb..",
        "...bbb.bbb.bbb..",
        "................",
        "................",
        "................",
        "................",
    ],
    "shotgun_shell": [
        "................",
        "................",
        "................",
        "....ppp..ppp....",
        "....pPp..pPp....",
        "....pPp..pPp....",
        "....pPp..pPp....",
        "....pPp..pPp....",
        "....pPp..pPp....",
        "....bBb..bBb....",
        "....bBb..bBb....",
        "...bbbb.bbbb....",
        "................",
        "................",
        "................",
        "................",
    ],
    "rocket": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "..ff............",
        "..fggggggggpp...",
        "..fGGGGGGGGpPp..",
        "..fggggggggpp...",
        "..ff............",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
}

# Anciennes munitions génériques remplacées par les calibres
del GUNS["bullet"], GUNS["rocket"]

# Lignes plus courtes que 16 caractères : complétées automatiquement par des "."
GUNS.update({
    "ak47": [
        "", "", "", "", "",
        "..........k",
        "wWWkmmmmmWWWkkkk",
        "wWWWkmmmmWWWk",
        "wW...kd.mm",
        "w....kk..mm",
        "......k...mm",
        "...........m",
    ],
    "m4": [
        "", "", "", "",
        "...kkkkkkkk",
        "kkkkmmmmmmkkk.k",
        "kkk.mmmmmmkkkkkk",
        "kk...kd.mm",
        "k....kk.mm",
        ".....k..mm",
    ],
    "famas": [
        "", "", "",
        "..kkkkkkkkkk",
        "..k........k",
        "kkkkkkkkkkkkk",
        "kmmmmmmmmmmmkkkk",
        "kkkkkkkkkkkkk",
        "...mm..kkdk",
        "...mm..kk.k",
        "...mm...kkk",
    ],
    "rpg7": [
        "", "", "", "", "",
        "............TT",
        "...k.......TTTT",
        "kkmmmWWWWmmtTTTT",
        "kkmmmWWWWmmtTTTT",
        "....kd..k..TTTT",
        "....kk..k...TT",
    ],
    "ammo_9mm": [
        "", "", "", "", "",
        "....c...c...c",
        "...cCc.cCc.cCc",
        "...bBb.bBb.bBb",
        "...bBb.bBb.bBb",
        "...bBb.bBb.bBb",
        "...bbb.bbb.bbb",
    ],
    "ammo_556": [
        "",
        "....n...n...n",
        "....c...c...c",
        "...cCc.cCc.cCc",
        "....b...b...b",
        "...bBb.bBb.bBb",
        "...bBb.bBb.bBb",
        "...bBb.bBb.bBb",
        "...bBb.bBb.bBb",
        "...bBb.bBb.bBb",
        "...bBb.bBb.bBb",
        "...bbb.bbb.bbb",
    ],
    "ammo_762": [
        "",
        "....c...c...c",
        "...cCc.cCc.cCc",
        "...cCc.cCc.cCc",
        "....e...e...e",
        "...eEe.eEe.eEe",
        "...eEe.eEe.eEe",
        "...eEe.eEe.eEe",
        "...eEe.eEe.eEe",
        "...eEe.eEe.eEe",
        "...eEe.eEe.eEe",
        "...eee.eee.eee",
    ],
    "rpg_rocket": [
        "", "", "", "", "",
        ".........TTT",
        "..f.....TTTTT",
        "..fggggtTTTTTTu",
        "..f.....TTTTT",
        ".........TTT",
    ],
    # ----- Accessoires -----
    "red_dot_sight": [
        "", "", "", "", "",
        "......kkkk",
        ".....kssssk",
        ".....ksRssk",
        ".....kssssk",
        "....kkkkkkkk",
        "....mmmmmmmm",
    ],
    "holo_sight": [
        "", "", "",
        "....kkkkkkkk",
        "....kssssssk",
        "....kssssssk",
        "....kssRsssk",
        "....kssssssk",
        "...kkkkkkkkkk",
        "...mmmmmmmmmm",
    ],
    "acog_scope": [
        "", "", "", "",
        ".kk.........kkk",
        ".kkkkkkkkkkkkkks",
        ".kkllllllllllkks",
        ".kkkkkkkkkkkkkks",
        ".kk...kkkk..kkk",
        "......mmmm",
    ],
    "silencer": [
        "", "", "", "", "",
        "..kkkkkkkkkkkk",
        "..kllllllllllk",
        "..kmmmmmmmmmmkd",
        "..kmmmmmmmmmmkd",
        "..kkkkkkkkkkkk",
    ],
    "compensator": [
        "", "", "", "", "", "",
        "....kkkkkkk",
        "....kldldlk",
        "....kmmmmmkd",
        "....kkkkkkk",
    ],
    "vertical_grip": [
        "", "", "",
        "....kkkkkkkk",
        "....mmmmmmmm",
        "......kmmk",
        "......kmmk",
        "......kmmk",
        "......kmmk",
        "......kmmk",
        "......kmmk",
        "......kkkk",
    ],
    "angled_grip": [
        "", "", "", "",
        "...kkkkkkkkk",
        "...mmmmmmmmm",
        "...kmmmmmmk",
        "....kmmmmk",
        ".....kmmk",
        "......kk",
    ],
    "magazine_24": [
        "", "",
        "......bBb",
        ".....kmmmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmmmk",
        ".....kkkkk",
    ],
    "magazine_36": [
        "......bBb",
        ".....kmmmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmlmk",
        ".....kmmmk",
        ".....kkkkk",
    ],
    "drum_magazine": [
        "", "",
        "......kk",
        "......kk",
        "....kkkkkk",
        "...kmmmmmmk",
        "..kmmmllmmmk",
        "..kmmllllmmk",
        "..kmmllllmmk",
        "..kmmmllmmmk",
        "...kmmmmmmk",
        "....kkkkkk",
    ],
})


def gun_icon(rows):
    rows = [r.ljust(16, ".")[:16] for r in rows] + ["." * 16] * (16 - len(rows))
    cv = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                cv.put(x, y, GUN_COLORS[ch], GUN_OUTLINE)
    return cv.image()


# ---------- Lunette du sniper (512x512, transparente au centre) ----------
def sniper_scope(n=512):
    c = n / 2
    radius = n * 0.44     # rayon de la zone visible
    feather = n * 0.05    # largeur du bord flou
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    for y in range(n):
        for x in range(n):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c)
            t = min(max((d - (radius - feather)) / feather, 0.0), 1.0)
            t = t * t * (3 - 2 * t)  # transition douce
            px[x, y] = (0, 0, 0, int(255 * t))

    draw = ImageDraw.Draw(img)
    black = (0, 0, 0, 255)
    inner = radius * 0.5   # là où s'arrêtent les barres épaisses
    thick = n * 0.012
    thin = 1

    # Barres épaisses : haut, gauche, droite
    draw.rectangle([c - thick, 0, c + thick, c - inner], fill=black)
    draw.rectangle([0, c - thick, c - inner, c + thick], fill=black)
    draw.rectangle([c + inner, c - thick, n, c + thick], fill=black)

    # Réticule fin
    draw.rectangle([c - inner, c - thin, c + inner, c + thin - 1], fill=black)
    draw.rectangle([c - thin, c - inner, c + thin - 1, c + inner * 1.3], fill=black)

    # Graduations sur la ligne horizontale
    step = inner / 10
    for i in range(1, 10):
        h = 7 if i % 2 == 0 else 4
        for sign in (-1, 1):
            x = c + sign * i * step
            draw.rectangle([x - 1, c - h, x, c + h], fill=black)
    for i in range(1, 6):
        h = 4
        draw.rectangle([c - h, c - i * step * 2 - 1, c + h, c - i * step * 2], fill=black)

    # Échelle de chute de balle sous le centre, avec les distances
    font = ImageFont.load_default()
    for i in range(1, 6):
        y = c + i * step * 1.25
        half = (6 - i) * 2.5 + 6
        draw.rectangle([c - half, y - 1, c + half, y], fill=black)
        draw.text((c + half + 4, y - 6), str(i * 100), fill=black, font=font)
    # Chiffres sur la droite de la ligne horizontale
    for i in (2, 4, 6, 8):
        draw.text((c + i * step - 3, c - 20), str(i), fill=black, font=font)
    return img


# ---------- Réticules de visée ----------
def scope_mask(n=512, radius_ratio=0.44, feather_ratio=0.05):
    """Image noire avec un cercle transparent au centre (bord flou)."""
    c = n / 2
    radius, feather = n * radius_ratio, n * feather_ratio
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    for y in range(n):
        for x in range(n):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c)
            t = min(max((d - (radius - feather)) / feather, 0.0), 1.0)
            px[x, y] = (0, 0, 0, int(255 * t * t * (3 - 2 * t)))
    return img


def acog_scope(n=512):
    """Lunette x4 : chevron rouge + fine ligne verticale graduée."""
    img = scope_mask(n, 0.42, 0.08)
    draw = ImageDraw.Draw(img)
    c = n / 2
    black, red = (0, 0, 0, 255), (230, 40, 40, 255)
    draw.rectangle([c - 1, c + 14, c, c + n * 0.25], fill=black)
    for i in range(1, 6):
        y = c + 14 + i * n * 0.04
        draw.rectangle([c - 6 + i, y, c + 6 - i, y + 1], fill=black)
    draw.rectangle([0, c - 1, c - n * 0.18, c], fill=black)
    draw.rectangle([c + n * 0.18, c - 1, n, c], fill=black)
    # chevron
    draw.line([(c - 10, c + 10), (c, c), (c + 10, c + 10)], fill=red, width=3)
    return img


def red_dot(n=32):
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    c = n / 2
    for y in range(n):
        for x in range(n):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c)
            if d <= 2.2:
                img.putpixel((x, y), (255, 40, 40, 255))
            elif d <= 4.5:
                img.putpixel((x, y), (255, 60, 60, int(120 * (4.5 - d) / 2.3)))
    return img


def holo_reticle(n=64):
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    c = n / 2
    for y in range(n):
        for x in range(n):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c)
            if abs(d - 22) <= 1.0 or d <= 1.8:
                img.putpixel((x, y), (255, 50, 50, 235))
            elif abs(d - 22) <= 2.2:
                img.putpixel((x, y), (255, 60, 60, 70))
    for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
        for k in range(17, 22):
            x, y = int(c + dx * k), int(c + dy * k)
            img.putpixel((x, y), (255, 50, 50, 235))
    return img


# ---------- Blocs des tables ----------
def plank_base(rng, dark, light):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            base = light if (y // 4) % 2 == 0 else dark
            v = rng.randint(-10, 10)
            img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base[:3]) + (255,))
        if y % 4 == 3:
            for x in range(16):
                img.putpixel((x, y), (60, 40, 20, 255))
    return img


def table_textures():
    rng = random.Random(7)
    metal = [hexc(c) for c in ("#3a3f46", "#454b54", "#50565f")]
    tex = {}

    # Table d'armement : plateau en métal sombre bordé de rubis, côtés en bois avec bande métal
    top = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            top.putpixel((x, y), rng.choice(metal))
    for i in range(16):
        for p in ((i, 0), (0, i), (i, 15), (15, i)):
            top.putpixel(p, RUBIS["m"])
    for p in ((2, 2), (13, 2), (2, 13), (13, 13)):
        top.putpixel(p, hexc("#9aa3ad"))
    for x in range(4, 12):  # silhouette d'arme gravée sur le plateau
        top.putpixel((x, 7), hexc("#1d2024"))
    for p in ((4, 8), (5, 8), (5, 9), (8, 8), (8, 9), (11, 6)):
        top.putpixel(p, hexc("#1d2024"))
    tex["armament_table_top"] = top

    side = plank_base(rng, hexc("#5a3e1b"), hexc("#7a5a2c"))
    for x in range(16):
        for y in range(0, 4):
            side.putpixel((x, y), rng.choice(metal))
        side.putpixel((x, 3), RUBIS["d"])
    tex["armament_table_side"] = side
    front = side.copy()
    gun = ["kkkkkkkkkk", "kkkk.k..k.", "kk........"]  # râtelier : un fusil accroché
    for dy, row in enumerate(gun):
        for dx, ch in enumerate(row):
            if ch == "k":
                front.putpixel((3 + dx, 7 + dy), hexc("#24272d"))
    tex["armament_table_front"] = front

    # Table de modification : tapis vert avec outils, côtés en bois
    mtop = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            mtop.putpixel((x, y), rng.choice([hexc("#2f5a3a"), hexc("#35663f"), hexc("#2a5034")]))
    for i in range(16):
        for p in ((i, 0), (0, i), (i, 15), (15, i)):
            mtop.putpixel(p, hexc("#3a3f46"))
    for i in range(3, 13, 3):  # grille de découpe
        for j in range(1, 15):
            mtop.putpixel((i, j), hexc("#4f8a5c"))
            mtop.putpixel((j, i), hexc("#4f8a5c"))
    for k in range(5):  # clé
        mtop.putpixel((4 + k, 10 - k), hexc("#9aa3ad"))
    mtop.putpixel((9, 4), hexc("#9aa3ad"))
    mtop.putpixel((10, 5), hexc("#9aa3ad"))
    tex["modification_table_top"] = mtop
    mside = plank_base(rng, hexc("#4a3218"), hexc("#6b4a24"))
    for x in range(16):
        mside.putpixel((x, 0), hexc("#3a3f46"))
        mside.putpixel((x, 1), hexc("#2f5a3a"))
    for k in range(6):  # viseur dessiné
        mside.putpixel((5 + k, 7), hexc("#24272d"))
        mside.putpixel((5 + k, 10), hexc("#24272d"))
    for k in range(3):
        mside.putpixel((5, 8 + k - 1), hexc("#24272d"))
        mside.putpixel((10, 8 + k - 1), hexc("#24272d"))
    mside.putpixel((8, 8), hexc("#5fd4ff"))
    mside.putpixel((7, 9), hexc("#5fd4ff"))
    tex["modification_table_side"] = mside
    return tex


def main():
    items = {
        "rubis": gem(),
        "rubis_sword": sword(RUBIS, GOLD),
        "rubis_dagger": dagger(RUBIS, GOLD),
        "rubis_war_hammer": hammer(RUBIS, GOLD),
        "rubis_pickaxe": pickaxe(),
        "rubis_axe": axe(),
        "rubis_shovel": shovel(),
        "rubis_hoe": hoe(),
        "flame_sword": sword(FIRE, palette("#1a0a05", "#3a1a0e", "#5a2a14", "#7a3a1c", "#a05030"), handle=WOOD),
        "frost_sword": sword(FROST, SILVER, handle=STEEL),
        "venom_dagger": dagger(VENOM, palette("#101010", "#2a2a2a", "#3f3f3f", "#5a5a5a", "#808080")),
        "ender_blade": sword(ENDER, TEAL, handle=palette("#0a0a0a", "#1c1c24", "#2a2a36", "#3c3c4c", "#5a5a70")),
        "thunder_hammer": hammer(STEEL, GOLD, handle=palette("#1a1a1a", "#3a2a1a", "#55402a", "#6e5638", "#8a7050")),
        "fire_staff": fire_staff(),
        "rubis_bow": bow(),
    }
    for p in range(3):
        items[f"rubis_bow_pulling_{p}"] = bow(p)
    for name, rows in ARMOR_ICONS.items():
        items[name] = ascii_icon(rows, RUBIS)

    for name, rows in GUNS.items():
        items[name] = gun_icon(rows)

    for name, img in items.items():
        save(img, "item", f"{name}.png")

    stone = [hexc(c) for c in ("#6e6e6e", "#7a7a7a", "#858585", "#8f8f8f", "#7f7f7f")]
    deepslate = [hexc(c) for c in ("#3a3a40", "#46464d", "#4f4f57", "#5a5a62", "#424249")]
    save(ore(1, stone), "block", "rubis_ore.png")
    save(ore(2, deepslate, streaks=True), "block", "deepslate_rubis_ore.png")
    save(rubis_block(), "block", "rubis_block.png")

    save(sniper_scope(), "misc", "sniper_scope.png")
    save(acog_scope(), "misc", "acog_scope.png")
    save(red_dot(), "misc", "red_dot.png")
    save(holo_reticle(), "misc", "holo_reticle.png")
    for name, img in table_textures().items():
        save(img, "block", f"{name}.png")
    save(armor_layer(1), "models", "armor", "rubis_layer_1.png")
    save(armor_layer(2), "models", "armor", "rubis_layer_2.png")
    print(f"{len(items) + 5} textures générées dans {os.path.normpath(TEX)}")


if __name__ == "__main__":
    main()

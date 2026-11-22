#!/usr/bin/env python3
"""
Skins sur mesure des bots (format joueur 64x64, bras larges) : une banque de skins par faction.

Aucun skin de base de Minecraft (Steve / Alex) : chaque personnage est peint pixel par pixel à partir d'une fiche
(teint, coiffure, barbe, lunettes, couvre-chef, masque, haut, bas, chaussures, détails de faction), avec un ombrage
par face (dessus éclairé, dos et dessous plus sombres), du grain de tissu et des plis.

Sortie : assets/modeminecraft/textures/entity/bot/<faction>_<n>.png (+ cape des Faucons Blancs, aperçu)
Utilisation : python3 tools/generate_skins.py
"""
import os
import random
import zlib

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
OUT = os.path.join(ROOT, "src/main/resources/assets/modeminecraft/textures/entity/bot")

# Découpage d'un pavé (u, v, largeur, hauteur, profondeur) dans la texture
PARTS = {
    "head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
    "rarm": (40, 16, 4, 12, 4), "rsleeve": (40, 32, 4, 12, 4),
    "larm": (32, 48, 4, 12, 4), "lsleeve": (48, 48, 4, 12, 4),
    "rleg": (0, 16, 4, 12, 4), "rpants": (0, 32, 4, 12, 4),
    "lleg": (16, 48, 4, 12, 4), "lpants": (0, 48, 4, 12, 4),
}
LIGHT = {"top": 1.12, "front": 1.0, "right": 0.9, "left": 0.9, "back": 0.84, "bottom": 0.7}


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def rnd(*a):
    return (zlib.crc32(repr(a).encode()) & 0xFFFF) / 0xFFFF


def faces(part):
    u, v, w, h, d = PARTS[part]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def paint(img, part, fn, seed):
    """fn(face, x, y, W, H) -> couleur (r,g,b) / (r,g,b,a) / None (transparent)."""
    px = img.load()
    for face, (fx, fy, W, H) in faces(part).items():
        for y in range(H):
            for x in range(W):
                c = fn(face, x, y, W, H)
                if c is None:
                    continue
                a = c[3] if len(c) > 3 else 255
                f = LIGHT[face] * (1 + (rnd(seed, part, face, x, y) - 0.5) * 0.09)
                px[fx + x, fy + y] = shade(c, f) + (a,)


# ------------------------------------------------------------------ fiches de personnages
SKIN_TONES = ["#f2c9a0", "#e0ac7d", "#c68a5c", "#a26a3f", "#7b4a2a", "#5a3420"]
HAIR = ["#1c1410", "#3a2414", "#6a4422", "#a87a3c", "#d8b878", "#8a8a8a", "#2a2a2e"]
EYES = ["#3a5a8a", "#4a6a3a", "#5a3a20", "#2a2a2a", "#6a8aa0"]


def person(rng, tones=None):
    return {"skin": rng.choice(tones or SKIN_TONES), "hair": rng.choice(HAIR), "eyes": rng.choice(EYES),
            "hair_style": rng.choice(["short", "short", "buzz", "side", "bald"]), "beard": rng.choice(["none", "none", "stubble", "full", "mustache"]),
            "glasses": rng.random() < 0.2, "scar": rng.random() < 0.15}


def head_fn(p):
    skin, hair = hexc(p["skin"]), hexc(p["hair"])
    dark = shade(skin, 0.82)

    def fn(face, x, y, W, H):
        style = p["hair_style"]
        top_rows = {"short": 2, "buzz": 1, "side": 2, "bald": 0, "long": 3}.get(style, 2)
        if face == "top":
            return hair if style != "bald" else skin
        if face == "bottom":
            return dark
        if face in ("back",):
            if style == "long":
                return hair
            return hair if y < (top_rows + 3 if style != "bald" else 0) else skin
        if face in ("left", "right"):
            if style != "bald" and (y < top_rows + 1 or (y < 4 and x > 3)):
                return hair
            if p["beard"] == "full" and y >= 5 and x < 4:
                return shade(hair, 0.95)
            return skin if not (x == 3 and y == 4) else dark       # oreille
        # visage
        if y < top_rows:
            return hair
        if style == "side" and y == top_rows and x < 4:
            return hair
        if y == 3 and x in (1, 2, 5, 6):
            return shade(hair, 0.85)                               # sourcils
        if y == 4:
            if x in (1, 6):
                return (235, 235, 230)
            if x in (2, 5):
                return hexc(p["eyes"])
        if p["glasses"] and y == 4 and 1 <= x <= 6:
            return (20, 22, 26)
        if y == 5 and x in (3, 4):
            return shade(skin, 0.86)                               # nez
        if p["beard"] == "full" and (y >= 6 or (y == 5 and x in (0, 1, 6, 7))):
            return shade(hair, 0.95) if not (y == 6 and 2 <= x <= 5) else (90, 50, 45)
        if p["beard"] == "mustache" and y == 5 and 2 <= x <= 5 and x not in (3, 4):
            return hair
        if p["beard"] == "mustache" and y == 6 and 2 <= x <= 5:
            return hair
        if p["beard"] == "stubble" and y >= 6:
            return shade(skin, 0.78) if (x + y) % 2 == 0 else shade(skin, 0.9)
        if y == 6 and 2 <= x <= 5:
            return (120, 60, 55) if 3 <= x <= 4 else shade(skin, 0.8)  # bouche
        if p["scar"] and x == 6 and y in (2, 3, 5):
            return (150, 80, 75)
        return skin
    return fn


def cloth(base, pattern=None, seed=0):
    """Tissu : couleur de base, motif (camouflage, rayures, fleurs), plis aléatoires."""
    b = hexc(base)

    def fn(face, x, y, W, H):
        c = b
        if pattern:
            c = pattern(face, x, y)
        if rnd(seed, face, x, y, "pli") < 0.06:
            c = shade(c, 0.86)
        return c
    return fn


def camo(colors, seed, scale=2):
    cols = [hexc(c) for c in colors]

    def pat(face, x, y):
        i = int(rnd(seed, face, x // scale, y // scale) * len(cols) * 1.6) % len(cols)
        return cols[i]
    return pat


def tropical(base, flower, leaf, seed):
    b, f, l = hexc(base), hexc(flower), hexc(leaf)

    def pat(face, x, y):
        r = rnd(seed, face, x // 2, y // 2)
        return f if r < 0.18 else l if r < 0.32 else b
    return pat


# ------------------------------------------------------------------ assemblage d'un skin
def skin(p, outfit, seed):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin_c = hexc(p["skin"])
    paint(img, "head", head_fn(p), seed)
    if outfit.get("head_over"):
        paint(img, "hat", outfit["head_over"], seed + 1)
    # torse
    paint(img, "body", outfit["top"], seed + 2)
    if outfit.get("top_over"):
        paint(img, "jacket", outfit["top_over"], seed + 3)
    # bras : manches longues ou courtes (peau visible), gants
    sleeve = outfit["sleeve"]
    short = outfit.get("short_sleeves", False)
    glove = hexc(outfit["gloves"]) if outfit.get("gloves") else None
    tattoo = outfit.get("tattoo")

    def arm(face, x, y, W, H):
        if face == "bottom":
            return glove or skin_c
        if y >= 10:
            return glove or skin_c
        if short and y >= 4:
            if tattoo and face in ("front", "left", "right") and 5 <= y <= 8 and rnd(seed, "tat", face, x, y) < 0.45:
                return hexc(tattoo)
            return skin_c
        return sleeve(face, x, y, W, H)
    paint(img, "rarm", arm, seed + 4)
    paint(img, "larm", arm, seed + 5)
    if outfit.get("arm_over"):
        paint(img, "rsleeve", outfit["arm_over"], seed + 6)
        paint(img, "lsleeve", outfit["arm_over"], seed + 7)
    # jambes
    boots = hexc(outfit["boots"])
    boot_h = outfit.get("boot_h", 3)

    def leg(face, x, y, W, H):
        if face == "bottom" or y >= H - boot_h:
            if face == "front" and y == H - boot_h and outfit.get("laces"):
                return hexc(outfit["laces"])
            return boots if y != H - 1 else shade(boots, 0.7)
        return outfit["pants"](face, x, y, W, H)
    paint(img, "rleg", leg, seed + 8)
    paint(img, "lleg", leg, seed + 9)
    if outfit.get("leg_over"):
        paint(img, "rpants", outfit["leg_over"], seed + 10)
        paint(img, "lpants", outfit["leg_over"], seed + 11)
    return img


# ------------------------------------------------------------------ détails réutilisables
def with_details(base_fn, details):
    """details(face, x, y) -> couleur ou None (sinon la couleur de base)."""
    def fn(face, x, y, W, H):
        d = details(face, x, y)
        return d if d is not None else base_fn(face, x, y, W, H)
    return fn


def belt(color, buckle="#c0b080", row=11):
    c, b = hexc(color), hexc(buckle)

    def d(face, x, y):
        if y == row and face in ("front", "back", "left", "right"):
            return b if face == "front" and x in (3, 4) else c
        return None
    return d


def combine(*ds):
    def d(face, x, y):
        for f in ds:
            r = f(face, x, y)
            if r is not None:
                return r
        return None
    return d


def cap(color, band=None, visor=None, badge=None):
    """Casquette (couche de la tête) : dessus, bandeau, visière."""
    c = hexc(color)

    def fn(face, x, y, W, H):
        if face == "top":
            return c
        if face == "bottom":
            return None
        if y <= 1:
            return c
        if y == 2:
            if face == "front" and badge and x in (3, 4):
                return hexc(badge)
            return hexc(band) if band else shade(c, 0.8)
        if y == 3 and face == "front" and visor:
            return hexc(visor)
        return None
    return fn


def hood(color, seed, open_face=True):
    c = hexc(color)

    def fn(face, x, y, W, H):
        if face == "bottom":
            return None
        if face == "front":
            if open_face and 1 <= x <= 6 and y >= 2:
                return None
            return shade(c, 0.9)
        return c if rnd(seed, face, x, y) > 0.05 else shade(c, 0.8)
    return fn


def mask_lower(color, seed, pattern=None, top_row=5):
    c = hexc(color)

    def fn(face, x, y, W, H):
        if face in ("top", "bottom"):
            return None
        if face == "front" and y >= top_row:
            return pattern(face, x, y) if pattern else c
        if face in ("left", "right") and y >= top_row:
            return c
        if face == "back" and y >= top_row:
            return c
        return None
    return fn


def keffieh(color, check, seed):
    """Keffieh : enroulé sur la tête et couvrant le bas du visage, motif à damier."""
    c, k = hexc(color), hexc(check)

    def pat(face, x, y):
        return k if (x + y) % 3 == 0 or (x - y) % 4 == 0 else c

    def fn(face, x, y, W, H):
        if face == "bottom":
            return None
        if face == "top":
            return pat(face, x, y)
        if face == "front":
            if y <= 2 or y >= 5:
                return pat(face, x, y)
            return None
        return pat(face, x, y)
    return fn


def hat_brim(color, band="#3a2414"):
    """Chapeau de cow-boy / shérif : couronne + bord (approximé dans la couche du chapeau)."""
    c, b = hexc(color), hexc(band)

    def fn(face, x, y, W, H):
        if face == "top":
            return c
        if face == "bottom":
            return None
        if y <= 1:
            return c
        if y == 2:
            return b
        if y == 3:
            return shade(c, 0.85)
        return None
    return fn


def chapka(color, fur="#5a4a3a"):
    c, f = hexc(color), hexc(fur)

    def fn(face, x, y, W, H):
        if face == "bottom":
            return None
        if face == "top":
            return f
        if face == "front":
            if y <= 2:
                return f if y == 2 else c
            return None
        if face in ("left", "right"):
            return f if y <= 5 else None                         # oreillettes
        return f if y <= 4 else None
    return fn


# ------------------------------------------------------------------ factions
def outfits(faction, rng, seed):
    """Tenue d'un membre de la faction (dictionnaire pour skin())."""
    s = seed
    if faction == "police":
        navy = "#1c2a4a"
        shirt = cloth(navy, seed=s)
        badge = lambda f, x, y: (220, 200, 90) if f == "front" and x == 5 and y in (2, 3) else (230, 230, 235) if f == "front" and x == 2 and y == 2 else None
        tie = lambda f, x, y: (20, 24, 34) if f == "front" and x in (3, 4) and 1 <= y <= 6 else None
        return {"top": with_details(shirt, combine(badge, tie, belt("#141414", "#c0c0c0"))), "sleeve": cloth(navy, seed=s),
                "pants": cloth("#16213a", seed=s), "boots": "#121212", "head_over": cap("#16213a", "#101010", "#0c0c0c", "#d8c060") if rng.random() < 0.7 else None,
                "short_sleeves": rng.random() < 0.3}
    if faction == "swat":
        black = "#1a1c20"
        return {"top": with_details(cloth(black, seed=s), combine(belt("#2a2c30", "#50555a"),
                lambda f, x, y: (230, 230, 230) if f == "back" and 3 <= y <= 4 and 1 <= x <= 6 and (x + y) % 2 == 0 else None)),
                "sleeve": cloth(black, seed=s), "pants": cloth("#22252a", seed=s), "boots": "#0e0e0e", "gloves": "#101010",
                "head_over": mask_lower("#121316", s, top_row=4)}
    if faction == "militia":
        sand = rng.choice(["#b8a27a", "#a8926a", "#9a8a68", "#c2ae86"])
        kef = keffieh(rng.choice(["#e8e2d4", "#d8d0c0"]), rng.choice(["#a02020", "#1a1a1a"]), s)
        band = lambda f, x, y: (90, 60, 30) if f == "front" and (x == y - 1 or x == y - 2) and y < 10 else None   # bandoulière
        return {"top": with_details(cloth(sand, seed=s), combine(band, belt("#5a3a20", "#808080"))), "sleeve": cloth(sand, seed=s),
                "pants": cloth(rng.choice(["#8a7a58", "#6a5e48", "#7a6a50"]), seed=s), "boots": "#3a2a1a", "head_over": kef}
    if faction == "rebel":
        greens = ["#4a5530", "#5a4a30", "#3a4028", "#6a5a3a"]
        return {"top": cloth(rng.choice(greens), seed=s), "sleeve": cloth(rng.choice(greens), seed=s),
                "pants": cloth(rng.choice(["#3a3a2a", "#4a3a28", "#2e3424"]), seed=s), "boots": "#2a1e14",
                "head_over": hood(rng.choice(["#3a3a2a", "#4a3e2a", "#2a2e22"]), s) if rng.random() < 0.6 else None}
    if faction == "garrison":
        wood = camo(["#4b5a2a", "#5a4630", "#1e2416", "#6b7d3a"], s)
        return {"top": with_details(cloth("#4b5a2a", wood, s), belt("#2a2a1e")), "sleeve": cloth("#4b5a2a", wood, s + 1),
                "pants": cloth("#4b5a2a", wood, s + 2), "boots": "#1a1a14", "gloves": "#2a2e22",
                "head_over": mask_lower("#2a3020", s, top_row=5) if rng.random() < 0.4 else None}
    if faction == "offshore":
        # combinaison étanche noire à bandes orange foncé, gilet tactique, cagoule
        suit = rng.choice(["#18191c", "#1e2024", "#222326"])
        stripe = lambda f, x, y: (190, 80, 20) if y in (2, 3) and f in ("left", "right") or (f == "front" and x in (0, 7) and y > 1) else None
        vest = lambda f, x, y: (52, 56, 50) if f in ("front", "back") and 1 <= x <= 6 and 1 <= y <= 9 else None
        pouches = lambda f, x, y: (34, 36, 32) if f == "front" and y in (7, 8) and x in (1, 2, 5, 6) else None
        return {"top": with_details(cloth(suit, seed=s), combine(vest, pouches, stripe, belt("#2a2a2a", "#909090"))),
                "sleeve": with_details(cloth(suit, seed=s), lambda f, x, y: (190, 80, 20) if y == 1 else None),
                "pants": cloth(suit, seed=s), "boots": "#101012", "gloves": "#2a2a2a",
                "head_over": mask_lower("#141416", s, top_row=3) if rng.random() < 0.8 else None}
    if faction == "soviet":
        coat = rng.choice(["#4a4a3e", "#3e463a", "#5a5248"])
        fur_collar = lambda f, x, y: (90, 74, 58) if y <= 1 and f in ("front", "back", "left", "right") else None
        buttons = lambda f, x, y: (40, 40, 40) if f == "front" and x == 4 and y in (3, 6, 9) else None
        return {"top": with_details(cloth(coat, seed=s), combine(fur_collar, buttons, belt("#2a1e14", "#909090", 8))),
                "sleeve": cloth(coat, seed=s), "pants": cloth("#3a3a34", seed=s), "boots": "#1a1612", "boot_h": 5,
                "top_over": with_details(lambda f, x, y, W, H: hexc(coat) if y >= 9 else None, lambda f, x, y: None),
                "head_over": chapka("#3e3e34") if rng.random() < 0.7 else None, "gloves": "#2a2420"}
    if faction == "arctic":
        white = camo(["#e8ecf0", "#d0d8e0", "#f4f6f8", "#b8c4cc"], s)
        return {"top": with_details(cloth("#e8ecf0", white, s), belt("#8a929a")), "sleeve": cloth("#e8ecf0", white, s + 1),
                "pants": cloth("#e0e6ea", white, s + 2), "boots": "#c8d0d6", "gloves": "#d8dee2",
                "head_over": mask_lower("#e4e8ec", s, top_row=5)}
    if faction == "mine_security":
        vis = lambda f, x, y: (220, 210, 40) if y in (5, 9) else (200, 200, 200) if y == 6 and f == "front" else None
        return {"top": with_details(cloth("#e06a10", seed=s), combine(vis, belt("#1a1a1a"))), "sleeve": cloth("#2a2e34", seed=s),
                "pants": cloth("#2a3442", seed=s), "boots": "#3a2a1a", "laces": "#c0a040",
                "head_over": cap("#e8d020", "#c0a010") if rng.random() < 0.8 else None}
    if faction == "cartel":
        base = rng.choice(["#2a7a8a", "#c04a3a", "#e0c060", "#3a3a3a", "#f0f0e8"])
        shirt = cloth(base, tropical(base, rng.choice(["#f06090", "#f0a020", "#ffffff"]), "#2a8a3a", s), s)
        chain = lambda f, x, y: (220, 190, 60) if f == "front" and ((y == 1 and 2 <= x <= 5) or (y == 2 and x in (2, 5))) else None
        return {"top": with_details(shirt, chain), "sleeve": shirt, "short_sleeves": True, "tattoo": "#2a3a5a",
                "pants": cloth(rng.choice(["#2a2a2e", "#d8d0c0", "#3a4a6a"]), seed=s), "boots": rng.choice(["#3a2414", "#e0e0e0"]),
                "head_over": None}
    if faction == "gang":
        top = rng.choice(["#e8e8e8", "#c03030", "#2a2a2a", "#3a6a3a"])
        return {"top": cloth(top, seed=s), "sleeve": cloth(top, seed=s), "short_sleeves": True, "tattoo": rng.choice(["#1a2a4a", "#4a1a1a"]),
                "pants": cloth(rng.choice(["#3a4a6a", "#5a5a50", "#2a2a2a"]), seed=s), "boots": rng.choice(["#e0e0e0", "#2a2a2a"]),
                "head_over": mask_lower(rng.choice(["#a02020", "#202060"]), s, top_row=5) if rng.random() < 0.4 else None}
    if faction == "sheriff":
        duster = rng.choice(["#6a4a2a", "#5a3e24", "#7a5a38"])
        star = lambda f, x, y: (230, 200, 80) if f == "front" and ((x == 5 and y in (2, 4)) or (y == 3 and 4 <= x <= 6)) else None
        return {"top": with_details(cloth("#c8b896", seed=s), combine(star, belt("#3a2414", "#c0a040"))),
                "sleeve": cloth(duster, seed=s), "top_over": lambda f, x, y, W, H: hexc(duster) if (f != "front" or x in (0, 1, 6, 7)) else None,
                "pants": cloth("#4a4a5a", seed=s), "boots": "#4a2a14", "boot_h": 4, "head_over": hat_brim(rng.choice(["#4a3420", "#2a2420", "#8a6a44"])),
                "leg_over": None}
    if faction == "refinery":
        cov = rng.choice(["#3a4a5a", "#4a3a2a", "#2a3a2a"])
        return {"top": with_details(cloth(cov, seed=s), combine(belt("#1a1a1a"), lambda f, x, y: (210, 120, 30) if y == 7 else None)),
                "sleeve": cloth(cov, seed=s), "pants": cloth(cov, seed=s), "boots": "#2a2014", "gloves": "#3a2a1a",
                "head_over": cap("#d0d0d0", "#a0a0a0") if rng.random() < 0.6 else None}
    if faction == "civilian":
        suit = rng.random() < 0.5
        top = rng.choice(["#2a2a34", "#3a3a44", "#4a4a5a"]) if suit else rng.choice(["#a03030", "#3060a0", "#e0e0e0", "#50a050", "#e0a030"])
        shirt = cloth(top, seed=s)
        tie = (lambda f, x, y: (200, 200, 205) if f == "front" and x in (3, 4) and y <= 2 else (150, 30, 30) if f == "front" and x in (3, 4) and y <= 7 else None) if suit else (lambda f, x, y: None)
        return {"top": with_details(shirt, tie), "sleeve": shirt, "short_sleeves": not suit and rng.random() < 0.5,
                "pants": cloth(rng.choice(["#2a2a34", "#3a4a6a", "#5a4a3a"]), seed=s), "boots": rng.choice(["#1a1a1a", "#4a2a1a", "#e0e0e0"])}
    if faction == "trader":
        coat = rng.choice(["#2a2420", "#3a2a3a", "#1e2a2a"])
        return {"top": with_details(cloth(coat, seed=s), belt("#1a1a1a", "#d0b040")), "sleeve": cloth(coat, seed=s),
                "pants": cloth("#1a1a1e", seed=s), "boots": "#141414", "gloves": "#2a2a2a",
                "head_over": hood(coat, s, open_face=True), "top_over": lambda f, x, y, W, H: hexc(coat) if y >= 8 else None}
    if faction == "falcon":
        white = "#e8e6e0"
        emblem = lambda f, x, y: (40, 40, 44) if f == "front" and ((y == 3 and x in (2, 5)) or (y == 4 and 2 <= x <= 5) or (y == 5 and x in (3, 4))) else None
        return {"top": with_details(cloth(white, seed=s), combine(emblem, belt("#4a3a2a", "#c0c0c0"))), "sleeve": cloth("#c8c6c0", seed=s),
                "pants": cloth(rng.choice(["#4a4a44", "#5a5a50"]), seed=s), "boots": "#2a2420", "gloves": "#3a3430",
                "head_over": hood("#ecebe6", s) if rng.random() < 0.5 else None}
    raise ValueError(faction)


def knight(seed):
    """Chef des Troupes de Choc : armure de plaques en acier sombre et argent usé, cotte de mailles aux articulations,
    gorgerin balafré (le heaume et la cape sont des modèles 3D à part)."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    steel, silver, mail = hexc("#3a3e44"), hexc("#9aa0a8"), hexc("#5a5e64")

    def plate(face, x, y, W, H):
        if (x + y) % 5 == 0 and rnd(seed, face, x, y) < 0.5:
            return silver                                          # usure argentée
        if rnd(seed, "scratch", face, x, y) < 0.05:
            return shade(silver, 1.1)
        return steel if y % 4 else shade(steel, 0.8)               # lames de l'armure

    def chain(face, x, y, W, H):
        return mail if (x + y) % 2 else shade(mail, 0.75)

    def head(face, x, y, W, H):
        if face == "front" and y == 4 and 1 <= x <= 6:
            return (12, 12, 14)                                    # fente de vision
        if face == "front" and x == 5 and 5 <= y <= 7:
            return (130, 70, 60)                                   # balafre
        return plate(face, x, y, W, H)
    paint(img, "head", head, seed)
    paint(img, "body", lambda f, x, y, W, H: chain(f, x, y, W, H) if y >= 10 else plate(f, x, y, W, H), seed + 1)
    paint(img, "rarm", lambda f, x, y, W, H: chain(f, x, y, W, H) if 4 <= y <= 5 else plate(f, x, y, W, H), seed + 2)
    paint(img, "larm", lambda f, x, y, W, H: chain(f, x, y, W, H) if 4 <= y <= 5 else plate(f, x, y, W, H), seed + 3)
    paint(img, "rleg", lambda f, x, y, W, H: chain(f, x, y, W, H) if 5 <= y <= 6 else plate(f, x, y, W, H), seed + 4)
    paint(img, "lleg", lambda f, x, y, W, H: chain(f, x, y, W, H) if 5 <= y <= 6 else plate(f, x, y, W, H), seed + 5)
    # épaulières en relief (couche des manches)
    paint(img, "rsleeve", lambda f, x, y, W, H: plate(f, x, y, W, H) if y <= 3 else None, seed + 6)
    paint(img, "lsleeve", lambda f, x, y, W, H: plate(f, x, y, W, H) if y <= 3 else None, seed + 7)
    return img


def bosses():
    """Les 5 Chefs de Faction uniques (le 6e, le Chef des Troupes de Choc, est le chevalier ci-dessus)."""
    out = {}
    gold = (230, 190, 60)

    # --- Commissaire Divisionnaire SWAT : tenue tactique noire d'élite, insignes dorés
    rng = random.Random(101)
    p = person(rng)
    p.update(beard="stubble", glasses=False, hair_style="buzz")
    black = "#15171b"
    insignia = lambda f, x, y: gold if f == "front" and ((y == 1 and x in (1, 2, 5, 6)) or (x == 5 and y in (3, 4))) else \
        (240, 240, 240) if f == "back" and 2 <= y <= 4 and 1 <= x <= 6 and (x + y) % 2 == 0 else None
    shoulder = lambda f, x, y, W, H: gold if y == 0 and f in ("top", "front", "back") else None
    out["swat"] = skin(p, {"top": with_details(cloth(black, seed=1), combine(insignia, belt("#2a2c30", "#d0b040"))),
                           "sleeve": cloth(black, seed=1), "arm_over": shoulder, "pants": cloth("#1c1f24", seed=1),
                           "boots": "#0a0a0a", "gloves": "#0e0e0e", "laces": "#d0b040"}, 101)

    # --- Général Suprême de la Milice : uniforme de parade sable et rouge, casquette à visière dorée, décorations
    rng = random.Random(102)
    p = person(rng, SKIN_TONES[1:4])
    p.update(beard="mustache", glasses=False, hair_style="short")
    medals = lambda f, x, y: [(200, 40, 40), gold, (40, 90, 160), (40, 140, 60)][(x + y) % 4] if f == "front" and x in (5, 6) and 2 <= y <= 4 else \
        gold if f == "front" and x == 2 and y in (2, 4, 6, 8) else None
    sash = lambda f, x, y: (170, 30, 30) if f == "front" and (x == y - 2 or x == y - 3) and y <= 10 else None
    out["militia"] = skin(p, {"top": with_details(cloth("#c8b48a", seed=2), combine(medals, sash, belt("#5a1a14", "#d8b040"))),
                              "sleeve": with_details(cloth("#c8b48a", seed=2), lambda f, x, y: (170, 30, 30) if y in (9, 10) else None),
                              "pants": with_details(cloth("#b8a47a", seed=2), lambda f, x, y: (170, 30, 30) if f in ("left", "right") and x == 1 else None),
                              "boots": "#1a120c", "boot_h": 4,
                              "head_over": cap("#c8b48a", "#a01e1e", "#d8b040", "#e8c860")}, 102)

    # --- Commandant des Forces Spéciales : tenue Ghillie forêt, cagoule sombre
    rng = random.Random(103)
    p = person(rng)
    ghillie = camo(["#3a4a22", "#5a6a2a", "#2a3418", "#6a5a34", "#4a5a2a"], 103, scale=1)

    def strands(face, x, y, W, H):
        if rnd(103, "g", face, x, y) < 0.55:
            return ghillie(face, x, y)
        return None
    out["garrison"] = skin(p, {"top": cloth("#3a4a22", ghillie, 3), "sleeve": cloth("#3a4a22", ghillie, 4),
                               "pants": cloth("#3a4a22", ghillie, 5), "boots": "#14140e", "gloves": "#1e2216",
                               "top_over": strands, "arm_over": strands, "leg_over": strands,
                               "head_over": mask_lower("#1a1c18", 103, top_row=0) if False else
                               (lambda f, x, y, W, H: None if f == "front" and y == 4 and 1 <= x <= 6 else (26, 28, 24) if f != "bottom" else None)}, 103)

    # --- Général 4 étoiles de l'US Army : uniforme de service vert, 4 étoiles d'argent, casquette à visière
    rng = random.Random(110)
    p = person(rng, SKIN_TONES[:3])
    p.update(beard=None, hair="#b0b0b0", hair_style="short")
    agsu = "#3e4a36"
    stars4 = lambda f, x, y: (230, 230, 235) if f == "front" and y == 0 and x in (0, 1, 6, 7) else None
    ribbons = lambda f, x, y: [(180, 30, 30), (30, 60, 160), (220, 190, 60)][(x + y) % 3] if f == "front" and x in (1, 2) and y in (2, 3) else None
    tie = lambda f, x, y: (40, 40, 40) if f == "front" and x in (3, 4) and y <= 4 else (230, 228, 220) if f == "front" and x in (2, 5) and y <= 1 else None
    out["garrison"] = skin(p, {"top": with_details(cloth(agsu, seed=8), combine(stars4, ribbons, tie, belt("#2a2a22", "#d0b040", 9))),
                               "sleeve": with_details(cloth(agsu, seed=8), lambda f, x, y: (200, 170, 60) if y == 10 else None),
                               "pants": cloth("#3a4434", seed=8), "boots": "#141210", "gloves": None,
                               "head_over": cap("#3e4a36", "#1a1a1a", "#1a1a1a", "#d0b040")}, 110)

    # --- Commandant « Blackwater-01 » : combinaison noire, plastron lourd, béret, bandes orange
    rng = random.Random(111)
    p = person(rng, SKIN_TONES[:4])
    p.update(beard="stubble", hair="#2a2018", hair_style="short")
    plate = lambda f, x, y: (40, 42, 44) if f in ("front", "back") and 1 <= x <= 6 and 1 <= y <= 9 else None
    orange = lambda f, x, y: (200, 90, 20) if f == "front" and y == 5 else None
    out["offshore"] = skin(p, {"top": with_details(cloth("#141518", seed=9), combine(plate, orange, belt("#2a2a2a", "#c0c0c0"))),
                               "sleeve": cloth("#141518", seed=9), "pants": cloth("#141518", seed=9), "boots": "#0c0c0e",
                               "gloves": "#1a1a1a", "head_over": cap("#1a1a1a")}, 111)

    # --- Maréchal de l'Est : long manteau d'officier gris acier, chapka fourrée, plastron blindé
    rng = random.Random(104)
    p = person(rng, SKIN_TONES[:2])
    p.update(beard="full", hair="#c8c8c8", hair_style="short")
    coat = "#5a646e"
    epaulets = lambda f, x, y: gold if f == "front" and y == 0 else None
    plastron = lambda f, x, y: (70, 72, 76) if f == "front" and 2 <= x <= 5 and 2 <= y <= 7 and not (x in (2, 5) and y == 7) else None
    stars = lambda f, x, y: (200, 30, 30) if f == "front" and x in (3, 4) and y == 4 else None
    out["soviet"] = skin(p, {"top": with_details(cloth(coat, seed=6), combine(epaulets, stars, plastron, belt("#2a1e14", "#d0b040", 8))),
                             "sleeve": with_details(cloth(coat, seed=6), lambda f, x, y: gold if y == 10 else None),
                             "pants": cloth(coat, seed=6), "boots": "#141210", "boot_h": 5, "gloves": "#2a2420",
                             "top_over": lambda f, x, y, W, H: hexc(coat) if y >= 8 else None,
                             "leg_over": lambda f, x, y, W, H: hexc(coat) if y <= 6 and f != "bottom" else None,
                             "head_over": chapka("#4a4a50", "#6a5a48")}, 104)

    # --- El Patrón : costume blanc tropical ouvert sur un gilet pare-balles, lunettes teintées, chaînes en or
    rng = random.Random(105)
    p = person(rng, SKIN_TONES[1:4])
    p.update(beard="stubble", glasses=False, hair="#1a1410", hair_style="side")
    vest = lambda f, x, y: (58, 62, 52) if f == "front" and 2 <= x <= 5 and 1 <= y <= 9 else None
    chains = lambda f, x, y: gold if f == "front" and ((y == 1 and 2 <= x <= 5) or (y == 2 and x in (2, 5)) or (y == 3 and x in (3, 4))) else None
    shades = lambda f, x, y, W, H: ((30, 20, 14) if 1 <= x <= 6 and x not in (3, 4) else gold) if f == "front" and y == 4 else \
        gold if f in ("left", "right") and y == 4 and x >= 4 else None
    suit = cloth("#f0ece0", tropical("#f0ece0", "#f0a020", "#2a8a3a", 105), 105)
    out["cartel"] = skin(p, {"top": with_details(suit, combine(chains, vest)), "sleeve": suit,
                             "pants": cloth("#ece8dc", seed=7), "boots": "#e8e4d8", "tattoo": "#2a3a5a",
                             "head_over": shades}, 105)
    return out


def falcon_cape():
    """Cape blanche déchirée ornée du faucon blanc (format cape 64x32 : pavé 10x16x1 en (0, 0))."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    white, dark, edge = hexc("#ecebe6"), hexc("#2a2a30"), hexc("#c8c6c0")
    # faces : dos de la cape (1,1)-(11,17) ; devant (12,1)-(22,17) ; côtés et bords
    for y in range(17):
        for x in range(22):
            torn = y >= 14 and rnd("torn", x) < 0.35 + (y - 14) * 0.2
            if torn:
                continue
            c = white if (x + y) % 7 else edge
            c = shade(c, 0.94 + rnd("cape", x, y) * 0.1)
            px[x, y] = c + (255,)
    # emblème du faucon (sur la face extérieure, x 1..10)
    falcon = ["..........", "....##....", "...####...", "#..####..#", "##.####.##", ".########.", "..######..",
              "...####...", "....##....", "...#..#..."]
    for j, row in enumerate(falcon):
        for i, ch in enumerate(row):
            if ch == "#":
                px[1 + i, 3 + j] = dark + (255,)
    return img


FACTIONS = {"police": 6, "swat": 4, "militia": 6, "rebel": 5, "garrison": 5, "soviet": 5, "arctic": 4, "mine_security": 4,
            "cartel": 6, "gang": 5, "sheriff": 5, "refinery": 4, "civilian": 8, "trader": 4, "falcon": 6, "offshore": 5}
TONES = {"militia": SKIN_TONES[1:5], "cartel": SKIN_TONES[1:5], "soviet": SKIN_TONES[:2], "arctic": SKIN_TONES[:3]}


def main():
    os.makedirs(OUT, exist_ok=True)
    previews = []
    for faction, count in FACTIONS.items():
        for n in range(count):
            seed = zlib.crc32(f"{faction}{n}".encode())
            rng = random.Random(seed)
            p = person(rng, TONES.get(faction))
            if faction in ("militia",) and rng.random() < 0.6:
                p["beard"] = "full"
            img = skin(p, outfits(faction, rng, seed), seed)
            img.save(os.path.join(OUT, f"{faction}_{n}.png"))
            previews.append(img)
    k = knight(77)
    k.save(os.path.join(OUT, "falcon_chief_0.png"))
    previews.append(k)
    falcon_cape().save(os.path.join(OUT, "falcon_cape.png"))
    for faction, img in bosses().items():
        img.save(os.path.join(OUT, f"boss_{faction}.png"))
        previews.append(img)
    # aperçu (pour vérifier à l'œil)
    sheet = Image.new("RGBA", (12 * 68, ((len(previews) + 11) // 12) * 68), (90, 90, 90, 255))
    for i, im in enumerate(previews):
        sheet.alpha_composite(im, ((i % 12) * 68 + 2, (i // 12) * 68 + 2))
    sheet.save(os.path.join(os.environ.get("PREVIEW_DIR", OUT), "_preview.png")) if os.environ.get("PREVIEW_DIR") else None
    print(f"{len(previews)} skins + cape générés")


if __name__ == "__main__":
    main()

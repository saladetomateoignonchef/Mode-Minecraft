#!/usr/bin/env python3
"""
Décors de réalisme (aéroport, hôpital, gare, métro) : blocs orientés à modèle en boîtes de couleur, certains avec une texture animée
(tapis à bagages, moniteurs, ventilateur, néons de tunnel). Génère pour chacun : texture (palette de couleurs), modèle de bloc,
états de bloc (4 orientations), modèle d'objet, table de butin, textes FR / EN et la table Java RealismDecoData (formes, lumière).

Utilisation : python3 tools/generate_realism_decor.py
Les blocs sont enregistrés automatiquement par ModBlocks / ModItems à partir de RealismDecoData.
"""
import json
import math
import os
import random
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/modeminecraft")
DATA = os.path.join(RES, "data/modeminecraft")
JAVA = os.path.join(ROOT, "src/main/java/fr/modeminecraft/block/city/RealismDecoData.java")
MOD = "modeminecraft"
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

PAL = {
    "white": "#ececea", "offwhite": "#d8d8d4", "gray": "#8a8e94", "dgray": "#4a4e54", "black": "#1c1d20", "steel": "#b4b8bf",
    "red": "#c0281f", "dred": "#8a1c16", "blue": "#2b5aa8", "lblue": "#7fb6d8", "green": "#2f9a52", "dgreen": "#1f6a38",
    "yellow": "#e8b81e", "orange": "#e07a20", "brown": "#7a4a28", "tan": "#c8a878", "pink": "#e8a0a8", "glass": "#a8d0e4",
    "case_a": "#34506e", "case_b": "#7a2a2a", "case_c": "#3a5a3a", "case_d": "#8a6a2a", "wheel": "#202226", "belt": "#2a2c30",
    "lamp": "#fff2c0", "curtain": "#9ec8d8", "hazard": "#e8c020",
}


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


# ------------------------------------------------------------------ textures animées (16 x 16 x n images)
def anim_belt():
    """Tapis roulant : lattes claires qui défilent."""
    frames = 8
    img = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                phase = (y * 2 + f * 2) % 16
                c = hex_rgb(PAL["belt"]) if phase < 12 else (74, 76, 82)
                if x in (0, 15):
                    c = (150, 154, 160)
                img.putpixel((x, y + 16 * f), c + (255,))
    return img, {"frametime": 2}


def anim_screen(kind):
    """Écrans : « flights » (liste ambre qui défile), « ecg » (tracé cardiaque vert), « xray » (valise sur fond bleu), « ticket » (menu)."""
    frames = 8
    img = Image.new("RGBA", (16, 16 * frames))
    rnd = random.Random(kind)
    for f in range(frames):
        base = {"flights": (12, 14, 18), "ecg": (6, 16, 10), "xray": (10, 24, 52), "ticket": (14, 26, 44)}[kind]
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), base + (255,))
        if kind == "flights":
            for row in range(5):
                y = 1 + row * 3
                for x in range(1, 15):
                    if (x * 7 + row * 5 + f * (row % 2 + 1)) % 5 not in (0, 1) and x % 5 != 4:
                        img.putpixel((x, y + 16 * f), (240, 190, 40, 255))
                        img.putpixel((x, y + 1 + 16 * f), (180, 140, 30, 255))
        elif kind == "ecg":
            for x in range(16):
                px = (x + f * 2) % 16
                y = 8 - (6 if px == 3 else 3 if px == 4 else -4 if px == 5 else 0)
                img.putpixel((x, y + 16 * f), (60, 255, 120, 255))
                if x > 0:
                    img.putpixel((x, min(15, y + 1) + 16 * f), (30, 160, 70, 255))
            img.putpixel((13, 2 + 16 * f), (255, 70, 70, 255) if f % 4 < 2 else (90, 30, 30, 255))
        elif kind == "xray":
            for x in range(3, 13):
                for y in range(5, 12):
                    img.putpixel((x, y + 16 * f), (90, 150, 220, 255))
            for x in range(5, 11):
                img.putpixel((x, 4 + 16 * f), (120, 190, 255, 255))
            y = 2 + (f * 2) % 14                                          # balayage
            for x in range(16):
                img.putpixel((x, y + 16 * f), (120, 255, 200, 255))
            if f % 3 == 0:
                img.putpixel((7, 8 + 16 * f), (255, 70, 60, 255))           # objet suspect
        else:
            for row in range(4):
                for x in range(2, 14):
                    if (x + row + f // 2) % 3:
                        img.putpixel((x, 2 + row * 3 + 16 * f), (120, 210, 255, 255))
    return img, {"frametime": 3}


def anim_fan():
    frames = 4
    img = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                d = (dx * dx + dy * dy) ** 0.5
                a = math.atan2(dy, dx) + f * math.pi / 6
                if d > 7:
                    c = (150, 154, 160)
                elif d < 1.6:
                    c = (40, 42, 46)
                elif math.sin(3 * a) > 0.25:
                    c = (86, 90, 98)
                else:
                    c = (22, 24, 28)
                img.putpixel((x, y + 16 * f), c + (255,))
    return img, {"frametime": 1}


def anim_flicker():
    """Néon de tunnel : lumière blanche qui vacille."""
    frames = 8
    img = Image.new("RGBA", (16, 16 * frames))
    pattern = [1.0, 1.0, 0.95, 0.55, 1.0, 0.9, 1.0, 0.7]
    for f in range(frames):
        k = pattern[f]
        for y in range(16):
            for x in range(16):
                inner = 2 <= x <= 13 and 5 <= y <= 10
                v = int((255 if inner else 120) * (k if inner else 1))
                img.putpixel((x, y + 16 * f), (v, v, min(255, v + 10), 255))
    return img, {"frametime": 2}


ANIMS = {"belt": anim_belt, "flights": lambda: anim_screen("flights"), "ecg": lambda: anim_screen("ecg"), "xray": lambda: anim_screen("xray"),
         "ticket": lambda: anim_screen("ticket"), "fan": anim_fan, "flicker": anim_flicker}

# ------------------------------------------------------------------ définition des décors
# élément : (x0, y0, z0, x1, y1, z1, couleur[, {face: "anim"}])   (pixels, face avant = nord)
# lumière : niveau émis ; passable : sans collision ; anim : texture animée (clé de ANIMS)
DECOS = [
    # ---------------- aéroport
    dict(id="baggage_carousel", fr="Carrousel à bagages", en="Baggage carousel", anim="belt", elements=[
        (0, 0, 0, 16, 5, 16, "dgray"), (0, 5, 1, 16, 7, 15, "belt", {"up": "anim"}), (0, 5, 0, 16, 8, 1, "steel"), (0, 5, 15, 16, 8, 16, "steel")]),
    dict(id="check_in_desk", fr="Comptoir d'enregistrement", en="Check-in desk", anim="belt", light=4, elements=[
        (0, 0, 3, 16, 11, 16, "white"), (0, 11, 3, 16, 12, 16, "steel"), (0, 0, 3, 16, 2, 16, "dgray"), (2, 12, 7, 14, 13, 12, "belt", {"up": "anim"}),
        (11, 12, 12, 14, 17, 13, "black"), (11, 12, 13, 14, 16, 14, "dgray"), (1, 12, 4, 6, 13, 7, "lblue"), (1, 0, 0, 15, 11, 3, "blue")]),
    dict(id="xray_scanner", fr="Scanner à rayons X", en="X-ray baggage scanner", anim="xray", light=4, elements=[
        (0, 0, 2, 16, 3, 14, "dgray"), (0, 3, 2, 16, 12, 4, "gray"), (0, 3, 12, 16, 12, 14, "gray"), (0, 12, 2, 16, 15, 14, "dgray"),
        (3, 3, 4, 13, 4, 12, "belt", {"up": "anim"}), (1, 12, 3, 5, 16, 6, "black"), (1, 13, 2, 5, 16, 3, "black", {"north": "anim"}), (14, 3, 3, 16, 12, 4, "yellow")]),
    dict(id="baggage_cart", fr="Chariot à bagages", en="Baggage cart", elements=[
        (1, 2, 2, 15, 3, 14, "steel"), (1, 3, 13, 15, 12, 14, "steel"), (1, 3, 2, 15, 4, 3, "steel"), (2, 3, 3, 8, 8, 8, "case_a"),
        (8, 3, 4, 14, 6, 11, "case_b"), (3, 3, 8, 7, 5, 12, "case_d"), (1, 0, 2, 4, 2, 5, "wheel"), (12, 0, 2, 15, 2, 5, "wheel"),
        (1, 0, 11, 4, 2, 14, "wheel"), (12, 0, 11, 15, 2, 14, "wheel")]),
    dict(id="flight_board", fr="Panneau des vols", en="Flight information board", anim="flights", light=8, elements=[
        (0, 2, 14, 16, 14, 16, "black"), (1, 3, 13.4, 15, 13, 14.2, "black", {"north": "anim"}), (0, 14, 13, 16, 15, 16, "steel")]),
    dict(id="boarding_gate_desk", fr="Comptoir d'embarquement", en="Boarding gate desk", elements=[
        (0, 0, 5, 16, 12, 14, "white"), (0, 12, 4, 16, 13, 15, "dgray"), (2, 13, 7, 7, 15, 8, "black"), (3, 13, 8, 6, 14, 10, "dgray"),
        (9, 13, 8, 13, 14, 12, "lblue"), (0, 0, 5, 16, 3, 14, "blue")]),
    dict(id="suitcase_pile", fr="Pile de valises", en="Suitcase pile", elements=[
        (1, 0, 2, 8, 6, 8, "case_a"), (8, 0, 3, 15, 5, 9, "case_b"), (2, 6, 3, 7, 10, 7, "case_c"), (3, 0, 9, 11, 4, 14, "case_d"),
        (3, 6, 4, 5, 7, 6, "steel"), (9, 5, 4, 12, 6, 7, "steel")]),
    dict(id="runway_light", fr="Feux d'approche de piste", en="Runway approach lights", light=15, elements=[
        (5, 0, 5, 11, 3, 11, "dgray"), (6, 3, 6, 10, 9, 10, "steel"), (5, 9, 5, 11, 11, 11, "lamp"), (4, 9, 7, 5, 10, 9, "red"), (11, 9, 7, 12, 10, 9, "red")]),
    # ---------------- hôpital
    dict(id="hospital_stretcher", fr="Brancard", en="Stretcher", elements=[
        (1, 6, 1, 15, 8, 15, "steel"), (1, 8, 1, 15, 10, 15, "white"), (1, 10, 1, 6, 12, 15, "offwhite"), (0, 5, 0, 2, 6, 16, "gray"),
        (14, 5, 0, 16, 6, 16, "gray"), (2, 0, 2, 4, 6, 4, "dgray"), (12, 0, 2, 14, 6, 4, "dgray"), (2, 0, 12, 4, 6, 14, "dgray"), (12, 0, 12, 14, 6, 14, "dgray"),
        (2, 0, 2, 4, 1, 4, "wheel"), (12, 0, 2, 14, 1, 4, "wheel"), (2, 0, 12, 4, 1, 14, "wheel"), (12, 0, 12, 14, 1, 14, "wheel")]),
    dict(id="iv_stand", fr="Pied à perfusion", en="IV drip stand", elements=[
        (7, 0, 7, 9, 28, 9, "steel"), (3, 0, 3, 13, 1, 13, "dgray"), (5, 28, 6, 11, 30, 10, "steel"), (9, 18, 6, 12, 26, 10, "lblue"), (10, 14, 7.5, 11, 18, 8.5, "glass"),
        (4, 24, 7, 5, 29, 9, "steel"), (11, 24, 7, 12, 29, 9, "steel")]),
    dict(id="heart_monitor", fr="Moniteur cardiaque", en="Heart monitor", anim="ecg", light=4, elements=[
        (3, 0, 5, 13, 2, 11, "dgray"), (7, 2, 7, 9, 10, 9, "steel"), (1, 10, 4, 15, 17, 12, "black"), (2, 11, 3.4, 14, 16, 4.2, "black", {"north": "anim"}),
        (13, 8, 6, 15, 10, 8, "red")]),
    dict(id="medical_cabinet", fr="Armoire à pharmacie", en="Medicine cabinet", elements=[
        (1, 0, 3, 15, 32, 15, "white"), (2, 3, 2.6, 14, 30, 3, "glass"), (2, 15, 2.4, 14, 16, 3, "steel"), (6, 20, 2.2, 10, 21, 3, "red"), (7.5, 18.5, 2.2, 8.5, 22.5, 3, "red"),
        (1, 0, 3, 15, 2, 15, "dgray"), (3, 4, 4, 13, 12, 14, "pink"), (3, 17, 4, 13, 25, 14, "blue")]),
    dict(id="wheelchair", fr="Fauteuil roulant", en="Wheelchair", elements=[
        (3, 6, 3, 13, 8, 13, "dgray"), (3, 8, 12, 13, 20, 13, "dgray"), (2, 2, 4, 3, 10, 12, "steel"), (13, 2, 4, 14, 10, 12, "steel"),
        (1, 0, 2, 4, 9, 14, "wheel"), (12, 0, 2, 15, 9, 14, "wheel"), (4, 0, 1, 12, 3, 3, "wheel"), (3, 20, 11, 13, 22, 14, "steel"), (3, 8, 3, 13, 9, 13, "blue")]),
    dict(id="surgical_light", fr="Scialytique", en="Surgical light", light=15, elements=[
        (7, 14, 7, 9, 16, 9, "steel"), (6, 11, 6, 10, 14, 10, "steel"), (1, 9, 1, 15, 11, 15, "steel"), (2, 7, 2, 14, 9, 14, "lamp"), (4, 6, 4, 12, 7, 12, "white")]),
    dict(id="hospital_curtain", fr="Rideau d'hôpital", en="Hospital curtain", passable=True, elements=[
        (0, 0, 7, 16, 28, 8, "curtain"), (0, 28, 6.5, 16, 30, 8.5, "steel"), (3, 0, 6.8, 4, 28, 8.2, "lblue"), (8, 0, 6.8, 9, 28, 8.2, "lblue"), (13, 0, 6.8, 14, 28, 8.2, "lblue")]),
    # ---------------- gare
    dict(id="rail_signal", fr="Signal ferroviaire", en="Railway signal", light=7, elements=[
        (6, 0, 6, 10, 2, 10, "dgray"), (7, 2, 7, 9, 30, 9, "steel"), (4, 18, 4, 12, 30, 7, "black"), (5.5, 25, 3.5, 10.5, 29, 4, "red"), (5.5, 20, 3.5, 10.5, 23, 4, "green"),
        (4, 30, 4, 12, 31, 7, "dgray")]),
    dict(id="ticket_machine", fr="Distributeur de billets de train", en="Train ticket machine", anim="ticket", light=6, elements=[
        (1, 0, 3, 15, 30, 15, "blue"), (2, 14, 2.4, 14, 26, 3, "black", {"north": "anim"}), (3, 8, 2.4, 13, 12, 3, "dgray"), (5, 6, 2.2, 11, 7, 3, "black"),
        (1, 28, 3, 15, 32, 15, "steel"), (4, 2, 2.4, 12, 5, 3, "steel")]),
    # ---------------- souterrains
    dict(id="metro_turnstile", fr="Portillon de métro", en="Metro turnstile", elements=[
        (3, 0, 4, 13, 18, 12, "steel"), (4, 18, 5, 12, 20, 11, "dgray"), (5, 20, 6, 11, 21, 10, "green"), (0, 12, 7, 3, 14, 9, "glass"), (13, 12, 7, 16, 14, 9, "glass"),
        (3, 8, 2, 13, 9, 4, "yellow"), (3, 4, 12, 13, 5, 14, "yellow")]),
    dict(id="tunnel_light", fr="Néon de tunnel", en="Tunnel light", anim="flicker", light=13, passable=True, elements=[
        (1, 3, 14, 15, 7, 16, "steel"), (2, 4, 13.2, 14, 6, 14, "lamp", {"north": "anim"}), (0, 2, 14.5, 1, 8, 16, "dgray"), (15, 2, 14.5, 16, 8, 16, "dgray")]),
    dict(id="exit_sign", fr="Panneau de sortie de secours", en="Emergency exit sign", light=10, passable=True, elements=[
        (2, 4, 14, 14, 10, 16, "dgreen"), (3, 5, 13.4, 13, 9, 14, "green"), (7, 5.5, 13.2, 9, 8.5, 13.4, "white"), (10, 6, 13.2, 12, 8, 13.4, "white")]),
    dict(id="vent_fan", fr="Ventilateur de tunnel", en="Tunnel ventilation fan", anim="fan", elements=[
        (1, 1, 11, 15, 15, 16, "dgray"), (2, 2, 10.4, 14, 14, 11, "black", {"north": "anim"}), (0, 0, 12, 16, 1, 16, "steel"), (0, 15, 12, 16, 16, 16, "steel")]),
    dict(id="emergency_generator", fr="Groupe électrogène de secours", en="Emergency generator", elements=[
        (0, 0, 2, 16, 3, 14, "dgray"), (1, 3, 3, 15, 13, 13, "orange"), (1, 13, 3, 15, 14, 13, "dgray"), (11, 14, 4, 13, 22, 6, "steel"), (2, 6, 2.4, 8, 11, 3, "black"),
        (9, 5, 2.4, 14, 11, 3, "yellow"), (14, 7, 6, 15, 10, 10, "red")]),
    dict(id="electrical_cabinet", fr="Armoire électrique", en="Electrical cabinet", elements=[
        (1, 0, 4, 15, 30, 14, "gray"), (2, 1, 3.4, 14, 29, 4, "dgray"), (2, 1, 3.2, 14, 4, 3.4, "hazard"), (6, 14, 3.0, 10, 18, 3.2, "red"), (13, 15, 3.0, 14, 17, 3.2, "steel")]),
]


# deuxième série (maison, cuisine, commerce, rue, bureau, industrie, militaire) : voir realism_decor_2.py
import realism_decor_2 as r2  # noqa: E402

PAL.update(r2.PAL2)
ANIMS.update(r2.ANIMS2)
DECOS.extend(r2.DECOS2)
KEYS = list(PAL)
assert len(KEYS) <= 64, "palette trop grande (8 x 8 pastilles de 2 px)"
# troisième série : seconde palette (texture « q ») pour les couleurs supplémentaires
import realism_decor_3 as r3  # noqa: E402

PAL_B = dict(r3.PAL3)
KEYS_B = [k for k in PAL_B if k not in PAL]
assert len(KEYS_B) <= 64, "seconde palette trop grande"
ANIMS.update(r3.ANIMS3)
DECOS.extend(r3.DECOS3)
# quatrième série (bases et cités soviétiques, très détaillée)
import realism_decor_4 as r4  # noqa: E402

PAL_B.update(r4.PAL4)
KEYS_B = [k for k in PAL_B if k not in PAL]
assert len(KEYS_B) <= 64, "seconde palette trop grande"
ANIMS.update(r4.ANIMS4)
DECOS.extend(r4.DECOS4)
# cinquième série (bases et villes américaines, très détaillée)
import realism_decor_5 as r5  # noqa: E402

PAL_B.update(r5.PAL5)
KEYS_B = [k for k in PAL_B if k not in PAL]
assert len(KEYS_B) <= 64, "seconde palette trop grande : %d" % len(KEYS_B)
ANIMS.update(r5.ANIMS5)
DECOS.extend(r5.DECOS5)
assert len({d["id"] for d in DECOS}) == len(DECOS), "identifiants de décors en double"


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def palette_image(keys=None, colors=None):
    keys = KEYS if keys is None else keys
    colors = PAL if colors is None else colors
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for i, k in enumerate(keys):
        u, v = (i % 8) * 2, (i // 8) * 2
        for y in range(2):
            for x in range(2):
                img.putpixel((u + x, v + y), hex_rgb(colors[k]) + (255,))
    return img


def uv(key):
    """Zone de la palette contenant la couleur : texture « #p » (palette principale) ou « #q » (seconde palette)."""
    if key in KEYS:
        i, tex = KEYS.index(key), "#p"
    else:
        i, tex = KEYS_B.index(key), "#q"
    u, v = (i % 8) * 2, (i // 8) * 2
    return [u + 0.5, v + 0.5, u + 1.5, v + 1.5], tex


def build(d):
    name = d["id"]
    # une seule texture de palette partagée + une texture animée par bloc
    elements = []
    used_q = False
    for e in d["elements"]:
        x0, y0, z0, x1, y1, z1, col = e[:7]
        over = e[7] if len(e) > 7 else {}
        faces = {}
        for face in ("north", "south", "east", "west", "up", "down"):
            if over.get(face) == "anim":
                faces[face] = {"uv": [0, 0, 16, 16], "texture": "#a"}
            else:
                box_uv, tex_ref = uv(col)
                faces[face] = {"uv": box_uv, "texture": tex_ref}
                used_q = used_q or tex_ref == "#q"
        elements.append({"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces})
    tex = {"p": f"{MOD}:block/realism_palette", "particle": f"{MOD}:block/realism_palette"}
    if used_q:
        tex["q"] = f"{MOD}:block/realism_palette_b"
    if d.get("anim"):
        tex["a"] = f"{MOD}:block/{name}_anim"
        img, meta = ANIMS[d["anim"]]()
        path = os.path.join(ASSETS, f"textures/block/{name}_anim.png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        with open(path + ".mcmeta", "w") as f:
            json.dump({"animation": meta}, f)
    write(os.path.join(ASSETS, f"models/block/{name}.json"), {"parent": "minecraft:block/block", "textures": tex, "elements": elements})
    variants = {f"facing={f}": {"model": f"{MOD}:block/{name}", "y": y} for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}
    write(os.path.join(ASSETS, f"blockstates/{name}.json"), {"variants": variants})
    write(os.path.join(ASSETS, f"models/item/{name}.json"), {"parent": f"{MOD}:block/{name}"})
    write(os.path.join(DATA, f"loot_table/blocks/{name}.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                                                                                                         "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    xs = [min(e[0] for e in d["elements"]), max(e[3] for e in d["elements"])]
    ys = [min(e[1] for e in d["elements"]), max(e[4] for e in d["elements"])]
    zs = [min(e[2] for e in d["elements"]), max(e[5] for e in d["elements"])]
    return [max(0, xs[0]), max(0, ys[0]), max(0, zs[0]), min(16, xs[1]), min(24, ys[1]), min(16, zs[1])]


def main():
    path = os.path.join(ASSETS, "textures/block/realism_palette.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    palette_image().save(path)
    palette_image(KEYS_B, PAL_B).save(os.path.join(ASSETS, "textures/block/realism_palette_b.png"))
    rows = []
    for d in DECOS:
        box = build(d)
        rows.append(f'        new Def("{d["id"]}", {d.get("light", 0)}, {str(bool(d.get("passable"))).lower()}, {box[0]}, {box[1]}, {box[2]}, {box[3]}, {box[4]}, {box[5]})')
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, "w", encoding="utf-8") as f:
        f.write("package fr.modeminecraft.block.city;\n\n")
        f.write("/** GÉNÉRÉ par tools/generate_realism_decor.py : ne pas modifier à la main. Décors de réalisme (aéroport, hôpital, gare, métro). */\n")
        f.write("public final class RealismDecoData {\n")
        f.write("    /** Un décor : identifiant, lumière émise, sans collision, forme (pixels, face avant au nord). */\n")
        f.write("    public record Def(String id, int light, boolean passable, double x0, double y0, double z0, double x1, double y1, double z1) {}\n\n")
        f.write("    public static final Def[] ALL = {\n" + ",\n".join(rows) + "\n    };\n\n    private RealismDecoData() {}\n}\n")
    for lang in ("fr", "en"):
        p = os.path.join(ASSETS, f"lang/{lang}_{'fr' if lang == 'fr' else 'us'}.json")
        with open(p, encoding="utf-8") as fh:
            data = json.load(fh)
        for d in DECOS:
            data[f"block.{MOD}.{d['id']}"] = d[lang]
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(data, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
    print(len(DECOS), "décors générés")


if __name__ == "__main__":
    main()

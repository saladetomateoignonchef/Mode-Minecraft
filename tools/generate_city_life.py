#!/usr/bin/env python3
"""
Ville vivante (mise à jour 0.3) : caisse enregistreuse (fermée / tiroir ouvert), liasse de billets, enseignes néon
clignotantes, écrans géants, bouches d'aération fumantes, fontaines, climatiseurs, horloges murales, abribus...
Génère textures (dont animées), modèles, états de blocs, modèles d'objets et tables de butin.
"""
import json
import math
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


def save(img, rel, anim=None):
    p = os.path.join(ASSETS, "textures", rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    if anim:
        with open(p + ".mcmeta", "w") as f:
            json.dump({"animation": anim}, f)


def block_loot(name):
    write(os.path.join(DATA, "loot_table/blocks", f"{name}.json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def face_all(t, uv=(0, 0, 16, 16)):
    return {d: {"uv": list(uv), "texture": t} for d in ("north", "south", "east", "west", "up", "down")}


def noise_fill(seed, base, n=6, w=16, h=16):
    r = random.Random(seed)
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            d = r.randint(-n, n)
            img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (255,))
    return img


def facing_states(name, extra=None):
    variants = {}
    for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for key, model in (extra or {"": name}).items():
            m = {"model": f"{MOD}:block/{model}"}
            if r:
                m["y"] = r
            variants[f"facing={f}" + key] = m
    write(os.path.join(ASSETS, f"blockstates/{name}.json"), {"variants": variants})


# ------------------------------------------------------------------ caisse enregistreuse et billets
def cash_register():
    body = noise_fill(61, (58, 60, 66))
    save(body, "block/cash_register_body.png")
    keys = noise_fill(62, (40, 42, 46))
    px = keys.load()
    for y in range(2, 14, 3):
        for x in range(2, 14, 3):
            px[x, y] = px[x + 1, y] = (200, 200, 205, 255)
            px[x, y + 1] = px[x + 1, y + 1] = (150, 150, 156, 255)
    for x in range(11, 14):
        for y in range(11, 14):
            px[x, y] = (190, 40, 40, 255)
    save(keys, "block/cash_register_keys.png")
    lcd = Image.new("RGBA", (16, 16), (24, 40, 26, 255))
    for x in range(2, 14):
        if x % 3 != 1:
            lcd.putpixel((x, 7), (90, 230, 110, 255))
            lcd.putpixel((x, 8), (70, 200, 90, 255))
    save(lcd, "block/cash_register_screen.png")
    bills = noise_fill(63, (96, 150, 92), 10)
    px = bills.load()
    for y in range(0, 16, 4):
        for x in range(16):
            px[x, y] = (210, 220, 200, 255)
    save(bills, "block/cash_register_bills.png")
    # liasse de billets (objet)
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    r = random.Random(64)
    for layer in range(3):
        for y in range(5 - layer, 12 - layer):
            for x in range(2 + layer, 14 + layer - 1):
                g = 130 + r.randint(-12, 12) - layer * 10
                icon.putpixel((x, y), (70, g, 66, 255) if 3 + layer < x < 13 + layer - 2 else (190, 205, 180, 255))
    for x in range(6, 10):                                # bande de papier
        for y in range(3, 12):
            icon.putpixel((x, y), (226, 210, 150, 255) if icon.getpixel((x, y))[3] else (0, 0, 0, 0))
    save(icon, "item/cash_bundle.png")
    write(os.path.join(ASSETS, "models/item/cash_bundle.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/cash_bundle"}})
    t = {"particle": f"{MOD}:block/cash_register_body", "body": f"{MOD}:block/cash_register_body", "keys": f"{MOD}:block/cash_register_keys",
         "screen": f"{MOD}:block/cash_register_screen", "bills": f"{MOD}:block/cash_register_bills"}
    base = [
        {"from": [2, 0, 3], "to": [14, 4, 14], "faces": face_all("#body")},                                   # socle
        {"from": [3, 4, 3], "to": [13, 6, 9], "faces": {**face_all("#body"), "up": {"uv": [0, 0, 16, 16], "texture": "#keys"}}},
        {"from": [7, 4, 11], "to": [9, 9, 13], "faces": face_all("#body")},                                   # pied de l'écran
        {"from": [4, 8, 10.5], "to": [12, 11, 12], "faces": {**face_all("#body"), "north": {"uv": [0, 4, 16, 12], "texture": "#screen"}}},
    ]
    closed = base + [{"from": [3, 0.5, 2.5], "to": [13, 3, 3], "faces": face_all("#body")}]
    opened = base + [{"from": [3, 0.5, -1], "to": [13, 3, 3], "faces": {**face_all("#body"), "up": {"uv": [0, 0, 16, 6], "texture": "#bills"}}}]
    write(os.path.join(ASSETS, "models/block/cash_register.json"), {"parent": "minecraft:block/block", "textures": t, "elements": closed})
    write(os.path.join(ASSETS, "models/block/cash_register_open.json"), {"parent": "minecraft:block/block", "textures": t, "elements": opened})
    facing_states("cash_register", {",open=false": "cash_register", ",open=true": "cash_register_open"})
    write(os.path.join(ASSETS, "models/item/cash_register.json"), {"parent": f"{MOD}:block/cash_register"})
    block_loot("cash_register")


def ambient_speaker():
    grille = noise_fill(71, (34, 34, 38), 4)
    px = grille.load()
    for y in range(2, 14):
        for x in range(2, 14):
            if (x + y) % 2 == 0 and (x - 8) ** 2 + (y - 8) ** 2 < 34:
                px[x, y] = (14, 14, 16, 255)
    save(grille, "block/ambient_speaker_front.png")
    save(noise_fill(72, (52, 52, 58), 4), "block/ambient_speaker_side.png")
    write(os.path.join(ASSETS, "models/block/ambient_speaker.json"), {
        "parent": "minecraft:block/block", "textures": {"particle": f"{MOD}:block/ambient_speaker_side", "f": f"{MOD}:block/ambient_speaker_front",
                                                        "s": f"{MOD}:block/ambient_speaker_side"},
        "elements": [{"from": [3, 3, 9], "to": [13, 13, 16], "faces": {**face_all("#s"), "north": {"uv": [0, 0, 16, 16], "texture": "#f"}}},
                     {"from": [7, 13, 12], "to": [9, 16, 14], "faces": face_all("#s")}]})
    facing_states("ambient_speaker")
    write(os.path.join(ASSETS, "models/item/ambient_speaker.json"), {"parent": f"{MOD}:block/ambient_speaker"})
    block_loot("ambient_speaker")


def birds():
    for name, base, head, wing, beak, legs in (("pigeon", (128, 132, 142), (96, 100, 116), (110, 114, 124), (60, 60, 64), (196, 96, 96)),
                                               ("gull", (232, 234, 236), (240, 240, 242), (150, 156, 164), (232, 190, 40), (226, 176, 70))):
        r = random.Random(81 if name == "pigeon" else 82)
        img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        px = img.load()
        def fill(x0, y0, x1, y1, col, n=8):
            for y in range(y0, y1):
                for x in range(x0, x1):
                    d = r.randint(-n, n)
                    px[x, y] = tuple(max(0, min(255, c + d)) for c in col) + (255,)
        fill(0, 0, 18, 9, base)                      # corps
        fill(0, 9, 8, 13, head)                      # tête
        if name == "pigeon":                          # reflets irisés du cou
            for x in range(0, 8):
                px[x, 12] = (90 + r.randint(0, 30), 140 + r.randint(0, 30), 110, 255) if x % 2 else (140, 90, 150, 255)
        fill(8, 9, 12, 11, beak, 4)                  # bec
        fill(0, 13, 24, 20, wing)                    # ailes
        for x in range(0, 24):                       # pointes des ailes
            px[x, 19] = (40, 40, 44, 255) if name == "gull" else (70, 72, 80, 255)
        fill(12, 9, 22, 13, wing)                    # queue
        fill(18, 0, 22, 4, legs, 4)                  # pattes
        save(img, f"entity/{name}.png")


def neon(name, color, text_rows, seed):
    """Enseigne néon : tubes lumineux qui clignotent (image animée : allumé, vacillement, éteint un instant)."""
    frames = 8
    img = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 0))
    r = random.Random(seed)
    for f in range(frames):
        on = 1.0 if f not in (5, 6) else (0.35 if f == 5 else 0.12)
        for y in range(16):
            for x in range(16):
                back = (18, 18, 22, 255) if 1 <= y <= 14 else (0, 0, 0, 0)
                img.putpixel((x, y + 16 * f), back)
        for (x0, y0, x1, y1) in text_rows:
            for y in range(y0, y1 + 1):
                for x in range(x0, x1 + 1):
                    c = tuple(int(min(255, ch * on + (30 if on > 0.9 else 0))) for ch in color)
                    img.putpixel((x, y + 16 * f), c + (255,))
    save(img, f"block/{name}.png", {"frametime": 4})
    write(os.path.join(ASSETS, f"models/block/{name}.json"), {"parent": "minecraft:block/block", "textures": {"particle": f"{MOD}:block/{name}", "n": f"{MOD}:block/{name}"},
        "elements": [{"from": [0, 2, 14.5], "to": [16, 14, 16], "shade": False, "faces": {"north": {"uv": [0, 2, 16, 14], "texture": "#n"},
                      "south": {"uv": [0, 2, 16, 14], "texture": "#n"}, "up": {"uv": [0, 0, 16, 1], "texture": "#n"}, "down": {"uv": [0, 0, 16, 1], "texture": "#n"},
                      "east": {"uv": [0, 2, 1, 14], "texture": "#n"}, "west": {"uv": [0, 2, 1, 14], "texture": "#n"}}}]})
    facing_states(name)
    write(os.path.join(ASSETS, f"models/item/{name}.json"), {"parent": f"{MOD}:block/{name}"})
    block_loot(name)


def giant_screen():
    """Écran géant 4 x 2 : publicité animée (dégradés, bandeau de texte qui défile), découpée en 8 tuiles."""
    W, H, frames = 64, 32, 12
    full = []
    r = random.Random(91)
    for f in range(frames):
        img = Image.new("RGBA", (W, H))
        scene = (f // 4) % 3
        for y in range(H):
            for x in range(W):
                if scene == 0:
                    c = (40 + y * 4, 20, 120 + x)
                elif scene == 1:
                    c = (220, 120 + y * 3, 30 + x // 2)
                else:
                    c = (20 + x // 2, 140 + y * 2, 160)
                if y in (0, H - 1) or x in (0, W - 1):
                    c = (12, 12, 14)
                img.putpixel((x, y), tuple(min(255, v) for v in c) + (255,))
        for x in range(W):                                # bandeau défilant
            if ((x + f * 5) // 3) % 4 != 0:
                for y in range(22, 27):
                    if (x * 7 + y * 3 + f) % 5:
                        img.putpixel((x, y), (250, 250, 250, 255))
        for k in range(6):                                # « logo »
            cx, cy = 14 + k * 7, 10
            for y in range(cy - 3, cy + 3):
                for x in range(cx - 2, cx + 2):
                    if 0 < x < W - 1:
                        img.putpixel((x, y), (255, 255, 255, 255) if (k + f) % 3 else (255, 230, 60, 255))
        full.append(img)
    for part in range(8):
        tx, ty = part % 4, part // 4
        tile = Image.new("RGBA", (16, 16 * frames))
        for f in range(frames):
            tile.paste(full[f].crop((tx * 16, ty * 16, tx * 16 + 16, ty * 16 + 16)), (0, 16 * f))
        save(tile, f"block/giant_screen_{part}.png", {"frametime": 5})
        write(os.path.join(ASSETS, f"models/block/giant_screen_{part}.json"), {
            "parent": "minecraft:block/block", "textures": {"particle": f"{MOD}:block/giant_screen_{part}", "s": f"{MOD}:block/giant_screen_{part}",
                                                            "b": f"{MOD}:block/ambient_speaker_side"},
            "elements": [{"from": [0, 0, 13], "to": [16, 16, 16], "shade": False, "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#s"},
                          "south": {"uv": [0, 0, 16, 16], "texture": "#b"}, "east": {"uv": [0, 0, 3, 16], "texture": "#b"},
                          "west": {"uv": [0, 0, 3, 16], "texture": "#b"}, "up": {"uv": [0, 0, 16, 3], "texture": "#b"}, "down": {"uv": [0, 0, 16, 3], "texture": "#b"}}}]})
    variants = {}
    for fdir, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for part in range(8):
            m = {"model": f"{MOD}:block/giant_screen_{part}"}
            if rot:
                m["y"] = rot
            variants[f"facing={fdir},part={part}"] = m
    write(os.path.join(ASSETS, "blockstates/giant_screen.json"), {"variants": variants})
    write(os.path.join(ASSETS, "models/item/giant_screen.json"), {"parent": f"{MOD}:block/giant_screen_1"})
    block_loot("giant_screen")


def small_decos():
    # bouche d'égout fumante : plaque en fonte à motifs
    cover = noise_fill(101, (58, 58, 62), 5)
    px = cover.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 7.6:
                px[x, y] = (0, 0, 0, 0)
            elif abs(d - 7) < 0.6 or (x + y) % 4 == 0 and d < 6:
                px[x, y] = (36, 36, 40, 255)
    save(cover, "block/steam_vent.png")
    write(os.path.join(ASSETS, "models/block/steam_vent.json"), {"parent": "minecraft:block/block", "textures": {"particle": f"{MOD}:block/steam_vent", "c": f"{MOD}:block/steam_vent"},
        "elements": [{"from": [0, 0, 0], "to": [16, 0.6, 16], "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#c"}, "down": {"uv": [0, 0, 16, 16], "texture": "#c"}}}]})
    # jet de fontaine : bec en pierre
    stone = noise_fill(102, (150, 150, 146), 8)
    save(stone, "block/fountain_spout.png")
    write(os.path.join(ASSETS, "models/block/fountain_spout.json"), {"parent": "minecraft:block/block", "textures": {"particle": f"{MOD}:block/fountain_spout", "s": f"{MOD}:block/fountain_spout"},
        "elements": [{"from": [5, 0, 5], "to": [11, 2, 11], "faces": face_all("#s")}, {"from": [6.5, 2, 6.5], "to": [9.5, 9, 9.5], "faces": face_all("#s")},
                     {"from": [5.5, 9, 5.5], "to": [10.5, 10, 10.5], "faces": face_all("#s")}]})
    # climatiseur : grille latérale, ventilateur animé sur le dessus
    side = noise_fill(103, (176, 178, 180), 4)
    px = side.load()
    for y in range(2, 14, 2):
        for x in range(2, 14):
            px[x, y] = (110, 112, 116, 255)
    save(side, "block/ac_unit_side.png")
    frames = 4
    fan = Image.new("RGBA", (16, 16 * frames))
    import math
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                d = (dx * dx + dy * dy) ** 0.5
                a = math.atan2(dy, dx) + f * math.pi / 8
                if d > 7:
                    c = (176, 178, 180)
                elif d < 1.5:
                    c = (60, 60, 64)
                elif math.sin(3 * a) > 0.3:
                    c = (70, 72, 78)
                else:
                    c = (24, 24, 28)
                if abs(d - 6.8) < 0.5 or (d < 7 and (x == 7 or y == 7)):
                    c = (130, 132, 136)                    # grille de protection
                fan.putpixel((x, y + 16 * f), c + (255,))
    save(fan, "block/ac_unit_fan.png", {"frametime": 1})
    write(os.path.join(ASSETS, "models/block/ac_unit.json"), {"parent": "minecraft:block/block", "textures": {"particle": f"{MOD}:block/ac_unit_side",
        "s": f"{MOD}:block/ac_unit_side", "t": f"{MOD}:block/ac_unit_fan"},
        "elements": [{"from": [1, 0, 1], "to": [15, 11, 15], "faces": {**face_all("#s"), "up": {"uv": [0, 0, 16, 16], "texture": "#t"}}}]})
    # horloge murale : cadran blanc, chiffres, cerclage noir
    face = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 6.2:
                face.putpixel((x, y), (236, 232, 222, 255))
            elif d < 7.6:
                face.putpixel((x, y), (30, 30, 34, 255))
    for k in range(12):
        a = k / 12 * 2 * math.pi
        face.putpixel((int(round(7.5 + math.sin(a) * 5)), int(round(7.5 - math.cos(a) * 5))), (20, 20, 20, 255))
    save(face, "block/wall_clock.png")
    write(os.path.join(ASSETS, "models/block/wall_clock.json"), {"parent": "minecraft:block/block", "textures": {"particle": f"{MOD}:block/wall_clock", "f": f"{MOD}:block/wall_clock",
        "b": f"{MOD}:block/ambient_speaker_side"},
        "elements": [{"from": [1, 1, 15], "to": [15, 15, 16], "faces": {"north": {"uv": [1, 1, 15, 15], "texture": "#f"}, "south": {"uv": [1, 1, 15, 15], "texture": "#b"},
                      "east": {"uv": [0, 0, 1, 14], "texture": "#b"}, "west": {"uv": [0, 0, 1, 14], "texture": "#b"}, "up": {"uv": [0, 0, 14, 1], "texture": "#b"},
                      "down": {"uv": [0, 0, 14, 1], "texture": "#b"}}}]})
    # cône de chantier : orange à bande blanche
    cone = noise_fill(104, (236, 110, 24), 6)
    for y in (6, 7):
        for x in range(16):
            cone.putpixel((x, y), (240, 240, 240, 255))
    save(cone, "block/traffic_cone.png")
    write(os.path.join(ASSETS, "models/block/traffic_cone.json"), {"parent": "minecraft:block/block", "textures": {"particle": f"{MOD}:block/traffic_cone", "c": f"{MOD}:block/traffic_cone"},
        "elements": [{"from": [3, 0, 3], "to": [13, 1, 13], "faces": face_all("#c")}, {"from": [5, 1, 5], "to": [11, 5, 11], "faces": face_all("#c")},
                     {"from": [6, 5, 6], "to": [10, 9, 10], "faces": face_all("#c")}, {"from": [7, 9, 7], "to": [9, 12, 9], "faces": face_all("#c")}]})
    for name in ("steam_vent", "fountain_spout", "ac_unit", "wall_clock", "traffic_cone"):
        facing_states(name)
        write(os.path.join(ASSETS, f"models/item/{name}.json"), {"parent": f"{MOD}:block/{name}"})
        block_loot(name)


if __name__ == "__main__":
    cash_register()
    neon("neon_sign", (255, 60, 170), [(2, 4, 13, 4), (2, 11, 13, 11), (2, 4, 2, 11), (13, 4, 13, 11), (5, 6, 5, 9), (7, 6, 8, 6), (7, 9, 8, 9), (10, 6, 10, 9)], 111)
    neon("neon_sign_cyan", (40, 230, 255), [(2, 5, 4, 10), (6, 5, 8, 5), (6, 10, 8, 10), (7, 5, 7, 10), (10, 5, 13, 5), (10, 8, 12, 8), (10, 5, 10, 10), (10, 10, 13, 10)], 112)
    giant_screen()
    small_decos()
    birds()
    ambient_speaker()
    print("ville vivante : assets générés")

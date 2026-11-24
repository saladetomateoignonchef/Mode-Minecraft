"""
Deuxième série de décors (maison, cuisine / salle de bain / chambre, commerce, rue / jardin / parc, bureau / école,
industrie / chantier, bases militaires / prison). Importé par generate_realism_decor.py.
Format : voir DECOS dans generate_realism_decor.py (pixels, face avant au nord).
"""
import math
import random

from PIL import Image

PAL2 = {
    "wood": "#9a6b3c", "dwood": "#5a3a22", "lwood": "#c8a070", "fabric_r": "#a83a3a", "fabric_b": "#3a5a9a", "fabric_g": "#4a7a4a",
    "leaf": "#3a8a3a", "dleaf": "#256a2a", "soil": "#4a3020", "pot": "#b0603a", "book_a": "#8a2a2a", "book_b": "#2a4a8a", "book_c": "#c8a030",
    "cream": "#efe6cf", "water": "#3a78c8", "stone": "#9a9a96", "flame": "#ff8a20", "rubber": "#101012", "mint": "#a8d8c0", "sky": "#87b8e8",
    "olive": "#4b5a2a", "khaki": "#8a7a4a", "chrome": "#cfd3d8", "slate": "#2c3a30", "cardboard": "#b08a58", "navy_d": "#1c2a4a",
}


def _frames(n):
    return Image.new("RGBA", (16, 16 * n))


def anim_tv():
    """Télévision : image qui change (paysage, rayures, journal)."""
    img = _frames(8)
    rnd = random.Random(7)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                if f % 4 == 0:        # paysage : ciel, colline
                    c = (110, 170, 230) if y < 8 + int(2 * math.sin(x / 3)) else (60, 140, 70)
                elif f % 4 == 1:      # mire de couleurs
                    c = [(230, 230, 230), (230, 210, 40), (40, 210, 220), (60, 210, 70), (220, 60, 200), (220, 50, 50), (50, 60, 220), (20, 20, 20)][x // 2]
                elif f % 4 == 2:      # journal télévisé
                    c = (30, 50, 120) if y < 11 else (200, 200, 210)
                    if 4 <= x <= 11 and 2 <= y <= 8:
                        c = (230, 190, 160)
                else:                 # neige
                    v = rnd.randint(60, 200)
                    c = (v, v, v)
                img.putpixel((x, y + 16 * f), c + (255,))
    return img, {"frametime": 14}


def anim_fire():
    img = _frames(6)
    rnd = random.Random(11)
    for f in range(6):
        for y in range(16):
            for x in range(16):
                h = 5 + 8 * (1 - abs(x - 7.5) / 8) + rnd.randint(-2, 2) + (f % 3)
                depth = 15 - y
                if depth < h:
                    t = depth / max(1, h)
                    c = (255, int(210 - 150 * t), int(40 * (1 - t)))
                else:
                    c = (30, 20, 16)
                img.putpixel((x, y + 16 * f), c + (255,))
    return img, {"frametime": 3}


def anim_fish():
    img = _frames(8)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                c = (40, 110, 170) if y < 14 else (200, 180, 120)
                img.putpixel((x, y + 16 * f), c + (255,))
        for k, (row, col, spd) in enumerate(((4, (255, 150, 30), 2), (8, (230, 230, 240), 1), (11, (230, 60, 60), 3))):
            px = (f * spd + k * 5) % 16
            img.putpixel((px, row + 16 * f), col + (255,))
            img.putpixel(((px - 1) % 16, row + 16 * f), col + (255,))
            img.putpixel(((px - 2) % 16, row - 1 + 16 * f), col + (255,))
        img.putpixel(((f * 2 + 3) % 16, (12 - f) % 12 + 16 * f), (220, 240, 255, 255))   # bulle
    return img, {"frametime": 3}


def anim_drum():
    """Machine à laver : tambour qui tourne, linge coloré."""
    img = _frames(8)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                d = (dx * dx + dy * dy) ** 0.5
                if d > 7:
                    c = (220, 222, 226)
                elif d > 5.5:
                    c = (150, 154, 160)
                else:
                    a = math.atan2(dy, dx) + f * math.pi / 4
                    c = (70, 120, 200) if math.sin(2 * a) > 0.3 and d > 2 else (200, 200, 205)
                img.putpixel((x, y + 16 * f), c + (255,))
    return img, {"frametime": 2}


def anim_desktop():
    img = _frames(8)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (20, 34, 60) + (255,))
        for row in range(5):
            ln = 4 + (row * 5 + f * 3) % 9
            for x in range(2, 2 + ln):
                img.putpixel((x, 2 + row * 2 + 16 * f), (120, 220, 140, 255) if row % 2 else (140, 180, 255, 255))
        if f % 2 == 0:
            img.putpixel((12, 13 + 16 * f), (240, 240, 240, 255))
    return img, {"frametime": 4}


def anim_blink():
    """Pupitre de commande : voyants qui clignotent."""
    img = _frames(8)
    rnd = random.Random(5)
    cols = [(60, 255, 100), (255, 60, 60), (255, 200, 40), (80, 160, 255)]
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (30, 32, 36, 255))
        for i in range(4):
            for j in range(3):
                on = rnd.random() < 0.55
                col = cols[(i + j) % 4] if on else (60, 62, 66)
                for dx in range(2):
                    for dy in range(2):
                        img.putpixel((2 + i * 3 + dx, 2 + j * 4 + dy + 16 * f), col + (255,))
    return img, {"frametime": 6}


def anim_atm():
    img = _frames(6)
    for f in range(6):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (12, 40, 24, 255))
        for x in range(2, 14):
            img.putpixel((x, 3 + 16 * f), (90, 240, 150, 255))
        for row in range(3):
            if (row + f) % 3:
                for x in range(3, 13):
                    img.putpixel((x, 7 + row * 2 + 16 * f), (60, 180, 110, 255))
    return img, {"frametime": 8}


ANIMS2 = {"tv": anim_tv, "fire": anim_fire, "fish": anim_fish, "drum": anim_drum, "desktop": anim_desktop, "blink": anim_blink, "atm": anim_atm}

DECOS2 = [
    # ======================================================= MAISON ET INTÉRIEUR
    dict(id="sofa", fr="Canapé", en="Sofa", elements=[
        (0, 0, 3, 16, 5, 16, "dwood"), (0, 5, 3, 16, 9, 14, "fabric_b"), (0, 5, 11, 16, 15, 16, "fabric_b"), (0, 5, 3, 2, 12, 14, "fabric_b"), (14, 5, 3, 16, 12, 14, "fabric_b"),
        (2, 9, 3, 14, 10, 11, "fabric_b"), (4, 10, 12, 12, 14, 15, "cream")]),
    dict(id="armchair", fr="Fauteuil club", en="Armchair", elements=[
        (2, 0, 3, 14, 4, 15, "dwood"), (2, 4, 3, 14, 8, 13, "fabric_r"), (2, 4, 11, 14, 16, 15, "fabric_r"), (1, 4, 3, 3, 11, 14, "fabric_r"), (13, 4, 3, 15, 11, 14, "fabric_r")]),
    dict(id="coffee_table", fr="Table basse", en="Coffee table", elements=[
        (1, 7, 1, 15, 9, 15, "wood"), (1, 0, 1, 3, 7, 3, "dwood"), (13, 0, 1, 15, 7, 3, "dwood"), (1, 0, 13, 3, 7, 15, "dwood"), (13, 0, 13, 15, 7, 15, "dwood"), (6, 9, 6, 10, 11, 10, "cream")]),
    dict(id="dining_table", fr="Table à manger", en="Dining table", elements=[
        (0, 11, 2, 16, 13, 14, "lwood"), (0, 0, 2, 2, 11, 4, "wood"), (14, 0, 2, 16, 11, 4, "wood"), (0, 0, 12, 2, 11, 14, "wood"), (14, 0, 12, 16, 11, 14, "wood"),
        (5, 13, 5, 11, 14, 11, "cream"), (7, 14, 7, 9, 16, 9, "red")]),
    dict(id="tv_set", fr="Télévision et meuble TV", en="TV set and stand", anim="tv", light=6, elements=[
        (0, 0, 3, 16, 7, 15, "dwood"), (1, 1, 3.5, 15, 6, 5, "wood"), (6, 7, 7, 10, 9, 9, "black"), (1, 9, 7, 15, 19, 9, "black"), (2, 10, 6.4, 14, 18, 7, "black", {"north": "anim"})]),
    dict(id="bookshelf", fr="Bibliothèque", en="Bookshelf", elements=[
        (0, 0, 8, 16, 30, 16, "dwood"), (1, 1, 9, 15, 7, 16, "book_a"), (1, 8, 9, 15, 14, 16, "book_b"), (1, 15, 9, 15, 21, 16, "book_c"), (1, 22, 9, 15, 29, 16, "book_a"),
        (3, 1, 8.4, 4, 7, 9, "white"), (8, 8, 8.4, 9, 14, 9, "white"), (12, 15, 8.4, 13, 21, 9, "white"), (5, 22, 8.4, 6, 29, 9, "white")]),
    dict(id="fireplace", fr="Cheminée", en="Fireplace", anim="fire", light=13, elements=[
        (0, 0, 8, 16, 18, 16, "stone"), (2, 1, 7.4, 14, 11, 8, "black", {"north": "anim"}), (0, 18, 7, 16, 20, 16, "dwood"), (6, 20, 12, 10, 28, 16, "stone"), (4, 0, 7, 12, 1, 9, "dgray")]),
    dict(id="standing_lamp", fr="Lampadaire d'intérieur", en="Standing lamp", light=12, elements=[
        (4, 0, 4, 12, 1, 12, "dgray"), (7, 1, 7, 9, 22, 9, "steel"), (3, 22, 3, 13, 30, 13, "lamp"), (3, 30, 3, 13, 31, 13, "cream")]),
    dict(id="floor_plant", fr="Ficus en pot", en="Potted ficus", elements=[
        (4, 0, 4, 12, 7, 12, "pot"), (5, 7, 5, 11, 8, 11, "soil"), (7, 8, 7, 9, 18, 9, "dwood"), (3, 14, 3, 13, 22, 13, "leaf"), (5, 22, 5, 11, 28, 11, "dleaf"), (2, 18, 6, 14, 20, 10, "dleaf")]),
    dict(id="potted_cactus", fr="Cactus en pot", en="Potted cactus", elements=[
        (4, 0, 4, 12, 5, 12, "pot"), (5, 5, 5, 11, 6, 11, "soil"), (6, 6, 6, 10, 15, 10, "dleaf"), (3, 9, 7, 6, 11, 9, "dleaf"), (3, 9, 7, 5, 14, 9, "dleaf"), (10, 8, 7, 13, 10, 9, "dleaf"), (12, 8, 7, 13, 12, 9, "dleaf")]),
    dict(id="round_rug", fr="Tapis rond", en="Round rug", passable=True, elements=[
        (2, 0, 2, 14, 1, 14, "fabric_r"), (0, 0, 5, 16, 1, 11, "fabric_r"), (5, 0, 0, 11, 1, 16, "fabric_r"), (4, 1, 4, 12, 1.2, 12, "cream"), (6, 1.2, 6, 10, 1.4, 10, "fabric_r")]),
    dict(id="aquarium", fr="Aquarium", en="Aquarium", anim="fish", light=7, elements=[
        (0, 0, 2, 16, 8, 14, "dwood"), (1, 8, 3, 15, 20, 13, "glass"), (2, 9, 3.4, 14, 19, 3.8, "water", {"north": "anim"}), (1, 20, 3, 15, 21, 13, "black"), (1, 8, 3, 15, 9, 13, "sand_a" if False else "tan")]),
    dict(id="upright_piano", fr="Piano droit", en="Upright piano", elements=[
        (0, 0, 5, 16, 22, 16, "black"), (0, 6, 1, 16, 8, 5, "black"), (1, 8, 1, 15, 9, 5, "white"), (3, 8, 3, 4, 9, 5, "black"), (6, 8, 3, 7, 9, 5, "black"), (9, 8, 3, 10, 9, 5, "black"),
        (12, 8, 3, 13, 9, 5, "black"), (2, 0, 1, 14, 6, 5, "black"), (6, 16, 4.6, 10, 20, 5, "book_c")]),
    dict(id="wall_painting", fr="Tableau", en="Wall painting", passable=True, elements=[
        (1, 2, 14.5, 15, 14, 16, "dwood"), (2, 3, 14, 14, 13, 14.5, "sky"), (2, 3, 13.8, 14, 7, 14.2, "leaf"), (9, 9, 13.6, 13, 12, 14, "yellow")]),
    dict(id="wall_mirror", fr="Miroir", en="Wall mirror", passable=True, elements=[
        (2, 1, 14.5, 14, 15, 16, "steel"), (3, 2, 14, 13, 14, 14.5, "glass")]),
    # ======================================================= CUISINE, SALLE DE BAIN, CHAMBRE
    dict(id="kitchen_stove", fr="Cuisinière", en="Kitchen stove", elements=[
        (0, 0, 2, 16, 15, 16, "white"), (0, 15, 2, 16, 16, 16, "dgray"), (2, 16, 4, 6, 17, 8, "black"), (10, 16, 4, 14, 17, 8, "black"), (2, 16, 10, 6, 17, 14, "black"), (10, 16, 10, 14, 17, 14, "black"),
        (2, 3, 1.4, 14, 10, 2, "black"), (4, 11, 1.4, 12, 12, 2, "steel"), (3, 13, 1.4, 5, 15, 2, "red"), (11, 13, 1.4, 13, 15, 2, "red")]),
    dict(id="fridge", fr="Réfrigérateur", en="Fridge", elements=[
        (1, 0, 2, 15, 30, 15, "offwhite"), (1, 19, 1.4, 15, 19.6, 2, "dgray"), (12, 22, 1, 13, 28, 2, "steel"), (12, 6, 1, 13, 16, 2, "steel"), (2, 0, 1, 14, 1, 2, "dgray")]),
    dict(id="kitchen_sink", fr="Évier de cuisine", en="Kitchen sink", elements=[
        (0, 0, 2, 16, 14, 16, "wood"), (0, 14, 2, 16, 15, 16, "stone"), (3, 14, 5, 13, 15, 13, "steel"), (7, 15, 10, 9, 19, 12, "chrome"), (7, 18, 7, 9, 19, 11, "chrome"), (1, 2, 1.4, 15, 12, 2, "lwood")]),
    dict(id="kitchen_counter", fr="Plan de travail", en="Kitchen counter", elements=[
        (0, 0, 2, 16, 14, 16, "lwood"), (0, 14, 2, 16, 16, 16, "stone"), (1, 2, 1.4, 7, 12, 2, "wood"), (9, 2, 1.4, 15, 12, 2, "wood"), (6, 8, 1, 7, 10, 2, "steel"), (9, 8, 1, 10, 10, 2, "steel")]),
    dict(id="microwave", fr="Four à micro-ondes", en="Microwave", anim="blink", light=3, elements=[
        (1, 0, 4, 15, 9, 14, "dgray"), (2, 1, 3.4, 10, 8, 4, "black"), (11, 1, 3.4, 14, 8, 4, "gray"), (11.5, 5, 3.2, 13.5, 7, 3.4, "blink_off" if False else "green")]),
    dict(id="bathtub", fr="Baignoire", en="Bathtub", elements=[
        (0, 0, 0, 16, 9, 16, "white"), (1, 4, 1, 15, 9, 15, "mint"), (2, 5, 2, 14, 9, 14, "water"), (7, 9, 0, 9, 14, 2, "chrome"), (7, 12, 2, 9, 14, 6, "chrome")]),
    dict(id="shower_cabin", fr="Cabine de douche", en="Shower cabin", passable=True, elements=[
        (0, 0, 0, 16, 1, 16, "white"), (0, 1, 0, 1, 30, 16, "glass"), (0, 1, 15, 16, 30, 16, "glass"), (15, 1, 0, 16, 30, 16, "glass"), (0, 30, 0, 16, 31, 16, "steel"),
        (7, 22, 1, 9, 25, 3, "chrome"), (6, 25, 0.5, 10, 27, 5, "chrome")]),
    dict(id="toilet", fr="Toilettes", en="Toilet", elements=[
        (4, 0, 6, 12, 5, 14, "white"), (3, 5, 5, 13, 8, 15, "white"), (5, 8, 11, 11, 18, 15, "white"), (7, 18, 12, 9, 19, 14, "chrome"), (5, 8, 6, 11, 9, 12, "cream")]),
    dict(id="bathroom_sink", fr="Lavabo", en="Bathroom sink", elements=[
        (6, 0, 9, 10, 10, 13, "white"), (2, 10, 5, 14, 14, 15, "white"), (3, 13, 6, 13, 14, 14, "mint"), (7, 14, 12, 9, 18, 14, "chrome"), (7, 17, 9, 9, 18, 13, "chrome")]),
    dict(id="washing_machine", fr="Lave-linge", en="Washing machine", anim="drum", elements=[
        (1, 0, 2, 15, 15, 15, "white"), (3, 3, 1.4, 13, 13, 2, "black", {"north": "anim"}), (2, 13.5, 1.4, 14, 14.5, 2, "dgray"), (11, 14, 1.2, 13, 15, 1.6, "red")]),
    dict(id="wardrobe", fr="Armoire", en="Wardrobe", elements=[
        (0, 0, 2, 16, 32, 16, "wood"), (1, 1, 1.4, 8, 31, 2, "lwood"), (8, 1, 1.4, 15, 31, 2, "lwood"), (6.5, 14, 1, 7.5, 19, 1.6, "steel"), (8.5, 14, 1, 9.5, 19, 1.6, "steel"), (0, 0, 2, 16, 2, 16, "dwood")]),
    dict(id="dresser", fr="Commode", en="Dresser", elements=[
        (0, 0, 3, 16, 15, 16, "wood"), (1, 11, 2.4, 15, 14, 3, "lwood"), (1, 6, 2.4, 15, 10, 3, "lwood"), (1, 1, 2.4, 15, 5, 3, "lwood"), (7, 12, 2, 9, 13, 2.4, "steel"),
        (7, 7.5, 2, 9, 8.5, 2.4, "steel"), (7, 2.5, 2, 9, 3.5, 2.4, "steel")]),
    dict(id="nightstand", fr="Table de chevet", en="Nightstand", elements=[
        (3, 0, 3, 13, 11, 13, "wood"), (4, 6, 2.4, 12, 10, 3, "lwood"), (4, 1, 2.4, 12, 5, 3, "lwood"), (7, 7.5, 2, 9, 8.5, 2.4, "steel"), (7, 2.5, 2, 9, 3.5, 2.4, "steel")]),
    dict(id="bedside_lamp", fr="Lampe de chevet", en="Bedside lamp", light=10, elements=[
        (5, 0, 5, 11, 1, 11, "dgray"), (7, 1, 7, 9, 6, 9, "steel"), (4, 6, 4, 12, 12, 12, "lamp"), (4, 12, 4, 12, 13, 12, "cream")]),
    # ======================================================= COMMERCE ET RESTAURATION
    dict(id="supermarket_shelf", fr="Rayon de supermarché", en="Supermarket shelf", elements=[
        (0, 0, 4, 16, 30, 12, "steel"), (1, 2, 4.5, 15, 7, 11.5, "red"), (1, 9, 4.5, 15, 14, 11.5, "yellow"), (1, 16, 4.5, 15, 21, 11.5, "blue"), (1, 23, 4.5, 15, 28, 11.5, "green"),
        (0, 7, 3.6, 16, 8, 12.4, "gray"), (0, 14, 3.6, 16, 15, 12.4, "gray"), (0, 21, 3.6, 16, 22, 12.4, "gray")]),
    dict(id="shopping_cart", fr="Caddie", en="Shopping cart", elements=[
        (2, 4, 2, 14, 5, 14, "chrome"), (2, 5, 2, 3, 12, 14, "steel"), (13, 5, 2, 14, 12, 14, "steel"), (2, 5, 13, 14, 12, 14, "steel"), (2, 12, 13, 14, 13, 14, "red"),
        (2, 0, 2, 4, 4, 4, "wheel"), (12, 0, 2, 14, 4, 4, "wheel"), (2, 0, 12, 4, 4, 14, "wheel"), (12, 0, 12, 14, 4, 14, "wheel"), (4, 6, 5, 11, 9, 10, "cardboard")]),
    dict(id="market_stall", fr="Étal de marché", en="Market stall", elements=[
        (0, 0, 6, 16, 12, 16, "wood"), (1, 12, 7, 15, 13, 15, "lwood"), (2, 13, 8, 6, 16, 12, "red"), (7, 13, 8, 11, 16, 12, "yellow"), (11, 13, 9, 14, 15, 13, "green"),
        (0, 22, 3, 16, 24, 16, "red"), (0, 0, 3, 1, 22, 5, "dwood"), (15, 0, 3, 16, 22, 5, "dwood")]),
    dict(id="display_case", fr="Vitrine réfrigérée", en="Display case", light=5, elements=[
        (0, 0, 3, 16, 8, 15, "white"), (0, 8, 3, 16, 16, 15, "glass"), (1, 9, 6, 15, 10, 14, "gray"), (2, 10, 7, 6, 13, 12, "pink"), (7, 10, 7, 11, 13, 12, "cream"), (12, 10, 7, 14, 12, 12, "yellow"),
        (0, 16, 3, 16, 17, 15, "steel")]),
    dict(id="coffee_machine", fr="Machine à café", en="Coffee machine", elements=[
        (2, 0, 4, 14, 3, 14, "dgray"), (3, 3, 5, 13, 14, 13, "black"), (4, 14, 5, 12, 16, 13, "steel"), (6, 3, 3, 10, 8, 5, "chrome"), (7, 1, 6, 9, 3, 8, "white"), (11, 9, 4.4, 13, 11, 5, "red")]),
    dict(id="bar_counter", fr="Comptoir de bar", en="Bar counter", elements=[
        (0, 0, 4, 16, 16, 14, "dwood"), (0, 16, 3, 16, 18, 15, "lwood"), (1, 2, 3.4, 15, 14, 4, "wood"), (3, 18, 7, 5, 22, 9, "glass"), (9, 18, 7, 11, 21, 9, "water"), (0, 0, 0, 16, 3, 3, "steel")]),
    dict(id="restaurant_table", fr="Table de restaurant", en="Restaurant table", elements=[
        (0, 10, 4, 16, 12, 12, "white"), (7, 0, 7, 9, 10, 9, "steel"), (5, 0, 5, 11, 1, 11, "dgray"), (6, 12, 6, 10, 13, 10, "cream"), (7, 13, 7, 9, 17, 9, "lamp"),
        (0, 0, 13, 4, 7, 16, "wood"), (12, 0, 13, 16, 7, 16, "wood"), (0, 0, 0, 4, 7, 3, "wood"), (12, 0, 0, 16, 7, 3, "wood")]),
    dict(id="freezer_chest", fr="Congélateur coffre", en="Chest freezer", elements=[
        (0, 0, 2, 16, 11, 14, "white"), (0, 11, 2, 16, 13, 14, "glass"), (1, 6, 1.4, 15, 8, 2, "blue"), (2, 8, 4, 14, 11, 12, "mint")]),
    dict(id="shop_mannequin", fr="Mannequin de vitrine", en="Shop mannequin", elements=[
        (4, 0, 4, 12, 1, 12, "dgray"), (7, 1, 7, 9, 14, 9, "steel"), (4, 14, 5, 12, 26, 11, "fabric_b"), (5, 26, 6, 11, 30, 10, "cream"), (2, 16, 6, 4, 26, 10, "cream"), (12, 16, 6, 14, 26, 10, "cream"),
        (4, 8, 5, 12, 14, 11, "navy_d")]),
    dict(id="menu_board", fr="Ardoise de menu", en="Menu board", elements=[
        (2, 0, 6, 4, 20, 8, "dwood"), (12, 0, 6, 14, 20, 8, "dwood"), (2, 10, 5.6, 14, 28, 8.4, "slate"), (3, 24, 5.2, 11, 25, 5.6, "white"), (3, 20, 5.2, 13, 21, 5.6, "white"), (3, 16, 5.2, 9, 17, 5.6, "white"),
        (3, 13, 5.2, 12, 14, 5.6, "white")]),
    # ======================================================= RUE, JARDIN ET PARC
    dict(id="mailbox", fr="Boîte aux lettres", en="Mailbox", elements=[
        (7, 0, 7, 9, 12, 9, "dgray"), (3, 12, 4, 13, 20, 12, "blue"), (3, 18, 4, 13, 20, 12, "navy_d"), (5, 13, 3.4, 11, 14, 4, "black"), (13, 16, 6, 14, 22, 8, "red")]),
    dict(id="phone_booth", fr="Cabine téléphonique", en="Phone booth", passable=True, light=6, elements=[
        (0, 0, 0, 16, 1, 16, "dgray"), (0, 1, 0, 1, 30, 16, "red"), (15, 1, 0, 16, 30, 16, "red"), (0, 1, 15, 16, 30, 16, "red"), (1, 4, 0, 15, 26, 0.5, "glass"), (0, 30, 0, 16, 32, 16, "red"),
        (4, 12, 13, 12, 18, 15, "black"), (6, 14, 12.6, 10, 16, 13, "green")]),
    dict(id="swing", fr="Balançoire", en="Swing", elements=[
        (0, 0, 7, 2, 28, 9, "dwood"), (14, 0, 7, 16, 28, 9, "dwood"), (0, 26, 7, 16, 28, 9, "wood"), (3, 8, 7.6, 3.6, 26, 8.4, "chrome"), (6, 8, 7.6, 6.6, 26, 8.4, "chrome"), (3, 6, 6, 7, 8, 10, "red")]),
    dict(id="slide", fr="Toboggan", en="Slide", elements=[
        (0, 0, 2, 3, 24, 14, "blue"), (3, 22, 2, 8, 24, 14, "yellow"), (8, 14, 2, 11, 22, 14, "yellow"), (11, 6, 2, 14, 14, 14, "yellow"), (14, 0, 2, 16, 6, 14, "yellow"), (0, 24, 2, 8, 26, 3, "red"), (0, 24, 13, 8, 26, 14, "red")]),
    dict(id="bbq_grill", fr="Barbecue", en="BBQ grill", elements=[
        (2, 0, 2, 4, 9, 4, "dgray"), (12, 0, 2, 14, 9, 4, "dgray"), (2, 0, 12, 4, 9, 14, "dgray"), (12, 0, 12, 14, 9, 14, "dgray"), (1, 9, 1, 15, 12, 15, "black"), (2, 12, 2, 14, 13, 14, "steel"),
        (3, 11, 3, 13, 12, 13, "flame"), (1, 12, 14, 15, 17, 15, "black")]),
    dict(id="patio_umbrella", fr="Parasol", en="Patio umbrella", elements=[
        (7, 0, 7, 9, 22, 9, "steel"), (5, 0, 5, 11, 2, 11, "dgray"), (0, 22, 0, 16, 24, 16, "red"), (3, 24, 3, 13, 26, 13, "white"), (6, 26, 6, 10, 27, 10, "red")]),
    dict(id="garden_gnome", fr="Nain de jardin", en="Garden gnome", elements=[
        (5, 0, 5, 11, 7, 11, "blue"), (6, 7, 6, 10, 11, 10, "cream"), (5, 11, 5, 11, 12, 11, "cream"), (6, 12, 6, 10, 18, 10, "red"), (7, 18, 7, 9, 20, 9, "red"), (6, 8, 5.4, 10, 11, 6, "white")]),
    dict(id="statue", fr="Statue de parc", en="Park statue", elements=[
        (2, 0, 2, 14, 6, 14, "stone"), (4, 6, 4, 12, 10, 12, "stone"), (5, 10, 5, 11, 22, 11, "chrome"), (6, 22, 6, 10, 28, 10, "chrome"), (2, 14, 6, 5, 20, 10, "chrome"), (11, 14, 6, 14, 22, 10, "chrome")]),
    dict(id="picnic_table", fr="Table de pique-nique", en="Picnic table", elements=[
        (0, 10, 3, 16, 12, 13, "wood"), (0, 5, 0, 16, 7, 3, "wood"), (0, 5, 13, 16, 7, 16, "wood"), (2, 0, 3, 4, 10, 5, "dwood"), (12, 0, 3, 14, 10, 5, "dwood"), (2, 0, 11, 4, 10, 13, "dwood"),
        (12, 0, 11, 14, 10, 13, "dwood")]),
    dict(id="flower_bed", fr="Parterre de fleurs", en="Flower bed", elements=[
        (0, 0, 0, 16, 5, 16, "stone"), (1, 5, 1, 15, 6, 15, "soil"), (2, 6, 2, 5, 9, 5, "red"), (7, 6, 3, 10, 10, 6, "yellow"), (11, 6, 2, 14, 9, 5, "pink"), (3, 6, 9, 6, 10, 12, "sky"),
        (9, 6, 10, 12, 9, 13, "red"), (12, 6, 9, 14, 10, 12, "yellow"), (2, 6, 6, 14, 7, 8, "leaf")]),
    dict(id="bird_bath", fr="Bain d'oiseaux", en="Bird bath", elements=[
        (5, 0, 5, 11, 2, 11, "stone"), (7, 2, 7, 9, 11, 9, "stone"), (2, 11, 2, 14, 14, 14, "stone"), (3, 13, 3, 13, 14, 13, "water")]),
    dict(id="bike_rack", fr="Rack à vélos", en="Bike rack", elements=[
        (0, 0, 7, 2, 9, 9, "steel"), (14, 0, 7, 16, 9, 9, "steel"), (0, 8, 7, 16, 10, 9, "steel"), (5, 0, 7, 7, 9, 9, "steel"), (9, 0, 7, 11, 9, 9, "steel"), (3, 8, 4, 5, 11, 12, "red")]),
    dict(id="newspaper_stand", fr="Kiosque à journaux", en="Newspaper stand", elements=[
        (1, 0, 2, 15, 12, 14, "yellow"), (1, 12, 2, 15, 14, 14, "dgray"), (2, 3, 1.4, 14, 11, 2, "glass"), (3, 5, 1.2, 8, 10, 1.6, "white"), (9, 5, 1.2, 13, 10, 1.6, "sky"), (3, 14, 4, 13, 22, 12, "cream")]),
    dict(id="atm_machine", fr="Distributeur de billets", en="ATM", anim="atm", light=6, elements=[
        (1, 0, 3, 15, 30, 15, "dgray"), (3, 18, 2.4, 13, 26, 3, "black", {"north": "anim"}), (3, 9, 2.4, 13, 14, 3, "steel"), (6, 5, 2.2, 10, 7, 3, "black"), (3, 2, 2.4, 13, 4, 3, "yellow")]),
    # ======================================================= BUREAU ET ÉCOLE
    dict(id="office_desk", fr="Bureau", en="Office desk", elements=[
        (0, 12, 2, 16, 14, 14, "lwood"), (0, 0, 2, 3, 12, 14, "wood"), (13, 0, 2, 16, 12, 14, "wood"), (3, 6, 3, 13, 11, 13, "wood"), (4, 7, 2.4, 12, 10, 3, "lwood"), (6, 14, 8, 10, 15, 12, "cream"),
        (11, 14, 4, 13, 16, 6, "red")]),
    dict(id="computer_monitor", fr="Ordinateur", en="Computer", anim="desktop", light=5, elements=[
        (4, 0, 4, 12, 1, 12, "dgray"), (6, 1, 7, 10, 4, 9, "dgray"), (1, 4, 6, 15, 14, 9, "black"), (2, 5, 5.4, 14, 13, 6, "black", {"north": "anim"}), (3, 0, 0, 13, 1, 3, "cream")]),
    dict(id="printer", fr="Imprimante", en="Printer", elements=[
        (1, 0, 3, 15, 7, 14, "cream"), (2, 7, 4, 14, 9, 13, "dgray"), (3, 9, 8, 13, 11, 9, "white"), (3, 1, 2.4, 13, 3, 3, "black"), (11, 4, 2.4, 13, 5, 3, "green")]),
    dict(id="copier", fr="Photocopieuse", en="Photocopier", elements=[
        (0, 0, 2, 16, 14, 16, "offwhite"), (0, 14, 2, 16, 17, 16, "dgray"), (1, 17, 3, 15, 18, 14, "glass"), (2, 6, 1.4, 14, 11, 2, "dgray"), (11, 12, 1.4, 14, 13, 2, "green"), (2, 12, 1.4, 8, 13, 2, "black")]),
    dict(id="filing_cabinet", fr="Classeur métallique", en="Filing cabinet", elements=[
        (3, 0, 3, 13, 26, 15, "gray"), (4, 19, 2.4, 12, 25, 3, "steel"), (4, 12, 2.4, 12, 18, 3, "steel"), (4, 5, 2.4, 12, 11, 3, "steel"), (7, 21, 2, 9, 23, 2.4, "dgray"),
        (7, 14, 2, 9, 16, 2.4, "dgray"), (7, 7, 2, 9, 9, 2.4, "dgray")]),
    dict(id="office_chair", fr="Chaise de bureau", en="Office chair", elements=[
        (7, 0, 7, 9, 8, 9, "steel"), (2, 0, 2, 14, 2, 4, "dgray"), (2, 0, 12, 14, 2, 14, "dgray"), (2, 0, 2, 4, 2, 14, "dgray"), (12, 0, 2, 14, 2, 14, "dgray"),
        (3, 8, 3, 13, 10, 13, "navy_d"), (3, 10, 12, 13, 22, 14, "navy_d"), (1, 12, 4, 3, 13, 12, "dgray"), (13, 12, 4, 15, 13, 12, "dgray")]),
    dict(id="blackboard", fr="Tableau noir", en="Blackboard", passable=True, elements=[
        (0, 3, 14.5, 16, 15, 16, "dwood"), (1, 4, 14, 15, 14, 14.5, "slate"), (3, 10, 13.8, 10, 10.6, 14.1, "white"), (3, 7, 13.8, 12, 7.6, 14.1, "white"), (1, 3, 13.5, 15, 4, 14.5, "dwood"), (11, 4, 13.4, 14, 5, 14, "white")]),
    dict(id="school_desk", fr="Pupitre", en="School desk", elements=[
        (1, 11, 3, 15, 13, 13, "lwood"), (2, 0, 4, 4, 11, 6, "steel"), (12, 0, 4, 14, 11, 6, "steel"), (2, 0, 10, 4, 6, 12, "steel"), (12, 0, 10, 14, 6, 12, "steel"), (3, 13, 4, 8, 14, 9, "book_b"),
        (2, 6, 10, 14, 8, 14, "wood")]),
    dict(id="school_locker", fr="Casier scolaire", en="School locker", elements=[
        (1, 0, 2, 15, 30, 15, "blue"), (7.4, 0, 1.4, 8.6, 30, 2, "navy_d"), (2, 18, 1.4, 7, 28, 2, "navy_d"), (9, 18, 1.4, 14, 28, 2, "navy_d"), (2, 2, 1.4, 7, 12, 2, "navy_d"), (9, 2, 1.4, 14, 12, 2, "navy_d"),
        (6, 21, 1, 7, 24, 1.6, "chrome"), (9, 21, 1, 10, 24, 1.6, "chrome")]),
    dict(id="projector", fr="Vidéoprojecteur", en="Projector", elements=[
        (7, 8, 7, 9, 16, 9, "steel"), (3, 6, 3, 13, 9, 13, "dgray"), (4, 3, 3, 8, 6, 7, "black"), (5, 4, 2.6, 7, 5, 3, "lamp"), (10, 6.5, 12, 12, 8.5, 13, "red")]),
    dict(id="globe", fr="Globe terrestre", en="Globe", elements=[
        (5, 0, 5, 11, 1, 11, "dwood"), (7, 1, 7, 9, 4, 9, "dwood"), (4, 4, 4, 12, 12, 12, "sky"), (5, 6, 5, 8, 10, 7, "leaf"), (9, 5, 8, 11, 9, 11, "leaf"), (7, 12, 7, 9, 13, 9, "steel")]),
    dict(id="whiteboard", fr="Tableau blanc", en="Whiteboard", passable=True, elements=[
        (0, 3, 14.5, 16, 15, 16, "steel"), (1, 4, 14, 15, 14, 14.5, "white"), (3, 10, 13.8, 9, 10.6, 14.1, "blue"), (3, 7, 13.8, 12, 7.6, 14.1, "red"), (11, 4, 13.4, 14, 5, 14, "black")]),
    # ======================================================= INDUSTRIE ET CHANTIER
    dict(id="wooden_pallet", fr="Palette de bois", en="Wooden pallet", elements=[
        (0, 0, 0, 16, 2, 3, "lwood"), (0, 0, 6.5, 16, 2, 9.5, "lwood"), (0, 0, 13, 16, 2, 16, "lwood"), (0, 2, 0, 16, 3, 16, "wood"), (1, 3, 1, 15, 4, 15, "lwood")]),
    dict(id="crate_stack", fr="Pile de caisses", en="Crate stack", elements=[
        (0, 0, 0, 10, 8, 10, "cardboard"), (6, 0, 6, 16, 8, 16, "wood"), (3, 8, 3, 13, 15, 13, "cardboard"), (0, 3.5, 0, 10, 4.5, 10, "dwood"), (6, 3.5, 6, 16, 4.5, 16, "dwood"), (3, 11, 3, 13, 12, 13, "dwood")]),
    dict(id="oil_barrels", fr="Fûts d'huile", en="Oil barrels", elements=[
        (0, 0, 1, 7, 12, 8, "blue"), (8, 0, 2, 15, 12, 9, "red"), (3, 0, 9, 10, 12, 16, "olive"), (0, 3, 1, 7, 4, 8, "dgray"), (8, 3, 2, 15, 4, 9, "dgray"), (3, 8, 9, 10, 9, 16, "dgray"),
        (0, 12, 1, 7, 13, 8, "steel"), (8, 12, 2, 15, 13, 9, "steel"), (3, 12, 9, 10, 13, 16, "steel")]),
    dict(id="industrial_workbench", fr="Établi d'atelier", en="Workshop bench", elements=[
        (0, 12, 3, 16, 15, 15, "wood"), (0, 0, 3, 2, 12, 15, "dgray"), (14, 0, 3, 16, 12, 15, "dgray"), (2, 5, 4, 14, 6, 14, "dgray"), (3, 15, 6, 8, 17, 11, "steel"), (10, 15, 5, 12, 16, 12, "red"),
        (0, 20, 14, 16, 26, 15, "cardboard"), (2, 21, 13.6, 4, 24, 14, "chrome"), (7, 21, 13.6, 9, 24, 14, "chrome")]),
    dict(id="control_panel", fr="Pupitre de commande", en="Control panel", anim="blink", light=6, elements=[
        (1, 0, 3, 15, 10, 15, "dgray"), (1, 10, 3, 15, 15, 9, "black"), (2, 10.5, 3.6, 14, 14.5, 8.6, "black", {"north": "anim"}), (3, 11, 9, 5, 12, 12, "red"), (7, 11, 9, 9, 12, 12, "green"),
        (11, 11, 9, 13, 12, 12, "yellow")]),
    dict(id="toolbox", fr="Caisse à outils", en="Toolbox", elements=[
        (2, 0, 4, 14, 7, 12, "red"), (2, 7, 4, 14, 8, 12, "dgray"), (6, 8, 7, 10, 10, 9, "steel"), (2, 3, 3.4, 14, 4, 4, "dgray"), (3, 8, 5, 7, 9, 8, "chrome"), (10, 8, 8, 13, 9, 11, "yellow")]),
    dict(id="construction_barrier", fr="Barrière de chantier", en="Construction barrier", elements=[
        (0, 0, 6, 2, 14, 10, "dgray"), (14, 0, 6, 16, 14, 10, "dgray"), (0, 14, 6, 16, 18, 10, "orange"), (0, 8, 6, 16, 12, 10, "white"), (3, 14, 5.6, 6, 18, 6, "white"), (10, 14, 5.6, 13, 18, 6, "white"),
        (6, 18, 7, 10, 20, 9, "yellow")]),
    dict(id="cement_mixer", fr="Bétonnière", en="Cement mixer", elements=[
        (2, 0, 4, 6, 8, 6, "dgray"), (10, 0, 4, 14, 8, 6, "dgray"), (2, 0, 10, 14, 4, 12, "dgray"), (2, 8, 3, 14, 20, 13, "orange"), (4, 20, 5, 12, 24, 11, "orange"), (6, 24, 6, 10, 26, 10, "dgray"),
        (5, 4, 13, 11, 8, 15, "stone")]),
    dict(id="conveyor_belt", fr="Tapis roulant industriel", en="Industrial conveyor", anim="belt", elements=[
        (0, 0, 2, 16, 8, 4, "dgray"), (0, 0, 12, 16, 8, 14, "dgray"), (0, 8, 3, 16, 10, 13, "belt", {"up": "anim"}), (2, 0, 5, 4, 6, 11, "steel"), (12, 0, 5, 14, 6, 11, "steel"), (0, 10, 3, 16, 12, 4, "yellow")]),
    dict(id="pipe_run", fr="Tuyauterie", en="Pipe run", elements=[
        (0, 6, 6, 16, 10, 10, "steel"), (0, 5, 5, 3, 11, 11, "dgray"), (13, 5, 5, 16, 11, 11, "dgray"), (6, 10, 7, 10, 18, 9, "steel"), (5, 18, 6, 11, 20, 10, "red"), (7, 20, 7.4, 9, 22, 8.6, "dgray"),
        (0, 0, 6, 2, 6, 10, "dgray"), (14, 0, 6, 16, 6, 10, "dgray")]),
    dict(id="storage_tank", fr="Cuve de stockage", en="Storage tank", elements=[
        (1, 0, 1, 15, 3, 15, "dgray"), (2, 3, 2, 14, 24, 14, "tank_gray" if False else "gray"), (3, 24, 3, 13, 27, 13, "gray"), (6, 27, 6, 10, 30, 10, "dgray"), (1.4, 8, 6, 2, 20, 10, "steel"), (7, 3, 1.4, 9, 8, 2, "hazard")]),
    dict(id="forklift", fr="Chariot élévateur", en="Forklift", elements=[
        (3, 2, 2, 13, 5, 13, "yellow"), (3, 5, 5, 13, 11, 13, "yellow"), (3, 11, 5, 13, 13, 13, "black"), (4, 13, 5, 12, 20, 6, "yellow"), (4, 13, 12, 12, 20, 13, "yellow"), (4, 20, 5, 12, 21, 13, "yellow"),
        (6, 2, 0, 7, 16, 3, "steel"), (9, 2, 0, 10, 16, 3, "steel"), (3, 0, 3, 6, 4, 6, "wheel"), (10, 0, 3, 13, 4, 6, "wheel"), (3, 0, 10, 6, 4, 13, "wheel"), (10, 0, 10, 13, 4, 13, "wheel")]),
    # ======================================================= BASES MILITAIRES ET PRISON
    dict(id="ammo_crate_stack", fr="Pile de caisses de munitions", en="Ammo crate stack", elements=[
        (0, 0, 0, 10, 6, 7, "olive"), (6, 0, 7, 16, 6, 14, "olive"), (2, 6, 2, 12, 12, 9, "khaki"), (0, 2, 0, 10, 3, 7, "dgray"), (6, 2, 7, 16, 3, 14, "dgray"), (2, 8, 2, 12, 9, 9, "dgray"),
        (3, 12, 3, 5, 13, 5, "yellow")]),
    dict(id="radio_set", fr="Poste radio militaire", en="Military radio set", anim="blink", light=4, elements=[
        (2, 0, 4, 14, 10, 12, "olive"), (3, 4, 3.4, 13, 9, 4, "black", {"north": "anim"}), (10, 10, 7, 11, 22, 8, "steel"), (3, 10, 6, 7, 12, 9, "dgray"), (4, 1, 3.4, 8, 3, 4, "dgray"), (9, 1, 3.4, 13, 3, 4, "khaki")]),
    dict(id="field_table", fr="Table d'état-major", en="Staff table", elements=[
        (0, 12, 1, 16, 14, 15, "khaki"), (1, 0, 2, 3, 12, 4, "dgray"), (13, 0, 2, 15, 12, 4, "dgray"), (1, 0, 12, 3, 12, 14, "dgray"), (13, 0, 12, 15, 12, 14, "dgray"), (2, 14, 3, 14, 14.4, 13, "cream"),
        (4, 14.4, 5, 7, 15, 8, "red"), (9, 14.4, 8, 12, 15, 11, "blue")]),
    dict(id="army_cot", fr="Lit de camp", en="Army cot", elements=[
        (0, 5, 0, 16, 7, 16, "olive"), (0, 0, 0, 2, 5, 2, "steel"), (14, 0, 0, 16, 5, 2, "steel"), (0, 0, 14, 2, 5, 16, "steel"), (14, 0, 14, 16, 5, 16, "steel"), (2, 7, 1, 14, 8, 15, "khaki"),
        (3, 8, 1, 13, 9, 5, "cream")]),
    dict(id="floodlight", fr="Projecteur de chantier", en="Floodlight", light=15, elements=[
        (3, 0, 3, 13, 2, 13, "dgray"), (7, 2, 7, 9, 22, 9, "steel"), (2, 22, 5, 14, 30, 11, "dgray"), (3, 23, 4.4, 13, 29, 5, "lamp"), (14, 26, 7, 16, 28, 9, "steel")]),
    dict(id="sandbag_corner", fr="Angle de sacs de sable", en="Sandbag corner", elements=[
        (0, 0, 0, 16, 5, 6, "khaki"), (0, 0, 6, 6, 5, 16, "khaki"), (0, 5, 0, 16, 10, 5, "tan"), (0, 5, 5, 5, 10, 16, "tan"), (2, 10, 0, 14, 14, 4, "khaki"), (0, 10, 4, 4, 14, 14, "khaki")]),
    dict(id="weapon_locker", fr="Armoire à armes", en="Weapon locker", elements=[
        (1, 0, 3, 15, 32, 15, "dgray"), (2, 2, 2.4, 14, 30, 3, "olive"), (6.6, 2, 2, 8.4, 30, 2.4, "black"), (6, 14, 1.6, 7, 17, 2, "steel"), (9, 14, 1.6, 10, 17, 2, "steel"), (3, 24, 2, 13, 26, 2.4, "yellow")]),
    dict(id="guard_desk", fr="Bureau de garde", en="Guard desk", elements=[
        (0, 0, 2, 16, 12, 14, "dgray"), (0, 12, 1, 16, 14, 15, "olive"), (3, 14, 5, 9, 15, 11, "cream"), (10, 14, 4, 13, 18, 7, "black"), (11, 18, 5, 12, 19, 6, "green"), (1, 2, 1.4, 15, 10, 2, "olive")]),
    dict(id="razor_wire", fr="Fil de fer barbelé", en="Razor wire", passable=True, elements=[
        (0, 0, 6, 2, 12, 8, "dgray"), (14, 0, 6, 16, 12, 8, "dgray"), (0, 11, 7, 16, 12, 9, "steel"), (0, 7, 7, 16, 8, 9, "steel"), (0, 3, 7, 16, 4, 9, "steel"), (3, 10, 6, 5, 14, 10, "steel"),
        (7, 10, 6, 9, 14, 10, "steel"), (11, 10, 6, 13, 14, 10, "steel")]),
    dict(id="interrogation_table", fr="Table d'interrogatoire", en="Interrogation table", elements=[
        (0, 11, 3, 16, 13, 13, "gray"), (1, 0, 4, 3, 11, 6, "dgray"), (13, 0, 4, 15, 11, 6, "dgray"), (1, 0, 10, 3, 11, 12, "dgray"), (13, 0, 10, 15, 11, 12, "dgray"), (6, 13, 6, 10, 14, 10, "lamp"),
        (2, 13, 7, 4, 14, 9, "steel"), (12, 13, 7, 14, 14, 9, "steel")]),
    dict(id="map_board", fr="Panneau de cartes", en="Map board", passable=True, elements=[
        (0, 2, 14.5, 16, 15, 16, "dwood"), (1, 3, 14, 15, 14, 14.5, "cream"), (2, 4, 13.8, 7, 8, 14.2, "leaf"), (8, 8, 13.8, 14, 12, 14.2, "sky"), (4, 9, 13.6, 6, 11, 14, "red"), (10, 5, 13.6, 12, 7, 14, "red"),
        (6, 6, 13.7, 11, 6.4, 14, "black")]),
]

"""
Cinquième série de décors : 96 objets détaillés pour les bases et villes américaines (colonel et général, briefing, patriotisme, communications,
cafétéria et PX, dortoir, salle de sport, loisirs, armurerie, hangar, opérations spéciales et missiles, défense, signalétique, camps, Main Street,
fêtes). Importé par generate_realism_decor.py.
"""
import math
import random

from PIL import Image

from realism_decor_4 import legs, table

PAL5 = {
    "us_olive": "#5a6240", "star_white": "#f4f4f6", "denim": "#3a5a8a", "pine": "#2a5a3a", "hay": "#d8b858", "bun": "#d8a050", "patty": "#5a3020",
    "cheese": "#f0c030", "ketchup": "#c82820", "asphalt": "#303236", "camo_sand": "#b09a68", "camo_g": "#4a5a30", "padded": "#6a7080", "mustard": "#d8b020",
    "brass": "#c8a040", "wine": "#5a1a2a",
}


def _f(n):
    return Image.new("RGBA", (16, 16 * n))


def anim_rack():
    """Rack de serveurs : voyants verts et ambre qui clignotent en rangées."""
    img = _f(8)
    rnd = random.Random(31)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (24, 26, 30, 255))
        for row in range(5):
            for x in range(2, 14):
                if x % 2 == 0:
                    img.putpixel((x, 2 + row * 3 + 16 * f), (70, 74, 82, 255))
            for i in range(3):
                col = (60, 255, 110) if rnd.random() < 0.6 else (255, 190, 50) if rnd.random() < 0.5 else (40, 50, 40)
                img.putpixel((11 + i, 2 + row * 3 + 16 * f), col + (255,))
    return img, {"frametime": 3}


def anim_barber():
    """Enseigne de barbier : spirale rouge, blanche et bleue qui monte."""
    img = _f(8)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                t = (x + y * 1 + f * 2) % 8
                c = (200, 40, 50) if t < 3 else (240, 240, 244) if t < 5 else (50, 80, 170) if t < 7 else (240, 240, 244)
                img.putpixel((x, y + 16 * f), c + (255,))
    return img, {"frametime": 3}


def anim_cinema():
    """Fronton de cinéma : ampoules chenillard autour d'un titre."""
    img = _f(6)
    for f in range(6):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (18, 12, 24, 255))
        for i in range(16):
            on = (i + f) % 3 == 0
            col = (255, 230, 120) if on else (90, 70, 40)
            for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
                img.putpixel((x, y + 16 * f), col + (255,))
        for x in range(3, 13):
            if x % 3:
                img.putpixel((x, 6 + 16 * f), (255, 80, 90, 255))
                img.putpixel((x, 7 + 16 * f), (255, 80, 90, 255))
                img.putpixel((x, 10 + 16 * f), (255, 255, 255, 255))
    return img, {"frametime": 4}


def anim_fireworks():
    """Feu d'artifice : gerbes colorées qui s'épanouissent."""
    img = _f(8)
    rnd = random.Random(77)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (10, 12, 32, 255))
        cx, cy, col = (5, 6, (255, 90, 90)) if f < 5 else (11, 9, (90, 200, 255))
        r = (f % 5) * 1.3 + 0.5
        for a in range(0, 360, 30):
            x = int(cx + r * math.cos(math.radians(a)))
            y = int(cy + r * math.sin(math.radians(a)))
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y + 16 * f), col + (255,))
        for _ in range(4):
            img.putpixel((rnd.randint(0, 15), rnd.randint(8, 15) + 16 * f), (255, 240, 160, 255))
    return img, {"frametime": 3}


ANIMS5 = {"rack": anim_rack, "barber": anim_barber, "cinema": anim_cinema, "fireworks": anim_fireworks}

DECOS5 = [
    # ================================================== BUREAU DU COLONEL / GÉNÉRAL
    dict(id="colonel_desk", fr="Bureau du colonel", en="Colonel's desk", light=5, elements=table(13, "wood", "dwood", 0, 16, 1, 15) + [
        (0, 13, 1, 16, 14, 15, "wine"), (2, 14, 4, 14, 14.4, 12, "dgreen"), (2, 14.4, 11, 6, 15.4, 12.6, "brass"), (3, 14.4, 6, 7, 15, 10, "paper"), (10, 14.4, 4, 13, 18, 7, "bakelite"), (11, 18, 4.6, 12, 19, 5.4, "chrome"),
        (8, 14.4, 11, 9, 20, 12, "usa_blue"), (9, 18, 11, 12, 20, 11.4, "usa_red"), (5, 3, 1, 11, 11, 2, "dwood"), (6, 4, 0.6, 10, 5, 1, "brass"), (6, 7, 0.6, 10, 8, 1, "brass")]),
    dict(id="president_portrait", fr="Portrait du président", en="President portrait", passable=True, elements=[
        (2, 1, 15, 14, 16, 16, "brass"), (3, 2, 14.6, 13, 15, 15, "dwood"), (4, 3, 14.2, 12, 14, 14.6, "usa_blue"), (6, 10, 14, 10, 13, 14.2, "tan"), (5, 5, 14, 11, 10, 14.2, "carbon"), (7, 4, 13.8, 9, 8, 14, "star_white"),
        (7.2, 7, 13.6, 7.8, 10, 14, "usa_red"), (5.4, 11, 13.8, 6.6, 12, 14, "carbon"), (9.4, 11, 13.8, 10.6, 12, 14, "carbon")]),
    dict(id="desk_flag_set", fr="Drapeaux de bureau", en="Desk flag set", elements=[
        (1, 0, 4, 15, 2, 12, "wine"), (3, 2, 6, 5, 20, 8, "brass"), (4, 14, 6.4, 11, 20, 7.6, "usa_blue"), (4, 16, 6.2, 11, 17, 7.8, "star_white"), (4, 18, 6.2, 11, 19, 7.8, "usa_red"), (11, 2, 6, 13, 18, 8, "brass"),
        (11, 12, 6.4, 15, 18, 7.6, "gold"), (11, 14, 6.2, 15, 15, 7.8, "usa_red"), (3, 20, 6, 5, 22, 8, "gold"), (11, 18, 6, 13, 20, 8, "gold")]),
    dict(id="standing_globe", fr="Globe terrestre sur pied", en="Standing globe", elements=[
        (3, 0, 3, 13, 2, 13, "dwood"), (7, 2, 7, 9, 12, 9, "dwood"), (2, 12, 2, 14, 24, 14, "water"), (3, 13, 3, 7, 19, 8, "leaf"), (8, 17, 6, 13, 22, 11, "leaf"), (4, 20, 9, 8, 23, 12, "leaf"), (2, 18, 2, 14, 19, 14, "brass"),
        (7, 24, 7, 9, 26, 9, "brass"), (1, 16, 7, 3, 22, 9, "brass"), (13, 16, 7, 15, 22, 9, "brass")]),
    dict(id="red_hotline_phone", fr="Téléphone rouge", en="Red hotline phone", light=3, elements=[
        (2, 0, 4, 14, 3, 12, "carbon"), (3, 3, 5, 13, 6, 11, "ketchup"), (4, 6, 6, 12, 8, 10, "ketchup"), (2, 7, 6, 4, 10, 10, "ketchup"), (12, 7, 6, 14, 10, 10, "ketchup"), (3, 10, 6.4, 5, 11, 9.6, "ketchup"), (11, 10, 6.4, 13, 11, 9.6, "ketchup"),
        (6, 5, 3.6, 10, 6, 4, "bone"), (7, 5.4, 3.4, 8, 6.4, 3.8, "carbon"), (13, 2, 6, 15, 7, 10, "rope"), (14, 0, 7, 16, 2, 9, "rope"), (5, 8, 9, 11, 9, 11, "star_white")]),
    dict(id="nuclear_football", fr="Valise nucléaire", en="Nuclear football", elements=[
        (1, 0, 4, 15, 10, 12, "carbon"), (1, 10, 4, 15, 11, 12, "dgray"), (6, 11, 6, 10, 13, 10, "carbon"), (2, 3, 3.4, 14, 4, 4, "steel"), (3, 5, 3.4, 5, 8, 4, "gold"), (11, 5, 3.4, 13, 8, 4, "gold"), (6, 5, 3.4, 10, 8, 4, "usa_red"),
        (1, 6, 12, 15, 8, 12.6, "steel"), (13, 10, 6, 15, 12, 10, "steel"), (2, 1, 3.2, 4, 3, 3.6, "chrome"), (12, 1, 3.2, 14, 3, 3.6, "chrome")]),
    # ================================================== BRIEFING ET ADMINISTRATION
    dict(id="briefing_podium", fr="Podium de briefing", en="Briefing podium", elements=[
        (2, 0, 3, 14, 14, 13, "dwood"), (1, 14, 2, 15, 16, 14, "wood"), (2, 16, 6, 14, 22, 12, "dwood"), (3, 22, 6, 13, 24, 8, "carbon"), (6, 24, 6, 10, 28, 7, "steel"), (7, 28, 6.4, 9, 29, 7, "carbon"), (3, 2, 2.4, 13, 12, 3, "usa_blue"),
        (5, 5, 2, 11, 9, 2.4, "star_white"), (6, 6, 1.8, 10, 8, 2.4, "usa_blue")]),
    dict(id="folding_chair_row", fr="Rangée de chaises pliantes", en="Folding chair row", elements=[
        (0, 0, 2, 16, 9, 7, "padded"), (0, 0, 9, 16, 9, 14, "padded"), (1, 9, 3, 3, 10, 6, "padded"), (5, 9, 3, 7, 10, 6, "padded"), (9, 9, 3, 11, 10, 6, "padded"), (13, 9, 3, 15, 10, 6, "padded"), (0, 9, 6, 16, 18, 7, "carbon"),
        (0, 9, 13, 16, 18, 14, "carbon"), (0, 0, 2, 16, 8, 3, "dgray")]),
    dict(id="projector_screen", fr="Écran de projection", en="Projection screen", passable=True, elements=[
        (1, 26, 12, 15, 28, 16, "carbon"), (2, 4, 14.6, 14, 26, 15.4, "star_white"), (1, 3, 14.2, 15, 4, 15.8, "carbon"), (1, 26, 14.2, 15, 27, 15.8, "carbon"), (0, 3, 14.2, 1, 27, 15.8, "carbon"), (15, 3, 14.2, 16, 27, 15.8, "carbon"),
        (6, 16, 14.2, 10, 24, 14.6, "denim"), (5, 6, 14.2, 11, 10, 14.6, "tan")]),
    dict(id="coffee_urn", fr="Percolateur à café", en="Coffee urn", elements=[
        (3, 0, 3, 13, 3, 13, "dgray"), (4, 3, 4, 12, 18, 12, "chrome"), (5, 18, 5, 11, 20, 11, "steel"), (6, 20, 6, 10, 22, 10, "carbon"), (13, 6, 6, 16, 8, 10, "chrome"), (14, 3, 7, 15, 6, 9, "carbon"), (4, 8, 3.4, 12, 14, 4, "glass"),
        (5, 9, 3.2, 11, 13, 3.6, "bakelite"), (6, 3, 2.6, 10, 5, 3.4, "chrome"), (7, 15, 3.4, 9, 17, 4, "usa_red")]),
    dict(id="admin_inbox_tray", fr="Corbeilles de courrier", en="Admin inbox trays", elements=[
        (0, 0, 2, 16, 2, 14, "dwood"), (1, 2, 3, 15, 4, 13, "steel"), (2, 4, 4, 14, 6, 12, "paper"), (1, 6, 3, 15, 8, 13, "steel"), (2, 8, 4, 14, 10, 12, "paper"), (1, 10, 3, 15, 12, 13, "steel"), (3, 12, 4, 7, 14, 12, "ketchup"),
        (9, 12, 5, 14, 13, 11, "paper"), (1, 2, 3, 3, 12, 5, "dgray"), (13, 2, 3, 15, 12, 5, "dgray")]),
    dict(id="wall_calendar_board", fr="Tableau calendrier", en="Wall calendar board", passable=True, elements=[
        (1, 1, 14.4, 15, 16, 16, "dwood"), (2, 2, 14, 14, 15, 14.4, "star_white"), (2, 12, 13.8, 14, 15, 14, "usa_red"), (3, 3, 13.8, 5, 5, 14, "carbon"), (6, 3, 13.8, 8, 5, 14, "carbon"), (9, 3, 13.8, 11, 5, 14, "carbon"), (12, 3, 13.8, 14, 5, 14, "carbon"),
        (3, 6, 13.8, 5, 8, 14, "carbon"), (6, 6, 13.8, 8, 8, 14, "ketchup"), (9, 6, 13.8, 11, 8, 14, "carbon"), (12, 6, 13.8, 14, 8, 14, "carbon"), (3, 9, 13.8, 5, 11, 14, "carbon"), (6, 9, 13.8, 8, 11, 14, "carbon")]),
    # ================================================== PATRIOTISME ET MÉMOIRE
    dict(id="golden_eagle", fr="Aigle doré", en="Golden eagle", elements=[
        (3, 0, 3, 13, 3, 13, "dwood"), (5, 3, 5, 11, 6, 11, "brass"), (6, 6, 6, 10, 14, 10, "gold"), (1, 10, 6.4, 6, 14, 9.6, "gold"), (10, 10, 6.4, 15, 14, 9.6, "gold"), (0, 13, 6.4, 3, 18, 9.6, "gold"), (13, 13, 6.4, 16, 18, 9.6, "gold"),
        (6, 14, 6, 10, 18, 10, "gold"), (7, 18, 6.4, 9, 20, 9.6, "gold"), (8.6, 17, 5.4, 9.6, 18, 6.4, "brass"), (6.6, 18, 5.6, 7.6, 18.6, 6.4, "carbon")]),
    dict(id="folded_flag_case", fr="Drapeau plié sous vitrine", en="Folded flag case", elements=[
        (1, 0, 4, 15, 4, 12, "dwood"), (1, 4, 4, 15, 12, 12, "glass"), (4, 4, 5, 12, 10, 11, "usa_blue"), (4, 4, 5, 12, 5, 11, "usa_red"), (4, 6, 5, 12, 7, 11, "star_white"), (4, 8, 5, 12, 9, 11, "usa_red"), (5, 10, 5.4, 8, 11, 10.6, "star_white"),
        (1, 12, 4, 15, 13, 12, "dwood"), (6, 1, 3.6, 10, 3, 4, "brass")]),
    dict(id="photo_wall", fr="Mur de photos", en="Photo wall", passable=True, elements=[
        (0, 1, 15, 16, 16, 16, "dwood"), (1, 9, 14.4, 6, 15, 15, "star_white"), (2, 10, 14, 5, 14, 14.4, "denim"), (7, 10, 14.4, 11, 15, 15, "star_white"), (8, 11, 14, 10, 14, 14.4, "camo_g"), (12, 9, 14.4, 15, 14, 15, "star_white"),
        (13, 10, 14, 14, 13, 14.4, "tan"), (2, 2, 14.4, 7, 8, 15, "star_white"), (3, 3, 14, 6, 7, 14.4, "pine"), (9, 2, 14.4, 14, 7, 15, "star_white"), (10, 3, 14, 13, 6, 14.4, "sky")]),
    dict(id="purple_heart_case", fr="Vitrine de la Purple Heart", en="Purple Heart display", light=4, elements=[
        (2, 0, 4, 14, 10, 12, "dwood"), (2, 10, 4, 14, 18, 12, "glass"), (3, 11, 5, 13, 12, 11, "usa_blue"), (5, 12, 6, 11, 13, 10, "purple"), (6, 13, 6.4, 10, 15, 9.6, "gold"), (7, 15, 6.4, 9, 17, 9.6, "purple"), (7, 11, 5.4, 9, 12, 10, "usa_red"),
        (2, 18, 4, 14, 19, 12, "dwood"), (6, 3, 3.6, 10, 6, 4, "brass")]),
    dict(id="memorial_plaque", fr="Plaque commémorative", en="Memorial plaque", passable=True, elements=[
        (1, 2, 14.4, 15, 14, 16, "stone"), (2, 3, 14, 14, 13, 14.4, "bronze"), (3, 11, 13.8, 13, 12, 14, "gold"), (3, 9, 13.8, 13, 10, 14, "brass"), (3, 7, 13.8, 11, 8, 14, "brass"), (3, 5, 13.8, 12, 6, 14, "brass"), (7, 3.4, 13.6, 9, 4.6, 14, "usa_red"),
        (0, 7, 15, 1, 9, 16, "gold"), (15, 7, 15, 16, 9, 16, "gold")]),
    dict(id="president_bust", fr="Buste de président", en="President bust", elements=[
        (2, 0, 2, 14, 8, 14, "stone"), (1, 8, 1, 15, 10, 15, "ash"), (4, 10, 4, 12, 17, 12, "star_white"), (5, 17, 5, 11, 24, 11, "star_white"), (6, 24, 6, 10, 26, 10, "star_white"), (5, 20, 4.4, 11, 21, 5, "ash"), (6, 21, 4.4, 7, 22, 5, "carbon"),
        (9, 21, 4.4, 10, 22, 5, "carbon"), (5, 1, 1.4, 11, 7, 2, "gold"), (6, 3, 1.2, 10, 4, 1.6, "carbon")]),
    # ================================================== COMMUNICATIONS ET CYBER
    dict(id="server_rack", fr="Rack de serveurs", en="Server rack", anim="rack", light=5, elements=[
        (1, 0, 3, 15, 32, 15, "carbon"), (2, 2, 2.4, 14, 30, 3, "carbon", {"north": "anim"}), (0, 0, 2, 2, 32, 4, "dgray"), (14, 0, 2, 16, 32, 4, "dgray"), (2, 31, 3, 14, 32, 15, "dgray"), (5, 1, 2, 11, 2, 2.4, "steel"),
        (13, 8, 14.4, 15, 20, 16, "carbon"), (1, 8, 14.4, 3, 20, 16, "carbon")]),
    dict(id="tactical_screen", fr="Écran tactique", en="Tactical display", anim="radar", light=8, elements=[
        (1, 0, 4, 15, 3, 12, "dgray"), (6, 3, 6, 10, 10, 10, "carbon"), (0, 10, 5, 16, 26, 9, "carbon"), (1, 11, 4.4, 15, 25, 5, "carbon", {"north": "anim"}), (0, 10, 4, 16, 11, 5, "steel"), (0, 25, 4, 16, 26, 5, "steel"),
        (7, 26, 6, 9, 28, 8, "dgray"), (3, 0.5, 3.4, 13, 1.5, 4, "usa_blue")]),
    dict(id="satellite_phone", fr="Téléphone satellite", en="Satellite phone", elements=[
        (3, 0, 4, 13, 3, 12, "us_olive"), (4, 3, 5, 12, 7, 11, "carbon"), (5, 7, 6, 11, 8, 10, "carbon"), (6, 8, 7, 10, 10, 9, "dgray"), (12, 7, 6, 14, 20, 8, "carbon"), (11, 18, 6, 15, 20, 8, "carbon"), (4, 4, 4.4, 12, 6, 5, "carbon"),
        (5, 5, 4.2, 7, 6, 4.6, "lamp"), (9, 5, 4.2, 11, 6, 4.6, "usa_red"), (1, 2, 6, 3, 8, 10, "us_olive")]),
    dict(id="radio_console_us", fr="Console radio", en="Radio console", anim="blink", light=5, elements=[
        (0, 0, 3, 16, 10, 15, "us_olive"), (0, 10, 3, 16, 13, 9, "dgray"), (0, 13, 9, 16, 20, 11, "carbon"), (1, 13.5, 8.4, 15, 19.5, 9, "carbon", {"north": "anim"}), (2, 10.6, 4, 6, 11.4, 8, "ketchup"), (7, 10.6, 4, 10, 11.4, 8, "mustard"),
        (11, 10.6, 4, 14, 11.4, 8, "pine"), (3, 11.4, 5, 4, 13, 6, "steel"), (12, 11.4, 5, 13, 13, 6, "steel"), (0, 0, 3, 16, 2, 15, "dgray")]),
    dict(id="headset_mic_desk", fr="Casque et micro de poste", en="Headset and mic desk", elements=table(11, "dgray", "carbon", 1, 15, 2, 14) + [
        (3, 11, 4, 8, 13, 9, "carbon"), (4, 13, 5, 7, 14, 8, "padded"), (4, 14, 4.6, 7, 15, 5.4, "carbon"), (3.4, 14, 5, 4.2, 18, 8, "carbon"), (6.8, 14, 5, 7.6, 18, 8, "carbon"), (3.4, 18, 5, 7.6, 19, 8, "carbon"),
        (10, 11, 6, 14, 12, 10, "carbon"), (11, 12, 7, 12, 17, 8, "steel"), (10.6, 17, 6.6, 12.4, 18, 8.4, "carbon")]),
    dict(id="field_antenna_mast", fr="Mât d'antenne de campagne", en="Field antenna mast", elements=[
        (4, 0, 4, 12, 3, 12, "dgray"), (7, 3, 7, 9, 28, 9, "steel"), (3, 8, 7.4, 13, 9, 8.6, "steel"), (4, 14, 7.4, 12, 15, 8.6, "steel"), (5, 20, 7.4, 11, 21, 8.6, "steel"), (6, 28, 6, 10, 30, 10, "usa_red"), (7, 30, 7, 9, 32, 9, "steel"),
        (9, 0, 3, 14, 6, 7, "us_olive"), (10, 6, 4, 13, 7, 6, "carbon"), (2, 3, 5, 4, 12, 7, "rope")]),
    # ================================================== CAFÉTÉRIA ET PX
    dict(id="burger_grill", fr="Grill à burgers", en="Burger grill", elements=[
        (0, 0, 3, 16, 12, 13, "chrome"), (0, 12, 3, 16, 14, 13, "dgray"), (1, 14, 4, 15, 15, 12, "carbon"), (2, 15, 5, 5, 16, 8, "patty"), (6, 15, 5, 9, 16, 8, "patty"), (10, 15, 6, 13, 16, 9, "patty"), (3, 16, 5.4, 4, 17, 7.6, "cheese"),
        (7, 16, 5.4, 8, 17, 7.6, "cheese"), (1, 14, 12, 15, 24, 13, "chrome"), (2, 2, 2.4, 14, 10, 3, "steel"), (11, 5, 2, 13, 7, 2.4, "ketchup")]),
    dict(id="snack_vending_us", fr="Distributeur de snacks", en="Snack vending machine", light=7, elements=[
        (1, 0, 3, 15, 30, 15, "usa_red"), (2, 12, 2.4, 11, 28, 3, "glass"), (3, 24, 2, 6, 27, 2.4, "mustard"), (7, 24, 2, 10, 27, 2.4, "usa_blue"), (3, 19, 2, 6, 22, 2.4, "cheese"), (7, 19, 2, 10, 22, 2.4, "ketchup"),
        (3, 14, 2, 6, 17, 2.4, "usa_blue"), (7, 14, 2, 10, 17, 2.4, "mustard"), (12, 20, 2.4, 14, 26, 3, "carbon"), (12, 6, 2.4, 14, 10, 3, "steel"), (3, 1, 2.4, 10, 5, 3, "carbon")]),
    dict(id="px_shelf", fr="Étagère du PX", en="PX shelf", elements=[
        (0, 0, 4, 16, 30, 12, "padded"), (1, 2, 4.4, 15, 3, 11.6, "star_white"), (1, 11, 4.4, 15, 12, 11.6, "star_white"), (1, 20, 4.4, 15, 21, 11.6, "star_white"), (2, 3, 5, 5, 9, 9, "usa_red"), (6, 3, 5, 9, 8, 9, "usa_blue"),
        (10, 3, 5, 13, 10, 9, "mustard"), (2, 12, 5, 7, 17, 9, "pine"), (8, 12, 5, 12, 16, 9, "cheese"), (3, 21, 5, 6, 27, 9, "ketchup"), (8, 21, 5, 13, 26, 9, "star_white")]),
    dict(id="drink_fridge", fr="Frigo à boissons", en="Drink fridge", light=6, elements=[
        (1, 0, 3, 15, 30, 14, "star_white"), (2, 4, 2.4, 14, 28, 3, "glass"), (3, 6, 3, 6, 12, 8, "usa_red"), (7, 6, 3, 10, 12, 8, "usa_blue"), (11, 6, 3, 13, 12, 8, "pine"), (3, 15, 3, 6, 21, 8, "usa_blue"), (7, 15, 3, 10, 21, 8, "mustard"),
        (11, 15, 3, 13, 21, 8, "usa_red"), (3, 23, 3, 13, 27, 8, "star_white"), (12, 12, 2, 14, 14, 2.4, "steel"), (1, 0, 3, 15, 2, 14, "dgray")]),
    dict(id="hotdog_stand", fr="Stand de hot-dogs", en="Hot dog stand", elements=[
        (0, 0, 4, 16, 14, 13, "usa_red"), (0, 14, 3, 16, 16, 14, "chrome"), (1, 2, 3.4, 15, 12, 4, "star_white"), (2, 16, 5, 6, 18, 8, "bun"), (7, 16, 5, 11, 18, 8, "bun"), (2, 17, 5.4, 6, 18.4, 7.6, "ketchup"), (7, 17, 5.4, 11, 18.4, 7.6, "mustard"),
        (7, 16, 11, 9, 28, 13, "chrome"), (0, 28, 3, 16, 30, 14, "usa_blue"), (2, 24, 3, 14, 28, 4, "star_white"), (4, 25, 2.6, 12, 27, 3, "usa_red")]),
    dict(id="cafeteria_tray_line", fr="Comptoir de service de la cafétéria", en="Cafeteria tray line", elements=[
        (0, 0, 3, 16, 12, 13, "chrome"), (0, 12, 2, 16, 14, 14, "steel"), (1, 14, 4, 15, 15, 12, "dgray"), (2, 14, 5, 6, 16, 11, "tin"), (7, 14, 5, 11, 16, 11, "tin"), (12, 14, 5, 15, 16, 11, "tin"), (3, 16, 6, 5, 17, 10, "ketchup"),
        (8, 16, 6, 10, 17, 10, "cheese"), (13, 16, 6, 14, 17, 10, "pine"), (0, 18, 1, 16, 20, 4, "chrome"), (1, 14, 2, 15, 15, 3, "carbon")]),
    # ================================================== DORTOIR
    dict(id="footlocker_us", fr="Coffre de caserne", en="Footlocker", elements=[
        (1, 0, 3, 15, 8, 13, "us_olive"), (1, 8, 3, 15, 10, 13, "camo_g"), (1, 3, 2.4, 15, 4, 3, "steel"), (1, 6, 2.4, 15, 7, 3, "steel"), (6, 6, 2, 10, 9, 3, "steel"), (7, 4, 1.8, 9, 5, 2.4, "brass"), (0, 0, 2, 2, 3, 4, "steel"), (14, 0, 2, 16, 3, 4, "steel"),
        (3, 10, 5, 9, 11, 9, "paper")]),
    dict(id="sea_bag", fr="Sac de marin", en="Duffel bag", elements=[
        (3, 0, 3, 13, 12, 13, "us_olive"), (4, 12, 4, 12, 15, 12, "camo_g"), (5, 15, 5, 11, 17, 11, "us_olive"), (3, 3, 2.6, 13, 4, 3, "rope"), (3, 8, 2.6, 13, 9, 3, "rope"), (6, 5, 2.4, 10, 7, 3, "star_white"), (7, 5.6, 2.2, 9, 6.4, 2.6, "carbon"),
        (13, 3, 6, 15, 7, 10, "us_olive")]),
    dict(id="combat_boots_pair", fr="Bottes de combat", en="Combat boots", elements=[
        (0, 0, 4, 16, 1, 12, "wood"), (1, 1, 5, 6, 12, 9, "leather"), (1, 1, 9, 6, 4, 12, "carbon"), (2, 11, 5.4, 5, 13, 8.6, "carbon"), (10, 1, 5, 15, 12, 9, "leather"), (10, 1, 9, 15, 4, 12, "carbon"), (11, 11, 5.4, 14, 13, 8.6, "carbon"),
        (1, 4, 5, 6, 5, 9, "carbon"), (10, 4, 5, 15, 5, 9, "carbon"), (2, 7, 4.6, 5, 8, 5, "rope"), (11, 7, 4.6, 14, 8, 5, "rope")]),
    dict(id="mini_fridge", fr="Mini-frigo", en="Mini fridge", elements=[
        (2, 0, 3, 14, 14, 13, "star_white"), (3, 1, 2.4, 13, 7, 3, "padded"), (3, 8, 2.4, 13, 13, 3, "star_white"), (11, 9, 2, 12, 12, 2.4, "steel"), (11, 2, 2, 12, 5, 2.4, "steel"), (2, 0, 3, 14, 1, 13, "dgray"), (4, 14, 5, 8, 16, 9, "usa_red"),
        (9, 14, 6, 13, 15, 10, "star_white")]),
    dict(id="desk_fan", fr="Ventilateur de bureau", en="Desk fan", elements=[
        (4, 0, 4, 12, 2, 12, "dgray"), (7, 2, 7, 9, 9, 9, "steel"), (3, 9, 5, 13, 17, 7, "carbon"), (4, 10, 4.4, 12, 16, 5, "steel"), (7, 12, 4, 9, 14, 5, "dgray"), (7.4, 9, 7, 8.6, 17, 7.6, "dgray"), (4, 10, 4.4, 5, 16, 5, "steel"),
        (11, 10, 4.4, 12, 16, 5, "steel"), (6, 0, 2.6, 10, 1, 3, "usa_red")]),
    dict(id="bedside_radio_us", fr="Radio de chevet", en="Bedside radio", light=3, elements=[
        (2, 0, 4, 14, 8, 12, "wood"), (3, 1, 3.4, 13, 7, 4, "carbon"), (4, 3, 3.2, 8, 6, 3.6, "lamp"), (9, 2, 3.2, 12, 5, 3.6, "star_white"), (12, 8, 6, 13, 18, 7, "steel"), (3, 8, 5, 6, 9, 9, "usa_blue"), (2, 0, 4, 14, 1, 12, "dwood")]),
    # ================================================== SALLE DE SPORT
    dict(id="weight_bench", fr="Banc de musculation", en="Weight bench", elements=[
        (3, 7, 1, 13, 10, 15, "carbon"), (3, 10, 1, 13, 11, 15, "usa_red"), (1, 0, 2, 4, 7, 4, "steel"), (12, 0, 2, 15, 7, 4, "steel"), (1, 0, 12, 4, 7, 14, "steel"), (12, 0, 12, 15, 7, 14, "steel"), (2, 14, 2, 4, 28, 4, "steel"),
        (12, 14, 2, 14, 28, 4, "steel"), (0, 24, 2, 16, 26, 4, "steel"), (0, 22, 1, 2, 28, 5, "carbon"), (14, 22, 1, 16, 28, 5, "carbon")]),
    dict(id="dumbbell_rack", fr="Râtelier d'haltères", en="Dumbbell rack", elements=[
        (0, 0, 4, 16, 3, 12, "dgray"), (1, 3, 5, 3, 14, 11, "steel"), (13, 3, 5, 15, 14, 11, "steel"), (2, 8, 4.4, 14, 9, 11.6, "carbon"), (2, 14, 4.4, 14, 15, 11.6, "carbon"), (2, 9, 5, 4, 13, 11, "carbon"), (6, 9, 5, 8, 12, 11, "carbon"),
        (10, 9, 5, 12, 11, 11, "carbon"), (3, 15, 5, 5, 18, 11, "steel"), (8, 15, 5, 10, 17, 11, "steel"), (12, 15, 5, 14, 16, 11, "steel")]),
    dict(id="treadmill", fr="Tapis de course", en="Treadmill", anim="belt", elements=[
        (1, 2, 1, 15, 4, 15, "carbon"), (3, 4, 2, 13, 6, 14, "belt", {"up": "anim"}), (1, 4, 1, 3, 7, 15, "dgray"), (13, 4, 1, 15, 7, 15, "dgray"), (2, 7, 13, 14, 24, 15, "dgray"), (3, 18, 12.4, 13, 24, 13, "carbon"), (5, 20, 12, 8, 23, 12.4, "lamp"),
        (2, 14, 12, 4, 20, 14, "steel"), (12, 14, 12, 14, 20, 14, "steel"), (1, 0, 1, 3, 2, 3, "dgray"), (13, 0, 13, 15, 2, 15, "dgray")]),
    dict(id="punching_bag", fr="Punching-ball", en="Punching bag", elements=[
        (3, 28, 3, 13, 31, 13, "steel"), (7.4, 22, 7.4, 8.6, 28, 8.6, "rope"), (5, 4, 5, 11, 22, 11, "usa_red"), (5, 10, 5, 11, 12, 11, "star_white"), (5, 4, 5, 11, 6, 11, "carbon"), (6, 2, 6, 10, 4, 10, "carbon"), (5, 20, 5, 11, 22, 11, "carbon"),
        (2, 0, 2, 14, 1, 14, "carbon")]),
    dict(id="pullup_bar", fr="Barre de traction", en="Pull-up bar", passable=True, elements=[
        (0, 0, 7, 2, 30, 9, "steel"), (14, 0, 7, 16, 30, 9, "steel"), (0, 28, 7, 16, 30, 9, "steel"), (3, 29, 7, 5, 30, 9, "carbon"), (11, 29, 7, 13, 30, 9, "carbon"), (0, 0, 6, 2, 2, 10, "dgray"), (14, 0, 6, 16, 2, 10, "dgray"),
        (6, 14, 7.4, 10, 16, 8.6, "usa_blue")]),
    dict(id="exercise_mat", fr="Tapis de sol", en="Exercise mat", passable=True, elements=[
        (0, 0, 0, 16, 1, 16, "usa_blue"), (1, 1, 1, 15, 1.4, 15, "denim"), (0, 0, 7, 16, 1.2, 9, "star_white"), (7, 0, 0, 9, 1.2, 16, "star_white"), (3, 1.4, 3, 5, 1.8, 13, "padded"), (11, 1.4, 3, 13, 1.8, 13, "padded")]),
    # ================================================== LOISIRS
    dict(id="ping_pong_table", fr="Table de ping-pong", en="Ping-pong table", elements=table(12, "pine", "dgray", 0, 16, 0, 16) + [
        (0, 12, 0, 16, 13, 16, "pine"), (0, 12.4, 7.6, 16, 13.2, 8.4, "star_white"), (0, 12.4, 0, 16, 13.2, 0.6, "star_white"), (0, 12.4, 15.4, 16, 13.2, 16, "star_white"), (7.6, 13, 0, 8.4, 16, 16, "padded"), (7.9, 13.2, 0, 8.1, 15.8, 16, "star_white"),
        (3, 13.2, 3, 5, 13.8, 4, "star_white"), (11, 13.2, 11, 13, 14, 13, "usa_red"), (11, 14, 11.2, 12, 15, 11.8, "usa_red")]),
    dict(id="dartboard", fr="Jeu de fléchettes", en="Dartboard", passable=True, elements=[
        (4, 4, 14.6, 12, 12, 16, "carbon"), (5, 5, 14.2, 11, 11, 14.6, "ketchup"), (6, 6, 13.8, 10, 10, 14.2, "star_white"), (7, 7, 13.4, 9, 9, 13.8, "pine"), (7.6, 7.6, 13.2, 8.4, 8.4, 13.4, "ketchup"), (3, 3, 14.8, 13, 4, 16, "dwood"),
        (3, 12, 14.8, 13, 13, 16, "dwood"), (3, 4, 14.8, 4, 12, 16, "dwood"), (12, 4, 14.8, 13, 12, 16, "dwood"), (8.8, 8, 12.6, 9.6, 9, 13.8, "usa_blue")]),
    dict(id="basketball_hoop", fr="Panier de basket", en="Basketball hoop", elements=[
        (6, 0, 12, 10, 2, 16, "dgray"), (7, 2, 13, 9, 28, 15, "padded"), (1, 20, 11, 15, 30, 13, "star_white"), (6, 21, 10.6, 10, 26, 11, "usa_red"), (0, 20, 10.8, 16, 21, 13.2, "usa_red"), (3, 18, 6, 13, 19, 10, "usa_red"), (3, 14, 6.4, 13, 18, 9.6, "star_white"),
        (6, 14, 6.4, 8, 18, 9.6, "usa_red"), (7, 17, 8, 9, 18, 12, "steel")]),
    dict(id="poker_table", fr="Table de poker", en="Poker table", elements=table(11, "pine", "dwood", 0, 16, 0, 16) + [
        (0, 11, 0, 16, 12, 16, "wood"), (1, 12, 1, 15, 12.6, 15, "pine"), (3, 12.6, 3, 5, 13, 6, "star_white"), (6, 12.6, 5, 8, 13, 8, "star_white"), (9, 12.6, 3, 11, 13, 6, "star_white"), (3, 12.6, 10, 5, 13, 13, "star_white"),
        (9, 12.6, 10, 11, 13, 13, "star_white"), (7, 12.6, 7, 9, 14, 9, "ketchup"), (7, 14, 7, 9, 15, 9, "usa_blue"), (12, 12.6, 6, 14, 14, 8, "gold")]),
    dict(id="camp_bbq", fr="Barbecue de camp", en="Camp BBQ", anim="fire", light=9, elements=[
        (2, 0, 3, 14, 2, 13, "dgray"), (3, 2, 4, 13, 8, 12, "carbon"), (4, 8, 5, 12, 10, 11, "dgray"), (5, 9, 6, 11, 10, 10, "carbon", {"up": "anim"}), (3, 10, 4, 13, 11, 12, "steel"), (4, 11, 5, 6, 12, 8, "patty"), (8, 11, 6, 11, 12, 9, "patty"),
        (1, 0, 3, 3, 8, 5, "steel"), (13, 0, 3, 15, 8, 5, "steel"), (13, 8, 6, 16, 10, 10, "wood")]),
    dict(id="outdoor_movie_screen", fr="Écran de cinéma en plein air", en="Outdoor movie screen", elements=[
        (0, 0, 7, 2, 24, 9, "dgray"), (14, 0, 7, 16, 24, 9, "dgray"), (1, 8, 7.6, 15, 28, 8.4, "star_white"), (0, 7, 7.2, 16, 8, 8.8, "carbon"), (0, 28, 7.2, 16, 29, 8.8, "carbon"), (5, 14, 7.4, 11, 22, 7.6, "denim"), (6, 11, 7.4, 10, 14, 7.6, "pine"),
        (2, 29, 6, 14, 30, 10, "carbon")]),
    # ================================================== ARMURERIE
    dict(id="m4_rack", fr="Râtelier de M4", en="M4 rifle rack", elements=[
        (0, 0, 5, 16, 3, 11, "dgray"), (0, 24, 5, 16, 26, 11, "dgray"), (0, 3, 5, 2, 24, 11, "us_olive"), (14, 3, 5, 16, 24, 11, "us_olive"), (2, 4, 7.4, 3.4, 22, 8.6, "carbon"), (4.6, 4, 7.4, 6, 22, 8.6, "carbon"), (7.2, 4, 7.4, 8.6, 22, 8.6, "carbon"),
        (9.8, 4, 7.4, 11.2, 22, 8.6, "carbon"), (12.4, 4, 7.4, 13.8, 22, 8.6, "carbon"), (2, 14, 7, 3.4, 16, 9, "dgray"), (4.6, 14, 7, 6, 16, 9, "dgray"), (7.2, 14, 7, 8.6, 16, 9, "dgray"), (9.8, 14, 7, 11.2, 16, 9, "dgray"),
        (12.4, 14, 7, 13.8, 16, 9, "dgray")]),
    dict(id="ammo_crates_us", fr="Caisses de munitions US", en="US ammo crates", elements=[
        (0, 0, 1, 10, 7, 9, "us_olive"), (6, 0, 8, 16, 7, 16, "us_olive"), (2, 7, 2, 12, 12, 8, "camo_g"), (0, 3, 1, 10, 4, 9, "dgray"), (6, 3, 8, 16, 4, 16, "dgray"), (3, 12, 3, 5, 14, 5, "mustard"), (7, 12, 3, 9, 13, 5, "mustard"),
        (2, 5, 0.6, 8, 6, 1, "star_white"), (9, 4, 7.6, 13, 5, 8, "star_white"), (1, 7.4, 4, 3, 8, 6, "steel")]),
    dict(id="armorer_bench", fr="Établi d'armurier", en="Armorer's bench", elements=table(13, "wood", "dwood", 0, 16, 2, 14) + [
        (0, 13, 2, 16, 14, 14, "dgray"), (1, 14, 4, 7, 15, 9, "tin"), (2, 15, 5, 6, 16, 8, "carbon"), (8, 14, 4, 15, 15, 7, "carbon"), (9, 15, 4.4, 14, 16, 5.6, "carbon"), (9, 14, 9, 14, 16, 12, "steel"), (1, 20, 13.4, 15, 28, 14, "padded"),
        (2, 22, 13, 5, 26, 13.4, "steel"), (7, 22, 13, 9, 26, 13.4, "steel"), (11, 22, 13, 13, 26, 13.4, "steel")]),
    dict(id="target_dummy_us", fr="Mannequin de tir US", en="US target dummy", elements=[
        (3, 0, 3, 13, 2, 13, "dwood"), (7, 2, 7, 9, 16, 9, "wood"), (4, 16, 6, 12, 26, 10, "camo_sand"), (5, 26, 6.4, 11, 29, 9.6, "camo_sand"), (2, 18, 6.4, 4, 24, 9.6, "camo_sand"), (12, 18, 6.4, 14, 24, 9.6, "camo_sand"), (6, 20, 5.6, 10, 24, 6, "usa_red"),
        (7, 21, 5.4, 9, 23, 5.8, "carbon"), (6, 28, 6, 10, 29, 6.4, "camo_g")]),
    dict(id="gear_lockers", fr="Casiers d'équipement", en="Gear lockers", elements=[
        (0, 0, 3, 8, 30, 15, "padded"), (8, 0, 3, 16, 30, 15, "us_olive"), (1, 2, 2.4, 7, 28, 3, "dgray"), (9, 2, 2.4, 15, 28, 3, "camo_g"), (3, 20, 2, 5, 22, 2.4, "steel"), (11, 20, 2, 13, 22, 2.4, "steel"), (3, 6, 2, 5, 8, 2.4, "steel"),
        (11, 6, 2, 13, 8, 2.4, "steel"), (2, 24, 1.8, 6, 26, 2.4, "usa_red"), (10, 24, 1.8, 14, 26, 2.4, "star_white")]),
    dict(id="oil_can_stack", fr="Bidons d'huile empilés", en="Oil can stack", elements=[
        (0, 0, 1, 8, 10, 9, "us_olive"), (8, 0, 3, 16, 10, 11, "us_olive"), (3, 10, 3, 11, 20, 11, "us_olive"), (0, 3, 1, 8, 4, 9, "steel"), (8, 3, 3, 16, 4, 11, "steel"), (3, 14, 3, 11, 15, 11, "steel"), (2, 10, 3, 6, 11, 7, "chrome"),
        (10, 10, 5, 14, 11, 9, "chrome"), (5, 20, 5, 9, 21, 9, "chrome"), (1, 5, 0.6, 7, 8, 1, "mustard"), (9, 5, 2.6, 15, 8, 3, "mustard")]),
    # ================================================== HANGAR ET AVIATION
    dict(id="aircraft_tug", fr="Tracteur de remorquage d'avion", en="Aircraft tug", elements=[
        (1, 2, 2, 15, 6, 14, "mustard"), (2, 6, 3, 10, 12, 13, "mustard"), (3, 12, 4, 9, 14, 12, "carbon"), (3.4, 8, 2.6, 8.6, 12, 3, "glass"), (11, 6, 5, 14, 9, 11, "dgray"), (0, 3, 6, 2, 5, 10, "carbon"), (1, 0, 1, 5, 4, 4, "wheel"),
        (10, 0, 1, 14, 4, 4, "wheel"), (1, 0, 12, 5, 4, 15, "wheel"), (10, 0, 12, 14, 4, 15, "wheel"), (4, 14, 6, 8, 16, 10, "usa_red")]),
    dict(id="aircraft_boarding_ladder", fr="Escabeau d'avion", en="Aircraft step ladder", elements=[
        (0, 0, 2, 3, 4, 14, "steel"), (3, 0, 2, 6, 8, 14, "steel"), (6, 0, 2, 9, 12, 14, "steel"), (9, 0, 2, 12, 16, 14, "steel"), (12, 0, 2, 16, 20, 14, "steel"), (12, 20, 2, 16, 22, 14, "usa_red"), (12, 22, 2, 14, 30, 3, "steel"),
        (12, 22, 13, 14, 30, 14, "steel"), (0, 0, 0, 2, 3, 2, "wheel"), (14, 0, 0, 16, 3, 2, "wheel"), (0, 0, 14, 2, 3, 16, "wheel"), (14, 0, 14, 16, 3, 16, "wheel")]),
    dict(id="pilot_helmet_stand", fr="Support de casque de pilote", en="Pilot helmet stand", elements=[
        (4, 0, 4, 12, 2, 12, "dgray"), (7, 2, 7, 9, 14, 9, "steel"), (4, 14, 4, 12, 24, 12, "star_white"), (3, 14, 5, 13, 18, 11, "star_white"), (4, 18, 3.4, 12, 22, 4.4, "carbon"), (5, 19, 3.2, 11, 21, 3.6, "brass"), (5, 14, 11, 11, 16, 13, "carbon"),
        (6, 22, 6, 10, 24, 10, "usa_blue"), (3, 15, 6, 4, 18, 10, "usa_red"), (12, 15, 6, 13, 18, 10, "usa_red")]),
    dict(id="folded_parachute", fr="Parachute plié", en="Folded parachute", elements=[
        (1, 0, 2, 15, 8, 14, "star_white"), (2, 8, 3, 14, 11, 13, "camo_sand"), (3, 11, 4, 13, 12, 12, "star_white"), (1, 3, 1.6, 15, 4, 2.4, "usa_red"), (1, 3, 13.6, 15, 4, 14.4, "usa_red"), (6, 12, 6, 10, 14, 10, "steel"), (0, 4, 5, 1, 8, 11, "rope"),
        (15, 4, 5, 16, 8, 11, "rope"), (7, 0, 14, 9, 3, 16, "usa_red"), (7, 5, 1.6, 9, 7, 2.4, "carbon")]),
    dict(id="jp8_fuel_drums", fr="Fûts de carburant JP-8", en="JP-8 fuel drums", elements=[
        (0, 0, 1, 7, 12, 8, "usa_red"), (8, 0, 2, 15, 12, 9, "usa_red"), (3, 0, 9, 10, 12, 16, "usa_red"), (0, 4, 1, 7, 5, 8, "steel"), (8, 4, 2, 15, 5, 9, "steel"), (3, 8, 9, 10, 9, 16, "steel"), (0, 12, 1, 7, 13, 8, "carbon"),
        (8, 12, 2, 15, 13, 9, "carbon"), (3, 12, 9, 10, 13, 16, "carbon"), (1, 6, 0.6, 6, 9, 1, "mustard"), (9, 6, 1.6, 14, 9, 2, "mustard")]),
    dict(id="maintenance_board", fr="Tableau de maintenance", en="Maintenance board", passable=True, elements=[
        (0, 1, 14.4, 16, 16, 16, "us_olive"), (1, 2, 14, 15, 15, 14.4, "star_white"), (2, 12, 13.8, 14, 13, 14, "denim"), (2, 9, 13.8, 14, 10, 14, "denim"), (2, 6, 13.8, 14, 7, 14, "denim"), (2, 3, 13.8, 14, 4, 14, "denim"),
        (3, 12.2, 13.6, 4.4, 13.2, 14, "pine"), (6, 9.2, 13.6, 7.4, 10.2, 14, "ketchup"), (9, 6.2, 13.6, 10.4, 7.2, 14, "pine"), (12, 3.2, 13.6, 13.4, 4.2, 14, "mustard"), (0, 0, 14.4, 1, 1, 16, "dgray")]),
    # ================================================== OPÉRATIONS SPÉCIALES ET MISSILES
    dict(id="us_launch_console", fr="Console de lancement US", en="US launch console", anim="blink", light=6, elements=[
        (0, 0, 3, 16, 10, 15, "us_olive"), (0, 10, 3, 16, 14, 8, "dgray"), (0, 14, 8, 16, 22, 10, "carbon"), (1, 14.5, 8, 15, 21.5, 7.4, "carbon", {"north": "anim"}), (2, 10.6, 4, 5, 11.6, 7, "ketchup"), (6, 10.6, 4, 9, 11.6, 7, "mustard"),
        (10, 10.6, 4, 13, 11.6, 7, "pine"), (3, 11.6, 5, 4, 13, 6, "ketchup"), (11, 11.6, 5, 12, 13, 6, "steel"), (0, 0, 3, 16, 2, 15, "dgray")]),
    dict(id="dual_key_station", fr="Poste à double clé", en="Dual key station", elements=[
        (2, 0, 3, 14, 6, 13, "carbon"), (3, 6, 4, 13, 8, 12, "dgray"), (3.4, 8, 5, 5.6, 9, 7, "steel"), (4, 9, 5.4, 5, 12, 6.6, "gold"), (10.4, 8, 5, 12.6, 9, 7, "steel"), (11, 9, 5.4, 12, 12, 6.6, "gold"), (3, 8, 9, 13, 10, 11, "usa_red"),
        (6, 10, 9.4, 10, 11, 10.6, "paper"), (7, 6, 3.4, 9, 8, 4, "ketchup"), (2, 2, 2.6, 14, 4, 3, "mustard")]),
    dict(id="missile_silo_hatch", fr="Trappe de silo à missile", en="Missile silo hatch", elements=[
        (0, 0, 0, 16, 2, 16, "carbon"), (1, 2, 1, 15, 4, 15, "dgray"), (2, 4, 2, 14, 6, 14, "padded"), (3, 6, 3, 13, 7, 13, "carbon"), (3, 6.6, 7.2, 13, 7.2, 8.8, "usa_red"), (7.2, 6.6, 3, 8.8, 7.2, 13, "usa_red"), (5, 7, 5, 11, 8, 11, "steel"),
        (6, 8, 6, 10, 9, 10, "mustard"), (1, 4, 1, 3, 5, 3, "mustard"), (13, 4, 13, 15, 5, 15, "mustard")]),
    dict(id="op_plans_table", fr="Table des plans d'opération", en="Operations plans table", light=5, elements=table(11, "dgray", "carbon", 0, 16, 1, 15) + [
        (1, 11, 2, 15, 12, 14, "denim"), (2, 12, 3, 8, 12.4, 9, "leaf"), (9, 12, 4, 14, 12.4, 10, "water"), (3, 12.4, 10, 6, 15, 13, "carbon"), (10, 12.4, 11, 12, 14, 13, "ketchup"), (4, 12.4, 5, 5, 14, 6, "usa_red"),
        (11, 12.4, 6, 12, 14, 7, "usa_blue"), (7, 12.4, 7, 9, 13, 9, "star_white")]),
    dict(id="tactical_mannequin", fr="Mannequin tactique", en="Tactical mannequin", elements=[
        (3, 0, 3, 13, 2, 13, "dgray"), (7, 2, 7, 9, 14, 9, "steel"), (4, 14, 6, 12, 26, 10, "camo_g"), (5, 26, 6.4, 11, 29, 9.6, "carbon"), (3, 16, 6.4, 4, 24, 9.6, "camo_g"), (12, 16, 6.4, 13, 24, 9.6, "camo_g"), (5, 18, 5.4, 11, 24, 6, "padded"),
        (6, 20, 5, 10, 22, 5.4, "mustard"), (5, 29, 6, 11, 31, 10, "camo_g"), (2, 14, 6, 14, 15, 10, "carbon")]),
    dict(id="body_armor_rack", fr="Support de gilets pare-balles", en="Body armor rack", elements=[
        (1, 0, 5, 15, 2, 11, "dgray"), (2, 2, 7, 4, 28, 9, "steel"), (12, 2, 7, 14, 28, 9, "steel"), (1, 26, 6, 15, 28, 10, "steel"), (3, 10, 6, 7, 26, 10, "camo_sand"), (9, 10, 6, 13, 26, 10, "camo_g"), (4, 12, 5.4, 6, 16, 6, "padded"),
        (10, 12, 5.4, 12, 16, 6, "padded"), (3, 20, 5.4, 7, 21, 6, "mustard"), (9, 20, 5.4, 13, 21, 6, "mustard")]),
    # ================================================== DÉFENSE
    dict(id="guard_post_us", fr="Poste de garde", en="Guard post", elements=[
        (1, 0, 1, 15, 2, 15, "dgray"), (2, 2, 2, 14, 16, 14, "us_olive"), (3, 6, 1.4, 13, 13, 2, "glass"), (2, 16, 1, 14, 18, 15, "carbon"), (6, 2, 1.4, 10, 12, 2, "carbon"), (0, 6, 6, 2, 8, 10, "mustard"), (7, 18, 7, 9, 21, 9, "ketchup"),
        (12, 18, 12, 14, 24, 14, "steel"), (11, 24, 11, 15, 26, 15, "usa_blue"), (3, 14, 1.2, 13, 15, 1.6, "star_white")]),
    dict(id="spike_barrier", fr="Herse routière", en="Spike barrier", elements=[
        (0, 0, 3, 16, 3, 13, "carbon"), (1, 3, 4, 15, 4, 12, "steel"), (1, 4, 5, 3, 8, 7, "steel"), (4, 4, 5, 6, 8, 7, "steel"), (7, 4, 5, 9, 8, 7, "steel"), (10, 4, 5, 12, 8, 7, "steel"), (13, 4, 5, 15, 8, 7, "steel"),
        (1, 4, 9, 3, 8, 11, "steel"), (4, 4, 9, 6, 8, 11, "steel"), (7, 4, 9, 9, 8, 11, "steel"), (10, 4, 9, 12, 8, 11, "steel"), (13, 4, 9, 15, 8, 11, "steel")]),
    dict(id="watchtower_post", fr="Mirador de surveillance", en="Watchtower post", elements=[
        (2, 0, 2, 4, 26, 4, "wood"), (12, 0, 2, 14, 26, 4, "wood"), (2, 0, 12, 4, 26, 14, "wood"), (12, 0, 12, 14, 26, 14, "wood"), (2, 8, 4, 14, 9, 12, "wood"), (2, 16, 4, 14, 17, 12, "wood"), (0, 26, 0, 16, 28, 16, "dwood"),
        (1, 28, 1, 15, 32, 2, "us_olive"), (1, 28, 14, 15, 32, 15, "us_olive"), (1, 28, 2, 2, 32, 14, "us_olive"), (14, 28, 2, 15, 32, 14, "us_olive"), (6, 28, 6, 10, 30, 10, "steel")]),
    dict(id="camo_net", fr="Filet de camouflage", en="Camo net", passable=True, elements=[
        (0, 24, 0, 16, 25, 16, "camo_g"), (1, 24.5, 1, 6, 25.5, 6, "camo_sand"), (8, 24.5, 2, 14, 25.5, 8, "us_olive"), (2, 24.5, 9, 9, 25.5, 15, "camo_sand"), (10, 24.5, 10, 15, 25.5, 15, "camo_g"), (0, 0, 0, 2, 24, 2, "wood"),
        (14, 0, 0, 16, 24, 2, "wood"), (0, 0, 14, 2, 24, 16, "wood"), (14, 0, 14, 16, 24, 16, "wood"), (3, 22, 3, 5, 24, 5, "camo_g"), (10, 22, 11, 12, 24, 13, "camo_sand")]),
    dict(id="base_floodlight", fr="Projecteur de base", en="Base floodlight", light=15, elements=[
        (4, 0, 4, 12, 2, 12, "dgray"), (7, 2, 7, 9, 26, 9, "steel"), (3, 22, 4, 13, 30, 12, "dgray"), (4, 23, 3.4, 12, 29, 4, "lamp"), (2, 24, 6, 3, 28, 10, "steel"), (13, 24, 6, 14, 28, 10, "steel"), (6, 30, 6, 10, 31, 10, "carbon"),
        (9, 0, 6, 15, 4, 10, "us_olive"), (10, 4, 7, 14, 5, 9, "carbon")]),
    dict(id="blast_door_us", fr="Porte blindée américaine", en="US blast door", elements=[
        (0, 0, 5, 16, 32, 11, "padded"), (1, 1, 4.4, 15, 31, 5, "carbon"), (2, 2, 4, 14, 30, 4.4, "dgray"), (11, 14, 3.4, 13, 18, 4, "steel"), (11, 17, 3, 12.4, 19, 4, "steel"), (3, 4, 3.8, 6, 6, 4.4, "mustard"), (3, 26, 3.8, 6, 28, 4.4, "mustard"),
        (3, 14, 3.8, 5, 18, 4.4, "steel"), (0, 8, 4.6, 2, 10, 5, "steel"), (0, 22, 4.6, 2, 24, 5, "steel"), (6, 24, 3.6, 10, 29, 4, "usa_blue"), (7, 26, 3.4, 9, 28, 3.8, "star_white")]),
    # ================================================== SIGNALÉTIQUE MILITAIRE
    dict(id="restricted_area_sign", fr="Panneau « Restricted Area »", en="Restricted Area sign", passable=True, elements=[
        (7, 0, 7.4, 9, 14, 8.6, "steel"), (2, 14, 7, 14, 28, 9, "ketchup"), (3, 15, 6.6, 13, 27, 7, "star_white"), (4, 16, 6.2, 12, 18, 6.6, "ketchup"), (4, 19, 6.2, 12, 21, 6.6, "carbon"), (4, 22, 6.2, 12, 24, 6.6, "carbon"), (4, 25, 6.2, 9, 26, 6.6, "carbon")]),
    dict(id="no_smoking_sign", fr="Panneau « No Smoking »", en="No Smoking sign", passable=True, elements=[
        (3, 4, 14.4, 13, 14, 16, "star_white"), (4, 5, 14, 12, 13, 14.4, "star_white"), (5, 8, 13.6, 11, 9, 14, "carbon"), (10, 8, 13.6, 11, 9.6, 14, "ketchup"), (5, 6, 13.6, 6, 12, 14, "ketchup"), (11, 6, 13.6, 12, 12, 14, "ketchup"),
        (6, 11, 13.6, 11, 12, 14, "ketchup"), (6, 6, 13.6, 11, 7, 14, "ketchup")]),
    dict(id="building_number_sign", fr="Numéro de bâtiment", en="Building number sign", passable=True, elements=[
        (7, 0, 7.4, 9, 12, 8.6, "steel"), (3, 12, 7, 13, 24, 9, "denim"), (4, 13, 6.6, 12, 23, 7, "star_white"), (5, 15, 6.2, 7, 21, 6.6, "carbon"), (9, 15, 6.2, 11, 21, 6.6, "carbon"), (5, 15, 6.2, 11, 17, 6.6, "carbon"), (5, 19, 6.2, 11, 21, 6.6, "carbon")]),
    dict(id="speed_limit_sign", fr="Limitation de vitesse", en="Speed limit sign", passable=True, elements=[
        (7, 0, 7.4, 9, 14, 8.6, "steel"), (3, 14, 7, 13, 28, 9, "star_white"), (4, 15, 6.6, 12, 27, 7, "star_white"), (4, 24, 6.2, 12, 26, 6.6, "carbon"), (5, 20, 6.2, 11, 23, 6.6, "carbon"), (6, 16, 6.2, 8, 19, 6.6, "carbon"), (9, 16, 6.2, 11, 19, 6.6, "carbon"),
        (3, 14, 7.4, 13, 15, 8.6, "carbon")]),
    dict(id="direction_arrows_sign", fr="Panneau de directions", en="Direction arrows sign", passable=True, elements=[
        (7, 0, 7.4, 9, 18, 8.6, "steel"), (1, 18, 7, 15, 22, 9, "usa_blue"), (2, 19, 6.6, 12, 21, 7, "star_white"), (12, 18, 6.8, 15, 22, 9.2, "usa_blue"), (1, 23, 7, 15, 27, 9, "pine"), (4, 24, 6.6, 14, 26, 7, "star_white"), (1, 23, 6.8, 4, 27, 9.2, "pine"),
        (2, 28, 7, 14, 31, 9, "mustard"), (4, 29, 6.6, 12, 30, 7, "carbon")]),
    dict(id="alarm_light_us", fr="Gyrophare d'alarme", en="Alarm light", anim="blink", light=13, passable=True, elements=[
        (4, 0, 12, 12, 2, 16, "dgray"), (5, 2, 12, 11, 4, 15, "steel"), (5, 4, 11, 11, 11, 15, "ketchup"), (5, 4, 10.6, 11, 11, 11, "carbon", {"north": "anim"}), (6, 11, 12, 10, 12, 14, "star_white"), (7, 5, 9.6, 9, 9, 10.6, "lamp")]),
    # ================================================== CAMPS ET TERRAIN
    dict(id="field_tent_us", fr="Tente de campagne US", en="US field tent", elements=[
        (0, 0, 1, 16, 2, 15, "dgray"), (1, 2, 2, 15, 7, 14, "us_olive"), (3, 7, 3, 13, 11, 13, "us_olive"), (5, 11, 4, 11, 14, 12, "camo_g"), (7, 14, 5, 9, 16, 11, "us_olive"), (6, 2, 0.8, 10, 9, 1.4, "carbon"), (7, 2, 0.6, 9, 8, 1, "padded"),
        (0, 0, 7, 1, 16, 9, "wood"), (15, 0, 7, 16, 16, 9, "wood"), (1, 4, 1.4, 4, 6, 2, "star_white")]),
    dict(id="campfire_us", fr="Feu de camp américain", en="US campfire", anim="fire", light=15, elements=[
        (3, 0, 3, 13, 2, 13, "stone"), (5, 2, 5, 11, 8, 11, "black", {"up": "anim", "north": "anim", "south": "anim", "east": "anim", "west": "anim"}), (3, 8, 7, 5, 14, 9, "dwood"), (11, 8, 7, 13, 14, 9, "dwood"), (3, 13, 7, 13, 14, 9, "dwood"),
        (6, 6, 6, 10, 11, 10, "dgray"), (6, 11, 6, 10, 12, 10, "steel"), (1, 0, 1, 4, 5, 4, "dwood"), (12, 0, 12, 15, 5, 15, "dwood")]),
    dict(id="cooler_box", fr="Glacière", en="Cooler box", elements=[
        (1, 0, 3, 15, 9, 13, "usa_blue"), (1, 9, 3, 15, 11, 13, "star_white"), (1, 5, 2.4, 15, 6, 3, "star_white"), (6, 10, 2.4, 10, 12, 3.4, "carbon"), (0, 4, 6, 1, 8, 10, "carbon"), (15, 4, 6, 16, 8, 10, "carbon"), (3, 11, 5, 7, 12, 9, "ketchup"),
        (9, 11, 6, 13, 12, 10, "star_white")]),
    dict(id="folding_cot", fr="Lit pliant", en="Folding cot", elements=[
        (0, 6, 1, 16, 8, 15, "us_olive"), (0, 0, 1, 2, 6, 3, "steel"), (14, 0, 1, 16, 6, 3, "steel"), (0, 0, 13, 2, 6, 15, "steel"), (14, 0, 13, 16, 6, 15, "steel"), (2, 8, 2, 14, 9, 14, "camo_g"), (3, 9, 2, 7, 10, 6, "star_white"),
        (1, 3, 1.6, 15, 4, 2.4, "steel"), (1, 3, 13.6, 15, 4, 14.4, "steel")]),
    dict(id="field_table_us", fr="Table de campagne", en="Field table", elements=table(12, "us_olive", "dgray", 0, 16, 2, 14) + [
        (1, 12, 3, 7, 13, 8, "paper"), (9, 12, 4, 14, 14, 8, "mustard"), (10, 14, 5, 13, 16, 7, "steel"), (3, 12, 9, 6, 15, 12, "ketchup"), (8, 12, 10, 12, 13, 13, "star_white"), (0, 4, 1, 16, 5, 3, "dgray"), (0, 4, 13, 16, 5, 15, "dgray")]),
    dict(id="site_generator", fr="Groupe électrogène de chantier", en="Site generator", elements=[
        (0, 0, 2, 16, 3, 14, "dgray"), (1, 3, 3, 15, 12, 13, "mustard"), (1, 12, 3, 15, 13, 13, "carbon"), (11, 13, 4, 13, 20, 6, "steel"), (2, 6, 2.4, 8, 11, 3, "carbon"), (9, 5, 2.4, 14, 11, 3, "ketchup"), (14, 6, 6, 15, 10, 10, "usa_red"),
        (2, 4, 2.4, 5, 5, 3, "steel"), (3, 13, 5, 6, 15, 9, "carbon"), (0, 3, 4, 1, 12, 12, "steel")]),
    # ================================================== MAIN STREET USA
    dict(id="barber_pole", fr="Enseigne de barbier", en="Barber pole", anim="barber", light=6, elements=[
        (3, 0, 3, 13, 2, 13, "dgray"), (4, 2, 4, 12, 4, 12, "steel"), (5, 4, 5, 11, 22, 11, "star_white", {"north": "anim", "south": "anim", "east": "anim", "west": "anim"}), (4, 22, 4, 12, 24, 12, "steel"), (6, 24, 6, 10, 26, 10, "usa_blue"),
        (7, 26, 7, 9, 28, 9, "usa_red"), (5, 3.6, 3.6, 11, 4.4, 4.4, "chrome")]),
    dict(id="cinema_marquee", fr="Enseigne de cinéma", en="Cinema marquee", anim="cinema", light=12, passable=True, elements=[
        (0, 6, 8, 16, 26, 16, "carbon"), (1, 7, 7.4, 15, 25, 8, "carbon", {"north": "anim"}), (0, 26, 6, 16, 28, 16, "usa_red"), (0, 4, 6, 16, 6, 16, "usa_red"), (6, 0, 12, 10, 4, 14, "steel"), (0, 28, 7, 16, 30, 9, "carbon"),
        (2, 29, 6.4, 14, 30, 7, "lamp")]),
    dict(id="ice_cream_cart", fr="Chariot de glaces", en="Ice cream cart", elements=[
        (1, 3, 3, 15, 12, 13, "star_white"), (0, 12, 2, 16, 14, 14, "usa_red"), (1, 14, 4, 15, 15, 12, "star_white"), (3, 15, 5, 6, 19, 8, "pink"), (7, 15, 5, 10, 18, 8, "cream"), (11, 15, 6, 14, 20, 9, "tan"), (2, 4, 2.4, 14, 11, 3, "glass"),
        (0, 24, 0, 16, 26, 16, "usa_red"), (0, 14, 0, 2, 24, 2, "chrome"), (14, 14, 0, 16, 24, 2, "chrome"), (1, 0, 4, 4, 3, 10, "wheel"), (12, 0, 4, 15, 3, 10, "wheel")]),
    dict(id="little_church", fr="Petite église", en="Little church", elements=[
        (1, 0, 2, 15, 8, 14, "star_white"), (0, 8, 1, 16, 10, 15, "denim"), (3, 10, 3, 13, 14, 13, "denim"), (5, 14, 4, 11, 18, 12, "denim"), (6, 4, 1.4, 10, 8, 2, "wood"), (7, 3, 1.2, 9, 4, 1.6, "brass"), (2, 3, 1.6, 4, 7, 2, "sky"),
        (12, 3, 1.6, 14, 7, 2, "sky"), (7, 18, 7, 9, 26, 9, "star_white"), (7.4, 22, 6.6, 8.6, 24, 7, "brass"), (6, 22, 7.4, 10, 23, 8.6, "brass")]),
    dict(id="school_bus_stop", fr="Arrêt de bus scolaire", en="School bus stop", elements=[
        (7, 0, 7, 9, 22, 9, "steel"), (2, 14, 6, 14, 26, 10, "mustard"), (3, 15, 5.6, 13, 25, 6, "carbon"), (4, 21, 5.2, 12, 24, 5.6, "mustard"), (4, 17, 5.2, 12, 20, 5.6, "carbon"), (6, 15, 5.2, 10, 16, 5.6, "usa_red"), (12, 22, 4, 14, 27, 6, "usa_red"),
        (5, 0, 5, 11, 1, 11, "dgray")]),
    dict(id="post_box_us", fr="Boîte postale américaine", en="US post box", elements=[
        (3, 0, 3, 13, 4, 13, "usa_blue"), (4, 4, 4, 12, 16, 12, "usa_blue"), (3, 16, 3, 13, 20, 13, "usa_blue"), (5, 20, 5, 11, 22, 11, "usa_blue"), (5, 11, 3.4, 11, 13, 4, "carbon"), (5, 7, 3.4, 11, 9, 4, "star_white"), (6, 8, 3.2, 10, 9, 3.6, "usa_red"),
        (2, 12, 6, 3, 18, 10, "dgray"), (13, 6, 6, 14, 8, 10, "carbon")]),
    # ================================================== FÊTES AMÉRICAINES
    dict(id="usa_bunting", fr="Guirlande aux couleurs US", en="US bunting", anim="garland", light=7, passable=True, elements=[
        (0, 12, 7.4, 16, 14, 8.6, "star_white", {"north": "anim", "south": "anim"}), (0, 6, 7, 1, 16, 9, "dwood"), (15, 6, 7, 16, 16, 9, "dwood"), (2, 9, 7.4, 5, 12, 8.6, "usa_red"), (6, 8, 7.4, 9, 12, 8.6, "star_white"),
        (10, 9, 7.4, 13, 12, 8.6, "usa_blue")]),
    dict(id="fourth_july_banner", fr="Banderole du 4 juillet", en="Fourth of July banner", passable=True, elements=[
        (0, 4, 14.6, 16, 14, 15.4, "usa_blue"), (0, 14, 14.2, 16, 15, 15.8, "wood"), (0, 3, 14.2, 16, 4, 15.8, "wood"), (0, 5, 14.2, 16, 6, 15.4, "usa_red"), (0, 8, 14.2, 16, 9, 15.4, "star_white"), (0, 11, 14.2, 16, 12, 15.4, "usa_red"),
        (2, 12, 14, 4, 13.6, 14.4, "star_white"), (6, 12, 14, 8, 13.6, 14.4, "star_white"), (10, 12, 14, 12, 13.6, 14.4, "star_white")]),
    dict(id="thanksgiving_corn", fr="Maïs de Thanksgiving", en="Thanksgiving corn", elements=[
        (1, 0, 4, 15, 2, 12, "wood"), (3, 2, 5, 7, 14, 9, "hay"), (4, 14, 5.4, 6, 20, 8.6, "leaf"), (9, 2, 6, 13, 12, 10, "hay"), (10, 12, 6.4, 12, 18, 9.6, "leaf"), (7, 2, 4, 9, 10, 6, "orange"), (5, 6, 7, 6, 10, 8, "dwood"),
        (10, 4, 7, 11, 9, 8, "dwood"), (6, 2, 11, 12, 6, 14, "orange"), (8, 6, 12, 10, 7, 13, "leaf")]),
    dict(id="pumpkin_stack", fr="Tas de citrouilles", en="Pumpkin stack", elements=[
        (0, 0, 2, 8, 8, 10, "orange"), (8, 0, 4, 16, 7, 12, "orange"), (3, 8, 3, 11, 14, 11, "orange"), (3, 0, 10, 9, 6, 16, "orange"), (3, 14, 6, 5, 16, 8, "leaf"), (11, 7, 7, 12, 9, 9, "leaf"), (3, 6, 2.4, 6, 8, 2.8, "carbon"),
        (9, 4, 4.4, 12, 6, 4.8, "carbon"), (5, 9, 3.4, 9, 11, 3.8, "lamp")]),
    dict(id="firework_display", fr="Feux d'artifice", en="Firework display", anim="fireworks", light=11, elements=[
        (2, 0, 2, 14, 2, 14, "carbon"), (3, 2, 3, 13, 10, 13, "usa_red"), (4, 10, 4, 12, 14, 12, "usa_blue"), (5, 14, 5, 11, 17, 11, "star_white"), (6, 17, 6, 10, 18, 10, "carbon"), (3, 2, 2.6, 13, 10, 3, "star_white", {"north": "anim"}),
        (0, 0, 6, 2, 8, 10, "usa_red"), (14, 0, 6, 16, 8, 10, "usa_blue"), (7, 18, 7, 9, 22, 9, "usa_red"), (6.4, 22, 6.4, 9.6, 23, 9.6, "gold")]),
    dict(id="christmas_wreath", fr="Couronne de Noël", en="Christmas wreath", passable=True, elements=[
        (2, 2, 14.4, 14, 14, 16, "dleaf"), (4, 4, 14, 12, 12, 14.4, "leaf"), (5, 5, 13.6, 11, 11, 14, "dleaf"), (6, 6, 13.4, 10, 10, 13.8, "dleaf"), (6, 11, 13.8, 10, 13, 14.4, "ketchup"), (3, 7, 13.8, 5, 9, 14.2, "ketchup"),
        (11, 7, 13.8, 13, 9, 14.2, "ketchup"), (7, 2, 13.8, 9, 4, 14.2, "brass"), (6, 11, 13.2, 7, 12, 13.8, "usa_red"), (9, 11, 13.2, 10, 12, 13.8, "usa_red")]),
]

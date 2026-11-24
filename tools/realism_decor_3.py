"""
Troisième série de décors : 100 objets pour les villes américaines, soviétiques, du désert et du Mesa, les bases militaires,
bunkers, commissariats, camps, cartel, gangs, rebelles, sites pétroliers, ports, plages, salles de boss et fêtes.
Importé par generate_realism_decor.py (seconde palette « q » pour les couleurs supplémentaires).
"""
import math
import random

from PIL import Image

PAL3 = {
    "gold": "#d8a820", "bronze": "#8a5a2a", "magenta": "#c83a9a", "purple": "#5a2a8a", "cyan": "#2ab8c8", "ice": "#cfe8f4", "sand": "#d8c890",
    "adobe": "#b8784a", "tcotta": "#a8502e", "neon_pink": "#ff4aa8", "neon_blue": "#3a9aff", "usa_red": "#b22234", "usa_blue": "#3c3b6e",
    "ussr_red": "#cc0000", "bone": "#e8e0c8", "leather": "#6a4028", "rust": "#8a4a22", "ash": "#5a5a5a", "wax": "#f4ead0", "turquoise": "#30b8a8",
    "rope": "#b89a68", "plank_dark": "#3e2c1c", "bloodred": "#7a0e12", "emerald": "#20a060", "obsidian": "#1a1228",
}


def _frames(n):
    return Image.new("RGBA", (16, 16 * n))


def anim_jukebox():
    img = _frames(8)
    rnd = random.Random(21)
    cols = [(255, 80, 80), (255, 200, 40), (80, 220, 120), (80, 160, 255)]
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (30, 20, 40, 255))
        for i in range(4):
            h = 3 + rnd.randint(0, 9)
            for y in range(h):
                for dx in range(2):
                    img.putpixel((2 + i * 3 + dx, 14 - y + 16 * f), cols[i] + (255,))
    return img, {"frametime": 3}


def anim_radar():
    img = _frames(12)
    for f in range(12):
        a0 = f * math.pi / 6
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                d = (dx * dx + dy * dy) ** 0.5
                if d > 7.5:
                    c = (40, 44, 48)
                else:
                    a = math.atan2(dy, dx)
                    diff = (a - a0) % (2 * math.pi)
                    glow = max(0, 1 - diff / 1.2)
                    c = (8, int(40 + 190 * glow), int(16 + 60 * glow))
                    if abs(d - 4) < 0.5 or abs(d - 7) < 0.5:
                        c = (20, 90, 40)
                img.putpixel((x, y + 16 * f), c + (255,))
    return img, {"frametime": 2}


def anim_garland():
    img = _frames(6)
    cols = [(255, 60, 60), (255, 210, 50), (70, 220, 100), (70, 150, 255), (230, 80, 220)]
    for f in range(6):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (20, 24, 20, 255) if y in (7, 8) else (0, 0, 0, 0))
        for i in range(8):
            col = cols[(i + f) % 5] if (i + f) % 3 else (70, 70, 70)
            x = 1 + i * 2
            y = 8 + int(2 * math.sin(i / 1.5))
            img.putpixel((x, y + 16 * f), col + (255,))
            img.putpixel((x, y + 1 + 16 * f), col + (255,))
    return img, {"frametime": 4}


def anim_disco():
    img = _frames(8)
    rnd = random.Random(33)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                v = 150 + 60 * (((x // 2) + (y // 2)) % 2)
                img.putpixel((x, y + 16 * f), (v, v, v + 20, 255))
        for _ in range(10):
            img.putpixel((rnd.randint(0, 15), rnd.randint(0, 15) + 16 * f), rnd.choice([(255, 255, 255), (255, 120, 200), (120, 220, 255), (255, 230, 120)]) + (255,))
    return img, {"frametime": 2}


def anim_arcade():
    img = _frames(8)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (8, 8, 30, 255))
        for i in range(4):                                                    # envahisseurs
            img.putpixel((3 + i * 3 + (f % 4 > 1), 3 + 16 * f), (90, 255, 120, 255))
            img.putpixel((3 + i * 3 + (f % 4 > 1), 5 + 16 * f), (255, 90, 120, 255))
        img.putpixel((2 + f, 13 + 16 * f), (255, 255, 90, 255))               # vaisseau
        img.putpixel((2 + f, 12 + 16 * f), (255, 255, 90, 255))
        if f % 2:
            img.putpixel((2 + f, 9 + 16 * f), (255, 255, 255, 255))
    return img, {"frametime": 4}


def anim_motel():
    img = _frames(6)
    for f in range(6):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (20, 8, 24, 255))
        for x in range(2, 14):
            img.putpixel((x, 3 + 16 * f), (255, 74, 168, 255) if (f + x // 3) % 2 else (130, 40, 90, 255))
            img.putpixel((x, 12 + 16 * f), (58, 154, 255, 255) if (f + x // 3) % 2 == 0 else (30, 80, 130, 255))
        for x in range(3, 13):
            if x % 3:
                img.putpixel((x, 7 + 16 * f), (255, 250, 220, 255) if f % 4 < 3 else (90, 90, 80, 255))
                img.putpixel((x, 8 + 16 * f), (255, 250, 220, 255) if f % 4 < 3 else (90, 90, 80, 255))
    return img, {"frametime": 5}


def anim_alembic():
    img = _frames(8)
    rnd = random.Random(8)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (30, 90, 50, 255) if y > 6 else (180, 210, 190, 255))
        for _ in range(5):
            img.putpixel((rnd.randint(2, 13), rnd.randint(1, 15) + 16 * f), (180, 255, 190, 255))
    return img, {"frametime": 3}


ANIMS3 = {"jukebox": anim_jukebox, "radar": anim_radar, "garland": anim_garland, "disco": anim_disco, "arcade": anim_arcade, "motel": anim_motel, "alembic": anim_alembic}

DECOS3 = [
    # ====================================================== VILLES AMÉRICAINES
    dict(id="diner_counter", fr="Comptoir de diner et tabourets", en="Diner counter with stools", elements=[
        (0, 0, 7, 16, 12, 16, "usa_red"), (0, 12, 6, 16, 14, 16, "chrome"), (0, 0, 7, 16, 2, 16, "steel"), (2, 14, 9, 5, 16, 12, "white"), (9, 14, 9, 13, 18, 12, "cream"),
        (2, 0, 0, 4, 8, 2, "chrome"), (2, 8, 0, 4, 10, 4, "usa_red"), (9, 0, 0, 11, 8, 2, "chrome"), (9, 8, 0, 11, 10, 4, "usa_red")]),
    dict(id="jukebox", fr="Juke-box", en="Jukebox", anim="jukebox", light=8, elements=[
        (1, 0, 3, 15, 4, 14, "chrome"), (1, 4, 3, 15, 24, 14, "usa_red"), (2, 12, 2.4, 14, 22, 3, "black", {"north": "anim"}), (3, 5, 2.4, 13, 10, 3, "gold"), (1, 24, 4, 15, 27, 13, "chrome"),
        (4, 25, 3, 12, 26, 4, "lamp")]),
    dict(id="motel_sign", fr="Enseigne de motel", en="Motel sign", anim="motel", light=12, elements=[
        (7, 0, 7, 9, 18, 9, "steel"), (1, 18, 6, 15, 30, 10, "dgray"), (2, 19, 5.4, 14, 29, 6, "black", {"north": "anim"}), (2, 19, 10, 14, 29, 10.6, "black", {"south": "anim"}),
        (5, 0, 5, 11, 2, 11, "dgray")]),
    dict(id="retro_gas_pump", fr="Pompe à essence rétro", en="Retro gas pump", elements=[
        (3, 0, 4, 13, 2, 12, "stone"), (4, 2, 5, 12, 24, 11, "usa_red"), (5, 16, 4.4, 11, 22, 5, "white"), (6, 18, 4.2, 10, 20, 4.4, "black"), (12, 10, 6, 15, 12, 10, "black"),
        (13, 4, 7, 14, 12, 9, "black"), (4, 24, 5, 12, 27, 11, "white"), (5, 27, 6, 11, 28, 10, "usa_red")]),
    dict(id="us_flagpole", fr="Mât et drapeau américain", en="US flagpole", elements=[
        (5, 0, 5, 11, 1, 11, "stone"), (7, 1, 7, 9, 30, 9, "chrome"), (7.4, 30, 7.4, 8.6, 32, 8.6, "gold"), (9, 18, 7.4, 16, 28, 8.6, "white"), (9, 18, 7.4, 16, 20, 8.6, "usa_red"),
        (9, 22, 7.4, 16, 24, 8.6, "usa_red"), (9, 26, 7.4, 16, 28, 8.6, "usa_red"), (9, 23, 7.2, 12, 28, 8.8, "usa_blue")]),
    dict(id="news_vending_box", fr="Distributeur de journaux", en="Newspaper vending box", elements=[
        (3, 0, 4, 13, 2, 12, "dgray"), (3, 2, 4, 13, 16, 12, "usa_blue"), (4, 8, 3.4, 12, 14, 4, "glass"), (5, 9, 3.2, 11, 13, 3.6, "white"), (4, 4, 3.4, 12, 6, 4, "black"), (4, 16, 4, 12, 17, 12, "chrome")]),
    # ====================================================== CITÉS SOVIÉTIQUES
    dict(id="propaganda_poster", fr="Affiche de propagande", en="Propaganda poster", passable=True, elements=[
        (1, 1, 15, 15, 15, 16, "ussr_red"), (2, 2, 14.6, 14, 14, 15, "cream"), (3, 3, 14.2, 13, 9, 14.6, "ussr_red"), (4, 10, 14.2, 8, 13, 14.6, "black"), (9, 10, 14.2, 12, 13, 14.6, "gold")]),
    dict(id="red_star_monument", fr="Monument à l'étoile rouge", en="Red star monument", elements=[
        (1, 0, 1, 15, 4, 15, "stone"), (3, 4, 3, 13, 8, 13, "ash"), (5, 8, 5, 11, 20, 11, "stone"), (6, 20, 6, 10, 21, 10, "ussr_red"), (4, 21, 7, 12, 24, 9, "ussr_red"), (7, 24, 7, 9, 28, 9, "ussr_red"),
        (3, 24, 7, 7, 26, 9, "ussr_red"), (9, 24, 7, 13, 26, 9, "ussr_red")]),
    dict(id="samovar", fr="Samovar", en="Samovar", elements=[
        (4, 0, 4, 12, 2, 12, "bronze"), (5, 2, 5, 11, 12, 11, "gold"), (4, 12, 4, 12, 14, 12, "bronze"), (6, 14, 6, 10, 17, 10, "gold"), (7, 17, 7, 9, 19, 9, "bronze"), (11, 6, 7, 14, 8, 9, "bronze"),
        (2, 8, 7, 5, 10, 9, "bronze")]),
    dict(id="tile_stove", fr="Poêle en faïence", en="Tiled stove", elements=[
        (1, 0, 1, 15, 28, 15, "white"), (1, 0, 1, 15, 2, 15, "dgray"), (1, 26, 1, 15, 28, 15, "blue"), (5, 3, 0.4, 11, 9, 1, "black"), (6, 4, 0.2, 10, 8, 0.6, "flame"), (1, 12, 0.6, 15, 13, 1, "blue"),
        (1, 18, 0.6, 15, 19, 1, "blue"), (6, 28, 6, 10, 32, 10, "steel")]),
    dict(id="kvas_tank", fr="Citerne à kvas", en="Kvas tank", elements=[
        (1, 0, 4, 15, 3, 12, "dgray"), (1, 3, 5, 15, 13, 11, "ussr_red" if False else "usa_blue"), (2, 6, 4.4, 14, 10, 5, "yellow"), (6, 3, 3, 10, 6, 4, "chrome"), (7, 2, 2, 9, 3, 4, "dgray"), (13, 13, 7, 15, 15, 9, "dgray"),
        (3, 14, 6, 6, 15, 10, "white")]),
    dict(id="street_loudspeaker", fr="Haut-parleur de rue", en="Street loudspeaker", elements=[
        (7, 0, 7, 9, 26, 9, "steel"), (5, 0, 5, 11, 2, 11, "dgray"), (2, 22, 5, 14, 28, 11, "dgray"), (3, 23, 4.4, 13, 27, 5, "black"), (5, 24, 4.2, 11, 26, 4.6, "ash"), (14, 24, 7, 16, 26, 9, "steel")]),
    # ====================================================== CITÉS DU DÉSERT
    dict(id="moroccan_lantern", fr="Lanterne marocaine", en="Moroccan lantern", light=14, passable=True, elements=[
        (7, 28, 7, 9, 32, 9, "bronze"), (4, 12, 4, 12, 14, 12, "bronze"), (5, 14, 5, 11, 28, 11, "gold"), (6, 15, 6, 10, 27, 10, "lamp"), (4, 26, 4, 12, 28, 12, "bronze"), (3, 12, 3, 13, 12.5, 13, "adobe")]),
    dict(id="hookah", fr="Narguilé", en="Hookah", elements=[
        (3, 0, 3, 13, 2, 13, "tcotta"), (4, 2, 4, 12, 9, 12, "glass"), (5, 3, 5, 11, 6, 11, "turquoise"), (7, 9, 7, 9, 18, 9, "gold"), (5, 18, 5, 11, 20, 11, "bronze"), (11, 6, 7, 15, 8, 9, "rope"),
        (14, 6, 6, 16, 9, 10, "gold")]),
    dict(id="clay_jars", fr="Jarres d'argile", en="Clay jars", elements=[
        (1, 0, 1, 8, 10, 8, "tcotta"), (2, 10, 2, 7, 13, 7, "adobe"), (3, 13, 3, 6, 15, 6, "tcotta"), (9, 0, 5, 15, 7, 11, "adobe"), (10, 7, 6, 14, 9, 10, "tcotta"), (3, 0, 9, 9, 5, 15, "tcotta"),
        (2, 4, 1.4, 7, 5, 2, "bronze"), (10, 3, 5.4, 14, 4, 6, "bronze")]),
    dict(id="low_divan", fr="Divan bas à coussins", en="Low divan", elements=[
        (0, 0, 2, 16, 5, 16, "dwood"), (0, 5, 2, 16, 8, 16, "fabric_r"), (0, 8, 11, 16, 13, 16, "fabric_r"), (1, 8, 4, 5, 11, 8, "gold"), (6, 8, 5, 10, 11, 9, "turquoise"), (11, 8, 4, 15, 11, 8, "magenta")]),
    dict(id="berber_rug_wall", fr="Tapis berbère mural", en="Berber wall rug", passable=True, elements=[
        (1, 1, 15, 15, 15, 16, "ussr_red" if False else "bloodred"), (2, 2, 14.6, 14, 14, 15, "sand"), (3, 3, 14.2, 13, 13, 14.6, "tcotta"), (5, 5, 13.8, 11, 11, 14.2, "turquoise"), (7, 7, 13.4, 9, 9, 13.8, "gold"),
        (0, 0, 15, 1, 16, 16, "rope"), (15, 0, 15, 16, 16, 16, "rope")]),
    dict(id="spice_stall", fr="Étal d'épices", en="Spice stall", elements=[
        (0, 0, 6, 16, 10, 16, "adobe"), (1, 10, 7, 15, 11, 15, "sand"), (2, 11, 8, 5, 14, 11, "ussr_red" if False else "red"), (6, 11, 8, 9, 14, 11, "orange"), (10, 11, 8, 13, 14, 11, "yellow"),
        (3, 11, 12, 6, 13, 14, "leaf"), (9, 11, 12, 13, 13, 14, "tcotta"), (0, 20, 4, 16, 22, 16, "red")]),
    # ====================================================== VILLES DU MESA (WESTERN)
    dict(id="saloon_doors", fr="Portes battantes de saloon", en="Saloon swing doors", passable=True, elements=[
        (0, 4, 7, 2, 28, 9, "dwood"), (14, 4, 7, 16, 28, 9, "dwood"), (2, 8, 7.4, 8, 24, 8.6, "lwood"), (8, 8, 7.4, 14, 24, 8.6, "lwood"), (2, 24, 7.4, 14, 25, 8.6, "dwood"), (6, 9, 7.2, 7, 22, 8.8, "wood"), (9, 9, 7.2, 10, 22, 8.8, "wood")]),
    dict(id="water_trough", fr="Abreuvoir", en="Water trough", elements=[
        (0, 4, 4, 16, 10, 12, "wood"), (1, 8, 5, 15, 10, 11, "water"), (0, 0, 4, 2, 4, 12, "dwood"), (14, 0, 4, 16, 4, 12, "dwood"), (0, 6, 3.4, 16, 7, 4, "dwood"), (0, 6, 12, 16, 7, 12.6, "dwood")]),
    dict(id="wanted_poster", fr="Avis de recherche « Wanted »", en="Wanted poster", passable=True, elements=[
        (3, 2, 15, 13, 15, 16, "wax"), (4, 3, 14.6, 12, 14, 15, "cream"), (5, 8, 14.2, 11, 13, 14.6, "leather"), (6, 6, 14.2, 10, 8, 14.6, "black"), (4, 3, 14.2, 12, 5, 14.6, "bloodred")]),
    dict(id="telegraph_desk", fr="Poste de télégraphe", en="Telegraph desk", elements=[
        (0, 10, 3, 16, 12, 13, "wood"), (1, 0, 3, 3, 10, 5, "dwood"), (13, 0, 3, 15, 10, 5, "dwood"), (1, 0, 11, 3, 10, 13, "dwood"), (13, 0, 11, 15, 10, 13, "dwood"), (3, 12, 5, 11, 14, 11, "bronze"),
        (5, 14, 7, 6, 17, 8, "gold"), (11, 12, 6, 14, 15, 9, "black")]),
    dict(id="wagon_wheel", fr="Roue de chariot", en="Wagon wheel", elements=[
        (1, 0, 7, 15, 1, 9, "wood"), (1, 14, 7, 15, 15, 9, "wood"), (0, 1, 7, 1, 14, 9, "wood"), (15, 1, 7, 16, 14, 9, "wood"), (7, 0, 7.4, 9, 16, 8.6, "dwood"), (0, 7, 7.4, 16, 9, 8.6, "dwood"),
        (6, 6, 7, 10, 10, 9, "bronze")]),
    dict(id="ranch_pump", fr="Pompe à eau de ranch", en="Ranch water pump", elements=[
        (4, 0, 4, 12, 3, 12, "stone"), (6, 3, 6, 10, 14, 10, "dgray"), (7, 14, 7, 9, 20, 9, "dgray"), (2, 17, 7, 10, 19, 9, "dgray"), (2, 12, 7, 4, 17, 9, "dgray"), (10, 8, 6, 14, 10, 10, "dgray"),
        (13, 4, 7, 15, 8, 9, "dgray")]),
    # ====================================================== BASE MILITAIRE AMÉRICAINE
    dict(id="base_flag_mast", fr="Mât de drapeau de base", en="Base flag mast", elements=[
        (4, 0, 4, 12, 2, 12, "stone"), (7, 2, 7, 9, 32, 9, "steel"), (9, 20, 7.4, 16, 30, 8.6, "usa_blue"), (10, 22, 7.2, 12, 24, 8.8, "white"), (13, 22, 7.2, 15, 24, 8.8, "white"), (11, 26, 7.2, 13, 28, 8.8, "white")]),
    dict(id="checkpoint_booth", fr="Poste de contrôle d'accès", en="Checkpoint booth", elements=[
        (1, 0, 1, 15, 2, 15, "dgray"), (2, 2, 2, 14, 16, 14, "olive"), (3, 6, 1.4, 13, 13, 2, "glass"), (2, 16, 1, 14, 18, 15, "dgray"), (6, 2, 1.4, 10, 12, 2, "dgray"), (0, 6, 6, 2, 8, 10, "yellow"),
        (7, 18, 7, 9, 21, 9, "red")]),
    dict(id="radar_dish", fr="Radar rotatif", en="Rotating radar", anim="radar", light=3, elements=[
        (4, 0, 4, 12, 6, 12, "dgray"), (6, 6, 6, 10, 10, 10, "steel"), (2, 10, 7, 14, 22, 9, "dgray"), (3, 11, 6.4, 13, 21, 7, "black", {"north": "anim"}), (5, 22, 7, 11, 24, 9, "steel"), (7, 24, 7.4, 9, 26, 8.6, "red")]),
    dict(id="tank_trap", fr="Herse anti-véhicules", en="Tank trap", elements=[
        (0, 0, 7, 16, 2, 9, "gray"), (0, 7, 0, 2, 9, 16, "gray"), (0, 0, 7, 2, 16, 9, "stone"), (14, 0, 7, 16, 16, 9, "stone"), (2, 3, 7, 14, 5, 9, "dgray"), (2, 11, 7, 14, 13, 9, "dgray"), (7, 5, 3, 9, 11, 13, "stone")]),
    dict(id="mess_table", fr="Table de mess et plateaux", en="Mess table with trays", elements=[
        (0, 10, 2, 16, 12, 14, "gray"), (1, 0, 3, 3, 10, 5, "dgray"), (13, 0, 3, 15, 10, 5, "dgray"), (1, 0, 11, 3, 10, 13, "dgray"), (13, 0, 11, 15, 10, 13, "dgray"), (2, 12, 4, 7, 13, 8, "olive"),
        (9, 12, 4, 14, 13, 8, "olive"), (3, 13, 5, 6, 14, 7, "cream"), (10, 13, 5, 13, 14, 7, "tan"), (3, 12, 10, 8, 13, 13, "olive")]),
    dict(id="mission_board", fr="Panneau d'affichage des missions", en="Mission board", passable=True, elements=[
        (0, 2, 14.4, 16, 16, 16, "dwood"), (1, 3, 14, 15, 15, 14.4, "leather"), (2, 9, 13.6, 7, 14, 14, "wax"), (9, 8, 13.6, 14, 14, 14, "cream"), (3, 4, 13.6, 8, 8, 14, "wax"), (10, 4, 13.6, 13, 7, 14, "cream"),
        (4, 13, 13.4, 5, 14, 13.8, "red"), (11, 13, 13.4, 12, 14, 13.8, "red")]),
    # ====================================================== BUNKERS SOVIÉTIQUES
    dict(id="red_star_wall", fr="Étoile rouge murale", en="Red star wall plaque", passable=True, elements=[
        (2, 2, 15, 14, 14, 16, "dgray"), (7, 3, 14.6, 9, 13, 15, "ussr_red"), (3, 7, 14.6, 13, 9, 15, "ussr_red"), (5, 5, 14.6, 11, 11, 15, "ussr_red"), (7, 7, 14.2, 9, 9, 14.6, "gold")]),
    dict(id="situation_map", fr="Carte de situation éclairée", en="Lit situation map", light=7, elements=[
        (0, 10, 2, 16, 12, 14, "dgray"), (1, 12, 3, 15, 13, 13, "glass"), (2, 13, 4, 7, 13.4, 9, "leaf"), (9, 13, 5, 14, 13.4, 11, "water"), (4, 13.4, 5, 5, 14, 6, "red"), (11, 13.4, 8, 12, 14, 9, "red"),
        (1, 0, 2, 3, 10, 4, "gray"), (13, 0, 2, 15, 10, 4, "gray"), (1, 0, 12, 3, 10, 14, "gray"), (13, 0, 12, 15, 10, 14, "gray")]),
    dict(id="crank_telephone", fr="Téléphone à manivelle", en="Crank telephone", passable=True, elements=[
        (3, 4, 14, 13, 18, 16, "wood"), (4, 12, 13, 12, 17, 14, "dwood"), (6, 8, 12.6, 10, 11, 14, "bronze"), (2, 9, 12, 4, 15, 14, "black"), (12, 9, 13, 14, 12, 14.6, "gold"), (6, 4, 13.4, 10, 6, 14, "black")]),
    dict(id="bunker_blower", fr="Ventilateur de bunker", en="Bunker blower", elements=[
        (2, 0, 4, 14, 3, 12, "dgray"), (3, 3, 5, 13, 13, 11, "olive"), (4, 5, 4.4, 12, 11, 5, "black"), (6, 13, 6, 10, 15, 10, "dgray"), (7, 15, 7, 9, 20, 9, "steel"), (5, 20, 6, 11, 22, 10, "dgray")]),
    dict(id="iron_bed", fr="Lit de fer", en="Iron bed", elements=[
        (0, 4, 0, 16, 6, 16, "dgray"), (1, 6, 1, 15, 9, 15, "white"), (3, 9, 1, 13, 10, 5, "cream"), (0, 0, 0, 2, 4, 2, "dgray"), (14, 0, 0, 16, 4, 2, "dgray"), (0, 0, 14, 2, 4, 16, "dgray"),
        (14, 0, 14, 16, 4, 16, "dgray"), (0, 6, 0, 16, 14, 1.4, "dgray"), (1, 6, 14.6, 15, 10, 16, "dgray")]),
    dict(id="wall_safe", fr="Coffre-fort mural", en="Wall safe", passable=True, elements=[
        (2, 2, 13, 14, 14, 16, "dgray"), (3, 3, 12.4, 13, 13, 13, "gray"), (9, 6, 12, 12, 10, 12.4, "steel"), (10, 7, 11.6, 11, 9, 12, "black"), (4, 4, 12, 6, 12, 12.4, "dgray"), (3, 3, 12, 4, 13, 12.4, "steel")]),
    # ====================================================== COMMISSARIAT, SWAT, PRISON
    dict(id="security_camera", fr="Caméra de surveillance rotative", en="Rotating security camera", anim="blink", passable=True, elements=[
        (6, 12, 12, 10, 16, 16, "dgray"), (7, 10, 6, 9, 14, 14, "dgray"), (5, 6, 1, 11, 11, 9, "black"), (6, 7, 0.4, 10, 10, 1, "black", {"north": "anim"}), (4, 9, 3, 5, 10, 5, "red")]),
    dict(id="armored_reception", fr="Comptoir d'accueil blindé", en="Armored reception desk", elements=[
        (0, 0, 4, 16, 12, 14, "blue" if False else "navy_d"), (0, 12, 3, 16, 14, 15, "dgray"), (1, 14, 7, 15, 30, 8, "glass"), (0, 14, 7, 1, 30, 8, "dgray"), (15, 14, 7, 16, 30, 8, "dgray"), (0, 30, 7, 16, 31, 8, "dgray"),
        (6, 14, 4, 10, 15, 7, "cream"), (2, 2, 3.4, 14, 10, 4, "steel")]),
    dict(id="evidence_lockers", fr="Casiers de preuves", en="Evidence lockers", elements=[
        (0, 0, 3, 16, 30, 15, "gray"), (1, 17, 2.4, 7, 29, 3, "dgray"), (9, 17, 2.4, 15, 29, 3, "dgray"), (1, 2, 2.4, 7, 15, 3, "dgray"), (9, 2, 2.4, 15, 15, 3, "dgray"), (6, 22, 2, 7, 24, 2.4, "yellow"), (9, 22, 2, 10, 24, 2.4, "yellow"),
        (6, 8, 2, 7, 10, 2.4, "yellow"), (9, 8, 2, 10, 10, 2.4, "yellow")]),
    dict(id="most_wanted_board", fr="Tableau des personnes recherchées", en="Most wanted board", passable=True, elements=[
        (0, 1, 14.4, 16, 16, 16, "plank_dark"), (1, 2, 14, 15, 15, 14.4, "cream"), (2, 8, 13.6, 6, 14, 14, "wax"), (7, 8, 13.6, 11, 14, 14, "wax"), (12, 8, 13.6, 14, 14, 14, "wax"), (2, 3, 13.6, 8, 7, 14, "wax"), (9, 3, 13.6, 14, 7, 14, "wax"),
        (3, 10, 13.4, 5, 12, 13.8, "black"), (8, 10, 13.4, 10, 12, 13.8, "black")]),
    dict(id="police_radio_desk", fr="Poste radio de police", en="Police radio desk", anim="blink", light=4, elements=[
        (0, 10, 3, 16, 12, 13, "dgray"), (1, 0, 3, 3, 10, 5, "dgray"), (13, 0, 3, 15, 10, 5, "dgray"), (1, 0, 11, 3, 10, 13, "dgray"), (13, 0, 11, 15, 10, 13, "dgray"), (2, 12, 5, 9, 18, 11, "black"),
        (3, 13, 4.4, 8, 17, 5, "black", {"north": "anim"}), (11, 12, 6, 14, 15, 9, "navy_d"), (12, 15, 7, 13, 21, 8, "steel")]),
    dict(id="roof_beacon", fr="Gyrophare de toit", en="Roof beacon", anim="blink", light=12, elements=[
        (4, 0, 4, 12, 2, 12, "dgray"), (5, 2, 5, 11, 4, 11, "steel"), (5, 4, 5, 8, 9, 11, "red"), (8, 4, 5, 11, 9, 11, "blue"), (5, 4, 4.6, 11, 9, 5, "black", {"north": "anim"}), (6, 9, 6, 10, 10, 10, "white")]),
    # ====================================================== CAMPS (FORÊT, ARCTIQUE)
    dict(id="campfire_pot", fr="Feu de camp avec marmite", en="Campfire with pot", anim="fire", light=15, elements=[
        (3, 0, 3, 13, 2, 13, "stone"), (5, 2, 5, 11, 8, 11, "black", {"up": "anim", "north": "anim", "south": "anim", "east": "anim", "west": "anim"}), (3, 8, 7, 5, 14, 9, "dwood"), (11, 8, 7, 13, 14, 9, "dwood"),
        (3, 13, 7, 13, 14, 9, "dwood"), (6, 6, 6, 10, 11, 10, "dgray"), (6, 11, 6, 10, 12, 10, "steel")]),
    dict(id="camp_tent", fr="Tente de camp", en="Camp tent", elements=[
        (0, 0, 1, 16, 2, 15, "dgray"), (1, 2, 2, 15, 6, 14, "khaki"), (3, 6, 3, 13, 10, 13, "khaki"), (5, 10, 4, 11, 13, 12, "khaki"), (7, 13, 5, 9, 15, 11, "olive"), (6, 2, 0.8, 10, 8, 1.4, "black"),
        (0, 0, 7, 1, 14, 9, "wood"), (15, 0, 7, 16, 14, 9, "wood")]),
    dict(id="hammock", fr="Hamac", en="Hammock", elements=[
        (0, 0, 7, 2, 16, 9, "wood"), (14, 0, 7, 16, 16, 9, "wood"), (2, 6, 5, 14, 8, 11, "fabric_r"), (2, 4, 6, 4, 6, 10, "fabric_r"), (12, 4, 6, 14, 6, 10, "fabric_r"), (2, 14, 7.4, 4, 15, 8.6, "rope"),
        (12, 14, 7.4, 14, 15, 8.6, "rope")]),
    dict(id="gas_stove", fr="Réchaud à gaz", en="Gas camping stove", elements=[
        (3, 0, 4, 13, 6, 12, "olive"), (3, 6, 4, 13, 7, 12, "dgray"), (5, 7, 6, 11, 9, 10, "steel"), (4, 9, 5, 12, 10, 11, "dgray"), (13, 0, 6, 16, 8, 10, "gray"), (14, 8, 7, 15, 10, 9, "red"),
        (6, 7, 6, 10, 8, 10, "flame")]),
    dict(id="dog_sled", fr="Traîneau à chiens", en="Dog sled", elements=[
        (0, 0, 3, 16, 1, 5, "wood"), (0, 0, 11, 16, 1, 13, "wood"), (1, 1, 3, 15, 4, 13, "lwood"), (2, 4, 4, 14, 7, 12, "fabric_b"), (13, 4, 4, 15, 14, 6, "wood"), (13, 4, 10, 15, 14, 12, "wood"), (13, 13, 4, 15, 14, 12, "wood"),
        (0, 1, 3, 2, 4, 5, "lwood"), (0, 1, 11, 2, 4, 13, "lwood")]),
    dict(id="jerrycan_stack", fr="Jerricans empilés", en="Jerrycan stack", elements=[
        (1, 0, 2, 7, 10, 8, "olive"), (9, 0, 3, 15, 10, 9, "olive"), (4, 10, 3, 10, 20, 9, "khaki"), (2, 10, 3, 4, 11, 5, "dgray"), (11, 10, 4, 13, 11, 6, "dgray"), (5, 20, 4, 8, 22, 6, "dgray"),
        (1, 6, 1.4, 7, 7, 2, "yellow"), (9, 6, 2.4, 15, 7, 3, "yellow")]),
    # ====================================================== CARTEL
    dict(id="cash_counting_table", fr="Table de comptage de billets", en="Cash counting table", elements=[
        (0, 10, 2, 16, 12, 14, "leather"), (1, 0, 3, 3, 10, 5, "dwood"), (13, 0, 3, 15, 10, 5, "dwood"), (1, 0, 11, 3, 10, 13, "dwood"), (13, 0, 11, 15, 10, 13, "dwood"), (2, 12, 4, 6, 15, 8, "green"),
        (7, 12, 5, 11, 14, 9, "green"), (3, 12, 9, 8, 13, 12, "dgreen"), (11, 12, 4, 14, 15, 8, "dgray")]),
    dict(id="still_alembic", fr="Alambic de laboratoire", en="Lab still", anim="alembic", light=3, elements=[
        (1, 0, 4, 15, 3, 12, "dgray"), (2, 3, 5, 8, 11, 11, "chrome"), (3, 11, 6, 7, 14, 10, "chrome"), (4, 14, 7, 6, 17, 9, "bronze"), (6, 16, 7.4, 14, 17.4, 8.6, "bronze"), (11, 3, 5, 15, 12, 11, "glass"),
        (12, 4, 4.4, 14, 11, 5, "emerald", {"north": "anim"}), (11, 12, 5, 15, 13, 11, "steel")]),
    dict(id="money_trunks", fr="Malles de billets", en="Money trunks", elements=[
        (0, 0, 2, 8, 7, 10, "leather"), (8, 0, 5, 16, 6, 13, "leather"), (1, 7, 3, 7, 10, 9, "gold"), (0, 7, 2, 8, 8, 10, "bronze"), (3, 2, 1.4, 5, 4, 2, "gold"), (11, 2, 4.4, 13, 4, 5, "gold"), (1, 8, 4, 7, 11, 8, "green")]),
    dict(id="villa_bar", fr="Bar de villa avec bouteilles", en="Villa bar", elements=[
        (0, 0, 4, 16, 14, 14, "plank_dark"), (0, 14, 3, 16, 16, 15, "bronze"), (1, 16, 8, 3, 22, 10, "emerald"), (4, 16, 8, 6, 23, 10, "red"), (7, 16, 8, 9, 21, 10, "gold"), (10, 16, 8, 12, 22, 10, "glass"),
        (13, 16, 8, 15, 20, 10, "orange"), (1, 24, 14, 15, 26, 16, "bronze"), (2, 26, 14.4, 14, 30, 15, "plank_dark")]),
    dict(id="bull_head_trophy", fr="Tête de taureau en trophée", en="Bull head trophy", passable=True, elements=[
        (3, 2, 14, 13, 14, 16, "plank_dark"), (4, 4, 11, 12, 11, 14, "leather"), (5, 3, 8, 11, 8, 11, "leather"), (6, 5, 6, 10, 8, 8, "bone"), (3, 7, 11, 5, 14, 12.6, "bone"), (11, 7, 11, 13, 14, 12.6, "bone"),
        (5, 8, 8, 6, 9, 9, "black"), (10, 8, 8, 11, 9, 9, "black")]),
    dict(id="chandelier", fr="Lustre lumineux", en="Chandelier", light=15, passable=True, elements=[
        (7, 26, 7, 9, 32, 9, "gold"), (3, 22, 3, 13, 26, 13, "gold"), (1, 20, 7, 15, 22, 9, "gold"), (7, 20, 1, 9, 22, 15, "gold"), (1, 22, 7, 3, 25, 9, "lamp"), (13, 22, 7, 15, 25, 9, "lamp"), (7, 22, 1, 9, 25, 3, "lamp"),
        (7, 22, 13, 9, 25, 15, "lamp"), (5, 22, 5, 11, 24, 11, "lamp")]),
    # ====================================================== GANGS ET RUES
    dict(id="graffiti_wall", fr="Graffiti mural", en="Wall graffiti", passable=True, elements=[
        (0, 0, 15, 16, 16, 16, "gray"), (1, 8, 14.6, 8, 15, 15, "magenta"), (3, 4, 14.2, 11, 11, 14.6, "neon_blue"), (6, 1, 14.2, 14, 8, 14.6, "yellow"), (2, 11, 14.2, 5, 14, 14.6, "black"), (9, 9, 14.2, 14, 13, 14.6, "red"),
        (4, 6, 13.8, 7, 8, 14.2, "white"), (10, 3, 13.8, 12, 6, 14.2, "emerald")]),
    dict(id="burning_barrel", fr="Baril de feu", en="Burning barrel", anim="fire", light=14, elements=[
        (3, 0, 3, 13, 12, 13, "rust"), (3, 4, 3, 13, 5, 13, "dgray"), (3, 8, 3, 13, 9, 13, "dgray"), (4, 12, 4, 12, 15, 12, "black", {"up": "anim", "north": "anim", "south": "anim", "east": "anim", "west": "anim"}),
        (4, 15, 4, 12, 19, 12, "flame")]),
    dict(id="abandoned_couch", fr="Canapé abandonné", en="Abandoned couch", elements=[
        (0, 1, 3, 16, 5, 16, "ash"), (0, 5, 3, 16, 9, 14, "fabric_g"), (0, 5, 11, 16, 15, 16, "fabric_g"), (0, 5, 3, 2, 11, 14, "fabric_g"), (14, 5, 3, 16, 11, 14, "fabric_g"), (4, 9, 4, 8, 10, 8, "soil"), (10, 15, 12, 14, 16, 15, "cardboard")]),
    dict(id="overturned_cart", fr="Caddie renversé", en="Overturned shopping cart", elements=[
        (2, 2, 2, 14, 3, 14, "chrome"), (2, 3, 2, 3, 10, 14, "steel"), (13, 3, 2, 14, 10, 14, "steel"), (2, 3, 2, 14, 10, 3, "steel"), (2, 9, 13, 14, 10, 14, "red"), (12, 0, 2, 14, 2, 4, "wheel"), (12, 0, 12, 14, 2, 14, "wheel"),
        (3, 3, 5, 6, 5, 8, "cardboard")]),
    dict(id="clothesline", fr="Fil à linge", en="Clothesline", passable=True, elements=[
        (0, 0, 7, 2, 26, 9, "dwood"), (14, 0, 7, 16, 26, 9, "dwood"), (2, 24, 7.6, 14, 25, 8.4, "rope"), (3, 14, 7.4, 6, 24, 8.6, "white"), (7, 16, 7.4, 10, 24, 8.6, "red"), (11, 12, 7.4, 13, 24, 8.6, "blue"),
        (4, 22, 6.8, 5, 24, 7.4, "wood")]),
    dict(id="tire_stack", fr="Pneus empilés", en="Tire stack", elements=[
        (2, 0, 2, 14, 4, 14, "rubber" if False else "wheel"), (3, 4, 3, 13, 8, 13, "wheel"), (2, 8, 2, 14, 12, 14, "wheel"), (4, 0.4, 4, 12, 3.6, 12, "dgray"), (5, 4.4, 5, 11, 7.6, 11, "dgray"), (4, 8.4, 4, 12, 11.6, 12, "dgray"),
        (14, 0, 6, 16, 4, 10, "wheel")]),
    # ====================================================== REBELLES ET MILICE
    dict(id="plank_barricade", fr="Barricade de planches", en="Plank barricade", elements=[
        (0, 0, 6, 16, 3, 10, "wood"), (0, 3, 6, 16, 6, 10, "plank_dark"), (0, 6, 6.4, 16, 9, 9.6, "wood"), (1, 9, 6, 7, 12, 10, "dwood"), (9, 9, 6.2, 15, 13, 9.8, "wood"), (0, 0, 5.4, 16, 1, 6, "dgray"),
        (6, 12, 7, 10, 18, 8, "steel")]),
    dict(id="faction_banner", fr="Bannière de faction murale", en="Faction wall banner", passable=True, elements=[
        (2, 4, 15, 14, 30, 16, "red"), (2, 30, 14.6, 14, 31, 16, "bronze"), (3, 3, 14.6, 5, 4, 15, "gold"), (5, 20, 14.4, 11, 26, 15, "black"), (7, 14, 14.4, 9, 20, 15, "black"), (4, 8, 14.4, 12, 12, 15, "gold"),
        (7, 4, 14.4, 9, 8, 15, "black")]),
    dict(id="improvised_antenna", fr="Antenne radio de fortune", en="Improvised radio antenna", elements=[
        (4, 0, 4, 12, 2, 12, "plank_dark"), (7, 2, 7, 9, 30, 9, "steel"), (3, 12, 7.4, 13, 13, 8.6, "steel"), (4, 18, 7.4, 12, 19, 8.6, "steel"), (5, 24, 7.4, 11, 25, 8.6, "steel"), (6, 30, 7, 10, 32, 9, "red"),
        (9, 0, 3, 14, 5, 7, "olive")]),
    dict(id="rebel_map_table", fr="Table de cartes rebelles", en="Rebel map table", elements=[
        (0, 10, 2, 16, 12, 14, "wood"), (1, 0, 3, 3, 10, 5, "dwood"), (13, 0, 3, 15, 10, 5, "dwood"), (1, 0, 11, 3, 10, 13, "dwood"), (13, 0, 11, 15, 10, 13, "dwood"), (2, 12, 3, 14, 12.6, 13, "cream"),
        (4, 12.6, 5, 7, 13, 8, "leaf"), (9, 12.6, 8, 12, 13, 11, "water"), (5, 12.6, 6, 6, 14, 7, "red"), (11, 12.6, 9, 12, 14, 10, "red")]),
    dict(id="torn_flag", fr="Drapeau déchiré", en="Torn flag", passable=True, elements=[
        (7, 0, 7, 9, 30, 9, "dwood"), (9, 18, 7.4, 16, 28, 8.6, "ussr_red" if False else "bloodred"), (9, 18, 7.4, 12, 22, 8.6, "dgray"), (13, 18, 7.4, 16, 20, 8.6, "black"), (14, 24, 7.4, 16, 26, 8.6, "wood"), (11, 25, 7.4, 13, 28, 8.6, "black")]),
    dict(id="powder_keg", fr="Tonneau de poudre", en="Powder keg", elements=[
        (2, 0, 2, 12, 12, 12, "wood"), (2, 2, 2, 12, 3, 12, "bronze"), (2, 8, 2, 12, 9, 12, "bronze"), (4, 12, 4, 10, 13, 10, "plank_dark"), (13, 0, 5, 16, 3, 11, "black"), (7, 12, 6, 8, 14, 7, "red"), (12, 3, 8, 14, 4, 12, "tan")]),
    # ====================================================== RAFFINERIE, MINE, PÉTROLE
    dict(id="gas_flare", fr="Torchère", en="Gas flare", anim="fire", light=15, elements=[
        (4, 0, 4, 12, 2, 12, "dgray"), (7, 2, 7, 9, 26, 9, "steel"), (5, 14, 5, 11, 16, 11, "dgray"), (6, 26, 6, 10, 28, 10, "dgray"), (5, 28, 5, 11, 32, 11, "black", {"up": "anim", "north": "anim", "south": "anim", "east": "anim", "west": "anim"}),
        (3, 8, 7.4, 13, 9, 8.6, "steel")]),
    dict(id="mine_cart", fr="Wagonnet de mine", en="Mine cart", elements=[
        (0, 0, 3, 16, 1, 5, "dgray"), (0, 0, 11, 16, 1, 13, "dgray"), (1, 2, 2, 15, 3, 14, "dgray"), (1, 3, 2, 15, 9, 3, "rust"), (1, 3, 13, 15, 9, 14, "rust"), (1, 3, 2, 2, 9, 14, "rust"), (14, 3, 2, 15, 9, 14, "rust"),
        (3, 3, 4, 13, 7, 12, "ash"), (3, 0, 2, 5, 3, 4, "wheel"), (11, 0, 12, 13, 3, 14, "wheel")]),
    dict(id="pumpjack", fr="Pompe à balancier", en="Pumpjack", elements=[
        (2, 0, 2, 14, 2, 14, "dgray"), (6, 2, 6, 10, 14, 10, "steel"), (0, 12, 6.6, 16, 16, 9.4, "usa_blue"), (0, 14, 6, 3, 20, 10, "dgray"), (12, 10, 7, 14, 12, 9, "steel"), (13, 2, 7, 14, 10, 9, "chrome"), (1, 4, 6, 5, 10, 10, "dgray"),
        (7, 14, 6.4, 9, 19, 9.6, "steel")]),
    dict(id="giant_valve", fr="Grande vanne", en="Giant valve", elements=[
        (0, 5, 5, 16, 11, 11, "steel"), (5, 3, 4, 11, 13, 12, "dgray"), (7, 13, 7, 9, 18, 9, "steel"), (3, 18, 3, 13, 20, 13, "red"), (7, 18, 3, 9, 20, 13, "red"), (3, 18, 7, 13, 20, 9, "red"), (0, 4, 4, 2, 12, 12, "dgray"),
        (14, 4, 4, 16, 12, 12, "dgray")]),
    dict(id="miner_locker", fr="Casier de mineur avec casque", en="Miner locker with helmet", elements=[
        (1, 0, 3, 15, 30, 15, "dgray"), (2, 2, 2.4, 14, 17, 3, "gray"), (2, 19, 2.4, 14, 29, 3, "gray"), (6, 8, 2, 7, 10, 2.4, "yellow"), (9, 24, 2, 11, 26, 2.4, "yellow"), (4, 29, 4, 12, 32, 12, "yellow"),
        (6, 30, 3.6, 10, 31.4, 4, "lamp")]),
    dict(id="drill_pipe", fr="Tuyaux de forage", en="Drill pipes", elements=[
        (0, 0, 1, 16, 3, 5, "ash"), (0, 0, 6, 16, 3, 10, "ash"), (0, 0, 11, 16, 3, 15, "ash"), (1, 3, 3, 15, 6, 7, "steel"), (1, 3, 9, 15, 6, 13, "steel"), (3, 6, 6, 13, 9, 10, "ash"), (0, 0, 1, 2, 3, 5, "yellow"),
        (14, 0, 6, 16, 3, 10, "yellow")]),
    # ====================================================== PORTS ET MARINE
    dict(id="buoy", fr="Bouée", en="Buoy", elements=[
        (4, 0, 4, 12, 3, 12, "red"), (5, 3, 5, 11, 7, 11, "white"), (4, 7, 4, 12, 10, 12, "red"), (7, 10, 7, 9, 18, 9, "dgray"), (6, 18, 6, 10, 20, 10, "red"), (7, 20, 7, 9, 21, 9, "lamp")]),
    dict(id="bollard_rope", fr="Bollard et cordage", en="Bollard with rope", elements=[
        (4, 0, 4, 12, 2, 12, "dgray"), (5, 2, 5, 11, 9, 11, "black"), (4, 9, 4, 12, 11, 12, "black"), (3, 3, 3, 13, 5, 13, "rope"), (3, 6, 3, 13, 8, 13, "rope"), (12, 2, 6, 16, 4, 10, "rope"), (0, 0, 7, 4, 2, 9, "rope")]),
    dict(id="fishing_net", fr="Filet de pêche", en="Fishing net", passable=True, elements=[
        (0, 0, 7, 16, 16, 8.4, "rope"), (1, 1, 7.4, 15, 15, 8, "turquoise"), (0, 0, 6.6, 1, 16, 9, "dwood"), (15, 0, 6.6, 16, 16, 9, "dwood"), (6, 6, 6.4, 10, 10, 7, "orange"), (2, 10, 6.4, 4, 13, 7, "usa_red")]),
    dict(id="ship_wheel", fr="Gouvernail décoratif", en="Decorative ship's wheel", passable=True, elements=[
        (7, 2, 7, 9, 14, 9, "wood"), (2, 7, 7, 14, 9, 9, "wood"), (4, 3, 7, 12, 13, 9, "dwood"), (5, 4, 6.8, 11, 12, 9.2, "wood"), (7, 7, 6.4, 9, 9, 9.6, "gold"), (1, 6, 7.2, 3, 10, 8.8, "gold"), (13, 6, 7.2, 15, 10, 8.8, "gold"),
        (6, 0, 7.4, 10, 2, 8.6, "dwood")]),
    dict(id="quay_crane", fr="Grue de quai", en="Quay crane", elements=[
        (2, 0, 2, 14, 2, 14, "dgray"), (6, 2, 6, 10, 20, 10, "orange"), (2, 20, 6, 16, 23, 10, "orange"), (13, 8, 7, 14, 20, 9, "rope"), (11, 5, 6.4, 16, 8, 9.6, "dgray"), (4, 3, 4, 7, 8, 7, "dgray"), (7, 23, 7, 9, 28, 9, "red")]),
    dict(id="stacked_containers", fr="Conteneurs empilés", en="Stacked containers", elements=[
        (0, 0, 1, 16, 8, 15, "usa_blue"), (0, 8, 1, 16, 16, 15, "red"), (0, 16, 1, 16, 24, 15, "green"), (1, 1, 0.6, 15, 7, 1, "cont_rib" if False else "black"), (1, 9, 0.6, 15, 15, 1, "black"), (1, 17, 0.6, 15, 23, 1, "black"),
        (7.4, 0, 0.6, 8.6, 24, 1, "dgray")]),
    dict(id="lighthouse_lamp", fr="Lampe de phare", en="Lighthouse lamp", light=15, elements=[
        (3, 0, 3, 13, 3, 13, "dgray"), (4, 3, 4, 12, 9, 12, "white"), (4, 9, 4, 12, 11, 12, "red"), (3, 11, 3, 13, 13, 13, "dgray"), (4, 13, 4, 12, 19, 12, "lamp"), (3, 19, 3, 13, 21, 13, "dgray"), (6, 21, 6, 10, 24, 10, "red")]),
    # ====================================================== PLAGE, NATURE, CAMPING
    dict(id="deck_chair", fr="Transat", en="Deck chair", elements=[
        (2, 0, 2, 4, 6, 14, "wood"), (12, 0, 2, 14, 6, 14, "wood"), (2, 5, 2, 14, 7, 10, "ussr_red" if False else "usa_red"), (2, 5, 2, 14, 7, 5, "white"), (3, 7, 9, 13, 14, 12, "white"), (3, 7, 9, 13, 9, 12, "usa_red"), (2, 8, 11, 4, 16, 13, "wood"),
        (12, 8, 11, 14, 16, 13, "wood")]),
    dict(id="surfboard_rack", fr="Râtelier de planches de surf", en="Surfboard rack", elements=[
        (0, 0, 6, 2, 14, 10, "wood"), (14, 0, 6, 16, 14, 10, "wood"), (0, 12, 6, 16, 14, 10, "wood"), (3, 2, 6.6, 6, 28, 9.4, "turquoise"), (7, 2, 6.6, 10, 26, 9.4, "yellow"), (11, 2, 6.6, 14, 24, 9.4, "magenta"), (4, 4, 6.2, 5, 26, 6.6, "white")]),
    dict(id="volleyball_net", fr="Filet de volley", en="Volleyball net", passable=True, elements=[
        (0, 0, 7, 2, 28, 9, "wood"), (14, 0, 7, 16, 28, 9, "wood"), (2, 14, 7.6, 14, 28, 8.4, "white"), (2, 14, 7.2, 14, 15, 8.8, "black"), (2, 20, 7.8, 14, 21, 8.2, "white"), (2, 26, 7.2, 14, 28, 8.8, "usa_red"), (0, 0, 6, 16, 1, 10, "sand")]),
    dict(id="canoe", fr="Canoë", en="Canoe", elements=[
        (0, 1, 4, 16, 3, 12, "red"), (0, 3, 4, 16, 6, 5, "red"), (0, 3, 11, 16, 6, 12, "red"), (3, 3, 5, 13, 4, 11, "lwood"), (0, 3, 6, 2, 8, 10, "red"), (14, 3, 6, 16, 8, 10, "red"), (5, 4, 7, 6, 12, 8, "wood"), (11, 4, 8, 12, 12, 9, "wood")]),
    dict(id="pedalo", fr="Pédalo", en="Pedal boat", elements=[
        (1, 0, 2, 15, 4, 14, "white"), (2, 4, 3, 14, 8, 13, "turquoise"), (10, 8, 4, 14, 14, 12, "white"), (2, 8, 4, 8, 11, 12, "yellow"), (13, 11, 3, 15, 15, 5, "white"), (13, 11, 11, 15, 15, 13, "white"), (1, 4, 6, 3, 6, 10, "usa_red"),
        (6, 4, 1, 10, 5, 3, "dgray")]),
    dict(id="igloo_tent", fr="Tente igloo", en="Dome tent", elements=[
        (0, 0, 1, 16, 2, 15, "dgray"), (1, 2, 2, 15, 6, 14, "orange"), (3, 6, 3, 13, 10, 13, "orange"), (5, 10, 5, 11, 12, 11, "orange"), (6, 2, 0.8, 10, 8, 1.4, "black"), (7, 2, 0.6, 9, 7, 1, "dgray"), (1, 2, 7, 2, 6, 9, "yellow")]),
    dict(id="beach_hammock", fr="Hamac de plage", en="Beach hammock", elements=[
        (0, 0, 7, 2, 18, 9, "wood"), (14, 0, 7, 16, 18, 9, "wood"), (2, 8, 5, 14, 10, 11, "fabric_b"), (2, 6, 6, 4, 8, 10, "fabric_b"), (12, 6, 6, 14, 8, 10, "fabric_b"), (2, 16, 7.4, 4, 17, 8.6, "rope"), (12, 16, 7.4, 14, 17, 8.6, "rope"),
        (6, 10, 6, 10, 11, 10, "white")]),
    # ====================================================== BOSS, COFFRES, TRÉSORS
    dict(id="throne", fr="Trône", en="Throne", elements=[
        (2, 0, 3, 14, 5, 14, "bronze"), (3, 5, 4, 13, 9, 13, "bloodred"), (2, 5, 11, 14, 30, 14, "bronze"), (3, 9, 11.4, 13, 28, 12.4, "bloodred"), (1, 5, 4, 3, 13, 13, "gold"), (13, 5, 4, 15, 13, 13, "gold"),
        (5, 30, 11, 11, 32, 14, "gold"), (7, 30, 11.4, 9, 32, 13, "gold")]),
    dict(id="brazier", fr="Brasero", en="Brazier", anim="fire", light=15, elements=[
        (6, 0, 6, 10, 2, 10, "dgray"), (7, 2, 7, 9, 10, 9, "dgray"), (3, 10, 3, 13, 12, 13, "bronze"), (3, 12, 3, 4, 14, 13, "bronze"), (12, 12, 3, 13, 14, 13, "bronze"), (4, 12, 4, 12, 15, 12, "black", {"up": "anim", "north": "anim", "south": "anim", "east": "anim", "west": "anim"}),
        (4, 15, 4, 12, 17, 12, "flame")]),
    dict(id="dragon_statue", fr="Statue de dragon", en="Dragon statue", elements=[
        (1, 0, 1, 15, 3, 15, "obsidian"), (3, 3, 4, 13, 10, 13, "ash"), (5, 10, 5, 11, 20, 11, "ash"), (2, 12, 6, 5, 20, 10, "ash"), (11, 12, 6, 14, 20, 10, "ash"), (6, 20, 2, 10, 24, 7, "ash"), (6, 21, 1, 10, 23, 2, "bloodred"),
        (6, 24, 4, 7, 27, 5, "bone"), (9, 24, 4, 10, 27, 5, "bone"), (7, 3, 12, 9, 6, 16, "ash")]),
    dict(id="treasure_chest_deco", fr="Coffre au trésor décoratif", en="Decorative treasure chest", elements=[
        (1, 0, 3, 15, 8, 13, "dwood"), (1, 8, 3, 15, 11, 13, "wood"), (1, 5, 2.6, 15, 6, 3, "gold"), (7, 6, 2.4, 9, 10, 3, "gold"), (3, 8, 4, 13, 11, 12, "gold"), (5, 11, 5, 8, 12, 8, "emerald"), (9, 11, 7, 12, 12, 10, "red"),
        (1, 0, 3, 3, 8, 5, "gold")]),
    dict(id="banner_stand", fr="Porte-bannières de faction", en="Faction banner stand", elements=[
        (2, 0, 6, 14, 2, 10, "dgray"), (3, 2, 7.4, 5, 30, 8.6, "steel"), (11, 2, 7.4, 13, 30, 8.6, "steel"), (3, 10, 6.8, 6, 28, 9.2, "bloodred"), (10, 10, 6.8, 13, 28, 9.2, "usa_blue"), (3, 30, 7, 5, 32, 9, "gold"), (11, 30, 7, 13, 32, 9, "gold"),
        (4, 18, 6.6, 5, 24, 9.4, "gold")]),
    dict(id="rune_pillar", fr="Pilier runique lumineux", en="Glowing rune pillar", light=13, elements=[
        (2, 0, 2, 14, 3, 14, "obsidian"), (4, 3, 4, 12, 30, 12, "ash"), (3, 30, 3, 13, 32, 13, "obsidian"), (5, 6, 3.6, 11, 8, 4, "neon_blue"), (6, 12, 3.6, 10, 14, 4, "neon_blue"), (5, 18, 3.6, 11, 20, 4, "neon_blue"), (7, 24, 3.6, 9, 28, 4, "neon_blue"),
        (3.6, 8, 6, 4, 22, 10, "neon_blue")]),
    dict(id="throne_chandelier", fr="Lustre de salle du trône", en="Throne room chandelier", light=15, passable=True, elements=[
        (7, 26, 7, 9, 32, 9, "bronze"), (0, 22, 7, 16, 24, 9, "gold"), (7, 22, 0, 9, 24, 16, "gold"), (2, 20, 2, 14, 22, 14, "gold"), (0, 24, 7, 2, 28, 9, "wax"), (14, 24, 7, 16, 28, 9, "wax"), (7, 24, 0, 9, 28, 2, "wax"),
        (7, 24, 14, 9, 28, 16, "wax"), (0.5, 28, 7.5, 1.5, 29, 8.5, "flame"), (14.5, 28, 7.5, 15.5, 29, 8.5, "flame")]),
    # ====================================================== FÊTES ET LOISIRS
    dict(id="string_lights", fr="Guirlandes lumineuses", en="String lights", anim="garland", light=10, passable=True, elements=[
        (0, 12, 7.4, 16, 14, 8.6, "dgray", {"north": "anim", "south": "anim"}), (0, 6, 7, 1, 16, 9, "dwood"), (15, 6, 7, 16, 16, 9, "dwood")]),
    dict(id="christmas_tree", fr="Sapin de Noël", en="Christmas tree", anim="garland", light=9, elements=[
        (6, 0, 6, 10, 3, 10, "wood"), (2, 3, 2, 14, 8, 14, "dleaf"), (4, 8, 4, 12, 13, 12, "leaf"), (5, 13, 5, 11, 18, 11, "dleaf"), (6, 18, 6, 10, 22, 10, "leaf"), (7, 22, 7, 9, 24, 9, "gold"),
        (2, 6, 1.6, 14, 7, 2.4, "usa_red"), (4, 12, 3.6, 12, 13, 4.4, "usa_red"), (5, 17, 4.6, 11, 18, 5.4, "gold")]),
    dict(id="jack_o_lantern", fr="Citrouille d'Halloween", en="Jack-o'-lantern", light=12, elements=[
        (2, 0, 2, 14, 10, 14, "orange"), (4, 10, 4, 12, 11, 12, "orange"), (7, 11, 7, 9, 13, 9, "leaf"), (4, 4, 1.6, 7, 7, 2, "lamp"), (9, 4, 1.6, 12, 7, 2, "lamp"), (5, 1.5, 1.6, 11, 3, 2, "lamp"), (7, 3, 1.6, 9, 4, 2, "black")]),
    dict(id="disco_ball", fr="Boule à facettes", en="Disco ball", anim="disco", light=11, passable=True, elements=[
        (7.6, 26, 7.6, 8.4, 32, 8.4, "chrome"), (3, 16, 3, 13, 26, 13, "chrome", {"north": "anim", "south": "anim", "east": "anim", "west": "anim", "up": "anim"}), (5, 14, 5, 11, 16, 11, "chrome")]),
    dict(id="arcade_cabinet", fr="Borne d'arcade", en="Arcade cabinet", anim="arcade", light=8, elements=[
        (1, 0, 3, 15, 14, 15, "navy_d"), (1, 14, 8, 15, 30, 15, "navy_d"), (2, 18, 7.4, 14, 28, 8, "black", {"north": "anim"}), (2, 14, 3, 14, 16, 9, "neon_pink"), (4, 16, 5, 6, 19, 7, "red"), (9, 16, 5, 11, 17, 7, "black"),
        (1, 28, 6, 15, 31, 15, "neon_blue")]),
    dict(id="foosball_table", fr="Baby-foot", en="Foosball table", elements=[
        (1, 0, 1, 3, 9, 3, "wood"), (13, 0, 1, 15, 9, 3, "wood"), (1, 0, 13, 3, 9, 15, "wood"), (13, 0, 13, 15, 9, 15, "wood"), (0, 9, 0, 16, 12, 16, "wood"), (1, 12, 1, 15, 13, 15, "leaf"), (0, 12, 7, 16, 13, 9, "white"),
        (0, 13, 3, 16, 14, 4, "steel"), (0, 13, 12, 16, 14, 13, "steel"), (3, 13, 2.4, 5, 15, 4.6, "usa_red"), (11, 13, 11.4, 13, 15, 13.6, "usa_blue"), (7.4, 13, 7.4, 8.6, 14, 8.6, "white")]),
    dict(id="billiard_table", fr="Table de billard", en="Billiard table", elements=[
        (1, 0, 1, 3, 9, 3, "dwood"), (13, 0, 1, 15, 9, 3, "dwood"), (1, 0, 13, 3, 9, 15, "dwood"), (13, 0, 13, 15, 9, 15, "dwood"), (0, 9, 0, 16, 13, 16, "dwood"), (1, 13, 1, 15, 14, 15, "dgreen"), (0, 13, 0, 16, 14, 1, "dwood"),
        (0, 13, 15, 16, 14, 16, "dwood"), (4, 14, 6, 6, 15, 8, "red"), (8, 14, 9, 10, 15, 11, "yellow"), (11, 14, 4, 13, 15, 6, "blue"), (6, 14, 11, 8, 15, 13, "white")]),
]

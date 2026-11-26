"""
Quatrième série de décors : 91 objets détaillés pour les bases et cités soviétiques (commandement, bureaucratie, propagande, radio et
espionnage, cantine, casernement, sauna et infirmerie, détente, armurerie, garage, missiles et nucléaire, défense, signalétique et lumières,
usure et guerre, goulag). Plus de boîtes par objet que dans les séries précédentes. Importé par generate_realism_decor.py.
"""
import math
import random

from PIL import Image

PAL4 = {
    "soviet_green": "#5a6a3a", "bakelite": "#3a2418", "med_white": "#e4ece8", "teal": "#3a7a78", "brick": "#8a3a2a", "tin": "#a8aeb4", "frost": "#bfe0ee",
    "tube_glow": "#ffae40", "paper": "#e2d3a8", "kaki_d": "#6a6040", "carbon": "#222428", "steam_w": "#eef2f4",
}


def _f(n):
    return Image.new("RGBA", (16, 16 * n))


def anim_reels():
    """Enregistreur à bandes : deux bobines qui tournent."""
    img = _f(8)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (36, 24, 16, 255))
        for cx in (4.5, 11.5):
            for y in range(16):
                for x in range(16):
                    dx, dy = x - cx, y - 8
                    d = (dx * dx + dy * dy) ** 0.5
                    if d < 3.6:
                        a = math.atan2(dy, dx) + f * math.pi / 4
                        c = (180, 180, 186) if d > 1.2 and abs(math.sin(2 * a)) > 0.4 else (70, 70, 76)
                        img.putpixel((x, y + 16 * f), c + (255,))
        for x in range(4, 12):
            img.putpixel((x, 12 + 16 * f), (120, 80, 50, 255))
    return img, {"frametime": 2}


def anim_steam():
    """Banya : pierres chaudes et vapeur blanche qui monte."""
    img = _f(8)
    rnd = random.Random(14)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (60, 54, 52, 255) if y > 9 else (90, 70, 56, 255))
        for _ in range(9):
            x, y = rnd.randint(0, 15), (rnd.randint(0, 9) - f) % 10
            img.putpixel((x, y + 16 * f), (236, 240, 244, 255))
        for x in range(2, 14, 3):
            img.putpixel((x, 12 + 16 * f), (220, 90, 40, 255) if (x + f) % 2 else (150, 60, 36, 255))
    return img, {"frametime": 3}


def anim_tube():
    """Radio à lampes : cadran éclairé et lampes orange qui pulsent."""
    img = _f(6)
    for f in range(6):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (40, 26, 18, 255))
        for x in range(2, 14):
            img.putpixel((x, 4 + 16 * f), (255, 200, 120, 255))
            img.putpixel((x, 5 + 16 * f), (200, 150, 90, 255))
        img.putpixel((2 + f * 2, 3 + 16 * f), (255, 60, 40, 255))
        for i in range(3):
            g = 150 + 80 * math.sin(f + i)
            for dx in range(2):
                for dy in range(3):
                    img.putpixel((3 + i * 4 + dx, 9 + dy + 16 * f), (255, int(g), 60, 255))
    return img, {"frametime": 4}


ANIMS4 = {"reels": anim_reels, "steam": anim_steam, "tube": anim_tube}


def legs(top, col, x0=1, x1=15, z0=2, z1=14, w=2):
    """Quatre pieds de table (de 0 à `top`)."""
    return [(x0, 0, z0, x0 + w, top, z0 + w, col), (x1 - w, 0, z0, x1, top, z0 + w, col), (x0, 0, z1 - w, x0 + w, top, z1, col), (x1 - w, 0, z1 - w, x1, top, z1, col)]


def table(top=12, col="wood", leg="dwood", x0=0, x1=16, z0=2, z1=14):
    return [(x0, top - 2, z0, x1, top, z1, col)] + legs(top - 2, leg, x0 + 1, x1 - 1, z0, z1)


DECOS4 = [
    # ================================================== BUREAU DU GÉNÉRAL
    dict(id="general_desk", fr="Grand bureau du général", en="General's desk", light=6, elements=table(13, "dwood", "plank_dark", 0, 16, 1, 15) + [
        (0, 13, 1, 16, 14, 15, "leather"), (2, 14, 4, 14, 14.4, 12, "dgreen"), (3, 14.4, 11, 6, 20, 12, "bronze"), (3, 18, 10, 6, 21, 13, "dgreen"), (10, 14.4, 4, 13, 15.2, 8, "paper"),
        (11, 14.4, 9, 14, 17, 11, "bakelite"), (12, 17, 9.4, 13, 17.4, 10.6, "steel"), (5, 3, 1, 11, 11, 2, "plank_dark"), (6, 4, 0.6, 10, 5, 1, "gold"), (6, 7, 0.6, 10, 8, 1, "gold")]),
    dict(id="leader_portrait", fr="Portrait du dirigeant", en="Leader portrait", passable=True, elements=[
        (2, 1, 15, 14, 16, 16, "gold"), (3, 2, 14.6, 13, 15, 15, "dwood"), (4, 3, 14.2, 12, 14, 14.6, "tan"), (6, 10, 14, 10, 13, 14.2, "bone"), (6, 7, 14, 10, 10, 14.2, "carbon"), (5, 5, 14, 11, 7, 14.2, "ussr_red"),
        (7, 8.4, 13.8, 9, 9.2, 14, "black"), (6, 11, 13.8, 7, 12, 14, "black"), (9, 11, 13.8, 10, 12, 14, "black")]),
    dict(id="soviet_flag_fringed", fr="Drapeau soviétique à franges", en="Fringed Soviet flag", elements=[
        (5, 0, 5, 11, 2, 11, "bronze"), (7, 2, 7, 9, 30, 9, "wood"), (7, 30, 7, 9, 32, 9, "gold"), (9, 14, 7.4, 16, 28, 8.6, "ussr_red"), (9.4, 24, 7.2, 12.6, 27, 8.8, "gold"), (9, 14, 7.2, 16, 15, 8.8, "gold"),
        (9, 27, 7.2, 16, 28, 8.8, "gold"), (10, 11, 7.6, 11, 14, 8.4, "gold"), (12.5, 11, 7.6, 13.5, 14, 8.4, "gold"), (15, 11, 7.6, 16, 14, 8.4, "gold")]),
    dict(id="model_table", fr="Table de maquettes tactiques", en="Tactical model table", elements=table(11, "dwood", "plank_dark", 0, 16, 1, 15) + [
        (1, 11, 2, 15, 12, 14, "dgreen"), (2, 12, 3, 7, 13, 8, "leaf"), (9, 12, 4, 14, 12.6, 10, "water"), (3, 12, 9, 6, 15, 12, "tin"), (8, 12, 10, 10, 14, 12, "olive"), (11, 12, 3, 13, 16, 4, "red"),
        (12, 12, 11, 14, 13, 13, "ussr_red"), (6, 12.6, 6, 9, 13, 7, "carbon")]),
    dict(id="teleprinter", fr="Télescripteur", en="Teleprinter", anim="blink", light=4, elements=[
        (2, 0, 3, 14, 3, 13, "dgray"), (3, 3, 4, 13, 10, 12, "carbon"), (4, 6, 3.6, 12, 10, 4, "bakelite", {"north": "anim"}), (4, 10, 5, 12, 14, 11, "dgray"), (5, 14, 6, 11, 16, 10, "paper"), (5, 16, 6, 11, 20, 8, "paper"),
        (3, 3, 4, 5, 6, 6, "steel"), (11, 3, 4, 13, 6, 6, "steel"), (6, 0, 2, 10, 1, 3, "carbon")]),
    dict(id="code_safe", fr="Coffre à codes", en="Code safe", elements=[
        (1, 0, 3, 15, 24, 15, "soviet_green"), (2, 1, 2.4, 14, 23, 3, "dgray"), (3, 15, 2, 8, 20, 2.4, "carbon"), (4, 16, 1.8, 7, 19, 2.2, "tin"), (10, 14, 1.8, 12, 20, 2.4, "steel"), (11, 6, 1.8, 12, 12, 2.2, "steel"),
        (4, 4, 2, 9, 9, 2.4, "carbon"), (5, 5, 1.8, 8, 8, 2.2, "gold"), (1, 0, 3, 15, 2, 15, "dgray")]),
    # ================================================== BUREAUCRATIE ET ARCHIVES
    dict(id="archive_cabinet", fr="Armoire d'archives", en="Archive cabinet", elements=[
        (1, 0, 3, 15, 30, 15, "soviet_green"), (2, 22, 2.4, 14, 29, 3, "dgray"), (2, 14, 2.4, 14, 21, 3, "dgray"), (2, 6, 2.4, 14, 13, 3, "dgray"), (2, 1, 2.4, 14, 5, 3, "dgray"), (5, 24, 2, 11, 25.5, 2.4, "paper"),
        (5, 16, 2, 11, 17.5, 2.4, "paper"), (5, 8, 2, 11, 9.5, 2.4, "paper"), (7, 26, 1.8, 9, 28, 2.4, "steel"), (7, 18, 1.8, 9, 20, 2.4, "steel"), (7, 10, 1.8, 9, 12, 2.4, "steel")]),
    dict(id="typewriter", fr="Machine à écrire", en="Typewriter", elements=[
        (2, 0, 3, 14, 3, 13, "carbon"), (3, 3, 4, 13, 7, 12, "dgray"), (4, 7, 5, 12, 9, 7, "carbon"), (4, 7, 8, 12, 8, 12, "steel"), (5, 8, 8.4, 6, 9, 9.4, "bone"), (7, 8, 8.4, 8, 9, 9.4, "bone"), (9, 8, 8.4, 10, 9, 9.4, "bone"),
        (3, 9, 11, 13, 10, 13, "steel"), (5, 9, 11, 11, 14, 12, "paper"), (13, 4, 6, 15, 8, 10, "dgray"), (1, 5, 6, 3, 7, 10, "dgray")]),
    dict(id="inkwell_stamp", fr="Encrier et tampon", en="Inkwell and stamp", elements=[
        (0, 0, 2, 16, 2, 14, "dwood"), (2, 2, 3, 8, 3, 8, "paper"), (3, 3, 4, 6, 6, 7, "carbon"), (3.5, 6, 4.5, 5.5, 7, 6.5, "gold"), (4.2, 7, 5.2, 4.8, 12, 5.8, "wood"), (10, 2, 3, 14, 3, 7, "ussr_red"), (10.5, 3, 3.5, 13.5, 4, 6.5, "bakelite"),
        (11.5, 4, 4.5, 12.5, 8, 5.5, "bakelite"), (10, 2, 9, 15, 2.6, 13, "paper"), (11, 2.6, 10, 14, 3, 12, "ussr_red")]),
    dict(id="pneumatic_mail", fr="Courrier pneumatique", en="Pneumatic mail", elements=[
        (4, 0, 4, 12, 3, 12, "dgray"), (5, 3, 5, 11, 14, 11, "bronze"), (4, 14, 4, 12, 18, 12, "bronze"), (6, 18, 6, 10, 24, 10, "tin"), (7, 24, 7, 9, 30, 9, "tin"), (3, 6, 6, 5, 10, 10, "steel"),
        (11, 6, 6, 13, 10, 10, "steel"), (6, 8, 4.4, 10, 12, 5, "carbon"), (7, 9, 4.2, 9, 11, 4.6, "red")]),
    dict(id="russian_wall_clock", fr="Horloge murale à cadran", en="Russian wall clock", passable=True, elements=[
        (2, 2, 14, 14, 14, 16, "dwood"), (3, 3, 13.6, 13, 13, 14, "bone"), (7.6, 4, 13.2, 8.4, 8, 13.6, "black"), (7.6, 7.6, 13.2, 11, 8.4, 13.6, "black"), (7.4, 7.4, 13, 8.6, 8.6, 13.4, "gold"), (7.6, 12, 13.2, 8.4, 12.8, 13.6, "black"),
        (7.6, 3.2, 13.2, 8.4, 4, 13.6, "black"), (3.2, 7.6, 13.2, 4, 8.4, 13.6, "black"), (12, 7.6, 13.2, 12.8, 8.4, 13.6, "black"), (7, 0, 14.4, 9, 2, 15.6, "gold")]),
    dict(id="staffing_board", fr="Tableau des effectifs", en="Staffing board", passable=True, elements=[
        (0, 1, 14.4, 16, 16, 16, "dwood"), (1, 2, 14, 15, 15, 14.4, "dgreen"), (2, 12, 13.6, 14, 13, 14, "paper"), (2, 9, 13.6, 14, 10, 14, "paper"), (2, 6, 13.6, 14, 7, 14, "paper"), (2, 3, 13.6, 14, 4, 14, "paper"),
        (3, 12.6, 13.2, 5, 13.4, 13.6, "red"), (7, 9.6, 13.2, 9, 10.4, 13.6, "yellow"), (11, 6.6, 13.2, 13, 7.4, 13.6, "red"), (4, 3.6, 13.2, 6, 4.4, 13.6, "red")]),
    # ================================================== PROPAGANDE ET CULTURE
    dict(id="leader_bust", fr="Buste du dirigeant", en="Leader bust", elements=[
        (2, 0, 2, 14, 8, 14, "stone"), (1, 8, 1, 15, 10, 15, "ash"), (4, 10, 4, 12, 17, 12, "bronze"), (5, 17, 5, 11, 24, 11, "bronze"), (6, 24, 6, 10, 26, 10, "bronze"), (5, 20, 4.4, 11, 22, 5, "gold"), (6, 22, 4.4, 7, 23, 5, "black"),
        (9, 22, 4.4, 10, 23, 5, "black"), (7, 19, 4.4, 9, 20, 5, "carbon"), (5, 1, 1.4, 11, 7, 2, "gold")]),
    dict(id="worker_soldier_statue", fr="Statue de l'ouvrier et du soldat", en="Worker and soldier statue", elements=[
        (0, 0, 0, 16, 4, 16, "stone"), (2, 4, 2, 14, 7, 14, "ash"), (3, 7, 5, 6, 18, 9, "steel"), (10, 7, 5, 13, 18, 9, "steel"), (3, 18, 5, 6, 24, 9, "steel"), (10, 18, 5, 13, 24, 9, "steel"), (4, 24, 5.4, 6, 27, 8.6, "steel"),
        (10, 24, 5.4, 12, 27, 8.6, "steel"), (2, 22, 6, 4, 30, 8, "steel"), (12, 20, 6, 14, 27, 8, "steel"), (6, 28, 6, 10, 30, 10, "ussr_red")]),
    dict(id="slogan_banner", fr="Banderole de slogan", en="Slogan banner", passable=True, elements=[
        (0, 3, 14.6, 16, 13, 15.4, "ussr_red"), (0, 13, 14.2, 16, 14, 15.8, "wood"), (0, 2, 14.2, 16, 3, 15.8, "wood"), (1, 6, 14.2, 3, 10, 14.6, "paper"), (4, 6, 14.2, 6, 10, 14.6, "paper"), (7, 6, 14.2, 9, 10, 14.6, "paper"),
        (10, 6, 14.2, 12, 10, 14.6, "paper"), (13, 6, 14.2, 15, 10, 14.6, "paper"), (7, 9, 14, 9, 12, 14.4, "gold")]),
    dict(id="red_corner", fr="Coin rouge", en="Red corner", elements=[
        (0, 0, 2, 16, 10, 14, "dwood"), (1, 10, 3, 15, 11, 13, "ussr_red"), (4, 11, 6, 12, 28, 8, "plank_dark"), (5, 14, 5.6, 11, 26, 6.4, "paper"), (7, 18, 5.2, 9, 22, 5.6, "ussr_red"), (6, 11, 8, 10, 14, 12, "gold"),
        (2, 12, 5, 4, 24, 9, "ussr_red"), (12, 12, 5, 14, 24, 9, "ussr_red"), (7.4, 24, 5.2, 8.6, 26, 5.6, "gold")]),
    dict(id="medal_case", fr="Vitrine de médailles", en="Medal display case", light=4, elements=[
        (1, 0, 4, 15, 12, 14, "plank_dark"), (1, 12, 4, 15, 22, 14, "glass"), (2, 13, 5, 14, 14, 13, "ussr_red"), (3, 14, 6, 5, 17, 8, "gold"), (6, 14, 6, 8, 17, 8, "tin"), (9, 14, 6, 11, 17, 8, "bronze"),
        (12, 14, 6, 13, 17, 8, "gold"), (4, 18, 6, 12, 21, 8, "ussr_red"), (1, 22, 4, 15, 23, 14, "plank_dark"), (7, 19, 6.4, 9, 21, 7.6, "gold")]),
    dict(id="glory_panel", fr="Panneau « Gloire à l'armée rouge »", en="Glory to the Red Army panel", passable=True, elements=[
        (0, 1, 14.6, 16, 16, 16, "ussr_red"), (1, 2, 14.2, 15, 15, 14.6, "ussr_red"), (6, 9, 14, 10, 14, 14.2, "gold"), (3, 11, 14, 6, 14, 14.2, "gold"), (10, 11, 14, 13, 14, 14.2, "gold"), (3, 3, 14, 13, 5, 14.2, "paper"),
        (3, 6, 14, 13, 8, 14.2, "paper"), (4, 9, 13.8, 5, 10, 14, "gold"), (11, 9, 13.8, 12, 10, 14, "gold")]),
    # ================================================== RADIO, ÉCOUTE, ESPIONNAGE
    dict(id="tube_radio", fr="Radio à lampes", en="Tube radio", anim="tube", light=5, elements=[
        (1, 0, 4, 15, 9, 13, "bakelite"), (2, 1, 3.4, 14, 8, 4, "carbon", {"north": "anim"}), (1, 0, 4, 15, 1, 13, "dwood"), (3, 9, 6, 6, 10, 9, "tube_glow"), (7, 9, 6, 10, 11, 9, "tube_glow"), (11, 9, 6, 13, 10, 9, "tube_glow"),
        (12, 3, 3, 14, 5, 3.4, "gold"), (13, 9, 8, 14, 22, 9, "steel")]),
    dict(id="tape_recorder", fr="Enregistreur à bandes", en="Tape recorder", anim="reels", elements=[
        (1, 0, 3, 15, 10, 13, "dgray"), (2, 4, 2.4, 14, 9, 3, "carbon", {"north": "anim"}), (2, 1, 2.4, 6, 3, 3, "steel"), (7, 1, 2.4, 9, 3, 3, "red"), (10, 1, 2.4, 14, 3, 3, "steel"), (1, 10, 4, 15, 11, 12, "tin"),
        (12, 11, 5, 14, 13, 8, "carbon"), (3, 10, 9, 6, 12, 11, "steel")]),
    dict(id="switchboard", fr="Standard téléphonique à fiches", en="Switchboard", elements=[
        (0, 0, 3, 16, 14, 14, "dwood"), (0, 14, 3, 16, 24, 12, "bakelite"), (1, 15, 11.4, 15, 23, 12, "carbon"), (2, 17, 11, 4, 19, 11.4, "red"), (5, 17, 11, 7, 19, 11.4, "red"), (8, 17, 11, 10, 19, 11.4, "yellow"),
        (11, 17, 11, 13, 19, 11.4, "red"), (2, 20, 11, 4, 22, 11.4, "yellow"), (5, 20, 11, 7, 22, 11.4, "yellow"), (8, 20, 11, 10, 22, 11.4, "red"), (3, 14, 1, 4, 15, 3, "steel"), (7, 14, 1, 8, 15, 3, "steel"),
        (11, 14, 1, 12, 15, 3, "steel"), (13, 14, 4, 15, 16, 6, "tin")]),
    dict(id="listening_table", fr="Table d'écoute", en="Listening table", anim="blink", elements=table(12, "dgray", "carbon", 0, 16, 2, 14) + [
        (2, 12, 4, 9, 15, 11, "olive"), (3, 13, 3.6, 8, 14, 4, "carbon", {"north": "anim"}), (11, 12, 5, 14, 13, 9, "carbon"), (11, 13, 5.5, 14, 14.5, 6.5, "steel"), (3, 15, 6, 8, 16, 8, "steel"),
        (5, 16, 6.5, 6, 21, 7.5, "steel"), (11, 12, 10, 15, 12.4, 13, "paper")]),
    dict(id="frequency_map", fr="Carte des fréquences", en="Frequency map", passable=True, elements=[
        (0, 2, 14.4, 16, 15, 16, "carbon"), (1, 3, 14, 15, 14, 14.4, "teal"), (2, 4, 13.8, 14, 5, 14, "paper"), (2, 7, 13.8, 14, 8, 14, "paper"), (2, 10, 13.8, 14, 11, 14, "paper"), (3, 12, 13.6, 5, 13, 13.8, "red"),
        (7, 9, 13.6, 9, 10, 13.8, "yellow"), (11, 6, 13.6, 13, 7, 13.8, "red"), (5, 4, 13.6, 6, 13, 13.8, "bronze")]),
    dict(id="signal_jammer", fr="Brouilleur de fréquences", en="Signal jammer", anim="blink", light=5, elements=[
        (2, 0, 3, 14, 11, 13, "soviet_green"), (3, 6, 2.4, 13, 10, 3, "carbon", {"north": "anim"}), (3, 1, 2.4, 6, 4, 3, "steel"), (7, 1, 2.4, 9, 4, 3, "red"), (10, 1, 2.4, 13, 4, 3, "steel"), (3, 11, 5, 5, 26, 7, "steel"),
        (11, 11, 5, 13, 24, 7, "steel"), (7, 11, 6, 9, 30, 8, "steel"), (3, 26, 4.5, 5, 27, 7.5, "red"), (6, 11, 4, 10, 13, 12, "tin")]),
    # ================================================== CANTINE ET CUISINE
    dict(id="big_cauldron", fr="Grande marmite", en="Big cauldron", elements=[
        (2, 0, 2, 14, 3, 14, "dgray"), (3, 3, 3, 13, 4, 13, "carbon"), (2, 4, 2, 14, 16, 14, "tin"), (3, 16, 3, 13, 17, 13, "steel"), (4, 16.6, 4, 12, 17, 12, "brick"), (1, 10, 6, 2, 13, 10, "steel"), (14, 10, 6, 15, 13, 10, "steel"),
        (6, 17, 6, 10, 19, 10, "tin"), (5, 4, 1.4, 11, 6, 2, "bakelite"), (3, 12, 1.4, 13, 13, 2, "carbon")]),
    dict(id="ladle_rack", fr="Louches et plateaux", en="Ladle and tray rack", elements=[
        (0, 0, 6, 16, 3, 10, "wood"), (1, 3, 7, 3, 24, 9, "dwood"), (13, 3, 7, 15, 24, 9, "dwood"), (1, 22, 7, 15, 24, 9, "dwood"), (3, 12, 7.4, 4, 22, 8.6, "steel"), (3, 10, 6.8, 6, 12, 9.2, "tin"), (7, 12, 7.4, 8, 22, 8.6, "steel"),
        (7, 10, 6.8, 10, 12, 9.2, "tin"), (11, 12, 7.4, 12, 22, 8.6, "steel"), (11, 10, 6.8, 14, 12, 9.2, "tin"), (2, 3, 6.2, 14, 4, 9.8, "tin")]),
    dict(id="canned_goods_shelf", fr="Étagère de conserves", en="Canned goods shelf", elements=[
        (0, 0, 4, 16, 30, 12, "dwood"), (1, 2, 4.4, 15, 3, 11.6, "wood"), (1, 11, 4.4, 15, 12, 11.6, "wood"), (1, 20, 4.4, 15, 21, 11.6, "wood"), (2, 3, 5, 5, 8, 8, "tin"), (6, 3, 5, 9, 8, 8, "red"), (10, 3, 5, 13, 8, 8, "tin"),
        (2, 12, 5, 5, 17, 8, "olive"), (6, 12, 5, 9, 17, 8, "tin"), (10, 12, 5, 13, 17, 8, "brick"), (3, 21, 5, 7, 27, 9, "paper"), (9, 21, 5, 13, 27, 9, "tin")]),
    dict(id="cabbage_barrel", fr="Fût de choux", en="Cabbage barrel", elements=[
        (2, 0, 2, 14, 14, 14, "wood"), (2, 3, 2, 14, 4, 14, "steel"), (2, 10, 2, 14, 11, 14, "steel"), (3, 14, 3, 13, 15, 13, "leaf"), (4, 15, 4, 12, 16, 12, "dleaf"), (6, 15.6, 6, 10, 16.6, 10, "leaf"), (5, 14, 5, 11, 15, 11, "dwood"),
        (13, 10, 6, 16, 12, 10, "dwood")]),
    dict(id="mess_hall_table", fr="Table de réfectoire", en="Mess hall table", elements=table(11, "lwood", "dwood", 0, 16, 1, 15) + [
        (1, 11, 2, 6, 12, 6, "tin"), (2, 12, 3, 5, 13, 5, "brick"), (8, 11, 2, 13, 12, 6, "tin"), (9, 12, 3, 12, 13, 5, "brick"), (2, 11, 9, 7, 12, 13, "tin"), (3, 12, 10, 6, 13, 12, "brick"), (10, 11, 9, 13, 12, 13, "tin"),
        (0, 4, 0, 16, 6, 2, "wood"), (0, 4, 14, 16, 6, 16, "wood")]),
    dict(id="ration_poster", fr="Affiche de rationnement", en="Ration poster", passable=True, elements=[
        (2, 2, 15, 14, 15, 16, "paper"), (3, 11, 14.6, 13, 14, 15, "ussr_red"), (4, 8, 14.2, 7, 11, 14.6, "tan"), (9, 8, 14.2, 12, 11, 14.6, "tan"), (3, 5, 14.2, 13, 6, 14.6, "carbon"), (3, 3, 14.2, 11, 4, 14.6, "carbon"),
        (7, 8.4, 14, 9, 10.6, 14.4, "bone")]),
    # ================================================== CASERNEMENT
    dict(id="soldier_locker", fr="Armoire de paquetage", en="Soldier's wardrobe", elements=[
        (1, 0, 3, 15, 32, 15, "soviet_green"), (2, 1, 2.4, 8, 31, 3, "olive"), (8, 1, 2.4, 14, 31, 3, "olive"), (6, 14, 2, 7, 18, 2.4, "steel"), (9, 14, 2, 10, 18, 2.4, "steel"), (3, 24, 2, 7, 27, 2.4, "paper"), (9, 24, 2, 13, 25, 2.4, "ussr_red"),
        (1, 0, 3, 15, 2, 15, "dgray"), (2, 28, 2.2, 4, 30, 2.6, "gold")]),
    dict(id="boots_row", fr="Bottes alignées", en="Lined-up boots", elements=[
        (0, 0, 4, 16, 1, 12, "wood"), (1, 1, 5, 4, 8, 9, "carbon"), (1, 1, 9, 4, 3, 11, "carbon"), (6, 1, 5, 9, 8, 9, "carbon"), (6, 1, 9, 9, 3, 11, "carbon"), (11, 1, 5, 14, 8, 9, "carbon"), (11, 1, 9, 14, 3, 11, "carbon"),
        (1, 7, 5.4, 4, 8, 8.6, "leather"), (6, 7, 5.4, 9, 8, 8.6, "leather"), (11, 7, 5.4, 14, 8, 8.6, "leather")]),
    dict(id="ushanka_rack", fr="Patère à chapkas", en="Ushanka rack", passable=True, elements=[
        (0, 12, 14.4, 16, 14, 16, "dwood"), (2, 10, 13, 4, 12, 15, "steel"), (7, 10, 13, 9, 12, 15, "steel"), (12, 10, 13, 14, 12, 15, "steel"), (1, 5, 12, 5, 10, 16, "olive"), (1, 5, 11, 5, 6, 12, "tin"), (6, 5, 12, 10, 10, 16, "kaki_d"),
        (6, 5, 11, 10, 6, 12, "tin"), (11, 5, 12, 15, 10, 16, "olive"), (11, 5, 11, 15, 6, 12, "tin"), (2.4, 10.6, 11.8, 3.6, 11.6, 12.4, "gold")]),
    dict(id="soldier_suitcase", fr="Valise de soldat", en="Soldier's suitcase", elements=[
        (1, 0, 3, 15, 9, 13, "leather"), (1, 9, 3, 15, 10, 13, "dwood"), (1, 4, 2.4, 15, 5, 3, "steel"), (1, 6, 12.6, 15, 7, 13, "steel"), (6, 7, 2.2, 10, 9, 3, "steel"), (7, 5, 2, 9, 6, 2.4, "gold"), (2, 10, 5, 8, 12, 9, "olive"),
        (9, 10, 6, 13, 11, 10, "paper"), (0, 0, 2, 2, 3, 4, "steel"), (14, 0, 2, 16, 3, 4, "steel")]),
    dict(id="duty_roster_board", fr="Tableau de service", en="Duty roster board", passable=True, elements=[
        (0, 1, 14.4, 16, 16, 16, "dwood"), (1, 2, 14, 15, 15, 14.4, "paper"), (1, 12, 13.8, 15, 13, 14, "carbon"), (1, 9, 13.8, 15, 10, 14, "carbon"), (1, 6, 13.8, 15, 7, 14, "carbon"), (1, 3, 13.8, 15, 4, 14, "carbon"),
        (5, 2, 13.8, 6, 15, 14, "carbon"), (10, 2, 13.8, 11, 15, 14, "carbon"), (2, 13.4, 13.4, 4, 14.4, 13.8, "red"), (12, 7.4, 13.4, 14, 8.4, 13.8, "red")]),
    dict(id="barracks_mirror", fr="Miroir de caserne", en="Barracks mirror", passable=True, elements=[
        (3, 1, 14.6, 13, 16, 16, "dwood"), (4, 2, 14, 12, 15, 14.6, "glass"), (4, 2, 13.8, 12, 3, 14.2, "tin"), (3, 0, 14.4, 13, 1, 16, "wood"), (5, 5, 13.6, 6, 12, 14, "frost"), (8, 8, 13.6, 10, 13, 14, "frost")]),
    # ================================================== SAUNA ET INFIRMERIE
    dict(id="banya_stove", fr="Poêle de banya", en="Banya stove", anim="steam", light=6, elements=[
        (1, 0, 1, 15, 3, 15, "dgray"), (2, 3, 2, 14, 12, 14, "carbon"), (3, 12, 3, 13, 18, 13, "dgray"), (4, 12, 4, 12, 13, 12, "carbon", {"up": "anim"}), (5, 13, 5, 11, 16, 11, "ash"), (6, 16, 6, 10, 17, 10, "ash"), (7, 18, 7, 9, 30, 9, "steel"),
        (6, 30, 6, 10, 32, 10, "tin"), (5, 4, 1.4, 11, 8, 2, "flame"), (4, 3, 1.2, 12, 9, 1.8, "carbon")]),
    dict(id="bucket_ladle", fr="Seau et louche de sauna", en="Sauna bucket and ladle", elements=[
        (2, 0, 2, 12, 9, 12, "wood"), (2, 2, 2, 12, 3, 12, "steel"), (2, 7, 2, 12, 8, 12, "steel"), (3, 9, 3, 11, 10, 11, "water"), (4, 9.6, 4, 10, 10, 10, "steam_w"), (11, 8, 6, 15, 9, 8, "wood"), (14, 8, 5, 16, 10, 9, "wood"),
        (11, 9, 6, 12, 14, 8, "wood")]),
    dict(id="uv_lamp", fr="Lampe à ultraviolets", en="UV lamp", light=12, elements=[
        (4, 0, 4, 12, 2, 12, "dgray"), (7, 2, 7, 9, 20, 9, "steel"), (3, 20, 3, 13, 22, 13, "tin"), (4, 22, 4, 12, 26, 12, "neon_blue"), (5, 26, 5, 11, 28, 11, "frost"), (7, 12, 6, 9, 14, 10, "carbon"), (11, 12, 6, 14, 16, 10, "tin"),
        (2, 14, 6, 4, 16, 10, "steel")]),
    dict(id="medical_jars", fr="Bocaux médicaux", en="Medical jars", elements=[
        (0, 0, 4, 16, 3, 12, "dwood"), (0, 11, 4, 16, 12, 12, "dwood"), (1, 3, 5, 5, 11, 11, "glass"), (2, 3, 6, 4, 9, 10, "teal"), (6, 3, 5, 10, 11, 11, "glass"), (7, 3, 6, 9, 7, 10, "paper"), (11, 3, 5, 15, 11, 11, "glass"),
        (12, 3, 6, 14, 10, 10, "bloodred"), (1, 12, 5, 5, 20, 11, "glass"), (2, 12, 6, 4, 18, 10, "frost"), (7, 12, 5, 11, 18, 11, "glass"), (8, 12, 6, 10, 15, 10, "tan"), (12, 12, 5, 15, 17, 11, "tin"), (1, 11, 5, 5, 12, 11, "tin")]),
    dict(id="treatment_table", fr="Table de soins", en="Treatment table", elements=table(13, "med_white", "tin", 0, 16, 1, 15) + [
        (1, 13, 2, 15, 14, 14, "teal"), (2, 14, 3, 14, 15, 13, "med_white"), (11, 15, 4, 14, 17, 8, "tin"), (12, 17, 5, 13, 22, 6, "steel"), (3, 14, 12, 7, 17, 14, "red"), (4, 17, 12.5, 6, 18, 13.5, "med_white"), (1, 4, 1, 15, 5, 15, "tin")]),
    dict(id="soviet_med_cabinet", fr="Armoire à pharmacie soviétique", en="Soviet medicine cabinet", elements=[
        (1, 0, 3, 15, 30, 14, "med_white"), (2, 14, 2.4, 14, 29, 3, "glass"), (2, 1, 2.4, 14, 13, 3, "tin"), (3, 16, 3, 6, 20, 8, "bloodred"), (7, 16, 3, 10, 21, 8, "teal"), (11, 16, 3, 13, 19, 8, "paper"), (3, 23, 3, 6, 27, 8, "tin"),
        (8, 23, 3, 13, 27, 8, "paper"), (6, 6, 2, 10, 10, 2.4, "red"), (7.4, 4.6, 2, 8.6, 11.4, 2.4, "red"), (1, 0, 3, 15, 2, 14, "tin")]),
    # ================================================== DÉTENTE
    dict(id="chess_table", fr="Table d'échecs", en="Chess table", elements=table(11, "dwood", "plank_dark", 2, 14, 2, 14) + [
        (3, 11, 3, 13, 11.6, 13, "bone"), (3, 11.6, 3, 5, 12, 5, "carbon"), (7, 11.6, 3, 9, 12, 5, "carbon"), (5, 11.6, 5, 7, 12, 7, "carbon"), (9, 11.6, 5, 11, 12, 7, "carbon"), (4, 11.6, 11, 6, 14.6, 12, "bone"),
        (9, 11.6, 10, 11, 13.6, 11, "carbon"), (7, 11.6, 8, 8, 13, 9, "bone"), (11, 11.6, 3, 12, 12.8, 4, "carbon")]),
    dict(id="domino_table", fr="Table de dominos", en="Domino table", elements=table(11, "dgreen", "dwood", 1, 15, 1, 15) + [
        (2, 11, 2, 14, 11.6, 14, "dgreen"), (3, 11.6, 3, 5, 12, 7, "bone"), (6, 11.6, 4, 8, 12, 8, "bone"), (9, 11.6, 3, 11, 12, 7, "bone"), (4, 11.6, 9, 6, 12, 13, "bone"), (8, 11.6, 10, 10, 12, 14, "bone"), (11, 11.6, 9, 13, 12, 13, "bone"),
        (3.6, 11.6, 4, 4.4, 12.2, 6, "carbon"), (9.6, 11.6, 4, 10.4, 12.2, 6, "carbon")]),
    dict(id="balalaika_stand", fr="Balalaïka sur support", en="Balalaika stand", elements=[
        (4, 0, 4, 12, 2, 12, "dgray"), (7, 2, 7, 9, 10, 9, "wood"), (4, 10, 6, 12, 12, 10, "dwood"), (5, 12, 5, 11, 17, 11, "wood"), (6, 17, 6, 10, 20, 10, "lwood"), (7, 14, 4.4, 9, 16, 5, "carbon"), (7.4, 20, 7, 8.6, 30, 9, "dwood"),
        (6.6, 29, 6.6, 9.4, 32, 9.4, "dwood"), (7.6, 14, 4.6, 8.4, 30, 5, "steel")]),
    dict(id="accordion", fr="Accordéon", en="Accordion", elements=[
        (3, 0, 4, 13, 2, 12, "dwood"), (2, 2, 4, 5, 14, 12, "bakelite"), (11, 2, 4, 14, 14, 12, "bakelite"), (5, 3, 5, 11, 13, 11, "ussr_red"), (5, 5, 4.6, 11, 6, 5, "bone"), (5, 8, 4.6, 11, 9, 5, "bone"), (5, 11, 4.6, 11, 12, 5, "bone"),
        (2.4, 4, 3.6, 4.6, 12, 4, "bone"), (11.4, 4, 3.6, 13.6, 12, 4, "carbon"), (3, 13, 5, 5, 14, 11, "gold"), (11, 13, 5, 13, 14, 11, "gold")]),
    dict(id="card_game_table", fr="Table de cartes", en="Card game table", elements=table(11, "felt" if False else "dgreen", "dwood", 1, 15, 1, 15) + [
        (1, 11, 1, 15, 11.5, 15, "dwood"), (2, 11.5, 2, 14, 12, 14, "dgreen"), (3, 12, 4, 5, 12.4, 7, "paper"), (6, 12, 3, 8, 12.4, 6, "paper"), (9, 12, 5, 11, 12.4, 8, "paper"), (5, 12, 10, 8, 12.4, 12, "paper"), (10, 12, 10, 12, 13, 12, "gold"),
        (12, 12, 3, 14, 12.6, 5, "tin")]),
    dict(id="tea_vodka_corner", fr="Coin thé et vodka", en="Tea and vodka corner", elements=table(12, "lwood", "dwood", 0, 16, 2, 14) + [
        (2, 12, 3, 5, 16, 6, "bronze"), (2.4, 16, 3.4, 4.6, 18, 5.6, "gold"), (7, 12, 3, 9, 20, 5, "glass"), (7.4, 12, 3.4, 8.6, 17, 4.6, "frost"), (10, 12, 4, 12, 14, 6, "glass"), (13, 12, 4, 15, 14, 6, "glass"), (4, 12, 9, 8, 13, 13, "paper"),
        (10, 12, 9, 14, 14, 11, "dwood")]),
    # ================================================== ARMURERIE
    dict(id="rifle_rack", fr="Râtelier de fusils", en="Rifle rack", elements=[
        (0, 0, 5, 16, 3, 11, "dwood"), (0, 24, 5, 16, 26, 11, "dwood"), (0, 3, 5, 2, 24, 11, "wood"), (14, 3, 5, 16, 24, 11, "wood"), (2, 3, 7.4, 3.2, 23, 8.6, "carbon"), (4.4, 3, 7.4, 5.6, 23, 8.6, "carbon"), (6.8, 3, 7.4, 8, 23, 8.6, "carbon"),
        (9.2, 3, 7.4, 10.4, 23, 8.6, "carbon"), (11.6, 3, 7.4, 12.8, 23, 8.6, "carbon"), (2, 3, 7, 3.2, 12, 9, "wood"), (4.4, 3, 7, 5.6, 12, 9, "wood"), (6.8, 3, 7, 8, 12, 9, "wood"), (9.2, 3, 7, 10.4, 12, 9, "wood"), (11.6, 3, 7, 12.8, 12, 9, "wood")]),
    dict(id="grenade_crates", fr="Caisses de grenades", en="Grenade crates", elements=[
        (0, 0, 1, 10, 7, 9, "olive"), (6, 0, 8, 16, 7, 16, "olive"), (2, 7, 2, 12, 12, 8, "kaki_d"), (0, 3, 1, 10, 4, 9, "dgray"), (6, 3, 8, 16, 4, 16, "dgray"), (3, 12, 3, 5, 14, 5, "yellow"), (7, 12, 3, 9, 13, 5, "yellow"),
        (2, 5, 0.6, 8, 6, 1, "paper"), (9, 4, 7.6, 13, 5, 8, "paper"), (1, 7.4, 4, 3, 8, 6, "steel")]),
    dict(id="repair_bench", fr="Établi de réparation d'armes", en="Weapon repair bench", elements=table(13, "wood", "dwood", 0, 16, 2, 14) + [
        (0, 13, 2, 16, 14, 14, "dgray"), (1, 14, 4, 6, 15, 10, "tin"), (2, 15, 5, 5, 17, 9, "carbon"), (8, 14, 4, 15, 15, 7, "carbon"), (9, 15, 4.4, 12, 16, 5.6, "wood"), (9, 15, 8, 14, 17, 11, "steel"), (3, 6, 2, 13, 7, 14, "dgray"),
        (1, 20, 13.4, 15, 28, 14, "dwood"), (2, 22, 13, 5, 26, 13.4, "steel"), (7, 22, 13, 9, 26, 13.4, "steel"), (11, 22, 13, 13, 26, 13.4, "steel")]),
    dict(id="ammo_sacks", fr="Sacs de munitions", en="Ammunition sacks", elements=[
        (0, 0, 2, 7, 5, 8, "kaki_d"), (7, 0, 3, 14, 5, 9, "kaki_d"), (2, 0, 8, 9, 5, 14, "kaki_d"), (2, 5, 3, 9, 9, 9, "tan"), (8, 5, 4, 14, 8, 10, "tan"), (4, 9, 4, 11, 12, 10, "kaki_d"), (1, 4.6, 4, 3, 5.4, 6, "rope"),
        (8, 4.6, 5, 10, 5.4, 7, "rope"), (5, 11.6, 6, 7, 12.4, 8, "rope")]),
    dict(id="oil_drum", fr="Bidon d'huile", en="Oil drum", elements=[
        (2, 0, 2, 14, 16, 14, "soviet_green"), (2, 2, 2, 14, 3, 14, "steel"), (2, 7, 2, 14, 8, 14, "steel"), (2, 13, 2, 14, 14, 14, "steel"), (3, 16, 3, 13, 17, 13, "dgray"), (4, 17, 4, 7, 18, 7, "steel"), (9, 17, 9, 12, 18, 12, "steel"),
        (5, 8, 1.8, 11, 12, 2, "paper"), (6, 9, 1.6, 10, 10, 2, "red")]),
    dict(id="shooting_dummy", fr="Mannequin de tir", en="Shooting dummy", elements=[
        (3, 0, 3, 13, 2, 13, "dwood"), (7, 2, 7, 9, 18, 9, "wood"), (4, 18, 6, 12, 28, 10, "tan"), (5, 28, 6.4, 11, 31, 9.6, "tan"), (3, 20, 6.4, 4, 26, 9.6, "tan"), (12, 20, 6.4, 13, 26, 9.6, "tan"), (6, 22, 5.6, 10, 26, 6, "bloodred"),
        (7, 23, 5.4, 9, 25, 5.8, "carbon"), (6, 29, 6, 10, 30, 6.4, "olive")]),
    # ================================================== GARAGE ET ATELIER
    dict(id="engine_block_stand", fr="Moteur sur support", en="Engine block on stand", elements=[
        (1, 0, 3, 15, 2, 13, "dgray"), (2, 2, 4, 5, 8, 12, "carbon"), (11, 2, 4, 14, 8, 12, "carbon"), (3, 8, 4, 13, 16, 12, "dgray"), (4, 16, 5, 12, 20, 11, "tin"), (5, 20, 6, 7, 24, 10, "steel"), (9, 20, 6, 11, 24, 10, "steel"),
        (3, 10, 3.4, 13, 14, 4, "steel"), (13, 9, 6, 16, 11, 10, "steel"), (2, 5, 12, 6, 7, 15, "brick"), (6, 12, 3.4, 10, 13, 4, "yellow")]),
    dict(id="tank_track_repair", fr="Chenille de char en réparation", en="Tank track under repair", elements=[
        (0, 0, 2, 16, 3, 14, "carbon"), (0, 3, 3, 3, 6, 13, "ash"), (4, 3, 3, 7, 6, 13, "dgray"), (8, 3, 3, 11, 6, 13, "ash"), (12, 3, 3, 16, 6, 13, "dgray"), (1, 6, 4, 4, 8, 12, "carbon"), (5, 6, 4, 9, 8, 12, "ash"), (10, 6, 4, 14, 8, 12, "carbon"),
        (3, 8, 5, 6, 10, 11, "rust"), (7, 6, 12, 9, 9, 14, "steel"), (12, 6, 1, 14, 8, 4, "steel"), (1, 3, 1.6, 5, 4, 3, "yellow")]),
    dict(id="chain_hoist", fr="Palan à chaîne", en="Chain hoist", elements=[
        (0, 28, 6, 16, 32, 10, "yellow"), (7, 24, 7, 9, 28, 9, "steel"), (6, 22, 6, 10, 24, 10, "carbon"), (7.4, 8, 7.4, 8.6, 22, 8.6, "ash"), (7, 6, 7, 9, 8, 9, "steel"), (6, 3, 6, 10, 6, 10, "steel"), (5, 3, 6, 7, 4, 7, "yellow"),
        (9, 3, 9, 11, 4, 10, "yellow"), (0, 26, 7, 2, 30, 9, "carbon"), (14, 26, 7, 16, 30, 9, "carbon")]),
    dict(id="inspection_pit", fr="Fosse de visite", en="Inspection pit", elements=[
        (0, 0, 0, 16, 2, 16, "carbon"), (0, 2, 0, 3, 8, 16, "dgray"), (13, 2, 0, 16, 8, 16, "dgray"), (3, 2, 0, 13, 3, 16, "ash"), (0, 8, 0, 3, 9, 16, "yellow"), (13, 8, 0, 16, 9, 16, "yellow"), (3, 3, 2, 4, 8, 14, "steel"), (12, 3, 2, 13, 8, 14, "steel"),
        (5, 3, 6, 11, 4, 10, "paper"), (6, 4, 7, 10, 6, 9, "steel")]),
    dict(id="ural_tire_pile", fr="Pile de pneus d'Oural", en="Ural tire pile", elements=[
        (0, 0, 0, 16, 6, 16, "carbon"), (1, 6, 1, 15, 12, 15, "carbon"), (3, 12, 3, 13, 17, 13, "carbon"), (2, 1, 2, 14, 5, 14, "ash"), (4, 7, 4, 12, 11, 12, "ash"), (6, 13, 6, 10, 16, 10, "ash"), (6, 2, 6, 10, 4, 10, "dgray"),
        (7, 8, 7, 9, 10, 9, "dgray"), (7, 14, 7, 9, 15, 9, "dgray")]),
    dict(id="soviet_fuel_pump", fr="Pompe à carburant soviétique", en="Soviet fuel pump", elements=[
        (2, 0, 3, 14, 2, 13, "stone"), (3, 2, 4, 13, 20, 12, "soviet_green"), (4, 12, 3.4, 12, 18, 4, "bone"), (5, 14, 3.2, 11, 17, 3.4, "carbon"), (3, 20, 4, 13, 22, 12, "dgray"), (6, 22, 5, 10, 26, 11, "tin"), (13, 6, 6, 16, 8, 10, "carbon"),
        (14, 2, 7, 15, 6, 9, "carbon"), (4, 4, 3.4, 12, 8, 4, "olive"), (7, 5, 3.2, 9, 7, 3.4, "yellow")]),
    # ================================================== MISSILES ET NUCLÉAIRE
    dict(id="rocket_model", fr="Maquette de fusée", en="Rocket model", elements=[
        (3, 0, 3, 13, 3, 13, "dgray"), (4, 3, 4, 12, 4, 12, "ash"), (6, 4, 6, 10, 18, 10, "white"), (6.4, 6, 5.6, 9.6, 8, 6, "ussr_red"), (6, 18, 6, 10, 22, 10, "tin"), (7, 22, 7, 9, 26, 9, "tin"), (7.4, 26, 7.4, 8.6, 28, 8.6, "ussr_red"),
        (4, 4, 7, 6, 10, 9, "ussr_red"), (10, 4, 7, 12, 10, 9, "ussr_red"), (7, 4, 4, 9, 10, 6, "ussr_red"), (7, 4, 10, 9, 10, 12, "ussr_red")]),
    dict(id="launch_console", fr="Console de lancement", en="Launch console", anim="blink", light=6, elements=[
        (0, 0, 3, 16, 10, 15, "soviet_green"), (0, 10, 3, 16, 14, 8, "dgray"), (0, 14, 8, 16, 22, 10, "carbon"), (1, 14.5, 8, 15, 21.5, 7.4, "carbon", {"north": "anim"}), (2, 10.6, 4, 5, 11.6, 7, "red"), (6, 10.6, 4, 9, 11.6, 7, "yellow"),
        (10, 10.6, 4, 13, 11.6, 7, "green"), (3, 11.6, 5, 4, 13, 6, "red"), (11, 11.6, 5, 12, 13, 6, "steel"), (0, 0, 3, 16, 2, 15, "dgray")]),
    dict(id="dual_launch_keys", fr="Double clé de lancement", en="Dual launch keys", elements=[
        (2, 0, 3, 14, 6, 13, "carbon"), (3, 6, 4, 13, 8, 12, "dgray"), (3.4, 8, 5, 5.6, 9, 7, "steel"), (4, 9, 5.4, 5, 12, 6.6, "gold"), (10.4, 8, 5, 12.6, 9, 7, "steel"), (11, 9, 5.4, 12, 12, 6.6, "gold"), (3, 8, 9, 13, 10, 11, "ussr_red"),
        (6, 10, 9.4, 10, 11, 10.6, "paper"), (7, 6, 3.4, 9, 8, 4, "red"), (2, 2, 2.6, 14, 4, 3, "yellow")]),
    dict(id="silo_hatch_deco", fr="Trappe de silo", en="Silo hatch", elements=[
        (0, 0, 0, 16, 2, 16, "carbon"), (1, 2, 1, 15, 4, 15, "dgray"), (2, 4, 2, 14, 6, 14, "ash"), (3, 6, 3, 13, 7, 13, "carbon"), (3, 6.6, 7.2, 13, 7.2, 8.8, "ussr_red"), (7.2, 6.6, 3, 8.8, 7.2, 13, "ussr_red"), (5, 7, 5, 11, 8, 11, "steel"),
        (6, 8, 6, 10, 9, 10, "yellow"), (1, 4, 1, 3, 5, 3, "yellow"), (13, 4, 13, 15, 5, 15, "yellow")]),
    dict(id="radiation_sign", fr="Panneau radiation", en="Radiation sign", passable=True, light=4, elements=[
        (7, 0, 7.4, 9, 16, 8.6, "steel"), (1, 14, 7, 15, 30, 9, "yellow"), (2, 15, 6.6, 14, 29, 7, "carbon"), (6, 20, 6.2, 10, 24, 6.6, "yellow"), (7, 16, 6.2, 9, 19, 6.6, "yellow"), (3, 25, 6.2, 6, 28, 6.6, "yellow"), (10, 25, 6.2, 13, 28, 6.6, "yellow"),
        (7.2, 21.2, 6, 8.8, 22.8, 6.4, "carbon")]),
    dict(id="geiger_bench", fr="Compteur Geiger", en="Geiger counter", anim="blink", light=3, elements=[
        (4, 0, 4, 12, 9, 12, "olive"), (4.4, 2, 3.6, 11.6, 7, 4, "carbon", {"north": "anim"}), (5, 0, 3.4, 11, 2, 4, "dgray"), (6, 9, 6, 10, 11, 10, "dgray"), (7, 11, 7, 9, 20, 9, "steel"), (6, 20, 6, 10, 22, 10, "tin"),
        (12, 3, 6, 15, 5, 10, "carbon"), (13, 5, 7, 14, 9, 9, "steel"), (3, 6, 6, 4, 8, 10, "red")]),
    # ================================================== DÉFENSE
    dict(id="mg_nest", fr="Nid de mitrailleuse", en="Machine-gun nest", elements=[
        (0, 0, 0, 16, 5, 6, "kaki_d"), (0, 0, 10, 16, 5, 16, "kaki_d"), (0, 0, 6, 3, 5, 10, "kaki_d"), (13, 0, 6, 16, 5, 10, "kaki_d"), (0, 5, 0, 16, 9, 5, "tan"), (0, 5, 11, 16, 8, 16, "tan"), (3, 5, 6, 13, 7, 10, "ash"),
        (4, 7, 7, 6, 9, 9, "carbon"), (5, 9, 6.6, 6, 10, 8.4, "steel"), (6, 8, 7.4, 16, 9, 8.6, "carbon"), (14, 7, 7, 16, 10, 9, "dgray")]),
    dict(id="aa_searchlight", fr="Projecteur de DCA", en="AA searchlight", light=15, elements=[
        (2, 0, 2, 14, 3, 14, "dgray"), (6, 3, 6, 10, 8, 10, "steel"), (3, 8, 3, 13, 10, 13, "olive"), (2, 10, 4, 14, 22, 12, "olive"), (3, 11, 3.6, 13, 21, 4, "lamp"), (4, 22, 5, 12, 24, 11, "dgray"), (1, 12, 6, 2, 20, 10, "steel"),
        (14, 12, 6, 15, 20, 10, "steel"), (6, 24, 6, 10, 26, 10, "steel")]),
    dict(id="armored_hatch_door", fr="Porte blindée de bunker", en="Armored bunker door", elements=[
        (0, 0, 5, 16, 32, 11, "ash"), (1, 1, 4.4, 15, 31, 5, "carbon"), (2, 2, 4, 14, 30, 4.4, "dgray"), (11, 14, 3.4, 13, 18, 4, "steel"), (11, 17, 3, 12.4, 19, 4, "steel"), (3, 4, 3.8, 6, 6, 4.4, "yellow"), (3, 26, 3.8, 6, 28, 4.4, "yellow"),
        (3, 14, 3.8, 5, 18, 4.4, "steel"), (0, 8, 4.6, 2, 10, 5, "steel"), (0, 22, 4.6, 2, 24, 5, "steel"), (7, 24, 3.6, 9, 29, 4, "ussr_red")]),
    dict(id="periscope", fr="Périscope", en="Periscope", elements=[
        (3, 0, 3, 13, 2, 13, "dgray"), (6, 2, 6, 10, 28, 10, "olive"), (5, 4, 4.6, 11, 8, 6, "carbon"), (6, 26, 5.4, 10, 31, 6, "carbon"), (6, 27, 10, 10, 30, 11, "glass"), (5, 12, 6, 6, 16, 10, "steel"), (10, 12, 6, 11, 16, 10, "steel"),
        (7, 6, 3.6, 9, 9, 4.6, "glass")]),
    dict(id="guard_checkpoint_bar", fr="Barrière de poste de garde", en="Guard checkpoint barrier", elements=[
        (0, 0, 6, 4, 14, 10, "ussr_red"), (0, 14, 5, 4, 16, 11, "carbon"), (1, 4, 5.6, 3, 10, 6, "steel"), (4, 10, 7, 16, 13, 9, "white"), (6, 10, 7, 8, 13, 9, "ussr_red"), (10, 10, 7, 12, 13, 9, "ussr_red"), (14, 10, 7, 16, 13, 9, "ussr_red"),
        (13, 0, 7, 16, 8, 9, "dgray")]),
    dict(id="sentry_post", fr="Poste de sentinelle", en="Sentry post", elements=[
        (2, 0, 2, 14, 2, 14, "kaki_d"), (2, 2, 2, 4, 20, 4, "soviet_green"), (12, 2, 2, 14, 20, 4, "soviet_green"), (2, 2, 12, 4, 20, 14, "soviet_green"), (12, 2, 12, 14, 20, 14, "soviet_green"), (1, 20, 1, 15, 22, 15, "dgray"),
        (2, 12, 2, 14, 13, 4, "tan"), (2, 12, 12, 14, 13, 14, "tan"), (5, 2, 11, 11, 8, 13, "paper"), (6, 22, 6, 10, 24, 10, "steel"), (7, 24, 7, 9, 28, 9, "ussr_red")]),
    # ================================================== SIGNALÉTIQUE ET LUMIÈRES
    dict(id="cyrillic_stop_sign", fr="Panneau « СТОП » en cyrillique", en="Cyrillic STOP sign", passable=True, elements=[
        (7, 0, 7.4, 9, 14, 8.6, "steel"), (3, 14, 7, 13, 24, 9, "ussr_red"), (4, 15, 6.6, 12, 23, 7, "carbon"), (5, 20, 6.2, 11, 22, 6.6, "white"), (5, 17, 6.2, 7, 20, 6.6, "white"), (7.5, 17, 6.2, 8.5, 20, 6.6, "white"), (9, 17, 6.2, 11, 20, 6.6, "white"),
        (3, 14, 7.4, 13, 15, 8.6, "carbon")]),
    dict(id="cyrillic_exit_sign", fr="Panneau « ВЫХОД » (sortie)", en="Cyrillic EXIT sign", passable=True, light=9, elements=[
        (1, 5, 14, 15, 11, 16, "dgreen"), (2, 6, 13.6, 14, 10, 14, "leaf"), (3, 7, 13.2, 4.4, 9, 13.6, "white"), (5.4, 7, 13.2, 7.4, 9, 13.6, "white"), (8.4, 7, 13.2, 10.4, 9, 13.6, "white"), (11.4, 7, 13.2, 13, 9, 13.6, "white"),
        (0, 4, 14.4, 1, 12, 15.6, "steel"), (15, 4, 14.4, 16, 12, 15.6, "steel")]),
    dict(id="cyrillic_danger_sign", fr="Panneau « ОПАСНО » (danger)", en="Cyrillic DANGER sign", passable=True, elements=[
        (7, 0, 7.4, 9, 12, 8.6, "steel"), (2, 12, 7, 14, 28, 9, "yellow"), (3, 13, 6.6, 13, 27, 7, "carbon"), (4, 14, 6.2, 12, 26, 6.6, "yellow"), (7, 21, 6, 9, 25, 6.4, "carbon"), (7, 16, 6, 9, 19, 6.4, "carbon"), (5, 14.4, 6, 11, 15.4, 6.4, "carbon")]),
    dict(id="alarm_beacon", fr="Gyrophare d'alarme rouge", en="Red alarm beacon", anim="blink", light=13, passable=True, elements=[
        (4, 0, 12, 12, 2, 16, "dgray"), (5, 2, 12, 11, 4, 15, "steel"), (5, 4, 11, 11, 11, 15, "red"), (5, 4, 10.6, 11, 11, 11, "carbon", {"north": "anim"}), (6, 11, 12, 10, 12, 14, "white"), (7, 5, 9.6, 9, 9, 10.6, "lamp")]),
    dict(id="wall_siren", fr="Sirène murale", en="Wall siren", passable=True, elements=[
        (3, 4, 12, 13, 12, 16, "dgray"), (4, 5, 8, 12, 11, 12, "soviet_green"), (3, 6, 4, 13, 10, 8, "tin"), (4, 7, 2, 12, 9, 4, "steel"), (6, 7.4, 1, 10, 8.6, 2, "ash"), (13, 6, 13, 15, 8, 15, "steel"), (1, 6, 13, 3, 8, 15, "steel"),
        (7, 12, 11, 9, 16, 13, "carbon")]),
    dict(id="caged_bunker_lamp", fr="Lampe de bunker grillagée", en="Caged bunker lamp", anim="flicker", light=13, passable=True, elements=[
        (5, 0, 13, 11, 2, 16, "dgray"), (6, 2, 12, 10, 4, 15, "steel"), (5, 4, 11, 11, 11, 15, "lamp"), (4.6, 4, 10.6, 11.4, 5, 15.4, "carbon"), (4.6, 7, 10.6, 11.4, 8, 15.4, "carbon"), (4.6, 10, 10.6, 11.4, 11, 15.4, "carbon"),
        (4.6, 4, 10.6, 5.4, 11, 15.4, "carbon"), (10.6, 4, 10.6, 11.4, 11, 15.4, "carbon"), (6, 11, 11.6, 10, 12, 15, "steel")]),
    dict(id="steam_pipes", fr="Tuyaux de vapeur", en="Steam pipes", elements=[
        (0, 10, 5, 16, 14, 9, "tin"), (0, 9, 4, 3, 15, 10, "steel"), (13, 9, 4, 16, 15, 10, "steel"), (6, 14, 6, 10, 24, 8, "tin"), (5, 24, 5, 11, 28, 9, "steel"), (7, 28, 6.4, 9, 31, 7.6, "red"), (2, 3, 6, 4, 9, 8, "tin"),
        (12, 3, 6, 14, 9, 8, "tin"), (0, 3, 5.4, 16, 5, 8.6, "rust"), (5, 15, 4.6, 11, 16, 5.4, "steam_w")]),
    # ================================================== USURE ET GUERRE
    dict(id="broken_crates", fr="Caisses défoncées", en="Smashed crates", elements=[
        (0, 0, 1, 8, 6, 8, "wood"), (1, 6, 2, 6, 8, 5, "dwood"), (8, 0, 7, 16, 5, 15, "dwood"), (9, 5, 8, 12, 9, 12, "wood"), (3, 0, 9, 7, 3, 14, "plank_dark"), (5, 1, 1, 6, 3, 8, "paper"), (10, 5, 12, 14, 7, 15, "rust"),
        (12, 5, 6, 15, 6, 8, "tan"), (0, 0, 7, 3, 2, 9, "dwood")]),
    dict(id="torn_soviet_flag", fr="Drapeau soviétique troué", en="Torn Soviet flag", elements=[
        (7, 0, 7, 9, 30, 9, "dwood"), (9, 16, 7.4, 16, 28, 8.6, "bloodred"), (9, 16, 7.4, 11, 19, 8.6, "carbon"), (13, 22, 7.4, 16, 25, 8.6, "carbon"), (10, 22, 7.2, 12, 25, 8.8, "ash"), (14, 17, 7.4, 16, 19, 8.6, "dgray"), (9.4, 24, 7.2, 11.6, 26.6, 8.8, "gold"),
        (14.4, 26, 7.4, 16, 28, 8.6, "ash")]),
    dict(id="bullet_holes_wall", fr="Impacts de balles", en="Bullet holes wall", passable=True, elements=[
        (0, 0, 15, 16, 16, 16, "ash"), (2, 10, 14.4, 4, 12, 15, "carbon"), (3, 9, 14.2, 4, 10, 14.6, "carbon"), (8, 12, 14.4, 10, 14, 15, "carbon"), (11, 6, 14.4, 13, 8, 15, "carbon"), (5, 4, 14.4, 7, 6, 15, "carbon"), (12, 11, 14.2, 13.4, 12.4, 14.6, "carbon"),
        (9, 3, 14.2, 10.4, 4.4, 14.6, "carbon"), (1, 2, 14.4, 2.6, 3.6, 15, "carbon")]),
    dict(id="barbed_coil", fr="Rouleau de barbelés", en="Barbed wire coil", passable=True, elements=[
        (0, 0, 6, 16, 2, 10, "rust"), (1, 2, 5, 5, 8, 11, "steel"), (5, 2, 5, 9, 8, 11, "ash"), (9, 2, 5, 13, 8, 11, "steel"), (13, 2, 5, 16, 6, 11, "ash"), (2, 8, 6, 6, 11, 10, "steel"), (7, 8, 6, 11, 11, 10, "ash"), (0, 0, 4, 2, 6, 12, "dwood"),
        (14, 0, 4, 16, 6, 12, "dwood")]),
    dict(id="wrecked_car_shell", fr="Carcasse de véhicule", en="Wrecked vehicle shell", elements=[
        (0, 2, 2, 16, 8, 14, "rust"), (1, 8, 3, 11, 13, 13, "ash"), (2, 9, 2.6, 6, 12, 3, "carbon"), (7, 9, 2.6, 10, 12, 3, "carbon"), (11, 8, 4, 16, 10, 12, "carbon"), (0, 0, 2, 4, 4, 4, "wheel"), (12, 0, 2, 16, 3, 4, "carbon"),
        (0, 0, 12, 4, 3, 14, "wheel"), (5, 13, 5, 9, 15, 10, "rust"), (3, 6, 14, 8, 10, 15, "carbon")]),
    dict(id="shot_up_dummy", fr="Mannequin criblé", en="Shot-up dummy", elements=[
        (3, 0, 3, 13, 2, 13, "dwood"), (7, 2, 7, 9, 16, 9, "wood"), (4, 16, 6, 12, 26, 10, "tan"), (5, 26, 6.4, 11, 29, 9.6, "tan"), (2, 18, 6.4, 4, 24, 9.6, "tan"), (12, 18, 6.4, 14, 22, 9.6, "tan"), (5, 18, 5.6, 7, 20, 6, "carbon"),
        (8, 21, 5.6, 10, 23, 6, "carbon"), (6, 24, 5.6, 8, 25, 6, "carbon"), (9, 27, 6, 10, 28, 6.4, "carbon"), (13, 14, 6, 15, 17, 9, "tan")]),
    # ================================================== GOULAG ET PRISON
    dict(id="barracks_bunk_frame", fr="Châlit de baraquement", en="Barracks bunk frame", elements=[
        (0, 0, 1, 2, 28, 3, "dwood"), (14, 0, 1, 16, 28, 3, "dwood"), (0, 0, 13, 2, 28, 15, "dwood"), (14, 0, 13, 16, 28, 15, "dwood"), (2, 4, 1, 14, 6, 15, "wood"), (2, 14, 1, 14, 16, 15, "wood"), (2, 24, 1, 14, 26, 15, "wood"),
        (3, 6, 3, 13, 8, 13, "kaki_d"), (3, 16, 3, 13, 18, 13, "kaki_d"), (3, 26, 3, 13, 28, 13, "kaki_d"), (3, 8, 3, 6, 9, 6, "tan")]),
    dict(id="guard_tower_light", fr="Tour de garde avec projecteur", en="Guard tower with floodlight", light=15, elements=[
        (2, 0, 2, 4, 24, 4, "dwood"), (12, 0, 2, 14, 24, 4, "dwood"), (2, 0, 12, 4, 24, 14, "dwood"), (12, 0, 12, 14, 24, 14, "dwood"), (2, 8, 2, 14, 9, 14, "wood"), (1, 24, 1, 15, 26, 15, "dgray"), (2, 26, 2, 14, 28, 4, "wood"),
        (2, 26, 12, 14, 28, 14, "wood"), (4, 26, 6, 10, 30, 10, "olive"), (5, 27, 5.4, 9, 29, 6, "lamp"), (6, 30, 6, 8, 32, 8, "steel")]),
    dict(id="call_bell", fr="Cloche d'appel", en="Call bell", elements=[
        (2, 0, 6, 4, 20, 10, "dwood"), (12, 0, 6, 14, 20, 10, "dwood"), (2, 20, 6, 14, 22, 10, "wood"), (5, 12, 5, 11, 18, 11, "bronze"), (4, 8, 4, 12, 12, 12, "bronze"), (6, 18, 6, 10, 20, 10, "gold"), (7, 6, 7, 9, 8, 9, "steel"),
        (7.4, 8, 7.4, 8.6, 12, 8.6, "ash"), (13, 12, 7, 16, 13, 9, "rope")]),
    dict(id="file_stamp_desk", fr="Bureau des dossiers", en="File desk", elements=table(13, "dgray", "carbon", 0, 16, 2, 14) + [
        (1, 13, 3, 7, 15, 9, "paper"), (2, 15, 4, 6, 16, 8, "paper"), (9, 13, 3, 14, 14, 7, "ussr_red"), (10, 14, 4, 13, 15, 6, "bakelite"), (11, 15, 4.6, 12, 18, 5.4, "bakelite"), (2, 13, 10, 8, 14, 13, "paper"), (10, 13, 9, 14, 14, 13, "carbon"),
        (11, 14, 10, 13, 15, 12, "steel")]),
    dict(id="punishment_stool", fr="Tabouret de punition", en="Punishment stool", elements=[
        (4, 6, 4, 12, 8, 12, "wood"), (4, 0, 4, 6, 6, 6, "dwood"), (10, 0, 4, 12, 6, 6, "dwood"), (4, 0, 10, 6, 6, 12, "dwood"), (10, 0, 10, 12, 6, 12, "dwood"), (6, 2, 6, 10, 3, 10, "dwood"), (3, 8, 3, 13, 9, 13, "wood"),
        (7, 9, 7, 9, 11, 9, "tin"), (0, 0, 7, 2, 2, 9, "rope"), (14, 0, 7, 16, 2, 9, "rope")]),
    dict(id="rules_sign", fr="Écriteau de règlement", en="Rules signboard", passable=True, elements=[
        (1, 2, 14.4, 15, 15, 16, "dwood"), (2, 3, 14, 14, 14, 14.4, "paper"), (3, 12, 13.8, 13, 13, 14, "carbon"), (3, 10, 13.8, 12, 11, 14, "carbon"), (3, 8, 13.8, 13, 9, 14, "carbon"), (3, 6, 13.8, 11, 7, 14, "carbon"),
        (3, 4, 13.8, 12, 5, 14, "carbon"), (11, 3.6, 13.4, 13, 4.6, 13.8, "ussr_red"), (0, 0, 14.6, 2, 3, 15.4, "wood"), (14, 0, 14.6, 16, 3, 15.4, "wood")]),
]

"""
Huitième série de décors : 96 objets de nature (hors villes et factions) : forêt, montagne, désert et steppe, marais et jungle, rivière et lac,
mer et plage, neige et glace, météo et ciel, faune et traces, cueillette et chasse, fermes et prairies, campement de randonneurs, ruines et vestiges,
magie et légendes, mines et grottes, épaves et crash. Posés dans le monde par la fonction de génération « nature_decor » (voir NatureDecorFeature).
Importé par generate_realism_decor.py ; les couleurs s'ajoutent à la première palette (PAL).
"""
import random

from PIL import Image

PAL8 = {
    "moss": "#5a7e3c", "bark": "#4a3626", "shroom": "#c8382e", "amethyst": "#8a5ac8", "crystal": "#6ad0ea", "basalt": "#383a40", "driftwood": "#a89a82",
    "shell": "#f0d9c9", "coral": "#e86c5c", "meteor": "#5a4842", "mud": "#6a5238",
}


def _f(n):
    return Image.new("RGBA", (16, 16 * n))


def anim_fireflies():
    """Lucioles : points jaunes qui dérivent dans l'obscurité."""
    img = _f(8)
    rnd = random.Random(17)
    pts = [(rnd.uniform(1, 14), rnd.uniform(1, 14), rnd.uniform(0, 6.28)) for _ in range(9)]
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (14, 22, 18, 255))
        for (px, py, ph) in pts:
            import math
            x = int(px + 2 * math.sin(ph + f * 0.8)) % 16
            y = int(py + 2 * math.cos(ph * 1.3 + f * 0.6)) % 16
            on = (f + int(ph * 3)) % 3 != 0
            img.putpixel((x, y + 16 * f), (255, 240, 120, 255) if on else (120, 120, 60, 255))
    return img, {"frametime": 4}


def anim_portal():
    """Portail ancien : spirale violette qui tourne."""
    import math
    img = _f(8)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                d = (dx * dx + dy * dy) ** 0.5
                a = math.atan2(dy, dx) + f * math.pi / 4 + d * 0.5
                v = 0.5 + 0.5 * math.sin(3 * a)
                c = (int(60 + 120 * v), int(20 + 40 * v), int(120 + 120 * v))
                img.putpixel((x, y + 16 * f), c + (255,))
    return img, {"frametime": 2}


def anim_geyser():
    """Geyser : jet de vapeur blanc qui monte."""
    img = _f(8)
    rnd = random.Random(23)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (120, 170, 200, 255))
        for k in range(14):
            y = (15 - k - f * 2) % 16
            w = 1 + (15 - y) // 5
            for x in range(8 - w, 8 + w):
                img.putpixel((x, y + 16 * f), (235, 245, 250, 255))
        for _ in range(8):
            img.putpixel((rnd.randint(2, 13), rnd.randint(0, 15) + 16 * f), (255, 255, 255, 255))
    return img, {"frametime": 3}


ANIMS8 = {"fireflies": anim_fireflies, "portal": anim_portal, "geyser": anim_geyser}

DECOS8 = [
    # ================================================== FORÊT ET SOUS-BOIS
    dict(id="hollow_log", fr="Tronc creux", en="Hollow log", elements=[
        (0, 0, 3, 16, 3, 13, "bark"), (0, 3, 2, 16, 12, 4, "bark"), (0, 3, 12, 16, 12, 14, "bark"), (0, 11, 3, 16, 13, 13, "bark"), (0, 3, 4, 16, 4, 12, "soil"), (1, 3, 5, 5, 5, 9, "moss"), (11, 11, 4, 15, 13.4, 12, "moss"),
        (0, 4, 4, 1, 11, 12, "dwood"), (6, 4, 11.4, 9, 6, 12, "shroom")]),
    dict(id="giant_mushrooms", fr="Champignons géants", en="Giant mushrooms", elements=[
        (3, 0, 3, 6, 12, 6, "cream"), (0, 12, 0, 9, 15, 9, "shroom"), (1, 15, 1, 8, 16, 8, "shroom"), (2, 15.4, 2, 4, 15.8, 4, "star_white"), (10, 0, 8, 12, 7, 10, "cream"), (8, 7, 6, 14, 9, 12, "shroom"), (9, 9, 7, 13, 9.6, 11, "shroom"),
        (11, 0, 2, 13, 5, 4, "cream"), (10, 5, 1, 15, 7, 6, "bone")]),
    dict(id="mossy_stump", fr="Souche moussue", en="Mossy stump", elements=[
        (3, 0, 3, 13, 9, 13, "bark"), (2, 0, 6, 5, 3, 10, "bark"), (11, 0, 5, 14, 3, 9, "bark"), (4, 9, 4, 12, 10, 12, "lwood"), (5, 10, 5, 11, 10.6, 11, "dwood"), (3, 6, 3, 8, 10, 8, "moss"), (9, 8, 9, 13, 11, 13, "moss"),
        (10, 10, 4, 12, 12, 6, "shroom")]),
    dict(id="fallen_branches", fr="Branches tombées", en="Fallen branches", passable=True, elements=[
        (0, 0, 3, 14, 2, 5, "bark"), (4, 0, 8, 16, 2, 10, "bark"), (2, 2, 5, 9, 3, 7, "dwood"), (8, 0, 11, 15, 2, 13, "wood"), (1, 0, 12, 6, 2, 14, "bark"), (6, 2, 8.4, 11, 3, 9.6, "moss"), (3, 3, 3.6, 6, 4, 4.4, "leaf"), (11, 2, 11.6, 14, 3, 12.4, "dleaf")]),
    dict(id="forest_fern_patch", fr="Touffe de fougères", en="Fern patch", passable=True, elements=[
        (7, 0, 7, 9, 5, 9, "dleaf"), (1, 3, 7.4, 8, 6, 8.6, "leaf"), (8, 3, 7.4, 15, 6, 8.6, "leaf"), (7.4, 3, 1, 8.6, 6, 8, "moss"), (7.4, 3, 8, 8.6, 6, 15, "moss"), (3, 5, 3, 7, 8, 7, "leaf"), (9, 5, 9, 13, 8, 13, "leaf"), (9, 4, 2, 13, 7, 6, "dleaf"),
        (2, 4, 10, 6, 7, 14, "dleaf")]),
    dict(id="log_pile_stack", fr="Pile de bûches", en="Log pile", elements=[
        (0, 0, 2, 16, 4, 6, "bark"), (0, 0, 7, 16, 4, 11, "bark"), (0, 0, 12, 16, 4, 16, "bark"), (2, 4, 3, 14, 8, 7, "wood"), (2, 4, 9, 14, 8, 13, "wood"), (4, 8, 5, 12, 12, 10, "bark"), (0, 1, 2, 1, 3, 6, "lwood"), (0, 1, 7, 1, 3, 11, "lwood"),
        (15, 1, 12, 16, 3, 16, "lwood")]),
    # ================================================== MONTAGNE ET ROCHE
    dict(id="stone_cairn", fr="Cairn de pierres", en="Stone cairn", elements=[
        (2, 0, 2, 14, 4, 14, "stone"), (3, 4, 3, 13, 7, 13, "ash"), (4, 7, 4, 12, 10, 12, "stone"), (5, 10, 5, 11, 13, 11, "ash"), (6, 13, 6, 10, 15, 10, "stone"), (7, 15, 7, 9, 16, 9, "slate"), (1, 0, 5, 3, 2, 9, "ash"), (13, 0, 8, 15, 3, 12, "stone")]),
    dict(id="erratic_boulder", fr="Bloc erratique", en="Erratic boulder", elements=[
        (1, 0, 1, 15, 8, 15, "stone"), (2, 8, 2, 14, 12, 14, "ash"), (4, 12, 4, 12, 14, 12, "stone"), (0, 0, 4, 3, 4, 12, "ash"), (13, 0, 3, 16, 5, 11, "ash"), (3, 5, 0.6, 8, 9, 1, "moss"), (9, 3, 15, 14, 6, 15.6, "moss"), (5, 9, 5, 9, 12, 9, "slate")]),
    dict(id="crystal_cluster", fr="Amas de cristaux", en="Crystal cluster", light=7, elements=[
        (2, 0, 2, 14, 3, 14, "stone"), (4, 3, 4, 8, 14, 8, "amethyst"), (5, 14, 5, 7, 18, 7, "amethyst"), (9, 3, 6, 12, 11, 10, "crystal"), (10, 11, 7, 11, 15, 9, "crystal"), (3, 3, 9, 6, 9, 12, "amethyst"), (8, 3, 10, 11, 8, 13, "crystal"),
        (12, 3, 3, 14, 7, 5, "amethyst")]),
    dict(id="scree_pile", fr="Éboulis", en="Scree pile", elements=[
        (0, 0, 1, 16, 3, 15, "stone"), (1, 3, 2, 12, 6, 12, "ash"), (3, 6, 4, 9, 9, 10, "stone"), (11, 3, 9, 15, 6, 14, "slate"), (0, 3, 11, 5, 5, 15, "ash"), (12, 0, 0, 16, 4, 3, "stone"), (5, 9, 6, 7, 10, 8, "slate"), (6, 3, 12, 9, 5, 15, "stone")]),
    dict(id="rock_arch_small", fr="Petite arche rocheuse", en="Small rock arch", elements=[
        (0, 0, 3, 5, 12, 13, "stone"), (11, 0, 3, 16, 14, 13, "stone"), (3, 12, 3, 13, 18, 13, "ash"), (2, 10, 4, 6, 14, 12, "ash"), (10, 12, 4, 14, 16, 12, "ash"), (0, 4, 2.4, 3, 8, 3, "moss"), (13, 6, 2.4, 16, 10, 3, "moss"), (5, 18, 5, 11, 20, 11, "stone"),
        (0, 0, 1, 4, 3, 3, "ash")]),
    dict(id="mountain_spire", fr="Aiguille de roche", en="Rock spire", elements=[
        (2, 0, 2, 14, 6, 14, "basalt"), (3, 6, 3, 13, 14, 13, "stone"), (4, 14, 4, 12, 22, 12, "ash"), (5, 22, 5, 11, 28, 11, "stone"), (6, 28, 6, 10, 32, 10, "slate"), (1, 0, 6, 3, 4, 10, "basalt"), (13, 0, 7, 15, 5, 11, "ash"), (6, 18, 3.6, 10, 24, 4, "frost"),
        (7, 30, 7, 9, 32, 9, "frost")]),
    # ================================================== DÉSERT ET STEPPE
    dict(id="dune_skeleton", fr="Squelette dans les dunes", en="Dune skeleton", elements=[
        (0, 0, 0, 16, 2, 16, "sand"), (1, 2, 5, 15, 3, 7, "bone"), (3, 3, 4, 4, 8, 8, "bone"), (6, 3, 4, 7, 8, 8, "bone"), (9, 3, 4, 10, 8, 8, "bone"), (12, 3, 4, 13, 7, 8, "bone"), (14, 2, 4, 16, 5, 8, "bone"), (0, 2, 10, 4, 3, 14, "bone"),
        (1, 3, 11, 3, 4, 13, "bone"), (8, 2, 11, 13, 4, 13, "bone")]),
    dict(id="saguaro_cactus", fr="Cactus saguaro", en="Saguaro cactus", elements=[
        (6, 0, 6, 10, 28, 10, "green"), (7, 28, 7, 9, 30, 9, "dleaf"), (1, 10, 7, 6, 13, 9, "green"), (1, 13, 7, 3, 22, 9, "green"), (10, 14, 7, 15, 17, 9, "green"), (13, 17, 7, 15, 25, 9, "green"), (6.4, 2, 5.6, 7, 26, 6, "star_white"),
        (9, 6, 10, 9.6, 24, 10.4, "star_white"), (1.4, 22, 7.4, 2.6, 24, 8.6, "pink"), (13.4, 25, 7.4, 14.6, 27, 8.6, "pink")]),
    dict(id="tumbleweed", fr="Boule de broussaille", en="Tumbleweed", passable=True, elements=[
        (4, 0, 4, 12, 8, 12, "driftwood"), (2, 2, 6, 14, 6, 10, "hay"), (6, 2, 2, 10, 6, 14, "hay"), (3, 4, 3, 13, 5, 13, "wood"), (5, 8, 5, 11, 9, 11, "driftwood"), (1, 1, 7, 5, 3, 9, "driftwood"), (11, 3, 7, 15, 5, 9, "hay"),
        (7, 4, 0, 9, 6, 4, "hay")]),
    dict(id="bleached_skull", fr="Crâne blanchi", en="Bleached skull", elements=[
        (0, 0, 0, 16, 2, 16, "sand"), (3, 2, 3, 13, 10, 13, "bone"), (2, 3, 5, 3, 8, 11, "bone"), (13, 3, 5, 14, 8, 11, "bone"), (4, 5, 2.4, 7, 8, 3, "carbon"), (9, 5, 2.4, 12, 8, 3, "carbon"), (7, 4, 2.4, 9, 5, 3, "carbon"), (3, 10, 2, 10, 13, 6, "bone"),
        (11, 10, 10, 15, 12, 14, "cream")]),
    dict(id="wind_rippled_dune", fr="Dune ridée par le vent", en="Wind-rippled dune", passable=True, elements=[
        (0, 0, 0, 16, 3, 16, "sand"), (1, 3, 2, 15, 5, 14, "hay"), (3, 5, 4, 13, 7, 12, "sand"), (0, 3, 0, 16, 3.6, 2, "cream"), (0, 3, 5, 16, 3.6, 7, "cream"), (0, 3, 10, 16, 3.6, 12, "cream"), (5, 7, 6, 11, 8, 10, "hay"), (2, 3.6, 14, 14, 4, 16, "cream")]),
    dict(id="wild_caravan_well", fr="Puits de caravane isolé", en="Wild caravan well", elements=[
        (1, 0, 1, 15, 8, 15, "sand"), (2, 8, 2, 14, 10, 14, "tan"), (4, 8.6, 4, 12, 9.6, 12, "water"), (1, 8, 1, 4, 11, 4, "tan"), (12, 8, 1, 15, 11, 4, "tan"), (1, 8, 12, 4, 11, 15, "tan"), (12, 8, 12, 15, 11, 15, "tan"), (7, 10, 7, 9, 26, 9, "wood"),
        (2, 24, 7, 14, 26, 9, "wood"), (7.4, 14, 7.4, 8.6, 24, 8.6, "rope"), (6, 12, 6, 10, 14, 10, "wood")]),
    # ================================================== MARAIS ET JUNGLE
    dict(id="giant_lily_pads", fr="Nénuphars géants", en="Giant lily pads", passable=True, elements=[
        (0, 0, 0, 16, 1, 16, "mud"), (1, 1, 1, 7, 1.6, 7, "leaf"), (9, 1, 2, 15, 1.6, 8, "dleaf"), (2, 1, 9, 8, 1.6, 15, "dleaf"), (9, 1, 10, 14, 1.6, 15, "leaf"), (3, 1.6, 3, 5, 3, 5, "pink"), (11, 1.6, 4, 13, 3.4, 6, "star_white"),
        (4, 1.6, 11, 6, 3, 13, "pink")]),
    dict(id="mangrove_roots", fr="Racines de palétuvier", en="Mangrove roots", elements=[
        (6, 14, 6, 10, 32, 10, "bark"), (2, 6, 2, 6, 16, 6, "dwood"), (10, 6, 2, 14, 16, 6, "dwood"), (2, 6, 10, 6, 16, 14, "dwood"), (10, 6, 10, 14, 16, 14, "dwood"), (0, 0, 0, 4, 8, 4, "bark"), (12, 0, 0, 16, 8, 4, "bark"), (0, 0, 12, 4, 8, 16, "bark"),
        (12, 0, 12, 16, 8, 16, "bark"), (3, 0, 3, 13, 2, 13, "mud")]),
    dict(id="swamp_fungus_stump", fr="Souche à champignons de marais", en="Swamp fungus stump", elements=[
        (3, 0, 3, 13, 8, 13, "mud"), (2, 6, 4, 14, 10, 12, "bark"), (3, 10, 3, 6, 12, 6, "shroom"), (9, 9, 8, 14, 11, 13, "amethyst"), (6, 11, 6, 10, 13, 10, "moss"), (1, 0, 8, 4, 4, 12, "moss"), (12, 0, 2, 15, 3, 6, "dleaf"),
        (4, 8, 10, 7, 12, 13, "shroom")]),
    dict(id="giant_jungle_flower", fr="Fleur géante de la jungle", en="Giant jungle flower", elements=[
        (7, 0, 7, 9, 18, 9, "leaf"), (1, 4, 7.4, 7, 6, 8.6, "dleaf"), (9, 8, 7.4, 15, 10, 8.6, "dleaf"), (2, 18, 2, 14, 20, 14, "magenta"), (4, 20, 4, 12, 24, 12, "pink"), (6, 24, 6, 10, 26, 10, "yellow"), (5, 17, 5, 11, 18, 11, "dleaf"), (0, 19, 6, 3, 21, 10, "magenta"),
        (13, 19, 6, 16, 21, 10, "magenta")]),
    dict(id="mud_pit", fr="Fosse de boue", en="Mud pit", passable=True, elements=[
        (0, 0, 0, 16, 2, 16, "dwood"), (1, 2, 1, 15, 3, 15, "mud"), (3, 3, 3, 13, 3.6, 13, "soil"), (5, 3.6, 5, 11, 4, 11, "mud"), (1, 2, 1, 3, 4, 5, "moss"), (13, 2, 11, 15, 5, 15, "moss"), (7, 4, 7, 9, 4.6, 9, "water"), (11, 2, 1, 15, 4, 3, "leaf")]),
    dict(id="stilt_hut_small", fr="Petite hutte sur pilotis", en="Small stilt hut", elements=[
        (1, 0, 1, 3, 12, 3, "wood"), (13, 0, 1, 15, 12, 3, "wood"), (1, 0, 13, 3, 12, 15, "wood"), (13, 0, 13, 15, 12, 15, "wood"), (0, 12, 0, 16, 14, 16, "dwood"), (2, 14, 2, 14, 22, 14, "wood"), (1, 22, 1, 15, 24, 15, "hay"), (3, 24, 3, 13, 27, 13, "hay"),
        (5, 27, 5, 11, 29, 11, "hay"), (6, 14, 1.4, 10, 20, 2, "carbon"), (7, 4, 6, 9, 12, 8, "rope")]),
    # ================================================== RIVIÈRE ET LAC
    dict(id="wooden_jetty", fr="Ponton en bois", en="Wooden jetty", elements=[
        (0, 6, 2, 16, 8, 14, "driftwood"), (1, 8, 3, 15, 8.4, 4, "lwood"), (1, 8, 7, 15, 8.4, 8, "lwood"), (1, 8, 11, 15, 8.4, 12, "lwood"), (1, 0, 2, 3, 6, 4, "wood"), (13, 0, 2, 15, 6, 4, "wood"), (1, 0, 12, 3, 6, 14, "wood"), (13, 0, 12, 15, 6, 14, "wood"),
        (14, 8, 6, 16, 14, 8, "dwood"), (14, 12, 6, 16, 14, 8, "rope")]),
    dict(id="moored_rowboat", fr="Barque amarrée", en="Moored rowboat", elements=[
        (1, 2, 4, 15, 4, 12, "wood"), (0, 4, 3, 16, 7, 5, "dwood"), (0, 4, 11, 16, 7, 13, "dwood"), (0, 4, 5, 2, 7, 11, "dwood"), (14, 4, 5, 16, 7, 11, "dwood"), (6, 4, 4, 8, 6, 12, "lwood"), (3, 4, 6, 5, 4.6, 10, "wood"), (10, 4, 6, 12, 4.6, 10, "wood"),
        (14, 7, 7, 16, 9, 9, "rope"), (5, 5, 1, 7, 6, 4, "wood")]),
    dict(id="drying_fishing_net", fr="Filet de pêche qui sèche", en="Drying fishing net", passable=True, elements=[
        (0, 0, 7, 2, 28, 9, "wood"), (14, 0, 7, 16, 28, 9, "wood"), (0, 26, 7, 16, 28, 9, "wood"), (2, 6, 7.6, 14, 26, 8.4, "rope"), (5, 6, 7, 6, 26, 9, "rope"), (9, 6, 7, 10, 26, 9, "rope"), (2, 12, 7, 14, 13, 9, "rope"), (2, 19, 7, 14, 20, 9, "rope"),
        (4, 6, 6.8, 6, 8, 9.2, "driftwood")]),
    dict(id="reed_clump", fr="Touffe de roseaux", en="Reed clump", passable=True, elements=[
        (1, 0, 2, 3, 24, 4, "leaf"), (5, 0, 7, 7, 28, 9, "moss"), (9, 0, 3, 11, 22, 5, "leaf"), (12, 0, 9, 14, 26, 11, "moss"), (2, 0, 11, 4, 20, 13, "leaf"), (7, 0, 12, 9, 25, 14, "moss"), (5, 24, 6.4, 7.6, 32, 9.6, "bark"),
        (1, 22, 1.6, 3, 28, 4.4, "bark"), (12, 26, 8.4, 14, 32, 11.6, "bark")]),
    dict(id="small_water_mill", fr="Petit moulin à eau", en="Small water mill", elements=[
        (0, 0, 4, 16, 3, 16, "stone"), (1, 3, 6, 11, 14, 16, "wood"), (0, 14, 5, 12, 17, 16, "dwood"), (2, 17, 7, 10, 20, 15, "dwood"), (11, 1, 5, 12, 15, 15, "dwood"), (12, 0, 8, 16, 2, 12, "wood"), (13, 2, 6, 15, 12, 14, "wood"), (12.6, 2, 7, 13, 12, 13, "wood"),
        (3, 4, 5.4, 6, 9, 6, "glass"), (7, 3, 5.4, 9, 8, 6, "dwood")]),
    dict(id="stepping_stones", fr="Pas de géant", en="Stepping stones", passable=True, elements=[
        (0, 0, 0, 16, 1, 16, "mud"), (1, 1, 2, 5, 3, 6, "stone"), (7, 1, 4, 11, 3.4, 8, "ash"), (11, 1, 9, 15, 2.6, 13, "stone"), (3, 1, 9, 7, 3, 13, "ash"), (8, 1, 11, 10, 2.4, 14, "slate"), (5, 3, 3, 6, 3.6, 4, "moss"), (12, 2.6, 10, 14, 3, 12, "moss")]),
    # ================================================== MER ET PLAGE
    dict(id="shell_pile", fr="Tas de coquillages", en="Shell pile", passable=True, elements=[
        (0, 0, 0, 16, 1, 16, "sand"), (2, 1, 2, 7, 3, 7, "shell"), (9, 1, 3, 14, 4, 8, "coral"), (3, 1, 9, 8, 3, 14, "cream"), (10, 1, 10, 14, 3, 14, "shell"), (4, 3, 4, 6, 4, 6, "coral"), (11, 4, 5, 13, 5, 7, "shell"), (5, 3, 11, 7, 4, 13, "coral")]),
    dict(id="driftwood_pile", fr="Tas de bois flotté", en="Driftwood pile", elements=[
        (0, 0, 2, 16, 3, 6, "driftwood"), (1, 0, 8, 15, 3, 12, "driftwood"), (3, 3, 4, 13, 6, 8, "lwood"), (2, 3, 9, 12, 5, 12, "driftwood"), (5, 6, 5, 11, 8, 9, "driftwood"), (12, 0, 12, 16, 2, 16, "driftwood"), (0, 0, 13, 5, 2, 15, "driftwood"),
        (6, 8, 6, 9, 9, 8, "rope")]),
    dict(id="beach_crab_rocks", fr="Rochers à crabes", en="Crab rocks", elements=[
        (0, 0, 0, 16, 2, 16, "sand"), (1, 2, 2, 8, 7, 9, "stone"), (9, 2, 8, 15, 5, 14, "ash"), (2, 7, 3, 6, 8, 7, "moss"), (10, 3, 3, 13, 5, 6, "coral"), (11, 5, 4, 12, 6, 5, "coral"), (12, 3, 3, 13, 4, 4, "carbon"), (4, 2, 11, 7, 4, 14, "coral"),
        (3, 4, 12, 4, 5, 13, "carbon")]),
    dict(id="seaweed_heap", fr="Tas d'algues", en="Seaweed heap", passable=True, elements=[
        (0, 0, 0, 16, 1, 16, "sand"), (1, 1, 2, 14, 4, 13, "dleaf"), (3, 4, 4, 12, 6, 11, "moss"), (6, 6, 6, 10, 7, 10, "leaf"), (0, 1, 8, 5, 3, 15, "leaf"), (11, 1, 1, 16, 3, 7, "moss"), (4, 1, 13, 10, 2, 16, "dleaf"), (7, 6.6, 7, 9, 8, 9, "coral")]),
    dict(id="sandcastle", fr="Château de sable", en="Sandcastle", elements=[
        (0, 0, 0, 16, 2, 16, "sand"), (2, 2, 2, 14, 7, 14, "hay"), (1, 7, 1, 5, 12, 5, "hay"), (11, 7, 1, 15, 12, 5, "hay"), (1, 7, 11, 5, 12, 15, "hay"), (11, 7, 11, 15, 12, 15, "hay"), (6, 7, 6, 10, 10, 10, "sand"), (7, 10, 7, 9, 12, 9, "hay"),
        (2, 12, 2, 4, 13, 4, "sand"), (7, 12, 7.4, 8, 15, 8.6, "red"), (7, 2, 1.6, 9, 5, 2, "carbon")]),
    dict(id="beached_buoy", fr="Bouée échouée", en="Beached buoy", elements=[
        (2, 0, 2, 14, 2, 14, "sand"), (4, 2, 4, 12, 8, 12, "red"), (4, 8, 4, 12, 12, 12, "star_white"), (4, 12, 4, 12, 16, 12, "red"), (6, 16, 6, 10, 20, 10, "steel"), (7, 20, 7, 9, 24, 9, "dgray"), (6, 22, 6.4, 10, 23, 7, "lamp"), (12, 2, 6, 16, 4, 10, "rope"),
        (0, 0, 10, 4, 3, 16, "driftwood")]),
    # ================================================== NEIGE ET GLACE
    dict(id="roadside_snowman", fr="Bonhomme de neige", en="Snowman", elements=[
        (0, 0, 0, 16, 2, 16, "frost"), (2, 2, 2, 14, 9, 14, "star_white"), (4, 9, 4, 12, 15, 12, "star_white"), (5, 15, 5, 11, 20, 11, "star_white"), (7, 17, 4.6, 9, 18, 5, "orange"), (6, 18, 4.6, 7, 19, 5, "carbon"), (9, 18, 4.6, 10, 19, 5, "carbon"),
        (5, 14, 4, 11, 15, 12, "red"), (3, 9, 7, 5, 14, 9, "bark"), (11, 9, 7, 13, 14, 9, "bark"), (5, 20, 5, 11, 24, 11, "carbon")]),
    dict(id="ice_stalagmites", fr="Stalagmites de glace", en="Ice stalagmites", elements=[
        (0, 0, 0, 16, 2, 16, "frost"), (2, 2, 2, 7, 14, 7, "ice"), (3, 14, 3, 6, 20, 6, "ice"), (4, 20, 4, 5, 24, 5, "frost"), (9, 2, 8, 13, 10, 12, "ice"), (10, 10, 9, 12, 14, 11, "frost"), (2, 2, 10, 5, 8, 13, "ice"), (11, 2, 2, 14, 6, 5, "frost")]),
    dict(id="snow_igloo_small", fr="Petit igloo", en="Small igloo", elements=[
        (0, 0, 0, 16, 1, 16, "frost"), (1, 1, 1, 15, 4, 15, "star_white"), (2, 4, 2, 14, 7, 14, "star_white"), (4, 7, 4, 12, 10, 12, "star_white"), (6, 10, 6, 10, 12, 10, "frost"), (5, 1, 0.4, 11, 5, 1.2, "carbon"), (0, 1, 5, 2, 3, 11, "ice"), (14, 1, 5, 16, 3, 11, "ice"),
        (7, 12, 7, 9, 13, 9, "ice")]),
    dict(id="frozen_geyser", fr="Geyser gelé", en="Frozen geyser", anim="geyser", elements=[
        (1, 0, 1, 15, 3, 15, "ice"), (3, 3, 3, 13, 6, 13, "frost"), (5, 6, 5, 11, 8, 11, "ice"), (6, 8, 6, 10, 20, 10, "steam_w", {"north": "anim", "south": "anim", "east": "anim", "west": "anim"}), (5, 20, 5, 11, 24, 11, "steam_w"), (0, 0, 5, 3, 6, 11, "frost"),
        (13, 0, 6, 16, 5, 10, "ice"), (7, 24, 7, 9, 28, 9, "star_white")]),
    dict(id="snow_footprints", fr="Traces de pas dans la neige", en="Snow footprints", passable=True, elements=[
        (0, 0, 0, 16, 1, 16, "star_white"), (2, 0.6, 2, 5, 1.2, 6, "frost"), (9, 0.6, 4, 12, 1.2, 8, "frost"), (3, 0.6, 9, 6, 1.2, 13, "frost"), (10, 0.6, 11, 13, 1.2, 15, "frost"), (6, 1, 1, 8, 1.2, 3, "frost"), (7, 1, 7, 9, 1.2, 10, "frost"),
        (13, 1, 1, 15, 1.2, 3, "frost")]),
    dict(id="icicle_cluster", fr="Grappe de glaçons", en="Icicle cluster", passable=True, elements=[
        (0, 28, 0, 16, 32, 16, "ice"), (2, 14, 2, 4, 28, 4, "ice"), (6, 8, 3, 8, 28, 5, "frost"), (10, 16, 2, 12, 28, 4, "ice"), (3, 10, 9, 5, 28, 11, "frost"), (8, 18, 10, 10, 28, 12, "ice"), (12, 12, 8, 14, 28, 10, "frost"), (7, 22, 7, 8, 28, 8, "ice")]),
    # ================================================== MÉTÉO ET CIEL
    dict(id="meteorite_crater", fr="Cratère de météorite", en="Meteorite crater", light=6, passable=True, elements=[
        (0, 0, 0, 16, 2, 16, "basalt"), (1, 2, 1, 15, 4, 15, "meteor"), (3, 3, 3, 13, 4, 13, "soil"), (5, 3, 5, 11, 4, 11, "meteor"), (6, 4, 6, 10, 8, 10, "meteor"), (7, 8, 7, 9, 10, 9, "flame"), (0, 2, 0, 3, 5, 3, "basalt"), (13, 2, 13, 16, 5, 16, "basalt"),
        (11, 2, 2, 14, 4, 5, "meteor")]),
    dict(id="fulgurite_glass", fr="Fulgurite de foudre", en="Fulgurite glass", elements=[
        (0, 0, 0, 16, 2, 16, "sand"), (7, 2, 7, 9, 22, 9, "glass"), (5, 2, 5, 7, 12, 7, "glass"), (9, 8, 9, 11, 16, 11, "glass"), (8, 12, 4, 10, 18, 6, "glass"), (4, 14, 8, 8, 16, 10, "glass"), (6.4, 22, 6.4, 9.6, 24, 9.6, "lamp"), (7.4, 3, 7.4, 8.6, 20, 8.6, "carbon")]),
    dict(id="eagle_aerie", fr="Aire d'aigle", en="Eagle aerie", elements=[
        (1, 0, 1, 15, 8, 15, "stone"), (0, 8, 0, 16, 11, 16, "ash"), (2, 11, 2, 14, 13, 14, "bark"), (3, 13, 3, 13, 14, 13, "wood"), (5, 14, 5, 11, 16, 11, "hay"), (6, 16, 6, 8, 18, 8, "star_white"), (9, 16, 8, 11, 17, 10, "star_white"), (1, 11, 5, 5, 13, 9, "dwood"),
        (11, 11, 8, 15, 13, 12, "dwood")]),
    dict(id="plains_tipi", fr="Tipi des plaines", en="Plains tipi", elements=[
        (0, 0, 0, 16, 1, 16, "sand"), (1, 1, 1, 15, 6, 15, "cream"), (3, 6, 3, 13, 12, 13, "cream"), (5, 12, 5, 11, 18, 11, "cream"), (6.6, 18, 6.6, 9.4, 24, 9.4, "cream"), (7, 24, 7, 9, 32, 9, "wood"), (5, 1, 0.6, 11, 6, 1.4, "carbon"),
        (2, 3, 1.6, 14, 4, 2.2, "red"), (4, 8, 3.6, 12, 9, 4.2, "denim")]),
    dict(id="wind_banner_pole", fr="Mât à banderoles", en="Wind banner pole", elements=[
        (5, 0, 5, 11, 2, 11, "stone"), (7.4, 2, 7.4, 8.6, 30, 8.6, "wood"), (8.6, 20, 7.4, 16, 23, 8.6, "red"), (8.6, 24, 7.4, 15, 27, 8.6, "star_white"), (8.6, 28, 7.4, 14, 30, 8.6, "denim"), (8.6, 16, 7.4, 13, 19, 8.6, "yellow"), (7, 30, 7, 9, 32, 9, "brass")]),
    dict(id="rain_gauge_post", fr="Pluviomètre sur piquet", en="Rain gauge post", elements=[
        (7, 0, 7, 9, 12, 9, "wood"), (4, 12, 4, 12, 14, 12, "steel"), (5, 14, 5, 11, 22, 11, "glass"), (5.4, 14.4, 5.4, 10.6, 17, 10.6, "water"), (4, 22, 4, 12, 24, 12, "steel"), (6, 18, 4.4, 10, 19, 5, "carbon"), (6, 20, 4.4, 10, 21, 5, "carbon"),
        (11, 4, 6, 14, 5, 10, "lwood")]),
    # ================================================== FAUNE ET TRACES
    dict(id="burrow_mound", fr="Terrier", en="Burrow mound", elements=[
        (0, 0, 0, 16, 2, 16, "soil"), (1, 2, 2, 15, 5, 14, "mud"), (3, 5, 3, 13, 7, 13, "soil"), (5, 7, 5, 11, 8, 11, "mud"), (5, 2, 1.6, 11, 5, 2.4, "carbon"), (0, 2, 8, 3, 4, 12, "leaf"), (13, 2, 3, 16, 3, 6, "leaf"), (6, 2, 0.8, 10, 3, 1.6, "mud"),
        (7, 7.6, 7, 9, 8.4, 9, "leaf")]),
    dict(id="ground_bird_nest", fr="Nid d'oiseau au sol", en="Ground bird nest", elements=[
        (2, 0, 2, 14, 2, 14, "soil"), (3, 2, 3, 13, 4, 13, "hay"), (4, 4, 4, 12, 5, 12, "dwood"), (5, 4.6, 5, 11, 5.4, 11, "hay"), (5, 5, 5, 7, 7, 7, "star_white"), (9, 5, 6, 11, 7, 8, "cream"), (6, 5, 9, 8, 7, 11, "turquoise"), (0, 0, 5, 3, 2, 9, "leaf"),
        (13, 0, 8, 16, 2, 12, "leaf")]),
    dict(id="animal_tracks", fr="Empreintes d'animaux", en="Animal tracks", passable=True, elements=[
        (0, 0, 0, 16, 1, 16, "mud"), (2, 1, 2, 4, 1.4, 4, "soil"), (2, 1, 5, 4, 1.4, 7, "soil"), (5, 1, 1, 7, 1.4, 3, "soil"), (5, 1, 4, 7, 1.4, 6, "soil"), (9, 1, 8, 11, 1.4, 10, "soil"), (12, 1, 6, 14, 1.4, 8, "soil"), (9, 1, 12, 11, 1.4, 14, "soil"),
        (12, 1, 11, 14, 1.4, 13, "soil")]),
    dict(id="carcass_bones", fr="Carcasse et ossements", en="Carcass and bones", elements=[
        (0, 0, 0, 16, 1, 16, "soil"), (1, 1, 4, 15, 3, 6, "bone"), (2, 3, 3, 3, 8, 7, "bone"), (5, 3, 3, 6, 9, 7, "bone"), (8, 3, 3, 9, 9, 7, "bone"), (11, 3, 3, 12, 8, 7, "bone"), (13, 1, 5, 16, 5, 7, "bone"), (3, 1, 9, 9, 4, 14, "cream"),
        (10, 1, 10, 14, 3, 13, "bone")]),
    dict(id="bee_skep_wild", fr="Ruche sauvage", en="Wild bee skep", elements=[
        (3, 0, 3, 13, 2, 13, "wood"), (4, 2, 4, 12, 8, 12, "hay"), (5, 8, 5, 11, 12, 11, "hay"), (6, 12, 6, 10, 14, 10, "hay"), (5, 4, 3.4, 11, 6, 4, "carbon"), (7, 14, 7, 9, 15, 9, "wood"), (4, 3, 4, 12, 4, 12, "dwood"), (4, 6, 4, 12, 7, 12, "dwood"),
        (4, 9.4, 5, 12, 10.2, 11, "yellow")]),
    dict(id="spider_web_cocoons", fr="Toiles et cocons d'araignée", en="Spider webs and cocoons", passable=True, elements=[
        (0, 0, 7.6, 16, 0.6, 8.4, "star_white"), (0, 0, 0, 16, 14, 1, "star_white"), (2, 2, 1, 7, 10, 5, "cream"), (9, 4, 2, 13, 12, 6, "cream"), (1, 14, 0.6, 15, 15, 1.4, "star_white"), (4, 2, 1.6, 5, 4, 2.4, "carbon"), (10, 4, 2.6, 11, 6, 3.4, "carbon"),
        (6, 8, 3, 9, 14, 6, "bone"), (0, 0, 4, 4, 8, 4.6, "star_white")]),
    # ================================================== CUEILLETTE ET CHASSE
    dict(id="hunter_blind", fr="Affût de chasseur", en="Hunter's blind", elements=[
        (1, 0, 3, 3, 14, 5, "wood"), (13, 0, 3, 15, 14, 5, "wood"), (1, 0, 11, 3, 14, 13, "wood"), (13, 0, 11, 15, 14, 13, "wood"), (0, 12, 2, 16, 15, 14, "dleaf"), (2, 0, 2, 14, 12, 3, "moss"), (4, 6, 1.4, 12, 8, 2, "carbon"), (3, 3, 11.4, 13, 8, 12, "bark"),
        (5, 15, 5, 11, 17, 11, "leaf")]),
    dict(id="pelt_drying_rack", fr="Séchoir à peaux", en="Pelt drying rack", elements=[
        (0, 0, 6, 2, 26, 10, "wood"), (14, 0, 6, 16, 26, 10, "wood"), (0, 24, 6, 16, 26, 10, "wood"), (2, 6, 7, 7, 24, 9, "leather"), (8, 4, 7, 13, 22, 9, "tan"), (3, 10, 6.6, 6, 14, 7, "bark"), (10, 8, 6.6, 12, 12, 7, "carbon"), (3, 0, 2, 13, 1, 6, "bark")]),
    dict(id="berry_basket", fr="Panier de baies", en="Berry basket", elements=[
        (2, 0, 2, 14, 8, 14, "wood"), (1, 8, 1, 15, 10, 15, "dwood"), (3, 10, 3, 13, 12, 13, "red"), (4, 12, 4, 8, 14, 8, "magenta"), (8, 12, 7, 12, 14, 11, "red"), (5, 12, 10, 9, 13, 13, "purple"), (3, 2, 1.6, 13, 3, 2.4, "lwood"), (3, 6, 1.6, 13, 7, 2.4, "lwood"),
        (5, 14, 5, 7, 15, 7, "leaf")]),
    dict(id="snare_trap", fr="Collet de chasseur", en="Snare trap", passable=True, elements=[
        (2, 0, 2, 4, 14, 4, "wood"), (12, 0, 12, 14, 8, 14, "wood"), (3, 12, 3, 12, 13, 4, "bark"), (3, 12, 3, 4, 13, 12, "rope"), (4, 6, 8, 12, 7, 9, "rope"), (5, 0, 5, 11, 1, 11, "leaf"), (6, 1, 6, 10, 3, 10, "mud"), (9, 7, 8, 12, 9, 9, "steel")]),
    dict(id="fish_smoker", fr="Fumoir à poissons", en="Fish smoker", anim="fire", light=5, elements=[
        (1, 0, 1, 15, 3, 15, "stone"), (2, 3, 2, 14, 18, 14, "wood"), (3, 3, 1.6, 13, 6, 2.4, "carbon", {"north": "anim"}), (3, 8, 2.4, 13, 17, 3, "dwood"), (3, 12, 3, 5, 17, 5, "tan"), (7, 10, 3, 9, 16, 5, "tan"), (11, 11, 3, 13, 17, 5, "tan"),
        (1, 18, 1, 15, 20, 15, "dwood"), (6, 20, 6, 10, 28, 10, "dgray"), (4, 6, 4, 12, 7, 12, "dgray")]),
    dict(id="mushroom_baskets", fr="Paniers de champignons", en="Mushroom baskets", elements=[
        (0, 0, 2, 8, 7, 10, "wood"), (8, 0, 4, 16, 6, 12, "wood"), (1, 7, 3, 7, 9, 9, "cream"), (2, 9, 4, 5, 11, 7, "shroom"), (9, 6, 5, 15, 8, 11, "bone"), (11, 8, 6, 14, 10, 9, "tan"), (3, 0, 10, 9, 5, 16, "lwood"), (4, 5, 11, 8, 7, 15, "cream"),
        (1, 0, 5.4, 7, 1, 6, "dwood")]),
    # ================================================== FERMES ET PRAIRIES
    dict(id="hay_bale_roll", fr="Rouleaux de foin", en="Hay bale rolls", elements=[
        (1, 0, 1, 15, 14, 15, "hay"), (0, 0, 4, 16, 12, 12, "hay"), (3, 14, 3, 13, 15, 13, "hay"), (1, 4, 0.4, 15, 5, 1.2, "rope"), (1, 9, 0.4, 15, 10, 1.2, "rope"), (7, 0, 15, 9, 14, 16, "rope"), (3, 1, 3, 8, 12, 4, "tan"), (9, 1, 5, 14, 11, 6, "tan")]),
    dict(id="scarecrow_field", fr="Épouvantail de champ", en="Field scarecrow", elements=[
        (7, 0, 7, 9, 26, 9, "wood"), (1, 17, 7, 15, 19, 9, "wood"), (4, 12, 6, 12, 24, 10, "denim"), (2, 17, 6.4, 4, 20, 9.6, "hay"), (12, 17, 6.4, 14, 20, 9.6, "hay"), (5, 24, 5.4, 11, 30, 10.6, "hay"), (4, 30, 4, 12, 31, 12, "dwood"),
        (6, 31, 6, 10, 32, 10, "dwood"), (6, 26, 4.8, 7, 27, 5.4, "carbon"), (9, 26, 4.8, 10, 27, 5.4, "carbon")]),
    dict(id="feed_trough", fr="Auge à fourrage", en="Feed trough", elements=[
        (0, 5, 3, 16, 7, 13, "wood"), (0, 7, 3, 16, 10, 5, "dwood"), (0, 7, 11, 16, 10, 13, "dwood"), (0, 7, 5, 2, 10, 11, "dwood"), (14, 7, 5, 16, 10, 11, "dwood"), (2, 7, 5, 14, 9, 11, "hay"), (1, 0, 4, 3, 5, 6, "wood"), (13, 0, 4, 15, 5, 6, "wood"),
        (1, 0, 10, 3, 5, 12, "wood"), (13, 0, 10, 15, 5, 12, "wood")]),
    dict(id="rustic_fence_section", fr="Clôture rustique", en="Rustic fence section", passable=True, elements=[
        (0, 0, 6, 3, 16, 10, "wood"), (13, 0, 6, 16, 16, 10, "wood"), (3, 11, 7, 13, 13, 9, "dwood"), (3, 4, 7, 13, 6, 9, "dwood"), (6, 0, 6, 10, 15, 10, "wood"), (0, 14, 5.6, 3, 16, 10.4, "lwood"), (13, 14, 5.6, 16, 16, 10.4, "lwood"),
        (6, 14, 5.6, 10, 16, 10.4, "lwood")]),
    dict(id="wind_pump", fr="Éolienne de pompage", en="Windpump", elements=[
        (2, 0, 2, 4, 30, 4, "steel"), (12, 0, 2, 14, 30, 4, "steel"), (2, 0, 12, 4, 30, 14, "steel"), (12, 0, 12, 14, 30, 14, "steel"), (2, 10, 2, 14, 11, 4, "steel"), (2, 20, 2, 14, 21, 4, "steel"), (6, 28, 6, 10, 32, 10, "dgray"),
        (0, 28, 7.4, 6, 29, 8.6, "steel"), (10, 28, 7.4, 16, 29, 8.6, "steel"), (7.4, 22, 7.4, 8.6, 28, 8.6, "steel"), (6, 0, 6, 10, 6, 10, "tin")]),
    dict(id="vegetable_patch", fr="Potager", en="Vegetable patch", elements=[
        (0, 0, 0, 16, 2, 16, "soil"), (1, 2, 1, 6, 3, 5, "mud"), (1, 3, 1.4, 6, 8, 2.4, "leaf"), (1, 3, 3.4, 6, 8, 4.4, "leaf"), (8, 2, 2, 15, 3, 6, "mud"), (9, 3, 3, 13, 7, 5, "orange"), (1, 2, 8, 7, 3, 13, "mud"), (2, 3, 9, 6, 6, 12, "red"),
        (9, 2, 9, 15, 3, 14, "mud"), (10, 3, 10, 14, 9, 11, "dleaf"), (3, 3, 14, 13, 4, 15, "wood")]),
    # ================================================== CAMPEMENT DE RANDONNEURS
    dict(id="hiker_tent", fr="Tente de randonneur", en="Hiker tent", elements=[
        (0, 0, 1, 16, 1, 15, "kaki_d"), (1, 1, 2, 15, 5, 14, "orange"), (3, 5, 3, 13, 9, 13, "orange"), (5, 9, 4, 11, 11, 12, "orange"), (6, 1, 0.8, 10, 4, 1.6, "carbon"), (0, 0, 7, 1, 12, 9, "steel"), (15, 0, 7, 16, 12, 9, "steel"), (2, 5, 12, 5, 6, 15, "red"),
        (11, 1, 14, 15, 4, 16, "denim")]),
    dict(id="camp_fire_ring", fr="Foyer en cercle de pierres", en="Camp fire ring", anim="fire", light=15, elements=[
        (2, 0, 2, 14, 1, 14, "ash"), (0, 0, 4, 3, 3, 12, "stone"), (13, 0, 4, 16, 3, 12, "stone"), (4, 0, 0, 12, 3, 3, "stone"), (4, 0, 13, 12, 3, 16, "stone"), (5, 1, 5, 11, 8, 11, "black", {"up": "anim", "north": "anim", "south": "anim", "east": "anim", "west": "anim"}),
        (4, 1, 7, 6, 3, 9, "dwood"), (10, 1, 7, 12, 3, 9, "dwood")]),
    dict(id="trail_marker_post", fr="Poteau de balisage", en="Trail marker post", elements=[
        (7, 0, 7, 9, 22, 9, "wood"), (5, 0, 5, 11, 2, 11, "stone"), (6, 14, 6.8, 10, 20, 9.2, "yellow"), (6.6, 15, 6.6, 9.4, 16, 6.8, "red"), (6.6, 18, 6.6, 9.4, 19, 6.8, "red"), (6, 22, 6, 10, 24, 10, "dwood"), (8, 8, 9, 14, 12, 9.6, "denim"),
        (9, 9, 9.6, 13, 10, 9.8, "star_white")]),
    dict(id="tree_hammock", fr="Hamac entre deux arbres", en="Tree hammock", elements=[
        (0, 0, 7, 3, 30, 9, "bark"), (13, 0, 7, 16, 30, 9, "bark"), (3, 16, 6, 13, 18, 10, "denim"), (2, 18, 6, 4, 26, 10, "rope"), (12, 18, 6, 14, 26, 10, "rope"), (4, 14, 6.4, 12, 16, 9.6, "kaki_d"), (5, 12, 6.8, 11, 14, 9.2, "orange"),
        (0, 28, 5, 3, 32, 11, "leaf"), (13, 28, 5, 16, 32, 11, "leaf")]),
    dict(id="rope_bridge_anchor", fr="Ancrage de pont de corde", en="Rope bridge anchor", elements=[
        (2, 0, 2, 14, 3, 14, "stone"), (3, 3, 3, 8, 14, 8, "wood"), (8, 3, 8, 13, 14, 13, "wood"), (3, 14, 3, 13, 16, 13, "dwood"), (8, 4, 7, 16, 6, 9, "rope"), (8, 11, 7, 16, 13, 9, "rope"), (14, 4, 6, 16, 14, 10, "wood"), (7, 16, 7, 9, 20, 9, "steel"),
        (0, 0, 6, 4, 2, 10, "mud")]),
    dict(id="refuge_bench", fr="Banc de refuge", en="Refuge bench", elements=[
        (0, 6, 4, 16, 8, 12, "lwood"), (1, 0, 4, 3, 6, 6, "wood"), (13, 0, 4, 15, 6, 6, "wood"), (1, 0, 10, 3, 6, 12, "wood"), (13, 0, 10, 15, 6, 12, "wood"), (0, 8, 10, 16, 16, 12, "lwood"), (1, 8, 10, 3, 18, 12, "wood"), (13, 8, 10, 15, 18, 12, "wood"),
        (6, 8.4, 5, 10, 9, 8, "red")]),
    # ================================================== RUINES ET VESTIGES
    dict(id="broken_column", fr="Colonne brisée", en="Broken column", elements=[
        (1, 0, 1, 15, 3, 15, "stone"), (3, 3, 3, 13, 5, 13, "ash"), (4, 5, 4, 12, 20, 12, "star_white"), (3, 20, 3, 13, 22, 13, "ash"), (5, 22, 5, 11, 26, 9, "star_white"), (9, 0, 0, 16, 4, 6, "ash"), (0, 0, 10, 6, 5, 16, "star_white"), (3, 8, 3.6, 7, 14, 4, "moss"),
        (9, 12, 11.6, 12, 18, 12.4, "moss")]),
    dict(id="overgrown_temple_arch", fr="Arc de temple envahi", en="Overgrown temple arch", elements=[
        (0, 0, 4, 4, 22, 12, "stone"), (12, 0, 4, 16, 22, 12, "stone"), (0, 22, 3, 16, 28, 13, "ash"), (4, 18, 4.4, 12, 22, 11.6, "ash"), (5, 16, 5, 11, 18, 11, "ash"), (1, 4, 3.6, 3, 16, 4, "moss"), (13, 8, 3.6, 15, 20, 4, "moss"), (0, 26, 2.6, 8, 28, 3, "leaf"),
        (10, 20, 12.4, 16, 28, 13.4, "dleaf"), (6, 28, 6, 10, 30, 10, "moss")]),
    dict(id="old_tomb_slab", fr="Dalle de tombe ancienne", en="Old tomb slab", elements=[
        (1, 0, 1, 15, 3, 15, "stone"), (2, 3, 2, 14, 6, 14, "ash"), (3, 6, 3, 13, 7, 13, "stone"), (7, 7, 4, 9, 9, 12, "slate"), (5, 9, 6, 11, 10, 8, "slate"), (6, 7, 6, 8, 8, 8, "moss"), (1, 3, 10, 6, 5, 14, "moss"), (10, 0, 0, 15, 2, 4, "ash")]),
    dict(id="mossy_statue_head", fr="Tête de statue moussue", en="Mossy statue head", elements=[
        (2, 0, 2, 14, 3, 14, "stone"), (3, 3, 3, 13, 18, 13, "ash"), (4, 18, 4, 12, 24, 12, "stone"), (5, 6, 2.4, 11, 10, 3, "ash"), (5, 12, 2.4, 7, 14, 3, "carbon"), (9, 12, 2.4, 11, 14, 3, "carbon"), (7, 14, 2.4, 9, 16, 3, "ash"), (3, 10, 3, 8, 18, 13, "moss"),
        (9, 4, 9, 13, 12, 13, "dleaf")]),
    dict(id="dry_stone_wall", fr="Mur en pierre sèche", en="Dry stone wall", elements=[
        (0, 0, 4, 16, 5, 12, "stone"), (0, 5, 5, 8, 9, 11, "ash"), (8, 5, 4, 16, 8, 12, "stone"), (2, 9, 6, 6, 11, 10, "ash"), (10, 8, 5, 14, 10, 11, "stone"), (0, 2, 3.6, 5, 4, 4, "slate"), (8, 1, 3.6, 12, 4, 4, "ash"), (4, 5, 3.8, 8, 7, 4.2, "moss"),
        (12, 3, 3.6, 16, 7, 4, "moss")]),
    dict(id="ruined_well", fr="Puits en ruine", en="Ruined well", elements=[
        (1, 0, 1, 15, 6, 15, "stone"), (3, 6, 3, 13, 9, 13, "ash"), (5, 5, 5, 11, 8, 11, "water"), (1, 6, 1, 5, 10, 5, "ash"), (11, 6, 11, 15, 12, 15, "ash"), (1, 6, 11, 4, 8, 15, "moss"), (11, 6, 1, 15, 8, 4, "stone"), (7, 8, 7, 9, 20, 9, "dwood"),
        (3, 18, 6, 13, 20, 10, "dwood"), (7.4, 11, 7.4, 8.6, 18, 8.6, "rope")]),
    # ================================================== MAGIE ET LÉGENDES
    dict(id="menhir_ring", fr="Cercle de menhirs", en="Menhir ring", elements=[
        (1, 0, 5, 4, 20, 11, "ash"), (12, 0, 5, 15, 22, 11, "stone"), (5, 0, 1, 11, 18, 4, "stone"), (5, 0, 12, 11, 24, 15, "ash"), (6, 0, 6, 10, 2, 10, "moss"), (7, 20, 6.4, 9, 22, 9.6, "amethyst"), (2, 6, 4.4, 3, 14, 5, "amethyst"), (13, 8, 4.4, 14, 16, 5, "amethyst"),
        (6, 12, 0.6, 10, 16, 1, "crystal")]),
    dict(id="runic_altar", fr="Autel runique", en="Runic altar", light=6, elements=[
        (1, 0, 3, 15, 4, 13, "slate"), (3, 4, 4, 13, 10, 12, "stone"), (2, 10, 3, 14, 13, 13, "slate"), (4, 13, 5, 12, 14, 11, "amethyst"), (5, 14, 6, 11, 15, 10, "crystal"), (5, 5, 3.6, 7, 9, 4, "crystal"), (9, 6, 3.6, 11, 10, 4, "amethyst"),
        (7, 3, 3.4, 9, 7, 4, "crystal"), (3, 13, 4, 5, 18, 6, "amethyst"), (11, 13, 10, 13, 17, 12, "crystal")]),
    dict(id="sacred_tree_shrine", fr="Arbre sacré et offrandes", en="Sacred tree shrine", elements=[
        (2, 0, 2, 14, 2, 14, "moss"), (6, 2, 6, 10, 24, 10, "bark"), (3, 2, 7, 7, 8, 9, "bark"), (9, 2, 7, 13, 8, 9, "bark"), (2, 24, 2, 14, 30, 14, "leaf"), (4, 30, 4, 12, 32, 12, "dleaf"), (5, 14, 5.4, 6, 20, 6, "red"), (10, 12, 5.4, 11, 18, 6, "star_white"),
        (6, 2, 11, 10, 4, 14, "stone"), (7, 4, 12, 9, 6, 13, "amethyst")]),
    dict(id="ancient_portal_arch", fr="Portail ancien", en="Ancient portal arch", anim="portal", light=12, elements=[
        (0, 0, 5, 4, 28, 11, "slate"), (12, 0, 5, 16, 28, 11, "slate"), (4, 24, 5, 12, 28, 11, "slate"), (4, 0, 5, 12, 4, 11, "slate"), (4, 4, 7.4, 12, 24, 8.6, "amethyst", {"north": "anim", "south": "anim"}), (1, 6, 4.6, 3, 8, 5, "crystal"), (13, 12, 4.6, 15, 14, 5, "crystal"),
        (1, 20, 4.6, 3, 22, 5, "crystal"), (6, 28, 6, 10, 32, 10, "stone")]),
    dict(id="firefly_swarm", fr="Nuée de lucioles", en="Firefly swarm", anim="fireflies", light=9, passable=True, elements=[
        (2, 4, 2, 14, 20, 14, "glass", {"north": "anim", "south": "anim", "east": "anim", "west": "anim", "up": "anim"}), (7, 8, 7, 9, 12, 9, "lamp"), (4, 12, 5, 5, 13, 6, "lamp"), (11, 6, 10, 12, 7, 11, "lamp"), (10, 16, 4, 11, 17, 5, "lamp")]),
    dict(id="witch_cauldron_wild", fr="Chaudron de sorcière en pleine nature", en="Wild witch cauldron", anim="fire", light=8, elements=[
        (3, 0, 3, 13, 2, 13, "stone"), (5, 2, 5, 11, 3, 11, "black", {"up": "anim"}), (2, 3, 2, 14, 12, 14, "carbon"), (1, 12, 1, 15, 14, 15, "dgray"), (3, 13, 3, 13, 13.8, 13, "leaf"), (5, 13.8, 5, 11, 14.2, 11, "emerald"), (1, 3, 7, 3, 9, 9, "dwood"),
        (13, 3, 7, 15, 9, 9, "dwood"), (6, 14, 6, 8, 16, 8, "bone"), (4, 2, 11, 6, 6, 13, "dwood")]),
    # ================================================== MINES ET GROTTES
    dict(id="ore_vein_rock", fr="Filon de minerai", en="Ore vein rock", elements=[
        (0, 0, 0, 16, 14, 16, "stone"), (1, 14, 1, 15, 16, 15, "slate"), (2, 4, 0.4, 6, 8, 1, "gold"), (7, 8, 0.4, 12, 12, 1, "crystal"), (9, 2, 0.4, 14, 5, 1, "gold"), (3, 10, 0.4, 5, 13, 1, "red"), (13, 6, 1.6, 15, 12, 4, "gold"), (0, 8, 11, 1, 12, 14, "gold"),
        (5, 14, 5, 9, 16, 9, "crystal")]),
    dict(id="mine_cart_wreck", fr="Wagonnet de mine abandonné", en="Abandoned mine cart", elements=[
        (0, 0, 2, 16, 2, 14, "steel"), (1, 2, 4, 15, 12, 12, "rust"), (0, 8, 3, 2, 12, 13, "rust"), (14, 8, 3, 16, 12, 13, "rust"), (3, 12, 5, 13, 14, 11, "stone"), (4, 14, 6, 8, 16, 10, "gold"), (1, 0, 3, 5, 3, 5, "wheel"), (11, 0, 3, 15, 3, 5, "wheel"),
        (1, 0, 11, 5, 3, 13, "wheel"), (11, 0, 11, 15, 3, 13, "wheel")]),
    dict(id="timber_support", fr="Étai de mine en bois", en="Timber support", elements=[
        (0, 0, 5, 3, 30, 11, "wood"), (13, 0, 5, 16, 30, 11, "wood"), (0, 26, 4, 16, 30, 12, "dwood"), (3, 12, 6, 13, 14, 10, "wood"), (1, 2, 4.6, 3, 4, 5, "stone"), (13, 20, 4.6, 15, 22, 5, "rust"), (5, 28, 3.6, 7, 29, 4, "rope"), (9, 24, 5, 11, 26, 11, "bark"),
        (0, 0, 4, 4, 3, 12, "soil")]),
    dict(id="cave_glow_crystals", fr="Cristaux lumineux de grotte", en="Cave glow crystals", light=11, elements=[
        (0, 0, 0, 16, 3, 16, "basalt"), (2, 3, 2, 6, 16, 6, "crystal"), (3, 16, 3, 5, 22, 5, "crystal"), (9, 3, 3, 13, 12, 7, "amethyst"), (10, 12, 4, 12, 18, 6, "amethyst"), (3, 3, 9, 7, 10, 13, "crystal"), (10, 3, 10, 14, 15, 14, "crystal"),
        (11, 15, 11, 13, 20, 13, "lamp"), (7, 3, 7, 9, 8, 9, "amethyst")]),
    dict(id="stalagmite_cluster", fr="Groupe de stalagmites", en="Stalagmite cluster", elements=[
        (0, 0, 0, 16, 2, 16, "slate"), (2, 2, 2, 7, 14, 7, "stone"), (3, 14, 3, 6, 20, 6, "ash"), (9, 2, 3, 13, 9, 7, "ash"), (10, 9, 4, 12, 13, 6, "stone"), (3, 2, 9, 7, 7, 13, "stone"), (9, 2, 9, 14, 17, 14, "ash"), (10, 17, 10, 13, 24, 13, "stone"),
        (7, 2, 7, 9, 5, 9, "slate")]),
    dict(id="miner_lantern_hook", fr="Lanterne de mineur accrochée", en="Miner lantern hook", anim="flicker", light=14, elements=[
        (7, 0, 7, 9, 20, 9, "wood"), (4, 20, 6.4, 12, 22, 9.6, "wood"), (10, 14, 6.4, 12, 22, 9.6, "rust"), (9, 8, 6, 13, 14, 10, "brass"), (10, 9, 6.4, 12, 13, 9.6, "lamp", {"north": "anim", "south": "anim", "east": "anim", "west": "anim"}), (9, 14, 6, 13, 15, 10, "steel"),
        (4, 0, 4, 12, 2, 12, "stone"), (4, 14, 7, 6, 20, 9, "rope")]),
    # ================================================== ÉPAVES ET CRASH
    dict(id="crashed_plane_wing", fr="Aile d'avion écrasé", en="Crashed plane wing", elements=[
        (0, 0, 2, 16, 3, 14, "steel"), (0, 3, 3, 16, 5, 13, "tin"), (2, 5, 4, 14, 6, 12, "steel"), (13, 3, 3, 16, 8, 6, "rust"), (0, 3, 10, 5, 4, 13, "red"), (7, 5, 5, 9, 12, 11, "tin"), (6, 12, 4, 10, 14, 12, "rust"), (10, 6, 7, 16, 8, 9, "steel"),
        (1, 4, 4, 4, 5, 6, "carbon")]),
    dict(id="rusted_car_wreck", fr="Voiture rouillée", en="Rusted car wreck", elements=[
        (0, 3, 2, 16, 6, 14, "rust"), (3, 6, 3, 11, 11, 13, "rust"), (4, 6, 2.4, 10, 10, 3, "carbon"), (3.4, 11, 3, 10.6, 12, 13, "orange"), (0, 0, 1, 4, 4, 4, "carbon"), (12, 0, 1, 16, 4, 4, "carbon"), (0, 0, 12, 4, 4, 15, "carbon"), (12, 0, 12, 16, 4, 15, "carbon"),
        (13, 6, 4, 16, 8, 12, "carbon"), (5, 11, 5, 9, 12, 9, "moss")]),
    dict(id="airdrop_crate", fr="Caisse de largage", en="Airdrop crate", elements=[
        (1, 0, 1, 15, 12, 15, "us_olive"), (0, 12, 0, 16, 14, 16, "dgray"), (0, 0, 0, 2, 12, 2, "dgray"), (14, 0, 0, 16, 12, 2, "dgray"), (0, 0, 14, 2, 12, 16, "dgray"), (14, 0, 14, 16, 12, 16, "dgray"), (4, 4, 0.4, 12, 8, 1, "yellow"),
        (6, 14, 6, 10, 15, 10, "steel"), (3, 14, 3, 5, 18, 5, "red"), (4, 7, 0.2, 7, 7.6, 1, "carbon")]),
    dict(id="tangled_parachute", fr="Parachute emmêlé", en="Tangled parachute", elements=[
        (0, 0, 0, 16, 2, 16, "star_white"), (1, 2, 2, 8, 5, 10, "orange"), (8, 2, 4, 15, 6, 13, "star_white"), (3, 5, 3, 10, 9, 8, "orange"), (9, 6, 6, 13, 10, 10, "star_white"), (0, 2, 11, 6, 4, 15, "orange"), (6, 9, 6, 10, 20, 10, "rope"),
        (4, 9, 4.6, 6, 18, 5, "rope"), (11, 9, 10, 13, 16, 11, "rope"), (7, 20, 7, 9, 22, 9, "steel")]),
    dict(id="distress_beacon", fr="Balise de détresse", en="Distress beacon", anim="blink", light=11, elements=[
        (4, 0, 4, 12, 2, 12, "steel"), (5, 2, 5, 11, 8, 11, "orange"), (6, 8, 6, 10, 10, 10, "carbon"), (7, 10, 7, 9, 22, 9, "steel"), (6, 22, 6, 10, 24, 10, "red", {"up": "anim"}), (6, 4, 4.4, 10, 7, 5, "carbon", {"north": "anim"}), (1, 0, 6, 4, 4, 10, "dgray"),
        (12, 0, 6, 15, 3, 10, "dgray")]),
    dict(id="shipwreck_ribs", fr="Côtes d'épave de bateau", en="Shipwreck ribs", elements=[
        (0, 0, 7, 16, 3, 9, "dwood"), (1, 3, 3, 3, 18, 5, "wood"), (1, 3, 11, 3, 18, 13, "wood"), (5, 3, 2, 7, 22, 4, "dwood"), (5, 3, 12, 7, 22, 14, "dwood"), (9, 3, 3, 11, 14, 5, "wood"), (9, 3, 11, 11, 14, 13, "wood"), (13, 3, 4, 15, 8, 6, "dwood"),
        (13, 3, 10, 15, 8, 12, "dwood"), (3, 16, 4, 14, 18, 5, "rust"), (4, 20, 3.4, 8, 22, 4.6, "moss")]),
]

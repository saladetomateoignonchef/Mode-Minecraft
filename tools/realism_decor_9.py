"""
Neuvième série : 12 plantes décoratives du réalisme naturel (lot 1 du cahier des charges des biomes).
  [26] Désert et Mesa : agave, cactus fleuris rouge et jaune, cactus tonneau, buisson sec, plante grasse en rosette.
  [27] Rivières et étangs : quenouilles hautes, roseaux, populage des marais, iris d'eau, nénuphars fleuris rose et blanc (posés sur l'eau).
Toutes sont traversables (sans collision). Posées dans le monde par NatureDecorFeature (thèmes arid_flora, shore_flora, pond_flora).
Importé par generate_realism_decor.py.
"""

PAL9 = {}   # aucune couleur nouvelle : les deux palettes sont pleines

DECOS9 = [
    # ================================================== [26] DÉSERT ET MESA
    dict(id="agave_plant", fr="Agave", en="Agave", passable=True, elements=[
        (6, 0, 6, 10, 4, 10, "moss"), (1, 2, 7.2, 7, 6, 8.8, "leaf"), (9, 2, 7.2, 15, 6, 8.8, "leaf"), (7.2, 2, 1, 8.8, 6, 7, "leaf"), (7.2, 2, 9, 8.8, 6, 15, "leaf"),
        (3, 4, 3, 7, 9, 5, "moss"), (9, 4, 11, 13, 9, 13, "moss"), (3, 4, 11, 5, 9, 13, "leaf"), (11, 4, 3, 13, 9, 5, "leaf"), (7, 4, 7, 9, 14, 9, "moss"), (7.4, 14, 7.4, 8.6, 20, 8.6, "dleaf")]),
    dict(id="flowering_cactus_red", fr="Cactus fleuri rouge", en="Red flowering cactus", passable=True, elements=[
        (5, 0, 5, 11, 12, 11, "green"), (6, 12, 6, 10, 14, 10, "dleaf"), (6, 5, 4.6, 7, 11, 5, "star_white"), (9, 7, 11, 10, 12, 11.4, "star_white"), (5.2, 14, 5.2, 10.8, 15.6, 10.8, "red"),
        (6.2, 15.6, 6.2, 9.8, 16, 9.8, "orange"), (4, 8, 7, 5, 11, 9, "red"), (11, 6, 7, 12, 9, 9, "red"), (7.4, 14, 7.4, 8.6, 16, 8.6, "yellow")]),
    dict(id="flowering_cactus_yellow", fr="Cactus fleuri jaune", en="Yellow flowering cactus", passable=True, elements=[
        (4, 0, 4, 9, 9, 9, "green"), (9, 0, 8, 13, 7, 12, "green"), (4.6, 9, 4.6, 8.4, 10, 8.4, "dleaf"), (3, 4, 6, 4, 7, 8, "star_white"), (9.5, 7, 8.5, 12.5, 8.2, 11.5, "dleaf"),
        (4, 10, 4, 9, 11.4, 9, "yellow"), (5.4, 11.4, 5.4, 7.6, 12, 7.6, "orange"), (9.4, 8.2, 8.4, 12.6, 9.2, 11.6, "yellow"), (11, 3, 8, 12, 5, 9, "star_white")]),
    dict(id="barrel_cactus", fr="Cactus tonneau", en="Barrel cactus", passable=True, elements=[
        (3, 0, 3, 13, 3, 13, "green"), (2, 3, 2, 14, 9, 14, "green"), (3, 9, 3, 13, 11, 13, "dleaf"), (7, 0, 2, 9, 11, 3, "dleaf"), (7, 0, 13, 9, 11, 14, "dleaf"), (2, 0, 7, 3, 11, 9, "dleaf"), (13, 0, 7, 14, 11, 9, "dleaf"),
        (5, 11, 5, 11, 12, 11, "yellow"), (6.4, 12, 6.4, 9.6, 13, 9.6, "red"), (4, 5, 1.6, 5, 7, 2, "star_white"), (11, 6, 14, 12, 8, 14.4, "star_white")]),
    dict(id="dry_shrub", fr="Buisson sec", en="Dry shrub", passable=True, elements=[
        (7.2, 0, 7.2, 8.8, 5, 8.8, "bark"), (3, 4, 7.4, 8, 6, 8.6, "wood"), (8, 5, 7.4, 13, 7, 8.6, "wood"), (7.4, 4, 3, 8.6, 6, 8, "wood"), (7.4, 5, 8, 8.6, 7, 13, "wood"),
        (2, 6, 6, 6, 10, 10, "hay"), (10, 7, 6, 14, 11, 10, "hay"), (6, 7, 2, 10, 11, 6, "hay"), (6, 7, 10, 10, 12, 14, "hay"), (6, 9, 6, 10, 13, 10, "tan")]),
    dict(id="succulent_rosette", fr="Plante grasse en rosette", en="Succulent rosette", passable=True, elements=[
        (5, 0, 5, 11, 2, 11, "soil"), (6, 2, 6, 10, 4, 10, "leaf"), (2, 2, 6.6, 6, 4, 9.4, "moss"), (10, 2, 6.6, 14, 4, 9.4, "moss"), (6.6, 2, 2, 9.4, 4, 6, "moss"), (6.6, 2, 10, 9.4, 4, 14, "moss"),
        (4, 4, 4, 7, 6, 7, "leaf"), (9, 4, 9, 12, 6, 12, "leaf"), (9, 4, 4, 12, 6, 7, "moss"), (4, 4, 9, 7, 6, 12, "moss"), (7, 5, 7, 9, 8, 9, "pink"), (7.4, 8, 7.4, 8.6, 9, 8.6, "magenta")]),
    # ================================================== [27] RIVIÈRES ET ÉTANGS
    dict(id="cattail_tall", fr="Quenouilles hautes", en="Tall cattails", passable=True, elements=[
        (2, 0, 3, 3, 30, 4, "moss"), (7, 0, 8, 8, 28, 9, "leaf"), (12, 0, 4, 13, 31, 5, "moss"), (4, 0, 11, 5, 26, 12, "leaf"), (10, 0, 12, 11, 29, 13, "moss"),
        (1.4, 20, 2.4, 3.6, 28, 4.6, "bark"), (6.4, 18, 7.4, 8.6, 26, 9.6, "bark"), (11.4, 22, 3.4, 13.6, 30, 5.6, "bark"), (9.4, 19, 11.4, 11.6, 27, 13.6, "bark"), (3.4, 17, 10.4, 5.6, 24, 12.6, "bark")]),
    dict(id="reed_bank", fr="Berge de roseaux", en="Reed bank", passable=True, elements=[
        (1, 0, 2, 2, 22, 3, "leaf"), (3, 0, 6, 4, 26, 7, "moss"), (5, 0, 1, 6, 20, 2, "moss"), (6, 0, 9, 7, 24, 10, "leaf"), (8, 0, 4, 9, 27, 5, "leaf"), (9, 0, 12, 10, 21, 13, "moss"),
        (11, 0, 7, 12, 25, 8, "moss"), (12, 0, 2, 13, 19, 3, "leaf"), (13, 0, 11, 14, 23, 12, "leaf"), (14, 0, 5, 15, 18, 6, "moss"), (2, 0, 12, 3, 17, 13, "leaf"), (7, 0, 14, 8, 20, 15, "moss")]),
    dict(id="marsh_marigold", fr="Populage des marais", en="Marsh marigold", passable=True, elements=[
        (7, 0, 7, 9, 5, 9, "leaf"), (3, 2, 6, 7, 4, 10, "moss"), (9, 2, 6, 13, 4, 10, "moss"), (6, 2, 3, 10, 4, 7, "leaf"), (6, 2, 9, 10, 4, 13, "leaf"),
        (4, 4, 4, 7, 7, 7, "yellow"), (9, 4, 9, 12, 8, 12, "yellow"), (9, 3, 3, 12, 6, 6, "yellow"), (4, 3, 10, 7, 6, 13, "yellow"), (5, 7, 5, 6, 8, 6, "orange"), (10, 8, 10, 11, 9, 11, "orange")]),
    dict(id="water_iris", fr="Iris d'eau", en="Water iris", passable=True, elements=[
        (7, 0, 7, 9, 10, 9, "moss"), (2, 0, 6, 4, 14, 8, "leaf"), (12, 0, 8, 14, 16, 10, "leaf"), (6, 0, 2, 8, 15, 4, "moss"), (8, 0, 12, 10, 13, 14, "moss"),
        (5, 10, 5, 11, 13, 11, "purple"), (4, 13, 6, 12, 15, 10, "magenta"), (6, 15, 6.4, 10, 17, 9.6, "purple"), (7, 12, 7, 9, 14, 9, "yellow")]),
    dict(id="pink_lily_pad", fr="Nénuphar à fleur rose", en="Pink water lily", passable=True, elements=[
        (1, 0, 1, 8, 0.6, 8, "leaf"), (8, 0, 2, 15, 0.6, 9, "moss"), (2, 0, 9, 9, 0.6, 15, "moss"), (10, 0, 10, 15, 0.6, 15, "leaf"),
        (4, 0.6, 4, 12, 2, 12, "pink"), (5, 2, 5, 11, 3.2, 11, "magenta"), (6.4, 3.2, 6.4, 9.6, 4, 9.6, "yellow")]),
    dict(id="white_lily_pad", fr="Nénuphar à fleur blanche", en="White water lily", passable=True, elements=[
        (1, 0, 3, 8, 0.6, 10, "moss"), (8, 0, 1, 15, 0.6, 8, "leaf"), (9, 0, 9, 15, 0.6, 15, "moss"), (1, 0, 11, 7, 0.6, 15, "leaf"),
        (5, 0.6, 5, 11, 2, 11, "star_white"), (6, 2, 6, 10, 3.2, 10, "cream"), (7, 3.2, 7, 9, 4, 9, "yellow")]),
    # ================================================== [36] SOURCES CHAUDES ET GEYSERS (évent : vapeur et régénération, voir SteamVentBlock)
    dict(id="hot_spring_vent", fr="Évent de source chaude", en="Hot spring vent", anim="steam", light=4, passable=True, elements=[
        (2, 0, 2, 14, 3, 14, "cream"), (4, 3, 4, 12, 5, 12, "bone"), (6, 5, 6, 10, 7, 10, "tan"), (7, 7, 7, 9, 8, 9, "dgray"), (5, 3, 5, 11, 4, 11, "teal"),
        (6, 7.4, 6, 10, 12, 10, "steam_w", {"up": "anim", "north": "anim", "south": "anim", "east": "anim", "west": "anim"}), (7, 12, 7, 9, 16, 9, "steam_w", {"north": "anim", "south": "anim", "east": "anim", "west": "anim"})]),
    # ================================================== [8] FLAQUE D'EAU (posée par la pluie, s'évapore au soleil : voir PuddleBlock)
    dict(id="water_puddle", fr="Flaque d'eau", en="Water puddle", passable=True, elements=[
        (1, 0, 2, 15, 0.7, 14, "water"), (3, 0, 0.5, 13, 0.7, 2, "water"), (3, 0, 14, 13, 0.7, 15.5, "water"), (0.5, 0, 4, 2, 0.7, 12, "water"), (14, 0, 4, 15.5, 0.7, 12, "water"),
        (5, 0.7, 6, 9, 0.8, 9, "sky")]),
]

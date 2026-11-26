"""
Septième série de décors : 96 objets détaillés pour les cartels de la jungle (bureau du narco-patron, salon kitsch, piscine et fête, chapelle,
laboratoire, plantations, blanchiment, contrebande et pistes, armurerie des sicarios, gardes et miradors, prison et interrogatoire, pièges,
camp de base, jungle et nature, enseignes et graffitis, musique et détente). Importé par generate_realism_decor.py.
"""
import random

from PIL import Image

from realism_decor_4 import table

PAL7 = {"mahogany": "#5c2a1c"}


def _f(n):
    return Image.new("RGBA", (16, 16 * n))


def anim_bubbling():
    """Cuve chimique : liquide vert qui bouillonne."""
    img = _f(8)
    rnd = random.Random(9)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (40, 150, 60, 255) if y > 3 else (30, 40, 34, 255))
        for _ in range(7):
            x, y = rnd.randint(1, 14), rnd.randint(4, 15)
            img.putpixel((x, y + 16 * f), (170, 255, 150, 255))
            if x < 15:
                img.putpixel((x + 1, y + 16 * f), (110, 220, 110, 255))
    return img, {"frametime": 3}


ANIMS7 = {"bubbling": anim_bubbling}

DECOS7 = [
    # ================================================== BUREAU DU NARCO-PATRON
    dict(id="patron_desk", fr="Bureau du narco-patron", en="Narco boss desk", light=5, elements=table(13, "mahogany", "dwood", 0, 16, 1, 15) + [
        (0, 13, 1, 16, 14, 15, "mahogany"), (2, 14, 4, 14, 14.4, 12, "wine"), (3, 14.4, 5, 7, 15, 9, "gold"), (4, 15, 6, 6, 17, 7, "gold"), (9, 14.4, 5, 13, 15.4, 8, "bloodred"), (10, 14.4, 9, 14, 20, 11, "lamp"),
        (10, 14.4, 9, 14, 15, 12, "brass"), (5, 3, 1, 11, 11, 2, "dwood"), (6, 4, 0.6, 10, 5, 1, "gold"), (6, 7, 0.6, 10, 8, 1, "gold")]),
    dict(id="boss_leather_chair", fr="Fauteuil de patron", en="Boss leather chair", elements=[
        (2, 0, 2, 14, 3, 14, "dgray"), (3, 3, 3, 13, 8, 13, "leather"), (2, 8, 11, 14, 26, 14, "leather"), (1, 8, 3, 3, 14, 13, "mahogany"), (13, 8, 3, 15, 14, 13, "mahogany"), (4, 26, 11.4, 12, 29, 13.6, "leather"), (6, 12, 10.6, 10, 22, 11, "gold"),
        (1, 0, 1, 4, 3, 4, "brass"), (12, 0, 12, 15, 3, 15, "brass")]),
    dict(id="gold_pistol_case", fr="Vitrine de pistolets dorés", en="Gold pistol case", light=4, elements=[
        (1, 0, 4, 15, 8, 12, "mahogany"), (1, 8, 4, 15, 15, 12, "glass"), (2, 8, 5, 14, 9, 11, "wine"), (3, 9, 6, 7, 11, 7, "gold"), (3, 11, 6, 5, 14, 7, "gold"), (9, 9, 6, 13, 11, 7, "gold"), (9, 11, 6, 11, 14, 7, "gold"),
        (1, 15, 4, 15, 16, 12, "mahogany"), (7, 3, 3.6, 9, 5, 4, "brass")]),
    dict(id="cigar_humidor", fr="Cave à cigares", en="Cigar humidor", elements=[
        (2, 0, 3, 14, 10, 13, "mahogany"), (2, 10, 3, 14, 12, 13, "dwood"), (3, 12, 4, 13, 13, 12, "gold"), (4, 13, 5, 8, 15, 7, "wood"), (9, 13, 7, 12, 14, 9, "leather"), (3, 3, 2.4, 13, 8, 3, "glass"), (4, 4, 3, 12, 7, 6, "tan"),
        (7, 1, 2.4, 9, 2, 3, "gold"), (11, 14, 9, 14, 15, 12, "ash")]),
    dict(id="patron_portrait", fr="Portrait du patron", en="Boss portrait", passable=True, elements=[
        (1, 1, 15, 15, 16, 16, "gold"), (2, 2, 14.6, 14, 15, 15, "mahogany"), (3, 3, 14.2, 13, 14, 14.6, "wine"), (6, 9, 14, 10, 13, 14.2, "tan"), (5, 5, 14, 11, 9, 14.2, "carbon"), (7, 12, 13.8, 9, 13, 14, "carbon"), (5, 11, 13.8, 7, 12, 14, "carbon"),
        (9, 11, 13.8, 11, 12, 14, "carbon"), (6, 3, 13.8, 10, 5, 14, "gold")]),
    dict(id="tequila_cabinet", fr="Meuble à tequila", en="Tequila cabinet", elements=[
        (1, 0, 3, 15, 30, 13, "mahogany"), (2, 2, 2.4, 14, 14, 3, "dwood"), (2, 15, 2.4, 14, 28, 3, "glass"), (3, 16, 3, 5, 24, 6, "lamp"), (7, 16, 3, 9, 26, 6, "tan"), (11, 16, 3, 13, 22, 6, "emerald"), (3, 4, 2.2, 13, 12, 2.6, "bloodred"),
        (7, 8, 2, 9, 10, 2.4, "gold"), (1, 14, 2.6, 15, 15.4, 13, "brass")]),
    # ================================================== SALON LUXUEUX ET KITSCH
    dict(id="leopard_sofa", fr="Canapé léopard", en="Leopard sofa", elements=[
        (0, 0, 2, 16, 4, 14, "gold"), (1, 4, 4, 15, 9, 14, "mustard"), (0, 4, 11, 16, 17, 14, "mustard"), (0, 4, 2, 3, 12, 14, "mustard"), (13, 4, 2, 16, 12, 14, "mustard"), (2, 9, 5, 5, 10, 8, "carbon"), (8, 9, 8, 11, 10, 11, "carbon"),
        (11, 9, 5, 13, 10, 7, "carbon"), (3, 4, 4, 13, 5, 5, "bloodred"), (1, 0, 1, 3, 2, 3, "brass"), (13, 0, 1, 15, 2, 3, "brass")]),
    dict(id="crystal_chandelier_big", fr="Grand lustre de cristal", en="Large crystal chandelier", light=14, passable=True, elements=[
        (7.4, 26, 7.4, 8.6, 32, 8.6, "dgray"), (4, 24, 4, 12, 26, 12, "gold"), (0, 20, 0, 16, 22, 16, "brass"), (6, 22, 6, 10, 24, 10, "glass"), (0, 22, 7, 2, 25, 9, "lamp"), (14, 22, 7, 16, 25, 9, "lamp"), (7, 22, 0, 9, 25, 2, "lamp"),
        (7, 22, 14, 9, 25, 16, "lamp"), (5, 16, 5, 11, 20, 11, "glass"), (7, 12, 7, 9, 16, 9, "glass")]),
    dict(id="stuffed_tiger", fr="Tigre empaillé", en="Stuffed tiger", elements=[
        (1, 0, 3, 15, 2, 13, "mahogany"), (2, 2, 4, 5, 8, 6, "orange"), (2, 2, 10, 5, 8, 12, "orange"), (10, 2, 4, 13, 8, 6, "orange"), (10, 2, 10, 13, 8, 12, "orange"), (2, 7, 3, 14, 13, 13, "orange"), (0, 8, 5, 4, 15, 11, "orange"),
        (0, 11, 6, 1, 12, 7, "carbon"), (0, 8, 5.4, 1, 10, 6.4, "star_white"), (4, 8, 3, 6, 13, 4, "carbon"), (8, 8, 3, 10, 13, 4, "carbon"), (13, 9, 7, 16, 12, 9, "orange")]),
    dict(id="golden_bust", fr="Buste doré du patron", en="Golden boss bust", elements=[
        (2, 0, 2, 14, 6, 14, "carbon"), (1, 6, 1, 15, 8, 15, "gold"), (4, 8, 4, 12, 15, 12, "gold"), (5, 15, 5, 11, 22, 11, "gold"), (6, 22, 6, 10, 24, 10, "gold"), (5, 18, 4.4, 11, 19, 5, "brass"), (6, 19, 4.4, 7, 20, 5, "carbon"),
        (9, 19, 4.4, 10, 20, 5, "carbon"), (6, 2, 1.4, 10, 5, 2, "gold")]),
    dict(id="giant_tv_wall", fr="Télé géante murale", en="Giant wall TV", anim="tv", light=8, passable=True, elements=[
        (0, 0, 13, 32, 18, 16, "carbon"), (1, 1, 12.4, 31, 17, 13, "carbon", {"north": "anim"}), (0, 0, 12, 32, 1, 13, "gold"), (0, 17, 12, 32, 18, 13, "gold"), (0, 1, 12, 1, 17, 13, "gold"), (31, 1, 12, 32, 17, 13, "gold"),
        (4, 0, 14, 8, 2, 16, "brass")]),
    dict(id="mini_bar_trolley", fr="Chariot mini-bar", en="Mini bar trolley", elements=[
        (1, 6, 3, 15, 8, 13, "gold"), (1, 14, 3, 15, 16, 13, "gold"), (1, 0, 3, 3, 14, 5, "brass"), (13, 0, 3, 15, 14, 5, "brass"), (1, 0, 11, 3, 14, 13, "brass"), (13, 0, 11, 15, 14, 13, "brass"), (2, 8, 4, 5, 14, 7, "emerald"),
        (6, 8, 5, 9, 12, 8, "lamp"), (10, 8, 4, 13, 13, 8, "bloodred"), (3, 16, 5, 7, 20, 9, "glass"), (9, 16, 6, 13, 18, 10, "glass"), (0, 0, 2, 3, 3, 4, "carbon")]),
    # ================================================== PISCINE ET FÊTE
    dict(id="pool_lounger", fr="Transat de piscine", en="Pool lounger", elements=[
        (2, 3, 1, 14, 5, 15, "star_white"), (2, 5, 1, 14, 7, 5, "star_white"), (2, 5, 4, 14, 8, 6, "denim"), (2, 0, 1, 4, 3, 3, "steel"), (12, 0, 1, 14, 3, 3, "steel"), (2, 0, 13, 4, 3, 15, "steel"), (12, 0, 13, 14, 3, 15, "steel"),
        (4, 5, 8, 12, 5.6, 14, "orange"), (6, 7, 2, 10, 8, 4, "cream")]),
    dict(id="pool_speaker_stack", fr="Enceintes de piscine", en="Pool speaker stack", elements=[
        (2, 0, 3, 14, 12, 13, "carbon"), (3, 12, 4, 13, 22, 12, "carbon"), (4, 2, 2.4, 12, 11, 3, "dgray"), (6, 5, 2.2, 10, 8, 2.6, "dgray"), (5, 14, 3.4, 11, 20, 4, "dgray"), (7, 16, 3.2, 9, 18, 3.6, "dgray"), (3, 22, 6, 13, 24, 10, "steel"),
        (13, 4, 6, 16, 6, 10, "carbon")]),
    dict(id="pinata_hanging", fr="Piñata suspendue", en="Hanging piñata", passable=True, elements=[
        (7.6, 24, 7.6, 8.4, 32, 8.4, "rope"), (3, 12, 4, 13, 24, 12, "pink"), (4, 14, 3.6, 12, 16, 4, "yellow"), (4, 18, 3.6, 12, 20, 4, "turquoise"), (2, 14, 6, 3, 22, 10, "orange"), (13, 14, 6, 14, 22, 10, "orange"), (1, 10, 7, 4, 12, 9, "red"),
        (12, 10, 7, 15, 12, 9, "red"), (6, 8, 7, 10, 12, 9, "magenta")]),
    dict(id="fiesta_banner", fr="Guirlande de fiesta", en="Fiesta banner", anim="garland", light=6, passable=True, elements=[
        (0, 13, 7.4, 16, 14, 8.6, "rope", {"north": "anim", "south": "anim"}), (0, 6, 7, 1, 16, 9, "wood"), (15, 6, 7, 16, 16, 9, "wood"), (2, 8, 7.4, 5, 13, 8.6, "pink"), (6, 8, 7.4, 9, 13, 8.6, "yellow"), (10, 8, 7.4, 13, 13, 8.6, "turquoise"),
        (13, 9, 7.4, 15, 13, 8.6, "orange")]),
    dict(id="champagne_fountain", fr="Fontaine de champagne", en="Champagne fountain", anim="fountain", elements=[
        (1, 0, 1, 15, 4, 15, "gold"), (2, 4, 2, 14, 6, 14, "glass"), (3, 5, 3, 13, 6, 13, "lamp", {"up": "anim"}), (4, 6, 4, 12, 8, 12, "gold"), (5, 8, 5, 11, 10, 11, "glass"), (6, 9, 6, 10, 10, 10, "lamp", {"up": "anim"}),
        (6, 10, 6, 10, 12, 10, "gold"), (7, 12, 7, 9, 16, 9, "lamp", {"north": "anim", "south": "anim", "east": "anim", "west": "anim"}), (7, 16, 7, 9, 17, 9, "gold")]),
    dict(id="pool_bar_stools", fr="Tabourets de bar de piscine", en="Pool bar stools", elements=[
        (1, 0, 5, 5, 1, 9, "gold"), (2.6, 1, 6.6, 3.4, 8, 7.4, "gold"), (1, 8, 5, 5, 10, 9, "orange"), (6, 0, 5, 10, 1, 9, "gold"), (7.6, 1, 6.6, 8.4, 8, 7.4, "gold"), (6, 8, 5, 10, 10, 9, "turquoise"), (11, 0, 5, 15, 1, 9, "gold"),
        (12.6, 1, 6.6, 13.4, 8, 7.4, "gold"), (11, 8, 5, 15, 10, 9, "pink")]),
    # ================================================== CHAPELLE ET RITUELS
    dict(id="santa_muerte_altar", fr="Autel de la Santa Muerte", en="Santa Muerte altar", anim="flicker", light=9, elements=[
        (0, 0, 3, 16, 10, 13, "carbon"), (0, 10, 3, 16, 11, 13, "bloodred"), (2, 11, 4, 14, 12, 12, "gold"), (5, 12, 6, 11, 24, 10, "bone"), (6, 24, 6.4, 10, 29, 9.6, "bone"), (5, 14, 5, 11, 15, 11, "purple"), (3, 12, 5, 5, 16, 7, "lamp", {"north": "anim"}),
        (11, 12, 5, 13, 16, 7, "lamp", {"north": "anim"}), (7, 25, 5.8, 9, 27, 6.2, "carbon"), (4, 12, 8, 6, 13, 10, "flame"), (6.4, 28, 5.6, 9.6, 30, 6, "gold")]),
    dict(id="ex_voto_wall", fr="Mur d'ex-voto", en="Ex-voto wall", passable=True, elements=[
        (0, 0, 15, 16, 16, 16, "dwood"), (1, 9, 14.4, 5, 13, 15, "gold"), (6, 10, 14.4, 9, 14, 15, "brass"), (10, 8, 14.4, 15, 12, 15, "gold"), (2, 3, 14.4, 6, 7, 15, "brass"), (7, 2, 14.4, 11, 6, 15, "gold"),
        (12, 3, 14.4, 15, 7, 15, "brass"), (3, 10, 14, 4, 12, 14.4, "bloodred"), (8, 11, 14, 9, 13, 14.4, "emerald"), (12, 9, 14, 14, 11, 14.4, "bloodred")]),
    dict(id="saint_statue", fr="Statue de saint", en="Saint statue", elements=[
        (3, 0, 3, 13, 4, 13, "stone"), (5, 4, 5, 11, 8, 11, "ash"), (4, 8, 4, 12, 22, 12, "star_white"), (5, 22, 5, 11, 28, 11, "star_white"), (6, 28, 6, 10, 30, 10, "gold"), (2, 14, 6, 4, 22, 10, "star_white"), (12, 14, 6, 14, 22, 10, "star_white"),
        (6, 24, 4.6, 10, 26, 5, "carbon"), (7, 10, 3.6, 9, 20, 4, "gold")]),
    dict(id="tall_candle_stand", fr="Grand chandelier à cierges", en="Tall candle stand", anim="flicker", light=13, elements=[
        (4, 0, 4, 12, 2, 12, "brass"), (7, 2, 7, 9, 22, 9, "brass"), (2, 22, 6, 14, 24, 10, "brass"), (2, 24, 7, 4, 29, 9, "wax", {"up": "anim"}), (7, 24, 7, 9, 31, 9, "wax", {"up": "anim"}), (12, 24, 7, 14, 29, 9, "wax", {"up": "anim"}),
        (2, 29, 7.4, 4, 31, 8.6, "flame"), (7, 31, 7.4, 9, 32, 8.6, "flame"), (12, 29, 7.4, 14, 31, 8.6, "flame")]),
    dict(id="offering_table", fr="Table d'offrandes", en="Offering table", elements=table(11, "mahogany", "dwood", 0, 16, 2, 14) + [
        (1, 11, 3, 15, 11.6, 13, "bloodred"), (2, 11.6, 4, 5, 15, 8, "orange"), (6, 11.6, 5, 9, 14, 8, "lamp"), (10, 11.6, 4, 14, 13, 7, "green"), (3, 11.6, 9, 6, 15, 12, "bone"), (8, 11.6, 9, 13, 14, 12, "gold"), (7, 11.6, 6, 8, 12.8, 7, "bloodred")]),
    dict(id="prayer_bench", fr="Banc de prière", en="Prayer bench", elements=[
        (0, 4, 1, 16, 7, 5, "mahogany"), (0, 7, 1, 16, 22, 3, "mahogany"), (0, 0, 1, 2, 4, 5, "dwood"), (14, 0, 1, 16, 4, 5, "dwood"), (1, 7, 5, 15, 8, 9, "bloodred"), (2, 0, 9, 14, 2, 14, "carbon"), (0, 22, 1, 16, 23, 3, "gold"),
        (6, 8, 5.4, 10, 9, 8.6, "bone")]),
    # ================================================== LABORATOIRE DE DROGUE
    dict(id="chemical_vat", fr="Cuve chimique", en="Chemical vat", anim="bubbling", light=4, elements=[
        (2, 0, 2, 14, 2, 14, "dgray"), (3, 2, 3, 13, 16, 13, "steel"), (4, 8, 2.6, 12, 14, 3, "glass", {"north": "anim"}), (3, 16, 3, 13, 18, 13, "dgray"), (6, 18, 6, 10, 22, 10, "steel"), (7, 22, 7, 9, 26, 9, "chrome"), (13, 4, 6, 16, 6, 10, "steel"),
        (15, 4, 6, 16, 12, 10, "steel"), (1, 6, 6, 3, 12, 10, "green")]),
    dict(id="flask_rack", fr="Support de fioles", en="Flask rack", elements=[
        (0, 0, 4, 16, 3, 12, "wood"), (1, 3, 5, 3, 22, 11, "steel"), (13, 3, 5, 15, 22, 11, "steel"), (1, 20, 5, 15, 22, 11, "steel"), (3, 3, 6, 6, 12, 10, "glass"), (3, 3.4, 6.4, 6, 8, 9.6, "green"), (7, 3, 6, 10, 14, 10, "glass"),
        (7, 3.4, 6.4, 10, 10, 9.6, "orange"), (11, 3, 6, 13, 10, 10, "glass"), (11, 3.4, 6.4, 13, 7, 9.6, "pink"), (4, 12, 7, 6, 18, 9, "lamp")]),
    dict(id="packing_table", fr="Table de conditionnement", en="Packing table", elements=table(12, "steel", "dgray", 0, 16, 2, 14) + [
        (1, 12, 3, 15, 12.4, 13, "chrome"), (2, 12.4, 4, 6, 14, 8, "star_white"), (3, 14, 5, 5, 15, 7, "cream"), (8, 12.4, 4, 12, 13.4, 8, "star_white"), (9, 13.4, 5, 11, 14.4, 7, "cream"), (2, 12.4, 9, 7, 16, 12, "cardboard"),
        (9, 12.4, 9, 14, 14, 12, "carbon"), (10, 14, 10, 13, 15, 11, "chrome")]),
    dict(id="fume_hood", fr="Hotte chimique", en="Fume hood", elements=[
        (0, 0, 3, 16, 12, 13, "steel"), (0, 12, 3, 16, 14, 13, "chrome"), (1, 14, 4, 15, 28, 12, "glass"), (0, 14, 3, 1, 28, 13, "steel"), (15, 14, 3, 16, 28, 13, "steel"), (0, 28, 3, 16, 30, 13, "steel"), (5, 30, 5, 11, 32, 11, "dgray"),
        (3, 14, 5, 7, 18, 9, "glass"), (3, 14.4, 5.4, 7, 16, 8.6, "green"), (9, 14, 6, 13, 16, 10, "dgray")]),
    dict(id="chemical_drums", fr="Fûts de produits chimiques", en="Chemical drums", elements=[
        (0, 0, 1, 7, 12, 8, "us_olive"), (8, 0, 2, 15, 12, 9, "teal"), (3, 0, 9, 10, 12, 16, "us_olive"), (0, 4, 1, 7, 5, 8, "steel"), (8, 4, 2, 15, 5, 9, "steel"), (3, 8, 9, 10, 9, 16, "steel"), (0, 12, 1, 7, 13, 8, "carbon"),
        (1, 6, 0.6, 6, 9, 1, "hazard"), (9, 6, 1.6, 14, 9, 2, "hazard"), (4, 6, 8.6, 9, 9, 9, "hazard")]),
    dict(id="pill_press", fr="Presse à comprimés", en="Pill press", elements=[
        (1, 0, 3, 15, 12, 13, "dgray"), (2, 12, 4, 14, 14, 12, "steel"), (6, 14, 5, 10, 24, 9, "chrome"), (2, 20, 5, 14, 24, 9, "steel"), (3, 14, 5, 5, 20, 7, "steel"), (11, 14, 5, 13, 20, 7, "steel"), (5, 24, 5, 11, 28, 9, "dgray"),
        (7, 28, 6, 9, 30, 8, "bloodred"), (4, 12, 2.4, 12, 14, 3, "cream"), (13, 14, 8, 15, 18, 11, "hazard")]),
    # ================================================== PLANTATIONS
    dict(id="coca_plants", fr="Plants de coca", en="Coca plants", elements=[
        (0, 0, 2, 16, 4, 14, "soil"), (2, 4, 3, 5, 14, 6, "leaf"), (3, 14, 4, 4, 18, 5, "leaf"), (7, 4, 3, 10, 16, 6, "dleaf"), (8, 16, 4, 9, 20, 5, "dleaf"), (12, 4, 3, 15, 12, 6, "leaf"), (2, 4, 9, 5, 12, 12, "dleaf"),
        (7, 4, 9, 10, 14, 12, "leaf"), (12, 4, 9, 15, 16, 12, "dleaf"), (3, 12, 10, 4, 15, 11, "leaf")]),
    dict(id="leaf_drying_rack", fr="Séchoir à feuilles", en="Leaf drying rack", elements=[
        (0, 0, 3, 2, 18, 5, "wood"), (14, 0, 3, 16, 18, 5, "wood"), (0, 0, 11, 2, 18, 13, "wood"), (14, 0, 11, 16, 18, 13, "wood"), (0, 5, 3, 16, 6, 13, "wood"), (0, 11, 3, 16, 12, 13, "wood"), (1, 6, 4, 15, 7, 12, "dleaf"),
        (2, 12, 5, 14, 13, 11, "leaf"), (3, 6.4, 5, 7, 7.4, 9, "leaf"), (9, 12.4, 6, 13, 13.4, 10, "dleaf")]),
    dict(id="camouflaged_greenhouse", fr="Serre camouflée", en="Camouflaged greenhouse", light=6, elements=[
        (0, 0, 2, 16, 2, 14, "soil"), (1, 2, 3, 15, 10, 13, "glass"), (1, 10, 3, 15, 12, 13, "camo_g"), (3, 12, 4, 13, 14, 12, "camo_g"), (5, 14, 5, 11, 15, 11, "camo_sand"), (2, 2, 4, 6, 8, 8, "leaf"), (8, 2, 5, 12, 9, 9, "dleaf"),
        (3, 2, 10, 7, 7, 12, "leaf"), (6, 2, 2.4, 10, 8, 3, "dgray"), (1, 2, 3, 2, 12, 13, "dwood"), (14, 2, 3, 15, 12, 13, "dwood")]),
    dict(id="irrigation_pipes_j", fr="Tuyaux d'irrigation", en="Irrigation pipes", passable=True, elements=[
        (0, 1, 6, 16, 4, 10, "teal"), (1, 4, 7, 3, 8, 9, "teal"), (7, 4, 7, 9, 10, 9, "teal"), (13, 4, 7, 15, 8, 9, "teal"), (0, 0, 5, 3, 1, 11, "soil"), (6, 0, 5, 10, 1, 11, "soil"), (13, 0, 5, 16, 1, 11, "soil"), (2, 2, 5.6, 4, 3, 6.4, "water"),
        (6, 10, 6, 10, 12, 10, "chrome")]),
    dict(id="leaf_sacks", fr="Sacs de feuilles", en="Leaf sacks", elements=[
        (0, 0, 2, 8, 9, 10, "cardboard"), (8, 0, 3, 16, 8, 11, "cream"), (3, 9, 3, 11, 15, 9, "cardboard"), (1, 0, 10, 9, 7, 16, "cream"), (9, 0, 11, 16, 9, 16, "cardboard"), (1, 5, 3, 7, 6, 3.6, "rope"), (9, 4, 3.6, 15, 5, 4.2, "rope"),
        (5, 14, 4, 9, 18, 8, "leaf"), (3, 6, 10.6, 7, 8, 11, "green")]),
    dict(id="press_tub", fr="Cuve de pressage", en="Press tub", elements=[
        (1, 0, 1, 15, 8, 15, "wood"), (0, 8, 0, 16, 10, 16, "dwood"), (2, 8, 2, 14, 9, 14, "dleaf"), (6, 10, 6, 10, 26, 10, "wood"), (1, 24, 5, 15, 26, 11, "wood"), (3, 26, 6, 5, 30, 10, "wood"), (11, 26, 6, 13, 30, 10, "wood"),
        (3, 8.4, 3, 7, 9.4, 7, "leaf"), (9, 8.4, 9, 13, 9.4, 13, "leaf")]),
    # ================================================== BLANCHIMENT ET COMPTABILITÉ
    dict(id="bill_counter_machine", fr="Compteuse de billets", en="Bill counting machine", anim="blink", light=4, elements=table(11, "dgray", "carbon", 1, 15, 2, 14) + [
        (3, 11, 4, 13, 14, 12, "star_white"), (4, 14, 5, 12, 15, 11, "carbon"), (5, 14.4, 5.4, 9, 15, 6.6, "lamp", {"up": "anim"}), (4, 11, 2.4, 12, 14, 3, "carbon"), (5, 12, 2.2, 8, 13, 2.6, "green"), (9, 12, 2.2, 11, 13, 2.6, "lamp"),
        (1, 11, 6, 3, 16, 10, "green")]),
    dict(id="dollar_pallet", fr="Palette de dollars", en="Dollar pallet", elements=[
        (0, 0, 1, 16, 3, 15, "dwood"), (1, 3, 2, 15, 12, 14, "green"), (1, 12, 2, 15, 13, 14, "emerald"), (2, 3, 1.6, 14, 4, 2, "rope"), (2, 8, 1.6, 14, 9, 2, "rope"), (3, 13, 4, 8, 15, 9, "green"), (9, 13, 5, 13, 16, 10, "green"),
        (6, 5, 1.2, 10, 10, 1.6, "star_white"), (1, 3, 8, 15, 4, 8.6, "rope")]),
    dict(id="ledger_desk", fr="Bureau des comptes", en="Ledger desk", elements=table(12, "mahogany", "dwood", 0, 16, 2, 14) + [
        (0, 12, 2, 16, 13, 14, "leather"), (2, 13, 4, 8, 13.4, 12, "paper"), (3, 13.4, 5, 7, 14.4, 11, "book_a"), (10, 13, 4, 14, 14, 8, "carbon"), (11, 14, 5, 13, 15, 7, "steel"), (11, 13, 9, 14, 17, 12, "lamp"), (10, 13, 9, 15, 14, 13, "brass")]),
    dict(id="cash_duffel_bags", fr="Sacs de sport remplis de billets", en="Cash duffel bags", elements=[
        (0, 0, 2, 8, 6, 12, "carbon"), (8, 0, 3, 16, 5, 13, "carbon"), (2, 6, 4, 10, 9, 10, "carbon"), (1, 1, 1.6, 7, 5, 2, "star_white"), (3, 5, 4.4, 8, 6, 5, "green"), (4, 9, 5, 8, 10, 8, "green"), (9, 5, 6, 14, 6, 9, "green"),
        (0, 5, 5, 8, 6, 9, "dgray"), (11, 0, 12, 15, 4, 16, "carbon")]),
    dict(id="paper_shredder", fr="Déchiqueteuse", en="Paper shredder", elements=[
        (3, 0, 3, 13, 14, 13, "dgray"), (2, 14, 2, 14, 16, 14, "carbon"), (4, 16, 6, 12, 17, 10, "steel"), (4, 16, 6, 5, 19, 10, "paper"), (7, 16, 6, 8, 20, 10, "paper"), (10, 16, 6, 11, 18, 10, "paper"), (5, 6, 2.4, 11, 10, 3, "carbon"),
        (7, 7, 2.2, 9, 8, 2.6, "lamp"), (4, 1, 3, 12, 6, 3.6, "paper")]),
    dict(id="safe_deposit_wall", fr="Mur de coffres", en="Safe deposit wall", passable=True, elements=[
        (0, 0, 13, 16, 16, 16, "steel"), (1, 1, 12.6, 7, 7, 13, "dgray"), (9, 1, 12.6, 15, 7, 13, "dgray"), (1, 9, 12.6, 7, 15, 13, "dgray"), (9, 9, 12.6, 15, 15, 13, "dgray"), (3, 3, 12.2, 5, 5, 12.6, "brass"), (11, 3, 12.2, 13, 5, 12.6, "brass"),
        (3, 11, 12.2, 5, 13, 12.6, "brass"), (11, 11, 12.2, 13, 13, 12.6, "brass")]),
    # ================================================== CONTREBANDE ET PISTES
    dict(id="airstrip_beacon", fr="Balise de piste", en="Airstrip beacon", anim="blink", light=12, elements=[
        (5, 0, 5, 11, 2, 11, "dgray"), (7, 2, 7, 9, 14, 9, "steel"), (5, 14, 5, 11, 20, 11, "carbon"), (6, 15, 4.6, 10, 19, 5, "lamp", {"north": "anim"}), (6, 15, 11, 10, 19, 11.4, "lamp", {"south": "anim"}), (4, 20, 4, 12, 21, 12, "dgray"),
        (7, 21, 7, 9, 24, 9, "red"), (0, 0, 7, 5, 4, 9, "orange")]),
    dict(id="jungle_fuel_drums", fr="Fûts de carburant de la jungle", en="Jungle fuel drums", elements=[
        (0, 0, 1, 7, 12, 8, "rust"), (8, 0, 2, 15, 12, 9, "bloodred"), (3, 0, 9, 10, 12, 16, "rust"), (0, 4, 1, 7, 5, 8, "steel"), (8, 4, 2, 15, 5, 9, "steel"), (3, 8, 9, 10, 9, 16, "steel"), (0, 12, 1, 7, 13, 8, "carbon"),
        (8, 12, 2, 15, 13, 9, "carbon"), (1, 6, 0.6, 6, 9, 1, "yellow"), (11, 12, 4, 13, 18, 6, "steel"), (11, 18, 4, 14, 19, 8, "carbon")]),
    dict(id="contraband_crate_stack", fr="Pile de caisses de contrebande", en="Contraband crate stack", elements=[
        (0, 0, 1, 8, 8, 9, "wood"), (8, 0, 2, 16, 7, 10, "dwood"), (3, 8, 2, 11, 14, 8, "wood"), (1, 0, 10, 9, 6, 16, "dwood"), (10, 0, 11, 16, 9, 16, "wood"), (1, 3, 0.6, 7, 4, 1, "rope"), (3, 10, 1.6, 6, 11, 2, "bloodred"),
        (11, 4, 10.6, 14, 5, 11, "carbon"), (4, 3, 10, 8, 4, 10.6, "rust"), (11, 9, 12, 14, 12, 15, "cardboard")]),
    dict(id="boat_motor_rack", fr="Rack de moteurs hors-bord", en="Outboard motor rack", elements=[
        (0, 0, 4, 16, 3, 12, "dwood"), (1, 3, 5, 3, 22, 11, "steel"), (13, 3, 5, 15, 22, 11, "steel"), (1, 20, 5, 15, 22, 11, "steel"), (3, 6, 6, 7, 18, 10, "carbon"), (3, 18, 5, 7, 20, 11, "dgray"), (4, 2, 7, 6, 6, 9, "steel"), (9, 6, 6, 13, 18, 10, "dgray"),
        (9, 18, 5, 13, 20, 11, "carbon"), (10, 2, 7, 12, 6, 9, "steel")]),
    dict(id="wind_sock", fr="Manche à air", en="Wind sock", elements=[
        (6, 0, 6, 10, 2, 10, "dgray"), (7.4, 2, 7.4, 8.6, 30, 8.6, "steel"), (8.6, 24, 7, 16, 28, 9, "orange"), (11, 24, 6.6, 13, 28, 9.4, "star_white"), (13, 24.4, 6.4, 16, 27.6, 9.6, "orange"), (7, 30, 7, 9, 31, 9, "red"), (8.6, 28, 7.4, 16, 28.4, 8.6, "steel")]),
    dict(id="cargo_net_bundle", fr="Ballot de cargaison sous filet", en="Netted cargo bundle", elements=[
        (0, 0, 2, 16, 10, 14, "cardboard"), (2, 10, 4, 14, 14, 12, "cream"), (0, 0, 2, 16, 1, 14, "rope"), (0, 4, 1.6, 16, 5, 2.4, "rope"), (0, 8, 1.6, 16, 9, 2.4, "rope"), (4, 0, 1.6, 5, 11, 2.4, "rope"), (8, 0, 1.6, 9, 11, 2.4, "rope"),
        (12, 0, 1.6, 13, 11, 2.4, "rope"), (3, 10, 4, 6, 12, 7, "green")]),
    # ================================================== ARMURERIE DES SICARIOS
    dict(id="ak_wall_rack", fr="Râtelier d'AK des sicarios", en="Sicario AK rack", elements=[
        (0, 0, 5, 16, 3, 11, "dwood"), (0, 24, 5, 16, 26, 11, "dwood"), (0, 3, 5, 2, 24, 11, "mahogany"), (14, 3, 5, 16, 24, 11, "mahogany"), (2, 4, 7.4, 3.4, 22, 8.6, "carbon"), (4.6, 4, 7.4, 6, 22, 8.6, "carbon"), (7.2, 4, 7.4, 8.6, 22, 8.6, "carbon"),
        (9.8, 4, 7.4, 11.2, 22, 8.6, "carbon"), (12.4, 4, 7.4, 13.8, 22, 8.6, "carbon"), (2, 4, 7, 3.4, 12, 9, "wood"), (4.6, 4, 7, 6, 12, 9, "wood"), (7.2, 4, 7, 8.6, 12, 9, "wood"), (9.8, 4, 7, 11.2, 12, 9, "wood")]),
    dict(id="ammo_belt_crates", fr="Caisses de bandes de munitions", en="Ammo belt crates", elements=[
        (0, 0, 1, 10, 7, 9, "us_olive"), (6, 0, 8, 16, 7, 16, "us_olive"), (2, 7, 2, 12, 12, 8, "kaki_d"), (2, 12, 3, 12, 13, 7, "brass"), (4, 13, 4, 10, 14, 6, "brass"), (0, 3, 1, 10, 4, 9, "dgray"), (6, 3, 8, 16, 4, 16, "dgray"),
        (2, 5, 0.6, 8, 6, 1, "paper"), (9, 4, 7.6, 13, 5, 8, "paper"), (12, 7, 9, 15, 9, 12, "brass")]),
    dict(id="rpg_cartel_rack", fr="Support de lance-roquettes du cartel", en="Cartel RPG rack", elements=[
        (0, 0, 4, 16, 3, 12, "dwood"), (1, 3, 5, 3, 22, 11, "mahogany"), (13, 3, 5, 15, 22, 11, "mahogany"), (1, 20, 5, 15, 22, 11, "mahogany"), (3, 6, 6.6, 6, 16, 9.4, "us_olive"), (3, 16, 6.2, 6, 20, 9.8, "dgray"), (7, 6, 6.6, 10, 16, 9.4, "us_olive"),
        (7, 16, 6.2, 10, 20, 9.8, "dgray"), (11, 6, 6.6, 13, 16, 9.4, "us_olive"), (11, 16, 6.2, 13, 20, 9.8, "dgray")]),
    dict(id="sicario_vest_rack", fr="Gilets des sicarios", en="Sicario vest rack", elements=[
        (1, 0, 5, 15, 2, 11, "dgray"), (2, 2, 7, 4, 28, 9, "steel"), (12, 2, 7, 14, 28, 9, "steel"), (1, 26, 6, 15, 28, 10, "steel"), (3, 10, 6, 7, 26, 10, "carbon"), (9, 10, 6, 13, 26, 10, "camo_g"), (4, 12, 5.4, 6, 16, 6, "padded"),
        (10, 12, 5.4, 12, 16, 6, "padded"), (3, 20, 5.4, 7, 21, 6, "bloodred"), (9, 20, 5.4, 13, 21, 6, "gold")]),
    dict(id="sicario_target_dummy", fr="Mannequin de tir des sicarios", en="Sicario target dummy", elements=[
        (3, 0, 3, 13, 2, 13, "dwood"), (7, 2, 7, 9, 16, 9, "wood"), (4, 16, 6, 12, 26, 10, "camo_g"), (5, 26, 6.4, 11, 29, 9.6, "tan"), (2, 18, 6.4, 4, 24, 9.6, "camo_g"), (12, 18, 6.4, 14, 24, 9.6, "camo_g"), (5, 20, 5.6, 11, 24, 6, "bloodred"),
        (6, 21, 5.4, 10, 23, 5.8, "carbon"), (5, 28, 6, 11, 29, 10, "carbon")]),
    dict(id="gun_cleaning_bench", fr="Établi de nettoyage d'armes", en="Gun cleaning bench", elements=table(12, "wood", "dwood", 0, 16, 2, 14) + [
        (1, 12, 3, 8, 12.4, 13, "star_white"), (2, 12.4, 4, 7, 13.4, 6, "carbon"), (2, 12.4, 8, 6, 13, 10, "dgray"), (3, 12.4, 11, 5, 14, 12, "tin"), (10, 12, 3, 14, 15, 7, "tin"), (11, 15, 4, 13, 16, 6, "carbon"), (10, 12, 9, 15, 14, 12, "us_olive"),
        (6, 4, 1.2, 10, 11, 2, "padded")]),
    # ================================================== GARDES ET MIRADORS
    dict(id="bamboo_watchtower", fr="Mirador en bambou", en="Bamboo watchtower", elements=[
        (2, 0, 2, 4, 26, 4, "leaf"), (12, 0, 2, 14, 26, 4, "leaf"), (2, 0, 12, 4, 26, 14, "leaf"), (12, 0, 12, 14, 26, 14, "leaf"), (2, 8, 4, 14, 9, 12, "leaf"), (2, 16, 4, 14, 17, 12, "leaf"), (0, 26, 0, 16, 28, 16, "wood"),
        (1, 28, 1, 15, 32, 2, "dleaf"), (1, 28, 14, 15, 32, 15, "dleaf"), (1, 28, 2, 2, 32, 14, "dleaf"), (14, 28, 2, 15, 32, 14, "dleaf"), (7, 28, 7, 9, 30, 9, "steel")]),
    dict(id="jungle_floodlight", fr="Projecteur de la jungle", en="Jungle floodlight", light=15, elements=[
        (4, 0, 4, 12, 2, 12, "dgray"), (7, 2, 7, 9, 26, 9, "wood"), (3, 22, 4, 13, 30, 12, "dgray"), (4, 23, 3.4, 12, 29, 4, "lamp"), (2, 24, 6, 3, 28, 10, "steel"), (13, 24, 6, 14, 28, 10, "steel"), (6, 30, 6, 10, 31, 10, "carbon"),
        (9, 0, 6, 15, 4, 10, "dleaf"), (10, 4, 7, 14, 5, 9, "leaf")]),
    dict(id="guard_dog_kennel", fr="Chenil des chiens de garde", en="Guard dog kennel", elements=[
        (0, 0, 2, 16, 14, 14, "wood"), (1, 14, 3, 15, 17, 13, "dwood"), (3, 17, 5, 13, 19, 11, "dwood"), (4, 0, 1.6, 12, 10, 2.4, "carbon"), (5, 1, 4, 11, 6, 10, "tan"), (4, 6, 5, 8, 9, 9, "tan"), (4, 8, 4.6, 6, 9, 5, "carbon"),
        (0, 4, 6, 1, 6, 10, "rope"), (13, 0, 14, 16, 2, 16, "steel"), (14, 2, 14.4, 15, 4, 15, "rust")]),
    dict(id="barbed_fence_j", fr="Clôture barbelée de la jungle", en="Jungle barbed fence", elements=[
        (0, 0, 7, 2, 24, 9, "wood"), (14, 0, 7, 16, 24, 9, "wood"), (2, 4, 7.6, 14, 5, 8.4, "steel"), (2, 10, 7.6, 14, 11, 8.4, "steel"), (2, 16, 7.6, 14, 17, 8.4, "steel"), (2, 22, 7.6, 14, 23, 8.4, "steel"), (5, 3, 7, 6, 6, 9, "steel"),
        (9, 9, 7, 10, 12, 9, "steel"), (4, 15, 7, 5, 18, 9, "steel"), (11, 21, 7, 12, 24, 9, "steel")]),
    dict(id="cctv_pole", fr="Mât de vidéosurveillance", en="CCTV pole", elements=[
        (4, 0, 4, 12, 2, 12, "dgray"), (7, 2, 7, 9, 28, 9, "steel"), (4, 28, 6, 12, 32, 10, "dgray"), (2, 28, 6.4, 4, 31, 9.6, "carbon"), (1, 29, 7, 2, 30, 9, "lamp"), (9, 12, 9, 11, 18, 10, "carbon"), (10, 17, 9.2, 11, 18, 9.8, "bloodred"),
        (5, 0, 5, 11, 1, 11, "dleaf")]),
    dict(id="mg_nest_j", fr="Nid de mitrailleuse de la jungle", en="Jungle MG nest", elements=[
        (0, 0, 2, 16, 4, 14, "dleaf"), (1, 4, 3, 15, 8, 13, "soil"), (2, 8, 4, 14, 11, 12, "dleaf"), (6, 8, 6, 10, 12, 10, "dgray"), (7, 11, 2, 9, 13, 8, "carbon"), (6, 10, 1, 10, 12, 3, "carbon"), (2, 4, 12, 6, 8, 14, "us_olive"),
        (10, 4, 12, 14, 8, 14, "us_olive"), (4, 8, 12, 12, 9, 13, "camo_g")]),
    # ================================================== PRISON ET INTERROGATOIRE
    dict(id="bamboo_cage", fr="Cage en bambou", en="Bamboo cage", elements=[
        (0, 0, 0, 16, 2, 16, "dwood"), (0, 2, 0, 2, 24, 2, "leaf"), (14, 2, 0, 16, 24, 2, "leaf"), (0, 2, 14, 2, 24, 16, "leaf"), (14, 2, 14, 16, 24, 16, "leaf"), (3, 2, 0.4, 4.4, 24, 1.8, "leaf"), (6, 2, 0.4, 7.4, 24, 1.8, "leaf"),
        (9, 2, 0.4, 10.4, 24, 1.8, "leaf"), (12, 2, 0.4, 13.4, 24, 1.8, "leaf"), (0, 24, 0, 16, 26, 16, "dwood"), (0, 11, 0, 16, 12, 2, "rope")]),
    dict(id="interrogation_chair", fr="Chaise d'interrogatoire", en="Interrogation chair", elements=[
        (2, 0, 2, 5, 12, 5, "steel"), (11, 0, 2, 14, 12, 5, "steel"), (2, 0, 11, 5, 12, 14, "steel"), (11, 0, 11, 14, 12, 14, "steel"), (1, 12, 1, 15, 14, 15, "wood"), (2, 14, 12, 14, 30, 15, "wood"), (1, 14, 3, 3, 18, 6, "rope"),
        (13, 14, 3, 15, 18, 6, "rope"), (5, 8, 1, 11, 9, 2, "rope"), (4, 30, 11, 12, 32, 14, "dgray")]),
    dict(id="chain_wall", fr="Mur à chaînes", en="Chain wall", passable=True, elements=[
        (0, 0, 14, 16, 16, 16, "stone"), (2, 3, 13.4, 4, 13, 14, "dgray"), (12, 3, 13.4, 14, 13, 14, "dgray"), (1, 12, 13, 5, 14, 14, "steel"), (11, 12, 13, 15, 14, 14, "steel"), (2, 2, 13, 4, 3, 14, "steel"), (12, 2, 13, 14, 3, 14, "steel"),
        (7, 6, 13.4, 9, 16, 14, "dgray"), (6, 4, 13, 10, 6, 14, "rust")]),
    dict(id="torture_table", fr="Table de torture", en="Torture table", elements=table(11, "dwood", "carbon", 1, 15, 2, 14) + [
        (1, 11, 3, 15, 12, 13, "dwood"), (0, 11, 4, 1, 14, 6, "steel"), (0, 11, 10, 1, 14, 12, "steel"), (15, 11, 4, 16, 14, 6, "steel"), (15, 11, 10, 16, 14, 12, "steel"), (3, 12, 5, 6, 13, 11, "bloodred"), (9, 12, 7, 13, 14, 9, "steel"),
        (10, 14, 6, 12, 15, 10, "carbon")]),
    dict(id="pit_grate", fr="Grille de fosse", en="Pit grate", passable=True, elements=[
        (0, 0, 0, 16, 2, 16, "carbon"), (1, 2, 1, 15, 4, 3, "steel"), (1, 2, 13, 15, 4, 15, "steel"), (1, 2, 3, 3, 4, 13, "steel"), (13, 2, 3, 15, 4, 13, "steel"), (4, 2, 4, 5, 4, 12, "steel"), (7, 2, 4, 8, 4, 12, "steel"), (10, 2, 4, 11, 4, 12, "steel"),
        (4, 2, 7, 12, 4, 8, "steel")]),
    dict(id="prison_slop_bucket", fr="Seau de cellule", en="Cell bucket", elements=[
        (4, 0, 4, 12, 8, 12, "steel"), (3, 8, 3, 13, 9, 13, "chrome"), (5, 8, 5, 11, 8.6, 11, "soil"), (2, 6, 7, 4, 8, 9, "steel"), (12, 6, 7, 14, 8, 9, "steel"), (6, 9, 6, 10, 10, 10, "rust"), (0, 0, 10, 5, 3, 16, "stone"),
        (10, 0, 0, 16, 2, 5, "tcotta")]),
    # ================================================== PIÈGES ET JUNGLE
    dict(id="spike_pit", fr="Fosse à pieux", en="Spike pit", passable=True, elements=[
        (0, 0, 0, 16, 2, 16, "soil"), (1, 2, 1, 15, 3, 15, "dleaf"), (3, 3, 3, 4, 10, 4, "wood"), (7, 3, 3, 8, 12, 4, "wood"), (11, 3, 3, 12, 9, 4, "wood"), (3, 3, 8, 4, 11, 9, "wood"), (8, 3, 8, 9, 9, 9, "wood"), (12, 3, 8, 13, 12, 9, "wood"),
        (5, 3, 12, 6, 10, 13, "wood"), (10, 3, 12, 11, 11, 13, "wood")]),
    dict(id="tripwire_trap", fr="Fil piégé", en="Tripwire trap", passable=True, elements=[
        (1, 0, 1, 3, 12, 3, "wood"), (13, 0, 13, 15, 12, 15, "wood"), (3, 6, 2, 13, 6.6, 14, "rope"), (4, 5, 2.4, 6, 8, 4, "us_olive"), (9, 3, 11, 11, 6, 13, "us_olive"), (1, 0, 1, 3, 1, 3, "dleaf"), (13, 0, 13, 15, 1, 15, "dleaf"),
        (4, 8, 2.6, 6, 9, 3.4, "steel")]),
    dict(id="camo_hide_hut", fr="Cabane de camouflage", en="Camouflage hide hut", elements=[
        (0, 0, 0, 16, 2, 16, "soil"), (1, 2, 2, 15, 9, 14, "dleaf"), (3, 9, 3, 13, 13, 13, "leaf"), (5, 13, 4, 11, 16, 12, "dleaf"), (6, 2, 1.4, 10, 8, 2, "carbon"), (1, 3, 3, 4, 7, 5, "leaf"), (12, 4, 4, 15, 8, 6, "dleaf"),
        (2, 9, 4, 5, 11, 6, "leaf"), (7, 16, 6, 9, 18, 10, "dleaf")]),
    dict(id="hunting_net_trap", fr="Filet de chasse", en="Hunting net trap", passable=True, elements=[
        (1, 0, 1, 3, 26, 3, "wood"), (13, 0, 1, 15, 26, 3, "wood"), (1, 24, 1, 15, 26, 3, "wood"), (3, 8, 1.8, 13, 9, 2.2, "rope"), (3, 14, 1.8, 13, 15, 2.2, "rope"), (3, 20, 1.8, 13, 21, 2.2, "rope"), (5, 8, 1.8, 6, 24, 2.2, "rope"),
        (8, 8, 1.8, 9, 24, 2.2, "rope"), (11, 8, 1.8, 12, 24, 2.2, "rope")]),
    dict(id="foot_trap", fr="Piège à mâchoires", en="Foot trap", passable=True, elements=[
        (3, 0, 3, 13, 1, 13, "rust"), (4, 1, 4, 6, 4, 12, "steel"), (10, 1, 4, 12, 4, 12, "steel"), (6, 1, 7, 10, 2, 9, "steel"), (4, 4, 4, 6, 5, 5, "steel"), (4, 4, 6, 6, 5, 7, "steel"), (4, 4, 8, 6, 5, 9, "steel"), (4, 4, 10, 6, 5, 11, "steel"),
        (10, 4, 4, 12, 5, 5, "steel"), (10, 4, 6, 12, 5, 7, "steel")]),
    dict(id="trench_section", fr="Section de tranchée", en="Trench section", elements=[
        (0, 0, 0, 16, 2, 16, "soil"), (0, 2, 0, 5, 10, 16, "soil"), (11, 2, 0, 16, 10, 16, "soil"), (0, 10, 0, 5, 12, 16, "dleaf"), (11, 10, 0, 16, 12, 16, "dleaf"), (5, 2, 1, 6, 8, 15, "wood"), (10, 2, 1, 11, 8, 15, "wood"),
        (6, 2, 14, 10, 3, 15, "dwood"), (0, 12, 3, 4, 16, 6, "us_olive"), (12, 12, 9, 16, 15, 12, "us_olive")]),
    # ================================================== CAMP DE BASE ET DORTOIR
    dict(id="hammock_bunk_row", fr="Rangée de hamacs superposés", en="Hammock bunk row", elements=[
        (0, 0, 2, 2, 30, 4, "wood"), (14, 0, 2, 16, 30, 4, "wood"), (0, 0, 12, 2, 30, 14, "wood"), (14, 0, 12, 16, 30, 14, "wood"), (2, 10, 3, 14, 12, 13, "camo_g"), (2, 8, 3, 14, 10, 13, "olive"), (2, 20, 3, 14, 22, 13, "camo_g"),
        (2, 18, 3, 14, 20, 13, "olive"), (4, 12, 5, 12, 14, 9, "star_white"), (4, 22, 5, 12, 24, 9, "cream")]),
    dict(id="jungle_field_kitchen", fr="Cuisine de campagne de la jungle", en="Jungle field kitchen", anim="fire", light=7, elements=[
        (0, 0, 2, 16, 8, 14, "dgray"), (1, 8, 3, 15, 10, 13, "steel"), (3, 10, 4, 8, 11, 9, "black", {"up": "anim"}), (3, 11, 4, 8, 13, 9, "steel"), (9, 10, 4, 14, 16, 9, "tin"), (10, 16, 5, 13, 18, 8, "steel"), (2, 10, 10, 8, 11, 13, "wood"),
        (10, 10, 10, 14, 14, 13, "cardboard"), (0, 8, 2, 16, 9, 3, "chrome")]),
    dict(id="cartel_radio_set", fr="Poste radio du cartel", en="Cartel radio set", anim="blink", light=4, elements=[
        (1, 0, 4, 15, 4, 12, "us_olive"), (2, 4, 5, 14, 10, 11, "kaki_d"), (3, 10, 6, 13, 14, 10, "dgray"), (4, 6, 4.4, 8, 9, 5, "carbon", {"north": "anim"}), (9, 5, 4.2, 13, 9, 4.6, "carbon"), (13, 9, 7, 14, 24, 8, "steel"), (3, 14, 7, 5, 16, 9, "bloodred"),
        (6, 4, 3.6, 10, 5.6, 4, "star_white")]),
    dict(id="jungle_tent", fr="Tente de jungle", en="Jungle tent", elements=[
        (0, 0, 1, 16, 1, 15, "soil"), (1, 1, 2, 15, 6, 14, "camo_g"), (3, 6, 3, 13, 10, 13, "us_olive"), (5, 10, 4, 11, 13, 12, "camo_g"), (7, 13, 5, 9, 16, 11, "us_olive"), (6, 1, 0.8, 10, 5, 1.6, "carbon"), (0, 0, 7, 1, 16, 9, "wood"), (15, 0, 7, 16, 16, 9, "wood"),
        (2, 6, 3, 5, 9, 4, "dleaf")]),
    dict(id="cartel_campfire", fr="Feu de camp du cartel", en="Cartel campfire", anim="fire", light=15, elements=[
        (3, 0, 3, 13, 2, 13, "stone"), (5, 2, 5, 11, 8, 11, "black", {"up": "anim", "north": "anim", "south": "anim", "east": "anim", "west": "anim"}), (2, 2, 6, 4, 8, 10, "dwood"), (12, 2, 6, 14, 8, 10, "dwood"), (6, 2, 12, 10, 8, 14, "dwood"),
        (6, 7, 6, 10, 11, 10, "dgray"), (6, 11, 6, 10, 12, 10, "steel"), (0, 0, 0, 3, 5, 3, "dwood"), (13, 0, 13, 16, 5, 16, "dwood")]),
    dict(id="supply_cache", fr="Cache de ravitaillement", en="Supply cache", elements=[
        (0, 0, 2, 16, 3, 14, "dleaf"), (1, 3, 3, 15, 8, 13, "us_olive"), (2, 8, 4, 14, 11, 12, "camo_g"), (3, 11, 5, 13, 13, 11, "dleaf"), (3, 3, 2.4, 7, 7, 3, "paper"), (9, 4, 2.4, 13, 8, 3, "mustard"), (4, 13, 6, 8, 15, 10, "leaf"),
        (11, 8, 4, 15, 9, 8, "cardboard"), (0, 3, 6, 2, 9, 10, "wood")]),
    # ================================================== JUNGLE ET NATURE
    dict(id="giant_fern", fr="Fougère géante", en="Giant fern", passable=True, elements=[
        (7, 0, 7, 9, 6, 9, "wood"), (1, 6, 7, 7, 8, 9, "leaf"), (9, 6, 7, 15, 8, 9, "leaf"), (7, 6, 1, 9, 8, 7, "dleaf"), (7, 6, 9, 9, 8, 15, "dleaf"), (0, 8, 7.4, 5, 10, 8.6, "dleaf"), (11, 8, 7.4, 16, 10, 8.6, "dleaf"), (7.4, 8, 0, 8.6, 10, 5, "leaf"),
        (7.4, 8, 11, 8.6, 10, 16, "leaf"), (6, 6, 6, 10, 9, 10, "leaf")]),
    dict(id="hanging_vines", fr="Lianes pendantes", en="Hanging vines", passable=True, elements=[
        (0, 28, 0, 16, 32, 16, "dleaf"), (2, 4, 2, 3, 28, 3, "leaf"), (6, 8, 4, 7, 28, 5, "dleaf"), (10, 2, 3, 11, 28, 4, "leaf"), (13, 10, 6, 14, 28, 7, "dleaf"), (4, 12, 10, 5, 28, 11, "dleaf"), (8, 6, 12, 9, 28, 13, "leaf"),
        (12, 14, 12, 13, 28, 13, "leaf"), (7, 20, 7, 8, 28, 8, "dleaf")]),
    dict(id="wild_beehive_log", fr="Ruche sauvage dans un tronc", en="Wild beehive log", elements=[
        (4, 0, 4, 12, 24, 12, "dwood"), (3, 0, 3, 13, 3, 13, "dwood"), (5, 10, 3.4, 11, 18, 4, "hay"), (6, 11, 3.2, 10, 17, 3.6, "carbon"), (3, 20, 5, 4, 28, 11, "leaf"), (12, 18, 5, 14, 26, 11, "dleaf"), (7, 12, 3, 9, 14, 3.4, "yellow"),
        (6, 24, 6, 10, 28, 10, "leaf"), (1, 0, 5, 4, 2, 11, "dleaf")]),
    dict(id="parrot_perch", fr="Perchoir à perroquet", en="Parrot perch", elements=[
        (4, 0, 4, 12, 2, 12, "dwood"), (7, 2, 7, 9, 18, 9, "wood"), (2, 18, 6.6, 14, 19.4, 9.4, "wood"), (3, 19, 6.4, 6, 25, 9.6, "red"), (3, 25, 6.8, 6, 28, 9.2, "red"), (2, 22, 7, 3, 23, 9, "yellow"), (6, 19, 7, 9, 21, 9, "blue"),
        (10, 19, 6.4, 13, 23, 9.6, "green"), (10, 23, 6.8, 13, 26, 9.2, "emerald"), (12, 25, 7.4, 14, 26, 8.6, "yellow")]),
    dict(id="orchid_cluster", fr="Orchidées sauvages", en="Wild orchids", passable=True, elements=[
        (0, 0, 2, 16, 2, 14, "soil"), (3, 2, 5, 4, 10, 6, "leaf"), (3, 10, 3, 7, 12, 9, "magenta"), (4, 11, 5, 6, 13, 7, "pink"), (8, 2, 8, 9, 12, 9, "leaf"), (7, 12, 6, 11, 14, 11, "purple"), (8, 13, 8, 10, 15, 10, "star_white"),
        (12, 2, 4, 13, 8, 5, "leaf"), (11, 8, 3, 15, 10, 7, "pink"), (1, 2, 9, 5, 6, 13, "dleaf")]),
    dict(id="overgrown_statue", fr="Statue envahie par la végétation", en="Overgrown statue", elements=[
        (2, 0, 2, 14, 4, 14, "stone"), (4, 4, 4, 12, 8, 12, "ash"), (5, 8, 5, 11, 22, 11, "stone"), (6, 22, 6, 10, 28, 10, "stone"), (3, 6, 3, 7, 14, 6, "leaf"), (9, 10, 9, 13, 20, 12, "dleaf"), (5, 18, 4.6, 8, 24, 5.2, "leaf"),
        (10, 20, 5, 12, 26, 11, "dleaf"), (6, 24, 4.8, 10, 27, 5.6, "ash"), (1, 0, 8, 4, 8, 12, "dleaf")]),
    # ================================================== ENSEIGNES ET GRAFFITIS
    dict(id="cartel_tag_wall", fr="Mur tagué du cartel", en="Cartel tagged wall", passable=True, elements=[
        (0, 0, 14, 16, 16, 16, "tan"), (1, 3, 13.6, 5, 13, 14, "bloodred"), (6, 5, 13.6, 10, 10, 14, "carbon"), (7, 6, 13.4, 9, 9, 13.8, "gold"), (11, 3, 13.6, 15, 6, 14, "bloodred"), (11, 8, 13.6, 15, 13, 14, "carbon"), (12, 9, 13.4, 14, 12, 13.8, "gold"),
        (2, 2, 13.4, 4, 3, 13.8, "star_white"), (6, 12, 13.4, 10, 13, 13.8, "bloodred")]),
    dict(id="private_property_sign", fr="Panneau « Propiedad privada »", en="Private property sign", passable=True, elements=[
        (7, 0, 7.4, 9, 14, 8.6, "wood"), (1, 14, 7, 15, 26, 9, "bloodred"), (2, 15, 6.6, 14, 25, 7, "star_white"), (3, 16, 6.2, 13, 18, 6.6, "carbon"), (3, 19, 6.2, 11, 21, 6.6, "carbon"), (3, 22, 6.2, 13, 24, 6.6, "bloodred"), (1, 14, 8.6, 15, 15, 9.4, "carbon")]),
    dict(id="cartel_banner_j", fr="Bannière du cartel", en="Cartel banner", passable=True, elements=[
        (0, 14, 14.6, 16, 16, 15.4, "mahogany"), (2, 1, 14.8, 14, 14, 15.2, "bloodred"), (3, 2, 14.5, 13, 13, 14.8, "carbon"), (5, 4, 14.2, 11, 11, 14.5, "gold"), (7, 6, 14, 9, 9, 14.2, "carbon"), (3, 1, 14.6, 5, 4, 15, "gold"), (11, 1, 14.6, 13, 4, 15, "gold")]),
    dict(id="reward_posters", fr="Affiches de récompense", en="Reward posters", passable=True, elements=[
        (0, 0, 15, 16, 16, 16, "dwood"), (1, 8, 14.4, 7, 15, 15, "paper"), (2, 9, 14, 6, 13, 14.4, "carbon"), (3, 13, 13.8, 5, 14, 14, "bloodred"), (9, 9, 14.4, 15, 15, 15, "paper"), (10, 10, 14, 14, 14, 14.4, "carbon"), (11, 14, 13.8, 13, 14.6, 14, "bloodred"),
        (4, 1, 14.4, 12, 7, 15, "paper"), (6, 2, 14, 10, 6, 14.4, "tan")]),
    dict(id="skull_warning_sign", fr="Panneau à tête de mort", en="Skull warning sign", passable=True, elements=[
        (7, 0, 7.4, 9, 14, 8.6, "wood"), (3, 14, 7, 13, 26, 9, "carbon"), (4, 15, 6.6, 12, 25, 7, "bloodred"), (5, 18, 6.2, 11, 24, 6.6, "bone"), (6, 19, 6, 8, 21, 6.2, "carbon"), (9, 19, 6, 11, 21, 6.2, "carbon"), (7, 17, 6.2, 9, 18, 6.6, "bone"),
        (6, 16, 6.2, 10, 17, 6.6, "bone")]),
    dict(id="jungle_trail_sign", fr="Poteau de sentier", en="Jungle trail sign", elements=[
        (7, 0, 7, 9, 26, 9, "wood"), (1, 20, 7.4, 8, 24, 8.6, "dwood"), (8, 15, 7.4, 15, 19, 8.6, "dwood"), (0, 21, 7.2, 2, 23, 8.8, "dwood"), (14, 16, 7.2, 16, 18, 8.8, "dwood"), (2, 21, 7, 7, 23, 7.4, "paper"), (9, 16, 7, 14, 18, 7.4, "paper"),
        (5, 0, 5, 11, 2, 11, "soil"), (6, 26, 6, 10, 28, 10, "dleaf")]),
    # ================================================== MUSIQUE ET DÉTENTE
    dict(id="guitar_stand_c", fr="Guitare sur support", en="Guitar stand", elements=[
        (2, 0, 4, 6, 2, 12, "dwood"), (10, 0, 4, 14, 2, 12, "dwood"), (3, 2, 5, 5, 14, 6, "wood"), (11, 2, 5, 13, 14, 6, "wood"), (4, 6, 4, 12, 12, 12, "wood"), (5, 12, 5, 11, 15, 11, "wood"), (7, 8, 3.4, 9, 10, 4, "carbon"), (7, 14, 6, 9, 26, 8, "dwood"),
        (6, 26, 5.6, 10, 30, 8.4, "dwood"), (5, 9, 3, 11, 10, 3.4, "gold")]),
    dict(id="norteno_accordion", fr="Accordéon norteño", en="Norteño accordion", elements=[
        (1, 0, 4, 15, 2, 12, "dwood"), (2, 2, 5, 6, 12, 11, "bloodred"), (10, 2, 5, 14, 12, 11, "bloodred"), (6, 4, 6, 10, 10, 10, "star_white"), (6, 5, 6, 10, 6, 10, "carbon"), (6, 7, 6, 10, 8, 10, "carbon"), (6, 9, 6, 10, 10, 10, "carbon"),
        (3, 12, 6, 5, 14, 10, "gold"), (11, 12, 6, 13, 14, 10, "gold"), (2, 3, 4.6, 6, 11, 5, "gold")]),
    dict(id="neon_jukebox", fr="Juke-box néon", en="Neon jukebox", anim="jukebox", light=10, elements=[
        (1, 0, 3, 15, 4, 13, "carbon"), (1, 4, 4, 15, 26, 13, "bloodred"), (3, 10, 3.4, 13, 22, 4, "carbon", {"north": "anim"}), (3, 24, 3.8, 13, 26, 4.2, "gold"), (0, 4, 4, 1, 26, 13, "gold"), (15, 4, 4, 16, 26, 13, "gold"), (5, 4, 3.6, 11, 8, 4, "dgray"),
        (7, 6, 3.4, 9, 7, 3.8, "gold"), (2, 26, 5, 14, 28, 12, "bloodred")]),
    dict(id="cartel_domino_table", fr="Table de dominos", en="Domino table", elements=table(11, "green", "mahogany", 0, 16, 0, 16) + [
        (0, 11, 0, 16, 12, 16, "mahogany"), (1, 12, 1, 15, 12.4, 15, "green"), (3, 12.4, 3, 5, 13, 5, "star_white"), (6, 12.4, 4, 8, 13, 6, "carbon"), (9, 12.4, 3, 11, 13, 5, "star_white"), (3, 12.4, 9, 5, 13, 11, "carbon"), (8, 12.4, 10, 10, 13, 12, "star_white"),
        (11, 12.4, 8, 13, 13, 10, "carbon")]),
    dict(id="tequila_bar_c", fr="Bar à tequila", en="Tequila bar", elements=[
        (0, 0, 2, 16, 12, 10, "mahogany"), (0, 12, 1, 16, 14, 11, "dwood"), (0, 14, 1, 16, 15, 3, "gold"), (1, 14, 5, 3, 22, 7, "lamp"), (4, 14, 5, 6, 20, 7, "emerald"), (7, 14, 5, 9, 21, 7, "tan"), (10, 14, 6, 12, 19, 8, "bloodred"),
        (13, 14, 5, 15, 22, 7, "lamp"), (0, 28, 12, 16, 30, 16, "wood"), (1, 14, 12, 15, 28, 13, "mahogany"), (3, 18, 11.6, 13, 19, 12, "gold")]),
    dict(id="hammock_lounge", fr="Hamac de détente", en="Lounge hammock", elements=[
        (0, 0, 7, 3, 28, 9, "wood"), (13, 0, 7, 16, 28, 9, "wood"), (3, 14, 6, 13, 16, 10, "bloodred"), (2, 16, 6, 4, 24, 10, "rope"), (12, 16, 6, 14, 24, 10, "rope"), (4, 14, 6.4, 12, 15, 9.6, "star_white"), (5, 12, 6.8, 11, 14, 9.2, "bloodred"),
        (0, 0, 6, 3, 2, 10, "dleaf"), (13, 0, 6, 16, 2, 10, "dleaf")]),
]

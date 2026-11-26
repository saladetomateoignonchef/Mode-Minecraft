"""
Sixième série de décors : 96 objets détaillés pour les villages et cités du désert (riad et maison, cuisine et hammam, palais du cheikh,
mosquée et savoir, souk et épices, artisans, agriculture et oasis, caravane et nomades, milice et armurerie, postes de garde et tours,
contrebande, ruines et usure, enseignes, fêtes et musique, thé et jeux, médical et rituels). Importé par generate_realism_decor.py.
"""
import random

from PIL import Image

from realism_decor_4 import table

PAL6 = {
    "henna": "#a8431c", "saffron": "#e8a412", "paprika": "#b8321a", "cumin": "#8a6a2a", "indigo": "#2a3a7a", "zellige": "#1f7a96", "copper": "#b8683a",
    "camel": "#c49a58", "dune": "#e2ca92", "silk_p": "#8a3a8a",
}


def _f(n):
    return Image.new("RGBA", (16, 16 * n))


def anim_fountain():
    """Fontaine : filets d'eau qui retombent dans le bassin."""
    img = _f(8)
    rnd = random.Random(5)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (40, 110, 160, 255))
        for x in (3, 7, 11):
            for k in range(6):
                y = (k * 3 + f * 2 + x) % 16
                img.putpixel((x, y + 16 * f), (190, 230, 250, 255))
                img.putpixel((x + 1, (y + 5) % 16 + 16 * f), (130, 200, 235, 255))
        for _ in range(8):
            img.putpixel((rnd.randint(0, 15), rnd.randint(0, 15) + 16 * f), (230, 245, 255, 255))
    return img, {"frametime": 3}


def anim_sandfall():
    """Sablier : le sable coule en filet."""
    img = _f(8)
    for f in range(8):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y + 16 * f), (196, 160, 96, 255) if y < 3 + f else (60, 44, 30, 255))
        for y in range(4, 14):
            if (y + f) % 2 == 0:
                img.putpixel((7, y + 16 * f), (240, 215, 150, 255))
                img.putpixel((8, y + 16 * f), (240, 215, 150, 255))
    return img, {"frametime": 3}


ANIMS6 = {"fountain": anim_fountain, "sandfall": anim_sandfall}

DECOS6 = [
    # ================================================== RIAD ET MAISON
    dict(id="low_brass_table", fr="Table basse en cuivre", en="Low brass table", elements=[
        (1, 6, 1, 15, 7, 15, "copper"), (2, 7, 2, 14, 7.6, 14, "brass"), (4, 7.6, 4, 12, 8, 12, "bronze"), (2, 0, 2, 4, 6, 4, "wood"), (12, 0, 2, 14, 6, 4, "wood"), (2, 0, 12, 4, 6, 14, "wood"), (12, 0, 12, 14, 6, 14, "wood"),
        (3, 2, 3, 13, 3, 13, "wood"), (7, 8, 7, 9, 9, 9, "gold")]),
    dict(id="wedding_chest", fr="Coffre de mariage", en="Wedding chest", elements=[
        (1, 0, 3, 15, 8, 13, "henna"), (1, 8, 3, 15, 10, 13, "dwood"), (0, 7, 2.6, 16, 8, 13.4, "brass"), (0, 3, 2.6, 16, 4, 13.4, "brass"), (6, 4, 2.4, 10, 8, 3, "gold"), (7, 5, 2.2, 9, 7, 2.6, "saffron"),
        (2, 10, 4, 14, 11, 12, "silk_p"), (1, 0, 3, 3, 1, 5, "brass"), (13, 0, 3, 15, 1, 5, "brass")]),
    dict(id="zellige_panel", fr="Panneau de zellige", en="Zellige panel", passable=True, elements=[
        (0, 0, 15, 16, 16, 16, "zellige"), (1, 1, 14.6, 15, 15, 15, "turquoise"), (2, 2, 14.2, 14, 14, 14.6, "indigo"), (3, 3, 14, 13, 13, 14.2, "star_white"), (5, 5, 13.8, 11, 11, 14, "zellige"), (7, 7, 13.6, 9, 9, 13.8, "gold"),
        (0, 0, 14.6, 1, 16, 15, "gold"), (15, 0, 14.6, 16, 16, 15, "gold")]),
    dict(id="arched_niche", fr="Niche en arc", en="Arched niche", passable=True, elements=[
        (1, 0, 13, 15, 16, 16, "tan"), (3, 1, 12.6, 13, 12, 13, "dgray"), (4, 12, 12.6, 12, 13, 13, "dgray"), (5, 13, 12.6, 11, 14, 13, "dgray"), (6, 14, 12.6, 10, 15, 13, "dgray"), (3, 1, 12.4, 4, 12, 13, "gold"),
        (12, 1, 12.4, 13, 12, 13, "gold"), (6, 1, 12.8, 10, 4, 14, "copper"), (7, 4, 13, 9, 7, 13.6, "gold")]),
    dict(id="pouf_stack", fr="Pile de poufs", en="Pouf stack", elements=[
        (2, 0, 2, 9, 5, 9, "henna"), (7, 0, 7, 14, 5, 14, "indigo"), (3, 5, 3, 8, 9, 8, "saffron"), (2, 0, 2, 9, 1, 9, "gold"), (7, 0, 7, 14, 1, 14, "gold"), (4, 9, 4, 7, 10, 7, "silk_p"), (9, 0, 2, 14, 4, 6, "turquoise"),
        (3, 5, 3, 8, 6, 8, "gold")]),
    dict(id="water_jug_stand", fr="Support de jarres d'eau", en="Water jug stand", elements=[
        (1, 0, 3, 15, 2, 13, "dwood"), (2, 2, 4, 6, 12, 8, "tcotta"), (3, 12, 5, 5, 15, 7, "tcotta"), (9, 2, 4, 13, 10, 8, "tcotta"), (10, 10, 5, 12, 13, 7, "tcotta"), (5, 2, 9, 11, 14, 13, "tcotta"), (6, 14, 10, 10, 16, 12, "tcotta"),
        (1, 0, 3, 2, 14, 13, "wood"), (14, 0, 3, 15, 14, 13, "wood"), (1, 12, 3, 15, 14, 4, "wood")]),
    # ================================================== CUISINE ET HAMMAM
    dict(id="tagine_stove", fr="Réchaud à tajine", en="Tagine stove", anim="fire", light=6, elements=[
        (2, 0, 3, 14, 4, 13, "adobe"), (3, 4, 4, 13, 6, 12, "dgray"), (4, 5, 5, 12, 6, 11, "black", {"up": "anim"}), (3, 6, 4, 13, 8, 12, "tcotta"), (4, 8, 5, 12, 10, 11, "tcotta"), (5, 10, 6, 11, 12, 10, "tcotta"), (6, 12, 7, 10, 15, 9, "tcotta"),
        (7, 15, 7, 9, 16, 9, "brass")]),
    dict(id="clay_bread_oven", fr="Four à pain en argile", en="Clay bread oven", anim="fire", light=8, elements=[
        (1, 0, 2, 15, 3, 14, "adobe"), (2, 3, 3, 14, 12, 13, "tcotta"), (4, 4, 2.6, 12, 9, 3, "black", {"north": "anim"}), (3, 9, 4, 13, 14, 12, "tcotta"), (5, 14, 6, 11, 17, 10, "tcotta"), (6, 17, 7, 10, 19, 9, "dgray"),
        (1, 3, 2, 3, 6, 4, "wood"), (3, 2, 1, 8, 3, 2.6, "cream"), (9, 3, 1, 13, 4, 2.4, "bun")]),
    dict(id="olive_oil_jars", fr="Jarres d'huile d'olive", en="Olive oil jars", elements=[
        (0, 0, 2, 7, 11, 9, "tcotta"), (2, 11, 4, 5, 14, 7, "tcotta"), (8, 0, 3, 15, 9, 10, "tcotta"), (10, 9, 5, 13, 12, 8, "tcotta"), (3, 0, 10, 9, 8, 16, "tcotta"), (5, 8, 12, 7, 10, 14, "tcotta"), (0, 4, 2, 7, 5, 9, "henna"),
        (8, 3, 3, 15, 4, 10, "henna"), (2, 14, 4, 5, 15, 7, "leather")]),
    dict(id="hammam_basin", fr="Bassin de hammam", en="Hammam basin", elements=[
        (0, 0, 0, 16, 6, 16, "zellige"), (1, 6, 1, 15, 8, 15, "turquoise"), (2, 6, 2, 14, 7, 14, "water"), (0, 8, 0, 16, 10, 2, "tan"), (0, 8, 14, 16, 10, 16, "tan"), (0, 8, 2, 2, 10, 14, "tan"), (14, 8, 2, 16, 10, 14, "tan"),
        (7, 8, 7, 9, 10, 9, "copper"), (3, 8, 3, 5, 9, 5, "copper")]),
    dict(id="steam_bench", fr="Banc de vapeur", en="Steam bench", anim="steam", elements=[
        (0, 0, 2, 16, 8, 14, "tan"), (1, 8, 3, 15, 10, 13, "turquoise"), (2, 10, 4, 14, 11, 12, "cream"), (3, 11, 5, 6, 14, 8, "steam_w", {"up": "anim"}), (9, 11, 6, 12, 15, 9, "steam_w", {"up": "anim"}), (0, 8, 2, 16, 9, 3, "tan"),
        (13, 10, 5, 15, 13, 8, "copper")]),
    dict(id="argan_press", fr="Presse à argan", en="Argan press", elements=[
        (1, 0, 3, 15, 4, 13, "dwood"), (2, 4, 4, 14, 8, 12, "wood"), (7, 8, 7, 9, 22, 9, "wood"), (1, 20, 6, 15, 22, 10, "wood"), (3, 22, 7, 5, 26, 9, "wood"), (11, 8, 6, 14, 12, 10, "tcotta"), (4, 4, 4, 12, 5, 12, "tan"),
        (12, 12, 7, 13, 14, 9, "cumin")]),
    # ================================================== PALAIS ET CHEIKH
    dict(id="sheikh_throne", fr="Trône du cheikh", en="Sheikh's throne", elements=[
        (1, 0, 2, 15, 3, 14, "wine"), (2, 3, 3, 14, 9, 13, "gold"), (3, 9, 8, 13, 22, 13, "wine"), (2, 9, 8, 3, 24, 13, "gold"), (13, 9, 8, 14, 24, 13, "gold"), (3, 22, 8, 13, 26, 13, "gold"), (5, 26, 9, 11, 30, 12, "gold"),
        (4, 9, 3, 12, 11, 8, "silk_p"), (2, 9, 3, 4, 14, 8, "gold"), (12, 9, 3, 14, 14, 8, "gold"), (7, 27, 8.6, 9, 29, 9, "emerald")]),
    dict(id="silk_gold_divan", fr="Divan de soie et d'or", en="Silk and gold divan", elements=[
        (0, 0, 2, 16, 4, 14, "gold"), (1, 4, 3, 15, 9, 13, "silk_p"), (0, 4, 2, 16, 14, 5, "wine"), (0, 9, 2, 2, 16, 14, "gold"), (14, 9, 2, 16, 16, 14, "gold"), (3, 9, 6, 6, 12, 10, "saffron"), (9, 9, 7, 13, 12, 11, "turquoise"),
        (6, 12, 7, 8, 13, 9, "gold")]),
    dict(id="palace_fountain", fr="Fontaine du palais", en="Palace fountain", anim="fountain", light=3, elements=[
        (0, 0, 0, 16, 5, 16, "tan"), (1, 5, 1, 15, 7, 15, "zellige"), (2, 5, 2, 14, 6.4, 14, "water", {"up": "anim"}), (6, 5, 6, 10, 12, 10, "tan"), (5, 12, 5, 11, 14, 11, "zellige"), (6, 13.4, 6, 10, 14, 10, "water", {"up": "anim"}),
        (7, 14, 7, 9, 20, 9, "water", {"north": "anim", "south": "anim", "east": "anim", "west": "anim"}), (7.4, 20, 7.4, 8.6, 21, 8.6, "gold")]),
    dict(id="gilded_arch", fr="Arcade dorée", en="Gilded arch", elements=[
        (0, 0, 5, 3, 24, 11, "gold"), (13, 0, 5, 16, 24, 11, "gold"), (0, 24, 5, 16, 28, 11, "gold"), (3, 20, 5.6, 13, 24, 10.4, "gold"), (4, 18, 6, 12, 20, 10, "gold"), (1, 4, 4.6, 2, 20, 5, "tan"), (14, 4, 4.6, 15, 20, 5, "tan"),
        (6, 28, 6, 10, 31, 10, "gold"), (7.4, 31, 7.4, 8.6, 32, 8.6, "gold"), (0, 0, 4.6, 3, 3, 11.4, "dwood"), (13, 0, 4.6, 16, 3, 11.4, "dwood")]),
    dict(id="ornate_chandelier", fr="Lustre ciselé", en="Ornate chandelier", light=12, passable=True, elements=[
        (7.4, 28, 7.4, 8.6, 32, 8.6, "dgray"), (4, 26, 4, 12, 28, 12, "gold"), (1, 22, 1, 15, 24, 15, "brass"), (7, 24, 7, 9, 26, 9, "gold"), (1, 24, 7, 3, 26, 9, "lamp"), (13, 24, 7, 15, 26, 9, "lamp"),
        (7, 24, 1, 9, 26, 3, "lamp"), (7, 24, 13, 9, 26, 15, "lamp"), (6, 20, 6, 10, 22, 10, "lamp"), (7.4, 22, 7.4, 8.6, 24, 8.6, "gold")]),
    dict(id="sultan_coffer", fr="Coffre du sultan", en="Sultan's coffer", elements=[
        (1, 0, 3, 15, 8, 13, "wine"), (1, 8, 4, 15, 11, 12, "wine"), (3, 11, 5, 13, 12, 11, "wine"), (0, 3, 2.6, 16, 5, 13.4, "gold"), (0, 8, 2.6, 16, 9, 13.4, "gold"), (6, 4, 2.4, 10, 8, 3, "gold"), (7, 5, 2.2, 9, 7, 2.6, "emerald"),
        (4, 12, 6, 7, 13, 9, "gold"), (9, 12, 6, 11, 13.6, 8, "emerald")]),
    # ================================================== MOSQUÉE ET SAVOIR
    dict(id="mihrab", fr="Mihrab", en="Mihrab", passable=True, elements=[
        (0, 0, 14, 16, 26, 16, "tan"), (2, 0, 13, 14, 20, 14, "zellige"), (3, 1, 12.6, 13, 18, 13, "dgray"), (4, 18, 12.6, 12, 20, 13, "dgray"), (5, 20, 12.6, 11, 22, 13, "dgray"), (2, 0, 12.4, 3, 20, 13, "gold"), (13, 0, 12.4, 14, 20, 13, "gold"),
        (6, 22, 12.4, 10, 23, 13, "gold"), (7, 8, 13, 9, 12, 13.6, "gold")]),
    dict(id="minbar", fr="Minbar", en="Minbar", elements=[
        (1, 0, 1, 15, 3, 5, "dwood"), (2, 3, 2, 14, 6, 6, "dwood"), (3, 6, 4, 13, 9, 8, "dwood"), (4, 9, 6, 12, 12, 10, "dwood"), (5, 12, 8, 11, 15, 12, "dwood"), (4, 15, 8, 12, 17, 12, "gold"), (1, 0, 1, 2, 12, 12, "wood"),
        (14, 0, 1, 15, 12, 12, "wood"), (5, 17, 9, 11, 22, 11, "wood"), (7, 22, 9.4, 9, 24, 10.6, "gold")]),
    dict(id="quran_stand", fr="Lutrin à Coran", en="Quran stand", elements=[
        (3, 0, 3, 13, 2, 13, "dwood"), (7, 2, 7, 9, 10, 9, "wood"), (2, 10, 3, 14, 11, 13, "dwood"), (2, 11, 3, 14, 12, 5, "dwood"), (3, 11, 5, 13, 13, 11, "paper"), (3, 11, 6, 13, 12, 6.4, "gold"), (7.6, 11, 5, 8.4, 13.2, 11, "dwood"),
        (5, 11.2, 7, 7, 11.8, 10, "book_a")]),
    dict(id="hanging_oil_lamp", fr="Lampe à huile suspendue", en="Hanging oil lamp", anim="flicker", light=13, passable=True, elements=[
        (7.6, 24, 7.6, 8.4, 32, 8.4, "dgray"), (6, 22, 6, 10, 24, 10, "brass"), (5, 14, 5, 11, 22, 11, "brass"), (6, 15, 6, 10, 21, 10, "lamp", {"north": "anim", "south": "anim", "east": "anim", "west": "anim"}), (4, 12, 4, 12, 14, 12, "brass"),
        (7, 10, 7, 9, 12, 9, "gold")]),
    dict(id="ablution_fountain", fr="Fontaine d'ablutions", en="Ablution fountain", anim="fountain", elements=[
        (0, 0, 0, 16, 4, 16, "tan"), (1, 4, 1, 15, 8, 15, "zellige"), (2, 7, 2, 14, 7.6, 14, "water", {"up": "anim"}), (6, 8, 6, 10, 18, 10, "tan"), (5, 18, 5, 11, 20, 11, "zellige"), (6, 20, 6, 10, 22, 10, "copper"), (7, 22, 7, 9, 23, 9, "gold"),
        (7, 12, 5.4, 9, 13, 6, "copper"), (7, 12, 10, 9, 13, 10.6, "copper")]),
    dict(id="sundial_stele", fr="Cadran solaire sur stèle", en="Sundial stele", elements=[
        (3, 0, 3, 13, 3, 13, "tan"), (5, 3, 5, 11, 14, 11, "tan"), (2, 14, 2, 14, 16, 14, "tan"), (3, 16, 3, 13, 17, 13, "sand"), (7.4, 17, 4, 8.6, 21, 9, "brass"), (4, 16.6, 7.6, 12, 17, 8.4, "carbon"), (7.6, 16.6, 4, 8.4, 17, 12, "carbon"),
        (6, 5, 4.6, 10, 12, 5, "carbon")]),
    # ================================================== SOUK ET ÉPICES
    dict(id="spice_cones", fr="Cônes d'épices", en="Spice cones", elements=[
        (0, 0, 2, 16, 3, 14, "dwood"), (1, 3, 3, 6, 5, 8, "paprika"), (2, 5, 4, 5, 8, 7, "paprika"), (3, 8, 5, 4, 10, 6, "paprika"), (6, 3, 3, 11, 5, 8, "saffron"), (7, 5, 4, 10, 8, 7, "saffron"), (8, 8, 5, 9, 10, 6, "saffron"),
        (11, 3, 3, 15, 5, 8, "cumin"), (12, 5, 4, 14, 7, 7, "cumin"), (2, 3, 9, 7, 5, 13, "green"), (3, 5, 10, 6, 7, 12, "green"), (9, 3, 9, 14, 5, 13, "henna")]),
    dict(id="merchant_scales", fr="Balance de marchand", en="Merchant scales", elements=[
        (3, 0, 4, 13, 2, 12, "dwood"), (7, 2, 7, 9, 18, 9, "brass"), (2, 17, 7.4, 14, 18, 8.6, "brass"), (1, 11, 7, 5, 12, 9, "copper"), (11, 11, 7, 15, 12, 9, "copper"), (2, 12, 7.6, 3, 17, 8.4, "rope"), (4, 12, 7.6, 5, 17, 8.4, "rope"),
        (12, 12, 7.6, 13, 17, 8.4, "rope"), (14, 12, 7.6, 15, 17, 8.4, "rope"), (2, 12, 7.2, 4, 13, 8.8, "saffron"), (7, 18, 7, 9, 20, 9, "gold")]),
    dict(id="hung_rugs_stall", fr="Étal de tapis suspendus", en="Hanging rugs stall", elements=[
        (0, 0, 5, 2, 28, 7, "wood"), (14, 0, 5, 16, 28, 7, "wood"), (0, 26, 5, 16, 28, 7, "wood"), (2, 4, 5.6, 7, 26, 6.4, "henna"), (3, 6, 5.2, 6, 24, 5.6, "indigo"), (4, 10, 5, 5, 20, 5.2, "saffron"), (8, 6, 5.6, 14, 26, 6.4, "indigo"),
        (9, 8, 5.2, 13, 24, 5.6, "henna"), (10, 12, 5, 12, 20, 5.2, "gold"), (2, 0, 3, 14, 3, 10, "dune")]),
    dict(id="pottery_stack", fr="Pile de poteries", en="Pottery stack", elements=[
        (0, 0, 2, 7, 6, 9, "tcotta"), (1, 6, 3, 6, 10, 8, "tcotta"), (2, 10, 4, 5, 13, 7, "turquoise"), (8, 0, 3, 15, 5, 10, "henna"), (9, 5, 4, 14, 9, 9, "zellige"), (3, 0, 10, 10, 4, 16, "tcotta"), (4, 4, 11, 9, 7, 15, "indigo"),
        (11, 0, 11, 15, 3, 15, "tan")]),
    dict(id="tea_seller_cart", fr="Chariot du vendeur de thé", en="Tea seller cart", elements=[
        (1, 4, 3, 15, 10, 13, "turquoise"), (0, 10, 2, 16, 12, 14, "dwood"), (2, 12, 4, 6, 20, 8, "copper"), (3, 20, 5, 5, 23, 7, "copper"), (9, 12, 4, 14, 14, 9, "brass"), (10, 14, 5, 13, 17, 8, "glass"), (0, 22, 1, 16, 24, 15, "henna"),
        (1, 12, 1, 2, 22, 2, "wood"), (14, 12, 1, 15, 22, 2, "wood"), (1, 0, 4, 5, 5, 10, "wheel"), (11, 0, 4, 15, 5, 10, "wheel")]),
    dict(id="fabric_bolts", fr="Rouleaux d'étoffes", en="Fabric bolts", elements=[
        (0, 0, 2, 16, 2, 14, "dwood"), (1, 2, 3, 5, 12, 7, "henna"), (6, 2, 3, 10, 14, 7, "indigo"), (11, 2, 3, 15, 10, 7, "saffron"), (1, 2, 8, 5, 10, 12, "emerald"), (6, 2, 8, 10, 12, 12, "silk_p"), (11, 2, 8, 15, 14, 12, "turquoise"),
        (2, 12, 4, 4, 13, 6, "gold")]),
    # ================================================== ARTISANS
    dict(id="blacksmith_forge", fr="Forge de forgeron", en="Blacksmith forge", anim="fire", light=12, elements=[
        (0, 0, 2, 16, 8, 14, "adobe"), (1, 8, 3, 15, 10, 13, "dgray"), (3, 10, 4, 13, 11, 12, "black", {"up": "anim"}), (2, 11, 3, 14, 12, 13, "dgray"), (11, 12, 10, 15, 28, 14, "adobe"), (12, 12, 11, 14, 30, 13, "dgray"),
        (0, 8, 2, 3, 12, 6, "steel"), (2, 12, 5, 4, 13, 8, "steel"), (13, 0, 2, 16, 6, 4, "dwood")]),
    dict(id="potter_wheel", fr="Tour de potier", en="Potter's wheel", elements=[
        (2, 0, 2, 14, 2, 14, "dwood"), (7, 2, 7, 9, 8, 9, "wood"), (3, 8, 3, 13, 10, 13, "wood"), (4, 10, 4, 12, 11, 12, "tcotta"), (6, 11, 6, 10, 15, 10, "tcotta"), (7, 15, 7, 9, 17, 9, "tcotta"), (0, 0, 0, 3, 6, 3, "tcotta"),
        (13, 0, 13, 16, 4, 16, "tcotta"), (12, 0, 1, 15, 3, 5, "tan")]),
    dict(id="weaving_loom", fr="Métier à tisser", en="Weaving loom", elements=[
        (0, 0, 2, 2, 26, 14, "wood"), (14, 0, 2, 16, 26, 14, "wood"), (0, 24, 2, 16, 26, 14, "wood"), (0, 6, 3, 16, 8, 5, "dwood"), (2, 8, 4, 14, 24, 4.4, "henna"), (3, 10, 4.2, 13, 16, 4.6, "indigo"), (4, 12, 4.4, 12, 14, 4.8, "saffron"),
        (2, 4, 6, 14, 5, 12, "wood"), (2, 0, 8, 14, 4, 12, "dune"), (5, 8, 5, 11, 10, 7, "emerald")]),
    dict(id="tannery_vat", fr="Cuve de tanneur", en="Tannery vat", elements=[
        (0, 0, 0, 16, 8, 16, "adobe"), (1, 8, 1, 15, 9, 15, "tan"), (2, 7, 2, 14, 8.6, 14, "soil"), (3, 8, 3, 7, 9, 7, "leather"), (9, 8, 8, 13, 9, 12, "leather"), (0, 8, 0, 16, 10, 1, "adobe"), (0, 8, 15, 16, 10, 16, "adobe"),
        (0, 8, 1, 1, 10, 15, "adobe"), (15, 8, 1, 16, 10, 15, "adobe")]),
    dict(id="dye_vats", fr="Cuves de teinturier", en="Dye vats", elements=[
        (0, 0, 2, 8, 6, 8, "adobe"), (8, 0, 2, 16, 6, 8, "adobe"), (0, 0, 9, 8, 6, 15, "adobe"), (8, 0, 9, 16, 6, 15, "adobe"), (1, 6, 3, 7, 6.8, 7, "indigo"), (9, 6, 3, 15, 6.8, 7, "henna"), (1, 6, 10, 7, 6.8, 14, "saffron"),
        (9, 6, 10, 15, 6.8, 14, "emerald"), (6, 8, 7.6, 10, 18, 8.4, "wood"), (2, 6, 7.8, 14, 7.4, 8.4, "dune")]),
    dict(id="jeweler_bench", fr="Établi de bijoutier", en="Jeweler's bench", elements=table(12, "wood", "dwood", 0, 16, 2, 14) + [
        (1, 12, 3, 7, 12.6, 9, "wine"), (2, 12.6, 4, 4, 13.2, 6, "gold"), (5, 12.6, 5, 6, 13.2, 7, "emerald"), (9, 12, 3, 14, 15, 9, "brass"), (10, 15, 4, 13, 18, 8, "glass"), (11, 12, 10, 15, 14, 13, "steel"), (3, 12, 10, 7, 13, 13, "chrome")]),
    # ================================================== AGRICULTURE ET OASIS
    dict(id="shaduf_well", fr="Puits à balancier", en="Shaduf well", elements=[
        (1, 0, 1, 7, 6, 7, "tan"), (2, 6, 2, 6, 7, 6, "water"), (1, 6, 1, 7, 8, 2, "tan"), (1, 6, 6, 7, 8, 7, "tan"), (11, 0, 7, 13, 24, 9, "wood"), (3, 21, 7.4, 15, 23, 8.6, "dwood"), (14, 16, 7, 16, 21, 9, "tan"), (3, 8, 7.8, 4, 21, 8.2, "rope"),
        (2, 6, 7, 5, 9, 9, "wood")]),
    dict(id="date_drying_rack", fr="Séchoir à dattes", en="Date drying rack", elements=[
        (0, 0, 3, 2, 18, 5, "wood"), (14, 0, 3, 16, 18, 5, "wood"), (0, 0, 11, 2, 18, 13, "wood"), (14, 0, 11, 16, 18, 13, "wood"), (0, 6, 3, 16, 7, 13, "wood"), (0, 12, 3, 16, 13, 13, "wood"), (1, 7, 4, 15, 8, 12, "dwood"),
        (2, 7, 5, 6, 9, 9, "cumin"), (9, 7, 6, 14, 9, 11, "cumin"), (3, 13, 5, 8, 15, 10, "cumin"), (10, 13, 4, 14, 15, 9, "dwood")]),
    dict(id="noria_wheel", fr="Noria", en="Noria wheel", elements=[
        (0, 0, 6, 2, 12, 10, "wood"), (14, 0, 6, 16, 12, 10, "wood"), (2, 10, 7, 14, 14, 9, "dwood"), (3, 2, 6, 13, 3, 10, "wood"), (3, 21, 6, 13, 22, 10, "wood"), (1, 6, 6, 2, 18, 10, "wood"), (14, 6, 6, 15, 18, 10, "wood"),
        (6, 14, 6, 10, 18, 10, "dwood"), (3, 3, 5.4, 5, 7, 6, "tcotta"), (11, 17, 5.4, 13, 21, 6, "tcotta"), (6, 0, 4, 10, 2, 12, "water")]),
    dict(id="camel_standing", fr="Dromadaire", en="Dromedary", elements=[
        (2, 0, 4, 4, 11, 6, "camel"), (2, 0, 10, 4, 11, 12, "camel"), (11, 0, 4, 13, 11, 6, "camel"), (11, 0, 10, 13, 11, 12, "camel"), (1, 11, 3, 14, 18, 13, "camel"), (6, 18, 5, 10, 22, 11, "camel"), (0, 15, 6, 3, 28, 10, "camel"),
        (0, 26, 5.6, 4, 30, 10.4, "camel"), (0, 28, 5, 1, 29, 6, "carbon"), (5, 14, 2.6, 11, 17, 3, "henna"), (13, 14, 6, 16, 16, 7, "camel")]),
    dict(id="irrigation_channel", fr="Canal d'irrigation", en="Irrigation channel", passable=True, elements=[
        (0, 0, 4, 16, 2, 12, "adobe"), (0, 2, 4, 16, 3, 6, "tan"), (0, 2, 10, 16, 3, 12, "tan"), (1, 1, 6, 15, 2.4, 10, "water"), (3, 3, 3, 5, 6, 4, "wood"), (11, 3, 3, 13, 6, 4, "wood"), (4, 2, 6.4, 12, 2.6, 9.6, "leaf"),
        (6, 2, 2, 10, 4, 4, "dune")]),
    dict(id="grain_sack_pile", fr="Tas de sacs de grain", en="Grain sack pile", elements=[
        (0, 0, 2, 7, 6, 8, "dune"), (8, 0, 2, 15, 6, 8, "cream"), (4, 6, 3, 11, 11, 9, "dune"), (1, 0, 9, 8, 5, 15, "cream"), (9, 0, 9, 15, 4, 15, "dune"), (2, 5, 4, 5, 6, 5, "rope"), (9, 5, 4, 12, 6, 5, "rope"), (5, 10, 5, 8, 11, 7, "hay"),
        (3, 4, 10, 7, 5, 11, "rope")]),
    # ================================================== CARAVANE ET NOMADES
    dict(id="bedouin_tent", fr="Tente bédouine", en="Bedouin tent", elements=[
        (0, 0, 1, 16, 1, 15, "dune"), (1, 1, 2, 15, 6, 14, "camel"), (3, 6, 3, 13, 10, 13, "dgray"), (5, 10, 4, 11, 13, 12, "camel"), (7, 13, 5, 9, 16, 11, "dgray"), (0, 0, 7, 1, 16, 9, "wood"), (15, 0, 7, 16, 16, 9, "wood"),
        (5, 1, 0.8, 11, 5, 1.6, "carbon"), (2, 1, 2, 4, 4, 3, "henna"), (12, 1, 2, 14, 4, 3, "indigo")]),
    dict(id="camel_saddle_stand", fr="Selle de dromadaire sur support", en="Camel saddle stand", elements=[
        (2, 0, 4, 4, 12, 6, "wood"), (12, 0, 4, 14, 12, 6, "wood"), (2, 0, 10, 4, 12, 12, "wood"), (12, 0, 10, 14, 12, 12, "wood"), (1, 12, 3, 15, 14, 13, "dwood"), (2, 14, 4, 14, 17, 12, "henna"), (4, 17, 5, 12, 21, 11, "leather"),
        (7, 21, 6, 9, 24, 10, "leather"), (3, 14, 3, 13, 15, 4, "gold"), (13, 8, 6, 16, 10, 7, "rope")]),
    dict(id="waterskin_rack", fr="Support d'outres", en="Waterskin rack", elements=[
        (1, 0, 5, 3, 24, 7, "wood"), (13, 0, 5, 15, 24, 7, "wood"), (1, 22, 5, 15, 24, 7, "wood"), (2, 6, 6, 6, 20, 10, "leather"), (3, 20, 6.4, 5, 22, 8.6, "leather"), (7, 8, 6, 11, 20, 10, "tan"), (8, 20, 6.4, 10, 22, 8.6, "tan"),
        (11, 5, 6, 14, 18, 10, "leather"), (2, 15, 5.8, 6, 16, 6.2, "rope")]),
    dict(id="cargo_bales", fr="Ballots de cargaison", en="Cargo bales", elements=[
        (0, 0, 2, 8, 8, 10, "dune"), (8, 0, 2, 16, 7, 10, "camel"), (3, 8, 3, 11, 14, 9, "dune"), (1, 0, 10, 9, 6, 16, "camel"), (9, 0, 10, 16, 8, 16, "dune"), (0, 3, 1.6, 8, 4, 10.4, "rope"), (8, 3, 1.6, 16, 4, 10.4, "rope"),
        (3, 11, 2.6, 11, 12, 9.4, "rope"), (5, 8, 10, 7, 9, 12, "henna")]),
    dict(id="nomad_fire", fr="Foyer nomade", en="Nomad fire", anim="fire", light=14, elements=[
        (2, 0, 2, 14, 2, 14, "stone"), (4, 2, 4, 12, 3, 12, "ash"), (5, 3, 5, 11, 8, 11, "black", {"up": "anim", "north": "anim", "south": "anim", "east": "anim", "west": "anim"}), (1, 0, 7, 4, 3, 10, "stone"), (12, 0, 7, 15, 3, 10, "stone"),
        (2, 3, 3, 4, 14, 4, "wood"), (12, 3, 3, 14, 14, 4, "wood"), (2, 13, 3, 14, 14, 4, "wood"), (6, 8, 3, 10, 12, 4, "copper")]),
    dict(id="caravan_signpost", fr="Poteau de caravane", en="Caravan signpost", elements=[
        (7, 0, 7, 9, 28, 9, "wood"), (2, 22, 7.4, 8, 26, 8.6, "dwood"), (8, 18, 7.4, 14, 22, 8.6, "dwood"), (1, 23, 7.2, 3, 25, 8.8, "dwood"), (13, 19, 7.2, 15, 21, 8.8, "dwood"), (3, 23, 7, 7, 25, 7.4, "paper"), (9, 19, 7, 13, 21, 7.4, "paper"),
        (5, 0, 5, 11, 2, 11, "dune"), (6, 28, 6, 10, 30, 10, "henna")]),
    # ================================================== MILICE ET ARMURERIE
    dict(id="desert_rifle_rack", fr="Râtelier d'AK du désert", en="Desert rifle rack", elements=[
        (0, 0, 5, 16, 3, 11, "dwood"), (0, 24, 5, 16, 26, 11, "dwood"), (0, 3, 5, 2, 24, 11, "camo_sand"), (14, 3, 5, 16, 24, 11, "camo_sand"), (2, 4, 7.4, 3.4, 22, 8.6, "carbon"), (4.6, 4, 7.4, 6, 22, 8.6, "carbon"), (7.2, 4, 7.4, 8.6, 22, 8.6, "carbon"),
        (9.8, 4, 7.4, 11.2, 22, 8.6, "carbon"), (12.4, 4, 7.4, 13.8, 22, 8.6, "carbon"), (2, 4, 7, 3.4, 12, 9, "wood"), (4.6, 4, 7, 6, 12, 9, "wood"), (7.2, 4, 7, 8.6, 12, 9, "wood")]),
    dict(id="militia_crates", fr="Caisses de la milice", en="Militia crates", elements=[
        (0, 0, 1, 10, 7, 9, "kaki_d"), (6, 0, 8, 16, 7, 16, "kaki_d"), (2, 7, 2, 12, 12, 8, "olive"), (0, 3, 1, 10, 4, 9, "dgray"), (6, 3, 8, 16, 4, 16, "dgray"), (2, 5, 0.6, 8, 6, 1, "paper"), (9, 4, 7.6, 13, 5, 8, "paper"),
        (3, 12, 3, 5, 14, 5, "mustard"), (1, 7.4, 4, 3, 8, 6, "steel")]),
    dict(id="desert_sandbag_wall", fr="Mur de sacs de sable du désert", en="Desert sandbag wall", elements=[
        (0, 0, 3, 8, 4, 8, "dune"), (8, 0, 3, 16, 4, 8, "camel"), (4, 4, 3, 12, 8, 8, "dune"), (0, 4, 3, 5, 8, 8, "camel"), (11, 4, 3, 16, 8, 8, "camel"), (0, 8, 3, 8, 11, 8, "camel"), (8, 8, 3, 16, 11, 8, "dune"),
        (3, 0, 8, 13, 3, 11, "dune"), (6, 3, 8, 10, 5, 9, "rope")]),
    dict(id="militia_flag", fr="Drapeau de la milice", en="Militia flag", elements=[
        (5, 0, 5, 11, 2, 11, "stone"), (7.4, 2, 7.4, 8.6, 30, 8.6, "steel"), (8.6, 16, 7.4, 16, 28, 8.6, "dred"), (9, 20, 7.2, 14, 24, 8.8, "kaki_d"), (10, 21, 7, 13, 23, 9, "gold"), (7, 30, 7, 9, 32, 9, "gold"), (6, 2, 6, 10, 3, 10, "dgray")]),
    dict(id="rpg_rack", fr="Support de lance-roquettes", en="RPG rack", elements=[
        (0, 0, 4, 16, 3, 12, "dwood"), (1, 3, 5, 3, 22, 11, "camo_sand"), (13, 3, 5, 15, 22, 11, "camo_sand"), (1, 20, 5, 15, 22, 11, "camo_sand"), (3, 6, 6.6, 6, 16, 9.4, "olive"), (3, 16, 6.2, 6, 20, 9.8, "dgray"), (7, 6, 6.6, 10, 16, 9.4, "olive"),
        (7, 16, 6.2, 10, 20, 9.8, "dgray"), (11, 6, 6.6, 13, 16, 9.4, "olive"), (11, 16, 6.2, 13, 20, 9.8, "dgray")]),
    dict(id="desert_target_dummy", fr="Mannequin de tir du désert", en="Desert target dummy", elements=[
        (3, 0, 3, 13, 2, 13, "dwood"), (7, 2, 7, 9, 16, 9, "wood"), (4, 16, 6, 12, 26, 10, "camo_sand"), (5, 26, 6.4, 11, 29, 9.6, "dune"), (2, 18, 6.4, 4, 24, 9.6, "camo_sand"), (12, 18, 6.4, 14, 24, 9.6, "camo_sand"), (5, 20, 5.6, 11, 24, 6, "dred"),
        (6, 21, 5.4, 10, 23, 5.8, "carbon"), (5, 28, 6, 11, 29, 10, "khaki")]),
    # ================================================== POSTES DE GARDE ET TOURS
    dict(id="checkpoint_booth_d", fr="Guérite de contrôle du désert", en="Desert checkpoint booth", elements=[
        (1, 0, 1, 15, 2, 15, "adobe"), (2, 2, 2, 14, 16, 14, "tan"), (3, 6, 1.4, 13, 13, 2, "glass"), (1, 16, 1, 15, 18, 15, "tcotta"), (6, 2, 1.4, 10, 12, 2, "dwood"), (0, 5, 5, 2, 8, 11, "dred"), (3, 18, 3, 13, 19, 13, "dune"),
        (7, 19, 7, 9, 24, 9, "steel"), (8.4, 22, 7.6, 12, 24, 8.4, "dred")]),
    dict(id="adobe_watchtower", fr="Tour de guet en adobe", en="Adobe watchtower", elements=[
        (2, 0, 2, 14, 18, 14, "adobe"), (3, 18, 3, 13, 20, 13, "tan"), (1, 20, 1, 15, 22, 15, "adobe"), (1, 22, 1, 3, 25, 3, "adobe"), (13, 22, 1, 15, 25, 3, "adobe"), (1, 22, 13, 3, 25, 15, "adobe"), (13, 22, 13, 15, 25, 15, "adobe"),
        (6, 6, 1.4, 10, 12, 2, "dgray"), (6, 20, 1.4, 10, 22, 2, "dgray"), (7, 22, 7, 9, 28, 9, "steel")]),
    dict(id="crenel_wall", fr="Muraille crénelée", en="Crenellated wall", elements=[
        (0, 0, 3, 16, 20, 13, "tan"), (0, 20, 3, 4, 24, 13, "tan"), (6, 20, 3, 10, 24, 13, "tan"), (12, 20, 3, 16, 24, 13, "tan"), (0, 0, 2.6, 16, 2, 3, "adobe"), (3, 8, 2.4, 5, 14, 3, "dgray"), (11, 8, 2.4, 13, 14, 3, "dgray"),
        (7, 12, 2.6, 9, 16, 3, "dgray")]),
    dict(id="portcullis_gate", fr="Herse de porte fortifiée", en="Portcullis gate", elements=[
        (0, 0, 4, 3, 26, 12, "tan"), (13, 0, 4, 16, 26, 12, "tan"), (0, 24, 4, 16, 28, 12, "tan"), (3, 6, 7.4, 13, 24, 8.6, "steel"), (4, 6, 7, 5, 24, 9, "dgray"), (7, 6, 7, 8, 24, 9, "dgray"), (10, 6, 7, 11, 24, 9, "dgray"),
        (3, 12, 7, 13, 13, 9, "dgray"), (3, 18, 7, 13, 19, 9, "dgray"), (3, 24, 7, 13, 26, 9, "dwood")]),
    dict(id="timber_barricade", fr="Barricade de madriers", en="Timber barricade", elements=[
        (0, 0, 6, 16, 4, 10, "dwood"), (1, 4, 6.4, 15, 8, 9.6, "wood"), (0, 8, 6, 16, 12, 10, "dwood"), (2, 0, 4, 4, 20, 6, "wood"), (12, 0, 4, 14, 20, 6, "wood"), (6, 0, 10, 8, 16, 12, "wood"), (3, 12, 5.6, 13, 14, 6.2, "rope"),
        (7, 14, 5, 9, 18, 6, "rust")]),
    dict(id="desert_floodlight", fr="Projecteur de mirador", en="Watchtower floodlight", light=15, elements=[
        (3, 0, 3, 13, 2, 13, "adobe"), (7, 2, 7, 9, 22, 9, "steel"), (2, 22, 3, 14, 30, 13, "dgray"), (3, 23, 2.4, 13, 29, 3, "lamp"), (1, 24, 6, 2, 28, 10, "steel"), (14, 24, 6, 15, 28, 10, "steel"), (5, 30, 5, 11, 31, 11, "carbon"),
        (9, 4, 6, 14, 8, 10, "dred")]),
    # ================================================== TRAFIC ET CONTREBANDE
    dict(id="smuggler_crates", fr="Caisses du trafiquant", en="Smuggler crates", elements=[
        (0, 0, 1, 8, 7, 9, "wood"), (8, 0, 2, 16, 6, 10, "dwood"), (3, 7, 2, 11, 12, 8, "wood"), (1, 0, 10, 9, 5, 16, "dwood"), (10, 0, 11, 16, 8, 16, "wood"), (1, 3, 0.6, 7, 4, 1, "rope"), (3, 9, 1.6, 6, 10, 2, "paprika"),
        (11, 4, 10.6, 14, 5, 11, "carbon"), (4, 3, 10, 8, 4, 10.6, "rust")]),
    dict(id="false_floor_hatch", fr="Trappe de cache", en="Hidden floor hatch", passable=True, elements=[
        (0, 0, 0, 16, 2, 16, "dune"), (1, 2, 1, 15, 3, 15, "sand"), (3, 3, 3, 13, 4, 13, "dwood"), (4, 4, 4, 12, 4.6, 12, "wood"), (7, 4.6, 7, 9, 5.4, 9, "brass"), (3, 3, 3, 4, 4.6, 13, "dgray"), (12, 3, 3, 13, 4.6, 13, "dgray"),
        (6, 3, 12, 10, 4, 13, "rope")]),
    dict(id="precision_scale", fr="Balance de précision", en="Precision scale", elements=table(10, "wood", "dwood", 2, 14, 3, 13) + [
        (4, 10, 5, 12, 12, 11, "steel"), (5, 12, 6, 11, 14, 10, "chrome"), (6, 12, 5.6, 10, 13, 6, "lamp"), (4, 14, 7, 7, 15, 9, "steel"), (9, 14, 7, 12, 15, 9, "steel"), (7, 12, 7.6, 9, 18, 8.4, "chrome"), (5, 15, 7, 6, 17, 9, "paprika")]),
    dict(id="smuggler_desk", fr="Bureau du trafiquant", en="Smuggler's desk", light=4, elements=table(13, "dwood", "carbon", 0, 16, 1, 15) + [
        (0, 13, 1, 16, 14, 15, "leather"), (2, 14, 4, 8, 14.4, 12, "paper"), (3, 14.4, 5, 6, 15, 8, "paprika"), (10, 14, 4, 14, 16, 8, "carbon"), (11, 16, 5, 13, 17, 7, "steel"), (11, 14, 10, 14, 18, 13, "lamp"), (10, 14, 10, 15, 15, 14, "brass"),
        (6, 3, 1, 10, 11, 2, "carbon"), (7, 6, 0.6, 9, 8, 1, "brass")]),
    dict(id="hidden_jerrycans", fr="Jerricans cachés", en="Hidden jerrycans", elements=[
        (0, 0, 2, 5, 11, 8, "olive"), (6, 0, 3, 11, 10, 9, "kaki_d"), (11, 0, 2, 16, 11, 8, "olive"), (1, 0, 9, 6, 9, 15, "kaki_d"), (8, 0, 10, 14, 10, 16, "olive"), (1, 11, 3, 4, 13, 7, "olive"), (12, 11, 3, 15, 13, 7, "olive"),
        (0, 0, 8, 16, 3, 16, "dune"), (1, 6, 1.6, 4, 9, 2.2, "paper")]),
    dict(id="cash_suitcase_d", fr="Valise de billets", en="Cash suitcase", elements=[
        (1, 0, 4, 15, 4, 12, "leather"), (1, 4, 4, 15, 7, 5, "leather"), (1, 4, 11, 15, 7, 12, "leather"), (2, 4, 5, 14, 6, 11, "green"), (2, 6, 5, 14, 7, 11, "emerald"), (4, 6.6, 6, 8, 8, 9, "paper"), (9, 6.6, 6, 12, 8.4, 9, "paper"),
        (6, 0, 3.4, 10, 3, 4, "brass"), (0, 1, 7, 1, 3, 9, "brass"), (15, 1, 7, 16, 3, 9, "brass")]),
    # ================================================== RUINES ET USURE
    dict(id="cracked_wall_section", fr="Pan de mur fissuré", en="Cracked wall section", elements=[
        (0, 0, 5, 16, 14, 11, "tan"), (0, 14, 5, 7, 20, 11, "tan"), (9, 14, 5, 16, 17, 11, "tan"), (3, 10, 4.6, 5, 24, 5, "dgray"), (4, 6, 4.6, 6, 12, 5, "dgray"), (9, 4, 4.6, 11, 12, 5, "dgray"), (7, 14, 4.6, 9, 20, 5, "dgray"),
        (11, 14, 4.6, 14, 16, 5, "adobe"), (1, 0, 3, 7, 3, 5, "dune")]),
    dict(id="bullet_pocked_stucco", fr="Crépi criblé d'impacts", en="Bullet-pocked stucco", passable=True, elements=[
        (0, 0, 14, 16, 16, 16, "tan"), (1, 12, 13.6, 4, 14, 14, "adobe"), (6, 2, 13.6, 11, 5, 14, "adobe"), (3, 3, 13.4, 4, 4, 13.8, "carbon"), (8, 9, 13.4, 9, 10, 13.8, "carbon"), (11, 6, 13.4, 12, 7, 13.8, "carbon"), (5, 12, 13.4, 6, 13, 13.8, "carbon"),
        (12, 11, 13.4, 13, 12, 13.8, "carbon"), (2, 7, 13.4, 3, 8, 13.8, "carbon"), (13, 2, 13.4, 14, 3, 13.8, "carbon")]),
    dict(id="wrecked_pickup", fr="Pick-up calciné", en="Burnt-out pickup", elements=[
        (0, 3, 2, 16, 6, 14, "rust"), (1, 6, 3, 8, 12, 13, "rust"), (9, 6, 3, 15, 9, 13, "carbon"), (2, 7, 2.6, 7, 11, 3, "carbon"), (0, 0, 1, 4, 4, 4, "carbon"), (12, 0, 1, 16, 4, 4, "carbon"), (0, 0, 12, 4, 4, 15, "carbon"),
        (12, 0, 12, 16, 4, 15, "carbon"), (9, 9, 3, 15, 10, 13, "rust"), (0, 6, 6, 1, 8, 10, "carbon")]),
    dict(id="sand_drift", fr="Dune de sable accumulée", en="Sand drift", passable=True, elements=[
        (0, 0, 0, 16, 3, 16, "dune"), (1, 3, 2, 15, 5, 14, "dune"), (3, 5, 4, 13, 7, 12, "sand"), (5, 7, 6, 11, 8, 10, "sand"), (0, 0, 10, 6, 5, 16, "sand"), (10, 0, 0, 16, 4, 5, "sand"), (7, 8, 7, 9, 12, 8, "wood"), (2, 3, 3, 4, 4, 5, "tan")]),
    dict(id="torn_militia_flag", fr="Drapeau de milice déchiré", en="Torn militia flag", elements=[
        (7.4, 0, 7.4, 8.6, 28, 8.6, "dgray"), (8.6, 16, 7.4, 16, 26, 8.6, "dred"), (9, 17, 7.2, 14, 21, 8.8, "dred"), (12, 15, 7.2, 14, 17, 8.8, "dred"), (14, 20, 7.2, 16, 26, 8.8, "dred"), (9, 22, 7.2, 11, 25, 8.8, "kaki_d"),
        (4, 0, 4, 12, 2, 12, "dune"), (5, 2, 5, 11, 4, 11, "tan"), (7, 28, 7, 9, 29, 9, "rust")]),
    dict(id="rubble_pile_d", fr="Tas de gravats", en="Rubble pile", elements=[
        (0, 0, 2, 12, 4, 12, "tan"), (3, 4, 4, 10, 8, 10, "adobe"), (5, 8, 6, 8, 10, 8, "tan"), (10, 0, 6, 16, 5, 14, "adobe"), (1, 0, 11, 7, 3, 16, "tan"), (2, 4, 3, 5, 6, 5, "dgray"), (11, 5, 8, 14, 7, 11, "tan"),
        (7, 10, 6.4, 9, 12, 7.6, "rust"), (6, 4, 11, 9, 5, 13, "dwood")]),
    # ================================================== ENSEIGNES ET SIGNALÉTIQUE
    dict(id="arabic_shop_sign", fr="Enseigne de boutique en arabe", en="Arabic shop sign", passable=True, elements=[
        (0, 4, 14, 16, 12, 16, "indigo"), (1, 5, 13.6, 15, 11, 14, "gold"), (2, 6, 13.2, 14, 10, 13.6, "indigo"), (3, 7, 13, 5, 9, 13.2, "gold"), (6, 8, 13, 8, 9.6, 13.2, "gold"), (9, 7, 13, 11, 9, 13.2, "gold"), (12, 7.4, 13, 13, 9.4, 13.2, "gold"),
        (0, 11, 14.4, 1, 14, 16, "dwood"), (15, 11, 14.4, 16, 14, 16, "dwood")]),
    dict(id="calligraphy_plate", fr="Plaque de calligraphie", en="Calligraphy plate", passable=True, elements=[
        (1, 2, 14.6, 15, 14, 16, "dwood"), (2, 3, 14.2, 14, 13, 14.6, "paper"), (3, 4, 14, 6, 12, 14.2, "carbon"), (7, 6, 14, 9, 11, 14.2, "carbon"), (10, 4, 14, 12, 9, 14.2, "carbon"), (4, 9, 13.8, 12, 10, 14.2, "carbon"),
        (7, 4, 13.8, 13, 5, 14.2, "carbon"), (2, 2, 14.2, 14, 3, 14.6, "gold"), (2, 13, 14.2, 14, 14, 14.6, "gold")]),
    dict(id="souk_banner", fr="Bannière du souk", en="Souk banner", passable=True, elements=[
        (0, 14, 14.6, 16, 16, 15.4, "dwood"), (1, 2, 14.8, 15, 14, 15.2, "henna"), (2, 3, 14.5, 14, 13, 14.8, "saffron"), (3, 5, 14.2, 13, 11, 14.5, "indigo"), (5, 6, 14, 11, 10, 14.2, "gold"), (7, 0, 14.8, 9, 2, 15.2, "henna"),
        (3, 0, 14.8, 5, 3, 15.2, "henna"), (11, 0, 14.8, 13, 3, 15.2, "henna")]),
    dict(id="street_name_tile", fr="Plaque de rue en zellige", en="Zellige street name tile", passable=True, elements=[
        (2, 5, 14.4, 14, 11, 16, "zellige"), (3, 6, 14, 13, 10, 14.4, "star_white"), (4, 7, 13.8, 6, 9, 14, "indigo"), (7, 7.4, 13.8, 9, 8.6, 14, "indigo"), (10, 6.6, 13.8, 12, 9.4, 14, "indigo"), (2, 5, 14.2, 3, 11, 14.4, "gold"),
        (13, 5, 14.2, 14, 11, 14.4, "gold")]),
    dict(id="stone_stele", fr="Stèle de pierre gravée", en="Engraved stone stele", elements=[
        (3, 0, 4, 13, 3, 12, "dune"), (4, 3, 5, 12, 24, 11, "tan"), (5, 24, 5.4, 11, 27, 10.6, "tan"), (6, 27, 6, 10, 29, 10, "tan"), (6, 6, 4.6, 10, 8, 5, "carbon"), (5, 10, 4.6, 11, 12, 5, "carbon"), (6, 14, 4.6, 9, 16, 5, "carbon"),
        (5, 18, 4.6, 10, 19, 5, "carbon"), (7, 21, 4.6, 9, 22, 5, "gold")]),
    dict(id="hotel_sign_d", fr="Enseigne d'hôtel du désert", en="Desert hotel sign", light=8, anim="flicker", elements=[
        (7, 0, 7, 9, 18, 9, "steel"), (1, 18, 6, 15, 30, 10, "indigo"), (2, 19, 5.6, 14, 29, 6, "carbon"), (3, 21, 5.4, 13, 27, 5.8, "lamp", {"north": "anim"}), (3, 28, 5.6, 5, 29, 6, "saffron"), (11, 28, 5.6, 13, 29, 6, "saffron"),
        (0, 17, 6, 16, 18, 10, "gold"), (6, 0, 6, 10, 2, 10, "dgray")]),
    # ================================================== FÊTES ET MUSIQUE
    dict(id="oud_stand", fr="Oud sur support", en="Oud stand", elements=[
        (2, 0, 4, 6, 2, 12, "dwood"), (10, 0, 4, 14, 2, 12, "dwood"), (3, 2, 5, 5, 16, 6, "wood"), (11, 2, 5, 13, 16, 6, "wood"), (4, 8, 4, 12, 15, 12, "wood"), (5, 9, 3.4, 11, 14, 4, "dwood"), (7, 10, 3.2, 9, 13, 3.6, "carbon"),
        (7, 15, 6, 9, 26, 8, "dwood"), (6, 26, 5.6, 10, 30, 8.4, "dwood"), (5, 12, 3, 11, 13, 3.4, "gold")]),
    dict(id="darbouka_drums", fr="Darboukas", en="Darbuka drums", elements=[
        (1, 0, 2, 7, 4, 8, "copper"), (2, 4, 3, 6, 10, 7, "copper"), (1, 10, 2, 7, 12, 8, "tan"), (2, 12, 3, 6, 13, 7, "cream"), (9, 0, 4, 15, 4, 10, "copper"), (10, 4, 5, 14, 9, 9, "copper"), (9, 9, 4, 15, 11, 10, "tan"),
        (10, 11, 5, 14, 12, 9, "cream"), (4, 0, 10, 10, 3, 15, "dune")]),
    dict(id="festival_lantern_string", fr="Guirlande de lampions", en="Festival lantern string", anim="garland", light=9, passable=True, elements=[
        (0, 13, 7.4, 16, 14, 8.6, "rope", {"north": "anim", "south": "anim"}), (0, 6, 7, 1, 16, 9, "wood"), (15, 6, 7, 16, 16, 9, "wood"), (2, 9, 7, 4, 13, 9, "henna"), (6, 9, 7, 8, 13, 9, "saffron"), (10, 9, 7, 12, 13, 9, "turquoise"),
        (13, 10, 7.4, 15, 13, 8.6, "silk_p")]),
    dict(id="dance_carpet", fr="Tapis de danse", en="Dance carpet", passable=True, elements=[
        (0, 0, 0, 16, 1, 16, "henna"), (1, 1, 1, 15, 1.4, 15, "indigo"), (2, 1.4, 2, 14, 1.8, 14, "saffron"), (4, 1.8, 4, 12, 2, 12, "henna"), (7, 2, 7, 9, 2.2, 9, "gold"), (0, 0, 0, 16, 1.2, 1, "gold"), (0, 0, 15, 16, 1.2, 16, "gold"),
        (0, 0, 1, 1, 1.2, 15, "gold"), (15, 0, 1, 16, 1.2, 15, "gold")]),
    dict(id="festival_drum", fr="Grand tambour de fête", en="Festival drum", elements=[
        (2, 0, 2, 14, 2, 14, "dwood"), (3, 2, 3, 13, 14, 13, "henna"), (2, 14, 2, 14, 16, 14, "leather"), (3, 16, 3, 13, 17, 13, "cream"), (3, 4, 2.6, 13, 5, 3, "gold"), (3, 10, 2.6, 13, 11, 3, "gold"), (3, 7, 2.6, 13, 8, 3, "indigo"),
        (12, 17, 6, 14, 22, 7, "wood"), (4, 17, 8, 6, 22, 9, "wood")]),
    dict(id="festival_fireworks", fr="Feux d'artifice de fête", en="Festival fireworks", anim="fireworks", light=11, elements=[
        (2, 0, 2, 14, 2, 14, "carbon"), (3, 2, 3, 13, 10, 13, "henna"), (4, 10, 4, 12, 14, 12, "indigo"), (5, 14, 5, 11, 17, 11, "saffron"), (6, 17, 6, 10, 18, 10, "carbon"), (3, 2, 2.6, 13, 10, 3, "gold", {"north": "anim"}),
        (0, 0, 6, 2, 8, 10, "emerald"), (14, 0, 6, 16, 8, 10, "silk_p"), (7, 18, 7, 9, 22, 9, "henna"), (6.4, 22, 6.4, 9.6, 23, 9.6, "gold")]),
    # ================================================== THÉ ET JEUX
    dict(id="tea_tray", fr="Plateau de thé", en="Tea tray", elements=[
        (0, 0, 1, 16, 1, 15, "copper"), (1, 1, 2, 15, 2, 14, "brass"), (2, 2, 3, 6, 8, 7, "copper"), (3, 8, 4, 5, 11, 6, "copper"), (3.4, 11, 4.4, 4.6, 12, 5.6, "gold"), (8, 2, 3, 10, 5, 5, "glass"), (11, 2, 3, 13, 5, 5, "glass"),
        (8, 2, 9, 10, 5, 11, "glass"), (11, 2, 9, 13, 5, 11, "glass"), (6, 2, 10, 8, 4, 13, "tan")]),
    dict(id="backgammon_table", fr="Table de backgammon", en="Backgammon table", elements=table(11, "wood", "dwood", 1, 15, 1, 15) + [
        (1, 11, 1, 15, 12, 15, "dwood"), (2, 12, 2, 14, 12.6, 14, "tan"), (2, 12.6, 7.6, 14, 12.8, 8.4, "dwood"), (3, 12.6, 3, 4, 13.2, 7, "henna"), (5, 12.6, 3, 6, 13.2, 7, "star_white"), (9, 12.6, 9, 10, 13.2, 13, "henna"),
        (11, 12.6, 9, 12, 13.2, 13, "star_white"), (7, 12.6, 5, 9, 13.8, 7, "star_white"), (10, 12.6, 4, 12, 13.8, 6, "carbon")]),
    dict(id="dama_board", fr="Jeu de dames", en="Dama board", elements=[
        (1, 0, 1, 15, 2, 15, "dwood"), (2, 2, 2, 14, 3, 14, "tan"), (2, 2, 2, 5, 3, 5, "dwood"), (8, 2, 2, 11, 3, 5, "dwood"), (5, 2, 5, 8, 3, 8, "dwood"), (11, 2, 5, 14, 3, 8, "dwood"), (2, 2, 8, 5, 3, 11, "dwood"),
        (8, 2, 8, 11, 3, 11, "dwood"), (5, 2, 11, 8, 3, 14, "dwood"), (11, 2, 11, 14, 3, 14, "dwood"), (3, 3, 3, 4, 4, 4, "henna"), (6, 3, 6, 7, 4, 7, "star_white")]),
    dict(id="kettle_stove", fr="Réchaud à bouilloire", en="Kettle stove", anim="fire", light=5, elements=[
        (3, 0, 3, 13, 5, 13, "copper"), (4, 5, 4, 12, 6, 12, "dgray"), (5, 5.4, 5, 11, 6, 11, "black", {"up": "anim"}), (4, 6, 4, 12, 7, 12, "dgray"), (4, 7, 4, 12, 13, 12, "copper"), (5, 13, 5, 11, 15, 11, "copper"), (7, 15, 7, 9, 17, 9, "gold"),
        (12, 8, 6, 15, 10, 10, "copper"), (2, 10, 7, 4, 14, 9, "copper")]),
    dict(id="dallah_coffee_pot", fr="Cafetière dallah", en="Dallah coffee pot", elements=[
        (3, 0, 4, 13, 2, 12, "brass"), (4, 2, 5, 12, 9, 11, "brass"), (5, 9, 6, 11, 14, 10, "brass"), (6, 14, 7, 10, 17, 9, "brass"), (7, 17, 7, 9, 19, 9, "gold"), (1, 8, 7, 4, 10, 9, "brass"), (12, 7, 7, 15, 9, 9, "brass"),
        (13, 9, 7.4, 15, 12, 8.6, "brass"), (3, 4, 7, 4, 10, 9, "gold")]),
    dict(id="game_cushion_set", fr="Coussins de jeu", en="Game cushion set", elements=[
        (0, 0, 0, 16, 1, 16, "indigo"), (1, 0, 1, 7, 4, 7, "henna"), (9, 0, 1, 15, 4, 7, "saffron"), (1, 0, 9, 7, 4, 15, "emerald"), (9, 0, 9, 15, 4, 15, "silk_p"), (3, 4, 3, 5, 5, 5, "gold"), (11, 4, 3, 13, 5, 5, "gold"),
        (3, 4, 11, 5, 5, 13, "gold"), (11, 4, 11, 13, 5, 13, "gold"), (7, 1, 7, 9, 2, 9, "tan")]),
    # ================================================== MÉDICAL ET RITUELS
    dict(id="herbalist_shelf", fr="Étagère d'herboriste", en="Herbalist shelf", elements=[
        (0, 0, 4, 16, 30, 12, "wood"), (1, 2, 4.4, 15, 3, 11.6, "dwood"), (1, 10, 4.4, 15, 11, 11.6, "dwood"), (1, 18, 4.4, 15, 19, 11.6, "dwood"), (2, 3, 5, 6, 9, 9, "leaf"), (7, 3, 5, 10, 8, 9, "henna"), (11, 3, 5, 14, 9, 9, "saffron"),
        (2, 11, 5, 5, 17, 9, "green"), (7, 11, 5, 11, 16, 9, "dune"), (3, 19, 5, 7, 26, 9, "emerald"), (9, 19, 5, 13, 25, 9, "cumin")]),
    dict(id="herb_jars", fr="Bocaux d'herbes", en="Herb jars", elements=[
        (0, 0, 2, 16, 2, 14, "dwood"), (1, 2, 3, 5, 9, 7, "glass"), (2, 9, 4, 4, 11, 6, "wood"), (2, 2.4, 3.4, 4.6, 7, 6.6, "leaf"), (6, 2, 3, 10, 8, 7, "glass"), (7, 8, 4, 9, 10, 6, "wood"), (6.4, 2.4, 3.4, 9.6, 6, 6.6, "henna"),
        (11, 2, 3, 15, 10, 7, "glass"), (12, 10, 4, 14, 12, 6, "wood"), (11.4, 2.4, 3.4, 14.6, 8, 6.6, "saffron"), (3, 2, 9, 12, 5, 13, "tcotta")]),
    dict(id="talisman_wall", fr="Mur de talismans", en="Talisman wall", passable=True, elements=[
        (1, 1, 14.6, 15, 15, 16, "dwood"), (2, 2, 14.2, 14, 14, 14.6, "indigo"), (3, 10, 14, 6, 13, 14.2, "paper"), (7, 9, 14, 10, 13, 14.2, "paper"), (11, 10, 14, 13, 13, 14.2, "paper"), (4, 4, 14, 7, 7, 14.2, "gold"),
        (9, 4, 14, 12, 8, 14.2, "turquoise"), (7.4, 7, 13.8, 8.6, 8.2, 14, "carbon"), (6, 2, 14, 7, 4, 14.2, "henna")]),
    dict(id="incense_burner", fr="Brûle-encens", en="Incense burner", anim="steam", light=3, elements=[
        (4, 0, 4, 12, 2, 12, "copper"), (5, 2, 5, 11, 5, 11, "brass"), (3, 5, 3, 13, 8, 13, "copper"), (4, 8, 4, 12, 10, 12, "brass"), (5, 9, 5, 11, 12, 11, "steam_w", {"up": "anim"}), (6, 10, 6, 10, 14, 10, "steam_w", {"up": "anim"}),
        (7, 14, 7, 9, 16, 9, "gold"), (7, 12, 7, 9, 18, 9, "steam_w", {"up": "anim", "north": "anim"})]),
    dict(id="dispensary_cot", fr="Lit du dispensaire", en="Dispensary cot", elements=[
        (0, 5, 1, 16, 8, 15, "wood"), (1, 8, 2, 15, 10, 14, "med_white"), (1, 10, 2, 5, 12, 6, "med_white"), (6, 10, 2, 15, 11, 14, "indigo"), (0, 0, 1, 2, 5, 3, "wood"), (14, 0, 1, 16, 5, 3, "wood"), (0, 0, 13, 2, 5, 15, "wood"),
        (14, 0, 13, 16, 5, 15, "wood"), (7, 11, 4, 9, 12, 6, "red"), (7, 11, 4.4, 9, 11.4, 5.6, "med_white")]),
    dict(id="remedy_cabinet", fr="Armoire à remèdes", en="Remedy cabinet", elements=[
        (1, 0, 3, 15, 30, 13, "tan"), (2, 2, 2.4, 14, 28, 3, "wood"), (3, 18, 2, 7, 27, 2.4, "glass"), (9, 18, 2, 13, 27, 2.4, "glass"), (3, 19, 2.2, 6, 21, 2.6, "leaf"), (10, 21, 2.2, 12, 24, 2.6, "henna"), (3, 4, 2, 13, 16, 2.4, "dwood"),
        (7.6, 8, 1.8, 8.4, 12, 2.4, "brass"), (12, 24, 1.8, 13, 26, 2.4, "gold"), (5, 3, 1.8, 11, 4, 2.4, "paper")]),
    dict(id="sundial_hourglass", fr="Sablier du devin", en="Fortune teller hourglass", anim="sandfall", elements=[
        (4, 0, 4, 12, 2, 12, "dwood"), (5, 2, 5, 6, 18, 6, "gold"), (10, 2, 5, 11, 18, 6, "gold"), (5, 2, 10, 6, 18, 11, "gold"), (10, 2, 10, 11, 18, 11, "gold"), (4, 18, 4, 12, 20, 12, "dwood"), (6, 3, 6, 10, 17, 10, "glass", {"north": "anim", "south": "anim", "east": "anim", "west": "anim"}),
        (7, 8, 7, 9, 12, 9, "dune")]),
]

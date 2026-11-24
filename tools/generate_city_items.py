#!/usr/bin/env python3
"""
Objets des villes et factions : la Pourfendeuse de Dragons (modèle 3D voxel, 2,4 blocs), cartes d'accès,
insigne du Faucon Blanc, œufs d'apparition des bots.
Utilisation : python3 tools/generate_city_items.py (puis tools/enhance_icons.py pour les icônes 2D)
"""
import json
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import voxel_bake as vb  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
MOD = "modeminecraft"


def box(frm, to, key):
    return {"from": frm, "to": to, "faces": {f: {"uv": [0, 0, 1, 1], "texture": "#p"} for f in vb.FACES}, "__key": key}


def dragonslayer():
    """Épée colossale en fer brut et acier noirci : lame très large à arête centrale, garde minimaliste."""
    e = [box([6.5, -6, 7.0], [9.5, -4.2, 9.0], "iron_d"),            # pommeau
         box([7.0, -4.2, 7.2], [9.0, 4.0, 8.8], "leather")]           # poignée
    e += [box([6.9, y, 7.1], [9.1, y + 0.5, 8.9], "leather_d") for y in (-3.4, -1.6, 0.2, 2.0)]   # bandes du cuir
    e += [box([4.6, 4.0, 6.8], [11.4, 5.4, 9.2], "iron_d"),           # garde
          box([5.4, 5.4, 7.45], [10.6, 30.0, 8.55], "iron"),          # lame (fer brut)
          box([7.55, 5.4, 7.25], [8.45, 29.0, 8.75], "iron_d"),       # arête centrale (chanfrein)
          box([4.9, 6.2, 7.7], [5.4, 29.4, 8.3], "edge"),             # tranchants
          box([10.6, 6.2, 7.7], [11.1, 29.4, 8.3], "edge"),
          box([5.9, 30.0, 7.55], [10.1, 31.2, 8.45], "iron"),         # pointe en marches
          box([6.9, 31.2, 7.65], [9.1, 32.0, 8.35], "edge")]
    return e


def main():
    colors = {"iron": "#55585e", "iron_d": "#2e3034", "edge": "#9aa0a8", "leather": "#3a2416", "leather_d": "#24160c"}
    material = {"iron": "metal", "iron_d": "metal", "edge": "metal", "leather": "leather", "leather_d": "leather"}
    painter = vb.Painter(colors, lambda k: material[k], seed="dragonslayer")
    baked = vb.bake({"m": dragonslayer()}, skip_chamfer=("leather_d",))
    vb.paint(baked, painter).save(os.path.join(ASSETS, "textures/item/dragonslayer_3d.png"))
    tex = f"{MOD}:item/dragonslayer_3d"
    display = {
        "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 6.5, 1.5], "scale": [1, 1, 1]},
        "thirdperson_lefthand": {"rotation": [0, -90, 0], "translation": [0, 6.5, 1.5], "scale": [1, 1, 1]},
        "firstperson_righthand": {"rotation": [-20, 90, 25], "translation": [1, 2, -2], "scale": [0.6, 0.6, 0.6]},
        "firstperson_lefthand": {"rotation": [-20, -90, -25], "translation": [1, 2, -2], "scale": [0.6, 0.6, 0.6]},
        "gui": {"rotation": [0, 0, -45], "translation": [-1.5, -1.5, 0], "scale": [0.36, 0.36, 0.36]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    }
    model = {"textures": {"p": tex, "particle": tex}, "display": display, "elements": vb.elements(baked, "m")}
    with open(os.path.join(ASSETS, "models/item/dragonslayer.json"), "w", encoding="utf-8") as f:
        json.dump(model, f, separators=(",", ":"))

    # cartes d'accès (16x16, passées en 32x32 par enhance_icons.py)
    for name, c1, c2 in (("blue_access_card", "#2a5ad8", "#8ab0ff"), ("red_access_card", "#c02028", "#ff8a8a"),
                         ("black_access_card", "#18181c", "#6a6a74")):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.rectangle([1, 4, 14, 12], fill=c1)
        d.line([(1, 4), (14, 4)], fill=c2)
        d.rectangle([3, 7, 5, 9], fill="#d8b040")                   # puce
        d.line([(3, 8), (5, 8)], fill="#a07a20")
        d.line([(7, 7), (12, 7)], fill="#e8e8e8")                    # texte
        d.line([(7, 9), (11, 9)], fill="#c8c8c8")
        d.line([(1, 11), (14, 11)], fill="#101010")                  # bande magnétique
        img.save(os.path.join(ASSETS, f"textures/item/{name}.png"))
    # insigne du Faucon Blanc
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(8, 1), (14, 4), (13, 11), (8, 15), (3, 11), (2, 4)], fill="#9aa0a8", outline="#4a4e54")
    falcon = ["......#.......", ".....###......", "#...#####...#.", "##..#####..##.", ".#########.#..", "..########....",
              "....####......", "....#..#......"]
    for j, row in enumerate(falcon):
        for i, ch in enumerate(row):
            if ch == "#":
                img.putpixel((2 + i, 4 + j), (240, 240, 236, 255))
    img.save(os.path.join(ASSETS, "textures/item/falcon_insignia.png"))
    for name in ("blue_access_card", "red_access_card", "black_access_card", "falcon_insignia"):
        with open(os.path.join(ASSETS, f"models/item/{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}}, f, indent=2)
    # soins tactiques
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([3, 4, 12, 12], fill="#e8e2d4", outline="#b8b0a0")              # rouleau de bandage
    d.ellipse([6, 7, 9, 9], fill="#c8c0b0")
    d.rectangle([11, 9, 14, 11], fill="#e8e2d4")
    d.line([(4, 8), (11, 8)], fill="#c02028")
    img.save(os.path.join(ASSETS, "textures/item/bandage.png"))
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 4, 13, 13], fill="#c02028", outline="#801018")            # trousse
    d.rectangle([6, 2, 9, 4], outline="#401010")
    d.rectangle([7, 6, 8, 11], fill="#f0f0f0")
    d.rectangle([5, 8, 10, 9], fill="#f0f0f0")
    img.save(os.path.join(ASSETS, "textures/item/first_aid_kit.png"))
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.line([(3, 12), (11, 4)], fill="#d8dee4", width=3)                       # seringue
    d.line([(4, 11), (9, 6)], fill="#f0c020", width=1)
    d.line([(11, 4), (14, 1)], fill="#a0a8b0")
    d.line([(2, 11), (4, 13)], fill="#606870", width=2)
    img.save(os.path.join(ASSETS, "textures/item/adrenaline_syringe.png"))
    for name in ("bandage", "first_aid_kit", "adrenaline_syringe"):
        with open(os.path.join(ASSETS, f"models/item/{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}}, f, indent=2)
    # blessures : garrot, attelle, plâtre, défibrillateur, effet « fracture »
    def icon(name, fn):
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        fn(ImageDraw.Draw(im))
        im.save(os.path.join(ASSETS, f"textures/item/{name}.png"))
        with open(os.path.join(ASSETS, f"models/item/{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}}, f, indent=2)

    def tourniquet(d):
        d.rectangle([3, 6, 12, 10], fill="#2a2a2a", outline="#101010")        # sangle noire
        d.rectangle([5, 4, 10, 6], fill="#c02028", outline="#801018")         # boucle
        d.line([(7, 10), (7, 13)], fill="#e8e2d4", width=2)                    # molette
        d.line([(4, 8), (11, 8)], fill="#5a5a5a")
    icon("tourniquet", tourniquet)

    def splint(d):
        d.rectangle([6, 1, 9, 14], fill="#d8c8a0", outline="#8a7a58")         # tige rigide
        for y in (4, 8, 12):
            d.line([(4, y), (11, y)], fill="#e8e2d4", width=2)                  # bandes
        d.line([(7, 2), (7, 13)], fill="#b8a880")
    icon("splint", splint)

    def cast(d):
        d.rectangle([3, 3, 12, 13], fill="#f4f1ea", outline="#b8b0a0")         # manchon de plâtre
        d.rectangle([5, 1, 10, 3], fill="#e0dccf", outline="#b8b0a0")
        d.line([(4, 6), (11, 6)], fill="#d6d1c2")
        d.line([(4, 9), (11, 9)], fill="#d6d1c2")
        d.line([(6, 11), (9, 11)], fill="#3a78d0")                             # signature
    icon("plaster_cast", cast)

    def defib(d):
        d.rectangle([2, 4, 13, 13], fill="#e8e8e8", outline="#808890")         # boîtier
        d.rectangle([3, 5, 12, 8], fill="#1c3a2c")                              # écran
        d.line([(3, 7), (5, 7), (6, 5), (8, 8), (9, 6), (12, 6)], fill="#40e070")
        d.ellipse([4, 10, 6, 12], fill="#d02020")
        d.ellipse([9, 10, 11, 12], fill="#d02020")
        d.rectangle([6, 2, 9, 4], fill="#a8a8b0")
    icon("defibrillator", defib)

    eff = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    de = ImageDraw.Draw(eff)
    de.line([(9, 1), (7, 7), (11, 10), (8, 17)], fill="#e8e2d4", width=3)         # os brisé
    de.line([(9, 1), (7, 7), (11, 10), (8, 17)], fill="#b8b0a0", width=1)
    de.ellipse([5, 5, 13, 11], outline="#c02028")
    os.makedirs(os.path.join(ASSETS, "textures/mob_effect"), exist_ok=True)
    eff.save(os.path.join(ASSETS, "textures/mob_effect/fracture.png"))
    # œufs d'apparition
    factions = ["police", "swat", "militia", "rebel", "garrison", "soviet", "arctic", "mine_security", "cartel", "gang",
                "sheriff", "refinery", "civilian", "trader", "falcon", "falcon_chief", "falcon_squad", "railway"]
    for fct in factions:
        with open(os.path.join(ASSETS, f"models/item/bot_egg_{fct}.json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/template_spawn_egg"}, f, indent=2)
    print("Pourfendeuse de Dragons, cartes, insigne et œufs générés")


if __name__ == "__main__":
    main()

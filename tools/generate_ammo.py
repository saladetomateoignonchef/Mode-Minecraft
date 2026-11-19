#!/usr/bin/env python3
"""
Génère tout ce qui concerne les munitions spéciales (traçantes, perforantes...) :
- la marque de couleur de chaque type (affichée sur l'icône de la munition) ;
- les modèles des munitions (une variante par type, choisie par le jeu selon le type) ;
- les recettes de la table d'armement (onglet "Spéciales") : 16 munitions normales + ingrédient = 16 spéciales.

Utilisation (depuis la racine du projet, APRÈS tools/generate_textures.py) :
    python3 tools/generate_ammo.py
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
DATA = os.path.join(ROOT, "src/main/resources/data/modeminecraft")
MOD = "modeminecraft"

# Ordre = ordre de l'énumération AmmoType en Java (l'indice sert à choisir le modèle)
TYPES = ["standard", "ap", "he", "tracer", "incendiary", "hollow_point", "subsonic", "rubber", "marker",
         "venom", "cryo", "electric", "ruby", "slug", "flechette", "dragon_breath", "magnum"]
SHELL_TYPES = {"slug", "flechette", "dragon_breath", "magnum"}

# Couleur de la marque (identique à AmmoType.color en Java)
COLORS = {
    "ap": "#3c8cff", "he": "#ff3c1e", "tracer": "#ffa020", "incendiary": "#ff6a00", "hollow_point": "#c0c0c0",
    "subsonic": "#5a5a70", "rubber": "#303030", "marker": "#ffff40", "venom": "#55b335", "cryo": "#7cc4ea",
    "electric": "#b070ff", "ruby": "#c8223b", "slug": "#9aa3ad", "flechette": "#d0d0d0",
    "dragon_breath": "#ff4000", "magnum": "#8a1426",
}

# Ingrédient ajouté à 16 munitions normales pour obtenir 16 munitions spéciales
INGREDIENTS = {
    "ap": [("minecraft:iron_ingot", 1), ("minecraft:flint", 1)],
    "he": [("minecraft:tnt", 1)],
    "tracer": [("minecraft:glowstone_dust", 2)],
    "incendiary": [("minecraft:blaze_powder", 2)],
    "hollow_point": [("minecraft:iron_nugget", 4)],
    "subsonic": [("#minecraft:wool", 1)],
    "rubber": [("minecraft:slime_ball", 2)],
    "marker": [("minecraft:glow_ink_sac", 1)],
    "venom": [("minecraft:spider_eye", 2)],
    "cryo": [("minecraft:packed_ice", 1)],
    "electric": [("minecraft:redstone", 2), ("minecraft:copper_ingot", 1)],
    "ruby": [("modeminecraft:rubis", 1)],
    "slug": [("minecraft:iron_ingot", 1)],
    "flechette": [("minecraft:arrow", 2)],
    "dragon_breath": [("minecraft:fire_charge", 1)],
    "magnum": [("minecraft:gunpowder", 2)],
}

BULLETS = ["ammo_9mm", "ammo_45acp", "ammo_50ae", "ammo_357", "ammo_57", "ammo_46", "ammo_762x25", "ammo_545",
           "ammo_556", "ammo_762", "ammo_762x54r", "ammo_792", "ammo_338", "ammo_50bmg",
           "ammo_45colt", "ammo_500sw", "ammo_44mag", "ammo_408"]
SHELLS = ["shotgun_shell"]


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def badge(color):
    """Pastille de couleur avec contour, en bas à droite de l'icône (16x16, le reste transparent)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(11, 16):
        for x in range(11, 16):
            edge = x in (11, 15) or y in (11, 15)
            corner = (x, y) in ((11, 11), (15, 11), (11, 15), (15, 15))
            if corner:
                continue
            img.putpixel((x, y), (20, 20, 24, 255) if edge else hexc(color))
    img.putpixel((12, 12), tuple(min(255, c + 70) for c in hexc(color)[:3]) + (255,))
    return img


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def ing(name, count):
    d = {"tag": name[1:]} if name.startswith("#") else {"item": name}
    d["count"] = count
    return d


def main():
    os.makedirs(os.path.join(ASSETS, "textures/item/ammo_badge"), exist_ok=True)
    for t, color in COLORS.items():
        badge(color).save(os.path.join(ASSETS, "textures/item/ammo_badge", f"{t}.png"))

    recipes = 0
    for ammo in BULLETS + SHELLS:
        shell = ammo in SHELLS
        types = [t for t in TYPES[1:] if (t in SHELL_TYPES) == shell]
        overrides = []
        for t in types:
            variant = f"{MOD}:item/ammo/{ammo}_{t}"
            write(os.path.join(ASSETS, "models/item/ammo", f"{ammo}_{t}.json"), {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"{MOD}:item/{ammo}", "layer1": f"{MOD}:item/ammo_badge/{t}"}})
            overrides.append({"predicate": {f"{MOD}:ammo_type": round(TYPES.index(t) / 100, 2)}, "model": variant})
            # Recette : 16 munitions normales + ingrédient -> 16 munitions spéciales
            write(os.path.join(DATA, "recipe/armament/special", f"{ammo}_{t}.json"), {
                "type": f"{MOD}:armament", "category": "special_ammo",
                "ingredients": [ing(f"{MOD}:{ammo}", 16)] + [ing(*i) for i in INGREDIENTS[t]],
                "result": {"id": f"{MOD}:{ammo}", "count": 16, "components": {f"{MOD}:ammo_type": t}}})
            recipes += 1
        write(os.path.join(ASSETS, "models/item", f"{ammo}.json"), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{MOD}:item/{ammo}"},
            "overrides": overrides})
    print(f"{len(COLORS)} marques, {recipes} recettes de munitions spéciales")


if __name__ == "__main__":
    main()

"""Données de monde des décors de nature : une fonction configurée, une fonction placée et un modificateur de biome par thème.
Les thèmes et leurs objets sont dans NatureDecorFeature.java ; les objets eux-mêmes dans realism_decor_8.py."""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "data", "modeminecraft")
MOD = "modeminecraft"

# thème : (biomes, rareté = 1 chunk sur N, essais par chunk touché, étape)
THEMES = {
    "forest": (["#minecraft:is_forest", "#minecraft:is_taiga"], 9, 3, "vegetal_decoration"),
    "mountain": (["#minecraft:is_mountain", "minecraft:stony_shore", "minecraft:windswept_hills"], 8, 3, "vegetal_decoration"),
    "desert": (["minecraft:desert", "#minecraft:is_badlands", "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna"], 8, 3, "vegetal_decoration"),
    "swamp": (["minecraft:swamp", "minecraft:mangrove_swamp", "#minecraft:is_jungle"], 8, 3, "vegetal_decoration"),
    "river": (["#minecraft:is_river", "minecraft:swamp", "minecraft:forest", "minecraft:plains", "minecraft:birch_forest", "minecraft:meadow"], 12, 3, "vegetal_decoration"),
    "beach": (["#minecraft:is_beach", "minecraft:stony_shore"], 5, 4, "vegetal_decoration"),
    "snow": (["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:snowy_slopes", "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:ice_spikes",
              "minecraft:grove", "minecraft:snowy_beach", "minecraft:frozen_river"], 7, 3, "vegetal_decoration"),
    "sky": (["#minecraft:is_overworld"], 40, 1, "vegetal_decoration"),
    "fauna": (["#minecraft:is_overworld"], 18, 2, "vegetal_decoration"),
    "forage": (["#minecraft:is_forest", "#minecraft:is_taiga", "minecraft:plains", "minecraft:meadow"], 16, 2, "vegetal_decoration"),
    "farm": (["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:savanna"], 10, 3, "vegetal_decoration"),
    "camp": (["#minecraft:is_forest", "#minecraft:is_taiga", "#minecraft:is_mountain", "minecraft:plains", "minecraft:meadow"], 22, 2, "vegetal_decoration"),
    "ruins": (["#minecraft:is_overworld"], 35, 2, "vegetal_decoration"),
    "magic": (["#minecraft:is_forest", "minecraft:swamp", "minecraft:dark_forest", "minecraft:meadow", "#minecraft:is_taiga"], 28, 2, "vegetal_decoration"),
    "mine": (["#minecraft:is_overworld"], 3, 4, "underground_decoration"),
    "wreck": (["#minecraft:is_overworld"], 60, 1, "vegetal_decoration"),
}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


for theme, (biomes, rarity, tries, step) in THEMES.items():
    write(f"{ROOT}/worldgen/configured_feature/nature_{theme}.json", {"type": f"{MOD}:nature_decor", "config": {"theme": theme, "tries": tries}})
    placement = [{"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"}]
    if theme != "mine":
        placement.append({"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"})
    else:
        placement.append({"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -40}, "max_inclusive": {"absolute": 30}}})
    placement.append({"type": "minecraft:biome"})
    write(f"{ROOT}/worldgen/placed_feature/nature_{theme}.json", {"feature": f"{MOD}:nature_{theme}", "placement": placement})
    write(f"{ROOT}/neoforge/biome_modifier/add_nature_{theme}.json",
          {"type": "neoforge:add_features", "biomes": biomes if len(biomes) > 1 else biomes[0], "features": [f"{MOD}:nature_{theme}"], "step": step})
print(len(THEMES), "thèmes écrits")

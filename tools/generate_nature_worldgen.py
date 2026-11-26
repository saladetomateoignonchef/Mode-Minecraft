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
    # réalisme naturel, lot 1 : plantes arides ([26]) et fluviales ([27]), denses
    "arid_flora": (["minecraft:desert", "#minecraft:is_badlands"], 1, 5, "vegetal_decoration"),
    "shore_flora": (["#minecraft:is_river", "minecraft:swamp", "minecraft:mangrove_swamp"], 2, 5, "vegetal_decoration"),
    "pond_flora": (["#minecraft:is_river", "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:forest", "minecraft:plains"], 3, 5, "vegetal_decoration"),
    "wreck": (["#minecraft:is_overworld"], 60, 1, "vegetal_decoration"),
}


VANILLA_JAR = "/root/.gradle/caches/neoformruntime/intermediate_results/stripServer_8bb3d3745bd6f4ac9d34d27c8a1809cf14c56fdd_resourcesOutput.jar"
TAGS_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "vanilla_biome_tags.json")


def vanilla_tag(name, _seen=None):
    """Biomes (identifiants) d'un tag vanilla, tags imbriqués inclus : lus dans le jar du jeu, puis mis en cache dans tools/vanilla_biome_tags.json."""
    cache = json.load(open(TAGS_FILE, encoding="utf-8")) if os.path.exists(TAGS_FILE) else {}
    if name in cache:
        return cache[name]
    import zipfile
    out = []
    with zipfile.ZipFile(VANILLA_JAR) as z:
        data = json.loads(z.read(f"data/minecraft/tags/worldgen/biome/{name}.json"))
    for v in data["values"]:
        v = v["id"] if isinstance(v, dict) else v
        if v.startswith("#minecraft:"):
            out += vanilla_tag(v[len("#minecraft:"):])
        else:
            out.append(v)
    out = sorted(set(out))
    cache[name] = out
    json.dump(cache, open(TAGS_FILE, "w", encoding="utf-8"), indent=1)
    return out


def expand(biomes):
    """Liste de biomes sans doublon : les tags (sauf is_overworld, seul dans sa liste) sont remplacés par leur contenu. Une même fonction ne doit jamais
    figurer deux fois dans un biome (cycle d'ordre des fonctions : « Feature order cycle found », plantage de la génération du monde)."""
    if len(biomes) == 1 and biomes[0].startswith("#"):
        return biomes[0]                                                     # un seul tag : aucun doublon possible
    out = set()
    for b in biomes:
        if b.startswith("#minecraft:"):
            out.update(vanilla_tag(b[len("#minecraft:"):]))
        else:
            out.add(b)
    return sorted(out)


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
    # un seul modificateur par thème, avec une liste de biomes sans doublon
    value = expand(biomes)
    write(f"{ROOT}/neoforge/biome_modifier/add_nature_{theme}.json",
          {"type": "neoforge:add_features", "biomes": value, "features": [f"{MOD}:nature_{theme}"], "step": step})

# ---- sources chaudes, geysers, banquise (lot 1 du réalisme naturel) : fonction, placement et modificateur de biome
SPECIAL = {
    "hot_spring": ("hot_spring", {"geyser": False}, 30, ["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:snowy_slopes", "minecraft:grove", "minecraft:ice_spikes",
                                                       "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:stony_peaks", "minecraft:windswept_hills"], "vegetal_decoration"),
    "geyser": ("hot_spring", {"geyser": True}, 45, ["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:snowy_slopes", "minecraft:grove", "minecraft:ice_spikes",
                                                  "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:stony_peaks", "minecraft:windswept_hills"], "vegetal_decoration"),
    "ice_floe_frozen": ("ice_floe", {"max_radius": 11}, 2, ["minecraft:frozen_ocean", "minecraft:deep_frozen_ocean"], "top_layer_modification"),
    "ice_floe_cold": ("ice_floe", {"max_radius": 7}, 6, ["minecraft:cold_ocean", "minecraft:deep_cold_ocean"], "top_layer_modification"),
}
for name, (ftype, cfg, rarity, biomes, step) in SPECIAL.items():
    write(f"{ROOT}/worldgen/configured_feature/{name}.json", {"type": f"{MOD}:{ftype}", "config": cfg})
    write(f"{ROOT}/worldgen/placed_feature/{name}.json", {"feature": f"{MOD}:{name}", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    write(f"{ROOT}/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": sorted(set(biomes)), "features": [f"{MOD}:{name}"], "step": step})
print(len(THEMES), "thèmes écrits,", len(SPECIAL), "fonctions spéciales")

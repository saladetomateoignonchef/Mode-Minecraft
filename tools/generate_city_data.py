#!/usr/bin/env python3
"""
Données des villes : structures (une par famille de biome), ensemble de placement, listes de biomes,
tables de butin des coffres, portes blindées (textures, modèles, états, butin).
Utilisation : python3 tools/generate_city_data.py
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
DATA = os.path.join(ROOT, "src/main/resources/data/modeminecraft")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
MOD = "modeminecraft"

BIOMES = {
    "plains": ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow"],
    "desert": ["minecraft:desert", "minecraft:savanna", "minecraft:savanna_plateau"],
    "forest": ["minecraft:forest", "minecraft:birch_forest", "minecraft:dark_forest", "minecraft:old_growth_birch_forest",
               "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:flower_forest",
               "minecraft:cherry_grove"],
    "snow": ["minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga", "minecraft:grove", "minecraft:snowy_slopes"],
    "jungle": ["minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle", "minecraft:swamp", "minecraft:mangrove_swamp"],
    "mesa": ["minecraft:badlands", "minecraft:wooded_badlands", "minecraft:eroded_badlands"],
}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)


def item(name, weight=1, count=(1, 1), extra=None):
    e = {"type": "minecraft:item", "name": name if ":" in name else f"{MOD}:{name}", "weight": weight}
    funcs = []
    if count != (1, 1):
        funcs.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": count[0], "max": count[1]}})
    if extra:
        funcs += extra
    if funcs:
        e["functions"] = funcs
    return e


def table(pools):
    return {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": r[0], "max": r[1]}, "entries": e} for r, e in pools]}


FOOD = [item("minecraft:bread", 4, (1, 3)), item("minecraft:cooked_beef", 2, (1, 2)), item("minecraft:apple", 3, (1, 3)),
        item("minecraft:baked_potato", 3, (1, 4)), item("minecraft:honey_bottle", 1)]
AMMO = [item("ammo_9mm", 4, (8, 24)), item("ammo_556", 3, (8, 24)), item("ammo_762", 3, (6, 18)), item("shotgun_shell", 2, (4, 12)),
        item("ammo_45acp", 2, (6, 18))]
MED = [item("bandage", 4, (1, 3)), item("first_aid_kit", 2), item("adrenaline_syringe", 1)]
GUNS_LIGHT = [item("glock17", 3), item("beretta_m9", 3), item("mp5", 2), item("uzi", 2), item("remington_870", 2)]
GUNS_MIL = [item("m4", 3), item("ak47", 3), item("hk416", 2), item("m249", 1), item("scar_h", 2), item("dragunov", 1), item("ak74u", 2)]
GEAR = [item("pasgt_vest", 2), item("swat_vest", 1), item("mich_2000", 2), item("ops_core_fast", 1), item("bdu_woodland_pants", 2),
        item("m65_combat_boots", 2), item("steel_ballistic_plate", 2)]
ATT = [item("red_dot_sight", 3), item("holo_sight", 2), item("acog_scope", 1), item("vertical_grip", 3), item("silencer", 1),
       item("surefire_m600", 1), item("tactical_battery", 3, (1, 3))]

LOOT = {
    "city_dumpster": table([((1, 3), FOOD + [item("minecraft:paper", 3, (1, 4)), item("minecraft:string", 2), item("minecraft:bone", 2),
                                             item("ammo_9mm", 1, (2, 6)), item("bandage", 1)])]),
    "city_vending": table([((1, 2), [item("minecraft:honey_bottle", 3), item("minecraft:cookie", 4, (2, 6)), item("minecraft:sweet_berries", 2, (2, 5))])]),
    "city_office": table([((1, 3), [item("minecraft:paper", 5, (2, 8)), item("minecraft:book", 3), item("minecraft:writable_book", 1),
                                     item("blue_access_card", 1), item("minecraft:emerald", 1, (1, 2))] + FOOD[:2])]),
    "city_house": table([((2, 5), FOOD + MED[:1] + [item("minecraft:emerald", 1, (1, 3)), item("ammo_9mm", 1, (4, 10))])]),
    "city_shop": table([((2, 4), FOOD + MED[:2] + [item("tactical_battery", 2, (1, 2)), item("minecraft:emerald", 1, (1, 2))])]),
    "city_safe": table([((2, 4), [item("minecraft:emerald", 4, (2, 6)), item("minecraft:gold_ingot", 3, (1, 4)), item("minecraft:diamond", 1),
                                   item("blue_access_card", 2), item("red_access_card", 1), item("desert_eagle", 1)])]),
    "city_armory": table([((3, 6), AMMO + MED + GUNS_LIGHT + GEAR[:3] + ATT[:4])]),
    "city_vault": table([((3, 6), [item("minecraft:diamond", 3, (1, 3)), item("minecraft:gold_block", 2), item("minecraft:emerald", 4, (3, 9)),
                                    item("titanium_ingot", 3, (2, 5)), item("red_access_card", 2), item("black_access_card", 1),
                                    item("minecraft:netherite_scrap", 1)] + GUNS_MIL[:3])]),
    "military_crate": table([((3, 6), AMMO + MED + GUNS_MIL + GEAR + ATT + [item("frag_grenade", 3, (1, 3)), item("flashbang", 2, (1, 2))])]),
    "rebel_cache": table([((2, 5), AMMO[:3] + FOOD[:3] + [item("kar98k", 2), item("fn_fal", 1), item("molotov", 3, (1, 3)),
                                                         item("bandage", 3, (1, 3)), item("minecraft:iron_ingot", 3, (2, 6))])]),
    "lab": table([((2, 4), MED + [item("minecraft:glowstone_dust", 3, (2, 6)), item("minecraft:redstone", 3, (3, 9)),
                                   item("titanium_ingot", 2, (1, 3)), item("black_access_card", 1), item("emp_grenade", 2)])]),
    "mine_crate": table([((2, 4), [item("raw_titanium", 4, (2, 6)), item("titanium_ingot", 2, (1, 3)), item("minecraft:iron_ingot", 3, (2, 5)),
                                    item("minecraft:torch", 3, (4, 12)), item("minecraft:iron_pickaxe", 1), item("blue_access_card", 1)])]),
    "cartel_safe": table([((3, 6), [item("minecraft:gold_ingot", 4, (2, 8)), item("minecraft:emerald", 4, (3, 10)), item("minecraft:diamond", 2, (1, 2)),
                                     item("desert_eagle", 2), item("galil_ace", 1), item("red_access_card", 2)] + AMMO)]),
    "bank_vault": table([((3, 6), [item("minecraft:gold_ingot", 5, (3, 10)), item("minecraft:gold_block", 2), item("minecraft:emerald", 4, (4, 12)),
                                    item("minecraft:diamond", 1, (1, 2)), item("red_access_card", 1)])]),
    "airdrop": table([((4, 7), GUNS_MIL + AMMO + MED + ATT + GEAR + [item("red_access_card", 1), item("black_access_card", 1),
                                                                     item("barrett_m82", 1), item("rpg7", 1), item("frag_grenade", 2, (2, 4))])]),
}

DOORS = {"vault_door_blue": (1, (40, 90, 210)), "vault_door_red": (2, (200, 32, 40)), "vault_door_black": (3, (24, 24, 28))}


def door_textures(name, color):
    steel = [(58, 62, 68), (78, 84, 92), (98, 104, 112), (120, 126, 134)]
    for half in ("bottom", "top"):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                c = steel[1 + ((x * 3 + y * 7) % 5 == 0)]
                if x in (0, 15) or y in (0, 15):
                    c = steel[0]
                if (x in (3, 12) and 2 <= y <= 13) or (y in (2, 13) and 3 <= x <= 12):
                    c = steel[3]                                           # rivets / cadre
                if half == "top" and 5 <= y <= 7:
                    c = color                                              # bande de niveau
                if half == "bottom" and 9 <= y <= 11 and 9 <= x <= 12:
                    c = (20, 20, 22) if not (y == 10 and x == 10) else (60, 220, 90)   # lecteur de carte
                if half == "bottom" and 2 <= y <= 4 and 4 <= x <= 11:
                    c = color
                img.putpixel((x, y), c + (255,))
        img.save(os.path.join(ASSETS, f"textures/block/{name}_{half}.png"))
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    bottom = Image.open(os.path.join(ASSETS, f"textures/block/{name}_bottom.png")).resize((8, 8))
    top = Image.open(os.path.join(ASSETS, f"textures/block/{name}_top.png")).resize((8, 8))
    icon.paste(top, (4, 0))
    icon.paste(bottom, (4, 8))
    icon.save(os.path.join(ASSETS, f"textures/item/{name}.png"))


def door_assets(name):
    variants = {}
    base = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, y0 in base.items():
        for half, hname in (("lower", "bottom"), ("upper", "top")):
            for hinge in ("left", "right"):
                for opened in (False, True):
                    model = f"{MOD}:block/{name}_{hname}_{hinge}" + ("_open" if opened else "")
                    y = (y0 + (90 if hinge == "left" else 270) * opened) % 360
                    v = {"model": model}
                    if y:
                        v["y"] = y
                    variants[f"facing={facing},half={half},hinge={hinge},open={'true' if opened else 'false'}"] = v
    write(os.path.join(ASSETS, f"blockstates/{name}.json"), {"variants": variants})
    for hname in ("bottom", "top"):
        for hinge in ("left", "right"):
            for opened in ("", "_open"):
                write(os.path.join(ASSETS, f"models/block/{name}_{hname}_{hinge}{opened}.json"),
                      {"parent": f"minecraft:block/door_{hname}_{hinge}{opened}",
                       "textures": {"bottom": f"{MOD}:block/{name}_bottom", "top": f"{MOD}:block/{name}_top"}})
    write(os.path.join(ASSETS, f"models/item/{name}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
    write(os.path.join(DATA, f"loot_table/blocks/{name}.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"{MOD}:{name}", "conditions": [{"condition": "minecraft:block_state_property", "block": f"{MOD}:{name}",
                                                                           "properties": {"half": "lower"}}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def main():
    for kind, biomes in BIOMES.items():
        write(os.path.join(DATA, f"tags/worldgen/biome/has_city/{kind}.json"), {"values": biomes})
        write(os.path.join(DATA, f"worldgen/structure/city_{kind}.json"), {
            "type": f"{MOD}:city", "kind": kind, "biomes": f"#{MOD}:has_city/{kind}", "step": "top_layer_modification",
            "spawn_overrides": {}, "terrain_adaptation": "none"})
    write(os.path.join(DATA, "worldgen/structure_set/cities.json"), {
        "structures": [{"structure": f"{MOD}:city_{k}", "weight": 1} for k in BIOMES],
        # un centre de ville par région de 40 x 40 chunks ; mégapoles découpées en districts de 11 chunks (3 x 3 au plus)
        "placement": {"type": "modeminecraft:city_grid", "spacing": 40, "separation": 34, "salt": 77031953, "district": 11}})
    for name, t in LOOT.items():
        write(os.path.join(DATA, f"loot_table/chests/{name}.json"), t)
    for name, (lvl, color) in DOORS.items():
        door_textures(name, color)
        door_assets(name)
    # les portes blindées sont minables à la pioche en fer (très résistantes)
    tag = os.path.join(ROOT, "src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json")
    cur = json.load(open(tag)) if os.path.exists(tag) else {"values": []}
    for name in DOORS:
        if f"{MOD}:{name}" not in cur["values"]:
            cur["values"].append(f"{MOD}:{name}")
    write(tag, cur)
    tag2 = os.path.join(ROOT, "src/main/resources/data/minecraft/tags/block/needs_diamond_tool.json")
    cur = json.load(open(tag2)) if os.path.exists(tag2) else {"values": []}
    for name in DOORS:
        if f"{MOD}:{name}" not in cur["values"]:
            cur["values"].append(f"{MOD}:{name}")
    write(tag2, cur)
    print(f"{len(BIOMES)} structures de villes, {len(LOOT)} tables de butin, {len(DOORS)} portes blindées")


if __name__ == "__main__":
    main()

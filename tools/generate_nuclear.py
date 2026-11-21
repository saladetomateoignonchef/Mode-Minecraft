#!/usr/bin/env python3
"""
Phase 25 (B-29, bombes, nucléaire, radiations) : icônes, textures, armures portées, blocs, recettes, minerais,
butin. Utilisation : python3 tools/generate_nuclear.py
"""
import json
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generate_military as gm  # noqa: E402
from generate_military import Icon, diag, hexc, shade, write, ASSETS, DATA, MOD  # noqa: E402

STONE = [hexc(c) for c in ("#6e6e6e", "#7a7a7a", "#858585", "#8f8f8f", "#7f7f7f")]
DEEP = [hexc(c) for c in ("#3a3a40", "#45454c", "#4f4f56", "#35353b", "#404047")]


def ore_tex(name, base, ore, light, seed, streaks=False):
    r = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = r.random()
            if streaks:
                v = (v + (0.35 if (y + (x // 5)) % 4 == 0 else 0)) % 1.0
            img.putpixel((x, y), base[min(int(v * len(base)), len(base) - 1)])
    for cx, cy in [(3, 3), (10, 2), (12, 9), (4, 11), (8, 7), (13, 13)]:
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if r.random() < 0.85:
                img.putpixel((cx + dx, cy + dy), ore)
        img.putpixel((cx, cy), light)
    p = os.path.join(ASSETS, "textures/block", name + ".png")
    img.save(p)
    write(os.path.join(ASSETS, f"blockstates/{name}.json"), {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
    write(os.path.join(ASSETS, f"models/block/{name}.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{name}"}})
    write(os.path.join(ASSETS, f"models/item/{name}.json"), {"parent": f"{MOD}:block/{name}"})


def ores():
    ore_tex("lead_ore", STONE, hexc("#5c6378"), hexc("#8a93ab"), 31)
    ore_tex("deepslate_lead_ore", DEEP, hexc("#5c6378"), hexc("#8a93ab"), 32, True)
    ore_tex("uranium_ore", STONE, hexc("#4fb830"), hexc("#a8ff60"), 33)
    ore_tex("deepslate_uranium_ore", DEEP, hexc("#4fb830"), hexc("#a8ff60"), 34, True)
    for metal, raw in (("lead", "raw_lead"), ("uranium", "raw_uranium")):
        for block in (f"{metal}_ore", f"deepslate_{metal}_ore"):
            write(os.path.join(DATA, f"loot_table/blocks/{block}.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [
                {"type": "minecraft:alternatives", "children": [
                    {"type": "minecraft:item", "name": f"{MOD}:{block}", "conditions": [{"condition": "minecraft:match_tool", "predicate": {"predicates": {
                        "minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                    {"type": "minecraft:item", "name": f"{MOD}:{raw}", "functions": [
                        {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                        {"function": "minecraft:explosion_decay"}]}]}]}]})
            for kind, t in (("smelting", 200), ("blasting", 100)):
                for src in (raw, block):
                    write(os.path.join(DATA, f"recipe/{metal}_ingot_from_{kind}_{src}.json"), {"type": f"minecraft:{kind}", "category": "misc",
                          "ingredient": {"item": f"{MOD}:{src}"}, "result": {"id": f"{MOD}:{metal}_ingot"}, "experience": 0.8, "cookingtime": t})
        # filons : plomb fréquent (Y -32..64), uranium rare et profond (Y -64..-8)
        lead = metal == "lead"
        write(os.path.join(DATA, f"worldgen/configured_feature/{metal}_ore.json"), {"type": "minecraft:ore", "config": {
            "discard_chance_on_air_exposure": 0.0 if lead else 0.4, "size": 9 if lead else 4, "targets": [
                {"state": {"Name": f"{MOD}:{metal}_ore"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
                {"state": {"Name": f"{MOD}:deepslate_{metal}_ore"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}}]}})
        write(os.path.join(DATA, f"worldgen/placed_feature/{metal}_ore.json"), {"feature": f"{MOD}:{metal}_ore", "placement": [
            {"type": "minecraft:count", "count": 10 if lead else 3}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -32 if lead else -64},
                                                          "max_inclusive": {"absolute": 64 if lead else -8}}},
            {"type": "minecraft:biome"}]})
        write(os.path.join(DATA, f"neoforge/biome_modifier/add_{metal}_ore.json"), {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                                                                                     "features": f"{MOD}:{metal}_ore", "step": "underground_ores"})
    # outils requis
    for tag, extra in (("mineable/pickaxe", ["lead_ore", "deepslate_lead_ore", "uranium_ore", "deepslate_uranium_ore", "uranium_centrifuge", "kerosene_tank"]),
                       ("needs_iron_tool", ["uranium_ore", "deepslate_uranium_ore", "uranium_centrifuge"]),
                       ("needs_stone_tool", ["lead_ore", "deepslate_lead_ore"])):
        p = os.path.join(gm.RES, f"data/minecraft/tags/block/{tag}.json")
        d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
        for b in extra:
            if f"{MOD}:{b}" not in d["values"]:
                d["values"].append(f"{MOD}:{b}")
        write(p, d)


def centrifuge():
    def side(img, r):
        for y in range(16):
            for x in range(16):
                c = shade(hexc("#a8acb0"), 1 + r.uniform(-0.05, 0.05))
                if x in (0, 15) or y in (0, 15):
                    c = hexc("#5a5e62")
                if 5 <= x <= 10 and 4 <= y <= 11:
                    c = hexc("#2a2c30") if (x + y) % 2 else hexc("#3a3e42")
                if y == 13 and 3 <= x <= 12:
                    c = hexc("#e0c020") if x % 2 else hexc("#202020")         # bande de danger
                img.putpixel((x, y), c)
        img.putpixel((12, 3), hexc("#40ff40"))

    def top(img, r):
        for y in range(16):
            for x in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                c = hexc("#5a5e62") if d > 7 else hexc("#c0c4c8") if d > 5 else hexc("#3a3e42") if d > 2 else hexc("#7cff40")
                img.putpixel((x, y), shade(c, 1 + r.uniform(-0.04, 0.04)))
    gm.block_tex("uranium_centrifuge_side", side)
    gm.block_tex("uranium_centrifuge_top", top)
    write(os.path.join(ASSETS, "blockstates/uranium_centrifuge.json"), {"variants": {"": {"model": f"{MOD}:block/uranium_centrifuge"}}})
    write(os.path.join(ASSETS, "models/block/uranium_centrifuge.json"), {"parent": "minecraft:block/cube_column", "textures": {
        "side": f"{MOD}:block/uranium_centrifuge_side", "end": f"{MOD}:block/uranium_centrifuge_top"}})
    write(os.path.join(ASSETS, "models/item/uranium_centrifuge.json"), {"parent": f"{MOD}:block/uranium_centrifuge"})
    write(os.path.join(DATA, "loot_table/blocks/uranium_centrifuge.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"{MOD}:uranium_centrifuge"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def kerosene():
    def side(img, r):
        for y in range(16):
            for x in range(16):
                c = shade(hexc("#b8b4a0"), 1 + r.uniform(-0.05, 0.05))
                if y in (2, 13):
                    c = hexc("#6a6858")                                       # cerclages
                if 4 <= y <= 7 and 3 <= x <= 12:
                    c = hexc("#c83020") if y in (4, 7) else hexc("#f0f0e0")   # étiquette « KÉROSÈNE »
                if y in (5, 6) and 4 <= x <= 11 and (x + y) % 2 == 0:
                    c = hexc("#202020")
                img.putpixel((x, y), c)

    def top(img, r):
        for y in range(16):
            for x in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                c = hexc("#8a8878") if d > 6.5 else hexc("#b8b4a0") if d > 2 else hexc("#404040")
                img.putpixel((x, y), shade(c, 1 + r.uniform(-0.04, 0.04)))
    gm.block_tex("kerosene_tank_side", side)
    gm.block_tex("kerosene_tank_top", top)
    write(os.path.join(ASSETS, "blockstates/kerosene_tank.json"), {"variants": {"": {"model": f"{MOD}:block/kerosene_tank"}}})
    write(os.path.join(ASSETS, "models/block/kerosene_tank.json"), {"parent": "minecraft:block/cube_column", "textures": {
        "side": f"{MOD}:block/kerosene_tank_side", "end": f"{MOD}:block/kerosene_tank_top"}})
    write(os.path.join(ASSETS, "models/item/kerosene_tank.json"), {"parent": f"{MOD}:block/kerosene_tank"})
    write(os.path.join(DATA, "loot_table/blocks/kerosene_tank.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"{MOD}:kerosene_tank"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(os.path.join(DATA, "recipe/kerosene_tank.json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SJS", "SJS", "SJS"],
        "key": {"S": {"item": f"{MOD}:steel_ingot"}, "J": {"item": f"{MOD}:jerrycan"}}, "result": {"id": f"{MOD}:kerosene_tank", "count": 1}})


def icons():
    olive, red, black, yellow = hexc("#5a6438"), hexc("#b02818"), hexc("#202020"), hexc("#d8b020")
    # bombes : explosive (olive), napalm (grise, bande rouge), nucléaire « Fat Man »
    for name, body, band in (("bomb_500kg_he", olive, black), ("bomb_incendiary_napalm", hexc("#8a8e92"), red)):
        i = Icon(name)
        diag(i, 12, 4, [(0.18, black), (0.3, band), (0.85, body), (1, shade(body, 0.8))], start=(2, 13))
        i.px(1, 13, black), i.px(2, 14, black), i.px(1, 11, black), i.px(4, 14, black)
        i.save(name)
    i = Icon("nuke")
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 9) / 6.0, (y - 7) / 5.0
            if dx * dx + dy * dy <= 1:
                i.px(x, y, shade(yellow, 1 - 0.25 * max(0, dx + dy)))
    i.rect(1, 5, 3, 9, black, bevel=False)                                  # empennage en caisson
    i.rect(3, 6, 4, 8, black, bevel=False)
    i.rect(8, 2, 9, 12, black, bevel=False)                                 # bande noire
    i.px(12, 5, hexc("#ffffff"))
    i.save("strategic_nuclear_bomb")
    # matières
    for name, col in (("raw_lead", "#5c6378"), ("raw_uranium", "#4fb830")):
        i = Icon(name)
        i.rect(3, 4, 12, 12, hexc(col), noise=0.12)
        i.rect(5, 3, 10, 5, shade(hexc(col), 1.15), noise=0.1)
        i.save(name)
    for name, col in (("lead_ingot", "#6a7288"), ("uranium_ingot", "#5cd030")):
        i = Icon(name)
        i.rect(2, 6, 13, 11, hexc(col), noise=0.05)
        i.rect(3, 5, 12, 6, shade(hexc(col), 1.2), bevel=False)
        i.save(name)
    i = Icon("core")
    i.rect(4, 4, 11, 11, hexc("#2a2e34"), noise=0.05)
    i.rect(6, 6, 9, 9, hexc("#7cff40"), bevel=False)
    i.px(7, 7, hexc("#e0ffd0"))
    for p in ((3, 7), (12, 7), (7, 3), (7, 12)):
        i.px(*p, hexc("#5cd030"))
    i.save("enriched_uranium_core")
    i = Icon("detonator")
    i.rect(3, 5, 12, 12, hexc("#30343a"), noise=0.05)
    i.rect(5, 7, 10, 9, hexc("#20d040"), bevel=False)
    i.px(11, 6, red), i.px(4, 3, red), i.px(4, 4, hexc("#c0a040"))
    i.save("precision_detonator")
    i = Icon("canvas")
    i.rect(2, 3, 13, 12, hexc("#c8a020"), noise=0.08)
    for x in range(2, 14, 3):
        i.px(x, 7, shade(hexc("#c8a020"), 0.7))
    i.save("rubberized_canvas")
    i = Icon("filter")
    i.rect(4, 3, 11, 12, hexc("#4a5040"), noise=0.05)
    i.rect(5, 2, 10, 3, hexc("#20242a"), bevel=False)
    i.rect(6, 6, 9, 9, hexc("#202020"), bevel=False)
    i.save("nbc_filter")
    i = Icon("engine")
    i.rect(3, 3, 12, 12, hexc("#6a6e72"), noise=0.05)
    for k in range(5):
        i.px(7, 2 + k * 3 % 12, hexc("#3a3e42"))
    i.rect(6, 6, 9, 9, hexc("#2a2c30"), bevel=False)
    i.save("aircraft_engine")
    i = Icon("cockpit")
    i.rect(2, 6, 13, 13, hexc("#c4c8cc"), noise=0.05)
    i.rect(4, 3, 11, 7, hexc("#7fa8c0"), bevel=False)
    i.rect(7, 3, 8, 7, hexc("#8a9096"), bevel=False)
    i.save("pressurized_cockpit")
    i = Icon("geiger")
    i.rect(2, 5, 11, 13, yellow, noise=0.05)
    i.rect(3, 6, 8, 9, hexc("#e8e0c0"), bevel=False)                       # écran analogique
    i.px(6, 7, red), i.px(5, 8, red)
    i.rect(3, 11, 8, 12, black, bevel=False)                               # haut-parleur
    i.rect(12, 3, 14, 10, hexc("#808488"), bevel=False)                    # sonde
    i.px(11, 10, black), i.px(12, 11, black)
    i.save("geiger_counter")
    i = Icon("kit")
    i.rect(2, 4, 13, 13, hexc("#e0c020"), noise=0.06)
    i.rect(5, 6, 10, 9, hexc("#7fa8c0"), bevel=False)
    i.rect(6, 2, 9, 4, black, bevel=False)
    i.save("hazmat_kit")


SUITS = {
    # nom : (couleur principale, détail, visière, bottes)
    "hazmat_suit": ("#e0c020", "#a08010", "#9fc8e0", "#2a2a2a"),
    "nbc_suit": ("#5a6048", "#3a4030", "#2a3a40", "#1e1e1e"),
}


def suit_layers(name, spec):
    main, detail, visor, boots = (hexc(c) for c in spec)
    r = random.Random(name)
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def px(img, x, y, c):
        img.putpixel((x, y), shade(c, 1 + r.uniform(-0.05, 0.05)))
    # capuche intégrale (tête 0..32 x 0..16) avec large visière à l'avant (8..16, 8..16)
    for x in range(0, 32):
        for y in range(0, 16):
            if y < 8 and not (8 <= x < 24):
                continue
            px(l1, x, y, main)
    for x in range(9, 15):
        for y in range(9, 14):
            l1.putpixel((x, y), shade(visor, 1.1 if y == 9 else 1.0))
    if name == "nbc_suit":
        for x in (10, 13):
            l1.putpixel((x, 14), black := hexc("#202020"))                 # cartouches du masque
            l1.putpixel((x, 15), black)
    # torse et bras, tout couverts
    for x in range(16, 56):
        for y in range(16, 32):
            if y < 20 and not (20 <= x < 36 or 44 <= x < 52):
                continue
            px(l1, x, y, main)
    for y in range(20, 32):
        for x in (19, 28):
            l1.putpixel((x, y), shade(detail, 1.0))                       # coutures / sangles
    for x in range(20, 28):
        l1.putpixel((x, 29), detail)                                      # ceinture
    if name == "hazmat_suit":
        for y in range(21, 28):                                           # bouteille d'air dorsale
            for x in range(33, 35):
                l1.putpixel((x, y), hexc("#c0c4c8"))
    else:
        for y in range(21, 28):                                           # gilet renforcé plomb / titane
            for x in range(21, 27):
                l1.putpixel((x, y), shade(detail, 1.1))
    # bottes
    for x in range(0, 16):
        for y in range(20, 32):
            if y >= 25:
                px(l1, x, y, boots)
    # pantalon (calque 2)
    for x in range(0, 40):
        for y in range(16, 32):
            if x < 16 and y < 20 and not (4 <= x < 12):
                continue
            if x >= 16 and not (y >= 26):
                continue
            px(l2, x, y, main)
    d = os.path.join(ASSETS, "textures/models/armor")
    os.makedirs(d, exist_ok=True)
    l1.save(os.path.join(d, f"{name}_layer_1.png"))
    l2.save(os.path.join(d, f"{name}_layer_2.png"))
    gm.outfit_icons(name, (spec[0], spec[0], spec[1], spec[0], spec[3], spec[2], "plain"))


def plain():
    img = Image.new("RGBA", (16, 16))
    r = random.Random(5)
    for y in range(16):
        for x in range(16):
            v = int(235 + r.uniform(-14, 14))
            img.putpixel((x, y), (v, v, v, 255))
    img.save(os.path.join(ASSETS, "textures/entity/plain.png"))


def recipes():
    R = os.path.join(DATA, "recipe")

    def shaped(name, pattern, key, result, count=1):
        write(os.path.join(R, name + ".json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                                 "key": {k: {"item": v} for k, v in key.items()}, "result": {"id": result, "count": count}})

    def shapeless(name, items, result, count=1):
        write(os.path.join(R, name + ".json"), {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                 "ingredients": [{"item": i} for i in items], "result": {"id": result, "count": count}})
    m = MOD + ":"
    shaped("rubberized_canvas", ["WHW"], {"W": "minecraft:white_wool", "H": "minecraft:honeycomb"}, m + "rubberized_canvas", 3)
    shaped("hazmat_kit", ["CCC", "LGL", "CCC"], {"C": m + "rubberized_canvas", "L": m + "lead_ingot", "G": "minecraft:glass"}, m + "hazmat_kit")
    shaped("nbc_filter", [" P ", "CIC"], {"P": "minecraft:paper", "C": "minecraft:charcoal", "I": "minecraft:iron_ingot"}, m + "nbc_filter", 2)
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        plate = m + ("titanium_plate" if piece in ("helmet", "chestplate") else "steel_plate")
        shapeless(f"nbc_suit_{piece}", [m + f"hazmat_suit_{piece}", plate, plate, m + "lead_ingot", m + "nbc_filter"], m + f"nbc_suit_{piece}")
    shaped("geiger_counter", ["SGS", "RCR", " B "], {"S": m + "steel_ingot", "G": "minecraft:glass_pane", "R": "minecraft:redstone",
                                                     "C": "minecraft:copper_ingot", "B": m + "tactical_battery"}, m + "geiger_counter")
    shaped("precision_detonator", ["QRQ", "GCG", "QRQ"], {"Q": "minecraft:quartz", "R": "minecraft:redstone", "G": "minecraft:gold_ingot",
                                                          "C": "minecraft:comparator"}, m + "precision_detonator")
    shaped("uranium_centrifuge", ["LPL", "SMS", "LRL"], {"L": m + "lead_ingot", "P": "minecraft:piston", "S": m + "steel_plate",
                                                         "M": "minecraft:hopper", "R": "minecraft:redstone_block"}, m + "uranium_centrifuge")
    # établi d'artillerie : bombes et bombe nucléaire (le plus cher du mod)
    A = os.path.join(R, "artillery")
    for name, ings, count in (
            ("bomb_500kg_he", [("modeminecraft:steel_plate", 4), ("minecraft:tnt", 8), ("minecraft:iron_ingot", 2)], 1),
            ("bomb_incendiary_napalm", [("modeminecraft:steel_plate", 2), ("minecraft:tnt", 3), ("minecraft:blaze_powder", 6),
                                        ("minecraft:fire_charge", 4)], 1),
            ("strategic_nuclear_bomb", [("modeminecraft:enriched_uranium_core", 4), ("minecraft:netherite_ingot", 4),
                                        ("modeminecraft:titanium_plate", 8), ("modeminecraft:precision_detonator", 2), ("minecraft:diamond_block", 1)], 1)):
        write(os.path.join(A, name + ".json"), {"type": f"{MOD}:armament", "category": "artillery",
                                                "ingredients": [{"item": i, "count": c} for i, c in ings], "result": {"id": f"{MOD}:{name}", "count": count}})
    # établi d'assemblage : pièces d'avion et B-29
    P = os.path.join(R, "assembly")
    for name, cat, ings in (
            ("aircraft_engine", "vehicle_parts", [("modeminecraft:steel_plate", 6), ("modeminecraft:titanium_ingot", 4), ("minecraft:piston", 4),
                                                  ("minecraft:blaze_rod", 2)]),
            ("pressurized_cockpit", "vehicle_parts", [("modeminecraft:titanium_plate", 4), ("minecraft:glass", 8), ("modeminecraft:fire_control_module", 1),
                                                      ("minecraft:redstone_block", 1)]),
            ("b29_superfortress", "vehicles", [("modeminecraft:aircraft_engine", 4), ("modeminecraft:pressurized_cockpit", 1),
                                              ("modeminecraft:titanium_plate", 24), ("modeminecraft:steel_plate", 16),
                                              ("modeminecraft:fire_control_module", 2), ("modeminecraft:wheel_set", 1)])):
        write(os.path.join(P, name + ".json"), {"type": f"{MOD}:armament", "category": cat,
                                                "ingredients": [{"item": i, "count": c} for i, c in ings], "result": {"id": f"{MOD}:{name}", "count": 1}})


def loot():
    def add(table, item, weight, lo, hi):
        p = os.path.join(DATA, f"loot_table/chests/{table}.json")
        d = json.load(open(p))
        names = [e.get("name") for pool in d["pools"] for e in pool["entries"]]
        if f"{MOD}:{item}" in names:
            return
        e = {"type": "minecraft:item", "name": f"{MOD}:{item}", "weight": weight}
        if hi > 1:
            e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        d["pools"][0]["entries"].append(e)
        write(p, d)
    # cœurs d'uranium : chambres fortes de niveau 3 (bunker, URSS), tenues et Geiger dans les bases
    add("boss_vault_forest_bunker", "enriched_uranium_core", 6, 1, 2)
    add("boss_vault_soviet_hq", "enriched_uranium_core", 6, 1, 2)
    add("boss_vault_forest_bunker", "nbc_suit_helmet", 4, 1, 1)
    add("boss_vault_soviet_hq", "nbc_suit_chestplate", 4, 1, 1)
    add("military_base", "geiger_counter", 3, 1, 1)
    add("military_base", "nbc_filter", 6, 1, 4)
    add("military_base", "bomb_500kg_he", 2, 1, 1)
    add("military_base", "hazmat_kit", 2, 1, 1)
    add("military_crate", "nbc_filter", 5, 1, 3)
    add("military_crate", "lead_ingot", 6, 2, 6)


if __name__ == "__main__":
    ores()
    centrifuge()
    kerosene()
    icons()
    for n, sp in SUITS.items():
        suit_layers(n, sp)
    plain()
    recipes()
    loot()
    print("ok")

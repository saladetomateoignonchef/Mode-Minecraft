#!/usr/bin/env python3
"""
Aviation militaire (phase 26) : icônes des missiles d'avion, de la bombe guidée et des pièces (réacteur, avionique,
panneaux absorbants), caisse de munitions d'aviation, recettes (établi d'artillerie / d'assemblage), butin.
Utilisation : python3 tools/generate_aviation.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generate_military as gm  # noqa: E402
from generate_military import Icon, diag, hexc, shade, write, ASSETS, DATA, MOD  # noqa: E402


def icons():
    white, black, red, yellow, grey = hexc("#e4e6e0"), hexc("#202020"), hexc("#b02818"), hexc("#d8b020"), hexc("#7a7e84")
    olive = hexc("#5a6438")
    # missiles : corps clair, bande de couleur (jaune explosif, bleu exercice…), ailettes noires
    for name, body, band, length in (("aam_ir_missile", white, yellow, 13), ("aam_radar_missile", white, hexc("#3060c0"), 14),
                                     ("agm_missile", olive, yellow, 12), ("arm_missile", white, hexc("#e07020"), 14),
                                     ("cruise_missile", grey, red, 14)):
        i = Icon(name)
        diag(i, length, 3, [(0.12, black), (0.22, shade(body, 0.8)), (0.7, body), (0.78, band), (0.92, body), (1, shade(body, 0.6))], start=(1, 14))
        # ailettes
        i.px(1, 12, black), i.px(3, 14, black), i.px(0, 13, black), i.px(2, 15, black)
        if name == "cruise_missile":
            i.px(6, 7, black), i.px(8, 11, black), i.px(5, 6, black), i.px(9, 12, black)      # ailes déployées
        i.save(name)
    i = Icon("guided_bomb")
    diag(i, 12, 4, [(0.15, black), (0.25, shade(olive, 0.8)), (0.8, olive), (0.88, yellow), (1, hexc("#c0a040"))], start=(2, 13))
    i.px(1, 13, black), i.px(2, 14, black), i.px(1, 11, black), i.px(4, 14, black)
    i.px(12, 2, hexc("#80d0ff"))                                                     # tête laser
    i.save("guided_bomb")
    i = Icon("jet_engine")
    i.rect(1, 5, 14, 11, hexc("#8a8e94"), noise=0.05)
    i.rect(0, 6, 1, 10, hexc("#2a2c30"), bevel=False)
    i.rect(13, 4, 15, 12, hexc("#5a5e64"), bevel=False)                               # tuyère
    i.rect(14, 6, 15, 10, black, bevel=False)
    for x in (4, 7, 10):
        i.rect(x, 5, x + 1, 11, hexc("#6a6e74"), bevel=False)
    i.save("jet_engine")
    i = Icon("avionics_suite")
    i.rect(2, 3, 13, 13, hexc("#2a2e34"), noise=0.04)
    i.rect(4, 5, 9, 9, hexc("#30d060"), bevel=False)                                  # écran
    i.px(5, 7, hexc("#c0ffd0")), i.px(7, 6, hexc("#c0ffd0"))
    for y in (5, 7, 9, 11):
        i.px(11, y, yellow)
    i.rect(4, 10, 9, 11, hexc("#c0a040"), bevel=False)
    i.save("avionics_suite")
    i = Icon("radar_absorbent_panel")
    i.rect(2, 2, 13, 13, hexc("#1c1e22"), noise=0.05)
    for k in range(2, 13, 3):
        for x in range(2, 14):
            y = k + (x // 3) % 2
            if 2 <= y <= 13:
                i.px(x, y, hexc("#2c2f34"))
    i.save("radar_absorbent_panel")


def crate():
    """Caisse de munitions d'aviation : caisse vert olive, pochoirs jaunes, cerclages."""
    def side(img, r):
        for y in range(16):
            for x in range(16):
                c = shade(hexc("#4f5a32"), 1 + r.uniform(-0.06, 0.06))
                if x in (0, 15) or y in (0, 15):
                    c = hexc("#2e3420")
                if y in (3, 12):
                    c = hexc("#6a6e72")                                                # cerclages métalliques
                if 5 <= y <= 9 and 3 <= x <= 12 and (y in (5, 9) or x in (3, 12)):
                    c = hexc("#d8b020")
                if y == 7 and 5 <= x <= 10 and x % 2 == 1:
                    c = hexc("#d8b020")                                                # pochoir
                img.putpixel((x, y), c)

    def top(img, r):
        for y in range(16):
            for x in range(16):
                c = shade(hexc("#55603a"), 1 + r.uniform(-0.05, 0.05))
                if x in (0, 15) or y in (0, 15) or x == 7 or x == 8:
                    c = hexc("#2e3420")
                if (x in (3, 12)) and 2 <= y <= 13:
                    c = hexc("#6a6e72")
                img.putpixel((x, y), c)
    gm.block_tex("aviation_ammo_crate_side", side)
    gm.block_tex("aviation_ammo_crate_top", top)
    write(os.path.join(ASSETS, "blockstates/aviation_ammo_crate.json"), {"variants": {"": {"model": f"{MOD}:block/aviation_ammo_crate"}}})
    write(os.path.join(ASSETS, "models/block/aviation_ammo_crate.json"), {"parent": "minecraft:block/cube_column", "textures": {
        "side": f"{MOD}:block/aviation_ammo_crate_side", "end": f"{MOD}:block/aviation_ammo_crate_top"}})
    write(os.path.join(ASSETS, "models/item/aviation_ammo_crate.json"), {"parent": f"{MOD}:block/aviation_ammo_crate"})
    write(os.path.join(DATA, "loot_table/blocks/aviation_ammo_crate.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"{MOD}:aviation_ammo_crate"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    p = os.path.join(gm.RES, "data/minecraft/tags/block/mineable/pickaxe.json")
    d = json.load(open(p))
    if f"{MOD}:aviation_ammo_crate" not in d["values"]:
        d["values"].append(f"{MOD}:aviation_ammo_crate")
    write(p, d)


PLANES = {
    # nom : (catégorie, ingrédients)
    "il2_sturmovik": [("aircraft_engine", 1), ("titanium_plate", 8), ("steel_plate", 10), ("minecraft:glass", 4), ("wheel_set", 1)],
    "light_civil_plane": [("aircraft_engine", 1), ("steel_plate", 6), ("minecraft:iron_ingot", 6), ("minecraft:white_wool", 8), ("minecraft:glass", 6), ("wheel_set", 1)],
    "la7": [("aircraft_engine", 1), ("steel_plate", 10), ("minecraft:iron_ingot", 8), ("minecraft:glass", 4), ("wheel_set", 1)],
    "p51_mustang": [("aircraft_engine", 1), ("steel_plate", 12), ("minecraft:iron_ingot", 8), ("minecraft:glass", 4), ("wheel_set", 1)],
    "mig15": [("jet_engine", 1), ("steel_plate", 12), ("titanium_plate", 4), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "f86_sabre": [("jet_engine", 1), ("steel_plate", 12), ("titanium_plate", 4), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "mig21": [("jet_engine", 1), ("titanium_plate", 10), ("steel_plate", 8), ("avionics_suite", 1), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "mig25": [("jet_engine", 2), ("steel_plate", 20), ("titanium_plate", 8), ("avionics_suite", 2), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "su25": [("jet_engine", 2), ("titanium_plate", 20), ("steel_plate", 8), ("avionics_suite", 1), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "a10_thunderbolt": [("jet_engine", 2), ("titanium_plate", 22), ("autocannon_mount", 1), ("avionics_suite", 1), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "f4_phantom": [("jet_engine", 2), ("titanium_plate", 14), ("steel_plate", 10), ("avionics_suite", 2), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "mig29": [("jet_engine", 2), ("titanium_plate", 16), ("avionics_suite", 2), ("fire_control_module", 1), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "su34": [("jet_engine", 2), ("titanium_plate", 22), ("avionics_suite", 3), ("fire_control_module", 1), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "f15e_strike_eagle": [("jet_engine", 2), ("titanium_plate", 20), ("avionics_suite", 3), ("fire_control_module", 1), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "f16_falcon": [("jet_engine", 1), ("titanium_plate", 14), ("avionics_suite", 2), ("fire_control_module", 1), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "su57": [("jet_engine", 2), ("titanium_plate", 20), ("radar_absorbent_panel", 12), ("avionics_suite", 3), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "f22_raptor": [("jet_engine", 2), ("titanium_plate", 20), ("radar_absorbent_panel", 16), ("avionics_suite", 3), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "f117_nighthawk": [("jet_engine", 2), ("titanium_plate", 16), ("radar_absorbent_panel", 24), ("avionics_suite", 2), ("pressurized_cockpit", 1), ("wheel_set", 1)],
    "tu95_bear": [("aircraft_engine", 4), ("titanium_plate", 24), ("steel_plate", 24), ("avionics_suite", 2), ("pressurized_cockpit", 1), ("wheel_set", 2)],
    "c130_hercules": [("aircraft_engine", 4), ("steel_plate", 28), ("titanium_plate", 10), ("avionics_suite", 1), ("pressurized_cockpit", 1), ("wheel_set", 2)],
    "b2_spirit": [("jet_engine", 4), ("titanium_plate", 32), ("radar_absorbent_panel", 40), ("avionics_suite", 4), ("pressurized_cockpit", 1), ("wheel_set", 2)],
}


def item_id(n):
    return n if ":" in n else f"{MOD}:{n}"


def recipes():
    R = os.path.join(DATA, "recipe")

    def shaped(name, pattern, key, result, count=1):
        write(os.path.join(R, name + ".json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                                 "key": {k: {"item": item_id(v)} for k, v in key.items()}, "result": {"id": item_id(result), "count": count}})

    def armament(folder, name, cat, ings, count=1):
        write(os.path.join(R, folder, name + ".json"), {"type": f"{MOD}:armament", "category": cat,
                                                        "ingredients": [{"item": item_id(i), "count": c} for i, c in ings],
                                                        "result": {"id": item_id(name), "count": count}})
    shaped("avionics_suite", ["QRQ", "GCG", "RPR"], {"Q": "minecraft:quartz", "R": "minecraft:redstone", "G": "minecraft:gold_ingot",
                                                    "C": "minecraft:comparator", "P": "minecraft:glass_pane"}, "avionics_suite")
    shaped("radar_absorbent_panel", ["DTD", "TCT", "DTD"], {"D": "minecraft:black_dye", "T": "titanium_plate", "C": "minecraft:coal_block"},
           "radar_absorbent_panel", 4)
    shaped("aviation_ammo_crate", ["SAS", "ATA", "SAS"], {"S": "steel_plate", "A": "ammo_box_50", "T": "minecraft:tnt"}, "aviation_ammo_crate")
    armament("assembly", "jet_engine", "vehicle_parts", [("titanium_ingot", 6), ("steel_plate", 4), ("minecraft:blaze_rod", 4),
                                                          ("minecraft:fire_charge", 2), ("minecraft:piston", 2)])
    for name, ings in PLANES.items():
        armament("assembly", name, "vehicles", ings)
    # munitions d'aviation (établi d'artillerie)
    for name, ings, count in (
            ("aam_ir_missile", [("steel_plate", 2), ("minecraft:tnt", 2), ("minecraft:blaze_powder", 2), ("minecraft:redstone", 4)], 2),
            ("aam_radar_missile", [("steel_plate", 3), ("minecraft:tnt", 3), ("minecraft:quartz", 4), ("minecraft:redstone", 4)], 2),
            ("agm_missile", [("steel_plate", 3), ("minecraft:tnt", 4), ("minecraft:redstone", 4), ("minecraft:glowstone_dust", 2)], 2),
            ("arm_missile", [("steel_plate", 3), ("minecraft:tnt", 3), ("minecraft:quartz", 6), ("minecraft:comparator", 1)], 2),
            ("cruise_missile", [("steel_plate", 6), ("minecraft:tnt", 10), ("minecraft:blaze_rod", 4), ("avionics_suite", 1)], 1),
            ("guided_bomb", [("steel_plate", 4), ("minecraft:tnt", 10), ("minecraft:redstone", 4), ("minecraft:comparator", 1)], 1)):
        armament("artillery", name, "artillery", ings, count)


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
    add("military_base", "aam_ir_missile", 3, 1, 2)
    add("military_base", "agm_missile", 2, 1, 2)
    add("military_base", "avionics_suite", 3, 1, 2)
    add("military_base", "jet_engine", 1, 1, 1)
    add("military_crate", "aam_ir_missile", 2, 1, 2)
    add("boss_vault_forest_bunker", "radar_absorbent_panel", 4, 4, 12)
    add("boss_vault_soviet_hq", "radar_absorbent_panel", 4, 4, 12)


if __name__ == "__main__":
    icons()
    crate()
    recipes()
    loot()
    print("ok")

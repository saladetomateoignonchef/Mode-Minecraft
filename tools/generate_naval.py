#!/usr/bin/env python3
"""
Guerre navale (phase 27) : icônes des munitions navales (obus de 130 mm, bande CIWS, missile antinavire, missile
hypersonique, missile antiaérien naval, torpille, roquette anti-sous-marine) et des pièces (section de coque,
sonar, réacteur nucléaire naval), recettes (établi d'assemblage / d'artillerie), butin.
Utilisation : python3 tools/generate_naval.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generate_aviation as ga  # noqa: E402
from generate_military import Icon, diag, hexc, shade, write, DATA, MOD  # noqa: E402

AMMO = ["naval_shell", "ciws_belt", "ashm_missile", "hypersonic_missile", "naval_sam_missile", "torpedo", "asw_rocket"]
PARTS = ["ship_hull_section", "sonar_array", "naval_reactor"]


def icons():
    white, black, red, yellow, grey = hexc("#e4e6e0"), hexc("#202020"), hexc("#b02818"), hexc("#d8b020"), hexc("#7a7e84")
    navy, brass, steel = hexc("#3a4a5e"), hexc("#c8a040"), hexc("#9aa0a8")
    i = Icon("naval_shell")
    diag(i, 12, 4, [(0.2, brass), (0.7, shade(brass, 0.9)), (0.78, red), (1, grey)], start=(2, 13))
    i.save("naval_shell")
    i = Icon("ciws_belt")
    for k in range(5):
        x = 2 + k * 2
        i.rect(x, 4 + k, x + 1, 11 + k // 2, brass, bevel=False)
        i.px(x, 3 + k, red)
    i.rect(1, 12, 13, 13, hexc("#3a3a3a"), bevel=False)
    i.save("ciws_belt")
    for name, body, band, length, w in (("ashm_missile", grey, red, 14, 3), ("hypersonic_missile", hexc("#2c2e34"), hexc("#ff7020"), 14, 3),
                                        ("naval_sam_missile", white, hexc("#3060c0"), 14, 3), ("asw_rocket", hexc("#4a5a40"), yellow, 10, 3)):
        i = Icon(name)
        diag(i, length, w, [(0.12, black), (0.22, shade(body, 0.8)), (0.7, body), (0.78, band), (0.92, body), (1, shade(body, 0.6))], start=(1, 14))
        i.px(1, 12, black), i.px(3, 14, black), i.px(0, 13, black), i.px(2, 15, black)
        if name == "hypersonic_missile":
            i.px(13, 2, hexc("#ffd080")), i.px(14, 1, hexc("#fff0c0"))             # nez chauffé à blanc
        i.save(name)
    i = Icon("torpedo")
    diag(i, 14, 4, [(0.1, black), (0.18, hexc("#c0a030")), (0.85, hexc("#3c4048")), (1, hexc("#2a2c30"))], start=(1, 14))
    i.px(0, 15, black), i.px(1, 13, black), i.px(3, 15, black)
    i.save("torpedo")
    i = Icon("ship_hull_section")
    i.rect(1, 4, 14, 12, navy, noise=0.05)
    i.rect(1, 10, 14, 12, hexc("#8a2a20"), bevel=False)                            # carène sous la flottaison
    for x in (3, 7, 11):
        i.rect(x, 5, x, 9, shade(navy, 0.8), bevel=False)
    i.save("ship_hull_section")
    i = Icon("sonar_array")
    i.rect(3, 2, 12, 13, hexc("#2a2e34"), noise=0.04)
    for y in range(3, 13, 2):
        for x in range(4, 12, 2):
            i.px(x, y, hexc("#40c0a0"))
    i.save("sonar_array")
    i = Icon("naval_reactor")
    i.rect(3, 2, 12, 13, steel, noise=0.05)
    i.rect(5, 5, 10, 10, hexc("#203040"), bevel=False)
    i.rect(6, 6, 9, 9, hexc("#40b0ff"), bevel=False)                               # cœur (Tcherenkov)
    i.px(7, 7, hexc("#d0f0ff"))
    for y in (3, 12):
        i.rect(4, y, 11, y, yellow, bevel=False)
    i.save("naval_reactor")


SHIPS = {
    "kirov_cruiser": [("ship_hull_section", 24), ("naval_reactor", 2), ("gas_turbine", 2), ("cannon_barrel", 2), ("launcher_module", 6),
                      ("fire_control_module", 6), ("sonar_array", 2), ("avionics_suite", 3)],
    "admiral_gorshkov": [("ship_hull_section", 12), ("gas_turbine", 2), ("cannon_barrel", 1), ("launcher_module", 3), ("fire_control_module", 4),
                         ("sonar_array", 1), ("avionics_suite", 2)],
    "slava_cruiser": [("ship_hull_section", 18), ("gas_turbine", 4), ("cannon_barrel", 2), ("launcher_module", 4), ("fire_control_module", 5),
                      ("sonar_array", 1), ("avionics_suite", 2)],
    "udaloy_destroyer": [("ship_hull_section", 14), ("gas_turbine", 4), ("cannon_barrel", 2), ("launcher_module", 3), ("fire_control_module", 4),
                         ("sonar_array", 2), ("avionics_suite", 2)],
    "buyan_corvette": [("ship_hull_section", 6), ("vehicle_engine", 2), ("cannon_barrel", 1), ("launcher_module", 1), ("fire_control_module", 2),
                       ("avionics_suite", 1)],
    "arleigh_burke": [("ship_hull_section", 14), ("gas_turbine", 4), ("cannon_barrel", 1), ("launcher_module", 4), ("fire_control_module", 5),
                      ("sonar_array", 2), ("avionics_suite", 3)],
    "ticonderoga_cruiser": [("ship_hull_section", 18), ("gas_turbine", 4), ("cannon_barrel", 2), ("launcher_module", 5), ("fire_control_module", 6),
                            ("sonar_array", 1), ("avionics_suite", 3)],
    "zumwalt_destroyer": [("ship_hull_section", 18), ("gas_turbine", 2), ("cannon_barrel", 2), ("launcher_module", 5), ("radar_absorbent_panel", 32),
                          ("fire_control_module", 5), ("avionics_suite", 4)],
    "ohio_submarine": [("ship_hull_section", 20), ("naval_reactor", 1), ("launcher_module", 8), ("sonar_array", 4), ("fire_control_module", 4),
                       ("avionics_suite", 3)],
    "virginia_submarine": [("ship_hull_section", 14), ("naval_reactor", 1), ("launcher_module", 2), ("sonar_array", 4), ("fire_control_module", 3),
                           ("avionics_suite", 2)],
    "mercenary_rhib": [("rubberized_canvas", 8), ("vehicle_engine", 2), ("steel_plate", 4), ("minecraft:oak_planks", 8)],
    "mercenary_interceptor_boat": [("ship_hull_section", 2), ("vehicle_engine", 2), ("autocannon_mount", 1), ("launcher_module", 1), ("steel_plate", 8)],
    "mercenary_explosive_usv": [("ship_hull_section", 1), ("vehicle_engine", 1), ("c4", 6), ("minecraft:tnt", 8), ("avionics_suite", 1)],
}


def recipes():
    R = os.path.join(DATA, "recipe")

    def armament(folder, name, cat, ings, count=1):
        write(os.path.join(R, folder, name + ".json"), {"type": f"{MOD}:armament", "category": cat,
                                                        "ingredients": [{"item": ga.item_id(i), "count": c} for i, c in ings],
                                                        "result": {"id": ga.item_id(name), "count": count}})
    armament("assembly", "ship_hull_section", "vehicle_parts", [("steel_plate", 12), ("titanium_plate", 2), ("minecraft:iron_block", 2),
                                                               ("minecraft:red_dye", 2)], 2)
    armament("assembly", "sonar_array", "vehicle_parts", [("steel_plate", 2), ("minecraft:quartz", 8), ("minecraft:redstone", 8),
                                                         ("minecraft:note_block", 4), ("minecraft:prismarine_shard", 6)])
    armament("assembly", "naval_reactor", "vehicle_parts", [("enriched_uranium_core", 1), ("lead_ingot", 16), ("steel_plate", 12),
                                                           ("titanium_plate", 6), ("minecraft:water_bucket", 2)])
    for name, ings in SHIPS.items():
        armament("assembly", name, "vehicles", ings)
    for name, ings, count in (
            ("naval_shell", [("minecraft:copper_ingot", 4), ("minecraft:tnt", 2), ("minecraft:gunpowder", 4)], 4),
            ("ciws_belt", [("minecraft:copper_ingot", 6), ("titanium_ingot", 2), ("minecraft:gunpowder", 8)], 1),
            ("ashm_missile", [("steel_plate", 6), ("minecraft:tnt", 10), ("minecraft:blaze_rod", 4), ("avionics_suite", 1)], 1),
            ("hypersonic_missile", [("titanium_plate", 8), ("minecraft:tnt", 12), ("minecraft:blaze_rod", 8), ("avionics_suite", 2),
                                    ("minecraft:fire_charge", 4)], 1),
            ("naval_sam_missile", [("steel_plate", 4), ("minecraft:tnt", 4), ("minecraft:quartz", 6), ("minecraft:redstone", 6)], 2),
            ("torpedo", [("steel_plate", 6), ("minecraft:tnt", 8), ("minecraft:prismarine_shard", 4), ("minecraft:redstone", 6)], 1),
            ("asw_rocket", [("steel_plate", 2), ("minecraft:tnt", 3), ("minecraft:gunpowder", 4)], 4)):
        armament("artillery", name, "artillery", ings, count)


def loot():
    import json
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
    add("military_base", "naval_shell", 3, 2, 8)
    add("military_base", "torpedo", 1, 1, 1)
    add("military_base", "sonar_array", 1, 1, 1)
    add("boss_vault_offshore_vault", "ashm_missile", 4, 1, 2)
    add("boss_vault_offshore_vault", "torpedo", 3, 1, 2)
    add("boss_vault_offshore_vault", "ship_hull_section", 4, 2, 6)


def structures():
    """Plateforme offshore, porte-avions, base navale : structures, ensembles (espacement), tables de butin."""
    W = os.path.join(DATA, "worldgen")
    for name, biomes in (("oil_rig", "#minecraft:is_deep_ocean"), ("carrier", "#minecraft:is_deep_ocean"), ("naval_base", "#minecraft:is_ocean")):
        write(os.path.join(W, "structure", name + ".json"), {"type": f"{MOD}:naval", "kind": name, "biomes": biomes,
                                                             "step": "surface_structures", "spawn_overrides": {}, "terrain_adaptation": "none"})
    for name, spacing, sep, salt in (("oil_rig", 40, 16, 73019455), ("carrier", 52, 20, 64118277), ("naval_base", 34, 12, 55120931)):
        write(os.path.join(W, "structure_set", name + ".json"), {"structures": [{"structure": f"{MOD}:{name}", "weight": 1}],
                                                                 "placement": {"type": "minecraft:random_spread", "spacing": spacing,
                                                                               "separation": sep, "salt": salt}})

    def table(name, rolls, entries):
        pool = []
        for item, w, lo, hi in entries:
            e = {"type": "minecraft:item", "name": item if ":" in item else f"{MOD}:{item}", "weight": w}
            if hi > 1:
                e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
            pool.append(e)
        write(os.path.join(DATA, "loot_table/chests", name + ".json"), {"type": "minecraft:chest", "pools": [
            {"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": pool}]})
    table("naval_depot", (2, 5), [("torpedo", 3, 1, 2), ("naval_shell", 6, 4, 16), ("asw_rocket", 4, 2, 8), ("ciws_belt", 4, 1, 3),
                                   ("naval_sam_missile", 3, 1, 2), ("ashm_missile", 2, 1, 1), ("ship_hull_section", 3, 1, 4),
                                   ("jerrycan", 3, 1, 2), ("sonar_array", 1, 1, 1)])
    table("offshore_supplies", (3, 6), [("ammo_box_50", 4, 1, 2), ("frag_grenade", 4, 1, 3), ("c4", 2, 1, 2), ("jerrycan", 4, 1, 2),
                                         ("minecraft:gold_ingot", 4, 2, 8), ("rpg_rocket", 3, 1, 2), ("bandage", 3, 1, 3),
                                         ("minecraft:cooked_cod", 4, 2, 6), ("ammo_556", 5, 16, 48), ("ammo_762", 4, 16, 40)])


if __name__ == "__main__":
    structures()
    icons()
    recipes()
    loot()
    print("ok")

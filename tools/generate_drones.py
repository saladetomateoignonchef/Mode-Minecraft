#!/usr/bin/env python3
"""
Drones de combat (modèles 3D voxel, échelle 1 : 16 px = 1 bloc) : corps + hélice (tournée par le rendu).
Repère : avant vers +X, bas en y = 0, centre en x = z = 8. Les positions des hélices sont dans DroneRenderer.java.
Utilisation : python3 tools/generate_drones.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import voxel_bake as vb  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
MOD = "modeminecraft"

COLORS = {"bag": "#5a6438", "bag_d": "#434a2a", "strap": "#2e3320", "buckle": "#1c1c1c", "zip": "#8a8f94",
          "grey": "#6e737a", "grey_d": "#4a4e54", "carbon": "#1a1c1f", "carbon_l": "#30343a", "tac": "#485143", "tac_d": "#343b30",
          "titan": "#8c9196", "titan_d": "#5c6166", "stealth": "#2a2d31", "stealth_l": "#3c4046", "lens": "#203a52", "led_g": "#40ff60",
          "led_r": "#ff3030", "orange": "#e07020", "prop": "#22252a", "gun": "#2a2c30", "rocket": "#c8ccd0", "warhead": "#b03020",
          "laser": "#40ff60", "bay": "#2a2f26"}
MATERIAL = {"bag": "fabric", "bag_d": "fabric", "strap": "fabric", "lens": "glass", "led_g": "flat", "led_r": "flat", "laser": "flat", "prop": "polymer", "carbon": "polymer",
            "carbon_l": "polymer", "tac": "paint", "tac_d": "paint", "stealth": "paint", "stealth_l": "paint", "orange": "paint"}


def box(frm, to, key):
    return {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to],
            "faces": {f: {"uv": [0, 0, 1, 1], "texture": "#p"} for f in vb.FACES}, "__key": key}


def b(x0, y0, z0, x1, y1, z1, key):
    return box([x0, y0, z0], [x1, y1, z1], key)


def arm(cx, cz, y, w, key):
    """Bras du centre (8, 8) vers (cx, cz) : un tronçon le long de X puis de Z (escalier pour les diagonales)."""
    out = []
    steps = 4
    for i in range(steps):
        x0 = 8 + (cx - 8) * i / steps
        x1 = 8 + (cx - 8) * (i + 1) / steps
        z0 = 8 + (cz - 8) * i / steps
        z1 = 8 + (cz - 8) * (i + 1) / steps
        out.append(b(min(x0, x1) - w / 2, y, min(z0, z1) - w / 2, max(x0, x1) + w / 2, y + w * 0.8, max(z0, z1) + w / 2, key))
    return out


def motor(cx, cz, y, r, key):
    return [b(cx - r, y, cz - r, cx + r, y + r * 1.6, cz + r, key), b(cx - r * 0.4, y + r * 1.6, cz - r * 0.4, cx + r * 0.4, y + r * 2.0, cz + r * 0.4, "titan")]


def rotor(length, width=0.5, hub=0.35):
    """Hélice bipale centrée en (8, 0, 8), tourne autour de Y."""
    return [b(8 - length / 2, 0, 8 - width / 2, 8 + length / 2, 0.18, 8 + width / 2, "prop"),
            b(8 - hub, -0.05, 8 - hub, 8 + hub, 0.3, 8 + hub, "titan")]


def hornet():
    e = [b(6.6, 0.6, 7.3, 9.6, 2.0, 8.7, "grey"),            # fuselage
         b(9.6, 0.8, 7.5, 10.2, 1.8, 8.5, "grey_d"),           # nez
         b(10.0, 1.0, 7.75, 10.3, 1.5, 8.25, "lens"),          # caméra
         b(3.4, 1.2, 7.8, 6.6, 1.6, 8.2, "grey"),             # poutre de queue
         b(3.0, 1.0, 7.9, 3.6, 2.4, 8.1, "grey_d"),            # dérive
         b(7.8, 2.0, 7.8, 8.2, 2.4, 8.2, "titan"),             # mât du rotor
         b(7.0, 0.0, 7.2, 9.2, 0.2, 7.4, "grey_d"), b(7.0, 0.0, 8.6, 9.2, 0.2, 8.8, "grey_d")]   # patins
    return e, rotor(5.0, 0.35, 0.2)


FPV_M = [(12.5, 8), (3.5, 8), (8, 12.5), (8, 3.5)]          # bras en "+" (le rendu tourne le drone de 45° : un X)


def fpv():
    e = [b(5.6, 0.8, 5.6, 10.4, 1.4, 10.4, "carbon"),         # platine carbone
         b(6.4, 1.4, 6.6, 9.6, 3.0, 9.4, "carbon_l"),          # pile du contrôleur de vol
         b(6.6, 3.0, 6.8, 9.4, 4.0, 9.2, "orange"),            # batterie LiPo
         b(9.6, 1.6, 7.4, 10.4, 2.6, 8.6, "carbon"), b(10.4, 1.8, 7.7, 10.6, 2.4, 8.3, "lens"),   # caméra FPV
         b(6.8, 0.0, 6.8, 9.2, 0.8, 9.2, "titan_d")]           # support ventral modulaire
    for cx, cz in FPV_M:
        e += arm(cx, cz, 0.9, 1.0, "carbon")
        e += motor(cx, cz, 1.4, 0.7, "carbon_l")
    e.append(b(5.4, 1.4, 7.6, 5.8, 2.6, 8.4, "led_r"))
    return e, rotor(5.2, 0.6)


MAVIC_M = [(13, 13), (13, 3), (3, 13), (3, 3)]


def mavic():
    e = [b(4.5, 1.2, 6.0, 11.5, 3.4, 10.0, "tac"),           # corps
         b(5.0, 3.4, 6.5, 11.0, 3.9, 9.5, "tac_d"),
         b(11.5, 1.6, 6.8, 12.6, 3.0, 9.2, "tac_d"), b(12.4, 1.8, 7.3, 12.8, 2.6, 8.7, "lens"),   # nacelle caméra
         b(5.5, 0.2, 6.2, 10.5, 1.2, 9.8, "bay"),              # soute ventrale double
         b(7.9, 0.2, 6.2, 8.1, 1.2, 9.8, "tac_d"),
         b(5.0, 0.0, 5.6, 5.6, 1.2, 6.0, "tac_d"), b(10.4, 0.0, 5.6, 11.0, 1.2, 6.0, "tac_d"),
         b(5.0, 0.0, 10.0, 5.6, 1.2, 10.4, "tac_d"), b(10.4, 0.0, 10.0, 11.0, 1.2, 10.4, "tac_d"),
         b(4.4, 2.0, 7.6, 4.6, 2.6, 8.4, "led_g")]
    for cx, cz in MAVIC_M:
        e += arm(cx, cz, 2.4, 0.9, "tac_d")
        e += motor(cx, cz, 2.6, 0.75, "tac")
    return e, rotor(6.4, 0.6)


WASP_M = [(15, 8), (11.5, 14), (4.5, 14), (1, 8), (4.5, 2), (11.5, 2)]


def wasp():
    e = [b(4.8, 2.0, 4.8, 11.2, 4.4, 11.2, "titan"),         # moyeu hexacoptère
         b(5.4, 4.4, 5.4, 10.6, 5.0, 10.6, "carbon"),
         b(5.6, 0.6, 5.6, 10.4, 2.0, 10.4, "titan_d"),
         b(6.6, -1.4, 6.6, 9.4, 0.6, 9.4, "gun"),              # tourelle ventrale orientable
         b(9.4, -0.9, 7.6, 14.4, -0.1, 8.4, "gun"),             # canon
         b(14.0, -1.1, 7.4, 14.6, 0.1, 8.6, "carbon"),
         b(9.0, 0.0, 6.4, 10.4, 0.6, 7.0, "laser"),            # laser vert
         b(11.2, 2.6, 7.2, 11.6, 3.6, 8.8, "lens")]
    for cx, cz in WASP_M:
        e += arm(cx, cz, 3.0, 1.0, "carbon")
        e += motor(cx, cz, 3.4, 0.8, "titan")
    return e, rotor(5.6, 0.6)


REAPER_M = [(19, 18), (19, -2), (-3, 18), (-3, -2)]


def reaper():
    e = [b(-2.0, 2.0, 4.0, 18.0, 5.4, 12.0, "stealth"),       # fuselage furtif
         b(0.0, 5.4, 5.0, 15.0, 6.4, 11.0, "stealth_l"),
         b(18.0, 2.6, 5.4, 21.0, 4.8, 10.6, "stealth"),        # nez
         b(17.0, 0.6, 6.6, 19.6, 2.6, 9.4, "stealth_l"), b(19.2, 0.9, 7.2, 19.9, 2.3, 8.8, "lens"),   # boule thermique
         b(-4.0, 3.0, 6.6, -2.0, 4.6, 9.4, "stealth_l")]
    for side in (1.0, 13.0):                                   # 2 nacelles de 2 x 2 mini-roquettes
        e.append(b(2.0, 0.4, side, 14.0, 2.0, side + 2.0, "stealth_l"))
        for k in (0.4, 1.2):
            e.append(b(12.0, 0.6 + k * 0.6, side + k - 0.2, 15.4, 1.2 + k * 0.6, side + k + 0.4, "rocket"))
            e.append(b(15.4, 0.7 + k * 0.6, side + k - 0.1, 16.0, 1.1 + k * 0.6, side + k + 0.3, "warhead"))
    for cx, cz in REAPER_M:
        e += arm(cx, cz, 3.6, 1.4, "stealth")
        e.append(b(cx - 3.4, 3.0, cz - 3.4, cx + 3.4, 4.6, cz + 3.4, "stealth_l"))     # carénage d'hélice
        e += motor(cx, cz, 4.0, 0.9, "stealth")
    e.append(b(-2.4, 4.6, 7.6, -2.0, 5.2, 8.4, "led_r"))
    return e, rotor(6.0, 0.7)


DRONES = {"black_hornet": hornet, "fpv_frelon": fpv, "mavic_tactique": mavic, "wasp_mg": wasp, "reaper_x": reaper}
DISPLAY = {"gui": {"rotation": [25, -135, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
           "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
           "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
           "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 3, 0], "scale": [0.35, 0.35, 0.35]},
           "firstperson_righthand": {"rotation": [0, 100, 0], "translation": [0, 3, 0], "scale": [0.35, 0.35, 0.35]}}
GUI_SCALE = {"black_hornet": 1.9, "fpv_frelon": 1.15, "mavic_tactique": 0.95, "wasp_mg": 0.8, "reaper_x": 0.55}


def controller():
    """Radiocommande : boîtier, deux manches, écran, antenne."""
    e = [b(3, 2, 6.5, 13, 8, 9.5, "grey_d"), b(4, 8, 7, 12, 8.4, 9, "carbon"),
         b(5, 4.5, 6.3, 11, 7.5, 6.5, "lens"),                 # écran
         b(4.4, 2.6, 6.2, 5.6, 3.8, 6.5, "carbon"), b(10.4, 2.6, 6.2, 11.6, 3.8, 6.5, "carbon"),   # manches
         b(12, 8, 7.6, 12.8, 13, 8.4, "titan"), b(12.1, 13, 7.7, 12.7, 13.4, 8.3, "led_g")]
    return e


def main():
    painter = vb.Painter(COLORS, lambda k: MATERIAL.get(k, "metal"), {"lens": 200}, seed="drone")
    os.makedirs(os.path.join(ASSETS, "textures/item/drone"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "models/drone"), exist_ok=True)
    for name, fn in DRONES.items():
        body, rot = fn()
        baked = vb.bake({"body": body, "rotor": rot}, skip_chamfer=("lens", "led_g", "led_r", "laser", "prop"))
        vb.paint(baked, painter).save(os.path.join(ASSETS, f"textures/item/drone/{name}.png"))
        tex = {"p": f"{MOD}:item/drone/{name}", "particle": f"{MOD}:item/drone/{name}"}
        for part in ("body", "rotor"):
            with open(os.path.join(ASSETS, f"models/drone/{name}_{part}.json"), "w", encoding="utf-8") as f:
                json.dump({"textures": tex, "elements": vb.elements(baked, part)}, f, separators=(",", ":"))
        disp = json.loads(json.dumps(DISPLAY))
        k = GUI_SCALE[name]
        for key in ("gui", "fixed", "ground"):
            disp[key]["scale"] = [round(v * k, 3) for v in disp[key]["scale"]]
        with open(os.path.join(ASSETS, f"models/item/{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"textures": tex, "elements": vb.elements(baked, "body"), "display": disp}, f, separators=(",", ":"))
        print(f"{name} : atlas {baked.w}x{baked.h}")
    baked = vb.bake({"c": controller()}, skip_chamfer=("lens", "led_g"))
    vb.paint(baked, painter).save(os.path.join(ASSETS, "textures/item/drone/drone_controller.png"))
    tex = {"p": f"{MOD}:item/drone/drone_controller", "particle": f"{MOD}:item/drone/drone_controller"}
    with open(os.path.join(ASSETS, "models/item/drone_controller.json"), "w", encoding="utf-8") as f:
        json.dump({"textures": tex, "elements": vb.elements(baked, "c"), "display": {
            "gui": {"rotation": [20, -30, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.7, 0.7, 0.7]},
            "thirdperson_righthand": {"rotation": [70, 180, 0], "translation": [0, 2, -2], "scale": [0.45, 0.45, 0.45]},
            "thirdperson_lefthand": {"rotation": [70, 180, 0], "translation": [0, 2, -2], "scale": [0.45, 0.45, 0.45]},
            "firstperson_righthand": {"rotation": [20, 170, 0], "translation": [-2, 3, 0], "scale": [0.6, 0.6, 0.6]},
            "firstperson_lefthand": {"rotation": [20, 170, 0], "translation": [-2, 3, 0], "scale": [0.6, 0.6, 0.6]}}},
            f, separators=(",", ":"))


def backpack():
    """Sac à dos tactique, dans le repère du corps (y vers le haut, cou en y = 14, dos en z = 10)."""
    e = [b(4.2, 3.0, 10.1, 11.8, 13.0, 14.0, "bag"),              # poche principale
         b(4.6, 12.6, 10.4, 11.4, 13.6, 13.6, "bag_d"),          # rabat
         b(5.0, 4.0, 14.0, 11.0, 9.5, 15.4, "bag_d"),            # poche avant
         b(5.4, 9.5, 14.0, 10.6, 9.9, 15.4, "zip"),              # fermeture
         b(3.2, 4.5, 10.6, 4.2, 10.5, 13.4, "bag_d"), b(11.8, 4.5, 10.6, 12.8, 10.5, 13.4, "bag_d"),   # poches latérales
         b(5.0, 5.0, 15.4, 11.0, 5.6, 15.8, "strap"), b(5.0, 7.6, 15.4, 11.0, 8.2, 15.8, "strap"),    # sangles MOLLE
         b(7.6, 8.2, 15.4, 8.4, 8.8, 15.9, "buckle"),
         b(4.8, 13.0, 9.6, 5.8, 14.2, 10.4, "strap"), b(10.2, 13.0, 9.6, 11.2, 14.2, 10.4, "strap")]  # bretelles
    return e


def module_icons():
    """Icônes 16x16 des modules : hélices silencieuses en carbone, batterie LiPo-Titan, caisson cargo."""
    from PIL import Image
    out = os.path.join(ASSETS, "textures/item")

    def save(name, pixels):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for (x, y), c in pixels.items():
            img.putpixel((x, y), c)
        img.save(os.path.join(out, f"{name}.png"))
        with open(os.path.join(ASSETS, f"models/item/{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}}, f, indent=2)
    px = {}
    for i in range(-6, 7):                                    # deux pales en carbone, en croix
        for w in (-1, 0):
            px[(7 + i, 7 + w)] = (30, 32, 36, 255) if abs(i) < 6 else (60, 64, 70, 255)
            px[(7 + w, 7 + i)] = (40, 42, 46, 255) if abs(i) < 6 else (70, 74, 80, 255)
    for x in range(6, 9):
        for y in range(6, 9):
            px[(x, y)] = (150, 156, 164, 255)
    save("silent_rotors", px)
    px = {}
    for x in range(3, 13):
        for y in range(4, 13):
            edge = x in (3, 12) or y in (4, 12)
            px[(x, y)] = (30, 60, 120, 255) if edge else (60, 140, 230, 255) if y < 8 else (40, 100, 190, 255)
    for x in (5, 6, 9, 10):
        px[(x, 3)] = (200, 200, 205, 255)
    for x in range(5, 11):
        px[(x, 10)] = (230, 230, 240, 255) if x < 9 else (60, 200, 90, 255)
    save("high_cap_battery", px)
    px = {}
    for x in range(2, 14):
        for y in range(4, 14):
            edge = x in (2, 13) or y in (4, 13)
            px[(x, y)] = (60, 66, 54, 255) if edge else (88, 98, 78, 255) if (x + y) % 5 else (70, 78, 62, 255)
    for x in range(6, 10):
        px[(x, 8)] = (200, 170, 60, 255)
    save("cargo_module", px)


def backpack_model():
    painter = vb.Painter(COLORS, lambda k: MATERIAL.get(k, "metal"), {}, seed="backpack")
    baked = vb.bake({"m": backpack()}, skip_chamfer=("zip",))
    vb.paint(baked, painter).save(os.path.join(ASSETS, "textures/item/drone/tactical_backpack.png"))
    tex = {"p": f"{MOD}:item/drone/tactical_backpack", "particle": f"{MOD}:item/drone/tactical_backpack"}
    with open(os.path.join(ASSETS, "models/item/tactical_backpack.json"), "w", encoding="utf-8") as f:
        json.dump({"textures": tex, "elements": vb.elements(baked, "m"), "display": {
            "gui": {"rotation": [20, 200, 0], "translation": [0, 0, -3], "scale": [1.1, 1.1, 1.1]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, -3], "scale": [1, 1, 1]},
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 1, -3], "scale": [0.6, 0.6, 0.6]},
            "firstperson_righthand": {"rotation": [0, 160, 0], "translation": [0, 2, -2], "scale": [0.6, 0.6, 0.6]}}},
            f, separators=(",", ":"))


if __name__ == "__main__":
    main()
    module_icons()
    backpack_model()

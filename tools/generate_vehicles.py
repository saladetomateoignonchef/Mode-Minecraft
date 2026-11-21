#!/usr/bin/env python3
"""
Véhicules (modèles 3D voxel, à l'échelle 1/2 : dessinés x2 en jeu) : quad, voiture de police, jeep, camion blindé.
Pièces : carrosserie (body) et roue (wheel, tournée et braquée par le rendu).
Repère : avant vers +X, sol en y = 0, centre en x = z = 8.
Utilisation : python3 tools/generate_vehicles.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import voxel_bake as vb  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
MOD = "modeminecraft"

COLORS = {"red": "#b02a20", "olive": "#4b5a2a", "olive_d": "#353f1d", "black": "#1c1e22", "white": "#e6e8ea", "glass": "#7fb0c8",
          "tire": "#141414", "rim": "#8a9098", "metal": "#5b6068", "seat": "#2a2420", "light": "#fff6c0", "red_l": "#ff2030",
          "blue_l": "#2050ff", "sand": "#a68b5b", "chrome": "#c8ccd2", "navy": "#24324e", "silver": "#9aa2ac", "yellow": "#e8b81e",
          "checker": "#202020", "bus": "#c8322a", "bus_w": "#ececec"}
MATERIAL = {"glass": "glass", "light": "flat", "red_l": "flat", "blue_l": "flat", "tire": "polymer", "seat": "leather"}


def box(frm, to, key):
    return {"from": frm, "to": to, "faces": {f: {"uv": [0, 0, 1, 1], "texture": "#p"} for f in vb.FACES}, "__key": key}


def zb(x0, y0, x1, y1, z0, z1, key):
    return box([x0, y0, z0], [x1, y1, z1], key)


def wheel(r, w):
    """Roue centrée sur (8, 8, 8) : pneu octogonal + jante (tourne autour de Z)."""
    return [box([8 - r, 8 - r * 0.42, 8 - w / 2], [8 + r, 8 + r * 0.42, 8 + w / 2], "tire"),
            box([8 - r * 0.42, 8 - r, 8 - w / 2], [8 + r * 0.42, 8 + r, 8 + w / 2], "tire"),
            box([8 - r * 0.72, 8 - r * 0.72, 8 - w / 2], [8 + r * 0.72, 8 + r * 0.72, 8 + w / 2], "tire"),
            box([8 - r * 0.45, 8 - r * 0.45, 8 - w / 2 - 0.1], [8 + r * 0.45, 8 + r * 0.45, 8 + w / 2 + 0.1], "rim"),
            box([8 - r * 0.15, 8 - r * 0.15, 8 - w / 2 - 0.2], [8 + r * 0.15, 8 + r * 0.15, 8 + w / 2 + 0.2], "metal")]


def quad():
    return [zb(1, 2.5, 15, 4, 4, 12, "metal"),                       # châssis
            zb(0, 4, 5, 6.5, 3, 13, "red"), zb(11, 4, 16, 6.5, 3, 13, "red"),   # garde-boue
            zb(5, 4, 11, 6, 5, 11, "red"), zb(4, 6, 10, 7.4, 5.5, 10.5, "seat"),
            zb(11, 6.5, 12, 9, 7.5, 8.5, "metal"), zb(11.5, 9, 12.5, 9.6, 4, 12, "black"),   # guidon
            zb(15.5, 5, 16.2, 6, 6, 10, "light"), zb(0, 6.5, 4, 7, 4, 12, "metal")]           # phare, porte-bagages


def police_car():
    e = [zb(-10, 2, 26, 6.5, 0.5, 15.5, "white"),                   # caisse
         zb(-10, 2, -2, 6.5, 0.4, 15.6, "black"), zb(17, 2, 26, 6.5, 0.4, 15.6, "black"),   # capot / coffre noirs
         zb(-1, 6.5, 15, 10, 1.5, 14.5, "white"),                    # habitacle
         zb(-0.6, 7, 14.6, 9.6, 1.4, 14.6, "glass"),
         zb(-1, 10, 15, 10.8, 1.5, 14.5, "black"),                   # toit
         zb(4, 10.8, 6, 11.8, 3, 7.8, "red_l"), zb(4, 10.8, 6, 11.8, 8.2, 13, "blue_l"),   # rampe lumineuse
         zb(25.6, 3.5, 26.4, 5, 2, 5, "light"), zb(25.6, 3.5, 26.4, 5, 11, 14, "light"),
         zb(-10.4, 3.5, -9.6, 5, 2, 5, "red_l"), zb(-10.4, 3.5, -9.6, 5, 11, 14, "red_l"),
         zb(26, 2, 27, 3.5, 1, 15, "chrome"), zb(-11, 2, -10, 3.5, 1, 15, "chrome")]
    e += [zb(6, 3, 12, 5.5, 0.3, 0.5, "black"), zb(6, 3, 12, 5.5, 15.5, 15.7, "black")]    # bandes POLICE
    return e


def jeep():
    return [zb(-8, 2.5, 24, 7, 1, 15, "olive"), zb(16, 7, 24, 7.6, 1.5, 14.5, "olive_d"),   # caisse, capot
            zb(15.4, 7, 16, 11, 1.5, 14.5, "metal"), zb(15.5, 7.6, 15.9, 10.6, 2.5, 13.5, "glass"),   # pare-brise
            zb(2, 7, 3, 13, 1.5, 2.5, "metal"), zb(2, 7, 3, 13, 13.5, 14.5, "metal"), zb(2, 12, 3, 13, 1.5, 14.5, "metal"),   # arceau
            zb(4, 7, 9, 8.6, 2.5, 7, "seat"), zb(4, 7, 9, 8.6, 9, 13.5, "seat"), zb(-6, 7, 1, 8.6, 2.5, 13.5, "seat"),
            zb(-9.5, 3.5, -8, 9.5, 5, 11, "tire"), zb(24, 4, 24.6, 6, 2, 4, "light"), zb(24, 4, 24.6, 6, 12, 14, "light"),
            zb(24, 3, 25, 4, 1, 15, "metal")]


def armored_truck():
    return squeeze_x([zb(-16, 4, 32, 7, 0, 16, "olive_d"),                     # châssis
            zb(-16, 7, 17, 21, -0.5, 16.5, "olive"),                 # caisse blindée
            zb(18, 7, 32, 18, 0, 16, "olive"),                       # cabine
            zb(30.4, 12, 32.2, 16, 2, 14, "glass"), zb(30.6, 12.5, 32.3, 15.5, 2, 14, "metal"),   # pare-brise grillagé
            zb(-14, 15, 15, 16, -0.6, 16.6, "olive_d"), zb(-14, 9, 15, 10, -0.6, 16.6, "olive_d"),   # renforts
            zb(-6, 13, 8, 14, -0.7, -0.5, "black"), zb(-6, 13, 8, 14, 16.5, 16.7, "black"),   # meurtrières
            zb(-4, 21, 6, 22, 3, 13, "metal"),                        # anneau de tourelle
            zb(31.8, 8, 32.6, 10, 1, 4, "light"), zb(31.8, 8, 32.6, 10, 12, 15, "light"),
            zb(32, 5, 33, 7, -0.5, 16.5, "metal"), zb(-16.6, 8, -16, 19, 5, 11, "black")], 0.92)


def sedan(color="navy"):
    """Berline civile : caisse basse, capot et coffre, habitacle vitré, phares."""
    return [zb(-10, 2, 26, 6, 0.5, 15.5, color),
            zb(-1, 6, 16, 9.8, 1.5, 14.5, color),
            zb(-0.6, 6.4, 15.6, 9.4, 1.4, 14.6, "glass"),
            zb(-1, 9.8, 16, 10.6, 1.5, 14.5, color),
            zb(25.6, 3.5, 26.4, 5, 2, 5, "light"), zb(25.6, 3.5, 26.4, 5, 11, 14, "light"),
            zb(-10.4, 3.5, -9.6, 5, 2, 5, "red_l"), zb(-10.4, 3.5, -9.6, 5, 11, 14, "red_l"),
            zb(26, 2, 27, 3.2, 1, 15, "chrome"), zb(-11, 2, -10, 3.2, 1, 15, "chrome"),
            zb(10, 6.5, 11, 7, 0.2, 0.5, "chrome"), zb(10, 6.5, 11, 7, 15.5, 15.8, "chrome")]   # poignées


def taxi():
    e = sedan("yellow")
    e += [zb(5, 10.6, 9, 11.8, 5, 11, "white"),                     # enseigne TAXI
          zb(-9, 4.5, 25, 5, 0.3, 0.5, "checker"), zb(-9, 4.5, 25, 5, 15.5, 15.7, "checker")]   # damier
    return e


def bus():
    """Bus urbain : long, vitres sur tout le flanc, portes, bande blanche, girouette (modèle resserré en longueur)."""
    return squeeze_x([zb(-16, 3, 32, 22, -0.5, 16.5, "bus"),
            zb(-15.5, 11, 31.5, 18, -0.6, 16.6, "glass"),           # vitres latérales
            zb(-16, 9, 32, 10.5, -0.7, 16.7, "bus_w"),               # bande blanche
            zb(31.6, 8, 32.4, 20, 1, 15, "glass"),                  # pare-brise
            zb(-16.4, 13, -15.8, 20, 2, 14, "glass"),
            zb(32, 20, 32.5, 21.5, 3, 13, "light"),                   # girouette
            zb(24, 3.5, 30, 18, 16.5, 16.8, "black"), zb(4, 3.5, 10, 18, 16.5, 16.8, "black"),   # portes (côté droit)
            zb(32, 4, 32.8, 6, 1, 4, "light"), zb(32, 4, 32.8, 6, 12, 15, "light"),
            zb(-16.8, 4, -16, 6, 1, 4, "red_l"), zb(-16.8, 4, -16, 6, 12, 15, "red_l"),
            zb(-15, 22, 10, 23, 2, 14, "silver")], 0.92)                 # climatisation sur le toit


def squeeze_x(els, k):
    """Resserre le modèle en longueur (les modèles de Minecraft sont limités à 48 pixels)."""
    for e in els:
        e["from"][0] = round(8 + (e["from"][0] - 8) * k, 3)
        e["to"][0] = round(8 + (e["to"][0] - 8) * k, 3)
    return els


VEHICLES = {"quad": (quad, 2.6, 2.2), "police_car": (police_car, 3.0, 2.4), "jeep": (jeep, 3.4, 2.6), "armored_truck": (armored_truck, 4.0, 3.2),
            "sedan": (sedan, 2.8, 2.2), "taxi": (taxi, 2.8, 2.2), "bus": (bus, 3.6, 3.0)}


def main():
    painter = vb.Painter(COLORS, lambda k: MATERIAL.get(k, "paint" if k in ("red", "white", "olive", "olive_d", "sand", "navy", "yellow", "bus", "bus_w")
                                                       else "metal"),
                         {"glass": 160}, seed="vehicle")
    os.makedirs(os.path.join(ASSETS, "textures/item/vehicle"), exist_ok=True)
    for name, (fn, r, w) in VEHICLES.items():
        baked = vb.bake({"body": fn(), "wheel": wheel(r, w)}, skip_chamfer=("glass", "light", "red_l", "blue_l"))
        vb.paint(baked, painter).save(os.path.join(ASSETS, f"textures/item/vehicle/{name}.png"))
        tex = {"p": f"{MOD}:item/vehicle/{name}", "particle": f"{MOD}:item/vehicle/{name}"}
        for part in ("body", "wheel"):
            path = os.path.join(ASSETS, f"models/vehicle/{name}_{part}.json")
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8") as f:
                json.dump({"textures": tex, "elements": vb.elements(baked, part)}, f, separators=(",", ":"))
        # objet : la carrosserie et 4 roues, vue en 3D
        els = vb.elements(baked, "body")
        with open(os.path.join(ASSETS, f"models/item/{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"textures": tex, "elements": els, "display": {
                "gui": {"rotation": [25, -135, 0], "translation": [0, -1, 0], "scale": [0.4, 0.4, 0.4]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]},
                "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.45, 0.45, 0.45]},
                "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
                "firstperson_righthand": {"rotation": [0, 100, 0], "translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]}}}, f, separators=(",", ":"))
        print(f"{name} : atlas {baked.w}x{baked.h}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Enregistre de vraies vidéos du jeu (client Minecraft sous Xvfb) pour le README : docs/videos/jeu_*.mp4 et .gif.

Prérequis : serveur dédié lancé avec RCON (port 25575, mot de passe « test », monde plat, mode créatif) et client lancé
avec un joueur « Demo » connecté (voir CLAUDE.md / tools/demo_lib.py).
Utilisation : python3 tools/record_demos.py [scène...]
"""
import os
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import demo_lib as L  # noqa: E402

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "docs/videos")
RAW = "/tmp/rec"
r = None
pad = None


def sh(c, wait=0.15):
    out = r.cmd(c)
    time.sleep(wait)
    return out


def reset(x=0, y=-59, z=0, yaw=0, pitch=0):
    pad.release_all()
    sh("gamemode creative Demo")
    sh("clear Demo")
    camera(0)
    sh("kill @e[type=!player]")
    sh("effect clear Demo")
    sh(f"tp Demo {x} {y} {z} {yaw} {pitch}", 0.6)


def finish(name, fps=20, width=640):
    """Convertit l'enregistrement brut en MP4 léger et en GIF pour le README."""
    raw = os.path.join(RAW, name + ".mp4")
    mp4 = os.path.join(OUT, name + ".mp4")
    gif = os.path.join(OUT, name + ".gif")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", raw, "-vf", f"scale={width}:-2", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "26", mp4], check=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", raw, "-vf",
                    f"fps=10,scale={min(width, 480)}:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=80:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4",
                    "-loop", "0", gif], check=True)
    print(name, round(os.path.getsize(mp4) / 1e6, 1), "Mo mp4,", round(os.path.getsize(gif) / 1e6, 1), "Mo gif")


CAM = 0


def camera(mode):
    """Vue : 0 = première personne, 1 = derrière, 2 = de face (F5 fait défiler les trois)."""
    global CAM
    while CAM != mode:
        pad.tap("F5")
        time.sleep(0.25)
        CAM = (CAM + 1) % 3


class Take:
    def __init__(self, name):
        self.name = name
        self.rec = L.Recorder(os.path.join(RAW, name + ".mp4"))

    def __enter__(self):
        self.rec.start()
        return self

    def __exit__(self, *a):
        pad.release_all()
        self.rec.stop()
        finish(self.name)


# ----------------------------------------------------------------------------- scènes
def armes():
    reset(0, -59, 0, 0, 0)
    for stand in range(-9, 10, 3):
        sh(f"summon minecraft:armor_stand {stand} -60 22 {{NoGravity:0b}}", 0.05)
    sh("summon minecraft:zombie 0 -60 30 {NoAI:1b,Silent:1b}", 0.05)
    kit = [("m4", "ammo_556"), ("ak47", "ammo_762"), ("glock17", "ammo_9mm"), ("remington_870", "shotgun_shell"), ("barrett_m82", "ammo_50bmg"), ("rpg7", "rpg_rocket")]
    for i, (gun, ammo) in enumerate(kit):
        sh(f"item replace entity Demo hotbar.{i} with modeminecraft:{gun}", 0.05)
        sh(f"give Demo modeminecraft:{ammo} 64", 0.05)
    time.sleep(5)                                   # laisse passer les notifications de recettes
    with Take("jeu_armes"):
        for slot in range(1, 7):
            pad.tap(str(slot))
            time.sleep(0.7)
            pad.tap("r")
            time.sleep(1.8)
            pad.mouse_down(3)                       # viser
            time.sleep(0.6)
            pad.mouse_down(1)                       # tirer
            time.sleep(1.4 if slot < 6 else 0.4)
            pad.mouse_up(1)
            pad.mouse_up(3)
            pad.move(60 * (1 if slot % 2 else -1), 0, steps=6)
            time.sleep(0.4)


def melee():
    reset(0, -59, 0, 0, 0)
    for k, x in enumerate((-3, 0, 3)):
        sh(f"summon minecraft:armor_stand {x} -60 3 {{ShowArms:1b}}", 0.05)
    for i, w in enumerate(("katana", "claymore", "combat_knife", "war_hammer")):
        sh(f"item replace entity Demo hotbar.{i} with modeminecraft:{w}", 0.05)
    time.sleep(5)
    with Take("jeu_melee"):
        for slot in range(1, 5):
            pad.tap(str(slot))
            time.sleep(0.6)
            for _ in range(3):                      # combo de trois coups
                pad.click(1)
                time.sleep(0.45)
            pad.mouse_down(3)                       # parade
            time.sleep(0.8)
            pad.mouse_up(3)
            pad.move(120 if slot % 2 else -120, 0, steps=8)
            time.sleep(0.3)


def armures():
    reset(0, -59, 0, 0, 0)
    sets = [("galea", "wwi_cuirass", None, None), ("mich_2000", "swat_vest", "swat_pants", "salomon_forces_boots"),
            ("altyn", "vest_6b3", "ratnik_pants", "m65_combat_boots"), ("ops_core_fast", "jpc_plate_carrier", "crye_g3_pants", "lowa_zephyr_boots")]
    time.sleep(2)
    camera(2)                                       # vue de face
    with Take("jeu_armures"):
        for head, chest, legs, feet in sets:
            for slot, item in (("head", head), ("chest", chest), ("legs", legs), ("feet", feet)):
                if item:
                    sh(f"item replace entity Demo armor.{slot} with modeminecraft:{item}", 0.05)
            pad.move(260, 0, steps=26, dt=0.05)
            time.sleep(0.5)
    camera(0)


def vehicules():
    reset(0, -59, 0, 0, 0)
    sh("summon modeminecraft:m1a2_abrams 0 -59.5 6 {}")
    sh("ride Demo mount @e[type=modeminecraft:m1a2_abrams,limit=1]", 1.0)
    camera(1)
    time.sleep(2)
    with Take("jeu_vehicules"):
        pad.down("w")
        time.sleep(4)
        pad.down("a")
        time.sleep(1.8)
        pad.up("a")
        time.sleep(1.5)
        pad.down("d")
        time.sleep(1.8)
        pad.up("d")
        time.sleep(1.5)
        pad.up("w")
        pad.mouse_down(1)                           # canon
        time.sleep(0.6)
        pad.mouse_up(1)
        time.sleep(2)
    camera(0)


def avion_leger():
    reset(0, -59, 0, 0, 0)
    sh("summon modeminecraft:light_civil_plane 0 -59.5 0 {}")
    sh("ride Demo mount @e[type=modeminecraft:light_civil_plane,limit=1]", 1.0)
    camera(1)
    time.sleep(2)
    with Take("jeu_avion_leger"):
        pad.down("w")
        time.sleep(4.0)                             # roulage et décollage court
        pad.move(0, -90, steps=12)                  # cabrer
        time.sleep(3)
        pad.move(0, 40, steps=8)
        pad.down("a")
        time.sleep(2.5)
        pad.up("a")
        time.sleep(3)
        pad.up("w")
    camera(0)



def porte_avions():
    """Porte-avions : barre en haut de l'îlot, avions sur le pont, passage de la passerelle à un avion, catapulte, décollage."""
    sh("time set day")
    sh("gamerule doDaylightCycle false")
    camera(0)
    pad.release_all()
    sh("gamemode creative Demo")
    sh("kill @e[type=!player]")
    sh("tp Demo 2000 -50 1900 0 0", 4)
    sh("summon modeminecraft:uss_gerald_r_ford 2000 -57.6 1950")
    for t, f in (("f16_falcon", -43), ("f22_raptor", -23), ("f16_falcon", -3), ("f22_raptor", 17)):
        sh(f"summon modeminecraft:{t} 2010 -39.1 {1950 + f}")
    time.sleep(3)
    sh("ride Demo mount @e[type=modeminecraft:uss_gerald_r_ford,limit=1]", 2)
    with Take("jeu_porte_avions"):
        time.sleep(2.5)
        pad.move(-120, 15, steps=15)                # regard sur le pont
        time.sleep(1.5)
        pad.down("w")                               # en avant : le navire prend de la vitesse
        time.sleep(3)
        pad.up("w")
        for _ in range(6):                          # touche C : places puis avion du pont
            pad.tap("c")
            time.sleep(0.6)
        time.sleep(1.5)
        pad.down("w")                               # plein gaz : catapulte
        time.sleep(2.2)
        pad.move(0, -90, steps=12)                  # cabrer
        time.sleep(5)
        pad.up("w")
    camera(0)


def bots():
    """Deux factions ennemies (garnison américaine contre soviétiques) engagent la fusillade, posées avec leurs œufs d'apparition."""
    reset(0, -59, 0, 180, 42)
    sh("give Demo modeminecraft:bot_egg_garrison 16", 0.05)
    sh("give Demo modeminecraft:bot_egg_soviet 16", 0.05)
    sh("item replace entity Demo hotbar.0 with modeminecraft:bot_egg_garrison 16", 0.05)
    sh("item replace entity Demo hotbar.1 with modeminecraft:bot_egg_soviet 16", 0.05)
    time.sleep(5)
    pad.tap("1")
    for _ in range(4):                              # garnison au nord
        pad.click(3, 0.3)
        time.sleep(0.4)
        pad.move(40, 0, steps=3)
    sh("tp Demo 0 -59 4 0 42", 0.6)
    pad.tap("2")
    for _ in range(4):                              # soviétiques au sud
        pad.click(3, 0.3)
        time.sleep(0.4)
        pad.move(-40, 0, steps=3)
    sh("tp Demo -8 -59 2 -90 4", 0.6)
    with Take("jeu_bots"):
        time.sleep(4)
        pad.move(50, 0, steps=10)
        time.sleep(6)
        pad.move(-100, 0, steps=15)
        time.sleep(6)


def explosifs():
    """Grenades à fragmentation lancées, charge C4 posée et déclenchée au détonateur."""
    reset(0, -59, 0, 0, -8)
    for x in range(-8, 9, 4):
        sh(f"summon minecraft:armor_stand {x} -60 22 {{}}", 0.05)
    sh("give Demo modeminecraft:frag_grenade 16", 0.05)
    sh("give Demo modeminecraft:c4 4", 0.05)
    sh("give Demo modeminecraft:detonator 1", 0.05)
    sh("item replace entity Demo hotbar.0 with modeminecraft:frag_grenade 16", 0.05)
    sh("item replace entity Demo hotbar.1 with modeminecraft:c4 4", 0.05)
    sh("item replace entity Demo hotbar.2 with modeminecraft:detonator", 0.05)
    time.sleep(5)
    with Take("jeu_explosifs"):
        pad.tap("1")
        time.sleep(0.5)
        for k in range(3):
            pad.click(3, 0.3)                          # lancer
            time.sleep(1.8)
            pad.move(30 * (1 if k % 2 else -1), 0, steps=3)
        time.sleep(1.5)
        sh("tp Demo 0 -59 6 0 35", 0.4)
        pad.tap("2")
        time.sleep(0.4)
        pad.click(3, 0.3)                              # pose du C4 au sol
        time.sleep(1.0)
        sh("tp Demo 0 -59 -10 0 0", 0.5)
        pad.tap("3")
        time.sleep(0.6)
        pad.click(3, 0.3)                              # détonateur
        time.sleep(3)
    sh("kill @e[type=!player]")


def helicoptere():
    reset(0, -59, 0, 0, 0)
    sh("summon modeminecraft:uh_60_black_hawk 0 -59.5 6 {}")
    sh("ride Demo mount @e[type=modeminecraft:uh_60_black_hawk,limit=1]", 1.0)
    camera(1)
    time.sleep(2)
    with Take("jeu_helicoptere"):
        pad.down("space")                           # monter
        time.sleep(4)
        pad.up("space")
        pad.down("w")                               # avancer
        time.sleep(3)
        pad.down("a")
        time.sleep(2)
        pad.up("a")
        time.sleep(2)
        pad.up("w")
        time.sleep(1.5)
    camera(0)


def navire():
    sh("time set day")
    camera(0)
    pad.release_all()
    sh("gamemode creative Demo")
    sh("kill @e[type=!player]")
    sh("tp Demo 2000 -50 1900 0 0", 4)
    sh("summon modeminecraft:arleigh_burke 2000 -57.6 1930")
    time.sleep(2)
    sh("ride Demo mount @e[type=modeminecraft:arleigh_burke,limit=1]", 1.5)
    camera(1)
    time.sleep(1.5)
    with Take("jeu_navire"):
        pad.down("w")
        time.sleep(4)
        pad.down("d")
        time.sleep(3)
        pad.up("d")
        time.sleep(1.5)
        pad.mouse_down(1)                           # tir du canon naval
        time.sleep(0.5)
        pad.mouse_up(1)
        time.sleep(2.5)
        pad.up("w")
    camera(0)


def drone():
    """Drone Mavic : on le pose, on associe la télécommande (Maj + clic droit sur le drone), puis pilotage à la caméra du drone."""
    reset(0, -59, 0, 0, 42)
    sh("item replace entity Demo hotbar.0 with modeminecraft:mavic_tactique", 0.05)
    sh("item replace entity Demo hotbar.1 with modeminecraft:drone_controller", 0.05)
    time.sleep(5)
    with Take("jeu_drone"):
        pad.tap("1")
        time.sleep(0.6)
        pad.move(4, 0, steps=2)
        pad.click(3, 0.3)                              # pose du drone
        time.sleep(1.5)
        sh("tp Demo 0.5 -59 0.5 0 36", 0.8)           # on vise le drone posé (il est à 2 blocs devant)
        pad.tap("2")
        time.sleep(0.6)
        pad.down("Shift_L")
        time.sleep(0.6)
        pad.move(2, 2, steps=1)
        pad.click(3, 0.3)                              # Maj + clic droit sur le drone : appairage
        time.sleep(1.2)
        pad.up("Shift_L")
        time.sleep(0.6)
        pad.click(3, 0.3)                              # clic droit : pilotage à la caméra du drone
        time.sleep(1.5)
        pad.down("space")                              # montée
        time.sleep(3)
        pad.up("space")
        pad.down("w")                                  # en avant, puis virage
        time.sleep(2.5)
        pad.move(220, 0, steps=20)
        time.sleep(3)
        pad.up("w")
        pad.tap("r")                                   # retour automatique au pilote
        time.sleep(3)


def hospital_origin():
    """Coin de l'hôpital (repère de la génération) : il est à 2 chunks à l'ouest et 1 au sud du point de la ville retourné par « locate »."""
    import re
    out = sh("locate structure modeminecraft:city_plains")
    m = re.search(r"\[(-?\d+), ~, (-?\d+)\]", out)
    if not m:
        raise SystemExit("ville introuvable : " + out)
    ox, oz = int(m.group(1)) - 32, int(m.group(2)) + 16
    sh(f"tp Demo {ox + 15} -45 {oz - 12} 0 25", 6)        # charge les chunks de l'hôpital
    # les entités de génération (ambulance, pharmacienne) ont pu être supprimées par une autre scène : on les remet
    if "No entity" in sh(f"data get entity @e[type=modeminecraft:vehicle,nbt={{VehicleType:7}},x={ox + 7},y=-60,z={oz + 1},distance=..30,limit=1] Pos") or \
            "found" in sh(f"data get entity @e[type=modeminecraft:vehicle,nbt={{VehicleType:7}},x={ox + 7},y=-60,z={oz + 1},distance=..30,limit=1] Pos"):
        sh(f"summon modeminecraft:vehicle {ox + 7.5} -60 {oz + 1.5} {{VehicleType:7,Rotation:[90f,0f]}}")
    if "No entity" in sh(f"data get entity @e[type=modeminecraft:human_bot,nbt={{Pharmacy:1b}},x={ox + 15},y=-60,z={oz + 9},distance=..12,limit=1] Pos") or \
            "found" in sh(f"data get entity @e[type=modeminecraft:human_bot,nbt={{Pharmacy:1b}},x={ox + 15},y=-60,z={oz + 9},distance=..12,limit=1] Pos"):
        sh(f"summon modeminecraft:human_bot {ox + 15.5} -60 {oz + 9.5} {{Pharmacy:1b,Shopkeeper:1b,Faction:\"civilian\",Role:\"CIVILIAN\",PersistenceRequired:1b}}")
    return float(ox), -61.0, float(oz)


def blessures():
    """Fracture en tombant, attelle puis plâtre ; saignement et garrot ; défibrillateur."""
    sh("time set day")
    reset(0, 80, 0, 0, 12)
    sh("gamemode survival Demo")
    sh("effect clear Demo")
    sh("tp Demo 400 -59 400 0 12", 3)
    for i, it in enumerate(("splint", "plaster_cast", "tourniquet", "defibrillator")):
        sh(f"item replace entity Demo hotbar.{i} with modeminecraft:{it}", 0.05)
    time.sleep(5)
    with Take("jeu_blessures"):
        time.sleep(1.5)
        sh("damage Demo 9 minecraft:fall")             # chute : fracture
        time.sleep(3)
        pad.tap("1")                                  # attelle
        time.sleep(0.5)
        pad.move(3, 0, steps=1)
        pad.click(3, 0.3)
        time.sleep(3)
        pad.tap("2")                                  # plâtre
        time.sleep(0.5)
        pad.move(3, 0, steps=1)
        pad.click(3, 0.3)
        time.sleep(3)
        sh("effect give Demo modeminecraft:bleeding 30 0")    # blessure par balle : saignement
        time.sleep(3)
        pad.tap("3")                                  # garrot
        time.sleep(0.5)
        pad.move(3, 0, steps=1)
        pad.click(3, 0.3)
        time.sleep(3)
        sh("effect give Demo minecraft:instant_health 1 4")     # on se remet d'abord en forme
        time.sleep(1)
        sh("damage Demo 11 minecraft:generic")        # blessure grave (moins de la moitié des PV)
        time.sleep(2)
        pad.tap("4")                                  # défibrillateur
        time.sleep(0.5)
        pad.move(3, 0, steps=1)
        pad.click(3, 0.3)
        time.sleep(3)
    sh("gamemode creative Demo")


def hopital():
    """Visite de l'hôpital de la métropole : façade et ambulance, accueil et pharmacienne, salle de soins, ambulance."""
    sh("time set day")
    sh("gamerule doDaylightCycle false")
    camera(0)
    pad.release_all()
    sh("gamemode creative Demo")
    ox, oy, oz = hospital_origin()
    sh(f"tp Demo {ox + 15} {oy + 1} {oz - 14} 0 6", 4)
    time.sleep(3)
    with Take("jeu_hopital"):
        time.sleep(2)
        pad.down("w")                                 # on marche vers l'entrée
        time.sleep(5.5)
        pad.up("w")
        sh(f"tp Demo {ox + 15} {oy + 1} {oz + 5} 0 8", 1)   # accueil
        time.sleep(2.5)
        pad.move(-40, 8, steps=8)
        time.sleep(2)
        sh(f"tp Demo {ox + 22} {oy + 1.4} {oz + 10.5} 0 8", 1)   # salle de soins (lits, perfusions)
        time.sleep(1.5)
        pad.move(60, 6, steps=12)
        time.sleep(2.5)
        sh(f"tp Demo {ox + 7.5} {oy + 1.6} {oz - 6} 0 6", 1)        # retour dehors : l'ambulance dans sa baie
        time.sleep(1)
        pad.move(70, 0, steps=14)
        time.sleep(3)


def aeroport():
    """Aéroport civil de la métropole : hall d'enregistrement, sûreté, salle d'embarquement, aire de stationnement, puis décollage d'un avion de ligne."""
    import re
    sh("time set day")
    sh("gamerule doDaylightCycle false")
    camera(0)
    pad.release_all()
    sh("gamemode creative Demo")
    out = sh("locate structure modeminecraft:city_plains")
    m = re.search(r"\[(-?\d+), ~, (-?\d+)\]", out)
    if not m:
        raise SystemExit("ville introuvable : " + out)
    cx, cz = int(m.group(1)), int(m.group(2))
    tx, tz = cx + 160, cz + 16                       # terminal : 10 chunks à l'est, 1 au sud du point de la ville
    ax, az = cx + 112, cz + 64                       # aire de stationnement et piste : 7 chunks à l'est, 4 au sud
    sh(f"tp Demo {tx + 16} -60 {tz - 12} 0 6", 6)
    sh(f"tp Demo {ax + 40} -50 {az + 8} 0 10", 6)    # charge aussi l'aire de stationnement
    sh("ride Demo dismount")                           # au cas où une prise précédente aurait laissé le pilote à bord
    sh("kill @e[type=modeminecraft:regional_airliner,distance=..8]")
    sh(f"tp Demo {tx + 16} -60 {tz - 12} 0 6", 4)
    with Take("jeu_aeroport"):
        time.sleep(2)
        pad.down("w")                                # on marche vers l'entrée du terminal
        time.sleep(3.2)
        pad.up("w")
        sh(f"tp Demo {tx + 16} -60 {tz + 6} 0 6", 1)       # hall d'enregistrement
        time.sleep(1)
        pad.move(-60, 0, steps=12)
        time.sleep(1.2)
        pad.move(120, 0, steps=20)
        time.sleep(1.2)
        sh(f"tp Demo {tx + 15} -60 {tz + 12.5} 0 8", 1)    # contrôle de sûreté : portiques et scanners
        time.sleep(3)
        sh(f"tp Demo {tx + 16} -60 {tz + 19} 0 5", 1)      # salle d'embarquement, avions derrière les baies vitrées
        time.sleep(1)
        pad.move(70, 0, steps=14)
        time.sleep(1.5)
        pad.move(-140, 0, steps=24)
        time.sleep(1.5)
        sh(f"tp Demo {ax + 44} -60 {az + 4} 20 8", 1)      # sur l'aire de stationnement
        time.sleep(2)
        pad.move(120, 0, steps=20)
        time.sleep(2.5)
        sh(f"tp Demo {ax + 10} -58 {az + 39}", 2)          # sur la piste (chunks chargés), puis l'avion de ligne, cap à l'est
        sh(f"summon modeminecraft:regional_airliner {ax + 6} -61 {az + 39} {{Rotation:[270f,0f]}}", 2)
        sh("ride Demo mount @e[type=modeminecraft:regional_airliner,limit=1,sort=nearest]", 1.5)
        camera(1)
        time.sleep(1.5)
        pad.down("w")                                # plein gaz : roulage de décollage
        time.sleep(6.5)
        pad.move(0, -55, steps=12)                   # cabrer doucement
        time.sleep(4)
        pad.up("w")
    camera(0)
    pad.tap("Shift_L")
    sh("kill @e[type=modeminecraft:regional_airliner,distance=..8]")


def metro():
    """Métro de la métropole (monde à sol profond : voir tools/demo_env.sh) : bouche de métro, quai et rame, tunnel, porte blindée du bunker et abri."""
    import re
    sh("time set day")
    sh("gamerule doDaylightCycle false")
    camera(0)
    pad.release_all()
    sh("gamemode creative Demo")
    out = sh("locate structure modeminecraft:city_plains")
    m = re.search(r"\[(-?\d+), ~, (-?\d+)\]", out)
    if not m:
        raise SystemExit("ville introuvable : " + out)
    cx, cz = int(m.group(1)), int(m.group(2))             # point de la ville = coin du chunk central ; station au centre de l'avenue
    sh(f"tp Demo {cx + 1.5} 120 {cz + 10} 0 10", 6)
    oy = float(re.findall(r"(-?\d+\.\d+)d", sh("data get entity Demo Pos"))[1])     # niveau de la rue
    sh(f"tp Demo {cx + 5} {oy + 3} {cz + 6} 45 25", 4)
    with Take("jeu_metro"):
        time.sleep(2.5)
        pad.move(-60, 0, steps=12)                         # la bouche de métro sur le trottoir
        time.sleep(2)
        sh(f"tp Demo {cx + 1.5} {oy - 14} {cz + 13.5} 180 6", 1)   # en bas de l'échelle : couloir, portillons
        time.sleep(2.5)
        sh(f"tp Demo {cx + 12.5} {oy - 14} {cz + 6.5} 90 4", 1)    # le quai, la rame garée à gauche
        time.sleep(1)
        pad.move(60, 0, steps=14)
        time.sleep(2)
        pad.move(-120, 0, steps=24)
        time.sleep(2)
        sh(f"tp Demo {cx + 13.5} {oy - 15} {cz + 11.5} 90 3", 1)    # sur la voie sud : le tunnel
        time.sleep(3)
        sh(f"tp Demo {cx - 140} {oy - 15} {cz + 7.5} 90 3", 1)       # bout de ligne : la porte blindée du bunker
        time.sleep(3)
        sh(f"tp Demo {cx - 150} {oy - 15} {cz + 7.5} 270 6", 1)      # dans le bunker
        time.sleep(1)
        pad.move(-100, 0, steps=20)
        time.sleep(2.5)
        pad.move(200, 0, steps=30)
        time.sleep(2)


def largage():
    reset(0, -59, 0, 0, 0)
    sh("summon modeminecraft:airdrop 0 -20 8 {}")
    time.sleep(5)
    with Take("jeu_largage"):
        pad.move(0, -60, steps=8)
        time.sleep(7)
        pad.move(0, 60, steps=8)
        time.sleep(5)


SCENES = {"armes": armes, "melee": melee, "armures": armures, "vehicules": vehicules, "avion_leger": avion_leger, "porte_avions": porte_avions,
          "bots": bots, "explosifs": explosifs, "helicoptere": helicoptere, "navire": navire, "drone": drone, "largage": largage, "blessures": blessures, "hopital": hopital, "aeroport": aeroport, "metro": metro}

if __name__ == "__main__":
    r = L.Rcon()
    pad = L.Pad()
    from Xlib import X
    from Xlib.ext import xtest
    xtest.fake_input(pad.d, X.MotionNotify, False, x=640, y=360)
    pad.d.sync()
    os.makedirs(OUT, exist_ok=True)
    for name in sys.argv[1:] or list(SCENES):
        SCENES[name]()

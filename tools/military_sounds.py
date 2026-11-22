#!/usr/bin/env python3
"""
Sons de la mise à jour 0.4 (véhicules militaires, missiles), par synthèse : coup de canon de char, canon automatique,
départ de roquette et de missile, mitrailleuses de véhicule, boucles moteur diesel / chenilles / rotor d'hélicoptère,
grosses détonations. Utilisation : python3 tools/military_sounds.py (écrit les .ogg et complète sounds.json).
generate_sounds.py appelle aussi register() pour que sounds.json reste complet.
"""
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generate_sounds as gs  # noqa: E402

SR = gs.SR


def boom(seed, dur=3.0, f0=38, crack=1.0, tail=1.2):
    rng = np.random.default_rng(seed)
    t = gs.t_axis(dur)
    n = len(t)
    thump = np.sin(2 * np.pi * np.cumsum(f0 * (1 + 3 * np.exp(-t / 0.03))) / SR) * gs.env(t, 0.002, 0.35)
    body = gs.band(gs.noise(n, rng), lo=30, hi=900) * gs.env(t, 0.001, 0.25)
    crk = gs.band(gs.noise(n, rng), lo=1500, hi=8000) * gs.env(t, 0.0005, 0.012) * crack
    rumble = gs.band(gs.noise(n, rng), lo=20, hi=200) * gs.env(t, 0.05, tail)
    x = thump * 1.2 + body + crk + rumble * 0.8
    x = gs.echo(x, [0.35, 0.9, 1.6], [0.35, 0.2, 0.1])
    return gs.normalize(x, 0.95)


def autocannon(seed):
    return gs.gunshot(seed, crack=1.2, crack_decay=0.01, body_fc=1100, body_decay=0.12, thump_f=55, thump=1.3, tail=0.35,
                      tail_decay=0.5, duration=1.0)


def mg(seed, heavy):
    p = dict(crack=1.1, crack_decay=0.008, body_fc=1600 if heavy else 2400, body_decay=0.08 if heavy else 0.05,
             thump_f=60 if heavy else 85, thump=1.0 if heavy else 0.6, tail=0.2, tail_decay=0.3, duration=0.5, echoes=False)
    return gs.gunshot(seed, **p)


def launch(seed, dur, rise=False):
    rng = np.random.default_rng(seed)
    t = gs.t_axis(dur)
    n = len(t)
    pop = gs.band(gs.noise(n, rng), lo=100, hi=2000) * gs.env(t, 0.001, 0.06)
    roar = gs.band(gs.noise(n, rng), lo=200, hi=4000) * gs.env(t, 0.08, dur * 0.5)
    if rise:
        roar *= 0.6 + 0.4 * np.sin(np.pi * np.clip(t / dur, 0, 1))
    hiss = gs.band(gs.noise(n, rng), lo=3000, hi=9000) * gs.env(t, 0.05, dur * 0.3) * 0.4
    return gs.normalize(pop * 1.2 + roar + hiss)


def loop_engine(seed, f, dur=2.0, harsh=0.4, clank=0.0):
    """Boucle moteur parfaitement cyclique (fréquences à nombre entier de cycles)."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(dur)
    n = len(t)
    f = round(f * dur) / dur
    x = np.zeros(n)
    for h in range(1, 8):
        x += np.sin(2 * np.pi * f * h * t + rng.uniform(0, 6.28)) / h ** (1.2 - harsh * 0.5)
    rough = np.fft.irfft(np.fft.rfft(rng.normal(size=n)) * (np.fft.rfftfreq(n, 1 / SR) < 600), n)
    x = x * (1 + 0.3 * np.sin(2 * np.pi * (f / 4) * t)) + rough * harsh * 3
    if clank:
        # chenilles : cliquetis réguliers des patins
        k = int(SR / 9)
        for i in range(0, n, k):
            m = min(n - i, 900)
            x[i:i + m] += clank * np.exp(-np.arange(m) / 120) * rng.uniform(0.6, 1.0) * np.sin(np.arange(m) * 0.9)
    return gs.normalize(x, 0.7)


def loop_rotor(seed, dur=2.0, bpm_hz=8.0):
    rng = np.random.default_rng(seed)
    t = gs.t_axis(dur)
    n = len(t)
    f = round(bpm_hz * dur) / dur
    whop = np.maximum(0, np.sin(2 * np.pi * f * t)) ** 6
    air = np.fft.irfft(np.fft.rfft(rng.normal(size=n)) * ((np.fft.rfftfreq(n, 1 / SR) > 60) & (np.fft.rfftfreq(n, 1 / SR) < 900)), n)
    turbine = np.sin(2 * np.pi * round(1900 * dur) / dur * t) * 0.08 + np.sin(2 * np.pi * round(3800 * dur) / dur * t) * 0.04
    return gs.normalize(air * (0.3 + whop * 1.5) + turbine, 0.75)


def huge_explosion(seed):
    return boom(seed, dur=5.0, f0=25, crack=1.3, tail=2.5)


def alarm(seed):
    t = gs.t_axis(2.0)
    f = 600 + 500 * (np.sin(2 * np.pi * 2 * t) > 0)
    return gs.normalize(np.sign(np.sin(2 * np.pi * np.cumsum(f) / SR)) * 0.5 + np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.5, 0.6)


def radio_call(seed):
    """Appel radio : bip d'émission, voix hachée (formants modulés) noyée dans les grésillements, bip de fin."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(2.4)
    n = len(t)
    beep = np.sin(2 * np.pi * 1400 * t) * ((t < 0.12) | ((t > 2.25) & (t < 2.35)))
    syll = (np.sin(2 * np.pi * 4.5 * t) > -0.2) * ((t > 0.2) & (t < 2.15))
    f0 = 140 + 30 * np.sin(2 * np.pi * 1.3 * t)
    voice = np.sign(np.sin(2 * np.pi * np.cumsum(f0) / gs.SR)) * syll
    voice = gs.band(voice, lo=500, hi=2600)
    hiss = gs.band(gs.noise(n, rng), lo=1500, hi=6000) * (0.25 + 0.15 * rng.random(n))
    return gs.normalize(beep * 0.6 + voice * 0.9 + hiss * 0.5, 0.8)


def radio_cut(seed):
    rng = np.random.default_rng(seed)
    t = gs.t_axis(0.8)
    burst = gs.band(gs.noise(len(t), rng), lo=800, hi=7000) * gs.env(t, 0.005, 0.25)
    click = np.sin(2 * np.pi * 900 * t) * (t > 0.55) * (t < 0.6)
    return gs.normalize(burst + click * 0.7, 0.8)


def vault_open(seed):
    """Porte blindée : déverrouillage (clac), sifflement hydraulique, roulement lourd, butée."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(3.2)
    n = len(t)
    clack = gs.band(gs.noise(n, rng), lo=300, hi=4000) * gs.env(t, 0.001, 0.04)
    hiss = gs.band(gs.noise(n, rng), lo=2500, hi=9000) * np.clip((t - 0.3) / 0.3, 0, 1) * np.exp(-np.maximum(t - 1.2, 0) / 0.5)
    rumble = gs.band(gs.noise(n, rng), lo=30, hi=220) * ((t > 0.6) & (t < 2.8)) * 0.9
    thud = np.sin(2 * np.pi * 55 * t) * gs.env(np.maximum(t - 2.8, 0), 0.002, 0.18) * (t > 2.8)
    return gs.normalize(clack * 0.8 + hiss * 0.6 + rumble + thud * 1.2, 0.85)


def hydraulics(seed):
    """Vérins hydrauliques : pompe qui monte en régime, sifflement de fluide, cliquetis de verrouillage final."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(2.2)
    n = len(t)
    f = 90 + 40 * np.clip(t / 0.4, 0, 1)
    pump = np.sign(np.sin(2 * np.pi * np.cumsum(f) / SR)) * 0.25 + np.sin(2 * np.pi * np.cumsum(f * 2) / SR) * 0.3
    pump *= np.clip(t / 0.15, 0, 1) * np.clip((2.0 - t) / 0.2, 0, 1)
    whine = np.sin(2 * np.pi * np.cumsum(700 + 250 * np.clip(t / 1.8, 0, 1)) / SR) * 0.15 * (t < 2.0)
    hiss = gs.band(gs.noise(n, rng), lo=2000, hi=7000) * 0.25 * (t < 2.0)
    clunk = gs.band(gs.noise(n, rng), lo=80, hi=1200) * gs.env(np.maximum(t - 2.0, 0), 0.001, 0.06) * (t > 2.0)
    return gs.normalize(pump + whine + hiss + clunk * 1.4, 0.8)


def crash_alarm(seed):
    """Alarme de cockpit : bips aigus rapides et voix de synthèse grave (« pull up »)."""
    t = gs.t_axis(2.0)
    beep = np.sin(2 * np.pi * 2400 * t) * ((t * 8) % 1 < 0.45)
    f0 = 180 + 20 * np.sin(2 * np.pi * 3 * t)
    voice = gs.band(np.sign(np.sin(2 * np.pi * np.cumsum(f0) / SR)), lo=300, hi=2200) * ((t > 0.9) & (t < 1.7))
    return gs.normalize(beep * 0.6 + voice * 0.7, 0.8)


def bay_release(seed):
    """Largage : déverrouillage métallique lourd (crochets de soute) et choc sourd."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(0.9)
    n = len(t)
    clank = gs.band(gs.noise(n, rng), lo=400, hi=5000) * gs.env(t, 0.001, 0.05)
    ring = (np.sin(2 * np.pi * 820 * t) + 0.6 * np.sin(2 * np.pi * 1330 * t)) * np.exp(-t / 0.18) * 0.4
    thump = np.sin(2 * np.pi * 70 * t) * gs.env(t, 0.003, 0.12)
    return gs.normalize(clank + ring + thump * 1.2, 0.85)


def bay_doors(seed):
    """Trappes de soute : moteur électrique, grincement, butée."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(1.6)
    n = len(t)
    motor = np.sin(2 * np.pi * np.cumsum(180 + 60 * np.clip(t / 1.2, 0, 1)) / SR) * 0.3 * (t < 1.35)
    squeal = gs.band(gs.noise(n, rng), lo=1500, hi=3500) * 0.2 * (t < 1.35)
    thud = gs.band(gs.noise(n, rng), lo=60, hi=600) * gs.env(np.maximum(t - 1.35, 0), 0.002, 0.1) * (t > 1.35)
    return gs.normalize(motor + squeal + thud * 1.5, 0.8)


def whistle(seed):
    """Sifflement de bombe qui tombe : glissando descendant."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(3.0)
    f = 1500 - 900 * (t / 3.0)
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.clip(t / 0.4, 0, 1)
    air = gs.band(gs.noise(len(t), rng), lo=800, hi=4000) * 0.25
    return gs.normalize((tone + air) * np.clip((3.0 - t) / 0.2, 0, 1), 0.7)


def nuke(seed):
    """Détonation nucléaire : craquement initial, souffle énorme et long grondement."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(7.0)
    n = len(t)
    crack = gs.band(gs.noise(n, rng), lo=800, hi=9000) * gs.env(t, 0.0008, 0.06) * 1.4
    blast = gs.band(gs.noise(n, rng), lo=20, hi=500) * gs.env(t, 0.02, 2.5) * 1.6
    sub = np.sin(2 * np.pi * np.cumsum(22 + 30 * np.exp(-t / 0.6)) / SR) * gs.env(t, 0.01, 3.5)
    x = gs.echo(crack + blast + sub * 1.3, [0.6, 1.4, 2.6], [0.4, 0.25, 0.12])
    return gs.normalize(x, 0.98)


def rumble(seed):
    """Grondement sourd et assourdissant de l'onde de choc (arrive avec retard)."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(9.0)
    n = len(t)
    low = gs.band(gs.noise(n, rng), lo=15, hi=160) * gs.env(t, 0.4, 4.0) * 2
    roll = gs.band(gs.noise(n, rng), lo=80, hi=700) * gs.env(t, 0.2, 2.0) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.7 * t))
    return gs.normalize(low + roll, 0.95)


def geiger(seed):
    """Un clic de compteur Geiger (sec et métallique)."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(0.03)
    n = len(t)
    click = gs.band(gs.noise(n, rng), lo=2000, hi=9000) * np.exp(-t / 0.0025)
    return gs.normalize(click + np.sin(2 * np.pi * 3200 * t) * np.exp(-t / 0.002) * 0.5, 0.75)


def prop_loop(seed):
    """Quatre moteurs en étoile : ronflement grave cadencé, souffle des hélices (boucle parfaite)."""
    rng = np.random.default_rng(seed)
    dur = 2.0
    t = gs.t_axis(dur)
    n = len(t)
    x = np.zeros(n)
    for f0, ph in ((38, 0.0), (41, 1.3), (44, 2.1), (47, 0.7)):
        f = round(f0 * dur) / dur
        for h in range(1, 6):
            x += np.sin(2 * np.pi * f * h * t + ph * h) / h
    beat = 1 + 0.35 * np.maximum(0, np.sin(2 * np.pi * (round(18 * dur) / dur) * t))
    air = np.fft.irfft(np.fft.rfft(rng.normal(size=n)) * ((np.fft.rfftfreq(n, 1 / SR) > 80) & (np.fft.rfftfreq(n, 1 / SR) < 1200)), n)
    return gs.normalize(x * beat * 0.3 + air * 0.8, 0.75)


def jet_loop(seed):
    """Turboréacteur : sifflement de turbine aigu, grondement de la tuyère (boucle parfaite)."""
    rng = np.random.default_rng(seed)
    dur = 2.0
    t = gs.t_axis(dur)
    n = len(t)
    spec = np.fft.rfft(rng.normal(size=n))
    fr = np.fft.rfftfreq(n, 1 / SR)
    roar = np.fft.irfft(spec * ((fr > 30) & (fr < 700)) * (1 / (1 + fr / 200)), n)
    hiss = np.fft.irfft(spec * ((fr > 2500) & (fr < 7000)), n) * 0.25
    whine = sum(np.sin(2 * np.pi * (round(f * dur) / dur) * t) / k for k, f in enumerate((3150, 4720, 6300), 1)) * 0.06
    return gs.normalize(roar * 1.4 + hiss + whine, 0.8)


def afterburner(seed):
    """Allumage de la postcombustion : détonation sourde puis rugissement grave."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(1.6)
    n = len(t)
    thump = np.sin(2 * np.pi * 45 * t) * gs.env(t, 0.005, 0.25)
    roar = gs.band(gs.noise(n, rng), lo=25, hi=900) * gs.env(t, 0.05, 1.2) * 1.4
    return gs.normalize(thump * 1.3 + roar, 0.9)


def sonic_boom(seed):
    """Bang supersonique : double claquement (onde en N) et roulement."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(2.5)
    n = len(t)
    crack1 = gs.band(gs.noise(n, rng), lo=30, hi=4000) * gs.env(t, 0.001, 0.08) * 1.6
    t2 = np.clip(t - 0.11, 0, None)
    crack2 = gs.band(gs.noise(n, rng), lo=30, hi=3000) * gs.env(t2, 0.001, 0.09) * (t > 0.11) * 1.3
    roll = gs.band(gs.noise(n, rng), lo=20, hi=300) * gs.env(t, 0.05, 1.4)
    return gs.normalize(crack1 + crack2 + roll, 0.98)


def flare(seed):
    """Éjection de leurres : claquements pyrotechniques en salve et chuintement."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(1.2)
    n = len(t)
    x = np.zeros(n)
    for k in range(4):
        tk = np.clip(t - k * 0.12, 0, None)
        x += gs.band(gs.noise(n, rng), lo=600, hi=7000) * gs.env(tk, 0.001, 0.04) * (t > k * 0.12)
    fizz = gs.band(gs.noise(n, rng), lo=2000, hi=8000) * gs.env(t, 0.05, 0.9) * 0.3
    return gs.normalize(x + fizz, 0.8)


def missile_warning(seed):
    """Alerte missile du cockpit : tonalité alternée aiguë très rapide."""
    t = gs.t_axis(1.0)
    f = np.where((t * 10) % 1 < 0.5, 1800, 1200)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * ((t * 10) % 0.5 < 0.42)
    return gs.normalize(x, 0.65)


def ejection(seed):
    """Siège éjectable : charge pyrotechnique de la verrière, fusée du siège."""
    rng = np.random.default_rng(seed)
    t = gs.t_axis(2.2)
    n = len(t)
    bang = gs.band(gs.noise(n, rng), lo=60, hi=6000) * gs.env(t, 0.001, 0.07) * 1.5
    rocket = gs.band(gs.noise(n, rng), lo=200, hi=3000) * gs.env(np.clip(t - 0.08, 0, None), 0.02, 0.6) * (t > 0.08)
    return gs.normalize(bang + rocket, 0.9)


SOUNDS = {
    "mil.radio_dispatch_call": (lambda s: radio_call(s), 2, "radio_call"),
    "mil.radio_call_cut": (lambda s: radio_cut(s), 1, "radio_cut"),
    "mil.vault_door_open": (lambda s: vault_open(s), 1, "vault_door_open"),
    "mil.tank_cannon": (lambda s: boom(s), 2, "tank_cannon"),
    "mil.autocannon": (lambda s: autocannon(s), 2, "autocannon"),
    "mil.rocket_launch": (lambda s: launch(s, 1.4), 2, "rocket_launch"),
    "mil.missile_launch": (lambda s: launch(s, 2.6, True), 1, "missile_launch"),
    "mil.mg_light": (lambda s: mg(s, False), 2, "vehicle_mg"),
    "mil.mg_heavy": (lambda s: mg(s, True), 2, "vehicle_mg"),
    "mil.diesel_loop": (lambda s: loop_engine(s, 34, harsh=0.5), 1, "diesel"),
    "mil.tracks_loop": (lambda s: loop_engine(s, 28, harsh=0.6, clank=0.5), 1, "tracks"),
    "mil.turbine_loop": (lambda s: loop_engine(s, 110, harsh=0.2), 1, "turbine"),
    "mil.rotor_loop": (lambda s: loop_rotor(s), 1, "rotor"),
    "mil.huge_explosion": (lambda s: huge_explosion(s), 2, "huge_explosion"),
    "mil.air_raid_alarm": (lambda s: alarm(s), 1, "air_raid_alarm"),
    "mil.himars_hydraulics": (lambda s: hydraulics(s), 1, "himars_hydraulics"),
    "mil.aircraft_crash_alarm": (lambda s: crash_alarm(s), 1, "aircraft_crash_alarm"),
    "mil.bomb_bay_release": (lambda s: bay_release(s), 1, "bomb_bay_release"),
    "mil.bomb_bay_doors": (lambda s: bay_doors(s), 1, "bomb_bay_doors"),
    "mil.bomb_whistle": (lambda s: whistle(s), 1, "bomb_whistle"),
    "mil.nuclear_detonation": (lambda s: nuke(s), 1, "nuclear_detonation"),
    "mil.nuclear_detonation_rumble": (lambda s: rumble(s), 1, "nuclear_rumble"),
    "mil.geiger_clicks": (lambda s: geiger(s), 3, "geiger"),
    "mil.prop_engine_loop": (lambda s: prop_loop(s), 1, "prop_engine"),
    "mil.jet_engine_loop": (lambda s: jet_loop(s), 1, "jet_engine"),
    "mil.afterburner": (lambda s: afterburner(s), 1, "afterburner"),
    "mil.sonic_boom": (lambda s: sonic_boom(s), 2, "sonic_boom"),
    "mil.flare_launch": (lambda s: flare(s), 1, "flare_launch"),
    "mil.missile_warning": (lambda s: missile_warning(s), 1, "missile_warning"),
    "mil.ejection_seat": (lambda s: ejection(s), 1, "ejection_seat"),
}


def register(add):
    """Entrées de sounds.json (appelé par generate_sounds.py)."""
    for event, (_, n, sub) in SOUNDS.items():
        name = event.split(".")[1]
        add(event, [f"mil/{name}_{v}" for v in range(n)], sub)


def main():
    sounds_dir = os.path.join(gs.ASSETS, "sounds")
    seed = 9000
    for event, (fn, n, _) in SOUNDS.items():
        name = event.split(".")[1]
        for v in range(n):
            gs.write_ogg(os.path.join(sounds_dir, "mil", f"{name}_{v}.ogg"), fn(seed))
            seed += 1
    path = os.path.join(gs.ASSETS, "sounds.json")
    with open(path, encoding="utf-8") as f:
        events = json.load(f)

    def add(event, files, sub):
        events[event] = {"subtitle": f"subtitles.{gs.MOD}.{sub}", "sounds": [f"{gs.MOD}:{x}" for x in files]}
    register(add)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(events, f, indent=2)
        f.write("\n")
    print(len(SOUNDS), "sons")


if __name__ == "__main__":
    main()

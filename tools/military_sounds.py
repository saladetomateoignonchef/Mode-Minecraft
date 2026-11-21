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


SOUNDS = {
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

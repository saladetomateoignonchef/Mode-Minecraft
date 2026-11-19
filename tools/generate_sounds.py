#!/usr/bin/env python3
"""
Génère les sons des armes par synthèse (aucun enregistrement extérieur) :
tirs (par famille d'arme, 3 variantes chacun), tirs silencieux, écho lointain,
bruits mécaniques (chargeur, culasse, verrou, pompe), douilles, hit marker.

Écrit des fichiers .ogg (via ffmpeg) dans assets/modeminecraft/sounds/ et le fichier sounds.json.
Pour utiliser tes propres sons : remplace simplement les .ogg (même nom).

Utilisation (depuis la racine du projet) :
    python3 tools/generate_sounds.py
"""
import json
import os
import subprocess
import tempfile
import wave

import numpy as np

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
SR = 44100
MOD = "modeminecraft"


def t_axis(duration):
    return np.arange(int(SR * duration)) / SR


def noise(n, rng):
    return rng.uniform(-1, 1, n)


def band(x, lo=None, hi=None, tilt=0.0):
    """Filtre fréquentiel par FFT : passe-bande [lo, hi] (Hz) avec pentes douces."""
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    gain = np.ones_like(f)
    if lo:
        gain *= 1 / (1 + (lo / np.maximum(f, 1)) ** 4)
    if hi:
        gain *= 1 / (1 + (f / hi) ** 4)
    if tilt:
        gain *= (np.maximum(f, 20) / 1000) ** tilt
    return np.fft.irfft(spec * gain, len(x))


def env(t, attack, decay):
    a = np.clip(t / max(attack, 1e-5), 0, 1)
    return a * np.exp(-np.maximum(t - attack, 0) / decay)


def echo(x, delays, gains):
    out = x.copy()
    for d, g in zip(delays, gains):
        k = int(d * SR)
        if k < len(x):
            out[k:] += x[:-k] * g
    return out


def normalize(x, peak=0.9):
    x = np.tanh(x / (np.max(np.abs(x)) + 1e-9) * 1.4)
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def gunshot(seed, crack=1.0, crack_decay=0.006, body_fc=2500, body_decay=0.06, thump_f=90, thump=0.8,
            thump_decay=0.08, tail=0.25, tail_decay=0.5, duration=1.2, echoes=True):
    rng = np.random.default_rng(seed)
    t = t_axis(duration)
    n = len(t)
    c = band(noise(n, rng), lo=1500) * env(t, 0.0003, crack_decay) * crack
    b = band(noise(n, rng), lo=60, hi=body_fc) * env(t, 0.001, body_decay)
    sweep = thump_f * (1 + 1.5 * np.exp(-t / 0.01))
    th = np.sin(2 * np.pi * np.cumsum(sweep) / SR) * env(t, 0.001, thump_decay) * thump
    tl = band(noise(n, rng), lo=80, hi=1200) * env(t, 0.02, tail_decay) * tail
    x = c + b + th + tl
    if echoes:
        x = echo(x, [0.09 + rng.uniform(0, 0.03), 0.21 + rng.uniform(0, 0.05)], [0.25, 0.12])
    return normalize(x)


def suppressed(seed, heavy=False):
    rng = np.random.default_rng(seed)
    t = t_axis(0.35)
    n = len(t)
    puff = band(noise(n, rng), lo=150, hi=1800 if heavy else 2500) * env(t, 0.001, 0.035 if heavy else 0.025)
    click = band(noise(n, rng), lo=2500, hi=8000) * env(t, 0.0002, 0.004) * 0.7
    ring = np.sin(2 * np.pi * (2900 if heavy else 3400) * t) * env(t, 0.0005, 0.02) * 0.15
    action = np.zeros(n)
    k = int(0.03 * SR)  # la culasse cycle juste après
    action[k:] = band(noise(n - k, rng), lo=2000, hi=7000) * env(t[:n - k], 0.0002, 0.006) * 0.5
    return normalize(puff + click + ring + action, 0.7)


def click(rng, t, at, f=3000, decay=0.006, amp=1.0, lo=1500, hi=9000):
    n = len(t)
    x = np.zeros(n)
    k = int(at * SR)
    tt = t[:n - k]
    x[k:] = (band(noise(n - k, rng), lo=lo, hi=hi) * env(tt, 0.0002, decay)
             + np.sin(2 * np.pi * f * tt) * env(tt, 0.0003, decay * 3) * 0.4) * amp
    return x


def scrape(rng, t, at, length, amp=0.3, lo=1500, hi=6000):
    n = len(t)
    x = np.zeros(n)
    k, m = int(at * SR), int(length * SR)
    seg = band(noise(m, rng), lo=lo, hi=hi) * np.sin(np.linspace(0, np.pi, m)) * amp
    x[k:k + m] = seg[: n - k]
    return x


def mechanical(name, seed):
    rng = np.random.default_rng(seed)
    t = t_axis(0.6)
    if name == "mag_out":
        x = click(rng, t, 0.0, 2600, 0.008) + scrape(rng, t, 0.02, 0.15, 0.25) + click(rng, t, 0.2, 1800, 0.01, 0.4, 300, 3000)
    elif name == "mag_in":
        x = scrape(rng, t, 0.0, 0.1, 0.2) + click(rng, t, 0.11, 2200, 0.012, 1.2) + click(rng, t, 0.13, 3500, 0.005, 0.5)
    elif name == "rack":
        x = click(rng, t, 0.0, 2400, 0.008, 0.8) + scrape(rng, t, 0.01, 0.1, 0.2) + click(rng, t, 0.16, 1900, 0.014, 1.3)
    elif name == "bolt":
        x = (click(rng, t, 0.0, 2800, 0.006, 0.7) + scrape(rng, t, 0.05, 0.12, 0.3) + click(rng, t, 0.18, 2000, 0.01)
             + scrape(rng, t, 0.26, 0.1, 0.3) + click(rng, t, 0.38, 2400, 0.008, 0.9) + click(rng, t, 0.45, 3000, 0.006, 0.7))
    elif name == "pump":
        x = click(rng, t, 0.0, 1300, 0.02, 1.0, 400, 6000) + scrape(rng, t, 0.02, 0.1, 0.3, 500, 4000) \
            + click(rng, t, 0.16, 1100, 0.025, 1.3, 300, 5000)
    elif name == "shell_insert":
        x = scrape(rng, t, 0.0, 0.07, 0.25, 800, 4000) + click(rng, t, 0.08, 1500, 0.015, 1.0, 300, 4000)
    elif name == "empty_click":
        x = click(rng, t, 0.0, 3800, 0.004, 0.8)
    elif name == "hitmarker":
        t = t_axis(0.08)
        x = np.sin(2 * np.pi * 2800 * t) * env(t, 0.0005, 0.012) + band(noise(len(t), rng), lo=3000) * env(t, 0.0002, 0.003) * 0.5
    elif name == "casing":
        t = t_axis(0.7)
        x = np.zeros(len(t))
        for at, a in [(0.0, 1.0), (0.13, 0.6), (0.22, 0.35), (0.29, 0.2)]:
            k = int(at * SR)
            tt = t[: len(t) - k]
            ping = sum(np.sin(2 * np.pi * f * (1 + rng.uniform(-0.02, 0.02)) * tt) * w
                       for f, w in [(4300, 1.0), (6150, 0.6), (8400, 0.4), (11200, 0.25)])
            x[k:] += ping * env(tt, 0.0003, 0.05) * a
    elif name == "shell_casing":
        t = t_axis(0.5)
        x = click(rng, t, 0.0, 700, 0.03, 1.0, 150, 2500) + click(rng, t, 0.15, 650, 0.03, 0.5, 150, 2500)
    else:
        raise ValueError(name)
    return normalize(x, 0.6)


def distant(seed):
    rng = np.random.default_rng(seed)
    t = t_axis(1.6)
    x = band(noise(len(t), rng), lo=40, hi=400) * env(t, 0.01, 0.35)
    x = echo(x, [0.3, 0.7], [0.4, 0.2])
    return normalize(x, 0.8)


def rocket(seed):
    rng = np.random.default_rng(seed)
    t = t_axis(1.5)
    n = len(t)
    blast = gunshot(seed, crack=0.6, body_fc=1500, body_decay=0.08, thump_f=60, thump=1.0, duration=1.5)
    whoosh = band(noise(n, rng), lo=300, hi=3000) * env(t, 0.15, 0.5)
    return normalize(blast * 0.7 + whoosh * 0.8)


def launcher(seed):
    rng = np.random.default_rng(seed)
    t = t_axis(0.8)
    th = np.sin(2 * np.pi * np.cumsum(70 * (1 + 2 * np.exp(-t / 0.02))) / SR) * env(t, 0.001, 0.09)
    pop = band(noise(len(t), rng), lo=200, hi=2500) * env(t, 0.001, 0.03) * 0.8
    return normalize(th + pop + click(rng, t, 0.0, 1800, 0.01, 0.3))


# Familles de tirs : paramètres de synthèse
SHOTS = {
    "pistol": dict(crack=0.9, crack_decay=0.005, body_fc=3500, body_decay=0.045, thump_f=110, thump=0.5, tail=0.18, tail_decay=0.35),
    "magnum": dict(crack=1.0, crack_decay=0.007, body_fc=2500, body_decay=0.07, thump_f=85, thump=0.9, tail=0.3, tail_decay=0.55),
    "smg": dict(crack=0.8, crack_decay=0.004, body_fc=3800, body_decay=0.035, thump_f=120, thump=0.4, tail=0.15, tail_decay=0.25, duration=0.8),
    "rifle": dict(crack=1.1, crack_decay=0.006, body_fc=3000, body_decay=0.05, thump_f=95, thump=0.6, tail=0.25, tail_decay=0.45),
    "ak": dict(crack=1.0, crack_decay=0.007, body_fc=2200, body_decay=0.065, thump_f=80, thump=0.8, tail=0.3, tail_decay=0.5),
    "battle": dict(crack=1.1, crack_decay=0.008, body_fc=2000, body_decay=0.08, thump_f=75, thump=0.9, tail=0.35, tail_decay=0.6),
    "sniper": dict(crack=1.3, crack_decay=0.009, body_fc=1800, body_decay=0.1, thump_f=60, thump=1.0, tail=0.45, tail_decay=0.9, duration=1.8),
    "heavy": dict(crack=1.2, crack_decay=0.012, body_fc=1400, body_decay=0.13, thump_f=50, thump=1.2, tail=0.5, tail_decay=1.0, duration=2.0),
    "shotgun": dict(crack=0.9, crack_decay=0.01, body_fc=1800, body_decay=0.1, thump_f=70, thump=1.0, tail=0.35, tail_decay=0.6, duration=1.4),
    "minigun": dict(crack=0.7, crack_decay=0.003, body_fc=3000, body_decay=0.025, thump_f=100, thump=0.4, tail=0.05, tail_decay=0.1, duration=0.25, echoes=False),
}


def write_ogg(path, x):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = tmp.name
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    os.remove(wav_path)


def main():
    sounds_dir = os.path.join(ASSETS, "sounds")
    events = {}

    def add(event, files, subtitle):
        events[event] = {"subtitle": f"subtitles.{MOD}.{subtitle}", "sounds": [f"{MOD}:{f}" for f in files]}

    seed = 1
    for family, params in SHOTS.items():
        files = []
        for v in range(3):
            write_ogg(os.path.join(sounds_dir, "shot", f"{family}_{v}.ogg"), gunshot(seed, **params))
            files.append(f"shot/{family}_{v}")
            seed += 1
        add(f"shot.{family}", files, "shot")
    for heavy, name in [(False, "suppressed"), (True, "suppressed_heavy")]:
        files = []
        for v in range(3):
            write_ogg(os.path.join(sounds_dir, "shot", f"{name}_{v}.ogg"), suppressed(seed, heavy))
            files.append(f"shot/{name}_{v}")
            seed += 1
        add(f"shot.{name}", files, "shot_suppressed")
    write_ogg(os.path.join(sounds_dir, "shot", "distant_0.ogg"), distant(seed))
    write_ogg(os.path.join(sounds_dir, "shot", "distant_1.ogg"), distant(seed + 1))
    add("shot.distant", ["shot/distant_0", "shot/distant_1"], "shot_distant")
    write_ogg(os.path.join(sounds_dir, "shot", "rocket_0.ogg"), rocket(seed + 2))
    add("shot.rocket", ["shot/rocket_0"], "shot_rocket")
    write_ogg(os.path.join(sounds_dir, "shot", "launcher_0.ogg"), launcher(seed + 3))
    write_ogg(os.path.join(sounds_dir, "shot", "launcher_1.ogg"), launcher(seed + 4))
    add("shot.launcher", ["shot/launcher_0", "shot/launcher_1"], "shot_launcher")
    for name in ["mag_out", "mag_in", "rack", "bolt", "pump", "shell_insert", "empty_click", "casing", "shell_casing", "hitmarker"]:
        files = []
        for v in range(2 if name in ("casing", "mag_in", "rack") else 1):
            write_ogg(os.path.join(sounds_dir, "gun", f"{name}_{v}.ogg"), mechanical(name, seed))
            files.append(f"gun/{name}_{v}")
            seed += 1
        add(f"gun.{name}", files, name)

    with open(os.path.join(ASSETS, "sounds.json"), "w", encoding="utf-8") as f:
        json.dump(events, f, indent=2)
        f.write("\n")
    print(f"{len(events)} sons générés")


if __name__ == "__main__":
    main()

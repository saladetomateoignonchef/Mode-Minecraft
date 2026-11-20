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
    elif name == "headshot":
        # "Ding" métallique : partiels inharmoniques qui résonnent
        t = t_axis(0.6)
        x = sum(np.sin(2 * np.pi * f * t) * w * env(t, 0.0005, d) for f, w, d in
                [(2650, 1.0, 0.25), (5310, 0.5, 0.15), (7120, 0.3, 0.1), (3980, 0.4, 0.2)])
    elif name == "garand_ping":
        t = t_axis(0.9)
        x = sum(np.sin(2 * np.pi * f * t) * w * env(t, 0.0004, d) for f, w, d in
                [(3420, 1.0, 0.35), (6870, 0.45, 0.2), (9100, 0.25, 0.12), (1710, 0.3, 0.3)])
        x += click(rng, t, 0.0, 2500, 0.004, 0.6)
    elif name in ("lock_search", "lock_tone"):
        f = 1000 if name == "lock_search" else 1700
        t = t_axis(0.12)
        x = np.sign(np.sin(2 * np.pi * f * t)) * 0.4 * np.clip(t / 0.005, 0, 1) * np.clip((0.12 - t) / 0.01, 0, 1)
        x = band(x, hi=6000)
    elif name == "gear_equip_cloth":
        # tissu froissé puis boucle de ceinture
        t = t_axis(0.9)
        x = scrape(rng, t, 0.0, 0.25, 0.5, 800, 5000) + scrape(rng, t, 0.3, 0.2, 0.35, 800, 5000) \
            + click(rng, t, 0.62, 2600, 0.015, 0.9, 900, 8000) + click(rng, t, 0.66, 4200, 0.006, 0.5, 2000, 10000)
    elif name == "boots_lace":
        # lacets tirés d'un coup sec, deux fois
        t = t_axis(0.8)
        x = scrape(rng, t, 0.05, 0.18, 0.6, 2500, 9000) + scrape(rng, t, 0.42, 0.2, 0.6, 2500, 9000) \
            + click(rng, t, 0.24, 1800, 0.01, 0.5, 600, 6000) + click(rng, t, 0.63, 1800, 0.01, 0.5, 600, 6000)
    elif name == "ranger_step":
        # semelle lourde sur le gravier : choc sourd + crissement
        t = t_axis(0.35)
        x = click(rng, t, 0.0, 220, 0.04, 1.0, 60, 1200) + scrape(rng, t, 0.01, 0.12, 0.35, 1500, 7000)
    elif name == "vest_equip_velcro":
        # fermeture éclair (petits clics rapides) puis velcros arrachés / plaqués
        t = t_axis(1.1)
        x = np.zeros(len(t))
        for i in range(18):
            x = x + click(rng, t, 0.02 + i * 0.016, 4500, 0.003, 0.35, 2500, 10000)
        x = x + scrape(rng, t, 0.45, 0.22, 0.9, 1500, 9000) + scrape(rng, t, 0.78, 0.18, 0.8, 1500, 9000)
    elif name == "plate_insert":
        t = t_axis(0.7)
        x = scrape(rng, t, 0.0, 0.3, 0.35, 400, 3000) + click(rng, t, 0.34, 600, 0.06, 1.0, 100, 2500) \
            + click(rng, t, 0.36, 2200, 0.01, 0.4, 800, 6000)
    elif name == "vest_bullet_hit":
        # impact sourd sur kevlar / céramique (pas de bruit de chair)
        t = t_axis(0.4)
        x = click(rng, t, 0.0, 350, 0.05, 1.0, 60, 1800) + np.sin(2 * np.pi * 120 * t) * env(t, 0.001, 0.06) * 0.8
    elif name == "plate_shatter":
        t = t_axis(1.0)
        x = click(rng, t, 0.0, 2800, 0.02, 1.0, 900, 10000)
        for i in range(10):
            x = x + click(rng, t, 0.04 + i * 0.05 + rng.uniform(0, 0.03), rng.uniform(3000, 7000), 0.01, 0.5 - i * 0.04, 1500, 11000)
    elif name == "helmet_strap_click":
        # boucle de jugulaire qui se ferme : clic sec + petit frottement de sangle
        t = t_axis(0.3)
        x = scrape(rng, t, 0.0, 0.08, 0.2, 1200, 5000) + click(rng, t, 0.09, 3800, 0.006, 1.0, 1500, 9000) \
            + click(rng, t, 0.11, 2200, 0.01, 0.5, 800, 6000)
    elif name == "helmet_ricochet":
        # balle qui ricoche sur l'acier : impact lourd + sifflement descendant
        t = t_axis(0.9)
        x = click(rng, t, 0.0, 1600, 0.03, 1.0, 300, 7000) + np.sin(2 * np.pi * 220 * t) * env(t, 0.001, 0.08) * 0.6
        f = 3400 - 1800 * t
        x = x + np.sin(2 * np.pi * np.cumsum(f) / SR) * env(t - 0.02, 0.01, 0.25) * (t > 0.02) * 0.45
    elif name == "helmet_visor":
        t = t_axis(0.55)
        x = scrape(rng, t, 0.0, 0.3, 0.3, 600, 3500) + click(rng, t, 0.32, 900, 0.05, 1.0, 200, 4000)
    elif name == "nvg_on":
        # électronique : clic puis sifflement aigu qui monte
        t = t_axis(1.0)
        f = 6000 + 4000 * np.clip(t / 0.6, 0, 1)
        x = click(rng, t, 0.0, 3000, 0.008, 0.8, 1000, 9000) + np.sin(2 * np.pi * np.cumsum(f) / SR) * env(t - 0.05, 0.1, 0.5) * (t > 0.05) * 0.25
    elif name == "helmet_breath":
        # respiration étouffée sous la visière : souffle grave filtré (inspiration puis expiration)
        t = t_axis(2.2)
        n = band(noise(len(t), rng), 150, 900)
        x = n * (np.exp(-((t - 0.5) / 0.28) ** 2) * 0.6 + np.exp(-((t - 1.45) / 0.35) ** 2))
    elif name == "turret_deploy":
        # trépied qui s'ouvre puis se verrouille au sol : chocs métalliques lourds
        t = t_axis(0.95)
        x = scrape(rng, t, 0.0, 0.35, 0.25, 800, 4000)
        for at, f, a in [(0.05, 900, 0.6), (0.35, 700, 0.8), (0.62, 500, 1.0), (0.7, 1400, 0.5)]:
            x = x + click(rng, t, at, f, 0.05, a, 150, 3000)
        x = x + np.sin(2 * np.pi * 95 * t) * env(t - 0.62, 0.002, 0.12) * (t > 0.62) * 0.7
    elif name == "turret_reload":
        # capot qui s'ouvre, bande engagée (cliquetis), capot claqué
        t = t_axis(1.5)
        x = click(rng, t, 0.02, 1800, 0.03, 0.8, 300, 5000) + scrape(rng, t, 0.05, 0.12, 0.2, 2000, 7000)
        for i in range(10):
            x = x + click(rng, t, 0.3 + i * 0.07 + rng.uniform(-0.01, 0.01), 3500, 0.008, 0.45, 1500, 9000)
        x = x + click(rng, t, 1.2, 1100, 0.06, 1.0, 150, 4000) + click(rng, t, 1.32, 2600, 0.02, 0.6, 800, 8000)
    elif name == "dry_fire":
        t = t_axis(0.25)
        x = click(rng, t, 0.0, 2600, 0.012, 1.0, 600, 8000) + click(rng, t, 0.05, 1600, 0.02, 0.5, 300, 4000)
    elif name == "grenade_pin":
        t = t_axis(0.5)
        x = click(rng, t, 0.0, 3200, 0.006, 1.0) + scrape(rng, t, 0.03, 0.08, 0.3, 3000, 9000) \
            + sum(np.sin(2 * np.pi * f * t) * w * env(t, 0.06, 0.12) for f, w in [(5200, 0.3), (7800, 0.2)])
    elif name == "grenade_bounce":
        t = t_axis(0.35)
        x = click(rng, t, 0.0, 1800, 0.02, 1.0, 400, 5000) + np.sin(2 * np.pi * 2400 * t) * env(t, 0.0005, 0.05) * 0.4
    elif name == "grenade_beep":
        t = t_axis(0.1)
        x = np.sin(2 * np.pi * 2200 * t) * np.clip(t / 0.004, 0, 1) * np.clip((0.1 - t) / 0.01, 0, 1)
    elif name == "sonar":
        t = t_axis(1.0)
        x = np.sin(2 * np.pi * (1500 - 300 * t) * t) * env(t, 0.005, 0.35)
        x = echo(x, [0.25, 0.5], [0.35, 0.15])
    elif name == "flash_ring":
        t = t_axis(3.0)
        x = np.sin(2 * np.pi * 4100 * t) * (0.8 + 0.2 * np.sin(2 * np.pi * 6 * t)) * np.exp(-t / 1.4) * np.clip(t / 0.05, 0, 1)
    elif name == "flashlight_switch_click":
        # interrupteur de lampe tactique : clic sec à l'appui, plus léger au relâchement
        t = t_axis(0.18)
        x = click(rng, t, 0.0, 3800, 0.003, 1.0, 2500, 11000) + click(rng, t, 0.07, 3200, 0.002, 0.45, 2500, 11000)
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


def bot_sound(name, seed):
    """Sons des bots et des Faucons Blancs."""
    rng = np.random.default_rng(seed)
    if name == "police_freeze_warning":
        # radio : grésillement, deux tons de sirène, voix filtrée (formants) puis fin de transmission
        t = t_axis(1.6)
        x = band(noise(len(t), rng), lo=1500, hi=5000) * env(t, 0.002, 0.08) * 0.5
        for k, (at, f) in enumerate([(0.12, 960), (0.3, 1280), (0.48, 960), (0.66, 1280)]):
            m = (t >= at) & (t < at + 0.16)
            x[m] += np.sign(np.sin(2 * np.pi * f * t[m])) * 0.35
        m = (t >= 0.9) & (t < 1.4)
        tt = t[m] - 0.9
        voice = (np.sin(2 * np.pi * 140 * tt) + 0.6 * np.sin(2 * np.pi * 700 * tt) + 0.4 * np.sin(2 * np.pi * 1150 * tt))
        voice *= (0.6 + 0.4 * np.sin(2 * np.pi * 7 * tt)) * np.clip(tt / 0.02, 0, 1) * np.clip((0.5 - tt) / 0.05, 0, 1)
        x[m] += band(voice, lo=300, hi=3400) * 0.7
        x += band(noise(len(t), rng), lo=2000, hi=6000) * ((t > 1.42) & (t < 1.5)) * 0.6
        return normalize(x, 0.8)
    if name == "falcon_horn_charge":
        # cor de guerre : son cuivré grave qui enfle, avec vibrato, puis deux appels plus aigus
        t = t_axis(3.2)
        x = np.zeros(len(t))
        for at, dur, f in [(0.0, 1.5, 155), (1.6, 0.6, 207), (2.3, 0.85, 207)]:
            m = (t >= at) & (t < at + dur)
            tt = t[m] - at
            fv = f * (1 + 0.012 * np.sin(2 * np.pi * 5.5 * tt))
            ph = 2 * np.pi * np.cumsum(fv) / SR
            tone = sum(np.sin(k * ph) / k ** 0.8 for k in range(1, 9))
            x[m] += tone * np.clip(tt / 0.25, 0, 1) * np.clip((dur - tt) / 0.12, 0, 1)
        x = echo(x, [0.18, 0.37], [0.3, 0.15])
        return normalize(x, 0.85)
    if name == "dragonslayer_unsheathe":
        t = t_axis(0.9)
        scr = band(noise(len(t), rng), lo=2500, hi=9000) * np.clip(t / 0.05, 0, 1) * np.clip((0.7 - t) / 0.1, 0, 1)
        ring = sum(np.sin(2 * np.pi * f * t) * w for f, w in [(820, 1), (1730, 0.5), (2900, 0.3)]) * env(t - 0.68, 0.001, 0.25) * (t > 0.68)
        return normalize(scr * 0.7 * (0.6 + 0.4 * t) + ring * 0.6, 0.8)
    if name == "dragonslayer_sheathe":
        t = t_axis(0.8)
        scr = band(noise(len(t), rng), lo=2000, hi=8000) * np.clip(t / 0.03, 0, 1) * np.clip((0.55 - t) / 0.08, 0, 1) * (1 - 0.5 * t)
        clunk = click(rng, t, 0.56, 300, 0.04, 1.4, 80, 1500)
        return normalize(scr * 0.6 + clunk, 0.8)
    if name == "dragonslayer_clang":
        t = t_axis(1.0)
        whoosh = band(noise(len(t), rng), lo=200, hi=1800) * np.sin(np.clip(t / 0.25, 0, 1) * np.pi) * 0.9
        ring = sum(np.sin(2 * np.pi * f * t) * w for f, w in [(410, 1), (980, 0.6), (1650, 0.35), (2470, 0.2)]) * env(t - 0.2, 0.001, 0.35) * (t > 0.2)
        return normalize(whoosh + ring * 0.8 + click(rng, t, 0.2, 600, 0.02, 1.2, 100, 4000), 0.85)
    if name == "berserk_roar_clang":
        # cri de guerre grave et métallique : grondement guttural + résonance d'armure
        t = t_axis(2.4)
        f0 = 70 + 25 * np.sin(np.clip(t / 2.2, 0, 1) * np.pi)
        ph = 2 * np.pi * np.cumsum(f0 * (1 + 0.04 * np.sin(2 * np.pi * 11 * t))) / SR
        growl = sum(np.sin(k * ph) / k for k in range(1, 12)) * (0.7 + 0.3 * band(noise(len(t), rng), lo=20, hi=60) * 8)
        growl = band(growl, lo=50, hi=2500) * np.clip(t / 0.15, 0, 1) * np.clip((2.3 - t) / 0.5, 0, 1)
        ring = sum(np.sin(2 * np.pi * f * t) * w for f, w in [(310, 1), (745, 0.6), (1290, 0.4), (2050, 0.25)]) * env(t, 0.002, 0.9)
        x = echo(growl * 0.9 + ring * 0.5 + click(rng, t, 0.0, 500, 0.03, 1.4, 80, 3000), [0.21, 0.45], [0.35, 0.18])
        return normalize(x, 0.9)
    if name == "transformer_short_circuit":
        # arc électrique : bourdonnement 50 Hz, crépitements, puis détonation sourde
        t = t_axis(2.2)
        hum = (np.sign(np.sin(2 * np.pi * 50 * t)) * 0.3 + np.sin(2 * np.pi * 100 * t) * 0.4) * np.clip((0.9 - t) / 0.2, 0, 1) * 0.5
        crackle = np.zeros(len(t))
        for _ in range(60):
            at = rng.uniform(0.05, 1.6)
            crackle += click(rng, t, at, rng.uniform(1500, 6000), 0.004, rng.uniform(0.3, 1.0), 800, 9000)
        boom = band(noise(len(t), rng), lo=30, hi=500) * env(t - 0.9, 0.002, 0.35) * (t > 0.9) * 1.6
        return normalize(hum + crackle * 0.6 + boom, 0.9)
    if name == "drone_paired":
        t = t_axis(0.5)
        x = np.zeros(len(t))
        for at, f in [(0.0, 1760), (0.14, 2350)]:
            m = (t >= at) & (t < at + 0.1)
            x[m] += np.sin(2 * np.pi * f * t[m]) * 0.8
        return normalize(x, 0.6)
    if name == "drone_motor":
        # bourdonnement d'hélices (boucle de 1 s) : fréquence de pale + harmoniques + souffle
        t = t_axis(1.0)
        f = 190
        ph = 2 * np.pi * f * t
        tone = sum(np.sin(k * ph + rng.uniform(0, 6)) / k ** 1.2 for k in range(1, 10))
        tone *= 0.8 + 0.2 * np.sin(2 * np.pi * 4 * t)
        x = tone * 0.6 + band(noise(len(t), rng), lo=300, hi=5000) * 0.35
        return normalize(x, 0.6)
    if name == "fpv_incoming_buzz":
        # moteur de drone de course qui approche : aigu, nerveux, effet Doppler montant
        t = t_axis(1.6)
        f = 380 + 260 * (t / 1.6) ** 2
        ph = 2 * np.pi * np.cumsum(f) / SR
        tone = sum(np.sign(np.sin(k * ph)) * 0.4 / k + np.sin(k * ph) / k for k in range(1, 7))
        x = tone * np.clip(t / 1.2, 0.05, 1) ** 1.5 + band(noise(len(t), rng), lo=1500, hi=7000) * 0.2
        return normalize(x, 0.75)
    if name == "emp_jammer":
        # impulsion électromagnétique : montée de charge aiguë puis souffle grave et grésillement
        t = t_axis(1.2)
        f = 600 + 3000 * np.clip(t / 0.35, 0, 1) ** 2
        charge = np.sin(2 * np.pi * np.cumsum(f) / SR) * (t < 0.35) * 0.5
        pulse = band(noise(len(t), rng), lo=60, hi=900) * env(t - 0.35, 0.001, 0.25) * (t > 0.35) * 1.2
        fizz = band(noise(len(t), rng), lo=3000, hi=9000) * env(t - 0.35, 0.01, 0.5) * (t > 0.35) * 0.4
        return normalize(charge + pulse + fizz, 0.85)
    if name == "drone_jammed":
        t = t_axis(0.8)
        x = band(noise(len(t), rng), lo=800, hi=6000) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 13 * t))) * env(t, 0.01, 0.6)
        return normalize(x, 0.6)
    raise ValueError(name)


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


BOT_SOUNDS = ["police_freeze_warning", "falcon_horn_charge", "dragonslayer_unsheathe", "dragonslayer_sheathe", "dragonslayer_clang",
              "berserk_roar_clang", "transformer_short_circuit", "drone_paired", "drone_motor", "fpv_incoming_buzz", "emp_jammer",
              "drone_jammed"]


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
    for name in ["mag_out", "mag_in", "rack", "bolt", "pump", "shell_insert", "empty_click", "casing", "shell_casing", "hitmarker", "headshot", "garand_ping", "lock_search", "lock_tone",
                 "grenade_pin", "grenade_bounce", "grenade_beep", "sonar", "flash_ring", "turret_deploy", "turret_reload", "dry_fire",
                 "helmet_strap_click", "helmet_ricochet", "helmet_visor", "nvg_on", "helmet_breath",
                 "vest_equip_velcro", "plate_insert", "vest_bullet_hit", "plate_shatter",
                 "gear_equip_cloth", "boots_lace", "ranger_step", "flashlight_switch_click"]:
        files = []
        for v in range(2 if name in ("casing", "mag_in", "rack") else 1):
            write_ogg(os.path.join(sounds_dir, "gun", f"{name}_{v}.ogg"), mechanical(name, seed))
            files.append(f"gun/{name}_{v}")
            seed += 1
        add(f"gun.{name}", files, name)

    for name in BOT_SOUNDS:
        write_ogg(os.path.join(sounds_dir, "bot", f"{name}.ogg"), bot_sound(name, seed))
        seed += 1
        add(f"bot.{name}", [f"bot/{name}"], name)

    with open(os.path.join(ASSETS, "sounds.json"), "w", encoding="utf-8") as f:
        json.dump(events, f, indent=2)
        f.write("\n")
    print(f"{len(events)} sons générés")


if __name__ == "__main__":
    main()

"""
Sons du réalisme naturel (lot 1), par synthèse : craquement de glace (3 variantes) et ruissellement d'un ruisseau de montagne (boucle de 4 s).
Écrit assets/modeminecraft/sounds/nature/*.ogg et ajoute les entrées « nature.* » de sounds.json.
"""
import json
import os
import subprocess
import wave

import numpy as np

SR = 44100
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "modeminecraft")


def band(x, lo=None, hi=None):
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    g = np.ones_like(f)
    if lo:
        g *= 1 / (1 + (lo / np.maximum(f, 1)) ** 4)
    if hi:
        g *= 1 / (1 + (f / hi) ** 4)
    return np.fft.irfft(spec * g, len(x))


def norm(x, peak=0.85):
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def ice_crack(seed):
    """Craquement sec : quelques claquements cristallins très brefs (bruit filtré 2-9 kHz) suivis de petites résonances aiguës."""
    rng = np.random.default_rng(seed)
    n = int(SR * 0.55)
    out = np.zeros(n)
    t = np.arange(n) / SR
    at = [0.0] + sorted(rng.uniform(0.03, 0.3, 3 + seed % 2))
    for k, a in enumerate(at):
        i = int(a * SR)
        ln = int(SR * rng.uniform(0.012, 0.04))
        burst = band(rng.standard_normal(ln), 2500, 9000) * np.exp(-np.arange(ln) / SR / rng.uniform(0.003, 0.009))
        out[i:i + ln] += burst * (1.0 if k == 0 else rng.uniform(0.3, 0.7))
        f = rng.uniform(3200, 6800)
        ring = np.sin(2 * np.pi * f * t[:int(SR * 0.12)]) * np.exp(-t[:int(SR * 0.12)] / 0.018) * 0.18
        out[i:i + len(ring)] += ring[:max(0, n - i)][:len(ring)] if i + len(ring) <= n else ring[:n - i]
    out += band(rng.standard_normal(n), 120, 600) * np.exp(-t / 0.04) * 0.25       # petit « tock » grave de la glace qui travaille
    return norm(out, 0.8)


def stream():
    """Ruissellement : bruit filtré 500-5000 Hz modulé par des bouillonnements lents ; boucle parfaitement cyclique (filtrage par FFT)."""
    rng = np.random.default_rng(7)
    n = SR * 4
    t = np.arange(n) / SR
    base = band(rng.standard_normal(n), 600, 4500)
    hiss = band(rng.standard_normal(n), 3000, 9000) * 0.35
    mod = 0.65 + 0.35 * np.sin(2 * np.pi * 3 * t + 1.0) * np.sin(2 * np.pi * 5 * t + 0.4) + 0.2 * np.sin(2 * np.pi * 11 * t)
    gurgle = np.zeros(n)
    for _ in range(40):
        i = int(rng.uniform(0, n - SR * 0.12))
        f0, f1 = rng.uniform(500, 900), rng.uniform(1200, 2400)
        d = int(SR * rng.uniform(0.04, 0.1))
        tt = np.arange(d) / SR
        gurgle[i:i + d] += np.sin(2 * np.pi * (f0 + (f1 - f0) * tt / (d / SR) / 2) * tt) * np.exp(-tt / 0.03) * rng.uniform(0.05, 0.14)
    x = (base * mod + hiss + gurgle)
    # raccord de boucle : fondu enchaîné des 0,1 dernières secondes avec le début
    fade = int(SR * 0.1)
    x[:fade] = x[:fade] * np.linspace(0, 1, fade) + x[-fade:] * np.linspace(1, 0, fade)
    return norm(x[:-fade], 0.6)


def surf():
    """Ressac : une vague qui monte, déferle et se retire (bruit filtré 150-3500 Hz avec crête d'écume plus aiguë), 5 s."""
    rng = np.random.default_rng(3)
    n = SR * 5
    t = np.arange(n) / SR
    swell = np.sin(np.pi * np.clip(t / 4.6, 0, 1)) ** 2.0
    body = band(rng.standard_normal(n), 150, 3500) * swell
    foam = band(rng.standard_normal(n), 2500, 9000) * np.clip(np.sin(np.pi * np.clip((t - 1.2) / 2.8, 0, 1)), 0, 1) ** 2 * 0.45
    return norm(body + foam, 0.7)


def write_ogg(path, x):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    wav = path[:-4] + ".wav"
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    os.remove(wav)


def main():
    d = os.path.join(ROOT, "sounds", "nature")
    for v in range(3):
        write_ogg(os.path.join(d, f"ice_crack_{v}.ogg"), ice_crack(11 + v))
    write_ogg(os.path.join(d, "stream_flow.ogg"), stream())
    write_ogg(os.path.join(d, "surf.ogg"), surf())
    p = os.path.join(ROOT, "sounds.json")
    data = json.load(open(p, encoding="utf-8"))
    data["nature.ice_crack"] = {"subtitle": "subtitles.modeminecraft.ice_crack", "sounds": [f"modeminecraft:nature/ice_crack_{v}" for v in range(3)]}
    data["nature.surf"] = {"subtitle": "subtitles.modeminecraft.surf", "sounds": ["modeminecraft:nature/surf"]}
    data["nature.stream_flow"] = {"subtitle": "subtitles.modeminecraft.stream_flow", "sounds": ["modeminecraft:nature/stream_flow"]}
    json.dump(data, open(p, "w", encoding="utf-8"), indent=2, ensure_ascii=False)
    open(p, "a").write("\n")
    for lang, name, subs in (("fr_fr", "fr", ("Craquement de la glace", "Ruissellement d'un ruisseau", "Ressac")), ("en_us", "en", ("Ice cracking", "Stream trickling", "Waves breaking"))):
        lp = os.path.join(ROOT, "lang", f"{lang}.json")
        ld = json.load(open(lp, encoding="utf-8"))
        ld["subtitles.modeminecraft.ice_crack"] = subs[0]
        ld["subtitles.modeminecraft.stream_flow"] = subs[1]
        ld["subtitles.modeminecraft.surf"] = subs[2]
        json.dump(ld, open(lp, "w", encoding="utf-8"), indent=2, ensure_ascii=False)
        open(lp, "a").write("\n")
    print("sons de nature écrits")


if __name__ == "__main__":
    main()

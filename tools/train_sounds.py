"""
Sons du train des villes du Mesa : sifflet à vapeur (deux notes qui montent, queue de vapeur) et arc électrique de la
matraque des contrôleurs. Écrit les .ogg dans assets/modeminecraft/sounds/train/ (via ffmpeg).
Utilisation : python3 tools/train_sounds.py
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../src/main/resources/assets/modeminecraft/sounds/train")


def write_ogg(path, x):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    x = np.clip(x / (np.max(np.abs(x)) + 1e-9) * 0.85, -1, 1)
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, "s.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((x * 32767).astype("<i2").tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", path], check=True)


def whistle():
    rng = np.random.default_rng(7)
    d = 2.2
    t = np.arange(int(SR * d)) / SR
    env = np.clip(t / 0.08, 0, 1) * np.clip((d - t) / 0.7, 0, 1) * (0.85 + 0.15 * np.sin(2 * np.pi * 5.5 * t))
    f1 = 392 * (1 + 0.03 * np.clip(t / 0.3, 0, 1))                       # sol, qui monte un peu
    f2 = 494 * (1 + 0.03 * np.clip(t / 0.3, 0, 1))                       # si : accord de deux tuyaux
    x = np.zeros_like(t)
    for k, a in ((1, 1.0), (2, 0.5), (3, 0.3), (4, 0.15)):
        x += a * (np.sin(2 * np.pi * f1 * k * t) + 0.9 * np.sin(2 * np.pi * f2 * k * t))
    steam = rng.uniform(-1, 1, len(t))
    spec = np.fft.rfft(steam)
    f = np.fft.rfftfreq(len(t), 1 / SR)
    spec *= 1 / (1 + (1800 / np.maximum(f, 1)) ** 4)
    steam = np.fft.irfft(spec, len(t))
    return (x / 6 + steam * 0.5) * env


def zap():
    rng = np.random.default_rng(11)
    d = 0.7
    t = np.arange(int(SR * d)) / SR
    noise = rng.uniform(-1, 1, len(t))
    crackle = np.where(rng.uniform(0, 1, len(t)) > 0.985, noise, 0) * np.exp(-t / 0.25)
    buzz = np.sign(np.sin(2 * np.pi * 110 * t)) * np.sin(2 * np.pi * 3100 * t) * np.exp(-t / 0.18)
    arc = noise * np.exp(-t / 0.05) * 0.9
    return crackle * 1.4 + buzz * 0.5 + arc


if __name__ == "__main__":
    write_ogg(os.path.join(OUT, "train_whistle.ogg"), whistle())
    write_ogg(os.path.join(OUT, "electric_zap.ogg"), zap())
    print("ok")

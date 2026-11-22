"""
Sons de la mise à jour 0.3 (police, prison, ville vivante), par synthèse. Les boucles d'ambiance sont parfaitement
cycliques : bruits filtrés par FFT (circulaires), fréquences à nombre entier de cycles, événements repliés en fin de
boucle sur le début.
"""
import numpy as np

SR = 44100


def t_axis(d):
    return np.arange(int(SR * d)) / SR


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
    x = np.tanh(x / (np.max(np.abs(x)) + 1e-9) * 1.3)
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def env(t, a, d):
    return np.clip(t / max(a, 1e-5), 0, 1) * np.exp(-np.maximum(t - a, 0) / d)


def add_wrap(buf, sig, at):
    """Ajoute `sig` à partir de l'échantillon `at`, en repliant ce qui dépasse sur le début (boucle sans couture)."""
    n = len(buf)
    for k in range(0, len(sig), n):
        part = sig[k:k + n]
        idx = (at + k + np.arange(len(part))) % n
        np.add.at(buf, idx, part)


def tone(f, d, harm=6, decay=None, shape=1.0, phase=0.0):
    t = t_axis(d)
    x = sum(np.sin(2 * np.pi * f * k * t + phase * k) / k ** shape for k in range(1, harm + 1))
    if decay:
        x *= np.exp(-t / decay)
    return x


def formant_voice(rng, f0_curve, dur, formants=(800, 1300, 2800), breath=0.15):
    t = t_axis(dur)
    ph = 2 * np.pi * np.cumsum(f0_curve(t)) / SR
    x = np.zeros(len(t))
    f0 = f0_curve(t)
    for k in range(1, 30):
        fk = f0 * k
        g = sum(np.exp(-((fk - F) / (F * 0.12)) ** 2) for F in formants) + 0.02
        x += np.sin(k * ph) * g / k ** 0.4
    x += band(rng.uniform(-1, 1, len(t)), 500, 5000) * breath
    return x


def city_sound(name, seed):
    rng = np.random.default_rng(seed)
    if name == "police_siren":
        t = t_axis(2.0)
        f = 700 + 800 * (0.5 - 0.5 * np.cos(2 * np.pi * t / 2.0))
        ph = 2 * np.pi * np.cumsum(f) / SR
        x = np.sin(ph) + 0.35 * np.sin(2 * ph) + 0.2 * np.sign(np.sin(ph)) * 0.5
        return norm(x, 0.8)
    if name == "crowbar_pry":
        t = t_axis(0.9)
        groan = np.sin(2 * np.pi * np.cumsum(180 + 60 * np.sin(2 * np.pi * 3 * t)) / SR) * 0.5
        creak = band(rng.uniform(-1, 1, len(t)), 700, 3200) * (0.4 + 0.6 * (np.sin(2 * np.pi * 23 * t) > 0.6))
        x = (groan + creak) * np.clip(t / 0.08, 0, 1) * np.clip((0.9 - t) / 0.1, 0, 1)
        snap = band(rng.uniform(-1, 1, len(t)), 1500, 9000) * env(t - 0.75, 0.001, 0.03) * (t > 0.75) * 2
        return norm(x + snap, 0.8)
    if name == "jail_door_slide":
        t = t_axis(1.5)
        roll = band(rng.uniform(-1, 1, len(t)), 50, 450) * np.clip(t / 0.1, 0, 1) * (t < 1.1)
        rattle = np.zeros(len(t))
        for at in np.arange(0.05, 1.05, 0.045):
            k = int(at * SR)
            n = int(0.02 * SR)
            rattle[k:k + n] += band(rng.uniform(-1, 1, n), 1500, 6000) * np.exp(-np.arange(n) / (0.004 * SR)) * 0.4
        clang = sum(np.sin(2 * np.pi * f * t) * np.exp(-(t - 1.1) / d) for f, d in ((310, 0.5), (830, 0.35), (1470, 0.25), (2290, 0.15))) * (t > 1.1)
        return norm(roll + rattle + clang * 0.6, 0.85)
    if name == "jail_buzzer":
        t = t_axis(0.8)
        x = np.sign(np.sin(2 * np.pi * 220 * t)) * (0.6 + 0.4 * np.sin(2 * np.pi * 50 * t)) * np.clip((0.8 - t) / 0.05, 0, 1)
        return norm(band(x, 100, 4000), 0.7)
    if name == "cash_register_bell":
        t = t_axis(1.2)
        bell = sum(np.sin(2 * np.pi * f * t) * np.exp(-t / d) * a for f, d, a in ((2093, 0.6, 1), (2793, 0.4, 0.5), (5240, 0.2, 0.3)))
        drawer = band(rng.uniform(-1, 1, len(t)), 100, 1200) * env(t - 0.15, 0.01, 0.12) * (t > 0.15) * 1.5
        clunk = np.sin(2 * np.pi * 90 * t) * env(t - 0.32, 0.002, 0.06) * (t > 0.32) * 1.2
        return norm(bell + drawer + clunk, 0.85)
    if name == "civilian_scream":
        return norm(formant_voice(rng, lambda t: 650 + 300 * np.sin(np.pi * np.clip(t / 1.1, 0, 1)) + 25 * np.sin(2 * np.pi * 7 * t), 1.1,
                                  (900, 1400, 3000), 0.25) * np.clip(t_axis(1.1) / 0.05, 0, 1) * np.clip((1.1 - t_axis(1.1)) / 0.3, 0, 1), 0.8)
    if name == "car_horn":
        t = t_axis(0.55)
        x = sum(np.sign(np.sin(2 * np.pi * f * t)) * 0.5 + np.sin(2 * np.pi * f * t) for f in (415, 523))
        return norm(band(x, 200, 3500) * np.clip(t / 0.02, 0, 1) * np.clip((0.55 - t) / 0.05, 0, 1), 0.75)
    if name == "bus_brakes":
        t = t_axis(1.3)
        hiss = band(rng.uniform(-1, 1, len(t)), 2000, 9000) * env(t - 0.3, 0.02, 0.4) * (t > 0.3)
        squeal = np.sin(2 * np.pi * np.cumsum(1250 - 200 * t) / SR) * np.clip(t / 0.05, 0, 1) * (t < 0.35) * 0.4
        return norm(hiss + squeal, 0.7)
    if name == "traffic_loop":
        t = t_axis(8.0)
        x = band(rng.uniform(-1, 1, len(t)), 30, 380) * 0.8
        for _ in range(7):                                   # véhicules qui passent (effet Doppler)
            at = rng.integers(0, len(t))
            d = rng.uniform(2.0, 3.5)
            tt = t_axis(d)
            f0 = rng.uniform(70, 120)
            f = f0 * (1.08 - 0.16 * np.clip(tt / d, 0, 1))
            pas = (np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.4 + band(rng.uniform(-1, 1, len(tt)), 200, 2500) * 0.6) * np.sin(np.pi * tt / d) ** 2
            add_wrap(x, pas * 0.9, at)
        return norm(x, 0.6)
    if name == "wind_loop":
        t = t_axis(8.0)
        n = band(rng.uniform(-1, 1, len(t)), 150, 1800)
        gust = 0.55 + 0.45 * np.sin(2 * np.pi * t / 8.0 * 2 + 0.3) * np.sin(2 * np.pi * t / 8.0 * 3)
        whistle = np.sin(2 * np.pi * np.cumsum(700 + 120 * np.sin(2 * np.pi * t / 4.0)) / SR) * 0.05 * gust
        return norm(n * gust + whistle, 0.55)
    if name == "market_loop":
        t = t_axis(8.0)
        x = band(rng.uniform(-1, 1, len(t)), 200, 3000) * 0.08
        for _ in range(60):                                  # brouhaha : syllabes de voix au hasard
            d = rng.uniform(0.12, 0.35)
            f0 = rng.uniform(110, 260)
            fm = tuple(rng.uniform(a, b) for a, b in ((350, 900), (900, 1800), (2200, 3000)))
            v = formant_voice(rng, lambda tt, f0=f0: f0 * (1 + 0.1 * np.sin(2 * np.pi * 3 * tt)), d, fm, 0.05)
            v *= np.sin(np.pi * t_axis(d) / d)
            add_wrap(x, v * rng.uniform(0.2, 0.6), rng.integers(0, len(t)))
        return norm(x, 0.55)
    if name in ("cafe_music", "saloon_piano", "street_guitar"):
        bpm = {"cafe_music": 90, "saloon_piano": 120, "street_guitar": 100}[name]
        beat = 60 / bpm
        bars = 4
        dur = bars * 4 * beat
        t = t_axis(dur)
        x = np.zeros(len(t))
        prog = {"cafe_music": [(57, 60, 64, 67), (62, 65, 69, 72), (55, 59, 62, 65), (60, 64, 67, 71)],
                "saloon_piano": [(48, 52, 55, 60), (53, 57, 60, 65), (55, 59, 62, 67), (48, 52, 55, 60)],
                "street_guitar": [(52, 55, 59, 64), (48, 52, 55, 60), (55, 59, 62, 67), (50, 54, 57, 62)]}[name]
        midi = lambda m: 440 * 2 ** ((m - 69) / 12)
        for b, chord in enumerate(prog):
            for i in range(8):                               # croches
                at = int((b * 4 * beat + i * beat / 2) * SR)
                if name == "saloon_piano":
                    notes = [chord[0] - 12] if i % 2 == 0 else list(chord[1:])
                    dec, harm, amp = 0.5, 8, 0.35
                elif name == "street_guitar":
                    notes = [chord[(0, 2, 1, 3, 2, 1, 3, 2)[i]]]
                    dec, harm, amp = 0.9, 10, 0.5
                else:
                    notes = [chord[(i // 2) % 4], chord[0] - 12] if i % 2 == 0 else [chord[(i // 2 + 2) % 4]]
                    dec, harm, amp = 1.2, 4, 0.3
                for m in notes:
                    s = tone(midi(m), min(dur, 2.0), harm, dec, 1.3 if name != "cafe_music" else 2.0) * amp
                    s *= np.clip(t_axis(min(dur, 2.0)) / 0.004, 0, 1)
                    add_wrap(x, s, at)
            if name == "cafe_music":                         # contrebasse
                for i in range(4):
                    at = int((b * 4 * beat + i * beat) * SR)
                    add_wrap(x, tone(midi(chord[0] - 24 + (0, 7, 12, 7)[i]), beat, 3, 0.4, 1.0) * 0.5, at)
        if name != "street_guitar":
            x = x + band(rng.uniform(-1, 1, len(t)), 3000, 9000) * 0.01      # souffle de disque / de salle
        return norm(x, 0.6)
    if name == "radio_crackle":
        t = t_axis(1.6)
        bursts = (np.sin(2 * np.pi * 2.3 * t + 1) > 0.2).astype(float)
        voice = band(rng.uniform(-1, 1, len(t)), 400, 2600) * bursts * (0.5 + 0.5 * np.sin(2 * np.pi * 6 * t))
        squelch = np.sin(2 * np.pi * 1700 * t) * ((t < 0.08) | (t > 1.5)) * 0.5
        crack = (rng.uniform(0, 1, len(t)) > 0.997) * rng.uniform(-1, 1, len(t)) * 2
        return norm(voice * 0.7 + squelch + band(crack, 800, 8000), 0.6)
    if name == "loudspeaker":
        t = t_axis(3.4)
        x = np.zeros(len(t))
        for i, m in enumerate((76, 72, 67)):                 # carillon d'annonce
            add_wrap(x, tone(440 * 2 ** ((m - 69) / 12), 1.0, 3, 0.5, 1.5) * np.clip(t_axis(1.0) / 0.005, 0, 1) * 0.8, int(i * 0.35 * SR))
        for _ in range(16):                                  # voix lointaine, filtrée comme un mégaphone
            d = rng.uniform(0.12, 0.28)
            v = formant_voice(rng, lambda tt: 140 + 20 * np.sin(2 * np.pi * 2 * tt), d, (700, 1200, 2500), 0.05) * np.sin(np.pi * t_axis(d) / d)
            add_wrap(x, v * 0.5, int(rng.uniform(1.2, 3.1) * SR))
        x = band(x, 350, 3200)
        y = x.copy()
        for dly, g in ((0.11, 0.4), (0.23, 0.25), (0.37, 0.15)):  # écho de haut-parleur
            k = int(dly * SR)
            y[k:] += x[:-k] * g
        return norm(y, 0.7)
    if name == "dog_bark":
        t = t_axis(0.7)
        x = np.zeros(len(t))
        for at in (0.0, 0.32):
            d = 0.18
            v = formant_voice(rng, lambda tt: 320 * (1 - 0.3 * tt / d), d, (500, 1100, 2400), 0.6) * env(t_axis(d), 0.01, 0.07)
            add_wrap(x, v, int(at * SR))
        return norm(x, 0.8)
    if name == "pigeon_coo":
        t = t_axis(1.4)
        x = np.zeros(len(t))
        for at, d in ((0.0, 0.35), (0.45, 0.25), (0.75, 0.55)):
            tt = t_axis(d)
            f = 330 + 60 * np.sin(np.pi * tt / d)
            v = np.sin(2 * np.pi * np.cumsum(f) / SR) * (0.6 + 0.4 * np.sin(2 * np.pi * 28 * tt)) * np.sin(np.pi * tt / d)
            add_wrap(x, band(v, 150, 1500), int(at * SR))
        return norm(x, 0.55)
    if name == "pigeon_flap":
        t = t_axis(0.9)
        x = np.zeros(len(t))
        for i in range(10):
            n = int(0.04 * SR)
            add_wrap(x, band(rng.uniform(-1, 1, n), 300, 4000) * np.exp(-np.arange(n) / (0.01 * SR)) * (1 - i / 12), int(i * 0.075 * SR))
        return norm(x, 0.7)
    if name == "rain_tin_loop":
        t = t_axis(4.0)
        x = band(rng.uniform(-1, 1, len(t)), 1500, 9000) * 0.12
        for _ in range(500):
            n = int(0.03 * SR)
            f = rng.uniform(1800, 5200)
            drop = np.sin(2 * np.pi * f * t_axis(0.03)) * np.exp(-np.arange(n) / (0.004 * SR))
            add_wrap(x, drop * rng.uniform(0.2, 1.0), rng.integers(0, len(t)))
        return norm(x, 0.5)
    if name == "transformer_hum":
        t = t_axis(3.0)
        x = np.sin(2 * np.pi * 100 * t) + 0.5 * np.sin(2 * np.pi * 200 * t) + 0.25 * np.sin(2 * np.pi * 300 * t) + 0.1 * np.sin(2 * np.pi * 50 * t)
        x *= 0.85 + 0.15 * np.sin(2 * np.pi * t / 3.0 * 2)
        return norm(x, 0.45)
    if name == "spray_paint":
        t = t_axis(1.1)
        rattle = np.zeros(len(t))
        for i in range(5):
            n = int(0.02 * SR)
            rattle[int(i * 0.04 * SR):int(i * 0.04 * SR) + n] += np.sin(2 * np.pi * 2400 * t_axis(0.02)) * np.exp(-np.arange(n) / 200)
        hiss = band(rng.uniform(-1, 1, len(t)), 3000, 10000) * np.clip((t - 0.25) / 0.05, 0, 1) * np.clip((1.1 - t) / 0.1, 0, 1)
        return norm(rattle * 0.6 + hiss, 0.6)
    raise ValueError(name)


CITY_SOUNDS = ["crowbar_pry", "jail_door_slide", "jail_buzzer", "cash_register_bell", "civilian_scream", "car_horn", "bus_brakes",
               "traffic_loop", "wind_loop", "market_loop", "cafe_music", "saloon_piano", "street_guitar", "radio_crackle", "loudspeaker",
               "dog_bark", "pigeon_coo", "pigeon_flap", "rain_tin_loop", "transformer_hum", "spray_paint"]

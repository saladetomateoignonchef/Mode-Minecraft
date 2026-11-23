#!/usr/bin/env python3
"""
Animations de présentation du README (docs/videos/*.gif et *.mp4), rendues à partir des modèles du mod (pas de capture du jeu) :
- présentation : tour complet de véhicules, avions, navires et du train, avec leur nom ;
- train : la locomotive et ses wagons roulent sur la voie (fumée, rails qui défilent) ;
- galerie d'armes : diaporama des armes à feu.
Utilisation : python3 tools/readme_videos.py  (nécessite ffmpeg)
"""
import math
import os
import shutil
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import readme_images as ri  # noqa: E402

OUT = os.path.join(HERE, "..", "docs/videos")
TMP = os.path.join("/tmp", "mm_frames")
os.makedirs(OUT, exist_ok=True)


# =============================================================================== rendu 3D (z-buffer)
def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


FACES = [  # normale, 4 sommets (indices de coin : bits x, y, z)
    ((1, 0, 0), [(1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)]),
    ((-1, 0, 0), [(0, 0, 1), (0, 0, 0), (0, 1, 0), (0, 1, 1)]),
    ((0, 1, 0), [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)]),
    ((0, -1, 0), [(0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0)]),
    ((0, 0, 1), [(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]),
    ((0, 0, -1), [(1, 0, 0), (0, 0, 0), (0, 1, 0), (1, 1, 0)]),
]
LIGHT = np.array([0.35, 0.85, 0.4])
LIGHT /= np.linalg.norm(LIGHT)


def tri(zbuf, img, p, z, col):
    x0, y0, x1, y1 = p[0][0], p[0][1], p[1][0], p[1][1]
    x2, y2 = p[2]
    minx, maxx = int(max(0, math.floor(min(x0, x1, x2)))), int(min(img.shape[1] - 1, math.ceil(max(x0, x1, x2))))
    miny, maxy = int(max(0, math.floor(min(y0, y1, y2)))), int(min(img.shape[0] - 1, math.ceil(max(y0, y1, y2))))
    if maxx < minx or maxy < miny:
        return
    det = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
    if abs(det) < 1e-9:
        return
    gx, gy = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
    a = ((y1 - y2) * (gx - x2) + (x2 - x1) * (gy - y2)) / det
    b = ((y2 - y0) * (gx - x2) + (x0 - x2) * (gy - y2)) / det
    c = 1 - a - b
    inside = (a >= -1e-6) & (b >= -1e-6) & (c >= -1e-6)
    depth = a * z[0] + b * z[1] + c * z[2]
    zb = zbuf[miny:maxy + 1, minx:maxx + 1]
    ok = inside & (depth > zb)
    zb[ok] = depth[ok]
    img[miny:maxy + 1, minx:maxx + 1][ok] = col


def render(g, boxes, yaw, elev=math.radians(24), size=(640, 360), ss=2, center=None, scale=None, ground=True):
    W, H = size[0] * ss, size[1] * ss
    R = rot_x(elev) @ rot_y(yaw)
    img = np.zeros((H, W, 4), np.uint8)
    zbuf = np.full((H, W), -1e9)
    if center is None:
        pts = np.array([[(b[0] + b[1]) / 2, (b[2] + b[3]) / 2, (b[4] + b[5]) / 2] for b in boxes])
        center = (pts.min(0) + pts.max(0)) / 2
    cx, cy, cz = center
    for (x0, x1, y0, y1, z0, z1, key) in boxes:
        base = np.array(ri.color_of(g, key), float)
        if key == "glass":
            base = np.array([110, 150, 190], float)
        for n, corners in FACES:
            nn = R @ np.array(n, float)
            if nn[2] <= 0:
                continue
            shade = 0.55 + 0.6 * max(0.0, float(np.dot(R.T @ LIGHT, np.array(n, float))))
            col = np.append(np.clip(base * shade, 0, 255), 255).astype(np.uint8)
            verts = []
            for (bx, by, bz) in corners:
                P = np.array([x1 if bx else x0, y1 if by else y0, z1 if bz else z0]) - np.array([cx, cy, cz])
                q = R @ P
                verts.append(q)
            v = [(q[0] * scale * ss + W / 2, -q[1] * scale * ss + H / 2, q[2]) for q in verts]
            tri(zbuf, img, [v[0][:2], v[1][:2], v[2][:2]], [v[0][2], v[1][2], v[2][2]], col)
            tri(zbuf, img, [v[0][:2], v[2][:2], v[3][:2]], [v[0][2], v[2][2], v[3][2]], col)
    im = Image.fromarray(img, "RGBA")
    return im.resize(size, Image.LANCZOS)


def fit_scale(boxes, size, elev=math.radians(24), margin=0.78):
    best = 0
    for yaw in np.linspace(0, math.pi * 2, 12, endpoint=False):
        R = rot_x(elev) @ rot_y(yaw)
        pts = []
        for (x0, x1, y0, y1, z0, z1, _k) in boxes:
            for x in (x0, x1):
                for y in (y0, y1):
                    for z in (z0, z1):
                        pts.append(R @ np.array([x, y, z]))
        pts = np.array(pts)
        c = (pts.min(0) + pts.max(0)) / 2
        w = (pts[:, 0].max() - pts[:, 0].min())
        h = (pts[:, 1].max() - pts[:, 1].min())
        best = max(best, w / (size[0] * margin), h / (size[1] * margin))
    return 1 / best


def background(size, c1=(30, 38, 50), c2=(10, 13, 18)):
    return ri.gradient(size[0], size[1], c1, c2)


def caption(frame, title, subtitle):
    d = ImageDraw.Draw(frame)
    w, h = frame.size
    d.rectangle([0, h - 62, w, h], fill=(8, 11, 15))
    d.rectangle([0, h - 62, 5, h], fill=ri.ACCENT)
    d.text((18, h - 54), title, font=ri.font(20, True), fill=ri.TEXT)
    d.text((18, h - 28), subtitle, font=ri.font(12), fill=ri.MUTED)
    d.text((w - 160, 12), "MODE MINECRAFT", font=ri.font(13, True), fill=ri.ACCENT)
    return frame


def encode(frames_dir, name, fps, width):
    gif = os.path.join(OUT, name + ".gif")
    mp4 = os.path.join(OUT, name + ".mp4")
    pat = os.path.join(frames_dir, "f%04d.png")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(fps), "-i", pat, "-vf",
                    f"scale={width}:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=96:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4",
                    "-loop", "0", gif], check=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(fps), "-i", pat, "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2",
                    "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "24", mp4], check=True)
    print(name, round(os.path.getsize(gif) / 1e6, 1), "Mo gif,", round(os.path.getsize(mp4) / 1e6, 1), "Mo mp4")


def fresh(d):
    shutil.rmtree(d, ignore_errors=True)
    os.makedirs(d)


# =============================================================================== animations
def presentation(g, models):
    size = (640, 360)
    segs = [("f22_raptor", "Lockheed Martin F-22 Raptor", "Aviation militaire : 21 avions, furtivité radar, arsenal aérien"),
            ("m1a2_abrams", "M1A2 Abrams", "Véhicules terrestres : chars, blindés, camions, complexes sol-air"),
            ("uss_gerald_r_ford", "USS Gerald R. Ford", "Porte-avions pilotable : avions sur le pont, catapulte, appontage"),
            ("kirov_cruiser", "Croiseur Kirov", "Marine : croiseurs, destroyers, sous-marins nucléaires"),
            ("light_civil_plane", "Avion léger biplace", "Vol pur sans armement, décollage en 15 à 20 blocs"),
            ("train", "Train des villes du Mesa", "Locomotive à vapeur, wagons, contrôleurs à matraque électrique")]
    fresh(TMP)
    n = 0
    per = 30
    for vid, title, sub in segs:
        if vid == "train":
            boxes = ri.iso_render(g, models["mesa_locomotive"])
            for i in range(2):
                boxes += ri.iso_render(g, models["mesa_wagon"], offset=(-10.6 - i * 11.4, 0, 0))
            scale = fit_scale(boxes, size, margin=0.86)
        else:
            boxes = ri.iso_render(g, models[vid])
            scale = fit_scale(boxes, size)
        for k in range(per):
            yaw = math.radians(-35 + 360 * k / per)
            im = render(g, boxes, yaw, size=size, scale=scale)
            f = background(size)
            f.paste(im, (0, -14), im)
            caption(f, title, sub).save(os.path.join(TMP, f"f{n:04d}.png"))
            n += 1
    encode(TMP, "presentation", 12, 560)


def train_anim(g, models):
    size = (720, 330)
    fresh(TMP)
    loco, wag = models["mesa_locomotive"], models["mesa_wagon"]
    boxes = ri.iso_render(g, loco)
    for i in range(2):
        boxes += ri.iso_render(g, wag, offset=(-10.6 - i * 11.4, 0, 0))
    elev = math.radians(20)
    yaw = math.radians(-58)
    scale = fit_scale(boxes, (size[0], size[1] - 70), elev=elev, margin=0.92)
    frames = 36
    rng = np.random.default_rng(3)
    puffs = []
    R = rot_x(elev) @ rot_y(yaw)
    for k in range(frames):
        f = background(size, (74, 52, 38), (22, 16, 14))
        d = ImageDraw.Draw(f, "RGBA")
        W, H = size
        # voie : traverses qui défilent (la locomotive avance vers la droite)
        shift = (k / frames) * 2.0
        for xs in range(-60, 20):
            x = xs + shift
            pts = []
            for z in (-1.15, 1.15):
                P = np.array([x - 20, 0, z])
                q = R @ P
                pts.append((q[0] * scale + W / 2, -q[1] * scale + H * 0.52 - 4))
            d.line(pts, fill=(88, 60, 38, 255), width=3)
        for z in (-0.75, 0.75):
            a, b = R @ np.array([-80, 0, z]), R @ np.array([40, 0, z])
            d.line([(a[0] * scale + W / 2, -a[1] * scale + H * 0.52 - 4), (b[0] * scale + W / 2, -b[1] * scale + H * 0.52 - 4)], fill=(150, 150, 156, 255), width=2)
        im = render(g, boxes, yaw, elev=elev, size=size, scale=scale, center=np.array([-14.0, 0.0, 0.0]))
        f.paste(im, (0, -16), im)
        # fumée de la cheminée
        chim = np.array([3.2, 4.2, 0.0]) - np.array([-14.0, 0.0, 0.0])
        q = R @ chim
        cx, cy = q[0] * scale + W / 2, -q[1] * scale + H / 2 - 16
        puffs.append([cx, cy, 0])
        for p in puffs:
            p[0] -= 5.0
            p[1] -= 3.2
            p[2] += 1
        puffs = [p for p in puffs if p[2] < 22]
        for p in puffs:
            r = 5 + p[2] * 1.1
            al = int(150 * (1 - p[2] / 22))
            d.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=(220, 220, 220, al))
        f = f.convert("RGB")
        caption(f, "Le train des villes du Mesa", "25 s d'arrêt en gare, sifflet au départ, contrôle des billets à bord").save(os.path.join(TMP, f"f{k:04d}.png"))
    encode(TMP, "train", 15, 640)


def weapons_slideshow():
    size = (560, 315)
    fresh(TMP)
    src = ri.ids_of(r'\bguns?\(\s*"([a-z0-9_]+)"', ri.ITEMS_SRC)
    ids = [i for i in src if ri.icon(i, 160) is not None][:24]
    n = 0
    per = 6
    for gid in ids:
        ic = ri.icon(gid, 200)
        for k in range(per):
            f = background(size)
            d = ImageDraw.Draw(f)
            t = k / (per - 1)
            sc = 0.9 + 0.12 * t
            im = ic.resize((int(ic.width * sc), int(ic.height * sc)), Image.NEAREST)
            sh = Image.new("RGBA", im.size, (0, 0, 0, 0))
            sh.putalpha(im.split()[3].point(lambda a: 90 if a else 0))
            x, y = (size[0] - im.width) // 2, 40 + (200 - im.height) // 2
            f.paste(sh, (x + 5, y + 5), sh)
            f.paste(im, (x, y), im)
            caption(f, ri.name_of(gid), "Armes à feu : 78 armes, réalisme du tir, accessoires et munitions").save(os.path.join(TMP, f"f{n:04d}.png"))
            n += 1
    encode(TMP, "armes", 10, 480)


if __name__ == "__main__":
    g, models = ri.render_models()
    which = sys.argv[1:] or ["presentation", "train", "armes"]
    if "presentation" in which:
        presentation(g, models)
    if "train" in which:
        train_anim(g, models)
    if "armes" in which:
        weapons_slideshow()

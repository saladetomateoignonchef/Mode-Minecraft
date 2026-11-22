"""
Flotte aérienne (cahier des charges « Aviation militaire ») : 20 avions soviétiques / russes et américains, en voxels.
Repère : avant +X, haut +Y, droite +Z, sol en y = 0 (train sorti). Pièces : hull, prop (hélice posée à chaque moyeu),
gear (train, caché en vol), bay / bay_open (trappes de soute ou rampe). Bouches de tir (mount 0, dans l'axe) :
0 = canon ; 1 / 2 = roquettes G / D ; 3 / 4 = missiles G / D ; 5 / 6 = 2e type de missiles G / D ; 7+ = tourelles.
"""
import math


def fuselage(v, p, x0, x1, cy, r, nose=1.5, tail=2.0, nose_key=None, tail_r=0.35):
    v.cyl("hull", "x", (cy, 0.0), r, x0 + tail, x1 - nose, p)
    for i in range(3):
        f = (i + 1) / 3.5
        a, b = x1 - nose + nose * i / 3, x1 - nose + nose * (i + 1) / 3
        v.cyl("hull", "x", (cy - r * 0.08 * (i + 1), 0.0), max(0.12, r * (1 - f * 0.85)), a, b, nose_key or p)
    for i in range(3):
        f = (i + 1) / 3
        a, b = x0 + tail * (2 - i) / 3, x0 + tail * (3 - i) / 3
        v.cyl("hull", "x", (cy + r * 0.25 * f, 0.0), max(0.15, r * (1 - f * (1 - tail_r))), a, b, p)


def wings(v, p, x_le, root, tip, half, y, thick=0.22, sweep=0.0, z0=0.5, steps=8, dihedral=0.0, flap=None):
    for i in range(steps):
        f = i / steps
        za, zb = z0 + (half - z0) * f, z0 + (half - z0) * (i + 1) / steps
        chord = root + (tip - root) * f
        xl = x_le - sweep * (za - z0)
        yy = y + dihedral * (za - z0)
        t = max(0.08, thick * (1 - 0.5 * f))
        for s in (-1, 1):
            a, b = sorted((s * za, s * zb))
            v.box("hull", xl - chord, xl, yy, yy + t, a, b, p)
            if flap:
                v.box("hull", xl - chord, xl - chord + min(0.4, chord * 0.2), yy - 0.01, yy + t * 0.8, a, b, flap)


def tailplane(v, p, x_te, root, tip, half, y, sweep=0.4, thick=0.14):
    wings(v, p, x_te + root, root, tip, half, y, thick, sweep, z0=0.2, steps=4)


def fin(v, p, x_te, root, tip, h, y0, z=0.0, sweep=0.6, thick=0.16, lean=0.0):
    n = 5
    for i in range(n):
        f = i / n
        ya, yb = y0 + h * f, y0 + h * (i + 1) / n
        chord = root + (tip - root) * f
        xs = x_te + sweep * h * f
        zz = z + lean * (ya - y0)
        v.box("hull", xs, xs + chord, ya, yb, zz - thick / 2, zz + thick / 2, p)


def canopy(v, x0, x1, y, w, h, key="glass", frame=None):
    v.box("hull", x0, x1, y, y + h * 0.7, -w / 2, w / 2, key)
    v.box("hull", x0 + (x1 - x0) * 0.15, x1 - (x1 - x0) * 0.2, y + h * 0.7, y + h, -w * 0.35, w * 0.35, key)
    if frame:
        v.box("hull", (x0 + x1) / 2 - 0.05, (x0 + x1) / 2 + 0.05, y, y + h + 0.02, -w / 2 - 0.01, w / 2 + 0.01, frame)


def jet_engine(v, x0, x1, cy, z, r, key="dark", intake=True):
    v.cyl("hull", "x", (cy, z), r, x0, x1, key)
    v.cyl("hull", "x", (cy, z), r * 0.75, x0 - 0.25, x0, "black")          # tuyère
    if intake:
        v.cyl("hull", "x", (cy, z), r * 0.7, x1, x1 + 0.08, "black")


def gear(v, x_main, track, x_nose, cy, r=0.35, nose_r=0.25, tail_wheel=None):
    for s in (-1, 1):
        z = s * track
        v.box("gear", x_main - 0.08, x_main + 0.08, r, cy - 0.2, z - 0.07, z + 0.07, "metal")
        v.cyl("gear", "z", (x_main, r), r, z - 0.14, z + 0.14, "tire")
    if x_nose is not None:
        v.box("gear", x_nose - 0.06, x_nose + 0.06, nose_r, cy - 0.2, -0.05, 0.05, "metal")
        v.cyl("gear", "z", (x_nose, nose_r), nose_r, -0.1, 0.1, "tire")
    if tail_wheel is not None:
        v.box("gear", tail_wheel - 0.05, tail_wheel + 0.05, 0.15, cy - 0.3, -0.04, 0.04, "metal")
        v.cyl("gear", "z", (tail_wheel, 0.15), 0.15, -0.06, 0.06, "tire")


def pylons(v, x, y, zs, length, key, r=0.09, part="hull"):
    """Missiles / roquettes sous les ailes (symétriques)."""
    for z in zs:
        for s in (-1, 1):
            v.box("hull", x - length * 0.3, x + length * 0.2, y, y + 0.1, s * z - 0.04, s * z + 0.04, "metal")
            v.cyl(part, "x", (y - r - 0.02, s * z), r, x - length / 2, x + length / 2, key)


def pod(v, x, y, z, length, r, key="dark"):
    for s in (-1, 1):
        v.cyl("hull", "x", (y - r, s * z), r, x - length / 2, x + length / 2, key)
        v.box("hull", x + length / 2, x + length / 2 + 0.03, y - 2 * r, y, s * z - r, s * z + r, "black")


def bomb_bay(v, x0, x1, y, w, key):
    v.box("bay", x0, x1, y - 0.06, y, -w / 2, w / 2, key)
    for s in (-1, 1):
        a, b = sorted((s * w / 2, s * (w / 2 + 0.06)))
        v.box("bay_open", x0, x1, y - 0.9, y, a, b, key)
    v.box("bay_open", x0 + 0.1, x1 - 0.1, y - 0.04, y, -w / 2 + 0.05, w / 2 - 0.05, "black")


def roundel_star(v, x, y, z, s=1, key="red"):
    v.box("hull", x - 0.35, x + 0.35, y - 0.35, y + 0.35, z - 0.01, z + 0.01, key)


def mount_muzzles(v, gun, rockets=None, missiles=None, missiles2=None, extra=()):
    v.muzzle(*gun, 0)
    for lst in (rockets, missiles, missiles2):
        if lst:
            x, y, z = lst
            v.muzzle(x, y, -z, 0)
            v.muzzle(x, y, z, 0)
        else:
            v.muzzle(gun[0], gun[1], -0.3, 0)
            v.muzzle(gun[0], gun[1], 0.3, 0)
    for m in extra:
        v.muzzle(*m, 0)


# =========================================================================== modèles
SCALE = {}


def make(V0):
    planes = []

    def V(name, sc):
        return V0(name, max(sc, SCALE.get(name, 0)))

    def reg(fn):
        def build():
            v = fn()
            need = max(v.xmax - v.xmin, 2 * v.zmax) / 2.9          # 48 px moins une marge
            if v.S < need:
                SCALE[v.name] = math.ceil(need * 2) / 2
                v = fn()
            return v
        planes.append(build)
        return fn

    # ---------------------------------------------------------------- URSS / Russie
    @reg
    def il2():
        v = V("il2_sturmovik", 5.0)
        p, cy = "ww2_green", 1.9
        fuselage(v, p, -5.8, 5.6, cy, 0.62, nose=1.4, tail=3.2)
        v.box("hull", 1.0, 4.6, cy - 0.65, cy + 0.2, -0.6, 0.6, "ww2_green_d")       # caisson blindé
        canopy(v, 0.0, 2.2, cy + 0.45, 0.8, 0.45, frame="ww2_green_d")
        wings(v, p, 2.8, 2.6, 1.1, 7.3, cy - 0.35, 0.3, sweep=0.15, z0=0.6, dihedral=0.05)
        tailplane(v, p, -5.7, 1.2, 0.6, 2.3, cy + 0.1)
        fin(v, p, -5.9, 1.3, 0.7, 1.5, cy + 0.3)
        v.box("hull", 5.6, 5.9, cy - 0.3, cy + 0.3, -0.3, 0.3, "metal")
        v.props.append((6.0, cy, 0.0))
        pod(v, 1.8, cy - 0.45, 3.2, 1.3, 0.09, "metal")                           # RS-82
        gear(v, 2.4, 1.6, None, cy, 0.4, tail_wheel=-5.2)
        roundel_star(v, -3.5, cy, 0.63)
        roundel_star(v, -3.5, cy, -0.63)
        mount_muzzles(v, (3.5, cy - 0.3, 2.0), rockets=(2.5, cy - 0.55, 3.2))
        v.seat(0.9, cy - 0.15, 0.0)
        return v

    @reg
    def la7():
        v = V("la7", 4.0)
        p, cy = "la7_gray", 1.7
        fuselage(v, p, -4.4, 4.2, cy, 0.55, nose=1.2, tail=2.6)
        v.cyl("hull", "x", (cy, 0.0), 0.62, 3.0, 4.0, "red")                      # capot rouge
        canopy(v, -0.6, 0.9, cy + 0.4, 0.7, 0.45, frame=p)
        wings(v, p, 1.9, 2.1, 0.9, 4.9, cy - 0.3, 0.25, sweep=0.1, z0=0.5, dihedral=0.06)
        tailplane(v, p, -4.3, 0.9, 0.5, 1.8, cy + 0.1)
        fin(v, p, -4.5, 1.0, 0.5, 1.2, cy + 0.3)
        v.props.append((4.5, cy, 0.0))
        gear(v, 1.4, 1.3, None, cy, 0.33, tail_wheel=-3.9)
        roundel_star(v, -2.7, cy, 0.56)
        roundel_star(v, -2.7, cy, -0.56)
        mount_muzzles(v, (4.3, cy + 0.3, 0.0))
        v.seat(0.1, cy - 0.1, 0.0)
        return v

    @reg
    def mig15():
        v = V("mig15", 4.0)
        p, cy = "silver", 1.6
        fuselage(v, p, -4.8, 5.2, cy, 0.62, nose=0.6, tail=2.0)
        v.cyl("hull", "x", (cy, 0.0), 0.45, 5.0, 5.3, "black")                    # entrée d'air de nez
        canopy(v, 2.0, 3.4, cy + 0.5, 0.65, 0.42, frame=p)
        wings(v, p, 1.8, 2.4, 1.2, 5.0, cy - 0.15, 0.22, sweep=0.75, z0=0.55, dihedral=-0.03)
        fin(v, p, -4.9, 2.2, 1.0, 2.2, cy + 0.4, sweep=0.9)
        tailplane(v, p, -3.3, 1.0, 0.5, 1.9, cy + 1.9, sweep=0.6)
        jet_engine(v, -5.0, -4.6, cy, 0.0, 0.4, intake=False)
        gear(v, 0.4, 1.3, 3.8, cy, 0.3)
        roundel_star(v, -3.0, cy + 1.2, 0.09)
        roundel_star(v, -3.0, cy + 1.2, -0.09)
        mount_muzzles(v, (4.8, cy - 0.35, 0.0))
        v.seat(2.5, cy - 0.05, 0.0)
        return v

    @reg
    def tu95():
        v = V("tu95_bear", 12.0)
        p, cy = "silver", 2.6
        fuselage(v, p, -15.5, 15.0, cy, 1.5, nose=2.5, tail=4.0)
        canopy(v, 10.5, 12.5, cy + 1.1, 1.6, 0.6, frame=p)
        v.box("hull", 14.6, 15.3, cy - 0.2, cy + 0.6, -0.4, 0.4, "black")           # perche de ravitaillement
        wings(v, p, 3.5, 6.0, 2.0, 17.5, cy - 0.3, 0.5, sweep=0.55, z0=1.4, steps=10, dihedral=-0.02)
        for z in (5.2, 10.0):
            for s in (-1, 1):
                x = 3.5 - 0.55 * (z - 1.4) + 0.5
                v.cyl("hull", "x", (cy - 0.6, s * z), 0.7, x - 3.5, x + 1.5, p)
                v.props.append((x + 1.8, cy - 0.6, s * z))
                v.props.append((x + 1.4, cy - 0.6, s * z))                         # hélices contrarotatives
        fin(v, p, -15.6, 4.5, 1.8, 6.0, cy + 1.0, sweep=1.0, thick=0.3)
        tailplane(v, p, -15.6, 3.0, 1.2, 6.5, cy + 0.8, sweep=0.7)
        v.box("hull", -16.3, -15.3, cy - 0.3, cy + 0.6, -0.4, 0.4, "glass")         # poste de queue
        for dz in (-0.12, 0.12):
            v.cyl("hull", "x", (cy + 0.1, dz), 0.06, -17.0, -16.2, "gun")
        v.cyl("hull", "y", (-6.0, 0.0), 0.5, cy + 1.4, cy + 1.8, "glass")           # tourelle dorsale
        bomb_bay(v, -2.0, 4.0, cy - 1.45, 1.8, "silver_d")
        gear(v, 2.0, 5.2, 11.0, cy, 0.55, 0.4)
        roundel_star(v, -10, cy, 1.52)
        roundel_star(v, -10, cy, -1.52)
        mount_muzzles(v, (14.0, cy - 0.8, 0.0), missiles=(1.0, cy - 1.0, 3.0),
                      extra=((-17.0, cy + 0.1, 0.0), (-6.0, cy + 1.9, 0.0)))
        v.seat(11.5, cy + 0.2, -0.45)
        v.seat(11.5, cy + 0.2, 0.45)
        v.seat(-15.8, cy - 0.4, 0.0)
        v.seat(-6.0, cy + 0.3, 0.0)
        return v

    @reg
    def mig21():
        v = V("mig21", 4.0)
        p, cy = "sov_gray", 1.6
        fuselage(v, p, -6.2, 6.6, cy, 0.55, nose=0.8, tail=1.5)
        v.cyl("hull", "x", (cy, 0.0), 0.32, 6.5, 7.2, "dark")                      # cône d'entrée d'air
        canopy(v, 3.2, 4.6, cy + 0.42, 0.6, 0.38, frame=p)
        wings(v, p, 2.0, 5.2, 0.4, 3.6, cy - 0.15, 0.2, sweep=1.25, z0=0.5, steps=6)   # aile delta
        fin(v, p, -6.2, 2.6, 1.0, 2.2, cy + 0.4, sweep=1.0)
        tailplane(v, p, -5.6, 1.2, 0.5, 1.7, cy, sweep=0.7)
        jet_engine(v, -6.4, -6.0, cy, 0.0, 0.42, intake=False)
        pylons(v, -0.8, cy - 0.25, (1.6, 2.5), 2.0, "missile")
        gear(v, -0.6, 1.4, 4.6, cy, 0.3)
        roundel_star(v, -4.5, cy + 1.3, 0.09)
        roundel_star(v, -4.5, cy + 1.3, -0.09)
        mount_muzzles(v, (2.5, cy - 0.55, 0.0), rockets=(-0.5, cy - 0.5, 2.5), missiles=(-0.5, cy - 0.5, 1.6))
        v.seat(3.6, cy - 0.05, 0.0)
        return v

    @reg
    def mig25():
        v = V("mig25", 5.0)
        p, cy = "silver", 2.0
        fuselage(v, p, -9.6, 9.8, cy, 0.7, nose=2.0, tail=1.0)
        for s in (-1, 1):
            v.box("hull", -8.0, 4.5, cy - 0.7, cy + 0.6, s * 0.7, s * 1.9, p)       # entrées d'air rectangulaires
            v.box("hull", 4.5, 4.7, cy - 0.6, cy + 0.5, s * 0.8, s * 1.8, "black")
            jet_engine(v, -10.2, -9.6, cy, s * 1.1, 0.62, intake=False)
            fin(v, p, -9.8, 2.6, 1.0, 2.6, cy + 0.6, z=s * 1.6, sweep=0.9, lean=0.15 * s)
        canopy(v, 5.5, 7.0, cy + 0.6, 0.7, 0.4, frame=p)
        wings(v, p, 1.0, 4.6, 1.6, 7.0, cy + 0.3, 0.24, sweep=0.55, z0=1.9, steps=6, dihedral=-0.05)
        tailplane(v, p, -9.6, 1.8, 0.8, 4.2, cy + 0.2)
        pylons(v, -1.5, cy + 0.05, (4.0, 5.5, 6.6), 3.0, "missile", r=0.12)
        gear(v, -1.2, 2.2, 6.0, cy, 0.4, 0.3)
        roundel_star(v, -7.5, cy + 2.0, 1.6)
        roundel_star(v, -7.5, cy + 2.0, -1.6)
        mount_muzzles(v, (9.0, cy, 0.0), missiles=(0.0, cy - 0.2, 4.0))
        v.seat(6.2, cy + 0.05, 0.0)
        return v

    @reg
    def su25():
        v = V("su25", 5.0)
        p, cy = "sov_camo", 1.9
        fuselage(v, p, -7.2, 7.4, cy, 0.62, nose=1.8, tail=1.8)
        canopy(v, 3.6, 5.0, cy + 0.45, 0.65, 0.4, key="glass", frame="sov_d")
        for s in (-1, 1):
            jet_engine(v, -3.5, 1.6, cy - 0.15, s * 0.95, 0.5)                    # nacelles accolées
        wings(v, p, 1.5, 3.0, 1.2, 7.2, cy - 0.2, 0.28, sweep=0.35, z0=1.4, steps=8)
        fin(v, p, -7.4, 2.4, 1.0, 2.3, cy + 0.4, sweep=0.9)
        tailplane(v, p, -6.8, 1.3, 0.6, 2.6, cy + 0.2)
        pod(v, 0.5, cy - 0.35, 3.4, 2.0, 0.22)                                    # paniers S-8
        pod(v, 0.5, cy - 0.3, 4.6, 2.0, 0.22)
        pylons(v, 0.3, cy - 0.3, (5.8,), 2.6, "missile_g", r=0.12)
        gear(v, -0.2, 1.4, 4.8, cy, 0.38, 0.3)
        roundel_star(v, -5.5, cy + 1.3, 0.09)
        roundel_star(v, -5.5, cy + 1.3, -0.09)
        mount_muzzles(v, (6.5, cy - 0.55, -0.3), rockets=(1.5, cy - 0.6, 4.0), missiles=(1.6, cy - 0.5, 5.8))
        v.seat(4.3, cy - 0.05, 0.0)
        return v

    @reg
    def mig29():
        v = V("mig29", 4.0)
        p, cy = "mig29_gray", 1.7
        fuselage(v, p, -8.0, 8.6, cy, 0.55, nose=2.2, tail=1.0)
        for s in (-1, 1):
            v.box("hull", -7.0, 2.6, cy - 0.65, cy + 0.15, s * 0.6, s * 1.6, p)
            v.box("hull", 2.6, 2.9, cy - 0.65, cy + 0.05, s * 0.65, s * 1.55, "black")
            jet_engine(v, -8.3, -7.4, cy - 0.25, s * 1.0, 0.45, intake=False)
            fin(v, p, -7.8, 1.9, 0.8, 2.0, cy + 0.15, z=s * 1.3, sweep=0.9, lean=0.1 * s)
        canopy(v, 3.4, 5.0, cy + 0.42, 0.65, 0.42, frame=p)
        wings(v, p, 1.2, 3.8, 1.1, 5.7, cy - 0.05, 0.2, sweep=0.75, z0=1.5, steps=6)
        tailplane(v, p, -7.6, 1.6, 0.6, 3.6, cy - 0.05)
        pylons(v, -0.6, cy - 0.15, (2.6, 3.6, 4.5), 2.2, "missile")
        gear(v, -0.8, 1.6, 5.0, cy, 0.33)
        roundel_star(v, -6.5, cy + 1.4, 1.38)
        roundel_star(v, -6.5, cy + 1.4, -1.38)
        mount_muzzles(v, (3.5, cy + 0.1, -0.6), missiles=(0.0, cy - 0.3, 2.6), missiles2=(0.0, cy - 0.3, 3.6))
        v.seat(4.2, cy - 0.05, 0.0)
        return v

    @reg
    def su34():
        v = V("su34", 5.0)
        p, cy = "su34_blue", 2.0
        fuselage(v, p, -9.8, 10.8, cy, 0.85, nose=3.0, tail=1.2)
        v.box("hull", 5.0, 8.2, cy - 0.5, cy + 0.55, -0.95, 0.95, p)               # cockpit côte à côte
        canopy(v, 5.6, 7.6, cy + 0.55, 1.5, 0.45, frame=p)
        for s in (-1, 1):
            v.box("hull", -8.6, 3.0, cy - 0.8, cy + 0.1, s * 0.8, s * 1.9, p)
            jet_engine(v, -10.2, -8.6, cy - 0.35, s * 1.25, 0.55, intake=False)
            fin(v, p, -9.6, 2.4, 1.0, 2.6, cy + 0.1, z=s * 1.6, sweep=0.9)
            wings(v, p, 3.2, 1.2, 0.5, 2.4, cy + 0.3, 0.12, sweep=0.6, z0=1.0, steps=2)   # plans canard
        wings(v, p, 1.0, 4.6, 1.3, 7.3, cy - 0.1, 0.24, sweep=0.75, z0=1.9, steps=7)
        tailplane(v, p, -9.4, 2.0, 0.8, 4.2, cy - 0.1)
        pylons(v, -0.5, cy - 0.25, (2.6, 3.8, 5.0), 2.5, "missile_g", r=0.14)
        gear(v, -1.0, 2.0, 7.0, cy, 0.42, 0.3)
        roundel_star(v, -8.0, cy + 1.6, 1.68)
        roundel_star(v, -8.0, cy + 1.6, -1.68)
        mount_muzzles(v, (5.5, cy + 0.2, 0.95), missiles=(0.5, cy - 0.5, 2.6), missiles2=(0.5, cy - 0.5, 3.8))
        v.seat(6.6, cy - 0.05, -0.45)
        v.seat(6.6, cy - 0.05, 0.45)
        return v

    @reg
    def su57():
        v = V("su57", 5.0)
        p, cy = "su57_gray", 1.9
        fuselage(v, p, -9.0, 10.0, cy, 0.6, nose=2.6, tail=1.0)
        v.box("hull", -8.0, 4.0, cy - 0.45, cy + 0.25, -2.0, 2.0, p)               # corps porteur aplati
        for s in (-1, 1):
            v.box("hull", 1.0, 4.0, cy - 0.65, cy - 0.15, s * 0.7, s * 1.7, "su57_d")
            jet_engine(v, -9.4, -8.0, cy - 0.15, s * 0.9, 0.5, intake=False)
            fin(v, p, -8.6, 2.0, 0.9, 1.9, cy + 0.25, z=s * 1.8, sweep=0.9, lean=0.45 * s)
        canopy(v, 5.0, 6.8, cy + 0.45, 0.7, 0.42, key="gold_glass")
        wings(v, p, 0.5, 4.6, 1.2, 7.0, cy, 0.18, sweep=0.85, z0=2.0, steps=6)
        tailplane(v, p, -8.8, 2.2, 1.0, 3.9, cy + 0.05, sweep=0.9)
        bomb_bay(v, -3.0, 2.0, cy - 0.45, 1.2, "su57_d")
        gear(v, -1.2, 1.8, 6.0, cy, 0.38, 0.3)
        mount_muzzles(v, (5.0, cy + 0.15, 0.7), missiles=(-0.5, cy - 0.8, 0.4), missiles2=(-1.5, cy - 0.8, 0.4))
        v.seat(5.8, cy - 0.05, 0.0)
        return v

    # ---------------------------------------------------------------- États-Unis
    @reg
    def p51():
        v = V("p51_mustang", 4.0)
        p, cy = "alu", 1.7
        fuselage(v, p, -4.8, 4.6, cy, 0.55, nose=1.4, tail=2.6)
        v.cyl("hull", "x", (cy, 0.0), 0.5, 4.0, 4.6, "red")
        canopy(v, -0.4, 1.0, cy + 0.42, 0.62, 0.5, key="glass")                    # verrière en goutte d'eau
        v.box("hull", -1.6, 1.6, cy - 0.9, cy - 0.5, -0.35, 0.35, p)               # écope de radiateur
        wings(v, p, 1.8, 2.4, 1.0, 5.6, cy - 0.35, 0.26, sweep=0.0, z0=0.5, dihedral=0.08)
        tailplane(v, p, -4.6, 1.0, 0.5, 2.0, cy + 0.15)
        fin(v, p, -4.9, 1.2, 0.6, 1.4, cy + 0.35)
        v.props.append((4.9, cy, 0.0))
        pod(v, 0.9, cy - 0.5, 3.0, 1.4, 0.08, "metal")                             # HVAR
        gear(v, 1.3, 1.5, None, cy, 0.36, tail_wheel=-4.2)
        v.box("hull", -3.0, -2.4, cy - 0.3, cy + 0.3, 0.56, 0.58, "navy")
        v.box("hull", -3.0, -2.4, cy - 0.3, cy + 0.3, -0.58, -0.56, "navy")
        mount_muzzles(v, (2.0, cy - 0.3, 2.2), rockets=(1.2, cy - 0.65, 3.0))
        v.seat(0.3, cy - 0.1, 0.0)
        return v

    # ---------------------------------------------------------------- civil
    @reg
    def light_civil():
        """Petit avion de tourisme biplace (type Cessna 172) : 8 m d'envergure, 6,5 m de long, aile haute haubanée, hélice
        bipale, train tricycle fixe, fuselage blanc à bandes rouge et bleue, verrière panoramique. Aucun armement."""
        v = V("light_civil_plane", 3.0)
        p, cy = "cs_white", 1.25
        fuselage(v, p, -3.2, 2.5, cy, 0.58, nose=0.9, tail=2.4, tail_r=0.25)
        v.cyl("hull", "x", (cy - 0.04, 0.0), 0.52, 1.9, 2.8, p)                      # capot moteur
        v.cyl("hull", "x", (cy - 0.04, 0.0), 0.2, 2.8, 3.1, "cs_red")                # cône d'hélice
        for s in (-1, 1):                                                           # bandes rouge et bleue le long du fuselage
            v.box("hull", -2.6, 1.9, cy + 0.02, cy + 0.17, s * 0.575, s * 0.605, "cs_red")
            v.box("hull", -2.6, 1.9, cy - 0.2, cy - 0.06, s * 0.575, s * 0.605, "cs_blue")
        canopy(v, -1.1, 1.2, cy + 0.38, 1.15, 0.58, key="glass", frame=p)           # verrière panoramique
        wings(v, p, 1.0, 1.5, 1.1, 4.0, cy + 0.98, 0.16, sweep=0.0, z0=0.45, steps=8)  # aile haute : 8 m d'envergure
        for s in (-1, 1):                                                           # haubans de l'aile
            for i in range(4):
                v.box("hull", 0.25, 0.4, cy - 0.45 + i * 0.36, cy - 0.05 + i * 0.36, s * (0.6 + i * 0.45), s * (0.85 + i * 0.45), "metal")
            v.box("hull", 1.0 - 1.5, 1.0, cy + 0.98, cy + 1.14, s * 3.95, s * 4.0, "cs_red")   # saumons colorés
        tailplane(v, p, -3.2, 0.8, 0.55, 1.5, cy + 0.12)
        fin(v, "cs_red", -3.3, 1.0, 0.5, 1.15, cy + 0.2, sweep=0.5)
        v.props.append((3.15, cy - 0.04, 0.0))
        # train d'atterrissage tricycle fixe (toujours visible, donc dans la caisse)
        for z in (-1.0, 1.0):
            v.box("hull", 0.3, 0.45, 0.3, cy - 0.45, z * 0.55, z * 0.55 + 0.06, "metal")
            v.box("hull", 0.3, 0.45, 0.3, 0.42, z * 0.55, z, "metal")
            v.cyl("hull", "z", (0.38, 0.28), 0.28, z - 0.1, z + 0.1, "tire")
        v.box("hull", 1.85, 1.95, 0.25, cy - 0.5, -0.04, 0.04, "metal")
        v.cyl("hull", "z", (1.9, 0.22), 0.22, -0.08, 0.08, "tire")
        v.seat(0.3, cy - 0.1, -0.3)                                                 # pilote (siège gauche)
        v.seat(0.3, cy - 0.1, 0.3)                                                  # passager
        return v

    @reg
    def f86():
        v = V("f86_sabre", 4.0)
        p, cy = "alu", 1.6
        fuselage(v, p, -5.2, 5.6, cy, 0.62, nose=0.6, tail=1.6)
        v.cyl("hull", "x", (cy, 0.0), 0.45, 5.4, 5.7, "black")
        canopy(v, 2.0, 3.5, cy + 0.5, 0.65, 0.48, key="glass")
        wings(v, p, 2.0, 2.6, 1.2, 5.6, cy - 0.25, 0.22, sweep=0.65, z0=0.55, dihedral=0.04)
        fin(v, p, -5.3, 2.0, 0.8, 2.0, cy + 0.4, sweep=0.9)
        tailplane(v, p, -5.0, 1.2, 0.5, 2.2, cy + 0.35, sweep=0.6)
        jet_engine(v, -5.4, -5.0, cy, 0.0, 0.42, intake=False)
        pod(v, 0.3, cy - 0.35, 2.6, 1.2, 0.08, "metal")
        gear(v, 0.3, 1.3, 4.0, cy, 0.3)
        v.box("hull", 0.0, 0.8, cy - 0.3, cy + 0.3, 0.62, 0.64, "navy")
        v.box("hull", 0.0, 0.8, cy - 0.3, cy + 0.3, -0.64, -0.62, "navy")
        mount_muzzles(v, (4.6, cy + 0.1, 0.45), rockets=(0.9, cy - 0.5, 2.6))
        v.seat(2.7, cy - 0.05, 0.0)
        return v

    @reg
    def f4():
        v = V("f4_phantom", 5.0)
        p, cy = "us_camo", 1.9
        fuselage(v, p, -8.6, 9.0, cy, 0.7, nose=2.4, tail=1.4)
        for s in (-1, 1):
            v.box("hull", -6.5, 4.2, cy - 0.75, cy + 0.2, s * 0.65, s * 1.6, p)
            jet_engine(v, -8.9, -7.5, cy - 0.3, s * 0.85, 0.48, intake=False)
        canopy(v, 3.2, 6.2, cy + 0.5, 0.7, 0.45, frame=p)
        wings(v, p, 1.4, 4.4, 1.5, 5.8, cy - 0.1, 0.22, sweep=0.8, z0=1.4, steps=6)
        fin(v, p, -8.6, 2.6, 1.0, 2.4, cy + 0.5, sweep=0.9)
        tailplane(v, p, -8.6, 1.6, 0.7, 2.8, cy + 0.2, sweep=0.5)                 # stabilisateurs à dièdre négatif
        pylons(v, -0.5, cy - 0.25, (2.4, 3.6), 2.5, "missile")
        gear(v, -0.8, 2.2, 5.8, cy, 0.4, 0.3)
        v.box("hull", -6.0, -5.0, cy - 0.3, cy + 0.4, 0.71, 0.73, "navy")
        v.box("hull", -6.0, -5.0, cy - 0.3, cy + 0.4, -0.73, -0.71, "navy")
        mount_muzzles(v, (8.4, cy - 0.5, 0.0), missiles=(0.0, cy - 0.5, 2.4), missiles2=(0.0, cy - 0.5, 3.6))
        v.seat(5.6, cy - 0.05, 0.0)
        v.seat(4.0, cy - 0.05, 0.0)
        return v

    @reg
    def c130():
        v = V("c130_hercules", 13.0)
        p, cy = "us_gray", 3.0
        fuselage(v, p, -14.0, 14.5, cy, 2.0, nose=2.4, tail=5.0, tail_r=0.5)
        canopy(v, 11.6, 13.2, cy + 1.3, 2.0, 0.6, frame=p)
        wings(v, p, 2.8, 4.2, 2.2, 19.5, cy + 1.6, 0.5, sweep=0.1, z0=1.8, steps=10)
        for z in (5.2, 9.8):
            for s in (-1, 1):
                v.cyl("hull", "x", (cy + 1.4, s * z), 0.75, -1.0, 4.4, p)
                v.props.append((4.7, cy + 1.4, s * z))
        fin(v, p, -14.2, 4.2, 2.0, 6.0, cy + 1.6, sweep=0.8, thick=0.3)
        tailplane(v, p, -14.0, 3.0, 1.4, 6.5, cy + 1.7)
        for s in (-1, 1):
            v.box("hull", -2.0, 4.0, 0.9, cy - 1.0, s * 1.8, s * 2.4, p)             # carénages de train
        v.box("bay", -12.0, -7.0, cy - 1.9, cy - 1.0, -1.6, 1.6, p)                  # rampe fermée
        v.box("bay_open", -14.5, -9.5, 0.1, 0.5, -1.6, 1.6, p)                      # rampe abaissée
        gear(v, 1.0, 2.1, 10.0, cy, 0.6, 0.45)
        v.box("hull", -9, -7, cy + 0.2, cy + 1.2, 2.01, 2.03, "navy")
        v.box("hull", -9, -7, cy + 0.2, cy + 1.2, -2.03, -2.01, "navy")
        mount_muzzles(v, (13.0, cy - 1.0, 0.0))
        v.seat(12.2, cy + 0.6, -0.5)
        v.seat(12.2, cy + 0.6, 0.5)
        for i in range(16):
            v.seat(7.0 - (i // 2) * 1.6, cy - 1.4, -1.2 if i % 2 == 0 else 1.2)
        return v

    @reg
    def a10():
        v = V("a10_thunderbolt", 6.0)
        p, cy = "a10_gray", 2.0
        fuselage(v, p, -8.0, 8.0, cy, 0.75, nose=1.6, tail=3.0)
        v.cyl("hull", "x", (cy - 0.45, 0.0), 0.18, 8.0, 9.2, "gun")                # GAU-8 Avenger
        canopy(v, 4.2, 5.8, cy + 0.6, 0.75, 0.5, key="glass", frame=p)
        wings(v, p, 1.8, 3.0, 1.6, 8.7, cy - 0.4, 0.34, sweep=0.0, z0=0.7, steps=8)
        for s in (-1, 1):
            jet_engine(v, -4.6, -1.2, cy + 0.9, s * 1.5, 0.65)                     # réacteurs sur pylônes arrière
            fin(v, p, -8.4, 1.8, 1.2, 2.4, cy + 0.2, z=s * 2.8, sweep=0.2)
        tailplane(v, p, -8.0, 1.6, 1.6, 3.0, cy + 0.2, sweep=0.0)
        pod(v, 0.8, cy - 0.5, 3.4, 2.0, 0.22)
        pod(v, 0.8, cy - 0.5, 5.0, 2.0, 0.22)
        pylons(v, 0.6, cy - 0.45, (6.4,), 2.4, "missile_g", r=0.14)
        gear(v, 1.8, 2.8, 5.5, cy, 0.42, 0.33)
        mount_muzzles(v, (9.2, cy - 0.45, 0.0), rockets=(1.8, cy - 0.75, 4.2), missiles=(1.8, cy - 0.7, 6.4))
        v.seat(5.0, cy, 0.0)
        return v

    @reg
    def f117():
        v = V("f117_nighthawk", 5.0)
        p, cy = "stealth", 1.7
        # carlingue à facettes : pyramide de pavés qui rétrécissent vers l'avant, arêtes vives
        for i in range(9):
            x0, x1 = -6.0 + i * 1.55, -6.0 + (i + 1) * 1.55
            half = max(0.25, 2.4 - abs(i - 3.2) * 0.45 - max(0, i - 4) * 0.25)
            h = max(0.2, 0.9 - abs(i - 3.5) * 0.12)
            v.box("hull", x0, x1, cy - 0.45, cy - 0.45 + h, -half, half, p if i % 2 else "stealth2")
        v.box("hull", 7.9, 8.6, cy - 0.35, cy - 0.15, -0.15, 0.15, p)               # nez pointu
        canopy(v, 3.0, 4.6, cy + 0.2, 0.7, 0.35, key="gold_glass", frame=p)
        wings(v, p, 2.5, 4.8, 0.9, 6.6, cy - 0.35, 0.18, sweep=1.25, z0=1.0, steps=6)   # flèche à 67°
        for s in (-1, 1):
            fin(v, p, -6.6, 1.6, 0.9, 1.8, cy + 0.3, z=s * 0.8, sweep=0.7, lean=0.75 * s)   # empennage en V
            v.box("hull", 0.6, 1.8, cy - 0.4, cy + 0.15, s * 1.0, s * 1.4, "grille")  # entrées grillagées
        v.box("hull", -6.4, -5.6, cy - 0.35, cy - 0.15, -1.0, 1.0, "black")          # tuyères plates
        bomb_bay(v, -2.6, 1.6, cy - 0.45, 1.4, "stealth2")
        gear(v, -1.0, 1.4, 5.5, cy, 0.33, 0.27)
        mount_muzzles(v, (0.0, cy - 0.7, 0.0), missiles=(-0.5, cy - 0.8, 0.35), missiles2=(0.5, cy - 0.8, 0.35))
        v.seat(3.8, cy - 0.15, 0.0)
        return v

    @reg
    def f15e():
        v = V("f15e_strike_eagle", 5.0)
        p, cy = "us_dark", 1.9
        fuselage(v, p, -9.0, 9.6, cy, 0.65, nose=2.6, tail=1.0)
        for s in (-1, 1):
            v.box("hull", -8.0, 4.0, cy - 0.7, cy + 0.25, s * 0.65, s * 1.75, p)
            v.box("hull", 3.8, 4.1, cy - 0.7, cy + 0.2, s * 0.7, s * 1.7, "black")
            jet_engine(v, -9.4, -8.0, cy - 0.25, s * 0.95, 0.55, intake=False)
            fin(v, p, -9.0, 2.2, 1.0, 2.5, cy + 0.25, z=s * 1.5, sweep=0.9)
        canopy(v, 4.0, 7.0, cy + 0.5, 0.75, 0.48, frame=p)
        wings(v, p, 1.0, 4.8, 1.3, 6.5, cy, 0.22, sweep=0.7, z0=1.7, steps=6)
        tailplane(v, p, -9.2, 2.2, 0.9, 4.0, cy + 0.05, sweep=0.6)
        pylons(v, -0.4, cy - 0.2, (2.8, 4.0), 2.5, "missile")
        pylons(v, -2.5, cy - 0.65, (1.0,), 2.0, "dark", r=0.18)                       # GBU-12 conformes
        gear(v, -1.2, 1.8, 6.4, cy, 0.4, 0.3)
        mount_muzzles(v, (4.3, cy + 0.1, 1.7), missiles=(0.0, cy - 0.45, 2.8), missiles2=(0.0, cy - 0.45, 4.0))
        v.seat(6.2, cy, 0.0)
        v.seat(4.8, cy, 0.0)
        return v

    @reg
    def f16():
        v = V("f16_falcon", 4.0)
        p, cy = "us_gray", 1.7
        fuselage(v, p, -7.0, 7.6, cy, 0.55, nose=2.2, tail=1.0)
        v.box("hull", -2.0, 3.2, cy - 0.9, cy - 0.45, -0.55, 0.55, p)                # entrée d'air ventrale
        v.box("hull", 3.2, 3.4, cy - 0.88, cy - 0.48, -0.5, 0.5, "black")
        canopy(v, 3.6, 5.6, cy + 0.42, 0.6, 0.48, key="gold_glass")                # verrière sans montants
        wings(v, p, 1.4, 3.6, 1.0, 4.7, cy - 0.05, 0.18, sweep=0.75, z0=0.6, steps=6)
        fin(v, p, -7.0, 2.2, 0.9, 2.4, cy + 0.4, sweep=0.9)
        tailplane(v, p, -6.8, 1.5, 0.6, 2.7, cy - 0.05, sweep=0.6)
        jet_engine(v, -7.3, -6.8, cy, 0.0, 0.45, intake=False)
        pylons(v, -0.5, cy - 0.15, (2.2, 3.2), 2.2, "missile")
        v.box("hull", -1.0, 1.0, cy - 0.05, cy + 0.05, 4.65, 4.75, "missile")      # rails de bout d'aile
        gear(v, -0.6, 1.2, 3.8, cy, 0.33, 0.27)
        mount_muzzles(v, (3.8, cy + 0.2, -0.55), rockets=(0.0, cy - 0.45, 3.2), missiles=(0.0, cy - 0.4, 2.2))
        v.seat(4.5, cy - 0.05, 0.0)
        return v

    @reg
    def b2():
        v = V("b2_spirit", 12.0)
        p, cy = "stealth", 2.2
        # aile volante : chevron de pavés de plus en plus fins vers les saumons, bord de fuite en dents de scie
        for i in range(16):
            za, zb = i * 1.07, (i + 1) * 1.07
            xl = 7.5 - za * 0.62
            chord = 15.0 - za * 0.78
            th = max(0.25, 1.6 - i * 0.09)
            for s in (-1, 1):
                a, b = sorted((s * za, s * zb))
                v.box("hull", xl - chord, xl, cy - th / 2, cy + th / 2, a, b, p if i % 2 else "stealth2")
        v.box("hull", -6.0, 6.5, cy, cy + 1.2, -2.6, 2.6, p)                          # bosse du cockpit et des réacteurs
        canopy(v, 3.8, 5.8, cy + 1.0, 1.4, 0.4, key="gold_glass")
        for s in (-1, 1):
            v.box("hull", 0.5, 2.0, cy + 0.6, cy + 1.3, s * 2.4, s * 4.0, "grille")
            v.box("hull", -6.5, -5.0, cy + 0.3, cy + 0.9, s * 2.0, s * 4.0, "black")
        bomb_bay(v, -2.5, 2.5, cy - 0.8, 3.2, "stealth2")
        gear(v, 0.5, 2.8, 5.5, cy, 0.55, 0.4)
        mount_muzzles(v, (4.0, cy - 0.9, 0.0))
        v.seat(4.8, cy + 0.6, -0.4)
        v.seat(4.8, cy + 0.6, 0.4)
        return v

    @reg
    def f22():
        v = V("f22_raptor", 5.0)
        p, cy = "f22_gray", 1.8
        fuselage(v, p, -8.8, 9.0, cy, 0.62, nose=2.4, tail=0.8)
        v.box("hull", -8.0, 4.4, cy - 0.5, cy + 0.2, -2.0, 2.0, p)
        for s in (-1, 1):
            v.box("hull", 2.5, 4.6, cy - 0.7, cy - 0.1, s * 0.75, s * 1.8, "f22_d")       # entrées d'air en losange
            jet_engine(v, -9.2, -8.0, cy - 0.15, s * 0.8, 0.5, intake=False)
            fin(v, p, -8.0, 2.4, 1.0, 2.0, cy + 0.2, z=s * 1.7, sweep=0.8, lean=0.4 * s)
        canopy(v, 4.8, 6.8, cy + 0.42, 0.7, 0.45, key="gold_glass")
        wings(v, p, 1.4, 5.4, 1.2, 6.8, cy, 0.18, sweep=0.75, z0=2.0, steps=6)
        tailplane(v, p, -8.9, 2.0, 1.0, 3.8, cy + 0.05, sweep=0.9)
        bomb_bay(v, -3.0, 2.0, cy - 0.5, 1.4, "f22_d")
        gear(v, -1.4, 1.7, 5.6, cy, 0.36, 0.28)
        mount_muzzles(v, (3.8, cy + 0.15, 1.75), missiles=(-0.5, cy - 0.9, 0.4), missiles2=(-1.5, cy - 0.9, 0.4))
        v.seat(5.6, cy - 0.05, 0.0)
        return v

    return planes


PLANE_COLORS = {
    "ww2_green": ["#4a5a32", "#45542e", "#4f5f36"], "ww2_green_d": "#2f3a20",
    "la7_gray": ["#7a8590", "#737e89", "#808b96"], "silver": ["#b8bcc0", "#b0b4b8", "#c0c4c8"], "silver_d": "#8e9296",
    "sov_gray": ["#8e9ba3", "#87949c", "#95a2aa"], "sov_camo": ["#6b7046", "#8a7a52", "#5d6440"], "sov_d": "#3a3e2a",
    "mig29_gray": ["#a4aeb4", "#9aa4aa", "#b0b9be"], "su34_blue": ["#6f8ea6", "#68879f", "#7795ad"],
    "su57_gray": ["#8a959c", "#6f7a82", "#9aa5ac"], "su57_d": "#4a5258",
    "us_camo": ["#6d6a4c", "#55603e", "#7a6d50"], "us_gray": ["#8c949c", "#858d95", "#939ba3"], "us_dark": ["#5d6670", "#57606a", "#636c76"],
    "a10_gray": ["#6b7366", "#656d60", "#71796c"], "stealth": ["#1c1e22", "#202226", "#191b1f"], "stealth2": "#2a2c31",
    "f22_gray": ["#7d858c", "#777f86", "#838b92"], "f22_d": "#565d63", "gold_glass": "#b89a40", "grille": "#3a3c40",
    "cs_white": ["#f2f2f0", "#eaeae8", "#f6f6f4"], "cs_red": "#c0281f", "cs_blue": "#2b4a9e",
}

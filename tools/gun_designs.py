"""
Formes 3D des armes à feu (utilisé par generate_gun_models.py).

Repère : le canon pointe vers +X, le haut est +Y, l'arme est centrée sur Z = 8.
Chaque arme = une liste de pavés (comme dans Blockbench) + son chargeur à part + ses points de fixation.

Pour la plupart des armes, on utilise des "constructeurs" paramétrés (pistol, rifle, bullpup...) :
il suffit de changer les longueurs, les couleurs et les options pour obtenir une nouvelle arme.

Couleurs (clés de la palette) :
  k = métal sombre   m = métal   l = métal clair   G = acier   d = noir
  w = bois sombre    W = bois clair
  t / T / u = kaki   g = or   r = rubis   s = verre
"""

from contextlib import contextmanager

KEYS = ["k", "m", "l", "w", "W", "r", "d", "s", "t", "T", "u", "g", "G"]


def cell_uv(color):
    i = KEYS.index(color)
    cx, cy = (i % 4) * 4, (i // 4) * 4
    return [cx, cy, cx + 4, cy + 4]


def box(frm, to, color, rot=None):
    """Un pavé. rot = (axe, angle, origine) avec angle dans -45, -22.5, 0, 22.5, 45."""
    uv = cell_uv(color)
    el = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to],
          "faces": {f: {"uv": uv, "texture": "#p"} for f in ("north", "east", "south", "west", "up", "down")}}
    if rot:
        axis, angle, origin = rot
        el["rotation"] = {"angle": angle, "axis": axis, "origin": origin}
    return el


class Gun:
    """Une arme en construction : corps, chargeur (séparé) et points de fixation."""

    def __init__(self):
        self.body, self.mag, self.slide = [], [], []
        self.mounts = {}
        self.flags = {}
        self.slide_info = None
        self._moving = False
        # Pièces d'origine remplaçables par un accessoire (poignée pistolet, crosse)
        self.parts = {"grip": [], "stock": []}
        self._part = None

    def add(self, frm, to, c, rot=None, mag=False):
        if mag:
            target = self.mag
        elif self._moving:
            target = self.slide
        elif self._part:
            target = self.parts[self._part]
        else:
            target = self.body
        target.append(box(frm, to, c, rot))

    @contextmanager
    def part(self, name):
        """Les pavés ajoutés dans ce bloc forment une pièce remplaçable ("grip" ou "stock")."""
        self._part = name
        try:
            yield self
        finally:
            self._part = None

    @contextmanager
    def moving(self, style, travel=0.0, pivot=None):
        """Les pavés ajoutés dans ce bloc forment la pièce mobile (culasse, pompe, canons...).
        style : "fast" (aller-retour rapide), "bolt" (verrou manuel), "pump" (pompe), "spin" (rotation)."""
        self.slide_info = {"style": style, "travel": travel}
        if pivot:
            self.slide_info["pivot"] = pivot
        self._moving = True
        try:
            yield self
        finally:
            self._moving = False

    def bolt(self, x, y0, y1, face, travel=1.6, style="fast", handle=True):
        """Culasse visible dans la fenêtre d'éjection (+ levier d'armement) qui recule au tir."""
        with self.moving(style, travel):
            self.add([x, y0, 8 + face + 0.06], [x + 1.8, y1, 8 + face + 0.12], "l")
            if handle:
                ym = (y0 + y1) / 2
                self.add([x + 1.5, ym - 0.25, 8 + face + 0.12], [x + 2.0, ym + 0.25, 8 + face + 0.7], "k")

    def zb(self, x0, y0, x1, y1, hw, c, rot=None, mag=False):
        """Pavé centré sur Z = 8, de demi-largeur hw."""
        self.add([x0, y0, 8 - hw], [x1, y1, 8 + hw], c, rot, mag)

    def mount(self, slot, x, y):
        self.mounts[slot] = [round(x, 3), round(y, 3), 8]

    # ---------- pièces communes ----------
    def barrel(self, x0, x1, y, r=0.4, c="k"):
        self.zb(x0, y - r, x1, y + r, r, c)
        self.zb(x1 - 0.05, y - r * 0.5, x1 + 0.05, y + r * 0.5, r * 0.5, "d")  # bouche
        self.mount("muzzle", x1 + 0.05, y)

    def muzzle_device(self, x, y, r, kind):
        """Cache-flamme, frein de bouche... posé au bout du canon (x)."""
        if kind == "flash":
            self.zb(x - 0.8, y - r - 0.15, x + 0.6, y + r + 0.15, r + 0.15, "k")
            end = x + 0.6
        elif kind == "brake":
            self.zb(x - 1.0, y - r - 0.25, x + 0.8, y + r + 0.25, r + 0.25, "l")
            self.zb(x - 0.6, y + r + 0.25, x + 0.2, y + r + 0.3, 0.2, "d")
            end = x + 0.8
        elif kind == "big":
            self.zb(x - 1.5, y - 1.0, x + 1.5, y + 1.0, 0.95, "m")
            self.zb(x - 0.8, y + 1.0, x + 0.8, y + 1.05, 0.5, "d")
            end = x + 1.5
        elif kind == "compensator":
            self.zb(x - 2.0, y - r - 0.2, x + 0.4, y + r + 0.2, r + 0.2, "m")
            for i in range(3):
                self.zb(x - 1.8 + i * 0.7, y + r + 0.2, x - 1.5 + i * 0.7, y + r + 0.25, 0.25, "d")
            end = x + 0.4
        else:
            return
        self.zb(end - 0.05, y - r * 0.5, end + 0.05, y + r * 0.5, r * 0.5, "d")
        self.mount("muzzle", end + 0.05, y)

    def grip(self, x, ytop, c="k", length=3.5, w=1.6, angle=-22.5):
        """Poignée pistolet, inclinée vers l'arrière (pièce remplaçable)."""
        with self.part("grip"):
            self.zb(x, ytop - length, x + w, ytop, 0.7, c, rot=("z", angle, [x + w / 2, ytop, 8]))
        self.mount("pistol_grip", x + w / 2, ytop)

    def trigger(self, x, y):
        """Pontet + détente sous la carcasse (y = bas de la carcasse)."""
        self.zb(x, y - 1.1, x + 2.0, y - 0.75, 0.4, "k")
        self.zb(x + 1.75, y - 1.1, x + 2.1, y, 0.4, "k")
        self.zb(x + 0.6, y - 0.7, x + 0.95, y, 0.2, "d")

    def post(self, x, y, h, w=0.5):
        self.zb(x, y, x + w, y + h, 0.25, "k")

    def rail(self, x0, x1, y):
        self.zb(x0, y, x1, y + 0.45, 0.55, "k")
        x = x0 + 0.4
        while x < x1 - 0.4:  # crans du rail Picatinny
            self.add([x, y + 0.45, 7.5], [x + 0.25, y + 0.5, 8.5], "d")
            x += 0.9
        self.mount("sight", (x0 + x1) / 2, y + 0.45)

    def side_detail(self, x0, y0, x1, y1, face, c="d", both=False):
        """Détail plat collé sur le flanc d'une pièce (face = demi-largeur de la pièce)."""
        self.add([x0, y0, 8 + face], [x1, y1, 8 + face + 0.06], c)
        if both:
            self.add([x0, y0, 8 - face - 0.06], [x1, y1, 8 - face], c)

    def scope(self, x0, x1, y, r=0.8, c="k"):
        """Lunette de visée : tube, oculaire, objectif, verres et montages (y = base)."""
        cy = y + 1.2 + r
        self.zb(x0 + 1.2, cy - r, x1 - 1.6, cy + r, r, c)
        self.zb(x0, cy - r - 0.2, x0 + 1.2, cy + r + 0.2, r + 0.2, c)
        self.zb(x1 - 1.6, cy - r - 0.35, x1, cy + r + 0.35, r + 0.35, c)
        self.zb(x1 - 0.05, cy - r, x1 + 0.05, cy + r, r, "s")
        self.zb(x0 - 0.05, cy - r + 0.1, x0 + 0.05, cy + r - 0.1, r - 0.1, "s")
        self.zb(x0 + 2, y, x0 + 2.8, cy - r, 0.4, "m")
        self.zb(x1 - 3, y, x1 - 2.2, cy - r, 0.4, "m")

    def bipod(self, x, y):
        """Bipied replié sous le canon."""
        self.zb(x - 0.5, y - 0.6, x + 0.5, y, 0.6, "k")
        for z in (7.3, 8.5):
            self.add([x, y - 0.55, z], [x + 5, y - 0.25, z + 0.25], "k")

    def carry_handle(self, x0, x1, y, h=1.4, c="k"):
        self.zb(x0, y, x0 + 0.7, y + h, 0.35, c)
        self.zb(x1 - 0.7, y, x1, y + h, 0.35, c)
        self.zb(x0, y + h, x1, y + h + 0.5, 0.45, c)

    def stock(self, kind, rear, x0, y0, y1, c, bore):
        """Crosse entre rear (arrière) et x0 (début du boîtier). y0/y1 = bas/haut du boîtier (pièce remplaçable)."""
        self.mount("stock", x0, (y0 + y1) / 2)
        with self.part("stock"):
            self._stock(kind, rear, x0, y0, y1, c, bore)

    def _stock(self, kind, rear, x0, y0, y1, c, bore):
        if kind in ("fixed", "wood", "polymer"):
            self.zb(rear, y0 - 1.0, x0, y1 - 0.6, 0.8, c)
            self.zb(rear - 0.3, y0 - 1.4, rear, y1 - 0.4, 0.85, "k")
        elif kind == "tube":
            self.zb(rear + 2.5, bore - 0.5, x0, bore + 0.5, 0.45, "k")
            self.zb(rear, y0 - 0.8, rear + 3, y1 - 0.2, 0.8, c)
        elif kind == "skeleton":
            self.zb(rear, y1 - 1.1, x0, y1 - 0.6, 0.55, c)
            self.zb(rear, y0 - 0.9, x0, y0 - 0.4, 0.55, c, rot=("z", 0, [x0, y0, 8]))
            self.zb(rear, y0 - 0.9, rear + 0.7, y1 - 0.6, 0.7, c)
        elif kind == "thumbhole":
            self.zb(rear, y1 - 1.4, x0, y1 - 0.4, 0.8, c)
            self.zb(rear, y0 - 1.6, x0 - 2.5, y0 - 0.2, 0.8, c)
            self.zb(rear - 0.3, y0 - 1.8, rear, y1 - 0.3, 0.85, "k")
        elif kind == "folded":
            self.add([x0, y0 + 0.4, 8.9], [x0 + 6, y1 - 0.6, 9.3], c)

    def magazine(self, kind, x, ytop, length, c, grip_bottom=None):
        """Chargeur d'origine (séparé du corps). Le point de fixation est le haut du chargeur."""
        if kind == "straight":
            self.zb(x - 1, ytop - length, x + 1, ytop, 0.6, c, mag=True)
            self.zb(x - 1.05, ytop - length - 0.3, x + 1.05, ytop - length, 0.65, "k", mag=True)
        elif kind == "curved":
            self.zb(x - 1, ytop - length, x + 1, ytop, 0.6, c, rot=("z", 22.5, [x, ytop, 8]), mag=True)
        elif kind == "drum":
            self.zb(x - 0.8, ytop - 1.5, x + 0.8, ytop, 0.6, "k", mag=True)
            cy = ytop - 1.5 - length / 2
            self.zb(x - length / 2, cy - length / 2, x + length / 2, cy + length / 2, 1.2, c, mag=True)
            self.zb(x - length / 2, cy - length / 2, x + length / 2, cy + length / 2, 1.2, c,
                    rot=("z", 45, [x, cy, 8]), mag=True)
            self.zb(x - 0.8, cy - 0.8, x + 0.8, cy + 0.8, 1.3, "l", mag=True)
        elif kind == "box":
            self.zb(x - 2, ytop - length, x + 2, ytop, 1.3, c, mag=True)
            self.zb(x - 2.1, ytop - 0.5, x + 2.1, ytop, 1.35, "u", mag=True)
        if kind == "grip" and grip_bottom:
            self.mount("magazine", *grip_bottom)
        elif kind:
            self.mount("magazine", x, ytop)


# =====================================================================
# Constructeurs
# =====================================================================
def pistol(slide_c="k", frame_c="k", grip_c="k", x0=3.0, x1=13.5, sy0=8.0, sy1=10.4, grip_len=5.0,
           top=None, open_slide=False, accent=True):
    g = Gun()
    by = (sy0 + sy1) / 2
    g.zb(x0 + 2, by - 0.45, x1 - 0.2, by + 0.45, 0.45, "G")    # canon (visible quand la culasse recule)
    g.zb(x0, sy0 - 1, x1 - 0.6, sy0, 0.9, frame_c)              # carcasse
    with g.moving("fast", travel=(x1 - x0) * 0.22):             # la culasse entière recule au tir
        g.zb(x0, sy0, x1, sy1, 0.95, slide_c)                   # culasse
        g.zb(x0, sy1, x1, sy1 + 0.4, 0.75, top or slide_c)      # dessus
        for i in range(4):                                      # stries de préhension
            g.side_detail(x0 + 0.5 + i * 0.45, sy0 + 0.4, x0 + 0.7 + i * 0.45, sy1 - 0.4, 0.95, both=True)
        g.side_detail(x0 + 5, sy1 - 1.0, x0 + 7, sy1 - 0.15, 0.95)  # fenêtre d'éjection
        if open_slide:  # Beretta : canon visible sur le dessus
            g.zb(x1 - 4.5, sy1 - 0.2, x1, sy1 + 0.55, 0.5, "G")
        g.zb(x1 - 0.05, by - 0.45, x1 + 0.05, by + 0.45, 0.45, "d")
        g.post(x1 - 0.8, sy1 + 0.4, 0.5)
        g.post(x0 + 0.3, sy1 + 0.4, 0.5, w=0.8)
    g.mount("muzzle", x1 + 0.05, by)
    gx = x0 + 0.1
    g.grip(gx, sy0 - 1, grip_c, length=grip_len, w=2.8)
    g.trigger(x0 + 3.0, sy0 - 1)
    if accent:
        g.zb(x0 + 6.5, sy0 - 0.8, x0 + 7.5, sy0 - 0.2, 1.05, "r")
    g.mount("sight", (x0 + x1) / 2, sy1 + 0.4)
    g.mount("stock", x0, sy0 - 0.5)  # attelle (brace) à l'arrière de la carcasse
    cx = gx + 1.4
    g.magazine("grip", 0, 0, 0, None, grip_bottom=(cx - 0.383 * grip_len, sy0 - 1 - 0.924 * grip_len))
    return g


def rifle(stock="polymer", stock_c="k", rear=-3.0, x0=3.0, x1=10.0, x2=14.5, x3=19.0,
          rc="m", ry0=6.4, ry1=9.4, hc="k", hy0=6.8, hy1=9.2, bore=8.0, br=0.4, bc="k",
          grip_x=4.0, grip_c="k", mag=("straight", 8.5, 4.0, "m"), muzzle="flash",
          fsight=True, rsight=False, rail=True, carry=None, gas=False, accent=True, bipod=False,
          scope=None, upper=None, action="fast"):
    """Fusil "classique" : crosse | boîtier | garde-main | canon.
    action = "fast" (semi-auto / automatique) ou "bolt" (fusil à verrou : levier manœuvré après le tir)."""
    g = Gun()
    g.stock(stock, rear, x0, ry0, ry1, stock_c, bore)
    g.zb(x0, ry0, x1, ry1, 0.9, rc)                              # boîtier
    if upper:  # partie haute du boîtier d'une autre couleur
        g.zb(x0, (ry0 + ry1) / 2 + 0.3, x1, ry1 + 0.05, 0.92, upper)
    g.zb(x1, hy0, x2, hy1, 0.85, hc)                             # garde-main
    g.side_detail(x0 + 3.2, (ry0 + ry1) / 2 - 0.1, x0 + 5.4, ry1 - 0.5, 0.9)   # fenêtre d'éjection
    if action == "bolt":
        g.bolt(x0 + 3.3, (ry0 + ry1) / 2, ry1 - 0.6, 0.9, travel=2.6, style="bolt")
    else:
        g.bolt(x0 + 3.3, (ry0 + ry1) / 2, ry1 - 0.6, 0.9, travel=1.6)
    for i in range(int((x2 - x1 - 1) / 1.3)):                    # fentes de refroidissement
        vx = x1 + 0.7 + i * 1.3
        g.side_detail(vx, (hy0 + hy1) / 2 - 0.3, vx + 0.6, (hy0 + hy1) / 2 + 0.3, 0.85, both=True)
    if gas:  # AK : tube de gaz au-dessus du canon
        g.zb(x1, hy1 + 0.2, x2 - 0.5, hy1 + 1.0, 0.6, hc)
    g.barrel(x2, x3, bore, br, bc)
    g.muzzle_device(x3, bore, br, muzzle)
    if fsight:
        g.post(x3 - 2.5, bore + br, 1.6 if not gas else 1.8)
    if rsight:
        g.post(x1 - 0.6, ry1, 0.6, w=0.6)
    if rail:
        g.rail(x0 + 0.5, x2 - 0.5 if rail == "long" else x1, ry1)
    else:
        g.mount("sight", (x0 + x1) / 2, ry1)
    if carry:
        g.carry_handle(x0 + 0.5, x1 - 0.5, ry1, h=carry)
        g.mount("sight", (x0 + x1) / 2, ry1 + carry + 0.5)
    if scope:
        sx0, sx1 = scope
        g.scope(sx0, sx1, ry1 + (0.45 if rail else 0))
    g.grip(grip_x, ry0, grip_c)
    g.trigger(grip_x + 1.8, ry0)
    if accent:
        g.zb(x0 + 4.5, ry0 + 0.6, x0 + 6, ry0 + 1.2, 0.95, "r")
    g.mount("grip", (x1 + x2) / 2, hy0)
    if bipod:
        g.bipod(x2 + 0.5, bore - br)
    if mag:
        kind, mx, length, mc = mag
        g.magazine(kind, mx, ry0, length, mc)
    return g


def bullpup(body_c="k", rear=-3.0, x1=11.0, x2=14.5, x3=18.5, ry0=5.5, ry1=9.3, bore=7.8,
            grip_x=8.0, mag_x=3.8, mag_len=3.2, mag_c="m", scope_top=False, rail=True, front_grip=False,
            hc=None, top_mag=False):
    """Bullpup : le chargeur est derrière la poignée (AUG, Tavor, P90)."""
    g = Gun()
    g.zb(rear, ry0, x1, ry1, 0.95, body_c)
    g.zb(rear - 0.3, ry0 - 0.3, rear, ry1, 1.0, "k")
    g.side_detail(1.0, (ry0 + ry1) / 2, 3.2, ry1 - 0.6, 0.95)  # fenêtre d'éjection
    if not top_mag:
        g.bolt(1.1, (ry0 + ry1) / 2 + 0.1, ry1 - 0.7, 0.95, travel=1.4)
    g.zb(x1, ry0 + 1.2, x2, ry1 - 0.4, 0.85, hc or body_c)
    g.barrel(x2, x3, bore, 0.4, "k")
    g.muzzle_device(x3, bore, 0.4, "flash")
    g.grip(grip_x, ry0, "k", length=3.3)
    g.trigger(grip_x + 1.6, ry0)
    if front_grip:
        g.zb(x1 - 0.5, ry0 - 2.5, x1 + 0.8, ry0 + 1.2, 0.65, "k")
    if scope_top:
        g.scope(1.0, x1 - 0.5, ry1, r=0.9, c=body_c)
        g.mount("sight", 4.5, ry1)
    elif rail:
        g.rail(rear + 4, x1, ry1)
    if top_mag:  # P90 : chargeur posé sur le dessus
        g.zb(rear + 1, ry1, x1 - 1, ry1 + 0.9, 0.75, "s", mag=True)
        g.mount("sight", x1 - 2, ry1 + 0.9)
    else:
        g.magazine("straight", mag_x, ry0, mag_len, mag_c)
    g.zb(2.5, ry0 + 1.5, 4.0, ry0 + 2.1, 1.0, "r")
    g.mount("grip", (x1 + x2) / 2, ry0 + 1.2)
    return g


def shotgun(stock="wood", stock_c="W", pump_c="W", rc="k", x0=3.0, x1=8.0, x3=19.5, pump=(10, 14),
            ghost=False, shield=False, mag_tube=True):
    g = Gun()
    ry0, ry1 = 7.0, 10.0
    g.stock(stock, -2.5, x0, ry0, ry1, stock_c, 8.5)
    g.zb(x0, ry0, x1, ry1, 1.0, rc)
    g.barrel(x1, x3, 9.4, 0.55, "k")
    if mag_tube:
        g.zb(x1, 7.4, x3 - 2, 8.5, 0.5, "l")
    if pump:
        with g.moving("pump", travel=2.6):
            g.zb(pump[0], 6.9, pump[1], 8.9, 0.85, pump_c)
    else:
        g.bolt(x0 + 1.5, 8.2, 9.4, 1.0, travel=1.4)
    if shield:  # SPAS-12 : protection perforée sur le canon
        g.zb(x1 + 1, 9.0, x3 - 3, 10.3, 0.75, "k")
        for i in range(5):
            g.zb(x1 + 2 + i * 1.6, 10.3, x1 + 2.6 + i * 1.6, 10.35, 0.4, "d")
    g.post(x3 - 0.8, 9.95, 0.4)
    if ghost:
        g.carry_handle(x1 - 1.6, x1 - 0.4, ry1, h=0.8)
    g.grip(x0 + 0.5, ry0, stock_c if stock in ("wood", "fixed") else "k")
    g.trigger(x0 + 2.3, ry0)
    g.zb(x0 + 1.5, ry0 + 1.2, x0 + 3.5, ry0 + 1.8, 1.05, "r")
    g.mount("sight", (x0 + x1) / 2, ry1)
    g.mount("grip", (pump[0] + pump[1]) / 2 if pump else 12, 6.9)
    return g


# =====================================================================
# Les armes (nom de l'item -> forme)
# =====================================================================
def design_glock17():
    return pistol(slide_c="k", frame_c="k", grip_c="k", x1=13.0)


def design_desert_eagle():
    g = pistol(slide_c="G", frame_c="k", grip_c="k", x1=14.5, sy1=11.0, top="l", grip_len=5.2)
    g.zb(5, 11.4, 14.5, 11.7, 0.4, "G")  # nervure triangulaire du canon
    return g


def design_beretta_m9():
    return pistol(slide_c="k", frame_c="k", grip_c="k", x1=13.8, open_slide=True)


def design_colt_m1911():
    return pistol(slide_c="G", frame_c="G", grip_c="W", x1=13.5, sy1=10.2)


def design_makarov():
    return pistol(slide_c="m", frame_c="m", grip_c="k", x1=12.0, sy1=10.1, grip_len=4.4)


def design_colt_python():
    g = Gun()
    g.zb(3, 7, 9, 10, 0.8, "G")                       # carcasse
    g.zb(5, 7.6, 8.5, 10.4, 1.3, "G")                 # barillet
    for y in (8.1, 9.0, 9.9):
        g.zb(5.2, y, 8.3, y + 0.15, 1.35, "k")        # cannelures
    g.zb(9, 8.6, 16, 9.8, 0.55, "G")                  # canon
    g.zb(9, 9.8, 16, 10.3, 0.35, "G")                 # bande ventilée
    g.zb(9, 7.9, 16, 8.6, 0.5, "G")                   # tenon sous le canon
    g.zb(15.95, 8.9, 16.05, 9.5, 0.3, "d")
    g.zb(2.6, 10, 3.6, 11, 0.3, "k")                  # chien
    g.post(15.2, 10.3, 0.5)
    g.grip(2.5, 7, "W", length=4.5, w=2.5)
    g.trigger(5.5, 7)
    g.mount("sight", 10.5, 10.3)
    g.mount("muzzle", 16.05, 9.2)
    return g


def design_mp5():
    g = rifle(stock="polymer", stock_c="k", rear=-1.5, x0=3, x1=11, x2=14, x3=15.5, rc="k", hc="k",
              hy0=6.9, hy1=8.9, bore=7.9, mag=("curved", 9.0, 4.2, "k"), muzzle=None, rail=False,
              fsight=True, rsight=True)
    g.zb(13.8, 8.9, 14.6, 10.2, 0.45, "k")           # guidon protégé
    return g


def design_uzi():
    g = Gun()
    g.zb(2.5, 6.5, 12.5, 9.5, 0.95, "m")              # boîtier
    g.zb(2.5, 9.5, 12.5, 9.9, 0.7, "k")
    g.barrel(12.5, 14.5, 8.0, 0.45, "k")
    g.grip(6.2, 6.5, "k", length=4.5, w=2.0, angle=0)  # la poignée contient le chargeur
    g.zb(6.4, 1.6, 8.0, 2.0, 0.6, "k", mag=True)      # bas du chargeur
    g.trigger(8.4, 6.5)
    with g.part("stock"):
        g.add([3, 7.2, 8.95], [9, 8.4, 9.25], "k")    # crosse repliée sur le côté
    g.mount("stock", 2.5, 8.0)
    g.post(11.6, 9.9, 0.7)
    with g.moving("fast", travel=2.0):                # levier d'armement sur le dessus
        g.zb(9, 9.9, 10, 10.5, 0.35, "k")
    g.zb(4, 7.2, 5.5, 7.8, 1.0, "r")
    g.mount("sight", 7, 9.9)
    g.mount("grip", 11, 6.5)
    g.mount("magazine", 7.2, 2.0)
    return g


def design_p90():
    g = bullpup(body_c="k", rear=-1.5, x1=11.5, x2=13, x3=14.5, ry0=5.6, ry1=9.0, bore=7.8,
                grip_x=7.5, top_mag=True, rail=False)
    g.zb(10.5, 5.6, 11.6, 7.4, 0.7, "k")              # trou pour le pouce (avant)
    return g


def design_mp7():
    g = Gun()
    g.zb(3, 6.6, 11, 9.4, 0.9, "k")
    g.rail(3.5, 10.5, 9.4)
    g.barrel(11, 13.5, 8.2, 0.4)
    g.grip(4.8, 6.6, "k", length=4.2, w=1.8, angle=0)  # chargeur dans la poignée
    g.zb(5.0, 2.0, 6.6, 2.4, 0.6, "k", mag=True)
    g.trigger(6.8, 6.6)
    g.zb(9.5, 3.8, 10.6, 6.6, 0.6, "k")               # poignée avant repliable
    g.bolt(6, 7.6, 8.8, 0.9, travel=1.2)
    g.stock("tube", -1, 3, 6.6, 9.4, "k", 8.2)
    g.zb(5, 7.2, 6.5, 7.8, 0.95, "r")
    g.mount("grip", 9.5, 6.6)
    g.mount("magazine", 5.8, 2.4)
    return g


def design_thompson():
    g = rifle(stock="wood", stock_c="W", rear=-3.5, x0=3, x1=10.5, x2=12.5, x3=17.5, rc="m",
              hc="W", hy0=5.5, hy1=7.0, bore=8.4, br=0.5, grip_c="W", mag=("drum", 8.5, 5.0, "k"),
              muzzle="compensator", rail=False, fsight=True, rsight=True)
    for i in range(6):  # ailettes de refroidissement
        g.zb(12.7 + i * 0.7, 7.85, 13.0 + i * 0.7, 8.95, 0.6, "m")
    return g


def design_vector():
    g = Gun()
    g.zb(2, 5.5, 11, 9.6, 0.95, "k")                 # boîtier anguleux
    g.zb(8, 4.5, 11, 5.5, 0.9, "k")
    g.rail(2.5, 10.5, 9.6)
    g.barrel(11, 14, 8.6, 0.4)
    g.grip(4.5, 5.5, "k", length=3.3)
    g.trigger(6.3, 5.5)
    g.magazine("straight", 9.5, 4.5, 3.5, "k")
    g.stock("skeleton", -2.5, 2, 5.8, 9.4, "k", 8.0)
    g.bolt(5, 7.5, 8.8, 0.95, travel=1.2)
    g.zb(3, 6.5, 4.5, 7.1, 1.0, "r")
    g.mount("grip", 12.5, 7.8)
    return g


def design_ppsh41():
    g = rifle(stock="wood", stock_c="W", rear=-3.5, x0=3, x1=9.5, x2=10, x3=18, rc="W", ry0=6.0,
              ry1=9.0, hc="W", hy0=6.5, hy1=7.8, bore=8.6, br=0.4, grip_c="W", mag=("drum", 9.0, 5.4, "k"),
              muzzle=None, rail=False, fsight=True, rsight=True)
    g.zb(9.5, 7.9, 16.5, 9.4, 0.8, "m")               # manchon perforé
    for i in range(5):
        g.zb(10 + i * 1.3, 9.4, 10.6 + i * 1.3, 9.45, 0.45, "d")
    return g


def design_ak74():
    return rifle(stock="wood", stock_c="w", rear=-3, x0=3, x1=10.5, x2=14.5, x3=18.5, rc="m", ry0=6.5,
                 ry1=9.0, hc="w", hy0=6.6, hy1=8.4, bore=7.8, grip_c="w", mag=("curved", 9.0, 4.4, "w"),
                 muzzle="brake", rail=False, gas=True, rsight=True)


def design_hk416():
    return rifle(stock="tube", stock_c="k", rear=-3.5, x0=3, x1=10, x2=15.5, x3=19.5, rc="m", upper="k",
                 hc="k", hy0=6.8, hy1=9.4, bore=8.2, rail="long", mag=("straight", 8.5, 3.9, "k"),
                 muzzle="flash", fsight=False)


def design_scar_h():
    return rifle(stock="skeleton", stock_c="T", rear=-3, x0=3, x1=10.5, x2=15, x3=19.5, rc="k", upper="T",
                 hc="T", hy0=7.0, hy1=9.4, bore=8.2, rail="long", mag=("straight", 8.5, 3.2, "k"),
                 muzzle="brake", fsight=False, grip_c="k")


def design_g36():
    return rifle(stock="skeleton", stock_c="k", rear=-3, x0=3, x1=10.5, x2=14.5, x3=18.5, rc="k", hc="k",
                 hy0=6.8, hy1=9.2, bore=8.0, rail=False, carry=1.8, mag=("curved", 8.8, 4.0, "l"),
                 muzzle="flash", fsight=False)


def design_aug():
    return bullpup(body_c="T", rear=-3, x1=10.5, x2=13.5, x3=18, ry0=5.8, ry1=9.0, bore=7.8, grip_x=8.0,
                   mag_x=3.8, mag_c="l", scope_top=True, front_grip=True)


def design_tavor():
    return bullpup(body_c="k", rear=-3, x1=11.5, x2=14.5, x3=18, ry0=5.6, ry1=9.4, bore=7.8, grip_x=8.3,
                   mag_x=3.8, mag_c="k", hc="T")


def design_m16a4():
    return rifle(stock="fixed", stock_c="k", rear=-4, x0=3, x1=10, x2=15.5, x3=21.5, rc="m", upper="m",
                 hc="k", hy0=6.9, hy1=9.4, bore=8.2, rail=True, mag=("straight", 8.5, 3.9, "m"),
                 muzzle="flash", fsight=True)


def design_g3():
    g = rifle(stock="fixed", stock_c="k", rear=-4, x0=3, x1=11, x2=15, x3=21, rc="k", hc="k", hy0=6.9,
              hy1=8.9, bore=8.2, rail=False, mag=("straight", 8.8, 3.4, "k"), muzzle="flash",
              fsight=True, rsight=True)
    g.zb(14.8, 8.6, 15.6, 10.2, 0.5, "k")
    return g


def design_an94():
    return rifle(stock="folded", stock_c="k", rear=-1, x0=3, x1=10.5, x2=14.5, x3=19, rc="k", ry0=6.5,
                 ry1=9.0, hc="k", hy0=6.6, hy1=8.6, bore=7.8, grip_c="k", mag=("curved", 9.4, 4.2, "k"),
                 muzzle="brake", rail=True, rsight=False)


def design_barrett_m82():
    g = rifle(stock="fixed", stock_c="k", rear=-4, x0=3, x1=11, x2=13, x3=25, rc="k", upper="m", ry0=6.0,
              ry1=9.6, hc="m", hy0=6.6, hy1=9.6, bore=8.4, br=0.55, mag=("straight", 9.0, 2.6, "k"),
              muzzle="big", fsight=False, rail=True, carry=None, bipod=True, scope=(2.5, 10.5))
    return g


def design_awm():
    return rifle(stock="thumbhole", stock_c="T", rear=-4, x0=3, x1=10, x2=11, x3=25, rc="T", ry0=6.2,
                 ry1=9.0, hc="T", hy0=6.6, hy1=8.6, bore=8.0, br=0.45, grip_c="T", mag=("straight", 8.5, 1.8, "k"),
                 muzzle="brake", fsight=False, rail=True, bipod=True, scope=(2.0, 10.5), action="bolt")


def design_dragunov():
    return rifle(stock="thumbhole", stock_c="W", rear=-4, x0=3, x1=10.5, x2=15, x3=24, rc="m", ry0=6.5,
                 ry1=9.0, hc="W", hy0=6.6, hy1=8.6, bore=7.9, br=0.4, grip_c="W", mag=("curved", 9.0, 3.0, "k"),
                 muzzle="flash", fsight=True, rail=False, scope=(3.0, 10.0), gas=True)


def design_mosin_nagant():
    g = Gun()
    g.zb(-4, 5.4, 3, 8.4, 0.8, "W")                   # crosse
    g.zb(-4.3, 5.0, -4, 8.6, 0.85, "k")
    g.zb(3, 6.6, 20, 8.0, 0.75, "W")                  # fût en bois jusqu'à l'avant
    g.zb(3, 8.0, 9, 9.0, 0.7, "m")                    # boîtier
    g.barrel(9, 24, 8.4, 0.4)
    with g.moving("bolt", travel=2.6):                # levier de verrou
        g.zb(5.5, 8.6, 6.0, 8.9, 1.6, "m")
        g.zb(5.5, 8.3, 6.4, 9.0, 0.5, "m")
    g.post(23.0, 8.8, 1.0)
    g.post(9.5, 9.0, 0.6, w=0.8)
    g.trigger(4.2, 6.6)
    g.zb(-1, 6.0, 1, 6.6, 0.85, "r")
    g.mount("sight", 6.5, 9.0)
    g.mount("grip", 15, 6.6)
    return g


def design_kar98k():
    g = Gun()
    g.zb(-4, 5.2, 3, 8.3, 0.8, "w")
    g.zb(-4.3, 4.8, -4, 8.5, 0.85, "k")
    g.zb(3, 6.6, 19, 8.0, 0.75, "w")
    g.zb(3, 8.0, 9, 9.0, 0.7, "k")
    g.barrel(9, 23.5, 8.4, 0.4)
    with g.moving("bolt", travel=2.6):                # levier coudé
        g.zb(5.0, 7.8, 5.5, 8.6, 1.6, "k", rot=("x", 22.5, [5.2, 8.6, 8.8]))
    g.post(22.7, 8.8, 1.0)
    g.post(10, 9.0, 0.6, w=0.8)
    g.trigger(4.2, 6.6)
    g.zb(-1, 5.8, 1, 6.4, 0.85, "r")
    g.mount("sight", 6.5, 9.0)
    g.mount("grip", 15, 6.6)
    return g


def design_m14_ebr():
    return rifle(stock="tube", stock_c="k", rear=-4, x0=3, x1=11, x2=16, x3=21, rc="k", hc="k", hy0=6.6,
                 hy1=9.4, bore=8.2, rail="long", mag=("straight", 9.0, 3.2, "k"), muzzle="brake", fsight=False)


def design_remington_870():
    return shotgun(stock="wood", stock_c="W", pump_c="W", rc="k")


def design_benelli_m4():
    return shotgun(stock="polymer", stock_c="k", pump_c="k", rc="k", pump=None, ghost=True)


def design_spas12():
    return shotgun(stock="skeleton", stock_c="k", pump_c="k", rc="k", x3=18.5, shield=True)


def design_aa12():
    g = Gun()
    g.zb(3, 6.6, 13, 10.0, 1.0, "k")                  # boîtier carré
    with g.part("stock"):
        g.zb(-3, 6.4, 3, 9.6, 0.85, "k")              # crosse droite
    g.mount("stock", 3, 8.3)
    g.barrel(13, 18, 8.8, 0.55)
    g.carry_handle(4, 12, 10.0, h=1.0)
    g.bolt(7, 8.2, 9.4, 1.0, travel=1.5)
    g.grip(4.2, 6.6, "k")
    g.trigger(6.0, 6.6)
    g.magazine("drum", 9.5, 6.6, 5.0, "k")
    g.zb(4, 8.6, 6, 9.2, 1.05, "r")
    g.mount("sight", 8, 11.5)
    g.mount("grip", 12, 6.6)
    return g


def design_sawed_off():
    g = Gun()
    g.zb(0, 5.5, 4, 8.6, 0.9, "W")                    # crosse courte
    g.grip(0.2, 6.5, "W", length=3.5, w=2.0)
    g.zb(4, 7.4, 6.5, 9.6, 1.0, "G")                  # bascule
    for z in (7.45, 8.55):                            # deux canons côte à côte
        g.add([6.5, 8.1, z - 0.55], [12.5, 9.4, z + 0.55], "k")
        g.add([12.45, 8.4, z - 0.3], [12.55, 9.1, z + 0.3], "d")
    g.zb(6.5, 7.2, 10, 8.1, 0.8, "W")                 # garde-main
    g.trigger(3.0, 7.4)
    g.mount("muzzle", 12.55, 8.75)
    g.mount("sight", 9, 9.4)
    return g


def design_m249():
    return rifle(stock="fixed", stock_c="k", rear=-4, x0=3, x1=12, x2=15.5, x3=21, rc="k", ry0=6.2, ry1=9.8,
                 hc="k", hy0=6.6, hy1=9.0, bore=8.0, br=0.45, mag=("box", 8.5, 4.0, "T"), muzzle="flash",
                 fsight=True, rail=True, carry=1.6, bipod=True)


def design_pkm():
    return rifle(stock="thumbhole", stock_c="W", rear=-4, x0=3, x1=12, x2=13, x3=22, rc="m", ry0=6.2,
                 ry1=9.4, hc="m", hy0=7.6, hy1=8.6, bore=8.2, br=0.45, grip_c="W", mag=("box", 8.0, 4.4, "T"),
                 muzzle="flash", fsight=True, rail=False, bipod=True)


def design_m60():
    return rifle(stock="fixed", stock_c="k", rear=-4, x0=3, x1=12, x2=16, x3=22.5, rc="k", ry0=6.2, ry1=9.4,
                 hc="k", hy0=6.6, hy1=9.0, bore=8.0, br=0.5, mag=("box", 8.0, 4.4, "T"), muzzle="flash",
                 fsight=True, rail=False, bipod=True, carry=1.4)


def design_minigun():
    g = Gun()
    g.zb(0, 6.0, 8, 10.6, 1.5, "k")                   # moteur / boîtier
    g.zb(8, 6.4, 9, 10.2, 1.7, "m")                   # plateau avant
    with g.moving("spin", pivot=[8.3, 8]):            # six canons qui tournent pendant le tir
        for y, z in ((7.3, 7.3), (7.3, 8.7), (8.3, 6.6), (8.3, 9.4), (9.3, 7.3), (9.3, 8.7)):
            g.add([9, y - 0.3, z - 0.3], [21, y + 0.3, z + 0.3], "k")
        g.zb(14.5, 6.8, 15.2, 9.8, 1.5, "m")          # colliers
        g.zb(20.4, 6.8, 21.0, 9.8, 1.5, "m")
    g.zb(1, 10.6, 2, 12.6, 0.4, "k")                  # poignées
    g.zb(5, 10.6, 6, 12.6, 0.4, "k")
    g.zb(1, 12.6, 6, 13.2, 0.45, "k")
    g.grip(2.5, 6.0, "k", length=3.0)
    g.zb(-3, 4.0, 2, 8.0, 1.6, "T", mag=True)         # caisse de munitions
    g.zb(-3.1, 7.6, 2.1, 8.0, 1.65, "u", mag=True)
    g.zb(3, 6.8, 6, 7.4, 1.55, "r")
    g.mount("muzzle", 21.05, 8.3)
    g.mount("sight", 4, 13.2)
    return g


def design_m32_mgl():
    g = Gun()
    g.zb(5, 6.2, 11, 11.0, 1.8, "k")                  # barillet de 6
    g.zb(5, 6.2, 11, 11.0, 1.8, "m", rot=("x", 45, [8, 8.6, 8]))
    g.barrel(11, 17, 8.6, 1.1, "k")
    g.zb(3, 7.2, 5, 10, 1.0, "k")
    g.rail(5, 16, 11.0)
    g.stock("tube", -3, 3, 7.2, 10.0, "k", 8.6)
    g.grip(3.2, 7.2, "k")
    g.trigger(4.4, 7.2)
    g.zb(12.5, 4.0, 13.8, 7.5, 0.65, "k")             # poignée avant
    g.zb(6, 6.0, 8, 6.4, 1.85, "r")
    g.mount("grip", 14.5, 7.5)
    return g


def design_m79():
    g = Gun()
    g.zb(-4, 5.4, 4, 8.8, 0.9, "W")                   # crosse en bois
    g.zb(-4.3, 5.0, -4, 9.0, 0.95, "k")
    g.zb(4, 7.0, 7, 10.0, 1.1, "k")                   # bascule
    g.barrel(7, 15.5, 8.8, 1.4, "T")                  # gros canon de 40 mm
    g.zb(5, 6.4, 13, 7.4, 0.9, "W")                   # garde-main
    g.zb(7.5, 10.2, 8.3, 11.6, 0.6, "k")              # hausse à échelle
    g.trigger(4.5, 7.0)
    g.zb(0, 6.4, 2, 7.0, 0.95, "r")
    g.mount("sight", 9, 10.2)
    return g


def design_at4():
    g = Gun()
    g.zb(-6, 6.6, 18, 9.6, 1.5, "T")                  # tube jetable
    g.zb(-6.3, 6.4, -5, 9.8, 1.65, "u")
    g.zb(17, 6.4, 18.3, 9.8, 1.65, "u")
    g.zb(18.25, 7.0, 18.35, 9.2, 1.1, "d")
    g.zb(-6.35, 7.0, -6.25, 9.2, 1.1, "d")
    g.zb(4, 9.6, 5, 10.6, 0.5, "k")                   # viseurs repliés
    g.zb(10, 9.6, 11, 10.4, 0.5, "k")
    g.grip(5.5, 6.6, "k", length=3.0)
    g.zb(9, 3.8, 10.3, 6.6, 0.65, "k")
    g.zb(13, 7.6, 15, 8.6, 1.55, "r")
    g.mount("muzzle", 18.35, 8.1)
    g.mount("sight", 7, 9.6)
    return g


# Armes conçues à la main (déjà existantes)
def design_ak47():
    g = Gun()
    with g.part("stock"):
        g.zb(-3, 4.8, 3, 8, 0.75, "W")
        g.zb(-3.3, 4.4, -3, 8.3, 0.8, "k")
    g.mount("stock", 3, 7.6)
    g.zb(3, 6.5, 10.5, 9, 0.9, "m")
    g.zb(3, 9, 10, 9.6, 0.7, "l")
    g.grip(4, 6.5, "w", length=3.5)
    g.trigger(5.8, 6.5)
    g.zb(10.5, 6.6, 14.5, 8.4, 0.85, "W")
    g.zb(10.5, 8.6, 14, 9.4, 0.6, "W")
    g.barrel(14.5, 19, 7.8, 0.35)
    g.zb(18.6, 7.3, 19.3, 8.3, 0.45, "k")
    g.mount("muzzle", 19.35, 7.8)
    g.post(16.5, 8.2, 1.6, w=0.7)
    g.post(10.2, 9.4, 0.5, w=0.6)
    g.zb(4, 7, 5, 7.5, 1.05, "r")
    g.bolt(6.5, 7.6, 8.6, 0.9, travel=2.0)            # levier d'armement de l'AK
    g.zb(8, 3, 10, 6.5, 0.6, "m", rot=("z", 22.5, [9, 6.5, 8]), mag=True)
    g.zb(9.3, 1, 11.1, 3.6, 0.6, "m", rot=("z", 45, [10.2, 3.6, 8]), mag=True)
    g.mount("sight", 6.5, 9.6)
    g.mount("grip", 12.5, 6.6)
    g.mount("magazine", 9, 6.5)
    return g


def design_m4():
    g = rifle(stock="tube", stock_c="k", rear=-3.5, x0=3, x1=10, x2=15, x3=19.5, rc="m", upper="m",
              hc="k", hy0=7.0, hy1=9.6, bore=8.2, rail=True, mag=("straight", 8.5, 3.9, "m"),
              muzzle="flash", fsight=True)
    return g


def design_famas():
    g = Gun()
    g.zb(-3, 5.5, 10, 9.5, 0.9, "k")
    g.zb(-3.3, 5, -3, 9.7, 0.85, "m")
    g.zb(-2, 9.5, 0, 10, 0.6, "m")
    g.bolt(1.0, 7.6, 8.8, 0.9, travel=1.4)
    g.carry_handle(0, 12, 9.5, h=2.0)
    g.zb(10, 6.5, 14, 9.2, 0.85, "m")
    g.barrel(14, 18, 7.8, 0.4)
    g.muzzle_device(18, 7.8, 0.4, "flash")
    g.grip(7.5, 5.5, "k", length=3.5)
    g.zb(9, 3.5, 11, 4, 0.4, "k")
    g.zb(10.6, 4, 11, 5.5, 0.4, "k")
    g.zb(5, 7, 6, 7.5, 1.05, "r")
    g.magazine("straight", 3.9, 5.5, 3.0, "m")
    g.mount("sight", 6, 12.0)
    g.mount("grip", 12, 6.5)
    return g


def design_rpg7():
    g = Gun()
    g.zb(-4, 7, 14, 8.6, 0.8, "m")
    g.zb(-6, 6.6, -4, 9, 1.2, "k")
    g.zb(-6.1, 7.2, -6, 8.4, 0.6, "d")
    g.zb(2, 6.7, 8, 8.9, 1.1, "W")
    g.grip(3.5, 6.7, "k", length=3.7, w=1.5)
    g.zb(5.2, 5.6, 6.8, 6, 0.4, "k")
    g.zb(8.5, 3.8, 9.8, 6.8, 0.65, "k")
    g.post(12, 8.6, 1.0)
    g.post(3, 8.6, 0.8)
    g.zb(13.9, 7.4, 14.1, 8.2, 0.6, "d")
    # Roquette PG-7 (visible seulement quand l'arme est chargée)
    g.zb(14, 7.2, 16, 8.4, 0.6, "t", mag=True)
    g.zb(16, 6.4, 19, 9.2, 1.4, "T", mag=True)
    g.zb(19, 6.9, 20.5, 8.7, 0.9, "T", mag=True)
    g.zb(20.5, 7.5, 22, 8.1, 0.3, "u", mag=True)
    g.mount("sight", 6.5, 8.9)
    g.flags["mag_needs_ammo"] = True
    return g


GUNS = {name[len("design_"):]: fn for name, fn in globals().items() if name.startswith("design_")}

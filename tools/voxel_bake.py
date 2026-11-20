#!/usr/bin/env python3
"""
"Cuisson" des modèles 3D voxel : de vraies textures détaillées, façon Minecraft (pixel art, pas de lissage).

Avant : chaque face d'un pavé affichait une case unie d'une petite palette (16 couleurs), étirée sur toute la face.
Maintenant, pour chaque modèle :
1. les pièces longues sont "arrondies" en marches : leurs arêtes sont chanfreinées (section octogonale en escalier),
   donc rondes mais pas lisses ;
2. chaque face reçoit sa propre zone dans une texture (atlas), à 3 pixels de texture par pixel de modèle ;
3. chaque pixel est peint selon la matière : métal brossé et rayé, bois veiné, polymère grainé, tissu tissé, cuir,
   peinture mate, verre ; avec des arêtes en relief (haut éclairé, bas dans l'ombre) et un camouflage continu
   d'une face à l'autre (calculé dans l'espace 3D) ;
4. les icônes d'inventaire sont dessinées à partir de ces textures (vue de profil, contour, ombrage).

Les pavés doivent porter leur couleur dans "__key" (ajouté par les fonctions box() des générateurs).
"""
import json
import math
import random
import zlib

from PIL import Image, ImageDraw

D = 3          # pixels de texture par pixel de modèle
PAD = 1        # marge autour de chaque face dans l'atlas (évite les bavures avec les mipmaps)

FACES = ("north", "south", "east", "west", "up", "down")


# ------------------------------------------------------------------ bruit
def _hash(*a):
    return zlib.crc32(repr(a).encode()) & 0xFFFFFFFF


def rnd(*a):
    return (_hash(*a) % 100000) / 100000.0


def noise3(seed, x, y, z, cell=1.0):
    """Bruit de valeur 3D interpolé (entre -1 et 1)."""
    fx, fy, fz = x / cell, y / cell, z / cell
    ix, iy, iz = math.floor(fx), math.floor(fy), math.floor(fz)
    tx, ty, tz = fx - ix, fy - iy, fz - iz
    tx, ty, tz = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty), tz * tz * (3 - 2 * tz)

    def v(a, b, c):
        return rnd(seed, ix + a, iy + b, iz + c) * 2 - 1
    x00 = v(0, 0, 0) * (1 - tx) + v(1, 0, 0) * tx
    x10 = v(0, 1, 0) * (1 - tx) + v(1, 1, 0) * tx
    x01 = v(0, 0, 1) * (1 - tx) + v(1, 0, 1) * tx
    x11 = v(0, 1, 1) * (1 - tx) + v(1, 1, 1) * tx
    y0 = x00 * (1 - ty) + x10 * ty
    y1 = x01 * (1 - ty) + x11 * ty
    return y0 * (1 - tz) + y1 * tz


def hexc(h, a=255):
    h = h.lstrip("#")
    return [int(h[i:i + 2], 16) for i in (0, 2, 4)] + [a]


def clamp8(v):
    return max(0, min(255, int(round(v))))


# ------------------------------------------------------------------ peintre (couleurs + matières)
class Painter:
    """
    colors : clé -> "#rrggbb" ou liste de couleurs (camouflage)
    material : fonction clé -> "metal" | "wood" | "polymer" | "fabric" | "leather" | "paint" | "flat" | "glass" | "gem"
    alpha : clé -> transparence (0-255) pour le verre
    """

    def __init__(self, colors, material, alpha=None, seed="p", glass_tint=None):
        self.colors = colors
        self.material = material
        self.alpha = alpha or {}
        self.seed = seed
        self.glass_tint = glass_tint or {}

    def base(self, key, p):
        v = self.colors[key]
        if isinstance(v, list):
            # camouflage : taches de ~1,5 pixel de modèle, continues d'une face à l'autre
            scores = [noise3((self.seed, key, i), p[0], p[1], p[2], cell=1.6) for i in range(len(v))]
            return hexc(v[scores.index(max(scores))])
        return hexc(v)

    def texel(self, key, p, normal, s, t, tw, th, axis_long):
        """Couleur d'un pixel de face. p = point 3D (pixels de modèle), (s, t) = position dans la face (0-1)."""
        mat = self.material(key)
        c = self.base(key, p)
        f = 1.0
        x, y, z = p
        jitter = rnd(self.seed, key, round(x * 3), round(y * 3), round(z * 3)) - 0.5
        if mat == "metal":
            along = p[axis_long]
            cross = sum(p) - along
            # métal brossé : stries fines dans le sens de la pièce + taches d'usure + rayures brillantes
            streak = rnd(key, "brush", round(cross * 3)) - 0.5
            f = 1 + 0.08 * noise3((key, "m"), x, y, z, cell=2.0) + 0.09 * streak + jitter * 0.1
            if rnd(key, "scratch", round(x * 3), round(y * 3), round(z * 3)) < 0.025:
                f *= 1.3
            elif rnd(key, "pit", round(x * 3), round(y * 3), round(z * 3)) < 0.02:
                f *= 0.8
        elif mat == "wood":
            cross = (p[1] if axis_long != 1 else p[0]) + 0.7 * noise3((key, "w"), x, y, z, cell=3)
            ring = 0.5 + 0.5 * math.sin(cross * 8.0)
            f = 0.84 + 0.26 * ring + jitter * 0.08
            if ring < 0.12:
                f *= 0.86                                    # veine sombre
            if rnd(key, "knot", round(x), round(y), round(z)) < 0.015:
                f *= 0.7
        elif mat == "polymer":
            f = 1 + 0.1 * noise3((key, "g"), x, y, z, cell=0.7) + jitter * 0.14
        elif mat == "fabric":
            weave = ((round(s * tw) + round(t * th)) % 2) * 0.09 - 0.045
            f = 1 + weave + 0.09 * noise3((key, "f"), x, y, z, cell=1.8) + jitter * 0.06
        elif mat == "leather":
            f = 1 + 0.09 * noise3((key, "l"), x, y, z, cell=0.9) + jitter * 0.05
            if rnd(key, "crease", round(x * 2), round(y * 2), round(z * 2)) < 0.04:
                f *= 0.82
        elif mat == "paint":
            f = 1 + 0.08 * noise3((key, "p"), x, y, z, cell=1.5) + jitter * 0.1
            if rnd(key, "chip", round(x * 3), round(y * 3), round(z * 3)) < 0.012:
                c = [min(255, v + 45) for v in c[:3]] + [c[3]]  # éclat de peinture (métal dessous)
        elif mat == "gem":
            f = 1.25 if (round(s * tw) + round(t * th)) % 5 < 2 else (0.8 if (round(s * tw) - round(t * th)) % 5 == 0 else 1.0)
        elif mat == "glass":
            f = 1.2 - 0.4 * (s + t) / 2
            if abs(s - t) < 0.12 and s < 0.6:
                f = 1.5
        # Arêtes en relief : bord haut éclairé, bas dans l'ombre, côtés un peu sombres (rond, mais en pixels)
        if mat not in ("glass", "flat") and tw >= 3 and th >= 3:
            i, j = round(s * tw - 0.5), round(t * th - 0.5)
            edge_l, edge_r, edge_t, edge_b = i == 0, i == tw - 1, j == 0, j == th - 1
            if normal == "up":
                if edge_l or edge_r or edge_t or edge_b:
                    f *= 1.2
            elif normal == "down":
                if edge_l or edge_r or edge_t or edge_b:
                    f *= 0.8
            else:
                if edge_t:
                    f *= 1.32
                elif edge_b:
                    f *= 0.64
                if edge_l or edge_r:
                    f *= 0.84
                if j == 1 and th >= 6:
                    f *= 1.1
                elif j == th - 2 and th >= 6:
                    f *= 0.9
        a = c[3]
        if key in self.alpha:
            a = self.alpha[key]
            if mat == "glass" and (s < 0.08 or t < 0.08 or s > 0.92 or t > 0.92):
                a = min(255, a + 28)
        # partie additive : les matières très sombres (noir, acier bruni) montrent aussi leur grain et leurs arêtes
        add = (f - 1) * 34 if mat not in ("glass", "flat") else 0
        out = [clamp8(c[0] * f + add), clamp8(c[1] * f + add), clamp8(c[2] * f + add), a]
        return tuple(out)


# ------------------------------------------------------------------ arrondi (chanfrein en marches)
def chamfer(elements, skip_keys=()):
    """Pièce longue et épaisse -> deux pavés croisés : section octogonale en escalier (arêtes arrondies)."""
    out = []
    for e in elements:
        key = e.get("__key")
        fr, to = e["from"], e["to"]
        dims = [to[i] - fr[i] for i in range(3)]
        long_axis = dims.index(max(dims))
        others = [i for i in range(3) if i != long_axis]
        small = min(dims[others[0]], dims[others[1]])
        if key in skip_keys or small < 1.0 or dims[long_axis] < 1.4:
            out.append(e)
            continue
        c = max(0.33, min(0.6, small * 0.22))
        a, b = others
        e1 = json.loads(json.dumps(e))
        e2 = json.loads(json.dumps(e))
        e1["from"][a] = round(fr[a] + c, 3)
        e1["to"][a] = round(to[a] - c, 3)
        e2["from"][b] = round(fr[b] + c, 3)
        e2["to"][b] = round(to[b] - c, 3)
        # le 2e pavé est un poil plus court : ses bouts ne se battent pas avec ceux du 1er (pas de scintillement)
        e2["from"][long_axis] = round(fr[long_axis] + 0.02, 3)
        e2["to"][long_axis] = round(to[long_axis] - 0.02, 3)
        out += [e1, e2]
    return out


# ------------------------------------------------------------------ géométrie des faces
def face_size(e, face):
    dx = e["to"][0] - e["from"][0]
    dy = e["to"][1] - e["from"][1]
    dz = e["to"][2] - e["from"][2]
    return {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}[face]


def face_point(e, face, s, t):
    """Point 3D d'une face pour (s, t) dans [0,1] (mêmes sens que les UV automatiques de Minecraft)."""
    (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
    if face == "north":
        return (x1 - s * (x1 - x0), y1 - t * (y1 - y0), z0)
    if face == "south":
        return (x0 + s * (x1 - x0), y1 - t * (y1 - y0), z1)
    if face == "east":
        return (x1, y1 - t * (y1 - y0), z1 - s * (z1 - z0))
    if face == "west":
        return (x0, y1 - t * (y1 - y0), z0 + s * (z1 - z0))
    if face == "up":
        return (x0 + s * (x1 - x0), y1, z0 + t * (z1 - z0))
    return (x0 + s * (x1 - x0), y0, z1 - t * (z1 - z0))


def texel_dims(e, face, d=D):
    w, h = face_size(e, face)
    return max(1, round(w * d)), max(1, round(h * d))


# ------------------------------------------------------------------ cuisson
class Baked:
    def __init__(self, size, rects, groups):
        self.w, self.h = size
        self.size = max(size)       # compatibilité (plus grand côté)
        self.rects = rects          # (id élément, face) -> (x, y, w, h)
        self.groups = groups        # nom -> éléments (avec "__key", sans UV définitifs)


def _shelf(items, w, h):
    rects, x, y, row = {}, 0, 0, 0
    for k, iw, ih in items:
        if iw > w:
            return None
        if x + iw > w:
            x, y, row = 0, y + row, 0
        if y + ih > h:
            return None
        rects[k] = (x + PAD, y + PAD, iw - 2 * PAD, ih - 2 * PAD)
        x += iw
        row = max(row, ih)
    return rects


def layout(groups, d=D):
    """Range toutes les faces de tous les groupes dans un atlas (rayonnage), carré ou deux fois plus large que haut :
    on garde la plus petite surface (moins de mémoire graphique)."""
    items = []
    for gname, els in groups.items():
        for idx, e in enumerate(els):
            for f in FACES:
                tw, th = texel_dims(e, f, d)
                items.append(((gname, idx, f), tw + 2 * PAD, th + 2 * PAD))
    items.sort(key=lambda it: (-it[2], -it[1]))
    # largeur = puissance de 2, hauteur = multiple de 16 (compatible avec les mipmaps de Minecraft)
    need = sum(w * h for _, w, h in items)
    cands = sorted((w * h, -w, w, h) for w in (16, 32, 64, 128, 256, 512, 1024) for h in range(16, w + 1, 16) if w * h >= need)
    for _, _, w, h in cands:
        rects = _shelf(items, w, h)
        if rects is not None:
            return (w, h), rects
    raise ValueError("modèle trop grand pour un atlas 1024x1024")


def bake(groups, d=D, skip_chamfer=()):
    """Prépare un ensemble de modèles (groupes de pavés) : arrondi + rangement des faces. Renvoie un Baked."""
    rounded = {g: chamfer(els, skip_chamfer) for g, els in groups.items()}
    size, rects = layout(rounded, d)
    return Baked(size, rects, rounded)


def paint(baked, painter):
    """Peint l'atlas d'un Baked avec un peintre (un skin)."""
    img = Image.new("RGBA", (baked.w, baked.h), (0, 0, 0, 0))
    px = img.load()
    for (g, idx, f), (x, y, w, h) in baked.rects.items():
        e = baked.groups[g][idx]
        key = e["__key"]
        dims = [e["to"][i] - e["from"][i] for i in range(3)]
        axis_long = dims.index(max(dims))
        for j in range(h):
            for i in range(w):
                s, t = (i + 0.5) / w, (j + 0.5) / h
                px[x + i, y + j] = painter.texel(key, face_point(e, f, s, t), f, s, t, w, h, axis_long)
        # marge : on recopie les bords (pas de bavure de couleur voisine)
        for i in range(-PAD, w + PAD):
            for j in range(-PAD, h + PAD):
                if 0 <= i < w and 0 <= j < h:
                    continue
                si, sj = min(max(i, 0), w - 1), min(max(j, 0), h - 1)
                if 0 <= x + i < baked.w and 0 <= y + j < baked.h:
                    px[x + i, y + j] = px[x + si, y + sj]
    return img


def elements(baked, group, texture="#p"):
    """Les pavés d'un groupe avec leurs UV dans l'atlas (prêts à écrire dans un modèle JSON)."""
    out = []
    kx, ky = 16 / baked.w, 16 / baked.h
    for idx, e in enumerate(baked.groups[group]):
        el = {"from": e["from"], "to": e["to"], "faces": {}}
        if "rotation" in e:
            el["rotation"] = e["rotation"]
        for f in FACES:
            x, y, w, h = baked.rects[(group, idx, f)]
            el["faces"][f] = {"uv": [round(x * kx, 4), round(y * ky, 4), round((x + w) * kx, 4), round((y + h) * ky, 4)], "texture": texture}
        out.append(el)
    return out


# ------------------------------------------------------------------ icônes d'inventaire
OUTLINE = (16, 17, 20, 255)


def _poly(e):
    (x0, y0, _), (x1, y1, _) = e["from"], e["to"]
    pts = [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
    rot = e.get("rotation")
    if rot and rot["axis"] == "z" and rot["angle"]:
        ox, oy, _ = rot["origin"]
        a = math.radians(rot["angle"])
        pts = [(ox + (x - ox) * math.cos(a) - (y - oy) * math.sin(a), oy + (x - ox) * math.sin(a) + (y - oy) * math.cos(a)) for x, y in pts]
    return pts


def icon(baked, atlas, groups, size=32, margin=1):
    """Vue de profil (côté +Z) dessinée avec les vraies textures des faces, contour sombre et ombre portée."""
    els = [(g, i, e) for g in groups for i, e in enumerate(baked.groups[g])]
    polys = [_poly(e) for _, _, e in els]
    xs = [x for p in polys for x, _ in p]
    ys = [y for p in polys for _, y in p]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    scale = min((size - 2 * margin - 1) / max(0.01, max(xs) - min(xs)), (size - 2 * margin - 1) / max(0.01, max(ys) - min(ys)))
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out = img.load()
    apx = atlas.load()
    order = sorted(range(len(els)), key=lambda n: els[n][2]["to"][2])
    owner = [[-1] * size for _ in range(size)]
    depth = [[-1e9] * size for _ in range(size)]
    for n in order:
        g, idx, e = els[n]
        pts = polys[n]
        mask = Image.new("1", (size, size), 0)
        ImageDraw.Draw(mask).polygon([((x - cx) * scale + size / 2 - 0.5, size / 2 - 0.5 - (y - cy) * scale) for x, y in pts], fill=1)
        mp = mask.load()
        x, y, w, h = baked.rects[(g, idx, "south")]
        (x0, y0, _), (x1, y1, _) = e["from"], e["to"]
        rot = e.get("rotation")
        for py in range(size):
            for pxl in range(size):
                if not mp[pxl, py]:
                    continue
                mx = (pxl + 0.5 - size / 2) / scale + cx
                my = (size / 2 - (py + 0.5)) / scale + cy
                if rot and rot["axis"] == "z" and rot["angle"]:
                    ox, oy, _ = rot["origin"]
                    a = -math.radians(rot["angle"])
                    mx, my = ox + (mx - ox) * math.cos(a) - (my - oy) * math.sin(a), oy + (mx - ox) * math.sin(a) + (my - oy) * math.cos(a)
                s = min(0.999, max(0, (mx - x0) / max(0.001, x1 - x0)))
                t = min(0.999, max(0, (y1 - my) / max(0.001, y1 - y0)))
                c = apx[x + int(s * w), y + int(t * h)]
                if c[3] < 40:
                    continue
                out[pxl, py] = c[:3] + (255,)
                owner[py][pxl] = n
                depth[py][pxl] = e["to"][2]
    # ombrage d'ensemble façon pixel art : lumière en haut à gauche, contour sombre,
    # liseré entre les pièces (une pièce devant une autre est détourée), reflets francs
    res = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    rp = res.load()

    def opaque(px, py):
        return 0 <= px < size and 0 <= py < size and out[px, py][3]

    for py in range(size):
        for pxl in range(size):
            c = out[pxl, py]
            if c[3] == 0:
                if any(opaque(pxl + dx, py + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    rp[pxl, py] = OUTLINE
                continue
            f, add = 1.0, 0
            if not opaque(pxl, py - 1):
                f, add = 1.3, 22                      # arête supérieure éclairée
            elif not opaque(pxl - 1, py):
                f, add = 1.15, 10                     # arête gauche
            elif not opaque(pxl, py + 1):
                f = 0.7                               # dessous dans l'ombre
            elif not opaque(pxl + 1, py):
                f = 0.85
            else:
                # séparation entre pièces : le pixel voisin appartient à une pièce plus en avant
                me = depth[py][pxl]
                for dx, dy, k in ((0, -1, 0.72), (-1, 0, 0.8), (0, 1, 1.12), (1, 0, 1.06)):
                    o = owner[py + dy][pxl + dx]
                    if o != owner[py][pxl] and depth[py + dy][pxl + dx] > me + 0.01:
                        f = k
                        break
            rp[pxl, py] = (clamp8(c[0] * f + add), clamp8(c[1] * f + add), clamp8(c[2] * f + add), 255)
    return res


def strip(els):
    """Supprime l'annotation "__key" (pour les modèles qui gardent l'ancienne palette)."""
    out = json.loads(json.dumps(els))
    for e in out:
        e.pop("__key", None)
    return out

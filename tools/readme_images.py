#!/usr/bin/env python3
"""
Images du README (docs/images/*.png), générées à partir des ressources du mod (aucune capture d'écran du jeu) :
- galeries d'icônes d'objets (armes, accessoires, munitions, équipements, drones...) à partir des textures ;
- rendus isométriques des véhicules, avions, navires, du train et de l'avion civil, à partir des modèles voxel
  (tools/generate_military_vehicles.py, aircraft_models.py, ship_models.py, train_models.py) ;
- planche des factions (skins de bots vus de face) ;
- plans des bases du scénario (ScenarioSites) et schéma du réseau ferroviaire ;
- bannière du projet.
Utilisation : python3 tools/readme_images.py
"""
import json
import math
import os
import re
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
ROOT = os.path.join(HERE, "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/modeminecraft")
OUT = os.path.join(ROOT, "docs/images")
LANG = json.load(open(os.path.join(ASSETS, "lang/fr_fr.json"), encoding="utf-8"))
JAVA = os.path.join(ROOT, "src/main/java/fr/modeminecraft")
os.makedirs(OUT, exist_ok=True)

BG1, BG2 = (22, 27, 34), (13, 17, 23)
ACCENT = (232, 200, 90)
TEXT = (230, 233, 238)
MUTED = (150, 158, 170)
FONT_DIR = "/usr/share/fonts/truetype/dejavu/"


def font(size, bold=False):
    for name in (("DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"),):
        try:
            return ImageFont.truetype(FONT_DIR + name, size)
        except OSError:
            pass
    return ImageFont.load_default()


def gradient(w, h, c1=BG1, c2=BG2):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))
        for x in range(w):
            px[x, y] = c
    return img


def name_of(item_id):
    n = LANG.get("item.modeminecraft." + item_id) or LANG.get("entity.modeminecraft." + item_id) or item_id.replace("_", " ").title()
    return n


def fit(draw, text, fnt, width, max_lines=2):
    if draw.textlength(text, font=fnt) <= width:
        return [text]
    words, lines, cur = text.split(), [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if draw.textlength(t, font=fnt) <= width:
            cur = t
        else:
            if cur:
                lines.append(cur)
            cur = w
    lines.append(cur)
    out = []
    for ln in lines[:max_lines]:
        while draw.textlength(ln, font=fnt) > width and len(ln) > 3:
            ln = ln[:-2]
        out.append(ln)
    return out


def titled(img, title, subtitle=None):
    """Ajoute un bandeau de titre en haut."""
    w = img.width
    head = 58 if subtitle else 46
    out = gradient(w, img.height + head)
    d = ImageDraw.Draw(out)
    d.rectangle([0, 0, 6, head - 8], fill=ACCENT)
    d.text((18, 6), title, font=font(24, True), fill=TEXT)
    if subtitle:
        d.text((18, 36), subtitle, font=font(13), fill=MUTED)
    out.paste(img, (0, head))
    return out


# =============================================================================== galeries d'objets
def icon(item_id, size):
    key = (item_id, size)
    if key in _ICON_CACHE:
        return _ICON_CACHE[key]
    im = _icon(item_id, size)
    _ICON_CACHE[key] = im
    return im


_ICON_CACHE = {}


def _icon(item_id, size):
    p3 = os.path.join(ASSETS, "models/item", item_id + ".json")
    if os.path.exists(p3) and not os.path.exists(os.path.join(ASSETS, f"textures/item/{item_id}.png")):
        r = model_icon(item_id, size)
        if r is not None:
            return r
    for sub in ("item", "block"):
        p = os.path.join(ASSETS, f"textures/{sub}/{item_id}.png")
        if os.path.exists(p):
            im = Image.open(p).convert("RGBA")
            if im.height > im.width:                              # animations (bandes verticales)
                im = im.crop((0, 0, im.width, im.width))
            if im.width > 64:
                im = im.resize((64, int(64 * im.height / im.width)), Image.NEAREST)
            k = max(1, size // max(im.width, im.height))
            return im.resize((im.width * k, im.height * k), Image.NEAREST)
    r = model_icon(item_id, size)
    return r


def gallery(ids, cols, tile=(112, 118), scale=64, labels=True):
    ids = [i for i in ids if icon(i, scale) is not None]
    rows = (len(ids) + cols - 1) // cols
    img = gradient(cols * tile[0] + 16, rows * tile[1] + 16)
    d = ImageDraw.Draw(img)
    fnt = font(10)
    for n, item in enumerate(ids):
        c, r = n % cols, n // cols
        x, y = 8 + c * tile[0], 8 + r * tile[1]
        d.rounded_rectangle([x + 3, y + 3, x + tile[0] - 4, y + tile[1] - 4], 8, fill=(30, 37, 46), outline=(48, 57, 69))
        ic = icon(item, scale)
        glow = Image.new("RGBA", ic.size, (0, 0, 0, 0))
        shadow = Image.new("RGBA", ic.size, (0, 0, 0, 110))
        shadow.putalpha(ic.split()[3].point(lambda a: 110 if a else 0))
        img.paste(shadow, (x + (tile[0] - ic.width) // 2 + 3, y + 10 + (scale - ic.height) // 2 + 3), shadow)
        img.paste(ic, (x + (tile[0] - ic.width) // 2, y + 10 + (scale - ic.height) // 2), ic)
        if labels:
            lines = fit(d, name_of(item), fnt, tile[0] - 14)
            for k, ln in enumerate(lines):
                d.text((x + tile[0] // 2 - d.textlength(ln, font=fnt) / 2, y + scale + 18 + k * 12), ln, font=fnt, fill=MUTED)
    return img



# =============================================================================== rendu de modèles JSON (éléments texturés)
import numpy as np


def _rot(rx, ry, rz):
    a, b, c = map(math.radians, (rx, ry, rz))
    Rx = np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])
    Ry = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    Rz = np.array([[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def _axis_rot(axis, angle):
    t = math.radians(angle)
    c, s = math.cos(t), math.sin(t)
    if axis == "x":
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == "y":
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


_TEX_CACHE = {}


def _texture(ref):
    ref = ref.split(":")[-1]
    if ref not in _TEX_CACHE:
        p = os.path.join(ASSETS, "textures", ref + ".png")
        _TEX_CACHE[ref] = Image.open(p).convert("RGBA") if os.path.exists(p) else None
    return _TEX_CACHE[ref]


FACE_DEF = {   # origine (en fonction de from/to), direction u, direction v, normale
    "north": (("x1", "y1", "z0"), (-1, 0, 0), (0, -1, 0), (0, 0, -1)),
    "south": (("x0", "y1", "z1"), (1, 0, 0), (0, -1, 0), (0, 0, 1)),
    "east": (("x1", "y1", "z1"), (0, 0, -1), (0, -1, 0), (1, 0, 0)),
    "west": (("x0", "y1", "z0"), (0, 0, 1), (0, -1, 0), (-1, 0, 0)),
    "up": (("x0", "y1", "z0"), (1, 0, 0), (0, 0, 1), (0, 1, 0)),
    "down": (("x0", "y0", "z1"), (1, 0, 0), (0, 0, -1), (0, -1, 0)),
}


def render_json_model(path, size=240, view=None, ss=3):
    """Rendu d'un modèle d'objet Minecraft (éléments à faces texturées) en projection orthographique."""
    m = json.load(open(path, encoding="utf-8"))
    els = m.get("elements")
    if not els:
        return None
    textures = m.get("textures", {})
    gui = (m.get("display", {}).get("gui", {}) or {})
    rot = view or gui.get("rotation", [25, -35, 0])
    if view is None and any("armor3d" in str(v) for v in textures.values()):
        rot = [-22, 150, 0]                                      # équipements : vus de dessus, de trois quarts avant
    R = _rot(*rot)
    faces = []
    for e in els:
        f, t = np.array(e["from"], float), np.array(e["to"], float)
        er = e.get("rotation")
        for fname, fdata in e.get("faces", {}).items():
            if fname not in FACE_DEF:
                continue
            (ox, oy, oz), ud, vd, nrm = FACE_DEF[fname]
            pick = {"x0": f[0], "x1": t[0], "y0": f[1], "y1": t[1], "z0": f[2], "z1": t[2]}
            origin = np.array([pick[ox], pick[oy], pick[oz]])
            wu = abs(np.dot(np.array(ud), t - f))
            wv = abs(np.dot(np.array(vd), t - f))
            Pu, Pv, P0 = origin + np.array(ud) * wu, origin + np.array(vd) * wv, origin
            n = np.array(nrm, float)
            if er:
                o = np.array(er["origin"], float)
                M = _axis_rot(er["axis"], er["angle"])
                P0, Pu, Pv = (M @ (P0 - o) + o), (M @ (Pu - o) + o), (M @ (Pv - o) + o)
                n = M @ n
            c = np.array([8, 8, 8], float)
            P0, Pu, Pv = R @ (P0 - c), R @ (Pu - c), R @ (Pv - c)
            n = R @ n
            if n[2] <= 0.001:
                continue
            uv = fdata.get("uv")
            tex = fdata.get("texture", "#p")
            ref = textures.get(tex[1:], None) if tex.startswith("#") else tex
            if ref and ref.startswith("#"):
                ref = textures.get(ref[1:])
            img = _texture(ref) if ref else None
            if img is None:
                continue
            tw, th = img.size
            if uv is None:
                uv = [0, 0, 16, 16]
            depth = (P0[2] + Pu[2] + Pv[2] + (Pu[2] + Pv[2] - P0[2])) / 4
            light = 0.62 + 0.38 * max(0.0, np.dot(n / (np.linalg.norm(n) + 1e-9), np.array([0.3, 0.8, 0.55])))
            faces.append((depth, P0, Pu, Pv, uv, img, tw, th, light))
    if not faces:
        return None
    faces.sort(key=lambda x: x[0])
    pts = []
    for (_d, P0, Pu, Pv, *_r) in faces:
        for P in (P0, Pu, Pv, Pu + Pv - P0):
            pts.append((P[0], -P[1]))
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    unit = size * ss * 0.9 / max(maxx - minx, maxy - miny, 1e-6)
    W = H = size * ss
    canvas = np.zeros((H, W, 4), np.uint8)
    cx, cy = (minx + maxx) / 2, (miny + maxy) / 2
    for (_d, P0, Pu, Pv, uv, img, tw, th, light) in faces:
        def S(P):
            return np.array([(P[0] - cx) * unit + W / 2, (-P[1] - cy) * unit + H / 2])
        s0, su, sv = S(P0), S(Pu) - S(P0), S(Pv) - S(P0)
        det = su[0] * sv[1] - su[1] * sv[0]
        if abs(det) < 1e-6:
            continue
        corners = [s0, s0 + su, s0 + sv, s0 + su + sv]
        x0, x1 = int(max(0, math.floor(min(c[0] for c in corners)))), int(min(W - 1, math.ceil(max(c[0] for c in corners))))
        y0, y1 = int(max(0, math.floor(min(c[1] for c in corners)))), int(min(H - 1, math.ceil(max(c[1] for c in corners))))
        if x1 < x0 or y1 < y0:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        dx, dy = gx - s0[0], gy - s0[1]
        a = (dx * sv[1] - dy * sv[0]) / det
        b = (su[0] * dy - su[1] * dx) / det
        inside = (a >= 0) & (a < 1) & (b >= 0) & (b < 1)
        u = uv[0] + a * (uv[2] - uv[0])
        v = uv[1] + b * (uv[3] - uv[1])
        tx = np.clip((u * tw / 16).astype(int), 0, tw - 1)
        ty = np.clip((v * th / 16).astype(int), 0, th - 1)
        arr = np.array(img)
        col = arr[ty, tx]
        ok = inside & (col[..., 3] > 20)
        region = canvas[y0:y1 + 1, x0:x1 + 1]
        rgb = (col[..., :3].astype(float) * light).clip(0, 255).astype(np.uint8)
        region[ok, :3] = rgb[ok]
        region[ok, 3] = 255
    out = Image.fromarray(canvas, "RGBA").resize((size, size), Image.LANCZOS)
    return out


def model_icon(item_id, size, view=None):
    for sub in ("models/item",):
        p = os.path.join(ASSETS, sub, item_id + ".json")
        if os.path.exists(p):
            return render_json_model(p, size, view)
    return None


def ids_of(pattern, text):
    seen, out = set(), []
    for m in re.finditer(pattern, text):
        i = m.group(1)
        if i not in seen:
            seen.add(i)
            out.append(i)
    return out


ITEMS_SRC = open(os.path.join(JAVA, "registry/ModItems.java"), encoding="utf-8").read()


def save(img, name, title=None, subtitle=None):
    if title:
        img = titled(img, title, subtitle)
    img.save(os.path.join(OUT, name), optimize=True)
    print("ok", name, img.size)


def galleries():
    guns = ids_of(r'\bguns?\(\s*"([a-z0-9_]+)"', ITEMS_SRC)
    save(gallery(guns, 10), "armes_a_feu.png", "Armes à feu", f"{len(guns)} armes : pistolets, fusils d'assaut, snipers, lance-roquettes, fusils à pompe")
    melee = ids_of(r'\bmelee\(\s*"([a-z0-9_]+)"', ITEMS_SRC) + ["rubis_sword", "rubis_dagger", "rubis_war_hammer", "fire_staff", "ender_blade", "dragonslayer"]
    save(gallery(melee, 9), "armes_blanches.png", "Armes blanches et magiques", "Combos de 3 coups, parades parfaites, coups dans le dos, capacités spéciales")
    att = ids_of(r'\battachment\(\s*"([a-z0-9_]+)"', ITEMS_SRC)
    save(gallery(att, 14, tile=(104, 112), scale=56), "accessoires.png", "Accessoires d'armes", f"{len(att)} accessoires : viseurs, silencieux, poignées, chargeurs, lasers, lampes")
    ammo = ids_of(r'\bammo\(\s*"([a-z0-9_]+)"', ITEMS_SRC) + ids_of(r'\bgrenade\(\s*"([a-z0-9_]+)"', ITEMS_SRC) + \
        ["c4", "claymore", "mortar_shell", "rpg_rocket", "stinger_missile", "javelin_missile", "torpedo", "grad_rocket", "cruise_missile", "aam_ir_missile"]
    save(gallery(ammo, 11), "munitions_explosifs.png", "Munitions, grenades et explosifs", "Calibres réalistes, grenades, charges, roquettes et missiles")
    gear = ids_of(r'\bhelmet\(\s*"([a-z0-9_]+)"', ITEMS_SRC) + ids_of(r'\bvest\(\s*"([a-z0-9_]+)"', ITEMS_SRC) + ids_of(r'\bgear\(\s*"([a-z0-9_]+)"', ITEMS_SRC)
    save(gallery(gear, 11), "equipements.png", "Casques, gilets, pantalons et rangers", "De l'Antiquité à aujourd'hui, en pièces 3D")
    outfits = ids_of(r'\boutfit\(\s*"([a-z0-9_]+)"', ITEMS_SRC)
    parts = [f"{o}_{p}" for o in outfits for p in ("helmet", "chestplate", "leggings", "boots")]
    save(gallery(parts, 8), "tenues.png", "Tenues d'infiltration et de faction", "Police, Chef du Désert, forces spéciales, soviétique, cartel, shérif")
    drones = ids_of(r'\bdrone\(\s*"([a-z0-9_]+)"', ITEMS_SRC) + ids_of(r'\bdroneModule\(\s*"([a-z0-9_]+)"', ITEMS_SRC) + ["drone_controller", "emp_jammer_rifle"]
    save(gallery(drones, 5, scale=96, tile=(150, 150)), "drones.png", "Drones et guerre électronique", "Micro-drone, FPV kamikaze, drone armé, brouilleur EMP")
    turrets = ids_of(r'\bturret\(\s*"([a-z0-9_]+)"', ITEMS_SRC)
    save(gallery(turrets, 7, scale=110, tile=(150, 160)), "tourelles.png", "Tourelles et mitrailleuses lourdes", "M2 Browning, DShK, MG42, Vickers, Maxim... à poser et à servir")
    misc = ["first_aid_kit", "bandage", "adrenaline_syringe", "military_ration", "red_access_card", "black_access_card", "mesa_train_ticket", "rubis", "uranium_canister",
            "nuclear_guidance_unit", "nuclear_neutron_reflector", "nuclear_implosion_lenses", "nuclear_krytron_trigger", "steel_plate", "titanium_plate", "aircraft_engine", "jet_engine",
            "avionics_suite", "radar_absorbent_panel", "vehicle_engine", "wheel_set", "track_set", "wrench", "jerrycan"]
    save(gallery(misc, 8), "objets.png", "Soins, cartes, matériaux et composants", "Soins, cartes d'accès, composants nucléaires, pièces de véhicules")


# =============================================================================== blocs
def blocks_gallery():
    names = ["rubis_ore", "deepslate_rubis_ore", "titanium_ore", "lead_ore", "uranium_ore", "armament_table_top", "assembly_table_top", "artillery_table_top",
             "barracks_locker_front", "traffic_light_side", "ac_unit_fan", "ambient_speaker_front", "cash_register_body", "weapon_rack_front"]
    avail = []
    for n in names:
        if os.path.exists(os.path.join(ASSETS, f"textures/block/{n}.png")):
            avail.append(n)
    allb = sorted(f[:-4] for f in os.listdir(os.path.join(ASSETS, "textures/block")) if f.endswith(".png"))
    pick = (avail + [b for b in allb if b not in avail])[:30]
    tiles = []
    for n in pick:
        im = Image.open(os.path.join(ASSETS, f"textures/block/{n}.png")).convert("RGBA")
        if im.height > im.width:
            im = im.crop((0, 0, im.width, im.width))
        tiles.append((n, im.resize((72, 72), Image.NEAREST)))
    cols = 10
    rows = (len(tiles) + cols - 1) // cols
    img = gradient(cols * 96 + 16, rows * 104 + 16)
    d = ImageDraw.Draw(img)
    for i, (n, im) in enumerate(tiles):
        x, y = 8 + (i % cols) * 96, 8 + (i // cols) * 104
        d.rounded_rectangle([x + 3, y + 3, x + 92, y + 100], 8, fill=(30, 37, 46), outline=(48, 57, 69))
        img.paste(im, (x + 12, y + 10), im)
        lab = n.replace("_", " ")[:16]
        d.text((x + 48 - d.textlength(lab, font=font(9)) / 2, y + 86), lab, font=font(9), fill=MUTED)
    save(img, "blocs.png", "Blocs et minerais", "Minerais (rubis, titane, plomb, uranium), établis, décors urbains")


# =============================================================================== skins des factions
def skin_front(path, scale=6):
    sk = Image.open(path).convert("RGBA")

    def part(x, y, w, h):
        return sk.crop((x, y, x + w, y + h))
    fig = Image.new("RGBA", (16, 32), (0, 0, 0, 0))

    def put(im, x, y):
        fig.alpha_composite(im, (x, y))
    put(part(8, 8, 8, 8), 4, 0)
    put(part(20, 20, 8, 12), 4, 8)
    put(part(44, 20, 4, 12), 0, 8)
    put(part(36, 52, 4, 12), 12, 8)
    put(part(4, 20, 4, 12), 4, 20)
    put(part(20, 52, 4, 12), 8, 20)
    for (sx, sy, sw, sh), (dx, dy) in (((40, 8, 8, 8), (4, 0)), ((20, 36, 8, 12), (4, 8)), ((44, 36, 4, 12), (0, 8)), ((52, 52, 4, 12), (12, 8)),
                                         ((4, 36, 4, 12), (4, 20)), ((4, 52, 4, 12), (8, 20))):
        put(part(sx, sy, sw, sh), dx, dy)
    return fig.resize((16 * scale, 32 * scale), Image.NEAREST)


def factions():
    d = os.path.join(ASSETS, "textures/entity/bot")
    order = ["police", "swat", "militia", "rebel", "garrison", "soviet", "arctic", "mine_security", "cartel", "gang", "sheriff", "refinery",
             "civilian", "trader", "falcon", "offshore", "railway"]
    names = {"police": "Police", "swat": "SWAT", "militia": "Milice du désert", "rebel": "Rebelles", "garrison": "Armée américaine", "soviet": "Armée soviétique",
             "arctic": "Forces arctiques", "mine_security": "Sécurité de la mine", "cartel": "Cartel", "gang": "Gangs", "sheriff": "Shérifs", "refinery": "Raffinerie",
             "civilian": "Civils", "trader": "Marchands", "falcon": "Faucons Blancs", "offshore": "Mercenaires Offshore", "railway": "Personnel ferroviaire"}
    cols = 6
    cell_w, cell_h = 232, 196
    rows = (len(order) + cols - 1) // cols
    img = gradient(cols * cell_w + 16, rows * cell_h + 16)
    dr = ImageDraw.Draw(img)
    for n, f in enumerate(order):
        x, y = 8 + (n % cols) * cell_w, 8 + (n // cols) * cell_h
        dr.rounded_rectangle([x + 3, y + 3, x + cell_w - 4, y + cell_h - 4], 10, fill=(30, 37, 46), outline=(48, 57, 69))
        files = sorted(p for p in os.listdir(d) if re.fullmatch(rf"{f}_\d+\.png", p))[:3]
        for k, fn in enumerate(files):
            fig = skin_front(os.path.join(d, fn), 4)
            img.paste(fig, (x + 16 + k * 68, y + 18), fig)
        lab = names[f]
        dr.text((x + cell_w / 2 - dr.textlength(lab, font=font(13, True)) / 2, y + cell_h - 30), lab, font=font(13, True), fill=TEXT)
    save(img, "factions.png", "Factions et bots", "Skins sur mesure de chaque faction (trois exemplaires par faction)")
    # chefs de faction
    bosses = sorted(p for p in os.listdir(d) if p.startswith("boss_"))
    img = gradient(len(bosses) * 150 + 16, 250)
    dr = ImageDraw.Draw(img)
    for k, fn in enumerate(bosses):
        fig = skin_front(os.path.join(d, fn), 5)
        img.paste(fig, (16 + k * 150 + 18, 16), fig)
        lab = fn[5:-4].replace("_", " ").title()
        dr.text((16 + k * 150 + 70 - dr.textlength(lab, font=font(12, True)) / 2, 192), lab, font=font(12, True), fill=TEXT)
    save(img, "chefs.png", "Chefs de faction", "Un chef unique par faction, avec sa chambre forte et sa tenue")


# =============================================================================== rendus isométriques des modèles voxel
def render_models():
    import generate_military_vehicles as g
    models = {}
    for fn in g.VEHICLES:
        v = fn()
        models[v.name] = v
    return g, models


def color_of(g, key):
    c = g.COLORS.get(key, "#808080")
    if isinstance(c, list):
        c = c[0]
    c = c.lstrip("#")
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def iso_render(g, v, size=(520, 300), scale_hint=None, yaw=0, dx=0.0, supersample=3, parts_skip=("bay_open", "doors_f"), offset=(0, 0, 0), canvas=None):
    """Rendu isométrique d'un modèle voxel (pavés alignés sur les axes, ordre du peintre)."""
    k = 16 / v.S
    boxes = []
    for part, els in v.parts.items():
        if part in parts_skip:
            continue
        for e in els:
            f, t = e["from"], e["to"]
            x0, x1 = (f[0] - v.ox) / k, (t[0] - v.ox) / k
            y0, y1 = f[1] / k, t[1] / k
            z0, z1 = (f[2] - 8) / k, (t[2] - 8) / k
            boxes.append((x0 + offset[0], x1 + offset[0], y0 + offset[1], y1 + offset[1], z0 + offset[2], z1 + offset[2], e["__key"]))
    for (x, y, z, r) in v.wheels:
        boxes.append((x - r, x + r, y - r, y + r, z - v.wheel_w / 2, z + v.wheel_w / 2, "tire"))
    for (x, y, z) in v.props:
        boxes.append((x - 0.1, x + 0.1, y - 1.0, y + 1.0, z - 0.12, z + 0.12, "rotor"))
    return boxes


def project(x, y, z, a):
    # vue isométrique : x vers l'avant (en bas à droite), z vers la droite du véhicule (en bas à gauche)
    return ((x - z) * math.cos(a), -y + (x + z) * math.sin(a))


def draw_boxes(g, boxes, canvas, origin, unit, a=math.radians(30)):
    d = ImageDraw.Draw(canvas)
    boxes = sorted(boxes, key=lambda b: ((b[0] + b[1]) / 2 + (b[4] + b[5]) / 2 + (b[2] + b[3]) / 2 * 0.9, b[0]))
    for (x0, x1, y0, y1, z0, z1, key) in boxes:
        base = color_of(g, key)
        if key == "glass":
            base = (110, 150, 190)

        def P(x, y, z):
            sx, sy = project(x, y, z, a)
            return (origin[0] + sx * unit, origin[1] + sy * unit)
        top = [P(x0, y1, z0), P(x1, y1, z0), P(x1, y1, z1), P(x0, y1, z1)]
        front = [P(x1, y0, z0), P(x1, y0, z1), P(x1, y1, z1), P(x1, y1, z0)]
        right = [P(x0, y0, z1), P(x1, y0, z1), P(x1, y1, z1), P(x0, y1, z1)]
        d.polygon(right, fill=shade(base, 0.72))
        d.polygon(front, fill=shade(base, 0.88))
        d.polygon(top, fill=shade(base, 1.12))


def vehicle_image(g, v, w=480, h=300, extra_boxes=None, margin=0.88):
    ss = 3
    boxes = iso_render(g, v)
    if extra_boxes:
        boxes += extra_boxes
    a = math.radians(30)
    pts = []
    for (x0, x1, y0, y1, z0, z1, _k) in boxes:
        for x in (x0, x1):
            for y in (y0, y1):
                for z in (z0, z1):
                    pts.append(project(x, y, z, a))
    minx, maxx = min(p[0] for p in pts), max(p[0] for p in pts)
    miny, maxy = min(p[1] for p in pts), max(p[1] for p in pts)
    unit = min(w * ss * margin / max(0.01, maxx - minx), h * ss * margin / max(0.01, maxy - miny))
    canvas = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    origin = (w * ss / 2 - (minx + maxx) / 2 * unit, h * ss / 2 - (miny + maxy) / 2 * unit)
    # ombre au sol
    sh = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ds = ImageDraw.Draw(sh)
    cx, cy = origin[0] + (minx + maxx) / 2 * unit, origin[1] + maxy * unit
    ds.ellipse([cx - (maxx - minx) * unit * 0.42, cy - 18 * ss, cx + (maxx - minx) * unit * 0.42, cy + 10 * ss], fill=(0, 0, 0, 90))
    sh = sh.filter(ImageFilter.GaussianBlur(8 * ss))
    canvas.alpha_composite(sh)
    draw_boxes(g, boxes, canvas, origin, unit)
    return canvas.resize((w, h), Image.LANCZOS)


def sheet(g, models, ids, cols, cell=(360, 250), title="", subtitle="", out="x.png"):
    ids = [i for i in ids if i in models]
    rows = (len(ids) + cols - 1) // cols
    img = gradient(cols * cell[0] + 16, rows * cell[1] + 16)
    dr = ImageDraw.Draw(img)
    for n, vid in enumerate(ids):
        x, y = 8 + (n % cols) * cell[0], 8 + (n // cols) * cell[1]
        dr.rounded_rectangle([x + 3, y + 3, x + cell[0] - 4, y + cell[1] - 4], 10, fill=(27, 33, 42), outline=(48, 57, 69))
        im = vehicle_image(g, models[vid], cell[0] - 30, cell[1] - 62)
        img.paste(im, (x + 15, y + 12), im)
        lab = name_of(vid)
        fn = font(12, True)
        lines = fit(dr, lab, fn, cell[0] - 24)
        for k, ln in enumerate(lines):
            dr.text((x + cell[0] / 2 - dr.textlength(ln, font=fn) / 2, y + cell[1] - 46 + k * 15), ln, font=fn, fill=TEXT)
    save(img, out, title, subtitle)


def vehicles():
    g, models = render_models()
    mil = re.findall(r'^\s{4}([A-Z_0-9]+)\("([a-z0-9_]+)", Side\.(\w+), Chassis\.(\w+)', open(os.path.join(JAVA, "entity/military/MilType.java"), encoding="utf-8").read(), re.M)
    by = {}
    for _n, vid, side, chassis in mil:
        by.setdefault(chassis, []).append(vid)
    ground = [v for c in ("WHEELS", "TRACKS") for v in by.get(c, [])]
    sheet(g, models, ground, 4, title="Véhicules terrestres militaires", subtitle="Chars, blindés, camions, lance-missiles et complexes sol-air soviétiques et américains", out="vehicules_terrestres.png")
    sheet(g, models, by.get("HELI", []), 3, title="Hélicoptères", subtitle="Mi-24 Hind, UH-60 Black Hawk, AH-64E Apache", out="helicopteres.png")
    planes = [v for v in by.get("PLANE", []) if v != "light_civil_plane"]
    sheet(g, models, planes, 4, cell=(340, 230), title="Aviation militaire", subtitle=f"{len(planes)} avions : chasseurs, bombardiers, avions d'attaque et de transport", out="avions.png")
    sheet(g, models, by.get("SHIP", []) + by.get("SUB", []), 3, cell=(400, 240), title="Marine et sous-marins", subtitle="Croiseurs, destroyers, sous-marins nucléaires et vedettes des Mercenaires Offshore", out="navires.png")
    return g, models


def hero_images(g, models):
    # avion léger civil
    save(vehicle_image(g, models["light_civil_plane"], 900, 520, margin=0.9), "avion_leger.png", "Avion léger biplace civil", "8 blocs d'envergure, hélice bipale, train fixe, aucun armement")
    # train : locomotive et deux wagons alignés
    loco, wag = models["mesa_locomotive"], models["mesa_wagon"]
    boxes = iso_render(g, loco, offset=(0, 0, 0))
    for i in range(2):
        boxes += iso_render(g, wag, offset=(-10.6 - i * 11.4, 0, 0))
    img = vehicle_image(g, loco, 1500, 520, extra_boxes=boxes[len(iso_render(g, loco)):], margin=0.94) if False else None
    ss = 3
    a = math.radians(30)
    allboxes = boxes
    pts = [project(x, y, z, a) for (x0, x1, y0, y1, z0, z1, _k) in allboxes for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
    minx, maxx = min(p[0] for p in pts), max(p[0] for p in pts)
    miny, maxy = min(p[1] for p in pts), max(p[1] for p in pts)
    w, h = 1500, 480
    unit = min(w * ss * 0.94 / (maxx - minx), h * ss * 0.9 / (maxy - miny))
    canvas = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    origin = (w * ss / 2 - (minx + maxx) / 2 * unit, h * ss / 2 - (miny + maxy) / 2 * unit)
    # voie ferrée
    d = ImageDraw.Draw(canvas)
    for x in range(-40, 12):
        for zz in (-0.75, 0.75):
            p0 = project(x, 0, zz, a)
            p1 = project(x + 1.0, 0, zz, a)
            d.line([(origin[0] + p0[0] * unit, origin[1] + p0[1] * unit), (origin[0] + p1[0] * unit, origin[1] + p1[1] * unit)], fill=(110, 112, 118), width=6 * ss // 2)
        if x % 2 == 0:
            p0, p1 = project(x + 0.3, 0, -1.1, a), project(x + 0.3, 0, 1.1, a)
            d.line([(origin[0] + p0[0] * unit, origin[1] + p0[1] * unit), (origin[0] + p1[0] * unit, origin[1] + p1[1] * unit)], fill=(82, 58, 38), width=5 * ss)
    draw_boxes(g, allboxes, canvas, origin, unit)
    img = canvas.resize((w, h), Image.LANCZOS)
    bg = gradient(w, h, (60, 40, 30), (18, 14, 14))
    bg.paste(img, (0, 0), img)
    save(bg, "train_mesa.png", "Train des villes du Mesa", "Locomotive à vapeur et wagons de voyageurs (rendu des modèles du mod)")
    return bg


# =============================================================================== plans de bases et réseau ferroviaire
def base_plans():
    src = open(os.path.join(JAVA, "scenario/ScenarioSites.java"), encoding="utf-8").read()
    sites = re.findall(r'^\s+([A-Z_]+)\((-?\d+), (-?\d+), (-?\d+), (-?\d+), (\d+), (\d+),', src, re.M)
    palette = {"DORMITORY": (84, 130, 180), "RECRUITMENT": (230, 190, 70), "ARMORY": (170, 90, 80), "CANTEEN": (110, 160, 100), "INFIRMARY": (200, 110, 150),
               "CORPORAL_BARRACKS": (90, 140, 170), "NCO_QUARTERS": (100, 120, 190), "OFFICER_PAVILION": (130, 110, 200), "OFFICERS_CLUB": (150, 120, 170),
               "WAR_ROOM": (200, 70, 70), "COMMAND_RESIDENCE": (180, 140, 90), "GENERAL_BUNKER": (120, 120, 130), "OFFICERS_PARKING": (110, 110, 100),
               "SHOOTING_RANGE": (150, 150, 80), "PRISON": (90, 90, 100)}
    for side, ix, title, sub in ((0, 2, "Plan de la base américaine", "Bâtiments du scénario (parcelles de 16 × 16 blocs)"),
                                 (1, 4, "Plan de la base soviétique", "Mêmes bâtiments, disposés dans la cité soviétique")):
        cells = []
        for name, ux, uz, sx, sz, w, h in sites:
            x, z = (int(ux), int(uz)) if side == 0 else (int(sx), int(sz))
            cells.append((name, x, z, int(w), int(h)))
        minx = min(c[1] for c in cells) - 1
        maxx = max(c[1] + c[3] for c in cells) + 1
        minz = min(c[2] for c in cells) - 1
        maxz = max(c[2] + c[4] for c in cells) + 1
        u = 86
        img = gradient((maxx - minx) * u + 40, (maxz - minz) * u + 40)
        d = ImageDraw.Draw(img)
        for gx in range(minx, maxx + 1):
            d.line([(20 + (gx - minx) * u, 20), (20 + (gx - minx) * u, 20 + (maxz - minz) * u)], fill=(34, 41, 51))
        for gz in range(minz, maxz + 1):
            d.line([(20, 20 + (gz - minz) * u), (20 + (maxx - minx) * u, 20 + (gz - minz) * u)], fill=(34, 41, 51))
        for name, x, z, w, h in cells:
            x0, y0 = 20 + (x - minx) * u + 3, 20 + (z - minz) * u + 3
            x1, y1 = x0 + w * u - 6, y0 + h * u - 6
            c = palette.get(name, (120, 120, 120))
            d.rounded_rectangle([x0, y0, x1, y1], 8, fill=shade(c, 0.55), outline=c, width=2)
            lab = LANG.get("scenario.modeminecraft.site." + name.lower(), name.replace("_", " ").title())
            fn = font(11, True)
            for k, ln in enumerate(fit(d, lab, fn, w * u - 14, 3)):
                d.text(((x0 + x1) / 2 - d.textlength(ln, font=fn) / 2, (y0 + y1) / 2 - 8 + k * 13), ln, font=fn, fill=TEXT)
        save(img, f"plan_base_{'us' if side == 0 else 'urss'}.png", title, sub)


def rail_diagram():
    w, h = 1300, 420
    img = gradient(w, h, (60, 40, 30), (24, 18, 16))
    d = ImageDraw.Draw(img)
    cities = [(150, 250, "Ville frontière A"), (520, 130, "Ville frontière B"), (900, 280, "Ville frontière C"), (1170, 150, "Ville frontière D")]
    for (x0, y0, _), (x1, y1, _) in zip(cities, cities[1:]):
        for off in (-3, 3):
            d.line([(x0, y0 + off), ((x0 + x1) / 2, (y0 + y1) / 2 + off + (-30 if y1 < y0 else 30)), (x1, y1 + off)], fill=(150, 150, 156), width=2)
        for t in range(0, 100, 5):
            tt = t / 100
            px = x0 + (x1 - x0) * tt
            py = y0 + (y1 - y0) * tt + math.sin(tt * math.pi) * (-30 if y1 < y0 else 30)
            d.line([(px - 2, py - 7), (px + 2, py + 7)], fill=(110, 80, 52), width=2)
    for x, y, lab in cities:
        d.rounded_rectangle([x - 70, y - 30, x + 70, y + 30], 8, fill=(86, 50, 36), outline=ACCENT, width=2)
        d.text((x - d.textlength(lab, font=font(13, True)) / 2, y - 18), lab, font=font(13, True), fill=TEXT)
        d.text((x - d.textlength("gare · quai · guichet", font=font(11)) / 2, y + 2), "gare · quai · guichet", font=font(11), fill=(230, 200, 150))
    d.text((24, 18), "Le train de ville en ville", font=font(24, True), fill=TEXT)
    d.text((24, 52), "arrêt de 25 s en gare, portes ouvertes, sifflet au départ · contrôle des billets à bord · expulsion des fraudeurs au quai", font=font(13), fill=MUTED)
    save(img, "reseau_ferroviaire.png")


# =============================================================================== bannière
def banner(g, models):
    w, h = 1600, 460
    img = gradient(w, h, (28, 36, 46), (10, 13, 18))
    d = ImageDraw.Draw(img)
    for i in range(0, w, 40):
        d.line([(i, 0), (i - 160, h)], fill=(20, 26, 34))
    # icônes d'armes en fond
    guns = ids_of(r'\bguns?\(\s*"([a-z0-9_]+)"', ITEMS_SRC)
    x = 20
    for gid in guns[:26]:
        ic = icon(gid, 96)
        if ic is None:
            continue
        a = ic.copy()
        a.putalpha(a.split()[3].point(lambda v: v * 38 // 100))
        img.paste(a, (x, h - 130), a)
        x += 62
    # véhicules
    for vid, px, py, ww, hh in (("f22_raptor", 880, 30, 380, 240), ("mesa_locomotive", 1180, 190, 380, 230), ("light_civil_plane", 640, 60, 260, 160),
                                ("m1a2_abrams", 940, 250, 330, 200)):
        if vid in models:
            im = vehicle_image(g, models[vid], ww, hh)
            img.paste(im, (px, py), im)
    d.text((60, 90), "MODE", font=font(92, True), fill=ACCENT)
    d.text((60, 190), "MINECRAFT", font=font(92, True), fill=TEXT)
    d.text((64, 306), "Mod NeoForge · Minecraft 1.21.1", font=font(22), fill=MUTED)
    d.text((64, 340), "Armes, véhicules, aviation, marine, villes vivantes, factions et scénario", font=font(17), fill=(120, 128, 140))
    d.rectangle([0, h - 6, w, h], fill=ACCENT)
    save(img, "banniere.png")


if __name__ == "__main__":
    galleries()
    blocks_gallery()
    factions()
    g, models = vehicles()
    hero_images(g, models)
    base_plans()
    rail_diagram()
    banner(g, models)

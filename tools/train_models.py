"""
Train des villes du Mesa (cahier des charges « Réseau ferroviaire Mesa ») : locomotive à vapeur du Far West industriel
(chaudière, cheminée, cabine vitrée, phare, chasse-pierres) et wagon de voyageurs (parois à fenêtres, banquettes, couloir
central, portes coulissantes dans la pièce « doors »), en voxels. Repère : avant +X, haut +Y, droite +Z, rails en y = 0.
"""
import math

TRAIN_COLORS = {
    "loco_green": ["#1f4d3a", "#1b4735", "#245640"], "loco_black": ["#1a1a1c", "#202022", "#161618"], "brass": "#b8923a",
    "loco_red": "#8a1f1a", "wagon_red": ["#8a3a24", "#823622", "#924028"], "wagon_cream": ["#d9cdaa", "#d2c6a2", "#e0d4b2"],
    "roof_gray": ["#6a6e72", "#64686c", "#707478"], "plank": ["#8a6a42", "#826240", "#92724a"], "bench": ["#5a3a24", "#52341f", "#62402a"],
    "door_wood": ["#6a3a20", "#62351d", "#724024"], "rail_steel": "#3a3c40", "lamp_yellow": "#ffe9a0",
}


def make(V0):
    trains = []
    SCALE = {}

    def V(name, sc):
        return V0(name, max(sc, SCALE.get(name, 0)))

    def reg(fn):
        def build():
            v = fn()
            need = max(v.xmax - v.xmin, 2 * v.zmax) / 2.9
            if v.S < need:
                SCALE[v.name] = math.ceil(need * 2) / 2
                v = fn()
            return v
        trains.append(build)
        return fn

    def wheel(v, x, z, r):
        v.cyl("hull", "z", (x, r), r, z - 0.1, z + 0.1, "loco_black")
        v.cyl("hull", "z", (x, r), r * 0.45, z - 0.13, z + 0.13, "brass")

    @reg
    def loco():
        v = V("mesa_locomotive", 3.0)
        # châssis et chasse-pierres
        v.box("hull", -4.2, 4.2, 0.55, 1.0, -1.25, 1.25, "loco_black")
        for i in range(4):
            v.box("hull", 4.2 + i * 0.12, 4.32 + i * 0.12, 0.45 + i * 0.1, 1.0, -0.2 - (3 - i) * 0.3, 0.2 + (3 - i) * 0.3, "loco_red")
        # chaudière, boîte à fumée, cheminée, dôme
        v.cyl("hull", "x", (2.15, 0.0), 1.0, -0.6, 3.9, "loco_green")
        v.cyl("hull", "x", (2.15, 0.0), 1.02, 3.9, 4.2, "loco_black")
        v.cyl("hull", "y", (3.2, 0.0), 0.28, 3.0, 3.8, "loco_black")
        v.cyl("hull", "y", (3.2, 0.0), 0.42, 3.8, 4.15, "loco_black")
        v.cyl("hull", "y", (1.3, 0.0), 0.36, 3.05, 3.5, "brass")
        v.cyl("hull", "y", (0.2, 0.0), 0.28, 3.05, 3.4, "brass")
        for x in (0.2, 1.4, 2.6):                                                    # cerclages en laiton
            v.cyl("hull", "x", (2.15, 0.0), 1.04, x, x + 0.1, "brass")
        # phare
        v.box("hull", 3.9, 4.35, 2.75, 3.15, -0.25, 0.25, "loco_black")
        v.box("hull", 4.35, 4.42, 2.8, 3.1, -0.17, 0.17, "lamp_yellow")
        # cabine vitrée à l'arrière
        v.box("hull", -4.2, -0.6, 1.0, 1.15, -1.4, 1.4, "plank")
        for s in (-1, 1):
            v.box("hull", -4.2, -0.6, 1.15, 2.2, s * 1.35, s * 1.45, "loco_green")
            v.box("hull", -4.2, -0.6, 3.5, 4.0, s * 1.35, s * 1.45, "loco_green")
            v.box("hull", -3.2, -1.6, 2.2, 3.5, s * 1.38, s * 1.43, "glass")
            v.box("hull", -4.2, -3.2, 2.2, 3.5, s * 1.35, s * 1.45, "loco_green")
            v.box("hull", -1.6, -0.6, 2.2, 3.5, s * 1.35, s * 1.45, "loco_green")
        v.box("hull", -4.25, -4.15, 1.15, 4.0, -1.4, 1.4, "loco_green")                # paroi arrière
        v.box("hull", -0.65, -0.55, 3.0, 4.0, -1.4, 1.4, "loco_green")                 # face avant de la cabine
        v.box("hull", -0.65, -0.55, 2.2, 3.0, -1.0, -0.2, "glass")
        v.box("hull", -0.65, -0.55, 2.2, 3.0, 0.2, 1.0, "glass")
        v.box("hull", -4.5, -0.3, 4.0, 4.2, -1.6, 1.6, "loco_black")                   # toit débordant
        v.box("hull", -4.3, -2.0, 1.15, 1.45, -0.1, 0.1, "brass")                      # chaudière : manettes
        v.box("hull", -2.9, -2.3, 1.15, 1.6, -1.2, -0.7, "bench")                      # siège du mécanicien
        # roues motrices, bielles, roues porteuses
        for z in (-1.3, 1.3):
            for x in (-1.2, 0.3, 1.8):
                wheel(v, x, z, 0.62)
            wheel(v, 3.4, z, 0.36)
            v.box("hull", -1.2, 1.8, 0.55, 0.7, z - 0.04, z + 0.04, "brass")
        v.seat(-2.6, 1.05, -0.9)
        return v

    @reg
    def wagon():
        v = V("mesa_wagon", 3.5)
        L2 = 5.0
        # châssis, plancher, bogies
        v.box("hull", -L2, L2, 0.5, 0.85, -1.3, 1.3, "loco_black")
        v.box("hull", -L2, L2, 0.85, 0.95, -1.45, 1.45, "plank")
        for bx in (-3.3, 3.3):
            for ax in (-0.55, 0.55):
                for z in (-1.2, 1.2):
                    wheel(v, bx + ax, z, 0.38)
            v.box("hull", bx - 0.9, bx + 0.9, 0.4, 0.55, -1.1, 1.1, "loco_black")
        # parois latérales : allège, fenêtres, montants, linteau ; ouverture de porte au milieu
        for s in (-1, 1):
            z0, z1 = (1.4, 1.5) if s > 0 else (-1.5, -1.4)
            v.box("hull", -L2, L2, 0.95, 1.75, z0, z1, "wagon_red")
            v.box("hull", -L2, L2, 2.7, 3.1, z0, z1, "wagon_cream")
            for i in range(-2, 3):
                cx = i * 2.0
                if abs(cx) < 1.2:
                    continue
                v.box("hull", cx - 0.75, cx + 0.75, 1.75, 2.7, z0 + 0.04, z1 - 0.04, "glass")
                v.box("hull", cx - 1.0, cx - 0.75, 1.75, 2.7, z0, z1, "wagon_red")
                v.box("hull", cx + 0.75, cx + 1.0, 1.75, 2.7, z0, z1, "wagon_red")
            v.box("hull", -1.2, -1.0, 1.75, 2.7, z0, z1, "wagon_red")
            v.box("hull", 1.0, 1.2, 1.75, 2.7, z0, z1, "wagon_red")
            v.box("hull", -1.0, 1.0, 2.5, 2.7, z0, z1, "wagon_red")
            # portes coulissantes (pièce « doors »), fermées : deux battants
            v.box("doors", -1.0, 0.0, 0.95, 2.5, z0 - 0.02 * s, z1 + 0.02 * s, "door_wood")
            v.box("doors_f", 0.0, 1.0, 0.95, 2.5, z0 - 0.02 * s, z1 + 0.02 * s, "door_wood")
        # parois d'extrémité avec porte de communication
        for x0, x1 in ((-L2, -L2 + 0.1), (L2 - 0.1, L2)):
            v.box("hull", x0, x1, 0.95, 3.1, -1.5, -0.5, "wagon_red")
            v.box("hull", x0, x1, 0.95, 3.1, 0.5, 1.5, "wagon_red")
            v.box("hull", x0, x1, 2.4, 3.1, -0.5, 0.5, "wagon_red")
        # toit en dos d'âne, avec lanterneau
        v.box("hull", -L2 - 0.2, L2 + 0.2, 3.1, 3.25, -1.6, 1.6, "roof_gray")
        v.box("hull", -L2, L2, 3.25, 3.4, -1.2, 1.2, "roof_gray")
        v.box("hull", -L2 + 0.5, L2 - 0.5, 3.4, 3.55, -0.7, 0.7, "roof_gray")
        # intérieur : couloir central, banquettes de part et d'autre, lanternes
        for s in (-1, 1):
            for cx in (-3.9, -2.5, -1.7, 1.7, 2.5, 3.9):
                v.box("hull", cx - 0.5, cx + 0.5, 0.95, 1.3, s * 0.55 if s > 0 else -1.25, 1.25 if s > 0 else -0.55, "bench")
                v.box("hull", cx - 0.5, cx + 0.5, 1.3, 1.9, 1.15 * s - 0.06, 1.15 * s + 0.06, "bench")
        for cx in (-3.0, 0.0, 3.0):
            v.box("hull", cx - 0.1, cx + 0.1, 2.95, 3.1, -0.1, 0.1, "lamp_yellow")
        # sièges : deux rangées de 6, de part et d'autre du couloir
        for cx in (-3.9, -2.5, -1.7, 1.7, 2.5, 3.9):
            v.seat(cx, 1.05, -0.9)
            v.seat(cx, 1.05, 0.9)
        return v

    return trains

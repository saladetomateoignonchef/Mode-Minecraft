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
    # TGV
    "tgv_white": ["#f0f2f4", "#e8eaee", "#f4f6f8"], "tgv_gray": ["#8a9098", "#848a92", "#90969e"], "tgv_blue": "#1c3f8a", "tgv_red": "#b02020",
    "tgv_dark": ["#2a2e34", "#262a30", "#2e3238"],
    # wagons de marchandises
    "tank_gray": ["#4a4e52", "#44484c", "#505458"], "tank_rust": ["#6b3a22", "#64361f", "#74402a"], "hazard": "#e8b81e",
    "cont_blue": ["#1f4f8a", "#1b4880", "#245794"], "cont_red": ["#9a2a1f", "#92271c", "#a22f24"], "cont_green": ["#2f6a3a", "#2a6235", "#357442"],
    "cont_rib": "#14161a", "armor_olive": ["#4b5a2a", "#455326", "#52622f"], "armor_dark": ["#2d3519", "#293115", "#323b1d"],
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


    def bogies(v, L2):
        """Châssis, plancher et deux bogies (comme le wagon de voyageurs)."""
        v.box("hull", -L2, L2, 0.5, 0.85, -1.3, 1.3, "loco_black")
        v.box("hull", -L2, L2, 0.85, 0.95, -1.45, 1.45, "plank")
        for bx in (-3.3, 3.3):
            for ax in (-0.55, 0.55):
                for z in (-1.2, 1.2):
                    wheel(v, bx + ax, z, 0.38)
            v.box("hull", bx - 0.9, bx + 0.9, 0.4, 0.55, -1.1, 1.1, "loco_black")

    @reg
    def tank():
        """Wagon-citerne : cuve cylindrique cerclée, dôme, échelles et passerelle."""
        v = V("mesa_tank", 3.5)
        L2 = 5.0
        bogies(v, L2)
        v.cyl("hull", "x", (2.05, 0.0), 1.1, -4.7, 4.7, "tank_gray")
        for x in (-3.6, -1.2, 1.2, 3.6):                                              # cerclages
            v.cyl("hull", "x", (2.05, 0.0), 1.14, x - 0.1, x + 0.1, "loco_black")
        v.cyl("hull", "y", (0.0, 0.0), 0.38, 3.0, 3.35, "tank_rust")                  # dôme et trou d'homme
        v.cyl("hull", "y", (0.0, 0.0), 0.22, 3.35, 3.5, "loco_black")
        v.box("hull", -4.5, 4.5, 3.14, 3.2, -0.18, 0.18, "plank")                      # passerelle
        for s in (-1, 1):                                                              # échelles d'extrémité
            for z in (0.0,):
                v.box("hull", s * 4.85, s * 4.95, 1.0, 3.0, z - 0.45, z - 0.38, "loco_black")
                v.box("hull", s * 4.85, s * 4.95, 1.0, 3.0, z + 0.38, z + 0.45, "loco_black")
                for h in (1.3, 1.8, 2.3, 2.8):
                    v.box("hull", s * 4.85, s * 4.95, h, h + 0.06, z - 0.45, z + 0.45, "loco_black")
        v.box("hull", -3.0, 3.0, 2.0, 2.5, 1.12, 1.16, "hazard")                       # losange / bandeau de danger
        return v

    @reg
    def container():
        """Wagon plat portant deux conteneurs de 20 pieds (nervures, portes, coins de renfort)."""
        v = V("mesa_container", 3.5)
        L2 = 5.0
        bogies(v, L2)
        for k, (x0, x1, col) in enumerate(((-4.85, -0.1, "cont_blue"), (0.1, 4.85, "cont_red"))):
            v.box("hull", x0, x1, 1.0, 3.55, -1.25, 1.25, col)
            for s in (-1, 1):                                                          # nervures verticales sur les flancs
                z0, z1 = (1.25, 1.3) if s > 0 else (-1.3, -1.25)
                x = x0 + 0.35
                while x < x1 - 0.2:
                    v.box("hull", x, x + 0.12, 1.1, 3.45, z0, z1, "cont_rib")
                    x += 0.55
            for xc in (x0, x1 - 0.12):                                                 # coins de renfort
                v.box("hull", xc, xc + 0.12, 1.0, 3.55, -1.3, 1.3, "cont_rib")
        v.box("hull", -4.85, 4.85, 3.5, 3.62, -1.3, 1.3, "cont_rib")                   # toits
        return v

    @reg
    def armored():
        """Wagon blindé de queue : casemate inclinée, créneaux, tourelle et canon, drapeau."""
        v = V("mesa_armored", 3.5)
        L2 = 5.0
        bogies(v, L2)
        v.box("hull", -4.9, 4.9, 0.95, 2.7, -1.35, 1.35, "armor_olive")                # caisse blindée
        v.box("hull", -4.9, 4.9, 2.7, 3.1, -1.1, 1.1, "armor_olive")                   # pans inclinés (en deux marches)
        v.box("hull", -4.9, 4.9, 3.1, 3.3, -0.8, 0.8, "armor_dark")
        for x in (-3.8, -1.9, 0.0, 1.9, 3.8):                                          # meurtrières
            for s in (-1, 1):
                z0, z1 = (1.35, 1.4) if s > 0 else (-1.4, -1.35)
                v.box("hull", x - 0.4, x + 0.4, 1.9, 2.15, z0, z1, "armor_dark")
        for s in (-1, 1):                                                              # plaques de blindage rivetées
            z0, z1 = (1.35, 1.42) if s > 0 else (-1.42, -1.35)
            for x in (-4.4, -2.7, -0.9, 0.9, 2.7, 4.4):
                v.box("hull", x - 0.12, x + 0.12, 1.1, 1.3, z0, z1, "armor_dark")
        v.cyl("hull", "y", (0.0, 0.0), 0.95, 3.3, 3.75, "armor_olive")                  # tourelle
        v.cyl("hull", "y", (0.0, 0.0), 1.0, 3.75, 3.85, "armor_dark")
        v.box("hull", 0.6, 3.2, 3.45, 3.62, -0.09, 0.09, "armor_dark")                  # canon
        v.box("hull", 3.2, 3.4, 3.42, 3.65, -0.12, 0.12, "loco_black")
        v.box("hull", -0.1, 0.1, 3.85, 4.9, -0.04, 0.04, "loco_black")                  # mât et drapeau rouge
        v.box("hull", 0.1, 0.9, 4.3, 4.9, -0.02, 0.02, "loco_red")
        return v

    def tgv_bogies(v, L2=5.0):
        """Châssis caréné et deux bogies à roues pleines (jupes latérales)."""
        v.box("hull", -L2, L2, 0.55, 1.0, -1.45, 1.45, "tgv_dark")
        for bx in (-3.3, 3.3):
            for ax in (-0.5, 0.5):
                for z in (-1.15, 1.15):
                    wheel(v, bx + ax, z, 0.4)
            v.box("hull", bx - 1.1, bx + 1.1, 0.4, 0.6, -1.2, 1.2, "tgv_dark")

    def tgv_body(v, x0, x1, bands=True):
        """Caisse de 3,6 blocs de large : bas gris métallisé, haut blanc, liseré bleu, toit arrondi (3 marches)."""
        v.box("hull", x0, x1, 1.0, 1.9, -1.8, 1.8, "tgv_gray")
        v.box("hull", x0, x1, 1.9, 3.0, -1.8, 1.8, "tgv_white")
        v.box("hull", x0, x1, 3.0, 3.4, -1.5, 1.5, "tgv_white")
        v.box("hull", x0, x1, 3.4, 3.7, -1.0, 1.0, "tgv_white")
        if bands:
            for s in (-1, 1):
                z0, z1 = (1.8, 1.86) if s > 0 else (-1.86, -1.8)
                v.box("hull", x0, x1, 1.75, 1.95, z0, z1, "tgv_blue")

    @reg
    def tgv_power():
        """Motrice de TGV : nez profilé (4 marches inclinées), pare-brise panoramique teinté, phares blancs, liseré rouge,
        pantographes sur le toit. La motrice arrière est cette même pièce tournée de 180 degrés."""
        v = V("tgv_power_car", 3.5)
        tgv_bogies(v)
        tgv_body(v, -5.0, 3.4)
        steps = [(3.4, 4.1, 1.0, 3.0, 1.65), (4.1, 4.8, 1.0, 2.5, 1.45), (4.8, 5.4, 1.0, 2.0, 1.2), (5.4, 5.8, 1.0, 1.6, 0.95)]
        for a, b, y0, y1, hw in steps:
            v.box("hull", a, b, y0, y0 + 0.9, -hw, hw, "tgv_gray")
            v.box("hull", a, b, y0 + 0.9, y1, -hw, hw, "tgv_white")
        v.box("hull", 3.4, 5.8, 1.0, 1.3, -1.65, 1.65, "tgv_dark")                         # chasse-neige
        v.box("hull", 3.4, 4.1, 2.4, 3.0, -1.3, 1.3, "glass")                              # pare-brise panoramique
        v.box("hull", 4.1, 4.8, 2.1, 2.5, -1.1, 1.1, "glass")
        v.box("hull", 2.2, 3.4, 3.0, 3.5, -1.2, 1.2, "glass")
        for z in (-1.0, 1.0):
            v.box("hull", 5.4, 5.85, 1.3, 1.55, z - 0.2, z + 0.2, "lamp_yellow")             # phares LED
        v.box("hull", 3.4, 5.4, 1.45, 1.6, -1.45, 1.45, "tgv_red")                          # liseré rouge
        for s in (-1, 1):                                                                  # portes de cabine et vitres latérales
            v.box("hull", 1.4, 3.0, 2.2, 2.8, s * 1.8 + (0.03 if s > 0 else -0.09), s * 1.8 + (0.09 if s > 0 else -0.03), "glass")
        for x in (-2.8, 0.2):                                                              # pantographes
            v.box("hull", x - 0.6, x + 0.6, 3.7, 3.78, -0.7, 0.7, "loco_black")
            v.box("hull", x - 0.05, x + 0.05, 3.7, 4.4, -0.5, -0.4, "loco_black")
            v.box("hull", x - 0.05, x + 0.05, 3.7, 4.4, 0.4, 0.5, "loco_black")
            v.box("hull", x - 0.5, x + 0.5, 4.4, 4.46, -0.6, 0.6, "tgv_dark")
        v.box("hull", -5.0, -4.9, 1.0, 3.4, -1.8, 1.8, "tgv_gray")                          # face arrière
        v.box("hull", -4.0, -1.0, 1.2, 1.7, -1.82, 1.82, "tgv_dark")                        # grilles de ventilation
        v.seat(2.3, 1.1, -0.7)                                                             # conducteur
        return v

    @reg
    def tgv_coach():
        """Voiture voyageurs de TGV : baies vitrées continues, portes coulissantes au milieu, 2 + 2 sièges (16 places)."""
        v = V("tgv_coach", 3.5)
        L2 = 5.0
        tgv_bogies(v)
        tgv_body(v, -L2, L2)
        for s in (-1, 1):
            z0, z1 = (1.8, 1.86) if s > 0 else (-1.86, -1.8)
            for a, b in ((-4.6, -1.5), (1.5, 4.6)):
                v.box("hull", a, b, 2.1, 2.8, z0, z1, "glass")                              # baies vitrées
            v.box("doors", -1.0, 0.0, 1.0, 2.95, z0 - 0.02 * s, z1 + 0.02 * s, "tgv_dark")
            v.box("doors_f", 0.0, 1.0, 1.0, 2.95, z0 - 0.02 * s, z1 + 0.02 * s, "tgv_dark")
        for x in (-3.6, -2.4, 2.4, 3.6):                                                    # sièges (dossier + assise)
            for z in (-1.2, -0.6, 0.6, 1.2):
                v.box("hull", x - 0.3, x + 0.3, 1.0, 1.35, z - 0.25, z + 0.25, "bench")
                v.box("hull", x - 0.35, x - 0.25, 1.35, 1.95, z - 0.25, z + 0.25, "bench")
        for x in (-3.0, 0.0, 3.0):
            v.box("hull", x - 0.1, x + 0.1, 3.3, 3.4, -0.1, 0.1, "lamp_yellow")
        for x in (-3.6, -2.4, 2.4, 3.6):
            for z in (-0.9, 0.9):
                v.seat(x, 1.0, z)
        return v

    @reg
    def tgv_bar():
        """Voiture-bar panoramique : verrière latérale haute, comptoir central, tables hautes et tabourets (8 places)."""
        v = V("tgv_bar_coach", 3.5)
        L2 = 5.0
        tgv_bogies(v)
        tgv_body(v, -L2, L2)
        for s in (-1, 1):
            z0, z1 = (1.8, 1.86) if s > 0 else (-1.86, -1.8)
            v.box("hull", -4.7, 4.7, 2.0, 3.0, z0, z1, "glass")                             # grandes baies
            v.box("hull", -0.9, 0.9, 1.0, 3.0, z0, z1, "tgv_dark")                          # porte
            v.box("doors", -1.0, 0.0, 1.0, 2.95, z0 - 0.02 * s, z1 + 0.02 * s, "tgv_dark")
            v.box("doors_f", 0.0, 1.0, 1.0, 2.95, z0 - 0.02 * s, z1 + 0.02 * s, "tgv_dark")
        v.box("hull", -2.2, 2.2, 1.0, 1.9, -0.3, 0.3, "plank")                              # comptoir
        v.box("hull", -2.2, 2.2, 1.9, 2.0, -0.4, 0.4, "tgv_white")
        for x in (-3.8, 3.8):                                                               # tables hautes et tabourets
            v.box("hull", x - 0.4, x + 0.4, 1.7, 1.8, -0.4, 0.4, "plank")
            v.box("hull", x - 0.05, x + 0.05, 1.0, 1.7, -0.05, 0.05, "tgv_dark")
            for z in (-1.1, 1.1):
                v.box("hull", x - 0.2, x + 0.2, 1.0, 1.4, z - 0.2, z + 0.2, "bench")
                v.seat(x, 1.0, z)
        for x in (-3.8, -1.5, 1.5, 3.8):
            for z in (-1.1, 1.1):
                if abs(x) < 3:
                    v.box("hull", x - 0.2, x + 0.2, 1.0, 1.4, z - 0.2, z + 0.2, "bench")
                    v.seat(x, 1.0, z)
        for x in (-3.0, 0.0, 3.0):
            v.box("hull", x - 0.1, x + 0.1, 3.3, 3.4, -0.1, 0.1, "lamp_yellow")
        return v

    return trains

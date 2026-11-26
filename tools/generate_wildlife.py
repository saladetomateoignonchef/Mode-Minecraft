"""Textures de la faune sauvage (64 x 64) : une couleur principale, deux teintes d'accent (bandes de la texture lues par WildlifeRenderer) et un léger grain.
Écrit assets/modeminecraft/textures/entity/wild_<espece>.png et les noms de langue."""
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "modeminecraft")
COLORS = {
    "eagle": ((104, 72, 44), (238, 238, 232), (232, 190, 40)),
    "gull": ((236, 236, 236), (150, 156, 164), (238, 168, 40)),
    "scorpion": ((196, 146, 84), (112, 72, 42), (58, 30, 22)),
    "snake": ((112, 124, 60), (66, 76, 34), (200, 50, 50)),
    "deer": ((166, 116, 66), (238, 232, 222), (88, 58, 34)),
}
NAMES = {"eagle": ("Aigle", "Eagle"), "gull": ("Mouette", "Seagull"), "scorpion": ("Scorpion", "Scorpion"), "snake": ("Serpent", "Snake"), "deer": ("Cerf", "Deer")}


def main():
    rnd = random.Random(5)
    for name, (base, a, b) in COLORS.items():
        img = Image.new("RGBA", (64, 64))
        for y in range(64):
            col = base if y < 48 else a if y < 56 else b
            for x in range(64):
                k = rnd.randint(-9, 9)
                img.putpixel((x, y), (max(0, min(255, col[0] + k)), max(0, min(255, col[1] + k)), max(0, min(255, col[2] + k)), 255))
        img.save(os.path.join(ROOT, "textures", "entity", f"wild_{name}.png"))
    for lang, idx in (("fr_fr", 0), ("en_us", 1)):
        p = os.path.join(ROOT, "lang", f"{lang}.json")
        d = json.load(open(p, encoding="utf-8"))
        for name, nm in NAMES.items():
            d[f"entity.modeminecraft.{name}"] = nm[idx]
        json.dump(d, open(p, "w", encoding="utf-8"), indent=2, ensure_ascii=False)
        open(p, "a").write("\n")
    print("textures de faune écrites")


if __name__ == "__main__":
    main()

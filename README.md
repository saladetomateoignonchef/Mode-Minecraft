# Mode Minecraft

Mod **NeoForge** pour **Minecraft 1.21.1** : tout ce qu'on a toujours voulu dans Minecraft.

## Contenu (v0.1.0)

### Minerai de rubis
- **Minerai de rubis** (pierre + abîmes) : généré dans l'Overworld entre Y=-64 et Y=32 (pic vers Y=-16).
- Se mine avec une pioche en **fer** minimum, compatible Fortune et Toucher de soie.
- **Bloc de rubis** (9 rubis), utilisable comme base de balise.

### Outils et armure en rubis
Matériau entre le diamant et la netherite (1800 de durabilité, enchantabilité 15).
- Pioche, hache, pelle, houe.
- Casque, plastron, jambières, bottes. **Bonus du set complet : Force I.**

### Armes de mêlée
| Arme | Dégâts | Vitesse | Particularité |
|---|---|---|---|
| Épée en rubis | 7.5 | 1.6 | — |
| Dague en rubis | 5.5 | 2.5 | Très rapide |
| Marteau de guerre | 11.5 | 0.6 | Onde de choc : blesse et repousse les ennemis autour |

### Armes magiques
| Arme | Pouvoir | Craft (sans forme) |
|---|---|---|
| Épée de flammes | Enflamme la cible 5 s | Épée en rubis + bâton de blaze + boule de feu |
| Lame de givre | Lenteur III + gel | Épée en rubis + glace bleue + boule de neige |
| Dague venimeuse | Poison II 5 s | Dague en rubis + œil d'araignée + œil d'araignée fermenté |
| Lame de l'End | Clic droit : téléportation de 8 blocs (3 s de recharge) | Épée en rubis + œil de l'Ender + 2 perles de l'Ender |
| Marteau du tonnerre | Onde de choc + clic droit : foudre jusqu'à 32 blocs (5 s de recharge) | Marteau de guerre + paratonnerre + bloc de diamant |

### Armes à distance
- **Arc en rubis** : flèches plus puissantes (+1.5 dégâts de base), 768 de durabilité.
- **Bâton de feu** : clic droit pour lancer une boule de feu.

### Armes à feu
Toutes les armes à feu, munitions et accessoires se fabriquent à la **table d'armement**.

| Arme | Calibre | Dégâts | Chargeur | Cadence | Particularité |
|---|---|---|---|---|---|
| Pistolet | 9 mm | 6 | 12 | 240 cps/min | Précis, semi-automatique |
| Fusil d'assaut | 5,56 mm | 5 | 30 | 400 cps/min | Automatique |
| **AK-47** | 7,62 mm | 7 | 30 | 400 cps/min | Automatique, gros dégâts, beaucoup de recul |
| **M4** | 5,56 mm | 5,5 | 30 | 600 cps/min | Automatique, précis |
| **FAMAS** | 5,56 mm | 4,5 | 25 | 1200 cps/min | Automatique, cadence énorme |
| Fusil à pompe | calibre 12 | 8 × 3,5 | 6 | 67 cps/min | 8 plombs |
| Fusil de sniper | 7,62 mm | 22 | 5 | 40 cps/min | Lunette x5 intégrée |
| Lance-roquettes | roquette RPG | explosion 3 | 1 | — | |
| **RPG-7** | roquette RPG | explosion 4 | 1 | — | La roquette est visible sur l'arme quand elle est chargée |

**Commandes :** clic droit = tirer (rechargement automatique si le chargeur est vide), **R** = recharger,
**s'accroupir** = viser (moins de dispersion, zoom et réticule si un viseur est monté). Munitions infinies en créatif.

**Mécaniques :** tir à la tête x1,5, les balles cassent le verre, recul de la caméra,
modèles 3D en main (icône 2D dans l'inventaire), pose « en joue » à la 3e personne,
message de mort « X a été abattu par Y ».

### Table d'armement
Se fabrique à la table de craft : `lingot de fer, poudre à canon, lingot de fer` / `lingot de fer, table de craft, lingot de fer` / `3 planches`.
Clic droit dessus : choisir un onglet (Armes / Munitions / Accessoires), un objet, puis **Fabriquer**
(Maj + clic = jusqu'à 10 d'un coup). Les matériaux sont pris directement dans l'inventaire.
Les recettes sont des fichiers JSON modifiables dans `data/modeminecraft/recipe/armament/`.

### Table de modification d'armes
Se fabrique à la table de craft : `_, vitre, _` / `lingot de fer, table de forgeron, lingot de fer` / `3 planches`.
Poser l'arme au centre, puis les accessoires autour. Les statistiques finales s'affichent à droite,
et les flèches changent le **skin** : Défaut, Désert, Camouflage forêt, Camouflage arctique, Or, Rubis.

| Emplacement | Accessoire | Effet |
|---|---|---|
| Viseur | Viseur point rouge | zoom x1,2 en visée, dispersion en visée -30 % |
| Viseur | Viseur holographique | zoom x1,3, réticule holographique, dispersion en visée -40 % |
| Viseur | Lunette x4 | zoom x4 avec lunette, dispersion en visée -60 % |
| Bouche | Silencieux | tir discret (son réduit, pas de flash, pas détecté par le sculk), dégâts -10 % |
| Bouche | Compensateur | recul -40 % |
| Poignée | Poignée verticale | recul -35 % |
| Poignée | Poignée inclinée | dispersion -30 % |
| Chargeur | Chargeur 24 coups | 24 balles |
| Chargeur | Chargeur 36 coups | 36 balles, rechargement +10 % |
| Chargeur | Chargeur camembert | 75 balles, rechargement +60 %, dispersion +10 % |

Un chargeur ne se monte que s'il contient plus de balles que celui d'origine. En le retirant,
les balles en trop sont rendues. Les accessoires montés sont **visibles sur le modèle 3D**.


Toutes les armes de mêlée sont enchantables comme leurs équivalents vanilla (Tranchant, Aura de feu, Puissance...).
Tous les items sont dans l'onglet créatif **Mode Minecraft**.

## Développer

Prérequis : **Java 21** (JDK).

```bash
./gradlew runClient     # lance le jeu avec le mod
./gradlew build         # produit build/libs/modeminecraft-<version>.jar
```

Le `.jar` produit se met dans le dossier `mods/` d'une installation NeoForge 1.21.1.

Avec IntelliJ IDEA : *Open* → choisir le dossier du projet, attendre l'import Gradle,
puis utiliser les configurations de lancement générées (Client, Server).

### Ajouter une nouvelle arme à feu
1. `ModItems.java` : copier par exemple `AK47` et changer ses statistiques avec le builder `GunStats`
   (dégâts, chargeur, cadence, recharge, dispersion, recul, automatique, munitions, emplacements d'accessoires...).
2. `tools/generate_textures.py` : ajouter son icône 2D (grille de lettres dans `GUNS`).
3. `tools/generate_gun_models.py` : ajouter sa forme 3D (liste de pavés + chargeur séparé), puis relancer les deux scripts.
4. `client/model/GunMounts.java` : indiquer où se fixent les accessoires.
5. Traductions (`lang/*.json`) et recette dans `data/modeminecraft/recipe/armament/`.

### Ajouter un skin
Ajouter une valeur dans `GunSkin.java` et sa palette dans `SKINS` de `tools/generate_gun_models.py`,
relancer le script et ajouter la traduction `skin.modeminecraft.<nom>`.

## Organisation du code

```
src/main/java/fr/modeminecraft/
├── ModeMinecraft.java          # point d'entrée du mod
├── registry/                   # enregistrement : items, blocs, matériaux, onglet créatif
├── item/                       # comportements des armes spéciales
│   └── gun/                    # armes à feu : GunItem (tir/rechargement), GunStats, accessoires, skins
├── block/                      # tables d'armement et de modification
├── menu/                       # logique des interfaces des tables (côté serveur)
├── recipe/                     # recettes de la table d'armement
├── entity/                     # projectiles : balle, roquette
├── network/                    # paquet réseau "recharger" (touche R)
├── event/                      # événements du jeu (bonus d'armure)
└── client/                     # code client : touche R, HUD, visée, écrans des tables,
                                # model/ = assemblage du modèle 3D (skin + accessoires)
src/main/resources/
├── assets/modeminecraft/       # modèles, textures, traductions (en_us, fr_fr)
└── data/                       # recettes, loot tables, tags, génération du minerai
tools/generate_textures.py      # génère les textures pixel art (provisoires) + la lunette du sniper
tools/generate_gun_models.py    # génère les modèles 3D des armes, accessoires et skins
```

### Ajouter une nouvelle arme magique
1. Dans `ModItems.java`, copier par exemple `VENOM_DAGGER` et changer l'effet dans le lambda `(target, attacker) -> ...`.
2. Ajouter le modèle `assets/modeminecraft/models/item/<nom>.json` et la texture `textures/item/<nom>.png`.
3. Ajouter le nom et l'infobulle dans `lang/fr_fr.json` et `lang/en_us.json`.
4. Ajouter une recette dans `data/modeminecraft/recipe/` et l'item au tag `data/minecraft/tags/item/swords.json`.

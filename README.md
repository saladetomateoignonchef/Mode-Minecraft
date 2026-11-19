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
| Arme | Dégâts | Chargeur | Cadence | Munitions | Particularité |
|---|---|---|---|---|---|
| Pistolet | 6 | 12 | 4 tirs/s | Balles | Précis, semi-automatique |
| Fusil d'assaut | 5 | 30 | 6,7 tirs/s | Balles | **Automatique** : maintenir le clic droit |
| Fusil à pompe | 8 × 3,5 | 6 | ~1 tir/s | Cartouches | 8 plombs, dévastateur de près |
| Fusil de sniper | 22 | 5 | 0,7 tir/s | Balles | **Zoom ×5** en s'accroupissant, précision parfaite en visée |
| Lance-roquettes | explosion | 1 | — | Roquettes | Roquette explosive (casse les blocs si `mobGriefing` est activé) |

**Commandes :**
- **Clic droit** : tirer (le chargeur vide se recharge automatiquement).
- **R** : recharger (modifiable dans *Options > Commandes > Mode Minecraft*).
- **S'accroupir** : viser (moins de dispersion, moins de recul).
- En **créatif**, les munitions sont infinies.

**Mécaniques :**
- Le chargeur est stocké dans l'arme : la barre sous l'icône et le HUD (à droite de la barre d'inventaire) affichent `chargeur / max [réserve]`.
- Le temps de rechargement s'affiche comme une recharge sur l'icône de l'arme.
- **Tir à la tête = dégâts ×1,5**.
- Les balles **cassent le verre** (si `mobGriefing` est activé).
- **Modèles 3D** en main et au sol (icône 2D dans l'inventaire), recul de la caméra à chaque tir,
  et pose « en joue » à deux mains en vue à la 3ᵉ personne.
- **Lunette du sniper** avec réticule gradué et échelle de chute de balle (100 à 500 m).
- Message de mort personnalisé : « X a été abattu par Y ».

**Crafts :**
- Balles ×12 : lingot de cuivre + poudre à canon + pépite de fer (sans forme)
- Cartouches ×6 : papier + poudre à canon + 2 pépites de fer (sans forme)
- Roquettes ×2 : lingot de fer / TNT / poudre à canon (en colonne)
- Les armes se fabriquent avec des lingots de fer, des planches, de la poudre à canon, et du rubis pour les meilleures (voir `data/modeminecraft/recipe/`).

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
Dans `ModItems.java`, copier par exemple `PISTOL` et changer ses statistiques (`GunStats`) :
dégâts, nombre de plombs, taille du chargeur, cadence, temps de recharge, vitesse, dispersion, recul, automatique ou non, munitions et son.
Puis ajouter sa forme 3D dans `tools/generate_gun_models.py` (liste de pavés), une texture d'icône,
les traductions et une recette.
Enfin, l'ajouter à la liste dans `ClientSetup.registerClientExtensions` pour la pose « en joue ».

## Organisation du code

```
src/main/java/fr/modeminecraft/
├── ModeMinecraft.java          # point d'entrée du mod
├── registry/                   # enregistrement : items, blocs, matériaux, onglet créatif
├── item/                       # comportements des armes spéciales
│   └── gun/                    # armes à feu (GunItem = toute la logique de tir/rechargement)
├── entity/                     # projectiles : balle, roquette
├── network/                    # paquet réseau "recharger" (touche R)
├── event/                      # événements du jeu (bonus d'armure)
└── client/                     # code client : touche R, HUD munitions, zoom, rendu des projectiles
src/main/resources/
├── assets/modeminecraft/       # modèles, textures, traductions (en_us, fr_fr)
└── data/                       # recettes, loot tables, tags, génération du minerai
tools/generate_textures.py      # génère les textures pixel art (provisoires) + la lunette du sniper
tools/generate_gun_models.py    # génère les modèles 3D des armes à feu
```

### Ajouter une nouvelle arme magique
1. Dans `ModItems.java`, copier par exemple `VENOM_DAGGER` et changer l'effet dans le lambda `(target, attacker) -> ...`.
2. Ajouter le modèle `assets/modeminecraft/models/item/<nom>.json` et la texture `textures/item/<nom>.png`.
3. Ajouter le nom et l'infobulle dans `lang/fr_fr.json` et `lang/en_us.json`.
4. Ajouter une recette dans `data/modeminecraft/recipe/` et l'item au tag `data/minecraft/tags/item/swords.json`.

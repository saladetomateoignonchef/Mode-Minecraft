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

Toutes les armes sont enchantables comme leurs équivalents vanilla (Tranchant, Aura de feu, Puissance...).
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

## Organisation du code

```
src/main/java/fr/modeminecraft/
├── ModeMinecraft.java          # point d'entrée du mod
├── registry/                   # enregistrement : items, blocs, matériaux, onglet créatif
├── item/                       # comportements des armes spéciales
├── event/                      # événements du jeu (bonus d'armure)
└── client/                     # code uniquement client (animation de l'arc)
src/main/resources/
├── assets/modeminecraft/       # modèles, textures, traductions (en_us, fr_fr)
└── data/                       # recettes, loot tables, tags, génération du minerai
tools/generate_textures.py      # génère les textures pixel art (provisoires)
```

### Ajouter une nouvelle arme magique
1. Dans `ModItems.java`, copier par exemple `VENOM_DAGGER` et changer l'effet dans le lambda `(target, attacker) -> ...`.
2. Ajouter le modèle `assets/modeminecraft/models/item/<nom>.json` et la texture `textures/item/<nom>.png`.
3. Ajouter le nom et l'infobulle dans `lang/fr_fr.json` et `lang/en_us.json`.
4. Ajouter une recette dans `data/modeminecraft/recipe/` et l'item au tag `data/minecraft/tags/item/swords.json`.

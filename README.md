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

### Armes à feu (44 armes réelles)
Toutes les armes à feu, munitions et accessoires se fabriquent à la **table d'armement**.

| Catégorie | Armes (calibre) |
|---|---|
| Pistolets | Glock 17 (9 mm), Desert Eagle (.50 AE), Beretta M9 (9 mm), Colt M1911 (.45 ACP), Colt Python (.357, recharge balle par balle), Makarov PM (9 mm) |
| Pistolets-mitrailleurs | MP5 (9 mm), UZI (9 mm), FN P90 (5,7 mm, chargeur de 50), MP7 (4,6 mm, **perce l'armure**), Thompson M1928 (.45, camembert), Kriss Vector (.45, cadence énorme), PPSh-41 (7,62×25, camembert de 71) |
| Fusils d'assaut | AK-47 (7,62), AK-74 (5,45), M4 (5,56), HK416 (5,56), FAMAS (5,56, 1200 cps/min), FN SCAR-H (7,62), HK G36 (5,56, lunette intégrée), Steyr AUG (5,56, bullpup + lunette), Tavor TAR-21 (5,56), M16A4 (5,56, **rafale de 3**), HK G3 (7,62), AN-94 (5,45, **rafale de 2**) |
| Fusils de précision | Barrett M82 (.50 BMG, **traverse 3 ennemis**), AWM (.338), Dragunov SVD (7,62×54R), Mosin-Nagant (7,62×54R), Kar98k (7,92), M14 EBR (7,62) |
| Fusils à pompe (calibre 12) | Remington 870, Benelli M4 (semi-auto), SPAS-12, AA-12 (**automatique**), fusil à canon scié (2 coups) |
| Mitrailleuses | M249 (5,56), PKM (7,62×54R), M60 (7,62), Minigun M134 (7,62, **1 s de lancement puis 20 balles/s**) |
| Lanceurs | RPG-7 (roquette RPG), M32 MGL (grenades 40 mm, barillet de 6), M79 (grenade 40 mm), AT4 (**jetable**, vendu chargé) |

**17 munitions réalistes :** 9 mm, .45 ACP, .50 AE, .357 Magnum, 5,7 mm, 4,6 mm, 7,62×25, 5,45 mm, 5,56 mm,
7,62 mm, 7,62×54R, 7,92 mm, .338 Lapua, .50 BMG, cartouches calibre 12, grenades 40 mm, roquettes RPG.

**Commandes (façon FPS) :**
- **Clic gauche** : tirer (maintenir pour le tir continu ; rechargement automatique si le chargeur est vide).
  Avec une arme en main, le clic gauche ne casse pas de bloc.
- **Clic droit maintenu** : viser (l'arme s'aligne au centre de l'écran en ~0,2 s, léger zoom). Un simple clic droit
  sur une porte, une table... fonctionne toujours. Fusil à canon scié : le clic droit tire les **deux canons** d'un coup.
- **Shift** (en visant à la lunette) : stabiliser / retenir son souffle.
- **B** : changer de mode de tir — **coup par coup → rafale → automatique** (selon les modes de l'arme).
  Comme une vraie détente, il faut relâcher le clic entre deux tirs en coup par coup et entre deux rafales.
  Cadences réelles en coups par minute (M4 800 CPM, Vector 1200, minigun 2000 après 0,6 s de lancement...).
- **H** : inspecter l'arme (on la tourne pour l'admirer) et vérifier la balle dans la chambre.
- **R** : recharger. Cartouche par cartouche (pompes, revolvers, fusils à verrou) : **tirer interrompt le rechargement**
  (les cartouches déjà mises restent).
- **C** : coup de crosse (ou de baïonnette).
- **Double appui sur s'accroupir** (sans viser) : s'allonger / se relever (aussi en sautant), avec ou sans arme.
  Allongé : recul et dispersion divisés par 2.
- Toutes les touches (R, B, H, G type de munition, T laser, V tir sous le canon, C crosse, A / E se pencher,
  Shift stabiliser) se changent dans *Options > Commandes > Mode Minecraft*. Munitions infinies en créatif.

**Dégâts (cahier des charges) :** tête x2 (x2,5 au fusil de précision, avec un « ding » métallique), torse x1,
bras et jambes x0,75 ; chaque calibre ignore une partie de l'armure (9 mm 10 %, 5,56 30 %, 7,62 45 %, .338 60 %,
.50 BMG 70 %, munitions perforantes +50 %) ; les gros calibres traversent un ennemi pour toucher celui de derrière,
et toutes les balles traversent vitres, feuilles, laine et bois fin (-30 % de dégâts par paroi) selon leur puissance.
Uzi : +10 % de vitesse de marche ; M249 et mitrailleuses : -15 % ; minigun : -25 % et pas de sprint.

**Réalisme :** balle chambrée (le HUD affiche `chargeur+1`) ; arme neuve ou vidée = le personnage tire la culasse
(au clic droit, ou automatiquement à la fin d'un rechargement à vide) ; culasse bloquée en arrière quand
l'arme est vide (pistolets, fusils type AR, SCAR, Barrett...) ; flash 3D au bout du canon ; douilles 3D éjectées
(laiton, acier ou cartouche rouge) ; l'arme s'incline pendant le rechargement.

**Munitions spéciales** (table d'armement, onglet *Spéciales* : 16 munitions normales + un ingrédient,
touche **G** pour changer le type chargé ; les balles chargées sont rendues) :
perforantes (flint + fer), explosives (TNT), traçantes (glowstone, traînée lumineuse visible par tous),
incendiaires (poudre de blaze), pointe creuse (pépites de fer), subsoniques (laine), caoutchouc (boules de slime),
marqueuses (poche d'encre lumineuse), venimeuses (yeux d'araignée), givrantes (glace compacte),
électriques (redstone + cuivre), rubis (rubis) ; pour les fusils à pompe : slug, fléchettes, dragon's breath,
chevrotine magnum. Une pastille de couleur sur l'icône indique le type.

**Mécaniques :** tir à la tête x1,5, les balles cassent le verre, recul de la caméra,
**culasse animée au tir** (pistolets : toute la culasse recule ; fusils : culasse et levier d'armement ;
fusils à verrou : le verrou est manœuvré après le tir ; fusils à pompe : la pompe recule puis revient ;
minigun : les canons tournent),
modèles 3D texturés en main (métal brossé, bois veiné, camouflages), icônes 32x32 dans l'inventaire,
pose « en joue » à la 3e personne,
message de mort « X a été abattu par Y ».

### Réalisme du tir
- **Zones** : tir à la tête = dégâts x2, aux jambes = x0,75 et la cible est ralentie.
- **Balistique** : les balles retombent avec la distance et sont **déviées par le vent** (plus fort sous la pluie
  et l'orage). Avec une lunette (x4 et plus), le HUD affiche la force et le sens du vent.
- **Pénétration** : les balles cassent le verre et traversent feuilles, laine et bois selon leur puissance
  (sniper > fusil > pistolet, munitions perforantes x2), en perdant des dégâts. Pas la pierre.
- **Ricochets** : une balle qui touche la pierre ou le métal très en biais peut rebondir (étincelles).
- **Respiration** : avec une lunette, la visée bouge légèrement (moins allongé ou avec un bipied).
  **Shift** = retenir son souffle 5 secondes ; après, la visée tremble plus le temps de reprendre son souffle.
- **Bruit** : un tir non silencieux attire les monstres proches (rayon selon le bruit de l'arme).
- **Se pencher** : en visant (clic droit), **A / E** (AZERTY) penchent à gauche / à droite pour tirer depuis un coin de mur.
  Hors visée, A et E gardent leur rôle (jeter / inventaire).
- **Surchauffe** (mitrailleuses) : jauge de chaleur ; à 100 l'arme se bloque jusqu'à refroidir. Avec un
  **canon de rechange** dans l'inventaire, **R** change le canon (puis 20 s avant de pouvoir rechanger).

### Visuel et sons
- **Vraie visée** : en visant, l'arme vient au centre de l'écran, l'œil tout près du viseur : on regarde à travers
  la vitre (transparente) du point rouge, de l'holo, du Kobra..., l'œil pile au centre optique de chaque viseur.
  Les **lunettes grossissantes** (x2 et plus : ACOG x3, x6, x12, sniper...) **remplissent tout l'écran**
  une fois l'arme épaulée, comme quand on colle l'œil à l'oculaire.
- **Bras en 1re personne** : main droite sur la poignée, main gauche sous le garde-main (ou sur la poignée avant).
- **Rechargement animé** : l'ancien chargeur sort, la main va en chercher un autre, le nouveau entre, puis la
  culasse est armée (visible aussi en 3e personne par les autres joueurs).
- **Sons propres à chaque famille d'arme** (pistolet, magnum, PM, 5,56, AK, 7,62 OTAN, sniper, .50, pompe,
  minigun, lance-grenades, roquette), tirs silencieux, **écho lointain** pour les joueurs éloignés, bruits de
  chargeur, culasse, verrou, pompe, cartouches, et **douilles qui tintent** en touchant le sol.
  Sons générés par `tools/generate_sounds.py` : on peut remplacer les `.ogg` par ses propres sons.
- **Flash de bouche** qui éclaire vraiment les alentours la nuit (pas avec un silencieux).
- **Hit marker** : croix blanche quand une balle touche, jaune à la tête, rouge quand elle tue, avec un petit son.
- **Impacts** : trous de balle sur les blocs (30 secondes), éclats et étincelles.
- **Armes rangées visibles** : la 1re arme longue de la barre d'action dans le dos, le 1er pistolet à la hanche.

### Table d'armement
Se fabrique à la table de craft : `lingot de fer, poudre à canon, lingot de fer` / `lingot de fer, table de craft, lingot de fer` / `3 planches`.
Clic droit dessus : choisir un onglet (Armes / Munitions / Accessoires), un objet, puis **Fabriquer**
(Maj + clic = jusqu'à 10 d'un coup). Les matériaux sont pris directement dans l'inventaire.
Les recettes sont des fichiers JSON modifiables dans `data/modeminecraft/recipe/armament/`.

### Table de modification d'armes
Se fabrique à la table de craft : `_, vitre, _` / `lingot de fer, table de forgeron, lingot de fer` / `3 planches`.
Poser l'arme au centre, puis les accessoires autour. Les statistiques finales s'affichent à droite,
et les flèches changent le **skin** : Défaut, Désert, Camouflage forêt, Camouflage arctique, Or, Rubis.

La table a **7 emplacements** : viseur, bouche, poignée avant, chargeur, poignée pistolet, crosse et rail latéral (laser, fusils d'assaut).
**138 accessoires** au total ; l'infobulle de chacun donne ses effets exacts.

| Emplacement | Accessoires | Effets principaux |
|---|---|---|
| Viseur (27) | **lunettes de sniper Leupold x6, Schmidt & Bender x12, Nightforce x20, S&B PM II 5-25x (fusils de précision uniquement)**, point rouge, holographique, Kobra, RMR, réflexe mini, hausse ghost ring, prisme x2, ACOG x3, PSO-1, chasse x4, ELCAN (x1/x4), x4 + viseur à 45° (x4/x1), holo + loupe x3 (x1,3/x3), **lunette variable 1-6x**, x6, x8, x12, télémétrique, numérique, rubis, **thermique**, **vision nocturne**, laser PEQ-15 | zoom, réticule propre à chaque viseur, précision en visée |
| Bouche (30) | compensateur, **frein de bouche** (-20 % de recul), **cache-flamme** (pas de flash), silencieux générique + 26 silencieux réels selon le calibre : Omega 9K, Osprey 45, Gemtech GM-9, B&T Impuls-IIA (9 mm / .45) ; Dead Air Wolfman, Hybrid 46, Rugged Obsidian 45, AAC Ti-RANT 45 (PM) ; SureFire SOCOM556, Huxwrx Flow, B&T Rotex-V, Dead Air Sandman-S (5,56) ; PBS-1, PBS-4, TGP-A (AK), Rugged Surge 7.62 ; SureFire SOCOM762, AAC SDN-6, Omega 300, Thunder Beast Ultra 9 (précision) ; **intégral MP5SD** (PM), **bouteille** (casse après 40 tirs), **rubis** (+5 % dégâts), **écho** (aucun bruit), **obsidienne**, OSS Helix 6 | tir discret, chaque silencieux a ses bonus |
| Poignée avant (22) | verticale (-15 % de recul), inclinée (+25 % de vitesse de visée), courte, Magpul AFG/RVG/MVG, BCM KAG, Tango Down, butée de main, bois, pliante, carbone, caoutchouc, rubis, lampe, laser, lampe + laser, Grip-Pod, bipied Harris, **baïonnette**, lance-grenades **M203** (M4, M16A4, HK416, Tavor), **M320** (M4, M16A4, HK416), **GP-25** (AK-47, AK-74, AN-94), **AG36** (G36), **FN EGLM** (SCAR-H), **HK79** (G3), fusil à pompe **M26 MASS** (M4, M16A4, HK416) | recul, dispersion, stabilité allongé, gadgets |
| Chargeur (5) | 24 coups, 36 coups, camembert, **chargeur étendu** (+35 %), **chargeur rapide** (rechargement +25 % plus rapide) | capacité, vitesse de rechargement |
| Poignée pistolet (20) | Magpul MOE/K2/MIAD, Hogue, BCM Gunfighter, Tango Down, ergonomique, noyer, nacrée, or, rubis, squelette, compacte, compétition, granitée, repose-pouce, Crimson Trace, rangement, empreintes, gel | recul, vitesse de visée, rechargement, sortie de l'arme |
| Rail latéral (9) | lasers rouge, vert, bleu, violet, jaune, orange, cyan, blanc, **arc-en-ciel** (fusils d'assaut uniquement) | laser visible, dispersion sans viser réduite |
| Crosse (20) | A2, CTR, Magpul PRS, squelette, pliante, filaire, bois, appui-joue, trou de pouce, **sans crosse**, attelle de pistolet (pistolets uniquement), tube tampon, Limbsaver, Knoxx, carbone, monopode, Accuracy International, Zhukov, SCAR, rubis | recul, mobilité, précision |

Fonctions spéciales :
- **Molette en visant** : change le grossissement (lunette variable 1-6x, loupe x3, ELCAN, viseur à 45°). Le zoom est progressif.
- **Lunette thermique** : l'image devient froide (bleue) et les créatures apparaissent en blanc, même derrière les blocs.
- **Lunette / lampe de vision nocturne** : vision nocturne tant que l'on vise.
- **Lunette télémétrique** : affiche la distance de la cible ; **lunette numérique** : affiche ses points de vie ;
  **lunette en rubis** : l'ennemi visé brille en rouge.
- **Laser** (poignée laser, PEQ-15, Crimson Trace) : point rouge visible par tous, dispersion sans viser réduite.
- **Lasers** : touche **T** pour allumer / éteindre ; faisceau et point lumineux de la couleur du laser, visibles par tous les joueurs.
- **Compatibilité réaliste** : un silencieux ne se monte que sur les armes de son calibre (ou sur les armes prévues, ex. PBS-1 sur AK-47) ; un lance-grenades seulement sur les armes qui l'acceptent en vrai. L'infobulle indique les armes ou calibres compatibles.
- **Lance-grenades / M26 MASS** : touche **V** pour charger puis tirer une grenade 40 mm / une cartouche de calibre 12.
- **Baïonnette** : +6 dégâts au corps à corps avec l'arme.
- **Bipied, Grip-Pod, monopode** : recul et dispersion très réduits quand on est allongé ou immobile en visée.
- **Délai pour sortir l'arme** : chaque arme a un temps de sortie (plus long pour les armes lourdes), modifié par la crosse et la poignée.
- Les poignées pistolet et les crosses **remplacent** celles d'origine sur le modèle 3D ; les armes bullpup n'acceptent pas de crosse.

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
   (dégâts, chargeur, cadence, recharge, dispersion, recul, automatique, rafale, perforation, munitions, emplacements...).
2. `tools/gun_designs.py` : ajouter une fonction `design_<nom>()` (souvent un simple appel à `rifle(...)`, `pistol(...)`,
   `bullpup(...)` ou `shotgun(...)` avec d'autres longueurs et couleurs).
3. Relancer `python3 tools/generate_gun_models.py` : modèle 3D, icône 2D (calculée depuis le modèle),
   skins et points de fixation des accessoires sont générés automatiquement.
4. Traductions (`lang/*.json`) et recette dans `data/modeminecraft/recipe/armament/`.

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
tools/gun_designs.py            # formes 3D des 44 armes (constructeurs paramétrés)
tools/generate_gun_models.py    # génère modèles 3D, icônes, skins, accessoires et gun_mounts.json
tools/generate_ammo.py          # munitions spéciales : pastilles, modèles, recettes
```

### Ajouter une nouvelle arme magique
1. Dans `ModItems.java`, copier par exemple `VENOM_DAGGER` et changer l'effet dans le lambda `(target, attacker) -> ...`.
2. Ajouter le modèle `assets/modeminecraft/models/item/<nom>.json` et la texture `textures/item/<nom>.png`.
3. Ajouter le nom et l'infobulle dans `lang/fr_fr.json` et `lang/en_us.json`.
4. Ajouter une recette dans `data/modeminecraft/recipe/` et l'item au tag `data/minecraft/tags/item/swords.json`.

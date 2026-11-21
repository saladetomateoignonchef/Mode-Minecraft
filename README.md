# Mode Minecraft

Mod **NeoForge** pour **Minecraft 1.21.1** : tout ce qu'on a toujours voulu dans Minecraft.

## Installation et mises à jour automatiques
1. **Une seule fois** : télécharge le dernier `.jar` sur la page
   [Releases](https://github.com/saladetomateoignonchef/Mode-Minecraft/releases/latest) et mets-le dans le dossier `mods`
   de ton profil NeoForge 1.21.1 (supprime l'ancienne version du mod si tu en as une).
2. **Ensuite, plus rien à faire** : à chaque lancement du jeu, le mod regarde s'il existe une version plus récente sur GitHub.
   Si oui, il la télécharge pendant que tu joues (une notification s'affiche) et la met à la place de l'ancienne
   **quand tu fermes le jeu**. Au lancement suivant, tu as la nouvelle version.
- Chaque fois que du code est envoyé sur GitHub, une nouvelle version (`v0.2.N`) est compilée et publiée automatiquement
  (`.github/workflows/release.yml`).
- Pour désactiver les mises à jour automatiques : `config/modeminecraft-client.toml` → `autoUpdate = false`.
- Si un jour la mise à jour automatique échoue (antivirus, dossier protégé...), il suffit de refaire l'étape 1.

## Contenu

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

### Armes blanches du cahier des charges (20)
Mécaniques communes :
- **Combos de 3 coups** : enchaîner les clics gauches (moins de 1,5 s entre deux coups) donne 3 animations différentes
  (balayage droite → gauche, revers gauche → droite, coup vertical ou estoc) ; le **3e coup fait +25 %**.
- **Parade** (clic droit maintenu) : l'arme en travers devant le torse, **-60 %** des dégâts au corps à corps, et les
  projectiles lents (flèches, boules de neige...) sont renvoyés. **Parade parfaite** (le coup arrive moins de 0,3 s après
  le début de la parade) : aucun dégât, son métallique, l'attaquant est étourdi 1 s.
- **Coup dans le dos** (cône de 90° derrière la cible) avec les armes légères : x2,5 (couteau), x3 (dague, karambit).

| Arme | Dégâts | Particularités |
|---|---|---|
| Couteau de combat | 6 | très rapide, +10 % de vitesse de marche |
| Karambit | 5,5 | tourne autour du poignet quand on le sort, +15 % de vitesse, ultra-rapide |
| Katana | 9 | portée 4,2, balayage ; **clic droit : ruée** de 4 blocs (3 s de recharge) |
| Masse d'armes | 11 | lente, ignore 50 % de l'armure, gros recul |
| Marteau de guerre | 14 | très lent ; **clic droit : onde de choc** qui projette les ennemis en l'air et brise les boucliers |
| Tomahawk | 8,5 | **clic droit : lancer** (12 dégâts), à ramasser ensuite |
| Matraque électrique | 5 | Lenteur IV + nausée 2,5 s, empêche les Creepers d'exploser |
| Machette | 8 | +50 % contre les morts-vivants, coupe plantes et toiles |
| Kukri | 8,5 | saignement (1 PV/s pendant 4 s) |
| Wakizashi | 7,5 | rapide, portée 3,5, parade parfaite plus facile (0,5 s) |
| Dague | 5,5 | portée 2,2, vitesse maximale |
| Claymore | 13 | portée 4,5, balayage qui touche jusqu'à 5 ennemis, lente |
| Rapière | 7,5 | estoc, perce 40 % de l'armure, portée 3,8, cible unique |
| Cimeterre | 8 | coup critique dès qu'on saute |
| Lance | 8 | portée 5, +50 % en courant ou à cheval |
| Hallebarde | 11,5 | portée 4,8, désarçonne les cavaliers |
| Faux de guerre | 10 | portée 4,5, attire les ennemis vers soi |
| Poing américain | 6 | portée 2, ultra-rapide, 25 % de chances d'étourdir 1 s |
| Pied-de-biche | 7,5 | clic droit : force portes et trappes en fer ; perce 30 % de l'armure |
| Sabre d'abordage | 8,5 | nage plus vite, +30 % de dégâts dans l'eau |

Toutes se fabriquent à la table d'armement (onglet Armes).

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

### Armes à feu (79 armes + 7 tourelles à poser)
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
- Toutes les touches (R, B, H, G type de munition, T laser, L lampe (Maj + L : mode), V tir sous le canon, C crosse, Y menu rapide des accessoires, N visière / vision nocturne du casque, O panneau des recettes, X s'allonger, A / E se pencher,
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

### Armes du cahier des charges (phase 8 : 33 nouvelles armes)
| Famille | Armes | Particularités |
|---|---|---|
| Pistolets | SIG P226, USP-S .45, FN Five-seveN, CZ-75 Auto, Luger P08, Glock 18, pistolet à silex | USP-S : silencieux intégré ; CZ-75 Auto et Glock 18 : automatiques ; Luger : la **genouillère se replie vers le haut** au tir ; silex : 1 coup, gros nuage de fumée, très puissant à bout portant |
| Revolvers | S&W Model 500, Colt Single Action Army, Mateba Unica 6, Taurus Raging Bull, Taurus Judge | Colt SAA : **fanning** (clic droit maintenu + clics gauches : la main gauche frappe le chien, 6 balles en ~1 s) ; Mateba : canon bas, recul -40 % ; Judge : tire des cartouches de calibre 12 |
| PM | UMP-45, PP-19 Bizon (chargeur hélicoïdal de 64), MAC-10, Scorpion EVO 3 | |
| Fusils | AK-74U (compact, portée réduite), Galil ACE, FN FAL, AK-12, M1 Garand | Garand : ne se recharge qu'à vide, le clip s'éjecte avec son « ping » |
| Pompe / précision | Mossberg 590, M24 SWS (pas de chute de balle sur 100 blocs), CheyTac M200 (.408, très longue portée) | |
| Lanceurs | M72 LAW (jetable), Panzerfaust 3 (50 dégâts directs, ignore 90 % de l'armure), **FIM-92 Stinger**, **FGM-148 Javelin** | Missiles à **tête chercheuse** : en visant, carré jaune + bip lent pendant la recherche (cône de 12°, 150 blocs, vue dégagée), carré rouge + bip continu une fois verrouillé (1,75 s) ; le missile corrige sa trajectoire de 6° par tick ; le Javelin monte d'abord à 55° puis plonge sur la cible |
| Spéciales | Arbalète tactique, taser, railgun en rubis, fusil à plasma | Arbalète : silencieuse, carreaux normaux / explosifs / incendiaires, **récupérables** ; taser : paralyse 4 s (non létal, empêche les Creepers d'exploser) ; railgun : 2 s de charge puis un rayon rouge qui traverse tous les ennemis alignés ; plasma : boules d'énergie qui brûlent, **sans munitions** mais il surchauffe |
| Mitrailleuses posées | **M2 Browning**, **DShK** | voir *Tourelles et mitrailleuses lourdes montables* |

Nouvelles munitions : .45 Colt, .500 S&W, .44 Magnum, .408 CheyTac, balles de plomb, carreaux, cartouches de taser,
cellules de rubis, ogives de Panzerfaust, missiles Stinger et Javelin. Le P90 a un chargeur transparent où l'on voit
les cartouches diminuer, la M249 une bande de munitions qui avance à chaque tir.

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

### Grenades et explosifs (12 grenades + C4, Claymore, mortier)
Clic droit maintenu : on dégoupille (le bras libre tire l'anneau, le minuteur tourne et s'affiche).
On relâche pour lancer ; si on la garde trop longtemps, elle explose dans la main. Les grenades rebondissent sur les murs
(réflexion V' = V - 2(V·N)N, puis perte d'énergie) et s'immobilisent au sol.

| Grenade | Effet |
|---|---|
| M67 (fragmentation) | 3,5 s, 45 éclats jusqu'à 12 blocs, brise les vitres |
| MK3A2 (offensive) | 3 s, onde de choc de 5 blocs, recul x3, sans éclats |
| Semtex | se colle aux blocs et aux créatures, explose 2,5 s après (bips + LED rouge) |
| Molotov | éclate à l'impact : flaque de feu de 4,5 blocs pendant 10 s |
| Sous-munitions | libère 6 mini-grenades qui rebondissent |
| M84 (flashbang) | écran blanc 3 s + persistance + acouphène si on regarde ; étourdit les monstres 5 s |
| M18 (fumigène) | dôme de fumée de 6 blocs pendant 20 s : les monstres perdent leur cible, pseudos masqués |
| Gaz toxique | nuage au ras du sol 15 s : poison + nausée + lenteur ; le **masque à gaz** protège |
| IEM | viseurs électroniques et lasers coupés 10 s (10 blocs), 15 dégâts aux golems de fer |
| Leurre | faux coups de feu et flashs pendant 12 s, attire les monstres à 30 blocs |
| Impulsion | aucun dégât : projette tout dans les airs ; le lanceur ne prend pas de dégâts de chute |
| Détection | se colle, impulsion sonar toutes les 2 s : les créatures brillent à travers les murs |

- **C4** : clic droit sur un bloc ou une créature pour le coller, puis le **détonateur** fait exploser tous tes C4 (128 blocs).
- **Mine Claymore** : à poser face à l'ennemi ; elle projette 40 éclats dans un cône quand une créature passe devant (jamais pour son poseur).
- **Mortier** : clic droit avec un obus = tir en cloche ; clic droit = angle +5° (45 à 85°) ; Shift + clic droit = tourner de 15°.
- Les explosifs posés explosent en chaîne s'ils sont pris dans une explosion. On les ramasse en les frappant.

### Tourelles et mitrailleuses lourdes montables (6)
On ne peut pas les tenir à la main : ce sont des entités fixes qu'on déploie.

| Tourelle | Munitions | Caractéristiques | Viseur fixe WWII |
|---|---|---|---|
| **Browning M2HB** (.50) | caisse de 100 balles .50 BMG | 550 coups/min, 14 PV, ignore 65 % de l'armure, brise les blocs fragiles, fort recul | hausse rabattable à anneau |
| **MG-42** (trépied Lafette) | caisse de 250 cartouches 7,92 mm | 1200 coups/min, 6,5 PV ; **surchauffe** après 80 balles sans relâcher (3,5 s bloquée, fumée, canon rouge) | grand **Kreiskorn** en toile d'araignée + mire arrière en anneau dans le même axe (en visée, les deux ne forment qu'un) |
| **Vickers / Maxim M1910** | bande en toile de 250 .303 British | 600 coups/min, 7,5 PV, refroidie à l'eau (jamais de surchauffe, vapeur blanche), **bouclier** qui arrête les tirs de face | cran de mire + guidon à lame |
| **Flakvierling 20 mm** | 4 chargeurs de 20 obus (80) | 4 canons en alternance, 400 coups/min, 16 PV + micro-explosion de 1,5 bloc, 360° et jusqu'à +75° | grand viseur circulaire à croisillons doubles |
| **Mk 19** | caisse de 32 grenades de 40 mm | 250 coups/min, grenades en cloche (60 blocs) qui explosent à l'impact (3,5 blocs) | hausse graduée en échelle |
| **DShK** (bonus) | caisse .50 BMG (100) | 600 coups/min, 13 PV | viseur antiaérien à anneaux |

- **Pose** : clic droit sur le dessus d'un bloc solide ; la tourelle est orientée comme ton regard et le trépied se déploie (0,8 s, bruit métallique).
- **Munitions obligatoires** : vide à la pose. Clic droit avec la caisse compatible : le capot s'ouvre et la bande est engagée (1,5 s).
  La caisse est sur le flanc gauche et la bande de cartouches en laiton défile pendant le tir ; caisse vide = plus de bande et clic à vide.
- **Monter** : clic droit (main nue ou autre objet) ; **Shift** pour descendre. Clic gauche = tir, **clic droit = visée** : l'œil se place
  dans l'axe des anneaux, zoom mécanique x1,3 au plus, la mire tremble pendant les longues rafales. Aucun viseur moderne ne s'y monte.
- **Angles** : trépieds 90° de chaque côté, de -25° à +45° ; Flakvierling 360°, jusqu'à +75°.
- Les deux bras du tireur tiennent les poignées arrière et suivent le recul (1re et 3e personne).
- **HUD** (en bas à droite) : caisse de munitions, compteur exact (`84 / 250`) et thermomètre de surchauffe.
- **Démontage** : Shift + clic droit avec la **clé à molette**, ou Shift + clic droit maintenu 1,5 s à main nue.
  Les munitions restantes sont gardées dans l'objet.
- Fabrication : tourelles et caisses à la table d'armement (lingots et blocs de fer, redstone, pistons...) ; clé à molette aussi à l'établi.

### Casques militaires 3D (12, de l'Antiquité à aujourd'hui)
Chaque casque a un **vrai modèle 3D voxel** sur la tête (reliefs, jugulaires, rebords, accessoires), aux teintes militaires mates.
On le met au **clic droit** : les deux bras le posent sur la tête (bruit de jugulaire). Fabrication : table d'armement, onglet **Casques**.

| Époque | Casque | Armure | Tir à la tête | Particularité |
|---|---|---|---|---|
| Antiquité / Moyen-Âge / 14-18 | Galea romain | 2 | -15 % | mêlée -10 % |
| | Heaume médiéval | 3 | -25 % | mêlée -20 %, vision périphérique assombrie |
| | Adrian M15 | 2,5 | -20 % | éclats de grenade -30 % |
| | Brodie Mk I | 2,5 | -20 % | éclats -35 % |
| Seconde Guerre mondiale | M1 américain | 3 | -35 % | explosions -15 % |
| | Stahlhelm M40 | 3,5 | -40 % | protège la nuque (bonus des coups dans le dos divisé par 2) |
| | SSh-40 soviétique | 3,5 | -38 % | très grande durabilité |
| Guerre froide | PASGT (kevlar, Woodland) | 4 | -50 % | 9 mm et .45 ACP : jamais de coup critique à la tête |
| | Altyn / K6-3 (titane) | 5 | -50 % / **-75 %** visière baissée | touche **N** : visière blindée (vision réduite, respiration étouffée) |
| Moderne | MICH 2000 / ACH | 4,5 | -60 % | pas de malus de vitesse de visée avec les lunettes de sniper |
| | Ops-Core FAST XP | 5 | -65 % | touche **N** : jumelles **GPNVG-18** (vision nocturne verte sans potion, pas des monstres amplifiés) |
| | Juggernaut | 6 | **-85 %** | survit à une .50 dans la tête, immunisé aux flashbangs et au gaz, -8 % de vitesse |

- Formule : dégâts = dégâts de base x multiplicateur tête x (1 - réduction du casque). Le casque s'use selon les dégâts absorbés ;
  à partir de -50 %, la balle ricoche (étincelles + bruit métallique).
- Touche **N** (modifiable) : le bras gauche monte sur le côté de la tête et abaisse la visière ou les jumelles.
- Les casques fermés affichent un contour à l'écran (heaume, fente de visière, tubes des jumelles, visière numérique), sans gêner le centre.
- Réparation à l'enclume (fer, acier, plaques de kevlar / titane, kevlar multicouche ou composite selon l'époque).

### Gilets pare-balles 3D (10) et plaques balistiques
Chaque gilet a un **vrai modèle 3D** sur le torse (poches, chargeurs, épaulières, cols, sangles), visible sur les joueurs,
les monstres et les supports d'armure. On le met au **clic droit** : les deux bras le serrent devant le torse (fermeture éclair, velcros).
Fabrication : table d'armement, onglet **Protections**.

| Époque | Gilet | Armure | Protection | Particularité |
|---|---|---|---|---|
| Guerres mondiales | Cuirasse 14-18 (Sappenpanzer) | 5 | éclats -40 %, pistolets -20 % | lourde : -10 % de vitesse |
| | Gilet pare-éclats M12 | 4 | éclats et grenades -55 %, balles -15 % | aucun malus |
| Guerre froide / 80-90 | Gilet PASGT (Woodland) | 6 | 9 mm / .45 / plombs -50 %, fusils -20 % | col montant |
| | Gilet lourd 6B3 / 6B5 | 7 | toutes les balles -40 % | rechargement +10 %, -5 % de vitesse |
| | Gilet SWAT / GIGN | 6,5 | pistolets, PM, fusils à pompe -60 %, explosions -30 % | protège-aine, « SWAT » dans le dos |
| Moderne | Porte-plaques JPC 2.0 | 6 → **8** | 2 plaques | visée +15 %, rechargement +15 % |
| | IOTV / Osprey Mk4 | 7,5 → **9,5** | balles -45 %, explosions -50 % | -4 % de vitesse |
| | Porte-plaques AVS | 8 → **10** | 2 plaques | saignement réduit de moitié, régénération +25 % hors combat |
| Prototypes lourds | Fort Defender-2 / Zabralo | 9 → **11** | balles -65 % (même .50 BMG) | -12 % de vitesse, pas de long sprint |
| | Combinaison EOD « Juggernaut » | 10 | explosions -85 %, balles -70 % | aucun recul ; -18 % de vitesse, sauter donne faim |

**Plaques balistiques** (porte-plaques JPC, IOTV, AVS, Fort Defender) : clic droit en tenant une plaque, le bras gauche l'enfonce dans
la poche avant puis arrière (si les deux sont pleines, elle remplace la plus abîmée). Une balle au torse touche la plaque avant
ou arrière selon d'où elle vient : la plaque absorbe d'abord, puis le gilet réduit le reste.

| Plaque | Fabrication | Contre pistolets / PM | Contre fusils | Contre snipers / .50 | Points |
|---|---|---|---|---|---|
| Acier (IIIA) | 2 plaques d'acier + 1 cuir | arrête tout | -30 % | -10 % | 40 (lourde : -3 % de vitesse) |
| Céramique (IV) | 2 composites céramique-titane + 2 plaques de kevlar | arrête tout | arrête tout | -50 % | 60 (se fissure vite) |
| Titane-netherite (IV+) | 2 plaques de titane + 1 netherite + 2 kevlars multicouches | arrête tout | arrête tout | -90 % | 120 |

Une plaque à 0 point se brise (bruit de céramique, message). Une jauge à côté de la barre d'armure montre l'état des plaques
avant / arrière (vert intacte, orange fissurée, rouge détruite).
Nouveaux matériaux : **tissu Cordura** (4 ficelles + 2 cuirs + 1 colorant vert, marron ou noir) et **sangle MOLLE**
(2 cuirs + 1 lingot de fer ou d'acier → 4).

### Pantalons militaires et rangers 3D (8 + 8)
Peu d'armure (+1 à +3,5) mais des **effets passifs** de mobilité et d'endurance, d'autant plus forts que la pièce est récente.
Vrai modèle 3D sur les jambes et les pieds (poches cargo, genouillères, holster de cuisse, bandes molletières, semelles crantées, lacets...).
Clic droit pour les mettre (les bras s'abaissent sur les côtés, bruit de tissu / de lacets). Les rangers ont des pas de semelles lourdes.
Fabrication : table d'armement, onglet **Protections**. Toutes ces pièces sont dans le livre des recettes (`@pantalon`, `@rangers`).

| Époque | Pantalon | Effets | Rangers | Effets |
|---|---|---|---|---|
| Guerres mondiales | Pantalon 14-18 (molletières) | immunité au gel | Brodequins cloutés | pas ralenti par le sable des âmes, la boue, la neige poudreuse |
| | Parachutiste M42 | faim -10 %, rechargement +10 % | « Corcoran » | chute -25 %, faim -10 % |
| Guerre froide | Treillis BDU Woodland | vitesse +8 %, accroupi et rampant +25 % | M65 / BMJA | vitesse +10 %, chute -30 %, faim -15 % |
| | SWAT / GIGN | accroupi et rampant +35 %, pistolets dégainés 30 % plus vite, balles de pistolet aux jambes -25 % | Jungle « Panama » | +15 % en forêt / jungle / marais, nage +25 %, faim -20 % |
| Moderne | Crye G3 | vitesse +15 %, rampant +50 %, accroupi +40 %, faim -25 % | Désert « Lowa Zephyr » | course +18 %, faim -40 %, chute -40 % |
| | « Infiltrator » | vitesse +18 %, rampant et accroupi +65 %, détection -50 %, faim -30 % | Furtives « Salomon Forces » | pas silencieux (-80 %, pas de capteurs sculk), vitesse +20 %, rampant +30 %, chute -50 % |
| Prototypes | Balistique « Ratnik » | aucun recul, rampant +30 %, faim -20 %, balles et explosions aux jambes -45 % | Anti-mines EOD | mines, Claymores, explosions au sol -75 %, aucun recul |
| | Exosquelette « TALOS » | course +25 %, rampant +80 %, faim -50 %, **montée automatique** d'un bloc | « Apex-Jump » | course +25 %, saut +50 %, **aucun dégât de chute** (jusqu'à 25 blocs), faim -50 % |

- **Ramper** : touche **X** (ou double appui sur s'accroupir) pour s'allonger ; on passe sous les dalles, et les pantalons / rangers accélèrent la reptation.
- **Faim** : les réductions du pantalon et des rangers s'additionnent, jusqu'à **-75 %** au maximum.
- **Bonus d'ensemble** : casque + gilet + pantalon + rangers de la même époque = régénération +10 %.

### Nouveaux minerais et matériaux
- **Titane** : minerai (et minerai des abîmes) entre Y = -64 et Y = 16, aussi rare que l'or, pioche en fer minimum.
  Titane brut → four / haut-fourneau → **lingot de titane** ; 4 lingots → 2 **plaques de titane**.
- **Composite céramique-titane** : plaque de titane + terre cuite + poudre de blaze + quartz → préforme → haut-fourneau.
- **Acier** : 1 lingot de fer + 2 charbons → **fer carburé** → haut-fourneau (ou four, plus lent) → **lingot d'acier** ;
  4 lingots → 2 **plaques d'acier**.
- **Kevlar** : ficelle + laine + boule de slime + charbon → 2 **fibres d'aramide** ; 4 fibres + 1 lingot d'acier (ou de fer)
  → **plaque de kevlar** ; 2 plaques de kevlar + 1 plaque de titane + 1 cuir → **kevlar multicouche**.

### Livre des recettes et encyclopédie (façon Not Enough Items)
- **Manuel d'armement & d'ingénierie militaire** : offert à la première connexion (désactivable :
  `config/modeminecraft-common.toml` → `giveManualOnFirstJoin`), ou 1 livre + 1 lingot de fer (ou 1 poudre à canon).
  Clic droit : les mains ouvrent le livre, avec une page d'accueil par catégories (armes, munitions, tourelles, casques, gilets...).
- **Panneau latéral** à droite de l'inventaire, de l'établi, des fours, de la table d'armement et de la table de modification
  (touche **O** pour l'afficher / le masquer) : tous les objets du mod, page par page, avec une **recherche** par nom,
  par `@catégorie` (`@sniper`, `@wwii`, `@kevlar`, `@casque`, `@gilet`, `@tourelle`...) ou par calibre (`9mm`, `556`...).
- Sur n'importe quel objet survolé (panneau ou inventaire) : **R** ou clic gauche = sa recette, **U** ou clic droit = ses utilisations,
  **Retour arrière** = page précédente.
- Onglets par station : **Établi 3x3**, **Établi d'armurier** (ingrédients + quantités + modèle 3D de l'objet), **Fonderie**
  (four / haut-fourneau, durée et XP), **Génération naturelle** (couches du minerai, meilleure couche, outil) et
  **Fiche technique** (dégâts en PV et en cœurs, multiplicateur tête, cadence, calibre, chargeur, pénétration d'armure ;
  époque, armure et protections des casques / gilets ; munitions, angles et surchauffe des tourelles).
- Bouton **[+]** (ouvert depuis un établi, un four ou la table d'armement) : place les ingrédients ; ceux qui manquent sont en rouge.
- **Mode triche** (créatif ou opérateur) : `[Triche]` dans le panneau, puis clic gauche = une pile, clic droit = un objet.
- Les recettes sont lues en direct dans le jeu : une recette changée par un datapack est à jour dans le livre.
- **JEI** : si Just Enough Items est installé, nos recettes de la table d'armement et les infos du titane y apparaissent aussi
  (notre panneau est alors masqué par défaut, touche O pour l'afficher).

### Lampes tactiques (rail latéral, touche L)
Les lampes se montent sur le **rail latéral** (flanc droit du garde-main, avec un contacteur déporté relié par un câble).
**L** allume / éteint (le bras gauche va presser l'interrupteur, avec un clic sec) ; **Maj + L** change de mode :
éteinte → continu → stroboscope → infrarouge (selon le modèle). Tant qu'on tient une arme, L n'ouvre pas l'écran des progrès.

| Lampe | Tier | Portée | Particularités | Recette (table d'armement) |
|---|---|---|---|---|
| Lampe de fortune scotchée | 1 | 15 blocs, lumière 12 jaune | scintille, -5 % de vitesse de visée | 2 fer + torche de redstone + ficelle (ou boule de slime) |
| SureFire M600 Scout | 2 | 28 blocs, lumière 15 blanche | aucun malus | 2 lingots d'acier + poudre lumineuse + redstone + vitre |
| Streamlight TLR-1 HL | 3 | 38 blocs, cône large | **stroboscope** (10 flashs/s) qui éblouit | 2 plaques d'acier + titane + bloc lumineux + 2 redstone |
| AN/PEQ-15 | 3 | 30 blocs | + **laser vert** (T) ; allumée : précision au jugé +25 %, visée +10 % | 2 plaques de titane + diamant + poudre lumineuse + 2 redstone + colorant vert |
| MAWL-C1+ IR | 4 | 45 blocs | stroboscope + **infrarouge** | 3 plaques de titane + netherite + améthyste + 2 quartz + 2 redstone |

- **Pile CR123A** (établi : cuivre ou fer + redstone + charbon → 2 piles) : 10 minutes d'éclairage. La charge est affichée
  par une petite jauge verte / jaune / rouge sous l'icône de l'arme et dans le HUD. Pour changer la pile : clic droit avec
  la pile (elle va dans l'arme tenue dans l'autre main, sinon dans l'arme à lampe la plus déchargée de la barre), ou
  dans l'inventaire : prendre la pile et faire un clic droit sur l'arme.
- **Éclairage réel** : des blocs de lumière invisibles (bloc `minecraft:light`, sans collision) sont posés devant la
  surface éclairée et tous les 9 blocs le long du faisceau, puis retirés dès qu'il bouge. Ils ne remplacent que de l'air
  (ou de l'eau), et leur liste est sauvegardée : même après un arrêt brutal, ils sont retirés au redémarrage.
- **Faisceau volumétrique** visible par tous les joueurs : cône lumineux qui suit le canon, tache de lumière sur les
  murs, poussière éclairée.
- **Stroboscope** : toute créature ou tout joueur à moins de 15 blocs, dans un cône de 15° et qui regarde la lampe, est
  ébloui (monstres : cécité, ralentissement et tirs imprécis ; joueurs : halo blanc pulsant).
- **Infrarouge** (MAWL) : aucune lumière visible, les autres ne voient pas le faisceau ; il n'apparaît qu'avec des
  jumelles de vision nocturne baissées (casque Ops-Core FAST XP), et fait briller les créatures invisibles et les Endermen.
- Une grenade IEM coupe aussi les lampes.

### Villes, factions et bots (monde vivant)
Des **villes générées** apparaissent selon le biome (un centre de ville par région de 640 blocs, sur terrain assez plat) :

| Biome | Ville (proportion) | Faction | Attitude |
|---|---|---|---|
| Plaine | **Métropole** 15 x 15 chunks : gratte-ciel de 35 à 75 blocs (hall en marbre, portique, open-spaces, salle de réunion, penthouse, héliport), Central Park, commissariat (cellules, armurerie blindée), commerces, station-service, égouts | Police (+ SWAT) | sommation |
| Désert | **Cité sous dictature** : riads à patio et toits-terrasses reliés (parkour), avenue à toiles d'ombrage, Grand Souk, palais fortifié (tourelles, salle forte rouge), portraits du Dictateur | Milice | hostile à vue |
| Forêt / taïga | **Bastion rebelle** (50 %) : cabanes dans les arbres à 12 blocs, ponts suspendus, caches sous trappes ; **base secrète** (50 %) : double grillage, miradors MG-42, hangars, radar, Flakvierling, bunker sur 3 niveaux | Rebelles / Garnison d'Élite | neutres / hostile |
| Neige | **Cité soviétique** (70 %) : khrouchtchevkas, gare enneigée, usine sidérurgique, statue ; **station polaire** (15 %) : dômes géodésiques, tunnels, labos, cultures UV ; **avant-poste minier** (15 %) : galeries de titane | Soviétiques / mercenaires arctiques / sécurité | hostile dedans / hostile / sommation |
| Jungle / marais | **Villa de cartel** (80 %) : hacienda, billard, coffre derrière un tableau, piscine, piste clandestine ; **pilotis** (20 %) : cabanes en tôle, pontons illuminés | Cartel / gangs | hostile / hostile si arme sortie |
| Badlands | **Ville frontière** (50 %) : saloon, banque à chambre forte, shérif, gare, château d'eau ; **raffinerie** (50 %) : cuves, chevalets de pompage, pipelines, torchère, Mk 19 | Shérifs / milice privée | sommation / hostile |

- **Bots** : corps de joueur, 78 skins 64x64 sur mesure (aucun Steve / Alex), casques / gilets / pantalons / rangers du mod,
  armes à deux mains, recul, rechargement, accroupis à couvert, snipers allongés sur les toits, servants de tourelles.
- **IA** : sommation de 3,5 s (icône jaune + radio) si une arme est visible (en main, dans le dos ou à la hanche) ; on la retire de la barre d'action = ils se calment, sinon toute
  la faction devient hostile (5 min). Vision en cône de 110° (45 blocs, 90 pour les snipers) et ouïe à 10 blocs ;
  coups de feu entendus à 40 blocs (16 avec silencieux) ; coffres pillés sous leurs yeux ; prise de couverture derrière
  un muret, rafales courtes, grenades ; patrouilles en binôme ; **renforts SWAT** en cas de fusillade avec la police.
- **Faucons Blancs** : escouades nomades de 15 (commandant, Chef des Troupes de Choc, 5 éclaireurs au katana qui
  « dashent » sous les tirs, 4 lanciers, 2 assassins aux tomahawks, 2 brutes), cape blanche à l'emblème, cor de charge,
  hostiles à tous. Le **Chef** (80 PV, x1,15) porte la **Pourfendeuse de Dragons** dans le dos et la dégaine à deux mains
  (26 dégâts, balayage 160° / 4 blocs, brise les boucliers, impossible à enchanter) ; il la lâche avec une carte rouge
  et l'Insigne du Faucon Blanc.
- **Salles fortes** : portes blindées à carte d'accès bleue / rouge / noire (fermeture auto après 5 s).
- **Marchands clandestins** (égouts, souk, saloon, pontons) : achètent armes et butin, vendent munitions, soins, piles,
  cartes d'accès (la carte noire contre un Insigne du Faucon Blanc).
- **Soins** (un simple clic droit, effet immédiat, cœurs et message ; rien n'est gâché en pleine santé) : bandage
  (+4 PV, arrête le saignement), trousse de premiers secours (+8 PV puis +4 PV, retire les effets négatifs),
  seringue d'adrénaline (+6 PV, vitesse, force, résistance).
- **Largages aériens** (≈ toutes les 25 min) : caisse sous parachute, coffre de butin rare et fumée ; les Faucons
  Blancs et la faction locale s'affrontent pour la récupérer.
- **Véhicules conduisibles** (table d'armement) : quad, voiture de police (L : sirène et gyrophares), jeep militaire,
  camion blindé (tourelle M2 sur le toit). Z/S/Q/D pour conduire, la direction dépend de la vitesse ; ils montent les
  marches, prennent des dégâts (blindage) et explosent quand ils sont détruits. Voitures garées dans les villes.
- **Convois de patrouille** : camion blindé + jeep (ou deux voitures de police sirène hurlante) conduits par des bots,
  qui traversent la région en évitant murs, ravins et eau ; s'ils repèrent un ennemi, l'équipage descend combattre.
- **Contrats de chasse** : les donneurs de contrats (commissariat, shérif, souk, QG rebelle, place soviétique,
  ponton) confient une cible nommée (★) cachée à 200-400 blocs avec ses gardes ; la **boussole de traque** pointe vers
  elle ; une fois abattue, on revient toucher la prime (émeraudes + bonus).
- **Décors fonctionnels** : feux tricolores qui changent de couleur (axes décalés aux carrefours), bouches d'incendie qui
  projettent un geyser une fois détruites (même par balle), extincteurs qui explosent en fumée, portiques détecteurs de
  métaux qui bipent si l'on est armé.
- **Maxim M1910** sur affût Sokolov (tourelle refroidie à l'eau, bande de 7,62x54R) sur les tours du palais du désert.
- Œufs d'apparition de chaque faction (et d'une escouade complète) dans l'onglet créatif.

### Faucons Blancs, nuit et Chefs de Faction

- **Mode Berserk** du Chef des Troupes de Choc (sous 30 % de ses PV) : heaume fissuré aux yeux rouges lumineux,
  fumée et étincelles, cri de guerre (40 blocs), insensible au recul, +40 % de vitesse, coups 25 % plus rapprochés.
- **La Pourfendeuse de Dragons entre vos mains** : rangée dans la barre d'action, elle se porte en diagonale dans le dos ;
  la sélectionner joue un dégainage de 0,6 s par-dessus l'épaule. -15 % de vitesse en main, 26 dégâts, ignore 60 % de
  l'armure, brise les boucliers (5 s) et arrache 25 % de durabilité aux plaques balistiques.
- **La nuit** (13 000 à 23 000) : lampadaires, néons et projecteurs des villes s'allument tout seuls (l'éclairage
  intérieur reste allumé le jour) ; les patrouilles allument la lampe tactique de leur arme (vrais faisceaux, vrai
  éclairage) et repèrent deux fois moins loin dans le noir ; la police impose un **couvre-feu** (sommation si l'on
  sprinte à moins de 12 blocs d'un agent ou si l'on entre aux abords du commissariat).
- **Transformateurs haute tension** dans chaque quartier : C4, grenade ou 6 balles et c'est le court-circuit — panne de
  secteur de 60 blocs pendant 5 minutes (lumières éteintes, serrures électroniques des portes blindées hors service).
- **Embuscades nocturnes** des Faucons Blancs : postés en silence derrière rochers, arbres et murs sur la route du joueur
  ou d'un convoi, sans bruit de pas ni cor de guerre avant le premier coup porté.
- **La chasse aux 6 Chefs de Faction** (100 PV, barre de boss, uniques par monde) : Commissaire Divisionnaire SWAT
  (Glock 18C « Prestige » + bouclier balistique lourd), Général Suprême de la Milice (AK-47 « Souverain »),
  Commandant des Forces Spéciales (VSS Vintorez « Fantôme » : aucun bruit, lunette thermique), Maréchal de l'Est
  (PKM « Hiver » : 150 balles perforantes), El Patrón (Desert Eagle « Cartel » + machette en or) et le Chef des Troupes
  de Choc (Pourfendeuse de Dragons). Progression « Chefs vaincus : n / 6 ».
- **Râtelier à trophées** (4 planches de chêne noir, 2 lingots d'acier, 1 pépite d'or) : expose n'importe quelle arme en
  relief ; un trophée de Chef reçoit une plaque nominative dorée et un éclairage.

### Cases « gilet » et « sac à dos » de l'inventaire

- Deux cases en plus à droite du personnage (aussi dans l'inventaire créatif) : un **gilet pare-balles** porté
  par-dessus un plastron (diamant, netherite...) avec toute sa protection et ses plaques, et le **sac à dos tactique**
  (18 cases, 8 nylon balistique + 1 sangle + 1 coffre), visible dans le dos. **Touche K** pour l'ouvrir quand il est
  porté (ou clic droit en main). Ils tombent à la mort, sauf avec keepInventory.
- Balles en **vrai 3D** (douille en laiton, ogive en cuivre, plombs pour la chevrotine) qui sortent du canon et filent
  droit vers le réticule ; fumigènes beaucoup plus denses (brouillard gris quand on est dedans) ; **bruit de moteur**
  des véhicules (plus aigu en accélérant).

### Drones de combat et guerre électronique

- **Télécommande tactique** (2 lingots d'acier, 2 redstone, 1 vitre, 1 pile CR123A) : Maj + clic droit sur un drone posé
  pour l'associer (Maj + clic droit dans le vide pour la dissocier), clic droit pour le piloter en **caméra FPV**
  (Z/Q/S/D, Espace / Ctrl, souris ; clic gauche : action, **N : explosion du drone kamikaze**, clic droit : zoom x2,5 ou
  thermique ; Maj ou R : retour automatique). Le pilote reste immobile et vulnérable. Les drones ne font aucun bruit
  quand on les frappe.
- **Retour automatique (RTH)** : prise d'altitude, ligne droite, atterrissage aux pieds du pilote — aussi en cas de perte
  de signal. Batterie de 5 min (10 avec la LiPo-Titan), rechargée avec une pile CR123A.
- **5 drones** : micro-drone *Black Hornet* (marque les ennemis à travers les murs), kamikaze FPV *Frelon* (n'importe
  quel explosif du mod), bombardier *Mavic Tactique* (2 grenades), *Wasp-MG* (60 balles, 600 coups/min, 6 PV, laser),
  *Reaper-X* (4 mini-roquettes guidées de 24 PV, optique thermique).
- **Modules** sur la table de modification (3 emplacements) : charge / grenades / module cargo (soute de 9 cases),
  hélices silencieuses en carbone (-70 % de bruit), batterie haute capacité.
- **Guerre électronique** : les patrouilles ennemies entendent les drones (22 blocs, 7 avec hélices silencieuses) et les
  abattent ; des **opérateurs de drones** (bases secrètes, cartels, raffineries, escouades SWAT) lancent un micro-drone
  qui vous marque pour leurs snipers ou un drone kamikaze qui fonce sur vous ; **fusil brouilleur EMP** (80 blocs : le
  drone touché atterrit, désactivé 15 s) ; **tours de brouillage** sur les toits (palais, bases, stations polaires) :
  neige analogique dans un rayon de 50 blocs, perte de contrôle à moins de 15.
- **Équilibrage balistique** : PM 800-1 100 coups/min et 4,5-6 PV ; 5,56 750-850 coups/min et 7-8 PV ; 7,62 9,5-12 PV
  mais plus lents (AK-47 : 600) ; snipers à verrou 35-50 coups/min, semi-automatiques 120-180, 18-38 PV ;
  mitrailleuses 650-850 coups/min, 8-14 PV, gros recul debout.

### Correctifs et réglages de la mise à jour 0.3

- **Nuit réaliste** : loin de toute lumière, la nuit est presque noire (même avec la luminosité au maximum) ; les zones
  éclairées par des blocs (torches, lampadaires, lampes tactiques) restent claires. Lampes tactiques, jumelles de vision
  nocturne, illuminateur infrarouge et lunettes thermiques deviennent indispensables.
- **Bruits de pas** : toujours ceux de Minecraft selon le bloc (plus de bruit de semelle ajouté) ; seules les rangers
  furtives les atténuent.
- **Drones** : son d'hélices en boucle parfaite (fondu, hauteur selon la vitesse) ; liaison stable dès le décollage
  (1 s de grâce, propriétaire connu du serveur et du client, plus de tours de brouillage « fantômes »).
- **Mortier en deux temps** : clic droit avec un obus = chargement (bruit de glissement), puis clic droit = tir.
- **Bouclier balistique + arme de poing** (pistolet ou revolver en main, bouclier dans l'autre main) : clic droit maintenu
  = bouclier levé, clic gauche = tir à une main sur le côté (dispersion x1,6) ; le bouclier s'abaisse le temps d'un
  rechargement puis remonte.
- **Bases militaires secrètes terminées** : hangar fermé (voûte pleine, mur du fond, porte de service, armurerie,
  éclairage), poste radar avec consoles, bunker à cloisons (commandement, radio, dortoir, armurerie, générateurs, salle
  forte) ; butin **rare** dédié (armes haut de gamme, plaques céramique / titane, lunettes thermiques, munitions lourdes,
  pièces de drones).
- **8 onglets créatifs** : armes à feu, armes blanches et boucliers, munitions / grenades / explosifs, accessoires,
  équipements 3D, drones et véhicules, soins / outils / objets spéciaux, blocs et décors.
- **Coffre bonus** : couteau de combat, Glock 17 et 32 balles de 9 mm en plus des objets de Minecraft.

### Routes, campements, police et ville vivante (mise à jour 0.3)

- **Mégapoles** : la métropole couvre désormais **31 x 31 chunks** (~500 blocs), les cités du désert et soviétique
  25 x 25 ; la ville est découpée en districts de 11 chunks (une grille commune : Minecraft ne construit les pièces d'une
  structure qu'à 8 chunks de son départ). Quartiers distincts : centre d'affaires (gratte-ciel à écrans géants),
  quartiers commerçants, résidentiels (immeubles d'habitation), places, parcs, zone industrielle (entrepôts, usines),
  avenues de 16 blocs (4 voies) et boulevard circulaire. `/locate structure modeminecraft:city_plains` fonctionne.
- **Routes** : toutes les villes d'un même biome sont reliées (asphalte à ligne blanche en plaine, grès pavé au désert,
  route gravillonnée enneigée...). Le tracé ondule, part dans l'axe de l'avenue centrale, épouse le relief (dalles dans
  les pentes, tranchées) ; au-dessus des ravins et des rivières : **pont à piliers jusqu'au sol**, jamais de route en l'air.
- **Campements abandonnés** (forêts, plaines, collines, déserts, montagnes) : aucun habitant, feu de camp éteint ou
  fumant, rondins, 2 à 3 **tentes 3D** (toile kaki sur arceaux, on entre dedans) avec **sac de couchage** et coffre de
  survie. Sac de couchage : clic droit = dormir sans changer de point de réapparition, **Maj + clic droit** = dormir et
  en faire son point de réapparition. **Tente de camping** pliée : clic droit au sol pour la monter.
- **Verre plein** : toutes les vitres des bâtiments des villes sont des blocs de verre pleins.
- **Voiture de police** : touche **H** (ou L) = gyrophares et sirène, entendue à 80 blocs ; civils et circulation s'écartent.
- **Pied-de-biche** (3 fer + 1 colorant rouge) : clic droit **maintenu 2,5 s** (levier, grincements) pour forcer les
  **portes verrouillées** (arrière-boutiques, bureaux, banque), les **casiers de police**, les **caisses enregistreuses**
  et les portes de cellule (depuis le couloir). Un policier qui vous voit faire : **flagrant délit**, la police attaque.
- **Commissariat central** : accueil, poste de garde vitré (moniteurs de surveillance, pupitre d'ouverture des cellules),
  **3 cellules de 5 x 5 x 4** (béton, porte coulissante à barreaux à voyant rouge / vert, lit, sanitaire en inox, banc,
  fenêtre haute à barreaux, plafond lumineux), un étage entier de **vestiaires : 160 casiers** verrouillés (gilets,
  casques, munitions, fusils à pompe, PM, cartes d'accès...), bureaux fermés à clé, armurerie au sous-sol.
- **Police** : elle ne réagit qu'à une arme **en main** (pas dans le dos). **Tué par la police** : vous êtes arrêté,
  tout votre équipement est confisqué et vous réapparaissez dans une cellule du commissariat ; la porte s'ouvre après
  **2 minutes** (minuterie au-dessus de la porte), ou plus tôt grâce au pupitre du poste de garde / un complice.
- **Civils** : flânent sur les trottoirs, traversent aux passages piétons, s'assoient sur les bancs, discutent par petits
  groupes, lisent le journal au kiosque, rentrent chez eux la nuit. Coup de feu, explosion ou sirène : ils crient et
  courent se réfugier à l'intérieur ; tenus en joue : **les mains en l'air**.
- **Commerçants** (cafés, supermarchés, boutiques, stations-service) : vendent rations, boissons, bandages, piles et
  outils contre des **liasses de billets**. **Braquage** : pointez une arme sur le commerçant, il lève les mains et ouvre
  le tiroir-caisse (billets, or, émeraudes) — ou forcez la caisse au pied-de-biche. L'alarme prévient la police
  (recherché) : les voitures de patrouille vous prennent en chasse puis déploient leurs agents.
- **Circulation** : berlines et taxis jaunes conduits par des civils, voitures de patrouille ; conduite à droite, choix
  aux carrefours, **arrêt aux feux rouges**, klaxon si on bloque la route. **Bus urbain** sur le boulevard circulaire :
  10 s d'arrêt à chaque **abribus**, on monte à bord gratuitement (clic droit).
- **Décors animés** : enseignes **néon clignotantes**, **écrans géants** publicitaires, **bouches d'égout fumantes**,
  **pigeons** (ou mouettes) qui s'envolent en nuée à l'approche, **journaux** emportés par le vent, **fontaines** à
  jets d'eau, **climatiseurs** aux ventilateurs qui tournent, **horloges murales** à l'heure du monde.
- **Paysage sonore** : rumeur de la circulation, vent entre les gratte-ciel, brouhaha du souk, musique des cafés et
  supermarchés, piano du saloon, annonces par haut-parleur (bases, dictature), grésillements radio des policiers, chiens
  au loin, roucoulements, grillons la nuit, pluie qui crépite sur les auvents et les toits en tôle.
- **Micro-événements** : contrôle d'identité, voleur poursuivi par la police, ouvriers de chantier (casques jaunes,
  gilets fluo, cônes, échafaudage), agent de propreté, **musicien de rue** et son public, artiste qui peint une
  **fresque murale** bloc par bloc.
- Correctif : un tableau mal créé pouvait **bloquer la génération** des villas de cartel (gel du jeu).
- **Mort** : la police et le SWAT oublient leur hostilité et les voitures de patrouille abandonnent la poursuite.
- **Performances** : ~3 fois moins d'habitants dans les métropoles, IA au ralenti loin des joueurs, détails 3D
  (équipement, armes, capes) masqués au-delà de 40 blocs, lampes des bots limitées à 32 blocs, routes et plan des
  villes calculés hors du fil principal et seulement quand c'est utile : de ~45 ms à ~6 ms par tick en métropole.

### Gameplay et progression
- **Niveaux des armes (1 à 5)** : chaque arme garde ses statistiques (éliminations, tirs à la tête, tirs, ratio)
  et gagne de l'expérience : +10 par élimination, +5 si tir à la tête, +10 pour un gros monstre (50 PV ou plus).
  Paliers : 100 / 300 / 600 / 1000 XP. Chaque niveau donne -4 % de recul, -3 % de dispersion et -3 % de temps de rechargement.
- **Skins** : 10 skins (Défaut, Désert, Forêt, Arctique, Or, Rubis, Urbain, Tigre, **Carbone** débloqué au niveau 3,
  **Diamant** débloqué au niveau 5), à choisir dans la table de modification.
- **Menu rapide des accessoires (touche Y)** : on maintient Y, une roue montre les accessoires compatibles de l'inventaire
  (en vert) et ceux déjà montés (en rouge, pour les retirer) ; on pointe avec la souris et on relâche Y.
- **Monstres armés** : 25 % des pillards (AK, fusils à pompe, PM), 4 % des squelettes (fusils à verrou)
  et 4 % des zombies (pistolets) ont une arme à feu (deux fois plus en difficile). Ils tirent par rafales, rechargent,
  et lâchent souvent des munitions (50 %), parfois leur arme (8 %).
- **Butin** : armes, munitions, accessoires et grenades dans les coffres des donjons, mines, avant-postes de pillards,
  forgerons de village, manoirs, bastions, cités antiques, trésors enfouis, etc. (les armes rares dans les coffres rares).
- **Râtelier d'armes** : expose 3 armes contre un mur (clic droit avec une arme pour la poser à la hauteur visée,
  main vide pour la reprendre). Recette : 3 colonnes planche / bâton / planche.
- **Cible de tir** : une balle donne un score de 0 à 10 selon la distance au centre, affiché avec la distance de tir et le
  total de la série ; elle émet un signal de redstone égal au score. Recette : laine blanche, colorant rouge et cible.

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
| Rail latéral (14) | lasers rouge, vert, bleu, violet, jaune, orange, cyan, blanc, **arc-en-ciel** (fusils d'assaut uniquement) ; **5 lampes tactiques** (voir plus bas) | laser visible, dispersion sans viser réduite, éclairage |
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

### Textures détaillées (style Minecraft, rondes mais pas lisses)
Tous les modèles 3D (armes, accessoires, tourelles, grenades, casques, gilets, pantalons, rangers) passent par
`tools/voxel_bake.py`, qui « cuit » une vraie texture pour chaque objet au lieu d'une simple case de couleur :
- **arrondi en marches** : les pièces longues ont leurs arêtes chanfreinées (section octogonale en escalier) ;
- **3 pixels de texture par pixel de modèle**, chaque face ayant sa propre zone dans la texture de l'objet ;
- **matières peintes pixel par pixel** : métal brossé et rayé, bois veiné, polymère grainé, tissu tissé, cuir,
  peinture écaillée, verre ; **arêtes en relief** (haut éclairé, bas dans l'ombre) ; **camouflages continus** d'une
  face à l'autre ;
- **icônes d'inventaire 32×32** dessinées depuis ces textures (lumière en haut à gauche, liseré entre les pièces, contour).

Les objets en 2D (armes blanches, munitions, matériaux, outils et armure en rubis, blocs) passent en 32×32 avec
`tools/enhance_icons.py` (agrandissement Scale2x qui arrondit les diagonales, relief, contour sélectif, grain).
Ordre de génération complet :

```bash
python3 tools/generate_gun_models.py
python3 tools/generate_turrets.py
python3 tools/generate_explosives.py
python3 tools/generate_helmets.py
python3 tools/enhance_icons.py
```

Les textures sont rangées au plus serré (largeur en puissance de 2, hauteur multiple de 16) : le tout tient dans un atlas
de blocs de 4096×4096, comme un pack de textures classique.

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
tools/generate_melee.py         # icônes des 20 armes blanches
tools/generate_explosives.py    # modèles 3D des grenades et explosifs
tools/generate_helmets.py       # casques 3D, palette militaire, contours d'écran, textures titane / acier / kevlar
tools/generate_turrets.py       # tourelles en pièces (trépied, arme, capot, bande, viseurs WWII) + turrets.json
tools/voxel_bake.py             # textures détaillées des modèles 3D (arrondi en marches, matières, icônes 32x32)
tools/enhance_icons.py          # icônes 2D et blocs en 32x32 (Scale2x, relief, contour, grain)
tools/generate_skins.py         # skins 64x64 des bots (15 factions + chevalier) et cape des Faucons Blancs
tools/generate_city_items.py    # Pourfendeuse de Dragons (3D), cartes d'accès, insigne, soins, œufs
tools/generate_city_data.py     # structures des villes, biomes, tables de butin, portes blindées
tools/generate_vehicles.py      # véhicules 3D (carrosserie + roue) : quad, police, jeep, camion blindé
tools/generate_drones.py        # drones 3D (corps + hélice), télécommande, icônes des modules
tools/generate_night_blocks.py  # éclairage de ville (allumé / éteint), transformateur haute tension
src/.../world/city/             # génération procédurale des villes (plan, parcelles, décors, bots)
src/.../entity/bot/             # bots, factions, IA, Faucons Blancs, marché noir
```

### Ajouter une nouvelle arme magique
1. Dans `ModItems.java`, copier par exemple `VENOM_DAGGER` et changer l'effet dans le lambda `(target, attacker) -> ...`.
2. Ajouter le modèle `assets/modeminecraft/models/item/<nom>.json` et la texture `textures/item/<nom>.png`.
3. Ajouter le nom et l'infobulle dans `lang/fr_fr.json` et `lang/en_us.json`.
4. Ajouter une recette dans `data/modeminecraft/recipe/` et l'item au tag `data/minecraft/tags/item/swords.json`.

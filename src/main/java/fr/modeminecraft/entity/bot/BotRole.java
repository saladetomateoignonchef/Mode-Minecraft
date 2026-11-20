package fr.modeminecraft.entity.bot;

/** Rôle d'un bot dans sa faction (équipement et comportement). */
public enum BotRole {
    SOLDIER,      // fusilier / policier / garde : patrouille en binôme
    SNIPER,       // allongé sur les toits, portée 90 blocs
    GUNNER,       // servant d'une tourelle fixe
    OFFICER,      // chef de patrouille (meilleur équipement)
    CIVILIAN,     // habitant pacifique (fuit les coups de feu)
    TRADER,       // marchand clandestin
    CONTRACTOR,   // donneur de contrats de chasse
    // Faucons Blancs (100 % corps à corps)
    COMMANDER, CHIEF, SCOUT, LANCER, ASSASSIN, BRUTE;

    public boolean falcon() {
        return ordinal() >= COMMANDER.ordinal();
    }
}

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
    COMMANDER, CHIEF, SCOUT, LANCER, ASSASSIN, BRUTE,
    /** Chef unique d'une faction (100 PV, arme légendaire). */
    BOSS,
    /** Opérateur de drones : déploie un micro-drone de reconnaissance ou un drone kamikaze dès que l'alarme sonne. */
    DRONE_OPERATOR,
    /** Scénario « Course à l'arme nucléaire » : officier recruteur, armurier, cuisinier, infirmier, scientifique transfuge. */
    RECRUITER, ARMORER, COOK, MEDIC, SCIENTIST,
    /** Train des villes du Mesa : conducteur de la locomotive, guichetier de la gare, contrôleur à matraque électrique. */
    TRAIN_DRIVER, TICKET_CLERK, TRAIN_INSPECTOR;

    /** Personnel ferroviaire (ne combat jamais, ne s'éloigne pas de son poste). */
    public boolean rail() {
        return this == TRAIN_DRIVER || this == TICKET_CLERK || this == TRAIN_INSPECTOR;
    }

    /** Personnel de base (ne patrouille pas, parle au joueur). */
    public boolean staff() {
        return this == RECRUITER || this == ARMORER || this == COOK || this == MEDIC;
    }

    public boolean falcon() {
        return ordinal() >= COMMANDER.ordinal() && ordinal() <= BRUTE.ordinal();
    }
}

package fr.modeminecraft.entity.bot;

import fr.modeminecraft.item.DisguiseArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/**
 * Tenues d'infiltration (récompenses des chambres fortes des boss) :
 * - tenue complète de Policier : la police et le SWAT le prennent pour un collègue (commissariats, arme en main) ;
 * - tenue du Boss du Désert (4 pièces, trouvées une à une) : immunité totale auprès des miliciens et de leurs
 *   tourelles une fois les 4 portées ensemble (1 à 3 pièces : ils attaquent toujours) ;
 * - uniformes soviétique, Forces Spéciales, Cartel, Shérif / Contremaître : distance de détection de la faction
 *   réduite de 65 % (tant qu'on ne s'approche pas à moins de 6 blocs d'un garde, ni ne dégaine).
 * Un crime commis sous les yeux d'un membre de la faction brise la couverture (appel radio).
 */
public final class Disguise {
    public enum Outfit { POLICE, DESERT, SOVIET, SPECIAL_FORCES, CARTEL, SHERIFF }

    /** Tenue complète portée (les 4 pièces de la même tenue), ou null. */
    public static Outfit worn(Player p) {
        Outfit o = null;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(p.getItemBySlot(s).getItem() instanceof DisguiseArmorItem d)) {
                return null;
            }
            if (o != null && o != d.outfit) {
                return null;
            }
            o = d.outfit;
        }
        return o;
    }

    /** La tenue correspond-elle à la faction de ce bot ? */
    static boolean matches(Outfit o, Faction f) {
        return switch (o) {
            case POLICE -> f.group.equals("police");
            case DESERT -> f == Faction.MILITIA;
            case SOVIET -> f == Faction.SOVIET || f == Faction.ARCTIC;
            case SPECIAL_FORCES -> f == Faction.GARRISON;
            case CARTEL -> f == Faction.CARTEL;
            case SHERIFF -> f == Faction.SHERIFF || f == Faction.REFINERY;
        };
    }

    /** Immunité totale (police, désert) : le bot n'attaque pas, ne fait pas de sommation, tant que la couverture tient. */
    public static boolean immune(HumanBotEntity bot, Player p) {
        if (FactionRelations.isAlly(p.getUUID(), bot.faction())) {
            return true;                                               // l'armée de son camp : ni sommation ni arrestation, armé ou non
        }
        Outfit o = worn(p);
        if (o != Outfit.POLICE && o != Outfit.DESERT || !matches(o, bot.faction())) {
            return false;
        }
        return !bot.hasGrudge(p) && !FactionRelations.isHostile(p.getUUID(), bot.faction(), bot.level().getGameTime());
    }

    /** Facteur de distance de détection (0,35 avec un uniforme de la faction, si l'on reste à distance et sans arme). */
    public static float sightFactor(HumanBotEntity bot, Player p) {
        Outfit o = worn(p);
        if (o == null || !matches(o, bot.faction()) || o == Outfit.POLICE || o == Outfit.DESERT) {
            return 1f;
        }
        if (bot.distanceTo(p) < 6 || BotGoals.armedInHand(p) || bot.hasGrudge(p)) {
            return 1f;
        }
        return 0.35f;
    }

    private Disguise() {}
}

package fr.modeminecraft.entity.bot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hostilité des factions envers chaque joueur (côté serveur). Une faction devient hostile à un joueur quand il
 * ignore une sommation, attaque l'un de ses membres, tire près d'elle ou pille ses coffres ; elle se calme après
 * 5 minutes sans incident.
 */
public final class FactionRelations {
    private static final long DURATION = 6000;
    private static final Map<UUID, Map<String, Long>> HOSTILE = new HashMap<>();

    public static boolean isHostile(UUID player, Faction faction, long now) {
        Map<String, Long> m = HOSTILE.get(player);
        Long until = m == null ? null : m.get(faction.group);
        return until != null && until > now;
    }

    public static void markHostile(UUID player, Faction faction, long now) {
        if (!faction.combatant() || faction == Faction.FALCON) {
            return;
        }
        HOSTILE.computeIfAbsent(player, k -> new HashMap<>()).put(faction.group, now + DURATION);
    }

    public static void forgive(UUID player, Faction faction) {
        Map<String, Long> m = HOSTILE.get(player);
        if (m != null) {
            m.remove(faction.group);
        }
    }

    /** Le joueur est mort : toutes les factions oublient leurs griefs contre lui. */
    public static void forgetAll(UUID player) {
        HOSTILE.remove(player);
    }

    public static void clear() {
        HOSTILE.clear();
    }

    private FactionRelations() {}
}

package fr.modeminecraft.entity.bot;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Escarmouches entre factions ennemies (police contre gangs, garnison contre soviétiques...) : pour que les villes ne
 * se vident pas en quelques minutes et que ça ne lague pas, au plus 2 fusillades à la fois par dimension. Une
 * fusillade dure 2 minutes ; les bots à moins de 40 blocs peuvent s'y joindre. Une fois finie, le quartier reste
 * calme pendant 3 minutes (pas de nouvelle fusillade à moins de 64 blocs).
 */
public final class Skirmishes {
    private record Fight(ResourceKey<Level> dim, Vec3 pos, long until) {}

    private static final List<Fight> FIGHTS = new ArrayList<>();
    private static final int MAX = 2, DURATION = 2400, COOLDOWN = 3600;

    /** Le bot peut-il engager ce bot d'une faction ennemie maintenant ? */
    public static synchronized boolean allow(HumanBotEntity bot, HumanBotEntity other) {
        long now = bot.level().getGameTime();
        var dim = bot.level().dimension();
        Vec3 at = bot.position();
        FIGHTS.removeIf(f -> f.until + COOLDOWN < now || f.until - DURATION > now + 1);   // + changement de monde
        int active = 0;
        for (Fight f : FIGHTS) {
            if (f.dim != dim) {
                continue;
            }
            boolean on = f.until > now;
            double d = f.pos.distanceToSqr(at);
            if (on && d < 40 * 40) {
                return true;                          // on rejoint la fusillade en cours
            }
            if (!on && d < 64 * 64) {
                return false;                         // le quartier vient de se calmer
            }
            if (on) {
                active++;
            }
        }
        if (active >= MAX) {
            return false;
        }
        FIGHTS.add(new Fight(dim, at.add(other.position()).scale(0.5), now + DURATION));
        return true;
    }

    private Skirmishes() {}
}

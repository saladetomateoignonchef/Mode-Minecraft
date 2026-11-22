package fr.modeminecraft.entity.military;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;

/**
 * Mise à l'eau de navires armés par des équipages de bots (flottes des bases navales, patrouilles des mercenaires) :
 * commandant, canonnier, officier d'armes, puis quelques matelots. Navires américains : US Army (garnison) ;
 * soviétiques : armée soviétique ; mercenaires : Offshore.
 */
public final class Fleets {
    private Fleets() {}

    public static Faction factionOf(MilType t) {
        return switch (t.side) {
            case US -> Faction.GARRISON;
            case SOVIET -> Faction.SOVIET;
            case MERC -> Faction.OFFSHORE;
        };
    }

    /** Navire à flot à (x, z), sa quille à la ligne de flottaison sous `surface`, équipage complet, soutes pleines. */
    public static MilVehicleEntity launch(ServerLevel level, MilType t, double x, double surface, double z, float yaw, boolean patrol) {
        return launch(level, t, x, surface, z, yaw, patrol, factionOf(t));
    }

    public static MilVehicleEntity launch(ServerLevel level, MilType t, double x, double surface, double z, float yaw, boolean patrol, Faction crew) {
        MilVehicleEntity v = MilVehicleEntity.create(level, t, x, surface - t.draft(), z, yaw);
        v.fillAmmo(1f);
        v.setFuel(1f);
        v.patrol = patrol;
        v.autoDefense = true;
        level.addFreshEntity(v);
        crew(level, v, crew);
        return v;
    }

    /** Équipage de bots (une place sur deux pour les matelots, au plus 3 en plus des postes de combat). */
    public static void crew(ServerLevel level, MilVehicleEntity v, Faction f) {
        int seats = v.type().seats.size();
        int posts = Math.min(seats, 3);
        int crew = Math.min(seats, posts + (t(v) ? 0 : Math.min(3, (seats - posts + 1) / 2)));
        BlockPos at = v.blockPosition();
        for (int i = 0; i < crew; i++) {
            HumanBotEntity b = ModEntities.HUMAN_BOT.get().create(level);
            if (b == null) {
                return;
            }
            b.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 0, 0);
            b.setup(f, i == 0 ? BotRole.OFFICER : BotRole.SOLDIER, level.getRandom());
            b.convoy = true;
            b.battle = true;                                                // équipages de guerre : ils engagent les navires ennemis
            b.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
            level.addFreshEntity(b);
            b.startRiding(v, true);
        }
    }

    /** Petites embarcations : seulement les postes. */
    private static boolean t(MilVehicleEntity v) {
        return v.type().rudder() > 2;
    }
}

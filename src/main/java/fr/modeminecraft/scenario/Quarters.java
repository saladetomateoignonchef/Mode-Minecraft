package fr.modeminecraft.scenario;

import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Logement attribué selon le grade : nouveau lit (point de réapparition) et nouveau casier personnel. */
public final class Quarters {
    private Quarters() {}

    /** Base principale du joueur (ville), ou null. */
    public static CityPlanner.City base(ServerPlayer p) {
        ScenarioData.Record r = Scenario.record(p);
        if (r.base == null || r.side == null) {
            return null;
        }
        ServerLevel level = p.serverLevel();
        CityPlanner.City c = CityPlanner.cityAtCached(CityPlanner.Env.of(level), r.base.getX(), r.base.getZ(), 8);
        return c != null && c.variant().equals(r.side.baseVariant) ? c : null;
    }

    public static void reassign(ServerPlayer p, Grade g) {
        ScenarioData.Record r = Scenario.record(p);
        CityPlanner.City c = base(p);
        ScenarioSites.Site site = ScenarioSites.housing(g);
        if (c == null || site.bed == null) {
            return;
        }
        ServerLevel level = p.serverLevel();
        BlockPos bed = ScenarioSites.at(c, r.side, site, site.bed), locker = ScenarioSites.at(c, r.side, site, site.locker);
        level.getChunk(bed.getX() >> 4, bed.getZ() >> 4);
        p.setRespawnPosition(level.dimension(), bed, 0, false, false);
        BarracksLockerBlock.assign(level, locker, p);
        r.bed = bed;
        r.locker = locker;
        ScenarioData.get(level).setDirty();
        p.sendSystemMessage(Component.translatable("message.modeminecraft.quarters.assigned",
                Component.translatable("scenario.modeminecraft.site." + site.plot())).withStyle(ChatFormatting.AQUA));
    }
}

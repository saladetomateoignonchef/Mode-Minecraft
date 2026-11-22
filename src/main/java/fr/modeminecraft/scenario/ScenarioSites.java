package fr.modeminecraft.scenario;

import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.core.BlockPos;

/**
 * Emplacement des bâtiments du scénario dans les bases (parcelles réservées du plan des villes « base » et
 * « soviet ») et coordonnées locales utiles (lit et casier du joueur, point d'arrivée).
 */
public final class ScenarioSites {
    private ScenarioSites() {}

    /** Dortoir des recrues : parcelle d'ancrage (2 x 1 chunks). */
    public static final int[] DORM_US = {-4, -2}, DORM_SOVIET = {-5, -8};
    /** Dans le dortoir (repère du bâtiment, y = 0 au niveau du sol) : arrivée, lit n° 1 et son casier. */
    public static final int[] DORM_SPAWN = {9, 0, 7}, DORM_BED = {7, 0, 2}, DORM_LOCKER = {7, 0, 3};

    public record Dorm(BlockPos spawn, BlockPos bed, BlockPos locker, float yaw) {}

    public static BlockPos at(CityPlanner.City c, int[] anchor, int[] local) {
        return new BlockPos((c.center().x + anchor[0]) * 16 + local[0], c.baseY() + local[1], (c.center().z + anchor[1]) * 16 + local[2]);
    }

    public static Dorm dorm(CityPlanner.City c, Side side) {
        int[] a = side == Side.US ? DORM_US : DORM_SOVIET;
        return new Dorm(at(c, a, DORM_SPAWN), at(c, a, DORM_BED), at(c, a, DORM_LOCKER), -90);
    }
}

package fr.modeminecraft.scenario;

import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.core.BlockPos;

/**
 * Emplacement des bâtiments du scénario dans les bases (parcelles réservées du plan des villes « base » et
 * « soviet ») et coordonnées locales utiles (lit et casier du joueur, point d'arrivée, PNJ).
 */
public final class ScenarioSites {
    private ScenarioSites() {}

    /** Bâtiments : parcelle d'ancrage US et URSS, taille (chunks), lit et casier du logement (repère du bâtiment). */
    public enum Site {
        DORMITORY(-4, -2, -5, -8, 2, 1, new int[]{7, 0, 2}, new int[]{7, 0, 3}, Grade.SOLDIER),
        RECRUITMENT(-3, -3, -5, -7, 2, 1, null, null, Grade.SOLDIER),
        ARMORY(-4, -3, -2, -8, 1, 1, null, null, Grade.SOLDIER),
        CANTEEN(-1, -3, -1, -8, 1, 1, null, null, Grade.SOLDIER),
        INFIRMARY(0, -3, -2, -7, 1, 1, null, null, Grade.SOLDIER),
        CORPORAL_BARRACKS(1, -3, -1, -7, 1, 1, new int[]{3, 0, 2}, new int[]{3, 0, 3}, Grade.CORPORAL),
        NCO_QUARTERS(-4, -4, -5, -5, 1, 1, new int[]{3, 0, 3}, new int[]{5, 0, 2}, Grade.SERGEANT),
        OFFICER_PAVILION(-3, -4, -4, -5, 1, 1, new int[]{3, 0, 3}, new int[]{7, 0, 2}, Grade.LIEUTENANT),
        OFFICERS_CLUB(-2, -4, -5, -4, 1, 1, null, null, Grade.SERGEANT),
        WAR_ROOM(-1, -4, -4, -4, 1, 1, null, null, Grade.LIEUTENANT),
        COMMAND_RESIDENCE(0, -4, -2, -5, 1, 1, new int[]{4, 0, 4}, new int[]{7, 0, 3}, Grade.MAJOR),
        GENERAL_BUNKER(1, -4, -1, -5, 1, 1, new int[]{4, -6, 4}, new int[]{7, -6, 3}, Grade.GENERAL),
        OFFICERS_PARKING(2, -4, -2, -4, 2, 1, null, null, Grade.LIEUTENANT),
        SHOOTING_RANGE(2, -3, 1, -5, 2, 1, null, null, Grade.SOLDIER),
        PRISON(2, -2, 1, -8, 2, 2, null, null, Grade.SOLDIER);

        public final int usX, usZ, sovX, sovZ, w, h;
        public final int[] bed, locker;
        public final Grade grade;

        Site(int usX, int usZ, int sovX, int sovZ, int w, int h, int[] bed, int[] locker, Grade grade) {
            this.usX = usX;
            this.usZ = usZ;
            this.sovX = sovX;
            this.sovZ = sovZ;
            this.w = w;
            this.h = h;
            this.bed = bed;
            this.locker = locker;
            this.grade = grade;
        }

        public String plot() {
            return name().toLowerCase();
        }

        public int[] anchor(Side s) {
            return s == Side.US ? new int[]{usX, usZ} : new int[]{sovX, sovZ};
        }

        /** Cette parcelle (dx, dz) appartient-elle au bâtiment dans la base de ce camp ? */
        public boolean covers(Side s, int dx, int dz) {
            int[] a = anchor(s);
            return dx >= a[0] && dx < a[0] + w && dz >= a[1] && dz < a[1] + h;
        }
    }

    public static Site byPlot(String type) {
        for (Site s : Site.values()) {
            if (s.plot().equals(type)) {
                return s;
            }
        }
        return null;
    }

    /** Bâtiment du scénario sur cette parcelle de la base du camp, ou null. */
    public static Site siteAt(Side s, int dx, int dz) {
        for (Site site : Site.values()) {
            if (site.covers(s, dx, dz)) {
                return site;
            }
        }
        return null;
    }

    /** Logement attribué selon le grade (Général : bunker privé… Soldat : dortoir des recrues). */
    public static Site housing(Grade g) {
        return switch (g) {
            case SOLDIER -> Site.DORMITORY;
            case CORPORAL -> Site.CORPORAL_BARRACKS;
            case SERGEANT -> Site.NCO_QUARTERS;
            case LIEUTENANT -> Site.OFFICER_PAVILION;
            case MAJOR -> Site.COMMAND_RESIDENCE;
            case GENERAL -> Site.GENERAL_BUNKER;
        };
    }

    public static final int[] DORM_SPAWN = {9, 0, 7};

    public static BlockPos at(CityPlanner.City c, int[] anchor, int[] local) {
        return new BlockPos((c.center().x + anchor[0]) * 16 + local[0], c.baseY() + local[1], (c.center().z + anchor[1]) * 16 + local[2]);
    }

    public static BlockPos at(CityPlanner.City c, Side side, Site site, int[] local) {
        return at(c, site.anchor(side), local);
    }

    public record Dorm(BlockPos spawn, BlockPos bed, BlockPos locker, float yaw) {}

    public static Dorm dorm(CityPlanner.City c, Side side) {
        Site d = Site.DORMITORY;
        return new Dorm(at(c, side, d, DORM_SPAWN), at(c, side, d, d.bed), at(c, side, d, d.locker), -90);
    }
}

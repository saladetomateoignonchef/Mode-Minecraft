package fr.modeminecraft.entity.military;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * FICHIER GÉNÉRÉ par tools/generate_military_vehicles.py — ne pas modifier à la main.
 * Géométrie des véhicules militaires, en mètres (= blocs) dans le repère du véhicule : x vers l'avant, y vers le haut,
 * z vers la droite, origine au centre du véhicule au ras du sol. Sert au rendu (pivots des pièces mobiles, roues) et
 * au gameplay (places des passagers, bouches de tir : mount 0 = caisse, 1 = tourelle, 2 = canon).
 */
public final class MilGeometry {
    /**
     * scale : 1 pixel de modèle = scale / 16 bloc ; ox : abscisse (pixels) de l'origine du véhicule dans le modèle.
     * wheels : {x, y, z, rayon}. seats : {x, y, z}. muzzles : {x, y, z, mount}.
     */
    public record Geo(float scale, float ox, Vec3 turret, Vec3 gun, Vec3 rotor, Vec3 tail, Vec3 radar, float[][] wheels, float[][] seats, float[][] muzzles) {}

    private static final Map<String, Geo> ALL = new HashMap<>();

    public static Geo get(String id) {
        return ALL.get(id);
    }

    private static void put(String id, Geo g) {
        ALL.put(id, g);
    }

    static {
        put("uaz_469", new Geo(2.0f, 8.0f, null, null, null, null, null,
                new float[][]{{1.25f, 0.42f, -0.8f, 0.42f}, {1.25f, 0.42f, 0.8f, 0.42f}, {-1.25f, 0.42f, -0.8f, 0.42f}, {-1.25f, 0.42f, 0.8f, 0.42f}},
                new float[][]{{0.0f, 0.85f, -0.4f}, {0.0f, 0.85f, 0.4f}, {-1.1f, 0.85f, -0.4f}, {-1.1f, 0.85f, 0.4f}},
                new float[][]{}));
        put("tigr", new Geo(2.5f, 8.5f, new Vec3(-0.3, 2.35, 0.0), new Vec3(0.0, 2.7, 0.0), null, null, null,
                new float[][]{{1.75f, 0.55f, -1.0f, 0.55f}, {1.75f, 0.55f, 1.0f, 0.55f}, {-1.75f, 0.55f, -1.0f, 0.55f}, {-1.75f, 0.55f, 1.0f, 0.55f}},
                new float[][]{{0.4f, 1.05f, -0.5f}, {0.4f, 1.05f, 0.5f}, {-0.3f, 1.75f, 0.0f}, {-1.6f, 1.05f, 0.0f}},
                new float[][]{{1.3f, 2.7f, 0.0f, 2f}}));
        put("kamaz", new Geo(3.0f, 8.5f, null, null, null, null, null,
                new float[][]{{2.6f, 0.6f, -1.05f, 0.6f}, {2.6f, 0.6f, 1.05f, 0.6f}, {-1.4f, 0.6f, -1.05f, 0.6f}, {-1.4f, 0.6f, 1.05f, 0.6f}, {-2.75f, 0.6f, -1.05f, 0.6f}, {-2.75f, 0.6f, 1.05f, 0.6f}},
                new float[][]{{2.3f, 1.15f, -0.5f}, {2.3f, 1.15f, 0.5f}, {-2.5f, 1.55f, -0.8f}, {-2.5f, 1.55f, 0.8f}, {-1.6f, 1.55f, -0.8f}, {-1.6f, 1.55f, 0.8f}, {-0.7f, 1.55f, -0.8f}, {-0.7f, 1.55f, 0.8f}, {0.20000000000000018f, 1.55f, -0.8f}, {0.20000000000000018f, 1.55f, 0.8f}},
                new float[][]{}));
        put("brdm_2", new Geo(2.5f, 8.0f, new Vec3(0.35, 2.0, 0.0), new Vec3(0.8, 2.3, 0.0), null, null, null,
                new float[][]{{1.45f, 0.55f, -1.1f, 0.55f}, {1.45f, 0.55f, 1.1f, 0.55f}, {-1.55f, 0.55f, -1.1f, 0.55f}, {-1.55f, 0.55f, 1.1f, 0.55f}},
                new float[][]{{1.2f, 0.9f, -0.45f}, {0.35f, 1.25f, 0.0f}, {1.2f, 0.9f, 0.45f}},
                new float[][]{{2.65f, 2.3f, 0.0f, 2f}, {1.5f, 2.3f, 0.18f, 2f}}));
        put("btr_80", new Geo(3.0f, 8.0f, new Vec3(0.9, 2.3, 0.0), new Vec3(1.4, 2.55, 0.0), null, null, null,
                new float[][]{{2.2f, 0.6f, -1.2f, 0.6f}, {2.2f, 0.6f, 1.2f, 0.6f}, {0.9f, 0.6f, -1.2f, 0.6f}, {0.9f, 0.6f, 1.2f, 0.6f}, {-1.0f, 0.6f, -1.2f, 0.6f}, {-1.0f, 0.6f, 1.2f, 0.6f}, {-2.3f, 0.6f, -1.2f, 0.6f}, {-2.3f, 0.6f, 1.2f, 0.6f}},
                new float[][]{{2.3f, 1.05f, -0.5f}, {0.9f, 1.3f, 0.0f}, {2.3f, 1.05f, 0.5f}, {-0.4f, 1.05f, -0.6f}, {-0.4f, 1.05f, 0.6f}, {-1.4f, 1.05f, -0.6f}, {-1.4f, 1.05f, 0.6f}, {-2.4f, 1.05f, -0.6f}, {-2.4f, 1.05f, 0.6f}},
                new float[][]{{3.65f, 2.55f, 0.0f, 2f}, {2.2f, 2.55f, 0.25f, 2f}}));
        put("bmp_2", new Geo(3.0f, 7.0f, new Vec3(0.2, 1.95, 0.0), new Vec3(0.95, 2.25, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.1f, 0.9f, -0.55f}, {0.2f, 1.3f, 0.25f}, {0.2f, 1.3f, -0.3f}, {-0.9f, 0.9f, -0.5f}, {-1.4f, 0.9f, 0.5f}, {-1.9f, 0.9f, -0.5f}, {-2.4f, 0.9f, 0.5f}, {-2.9f, 0.9f, -0.5f}},
                new float[][]{{3.85f, 2.25f, 0.0f, 2f}, {0.4f, 2.7f, 0.48f, 1f}}));
        put("bm_21_grad", new Geo(3.0f, 8.5f, new Vec3(-1.6, 1.75, 0.0), new Vec3(-1.6, 2.25, 0.0), null, null, null,
                new float[][]{{2.7f, 0.6f, -1.05f, 0.6f}, {2.7f, 0.6f, 1.05f, 0.6f}, {-1.5f, 0.6f, -1.05f, 0.6f}, {-1.5f, 0.6f, 1.05f, 0.6f}, {-2.8f, 0.6f, -1.05f, 0.6f}, {-2.8f, 0.6f, 1.05f, 0.6f}},
                new float[][]{{1.3f, 1.15f, -0.5f}, {1.3f, 1.15f, 0.5f}},
                new float[][]{{0.65f, 2.36f, 0.07f, 2f}}));
        put("zsu_23_4", new Geo(3.0f, 7.5f, new Vec3(-0.2, 1.85, 0.0), new Vec3(1.3, 2.35, 0.0), null, null, new Vec3(-1.3, 3.35, 0.0),
                new float[][]{},
                new float[][]{{2.1f, 0.85f, -0.5f}, {-0.2f, 1.4f, 0.4f}, {-0.2f, 1.4f, -0.4f}},
                new float[][]{{3.45f, 2.35f, 0.0f, 2f}}));
        put("mi_24", new Geo(5.0f, 8.0f, new Vec3(6.0, 1.0, 0.0), new Vec3(6.0, 0.75, 0.0), new Vec3(0.6, 3.75, 0.0), new Vec3(-6.3, 3.7, 0.3), null,
                new float[][]{},
                new float[][]{{4.1f, 2.0f, 0.0f}, {5.6f, 1.4f, 0.0f}, {1.8f, 1.0f, -0.5f}, {1.8f, 1.0f, 0.5f}, {0.5f, 1.0f, -0.5f}, {0.5f, 1.0f, 0.5f}, {-0.8f, 1.0f, -0.5f}, {-0.8f, 1.0f, 0.5f}},
                new float[][]{{7.05f, 0.75f, 0.0f, 2f}, {1.05f, 1.5f, -1.8f, 0f}, {1.05f, 1.5f, 1.8f, 0f}, {1.25f, 1.9f, -3.25f, 0f}, {1.25f, 1.9f, 3.25f, 0f}}));
        put("t_90m", new Geo(3.25f, 2.5f, new Vec3(0.0, 1.55, 0.0), new Vec3(1.6, 1.95, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.2f, 0.75f, 0.0f}, {0.0f, 1.05f, 0.5f}, {0.0f, 1.05f, -0.5f}},
                new float[][]{{5.95f, 1.95f, 0.0f, 2f}, {0.75f, 2.65f, 0.6f, 1f}}));
        put("t_80bvm", new Geo(3.25f, 2.5f, new Vec3(0.0, 1.5, 0.0), new Vec3(1.5, 1.9, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.2f, 0.75f, 0.0f}, {0.0f, 1.05f, 0.5f}, {0.0f, 1.05f, -0.5f}},
                new float[][]{{5.95f, 1.9f, 0.0f, 2f}, {0.75f, 2.62f, 0.55f, 1f}}));
        put("t_14_armata", new Geo(3.75f, 4.5f, new Vec3(-0.6, 1.65, 0.0), new Vec3(1.0, 2.0, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.6f, 1.05f, -0.45f}, {2.6f, 1.05f, 0.0f}, {2.6f, 1.05f, 0.45f}},
                new float[][]{{6.35f, 2.0f, 0.0f, 2f}, {2.25f, 2.0f, 0.42f, 2f}}));
        put("bmpt_terminator", new Geo(3.0f, 8.5f, new Vec3(-0.3, 1.5, 0.0), new Vec3(0.6, 2.0, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.2f, 0.75f, 0.0f}, {-0.3f, 1.05f, 0.45f}, {-0.3f, 1.05f, -0.45f}, {1.8f, 0.75f, 1.0f}, {1.8f, 0.75f, -1.0f}},
                new float[][]{{3.45f, 2.2f, -1.2f, 2f}, {3.45f, 2.2f, 1.2f, 2f}, {0.95f, 2.69f, -1.2f, 2f}, {0.95f, 2.69f, 1.2f, 2f}, {2.05f, 1.6f, 1.05f, 0f}}));
        put("sprut_sd", new Geo(3.25f, 2.0f, new Vec3(-0.2, 1.55, 0.0), new Vec3(1.3, 1.85, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.1f, 0.7f, 0.0f}, {-0.2f, 1.0f, 0.45f}, {-0.2f, 1.0f, -0.45f}},
                new float[][]{{5.95f, 1.85f, 0.0f, 2f}}));
        put("humvee", new Geo(2.0f, 7.5f, new Vec3(-0.15, 2.05, 0.0), new Vec3(0.15, 2.5, 0.0), null, null, null,
                new float[][]{{1.55f, 0.45f, -0.95f, 0.45f}, {1.55f, 0.45f, 0.95f, 0.45f}, {-1.5f, 0.45f, -0.95f, 0.45f}, {-1.5f, 0.45f, 0.95f, 0.45f}},
                new float[][]{{0.3f, 0.9f, -0.45f}, {0.3f, 0.9f, 0.45f}, {-0.15f, 1.55f, 0.0f}, {-0.7f, 0.9f, -0.45f}, {-0.7f, 0.9f, 0.45f}},
                new float[][]{{1.4f, 2.5f, 0.0f, 2f}}));
        put("m_atv", new Geo(2.5f, 7.5f, new Vec3(-0.7, 2.65, 0.0), new Vec3(-0.3, 3.05, 0.0), null, null, null,
                new float[][]{{1.9f, 0.6f, -1.05f, 0.6f}, {1.9f, 0.6f, 1.05f, 0.6f}, {-1.9f, 0.6f, -1.05f, 0.6f}, {-1.9f, 0.6f, 1.05f, 0.6f}},
                new float[][]{{0.5f, 1.2f, -0.5f}, {0.5f, 1.2f, 0.5f}, {-0.7f, 2.15f, 0.0f}, {-1.3f, 1.2f, -0.5f}, {-1.3f, 1.2f, 0.5f}},
                new float[][]{{1.0f, 3.05f, 0.0f, 2f}}));
        put("m939", new Geo(3.0f, 8.5f, null, null, null, null, null,
                new float[][]{{2.9f, 0.58f, -1.05f, 0.58f}, {2.9f, 0.58f, 1.05f, 0.58f}, {-1.6f, 0.58f, -1.05f, 0.58f}, {-1.6f, 0.58f, 1.05f, 0.58f}, {-2.9f, 0.58f, -1.05f, 0.58f}, {-2.9f, 0.58f, 1.05f, 0.58f}},
                new float[][]{{1.4f, 1.15f, -0.5f}, {1.4f, 1.15f, 0.5f}, {-3.3f, 1.5f, -0.8f}, {-3.3f, 1.5f, 0.8f}, {-2.4f, 1.5f, -0.8f}, {-2.4f, 1.5f, 0.8f}, {-1.4999999999999998f, 1.5f, -0.8f}, {-1.4999999999999998f, 1.5f, 0.8f}, {-0.5999999999999996f, 1.5f, -0.8f}, {-0.5999999999999996f, 1.5f, 0.8f}},
                new float[][]{}));
        put("stryker", new Geo(3.0f, 8.0f, new Vec3(0.4, 2.2, 0.0), new Vec3(0.6, 2.75, -0.15), null, null, null,
                new float[][]{{2.2f, 0.58f, -1.25f, 0.58f}, {2.2f, 0.58f, 1.25f, 0.58f}, {0.9f, 0.58f, -1.25f, 0.58f}, {0.9f, 0.58f, 1.25f, 0.58f}, {-1.0f, 0.58f, -1.25f, 0.58f}, {-1.0f, 0.58f, 1.25f, 0.58f}, {-2.3f, 0.58f, -1.25f, 0.58f}, {-2.3f, 0.58f, 1.25f, 0.58f}},
                new float[][]{{2.3f, 1.1f, -0.5f}, {0.6f, 1.1f, 0.5f}, {-0.2f, 1.05f, -0.6f}, {-0.2f, 1.05f, 0.6f}, {-0.95f, 1.05f, -0.6f}, {-0.95f, 1.05f, 0.6f}, {-1.7f, 1.05f, -0.6f}, {-1.7f, 1.05f, 0.6f}, {-2.45f, 1.05f, -0.6f}, {-2.45f, 1.05f, 0.6f}, {-3.2f, 1.05f, -0.6f}},
                new float[][]{{1.85f, 2.75f, -0.15f, 2f}}));
        put("m113", new Geo(2.5f, 8.0f, new Vec3(0.55, 2.12, -0.2), new Vec3(0.7, 2.45, -0.2), null, null, null,
                new float[][]{},
                new float[][]{{1.4f, 0.85f, -0.45f}, {0.55f, 1.55f, -0.2f}, {0.3f, 0.85f, -0.5f}, {0.3f, 0.85f, 0.5f}, {-0.3f, 0.85f, -0.5f}, {-0.3f, 0.85f, 0.5f}, {-0.8999999999999999f, 0.85f, -0.5f}, {-0.8999999999999999f, 0.85f, 0.5f}, {-1.4999999999999998f, 0.85f, -0.5f}, {-1.4999999999999998f, 0.85f, 0.5f}, {-2.1f, 0.85f, -0.5f}},
                new float[][]{{1.95f, 2.45f, -0.2f, 2f}}));
        put("m2a3_bradley", new Geo(3.0f, 6.5f, new Vec3(0.0, 1.9, 0.0), new Vec3(1.4, 2.35, 0.3), null, null, null,
                new float[][]{},
                new float[][]{{2.1f, 0.8f, -0.55f}, {0.0f, 1.3f, 0.45f}, {0.0f, 1.3f, -0.45f}, {-1.0f, 0.85f, -0.55f}, {-1.0f, 0.85f, 0.55f}, {-1.7f, 0.85f, -0.55f}, {-1.7f, 0.85f, 0.55f}, {-2.4f, 0.85f, -0.55f}, {-2.4f, 0.85f, 0.55f}},
                new float[][]{{3.95f, 2.35f, 0.3f, 2f}, {0.7f, 2.3f, -1.28f, 1f}}));
        put("m1a2_abrams", new Geo(3.5f, 4.0f, new Vec3(-0.3, 1.6, 0.0), new Vec3(1.7, 2.0, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.4f, 0.75f, 0.0f}, {-0.3f, 1.1f, 0.5f}, {-0.3f, 1.1f, -0.5f}, {-0.8f, 1.1f, 0.0f}},
                new float[][]{{5.95f, 2.0f, 0.0f, 2f}, {0.6f, 2.8f, 0.65f, 1f}}));
        put("m142_himars", new Geo(3.0f, 8.5f, new Vec3(-1.0, 1.45, 0.0), new Vec3(-2.6, 1.9, 0.0), null, null, null,
                new float[][]{{2.6f, 0.58f, -1.05f, 0.58f}, {2.6f, 0.58f, 1.05f, 0.58f}, {-1.4f, 0.58f, -1.05f, 0.58f}, {-1.4f, 0.58f, 1.05f, 0.58f}, {-2.7f, 0.58f, -1.05f, 0.58f}, {-2.7f, 0.58f, 1.05f, 0.58f}},
                new float[][]{{2.4f, 1.15f, -0.55f}, {2.4f, 1.15f, 0.0f}, {2.4f, 1.15f, 0.55f}},
                new float[][]{{1.3f, 2.25f, 0.0f, 2f}}));
        put("avenger", new Geo(2.0f, 7.5f, new Vec3(-1.1, 1.45, 0.0), new Vec3(-1.1, 2.1, 0.0), null, null, null,
                new float[][]{{1.55f, 0.45f, -0.95f, 0.45f}, {1.55f, 0.45f, 0.95f, 0.45f}, {-1.5f, 0.45f, -0.95f, 0.45f}, {-1.5f, 0.45f, 0.95f, 0.45f}},
                new float[][]{{0.6f, 0.9f, -0.45f}, {-1.1f, 1.5f, 0.0f}},
                new float[][]{{-0.25f, 2.12f, -0.8f, 2f}, {-0.25f, 2.12f, 0.8f, 2f}, {0.35f, 1.75f, 0.0f, 2f}}));
        put("uh_60_black_hawk", new Geo(5.5f, 8.5f, null, null, new Vec3(0.6, 3.5, 0.0), new Vec3(-6.8, 3.4, 0.25), null,
                new float[][]{},
                new float[][]{{3.4f, 1.4f, -0.45f}, {2.2f, 1.3f, -0.9f}, {2.2f, 1.3f, 0.9f}, {3.4f, 1.4f, 0.45f}, {1.2f, 1.0f, -0.5f}, {1.2f, 1.0f, 0.5f}, {0.3999999999999999f, 1.0f, -0.5f}, {0.3999999999999999f, 1.0f, 0.5f}, {-0.40000000000000013f, 1.0f, -0.5f}, {-0.40000000000000013f, 1.0f, 0.5f}, {-1.2000000000000004f, 1.0f, -0.5f}, {-1.2000000000000004f, 1.0f, 0.5f}},
                new float[][]{{3.05f, 1.82f, -1.25f, 0f}, {3.05f, 1.82f, 1.25f, 0f}}));
        put("ah_64e_apache", new Geo(5.0f, 9.5f, new Vec3(3.2, 0.9, 0.0), new Vec3(3.2, 0.65, 0.0), new Vec3(0.3, 3.15, 0.0), new Vec3(-6.3, 3.2, 0.25), null,
                new float[][]{},
                new float[][]{{3.1f, 1.55f, 0.0f}, {4.1f, 1.35f, 0.0f}},
                new float[][]{{4.45f, 0.65f, 0.0f, 2f}, {0.95f, 1.15f, -1.4f, 0f}, {0.95f, 1.15f, 1.4f, 0f}, {0.95f, 1.28f, -2.3f, 0f}, {0.95f, 1.28f, 2.3f, 0f}}));
        put("s_400", new Geo(4.25f, 8.0f, new Vec3(-5.8, 1.6, 0.0), new Vec3(-5.8, 2.0, 0.0), null, null, new Vec3(3.15, 4.0, 0.0),
                new float[][]{{5.0f, 0.68f, -1.3f, 0.68f}, {5.0f, 0.68f, 1.3f, 0.68f}, {3.4f, 0.68f, -1.3f, 0.68f}, {3.4f, 0.68f, 1.3f, 0.68f}, {-2.6f, 0.68f, -1.3f, 0.68f}, {-2.6f, 0.68f, 1.3f, 0.68f}, {-4.2f, 0.68f, -1.3f, 0.68f}, {-4.2f, 0.68f, 1.3f, 0.68f}},
                new float[][]{{5.3f, 1.2f, -0.6f}, {5.3f, 1.2f, 0.6f}},
                new float[][]{{1.9f, 2.25f, -0.95f, 2f}}));
        put("s_500", new Geo(4.75f, 8.5f, new Vec3(-6.3, 1.6, 0.0), new Vec3(-6.3, 2.1, 0.0), null, null, new Vec3(3.5, 3.4, 0.0),
                new float[][]{{5.4f, 0.7f, -1.3f, 0.7f}, {5.4f, 0.7f, 1.3f, 0.7f}, {4.0f, 0.7f, -1.3f, 0.7f}, {4.0f, 0.7f, 1.3f, 0.7f}, {-1.6f, 0.7f, -1.3f, 0.7f}, {-1.6f, 0.7f, 1.3f, 0.7f}, {-3.0f, 0.7f, -1.3f, 0.7f}, {-3.0f, 0.7f, 1.3f, 0.7f}, {-4.4f, 0.7f, -1.3f, 0.7f}, {-4.4f, 0.7f, 1.3f, 0.7f}},
                new float[][]{{5.6f, 1.25f, -0.6f}, {5.6f, 1.25f, 0.6f}},
                new float[][]{{2.35f, 2.3f, -0.65f, 2f}}));
        put("iskander", new Geo(4.5f, 8.0f, new Vec3(-5.8, 2.0, 0.0), new Vec3(-5.8, 2.3, 0.0), null, null, null,
                new float[][]{{5.0f, 0.7f, -1.3f, 0.7f}, {5.0f, 0.7f, 1.3f, 0.7f}, {3.4f, 0.7f, -1.3f, 0.7f}, {3.4f, 0.7f, 1.3f, 0.7f}, {-2.8f, 0.7f, -1.3f, 0.7f}, {-2.8f, 0.7f, 1.3f, 0.7f}, {-4.4f, 0.7f, -1.3f, 0.7f}, {-4.4f, 0.7f, 1.3f, 0.7f}},
                new float[][]{{5.2f, 1.2f, -0.6f}, {5.2f, 1.2f, 0.6f}},
                new float[][]{{2.15f, 2.3f, -0.5f, 2f}}));
    }

    private MilGeometry() {}
}

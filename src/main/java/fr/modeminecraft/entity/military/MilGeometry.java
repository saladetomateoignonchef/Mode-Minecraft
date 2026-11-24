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
     * wheels : {x, y, z, rayon}. seats : {x, y, z}. muzzles : {x, y, z, mount}. props : moyeux d'hélices {x, y, z}.
     */
    public record Geo(float scale, float ox, Vec3 turret, Vec3 gun, Vec3 rotor, Vec3 tail, Vec3 radar, float[][] wheels, float[][] seats, float[][] muzzles,
                      float[][] props, float halfSpan) {}

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
                new float[][]{},
                new float[][]{}, 0.95f));
        put("tigr", new Geo(2.5f, 8.5f, new Vec3(-0.3, 2.35, 0.0), new Vec3(0.0, 2.7, 0.0), null, null, null,
                new float[][]{{1.75f, 0.55f, -1.0f, 0.55f}, {1.75f, 0.55f, 1.0f, 0.55f}, {-1.75f, 0.55f, -1.0f, 0.55f}, {-1.75f, 0.55f, 1.0f, 0.55f}},
                new float[][]{{0.4f, 1.05f, -0.5f}, {0.4f, 1.05f, 0.5f}, {-0.3f, 1.75f, 0.0f}, {-1.6f, 1.05f, 0.0f}},
                new float[][]{{1.3f, 2.7f, 0.0f, 2f}},
                new float[][]{}, 1.2f));
        put("kamaz", new Geo(3.0f, 8.5f, null, null, null, null, null,
                new float[][]{{2.6f, 0.6f, -1.05f, 0.6f}, {2.6f, 0.6f, 1.05f, 0.6f}, {-1.4f, 0.6f, -1.05f, 0.6f}, {-1.4f, 0.6f, 1.05f, 0.6f}, {-2.75f, 0.6f, -1.05f, 0.6f}, {-2.75f, 0.6f, 1.05f, 0.6f}},
                new float[][]{{2.3f, 1.15f, -0.5f}, {2.3f, 1.15f, 0.5f}, {-2.5f, 1.55f, -0.8f}, {-2.5f, 1.55f, 0.8f}, {-1.6f, 1.55f, -0.8f}, {-1.6f, 1.55f, 0.8f}, {-0.7f, 1.55f, -0.8f}, {-0.7f, 1.55f, 0.8f}, {0.20000000000000018f, 1.55f, -0.8f}, {0.20000000000000018f, 1.55f, 0.8f}},
                new float[][]{},
                new float[][]{}, 1.3f));
        put("brdm_2", new Geo(2.5f, 8.0f, new Vec3(0.35, 2.0, 0.0), new Vec3(0.8, 2.3, 0.0), null, null, null,
                new float[][]{{1.45f, 0.55f, -1.1f, 0.55f}, {1.45f, 0.55f, 1.1f, 0.55f}, {-1.55f, 0.55f, -1.1f, 0.55f}, {-1.55f, 0.55f, 1.1f, 0.55f}},
                new float[][]{{1.2f, 0.9f, -0.45f}, {0.35f, 1.25f, 0.0f}, {1.2f, 0.9f, 0.45f}},
                new float[][]{{2.65f, 2.3f, 0.0f, 2f}, {1.5f, 2.3f, 0.18f, 2f}},
                new float[][]{}, 1.2f));
        put("btr_80", new Geo(3.0f, 8.0f, new Vec3(0.9, 2.3, 0.0), new Vec3(1.4, 2.55, 0.0), null, null, null,
                new float[][]{{2.2f, 0.6f, -1.2f, 0.6f}, {2.2f, 0.6f, 1.2f, 0.6f}, {0.9f, 0.6f, -1.2f, 0.6f}, {0.9f, 0.6f, 1.2f, 0.6f}, {-1.0f, 0.6f, -1.2f, 0.6f}, {-1.0f, 0.6f, 1.2f, 0.6f}, {-2.3f, 0.6f, -1.2f, 0.6f}, {-2.3f, 0.6f, 1.2f, 0.6f}},
                new float[][]{{2.3f, 1.05f, -0.5f}, {0.9f, 1.3f, 0.0f}, {2.3f, 1.05f, 0.5f}, {-0.4f, 1.05f, -0.6f}, {-0.4f, 1.05f, 0.6f}, {-1.4f, 1.05f, -0.6f}, {-1.4f, 1.05f, 0.6f}, {-2.4f, 1.05f, -0.6f}, {-2.4f, 1.05f, 0.6f}},
                new float[][]{{3.65f, 2.55f, 0.0f, 2f}, {2.2f, 2.55f, 0.25f, 2f}},
                new float[][]{}, 1.3f));
        put("bmp_2", new Geo(3.0f, 7.0f, new Vec3(0.2, 1.95, 0.0), new Vec3(0.95, 2.25, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.1f, 0.9f, -0.55f}, {0.2f, 1.3f, 0.25f}, {0.2f, 1.3f, -0.3f}, {-0.9f, 0.9f, -0.5f}, {-1.4f, 0.9f, 0.5f}, {-1.9f, 0.9f, -0.5f}, {-2.4f, 0.9f, 0.5f}, {-2.9f, 0.9f, -0.5f}},
                new float[][]{{3.85f, 2.25f, 0.0f, 2f}, {0.4f, 2.7f, 0.48f, 1f}},
                new float[][]{}, 1.61f));
        put("bm_21_grad", new Geo(3.0f, 8.5f, new Vec3(-1.6, 1.75, 0.0), new Vec3(-1.6, 2.25, 0.0), null, null, null,
                new float[][]{{2.7f, 0.6f, -1.05f, 0.6f}, {2.7f, 0.6f, 1.05f, 0.6f}, {-1.5f, 0.6f, -1.05f, 0.6f}, {-1.5f, 0.6f, 1.05f, 0.6f}, {-2.8f, 0.6f, -1.05f, 0.6f}, {-2.8f, 0.6f, 1.05f, 0.6f}},
                new float[][]{{1.3f, 1.15f, -0.5f}, {1.3f, 1.15f, 0.5f}},
                new float[][]{{0.65f, 2.36f, 0.07f, 2f}},
                new float[][]{}, 1.3f));
        put("zsu_23_4", new Geo(3.0f, 7.5f, new Vec3(-0.2, 1.85, 0.0), new Vec3(1.3, 2.35, 0.0), null, null, new Vec3(-1.3, 3.35, 0.0),
                new float[][]{},
                new float[][]{{2.1f, 0.85f, -0.5f}, {-0.2f, 1.4f, 0.4f}, {-0.2f, 1.4f, -0.4f}},
                new float[][]{{3.45f, 2.35f, 0.0f, 2f}},
                new float[][]{}, 1.56f));
        put("mi_24", new Geo(5.0f, 8.0f, new Vec3(6.0, 1.0, 0.0), new Vec3(6.0, 0.75, 0.0), new Vec3(0.6, 3.75, 0.0), new Vec3(-6.3, 3.7, 0.3), null,
                new float[][]{},
                new float[][]{{4.1f, 2.0f, 0.0f}, {5.6f, 1.4f, 0.0f}, {1.8f, 1.0f, -0.5f}, {1.8f, 1.0f, 0.5f}, {0.5f, 1.0f, -0.5f}, {0.5f, 1.0f, 0.5f}, {-0.8f, 1.0f, -0.5f}, {-0.8f, 1.0f, 0.5f}},
                new float[][]{{7.05f, 0.75f, 0.0f, 2f}, {1.05f, 1.5f, -1.8f, 0f}, {1.05f, 1.5f, 1.8f, 0f}, {1.25f, 1.9f, -3.25f, 0f}, {1.25f, 1.9f, 3.25f, 0f}},
                new float[][]{}, 6.56f));
        put("t_90m", new Geo(3.25f, 2.5f, new Vec3(0.0, 1.55, 0.0), new Vec3(1.6, 1.95, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.2f, 0.75f, 0.0f}, {0.0f, 1.05f, 0.5f}, {0.0f, 1.05f, -0.5f}},
                new float[][]{{5.95f, 1.95f, 0.0f, 2f}, {0.75f, 2.65f, 0.6f, 1f}},
                new float[][]{}, 1.86f));
        put("t_80bvm", new Geo(3.25f, 2.5f, new Vec3(0.0, 1.5, 0.0), new Vec3(1.5, 1.9, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.2f, 0.75f, 0.0f}, {0.0f, 1.05f, 0.5f}, {0.0f, 1.05f, -0.5f}},
                new float[][]{{5.95f, 1.9f, 0.0f, 2f}, {0.75f, 2.62f, 0.55f, 1f}},
                new float[][]{}, 1.83f));
        put("t_14_armata", new Geo(3.75f, 4.5f, new Vec3(-0.6, 1.65, 0.0), new Vec3(1.0, 2.0, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.6f, 1.05f, -0.45f}, {2.6f, 1.05f, 0.0f}, {2.6f, 1.05f, 0.45f}},
                new float[][]{{6.35f, 2.0f, 0.0f, 2f}, {2.25f, 2.0f, 0.42f, 2f}},
                new float[][]{}, 1.86f));
        put("bmpt_terminator", new Geo(3.0f, 8.5f, new Vec3(-0.3, 1.5, 0.0), new Vec3(0.6, 2.0, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.2f, 0.75f, 0.0f}, {-0.3f, 1.05f, 0.45f}, {-0.3f, 1.05f, -0.45f}, {1.8f, 0.75f, 1.0f}, {1.8f, 0.75f, -1.0f}},
                new float[][]{{3.45f, 2.2f, -1.2f, 2f}, {3.45f, 2.2f, 1.2f, 2f}, {0.95f, 2.69f, -1.2f, 2f}, {0.95f, 2.69f, 1.2f, 2f}, {2.05f, 1.6f, 1.05f, 0f}},
                new float[][]{}, 1.83f));
        put("sprut_sd", new Geo(3.25f, 2.0f, new Vec3(-0.2, 1.55, 0.0), new Vec3(1.3, 1.85, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.1f, 0.7f, 0.0f}, {-0.2f, 1.0f, 0.45f}, {-0.2f, 1.0f, -0.45f}},
                new float[][]{{5.95f, 1.85f, 0.0f, 2f}},
                new float[][]{}, 1.61f));
        put("humvee", new Geo(2.0f, 7.5f, new Vec3(-0.15, 2.05, 0.0), new Vec3(0.15, 2.5, 0.0), null, null, null,
                new float[][]{{1.55f, 0.45f, -0.95f, 0.45f}, {1.55f, 0.45f, 0.95f, 0.45f}, {-1.5f, 0.45f, -0.95f, 0.45f}, {-1.5f, 0.45f, 0.95f, 0.45f}},
                new float[][]{{0.3f, 0.9f, -0.45f}, {0.3f, 0.9f, 0.45f}, {-0.15f, 1.55f, 0.0f}, {-0.7f, 0.9f, -0.45f}, {-0.7f, 0.9f, 0.45f}},
                new float[][]{{1.4f, 2.5f, 0.0f, 2f}},
                new float[][]{}, 1.08f));
        put("m_atv", new Geo(2.5f, 7.5f, new Vec3(-0.7, 2.65, 0.0), new Vec3(-0.3, 3.05, 0.0), null, null, null,
                new float[][]{{1.9f, 0.6f, -1.05f, 0.6f}, {1.9f, 0.6f, 1.05f, 0.6f}, {-1.9f, 0.6f, -1.05f, 0.6f}, {-1.9f, 0.6f, 1.05f, 0.6f}},
                new float[][]{{0.5f, 1.2f, -0.5f}, {0.5f, 1.2f, 0.5f}, {-0.7f, 2.15f, 0.0f}, {-1.3f, 1.2f, -0.5f}, {-1.3f, 1.2f, 0.5f}},
                new float[][]{{1.0f, 3.05f, 0.0f, 2f}},
                new float[][]{}, 1.22f));
        put("m939", new Geo(3.0f, 8.5f, null, null, null, null, null,
                new float[][]{{2.9f, 0.58f, -1.05f, 0.58f}, {2.9f, 0.58f, 1.05f, 0.58f}, {-1.6f, 0.58f, -1.05f, 0.58f}, {-1.6f, 0.58f, 1.05f, 0.58f}, {-2.9f, 0.58f, -1.05f, 0.58f}, {-2.9f, 0.58f, 1.05f, 0.58f}},
                new float[][]{{1.4f, 1.15f, -0.5f}, {1.4f, 1.15f, 0.5f}, {-3.3f, 1.5f, -0.8f}, {-3.3f, 1.5f, 0.8f}, {-2.4f, 1.5f, -0.8f}, {-2.4f, 1.5f, 0.8f}, {-1.4999999999999998f, 1.5f, -0.8f}, {-1.4999999999999998f, 1.5f, 0.8f}, {-0.5999999999999996f, 1.5f, -0.8f}, {-0.5999999999999996f, 1.5f, 0.8f}},
                new float[][]{},
                new float[][]{}, 1.27f));
        put("stryker", new Geo(3.0f, 8.0f, new Vec3(0.4, 2.2, 0.0), new Vec3(0.6, 2.75, -0.15), null, null, null,
                new float[][]{{2.2f, 0.58f, -1.25f, 0.58f}, {2.2f, 0.58f, 1.25f, 0.58f}, {0.9f, 0.58f, -1.25f, 0.58f}, {0.9f, 0.58f, 1.25f, 0.58f}, {-1.0f, 0.58f, -1.25f, 0.58f}, {-1.0f, 0.58f, 1.25f, 0.58f}, {-2.3f, 0.58f, -1.25f, 0.58f}, {-2.3f, 0.58f, 1.25f, 0.58f}},
                new float[][]{{2.3f, 1.1f, -0.5f}, {0.6f, 1.1f, 0.5f}, {-0.2f, 1.05f, -0.6f}, {-0.2f, 1.05f, 0.6f}, {-0.95f, 1.05f, -0.6f}, {-0.95f, 1.05f, 0.6f}, {-1.7f, 1.05f, -0.6f}, {-1.7f, 1.05f, 0.6f}, {-2.45f, 1.05f, -0.6f}, {-2.45f, 1.05f, 0.6f}, {-3.2f, 1.05f, -0.6f}},
                new float[][]{{1.85f, 2.75f, -0.15f, 2f}},
                new float[][]{}, 1.38f));
        put("m113", new Geo(2.5f, 8.0f, new Vec3(0.55, 2.12, -0.2), new Vec3(0.7, 2.45, -0.2), null, null, null,
                new float[][]{},
                new float[][]{{1.4f, 0.85f, -0.45f}, {0.55f, 1.55f, -0.2f}, {0.3f, 0.85f, -0.5f}, {0.3f, 0.85f, 0.5f}, {-0.3f, 0.85f, -0.5f}, {-0.3f, 0.85f, 0.5f}, {-0.8999999999999999f, 0.85f, -0.5f}, {-0.8999999999999999f, 0.85f, 0.5f}, {-1.4999999999999998f, 0.85f, -0.5f}, {-1.4999999999999998f, 0.85f, 0.5f}, {-2.1f, 0.85f, -0.5f}},
                new float[][]{{1.95f, 2.45f, -0.2f, 2f}},
                new float[][]{}, 1.41f));
        put("m2a3_bradley", new Geo(3.0f, 6.5f, new Vec3(0.0, 1.9, 0.0), new Vec3(1.4, 2.35, 0.3), null, null, null,
                new float[][]{},
                new float[][]{{2.1f, 0.8f, -0.55f}, {0.0f, 1.3f, 0.45f}, {0.0f, 1.3f, -0.45f}, {-1.0f, 0.85f, -0.55f}, {-1.0f, 0.85f, 0.55f}, {-1.7f, 0.85f, -0.55f}, {-1.7f, 0.85f, 0.55f}, {-2.4f, 0.85f, -0.55f}, {-2.4f, 0.85f, 0.55f}},
                new float[][]{{3.95f, 2.35f, 0.3f, 2f}, {0.7f, 2.3f, -1.28f, 1f}},
                new float[][]{}, 1.86f));
        put("m1a2_abrams", new Geo(3.5f, 4.0f, new Vec3(-0.3, 1.6, 0.0), new Vec3(1.7, 2.0, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{2.4f, 0.75f, 0.0f}, {-0.3f, 1.1f, 0.5f}, {-0.3f, 1.1f, -0.5f}, {-0.8f, 1.1f, 0.0f}},
                new float[][]{{5.95f, 2.0f, 0.0f, 2f}, {0.6f, 2.8f, 0.65f, 1f}},
                new float[][]{}, 1.89f));
        put("m142_himars", new Geo(3.0f, 8.5f, new Vec3(-1.0, 1.45, 0.0), new Vec3(-2.6, 1.9, 0.0), null, null, null,
                new float[][]{{2.6f, 0.58f, -1.05f, 0.58f}, {2.6f, 0.58f, 1.05f, 0.58f}, {-1.4f, 0.58f, -1.05f, 0.58f}, {-1.4f, 0.58f, 1.05f, 0.58f}, {-2.7f, 0.58f, -1.05f, 0.58f}, {-2.7f, 0.58f, 1.05f, 0.58f}},
                new float[][]{{2.4f, 1.15f, -0.55f}, {2.4f, 1.15f, 0.0f}, {2.4f, 1.15f, 0.55f}},
                new float[][]{{1.3f, 2.25f, 0.0f, 2f}},
                new float[][]{}, 1.22f));
        put("avenger", new Geo(2.0f, 7.5f, new Vec3(-1.1, 1.45, 0.0), new Vec3(-1.1, 2.1, 0.0), null, null, null,
                new float[][]{{1.55f, 0.45f, -0.95f, 0.45f}, {1.55f, 0.45f, 0.95f, 0.45f}, {-1.5f, 0.45f, -0.95f, 0.45f}, {-1.5f, 0.45f, 0.95f, 0.45f}},
                new float[][]{{0.6f, 0.9f, -0.45f}, {-1.1f, 1.5f, 0.0f}},
                new float[][]{{-0.25f, 2.12f, -0.8f, 2f}, {-0.25f, 2.12f, 0.8f, 2f}, {0.35f, 1.75f, 0.0f, 2f}},
                new float[][]{}, 1.1f));
        put("uh_60_black_hawk", new Geo(5.5f, 8.5f, null, null, new Vec3(0.6, 3.5, 0.0), new Vec3(-6.8, 3.4, 0.25), null,
                new float[][]{},
                new float[][]{{3.4f, 1.4f, -0.45f}, {2.2f, 1.3f, -0.9f}, {2.2f, 1.3f, 0.9f}, {3.4f, 1.4f, 0.45f}, {1.2f, 1.0f, -0.5f}, {1.2f, 1.0f, 0.5f}, {0.3999999999999999f, 1.0f, -0.5f}, {0.3999999999999999f, 1.0f, 0.5f}, {-0.40000000000000013f, 1.0f, -0.5f}, {-0.40000000000000013f, 1.0f, 0.5f}, {-1.2000000000000004f, 1.0f, -0.5f}, {-1.2000000000000004f, 1.0f, 0.5f}},
                new float[][]{{3.05f, 1.82f, -1.25f, 0f}, {3.05f, 1.82f, 1.25f, 0f}},
                new float[][]{}, 7.02f));
        put("ah_64e_apache", new Geo(5.0f, 9.5f, new Vec3(3.2, 0.9, 0.0), new Vec3(3.2, 0.65, 0.0), new Vec3(0.3, 3.15, 0.0), new Vec3(-6.3, 3.2, 0.25), null,
                new float[][]{},
                new float[][]{{3.1f, 1.55f, 0.0f}, {4.1f, 1.35f, 0.0f}},
                new float[][]{{4.45f, 0.65f, 0.0f, 2f}, {0.95f, 1.15f, -1.4f, 0f}, {0.95f, 1.15f, 1.4f, 0f}, {0.95f, 1.28f, -2.3f, 0f}, {0.95f, 1.28f, 2.3f, 0f}},
                new float[][]{}, 6.12f));
        put("s_400", new Geo(4.25f, 8.0f, new Vec3(-5.8, 1.6, 0.0), new Vec3(-5.8, 2.0, 0.0), null, null, new Vec3(3.15, 4.0, 0.0),
                new float[][]{{5.0f, 0.68f, -1.3f, 0.68f}, {5.0f, 0.68f, 1.3f, 0.68f}, {3.4f, 0.68f, -1.3f, 0.68f}, {3.4f, 0.68f, 1.3f, 0.68f}, {-2.6f, 0.68f, -1.3f, 0.68f}, {-2.6f, 0.68f, 1.3f, 0.68f}, {-4.2f, 0.68f, -1.3f, 0.68f}, {-4.2f, 0.68f, 1.3f, 0.68f}},
                new float[][]{{5.3f, 1.2f, -0.6f}, {5.3f, 1.2f, 0.6f}},
                new float[][]{{1.9f, 2.25f, -0.95f, 2f}},
                new float[][]{}, 1.42f));
        put("s_500", new Geo(4.75f, 8.5f, new Vec3(-6.3, 1.6, 0.0), new Vec3(-6.3, 2.1, 0.0), null, null, new Vec3(3.5, 3.4, 0.0),
                new float[][]{{5.4f, 0.7f, -1.3f, 0.7f}, {5.4f, 0.7f, 1.3f, 0.7f}, {4.0f, 0.7f, -1.3f, 0.7f}, {4.0f, 0.7f, 1.3f, 0.7f}, {-1.6f, 0.7f, -1.3f, 0.7f}, {-1.6f, 0.7f, 1.3f, 0.7f}, {-3.0f, 0.7f, -1.3f, 0.7f}, {-3.0f, 0.7f, 1.3f, 0.7f}, {-4.4f, 0.7f, -1.3f, 0.7f}, {-4.4f, 0.7f, 1.3f, 0.7f}},
                new float[][]{{5.6f, 1.25f, -0.6f}, {5.6f, 1.25f, 0.6f}},
                new float[][]{{2.35f, 2.3f, -0.65f, 2f}},
                new float[][]{}, 1.45f));
        put("iskander", new Geo(4.5f, 8.0f, new Vec3(-5.8, 2.0, 0.0), new Vec3(-5.8, 2.3, 0.0), null, null, null,
                new float[][]{{5.0f, 0.7f, -1.3f, 0.7f}, {5.0f, 0.7f, 1.3f, 0.7f}, {3.4f, 0.7f, -1.3f, 0.7f}, {3.4f, 0.7f, 1.3f, 0.7f}, {-2.8f, 0.7f, -1.3f, 0.7f}, {-2.8f, 0.7f, 1.3f, 0.7f}, {-4.4f, 0.7f, -1.3f, 0.7f}, {-4.4f, 0.7f, 1.3f, 0.7f}},
                new float[][]{{5.2f, 1.2f, -0.6f}, {5.2f, 1.2f, 0.6f}},
                new float[][]{{2.15f, 2.3f, -0.5f, 2f}},
                new float[][]{}, 1.42f));
        put("b29_superfortress", new Geo(10.0f, 8.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{8.0f, 2.1f, -0.35f}, {9.0f, 1.9000000000000001f, 0.0f}, {4.8f, 3.1f, 0.0f}, {-2.4f, 3.1f, 0.0f}, {-1.5f, 1.9000000000000001f, 0.5f}, {-9.5f, 2.3000000000000003f, 0.0f}},
                new float[][]{{6.5f, 4.2f, 0.0f, 0f}, {-1.3f, 4.2f, 0.0f, 0f}, {-0.2f, 1.2500000000000002f, 0.0f, 0f}, {-11.0f, 2.8000000000000003f, 0.0f, 0f}},
                new float[][]{{5.25f, 2.5500000000000003f, -4.4f}, {5.25f, 2.5500000000000003f, 4.4f}, {5.25f, 2.5500000000000003f, -8.4f}, {5.25f, 2.5500000000000003f, 8.4f}}, 13.95f));
        put("il2_sturmovik", new Geo(5.5f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{0.9f, 1.75f, 0.0f}},
                new float[][]{{3.5f, 1.5999999999999999f, 2.0f, 0f}, {2.5f, 1.3499999999999999f, -3.2f, 0f}, {2.5f, 1.3499999999999999f, 3.2f, 0f}, {3.5f, 1.5999999999999999f, -0.3f, 0f}, {3.5f, 1.5999999999999999f, 0.3f, 0f}, {3.5f, 1.5999999999999999f, -0.3f, 0f}, {3.5f, 1.5999999999999999f, 0.3f, 0f}},
                new float[][]{{6.0f, 1.9f, 0.0f}}, 7.3f));
        put("la7", new Geo(4.0f, 8.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{0.1f, 1.5999999999999999f, 0.0f}},
                new float[][]{{4.3f, 2.0f, 0.0f, 0f}, {4.3f, 2.0f, -0.3f, 0f}, {4.3f, 2.0f, 0.3f, 0f}, {4.3f, 2.0f, -0.3f, 0f}, {4.3f, 2.0f, 0.3f, 0f}, {4.3f, 2.0f, -0.3f, 0f}, {4.3f, 2.0f, 0.3f, 0f}},
                new float[][]{{4.5f, 1.7f, 0.0f}}, 4.9f));
        put("mig15", new Geo(4.0f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{2.5f, 1.55f, 0.0f}},
                new float[][]{{4.8f, 1.25f, 0.0f, 0f}, {4.8f, 1.25f, -0.3f, 0f}, {4.8f, 1.25f, 0.3f, 0f}, {4.8f, 1.25f, -0.3f, 0f}, {4.8f, 1.25f, 0.3f, 0f}, {4.8f, 1.25f, -0.3f, 0f}, {4.8f, 1.25f, 0.3f, 0f}},
                new float[][]{}, 5.0f));
        put("tu95_bear", new Geo(12.5f, 9.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{11.5f, 2.8000000000000003f, -0.45f}, {11.5f, 2.8000000000000003f, 0.45f}, {-15.8f, 2.2f, 0.0f}, {-6.0f, 2.9f, 0.0f}},
                new float[][]{{14.0f, 1.8f, 0.0f, 0f}, {14.0f, 1.8f, -0.3f, 0f}, {14.0f, 1.8f, 0.3f, 0f}, {1.0f, 1.6f, -3.0f, 0f}, {1.0f, 1.6f, 3.0f, 0f}, {14.0f, 1.8f, -0.3f, 0f}, {14.0f, 1.8f, 0.3f, 0f}, {-17.0f, 2.7f, 0.0f, 0f}, {-6.0f, 4.5f, 0.0f, 0f}},
                new float[][]{{3.71f, 2.0f, -5.2f}, {3.3099999999999996f, 2.0f, -5.2f}, {3.71f, 2.0f, 5.2f}, {3.3099999999999996f, 2.0f, 5.2f}, {1.0699999999999996f, 2.0f, -10.0f}, {0.6699999999999995f, 2.0f, -10.0f}, {1.0699999999999996f, 2.0f, 10.0f}, {0.6699999999999995f, 2.0f, 10.0f}}, 17.5f));
        put("mig21", new Geo(5.0f, 7.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{3.6f, 1.55f, 0.0f}},
                new float[][]{{2.5f, 1.05f, 0.0f, 0f}, {-0.5f, 1.1f, -2.5f, 0f}, {-0.5f, 1.1f, 2.5f, 0f}, {-0.5f, 1.1f, -1.6f, 0f}, {-0.5f, 1.1f, 1.6f, 0f}, {2.5f, 1.05f, -0.3f, 0f}, {2.5f, 1.05f, 0.3f, 0f}},
                new float[][]{}, 3.6f));
        put("mig25", new Geo(7.0f, 8.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{6.2f, 2.05f, 0.0f}},
                new float[][]{{9.0f, 2.0f, 0.0f, 0f}, {9.0f, 2.0f, -0.3f, 0f}, {9.0f, 2.0f, 0.3f, 0f}, {0.0f, 1.8f, -4.0f, 0f}, {0.0f, 1.8f, 4.0f, 0f}, {9.0f, 2.0f, -0.3f, 0f}, {9.0f, 2.0f, 0.3f, 0f}},
                new float[][]{}, 7.0f));
        put("su25", new Geo(5.5f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{4.3f, 1.8499999999999999f, 0.0f}},
                new float[][]{{6.5f, 1.3499999999999999f, -0.3f, 0f}, {1.5f, 1.2999999999999998f, -4.0f, 0f}, {1.5f, 1.2999999999999998f, 4.0f, 0f}, {1.6f, 1.4f, -5.8f, 0f}, {1.6f, 1.4f, 5.8f, 0f}, {6.5f, 1.3499999999999999f, -0.3f, 0f}, {6.5f, 1.3499999999999999f, 0.3f, 0f}},
                new float[][]{}, 7.2f));
        put("mig29", new Geo(6.0f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{4.2f, 1.65f, 0.0f}},
                new float[][]{{3.5f, 1.8f, -0.6f, 0f}, {3.5f, 1.8f, -0.3f, 0f}, {3.5f, 1.8f, 0.3f, 0f}, {0.0f, 1.4f, -2.6f, 0f}, {0.0f, 1.4f, 2.6f, 0f}, {0.0f, 1.4f, -3.6f, 0f}, {0.0f, 1.4f, 3.6f, 0f}},
                new float[][]{}, 5.7f));
        put("su34", new Geo(7.5f, 7.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{6.6f, 1.95f, -0.45f}, {6.6f, 1.95f, 0.45f}},
                new float[][]{{5.5f, 2.2f, 0.95f, 0f}, {5.5f, 2.2f, -0.3f, 0f}, {5.5f, 2.2f, 0.3f, 0f}, {0.5f, 1.5f, -2.6f, 0f}, {0.5f, 1.5f, 2.6f, 0f}, {0.5f, 1.5f, -3.8f, 0f}, {0.5f, 1.5f, 3.8f, 0f}},
                new float[][]{}, 7.3f));
        put("su57", new Geo(7.5f, 8.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{5.8f, 1.8499999999999999f, 0.0f}},
                new float[][]{{5.0f, 2.05f, 0.7f, 0f}, {5.0f, 2.05f, -0.3f, 0f}, {5.0f, 2.05f, 0.3f, 0f}, {-0.5f, 1.0999999999999999f, -0.4f, 0f}, {-0.5f, 1.0999999999999999f, 0.4f, 0f}, {-1.5f, 1.0999999999999999f, -0.4f, 0f}, {-1.5f, 1.0999999999999999f, 0.4f, 0f}},
                new float[][]{}, 7.0f));
        put("p51_mustang", new Geo(4.0f, 8.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{0.3f, 1.5999999999999999f, 0.0f}},
                new float[][]{{2.0f, 1.4f, 2.2f, 0f}, {1.2f, 1.0499999999999998f, -3.0f, 0f}, {1.2f, 1.0499999999999998f, 3.0f, 0f}, {2.0f, 1.4f, -0.3f, 0f}, {2.0f, 1.4f, 0.3f, 0f}, {2.0f, 1.4f, -0.3f, 0f}, {2.0f, 1.4f, 0.3f, 0f}},
                new float[][]{{4.9f, 1.7f, 0.0f}}, 5.6f));
        put("light_civil_plane", new Geo(3.0f, 9.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{0.3f, 1.15f, -0.3f}, {0.3f, 1.15f, 0.3f}},
                new float[][]{},
                new float[][]{{3.15f, 1.21f, 0.0f}}, 4.0f));
        put("regional_airliner", new Geo(10.0f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{7.4f, 2.3f, -0.55f}, {7.4f, 2.3f, 0.55f}, {4.0f, 1.95f, -0.75f}, {4.0f, 1.95f, 0.75f}, {2.5f, 1.95f, -0.75f}, {2.5f, 1.95f, 0.75f}, {1.0f, 1.95f, -0.75f}, {1.0f, 1.95f, 0.75f}},
                new float[][]{},
                new float[][]{{4.15f, 3.55f, -4.6f}, {4.15f, 3.55f, 4.6f}}, 13.0f));
        put("f86_sabre", new Geo(4.0f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{2.7f, 1.55f, 0.0f}},
                new float[][]{{4.6f, 1.7000000000000002f, 0.45f, 0f}, {0.9f, 1.1f, -2.6f, 0f}, {0.9f, 1.1f, 2.6f, 0f}, {4.6f, 1.7000000000000002f, -0.3f, 0f}, {4.6f, 1.7000000000000002f, 0.3f, 0f}, {4.6f, 1.7000000000000002f, -0.3f, 0f}, {4.6f, 1.7000000000000002f, 0.3f, 0f}},
                new float[][]{}, 5.6f));
        put("f4_phantom", new Geo(6.5f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{5.6f, 1.8499999999999999f, 0.0f}, {4.0f, 1.8499999999999999f, 0.0f}},
                new float[][]{{8.4f, 1.4f, 0.0f, 0f}, {8.4f, 1.4f, -0.3f, 0f}, {8.4f, 1.4f, 0.3f, 0f}, {0.0f, 1.4f, -2.4f, 0f}, {0.0f, 1.4f, 2.4f, 0f}, {0.0f, 1.4f, -3.6f, 0f}, {0.0f, 1.4f, 3.6f, 0f}},
                new float[][]{}, 5.8f));
        put("c130_hercules", new Geo(13.5f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{12.2f, 3.6f, -0.5f}, {12.2f, 3.6f, 0.5f}, {7.0f, 1.6f, -1.2f}, {7.0f, 1.6f, 1.2f}, {5.4f, 1.6f, -1.2f}, {5.4f, 1.6f, 1.2f}, {3.8f, 1.6f, -1.2f}, {3.8f, 1.6f, 1.2f}, {2.1999999999999993f, 1.6f, -1.2f}, {2.1999999999999993f, 1.6f, 1.2f}, {0.5999999999999996f, 1.6f, -1.2f}, {0.5999999999999996f, 1.6f, 1.2f}, {-1.0f, 1.6f, -1.2f}, {-1.0f, 1.6f, 1.2f}, {-2.6000000000000014f, 1.6f, -1.2f}, {-2.6000000000000014f, 1.6f, 1.2f}, {-4.200000000000001f, 1.6f, -1.2f}, {-4.200000000000001f, 1.6f, 1.2f}},
                new float[][]{{13.0f, 2.0f, 0.0f, 0f}, {13.0f, 2.0f, -0.3f, 0f}, {13.0f, 2.0f, 0.3f, 0f}, {13.0f, 2.0f, -0.3f, 0f}, {13.0f, 2.0f, 0.3f, 0f}, {13.0f, 2.0f, -0.3f, 0f}, {13.0f, 2.0f, 0.3f, 0f}},
                new float[][]{{4.7f, 4.4f, -5.2f}, {4.7f, 4.4f, 5.2f}, {4.7f, 4.4f, -9.8f}, {4.7f, 4.4f, 9.8f}}, 19.5f));
        put("a10_thunderbolt", new Geo(6.5f, 7.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{5.0f, 2.0f, 0.0f}},
                new float[][]{{9.2f, 1.55f, 0.0f, 0f}, {1.8f, 1.25f, -4.2f, 0f}, {1.8f, 1.25f, 4.2f, 0f}, {1.8f, 1.3f, -6.4f, 0f}, {1.8f, 1.3f, 6.4f, 0f}, {9.2f, 1.55f, -0.3f, 0f}, {9.2f, 1.55f, 0.3f, 0f}},
                new float[][]{}, 8.7f));
        put("f117_nighthawk", new Geo(5.5f, 5.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{3.8f, 1.55f, 0.0f}},
                new float[][]{{0.0f, 1.0f, 0.0f, 0f}, {0.0f, 1.0f, -0.3f, 0f}, {0.0f, 1.0f, 0.3f, 0f}, {-0.5f, 0.8999999999999999f, -0.35f, 0f}, {-0.5f, 0.8999999999999999f, 0.35f, 0f}, {0.5f, 0.8999999999999999f, -0.35f, 0f}, {0.5f, 0.8999999999999999f, 0.35f, 0f}},
                new float[][]{}, 6.6f));
        put("f15e_strike_eagle", new Geo(7.0f, 8.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{6.2f, 1.9f, 0.0f}, {4.8f, 1.9f, 0.0f}},
                new float[][]{{4.3f, 2.0f, 1.7f, 0f}, {4.3f, 2.0f, -0.3f, 0f}, {4.3f, 2.0f, 0.3f, 0f}, {0.0f, 1.45f, -2.8f, 0f}, {0.0f, 1.45f, 2.8f, 0f}, {0.0f, 1.45f, -4.0f, 0f}, {0.0f, 1.45f, 4.0f, 0f}},
                new float[][]{}, 6.5f));
        put("f16_falcon", new Geo(5.5f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{4.5f, 1.65f, 0.0f}},
                new float[][]{{3.8f, 1.9f, -0.55f, 0f}, {0.0f, 1.25f, -3.2f, 0f}, {0.0f, 1.25f, 3.2f, 0f}, {0.0f, 1.2999999999999998f, -2.2f, 0f}, {0.0f, 1.2999999999999998f, 2.2f, 0f}, {3.8f, 1.9f, -0.3f, 0f}, {3.8f, 1.9f, 0.3f, 0f}},
                new float[][]{}, 4.75f));
        put("b2_spirit", new Geo(12.0f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{4.8f, 2.8000000000000003f, -0.4f}, {4.8f, 2.8000000000000003f, 0.4f}},
                new float[][]{{4.0f, 1.3000000000000003f, 0.0f, 0f}, {4.0f, 1.3000000000000003f, -0.3f, 0f}, {4.0f, 1.3000000000000003f, 0.3f, 0f}, {4.0f, 1.3000000000000003f, -0.3f, 0f}, {4.0f, 1.3000000000000003f, 0.3f, 0f}, {4.0f, 1.3000000000000003f, -0.3f, 0f}, {4.0f, 1.3000000000000003f, 0.3f, 0f}},
                new float[][]{}, 17.12f));
        put("f22_raptor", new Geo(7.0f, 10.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{5.6f, 1.75f, 0.0f}},
                new float[][]{{3.8f, 1.95f, 1.75f, 0f}, {3.8f, 1.95f, -0.3f, 0f}, {3.8f, 1.95f, 0.3f, 0f}, {-0.5f, 0.9f, -0.4f, 0f}, {-0.5f, 0.9f, 0.4f, 0f}, {-1.5f, 0.9f, -0.4f, 0f}, {-1.5f, 0.9f, 0.4f, 0f}},
                new float[][]{}, 6.8f));
        put("uss_gerald_r_ford", new Geo(46f, 8.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{2f, 33.1f, 14.2f}, {45.0f, 19.7f, -15.0f}, {-1f, 33.1f, 15.6f}, {-8.0f, 33.1f, 13.0f}, {-5.5f, 33.1f, 13.0f}, {-3.0f, 33.1f, 13.0f}},
                new float[][]{{45.0f, 19.7f, -15.8f, 0f}, {45.0f, 19.7f, -15.8f, 0f}, {25.0f, 18.9f, -3f, 0f}, {25.0f, 18.9f, 3f, 0f}, {35.0f, 18.8f, 0f, 0f}, {0f, 2.0f, -6f, 0f}, {0f, 2.0f, 6f, 0f}, {-53.0f, 20.0f, 15.8f, 0f}, {-57.0f, 18.9f, 0f, 0f}},
                new float[][]{}, 17.0f));
        put("admiral_kuznetsov", new Geo(46f, 8.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{2f, 32.1f, 12.2f}, {40.0f, 18.7f, -13.0f}, {-1f, 32.1f, 13.6f}, {-8.0f, 32.1f, 11.0f}, {-5.5f, 32.1f, 11.0f}, {-3.0f, 32.1f, 11.0f}},
                new float[][]{{40.0f, 18.7f, -13.8f, 0f}, {40.0f, 18.7f, -13.8f, 0f}, {20.0f, 17.9f, -3f, 0f}, {20.0f, 17.9f, 3f, 0f}, {30.0f, 17.8f, 0f, 0f}, {0f, 2.0f, -6f, 0f}, {0f, 2.0f, 6f, 0f}, {-48.0f, 19.0f, 13.8f, 0f}, {-52.0f, 17.9f, 0f, 0f}},
                new float[][]{}, 15.0f));
        put("kirov_cruiser", new Geo(33f, 8.0f, new Vec3(38, 7.06, 0.0), new Vec3(39.92, 7.9399999999999995, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{10f, 15.0f, 0f}, {-38f, 8.0f, 0f}, {6f, 15.0f, 1.5f}, {0f, 8.0f, 3.5f}, {-3f, 8.0f, 3.5f}, {-6f, 8.0f, 3.5f}},
                new float[][]{{46.0f, 7.9399999999999995f, 0.0f, 2f}, {15.4f, 16.6f, 5.5f, 0f}, {26f, 8.0f, -3.0f, 0f}, {26f, 8.0f, 3.0f, 0f}, {26f, 8.0f, 0f, 0f}, {10f, 2f, -7.6f, 0f}, {10f, 2f, 7.6f, 0f}, {-29f, 8.0f, 0f, 0f}, {-40f, 8.0f, 0f, 0f}},
                new float[][]{}, 8.0f));
        put("admiral_gorshkov", new Geo(19f, 8.0f, new Vec3(20, 5.06, 0.0), new Vec3(21.44, 5.72, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{6f, 11.0f, 0f}, {16f, 6.0f, 0f}, {4f, 11.0f, 1.2f}, {-3f, 6.0f, 2.5f}},
                new float[][]{{26.0f, 5.72f, 0.0f, 2f}, {-7.6f, 9.6f, 2.5f, 0f}, {13f, 6.0f, -1.5f, 0f}, {13f, 6.0f, 1.5f, 0f}, {13f, 6.0f, 0f, 0f}, {-4f, 1.6f, -4.4f, 0f}, {-4f, 1.6f, 4.4f, 0f}, {12f, 6.0f, 0f, 0f}, {-10f, 6.0f, 0f, 0f}},
                new float[][]{}, 4.6f));
        put("slava_cruiser", new Geo(26f, 8.0f, new Vec3(30, 6.06, 0.0), new Vec3(31.68, 6.83, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{10f, 13.0f, 0f}, {26f, 7.0f, 0f}, {6f, 13.0f, 1.2f}, {-2f, 7.0f, 3f}},
                new float[][]{{37.0f, 6.83f, 0.0f, 2f}, {-12.6f, 10.6f, 3.5f, 0f}, {24f, 7.5f, -4.6f, 0f}, {24f, 7.5f, 4.6f, 0f}, {10f, 7.0f, 0f, 0f}, {-6f, 2f, -6.2f, 0f}, {-6f, 2f, 6.2f, 0f}, {-21f, 7.0f, 0f, 0f}, {20f, 7.0f, 1.5f, 0f}},
                new float[][]{}, 6.6f));
        put("udaloy_destroyer", new Geo(23f, 8.0f, new Vec3(26, 5.659999999999999, 0.0), new Vec3(27.56, 6.374999999999999, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{14f, 12.6f, 0f}, {22f, 6.6f, 0f}, {10f, 12.6f, 1.2f}, {-8f, 6.6f, 3f}},
                new float[][]{{32.5f, 6.374999999999999f, 0.0f, 2f}, {-12.6f, 10.2f, 3f, 0f}, {21f, 6.6f, -1.5f, 0f}, {21f, 6.6f, 1.5f, 0f}, {21f, 6.6f, 0f, 0f}, {-2f, 1.8f, -5.2f, 0f}, {-2f, 1.8f, 5.2f, 0f}, {-15f, 6.6f, 0f, 0f}, {20f, 6.8999999999999995f, 2.8f, 0f}},
                new float[][]{}, 5.6f));
        put("buyan_corvette", new Geo(12f, 8.0f, new Vec3(9, 3.66, 0.0), new Vec3(10.08, 4.155, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{3f, 7.6f, 0f}, {8f, 4.6f, 0f}, {1f, 7.6f, 1f}},
                new float[][]{{13.5f, 4.155f, 0.0f, 2f}, {-10.6f, 4.26f, 0f, 0f}, {-8f, 4.6f, -1f, 0f}, {-8f, 4.6f, 1f, 0f}, {-8.5f, 4.6f, 0f, 0f}, {0f, 1.2f, -3.3f, 0f}, {0f, 1.2f, 3.3f, 0f}, {-8f, 4.6f, 0f, 0f}, {-8f, 4.6f, 0f, 0f}},
                new float[][]{}, 3.4f));
        put("arleigh_burke", new Geo(22f, 8.0f, new Vec3(25, 5.46, 0.0), new Vec3(26.32, 6.065, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{12f, 13.4f, 0f}, {22f, 6.4f, 0f}, {8f, 13.4f, 1.2f}, {-6f, 6.4f, 3f}},
                new float[][]{{30.5f, 6.065f, 0.0f, 2f}, {16.4f, 13.0f, 0f, 0f}, {-12f, 6.4f, -2.5f, 0f}, {-12f, 6.4f, 2.5f, 0f}, {20f, 6.4f, 0f, 0f}, {-5f, 1.8f, -4.8f, 0f}, {-5f, 1.8f, 4.8f, 0f}, {-18f, 6.4f, 0f, 0f}, {20f, 6.4f, 1.0f, 0f}},
                new float[][]{}, 5.2f));
        put("ticonderoga_cruiser", new Geo(26f, 8.5f, new Vec3(30, 5.859999999999999, 0.0), new Vec3(31.32, 6.465, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{14f, 13.8f, 0f}, {26f, 6.8f, 0f}, {10f, 13.8f, 1.2f}, {-10f, 6.8f, 3f}},
                new float[][]{{35.5f, 6.465f, 0.0f, 2f}, {17.4f, 13.4f, 0f, 0f}, {-16f, 6.8f, -3f, 0f}, {-16f, 6.8f, 3f, 0f}, {23f, 6.8f, 0f, 0f}, {-4f, 1.9f, -5.4f, 0f}, {-4f, 1.9f, 5.4f, 0f}, {-24f, 6.8f, 0f, 0f}, {24f, 6.8f, 1.0f, 0f}},
                new float[][]{}, 5.8f));
        put("zumwalt_destroyer", new Geo(26f, 8.0f, new Vec3(22, 6.06, 0.0), new Vec3(23.2, 6.609999999999999, 0.0), null, null, null,
                new float[][]{},
                new float[][]{{10f, 14.0f, 0f}, {20f, 7.0f, 0f}, {6f, 14.0f, 1.2f}, {-8f, 7.0f, 3f}},
                new float[][]{{27.0f, 6.609999999999999f, 0.0f, 2f}, {-12.6f, 15.6f, 0f, 0f}, {-20f, 7.0f, -5f, 0f}, {-20f, 7.0f, 5f, 0f}, {0f, 7.0f, 5f, 0f}, {-5f, 2f, -5.6f, 0f}, {-5f, 2f, 5.6f, 0f}, {-24f, 7.0f, 5f, 0f}, {-24f, 7.0f, 0f, 0f}},
                new float[][]{}, 6.0f));
        put("ohio_submarine", new Geo(25f, 8.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{12f, 8.2f, 0f}, {10f, 4.1f, 0f}, {8f, 4.1f, 0.8f}, {4f, 4.1f, -0.8f}},
                new float[][]{{30f, 3.6f, 0f, 0f}, {12f, 11.2f, 0f, 0f}, {-2f, 7.2f, 1f, 0f}, {-2f, 7.2f, -1f, 0f}, {-2f, 7.2f, 0f, 0f}, {33f, 2.6f, -1.2f, 0f}, {33f, 2.6f, 1.2f, 0f}, {12f, 11.2f, 0f, 0f}, {30f, 3.6f, 0f, 0f}},
                new float[][]{}, 3.6f));
        put("virginia_submarine", new Geo(18f, 8.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{11f, 6.199999999999999f, 0f}, {9f, 3.1999999999999997f, 0f}, {6f, 3.1999999999999997f, 0.6f}},
                new float[][]{{22f, 2.8f, 0f, 0f}, {11f, 8.6f, 0f, 0f}, {18f, 5.6f, 0.5f, 0f}, {18f, 5.6f, -0.5f, 0f}, {18f, 5.6f, 0f, 0f}, {24f, 1.9999999999999998f, -1.0f, 0f}, {24f, 1.9999999999999998f, 1.0f, 0f}, {11f, 8.6f, 0f, 0f}, {22f, 2.8f, 0f, 0f}},
                new float[][]{}, 2.8f));
        put("mercenary_rhib", new Geo(4f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{0.0f, 1.2f, 0f}, {2.6f, 1.2f, 0f}, {-1.4f, 1.2f, -0.7f}, {-1.4f, 1.2f, 0.7f}, {-2.6f, 1.2f, 0f}},
                new float[][]{{3.9f, 1.9f, 0f, 0f}},
                new float[][]{}, 1.87f));
        put("mercenary_interceptor_boat", new Geo(5f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{0.6f, 1.6f, 0f}, {3.6f, 1.6f, 0f}, {-2.4f, 1.6f, 0f}},
                new float[][]{{5.8f, 2.2f, 0f, 0f}, {-3.0f, 2.0f, -0.3f, 0f}, {-3.0f, 2.0f, 0.3f, 0f}},
                new float[][]{}, 1.8f));
        put("mercenary_explosive_usv", new Geo(3f, 8.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{-0.4f, 0.9f, 0f}},
                new float[][]{},
                new float[][]{}, 0.9f));
        put("mesa_locomotive", new Geo(3.5f, 7.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{-2.6f, 1.05f, -0.9f}},
                new float[][]{},
                new float[][]{}, 1.6f));
        put("mesa_wagon", new Geo(4.0f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{-3.9f, 1.05f, -0.9f}, {-3.9f, 1.05f, 0.9f}, {-2.5f, 1.05f, -0.9f}, {-2.5f, 1.05f, 0.9f}, {-1.7f, 1.05f, -0.9f}, {-1.7f, 1.05f, 0.9f}, {1.7f, 1.05f, -0.9f}, {1.7f, 1.05f, 0.9f}, {2.5f, 1.05f, -0.9f}, {2.5f, 1.05f, 0.9f}, {3.9f, 1.05f, -0.9f}, {3.9f, 1.05f, 0.9f}},
                new float[][]{},
                new float[][]{}, 1.6f));
        put("mesa_tank", new Geo(3.5f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{},
                new float[][]{},
                new float[][]{}, 1.45f));
        put("mesa_container", new Geo(3.5f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{},
                new float[][]{},
                new float[][]{}, 1.45f));
        put("mesa_armored", new Geo(3.5f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{},
                new float[][]{},
                new float[][]{}, 1.45f));
        put("tgv_power_car", new Geo(4.0f, 6.5f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{2.3f, 1.1f, -0.7f}},
                new float[][]{},
                new float[][]{}, 1.89f));
        put("tgv_coach", new Geo(3.5f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{-3.6f, 1.0f, -0.9f}, {-3.6f, 1.0f, 0.9f}, {-2.4f, 1.0f, -0.9f}, {-2.4f, 1.0f, 0.9f}, {2.4f, 1.0f, -0.9f}, {2.4f, 1.0f, 0.9f}, {3.6f, 1.0f, -0.9f}, {3.6f, 1.0f, 0.9f}},
                new float[][]{},
                new float[][]{}, 1.88f));
        put("tgv_bar_coach", new Geo(3.5f, 8.0f, null, null, null, null, null,
                new float[][]{},
                new float[][]{{-3.8f, 1.0f, -1.1f}, {-3.8f, 1.0f, 1.1f}, {3.8f, 1.0f, -1.1f}, {3.8f, 1.0f, 1.1f}, {-1.5f, 1.0f, -1.1f}, {-1.5f, 1.0f, 1.1f}, {1.5f, 1.0f, -1.1f}, {1.5f, 1.0f, 1.1f}},
                new float[][]{},
                new float[][]{}, 1.88f));
    }

    private MilGeometry() {}
}

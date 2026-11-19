package fr.modeminecraft.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.modeminecraft.ModeMinecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Géométrie des tourelles (points calculés par tools/generate_turrets.py, fichier assets/modeminecraft/turrets.json).
 * Lu directement dans le jar : le serveur s'en sert aussi (bouches des canons, place du tireur).
 *
 * Repère du modèle (pixels) : pivot de l'arme en (8, 8, 8), canon vers +X, flanc gauche vers -Z.
 */
public final class TurretGeometry {
    /** Bande de cartouches : courbe de Bézier from -> ctrl -> to (de la caisse à l'arme). */
    public record Belt(Vector3f from, Vector3f ctrl, Vector3f to, float spacing, int count) {}

    public record Geo(float pivot, Vector3f hipEye, Vector3f adsEye, List<Vector3f> grips, List<Vector3f> muzzles,
                      Belt belt, Vector3f coverHinge, Vector3f baseHead, Vector3f steam, List<String> parts) {}

    private static Map<String, Geo> data;

    public static Geo get(String name) {
        if (data == null) {
            data = load();
        }
        return data.get(name);
    }

    private static Vector3f vec(JsonObject o, String key) {
        if (!o.has(key)) {
            return null;
        }
        JsonArray a = o.getAsJsonArray(key);
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    private static List<Vector3f> vecs(JsonObject o, String key) {
        List<Vector3f> list = new ArrayList<>();
        if (o.has(key)) {
            for (var e : o.getAsJsonArray(key)) {
                JsonArray a = e.getAsJsonArray();
                list.add(new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()));
            }
        }
        return list;
    }

    private static Map<String, Geo> load() {
        Map<String, Geo> map = new HashMap<>();
        try (InputStream in = TurretGeometry.class.getResourceAsStream("/assets/modeminecraft/turrets.json")) {
            if (in == null) {
                ModeMinecraft.LOGGER.error("turrets.json introuvable");
                return map;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : root.entrySet()) {
                JsonObject o = entry.getValue().getAsJsonObject();
                Belt belt = null;
                if (o.has("belt")) {
                    JsonObject b = o.getAsJsonObject("belt");
                    belt = new Belt(vec(b, "from"), vec(b, "ctrl"), vec(b, "to"), b.get("spacing").getAsFloat(), b.get("count").getAsInt());
                }
                List<String> parts = new ArrayList<>();
                o.getAsJsonArray("parts").forEach(p -> parts.add(p.getAsString()));
                map.put(entry.getKey(), new Geo(o.get("pivot").getAsFloat(), vec(o, "hip_eye"), vec(o, "ads_eye"), vecs(o, "grips"),
                        vecs(o, "muzzles"), belt, vec(o, "cover_hinge"), vec(o, "base_head"), vec(o, "steam"), parts));
            }
        } catch (Exception e) {
            ModeMinecraft.LOGGER.error("Impossible de lire turrets.json", e);
        }
        return map;
    }

    /** Rotation de l'arme : d'abord la hauteur (autour de Z), puis la direction (autour de Y). */
    public static Quaternionf rotation(float yaw, float pitch) {
        return new Quaternionf().rotateY((float) Math.toRadians(-(yaw + 90))).rotateZ((float) Math.toRadians(-pitch));
    }

    /** Point du modèle (pixels) -> décalage en blocs depuis la position de l'entité. */
    public static Vec3 offset(float pivot, float yaw, float pitch, Vector3f px) {
        Vector3f v = new Vector3f(px).sub(8, 8, 8).div(16f);
        rotation(yaw, pitch).transform(v);
        return new Vec3(v.x, v.y + pivot, v.z);
    }

    private TurretGeometry() {}
}

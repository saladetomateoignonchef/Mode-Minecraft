package fr.modeminecraft.client.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.AttachmentSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Points de fixation des accessoires sur chaque arme, en coordonnées de modèle
 * (canon vers +X, arme centrée sur Z = 8).
 *
 * Ils sont calculés par tools/generate_gun_models.py à partir des formes de tools/gun_designs.py
 * et écrits dans assets/modeminecraft/gun_mounts.json, lu ici au chargement des ressources.
 */
public final class GunMounts {
    private static final ResourceLocation FILE = ModeMinecraft.id("gun_mounts.json");
    private static Map<String, Entry> data;

    private record Entry(Map<AttachmentSlot, Vector3f> mounts, boolean magNeedsAmmo) {}

    /** Position (en unités de bloc) où placer l'accessoire, ou null si l'arme n'a pas ce point. */
    public static Vector3f get(String gun, AttachmentSlot slot) {
        Entry e = data().get(gun);
        Vector3f p = e == null ? null : e.mounts.get(slot);
        return p == null ? null : new Vector3f(p).div(16f);
    }

    /** Le chargeur d'origine n'est affiché que si l'arme est chargée (roquette du RPG-7). */
    public static boolean magNeedsAmmo(String gun) {
        Entry e = data().get(gun);
        return e != null && e.magNeedsAmmo;
    }

    /** À appeler quand les ressources sont rechargées (F3 + T). */
    static void clear() {
        data = null;
    }

    private static Map<String, Entry> data() {
        if (data == null) {
            data = load();
        }
        return data;
    }

    private static Map<String, Entry> load() {
        Map<String, Entry> result = new HashMap<>();
        var resource = Minecraft.getInstance().getResourceManager().getResource(FILE);
        if (resource.isEmpty()) {
            ModeMinecraft.LOGGER.error("Fichier {} introuvable", FILE);
            return result;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var gun : root.entrySet()) {
                JsonObject obj = gun.getValue().getAsJsonObject();
                Map<AttachmentSlot, Vector3f> mounts = new EnumMap<>(AttachmentSlot.class);
                for (var m : obj.getAsJsonObject("mounts").entrySet()) {
                    var pos = m.getValue().getAsJsonArray();
                    AttachmentSlot slot = AttachmentSlot.valueOf(m.getKey().toUpperCase());
                    mounts.put(slot, new Vector3f(pos.get(0).getAsFloat(), pos.get(1).getAsFloat(), pos.get(2).getAsFloat()));
                }
                JsonElement flag = obj.get("mag_needs_ammo");
                result.put(gun.getKey(), new Entry(mounts, flag != null && flag.getAsBoolean()));
            }
        } catch (Exception e) {
            ModeMinecraft.LOGGER.error("Impossible de lire {}", FILE, e);
        }
        return result;
    }

    private GunMounts() {}
}

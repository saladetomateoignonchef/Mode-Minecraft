package fr.modeminecraft.client.model;

import fr.modeminecraft.item.gun.AttachmentSlot;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/**
 * Points de fixation des accessoires sur chaque arme, en coordonnées de modèle (comme dans Blockbench :
 * le canon pointe vers +X, l'arme est centrée sur Z = 8). Ils correspondent aux modèles de
 * tools/generate_gun_models.py : si tu modifies la forme d'une arme, ajuste ses points ici.
 */
public final class GunMounts {
    private static final Map<String, Map<AttachmentSlot, Vector3f>> MOUNTS = Map.of(
            "pistol", mounts(v(8, 11, 8), v(14.1f, 9.25f, 8), null, v(2.8f, 2.4f, 8)),
            "assault_rifle", mounts(v(6.5f, 10, 8), v(18.55f, 8, 8), v(12, 6.8f, 8), v(8.5f, 6.5f, 8)),
            "ak47", mounts(v(6.5f, 9.6f, 8), v(19.35f, 7.8f, 8), v(12.5f, 6.6f, 8), v(9, 6.5f, 8)),
            "m4", mounts(v(7, 10, 8), v(20.05f, 8.2f, 8), v(12.5f, 7, 8), v(8.5f, 6.3f, 8)),
            "famas", mounts(v(6, 12.3f, 8), v(18.45f, 7.8f, 8), v(12, 6.5f, 8), v(3.9f, 5.5f, 8)),
            "shotgun", mounts(v(5.5f, 10, 8), v(19.05f, 9.4f, 8), v(12, 6.8f, 8), null),
            "sniper_rifle", mounts(null, v(22.55f, 7.8f, 8), v(15, 7.3f, 8), null),
            "rocket_launcher", mounts(null, null, null, null),
            "rpg7", mounts(v(6.5f, 8.9f, 8), null, null, null));

    /** Position (en unités de bloc) où placer l'accessoire, ou null si l'arme n'a pas ce point. */
    public static Vector3f get(String gun, AttachmentSlot slot) {
        Map<AttachmentSlot, Vector3f> m = MOUNTS.get(gun);
        Vector3f p = m == null ? null : m.get(slot);
        return p == null ? null : new Vector3f(p).div(16f);
    }

    private static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    private static Map<AttachmentSlot, Vector3f> mounts(Vector3f sight, Vector3f muzzle, Vector3f grip, Vector3f magazine) {
        Map<AttachmentSlot, Vector3f> m = new EnumMap<>(AttachmentSlot.class);
        if (sight != null) m.put(AttachmentSlot.SIGHT, sight);
        if (muzzle != null) m.put(AttachmentSlot.MUZZLE, muzzle);
        if (grip != null) m.put(AttachmentSlot.GRIP, grip);
        if (magazine != null) m.put(AttachmentSlot.MAGAZINE, magazine);
        return m;
    }

    private GunMounts() {}
}

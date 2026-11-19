package fr.modeminecraft.item.gun;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mémorise quand chaque joueur a tiré pour la dernière fois (côté client), afin d'animer
 * la culasse qui recule. Le joueur local est enregistré directement au moment du tir ;
 * pour les autres joueurs, on détecte le tir quand leurs munitions diminuent.
 */
public final class GunAnimations {
    private static final Map<Integer, Long> LAST_SHOT = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> LAST_AMMO = new ConcurrentHashMap<>();

    /** Appelé côté client quand le joueur local tire. */
    public static void recordShot(Entity shooter) {
        LAST_SHOT.put(shooter.getId(), shooter.level().getGameTime());
    }

    /** Appelé à l'affichage de l'arme d'un autre joueur : ses munitions ont baissé = il vient de tirer. */
    public static void observeAmmo(Entity holder, ItemStack stack) {
        long key = ((long) holder.getId() << 32) | BuiltInRegistries.ITEM.getId(stack.getItem());
        int ammo = GunItem.getAmmo(stack);
        Integer previous = LAST_AMMO.put(key, ammo);
        if (previous != null && ammo < previous) {
            recordShot(holder);
        }
    }

    /** Temps écoulé depuis le dernier tir, en ticks (avec la fraction de tick pour une animation fluide). */
    public static float ticksSinceShot(Entity holder, float partialTick) {
        Long tick = LAST_SHOT.get(holder.getId());
        return tick == null ? Float.MAX_VALUE : holder.level().getGameTime() - tick + partialTick;
    }

    /**
     * Position de la pièce mobile entre 0 (repos) et 1 (complètement en arrière).
     * fast : aller-retour très rapide (culasse d'arme semi-auto / automatique)
     * pump : la pompe recule puis revient juste après le tir
     * bolt : le tireur manœuvre le verrou (plus lent)
     */
    public static float progress(String style, float t) {
        return switch (style) {
            case "fast" -> t < 1 ? t : (t < 3 ? 1 - (t - 1) / 2 : 0);
            case "pump" -> t < 2 ? 0 : (t < 6 ? (t - 2) / 4 : (t < 10 ? 1 - (t - 6) / 4 : 0));
            case "bolt" -> t < 3 ? 0 : (t < 8 ? (t - 3) / 5 : (t < 13 ? 1 - (t - 8) / 5 : 0));
            default -> 0;
        };
    }

    /**
     * Le personnage tire la culasse (après un rechargement à vide, ou si la chambre est vide) :
     * la pièce mobile part en arrière, reste un instant, puis revient d'un coup.
     */
    public static float rackProgress(String style, float t) {
        return switch (style) {
            case "pump" -> progress("pump", t + 2);
            case "bolt" -> progress("bolt", t + 3);
            default -> t < 0 ? 0 : (t < 3 ? t / 3 : (t < 6 ? 1 : (t < 8 ? 1 - (t - 6) / 2 : 0)));
        };
    }

    /** Vérification de la chambre : on entrouvre la culasse (un tiers de sa course). */
    public static float checkProgress(float t) {
        return t < 0 ? 0 : (t < 3 ? 0.35f * t / 3 : (t < 8 ? 0.35f : (t < 11 ? 0.35f * (1 - (t - 8) / 3) : 0)));
    }

    /** Le chargeur n'est pas dans l'arme (entre le retrait de l'ancien et l'arrivée du nouveau). */
    public static final float MAG_HIDDEN = Float.NaN;

    /**
     * Rechargement : décalage du chargeur vers le bas (en pixels du modèle), ou MAG_HIDDEN.
     * 0-25 % : l'ancien chargeur sort ; 25-55 % : la main va chercher le nouveau ; 55-85 % : il entre.
     */
    public static float reloadMagOffset(ItemStack stack, float now) {
        if (!(stack.getItem() instanceof GunItem gun) || gun.stats().reloadPerRound()) {
            return 0;
        }
        Long start = stack.get(fr.modeminecraft.registry.ModDataComponents.RELOAD_START.get());
        Long end = stack.get(fr.modeminecraft.registry.ModDataComponents.RELOAD_END.get());
        if (start == null || end == null || now < start || now > end) {
            return 0;
        }
        Long rack = stack.get(fr.modeminecraft.registry.ModDataComponents.RACK_AT.get());
        long magEnd = rack != null && rack > start && rack <= end ? rack : end;
        float f = (now - start) / Math.max(1, magEnd - start);
        if (f < 0.25f) {
            return 12 * f / 0.25f;
        } else if (f < 0.55f) {
            return MAG_HIDDEN;
        } else if (f < 0.85f) {
            return 12 * (1 - (f - 0.55f) / 0.3f);
        }
        return 0;
    }

    private GunAnimations() {}
}

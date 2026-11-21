package fr.modeminecraft.nuclear;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModDamageTypes;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Zones radioactives persistantes (cahier des charges « Bombe nucléaire » §2.3.C) : rayon 160 blocs autour du point
 * zéro, pendant exactement 7 jours Minecraft (168 000 ticks). Sauvegardées dans data/modeminecraft_radiation_zones.dat
 * (le décompte survit aux redémarrages). Une fois par seconde : exposition de toutes les entités vivantes, intensité
 * 1 - d / 160 ; sans tenue anti-radiation complète, nausée, flou, faiblesse et 2 à 6 PV par seconde (mort rapide au
 * centre). La liste des zones est envoyée aux clients (ciel verdâtre, cendres, compteur Geiger).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public class RadiationZones extends SavedData {
    public static final long DURATION = 168_000L;
    public static final float RADIUS = 160f;

    public record Zone(BlockPos center, float radius, long expire) {
        /** Intensité (0..1) en un point : maximale au point zéro, nulle au bord. */
        public double intensity(double x, double y, double z) {
            double dx = x - center.getX() - 0.5, dz = z - center.getZ() - 0.5, dy = Math.max(0, Math.abs(y - center.getY()) - 40);
            double d = Math.sqrt(dx * dx + dz * dz + dy * dy);
            return d >= radius ? 0 : 1 - d / radius;
        }

        public double distance(double x, double z) {
            return Math.hypot(x - center.getX() - 0.5, z - center.getZ() - 0.5);
        }
    }

    private final List<Zone> zones = new ArrayList<>();

    public static RadiationZones get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(RadiationZones::new, RadiationZones::load, null),
                "modeminecraft_radiation_zones");
    }

    public List<Zone> zones() {
        return zones;
    }

    public static void add(ServerLevel level, BlockPos center) {
        RadiationZones z = get(level);
        z.zones.add(new Zone(center, RADIUS, level.getGameTime() + DURATION));
        z.setDirty();
        sync(level, null);
    }

    private static RadiationZones load(CompoundTag tag, HolderLookup.Provider registries) {
        RadiationZones z = new RadiationZones();
        for (Tag t : tag.getList("Zones", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            z.zones.add(new Zone(new BlockPos(c.getInt("X"), c.getInt("Y"), c.getInt("Z")), c.getFloat("Radius"), c.getLong("Expire")));
        }
        return z;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Zone zone : zones) {
            CompoundTag c = new CompoundTag();
            c.putInt("X", zone.center.getX());
            c.putInt("Y", zone.center.getY());
            c.putInt("Z", zone.center.getZ());
            c.putFloat("Radius", zone.radius);
            c.putLong("Expire", zone.expire);
            list.add(c);
        }
        tag.put("Zones", list);
        return tag;
    }

    /** Envoie la liste des zones à un joueur (ou à tous). */
    public static void sync(ServerLevel level, ServerPlayer only) {
        List<RadiationPayload.Entry> list = new ArrayList<>();
        for (Zone z : get(level).zones) {
            list.add(new RadiationPayload.Entry(z.center.getX(), z.center.getY(), z.center.getZ(), z.radius, z.expire));
        }
        RadiationPayload p = new RadiationPayload(list, level.getGameTime());
        if (only != null) {
            PacketDistributor.sendToPlayer(only, p);
        } else {
            PacketDistributor.sendToPlayersInDimension(level, p);
        }
    }

    /** Intensité maximale des zones en un point (serveur). */
    public static double intensityAt(ServerLevel level, double x, double y, double z) {
        double best = 0;
        for (Zone zone : get(level).zones) {
            best = Math.max(best, zone.intensity(x, y, z));
        }
        return best;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.level() instanceof ServerLevel sl) {
            sync(sl, sp);
        }
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.level() instanceof ServerLevel sl) {
            sync(sl, sp);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        if (now % 20 != 0) {
            return;
        }
        RadiationZones data = get(level);
        if (data.zones.isEmpty()) {
            return;
        }
        if (data.zones.removeIf(z -> now >= z.expire)) {
            data.setDirty();
            sync(level, null);                                                  // fin de la semaine : la zone est saine
        }
        if (now % 600 == 0) {
            sync(level, null);
        }
        for (Zone zone : data.zones) {
            BlockPos c = zone.center;
            AABB box = new AABB(c).inflate(zone.radius, zone.radius, zone.radius);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
                double k = zone.intensity(e.getX(), e.getY(), e.getZ());
                if (k > 0) {
                    expose(level, e, k);
                }
            }
        }
    }

    /** Exposition d'une seconde à l'intensité k (0..1). */
    static void expose(ServerLevel level, LivingEntity e, double k) {
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        boolean protectedSuit = RadiationSuitItem.fullyProtected(e);
        if (protectedSuit) {
            RadiationSuitItem.wear(e, k > 0.7 ? 2 : 1);                        // saturation de la tenue
            if (k > 0.6 && RadiationSuitItem.nbc(e) && e instanceof Player p) {
                // tenue NBC au cœur du cratère : une cartouche filtrante par minute
                long last = p.getPersistentData().getLong("modeminecraft:nbc_filter");
                if (level.getGameTime() - last >= 1200) {
                    var inv = p.getInventory();
                    int slot = -1;
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        if (inv.getItem(i).is(ModItems.NBC_FILTER.get())) {
                            slot = i;
                            break;
                        }
                    }
                    if (slot >= 0) {
                        inv.getItem(slot).shrink(1);
                        p.getPersistentData().putLong("modeminecraft:nbc_filter", level.getGameTime());
                        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.modeminecraft.nbc_filter_used")
                                .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
                    } else {
                        protectedSuit = false;                                     // filtres épuisés : le masque laisse passer
                        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.modeminecraft.nbc_no_filter")
                                .withStyle(net.minecraft.ChatFormatting.RED), true);
                    }
                }
            }
            if (protectedSuit) {
                return;
            }
        }
        float dmg = k > 0.85 ? 12f : (float) (2 + 4 * k);                        // 2 à 6 PV / s, mortel au centre
        e.hurt(level.damageSources().source(ModDamageTypes.RADIATION), dmg);
        if (k > 0.15) {
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false));
        }
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, k > 0.5 ? 1 : 0, false, false));
        e.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 1, false, false));
        if (k > 0.45) {
            e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));       // vision qui se brouille
        }
    }
}

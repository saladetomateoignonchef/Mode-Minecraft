package fr.modeminecraft.item;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.ProneHandler;
import fr.modeminecraft.registry.ModDamageTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Effets passifs des pantalons et rangers qui dépendent de la situation :
 * - bonus de sprint, de reptation (allongé) et de vitesse en forêt / jungle / marais (Panama) ;
 * - faim : la dépense d'énergie (exhaustion) est réduite, au total -75 % au plus (voir PlayerExhaustionMixin) ;
 * - détection par les monstres (Infiltrator, Salomon), pas qui ne déclenchent pas les capteurs sculk (Salomon) ;
 * - explosions au sol (bottes EOD), explosions et éclats aux jambes (Ratnik, SWAT), froid (pantalon 14-18) ;
 * - bonus d'ensemble : casque + gilet + pantalon + rangers de la même époque = +10 % de régénération.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class GearEffects {
    private static final ResourceLocation SPRINT = ModeMinecraft.id("gear_sprint");
    private static final ResourceLocation PRONE = ModeMinecraft.id("gear_prone");
    private static final ResourceLocation BIOME = ModeMinecraft.id("gear_biome");
    /** Part des dégâts d'une explosion qui touche les jambes. */
    private static final float LEG_SHARE = 0.45f;

    private static GearItem.Stats stats(LivingEntity e, EquipmentSlot slot) {
        GearItem g = GearItem.worn(e, slot);
        return g == null ? null : g.stats();
    }

    private static double sum(LivingEntity e, java.util.function.ToDoubleFunction<GearItem.Stats> f) {
        double v = 0;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            GearItem.Stats st = stats(e, s);
            if (st != null) {
                v += f.applyAsDouble(st);
            }
        }
        return v;
    }

    /** Multiplicateur de la dépense d'énergie (faim) : -75 % au plus avec pantalon + rangers. */
    public static float foodMul(Player p) {
        return 1 - (float) Math.min(0.75, sum(p, s -> s.food));
    }

    /** Rechargement plus rapide grâce aux poches (pantalon M42). */
    public static float reloadBonus(Player p) {
        GearItem.Stats s = stats(p, EquipmentSlot.LEGS);
        return s == null ? 0 : s.reload;
    }

    /** Dégainage des pistolets plus rapide (holster de cuisse du pantalon SWAT). */
    public static float drawMul(Player p) {
        GearItem.Stats s = stats(p, EquipmentSlot.LEGS);
        return s == null ? 1 : 1 - s.draw;
    }

    public static boolean stealthy(LivingEntity e) {
        GearItem.Stats s = stats(e, EquipmentSlot.FEET);
        return s != null && s.stealth;
    }

    /** Les 4 pièces (casque, gilet, pantalon, rangers) sont de la même époque. */
    public static boolean setBonus(LivingEntity e) {
        int[] tiers = new int[4];
        ItemStack head = e.getItemBySlot(EquipmentSlot.HEAD), chest = e.getItemBySlot(EquipmentSlot.CHEST);
        if (!(head.getItem() instanceof HelmetItem h) || !(chest.getItem() instanceof VestItem v)) {
            return false;
        }
        GearItem.Stats legs = stats(e, EquipmentSlot.LEGS), feet = stats(e, EquipmentSlot.FEET);
        if (legs == null || feet == null) {
            return false;
        }
        tiers[0] = h.stats().tier();
        tiers[1] = v.stats().tier();
        tiers[2] = legs.tier;
        tiers[3] = feet.tier;
        return tiers[0] == tiers[1] && tiers[1] == tiers[2] && tiers[2] == tiers[3];
    }

    private static void transientSpeed(Player p, ResourceLocation id, double value) {
        var attr = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) {
            return;
        }
        if (value == 0) {
            if (attr.hasModifier(id)) {
                attr.removeModifier(id);
            }
        } else {
            AttributeModifier current = attr.getModifier(id);
            if (current == null || current.amount() != value) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        transientSpeed(p, SPRINT, p.isSprinting() ? sum(p, s -> s.sprint) : 0);
        transientSpeed(p, PRONE, ProneHandler.isProne(p) && !p.isInWater() ? sum(p, s -> s.prone) : 0);
        double biome = 0;
        if (p.tickCount % 10 == 0 || p.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(BIOME)) {
            var b = p.level().getBiome(p.blockPosition());
            boolean wild = b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_JUNGLE) || b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP);
            biome = wild ? sum(p, s -> s.biomeSpeed) : 0;
            transientSpeed(p, BIOME, biome);
        }
    }

    /** Monstres : ils repèrent moins vite (Infiltrator -50 %, Salomon -30 %). */
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        double d = Math.min(0.8, sum(event.getEntity(), s -> s.detection));
        if (d > 0) {
            event.modifyVisibility(1 - d);
        }
    }

    /** Bottes furtives : marcher ne réveille pas les capteurs sculk ni le Warden (sprinter, si). */
    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (event.getCause() instanceof LivingEntity e && stealthy(e) && !e.isSprinting()
                && (event.getVanillaEvent().is(GameEvent.STEP) || event.getVanillaEvent().is(GameEvent.HIT_GROUND))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        var source = event.getSource();
        GearItem.Stats legs = stats(e, EquipmentSlot.LEGS), feet = stats(e, EquipmentSlot.FEET);
        if (legs == null && feet == null) {
            return;
        }
        if (source.is(DamageTypes.FREEZE) && legs != null && legs.coldImmune) {
            event.setCanceled(true); // drap de laine épais : le froid ne passe pas
            return;
        }
        float r = 0;
        boolean explosion = source.is(DamageTypeTags.IS_EXPLOSION);
        boolean shrapnel = source.is(ModDamageTypes.BULLET) && !(source.getDirectEntity() instanceof fr.modeminecraft.entity.BulletEntity);
        if (explosion && feet != null && feet.groundBlast > 0) {
            var pos = source.getSourcePosition();
            if (pos != null && pos.y <= e.getY() + 1.0) {
                r = Math.max(r, feet.groundBlast); // mine, Claymore, grenade au sol : les semelles dévient le souffle
            }
        }
        if (legs != null && legs.legExplosion > 0 && (explosion || shrapnel)) {
            r = 1 - (1 - r) * (1 - legs.legExplosion * LEG_SHARE);
        }
        if (r > 0) {
            event.setAmount(event.getAmount() * (1 - r));
        }
    }

    /** Bonus d'ensemble : +10 % de régénération. */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (setBonus(event.getEntity())) {
            event.setAmount(event.getAmount() * 1.10f);
        }
    }

    private GearEffects() {}
}

package fr.modeminecraft.item.melee;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Règles du combat au corps à corps (combos, parade, coup dans le dos, capacités des armes). */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class MeleeEvents {
    /** Combo en cours : n° du coup (0, 1, 2) et instant du dernier coup. */
    private record Combo(int index, long time) {}

    // Un tableau par côté (client / serveur), car en solo les deux tournent dans le même programme
    private static final Map<UUID, Combo> COMBOS = new ConcurrentHashMap<>();
    private static final Map<UUID, Combo> COMBOS_CLIENT = new ConcurrentHashMap<>();

    private static Map<UUID, Combo> combos(Player p) {
        return p.level().isClientSide ? COMBOS_CLIENT : COMBOS;
    }
    private static final int COMBO_WINDOW = 30; // 1,5 s pour enchaîner

    /** N° du coup en cours dans le combo (pour les dégâts et l'animation). */
    public static int comboIndex(Player player) {
        Combo c = combos(player).get(player.getUUID());
        return c == null ? 0 : c.index();
    }

    /** Côté client : on avance le combo à chaque clic (animation des 3 coups différents). */
    public static void advanceCombo(Player player) {
        long now = player.level().getGameTime();
        Combo c = combos(player).get(player.getUUID());
        int next = c != null && now - c.time() <= COMBO_WINDOW ? (c.index() + 1) % 3 : 0;
        combos(player).put(player.getUUID(), new Combo(next, now));
    }

    private static MeleeWeaponItem weapon(Entity e) {
        return e instanceof LivingEntity l && l.getMainHandItem().getItem() instanceof MeleeWeaponItem m ? m : null;
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        MeleeWeaponItem w = weapon(player);
        if (w == null || player.level().isClientSide) {
            return;
        }
        advanceCombo(player);
        // Claymore : le balayage touche jusqu'à 5 ennemis devant soi
        if (w.stats().ability() == MeleeWeaponItem.Ability.WIDE_SWEEP && player.getAttackStrengthScale(0.5f) > 0.8f) {
            Vec3 look = player.getLookAngle();
            int hits = 0;
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.5),
                    e -> e != player && e != event.getTarget() && e.isAlive() && !e.isAlliedTo(player))) {
                Vec3 to = e.position().subtract(player.position());
                if (hits < 4 && to.length() < 4.5 && to.normalize().dot(look) > 0.4) {
                    e.hurt(player.damageSources().playerAttack(player), w.stats().damage() * 0.6f);
                    e.knockback(0.4, player.getX() - e.getX(), player.getZ() - e.getZ());
                    hits++;
                }
            }
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + look.x * 2,
                    player.getY() + 1, player.getZ() + look.z * 2, 3, 0.8, 0.1, 0.8, 0);
        }
    }

    /** Rapière : pas de balayage ; cimeterre : coups critiques dès qu'on saute. */
    @SubscribeEvent
    public static void onCrit(CriticalHitEvent event) {
        MeleeWeaponItem w = weapon(event.getEntity());
        if (w == null) {
            return;
        }
        var a = w.stats().ability();
        if (a == MeleeWeaponItem.Ability.THRUST_PIERCE || a == MeleeWeaponItem.Ability.WIDE_SWEEP || a == MeleeWeaponItem.Ability.CHARGE) {
            event.setDisableSweep(true);
        }
        if (a == MeleeWeaponItem.Ability.JUMP_CRITS && !event.getEntity().onGround()) {
            event.setCriticalHit(true);
            event.setDamageMultiplier(1.6f);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        var source = event.getSource();
        Entity attacker = source.getEntity();

        // ---------- Parade du défenseur ----------
        if (target instanceof Player defender && defender.isUsingItem() && defender.getUseItem().getItem() instanceof MeleeWeaponItem parry
                && attacker != null && source.getDirectEntity() == attacker && inFront(defender, attacker)) {
            if (defender.getTicksUsingItem() <= parry.perfectParryTicks()) {
                // Parade parfaite : aucun dégât, l'attaquant est étourdi 1 s
                event.setCanceled(true);
                if (attacker instanceof LivingEntity la) {
                    la.addEffect(new MobEffectInstance(ModEffects.STUNNED, 20), defender);
                }
                defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.ANVIL_PLACE,
                        SoundSource.PLAYERS, 0.8f, 1.8f);
                if (defender.level() instanceof ServerLevel sl) {
                    Vec3 p = defender.getEyePosition().add(defender.getLookAngle().scale(0.8));
                    sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 12, 0.2, 0.2, 0.2, 0.3);
                }
                return;
            }
            event.setAmount(event.getAmount() * 0.4f); // -60 %
            defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 0.8f, 1.3f);
        }

        // ---------- Attaque au corps à corps avec une arme du mod ----------
        if (!(attacker instanceof Player player) || source.getDirectEntity() != player) {
            return;
        }
        MeleeWeaponItem w = weapon(player);
        if (w == null) {
            return;
        }
        MeleeWeaponItem.Stats s = w.stats();
        float amount = event.getAmount();
        if (comboIndex(player) == 2) {
            amount *= 1.25f; // 3e coup du combo
        }
        if (s.light() && isBehind(target, player)) {
            // coup dans le dos (le Stahlhelm protège aussi la nuque : bonus divisé par deux)
            boolean neckGuard = target.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(fr.modeminecraft.registry.ModItems.STAHLHELM_M40.get());
            amount *= neckGuard ? 1 + (s.backstab() - 1) / 2 : s.backstab();
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getEyeY(), target.getZ(), 6, 0.2, 0.2, 0.2, 0.1);
            }
        }
        switch (s.ability()) {
            case ANTI_UNDEAD -> { if (target.getType().is(EntityTypeTags.UNDEAD)) amount *= 1.5f; }
            case CHARGE -> { if (player.isSprinting() || player.isPassenger()) amount *= 1.5f; }
            case WATER -> { if (player.isInWater()) amount *= 1.3f; }
            case STUN -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3), player);
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 50), player);
                target.addEffect(new MobEffectInstance(ModEffects.STUNNED, 50), player); // empêche aussi les Creepers d'exploser
            }
            case BLEED -> target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 81), player);
            case STUN_CHANCE -> { if (player.getRandom().nextFloat() < 0.25f) target.addEffect(new MobEffectInstance(ModEffects.STUNNED, 20), player); }
            case DISMOUNT -> { if (target.isPassenger()) target.stopRiding(); }
            case PULL -> {
                Vec3 pull = player.position().subtract(target.position()).normalize().scale(0.45);
                target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.05, pull.z));
                target.hurtMarked = true;
            }
            case ARMOR_BREAK -> {
                Vec3 push = target.position().subtract(player.position()).normalize();
                target.setDeltaMovement(target.getDeltaMovement().add(push.x * 0.9, 0.3, push.z * 0.9));
                target.hurtMarked = true;
            }
            default -> { }
        }
        event.setAmount(amount);
        if (s.armorPen() > 0) {
            float keep = 1 - s.armorPen();
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * keep);
        }
    }

    /** Parade : on dévie les projectiles lents (flèches, boules de neige...) qui arrivent de face. */
    @SubscribeEvent
    public static void onProjectile(ProjectileImpactEvent event) {
        Projectile p = event.getProjectile();
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof Player defender)) {
            return;
        }
        if (defender.isUsingItem() && defender.getUseItem().getItem() instanceof MeleeWeaponItem
                && p.getDeltaMovement().length() < 3.5 && !(p instanceof fr.modeminecraft.entity.BulletEntity) && inFront(defender, p)) {
            event.setCanceled(true);
            p.setDeltaMovement(p.getDeltaMovement().scale(-0.7));
            p.setOwner(defender);
            defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.ANVIL_LAND,
                    SoundSource.PLAYERS, 0.5f, 2.0f);
        }
    }

    /** Sabre d'abordage : on nage plus vite. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        MeleeWeaponItem w = weapon(player);
        if (w != null && w.stats().ability() == MeleeWeaponItem.Ability.WATER && player.isInWater() && player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, false, false, true));
        }
    }

    /** Marteau de guerre : le bouclier de la cible est brisé (désactivé 5 s). */
    public static void breakShield(LivingEntity e) {
        if (e instanceof Player p && p.isBlocking()) {
            p.getCooldowns().addCooldown(Items.SHIELD, 100);
            p.stopUsingItem();
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    private static boolean inFront(LivingEntity defender, Entity from) {
        Vec3 look = defender.getViewVector(1.0f);
        Vec3 to = from.position().subtract(defender.position());
        return new Vec3(look.x, 0, look.z).normalize().dot(new Vec3(to.x, 0, to.z).normalize()) > 0.2;
    }

    /** L'attaquant est-il dans le cône de 90° derrière la cible ? */
    private static boolean isBehind(LivingEntity target, Entity attacker) {
        float yaw = (float) Math.toRadians(target.yBodyRot);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 to = attacker.position().subtract(target.position());
        return facing.dot(new Vec3(to.x, 0, to.z).normalize()) < -0.707;
    }

    private MeleeEvents() {}
}

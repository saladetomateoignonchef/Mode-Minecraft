package fr.modeminecraft.item.explosive;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.AreaCloudEntity;
import fr.modeminecraft.entity.ThrownGrenadeEntity;
import fr.modeminecraft.registry.ModDamageTypes;
import fr.modeminecraft.registry.ModEffects;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Effets des grenades et explosifs (cahier des charges, partie 3). */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class Detonations {
    /** Grenade à impulsion : le lanceur ne prend pas de dégâts de chute pendant 10 s. */
    private static final Map<UUID, Long> NO_FALL = new ConcurrentHashMap<>();

    /**
     * Fait exploser la grenade. Renvoie > 0 si elle reste active ensuite (leurre, détection) pendant ce nombre de ticks.
     */
    public static int detonate(ThrownGrenadeEntity g, GrenadeItem.Kind kind) {
        if (!(g.level() instanceof ServerLevel level)) {
            return 0;
        }
        Vec3 p = g.position();
        Entity owner = g.getOwner();
        switch (kind) {
            case FRAG -> {
                level.explode(g, p.x, p.y, p.z, 2.2f, Level.ExplosionInteraction.NONE);
                shrapnel(level, p, owner, 45, null, -1, 12, 4, 6);
                breakGlass(level, BlockPos.containing(p), 4);
            }
            case OFFENSIVE -> {
                // Onde de choc surpuissante : tue même en Netherite à bout portant, recul x3, pas d'éclats
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                level.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4f, 0.7f);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(5))) {
                    double d = e.position().distanceTo(p);
                    if (d < 5) {
                        e.hurt(source(level, g, owner, true), (float) (40 * (1 - d / 5)));
                        Vec3 push = e.position().subtract(p).normalize().scale(3 * (1 - d / 5));
                        e.push(push.x, 0.6 + push.y * 0.5, push.z);
                        e.hurtMarked = true;
                    }
                }
            }
            case SEMTEX -> level.explode(g, p.x, p.y, p.z, 2.6f, Level.ExplosionInteraction.NONE);
            case MOLOTOV -> {
                level.playSound(null, p.x, p.y, p.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 0.9f);
                level.addFreshEntity(new AreaCloudEntity(level, AreaCloudEntity.Type.FIRE, p, 4.5f, 200, owner));
            }
            case CLUSTER -> {
                level.explode(g, p.x, p.y, p.z, 1.2f, Level.ExplosionInteraction.NONE);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    var b = new ThrownGrenadeEntity(level, owner instanceof LivingEntity l ? l : null, GrenadeItem.Kind.BOMBLET, 24);
                    b.setPos(p.add(0, 0.3, 0));
                    b.setDeltaMovement(Math.cos(a) * 0.45, 0.45, Math.sin(a) * 0.45);
                    level.addFreshEntity(b);
                }
            }
            case BOMBLET -> {
                level.explode(g, p.x, p.y, p.z, 1.4f, Level.ExplosionInteraction.NONE);
                shrapnel(level, p, owner, 10, null, -1, 5, 2, 4);
            }
            case FLASHBANG -> flashbang(level, g, p);
            case SMOKE -> level.addFreshEntity(new AreaCloudEntity(level, AreaCloudEntity.Type.SMOKE, p, 6, 400, owner));
            case GAS -> level.addFreshEntity(new AreaCloudEntity(level, AreaCloudEntity.Type.GAS, p, 5, 300, owner));
            case EMP -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.5, p.z, 120, 3, 2, 3, 0.4);
                level.playSound(null, p.x, p.y, p.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2f, 0.5f);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(10))) {
                    if (e.position().distanceTo(p) < 10) {
                        e.addEffect(new MobEffectInstance(ModEffects.EMP, 200)); // viseurs électroniques coupés 10 s
                        if (e instanceof IronGolem) {
                            e.hurt(source(level, g, owner, true), 15);
                        }
                    }
                }
            }
            case DECOY -> {
                return 240; // 12 s de faux tirs
            }
            case IMPULSE -> {
                level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y + 0.5, p.z, 1, 0, 0, 0, 0);
                level.playSound(null, p.x, p.y, p.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 1.4f);
                for (Entity e : level.getEntities(g, new AABB(p, p).inflate(6), e -> e instanceof LivingEntity)) {
                    Vec3 push = e.position().subtract(p).normalize();
                    e.setDeltaMovement(push.x * 0.8, 1.35, push.z * 0.8); // 8 à 10 blocs de haut, sans dégâts
                    e.hurtMarked = true;
                    if (e instanceof Player pl) {
                        NO_FALL.put(pl.getUUID(), level.getGameTime() + 200);
                    }
                }
                if (owner instanceof Player pl) {
                    NO_FALL.put(pl.getUUID(), level.getGameTime() + 200);
                }
            }
            case SENSOR -> {
                return 200; // impulsions sonar pendant 10 s
            }
        }
        return 0;
    }

    /** Leurre / détection : appelé à chaque tick tant qu'ils sont actifs. */
    public static boolean tickActive(ThrownGrenadeEntity g, GrenadeItem.Kind kind, int left) {
        if (!(g.level() instanceof ServerLevel level)) {
            return true;
        }
        Vec3 p = g.position();
        if (kind == GrenadeItem.Kind.DECOY) {
            // Faux coups de feu (rafales irrégulières) et flashs ; les monstres à 30 blocs viennent voir
            if (level.random.nextFloat() < 0.35f) {
                level.playSound(null, p.x, p.y, p.z, ModSounds.SHOT_RIFLE.get(), SoundSource.PLAYERS, 5f, 0.95f + level.random.nextFloat() * 0.1f);
                level.sendParticles(ParticleTypes.FLASH, p.x, p.y + 0.3, p.z, 1, 0, 0, 0, 0);
            }
            if (left % 20 == 0) {
                for (Mob m : level.getEntitiesOfClass(Monster.class, new AABB(p, p).inflate(30))) {
                    m.setTarget(null);
                    m.getNavigation().moveTo(p.x, p.y, p.z, 1.1);
                }
            }
        } else if (kind == GrenadeItem.Kind.SENSOR && left % 40 == 0) {
            // Impulsion sonar : les créatures à 25 blocs brillent à travers les murs
            level.playSound(null, p.x, p.y, p.z, ModSounds.SONAR.get(), SoundSource.PLAYERS, 1.5f, 1.0f);
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, p.x, p.y + 0.2, p.z, 20, 0.5, 0.5, 0.5, 0.05);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(25))) {
                if (e != g.getOwner() && e.position().distanceTo(p) < 25) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 45));
                }
            }
        }
        return true;
    }

    /** Flashbang : écran blanc + acouphène pour les joueurs qui regardent ; les monstres sont étourdis 5 s. */
    private static void flashbang(ServerLevel level, Entity g, Vec3 p) {
        level.sendParticles(ParticleTypes.FLASH, p.x, p.y + 0.3, p.z, 3, 0, 0, 0, 0);
        level.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3f, 1.8f);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(20))) {
            double d = e.getEyePosition().distanceTo(p);
            boolean visible = level.clip(new ClipContext(e.getEyePosition(), p.add(0, 0.2, 0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS;
            if (!visible || d > 20) {
                continue;
            }
            if (e instanceof ServerPlayer player) {
                double facing = player.getLookAngle().dot(p.subtract(player.getEyePosition()).normalize());
                float strength = (float) Math.max(0, Math.min(1, (1.2 - d / 20) * (facing > 0.3 ? 1 : 0.35)));
                PacketDistributor.sendToPlayer(player, new fr.modeminecraft.network.FlashPayload(strength));
            } else if (d < 10) {
                e.addEffect(new MobEffectInstance(ModEffects.STUNNED, 100));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100));
            }
        }
    }

    /**
     * Éclats : rayons aléatoires (dans une sphère, ou dans un cône autour de dir) jusqu'à range blocs ;
     * chaque éclat blesse la première créature touchée (entre dmgMin et dmgMax). Le lanceur n'est jamais touché.
     */
    public static void shrapnel(ServerLevel level, Vec3 origin, Entity owner, int count, Vec3 dir, double coneCos,
                                double range, float dmgMin, float dmgMax) {
        RandomSource r = level.random;
        DamageSource src = level.damageSources().source(ModDamageTypes.BULLET, owner, owner);
        for (int i = 0; i < count; i++) {
            Vec3 d;
            do {
                d = new Vec3(r.nextGaussian(), r.nextGaussian() * 0.6, r.nextGaussian()).normalize();
            } while (dir != null && d.dot(dir) < coneCos);
            Vec3 from = origin.add(0, 0.3, 0);
            Vec3 to = from.add(d.scale(range));
            var block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
            Vec3 end = block.getLocation();
            var hit = ProjectileUtil.getEntityHitResult(level, owner, from, end, new AABB(from, end).inflate(0.5),
                    e -> e instanceof LivingEntity && e != owner && e.isAlive(), 0.2f);
            Vec3 stop = hit != null ? hit.getLocation() : end;
            if (hit != null) {
                hit.getEntity().invulnerableTime = 0;
                hit.getEntity().hurt(src, dmgMin + r.nextFloat() * (dmgMax - dmgMin));
            }
            for (int k = 1; k <= 3; k++) {
                Vec3 q = from.lerp(stop, k / 4.0);
                level.sendParticles(ParticleTypes.CRIT, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Les vitres proches volent en éclats (si la règle mobGriefing le permet). */
    public static void breakGlass(ServerLevel level, BlockPos center, int radius) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            var state = level.getBlockState(pos);
            if ((state.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) || state.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES))
                    && pos.distSqr(center) <= radius * radius) {
                level.destroyBlock(pos, false);
            }
        }
    }

    private static DamageSource source(ServerLevel level, Entity direct, Entity owner, boolean bypassArmor) {
        return level.damageSources().source(bypassArmor ? ModDamageTypes.BULLET_AP : ModDamageTypes.BULLET, direct, owner);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        Long until = NO_FALL.get(event.getEntity().getUUID());
        if (until != null && event.getEntity().level().getGameTime() < until) {
            event.setCanceled(true);
            if (event.getEntity().onGround()) {
                NO_FALL.remove(event.getEntity().getUUID());
            }
        }
    }

    private Detonations() {}
}

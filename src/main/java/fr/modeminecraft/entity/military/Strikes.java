package fr.modeminecraft.entity.military;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Frappes balistiques (9K720 Iskander) : le vol est simulé par le serveur (il peut traverser des régions non chargées),
 * le missile n'est dessiné que près des joueurs. Montée dans la stratosphère avec une épaisse traînée blanche, piqué
 * à la verticale à Mach 6 avec manœuvres d'évitement, alerte sonore 2 s avant l'impact, puis détonation :
 * - 9M723-HE : cratère de 22 blocs de diamètre (bâtiments, bunkers et blindés rasés) ;
 * - 9M723-Cluster : s'ouvre à 60 blocs du sol, 36 sous-munitions sur 45 x 45 blocs ;
 * - 9M723-EMP / thermobarique : boule de feu géante + impulsion électromagnétique sur 80 blocs (drones, radars,
 *   tourelles, véhicules et éclairages grillés).
 * Le S-500 (sentinelle) et le S-400 peuvent les intercepter en haute altitude.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class Strikes {
    public enum Warhead { HE, CLUSTER, EMP }

    public static final class Strike {
        public final UUID id = UUID.randomUUID();
        public final Warhead warhead;
        public final Vec3 from, target;
        public final long launch, impact;
        public final Entity owner;
        boolean alarmed, intercepting;
        long interceptAt;

        Strike(Warhead w, Vec3 from, Vec3 target, long launch, Entity owner) {
            this.warhead = w;
            this.from = from;
            this.target = target;
            this.launch = launch;
            double d = Math.hypot(target.x - from.x, target.z - from.z);
            this.impact = launch + 70 + (long) (d / 7);                     // montée ~3,5 s, puis Mach 6 (~7 blocs / tick)
            this.owner = owner;
        }

        /** Position simulée (montée, croisière très haute, piqué). */
        public Vec3 position(long now) {
            double f = Mth.clamp((now - launch) / (double) (impact - launch), 0, 1);
            double apex = 220 + from.distanceTo(target) * 0.15;
            double y = f < 0.85 ? Mth.lerp(Math.min(1, f / 0.25), from.y, from.y + apex) : Mth.lerp((f - 0.85) / 0.15, from.y + apex, target.y);
            double hf = f < 0.25 ? f * 0.2 : 0.05 + (f - 0.25) / 0.75 * 0.95;
            return new Vec3(Mth.lerp(hf, from.x, target.x), y, Mth.lerp(hf, from.z, target.z));
        }
    }

    private static final List<Strike> ACTIVE = new ArrayList<>();

    public static List<Strike> active() {
        return ACTIVE;
    }

    public static Strike launch(ServerLevel level, Warhead w, Vec3 from, Vec3 target, Entity owner) {
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(target.x), (int) Math.floor(target.z));
        Vec3 t = new Vec3(target.x, Math.max(level.getMinBuildHeight() + 5, ground), target.z);
        if (!level.isLoaded(BlockPos.containing(t))) {
            t = new Vec3(target.x, target.y, target.z);                      // région non chargée : altitude visée
        }
        Strike s = new Strike(w, from, t, level.getGameTime(), owner);
        ACTIVE.add(s);
        // départ : missile visible qui monte depuis le lanceur
        ShellEntity visual = new ShellEntity(level, owner instanceof LivingEntity le ? le : null, MilAmmo.Shell.BALLISTIC, null);
        visual.setPos(from.x, from.y, from.z);
        visual.visualAscent();
        level.addFreshEntity(visual);
        level.playSound(null, from.x, from.y, from.z, ModSounds.MISSILE_LAUNCH.get(), SoundSource.NEUTRAL, 8f, 0.6f);
        ModeMinecraft.LOGGER.info("Frappe Iskander ({}) vers {} — impact dans {} s", w, BlockPos.containing(t), (s.impact - s.launch) / 20);
        return s;
    }

    /** Interception (S-500 / S-400) : réussie sauf si le missile manœuvre à temps (25 %). */
    public static boolean intercept(ServerLevel level, Strike s, Vec3 battery, long at) {
        if (s.intercepting) {
            return false;
        }
        s.intercepting = true;
        s.interceptAt = at;
        return true;
    }

    /**
     * L'intercepteur est arrivé au contact du missile balistique : explosion à sa position (visible), sauf manœuvre
     * d'évitement réussie (25 %) ; le missile poursuit alors sa route et une autre batterie peut retenter sa chance.
     */
    public static boolean resolveIntercept(ServerLevel level, Strike s, Vec3 at) {
        if (!ACTIVE.contains(s)) {
            return false;
        }
        if (level.random.nextFloat() < 0.25f) {
            s.intercepting = false;
            return false;
        }
        ACTIVE.remove(s);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 2, 2, 2, 2, 0);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 3, 1, 1, 1, 0);
        level.playSound(null, at.x, at.y, at.z, ModSounds.HUGE_EXPLOSION.get(), SoundSource.NEUTRAL, 16f, 1.4f);
        ModeMinecraft.LOGGER.info("Frappe {} interceptée à {}", s.warhead, BlockPos.containing(at));
        return true;
    }

    /** Intercepteur détruit / perdu sans atteindre le missile : une autre batterie peut tirer. */
    public static void interceptFailed(Strike s) {
        s.intercepting = false;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        for (Iterator<Strike> it = ACTIVE.iterator(); it.hasNext(); ) {
            Strike s = it.next();
            Vec3 p = s.position(now);
            // traînée visible près des joueurs
            if (now % 2 == 0 && level.getNearestPlayer(p.x, p.y, p.z, 240, false) != null) {
                level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 4, 0.4, 0.4, 0.4, 0.01);
                level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.01);
            }
            if (s.intercepting && now >= s.interceptAt) {
                it.remove();
                if (level.random.nextFloat() < 0.25f) {
                    // manœuvre d'évitement : le missile échappe à l'intercepteur (il reste en vol)
                    s.intercepting = false;
                    ACTIVE.add(0, s);
                    break;
                }
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y, p.z, 2, 2, 2, 2, 0);
                level.sendParticles(ParticleTypes.FLASH, p.x, p.y, p.z, 3, 1, 1, 1, 0);
                level.playSound(null, p.x, p.y, p.z, ModSounds.HUGE_EXPLOSION.get(), SoundSource.NEUTRAL, 16f, 1.4f);
                ModeMinecraft.LOGGER.info("Frappe {} interceptée à {}", s.warhead, BlockPos.containing(p));
                continue;
            }
            if (!s.alarmed && now >= s.impact - 40) {
                s.alarmed = true;
                level.playSound(null, s.target.x, s.target.y + 2, s.target.z, ModSounds.AIR_RAID_ALARM.get(), SoundSource.NEUTRAL, 10f, 1.2f);
                AirRaid.alert(level, s.target, 160);
                if (level.isLoaded(BlockPos.containing(s.target))) {
                    ShellEntity dive = new ShellEntity(level, s.owner instanceof LivingEntity le ? le : null, MilAmmo.Shell.BALLISTIC, null);
                    dive.setPos(s.target.x, s.target.y + 200, s.target.z);
                    dive.visualDive(s.target, 40);
                    level.addFreshEntity(dive);
                }
            }
            if (s.warhead == Warhead.CLUSTER && now == s.impact - 8) {
                cluster(level, s);
            }
            if (now >= s.impact) {
                it.remove();
                if (s.warhead != Warhead.CLUSTER) {
                    detonate(level, s);
                }
            }
        }
    }

    private static void detonate(ServerLevel level, Strike s) {
        Vec3 t = s.target;
        level.getChunk(BlockPos.containing(t));                              // charge la zone visée si besoin
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(t.x), (int) Math.floor(t.z));
        Vec3 at = new Vec3(t.x, ground, t.z);
        Entity src = s.owner;
        if (s.warhead == Warhead.HE) {
            // cratère de 22 blocs de diamètre
            int r = 11;
            BlockPos c = BlockPos.containing(at);
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dy = -r; dy <= r; dy++) {
                        double d = Math.sqrt(dx * dx + dz * dz + dy * dy * 2.2);
                        if (d > r + level.random.nextFloat() * 0.8) {
                            continue;
                        }
                        m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                        BlockState st = level.getBlockState(m);
                        if (!st.isAir() && st.getDestroySpeed(level, m) >= 0 && !st.is(Blocks.BEDROCK)) {
                            level.setBlock(m, Blocks.AIR.defaultBlockState(), 2 | 16);
                        }
                    }
                }
            }
            level.explode(src, null, null, at.x, at.y, at.z, 9f, true, Level.ExplosionInteraction.TNT);
            hurtAround(level, at, 22, 90, src);
            MilHelpers.damageVehicles(level, at, 22, 400);
        } else {
            // thermobarique : boule de feu + impulsion électromagnétique sur 80 blocs
            level.explode(src, null, null, at.x, at.y + 2, at.z, 7f, true, Level.ExplosionInteraction.TNT);
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 4, at.z, 300, 8, 5, 8, 0.15);
            hurtAround(level, at, 18, 60, src);
            emp(level, at, 80);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 2, at.z, 6, 6, 3, 6, 0);
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, at.x, at.y + 4, at.z, 120, 6, 8, 6, 0.08);
        level.playSound(null, at.x, at.y, at.z, ModSounds.HUGE_EXPLOSION.get(), SoundSource.NEUTRAL, 24f, 0.7f);
        fr.modeminecraft.entity.bot.CivilianLife.panic(level, at, 120, 600);
    }

    /** Ogive à sous-munitions : 36 bombettes sur 45 x 45 blocs. */
    private static void cluster(ServerLevel level, Strike s) {
        Vec3 t = s.target;
        boolean live = level.isPositionEntityTicking(BlockPos.containing(t));
        if (live) {
            level.sendParticles(ParticleTypes.EXPLOSION, t.x, t.y + 60, t.z, 4, 2, 1, 2, 0);
            level.playSound(null, t.x, t.y + 60, t.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 10f, 1.4f);
        }
        for (int i = 0; i < 36; i++) {
            double x = t.x + (level.random.nextDouble() - 0.5) * 45, z = t.z + (level.random.nextDouble() - 0.5) * 45;
            if (live) {
                ShellEntity sub = new ShellEntity(level, s.owner instanceof LivingEntity le ? le : null, MilAmmo.Shell.SUBMUNITION, null);
                sub.setPos(t.x, t.y + 60, t.z);
                double flight = 30;
                sub.setDeltaMovement((x - t.x) / flight, -0.2, (z - t.z) / flight);
                level.addFreshEntity(sub);
            } else {
                level.getChunk(BlockPos.containing(x, 0, z));
                int g = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
                level.explode(s.owner, x, g, z, 3.5f, Level.ExplosionInteraction.TNT);
            }
        }
    }

    private static void hurtAround(ServerLevel level, Vec3 at, double r, float dmg, Entity src) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r))) {
            double d = e.position().distanceTo(at);
            if (d < r) {
                e.hurt(src != null ? level.damageSources().explosion(src, src) : level.damageSources().explosion(null, null), (float) (dmg * (1 - d / r)));
            }
        }
    }

    /** Impulsion électromagnétique : drones, radars, tourelles, véhicules et éclairages grillés. */
    public static void emp(ServerLevel level, Vec3 at, double r) {
        AABB box = new AABB(at, at).inflate(r);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 3, at.z, 400, r / 4, 6, r / 4, 0.6);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 10f, 0.4f);
        for (Entity e : level.getEntities((Entity) null, box, e -> e.position().distanceTo(at) < r)) {
            if (e instanceof fr.modeminecraft.entity.drone.DroneEntity d) {
                d.jam(600);
                d.hurt(level.damageSources().lightningBolt(), 50);
            } else if (e instanceof MilVehicleEntity v) {
                v.empHit();
            } else if (e instanceof fr.modeminecraft.entity.VehicleEntity car) {
                car.hurt(level.damageSources().lightningBolt(), 25);
            } else if (e instanceof fr.modeminecraft.entity.MountedGunEntity gun) {
                gun.hurt(level.damageSources().lightningBolt(), 40);
            } else if (e instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(fr.modeminecraft.registry.ModEffects.EMP, 1200));
            }
        }
        // éclairages éteints (rayon réduit : 24 blocs autour de l'impact)
        BlockPos c = BlockPos.containing(at);
        int lr = 24;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-lr, -8, -lr), c.offset(lr, 24, lr))) {
            BlockState s = level.getBlockState(p);
            if (s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT) && !s.is(Blocks.FURNACE) && !s.is(Blocks.CAMPFIRE)) {
                level.setBlock(p, s.setValue(BlockStateProperties.LIT, false), 3);
            } else if (s.is(Blocks.SEA_LANTERN) || s.is(Blocks.GLOWSTONE)) {
                level.setBlock(p, Blocks.GRAY_STAINED_GLASS.defaultBlockState(), 3);
            }
        }
    }

    private Strikes() {}
}

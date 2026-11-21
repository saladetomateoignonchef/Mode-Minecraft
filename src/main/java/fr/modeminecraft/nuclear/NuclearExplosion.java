package fr.modeminecraft.nuclear;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.entity.military.Strikes;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Détonation nucléaire en 4 phases (cahier des charges §2.3.B) :
 * <ol>
 * <li>T = 0 : flash blanc sur 400 blocs (cécité 3 s pour qui regarde l'explosion sans visière), cœur vaporisé ;</li>
 * <li>T = 0,5 à 25 s : boule de feu puis champignon atomique (entité {@link NukeCloudEntity}, 180 blocs de haut) ;
 * cratère de 110 blocs de diamètre et 35 de profondeur, creusé par anneaux sur 40 ticks (pas de gel du serveur) ;</li>
 * <li>T = 1 à 6 s : onde de choc visible jusqu'à 180 blocs (vitres et blocs fragiles soufflés, feuillages arrachés,
 * entités et véhicules projetés), grondement qui arrive avec un retard proportionnel à la distance ;</li>
 * <li>impulsion électromagnétique sur 220 blocs ; puis zone radioactive de 160 blocs pendant 7 jours.</li>
 * </ol>
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class NuclearExplosion {
    public static final int CRATER_R = 55, CRATER_DEPTH = 35, CRATER_TICKS = 40, WAVE_R = 180, WAVE_TICKS = 100, FLASH_R = 400, EMP_R = 220;

    private record Rumble(ServerPlayer p, long at, float volume) {}

    private static final class Blast {
        final ServerLevel level;
        final BlockPos c;
        final Entity owner;
        int t;
        final List<Rumble> rumbles = new ArrayList<>();

        Blast(ServerLevel level, BlockPos c, Entity owner) {
            this.level = level;
            this.c = c;
            this.owner = owner;
        }
    }

    private static final List<Blast> ACTIVE = new ArrayList<>();

    public static void start(ServerLevel level, BlockPos c, Entity owner) {
        Vec3 at = Vec3.atCenterOf(c);
        Blast b = new Blast(level, c, owner);
        ModeMinecraft.LOGGER.info("Détonation nucléaire à {}", c);
        // 1. flash et cœur vaporisé
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(at);
            if (d < FLASH_R) {
                PacketDistributor.sendToPlayer(p, new FlashPayload(at.x, at.y, at.z, (float) (1 - d / FLASH_R * 0.7)));
                Vec3 look = p.getViewVector(1);
                Vec3 to = at.add(0, 20, 0).subtract(p.getEyePosition()).normalize();
                if (look.dot(to) > 0.35 && !RadiationSuitItem.visor(p)) {
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60));
                }
            }
            if (d < 1500) {
                b.rumbles.add(new Rumble(p, level.getGameTime() + (long) (d / 34), (float) Math.max(0.3, 1 - d / 1500)));
            }
        }
        for (Entity e : level.getEntities((Entity) null, new AABB(c).inflate(CRATER_R), e -> e.position().distanceTo(at) < CRATER_R)) {
            if (e instanceof LivingEntity le) {
                le.hurt(level.damageSources().explosion(owner, owner), 10_000);
            } else if (e instanceof MilVehicleEntity v) {
                v.blastHit(at, 100_000);
            } else if (!(e instanceof net.minecraft.world.entity.player.Player)) {
                e.discard();
            }
        }
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 10, at.z, 10, 6, 6, 6, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 4, at.z, 30, 20, 10, 20, 0);
        level.playSound(null, at.x, at.y, at.z, ModSounds.NUCLEAR_DETONATION.get(), SoundSource.NEUTRAL, 64f, 1f);
        // 2. champignon atomique, 4. EMP et zone radioactive
        level.addFreshEntity(new NukeCloudEntity(level, at));
        Strikes.emp(level, at, EMP_R);
        RadiationZones.add(level, c);
        fr.modeminecraft.entity.bot.CivilianLife.panic(level, at, 300, 2400);
        ACTIVE.add(b);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        for (Iterator<Blast> it = ACTIVE.iterator(); it.hasNext(); ) {
            Blast b = it.next();
            long now = b.level.getGameTime();
            b.rumbles.removeIf(r -> {
                if (now >= r.at && r.p.isAlive()) {
                    r.p.playNotifySound(ModSounds.NUCLEAR_RUMBLE.get(), SoundSource.NEUTRAL, r.volume * 1.4f, 1f);
                    return true;
                }
                return now >= r.at;
            });
            if (b.t < CRATER_TICKS) {
                craterRing(b, b.t);
            }
            if (b.t < WAVE_TICKS) {
                waveRing(b, b.t);
            }
            b.t++;
            if (b.t >= Math.max(CRATER_TICKS, WAVE_TICKS) && b.rumbles.isEmpty()) {
                it.remove();
            }
        }
    }

    /** Anneau n du cratère : vaporise tout entre la cuvette et 60 blocs au-dessus ; bords calcinés au dernier tick. */
    private static void craterRing(Blast b, int n) {
        ServerLevel level = b.level;
        double r0 = CRATER_R * n / (double) CRATER_TICKS, r1 = CRATER_R * (n + 1) / (double) CRATER_TICKS;
        BlockPos c = b.c;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int R = (int) Math.ceil(r1);
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < r0 || d >= r1) {
                    continue;
                }
                int x = c.getX() + dx, z = c.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                double f = d / CRATER_R;
                int bottom = c.getY() - (int) (CRATER_DEPTH * Math.sqrt(Math.max(0, 1 - f * f))) + level.random.nextInt(2);
                int top = Math.min(level.getMaxBuildHeight() - 1, c.getY() + 60);
                for (int y = bottom; y <= top; y++) {
                    m.set(x, y, z);
                    Bombs.clear(level, m);
                }
                // fond vitrifié
                m.set(x, bottom - 1, z);
                BlockState st = level.getBlockState(m);
                if (!st.isAir() && st.getDestroySpeed(level, m) >= 0 && level.random.nextFloat() < 0.5f) {
                    level.setBlock(m, level.random.nextFloat() < 0.15f ? Blocks.MAGMA_BLOCK.defaultBlockState()
                            : level.random.nextBoolean() ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState(), 2 | 16);
                }
            }
        }
        if (n == CRATER_TICKS - 1) {
            scorch(b);
        }
    }

    /** Couronne calcinée autour du cratère (55 à 85 blocs) : sol brûlé, feux. */
    private static void scorch(Blast b) {
        ServerLevel level = b.level;
        BlockPos c = b.c;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int R = (int) (CRATER_R * 1.55);
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < CRATER_R || d > R || level.random.nextFloat() > 0.75f * (1 - (d - CRATER_R) / (R - CRATER_R)) + 0.1f) {
                    continue;
                }
                int x = c.getX() + dx, z = c.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                m.set(x, y, z);
                BlockState st = level.getBlockState(m);
                if (st.isAir() || st.getDestroySpeed(level, m) < 0) {
                    continue;
                }
                float k = level.random.nextFloat();
                level.setBlock(m, k < 0.4f ? Blocks.COARSE_DIRT.defaultBlockState() : k < 0.7f ? Blocks.BLACKSTONE.defaultBlockState()
                        : k < 0.85f ? Blocks.BASALT.defaultBlockState() : Blocks.COAL_BLOCK.defaultBlockState(), 2);
                if (level.random.nextFloat() < 0.12f) {
                    m.move(0, 1, 0);
                    if (level.getBlockState(m).isAir()) {
                        level.setBlock(m, Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /** Onde de choc (anneau n) : vitres et blocs fragiles soufflés, feuilles arrachées, entités projetées. */
    private static void waveRing(Blast b, int n) {
        ServerLevel level = b.level;
        BlockPos c = b.c;
        Vec3 at = Vec3.atCenterOf(c);
        double r0 = WAVE_R * n / (double) WAVE_TICKS, r1 = WAVE_R * (n + 1) / (double) WAVE_TICKS;
        // anneau de poussière visible
        int pts = (int) (16 + r1 * 0.9);
        for (int i = 0; i < pts; i++) {
            double a = 2 * Math.PI * i / pts;
            double x = at.x + Math.cos(a) * r1, z = at.z + Math.sin(a) * r1;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            level.sendParticles(ParticleTypes.CLOUD, x, y + 1, z, 2, 1, 0.6, 1, 0.08);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 2, z, 1, 1, 1, 1, 0.02);
            }
        }
        // entités atteintes par le front de l'onde
        for (Entity e : level.getEntities((Entity) null, new AABB(c).inflate(r1, 120, r1), e -> {
            double d = Math.hypot(e.getX() - at.x, e.getZ() - at.z);
            return d >= r0 && d < r1 && d >= CRATER_R;
        })) {
            double d = Math.hypot(e.getX() - at.x, e.getZ() - at.z);
            double k = 1 - d / WAVE_R;
            Vec3 out = new Vec3(e.getX() - at.x, 0, e.getZ() - at.z).normalize();
            if (e instanceof LivingEntity le) {
                le.hurt(level.damageSources().explosion(b.owner, b.owner), (float) (140 * k * k));
                le.knockback(3.5 * k, -out.x, -out.z);
                le.setDeltaMovement(le.getDeltaMovement().add(0, 0.7 * k, 0));
                le.hurtMarked = true;
                le.igniteForSeconds((int) (10 * k));
            } else if (e instanceof MilVehicleEntity v) {
                v.blastHit(at, (float) (1500 * k * k));
            } else {
                e.setDeltaMovement(e.getDeltaMovement().add(out.scale(2.5 * k)).add(0, 0.5 * k, 0));
                e.hurtMarked = true;
            }
        }
        // blocs fragiles du front d'onde
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int R = (int) Math.ceil(r1);
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < r0 || d >= r1 || d < CRATER_R) {
                    continue;
                }
                int x = c.getX() + dx, z = c.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                double k = 1 - d / WAVE_R;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                for (int y = top; y >= top - 28; y--) {
                    m.set(x, y, z);
                    BlockState st = level.getBlockState(m);
                    if (st.isAir()) {
                        continue;
                    }
                    boolean glass = st.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) || st.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES)
                            || st.is(Blocks.IRON_BARS);
                    boolean leaves = st.is(BlockTags.LEAVES);
                    boolean soft = st.is(BlockTags.FLOWERS) || st.is(BlockTags.REPLACEABLE) || st.is(BlockTags.WOOL_CARPETS);
                    if (glass && level.random.nextFloat() < 0.4 + k
                            || leaves && level.random.nextFloat() < k * 1.3
                            || soft && level.random.nextFloat() < k * 1.5) {
                        level.setBlock(m, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private NuclearExplosion() {}
}

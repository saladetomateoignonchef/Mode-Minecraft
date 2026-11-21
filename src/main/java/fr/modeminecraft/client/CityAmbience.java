package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.block.city.AmbientSpeakerBlock;
import fr.modeminecraft.block.city.AmbientSpeakerBlockEntity;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Paysage sonore des villes (côté client) :
 * - fond : rumeur de la circulation (métropole, cité soviétique), vent entre les gratte-ciel, brouhaha du souk
 *   (cité du désert), grillons la nuit, pluie qui crépite sur les auvents et les toits en tôle ;
 * - musiques des cafés et supermarchés, piano du saloon, annonces des haut-parleurs (bases, dictature) ;
 * - grésillements radio près des policiers, aboiements de chiens au loin, bourdonnement des transformateurs.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class CityAmbience {
    private static String variant = "";
    private static int baseY;
    private static final Map<String, Loop> BED = new HashMap<>();
    private static final Map<BlockPos, Loop> SPOTS = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();
    private static long nextDog, nextAnnounce;

    public static void setCity(String v, int y) {
        variant = v;
        baseY = y;
    }

    public static String variant() {
        return variant;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            variant = "";
            BED.clear();
            SPOTS.clear();
            return;
        }
        BED.values().removeIf(Loop::isStopped);
        SPOTS.values().removeIf(Loop::isStopped);
        var level = mc.level;
        var p = mc.player;
        boolean city = !variant.isEmpty();
        boolean night = fr.modeminecraft.world.night.NightOps.isNight(level);
        float rain = level.getRainLevel(1f);
        // fonds sonores (non positionnés), qui montent et descendent en douceur
        bed("traffic", ModSounds.TRAFFIC_LOOP, city && (variant.equals("metropolis") || variant.equals("soviet")) ? (night ? 0.18f : 0.38f) : 0);
        float high = city && variant.equals("metropolis") ? Mth.clamp((float) (p.getY() - baseY - 12) / 30f, 0.08f, 0.6f) : 0;
        bed("wind", ModSounds.WIND_LOOP, high);
        bed("market", ModSounds.MARKET_LOOP, city && variant.equals("desert") && !night ? 0.32f : 0);
        bed("crickets", ModSounds.CRICKETS_LOOP, night && rain < 0.2f && (city || p.getY() > 55) ? 0.22f : 0);
        bed("rain_tin", ModSounds.RAIN_TIN_LOOP, rain > 0.2f && roofOverhead(level, p.blockPosition()) ? 0.45f * rain : 0);
        // journaux emportés par le vent dans les rues (le jour, sans pluie)
        if (city && !night && rain < 0.2f && p.tickCount % 7 == 0 && RANDOM.nextFloat() < 0.35f) {
            double a = level.getGameTime() * 0.0004, wx = Math.cos(a) * 0.12, wz = Math.sin(a) * 0.12;
            double x = p.getX() - wx * 120 + (RANDOM.nextDouble() - 0.5) * 30, z = p.getZ() - wz * 120 + (RANDOM.nextDouble() - 0.5) * 30;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
            if (Math.abs(y - p.getY()) < 6) {
                level.addParticle(fr.modeminecraft.registry.ModParticles.NEWSPAPER.get(), x, y + 0.4, z, wx, 0.02, wz);
            }
        }
        if (p.tickCount % 20 != 0) {
            return;
        }
        // musiques et annonces : haut-parleurs à moins de 24 blocs
        ChunkPos cp = p.chunkPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!level.hasChunk(cp.x + dx, cp.z + dz)) {
                    continue;
                }
                for (var be : level.getChunk(cp.x + dx, cp.z + dz).getBlockEntities().values()) {
                    if (!(be instanceof AmbientSpeakerBlockEntity) || be.getBlockPos().distSqr(p.blockPosition()) > 24 * 24) {
                        continue;
                    }
                    var kind = be.getBlockState().getValue(AmbientSpeakerBlock.KIND);
                    if (kind == AmbientSpeakerBlock.Kind.LOUDSPEAKER) {
                        if (level.getGameTime() > nextAnnounce) {
                            nextAnnounce = level.getGameTime() + 900 + RANDOM.nextInt(900);
                            play(ModSounds.LOUDSPEAKER.get(), be.getBlockPos(), 2.5f, 1f);
                        }
                    } else if (!SPOTS.containsKey(be.getBlockPos())) {
                        Loop l = new Loop(kind == AmbientSpeakerBlock.Kind.SALOON ? ModSounds.SALOON_PIANO.get() : ModSounds.CAFE_MUSIC.get(), 0.7f);
                        l.at(be.getBlockPos());
                        SPOTS.put(be.getBlockPos().immutable(), l);
                        mc.getSoundManager().play(l);
                    }
                }
            }
        }
        if (!city) {
            return;
        }
        // grésillements radio des policiers, chiens au loin, transformateurs
        for (HumanBotEntity bot : level.getEntitiesOfClass(HumanBotEntity.class, p.getBoundingBox().inflate(16),
                b -> b.faction().group.equals("police"))) {
            if (RANDOM.nextFloat() < 0.05f) {
                play(ModSounds.RADIO_CRACKLE.get(), bot.blockPosition(), 0.5f, 0.95f + RANDOM.nextFloat() * 0.1f);
            }
        }
        if (level.getGameTime() > nextDog) {
            nextDog = level.getGameTime() + 800 + RANDOM.nextInt(1600);
            double a = RANDOM.nextDouble() * Math.PI * 2;
            play(ModSounds.DOG_BARK.get(), BlockPos.containing(p.getX() + Math.cos(a) * 35, p.getY(), p.getZ() + Math.sin(a) * 35), 3f,
                    0.85f + RANDOM.nextFloat() * 0.3f);
        }
        if (p.tickCount % 60 == 0) {
            for (BlockPos q : BlockPos.betweenClosed(p.blockPosition().offset(-8, -3, -8), p.blockPosition().offset(8, 3, 8))) {
                if (!SPOTS.containsKey(q) && level.getBlockState(q).is(fr.modeminecraft.registry.ModBlocks.HIGH_VOLTAGE_TRANSFORMER.get())) {
                    Loop l = new Loop(ModSounds.TRANSFORMER_HUM.get(), 0.4f);
                    l.at(q);
                    SPOTS.put(q.immutable(), l);
                    mc.getSoundManager().play(l);
                }
            }
        }
    }

    /** Un toit d'auvent ou de tôle juste au-dessus (laine, tôle, trappe en fer...) ? */
    private static boolean roofOverhead(net.minecraft.world.level.Level level, BlockPos pos) {
        for (int y = 2; y <= 5; y++) {
            var s = level.getBlockState(pos.above(y));
            if (s.is(net.minecraft.tags.BlockTags.WOOL) || s.is(Blocks.IRON_BLOCK) || s.is(Blocks.IRON_TRAPDOOR) || s.is(Blocks.LIGHT_GRAY_CONCRETE)
                    || s.is(net.minecraft.tags.BlockTags.WOOL_CARPETS)) {
                return true;
            }
            if (!s.isAir()) {
                return false;
            }
        }
        return false;
    }

    private static void play(SoundEvent sound, BlockPos pos, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.AMBIENT, volume, pitch, RANDOM,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    private static void bed(String key, Supplier<SoundEvent> sound, float target) {
        Loop l = BED.get(key);
        if (l == null && target > 0) {
            l = new Loop(sound.get(), 0);
            l.relativeTo();
            l.target = target;
            BED.put(key, l);
            Minecraft.getInstance().getSoundManager().play(l);
        } else if (l != null) {
            l.target = target;
        }
    }

    /** Boucle qui suit une consigne de volume (fondu), positionnée ou non. */
    static final class Loop extends AbstractTickableSoundInstance {
        float target;
        private float level;

        Loop(SoundEvent sound, float volume) {
            super(sound, SoundSource.AMBIENT, RANDOM);
            this.looping = true;
            this.delay = 0;
            this.target = volume;
            this.volume = 0.01f;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
        }

        void relativeTo() {
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.x = this.y = this.z = 0;
        }

        void at(BlockPos pos) {
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
            this.level = 0;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (!relative && mc.player != null) {
                double d = mc.player.distanceToSqr(x, y, z);
                if (d > 30 * 30 || mc.level != null && mc.level.getBlockState(BlockPos.containing(x, y, z)).isAir()) {
                    target = 0;
                }
            }
            level += Mth.clamp(target - level, -0.02f, 0.02f);
            volume = Math.max(0.001f, level);
            if (target <= 0 && level <= 0.001f) {
                stop();
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }

    private CityAmbience() {}
}

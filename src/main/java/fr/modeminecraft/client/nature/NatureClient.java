package fr.modeminecraft.client.nature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModParticles;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Réalisme naturel côté client (lot 2) : tempêtes de sable [1], brouillard matinal [2], blizzards [3], écume et ressac [4], brume de chaleur [5],
 * aurores boréales [6], brume des sommets [10], cascades [35], eau turquoise des hauts-fonds [42], empreintes et traces de pneus [29], et, pour la
 * faune [19][20], lucioles (la nuit, muettes) et papillons (le jour). Tout se déduit de l'heure du monde et des biomes : rien n'est envoyé par le réseau.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class NatureClient {
    // intensités lissées (0 à 1), pour que le passage d'un biome à l'autre soit progressif
    private static float sand, fog, blizzard, haze, aurora;
    private static int surfCooldown, fallCooldown, butterflyCooldown;
    private static final Map<Integer, double[]> LAST_PRINT = new HashMap<>();

    private NatureClient() {}

    private static boolean isDesert(Holder<Biome> b) {
        return b.is(Biomes.DESERT) || b.is(BiomeTags.IS_BADLANDS);
    }

    private static boolean isMistyBiome(Holder<Biome> b) {
        return b.is(Biomes.PLAINS) || b.is(Biomes.SUNFLOWER_PLAINS) || b.is(Biomes.MEADOW) || b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP)
                || b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_TAIGA);
    }

    private static boolean isPeaks(Holder<Biome> b) {
        return b.is(Biomes.JAGGED_PEAKS) || b.is(Biomes.STONY_PEAKS) || b.is(Biomes.FROZEN_PEAKS) || b.is(BiomeTags.IS_MOUNTAIN);
    }

    private static float approach(float cur, float target, float rate) {
        return cur + Mth.clamp(target - cur, -rate, rate);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player p = mc.player;
        if (level == null || p == null || mc.isPaused()) {
            return;
        }
        BlockPos pos = p.blockPosition();
        Holder<Biome> biome = level.getBiome(pos);
        long dt = level.getDayTime();
        long tod = Math.floorMod(dt, 24000L);
        float rain = level.getRainLevel(1f);
        boolean outdoor = level.canSeeSky(pos) || level.canSeeSky(pos.above(2));
        RandomSource rnd = level.random;
        double wa = NatureClock.windAngle(dt);
        double wx = Math.cos(wa), wz = Math.sin(wa);

        // ----- intensités cibles
        float sandT = isDesert(biome) && rain < 0.2f ? NatureClock.sandstorm(dt) * (outdoor ? 1f : 0.25f) : 0f;
        float fogT = isMistyBiome(biome) && rain < 0.6f ? NatureClock.morningFog(dt) * Mth.clamp(1f - (float) (p.getY() - level.getSeaLevel() - 6) / 45f, 0f, 1f) : 0f;
        float blizT = biome.value().coldEnoughToSnow(pos) ? Mth.clamp((rain - 0.2f) / 0.6f, 0f, 1f) * (outdoor ? 1f : 0.3f) : 0f;
        float hazeT = isDesert(biome) && rain < 0.1f ? NatureClock.heatHaze(dt) : 0f;
        float nightF = Mth.clamp(level.getStarBrightness(1f) * 2.5f, 0f, 1f);
        float auroraT = biome.value().getBaseTemperature() <= 0.15f && rain < 0.3f ? nightF * (1f - rain) : 0f;
        sand = approach(sand, sandT, 0.01f);
        fog = approach(fog, fogT, 0.006f);
        blizzard = approach(blizzard, blizT, 0.01f);
        haze = approach(haze, hazeT, 0.01f);
        aurora = approach(aurora, auroraT, 0.004f);

        // ----- vent : léger poussée latérale des joueurs à découvert pendant une tempête de sable ou un blizzard
        float windy = Math.max(sand, blizzard);
        if (windy > 0.3f && outdoor && !p.isSpectator() && !p.getAbilities().flying) {
            Vec3 dm = p.getDeltaMovement();
            p.setDeltaMovement(dm.x + wx * 0.0045 * windy, dm.y, dm.z + wz * 0.0045 * windy);
        }

        // ----- [1] tempête de sable : grains emportés par le vent
        if (sand > 0.12f) {
            DustParticleOptions grain = new DustParticleOptions(new Vector3f(0.83f, 0.68f, 0.42f), 1.4f);
            int n = (int) (10 * sand);
            for (int i = 0; i < n; i++) {
                level.addParticle(grain, p.getX() + (rnd.nextDouble() - 0.5) * 36, p.getY() + rnd.nextDouble() * 7 - 1, p.getZ() + (rnd.nextDouble() - 0.5) * 36,
                        wx * 0.55, 0.01, wz * 0.55);
            }
        }
        // ----- [3] blizzard : flocons presque horizontaux
        if (blizzard > 0.12f) {
            int n = (int) (8 * blizzard);
            for (int i = 0; i < n; i++) {
                level.addParticle(ParticleTypes.SNOWFLAKE, p.getX() + (rnd.nextDouble() - 0.5) * 30, p.getY() + rnd.nextDouble() * 8 - 1, p.getZ() + (rnd.nextDouble() - 0.5) * 30,
                        wx * 0.45, -0.04, wz * 0.45);
            }
        }
        // ----- [10] nappes de brume accrochées aux flancs des hauts sommets
        if (p.getY() > 95 && p.getY() < 190 && isPeaks(biome) && level.getGameTime() % 4 == 0) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(ModParticles.MIST.get(), p.getX() + (rnd.nextDouble() - 0.5) * 80, Mth.clamp(p.getY() + (rnd.nextDouble() - 0.5) * 40, 105, 165),
                        p.getZ() + (rnd.nextDouble() - 0.5) * 80, wx * 0.04, 0, wz * 0.04);
            }
        }
        // ----- [19] lucioles : la nuit, marais et forêts, visuelles seulement (aucun son)
        if (tod > 13000 && tod < 23500 && rain < 0.3f && (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP) || biome.is(BiomeTags.IS_FOREST))
                && level.getGameTime() % 5 == 0) {
            int x = pos.getX() + rnd.nextInt(29) - 14, z = pos.getZ() + rnd.nextInt(29) - 14;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            if (!level.getBlockState(ground).isAir() && level.getFluidState(ground).isEmpty() || level.getFluidState(ground).is(FluidTags.WATER)) {
                level.addParticle(ModParticles.FIREFLY.get(), x + 0.5, y + 0.3 + rnd.nextDouble() * 1.8, z + 0.5, 0, 0, 0);
            }
        }
        // ----- [20] papillons autour des fleurs, le jour
        if (--butterflyCooldown <= 0) {
            butterflyCooldown = 25;
            if (tod < 12500 && rain < 0.2f && (biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS) || biome.is(Biomes.MEADOW) || biome.is(Biomes.FLOWER_FOREST)
                    || biome.is(Biomes.FOREST) || biome.is(Biomes.BIRCH_FOREST))) {
                for (int i = 0; i < 12; i++) {
                    BlockPos f = pos.offset(rnd.nextInt(29) - 14, rnd.nextInt(5) - 2, rnd.nextInt(29) - 14);
                    if (level.getBlockState(f).is(BlockTags.SMALL_FLOWERS)) {
                        float[][] palette = {{1f, 0.62f, 0.15f}, {0.35f, 0.6f, 1f}, {1f, 0.9f, 0.3f}, {1f, 1f, 1f}, {0.95f, 0.45f, 0.7f}};
                        float[] c = palette[rnd.nextInt(palette.length)];
                        level.addParticle(ModParticles.BUTTERFLY.get(), f.getX() + 0.5, f.getY() + 0.9, f.getZ() + 0.5, c[0], c[1], c[2]);
                        break;
                    }
                }
            }
        }
        // ----- [4] écume et ressac sur les côtes ; [35] cascades
        if (level.getGameTime() % 6 == 0) {
            shoreAndFalls(level, p, pos, rnd, wx, wz);
        }
        // ----- [29] empreintes et traces de pneus : tous les joueurs et véhicules au sol visibles
    }

    private static void shoreAndFalls(ClientLevel level, Player p, BlockPos center, RandomSource rnd, double wx, double wz) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        surfCooldown--;
        fallCooldown--;
        for (int i = 0; i < 14; i++) {
            m.set(center.getX() + rnd.nextInt(41) - 20, center.getY() + rnd.nextInt(11) - 5, center.getZ() + rnd.nextInt(41) - 20);
            FluidState fluid = level.getFluidState(m);
            if (!fluid.is(FluidTags.WATER)) {
                continue;
            }
            // cascade : eau qui tombe sur au moins 4 blocs de haut ; on repère le pied de la chute
            boolean falling = fluid.hasProperty(FlowingFluid.FALLING) && fluid.getValue(FlowingFluid.FALLING);
            if (falling) {
                FluidState below = level.getFluidState(m.below());
                boolean bottom = !below.is(FluidTags.WATER) || below.isSource() || !(below.hasProperty(FlowingFluid.FALLING) && below.getValue(FlowingFluid.FALLING));
                if (bottom) {
                    int h = 0;
                    BlockPos.MutableBlockPos u = m.mutable();
                    while (h < 16) {
                        u.move(Direction.UP);
                        FluidState f = level.getFluidState(u);
                        if (f.is(FluidTags.WATER) && f.hasProperty(FlowingFluid.FALLING) && f.getValue(FlowingFluid.FALLING)) {
                            h++;
                        } else {
                            break;
                        }
                    }
                    if (h >= 3) {
                        double x = m.getX() + 0.5, y = m.getY() + 0.2, z = m.getZ() + 0.5;
                        for (int k = 0; k < 3; k++) {
                            level.addParticle(ParticleTypes.CLOUD, x + (rnd.nextDouble() - 0.5), y + rnd.nextDouble() * 0.6, z + (rnd.nextDouble() - 0.5), 0, 0.04, 0);
                            level.addParticle(ParticleTypes.SPLASH, x + (rnd.nextDouble() - 0.5), y + 0.3, z + (rnd.nextDouble() - 0.5), 0, 0.2, 0);
                        }
                        if (fallCooldown <= 0 && m.distSqr(center) < 20 * 20) {
                            level.playLocalSound(x, y, z, ModSounds.STREAM_FLOW.get(), SoundSource.AMBIENT, 1.0f, 0.7f, false);
                            fallCooldown = 14;
                        }
                    }
                }
                continue;
            }
            // écume : eau de mer qui touche le sable ou la roche, en biome océanique ou de plage
            if (!fluid.isSource() || !level.getFluidState(m.above()).isEmpty()) {
                continue;
            }
            Holder<Biome> b = level.getBiome(m);
            if (!(b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_BEACH))) {
                continue;
            }
            boolean shore = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockState s = level.getBlockState(m.relative(d));
                if (!s.isAir() && level.getFluidState(m.relative(d)).isEmpty() && s.isSolid()) {
                    shore = true;
                    break;
                }
            }
            if (shore) {
                double x = m.getX() + 0.5, y = m.getY() + 0.95, z = m.getZ() + 0.5;
                level.addParticle(ParticleTypes.SPLASH, x + (rnd.nextDouble() - 0.5), y, z + (rnd.nextDouble() - 0.5), wx * 0.05, 0.12, wz * 0.05);
                if (rnd.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.CLOUD, x, y, z, wx * 0.02, 0.02, wz * 0.02);
                }
                if (surfCooldown <= 0 && m.distSqr(center) < 16 * 16) {
                    level.playLocalSound(x, y, z, ModSounds.SURF.get(), SoundSource.AMBIENT, 0.45f, 0.9f + rnd.nextFloat() * 0.2f, false);
                    surfCooldown = 20 + rnd.nextInt(20);
                }
            }
        }
    }

    // =================================================================================================== brouillard, couleurs, champ de vision
    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        float far = event.getFarPlaneDistance(), near = event.getNearPlaneDistance();
        float newFar = far, newNear = near;
        if (sand > 0.01f) {
            newFar = Math.min(newFar, Mth.lerp(sand, far, 34f));
            newNear = Mth.lerp(sand, near, 0f);
        }
        if (blizzard > 0.01f) {
            newFar = Math.min(newFar, Mth.lerp(blizzard, far, 52f));
            newNear = Mth.lerp(blizzard, newNear, 0f);
        }
        if (fog > 0.01f) {
            newFar = Math.min(newFar, Mth.lerp(fog, far, 48f));
            newNear = Mth.lerp(fog, newNear, 0f);
        }
        if (newFar < far) {
            event.setFarPlaneDistance(newFar);
            event.setNearPlaneDistance(newNear);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float r = event.getRed(), g = event.getGreen(), b = event.getBlue();
        if (sand > 0.01f) {
            r = Mth.lerp(sand * 0.85f, r, 0.80f);
            g = Mth.lerp(sand * 0.85f, g, 0.63f);
            b = Mth.lerp(sand * 0.85f, b, 0.38f);
        }
        if (blizzard > 0.01f) {
            r = Mth.lerp(blizzard * 0.8f, r, 0.86f);
            g = Mth.lerp(blizzard * 0.8f, g, 0.9f);
            b = Mth.lerp(blizzard * 0.8f, b, 0.95f);
        }
        if (fog > 0.01f) {
            r = Mth.lerp(fog * 0.7f, r, 0.8f);
            g = Mth.lerp(fog * 0.7f, g, 0.84f);
            b = Mth.lerp(fog * 0.7f, b, 0.88f);
        }
        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }

    /** [5] Brume de chaleur : l'horizon ondule très légèrement (oscillation du champ de vision, à peine perceptible), en plein midi dans les déserts. */
    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (haze > 0.02f) {
            Minecraft mc = Minecraft.getInstance();
            double t = (mc.level == null ? 0 : mc.level.getGameTime()) + event.getPartialTick();
            event.setFOV(event.getFOV() + (Math.sin(t * 0.11) * 0.45 + Math.sin(t * 0.27) * 0.2) * haze);
        }
    }

    // =================================================================================================== [6] aurores boréales
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || aurora < 0.02f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double time = mc.level.getGameTime() + partial;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        Matrix4f m = new Matrix4f(pose.last().pose());
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.gui());                // sans brouillard (le type « lightning » en applique un : le ruban, plus loin que lui, disparaîtrait)
        int segments = 44;
        for (int k = 0; k < 3; k++) {
            double base = k * 2.3 + 0.6, span = 1.5;
            double radius = 165 + k * 22;
            double yBase = 92 + k * 9 - cam.y;
            for (int i = 0; i < segments; i++) {
                double a0 = base + span * i / segments, a1 = base + span * (i + 1) / segments;
                float[] p0 = ribbonPoint(a0, time, k, radius, yBase), p1 = ribbonPoint(a1, time, k, radius, yBase);
                float i0 = aurora * (0.35f + 0.65f * (float) (0.5 + 0.5 * Math.sin(a0 * 4 + time * 0.021 + k))) * edge(i, segments);
                float i1 = aurora * (0.35f + 0.65f * (float) (0.5 + 0.5 * Math.sin(a1 * 4 + time * 0.021 + k))) * edge(i + 1, segments);
                // rangée basse (vert), milieu (vert vif, le plus lumineux), haut (violet, transparent)
                quad(vc, m, p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], 0f, p0[3] * 0.45f, p1[3] * 0.45f, new float[]{0.15f, 1f, 0.45f}, new float[]{0.25f, 1f, 0.55f}, i0, i1, 0f, 0.55f);
                quad(vc, m, p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], p0[3] * 0.45f, p0[3], p1[3], new float[]{0.25f, 1f, 0.55f}, new float[]{0.6f, 0.3f, 1f}, i0, i1, 0.55f, 0f);
            }
        }
        buffers.endBatch(RenderType.gui());
    }

    private static float edge(int i, int n) {
        return Mth.clamp(Math.min(i, n - i) / 6f, 0f, 1f);
    }

    /** Point d'un ruban : x, z relatifs à la caméra, y de la base, et hauteur du rideau. */
    private static float[] ribbonPoint(double a, double time, int k, double radius, double yBase) {
        double r = radius + 14 * Math.sin(a * 5 + time * 0.012 + k);
        double h = 50 + 28 * Math.sin(a * 3 + time * 0.017 + k * 2.0) + 10 * Math.sin(a * 9 - time * 0.03);
        return new float[]{(float) (r * Math.cos(a)), (float) yBase, (float) (r * Math.sin(a)), (float) h};
    }

    /** Deux rangées de sommets entre les hauteurs relatives y0 et y1 (fraction de la hauteur du rideau), couleurs c0 -> c1, opacités a0 / a1. */
    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float yb, float z0, float x1, float yb1, float z1, float hLow, float hHigh0, float hHigh1,
                             float[] c0, float[] c1, float i0, float i1, float aLow, float aHigh) {
        float yl0 = yb + hLow, yl1 = yb1 + hLow, yh0 = yb + hHigh0, yh1 = yb1 + hHigh1;
        vc.addVertex(m, x0, yl0, z0).setColor(c0[0], c0[1], c0[2], aLow * i0);
        vc.addVertex(m, x1, yl1, z1).setColor(c0[0], c0[1], c0[2], aLow * i1);
        vc.addVertex(m, x1, yh1, z1).setColor(c1[0], c1[1], c1[2], aHigh * i1);
        vc.addVertex(m, x0, yh0, z0).setColor(c1[0], c1[1], c1[2], aHigh * i0);
    }

    // =================================================================================================== [42] eau turquoise des hauts-fonds
    /** Redéfinit l'extension cliente de l'eau (textures identiques à celles de NeoForge) avec une teinte qui dépend de la profondeur. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOW)
    public static void onFluidExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
            private static final net.minecraft.resources.ResourceLocation STILL = net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_still");
            private static final net.minecraft.resources.ResourceLocation FLOW = net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_flow");
            private static final net.minecraft.resources.ResourceLocation OVERLAY = net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_overlay");
            private static final net.minecraft.resources.ResourceLocation UNDERWATER = net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");

            @Override
            public net.minecraft.resources.ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public net.minecraft.resources.ResourceLocation getFlowingTexture() {
                return FLOW;
            }

            @Override
            public net.minecraft.resources.ResourceLocation getOverlayTexture() {
                return OVERLAY;
            }

            @Override
            public net.minecraft.resources.ResourceLocation getRenderOverlayTexture(Minecraft mc) {
                return UNDERWATER;
            }

            @Override
            public int getTintColor() {
                return 0xFF3F76E4;
            }

            @Override
            public int getTintColor(FluidState state, net.minecraft.world.level.BlockAndTintGetter level, BlockPos pos) {
                return 0xFF000000 | waterColor(level, pos);
            }
        }, net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value());
    }

    /** Couleur de l'eau : celle du biome, sauf près des côtes et dans les rivières où elle passe du turquoise (peu profond) au bleu sombre (profond). */
    static int waterColor(net.minecraft.world.level.BlockAndTintGetter level, BlockPos pos) {
        int base = BiomeColors.getAverageWaterColor(level, pos);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return base;
        }
        Holder<Biome> b = mc.level.getBiome(pos);
        if (!(b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_BEACH) || b.is(BiomeTags.IS_RIVER))) {
            return base;
        }
        BlockPos.MutableBlockPos m = pos.mutable();
        int depth = 0;
        while (depth < 12 && level.getFluidState(m).is(FluidTags.WATER)) {
            depth++;
            m.move(Direction.DOWN);
        }
        float t = Mth.clamp((depth - 1) / 8f, 0f, 1f);
        t = t * t * (3 - 2 * t);
        int shallow = 0x4FE3D2;                                              // turquoise
        int deep = ((int) (((base >> 16) & 255) * 0.78) << 16) | ((int) (((base >> 8) & 255) * 0.8) << 8) | (int) ((base & 255) * 0.88);
        int r = (int) Mth.lerp(t, (shallow >> 16) & 255, (deep >> 16) & 255);
        int g = (int) Mth.lerp(t, (shallow >> 8) & 255, (deep >> 8) & 255);
        int bl = (int) Mth.lerp(t, shallow & 255, deep & 255);
        return (r << 16) | (g << 8) | bl;
    }

    // =================================================================================================== [29] empreintes et traces de pneus
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!e.level().isClientSide || !(e.level() instanceof ClientLevel level)) {
            return;
        }
        boolean person = e instanceof Player pl && !pl.isSpectator() && !pl.isPassenger() && !pl.isSwimming();
        boolean vehicle = e instanceof fr.modeminecraft.entity.VehicleEntity
                || e instanceof fr.modeminecraft.entity.military.MilVehicleEntity mv && !mv.type().plane() && !mv.type().heli() && !mv.type().naval();
        if ((!person && !vehicle) || !e.onGround() || e.isInWater()) {
            return;
        }
        double[] last = LAST_PRINT.computeIfAbsent(e.getId(), k -> new double[]{e.getX(), e.getZ(), 0});
        double dx = e.getX() - last[0], dz = e.getZ() - last[1];
        double step = person ? 0.9 : 0.8;
        if (dx * dx + dz * dz < step * step) {
            return;
        }
        BlockPos feet = e.blockPosition();
        BlockState at = level.getBlockState(feet);
        BlockPos groundPos = at.is(Blocks.SNOW) ? feet : feet.below();
        BlockState ground = level.getBlockState(groundPos);
        boolean soft = ground.is(BlockTags.SAND) || ground.is(Blocks.SNOW) || ground.is(Blocks.SNOW_BLOCK) || ground.is(Blocks.MUD) || ground.is(Blocks.DIRT)
                || ground.is(Blocks.COARSE_DIRT) || ground.is(Blocks.PODZOL) || ground.is(Blocks.SOUL_SAND) || ground.is(Blocks.RED_SAND);
        last[0] = e.getX();
        last[1] = e.getZ();
        if (!soft) {
            return;
        }
        float tone = ground.is(Blocks.SNOW) || ground.is(Blocks.SNOW_BLOCK) ? 0.62f : ground.is(BlockTags.SAND) ? 0.5f : 0.3f;
        double len = Math.sqrt(dx * dx + dz * dz);
        double ux = dx / len, uz = dz / len;
        double yaw = Math.atan2(-ux, uz);                                    // direction du déplacement
        double y = groundPos.getY() + (ground.is(Blocks.SNOW) ? ground.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) / 8.0 : 1.0) + 0.012;
        double px = -uz, pz = ux;                                            // perpendiculaire
        if (person) {
            last[2] = 1 - last[2];
            double side = last[2] > 0.5 ? 0.13 : -0.13;
            level.addParticle(ModParticles.FOOTPRINT.get(), true, e.getX() + px * side, y, e.getZ() + pz * side, yaw, last[2] > 0.5 ? 1 : 0, tone);
        } else {
            double w = 0.62;
            level.addParticle(ModParticles.TIRE_TRACK.get(), true, e.getX() + px * w, y, e.getZ() + pz * w, yaw, 0, tone);
            level.addParticle(ModParticles.TIRE_TRACK.get(), true, e.getX() - px * w, y, e.getZ() - pz * w, yaw, 0, tone);
        }
    }
}

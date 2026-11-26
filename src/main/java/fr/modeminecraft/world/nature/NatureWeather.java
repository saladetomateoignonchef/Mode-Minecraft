package fr.modeminecraft.world.nature;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModBlocks;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Météo côté serveur : [7] orages où la foudre vise les points hauts exposés (cimes d'arbres isolés, sommets rocheux) et noircit le bois et la roche
 * touchés ; [8] flaques d'eau temporaires sur les chemins de terre et les roches plates pendant les averses.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class NatureWeather {
    private static final String MARK = "mm_retargeted";

    // =================================================================================================== [7] foudre ciblée
    @SubscribeEvent
    public static void onLightning(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt bolt) || !(event.getLevel() instanceof ServerLevel level) || bolt.getTags().contains(MARK)) {
            return;
        }
        bolt.addTag(MARK);
        BlockPos from = bolt.blockPosition();
        CityPlanner.Env env = new CityPlanner.Env(level.getSeed(), level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level, level.registryAccess());
        if (CityPlanner.cityAtCached(env, from.getX(), from.getZ(), 8) != null) {
            return;                                                          // jamais dans une ville : on laisse la foudre normale
        }
        // le point le plus haut et le plus exposé dans un rayon de 24 blocs (les troncs et feuillages isolés comptent davantage)
        BlockPos target = null;
        int bestScore = Integer.MIN_VALUE;
        for (int dx = -24; dx <= 24; dx += 3) {
            for (int dz = -24; dz <= 24; dz += 3) {
                BlockPos column = from.offset(dx, 0, dz);
                if (!level.hasChunkAt(column)) {
                    continue;
                }
                BlockPos hit = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column).below();
                BlockState top = level.getBlockState(hit);
                if (!level.getFluidState(hit).isEmpty()) {
                    continue;
                }
                int score = hit.getY() * 4 - (Math.abs(dx) + Math.abs(dz)) / 3 + (top.is(BlockTags.LOGS) || top.is(BlockTags.LEAVES) ? 6 : 0);
                if (score > bestScore) {
                    bestScore = score;
                    target = hit;
                }
            }
        }
        if (target == null || CityPlanner.cityAtCached(env, target.getX(), target.getZ(), 8) != null) {
            return;
        }
        bolt.setPos(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5);
        scorch(level, target, level.getRandom());
    }

    /** Trace calcinée : le bois frappé devient du charbon, la roche de la basalte ; les feuilles voisines brûlent. */
    private static void scorch(ServerLevel level, BlockPos target, RandomSource rnd) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy >= -2; dy--) {
                    BlockPos p = target.offset(dx, dy, dz);
                    if (dx * dx + dz * dz + dy * dy > 3 || rnd.nextInt(3) == 0) {
                        continue;
                    }
                    BlockState s = level.getBlockState(p);
                    if (s.is(BlockTags.LOGS)) {
                        level.setBlock(p, Blocks.COAL_BLOCK.defaultBlockState(), 3);
                    } else if (s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.COBBLESTONE) || s.is(BlockTags.STONE_BRICKS)) {
                        level.setBlock(p, (rnd.nextBoolean() ? Blocks.BLACKSTONE : Blocks.SMOOTH_BASALT).defaultBlockState(), 3);
                    } else if (s.is(BlockTags.LEAVES) && dy >= -1) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX() + 0.5, target.getY() + 1.2, target.getZ() + 0.5, 30, 0.6, 0.5, 0.6, 0.3);
    }

    // =================================================================================================== [8] flaques après la pluie
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || !level.isRaining() || level.getGameTime() % 40 != 0) {
            return;
        }
        RandomSource rnd = level.getRandom();
        Block puddle = ModBlocks.realism("water_puddle");
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) {
                continue;
            }
            for (int i = 0; i < 6; i++) {
                BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.blockPosition().offset(rnd.nextInt(81) - 40, 0, rnd.nextInt(81) - 40));
                if (!level.isLoaded(pos) || !level.isRainingAt(pos) || !level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) {
                    continue;
                }
                BlockState ground = level.getBlockState(pos.below());
                boolean soft = ground.is(Blocks.DIRT_PATH) || ground.is(Blocks.DIRT) || ground.is(Blocks.COARSE_DIRT) || ground.is(Blocks.PODZOL) || ground.is(Blocks.GRAVEL)
                        || ground.is(Blocks.STONE) || ground.is(Blocks.ANDESITE) || ground.is(Blocks.GRANITE) || ground.is(Blocks.DIORITE) || ground.is(Blocks.SMOOTH_STONE);
                if (!soft || !flat(level, pos)) {
                    continue;
                }
                level.setBlock(pos, puddle.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(rnd)), 3);
                level.scheduleTick(pos, puddle, 120);
            }
        }
    }

    /** Creux plat : les quatre cases voisines ont le même niveau de sol. */
    private static boolean flat(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = pos.relative(d);
            BlockState under = level.getBlockState(n.below());
            if (!level.getBlockState(n).isAir() || under.isAir() || !under.getFluidState().isEmpty()) {
                return false;                                                // chemin de terre compris (dalle de 15/16 : pas une face pleine)
            }
        }
        return true;
    }

    private NatureWeather() {}
}

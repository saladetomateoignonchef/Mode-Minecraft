package fr.modeminecraft.entity.wild;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Apparition de la faune sauvage autour des joueurs (une tentative toutes les 5 secondes par joueur), selon le biome, avec un plafond par espèce
 * dans un rayon de 128 blocs. Jamais dans une ville ; les animaux disparaissent quand ils sont loin (non persistants).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class WildlifeSpawner {
    private WildlifeSpawner() {}

    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 100 != 17) {
            return;
        }
        RandomSource rnd = level.getRandom();
        CityPlanner.Env env = new CityPlanner.Env(level.getSeed(), level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level, level.registryAccess());
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || rnd.nextInt(3) == 0) {
                continue;
            }
            WildlifeEntity.Kind kind = WildlifeEntity.Kind.values()[rnd.nextInt(WildlifeEntity.Kind.values().length)];
            double a = rnd.nextDouble() * Math.PI * 2, d = 30 + rnd.nextInt(32);
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            if (!level.hasChunkAt(new BlockPos(x, 64, z)) || CityPlanner.cityAtCached(env, x, z, 12) != null) {
                continue;
            }
            int cap = switch (kind) {
                case EAGLE -> 2;
                case GULL -> 5;
                case SCORPION, SNAKE -> 3;
                case DEER -> 6;
            };
            int near = level.getEntitiesOfClass(WildlifeEntity.class, p.getBoundingBox().inflate(128), e -> e.kind == kind).size();
            if (near >= cap) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockState gs = level.getBlockState(ground);
            Holder<Biome> biome = level.getBiome(ground);
            boolean water = !level.getFluidState(ground).isEmpty();
            boolean ok = switch (kind) {
                case EAGLE -> (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_BADLANDS)) && !water;
                case GULL -> biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_OCEAN);
                case SCORPION -> (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS)) && !water && (gs.is(BlockTags.SAND) || gs.is(BlockTags.TERRACOTTA));
                case SNAKE -> (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(Biomes.SWAMP)) && !water && gs.isSolid();
                case DEER -> (biome.is(BiomeTags.IS_FOREST)) && !water && gs.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            };
            if (!ok) {
                continue;
            }
            int group = switch (kind) {
                case GULL -> 2 + rnd.nextInt(2);
                case DEER -> 3 + rnd.nextInt(2);
                default -> 1;
            };
            for (int i = 0; i < group; i++) {
                WildlifeEntity e = ModEntities.WILD.get(kind).get().create(level);
                if (e == null) {
                    continue;
                }
                double px = x + 0.5 + rnd.nextInt(5) - 2, pz = z + 0.5 + rnd.nextInt(5) - 2;
                double py = switch (kind) {
                    case EAGLE -> y + 30;
                    case GULL -> Math.max(y, level.getSeaLevel()) + 7;
                    default -> level.getHeight(Heightmap.Types.WORLD_SURFACE, (int) px, (int) pz);
                };
                e.moveTo(px, py, pz, rnd.nextFloat() * 360f, 0f);
                if (kind.flier()) {
                    e.setAnchor(x + 0.5, z + 0.5, kind == WildlifeEntity.Kind.EAGLE ? 26 + rnd.nextInt(14) : 8 + rnd.nextInt(8));
                }
                level.addFreshEntity(e);
            }
        }
    }
}

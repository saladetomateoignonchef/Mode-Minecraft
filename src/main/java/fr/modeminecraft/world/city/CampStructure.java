package fr.modeminecraft.world.city;

import com.mojang.serialization.MapCodec;
import fr.modeminecraft.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Campement abandonné de survivants / d'éclaireurs : feu de camp éteint ou fumant, rondins, caisses, 2 à 3 tentes
 * avec sac de couchage et coffre de survie. Aucun habitant. Jamais dans l'eau, ni dans une ville.
 */
public class CampStructure extends Structure {
    public static final MapCodec<CampStructure> CODEC = simpleCodec(CampStructure::new);

    public CampStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CAMP.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int cx = cp.getMiddleBlockX(), cz = cp.getMiddleBlockZ();
        var gen = ctx.chunkGenerator();
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int[] o : new int[][]{{-7, -7}, {7, -7}, {-7, 7}, {7, 7}, {0, 0}}) {
            int floor = gen.getFirstOccupiedHeight(cx + o[0], cz + o[1], Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
            int surf = gen.getFirstOccupiedHeight(cx + o[0], cz + o[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            if (surf > floor + 1 || floor <= gen.getSeaLevel()) {
                return Optional.empty();                                   // eau
            }
            min = Math.min(min, floor);
            max = Math.max(max, floor);
        }
        if (max - min > 6) {
            return Optional.empty();                                       // trop pentu
        }
        var env = new CityPlanner.Env(ctx.seed(), gen, ctx.randomState(), ctx.heightAccessor(), ctx.registryAccess());
        if (CityPlanner.cityAt(env, cx, cz, 48) != null) {
            return Optional.empty();
        }
        int baseY = gen.getFirstOccupiedHeight(cx, cz, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
        long seed = ctx.random().nextLong();
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        return Optional.of(new GenerationStub(new BlockPos(cx, baseY, cz), b ->
                b.addPiece(new CampPiece(new BoundingBox(x0, baseY - 4, z0, x0 + 15, baseY + 10, z0 + 15), baseY, seed))));
    }
}

package fr.modeminecraft.world.city;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.modeminecraft.registry.ModStructures;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/**
 * Placement des villes : un centre par région (comme random_spread, donc /locate fonctionne), plus une grille de
 * « districts » autour du centre. Minecraft ne construit les pièces d'une structure qu'à 8 chunks au plus de son
 * départ ; une mégapole de 31 x 31 chunks est donc découpée en districts de `district` chunks, chacun étant un départ
 * de structure qui ne construit que sa part du plan commun de la ville.
 */
public class CityPlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<CityPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> placementCodec(i)
            .and(Codec.intRange(1, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing))
            .and(Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation))
            .and(Codec.intRange(1, 17).fieldOf("district").forGetter(p -> p.district))
            .apply(i, CityPlacement::new));

    /** Côté d'un district (en chunks) et nombre de districts de chaque côté du centre. */
    public final int district, reach;

    public CityPlacement(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt, Optional<ExclusionZone> exclusion,
                         int spacing, int separation, int district) {
        super(locateOffset, method, frequency, salt, exclusion, spacing, separation, RandomSpreadType.LINEAR);
        this.district = district;
        this.reach = 1;                                    // 3 x 3 districts au plus (33 x 33 chunks)
    }

    /** Centre de la ville de la région (rx, rz). */
    public ChunkPos center(long seed, int rx, int rz) {
        return getPotentialStructureChunk(seed, rx * spacing(), rz * spacing());
    }

    /** Si ce chunk est le départ d'un district : {région x, région z, i, j} (i, j = district par rapport au centre). */
    public int[] districtAt(long seed, int x, int z) {
        int rx = Math.floorDiv(x, spacing()), rz = Math.floorDiv(z, spacing());
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                ChunkPos c = center(seed, rx + di, rz + dj);
                int ox = x - c.x, oz = z - c.z;
                if (ox % district == 0 && oz % district == 0 && Math.abs(ox) <= reach * district && Math.abs(oz) <= reach * district) {
                    return new int[]{rx + di, rz + dj, ox / district, oz / district};
                }
            }
        }
        return null;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        return districtAt(state.getLevelSeed(), x, z) != null;
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModStructures.CITY_PLACEMENT.get();
    }
}

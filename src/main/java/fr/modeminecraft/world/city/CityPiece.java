package fr.modeminecraft.world.city;

import fr.modeminecraft.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Une parcelle d'une ville (un chunk) : route, bâtiment, place... construite par {@link CityGen}. */
public class CityPiece extends StructurePiece {
    private final String variant, type;
    private final int dx, dz, rad, baseY;
    private final long seed;

    public CityPiece(BoundingBox box, String variant, String type, int dx, int dz, int rad, int baseY, long seed) {
        super(ModStructures.CITY_PIECE.get(), 0, box);
        this.variant = variant;
        this.type = type;
        this.dx = dx;
        this.dz = dz;
        this.rad = rad;
        this.baseY = baseY;
        this.seed = seed;
    }

    public CityPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.CITY_PIECE.get(), tag);
        variant = tag.getString("Variant");
        type = tag.getString("Type");
        dx = tag.getInt("Dx");
        dz = tag.getInt("Dz");
        rad = tag.getInt("Rad");
        baseY = tag.getInt("BaseY");
        seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putString("Variant", variant);
        tag.putString("Type", type);
        tag.putInt("Dx", dx);
        tag.putInt("Dz", dz);
        tag.putInt("Rad", rad);
        tag.putInt("BaseY", baseY);
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        CityLayout layout = new CityLayout(variant, rad, seed);
        CityLayout.Plot plot = layout.plot(dx, dz);
        if (plot == null) {
            return;
        }
        BoundingBox box = getBoundingBox();
        // origine du bâtiment (chunk d'ancrage pour les bâtiments sur plusieurs chunks)
        int ox = box.minX() - (dx - plot.ax()) * 16, oz = box.minZ() - (dz - plot.az()) * 16;
        RandomSource r = RandomSource.create(seed ^ (plot.ax() * 4987142L) ^ (plot.az() * 5947611L) ^ type.hashCode());
        B b = new B(level, chunkBox, ox, baseY, oz, r, generator.getSeaLevel());
        b.part(dx - plot.ax(), dz - plot.az(), plot.w() * 16, plot.h() * 16);
        CityGen.build(b, layout, plot, variant, dx, dz, rad);
    }
}

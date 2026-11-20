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
        // tour de brouillage sur le toit du palais du dictateur, de la base secrète (radar) et de la station polaire
        if (dx == plot.ax() && dz == plot.az() && (plot.type().equals("palace") || plot.type().equals("radar") || plot.type().equals("dome"))) {
            jammer(new B(level, chunkBox, box.minX(), baseY, box.minZ(), r, generator.getSeaLevel()));
        }
        // un transformateur haute tension par quartier (blocs de 3 x 3 chunks), dans une ruelle contre un mur
        if (Math.floorMod(dx, 3) == 1 && Math.floorMod(dz, 3) == 1) {
            transformer(new B(level, chunkBox, box.minX(), baseY, box.minZ(), r, generator.getSeaLevel()));
        }
    }

    private static final int[][] SPOTS = {{1, 1}, {14, 1}, {1, 14}, {14, 14}, {7, 1}, {1, 7}, {14, 7}, {7, 14}, {4, 4}, {11, 11}, {4, 11}, {11, 4}};

    private static void jammer(B b) {
        for (int[] c : new int[][]{{8, 8}, {4, 4}, {11, 11}, {4, 11}, {11, 4}}) {
            for (int y = 80; y >= -2; y--) {
                var s = b.get(c[0], y, c[1]);
                if (s.isAir()) {
                    continue;
                }
                if (s.isFaceSturdy(b.level, b.pos(c[0], y, c[1]), net.minecraft.core.Direction.UP) && b.get(c[0], y + 1, c[1]).isAir()
                        && b.get(c[0], y + 2, c[1]).isAir()) {
                    b.set(c[0], y + 1, c[1], fr.modeminecraft.registry.ModBlocks.JAMMER_TOWER.get().defaultBlockState());
                    return;
                }
                break;
            }
        }
    }

    private static void transformer(B b) {
        int[] best = null;
        net.minecraft.core.Direction bestFacing = net.minecraft.core.Direction.NORTH;
        int bestScore = 0;
        for (int[] c : SPOTS) {
            for (int y = -3; y <= 10; y++) {
                int x = c[0], z = c[1];
                if (!b.get(x, y, z).isAir() || !b.get(x, y + 1, z).isAir()
                        || !b.get(x, y - 1, z).isFaceSturdy(b.level, b.pos(x, y - 1, z), net.minecraft.core.Direction.UP)) {
                    continue;
                }
                boolean open = true;
                for (int k = 2; k <= 7 && open; k++) {
                    open = b.get(x, y + k, z).isAir();
                }
                net.minecraft.core.Direction facing = null;
                for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    if (b.get(x + d.getStepX(), y, z + d.getStepZ()).isSolid()) {
                        facing = d.getOpposite();                // dos au mur
                        break;
                    }
                }
                int score = (open ? 2 : 0) + (facing != null ? 1 : 0);
                if (score > bestScore) {
                    bestScore = score;
                    best = new int[]{x, y, z};
                    bestFacing = facing != null ? facing : net.minecraft.core.Direction.NORTH;
                }
                break;
            }
            if (bestScore == 3) {
                break;
            }
        }
        if (best != null && bestScore >= 2) {
            b.set(best[0], best[1], best[2], fr.modeminecraft.registry.ModBlocks.HIGH_VOLTAGE_TRANSFORMER.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, bestFacing));
        }
    }
}

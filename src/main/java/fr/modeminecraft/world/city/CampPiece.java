package fr.modeminecraft.world.city;

import fr.modeminecraft.item.CampingTentItem;
import fr.modeminecraft.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Construction d'un campement abandonné (un chunk). */
public class CampPiece extends StructurePiece {
    private final int baseY;
    private final long seed;

    public CampPiece(BoundingBox box, int baseY, long seed) {
        super(ModStructures.CAMP_PIECE.get(), 0, box);
        this.baseY = baseY;
        this.seed = seed;
    }

    public CampPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.CAMP_PIECE.get(), tag);
        baseY = tag.getInt("BaseY");
        seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("BaseY", baseY);
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BoundingBox box = getBoundingBox();
        RandomSource r = RandomSource.create(seed);
        B b = new B(level, chunkBox, box.minX(), baseY, box.minZ(), r, generator.getSeaLevel());
        // sol du campement : on garde le revêtement naturel (herbe, sable, neige...), on dégage arbres et buissons
        BlockState surface = b.get(8, -1, 8);
        if (surface.isAir() || !surface.getFluidState().isEmpty() || surface.is(net.minecraft.tags.BlockTags.LEAVES)) {
            surface = Blocks.COARSE_DIRT.defaultBlockState();
        }
        b.ground(1, 1, 14, 14, surface, Blocks.DIRT.defaultBlockState(), 12);
        b.fill(6, -1, 6, 10, -1, 10, Blocks.COARSE_DIRT);
        // feu de camp éteint (ou braises qui fument encore)
        b.set(8, 0, 8, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, r.nextFloat() < 0.3f));
        for (int[] l : new int[][]{{8, 5, 0}, {8, 11, 0}, {5, 8, 1}, {11, 8, 1}}) {                    // rondins pour s'asseoir
            if (r.nextFloat() < 0.75f) {
                var axis = l[2] == 0 ? net.minecraft.core.Direction.Axis.X : net.minecraft.core.Direction.Axis.Z;
                BlockState log = Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
                b.set(l[0], 0, l[1], log);
                b.set(l[0] + (l[2] == 0 ? 1 : 0), 0, l[1] + (l[2] == 1 ? 1 : 0), log);
            }
        }
        // 2 ou 3 tentes tournées vers le feu, chacune avec son sac de couchage et un coffre caché
        int[][] spots = {{3, 1, 0}, {12, 3, 1}, {3, 13, 2}};
        Direction[] fronts = {Direction.SOUTH, Direction.WEST, Direction.NORTH};
        int tents = 2 + r.nextInt(2);
        for (int i = 0; i < tents; i++) {
            BlockPos origin = b.pos(spots[i][0] + (i == 1 ? 0 : 2), 0, spots[i][1] + (i == 0 ? 3 : i == 2 ? -3 : 2));
            if (!chunkBox.isInside(origin)) {
                continue;
            }
            BlockPos chestAt = CampingTentItem.build(level, origin, fronts[i], true, true);
            if (chunkBox.isInside(chestAt)) {
                int lx = chestAt.getX() - box.minX(), lz = chestAt.getZ() - box.minZ();
                b.chest(lx, 0, lz, fronts[i], "camp_supplies", false);
            }
        }
        // caisses et lanterne
        b.set(13, 0, 12, Blocks.BARREL.defaultBlockState());
        b.set(13, 1, 12, Blocks.LANTERN.defaultBlockState());
        if (r.nextFloat() < 0.5f) {
            b.set(2, 0, 8, Blocks.BARREL.defaultBlockState());
        }
    }
}

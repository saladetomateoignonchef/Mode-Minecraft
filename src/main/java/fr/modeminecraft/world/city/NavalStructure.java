package fr.modeminecraft.world.city;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.modeminecraft.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * Structures maritimes (cahier des charges « Guerre navale ») :
 * - « oil_rig » : plateforme pétrolière des Mercenaires Offshore, en océan profond à plus de 250 blocs des côtes ;
 * - « carrier » : groupe aéronaval (USS Gerald R. Ford ou Amiral Kouznetsov selon les eaux, chaudes ou froides) au
 *   mouillage en océan profond, avec ses chasseurs sur le pont et deux navires d'escorte ;
 * - « naval_base » : base navale côtière (américaine, ou soviétique en eaux froides) : quai, jetées, grues, dépôt de
 *   carburant et de torpilles, batteries côtières, cale sèche couverte (50 x 22 x 16), navires à quai.
 * Toutes exigent une étendue d'eau libre d'au moins 140 x 140 blocs et 12 de fond (sinon pas de génération).
 */
public class NavalStructure extends Structure {
    public static final MapCodec<NavalStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(settingsCodec(i),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind)).apply(i, NavalStructure::new));

    private final String kind;
    /** Tests : génère ici sans contrôle du terrain (null = normal). */
    public static Boolean forceSoviet;

    public NavalStructure(StructureSettings settings, String kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.NAVAL.get();
    }

    private static int floor(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
    }

    /** Étendue d'eau libre de 140 x 140 blocs, 12 blocs de fond au moins, centrée en (x, z). */
    private static boolean openWater(GenerationContext ctx, int x, int z, int sea) {
        for (int dx = -70; dx <= 70; dx += 35) {
            for (int dz = -70; dz <= 70; dz += 35) {
                if (floor(ctx, x + dx, z + dz) > sea - 12) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Aucune terre émergée à moins de `r` blocs (16 directions, deux cercles). */
    private static boolean farFromCoast(GenerationContext ctx, int x, int z, int sea, int r) {
        for (int k = 0; k < 16; k++) {
            double a = k * Math.PI / 8;
            for (int rr : new int[]{r / 2, r}) {
                if (floor(ctx, x + (int) (Math.cos(a) * rr), z + (int) (Math.sin(a) * rr)) >= sea - 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean cold(GenerationContext ctx, int x, int z) {
        var b = ctx.biomeSource().getNoiseBiome(x >> 2, 16, z >> 2, ctx.randomState().sampler());
        return b.value().getBaseTemperature() < 0.3f;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int cx = cp.getMiddleBlockX(), cz = cp.getMiddleBlockZ();
        int sea = ctx.chunkGenerator().getSeaLevel();
        long seed = ctx.random().nextLong();
        boolean force = forceSoviet != null;
        switch (kind) {
            case "oil_rig" -> {
                if (!force && (!openWater(ctx, cx, cz, sea) || !farFromCoast(ctx, cx, cz, sea, 250))) {
                    return Optional.empty();
                }
                int ox = cx - 32, oz = cz - 32;
                return Optional.of(new GenerationStub(new BlockPos(cx, sea, cz), b -> pieces(b, kind, false, ox, oz, 0,
                        -24, -24, 88, 88, sea - 70, sea + 52, seed)));
            }
            case "carrier" -> {
                if (!force && (!openWater(ctx, cx, cz, sea) || !farFromCoast(ctx, cx, cz, sea, 180))) {
                    return Optional.empty();
                }
                boolean soviet = force ? forceSoviet : cold(ctx, cx, cz);
                int ox = cx - 65, oz = cz - 17;
                return Optional.of(new GenerationStub(new BlockPos(cx, sea, cz), b -> pieces(b, kind, soviet, ox, oz, 0,
                        -4, -60, 136, 96, sea - 14, sea + 40, seed)));
            }
            default -> {
                // base navale : le port (eau libre) ici, la terre dans l'une des quatre directions
                if (!force && !openWater(ctx, cx, cz, sea)) {
                    return Optional.empty();
                }
                int dir = -1;
                int[][] dirs = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
                for (int k = 0; k < 4 && !force; k++) {
                    int[] d = dirs[k];
                    // le quai s'étend de 70 à 130 blocs du centre du port (vers la terre)
                    if (floor(ctx, cx + d[0] * 115, cz + d[1] * 115) > sea && floor(ctx, cx + d[0] * 160, cz + d[1] * 160) > sea - 2) {
                        dir = k;
                        break;
                    }
                }
                if (force) {
                    dir = 0;
                }
                if (dir < 0) {
                    return Optional.empty();
                }
                boolean soviet = force ? forceSoviet : cold(ctx, cx, cz);
                int land = dir;
                return Optional.of(new GenerationStub(new BlockPos(cx, sea, cz), b -> pieces(b, kind, soviet, cx, cz, land,
                        0, -60, 160, 90, sea - 40, sea + 34, seed)));
            }
        }
    }

    /**
     * Pièces d'un chunk qui couvrent l'emprise locale [u0, u1] x [v0, v1] (repère tourné de la structure).
     * Pour la base navale, (ox, oz) est le centre du port et `dir` la direction de la terre.
     */
    private static void pieces(StructurePiecesBuilder builder, String kind, boolean soviet, int ox, int oz, int dir,
                               int u0, int v0, int u1, int v1, int y0, int y1, long seed) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int u : new int[]{u0, u1}) {
            for (int v : new int[]{v0, v1}) {
                int[] w = NavalPiece.toWorld(kind, ox, oz, dir, u, v);
                minX = Math.min(minX, w[0]);
                maxX = Math.max(maxX, w[0]);
                minZ = Math.min(minZ, w[1]);
                maxZ = Math.max(maxZ, w[1]);
            }
        }
        for (int c = minX >> 4; c <= maxX >> 4; c++) {
            for (int d = minZ >> 4; d <= maxZ >> 4; d++) {
                BoundingBox box = new BoundingBox(c * 16, y0, d * 16, c * 16 + 15, y1, d * 16 + 15);
                builder.addPiece(new NavalPiece(box, kind, soviet, ox, oz, dir, seed));
            }
        }
    }
}

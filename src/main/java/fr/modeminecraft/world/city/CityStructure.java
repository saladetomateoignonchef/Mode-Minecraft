package fr.modeminecraft.world.city;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.modeminecraft.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * Une ville générée (une structure par famille de biome ; la variante est tirée au hasard selon le cahier des charges) :
 * - plaine : métropole moderne (gratte-ciel, Central Park, commissariat, égouts) ;
 * - désert : cité sous dictature (riads, souk, palais fortifié) ;
 * - forêt / taïga : bastion rebelle arboricole (50 %) ou base militaire secrète (50 %) ;
 * - neige : cité soviétique (70 %), station polaire (15 %), avant-poste minier (15 %) ;
 * - jungle / marais : villa de cartel (80 %) ou bidonville sur pilotis (20 %) ;
 * - badlands : ville frontière (50 %) ou raffinerie (50 %).
 * La ville est découpée en parcelles d'un chunk (routes, bâtiments, places), générées par {@link CityPiece}.
 */
public class CityStructure extends Structure {
    public static final MapCodec<CityStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(settingsCodec(i),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind)).apply(i, CityStructure::new));

    private final String kind;
    /** Tests / commandes : impose la variante et ignore le contrôle du relief (null = normal). */
    public static String forceVariant;

    public CityStructure(StructureSettings settings, String kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CITY.get();
    }

    /** Variante tirée au hasard (proportions du cahier des charges). */
    static String variant(String kind, RandomSource r) {
        float x = r.nextFloat();
        return switch (kind) {
            case "plains" -> "metropolis";
            case "desert" -> "desert";
            case "forest" -> x < 0.5f ? "rebels" : "base";
            case "snow" -> x < 0.7f ? "soviet" : x < 0.85f ? "polar" : "mine";
            case "jungle" -> x < 0.8f ? "cartel" : "stilts";
            case "mesa" -> x < 0.5f ? "frontier" : "refinery";
            default -> kind;
        };
    }

    /** Demi-taille de la ville, en chunks (métropole : 15 x 15 chunks). */
    static int radius(String variant) {
        return switch (variant) {
            case "metropolis" -> 7;
            case "desert" -> 6;
            case "soviet" -> 4;
            case "rebels", "base", "frontier", "refinery" -> 3;
            default -> 2;
        };
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        RandomSource r = ctx.random();
        String variant = forceVariant != null ? forceVariant : variant(kind, r);
        int rad = radius(variant);
        ChunkPos cp = ctx.chunkPos();
        int cx = cp.getMiddleBlockX(), cz = cp.getMiddleBlockZ();
        var gen = ctx.chunkGenerator();
        int sea = gen.getSeaLevel();
        int center = gen.getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int min = center, max = center;
        int water = 0;
        int span = rad * 16;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int x = cx + i * span, z = cz + j * span;
                int h = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
                int s = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
                if (s > h + 1) {
                    water++;
                }
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        int base;
        if (forceVariant != null) {
            base = variant.equals("stilts") ? Math.max(center, sea + 2) : center;
        } else if (variant.equals("stilts")) {
            base = sea + 2;                                   // planchers au-dessus de l'eau
            if (center > sea + 6) {
                return Optional.empty();
            }
        } else {
            int rough = switch (variant) {
                case "mine" -> 55;
                case "frontier", "refinery" -> 45;                    // badlands : relief tourmenté
                case "metropolis", "desert", "soviet" -> 34;
                default -> 40;
            };
            if (water > 3 || center <= sea || max - min > rough) {
                return Optional.empty();                      // trop accidenté ou dans l'eau
            }
            base = variant.equals("mine") ? min + 1 : center;
        }
        final int baseY = base;
        long seed = r.nextLong();
        BlockPos pos = new BlockPos(cx, baseY, cz);
        return Optional.of(new GenerationStub(pos, b -> pieces(b, variant, cp, rad, baseY, seed)));
    }

    private static void pieces(StructurePiecesBuilder builder, String variant, ChunkPos center, int rad, int baseY, long seed) {
        CityLayout layout = new CityLayout(variant, rad, seed);
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                CityLayout.Plot plot = layout.plot(dx, dz);
                if (plot == null) {
                    continue;
                }
                int x0 = (center.x + dx) * 16, z0 = (center.z + dz) * 16;
                BoundingBox box = new BoundingBox(x0, baseY - plot.below(), z0, x0 + 15, baseY + plot.above(), z0 + 15);
                builder.addPiece(new CityPiece(box, variant, plot.type(), dx, dz, rad, baseY, seed));
            }
        }
    }
}

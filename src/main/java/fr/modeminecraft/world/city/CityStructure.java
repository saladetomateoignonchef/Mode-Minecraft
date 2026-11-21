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
    /** Tests : demi-taille imposée (0 = celle de la variante). */
    public static int forceRadius;

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

    /** Demi-taille de la ville, en chunks (métropole : 31 x 31 chunks, cités du désert et soviétique : 25 x 25). */
    static int radius(String variant) {
        return switch (variant) {
            case "metropolis" -> 15;
            case "desert", "soviet" -> 12;
            case "frontier", "base" -> 5;
            case "rebels", "refinery" -> 4;
            default -> 3;
        };
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        CityPlanner.Env env = new CityPlanner.Env(ctx.seed(), ctx.chunkGenerator(), ctx.randomState(), ctx.heightAccessor(), ctx.registryAccess());
        if (forceVariant != null) {
            // tests / commande /place : ville entière centrée ici, variante imposée, relief ignoré
            String variant = forceVariant;
            int rad = forceRadius > 0 ? forceRadius : radius(variant);
            int cx = cp.getMiddleBlockX(), cz = cp.getMiddleBlockZ();
            int center = ctx.chunkGenerator().getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            int baseY = CityPlanner.baseY(env, variant, rad, cx, cz, center, true);
            long seed = ctx.random().nextLong();
            return Optional.of(new GenerationStub(new BlockPos(cx, center, cz),
                    b -> pieces(b, variant, cp, rad, baseY, seed, -rad, rad, -rad, rad)));
        }
        CityPlacement placement = CityPlanner.placement(ctx.registryAccess());
        int[] d = placement == null ? null : placement.districtAt(ctx.seed(), cp.x, cp.z);
        if (d == null) {
            return Optional.empty();
        }
        CityPlanner.City city = CityPlanner.city(env, d[0], d[1]);
        if (city == null || !city.kind().equals(kind)) {
            return Optional.empty();
        }
        // part du plan construite par ce district
        int half = placement.district / 2;
        int x0 = Math.max(-city.rad(), d[2] * placement.district - half), x1 = Math.min(city.rad(), d[2] * placement.district + half);
        int z0 = Math.max(-city.rad(), d[3] * placement.district - half), z1 = Math.min(city.rad(), d[3] * placement.district + half);
        if (x0 > x1 || z0 > z1) {
            return Optional.empty();
        }
        int cx = city.middleX(), cz = city.middleZ();
        int center = ctx.chunkGenerator().getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        // la position sert au contrôle du biome : celle du centre, pour que tous les districts soient d'accord
        return Optional.of(new GenerationStub(new BlockPos(cx, center, cz),
                b -> pieces(b, city.variant(), city.center(), city.rad(), city.baseY(), city.seed(), x0, x1, z0, z1)));
    }

    private static void pieces(StructurePiecesBuilder builder, String variant, ChunkPos center, int rad, int baseY, long seed,
                               int x0, int x1, int z0, int z1) {
        CityLayout layout = new CityLayout(variant, rad, seed);
        for (int dx = x0; dx <= x1; dx++) {
            for (int dz = z0; dz <= z1; dz++) {
                CityLayout.Plot plot = layout.plot(dx, dz);
                if (plot == null) {
                    continue;
                }
                int bx = (center.x + dx) * 16, bz = (center.z + dz) * 16;
                BoundingBox box = new BoundingBox(bx, baseY - plot.below(), bz, bx + 15, baseY + plot.above(), bz + 15);
                builder.addPiece(new CityPiece(box, variant, plot.type(), dx, dz, rad, baseY, seed));
            }
        }
    }
}

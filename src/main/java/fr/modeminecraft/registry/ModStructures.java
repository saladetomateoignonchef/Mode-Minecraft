package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.world.city.CityPiece;
import fr.modeminecraft.world.city.CityStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Structures du mod : les villes (une par famille de biome, variantes tirées au hasard). */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ModeMinecraft.MODID);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, ModeMinecraft.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ModeMinecraft.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<CityStructure>> CITY = TYPES.register("city", () -> () -> CityStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<fr.modeminecraft.world.city.CampStructure>> CAMP =
            TYPES.register("camp", () -> () -> fr.modeminecraft.world.city.CampStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CAMP_PIECE = PIECES.register("camp_piece",
            () -> (StructurePieceType) fr.modeminecraft.world.city.CampPiece::new);
    /** Structures maritimes : plateforme offshore, porte-avions, base navale. */
    public static final DeferredHolder<StructureType<?>, StructureType<fr.modeminecraft.world.city.NavalStructure>> NAVAL =
            TYPES.register("naval", () -> () -> fr.modeminecraft.world.city.NavalStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> NAVAL_PIECE = PIECES.register("naval_piece",
            () -> (StructurePieceType) fr.modeminecraft.world.city.NavalPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CITY_PIECE = PIECES.register("city_piece",
            () -> (StructurePieceType) CityPiece::new);

    /** Placement des villes en districts (mégapoles de plus de 17 chunks de côté). */
    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>,
            net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<fr.modeminecraft.world.city.CityPlacement>> CITY_PLACEMENT =
            PLACEMENTS.register("city_grid", () -> () -> fr.modeminecraft.world.city.CityPlacement.CODEC);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ModeMinecraft.MODID);
    /** Routes entre les villes d'un même biome. */
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, fr.modeminecraft.world.road.RoadFeature> ROAD =
            FEATURES.register("road", fr.modeminecraft.world.road.RoadFeature::new);
    /** Voies ferrées entre les villes frontières du Mesa. */
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, fr.modeminecraft.world.rail.RailFeature> RAIL =
            FEATURES.register("rail", fr.modeminecraft.world.rail.RailFeature::new);
    /** Raccord progressif du relief autour des villes (anneau de 24 blocs). */
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, fr.modeminecraft.world.road.CityBlendFeature> CITY_BLEND =
            FEATURES.register("city_blend", fr.modeminecraft.world.road.CityBlendFeature::new);

    /** Décors de nature (troncs creux, cairns, ruines, épaves...) posés hors des villes selon le biome. */
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, fr.modeminecraft.world.nature.NatureDecorFeature> NATURE_DECOR =
            FEATURES.register("nature_decor", fr.modeminecraft.world.nature.NatureDecorFeature::new);

    private ModStructures() {}
}

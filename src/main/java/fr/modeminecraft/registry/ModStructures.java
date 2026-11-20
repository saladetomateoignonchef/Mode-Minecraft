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
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ModeMinecraft.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<CityStructure>> CITY = TYPES.register("city", () -> () -> CityStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CITY_PIECE = PIECES.register("city_piece",
            () -> (StructurePieceType) CityPiece::new);

    private ModStructures() {}
}

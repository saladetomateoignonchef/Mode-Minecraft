package fr.modeminecraft.block.city;

import fr.modeminecraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Repère des haut-parleurs (permet au client de les trouver vite, chunk par chunk). */
public class AmbientSpeakerBlockEntity extends BlockEntity {
    public AmbientSpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AMBIENT_SPEAKER.get(), pos, state);
    }
}

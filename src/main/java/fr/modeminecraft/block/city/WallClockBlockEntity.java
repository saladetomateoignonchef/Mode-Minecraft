package fr.modeminecraft.block.city;

import fr.modeminecraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Horloge murale : les aiguilles sont dessinées par son rendu, d'après l'heure du monde. */
public class WallClockBlockEntity extends BlockEntity {
    public WallClockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WALL_CLOCK.get(), pos, state);
    }
}

package fr.modeminecraft.block.city;

import fr.modeminecraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Support du rendu animé du feu tricolore (aucune donnée). */
public class TrafficLightBlockEntity extends BlockEntity {
    public TrafficLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAFFIC_LIGHT.get(), pos, state);
    }
}

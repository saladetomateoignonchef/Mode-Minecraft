package fr.modeminecraft.nuclear;

import fr.modeminecraft.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CentrifugeBlockEntity extends BlockEntity {
    public static final int CAPACITY = 64, PER_CORE = 16, DURATION = 1200;
    int ingots, cores, progress;

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CENTRIFUGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CentrifugeBlockEntity be) {
        boolean powered = level.hasNeighborSignal(pos);
        if (!powered || be.ingots < PER_CORE) {
            return;
        }
        be.progress++;
        if (be.progress % 20 == 0) {
            level.playSound(null, pos, SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 0.35f, 1.8f);
            ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0);
        }
        if (be.progress >= DURATION) {
            be.progress = 0;
            be.ingots -= PER_CORE;
            be.cores++;
            level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1f, 1.4f);
        }
        be.setChanged();
    }

    void status(Player p) {
        boolean powered = level != null && level.hasNeighborSignal(worldPosition);
        p.displayClientMessage(Component.translatable("message.modeminecraft.centrifuge.status", ingots, CAPACITY, progress * 100 / DURATION, cores,
                Component.translatable(powered ? "message.modeminecraft.centrifuge.on" : "message.modeminecraft.centrifuge.off"))
                .withStyle(powered ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ingots = tag.getInt("Ingots");
        cores = tag.getInt("Cores");
        progress = tag.getInt("Progress");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Ingots", ingots);
        tag.putInt("Cores", cores);
        tag.putInt("Progress", progress);
    }
}

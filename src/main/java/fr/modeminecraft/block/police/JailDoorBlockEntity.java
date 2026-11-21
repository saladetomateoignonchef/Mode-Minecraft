package fr.modeminecraft.block.police;

import fr.modeminecraft.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Minuterie de détention d'une porte de cellule (moitié basse) : fin de peine et prisonnier. */
public class JailDoorBlockEntity extends BlockEntity {
    public long lockedUntil;
    public UUID prisoner;

    public JailDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.JAIL_DOOR.get(), pos, state);
    }

    /** Enferme le prisonnier : porte fermée et verrouillée pendant `ticks`. */
    public static void lock(Level level, BlockPos lower, long ticks, UUID prisoner) {
        SlidingJailDoorBlock.setOpen(level, lower, false, true);
        if (level.getBlockEntity(lower) instanceof JailDoorBlockEntity be) {
            be.lockedUntil = level.getGameTime() + ticks;
            be.prisoner = prisoner;
            be.sync();
        }
    }

    /** Fin de peine (ou libération par le pupitre / un complice) : la porte se déverrouille et s'ouvre. */
    public static void release(Level level, BlockPos lower) {
        if (level.getBlockEntity(lower) instanceof JailDoorBlockEntity be) {
            if (be.prisoner != null && level.getPlayerByUUID(be.prisoner) instanceof net.minecraft.world.entity.player.Player p) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.jail.released").withStyle(ChatFormatting.GREEN), true);
            }
            be.lockedUntil = 0;
            be.prisoner = null;
            be.sync();
        }
        level.playSound(null, lower, fr.modeminecraft.registry.ModSounds.JAIL_BUZZER.get(), SoundSource.BLOCKS, 1f, 1.2f);
        SlidingJailDoorBlock.setOpen(level, lower, true, false);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, JailDoorBlockEntity be) {
        if (state.getValue(SlidingJailDoorBlock.LOCKED) && be.lockedUntil > 0 && level.getGameTime() >= be.lockedUntil) {
            release(level, pos);
        }
    }

    void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lockedUntil = tag.getLong("LockedUntil");
        prisoner = tag.hasUUID("Prisoner") ? tag.getUUID("Prisoner") : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("LockedUntil", lockedUntil);
        if (prisoner != null) {
            tag.putUUID("Prisoner", prisoner);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("LockedUntil", lockedUntil);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

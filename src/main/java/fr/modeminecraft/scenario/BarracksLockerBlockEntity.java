package fr.modeminecraft.scenario;

import fr.modeminecraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Casier personnel des dortoirs (27 emplacements) : attribué à un soldat, son contenu survit à la mort. */
public class BarracksLockerBlockEntity extends fr.modeminecraft.block.police.PoliceLockerBlockEntity {
    public UUID owner;
    public String ownerName = "";

    public BarracksLockerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARRACKS_LOCKER.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return ownerName.isEmpty() ? Component.translatable("block.modeminecraft.barracks_locker")
                : Component.translatable("block.modeminecraft.barracks_locker.owned", ownerName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
    }
}

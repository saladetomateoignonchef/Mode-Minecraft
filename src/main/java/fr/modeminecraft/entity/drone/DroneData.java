package fr.modeminecraft.entity.drone;

import fr.modeminecraft.item.explosive.GrenadeItem;
import fr.modeminecraft.item.explosive.PlaceableExplosiveItem;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Données d'un drone rangé dans l'inventaire (composant CUSTOM_DATA de l'objet, clé "Drone"). */
public final class DroneData {
    /** Charge acceptée par le drone kamikaze : grenade du mod, C4 / explosif posable, ou roquette RPG. */
    public static boolean fpvCharge(ItemStack s) {
        return s.getItem() instanceof GrenadeItem || s.getItem() instanceof PlaceableExplosiveItem || s.is(ModItems.RPG_ROCKET.get());
    }

    /** Lit les données du drone depuis l'objet (vide si neuf). */
    public static CompoundTag read(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag().getCompound("Drone");
    }

    public static void write(ItemStack stack, CompoundTag drone) {
        CompoundTag root = new CompoundTag();
        root.put("Drone", drone);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(root));
    }

    public static ItemStack payload(CompoundTag drone, HolderLookup.Provider reg) {
        return drone.contains("Payload") ? ItemStack.parseOptional(reg, drone.getCompound("Payload")) : ItemStack.EMPTY;
    }

    public static void setPayload(CompoundTag drone, ItemStack payload, HolderLookup.Provider reg) {
        if (payload.isEmpty()) {
            drone.remove("Payload");
        } else {
            drone.put("Payload", payload.save(reg));
        }
    }

    private DroneData() {}
}

package fr.modeminecraft.mixin;

import fr.modeminecraft.item.tactical.TacticalSlot;
import fr.modeminecraft.item.tactical.TacticalSlots;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Inventaire du joueur : deux cases en plus, à droite du personnage (gilet à côté du plastron, sac à dos dessous). */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
    protected InventoryMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void modeminecraft$tacticalSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        addSlot(new TacticalSlot(owner, TacticalSlots.VEST, 77, 26));
        addSlot(new TacticalSlot(owner, TacticalSlots.BACKPACK, 77, 44));
    }
}

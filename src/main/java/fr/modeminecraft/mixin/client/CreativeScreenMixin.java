package fr.modeminecraft.mixin.client;

import fr.modeminecraft.item.tactical.TacticalSlot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Onglet « inventaire » du mode créatif : les cases gilet et sac à dos à droite du personnage. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenMixin {
    @ModifyArgs(method = "selectTab", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
    private void modeminecraft$placeTacticalSlots(Args args) {
        Slot target = args.get(0);
        if (target instanceof TacticalSlot t) {
            args.set(2, 127 + t.kind * 18);
            args.set(3, 20);
        }
    }
}

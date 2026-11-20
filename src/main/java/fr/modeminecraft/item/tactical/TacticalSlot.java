package fr.modeminecraft.item.tactical;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Case « gilet » ou « sac à dos » ajoutée à l'inventaire du joueur (lit toujours les données actuelles du joueur). */
public class TacticalSlot extends Slot {
    public final int kind;

    public TacticalSlot(Player player, int kind, int x, int y) {
        super(new Wrapper(player), kind, x, y);
        this.kind = kind;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return container instanceof Wrapper w && TacticalSlots.of(w.player).isItemValid(kind, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** Accès aux deux cases du joueur comme à un petit coffre. */
    public static final class Wrapper implements Container {
        final Player player;

        Wrapper(Player player) {
            this.player = player;
        }

        private TacticalSlots h() {
            return TacticalSlots.of(player);
        }

        @Override public int getContainerSize() { return 2; }
        @Override public boolean isEmpty() { return h().getStackInSlot(0).isEmpty() && h().getStackInSlot(1).isEmpty(); }
        @Override public ItemStack getItem(int i) { return h().getStackInSlot(i); }
        @Override public ItemStack removeItem(int i, int n) { return h().extractItem(i, n, false); }
        @Override public ItemStack removeItemNoUpdate(int i) {
            ItemStack s = h().getStackInSlot(i);
            h().setStackInSlot(i, ItemStack.EMPTY);
            return s;
        }
        @Override public void setItem(int i, ItemStack s) { h().setStackInSlot(i, s); }
        @Override public void setChanged() {}
        @Override public boolean stillValid(Player p) { return true; }
        @Override public void clearContent() { h().setStackInSlot(0, ItemStack.EMPTY); h().setStackInSlot(1, ItemStack.EMPTY); }
    }
}

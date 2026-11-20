package fr.modeminecraft.item.tactical;

import fr.modeminecraft.item.VestItem;
import fr.modeminecraft.registry.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Deux cases d'équipement en plus, à côté de l'armure du joueur : le gilet pare-balles (porté par-dessus un plastron)
 * et le sac à dos tactique (18 cases). Sauvegardées avec le joueur.
 */
public class TacticalSlots extends ItemStackHandler {
    public static final int VEST = 0, BACKPACK = 1;

    public TacticalSlots() {
        super(2);
    }

    public static TacticalSlots of(Player player) {
        return player.getData(ModAttachments.TACTICAL);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == VEST ? stack.getItem() instanceof VestItem : stack.getItem() instanceof BackpackItem;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    /** Le gilet porté : dans la case du plastron, ou à défaut dans la case « gilet ». */
    public static ItemStack vest(LivingEntity e) {
        ItemStack chest = e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (chest.getItem() instanceof VestItem || !(e instanceof Player p)) {
            return chest;
        }
        ItemStack v = of(p).getStackInSlot(VEST);
        return v.getItem() instanceof VestItem ? v : ItemStack.EMPTY;
    }

    /** Le gilet est-il dans la case « gilet » (et non dans celle du plastron) ? */
    public static boolean vestInTacticalSlot(LivingEntity e) {
        return e instanceof Player p && !(e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem() instanceof VestItem)
                && of(p).getStackInSlot(VEST).getItem() instanceof VestItem;
    }

    public static ItemStack backpack(Player p) {
        return of(p).getStackInSlot(BACKPACK);
    }
}

package fr.modeminecraft.item.tactical;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Sac à dos tactique : 18 cases en plus. Se porte dans la case « sac à dos » de l'inventaire (touche K pour l'ouvrir)
 * ou s'ouvre en main (clic droit). On ne peut pas mettre un sac à dos dans un sac à dos.
 */
public class BackpackItem extends Item {
    public static final int SIZE = 18;

    public BackpackItem(Properties properties) {
        super(properties.stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            int locked = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
            open(sp, () -> player.getItemInHand(hand), locked);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Ouvre le sac (son contenu est relu et réécrit dans l'objet). lockedSlot = case de l'inventaire à bloquer. */
    public static void open(ServerPlayer player, Supplier<ItemStack> source, int lockedSlot) {
        ItemStack bag = source.get();
        if (!(bag.getItem() instanceof BackpackItem)) {
            return;
        }
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        SimpleContainer box = new SimpleContainer(SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                ItemStack target = source.get();
                if (target.getItem() instanceof BackpackItem) {
                    target.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
                }
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return !(stack.getItem() instanceof BackpackItem) && stack.getItem().canFitInsideContainerItems();
            }
        };
        for (int i = 0; i < SIZE; i++) {
            box.setItem(i, items.get(i));
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8f, 1.1f);
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new Menu(id, inv, box, lockedSlot),
                Component.translatable("container.modeminecraft.backpack")));
    }

    /** Menu du sac : refuse les sacs à dos et bloque la case d'où le sac a été ouvert. */
    static final class Menu extends ChestMenu {
        private final int lockedSlot;
        private final SimpleContainer box;

        Menu(int id, Inventory inv, SimpleContainer box, int lockedSlot) {
            super(MenuType.GENERIC_9x2, id, inv, box, 2);
            this.box = box;
            this.lockedSlot = lockedSlot;
        }

        private boolean locked(int slotId) {
            if (slotId < 0 || slotId >= slots.size()) {
                return false;
            }
            Slot s = slots.get(slotId);
            return s.container instanceof Inventory && s.getContainerSlot() == lockedSlot;
        }

        @Override
        public void clicked(int slotId, int button, ClickType type, Player player) {
            if (locked(slotId) || type == ClickType.SWAP && button == lockedSlot) {
                return;                                              // on ne déplace pas le sac ouvert
            }
            if (slotId >= 0 && slotId < SIZE && getCarried().getItem() instanceof BackpackItem) {
                return;
            }
            if (type == ClickType.SWAP && slotId >= 0 && slotId < SIZE
                    && player.getInventory().getItem(button).getItem() instanceof BackpackItem) {
                return;
            }
            super.clicked(slotId, button, type, player);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            if (index >= SIZE && slots.get(index).getItem().getItem() instanceof BackpackItem) {
                return ItemStack.EMPTY;
            }
            return super.quickMoveStack(player, index);
        }

        @Override
        public void removed(Player player) {
            super.removed(player);
            box.setChanged();
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int n = 0;
        for (ItemStack s : stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems()) {
            n++;
        }
        tooltip.add(Component.translatable("tooltip.modeminecraft.backpack.slots", n, SIZE).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.backpack.how").withStyle(ChatFormatting.DARK_GRAY));
    }
}

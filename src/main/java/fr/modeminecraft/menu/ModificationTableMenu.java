package fr.modeminecraft.menu;

import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.AttachmentSlot;
import fr.modeminecraft.item.gun.GunAttachments;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.registry.ModBlocks;
import fr.modeminecraft.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu de la table de modification.
 *
 * Emplacement 0 = l'arme. Emplacements 1 à 6 = accessoires (viseur, bouche, poignée avant, chargeur,
 * poignée pistolet, crosse).
 * Les emplacements d'accessoires sont "virtuels" : ils montrent ce qui est monté sur l'arme,
 * et chaque changement est immédiatement écrit dans l'arme. Quand on retire l'arme,
 * ses accessoires partent avec elle (ils ne sont jamais dupliqués).
 */
public class ModificationTableMenu extends AbstractContainerMenu {
    public static final int BUTTON_PREVIOUS_SKIN = 0;
    public static final int BUTTON_NEXT_SKIN = 1;

    /** Position des emplacements dans l'écran. */
    public static final int GUN_X = 44, GUN_Y = 38;
    /** Position de chaque emplacement d'accessoire (même ordre que AttachmentSlot). */
    public static final int[][] PART_POS = {
            {44, 14},  // viseur : au-dessus
            {72, 38},  // bouche du canon : à droite
            {72, 62},  // poignée avant : en bas à droite (sous le canon)
            {44, 62},  // chargeur : en dessous
            {16, 62},  // poignée pistolet : en bas à gauche
            {16, 38},  // crosse : à gauche
            {72, 14},  // rail latéral (laser) : en haut à droite
    };

    private static final int TABLE_SLOTS = 1 + AttachmentSlot.values().length;
    private final SimpleContainer container = new SimpleContainer(TABLE_SLOTS);
    private final ContainerLevelAccess access;
    private final Player player;
    private boolean loading;

    /** Constructeur côté client. */
    public ModificationTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ModificationTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.MODIFICATION_TABLE.get(), containerId);
        this.access = access;
        this.player = inventory.player;

        addSlot(new GunSlot(container, GUN_X, GUN_Y));
        AttachmentSlot[] types = AttachmentSlot.values();
        for (int i = 0; i < types.length; i++) {
            addSlot(new PartSlot(container, i + 1, types[i], PART_POS[i][0], PART_POS[i][1]));
        }
        // Inventaire du joueur
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 162));
        }
    }

    public ItemStack gunStack() {
        return container.getItem(0);
    }

    /** L'arme a changé : on affiche ses accessoires dans les emplacements 1 à 4. */
    private void loadAttachments() {
        loading = true;
        ItemStack gun = gunStack();
        GunAttachments attachments = gun.getItem() instanceof GunItem ? GunItem.getAttachments(gun) : GunAttachments.EMPTY;
        for (AttachmentSlot type : AttachmentSlot.values()) {
            container.setItem(type.ordinal() + 1, attachments.get(type).map(ItemStack::new).orElse(ItemStack.EMPTY));
        }
        loading = false;
    }

    /** Un accessoire a été ajouté/retiré : on l'écrit dans l'arme. */
    private void saveAttachments() {
        if (loading || !(gunStack().getItem() instanceof GunItem gun)) {
            return;
        }
        ItemStack stack = gunStack();
        GunAttachments attachments = GunAttachments.EMPTY;
        for (AttachmentSlot type : AttachmentSlot.values()) {
            ItemStack part = container.getItem(type.ordinal() + 1);
            if (!part.isEmpty()) {
                attachments = attachments.with(type, part.getItem());
            }
        }
        if (!attachments.get(AttachmentSlot.MUZZLE).equals(GunItem.getAttachments(stack).get(AttachmentSlot.MUZZLE))) {
            stack.remove(fr.modeminecraft.registry.ModDataComponents.MUZZLE_WEAR.get()); // nouveau silencieux : neuf
        }
        GunItem.setAttachments(stack, attachments);

        // Chargeur plus petit : les balles en trop sont rendues au joueur
        int capacity = gun.resolve(stack).magazineSize();
        int ammo = GunItem.getAmmo(stack);
        if (ammo > capacity) {
            GunItem.setAmmo(stack, capacity);
            if (!player.level().isClientSide) {
                gun.giveBack(player, ammo - capacity, GunItem.getLoadedType(stack));
            }
        }
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(),
                SoundSource.BLOCKS, 0.8f, 1.2f));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        ItemStack stack = gunStack();
        if (!(stack.getItem() instanceof GunItem)) {
            return false;
        }
        if (id == BUTTON_PREVIOUS_SKIN) {
            GunItem.setSkin(stack, GunItem.getSkin(stack).previous());
        } else if (id == BUTTON_NEXT_SKIN) {
            GunItem.setSkin(stack, GunItem.getSkin(stack).next());
        } else {
            return false;
        }
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int inventoryEnd = slots.size();

        if (index < TABLE_SLOTS) {
            // Table -> inventaire
            if (!moveItemStackTo(stack, TABLE_SLOTS, inventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof GunItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof AttachmentItem) {
            if (!moveItemStackTo(stack, 1, TABLE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TABLE_SLOTS + 27) {
            if (!moveItemStackTo(stack, TABLE_SLOTS + 27, inventoryEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, TABLE_SLOTS, TABLE_SLOTS + 27, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // On rend uniquement l'arme : les accessoires sont déjà dedans
        access.execute((level, pos) -> {
            ItemStack gun = container.removeItemNoUpdate(0);
            if (gun.isEmpty()) {
                return;
            }
            if (!player.isAlive() || player instanceof ServerPlayer sp && sp.hasDisconnected()) {
                player.drop(gun, false);
            } else {
                player.getInventory().placeItemBackInInventory(gun);
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.MODIFICATION_TABLE.get());
    }

    /** Emplacement de l'arme. */
    private class GunSlot extends Slot {
        GunSlot(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof GunItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            loadAttachments();
        }
    }

    /** Emplacement d'accessoire : n'accepte que les accessoires compatibles avec l'arme posée. */
    public class PartSlot extends Slot {
        public final AttachmentSlot type;

        PartSlot(Container container, int index, AttachmentSlot type, int x, int y) {
            super(container, index, x, y);
            this.type = type;
        }

        /** L'arme posée a-t-elle cet emplacement ? (sert aussi à griser la case dans l'écran) */
        public boolean isAvailable() {
            return gunStack().getItem() instanceof GunItem gun && gun.stats().slots().contains(type);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return gunStack().getItem() instanceof GunItem gun
                    && stack.getItem() instanceof AttachmentItem attachment
                    && attachment.slot() == type
                    && gun.accepts(attachment);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            saveAttachments();
        }
    }
}

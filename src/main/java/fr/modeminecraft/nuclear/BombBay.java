package fr.modeminecraft.nuclear;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Soute à bombes d'un avion : sa contenance dépend de l'appareil (16 pour le B-29, 4 pour le F-117…), une bombe
 * nucléaire (2 pour le B-2) occupe toute la soute. Ce qui ne rentre pas est rendu au joueur.
 */
public class BombBay extends SimpleContainer {
    private Player user;
    /** Contenance conventionnelle et nombre de bombes nucléaires admises (selon l'avion). */
    private int capacity = 16, nukes = 1;

    public BombBay() {
        super(18);
    }

    public void configure(int capacity, int nukes) {
        this.capacity = capacity;
        this.nukes = nukes;
    }

    public int capacity() {
        return capacity;
    }

    public void user(Player p) {
        user = p;
    }

    public int count(BombItem.Kind kind, int skipSlot) {
        int n = 0;
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack s = getItem(i);
            if (i != skipSlot && s.getItem() instanceof BombItem b && (kind == null || b.kind == kind)) {
                n += s.getCount();
            }
        }
        return n;
    }

    public boolean hasNuke() {
        return count(BombItem.Kind.NUKE, -1) > 0;
    }

    public int bombs() {
        return count(null, -1);
    }

    /** Nombre de bombes de ce type qu'on peut encore mettre dans la case `slot` (en remplacement de son contenu). */
    private int room(int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof BombItem b)) {
            return 0;
        }
        int nukes = count(BombItem.Kind.NUKE, slot), total = count(null, slot);
        if (b.kind == BombItem.Kind.NUKE) {
            return total - nukes == 0 ? Math.max(0, this.nukes - nukes) : 0;
        }
        if (nukes > 0) {
            return 0;
        }
        int sameKind = count(b.kind, slot);
        return Math.max(0, Math.min(b.kind.bayMax - sameKind, capacity - total));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return room(slot, stack) > 0;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            super.setItem(slot, stack);
            return;
        }
        int room = room(slot, stack);
        if (room >= stack.getCount()) {
            super.setItem(slot, stack);
            return;
        }
        ItemStack back = stack.copy();
        if (room > 0) {
            super.setItem(slot, stack.copyWithCount(room));
            back.shrink(room);
        }
        if (user != null) {
            if (!user.getInventory().add(back)) {
                user.drop(back, false);
            }
            user.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.modeminecraft.b29.bay_full")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
        }
    }

    /** Réarmement au sol : soute pleine de bombes `kind` (sauf si elle contient une bombe nucléaire). */
    public void refill(BombItem.Kind kind) {
        if (hasNuke() || capacity <= 0) {
            return;
        }
        int missing = capacity - bombs();
        var item = switch (kind) {
            case GUIDED -> fr.modeminecraft.registry.ModItems.BOMB_GUIDED.get();
            case NAPALM -> fr.modeminecraft.registry.ModItems.BOMB_NAPALM.get();
            default -> fr.modeminecraft.registry.ModItems.BOMB_HE.get();
        };
        for (int i = 0; i < getContainerSize() && missing > 0; i++) {
            ItemStack st = getItem(i);
            if (st.isEmpty()) {
                int n = Math.min(missing, 4);
                super.setItem(i, new ItemStack(item, n));
                missing -= n;
            } else if (st.is(item) && st.getCount() < 4) {
                int n = Math.min(missing, 4 - st.getCount());
                st.grow(n);
                missing -= n;
            }
        }
        setChanged();
    }

    /** Retire la prochaine bombe à larguer (la nucléaire d'abord). */
    public BombItem.Kind take() {
        for (BombItem.Kind k : new BombItem.Kind[]{BombItem.Kind.NUKE, BombItem.Kind.GUIDED, BombItem.Kind.HE, BombItem.Kind.NAPALM}) {
            for (int i = 0; i < getContainerSize(); i++) {
                ItemStack s = getItem(i);
                if (s.getItem() instanceof BombItem b && b.kind == k) {
                    s.shrink(1);
                    setChanged();
                    return k;
                }
            }
        }
        return null;
    }
}

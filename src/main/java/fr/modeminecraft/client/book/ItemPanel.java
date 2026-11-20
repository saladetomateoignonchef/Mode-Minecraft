package fr.modeminecraft.client.book;

import fr.modeminecraft.network.GiveItemPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Panneau d'objets façon "Not Enough Items" : grille paginée de 100 % des objets du mod + barre de recherche en bas.
 * Clic gauche = recette, clic droit = utilisations ; en mode triche (créatif / opérateur) : clic gauche = une pile,
 * clic droit = un objet. Molette = page suivante / précédente.
 */
public class ItemPanel {
    /** Choix d'un objet : uses = false pour sa recette, true pour ses utilisations. */
    public interface Listener {
        void pick(ItemStack stack, boolean uses);
    }

    private static final int CELL = 18;
    private static String savedQuery = "";
    private static int savedPage;
    private static boolean cheat;

    private final int x, y, w, h, cols, rows;
    private final Listener listener;
    public final EditBox search;
    private List<RecipeIndex.Entry> list = List.of();
    private String shownQuery;

    public ItemPanel(Font font, int x, int y, int w, int h, Listener listener) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.listener = listener;
        this.cols = Math.max(1, (w - 2) / CELL);
        this.rows = Math.max(1, (h - 34) / CELL);
        search = new EditBox(font, x + 1, y + h - 15, w - 2, 14, Component.translatable("gui.modeminecraft.book.search"));
        search.setMaxLength(60);
        search.setHint(Component.translatable("gui.modeminecraft.book.search_hint").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(savedQuery);
        search.setResponder(s -> {
            savedQuery = s;
            savedPage = 0;
        });
    }

    /** Remplit la recherche (ex. "@casque" depuis la page d'accueil du manuel). */
    public void setQuery(String q) {
        search.setValue(q);
    }

    public static boolean canCheat() {
        var p = Minecraft.getInstance().player;
        return p != null && (p.isCreative() || p.hasPermissions(2));
    }

    private int perPage() {
        return cols * rows;
    }

    private int pages() {
        return Math.max(1, (list.size() + perPage() - 1) / perPage());
    }

    private void refresh() {
        if (!search.getValue().equals(shownQuery)) {
            shownQuery = search.getValue();
            list = RecipeIndex.search(shownQuery);
        }
        savedPage = Math.max(0, Math.min(savedPage, pages() - 1));
    }

    public boolean inside(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void render(GuiGraphics g, Font font, int mx, int my) {
        refresh();
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xC0101010);
        g.renderOutline(x - 2, y - 2, w + 4, h + 4, 0xFF5A5A5A);
        // En-tête : pages et mode triche
        String pageText = (savedPage + 1) + "/" + pages();
        g.drawString(font, "◀", x + 2, y + 2, inArrow(mx, my, true) ? 0xFFFFFF55 : 0xFFCCCCCC, false);
        g.drawString(font, "▶", x + w - 8, y + 2, inArrow(mx, my, false) ? 0xFFFFFF55 : 0xFFCCCCCC, false);
        g.drawCenteredString(font, pageText, x + w / 2, y + 2, 0xFFFFFFFF);
        if (canCheat()) {
            Component c = Component.translatable(cheat ? "gui.modeminecraft.book.cheat_on" : "gui.modeminecraft.book.cheat_off");
            g.drawString(font, c, x + 2, y + h - 27, cheat ? 0xFFFF5555 : 0xFF888888, false);
        }
        int start = savedPage * perPage();
        for (int i = 0; i < perPage() && start + i < list.size(); i++) {
            int cx = x + 1 + (i % cols) * CELL, cy = y + 13 + (i / cols) * CELL;
            ItemStack s = list.get(start + i).stack();
            if (mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL) {
                g.fill(cx, cy, cx + CELL, cy + CELL, 0x60FFFFFF);
            }
            g.renderItem(s, cx + 1, cy + 1);
        }
        search.render(g, mx, my, 0);
    }

    private boolean inArrow(double mx, double my, boolean left) {
        return my >= y && my < y + 11 && (left ? mx >= x && mx < x + 14 : mx >= x + w - 14 && mx < x + w);
    }

    private boolean inCheat(double mx, double my) {
        return canCheat() && mx >= x && mx < x + w && my >= y + h - 29 && my < y + h - 17;
    }

    public ItemStack hovered(double mx, double my) {
        int start = savedPage * perPage();
        for (int i = 0; i < perPage() && start + i < list.size(); i++) {
            int cx = x + 1 + (i % cols) * CELL, cy = y + 13 + (i / cols) * CELL;
            if (mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL) {
                return list.get(start + i).stack();
            }
        }
        return ItemStack.EMPTY;
    }

    /** Renvoie vrai si le clic était pour le panneau. */
    public boolean click(double mx, double my, int button) {
        if (!inside(mx, my)) {
            search.setFocused(false);
            return false;
        }
        if (search.isMouseOver(mx, my)) {
            search.setFocused(true);
            if (button == 1) {
                search.setValue("");
            }
            return true;
        }
        search.setFocused(false);
        if (inArrow(mx, my, true)) {
            savedPage = (savedPage - 1 + pages()) % pages();
        } else if (inArrow(mx, my, false)) {
            savedPage = (savedPage + 1) % pages();
        } else if (inCheat(mx, my)) {
            cheat = !cheat;
        } else {
            ItemStack s = hovered(mx, my);
            if (!s.isEmpty()) {
                if (cheat && canCheat()) {
                    PacketDistributor.sendToServer(new GiveItemPayload(s.copyWithCount(1), button == 0 ? s.getMaxStackSize() : 1));
                } else {
                    listener.pick(s, button == 1);
                }
            }
        }
        return true;
    }

    public boolean scroll(double mx, double my, double delta) {
        if (!inside(mx, my)) {
            return false;
        }
        savedPage = Math.floorMod(savedPage + (delta < 0 ? 1 : -1), pages());
        return true;
    }
}

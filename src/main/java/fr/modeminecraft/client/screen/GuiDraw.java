package fr.modeminecraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/** Petites fonctions pour dessiner des panneaux et des cases dans le style de Minecraft. */
final class GuiDraw {
    static final int PANEL = 0xFFC6C6C6;
    static final int TEXT = 0xFF404040;

    /** Panneau gris avec bordure claire/foncée (comme les inventaires vanilla). */
    static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
    }

    /** Zone creuse (case d'objet, liste...). */
    static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF8B8B8B);
        g.fill(x, y, x + w - 1, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + h - 1, 0xFF373737);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFFFFFFFF);
    }

    /** Case d'inventaire 18x18 (x, y = coin de l'objet, comme pour un Slot). */
    static void slot(GuiGraphics g, int x, int y) {
        inset(g, x - 1, y - 1, 18, 18);
    }

    private GuiDraw() {}
}

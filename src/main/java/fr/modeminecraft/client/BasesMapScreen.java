package fr.modeminecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carte tactique grand format : chaque base ou infrastructure clé avec son icône (couleur + lettre), le joueur au
 * centre (flèche orientée), une grille de 1 000 blocs, la légende, et les coordonnées du site survolé.
 */
public class BasesMapScreen extends Screen {
    /** Icônes : couleur, lettre. */
    private static final Map<String, int[]> ICONS = new LinkedHashMap<>();

    static {
        ICONS.put("base", new int[]{0xFF2A5AD0, 'U'});
        ICONS.put("soviet", new int[]{0xFFC02020, 'S'});
        ICONS.put("naval_base", new int[]{0xFF20A0C0, 'N'});
        ICONS.put("carrier", new int[]{0xFF4060A0, 'P'});
        ICONS.put("oil_rig", new int[]{0xFF202020, 'O'});
        ICONS.put("refinery", new int[]{0xFFB06020, 'R'});
        ICONS.put("desert", new int[]{0xFFE0B020, 'D'});
        ICONS.put("metropolis", new int[]{0xFF606878, 'M'});
        ICONS.put("cartel", new int[]{0xFF2A9A6A, 'C'});
    }

    private final CompoundTag data;

    public BasesMapScreen(CompoundTag data) {
        super(Component.translatable("gui.modeminecraft.map.title"));
        this.data = data;
    }

    public static void open(CompoundTag data) {
        Minecraft.getInstance().setScreen(new BasesMapScreen(data));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        var p = Minecraft.getInstance().player;
        if (p == null) {
            return;
        }
        int size = Math.min(width - 150, height - 40);
        int x0 = 12, y0 = 24, cx = x0 + size / 2, cy = y0 + size / 2;
        // étendue : le site le plus lointain (au moins 1 500 blocs)
        double range = 1500;
        for (Tag t : data.getList("Sites", Tag.TAG_COMPOUND)) {
            CompoundTag s = (CompoundTag) t;
            range = Math.max(range, Math.max(Math.abs(s.getInt("X") - p.getX()), Math.abs(s.getInt("Z") - p.getZ())) * 1.08);
        }
        double scale = size / 2.0 / range;
        g.fill(x0 - 3, y0 - 3, x0 + size + 3, y0 + size + 3, 0xFF6A5A30);
        g.fill(x0, y0, x0 + size, y0 + size, 0xFFD8CCA0);
        // grille de 1 000 blocs
        int step = range > 6000 ? 2000 : 1000;
        for (int k = -20; k <= 20; k++) {
            double wx = Math.floor(p.getX() / step) * step + k * step;
            int sx = (int) (cx + (wx - p.getX()) * scale);
            if (sx > x0 && sx < x0 + size) {
                g.fill(sx, y0, sx + 1, y0 + size, 0x30604020);
            }
            double wz = Math.floor(p.getZ() / step) * step + k * step;
            int sy = (int) (cy + (wz - p.getZ()) * scale);
            if (sy > y0 && sy < y0 + size) {
                g.fill(x0, sy, x0 + size, sy + 1, 0x30604020);
            }
        }
        g.drawCenteredString(font, title, width / 2, 8, 0xFFE8D890);
        g.drawString(font, "N", cx - 2, y0 + 2, 0xFF403020, false);
        CompoundTag hover = null;
        for (Tag t : data.getList("Sites", Tag.TAG_COMPOUND)) {
            CompoundTag s = (CompoundTag) t;
            int[] icon = ICONS.getOrDefault(s.getString("K"), new int[]{0xFF808080, '?'});
            int sx = (int) (cx + (s.getInt("X") - p.getX()) * scale), sy = (int) (cy + (s.getInt("Z") - p.getZ()) * scale);
            g.fill(sx - 5, sy - 5, sx + 6, sy + 6, 0xFF101010);
            g.fill(sx - 4, sy - 4, sx + 5, sy + 5, icon[0]);
            g.drawString(font, String.valueOf((char) icon[1]), sx - 2, sy - 3, 0xFFFFFFFF, false);
            if (Math.abs(mx - sx) <= 5 && Math.abs(my - sy) <= 5) {
                hover = s;
            }
        }
        // joueur : flèche orientée
        double a = Math.toRadians(p.getYRot());
        int fx = (int) (cx - Math.sin(a) * 7), fz = (int) (cy + Math.cos(a) * 7);
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFFFFFF);
        g.fill(fx - 1, fz - 1, fx + 2, fz + 2, 0xFFFF3030);
        // légende
        int lx = x0 + size + 14, ly = y0;
        for (var e : ICONS.entrySet()) {
            g.fill(lx, ly, lx + 9, ly + 9, e.getValue()[0]);
            g.drawString(font, String.valueOf((char) e.getValue()[1]), lx + 2, ly + 1, 0xFFFFFFFF, false);
            g.drawString(font, Component.translatable("gui.modeminecraft.map." + e.getKey()), lx + 13, ly + 1, 0xFFE0E0E0);
            ly += 13;
        }
        ly += 6;
        g.drawString(font, Component.translatable("gui.modeminecraft.map.scale", step), lx, ly, 0xFFA0A0A0);
        if (hover != null) {
            int d = (int) Math.hypot(hover.getInt("X") - p.getX(), hover.getInt("Z") - p.getZ());
            Component tip = Component.translatable("gui.modeminecraft.map." + hover.getString("K")).append(" — X " + hover.getInt("X")
                    + "  Z " + hover.getInt("Z") + "  (" + d + " m)");
            g.renderTooltip(font, tip, mx, my);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

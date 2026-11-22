package fr.modeminecraft.client;

import fr.modeminecraft.scenario.ScenarioPayloads;
import fr.modeminecraft.scenario.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Écran de sélection de la faction (premier spawn en survie) : deux grandes fenêtres côte à côte, États-Unis à gauche,
 * Union soviétique à droite (drapeau, présentation, paquetage), bouton « S'engager ». Ne se ferme pas sans choix.
 */
public class FactionSelectionScreen extends Screen {
    private boolean sent;

    public FactionSelectionScreen() {
        super(Component.translatable("gui.modeminecraft.faction.title"));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof FactionSelectionScreen)) {
            mc.setScreen(new FactionSelectionScreen());
        }
    }

    private int panelW() {
        return Math.min(220, (width - 36) / 2);
    }

    @Override
    protected void init() {
        int w = panelW(), gap = 12, top = 34, h = height - top - 12;
        int lx = width / 2 - gap / 2 - w, rx = width / 2 + gap / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.faction.join_us"), b -> choose(Side.US))
                .bounds(lx + 10, top + h - 28, w - 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.faction.join_soviet"), b -> choose(Side.SOVIET))
                .bounds(rx + 10, top + h - 28, w - 20, 20).build());
    }

    private void choose(Side s) {
        if (!sent) {
            sent = true;
            PacketDistributor.sendToServer(new ScenarioPayloads.Choose(s.id));
            onClose();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0xF0101418);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFE8D890);
        g.drawCenteredString(font, Component.translatable("gui.modeminecraft.faction.subtitle"), width / 2, 21, 0xFFA0A0A0);
        int w = panelW(), gap = 12, top = 34, h = height - top - 12;
        int lx = width / 2 - gap / 2 - w, rx = width / 2 + gap / 2;
        panel(g, lx, top, w, h, Side.US, mx, my);
        panel(g, rx, top, w, h, Side.SOVIET, mx, my);
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, Side s, int mx, int my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, hover ? s.color : 0xFF303438);
        g.fill(x, y, x + w, y + h, 0xFF1A1E22);
        int fh = Math.min((w - 20) * 2 / 3, Math.max(30, h / 4)), fw = fh * 3 / 2, fx = x + (w - fw) / 2, fy = y + 6;
        if (s == Side.US) {
            for (int i = 0; i < 13; i++) {                                         // 13 bandes
                int y0 = fy + fh * i / 13, y1 = fy + fh * (i + 1) / 13;
                g.fill(fx, y0, fx + fw, y1, i % 2 == 0 ? 0xFFB22234 : 0xFFFFFFFF);
            }
            int cw = fw * 2 / 5, ch = fh * 7 / 13;
            g.fill(fx, fy, fx + cw, fy + ch, 0xFF3C3B6E);
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 6; c++) {
                    int sx = fx + 3 + c * (cw - 6) / 6 + (r % 2) * 2, sy = fy + 2 + r * (ch - 4) / 5;
                    g.fill(sx, sy, sx + 2, sy + 2, 0xFFFFFFFF);
                }
            }
        } else {
            g.fill(fx, fy, fx + fw, fy + fh, 0xFFCC0000);
            int u = Math.max(1, fw / 40), sx = fx + fw / 8, sy = fy + fh / 8;
            // étoile, faucille et marteau stylisés (pixels jaunes)
            int[][] star = {{2, 0}, {1, 1}, {2, 1}, {3, 1}, {0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2}, {1, 3}, {3, 3}};
            for (int[] p : star) {
                g.fill(sx + p[0] * u, sy + p[1] * u, sx + (p[0] + 1) * u, sy + (p[1] + 1) * u, 0xFFFFD700);
            }
            int hx = sx, hy = sy + 6 * u;
            int[][] hs = {{4, 0}, {5, 1}, {6, 2}, {6, 3}, {5, 4}, {4, 5}, {3, 5}, {2, 4}, {1, 6}, {0, 7}, {2, 2}, {3, 3}, {1, 1}, {0, 0}, {1, 0}, {0, 1}};
            for (int[] p : hs) {
                g.fill(hx + p[0] * u, hy + p[1] * u, hx + (p[0] + 1) * u, hy + (p[1] + 1) * u, 0xFFFFD700);
            }
        }
        g.fill(fx - 1, fy - 1, fx + fw + 1, fy, 0xFF000000);
        g.fill(fx - 1, fy + fh, fx + fw + 1, fy + fh + 1, 0xFF000000);
        int ty = fy + fh + 8;
        g.drawCenteredString(font, Component.translatable("gui.modeminecraft.faction.name_" + s.id).withStyle(net.minecraft.ChatFormatting.BOLD), x + w / 2, ty, 0xFFFFFFFF);
        ty += 12;
        // texte réduit (75 %) pour tenir dans la fenêtre quelle que soit l'échelle de l'interface
        float k = 0.75f;
        g.pose().pushPose();
        g.pose().scale(k, k, 1);
        int wrap = (int) ((w - 12) / k);
        float yy = ty / k;
        for (int i = 1; i <= 6; i++) {
            Component line = Component.translatable("gui.modeminecraft.faction." + s.id + "." + i);
            for (var seq : font.split(line, wrap)) {
                if (yy * k > y + h - 34) {
                    break;
                }
                g.drawString(font, seq, (int) ((x + 6) / k), (int) yy, i <= 2 ? 0xFFD8D8D8 : 0xFFB0C8A0);
                yy += 9;
            }
            yy += 2;
        }
        g.pose().popPose();
    }
}

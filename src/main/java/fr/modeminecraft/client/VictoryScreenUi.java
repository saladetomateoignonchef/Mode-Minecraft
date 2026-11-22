package fr.modeminecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Écran de victoire de la Course à l'arme nucléaire (statistiques), puis la partie continue librement. */
public class VictoryScreenUi extends Screen {
    private final String side;
    private final int rep, days;

    public VictoryScreenUi(String side, int rep, int days) {
        super(Component.translatable("gui.modeminecraft.victory.title"));
        this.side = side;
        this.rep = rep;
        this.days = days;
    }

    public static void open(String side, int rep, int days) {
        Minecraft.getInstance().setScreen(new VictoryScreenUi(side, rep, days));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.victory.continue"), b -> onClose())
                .bounds(width / 2 - 80, height - 40, 160, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0xE0000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.pose().pushPose();
        g.pose().scale(2.5f, 2.5f, 1);
        g.drawCenteredString(font, title, (int) (width / 2 / 2.5f), (int) (height / 5 / 2.5f), 0xFFFFD040);
        g.pose().popPose();
        int y = height / 5 + 40;
        g.drawCenteredString(font, Component.translatable("gui.modeminecraft.victory.line_" + side), width / 2, y, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("gui.modeminecraft.victory.stats", days, rep), width / 2, y + 20, 0xFFB0D0B0);
        g.drawCenteredString(font, Component.translatable("gui.modeminecraft.victory.free"), width / 2, y + 40, 0xFFA0A0A0);
    }
}

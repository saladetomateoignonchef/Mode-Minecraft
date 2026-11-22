package fr.modeminecraft.client;

import fr.modeminecraft.scenario.MissionNet;
import fr.modeminecraft.scenario.Missions;
import fr.modeminecraft.scenario.ScenarioClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tableau des opérations stratégiques (Centre de recrutement) : les 4 missions principales de la Course à l'arme
 * nucléaire et les 5 missions stratégiques, avec description, récompense en réputation, état (disponible, en cours,
 * accomplie, verrouillée, en repos) et bouton « Accepter ». Jusqu'à 5 missions actives.
 */
public class OperationsBoardScreen extends Screen {
    private Missions.Mission selected = Missions.Mission.OIL;
    private Button accept;

    public OperationsBoardScreen() {
        super(Component.translatable("gui.modeminecraft.ops.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new OperationsBoardScreen());
    }

    private int left() {
        return width / 2 - 200;
    }

    @Override
    protected void init() {
        int x = left(), y = 34;
        int i = 0;
        for (Missions.Mission m : Missions.Mission.values()) {
            int row = i++;
            addRenderableWidget(Button.builder(Component.translatable("mission.modeminecraft." + m.id()), b -> selected = m)
                    .bounds(x, y + row * 21 + (m.kind == Missions.Kind.STRATEGIC ? 10 : 0), 160, 19).build());
        }
        accept = addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.ops.accept"), b -> {
            PacketDistributor.sendToServer(new MissionNet.Action("accept", selected.id()));
        }).bounds(x + 170, height - 34, 110, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.ops.journal"), b -> Minecraft.getInstance().setScreen(new JournalScreen()))
                .bounds(x + 290, height - 34, 110, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        int x = left();
        g.drawCenteredString(font, title, width / 2, 10, 0xFFE8D890);
        g.drawString(font, Component.translatable("gui.modeminecraft.ops.main"), x, 24, 0xFFFFC060);
        g.drawString(font, Component.translatable("gui.modeminecraft.ops.strategic"), x, 34 + 4 * 21 + 1, 0xFF80C0FF);
        var avail = ScenarioClientState.missions.getCompound("Avail");
        String why = avail.getString(selected.id());
        accept.active = why.isEmpty();
        int px = x + 170, py = 34, pw = 230;
        g.fill(px - 4, py - 4, px + pw, height - 42, 0xC0101418);
        g.drawString(font, Component.translatable("mission.modeminecraft." + selected.id()).withStyle(net.minecraft.ChatFormatting.BOLD), px, py, 0xFFFFFFFF);
        py += 12;
        g.drawString(font, Component.translatable("gui.modeminecraft.ops.reward", selected.reputation), px, py, 0xFF80FF80);
        py += 12;
        for (var line : font.split(Component.translatable("mission.modeminecraft." + selected.id() + ".desc"), pw - 8)) {
            if (py > height - 70) {
                break;
            }
            g.drawString(font, line, px, py, 0xFFD0D0D0);
            py += 10;
        }
        py += 4;
        for (var line : font.split(Component.translatable("mission.modeminecraft." + selected.id() + ".loot"), pw - 8)) {
            if (py > height - 58) {
                break;
            }
            g.drawString(font, line, px, py, 0xFFE0C060);
            py += 10;
        }
        if (!why.isEmpty()) {
            g.drawString(font, Component.translatable("gui.modeminecraft.ops.state." + why), px, height - 54, 0xFFFF8060);
        }
        int n = ScenarioClientState.missions.getList("Active", 10).size();
        Component act = Component.translatable("gui.modeminecraft.ops.active", n, Missions.MAX_ACTIVE);
        g.drawString(font, act, x + 400 - font.width(act), 24, 0xFFA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package fr.modeminecraft.client;

import fr.modeminecraft.scenario.MissionNet;
import fr.modeminecraft.scenario.ScenarioClientState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Journal de bord tactique (touche J) : missions et tâches actives (5 au plus), épingler au HUD (3), abandonner. */
public class JournalScreen extends Screen {
    private int count = -1;

    public JournalScreen() {
        super(Component.translatable("gui.modeminecraft.journal.title"));
    }

    @Override
    protected void init() {
        clearWidgets();
        var list = ScenarioClientState.missions.getList("Active", Tag.TAG_COMPOUND);
        count = list.size();
        int x = width / 2 - 180, y = 40;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            String id = "#" + i;
            addRenderableWidget(Button.builder(Component.translatable(e.getBoolean("Pinned") ? "gui.modeminecraft.journal.unpin" : "gui.modeminecraft.journal.pin"),
                    b -> PacketDistributor.sendToServer(new MissionNet.Action("pin", id))).bounds(x + 250, y + i * 34, 52, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.journal.abandon"),
                    b -> PacketDistributor.sendToServer(new MissionNet.Action("abandon", id))).bounds(x + 306, y + i * 34, 56, 18).build());
        }
    }

    @Override
    public void tick() {
        if (ScenarioClientState.missions.getList("Active", Tag.TAG_COMPOUND).size() != count) {
            init();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFE8D890);
        var list = ScenarioClientState.missions.getList("Active", Tag.TAG_COMPOUND);
        int x = width / 2 - 180, y = 40;
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.modeminecraft.journal.empty"), width / 2, y + 10, 0xFFA0A0A0);
        }
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            int ry = y + i * 34;
            g.fill(x - 4, ry - 3, x + 366, ry + 29, 0x90101418);
            g.drawString(font, MissionText.title(e), x, ry, e.getBoolean("Pinned") ? 0xFFFFE080 : 0xFFFFFFFF);
            g.drawString(font, MissionText.objective(e), x, ry + 11, 0xFFC0C0C0);
            String where = MissionText.where(e);
            if (!where.isEmpty()) {
                g.drawString(font, where, x + 170, ry, 0xFF80E0FF);
            }
        }
        var flags = ScenarioClientState.missions.getCompound("Flags");
        int fy = height - 30;
        StringBuilder sb = new StringBuilder();
        for (String f : flags.getAllKeys()) {
            sb.append(Component.translatable("journal.modeminecraft.flag." + f).getString()).append("   ");
        }
        if (!sb.isEmpty()) {
            g.drawCenteredString(font, sb.toString(), width / 2, fy, 0xFF80FF80);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

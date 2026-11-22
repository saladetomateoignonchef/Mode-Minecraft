package fr.modeminecraft.client;

import fr.modeminecraft.scenario.Grade;
import fr.modeminecraft.scenario.ScenarioClientState;
import fr.modeminecraft.scenario.Side;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/** HUD du scénario (côté droit) : grade et réputation, jusqu'à 3 objectifs épinglés avec distance de la cible. */
public final class ScenarioHud {
    private ScenarioHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Side side = ScenarioClientState.side;
        if (side == null || mc.options.hideGui || mc.player == null || mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) {
            return;
        }
        float k = MilClient.panelScale();
        g.pose().pushPose();
        g.pose().scale(k, k, 1);
        int w = (int) (g.guiWidth() / k), x = w - 6, y = 6;
        Grade grade = Grade.of(ScenarioClientState.reputation);
        Grade next = grade.next();
        Component head = Component.translatable(grade.key(side)).append(next == null ? Component.literal("  " + ScenarioClientState.reputation + " rép.")
                : Component.literal("  " + ScenarioClientState.reputation + " / " + next.reputation + " rép."));
        int hw = mc.font.width(head);
        g.fill(x - hw - 4, y - 2, x + 2, y + 10, 0x90000000);
        g.drawString(mc.font, head, x - hw, y, side == Side.US ? 0xFF8EB4FF : 0xFFFF8E8E);
        y += 14;
        var list = ScenarioClientState.missions.getList("Active", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (!e.getBoolean("Pinned")) {
                continue;
            }
            Component t = MissionText.title(e), o = MissionText.objective(e);
            String where = MissionText.where(e);
            int bw = Math.max(mc.font.width(t), mc.font.width(o) + (where.isEmpty() ? 0 : mc.font.width(where) + 8));
            g.fill(x - bw - 4, y - 2, x + 2, y + 20, 0x90000000);
            g.drawString(mc.font, t, x - mc.font.width(t), y, 0xFFFFE080);
            g.drawString(mc.font, o, x - bw, y + 10, 0xFFD0D0D0);
            if (!where.isEmpty()) {
                g.drawString(mc.font, where, x - mc.font.width(where), y + 10, 0xFF80E0FF);
            }
            y += 26;
        }
        g.pose().popPose();
    }
}

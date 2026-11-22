package fr.modeminecraft.client;

import fr.modeminecraft.scenario.Armory;
import fr.modeminecraft.scenario.ArmoryOffers;
import fr.modeminecraft.scenario.Grade;
import fr.modeminecraft.scenario.ScenarioClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Armurerie fortifiée : boutique (ou réquisition gratuite pour un Général), contrats d'élimination (chef de faction,
 * Faucons Blancs) et rappel du prix de la carte des bases.
 */
public class ArmoryScreen extends Screen {
    private CompoundTag data;
    private int scroll;
    private List<Integer> items = List.of();

    public ArmoryScreen(CompoundTag data) {
        super(Component.translatable("gui.modeminecraft.armory.title"));
        this.data = data;
    }

    public static void open(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ArmoryScreen s) {
            s.data = data;
            s.rebuildWidgets();
        } else {
            mc.setScreen(new ArmoryScreen(data));
        }
    }

    private boolean general() {
        return data.getInt("Grade") == Grade.GENERAL.ordinal();
    }

    private int left() {
        return width / 2 - 200;
    }

    private static void send(String a) {
        PacketDistributor.sendToServer(new Armory.Act(a));
    }

    @Override
    protected void init() {
        int x = left(), y = 40;
        items = ScenarioClientState.side == null ? List.of() : ArmoryOffers.forSide(ScenarioClientState.side);
        if (!general()) {
            addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.armory.shop"), b -> send("shop"))
                    .bounds(x, y, 170, 20).build());
        }
        CompoundTag c = data.getCompound("Contract");
        boolean has = data.contains("Contract");
        boolean wait = data.getInt("ContractWait") > 0;
        Button a = addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.armory.contract_boss"), b -> send("contract_boss"))
                .bounds(x, y + 50, 170, 20).build());
        Button f = addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.armory.contract_falcon"), b -> send("contract_falcon"))
                .bounds(x, y + 74, 170, 20).build());
        a.active = f.active = !has && !wait;
        Button claim = addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.armory.claim"), b -> send("claim"))
                .bounds(x, y + 98, 170, 20).build());
        claim.active = has && c.getInt("Progress") >= c.getInt("Goal");
        if (general()) {
            int rows = Math.max(1, (height - 96) / 20);
            for (int k = 0; k < rows && k + scroll < items.size(); k++) {
                int idx = items.get(k + scroll);
                var e = ArmoryOffers.catalogue().get(idx);
                var stack = e.stack();
                Button r = addRenderableWidget(Button.builder(Component.literal((e.count() > 1 ? e.count() + " x " : "")).append(stack.getHoverName()),
                        b -> send("req:" + idx)).bounds(x + 200, 46 + k * 20, 200, 19).build());
                r.active = data.getInt("ReqWait") <= 0;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (general()) {
            int rows = Math.max(1, (height - 96) / 20);
            scroll = Math.max(0, Math.min(Math.max(0, items.size() - rows), scroll - (int) Math.signum(sy)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        int x = left();
        g.drawCenteredString(font, title, width / 2, 10, 0xFFE8D890);
        Grade gr = Grade.values()[Math.min(Grade.values().length - 1, data.getInt("Grade"))];
        var side = ScenarioClientState.side;
        Component gradeName = side == null ? Component.empty() : Component.translatable(gr.key(side));
        g.drawCenteredString(font, Component.translatable("gui.modeminecraft.armory.grade", gradeName, Math.round(gr.discount * 100)),
                width / 2, 22, 0xFFA0C0FF);
        // contrat en cours
        int y = 40 + 124;
        if (data.contains("Contract")) {
            CompoundTag c = data.getCompound("Contract");
            g.drawString(font, Component.translatable("journal.modeminecraft.contract." + c.getString("Kind"),
                    Component.translatable("entity.modeminecraft.bot." + c.getString("Faction"))), x, y, 0xFFFFD060);
            g.drawString(font, Component.translatable("journal.modeminecraft.contract.progress", c.getInt("Progress"), c.getInt("Goal")), x, y + 11, 0xFFD0D0D0);
        } else if (data.getInt("ContractWait") > 0) {
            g.drawString(font, Component.translatable("gui.modeminecraft.armory.contract_wait", data.getInt("ContractWait") / 1200 + 1), x, y, 0xFFB0B0B0);
        }
        int ty = 40 + 26;
        for (var line : font.split(Component.translatable("gui.modeminecraft.armory.contracts_info"), 175)) {
            g.drawString(font, line, x, ty, 0xFF909090);
            ty += 9;
            if (ty > 40 + 48) {
                break;
            }
        }
        int by = height - 30;
        for (var line : font.split(Component.translatable("gui.modeminecraft.armory.map_info"), 380)) {
            g.drawString(font, line, x, by, 0xFFE0C060);
            by += 10;
        }
        if (general()) {
            int wait = data.getInt("ReqWait");
            g.drawString(font, wait > 0 ? Component.translatable("gui.modeminecraft.armory.req_wait", wait / 20 + 1)
                    : Component.translatable("gui.modeminecraft.armory.req_ready").withStyle(ChatFormatting.GREEN), x + 200, 35, 0xFFFFFFFF);
            int rows = Math.max(1, (height - 96) / 20);
            for (int k = 0; k < rows && k + scroll < items.size(); k++) {
                g.renderItem(ArmoryOffers.catalogue().get(items.get(k + scroll)).stack(), x + 180, 46 + k * 20 + 1);
            }
        } else {
            g.drawString(font, Component.translatable("gui.modeminecraft.armory.shop_info"), x + 180, 46, 0xFF909090);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

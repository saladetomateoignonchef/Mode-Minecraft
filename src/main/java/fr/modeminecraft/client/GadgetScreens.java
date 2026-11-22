package fr.modeminecraft.client;

import fr.modeminecraft.scenario.Gadgets;
import fr.modeminecraft.scenario.Prisoners;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Écrans des gadgets : talkie-walkie de commandement, mallette de frappe stratégique, interrogatoire d'un détenu. */
public final class GadgetScreens {
    private GadgetScreens() {}

    public static void open(String kind) {
        Minecraft.getInstance().setScreen(kind.equals("radio") ? new Radio() : new Briefcase());
    }

    public static void interrogation(int entity, int left, String name) {
        Minecraft.getInstance().setScreen(new Interrogation(entity, left, name));
    }

    private abstract static class Panel extends Screen {
        Panel(String key) {
            super(Component.translatable(key));
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float partial) {
            super.render(g, mx, my, partial);
            g.fill(width / 2 - 130, height / 2 - 70, width / 2 + 130, height / 2 + 70, 0xC0101418);
            g.drawCenteredString(font, title, width / 2, height / 2 - 62, 0xFFE8D890);
            int y = height / 2 - 48;
            for (var line : font.split(Component.translatable(getTitle().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    ? t.getKey() + ".info" : ""), 240)) {
                g.drawString(font, line, width / 2 - 120, y, 0xFFC0C0C0);
                y += 10;
            }
            extra(g);
        }

        void extra(GuiGraphics g) {
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    /** Talkie-walkie : renforts d'élite ou largage C-130. */
    static class Radio extends Panel {
        Radio() {
            super("gui.modeminecraft.radio.title");
        }

        @Override
        protected void init() {
            addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.radio.elite"), b -> {
                PacketDistributor.sendToServer(new Gadgets.Use("radio", "elite"));
                onClose();
            }).bounds(width / 2 - 120, height / 2 + 10, 115, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.radio.drop"), b -> {
                PacketDistributor.sendToServer(new Gadgets.Use("radio", "drop"));
                onClose();
            }).bounds(width / 2 + 5, height / 2 + 10, 115, 20).build());
        }
    }

    /** Mallette : coordonnées X / Z de la frappe. */
    static class Briefcase extends Panel {
        private EditBox x, z;

        Briefcase() {
            super("gui.modeminecraft.briefcase.title");
        }

        @Override
        protected void init() {
            var p = Minecraft.getInstance().player;
            x = addRenderableWidget(new EditBox(font, width / 2 - 120, height / 2 + 5, 110, 18, Component.literal("X")));
            z = addRenderableWidget(new EditBox(font, width / 2 + 10, height / 2 + 5, 110, 18, Component.literal("Z")));
            x.setValue(p == null ? "0" : String.valueOf(p.getBlockX()));
            z.setValue(p == null ? "0" : String.valueOf(p.getBlockZ()));
            addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.briefcase.launch"), b -> {
                PacketDistributor.sendToServer(new Gadgets.Use("strike", x.getValue() + "," + z.getValue()));
                onClose();
            }).bounds(width / 2 - 60, height / 2 + 32, 120, 20).build());
        }

        @Override
        void extra(GuiGraphics g) {
            g.drawString(font, "X", width / 2 - 128, height / 2 + 10, 0xFFFFFFFF);
            g.drawString(font, "Z", width / 2 + 2, height / 2 + 10, 0xFFFFFFFF);
        }
    }

    /** Interrogatoire : persuader, intimider, marchander. */
    static class Interrogation extends Panel {
        private final int entity, left;
        private final String name;

        Interrogation(int entity, int left, String name) {
            super("gui.modeminecraft.interrogation.title");
            this.entity = entity;
            this.left = left;
            this.name = name;
        }

        @Override
        protected void init() {
            String[] ap = {"persuade", "intimidate", "bribe"};
            for (int i = 0; i < 3; i++) {
                String a = ap[i];
                addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.interrogation." + a), b -> {
                    PacketDistributor.sendToServer(new Prisoners.Act(entity, a));
                    onClose();
                }).bounds(width / 2 - 120 + i * 82, height / 2 + 20, 78, 20).build());
            }
        }

        @Override
        void extra(GuiGraphics g) {
            g.drawCenteredString(font, Component.translatable("gui.modeminecraft.interrogation.subject", name, left), width / 2, height / 2 + 4, 0xFFFFD060);
        }
    }
}

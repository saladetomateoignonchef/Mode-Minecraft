package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.RocketLauncherEntity;
import fr.modeminecraft.network.LauncherPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Écran de pointage d'un lance-roquettes statique (Maj + clic droit) : azimut (cap, 0 = nord), élévation 0-65°,
 * portée estimée, tir d'une roquette ou de toute la salve. Monté dessus : Espace = salve, et un petit affichage.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public class RocketLauncherScreen extends Screen {
    private final RocketLauncherEntity launcher;
    private float bearing, elevation;

    public RocketLauncherScreen(RocketLauncherEntity launcher) {
        super(Component.translatable("gui.modeminecraft.launcher"));
        this.launcher = launcher;
        this.bearing = Mth.positiveModulo(launcher.azimuth() + 180, 360);
        this.elevation = launcher.elevation();
    }

    public static void open(RocketLauncherEntity l) {
        Minecraft.getInstance().setScreen(new RocketLauncherScreen(l));
    }

    private float yaw() {
        return bearing - 180;                                              // cap (0 = nord) -> lacet de Minecraft (0 = sud)
    }

    private void sendAim() {
        PacketDistributor.sendToServer(new LauncherPayload(launcher.getId(), LauncherPayload.AIM, yaw(), elevation));
    }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 - 50;
        addRenderableWidget(new AbstractSliderButton(cx - 100, y, 200, 20, Component.empty(), bearing / 359.0) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("gui.modeminecraft.launcher.bearing", Math.round(value * 359)));
            }

            @Override
            protected void applyValue() {
                bearing = (float) Math.round(value * 359);
                sendAim();
            }
        });
        addRenderableWidget(new AbstractSliderButton(cx - 100, y + 26, 200, 20, Component.empty(), elevation / RocketLauncherEntity.MAX_ELEVATION) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("gui.modeminecraft.launcher.elevation", Math.round(value * RocketLauncherEntity.MAX_ELEVATION)));
            }

            @Override
            protected void applyValue() {
                elevation = (float) Math.round(value * RocketLauncherEntity.MAX_ELEVATION);
                sendAim();
            }
        });
        addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.launcher.fire_one"), b -> {
            sendAim();
            PacketDistributor.sendToServer(new LauncherPayload(launcher.getId(), LauncherPayload.FIRE_ONE, 0, 0));
        }).bounds(cx - 100, y + 80, 97, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.launcher.salvo"), b -> {
            sendAim();
            PacketDistributor.sendToServer(new LauncherPayload(launcher.getId(), LauncherPayload.SALVO, 0, 0));
        }).bounds(cx + 3, y + 80, 97, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int cx = width / 2, y = height / 2 - 50;
        g.drawCenteredString(font, Component.translatable("entity.modeminecraft.rocket_launcher." + launcher.kind().id), cx, y - 30, 0xFFE8D890);
        g.drawCenteredString(font, Component.translatable("gui.modeminecraft.launcher.loaded", launcher.loaded(), launcher.kind().tubes), cx, y - 16,
                launcher.loaded() > 0 ? 0xFF60E060 : 0xFFE06060);
        double r = RocketLauncherEntity.range(launcher.kind(), elevation);
        g.drawCenteredString(font, Component.translatable("gui.modeminecraft.launcher.range", (int) Math.round(r)), cx, y + 56,
                r < 50 || r > 400 ? 0xFFFFB040 : 0xFFD0D0D0);
        g.drawCenteredString(font, Component.translatable("gui.modeminecraft.launcher.redstone"), cx, y + 108, 0xFF909090);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (launcher.isRemoved() || Minecraft.getInstance().player == null || launcher.distanceToSqr(Minecraft.getInstance().player) > 8 * 8) {
            onClose();
        }
    }

    // ------------------------------------------------------------------ servant monté
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof RocketLauncherEntity l && mc.screen == null) {
            while (mc.options.keyJump.consumeClick()) {
                PacketDistributor.sendToServer(new LauncherPayload(l.getId(), LauncherPayload.SALVO, 0, 0));
            }
        }
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof RocketLauncherEntity l) || mc.options.hideGui) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        var font = mc.font;
        int x = 6, y = 6;
        g.fill(x - 3, y - 3, x + 200, y + 46, 0x90000000);
        g.drawString(font, Component.translatable("entity.modeminecraft.rocket_launcher." + l.kind().id), x, y, 0xFFE8D890);
        g.drawString(font, Component.translatable("gui.modeminecraft.launcher.loaded", l.loaded(), l.kind().tubes), x, y + 11,
                l.loaded() > 0 ? 0xFF60E060 : 0xFFE06060);
        g.drawString(font, Component.translatable("gui.modeminecraft.launcher.status", Math.round(Mth.positiveModulo(l.azimuth() + 180, 360)),
                Math.round(l.elevation()), (int) Math.round(RocketLauncherEntity.range(l.kind(), l.elevation()))), x, y + 22, 0xFFD0D0D0);
        g.drawString(font, Component.translatable("gui.modeminecraft.launcher.keys"), x, y + 33, 0xFF909090);
    }
}

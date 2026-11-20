package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.client.screen.GuiDraw;
import fr.modeminecraft.item.tactical.TacticalSlot;
import fr.modeminecraft.network.OpenBackpackPayload;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Cases « gilet » et « sac à dos » de l'inventaire : cadre, silhouette quand elles sont vides, touche K. */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class TacticalSlotsClient {
    private static ItemStack vestIcon, bagIcon;

    private static boolean tactical(Slot s) {
        return s.container instanceof TacticalSlot.Wrapper;
    }

    private static int kind(Slot s) {
        return s.getContainerSlot();
    }

    @SubscribeEvent
    public static void onBackground(ContainerScreenEvent.Render.Background event) {
        var screen = event.getContainerScreen();
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) {
            return;
        }
        if (vestIcon == null) {
            vestIcon = new ItemStack(ModItems.ITEMS.getEntries().stream().<net.minecraft.world.item.Item>map(h -> h.get())
                    .filter(i -> i instanceof fr.modeminecraft.item.VestItem).findFirst().orElse(net.minecraft.world.item.Items.LEATHER_CHESTPLATE));
            bagIcon = new ItemStack(ModItems.TACTICAL_BACKPACK.get());
        }
        var g = event.getGuiGraphics();
        for (Slot s : screen.getMenu().slots) {
            if (!tactical(s) || s.x < 0 || s.y < 0) {
                continue;
            }
            int x = screen.getGuiLeft() + s.x, y = screen.getGuiTop() + s.y;
            GuiDraw.slot(g, x, y);
            if (!s.hasItem()) {
                g.renderItem(kind(s) == 0 ? vestIcon : bagIcon, x, y);
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(x, y, x + 16, y + 16, 0xB08B8B8B);           // silhouette grisée
                g.pose().popPose();
            }
        }
    }

    /** Infobulle des cases vides. */
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)
                || !(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) {
            return;
        }
        Slot s = screen.getSlotUnderMouse();
        if (s != null && tactical(s) && !s.hasItem() && Minecraft.getInstance().player.containerMenu.getCarried().isEmpty()) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
                    Component.translatable(kind(s) == 0 ? "gui.modeminecraft.slot.vest" : "gui.modeminecraft.slot.backpack"),
                    event.getMouseX(), event.getMouseY());
        }
    }

    /** Touche K : ouvrir le sac à dos porté. */
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        while (ModKeyMappings.BACKPACK.consumeClick()) {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null) {
                PacketDistributor.sendToServer(new OpenBackpackPayload());
            }
        }
    }

    private TacticalSlotsClient() {}
}

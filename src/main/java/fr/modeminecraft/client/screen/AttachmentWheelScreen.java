package fr.modeminecraft.client.screen;

import fr.modeminecraft.client.ModKeyMappings;
import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.AttachmentSlot;
import fr.modeminecraft.item.gun.GunAttachments;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.network.QuickAttachPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu rapide des accessoires (roue) : on maintient la touche Y, on pointe un accessoire avec la souris,
 * et on relâche la touche (ou on clique) pour le monter. Les accessoires déjà montés apparaissent en rouge :
 * les choisir les retire. Le jeu n'est pas mis en pause.
 */
public class AttachmentWheelScreen extends Screen {
    private record Option(ItemStack icon, int inventorySlot, int removeSlot, AttachmentSlot type) {}

    private final List<Option> options = new ArrayList<>();
    private int hovered = -1;

    private final boolean closeOnRelease;

    public AttachmentWheelScreen() {
        this(true);
    }

    /** closeOnRelease = false : la roue reste ouverte jusqu'au clic (ouverture sans la touche). */
    public AttachmentWheelScreen(boolean closeOnRelease) {
        super(Component.translatable("screen.modeminecraft.attachment_wheel"));
        this.closeOnRelease = closeOnRelease;
    }

    @Override
    protected void init() {
        options.clear();
        var player = minecraft.player;
        ItemStack gunStack = player.getMainHandItem();
        if (!(gunStack.getItem() instanceof GunItem gun)) {
            return;
        }
        GunAttachments mounted = GunItem.getAttachments(gunStack);
        for (AttachmentSlot slot : AttachmentSlot.values()) {
            mounted.get(slot).ifPresent(item -> options.add(new Option(new ItemStack(item), -1, slot.ordinal(), slot)));
        }
        var inv = player.getInventory();
        List<net.minecraft.world.item.Item> seen = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof AttachmentItem a && gun.accepts(a) && !seen.contains(a)
                    && mounted.get(a.slot()).map(m -> m != a).orElse(true)) {
                seen.add(a);
                options.add(new Option(s.copyWithCount(1), i, -1, a.slot()));
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // pas de flou : on garde la vue du jeu
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2, cy = height / 2;
        int n = options.size();
        if (n == 0) {
            g.drawCenteredString(font, Component.translatable("screen.modeminecraft.attachment_wheel.empty"), cx, cy - 4, 0xFFFF7070);
            return;
        }
        double dx = mouseX - cx, dy = mouseY - cy;
        hovered = -1;
        if (dx * dx + dy * dy > 18 * 18) {
            double angle = Mth.positiveModulo(Math.atan2(dy, dx) + Math.PI / 2 + Math.PI / n, Math.PI * 2);
            hovered = (int) (angle / (Math.PI * 2) * n) % n;
        }
        int radius = Math.max(52, Math.min(110, 14 * n));
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xAAFFFFFF);
        for (int i = 0; i < n; i++) {
            Option o = options.get(i);
            double a = i * Math.PI * 2 / n - Math.PI / 2;
            int x = cx + (int) (Math.cos(a) * radius), y = cy + (int) (Math.sin(a) * radius);
            boolean h = i == hovered;
            int bg = o.removeSlot >= 0 ? (h ? 0xD0A02020 : 0x90601010) : (h ? 0xD0308040 : 0x90202020);
            int size = h ? 14 : 11;
            g.fill(x - size, y - size, x + size, y + size, bg);
            g.renderOutline(x - size, y - size, size * 2, size * 2, h ? 0xFFFFFFFF : 0x80FFFFFF);
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            float scale = h ? 1.5f : 1.1f;
            g.pose().scale(scale, scale, 1);
            g.renderItem(o.icon, -8, -8);
            g.pose().popPose();
        }
        if (hovered >= 0) {
            Option o = options.get(hovered);
            Component name = o.icon.getHoverName();
            Component line = o.removeSlot >= 0
                    ? Component.translatable("screen.modeminecraft.attachment_wheel.remove", name).withStyle(ChatFormatting.RED)
                    : Component.translatable("screen.modeminecraft.attachment_wheel.mount", name).withStyle(ChatFormatting.GREEN);
            g.drawCenteredString(font, line, cx, cy + 14, 0xFFFFFFFF);
            g.drawCenteredString(font, Component.translatable(o.type.translationKey()), cx, cy - 22, 0xFFBBBBBB);
        } else {
            g.drawCenteredString(font, title, cx, cy + 14, 0xFFDDDDDD);
        }
    }

    private void select() {
        if (hovered >= 0 && hovered < options.size()) {
            Option o = options.get(hovered);
            PacketDistributor.sendToServer(new QuickAttachPayload(o.inventorySlot, o.removeSlot));
        }
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        select();
        return true;
    }

    @Override
    public void tick() {
        // Touche relâchée : on choisit l'accessoire pointé
        var key = ModKeyMappings.QUICK_ATTACH.getKey();
        if (closeOnRelease && key.getType() == InputConstants.Type.KEYSYM && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue())) {
            select();
        }
        if (!(minecraft.player.getMainHandItem().getItem() instanceof GunItem)) {
            onClose();
        }
    }
}

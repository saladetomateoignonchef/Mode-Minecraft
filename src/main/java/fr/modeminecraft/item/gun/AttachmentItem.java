package fr.modeminecraft.item.gun;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Accessoire d'arme (viseur, silencieux, poignée, chargeur...).
 * Ses effets sont décrits par {@link Stats} : des multiplicateurs appliqués aux statistiques de l'arme.
 */
public class AttachmentItem extends Item {
    private final Stats stats;

    public AttachmentItem(Stats stats, Properties properties) {
        super(properties.stacksTo(16));
        this.stats = stats;
    }

    public Stats stats() {
        return stats;
    }

    public AttachmentSlot slot() {
        return stats.slot;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.attachment.slot",
                Component.translatable(stats.slot.translationKey())).withStyle(ChatFormatting.GOLD));
        if (stats.magazineSize > 0) {
            line(tooltip, "magazine", stats.magazineSize, true);
        }
        if (stats.zoom < 1.0f) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.attachment.zoom",
                    String.format("%.1f", 1.0f / stats.zoom)).withStyle(ChatFormatting.BLUE));
        }
        percent(tooltip, "damage", stats.damageMul, true);
        percent(tooltip, "spread", stats.spreadMul, false);
        percent(tooltip, "aim_spread", stats.aimSpreadMul, false);
        percent(tooltip, "recoil", stats.recoilMul, false);
        percent(tooltip, "reload", stats.reloadMul, false);
        if (stats.silenced) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.attachment.silenced").withStyle(ChatFormatting.BLUE));
        }
    }

    /** Affiche "+20 %" en vert ou "-10 %" en rouge selon que c'est un bonus ou un malus. */
    private static void percent(List<Component> tooltip, String key, float mul, boolean higherIsBetter) {
        if (mul == 1.0f) {
            return;
        }
        int pct = Math.round((mul - 1.0f) * 100);
        boolean good = higherIsBetter == (pct > 0);
        tooltip.add(Component.translatable("tooltip.modeminecraft.attachment." + key, (pct > 0 ? "+" : "") + pct + " %")
                .withStyle(good ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private static void line(List<Component> tooltip, String key, int value, boolean good) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.attachment." + key, value)
                .withStyle(good ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    /** Effets d'un accessoire. Un multiplicateur de 1 = aucun effet. */
    public static final class Stats {
        final AttachmentSlot slot;
        float damageMul = 1, spreadMul = 1, aimSpreadMul = 1, recoilMul = 1, reloadMul = 1, volumeMul = 1;
        int magazineSize = 0;           // 0 = ne change pas la capacité
        float zoom = 1.0f;              // < 1 = zoom quand on vise (0.5 = x2)
        SightOverlay overlay = SightOverlay.NONE;
        boolean silenced = false;

        private Stats(AttachmentSlot slot) {
            this.slot = slot;
        }

        public static Stats of(AttachmentSlot slot) {
            return new Stats(slot);
        }

        public Stats damage(float mul) { damageMul = mul; return this; }
        public Stats spread(float mul) { spreadMul = mul; return this; }
        public Stats aimSpread(float mul) { aimSpreadMul = mul; return this; }
        public Stats recoil(float mul) { recoilMul = mul; return this; }
        public Stats reload(float mul) { reloadMul = mul; return this; }
        public Stats magazine(int size) { magazineSize = size; return this; }
        public Stats sight(float zoom, SightOverlay overlay) { this.zoom = zoom; this.overlay = overlay; return this; }
        public Stats silencer() { silenced = true; volumeMul = 0.2f; return this; }

        public AttachmentSlot slot() { return slot; }
        public int magazineSize() { return magazineSize; }
    }
}

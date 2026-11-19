package fr.modeminecraft.item.gun;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Accessoire d'arme (viseur, silencieux, poignée, crosse, chargeur...).
 * Ses effets sont décrits par {@link Stats} : des multiplicateurs appliqués aux statistiques de l'arme,
 * et éventuellement des effets spéciaux ({@link Special}).
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

    /** Effets spéciaux (au-delà des simples statistiques). */
    public enum Special {
        NIGHT_VISION,        // vision nocturne quand on vise
        LASER,               // point laser visible
        THERMAL,             // vision thermique quand on vise
        DIGITAL,             // affiche les points de vie de la cible visée
        RANGEFINDER,         // affiche la distance de la cible visée
        RUBY_MARK,           // l'ennemi visé brille en rouge
        UNDERBARREL_GRENADE, // tir secondaire (touche V) : grenade 40 mm
        UNDERBARREL_SHOTGUN, // tir secondaire (touche V) : coup de calibre 12
        BAYONET              // +6 dégâts au corps à corps
    }

    /** Sur quelles armes l'accessoire se monte. */
    public enum Compat { ANY, PISTOL, LONG }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.attachment.slot",
                Component.translatable(stats.slot.translationKey())).withStyle(ChatFormatting.GOLD));
        if (stats.compat != Compat.ANY) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.attachment.compat." + stats.compat.name().toLowerCase())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (stats.magazineSize > 0) {
            line(tooltip, "magazine", stats.magazineSize);
        }
        if (!stats.modes.isEmpty()) {
            List<String> zooms = new ArrayList<>();
            for (SightMode m : stats.modes) {
                zooms.add(String.format("x%.1f", m.magnification()).replace(".0", ""));
            }
            tooltip.add(Component.translatable(stats.modes.size() > 1 ? "tooltip.modeminecraft.attachment.zoom_levels"
                    : "tooltip.modeminecraft.attachment.zoom", String.join(" / ", zooms)).withStyle(ChatFormatting.BLUE));
        }
        percent(tooltip, "damage", stats.damageMul, true);
        percent(tooltip, "spread", stats.spreadMul, false);
        percent(tooltip, "hip_spread", stats.hipSpreadMul, false);
        percent(tooltip, "aim_spread", stats.aimSpreadMul, false);
        percent(tooltip, "recoil", stats.recoilMul, false);
        percent(tooltip, "reload", stats.reloadMul, false);
        percent(tooltip, "mobility", stats.mobilityMul, true);
        percent(tooltip, "aim_speed", stats.aimSpeedMul, true);
        percent(tooltip, "moving_spread", stats.movingSpreadMul, false);
        percent(tooltip, "steady_recoil", stats.steadyRecoilMul, false);
        percent(tooltip, "steady_spread", stats.steadySpreadMul, false);
        percent(tooltip, "draw", stats.drawMul, false);
        if (stats.silenced) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.attachment.silenced").withStyle(ChatFormatting.BLUE));
        }
        for (Special special : stats.specials) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.attachment.special." + special.name().toLowerCase())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (stats.cosmetic) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.attachment.cosmetic").withStyle(ChatFormatting.LIGHT_PURPLE));
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

    private static void line(List<Component> tooltip, String key, int value) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.attachment." + key, value).withStyle(ChatFormatting.GREEN));
    }

    /** Effets d'un accessoire. Un multiplicateur de 1 = aucun effet. */
    public static final class Stats {
        final AttachmentSlot slot;
        float damageMul = 1, spreadMul = 1, hipSpreadMul = 1, aimSpreadMul = 1, recoilMul = 1, reloadMul = 1, volumeMul = 1;
        float mobilityMul = 1;      // vitesse de déplacement en visant
        float aimSpeedMul = 1;      // vitesse du zoom quand on commence à viser
        float movingSpreadMul = 1;  // perte de précision en marchant
        float steadyRecoilMul = 1;  // recul quand on est stable (allongé, ou accroupi immobile)
        float steadySpreadMul = 1;  // dispersion quand on est stable
        float drawMul = 1;          // délai pour sortir l'arme
        int magazineSize = 0;       // 0 = ne change pas la capacité
        List<SightMode> modes = List.of(); // réglages du viseur (plusieurs = molette pour changer)
        boolean silenced = false, cosmetic = false;
        Compat compat = Compat.ANY;
        final Set<Special> specials = EnumSet.noneOf(Special.class);

        private Stats(AttachmentSlot slot) {
            this.slot = slot;
        }

        public static Stats of(AttachmentSlot slot) {
            return new Stats(slot);
        }

        public Stats damage(float mul) { damageMul = mul; return this; }
        public Stats spread(float mul) { spreadMul = mul; return this; }
        public Stats hipSpread(float mul) { hipSpreadMul = mul; return this; }
        public Stats aimSpread(float mul) { aimSpreadMul = mul; return this; }
        public Stats recoil(float mul) { recoilMul = mul; return this; }
        public Stats reload(float mul) { reloadMul = mul; return this; }
        public Stats mobility(float mul) { mobilityMul = mul; return this; }
        public Stats aimSpeed(float mul) { aimSpeedMul = mul; return this; }
        public Stats movingSpread(float mul) { movingSpreadMul = mul; return this; }
        public Stats steady(float recoil, float spread) { steadyRecoilMul = recoil; steadySpreadMul = spread; return this; }
        public Stats draw(float mul) { drawMul = mul; return this; }
        public Stats magazine(int size) { magazineSize = size; return this; }
        /** Viseur à un seul réglage. */
        public Stats sight(float zoom, SightOverlay overlay) { modes = List.of(new SightMode(zoom, overlay)); return this; }
        /** Viseur à plusieurs réglages (molette pendant la visée). */
        public Stats sights(SightMode... m) { modes = List.of(m); return this; }
        public Stats silencer() { silenced = true; volumeMul = 0.2f; return this; }
        public Stats special(Special... s) { specials.addAll(List.of(s)); return this; }
        public Stats only(Compat c) { compat = c; return this; }
        public Stats cosmetic() { cosmetic = true; return this; }

        public AttachmentSlot slot() { return slot; }
        public int magazineSize() { return magazineSize; }
        public Compat compat() { return compat; }
        public Set<Special> specials() { return specials; }
    }
}

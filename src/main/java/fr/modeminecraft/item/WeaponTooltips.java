package fr.modeminecraft.item;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Ajoute une ligne de description (traduite) sous le nom de l'arme. */
final class WeaponTooltips {
    static void add(List<Component> tooltip, String key) {
        if (key != null) {
            tooltip.add(Component.translatable("tooltip." + ModeMinecraft.MODID + "." + key)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private WeaponTooltips() {}
}

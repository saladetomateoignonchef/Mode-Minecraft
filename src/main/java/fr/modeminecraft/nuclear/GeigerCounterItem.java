package fr.modeminecraft.nuclear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Compteur Geiger-Müller : crépite dès qu'il est dans la barre d'action, affiche le débit de dose en main (voir RadiationClient). */
public class GeigerCounterItem extends Item {
    public GeigerCounterItem(Properties p) {
        super(p);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext c, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.geiger").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.modeminecraft.geiger.use").withStyle(ChatFormatting.GRAY));
    }
}

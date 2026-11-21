package fr.modeminecraft.item;

import fr.modeminecraft.entity.bot.Disguise;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Pièce d'une tenue d'infiltration (déguisement de faction). */
public class DisguiseArmorItem extends ArmorItem {
    public final Disguise.Outfit outfit;

    public DisguiseArmorItem(Disguise.Outfit outfit, Holder<ArmorMaterial> material, Type type, Properties p) {
        super(material, type, p);
        this.outfit = outfit;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext c, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.outfit." + outfit.name().toLowerCase()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.outfit.full_set").withStyle(ChatFormatting.DARK_GRAY));
    }
}

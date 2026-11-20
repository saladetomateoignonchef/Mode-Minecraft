package fr.modeminecraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * Bouclier balistique lourd (niveau IIIA) : se lève comme un bouclier et arrête les balles venant de face ;
 * fenêtre blindée pour voir à travers. Lourd : -10 % de vitesse tant qu'il est dans la main secondaire.
 */
public class BallisticShieldItem extends ShieldItem {
    public BallisticShieldItem(Properties properties) {
        super(properties.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(fr.modeminecraft.ModeMinecraft.id("ballistic_shield_weight"), -0.10,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.OFFHAND)
                .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(fr.modeminecraft.ModeMinecraft.id("ballistic_shield_kb"), 0.3,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.OFFHAND)
                .build()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.ballistic_shield").withStyle(ChatFormatting.GRAY));
    }
}

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
 * Avec une arme de poing dans l'autre main : on tire sur le côté du bouclier levé (voir {@link ShieldCombat}).
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
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                                                                       net.minecraft.world.InteractionHand hand) {
        if (ShieldCombat.active(player) && ShieldCombat.reloading(player)) {
            return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));   // on recharge : bouclier baissé
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.ballistic_shield").withStyle(ChatFormatting.GRAY));
    }
}

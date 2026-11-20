package fr.modeminecraft.item;

import fr.modeminecraft.entity.bot.Contracts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Contrat de chasse : la cible, sa faction, la prime et l'état du contrat. */
public class HuntingContractItem extends Item {
    public HuntingContractItem(Properties p) {
        super(p.stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        Contracts.Contract c = Contracts.of(stack);
        return c != null && c.done();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        Contracts.Contract c = Contracts.of(stack);
        if (c == null) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.contract.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.modeminecraft.contract.target", c.name()).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.modeminecraft.contract.faction", Component.translatable("entity.modeminecraft.bot." + c.faction().id))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.contract.reward", c.reward()).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable(c.done() ? "tooltip.modeminecraft.contract.done" : "tooltip.modeminecraft.contract.hunt")
                .withStyle(c.done() ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
    }
}

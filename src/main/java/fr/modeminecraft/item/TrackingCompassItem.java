package fr.modeminecraft.item;

import fr.modeminecraft.entity.bot.Contracts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Boussole de traque : pointe vers la cible du contrat de chasse en cours (mise à jour chaque seconde). */
public class TrackingCompassItem extends Item {
    public TrackingCompassItem(Properties p) {
        super(p.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof ServerPlayer p && level.getGameTime() % 20 == 0) {
            Contracts.updateCompass(stack, p);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.tracking_compass").withStyle(ChatFormatting.GRAY));
    }
}

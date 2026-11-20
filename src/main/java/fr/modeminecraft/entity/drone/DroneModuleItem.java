package fr.modeminecraft.entity.drone;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Module de drone à installer sur la table de modification : hélices silencieuses, batterie LiPo-Titan, cargo. */
public class DroneModuleItem extends Item {
    public enum Kind { ROTORS, BATTERY, CARGO }

    private final Kind kind;

    public DroneModuleItem(Kind kind, Properties properties) {
        super(properties.stacksTo(16));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.drone_module." + kind.name().toLowerCase()).withStyle(ChatFormatting.GRAY));
    }
}

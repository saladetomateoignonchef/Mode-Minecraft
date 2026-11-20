package fr.modeminecraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Carte d'accès (1 = bleue, 2 = rouge, 3 = noire) : ouvre les salles blindées de son niveau et des niveaux inférieurs. */
public class AccessCardItem extends Item {
    private final int level;

    public AccessCardItem(int level, Properties properties) {
        super(properties);
        this.level = level;
    }

    public int level() {
        return level;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.access_card." + level).withStyle(ChatFormatting.GRAY));
    }
}

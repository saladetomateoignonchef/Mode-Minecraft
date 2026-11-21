package fr.modeminecraft.nuclear;

import fr.modeminecraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Kit de combinaison Hazmat (6 toiles caoutchoutées + 2 lingots de plomb + 1 bloc de verre) : clic droit = les 4 pièces. */
public class HazmatKitItem extends Item {
    public HazmatKitItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack kit = player.getItemInHand(hand);
        if (!level.isClientSide) {
            for (String piece : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
                ItemStack st = new ItemStack(ModItems.RAD_SUITS.get("hazmat_suit_" + piece).get());
                if (!player.getInventory().add(st)) {
                    player.drop(st, false);
                }
            }
            kit.consume(1, player);
            player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1f, 0.8f);
        }
        return InteractionResultHolder.sidedSuccess(kit, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext c, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.hazmat_kit").withStyle(ChatFormatting.GRAY));
    }
}

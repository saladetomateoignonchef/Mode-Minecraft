package fr.modeminecraft.item;

import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Pile tactique CR123A : 10 minutes d'éclairage pour une lampe tactique.
 * - clic droit en main : remplace la pile de la lampe de l'arme tenue dans l'autre main
 *   (sinon de l'arme à lampe la plus déchargée de la barre d'action) ;
 * - dans l'inventaire : prendre la pile et faire un clic droit sur l'arme.
 */
public class TacticalBatteryItem extends Item {
    public TacticalBatteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack battery = player.getItemInHand(hand);
        ItemStack target = ItemStack.EMPTY;
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (needsBattery(other)) {
            target = other;
        } else {
            for (int i = 0; i < 9; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (needsBattery(s) && (target.isEmpty() || GunItem.lightBattery(s) < GunItem.lightBattery(target))) {
                    target = s;
                }
            }
        }
        if (target.isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.modeminecraft.battery.no_target").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(battery);
        }
        if (!level.isClientSide) {
            insert(player, target, battery);
        }
        return InteractionResultHolder.sidedSuccess(battery, level.isClientSide);
    }

    /** Dans l'inventaire : pile prise avec la souris, clic droit sur l'arme. */
    @Override
    public boolean overrideStackedOnOther(ItemStack battery, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !needsBattery(slot.getItem())) {
            return false;
        }
        insert(player, slot.getItem(), battery);
        slot.setChanged();
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack battery, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        return false;
    }

    /** Arme avec une lampe dont la pile n'est pas neuve. */
    public static boolean needsBattery(ItemStack stack) {
        return stack.getItem() instanceof GunItem && GunItem.light(stack) != null && GunItem.lightBattery(stack) < GunItem.BATTERY_SECONDS;
    }

    private static void insert(Player player, ItemStack gun, ItemStack battery) {
        GunItem.setLightBattery(gun, GunItem.BATTERY_SECONDS);
        if (!player.getAbilities().instabuild) {
            battery.shrink(1);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLASHLIGHT_CLICK.get(), SoundSource.PLAYERS, 0.7f, 0.6f);
        player.displayClientMessage(Component.translatable("message.modeminecraft.battery.inserted", gun.getHoverName()).withStyle(ChatFormatting.GREEN), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.battery.autonomy").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.modeminecraft.battery.use").withStyle(ChatFormatting.GRAY));
    }
}

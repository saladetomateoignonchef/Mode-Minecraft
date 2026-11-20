package fr.modeminecraft.entity.drone;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * Télécommande tactique : Maj + clic droit sur un drone posé pour l'associer (bip et LED verte),
 * puis clic droit pour prendre les commandes (caméra FPV). Maj ou R : le drone rentre seul.
 */
public class DroneControllerItem extends Item {
    public DroneControllerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static UUID linked(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().hasUUID("Drone") ? data.copyTag().getUUID("Drone") : null;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            UUID id = linked(stack);
            if (id == null) {
                sp.displayClientMessage(Component.translatable("message.modeminecraft.drone.not_paired").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            if (!(sl.getEntity(id) instanceof DroneEntity drone) || !drone.isAlive()) {
                sp.displayClientMessage(Component.translatable("message.modeminecraft.drone.no_signal").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            if (drone.distanceTo(sp) > drone.radioRange()) {
                sp.displayClientMessage(Component.translatable("message.modeminecraft.drone.signal_lost").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            if (drone.battery() <= 0) {
                sp.displayClientMessage(Component.translatable("message.modeminecraft.drone.battery").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            drone.owner = sp.getUUID();
            drone.startPilot(sp);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(linked(stack) != null ? "tooltip.modeminecraft.drone_controller.paired"
                : "tooltip.modeminecraft.drone_controller.unpaired").withStyle(linked(stack) != null ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.drone_controller.keys").withStyle(ChatFormatting.DARK_GRAY));
    }
}

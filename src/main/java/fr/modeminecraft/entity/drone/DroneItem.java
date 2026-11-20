package fr.modeminecraft.entity.drone;

import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Drone rangé : clic droit sur le sol pour le poser (puis Maj + clic droit avec la télécommande pour l'associer). */
public class DroneItem extends Item {
    private final DroneType type;

    public DroneItem(DroneType type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public DroneType type() {
        return type;
    }

    public static ItemStack create(DroneType type, CompoundTag data) {
        ItemStack s = new ItemStack(switch (type) {
            case HORNET -> ModItems.BLACK_HORNET.get();
            case FPV -> ModItems.FPV_FRELON.get();
            case MAVIC -> ModItems.MAVIC_TACTIQUE.get();
            case WASP -> ModItems.WASP_MG.get();
            case REAPER -> ModItems.REAPER_X.get();
        });
        DroneData.write(s, data);
        return s;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        DroneEntity drone = ModEntities.DRONE.get().create(level);
        if (drone == null) {
            return InteractionResult.FAIL;
        }
        CompoundTag data = DroneData.read(ctx.getItemInHand());
        data.putString("DroneType", type.id);
        if (!data.contains("Arms")) {
            data.putInt("Arms", type == DroneType.WASP ? 60 : type == DroneType.REAPER ? 4 : 0);   // neuf : armé
        }
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ctx.getRotation(), 0);
        drone.loadDrone(data);
        if (ctx.getPlayer() != null) {
            drone.owner = ctx.getPlayer().getUUID();
        }
        level.addFreshEntity(drone);
        ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft." + type.id).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.drone.range", type.range).withStyle(ChatFormatting.DARK_AQUA));
        CompoundTag d = DroneData.read(stack);
        if (d.getBoolean("Silent")) {
            tooltip.add(Component.translatable("item.modeminecraft.silent_rotors").withStyle(ChatFormatting.AQUA));
        }
        if (d.getBoolean("HighCap")) {
            tooltip.add(Component.translatable("item.modeminecraft.high_cap_battery").withStyle(ChatFormatting.AQUA));
        }
        if (d.getBoolean("Cargo")) {
            tooltip.add(Component.translatable("item.modeminecraft.cargo_module").withStyle(ChatFormatting.AQUA));
        }
        if (d.contains("Payload") && context.registries() != null) {
            ItemStack p = DroneData.payload(d, context.registries());
            tooltip.add(Component.literal("+ " + p.getCount() + " x ").append(p.getHoverName()).withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("tooltip.modeminecraft.drone.how").withStyle(ChatFormatting.DARK_GRAY));
    }
}

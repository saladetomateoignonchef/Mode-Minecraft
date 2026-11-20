package fr.modeminecraft.item;

import fr.modeminecraft.entity.VehicleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Pose un véhicule conduisible (clic droit sur le sol). */
public class VehicleItem extends Item {
    private final VehicleEntity.Type type;

    public VehicleItem(VehicleEntity.Type type, Properties p) {
        super(p.stacksTo(1));
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!ctx.getLevel().isClientSide) {
            BlockPos p = ctx.getClickedPos().relative(ctx.getClickedFace());
            float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0;
            VehicleEntity v = new VehicleEntity(ctx.getLevel(), type, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw);
            if (!ctx.getLevel().noCollision(v, v.getBoundingBox())) {
                return InteractionResult.FAIL;
            }
            ctx.getLevel().addFreshEntity(v);
            if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext c, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.vehicle.stats", Math.round(type.maxSpeed * 72), (int) type.health, type.seats)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.vehicle.controls" + (type.siren ? "_siren" : "")).withStyle(ChatFormatting.DARK_GRAY));
    }
}

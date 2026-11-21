package fr.modeminecraft.item.military;

import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Pose un véhicule militaire (clic droit sur le sol) : réservoir à 60 %, soutes à moitié pleines. */
public class MilVehicleItem extends Item {
    public final MilType type;

    public MilVehicleItem(MilType type, Properties p) {
        super(p.stacksTo(1));
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!ctx.getLevel().isClientSide) {
            BlockPos p = ctx.getClickedPos().relative(ctx.getClickedFace());
            float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0;
            MilVehicleEntity v = null;
            for (int dy = 0; dy <= 3; dy++) {
                MilVehicleEntity c = MilVehicleEntity.create(ctx.getLevel(), type, p.getX() + 0.5, p.getY() + dy, p.getZ() + 0.5, yaw);
                if (ctx.getLevel().noCollision(c, c.getBoundingBox().deflate(0.05))) {
                    v = c;
                    break;
                }
            }
            if (v == null) {
                return InteractionResult.FAIL;
            }
            v.fillAmmo(0.5f);
            ctx.getLevel().addFreshEntity(v);
            if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext c, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.mil.stats", type.kmh, (int) type.health, type.seats.size())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.mil." + type.id).withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable(type.heli() ? "tooltip.modeminecraft.mil.controls_heli" : "tooltip.modeminecraft.mil.controls")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}

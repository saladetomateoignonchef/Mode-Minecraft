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
        if (type.naval()) {
            return InteractionResult.PASS;                                       // navires : posés sur l'eau (voir use)
        }
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

    /** Navire : clic droit vers une étendue d'eau libre, il est mis à l'eau à sa ligne de flottaison. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                                                                      net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!type.naval()) {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
        var hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                || !level.getFluidState(hit.getBlockPos()).is(net.minecraft.tags.FluidTags.WATER)) {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            BlockPos p = hit.getBlockPos();
            while (level.getFluidState(p.above()).is(net.minecraft.tags.FluidTags.WATER)) {
                p = p.above();
            }
            double surf = p.getY() + 0.9;
            // le navire est mis à l'eau devant le joueur (pas sur lui)
            float yaw = player.getYRot();
            double rad = Math.toRadians(yaw), back = MilVehicleEntity.dims(type)[1] * 0.5 + 2;
            double x = hit.getLocation().x - Math.sin(rad) * 0, z = hit.getLocation().z;
            double dx = x - player.getX(), dz = z - player.getZ(), dist = Math.hypot(dx, dz);
            if (dist < back) {
                x = player.getX() - Math.sin(rad) * back;
                z = player.getZ() + Math.cos(rad) * back;
            }
            MilVehicleEntity v = MilVehicleEntity.create(level, type, x, surf - type.draft(), z, yaw);
            if (!level.noCollision(v, v.getBoundingBox().deflate(0.1))) {
                player.displayClientMessage(Component.translatable("message.modeminecraft.naval.need_water").withStyle(ChatFormatting.RED), true);
                return net.minecraft.world.InteractionResultHolder.fail(stack);
            }
            v.fillAmmo(0.5f);
            level.addFreshEntity(v);
            if (type.carrier()) {
                v.populateDeck();                                                  // 4 chasseurs sur le pont
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext c, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.mil.stats", type.kmh, (int) type.health, type.seats.size())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.mil." + type.id).withStyle(ChatFormatting.DARK_GREEN));
        if (type.plane() && type.rcs() < 1) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.mil.stealth").withStyle(ChatFormatting.DARK_PURPLE));
        }
        tooltip.add(Component.translatable(type.submarine() ? "tooltip.modeminecraft.mil.controls_sub" : type.naval() ? "tooltip.modeminecraft.mil.controls_ship"
                : type.plane() && type != fr.modeminecraft.entity.military.MilType.B29_SUPERFORTRESS ? "tooltip.modeminecraft.mil.controls_jet"
                : type.plane() ? "tooltip.modeminecraft.mil.controls_plane" : type.heli() ? "tooltip.modeminecraft.mil.controls_heli" : "tooltip.modeminecraft.mil.controls")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}

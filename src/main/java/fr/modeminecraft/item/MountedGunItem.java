package fr.modeminecraft.item;

import fr.modeminecraft.entity.MountedGunEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Mitrailleuse lourde à poser au sol (clic droit sur un bloc). */
public class MountedGunItem extends Item {
    private final MountedGunEntity.Kind kind;

    public MountedGunItem(MountedGunEntity.Kind kind, Properties properties) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        if (!level.isClientSide) {
            var pos = context.getClickLocation();
            float yaw = context.getPlayer() == null ? 0 : context.getPlayer().getYRot();
            level.addFreshEntity(new MountedGunEntity(level, kind, pos.x, Math.floor(pos.y + 0.01), pos.z, yaw));
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.mounted_gun").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.damage", kind.damage).withStyle(ChatFormatting.GRAY));
    }
}

package fr.modeminecraft.item.military;

import fr.modeminecraft.entity.military.RocketLauncherEntity;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Lance-roquettes Type 63 replié : clic droit sur le dessus d'un bloc pour le poser (tubes vers où l'on regarde). */
public class RocketLauncherItem extends Item {
    public RocketLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }
        var hit = context.getClickLocation();
        double y = pos.getY() + 1.0;
        var kind = RocketLauncherEntity.Kind.TYPE63;
        var box = net.minecraft.world.phys.AABB.ofSize(new net.minecraft.world.phys.Vec3(hit.x, y + kind.height / 2 + 0.05, hit.z),
                kind.width, kind.height - 0.1, kind.width);
        if (!level.noCollision(box)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            float yaw = context.getPlayer() == null ? 0 : context.getPlayer().getYRot();
            level.addFreshEntity(new RocketLauncherEntity(level, kind, hit.x, y, hit.z, yaw, false));
            level.playSound(null, hit.x, y, hit.z, ModSounds.TURRET_DEPLOY.get(), SoundSource.PLAYERS, 1.2f, 0.8f);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.type63").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.modeminecraft.launcher.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.launcher.fire").withStyle(ChatFormatting.DARK_GRAY));
    }
}

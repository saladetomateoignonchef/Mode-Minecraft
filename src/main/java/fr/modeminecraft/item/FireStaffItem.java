package fr.modeminecraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bâton de feu : clic droit pour lancer une boule de feu (comme un blaze).
 */
public class FireStaffItem extends Item {
    private static final int COOLDOWN_TICKS = 15;

    public FireStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            Vec3 look = player.getLookAngle();
            SmallFireball fireball = new SmallFireball(level, player, look.scale(1.5));
            fireball.setPos(player.getX() + look.x, player.getEyeY() - 0.1 + look.y, player.getZ() + look.z);
            level.addFreshEntity(fireball);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        WeaponTooltips.add(tooltip, "fire_staff");
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

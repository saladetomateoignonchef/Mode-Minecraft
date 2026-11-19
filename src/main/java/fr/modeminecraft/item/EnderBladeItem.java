package fr.modeminecraft.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Lame de l'End : clic droit pour se téléporter jusqu'à 8 blocs dans la direction du regard.
 * On cherche la position libre la plus lointaine pour ne jamais finir dans un mur.
 */
public class EnderBladeItem extends MagicSwordItem {
    private static final int MAX_DISTANCE = 8;
    private static final int COOLDOWN_TICKS = 60; // 3 secondes

    public EnderBladeItem(Tier tier, Properties properties, String tooltipKey) {
        super(tier, properties, tooltipKey, (target, attacker) -> {});
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 look = player.getLookAngle();

        for (int distance = MAX_DISTANCE; distance >= 2; distance--) {
            Vec3 offset = look.scale(distance);
            if (level.noCollision(player, player.getBoundingBox().move(offset))) {
                if (!level.isClientSide) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    Vec3 dest = player.position().add(offset);
                    player.teleportTo(dest.x, dest.y, dest.z);
                    player.resetFallDistance();
                    level.playSound(null, dest.x, dest.y, dest.z,
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                }
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        return InteractionResultHolder.fail(stack);
    }
}

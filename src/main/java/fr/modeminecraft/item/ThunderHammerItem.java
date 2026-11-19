package fr.modeminecraft.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Marteau du tonnerre : onde de choc au corps à corps (comme le marteau de guerre)
 * + clic droit pour faire tomber la foudre là où on regarde (jusqu'à 32 blocs).
 */
public class ThunderHammerItem extends WarHammerItem {
    private static final double RANGE = 32.0;
    private static final int COOLDOWN_TICKS = 100; // 5 secondes

    public ThunderHammerItem(Tier tier, Properties properties, String tooltipKey) {
        super(tier, properties, tooltipKey);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(RANGE, 0.0f, false);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                Vec3 pos = hit.getLocation();
                bolt.moveTo(pos.x, pos.y, pos.z);
                if (player instanceof ServerPlayer serverPlayer) {
                    bolt.setCause(serverPlayer);
                }
                level.addFreshEntity(bolt);
            }
            stack.hurtAndBreak(2, player, LivingEntity.getSlotForHand(hand));
        }
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

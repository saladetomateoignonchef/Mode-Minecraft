package fr.modeminecraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Marteau de guerre : lent mais très puissant.
 * Chaque coup crée une onde de choc qui blesse et repousse les ennemis autour de la cible.
 */
public class WarHammerItem extends SwordItem {
    private static final double SHOCKWAVE_RADIUS = 2.5;
    private static final float SHOCKWAVE_DAMAGE = 4.0f;

    private final String tooltipKey;

    public WarHammerItem(Tier tier, Properties properties, String tooltipKey) {
        super(tier, properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel level) {
            DamageSource source = attacker instanceof Player player
                    ? level.damageSources().playerAttack(player)
                    : level.damageSources().mobAttack(attacker);

            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(SHOCKWAVE_RADIUS),
                    e -> e != attacker && e != target && e.isAlive() && !e.isAlliedTo(attacker));

            for (LivingEntity e : nearby) {
                e.hurt(source, SHOCKWAVE_DAMAGE);
                e.knockback(0.8, attacker.getX() - e.getX(), attacker.getZ() - e.getZ());
            }

            level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(),
                    1, 0, 0, 0, 0);
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        WeaponTooltips.add(tooltip, tooltipKey);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

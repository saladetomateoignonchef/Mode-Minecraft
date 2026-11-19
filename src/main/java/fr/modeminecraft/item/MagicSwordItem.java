package fr.modeminecraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Épée (ou dague) qui applique un effet spécial à chaque coup porté.
 * L'effet est passé en paramètre : (cible, attaquant) -> { ... }.
 */
public class MagicSwordItem extends SwordItem {
    private final BiConsumer<LivingEntity, LivingEntity> onHit;
    private final String tooltipKey;

    public MagicSwordItem(Tier tier, Properties properties, String tooltipKey,
                          BiConsumer<LivingEntity, LivingEntity> onHit) {
        super(tier, properties);
        this.tooltipKey = tooltipKey;
        this.onHit = onHit;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            onHit.accept(target, attacker);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        WeaponTooltips.add(tooltip, tooltipKey);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

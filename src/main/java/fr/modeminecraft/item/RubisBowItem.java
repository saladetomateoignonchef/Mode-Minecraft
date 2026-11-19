package fr.modeminecraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Arc en rubis : fonctionne comme un arc normal, mais les flèches font plus de dégâts.
 */
public class RubisBowItem extends BowItem {
    private static final double EXTRA_DAMAGE = 1.5; // flèche vanilla = 2.0 de base

    public RubisBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        arrow.setBaseDamage(arrow.getBaseDamage() + EXTRA_DAMAGE);
        return arrow;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        WeaponTooltips.add(tooltip, "rubis_bow");
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

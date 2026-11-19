package fr.modeminecraft.item;

import fr.modeminecraft.entity.MountedGunEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Caisse / bande de munitions d'une tourelle : clic droit sur la tourelle (pas montée dessus) pour la charger. */
public class TurretAmmoItem extends Item {
    public TurretAmmoItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (MountedGunEntity.Kind k : MountedGunEntity.Kind.values()) {
            if (k.ammo() == this) {
                tooltip.add(Component.translatable("tooltip.modeminecraft.turret_ammo", k.perLoad, k.item().getDescription())
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        tooltip.add(Component.translatable("tooltip.modeminecraft.turret_ammo.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}

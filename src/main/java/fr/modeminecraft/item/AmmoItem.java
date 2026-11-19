package fr.modeminecraft.item;

import fr.modeminecraft.item.gun.AmmoType;
import fr.modeminecraft.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Munition (un calibre). Peut avoir un type spécial (traçante, perforante...) stocké dans l'ItemStack.
 */
public class AmmoItem extends Item {
    private final boolean shotgunShell;

    public AmmoItem(boolean shotgunShell, Properties properties) {
        super(properties);
        this.shotgunShell = shotgunShell;
    }

    public boolean isShotgunShell() {
        return shotgunShell;
    }

    public static AmmoType getType(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AMMO_TYPE.get(), AmmoType.STANDARD);
    }

    /** Une pile de munitions de ce calibre, avec ce type. */
    public static ItemStack of(Item ammo, AmmoType type, int count) {
        ItemStack stack = new ItemStack(ammo, count);
        if (type != AmmoType.STANDARD) {
            stack.set(ModDataComponents.AMMO_TYPE.get(), type);
        }
        return stack;
    }

    /** "Munitions 5,56 mm" -> "Munitions 5,56 mm (traçantes)" */
    @Override
    public Component getName(ItemStack stack) {
        AmmoType type = getType(stack);
        Component base = super.getName(stack);
        return type == AmmoType.STANDARD ? base
                : Component.translatable("item.modeminecraft.ammo.typed", base, Component.translatable(type.translationKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        AmmoType type = getType(stack);
        if (type != AmmoType.STANDARD) {
            tooltip.add(Component.translatable(type.descriptionKey()).withStyle(ChatFormatting.BLUE));
        }
        tooltip.add(Component.translatable("tooltip.modeminecraft.ammo.switch").withStyle(ChatFormatting.DARK_GRAY));
    }
}

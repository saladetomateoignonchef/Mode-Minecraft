package fr.modeminecraft.nuclear;

import fr.modeminecraft.registry.ModArmorMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Tenues anti-radiation : Hazmat civile (étanche, fragile) et tenue de combat NBC militaire (étanche + blindée, consomme
 * des cartouches filtrantes au cœur du cratère). Les 4 pièces portées (mélange possible) bloquent 100 % des radiations
 * tant qu'elles ont de la durabilité.
 */
public class RadiationSuitItem extends ArmorItem {
    public enum Suit {
        HAZMAT("hazmat_suit"), NBC("nbc_suit");

        public final String id;

        Suit(String id) {
            this.id = id;
        }

        Holder<ArmorMaterial> material() {
            return this == HAZMAT ? ModArmorMaterials.HAZMAT : ModArmorMaterials.NBC;
        }
    }

    public final Suit suit;

    public RadiationSuitItem(Suit suit, Type type, Properties p) {
        super(suit.material(), type, p);
        this.suit = suit;
    }

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Les 4 pièces portées, toutes en état ? */
    public static boolean fullyProtected(LivingEntity e) {
        for (EquipmentSlot s : SLOTS) {
            ItemStack st = e.getItemBySlot(s);
            if (!(st.getItem() instanceof RadiationSuitItem) || st.isDamageableItem() && st.getDamageValue() >= st.getMaxDamage() - 1) {
                return false;
            }
        }
        return true;
    }

    /** Tenue NBC complète (les 4 pièces militaires) ? */
    public static boolean nbc(LivingEntity e) {
        for (EquipmentSlot s : SLOTS) {
            if (!(e.getItemBySlot(s).getItem() instanceof RadiationSuitItem r) || r.suit != Suit.NBC) {
                return false;
            }
        }
        return true;
    }

    /** Le masque de la tenue sert aussi de visière contre le flash nucléaire. */
    public static boolean visor(LivingEntity e) {
        return e.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof RadiationSuitItem;
    }

    /** Usure de la tenue en zone irradiée (« saturation »), une fois par seconde. */
    public static void wear(LivingEntity e, int amount) {
        for (EquipmentSlot s : SLOTS) {
            ItemStack st = e.getItemBySlot(s);
            if (st.getItem() instanceof RadiationSuitItem && st.isDamageableItem() && st.getDamageValue() < st.getMaxDamage() - 1) {
                st.setDamageValue(Math.min(st.getMaxDamage() - 1, st.getDamageValue() + amount));
            }
        }
    }

    /** Saturation moyenne de la tenue portée (0 = neuve, 1 = hors d'usage), -1 sans tenue complète. */
    public static float saturation(LivingEntity e) {
        float sum = 0;
        for (EquipmentSlot s : SLOTS) {
            ItemStack st = e.getItemBySlot(s);
            if (!(st.getItem() instanceof RadiationSuitItem)) {
                return -1;
            }
            sum += st.isDamageableItem() ? st.getDamageValue() / (float) st.getMaxDamage() : 0;
        }
        return sum / 4;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext c, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.rad_suit." + suit.id).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.modeminecraft.rad_suit.full").withStyle(ChatFormatting.GRAY));
    }
}

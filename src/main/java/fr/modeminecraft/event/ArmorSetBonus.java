package fr.modeminecraft.event;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModArmorMaterials;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Bonus de set complet : porter les 4 pièces d'armure en rubis donne Force I.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class ArmorSetBonus {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        // Une vérification par seconde suffit, et uniquement côté serveur
        if (player.level().isClientSide || player.tickCount % 20 != 0) {
            return;
        }
        if (isWearingFullRubis(player)) {
            // ambient = true, particules cachées, icône visible
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
        }
    }

    private static boolean isWearingFullRubis(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getMaterial().value() != ModArmorMaterials.RUBIS.value()) {
                return false;
            }
        }
        return true;
    }

    private ArmorSetBonus() {}
}

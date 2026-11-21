package fr.modeminecraft.item;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunStats;
import fr.modeminecraft.registry.ModDataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Combo bouclier balistique + arme de poing : bouclier dans la main secondaire, pistolet ou revolver dans la main
 * principale. Clic droit maintenu = bouclier levé devant le torse (au lieu de viser) ; clic gauche = tir à une main sur
 * le côté du bouclier (dispersion x1,6). Pendant un rechargement, le bouclier s'abaisse le temps de remettre un
 * chargeur, puis remonte tout seul si le clic droit est toujours maintenu.
 * <p>
 * Un bouclier balistique ne s'utilise qu'avec une arme de poing ou une arme blanche légère : avec une arme à deux mains
 * (fusil, fusil de précision, fusil à pompe, mitrailleuse, lance-roquettes, arme blanche lourde ou longue), le bouclier
 * ne se lève plus, et l'arme à feu ne peut ni viser ni tirer tant que le bouclier est dans l'autre main.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class ShieldCombat {
    public static final float ONE_HAND_SPREAD = 1.6f;

    public static boolean active(Player player) {
        return player.getOffhandItem().getItem() instanceof BallisticShieldItem
                && player.getMainHandItem().getItem() instanceof GunItem gun && gun.stats().category() == GunStats.Category.PISTOL;
    }

    /** Arme qui se tient à deux mains (incompatible avec le bouclier balistique). */
    public static boolean twoHanded(ItemStack stack) {
        if (stack.getItem() instanceof GunItem gun) {
            return gun.stats().category() != GunStats.Category.PISTOL;
        }
        if (stack.getItem() instanceof fr.modeminecraft.item.melee.MeleeWeaponItem m) {
            return m.stats().speed() < 1.0f || m.stats().reach() > 4.0f;          // masses, épées longues, armes d'hast
        }
        return false;
    }

    /** Bouclier dans la main secondaire + arme à deux mains : bouclier inutilisable, arme bloquée. */
    public static boolean blocked(net.minecraft.world.entity.LivingEntity e) {
        return e.getOffhandItem().getItem() instanceof BallisticShieldItem && twoHanded(e.getMainHandItem());
    }

    /** Message « impossible » (au plus toutes les 2 s). */
    public static void warn(Player player) {
        if (!player.level().isClientSide && player.tickCount % 40 == 0) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.modeminecraft.shield.two_handed")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
        }
    }

    /** L'arme de poing est en train d'être rechargée : le bouclier doit rester baissé. */
    public static boolean reloading(Player player) {
        ItemStack gun = player.getMainHandItem();
        Long start = gun.get(ModDataComponents.RELOAD_START.get());
        Long end = gun.get(ModDataComponents.RELOAD_END.get());
        long now = player.level().getGameTime();
        return start != null && end != null && now >= start && now < end;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (active(player) && reloading(player) && player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            player.stopUsingItem();                                  // abaissement temporaire pour recharger
        }
        if (blocked(player) && player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            player.stopUsingItem();                                  // on a sorti une arme à deux mains : garde annulée
            if (!player.level().isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.modeminecraft.shield.two_handed")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
            }
        }
    }

    private ShieldCombat() {}
}

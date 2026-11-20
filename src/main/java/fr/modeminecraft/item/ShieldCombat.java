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
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class ShieldCombat {
    public static final float ONE_HAND_SPREAD = 1.6f;

    public static boolean active(Player player) {
        return player.getOffhandItem().getItem() instanceof BallisticShieldItem
                && player.getMainHandItem().getItem() instanceof GunItem gun && gun.stats().category() == GunStats.Category.PISTOL;
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
    }

    private ShieldCombat() {}
}

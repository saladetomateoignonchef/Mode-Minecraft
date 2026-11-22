package fr.modeminecraft.entity.train;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Règles du train : pas de descente en marche, et un joueur étourdi ne peut ni tirer, ni frapper, ni utiliser d'objet. */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class TrainEvents {
    private TrainEvents() {}

    /** Pas de saut du train en marche (sauf expulsion ou changement de wagon voulus par le jeu). */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!event.isDismounting() || TrainStaff.forced() || !(event.getEntityMounting() instanceof Player p)) {
            return;
        }
        if (event.getEntityBeingMounted() instanceof MesaTrainCarEntity car && !car.isRemoved() && Math.abs(car.trainSpeed()) > 0.08f) {
            event.setCanceled(true);
            if (!p.level().isClientSide) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.train.no_jump"), true);
            }
        }
    }

    private static boolean stunned(Player p) {
        return p.hasEffect(ModEffects.STUNNED);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (stunned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (stunned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (stunned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player p && stunned(p)) {
            event.setCanceled(true);
        }
    }
}

package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.network.ReloadPayload;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Événements client liés aux armes à feu (touche R, vitesse de déplacement, zoom du sniper). */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class GunClientEvents {

    /** Touche R : on demande au serveur de recharger l'arme en main. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ModKeyMappings.RELOAD.consumeClick()) {
            if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof GunItem) {
                PacketDistributor.sendToServer(ReloadPayload.INSTANCE);
            }
        }
    }

    /**
     * Minecraft ralentit le joueur (x0.2) quand il "utilise" un objet, comme pour tendre un arc.
     * Le fusil d'assaut tire en mode "utilisation" : on compense pour pouvoir marcher normalement.
     * (Cet événement est appelé juste AVANT le ralentissement vanilla.)
     */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (player.isUsingItem() && !player.isPassenger() && player.getUseItem().getItem() instanceof GunItem) {
            event.getInput().leftImpulse *= 5.0f;
            event.getInput().forwardImpulse *= 5.0f;
        }
    }

    /** Zoom x5 quand on s'accroupit avec le sniper en main. */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (isScoping(event.getPlayer())) {
            event.setNewFovModifier(0.2f);
        }
    }

    /** On cache la main et l'arme pendant la visée à la lunette. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isScoping(Minecraft.getInstance().player)) {
            event.setCanceled(true);
        }
    }

    public static boolean isScoping(Player player) {
        return player != null
                && player.isCrouching()
                && player.getMainHandItem().is(ModItems.SNIPER_RIFLE.get())
                && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    public static boolean isScoping(LocalPlayer player) {
        return isScoping((Player) player);
    }

    private GunClientEvents() {}
}

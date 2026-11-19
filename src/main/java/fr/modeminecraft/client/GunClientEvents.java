package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.ResolvedGun;
import fr.modeminecraft.item.gun.SightOverlay;
import fr.modeminecraft.network.ReloadPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/** Événements client liés aux armes à feu (touche R, vitesse de déplacement, visée). */
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
     * Les armes automatiques tirent en mode "utilisation" : on compense pour pouvoir marcher normalement.
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

    /** Zoom quand on vise (accroupi) : lunette du sniper ou viseur monté sur l'arme. */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        ResolvedGun aim = aiming(event.getPlayer());
        if (aim != null && aim.zoom() < 1.0f) {
            event.setNewFovModifier(aim.zoom());
        }
    }

    /** On cache la main et l'arme quand on regarde dans une lunette. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        ResolvedGun aim = aiming(Minecraft.getInstance().player);
        if (aim != null && aim.overlay().isFullScope()) {
            event.setCanceled(true);
        }
    }

    /** Quand on vise avec un viseur, son réticule remplace la croix de visée de Minecraft. */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            ResolvedGun aim = aiming(Minecraft.getInstance().player);
            if (aim != null && aim.overlay() != SightOverlay.NONE) {
                event.setCanceled(true);
            }
        }
    }

    /** Statistiques de l'arme si le joueur est en train de viser en vue à la 1re personne, sinon null. */
    public static @Nullable ResolvedGun aiming(@Nullable Player player) {
        if (player == null || !player.isCrouching()
                || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof GunItem gun ? gun.resolve(stack) : null;
    }

    private GunClientEvents() {}
}

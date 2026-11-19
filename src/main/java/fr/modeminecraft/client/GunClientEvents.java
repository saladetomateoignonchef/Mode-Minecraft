package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.ResolvedGun;
import fr.modeminecraft.item.gun.SightOverlay;
import fr.modeminecraft.item.gun.ProneHandler;
import fr.modeminecraft.network.AmmoTypePayload;
import fr.modeminecraft.network.CheckChamberPayload;
import fr.modeminecraft.network.FireModePayload;
import fr.modeminecraft.network.PronePayload;
import fr.modeminecraft.network.UnderbarrelPayload;
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

    /** Touche R : recharger. Touche B : changer de mode de tir. (Le serveur fait le travail.) */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean holdingGun = mc.player != null && mc.player.getMainHandItem().getItem() instanceof GunItem;
        while (ModKeyMappings.RELOAD.consumeClick()) {
            if (holdingGun) {
                PacketDistributor.sendToServer(ReloadPayload.INSTANCE);
            }
        }
        while (ModKeyMappings.FIRE_MODE.consumeClick()) {
            if (holdingGun) {
                PacketDistributor.sendToServer(FireModePayload.INSTANCE);
            }
        }
        while (ModKeyMappings.AMMO_TYPE.consumeClick()) {
            if (holdingGun) {
                PacketDistributor.sendToServer(AmmoTypePayload.INSTANCE);
            }
        }
        while (ModKeyMappings.LASER.consumeClick()) {
            if (holdingGun) {
                PacketDistributor.sendToServer(fr.modeminecraft.network.LaserPayload.INSTANCE);
            }
        }
        while (ModKeyMappings.UNDERBARREL.consumeClick()) {
            if (holdingGun) {
                PacketDistributor.sendToServer(UnderbarrelPayload.INSTANCE);
            }
        }
        while (ModKeyMappings.CHECK_CHAMBER.consumeClick()) {
            if (holdingGun) {
                PacketDistributor.sendToServer(CheckChamberPayload.INSTANCE);
            }
        }
        handleProne(mc);
    }

    private static boolean sneakWasDown;
    private static long lastSneakPress = -100;
    private static boolean jumpWasDown;

    /** Double appui sur "s'accroupir" = s'allonger / se relever. Sauter = se relever aussi. */
    private static void handleProne(Minecraft mc) {
        if (mc.player == null || mc.screen != null) {
            return;
        }
        long now = mc.player.level().getGameTime();
        boolean sneak = mc.options.keyShift.isDown();
        if (sneak && !sneakWasDown) {
            if (now - lastSneakPress <= 7) {
                toggleProne(mc, !ProneHandler.isProne(mc.player));
                lastSneakPress = -100;
            } else {
                lastSneakPress = now;
            }
        }
        sneakWasDown = sneak;
        boolean jump = mc.options.keyJump.isDown();
        if (jump && !jumpWasDown && ProneHandler.isProne(mc.player)) {
            toggleProne(mc, false);
        }
        jumpWasDown = jump;
    }

    private static void toggleProne(Minecraft mc, boolean prone) {
        if (ProneHandler.setProne(mc.player, prone)) {
            PacketDistributor.sendToServer(new PronePayload(prone));
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
        // Mobilité en visant (crosses légères, sans crosse...)
        ResolvedGun aim = aiming(player);
        if (aim != null && aim.mobility() != 1.0f) {
            event.getInput().leftImpulse *= aim.mobility();
            event.getInput().forwardImpulse *= aim.mobility();
        }
    }

    private static float currentZoom = 1.0f;

    /** Zoom quand on vise (accroupi), progressif selon la vitesse de visée des accessoires. */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        ResolvedGun aim = aiming(event.getPlayer());
        float target = aim == null ? 1.0f : SightEffects.currentMode(aim).zoom();
        float speed = aim == null ? 0.4f : Math.min(1.0f, 0.3f * aim.aimSpeed());
        currentZoom += (target - currentZoom) * speed;
        if (Math.abs(currentZoom - 1.0f) > 0.002f) {
            event.setNewFovModifier(event.getNewFovModifier() * currentZoom);
        }
    }

    /** On cache la main et l'arme quand on regarde dans une lunette. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        ResolvedGun aim = aiming(Minecraft.getInstance().player);
        if (aim != null && SightEffects.currentMode(aim).overlay().isFullScope()
                && GunPoseHelper.aimProgress(event.getPartialTick()) > 0.85f) {
            event.setCanceled(true);
        }
    }

    /** Quand on vise avec un viseur, son réticule remplace la croix de visée de Minecraft. */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            ResolvedGun aim = aiming(Minecraft.getInstance().player);
            if (aim != null && SightEffects.currentMode(aim).overlay() != SightOverlay.NONE) {
                event.setCanceled(true);
            }
        }
    }

    /** Statistiques de l'arme si le joueur est en train de viser en vue à la 1re personne, sinon null. */
    public static @Nullable ResolvedGun aiming(@Nullable Player player) {
        if (player == null || !GunItem.isAiming(player)
                || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof GunItem gun ? gun.resolve(stack) : null;
    }

    private GunClientEvents() {}
}

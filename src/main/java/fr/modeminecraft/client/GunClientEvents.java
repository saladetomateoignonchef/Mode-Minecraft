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
        while (ModKeyMappings.MELEE.consumeClick()) {
            if (holdingGun) {
                PacketDistributor.sendToServer(fr.modeminecraft.network.MeleePayload.INSTANCE);
            }
        }
        while (ModKeyMappings.CHECK_CHAMBER.consumeClick()) {
            if (holdingGun) {
                PacketDistributor.sendToServer(CheckChamberPayload.INSTANCE);
            }
        }
        handleProne(mc);
    }

    private static boolean triggerSent, aimSent, useWasDown;
    private static long usePressTick = -1;

    /**
     * Avant que Minecraft ne traite les clics : avec une arme, clic gauche = détente, clic droit = viser.
     */
    @SubscribeEvent
    public static void onTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        boolean mounted = player.getVehicle() instanceof fr.modeminecraft.entity.MountedGunEntity;
        boolean gun = (player.getMainHandItem().getItem() instanceof GunItem || mounted) && mc.screen == null
                && !player.isSpectator() && mc.mouseHandler.isMouseGrabbed();
        boolean trigger = gun && mc.options.keyAttack.isDown();
        boolean aim = gun && mc.options.keyUse.isDown();
        boolean use = mc.options.keyUse.isDown();
        if (use && !useWasDown) {
            usePressTick = player.level().getGameTime();
        }
        useWasDown = use;
        if (trigger != triggerSent || aim != aimSent) {
            triggerSent = trigger;
            aimSent = aim;
            fr.modeminecraft.item.gun.TriggerHandler.setTrigger(player, trigger);
            fr.modeminecraft.item.gun.TriggerHandler.setAiming(player, aim);
            PacketDistributor.sendToServer(new fr.modeminecraft.network.TriggerPayload(trigger, aim));
        }
    }

    /**
     * Arme en main : le clic gauche ne casse pas de bloc et ne frappe pas (il tire) ;
     * le clic droit ne garde que son 1er appui (ouvrir une porte, une table...), sans répétition.
     */
    @SubscribeEvent
    public static void onClickInput(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        boolean mounted = mc.player != null && mc.player.getVehicle() instanceof fr.modeminecraft.entity.MountedGunEntity;
        if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GunItem) && !mounted) {
            return;
        }
        if (event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        } else if (event.isUseItem() && mc.player.level().getGameTime() != usePressTick) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
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
        boolean sneak = mc.options.keyShift.isDown() && !GunItem.isAiming(mc.player); // en visant, Shift stabilise

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
        // En visant on marche plus lentement (x0,65), selon la mobilité des accessoires (crosses légères...)
        ResolvedGun aim = player.getMainHandItem().getItem() instanceof GunItem g && GunItem.isAiming(player)
                ? g.resolve(player.getMainHandItem()) : null;
        if (aim != null) {
            float mul = 0.65f * aim.mobility();
            event.getInput().leftImpulse *= mul;
            event.getInput().forwardImpulse *= mul;
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
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.getVehicle() instanceof fr.modeminecraft.entity.MountedGunEntity) {
            event.setCanceled(true); // sur une mitrailleuse posée : les mains sont sur les poignées
            return;
        }
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

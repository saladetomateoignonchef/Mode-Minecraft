package fr.modeminecraft.client;

import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.LeanHandler;
import fr.modeminecraft.item.gun.ProneHandler;
import fr.modeminecraft.item.gun.ResolvedGun;
import fr.modeminecraft.network.LeanPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Visée côté client :
 * - se pencher à gauche / droite (A / E sur AZERTY) seulement en visant : hors visée ces touches gardent
 *   leur rôle normal (jeter / inventaire) ;
 * - respiration du sniper : la lunette bouge, on peut retenir son souffle quelques secondes ;
 * - fumée du canon des mitrailleuses brûlantes.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class AimClient {
    private static final float LEAN_ROLL = 12f;   // inclinaison de la caméra (degrés)
    private static final int BREATH_MAX = 100;    // 5 secondes de souffle retenu

    private static int leanTarget;
    private static float lean, leanPrev;          // -1 à 1, lissé

    private static float breath = BREATH_MAX;
    private static boolean holdingBreath, exhausted;
    private static double swayYaw, swayPitch;     // balancement déjà appliqué à la caméra

    public static float lean(float partialTick) {
        return Mth.lerp(partialTick, leanPrev, lean);
    }

    public static float breath() {
        return breath / BREATH_MAX;
    }

    public static boolean holdingBreath() {
        return holdingBreath;
    }

    public static boolean exhausted() {
        return exhausted;
    }

    /** Avant que Minecraft ne traite les touches : en visant, A / E servent à se pencher. */
    @SubscribeEvent
    public static void onTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        boolean aiming = player != null && mc.screen == null && player.getMainHandItem().getItem() instanceof GunItem
                && GunItem.isAiming(player);
        int want = 0;
        if (aiming) {
            if (ModKeyMappings.LEAN_LEFT.isDown()) {
                want--;
            }
            if (ModKeyMappings.LEAN_RIGHT.isDown()) {
                want++;
            }
            // Les touches de Minecraft sur la même touche (jeter, inventaire...) ne doivent pas réagir
            for (KeyMapping vanilla : new KeyMapping[]{mc.options.keyDrop, mc.options.keyInventory, mc.options.keySwapOffhand}) {
                if (vanilla.same(ModKeyMappings.LEAN_LEFT) || vanilla.same(ModKeyMappings.LEAN_RIGHT)) {
                    while (vanilla.consumeClick()) {
                        // on vide les appuis
                    }
                }
            }
        }
        if (player != null && want != leanTarget) {
            leanTarget = want;
            PacketDistributor.sendToServer(new LeanPayload(want));
        }
    }

    @SubscribeEvent
    public static void onTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Penché : la tête glisse doucement sur le côté (moins si un mur gêne)
        leanPrev = lean;
        double room = leanTarget == 0 ? 1 : LeanHandler.offsetFor(player, leanTarget).length() / LeanHandler.MAX_OFFSET;
        lean += (float) ((leanTarget * room - lean) * 0.35);

        // Souffle : retenu avec la touche, jusqu'à épuisement ; il revient ensuite
        ResolvedGun aim = GunClientEvents.aiming(player);
        boolean scoped = aim != null && SightEffects.currentMode(aim).zoom() <= 0.26f;
        holdingBreath = scoped && !exhausted && breath > 0 && ModKeyMappings.HOLD_BREATH.isDown();
        if (holdingBreath) {
            breath -= 1;
            if (breath <= 0) {
                exhausted = true;
            }
        } else {
            breath = Math.min(BREATH_MAX, breath + (exhausted ? 0.6f : 1.5f));
            if (exhausted && breath >= BREATH_MAX * 0.6f) {
                exhausted = false;
            }
        }

        // Mitrailleuses brûlantes : le canon fume
        for (Player other : mc.level.players()) {
            ItemStack stack = other.getMainHandItem();
            if (stack.getItem() instanceof GunItem gun && gun.stats().heatPerShot() > 0 && other.distanceToSqr(player) < 48 * 48) {
                float heat = GunItem.heat(stack, mc.level);
                if (heat > 55 && mc.level.random.nextFloat() < (heat - 50) / 60f) {
                    Vec3 p = other.getEyePosition().add(other.getLookAngle().scale(1.3)).add(LeanHandler.right(other).scale(0.3)).add(0, -0.3, 0);
                    mc.level.addParticle(heat > 90 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, p.x, p.y, p.z, 0, 0.03, 0);
                }
            }
        }
    }

    /** Balancement de la lunette, recalculé à chaque image (fluide même à fort grossissement). */
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.isPaused()) {
            return;
        }
        ResolvedGun aim = GunClientEvents.aiming(player);
        boolean scoped = aim != null && SightEffects.currentMode(aim).zoom() <= 0.26f;
        double yaw = 0, pitch = 0;
        if (scoped) {
            float amp = 0.3f;                                    // degrés
            if (ProneHandler.isProne(player)) {
                amp *= 0.4f;
            }
            if (GunItem.isSteady(player) && aim.steadySpread() < 1) {
                amp *= 0.5f;                                     // bipied, monopode...
            }
            if (holdingBreath) {
                amp *= 0.08f;
            } else if (exhausted) {
                amp *= 2.2f;
            }
            double t = (player.tickCount + event.getPartialTick().getGameTimeDeltaPartialTick(false)) / 20.0;
            yaw = amp * (Math.sin(t * 1.1) + 0.4 * Math.sin(t * 3.1));
            pitch = amp * 0.8 * (Math.sin(t * 1.7 + 1.3) + 0.3 * Math.sin(t * 4.3));
        }
        // On applique seulement la différence avec l'image précédente : la souris garde le contrôle
        player.setYRot(player.getYRot() + (float) (yaw - swayYaw));
        player.setXRot(Mth.clamp(player.getXRot() + (float) (pitch - swayPitch), -90f, 90f));
        swayYaw = yaw;
        swayPitch = pitch;
    }

    /** Penché : la caméra s'incline. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getCamera().getEntity() == mc.player && !event.getCamera().isDetached()) {
            event.setRoll(event.getRoll() + lean((float) event.getPartialTick()) * LEAN_ROLL);
        }
    }

    /** Les joueurs penchés sont affichés inclinés (vue à la 3e personne, autres joueurs). */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        float amount = player == Minecraft.getInstance().player ? lean(event.getPartialTick()) : LeanHandler.CLIENT.getOrDefault(player.getId(), 0);
        event.getPoseStack().pushPose();
        if (amount != 0) {
            float bodyYaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
            var pose = event.getPoseStack();
            pose.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            pose.mulPose(Axis.ZP.rotationDegrees(-amount * 15f));
            pose.mulPose(Axis.YP.rotationDegrees(bodyYaw));
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        event.getPoseStack().popPose();
    }

    private AimClient() {}
}

package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.HelmetItem;
import fr.modeminecraft.network.HelmetModulePayload;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/**
 * Casques côté client :
 * - animation d'équipement (1re personne) : les deux bras montent au-dessus du champ de vision et posent le casque ;
 * - touche N : le bras gauche monte sur le côté de la tête et abaisse la visière / les jumelles (1re et 3e personne) ;
 * - contours d'écran des casques fermés (heaume, visière blindée, jumelles quadritubes, visière numérique) ;
 * - jumelles de vision nocturne : filtre vert phosphoré + bruits de pas des monstres proches amplifiés ;
 * - Altyn visière baissée : respiration étouffée.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class HelmetClient {
    private static final int EQUIP_TICKS = 14, MODULE_TICKS = 10;
    private static long equipStart = -1000;
    private static ItemStack equipStack = ItemStack.EMPTY;

    /** Appelé quand le joueur met un casque depuis sa main (clic droit). */
    public static void startEquip(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            equipStart = mc.level.getGameTime();
            equipStack = stack;
        }
    }

    private static ItemStack helmet() {
        var p = Minecraft.getInstance().player;
        return p == null ? ItemStack.EMPTY : p.getItemBySlot(EquipmentSlot.HEAD);
    }

    private static boolean module(HelmetItem.Module m) {
        ItemStack h = helmet();
        return h.getItem() instanceof HelmetItem item && item.stats().module() == m && HelmetItem.isModuleActive(h);
    }

    // ------------------------------------------------------------------ touche N, sons
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        while (ModKeyMappings.HELMET_MODULE.consumeClick()) {
            if (helmet().getItem() instanceof HelmetItem h && h.stats().module() != HelmetItem.Module.NONE) {
                PacketDistributor.sendToServer(HelmetModulePayload.INSTANCE);
            }
        }
        if (player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        // Jumelles : les pas des monstres proches sont amplifiés
        if (module(HelmetItem.Module.NVG) && player.tickCount % 6 == 0) {
            for (Monster m : mc.level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(16))) {
                if (m.onGround() && m.getDeltaMovement().horizontalDistanceSqr() > 1.0e-4) {
                    var state = mc.level.getBlockState(m.getOnPos());
                    var type = state.getSoundType(mc.level, m.getOnPos(), m);
                    mc.level.playLocalSound(m.getX(), m.getY(), m.getZ(), type.getStepSound(), SoundSource.HOSTILE,
                            type.getVolume() * 0.9f, type.getPitch() * 0.9f, false);
                }
            }
        }
        // Visière blindée baissée : respiration étouffée
        if (module(HelmetItem.Module.VISOR) && player.tickCount % 70 == 0) {
            mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), ModSounds.HELMET_BREATH.get(), SoundSource.PLAYERS,
                    0.35f, 0.9f + player.getRandom().nextFloat() * 0.15f, false);
        }
    }

    // ------------------------------------------------------------------ bras en vue à la 1re personne
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        float time = mc.level.getGameTime() + event.getPartialTick();
        float equip = (time - equipStart) / EQUIP_TICKS;
        ItemStack helm = helmet();
        long moduleTime = helm.getOrDefault(ModDataComponents.HELMET_MODULE_TIME.get(), -1000L);
        float mod = (time - moduleTime) / MODULE_TICKS;
        if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        var pose = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        ResourceLocation skin = player.getSkin().texture();
        if (equip >= 0 && equip < 1 && !equipStack.isEmpty()) {
            // Les deux bras montent au-dessus du champ de vision en tenant le casque, puis le posent sur la tête
            float e = equip * equip * (3 - 2 * equip);
            float y = -0.9f + 1.6f * e, z = -0.7f + 0.2f * e;
            drawArm(pose, buffers, light, skin, model, player, true, new Vector3f(0.17f, y, z));
            drawArm(pose, buffers, light, skin, model, player, false, new Vector3f(-0.17f, y, z));
            pose.pushPose();
            pose.translate(0, y + 0.1f, z);
            pose.scale(0.3f, 0.3f, 0.3f);
            mc.getItemRenderer().renderStatic(equipStack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
            pose.popPose();
            event.setCanceled(true);
            return;
        }
        if (mod >= 0 && mod < 1 && helm.getItem() instanceof HelmetItem) {
            // Bras gauche : il monte sur le côté de la tête puis pousse vers le bas (visière / jumelles)
            Vector3f hand = mod < 0.55f
                    ? new Vector3f(-0.5f, -0.9f, -0.42f).lerp(new Vector3f(-0.34f, 0.3f, -0.34f), Mth.sin(mod / 0.55f * Mth.HALF_PI))
                    : new Vector3f(-0.34f, 0.3f, -0.34f).lerp(new Vector3f(-0.24f, -0.02f, -0.4f), (mod - 0.55f) / 0.45f);
            drawArm(pose, buffers, light, skin, model, player, false, hand);
        }
    }

    private static void drawArm(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light,
                                ResourceLocation skin, PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player, boolean right,
                                Vector3f hand) {
        GunPoseHelper.drawArm(pose, buffers, light, skin, right ? model.rightArm : model.leftArm,
                player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE)
                        ? (right ? model.rightSleeve : model.leftSleeve) : null,
                right ? GunPoseHelper.RIGHT_SHOULDER : GunPoseHelper.LEFT_SHOULDER, hand, right);
    }

    // ------------------------------------------------------------------ bras en vue à la 3e personne (mixin du modèle)
    /** Appelé à la fin de HumanoidModel.setupAnim : pose des bras pendant l'équipement et le geste de la visière. */
    public static void poseArms(LivingEntity entity, HumanoidModel<?> model) {
        ItemStack helm = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helm.getItem() instanceof HelmetItem)) {
            return;
        }
        float time = entity.level().getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float equip = (time - helm.getOrDefault(ModDataComponents.HELMET_EQUIP_TIME.get(), -1000L)) / EQUIP_TICKS;
        if (equip >= 0 && equip < 1) {
            float lift = Mth.sin(equip * Mth.PI);
            model.rightArm.xRot = Mth.lerp(lift, model.rightArm.xRot, -2.9f);
            model.leftArm.xRot = Mth.lerp(lift, model.leftArm.xRot, -2.9f);
            model.rightArm.zRot = Mth.lerp(lift, model.rightArm.zRot, 0.35f);
            model.leftArm.zRot = Mth.lerp(lift, model.leftArm.zRot, -0.35f);
            return;
        }
        float mod = (time - helm.getOrDefault(ModDataComponents.HELMET_MODULE_TIME.get(), -1000L)) / MODULE_TICKS;
        if (mod >= 0 && mod < 1) {
            float up = mod < 0.55f ? mod / 0.55f : 1 - (mod - 0.55f) / 0.45f * 0.4f;
            model.leftArm.xRot = Mth.lerp(up, model.leftArm.xRot, -2.7f);
            model.leftArm.zRot = Mth.lerp(up, model.leftArm.zRot, -0.55f);
        }
    }

    // ------------------------------------------------------------------ contours d'écran
    private static final ResourceLocation GREAT_HELM = ModeMinecraft.id("textures/misc/helmet_great_helm.png");
    private static final ResourceLocation VISOR = ModeMinecraft.id("textures/misc/helmet_visor.png");
    private static final ResourceLocation NVG = ModeMinecraft.id("textures/misc/helmet_nvg.png");
    private static final ResourceLocation JUGGERNAUT = ModeMinecraft.id("textures/misc/helmet_juggernaut.png");

    public static void renderOverlay(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        ItemStack helm = helmet();
        if (!(helm.getItem() instanceof HelmetItem)) {
            return;
        }
        int w = g.guiWidth(), h = g.guiHeight();
        ResourceLocation tex = null;
        float since = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false)
                - helm.getOrDefault(ModDataComponents.HELMET_MODULE_TIME.get(), -1000L);
        boolean lowered = since >= MODULE_TICKS * 0.6f;
        if (helm.is(ModItems.GREAT_HELM.get())) {
            tex = GREAT_HELM;
        } else if (helm.is(ModItems.JUGGERNAUT_HELMET.get())) {
            tex = JUGGERNAUT;
        } else if (module(HelmetItem.Module.VISOR) && lowered) {
            tex = VISOR;
        } else if (module(HelmetItem.Module.NVG) && lowered) {
            // Filtre vert phosphoré, avec un léger grain
            g.fill(0, 0, w, h, 0x4A10FF38);
            var r = mc.player.getRandom();
            for (int i = 0; i < 90; i++) {
                int x = r.nextInt(w), y = r.nextInt(h);
                g.fill(x, y, x + 1, y + 1, 0x5530FF60);
            }
            g.flush(); // le voile vert d'abord, puis le contour des tubes par-dessus
            tex = NVG;
        }
        if (tex != null) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            g.blit(tex, 0, 0, 0, 0, 0, w, h, w, h); // même plan que le voile vert (sinon le test de profondeur le cache)
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }

    private HelmetClient() {}
}

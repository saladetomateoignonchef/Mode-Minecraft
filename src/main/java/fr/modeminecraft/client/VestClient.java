package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.VestItem;
import fr.modeminecraft.registry.ModDataComponents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Vector3f;

/**
 * Gilets côté client :
 * - équipement (clic droit) : les deux bras se rejoignent devant le torse et serrent le gilet ;
 * - plaque balistique : le bras gauche la tient devant la poitrine et l'enfonce dans la poche ;
 * - jauge à côté de la barre d'armure : état des plaques avant / arrière (vert intacte, orange fissurée, rouge détruite) ;
 * - Fort Defender : impossible de sprinter longtemps.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class VestClient {
    private static final int EQUIP_TICKS = 14, PLATE_TICKS = 14;
    private static long equipStart = -1000, plateStart = -1000;
    private static ItemStack plateStack = ItemStack.EMPTY;
    private static int sprintTicks, sprintCooldown;

    public static void startEquip() {
        var mc = Minecraft.getInstance();
        if (mc.level != null) {
            equipStart = mc.level.getGameTime();
        }
    }

    public static void startPlateInsert(ItemStack plate) {
        var mc = Minecraft.getInstance();
        if (mc.level != null) {
            plateStart = mc.level.getGameTime();
            plateStack = plate;
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        VestItem v = VestItem.worn(player);
        if (v == null || v.stats().special() != VestItem.Special.FORT_DEFENDER) {
            sprintTicks = sprintCooldown = 0;
            return;
        }
        // Armure lourde : 5 s de sprint au plus, puis 3 s pour reprendre son souffle
        if (sprintCooldown > 0) {
            sprintCooldown--;
            player.setSprinting(false);
        } else if (player.isSprinting() && ++sprintTicks > 100) {
            sprintTicks = 0;
            sprintCooldown = 60;
            player.setSprinting(false);
        } else if (!player.isSprinting()) {
            sprintTicks = Math.max(0, sprintTicks - 2);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null || event.getHand() != InteractionHand.MAIN_HAND
                || !(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        float time = mc.level.getGameTime() + event.getPartialTick();
        float equip = (time - equipStart) / EQUIP_TICKS;
        float plate = (time - plateStart) / PLATE_TICKS;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        var pose = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        ResourceLocation skin = player.getSkin().texture();
        if (equip >= 0 && equip < 1) {
            // Les mains viennent des côtés, se rejoignent devant le torse, puis serrent (aller-retour latéral)
            float join = Math.min(1, equip / 0.5f);
            float squeeze = equip > 0.5f ? Mth.sin((equip - 0.5f) / 0.5f * Mth.PI * 2) * 0.05f : 0;
            float x = Mth.lerp(join, 0.6f, 0.07f) + squeeze;
            drawArm(pose, buffers, light, skin, model, player, true, new Vector3f(x, -0.34f, -0.52f));
            drawArm(pose, buffers, light, skin, model, player, false, new Vector3f(-x, -0.34f, -0.52f));
            event.setCanceled(true);
            return;
        }
        if (plate >= 0 && plate < 1 && !plateStack.isEmpty()) {
            // Le bras gauche tient la plaque devant la poitrine et l'enfonce vers le bas dans la poche
            float y = plate < 0.4f ? Mth.lerp(plate / 0.4f, -0.8f, -0.12f) : Mth.lerp((plate - 0.4f) / 0.6f, -0.12f, -0.6f);
            Vector3f hand = new Vector3f(-0.12f, y, -0.5f);
            drawArm(pose, buffers, light, skin, model, player, false, hand);
            pose.pushPose();
            pose.translate(hand.x + 0.06f, hand.y + 0.12f, hand.z);
            pose.scale(0.35f, 0.35f, 0.35f);
            mc.getItemRenderer().renderStatic(plateStack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
            pose.popPose();
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

    /** Vue à la 3e personne : bras pendant l'équipement et l'insertion d'une plaque (mixin du modèle). */
    public static void poseArms(LivingEntity entity, HumanoidModel<?> model) {
        ItemStack vest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!(vest.getItem() instanceof VestItem)) {
            return;
        }
        float time = entity.level().getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float equip = (time - vest.getOrDefault(ModDataComponents.VEST_EQUIP_TIME.get(), -1000L)) / EQUIP_TICKS;
        if (equip >= 0 && equip < 1) {
            float k = Mth.sin(equip * Mth.PI);
            float squeeze = Mth.sin(equip * Mth.PI * 4) * 0.15f * k;
            model.rightArm.xRot = Mth.lerp(k, model.rightArm.xRot, -1.0f);
            model.leftArm.xRot = Mth.lerp(k, model.leftArm.xRot, -1.0f);
            model.rightArm.yRot = Mth.lerp(k, model.rightArm.yRot, -0.6f) + squeeze;
            model.leftArm.yRot = Mth.lerp(k, model.leftArm.yRot, 0.6f) - squeeze;
            return;
        }
        float plate = (time - vest.getOrDefault(ModDataComponents.PLATE_INSERT_TIME.get(), -1000L)) / PLATE_TICKS;
        if (plate >= 0 && plate < 1) {
            float up = plate < 0.4f ? plate / 0.4f : 1 - (plate - 0.4f) / 0.6f * 0.6f;
            model.leftArm.xRot = Mth.lerp(up, model.leftArm.xRot, -1.5f);
            model.leftArm.yRot = Mth.lerp(up, model.leftArm.yRot, 0.5f);
        }
    }

    // ------------------------------------------------------------------ jauge des plaques
    public static void renderHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || (mc.gameMode != null && !mc.gameMode.canHurtPlayer())) {
            return;
        }
        ItemStack vest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(vest.getItem() instanceof VestItem v) || !v.stats().plates()) {
            return;
        }
        int x = g.guiWidth() / 2 - 91 - 24, y = g.guiHeight() - 50;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.renderItem(vest, 0, 0);
        g.pose().popPose();
        for (int i = 0; i < 2; i++) {
            VestItem.Plate p = VestItem.plate(vest, i == 0);
            int state = p == null ? 0 : p.state();
            int color = state == 2 ? 0xFF3FC86A : state == 1 ? 0xFFE89A2C : 0xFFD0303A;
            int bx = x + 13 + i * 5;
            g.fill(bx - 1, y - 1, bx + 4, y + 13, 0xAA000000);
            float frac = p == null || p.kind() == null ? 0 : Math.max(0, (float) p.durability() / p.kind().durability);
            int h = p == null ? 12 : Math.max(1, Math.round(12 * frac));
            g.fill(bx, y + 12 - h, bx + 3, y + 12, p == null ? 0x66D0303A : color);
        }
    }

    private VestClient() {}
}

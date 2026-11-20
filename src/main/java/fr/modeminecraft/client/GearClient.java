package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.GearItem;
import fr.modeminecraft.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Vector3f;

/** Pantalons et rangers : à l'équipement, les deux bras s'abaissent brièvement sur les côtés (1re et 3e personne). */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class GearClient {
    private static final int EQUIP_TICKS = 12;
    private static long equipStart = -1000;

    public static void startEquip() {
        var mc = Minecraft.getInstance();
        if (mc.level != null) {
            equipStart = mc.level.getGameTime();
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
        float t = (mc.level.getGameTime() + event.getPartialTick() - equipStart) / EQUIP_TICKS;
        if (t < 0 || t >= 1) {
            return;
        }
        // Les mains descendent le long des cuisses puis remontent
        float down = Mth.sin(t * Mth.PI);
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        var skin = player.getSkin().texture();
        for (boolean right : new boolean[]{true, false}) {
            Vector3f hand = new Vector3f(right ? 0.42f : -0.42f, -0.35f - 0.45f * down, -0.45f + 0.1f * down);
            GunPoseHelper.drawArm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), skin,
                    right ? model.rightArm : model.leftArm,
                    player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE) ? (right ? model.rightSleeve : model.leftSleeve) : null,
                    right ? GunPoseHelper.RIGHT_SHOULDER : GunPoseHelper.LEFT_SHOULDER, hand, right);
        }
        event.setCanceled(true);
    }

    /** Vue à la 3e personne (mixin du modèle) : bras écartés vers le bas pendant l'équipement. */
    public static void poseArms(LivingEntity entity, HumanoidModel<?> model) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = entity.getItemBySlot(slot);
            if (!(s.getItem() instanceof GearItem)) {
                continue;
            }
            float time = entity.level().getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            float t = (time - s.getOrDefault(ModDataComponents.GEAR_EQUIP_TIME.get(), -1000L)) / EQUIP_TICKS;
            if (t >= 0 && t < 1) {
                float k = Mth.sin(t * Mth.PI);
                model.rightArm.xRot = Mth.lerp(k, model.rightArm.xRot, 0.35f);
                model.leftArm.xRot = Mth.lerp(k, model.leftArm.xRot, 0.35f);
                model.rightArm.zRot = Mth.lerp(k, model.rightArm.zRot, 0.25f);
                model.leftArm.zRot = Mth.lerp(k, model.leftArm.zRot, -0.25f);
                return;
            }
        }
    }

    private GearClient() {}
}

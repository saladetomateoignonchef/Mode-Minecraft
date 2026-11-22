package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Rendu des bots : modèle de joueur (bipède cubique, bras larges), skin sur mesure de la faction, armures du mod
 * (casques, gilets, pantalons, rangers via les couches communes), arme tenue à deux mains en joue, recul au tir,
 * changement de chargeur, position accroupie derrière un abri et allongée pour les snipers.
 * Faucons Blancs : cape blanche à l'emblème du faucon ; Chef des Troupes de Choc : épée géante dans le dos,
 * dégainée / rengainée avec une animation des deux bras.
 */
public class HumanBotRenderer extends HumanoidMobRenderer<HumanBotEntity, HumanBotRenderer.BotModel> {
    private static final ResourceLocation CAPE = ModeMinecraft.id("textures/entity/bot/falcon_cape.png");

    public HumanBotRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BotModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, HumanBotEntity bot, float a, float b, float c, float d, float e, float f) {
                if (!RenderLod.far(bot)) {
                    super.render(pose, buffers, light, bot, a, b, c, d, e, f);
                }
            }
        });
        // armes tenues (modèles 3D détaillés) : seulement de près
        layers.removeIf(l -> l instanceof net.minecraft.client.renderer.entity.layers.ItemInHandLayer);
        addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this, ctx.getItemInHandRenderer()) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, HumanBotEntity bot, float a, float b, float c, float d, float e, float f) {
                if (!RenderLod.far(bot)) {
                    super.render(pose, buffers, light, bot, a, b, c, d, e, f);
                }
            }
        });
        addLayer(new CapeLayer(this));
        addLayer(new BackSwordLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(HumanBotEntity bot) {
        return bot.skinTexture();
    }

    @Override
    public void render(HumanBotEntity bot, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        BotModel m = getModel();
        ItemStack main = bot.getMainHandItem();
        m.crouching = bot.isCrouching();
        m.rightArmPose = HumanoidModel.ArmPose.ITEM;
        m.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        if (main.isEmpty() || fr.modeminecraft.entity.TurretPost.operating(bot)) {
            m.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        } else if (main.getItem() instanceof GunItem) {
            m.rightArmPose = bot.isAggressive() ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        boolean sitting = bot.activity() == fr.modeminecraft.entity.bot.CivilianLife.SITTING;
        m.riding = sitting;
        pose.pushPose();
        if (sitting) {
            pose.translate(0, -0.55, 0);                                              // assis sur le banc
        }
        super.render(bot, yaw, partial, pose, buffers, light);
        pose.popPose();
        if (bot.alertEnd() > bot.level().getGameTime()) {
            renderAlert(bot, pose, buffers, light);
        }
    }

    /** Icône d'alerte jaune (sommation) au-dessus de la tête. */
    private void renderAlert(HumanBotEntity bot, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, bot.getBbHeight() + 0.85, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float pulse = 1 + 0.12f * Mth.sin((bot.tickCount) * 0.6f);
        pose.scale(0.07f * pulse, -0.07f * pulse, 0.07f * pulse);
        Font font = getFont();
        Matrix4f mat = pose.last().pose();
        String s = " ! ";
        float x = -font.width(s) / 2f;
        font.drawInBatch(s, x, -4, 0xFF1A1A1A, false, mat, buffers, Font.DisplayMode.SEE_THROUGH, 0xFFF0C020, 0xF000F0);
        pose.popPose();
    }

    // ------------------------------------------------------------------ modèle
    public static class BotModel extends PlayerModel<HumanBotEntity> {
        public BotModel(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(HumanBotEntity bot, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
            boolean armed = bot.getMainHandItem().getItem() instanceof GunItem;
            if (armed) {
                // la tête ne se retourne jamais de plus de 80° par rapport au corps : sinon le bras qui porte l'arme
                // suit le regard (yRot de la pose de visée = yRot de la tête) et le fusil finit retourné vers l'arrière
                headYaw = Mth.clamp(headYaw, -80f, 80f);
            }
            super.setupAnim(bot, limbSwing, limbAmount, age, headYaw, headPitch);
            if (armed && bot.getPose() != Pose.SWIMMING) {
                stabilizeGunArms(bot);
            }
            float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            float now = bot.level().getGameTime() + partial;
            boolean gun = bot.getMainHandItem().getItem() instanceof GunItem;
            float pitch = headPitch * Mth.DEG_TO_RAD, yawH = headYaw * Mth.DEG_TO_RAD;
            if (bot.getPose() == Pose.SWIMMING) {
                // allongé (sniper) : les deux bras vers l'avant, l'arme posée devant, jambes tendues
                rightArm.xRot = -Mth.PI + 0.15f;
                leftArm.xRot = -Mth.PI + 0.25f;
                rightArm.yRot = -0.05f;
                leftArm.yRot = 0.45f;
                rightArm.zRot = leftArm.zRot = 0;
                rightLeg.xRot = leftLeg.xRot = 0;
                rightLeg.zRot = 0.05f;
                leftLeg.zRot = -0.05f;
            } else if (gun) {
                float kick = now - bot.shotAt();
                if (kick >= 0 && kick < 3) {
                    float k = 0.22f * (1 - kick / 3);   // recul
                    rightArm.xRot -= k;
                    leftArm.xRot -= k;
                }
                if (bot.reloadEnd() > now) {
                    // changement de chargeur : la main gauche descend chercher un chargeur neuf puis remonte
                    float bob = Mth.sin(now * 0.5f) * 0.1f;
                    leftArm.xRot = -0.35f + bob;
                    leftArm.yRot = 0.35f;
                    rightArm.xRot = -0.9f + pitch * 0.5f;
                }
            }
            if (bot.role() == BotRole.CHIEF) {
                sword(bot, now);
            }
            byte act = bot.activity();
            if (act == fr.modeminecraft.entity.bot.CivilianLife.HANDS_UP) {
                // mains en l'air (tenu en joue / braquage), légèrement tremblantes
                float shake = Mth.sin(now * 1.7f) * 0.04f;
                rightArm.xRot = -Mth.PI + 0.25f + shake;
                leftArm.xRot = -Mth.PI + 0.25f - shake;
                rightArm.zRot = 0.35f;
                leftArm.zRot = -0.35f;
                rightArm.yRot = leftArm.yRot = 0;
            } else if (act == fr.modeminecraft.entity.bot.CivilianLife.CHATTING) {
                rightArm.xRot = -0.45f + Mth.sin(now * 0.25f) * 0.25f;                 // gestes de la main en parlant
                rightArm.zRot = -0.1f;
            } else if (act == fr.modeminecraft.entity.bot.CivilianLife.READING) {
                rightArm.xRot = leftArm.xRot = -1.1f;                                   // journal tenu devant soi
                rightArm.yRot = -0.35f;
                leftArm.yRot = 0.35f;
                head.xRot = 0.45f;
            }
            // capes et gilets suivent le corps ; vestes / manches de la couche externe aussi
            jacket.copyFrom(body);
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
            rightPants.copyFrom(rightLeg);
            leftPants.copyFrom(leftLeg);
            hat.copyFrom(head);
        }

        /**
         * Orientation déterministe de l'arme : le bras droit (qui porte l'arme) ne roule jamais sur lui-même (zRot = 0)
         * et reste dans un cône avant fixe, en visée comme en veille, quelles que soient les animations de marche,
         * de tir ou de balancement qui viennent de s'appliquer.
         */
        private void stabilizeGunArms(HumanBotEntity bot) {
            boolean aiming = bot.isAggressive();
            float xMin = aiming ? -2.0f : -1.6f, xMax = aiming ? -0.9f : 0.3f;
            rightArm.xRot = Mth.clamp(rightArm.xRot, xMin, xMax);
            rightArm.yRot = Mth.clamp(rightArm.yRot, -0.9f, 0.5f);
            rightArm.zRot = 0;
            leftArm.yRot = Mth.clamp(leftArm.yRot, -0.5f, 1.0f);
            leftArm.zRot = Mth.clamp(leftArm.zRot, -0.3f, 0.3f);
        }

        /** Dégainer (14 ticks), tenir à deux mains, rengainer (16 ticks). */
        private void sword(HumanBotEntity bot, float now) {
            float t = now - bot.swordAt();
            switch (bot.swordState()) {
                case HumanBotEntity.SWORD_DRAWING -> {
                    float h = HumanBotEntity.DRAW_TICKS / 2f;
                    if (t < h) {
                        float k = smooth(t / h);                        // la main droite passe par-dessus l'épaule
                        rightArm.xRot = Mth.lerp(k, 0, -2.9f);
                        rightArm.zRot = Mth.lerp(k, 0, -0.35f);
                        rightArm.yRot = 0;
                    } else {
                        float k = smooth(Math.min(1, (t - h) / h));     // la lame sort et vient devant, la main gauche la saisit
                        rightArm.xRot = Mth.lerp(k, -2.9f, -0.95f);
                        rightArm.zRot = Mth.lerp(k, -0.35f, 0);
                        leftArm.xRot = Mth.lerp(k, 0, -0.95f);
                        leftArm.yRot = Mth.lerp(k, 0, 0.6f);
                    }
                }
                case HumanBotEntity.SWORD_DRAWN -> {
                    if (attackTime <= 0) {
                        rightArm.xRot = -0.95f;
                        rightArm.yRot = -0.15f;
                        leftArm.xRot = -0.95f;
                        leftArm.yRot = 0.6f;
                    }
                }
                case HumanBotEntity.SWORD_SHEATHING -> {
                    float h = HumanBotEntity.SHEATHE_TICKS / 2f;
                    if (t < h) {
                        float k = smooth(t / h);
                        rightArm.xRot = Mth.lerp(k, -0.95f, -2.9f);
                        rightArm.zRot = Mth.lerp(k, 0, -0.35f);
                        leftArm.xRot = Mth.lerp(k, -0.95f, 0);
                        leftArm.yRot = Mth.lerp(k, 0.6f, 0);
                    } else {
                        float k = smooth(Math.min(1, (t - h) / h));
                        rightArm.xRot = Mth.lerp(k, -2.9f, 0);
                        rightArm.zRot = Mth.lerp(k, -0.35f, 0);
                    }
                }
                default -> {
                }
            }
        }

        private static float smooth(float t) {
            t = Mth.clamp(t, 0, 1);
            return t * t * (3 - 2 * t);
        }
    }

    // ------------------------------------------------------------------ cape des Faucons Blancs
    static class CapeLayer extends RenderLayer<HumanBotEntity, BotModel> {
        CapeLayer(RenderLayerParent<HumanBotEntity, BotModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, HumanBotEntity bot, float limbSwing, float limbAmount,
                           float partial, float age, float headYaw, float headPitch) {
            if (RenderLod.far(bot)) {
                return;
            }
            if (bot.faction() != Faction.FALCON || bot.isInvisible() || bot.getPose() == Pose.SWIMMING) {
                return;
            }
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            pose.translate(0, 0, 0.125 + (bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty() ? 0 : 0.09));
            // la cape flotte avec la marche et le vent
            float sway = 6 + limbAmount * 55 + Mth.sin(age * 0.08f) * 3;
            pose.mulPose(Axis.XP.rotationDegrees(Math.min(70, sway) + (bot.isCrouching() ? 25 : 0)));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.05f) * 2));
            pose.mulPose(Axis.YP.rotationDegrees(180));
            getParentModel().renderCloak(pose, buffers.getBuffer(RenderType.entitySolid(CAPE)), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------ épée dans le dos du Chef
    static class BackSwordLayer extends RenderLayer<HumanBotEntity, BotModel> {
        private ItemStack sword;

        BackSwordLayer(RenderLayerParent<HumanBotEntity, BotModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, HumanBotEntity bot, float limbSwing, float limbAmount,
                           float partial, float age, float headYaw, float headPitch) {
            if (RenderLod.far(bot)) {
                return;
            }
            if (bot.role() != BotRole.CHIEF || !bot.getMainHandItem().isEmpty()) {
                return;
            }
            if (sword == null) {
                sword = new ItemStack(ModItems.DRAGONSLAYER.get());
            }
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            // en diagonale : de l'épaule droite (poignée qui dépasse) à la hanche gauche
            // (repère du corps : y vers le bas, +x = côté gauche) ; le plat de la lame tourné vers l'arrière
            pose.translate(0, 0.23, 0.3);
            pose.mulPose(Axis.ZP.rotationDegrees(-30));
            Minecraft.getInstance().getItemRenderer().renderStatic(bot, sword, ItemDisplayContext.FIXED, false, pose, buffers, bot.level(),
                    light, OverlayTexture.NO_OVERLAY, bot.getId());
            pose.popPose();
        }
    }
}

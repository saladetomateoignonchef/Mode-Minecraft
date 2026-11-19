package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.PlacedExplosiveEntity;
import fr.modeminecraft.entity.ThrownGrenadeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Rendu des grenades (qui tournent en vol) et des explosifs posés (orientés). */
public final class ExplosiveRenderers {
    private static final ResourceLocation TEX = ModeMinecraft.id("textures/item/gun_palette_default.png");

    public static class Grenade extends EntityRenderer<ThrownGrenadeEntity> {
        public Grenade(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public void render(ThrownGrenadeEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            pose.pushPose();
            pose.translate(0, 0.12, 0);
            if (!e.isStuckOrResting()) {
                float age = e.tickCount + partial;
                pose.mulPose(Axis.YP.rotationDegrees(age * 23));
                pose.mulPose(Axis.XP.rotationDegrees(age * 31)); // rotation continue en vol
            }
            pose.scale(0.8f, 0.8f, 0.8f);
            Minecraft.getInstance().getItemRenderer().renderStatic(e.getItem(), ItemDisplayContext.GROUND, light,
                    OverlayTexture.NO_OVERLAY, pose, buffers, e.level(), e.getId());
            pose.popPose();
            super.render(e, yaw, partial, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(ThrownGrenadeEntity e) {
            return TEX;
        }
    }

    public static class Placed extends EntityRenderer<PlacedExplosiveEntity> {
        public Placed(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public void render(PlacedExplosiveEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot() + 180));
            if (e.kind() == PlacedExplosiveEntity.Kind.MORTAR) {
                pose.translate(0, 0.5, 0);
            } else {
                pose.translate(0, 0.25, 0);
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(e.item()), ItemDisplayContext.FIXED, light,
                    OverlayTexture.NO_OVERLAY, pose, buffers, e.level(), e.getId());
            pose.popPose();
            super.render(e, yaw, partial, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(PlacedExplosiveEntity e) {
            return TEX;
        }
    }

    /** Nuages (fumée, gaz, feu) : seulement des particules. */
    public static class Invisible<T extends Entity> extends EntityRenderer<T> {
        public Invisible(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return TEX;
        }
    }

    private ExplosiveRenderers() {}
}

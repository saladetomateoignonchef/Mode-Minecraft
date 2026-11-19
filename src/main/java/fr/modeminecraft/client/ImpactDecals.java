package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.modeminecraft.ModeMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;

/** Trous de balle sur les blocs : ils restent 30 secondes puis s'effacent (256 au maximum). */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class ImpactDecals {
    private static final ResourceLocation TEXTURE = ModeMinecraft.id("textures/misc/bullet_hole.png");
    private static final int LIFETIME = 600, FADE = 60, MAX = 256;
    private static final float SIZE = 0.1f;

    private record Decal(Vec3 pos, Direction face, float angle, long born) {}

    private static final Deque<Decal> DECALS = new ArrayDeque<>();

    public static void add(Vec3 hit, Direction face) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (DECALS.size() >= MAX) {
            DECALS.pollFirst();
        }
        DECALS.addLast(new Decal(hit, face, level.random.nextFloat() * 6.283f, level.getGameTime()));
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        DECALS.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || DECALS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        DECALS.removeIf(d -> now - d.born > LIFETIME);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucent(TEXTURE);
        VertexConsumer vc = buffers.getBuffer(type);
        for (Decal d : DECALS) {
            if (d.pos.distanceToSqr(cam) > 64 * 64) {
                continue;
            }
            long age = now - d.born;
            int alpha = age > LIFETIME - FADE ? (int) (255 * (LIFETIME - age) / (float) FADE) : 255;
            Vector3f n = d.face.step();
            // Deux axes dans le plan de la face, tournés d'un angle aléatoire
            Vector3f u = Math.abs(n.y()) > 0.5f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            Vector3f v = new Vector3f(n).cross(u).normalize();
            u = new Vector3f(v).cross(n).normalize();
            float c = (float) Math.cos(d.angle) * SIZE, s = (float) Math.sin(d.angle) * SIZE;
            Vector3f a = new Vector3f(u).mul(c).add(new Vector3f(v).mul(s));
            Vector3f b = new Vector3f(v).mul(c).sub(new Vector3f(u).mul(s));
            Vec3 p = d.pos.add(n.x() * 0.003, n.y() * 0.003, n.z() * 0.003);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(p.add(n.x() * 0.1, n.y() * 0.1, n.z() * 0.1)));
            float[][] corners = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
            for (float[] k : corners) {
                float x = (float) p.x + a.x() * k[0] + b.x() * k[1];
                float y = (float) p.y + a.y() * k[0] + b.y() * k[1];
                float z = (float) p.z + a.z() * k[0] + b.z() * k[1];
                vc.addVertex(m, x, y, z).setColor(255, 255, 255, alpha).setUv(k[2], k[3])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), n.x(), n.y(), n.z());
            }
        }
        buffers.endBatch(type);
        pose.popPose();
    }

    private ImpactDecals() {}
}

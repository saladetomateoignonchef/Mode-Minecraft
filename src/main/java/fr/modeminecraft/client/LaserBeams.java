package fr.modeminecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.AttachmentSlot;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.LeanHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Faisceau laser continu : un trait lumineux de la lentille du laser jusqu'à ce qu'il touche.
 * - Vue à la 1re personne : dessiné avec l'arme (il part vraiment de la lentille du laser monté).
 * - Autres joueurs / vue à la 3e personne : dessiné dans le monde depuis l'arme du joueur.
 * Le point lumineux sur la cible est une particule (voir SightEffects).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class LaserBeams {
    /** Position de la lentille dans le modèle de chaque laser (pixels, relatif au point de fixation). */
    private static final Map<String, Vector3f> LENS = Map.of(
            "laser_grip", new Vector3f(2.45f, -0.85f, 0),
            "light_laser_grip", new Vector3f(2.45f, -2.0f, 0),
            "peq15", new Vector3f(1.85f, 0.45f, 0),
            "lasergrip", new Vector3f(1.25f, -0.55f, 0));
    private static final Vector3f SIDE_LENS = new Vector3f(1.95f, 0, -0.62f);

    /** Point de départ du faisceau dans le modèle de l'arme (blocs), ou null si pas de laser allumé. */
    public static Vector3f lensPoint(String gun, ItemStack stack) {
        if (GunItem.activeLaserColor(stack) == null) {
            return null;
        }
        for (var entry : GunItem.getAttachments(stack).parts().entrySet()) {
            if (entry.getValue() instanceof AttachmentItem a && a.stats().specials().contains(AttachmentItem.Special.LASER)) {
                Vector3f mount = fr.modeminecraft.client.model.GunMounts.get(gun, entry.getKey());
                if (mount == null) {
                    return null;
                }
                String id = BuiltInRegistries.ITEM.getKey(a).getPath();
                Vector3f lens = id.equals("an_peq15") ? new Vector3f(2.3f, 0.45f, 2.05f)
                        : entry.getKey() == AttachmentSlot.SIDE_RAIL ? SIDE_LENS : LENS.getOrDefault(id, new Vector3f(2, 0, 0));
                return new Vector3f(lens).div(16f).add(mount);
            }
        }
        return null;
    }

    /** Couleur actuelle du laser (l'arc-en-ciel change en continu). */
    public static int rgb(int color, float time) {
        return color == -1 ? Mth.hsvToRgb((time % 80) / 80f, 1f, 1f) : color;
    }

    /**
     * Vue à la 1re personne : appelé pendant l'affichage de l'arme, avec la position de la lentille
     * dans l'espace de dessin de la main.
     */
    public static void drawFirstPerson(Player player, ItemStack stack, Vector3f lensVertex) {
        Integer color = GunItem.activeLaserColor(stack);
        if (color == null || player.hasEffect(fr.modeminecraft.registry.ModEffects.EMP)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f invView = new Matrix4f(view).invert();
        Vector3f start = view.transformPosition(new Vector3f(lensVertex));    // espace "œil"
        HitResult hit = SightEffects.pickFor(player, SightEffects.LASER_RANGE);
        double d = hit.getType() == HitResult.Type.MISS ? SightEffects.LASER_RANGE : hit.getLocation().distanceTo(player.getEyePosition());
        Vector3f end = new Vector3f(0, 0, (float) -d);                        // le centre de l'écran, à distance d
        VertexConsumer vc = mc.renderBuffers().bufferSource().getBuffer(RenderType.lightning());
        int rgb = rgb(color, mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false));
        ribbon(vc, invView, start, end, new Vector3f(), rgb);
    }

    /** Autres joueurs (et soi-même en vue à la 3e personne) : faisceau dans le monde. */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = null;
        for (Player p : mc.level.players()) {
            if (p == mc.player && mc.options.getCameraType().isFirstPerson() || p.distanceToSqr(cam) > 96 * 96) {
                continue;
            }
            ItemStack stack = p.getMainHandItem();
            Integer color = stack.getItem() instanceof GunItem && !p.hasEffect(fr.modeminecraft.registry.ModEffects.EMP)
                    ? GunItem.activeLaserColor(stack) : null;
            if (color == null) {
                continue;
            }
            if (vc == null) {
                vc = buffers.getBuffer(RenderType.lightning());
            }
            Vec3 eye = p.getEyePosition(partial);
            Vec3 look = p.getViewVector(partial);
            Vec3 right = LeanHandler.right(p);
            Vec3 up = right.cross(look).normalize();
            Vec3 from = eye.add(look.scale(0.85)).add(right.scale(0.24)).add(up.scale(-0.3));
            HitResult hit = SightEffects.pickFor(p, SightEffects.LASER_RANGE);
            Vec3 to = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(SightEffects.LASER_RANGE)) : hit.getLocation();
            Matrix4f m = new Matrix4f(pose.last().pose());
            Vector3f a = new Vector3f((float) (from.x - cam.x), (float) (from.y - cam.y), (float) (from.z - cam.z));
            Vector3f b = new Vector3f((float) (to.x - cam.x), (float) (to.y - cam.y), (float) (to.z - cam.z));
            ribbon(vc, m, a, b, new Vector3f(), rgb(color, mc.level.getGameTime() + partial));
        }
        if (vc != null) {
            buffers.endBatch(RenderType.lightning());
        }
    }

    /**
     * Trait lumineux de a à b (coordonnées relatives à l'œil `eye`), toujours tourné vers l'œil,
     * plus large au loin pour rester visible : un cœur vif + un halo doux.
     */
    private static void ribbon(VertexConsumer vc, Matrix4f toVertex, Vector3f a, Vector3f b, Vector3f eye, int rgb) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, bl = rgb & 255;
        int steps = 24;
        Vector3f dir = new Vector3f(b).sub(a).normalize();
        for (int layer = 0; layer < 2; layer++) {
            float widthMul = layer == 0 ? 1f : 3.2f;
            int alpha = layer == 0 ? 220 : 55;
            for (int i = 0; i < steps; i++) {
                Vector3f p0 = new Vector3f(a).lerp(b, i / (float) steps);
                Vector3f p1 = new Vector3f(a).lerp(b, (i + 1) / (float) steps);
                Vector3f s0 = side(dir, p0, eye, widthMul), s1 = side(dir, p1, eye, widthMul);
                vertex(vc, toVertex, new Vector3f(p0).add(s0), r, g, bl, alpha);
                vertex(vc, toVertex, new Vector3f(p0).sub(s0), r, g, bl, alpha);
                vertex(vc, toVertex, new Vector3f(p1).sub(s1), r, g, bl, alpha);
                vertex(vc, toVertex, new Vector3f(p1).add(s1), r, g, bl, alpha);
            }
        }
    }

    /** Demi-largeur du trait au point p, perpendiculaire au trait et à la direction du regard. */
    private static Vector3f side(Vector3f dir, Vector3f p, Vector3f eye, float mul) {
        Vector3f toEye = new Vector3f(p).sub(eye);
        float dist = toEye.length();
        Vector3f s = new Vector3f(dir).cross(toEye);
        if (s.lengthSquared() < 1e-10f) {
            s.set(0, 1, 0);
        }
        return s.normalize().mul(Math.max(0.0012f, dist * 0.0011f) * mul);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vector3f p, int r, int g, int b, int a) {
        vc.addVertex(m, p.x, p.y, p.z).setColor(r, g, b, a);
    }

    private LaserBeams() {}
}

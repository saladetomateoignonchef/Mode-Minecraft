package fr.modeminecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.client.model.GunMounts;
import fr.modeminecraft.item.HelmetItem;
import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.AttachmentSlot;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.LeanHandler;
import fr.modeminecraft.item.gun.LightMode;
import fr.modeminecraft.network.FlashlightPayload;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Lampes tactiques, côté client :
 * - touche L (allumer / éteindre) et Maj + L (mode suivant) ; sur L, l'écran des progrès de Minecraft ne s'ouvre pas
 *   tant qu'on tient une arme ;
 * - faisceau volumétrique : un cône lumineux additif (plusieurs plans qui se croisent sur l'axe, dégradé vers le bout)
 *   qui part de la lentille et suit le canon, plus une tache de lumière sur la surface éclairée et quelques grains de
 *   poussière dans le faisceau. Tous les joueurs voient les faisceaux des autres ;
 * - stroboscope : le faisceau clignote 10 fois par seconde ; infrarouge : visible seulement avec des jumelles de vision
 *   nocturne baissées ;
 * - halo blanc pulsant quand on regarde une lampe en stroboscope ; jauge de pile sous l'icône de l'arme ;
 * - animation : le bras gauche va presser l'interrupteur.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class FlashlightClient {
    /** Position de la lentille de chaque lampe (pixels, relatif au point de fixation du rail latéral). */
    private static final Map<String, Vector3f> LENS = Map.of(
            "duct_tape_flashlight", new Vector3f(2.75f, 0, 2.35f),
            "surefire_m600", new Vector3f(2.6f, 0, 2.35f),
            "streamlight_tlr1", new Vector3f(2.6f, -0.12f, 2.3f),
            "an_peq15", new Vector3f(2.12f, -0.35f, 2.0f),
            "mawl_c1", new Vector3f(2.7f, 0, 2.3f));
    private static final int IR_COLOR = 0xA8FFB0;
    private static final int PRESS_TICKS = 8;

    // ------------------------------------------------------------------ touches
    @SubscribeEvent
    public static void onTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        int presses = 0;
        while (ModKeyMappings.FLASHLIGHT.consumeClick()) {
            presses++;
        }
        if (presses == 0 || mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GunItem)) {
            return;
        }
        PacketDistributor.sendToServer(new FlashlightPayload(Screen.hasShiftDown()));
        if (ModKeyMappings.FLASHLIGHT.same(mc.options.keyAdvancements)) {
            while (mc.options.keyAdvancements.consumeClick()) {
                // L sert à la lampe quand on tient une arme : pas d'écran des progrès
            }
        }
    }

    /** Grains de poussière éclairés dans les faisceaux visibles. */
    @SubscribeEvent
    public static void onTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        for (Player p : mc.level.players()) {
            ItemStack stack = p.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem) || p.distanceToSqr(mc.player) > 48 * 48) {
                continue;
            }
            LightMode mode = GunItem.activeLightMode(stack);
            if (!mode.visible() || p.hasEffect(ModEffects.EMP) || p.getRandom().nextFloat() > 0.6f) {
                continue;
            }
            AttachmentItem.Light l = GunItem.light(stack);
            Vec3 eye = p.getEyePosition();
            Vec3 look = p.getLookAngle();
            double d = 1.5 + p.getRandom().nextDouble() * Math.min(12, l.range() * 0.5);
            double r = d * Math.tan(Math.toRadians(l.cone())) * p.getRandom().nextDouble();
            Vec3 right = LeanHandler.right(p);
            Vec3 up = right.cross(look).normalize();
            double a = p.getRandom().nextDouble() * Math.PI * 2;
            Vec3 pos = eye.add(look.scale(d)).add(right.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r));
            if (!mc.level.getBlockState(net.minecraft.core.BlockPos.containing(pos)).isAir()) {
                continue;
            }
            int c = l.color();
            mc.level.addParticle(new DustParticleOptions(new Vector3f((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f), 0.35f),
                    pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ visibilité
    /** Jumelles de vision nocturne baissées sur le casque du joueur local ? */
    public static boolean localNvg() {
        Player p = Minecraft.getInstance().player;
        if (p == null) {
            return false;
        }
        ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
        return head.getItem() instanceof HelmetItem h && h.stats().module() == HelmetItem.Module.NVG && HelmetItem.isModuleActive(head);
    }

    /** Le faisceau de cette arme est-il visible pour nous à cet instant ? (clignotement, infrarouge, IEM) */
    private static boolean shows(Player owner, ItemStack stack, float time) {
        LightMode m = GunItem.activeLightMode(stack);
        if (m == LightMode.OFF || owner.hasEffect(ModEffects.EMP)) {
            return false;
        }
        if (m == LightMode.STROBE && ((int) time) % 2 == 1) {
            return false;                                    // 10 flashs par seconde
        }
        return m != LightMode.IR || localNvg();
    }

    private static int color(AttachmentItem.Light l, ItemStack stack) {
        return GunItem.activeLightMode(stack) == LightMode.IR ? IR_COLOR : l.color();
    }

    private static float flicker(AttachmentItem.Light l, float time) {
        if (!l.flicker()) {
            return 1;
        }
        float n = Mth.sin(time * 1.7f) * Mth.sin(time * 0.53f + 1.3f);
        return 0.82f + 0.18f * n;
    }

    // ------------------------------------------------------------------ 1re personne
    /** Point de départ du faisceau dans le modèle de l'arme (blocs), ou null si pas de lampe. */
    public static Vector3f lensPoint(String gun, ItemStack stack) {
        AttachmentItem a = GunItem.lightItem(stack);
        if (a == null || GunItem.activeLightMode(stack) == LightMode.OFF) {
            return null;
        }
        Vector3f mount = GunMounts.get(gun, AttachmentSlot.SIDE_RAIL);
        if (mount == null) {
            return null;
        }
        String id = BuiltInRegistries.ITEM.getKey(a).getPath();
        return new Vector3f(LENS.getOrDefault(id, new Vector3f(2.6f, 0, 2.35f))).div(16f).add(mount);
    }

    /** Vue à la 1re personne : le cône part vraiment de la lentille de la lampe et va vers le point visé. */
    public static void drawFirstPerson(Player player, ItemStack stack, Vector3f lensVertex) {
        Minecraft mc = Minecraft.getInstance();
        float time = mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        if (!shows(player, stack, time)) {
            return;
        }
        AttachmentItem.Light l = GunItem.light(stack);
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f invView = new Matrix4f(view).invert();
        Vector3f start = view.transformPosition(new Vector3f(lensVertex));
        HitResult hit = SightEffects.pickFor(player, l.range());
        double d = hit.getType() == HitResult.Type.MISS ? l.range() : hit.getLocation().distanceTo(player.getEyePosition());
        Vector3f end = new Vector3f(0, 0, (float) -d);
        VertexConsumer vc = mc.renderBuffers().bufferSource().getBuffer(RenderType.lightning());
        cone(vc, invView, start, end, new Vector3f(), l.cone(), color(l, stack), 0.55f * flicker(l, time));
    }

    // ------------------------------------------------------------------ dans le monde
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
        float time = mc.level.getGameTime() + partial;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = null;
        for (Player p : mc.level.players()) {
            ItemStack stack = p.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem) || p.distanceToSqr(cam) > 128 * 128) {
                continue;
            }
            AttachmentItem.Light l = GunItem.light(stack);
            if (l == null || !shows(p, stack, time)) {
                continue;
            }
            if (vc == null) {
                vc = buffers.getBuffer(RenderType.lightning());
            }
            int rgb = color(l, stack);
            float fl = flicker(l, time);
            Vec3 eye = p.getEyePosition(partial);
            Vec3 look = p.getViewVector(partial);
            HitResult hit = SightEffects.pickFor(p, l.range());
            double dist = hit.getType() == HitResult.Type.MISS ? l.range() : hit.getLocation().distanceTo(eye);
            Matrix4f m = new Matrix4f(pose.last().pose());
            boolean firstPerson = p == mc.player && mc.options.getCameraType().isFirstPerson();
            if (!firstPerson) {
                Vec3 right = LeanHandler.right(p);
                Vec3 up = right.cross(look).normalize();
                Vec3 from = eye.add(look.scale(0.85)).add(right.scale(0.3)).add(up.scale(-0.28));
                Vec3 to = eye.add(look.scale(dist));
                cone(vc, m, rel(from, cam), rel(to, cam), new Vector3f(), l.cone(), rgb, 0.7f * fl);
            }
            if (hit.getType() != HitResult.Type.MISS) {
                float strength = (float) (1 - 0.6 * dist / l.range()) * fl;
                float radius = (float) (dist * Math.tan(Math.toRadians(l.cone())) + 0.25);
                Vec3 normal = hit instanceof BlockHitResult bh ? Vec3.atLowerCornerOf(bh.getDirection().getNormal()) : cam.subtract(hit.getLocation()).normalize();
                spot(vc, m, rel(hit.getLocation().add(normal.scale(0.02)), cam), normal.toVector3f(), radius, rgb, strength);
            }
        }
        if (vc != null) {
            buffers.endBatch(RenderType.lightning());
        }
    }

    private static Vector3f rel(Vec3 v, Vec3 cam) {
        return new Vector3f((float) (v.x - cam.x), (float) (v.y - cam.y), (float) (v.z - cam.z));
    }

    /**
     * Cône de lumière de a vers b : trois plans contenant l'axe (l'un tourné vers l'œil), chacun dégradé du centre
     * (lumineux) vers le bord (transparent) et de la lentille vers le bout. Mélange additif : effet volumétrique doux.
     */
    private static void cone(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, Vector3f eye, float halfAngle, int rgb, float alpha) {
        Vector3f axis = new Vector3f(b).sub(a);
        float len = axis.length();
        if (len < 0.1f) {
            return;
        }
        Vector3f dir = new Vector3f(axis).div(len);
        Vector3f toEye = new Vector3f(eye).sub(a);
        Vector3f s0 = new Vector3f(dir).cross(toEye);
        if (s0.lengthSquared() < 1e-8f) {
            s0 = new Vector3f(dir).cross(new Vector3f(0, 1, 0));
            if (s0.lengthSquared() < 1e-8f) {
                s0.set(1, 0, 0);
            }
        }
        s0.normalize();
        Vector3f u0 = new Vector3f(dir).cross(s0).normalize();
        float tan = (float) Math.tan(Math.toRadians(halfAngle));
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, bl = rgb & 255;
        int segments = 10;
        for (int k = 0; k < 3; k++) {
            double ang = Math.toRadians(k * 60);
            Vector3f side = new Vector3f(s0).mul((float) Math.cos(ang)).add(new Vector3f(u0).mul((float) Math.sin(ang)));
            float planeAlpha = alpha * (k == 0 ? 1f : 0.6f);
            for (int i = 0; i < segments; i++) {
                float t0 = i / (float) segments, t1 = (i + 1) / (float) segments;
                Vector3f p0 = new Vector3f(a).lerp(b, t0), p1 = new Vector3f(a).lerp(b, t1);
                float w0 = 0.03f + t0 * len * tan, w1 = 0.03f + t1 * len * tan;
                int a0 = (int) (90 * planeAlpha * fade(t0)), a1 = (int) (90 * planeAlpha * fade(t1));
                for (int sgn = -1; sgn <= 1; sgn += 2) {
                    Vector3f e0 = new Vector3f(side).mul(w0 * sgn).add(p0), e1 = new Vector3f(side).mul(w1 * sgn).add(p1);
                    vertex(vc, m, p0, r, g, bl, a0);
                    vertex(vc, m, e0, r, g, bl, 0);
                    vertex(vc, m, e1, r, g, bl, 0);
                    vertex(vc, m, p1, r, g, bl, a1);
                }
            }
        }
    }

    /** Intensité le long du faisceau : forte près de la lentille, s'éteint doucement au bout. */
    private static float fade(float t) {
        return (1 - t) * (1 - t) * 0.85f + 0.15f * (1 - t);
    }

    /** Tache de lumière sur la surface éclairée (disque dégradé, posé à plat sur la surface). */
    private static void spot(VertexConsumer vc, Matrix4f m, Vector3f c, Vector3f normal, float radius, int rgb, float strength) {
        Vector3f t1 = Math.abs(normal.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(normal).cross(t1).normalize();
        Vector3f v = new Vector3f(normal).cross(u).normalize();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        int center = (int) (110 * Mth.clamp(strength, 0, 1));
        int n = 16;
        for (int ring = 0; ring < 2; ring++) {
            float r0 = ring == 0 ? 0 : radius * 0.55f, r1 = ring == 0 ? radius * 0.55f : radius;
            int a0 = ring == 0 ? center : center * 2 / 3, a1 = ring == 0 ? center * 2 / 3 : 0;
            for (int i = 0; i < n; i++) {
                double q0 = Math.PI * 2 * i / n, q1 = Math.PI * 2 * (i + 1) / n;
                Vector3f d0 = new Vector3f(u).mul((float) Math.cos(q0)).add(new Vector3f(v).mul((float) Math.sin(q0)));
                Vector3f d1 = new Vector3f(u).mul((float) Math.cos(q1)).add(new Vector3f(v).mul((float) Math.sin(q1)));
                vertex(vc, m, new Vector3f(d0).mul(r0).add(c), r, g, b, a0);
                vertex(vc, m, new Vector3f(d0).mul(r1).add(c), r, g, b, a1);
                vertex(vc, m, new Vector3f(d1).mul(r1).add(c), r, g, b, a1);
                vertex(vc, m, new Vector3f(d1).mul(r0).add(c), r, g, b, a0);
            }
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vector3f p, int r, int g, int b, int a) {
        vc.addVertex(m, p.x, p.y, p.z).setColor(r, g, b, Mth.clamp(a, 0, 255));
    }

    // ------------------------------------------------------------------ animation de l'interrupteur
    /** 0 -> 1 -> 0 pendant l'appui sur l'interrupteur (main gauche). */
    public static float pressAmount(ItemStack stack, float now) {
        Long at = stack.get(ModDataComponents.LIGHT_SWITCH_AT.get());
        if (at == null) {
            return 0;
        }
        float t = now - at;
        return t < 0 || t > PRESS_TICKS ? 0 : Mth.sin((float) Math.PI * t / PRESS_TICKS);
    }

    /** Vu de l'extérieur : le bras gauche pivote vers le flanc du canon pour presser l'interrupteur. */
    public static void poseArms(LivingEntity entity, HumanoidModel<?> model) {
        if (!(entity instanceof Player p) || !(p.getMainHandItem().getItem() instanceof GunItem)) {
            return;
        }
        float k = pressAmount(p.getMainHandItem(), p.level().getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (k > 0) {
            model.leftArm.yRot -= 0.45f * k;
            model.leftArm.xRot -= 0.15f * k;
        }
    }

    // ------------------------------------------------------------------ éblouissement (stroboscope)
    private static long dazzleStart = -1000;
    private static float dazzleStrength;

    public static void dazzle(float strength) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        dazzleStart = mc.level.getGameTime();
        dazzleStrength = Math.max(strength, mc.level.getGameTime() - dazzleStart < 6 ? dazzleStrength : 0);
    }

    /** Halo blanc pulsant (au rythme du stroboscope), qui s'efface en un peu plus d'une demi-seconde. */
    public static void renderDazzle(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float t = mc.level.getGameTime() - dazzleStart + delta.getGameTimeDeltaPartialTick(false);
        if (t > 14) {
            return;
        }
        float pulse = 0.55f + 0.45f * Mth.cos(t * (float) Math.PI);
        int alpha = (int) (215 * dazzleStrength * (1 - t / 14) * pulse);
        int w = g.guiWidth(), h = g.guiHeight();
        g.fill(0, 0, w, h, Mth.clamp(alpha, 0, 255) << 24 | 0xFFFFFF);
        // halo plus fort au centre
        g.fill(w / 4, h / 4, w * 3 / 4, h * 3 / 4, Mth.clamp(alpha / 2, 0, 255) << 24 | 0xFFFFF4);
    }

    // ------------------------------------------------------------------ jauge de pile
    public static void registerDecorators(RegisterItemDecorationsEvent event) {
        for (var holder : fr.modeminecraft.registry.ModItems.ITEMS.getEntries()) {
            if (holder.get() instanceof GunItem) {
                event.register(holder.get(), (graphics, font, stack, x, y) -> {
                    if (GunItem.light(stack) == null) {
                        return false;
                    }
                    float f = GunItem.lightBattery(stack) / (float) GunItem.BATTERY_SECONDS;
                    int color = f > 0.5f ? 0xFF40E040 : f > 0.2f ? 0xFFE0D040 : 0xFFE04040;
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 200);
                    graphics.fill(x + 2, y + 11, x + 15, y + 12, 0xFF000000);
                    graphics.fill(x + 2, y + 11, x + 2 + Math.round(13 * f), y + 12, color);
                    graphics.pose().popPose();
                    return false;
                });
            }
        }
    }

    private FlashlightClient() {}
}

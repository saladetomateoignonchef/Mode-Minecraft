package fr.modeminecraft.client;

import com.mojang.blaze3d.shaders.FogShape;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.AreaCloudEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Dans un nuage de fumigène : brouillard gris opaque (on ne voit qu'à 2-3 blocs). */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class SmokeFog {
    private static float density;

    private static float inside(Vec3 cam) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 0;
        }
        float best = 0;
        for (AreaCloudEntity c : mc.level.getEntitiesOfClass(AreaCloudEntity.class, new AABB(cam, cam).inflate(12),
                c -> c.cloudType() == AreaCloudEntity.Type.SMOKE)) {
            if (c.contains(cam)) {
                double d = Math.sqrt(Math.pow(cam.x - c.getX(), 2) + Math.pow(cam.z - c.getZ(), 2)) / Math.max(1, c.radius());
                best = Math.max(best, (float) (1 - d * 0.5));
            }
        }
        return best;
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        float target = inside(event.getCamera().getPosition());
        density += (target - density) * 0.15f;                     // transition douce en entrant / sortant
        if (density < 0.02f) {
            return;
        }
        float end = (float) (2.5 + (1 - density) * 40);
        event.setNearPlaneDistance(0);
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), end));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (density >= 0.02f) {
            float g = 0.72f;
            event.setRed(event.getRed() + (g - event.getRed()) * density);
            event.setGreen(event.getGreen() + (g - event.getGreen()) * density);
            event.setBlue(event.getBlue() + (g + 0.02f - event.getBlue()) * density);
        }
    }

    private SmokeFog() {}
}

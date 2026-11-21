package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.entity.bot.RadioCalls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

/**
 * Appel radio en cours : au-dessus de la tête du témoin, une icône de signal radio rouge clignotante et la barre des
 * 30 secondes (le temps qu'il reste pour l'empêcher de donner l'alerte). Visible à travers les murs jusqu'à 48 blocs.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class RadioOverlay {
    @SubscribeEvent
    public static void onRender(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof HumanBotEntity bot)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = bot.level().getGameTime();
        long end = bot.radioEnd();
        if (end <= now || mc.player == null || bot.distanceToSqr(mc.player) > 48 * 48) {
            return;
        }
        float left = (end - now - event.getPartialTick()) / RadioCalls.DURATION;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, bot.getBbHeight() + 0.85, 0);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        // l'icône grossit avec la distance pour rester lisible de loin
        float k = 0.03f * Math.max(1f, (float) Math.sqrt(bot.distanceToSqr(mc.player)) / 8f);
        pose.scale(k, -k, k);
        Matrix4f m = pose.last().pose();
        MultiBufferSource buffers = event.getMultiBufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.textBackgroundSeeThrough());
        int light = LightTexture.FULL_BRIGHT;
        boolean blink = (now / 5) % 2 == 0;
        // icône : talkie-walkie et ondes
        quad(vc, m, -6, -22, 6, -8, blink ? 0xFFE02020 : 0xFF801010, light);
        quad(vc, m, -2, -28, 0, -22, 0xFFE02020, light);                      // antenne
        quad(vc, m, -4, -19, 4, -15, 0xFF201010, light);                      // grille
        if (blink) {
            quad(vc, m, 8, -20, 10, -12, 0xFFFF6060, light);                  // ondes
            quad(vc, m, 12, -23, 14, -9, 0xFFFF6060, light);
            quad(vc, m, -10, -20, -8, -12, 0xFFFF6060, light);
            quad(vc, m, -14, -23, -12, -9, 0xFFFF6060, light);
        }
        // barre des 30 secondes
        quad(vc, m, -21, -5, 21, 0, 0xC0000000, light);
        quad(vc, m, -20, -4, -20 + 40 * left, -1, 0xFFFF3030, light);
        Font font = mc.font;
        String s = (int) Math.ceil((end - now) / 20.0) + " s";
        font.drawInBatch(s, -font.width(s) / 2f, 2, 0xFFFF6060, false, m, buffers, Font.DisplayMode.SEE_THROUGH, 0, light);
        pose.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, int argb, int light) {
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        vc.addVertex(m, x0, y1, 0).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x1, y1, 0).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x1, y0, 0).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, x0, y0, 0).setColor(r, g, b, a).setLight(light);
    }

    private RadioOverlay() {}
}

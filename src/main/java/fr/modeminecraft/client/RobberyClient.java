package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.bot.BotGoals;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.entity.bot.Robbery;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Braquage côté client : clic droit maintenu sur un civil à moins de 3,5 blocs, arme en main ; barre de progression. */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class RobberyClient {
    private static int target = -1, held;
    private static boolean sent;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        HumanBotEntity bot = null;
        if (p != null && mc.screen == null && mc.options.keyUse.isDown() && BotGoals.armedInHand(p) && p.getVehicle() == null) {
            bot = aimed(mc);
        }
        if (bot == null || bot.getId() != target) {
            target = bot == null ? -1 : bot.getId();
            held = 0;
            sent = false;
            return;
        }
        if (++held >= Robbery.HOLD_TICKS && !sent) {
            sent = true;
            PacketDistributor.sendToServer(new fr.modeminecraft.network.RobPayload(bot.getId()));
        }
    }

    private static HumanBotEntity aimed(Minecraft mc) {
        var p = mc.player;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        HumanBotEntity best = null;
        double bestDot = 0.92;
        for (HumanBotEntity b : p.level().getEntitiesOfClass(HumanBotEntity.class, p.getBoundingBox().inflate(Robbery.RANGE + 1))) {
            if (!Robbery.robbable(b)) {
                continue;
            }
            Vec3 to = b.getBoundingBox().getCenter().add(0, 0.4, 0).subtract(eye);
            if (to.length() > Robbery.RANGE + 0.3) {
                continue;
            }
            double dot = to.normalize().dot(look);
            if (dot > bestDot && p.hasLineOfSight(b)) {
                bestDot = dot;
                best = b;
            }
        }
        return best;
    }

    /** Barre « Braquage... » sous le réticule. */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        if (target < 0 || held <= 2 || sent) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        int bw = 80, x = (w - bw) / 2, y = h / 2 + 14;
        float f = Math.min(1, held / (float) Robbery.HOLD_TICKS);
        g.fill(x - 1, y - 1, x + bw + 1, y + 5, 0xAA000000);
        g.fill(x, y, x + (int) (bw * f), y + 4, 0xFFE0B020);
        g.drawCenteredString(mc.font, Component.translatable("hud.modeminecraft.robbing"), w / 2, y + 7, 0xFFE8E0C0);
    }

    private RobberyClient() {}
}

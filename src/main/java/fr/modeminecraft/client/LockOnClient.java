package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunStats;
import fr.modeminecraft.network.LockPayload;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Verrouillage du Stinger et du Javelin : en visant (clic droit), on cherche une créature dans un cône de 12°
 * jusqu'à 150 blocs, avec la vue dégagée. Au bout de 35 ticks (1,75 s) elle est verrouillée.
 * Carré jaune + bip lent pendant la recherche, carré rouge + bip continu une fois verrouillé.
 * Si la cible sort du viseur plus de 10 ticks, on recommence.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class LockOnClient {
    public static final int LOCK_TICKS = 35;
    private static LivingEntity candidate;
    private static int lockTicks, lostTicks, sentId = -1;

    public static String debug() {
        Player p = Minecraft.getInstance().player;
        return "active=" + active() + " candidate=" + candidate + " ticks=" + lockTicks + " scan=" + (p == null ? null : scan(p));
    }

    public static boolean active() {
        Player p = Minecraft.getInstance().player;
        return p != null && p.getMainHandItem().getItem() instanceof GunItem g
                && (g.stats().special() == GunStats.Special.LOCK_ON || g.stats().special() == GunStats.Special.LOCK_ON_TOP_ATTACK)
                && GunItem.isAiming(p);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !active()) {
            candidate = null;
            lockTicks = 0;
            send(-1);
            return;
        }
        LivingEntity seen = scan(player);
        if (seen != null && seen == candidate) {
            lockTicks = Math.min(LOCK_TICKS, lockTicks + 1);
            lostTicks = 0;
        } else if (seen != null && candidate == null) {
            candidate = seen;
            lockTicks = 1;
        } else if (candidate != null && ++lostTicks > 10) {
            candidate = seen;
            lockTicks = seen == null ? 0 : 1;
            lostTicks = 0;
        }
        boolean locked = candidate != null && lockTicks >= LOCK_TICKS;
        send(locked ? candidate.getId() : -1);
        if (candidate != null) {
            int period = locked ? 3 : 10;
            if (player.tickCount % period == 0) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI((locked ? ModSounds.LOCK_TONE : ModSounds.LOCK_SEARCH).get(), 1.0f, 0.5f));
            }
        }
    }

    private static void send(int id) {
        if (id != sentId) {
            sentId = id;
            PacketDistributor.sendToServer(new LockPayload(id));
        }
    }

    /** La créature la plus proche du centre du viseur dans un cône de 12°, à moins de 150 blocs, visible. */
    private static LivingEntity scan(Player player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        double cos = Math.cos(Math.toRadians(6));
        LivingEntity best = null;
        double bestDot = cos;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(150),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double d = to.length();
            if (d > 150 || d < 3) {
                continue;
            }
            double dot = to.normalize().dot(look);
            if (dot > bestDot && player.level().clip(new ClipContext(eye, eye.add(to), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS) {
                bestDot = dot;
                best = e;
            }
        }
        return best;
    }

    /** Couche d'affichage : carré de verrouillage au centre. */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        if (!active() || candidate == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean locked = lockTicks >= LOCK_TICKS;
        int color = locked ? 0xFFFF2020 : 0xFFFFD020;
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        int s = locked ? 18 : 26 - (int) (8f * lockTicks / LOCK_TICKS);
        g.fill(cx - s, cy - s, cx + s, cy - s + 1, color);
        g.fill(cx - s, cy + s - 1, cx + s, cy + s, color);
        g.fill(cx - s, cy - s, cx - s + 1, cy + s, color);
        g.fill(cx + s - 1, cy - s, cx + s, cy + s, color);
        g.drawCenteredString(mc.font, Component.translatable(locked ? "hud.modeminecraft.lock_locked" : "hud.modeminecraft.lock_search"),
                cx, cy + s + 4, color);
    }

    private LockOnClient() {}
}

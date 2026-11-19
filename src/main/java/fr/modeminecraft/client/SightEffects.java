package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.ResolvedGun;
import fr.modeminecraft.item.gun.SightMode;
import net.minecraft.client.Minecraft;
import fr.modeminecraft.registry.ModParticles;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Effets des viseurs côté client :
 * - réglage du viseur à la molette (zoom variable, loupe x3, ELCAN, viseur à 45°) ;
 * - vision thermique / nocturne (effet d'image) ;
 * - point laser (visible pour tous les joueurs proches) ;
 * - cible marquée par la lunette en rubis ;
 * - informations de la lunette numérique et du télémètre.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class SightEffects {
    public static final double LASER_RANGE = 64;
    private static final double SIGHT_RANGE = 160;

    private static int modeIndex;
    private static ItemStack lastHeld = ItemStack.EMPTY;
    private static String activePostEffect;
    private static boolean thermal;
    private static int rubyTarget = -1;
    @Nullable private static HitResult aimedAt;

    /** Réglage actuel du viseur de l'arme tenue. */
    public static SightMode currentMode(ResolvedGun gun) {
        return gun.sightMode(modeIndex);
    }

    /** Ce que vise le joueur (pour la lunette numérique et le télémètre), ou null. */
    public static @Nullable HitResult aimedAt() {
        return aimedAt;
    }

    public static boolean shouldGlow(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player) {
            return false;
        }
        return thermal && entity instanceof LivingEntity || entity.getId() == rubyTarget;
    }

    public static @Nullable Integer glowColor(Entity entity) {
        if (entity.getId() == rubyTarget) {
            return 0xFF2040;
        }
        return thermal && entity instanceof LivingEntity ? 0xFF00FF : null; // magenta : reconnu par le shader thermique
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            clearPost(mc);
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held != lastHeld && !ItemStack.isSameItemSameComponents(held, lastHeld)) {
            modeIndex = 0; // nouvelle arme : on revient au premier réglage du viseur
        }
        lastHeld = held;

        ResolvedGun aim = GunClientEvents.aiming(player);
        SightMode mode = aim == null ? null : currentMode(aim);

        // Vision thermique / nocturne : effet d'image tant que l'on vise
        String wanted = mode == null ? null : mode.overlay().postEffect;
        if (wanted == null) {
            clearPost(mc);
        } else if (!wanted.equals(activePostEffect)) {
            mc.gameRenderer.loadEffect(ModeMinecraft.id("shaders/post/" + wanted + ".json"));
            activePostEffect = wanted;
        }
        thermal = aim != null && aim.has(AttachmentItem.Special.THERMAL);

        // Ce que l'on vise (lunette numérique, télémètre, rubis)
        aimedAt = aim == null ? null : pick(player, SIGHT_RANGE);
        rubyTarget = aim != null && aim.has(AttachmentItem.Special.RUBY_MARK) && aimedAt instanceof EntityHitResult e
                ? e.getEntity().getId() : -1;

        // Laser (point + faisceau) de tous les joueurs proches dont le laser est allumé
        for (Player other : mc.level.players()) {
            ItemStack stack = other.getMainHandItem();
            if (other.distanceToSqr(player) < 64 * 64 && stack.getItem() instanceof GunItem) {
                Integer color = GunItem.activeLaserColor(stack);
                if (color != null) {
                    drawLaser(mc, other, color == -1
                            ? Mth.hsvToRgb((mc.level.getGameTime() % 80) / 80f, 1f, 1f) : color);
                }
            }
        }
    }

    /** Point lumineux sur ce que vise le joueur, et faisceau translucide depuis l'arme. */
    private static void drawLaser(Minecraft mc, Player shooter, int rgb) {
        HitResult hit = pick(shooter, LASER_RANGE);
        Vec3 eye = shooter.getEyePosition();
        Vec3 look = shooter.getLookAngle();
        boolean touched = hit.getType() != HitResult.Type.MISS;
        Vec3 end = touched ? hit.getLocation() : eye.add(look.scale(LASER_RANGE));
        if (touched) {
            Vec3 p = end.subtract(look.scale(0.03)); // juste devant la surface touchée
            mc.level.addParticle(ColorParticleOption.create(ModParticles.LASER.get(), 0xFF000000 | rgb), p.x, p.y, p.z, 0, 0, 0);
        }
    }

    private static void clearPost(Minecraft mc) {
        if (activePostEffect != null) {
            mc.gameRenderer.shutdownEffect();
            activePostEffect = null;
        }
        thermal = false;
        rubyTarget = -1;
    }

    /** Ce que touche le regard de ce joueur (utilisé par le faisceau laser). */
    public static HitResult pickFor(Player player, double range) {
        return pick(player, range);
    }

    /** Lancer de rayon depuis les yeux du joueur : bloc ou entité touché. */
    private static HitResult pick(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        HitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eye, limit,
                player.getBoundingBox().expandTowards(limit.subtract(eye)).inflate(1), e -> !e.isSpectator() && e.isPickable(),
                eye.distanceToSqr(limit));
        return entity != null ? entity : block;
    }

    /** Molette pendant la visée : changer de réglage (zoom) au lieu de changer d'objet. */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ResolvedGun aim = GunClientEvents.aiming(mc.player);
        if (aim == null || aim.sightModes().size() < 2) {
            return;
        }
        int count = aim.sightModes().size();
        int next = Math.max(0, Math.min(count - 1, Math.floorMod(modeIndex, count) + (event.getScrollDeltaY() > 0 ? 1 : -1)));
        if (next != Math.floorMod(modeIndex, count)) {
            modeIndex = next;
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.8f);
        }
        event.setCanceled(true);
    }

    private SightEffects() {}
}

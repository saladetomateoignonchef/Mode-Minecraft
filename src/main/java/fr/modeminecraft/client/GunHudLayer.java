package fr.modeminecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.ResolvedGun;
import fr.modeminecraft.item.gun.SightOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Affichage à l'écran :
 * - munitions (chargeur / réserve) à droite de la barre d'inventaire ;
 * - réticule ou lunette quand on vise.
 */
public final class GunHudLayer {
    public static final LayeredDraw.Layer AMMO = GunHudLayer::renderAmmo;
    public static final LayeredDraw.Layer SIGHT_OVERLAY = GunHudLayer::renderSight;

    private static void renderAmmo(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        int ammo = GunItem.getAmmo(stack);
        // Rechargement cartouche par cartouche : on voit les cartouches entrer une à une
        Long rStart = stack.get(fr.modeminecraft.registry.ModDataComponents.RELOAD_START.get());
        Long rEnd = stack.get(fr.modeminecraft.registry.ModDataComponents.RELOAD_END.get());
        Integer rFrom = stack.get(fr.modeminecraft.registry.ModDataComponents.RELOAD_FROM.get());
        long now = player.level().getGameTime();
        if (gun.stats().reloadPerRound() && rStart != null && rEnd != null && rFrom != null && now >= rStart && now < rEnd) {
            int per = Math.max(1, gun.resolve(stack).reloadTicks());
            ammo = Math.min(ammo, rFrom + (int) ((now - rStart) / per));
        }
        int max = gun.resolve(stack).magazineSize();
        String reserve = player.getAbilities().instabuild ? "∞" : Integer.toString(gun.countReserve(player, stack));
        boolean chambered = gun.stats().chamber() && (GunItem.isChambered(stack) || player.getAbilities().instabuild);
        String text = ammo + (chambered ? "+1" : "") + " / " + max + "  [" + reserve + "]";

        int x = graphics.guiWidth() / 2 + 91 + 8;
        int y = graphics.guiHeight() - 19;
        graphics.renderItem(new ItemStack(gun.stats().ammo().get()), x, y - 1);
        int color = ammo == 0 ? 0xFF5555 : (ammo <= max / 4 ? 0xFFAA00 : 0xFFFFFF);
        graphics.drawString(mc.font, text, x + 18, y + 4, color, true);
        // Mitrailleuses : jauge de chaleur du canon
        if (gun.stats().heatPerShot() > 0) {
            float heat = GunItem.heat(stack, player.level());
            boolean over = GunItem.isOverheated(stack, player.level());
            int bx = x + 18, by = y - 12, bw = 60;
            int hc = over ? 0xFFFF3030 : heat > 70 ? 0xFFFF8020 : heat > 40 ? 0xFFFFD040 : 0xFF60D060;
            graphics.fill(bx - 1, by - 1, bx + bw + 1, by + 3, 0xA0000000);
            graphics.fill(bx, by, bx + Math.round(bw * heat / 100f), by + 2, hc);
            if (over) {
                graphics.drawString(mc.font, Component.translatable("hud.modeminecraft.overheated"), bx, by - 10, 0xFF4040, true);
            }
        }
        // Mode de tir, juste au-dessus des munitions
        Component mode = Component.translatable(gun.getFireMode(stack).shortKey());
        graphics.drawString(mc.font, mode, x + 18, y - 6, 0xFFD84A, true);
        var type = GunItem.getLoadedType(stack);
        if (type != fr.modeminecraft.item.gun.AmmoType.STANDARD) {
            graphics.drawString(mc.font, Component.translatable(type.translationKey()),
                    x + 22 + mc.font.width(mode), y - 6, type.color, true);
        }
    }

    /** Grenade dégoupillée : temps restant avant l'explosion. */
    public static void renderGrenadeTimer(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isUsingItem()
                || !(mc.player.getUseItem().getItem() instanceof fr.modeminecraft.item.explosive.GrenadeItem g) || !g.kind().cooks()) {
            return;
        }
        float left = (g.kind().fuse - mc.player.getTicksUsingItem()) / 20f;
        int color = left < 1 ? 0xFF4040 : 0xFFD84A;
        graphics.drawCenteredString(mc.font, Component.translatable("hud.modeminecraft.grenade_timer", String.format("%.1f", Math.max(0, left))),
                graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 16, color);
    }

    /** Mitrailleuse posée : bande de munitions et chaleur. */
    public static void renderMounted(GuiGraphics graphics, DeltaTracker deltaTracker) {
        TurretClient.renderHud(graphics, deltaTracker);
    }

    private static void renderSight(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ResolvedGun aim = GunClientEvents.aiming(Minecraft.getInstance().player);
        if (aim == null) {
            return;
        }
        SightOverlay overlay = SightEffects.currentMode(aim).overlay();
        renderSightInfo(graphics, aim);
        if (overlay.texture == null) {
            return;
        }
        // Le réticule n'apparaît qu'une fois l'arme épaulée
        if (GunPoseHelper.aimProgress(deltaTracker.getGameTimeDeltaPartialTick(false)) < 0.85f) {
            return;
        }
        ResourceLocation texture = ModeMinecraft.id(overlay.texture);
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        RenderSystem.enableBlend(); // les réticules sont transparents
        if (overlay.isFullScope()) {
            // Lunette : elle remplit tout l'écran (on a l'œil collé à l'oculaire) ; seuls les coins sont sombres
            int size = Math.max(w, h);
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            graphics.blit(texture, x, y, -90, 0.0f, 0.0f, size, size, size, size);
        } else {
            // Viseur point rouge / holographique : petit réticule au centre
            int size = switch (overlay) {
                case HOLO, KOBRA, GHOST_RING -> 64;
                default -> 16;
            };
            graphics.blit(texture, (w - size) / 2, (h - size) / 2, 0, 0.0f, 0.0f, size, size, size, size);
        }
        RenderSystem.disableBlend();
    }

    /** Lunette numérique (cible + points de vie), télémètre (distance), zoom variable. */
    /** Lunette : force et sens du vent (par rapport à là où l'on regarde) + jauge de souffle. */
    private static void renderWindAndBreath(GuiGraphics graphics, int cx, int cy) {
        Minecraft mc = Minecraft.getInstance();
        var wind = fr.modeminecraft.item.gun.Wind.get(mc.level);
        var look = mc.player.getLookAngle();
        var forward = new net.minecraft.world.phys.Vec3(look.x, 0, look.z).normalize();
        var right = fr.modeminecraft.item.gun.LeanHandler.right(mc.player);
        double side = wind.dot(right), ahead = wind.dot(forward);
        // Flèche : sens dans lequel le vent pousse la balle, vu par le tireur
        String[] arrows = {"→", "↗", "↑", "↖", "←", "↙", "↓", "↘"};
        double angle = Math.atan2(ahead, side);
        String arrow = arrows[Math.floorMod((int) Math.round(angle / (Math.PI / 4)), 8)];
        String speed = String.format("%.1f", wind.length());
        graphics.drawString(mc.font, Component.translatable("hud.modeminecraft.wind", speed, arrow),
                cx + 70, cy - 4, 0xA0E0FF, true);
        // Souffle
        int w = 50, x0 = cx - w / 2, y0 = cy + 60;
        float breath = AimClient.breath();
        int color = AimClient.exhausted() ? 0xFFFF5050 : AimClient.holdingBreath() ? 0xFF60D0FF : 0xFFE0E0E0;
        graphics.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + 3, 0xA0000000);
        graphics.fill(x0, y0, x0 + Math.round(w * breath), y0 + 2, color);
        graphics.drawCenteredString(mc.font, Component.translatable("hud.modeminecraft.breath",
                ModKeyMappings.HOLD_BREATH.getTranslatedKeyMessage()), cx, y0 + 5, 0xC0C0C0);
    }

    private static void renderSightInfo(GuiGraphics graphics, ResolvedGun aim) {
        Minecraft mc = Minecraft.getInstance();
        int cx = graphics.guiWidth() / 2, cy = graphics.guiHeight() / 2;
        var hit = SightEffects.aimedAt();
        int line = cy + 40;
        if (aim.sightModes().size() > 1) {
            String zoom = String.format("x%.1f", SightEffects.currentMode(aim).magnification()).replace(".0", "");
            graphics.drawCenteredString(mc.font, net.minecraft.network.chat.Component.translatable("hud.modeminecraft.zoom", zoom),
                    cx, cy - 52, 0xFFD84A);
        }
        if (SightEffects.currentMode(aim).zoom() <= 0.26f) {
            renderWindAndBreath(graphics, cx, cy);
        }
        if (hit == null) {
            return;
        }
        if (aim.has(fr.modeminecraft.item.gun.AttachmentItem.Special.RANGEFINDER) && hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            int meters = (int) Math.round(Math.sqrt(hit.getLocation().distanceToSqr(mc.player.getEyePosition())));
            graphics.drawCenteredString(mc.font, net.minecraft.network.chat.Component.translatable("hud.modeminecraft.distance", meters),
                    cx, line, 0xFF5050);
            line += 10;
        }
        if (aim.has(fr.modeminecraft.item.gun.AttachmentItem.Special.DIGITAL)
                && hit instanceof net.minecraft.world.phys.EntityHitResult e && e.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
            graphics.drawCenteredString(mc.font, living.getDisplayName(), cx, line, 0x60FF80);
            graphics.drawCenteredString(mc.font, net.minecraft.network.chat.Component.translatable("hud.modeminecraft.health",
                    String.format("%.1f", living.getHealth()), String.format("%.0f", living.getMaxHealth())), cx, line + 10, 0x60FF80);
        }
    }

    private GunHudLayer() {}
}

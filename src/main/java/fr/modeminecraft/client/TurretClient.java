package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.MountedGunEntity;
import fr.modeminecraft.entity.TurretGeometry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Côté client des tourelles :
 * - visée (clic droit maintenu) : l'œil vient se placer dans l'axe des anneaux du viseur WWII, léger zoom mécanique
 *   (x1,3 au plus, sans lentille), et la mire tremble pendant les longues rafales ;
 * - HUD (coin inférieur droit) : caisse de munitions, compteur "84 / 250" et thermomètre de surchauffe ;
 * - en vue à la 3e personne, les bras du modèle du joueur sont cachés : ce sont ceux dessinés sur les poignées.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class TurretClient {
    private static float ads, adsO;

    private static MountedGunEntity turret() {
        Player p = Minecraft.getInstance().player;
        return p != null && p.getVehicle() instanceof MountedGunEntity g ? g : null;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        adsO = ads;
        boolean aiming = turret() != null && mc.screen == null && mc.options.keyUse.isDown() && mc.mouseHandler.isMouseGrabbed();
        ads = Mth.clamp(ads + (aiming ? 0.25f : -0.25f), 0, 1);
    }

    public static float ads(float partialTick) {
        float a = Mth.lerp(partialTick, adsO, ads);
        return a * a * (3 - 2 * a);
    }

    /** Position de la caméra (vue à la 1re personne) sur une tourelle, ou null pour la position normale. */
    public static Vec3 cameraPosition(Vec3 normal, float partialTick) {
        MountedGunEntity gun = turret();
        Minecraft mc = Minecraft.getInstance();
        if (gun == null || !mc.options.getCameraType().isFirstPerson()) {
            return null;
        }
        TurretGeometry.Geo geo = gun.kind().geo();
        float a = ads(partialTick);
        Vec3 pos = normal;
        if (a > 0 && geo != null) {
            Vec3 sight = gun.getPosition(partialTick).add(TurretGeometry.offset(geo.pivot(), gun.gunYaw(partialTick),
                    gun.gunPitch(partialTick), geo.adsEye()));
            pos = normal.lerp(sight, a);
        }
        // Tremblement mécanique de la mire pendant les rafales (plus fort en visée et au fil de la rafale)
        float since = gun.level().getGameTime() + partialTick - gun.lastShot();
        if (since < 3) {
            float amp = (0.004f + 0.012f * Math.min(1, gun.burst() / 40f)) * (0.5f + a) * (1 - since / 3);
            var r = gun.getRandom();
            pos = pos.add((r.nextFloat() - 0.5f) * amp, (r.nextFloat() - 0.5f) * amp, (r.nextFloat() - 0.5f) * amp);
        }
        return pos;
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        float a = ads(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        if (turret() != null && a > 0) {
            event.setNewFovModifier(event.getNewFovModifier() / (1 + 0.3f * a)); // zoom mécanique x1,3 au plus
        }
    }

    /** En visée, pas de réticule : on vise avec les anneaux du viseur. */
    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && turret() != null
                && ads(event.getPartialTick().getGameTimeDeltaPartialTick(false)) > 0.3f) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ arme personnelle masquée au poste de tourelle
    private static final java.util.Map<Integer, ItemStack[]> HIDDEN = new java.util.HashMap<>();

    /** Le temps du rendu, les mains du tireur sont vides (les items sont remis juste après). */
    @SubscribeEvent
    public static void onRenderLivingPre(net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?> event) {
        var e = event.getEntity();
        if (e.getVehicle() == null || !fr.modeminecraft.entity.TurretPost.operating(e)) {
            return;
        }
        var main = e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        var off = e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        if (main.isEmpty() && off.isEmpty()) {
            return;
        }
        HIDDEN.put(e.getId(), new ItemStack[]{main, off});
        e.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        e.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void onRenderLivingPost(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) {
        ItemStack[] saved = HIDDEN.remove(event.getEntity().getId());
        if (saved != null) {
            event.getEntity().setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, saved[0]);
            event.getEntity().setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, saved[1]);
        }
    }

    /** Première personne : pas d'arme dans les mains tant qu'on est au poste de tourelle. */
    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        Player p = Minecraft.getInstance().player;
        if (p != null && fr.modeminecraft.entity.TurretPost.operating(p)) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ bras du modèle cachés (3e personne)
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getVehicle() instanceof MountedGunEntity) {
            var m = event.getRenderer().getModel();
            m.leftArm.visible = m.rightArm.visible = m.leftSleeve.visible = m.rightSleeve.visible = false;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (event.getEntity().getVehicle() instanceof MountedGunEntity) {
            var m = event.getRenderer().getModel();
            m.leftArm.visible = m.rightArm.visible = true;
            m.leftSleeve.visible = event.getEntity().isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE);
            m.rightSleeve.visible = event.getEntity().isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE);
        }
    }

    // ------------------------------------------------------------------ HUD
    public static void renderHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        MountedGunEntity gun = turret();
        if (gun == null || mc.options.hideGui) {
            return;
        }
        float partial = delta.getGameTimeDeltaPartialTick(false);
        MountedGunEntity.Kind kind = gun.kind();
        int w = g.guiWidth(), h = g.guiHeight();
        int x = w - 74, y = h - 46;
        // Icône de la caisse de munitions
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(1.75f, 1.75f, 1);
        g.renderItem(new ItemStack(kind.ammo()), 0, 0);
        g.pose().popPose();
        // Thermomètre de surchauffe (vertical)
        int tx = x + 34, ty = y, th = 28;
        g.fill(tx - 1, ty - 1, tx + 6, ty + th + 1, 0xAA000000);
        float heat = gun.isOverheated() ? 1 : gun.heat();
        int filled = Math.round(th * heat);
        int color = heat > 0.75f ? 0xFFE03020 : heat > 0.4f ? 0xFFE0A020 : 0xFF40C040;
        if (gun.isOverheated() && (mc.level.getGameTime() / 4) % 2 == 0) {
            color = 0xFFFFFFFF;
        }
        g.fill(tx, ty + th - filled, tx + 5, ty + th, color);
        g.fill(tx - 2, ty + th - 1, tx + 7, ty + th + 4, color);   // réservoir du thermomètre
        // Compteur exact
        int ammo = gun.ammo();
        String count = ammo + " / " + kind.capacity;
        int cc = ammo == 0 ? 0xFFFF5050 : ammo < kind.capacity / 5 ? 0xFFFFC040 : 0xFFFFFFFF;
        g.pose().pushPose();
        g.pose().translate(w - 8 - mc.font.width(count) * 1.5f, h - 14, 0);
        g.pose().scale(1.5f, 1.5f, 1);
        g.drawString(mc.font, count, 0, 0, cc, true);
        g.pose().popPose();
        // État
        Component status = null;
        float r = gun.reloadProgress(partial);
        if (r >= 0) {
            status = Component.translatable("hud.modeminecraft.turret.reloading", Math.round(r * 100)).withStyle(ChatFormatting.YELLOW);
        } else if (gun.isOverheated()) {
            status = Component.translatable("hud.modeminecraft.turret.overheated").withStyle(ChatFormatting.RED);
        } else if (ammo == 0) {
            status = Component.translatable("hud.modeminecraft.turret.empty").withStyle(ChatFormatting.RED);
        } else if (gun.deployTicks() < MountedGunEntity.DEPLOY_TICKS) {
            status = Component.translatable("hud.modeminecraft.turret.deploying").withStyle(ChatFormatting.GRAY);
        }
        if (status != null) {
            g.drawString(mc.font, status, w - 8 - mc.font.width(status), y - 12, 0xFFFFFFFF, true);
        }
    }

    private TurretClient() {}
}

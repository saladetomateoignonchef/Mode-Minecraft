package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunAnimations;
import fr.modeminecraft.network.HitMarkerPayload;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Effets des tirs côté client :
 * - flash de bouche qui éclaire vraiment les alentours la nuit (bloc de lumière invisible, côté client
 *   seulement, retiré 2 ticks plus tard) ;
 * - culasse qui recule sur l'arme des autres joueurs ;
 * - hit marker (croix au centre de l'écran + petit son) quand nos balles touchent.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class GunFx {
    private record Lit(BlockPos pos, BlockState previous, long until) {}

    private static final List<Lit> LIT = new ArrayList<>();
    private static long hitTime = -100;
    private static int hitKind;

    /** Tir du joueur local (appelé par GunItem côté client). */
    public static void onLocalShot(Player player, boolean silenced) {
        if (!silenced) {
            // Pas tout de suite : pendant le clic, Minecraft "prédit" les blocs et annulerait la lumière
            PENDING.add(player);
        }
    }

    private static final List<Player> PENDING = new ArrayList<>();

    /** Un autre joueur a tiré (message du serveur). */
    public static void onRemoteShot(int entityId, boolean silenced) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity e = mc.level.getEntity(entityId);
        if (e instanceof Player shooter) {
            GunAnimations.recordShot(shooter);
            if (!silenced) {
                flash(shooter);
            }
        }
    }

    private static void flash(Player shooter) {
        var level = Minecraft.getInstance().level;
        Vec3 muzzle = shooter.getEyePosition().add(shooter.getLookAngle().scale(1.2));
        BlockPos pos = BlockPos.containing(muzzle);
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            pos = BlockPos.containing(shooter.getEyePosition());
            state = level.getBlockState(pos);
            if (!state.isAir()) {
                return;
            }
        }
        int sky = (int) (level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) * level.getSkyDarken(1.0f));
        if (Math.max(sky, level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos)) >= 12) {
            return; // déjà bien éclairé : inutile
        }
        for (Lit lit : LIT) {
            if (lit.pos.equals(pos)) {
                return;
            }
        }
        level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 13), 3);
        LIT.add(new Lit(pos, state, level.getGameTime() + 3));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            LIT.clear();
            PENDING.clear();
            return;
        }
        for (Player p : PENDING) {
            flash(p);
        }
        PENDING.clear();
        LIT.removeIf(lit -> {
            if (level.getGameTime() < lit.until) {
                return false;
            }
            if (level.getBlockState(lit.pos).is(Blocks.LIGHT)) {
                level.setBlock(lit.pos, lit.previous, 3);
            }
            return true;
        });
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        LIT.clear();
    }

    // ------------------------------------------------------------------ hit marker
    public static void onHitMarker(int kind) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        hitTime = mc.level.getGameTime();
        hitKind = kind;
        float pitch = kind == HitMarkerPayload.KILL ? 0.75f : kind == HitMarkerPayload.HEADSHOT ? 1.3f : 1.0f;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HITMARKER.get(), pitch, 0.6f));
    }

    /** Couche d'affichage : la croix du hit marker, qui s'efface en 8 ticks. */
    public static void renderHitMarker(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) {
            return;
        }
        float age = mc.level.getGameTime() - hitTime + delta.getGameTimeDeltaPartialTick(false);
        if (age > 8) {
            return;
        }
        int alpha = (int) (255 * (1 - age / 8f));
        int rgb = hitKind == HitMarkerPayload.KILL ? 0xFF3030 : hitKind == HitMarkerPayload.HEADSHOT ? 0xFFD020 : 0xFFFFFF;
        int color = alpha << 24 | rgb;
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        int gap = 3, len = hitKind == HitMarkerPayload.KILL ? 7 : 5;
        for (int i = gap; i < gap + len; i++) {
            g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
            g.fill(cx - i, cy + i, cx - i + 1, cy + i + 1, color);
            g.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, color);
            g.fill(cx - i, cy - i, cx - i + 1, cy - i + 1, color);
        }
    }

    private GunFx() {}
}

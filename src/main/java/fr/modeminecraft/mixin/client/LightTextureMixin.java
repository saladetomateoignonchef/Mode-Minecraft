package fr.modeminecraft.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Nuit réaliste : loin de toute source de lumière, la lumière du ciel nocturne tombe presque à zéro (au lieu du clair
 * de lune très lumineux de Minecraft), et le réglage de luminosité ne la remonte plus. Les zones éclairées par des
 * blocs (torches, lampadaires, lampes tactiques) gardent leur luminosité ; le jour et les grottes ne changent pas.
 * La vision nocturne (jumelles GPNVG-18, lunettes de vision nocturne, potion) annule l'effet.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"))
    private void modeminecraft$darkNights(NativeImage image, int block, int sky, int abgr) {
        image.setPixelRGBA(block, sky, modeminecraft$darken(block, sky, abgr));
    }

    private static int modeminecraft$darken(int block, int sky, int abgr) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.level.dimensionType().hasSkyLight()
                || mc.level.effects().forceBrightLightmap() || mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
            return abgr;
        }
        // 1 le jour, ~0,2 la nuit : intensité de la nuit
        float skyDarken = mc.level.getSkyDarken(1.0F);
        float night = Mth.clamp((0.75f - skyDarken) / 0.5f, 0, 1);
        if (night <= 0 || mc.level.getSkyFlashTime() > 0) {
            return abgr;
        }
        float bl = block / 15f;
        float blockShare = bl * bl * (3 - 2 * bl);               // lumière des blocs : préservée
        float skyShare = Math.min(1, sky / 5f);                   // grottes (pas de ciel) : inchangées
        float k = 1 - night * 0.93f * (1 - blockShare) * skyShare;
        int r = (int) ((abgr & 0xFF) * k), g = (int) (((abgr >> 8) & 0xFF) * k), b = (int) (((abgr >> 16) & 0xFF) * k);
        return 0xFF000000 | b << 16 | g << 8 | r;
    }
}

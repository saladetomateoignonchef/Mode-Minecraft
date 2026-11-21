package fr.modeminecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Niveau de détail : au-delà de 40 blocs, les détails 3D coûteux (casques, gilets, armes tenues, sacs, capes) d'un
 * personnage ne sont plus dessinés ; à cette distance ils ne mesurent que quelques pixels. Gros gain d'images par
 * seconde dans les villes peuplées.
 */
public final class RenderLod {
    public static final double DETAIL = 40;

    public static boolean far(Entity e) {
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        return e != Minecraft.getInstance().player && cam.getPosition().distanceToSqr(e.position()) > DETAIL * DETAIL;
    }

    private RenderLod() {}
}

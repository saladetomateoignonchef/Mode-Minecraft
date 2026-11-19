package fr.modeminecraft.client;

import fr.modeminecraft.entity.BoltEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Carreau d'arbalète : dessiné comme une flèche. */
public class BoltRenderer extends ArrowRenderer<BoltEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public BoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BoltEntity entity) {
        return TEXTURE;
    }
}

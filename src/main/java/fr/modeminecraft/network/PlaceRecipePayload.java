package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Livre des recettes, bouton [+] : place les ingrédients d'une recette du mod dans l'établi / le four ouvert.
 * Minecraft refuse de placer une recette que le joueur n'a pas "débloquée" : on la débloque d'abord
 * (elle apparaît alors aussi dans le livre de recettes vanilla), puis on utilise le placement de Minecraft.
 */
public record PlaceRecipePayload(int containerId, ResourceLocation recipe, boolean all) implements CustomPacketPayload {
    public static final Type<PlaceRecipePayload> TYPE = new Type<>(ModeMinecraft.id("place_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceRecipePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlaceRecipePayload::containerId, ResourceLocation.STREAM_CODEC, PlaceRecipePayload::recipe,
            ByteBufCodecs.BOOL, PlaceRecipePayload::all, PlaceRecipePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlaceRecipePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()
                    || player.containerMenu.containerId != payload.containerId() || !(player.containerMenu instanceof RecipeBookMenu<?, ?> menu)
                    || !payload.recipe().getNamespace().equals(ModeMinecraft.MODID)) {
                return;
            }
            player.server.getRecipeManager().byKey(payload.recipe()).ifPresent(holder -> {
                player.awardRecipes(List.of(holder));
                menu.handlePlacement(payload.all(), holder, player);
            });
        });
    }
}

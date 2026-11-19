package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Code uniquement côté client (affichage), exécuté au démarrage du jeu.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> registerBowProperties(ModItems.RUBIS_BOW.get()));
    }

    /** Les balles et roquettes sont affichées avec la texture de leur item. */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BULLET.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.5f, true));
        event.registerEntityRenderer(ModEntities.ROCKET.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.25f, true));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.RELOAD);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ModeMinecraft.id("gun_ammo"), GunHudLayer.AMMO);
        event.registerBelow(VanillaGuiLayers.HOTBAR, ModeMinecraft.id("sniper_scope"), GunHudLayer.SCOPE_OVERLAY);
    }

    /** En vue à la 3e personne, le joueur tient ses armes à feu à deux mains, en joue. */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        }, ModItems.PISTOL.get(), ModItems.ASSAULT_RIFLE.get(), ModItems.SHOTGUN.get(),
                ModItems.SNIPER_RIFLE.get(), ModItems.ROCKET_LAUNCHER.get());
    }

    /** Permet à l'arc de changer de texture quand on le tend (même logique que l'arc vanilla). */
    private static void registerBowProperties(Item bow) {
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> {
            if (entity == null || entity.getUseItem() != stack) {
                return 0.0f;
            }
            return (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0f;
        });
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f);
    }

    private ClientSetup() {}
}

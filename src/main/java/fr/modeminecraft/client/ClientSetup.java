package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.client.particle.CasingParticle;
import fr.modeminecraft.client.screen.ArmamentTableScreen;
import fr.modeminecraft.client.screen.ModificationTableScreen;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.registry.ModMenus;
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
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
        event.enqueueWork(() -> {
            registerBowProperties(ModItems.RUBIS_BOW.get());
            // Munitions spéciales : le modèle affiche une marque de couleur selon le type
            ModItems.ITEMS.getEntries().stream().map(h -> h.get())
                    .filter(i -> i instanceof fr.modeminecraft.item.AmmoItem)
                    .forEach(i -> ItemProperties.register(i, ModeMinecraft.id("ammo_type"), (stack, level, entity, seed) ->
                            fr.modeminecraft.item.AmmoItem.getType(stack).ordinal() / 100f + 0.001f));
        });
    }

    /** Les balles et roquettes sont affichées avec la texture de leur item. */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BULLET.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.5f, true));
        event.registerEntityRenderer(ModEntities.ROCKET.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.25f, true));
        event.registerEntityRenderer(ModEntities.GRENADE.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.8f, false));
    }

    @SubscribeEvent
    public static void registerParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(fr.modeminecraft.registry.ModParticles.CASING_BRASS.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(fr.modeminecraft.registry.ModParticles.CASING_STEEL.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(fr.modeminecraft.registry.ModParticles.CASING_SHELL.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(fr.modeminecraft.registry.ModParticles.TRACER.get(),
                fr.modeminecraft.client.particle.TracerParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.RELOAD);
        event.register(ModKeyMappings.FIRE_MODE);
        event.register(ModKeyMappings.CHECK_CHAMBER);
        event.register(ModKeyMappings.AMMO_TYPE);
        event.register(ModKeyMappings.UNDERBARREL);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ModeMinecraft.id("gun_ammo"), GunHudLayer.AMMO);
        event.registerBelow(VanillaGuiLayers.HOTBAR, ModeMinecraft.id("gun_sight"), GunHudLayer.SIGHT_OVERLAY);
    }

    /** En vue à la 3e personne, le joueur tient toutes ses armes à feu à deux mains, en joue. */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }

            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                   net.minecraft.client.player.LocalPlayer player,
                                                   net.minecraft.world.entity.HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProcess, float swingProcess) {
                GunPoseHelper.apply(poseStack, player, arm, stack, partialTick, equipProcess, swingProcess);
                return true;
            }
        }, ModItems.ITEMS.getEntries().stream().map(h -> h.get())
                .filter(item -> item instanceof GunItem).toArray(Item[]::new));
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ARMAMENT_TABLE.get(), ArmamentTableScreen::new);
        event.register(ModMenus.MODIFICATION_TABLE.get(), ModificationTableScreen::new);
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

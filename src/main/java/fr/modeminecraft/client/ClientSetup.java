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
        fr.modeminecraft.update.AutoUpdater.start(); // nouvelle version sur GitHub ? (en arrière-plan)
        event.enqueueWork(() -> {
            // Casques : modèle "visière abaissée / jumelles abaissées" quand le module est actif
            for (var h : ModItems.ITEMS.getEntries()) {
                if (h.get() instanceof fr.modeminecraft.item.HelmetItem) {
                    net.minecraft.client.renderer.item.ItemProperties.register(h.get(), ModeMinecraft.id("module"),
                            (stack, level, entity, seed) -> fr.modeminecraft.item.HelmetItem.isModuleActive(stack) ? 1 : 0);
                }
            }
            registerBowProperties(ModItems.RUBIS_BOW.get());
            ItemProperties.register(ModItems.BALLISTIC_SHIELD.get(), ResourceLocation.withDefaultNamespace("blocking"), (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0);
            // Heaume du Chef des Troupes de Choc : fissuré, yeux rouges en Mode Berserk
            ItemProperties.register(ModItems.GREAT_HELM.get(), ModeMinecraft.id("berserk"), (stack, level, entity, seed) ->
                    entity instanceof fr.modeminecraft.entity.bot.HumanBotEntity b && b.berserk() ? 1 : 0);
            // Munitions spéciales : le modèle affiche une marque de couleur selon le type
            ModItems.ITEMS.getEntries().stream().map(h -> h.get())
                    .filter(i -> i instanceof fr.modeminecraft.item.AmmoItem)
                    .forEach(i -> ItemProperties.register(i, ModeMinecraft.id("ammo_type"), (stack, level, entity, seed) ->
                            fr.modeminecraft.item.AmmoItem.getType(stack).ordinal() / 100f + 0.001f));
        });
    }

    /** Les balles et roquettes sont affichées avec la texture de leur item. */
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PigeonRenderer.LAYER, PigeonRenderer::layer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(fr.modeminecraft.registry.ModBlockEntities.WEAPON_RACK.get(), WeaponRackRenderer::new);
        event.registerBlockEntityRenderer(fr.modeminecraft.registry.ModBlockEntities.TROPHY_RACK.get(), TrophyRackRenderer::new);
        event.registerBlockEntityRenderer(fr.modeminecraft.registry.ModBlockEntities.TRAFFIC_LIGHT.get(), TrafficLightRenderer::new);
        event.registerBlockEntityRenderer(fr.modeminecraft.registry.ModBlockEntities.JAIL_DOOR.get(), JailDoorRenderer::new);
        event.registerBlockEntityRenderer(fr.modeminecraft.registry.ModBlockEntities.WALL_CLOCK.get(), WallClockRenderer::new);
        event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
        event.registerEntityRenderer(ModEntities.ROCKET.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.25f, true));
        event.registerEntityRenderer(ModEntities.GRENADE.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.8f, false));
        event.registerEntityRenderer(ModEntities.MISSILE.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.4f, true));
        event.registerEntityRenderer(ModEntities.PLASMA.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.9f, true));
        event.registerEntityRenderer(ModEntities.BOLT.get(), BoltRenderer::new);
        event.registerEntityRenderer(ModEntities.TOMAHAWK.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.2f, false));
        event.registerEntityRenderer(ModEntities.MOUNTED_GUN.get(), MountedGunRenderer::new);
        event.registerEntityRenderer(ModEntities.HUMAN_BOT.get(), HumanBotRenderer::new);
        event.registerEntityRenderer(ModEntities.PIGEON.get(), PigeonRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRDROP.get(), AirdropRenderer::new);
        event.registerEntityRenderer(ModEntities.GEYSER.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.VEHICLE.get(), VehicleRenderer::new);
        event.registerEntityRenderer(ModEntities.DRONE.get(), DroneRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_GRENADE.get(), ExplosiveRenderers.Grenade::new);
        event.registerEntityRenderer(ModEntities.PLACED_EXPLOSIVE.get(), ExplosiveRenderers.Placed::new);
        event.registerEntityRenderer(ModEntities.AREA_CLOUD.get(), ExplosiveRenderers.Invisible::new);
    }

    @SubscribeEvent
    public static void registerParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(fr.modeminecraft.registry.ModParticles.CASING_BRASS.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(fr.modeminecraft.registry.ModParticles.NEWSPAPER.get(), fr.modeminecraft.client.particle.NewspaperParticle.Provider::new);
        event.registerSpriteSet(fr.modeminecraft.registry.ModParticles.CASING_STEEL.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(fr.modeminecraft.registry.ModParticles.CASING_SHELL.get(), s -> new CasingParticle.Provider(s, true));
        event.registerSpriteSet(fr.modeminecraft.registry.ModParticles.TRACER.get(),
                fr.modeminecraft.client.particle.TracerParticle.Provider::new);
        event.registerSpriteSet(fr.modeminecraft.registry.ModParticles.MUZZLE_FLASH.get(),
                fr.modeminecraft.client.particle.MuzzleFlashParticle.Provider::new);
        event.registerSpriteSet(fr.modeminecraft.registry.ModParticles.LASER.get(),
                fr.modeminecraft.client.particle.LaserParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.RELOAD);
        event.register(ModKeyMappings.FIRE_MODE);
        event.register(ModKeyMappings.CHECK_CHAMBER);
        event.register(ModKeyMappings.AMMO_TYPE);
        event.register(ModKeyMappings.UNDERBARREL);
        event.register(ModKeyMappings.LASER);
        event.register(ModKeyMappings.FLASHLIGHT);
        event.register(ModKeyMappings.LEAN_LEFT);
        event.register(ModKeyMappings.LEAN_RIGHT);
        event.register(ModKeyMappings.HOLD_BREATH);
        event.register(ModKeyMappings.MELEE);
        event.register(ModKeyMappings.QUICK_ATTACH);
        event.register(ModKeyMappings.HELMET_MODULE);
        event.register(ModKeyMappings.BACKPACK);
        event.register(ModKeyMappings.SIREN);
        event.register(ModKeyMappings.ITEM_PANEL);
        event.register(ModKeyMappings.PRONE);
        event.register(ModKeyMappings.SHOW_RECIPE);
        event.register(ModKeyMappings.SHOW_USES);
    }

    /** Boussole de traque : même aiguille que la boussole, pointée sur la cible du contrat. */
    @SubscribeEvent
    public static void compassProperty(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(ModItems.TRACKING_COMPASS.get(),
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("angle"),
                new net.minecraft.client.renderer.item.CompassItemPropertyFunction((level, stack, entity) -> {
                    var t = stack.get(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER);
                    return t != null ? t.target().orElse(null) : null;
                })));
    }

    /** Couleurs des œufs d'apparition des bots (celle de la faction). */
    @SubscribeEvent
    public static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        for (var egg : ModItems.BOT_EGGS) {
            event.register((stack, tint) -> {
                var f = ((fr.modeminecraft.item.BotSpawnEggItem) stack.getItem()).faction();
                int c = f.color;
                return 0xFF000000 | (tint == 0 ? c : (c >> 1 & 0x7F7F7F));
            }, egg.get());
        }
    }

    @SubscribeEvent
    public static void registerDecorations(net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent event) {
        FlashlightClient.registerDecorators(event);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ModeMinecraft.id("gun_ammo"), GunHudLayer.AMMO);
        event.registerBelow(VanillaGuiLayers.HOTBAR, ModeMinecraft.id("gun_sight"), GunHudLayer.SIGHT_OVERLAY);
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, ModeMinecraft.id("helmet_overlay"), HelmetClient::renderOverlay);
        event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL, ModeMinecraft.id("vest_plates"), VestClient::renderHud);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ModeMinecraft.id("hit_marker"), GunFx::renderHitMarker);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ModeMinecraft.id("lock_on"), LockOnClient::render);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ModeMinecraft.id("grenade_timer"), GunHudLayer::renderGrenadeTimer);
        event.registerAboveAll(ModeMinecraft.id("flashbang"), FlashClient::render);
        event.registerAboveAll(ModeMinecraft.id("drone_hud"), DroneClient::renderHud);
        event.registerAboveAll(ModeMinecraft.id("strobe_dazzle"), FlashlightClient::renderDazzle);
        event.registerAbove(VanillaGuiLayers.HOTBAR, ModeMinecraft.id("mounted_gun"), GunHudLayer::renderMounted);
    }

    /** Armes rangées visibles sur les joueurs (dos / hanche). */
    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void addLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer player) {
                player.addLayer(new BackGunsLayer(player));
                player.addLayer(new BackpackLayer(player));
                player.addLayer(new VestLayer<>(player));
                player.addLayer(new GearLayer<>(player));
            }
        }
        // Gilets pare-balles 3D aussi sur les zombies, squelettes, supports d'armure...
        for (var type : event.getEntityTypes()) {
            addVestLayer(event.getRenderer(type));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addVestLayer(Object renderer) {
        if (renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer lr && lr.getModel() instanceof HumanoidModel) {
            lr.addLayer(new VestLayer(lr));
            lr.addLayer(new GearLayer(lr));
        }
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
        // Armes blanches : 3 coups de combo différents, parade, pose à deux mains / de garde en 3e personne
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return entity.isUsingItem() && entity.getUseItem() == stack ? HumanoidModel.ArmPose.BLOCK : null;
            }

            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                   net.minecraft.client.player.LocalPlayer player,
                                                   net.minecraft.world.entity.HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProcess, float swingProcess) {
                MeleePoseHelper.apply(poseStack, player, arm, stack, partialTick, equipProcess, swingProcess);
                return true;
            }
        }, ModItems.ITEMS.getEntries().stream().map(h -> h.get())
                .filter(item -> item instanceof fr.modeminecraft.item.melee.MeleeWeaponItem).toArray(Item[]::new));
        // Pourfendeuse de Dragons : dégainage par-dessus l'épaule puis tenue à deux mains (3e personne)
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return hand == InteractionHand.MAIN_HAND && entity instanceof net.minecraft.world.entity.player.Player
                        ? GreatswordPose.pose() : null;
            }
        }, ModItems.DRAGONSLAYER.get());
        // Télécommande de drone : les deux mains la tiennent devant le torse pendant le pilotage
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return ControllerPose.pose();
            }
        }, ModItems.DRONE_CONTROLLER.get());
        // Grenades : dégoupillage avec l'autre bras, puis lancer en arc de cercle
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                   net.minecraft.client.player.LocalPlayer player,
                                                   net.minecraft.world.entity.HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProcess, float swingProcess) {
                GrenadePoseHelper.apply(poseStack, player, arm, stack, partialTick, equipProcess, swingProcess);
                return true;
            }
        }, ModItems.ITEMS.getEntries().stream().map(h -> h.get())
                .filter(item -> item instanceof fr.modeminecraft.item.explosive.GrenadeItem).toArray(Item[]::new));
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

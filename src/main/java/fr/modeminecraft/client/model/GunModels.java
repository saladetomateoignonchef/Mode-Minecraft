package fr.modeminecraft.client.model;

import com.mojang.math.Transformation;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.AttachmentSlot;
import fr.modeminecraft.item.gun.GunAnimations;
import fr.modeminecraft.item.gun.GunAttachments;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunSkin;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.QuadTransformers;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assemble le modèle 3D d'une arme selon son ItemStack : skin + chargeur + accessoires.
 *
 * Fonctionnement :
 * 1. Au chargement des ressources, on enregistre des modèles "en plus" (un par skin, chargeurs, accessoires).
 * 2. Le modèle de chaque arme est remplacé par un "emballage" dont les overrides regardent l'ItemStack
 *    et renvoient un modèle composé (mis en cache : il n'est construit qu'une fois par combinaison).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class GunModels {
    private static final Map<Key, BakedModel> CACHE = new HashMap<>();

    /** frame = image de l'animation de la pièce mobile (0 = au repos). */
    private record Key(String gun, GunSkin skin, GunAttachments attachments, boolean loaded, int frame) {}

    /** Nombre d'images de l'animation de recul (plus = plus fluide, mais plus de modèles en cache). */
    private static final int FRAMES = 8;

    private static ResourceLocation skinModel(String gun, GunSkin skin) {
        return ModeMinecraft.id("item/skins/" + gun + "_" + skin.getSerializedName());
    }

    private static ResourceLocation magModel(String gun, GunSkin skin) {
        return ModeMinecraft.id("item/skins/" + gun + "_" + skin.getSerializedName() + "_mag");
    }

    private static ResourceLocation slideModel(String gun, GunSkin skin) {
        return ModeMinecraft.id("item/skins/" + gun + "_" + skin.getSerializedName() + "_slide");
    }

    private static ResourceLocation partModel(Item attachment) {
        return ModeMinecraft.id("item/parts/" + BuiltInRegistries.ITEM.getKey(attachment).getPath());
    }

    private static List<GunItem> guns() {
        return ModItems.ITEMS.getEntries().stream().map(h -> h.get())
                .filter(i -> i instanceof GunItem).map(i -> (GunItem) i).toList();
    }

    private static String name(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (GunItem gun : guns()) {
            for (GunSkin skin : GunSkin.values()) {
                event.register(ModelResourceLocation.standalone(skinModel(name(gun), skin)));
                event.register(ModelResourceLocation.standalone(magModel(name(gun), skin)));
                event.register(ModelResourceLocation.standalone(slideModel(name(gun), skin)));
            }
        }
        ModItems.ITEMS.getEntries().stream().map(h -> h.get()).filter(i -> i instanceof AttachmentItem)
                .forEach(i -> event.register(ModelResourceLocation.standalone(partModel(i))));
    }

    /** On "emballe" le modèle de chaque arme pour qu'il dépende de l'ItemStack. */
    @SubscribeEvent
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        for (GunItem gun : guns()) {
            ModelResourceLocation key = ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(gun));
            BakedModel original = event.getModels().get(key);
            if (original != null) {
                event.getModels().put(key, new DynamicGunModel(original, name(gun)));
            }
        }
    }

    @SubscribeEvent
    public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
        CACHE.clear();
        GunMounts.clear();
    }

    private static BakedModel compose(Key key) {
        var models = Minecraft.getInstance().getModelManager();
        BakedModel skin = models.getModel(ModelResourceLocation.standalone(skinModel(key.gun, key.skin)));
        List<BakedQuad> extra = new ArrayList<>();

        // Chargeur : celui monté en accessoire, sinon celui d'origine (RPG-7 : la roquette, si chargée)
        var magazine = key.attachments.get(AttachmentSlot.MAGAZINE);
        if (magazine.isPresent()) {
            addPart(extra, magazine.get(), key.gun, AttachmentSlot.MAGAZINE);
        } else if (key.loaded) {
            addQuads(extra, models.getModel(ModelResourceLocation.standalone(magModel(key.gun, key.skin))), null);
        }
        for (AttachmentSlot slot : new AttachmentSlot[]{AttachmentSlot.SIGHT, AttachmentSlot.MUZZLE, AttachmentSlot.GRIP}) {
            key.attachments.get(slot).ifPresent(item -> addPart(extra, item, key.gun, slot));
        }
        // Pièce mobile (culasse, pompe, verrou, canons), déplacée selon l'image de l'animation
        GunMounts.Slide slide = GunMounts.slide(key.gun);
        if (slide != null) {
            Matrix4f m = new Matrix4f();
            if (slide.style().equals("spin")) {
                // Minigun : rotation des canons autour de leur axe (60° = symétrie des 6 canons)
                float angle = (float) Math.toRadians(60.0 * key.frame / FRAMES);
                m.translation(0, slide.pivotY() / 16f, slide.pivotZ() / 16f).rotateX(angle)
                        .translate(0, -slide.pivotY() / 16f, -slide.pivotZ() / 16f);
            } else {
                m.translation(-slide.travel() / 16f * key.frame / FRAMES, 0, 0); // recul vers l'arrière
            }
            addQuads(extra, models.getModel(ModelResourceLocation.standalone(slideModel(key.gun, key.skin))),
                    key.frame == 0 ? null : new Transformation(m));
        }
        return new ComposedModel(skin, List.copyOf(extra));
    }

    private static void addPart(List<BakedQuad> out, Item attachment, String gun, AttachmentSlot slot) {
        Vector3f mount = GunMounts.get(gun, slot);
        if (mount != null) {
            BakedModel part = Minecraft.getInstance().getModelManager()
                    .getModel(ModelResourceLocation.standalone(partModel(attachment)));
            addQuads(out, part, new Transformation(new Matrix4f().translation(mount)));
        }
    }

    private static void addQuads(List<BakedQuad> out, BakedModel model, @Nullable Transformation transform) {
        RandomSource random = RandomSource.create(42);
        List<BakedQuad> quads = new ArrayList<>(model.getQuads(null, null, random));
        for (Direction dir : Direction.values()) {
            quads.addAll(model.getQuads(null, dir, random));
        }
        for (BakedQuad quad : quads) {
            out.add(transform == null ? quad : QuadTransformers.applying(transform).process(quad));
        }
    }

    /** Image de l'animation de la pièce mobile pour cette arme, tenue par cette entité. */
    private static int animationFrame(String gun, ItemStack stack, @Nullable LivingEntity entity) {
        GunMounts.Slide slide = GunMounts.slide(gun);
        if (slide == null || entity == null || entity.level() == null) {
            return 0; // inventaire, sol, cadre : pas d'animation
        }
        boolean inHand = entity.getMainHandItem() == stack || entity.getOffhandItem() == stack;
        if (!inHand) {
            return 0;
        }
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        if (slide.style().equals("spin")) {
            // Les canons tournent tant que le joueur tient le clic (lancement + tir)
            if (!entity.isUsingItem() || entity.getUseItem() != stack) {
                return 0;
            }
            return (int) ((entity.level().getGameTime() + partial) * 3) % FRAMES;
        }
        if (entity != Minecraft.getInstance().player) {
            GunAnimations.observeAmmo(entity, stack);
        }
        float now = entity.level().getGameTime() + partial;
        float progress = GunAnimations.progress(slide.style(), GunAnimations.ticksSinceShot(entity, partial));
        Long rack = stack.get(ModDataComponents.RACK_AT.get());
        if (rack != null) {
            progress = Math.max(progress, GunAnimations.rackProgress(slide.style(), now - rack));
        }
        Long check = stack.get(ModDataComponents.CHECK_AT.get());
        if (check != null) {
            progress = Math.max(progress, GunAnimations.checkProgress(now - check));
        }
        // Pistolet vide : la culasse reste bloquée en arrière
        if (stack.getItem() instanceof GunItem held && held.stats().slideLock() && !GunItem.isChambered(stack)
                && GunItem.getAmmo(stack) == 0 && !(entity instanceof Player p && p.getAbilities().instabuild)) {
            progress = 1;
        }
        return Math.round(progress * FRAMES);
    }

    /** Modèle de l'item : choisit le modèle composé selon l'ItemStack. */
    private static class DynamicGunModel extends BakedModelWrapper<BakedModel> {
        private final ItemOverrides overrides;

        DynamicGunModel(BakedModel original, String gunName) {
            super(original);
            this.overrides = new ItemOverrides() {
                @Override
                public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                          @Nullable LivingEntity entity, int seed) {
                    // Le RPG-7 n'affiche sa roquette que s'il est chargé
                    boolean loaded = !GunMounts.magNeedsAmmo(gunName) || GunItem.getAmmo(stack) > 0;
                    Key key = new Key(gunName, GunItem.getSkin(stack), GunItem.getAttachments(stack), loaded,
                            animationFrame(gunName, stack, entity));
                    return CACHE.computeIfAbsent(key, GunModels::compose);
                }
            };
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }

    /**
     * Skin + pièces en plus. Dans l'inventaire (gui) et les cadres, on garde l'icône 2D du skin ;
     * ailleurs (en main, au sol), on ajoute les pièces 3D au modèle 3D.
     */
    private static class ComposedModel extends BakedModelWrapper<BakedModel> {
        private final List<BakedQuad> extra;
        private BakedModel lastBase;
        private BakedModel lastWithParts;

        ComposedModel(BakedModel skin, List<BakedQuad> extra) {
            super(skin);
            this.extra = extra;
        }

        @Override
        public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack, boolean leftHand) {
            BakedModel transformed = originalModel.applyTransform(context, poseStack, leftHand);
            if (extra.isEmpty() || context == net.minecraft.world.item.ItemDisplayContext.GUI
                    || context == net.minecraft.world.item.ItemDisplayContext.FIXED) {
                return transformed;
            }
            if (transformed != lastBase) {
                lastBase = transformed;
                lastWithParts = new WithParts(transformed, extra);
            }
            return lastWithParts;
        }
    }

    /** Un modèle déjà positionné, auquel on ajoute des quads. */
    private static class WithParts extends BakedModelWrapper<BakedModel> {
        private final List<BakedQuad> extra;

        WithParts(BakedModel base, List<BakedQuad> extra) {
            super(base);
            this.extra = extra;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable net.minecraft.world.level.block.state.BlockState state,
                                        @Nullable Direction side, RandomSource rand) {
            return side != null ? originalModel.getQuads(state, side, rand) : merge(originalModel.getQuads(state, null, rand));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable net.minecraft.world.level.block.state.BlockState state,
                                        @Nullable Direction side, RandomSource rand,
                                        net.neoforged.neoforge.client.model.data.ModelData data,
                                        @Nullable net.minecraft.client.renderer.RenderType renderType) {
            List<BakedQuad> base = originalModel.getQuads(state, side, rand, data, renderType);
            return side != null ? base : merge(base);
        }

        private List<BakedQuad> merge(List<BakedQuad> base) {
            List<BakedQuad> all = new ArrayList<>(base.size() + extra.size());
            all.addAll(base);
            all.addAll(extra);
            return all;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }

        @Override
        public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack, boolean leftHand) {
            return this; // déjà positionné
        }
    }

    private GunModels() {}
}

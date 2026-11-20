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
    /** magOffset = chargeur décalé vers le bas pendant le rechargement (pixels ; -1 = retiré). */
    private record Key(String gun, GunSkin skin, GunAttachments attachments, boolean loaded, int frame, int magOffset, int ammoVisual) {}

    /** Nombre d'images de l'animation de recul (plus = plus fluide, mais plus de modèles en cache). */
    private static final int FRAMES = 8;

    private static ResourceLocation skinModel(String gun, GunSkin skin) {
        return ModeMinecraft.id("item/skins/" + gun + "_" + skin.getSerializedName());
    }

    private static ResourceLocation magModel(String gun, GunSkin skin) {
        return ModeMinecraft.id("item/skins/" + gun + "_" + skin.getSerializedName() + "_mag");
    }

    /** Pièce d'origine remplaçable (poignée "grip", crosse "stock") pour un skin. */
    private static ResourceLocation defaultPartModel(String gun, GunSkin skin, String part) {
        return ModeMinecraft.id("item/skins/" + gun + "_" + skin.getSerializedName() + "_" + part);
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
                if (!skin.availableFor(name(gun))) {
                    continue;
                }
                event.register(ModelResourceLocation.standalone(skinModel(name(gun), skin)));
                event.register(ModelResourceLocation.standalone(magModel(name(gun), skin)));
                event.register(ModelResourceLocation.standalone(slideModel(name(gun), skin)));
                event.register(ModelResourceLocation.standalone(defaultPartModel(name(gun), skin, "grip")));
                event.register(ModelResourceLocation.standalone(defaultPartModel(name(gun), skin, "stock")));
            }
        }
        for (var h : ModItems.ITEMS.getEntries()) {                                    // pantalons et rangers, en pièces
            if (h.get() instanceof fr.modeminecraft.item.GearItem gear) {
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(gear).getPath();
                for (String p : fr.modeminecraft.client.GearLayer.parts(gear)) {
                    event.register(fr.modeminecraft.client.GearLayer.part(id, p));
                }
            }
        }
        for (var kind : fr.modeminecraft.entity.MountedGunEntity.Kind.values()) {   // tourelles, en pièces
            var geo = kind.geo();
            if (geo != null) {
                geo.parts().forEach(p -> event.register(fr.modeminecraft.client.MountedGunRenderer.part(kind.id, p)));
            }
        }
        for (var dt : fr.modeminecraft.entity.drone.DroneType.values()) {
            event.register(fr.modeminecraft.client.DroneRenderer.part(dt.id, "body"));
            event.register(fr.modeminecraft.client.DroneRenderer.part(dt.id, "rotor"));
        }
        for (var vt : fr.modeminecraft.entity.VehicleEntity.Type.values()) {             // véhicules : carrosserie + roue
            event.register(fr.modeminecraft.client.VehicleRenderer.part(vt.id, "body"));
            event.register(fr.modeminecraft.client.VehicleRenderer.part(vt.id, "wheel"));
        }
        event.register(ModelResourceLocation.standalone(ModeMinecraft.id("item/parts/p90_round")));
        event.register(ModelResourceLocation.standalone(ModeMinecraft.id("item/parts/belt_link")));
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
        Transformation magMove = key.magOffset > 0 ? new Transformation(new Matrix4f().translation(0, -key.magOffset / 16f, 0)) : null;
        if (key.magOffset < 0) {
            // rechargement : pas de chargeur dans l'arme pour l'instant
        } else if (magazine.isPresent()) {
            addPart(extra, magazine.get(), key.gun, AttachmentSlot.MAGAZINE, magMove);
        } else if (key.loaded) {
            addQuads(extra, models.getModel(ModelResourceLocation.standalone(magModel(key.gun, key.skin))), magMove);
        }
        for (AttachmentSlot slot : new AttachmentSlot[]{AttachmentSlot.SIGHT, AttachmentSlot.MUZZLE, AttachmentSlot.GRIP, AttachmentSlot.SIDE_RAIL}) {
            key.attachments.get(slot).ifPresent(item -> addPart(extra, item, key.gun, slot));
        }
        // P90 : les cartouches se voient à travers le chargeur et diminuent au fil des tirs
        if (key.gun.equals("p90") && magazine.isEmpty()) {
            BakedModel round = models.getModel(ModelResourceLocation.standalone(ModeMinecraft.id("item/parts/p90_round")));
            for (int i = 0; i < Math.min(10, key.ammoVisual); i++) {
                addQuads(extra, round, new Transformation(new Matrix4f().translation((0.4f + i * 1.0f) / 16f, 9.45f / 16f, 0.5f)));
            }
        }
        // M249 : bande de munitions qui avance d'un cran à chaque tir
        if (key.gun.equals("m249") && key.loaded) {
            BakedModel link = models.getModel(ModelResourceLocation.standalone(ModeMinecraft.id("item/parts/belt_link")));
            for (int i = 0; i < 4; i++) {
                float y = 6.4f + i * 0.7f + key.ammoVisual * 0.35f;
                addQuads(extra, link, new Transformation(new Matrix4f().translation(10.2f / 16f, y / 16f, 6.75f / 16f)));
            }
        }
        // Poignée pistolet et crosse : celles montées en accessoire, sinon celles d'origine
        String[][] replaceable = {{"PISTOL_GRIP", "grip"}, {"STOCK", "stock"}};
        for (String[] r : replaceable) {
            AttachmentSlot slot = AttachmentSlot.valueOf(r[0]);
            var mounted = key.attachments.get(slot);
            if (mounted.isPresent()) {
                addPart(extra, mounted.get(), key.gun, slot);
            } else {
                addQuads(extra, models.getModel(ModelResourceLocation.standalone(defaultPartModel(key.gun, key.skin, r[1]))), null);
            }
        }
        // Pièce mobile (culasse, pompe, verrou, canons), déplacée selon l'image de l'animation
        GunMounts.Slide slide = GunMounts.slide(key.gun);
        if (slide != null) {
            Matrix4f m = new Matrix4f();
            if (slide.style().equals("toggle")) {
                // Luger P08 : la genouillère se replie vers le haut autour de son axe arrière
                float angle = (float) Math.toRadians(slide.travel() * key.frame / FRAMES);
                m.translation(slide.pivotY() / 16f, slide.pivotZ() / 16f, 0).rotateZ(angle)
                        .translate(-slide.pivotY() / 16f, -slide.pivotZ() / 16f, 0);
            } else if (slide.style().equals("spin")) {
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
        addPart(out, attachment, gun, slot, null);
    }

    private static void addPart(List<BakedQuad> out, Item attachment, String gun, AttachmentSlot slot, @Nullable Transformation extraMove) {
        Vector3f mount = GunMounts.get(gun, slot);
        if (mount != null && extraMove != null) {
            mount = new Vector3f(mount).add(extraMove.getTranslation());
        }
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

    /** P90 : nombre de rangées de cartouches visibles dans le chargeur transparent ; M249 : position de la bande. */
    private static int ammoVisual(String gun, ItemStack stack) {
        return switch (gun) {
            case "p90" -> (int) Math.ceil(GunItem.getAmmo(stack) / 5.0);
            case "m249" -> GunItem.getAmmo(stack) % 2;
            default -> 0;
        };
    }

    /** Position du chargeur pendant le rechargement (seulement pour l'arme tenue en main). */
    private static int magOffset(ItemStack stack, @Nullable LivingEntity entity) {
        if (entity == null || entity.level() == null || entity.getMainHandItem() != stack) {
            return 0;
        }
        float now = entity.level().getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float off = GunAnimations.reloadMagOffset(stack, now);
        return Float.isNaN(off) ? -1 : Math.round(off);
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
            boolean firing = entity instanceof Player pl && (fr.modeminecraft.item.gun.TriggerHandler.isTriggerDown(pl)
                    || GunAnimations.ticksSinceShot(entity, partial) < 6);
            if (!firing) {
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
                    GunSkin shownSkin = GunItem.getSkin(stack).availableFor(gunName) ? GunItem.getSkin(stack) : GunSkin.DEFAULT;
                    Key key = new Key(gunName, shownSkin, GunItem.getAttachments(stack), loaded,
                            animationFrame(gunName, stack, entity), magOffset(stack, entity), ammoVisual(gunName, stack));
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

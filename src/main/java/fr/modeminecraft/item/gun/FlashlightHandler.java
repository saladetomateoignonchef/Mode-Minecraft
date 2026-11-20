package fr.modeminecraft.item.gun;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.network.DazzlePayload;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModEffects;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lampes tactiques, côté serveur :
 * - la pile se vide (1 seconde d'autonomie par seconde allumée, sauf en créatif) ;
 * - éclairage dynamique : des blocs de lumière invisibles ({@link Blocks#LIGHT}, sans collision) sont posés le long
 *   du faisceau (devant le bloc touché + des relais tous les 9 blocs) et retirés dès que le faisceau bouge.
 *   Ils ne remplacent que de l'air (ou de l'eau, en version immergée) ; leur liste est sauvegardée avec le monde
 *   pour qu'aucun ne reste après un arrêt brutal du serveur ;
 * - stroboscope : éblouit les créatures et les joueurs qui regardent la lampe (cône de 15°, 15 blocs) ;
 * - infrarouge : aucune lumière visible, mais révèle les créatures invisibles touchées par le faisceau.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class FlashlightHandler {
    /** Blocs de lumière posés par chaque joueur (dans son monde actuel). */
    private static final Map<UUID, Set<BlockPos>> PLACED = new HashMap<>();
    private static final Map<UUID, ServerLevel> LEVEL_OF = new HashMap<>();
    /** Distance entre deux relais de lumière le long du faisceau. */
    private static final double RELAY = 9.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ItemStack stack = player.getMainHandItem();
        LightMode mode = stack.getItem() instanceof GunItem ? GunItem.activeLightMode(stack) : LightMode.OFF;
        if (mode == LightMode.OFF || player.isSpectator() || player.hasEffect(ModEffects.EMP)) {
            clear(player);
            return;
        }
        AttachmentItem.Light light = GunItem.light(stack);
        long time = level.getGameTime();
        // --- La pile se vide ---
        if (!player.getAbilities().instabuild && time % 20 == 0) {
            int left = GunItem.lightBattery(stack) - 1;
            GunItem.setLightBattery(stack, left);
            if (left <= 0) {
                stack.set(ModDataComponents.LIGHT_MODE.get(), LightMode.OFF);
                player.displayClientMessage(Component.translatable("message.modeminecraft.light.empty").withStyle(ChatFormatting.RED), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLASHLIGHT_CLICK.get(), SoundSource.PLAYERS, 0.5f, 0.7f);
                clear(player);
                return;
            }
        }
        if (mode == LightMode.IR) {
            clear(player);                                   // l'infrarouge n'éclaire pas à l'œil nu
            if (time % 4 == 0) {
                reveal(player, level, light);
            }
            return;
        }
        // --- Éclairage dynamique (le stroboscope allume / éteint les blocs 5 fois par seconde) ---
        boolean strobeDark = mode == LightMode.STROBE && (time / 2) % 2 == 1;
        if (strobeDark) {
            clear(player);
        } else if (time % 2 == 0 || !PLACED.containsKey(player.getUUID())) {
            update(player, level, light, time);
        }
        if (mode == LightMode.STROBE && time % 4 == 0) {
            dazzle(player, level);
        }
    }

    // ------------------------------------------------------------------ blocs de lumière
    /** Bots de patrouille : leur lampe éclaire aussi (sans user de pile). */
    public static void updateBot(LivingEntity bot, ServerLevel level, AttachmentItem.Light light) {
        update(bot, level, light, level.getGameTime());
    }

    private static void update(LivingEntity player, ServerLevel level, AttachmentItem.Light light, long time) {
        if (LEVEL_OF.get(player.getUUID()) != null && LEVEL_OF.get(player.getUUID()) != level) {
            clear(player);                                   // changement de dimension
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        HitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(light.range())), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double dist = hit.getType() == HitResult.Type.MISS ? light.range() : hit.getLocation().distanceTo(eye);
        Map<BlockPos, Integer> want = new HashMap<>();
        int lvl = light.level();
        if (light.flicker() && (time * 7919 % 13) == 0) {
            lvl = Math.max(8, lvl - 3);                      // lampe de fortune : léger scintillement
        }
        if (hit instanceof net.minecraft.world.phys.BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            want.put(bh.getBlockPos().relative(bh.getDirection()), lvl);
        }
        for (double d = 4.5; d < dist - 2.5; d += RELAY) {
            want.put(BlockPos.containing(eye.add(look.scale(d))), Math.max(6, lvl - 2));
        }
        Set<BlockPos> old = PLACED.getOrDefault(player.getUUID(), Set.of());
        Set<BlockPos> now = new LinkedHashSet<>();
        Data data = Data.get(level);
        for (var e : want.entrySet()) {
            BlockPos pos = e.getKey();
            if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            boolean ours = old.contains(pos) || data.positions.contains(pos.asLong());
            if (state.is(Blocks.LIGHT)) {
                if (!ours) {
                    continue;                                // bloc de lumière posé par un joueur : on n'y touche pas
                }
                if (state.getValue(LightBlock.LEVEL) != e.getValue()) {
                    level.setBlock(pos, state.setValue(LightBlock.LEVEL, e.getValue()), Block.UPDATE_CLIENTS);
                }
            } else if (state.isAir()) {
                level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, e.getValue()), Block.UPDATE_CLIENTS);
            } else if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, e.getValue())
                        .setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_CLIENTS);
            } else {
                continue;
            }
            now.add(pos);
            data.add(pos);
        }
        for (BlockPos pos : old) {
            if (!now.contains(pos)) {
                remove(level, pos, data);
            }
        }
        PLACED.put(player.getUUID(), now);
        LEVEL_OF.put(player.getUUID(), level);
    }

    private static void remove(ServerLevel level, BlockPos pos, Data data) {
        if (level.isLoaded(pos)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.LIGHT)) {
                level.setBlock(pos, state.getValue(BlockStateProperties.WATERLOGGED)
                        ? Fluids.WATER.defaultFluidState().createLegacyBlock() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        data.remove(pos);
    }

    /** Retire toute la lumière posée par ce joueur. */
    public static void clear(LivingEntity player) {
        Set<BlockPos> set = PLACED.remove(player.getUUID());
        ServerLevel level = LEVEL_OF.remove(player.getUUID());
        if (set == null || level == null) {
            return;
        }
        Data data = Data.get(level);
        for (BlockPos pos : set) {
            remove(level, pos, data);
        }
    }

    // ------------------------------------------------------------------ stroboscope et infrarouge
    /** Créatures et joueurs dans le faisceau (15°, 15 blocs) qui regardent vers la lampe : éblouis. */
    private static void dazzle(ServerPlayer player, ServerLevel level) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double cos = Math.cos(Math.toRadians(15));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(15), x -> x != player && x.isAlive())) {
            Vec3 to = e.getEyePosition().subtract(eye);
            double d = to.length();
            if (d > 15 || d < 0.1 || to.normalize().dot(look) < cos) {
                continue;
            }
            Vec3 toShooter = eye.subtract(e.getEyePosition()).normalize();
            if (e.getLookAngle().dot(toShooter) <= 0.75 || !visible(level, player, eye, e.getEyePosition())) {
                continue;
            }
            if (e instanceof ServerPlayer target) {
                PacketDistributor.sendToPlayer(target, new DazzlePayload((float) (1 - 0.5 * d / 15)));
            } else {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
            }
        }
    }

    /** Infrarouge : les créatures invisibles (et les Endermen) touchées par le faisceau se mettent à briller. */
    private static void reveal(ServerPlayer player, ServerLevel level, AttachmentItem.Light light) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double cos = Math.cos(Math.toRadians(light.cone() + 2));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(light.range()),
                x -> x != player && x.isAlive() && (x.isInvisible() || x instanceof EnderMan))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > light.range() || to.normalize().dot(look) < cos || !visible(level, player, eye, e.getBoundingBox().getCenter())) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
        }
    }

    private static boolean visible(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    // ------------------------------------------------------------------ nettoyage
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            clear(p);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            clear(p);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            clear(p);
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            clear(p);
        }
        PLACED.clear();
        LEVEL_OF.clear();
        fr.modeminecraft.entity.drone.Jamming.clear();
    }

    /** Au démarrage : on retire les lumières restées en place (arrêt brutal du serveur). */
    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Data data = Data.get(level);
            if (data.positions.isEmpty()) {
                continue;
            }
            for (long l : new ArrayList<>(data.positions)) {
                BlockPos pos = BlockPos.of(l);
                remove(level, pos, data);
            }
            ModeMinecraft.LOGGER.info("Lampes tactiques : lumières orphelines retirées dans {}", level.dimension().location());
        }
    }

    /** Positions des lumières posées, sauvegardées avec le monde. */
    public static final class Data extends SavedData {
        final Set<Long> positions = new HashSet<>();

        static Data get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), "modeminecraft_flashlights");
        }

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            for (long l : tag.getLongArray("pos")) {
                d.positions.add(l);
            }
            return d;
        }

        void add(BlockPos pos) {
            if (positions.add(pos.asLong())) {
                setDirty();
            }
        }

        void remove(BlockPos pos) {
            if (positions.remove(pos.asLong())) {
                setDirty();
            }
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.put("pos", new LongArrayTag(new ArrayList<>(positions)));
            return tag;
        }
    }

    private FlashlightHandler() {}

    /** Lumières actuellement posées par ce joueur (tests). */
    public static List<BlockPos> placed(ServerPlayer p) {
        return new ArrayList<>(PLACED.getOrDefault(p.getUUID(), Set.of()));
    }
}

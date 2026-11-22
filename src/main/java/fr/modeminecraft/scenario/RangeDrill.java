package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entraînement au stand de tir : la cloche du stand lance un défi chronométré de 60 s. Des cibles surgissent au hasard
 * dans les couloirs de tir (et changent de place au bout de 3 s si on les rate) ; chaque cible touchée rapporte un point.
 * Record personnel conservé ; 15 cibles ou plus : +15 réputation (une fois par jour), 25 : mention « tireur d'élite ».
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class RangeDrill {
    private RangeDrill() {}

    public static final int DURATION = 1200, MOVE = 60;
    /** Position de la cloche (repère local du stand de tir, 32 x 16). */
    public static final int[] BELL = {4, 0, 14};
    public static final String TARGET = "modeminecraft_range_target";

    private static final class Drill {
        BlockPos origin;
        long end, placed;
        int score;
        UUID target;
    }

    private static final Map<UUID, Drill> ACTIVE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onBell(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !event.getLevel().getBlockState(event.getPos()).is(Blocks.BELL)) {
            return;
        }
        start(p, event.getPos());
    }

    /** Lance le défi si `bell` est la cloche du stand de tir de la base du joueur. */
    public static boolean start(ServerPlayer p, BlockPos bell) {
        Side side = Scenario.side(p);
        CityPlanner.City c = side == null ? null : Quarters.base(p);
        if (c == null || !ScenarioSites.at(c, side, ScenarioSites.Site.SHOOTING_RANGE, BELL).equals(bell) || ACTIVE.containsKey(p.getUUID())) {
            return false;
        }
        Drill d = new Drill();
        d.origin = ScenarioSites.at(c, side, ScenarioSites.Site.SHOOTING_RANGE, new int[]{0, 0, 0});
        d.end = p.level().getGameTime() + DURATION;
        ACTIVE.put(p.getUUID(), d);
        p.displayClientMessage(Component.translatable("message.modeminecraft.range.start").withStyle(ChatFormatting.GOLD), false);
        next(p.serverLevel(), p, d);
        return true;
    }

    /** Cible actuelle du défi de ce joueur (tests). */
    public static UUID target(ServerPlayer p) {
        Drill d = ACTIVE.get(p.getUUID());
        return d == null ? null : d.target;
    }

    public static int score(ServerPlayer p) {
        Drill d = ACTIVE.get(p.getUUID());
        return d == null ? -1 : d.score;
    }

    private static void next(ServerLevel level, ServerPlayer p, Drill d) {
        if (d.target != null) {
            Entity old = level.getEntity(d.target);
            if (old != null) {
                old.discard();
            }
        }
        net.minecraft.world.entity.monster.Husk t = EntityType.HUSK.create(level);
        if (t == null) {
            return;
        }
        int x = 8 + level.random.nextInt(20), z = 2 + level.random.nextInt(12);
        t.moveTo(d.origin.getX() + x + 0.5, d.origin.getY(), d.origin.getZ() + z + 0.5, 90, 0);
        t.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.TARGET));
        t.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        t.setDropChance(EquipmentSlot.HEAD, 0);
        t.setDropChance(EquipmentSlot.CHEST, 0);
        t.setNoAi(true);
        t.setSilent(true);
        t.setPersistenceRequired();
        t.setGlowingTag(true);
        t.addTag(TARGET);
        t.setCustomName(Component.literal("◎"));
        level.addFreshEntity(t);
        level.playSound(null, t.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8f, 1.4f);
        d.target = t.getUUID();
        d.placed = level.getGameTime();
    }

    @SubscribeEvent
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.LivingEntity a) || !a.getTags().contains(TARGET)) {
            return;
        }
        event.setCanceled(true);
        Entity src = event.getSource().getEntity();
        if (!(src instanceof ServerPlayer p)) {
            return;
        }
        Drill d = ACTIVE.get(p.getUUID());
        if (d == null || !a.getUUID().equals(d.target)) {
            return;
        }
        d.score++;
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.8f, 1.2f);
        next(p.serverLevel(), p, d);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        for (var it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
            Drill d = e.getValue();
            if (p == null || now >= d.end) {
                Entity t = d.target == null ? null : level.getEntity(d.target);
                if (t != null) {
                    t.discard();
                }
                it.remove();
                if (p != null) {
                    finish(p, d);
                }
                continue;
            }
            if (now - d.placed >= MOVE) {
                next(level, p, d);
            }
            if (now % 20 == 0) {
                long left = (d.end - now) / 20;
                p.displayClientMessage(Component.translatable("message.modeminecraft.range.hud", d.score, left / 60, String.format("%02d", left % 60))
                        .withStyle(ChatFormatting.YELLOW), true);
            }
        }
    }

    private static void finish(ServerPlayer p, Drill d) {
        var x = Scenario.record(p).extra;
        int best = x.getInt("RangeBest");
        if (d.score > best) {
            x.putInt("RangeBest", d.score);
        }
        p.sendSystemMessage(Component.translatable("message.modeminecraft.range.end", d.score, Math.max(best, d.score)).withStyle(ChatFormatting.GOLD));
        long day = p.level().getDayTime() / 24000;
        if (d.score >= 15 && x.getLong("RangeDay") != day + 1) {
            x.putLong("RangeDay", day + 1);
            Reputation.add(p, 15, "rep.modeminecraft.range");
        }
        if (d.score >= 25) {
            p.sendSystemMessage(Component.translatable("message.modeminecraft.range.marksman").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        }
        ScenarioData.get(p.serverLevel()).setDirty();
    }
}

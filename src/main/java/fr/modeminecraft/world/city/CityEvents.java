package fr.modeminecraft.world.city;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.CivilianLife;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.entity.city.PigeonEntity;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Micro-événements des quartiers (une scène à la fois près de chaque joueur, toutes les 1 à 2 minutes) :
 * contrôle d'identité, voleur poursuivi par la police, ouvriers de chantier, agent de propreté, musicien de rue qui
 * attire un petit public, artiste qui peint une fresque murale. Plus : nuées de pigeons sur les places le jour.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class CityEvents {
    private static final List<Scene> SCENES = new ArrayList<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final Block[] MURAL = {Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE,
            Blocks.LIGHT_BLUE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.CYAN_CONCRETE};

    /** Une scène en cours : ses acteurs, sa fin, et ce qu'elle fait à chaque tick. */
    static final class Scene {
        final String kind;
        final List<HumanBotEntity> actors = new ArrayList<>();
        final List<BlockPos> props = new ArrayList<>();
        final long end;
        BlockPos wall;
        Direction wallFace;
        int painted;
        Vec3 flee;

        Scene(String kind, long end) {
            this.kind = kind;
            this.end = end;
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        SCENES.removeIf(s -> {
            boolean over = now > s.end || s.actors.stream().noneMatch(HumanBotEntity::isAlive)
                    || level.getNearestPlayer(s.actors.get(0), 96) == null;
            if (over) {
                end(level, s);
            } else {
                tick(level, s, now);
            }
            return over;
        });
        if (now % 100 != 31) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            CityPlanner.City city = CityPresence.of(player);
            if (city == null || player.isSpectator()) {
                continue;
            }
            pigeons(level, player, city);
            if (city.variant().equals("metropolis") && now > NEXT.getOrDefault(player.getUUID(), 0L)
                    && !fr.modeminecraft.world.night.NightOps.isNight(level)) {
                NEXT.put(player.getUUID(), now + 1800 + level.getRandom().nextInt(1200));
                start(level, player);
            }
        }
    }

    // ------------------------------------------------------------------ pigeons
    private static void pigeons(ServerLevel level, ServerPlayer player, CityPlanner.City city) {
        if (fr.modeminecraft.world.night.NightOps.isNight(level)
                || level.getEntitiesOfClass(PigeonEntity.class, player.getBoundingBox().inflate(48)).size() >= 10) {
            return;
        }
        var r = level.getRandom();
        for (int tries = 0; tries < 6; tries++) {
            double a = r.nextDouble() * Math.PI * 2, d = 18 + r.nextDouble() * 22;
            int x = (int) (player.getX() + Math.cos(a) * d), z = (int) (player.getZ() + Math.sin(a) * d);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockState ground = level.getBlockState(new BlockPos(x, y - 1, z));
            if (Math.abs(y - city.baseY()) > 3 || !(ground.is(Blocks.SMOOTH_STONE) || ground.is(Blocks.POLISHED_ANDESITE) || ground.is(Blocks.GRAVEL)
                    || ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.STONE_BRICKS) || ground.is(Blocks.SMOOTH_SANDSTONE))) {
                continue;
            }
            boolean gulls = city.variant().equals("stilts") || level.getBlockState(new BlockPos(x, y - 1, z)).is(Blocks.SAND);
            int n = 3 + r.nextInt(4);
            for (int i = 0; i < n; i++) {
                PigeonEntity p = ModEntities.PIGEON.get().create(level);
                if (p == null) continue;
                p.moveTo(x + r.nextDouble() * 3 - 1.5, y, z + r.nextDouble() * 3 - 1.5, r.nextFloat() * 360, 0);
                p.setGull(gulls);
                level.addFreshEntity(p);
            }
            return;
        }
    }

    // ------------------------------------------------------------------ scènes
    private static BlockPos sidewalk(ServerLevel level, ServerPlayer player, int min, int max) {
        var r = level.getRandom();
        for (int tries = 0; tries < 20; tries++) {
            double a = r.nextDouble() * Math.PI * 2, d = min + r.nextDouble() * (max - min);
            int x = (int) (player.getX() + Math.cos(a) * d), z = (int) (player.getZ() + Math.sin(a) * d);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            BlockState g = level.getBlockState(p.below());
            if ((g.is(Blocks.SMOOTH_STONE) || g.is(Blocks.POLISHED_ANDESITE) || g.is(Blocks.STONE_BRICKS)) && level.getBlockState(p).isAir()
                    && level.getBlockState(p.above()).isAir()) {
                return p;
            }
        }
        return null;
    }

    private static HumanBotEntity actor(ServerLevel level, Scene s, BlockPos at, Faction f, BotRole role, double ox, double oz) {
        HumanBotEntity b = ModEntities.HUMAN_BOT.get().create(level);
        if (b == null) {
            return null;
        }
        b.moveTo(at.getX() + 0.5 + ox, at.getY(), at.getZ() + 0.5 + oz, level.getRandom().nextFloat() * 360, 0);
        b.setup(f, role, level.getRandom());
        b.convoy = true;                                   // disparaît quand plus personne n'est là
        b.eventUntil = s.end;
        b.home = at;
        level.addFreshEntity(b);
        s.actors.add(b);
        return b;
    }

    private static ItemStack dyed(net.minecraft.world.item.Item item, int color) {
        ItemStack st = new ItemStack(item);
        st.set(DataComponents.DYED_COLOR, new DyedItemColor(color, false));
        return st;
    }

    private static void start(ServerLevel level, ServerPlayer player) {
        BlockPos at = sidewalk(level, player, 20, 36);
        if (at == null) {
            return;
        }
        long now = level.getGameTime();
        String[] kinds = {"id_check", "thief", "workers", "sweeper", "musician", "muralist"};
        String kind = kinds[level.getRandom().nextInt(kinds.length)];
        Scene s = new Scene(kind, now + 2400);
        switch (kind) {
            case "id_check" -> {
                HumanBotEntity cop = actor(level, s, at, Faction.POLICE, BotRole.SOLDIER, 0, 0);
                HumanBotEntity civ = actor(level, s, at, Faction.CIVILIAN, BotRole.CIVILIAN, 1.6, 0);
                if (cop != null && civ != null) {
                    cop.setActivity(CivilianLife.CHATTING);
                    civ.setActivity(CivilianLife.HANDS_UP);
                }
            }
            case "thief" -> {
                HumanBotEntity thief = actor(level, s, at, Faction.CIVILIAN, BotRole.CIVILIAN, 0, 0);
                actor(level, s, at, Faction.POLICE, BotRole.SOLDIER, -5, 0);
                actor(level, s, at, Faction.POLICE, BotRole.SOLDIER, -6, 1);
                if (thief != null) {
                    thief.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(fr.modeminecraft.registry.ModItems.CASH_BUNDLE.get()));
                    Vec3 away = thief.position().subtract(player.position()).normalize().scale(45);
                    s.flee = thief.position().add(away.x, 0, away.z);
                }
                level.playSound(null, at, ModSounds.POLICE_FREEZE_WARNING.get(), SoundSource.HOSTILE, 2f, 1f);
            }
            case "workers" -> {
                for (int i = 0; i < 3; i++) {
                    HumanBotEntity w = actor(level, s, at, Faction.CIVILIAN, BotRole.CIVILIAN, i * 1.5 - 1.5, 0.5);
                    if (w != null) {
                        w.setItemSlot(EquipmentSlot.HEAD, dyed(Items.LEATHER_HELMET, 0xF2C511));        // casque de chantier jaune
                        w.setItemSlot(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, i % 2 == 0 ? 0xC6F01C : 0xFF7A12));  // gilet fluo
                        w.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(i == 1 ? Items.IRON_SHOVEL : Items.IRON_PICKAXE));
                        w.setDropChance(EquipmentSlot.HEAD, 0);
                        w.setDropChance(EquipmentSlot.CHEST, 0);
                    }
                }
                for (int[] c : new int[][]{{-3, -1}, {3, -1}, {-3, 2}, {3, 2}}) {        // cônes de signalisation
                    BlockPos p = at.offset(c[0], 0, c[1]);
                    if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isSolid()) {
                        level.setBlock(p, fr.modeminecraft.registry.ModBlocks.TRAFFIC_CONE.get().defaultBlockState(), 3);
                        s.props.add(p);
                    }
                }
                BlockPos sc = at.offset(0, 0, 3);
                for (int y = 0; y < 3 && level.getBlockState(sc.above(y)).isAir(); y++) {     // échafaudage
                    level.setBlock(sc.above(y), Blocks.SCAFFOLDING.defaultBlockState(), 3);
                    s.props.add(sc.above(y));
                }
            }
            case "sweeper" -> {
                HumanBotEntity w = actor(level, s, at, Faction.CIVILIAN, BotRole.CIVILIAN, 0, 0);
                if (w != null) {
                    w.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BRUSH));                // balai
                    w.setItemSlot(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, 0x2E7D32));     // tenue d'agent municipal
                }
            }
            case "musician" -> {
                HumanBotEntity m = actor(level, s, at, Faction.CIVILIAN, BotRole.CIVILIAN, 0, 0);
                if (m != null) {
                    m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(fr.modeminecraft.registry.ModItems.ACOUSTIC_GUITAR.get()));
                    for (int i = 0; i < 4; i++) {                                                         // petit public
                        double a = Math.PI * (0.25 + i * 0.17);
                        actor(level, s, at, Faction.CIVILIAN, BotRole.CIVILIAN, Math.cos(a) * 3.2, Math.sin(a) * 3.2);
                    }
                }
            }
            case "muralist" -> {
                // un mur plein, face au trottoir : la fresque (4 x 3) y sera peinte bloc par bloc
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    for (int k = 1; k <= 6 && s.wall == null; k++) {
                        BlockPos w = at.relative(d, k);
                        boolean ok = true;
                        for (int u = -1; u <= 2 && ok; u++) {
                            for (int v = 0; v < 3 && ok; v++) {
                                BlockState st = level.getBlockState(w.relative(d.getClockWise(), u).above(v));
                                ok = st.isCollisionShapeFullBlock(level, w) && !st.hasBlockEntity() && !st.is(Blocks.GLASS)
                                        && !(st.getBlock() instanceof net.minecraft.world.level.block.StainedGlassBlock);
                            }
                        }
                        if (ok) {
                            s.wall = w;
                            s.wallFace = d.getOpposite();
                        }
                    }
                }
                if (s.wall == null) {
                    return;
                }
                BlockPos stand = s.wall.relative(s.wallFace, 2);
                HumanBotEntity a = actor(level, s, stand, Faction.CIVILIAN, BotRole.CIVILIAN, 0, 0);
                if (a != null) {
                    a.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(fr.modeminecraft.registry.ModItems.SPRAY_CAN.get()));
                }
            }
            default -> { }
        }
        if (!s.actors.isEmpty()) {
            SCENES.add(s);
        }
    }

    private static void tick(ServerLevel level, Scene s, long now) {
        HumanBotEntity lead = s.actors.get(0);
        switch (s.kind) {
            case "id_check" -> {
                if (s.actors.size() > 1) {
                    lead.getLookControl().setLookAt(s.actors.get(1));
                    s.actors.get(1).getLookControl().setLookAt(lead);
                    lead.getNavigation().stop();
                    s.actors.get(1).getNavigation().stop();
                    if (now % 60 == 0) {
                        lead.swing(InteractionHand.MAIN_HAND);
                        level.playSound(null, lead.blockPosition(), ModSounds.RADIO_CRACKLE.get(), SoundSource.NEUTRAL, 0.6f, 1f);
                    }
                }
            }
            case "thief" -> {
                if (s.flee != null && now % 10 == 0) {
                    lead.setSprinting(true);
                    lead.getNavigation().moveTo(s.flee.x, s.flee.y, s.flee.z, 1.45);
                    for (int i = 1; i < s.actors.size(); i++) {
                        s.actors.get(i).setSprinting(true);
                        s.actors.get(i).getNavigation().moveTo(lead, 1.4);
                        s.actors.get(i).getLookControl().setLookAt(lead);
                    }
                    if (now % 80 == 0) {
                        level.playSound(null, s.actors.get(s.actors.size() - 1).blockPosition(), ModSounds.POLICE_FREEZE_WARNING.get(),
                                SoundSource.HOSTILE, 1.6f, 1.05f);
                    }
                }
            }
            case "workers", "sweeper" -> {
                if (now % 12 == 0) {
                    for (HumanBotEntity w : s.actors) {
                        w.getNavigation().stop();
                        w.swing(InteractionHand.MAIN_HAND);                                    // coups de pioche, de balai
                    }
                }
                if (s.kind.equals("sweeper") && now % 80 == 0) {
                    Vec3 next = lead.position().add(lead.getRandom().nextInt(9) - 4, 0, lead.getRandom().nextInt(9) - 4);
                    lead.getNavigation().moveTo(next.x, next.y, next.z, 0.5);
                }
                if (now % 15 == 0 && s.kind.equals("workers")) {
                    level.playSound(null, lead.blockPosition(), net.minecraft.sounds.SoundEvents.STONE_HIT, SoundSource.NEUTRAL, 0.6f, 0.8f);
                }
            }
            case "musician" -> {
                lead.getNavigation().stop();
                if (now % 192 == 0) {
                    level.playSound(null, lead.blockPosition(), ModSounds.STREET_GUITAR.get(), SoundSource.RECORDS, 1.4f, 1f);
                }
                if (now % 8 == 0) {
                    lead.swing(InteractionHand.MAIN_HAND);
                }
                for (int i = 1; i < s.actors.size(); i++) {                                   // le public écoute et applaudit
                    HumanBotEntity c = s.actors.get(i);
                    c.getLookControl().setLookAt(lead);
                    if (c.distanceTo(lead) > 4) {
                        c.getNavigation().moveTo(lead, 0.7);
                    } else {
                        c.getNavigation().stop();
                    }
                    if (now % 50 == i * 7) {
                        c.swing(InteractionHand.MAIN_HAND);
                    }
                }
            }
            case "muralist" -> {
                if (s.wall == null) {
                    return;
                }
                lead.getLookControl().setLookAt(Vec3.atCenterOf(s.wall));
                lead.getNavigation().stop();
                if (now % 30 == 0 && s.painted < 12) {
                    int u = s.painted % 4 - 1, v = s.painted / 4;
                    BlockPos p = s.wall.relative(s.wallFace.getClockWise(), u).above(v);
                    Block color = MURAL[(u + 1 + v * 3 + (int) (s.wall.asLong() & 7)) % MURAL.length];
                    level.setBlock(p, color.defaultBlockState(), 3);                            // la fresque prend forme
                    level.playSound(null, lead.blockPosition(), ModSounds.SPRAY_PAINT.get(), SoundSource.NEUTRAL, 0.8f, 1f);
                    level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                            color.defaultBlockState()), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
                    lead.swing(InteractionHand.MAIN_HAND);
                    s.painted++;
                }
            }
            default -> { }
        }
    }

    private static void end(ServerLevel level, Scene s) {
        for (BlockPos p : s.props) {                       // on range les cônes et l'échafaudage
            if (level.isLoaded(p)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        for (HumanBotEntity a : s.actors) {
            a.eventUntil = 0;
            a.setActivity(CivilianLife.NONE);
            a.setSprinting(false);
        }
    }

    private CityEvents() {}
}

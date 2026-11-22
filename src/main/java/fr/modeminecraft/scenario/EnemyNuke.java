package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.military.AirAI;
import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.nuclear.BombEntity;
import fr.modeminecraft.nuclear.BombItem;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Pression ennemie dynamique : chaque camp mène son propre programme nucléaire. Tant qu'un joueur du camp adverse est
 * en jeu, il progresse de 1 % toutes les 2 minutes (≈ 10 jours de jeu), freiné par les missions stratégiques réussies
 * (espionnage, convoi, sous-marin, scientifique, SEAD) et par les interrogatoires de prisonniers. À 100 %, un bombardier
 * stratégique (Tu-95 soviétique ou B-2 américain) décolle de la base ennemie avec une bombe nucléaire : le joueur a
 * 1 jour de jeu pour l'intercepter. Il tourne d'abord au-dessus de sa base, puis part de façon à frapper la base du joueur
 * à l'échéance. Abattu : programme ramené à 40 % et +300 réputation ; sinon la base est rasée (-150 réputation) et le
 * programme repart de zéro.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class EnemyNuke {
    private EnemyNuke() {}

    public static final int STEP_TICKS = 2400, WINDOW = 24000;
    private static final int[] WARNINGS = {25, 50, 75, 90};

    private static CompoundTag prog(ServerLevel level, Side attacker) {
        ScenarioData d = ScenarioData.get(level);
        CompoundTag t = d.global.getCompound("Enemy_" + attacker.id);
        d.global.put("Enemy_" + attacker.id, t);
        return t;
    }

    /** Avancement (0-100) du programme du camp `attacker`. */
    public static int progress(ServerLevel level, Side attacker) {
        return prog(level, attacker).getInt("Progress");
    }

    /** Recul du programme de l'ennemi de `victim` (missions réussies, interrogatoires). */
    public static void setback(ServerPlayer p, int pct) {
        Side side = Scenario.side(p);
        if (side == null) {
            return;
        }
        CompoundTag t = prog(p.serverLevel(), side.opposite());
        if (t.contains("Bomber")) {
            return;                                                          // le bombardier est déjà en l'air
        }
        int before = t.getInt("Progress");
        t.putInt("Progress", Math.max(0, before - pct));
        ScenarioData.get(p.serverLevel()).setDirty();
        if (before > 0) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.enemy.setback", pct, t.getInt("Progress"))
                    .withStyle(ChatFormatting.AQUA), false);
        }
    }

    private static List<ServerPlayer> targets(ServerLevel level, Side victim) {
        return level.getServer().getPlayerList().getPlayers().stream().filter(p -> Scenario.side(p) == victim).toList();
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        if (now % 20 != 0 || !level.getGameRules().getBoolean(Scenario.RULE) || ScenarioData.get(level).global.contains("Winner")) {
            return;
        }
        for (Side attacker : Side.values()) {
            Side victim = attacker.opposite();
            List<ServerPlayer> ps = targets(level, victim);
            CompoundTag t = prog(level, attacker);
            if (t.contains("Bomber")) {
                bomberTick(level, attacker, t, ps, now);
            } else if (!ps.isEmpty()) {
                if (now % STEP_TICKS == 0) {
                    int p = Math.min(100, t.getInt("Progress") + 1);
                    t.putInt("Progress", p);
                    ScenarioData.get(level).setDirty();
                    for (int w : WARNINGS) {
                        if (p == w) {
                            ps.forEach(pl -> Briefings.radio(pl, "enemy_" + w));
                        }
                    }
                    ps.forEach(MissionNet::sync);
                }
                if (t.getInt("Progress") >= 100) {
                    launch(level, attacker, t, ps.get(0), now);
                }
            }
            if (now % 100 == 0) {
                ps.forEach(MissionNet::sync);
            }
        }
    }

    /** Décollage du bombardier nucléaire ennemi. */
    static void launch(ServerLevel level, Side attacker, CompoundTag t, ServerPlayer victim, long now) {
        CityPlanner.City home = Quarters.base(victim);
        BlockPos target = home != null ? new BlockPos(home.middleX(), home.baseY(), home.middleZ()) : victim.blockPosition();
        CityPlanner.City enemy = Scenario.findBase(level, attacker, target);
        Vec3 from = enemy != null ? new Vec3(enemy.middleX(), enemy.baseY() + 60, enemy.middleZ())
                : Vec3.atCenterOf(target).add(1800, 0, 0);
        MilType type = attacker == Side.SOVIET ? MilType.TU_95 : MilType.B_2;
        MilVehicleEntity v = AirAI.spawnPlane(level, type, from, attacker.ally, null, Vec3.atCenterOf(target));
        double dist = Math.hypot(target.getX() - from.x, target.getZ() - from.z);
        long flight = (long) (dist / Math.max(0.4, type.maxSpeed() * 0.6)) + 600;
        long deadline = now + WINDOW;
        t.putUUID("Bomber", v.getUUID());
        t.putLong("Deadline", deadline);
        t.putLong("Depart", Math.max(now + 1200, deadline - flight));
        t.putLong("Target", target.asLong());
        t.putLong("Home", BlockPos.containing(from).asLong());
        t.putInt("BX", v.getBlockX());
        t.putInt("BZ", v.getBlockZ());
        ScenarioData.get(level).setDirty();
        for (ServerPlayer p : targets(level, attacker.opposite())) {
            Briefings.radio(p, "bomber_launch");
            Cinematics.play(p, "bomber", v.getBlockX(), v.getBlockZ(), target.getX(), target.getZ());
            MissionNet.sync(p);
        }
        ModeMinecraft.LOGGER.info("Bombardier nucléaire {} ({}) en vol vers {}", attacker.id, type.id, target);
    }

    private static void bomberTick(ServerLevel level, Side attacker, CompoundTag t, List<ServerPlayer> ps, long now) {
        Entity e = level.getEntity(t.getUUID("Bomber"));
        BlockPos target = BlockPos.of(t.getLong("Target"));
        if (e instanceof MilVehicleEntity v && v.isAlive() && !v.is(MilVehicleEntity.WRECK)) {
            v.holdChunks();
            t.putInt("BX", v.getBlockX());
            t.putInt("BZ", v.getBlockZ());
            boolean depart = now >= t.getLong("Depart");
            AirAI.assign(v, Vec3.atCenterOf(depart ? target : BlockPos.of(t.getLong("Home"))), null);
            double flat = Math.hypot(v.getX() - target.getX() - 0.5, v.getZ() - target.getZ() - 0.5);
            if (depart && flat < 170 && !t.getBoolean("Dropped")) {
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, target.getX(), target.getZ());
                BombEntity bomb = new BombEntity(level, BombItem.Kind.NUKE, null, v, v.position().add(0, -3, 0), v.getDeltaMovement().scale(0.5));
                bomb.guideTo(new Vec3(target.getX() + 0.5, ground, target.getZ() + 0.5));
                level.addFreshEntity(bomb);
                t.putBoolean("Dropped", true);
                t.putLong("DropAt", now);
                ScenarioData.get(level).setDirty();
            }
            if (t.getBoolean("Dropped") && now - t.getLong("DropAt") > 200) {
                struck(level, attacker, t, ps, false, v);
            } else if (now >= t.getLong("Deadline")) {
                struck(level, attacker, t, ps, true, v);
            }
        } else if (e == null && now >= t.getLong("Deadline")) {
            // bombardier hors zone chargée à l'échéance : la frappe a lieu quand même
            struck(level, attacker, t, ps, true, null);
        }
    }

    private static void struck(ServerLevel level, Side attacker, CompoundTag t, List<ServerPlayer> ps, boolean direct, MilVehicleEntity v) {
        BlockPos target = BlockPos.of(t.getLong("Target"));
        if (direct) {
            level.getChunk(target.getX() >> 4, target.getZ() >> 4);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, target.getX(), target.getZ());
            fr.modeminecraft.nuclear.NuclearExplosion.start(level, new BlockPos(target.getX(), ground, target.getZ()), v);
        }
        if (v != null) {
            v.getPassengers().forEach(Entity::discard);
            v.discard();
        }
        clear(level, attacker, 0);
        for (ServerPlayer p : ps) {
            Briefings.radio(p, "base_struck");
            Reputation.add(p, -150, "rep.modeminecraft.base_struck");
            Cinematics.play(p, "struck", target.getX(), target.getZ(), 0, 0);
        }
    }

    private static void clear(ServerLevel level, Side attacker, int progress) {
        CompoundTag t = new CompoundTag();
        t.putInt("Progress", progress);
        ScenarioData.get(level).global.put("Enemy_" + attacker.id, t);
        ScenarioData.get(level).setDirty();
    }

    /** Un appareil détruit : est-ce un bombardier nucléaire ennemi ? */
    public static void onLost(MilVehicleEntity v, Entity by) {
        if (!(v.level() instanceof ServerLevel level)) {
            return;
        }
        for (Side attacker : Side.values()) {
            CompoundTag t = prog(level, attacker);
            if (t.hasUUID("Bomber") && t.getUUID("Bomber").equals(v.getUUID())) {
                boolean dropped = t.getBoolean("Dropped");
                if (dropped) {
                    return;                                                  // trop tard : la bombe tombe déjà
                }
                clear(level, attacker, 40);
                Entity killer = by instanceof net.minecraft.world.entity.projectile.Projectile pr ? pr.getOwner() : by;
                if (killer != null && killer.getControllingPassenger() instanceof ServerPlayer pilot) {
                    killer = pilot;
                }
                for (ServerPlayer p : targets(level, attacker.opposite())) {
                    Briefings.radio(p, "bomber_down");
                    Cinematics.play(p, "intercepted", v.getBlockX(), v.getBlockZ(), 0, 0);
                    Reputation.add(p, p == killer ? 300 : 100, "rep.modeminecraft.bomber_intercepted");
                }
                return;
            }
        }
    }

    /** État pour le HUD du joueur (avancement, bombardier). */
    static CompoundTag hud(ServerPlayer p) {
        CompoundTag out = new CompoundTag();
        Side side = Scenario.side(p);
        if (side == null) {
            return out;
        }
        CompoundTag t = prog(p.serverLevel(), side.opposite());
        out.putInt("Progress", t.getInt("Progress"));
        if (t.contains("Bomber")) {
            out.putBoolean("Bomber", true);
            out.putInt("BX", t.getInt("BX"));
            out.putInt("BZ", t.getInt("BZ"));
            out.putLong("Left", Math.max(0, t.getLong("Deadline") - p.level().getGameTime()));
            out.putBoolean("Depart", p.level().getGameTime() >= t.getLong("Depart"));
        }
        return out;
    }
}

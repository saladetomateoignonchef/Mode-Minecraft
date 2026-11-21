package fr.modeminecraft.entity.military;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.AirdropEntity;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Idées du QCM :
 * - chars ennemis pilotés par l'IA : des T-80, T-90, BMP, BTR soviétiques (grand Nord), des Abrams, Bradley et Stryker
 *   de la garnison (forêts) ou des blindés de milice patrouillent et attaquent au canon ;
 * - grandes batailles OTAN contre URSS : événement rare, une vraie bataille de chars et d'infanterie éclate non loin
 *   du joueur (les deux camps avancent l'un vers l'autre et s'affrontent sans limite d'escarmouche).
 */
public final class ArmorPatrols {
    private static final MilType[] SOVIET = {MilType.T_80BVM, MilType.T_90M, MilType.BMP_2, MilType.BTR_80, MilType.BRDM_2, MilType.ZSU_23_4,
            MilType.T_14_ARMATA, MilType.BMPT_TERMINATOR};
    private static final MilType[] NATO = {MilType.M1A2_ABRAMS, MilType.M2A3_BRADLEY, MilType.STRYKER, MilType.HUMVEE, MilType.M_ATV};
    private static final MilType[] MILITIA = {MilType.BTR_80, MilType.BRDM_2, MilType.BMP_2, MilType.TIGR};

    /** Une patrouille blindée (1 ou 2 véhicules) traverse la région près du joueur. */
    public static void spawnPatrol(ServerLevel level, Vec3 near) {
        spawnPatrol(level, near, 100);
    }

    public static void spawnPatrol(ServerLevel level, Vec3 near, double dist) {
        var r = level.getRandom();
        double a = r.nextDouble() * Math.PI * 2;
        Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
        BlockPos start = surface(level, near.add(dir.scale(dist)));
        BlockPos dest = surface(level, near.subtract(dir.scale(160)).add(-dir.z * 25, 0, dir.x * 25));
        if (!level.isLoaded(start) || !level.getFluidState(start.below()).isEmpty()) {
            return;
        }
        Faction local = AirdropEntity.localFaction(level, start);
        Faction f = local == Faction.SOVIET ? Faction.SOVIET : local == Faction.GARRISON ? Faction.GARRISON : Faction.MILITIA;
        MilType[] pool = f == Faction.SOVIET ? SOVIET : f == Faction.GARRISON ? NATO : MILITIA;
        float yaw = (float) (Mth.atan2(dest.getZ() - start.getZ(), dest.getX() - start.getX()) * Mth.RAD_TO_DEG) - 90;
        MilVehicleEntity lead = vehicle(level, pool[r.nextInt(pool.length)], start, yaw, f, false);
        lead.destination = dest;
        if (r.nextFloat() < 0.5f) {
            MilVehicleEntity escort = vehicle(level, pool[r.nextInt(pool.length)], surface(level, Vec3.atCenterOf(start).add(dir.scale(14))), yaw, f, false);
            escort.leader = lead.getUUID();
        }
        ModeMinecraft.LOGGER.info("Patrouille blindée ({}, {}) de {} vers {}", f.id, lead.type().id, start, dest);
    }

    /** Grande bataille OTAN contre URSS, à 150-250 blocs du joueur. */
    public static void startBattle(ServerLevel level, Vec3 near) {
        startBattle(level, near, 150 + level.getRandom().nextInt(100), 70);
    }

    public static void startBattle(ServerLevel level, Vec3 near, double dist, double spread) {
        var r = level.getRandom();
        double a = r.nextDouble() * Math.PI * 2;
        Vec3 center = near.add(Math.cos(a) * dist, 0, Math.sin(a) * dist);
        Vec3 axis = new Vec3(Math.cos(a + Math.PI / 2), 0, Math.sin(a + Math.PI / 2));
        Vec3 sov = center.add(axis.scale(-spread)), nato = center.add(axis.scale(spread));
        if (!level.isLoaded(BlockPos.containing(sov)) || !level.isLoaded(BlockPos.containing(nato))) {
            return;
        }
        float yawToNato = (float) (Mth.atan2(nato.z - sov.z, nato.x - sov.x) * Mth.RAD_TO_DEG) - 90;
        side(level, sov, nato, yawToNato, Faction.SOVIET, new MilType[]{r.nextBoolean() ? MilType.T_90M : MilType.T_80BVM, MilType.BMP_2,
                r.nextFloat() < 0.4f ? MilType.T_14_ARMATA : MilType.BTR_80}, axis);
        side(level, nato, sov, yawToNato + 180, Faction.GARRISON, new MilType[]{MilType.M1A2_ABRAMS, MilType.M2A3_BRADLEY,
                r.nextBoolean() ? MilType.M1A2_ABRAMS : MilType.STRYKER}, axis);
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceTo(center) < 500) {
                int d = (int) p.position().distanceTo(center);
                p.displayClientMessage(Component.translatable("message.modeminecraft.battle", d, compass(center.subtract(p.position())))
                        .withStyle(ChatFormatting.GOLD), false);
            }
        }
        fr.modeminecraft.entity.bot.CivilianLife.panic(level, center, 200, 1200);
        ModeMinecraft.LOGGER.info("Bataille OTAN / URSS autour de {}", BlockPos.containing(center));
    }

    private static void side(ServerLevel level, Vec3 at, Vec3 enemy, float yaw, Faction f, MilType[] types, Vec3 axis) {
        var r = level.getRandom();
        for (int i = 0; i < types.length; i++) {
            Vec3 p = at.add(axis.scale((i - 1) * 4)).add(axis.z * (i - 1) * 16, 0, -axis.x * (i - 1) * 16);
            MilVehicleEntity v = vehicle(level, types[i], surface(level, p), yaw, f, true);
            v.destination = surface(level, enemy.add((r.nextDouble() - 0.5) * 30, 0, (r.nextDouble() - 0.5) * 30));
        }
        for (int i = 0; i < 6; i++) {
            Vec3 p = at.add((r.nextDouble() - 0.5) * 24, 0, (r.nextDouble() - 0.5) * 24);
            HumanBotEntity b = bot(level, surface(level, p), f, i == 0 ? BotRole.OFFICER : i == 5 ? BotRole.SNIPER : BotRole.SOLDIER);
            b.battle = true;
            b.getNavigation().moveTo(enemy.x, enemy.y, enemy.z, 0.8);
        }
    }

    private static MilVehicleEntity vehicle(ServerLevel level, MilType t, BlockPos pos, float yaw, Faction f, boolean battle) {
        MilVehicleEntity v = MilVehicleEntity.create(level, t, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, yaw);
        v.fillAmmo(1f);
        v.setFuel(1f);
        v.patrol = true;
        level.addFreshEntity(v);
        for (int s = 0; s < Math.min(3, t.seats.size()); s++) {
            MilType.Role role = t.seats.get(s).role();
            if (s > 0 && role == MilType.Role.PASSENGER) {
                break;
            }
            HumanBotEntity b = bot(level, pos, f, s == 0 ? BotRole.OFFICER : role == MilType.Role.GUNNER ? BotRole.GUNNER : BotRole.SOLDIER);
            b.battle = battle;
            b.startRiding(v, true);
        }
        return v;
    }

    private static HumanBotEntity bot(ServerLevel level, BlockPos pos, Faction f, BotRole role) {
        HumanBotEntity b = ModEntities.HUMAN_BOT.get().create(level);
        b.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
        b.setup(f, role, level.getRandom());
        b.convoy = true;
        b.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        level.addFreshEntity(b);
        return b;
    }

    private static BlockPos surface(ServerLevel level, Vec3 v) {
        int x = (int) Math.floor(v.x), z = (int) Math.floor(v.z);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    private static Component compass(Vec3 d) {
        String[] dirs = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
        int i = Math.floorMod(Math.round((float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG) / 45f), 8);
        return Component.translatable("direction.modeminecraft." + dirs[i]);
    }

    private ArmorPatrols() {}
}

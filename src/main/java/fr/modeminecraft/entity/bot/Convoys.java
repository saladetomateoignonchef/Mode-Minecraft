package fr.modeminecraft.entity.bot;

import fr.modeminecraft.entity.AirdropEntity;
import fr.modeminecraft.entity.MountedGunEntity;
import fr.modeminecraft.entity.VehicleEntity;
import fr.modeminecraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Convois de patrouille blindés : un camion blindé (mitrailleuse M2 sur le toit) suivi d'une jeep — ou deux voitures
 * de police sirène hurlante — qui traversent la région en passant près du joueur. S'ils repèrent un ennemi,
 * les véhicules s'arrêtent et l'équipage descend combattre.
 */
public final class Convoys {
    public static void spawn(ServerLevel level, Vec3 near) {
        var r = level.getRandom();
        double a = r.nextDouble() * Math.PI * 2;
        Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
        Vec3 perp = new Vec3(-dir.z, 0, dir.x);
        BlockPos start = surface(level, near.add(dir.scale(80)));
        BlockPos dest = surface(level, near.subtract(dir.scale(150)).add(perp.scale(12)));
        if (!level.isLoaded(start)) {
            return;
        }
        Faction f = AirdropEntity.localFaction(level, start);
        boolean police = f == Faction.SWAT;
        if (police) {
            f = Faction.POLICE;
        }
        float yaw = (float) (Mth.atan2(dest.getZ() - start.getZ(), dest.getX() - start.getX()) * Mth.RAD_TO_DEG) - 90;
        VehicleEntity lead = vehicle(level, police ? VehicleEntity.Type.POLICE_CAR : VehicleEntity.Type.ARMORED_TRUCK, start, yaw, f, police ? 2 : 3);
        lead.destination = dest;
        if (!police) {
            MountedGunEntity gun = new MountedGunEntity(level, MountedGunEntity.Kind.M2, lead.getX(), lead.getY() + 2.5, lead.getZ(), yaw, MountedGunEntity.Kind.M2.capacity);
            gun.deployNow();
            level.addFreshEntity(gun);
            gun.startRiding(lead, true);
            HumanBotEntity gunner = bot(level, start, f, BotRole.GUNNER);
            gunner.startRiding(gun, true);
        } else {
            lead.toggleSiren();
        }
        BlockPos back = surface(level, Vec3.atCenterOf(start).add(dir.scale(10)));
        VehicleEntity follow = vehicle(level, police ? VehicleEntity.Type.POLICE_CAR : VehicleEntity.Type.JEEP, back, yaw, f, 3);
        follow.leader = lead.getUUID();
        if (police) {
            follow.toggleSiren();
        }
        fr.modeminecraft.ModeMinecraft.LOGGER.info("Convoi ({}) en route de {} vers {}", f.id, start, dest);
    }

    private static VehicleEntity vehicle(ServerLevel level, VehicleEntity.Type type, BlockPos pos, float yaw, Faction f, int crew) {
        VehicleEntity v = new VehicleEntity(level, type, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, yaw);
        v.convoy = true;
        level.addFreshEntity(v);
        HumanBotEntity driver = bot(level, pos, f, BotRole.OFFICER);
        driver.startRiding(v, true);
        for (int i = 1; i < crew; i++) {
            bot(level, pos, f, BotRole.SOLDIER).startRiding(v, true);
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

    private Convoys() {}
}

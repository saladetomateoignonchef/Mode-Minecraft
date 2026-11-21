package fr.modeminecraft.entity.military;

import fr.modeminecraft.network.RadarPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Complexes lance-missiles stratégiques (cahier des charges 0.4, §4) :
 * - S-400 Triumf : radar panoramique de 600 blocs (drones, hélicoptères, avions de largage, missiles), verrouillage de
 *   4 cibles, lancement vertical « à froid » (éjection à 25 blocs puis virage) ; 48N6E3 longue portée (fragmentation de
 *   proximité) ou 9M96E2 très agile (petits drones, missiles en vol) ;
 * - S-500 Prometey : 800 blocs, interception des missiles balistiques (77N6-N, impact direct en haute altitude) ou des
 *   cibles aériennes en limite de portée (40N6M) ; mode Sentinelle : tire seul sur tout missile qui approche ;
 * - 9K720 Iskander : frappe sur coordonnées (X, Z) ou sur la marque du désignateur laser, jusqu'à 1000 blocs.
 * Il faut d'abord déployer le véhicule (H : béquilles + relèvement des tubes), puis l'opérateur ouvre la console de tir
 * (clic droit). Les bases militaires secrètes ont leur S-400 en défense automatique.
 */
public final class StrategicSystems {
    public static final byte HELI = 0, DRONE = 1, MISSILE = 2, PLANE = 3, BALLISTIC = 4;

    public static double range(MilType t) {
        return t == MilType.S_500 ? 800 : t == MilType.S_400 ? 600 : 1000;
    }

    /** Contacts radar (positions relatives) : entités aériennes chargées + frappes balistiques simulées. */
    public static List<RadarPayload.Blip> scan(MilVehicleEntity v) {
        List<RadarPayload.Blip> out = new ArrayList<>();
        double r = range(v.type());
        long now = v.level().getGameTime();
        for (Entity e : v.level().getEntities(v, new AABB(v.position(), v.position()).inflate(r, 400, r), MilVehicleEntity::isAircraft)) {
            if (v.hasPassenger(e) || e instanceof ShellEntity s && (s.visual() || s.getOwner() != null && v.hasPassenger(s.getOwner()))) {
                continue;
            }
            byte kind = e instanceof MilVehicleEntity ? HELI : e instanceof fr.modeminecraft.entity.drone.DroneEntity ? DRONE
                    : e instanceof fr.modeminecraft.entity.AirdropEntity ? PLANE : MISSILE;
            out.add(new RadarPayload.Blip(e.getId(), (float) (e.getX() - v.getX()), (float) (e.getZ() - v.getZ()), (float) (e.getY() - v.getY()), kind));
        }
        List<Strikes.Strike> strikes = Strikes.active();
        for (int i = 0; i < strikes.size(); i++) {
            Vec3 p = strikes.get(i).position(now);
            if (Math.hypot(p.x - v.getX(), p.z - v.getZ()) < r) {
                out.add(new RadarPayload.Blip(-(i + 1), (float) (p.x - v.getX()), (float) (p.z - v.getZ()), (float) (p.y - v.getY()), BALLISTIC));
            }
        }
        return out;
    }

    /** Toutes les 10 ticks : contacts envoyés à l'opérateur ; sentinelle du S-500 ; défense automatique des bases. */
    static void tick(MilVehicleEntity v) {
        if (!(v.level() instanceof ServerLevel sl) || v.tickCount % 10 != 0) {
            return;
        }
        MilType t = v.type();
        if (t != MilType.S_400 && t != MilType.S_500) {
            return;
        }
        Entity op = v.occupant(1);
        if (op instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new RadarPayload(v.getId(), v.is(MilVehicleEntity.DEPLOYED), v.sentinel, scan(v)));
        }
        if (!v.is(MilVehicleEntity.DEPLOYED) || v.is(MilVehicleEntity.ENGINE_DOWN)) {
            return;
        }
        // sentinelle automatique : missiles balistiques qui approchent
        if (v.sentinel || v.autoDefense) {
            long now = sl.getGameTime();
            for (Strikes.Strike s : List.copyOf(Strikes.active())) {
                // une batterie n'abat pas les frappes de son propre camp
                if (s.owner != null && (s.owner.getUUID().equals(v.ownerId) || v.autoDefense && !(s.owner instanceof Player))) {
                    continue;
                }
                Vec3 p = s.position(now);
                if (!s.intercepting && Math.hypot(p.x - v.getX(), p.z - v.getZ()) < range(t) * 0.7 && s.impact - now > 30
                        && Math.hypot(s.target.x - v.getX(), s.target.z - v.getZ()) < range(t)) {
                    MilAmmo a = pick(v, true);
                    if (a != null && v.consume(a)) {
                        launchInterceptor(sl, v, null);
                        Strikes.intercept(sl, s, v.position(), now + 25);
                    }
                }
            }
        }
        // défense aérienne automatique (S-400 des bases) : hélicoptères et drones des joueurs
        if (v.autoDefense && v.tickCount % 60 == 0) {
            for (Entity e : sl.getEntities(v, v.getBoundingBox().inflate(300, 200, 300), MilVehicleEntity::isAircraft)) {
                boolean hostile = e instanceof MilVehicleEntity mv && mv.getControllingPassenger() instanceof Player p && !p.isCreative()
                        || e instanceof fr.modeminecraft.entity.drone.DroneEntity d && d.owner != null;
                if (hostile) {
                    MilAmmo a = pick(v, false);
                    if (a != null && v.consume(a)) {
                        launchInterceptor(sl, v, e);
                    }
                    break;
                }
            }
            if (v.tickCount % 2400 == 0) {
                v.fillAmmo(1f);                                                        // ravitaillement de la base
            }
        }
    }

    /** Missile choisi : antimissile (agile / cinétique) pour un missile, longue portée sinon. */
    private static MilAmmo pick(MilVehicleEntity v, boolean antiMissile) {
        MilAmmo[] order = v.type() == MilType.S_500
                ? (antiMissile ? new MilAmmo[]{MilAmmo.S500_77N6, MilAmmo.S500_40N6} : new MilAmmo[]{MilAmmo.S500_40N6, MilAmmo.S500_77N6})
                : (antiMissile ? new MilAmmo[]{MilAmmo.S400_9M96, MilAmmo.S400_48N6} : new MilAmmo[]{MilAmmo.S400_48N6, MilAmmo.S400_9M96});
        for (MilAmmo a : order) {
            if (v.ammo(a) > 0) {
                return a;
            }
        }
        return null;
    }

    private static void launchInterceptor(ServerLevel sl, MilVehicleEntity v, Entity target) {
        MilAmmo a = pick(v, target == null || target instanceof ShellEntity);
        MilAmmo.Shell shell = a == null ? MilAmmo.Shell.SAM_LONG : a.shell;
        Vec3 tube = v.toWorld(-MilVehicleEntity.dims(v.type())[0] / 2 + 1, MilVehicleEntity.dims(v.type())[2] + 2.5, 0);
        ShellEntity m = new ShellEntity(sl, v.occupant(1) instanceof LivingEntity le ? le : null, shell, v);
        m.setPos(tube.x, tube.y, tube.z);
        m.coldLaunch(target);
        sl.addFreshEntity(m);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, tube.x, tube.y, tube.z, 30, 0.6, 0.3, 0.6, 0.05);
        sl.playSound(null, tube.x, tube.y, tube.z, fr.modeminecraft.registry.ModSounds.MISSILE_LAUNCH.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 8f, 0.8f);
    }

    /** Console S-400 / S-500 : tir sur les contacts choisis (jusqu'à 4 / 2). */
    public static void fireSam(MilVehicleEntity v, ServerPlayer p, int[] targets, int ammoIndex) {
        if (!checkReady(v, p)) {
            return;
        }
        int max = v.type() == MilType.S_500 ? 2 : 4;
        List<MilAmmo> list = v.type().weapons.get(0).ammo();
        MilAmmo wanted = list.get(Math.floorMod(ammoIndex, list.size()));
        ServerLevel sl = (ServerLevel) v.level();
        int fired = 0;
        for (int i = 0; i < Math.min(max, targets.length); i++) {
            MilAmmo a = v.ammo(wanted) > 0 ? wanted : pick(v, false);
            if (a == null || !v.consume(a)) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.strategic.no_missile").withStyle(ChatFormatting.RED), true);
                break;
            }
            int id = targets[i];
            if (id < 0) {
                List<Strikes.Strike> strikes = Strikes.active();
                int k = -id - 1;
                if (k < strikes.size()) {
                    launchInterceptor(sl, v, null);
                    Strikes.intercept(sl, strikes.get(k), v.position(), sl.getGameTime() + 25);
                }
            } else {
                launchInterceptor(sl, v, sl.getEntity(id));
            }
            fired++;
        }
        if (fired > 0) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.strategic.launched", fired).withStyle(ChatFormatting.GREEN), true);
        }
    }

    /**
     * Console S-400 / S-500 : tir sur un point libre de l'écran radar (X, Z, altitude facultative : -1 = auto). Le missile
     * est lancé à froid puis vire vers ces coordonnées, où sa fusée de proximité le fait exploser.
     */
    public static void fireSamAt(MilVehicleEntity v, ServerPlayer p, int x, int alt, int z, int ammoIndex) {
        if (!checkReady(v, p)) {
            return;
        }
        double d = Math.hypot(x - v.getX(), z - v.getZ());
        if (d > range(v.type()) || d < 20) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.strategic.out_of_range", (int) d).withStyle(ChatFormatting.RED), true);
            return;
        }
        List<MilAmmo> list = v.type().weapons.get(0).ammo();
        MilAmmo wanted = list.get(Math.floorMod(ammoIndex, list.size()));
        MilAmmo a = v.ammo(wanted) > 0 ? wanted : pick(v, false);
        if (a == null || !v.consume(a)) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.strategic.no_missile").withStyle(ChatFormatting.RED), true);
            return;
        }
        ServerLevel sl = (ServerLevel) v.level();
        double y = alt >= 0 ? alt : sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) + 2;
        Vec3 tube = v.toWorld(-MilVehicleEntity.dims(v.type())[0] / 2 + 1, MilVehicleEntity.dims(v.type())[2] + 2.5, 0);
        ShellEntity m = new ShellEntity(sl, p, a.shell, v);
        m.setPos(tube.x, tube.y, tube.z);
        m.coldLaunchAt(new Vec3(x + 0.5, y, z + 0.5));
        sl.addFreshEntity(m);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, tube.x, tube.y, tube.z, 30, 0.6, 0.3, 0.6, 0.05);
        sl.playSound(null, tube.x, tube.y, tube.z, fr.modeminecraft.registry.ModSounds.MISSILE_LAUNCH.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 8f, 0.8f);
        p.displayClientMessage(Component.translatable("message.modeminecraft.strategic.launched_point", x, (int) y, z).withStyle(ChatFormatting.GREEN), true);
    }

    /** Console Iskander : frappe balistique sur (X, Z). */
    public static void fireIskander(MilVehicleEntity v, ServerPlayer p, int x, int z, int warhead) {
        if (!checkReady(v, p)) {
            return;
        }
        v.ownerId = p.getUUID();
        List<MilAmmo> list = v.type().weapons.get(0).ammo();
        MilAmmo a = list.get(Math.floorMod(warhead, list.size()));
        double d = Math.hypot(x - v.getX(), z - v.getZ());
        if (d > 1000 || d < 60) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.strategic.out_of_range", (int) d).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!v.consume(a)) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.strategic.no_missile").withStyle(ChatFormatting.RED), true);
            return;
        }
        Strikes.Warhead w = a == MilAmmo.ISK_CLUSTER ? Strikes.Warhead.CLUSTER : a == MilAmmo.ISK_EMP ? Strikes.Warhead.EMP : Strikes.Warhead.HE;
        Vec3 tube = v.toWorld(1.5, MilVehicleEntity.dims(v.type())[2] + 4, 0);
        Strikes.Strike s = Strikes.launch((ServerLevel) v.level(), w, tube, new Vec3(x + 0.5, v.getY(), z + 0.5), p);
        p.displayClientMessage(Component.translatable("message.modeminecraft.strategic.strike", x, z, (s.impact - s.launch) / 20)
                .withStyle(ChatFormatting.GOLD), true);
    }

    private static boolean checkReady(MilVehicleEntity v, ServerPlayer p) {
        if (v.seatOf(p) != 1) {
            return false;
        }
        if (!v.is(MilVehicleEntity.DEPLOYED)) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.strategic.not_deployed").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (v.is(MilVehicleEntity.ENGINE_DOWN)) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.strategic.offline").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }

    private StrategicSystems() {}
}

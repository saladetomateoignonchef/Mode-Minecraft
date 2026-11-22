package fr.modeminecraft.entity.military;

import fr.modeminecraft.entity.bot.HumanBotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Équipages de bots des navires : le commandant patrouille en restant en eau libre (il vire avant la côte), engage
 * les bâtiments ennemis en se tenant à distance de tir (150 à 250 blocs) ; un sous-marin plonge à 20 blocs dès qu'une
 * menace approche. L'officier d'armes choisit sa station : antinavires / hypersoniques au-delà de 120 blocs, torpilles
 * sous 80 blocs, charges anti-sous-marines sur un sous-marin en plongée ; une salve toutes les 10 secondes.
 */
public final class NavalAI {
    private NavalAI() {}

    /** Cible navale de l'équipage : navire, sous-marin ou véhicule ennemi le plus proche (500 blocs), sinon la cible du bot. */
    public static Entity target(MilVehicleEntity v, HumanBotEntity bot) {
        if (bot.getTarget() != null && bot.getTarget().isAlive() && bot.getTarget().distanceTo(v) < 500) {
            Entity t = bot.getTarget();
            return t.getVehicle() instanceof MilVehicleEntity mv ? mv : t;
        }
        MilVehicleEntity best = null;
        double bestD = 500 * 500;
        for (MilVehicleEntity o : v.level().getEntitiesOfClass(MilVehicleEntity.class, v.getBoundingBox().inflate(500, 120, 500),
                o -> o != v && o.type().naval() && !o.is(MilVehicleEntity.SINKING) && v.hostileTo(o))) {
            double d = o.distanceToSqr(v);
            if (o.is(MilVehicleEntity.SUBMERGED) && d > 220 * 220 || o.type() == MilType.ZUMWALT && d > 150 * 150) {
                continue;                                                   // un sous-marin en plongée ne se détecte qu'au sonar
            }
            if (d < bestD) {
                bestD = d;
                best = o;
            }
        }
        return best;
    }

    /** Commandes du commandant : {chadburn (-1..1), barre (-1..1)}. */
    static float[] drive(MilVehicleEntity v, HumanBotEntity bot) {
        if (v.is(MilVehicleEntity.MOBILITY_DOWN) && v.getRandom().nextFloat() < 0.002f) {
            return new float[]{0, 0};
        }
        float len = MilVehicleEntity.dims(v.type())[0];
        Entity enemy = target(v, bot);
        Vec3 goal;
        float throttle = 0.7f;
        if (enemy != null) {
            double d = enemy.distanceTo(v);
            Vec3 to = enemy.position().subtract(v.position());
            if (v.type() == MilType.EXPLOSIVE_USV || v.type().rudder() > 2) {
                goal = enemy.position();                                     // vedettes : à l'abordage
                throttle = 1;
            } else if (d > 250) {
                goal = enemy.position();
                throttle = 1;
            } else {
                // bord à bord : cap perpendiculaire à la cible (toutes les armes portent), on s'éloigne si trop près
                Vec3 side = new Vec3(-to.z, 0, to.x).normalize().scale(100);
                goal = v.position().add(d < 150 ? side.subtract(to.normalize().scale(60)) : side);
                throttle = 0.6f;
            }
        } else {
            if (v.moored) {
                return new float[]{0, 0};                                   // à quai : on ne bouge pas sans menace
            }
            if (v.destination == null || v.position().distanceToSqr(Vec3.atCenterOf(v.destination)) < (len + 20) * (len + 20) || v.tickCount % 1200 == 0) {
                v.destination = pickWater(v);
            }
            if (v.destination == null) {
                return new float[]{0, 0};
            }
            goal = Vec3.atCenterOf(v.destination);
        }
        Vec3 to = goal.subtract(v.position());
        float want = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90;
        float diff = Mth.wrapDegrees(want - v.getYRot());
        // côte ou haut-fond devant : on vire franchement et on change de destination
        if (!open(v, v.getYRot(), len / 2 + 25)) {
            boolean leftOpen = open(v, v.getYRot() - 45, len / 2 + 25);
            diff = leftOpen ? -90 : 90;
            throttle = 0.3f;
            if (enemy == null && v.tickCount % 40 == 0) {
                v.destination = pickWater(v);
            }
        }
        float side = Mth.clamp(-diff / 20f, -1, 1);
        return new float[]{Math.abs(diff) > 120 ? throttle * 0.4f : throttle, side};
    }

    /** Eau libre (au moins 4 blocs de fond) à `dist` blocs dans la direction `yaw`. */
    private static boolean open(MilVehicleEntity v, float yaw, double dist) {
        double rad = Math.toRadians(yaw);
        int sea = v.level().getSeaLevel();
        for (double d = dist * 0.5; d <= dist; d += dist * 0.25) {
            int x = Mth.floor(v.getX() - Math.sin(rad) * d), z = Mth.floor(v.getZ() + Math.cos(rad) * d);
            if (!(v.level() instanceof ServerLevel sl) || !sl.hasChunk(x >> 4, z >> 4)) {
                return true;                                                // terrain pas chargé : on suppose la mer
            }
            if (!water(v, x, sea - 1, z) || !water(v, x, sea - 4, z)) {
                return false;
            }
        }
        return true;
    }

    private static boolean water(MilVehicleEntity v, int x, int y, int z) {
        return v.level().getFluidState(new BlockPos(x, y, z)).is(net.minecraft.tags.FluidTags.WATER);
    }

    /** Point de patrouille au hasard en eau libre (60 à 220 blocs). */
    private static BlockPos pickWater(MilVehicleEntity v) {
        int sea = v.level().getSeaLevel();
        Vec3 home = v.navalHome != null ? Vec3.atCenterOf(v.navalHome) : v.position();
        for (int k = 0; k < 12; k++) {
            double a = v.getRandom().nextDouble() * Math.PI * 2, r = 60 + v.getRandom().nextDouble() * 160;
            int x = Mth.floor(home.x + Math.cos(a) * r), z = Mth.floor(home.z + Math.sin(a) * r);
            if (v.level() instanceof ServerLevel sl && sl.hasChunk(x >> 4, z >> 4) && water(v, x, sea - 1, z) && water(v, x, sea - 6, z)) {
                return new BlockPos(x, sea, z);
            }
        }
        return null;
    }

    /** Sous-marin : +1 remonter, -1 plonger, 0 tenir l'immersion (20 blocs en présence d'une menace, 8 sinon). */
    static int dive(MilVehicleEntity v, HumanBotEntity bot) {
        float want = target(v, bot) != null ? 20 : 8;
        return v.subDepth < want - 1 ? -1 : v.subDepth > want + 1 ? 1 : 0;
    }

    /** L'officier d'armes (bot) lance-t-il ? Choisit aussi la station adaptée à la cible. */
    static boolean wantsLaunch(MilVehicleEntity v, HumanBotEntity bot) {
        if ((v.tickCount + v.getId()) % 200 != 0) {
            return false;
        }
        Entity t = target(v, bot);
        if (t == null) {
            return false;
        }
        double d = t.distanceTo(v);
        boolean naval = t instanceof MilVehicleEntity mv && mv.type().naval();
        boolean under = t instanceof MilVehicleEntity mv && mv.is(MilVehicleEntity.SUBMERGED);
        MilType type = v.type();
        int pick = -1;
        for (int wi : type.stations()) {
            MilType.Weapon w = type.weapons.get(wi);
            boolean ok = switch (w.kind()) {
                case TORPEDO -> naval && d < 80 && !v.is(MilVehicleEntity.SINKING);
                case ASW -> under && d < 200;
                case NAVAL_MISSILE -> !under && d > 120;
                default -> false;
            };
            if (ok && v.stock(w) > 0) {
                pick = wi;
                if (w.kind() == MilType.Kind.TORPEDO) {
                    break;                                                  // à courte portée, la torpille d'abord
                }
            }
        }
        if (pick < 0) {
            return false;
        }
        v.selectStation(pick);
        return true;
    }

    /** Cible vivante d'un bot à bord (pour l'artillerie navale). */
    static LivingEntity crewTarget(MilVehicleEntity v) {
        return MilAI.crewTarget(v);
    }
}

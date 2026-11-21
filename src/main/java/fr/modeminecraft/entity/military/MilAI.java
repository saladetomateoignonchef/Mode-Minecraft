package fr.modeminecraft.entity.military;

import fr.modeminecraft.entity.bot.HumanBotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Équipages de bots : conducteur (suit sa destination ou le véhicule de tête, contourne les obstacles, s'arrête et
 * présente l'avant blindé face à l'ennemi), tireur (acquiert les cibles jusqu'à 120 blocs, pointe la tourelle en
 * anticipant le mouvement et la chute de l'obus, tire quand c'est aligné et dégagé), pilote d'hélicoptère.
 */
public final class MilAI {
    /** Commandes du conducteur : {avancer (-1..1), tourner (-1..1)}. */
    static float[] drive(MilVehicleEntity v, HumanBotEntity driver) {
        LivingEntity enemy = crewTarget(v);
        if (enemy != null && enemy.isAlive()) {
            double d = v.distanceTo(enemy);
            float want = (float) (Mth.atan2(enemy.getZ() - v.getZ(), enemy.getX() - v.getX()) * Mth.RAD_TO_DEG) - 90;
            float diff = Mth.wrapDegrees(want - v.getYRot());
            if (v.type().tracked() && !v.type().soft()) {
                // char : avant blindé face à la menace, distance d'engagement 35-90 blocs
                float fwd = d > 90 ? 0.6f : d < 25 ? -0.5f : 0;
                return new float[]{Math.abs(diff) > 30 ? 0 : fwd, Mth.clamp(-diff / 20f, -1, 1)};
            }
            // véhicule léger : ralentit et se met de travers pour donner un angle de tir à sa tourelle
            if (d < 45) {
                float side = Mth.clamp(-(Mth.wrapDegrees(diff - 70)) / 30f, -1, 1);
                return new float[]{Math.abs(v.speed()) > 0.15f ? -1 : 0, side};
            }
        }
        Vec3 goal;
        if (v.leader != null && v.level() instanceof ServerLevel sl && sl.getEntity(v.leader) instanceof MilVehicleEntity lead && lead.isAlive()) {
            double r = Math.toRadians(lead.getYRot());
            goal = lead.position().subtract(-Math.sin(r) * 12, 0, Math.cos(r) * 12);
        } else if (v.destination != null) {
            goal = Vec3.atBottomCenterOf(v.destination);
        } else {
            return new float[]{0, 0};
        }
        Vec3 to = goal.subtract(v.position());
        double dist = Math.sqrt(to.x * to.x + to.z * to.z);
        if (dist < 8) {
            if (v.leader == null) {
                v.destination = null;
            }
            return new float[]{-1, 0};
        }
        float want = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90;
        float diff = Mth.wrapDegrees(want - v.getYRot());
        double rad = Math.toRadians(v.getYRot());
        float len = MilVehicleEntity.dims(v.type())[0];
        Vec3 eye = v.position().add(0, 1.4, 0);
        Vec3 ahead = eye.add(-Math.sin(rad) * (len / 2 + 4), 0, Math.cos(rad) * (len / 2 + 4));
        boolean blocked = !v.type().tracked()
                && v.level().clip(new ClipContext(eye, ahead, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, v)).getType() == HitResult.Type.BLOCK;
        for (double d = len / 2 + 2; d <= len / 2 + 6 && !blocked; d += 2) {
            int ax = Mth.floor(v.getX() - Math.sin(rad) * d), az = Mth.floor(v.getZ() + Math.cos(rad) * d);
            int ground = v.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ax, az);
            if (ground < v.getY() - 3 || !v.type().has(MilType.Flag.AMPHIBIOUS) && !v.level().getFluidState(new BlockPos(ax, ground - 1, az)).isEmpty()) {
                blocked = true;
            }
        }
        if (blocked) {
            diff = diff >= 0 ? 60 : -60;
        }
        float side = Mth.clamp(-diff / 25f, -1, 1);
        float fwd = Math.abs(diff) > 100 ? 0.2f : 1f;
        return new float[]{fwd * (v.leader != null && dist < 16 ? 0.4f : 0.75f), side};
    }

    /** Pilote d'hélicoptère (bot) : garde ~25 blocs au-dessus du sol. */
    static int lift(MilVehicleEntity v) {
        int ground = v.level().getHeight(Heightmap.Types.MOTION_BLOCKING, v.getBlockX(), v.getBlockZ());
        double h = v.getY() - ground;
        return h < 22 ? 1 : h > 30 ? -1 : 0;
    }

    /** Cible de l'équipage (celle du tireur, sinon d'un autre membre). */
    static LivingEntity crewTarget(MilVehicleEntity v) {
        for (Entity p : v.getPassengers()) {
            if (p instanceof HumanBotEntity b && b.getTarget() != null && b.getTarget().isAlive()) {
                return b.getTarget();
            }
        }
        return null;
    }

    /** Pointage du tireur bot : {lacet, élévation} relatifs à la caisse, ou null. */
    static float[] aim(MilVehicleEntity v, HumanBotEntity gunner, MilType.Weapon w) {
        acquire(v, gunner, w);
        LivingEntity t = gunner.getTarget();
        if (t == null || !t.isAlive()) {
            return null;
        }
        MilAmmo a = w.ammo().get(0);
        double d = v.distanceTo(t);
        double flight = d / Math.max(0.5, a.shell.speed);
        Vec3 p = t.position().add(0, t.getBbHeight() * 0.5, 0).add(t.getDeltaMovement().scale(flight));
        float[] r = v.aimAt(p);
        // compensation de la chute (obus explosifs, grenades, roquettes)
        r[1] += (float) Math.toDegrees(Math.atan(a.shell.gravity * flight * flight / 2 / Math.max(1, d)));
        return r;
    }

    /** Recherche de cible à longue portée (les équipages voient loin : optiques). */
    private static void acquire(MilVehicleEntity v, HumanBotEntity gunner, MilType.Weapon w) {
        if (gunner.getTarget() != null && gunner.getTarget().isAlive() || (v.tickCount + v.getId()) % 20 != 0) {
            return;
        }
        double range = w.kind() == MilType.Kind.CANNON || w.kind() == MilType.Kind.ROCKETS ? 140 : 100;
        LivingEntity best = null;
        double bestD = range * range;
        for (LivingEntity e : v.level().getEntitiesOfClass(LivingEntity.class, v.getBoundingBox().inflate(range), e -> e.isAlive() && !v.hasPassenger(e))) {
            if (!gunner.shouldAttack(e)) {
                continue;
            }
            double d = e.distanceToSqr(v);
            if (d < bestD && clear(v, e)) {
                bestD = d;
                best = e;
            }
        }
        // les véhicules ennemis aussi (leur équipage est une cible)
        if (best == null) {
            for (MilVehicleEntity o : v.level().getEntitiesOfClass(MilVehicleEntity.class, v.getBoundingBox().inflate(range), o -> o != v)) {
                for (Entity p : o.getPassengers()) {
                    if (p instanceof LivingEntity le && gunner.shouldAttack(le) && clear(v, o)) {
                        best = le;
                        break;
                    }
                }
            }
        }
        if (best != null) {
            gunner.setTarget(best);
        }
    }

    private static boolean clear(MilVehicleEntity v, Entity e) {
        Vec3 from = v.position().add(0, MilVehicleEntity.dims(v.type())[2] + 0.3, 0);
        Vec3 to = e.position().add(0, e.getBbHeight() * 0.6, 0);
        return v.level().clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, v)).getType() == HitResult.Type.MISS;
    }

    /** Le tireur bot tire-t-il ? (cible à portée, tourelle alignée, ligne de tir dégagée) */
    static boolean wantsFire(MilVehicleEntity v, HumanBotEntity bot, MilType.Seat seat) {
        if (seat.primary() < 0 || bot.getTarget() == null || !bot.getTarget().isAlive()) {
            return false;
        }
        MilType.Weapon w = v.type().weapons.get(seat.primary());
        if (w.free()) {
            return false;
        }
        LivingEntity t = bot.getTarget();
        double d = v.distanceTo(t);
        if (d > (w.kind() == MilType.Kind.MG ? 90 : 150) || w.kind() == MilType.Kind.ROCKETS && d < 40) {
            return false;
        }
        float[] want = aim(v, bot, w);
        if (want == null) {
            return false;
        }
        float dy = Math.abs(Mth.wrapDegrees(want[0] - v.turret(1))), dp = Math.abs(want[1] - v.gunPitch(1));
        return dy < 3 && dp < 3 && clear(v, t);
    }

    /** Passager bot : à l'arrêt, tire au fusil d'assaut par la fenêtre sur la cible de l'équipage. */
    static void passengerFire(MilVehicleEntity v, HumanBotEntity bot) {
        LivingEntity t = crewTarget(v);
        if (t == null || Math.abs(v.speed()) > 0.06f || (v.tickCount + bot.getId()) % 5 != 0
                || !(bot.getMainHandItem().getItem() instanceof fr.modeminecraft.item.gun.GunItem gun)) {
            return;
        }
        Vec3 eye = bot.getEyePosition();
        Vec3 to = t.position().add(0, t.getBbHeight() * 0.6, 0);
        if (eye.distanceTo(to) > 55 || v.level().clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bot)).getType() != HitResult.Type.MISS) {
            return;
        }
        bot.setTarget(t);
        bot.getLookControl().setLookAt(t, 30, 30);
        var stats = gun.stats();
        var b = new fr.modeminecraft.entity.BulletEntity(v.level(), bot, stats.damage() * 0.6f, 0, false, fr.modeminecraft.item.gun.AmmoType.STANDARD);
        b.setWeapon(bot.getMainHandItem());
        b.setPos(eye.x, eye.y, eye.z);
        b.shoot(to.x - eye.x, to.y - eye.y, to.z - eye.z, stats.velocity(), 3.5f);
        v.level().addFreshEntity(b);
        bot.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        v.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(), stats.sound().get(), net.minecraft.sounds.SoundSource.HOSTILE, 4f, stats.pitch());
    }

    private MilAI() {}
}

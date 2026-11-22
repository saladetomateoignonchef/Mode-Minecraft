package fr.modeminecraft.entity.ai;

import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.item.gun.AmmoType;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunStats;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * IA d'un monstre armé d'une arme à feu : il s'approche jusqu'à être à bonne distance, vise, tire par rafales
 * (ou coup par coup pour les armes semi-automatiques et à verrou), et recharge quand son chargeur est vide.
 * Il est bien moins précis qu'un joueur et ses balles font 60 % des dégâts de l'arme.
 */
public class GunAttackGoal extends Goal {
    private final Mob mob;
    private int cooldown;
    private int burstLeft;
    private int magazine = -1;
    private int seeTime;

    public GunAttackGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private GunItem gun() {
        return mob.getMainHandItem().getItem() instanceof GunItem g ? g : null;
    }

    private double range() {
        GunItem g = gun();
        if (g == null) {
            return 0;
        }
        return switch (g.stats().category()) {
            case PISTOL, SHOTGUN -> 16;
            case SMG -> 20;
            case SNIPER -> 48;
            default -> 32;
        };
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        GunItem g = gun();
        if (fr.modeminecraft.entity.TurretPost.operating(mob)) {
            return false;
        }
        return g != null && g.stats().projectile() == GunStats.ProjectileKind.BULLET && target != null && target.isAlive()
                && mob.distanceTo(target) < range() * 1.5;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        seeTime = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        GunItem g = gun();
        if (target == null || g == null) {
            return;
        }
        GunStats stats = g.stats();
        if (magazine < 0) {
            magazine = stats.magazineSize();
        }
        double dist = mob.distanceTo(target);
        boolean sees = mob.getSensing().hasLineOfSight(target);
        seeTime = sees ? seeTime + 1 : 0;
        mob.getLookControl().setLookAt(target, 30, 30);
        mob.setAggressive(true);
        if (!sees || dist > range()) {
            mob.getNavigation().moveTo(target, 1.0);
        } else if (dist < 4) {
            mob.getNavigation().stop();
            mob.getMoveControl().strafe(-0.5f, 0); // recule un peu
        } else {
            mob.getNavigation().stop();
        }
        if (--cooldown > 0 || !sees || seeTime < 15 || dist > range()) {
            return;
        }
        if (magazine <= 0) {
            // Rechargement
            magazine = stats.magazineSize();
            cooldown = stats.reloadTicks() + 10;
            mob.playSound(fr.modeminecraft.registry.ModSounds.MAG_IN.get(), 0.8f, 1.0f);
            return;
        }
        fire(stats, target);
        magazine--;
        boolean automatic = stats.rpm() > 0 || stats.fireDelay() <= 3;
        if (automatic) {
            if (burstLeft <= 0) {
                burstLeft = 2 + mob.getRandom().nextInt(4); // rafale de 3 à 6 balles
            }
            burstLeft--;
            cooldown = burstLeft > 0 ? Math.max(1, Math.round(stats.shotInterval())) : 30 + mob.getRandom().nextInt(20);
        } else {
            cooldown = Math.max(stats.fireDelay(), stats.pistol() ? 14 : 30) + mob.getRandom().nextInt(10);
        }
    }

    private void fire(GunStats stats, LivingEntity target) {
        var level = mob.level();
        float damage = stats.damage() * 0.6f;
        int pellets = Math.max(1, stats.pellets());
        for (int i = 0; i < pellets; i++) {
            var bullet = new BulletEntity(level, mob, pellets > 1 ? damage / pellets * 1.5f : damage, 0, false, AmmoType.STANDARD);
            bullet.setWeapon(mob.getMainHandItem()); // calibre (gilets pare-balles)
            double dx = target.getX() - mob.getX();
            double dy = target.getY(0.6) - bullet.getY();
            double dz = target.getZ() - mob.getZ();
            float inaccuracy = (pellets > 1 ? 6 : 2.5f) + (14 - level.getDifficulty().getId() * 4) * 0.4f;
            if (mob.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) {
                inaccuracy *= 4;    // ébloui (lampe en stroboscope) : il tire presque au hasard
            }
            bullet.shoot(dx, dy, dz, stats.velocity(), inaccuracy);
            level.addFreshEntity(bullet);
        }
        mob.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), stats.sound().get(), SoundSource.HOSTILE, 4f,
                stats.pitch() + (mob.getRandom().nextFloat() - 0.5f) * 0.06f);
        ItemStack stack = mob.getMainHandItem();
        GunItem.setAmmo(stack, Math.max(0, magazine - 1));
    }
}

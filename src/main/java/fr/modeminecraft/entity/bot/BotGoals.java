package fr.modeminecraft.entity.bot;

import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.entity.MountedGunEntity;
import fr.modeminecraft.entity.RocketEntity;
import fr.modeminecraft.entity.ThrownGrenadeEntity;
import fr.modeminecraft.entity.TomahawkEntity;
import fr.modeminecraft.item.explosive.GrenadeItem;
import fr.modeminecraft.item.gun.AmmoType;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunStats;
import fr.modeminecraft.item.melee.MeleeWeaponItem;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/** Les comportements (IA) des bots humains. */
public final class BotGoals {
    /** Le joueur tient-il une arme (arme à feu, grenade, arme blanche, épée, hache, arc...) en main ? */
    public static boolean armedInHand(Player p) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            var i = s.getItem();
            if (i instanceof GunItem || i instanceof GrenadeItem || i instanceof SwordItem || i instanceof AxeItem
                    || i instanceof BowItem || i instanceof CrossbowItem || i instanceof TridentItem
                    || i instanceof fr.modeminecraft.item.explosive.PlaceableExplosiveItem || s.is(ModItems.DRAGONSLAYER.get())) {
                return true;
            }
        }
        return false;
    }

    /** Arme visible : en main, ou rangée dans le dos / à la hanche. */
    public static boolean armedVisible(Player p) {
        if (armedInHand(p)) {
            return true;
        }
        for (ItemStack s : fr.modeminecraft.item.gun.BackGunsHandler.pick(p)) {
            if (!s.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Dans le cône de vision (110°) et à portée, sans obstacle ? */
    static boolean canSee(HumanBotEntity bot, LivingEntity t, double range) {
        Vec3 to = t.getEyePosition().subtract(bot.getEyePosition());
        double d = to.length();
        if (d > range) {
            return false;
        }
        // ouïe : un joueur qui ne se faufile pas est repéré à moins de 10 blocs, même de dos
        boolean heard = d < 10 && !(t instanceof Player p && p.isShiftKeyDown());
        if (d > 3 && !heard) {
            Vec3 look = Vec3.directionFromRotation(0, bot.getYHeadRot());
            Vec3 flat = new Vec3(to.x, 0, to.z).normalize();
            if (flat.dot(look) < Math.cos(Math.toRadians(55))) {
                return false;
            }
        }
        return bot.getSensing().hasLineOfSight(t);
    }

    // =====================================================================================================
    /** Cible : la créature hostile la plus proche dans le champ de vision. */
    static final class SightTarget extends TargetGoal {
        private final HumanBotEntity bot;
        private int scan;

        SightTarget(HumanBotEntity bot) {
            super(bot, false);
            this.bot = bot;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!bot.faction().combatant() || --scan > 0) {
                return false;
            }
            scan = 10;
            // économie : sans joueur à moins de 64 blocs, seuls les Faucons cherchent des cibles (les autres factions)
            if (bot.faction() != Faction.FALCON && bot.level().getNearestPlayer(bot, 64) == null) {
                scan = 40;
                return false;
            }
            LivingEntity cur = bot.getTarget();
            if (cur != null && cur.isAlive()) {
                return false;
            }
            double range = bot.faction().sightRange(bot.role());
            LivingEntity best = null;
            double bestD = Double.MAX_VALUE;
            for (LivingEntity e : bot.level().getEntitiesOfClass(LivingEntity.class, bot.getBoundingBox().inflate(range))) {
                if (!bot.shouldAttack(e) || !canSee(bot, e, range)) {
                    continue;
                }
                double d = bot.distanceToSqr(e);
                if (d < bestD) {
                    bestD = d;
                    best = e;
                }
            }
            if (best == null) {
                return false;
            }
            targetMob = best;
            return true;
        }

        @Override
        public void start() {
            bot.setTarget(targetMob);
            BotEvents.onTargetAcquired(bot, targetMob);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = bot.getTarget();
            double range = bot.faction().sightRange(bot.role()) * 1.4;
            return t != null && t.isAlive() && bot.distanceToSqr(t) < range * range
                    && (bot.shouldAttack(t) || t == bot.getLastHurtByMob()) && !(t instanceof Player p && (p.isCreative() || p.isSpectator()));
        }

        @Override
        protected double getFollowDistance() {
            return bot.faction().sightRange(bot.role()) * 1.4;
        }
    }

    /** Riposte : on attaque celui qui nous a blessés (et la faction est prévenue). */
    static final class Retaliate extends TargetGoal {
        private final HumanBotEntity bot;
        private int stamp;

        Retaliate(HumanBotEntity bot) {
            super(bot, true);
            this.bot = bot;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity a = bot.getLastHurtByMob();
            return bot.faction().combatant() && a != null && bot.getLastHurtByMobTimestamp() != stamp && a.isAlive()
                    && !(a instanceof HumanBotEntity o && o.faction().group.equals(bot.faction().group))
                    && !(a instanceof Player p && (p.isCreative() || p.isSpectator()));
        }

        @Override
        public void start() {
            stamp = bot.getLastHurtByMobTimestamp();
            bot.setTarget(bot.getLastHurtByMob());
            targetMob = bot.getLastHurtByMob();
            unseenMemoryTicks = 300;
            super.start();
        }
    }

    // =====================================================================================================
    /** Sommation (police, shérifs, sécurité) : arme pointée, alerte jaune 3,5 s ; arme rangée = on se calme. */
    static final class Warn extends Goal {
        private final HumanBotEntity bot;
        private Player suspect;
        private long end;
        private long cooldownUntil;

        Warn(HumanBotEntity bot) {
            this.bot = bot;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean warns() {
            Faction.Attitude a = bot.faction().attitude;
            return a == Faction.Attitude.WARN || a == Faction.Attitude.HOSTILE_INDOORS;
        }

        @Override
        public boolean canUse() {
            if (!warns() || bot.getTarget() != null || bot.level().getGameTime() < cooldownUntil || bot.tickCount % 5 != 0) {
                return false;
            }
            long now = bot.level().getGameTime();
            for (Player p : bot.level().getEntitiesOfClass(Player.class, bot.getBoundingBox().inflate(20))) {
                if (!p.isCreative() && !p.isSpectator() && armedVisible(p) && !FactionRelations.isHostile(p.getUUID(), bot.faction(), now)
                        && canSee(bot, p, 20)) {
                    suspect = p;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            long now = bot.level().getGameTime();
            end = now + 70;
            bot.setAlertEnd(end);
            bot.setAggressive(true);
            bot.getNavigation().stop();
            bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(), ModSounds.POLICE_FREEZE_WARNING.get(), SoundSource.HOSTILE, 1.6f, 1f);
            suspect.displayClientMessage(Component.translatable("message.modeminecraft.bot.warn." + bot.faction().group)
                    .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
        }

        @Override
        public boolean canContinueToUse() {
            return suspect != null && suspect.isAlive() && bot.getTarget() == null && bot.level().getGameTime() <= end;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            bot.getLookControl().setLookAt(suspect, 30, 30);
            long now = bot.level().getGameTime();
            if (!armedVisible(suspect)) {
                // plus aucune arme visible (ni en main, ni dans le dos / à la hanche) : on abaisse la sienne
                end = 0;
                cooldownUntil = now + 100;
                return;
            }
            if (now >= end) {
                FactionRelations.markHostile(suspect.getUUID(), bot.faction(), now);
                bot.setTarget(suspect);
                BotEvents.alertFaction(bot, suspect);
            }
        }

        @Override
        public void stop() {
            bot.setAlertEnd(0);
            if (bot.getTarget() == null) {
                bot.setAggressive(false);
            }
            suspect = null;
        }
    }

    // =====================================================================================================
    /** Combat à l'arme à feu : rafales, rechargement, couverture, grenades ; les snipers tirent allongés. */
    static final class GunCombat extends Goal {
        private enum State { ENGAGE, SEEK, COVER }

        private final HumanBotEntity bot;
        private State state = State.ENGAGE;
        private BlockPos cover;
        private int stateTicks, cooldown, burstLeft, magazine = -1, seeTime;
        private long nextCover, nextGrenade;

        GunCombat(HumanBotEntity bot) {
            this.bot = bot;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private GunItem gun() {
            return bot.getMainHandItem().getItem() instanceof GunItem g ? g : null;
        }

        private double range() {
            GunItem g = gun();
            if (g == null) {
                return 0;
            }
            if (bot.role() == BotRole.SNIPER) {
                return 90;
            }
            return switch (g.stats().category()) {
                case PISTOL, SHOTGUN -> 18;
                case SMG -> 22;
                case SNIPER -> 60;
                case LAUNCHER -> 40;
                default -> 36;
            };
        }

        @Override
        public boolean canUse() {
            LivingEntity t = bot.getTarget();
            return gun() != null && !bot.isPassenger() && t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            // on reste à l'abri si on y est déjà (la cible a juste changé)
            if (!(state == State.COVER && cover != null && bot.distanceToSqr(Vec3.atBottomCenterOf(cover)) < 4)) {
                state = State.ENGAGE;
            }
            bot.setAggressive(true);
        }

        @Override
        public void stop() {
            bot.setAggressive(false);
            bot.setStance(Pose.STANDING);
            bot.getNavigation().stop();
            seeTime = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = bot.getTarget();
            GunItem g = gun();
            if (target == null || g == null) {
                return;
            }
            GunStats stats = g.stats();
            if (magazine < 0) {
                magazine = stats.magazineSize();
            }
            long now = bot.level().getGameTime();
            double dist = bot.distanceTo(target);
            boolean sees = bot.getSensing().hasLineOfSight(target);
            seeTime = sees ? seeTime + 1 : 0;
            bot.getLookControl().setLookAt(target, 30, 30);
            double range = range();

            bot.debugState = state + (cover != null ? " cover=" + cover : "");
            if (bot.role() == BotRole.SNIPER) {
                // sniper : reste en place, allongé sur le rebord
                bot.getNavigation().stop();
                bot.setStance(dist > 10 ? Pose.SWIMMING : Pose.STANDING);
                tryFire(target, stats, sees, dist, range, now);
                return;
            }
            switch (state) {
                case ENGAGE -> {
                    bot.setStance(Pose.STANDING);
                    if (now - bot.lastRangedHit < 10 && now >= nextCover) {
                        cover = findCover(target);
                        bot.debugState = "find cover -> " + cover;
                        nextCover = now + 120;
                        if (cover != null) {
                            state = State.SEEK;
                            stateTicks = 0;
                            bot.getNavigation().moveTo(cover.getX() + 0.5, cover.getY(), cover.getZ() + 0.5, 1.35);
                            return;
                        }
                    }
                    if (!sees || dist > range) {
                        if (bot.tickCount % 10 == 0) {
                            bot.getNavigation().moveTo(target, 1.0);
                        }
                    } else if (dist < 4) {
                        bot.getNavigation().stop();
                        bot.getMoveControl().strafe(-0.5f, 0);
                    } else {
                        bot.getNavigation().stop();
                    }
                    tryFire(target, stats, sees, dist, range, now);
                }
                case SEEK -> {
                    stateTicks++;
                    if (cover == null || stateTicks > 80) {
                        state = State.ENGAGE;
                    } else if (bot.distanceToSqr(Vec3.atBottomCenterOf(cover)) < 1.6 || bot.getNavigation().isDone()) {
                        state = State.COVER;
                        stateTicks = 0;
                        bot.getNavigation().stop();
                    }
                }
                case COVER -> {
                    bot.getNavigation().stop();
                    int cycle = stateTicks % 70;
                    stateTicks++;
                    if (cycle < 40) {
                        bot.setStance(Pose.CROUCHING);                  // à l'abri
                        if (magazine < stats.magazineSize() / 2 && bot.reloadEnd() < now) {
                            startReload(stats, now);
                        }
                    } else {
                        bot.setStance(Pose.STANDING);                   // on se relève et on tire une courte rafale
                        tryFire(target, stats, sees, dist, range, now);
                    }
                    if (stateTicks > 300 || dist > range * 1.3) {
                        state = State.ENGAGE;
                        nextCover = now + 60;
                    }
                }
            }
            // grenades sur une cible à couvert ou depuis notre abri
            String grenade = bot.faction().loadout.grenade();
            if (grenade != null && now >= nextGrenade && dist > 6 && dist < 22 && (!sees || state == State.COVER)
                    && bot.getRandom().nextFloat() < 0.03f) {
                throwGrenade(grenade, target);
                nextGrenade = now + 400;
            }
        }

        private void tryFire(LivingEntity target, GunStats stats, boolean sees, double dist, double range, long now) {
            if (--cooldown > 0 || !sees || seeTime < (bot.role() == BotRole.SNIPER ? 25 : 10) || dist > range || bot.reloadEnd() > now) {
                return;
            }
            if (magazine <= 0) {
                startReload(stats, now);
                return;
            }
            fire(stats, target);
            magazine--;
            GunItem.setAmmo(bot.getMainHandItem(), Math.max(0, magazine));
            boolean automatic = stats.rpm() > 0 || stats.fireDelay() <= 3;
            if (automatic && bot.role() != BotRole.SNIPER) {
                if (burstLeft <= 0) {
                    burstLeft = 2 + bot.getRandom().nextInt(4);           // courtes rafales de 3 à 6 balles
                }
                burstLeft--;
                cooldown = burstLeft > 0 ? Math.max(1, Math.round(stats.shotInterval())) : 25 + bot.getRandom().nextInt(20);
            } else {
                cooldown = Math.max(stats.fireDelay(), stats.pistol() ? 12 : bot.role() == BotRole.SNIPER ? 50 : 28) + bot.getRandom().nextInt(10);
            }
        }

        private void startReload(GunStats stats, long now) {
            magazine = stats.magazineSize();
            int t = Math.max(20, stats.reloadTicks());
            bot.setReloadEnd(now + t);
            cooldown = t + 5;
            bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(), ModSounds.MAG_OUT.get(), SoundSource.HOSTILE, 0.8f, 1.0f);
            DelayedSound.schedule(bot, t - 6, ModSounds.MAG_IN.get());
        }

        private void fire(GunStats stats, LivingEntity target) {
            var level = bot.level();
            bot.markShot();
            Vec3 eye = bot.getEyePosition();
            double dx = target.getX() - bot.getX();
            double dy = target.getY(0.6) - eye.y;
            double dz = target.getZ() - bot.getZ();
            if (stats.category() == GunStats.Category.LAUNCHER) {
                RocketEntity r = new RocketEntity(ModEntities.ROCKET.get(), level, bot, 2.5f);
                r.setPos(eye.add(bot.getLookAngle().scale(0.8)));
                r.shoot(dx, dy, dz, 2.2f, 2.5f);
                level.addFreshEntity(r);
                magazine = 0;
            } else {
                float damage = stats.damage() * 0.6f;
                int pellets = Math.max(1, stats.pellets());
                float inaccuracy = (pellets > 1 ? 6 : 2.5f) + (14 - level.getDifficulty().getId() * 4) * 0.4f;
                if (bot.role() == BotRole.SNIPER) {
                    inaccuracy = 0.6f + (3 - level.getDifficulty().getId()) * 0.3f;
                }
                if (bot.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) {
                    inaccuracy *= 4;
                }
                for (int i = 0; i < pellets; i++) {
                    var b = new BulletEntity(level, bot, pellets > 1 ? damage / pellets * 1.5f : damage, 0, false, AmmoType.STANDARD);
                    b.setWeapon(bot.getMainHandItem());
                    b.shoot(dx, dy, dz, stats.velocity(), inaccuracy);
                    level.addFreshEntity(b);
                }
            }
            bot.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, bot.getX(), bot.getY(), bot.getZ(), stats.sound().get(), SoundSource.HOSTILE, 4f,
                    stats.pitch() + (bot.getRandom().nextFloat() - 0.5f) * 0.06f);
            level.gameEvent(bot, net.minecraft.world.level.gameevent.GameEvent.PROJECTILE_SHOOT, bot.position());
        }

        private void throwGrenade(String id, LivingEntity target) {
            if (!(HumanBotEntity.item(id) instanceof GrenadeItem gi)) {
                return;
            }
            var g = new ThrownGrenadeEntity(bot.level(), bot, gi.kind(), gi.kind().fuse);
            Vec3 eye = bot.getEyePosition();
            g.setPos(eye.x, eye.y, eye.z);
            double dx = target.getX() - bot.getX(), dz = target.getZ() - bot.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            g.shoot(dx, target.getY() - eye.y + d * 0.35, dz, (float) Math.min(1.3, 0.45 + d * 0.045), 4f);
            bot.level().addFreshEntity(g);
            bot.swing(InteractionHand.OFF_HAND);
        }

        /** Un abri à moins de 8 blocs : un bloc solide entre lui et la cible, à hauteur de hanche. */
        private BlockPos findCover(LivingEntity target) {
            var level = bot.level();
            BlockPos best = null;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < 17 * 17; i++) {
                BlockPos p = bot.blockPosition().offset(i % 17 - 8, 0, i / 17 - 8);
                if (p.distSqr(bot.blockPosition()) > 64) {
                    continue;
                }
                p = standable(p);
                if (p == null) {
                    continue;
                }
                Vec3 to = target.position().subtract(Vec3.atBottomCenterOf(p));
                Direction dir = Direction.getNearest(to.x, 0, to.z);
                BlockPos wall = p.relative(dir);
                var shape = level.getBlockState(wall).getCollisionShape(level, wall);
                if (shape.isEmpty() || shape.max(Direction.Axis.Y) < 0.9) {
                    continue;                               // il faut au moins un muret à hauteur de hanche
                }
                // à genoux, la ligne de tir est bien coupée
                Vec3 low = Vec3.atBottomCenterOf(p).add(0, 1.1, 0);
                if (level.clip(new ClipContext(low, target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bot)).getType() != HitResult.Type.BLOCK) {
                    continue;
                }
                double d = bot.distanceToSqr(Vec3.atBottomCenterOf(p));
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
            return best;
        }

        private BlockPos standable(BlockPos p) {
            var level = bot.level();
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos q = p.above(dy);
                if (level.getBlockState(q.below()).isSolidRender(level, q.below()) && level.getBlockState(q).getCollisionShape(level, q).isEmpty()
                        && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()) {
                    return q;
                }
            }
            return null;
        }
    }

    // =====================================================================================================
    /** Corps à corps : sprint, esquives latérales, dash au katana, lancer de tomahawk, coups balayants. */
    static final class MeleeCombat extends Goal {
        private final HumanBotEntity bot;
        private int attackCooldown, dashCooldown, throwCooldown, strafeTicks;
        private float strafeDir;

        MeleeCombat(HumanBotEntity bot) {
            this.bot = bot;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = bot.getTarget();
            return !bot.hasGun() && !bot.isPassenger() && t != null && t.isAlive() && bot.faction().combatant();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            bot.setAggressive(true);
        }

        @Override
        public void stop() {
            bot.setAggressive(false);
            bot.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = bot.getTarget();
            if (target == null) {
                return;
            }
            attackCooldown--;
            dashCooldown--;
            throwCooldown--;
            bot.getLookControl().setLookAt(target, 30, 30);
            double dist = bot.distanceTo(target);
            long now = bot.level().getGameTime();
            boolean drawing = !bot.swordReady();
            if (drawing) {
                bot.getNavigation().stop();           // le Chef dégaine avant de charger
                return;
            }
            // déplacement : charge en sprint, esquives latérales à moyenne distance
            if (strafeTicks > 0) {
                strafeTicks--;
                bot.getNavigation().stop();
                bot.getMoveControl().strafe(0.6f, strafeDir);
            } else if (!bot.isWithinMeleeAttackRange(target)) {
                if (bot.tickCount % 8 == 0) {
                    bot.getNavigation().moveTo(target, 1.3);
                }
                if (dist > 3 && dist < 8 && target instanceof Player && bot.getRandom().nextFloat() < 0.04f) {
                    strafeTicks = 10;
                    strafeDir = bot.getRandom().nextBoolean() ? 0.9f : -0.9f;
                }
            } else {
                bot.getNavigation().stop();
            }
            MeleeWeaponItem.Ability ability = bot.getMainHandItem().getItem() instanceof MeleeWeaponItem m ? m.stats().ability() : MeleeWeaponItem.Ability.NONE;
            // Dash au katana / wakizashi (surtout sous les tirs)
            boolean dasher = bot.role() == BotRole.SCOUT || ability == MeleeWeaponItem.Ability.DASH;
            if (dasher && dashCooldown <= 0 && dist > 4 && dist < 11 && bot.onGround()
                    && (now - bot.lastRangedHit < 20 || bot.getRandom().nextFloat() < 0.05f)) {
                Vec3 dir = target.position().subtract(bot.position()).normalize();
                bot.setDeltaMovement(dir.x * 1.5, 0.3, dir.z * 1.5);
                bot.hasImpulse = true;
                dashCooldown = 60;
                bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 1.4f);
            }
            // Lancer de tomahawk (assassins)
            if (bot.role() == BotRole.ASSASSIN && throwCooldown <= 0 && dist > 5 && dist < 16 && bot.getSensing().hasLineOfSight(target)) {
                TomahawkEntity t = new TomahawkEntity(bot.level(), bot, new ItemStack(ModItems.TOMAHAWK.get()));
                t.pickup = AbstractArrow.Pickup.DISALLOWED;
                Vec3 eye = bot.getEyePosition();
                t.setPos(eye.x, eye.y - 0.1, eye.z);
                double dx = target.getX() - bot.getX(), dz = target.getZ() - bot.getZ();
                t.shoot(dx, target.getY(0.5) - t.getY() + Math.sqrt(dx * dx + dz * dz) * 0.1, dz, 1.6f, 2f);
                bot.level().addFreshEntity(t);
                bot.swing(InteractionHand.MAIN_HAND);
                bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1f, 1.2f);
                throwCooldown = 70;
            }
            // Attaque
            if (attackCooldown <= 0 && bot.isWithinMeleeAttackRange(target) && bot.getSensing().hasLineOfSight(target)) {
                bot.doHurtTarget(target);
                boolean sweep = bot.role() == BotRole.BRUTE || ability == MeleeWeaponItem.Ability.WIDE_SWEEP || ability == MeleeWeaponItem.Ability.SHOCKWAVE;
                if (sweep && !bot.getMainHandItem().is(ModItems.DRAGONSLAYER.get())) {
                    sweepAround(target);
                }
                attackCooldown = cooldownTicks();
            }
        }

        private int cooldownTicks() {
            if (bot.getMainHandItem().is(ModItems.DRAGONSLAYER.get())) {
                return 32;
            }
            if (bot.getMainHandItem().getItem() instanceof MeleeWeaponItem m) {
                return Math.max(8, Math.round(20 / Math.max(0.4f, m.stats().speed())));
            }
            return 20;
        }

        /** Coup balayant : touche aussi les ennemis autour de la cible. */
        private void sweepAround(LivingEntity main) {
            float dmg = (float) bot.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.6f;
            for (LivingEntity e : bot.level().getEntitiesOfClass(LivingEntity.class, bot.getBoundingBox().inflate(3, 1, 3),
                    x -> x != bot && x != main && bot.shouldAttack(x))) {
                e.hurt(bot.damageSources().mobAttack(bot), dmg);
                e.knockback(0.5, bot.getX() - e.getX(), bot.getZ() - e.getZ());
            }
            bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 0.8f);
        }
    }

    // =====================================================================================================
    /** Servant de tourelle : vise la cible et tient la détente quand la tourelle est alignée. */
    static final class TurretGunner extends Goal {
        private final HumanBotEntity bot;
        private float scan;

        TurretGunner(HumanBotEntity bot) {
            this.bot = bot;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return bot.getVehicle() instanceof MountedGunEntity;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = bot.getTarget();
            if (t == null || !bot.getSensing().hasLineOfSight(t)) {
                // balayage lent de l'horizon
                scan += 0.6f;
                float yaw = bot.getYRot() + Mth.sin(scan * 0.05f) * 0.8f;
                bot.setYRot(yaw);
                bot.setYHeadRot(yaw);
                bot.setXRot(Mth.lerp(0.1f, bot.getXRot(), 0));
                return;
            }
            Vec3 to = t.getEyePosition().subtract(0, 0.4, 0).subtract(bot.getEyePosition());
            float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90;
            float pitch = (float) -(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * Mth.RAD_TO_DEG);
            float ny = Mth.approachDegrees(bot.getYRot(), yaw, 8);
            float np = Mth.approach(bot.getXRot(), pitch, 6);
            bot.setYRot(ny);
            bot.setYHeadRot(ny);
            bot.setXRot(np);
            if (Math.abs(Mth.wrapDegrees(ny - yaw)) < 6 && Math.abs(np - pitch) < 6 && bot.distanceTo(t) < 80) {
                bot.turretFireTicks = 3;
            }
        }
    }

    // =====================================================================================================
    /** Civils et marchands : fuient les coups de feu et les agresseurs. */
    static final class Flee extends Goal {
        private final HumanBotEntity bot;

        Flee(HumanBotEntity bot) {
            this.bot = bot;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !bot.faction().combatant() && bot.scaredUntil > bot.level().getGameTime();
        }

        @Override
        public void start() {
            run();
        }

        @Override
        public void tick() {
            if (bot.getNavigation().isDone()) {
                run();
            }
        }

        private void run() {
            Entity threat = bot.getLastHurtByMob() != null ? bot.getLastHurtByMob()
                    : bot.level().getNearestPlayer(bot, 24);
            Vec3 away = threat == null ? DefaultRandomPos.getPos(bot, 12, 5) : DefaultRandomPos.getPosAway(bot, 16, 6, threat.position());
            if (away != null) {
                bot.getNavigation().moveTo(away.x, away.y, away.z, 1.35);
            }
        }
    }

    // =====================================================================================================
    /** Patrouilles en binôme le long des avenues (autour du point d'attache), ou flânerie des civils. */
    static final class Patrol extends Goal {
        private final HumanBotEntity bot;
        private int wait;

        Patrol(HumanBotEntity bot) {
            this.bot = bot;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BotRole r = bot.role();
            return bot.getTarget() == null && !bot.isPassenger() && bot.squad == null && r != BotRole.SNIPER && r != BotRole.GUNNER
                    && r != BotRole.TRADER && r != BotRole.CONTRACTOR && bot.alertEnd() < bot.level().getGameTime();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return false;
        }

        @Override
        public void tick() {
            if (bot.partner != null && bot.level() instanceof ServerLevel sl && sl.getEntity(bot.partner) instanceof HumanBotEntity leader && leader.isAlive()) {
                if (bot.distanceToSqr(leader) > 9) {
                    bot.getNavigation().moveTo(leader, 0.75);
                }
                return;
            }
            if (--wait > 0 && !bot.getNavigation().isDone()) {
                return;
            }
            if (wait > 0) {
                return;
            }
            wait = 100 + bot.getRandom().nextInt(200);
            Vec3 dest;
            if (bot.home != null && bot.distanceToSqr(Vec3.atCenterOf(bot.home)) > (double) bot.homeRadius * bot.homeRadius) {
                dest = LandRandomPos.getPosTowards(bot, 12, 6, Vec3.atBottomCenterOf(bot.home));
            } else {
                dest = LandRandomPos.getPos(bot, 12, 6);
            }
            if (dest != null) {
                bot.getNavigation().moveTo(dest.x, dest.y, dest.z, 0.6);
            }
        }
    }

    // =====================================================================================================
    /** Escouades nomades (Faucons Blancs) : le chef choisit une destination lointaine, les autres le suivent. */
    static final class SquadMarch extends Goal {
        private final HumanBotEntity bot;
        private HumanBotEntity leader;

        SquadMarch(HumanBotEntity bot) {
            this.bot = bot;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return bot.squad != null && bot.getTarget() == null && !bot.isPassenger();
        }

        @Override
        public void tick() {
            if (bot.squadLeader) {
                if (bot.march == null || bot.distanceToSqr(Vec3.atBottomCenterOf(bot.march)) < 36 || bot.tickCount % 1200 == 0) {
                    double a = bot.getRandom().nextDouble() * Math.PI * 2, d = 60 + bot.getRandom().nextInt(60);
                    int x = (int) (bot.getX() + Math.cos(a) * d), z = (int) (bot.getZ() + Math.sin(a) * d);
                    bot.march = new BlockPos(x, bot.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                }
                if (bot.tickCount % 40 == 0 || bot.getNavigation().isDone()) {
                    Vec3 step = LandRandomPos.getPosTowards(bot, 20, 7, Vec3.atBottomCenterOf(bot.march));
                    if (step != null) {
                        bot.getNavigation().moveTo(step.x, step.y, step.z, 0.75);
                    }
                }
                return;
            }
            if (leader == null || !leader.isAlive() || bot.tickCount % 100 == 0) {
                leader = FalconSquads.leaderOf(bot);
            }
            if (leader == null) {
                bot.squadLeader = true;                 // le chef est tombé : un autre prend la tête
                return;
            }
            if (bot.distanceToSqr(leader) > 36 && bot.tickCount % 10 == 0) {
                bot.getNavigation().moveTo(leader, 0.85);
            }
        }
    }

    /** Petit utilitaire : jouer un son après un délai (fin du rechargement). */
    static final class DelayedSound {
        static void schedule(HumanBotEntity bot, int delay, net.minecraft.sounds.SoundEvent sound) {
            BotEvents.later(bot, delay, () -> bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(), sound, SoundSource.HOSTILE, 0.8f, 1.0f));
        }
    }

    @SuppressWarnings("unused")
    private static List<String> debug() {
        return List.of();
    }

    private BotGoals() {}
}

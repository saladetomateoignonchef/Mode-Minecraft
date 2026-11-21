package fr.modeminecraft.entity.military;

import fr.modeminecraft.entity.AreaCloudEntity;
import fr.modeminecraft.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Projectile lourd des véhicules : obus de char (flèche APFSDS, explosif HE-FRAG, charge creuse HEAT, missile tiré
 * par le canon), obus de canon automatique (AP-I, HE-I), grenades 40 mm, roquettes (Grad, S-8, Hydra, MLRS 227 mm
 * guidées GPS, ATACMS), missiles antichars guidés (suivent le réticule du tireur), missiles sol-air (autodirecteur IR).
 * Les fumigènes et le brouilleur Shtora font perdre le guidage ; la protection active Afganit les détruit en vol.
 */
public class ShellEntity extends Projectile {
    private static final EntityDataAccessor<Integer> SHELL = SynchedEntityData.defineId(ShellEntity.class, EntityDataSerializers.INT);

    private int launcher = -1;
    /** Les missiles en vol gardent leurs chunks chargés (sinon ils se figent hors de la distance de simulation). */
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> TICKET = net.minecraft.server.level.TicketType.create(
            "modeminecraft_missile", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 100);
    /** Couloir de vol préchargé au lancement (missiles sol-air à longue portée). */
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> PATH = net.minecraft.server.level.TicketType.create(
            "modeminecraft_missile_path", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 400);
    private net.minecraft.world.level.ChunkPos ticketHere, ticketAhead;
    /** Poussée relative du moteur-fusée (charge propulsive réduite des lanceurs fixes). */
    private float thrust = 1;
    /** Guidage : 0 aucun, 1 faisceau (réticule du tireur), 2 autodirecteur (cible), 3 GPS (point). */
    private int mode;
    private int guideId = -1;
    private Vec3 gpsTarget;
    private boolean jammed;
    private int life;

    public ShellEntity(EntityType<? extends ShellEntity> type, Level level) {
        super(type, level);
    }

    public ShellEntity(Level level, LivingEntity owner, MilAmmo.Shell shell, Entity launcher) {
        this(ModEntities.SHELL.get(), level);
        setOwner(owner);
        entityData.set(SHELL, shell.ordinal());
        this.launcher = launcher == null ? -1 : launcher.getId();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(SHELL, 0);
    }

    public MilAmmo.Shell shell() {
        return MilAmmo.Shell.values()[Math.floorMod(entityData.get(SHELL), MilAmmo.Shell.values().length)];
    }

    /** SACLOS : le missile suit le point visé par le tireur (réticule) tant que celui-ci garde la cible en vue. */
    public void beamRider(Entity shooter) {
        mode = 1;
        guideId = shooter == null ? -1 : shooter.getId();
    }

    public void home(Entity target) {
        if (target != null) {
            mode = 2;
            guideId = target.getId();
        }
    }

    public void gps(Vec3 target) {
        mode = 3;
        gpsTarget = target;
    }

    /**
     * Précharge les chunks entre le lanceur et la cible (et autour d'elle) : sans cela, un missile tiré au-delà de la
     * distance de simulation arrive sur des chunks pas encore chargés, se fige et n'atteint jamais sa cible.
     */
    public static void preloadPath(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 d = new Vec3(to.x - from.x, 0, to.z - from.z);
        double len = d.length();
        java.util.Set<Long> done = new java.util.HashSet<>();
        for (double t = 0; t <= len + 16; t += 12) {
            Vec3 p = len < 1 ? from : from.add(d.scale(Math.min(t, len) / len));
            var cp = new net.minecraft.world.level.ChunkPos(net.minecraft.core.BlockPos.containing(p));
            if (done.add(cp.toLong())) {
                sl.getChunkSource().addRegionTicket(PATH, cp, 3, cp);
            }
        }
    }

    /** Ticket de chargement sur le chunk du projectile, un peu plus loin sur sa route et autour de sa cible. */
    private void keepLoaded(ServerLevel sl) {
        if (life % 10 == 1 && (mode == 2 || mode == 4) && guideId >= 0 && sl.getEntity(guideId) instanceof Entity t) {
            sl.getChunkSource().addRegionTicket(TICKET, t.chunkPosition(), 3, t.chunkPosition());   // la cible continue de bouger
        }
        var here = chunkPosition();
        var ahead = new net.minecraft.world.level.ChunkPos(net.minecraft.core.BlockPos.containing(position().add(getDeltaMovement().scale(20))));
        if (!here.equals(ticketHere) || life % 20 == 0) {
            sl.getChunkSource().addRegionTicket(TICKET, here, 3, here);
            ticketHere = here;
        }
        if (!ahead.equals(ticketAhead) && !ahead.equals(here)) {
            sl.getChunkSource().addRegionTicket(TICKET, ahead, 3, ahead);
            ticketAhead = ahead;
        }
    }

    public void setThrust(float k) {
        thrust = k;
    }

    public void jam() {
        jammed = true;
    }

    /** Lancement à froid (S-400 / S-500) : éjecté à la verticale à 25 blocs, puis vire vers sa cible. */
    public void coldLaunch(Entity target) {
        mode = 4;
        guideId = target == null ? -1 : target.getId();
        setDeltaMovement(0, 1.6, 0);
        setNoGravity(true);
    }

    /** Missile balistique (simulé par Strikes) poursuivi par cet intercepteur. */
    private Strikes.Strike strike;

    /** Premier point de la trajectoire du missile balistique que l'intercepteur peut atteindre à temps. */
    public static Vec3 meetingPoint(Strikes.Strike s, Vec3 from, double speed, long now) {
        long rem = s.impact - now;
        for (int t = 1; t < rem; t++) {
            Vec3 p = s.position(now + t);
            if (p.distanceTo(from) <= speed * t) {
                return p;
            }
        }
        return s.position(Math.max(now, s.impact - 6));
    }

    /** Lancement à froid vers un missile balistique en vol : poursuite de sa position simulée, explosion au contact. */
    public void interceptStrike(Strikes.Strike s, long now) {
        coldLaunch(null);
        strike = s;
        gpsTarget = s.position(now);
    }

    /** Lancement à froid vers un point fixe (marqueur posé sur l'écran radar du S-400). */
    public void coldLaunchAt(Vec3 point) {
        coldLaunch(null);
        gpsTarget = point;
    }

    /** Missile balistique, montée (purement visuelle : le vol réel est simulé par Strikes). */
    public void visualAscent() {
        mode = 5;
        setDeltaMovement(0, 1.2, 0);
        setNoGravity(true);
    }

    /** Missile balistique, piqué final sur la cible (visuel, avec manœuvres d'évitement). */
    public void visualDive(Vec3 target, int ticks) {
        mode = 6;
        gpsTarget = target;
        Vec3 d = target.subtract(position());
        setDeltaMovement(d.scale(1.0 / ticks));
        setNoGravity(true);
    }

    /** Cible de cet intercepteur (pour le radar et les tests). */
    public int guideId() {
        return guideId;
    }

    public boolean visual() {
        return mode == 5 || mode == 6;
    }

    @Override
    public void tick() {
        super.tick();
        MilAmmo.Shell s = shell();
        if (++life > (s == MilAmmo.Shell.SAM_EXTREME || s == MilAmmo.Shell.SAM_LONG ? 900 : 600) || getY() < level().getMinBuildHeight() - 20) {
            discard();
            return;
        }
        if (mode == 5 || mode == 6) {
            // visuel : pas de collision
            if (mode == 5) {
                setDeltaMovement(getDeltaMovement().add(0, 0.05, 0));
            } else if (gpsTarget != null) {
                double wiggle = Math.sin(life * 0.7) * 0.35;                       // manœuvres d'évitement
                setDeltaMovement(getDeltaMovement().add(wiggle, 0, -wiggle));
            }
            Vec3 p = position().add(getDeltaMovement());
            setPos(p.x, p.y, p.z);
            updateRotation();
            if (level().isClientSide) {
                Vec3 back = position();
                level().addParticle(ParticleTypes.CLOUD, back.x, back.y, back.z, 0, 0, 0);
                level().addParticle(ParticleTypes.FLAME, back.x, back.y, back.z, 0, -0.1, 0);
            } else if (mode == 5 && life > 70 || mode == 6 && life > 45) {
                discard();
            }
            return;
        }
        if (level() instanceof ServerLevel sl) {
            keepLoaded(sl);
        }
        if (level() instanceof ServerLevel sl2 && strike != null && mode == 4) {
            long now = sl2.getGameTime();
            if (!Strikes.active().contains(strike) || life > 400) {
                Strikes.interceptFailed(strike);                                    // cible disparue (impact, déjà abattue)
                detonate(position());
                return;
            }
            // point de rencontre : premier instant où l'intercepteur peut atteindre la position future du missile
            gpsTarget = meetingPoint(strike, position(), Math.max(shell().speed, getDeltaMovement().length()), now);
            // fusée de proximité sur tout le tick (vitesses relatives de plus de 15 blocs / tick)
            Vec3 s0 = strike.position(now), s1 = strike.position(now + 1), v0 = getDeltaMovement();
            double closest = 1e9;
            for (int k = 0; k <= 6; k++) {
                double f = k / 6.0;
                closest = Math.min(closest, position().add(v0.scale(f)).distanceTo(s0.add(s1.subtract(s0).scale(f))));
            }
            if (life > 16 && closest < 12) {
                Strikes.resolveIntercept(sl2, strike, position());
                strike = null;
                detonate(position());
                return;
            }
        }
        if (!level().isClientSide && mode == 4 && guideId < 0 && gpsTarget != null && strike == null && life > 16
                && (position().distanceTo(gpsTarget) < 4 || getDeltaMovement().dot(gpsTarget.subtract(position())) < 0 && life > 40)) {
            detonate(position());                                                  // point du radar atteint : fusée de proximité
            return;
        }
        if (!level().isClientSide && mode == 4 && guideId < 0 && gpsTarget == null && life > 30) {
            // intercepteur d'un missile balistique (vol simulé) : explosion de proximité en haute altitude
            ((ServerLevel) level()).sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            discard();
            return;
        }
        if (!level().isClientSide && mode == 4 && life < 16) {
            setDeltaMovement(0, 1.6, 0);                                           // éjection verticale (charge de poudre)
        } else if (!level().isClientSide && mode != 0 && !jammed) {
            guide(s);
        }
        // fusée de proximité des missiles sol-air
        if (!level().isClientSide && (mode == 2 || mode == 4) && guideId >= 0) {
            Entity t = level().getEntity(guideId);
            // distance au segment parcouru pendant ce tick (à 5 blocs/tick, un simple test de position le « saute »)
            Vec3 c = t == null ? null : t.position().add(0, t.getBbHeight() / 2, 0);
            Vec3 step = getDeltaMovement();
            double k = c == null || step.lengthSqr() < 1e-6 ? 0 : Mth.clamp(c.subtract(position()).dot(step) / step.lengthSqr(), 0, 1);
            double fuse = 4.5 + Math.max(t == null ? 0 : t.getBbWidth(), t == null ? 0 : t.getBbHeight()) * 0.5;
            if (t != null && position().add(step.scale(k)).distanceTo(c) < fuse) {
                if (t instanceof ShellEntity || t instanceof fr.modeminecraft.entity.RocketEntity) {
                    t.discard();                                                     // missile ou roquette abattu
                } else {
                    t.hurt(getOwner() instanceof LivingEntity le ? damageSources().explosion(this, le) : damageSources().generic(), s.damage);
                }
                detonate(position());
                return;
            }
        }
        Vec3 v = getDeltaMovement();
        if (!isNoGravity()) {
            v = v.add(0, -s.gravity, 0);
        }
        // roquettes : poussée pendant la combustion (les missiles gardent leur vitesse)
        if (s.rocket() && life < 40 && v.length() < s.speed * thrust) {
            v = v.normalize().scale(Math.min(s.speed * thrust, v.length() + 0.12));
        }
        setDeltaMovement(v);
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this, hit)) {
            onHit(hit);
            if (isRemoved()) {
                return;
            }
        }
        Vec3 p = position().add(getDeltaMovement());
        setPos(p.x, p.y, p.z);
        updateRotation();
        if (level().isClientSide) {
            trail(s);
        }
    }

    private void trail(MilAmmo.Shell s) {
        Vec3 back = position().subtract(getDeltaMovement().scale(0.5));
        if (s.rocket()) {
            level().addParticle(ParticleTypes.FLAME, back.x, back.y, back.z, 0, 0, 0);
            level().addParticle(life < 40 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, back.x, back.y, back.z, 0, 0.01, 0);
            if (s == MilAmmo.Shell.ATACMS) {
                level().addParticle(ParticleTypes.CLOUD, back.x, back.y, back.z, 0, 0, 0);
            }
        } else if (s == MilAmmo.Shell.HE || s == MilAmmo.Shell.HEAT) {
            level().addParticle(ParticleTypes.SMOKE, back.x, back.y, back.z, 0, 0, 0);
        }
    }

    private void guide(MilAmmo.Shell s) {
        Vec3 aim = null;
        double turn = 4;
        if (mode == 1) {
            Entity shooter = level().getEntity(guideId);
            if (shooter != null && shooter.isAlive()) {
                Vec3 eye = shooter.getEyePosition();
                Vec3 end = eye.add(shooter.getLookAngle().scale(600));
                var bh = level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
                Vec3 to = bh.getType() == HitResult.Type.MISS ? end : bh.getLocation();
                var eh = ProjectileUtil.getEntityHitResult(level(), shooter, eye, to, shooter.getBoundingBox().expandTowards(to.subtract(eye)).inflate(1),
                        e -> !e.isSpectator() && e.isPickable() && e != shooter.getVehicle() && !(e instanceof MilPart mp && mp.getParent() == shooter.getVehicle()));
                aim = eh != null ? eh.getLocation() : to;
                // missile sur l'axe du faisceau : on vise un point un peu devant le missile, sur la ligne de visée
                Vec3 axis = to.subtract(eye).normalize();
                double along = position().subtract(eye).dot(axis) + 30;
                Vec3 onBeam = eye.add(axis.scale(Math.min(along, aim.distanceTo(eye))));
                aim = onBeam;
                turn = 5;
            }
        } else if (mode == 4 && guideId < 0 && gpsTarget != null) {
            aim = gpsTarget;
            turn = strike != null ? 14 : 9;
        } else if (mode == 2 || mode == 4) {
            Entity t = level().getEntity(guideId);
            if (t != null && t.isAlive()) {
                aim = t.position().add(0, t.getBbHeight() / 2, 0).add(t.getDeltaMovement().scale(position().distanceTo(t.position()) / Math.max(1, s.speed)));
                turn = s == MilAmmo.Shell.SAM_AGILE || s == MilAmmo.Shell.ABM ? 14 : 9;
                // brouilleur Shtora du char visé
                if (t instanceof MilVehicleEntity v && position().distanceToSqr(t.position()) < 60 * 60 && life % 10 == 0 && v.shtora()) {
                    jammed = true;
                    return;
                }
            }
        } else if (mode == 3 && gpsTarget != null) {
            Vec3 flat = new Vec3(gpsTarget.x - getX(), 0, gpsTarget.z - getZ());
            // trajectoire en cloche : montée tant qu'on est loin, puis piqué sur les coordonnées
            if (life < 25) {
                aim = position().add(flat.normalize().scale(10)).add(0, 12, 0);
            } else {
                aim = gpsTarget;
            }
            turn = 3;
        }
        if (aim == null) {
            return;
        }
        // fumigène entre le missile et sa cible : le guidage décroche
        if ((mode == 1 || mode == 2) && life % 5 == 0 && smokeBetween(position(), aim)) {
            jammed = true;
            return;
        }
        Vec3 v = getDeltaMovement();
        double speed = Math.max(v.length(), s.speed * 0.8);
        Vec3 cur = v.normalize(), want = aim.subtract(position()).normalize();
        double angle = Math.acos(Mth.clamp(cur.dot(want), -1, 1));
        double max = Math.toRadians(turn);
        Vec3 next = angle <= max ? want : cur.add(want.subtract(cur).scale(max / angle)).normalize();
        setDeltaMovement(next.scale(speed));
        setNoGravity(true);
        hasImpulse = true;
    }

    private boolean smokeBetween(Vec3 a, Vec3 b) {
        for (AreaCloudEntity c : level().getEntitiesOfClass(AreaCloudEntity.class, new net.minecraft.world.phys.AABB(a, b).inflate(5),
                c -> c.cloudType() == AreaCloudEntity.Type.SMOKE)) {
            Vec3 ab = b.subtract(a);
            double t = Mth.clamp(c.position().subtract(a).dot(ab) / Math.max(1e-6, ab.lengthSqr()), 0, 1);
            if (a.add(ab.scale(t)).distanceTo(c.position()) < c.radius()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (visual()) {
            return false;
        }
        if (!super.canHitEntity(e) && !(e instanceof MilPart)) {
            return false;
        }
        Entity l = launcher < 0 ? null : level().getEntity(launcher);
        if (l != null && (e == l || l.hasPassenger(e) || e instanceof MilPart mp && mp.getParent() == l)) {
            return false;
        }
        return !(e instanceof ShellEntity);
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        if (level().isClientSide) {
            return;
        }
        Entity e = r.getEntity();
        MilAmmo.Shell s = shell();
        Entity owner = getOwner();
        MilVehicleEntity v = e instanceof MilVehicleEntity mv ? mv : e instanceof MilPart mp ? mp.getParent() : null;
        if (v != null) {
            v.shellHit(this, r.getLocation());
        } else {
            var src = owner instanceof LivingEntity le ? damageSources().mobProjectile(this, le) : damageSources().generic();
            e.hurt(src, s.damage * (e instanceof fr.modeminecraft.entity.VehicleEntity ? 2f : 1f));
            if (s == MilAmmo.Shell.AP_I || s == MilAmmo.Shell.HE_I) {
                e.igniteForSeconds(3);
            }
        }
        detonate(r.getLocation());
    }

    @Override
    protected void onHitBlock(BlockHitResult r) {
        if (!level().isClientSide) {
            detonate(r.getLocation());
        }
    }

    private void detonate(Vec3 at) {
        MilAmmo.Shell s = shell();
        ServerLevel sl = (ServerLevel) level();
        if (s.blast > 0) {
            Level.ExplosionInteraction mode = s.blast >= 1.4f ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
            sl.explode(this, getOwner() instanceof LivingEntity le ? damageSources().explosion(this, le) : null, null,
                    at.x, at.y, at.z, s.blast, s == MilAmmo.Shell.HE_I, mode);
            if (s.blast >= 4) {
                fr.modeminecraft.entity.bot.CivilianLife.panic(sl, at, 40, 200);
            }
        } else {
            sl.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.3);
        }
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 256 * 256;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Shell", entityData.get(SHELL));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SHELL, tag.getInt("Shell"));
    }
}

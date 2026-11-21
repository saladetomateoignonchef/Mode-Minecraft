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

    public void jam() {
        jammed = true;
    }

    @Override
    public void tick() {
        super.tick();
        MilAmmo.Shell s = shell();
        if (++life > 600 || getY() < level().getMinBuildHeight() - 20) {
            discard();
            return;
        }
        if (!level().isClientSide && mode != 0 && !jammed) {
            guide(s);
        }
        Vec3 v = getDeltaMovement();
        if (!isNoGravity()) {
            v = v.add(0, -s.gravity, 0);
        }
        // roquettes : poussée pendant la combustion (les missiles gardent leur vitesse)
        if (s.rocket() && life < 40 && v.length() < s.speed) {
            v = v.normalize().scale(Math.min(s.speed, v.length() + 0.12));
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
        } else if (mode == 2) {
            Entity t = level().getEntity(guideId);
            if (t != null && t.isAlive()) {
                aim = t.position().add(0, t.getBbHeight() / 2, 0).add(t.getDeltaMovement().scale(position().distanceTo(t.position()) / Math.max(1, s.speed)));
                turn = 9;
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

package fr.modeminecraft.entity;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.AmmoType;
import fr.modeminecraft.item.gun.TriggerHandler;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.function.Supplier;

/**
 * Tourelle / mitrailleuse lourde déployable (cahier des charges "tourelles") : on ne la tient pas à la main.
 *
 * - Pose : clic droit sur le dessus d'un bloc solide, orientée comme le regard ; les pieds s'écartent (0,8 s).
 * - Munitions obligatoires : le réservoir est vide à la pose ; clic droit avec la caisse compatible = rechargement
 *   (capot ouvert, 1,5 s). Caisse vide : la bande disparaît et la détente fait "clic".
 * - Montée : clic droit (main nue ou autre objet) ; Shift pour descendre. Clic gauche = tir, clic droit = visée.
 * - Démontage : Shift + clic droit avec la clé à molette, ou Shift + clic droit main nue maintenu 1,5 s.
 *   Les munitions restantes sont gardées dans l'objet.
 * - Angles : trépieds 90° de chaque côté, -25° à +45° ; Flakvierling 360° et jusqu'à +75°.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public class MountedGunEntity extends Entity {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AMMO = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> GUN_YAW = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GUN_PITCH = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEAT = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LAST_SHOT = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BURST = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHOTS = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RELOAD_END = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OVERHEAT_END = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEPLOY = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.INT);

    public static final int DEPLOY_TICKS = 16;
    public static final int RELOAD_TICKS = 30;
    public static final int OVERHEAT_TICKS = 70;
    /** Le tireur est assis : ses yeux sont 1,02 bloc au-dessus de son point d'attache (0,6 + 1,62 - 0,6... cf. Player). */
    private static final double SEATED_EYE = 1.02;

    public enum Projectile { BULLET, FLAK, GRENADE }

    /** Les modèles de tourelles. */
    public enum Kind {
        //   nom            dégâts cadence capacité  caisse              par caisse chauffe  armure perf. fragile recul  projectile      arc  haut bas  vitesse disp. son                        hauteur  largeur haut.
        M2("m2_browning",    14f,  550, 100, "ammo_box_50",       100, 1 / 160f, 0.65f, 1, true,  1.2f, Projectile.BULLET, 90, 45, 25, 7.0f, 0.7f, ModSounds.SHOT_HEAVY, 1.0f, 1.3f, 1.1f),
        DSHK("dshk",         13f,  600, 100, "ammo_box_50",       100, 1 / 150f, 0.6f,  1, true,  1.0f, Projectile.BULLET, 90, 45, 25, 7.0f, 0.8f, ModSounds.SHOT_HEAVY, 0.9f, 1.3f, 1.1f),
        MG42("mg42",         6.5f, 1200, 250, "ammo_box_792",     250, 1 / 80f,  0.25f, 0, false, 0,    Projectile.BULLET, 90, 45, 25, 6.0f, 1.3f, ModSounds.SHOT_BATTLE, 1.3f, 1.2f, 1.1f),
        VICKERS("vickers",   7.5f, 600, 250, "ammo_belt_303",     250, 0,        0.3f,  0, false, 0,    Projectile.BULLET, 90, 45, 25, 6.0f, 0.9f, ModSounds.SHOT_RIFLE, 0.85f, 1.3f, 1.2f),
        FLAK("flakvierling", 16f,  400, 80,  "flak_magazine_20mm", 20, 0,        0.5f,  0, false, 0.6f, Projectile.FLAK,  180, 75, 10, 6.0f, 0.6f, ModSounds.SHOT_HEAVY, 0.7f, 2.4f, 1.9f),
        MK19("mk19",         22f,  250, 32,  "ammo_box_40mm",      32, 0,        0f,    0, false, 0,    Projectile.GRENADE, 90, 45, 25, 1.7f, 1.0f, ModSounds.SHOT_LAUNCHER, 0.9f, 1.3f, 1.1f);

        public final String id;
        public final float damage, rpm, heatPerShot, armorPen, knockback, velocity, inaccuracy, soundPitch, width, height;
        public final int capacity, perLoad, pierce, yawArc, pitchUp, pitchDown;
        public final String ammoItem;
        public final boolean breaksFragile;
        public final Projectile projectile;
        private final Supplier<SoundEvent> sound;

        Kind(String id, float damage, float rpm, int capacity, String ammoItem, int perLoad, float heatPerShot, float armorPen, int pierce,
             boolean breaksFragile, float knockback, Projectile projectile, int yawArc, int pitchUp, int pitchDown, float velocity,
             float inaccuracy, Supplier<SoundEvent> sound, float soundPitch, float width, float height) {
            this.id = id;
            this.damage = damage;
            this.rpm = rpm;
            this.capacity = capacity;
            this.ammoItem = ammoItem;
            this.perLoad = perLoad;
            this.heatPerShot = heatPerShot;
            this.armorPen = armorPen;
            this.pierce = pierce;
            this.breaksFragile = breaksFragile;
            this.knockback = knockback;
            this.projectile = projectile;
            this.yawArc = yawArc;
            this.pitchUp = pitchUp;
            this.pitchDown = pitchDown;
            this.velocity = velocity;
            this.inaccuracy = inaccuracy;
            this.sound = sound;
            this.soundPitch = soundPitch;
            this.width = width;
            this.height = height;
        }

        public Item item() {
            return BuiltInRegistries.ITEM.get(ModeMinecraft.id(id));
        }

        public Item ammo() {
            return BuiltInRegistries.ITEM.get(ModeMinecraft.id(ammoItem));
        }

        public TurretGeometry.Geo geo() {
            return TurretGeometry.get(id);
        }

        /** Ne surchauffe jamais (refroidissement à eau, obus). */
        public boolean coolsItself() {
            return heatPerShot <= 0;
        }

        /** Refroidissement par tick quand on ne tire pas. */
        public float cooling() {
            return this == MG42 ? 0.04f : 0.008f;
        }

        public static Kind byItem(Item item) {
            for (Kind k : values()) {
                if (k.item() == item) {
                    return k;
                }
            }
            return null;
        }
    }

    private int firingTicks;
    private int shotsThisPull;
    private boolean triggerWasDown;
    private int pendingLoad;
    private int dismantleProgress;
    private int lastDismantleTick = -100;

    public MountedGunEntity(EntityType<? extends MountedGunEntity> type, Level level) {
        super(type, level);
    }

    public MountedGunEntity(Level level, Kind kind, double x, double y, double z, float yaw, int ammo) {
        this(ModEntities.MOUNTED_GUN.get(), level);
        setPos(x, y, z);
        setYRot(yaw);
        yRotO = yaw;
        entityData.set(KIND, kind.ordinal());
        entityData.set(GUN_YAW, yaw);
        entityData.set(AMMO, Math.min(ammo, kind.capacity));
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0).define(AMMO, 0).define(GUN_YAW, 0f).define(GUN_PITCH, 0f).define(HEAT, 0f)
                .define(LAST_SHOT, -1000).define(BURST, 0).define(SHOTS, 0).define(RELOAD_END, 0).define(OVERHEAT_END, 0)
                .define(DEPLOY, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (KIND.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        Kind k = kind();
        return EntityDimensions.scalable(k.width, k.height);
    }

    // ------------------------------------------------------------------ état
    public Kind kind() {
        return Kind.values()[Mth.clamp(entityData.get(KIND), 0, Kind.values().length - 1)];
    }

    public int ammo() {
        return entityData.get(AMMO);
    }

    public float heat() {
        return entityData.get(HEAT);
    }

    public int lastShot() {
        return entityData.get(LAST_SHOT);
    }

    public int burst() {
        return entityData.get(BURST);
    }

    public int shotsFired() {
        return entityData.get(SHOTS);
    }

    public int deployTicks() {
        return entityData.get(DEPLOY);
    }

    private int now() {
        return (int) level().getGameTime();
    }

    /** Progression du rechargement (0 à 1), ou -1 si on ne recharge pas. */
    public float reloadProgress(float partialTick) {
        int end = entityData.get(RELOAD_END);
        if (end == 0) {
            return -1;
        }
        float left = end - (level().getGameTime() + partialTick);
        return left <= 0 ? -1 : Mth.clamp(1 - left / RELOAD_TICKS, 0, 1);
    }

    public boolean isOverheated() {
        return entityData.get(OVERHEAT_END) > now();
    }

    /** Direction de l'arme : celle du regard du tireur (bornée), sinon la dernière connue. */
    public float gunYaw(float partialTick) {
        if (getFirstPassenger() instanceof Player p) {
            return clampYaw(Mth.rotLerp(partialTick, p.yRotO, p.getYRot()));
        }
        return entityData.get(GUN_YAW);
    }

    public float gunPitch(float partialTick) {
        if (getFirstPassenger() instanceof Player p) {
            return clampPitch(Mth.lerp(partialTick, p.xRotO, p.getXRot()));
        }
        return entityData.get(GUN_PITCH);
    }

    private float clampYaw(float yaw) {
        Kind k = kind();
        if (k.yawArc >= 180) {
            return yaw;
        }
        return getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - getYRot()), -k.yawArc, k.yawArc);
    }

    private float clampPitch(float pitch) {
        return Mth.clamp(pitch, -kind().pitchUp, kind().pitchDown);
    }

    // ------------------------------------------------------------------ tireur
    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && deployTicks() >= DEPLOY_TICKS;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        float yaw = entityData.get(GUN_YAW);
        passenger.setYRot(yaw);
        passenger.yRotO = yaw;
        passenger.setXRot(0);
        passenger.xRotO = 0;
    }

    /** Le tireur est derrière les poignées : yeux au point "œil à la hanche" du modèle (il tourne avec l'arme). */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        TurretGeometry.Geo geo = kind().geo();
        if (geo == null) {
            return new Vec3(0, 0.3, 0);
        }
        float yaw = getFirstPassenger() == passenger ? clampYaw(passenger.getYRot()) : entityData.get(GUN_YAW);
        Vec3 eye = TurretGeometry.offset(geo.pivot(), yaw, 0, geo.hipEye());
        return eye.subtract(0, SEATED_EYE, 0);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        clampRotation(passenger);
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        clampRotation(passenger);
    }

    /** L'arme ne pivote que dans son arc : le regard du tireur est borné pareil (comme dans un bateau). */
    private void clampRotation(Entity p) {
        float yaw = p.getYRot();
        float clamped = clampYaw(yaw);
        if (clamped != yaw) {
            p.yRotO += clamped - yaw;
            p.setYRot(clamped);
        }
        p.setYHeadRot(p.getYRot());
        p.setYBodyRot(p.getYRot());
        float pitch = clampPitch(p.getXRot());
        if (pitch != p.getXRot()) {
            p.setXRot(pitch);
            p.xRotO = clampPitch(p.xRotO);
        }
    }

    // ------------------------------------------------------------------ interactions
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || isVehicle()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        Kind kind = kind();
        // Caisse de munitions compatible : rechargement
        if (!held.isEmpty() && held.getItem() == kind.ammo()) {
            if (!level().isClientSide) {
                load(player, held);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive()) {
            if (held.is(ModItems.WRENCH.get())) {
                if (!level().isClientSide) {
                    dismantle(player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (held.isEmpty()) {
                // Shift + clic droit maintenu à main nue : 1,5 s
                if (!level().isClientSide) {
                    int gap = tickCount - lastDismantleTick;
                    dismantleProgress = gap <= 6 ? dismantleProgress + gap : 0;
                    lastDismantleTick = tickCount;
                    if (dismantleProgress >= 30) {
                        dismantle(player);
                    } else {
                        player.displayClientMessage(Component.translatable("message.modeminecraft.turret.dismantling",
                                Math.round(dismantleProgress * 100f / 30)).withStyle(ChatFormatting.YELLOW), true);
                    }
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            return InteractionResult.PASS;
        }
        if (deployTicks() < DEPLOY_TICKS) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void load(Player player, ItemStack box) {
        Kind kind = kind();
        if (reloadProgress(0) >= 0) {
            return;
        }
        if (ammo() + pendingLoad >= kind.capacity) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.turret.full").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        pendingLoad = kind.perLoad;
        entityData.set(RELOAD_END, now() + RELOAD_TICKS);
        if (!player.getAbilities().instabuild) {
            box.shrink(1);
        }
        level().playSound(null, getX(), getY(), getZ(), ModSounds.TURRET_RELOAD.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
    }

    private void dismantle(Player player) {
        ItemStack stack = new ItemStack(kind().item());
        if (ammo() + pendingLoad > 0) {
            stack.set(ModDataComponents.TURRET_AMMO.get(), Math.min(kind().capacity, ammo() + pendingLoad));
        }
        ejectPassengers();
        level().playSound(null, getX(), getY(), getZ(), ModSounds.TURRET_DEPLOY.get(), SoundSource.PLAYERS, 1.0f, 0.8f);
        if (!player.getInventory().add(stack)) {
            spawnAtLocation(stack);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Indestructible, sauf en créatif (frapper la retire, comme un support d'armure)
        if (!level().isClientSide && !isRemoved() && source.getDirectEntity() instanceof Player p && p.getAbilities().instabuild
                && getPassengers().isEmpty()) {
            discard();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ tir
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (deployTicks() < DEPLOY_TICKS) {
            entityData.set(DEPLOY, deployTicks() + 1);
        }
        int end = entityData.get(RELOAD_END);
        if (end != 0 && now() >= end) {
            entityData.set(RELOAD_END, 0);
            entityData.set(AMMO, Math.min(kind().capacity, ammo() + pendingLoad));
            pendingLoad = 0;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.RACK.get(), SoundSource.PLAYERS, 1.2f, 0.7f);
        }
        Entity rider = getFirstPassenger();
        if (rider instanceof Player player) {
            entityData.set(GUN_YAW, clampYaw(player.getYRot()));
            entityData.set(GUN_PITCH, clampPitch(player.getXRot()));
            fire(player);
        } else {
            stopFiring();
        }
        if (isOverheated() && tickCount % 3 == 0) {
            Vec3 m = muzzle(0);
            ((ServerLevel) level()).sendParticles(ParticleTypes.LARGE_SMOKE, m.x, m.y, m.z, 1, 0.1, 0.05, 0.1, 0.01);
        }
    }

    private void stopFiring() {
        firingTicks = 0;
        shotsThisPull = 0;
        triggerWasDown = false;
        if (burst() != 0) {
            entityData.set(BURST, 0);
        }
        float heat = heat();
        if (heat > 0 && !isOverheated()) {
            entityData.set(HEAT, Math.max(0, heat - kind().cooling()));
        }
    }

    private void fire(Player player) {
        if (deployTicks() < DEPLOY_TICKS || reloadProgress(0) >= 0 || !TriggerHandler.isTriggerDown(player)) {
            stopFiring();
            return;
        }
        if (isOverheated()) {
            if (!triggerWasDown) {
                player.displayClientMessage(Component.translatable("message.modeminecraft.turret.overheated").withStyle(ChatFormatting.RED), true);
            }
            triggerWasDown = true;
            return;
        }
        if (ammo() <= 0) {
            if (!triggerWasDown) {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.TURRET_DRY_FIRE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                player.displayClientMessage(Component.translatable("message.modeminecraft.turret.empty", kind().ammo().getDescription())
                        .withStyle(ChatFormatting.RED), true);
            }
            triggerWasDown = true;
            return;
        }
        triggerWasDown = true;
        Kind kind = kind();
        float interval = 1200f / kind.rpm;
        int due = (int) Math.floor(firingTicks / interval) + 1 - shotsThisPull;
        firingTicks++;
        for (int i = 0; i < due && ammo() > 0 && !isOverheated(); i++) {
            shootOnce(player);
            shotsThisPull++;
        }
    }

    /** Bouche du canon n (Flakvierling : les 4 canons tirent à tour de rôle), en coordonnées du monde. */
    public Vec3 muzzle(int shot) {
        TurretGeometry.Geo geo = kind().geo();
        var muzzles = geo.muzzles();
        return position().add(TurretGeometry.offset(geo.pivot(), entityData.get(GUN_YAW), entityData.get(GUN_PITCH),
                muzzles.get(Math.floorMod(shot, muzzles.size()))));
    }

    private void shootOnce(Player player) {
        ServerLevel level = (ServerLevel) level();
        Kind kind = kind();
        TurretGeometry.Geo geo = kind.geo();
        float yaw = entityData.get(GUN_YAW), pitch = entityData.get(GUN_PITCH);
        int shot = shotsFired();
        Vec3 muzzle = muzzle(shot);
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);
        // Les balles convergent vers la ligne de visée à 60 blocs (le viseur est au-dessus du canon)
        Vec3 sightLine = position().add(TurretGeometry.offset(geo.pivot(), yaw, pitch, geo.adsEye()));
        Vec3 dir = kind.projectile == Projectile.GRENADE ? look : sightLine.add(look.scale(60)).subtract(muzzle).normalize();
        net.minecraft.world.entity.projectile.Projectile projectile;
        if (kind.projectile == Projectile.GRENADE) {
            projectile = new RocketEntity(ModEntities.GRENADE.get(), level, player, 1.75f); // rayon d'explosion ~3,5 blocs
        } else {
            BulletEntity b = new BulletEntity(level, player, kind.damage, kind.pierce, false, AmmoType.STANDARD);
            b.setCaliberClass(kind == Kind.MG42 || kind == Kind.VICKERS ? 1 : 2);
            b.setArmorPenetration(kind.armorPen);
            b.setHeavy(kind.breaksFragile, kind.knockback, kind.projectile == Projectile.FLAK ? 0.75f : 0);
            b.setBallistics(0.002f, 60, 100);
            projectile = b;
        }
        projectile.setPos(muzzle);
        projectile.shoot(dir.x, dir.y, dir.z, kind.velocity, kind.inaccuracy);
        level.addFreshEntity(projectile);

        entityData.set(AMMO, ammo() - 1);
        entityData.set(SHOTS, shot + 1);
        entityData.set(LAST_SHOT, now());
        entityData.set(BURST, burst() + 1);
        if (kind.heatPerShot > 0) {
            float heat = heat() + kind.heatPerShot;
            if (heat >= 1) {
                heat = 1;
                entityData.set(OVERHEAT_END, now() + OVERHEAT_TICKS);
                level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.2f, 0.6f);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 12, 0.3, 0.15, 0.3, 0.02);
                player.displayClientMessage(Component.translatable("message.modeminecraft.turret.overheated").withStyle(ChatFormatting.RED), true);
            }
            entityData.set(HEAT, heat);
        }
        level.playSound(null, muzzle.x, muzzle.y, muzzle.z, kind.sound.get(), SoundSource.PLAYERS, 9f,
                kind.soundPitch + (random.nextFloat() - 0.5f) * 0.06f);
        // Fumée voxelisée et étincelles à la bouche du canon
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.05, 0.05, 0.05, 0.02);
        level.sendParticles(ParticleTypes.CRIT, muzzle.x, muzzle.y, muzzle.z, 3, 0.05, 0.05, 0.05, 0.25);
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 1, 0.02, 0.02, 0.02, 0.01);
        if (shot % 4 == 0) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, muzzle.x, muzzle.y, muzzle.z, 1, 0.05, 0.05, 0.05, 0.005);
        }
        // Vickers : gros nuages de vapeur par le tuyau du manchon pendant les longues rafales
        if (kind == Kind.VICKERS && burst() > 30 && shot % 3 == 0 && geo.steam() != null) {
            Vec3 s = position().add(TurretGeometry.offset(geo.pivot(), yaw, pitch, geo.steam()));
            level.sendParticles(ParticleTypes.CLOUD, s.x, s.y, s.z, 3, 0.08, 0.1, 0.08, 0.03);
        }
    }

    // ------------------------------------------------------------------ bouclier du Vickers
    /** Le bouclier d'acier du Vickers arrête les projectiles tirés de face vers le torse et la tête du tireur. */
    @SubscribeEvent
    public static void onRiderHurt(LivingIncomingDamageEvent event) {
        var victim = event.getEntity();
        if (!(victim.getVehicle() instanceof MountedGunEntity gun) || gun.kind() != Kind.VICKERS
                || !(event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile proj)) {
            return;
        }
        Vec3 dir = proj.getDeltaMovement().normalize();
        Vec3 front = Vec3.directionFromRotation(0, gun.entityData.get(GUN_YAW));
        boolean upper = proj.getY() > victim.getY() + 0.5;
        if (dir.dot(front) < -0.35 && upper) {
            event.setCanceled(true);
            proj.discard();
            victim.level().playSound(null, proj.getX(), proj.getY(), proj.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.4f);
            if (victim.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, proj.getX(), proj.getY(), proj.getZ(), 6, 0.1, 0.1, 0.1, 0.2);
            }
        }
    }

    // ------------------------------------------------------------------ sauvegarde
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(KIND, tag.getInt("Kind"));
        entityData.set(AMMO, tag.contains("Ammo") ? tag.getInt("Ammo") : tag.getInt("Belt"));
        entityData.set(GUN_YAW, tag.getFloat("GunYaw"));
        entityData.set(GUN_PITCH, tag.getFloat("GunPitch"));
        entityData.set(HEAT, tag.getFloat("Heat"));
        entityData.set(DEPLOY, DEPLOY_TICKS);
        pendingLoad = tag.getInt("PendingLoad");
        if (pendingLoad > 0) {
            entityData.set(AMMO, Math.min(kind().capacity, ammo() + pendingLoad)); // rechargement interrompu : terminé
            pendingLoad = 0;
        }
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", entityData.get(KIND));
        tag.putInt("Ammo", ammo());
        tag.putFloat("GunYaw", entityData.get(GUN_YAW));
        tag.putFloat("GunPitch", entityData.get(GUN_PITCH));
        tag.putFloat("Heat", heat());
        tag.putInt("PendingLoad", pendingLoad);
    }
}

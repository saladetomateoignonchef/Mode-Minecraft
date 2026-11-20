package fr.modeminecraft.entity.drone;

import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.entity.RocketEntity;
import fr.modeminecraft.entity.ThrownGrenadeEntity;
import fr.modeminecraft.item.explosive.GrenadeItem;
import fr.modeminecraft.item.explosive.PlaceableExplosiveItem;
import fr.modeminecraft.item.gun.AmmoType;
import fr.modeminecraft.network.DronePilotPayload;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Drone de combat pilotable (5 modèles, voir {@link DroneType}).
 * - associé à une télécommande (Maj + clic droit), piloté en vue caméra FPV (Z/Q/S/D, Espace, Ctrl, souris) ;
 * - Retour Automatique (RTH) : quand on quitte la caméra, sur la touche R, ou si le signal est perdu ;
 * - batterie de 5 minutes (10 avec la batterie LiPo-Titan), rechargée avec une pile CR123A ;
 * - armement selon le modèle : marquage, charge kamikaze, largage de grenades, mitrailleuse, roquettes guidées.
 */
public class DroneEntity extends LivingEntity {
    public static final byte IDLE = 0, PILOT = 1, RTH = 2, DOWN = 3;
    public static final int BATTERY = 6000;
    private static final TicketType<ChunkPos> TICKET = TicketType.create("modeminecraft_drone", Comparator.comparingLong(ChunkPos::toLong), 60);

    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> BATTERY_LEFT = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ARMS = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PILOT_ID = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> JAMMED_UNTIL = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> SILENT = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HIGH_CAP = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CARGO = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> PAYLOAD = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> INTERFERENCE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);

    public UUID owner;
    /** Bot opérateur (drone ennemi) et sa faction. */
    public int ownerBotId = -1;
    private String botFaction;
    /** Opérateur bot (drone ennemi) : le drone poursuit seul sa cible. */
    public int botTarget = -1;
    private final SimpleContainer cargo = new SimpleContainer(9);
    // dernière commande reçue du pilote
    private float inForward, inStrafe, inVertical;
    private boolean inFire, lastFire;
    private long inputAt;
    private int fireCooldown, salvo;
    private int salvoTarget = -1;
    public float rotorSpin, rotorSpinO;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.FLYING_SPEED, 0.6);
    }

    // ------------------------------------------------------------------ données
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(TYPE, 0);
        b.define(MODE, IDLE);
        b.define(BATTERY_LEFT, BATTERY);
        b.define(ARMS, 0);
        b.define(PILOT_ID, -1);
        b.define(JAMMED_UNTIL, 0L);
        b.define(SILENT, false);
        b.define(HIGH_CAP, false);
        b.define(CARGO, false);
        b.define(PAYLOAD, ItemStack.EMPTY);
        b.define(INTERFERENCE, 0f);
    }

    public DroneType droneType() {
        return DroneType.values()[Math.floorMod(entityData.get(TYPE), DroneType.values().length)];
    }

    public void setDroneType(DroneType t) {
        entityData.set(TYPE, t.ordinal());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(t.health);
        setHealth(t.health);
        refreshDimensions();
    }

    public byte mode() { return entityData.get(MODE); }
    public void setMode(byte m) { entityData.set(MODE, m); }
    public int battery() { return entityData.get(BATTERY_LEFT); }
    public int maxBattery() { return entityData.get(HIGH_CAP) ? BATTERY * 2 : BATTERY; }
    /** Munitions restantes : balles (Wasp), roquettes (Reaper), grenades (Mavic) ou charge (FPV). */
    public int arms() { return entityData.get(ARMS); }
    public int pilotId() { return entityData.get(PILOT_ID); }
    public boolean jammed() { return entityData.get(JAMMED_UNTIL) > level().getGameTime(); }
    public boolean silentRotors() { return entityData.get(SILENT); }
    public boolean highCap() { return entityData.get(HIGH_CAP); }
    public boolean hasCargo() { return entityData.get(CARGO); }
    public ItemStack payload() { return entityData.get(PAYLOAD); }
    public SimpleContainer cargo() { return cargo; }
    /** Brouillage subi (0 à 1) : neige analogique sur l'écran du pilote. */
    public float interference() { return entityData.get(INTERFERENCE); }
    public void setInterference(float f) { entityData.set(INTERFERENCE, f); }

    /** Distance à laquelle on entend ce drone (hélices silencieuses : -70 %). */
    public double noiseRadius() {
        DroneType t = droneType();
        return t == DroneType.HORNET ? t.noise : silentRotors() ? 7 : t.noise;
    }

    /** Portée radio effective : celle du modèle, limitée à la distance d'affichage du serveur. */
    public double radioRange() {
        int view = level() instanceof ServerLevel sl ? sl.getServer().getPlayerList().getViewDistance() * 16 - 24 : 9999;
        return Math.min(droneType().range, Math.max(96, view));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (TYPE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        DroneType t = droneType();
        return EntityDimensions.scalable(t.width, t.height);
    }

    // ------------------------------------------------------------------ sauvegarde (entité et objet)
    /** Écrit les données du drone (modules, charge, batterie, soute) : pour l'entité et pour l'objet ramassé. */
    public CompoundTag saveDrone(CompoundTag tag) {
        tag.putString("DroneType", droneType().id);
        tag.putInt("Battery", battery());
        tag.putInt("Arms", arms());
        tag.putBoolean("Silent", silentRotors());
        tag.putBoolean("HighCap", highCap());
        tag.putBoolean("Cargo", hasCargo());
        if (!payload().isEmpty()) {
            tag.put("Payload", payload().save(registryAccess()));
        }
        net.minecraft.world.ContainerHelper.saveAllItems(tag, cargoList(), true, registryAccess());
        return tag;
    }

    public void loadDrone(CompoundTag tag) {
        setDroneType(DroneType.byId(tag.getString("DroneType")));
        entityData.set(BATTERY_LEFT, tag.contains("Battery") ? tag.getInt("Battery") : BATTERY);
        entityData.set(ARMS, tag.getInt("Arms"));
        entityData.set(SILENT, tag.getBoolean("Silent"));
        entityData.set(HIGH_CAP, tag.getBoolean("HighCap"));
        entityData.set(CARGO, tag.getBoolean("Cargo"));
        entityData.set(PAYLOAD, tag.contains("Payload") ? ItemStack.parseOptional(registryAccess(), tag.getCompound("Payload")) : ItemStack.EMPTY);
        NonNullList<ItemStack> list = NonNullList.withSize(9, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, list, registryAccess());
        for (int i = 0; i < 9; i++) {
            cargo.setItem(i, list.get(i));
        }
        updateArms();
    }

    private NonNullList<ItemStack> cargoList() {
        NonNullList<ItemStack> list = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) {
            list.set(i, cargo.getItem(i));
        }
        return list;
    }

    /** Charge visible (FPV / Mavic) : le nombre d'explosifs embarqués. */
    private void updateArms() {
        DroneType t = droneType();
        if (t == DroneType.FPV || t == DroneType.MAVIC) {
            entityData.set(ARMS, hasCargo() ? 0 : payload().getCount());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveDrone(tag);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadDrone(tag);
        if (tag.hasUUID("Owner")) {
            owner = tag.getUUID("Owner");
        }
        setMode(IDLE);
    }

    // ------------------------------------------------------------------ équipement (aucun)
    @Override public Iterable<ItemStack> getArmorSlots() { return List.of(); }
    @Override public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }
    @Override public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}
    @Override public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }
    @Override public boolean isPushable() { return mode() == IDLE; }
    @Override protected void doPush(net.minecraft.world.entity.Entity e) {}
    @Override public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.GENERIC_EXPLODE.value(); }

    // ------------------------------------------------------------------ pilotage
    public boolean powered() {
        return battery() > 0 && !jammed() && (mode() == PILOT || mode() == RTH);
    }

    public void startPilot(ServerPlayer player) {
        setMode(PILOT);
        entityData.set(PILOT_ID, player.getId());
        inputAt = level().getGameTime();
        PacketDistributor.sendToPlayer(player, new DronePilotPayload(getId(), true));
    }

    /** Fin du pilotage : le drone rentre seul au pilote. */
    public void startRth(String reason) {
        if (level() instanceof ServerLevel sl && pilotId() >= 0 && sl.getEntity(pilotId()) instanceof ServerPlayer p) {
            PacketDistributor.sendToPlayer(p, new DronePilotPayload(getId(), false));
            if (reason != null) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.drone." + reason).withStyle(ChatFormatting.YELLOW), true);
            }
        }
        entityData.set(PILOT_ID, -1);
        if (mode() == PILOT) {
            setMode(owner != null ? RTH : DOWN);
        }
    }

    /** Commande du pilote (envoyée 20 fois par seconde). */
    public void applyInput(ServerPlayer player, float forward, float strafe, float vertical, float yaw, float pitch, boolean fire, boolean exit) {
        if (player.getId() != pilotId() || mode() != PILOT) {
            return;
        }
        if (exit) {
            startRth(null);
            return;
        }
        inForward = Mth.clamp(forward, -1, 1);
        inStrafe = Mth.clamp(strafe, -1, 1);
        inVertical = Mth.clamp(vertical, -1, 1);
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
        setXRot(Mth.clamp(pitch, -90, 90));
        inFire = fire;
        inputAt = level().getGameTime();
    }

    /** Brouillage (fusil EMP) : atterrissage forcé en douceur, désactivé pendant `ticks`. */
    public void jam(int ticks) {
        entityData.set(JAMMED_UNTIL, level().getGameTime() + ticks);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.DRONE_JAMMED.get(), SoundSource.NEUTRAL, 1.2f, 1f);
    }

    @Override
    public void tick() {
        super.tick();
        rotorSpinO = rotorSpin;
        if (powered() || botTarget >= 0 && !jammed()) {
            rotorSpin += 1.4f;
        }
        if (level().isClientSide) {
            if (jammed() && tickCount % 3 == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + getBbHeight(), getZ(), 0, 0.05, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        boolean flying = powered() || botTarget >= 0;
        if (flying) {
            // la batterie se vide en vol ; le chunk du drone reste chargé
            int b = battery() - 1;
            entityData.set(BATTERY_LEFT, Math.max(0, b));
            if (b <= 0) {
                startRth("battery");
                setMode(DOWN);
            }
            if (tickCount % 20 == 0) {
                level.getChunkSource().addRegionTicket(TICKET, chunkPosition(), 2, chunkPosition());
                float vol = (float) (noiseRadius() / 16.0);
                level.playSound(null, getX(), getY(), getZ(), ModSounds.DRONE_MOTOR.get(), SoundSource.NEUTRAL, Math.max(0.1f, vol),
                        droneType() == DroneType.FPV ? 1.5f : droneType() == DroneType.REAPER ? 0.7f : 1.0f);
            }
        }
        Jamming.tickDrone(this, level);
        if (fireCooldown > 0) {
            fireCooldown--;
        }
        tickSalvo(level);
        if (botTarget >= 0) {
            if (!jammed()) {
                tickBot(level);
            }
        } else {
            switch (mode()) {
                case PILOT -> tickPilot(level);
                case RTH -> tickRth(level);
                default -> {
                }
            }
        }
        if (isRemoved()) {
            return;
        }
        if (droneType() == DroneType.FPV && (mode() == PILOT || botTarget >= 0) && !jammed()) {
            double speed = getDeltaMovement().length();
            // kamikaze : explose au contact d'une cible, ou en percutant un mur / le sol à pleine vitesse
            if (speed > 0.2 && touchesEnemy() || (horizontalCollision || verticalCollision) && lastSpeed > 0.45) {
                detonate(level);
                return;
            }
            lastSpeed = speed;
        }
    }

    private boolean touchesEnemy() {
        return !level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.5),
                e -> e != this && e.isAlive() && e.getId() != pilotId() && e.getId() != ownerBotId && !(e instanceof DroneEntity)
                        && !(botFaction != null && e instanceof fr.modeminecraft.entity.bot.HumanBotEntity hb
                        && hb.faction().group.equals(fr.modeminecraft.entity.bot.Faction.byId(botFaction).group))).isEmpty();
    }

    /** Drone ennemi lancé par un bot opérateur vers une cible. */
    public void launchBy(fr.modeminecraft.entity.bot.HumanBotEntity operator, LivingEntity target) {
        ownerBotId = operator.getId();
        botFaction = operator.faction().id;
        botTarget = target.getId();
        setMode(DOWN);
    }

    /** IA des drones ennemis : le micro-drone tourne au-dessus de la cible et la marque, le FPV fonce dessus. */
    private void tickBot(ServerLevel level) {
        var t = level.getEntity(botTarget);
        if (!(t instanceof LivingEntity target) || !target.isAlive() || tickCount > 900 || level.getNearestPlayer(this, 128) == null) {
            if (droneType() == DroneType.FPV && t != null) {
                detonate(level);
            } else {
                discard();
            }
            return;
        }
        Vec3 aim;
        if (droneType() == DroneType.HORNET) {
            double a = tickCount * 0.05;
            aim = target.position().add(Math.cos(a) * 6, 9, Math.sin(a) * 6);
            if (tickCount % 40 == 0 && hasLineOfSight(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
                // tous les snipers et soldats de la base sont prévenus
                for (var b : level.getEntitiesOfClass(fr.modeminecraft.entity.bot.HumanBotEntity.class, getBoundingBox().inflate(96),
                        b -> botFaction != null && b.faction().id.equals(botFaction) && b.getTarget() == null && b.shouldAttack(target))) {
                    b.setTarget(target);
                }
            }
        } else {
            aim = target.getEyePosition();
            if (tickCount % 32 == 0) {
                level.playSound(null, getX(), getY(), getZ(), ModSounds.FPV_BUZZ.get(), SoundSource.HOSTILE, 2.5f, 1f);
            }
        }
        Vec3 to = aim.subtract(position());
        Vec3 dir = to.lengthSqr() > 1.0E-4 ? to.normalize() : Vec3.ZERO;
        if (horizontalCollision) {
            dir = dir.add(0, 1.2, 0).normalize();                     // obstacle : on passe par-dessus
        }
        double speed = droneType().speed * (droneType() == DroneType.HORNET ? Math.min(1, to.length() / 6) : 0.85);
        targetVelocity = dir.scale(speed);
        setYRot((float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG));
        setXRot((float) Mth.clamp(-Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))), -60, 60));
    }

    private void tickPilot(ServerLevel level) {
        var e = level.getEntity(pilotId());
        if (!(e instanceof ServerPlayer pilot) || !pilot.isAlive() || pilot.isSpectator()) {
            startRth(null);
            return;
        }
        if (!pilot.getMainHandItem().is(ModItems.DRONE_CONTROLLER.get()) && !pilot.getOffhandItem().is(ModItems.DRONE_CONTROLLER.get())) {
            startRth(null);
            return;
        }
        if (distanceTo(pilot) > radioRange()) {
            startRth("signal_lost");                                // perte de signal radio : retour automatique
            return;
        }
        if (Jamming.lostControl(level, position())) {
            startRth("jammed");
            return;
        }
        if (level.getGameTime() - inputAt > 10) {
            inForward = inStrafe = inVertical = 0;
            inFire = false;
        }
        if (inFire) {
            fire(level, pilot, !lastFire);
        }
        lastFire = inFire;
    }

    /** Retour au pilote : prise d'altitude, ligne droite, puis atterrissage en douceur à ses pieds. */
    private void tickRth(ServerLevel level) {
        Player home = owner == null ? null : level.getPlayerByUUID(owner);
        if (home == null || home.level() != level) {
            setMode(DOWN);
            return;
        }
        Vec3 to = home.position().subtract(position());
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        int ground = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ()),
                level.getHeight(Heightmap.Types.MOTION_BLOCKING, home.getBlockX(), home.getBlockZ()));
        double cruise = Math.max(ground + 6, home.getY() + 4);
        Vec3 want;
        if (flat > 2.5) {
            double up = getY() < cruise - 0.5 ? 1 : getY() > cruise + 2 ? -0.5 : 0;
            Vec3 h = new Vec3(to.x, 0, to.z).normalize().scale(getY() < cruise - 3 ? 0.2 : 1);
            want = new Vec3(h.x, up, h.z);
            setYRot((float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG));
        } else {
            want = new Vec3(to.x * 0.3, -0.25, to.z * 0.3);           // descente douce
            if (onGround() || getY() <= home.getY() + 0.05) {
                setMode(IDLE);
                targetVelocity = null;
                setDeltaMovement(Vec3.ZERO);
                level.playSound(null, getX(), getY(), getZ(), ModSounds.DRONE_PAIRED.get(), SoundSource.NEUTRAL, 0.5f, 0.8f);
            }
        }
        inForward = 0;
        targetVelocity = want.scale(droneType().speed * 0.8);
    }

    private Vec3 targetVelocity;
    private double lastSpeed;

    @Override
    public void travel(Vec3 input) {
        if (!powered() && botTarget < 0 || jammed()) {
            // hors tension / brouillé : il descend en douceur (pas de chute libre)
            Vec3 v = getDeltaMovement();
            double fall = jammed() || battery() <= 0 ? -0.2 : -0.3;
            setDeltaMovement(v.x * 0.8, onGround() ? 0 : Math.max(fall, v.y - 0.04), v.z * 0.8);
            move(MoverType.SELF, getDeltaMovement());
            if (onGround() && mode() == DOWN && !jammed()) {
                setMode(IDLE);
            }
            return;
        }
        Vec3 want;
        if (mode() == PILOT) {
            float yaw = getYRot() * Mth.DEG_TO_RAD;
            Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
            Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
            // le FPV de course vole là où regarde sa caméra (piqués et chandelles) ; les autres restent à plat
            Vec3 ahead = droneType() == DroneType.FPV ? lookDirection() : fwd;
            want = ahead.scale(inForward).add(right.scale(-inStrafe)).add(0, inVertical * 0.7, 0);
            if (want.lengthSqr() > 1) {
                want = want.normalize();
            }
            want = want.scale(droneType().speed);
        } else if (botTarget >= 0 && targetVelocity != null) {
            want = targetVelocity;
        } else {
            want = targetVelocity != null ? targetVelocity : Vec3.ZERO;
        }
        double agility = droneType() == DroneType.FPV ? 0.35 : droneType() == DroneType.HORNET ? 0.3 : 0.18;
        Vec3 v = getDeltaMovement().lerp(want, agility);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
    }

    public void setTargetVelocity(Vec3 v) {
        targetVelocity = v;
    }

    // ------------------------------------------------------------------ armement
    private void fire(ServerLevel level, ServerPlayer pilot, boolean pressed) {
        DroneType t = droneType();
        if (hasCargo() && t != DroneType.HORNET && t != DroneType.FPV) {
            return;                                                    // le cargo remplace l'armement ventral
        }
        switch (t) {
            case HORNET -> {
                if (pressed && fireCooldown <= 0) {
                    mark(level);
                    fireCooldown = 40;
                }
            }
            case FPV -> {
                if (pressed) {
                    detonate(level);
                }
            }
            case MAVIC -> {
                if (pressed && fireCooldown <= 0) {
                    dropGrenade(level, pilot);
                    fireCooldown = 10;
                }
            }
            case WASP -> {
                if (fireCooldown <= 0 && arms() > 0) {
                    shootBullet(level, pilot, lookDirection());
                    fireCooldown = 2;                                  // 600 coups / minute
                }
            }
            case REAPER -> {
                if (pressed && fireCooldown <= 0 && arms() > 0 && salvo == 0) {
                    HitResult aim = aim(level, 200);
                    salvoTarget = aim instanceof EntityHitResult eh ? eh.getEntity().getId() : -1;
                    salvo = Math.min(4, arms());
                    fireCooldown = 30;
                }
            }
        }
    }

    public Vec3 lookDirection() {
        return Vec3.directionFromRotation(getXRot(), getYRot());
    }

    /** Ce que vise la caméra du drone. */
    public HitResult aim(Level level, double range) {
        Vec3 eye = getEyePosition();
        Vec3 end = eye.add(lookDirection().scale(range));
        HitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult ent = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(this, eye, limit,
                getBoundingBox().expandTowards(limit.subtract(eye)).inflate(1), x -> x.isPickable() && x.getId() != pilotId() && x != this,
                eye.distanceToSqr(limit));
        return ent != null ? ent : block;
    }

    /** Black Hornet : les ennemis visés (cône de 20°, 40 blocs) brillent en rouge à travers les murs pendant 15 s. */
    private void mark(ServerLevel level) {
        Vec3 eye = getEyePosition(), look = lookDirection();
        double cos = Math.cos(Math.toRadians(20));
        int n = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(40),
                x -> x.isAlive() && x != this && x.getId() != pilotId() && !(x instanceof DroneEntity))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() < 40 && to.normalize().dot(look) > cos) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false));
                n++;
            }
        }
        level.playSound(null, getX(), getY(), getZ(), ModSounds.DRONE_PAIRED.get(), SoundSource.NEUTRAL, 0.3f, 1.8f);
        if (level.getEntity(pilotId()) instanceof ServerPlayer p) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.drone.marked", n).withStyle(ChatFormatting.RED), true);
        }
    }

    private void shootBullet(ServerLevel level, LivingEntity shooter, Vec3 dir) {
        BulletEntity b = new BulletEntity(level, shooter, 6f, 0, false, AmmoType.STANDARD);
        Vec3 from = position().add(0, -0.05, 0).add(dir.scale(0.6));
        b.setPos(from.x, from.y, from.z);
        b.shoot(dir.x, dir.y, dir.z, 4.5f, 1.2f);
        level.addFreshEntity(b);
        entityData.set(ARMS, arms() - 1);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.SHOT_RIFLE.get(), SoundSource.HOSTILE, 1.2f, 1.3f);
        level.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 1, 0, 0, 0, 0);
    }

    /** Wasp piloté par un bot : rafale vers une cible. */
    public void botShoot(ServerLevel level, LivingEntity target) {
        if (fireCooldown > 0 || arms() <= 0) {
            return;
        }
        Vec3 dir = target.getBoundingBox().getCenter().subtract(position()).normalize();
        shootBullet(level, this, dir);
        fireCooldown = 2;
    }

    private void tickSalvo(ServerLevel level) {
        if (salvo <= 0 || tickCount % 3 != 0) {
            return;
        }
        LivingEntity shooter = level.getEntity(pilotId()) instanceof ServerPlayer p ? p : this;
        RocketEntity r = new RocketEntity(ModEntities.MISSILE.get(), level, shooter, 1.2f);
        r.setDirectDamage(24);
        Vec3 dir = lookDirection();
        Vec3 side = new Vec3(-dir.z, 0, dir.x).normalize().scale(salvo % 2 == 0 ? 0.4 : -0.4);
        Vec3 from = position().add(side).add(0, -0.1, 0);
        r.setPos(from.x, from.y, from.z);
        r.shoot(dir.x, dir.y, dir.z, 2.2f, 0.5f);
        var target = salvoTarget >= 0 ? level.getEntity(salvoTarget) : null;
        if (target != null) {
            r.guide(target, false);                                   // roquettes guidées sur la cible visée
        }
        level.addFreshEntity(r);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.SHOT_ROCKET.get(), SoundSource.HOSTILE, 1.4f, 1.4f);
        entityData.set(ARMS, arms() - 1);
        salvo--;
    }

    private void dropGrenade(ServerLevel level, ServerPlayer pilot) {
        ItemStack p = payload();
        if (p.isEmpty() || !(p.getItem() instanceof GrenadeItem g)) {
            return;
        }
        ThrownGrenadeEntity grenade = new ThrownGrenadeEntity(level, pilot, g.kind(), g.kind().fuse);
        grenade.setPos(getX(), getY() - 0.2, getZ());
        grenade.setDeltaMovement(getDeltaMovement().scale(0.5).add(0, -0.15, 0));
        level.addFreshEntity(grenade);
        ItemStack left = p.copy();
        left.shrink(1);
        entityData.set(PAYLOAD, left);
        updateArms();
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.DISPENSER_LAUNCH, SoundSource.NEUTRAL, 0.8f, 1.4f);
    }

    /** Drone kamikaze : la charge explose (ou la batterie, sans charge). Le drone est détruit. */
    public void detonate(ServerLevel level) {
        if (isRemoved()) {
            return;
        }
        LivingEntity owner = level.getEntity(pilotId()) instanceof ServerPlayer p ? p : this;
        ItemStack p = payload();
        Vec3 c = position();
        startRth(null);
        discard();
        if (p.getItem() instanceof GrenadeItem g) {
            ThrownGrenadeEntity grenade = new ThrownGrenadeEntity(level, owner, g.kind(), 1);
            grenade.setPos(c.x, c.y, c.z);
            level.addFreshEntity(grenade);
            if (g.kind().impact) {
                level.explode(this, c.x, c.y, c.z, 0.5f, Level.ExplosionInteraction.NONE);
                fr.modeminecraft.item.explosive.Detonations.detonate(grenade, g.kind());
                grenade.discard();
            }
        } else if (p.getItem() instanceof PlaceableExplosiveItem || p.is(ModItems.RPG_ROCKET.get())) {
            // C4 / roquette : explosion lourde anti-blindage (28 PV, brise véhicules et tourelles)
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3))) {
                e.hurt(level.damageSources().explosion(this, owner), 28);
            }
            for (var v : level.getEntitiesOfClass(fr.modeminecraft.entity.VehicleEntity.class, getBoundingBox().inflate(3))) {
                v.hurt(level.damageSources().explosion(this, owner), 60);
            }
            for (var t : level.getEntitiesOfClass(fr.modeminecraft.entity.MountedGunEntity.class, getBoundingBox().inflate(3))) {
                t.hurt(level.damageSources().explosion(this, owner), 60);
            }
            level.explode(this, c.x, c.y, c.z, 3.5f, Level.ExplosionInteraction.TNT);
        } else {
            level.explode(this, c.x, c.y, c.z, 1.0f, Level.ExplosionInteraction.NONE);
        }
    }

    // ------------------------------------------------------------------ dégâts
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.2, getZ(), 6, 0.2, 0.1, 0.2, 0.2);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) {
            startRth("destroyed");
            if (droneType() == DroneType.FPV && !payload().isEmpty()) {
                detonate(sl);
                return;
            }
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 12, 0.3, 0.2, 0.3, 0.02);
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 1, 0, 0, 0, 0);
            for (int i = 0; i < 9; i++) {
                spawnAtLocation(cargo.getItem(i));
            }
        }
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    // ------------------------------------------------------------------ interactions
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        DroneType t = droneType();
        // Maj + clic droit avec la télécommande : association
        if (stack.is(ModItems.DRONE_CONTROLLER.get())) {
            if (player.isShiftKeyDown()) {
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(link(getUUID())));
                owner = player.getUUID();
                level().playSound(null, getX(), getY(), getZ(), ModSounds.DRONE_PAIRED.get(), SoundSource.NEUTRAL, 1f, 1f);
                player.displayClientMessage(Component.translatable("message.modeminecraft.drone.paired",
                        Component.translatable("item.modeminecraft." + t.id)).withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.CONSUME;
        }
        // pile CR123A : recharge
        if (stack.is(ModItems.TACTICAL_BATTERY.get()) && battery() < maxBattery()) {
            entityData.set(BATTERY_LEFT, maxBattery());
            stack.consume(1, player);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.DRONE_PAIRED.get(), SoundSource.NEUTRAL, 0.6f, 1.3f);
            return InteractionResult.CONSUME;
        }
        // rechargement de l'armement
        if (!hasCargo() && load(stack, player)) {
            return InteractionResult.CONSUME;
        }
        if (stack.isEmpty() && player.isShiftKeyDown() && mode() == IDLE) {
            // Maj + clic droit main vide : on ramasse le drone (modules, charge, soute et batterie gardés)
            ItemStack item = DroneItem.create(t, saveDrone(new CompoundTag()));
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            discard();
            return InteractionResult.CONSUME;
        }
        if (stack.isEmpty() && hasCargo()) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) ->
                    new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inv, cargo, 1),
                    Component.translatable("container.modeminecraft.drone_cargo")));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public static CompoundTag link(UUID drone) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Drone", drone);
        return tag;
    }

    /** Charge un explosif / des grenades / des munitions selon le modèle. */
    private boolean load(ItemStack stack, Player player) {
        DroneType t = droneType();
        if (t == DroneType.FPV && payload().isEmpty() && DroneData.fpvCharge(stack)) {
            entityData.set(PAYLOAD, stack.copyWithCount(1));
            stack.consume(1, player);
        } else if (t == DroneType.MAVIC && stack.getItem() instanceof GrenadeItem
                && (payload().isEmpty() || ItemStack.isSameItemSameComponents(payload(), stack)) && payload().getCount() < 2) {
            entityData.set(PAYLOAD, stack.copyWithCount(payload().getCount() + 1));
            stack.consume(1, player);
        } else if (t == DroneType.WASP && stack.is(ModItems.AMMO_556.get()) && arms() < 60) {
            int n = Math.min(60 - arms(), stack.getCount());
            entityData.set(ARMS, arms() + n);
            stack.consume(n, player);
        } else if (t == DroneType.REAPER && stack.is(ModItems.RPG_ROCKET.get()) && arms() < 4) {
            entityData.set(ARMS, 4);
            stack.consume(1, player);
        } else {
            return false;
        }
        updateArms();
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }
}

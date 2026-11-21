package fr.modeminecraft.entity.military;

import fr.modeminecraft.item.gun.TriggerHandler;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Lance-roquettes statiques (cahier des charges « Systèmes de missiles » §5) :
 * <ul>
 * <li><b>Type 63</b> : trépied léger à 4 tubes de 107 mm, fabricable et posable n'importe où ;</li>
 * <li><b>batterie fixe de 12 tubes de 122 mm</b>, générée sur le périmètre des bases secrètes, des villes soviétiques
 * et des raffineries (chargée).</li>
 * </ul>
 * Chargement : clic droit avec des roquettes. Pointage : Maj + clic droit (écran : azimut, élévation 0-65°, portée
 * estimée 50-400 blocs) ou en montant dessus (clic droit main nue : la souris oriente les tubes). Tir : clic gauche =
 * une roquette, clic droit / Espace = salve complète ; ou signal de redstone (front montant) = salve.
 */
public class RocketLauncherEntity extends Entity {
    public enum Kind {
        TYPE63("type63", 4, 1.4f, 1.1f, MilAmmo.ROCKET_107, 1f),
        /** Charge propulsive réduite : portée 50-400 blocs comme le Type 63 (la Grad du BM-21 porte plus loin). */
        BATTERY("battery_122", 12, 2.8f, 2.0f, MilAmmo.ROCKET_122, 0.8f);

        public final String id;
        public final int tubes;
        public final float width, height, thrust;
        public final MilAmmo ammo;

        Kind(String id, int tubes, float width, float height, MilAmmo ammo, float thrust) {
            this.id = id;
            this.tubes = tubes;
            this.width = width;
            this.height = height;
            this.ammo = ammo;
            this.thrust = thrust;
        }
    }

    public static final float MAX_ELEVATION = 65;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(RocketLauncherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> AZIMUTH = SynchedEntityData.defineId(RocketLauncherEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ELEVATION = SynchedEntityData.defineId(RocketLauncherEntity.class, EntityDataSerializers.FLOAT);
    /** Tubes chargés : un bit par tube (on tire dans l'ordre). */
    private static final EntityDataAccessor<Integer> LOADED = SynchedEntityData.defineId(RocketLauncherEntity.class, EntityDataSerializers.INT);

    private int cooldown, salvo, salvoGap;
    private boolean powered;
    private java.util.UUID lastUser;

    public RocketLauncherEntity(EntityType<? extends RocketLauncherEntity> type, Level level) {
        super(type, level);
    }

    public RocketLauncherEntity(Level level, Kind kind, double x, double y, double z, float yaw, boolean full) {
        this(ModEntities.ROCKET_LAUNCHER.get(), level);
        entityData.set(KIND, kind.ordinal());
        entityData.set(AZIMUTH, yaw);
        entityData.set(ELEVATION, 30f);
        if (full) {
            entityData.set(LOADED, (1 << kind.tubes) - 1);
        }
        moveTo(x, y, z, 0, 0);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, 0).define(AZIMUTH, 0f).define(ELEVATION, 30f).define(LOADED, 0);
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
        return EntityDimensions.fixed(k.width, k.height);
    }

    public Kind kind() {
        return Kind.values()[Math.floorMod(entityData.get(KIND), Kind.values().length)];
    }

    public float azimuth() {
        return entityData.get(AZIMUTH);
    }

    public float elevation() {
        return entityData.get(ELEVATION);
    }

    public int loadedMask() {
        return entityData.get(LOADED);
    }

    public int loaded() {
        return Integer.bitCount(loadedMask());
    }

    /** Pointage (azimut absolu en degrés de Minecraft, élévation 0-65°). */
    public void setAim(float azimuth, float elevation) {
        entityData.set(AZIMUTH, Mth.wrapDegrees(azimuth));
        entityData.set(ELEVATION, Mth.clamp(elevation, 0, MAX_ELEVATION));
    }

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
        return getPassengers().isEmpty();
    }

    /** Le servant s'assoit derrière le lanceur. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double rad = Math.toRadians(azimuth());
        double back = kind().width * 0.75;
        return new Vec3(Math.sin(rad) * back, 0.1, -Math.cos(rad) * back);    // le lanceur lui-même ne tourne pas (yRot = 0)
    }

    @Override
    public void onPassengerTurned(Entity p) {
        p.setXRot(Mth.clamp(p.getXRot(), -MAX_ELEVATION, 10));
    }

    // ------------------------------------------------------------------ interactions
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || hasPassenger(player)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        Kind k = kind();
        if (!held.isEmpty() && held.is(k.ammo.item())) {
            if (!level().isClientSide) {
                load(player, held);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive()) {
            if (held.is(ModItems.WRENCH.get()) && k == Kind.TYPE63) {
                if (!level().isClientSide) {
                    dismantle(player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (level().isClientSide) {
                fr.modeminecraft.client.RocketLauncherScreen.open(this);         // écran de pointage
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.isEmpty() && !isVehicle()) {
            if (!level().isClientSide) {
                player.startRiding(this);
                player.setYRot(azimuth());
                player.setXRot(-elevation());
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    private void load(Player player, ItemStack rockets) {
        int mask = loadedMask();
        int tubes = kind().tubes;
        int n = 0;
        for (int i = 0; i < tubes && !rockets.isEmpty(); i++) {
            if ((mask & 1 << i) == 0) {
                mask |= 1 << i;
                n++;
                if (!player.getAbilities().instabuild) {
                    rockets.shrink(1);
                }
            }
        }
        if (n == 0) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.launcher.full").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        entityData.set(LOADED, mask);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.TURRET_RELOAD.get(), SoundSource.PLAYERS, 1.2f, 0.7f);
        player.displayClientMessage(Component.translatable("message.modeminecraft.launcher.loaded", Integer.bitCount(mask), tubes)
                .withStyle(ChatFormatting.GREEN), true);
    }

    private void dismantle(Player player) {
        ejectPassengers();
        ItemStack stack = new ItemStack(ModItems.TYPE63_LAUNCHER.get());
        if (!player.getInventory().add(stack)) {
            spawnAtLocation(stack);
        }
        if (loaded() > 0) {
            ItemStack r = new ItemStack(kind().ammo.item(), loaded());
            if (!player.getInventory().add(r)) {
                spawnAtLocation(r);
            }
        }
        level().playSound(null, getX(), getY(), getZ(), ModSounds.TURRET_DEPLOY.get(), SoundSource.PLAYERS, 1.0f, 0.8f);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return false;
        }
        if (source.getDirectEntity() instanceof Player p && p.getAbilities().instabuild && getPassengers().isEmpty()) {
            discard();
            return true;
        }
        // une explosion proche fait partir les roquettes chargées
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && amount > 12 && loaded() > 0 && random.nextFloat() < 0.3f) {
            salvo = loaded();
        }
        return false;
    }

    /** Ordre de tir (servant monté, écran de pointage ou redstone) : une roquette ou toute la salve. */
    public void command(Player by, boolean all) {
        if (by != null) {
            lastUser = by.getUUID();
        }
        if (loaded() == 0) {
            if (by != null) {
                by.displayClientMessage(Component.translatable("message.modeminecraft.launcher.empty").withStyle(ChatFormatting.RED), true);
            }
            level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, SoundSource.NEUTRAL, 0.8f, 1.2f);
            return;
        }
        if (all) {
            salvo = loaded();
        } else if (cooldown <= 0) {
            fireOne();
        }
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.08, 0).multiply(0.9, 0.98, 0.9));
            move(MoverType.SELF, getDeltaMovement());
        }
        if (level().isClientSide) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        Entity rider = getFirstPassenger();
        if (rider instanceof Player p) {
            setAim(p.getYRot(), -p.getXRot());
            if (TriggerHandler.isTriggerDown(p) && cooldown <= 0 && salvo == 0) {
                command(p, false);
            }
            if (TriggerHandler.isRightHeld(p) && salvo == 0 && loaded() > 0 && cooldown <= 0) {
                command(p, true);
            }
        }
        // redstone : salve sur front montant (bloc sous le lanceur ou à côté)
        boolean signal = level().hasNeighborSignal(blockPosition()) || level().hasNeighborSignal(blockPosition().below());
        if (signal && !powered) {
            command(null, true);
        }
        powered = signal;
        if (salvo > 0 && --salvoGap <= 0) {
            if (loaded() > 0) {
                fireOne();
            }
            salvo = loaded() > 0 ? salvo - 1 : 0;
            salvoGap = kind() == Kind.BATTERY ? 5 : 8;
        }
    }

    /** Direction des tubes. */
    public Vec3 direction() {
        return Vec3.directionFromRotation(-elevation(), azimuth());
    }

    /** Bouche du tube n (grille de 2 x 2 ou 3 x 4), dans le monde. */
    public Vec3 tubeMouth(int i) {
        Kind k = kind();
        int cols = k == Kind.TYPE63 ? 2 : 4;
        int rows = k.tubes / cols;
        double spacing = k == Kind.TYPE63 ? 0.22 : 0.3;
        double cx = (i % cols - (cols - 1) / 2.0) * spacing;
        double cy = (i / cols - (rows - 1) / 2.0) * spacing;
        Vec3 dir = direction();
        double rad = Math.toRadians(azimuth());
        Vec3 right = new Vec3(-Math.cos(rad), 0, -Math.sin(rad));
        Vec3 up = right.cross(dir).normalize().scale(-1);
        if (up.y < 0) {
            up = up.scale(-1);
        }
        Vec3 pivot = position().add(0, pivotHeight(), 0);
        return pivot.add(dir.scale(tubeLength() * 0.55)).add(right.scale(cx)).add(up.scale(cy));
    }

    public double pivotHeight() {
        return kind() == Kind.TYPE63 ? 0.75 : 1.35;
    }

    public double tubeLength() {
        return kind() == Kind.TYPE63 ? 1.1 : 2.4;
    }

    private void fireOne() {
        int mask = loadedMask();
        int tube = Integer.numberOfTrailingZeros(mask);
        if (tube >= kind().tubes) {
            return;
        }
        entityData.set(LOADED, mask & ~(1 << tube));
        cooldown = 6;
        ServerLevel sl = (ServerLevel) level();
        Vec3 pos = tubeMouth(tube);
        Vec3 dir = direction();
        float spread = 0.018f;
        dir = dir.add(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread).normalize();
        MilAmmo.Shell sh = kind().ammo.shell;
        LivingEntity owner = getFirstPassenger() instanceof LivingEntity le ? le : lastUser != null ? sl.getPlayerByUUID(lastUser) : null;
        ShellEntity s = new ShellEntity(sl, owner, sh, this);
        Vec3 out = pos.add(dir.scale(0.6));
        s.setPos(out.x, out.y, out.z);
        s.setThrust(kind().thrust);
        s.setDeltaMovement(dir.scale(sh.speed * kind().thrust));
        sl.addFreshEntity(s);
        Vec3 back = pos.subtract(dir.scale(tubeLength()));
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, back.x, back.y, back.z, 10, 0.3, 0.2, 0.3, 0.05);
        sl.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 6, 0.1, 0.1, 0.1, 0.05);
        sl.sendParticles(ParticleTypes.CLOUD, back.x, back.y, back.z, 6, 0.4, 0.1, 0.4, 0.02);
        sl.playSound(null, pos.x, pos.y, pos.z, ModSounds.ROCKET_LAUNCH.get(), SoundSource.NEUTRAL, 4f, kind() == Kind.TYPE63 ? 1.1f : 0.9f);
    }

    /**
     * Portée estimée sur terrain plat : simulation du vol exact de la roquette (poussée pendant la combustion, gravité),
     * jusqu'au retour à la hauteur de départ.
     */
    public static double range(Kind kind, float elevation) {
        MilAmmo.Shell s = kind.ammo.shell;
        double top = s.speed * kind.thrust;
        Vec3 v = Vec3.directionFromRotation(-elevation, 0).scale(top);
        double x = 0, y = 0;
        for (int life = 1; life < 1200; life++) {
            v = v.add(0, -s.gravity, 0);
            if (life < 40 && v.length() < top) {
                v = v.normalize().scale(Math.min(top, v.length() + 0.12));
            }
            x += Math.hypot(v.x, v.z);
            y += v.y;
            if (y < 0 && life > 2) {
                return x;
            }
        }
        return x;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(KIND, tag.getInt("Kind"));
        entityData.set(AZIMUTH, tag.getFloat("Azimuth"));
        entityData.set(ELEVATION, tag.contains("Elevation") ? tag.getFloat("Elevation") : 30f);
        entityData.set(LOADED, tag.getInt("Loaded"));
        powered = tag.getBoolean("Powered");
        if (tag.hasUUID("LastUser")) {
            lastUser = tag.getUUID("LastUser");
        }
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", entityData.get(KIND));
        tag.putFloat("Azimuth", azimuth());
        tag.putFloat("Elevation", elevation());
        tag.putInt("Loaded", loadedMask());
        tag.putBoolean("Powered", powered);
        if (lastUser != null) {
            tag.putUUID("LastUser", lastUser);
        }
    }

    /** Munition acceptée (pour l'infobulle). */
    public static Item ammoItem(Kind k) {
        return k.ammo.item();
    }
}

package fr.modeminecraft.entity;

import fr.modeminecraft.item.gun.AmmoType;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Mitrailleuse lourde sur trépied (M2 Browning, DShK) : on ne peut pas la tenir à la main.
 * On la pose au sol, clic droit pour s'installer derrière, clic gauche pour tirer, Shift pour la quitter.
 * Frapper la mitrailleuse (sans être dessus) la remet dans l'inventaire.
 * Elle tire des balles de .50 BMG prises dans l'inventaire du tireur (bande de 100).
 */
public class MountedGunEntity extends Entity {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BELT = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> GUN_YAW = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GUN_PITCH = SynchedEntityData.defineId(MountedGunEntity.class, EntityDataSerializers.FLOAT);
    public static final int BELT_SIZE = 100;
    /** Hauteur du pivot de l'arme au-dessus du sol. */
    public static final double PIVOT = 1.0;

    /** Les deux modèles : dégâts, cadence (CPM), objet. */
    public enum Kind {
        M2(14f, 550), DSHK(13f, 600);
        public final float damage, rpm;

        Kind(float damage, float rpm) {
            this.damage = damage;
            this.rpm = rpm;
        }

        public Item item() {
            return this == M2 ? ModItems.M2_BROWNING.get() : ModItems.DSHK.get();
        }
    }

    private int firingTicks;
    private int shots;
    private int reloadUntil = -1;
    private float heat;

    public MountedGunEntity(EntityType<? extends MountedGunEntity> type, Level level) {
        super(type, level);
    }

    public MountedGunEntity(Level level, Kind kind, double x, double y, double z, float yaw) {
        this(ModEntities.MOUNTED_GUN.get(), level);
        setPos(x, y, z);
        setYRot(yaw);
        entityData.set(KIND, kind.ordinal());
        entityData.set(GUN_YAW, yaw);
        entityData.set(BELT, BELT_SIZE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0).define(BELT, BELT_SIZE).define(GUN_YAW, 0f).define(GUN_PITCH, 0f);
    }

    public Kind kind() {
        return Kind.values()[Mth.clamp(entityData.get(KIND), 0, Kind.values().length - 1)];
    }

    public int belt() {
        return entityData.get(BELT);
    }

    public float gunYaw() {
        return entityData.get(GUN_YAW);
    }

    public float gunPitch() {
        return entityData.get(GUN_PITCH);
    }

    public float heat() {
        return heat;
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
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || isVehicle()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof Player player && !isPassengerOfSameVehicle(player) && getPassengers().isEmpty()) {
            if (!player.getAbilities().instabuild) {
                spawnAtLocation(new ItemStack(kind().item()));
            }
            discard();
            return true;
        }
        return false;
    }

    /** Le tireur est assis juste derrière les poignées, un peu bas (yeux à hauteur de la hausse). */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        float yaw = (float) Math.toRadians(gunYaw());
        Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw)).scale(1.05);
        return back.add(0, 0.32, 0); // assis : les yeux juste au-dessus de la hausse
    }

    @Override
    public void tick() {
        super.tick();
        Entity rider = getFirstPassenger();
        if (rider instanceof Player player) {
            // L'arme suit le regard du tireur, dans un arc de ±70° autour du trépied, -30° à +25° en hauteur
            float base = getYRot();
            float yaw = base + Mth.clamp(Mth.wrapDegrees(player.getYRot() - base), -70, 70);
            float pitch = Mth.clamp(player.getXRot(), -25, 30);
            entityData.set(GUN_YAW, yaw);
            entityData.set(GUN_PITCH, pitch);
            if (!level().isClientSide) {
                fire(player);
            }
        } else {
            firingTicks = 0;
            shots = 0;
        }
        heat = Math.max(0, heat - 0.5f);
    }

    private void fire(Player player) {
        if (reloadUntil >= 0) {
            if (tickCount >= reloadUntil) {
                reloadUntil = -1;
                level().playSound(null, getX(), getY(), getZ(), ModSounds.RACK.get(), SoundSource.PLAYERS, 1.2f, 0.7f);
            }
            return;
        }
        if (!TriggerHandler.isTriggerDown(player)) {
            firingTicks = 0;
            shots = 0;
            return;
        }
        float interval = 1200f / kind().rpm;
        int due = (int) Math.floor(firingTicks / interval) + 1 - shots;
        firingTicks++;
        for (int i = 0; i < due; i++) {
            if (heat >= 100) {
                player.displayClientMessage(Component.translatable("message.modeminecraft.overheated").withStyle(ChatFormatting.RED), true);
                return;
            }
            if (belt() <= 0 && !reloadBelt(player)) {
                return;
            }
            shootOnce(player);
            shots++;
        }
    }

    /** Nouvelle bande de 100 balles de .50 BMG prise dans l'inventaire (3 secondes). */
    private boolean reloadBelt(Player player) {
        int need = BELT_SIZE;
        int taken = 0;
        if (player.getAbilities().instabuild) {
            taken = need;
        } else {
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize() && taken < need; i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(ModItems.AMMO_50BMG.get())) {
                    int n = Math.min(need - taken, s.getCount());
                    s.shrink(n);
                    taken += n;
                }
            }
        }
        if (taken == 0) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.no_ammo",
                    ModItems.AMMO_50BMG.get().getDescription()).withStyle(ChatFormatting.RED), true);
            return false;
        }
        entityData.set(BELT, taken);
        reloadUntil = tickCount + 60;
        level().playSound(null, getX(), getY(), getZ(), ModSounds.MAG_IN.get(), SoundSource.PLAYERS, 1.2f, 0.6f);
        return false;
    }

    private void shootOnce(Player player) {
        ServerLevel level = (ServerLevel) level();
        Vec3 dir = Vec3.directionFromRotation(gunPitch(), gunYaw());
        Vec3 muzzle = position().add(0, PIVOT, 0).add(dir.scale(1.1));
        BulletEntity b = new BulletEntity(level, player, kind().damage, 2, false, AmmoType.STANDARD);
        b.setArmorPenetration(0.7f);
        b.setPos(muzzle);
        b.shoot(dir.x, dir.y, dir.z, 7.0f, 1.2f);
        level.addFreshEntity(b);
        if (!player.getAbilities().instabuild) {
            entityData.set(BELT, belt() - 1);
        }
        heat += 1.2f + 0.5f * (1200f / kind().rpm);
        level.playSound(null, muzzle.x, muzzle.y, muzzle.z, ModSounds.SHOT_HEAVY.get(), SoundSource.PLAYERS, 9f,
                (kind() == Kind.DSHK ? 0.9f : 1.0f) + (random.nextFloat() - 0.5f) * 0.06f);
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.05, 0.05, 0.05, 0.01);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.05, 0.05, 0.05, 0.02);
        if (heat > 60) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 1, 0.1, 0.05, 0.1, 0.01);
        }
        // Recul : la vue du tireur tremble un peu
        player.setXRot(player.getXRot() - 0.6f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(KIND, tag.getInt("Kind"));
        entityData.set(BELT, tag.contains("Belt") ? tag.getInt("Belt") : BELT_SIZE);
        entityData.set(GUN_YAW, tag.getFloat("GunYaw"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", entityData.get(KIND));
        tag.putInt("Belt", belt());
        tag.putFloat("GunYaw", gunYaw());
    }
}

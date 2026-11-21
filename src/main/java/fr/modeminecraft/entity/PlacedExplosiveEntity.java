package fr.modeminecraft.entity;

import fr.modeminecraft.item.explosive.Detonations;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Explosif posé :
 * - C4 : collé à un bloc ou à une créature, il explose quand son propriétaire utilise le détonateur ;
 * - Claymore : mine directionnelle, elle explose quand un ennemi passe devant (jamais pour son propriétaire) ;
 * - Mortier : tube posé ; clic droit avec un obus = tir en cloche, clic droit à vide = angle +5°, Shift + clic droit = tourner de 15°.
 * Frapper l'objet le reprend (sauf le C4 armé collé à une créature).
 */
public class PlacedExplosiveEntity extends Entity {
    /** TM-62 (soviétique) et M15 (américaine) : mines antichars, déclenchées par le poids d'un véhicule seulement. */
    public enum Kind { C4, CLAYMORE, MORTAR, TM62, M15 }

    public boolean antiTank() {
        return kind() == Kind.TM62 || kind() == Kind.M15;
    }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANGLE = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.INT);
    /** Mortier : un obus est dans le tube, prêt à partir. */
    private static final EntityDataAccessor<Boolean> LOADED = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.BOOLEAN);
    private UUID owner;
    private int stuckTo = -1;
    private Vec3 stuckOffset = Vec3.ZERO;

    public PlacedExplosiveEntity(EntityType<? extends PlacedExplosiveEntity> type, Level level) {
        super(type, level);
    }

    public PlacedExplosiveEntity(Level level, Kind kind, Vec3 pos, float yaw, Player owner) {
        this(ModEntities.PLACED_EXPLOSIVE.get(), level);
        setPos(pos);
        setYRot(yaw);
        entityData.set(KIND, kind.ordinal());
        this.owner = owner == null ? null : owner.getUUID();
    }

    public void stickTo(Entity host) {
        stuckTo = host.getId();
        stuckOffset = position().subtract(host.position());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0).define(ANGLE, 65).define(LOADED, false);
    }

    public Kind kind() {
        return Kind.values()[entityData.get(KIND)];
    }

    public boolean loaded() {
        return entityData.get(LOADED);
    }

    public int angle() {
        return entityData.get(ANGLE);
    }

    public Item item() {
        return switch (kind()) {
            case C4 -> ModItems.C4.get();
            case CLAYMORE -> ModItems.CLAYMORE_MINE.get();
            case MORTAR -> ModItems.MORTAR.get();
            case TM62 -> ModItems.TM62_MINE.get();
            case M15 -> ModItems.M15_MINE.get();
        };
    }

    public boolean ownedBy(Player p) {
        return owner != null && owner.equals(p.getUUID());
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (stuckTo >= 0) {
            Entity host = level().getEntity(stuckTo);
            if (host == null || !host.isAlive()) {
                stuckTo = -1;
            } else {
                setPos(host.position().add(stuckOffset));
            }
        }
        if (level().isClientSide) {
            if (kind() != Kind.MORTAR && !antiTank() && tickCount % 20 < 2) {
                level().addParticle(new DustParticleOptions(new Vector3f(1, 0.05f, 0.05f), 0.5f), getX(), getY() + 0.2, getZ(), 0, 0, 0);
            }
            return;
        }
        if (antiTank() && tickCount > 40 && tickCount % 2 == 0) {
            // mine antichar : seul le poids d'un véhicule la déclenche (un piéton peut marcher dessus)
            AABB box = new AABB(position(), position()).inflate(0.9, 0.2, 0.9).expandTowards(0, 1.2, 0);
            for (Entity e : level().getEntities(this, box, e -> e instanceof fr.modeminecraft.entity.military.MilVehicleEntity
                    || e instanceof fr.modeminecraft.entity.military.MilPart || e instanceof fr.modeminecraft.entity.VehicleEntity)) {
                explodeAntiTank(e);
                return;
            }
        }
        if (kind() == Kind.CLAYMORE && tickCount > 40) {
            // Mine Claymore : un ennemi dans le cône de 60° devant elle, à moins de 3,5 blocs
            Vec3 front = Vec3.directionFromRotation(0, getYRot());
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(3.5))) {
                if (e instanceof Player p && (ownedBy(p) || p.isCreative() || p.isSpectator())) {
                    continue;
                }
                Vec3 to = e.position().subtract(position());
                if (to.length() < 3.5 && to.length() > 0.3 && new Vec3(to.x, 0, to.z).normalize().dot(front) > 0.5) {
                    explodeClaymore(front);
                    return;
                }
            }
        }
    }

    private void explodeClaymore(Vec3 front) {
        ServerLevel level = (ServerLevel) level();
        discard();
        Player ownerPlayer = owner == null ? null : level.getPlayerByUUID(owner);
        level.explode(this, getX(), getY() + 0.3, getZ(), 1.0f, Level.ExplosionInteraction.NONE);
        Detonations.shrapnel(level, position(), ownerPlayer, 40, front, 0.6, 10, 6, 9);
        discard();
    }

    private void explodeAntiTank(Entity hit) {
        ServerLevel level = (ServerLevel) level();
        discard();
        var v = hit instanceof fr.modeminecraft.entity.military.MilPart part ? part.getParent()
                : hit instanceof fr.modeminecraft.entity.military.MilVehicleEntity mv ? mv : null;
        level.explode(this, getX(), getY() + 0.2, getZ(), 3.5f, Level.ExplosionInteraction.TNT);
        if (v != null) {
            // charge sous la caisse : chenilles / roues arrachées ; coque en V du M-ATV : -80 %
            v.mineHit(position(), 200 * v.mineFactor());
        } else if (hit instanceof fr.modeminecraft.entity.VehicleEntity car) {
            car.hurt(level.damageSources().explosion(this, null), 200);
        }
        discard();
    }

    /** Détonateur : le C4 explose. */
    public void detonate() {
        if (isRemoved()) {
            return;
        }
        discard();
        level().explode(this, getX(), getY(), getZ(), 4.0f, Level.ExplosionInteraction.TNT);
        discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (kind() != Kind.MORTAR) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() && !loaded() && held.is(ModItems.MORTAR_SHELL.get())) {
            // 1er temps : l'obus glisse dans le tube, mais rien ne part encore
            entityData.set(LOADED, true);
            player.swing(hand, true);
            level().playSound(null, getX(), getY(), getZ(), fr.modeminecraft.registry.ModSounds.SHELL_INSERT.get(), SoundSource.PLAYERS, 1.2f, 0.55f);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8f, 0.6f);
            player.displayClientMessage(Component.translatable("message.modeminecraft.mortar.loaded").withStyle(ChatFormatting.GOLD), true);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        } else if (!player.isSecondaryUseActive() && loaded()) {
            // 2e temps : percussion, l'obus part en cloche dans la direction du mortier, avec l'angle réglé
            entityData.set(LOADED, false);
            player.swing(hand, true);
            var shell = new RocketEntity(ModEntities.GRENADE.get(), level(), player, 3.0f);
            shell.setItem(new ItemStack(ModItems.MORTAR_SHELL.get()));
            shell.setPos(getX(), getY() + 1.1, getZ());
            Vec3 dir = Vec3.directionFromRotation(-angle(), getYRot());
            shell.shoot(dir.x, dir.y, dir.z, 1.7f, 0.5f);
            level().addFreshEntity(shell);
            level().playSound(null, getX(), getY(), getZ(), fr.modeminecraft.registry.ModSounds.SHOT_LAUNCHER.get(), SoundSource.PLAYERS, 4f, 0.6f);
            ((ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.3, getZ(), 8, 0.2, 0.2, 0.2, 0.02);
        } else if (player.isSecondaryUseActive()) {
            setYRot(getYRot() + 15);
            player.displayClientMessage(Component.translatable("message.modeminecraft.mortar.aim", angle(), Math.round(getYRot())).withStyle(ChatFormatting.YELLOW), true);
        } else {
            entityData.set(ANGLE, angle() >= 85 ? 45 : angle() + 5);
            player.displayClientMessage(Component.translatable("message.modeminecraft.mortar.aim", angle(), Math.round(getYRot())).withStyle(ChatFormatting.YELLOW), true);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof Player player && source.getDirectEntity() == player) {
            if (!player.getAbilities().instabuild) {
                spawnAtLocation(new ItemStack(item()));
                if (loaded()) {
                    spawnAtLocation(new ItemStack(ModItems.MORTAR_SHELL.get()));
                }
            }
            discard();
            return true;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && kind() != Kind.MORTAR && !isRemoved()) {
            // réaction en chaîne (la mine disparaît avant d'exploser : pas d'explosions en boucle)
            if (kind() == Kind.C4) detonate(); else if (antiTank()) explodeAntiTank(null); else explodeClaymore(Vec3.directionFromRotation(0, getYRot()));
            return true;
        }
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(KIND, tag.getInt("Kind"));
        entityData.set(ANGLE, tag.contains("Angle") ? tag.getInt("Angle") : 65);
        entityData.set(LOADED, tag.getBoolean("Loaded"));
        if (tag.hasUUID("Owner")) {
            owner = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", entityData.get(KIND));
        tag.putInt("Angle", angle());
        tag.putBoolean("Loaded", loaded());
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }
}

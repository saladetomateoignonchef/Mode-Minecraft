package fr.modeminecraft.entity.bot;

import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.melee.MeleeWeaponItem;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Bot humain (PNJ) : morphologie de joueur (bipède cubique), skin sur mesure de sa faction, casques / gilets /
 * pantalons / rangers du mod, armes à feu ou armes blanches.
 * <p>
 * IA (voir {@link BotGoals}) : sommations, hostilité de faction, prise de couverture, rafales, snipers allongés,
 * servants de tourelles, patrouilles en binôme, escouades nomades des Faucons Blancs.
 */
public class HumanBotEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> FACTION = SynchedEntityData.defineId(HumanBotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(HumanBotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(HumanBotEntity.class, EntityDataSerializers.INT);
    /** Fin de la sommation (icône d'alerte jaune au-dessus de la tête). */
    private static final EntityDataAccessor<Long> ALERT_END = SynchedEntityData.defineId(HumanBotEntity.class, EntityDataSerializers.LONG);
    /** Fin du rechargement (bras gauche qui change le chargeur). */
    private static final EntityDataAccessor<Long> RELOAD_END = SynchedEntityData.defineId(HumanBotEntity.class, EntityDataSerializers.LONG);
    /** Heure du dernier tir (recul des bras). */
    private static final EntityDataAccessor<Long> SHOT_AT = SynchedEntityData.defineId(HumanBotEntity.class, EntityDataSerializers.LONG);
    /** Chef des Troupes de Choc : épée dans le dos / en train de dégainer / en main / en train de rengainer. */
    private static final EntityDataAccessor<Byte> SWORD_STATE = SynchedEntityData.defineId(HumanBotEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> SWORD_AT = SynchedEntityData.defineId(HumanBotEntity.class, EntityDataSerializers.LONG);

    public static final byte SWORD_BACK = 0, SWORD_DRAWING = 1, SWORD_DRAWN = 2, SWORD_SHEATHING = 3;
    public static final int DRAW_TICKS = 14, SHEATHE_TICKS = 16;

    // --- état serveur ---
    public BlockPos home;
    public int homeRadius = 24;
    public UUID partner;          // binôme de patrouille (le meneur)
    public UUID squad;            // escouade (Faucons Blancs)
    public boolean squadLeader;
    public BlockPos march;        // destination de l'escouade nomade
    public long lastRangedHit = -1000;
    public long scaredUntil;
    public long lastTargetSeen;
    public int turretFireTicks;   // servant de tourelle : gâchette pressée
    public String debugState = "";

    public HumanBotEntity(EntityType<? extends HumanBotEntity> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
        getNavigation().setCanFloat(true);
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation g) {
            g.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 96)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.ATTACK_KNOCKBACK, 0.2)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(FACTION, Faction.CIVILIAN.ordinal());
        b.define(ROLE, BotRole.CIVILIAN.ordinal());
        b.define(SKIN, 0);
        b.define(ALERT_END, 0L);
        b.define(RELOAD_END, 0L);
        b.define(SHOT_AT, -100L);
        b.define(SWORD_STATE, SWORD_BACK);
        b.define(SWORD_AT, -100L);
    }

    public Faction faction() {
        return Faction.values()[Math.floorMod(entityData.get(FACTION), Faction.values().length)];
    }

    public BotRole role() {
        return BotRole.values()[Math.floorMod(entityData.get(ROLE), BotRole.values().length)];
    }

    public int skin() {
        return entityData.get(SKIN);
    }

    public ResourceLocation skinTexture() {
        if (role() == BotRole.CHIEF) {
            return fr.modeminecraft.ModeMinecraft.id("textures/entity/bot/falcon_chief_0.png");
        }
        Faction f = faction();
        return fr.modeminecraft.ModeMinecraft.id("textures/entity/bot/" + f.id + "_" + Math.floorMod(skin(), f.skins) + ".png");
    }

    public long alertEnd() { return entityData.get(ALERT_END); }
    public void setAlertEnd(long t) { entityData.set(ALERT_END, t); }
    public long reloadEnd() { return entityData.get(RELOAD_END); }
    public void setReloadEnd(long t) { entityData.set(RELOAD_END, t); }
    public long shotAt() { return entityData.get(SHOT_AT); }
    public void markShot() { entityData.set(SHOT_AT, level().getGameTime()); }
    public byte swordState() { return entityData.get(SWORD_STATE); }
    public long swordAt() { return entityData.get(SWORD_AT); }

    // ------------------------------------------------------------------ création
    /** Donne faction, rôle, skin, équipement et points de vie. */
    public void setup(Faction faction, BotRole role, RandomSource r) {
        entityData.set(FACTION, faction.ordinal());
        entityData.set(ROLE, role.ordinal());
        entityData.set(SKIN, r.nextInt(Math.max(1, faction.skins)));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setItemSlot(slot, ItemStack.EMPTY);
        }
        double hp = switch (role) {
            case CHIEF -> 80;
            case COMMANDER -> 60;
            case BRUTE -> 40;
            case SCOUT, LANCER, ASSASSIN -> 30;
            case OFFICER -> 30;
            case CIVILIAN, TRADER -> 20;
            default -> 24;
        };
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth((float) hp);
        if (role == BotRole.CHIEF) {
            getAttribute(Attributes.SCALE).setBaseValue(1.15);
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.7);
        }
        if (role.falcon()) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(role == BotRole.SCOUT ? 0.36 : role == BotRole.BRUTE ? 0.27 : 0.32);
            FalconSquads.equip(this, role, r);
            return;
        }
        Faction.Loadout l = faction.loadout;
        if (!l.weapons().isEmpty() || !l.sniper().isEmpty()) {
            List<String> pool = role == BotRole.SNIPER && !l.sniper().isEmpty() ? l.sniper() : l.weapons();
            String w = pool.get(r.nextInt(pool.size()));
            if (role == BotRole.OFFICER && pool.size() > 1) {
                w = pool.get(0);
            }
            setItemSlot(EquipmentSlot.MAINHAND, armed(w));
        }
        boolean geared = role == BotRole.OFFICER || r.nextFloat() < 0.75f;
        if (geared) {
            pick(EquipmentSlot.HEAD, l.helmets(), r);
            pick(EquipmentSlot.CHEST, l.vests(), r);
            pick(EquipmentSlot.LEGS, l.pants(), r);
            pick(EquipmentSlot.FEET, l.boots(), r);
        }
        for (EquipmentSlot s : EquipmentSlot.values()) {
            setDropChance(s, s == EquipmentSlot.MAINHAND ? 0.12f : 0.05f);
        }
    }

    private void pick(EquipmentSlot slot, List<String> ids, RandomSource r) {
        if (!ids.isEmpty()) {
            setItemSlot(slot, new ItemStack(item(ids.get(r.nextInt(ids.size())))));
        }
    }

    public static Item item(String id) {
        return BuiltInRegistries.ITEM.get(fr.modeminecraft.ModeMinecraft.id(id));
    }

    /** Arme prête à tirer (chargeur plein, cartouche chambrée). */
    public static ItemStack armed(String id) {
        ItemStack s = new ItemStack(item(id));
        if (s.getItem() instanceof GunItem g) {
            GunItem.setAmmo(s, g.stats().magazineSize());
            s.set(fr.modeminecraft.registry.ModDataComponents.CHAMBERED.get(), true);
        }
        return s;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        if (role() == BotRole.CIVILIAN && faction() == Faction.CIVILIAN && getMainHandItem().isEmpty() && reason != MobSpawnType.STRUCTURE) {
            setup(Faction.CIVILIAN, BotRole.CIVILIAN, level.getRandom());
        }
        return d;
    }

    // ------------------------------------------------------------------ IA
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BotGoals.TurretGunner(this));
        goalSelector.addGoal(1, new BotGoals.Flee(this));
        goalSelector.addGoal(2, new BotGoals.GunCombat(this));
        goalSelector.addGoal(2, new BotGoals.MeleeCombat(this));
        goalSelector.addGoal(3, new BotGoals.Warn(this));
        goalSelector.addGoal(4, new BotGoals.SquadMarch(this));
        goalSelector.addGoal(5, new BotGoals.Patrol(this));
        goalSelector.addGoal(6, new OpenDoorGoal(this, true));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new BotGoals.Retaliate(this));
        targetSelector.addGoal(2, new BotGoals.SightTarget(this));
    }

    public boolean hasGun() {
        return getMainHandItem().getItem() instanceof GunItem;
    }

    /** Ce bot doit-il combattre cette créature ? */
    public boolean shouldAttack(LivingEntity t) {
        if (t == null || !t.isAlive() || t == this) {
            return false;
        }
        Faction f = faction();
        if (!f.combatant()) {
            return false;
        }
        if (t instanceof HumanBotEntity other) {
            if (other.faction() == f) {
                return false;
            }
            return f == Faction.FALCON ? other.faction().combatant() || other.faction() == Faction.CIVILIAN
                    : other.faction() == Faction.FALCON;
        }
        if (t instanceof Player p) {
            if (p.isCreative() || p.isSpectator()) {
                return false;
            }
            long now = level().getGameTime();
            return switch (f.attitude) {
                case PASSIVE -> false;
                case PROVOKED, WARN -> FactionRelations.isHostile(p.getUUID(), f, now);
                case ARMED_HOSTILE -> FactionRelations.isHostile(p.getUUID(), f, now) || BotGoals.armedInHand(p);
                case HOSTILE, HOSTILE_ALL -> true;
                case HOSTILE_INDOORS -> FactionRelations.isHostile(p.getUUID(), f, now) || !level().canSeeSky(p.blockPosition().above());
            };
        }
        // les bots armés se défendent contre les monstres proches
        return t instanceof net.minecraft.world.entity.monster.Enemy && !(t instanceof net.minecraft.world.entity.monster.Creeper)
                && distanceToSqr(t) < 16 * 16;
    }

    /** Même groupe (police + SWAT, ou même faction) : alliés, pas de tirs amis. */
    @Override
    public boolean isAlliedTo(Entity e) {
        return e instanceof HumanBotEntity b && b.faction().group.equals(faction().group) || super.isAlliedTo(e);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof HumanBotEntity b && b.faction() == faction()) && super.canAttack(target);
    }

    /** Portée au corps à corps : celle de l'arme (lances : 5 blocs). */
    public double reach() {
        if (getMainHandItem().getItem() instanceof MeleeWeaponItem m) {
            return m.stats().reach() * getScale();
        }
        if (getMainHandItem().is(ModItems.DRAGONSLAYER.get())) {
            return 4.0;
        }
        return 2.6 * getScale();
    }

    @Override
    public boolean isWithinMeleeAttackRange(LivingEntity target) {
        return getEyePosition().distanceTo(target.getBoundingBox().getCenter()) <= reach() + target.getBbWidth() / 2;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (getMainHandItem().is(ModItems.DRAGONSLAYER.get())) {
            return fr.modeminecraft.item.DragonslayerItem.cleave(this);
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return super.hurt(source, amount);
        }
        if (source.getEntity() instanceof HumanBotEntity ally && ally.faction().group.equals(faction().group)) {
            return false;                                            // balle perdue d'un allié : ignorée
        }
        if (source.getDirectEntity() instanceof Projectile) {
            lastRangedHit = level().getGameTime();
        }
        // Faucons Blancs : parade à l'arme blanche face aux coups de corps à corps venant de devant
        if (role().falcon() && source.getDirectEntity() instanceof LivingEntity attacker && source.getDirectEntity() == source.getEntity()
                && getMainHandItem().getItem() instanceof MeleeWeaponItem && random.nextFloat() < 0.25f) {
            Vec3 to = attacker.position().subtract(position()).normalize();
            if (to.dot(getLookAngle()) > 0.4) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1f, 1.3f);
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                attacker.knockback(0.4, getX() - attacker.getX(), getZ() - attacker.getZ());
                return false;
            }
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof Player p && !p.isCreative()) {
            FactionRelations.markHostile(p.getUUID(), faction(), level().getGameTime());
            BotEvents.alertFaction(this, p);
        }
        if (hurt && faction() == Faction.CIVILIAN || hurt && faction() == Faction.TRADER) {
            scaredUntil = level().getGameTime() + 200;
        }
        return hurt;
    }

    // ------------------------------------------------------------------ postures
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return switch (pose) {
            case CROUCHING -> EntityDimensions.scalable(0.6f, 1.5f).withEyeHeight(1.27f);
            case SWIMMING -> EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f);
            default -> EntityDimensions.scalable(0.6f, 1.8f).withEyeHeight(1.62f);
        };
    }

    public void setStance(Pose pose) {
        if (getPose() != pose) {
            setPose(pose);
            refreshDimensions();
        }
    }

    // ------------------------------------------------------------------ Chef des Troupes de Choc : épée dans le dos
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (role() == BotRole.CHIEF) {
            tickSword();
        }
        if (getPose() == Pose.CROUCHING && getTarget() == null && !isPassenger()) {
            setStance(Pose.STANDING);
        }
    }

    private void tickSword() {
        long now = level().getGameTime();
        byte state = swordState();
        long since = now - swordAt();
        LivingEntity target = getTarget();
        if (target != null) {
            lastTargetSeen = now;
        }
        switch (state) {
            case SWORD_BACK -> {
                if (target != null) {
                    setSword(SWORD_DRAWING, now);
                    level().playSound(null, getX(), getY(), getZ(), ModSounds.DRAGONSLAYER_UNSHEATHE.get(), SoundSource.HOSTILE, 1.4f, 1f);
                }
            }
            case SWORD_DRAWING -> {
                if (since >= DRAW_TICKS / 2 && getMainHandItem().isEmpty()) {
                    setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.DRAGONSLAYER.get()));
                    setDropChance(EquipmentSlot.MAINHAND, 0);
                }
                if (since >= DRAW_TICKS) {
                    setSword(SWORD_DRAWN, now);
                }
            }
            case SWORD_DRAWN -> {
                if (target == null && now - lastTargetSeen > 100) {
                    setSword(SWORD_SHEATHING, now);
                    level().playSound(null, getX(), getY(), getZ(), ModSounds.DRAGONSLAYER_SHEATHE.get(), SoundSource.HOSTILE, 1.2f, 1f);
                }
            }
            case SWORD_SHEATHING -> {
                if (since >= SHEATHE_TICKS / 2 && !getMainHandItem().isEmpty()) {
                    setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
                if (since >= SHEATHE_TICKS) {
                    setSword(SWORD_BACK, now);
                }
            }
            default -> setSword(SWORD_BACK, now);
        }
    }

    private void setSword(byte state, long now) {
        entityData.set(SWORD_STATE, state);
        entityData.set(SWORD_AT, now);
    }

    /** L'épée est-elle prête à frapper ? */
    public boolean swordReady() {
        return role() != BotRole.CHIEF || swordState() == SWORD_DRAWN;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        FalconSquads.dropLoot(this, level);
    }

    // ------------------------------------------------------------------ sons
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return (role().falcon() || faction() == Faction.SWAT) && !isPersistenceRequired();
    }

    // ------------------------------------------------------------------ sauvegarde
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Faction", faction().id);
        tag.putString("Role", role().name());
        tag.putInt("Skin", skin());
        tag.putByte("Sword", swordState());
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
            tag.putInt("HomeRadius", homeRadius);
        }
        if (partner != null) {
            tag.putUUID("Partner", partner);
        }
        if (squad != null) {
            tag.putUUID("Squad", squad);
            tag.putBoolean("SquadLeader", squadLeader);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Faction")) {
            entityData.set(FACTION, Faction.byId(tag.getString("Faction")).ordinal());
        }
        if (tag.contains("Role")) {
            try {
                entityData.set(ROLE, BotRole.valueOf(tag.getString("Role")).ordinal());
            } catch (IllegalArgumentException ignored) {
                // rôle inconnu : on garde la valeur par défaut
            }
        }
        entityData.set(SKIN, tag.getInt("Skin"));
        entityData.set(SWORD_STATE, tag.getByte("Sword") == SWORD_DRAWN ? SWORD_DRAWN : SWORD_BACK);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(p -> home = p);
        if (tag.contains("HomeRadius")) {
            homeRadius = tag.getInt("HomeRadius");
        }
        if (tag.hasUUID("Partner")) {
            partner = tag.getUUID("Partner");
        }
        if (tag.hasUUID("Squad")) {
            squad = tag.getUUID("Squad");
            squadLeader = tag.getBoolean("SquadLeader");
        }
    }

    /** Nom affiché au-dessus de la tête (rôle et faction). */
    @Override
    protected net.minecraft.network.chat.Component getTypeName() {
        return net.minecraft.network.chat.Component.translatable("entity.modeminecraft.bot." + faction().id + (role() == BotRole.CHIEF ? ".chief"
                : role() == BotRole.COMMANDER ? ".commander" : role() == BotRole.SNIPER ? ".sniper" : ""));
    }
}

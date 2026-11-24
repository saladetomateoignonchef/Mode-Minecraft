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
public class HumanBotEntity extends PathfinderMob implements net.minecraft.world.item.trading.Merchant {
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
    private static final EntityDataAccessor<Boolean> BERSERK = SynchedEntityData.defineId(HumanBotEntity.class, EntityDataSerializers.BOOLEAN);
    /** Activité visible d'un civil : assis, mains en l'air, en discussion, en lecture (voir CivilianLife). */
    private static final EntityDataAccessor<Byte> ACTIVITY = SynchedEntityData.defineId(HumanBotEntity.class, EntityDataSerializers.BYTE);
    /** Appel radio en cours : fin du compte à rebours de 30 s (icône et barre au-dessus de la tête). */
    private static final EntityDataAccessor<Long> RADIO_END = SynchedEntityData.defineId(HumanBotEntity.class, EntityDataSerializers.LONG);
    private static final ResourceLocation BERSERK_SPEED = fr.modeminecraft.ModeMinecraft.id("berserk_speed");

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
    /** Vie civile : dernier cri, commerçant derrière son comptoir (déjà braqué ?), occupé par un événement jusqu'à. */
    public long lastScream, eventUntil;
    public boolean shopkeeper, robbed;
    /** Pharmacienne de l'hôpital : vend les soins (bandages, garrots, attelles, plâtres, défibrillateurs). */
    public boolean pharmacy;
    /** Bandages qu'il lui reste pour se soigner en plein combat (voir BotGoals.SelfHeal). */
    public int healsLeft = 2;
    public long lastTargetSeen;
    public int turretFireTicks;   // servant de tourelle : gâchette pressée
    public String debugState = "";
    /** Équipage d'un convoi (disparaît quand aucun joueur n'est proche). */
    public boolean convoy;
    /** Combattant d'une grande bataille (OTAN contre URSS) : pas de limite d'escarmouche. */
    public boolean battle;
    /** Faucon Blanc en embuscade nocturne : silencieux et tapi jusqu'au premier coup. */
    public boolean ambush;

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
        b.define(RADIO_END, 0L);
        b.define(RELOAD_END, 0L);
        b.define(SHOT_AT, -100L);
        b.define(SWORD_STATE, SWORD_BACK);
        b.define(SWORD_AT, -100L);
        b.define(BERSERK, false);
        b.define(ACTIVITY, (byte) 0);
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
        if (role() == BotRole.BOSS) {
            return fr.modeminecraft.ModeMinecraft.id("textures/entity/bot/boss_" + faction().id + ".png");
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
    /** Chef des Troupes de Choc sous 30 % de ses PV : Mode Berserk. */
    public boolean berserk() { return entityData.get(BERSERK); }
    public byte activity() { return entityData.get(ACTIVITY); }
    public void setActivity(byte a) {
        if (activity() != a) {
            entityData.set(ACTIVITY, a);
        }
    }

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
            case BOSS -> 100;
            case CHIEF -> 80;
            case COMMANDER -> 60;
            case BRUTE -> 40;
            case SCOUT, LANCER, ASSASSIN -> 30;
            case OFFICER -> 30;
            case CIVILIAN, TRADER, CONTRACTOR -> 20;
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
        if (role == BotRole.BOSS) {
            FactionBosses.equip(this, faction);
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
            BotNight.equipLight(getMainHandItem(), faction, r);
        }
        if (role == BotRole.DRONE_OPERATOR) {
            setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.DRONE_CONTROLLER.get()));
        }
        if (role == BotRole.TRAIN_INSPECTOR) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.STUN_BATON.get()));      // matraque électrique (paralysie)
        }
        if (role.rail()) {
            setPersistenceRequired();
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
            setHealth(40);
            return;
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
        goalSelector.addGoal(1, new CivilianLife.HandsUp(this));
        goalSelector.addGoal(1, new BotGoals.Flee(this));
        goalSelector.addGoal(1, new BotGoals.DroneOperator(this));
        goalSelector.addGoal(2, new BotGoals.SelfHeal(this));
        goalSelector.addGoal(2, new BotGoals.GunCombat(this));
        goalSelector.addGoal(2, new BotGoals.MeleeCombat(this));
        goalSelector.addGoal(3, new BotGoals.Warn(this));
        goalSelector.addGoal(4, new BotGoals.SquadMarch(this));
        goalSelector.addGoal(5, new CivilianLife.Routine(this));
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
    // ------------------------------------------------------------------ témoin / appel radio
    private java.util.UUID grudge, radioTarget;
    private long grudgeUntil;

    /** Ce bot a vu ce joueur commettre un crime (hostilité personnelle, même sans alerte générale). */
    public void setGrudge(Player p, long until) {
        if (FactionRelations.isAlly(p.getUUID(), faction())) {
            return;                                                          // camp du joueur : jamais de rancune
        }
        grudge = p.getUUID();
        grudgeUntil = until;
    }

    public boolean hasGrudge(Player p) {
        return grudge != null && !FactionRelations.isAlly(p.getUUID(), faction()) && grudge.equals(p.getUUID()) && level().getGameTime() < grudgeUntil;
    }

    /** Le joueur est mort ou a changé de lieu : ce bot l'oublie complètement (cible, rancune, appel radio, sommation). */
    public void forgetPlayer(java.util.UUID id) {
        if (id.equals(grudge)) {
            grudge = null;
        }
        if (id.equals(radioTarget)) {
            startRadio(null, 0);
        }
        if (id.equals(ignored)) {
            ignored = null;
        }
        LivingEntity t = getTarget();
        if (t != null && t.getUUID().equals(id) || getLastHurtByMob() != null && getLastHurtByMob().getUUID().equals(id)) {
            setTarget(null);
            setLastHurtByMob(null);
            getNavigation().stop();
        }
    }

    /**
     * Portée maximale de poursuite d'un joueur : la portée de vue de la faction (45, 90 pour un sniper) plus 5 blocs, et
     * jamais au-delà de 50 blocs pour un fantassin ; pas de poursuite d'une dimension à l'autre. Au-delà : cible oubliée.
     */
    private void checkAggro() {
        LivingEntity t = getTarget();
        if (!(t instanceof Player p)) {
            return;
        }
        double max = role() == BotRole.SNIPER ? faction().sightRange(role()) + 5 : Math.min(50, faction().sightRange(role()) + 5);
        if (isPassenger()) {
            return;                                                          // équipages de véhicules et de tourelles : portée de l'arme
        }
        if (p.level() != level() || !p.isAlive() || distanceToSqr(p) > max * max || FactionRelations.isAlly(p.getUUID(), faction())) {
            setTarget(null);
            setLastHurtByMob(null);
            getNavigation().stop();
        }
    }

    public void forgetGrudge() {
        grudge = null;
        startRadio(null, 0);
    }

    public long radioEnd() {
        return entityData.get(RADIO_END);
    }

    public java.util.UUID radioTarget() {
        return radioTarget;
    }

    public void startRadio(Player p, long end) {
        radioTarget = p == null ? null : p.getUUID();
        entityData.set(RADIO_END, end);
    }

    /** Joueur qui a semé ce bot (Faucons Blancs) : ignoré jusqu'à {@link #ignoreUntil}, sauf s'il revient tout près. */
    public java.util.UUID ignored;
    public long ignoreUntil;

    /** Abandon de la poursuite (toute l'escouade autour) : le joueur leur a échappé. */
    public void giveUpOn(LivingEntity t) {
        long until = level().getGameTime() + 900;
        for (HumanBotEntity b : level().getEntitiesOfClass(HumanBotEntity.class, getBoundingBox().inflate(64),
                b -> b.faction() == faction() && (b.getTarget() == t || b.getLastHurtByMob() == t))) {
            b.setTarget(null);
            b.setLastHurtByMob(null);
            b.setAggressive(false);
            b.ignored = t.getUUID();
            b.ignoreUntil = until;
        }
    }

    public boolean shouldAttack(LivingEntity t) {
        if (t == null || !t.isAlive() || t == this) {
            return false;
        }
        // scénario : les prisonniers (capturés ou détenus) n'attaquent pas et ne sont pas pris pour cible
        if (getPersistentData().getBoolean("modeminecraft_prisoner") || t instanceof HumanBotEntity o && o.getPersistentData().getBoolean("modeminecraft_prisoner")
                || t.getTags().contains("modeminecraft_range_target")) {
            return false;
        }
        if (ignored != null && t.getUUID().equals(ignored) && level().getGameTime() < ignoreUntil && distanceToSqr(t) > 12 * 12) {
            return false;
        }
        Faction f = faction();
        if (!f.combatant()) {
            return false;
        }
        if (t instanceof fr.modeminecraft.entity.drone.DroneEntity d) {
            // drone d'un joueur en vol : les patrouilles ennemies l'abattent au fusil
            boolean flying = d.mode() == fr.modeminecraft.entity.drone.DroneEntity.PILOT || d.mode() == fr.modeminecraft.entity.drone.DroneEntity.RTH;
            boolean enemy = switch (f.attitude) {
                case HOSTILE, HOSTILE_ALL, HOSTILE_INDOORS, ARMED_HOSTILE -> true;
                default -> d.owner != null && FactionRelations.isHostile(d.owner, f, level().getGameTime());
            };
            return hasGun() && d.botTarget < 0 && flying && enemy;
        }
        if (t instanceof HumanBotEntity other) {
            if (other.faction() == f) {
                return false;
            }
            if (f == Faction.FALCON) {
                return other.faction().combatant() || other.faction() == Faction.CIVILIAN;
            }
            return other.faction() == Faction.FALCON || f.enemyOf(other.faction()) && (battle && other.battle || Skirmishes.allow(this, other));
        }
        if (t instanceof Player p) {
            if (p.isCreative() || p.isSpectator() || FactionRelations.isAlly(p.getUUID(), f)) {
                return false;                                              // l'armée de son propre camp ne tire jamais sur lui
            }
            long now = level().getGameTime();
            if (hasGrudge(p)) {
                return true;                                               // témoin d'un crime de ce joueur
            }
            if (Disguise.immune(this, p)) {
                return false;                                              // déguisé en membre de la faction
            }
            // scénario « Course à l'arme nucléaire » : l'armée de son pays est alliée (sauf trahison), l'adverse tire à vue
            fr.modeminecraft.scenario.Side side = fr.modeminecraft.scenario.Scenario.side(p);
            if (side != null) {
                if (f == side.ally) {
                    return FactionRelations.isHostile(p.getUUID(), f, now);
                }
                if (f == side.enemy) {
                    return true;
                }
            }
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
        boolean hit;
        if (getMainHandItem().is(ModItems.DRAGONSLAYER.get())) {
            hit = fr.modeminecraft.item.DragonslayerItem.cleave(this);
        } else {
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            hit = super.doHurtTarget(target);
        }
        if (hit && ambush) {
            FalconSquads.endAmbush(this);
        }
        return hit;
    }

    /** En embuscade : pas un bruit de pas. */
    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!ambush) {
            super.playStepSound(pos, state);
        }
    }

    private int lastAllyShot = -1000;

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return super.hurt(source, amount);
        }
        if (source.getEntity() instanceof HumanBotEntity ally && ally.faction().group.equals(faction().group)) {
            return false;                                            // balle perdue d'un allié : ignorée
        }
        if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp && tickCount - lastAllyShot > 40
                && fr.modeminecraft.scenario.Scenario.side(sp) != null && fr.modeminecraft.scenario.Scenario.side(sp).ally == faction()) {
            lastAllyShot = tickCount;
            fr.modeminecraft.scenario.Reputation.add(sp, -10, "rep.modeminecraft.shot_ally");   // tir sur un allié
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
        if (hurt && ambush) {
            FalconSquads.endAmbush(this);
        }
        if (hurt && source.getEntity() instanceof Player p && !p.isCreative() && level() instanceof net.minecraft.server.level.ServerLevel sl) {
            // agression : seuls la victime et ceux qui ont vu la scène réagissent (appel radio de 30 s)
            RadioCalls.crime(sl, p, position(), this);
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
    /** Aucun joueur à moins de 48 blocs (recalculé chaque seconde) : l'IA tourne au ralenti. */
    private boolean farFromPlayers;

    @Override
    public void aiStep() {
        if (!level().isClientSide && radioEnd() > 0) {
            RadioCalls.tick(this);
        }
        if (!level().isClientSide && (tickCount + getId()) % 20 == 0 && fr.modeminecraft.item.ShieldCombat.blocked(this)) {
            // un bot au bouclier (SWAT) ne garde qu'une arme de poing ou une arme blanche légère
            setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(item("glock17")));
        }
        if (!level().isClientSide && (tickCount + getId()) % 10 == 0) {
            checkAggro();
        }
        if (!level().isClientSide) {
            if ((tickCount + getId()) % 20 == 0) {
                farFromPlayers = level().getNearestPlayer(this, 48) == null;
            }
            // économie : un bot loin des joueurs, au repos, ne réfléchit qu'un tick sur quatre
            if (farFromPlayers && (tickCount + getId()) % 4 != 0 && onGround() && getNavigation().isDone() && getTarget() == null
                    && !isPassenger()) {
                return;
            }
        }
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (tradingPlayer != null && (!tradingPlayer.isAlive() || !(tradingPlayer.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu)
                || tradingPlayer.distanceToSqr(this) > 64)) {
            tradingPlayer = null;
        }
        if (tradingPlayer != null) {
            getNavigation().stop();
            getLookControl().setLookAt(tradingPlayer);
        }
        BotNight.tick(this);
        tickBoss();
        if (role() == BotRole.CHIEF) {
            tickSword();
            if (!berserk() && isAlive() && getHealth() <= getMaxHealth() * 0.3f) {
                enterBerserk();
            }
        }
        if (ambush && !fr.modeminecraft.world.night.NightOps.isNight(level())) {
            ambush = false;                                          // le jour se lève : l'embuscade est levée
        }
        if (getPose() == Pose.CROUCHING && getTarget() == null && !isPassenger() && !ambush) {
            setStance(Pose.STANDING);
        }
    }

    // ------------------------------------------------------------------ Chefs de Faction : barre de boss
    private net.minecraft.server.level.ServerBossEvent bossBar;
    private boolean bossChecked;

    private void tickBoss() {
        String id = FactionBosses.bossId(this);
        if (id == null) {
            return;
        }
        if (!bossChecked && level() instanceof net.minecraft.server.level.ServerLevel sl) {
            bossChecked = true;
            if (role() == BotRole.BOSS && FactionBosses.alreadyDefeated(sl, id)) {
                setup(faction(), BotRole.OFFICER, random);    // ce Chef unique est déjà tombé : un simple officier le remplace
                return;
            }
        }
        if (bossBar == null) {
            bossBar = new net.minecraft.server.level.ServerBossEvent(getDisplayName(), FactionBosses.color(faction()),
                    net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_10);
        }
        bossBar.setProgress(getHealth() / getMaxHealth());
        bossBar.setName(getDisplayName());
        // visible pour les joueurs proches qui sont en train de se battre (ou repérés)
        if (tickCount % 10 == 0) {
            for (var p : java.util.List.copyOf(bossBar.getPlayers())) {
                if (p.distanceToSqr(this) > 48 * 48 || !p.isAlive() || !isAlive()) {
                    bossBar.removePlayer(p);
                }
            }
            if (isAlive()) {
                for (var p : level().players()) {
                    if (p instanceof net.minecraft.server.level.ServerPlayer sp && sp.distanceToSqr(this) < 32 * 32
                            && (getTarget() == sp || hasLineOfSight(sp))) {
                        bossBar.addPlayer(sp);
                    }
                }
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            RadioCalls.cut(this);                                          // transmission coupée
            if (source.getEntity() instanceof Player p && level() instanceof net.minecraft.server.level.ServerLevel sl) {
                RadioCalls.crime(sl, p, position(), null);                 // meurtre : témoins éventuels
            }
        }
        super.die(source);
        if (!level().isClientSide) {
            FactionBosses.onDeath(this, source.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null);
            if (bossBar != null) {
                bossBar.removeAllPlayers();
            }
        }
    }

    @Override
    public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    /**
     * Mode Berserk : heaume fissuré aux yeux rouges, fumée et étincelles, cri de guerre (40 blocs),
     * insensible au recul, +40 % de vitesse, coups plus rapprochés (-25 %).
     */
    public void enterBerserk() {
        entityData.set(BERSERK, true);
        applyBerserk();
        level().playSound(null, getX(), getY(), getZ(), ModSounds.BERSERK_ROAR.get(), SoundSource.HOSTILE, 2.5f, 0.9f);
        if (swordState() == SWORD_BACK) {
            setSword(SWORD_DRAWING, level().getGameTime());
        }
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.5, 0.8, 0.5, 0.05);
        }
    }

    private void applyBerserk() {
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (!speed.hasModifier(BERSERK_SPEED)) {
            speed.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BERSERK_SPEED, 0.4,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && berserk() && isAlive()) {
            // fumée sombre et étincelles rouges autour de l'armure
            if (random.nextInt(2) == 0) {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.03, 0);
            }
            level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1f, 0.1f, 0.05f), 0.8f),
                    getRandomX(0.7), getRandomY(), getRandomZ(0.7), 0, 0.05, 0);
            if (random.nextInt(6) == 0) {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.LAVA, getRandomX(0.5), getY() + 1.2, getRandomZ(0.5), 0, 0, 0);
            }
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
        FactionBosses.dropLoot(this);
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
        return (role().falcon() || faction() == Faction.SWAT || convoy) && !isPersistenceRequired();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            fr.modeminecraft.item.gun.FlashlightHandler.clear(this);   // plus de faisceau orphelin
        }
        super.remove(reason);
    }

    // ------------------------------------------------------------------ sauvegarde
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Faction", faction().id);
        tag.putString("Role", role().name());
        tag.putInt("Skin", skin());
        tag.putByte("Sword", swordState());
        tag.putBoolean("Berserk", berserk());
        tag.putBoolean("Convoy", convoy);
        tag.putBoolean("Ambush", ambush);
        tag.putBoolean("Shopkeeper", shopkeeper);
        tag.putBoolean("Pharmacy", pharmacy);
        tag.putInt("Heals", healsLeft);
        tag.putBoolean("Robbed", robbed);
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
        convoy = tag.getBoolean("Convoy");
        ambush = tag.getBoolean("Ambush");
        shopkeeper = tag.getBoolean("Shopkeeper");
        pharmacy = tag.getBoolean("Pharmacy");
        healsLeft = tag.contains("Heals") ? tag.getInt("Heals") : 2;
        robbed = tag.getBoolean("Robbed");
        entityData.set(SWORD_STATE, tag.getByte("Sword") == SWORD_DRAWN ? SWORD_DRAWN : SWORD_BACK);
        if (tag.getBoolean("Berserk")) {
            entityData.set(BERSERK, true);
            applyBerserk();
        }
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

    // ------------------------------------------------------------------ marché noir (marchands clandestins)
    private Player tradingPlayer;
    private net.minecraft.world.item.trading.MerchantOffers offers;

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        // scénario : personnel de base, scientifique, petites tâches des soldats de sa base
        if (isAlive() && hand == net.minecraft.world.InteractionHand.MAIN_HAND && player instanceof net.minecraft.server.level.ServerPlayer sp
                && tradingPlayer == null && fr.modeminecraft.scenario.ScenarioNpc.interact(this, sp)) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (level().isClientSide && (role().staff() || role() == BotRole.SCIENTIST)) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (role() == BotRole.TICKET_CLERK && isAlive()) {
            if (!level().isClientSide) {
                getLookControl().setLookAt(player);
                fr.modeminecraft.entity.train.TrainStaff.sellTicket(this, player);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (role() == BotRole.CONTRACTOR && isAlive()) {
            if (!level().isClientSide) {
                getLookControl().setLookAt(player);
                Contracts.interact(this, player);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        // arme en main face à un civil : c'est un braquage (maintenir le clic droit), pas un achat
        if ((role() == BotRole.TRADER || shopkeeper && scaredUntil < level().getGameTime()) && isAlive() && tradingPlayer == null && !player.isSecondaryUseActive()
                && !BotGoals.armedInHand(player)) {
            if (!level().isClientSide) {
                setTradingPlayer(player);
                openTradingScreen(player, getDisplayName(), 1);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void setTradingPlayer(Player player) {
        tradingPlayer = player;
    }

    /** Boutique du personnel de base (offres calculées pour ce joueur : grade, camp). */
    public void openShop(Player player, net.minecraft.world.item.trading.MerchantOffers custom) {
        offers = custom;
        setTradingPlayer(player);
        openTradingScreen(player, getDisplayName(), 1);
    }

    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public net.minecraft.world.item.trading.MerchantOffers getOffers() {
        if (offers == null) {
            offers = pharmacy ? CivilianLife.pharmacyOffers() : shopkeeper ? CivilianLife.shopOffers(random) : BlackMarket.offers(random);
        }
        return offers;
    }

    @Override
    public void overrideOffers(net.minecraft.world.item.trading.MerchantOffers o) {
    }

    @Override
    public void notifyTrade(net.minecraft.world.item.trading.MerchantOffer offer) {
        offer.increaseUses();
        playSound(SoundEvents.UI_STONECUTTER_TAKE_RESULT, 0.6f, 1.2f);
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.UI_STONECUTTER_TAKE_RESULT;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide;
    }

    /** Nom affiché au-dessus de la tête (rôle et faction). */
    @Override
    protected net.minecraft.network.chat.Component getTypeName() {
        if (role() == BotRole.CONTRACTOR) {
            return net.minecraft.network.chat.Component.translatable("entity.modeminecraft.bot.contractor");
        }
        if (role().staff() || role() == BotRole.SCIENTIST) {
            return net.minecraft.network.chat.Component.translatable("entity.modeminecraft.bot." + role().name().toLowerCase()
                    + (role() == BotRole.SCIENTIST ? "" : "." + faction().id));
        }
        if (role().rail()) {
            return net.minecraft.network.chat.Component.translatable("entity.modeminecraft.bot." + role().name().toLowerCase());
        }
        if (role() == BotRole.BOSS) {
            return net.minecraft.network.chat.Component.translatable("entity.modeminecraft.bot.boss_" + faction().id);
        }
        return net.minecraft.network.chat.Component.translatable("entity.modeminecraft.bot." + faction().id + (role() == BotRole.CHIEF ? ".chief"
                : role() == BotRole.COMMANDER ? ".commander" : role() == BotRole.SNIPER ? ".sniper" : ""));
    }
}

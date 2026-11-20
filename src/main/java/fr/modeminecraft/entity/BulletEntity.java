package fr.modeminecraft.entity;

import fr.modeminecraft.item.gun.AmmoType;
import fr.modeminecraft.registry.ModDamageTypes;
import fr.modeminecraft.registry.ModParticles;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * Balle tirée par une arme à feu.
 * - Va très vite, retombe un peu avec la distance et est déviée par le vent ({@link Wind}).
 * - Zones : tête = dégâts x2, jambes = x0,75 et la cible est ralentie.
 * - Casse le verre (si la règle mobGriefing est activée) et traverse les blocs fins (feuilles, laine, bois)
 *   selon sa puissance ; peut ricocher sur la pierre ou le métal quand elle arrive en biais.
 * - Son type (traçante, explosive, incendiaire...) ajoute un effet à l'impact, voir {@link #applyTypeEffect}.
 */
public class BulletEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE_TICKS = 60;
    private float headshotMultiplier = 2.0f;   // x2,5 pour les fusils de précision
    private float armorPenetration = 0;        // part de l'armure ignorée (selon le calibre)
    private static final float LEGS_MULTIPLIER = 0.75f;
    private static final double GRAVITY = 0.0025;

    /** Arme qui a tiré cette balle (pour ses statistiques), ou vide. */
    private net.minecraft.world.item.ItemStack weapon = net.minecraft.world.item.ItemStack.EMPTY;

    /** Mitrailleuses lourdes : brise les blocs fragiles (vitres, feuilles, lanternes, trappes en bois...). */
    private boolean breaksFragile;
    /** Recul supplémentaire infligé à la cible (M2 : fort recul). */
    private float knockback;
    /** Obus explosif (Flakvierling 20 mm) : micro-explosion de cette puissance à l'impact. */
    private float impactExplosion;

    public void setHeavy(boolean breaksFragile, float knockback, float impactExplosion) {
        this.breaksFragile = breaksFragile;
        this.knockback = knockback;
        this.impactExplosion = impactExplosion;
    }

    private static boolean isFragile(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.LEAVES) || state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)
                || state.is(net.minecraft.world.level.block.Blocks.LANTERN) || state.is(net.minecraft.world.level.block.Blocks.SOUL_LANTERN)
                || state.is(net.minecraft.tags.BlockTags.WOODEN_TRAPDOORS) || state.is(net.minecraft.tags.BlockTags.FLOWER_POTS)
                || state.is(net.minecraft.tags.BlockTags.WOODEN_BUTTONS) || state.is(net.minecraft.tags.BlockTags.WOODEN_PRESSURE_PLATES)
                || state.is(net.minecraft.world.level.block.Blocks.TORCH) || state.is(net.minecraft.world.level.block.Blocks.WALL_TORCH)
                || state.is(Tags.Blocks.FENCES_WOODEN) || state.is(net.minecraft.world.level.block.Blocks.SCAFFOLDING);
    }

    /**
     * Casque militaire : Dégâts = Dégâts de base x Multiplicateur tête x (1 - réduction du casque).
     * Le casque s'use selon les dégâts absorbés ; à partir de -50 % : étincelles et bruit de ricochet.
     * PASGT : les calibres de pistolet (9 mm, .45 ACP) ne font pas de coup critique à la tête.
     */
    private float helmetProtection(ServerLevel level, LivingEntity target, float damage) {
        var helm = target.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (!(helm.getItem() instanceof fr.modeminecraft.item.HelmetItem h)) {
            return damage;
        }
        if (h == fr.modeminecraft.registry.ModItems.PASGT.get() && weapon.getItem() instanceof fr.modeminecraft.item.gun.GunItem gun) {
            String caliber = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(gun.stats().ammo().get()).getPath();
            if (caliber.equals("ammo_9mm") || caliber.equals("ammo_45acp")) {
                damage /= headshotMultiplier;
            }
        }
        float reduction = h.headshotReduction(helm);
        float absorbed = damage * reduction;
        helm.hurtAndBreak(Math.max(1, Math.round(absorbed / 2)), target, net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (reduction >= 0.5f) {
            level.playSound(null, target.getX(), target.getEyeY(), target.getZ(), fr.modeminecraft.registry.ModSounds.HELMET_RICOCHET.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.9f + random.nextFloat() * 0.2f);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY() + 0.25, target.getZ(), 14, 0.15, 0.1, 0.15, 0.35);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getEyeY() + 0.3, target.getZ(), 6, 0.1, 0.1, 0.1, 0.2);
        }
        return damage - absorbed;
    }

    /** Classe de calibre pour les gilets : -1 = d'après l'arme, 0 = léger, 1 = fusil, 2 = gros calibre. */
    private int caliberClass = -1;

    public void setCaliberClass(int c) {
        caliberClass = c;
    }

    public int caliberClass() {
        if (caliberClass >= 0) {
            return caliberClass;
        }
        if (weapon.getItem() instanceof fr.modeminecraft.item.gun.GunItem gun) {
            var st = gun.stats();
            String caliber = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.ammo().get()).getPath();
            if (st.category() == fr.modeminecraft.item.gun.GunStats.Category.SNIPER || caliber.equals("ammo_50bmg")
                    || caliber.equals("ammo_408") || caliber.equals("ammo_338")) {
                return 2;
            }
            return switch (st.category()) {
                case PISTOL, SMG, SHOTGUN -> 0;
                default -> 1;
            };
        }
        return damage < 7 ? 0 : damage < 13 ? 1 : 2;
    }

    public void setWeapon(net.minecraft.world.item.ItemStack stack) {
        weapon = stack;
    }

    public net.minecraft.world.item.ItemStack weapon() {
        return weapon;
    }

    public void setHeadshotMultiplier(float mul) {
        headshotMultiplier = mul;
    }

    private float falloff = 0;          // perte de dégâts par bloc parcouru
    private float noDropBlocks = 0;     // pas de chute de balle sur cette distance
    private int lifeTicks = MAX_LIFE_TICKS;
    private Vec3 origin;

    /** Balistique propre à l'arme (voir GunStats). */
    public void setBallistics(float falloffPerBlock, float noDropBlocks, int lifeTicks) {
        this.falloff = falloffPerBlock;
        this.noDropBlocks = noDropBlocks;
        this.lifeTicks = lifeTicks;
    }

    private double travelled() {
        return origin == null ? 0 : origin.distanceTo(position());
    }

    public void setArmorPenetration(float fraction) {
        armorPenetration = Math.max(0, Math.min(1, fraction));
    }

    /** Type de munition, synchronisé avec le client (pour la traînée des traçantes). */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> TYPE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(BulletEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);

    private float damage = 5.0f;
    private int pierceLeft = 0;          // ennemis encore traversables
    private boolean armorPiercing = false;
    private float penetration = 0;       // "puissance" restante pour traverser des blocs
    private boolean ricocheted = false;
    private net.minecraft.core.Direction lastFace;
    private final java.util.Set<Integer> alreadyHit = new java.util.HashSet<>();

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public BulletEntity(Level level, LivingEntity shooter, float damage, int pierce, boolean armorPiercing, AmmoType type) {
        super(ModEntities.BULLET.get(), shooter, level);
        this.damage = damage;
        this.pierceLeft = pierce;
        this.armorPiercing = armorPiercing;
        this.armorPenetration = armorPiercing ? 1 : 0;
        this.penetration = damage * 0.4f * (armorPiercing ? 2 : 1);
        this.entityData.set(TYPE, type.ordinal());
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 0);
    }

    public AmmoType ammoType() {
        int i = entityData.get(TYPE);
        return i >= 0 && i < AmmoType.values().length ? AmmoType.values()[i] : AmmoType.STANDARD;
    }

    /** Une balle perforante ne touche pas deux fois le même ennemi. */
    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !alreadyHit.contains(target.getId());
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.AMMO_9MM.get();
    }

    @Override
    protected double getDefaultGravity() {
        if (ammoType() == AmmoType.DRAGON_BREATH || travelled() < noDropBlocks) {
            return 0.0; // M24 : trajectoire tendue sur 100 blocs
        }
        return GRAVITY; // la balle retombe un peu
    }

    @Override
    public void tick() {
        Vec3 before = position();
        if (origin == null) {
            origin = position();
        }
        if (!isInWater()) {
            // Le vent pousse la balle sur le côté (même calcul côté client et serveur)
            Vec3 wind = fr.modeminecraft.item.gun.Wind.get(level()).scale(fr.modeminecraft.item.gun.Wind.ACCEL_PER_MS);
            setDeltaMovement(getDeltaMovement().add(wind));
        }
        super.tick();
        AmmoType type = ammoType();
        if (level().isClientSide) {
            // Traçante : une traînée lumineuse le long de la trajectoire, visible par tout le monde
            if (type == AmmoType.TRACER || type == AmmoType.DRAGON_BREATH) {
                Vec3 after = position();
                double length = after.distanceTo(before);
                int steps = Math.max(1, (int) (length / 0.2));
                Entity owner = getOwner();
                for (int i = 0; i < steps; i++) {
                    Vec3 p = before.lerp(after, i / (double) steps);
                    // Rien juste devant le tireur : la traînée ne doit pas masquer sa vue
                    if (owner != null && owner.getEyePosition().distanceToSqr(p) < 2.5 * 2.5) {
                        continue;
                    }
                    level().addParticle(type == AmmoType.TRACER ? ModParticles.TRACER.get() : ParticleTypes.FLAME,
                            p.x, p.y, p.z, 0, 0, 0);
                }
            }
        } else if (tickCount > (type == AmmoType.DRAGON_BREATH ? 8 : lifeTicks)
                || getY() > level().getMaxBuildHeight() + 64) {
            discard(); // le souffle du dragon porte très peu loin
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Entity target = result.getEntity();
        float finalDamage = damage * (float) Math.max(0.35, 1 - falloff * travelled()); // perte avec la distance

        // Zone touchée : on regarde à quelle hauteur la balle traverse la hitbox de la cible
        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement());
        double hitY = target.getBoundingBox().clip(start, end).map(h -> h.y).orElse(target.getY() + target.getBbHeight() / 2);
        boolean headshot = target instanceof LivingEntity living && hitY >= living.getEyeY() - 0.25;
        boolean legs = target instanceof LivingEntity && !headshot && hitY < target.getY() + target.getBbHeight() * 0.35;
        // Bras : touché sur le côté du corps (loin du centre, perpendiculairement au regard de la cible)
        if (target instanceof LivingEntity living && !headshot && !legs) {
            Vec3 hitPoint = target.getBoundingBox().clip(start, end).orElse(target.position());
            float yaw = (float) Math.toRadians(living.yBodyRot);
            double side = (hitPoint.x - target.getX()) * -Math.cos(yaw) + (hitPoint.z - target.getZ()) * -Math.sin(yaw);
            legs = Math.abs(side) > target.getBbWidth() * 0.34; // les membres comptent pareil : x0,75
        }
        if (legs) {
            finalDamage *= LEGS_MULTIPLIER;
            ((LivingEntity) target).addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), getOwner());
        }
        if (headshot) {
            finalDamage *= headshotMultiplier;
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(), 8, 0.2, 0.2, 0.2, 0.2);
            finalDamage = helmetProtection(level, (LivingEntity) target, finalDamage);
        }

        // Pantalon renforcé : balle dans les jambes (pas dans les bras)
        if (legs && hitY < target.getY() + target.getBbHeight() * 0.375 && target instanceof LivingEntity living) {
            finalDamage = fr.modeminecraft.item.GearItem.protectLegs(level, living, finalDamage, caliberClass());
        }
        // Gilet pare-balles : tir au torse (ni la tête ni les membres) -> plaque balistique puis gilet
        if (!headshot && !legs && target instanceof LivingEntity living) {
            Vec3 look = Vec3.directionFromRotation(0, living.yBodyRot);
            boolean front = getDeltaMovement().x * look.x + getDeltaMovement().z * look.z < 0;
            finalDamage = fr.modeminecraft.item.VestItem.protect(level, living, finalDamage, caliberClass(), front);
        }
        AmmoType type = ammoType();
        if (type == AmmoType.HOLLOW_POINT && target instanceof LivingEntity living) {
            finalDamage *= living.getArmorValue() > 0 ? 0.6f : 1.35f; // très efficace sans armure
        }
        // Perforation d'armure : on calcule nous-mêmes l'armure restante, selon le calibre
        DamageSource source = level.damageSources().source(ModDamageTypes.BULLET_AP, this, getOwner());
        if (target instanceof LivingEntity living && armorPenetration < 1) {
            float armor = living.getArmorValue() * (1 - armorPenetration);
            float toughness = (float) living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)
                    * (1 - armorPenetration);
            finalDamage = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(living, finalDamage, source, armor, toughness);
        }
        if (target.hurt(source, finalDamage)) {
            applyTypeEffect(level, target, finalDamage);
            if (knockback > 0 && target instanceof LivingEntity living) {
                Vec3 push = getDeltaMovement().normalize().scale(knockback);
                living.push(push.x, 0.15 * knockback, push.z);
                living.hurtMarked = true;
            }
            if (getOwner() instanceof net.minecraft.server.level.ServerPlayer shooter && target instanceof LivingEntity) {
                int kind = (!target.isAlive() ? fr.modeminecraft.network.HitMarkerPayload.KILL : fr.modeminecraft.network.HitMarkerPayload.HIT)
                        | (headshot ? fr.modeminecraft.network.HitMarkerPayload.HEADSHOT : 0);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(shooter, new fr.modeminecraft.network.HitMarkerPayload(kind));
                if (!target.isAlive() && weapon.getItem() instanceof fr.modeminecraft.item.gun.GunItem) {
                    fr.modeminecraft.item.gun.GunItem.recordKill(weapon, shooter, headshot, ((LivingEntity) target).getMaxHealth() >= 50);
                }
            }
            // Supprime l'invincibilité temporaire : sinon les plombs du fusil à pompe
            // et les rafales ne toucheraient qu'une fois sur deux.
            target.invulnerableTime = 0;
        }
        if (impactExplosion > 0) {
            level.explode(this, getX(), getY(), getZ(), impactExplosion, Level.ExplosionInteraction.NONE);
            discard();
            return;
        }
        if (pierceLeft > 0) {
            // Balle perforante (Barrett) : elle continue sa route
            pierceLeft--;
            alreadyHit.add(target.getId());
            damage *= 0.8f;
        } else {
            discard();
        }
    }

    /** Effet du type de munition sur l'ennemi touché. */
    private void applyTypeEffect(ServerLevel level, Entity target, float dealt) {
        LivingEntity living = target instanceof LivingEntity l ? l : null;
        switch (ammoType()) {
            case EXPLOSIVE -> level.explode(this, getX(), getY(), getZ(), 1.2f, Level.ExplosionInteraction.NONE);
            case INCENDIARY, DRAGON_BREATH -> target.igniteForSeconds(4);
            case RUBBER -> {
                if (living != null) {
                    Vec3 push = getDeltaMovement().normalize();
                    living.knockback(1.4, -push.x, -push.z);
                }
            }
            case MARKER -> { if (living != null) living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200), getOwner()); }
            case VENOM -> { if (living != null) living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), getOwner()); }
            case CRYO -> {
                if (living != null) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), getOwner());
                    living.setTicksFrozen(Math.max(living.getTicksFrozen(), living.getTicksRequiredToFreeze() + 30));
                }
            }
            case ELECTRIC -> {
                if (living != null) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4), getOwner());
                }
                // L'éclair saute sur l'ennemi le plus proche
                level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4),
                                e -> e != target && e != getOwner() && e.isAlive())
                        .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(target)))
                        .ifPresent(next -> {
                            next.hurt(level.damageSources().source(ModDamageTypes.BULLET, this, getOwner()), dealt * 0.5f);
                            Vec3 a = target.position().add(0, target.getBbHeight() / 2, 0);
                            Vec3 b = next.position().add(0, next.getBbHeight() / 2, 0);
                            for (int i = 0; i <= 8; i++) {
                                Vec3 p = a.lerp(b, i / 8.0);
                                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
                            }
                        });
                level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.3f, 2.0f);
            }
            default -> { }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel level)) {
            return; // le serveur décide (traverser, ricocher ou s'arrêter)
        }
        BlockPos pos = result.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Vec3 hit = result.getLocation();
        AmmoType type = ammoType();
        if (impactExplosion > 0) {
            level.explode(this, hit.x, hit.y, hit.z, impactExplosion, Level.ExplosionInteraction.NONE);
            discard();
            return;
        }
        if (fr.modeminecraft.block.city.BreakableDecoBlock.shot(level, pos, state, this)) {
            discard();                                   // bouche d'incendie / extincteur touché
            return;
        }
        if (breaksFragile && isFragile(state) && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            level.destroyBlock(pos, false, getOwner());
            passThrough(pos, hit, getDeltaMovement().normalize(), 0.95f);
            return;
        }
        if (type == AmmoType.EXPLOSIVE) {
            level.explode(this, hit.x, hit.y, hit.z, 1.2f, Level.ExplosionInteraction.NONE);
            discard();
            return;
        }
        boolean griefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        if ((type == AmmoType.INCENDIARY || type == AmmoType.DRAGON_BREATH) && griefing) {
            BlockPos fire = pos.relative(result.getDirection());
            if (net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(level, fire, result.getDirection())) {
                level.setBlockAndUpdate(fire, net.minecraft.world.level.block.BaseFireBlock.getState(level, fire));
            }
        }
        if (state.getBlock() instanceof fr.modeminecraft.block.ShootingTargetBlock) {
            fr.modeminecraft.block.ShootingTargetBlock.onBulletHit(level, pos, state, hit, getOwner());
            discard();
            return;
        }
        Vec3 dir = getDeltaMovement().normalize();
        lastFace = result.getDirection();

        // Verre : il casse et la balle continue
        boolean glass = state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES);
        if (glass && griefing) {
            level.destroyBlock(pos, false, getOwner());
            passThrough(pos, hit, dir, 0.9f);
            return;
        }
        // Blocs fins : la balle les traverse si elle est assez puissante
        int cost = penetrationCost(state);
        if (cost > 0 && penetration >= cost) {
            penetration -= cost;
            impact(level, state, pos, hit);
            passThrough(pos, hit, dir, 0.7f); // -30 % de dégâts par paroi traversée
            return;
        }
        // Ricochet : sur un bloc dur, quand la balle arrive très en biais
        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        double facing = -dir.dot(normal); // 1 = de face, 0 = parallèle à la surface
        if (!ricocheted && facing < 0.34 && isHard(level, state, pos) && random.nextFloat() < 0.7f) {
            ricocheted = true;
            Vec3 v = getDeltaMovement();
            Vec3 bounce = v.subtract(normal.scale(2 * v.dot(normal))).scale(0.6);
            damage *= 0.5f;
            setDeltaMovement(bounce);
            hasImpulse = true;
            // ThrowableProjectile ajoute ensuite la vitesse : on recule d'autant pour repartir du point d'impact
            setPos(hit.add(normal.scale(0.02)).subtract(bounce));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y, hit.z, 6, 0.03, 0.03, 0.03, 0.15);
            level.sendParticles(ParticleTypes.CRIT, hit.x, hit.y, hit.z, 3, 0.02, 0.02, 0.02, 0.1);
            level.playSound(null, pos, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.BLOCKS, 0.7f, 1.7f + random.nextFloat() * 0.3f);
            return;
        }
        if (glass) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), hit.x, hit.y, hit.z, 6, 0.05, 0.05, 0.05, 0.1);
        } else {
            impact(level, state, pos, hit);
        }
        discard();
    }

    /** Coût pour traverser un bloc (0 = impossible). */
    private static int penetrationCost(BlockState state) {
        if (state.is(net.minecraft.tags.BlockTags.LEAVES)) {
            return 1;
        }
        if (state.is(net.minecraft.tags.BlockTags.WOOL) || state.is(net.minecraft.tags.BlockTags.WOOL_CARPETS)) {
            return 2;
        }
        var sound = state.getSoundType();
        if (state.is(net.minecraft.tags.BlockTags.PLANKS) || state.is(net.minecraft.tags.BlockTags.LOGS)
                || sound == net.minecraft.world.level.block.SoundType.WOOD || sound == net.minecraft.world.level.block.SoundType.NETHER_WOOD
                || sound == net.minecraft.world.level.block.SoundType.BAMBOO_WOOD || sound == net.minecraft.world.level.block.SoundType.CHERRY_WOOD) {
            return 3;
        }
        return 0;
    }

    /** Bloc dur sur lequel une balle peut ricocher (pierre, métal...). */
    private static boolean isHard(ServerLevel level, BlockState state, BlockPos pos) {
        return state.getDestroySpeed(level, pos) >= 1.5f && penetrationCost(state) == 0;
    }

    /** La balle ressort de l'autre côté du bloc, un peu ralentie. */
    private void passThrough(BlockPos pos, Vec3 hit, Vec3 dir, float damageMul) {
        Vec3 exit = hit;
        for (int i = 0; i < 40 && BlockPos.containing(exit).equals(pos); i++) {
            exit = exit.add(dir.scale(0.05));
        }
        damage *= damageMul;
        Vec3 v = getDeltaMovement().scale(0.85);
        setDeltaMovement(v);
        hasImpulse = true;
        setPos(exit.subtract(v)); // ThrowableProjectile ajoute ensuite la vitesse : on arrive juste derrière le bloc
        if (level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(pos)),
                    exit.x, exit.y, exit.z, 5, 0.05, 0.05, 0.05, 0.15);
        }
    }

    /** Éclats, fumée et bruit à l'impact, et un trou de balle sur la face touchée. */
    private void impact(ServerLevel level, BlockState state, BlockPos pos, Vec3 hit) {
        if (lastFace != null && state.isSolidRender(level, pos)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level, null, hit.x, hit.y, hit.z, 96,
                    new fr.modeminecraft.network.ImpactPayload(hit, lastFace));
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), hit.x, hit.y, hit.z, 6, 0.05, 0.05, 0.05, 0.1);
        level.sendParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, 2, 0.02, 0.02, 0.02, 0.01);
        level.playSound(null, pos, state.getSoundType(level, pos, this).getHitSound(), SoundSource.BLOCKS, 0.5f, 1.4f);
    }

    /** Une balle ne vit que quelques secondes : inutile de l'enregistrer avec le monde. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Pierce", pierceLeft);
        tag.putBoolean("ArmorPiercing", armorPiercing);
        tag.putFloat("Penetration", penetration);
        tag.putInt("AmmoType", entityData.get(TYPE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        pierceLeft = tag.getInt("Pierce");
        armorPiercing = tag.getBoolean("ArmorPiercing");
        penetration = tag.getFloat("Penetration");
        entityData.set(TYPE, tag.getInt("AmmoType"));
    }
}


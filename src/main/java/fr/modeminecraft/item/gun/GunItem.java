package fr.modeminecraft.item.gun;

import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.item.AmmoItem;
import fr.modeminecraft.entity.RocketEntity;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Arme à feu. Toutes les armes (pistolet, AK-47, sniper, RPG-7...) utilisent cette classe
 * avec des statistiques différentes (voir {@link GunStats}).
 *
 * Données stockées dans l'arme (ItemStack) :
 * - munitions dans le chargeur (AMMO) ;
 * - accessoires montés (ATTACHMENTS) -> modifient les statistiques, voir {@link ResolvedGun} ;
 * - skin (SKIN) -> purement visuel ;
 * - balle dans la chambre (CHAMBERED) : la plupart des armes tirent la balle chambrée, puis la suivante
 *   monte du chargeur. Chambre vide (arme neuve, chargeur vidé) = il faut tirer la culasse ;
 * - instants des animations (RACK_AT, CHECK_AT, RELOAD_START, RELOAD_END).
 *
 * Commandes : clic droit = tirer, B = mode de tir, R = recharger, T = type de munition, H = vérifier la chambre,
 * accroupi = viser.
 */
public class GunItem extends Item {
    protected final GunStats stats;

    public GunItem(GunStats stats, Properties properties) {
        super(properties.stacksTo(1));
        this.stats = stats;
    }

    public GunStats stats() {
        return stats;
    }

    // ------------------------------------------------------------------
    // Données de l'arme
    // ------------------------------------------------------------------
    public static int getAmmo(ItemStack stack) {
        // Une arme jetable (AT4) est vendue chargée
        int empty = stack.getItem() instanceof GunItem gun && gun.stats.disposable() ? gun.stats.magazineSize() : 0;
        return stack.getOrDefault(ModDataComponents.AMMO.get(), empty);
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        stack.set(ModDataComponents.AMMO.get(), ammo);
    }

    public static boolean isChambered(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CHAMBERED.get(), false);
    }

    public static void setChambered(ItemStack stack, boolean chambered) {
        stack.set(ModDataComponents.CHAMBERED.get(), chambered);
    }

    /** Balles disponibles : chargeur + chambre. */
    public int totalRounds(ItemStack stack) {
        return getAmmo(stack) + (stats.chamber() && isChambered(stack) ? 1 : 0);
    }

    /** Le joueur peut-il tirer tout de suite ? */
    public boolean canFire(Player player, ItemStack stack) {
        if (isOverheated(stack, player.level())) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        return stats.chamber() ? isChambered(stack) : getAmmo(stack) > 0;
    }

    /** Durée (ticks) pour tirer la culasse selon le mécanisme de l'arme. */
    public int rackTicks() {
        return switch (stats.casing()) {
            case SHELL -> 10;          // pompe
            default -> stats.reloadPerRound() ? 13 : 8;  // verrou / culasse
        };
    }

    /** Type des balles chargées dans l'arme (traçantes, perforantes...). */
    public static AmmoType getLoadedType(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AMMO_TYPE.get(), AmmoType.STANDARD);
    }

    public static void setLoadedType(ItemStack stack, AmmoType type) {
        if (type == AmmoType.STANDARD) {
            stack.remove(ModDataComponents.AMMO_TYPE.get());
        } else {
            stack.set(ModDataComponents.AMMO_TYPE.get(), type);
        }
    }

    private boolean isAmmo(ItemStack s, AmmoType type) {
        return s.is(stats.ammo().get()) && AmmoItem.getType(s) == type;
    }

    /** Rend des balles au joueur (en gardant leur type). */
    public void giveBack(Player player, int count, AmmoType type) {
        if (count > 0) {
            player.getInventory().placeItemBackInInventory(AmmoItem.of(stats.ammo().get(), type, count));
        }
    }

    /**
     * Touche T : passer au type de munition suivant parmi ceux que le joueur possède.
     * Les balles chargées (chargeur + chambre) sont rendues, puis l'arme est rechargée avec le nouveau type.
     */
    public void cycleAmmoType(Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this) || stats.disposable()) {
            return;
        }
        AmmoType current = getLoadedType(stack);
        List<AmmoType> available = new java.util.ArrayList<>();
        for (AmmoType type : AmmoType.values()) {
            if (type != current && countReserve(player, type) > 0) {
                available.add(type);
            }
        }
        if (available.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.ammo_type.none")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        // Le type suivant dans l'ordre de la liste (on boucle)
        AmmoType next = available.stream().filter(t -> t.ordinal() > current.ordinal()).findFirst().orElse(available.get(0));
        giveBack(player, getAmmo(stack) + (stats.chamber() && isChambered(stack) ? 1 : 0), current);
        setAmmo(stack, 0);
        setChambered(stack, false);
        setLoadedType(stack, next);
        player.displayClientMessage(Component.translatable("message.modeminecraft.ammo_type",
                Component.translatable(next.translationKey())).withStyle(ChatFormatting.YELLOW), true);
        reload(player, stack);
    }

    public static GunAttachments getAttachments(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ATTACHMENTS.get(), GunAttachments.EMPTY);
    }

    public static void setAttachments(ItemStack stack, GunAttachments attachments) {
        if (attachments.isEmpty()) {
            stack.remove(ModDataComponents.ATTACHMENTS.get());
        } else {
            stack.set(ModDataComponents.ATTACHMENTS.get(), attachments);
        }
    }

    public static GunSkin getSkin(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SKIN.get(), GunSkin.DEFAULT);
    }

    public static void setSkin(ItemStack stack, GunSkin skin) {
        if (skin == GunSkin.DEFAULT) {
            stack.remove(ModDataComponents.SKIN.get());
        } else {
            stack.set(ModDataComponents.SKIN.get(), skin);
        }
    }

    /** Mode de tir actuel de l'arme. */
    public FireMode getFireMode(ItemStack stack) {
        FireMode mode = stack.get(ModDataComponents.FIRE_MODE.get());
        return mode != null && stats.fireModes().contains(mode) ? mode : stats.defaultMode();
    }

    /** Touche B : passe au mode de tir suivant (coup par coup -> rafale -> automatique). */
    public void cycleFireMode(Player player, ItemStack stack) {
        List<FireMode> modes = stats.fireModes();
        if (modes.size() <= 1) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.fire_mode.single",
                    Component.translatable(getFireMode(stack).translationKey())).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        FireMode next = modes.get((modes.indexOf(getFireMode(stack)) + 1) % modes.size());
        stack.set(ModDataComponents.FIRE_MODE.get(), next);
        player.stopUsingItem();
        Component name = Component.translatable(next.translationKey());
        if (next == FireMode.BURST) {
            name = Component.translatable("message.modeminecraft.fire_mode.burst_count", name, stats.burstSize());
        }
        player.displayClientMessage(Component.translatable("message.modeminecraft.fire_mode", name)
                .withStyle(ChatFormatting.YELLOW), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    /** Statistiques finales (accessoires compris). */
    public ResolvedGun resolve(ItemStack stack) {
        return ResolvedGun.of(stats, getAttachments(stack));
    }

    /** Cet accessoire peut-il être monté sur cette arme ? */
    public boolean accepts(AttachmentItem attachment) {
        if (!stats.slots().contains(attachment.slot())) {
            return false;
        }
        if (!attachment.fits(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath(), stats)) {
            return false;
        }
        // Un chargeur doit contenir plus de balles que celui d'origine
        return attachment.slot() != AttachmentSlot.MAGAZINE
                || attachment.stats().magazineSize() > stats.magazineSize();
    }

    /** Munitions de ce calibre et de ce type dans l'inventaire du joueur (réserve). */
    public int countReserve(Player player, AmmoType type) {
        Inventory inv = player.getInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isAmmo(s, type)) {
                total += s.getCount();
            }
        }
        return total;
    }

    /** Réserve du type actuellement chargé dans l'arme. */
    public int countReserve(Player player, ItemStack gun) {
        return countReserve(player, getLoadedType(gun));
    }

    // ------------------------------------------------------------------
    // Tir
    // ------------------------------------------------------------------
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!canFire(player, stack)) {
            if (!level.isClientSide) {
                if (isOverheated(stack, level)) {
                    overheatMessage(player);
                } else {
                    rackOrReload(player, stack);
                }
            }
            return InteractionResultHolder.fail(stack);
        }

        // On "utilise" l'arme tant que le clic est maintenu : comme une vraie détente,
        // il faut relâcher le clic pour tirer à nouveau en coup par coup ou en rafale.
        player.startUsingItem(hand);
        if (stats.spinUp() == 0) {
            // La première balle part tout de suite (les suivantes dans onUseTick)
            shoot(level, player, stack);
            if (getFireMode(stack) == FireMode.SEMI) {
                player.getCooldowns().addCooldown(this, Math.max(stats.fireDelay(), 3));
            }
        }
        // "consume" = pas d'animation de coup de poing
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player) || player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        int ticksUsed = getUseDuration(stack, entity) - remainingUseDuration;
        if (stats.spinUp() > 0) {
            // Minigun : les canons tournent d'abord sans tirer
            if (ticksUsed < stats.spinUp()) {
                if (ticksUsed % 4 == 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.MINECART_RIDING,
                            SoundSource.PLAYERS, 0.4f, 0.8f + 1.2f * ticksUsed / stats.spinUp());
                }
                return;
            }
            ticksUsed -= stats.spinUp();
        }
        if (ticksUsed % stats.fireDelay() != 0) {
            return;
        }
        FireMode mode = getFireMode(stack);
        int shotIndex = ticksUsed / stats.fireDelay();
        if (mode == FireMode.SEMI || (shotIndex == 0 && stats.spinUp() == 0)) {
            return; // coup par coup : une seule balle par appui ; la 1re balle est tirée dans use()
        }
        if (mode == FireMode.BURST && shotIndex >= stats.burstSize()) {
            if (shotIndex == stats.burstSize()) {
                player.getCooldowns().addCooldown(this, stats.burstPause()); // pause après la rafale
            }
            return;
        }
        if (!canFire(player, stack)) {
            player.stopUsingItem();
            if (!level.isClientSide) {
                if (isOverheated(stack, level)) {
                    overheatMessage(player);
                } else {
                    rackOrReload(player, stack);
                }
            }
            return;
        }
        shoot(level, player, stack);
    }

    /** Plus de balle chambrée : s'il en reste dans le chargeur on tire la culasse, sinon on recharge. */
    private void rackOrReload(Player player, ItemStack stack) {
        if (stats.chamber() && getAmmo(stack) > 0) {
            rack(player, stack);
        } else {
            reload(player, stack);
        }
    }

    /** Tirer la culasse : une balle passe du chargeur à la chambre (animation visible). */
    public void rack(Player player, ItemStack stack) {
        if (!stats.chamber() || isChambered(stack) || getAmmo(stack) <= 0 || player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        setAmmo(stack, getAmmo(stack) - 1);
        setChambered(stack, true);
        stack.set(ModDataComponents.RACK_AT.get(), player.level().getGameTime());
        player.getCooldowns().addCooldown(this, rackTicks());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    /** Touche H : on entrouvre la culasse pour voir s'il y a une balle dans la chambre. */
    public void checkChamber(Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        Component state;
        if (!stats.chamber()) {
            state = Component.translatable("message.modeminecraft.chamber.none", getAmmo(stack), resolve(stack).magazineSize());
        } else if (isChambered(stack) || player.getAbilities().instabuild) {
            state = Component.translatable("message.modeminecraft.chamber.loaded", getAmmo(stack), resolve(stack).magazineSize())
                    .withStyle(ChatFormatting.GREEN);
        } else {
            state = Component.translatable("message.modeminecraft.chamber.empty", getAmmo(stack), resolve(stack).magazineSize())
                    .withStyle(ChatFormatting.RED);
        }
        player.displayClientMessage(state, true);
        stack.set(ModDataComponents.CHECK_AT.get(), player.level().getGameTime());
        player.getCooldowns().addCooldown(this, 12);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.5f, 1.5f);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** Le joueur se déplace-t-il ? (la précision baisse en marchant) */
    public static boolean isMoving(Player player) {
        if (player.level().isClientSide) {
            return player.getDeltaMovement().horizontalDistanceSqr() > 0.002;
        }
        double dx = player.getX() - player.xo, dz = player.getZ() - player.zo;
        return dx * dx + dz * dz > 0.002;
    }

    /** Stable = allongé, ou en train de viser sans bouger (bipieds, monopode...). */
    public static boolean isSteady(Player player) {
        return ProneHandler.isProne(player) || isAiming(player) && !isMoving(player);
    }

    /** Le joueur vise-t-il ? (touche accroupir maintenue, debout ou allongé) */
    public static boolean isAiming(Player player) {
        return player.isShiftKeyDown();
    }

    protected void shoot(Level level, Player player, ItemStack stack) {
        ResolvedGun gun = resolve(stack);
        AmmoType type = getLoadedType(stack);
        boolean shells = stats.ammo().get() instanceof AmmoItem a && a.isShotgunShell();
        boolean silenced = gun.silenced() || type == AmmoType.SUBSONIC;
        boolean aiming = isAiming(player);
        // Allongé : l'arme est bien plus stable ; stable (bipied, monopode...) : bonus des accessoires
        float stability = ProneHandler.isProne(player) ? 0.5f : 1.0f;
        boolean steady = isSteady(player);
        float steadyRecoil = steady ? gun.steadyRecoil() : 1, steadySpread = steady ? gun.steadySpread() : 1;

        if (level.isClientSide) {
            GunAnimations.recordShot(player); // la culasse recule (animation)
            ejectCasing(level, player, null);
            // Recul : la caméra remonte un peu (uniquement côté client, le serveur suit)
            float recoil = (aiming ? gun.recoil() * 0.6f : gun.recoil()) * stability * type.recoilMul * steadyRecoil;
            player.setXRot(Mth.clamp(player.getXRot() - recoil, -90f, 90f));
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * recoil * 0.5f);
            return;
        }

        float heat = heat(stack, level);
        float spread = (aiming ? gun.aimSpread() : gun.spread()) * stability * type.spreadMul * steadySpread
                * (isMoving(player) ? gun.movingPenalty() : 1)
                * (1 + Math.max(0, heat - 60) / 40f); // canon trop chaud : moins précis
        Vec3 lean = LeanHandler.offset(player);
        int pellets = shells ? type.pellets(stats.pellets()) : stats.pellets();
        float damage = shells ? type.pelletDamage(gun.damage(), stats.pellets()) : gun.damage() * type.damageMul;
        for (int i = 0; i < pellets; i++) {
            Projectile projectile = createProjectile(level, player, gun, type, damage);
            if (lean != Vec3.ZERO) {
                projectile.setPos(projectile.position().add(lean)); // penché : la balle part de plus loin sur le côté
            }
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f,
                    stats.velocity() * type.velocityMul, spread);
            level.addFreshEntity(projectile);
        }

        if (!player.getAbilities().instabuild) {
            if (stats.chamber()) {
                // La balle chambrée part ; la suivante monte du chargeur (s'il en reste)
                if (getAmmo(stack) > 0) {
                    setAmmo(stack, getAmmo(stack) - 1);
                } else {
                    setChambered(stack, false);
                }
            } else {
                setAmmo(stack, getAmmo(stack) - 1);
            }
            if (stats.disposable() && getAmmo(stack) <= 0) {
                // AT4 : le tube vide est jeté
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);
                stack.shrink(1);
            }
        }

        float pitch = stats.pitch() + (silenced ? 0.5f : 0f) + (level.random.nextFloat() - 0.5f) * 0.1f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                stats.sound().get(), SoundSource.PLAYERS, gun.volume() * type.volumeMul, pitch);

        // Flash et fumée au bout du canon (pas de flash avec un silencieux)
        if (level instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle();
            Vec3 muzzle = player.getEyePosition().add(look.scale(1.2)).add(0, -0.15, 0);
            serverLevel.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.03, 0.03, 0.03, 0.01);
            if (!silenced) {
                serverLevel.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 1, 0, 0, 0, 0);
            }
        }

        // Douille éjectée, visible par les autres joueurs (le tireur l'a déjà vue côté client)
        if (level instanceof ServerLevel serverLevel) {
            ejectCasing(serverLevel, player, serverLevel);
        }

        wearMuzzle(player, stack);
        addHeat(player, stack, heat);
        alertMonsters(player, gun.volume() * type.volumeMul);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!silenced) {
            // Un tir silencieux n'est pas détecté par les capteurs sculk
            level.gameEvent(player, GameEvent.PROJECTILE_SHOOT, player.position());
        }
    }

    /**
     * Éjecte une douille par la fenêtre d'éjection (à droite de l'arme), vers la droite et le haut.
     * Côté client : particule pour le tireur. Côté serveur : envoyée aux autres joueurs proches.
     */
    private void ejectCasing(Level level, Player player, ServerLevel server) {
        var type = switch (stats.casing()) {
            case NONE -> null;
            case BRASS -> ModParticles.CASING_BRASS.get();
            case STEEL -> ModParticles.CASING_STEEL.get();
            case SHELL -> ModParticles.CASING_SHELL.get();
        };
        if (type == null) {
            return;
        }
        float yaw = (float) Math.toRadians(player.getYRot());
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 pos = player.getEyePosition().add(look.scale(0.55)).add(right.scale(0.28)).add(0, -0.18, 0);
        Vec3 velocity = right.scale(0.12 + level.random.nextFloat() * 0.05).add(0, 0.12 + level.random.nextFloat() * 0.05, 0)
                .add(look.scale(-0.02)).add(player.getDeltaMovement());
        if (server == null) {
            level.addParticle(type, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        } else {
            for (var other : server.players()) {
                if (other != player && other.distanceToSqr(player) < 48 * 48) {
                    server.sendParticles(other, type, false, pos.x, pos.y, pos.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
                }
            }
        }
    }

    protected Projectile createProjectile(Level level, Player player, ResolvedGun gun, AmmoType type, float damage) {
        return switch (stats.projectile()) {
            case ROCKET -> new RocketEntity(ModEntities.ROCKET.get(), level, player, stats.explosionPower());
            case GRENADE -> new RocketEntity(ModEntities.GRENADE.get(), level, player, stats.explosionPower());
            case BULLET -> new BulletEntity(level, player, damage,
                    stats.pierce() + (type == AmmoType.ARMOR_PIERCING ? 1 : 0),
                    stats.armorPiercing() || type == AmmoType.ARMOR_PIERCING, type);
        };
    }

    // ------------------------------------------------------------------
    // Tir secondaire (touche V) : lance-grenades M203 ou fusil à pompe M26 sous le canon
    // ------------------------------------------------------------------
    public static boolean isUnderbarrelLoaded(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.UNDERBARREL_LOADED.get(), false);
    }

    // ------------------------------------------------------------------
    // Surchauffe du canon (mitrailleuses)
    // ------------------------------------------------------------------
    private static final float COOLING_PER_TICK = 0.6f;
    private static final float RESUME_BELOW = 35f;
    private static final int SPARE_BARREL_COOLDOWN = 400;

    /** Chaleur actuelle du canon (0 à 100) : elle redescend avec le temps. */
    public static float heat(ItemStack stack, Level level) {
        Float stored = stack.get(ModDataComponents.HEAT.get());
        if (stored == null) {
            return 0;
        }
        long since = level.getGameTime() - stack.getOrDefault(ModDataComponents.HEAT_TIME.get(), 0L);
        return Math.max(0, stored - since * COOLING_PER_TICK);
    }

    public static boolean isOverheated(ItemStack stack, Level level) {
        return stack.getOrDefault(ModDataComponents.OVERHEATED.get(), false) && heat(stack, level) >= RESUME_BELOW;
    }

    private void addHeat(Player player, ItemStack stack, float current) {
        if (stats.heatPerShot() <= 0 || stack.isEmpty()) {
            return;
        }
        // heatPerShot = chaleur NETTE par tir : on compense le refroidissement entre deux tirs
        float heat = Math.min(100, current + stats.heatPerShot() + COOLING_PER_TICK * stats.fireDelay());
        stack.set(ModDataComponents.HEAT.get(), heat);
        stack.set(ModDataComponents.HEAT_TIME.get(), player.level().getGameTime());
        if (heat >= 100) {
            stack.set(ModDataComponents.OVERHEATED.get(), true);
            player.stopUsingItem();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 0.8f);
            overheatMessage(player);
        } else if (stack.has(ModDataComponents.OVERHEATED.get())) {
            stack.remove(ModDataComponents.OVERHEATED.get());
        }
    }

    private static void overheatMessage(Player player) {
        player.displayClientMessage(Component.translatable("message.modeminecraft.overheated").withStyle(ChatFormatting.RED), true);
    }

    /** Touche R sur une mitrailleuse brûlante : on remplace le canon par le canon de rechange. */
    private boolean swapBarrel(Player player, ItemStack stack) {
        Level level = player.level();
        if (!player.getAbilities().instabuild && !player.getInventory().hasAnyMatching(s -> s.is(ModItems.SPARE_BARREL.get()))) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.no_spare_barrel").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        long since = level.getGameTime() - stack.getOrDefault(ModDataComponents.BARREL_SWAP_AT.get(), -SPARE_BARREL_COOLDOWN * 2L);
        if (since < SPARE_BARREL_COOLDOWN) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.barrel_cooling",
                    (SPARE_BARREL_COOLDOWN - since) / 20 + 1).withStyle(ChatFormatting.GOLD), true);
            return true;
        }
        stack.remove(ModDataComponents.HEAT.get());
        stack.remove(ModDataComponents.OVERHEATED.get());
        stack.set(ModDataComponents.BARREL_SWAP_AT.get(), level.getGameTime());
        player.getCooldowns().addCooldown(this, 50);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
        player.displayClientMessage(Component.translatable("message.modeminecraft.barrel_swapped").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    // ------------------------------------------------------------------
    // Le bruit des tirs attire les monstres
    // ------------------------------------------------------------------
    private static void alertMonsters(Player player, float loudness) {
        double radius = Math.min(64, loudness * 48);
        if (radius < 3 || player.isSpectator()) {
            return;
        }
        for (var monster : player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                player.getBoundingBox().inflate(radius),
                m -> m.getTarget() == null && !(m instanceof net.minecraft.world.entity.NeutralMob)
                        && m.distanceToSqr(player) < radius * radius)) {
            if (player.isCreative()) {
                monster.getNavigation().moveTo(player, 1.0); // en créatif ils viennent voir, sans attaquer
            } else {
                monster.setTarget(player);
            }
        }
    }

    /** Silencieux artisanal : il s'use à chaque tir et finit par casser. */
    private void wearMuzzle(Player player, ItemStack stack) {
        if (!(getAttachments(stack).get(AttachmentSlot.MUZZLE).orElse(null) instanceof AttachmentItem muzzle)
                || muzzle.stats().wearShots() <= 0 || stack.isEmpty()) {
            return;
        }
        int wear = stack.getOrDefault(ModDataComponents.MUZZLE_WEAR.get(), 0) + 1;
        if (wear < muzzle.stats().wearShots()) {
            stack.set(ModDataComponents.MUZZLE_WEAR.get(), wear);
            return;
        }
        stack.remove(ModDataComponents.MUZZLE_WEAR.get());
        setAttachments(stack, getAttachments(stack).with(AttachmentSlot.MUZZLE, null));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
        player.displayClientMessage(Component.translatable("message.modeminecraft.muzzle_broken", muzzle.getDescription())
                .withStyle(ChatFormatting.RED), true);
    }

    /** Laser allumé ? (allumé par défaut quand on le monte) */
    public static boolean isLaserOn(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LASER_ON.get(), true);
    }

    /** Couleur du laser monté (0xRRGGBB, -1 = arc-en-ciel), ou null si l'arme n'a pas de laser allumé. */
    public static Integer activeLaserColor(ItemStack stack) {
        if (!isLaserOn(stack)) {
            return null;
        }
        for (Item item : getAttachments(stack).parts().values()) {
            if (item instanceof AttachmentItem a && a.stats().specials().contains(AttachmentItem.Special.LASER)) {
                return a.stats().laserColor();
            }
        }
        return null;
    }

    /** Touche T : allumer / éteindre le laser. */
    public void toggleLaser(Player player, ItemStack stack) {
        if (!resolve(stack).has(AttachmentItem.Special.LASER)) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.laser.none")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean on = !isLaserOn(stack);
        stack.set(ModDataComponents.LASER_ON.get(), on);
        player.displayClientMessage(Component.translatable(on ? "message.modeminecraft.laser.on" : "message.modeminecraft.laser.off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 0.4f, on ? 1.8f : 1.4f);
    }

    public void secondaryFire(Player player, ItemStack stack) {
        ResolvedGun gun = resolve(stack);
        boolean grenade = gun.has(AttachmentItem.Special.UNDERBARREL_GRENADE);
        if (!grenade && !gun.has(AttachmentItem.Special.UNDERBARREL_SHOTGUN)) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.underbarrel.none")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        Level level = player.level();
        Item ammo = grenade ? ModItems.GRENADE_40MM.get() : ModItems.SHOTGUN_SHELL.get();
        if (!isUnderbarrelLoaded(stack)) {
            // On charge une grenade / une cartouche
            if (!player.getAbilities().instabuild) {
                if (countPlain(player, ammo) == 0) {
                    player.displayClientMessage(Component.translatable("message.modeminecraft.no_ammo",
                            ammo.getDescription()).withStyle(ChatFormatting.RED), true);
                    return;
                }
                removePlain(player, ammo);
            }
            stack.set(ModDataComponents.UNDERBARREL_LOADED.get(), true);
            player.getCooldowns().addCooldown(this, grenade ? 30 : 20);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0f, 0.6f);
            player.displayClientMessage(Component.translatable(grenade ? "message.modeminecraft.underbarrel.loaded_grenade"
                    : "message.modeminecraft.underbarrel.loaded_shell").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        stack.set(ModDataComponents.UNDERBARREL_LOADED.get(), false);
        if (grenade) {
            var g = new RocketEntity(ModEntities.GRENADE.get(), level, player, 2.2f);
            g.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.6f, 1.0f);
            level.addFreshEntity(g);
        } else {
            for (int i = 0; i < 8; i++) {
                var b = new BulletEntity(level, player, 3.5f, 0, false, AmmoType.STANDARD);
                b.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3.0f, 8.0f);
                level.addFreshEntity(b);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                grenade ? SoundEvents.FIREWORK_ROCKET_BLAST : SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, grenade ? 1.0f : 0.7f, grenade ? 0.6f : 1.2f);
        player.getCooldowns().addCooldown(this, 10);
    }

    private static int countPlain(Player player, Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(item) && !s.has(ModDataComponents.AMMO_TYPE.get())) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void removePlain(Player player, Item item) {
        for (ItemStack s : player.getInventory().items) {
            if (s.is(item) && !s.has(ModDataComponents.AMMO_TYPE.get())) {
                s.shrink(1);
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // Rechargement (appelé côté serveur : touche R ou chargeur vide)
    // ------------------------------------------------------------------
    public void reload(Player player, ItemStack stack) {
        if (stats.heatPerShot() > 0 && heat(stack, player.level()) > 50 && !player.getCooldowns().isOnCooldown(this)
                && swapBarrel(player, stack)) {
            return;
        }
        ResolvedGun gun = resolve(stack);
        int missing = gun.magazineSize() - getAmmo(stack);
        if (stats.disposable() || player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        if (missing <= 0) {
            rack(player, stack); // chargeur plein mais chambre vide : on arme
            return;
        }

        // Arme vide et plus de balles du type chargé : on prend n'importe quel type disponible
        if (!player.getAbilities().instabuild && getAmmo(stack) == 0 && !isChambered(stack)
                && countReserve(player, getLoadedType(stack)) == 0) {
            for (AmmoType type : AmmoType.values()) {
                if (countReserve(player, type) > 0) {
                    setLoadedType(stack, type);
                    break;
                }
            }
        }
        int taken = player.getAbilities().instabuild ? missing : takeAmmo(player, missing, getLoadedType(stack));
        if (taken == 0) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.no_ammo",
                    stats.ammo().get().getDescription()).withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.6f);
            return;
        }

        setAmmo(stack, getAmmo(stack) + taken);
        // Pas de stopUsingItem() ici : il ne s'exécuterait que côté serveur et désynchroniserait le client.
        // Le cooldown (synchronisé) suffit à suspendre le tir automatique pendant le rechargement.
        // Rechargement balle par balle : la durée dépend du nombre de balles insérées
        int time = stats.reloadPerRound() ? gun.reloadTicks() * taken : gun.reloadTicks();
        long now = player.level().getGameTime();
        if (stats.chamber() && !isChambered(stack)) {
            // Rechargement à vide : après avoir mis le chargeur, le personnage tire la culasse
            setAmmo(stack, getAmmo(stack) - 1);
            setChambered(stack, true);
            stack.set(ModDataComponents.RACK_AT.get(), now + time);
            time += rackTicks();
        }
        stack.set(ModDataComponents.RELOAD_START.get(), now);
        stack.set(ModDataComponents.RELOAD_END.get(), now + time);
        player.getCooldowns().addCooldown(this, time);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    private int takeAmmo(Player player, int wanted, AmmoType type) {
        Inventory inv = player.getInventory();
        int taken = 0;
        for (int i = 0; i < inv.getContainerSize() && taken < wanted; i++) {
            ItemStack s = inv.getItem(i);
            if (isAmmo(s, type)) {
                int n = Math.min(s.getCount(), wanted - taken);
                s.shrink(n);
                taken += n;
            }
        }
        return taken;
    }

    // ------------------------------------------------------------------
    // Affichage
    // ------------------------------------------------------------------
    /** La barre sous l'icône montre le remplissage du chargeur. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(13, Math.round(13.0f * totalRounds(stack) / resolve(stack).magazineSize()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return getAmmo(stack) <= resolve(stack).magazineSize() / 4 ? 0xFF5555 : 0xFFD84A;
    }

    /** Évite que l'arme "se rééquipe" (animation) à chaque balle tirée. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    /** Pas de destruction de blocs au clic gauche en créatif (comme les épées). */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResolvedGun gun = resolve(stack);
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.ammo", getAmmo(stack), gun.magazineSize())
                .withStyle(ChatFormatting.GOLD));
        if (stats.chamber()) {
            tooltip.add(Component.translatable(isChambered(stack) ? "tooltip.modeminecraft.gun.chambered"
                    : "tooltip.modeminecraft.gun.not_chambered").withStyle(isChambered(stack) ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        String damage = stats.projectile() != GunStats.ProjectileKind.BULLET
                ? Component.translatable("tooltip.modeminecraft.gun.explosive").getString()
                : (stats.pellets() > 1 ? stats.pellets() + " x " : "") + format(gun.damage());
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.damage", damage).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.ammo_type", stats.ammo().get().getDescription())
                .withStyle(ChatFormatting.GRAY));
        if (getLoadedType(stack) != AmmoType.STANDARD) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.gun.loaded_type",
                    Component.translatable(getLoadedType(stack).translationKey())).withStyle(ChatFormatting.BLUE));
        }

        GunAttachments attachments = getAttachments(stack);
        for (AttachmentSlot slot : AttachmentSlot.values()) {
            attachments.get(slot).ifPresent(item -> tooltip.add(Component.literal(" + ")
                    .append(item.getDescription()).withStyle(ChatFormatting.AQUA)));
        }
        GunSkin skin = getSkin(stack);
        if (skin != GunSkin.DEFAULT) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.gun.skin", Component.translatable(skin.translationKey()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.fire_mode",
                Component.translatable(getFireMode(stack).translationKey())).withStyle(ChatFormatting.YELLOW));
        if (stats.pierce() > 0) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.gun.pierce", stats.pierce()).withStyle(ChatFormatting.BLUE));
        }
        if (stats.armorPiercing()) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.gun.armor_piercing").withStyle(ChatFormatting.BLUE));
        }
        if (stats.disposable()) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.gun.disposable").withStyle(ChatFormatting.BLUE));
        }
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.controls").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String format(float value) {
        return value == (int) value ? Integer.toString((int) value) : String.format("%.1f", value);
    }
}

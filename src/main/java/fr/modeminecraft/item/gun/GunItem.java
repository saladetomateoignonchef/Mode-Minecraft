package fr.modeminecraft.item.gun;

import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.item.AmmoItem;
import fr.modeminecraft.entity.RocketEntity;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.item.gun.LightMode;
import fr.modeminecraft.registry.ModSounds;
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
        if (player.getAbilities().instabuild || stats.special() == GunStats.Special.NO_AMMO) {
            return true;
        }
        return stats.chamber() ? isChambered(stack) : getAmmo(stack) > 0;
    }

    /** Part de l'armure ignorée selon le calibre (9 mm faible, 7,62 et .50 BMG de 40 à 70 %). */
    /** Part de l'armure ignorée par une balle de ce calibre (livre des recettes : fiche technique). */
    public static float armorPenetration(String caliber) {
        return ARMOR_PEN.getOrDefault(caliber, 0.1f);
    }

    private static final java.util.Map<String, Float> ARMOR_PEN = java.util.Map.ofEntries(
            java.util.Map.entry("ammo_9mm", 0.1f), java.util.Map.entry("ammo_45acp", 0.1f), java.util.Map.entry("ammo_357", 0.2f),
            java.util.Map.entry("ammo_50ae", 0.25f), java.util.Map.entry("ammo_57", 0.5f), java.util.Map.entry("ammo_46", 0.5f),
            java.util.Map.entry("ammo_762x25", 0.2f), java.util.Map.entry("ammo_545", 0.3f), java.util.Map.entry("ammo_556", 0.3f),
            java.util.Map.entry("ammo_762", 0.45f), java.util.Map.entry("ammo_762x54r", 0.5f), java.util.Map.entry("ammo_792", 0.5f),
            java.util.Map.entry("ammo_338", 0.6f), java.util.Map.entry("ammo_50bmg", 0.7f), java.util.Map.entry("shotgun_shell", 0.0f));
    /** Gros calibres : la balle traverse un ennemi pour toucher celui de derrière. */
    private static final java.util.Set<String> BIG_CALIBERS = java.util.Set.of("ammo_762", "ammo_762x54r", "ammo_792", "ammo_338", "ammo_50bmg");

    /** Fusil à verrou (on manœuvre le verrou après chaque tir). */
    public boolean isBoltAction() {
        return stats.category() == GunStats.Category.SNIPER && stats.fireDelay() >= 24;
    }

    /** Bruit de la culasse / du verrou / de la pompe. */
    public net.minecraft.sounds.SoundEvent actionSound() {
        if (stats.casing() == GunStats.Casing.SHELL) {
            return ModSounds.PUMP.get();
        }
        return isBoltAction() ? ModSounds.BOLT.get() : ModSounds.RACK.get();
    }

    /**
     * Son du tir : entendu de loin (portée = 16 blocs x volume), silencieux si besoin,
     * et un grondement lointain pour les joueurs encore plus éloignés.
     */
    private void playShotSound(Level level, Player player, ResolvedGun gun, AmmoType type, boolean silenced) {
        float loudness = gun.volume() * type.volumeMul;
        if (loudness <= 0.001f) {
            return; // silencieux à écho : aucun bruit
        }
        float pitch = stats.pitch() + (level.random.nextFloat() - 0.5f) * 0.06f;
        net.minecraft.sounds.SoundEvent sound = stats.sound().get();
        if (silenced && stats.projectile() == GunStats.ProjectileKind.BULLET) {
            boolean light = stats.pistol() || stats.category() == GunStats.Category.SMG;
            sound = (light ? ModSounds.SHOT_SUPPRESSED : ModSounds.SHOT_SUPPRESSED_HEAVY).get();
        }
        float volume = loudness * 8f; // au-delà de 1, Minecraft augmente la portée (16 blocs par point)
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
        if (silenced || !(level instanceof ServerLevel server)) {
            return;
        }
        double range = 16 * volume, far = Math.min(320, range * 4);
        var holder = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SHOT_DISTANT.get());
        for (var listener : server.players()) {
            double d = listener.distanceTo(player);
            if (d > range && d < far) {
                // Son placé à 6 blocs de l'auditeur, dans la direction du tireur : on sait d'où ça vient
                Vec3 dir = player.position().subtract(listener.position()).normalize();
                Vec3 at = listener.getEyePosition().add(dir.scale(6));
                listener.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(holder, SoundSource.PLAYERS,
                        at.x, at.y, at.z, (float) (0.25 + 0.6 * (1 - d / far)), 0.85f + level.random.nextFloat() * 0.2f,
                        level.random.nextLong()));
            }
        }
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
        ResolvedGun r = ResolvedGun.of(stats, getAttachments(stack)).withLevel(getProgress(stack).level());
        Legendary legendary = Legendary.of(stack);
        if (legendary != null) {
            r = legendary.apply(r);                          // arme unique d'un Chef de Faction
        }
        AttachmentItem.Light l = light(stack);
        if (l != null && activeLightMode(stack) != LightMode.OFF) {
            r = r.withLightBonus(l.onHipSpread(), l.onAimSpeed());
        }
        return r;
    }

    // ------------------------------------------------------------------
    // Lampe tactique (rail latéral) : touche L = allumer / éteindre, Maj + L = changer de mode
    // ------------------------------------------------------------------
    /** Autonomie d'une pile CR123A neuve, en secondes (10 minutes). */
    public static final int BATTERY_SECONDS = 600;

    /** La lampe montée sur l'arme, ou null. */
    public static AttachmentItem.Light light(ItemStack stack) {
        for (Item item : getAttachments(stack).parts().values()) {
            if (item instanceof AttachmentItem a && a.stats().light() != null) {
                return a.stats().light();
            }
        }
        return null;
    }

    /** L'accessoire lampe monté, ou null. */
    public static AttachmentItem lightItem(ItemStack stack) {
        for (Item item : getAttachments(stack).parts().values()) {
            if (item instanceof AttachmentItem a && a.stats().light() != null) {
                return a;
            }
        }
        return null;
    }

    public static LightMode lightMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LIGHT_MODE.get(), LightMode.OFF);
    }

    public static int lightBattery(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LIGHT_BATTERY.get(), BATTERY_SECONDS);
    }

    public static void setLightBattery(ItemStack stack, int seconds) {
        stack.set(ModDataComponents.LIGHT_BATTERY.get(), Math.max(0, Math.min(BATTERY_SECONDS, seconds)));
    }

    /** Mode réellement actif : éteinte si pas de lampe, pile vide, ou mode indisponible sur ce modèle. */
    public static LightMode activeLightMode(ItemStack stack) {
        LightMode mode = lightMode(stack);
        AttachmentItem.Light l = light(stack);
        if (mode == LightMode.OFF || l == null || lightBattery(stack) <= 0
                || mode == LightMode.STROBE && !l.strobe() || mode == LightMode.IR && !l.ir()) {
            return LightMode.OFF;
        }
        return mode;
    }

    /** Touche L (allumer / éteindre) ou Maj + L (mode suivant : continu -> stroboscope -> infrarouge -> éteint). */
    public void switchLight(Player player, ItemStack stack, boolean cycle) {
        AttachmentItem.Light l = light(stack);
        if (l == null) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.light.none").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        LightMode mode = lightMode(stack);
        LightMode next;
        if (cycle) {
            next = switch (mode) {
                case OFF -> LightMode.ON;
                case ON -> l.strobe() ? LightMode.STROBE : l.ir() ? LightMode.IR : LightMode.OFF;
                case STROBE -> l.ir() ? LightMode.IR : LightMode.OFF;
                case IR -> LightMode.OFF;
            };
        } else {
            next = mode == LightMode.OFF ? LightMode.ON : LightMode.OFF;
        }
        if (next != LightMode.OFF && lightBattery(stack) <= 0) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.light.empty").withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLASHLIGHT_CLICK.get(), SoundSource.PLAYERS, 0.5f, 0.7f);
            return;
        }
        stack.set(ModDataComponents.LIGHT_MODE.get(), next);
        stack.set(ModDataComponents.LIGHT_SWITCH_AT.get(), player.level().getGameTime());
        player.displayClientMessage(Component.translatable("message.modeminecraft.light." + next.getSerializedName(),
                lightBattery(stack) * 100 / BATTERY_SECONDS).withStyle(next == LightMode.OFF ? ChatFormatting.GRAY : ChatFormatting.YELLOW), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLASHLIGHT_CLICK.get(),
                SoundSource.PLAYERS, 0.6f, next == LightMode.OFF ? 0.85f : 1.05f);
    }

    public static GunProgress getProgress(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PROGRESS.get(), GunProgress.EMPTY);
    }

    /** Élimination avec cette arme : expérience, et message + son si elle monte de niveau. */
    public static void recordKill(ItemStack stack, Player player, boolean headshot, boolean big) {
        GunProgress before = getProgress(stack);
        GunProgress after = before.withKill(headshot, big);
        stack.set(ModDataComponents.PROGRESS.get(), after);
        if (after.level() > before.level()) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.gun.level_up", stack.getHoverName(), after.level())
                    .withStyle(ChatFormatting.GOLD), false);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.3f);
            for (GunSkin skin : GunSkin.values()) {
                if (skin.requiredLevel() == after.level()) {
                    player.displayClientMessage(Component.translatable("message.modeminecraft.gun.skin_unlocked",
                            Component.translatable(skin.translationKey())).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                }
            }
        }
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
        return attachment.slot() != AttachmentSlot.MAGAZINE || attachment.stats().magazineSize() == 0
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
    /** Le clic droit sert à viser (voir TriggerHandler) : il ne fait rien d'autre avec une arme en main. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    /**
     * Appelé à chaque tick tant que la détente (clic gauche) est maintenue, côté client et côté serveur.
     * - au premier tick : 1re balle (ou armement / rechargement si l'arme est vide) ;
     * - ensuite : tir continu à la cadence de l'arme (rafale limitée, coup par coup = une balle par appui) ;
     * - minigun : rotation des canons avant le premier tir.
     */
    public void triggerTick(Level level, Player player, ItemStack stack, TriggerHandler.Trigger trigger) {
        if (trigger.locked) {
            return;
        }
        if (trigger.held() == 0 && interruptReload(player, stack)) {
            trigger.lock();
            return;
        }
        FireMode mode = getFireMode(stack);
        if (stats.chargeTicks() > 0 && !player.getCooldowns().isOnCooldown(this)) {
            // Railgun : il faut garder la détente le temps de la charge, puis le rayon part tout seul
            if (trigger.held() == 0 && !level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                        SoundSource.PLAYERS, 1.0f, 1.6f);
            }
            if (trigger.held() < stats.chargeTicks()) {
                return;
            }
            if (canFire(player, stack)) {
                shoot(level, player, stack);
                player.getCooldowns().addCooldown(this, stats.fireDelay());
            }
            trigger.lock();
            return;
        }
        int t = trigger.held() - stats.spinUp();
        if (player.getCooldowns().isOnCooldown(this)) {
            if (trigger.held() == 0 || mode == FireMode.SEMI) {
                trigger.lock(); // appui pendant un délai (rechargement...) : il faudra réappuyer
            } else {
                trigger.shots = expectedShots(t); // pas de rattrapage après un délai
            }
            return;
        }
        if (stats.spinUp() > 0 && t < 0) {
            // Minigun : les canons tournent d'abord sans tirer
            if (trigger.held() % 4 == 0 && !level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.MINECART_RIDING,
                        SoundSource.PLAYERS, 0.4f, 0.8f + 1.2f * trigger.held() / stats.spinUp());
            }
            return;
        }
        int due = expectedShots(t) - trigger.shots;
        for (int i = 0; i < due; i++) {
            if (mode == FireMode.BURST && trigger.shots >= stats.burstSize()) {
                player.getCooldowns().addCooldown(this, stats.burstPause()); // pause après la rafale
                trigger.lock();
                return;
            }
            if (!canFire(player, stack)) {
                if (!level.isClientSide) {
                    if (isOverheated(stack, level)) {
                        overheatMessage(player);
                    } else if (trigger.shots == 0) {
                        rackOrReload(player, stack);
                    } else {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                ModSounds.EMPTY_CLICK.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
                    }
                }
                trigger.lock();
                return;
            }
            shoot(level, player, stack);
            trigger.shots++;
            if (mode == FireMode.SEMI) {
                boolean fanning = stats.special() == GunStats.Special.FANNING && TriggerHandler.isRightHeld(player);
                player.getCooldowns().addCooldown(this, fanning ? 3 : Math.max(stats.fireDelay(), 3));
                trigger.lock();
                return;
            }
        }
    }

    /** Fusil à deux canons : les deux coups partent ensemble (16 plombs, recul énorme). */
    public void fireBothBarrels(Level level, Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        for (int i = 0; i < 2 && canFire(player, stack); i++) {
            shoot(level, player, stack);
        }
        player.getCooldowns().addCooldown(this, 10);
    }

    /** Taser : électrodes jusqu'à 8 blocs, la cible est paralysée 4 secondes (non létal). */
    private void taserShot(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition(), dir = player.getLookAngle();
        Vec3 end = eye.add(dir.scale(8));
        var block = level.clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        Vec3 limit = block.getLocation();
        var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player, eye, limit,
                player.getBoundingBox().expandTowards(dir.scale(8)).inflate(1), e -> e.isPickable() && e != player, eye.distanceToSqr(limit));
        Vec3 to = hit != null ? hit.getLocation() : limit;
        for (int i = 0; i <= 16; i++) {
            Vec3 q = eye.add(0, -0.2, 0).lerp(to, i / 16.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
        }
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(fr.modeminecraft.registry.ModEffects.STUNNED, 80, 0), player);
            living.hurt(player.damageSources().playerAttack(player), 1.0f);
            level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.PLAYERS, 1.0f, 2.0f);
        }
    }

    /** Railgun en rubis : un rayon rouge qui traverse tout en ligne droite (blocs exceptés) jusqu'à 160 blocs. */
    private void railShot(ServerLevel level, Player player, float damage) {
        Vec3 eye = player.getEyePosition(), dir = player.getLookAngle();
        Vec3 end = eye.add(dir.scale(160));
        var block = level.clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        Vec3 limit = block.getLocation();
        var box = player.getBoundingBox().expandTowards(limit.subtract(eye)).inflate(1);
        var source = level.damageSources().source(fr.modeminecraft.registry.ModDamageTypes.BULLET_AP, player, player);
        for (var e : level.getEntities(player, box, e -> e instanceof LivingEntity && e.isAlive())) {
            if (e.getBoundingBox().inflate(0.3).clip(eye, limit).isPresent()) {
                e.hurt(source, damage);
                e.invulnerableTime = 0;
            }
        }
        var red = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1f, 0.1f, 0.2f), 1.4f);
        double len = eye.distanceTo(limit);
        for (double t = 1; t < len; t += 0.4) {
            Vec3 q = eye.add(dir.scale(t)).add(0, -0.15, 0);
            level.sendParticles(red, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.END_ROD, limit.x, limit.y, limit.z, 12, 0.1, 0.1, 0.1, 0.1);
    }

    /** Nombre de balles qui devraient être parties t ticks après le début du tir. */
    private int expectedShots(int t) {
        return t < 0 ? 0 : (int) Math.floor(t / stats.shotInterval()) + 1;
    }

    /** Rechargement cartouche par cartouche : tirer l'interrompt (les cartouches déjà mises restent). */
    private boolean interruptReload(Player player, ItemStack stack) {
        if (!stats.reloadPerRound()) {
            return false;
        }
        Long start = stack.get(ModDataComponents.RELOAD_START.get());
        Long end = stack.get(ModDataComponents.RELOAD_END.get());
        long now = player.level().getGameTime();
        if (start == null || end == null || now >= end - 1 || now < start) {
            return false;
        }
        int per = Math.max(1, Math.round(resolve(stack).reloadTicks() * fr.modeminecraft.item.VestItem.reloadMul(player)));
        int target = stack.getOrDefault(ModDataComponents.RELOAD_TARGET.get(), getAmmo(stack));
        int before = stack.getOrDefault(ModDataComponents.RELOAD_FROM.get(), getAmmo(stack));
        int inserted = (int) Math.max(0, Math.min(target - before, (now - start) / per));
        if (!player.level().isClientSide) {
            // Si ce rechargement devait aussi armer (arme vide), une des cartouches insérées part dans la chambre
            Long rack = stack.get(ModDataComponents.RACK_AT.get());
            boolean racks = rack != null && rack >= start;
            int loaded = before + inserted;
            int now_ammo = getAmmo(stack) + (racks ? 1 : 0); // ce que le rechargement complet aurait donné
            int extra = now_ammo - loaded;
            if (extra > 0 && !player.getAbilities().instabuild) {
                giveBack(player, extra, getLoadedType(stack)); // les cartouches pas encore mises sont rendues
            }
            if (racks) {
                setChambered(stack, loaded > 0);
                setAmmo(stack, Math.max(0, loaded - 1));
                stack.remove(ModDataComponents.RACK_AT.get());
            } else {
                setAmmo(stack, loaded);
            }
            stack.set(ModDataComponents.RELOAD_END.get(), now);
        }
        player.getCooldowns().removeCooldown(this);
        return true;
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
                actionSound(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /** Durée de l'inspection de l'arme (touche H). */
    public static final int INSPECT_TICKS = 40;

    /** Touche H : inspection de l'arme (on la tourne pour l'admirer) et vérification de la chambre. */
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
        player.getCooldowns().addCooldown(this, INSPECT_TICKS);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                actionSound(), SoundSource.PLAYERS, 0.4f, 1.25f);
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

    /** Le joueur vise-t-il ? (clic droit maintenu, debout ou allongé) */
    public static boolean isAiming(Player player) {
        return TriggerHandler.isAiming(player);
    }

    protected void shoot(Level level, Player player, ItemStack stack) {
        ResolvedGun gun = resolve(stack);
        AmmoType type = getLoadedType(stack);
        boolean shells = stats.ammo().get() instanceof AmmoItem a && a.isShotgunShell();
        boolean silenced = gun.silenced() || type == AmmoType.SUBSONIC || stats.special() == GunStats.Special.INTEGRATED_SILENCER;
        boolean aiming = isAiming(player);
        boolean fanning = stats.special() == GunStats.Special.FANNING && TriggerHandler.isRightHeld(player);
        // Allongé : l'arme est bien plus stable ; stable (bipied, monopode...) : bonus des accessoires
        float stability = ProneHandler.isProne(player) ? 0.5f : 1.0f;
        boolean steady = isSteady(player);
        float steadyRecoil = steady ? gun.steadyRecoil() : 1, steadySpread = steady ? gun.steadySpread() : 1;

        if (level.isClientSide) {
            GunAnimations.recordShot(player); // la culasse recule (animation)
            fr.modeminecraft.client.GunFx.onLocalShot(player, silenced || gun.has(AttachmentItem.Special.NO_FLASH));
            ejectCasing(level, player, null);
            // Recul : la caméra remonte un peu (uniquement côté client, le serveur suit)
            float recoil = (aiming ? gun.recoil() * 0.6f : gun.recoil()) * stability * type.recoilMul * steadyRecoil;
            // Mitrailleuses : très fort recul debout (s'accroupir, s'allonger ou poser le bipied pour la tenir)
            if (stats.category() == GunStats.Category.MACHINE_GUN && !player.isCrouching() && !ProneHandler.isProne(player) && !steady) {
                recoil *= 1.7f;
            }
            player.setXRot(Mth.clamp(player.getXRot() - recoil, -90f, 90f));
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * recoil * 0.5f);
            return;
        }

        float heat = heat(stack, level);
        float spread = (aiming ? gun.aimSpread() : gun.spread()) * stability * type.spreadMul * steadySpread
                * (isMoving(player) ? gun.movingPenalty() : 1)
                * (1 + Math.max(0, heat - 60) / 40f); // canon trop chaud : moins précis
        if (fanning) {
            spread *= 2.5f; // tir au chien frappé : rapide mais imprécis
        }
        if (fr.modeminecraft.item.ShieldCombat.active(player)) {
            spread *= fr.modeminecraft.item.ShieldCombat.ONE_HAND_SPREAD;   // tir à une main sur le côté du bouclier
        }
        Vec3 lean = LeanHandler.offset(player);
        int pellets = shells ? type.pellets(stats.pellets()) : stats.pellets();
        if (stats.projectile() == GunStats.ProjectileKind.TASER) {
            taserShot((ServerLevel) level, player);
            pellets = 0;
        } else if (stats.projectile() == GunStats.ProjectileKind.RAIL) {
            railShot((ServerLevel) level, player, gun.damage());
            pellets = 0;
        } else if (stats.projectile() == GunStats.ProjectileKind.EMP) {
            fr.modeminecraft.entity.drone.Jamming.empShot((ServerLevel) level, player);
            pellets = 0;
        }
        float damage = shells ? type.pelletDamage(gun.damage(), stats.pellets()) : gun.damage() * type.damageMul;
        for (int i = 0; i < pellets; i++) {
            Projectile projectile = createProjectile(level, player, gun, type, damage);
            if (lean != Vec3.ZERO) {
                projectile.setPos(projectile.position().add(lean)); // penché : la balle part de plus loin sur le côté
            }
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f,
                    stats.velocity() * type.velocityMul, spread);
            if (projectile instanceof BulletEntity bullet) {
                bullet.setWeapon(stack); // pour compter les éliminations de cette arme
            }
            level.addFreshEntity(projectile);
        }
        stack.set(ModDataComponents.PROGRESS.get(), getProgress(stack).withShot());

        if (!player.getAbilities().instabuild && stats.special() != GunStats.Special.NO_AMMO) {
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

        playShotSound(level, player, gun, type, silenced);
        if (stats.special() == GunStats.Special.EN_BLOC && getAmmo(stack) == 0 && !isChambered(stack)
                && !player.getAbilities().instabuild) {
            // M1 Garand : le clip vide est éjecté avec son fameux « ping »
            DelayedSounds.play(player, ModSounds.GARAND_PING, 1.0f, 1.0f, 2);
        }
        if (stats.special() == GunStats.Special.BLACK_POWDER && level instanceof ServerLevel sl) {
            Vec3 m = player.getEyePosition().add(player.getLookAngle().scale(1.4));
            sl.sendParticles(ParticleTypes.CLOUD, m.x, m.y, m.z, 18, 0.35, 0.25, 0.35, 0.02);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, m.x, m.y, m.z, 12, 0.3, 0.2, 0.3, 0.01);
        }
        // Pompe / verrou : le tireur réarme juste après le tir
        if (stats.casing() == GunStats.Casing.SHELL && stats.fireDelay() >= 12) {
            DelayedSounds.play(player, ModSounds.PUMP, 0.9f, 1.0f, 4);
        } else if (isBoltAction()) {
            DelayedSounds.play(player, ModSounds.BOLT, 0.9f, 1.0f, 5);
        }
        // Les joueurs proches voient le flash et la culasse de ce tir
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(sp,
                    new fr.modeminecraft.network.ShotFxPayload(player.getId(), silenced || gun.has(AttachmentItem.Special.NO_FLASH)));
        }

        // Flash et fumée au bout du canon (pas de flash avec un silencieux)
        if (level instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle();
            Vec3 muzzle = player.getEyePosition().add(look.scale(1.2)).add(0, -0.15, 0);
            serverLevel.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.03, 0.03, 0.03, 0.01);
            // (le flash 3D est affiché par les clients : GunFx)
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
            case ROCKET -> {
                var r = new RocketEntity(ModEntities.ROCKET.get(), level, player, stats.explosionPower());
                r.setDirectDamage(stats.directDamage());
                yield r;
            }
            case MISSILE -> {
                var m = new RocketEntity(ModEntities.MISSILE.get(), level, player, stats.explosionPower());
                m.setItem(new ItemStack(stats.ammo().get()));
                m.guide(LockOnHandler.target(player), stats.special() == GunStats.Special.LOCK_ON_TOP_ATTACK);
                yield m;
            }
            case BOLT -> new fr.modeminecraft.entity.BoltEntity(level, player, damage / 2.0, type);
            case PLASMA -> new fr.modeminecraft.entity.PlasmaEntity(level, player, damage);
            case TASER, RAIL, EMP -> throw new IllegalStateException("arme sans projectile");
            case GRENADE -> new RocketEntity(ModEntities.GRENADE.get(), level, player, stats.explosionPower());
            case BULLET -> {
                String caliber = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stats.ammo().get()).getPath();
                boolean big = BIG_CALIBERS.contains(caliber);
                var bullet = new BulletEntity(level, player, damage,
                        stats.pierce() + (type == AmmoType.ARMOR_PIERCING ? 1 : 0) + (big ? 1 : 0),
                        type == AmmoType.ARMOR_PIERCING, type);
                float pen = ARMOR_PEN.getOrDefault(caliber, 0.1f) + (type == AmmoType.ARMOR_PIERCING ? 0.5f : 0)
                        + (stats.armorPiercing() ? 0.3f : 0)
                        + (Legendary.of(player.getMainHandItem()) instanceof Legendary l && l.armorPiercing ? 0.4f : 0);
                bullet.setArmorPenetration(pen);
                bullet.setHeadshotMultiplier(stats.category() == GunStats.Category.SNIPER ? 2.5f : 2.0f);
                bullet.setBallistics(stats.falloff(), stats.noDropBlocks(), stats.bulletLife());
                yield bullet;
            }
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
        Legendary legendary = Legendary.of(stack);
        float perShot = stats.heatPerShot() * (legendary != null ? legendary.heatMul : 1);
        float heat = Math.min(100, current + perShot + COOLING_PER_TICK * stats.fireDelay());
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

    /** Touche C : coup de crosse (3 dégâts) ou de baïonnette (+6), sur l'ennemi visé à moins de 3 blocs. */
    public void melee(Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        player.getCooldowns().addCooldown(this, 14);
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(3.0));
        var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player, eye, end,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(3.0)).inflate(1), e -> e.isPickable() && e != player, 9.0);
        if (hit == null) {
            return;
        }
        boolean bayonet = resolve(stack).has(AttachmentItem.Special.BAYONET);
        float damage = 3.0f; // les +6 de la baïonnette sont ajoutés par GunEvents
        hit.getEntity().hurt(player.damageSources().playerAttack(player), damage);
        if (hit.getEntity() instanceof LivingEntity living) {
            living.knockback(bayonet ? 0.3 : 0.6, player.getX() - living.getX(), player.getZ() - living.getZ());
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                bayonet ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.8f, 1.0f);
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
        if (stats.special() == GunStats.Special.NO_AMMO) {
            return;
        }
        if (stats.special() == GunStats.Special.EN_BLOC && (getAmmo(stack) > 0 || isChambered(stack))
                && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.en_bloc").withStyle(ChatFormatting.GRAY), true);
            return;
        }
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
                    ModSounds.EMPTY_CLICK.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            return;
        }

        stack.set(ModDataComponents.RELOAD_FROM.get(), getAmmo(stack));
        setAmmo(stack, getAmmo(stack) + taken);
        stack.set(ModDataComponents.RELOAD_TARGET.get(), getAmmo(stack));
        // Pas de stopUsingItem() ici : il ne s'exécuterait que côté serveur et désynchroniserait le client.
        // Le cooldown (synchronisé) suffit à suspendre le tir automatique pendant le rechargement.
        // Rechargement balle par balle : la durée dépend du nombre de balles insérées
        // Gilet à poches ventrales (6B3, JPC) : rechargement plus rapide
        int per = Math.max(1, Math.round(gun.reloadTicks() * fr.modeminecraft.item.VestItem.reloadMul(player)));
        int time = stats.reloadPerRound() ? per * taken : per;
        long now = player.level().getGameTime();
        boolean wasChambered = isChambered(stack);
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
        // Sons du rechargement : chargeur retiré / mis, ou une cartouche après l'autre, puis la culasse
        if (stats.reloadPerRound()) {
            for (int i = 0; i < taken; i++) {
                DelayedSounds.play(player, ModSounds.SHELL_INSERT, 0.8f, 0.95f + player.getRandom().nextFloat() * 0.1f,
                        per * i + per / 2);
            }
        } else {
            DelayedSounds.play(player, ModSounds.MAG_OUT, 0.9f, 1.0f, 0);
            DelayedSounds.play(player, ModSounds.MAG_IN, 1.0f, 1.0f, Math.round((stats.chamber() && !wasChambered ? time - rackTicks() : time) * 0.7f));
        }
        if (stats.chamber() && !wasChambered) {
            DelayedSounds.play(player, this::actionSound, 1.0f, 1.0f, time - rackTicks());
        }
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
        Legendary legendary = Legendary.of(stack);
        if (legendary != null) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.legendary." + legendary.id).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("tooltip.modeminecraft.legendary.trophy",
                    Component.translatable("entity.modeminecraft.bot." + legendary.boss)).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
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
        GunProgress progress = getProgress(stack);
        int next = progress.nextLevelXp();
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.level", progress.level(),
                next < 0 ? Component.translatable("tooltip.modeminecraft.gun.level_max") : Component.literal(progress.xp() + " / " + next))
                .withStyle(ChatFormatting.YELLOW));
        if (progress.shots() > 0) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.gun.stats", progress.kills(), progress.headshots(), progress.shots(),
                    progress.shots() == 0 ? 0 : Math.round(100f * progress.kills() / progress.shots())).withStyle(ChatFormatting.DARK_GRAY));
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

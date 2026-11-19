package fr.modeminecraft.item.gun;

import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.entity.RocketEntity;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModEntities;
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
 * - skin (SKIN) -> purement visuel.
 *
 * Commandes : clic droit = tirer, B = mode de tir, R = recharger, accroupi = viser.
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
        // Un chargeur doit contenir plus de balles que celui d'origine
        return attachment.slot() != AttachmentSlot.MAGAZINE
                || attachment.stats().magazineSize() > stats.magazineSize();
    }

    /** Nombre de munitions de ce type dans l'inventaire du joueur (réserve). */
    public int countReserve(Player player) {
        Inventory inv = player.getInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(stats.ammo().get())) {
                total += s.getCount();
            }
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Tir
    // ------------------------------------------------------------------
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (getAmmo(stack) <= 0 && !player.getAbilities().instabuild) {
            if (!level.isClientSide) {
                reload(player, stack);
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
        if (getAmmo(stack) <= 0 && !player.getAbilities().instabuild) {
            player.stopUsingItem();
            if (!level.isClientSide) {
                reload(player, stack);
            }
            return;
        }
        shoot(level, player, stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    protected void shoot(Level level, Player player, ItemStack stack) {
        ResolvedGun gun = resolve(stack);
        boolean aiming = player.isCrouching();

        if (level.isClientSide) {
            GunAnimations.recordShot(player); // la culasse recule (animation)
            // Recul : la caméra remonte un peu (uniquement côté client, le serveur suit)
            float recoil = aiming ? gun.recoil() * 0.6f : gun.recoil();
            player.setXRot(Mth.clamp(player.getXRot() - recoil, -90f, 90f));
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * recoil * 0.5f);
            return;
        }

        float spread = aiming ? gun.aimSpread() : gun.spread();
        for (int i = 0; i < stats.pellets(); i++) {
            Projectile projectile = createProjectile(level, player, gun);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, stats.velocity(), spread);
            level.addFreshEntity(projectile);
        }

        if (!player.getAbilities().instabuild) {
            setAmmo(stack, getAmmo(stack) - 1);
            if (stats.disposable() && getAmmo(stack) <= 0) {
                // AT4 : le tube vide est jeté
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);
                stack.shrink(1);
            }
        }

        float pitch = stats.pitch() + (gun.silenced() ? 0.5f : 0f) + (level.random.nextFloat() - 0.5f) * 0.1f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                stats.sound().get(), SoundSource.PLAYERS, gun.volume(), pitch);

        // Flash et fumée au bout du canon (pas de flash avec un silencieux)
        if (level instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle();
            Vec3 muzzle = player.getEyePosition().add(look.scale(1.2)).add(0, -0.15, 0);
            serverLevel.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.03, 0.03, 0.03, 0.01);
            if (!gun.silenced()) {
                serverLevel.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 1, 0, 0, 0, 0);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!gun.silenced()) {
            // Un tir silencieux n'est pas détecté par les capteurs sculk
            level.gameEvent(player, GameEvent.PROJECTILE_SHOOT, player.position());
        }
    }

    protected Projectile createProjectile(Level level, Player player, ResolvedGun gun) {
        return switch (stats.projectile()) {
            case ROCKET -> new RocketEntity(ModEntities.ROCKET.get(), level, player, stats.explosionPower());
            case GRENADE -> new RocketEntity(ModEntities.GRENADE.get(), level, player, stats.explosionPower());
            case BULLET -> new BulletEntity(level, player, gun.damage(), stats.pierce(), stats.armorPiercing());
        };
    }

    // ------------------------------------------------------------------
    // Rechargement (appelé côté serveur : touche R ou chargeur vide)
    // ------------------------------------------------------------------
    public void reload(Player player, ItemStack stack) {
        ResolvedGun gun = resolve(stack);
        int missing = gun.magazineSize() - getAmmo(stack);
        if (stats.disposable() || missing <= 0 || player.getCooldowns().isOnCooldown(this)) {
            return;
        }

        int taken = player.getAbilities().instabuild ? missing : takeAmmo(player, missing);
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
        player.getCooldowns().addCooldown(this, stats.reloadPerRound() ? gun.reloadTicks() * taken : gun.reloadTicks());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    private int takeAmmo(Player player, int wanted) {
        Inventory inv = player.getInventory();
        int taken = 0;
        for (int i = 0; i < inv.getContainerSize() && taken < wanted; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(stats.ammo().get())) {
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
        return Math.round(13.0f * getAmmo(stack) / resolve(stack).magazineSize());
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
        String damage = stats.projectile() != GunStats.ProjectileKind.BULLET
                ? Component.translatable("tooltip.modeminecraft.gun.explosive").getString()
                : (stats.pellets() > 1 ? stats.pellets() + " x " : "") + format(gun.damage());
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.damage", damage).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.ammo_type", stats.ammo().get().getDescription())
                .withStyle(ChatFormatting.GRAY));

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

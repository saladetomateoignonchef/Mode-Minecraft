package fr.modeminecraft.item.gun;

import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Supplier;

/**
 * Arme à feu générique. Toutes les armes (pistolet, fusil, sniper...) utilisent cette classe
 * avec des statistiques différentes (voir {@link GunStats}).
 *
 * Fonctionnement :
 * - Clic droit : tirer (en continu pour les armes automatiques).
 * - Touche R : recharger (consomme les munitions de l'inventaire).
 * - Chargeur vide + clic droit : rechargement automatique.
 * - S'accroupir : viser (moins de dispersion ; zoom pour le sniper).
 * - En créatif : munitions infinies.
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
    // Munitions dans le chargeur
    // ------------------------------------------------------------------
    public static int getAmmo(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AMMO.get(), 0);
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        stack.set(ModDataComponents.AMMO.get(), ammo);
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

        if (stats.automatic()) {
            // Le tir se fait dans onUseTick tant que le clic droit est maintenu
            player.startUsingItem(hand);
        } else {
            shoot(level, player, stack);
            player.getCooldowns().addCooldown(this, stats.fireDelay());
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
        if (ticksUsed % stats.fireDelay() != 0) {
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
        boolean aiming = player.isCrouching();

        if (level.isClientSide) {
            // Recul : la caméra remonte un peu (uniquement côté client, le serveur suit)
            float recoil = aiming ? stats.recoil() * 0.6f : stats.recoil();
            player.setXRot(Mth.clamp(player.getXRot() - recoil, -90f, 90f));
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * recoil * 0.5f);
            return;
        }

        float spread = aiming ? stats.aimSpread() : stats.spread();
        for (int i = 0; i < stats.pellets(); i++) {
            Projectile projectile = createProjectile(level, player);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, stats.velocity(), spread);
            level.addFreshEntity(projectile);
        }

        if (!player.getAbilities().instabuild) {
            setAmmo(stack, getAmmo(stack) - 1);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                stats.sound(), SoundSource.PLAYERS, stats.volume(), stats.pitch() + (level.random.nextFloat() - 0.5f) * 0.1f);

        // Flash et fumée au bout du canon
        if (level instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle();
            Vec3 muzzle = player.getEyePosition().add(look.scale(1.2)).add(0, -0.15, 0);
            serverLevel.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.03, 0.03, 0.03, 0.01);
            serverLevel.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 1, 0, 0, 0, 0);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        level.gameEvent(player, GameEvent.PROJECTILE_SHOOT, player.position());
    }

    /** Crée le projectile tiré. Redéfini par le lance-roquettes. */
    protected Projectile createProjectile(Level level, Player player) {
        return new BulletEntity(level, player, stats.damage());
    }

    // ------------------------------------------------------------------
    // Rechargement (appelé côté serveur : touche R ou chargeur vide)
    // ------------------------------------------------------------------
    public void reload(Player player, ItemStack stack) {
        int missing = stats.magazineSize() - getAmmo(stack);
        if (missing <= 0 || player.getCooldowns().isOnCooldown(this)) {
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
        // Le temps de recharge est affiché comme un "cooldown" sur l'icône de l'arme
        player.getCooldowns().addCooldown(this, stats.reloadTicks());
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
        return Math.round(13.0f * getAmmo(stack) / stats.magazineSize());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return getAmmo(stack) <= stats.magazineSize() / 4 ? 0xFF5555 : 0xFFD84A;
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
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.ammo", getAmmo(stack), stats.magazineSize())
                .withStyle(ChatFormatting.GOLD));
        String damage = stats.pellets() > 1
                ? stats.pellets() + " x " + formatNumber(stats.damage())
                : formatNumber(stats.damage());
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.damage", damage).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.gun.ammo_type", stats.ammo().get().getDescription())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(stats.automatic()
                ? "tooltip.modeminecraft.gun.controls_auto"
                : "tooltip.modeminecraft.gun.controls").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String formatNumber(float value) {
        return value == (int) value ? Integer.toString((int) value) : Float.toString(value);
    }

    /** Statistiques d'une arme. Les valeurs de dispersion sont en "degrés environ". */
    public record GunStats(
            float damage,          // dégâts par balle (1 coeur = 2)
            int pellets,           // nombre de projectiles par tir (fusil à pompe > 1)
            int magazineSize,      // capacité du chargeur
            int fireDelay,         // ticks entre deux tirs (20 ticks = 1 seconde)
            int reloadTicks,       // durée du rechargement
            float velocity,        // vitesse du projectile (flèche d'arc tendu = 3)
            float spread,          // dispersion en tir normal
            float aimSpread,       // dispersion accroupi (visée)
            float recoil,          // recul de la caméra en degrés
            boolean automatic,     // tir continu en maintenant le clic droit
            Supplier<? extends Item> ammo,
            SoundEvent sound, float volume, float pitch) {
    }
}

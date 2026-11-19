package fr.modeminecraft.item.explosive;

import fr.modeminecraft.entity.ThrownGrenadeEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Grenade : clic droit maintenu = on dégoupille (le minuteur tourne dans la main), relâcher = lancer.
 * Plus on garde la grenade, moins il reste de temps avant l'explosion (mais elle part plus loin, jusqu'à 1 s).
 * Si on la garde trop longtemps... elle explose dans la main.
 */
public class GrenadeItem extends Item {
    /** Les grenades du cahier des charges : minuteur (ticks), vitesse de lancer, comportement. */
    public enum Kind {
        FRAG(70, 1.7f, false, false), OFFENSIVE(60, 1.8f, false, false), SEMTEX(50, 1.6f, false, true),
        MOLOTOV(0, 1.6f, true, false), CLUSTER(60, 1.7f, false, false), FLASHBANG(40, 1.3f, false, false),
        SMOKE(50, 1.3f, false, false), GAS(50, 1.3f, false, false), EMP(40, 1.3f, false, false),
        DECOY(30, 1.3f, false, false), IMPULSE(30, 1.3f, false, false), SENSOR(10, 1.4f, false, true),
        BOMBLET(24, 0.6f, false, false);

        public final int fuse;
        public final float velocity;
        public final boolean impact, sticky;

        Kind(int fuse, float velocity, boolean impact, boolean sticky) {
            this.fuse = fuse;
            this.velocity = velocity;
            this.impact = impact;
            this.sticky = sticky;
        }

        /** Le minuteur tourne dès qu'on dégoupille (pas pour les grenades à impact ou collantes). */
        public boolean cooks() {
            return !impact && !sticky;
        }
    }

    private final Kind kind;

    public GrenadeItem(Kind kind, Properties properties) {
        super(properties.stacksTo(8));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), fr.modeminecraft.registry.ModSounds.GRENADE_PIN.get(),
                SoundSource.PLAYERS, 0.8f, 1.0f);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int held = getUseDuration(stack, entity) - remaining;
        if (kind.cooks() && held >= kind.fuse && entity instanceof Player player) {
            // Gardée trop longtemps : elle explose dans la main
            player.stopUsingItem();
            if (!level.isClientSide) {
                ThrownGrenadeEntity g = new ThrownGrenadeEntity(level, player, kind, 1);
                g.setPos(player.getX(), player.getEyeY() - 0.3, player.getZ());
                level.addFreshEntity(g);
                consume(player, stack);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int held = getUseDuration(stack, entity) - remaining;
        if (!level.isClientSide) {
            int fuse = kind.cooks() ? Math.max(1, kind.fuse - held) : kind.fuse;
            ThrownGrenadeEntity g = new ThrownGrenadeEntity(level, player, kind, fuse);
            float power = 0.7f + 0.3f * Math.min(1, held / 20f);
            g.shootFromRotation(player, player.getXRot(), player.getYRot(), -5, kind.velocity * power, 1.0f);
            level.addFreshEntity(g);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8f, 0.6f);
            consume(player, stack);
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        player.getCooldowns().addCooldown(this, 10);
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath();
        tooltip.add(Component.translatable("tooltip.modeminecraft.grenade." + id).withStyle(ChatFormatting.GOLD));
        if (kind.fuse > 0 && kind.cooks()) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.grenade.fuse", String.format("%.1f", kind.fuse / 20f))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.modeminecraft.grenade.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}

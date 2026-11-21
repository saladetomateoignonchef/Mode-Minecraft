package fr.modeminecraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Soins tactiques :
 * - bandage : +4 PV tout de suite (+ régénération), arrête le saignement ;
 * - trousse de premiers secours : +8 PV tout de suite, +4 PV de régénération, retire tous les effets négatifs ;
 * - seringue d'adrénaline : +6 PV, vitesse II, force I et résistance I pendant 15 s.
 */
public class MedicalItem extends Item {
    public enum Kind { BANDAGE, FIRST_AID_KIT, ADRENALINE }

    private final Kind kind;

    public MedicalItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    /**
     * Un simple clic droit suffit : le soin est appliqué tout de suite (cœurs, son, message), avec un court délai
     * avant le suivant. Pas de gaspillage : en pleine santé et sans effet à soigner, l'objet n'est pas consommé.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean hurt = player.getHealth() < player.getMaxHealth();
        boolean bad = player.getActiveEffects().stream().anyMatch(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
        if (kind != Kind.ADRENALINE && !hurt && !bad) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.modeminecraft.medical.full").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.swing(hand, true);
        if (!level.isClientSide) {
            float before = player.getHealth();
            treat(player);
            float healed = player.getHealth() - before;
            level.playSound(null, player.getX(), player.getY(), player.getZ(), kind == Kind.ADRENALINE ? SoundEvents.HONEY_DRINK : SoundEvents.ARMOR_EQUIP_LEATHER.value(),
                    SoundSource.PLAYERS, 0.9f, kind == Kind.ADRENALINE ? 1.6f : 1.0f);
            ((net.minecraft.server.level.ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.2, player.getZ(), Math.max(2, (int) healed), 0.4, 0.3, 0.4, 0.05);
            player.displayClientMessage(Component.translatable("message.modeminecraft.medical." + kind.name().toLowerCase(),
                    String.format(java.util.Locale.ROOT, "%.0f", healed)).withStyle(ChatFormatting.GREEN), true);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, kind == Kind.FIRST_AID_KIT ? 40 : 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void treat(LivingEntity user) {
        switch (kind) {
            case BANDAGE -> {
                user.heal(4);
                removeBleeding(user);
                user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));            // + un peu de régénération
            }
            case FIRST_AID_KIT -> {
                user.heal(8);
                List<Holder<MobEffect>> bad = new ArrayList<>();
                for (MobEffectInstance e : user.getActiveEffects()) {
                    if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        bad.add(e.getEffect());
                    }
                }
                bad.forEach(user::removeEffect);
                user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));           // +4 PV sur 5 s
            }
            case ADRENALINE -> {
                user.heal(6);
                user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
                user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
                user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
            }
        }
    }

    /** Pour les bots (soins entre deux combats). */
    public void applyTo(LivingEntity user) {
        treat(user);
    }

    private static void removeBleeding(LivingEntity user) {
        for (MobEffectInstance e : new ArrayList<>(user.getActiveEffects())) {
            if (e.getEffect().getRegisteredName().contains("bleed")) {
                user.removeEffect(e.getEffect());
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.medical." + kind.name().toLowerCase()).withStyle(ChatFormatting.GREEN));
    }
}

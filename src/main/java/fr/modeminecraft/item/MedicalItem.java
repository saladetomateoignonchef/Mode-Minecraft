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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Soins tactiques :
 * - bandage (1,6 s) : +4 PV, arrête le saignement ;
 * - trousse de premiers secours (3 s) : +12 PV et retire tous les effets négatifs ;
 * - seringue d'adrénaline (0,5 s) : +6 PV, vitesse II, force I et résistance I pendant 15 s.
 */
public class MedicalItem extends Item {
    public enum Kind { BANDAGE, FIRST_AID_KIT, ADRENALINE }

    private final Kind kind;

    public MedicalItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return switch (kind) {
            case BANDAGE -> 32;
            case FIRST_AID_KIT -> 60;
            case ADRENALINE -> 10;
        };
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.ADRENALINE ? UseAnim.SPEAR : UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide) {
            switch (kind) {
                case BANDAGE -> {
                    user.heal(4);
                    removeBleeding(user);
                }
                case FIRST_AID_KIT -> {
                    user.heal(12);
                    List<Holder<MobEffect>> bad = new ArrayList<>();
                    for (MobEffectInstance e : user.getActiveEffects()) {
                        if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                            bad.add(e.getEffect());
                        }
                    }
                    bad.forEach(user::removeEffect);
                }
                case ADRENALINE -> {
                    user.heal(6);
                    user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
                    user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
                    user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
                }
            }
            level.playSound(null, user.getX(), user.getY(), user.getZ(), kind == Kind.ADRENALINE ? SoundEvents.HONEY_DRINK : SoundEvents.ARMOR_EQUIP_LEATHER.value(),
                    SoundSource.PLAYERS, 0.8f, kind == Kind.ADRENALINE ? 1.6f : 1.0f);
        }
        if (!(user instanceof Player p && p.getAbilities().instabuild)) {
            stack.shrink(1);
        }
        return stack;
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

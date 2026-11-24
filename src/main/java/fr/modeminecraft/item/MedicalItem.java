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
 * - seringue d'adrénaline : +6 PV, vitesse II, force I et résistance I pendant 15 s ;
 * - garrot : arrête le saignement et empêche d'en subir un nouveau pendant 1 minute ;
 * - attelle : immobilise une jambe cassée (fracture réduite à un niveau léger pendant 4 minutes) ;
 * - plâtre : guérit complètement la fracture ;
 * - défibrillateur : +12 PV et retire paralysie et brouillage, réservé à quelqu'un de gravement blessé.
 * Chaque soin peut aussi être appliqué à un autre joueur ou à un bot en lui faisant un clic droit.
 */
public class MedicalItem extends Item {
    public enum Kind { BANDAGE, FIRST_AID_KIT, ADRENALINE, TOURNIQUET, SPLINT, CAST, DEFIBRILLATOR }

    /** Étiquette (données du joueur) : jusqu'à quel instant le garrot empêche de saigner. */
    public static final String TOURNIQUET_TAG = "mm_tourniquet";

    private final Kind kind;

    public MedicalItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    /** Ce soin a-t-il quelque chose à soigner chez cette personne ? (sinon on ne gaspille pas l'objet) */
    private boolean needed(LivingEntity who) {
        boolean hurt = who.getHealth() < who.getMaxHealth();
        boolean bad = who.getActiveEffects().stream().anyMatch(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
        return switch (kind) {
            case ADRENALINE -> true;
            case TOURNIQUET -> who.hasEffect(fr.modeminecraft.registry.ModEffects.BLEEDING);
            case SPLINT -> who.hasEffect(fr.modeminecraft.registry.ModEffects.FRACTURE)
                    && who.getEffect(fr.modeminecraft.registry.ModEffects.FRACTURE).getAmplifier() >= 1;
            case CAST -> who.hasEffect(fr.modeminecraft.registry.ModEffects.FRACTURE);
            case DEFIBRILLATOR -> who.getHealth() <= who.getMaxHealth() * 0.5f;
            default -> hurt || bad;
        };
    }

    /**
     * Un simple clic droit suffit : le soin est appliqué tout de suite (cœurs, son, message), avec un court délai
     * avant le suivant. Pas de gaspillage : si rien n'est à soigner, l'objet n'est pas consommé.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!needed(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.modeminecraft.medical.full").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.swing(hand, true);
        if (!level.isClientSide) {
            apply(stack, player, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Soigner un autre joueur ou un bot : clic droit sur lui. */
    @Override
    public net.minecraft.world.InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!needed(target)) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.modeminecraft.medical.full").withStyle(ChatFormatting.GRAY), true);
            }
            return net.minecraft.world.InteractionResult.FAIL;
        }
        player.swing(hand, true);
        if (!player.level().isClientSide) {
            apply(stack, player, target);
            if (target instanceof Player other) {
                other.displayClientMessage(Component.translatable("message.modeminecraft.medical.treated_by", player.getDisplayName())
                        .withStyle(ChatFormatting.GREEN), true);
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    private void apply(ItemStack stack, Player user, LivingEntity patient) {
        Level level = user.level();
        float before = patient.getHealth();
        treat(patient);
        float healed = patient.getHealth() - before;
        boolean light = kind == Kind.BANDAGE || kind == Kind.FIRST_AID_KIT;
        level.playSound(null, patient.getX(), patient.getY(), patient.getZ(),
                kind == Kind.ADRENALINE ? SoundEvents.HONEY_DRINK : kind == Kind.DEFIBRILLATOR ? SoundEvents.LIGHTNING_BOLT_IMPACT : SoundEvents.ARMOR_EQUIP_LEATHER.value(),
                SoundSource.PLAYERS, kind == Kind.DEFIBRILLATOR ? 0.5f : 0.9f, kind == Kind.ADRENALINE ? 1.6f : 1.0f);
        ((net.minecraft.server.level.ServerLevel) level).sendParticles(
                kind == Kind.DEFIBRILLATOR ? net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK : net.minecraft.core.particles.ParticleTypes.HEART,
                patient.getX(), patient.getY() + 1.2, patient.getZ(), Math.max(2, (int) healed), 0.4, 0.3, 0.4, 0.05);
        user.displayClientMessage(Component.translatable("message.modeminecraft.medical." + kind.name().toLowerCase(),
                String.format(java.util.Locale.ROOT, "%.0f", healed)).withStyle(ChatFormatting.GREEN), true);
        if (!user.getAbilities().instabuild) {
            stack.shrink(1);
        }
        user.getCooldowns().addCooldown(this, kind == Kind.FIRST_AID_KIT ? 40 : kind == Kind.DEFIBRILLATOR ? 600 : 20);
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
            case TOURNIQUET -> {
                removeBleeding(user);
                user.getPersistentData().putLong(TOURNIQUET_TAG, user.level().getGameTime() + 1200);   // 60 s sans nouveau saignement
            }
            case SPLINT -> {
                user.removeEffect(fr.modeminecraft.registry.ModEffects.FRACTURE);          // sinon la fracture complète (niveau plus fort) reste prioritaire
                user.addEffect(new MobEffectInstance(fr.modeminecraft.registry.ModEffects.FRACTURE, 4800, 0));
            }
            case CAST -> {
                user.removeEffect(fr.modeminecraft.registry.ModEffects.FRACTURE);
                user.heal(2);
            }
            case DEFIBRILLATOR -> {
                user.heal(12);
                user.removeEffect(fr.modeminecraft.registry.ModEffects.STUNNED);
                user.removeEffect(fr.modeminecraft.registry.ModEffects.EMP);
                user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
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

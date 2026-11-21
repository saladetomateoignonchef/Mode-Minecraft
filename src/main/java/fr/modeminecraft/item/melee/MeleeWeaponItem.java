package fr.modeminecraft.item.melee;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Arme blanche du cahier des charges.
 * - Combos : 3 coups enchaînés (balayage, revers, coup vertical / estoc), le 3e fait +25 % ;
 * - clic droit maintenu = parade (-60 % de dégâts au corps à corps), parade parfaite dans les 0,3 s ;
 * - armes légères : coup dans le dos x2,5 (x3 dague / karambit) ;
 * - capacité propre (ruée du katana, lancer du tomahawk, onde de choc du marteau...).
 */
public class MeleeWeaponItem extends SwordItem {
    /** Capacités particulières. */
    public enum Ability {
        NONE, DASH, THROW, SHOCKWAVE, STUN, ANTI_UNDEAD, BLEED, FAST_PARRY, WIDE_SWEEP, THRUST_PIERCE,
        JUMP_CRITS, CHARGE, DISMOUNT, PULL, STUN_CHANCE, CROWBAR, WATER, ARMOR_BREAK
    }

    /** Statistiques : dégâts (PV), attaques par seconde, portée, vitesse de marche, coup dans le dos, armure ignorée. */
    public record Stats(float damage, float speed, float reach, float walk, float backstab, float armorPen, Ability ability) {
        public boolean light() {
            return backstab > 1;
        }
    }

    private final Stats stats;

    public MeleeWeaponItem(Stats stats, Properties properties) {
        super(ModTiers.RUBIS, properties.attributes(attributes(stats)));
        this.stats = stats;
    }

    private static ItemAttributeModifiers attributes(Stats s) {
        var b = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, s.damage() - 1, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, s.speed() - 4, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);
        if (s.reach() != 3) {
            b.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ModeMinecraft.id("melee_reach"), s.reach() - 3,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        if (s.walk() != 1) {
            b.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(ModeMinecraft.id("melee_walk"), s.walk() - 1,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND);
        }
        return b.build();
    }

    public Stats stats() {
        return stats;
    }

    /** Fenêtre de parade parfaite (ticks) : 0,3 s, 0,5 s pour le wakizashi. */
    public int perfectParryTicks() {
        return stats.ability() == Ability.FAST_PARRY ? 10 : 6;
    }

    /** Ces armes utilisent le clic droit pour leur capacité au lieu de parer. */
    public boolean hasActiveAbility() {
        return stats.ability() == Ability.DASH || stats.ability() == Ability.THROW || stats.ability() == Ability.SHOCKWAVE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasActiveAbility()) {
            player.startUsingItem(hand); // parade
            return InteractionResultHolder.consume(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        switch (stats.ability()) {
            case DASH -> {
                // Katana : ruée de 4 blocs vers l'avant
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(look.x * 1.6, Math.max(0.15, look.y * 0.6), look.z * 1.6);
                player.hurtMarked = true;
                player.getCooldowns().addCooldown(this, 60);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.4f);
            }
            case THROW -> {
                // Tomahawk : lancer en cloche, récupérable
                if (!level.isClientSide) {
                    var axe = new fr.modeminecraft.entity.TomahawkEntity(level, player, stack.copyWithCount(1));
                    axe.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.6f, 1.0f);
                    level.addFreshEntity(axe);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                player.getCooldowns().addCooldown(this, 10);
            }
            case SHOCKWAVE -> {
                // Marteau de guerre : onde de choc qui projette les ennemis proches en l'air et brise les boucliers
                if (!level.isClientSide) {
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.5),
                            e -> e != player && e.isAlive() && !e.isAlliedTo(player))) {
                        e.hurt(player.damageSources().playerAttack(player), 4);
                        Vec3 push = e.position().subtract(player.position()).normalize();
                        e.setDeltaMovement(push.x * 0.6, 1.1, push.z * 0.6);
                        e.hurtMarked = true;
                        MeleeEvents.breakShield(e);
                    }
                    ((net.minecraft.server.level.ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                            player.getX(), player.getY() + 0.2, player.getZ(), 6, 2, 0.1, 2, 0);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 0.7f);
                }
                player.getCooldowns().addCooldown(this, 80);
            }
            default -> { }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Pied-de-biche : force instantanément les portes et trappes en fer. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (stats.ability() != Ability.CROWBAR) {
            return super.useOn(context);
        }
        Level level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (context.getPlayer() != null && Crowbar.pryable(level, pos, context.getPlayer())) {
            // effraction : 2,5 s de levier (clic droit maintenu)
            Crowbar.begin(context.getPlayer(), pos);
            context.getPlayer().startUsingItem(context.getHand());
            return InteractionResult.CONSUME;
        }
        if (state.getBlock() instanceof fr.modeminecraft.block.police.LockedDoorBlock) {
            return InteractionResult.PASS;
        }
        if ((state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock) && state.hasProperty(BlockStateProperties.OPEN)) {
            if (!level.isClientSide) {
                boolean open = !state.getValue(BlockStateProperties.OPEN);
                if (state.getBlock() instanceof DoorBlock door) {
                    door.setOpen(context.getPlayer(), level, state, pos, open);
                } else {
                    level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, open), 10);
                }
                level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 0.7f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(context);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return stats.ability() == Ability.CROWBAR ? UseAnim.BRUSH : UseAnim.NONE; // levier ; la parade est animée par le mod
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (stats.ability() == Ability.CROWBAR && entity instanceof Player p && Crowbar.target(p) != null) {
            return Crowbar.PRY_TICKS;
        }
        return hasActiveAbility() ? 0 : 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (stats.ability() == Ability.CROWBAR && entity instanceof Player p && Crowbar.target(p) != null) {
            if (!Crowbar.tick(level, p, Crowbar.PRY_TICKS - remaining)) {
                Crowbar.cancel(p);
                p.stopUsingItem();
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (stats.ability() == Ability.CROWBAR && entity instanceof Player p && Crowbar.target(p) != null) {
            Crowbar.finish(level, p);
            if (!level.isClientSide) {
                stack.hurtAndBreak(1, p, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            }
        }
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player p) {
            Crowbar.cancel(p);
        }
        super.releaseUsing(stack, level, entity, timeLeft);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.melee.stats", fmt(stats.damage()), fmt(stats.speed()), fmt(stats.reach()))
                .withStyle(ChatFormatting.GRAY));
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath();
        tooltip.add(Component.translatable("tooltip.modeminecraft.melee." + id).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(hasActiveAbility() ? "tooltip.modeminecraft.melee.ability" : "tooltip.modeminecraft.melee.parry")
                .withStyle(ChatFormatting.DARK_GRAY));
        if (stats.light()) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.melee.backstab", fmt(stats.backstab())).withStyle(ChatFormatting.DARK_RED));
        }
    }

    private static String fmt(float v) {
        return v == (int) v ? Integer.toString((int) v) : String.format("%.1f", v);
    }
}

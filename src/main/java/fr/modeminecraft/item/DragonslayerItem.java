package fr.modeminecraft.item;

import fr.modeminecraft.registry.ModSounds;
import fr.modeminecraft.registry.ModTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * La Pourfendeuse de Dragons : épée colossale en fer brut (2,4 blocs), portée par le Chef des Troupes de Choc.
 * - 26 PV par coup, attaque balayante sur un arc de 160° et 4 blocs, fort recul, brise les boucliers ;
 * - impossible à enchanter (ni table d'enchantement, ni enclume).
 */
public class DragonslayerItem extends SwordItem {
    public static final float DAMAGE = 26f;
    private static final ResourceLocation REACH = fr.modeminecraft.ModeMinecraft.id("dragonslayer_reach");

    public DragonslayerItem(Properties properties) {
        super(ModTiers.RUBIS, properties.stacksTo(1).fireResistant().attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, DAMAGE - 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH, 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(fr.modeminecraft.ModeMinecraft.id("dragonslayer_weight"), -0.15,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    // --- aucun enchantement possible ---
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return true;
    }

    /** Un joueur frappe : la lame balaie aussi tout ce qui est devant lui. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player p && !p.level().isClientSide && p.getAttackStrengthScale(0.5f) > 0.8f) {
            sweep(attacker, target);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /** Coup du Chef des Troupes de Choc : touche tout l'arc devant lui. */
    public static boolean cleave(LivingEntity attacker) {
        attacker.swing(InteractionHand.MAIN_HAND);
        return sweep(attacker, null) > 0;
    }

    private static int sweep(LivingEntity attacker, LivingEntity already) {
        Vec3 look = Vec3.directionFromRotation(0, attacker.getYRot());
        double cos = Math.cos(Math.toRadians(80));
        int hits = 0;
        for (LivingEntity e : attacker.level().getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(4.2, 1.5, 4.2),
                x -> x != attacker && x != already && x.isAlive() && !x.isAlliedTo(attacker))) {
            Vec3 to = e.position().subtract(attacker.position());
            Vec3 flat = new Vec3(to.x, 0, to.z);
            if (flat.length() > 4.0 + e.getBbWidth() / 2 || flat.length() > 0.3 && flat.normalize().dot(look) < cos) {
                continue;
            }
            var source = attacker instanceof Player p ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker);
            if (e instanceof Player p && p.isBlocking()) {
                p.disableShield();
            }
            if (e.hurt(source, DAMAGE)) {
                e.knockback(1.6, attacker.getX() - e.getX(), attacker.getZ() - e.getZ());
                hits++;
            }
        }
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), ModSounds.DRAGONSLAYER_CLANG.get(),
                SoundSource.HOSTILE, 1.3f, 0.9f + attacker.getRandom().nextFloat() * 0.15f);
        return hits;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.dragonslayer.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.dragonslayer.2").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.modeminecraft.dragonslayer.3").withStyle(ChatFormatting.RED));
    }
}

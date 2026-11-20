package fr.modeminecraft.item;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Casque militaire 3D (cahier des charges "casques"). Ce n'est pas un ArmorItem : Minecraft le dessine alors
 * avec son vrai modèle 3D sur la tête (comme une citrouille), pour les joueurs, les monstres et les supports d'armure.
 *
 * Protection : armure (attribut), réduction des tirs à la tête (BulletEntity), et selon le casque résistance
 * à la mêlée, aux éclats de grenade et aux explosions (HelmetEvents).
 * Module (touche N) : visière blindée (Altyn) ou jumelles de vision nocturne (Ops-Core FAST).
 */
public class HelmetItem extends Item implements Equipable {
    public enum Module { NONE, VISOR, NVG }

    /**
     * @param headshot   réduction des dégâts d'un tir à la tête (0,65 = -65 %)
     * @param headshotModule réduction quand le module est actif (visière abaissée), sinon = headshot
     */
    public record Stats(int tier, double armor, float headshot, float headshotModule, float melee, float shrapnel,
                        float explosion, int durability, Module module, double speed, Supplier<Item> repair) {}

    private final Stats stats;

    public HelmetItem(Stats stats, Properties properties) {
        super(properties.stacksTo(1).durability(stats.durability()).attributes(attributes(stats)));
        this.stats = stats;
    }

    private static ItemAttributeModifiers attributes(Stats s) {
        var b = ItemAttributeModifiers.builder().add(Attributes.ARMOR,
                new AttributeModifier(ModeMinecraft.id("helmet_armor"), s.armor(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.HEAD);
        if (s.speed() != 0) {
            b.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(ModeMinecraft.id("helmet_speed"), s.speed(),
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.HEAD);
        }
        return b.build();
    }

    public Stats stats() {
        return stats;
    }

    /** Le casque porté par cette créature (ou null). */
    public static HelmetItem worn(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof HelmetItem h ? h : null;
    }

    public static boolean isModuleActive(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.HELMET_MODULE.get(), false);
    }

    /** Réduction des tirs à la tête de ce casque (selon la visière). */
    public float headshotReduction(ItemStack stack) {
        return isModuleActive(stack) && stats.module() == Module.VISOR ? stats.headshotModule() : stats.headshot();
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return ModSounds.HELMET_STRAP;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            // Animation d'équipement : les deux bras posent le casque sur la tête
            stack.set(ModDataComponents.HELMET_EQUIP_TIME.get(), level.getGameTime());
            if (level.isClientSide) {
                fr.modeminecraft.client.HelmetClient.startEquip(stack.copy());
            }
        }
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(stats.repair().get());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 10 + stats.tier() * 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath();
        tooltip.add(Component.translatable("tooltip.modeminecraft.helmet.era." + stats.tier()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.modeminecraft.helmet.headshot", Math.round(stats.headshot() * 100))
                .withStyle(ChatFormatting.GRAY));
        if (stats.module() == Module.VISOR) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.helmet.headshot_visor", Math.round(stats.headshotModule() * 100))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (stats.melee() > 0) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.helmet.melee", Math.round(stats.melee() * 100)).withStyle(ChatFormatting.GRAY));
        }
        if (stats.shrapnel() > 0) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.helmet.shrapnel", Math.round(stats.shrapnel() * 100)).withStyle(ChatFormatting.GRAY));
        }
        if (stats.explosion() > 0) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.helmet.explosion", Math.round(stats.explosion() * 100)).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.modeminecraft.helmet." + id).withStyle(ChatFormatting.DARK_AQUA));
        if (stats.module() != Module.NONE) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.helmet.module." + stats.module().name().toLowerCase(),
                    Component.keybind("key.modeminecraft.helmet_module")).withStyle(ChatFormatting.YELLOW));
        }
    }
}

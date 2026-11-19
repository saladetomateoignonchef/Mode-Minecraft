package fr.modeminecraft.item.explosive;

import fr.modeminecraft.entity.PlacedExplosiveEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Détonateur : fait exploser tous ses C4 à moins de 128 blocs. */
public class DetonatorItem extends Item {
    public DetonatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 1f, 1.6f);
        if (!level.isClientSide) {
            var list = level.getEntitiesOfClass(PlacedExplosiveEntity.class, new AABB(player.blockPosition()).inflate(128),
                    e -> e.kind() == PlacedExplosiveEntity.Kind.C4 && e.ownedBy(player));
            list.forEach(PlacedExplosiveEntity::detonate);
            player.displayClientMessage(Component.translatable("message.modeminecraft.detonator", list.size()).withStyle(ChatFormatting.RED), true);
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.detonator").withStyle(ChatFormatting.GOLD));
    }
}

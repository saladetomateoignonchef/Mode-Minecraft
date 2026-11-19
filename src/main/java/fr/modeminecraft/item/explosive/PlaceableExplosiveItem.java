package fr.modeminecraft.item.explosive;

import fr.modeminecraft.entity.PlacedExplosiveEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** C4 (à coller sur un bloc ou une créature), mine Claymore et mortier (à poser au sol). */
public class PlaceableExplosiveItem extends Item {
    private final PlacedExplosiveEntity.Kind kind;

    public PlaceableExplosiveItem(PlacedExplosiveEntity.Kind kind, Properties properties) {
        super(properties.stacksTo(kind == PlacedExplosiveEntity.Kind.MORTAR ? 1 : 8));
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        if (!level.isClientSide) {
            Vec3 pos = context.getClickLocation();
            float yaw = context.getPlayer() == null ? 0 : context.getPlayer().getYRot();
            if (kind != PlacedExplosiveEntity.Kind.C4) {
                pos = new Vec3(pos.x, Math.floor(pos.y + 0.01), pos.z);
            }
            level.addFreshEntity(new PlacedExplosiveEntity(level, kind, pos, yaw, context.getPlayer()));
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** C4 : on peut aussi le coller sur une créature. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (kind != PlacedExplosiveEntity.Kind.C4) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            var c4 = new PlacedExplosiveEntity(player.level(), kind, target.position().add(0, target.getBbHeight() * 0.6, 0), player.getYRot(), player);
            c4.stickTo(target);
            player.level().addFreshEntity(c4);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.explosive." + kind.name().toLowerCase()).withStyle(ChatFormatting.GOLD));
    }
}

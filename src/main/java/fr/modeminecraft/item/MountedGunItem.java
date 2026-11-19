package fr.modeminecraft.item;

import fr.modeminecraft.entity.MountedGunEntity;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Tourelle / mitrailleuse lourde repliée (non empilable). Clic droit sur le dessus d'un bloc solide pour la déployer,
 * orientée dans la direction du regard. Les munitions restantes (démontage) sont gardées dans l'objet.
 */
public class MountedGunItem extends Item {
    private final MountedGunEntity.Kind kind;

    public MountedGunItem(MountedGunEntity.Kind kind, Properties properties) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public MountedGunEntity.Kind kind() {
        return kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (context.getClickedFace() != Direction.UP || !level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) {
            if (context.getPlayer() != null && !level.isClientSide) {
                context.getPlayer().displayClientMessage(Component.translatable("message.modeminecraft.turret.place")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        var hit = context.getClickLocation();
        double y = pos.getY() + 1.0;
        var box = net.minecraft.world.phys.AABB.ofSize(new net.minecraft.world.phys.Vec3(hit.x, y + kind.height / 2 + 0.05, hit.z),
                kind.width, kind.height - 0.1, kind.width);
        if (!level.noCollision(box)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            float yaw = context.getPlayer() == null ? 0 : context.getPlayer().getYRot();
            ItemStack stack = context.getItemInHand();
            int ammo = stack.getOrDefault(ModDataComponents.TURRET_AMMO.get(), 0);
            level.addFreshEntity(new MountedGunEntity(level, kind, hit.x, y, hit.z, yaw, ammo));
            level.playSound(null, hit.x, y, hit.z, ModSounds.TURRET_DEPLOY.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.turret." + kind.id).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.modeminecraft.turret.stats", kind.projectile == MountedGunEntity.Projectile.GRENADE
                        ? "22 (3,5 blocs)" : String.valueOf(kind.damage), Math.round(kind.rpm), kind.capacity)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.turret.ammo", kind.ammo().getDescription()).withStyle(ChatFormatting.GRAY));
        int ammo = stack.getOrDefault(ModDataComponents.TURRET_AMMO.get(), 0);
        tooltip.add(Component.translatable("tooltip.modeminecraft.turret.loaded", ammo, kind.capacity)
                .withStyle(ammo > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.modeminecraft.turret.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}

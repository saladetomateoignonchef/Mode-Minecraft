package fr.modeminecraft.item.military;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Désignateur laser : clic droit pour marquer une position jusqu'à 1000 blocs. Les roquettes guidées GPS (HIMARS),
 * les missiles balistiques (Iskander) et les frappes d'artillerie de vos véhicules visent la dernière marque
 * (valable 5 minutes). Le point marqué est signalé par un éclat rouge visible de loin.
 */
public class LaserDesignatorItem extends Item {
    private record Mark(Vec3 pos, long time) {}

    private static final Map<UUID, Mark> MARKS = new ConcurrentHashMap<>();

    public LaserDesignatorItem(Properties p) {
        super(p.stacksTo(1));
    }

    /** Dernière marque valable de ce tireur (ou null). */
    public static Vec3 mark(Entity shooter) {
        if (shooter == null) {
            return null;
        }
        Mark m = MARKS.get(shooter.getUUID());
        return m != null && shooter.level().getGameTime() - m.time < 6000 ? m.pos : null;
    }

    public static void setMark(Entity e, Vec3 pos) {
        MARKS.put(e.getUUID(), new Mark(pos, e.level().getGameTime()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(1000));
        var hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        Vec3 p = hit.getLocation();
        setMark(player, p);                                         // aussi côté client (console de l'Iskander)
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(new DustParticleOptions(new Vector3f(1, 0.05f, 0.05f), 2.5f), p.x, p.y + 0.2, p.z, 30, 0.3, 0.3, 0.3, 0);
            player.displayClientMessage(Component.translatable("message.modeminecraft.designator.marked", (int) p.x, (int) p.y, (int) p.z,
                    (int) eye.distanceTo(p)).withStyle(ChatFormatting.RED), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.6f, 2f);
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext c, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.laser_designator").withStyle(ChatFormatting.GRAY));
    }
}

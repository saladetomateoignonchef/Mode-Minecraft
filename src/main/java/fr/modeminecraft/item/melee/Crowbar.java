package fr.modeminecraft.item.melee;

import fr.modeminecraft.block.police.LockedDoorBlock;
import fr.modeminecraft.block.police.PoliceLockerBlock;
import fr.modeminecraft.block.police.SlidingJailDoorBlock;
import fr.modeminecraft.entity.bot.BotEvents;
import fr.modeminecraft.entity.bot.BotGoals;
import fr.modeminecraft.entity.bot.FactionRelations;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Effraction au pied-de-biche : clic droit maintenu 2,5 s sur une porte verrouillée, un casier de police, une porte de
 * cellule (depuis le couloir) ou une caisse enregistreuse ; le bras fait levier (animation), le métal grince. Si un
 * policier voit la scène, c'est un flagrant délit : la police devient hostile.
 */
public final class Crowbar {
    public static final int PRY_TICKS = 50;
    private static final Map<UUID, BlockPos> TARGET = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> TARGET_CLIENT = new ConcurrentHashMap<>();

    private static Map<UUID, BlockPos> map(Level level) {
        return level.isClientSide ? TARGET_CLIENT : TARGET;
    }

    /** Ce bloc se force-t-il au pied-de-biche ? */
    public static boolean pryable(Level level, BlockPos pos, Player player) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof LockedDoorBlock || s.getBlock() instanceof PoliceLockerBlock) {
            return s.getValue(LockedDoorBlock.LOCKED);
        }
        if (s.getBlock() instanceof SlidingJailDoorBlock) {
            return s.getValue(SlidingJailDoorBlock.LOCKED) && SlidingJailDoorBlock.outside(s, pos, player);
        }
        return s.getBlock() instanceof fr.modeminecraft.block.city.CashRegisterBlock && !s.getValue(fr.modeminecraft.block.city.CashRegisterBlock.OPEN);
    }

    /** Un pied-de-biche en main : les blocs verrouillés laissent l'outil agir (au lieu de leur propre clic droit). */
    public static boolean isCrowbar(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof MeleeWeaponItem m && m.stats().ability() == MeleeWeaponItem.Ability.CROWBAR;
    }

    public static net.minecraft.world.ItemInteractionResult onBlock(net.minecraft.world.item.ItemStack stack, Level level, BlockPos pos, Player player) {
        return isCrowbar(stack) && pryable(level, pos, player) ? net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                : net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static void begin(Player player, BlockPos pos) {
        map(player.level()).put(player.getUUID(), pos.immutable());
        if (player.level() instanceof ServerLevel sl) {
            witness(sl, player);
        }
    }

    public static BlockPos target(Player player) {
        return map(player.level()).get(player.getUUID());
    }

    public static void cancel(Player player) {
        map(player.level()).remove(player.getUUID());
    }

    /** Pendant l'effort : grincements, éclats, et vérification qu'on vise toujours le même bloc. */
    public static boolean tick(Level level, Player player, int used) {
        BlockPos pos = target(player);
        if (pos == null || player.distanceToSqr(pos.getCenter()) > 20 || !pryable(level, pos, player)) {
            return false;
        }
        if (!level.isClientSide && used % 10 == 0) {
            level.playSound(null, pos, ModSounds.CROWBAR_PRY.get(), SoundSource.PLAYERS, 0.9f, 0.85f + level.random.nextFloat() * 0.3f);
            ((ServerLevel) level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(pos)),
                    pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.05);
            if (used % 20 == 0) {
                witness((ServerLevel) level, player);
            }
        }
        return true;
    }

    /** Fin de l'effort : la serrure cède. */
    public static void finish(Level level, Player player) {
        BlockPos pos = target(player);
        cancel(player);
        if (pos == null || level.isClientSide || !pryable(level, pos, player)) {
            return;
        }
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof LockedDoorBlock) {
            LockedDoorBlock.force(level, pos);
        } else if (s.getBlock() instanceof PoliceLockerBlock) {
            if (s.getBlock() instanceof fr.modeminecraft.scenario.BarracksLockerBlock && player instanceof net.minecraft.server.level.ServerPlayer sp
                    && level.getBlockEntity(pos) instanceof fr.modeminecraft.scenario.BarracksLockerBlockEntity be && be.owner != null && !be.owner.equals(sp.getUUID())) {
                fr.modeminecraft.scenario.Reputation.add(sp, -50, "rep.modeminecraft.stole");    // vol dans sa propre base
            }
            PoliceLockerBlock.force(level, pos, player);
        } else if (s.getBlock() instanceof SlidingJailDoorBlock) {
            fr.modeminecraft.block.police.JailDoorBlockEntity.release(level, SlidingJailDoorBlock.lower(s, pos));
        } else if (s.getBlock() instanceof fr.modeminecraft.block.city.CashRegisterBlock) {
            fr.modeminecraft.block.city.CashRegisterBlock.burst(level, pos, player);
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1f, 0.6f);
        player.displayClientMessage(Component.translatable("message.modeminecraft.crowbar.forced").withStyle(ChatFormatting.GOLD), true);
        witness((ServerLevel) level, player);
    }

    /** Flagrant délit : tout policier qui voit l'effraction passe en mode arrestation. */
    public static void witness(ServerLevel level, Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        long now = level.getGameTime();
        for (HumanBotEntity bot : level.getEntitiesOfClass(HumanBotEntity.class, player.getBoundingBox().inflate(24),
                b -> b.isAlive() && (b.faction().group.equals("police") || b.faction().group.equals("swat")))) {
            if (fr.modeminecraft.entity.bot.RadioCalls.sees(bot, player)) {
                if (!FactionRelations.isHostile(player.getUUID(), bot.faction(), now)) {
                    player.displayClientMessage(Component.translatable("message.modeminecraft.crowbar.caught").withStyle(ChatFormatting.RED), true);
                }
                fr.modeminecraft.entity.bot.RadioCalls.witness(bot, player);    // effraction vue : appel radio
            }
        }
    }

    private Crowbar() {}
}

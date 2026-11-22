package fr.modeminecraft.scenario;

import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Terminal des plans (salle de stratégie de chaque base) : en mission d'espionnage, un espion en uniforme complet de
 * l'ennemi y télécharge les plans secrets ; il doit ensuite quitter la base et les remettre à son recruteur.
 */
public class PlansTerminalBlock extends fr.modeminecraft.block.police.FacingDecoBlock {
    public PlansTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        var city = CityPlanner.cityAtCached(CityPlanner.Env.of(sl), pos.getX(), pos.getZ(), 8);
        Side side = city == null ? null : city.variant().equals("base") ? Side.US : city.variant().equals("soviet") ? Side.SOVIET : null;
        if (Missions.stealPlans(sp, side)) {
            level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1f, 1.4f);
        } else {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.6f, 0.6f);
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.modeminecraft.terminal.locked"), true);
        }
        return InteractionResult.CONSUME;
    }
}

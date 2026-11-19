package fr.modeminecraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Bloc "établi" générique : un clic droit ouvre une interface (comme la table de craft).
 * Utilisé par la table d'armement et la table de modification.
 */
public class WorkbenchBlock extends Block {
    @FunctionalInterface
    public interface MenuFactory {
        AbstractContainerMenu create(int containerId, Inventory inventory, ContainerLevelAccess access);
    }

    private final MenuFactory menuFactory;
    private final Component title;

    public WorkbenchBlock(Properties properties, Component title, MenuFactory menuFactory) {
        super(properties);
        this.title = title;
        this.menuFactory = menuFactory;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) ->
                menuFactory.create(id, inventory, ContainerLevelAccess.create(level, pos)), title);
    }
}

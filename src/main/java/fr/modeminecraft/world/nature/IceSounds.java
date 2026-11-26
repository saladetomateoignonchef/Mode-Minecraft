package fr.modeminecraft.world.nature;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * [48] Craquement de la glace : quand un joueur marche sur de la glace (glace, glace compacte, glace bleue, glace givrée) dans un biome froid,
 * on entend de temps en temps un bruit sec et cristallin (plus souvent en courant).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class IceSounds {
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || !p.onGround() || p.isSpectator() || p.isCrouching()) {
            return;
        }
        int every = p.isSprinting() ? 8 : 14;
        if (p.tickCount % every != 0 || p.getDeltaMovement().horizontalDistanceSqr() < 0.0009) {
            return;
        }
        BlockPos below = p.getBlockPosBelowThatAffectsMyMovement();
        BlockState state = p.level().getBlockState(below);
        if (!state.is(BlockTags.ICE) || p.level().getBiome(below).value().getBaseTemperature() > 0.15f) {
            return;
        }
        if (p.getRandom().nextFloat() < 0.65f) {
            p.level().playSound(null, below, ModSounds.ICE_CRACK.get(), SoundSource.BLOCKS, 0.55f, 0.85f + p.getRandom().nextFloat() * 0.35f);
        }
    }

    private IceSounds() {}
}

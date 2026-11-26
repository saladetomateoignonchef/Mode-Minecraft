package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * [49] Ruissellement des petits ruisseaux : à moins de 10 blocs d'une eau qui coule en pente (eau non source avec de l'eau qui tombe dessous ou
 * un niveau plus bas à côté), on entend un son d'eau vive spatialisé, répété de temps en temps (jamais plus d'une source à la fois).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class StreamSounds {
    private static int cooldown;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (--cooldown > 0) {
            return;
        }
        cooldown = 20;
        RandomSource rnd = level.random;
        BlockPos center = mc.player.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 24; i++) {
            m.set(center.getX() + rnd.nextInt(21) - 10, center.getY() + rnd.nextInt(9) - 4, center.getZ() + rnd.nextInt(21) - 10);
            FluidState fluid = level.getFluidState(m);
            if (!fluid.is(FluidTags.WATER) || fluid.isSource()) {
                continue;
            }
            boolean falling = level.getFluidState(m.below()).is(FluidTags.WATER);
            boolean slope = false;
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                FluidState side = level.getFluidState(m.relative(d));
                if (side.is(FluidTags.WATER) && !side.isSource() && side.getAmount() < fluid.getAmount()) {
                    slope = true;
                }
            }
            if (falling || slope) {
                level.playLocalSound(m.getX() + 0.5, m.getY() + 0.5, m.getZ() + 0.5, ModSounds.STREAM_FLOW.get(), SoundSource.AMBIENT, 0.55f, 0.9f + rnd.nextFloat() * 0.2f, false);
                cooldown = 70 + rnd.nextInt(40);                    // le son dure 4 s : on laisse finir avant d'en lancer un autre
                return;
            }
        }
    }

    private StreamSounds() {}
}

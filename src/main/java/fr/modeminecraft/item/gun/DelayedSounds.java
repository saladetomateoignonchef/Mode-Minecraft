package fr.modeminecraft.item.gun;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/** Sons joués un peu plus tard (ex. le chargeur qui s'enclenche à la fin du rechargement). */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class DelayedSounds {
    private record Pending(Entity source, Supplier<SoundEvent> sound, float volume, float pitch, long at) {}

    private static final List<Pending> PENDING = new ArrayList<>();

    /** Joue le son à la position de source dans delay ticks (côté serveur uniquement). */
    public static void play(Entity source, Supplier<SoundEvent> sound, float volume, float pitch, int delay) {
        if (source.level() instanceof ServerLevel level) {
            if (delay <= 0) {
                level.playSound(null, source.getX(), source.getY(), source.getZ(), sound.get(), SoundSource.PLAYERS, volume, pitch);
            } else {
                synchronized (PENDING) {
                    PENDING.add(new Pending(source, sound, volume, pitch, level.getGameTime() + delay));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        synchronized (PENDING) {
            Iterator<Pending> it = PENDING.iterator();
            while (it.hasNext()) {
                Pending p = it.next();
                Entity e = p.source();
                if (e.isRemoved()) {
                    it.remove();
                } else if (e.level().getGameTime() >= p.at()) {
                    e.level().playSound(null, e.getX(), e.getY(), e.getZ(), p.sound().get(), SoundSource.PLAYERS, p.volume(), p.pitch());
                    it.remove();
                }
            }
        }
    }

    private DelayedSounds() {}
}

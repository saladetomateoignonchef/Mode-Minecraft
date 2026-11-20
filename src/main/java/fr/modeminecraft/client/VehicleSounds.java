package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.VehicleEntity;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Bruit de moteur des véhicules : un son en boucle qui suit chaque véhicule conduit ; le régime (hauteur du son)
 * et le volume montent avec la vitesse. Quad aigu, voiture de police et jeep moyens, camion blindé grave (diesel).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class VehicleSounds {
    private static final Map<Integer, Engine> PLAYING = new HashMap<>();

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            PLAYING.clear();
            return;
        }
        PLAYING.values().removeIf(Engine::isStopped);
        for (VehicleEntity v : mc.level.getEntitiesOfClass(VehicleEntity.class, mc.player.getBoundingBox().inflate(48))) {
            if (v.getControllingPassenger() != null && !PLAYING.containsKey(v.getId())) {
                Engine e = new Engine(v);
                PLAYING.put(v.getId(), e);
                mc.getSoundManager().play(e);
            }
        }
    }

    static final class Engine extends AbstractTickableSoundInstance {
        private final VehicleEntity vehicle;
        private final float base;

        Engine(VehicleEntity v) {
            super(ModSounds.VEHICLE_ENGINE.get(), SoundSource.NEUTRAL, RandomSource.create());
            this.vehicle = v;
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.base = switch (v.type()) {
                case QUAD -> 1.45f;
                case POLICE_CAR -> 1.1f;
                case JEEP -> 0.95f;
                case ARMORED_TRUCK -> 0.62f;
            };
            this.volume = 0.5f;
            this.pitch = base;
            this.x = v.getX();
            this.y = v.getY();
            this.z = v.getZ();
        }

        @Override
        public void tick() {
            if (vehicle.isRemoved() || vehicle.getControllingPassenger() == null) {
                stop();
                return;
            }
            x = vehicle.getX();
            y = vehicle.getY();
            z = vehicle.getZ();
            float speed = Mth.clamp(Math.abs(vehicle.speed()) / vehicle.type().maxSpeed, 0, 1);
            pitch = base * (0.8f + speed * 0.75f);
            volume = 0.45f + speed * 0.75f;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }

    private VehicleSounds() {}
}

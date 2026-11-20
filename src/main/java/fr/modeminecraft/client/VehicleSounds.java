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
    static final Map<Integer, Rotors> DRONES = new HashMap<>();

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            PLAYING.clear();
            DRONES.clear();
            return;
        }
        DRONES.values().removeIf(Rotors::isStopped);
        for (var d : mc.level.getEntitiesOfClass(fr.modeminecraft.entity.drone.DroneEntity.class, mc.player.getBoundingBox().inflate(40))) {
            if (d.rotorsRunning() && !DRONES.containsKey(d.getId())) {
                Rotors r = new Rotors(d);
                DRONES.put(d.getId(), r);
                mc.getSoundManager().play(r);
            }
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

    /**
     * Hélices d'un drone : boucle parfaite (drone_rotor_loop), fondu d'entrée et de sortie, hauteur selon la vitesse ;
     * on n'entend donc jamais le début ni la fin du fichier. Hélices silencieuses : -70 %.
     */
    static final class Rotors extends AbstractTickableSoundInstance {
        private final fr.modeminecraft.entity.drone.DroneEntity drone;
        private final float base;
        private float fade;
        private boolean ending;

        Rotors(fr.modeminecraft.entity.drone.DroneEntity d) {
            super(ModSounds.DRONE_ROTOR_LOOP.get(), SoundSource.NEUTRAL, RandomSource.create());
            this.drone = d;
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.base = switch (d.droneType()) {
                case HORNET -> 1.9f;
                case FPV -> 1.5f;
                case REAPER -> 0.7f;
                default -> 1.0f;
            };
            this.volume = 0.01f;
            this.pitch = base;
            this.x = d.getX();
            this.y = d.getY();
            this.z = d.getZ();
        }

        @Override
        public void tick() {
            if (drone.isRemoved() || !drone.rotorsRunning()) {
                ending = true;
            }
            fade = Mth.clamp(fade + (ending ? -0.1f : 0.08f), 0, 1);
            if (ending && fade <= 0) {
                stop();
                return;
            }
            x = drone.getX();
            y = drone.getY();
            z = drone.getZ();
            float speed = (float) Mth.clamp(drone.getDeltaMovement().length() / Math.max(0.1, drone.droneType().speed), 0, 1);
            pitch = base * (0.88f + speed * 0.3f);
            float loud = (float) Mth.clamp(drone.noiseRadius() / 16.0, 0.12, 1.6);
            volume = Math.max(0.001f, fade * loud * (0.75f + speed * 0.35f));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }

    private VehicleSounds() {}
}

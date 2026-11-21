package fr.modeminecraft.entity.military;

import fr.modeminecraft.registry.ModSounds;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Alerte aérienne (idée du QCM) : quand un missile ou une salve de roquettes arrive sur une ville, les sirènes
 * hurlent dans toute la ville et les civils courent se mettre à l'abri (hall des immeubles, magasins).
 */
public final class AirRaid {
    public static void alert(ServerLevel level, Vec3 target, double radius) {
        var city = CityPlanner.cityAtCached(CityPlanner.Env.of(level), (int) target.x, (int) target.z, 64);
        Vec3 center = city != null ? new Vec3(city.middleX(), city.baseY(), city.middleZ()) : target;
        double r = city != null ? Math.max(radius, city.rad() * 16 + 40) : radius;
        // sirènes : plusieurs points autour du centre, entendues de loin
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i;
            level.playSound(null, center.x + Math.cos(a) * r * 0.4, center.y + 10, center.z + Math.sin(a) * r * 0.4,
                    ModSounds.AIR_RAID_ALARM.get(), SoundSource.AMBIENT, 12f, 1f);
        }
        fr.modeminecraft.entity.bot.CivilianLife.panic(level, center, r, 900);
    }

    private AirRaid() {}
}

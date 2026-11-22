package fr.modeminecraft.scenario;

import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Véhicules de sa propre base selon le grade : Sergent (blindés légers à roues : Humvee, Tigr, UAZ…), Lieutenant
 * (chars, transports de troupes, hélicoptères, lance-missiles, chasseurs légers), Commandant (chasseurs lourds,
 * bombardiers, navires de guerre). Les véhicules capturés ou fabriqués ailleurs sont libres.
 */
public final class VehicleAccess {
    private VehicleAccess() {}

    public static Grade required(MilType t) {
        if (t.naval() || t.plane() && (t.heavy() || t.health >= 900)) {
            return Grade.MAJOR;
        }
        if (t.chassis == MilType.Chassis.WHEELS && t.health < 200 && !t.has(MilType.Flag.STRATEGIC)) {
            return Grade.SERGEANT;
        }
        return Grade.LIEUTENANT;
    }

    public static boolean allowed(ServerPlayer p, MilVehicleEntity v) {
        Side s = Scenario.side(p);
        if (s == null || p.isCreative() || v.type().side != s.vehicles) {
            return true;
        }
        CityPlanner.City c = Quarters.base(p);
        if (c == null || v.getX() < c.minX() || v.getX() > c.maxX() || v.getZ() < c.minZ() || v.getZ() > c.maxZ()) {
            return true;                                                        // hors de la base : libre
        }
        Grade need = required(v.type());
        if (Reputation.grade(p).atLeast(need)) {
            return true;
        }
        p.displayClientMessage(Component.translatable("message.modeminecraft.vehicle.rank", Component.translatable(need.key(s))).withStyle(ChatFormatting.RED), true);
        return false;
    }
}

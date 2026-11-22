package fr.modeminecraft.entity.military;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Patrouilles navales en haute mer : toutes les 30 s, un joueur au large (sur l'eau, loin des côtes) a une chance de
 * croiser une patrouille à ~250 blocs — mercenaires (semi-rigides et intercepteur, hostiles), marine soviétique en eaux
 * froides, US Navy en eaux tempérées. Il faut une étendue d'eau libre de 140 x 140 blocs et 12 de fond au point
 * d'apparition. Les patrouilles disparaissent quand plus aucun joueur n'est à moins de 420 blocs.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class NavalPatrols {
    public static float CHANCE = 0.12f;

    private NavalPatrols() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        if (now % 600 != 0 || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        int sea = level.getSeaLevel();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || level.random.nextFloat() >= CHANCE || p.getY() < sea - 30 || p.getY() > sea + 40) {
                continue;
            }
            BlockPos at = p.blockPosition();
            if (floor(level, at.getX(), at.getZ()) > sea - 8) {
                continue;                                                   // pas au large
            }
            long ships = level.getEntitiesOfClass(MilVehicleEntity.class, p.getBoundingBox().inflate(500, 100, 500),
                    v -> v.type().naval() && v.patrol).size();
            if (ships >= 3) {
                continue;
            }
            double a = level.random.nextDouble() * Math.PI * 2;
            int x = (int) (p.getX() + Math.cos(a) * 250), z = (int) (p.getZ() + Math.sin(a) * 250);
            if (!openWater(level, x, z, sea)) {
                continue;
            }
            float yaw = (float) Math.toDegrees(Math.atan2(-(p.getX() - x), p.getZ() - z));
            boolean cold = level.getBiome(new BlockPos(x, sea, z)).value().getBaseTemperature() < 0.3f;
            float roll = level.random.nextFloat();
            double surf = sea - 0.1;
            if (roll < 0.4f) {
                Fleets.launch(level, MilType.RHIB, x, surf, z, yaw, true);
                Fleets.launch(level, MilType.INTERCEPTOR_BOAT, x + 12, surf, z + 8, yaw, true);
                if (level.random.nextBoolean()) {
                    Fleets.launch(level, MilType.RHIB, x - 10, surf, z + 14, yaw, true);
                }
            } else if (cold || roll < 0.7f) {
                MilType t = level.random.nextFloat() < 0.5f ? MilType.UDALOY : MilType.BUYAN;
                Fleets.launch(level, t, x, surf, z, yaw, true);
            } else {
                MilType t = level.random.nextFloat() < 0.6f ? MilType.ARLEIGH_BURKE : MilType.VIRGINIA;
                Fleets.launch(level, t, x, surf, z, yaw, true);
            }
            ModeMinecraft.LOGGER.info("Patrouille navale près de {} en {} {}", p.getName().getString(), x, z);
        }
    }

    private static int floor(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
    }

    private static boolean openWater(ServerLevel level, int x, int z, int sea) {
        for (int dx = -70; dx <= 70; dx += 35) {
            for (int dz = -70; dz <= 70; dz += 35) {
                if (!level.hasChunk((x + dx) >> 4, (z + dz) >> 4) || floor(level, x + dx, z + dz) > sea - 12) {
                    return false;
                }
            }
        }
        return true;
    }
}

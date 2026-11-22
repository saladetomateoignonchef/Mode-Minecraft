package fr.modeminecraft.entity.train;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.world.city.CityPlanner;
import fr.modeminecraft.world.rail.RailNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Met en service le train de chaque ville frontière du Mesa dès qu'un joueur s'en approche (une seule fois par gare, même
 * si le train est détruit ensuite) : locomotive, 2 ou 3 wagons, conducteur PNJ et deux contrôleurs. Il circule ensuite de
 * ville en ville tant que les chunks de sa voie sont chargés.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class TrainManager {
    private TrainManager() {}

    /** Gares dont le train a déjà été mis en service (ou qui n'ont aucune ligne). */
    public static final class Data extends SavedData {
        final Set<String> done = new HashSet<>();

        static Data load(CompoundTag tag, HolderLookup.Provider reg) {
            Data d = new Data();
            ListTag l = tag.getList("Done", 8);
            for (int i = 0; i < l.size(); i++) {
                d.done.add(l.getString(i));
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
            ListTag l = new ListTag();
            for (String s : done) {
                l.add(StringTag.valueOf(s));
            }
            tag.put("Done", l);
            return tag;
        }
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), "mesa_trains");
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 100 != 17) {
            return;
        }
        ServerLevel level = server.overworld();
        if (CityPlanner.placement(level.registryAccess()) == null) {
            return;
        }
        CityPlanner.Env env = CityPlanner.Env.of(level);
        for (ServerPlayer p : level.players()) {
            CityPlanner.City city = CityPlanner.cityAtCached(env, p.getBlockX(), p.getBlockZ(), 150);
            if (!RailNetwork.station(city)) {
                continue;
            }
            Data d = data(level);
            String key = city.rx() + ":" + city.rz();
            if (d.done.contains(key) || !stationReady(level, city)) {
                continue;
            }
            d.done.add(key);
            d.setDirty();
            if (!RailNetwork.links(env, city).isEmpty()) {
                spawn(level, city);
            }
        }
    }

    /** La gare est construite (rails posés) et ses chunks sont chargés sur 50 blocs de chaque côté. */
    private static boolean stationReady(ServerLevel level, CityPlanner.City city) {
        double z = RailNetwork.railZ(city);
        for (int dx = -48; dx <= 48; dx += 16) {
            if (!level.hasChunkAt(BlockPos.containing(city.middleX() + dx, city.baseY(), z))) {
                return false;
            }
        }
        return level.getBlockState(BlockPos.containing(city.middleX(), city.baseY(), z)).is(Blocks.RAIL);
    }

    public static void spawn(ServerLevel level, CityPlanner.City city) {
        UUID id = UUID.randomUUID();
        int wagons = 2 + (int) ((city.seed() >>> 7) & 1);
        double z = RailNetwork.railZ(city), x = city.middleX() + 0.5;
        MesaTrainCarEntity loco = MesaTrainCarEntity.create(level, MesaTrainCarEntity.LOCOMOTIVE, id, 0);
        loco.setWagonCount(wagons);
        loco.startAt(city);
        loco.moveTo(x, city.baseY() + 0.0625, z, 90, 0);
        level.addFreshEntity(loco);
        List<MesaTrainCarEntity> cars = new ArrayList<>();
        for (int i = 1; i <= wagons; i++) {
            MesaTrainCarEntity w = MesaTrainCarEntity.create(level, MesaTrainCarEntity.WAGON, id, i);
            w.setWagonCount(wagons);
            w.moveTo(x, city.baseY() + 0.0625, z, 90, 0);
            level.addFreshEntity(w);
            cars.add(w);
        }
        TrainStaff.crew(level, loco, cars);
    }
}

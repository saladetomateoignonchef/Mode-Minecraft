package fr.modeminecraft.scenario;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Progression du scénario, par joueur (camp, réputation, base, lit, casier, missions…), sauvegardée avec le monde. */
public class ScenarioData extends SavedData {
    private static final String NAME = "modeminecraft_scenario";

    /** Dossier militaire d'un joueur. */
    public static final class Record {
        public Side side;
        public int reputation;
        /** Base principale (centre), lit du dortoir, casier personnel. */
        public BlockPos base, bed, locker;
        /** Données libres des phases suivantes (missions, grades…). */
        public CompoundTag extra = new CompoundTag();

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            if (side != null) {
                t.putString("Side", side.id);
            }
            t.putInt("Rep", reputation);
            if (base != null) {
                t.putLong("Base", base.asLong());
            }
            if (bed != null) {
                t.putLong("Bed", bed.asLong());
            }
            if (locker != null) {
                t.putLong("Locker", locker.asLong());
            }
            t.put("Extra", extra);
            return t;
        }

        static Record load(CompoundTag t) {
            Record r = new Record();
            r.side = Side.byId(t.getString("Side"));
            r.reputation = t.getInt("Rep");
            r.base = t.contains("Base") ? BlockPos.of(t.getLong("Base")) : null;
            r.bed = t.contains("Bed") ? BlockPos.of(t.getLong("Bed")) : null;
            r.locker = t.contains("Locker") ? BlockPos.of(t.getLong("Locker")) : null;
            r.extra = t.getCompound("Extra");
            return r;
        }
    }

    private final Map<UUID, Record> players = new HashMap<>();
    /** État global : avancement de la bombe adverse, victoire… (phases suivantes). */
    public CompoundTag global = new CompoundTag();

    public static ScenarioData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ScenarioData::new, ScenarioData::load, null), NAME);
    }

    public Record of(UUID id) {
        return players.computeIfAbsent(id, k -> new Record());
    }

    public Record peek(UUID id) {
        return players.get(id);
    }

    public Map<UUID, Record> all() {
        return players;
    }

    private static ScenarioData load(CompoundTag tag, HolderLookup.Provider p) {
        ScenarioData d = new ScenarioData();
        ListTag list = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            d.players.put(t.getUUID("Id"), Record.load(t));
        }
        d.global = tag.getCompound("Global");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        ListTag list = new ListTag();
        for (var e : players.entrySet()) {
            CompoundTag t = e.getValue().save();
            t.putUUID("Id", e.getKey());
            list.add(t);
        }
        tag.put("Players", list);
        tag.put("Global", global);
        return tag;
    }
}

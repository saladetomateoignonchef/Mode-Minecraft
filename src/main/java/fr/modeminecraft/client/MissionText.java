package fr.modeminecraft.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Libellés des entrées du journal (missions et petites tâches). */
final class MissionText {
    private MissionText() {}

    static Component title(CompoundTag e) {
        String id = e.getString("Id");
        if (id.equals("task")) {
            if (e.getString("Kind").equals("patrol")) {
                return Component.translatable("journal.modeminecraft.task.patrol", e.getInt("Goal"));
            }
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.getString("Item")));
            return Component.translatable("journal.modeminecraft.task.deliver", e.getInt("Goal"), item.getDescription());
        }
        if (id.equals("intel")) {
            return Component.translatable("journal.modeminecraft.intel", Component.translatable("gui.modeminecraft.map." + e.getString("Kind")));
        }
        if (id.equals("contract")) {
            return Component.translatable("journal.modeminecraft.contract." + e.getString("Kind"),
                    Component.translatable("entity.modeminecraft.bot." + e.getString("Faction")));
        }
        return Component.translatable("mission.modeminecraft." + id);
    }

    static Component objective(CompoundTag e) {
        String id = e.getString("Id");
        if (id.equals("task")) {
            return Component.translatable("journal.modeminecraft.task.giver", e.getString("GiverName"), e.getInt("Progress"), e.getInt("Goal"));
        }
        if (id.equals("intel")) {
            return Component.translatable("journal.modeminecraft.intel.objective", e.getInt("TX"), e.getInt("TZ"));
        }
        if (id.equals("contract")) {
            return e.getInt("Progress") >= e.getInt("Goal") ? Component.translatable("journal.modeminecraft.contract.claim")
                    : Component.translatable("journal.modeminecraft.contract.progress", e.getInt("Progress"), e.getInt("Goal"));
        }
        String stage = id.equals("scientist") && e.getBoolean("Freed") ? ".escort" : id.equals("espionage") && e.getInt("Progress") > 0 ? ".report" : "";
        return Component.translatable("mission.modeminecraft." + id + ".objective" + stage, e.getInt("Progress"), e.getInt("Goal"));
    }

    /** Distance et direction de la cible (« 230 m NE »), ou vide. */
    static String where(CompoundTag e) {
        var p = net.minecraft.client.Minecraft.getInstance().player;
        if (p == null || !e.getBoolean("HasTarget")) {
            return "";
        }
        double dx = e.getInt("TX") + 0.5 - p.getX(), dz = e.getInt("TZ") + 0.5 - p.getZ();
        int d = (int) Math.hypot(dx, dz);
        String[] dirs = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};
        int k = Math.floorMod(Math.round((float) (Math.toDegrees(Math.atan2(-dx, dz)) / 45)), 8);
        return d + " m " + dirs[k];
    }
}

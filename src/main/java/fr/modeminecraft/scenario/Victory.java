package fr.modeminecraft.scenario;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Victoire de la Course à l'arme nucléaire (QG ennemi rayé de la carte) : annonce, puis la partie continue librement. */
public final class Victory {
    private Victory() {}

    public static void win(ServerPlayer p) {
        ScenarioData data = ScenarioData.get(p.serverLevel());
        Side s = Scenario.side(p);
        data.global.putString("Winner", s == null ? "" : s.id);
        data.global.putLong("WonAt", p.level().getGameTime());
        data.setDirty();
        for (ServerPlayer o : p.getServer().getPlayerList().getPlayers()) {
            o.sendSystemMessage(Component.translatable("message.modeminecraft.scenario.victory", p.getName(),
                    Component.translatable("gui.modeminecraft.faction.name_" + (s == null ? "us" : s.id))).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new ScenarioPayloads.VictoryScreen(s == null ? "us" : s.id,
                Scenario.record(p).reputation, (int) (p.level().getGameTime() / 24000)));
    }
}

package fr.modeminecraft.scenario;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Réputation et grade : gains (missions, petites tâches), pertes (tirs sur un allié, vol dans sa propre base),
 * promotions et dégradations (message, logement réattribué, insigne de grade devant le nom au-dessus de la tête et
 * dans la liste des joueurs, par une équipe du tableau des scores).
 */
public final class Reputation {
    private Reputation() {}

    public static Grade grade(ServerPlayer p) {
        return Grade.of(Scenario.record(p).reputation);
    }

    public static void add(ServerPlayer p, int amount, String reasonKey) {
        ScenarioData.Record r = Scenario.record(p);
        if (r.side == null || amount == 0) {
            return;
        }
        Grade before = Grade.of(r.reputation);
        r.reputation = Math.max(0, r.reputation + amount);
        ScenarioData.get(p.serverLevel()).setDirty();
        Grade after = Grade.of(r.reputation);
        p.displayClientMessage(Component.translatable(amount > 0 ? "message.modeminecraft.rep.gain" : "message.modeminecraft.rep.loss",
                Math.abs(amount), Component.translatable(reasonKey)).withStyle(amount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        if (after != before) {
            boolean up = after.ordinal() > before.ordinal();
            p.sendSystemMessage(Component.translatable(up ? "message.modeminecraft.rep.promoted" : "message.modeminecraft.rep.demoted",
                    Component.translatable(after.key(r.side))).withStyle(up ? ChatFormatting.GOLD : ChatFormatting.DARK_RED));
            p.level().playSound(null, p.blockPosition(), up ? SoundEvents.PLAYER_LEVELUP : SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1f, up ? 0.8f : 1f);
            Quarters.reassign(p, after);
            nameplate(p);
            // dotations de grade : talkie-walkie de commandement (Lieutenant), mallette de frappe (Général)
            gift(p, r, up && after.atLeast(Grade.LIEUTENANT), "GiftRadio", fr.modeminecraft.registry.ModItems.COMMAND_RADIO.get());
            gift(p, r, up && after == Grade.GENERAL, "GiftBriefcase", fr.modeminecraft.registry.ModItems.STRATEGIC_BRIEFCASE.get());
        }
        Scenario.sync(p);
    }

    private static void gift(ServerPlayer p, ScenarioData.Record r, boolean due, String key, net.minecraft.world.item.Item item) {
        if (due && !r.extra.getBoolean(key)) {
            r.extra.putBoolean(key, true);
            Missions.give(p, new net.minecraft.world.item.ItemStack(item));
            p.sendSystemMessage(Component.translatable("message.modeminecraft.rep.gift", item.getDescription()).withStyle(ChatFormatting.GOLD));
        }
    }

    /** Insigne de grade devant le nom (équipe « mm_<camp>_<grade> » du tableau des scores). */
    public static void nameplate(ServerPlayer p) {
        ScenarioData.Record r = Scenario.record(p);
        Scoreboard sb = p.getServer().getScoreboard();
        if (r.side == null) {
            return;
        }
        Grade g = Grade.of(r.reputation);
        String name = "mm_" + r.side.id + "_" + g.name().toLowerCase();
        PlayerTeam team = sb.getPlayerTeam(name);
        if (team == null) {
            team = sb.addPlayerTeam(name);
            team.setPlayerPrefix(Component.literal("[" + (r.side == Side.US ? g.shortUs : g.shortSoviet) + "] ")
                    .withStyle(r.side == Side.US ? ChatFormatting.BLUE : ChatFormatting.RED));
        }
        sb.addPlayerToTeam(p.getScoreboardName(), team);
    }
}

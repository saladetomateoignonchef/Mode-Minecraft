package fr.modeminecraft.world.police;

import fr.modeminecraft.entity.bot.BotEvents;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.FactionRelations;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Avis de recherche : un braquage ou une effraction déclenche l'alarme ; toutes les patrouilles de police à 64 blocs
 * convergent vers le joueur (voitures de patrouille comprises), et le SWAT est appelé si personne n'est assez près.
 */
public final class Wanted {
    public static void alarm(ServerLevel level, BlockPos pos, Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        long now = level.getGameTime();
        FactionRelations.markHostile(player.getUUID(), Faction.POLICE, now);
        player.displayClientMessage(Component.translatable("message.modeminecraft.wanted").withStyle(ChatFormatting.RED), true);
        boolean any = false;
        for (HumanBotEntity bot : level.getEntitiesOfClass(HumanBotEntity.class, player.getBoundingBox().inflate(64),
                b -> b.isAlive() && b.faction().group.equals("police"))) {
            bot.setTarget(player);
            BotEvents.alertFaction(bot, player);
            any = true;
        }
        PolicePatrols.dispatch(level, pos, player);
        if (!any) {
            BotEvents.callSwat(level, player);
        }
    }

    private Wanted() {}
}

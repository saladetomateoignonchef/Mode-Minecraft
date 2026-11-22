package fr.modeminecraft.entity.bot;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Règles d'agro :
 * - immunité absolue entre un joueur et l'armée de son propre camp (soldats, gardes, pilotes, tourelles) : aucun ciblage,
 *   aucun coup reçu de lui (le coup est absorbé sans alerte ni riposte) ;
 * - mort, réapparition ou changement de dimension d'un joueur : tous les bots, véhicules et tourelles qui le visaient
 *   effacent leur cible, leur rancune et leur appel radio, et reprennent leur patrouille.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class AggroGuard {
    private AggroGuard() {}

    /** Aucun PNJ d'une faction ne prend pour cible le joueur de son camp, par aucun chemin (buts, vengeance, véhicules). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player p && event.getEntity() instanceof HumanBotEntity bot
                && FactionRelations.isAlly(p.getUUID(), bot.faction())) {
            event.setCanceled(true);
        }
    }

    /** Zéro tir allié : un coup du joueur sur un soldat de son camp est absorbé (pas de dégâts, pas d'alerte). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof HumanBotEntity bot)) {
            return;
        }
        Entity src = event.getSource().getEntity();
        if (src instanceof Player p && FactionRelations.isAlly(p.getUUID(), bot.faction())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            forget(p);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            forget(p);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            forget(p);
        }
    }

    /** Tout ce qui visait ce joueur l'oublie, et les factions lui pardonnent : retour au calme. */
    public static void forget(ServerPlayer player) {
        var id = player.getUUID();
        FactionRelations.forgetAll(id);
        for (ServerLevel level : player.server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof HumanBotEntity bot) {
                    bot.forgetPlayer(id);
                } else if (e instanceof Mob m && m.getTarget() == player) {
                    m.setTarget(null);
                    m.setLastHurtByMob(null);
                    m.getNavigation().stop();
                }
            }
        }
    }
}

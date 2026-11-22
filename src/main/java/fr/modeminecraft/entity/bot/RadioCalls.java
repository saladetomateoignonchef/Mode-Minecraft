package fr.modeminecraft.entity.bot;

import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Furtivité et appel radio (cahier des charges « Système d'alerte radio ») : plus aucune alerte télépathique.
 * <ul>
 * <li>un crime (meurtre, agression, braquage, effraction au pied-de-biche, coup de feu) n'alerte personne si aucun
 * témoin vivant ne l'a vu (cône de vision + ligne de vue) ; une arme silencieuse ne s'entend qu'à 8 blocs ;</li>
 * <li>le témoin passe en alerte individuelle et lance un appel radio : icône rouge clignotante et barre de 30 s
 * au-dessus de sa tête ; pendant ces 30 s, seuls lui et les alliés à portée de voix (12 blocs) sont hostiles ;</li>
 * <li>éliminé avant la fin, l'appel est coupé (grésillement) et le reste de la ville n'en saura jamais rien ;</li>
 * <li>à T + 30 s, l'alerte part sur la fréquence de la faction : tout le monde à 160 blocs passe en recherche / hostile
 * (police : sirènes, voitures de patrouille et renforts vers la dernière position connue).</li>
 * </ul>
 */
public final class RadioCalls {
    public static final int DURATION = 600, VOICE = 12, BROADCAST = 160, GRUDGE = 2400;

    /** La faction dispose-t-elle de radios (forces armées organisées) ? */
    static boolean hasRadio(Faction f) {
        return f.combatant() && f != Faction.FALCON && f != Faction.GANG && f != Faction.REBEL;
    }

    /** Vraie vision : cône de 110°, portée de la faction (réduite la nuit et par un déguisement), ligne de vue. */
    public static boolean sees(HumanBotEntity bot, LivingEntity t) {
        Vec3 to = t.getEyePosition().subtract(bot.getEyePosition());
        double d = to.length();
        double range = bot.faction().sightRange(bot.role());
        if (t instanceof Player p) {
            range *= Disguise.sightFactor(bot, p);
        }
        range = BotNight.sightRange(bot, t, range);
        if (d > range) {
            return false;
        }
        if (d > 2) {
            Vec3 look = Vec3.directionFromRotation(0, bot.getYHeadRot());
            if (new Vec3(to.x, 0, to.z).normalize().dot(look) < Math.cos(Math.toRadians(55))) {
                return false;
            }
        }
        return bot.hasLineOfSight(t);
    }

    /** Ce bot voit-il le point `at` (la victime, la scène) ? */
    static boolean seesPoint(HumanBotEntity bot, Vec3 at, double range) {
        Vec3 eye = bot.getEyePosition();
        Vec3 to = at.subtract(eye);
        if (to.length() > range) {
            return false;
        }
        Vec3 look = Vec3.directionFromRotation(0, bot.getYHeadRot());
        if (to.length() > 2 && new Vec3(to.x, 0, to.z).normalize().dot(look) < Math.cos(Math.toRadians(55))) {
            return false;
        }
        return bot.level().clip(new ClipContext(eye, at, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, bot)).getType() == HitResult.Type.MISS;
    }

    /**
     * Un crime vient d'être commis par `p` à `at` (victime éventuelle `victim`, encore vivante ou non) : les membres des
     * factions armées qui voient le joueur ou la scène deviennent témoins. Renvoie vrai s'il y a eu au moins un témoin.
     */
    public static boolean crime(ServerLevel level, Player p, Vec3 at, HumanBotEntity victim) {
        if (p.isCreative() || p.isSpectator()) {
            return false;
        }
        boolean any = false;
        if (victim != null && victim.isAlive() && victim.faction().combatant()) {
            witness(victim, p);                                          // la victime sait qui l'a attaquée
            any = true;
        }
        for (HumanBotEntity b : level.getEntitiesOfClass(HumanBotEntity.class, new AABB(at, at).inflate(64),
                b -> b.isAlive() && b != victim && b.faction().combatant())) {
            if (sees(b, p) || seesPoint(b, at.add(0, 1, 0), 24)) {
                witness(b, p);
                any = true;
            }
        }
        return any;
    }

    /** Ce bot est témoin d'un crime de `p` : rancune, alliés à portée de voix, appel radio de 30 s. */
    public static void witness(HumanBotEntity bot, Player p) {
        if (p.isCreative() || p.isSpectator() || !bot.isAlive() || FactionRelations.isAlly(p.getUUID(), bot.faction())) {
            return;
        }
        long now = bot.level().getGameTime();
        bot.setGrudge(p, now + GRUDGE);
        if (bot.getTarget() == null) {
            bot.setTarget(p);
        }
        // alliés à portée de voix directe
        for (HumanBotEntity o : bot.level().getEntitiesOfClass(HumanBotEntity.class, bot.getBoundingBox().inflate(VOICE),
                o -> o != bot && o.isAlive() && o.faction().group.equals(bot.faction().group) && o.faction().combatant())) {
            o.setGrudge(p, now + GRUDGE);
            if (o.getTarget() == null) {
                o.setTarget(p);
            }
        }
        if (hasRadio(bot.faction()) && bot.radioEnd() <= now && !FactionRelations.isHostile(p.getUUID(), bot.faction(), now)
                && !fr.modeminecraft.scenario.Gadgets.jammed(bot.level(), bot.position())) {
            bot.startRadio(p, now + DURATION);
            bot.level().playSound(null, bot.getX(), bot.getY() + 1.5, bot.getZ(), ModSounds.RADIO_DISPATCH_CALL.get(), SoundSource.HOSTILE, 1.4f, 1f);
        }
    }

    /** Un garde d'une faction hostile repère l'intrus : il le signale par radio (30 s pour l'en empêcher). */
    static void spotted(HumanBotEntity bot, Player p) {
        if (FactionRelations.isAlly(p.getUUID(), bot.faction())) {
            return;
        }
        long now = bot.level().getGameTime();
        if (!p.isCreative() && hasRadio(bot.faction()) && bot.radioEnd() <= now && !FactionRelations.isHostile(p.getUUID(), bot.faction(), now)
                && !fr.modeminecraft.scenario.Gadgets.jammed(bot.level(), bot.position())
                && bot.level().getEntitiesOfClass(HumanBotEntity.class, bot.getBoundingBox().inflate(VOICE),
                o -> o.radioEnd() > now && o.faction().group.equals(bot.faction().group)).isEmpty()) {
            bot.startRadio(p, now + DURATION);
            bot.level().playSound(null, bot.getX(), bot.getY() + 1.5, bot.getZ(), ModSounds.RADIO_DISPATCH_CALL.get(), SoundSource.HOSTILE, 1.4f, 1f);
        }
    }

    /** Fin du compte à rebours : l'alerte est diffusée à toute la faction. */
    static void tick(HumanBotEntity bot) {
        long end = bot.radioEnd();
        if (end <= 0 || !(bot.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (now < end) {
            if ((end - now) % 100 == 0) {
                level.playSound(null, bot.getX(), bot.getY() + 1.5, bot.getZ(), ModSounds.RADIO_CRACKLE.get(), SoundSource.HOSTILE, 0.9f, 1.1f);
            }
            return;
        }
        Player p = bot.radioTarget() == null ? null : level.getPlayerByUUID(bot.radioTarget());
        bot.startRadio(null, 0);
        if (fr.modeminecraft.scenario.Gadgets.jammed(level, bot.position())) {
            // brouilleur portatif : l'appel n'aboutit pas
            level.playSound(null, bot.getX(), bot.getY() + 1.5, bot.getZ(), ModSounds.RADIO_CALL_CUT.get(), SoundSource.HOSTILE, 1.4f, 0.8f);
            return;
        }
        if (p == null || p.isCreative()) {
            return;
        }
        broadcast(level, bot, p);
    }

    /** Alerte générale sur la fréquence de la faction (160 blocs). */
    public static void broadcast(ServerLevel level, HumanBotEntity caller, Player p) {
        long now = level.getGameTime();
        Faction f = caller.faction();
        FactionRelations.markHostile(p.getUUID(), f, now);
        for (HumanBotEntity o : level.getEntitiesOfClass(HumanBotEntity.class, caller.getBoundingBox().inflate(BROADCAST),
                o -> o.isAlive() && o.faction().group.equals(f.group) && o.faction().combatant())) {
            o.setGrudge(p, now + GRUDGE);
            if (o.getTarget() == null) {
                o.setTarget(p);
                o.getNavigation().moveTo(p.getX(), p.getY(), p.getZ(), 1.1);          // convergent vers la dernière position
            }
        }
        level.playSound(null, caller.getX(), caller.getY() + 1.5, caller.getZ(), ModSounds.RADIO_DISPATCH_CALL.get(), SoundSource.HOSTILE, 2f, 0.8f);
        if (f.group.equals("police")) {
            fr.modeminecraft.world.police.Wanted.alarm(level, p.blockPosition(), p);      // sirènes, voitures, SWAT
        } else {
            p.displayClientMessage(Component.translatable("message.modeminecraft.radio.alert", Component.translatable("faction.modeminecraft." + f.id))
                    .withStyle(ChatFormatting.RED), true);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.LOUDSPEAKER.get(), SoundSource.HOSTILE, 1.5f, 0.8f);
        }
    }

    /** Le témoin est mort pendant son appel : la transmission est coupée. */
    static void cut(HumanBotEntity bot) {
        if (bot.radioEnd() > bot.level().getGameTime()) {
            bot.level().playSound(null, bot.getX(), bot.getY() + 1.5, bot.getZ(), ModSounds.RADIO_CALL_CUT.get(), SoundSource.HOSTILE, 1.4f, 1f);
            bot.startRadio(null, 0);
        }
    }

    /**
     * Coup de feu : seuls ceux qui l'entendent (40 blocs, 8 avec un silencieux) réagissent. S'ils voient le tireur, ils
     * deviennent témoins ; sinon ils vont voir ce qui se passe (sans savoir qui a tiré).
     */
    static void gunshot(ServerLevel level, Player p, boolean silenced) {
        double radius = silenced ? 8 : 40;
        for (HumanBotEntity bot : level.getEntitiesOfClass(HumanBotEntity.class, p.getBoundingBox().inflate(radius),
                b -> b.isAlive() && b.faction().combatant() && b.faction() != Faction.FALCON)) {
            if (bot.position().distanceTo(p.position()) > radius) {
                continue;
            }
            if (bot.hasLineOfSight(p)) {
                witness(bot, p);
            } else if (bot.getTarget() == null) {
                bot.getNavigation().moveTo(p.getX(), p.getY(), p.getZ(), 1.0);              // enquête
                bot.getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ());
            }
        }
    }

    private RadioCalls() {}
}

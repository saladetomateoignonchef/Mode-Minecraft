package fr.modeminecraft.entity.bot;

import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Braquage des PNJ civils non armés (habitants, passants, commerçants, ouvriers) : il suffit d'une arme blanche
 * (couteau tactique minimum) ou d'une arme à feu. À moins de 3,5 blocs, on vise le civil et on maintient le clic droit
 * 1,5 s : il lève les mains en tremblant et lâche son argent (billets, pépites d'or, émeraudes, parfois un téléphone,
 * une montre ou une ration). Si aucun représentant de l'ordre (policier, soldat, garde de faction, shérif) ne voit la
 * scène dans son champ de vision, il ne se passe rien ; sinon, il passe en mode arrestation / hostile.
 */
public final class Robbery {
    public static final double RANGE = 3.5;
    public static final int HOLD_TICKS = 30;

    /** Civil braquable : non combattant et sans arme (les policiers, miliciens, soldats, shérifs et gardes, non). */
    public static boolean robbable(HumanBotEntity bot) {
        Faction f = bot.faction();
        return bot.isAlive() && !f.combatant() && !bot.hasGun() && bot.role() != BotRole.CONTRACTOR;
    }

    /** Le joueur a tenu le civil en joue assez longtemps (paquet du client, vérifié ici). */
    public static void rob(ServerLevel level, Player player, HumanBotEntity bot) {
        if (!robbable(bot) || !BotGoals.armedInHand(player) || player.distanceTo(bot) > RANGE + 1 || player.isSpectator()) {
            return;
        }
        long now = level.getGameTime();
        bot.getNavigation().stop();
        bot.setActivity(CivilianLife.HANDS_UP);
        bot.eventUntil = now + 60;
        if (bot.robbed) {
            player.displayClientMessage(Component.translatable("message.modeminecraft.robbery_empty").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        bot.robbed = true;
        if (now - bot.lastScream > 100) {
            bot.lastScream = now;
            bot.playSound(ModSounds.CIVILIAN_SCREAM.get(), 1f, 1.15f);
        }
        boolean register = false;
        if (bot.shopkeeper) {
            // le commerçant ouvre le tiroir-caisse le plus proche
            for (BlockPos p : BlockPos.betweenClosed(bot.blockPosition().offset(-4, -1, -4), bot.blockPosition().offset(4, 2, 4))) {
                if (level.getBlockState(p).getBlock() instanceof fr.modeminecraft.block.city.CashRegisterBlock) {
                    fr.modeminecraft.block.city.CashRegisterBlock.burst(level, p.immutable(), player);
                    register = true;
                    break;
                }
            }
        }
        if (!register) {
            dropPockets(level, bot, player);
            if (!witnessed(level, player)) {
                player.displayClientMessage(Component.translatable("message.modeminecraft.robbery_quiet").withStyle(ChatFormatting.GREEN), true);
            }
        }
    }

    private static void dropPockets(ServerLevel level, HumanBotEntity bot, Player player) {
        var r = bot.getRandom();
        ItemStack[] loot = {
                new ItemStack(ModItems.CASH_BUNDLE.get(), 1 + r.nextInt(3)),
                new ItemStack(Items.GOLD_NUGGET, 2 + r.nextInt(6)),
                r.nextFloat() < 0.45f ? new ItemStack(Items.EMERALD, 1 + r.nextInt(2)) : ItemStack.EMPTY,
                r.nextFloat() < 0.2f ? new ItemStack(ModItems.SMARTPHONE.get()) : ItemStack.EMPTY,
                r.nextFloat() < 0.15f ? new ItemStack(ModItems.WRISTWATCH.get()) : ItemStack.EMPTY,
                r.nextFloat() < 0.25f ? new ItemStack(ModItems.MILITARY_RATION.get()) : ItemStack.EMPTY};
        Vec3 dir = player.position().subtract(bot.position()).normalize().scale(0.15);
        for (ItemStack st : loot) {
            if (st.isEmpty()) {
                continue;
            }
            ItemEntity e = new ItemEntity(level, bot.getX(), bot.getY() + 1.0, bot.getZ(), st);
            e.setDeltaMovement(dir.x + (r.nextDouble() - 0.5) * 0.12, 0.2, dir.z + (r.nextDouble() - 0.5) * 0.12);
            level.addFreshEntity(e);
        }
        level.playSound(null, bot.getX(), bot.getY(), bot.getZ(), net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 0.8f, 1.3f);
    }

    /** Représentant de l'ordre (pas les gangs, cartels ni Faucons, qui se moquent bien d'un braquage). */
    private static boolean lawful(Faction f) {
        return f.combatant() && f != Faction.GANG && f != Faction.CARTEL && f != Faction.FALCON;
    }

    /** Vraie vision (cône de 110°, ligne de vue dégagée, portée de la faction ; pas d'ouïe). */
    private static boolean sees(HumanBotEntity bot, Player p) {
        Vec3 to = p.getEyePosition().subtract(bot.getEyePosition());
        double d = to.length();
        if (d > bot.faction().sightRange(bot.role())) {
            return false;
        }
        if (d > 2) {
            Vec3 look = Vec3.directionFromRotation(0, bot.getYHeadRot());
            if (new Vec3(to.x, 0, to.z).normalize().dot(look) < Math.cos(Math.toRadians(55))) {
                return false;
            }
        }
        return bot.hasLineOfSight(p) && BotNight.sightRange(bot, p, bot.faction().sightRange(bot.role())) >= d;
    }

    /**
     * Des représentants de l'ordre ont-ils vu le braquage ? Les policiers déclenchent l'avis de recherche ; les autres
     * (soldats, gardes, shérifs) deviennent hostiles. Personne : il ne se passe rien.
     */
    public static boolean witnessed(ServerLevel level, Player p) {
        if (p.isCreative() || p.isSpectator()) {
            return false;
        }
        boolean any = false;
        for (HumanBotEntity b : level.getEntitiesOfClass(HumanBotEntity.class, p.getBoundingBox().inflate(90),
                b -> b.isAlive() && lawful(b.faction()))) {
            if (sees(b, p)) {
                RadioCalls.witness(b, p);                                   // appel radio : 30 s pour l'en empêcher
                any = true;
            }
        }
        if (any) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.robbery_seen").withStyle(ChatFormatting.RED), true);
        }
        return any;
    }

    private Robbery() {}
}

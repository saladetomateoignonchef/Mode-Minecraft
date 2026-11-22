package fr.modeminecraft.entity.train;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModEffects;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Personnel et voyageurs du train : le guichetier vend les billets, les contrôleurs vérifient les voyageurs qu'ils croisent
 * dans le couloir (billet composté pour le trajet, sinon coup de matraque électrique : paralysie de 5 secondes sans
 * dégâts mortels), et les fraudeurs sont expulsés sur le quai à la gare suivante. Les voyageurs passent d'un wagon à
 * l'autre en marche avec Z / S.
 */
public final class TrainStaff {
    private TrainStaff() {}

    private static boolean forced;
    private static final Map<UUID, Long> HOP = new ConcurrentHashMap<>();

    /** Descente ou changement de wagon voulus par le jeu lui-même (pas par la touche Shift d'un joueur en marche). */
    public static void force(Runnable r) {
        boolean old = forced;
        forced = true;
        try {
            r.run();
        } finally {
            forced = old;
        }
    }

    public static boolean forced() {
        return forced;
    }

    // ------------------------------------------------------------------ guichet
    public static final int PRICE_EMERALDS = 2;

    private static int count(Player p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void take(Player p, Item item, int n) {
        for (ItemStack s : p.getInventory().items) {
            if (n <= 0) {
                return;
            }
            if (s.is(item)) {
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
    }

    /** Le guichetier vend un ticket contre 2 émeraudes ou 1 lingot d'or. */
    public static void sellTicket(HumanBotEntity clerk, Player player) {
        if (count(player, Items.GOLD_INGOT) >= 1) {
            take(player, Items.GOLD_INGOT, 1);
        } else if (count(player, Items.EMERALD) >= PRICE_EMERALDS) {
            take(player, Items.EMERALD, PRICE_EMERALDS);
        } else {
            player.displayClientMessage(Component.translatable("message.modeminecraft.train.clerk_price").withStyle(ChatFormatting.GOLD), false);
            return;
        }
        player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.MESA_TRAIN_TICKET.get()));
        clerk.level().playSound(null, clerk.blockPosition(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.NEUTRAL, 1f, 1.3f);
        player.displayClientMessage(Component.translatable("message.modeminecraft.train.clerk_sold").withStyle(ChatFormatting.GREEN), false);
    }

    // ------------------------------------------------------------------ à bord
    /** Appelée toutes les 4 ticks par la locomotive : contrôles, changements de wagon, contrôleurs qui changent de voiture. */
    static void tick(MesaTrainCarEntity loco, List<MesaTrainCarEntity> cars) {
        ServerLevel sl = (ServerLevel) loco.level();
        long now = sl.getGameTime();
        List<MesaTrainCarEntity> wagons = new ArrayList<>();
        for (MesaTrainCarEntity c : cars) {
            if (!c.isLoco()) {
                wagons.add(c);
            }
        }
        for (MesaTrainCarEntity car : cars) {
            for (Entity e : new ArrayList<>(car.getPassengers())) {
                if (e instanceof HumanBotEntity bot && bot.role() == BotRole.TRAIN_INSPECTOR && !car.isLoco()) {
                    // un contrôleur change de wagon de temps en temps
                    if (now % 400 < 4 && wagons.size() > 1 && (bot.getId() + now / 400) % 3 == 0) {
                        MesaTrainCarEntity next = wagons.get((wagons.indexOf(car) + 1) % wagons.size());
                        if (next != car && next.getPassengers().size() < next.capacity()) {
                            force(() -> bot.stopRiding());
                            bot.startRiding(next, true);
                            continue;
                        }
                    }
                    for (Entity other : new ArrayList<>(car.getPassengers())) {
                        if (other instanceof Player p && !p.isSpectator() && bot.distanceToSqr(p) < 4.0) {
                            inspect(loco, bot, p, now);
                        }
                    }
                } else if (e instanceof Player p && !car.isLoco() && p.zza != 0 && HOP.getOrDefault(p.getUUID(), 0L) <= now) {
                    // circulation dans le couloir : Z vers la tête du train, S vers la queue
                    int i = wagons.indexOf(car) + (p.zza > 0 ? -1 : 1);
                    if (i >= 0 && i < wagons.size() && wagons.get(i).getPassengers().size() < wagons.get(i).capacity()) {
                        MesaTrainCarEntity to = wagons.get(i);
                        HOP.put(p.getUUID(), now + 12);
                        force(() -> p.stopRiding());
                        p.startRiding(to, true);
                    }
                }
            }
        }
    }

    private static void inspect(MesaTrainCarEntity loco, HumanBotEntity inspector, Player p, long now) {
        if (loco.validated().contains(p.getUUID())) {
            return;
        }
        for (ItemStack s : p.getInventory().items) {
            if (s.is(ModItems.MESA_TRAIN_TICKET.get())) {
                s.shrink(1);                                              // composté pour ce trajet
                loco.validated().add(p.getUUID());
                inspector.swing(InteractionHand.OFF_HAND);
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.NEUTRAL, 1f, 1.5f);
                p.displayClientMessage(Component.translatable("message.modeminecraft.train.validated").withStyle(ChatFormatting.GREEN), true);
                return;
            }
        }
        // fraudeur : coup de matraque électrique (paralysie 5 s, 0,5 PV de dégâts symboliques au plus)
        if (loco.stunCooldown().getOrDefault(p.getUUID(), 0L) > now) {
            return;
        }
        loco.stunCooldown().put(p.getUUID(), now + 160);
        loco.fraudsters().add(p.getUUID());
        stun(p, 100);
        inspector.swing(InteractionHand.MAIN_HAND);
        p.displayClientMessage(Component.translatable("message.modeminecraft.train.fraud").withStyle(ChatFormatting.RED), true);
    }

    /** Étourdissement électrique : immobile, sans arme, caméra qui tremble ; ne tue jamais. */
    public static void stun(net.minecraft.world.entity.LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(ModEffects.STUNNED, ticks, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 255, false, false));
        if (target.getHealth() > 1f) {
            target.hurt(target.damageSources().generic(), 0.5f);
        }
        if (target.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
            sl.playSound(null, target.blockPosition(), ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 1.2f, 1f);
        }
    }

    /** Arrêt en gare : les fraudeurs sont expulsés sur le quai ; tout le monde doit recomposter son billet au trajet suivant. */
    static void arrived(MesaTrainCarEntity loco, List<MesaTrainCarEntity> cars, CityPlanner.City city) {
        loco.validated().clear();
        if (city == null) {
            return;
        }
        for (MesaTrainCarEntity car : cars) {
            for (Entity e : new ArrayList<>(car.getPassengers())) {
                if (loco.fraudsters().remove(e.getUUID())) {
                    force(() -> e.stopRiding());
                    Vec3 spot = car.platformSpot(city);
                    e.teleportTo(spot.x, spot.y, spot.z);
                    if (e instanceof Player p) {
                        p.displayClientMessage(Component.translatable("message.modeminecraft.train.expelled").withStyle(ChatFormatting.RED), false);
                    }
                }
            }
        }
        loco.fraudsters().clear();
    }

    /** Équipage d'un nouveau train : conducteur dans la locomotive, un contrôleur par wagon (deux au plus). */
    public static void crew(ServerLevel level, MesaTrainCarEntity loco, List<MesaTrainCarEntity> wagons) {
        HumanBotEntity driver = bot(level, BotRole.TRAIN_DRIVER, loco);
        if (driver != null) {
            driver.startRiding(loco, true);
        }
        for (int i = 0; i < Math.min(2, wagons.size()); i++) {
            HumanBotEntity inspector = bot(level, BotRole.TRAIN_INSPECTOR, wagons.get(i));
            if (inspector != null) {
                inspector.startRiding(wagons.get(i), true);
            }
        }
    }

    private static HumanBotEntity bot(ServerLevel level, BotRole role, Entity at) {
        HumanBotEntity b = fr.modeminecraft.registry.ModEntities.HUMAN_BOT.get().create(level);
        if (b == null) {
            return null;
        }
        b.moveTo(at.getX(), at.getY() + 1, at.getZ(), at.getYRot(), 0);
        b.setup(Faction.RAILWAY, role, level.getRandom());
        b.home = at.blockPosition();
        b.homeRadius = 4;
        level.addFreshEntity(b);
        return b;
    }
}

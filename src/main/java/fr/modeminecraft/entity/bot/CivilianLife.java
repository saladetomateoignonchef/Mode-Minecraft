package fr.modeminecraft.entity.bot;

import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Vie quotidienne des civils des villes :
 * - routines : flâner sur les trottoirs (en traversant aux passages piétons), s'asseoir sur les bancs publics,
 *   discuter en petits groupes, lire les journaux au kiosque, rentrer chez soi à la tombée de la nuit ;
 * - panique : coup de feu, explosion ou sirène à proximité : cri, puis sprint vers l'intérieur (le hall de leur
 *   immeuble, leur magasin) ; tenus en joue : les mains en l'air ;
 * - commerçants (derrière leur comptoir) : vendent rations, boissons, bandages, piles et outils contre des liasses
 *   de billets ; braqués (arme pointée sur eux), ils lèvent les mains et ouvrent le tiroir-caisse.
 */
public final class CivilianLife {
    public static final byte NONE = 0, SITTING = 1, HANDS_UP = 2, CHATTING = 3, READING = 4;

    static boolean civilian(HumanBotEntity bot) {
        return bot.faction() == Faction.CIVILIAN && bot.role() == BotRole.CIVILIAN;
    }

    /** Un joueur tient-il ce civil en joue (arme pointée sur lui, à 10 blocs, sans obstacle) ? */
    static Player aimingAt(HumanBotEntity bot) {
        for (Player p : bot.level().getEntitiesOfClass(Player.class, bot.getBoundingBox().inflate(10))) {
            if (p.isSpectator() || !(p.getMainHandItem().getItem() instanceof GunItem)) {
                continue;
            }
            Vec3 to = bot.getEyePosition().subtract(p.getEyePosition());
            double d = to.length();
            if (d < 0.5 || to.normalize().dot(p.getLookAngle()) < 0.965 || !bot.hasLineOfSight(p)) {
                continue;
            }
            return p;
        }
        return null;
    }

    /** Panique générale autour d'un point (coup de feu, explosion, sirène). */
    public static void panic(net.minecraft.world.level.Level level, Vec3 at, double radius, int ticks) {
        long now = level.getGameTime();
        for (HumanBotEntity bot : level.getEntitiesOfClass(HumanBotEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius),
                b -> !b.faction().combatant() && b.isAlive())) {
            boolean fresh = bot.scaredUntil < now;
            bot.scaredUntil = Math.max(bot.scaredUntil, now + ticks);
            bot.setActivity(NONE);
            if (fresh && now - bot.lastScream > 200 && bot.getRandom().nextFloat() < 0.5f) {
                bot.lastScream = now;
                level.playSound(null, bot.getX(), bot.getY(), bot.getZ(), ModSounds.CIVILIAN_SCREAM.get(), SoundSource.NEUTRAL, 1.2f,
                        0.85f + bot.getRandom().nextFloat() * 0.35f);
            }
        }
    }

    /** Fuite paniquée : vers l'intérieur (hall d'immeuble, magasin) s'il est proche, sinon loin de la menace. */
    static Vec3 shelter(HumanBotEntity bot) {
        if (bot.home != null && bot.home.distSqr(bot.blockPosition()) < 40 * 40) {
            return Vec3.atBottomCenterOf(bot.home);
        }
        Player p = bot.level().getNearestPlayer(bot, 32);
        return p == null ? DefaultRandomPos.getPos(bot, 14, 5) : DefaultRandomPos.getPosAway(bot, 18, 6, p.position());
    }

    // ------------------------------------------------------------------ mains en l'air (et braquage des commerçants)
    static final class HandsUp extends Goal {
        private final HumanBotEntity bot;
        private Player threat;
        private int held, grace;

        HandsUp(HumanBotEntity bot) {
            this.bot = bot;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!civilian(bot) && bot.role() != BotRole.TRADER || bot.tickCount % 4 != 0) {
                return false;
            }
            threat = aimingAt(bot);
            return threat != null;
        }

        @Override
        public void start() {
            held = 0;
            grace = 40;
            bot.getNavigation().stop();
            bot.setActivity(HANDS_UP);
            if (bot.level().getGameTime() - bot.lastScream > 200) {
                bot.lastScream = bot.level().getGameTime();
                bot.playSound(ModSounds.CIVILIAN_SCREAM.get(), 1f, 1.1f);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return threat != null && threat.isAlive() && grace > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            bot.getLookControl().setLookAt(threat, 30, 30);
            bot.getNavigation().stop();
            if (aimingAt(bot) == threat) {
                grace = 40;
            } else {
                grace--;
            }
            held++;
            // braquage : le commerçant ouvre le tiroir-caisse le plus proche
            if (bot.shopkeeper && !bot.robbed && held == 30) {
                for (BlockPos p : BlockPos.betweenClosed(bot.blockPosition().offset(-4, -1, -4), bot.blockPosition().offset(4, 2, 4))) {
                    if (bot.level().getBlockState(p).getBlock() instanceof fr.modeminecraft.block.city.CashRegisterBlock) {
                        fr.modeminecraft.block.city.CashRegisterBlock.burst(bot.level(), p.immutable(), threat);
                        bot.robbed = true;
                        break;
                    }
                }
            }
        }

        @Override
        public void stop() {
            bot.setActivity(NONE);
            bot.scaredUntil = bot.level().getGameTime() + 100;                // puis il s'enfuit
            threat = null;
        }
    }

    // ------------------------------------------------------------------ routines quotidiennes
    static final class Routine extends Goal {
        private final HumanBotEntity bot;
        private int left;
        private BlockPos spot;
        private HumanBotEntity partner;
        private Vec3 via;

        Routine(HumanBotEntity bot) {
            this.bot = bot;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return civilian(bot) && !bot.shopkeeper && bot.eventUntil < bot.level().getGameTime() && bot.getVehicle() == null
                    && bot.scaredUntil < bot.level().getGameTime() && bot.getRandom().nextInt(40) == 0;
        }

        @Override
        public void start() {
            var level = bot.level();
            bot.setActivity(NONE);
            spot = null;
            partner = null;
            via = null;
            if (fr.modeminecraft.world.night.NightOps.isNight(level)) {
                // la nuit, on rentre à l'intérieur
                if (bot.home != null) {
                    bot.getNavigation().moveTo(bot.home.getX() + 0.5, bot.home.getY(), bot.home.getZ() + 0.5, 0.9);
                }
                left = 400;
                return;
            }
            float r = bot.getRandom().nextFloat();
            if (r < 0.3f && (spot = find(12, s -> s.getBlock() instanceof StairBlock && s.getValue(StairBlock.HALF) == Half.BOTTOM
                    && s.is(Blocks.DARK_OAK_STAIRS))) != null) {
                left = 300 + bot.getRandom().nextInt(400);                        // banc public
            } else if (r < 0.55f && (partner = chatPartner()) != null) {
                left = 200 + bot.getRandom().nextInt(300);                        // discussion
                partner.eventUntil = level.getGameTime() + left;
                partner.setActivity(CHATTING);
                partner.getNavigation().stop();
            } else if (r < 0.7f && (spot = find(14, s -> s.is(Blocks.LECTERN))) != null) {
                left = 160 + bot.getRandom().nextInt(200);                        // kiosque à journaux
            } else {
                Vec3 dest = DefaultRandomPos.getPos(bot, 16, 4);
                if (dest != null) {
                    via = crosswalk(dest);
                    Vec3 first = via != null ? via : dest;
                    bot.getNavigation().moveTo(first.x, first.y, first.z, 0.75);
                    spot = BlockPos.containing(dest);
                }
                left = 260;
            }
            if (spot != null && partner == null) {
                bot.getNavigation().moveTo(spot.getX() + 0.5, spot.getY() + (bot.getRandom().nextBoolean() ? 0 : 0), spot.getZ() + 0.5, 0.8);
            }
            if (partner != null) {
                bot.getNavigation().moveTo(partner, 0.8);
            }
        }

        /** Destination de l'autre côté d'une chaussée : on passe par le passage piéton le plus proche. */
        private Vec3 crosswalk(Vec3 dest) {
            Vec3 from = bot.position();
            boolean crossesRoad = false;
            for (int i = 1; i < 8 && !crossesRoad; i++) {
                Vec3 p = from.lerp(dest, i / 8.0);
                crossesRoad = bot.level().getBlockState(BlockPos.containing(p.x, bot.getY() - 0.5, p.z)).is(Blocks.BLACK_CONCRETE);
            }
            if (!crossesRoad) {
                return null;
            }
            BlockPos w = find(16, s -> s.is(Blocks.WHITE_CONCRETE), -1);
            return w == null ? null : Vec3.atBottomCenterOf(w.above());
        }

        private HumanBotEntity chatPartner() {
            for (HumanBotEntity o : bot.level().getEntitiesOfClass(HumanBotEntity.class, bot.getBoundingBox().inflate(10),
                    o -> o != bot && civilian(o) && !o.shopkeeper && o.eventUntil < bot.level().getGameTime() && o.scaredUntil < bot.level().getGameTime())) {
                return o;
            }
            return null;
        }

        private BlockPos find(int r, java.util.function.Predicate<BlockState> test) {
            return find(r, test, 0);
        }

        private BlockPos find(int r, java.util.function.Predicate<BlockState> test, int dy) {
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            BlockPos c = bot.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2 + dy, -r), c.offset(r, 2 + dy, r))) {
                if (test.test(bot.level().getBlockState(p)) && bot.level().getBlockState(p.above()).isAir()) {
                    double d = p.distSqr(c) + bot.getRandom().nextInt(20);
                    if (d < bd) {
                        bd = d;
                        best = p.immutable();
                    }
                }
            }
            return best;
        }

        @Override
        public boolean canContinueToUse() {
            return left > 0 && bot.scaredUntil < bot.level().getGameTime() && bot.getTarget() == null;
        }

        @Override
        public void tick() {
            left--;
            if (partner != null) {
                if (!partner.isAlive()) {
                    left = 0;
                    return;
                }
                if (bot.distanceTo(partner) < 2.2) {
                    bot.getNavigation().stop();
                    bot.setActivity(CHATTING);
                    bot.getLookControl().setLookAt(partner);
                    partner.getLookControl().setLookAt(bot);
                    if (bot.getRandom().nextInt(40) == 0) bot.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    if (bot.getRandom().nextInt(50) == 0) partner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                } else if (bot.getNavigation().isDone()) {
                    bot.getNavigation().moveTo(partner, 0.8);
                }
                return;
            }
            if (via != null && bot.getNavigation().isDone() && spot != null) {
                via = null;
                bot.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.75);
                return;
            }
            if (spot == null) {
                return;
            }
            var st = bot.level().getBlockState(spot);
            double d = bot.position().distanceToSqr(Vec3.atBottomCenterOf(spot));
            if (st.getBlock() instanceof StairBlock && d < 2.5) {
                // assis sur le banc, tourné vers l'avant du banc
                bot.getNavigation().stop();
                if (bot.activity() != SITTING) {
                    bot.moveTo(spot.getX() + 0.5, spot.getY() + 0.5, spot.getZ() + 0.5, st.getValue(StairBlock.FACING).getOpposite().toYRot(), 0);
                    bot.setActivity(SITTING);
                }
                bot.setYBodyRot(st.getValue(StairBlock.FACING).getOpposite().toYRot());
            } else if (st.is(Blocks.LECTERN) && d < 4) {
                bot.getNavigation().stop();
                bot.setActivity(READING);
                bot.getLookControl().setLookAt(spot.getX() + 0.5, spot.getY() + 1.1, spot.getZ() + 0.5);
            }
        }

        @Override
        public void stop() {
            if (partner != null) {
                partner.setActivity(NONE);
                partner.eventUntil = 0;
            }
            bot.setActivity(NONE);
            partner = null;
            spot = null;
        }
    }

    // ------------------------------------------------------------------ commerçants
    /** Rations, boissons, bandages, piles CR123A et outils, payés en liasses de billets. */
    static MerchantOffers shopOffers(net.minecraft.util.RandomSource r) {
        ItemStack cash = new ItemStack(fr.modeminecraft.registry.ModItems.CASH_BUNDLE.get());
        java.util.List<MerchantOffer> all = new java.util.ArrayList<>(java.util.List.of(
                offer(1, new ItemStack(Items.BREAD, 3)), offer(1, new ItemStack(Items.COOKED_BEEF, 2)), offer(1, new ItemStack(Items.BAKED_POTATO, 4)),
                offer(1, new ItemStack(Items.HONEY_BOTTLE, 2)), offer(1, new ItemStack(Items.MILK_BUCKET)), offer(2, new ItemStack(Items.GOLDEN_CARROT, 3)),
                offer(2, new ItemStack(HumanBotEntity.item("bandage"), 3)), offer(4, new ItemStack(HumanBotEntity.item("first_aid_kit"))),
                offer(2, new ItemStack(HumanBotEntity.item("tactical_battery"), 2)), offer(3, new ItemStack(Items.IRON_PICKAXE)),
                offer(2, new ItemStack(Items.IRON_SHOVEL)), offer(3, new ItemStack(HumanBotEntity.item("crowbar"))), offer(1, new ItemStack(Items.TORCH, 8)),
                offer(2, new ItemStack(Items.FLINT_AND_STEEL))));
        java.util.Collections.shuffle(all, new java.util.Random(r.nextLong()));
        MerchantOffers out = new MerchantOffers();
        all.stream().limit(7).forEach(out::add);
        // reprise : 4 liasses contre une émeraude
        out.add(new MerchantOffer(new ItemCost(cash.getItem(), 4), new ItemStack(Items.EMERALD), 16, 1, 0.05f));
        return out;
    }

    private static MerchantOffer offer(int price, ItemStack result) {
        return new MerchantOffer(new ItemCost(fr.modeminecraft.registry.ModItems.CASH_BUNDLE.get(), price), result, 16, 1, 0.05f);
    }

    private CivilianLife() {}
}

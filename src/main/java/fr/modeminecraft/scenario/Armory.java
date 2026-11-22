package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.FalconSquads;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.world.city.CityPlanner;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Armurerie fortifiée de la base : l'armurier propose 3 services.
 * 1. Boutique (arsenal de la faction contre or, diamants, émeraudes, barils de pétrole ; remise selon le grade).
 *    Un Général ne paie pas : il réquisitionne un article toutes les 2 minutes.
 * 2. Contrats d'élimination : A = abattre le chef d'une faction adverse (Cartel, Milice, commandant ennemi) ;
 *    B = neutraliser la troupe des Faucons Blancs (10 de ses 15 guerriers). Récompense remise par l'armurier : armes
 *    puissantes, lunette thermique, caisses de munitions lourdes, et de la réputation.
 * 3. Carte tactique de toutes les bases (dans la boutique : 4 lingots d'or ou 8 émeraudes).
 */
public final class Armory {
    private Armory() {}

    public static final int REQUISITION_COOLDOWN = 2400;
    public static final int FALCON_GOAL = 10;

    public static void open(HumanBotEntity bot, ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new Open(state(p)));
    }

    /** État envoyé à l'écran de l'armurerie. */
    static CompoundTag state(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        t.putInt("Grade", Reputation.grade(p).ordinal());
        long now = p.level().getGameTime();
        t.putInt("ReqWait", (int) Scenario.record(p).wait("Requisition", REQUISITION_COOLDOWN, now));
        CompoundTag c = contract(p);
        if (c != null) {
            t.put("Contract", c.copy());
        }
        t.putInt("ContractWait", (int) Scenario.record(p).wait("ContractDone", 12000, now));
        return t;
    }

    static CompoundTag contract(ServerPlayer p) {
        for (Tag t : Missions.active(p)) {
            if (((CompoundTag) t).getString("Id").equals("contract")) {
                return (CompoundTag) t;
            }
        }
        return null;
    }

    private static HumanBotEntity armorer(ServerPlayer p) {
        Side side = Scenario.side(p);
        List<HumanBotEntity> l = p.serverLevel().getEntitiesOfClass(HumanBotEntity.class, p.getBoundingBox().inflate(8),
                b -> b.role() == BotRole.ARMORER && side != null && b.faction() == side.ally);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Actions de l'écran : shop, req:<index>, contract_boss, contract_falcon, claim. */
    static void act(ServerPlayer p, String action) {
        HumanBotEntity bot = armorer(p);
        Side side = Scenario.side(p);
        if (bot == null || side == null) {
            return;
        }
        Grade g = Reputation.grade(p);
        if (action.equals("shop")) {
            bot.openShop(p, ArmoryOffers.offers(side, g));
            return;
        }
        if (action.startsWith("req:")) {
            requisition(p, side, g, Integer.parseInt(action.substring(4)));
        } else if (action.equals("contract_boss") || action.equals("contract_falcon")) {
            startContract(p, side, action.equals("contract_falcon"));
        } else if (action.equals("claim")) {
            claim(p, bot);
        }
        PacketDistributor.sendToPlayer(p, new Open(state(p)));
    }

    /** Général : un article gratuit du catalogue toutes les 2 minutes. */
    private static void requisition(ServerPlayer p, Side side, Grade g, int index) {
        if (g != Grade.GENERAL || !ArmoryOffers.forSide(side).contains(index)) {
            return;
        }
        var x = Scenario.record(p).extra;
        long now = p.level().getGameTime();
        if (Scenario.record(p).wait("Requisition", REQUISITION_COOLDOWN, now) > 0) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.armory.req_wait"), true);
            return;
        }
        x.putLong("Requisition", now);
        ItemStack s = ArmoryOffers.catalogue().get(index).stack();
        Missions.give(p, s.copy());
        p.displayClientMessage(Component.translatable("message.modeminecraft.armory.requisition", s.getHoverName()).withStyle(ChatFormatting.GOLD), true);
        ScenarioData.get(p.serverLevel()).setDirty();
    }

    // ------------------------------------------------------------------ contrats d'élimination
    private static void startContract(ServerPlayer p, Side side, boolean falcons) {
        if (contract(p) != null) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.armory.contract_ongoing").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (Missions.active(p).size() >= Missions.MAX_ACTIVE) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.mission.full").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (Scenario.record(p).wait("ContractDone", 12000, p.level().getGameTime()) > 0) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.armory.contract_wait"), true);
            return;
        }
        ServerLevel level = p.serverLevel();
        CompoundTag e = new CompoundTag();
        e.putString("Id", "contract");
        e.putBoolean("Pinned", true);
        if (falcons) {
            Vec3 at = Missions.landSpot(level, p.position(), 250, 400);
            if (at == null) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.mission.no_spot").withStyle(ChatFormatting.RED), true);
                return;
            }
            Missions.keepLoaded(level, BlockPos.containing(at));
            List<HumanBotEntity> squad = FalconSquads.spawnSquad(level, BlockPos.containing(at));
            if (squad.isEmpty()) {
                return;
            }
            e.putString("Kind", "falcon");
            e.putString("Faction", Faction.FALCON.id);
            e.putInt("Goal", FALCON_GOAL);
            e.putInt("Rep", 150);
            e.putUUID("Target", squad.get(0).getUUID());
            Missions.putTarget(e, BlockPos.containing(at));
        } else {
            // cible : le chef d'une faction adverse dans la ville la plus proche (Cartel, Milice, ou base ennemie)
            Faction[] choices = {Faction.CARTEL, Faction.MILITIA, side.enemy};
            CityPlanner.City best = null;
            Faction target = null;
            double bestD = Double.MAX_VALUE;
            for (Faction f : choices) {
                String variant = f == Faction.CARTEL ? "cartel" : f == Faction.MILITIA ? "desert" : side.opposite().baseVariant;
                CityPlanner.City c = Scenario.findCity(level, x -> x.variant().equals(variant), p.blockPosition(), 8);
                if (c != null) {
                    double d = Math.hypot(c.middleX() - p.getX(), c.middleZ() - p.getZ()) * (0.8 + level.random.nextDouble() * 0.4);
                    if (d < bestD) {
                        bestD = d;
                        best = c;
                        target = f;
                    }
                }
            }
            if (best == null) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.mission.no_enemy_base").withStyle(ChatFormatting.RED), true);
                return;
            }
            e.putString("Kind", "boss");
            e.putString("Faction", target.id);
            e.putInt("Goal", 1);
            e.putInt("Rep", 120);
            Missions.putTarget(e, new BlockPos(best.middleX(), best.baseY(), best.middleZ()));
        }
        e.putInt("Progress", 0);
        Missions.active(p).add(e);
        p.displayClientMessage(Component.translatable("message.modeminecraft.armory.contract_new." + e.getString("Kind"),
                Component.translatable("entity.modeminecraft.bot." + e.getString("Faction"))).withStyle(ChatFormatting.GOLD), false);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        Missions.dirty(p);
    }

    /** Ennemi abattu par le joueur : progression du contrat en cours. */
    static void onKill(ServerPlayer p, LivingEntity dead) {
        CompoundTag e = contract(p);
        if (e == null || !(dead instanceof HumanBotEntity hb) || e.getInt("Progress") >= e.getInt("Goal")) {
            return;
        }
        boolean hit = e.getString("Kind").equals("falcon") ? hb.faction() == Faction.FALCON
                : hb.role() == BotRole.BOSS && hb.faction().id.equals(e.getString("Faction"));
        if (hit) {
            e.putInt("Progress", e.getInt("Progress") + 1);
            if (e.getInt("Progress") >= e.getInt("Goal")) {
                p.displayClientMessage(Component.translatable("message.modeminecraft.armory.contract_done").withStyle(ChatFormatting.GREEN), false);
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1f);
                e.putBoolean("HasTarget", false);
            }
            Missions.dirty(p);
        }
    }

    /** Suivi de la troupe des Faucons sur le HUD (chef ou survivant le plus proche). */
    static void tick(ServerPlayer p, CompoundTag e) {
        if (!e.getString("Kind").equals("falcon") || e.getInt("Progress") >= e.getInt("Goal")) {
            return;
        }
        ServerLevel level = p.serverLevel();
        Entity lead = e.hasUUID("Target") ? level.getEntity(e.getUUID("Target")) : null;
        if (lead == null || !lead.isAlive()) {
            List<HumanBotEntity> left = level.getEntitiesOfClass(HumanBotEntity.class,
                    new net.minecraft.world.phys.AABB(new BlockPos(e.getInt("TX"), e.getInt("TY"), e.getInt("TZ"))).inflate(96),
                    b -> b.faction() == Faction.FALCON && b.isAlive());
            if (!left.isEmpty()) {
                lead = left.get(0);
                e.putUUID("Target", lead.getUUID());
            }
        }
        if (lead != null) {
            Missions.putTarget(e, lead.blockPosition());
        }
        Missions.keepLoaded(level, new BlockPos(e.getInt("TX"), 0, e.getInt("TZ")));
    }

    private static void claim(ServerPlayer p, HumanBotEntity bot) {
        CompoundTag e = contract(p);
        if (e == null || e.getInt("Progress") < e.getInt("Goal")) {
            return;
        }
        Missions.active(p).remove(e);
        Scenario.record(p).extra.putLong("ContractDone", p.level().getGameTime());
        boolean us = Scenario.side(p) == Side.US;
        if (e.getString("Kind").equals("falcon")) {
            Missions.give(p, Missions.item(us ? "barrett_m82" : "pkm", 1), Missions.item("thermal_scope", 1),
                    Missions.item(us ? "ammo_50bmg" : "ammo_762x54r", 40), Missions.item("ammo_box_50", 4));
        } else {
            Missions.give(p, Missions.item(us ? "m249" : "pkm", 1), Missions.item(us ? "ammo_556" : "ammo_762x54r", 120),
                    Missions.item("ammo_box_50", 2), new ItemStack(Items.GOLD_INGOT, 6));
        }
        p.displayClientMessage(Component.translatable("message.modeminecraft.armory.contract_paid").withStyle(ChatFormatting.GREEN), false);
        p.serverLevel().playSound(null, bot.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.2f);
        Reputation.add(p, e.getInt("Rep"), "rep.modeminecraft.contract");
        Missions.dirty(p);
    }

    // ------------------------------------------------------------------ réseau
    public record Open(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ModeMinecraft.id("armory_open"));
        public static final StreamCodec<ByteBuf, Open> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(Open::new, Open::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open o, IPayloadContext ctx) {
            ctx.enqueueWork(() -> fr.modeminecraft.client.ArmoryScreen.open(o.data()));
        }
    }

    public record Act(String action) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(ModeMinecraft.id("armory_act"));
        public static final StreamCodec<ByteBuf, Act> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Act::new, Act::action);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Act a, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    act(sp, a.action());
                }
            });
        }
    }
}

package fr.modeminecraft.scenario;

import fr.modeminecraft.entity.bot.Disguise;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Désertion : un joueur qui se présente à l'officier recruteur de la base ennemie en uniforme complet de cette armée
 * peut changer de camp (deux clics à moins de 10 s d'intervalle). Il repart Soldat (réputation à zéro), reçoit un lit
 * et un casier dans le dortoir de sa nouvelle base ; ses missions en cours sont perdues et ses anciens frères d'armes le
 * considèrent désormais comme un traître.
 */
public final class Defection {
    private Defection() {}

    public static boolean request(ServerPlayer p, HumanBotEntity recruiter) {
        Side side = Scenario.side(p);
        if (side == null) {
            return false;
        }
        Side to = side.opposite();
        Disguise.Outfit need = to == Side.SOVIET ? Disguise.Outfit.SOVIET : Disguise.Outfit.SPECIAL_FORCES;
        if (Disguise.worn(p) != need) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.staff.enemy").withStyle(ChatFormatting.RED), true);
            return true;
        }
        ScenarioData.Record r = Scenario.record(p);
        long now = p.level().getGameTime();
        if (!r.extra.contains("DefectAsk") || now - r.extra.getLong("DefectAsk") > 200) {
            r.extra.putLong("DefectAsk", now);
            p.sendSystemMessage(Component.translatable("message.modeminecraft.defect.ask",
                    Component.translatable("gui.modeminecraft.faction.name_" + to.id)).withStyle(ChatFormatting.GOLD));
            return true;
        }
        r.extra.remove("DefectAsk");
        defect(p, recruiter, to);
        return true;
    }

    static void defect(ServerPlayer p, HumanBotEntity recruiter, Side to) {
        ServerLevel level = p.serverLevel();
        ScenarioData data = ScenarioData.get(level);
        ScenarioData.Record r = data.of(p.getUUID());
        Side from = r.side;
        r.side = to;
        r.reputation = 0;
        r.extra.put("Active", new net.minecraft.nbt.ListTag());
        r.extra.put("Done", new CompoundTag());
        r.extra.put("Flags", new CompoundTag());
        r.extra.putString("DefectedFrom", from == null ? "" : from.id);
        CityPlanner.City base = CityPlanner.cityAtCached(CityPlanner.Env.of(level), recruiter.getBlockX(), recruiter.getBlockZ(), 8);
        if (base == null || !base.variant().equals(to.baseVariant)) {
            base = Scenario.findBase(level, to, p.blockPosition());
        }
        if (base != null) {
            ScenarioSites.Dorm dorm = ScenarioSites.dorm(base, to);
            level.getChunk(dorm.bed().getX() >> 4, dorm.bed().getZ() >> 4);
            p.setRespawnPosition(level.dimension(), dorm.bed(), dorm.yaw(), false, false);
            BarracksLockerBlock.assign(level, dorm.locker(), p);
            r.base = new BlockPos(base.middleX(), base.baseY(), base.middleZ());
            r.bed = dorm.bed();
            r.locker = dorm.locker();
        }
        data.setDirty();
        Reputation.nameplate(p);
        Scenario.sync(p);
        MissionNet.sync(p);
        level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 0.6f);
        p.sendSystemMessage(Component.translatable("message.modeminecraft.defect.done",
                Component.translatable("gui.modeminecraft.faction.name_" + to.id)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (ServerPlayer o : level.getServer().getPlayerList().getPlayers()) {
            if (o != p && Scenario.side(o) == from) {
                o.sendSystemMessage(Component.translatable("message.modeminecraft.defect.traitor", p.getName()).withStyle(ChatFormatting.RED));
            }
        }
        Cinematics.play(p, "defect", 0, 0, 0, 0);
    }
}

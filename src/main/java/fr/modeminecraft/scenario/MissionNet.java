package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Réseau du journal : état des missions (serveur -> client), actions du joueur (client -> serveur), Tableau des opérations. */
public final class MissionNet {
    private MissionNet() {}

    /** Missions actives, accomplies, privilèges, disponibilité au Tableau des opérations. */
    public static void sync(ServerPlayer p) {
        ScenarioData.Record r = Scenario.record(p);
        CompoundTag t = new CompoundTag();
        t.put("Active", Missions.active(p).copy());
        t.put("Done", r.extra.getCompound("Done").copy());
        t.put("Flags", r.extra.getCompound("Flags").copy());
        CompoundTag avail = new CompoundTag();
        for (Missions.Mission m : Missions.Mission.values()) {
            avail.putString(m.id(), Missions.availability(p, m));
        }
        t.put("Avail", avail);
        PacketDistributor.sendToPlayer(p, new State(t, false));
    }

    public static void openBoard(ServerPlayer p) {
        sync(p);
        PacketDistributor.sendToPlayer(p, new State(new CompoundTag(), true));
    }

    public record State(CompoundTag data, boolean openBoard) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ModeMinecraft.id("mission_state"));
        public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, State::data,
                ByteBufCodecs.BOOL, State::openBoard, State::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(State p, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (p.openBoard()) {
                    fr.modeminecraft.client.OperationsBoardScreen.open();
                } else {
                    ScenarioClientState.missions = p.data();
                }
            });
        }
    }

    /** Action : accept / abandon / pin (identifiant de mission, ou « task:<index> » pour une tâche). */
    public record Action(String action, String id) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ModeMinecraft.id("mission_action"));
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Action::action,
                ByteBufCodecs.STRING_UTF8, Action::id, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action a, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer sp)) {
                    return;
                }
                switch (a.action()) {
                    case "accept" -> {
                        Missions.Mission m = Missions.Mission.byId(a.id());
                        if (m != null && nearRecruiter(sp)) {
                            Missions.accept(sp, m);
                        }
                    }
                    case "abandon" -> abandon(sp, a.id());
                    case "pin" -> pin(sp, a.id());
                    case "sync" -> sync(sp);
                    default -> {
                    }
                }
            });
        }
    }

    /** Index d'une entrée du journal (les tâches n'ont pas d'identifiant unique). */
    private static net.minecraft.nbt.CompoundTag byIndex(ServerPlayer p, String id) {
        if (id.startsWith("#")) {
            int i = Integer.parseInt(id.substring(1));
            var list = Missions.active(p);
            return i >= 0 && i < list.size() ? list.getCompound(i) : null;
        }
        return Missions.entry(p, id);
    }

    private static void abandon(ServerPlayer p, String id) {
        CompoundTag e = byIndex(p, id);
        if (e != null) {
            Missions.abandon(p, e.getString("Id").equals("task") ? null : e.getString("Id"));
            if (e.getString("Id").equals("task")) {
                Missions.active(p).remove(e);
                sync(p);
            }
        }
    }

    private static void pin(ServerPlayer p, String id) {
        CompoundTag e = byIndex(p, id);
        if (e != null) {
            boolean on = e.getBoolean("Pinned");
            int pins = 0;
            for (var t : Missions.active(p)) {
                pins += ((CompoundTag) t).getBoolean("Pinned") ? 1 : 0;
            }
            if (!on && pins >= Missions.MAX_PINNED) {
                return;
            }
            e.putBoolean("Pinned", !on);
            ScenarioData.get(p.serverLevel()).setDirty();
            sync(p);
        }
    }

    /** Le Tableau des opérations ne s'utilise qu'au Centre de recrutement (officier recruteur à 8 blocs). */
    private static boolean nearRecruiter(ServerPlayer p) {
        return !p.serverLevel().getEntitiesOfClass(fr.modeminecraft.entity.bot.HumanBotEntity.class, p.getBoundingBox().inflate(8),
                b -> b.role() == fr.modeminecraft.entity.bot.BotRole.RECRUITER).isEmpty();
    }
}

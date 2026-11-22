package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.entity.military.Fleets;
import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * Journal de bord tactique : jusqu'à 5 missions actives en même temps (3 épinglées au HUD).
 * - Missions principales du Centre de recrutement : Or Noir (carburant volé), Yellowcake (uranium des milices),
 *   Pièces maîtresses (4 composants de la bombe), Frappe finale (bombe nucléaire sur le QG ennemi) ;
 * - Missions stratégiques : espionnage / sabotage, embuscade de convoi blindé, chasse au sous-marin espion, extraction
 *   d'un scientifique, destruction des défenses antiaériennes (SEAD) ;
 * - Petites tâches logistiques des PNJ de la base (bois, pierre, vivres, carburant, patrouille de périmètre).
 * Les données vivent dans le dossier du joueur (ScenarioData.Record.extra).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class Missions {
    public static final int MAX_ACTIVE = 5, MAX_PINNED = 3;

    public enum Kind { MAIN, STRATEGIC, TASK }

    public enum Mission {
        OIL(Kind.MAIN, 200, 4), YELLOWCAKE(Kind.MAIN, 300, 8), COMPONENTS(Kind.MAIN, 400, 4), FINAL_STRIKE(Kind.MAIN, 1000, 1),
        ESPIONAGE(Kind.STRATEGIC, 250, 1), CONVOY(Kind.STRATEGIC, 200, 1), SUBMARINE(Kind.STRATEGIC, 300, 1),
        SCIENTIST(Kind.STRATEGIC, 350, 1), SEAD(Kind.STRATEGIC, 400, 1);

        public final Kind kind;
        public final int reputation, goal;

        Mission(Kind kind, int reputation, int goal) {
            this.kind = kind;
            this.reputation = reputation;
            this.goal = goal;
        }

        public String id() {
            return name().toLowerCase();
        }

        public static Mission byId(String id) {
            for (Mission m : values()) {
                if (m.id().equals(id)) {
                    return m;
                }
            }
            return null;
        }
    }

    private Missions() {}

    // ------------------------------------------------------------------ données
    public static ListTag active(ServerPlayer p) {
        CompoundTag x = Scenario.record(p).extra;
        if (!x.contains("Active")) {
            x.put("Active", new ListTag());
        }
        return x.getList("Active", Tag.TAG_COMPOUND);
    }

    public static CompoundTag entry(ServerPlayer p, String id) {
        for (Tag t : active(p)) {
            if (t instanceof CompoundTag c && c.getString("Id").equals(id)) {
                return c;
            }
        }
        return null;
    }

    public static boolean done(ServerPlayer p, Mission m) {
        return Scenario.record(p).extra.getCompound("Done").getInt(m.id()) > 0;
    }

    public static boolean flag(ServerPlayer p, String f) {
        return Scenario.record(p).extra.getCompound("Flags").getBoolean(f);
    }

    static void setFlag(ServerPlayer p, String f) {
        CompoundTag x = Scenario.record(p).extra;
        CompoundTag flags = x.getCompound("Flags");
        flags.putBoolean(f, true);
        x.put("Flags", flags);
    }

    static void dirty(ServerPlayer p) {
        ScenarioData.get(p.serverLevel()).setDirty();
        MissionNet.sync(p);
    }

    /** Disponibilité d'une mission au Tableau des opérations : "" = disponible, sinon clé de la raison. */
    public static String availability(ServerPlayer p, Mission m) {
        if (entry(p, m.id()) != null) {
            return "active";
        }
        if (m.kind == Kind.MAIN && done(p, m)) {
            return "done";
        }
        if (m == Mission.FINAL_STRIKE && (!done(p, Mission.COMPONENTS) || !flag(p, "sead"))) {
            return "locked_final";
        }
        long cd = Scenario.record(p).extra.getCompound("Cooldown").getLong(m.id());
        if (cd > p.level().getGameTime()) {
            return "cooldown";
        }
        if (active(p).size() >= MAX_ACTIVE) {
            return "full";
        }
        return "";
    }

    // ------------------------------------------------------------------ actions du joueur
    public static void accept(ServerPlayer p, Mission m) {
        if (Scenario.side(p) == null) {
            return;
        }
        String why = availability(p, m);
        if (!why.isEmpty()) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.mission.unavailable." + why).withStyle(ChatFormatting.RED), true);
            return;
        }
        CompoundTag e = new CompoundTag();
        e.putString("Id", m.id());
        e.putInt("Goal", m.goal);
        e.putBoolean("Pinned", pinnedCount(p) < MAX_PINNED);
        if (!setup(p, m, e)) {
            return;
        }
        active(p).add(e);
        p.sendSystemMessage(Component.translatable("message.modeminecraft.mission.accepted", Component.translatable("mission.modeminecraft." + m.id()))
                .withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.translatable("mission.modeminecraft." + m.id() + ".brief").withStyle(ChatFormatting.GRAY));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        Briefings.radio(p, "mission." + m.id());
        dirty(p);
    }

    public static void abandon(ServerPlayer p, String id) {
        if (id == null) {
            return;
        }
        CompoundTag e = entry(p, id);
        if (e == null) {
            return;
        }
        cleanup(p, e);
        active(p).remove(e);
        p.displayClientMessage(Component.translatable("message.modeminecraft.mission.abandoned"), true);
        dirty(p);
    }

    public static void togglePin(ServerPlayer p, String id) {
        CompoundTag e = entry(p, id);
        if (e == null) {
            return;
        }
        if (!e.getBoolean("Pinned") && pinnedCount(p) >= MAX_PINNED) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.mission.pins_full").withStyle(ChatFormatting.RED), true);
            return;
        }
        e.putBoolean("Pinned", !e.getBoolean("Pinned"));
        dirty(p);
    }

    private static int pinnedCount(ServerPlayer p) {
        int n = 0;
        for (Tag t : active(p)) {
            n += ((CompoundTag) t).getBoolean("Pinned") ? 1 : 0;
        }
        return n;
    }

    // ------------------------------------------------------------------ mise en place
    /** Prépare la mission (cibles, convoi, sous-marin, scientifique) ; false si impossible ici. */
    private static boolean setup(ServerPlayer p, Mission m, CompoundTag e) {
        ServerLevel level = p.serverLevel();
        Side side = Scenario.side(p);
        switch (m) {
            case ESPIONAGE, SEAD, FINAL_STRIKE -> {
                CityPlanner.City enemy = Scenario.findBase(level, side.opposite(), p.blockPosition());
                if (enemy == null) {
                    p.displayClientMessage(Component.translatable("message.modeminecraft.mission.no_enemy_base").withStyle(ChatFormatting.RED), true);
                    return false;
                }
                putTarget(e, new BlockPos(enemy.middleX(), enemy.baseY(), enemy.middleZ()));
            }
            case CONVOY -> {
                Vec3 at = landSpot(level, p.position(), 180, 300);
                if (at == null) {
                    p.displayClientMessage(Component.translatable("message.modeminecraft.mission.no_spot").withStyle(ChatFormatting.RED), true);
                    return false;
                }
                MilVehicleEntity truck = convoy(level, at, side.enemy, p.position());
                e.putUUID("Target", truck.getUUID());
                putTarget(e, truck.blockPosition());
            }
            case SUBMARINE -> {
                Vec3 at = waterSpot(level, p.position());
                if (at == null) {
                    p.displayClientMessage(Component.translatable("message.modeminecraft.mission.no_sea").withStyle(ChatFormatting.RED), true);
                    return false;
                }
                keepLoaded(level, BlockPos.containing(at));
                MilVehicleEntity sub = Fleets.launch(level, MilType.VIRGINIA, at.x, level.getSeaLevel() - 0.1, at.z, level.random.nextFloat() * 360, false, side.enemy);
                sub.subDepth = 12;
                sub.navalHome = BlockPos.containing(at);
                e.putUUID("Target", sub.getUUID());
                putTarget(e, sub.blockPosition());
            }
            case SCIENTIST -> {
                Vec3 at = landSpot(level, p.position(), 200, 320);
                if (at == null) {
                    p.displayClientMessage(Component.translatable("message.modeminecraft.mission.no_spot").withStyle(ChatFormatting.RED), true);
                    return false;
                }
                HumanBotEntity sci = spawnBot(level, at, Faction.CIVILIAN, BotRole.SCIENTIST);
                sci.home = BlockPos.containing(at);
                sci.homeRadius = 3;
                Faction guards = level.random.nextBoolean() ? side.enemy : Faction.FALCON;
                for (int i = 0; i < 4; i++) {
                    HumanBotEntity g = spawnBot(level, at.add(Math.cos(i * 1.57) * 5, 0, Math.sin(i * 1.57) * 5), guards == Faction.FALCON ? side.enemy : guards, BotRole.SOLDIER);
                    g.home = BlockPos.containing(at);
                    g.homeRadius = 10;
                }
                e.putUUID("Target", sci.getUUID());
                putTarget(e, BlockPos.containing(at));
            }
            default -> {
            }
        }
        return true;
    }

    /** Garde chargée la cible mobile d'une mission (convoi, sous-marin). */
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> TARGET_TICKET = net.minecraft.server.level.TicketType.create(
            "modeminecraft_mission_target", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 60);

    static void keepLoaded(ServerLevel level, BlockPos at) {
        net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(at);
        level.getChunkSource().addRegionTicket(TARGET_TICKET, cp, 3, cp);
    }

    static void putTarget(CompoundTag e, BlockPos p) {
        e.putInt("TX", p.getX());
        e.putInt("TY", p.getY());
        e.putInt("TZ", p.getZ());
        e.putBoolean("HasTarget", true);
    }

    private static void cleanup(ServerPlayer p, CompoundTag e) {
        if (e.hasUUID("Target") && p.serverLevel().getEntity(e.getUUID("Target")) instanceof Entity t) {
            Mission m = Mission.byId(e.getString("Id"));
            if (m == Mission.SCIENTIST || m == Mission.CONVOY || m == Mission.SUBMARINE) {
                for (Entity c : List.copyOf(t.getPassengers())) {
                    c.discard();
                }
                t.discard();
            }
        }
    }

    static HumanBotEntity spawnBot(ServerLevel level, Vec3 at, Faction f, BotRole role) {
        HumanBotEntity b = ModEntities.HUMAN_BOT.get().create(level);
        BlockPos bp = BlockPos.containing(at);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bp.getX(), bp.getZ());
        b.moveTo(at.x, y, at.z, level.random.nextFloat() * 360, 0);
        b.setup(f, role, level.getRandom());
        b.setPersistenceRequired();
        b.finalizeSpawn(level, level.getCurrentDifficultyAt(bp), MobSpawnType.EVENT, null);
        level.addFreshEntity(b);
        return b;
    }

    /** Terre ferme dégagée à une distance [min, max] du joueur (chunks chargés). */
    static Vec3 landSpot(ServerLevel level, Vec3 from, int min, int max) {
        for (int k = 0; k < 24; k++) {
            double a = level.random.nextDouble() * Math.PI * 2, r = min + level.random.nextDouble() * (max - min);
            int x = (int) (from.x + Math.cos(a) * r), z = (int) (from.z + Math.sin(a) * r);
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos below = new BlockPos(x, y - 1, z);
            if (y > level.getSeaLevel() && level.getFluidState(below).isEmpty() && !level.getBlockState(below).is(net.minecraft.tags.BlockTags.LEAVES)) {
                return new Vec3(x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    /** Eau libre (140 x 140, 12 de fond) entre 120 et 500 blocs, chunks générés au besoin. */
    static Vec3 waterSpot(ServerLevel level, Vec3 from) {
        int sea = level.getSeaLevel();
        for (int k = 0; k < 40; k++) {
            double a = level.random.nextDouble() * Math.PI * 2, r = 120 + level.random.nextDouble() * 380;
            int x = (int) (from.x + Math.cos(a) * r), z = (int) (from.z + Math.sin(a) * r);
            boolean ok = true;
            for (int dx = -70; dx <= 70 && ok; dx += 35) {
                for (int dz = -70; dz <= 70 && ok; dz += 35) {
                    level.getChunk((x + dx) >> 4, (z + dz) >> 4);
                    ok = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x + dx, z + dz) <= sea - 12;
                }
            }
            if (ok) {
                return new Vec3(x + 0.5, sea, z + 0.5);
            }
        }
        return null;
    }

    /** Convoi blindé ennemi : camion logistique et deux blindés légers, équipages complets, en route. */
    private static MilVehicleEntity convoy(ServerLevel level, Vec3 at, Faction f, Vec3 player) {
        boolean sov = f == Faction.SOVIET;
        MilType truckType = sov ? MilType.KAMAZ : MilType.M939, escort = sov ? MilType.TIGR : MilType.HUMVEE;
        Vec3 dir = at.subtract(player).multiply(1, 0, 1).normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        float yaw = (float) Math.toDegrees(Math.atan2(-side.x, side.z));
        BlockPos dest = BlockPos.containing(at.add(side.scale(700)));
        keepLoaded(level, BlockPos.containing(at));
        MilVehicleEntity truck = milAt(level, truckType, at, yaw, f, 2);
        truck.destination = dest;
        for (int i = -1; i <= 1; i += 2) {
            MilVehicleEntity e = milAt(level, escort, at.add(side.scale(i * 14)), yaw, f, 3);
            e.leader = truck.getUUID();
            e.destination = dest;
        }
        return truck;
    }

    private static MilVehicleEntity milAt(ServerLevel level, MilType t, Vec3 at, float yaw, Faction f, int crew) {
        BlockPos bp = BlockPos.containing(at);
        level.getChunk(bp.getX() >> 4, bp.getZ() >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bp.getX(), bp.getZ());
        MilVehicleEntity v = MilVehicleEntity.create(level, t, at.x, y + 0.5, at.z, yaw);
        v.fillAmmo(1f);
        v.setFuel(1f);
        level.addFreshEntity(v);
        for (int i = 0; i < crew; i++) {
            HumanBotEntity b = spawnBot(level, at, f, i == 0 ? BotRole.OFFICER : BotRole.SOLDIER);
            b.convoy = true;
            b.startRiding(v, true);
        }
        return v;
    }

    // ------------------------------------------------------------------ réalisation
    /** Remise au Centre de recrutement (clic droit sur l'officier recruteur) : livraisons, rapports. */
    public static void report(ServerPlayer p) {
        boolean any = false;
        for (Tag t : List.copyOf(active(p))) {
            CompoundTag e = (CompoundTag) t;
            Mission m = Mission.byId(e.getString("Id"));
            if (m == null) {
                continue;
            }
            switch (m) {
                case OIL -> {
                    int have = count(p, ModItems.JERRYCAN.get()) + count(p, oilBarrel());
                    if (have >= m.goal) {
                        take(p, oilBarrel(), take(p, ModItems.JERRYCAN.get(), m.goal));
                        refuelBase(p);
                        complete(p, e, m);
                        any = true;
                    }
                }
                case YELLOWCAKE -> {
                    if (count(p, ModItems.URANIUM_CANISTER.get()) >= m.goal) {
                        complete(p, e, m);                                       // l'uranium reste au joueur (la bombe)
                        any = true;
                    }
                }
                case COMPONENTS -> {
                    if (components(p) >= 4) {
                        complete(p, e, m);
                        any = true;
                    }
                }
                case ESPIONAGE -> {
                    if (count(p, ModItems.SECRET_PLANS.get()) > 0) {
                        take(p, ModItems.SECRET_PLANS.get(), 1);
                        complete(p, e, m);
                        any = true;
                    }
                }
                default -> {
                }
            }
        }
        if (!any) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.mission.nothing_to_report"), true);
        }
    }

    public static int components(ServerPlayer p) {
        int n = 0;
        for (Item i : new Item[]{ModItems.NUCLEAR_GUIDANCE_UNIT.get(), ModItems.NUCLEAR_NEUTRON_REFLECTOR.get(),
                ModItems.NUCLEAR_IMPLOSION_LENSES.get(), ModItems.NUCLEAR_KRYTRON_TRIGGER.get()}) {
            n += count(p, i) > 0 ? 1 : 0;
        }
        return n;
    }

    private static Item oilBarrel() {
        Item i = BuiltInRegistries.ITEM.get(ModeMinecraft.id("oil_barrel"));
        return i == Items.AIR ? ModItems.JERRYCAN.get() : i;
    }

    static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    /** Retire jusqu'à `n` objets ; renvoie ce qui reste à retirer. */
    static int take(ServerPlayer p, Item item, int n) {
        for (int i = 0; i < p.getInventory().getContainerSize() && n > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) {
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
        return n;
    }

    /** Or Noir réussi : les véhicules de la base reçoivent la moitié d'un plein. */
    private static void refuelBase(ServerPlayer p) {
        CityPlanner.City c = Quarters.base(p);
        if (c == null) {
            return;
        }
        Side s = Scenario.side(p);
        int n = 0;
        for (MilVehicleEntity v : p.serverLevel().getEntitiesOfClass(MilVehicleEntity.class,
                new net.minecraft.world.phys.AABB(c.minX(), c.baseY() - 20, c.minZ(), c.maxX(), c.baseY() + 60, c.maxZ()), v -> v.type().side == s.vehicles)) {
            if (v.fuel() < 0.5f) {
                v.setFuel(0.5f);
                n++;
            }
        }
        p.sendSystemMessage(Component.translatable("message.modeminecraft.mission.oil.refuelled", n).withStyle(ChatFormatting.AQUA));
    }

    static void complete(ServerPlayer p, CompoundTag e, Mission m) {
        active(p).remove(e);
        CompoundTag x = Scenario.record(p).extra;
        CompoundTag doneTag = x.getCompound("Done");
        doneTag.putInt(m.id(), doneTag.getInt(m.id()) + 1);
        x.put("Done", doneTag);
        if (m.kind == Kind.STRATEGIC) {
            CompoundTag cd = x.getCompound("Cooldown");
            cd.putLong(m.id(), p.level().getGameTime() + 24000);                   // une fois par jour de jeu
            x.put("Cooldown", cd);
        }
        rewards(p, m);
        p.sendSystemMessage(Component.translatable("message.modeminecraft.mission.completed", Component.translatable("mission.modeminecraft." + m.id()))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        Reputation.add(p, m.reputation, "mission.modeminecraft." + m.id());
        Briefings.radio(p, "done." + m.id());
        dirty(p);
        if (m == Mission.FINAL_STRIKE) {
            Victory.win(p);
        }
    }

    private static void rewards(ServerPlayer p, Mission m) {
        switch (m) {
            case OIL -> give(p, new ItemStack(Items.GOLD_INGOT, 4), item("ammo_box_50", 2));
            case YELLOWCAKE -> give(p, new ItemStack(Items.GOLD_INGOT, 6), new ItemStack(Items.DIAMOND, 1), item("he_shell", 4), item("jerrycan", 2));
            case COMPONENTS -> give(p, new ItemStack(Items.GOLD_INGOT, 8), new ItemStack(Items.DIAMOND, 2));
            case ESPIONAGE -> {
                setFlag(p, "plans");                                              // bombe moins chère, plans furtifs
                give(p, new ItemStack(Items.GOLD_INGOT, 4), item("radar_absorbent_panel", 8));
            }
            case CONVOY -> give(p, item("he_shell", 6), item("ap_i_belt", 2), new ItemStack(ModItems.URANIUM_CANISTER.get(), 2), new ItemStack(Items.GOLD_INGOT, 4));
            case SUBMARINE -> {
                setFlag(p, "cruise");
                give(p, item("cruise_missile", 2), item("sonar_array", 1), new ItemStack(Items.GOLD_INGOT, 4));
            }
            case SCIENTIST -> {
                setFlag(p, "uranium_half");                                       // -50 % d'uranium pour la bombe
                give(p, new ItemStack(Items.GOLD_INGOT, 6));
            }
            case SEAD -> {
                setFlag(p, "sead");                                               // couloir aérien sécurisé
                give(p, item("agm_missile", 4), item("guided_bomb", 2));
            }
            default -> {
            }
        }
    }

    static ItemStack item(String id, int n) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ModeMinecraft.MODID, id)), n);
    }

    static void give(ServerPlayer p, ItemStack... stacks) {
        for (ItemStack s : stacks) {
            if (!s.isEmpty() && !p.getInventory().add(s)) {
                p.drop(s, false);
            }
        }
    }

    // ------------------------------------------------------------------ événements du monde
    /** Un véhicule détruit ou coulé : convoi, sous-marin espion, défense antiaérienne ennemie. */
    public static void onVehicleLost(MilVehicleEntity v, Entity by) {
        if (!(v.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer killer = by instanceof ServerPlayer sp ? sp : by != null && by.getVehicle() != null
                && by.getVehicle().getControllingPassenger() instanceof ServerPlayer sp2 ? sp2 : null;
        for (ServerPlayer p : level.players()) {
            for (Tag t : List.copyOf(active(p))) {
                CompoundTag e = (CompoundTag) t;
                Mission m = Mission.byId(e.getString("Id"));
                if ((m == Mission.CONVOY || m == Mission.SUBMARINE) && e.hasUUID("Target") && e.getUUID("Target").equals(v.getUUID())) {
                    complete(p, e, m);
                } else if (m == Mission.SEAD && p == killer && Scenario.side(p) != null && v.type().side == Scenario.side(p).opposite().vehicles
                        && isAirDefense(v.type())) {
                    complete(p, e, m);
                }
            }
        }
    }

    private static boolean isAirDefense(MilType t) {
        return t == MilType.S_400 || t == MilType.S_500 || t == MilType.AVENGER || t == MilType.ZSU_23_4;
    }

    /** Détonation nucléaire : Frappe finale si elle touche le QG ennemi (moins de 220 blocs de son centre). */
    public static void onNuke(ServerLevel level, Vec3 at, Entity owner) {
        for (ServerPlayer p : level.players()) {
            CompoundTag e = entry(p, Mission.FINAL_STRIKE.id());
            if (e != null && e.getBoolean("HasTarget") && Math.hypot(e.getInt("TX") - at.x, e.getInt("TZ") - at.z) < 220) {
                complete(p, e, Mission.FINAL_STRIKE);
            }
        }
    }

    /** Terminal des plans dans l'état-major ennemi : en tenue complète de l'ennemi, on vole les plans. */
    public static boolean stealPlans(ServerPlayer p, Side terminalSide) {
        CompoundTag e = entry(p, Mission.ESPIONAGE.id());
        Side s = Scenario.side(p);
        if (e == null || s == null || terminalSide != s.opposite()) {
            return false;
        }
        var worn = fr.modeminecraft.entity.bot.Disguise.worn(p);
        boolean disguised = worn == (s.opposite() == Side.SOVIET ? fr.modeminecraft.entity.bot.Disguise.Outfit.SOVIET
                : fr.modeminecraft.entity.bot.Disguise.Outfit.SPECIAL_FORCES);
        if (!disguised) {
            p.displayClientMessage(Component.translatable("message.modeminecraft.mission.espionage.need_uniform").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (count(p, ModItems.SECRET_PLANS.get()) == 0) {
            give(p, new ItemStack(ModItems.SECRET_PLANS.get()));
            e.putInt("Progress", 1);
            p.displayClientMessage(Component.translatable("message.modeminecraft.mission.espionage.got_plans").withStyle(ChatFormatting.GOLD), true);
            dirty(p);
        }
        return true;
    }

    /** Chaque seconde : progression affichée, cibles mobiles, scientifique escorté, petites tâches. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0 || Scenario.side(p) == null) {
            return;
        }
        ListTag list = active(p);
        if (list.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (Tag t : List.copyOf(list)) {
            CompoundTag e = (CompoundTag) t;
            Mission m = Mission.byId(e.getString("Id"));
            int before = e.getInt("Progress");
            if (m == null) {
                if (e.getString("Id").equals("contract")) {
                    Armory.tick(p, e);
                } else {
                    Tasks.tick(p, e);
                }
                changed |= e.getInt("Progress") != before;
                continue;
            }
            switch (m) {
                case OIL -> e.putInt("Progress", Math.min(m.goal, count(p, ModItems.JERRYCAN.get()) + (oilBarrel() == ModItems.JERRYCAN.get() ? 0 : count(p, oilBarrel()))));
                case YELLOWCAKE -> e.putInt("Progress", Math.min(m.goal, count(p, ModItems.URANIUM_CANISTER.get())));
                case COMPONENTS -> e.putInt("Progress", components(p));
                case CONVOY, SUBMARINE -> {
                    // la cible reste chargée (elle roule / navigue même loin du joueur), suivie sur le HUD
                    if (e.getBoolean("HasTarget")) {
                        keepLoaded(p.serverLevel(), new BlockPos(e.getInt("TX"), 0, e.getInt("TZ")));
                    }
                    if (e.hasUUID("Target") && p.serverLevel().getEntity(e.getUUID("Target")) instanceof Entity target) {
                        putTarget(e, target.blockPosition());
                    }
                }
                case SCIENTIST -> escort(p, e);
                default -> {
                }
            }
            changed |= e.getInt("Progress") != before || m == Mission.CONVOY || m == Mission.SUBMARINE;
        }
        if (changed) {
            MissionNet.sync(p);
        }
    }

    /** Scientifique libéré : il suit le joueur (à pied ou dans son véhicule) ; arrivé au centre de recrutement, c'est gagné. */
    private static void escort(ServerPlayer p, CompoundTag e) {
        if (!e.hasUUID("Target") || !(p.serverLevel().getEntity(e.getUUID("Target")) instanceof HumanBotEntity sci) || !sci.isAlive()) {
            if (e.getInt("Progress") >= 0 && e.hasUUID("Target") && p.serverLevel().getEntity(e.getUUID("Target")) == null
                    && e.getBoolean("Freed")) {
                return;                                                          // hors de portée de chargement
            }
            return;
        }
        putTarget(e, sci.blockPosition());
        if (!e.getBoolean("Freed")) {
            return;
        }
        Entity veh = p.getVehicle();
        if (veh != null && sci.getVehicle() != veh && sci.distanceTo(p) < 8 && veh.getPassengers().size() < 4) {
            sci.startRiding(veh, true);
        } else if (veh == null && sci.getVehicle() != null) {
            sci.stopRiding();
        } else if (sci.getVehicle() == null && sci.distanceTo(p) > 3) {
            sci.getNavigation().moveTo(p, 1.25);
        }
        CityPlanner.City c = Quarters.base(p);
        if (c != null) {
            BlockPos rc = ScenarioSites.at(c, Scenario.side(p), ScenarioSites.Site.RECRUITMENT, new int[]{16, 0, 8});
            if (sci.blockPosition().distSqr(rc) < 14 * 14) {
                sci.discard();                                                  // pris en charge par l'état-major
                complete(p, e, Mission.SCIENTIST);
            }
        }
    }

    /** Clic droit sur le scientifique retenu : il est libéré et suit son sauveteur. */
    public static boolean freeScientist(ServerPlayer p, HumanBotEntity sci) {
        CompoundTag e = entry(p, Mission.SCIENTIST.id());
        if (e == null || !e.hasUUID("Target") || !e.getUUID("Target").equals(sci.getUUID())) {
            return false;
        }
        if (!e.getBoolean("Freed")) {
            e.putBoolean("Freed", true);
            e.putInt("Progress", 0);
            sci.home = null;
            p.displayClientMessage(Component.translatable("message.modeminecraft.mission.scientist.freed").withStyle(ChatFormatting.GOLD), true);
            dirty(p);
        }
        return true;
    }

    /** Patrouille de périmètre : les créatures hostiles abattues près de sa base comptent. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || Scenario.side(p) == null) {
            return;
        }
        boolean hostile = dead instanceof Enemy || dead instanceof HumanBotEntity hb && (hb.faction() == Faction.MILITIA || hb.faction() == Faction.CARTEL
                || hb.faction() == Faction.FALCON || hb.faction() == Faction.OFFSHORE);
        if (hostile) {
            Tasks.onKill(p, dead);
        }
        Armory.onKill(p, dead);
        if (dead instanceof HumanBotEntity hb && hb.faction() == Scenario.side(p).ally && hb.role() != BotRole.BOSS) {
            Reputation.add(p, -40, "rep.modeminecraft.killed_ally");
        }
    }

    public static UUID targetOf(CompoundTag e) {
        return e.hasUUID("Target") ? e.getUUID("Target") : null;
    }
}

package fr.modeminecraft.scenario;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Scénario de survie « La Course à l'arme nucléaire » : au premier spawn en survie, le joueur (invulnérable, immobile)
 * choisit son camp, États-Unis ou URSS. Il reçoit son paquetage (M4A1 ou AK-47, 150 cartouches, tenue de recrue,
 * rations, bandages) et apparaît dans le dortoir des recrues de la base principale de son pays : son lit devient son
 * point de réapparition, un casier personnel lui est attribué. L'armée de son pays est son alliée, l'armée adverse
 * l'attaque à vue ; les villes des plaines restent neutres. Règle de jeu : modeminecraftScenario (activée par défaut).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class Scenario {
    public static final GameRules.Key<GameRules.BooleanValue> RULE = GameRules.register("modeminecraftScenario", GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(true));
    /** Joueurs qui n'ont pas encore choisi leur camp (position où ils attendent). */
    private static final Map<UUID, Vec3> PENDING = new HashMap<>();

    private Scenario() {}

    /** Camp du joueur (côté serveur), ou null. */
    public static Side side(Player p) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return ScenarioClientState.side;
        }
        ScenarioData.Record r = ScenarioData.get(sl).peek(p.getUUID());
        return r == null ? null : r.side;
    }

    public static ScenarioData.Record record(ServerPlayer p) {
        return ScenarioData.get(p.serverLevel()).of(p.getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        ScenarioData.Record r = record(sp);
        if (r.side == null && sp.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && sp.serverLevel().getGameRules().getBoolean(RULE)) {
            begin(sp);
        }
        sync(sp);
        if (r.side != null) {
            Reputation.nameplate(sp);
            MissionNet.sync(sp);
        }
    }

    /** Ouvre le choix du camp : joueur immobile et invulnérable jusqu'à sa décision. */
    public static void begin(ServerPlayer sp) {
        PENDING.put(sp.getUUID(), sp.position());
        sp.setInvulnerable(true);
        PacketDistributor.sendToPlayer(sp, ScenarioPayloads.OpenChoice.INSTANCE);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (PENDING.remove(event.getEntity().getUUID()) != null) {
            event.getEntity().setInvulnerable(false);
        }
    }

    /** En attente du choix : immobile et invulnérable ; l'écran est rouvert s'il a été fermé. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        Vec3 at = PENDING.get(sp.getUUID());
        if (at == null) {
            return;
        }
        if (sp.position().distanceToSqr(at) > 0.25) {
            sp.connection.teleport(at.x, at.y, at.z, sp.getYRot(), sp.getXRot());
        }
        if (sp.tickCount % 100 == 0) {
            PacketDistributor.sendToPlayer(sp, ScenarioPayloads.OpenChoice.INSTANCE);
        }
    }

    /** Camp choisi (paquet du client). */
    public static void choose(ServerPlayer sp, Side side) {
        if (!PENDING.containsKey(sp.getUUID()) || side == null) {
            return;
        }
        PENDING.remove(sp.getUUID());
        sp.setInvulnerable(false);
        ScenarioData data = ScenarioData.get(sp.serverLevel());
        ScenarioData.Record r = data.of(sp.getUUID());
        r.side = side;
        loadout(sp, side);
        CityPlanner.City base = findBase(sp.serverLevel(), side, sp.blockPosition());
        if (base != null) {
            ScenarioSites.Dorm dorm = ScenarioSites.dorm(base, side);
            ServerLevel level = sp.serverLevel();
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.getChunk((dorm.spawn().getX() >> 4) + dx, (dorm.spawn().getZ() >> 4) + dz);      // dortoir généré
                }
            }
            sp.teleportTo(level, dorm.spawn().getX() + 0.5, dorm.spawn().getY(), dorm.spawn().getZ() + 0.5, dorm.yaw(), 0);
            sp.setRespawnPosition(level.dimension(), dorm.bed(), dorm.yaw(), false, false);   // couchette : point de réveil à côté
            fr.modeminecraft.scenario.BarracksLockerBlock.assign(level, dorm.locker(), sp);
            r.base = new BlockPos(base.middleX(), base.baseY(), base.middleZ());
            Reputation.nameplate(sp);
            r.bed = dorm.bed();
            r.locker = dorm.locker();
            sp.displayClientMessage(Component.translatable("message.modeminecraft.scenario.arrived." + side.id).withStyle(ChatFormatting.GOLD), false);
        } else {
            sp.displayClientMessage(Component.translatable("message.modeminecraft.scenario.no_base").withStyle(ChatFormatting.RED), false);
        }
        sp.displayClientMessage(Component.translatable("message.modeminecraft.scenario.allegiance." + side.id).withStyle(ChatFormatting.YELLOW), false);
        data.setDirty();
        sync(sp);
        ModeMinecraft.LOGGER.info("Scénario : {} rejoint le camp {} (base {})", sp.getName().getString(), side.id, r.base);
    }

    /** Paquetage réglementaire : fusil d'assaut chargé, 150 cartouches, tenue de recrue, 4 rations, 2 bandages. */
    static void loadout(ServerPlayer sp, Side side) {
        boolean us = side == Side.US;
        ItemStack gun = new ItemStack(us ? ModItems.M4.get() : ModItems.AK47.get());
        if (gun.getItem() instanceof GunItem g) {
            GunItem.setAmmo(gun, g.stats().magazineSize());
            GunItem.setChambered(gun, true);
        }
        give(sp, gun);
        Item ammo = us ? ModItems.AMMO_556.get() : ModItems.AMMO_762.get();
        for (int left = 150; left > 0; ) {
            int n = Math.min(left, new ItemStack(ammo).getMaxStackSize());
            give(sp, new ItemStack(ammo, n));
            left -= n;
        }
        String uniform = us ? "special_forces" : "soviet_uniform";
        equip(sp, EquipmentSlot.HEAD, new ItemStack(us ? ModItems.PASGT.get() : ModItems.SSH40.get()));
        equip(sp, EquipmentSlot.CHEST, new ItemStack(ModItems.OUTFITS.get(uniform + "_chestplate").get()));
        equip(sp, EquipmentSlot.LEGS, new ItemStack(ModItems.OUTFITS.get(uniform + "_leggings").get()));
        equip(sp, EquipmentSlot.FEET, new ItemStack(ModItems.OUTFITS.get(uniform + "_boots").get()));
        give(sp, new ItemStack(ModItems.MILITARY_RATION.get(), 4));
        give(sp, new ItemStack(ModItems.BANDAGE.get(), 2));
    }

    private static void equip(ServerPlayer sp, EquipmentSlot slot, ItemStack s) {
        if (sp.getItemBySlot(slot).isEmpty()) {
            sp.setItemSlot(slot, s);
        } else {
            give(sp, s);
        }
    }

    private static void give(ServerPlayer sp, ItemStack s) {
        if (!sp.getInventory().add(s)) {
            sp.drop(s, false);
        }
    }

    /** Base principale du camp la plus proche (villes « base » américaines / « soviet »), jusqu'à ~12 régions de distance. */
    public static CityPlanner.City findBase(ServerLevel level, Side side, BlockPos from) {
        var placement = CityPlanner.placement(level.registryAccess());
        if (placement == null) {
            return null;
        }
        var env = CityPlanner.Env.of(level);
        int rx0 = Math.floorDiv(from.getX() >> 4, placement.spacing()), rz0 = Math.floorDiv(from.getZ() >> 4, placement.spacing());
        CityPlanner.City best = null;
        double bestD = Double.MAX_VALUE;
        for (int ring = 0; ring <= 12; ring++) {
            for (int i = -ring; i <= ring; i++) {
                for (int j = -ring; j <= ring; j++) {
                    if (Math.max(Math.abs(i), Math.abs(j)) != ring) {
                        continue;
                    }
                    CityPlanner.City c = CityPlanner.city(env, rx0 + i, rz0 + j);
                    if (c != null && c.variant().equals(side.baseVariant)) {
                        double d = Math.hypot(c.middleX() - from.getX(), c.middleZ() - from.getZ());
                        if (d < bestD) {
                            bestD = d;
                            best = c;
                        }
                    }
                }
            }
            if (best != null && ring >= 1 && bestD < (ring - 0.5) * placement.spacing() * 16) {
                break;                                                     // aucune région plus lointaine ne peut être plus proche
            }
        }
        return best;
    }

    /** Envoie au client son camp (HUD, écrans). */
    public static void sync(ServerPlayer sp) {
        ScenarioData.Record r = record(sp);
        PacketDistributor.sendToPlayer(sp, new ScenarioPayloads.Sync(r.side == null ? "" : r.side.id, r.reputation));
    }
}

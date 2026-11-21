package fr.modeminecraft.world.police;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.block.police.JailDoorBlockEntity;
import fr.modeminecraft.block.police.SlidingJailDoorBlock;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.world.city.CityPlacement;
import fr.modeminecraft.world.city.CityPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Arrestation : un joueur tué par la police (ou le SWAT) n'apparaît pas à son lit mais dans une cellule du
 * commissariat central de la ville (métropole), les mains vides : tout son équipement est confisqué. La porte de la
 * cellule reste verrouillée 2 minutes (minuterie au-dessus de la porte), sauf si un complice ouvre depuis le poste
 * de garde ou force la porte au pied-de-biche depuis le couloir.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class Arrest {
    public static final int SENTENCE = 2400;
    /** Arrestations en attente de réapparition (cellule : intérieur x, y, z, porte x, y, z). */
    private static final java.util.Map<java.util.UUID, int[]> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

    /** Cellules du commissariat (repère local de la parcelle « police », 32 x 32) : x du bord gauche de chaque cellule. */
    public static final int[] CELL_X = {5, 11, 17};
    public static final int CELL_Z = 19, DOOR_Z = 18;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof HumanBotEntity bot) || !bot.faction().group.equals("police")) {
            return;
        }
        CityPlanner.City city = nearestMetropolis(level, player.getBlockX(), player.getBlockZ());
        if (city == null) {
            return;
        }
        int[] cell = cell(city, Math.floorMod(player.getUUID().hashCode(), CELL_X.length));
        PENDING.put(player.getUUID(), cell);
        // confiscation : l'inventaire est vidé avant la mort (rien ne tombe, rien n'est gardé)
        player.getInventory().clearContent();
        var tactical = fr.modeminecraft.item.tactical.TacticalSlots.of(player);
        for (int i = 0; i < tactical.getSlots(); i++) {
            tactical.setStackInSlot(i, ItemStack.EMPTY);
        }
        player.containerMenu.setCarried(ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isEndConquered()) {
            return;
        }
        int[] c = PENDING.remove(player.getUUID());
        if (c == null) {
            return;
        }
        ServerLevel level = player.server.overworld();
        BlockPos inside = new BlockPos(c[0], c[1], c[2]), door = new BlockPos(c[3], c[4], c[5]);
        level.getChunk(inside);                                         // génère / charge le commissariat
        // la cellule la plus libre : on évite d'enfermer deux joueurs ensemble
        BlockPos lower = findDoor(level, door);
        player.teleportTo(level, inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, 0, 0);
        if (lower != null) {
            JailDoorBlockEntity.lock(level, lower, SENTENCE, player.getUUID());
        }
        player.displayClientMessage(Component.translatable("message.modeminecraft.jail.arrested", SENTENCE / 1200).withStyle(ChatFormatting.RED), false);
    }

    private static BlockPos findDoor(Level level, BlockPos near) {
        for (BlockPos p : BlockPos.betweenClosed(near.offset(-2, -2, -2), near.offset(2, 2, 2))) {
            var s = level.getBlockState(p);
            if (s.getBlock() instanceof SlidingJailDoorBlock && s.getValue(SlidingJailDoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                return p.immutable();
            }
        }
        return null;
    }

    /** {intérieur x, y, z, porte x, y, z} de la cellule `i` du commissariat de la ville. */
    public static int[] cell(CityPlanner.City city, int i) {
        int ox = (city.center().x + 4) * 16, oz = (city.center().z + 1) * 16, y = city.baseY();
        int x0 = CELL_X[i];
        return new int[]{ox + x0 + 2, y + 1, oz + CELL_Z + 2, ox + x0 + 2, y + 1, oz + DOOR_Z};
    }

    /** La métropole (avec commissariat central) la plus proche, à 1 500 blocs au plus. */
    public static CityPlanner.City nearestMetropolis(ServerLevel level, int x, int z) {
        CityPlanner.Env env = CityPlanner.Env.of(level);
        CityPlacement p = CityPlanner.placement(level.registryAccess());
        if (p == null) {
            return null;
        }
        int rx = Math.floorDiv(x >> 4, p.spacing()), rz = Math.floorDiv(z >> 4, p.spacing());
        CityPlanner.City best = null;
        double bestD = 1500 * 1500;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                CityPlanner.City c = CityPlanner.city(env, rx + i, rz + j);
                if (c != null && c.variant().equals("metropolis")) {
                    double d = Math.pow(c.middleX() - x, 2) + Math.pow(c.middleZ() - z, 2);
                    if (d < bestD) {
                        bestD = d;
                        best = c;
                    }
                }
            }
        }
        return best;
    }

    private Arrest() {}
}
